// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/OptimizerBase.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (UI, reported): ReportProgress stays as the abstract hook but the concrete optimizers no
// longer post UI events (see ItemInstanceOptimizer/ItemOptimizer). The ThreadPool valuation paths
// (GetThreadPoolValuation/ThreadPoolDirectUpgradeValuation/EffectiveMaxConcurrencyLevel) are replaced
// by an opt-in deterministic equivalent: setMaxThreads(n > 1) evaluates candidates on a bounded
// pool, collects by index and merges in ascending index order (see DECISIONS #58).
package rawr.base.optimizer;

import rawr.base.util.KeyValuePair;

import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizerRangeValidatorBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Abstract optimizer class that supports optimization of individuals that are defined by a list of
 * items that fill slots where each slot has a list of valid items that can fill it and there can be
 * restrictions on which item combinations are valid.
 */
/**
 * Abstract skeleton of the search that finds the best gear set for a character: a genetic algorithm
 * (with an optional simulated annealing mode) over "individuals" &mdash; complete candidate gear
 * sets &mdash; whose genes are the items chosen for a fixed list of slots.
 *
 * <p>The class is generic over three types; each concrete optimizer supplies them:</p>
 * <ul>
 *   <li>{@code TItem} &mdash; one thing that can fill a slot (an {@link ItemInstance} for
 *       {@link ItemInstanceOptimizer}, an {@link Item} for {@link ItemOptimizer});</li>
 *   <li>{@code TIndividual} &mdash; one complete gear set, rebuildable from its gene array;</li>
 *   <li>{@code TValuation} &mdash; the model result of evaluating an individual; the search
 *       maximizes the objective value derived from it.</li>
 * </ul>
 *
 * <h2>The genetic algorithm</h2>
 *
 * <p>{@code optimizeGA} treats the population as a set of "islands" of 20 individuals and runs
 * generations until the global best value has not improved for {@code thoroughness} generations.
 * Each generation valuates every individual, records the best individual of each island and the
 * global best, then builds the next population:</p>
 * <ol>
 *   <li>the first individual of each island is replaced by a copy of that island's best (or a fresh
 *       random individual while the island has no best yet);</li>
 *   <li>with probability 0.05 the position becomes a completely random individual;</li>
 *   <li>otherwise, with probability 0.4, it becomes a <i>crossover</i> child whose every slot is
 *       copied from one of two parents; the parents are picked within their island by the roulette
 *       weight {@code ((value - islandMin) / (islandMax - islandMin))^10 + 0.1}, normalized per
 *       island, which strongly favors the higher-valued individuals;</li>
 *   <li>the remaining positions become <i>mutants</i>: a copy of one parent with at least 2 and
 *       usually more slots re-randomized.</li>
 * </ol>
 *
 * <p>An island that has not improved for more than 50 generations may take a parent from a random
 * other island (a "transplant") to break out of a local optimum. The terminating generation runs
 * {@code lookForDirectItemUpgrades()} as a last resort; when that greedy single-slot scan finds an
 * improvement the generation counter is reset and the search continues.</p>
 *
 * <h2>Thoroughness</h2>
 *
 * <p>{@link #thoroughness} is the user-facing effort dial. In the genetic algorithm it is both the
 * population size and the generation budget; a value of 1 disables the search entirely and leaves
 * the direct-upgrade scan as the whole optimization (start from the injected or from a random
 * individual and keep performing the best single-slot swap). In simulated annealing the search runs
 * {@code thoroughness * thoroughness} random-swap cycles.</p>
 *
 * <h2>Validation, recycling, randomness</h2>
 *
 * <ul>
 *   <li>{@link #validators} veto partial gene arrays (for example two copies of a unique item).
 *       {@link #generatorBuildIndividual} rebuilds a slot and rewinds to the validator's
 *       {@code startSlot} when a check fails, so a generated individual always satisfies every
 *       validator.</li>
 *   <li>{@link #supportsRecycling} lets a subclass keep two individual arrays and refill them every
 *       generation instead of allocating new individuals; the subclass's {@code generateIndividual}
 *       decides whether it may adopt or must copy the gene array.</li>
 *   <li>Randomness comes from {@link NetRandom}, a faithful port of the .NET subtractive generator.
 *       The seed is either the one supplied via {@link #setRndSeed(int)} (the deterministic mode
 *       used by tests and the oracle comparison) or a value drawn from a shared, time-seeded static
 *       generator, which makes the default behavior non-reproducible across runs.</li>
 * </ul>
 *
 * <h2>Parallel candidate evaluation</h2>
 *
 * <p>{@code optimize} runs inside {@link #withEvaluationPool} when {@link #setMaxThreads} is
 * greater than 1. Population valuations and direct-upgrade candidates are then evaluated by
 * {@link #evaluateAll}, which returns results in ascending candidate index order, and the callers
 * merge them with the original strict-inequality comparisons. The RNG draws and all search-order
 * decisions stay on the calling thread, so a fixed seed yields bit-identical scores and chosen
 * items at any thread count (DECISIONS #58). The pool is shut down and awaited before the run
 * returns.</p>
 */
public abstract class OptimizerBase<TItem, TIndividual, TValuation> {
    /** Result of one search run: the winner, its score/valuation and the injection flag. */
    protected record OptimizationResult<TI, TV>(TI individual, float bestValue, TV bestValuation, boolean injected) {
        public OptimizationResult {
        }
    }

    /** Result of one direct-upgrade scan. */
    protected record DirectUpgradeResult<TI, TV>(float bestValue, TI individual, TV valuation) {
        public DirectUpgradeResult {
        }
    }

    /** Sentinels used by the search when no candidate/valuation exists yet. */
    protected static final float NO_SEARCH_SCORE = -10000000f;
    protected static final float MAX_SEARCH_SCORE = 10000000f;
    /** Penalty per invalid gem requirement, kept as an {@code int} so the product stays int arithmetic. */
    protected static final int GEM_REQUIREMENT_PENALTY = 100000;
    /**
     * User-facing effort dial; see the class documentation for the per-mode meaning. Higher values
     * grow the population/generation budget (genetic algorithm) or the cycle count (simulated
     * annealing), so the runtime grows roughly with the square of this value.
     */
    protected int thoroughness;

    /** Selects the search: the genetic algorithm (default) or simulated annealing. */
    public OptimizationMethod optimizationMethod = OptimizationMethod.GeneticAlgorithm;

    /** Number of item slots that define an individual. */
    protected int slotCount;

    /**
     * Array of lists for each slot of potential items to fill the slot. This should be populated
     * unless a custom implementation of getRandomItem and lookForDirectItemUpgrades is provided.
     */
    @SuppressWarnings("unchecked")
    protected List<TItem>[] slotItems;

    /** List of validators sorted in the order they should be executed. */
    protected List<OptimizerRangeValidatorBase<TItem>> validators;

    /**
     * When set to true it will maintain two arrays of individuals and reuse them, but when an
     * individual is copied between generations it needs to make an explicit copy over. If you use
     * recycling that would create an error since the same individual could be reused in the same
     * generation for different genes.
     */
    protected boolean supportsRecycling;

    protected boolean cancellationPending;

    /**
     * Requests cancellation. A running search notices at its next generation/cycle boundary and
     * returns {@code null} instead of a result, leaving any partially built individual to be
     * discarded by the caller.
     */
    public void cancelAsync() {
        cancellationPending = true;
    }

    /**
     * Opt-in deterministic parallel candidate evaluation (DECISIONS #58), the Java equivalent of the
     * original's optional {@code ThreadPoolValuation} multithreading. 1 (default) keeps the ported
     * sequential behavior. When greater than 1, pure candidate evaluations (GA population valuations
     * and {@code lookForDirectItemUpgrades}) run on a bounded pool while the GA/search order and all
     * RNG use stay on the calling thread; results are merged in ascending candidate index order, so
     * the same {@link #setRndSeed(int) seed} yields bit-identical scores and chosen items.
     */
    private int maxThreads = 1;

    /** Pool for the current run; non-null only while {@link #withEvaluationPool} holds a scope. */
    private ExecutorService evaluationPool;

    /** Worker-thread name prefix; tests assert cleanup by scanning live threads. */
    public static final String EVALUATION_THREAD_PREFIX = "optimizer-eval-";

    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Sets the worker count for candidate evaluation; values below 1 are rejected. The default 1
     * reproduces the sequential behavior of the original; a value above 1 is the opt-in parallel
     * mode described in the class documentation.
     */
    public void setMaxThreads(int maxThreads) {
        if (maxThreads < 1) throw new IllegalArgumentException("maxThreads must be at least 1.");
        this.maxThreads = maxThreads;
    }

    public boolean isParallelEvaluation() {
        return maxThreads > 1;
    }

    /**
     * Runs {@code action} with a bounded evaluation pool when parallel evaluation is enabled.
     * Reentrant: nested calls reuse the pool opened by the outermost scope. The pool is always shut
     * down and awaited on exit, so no worker threads outlive the scope.
     */
    protected <R> R withEvaluationPool(Supplier<R> action) {
        if (maxThreads <= 1 || evaluationPool != null) {
            return action.get();
        }
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, EVALUATION_THREAD_PREFIX + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        ExecutorService pool = Executors.newFixedThreadPool(maxThreads, factory);
        evaluationPool = pool;
        try {
            return action.get();
        } finally {
            evaluationPool = null;
            pool.shutdownNow();
            try {
                pool.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Evaluates {@code count} candidates by index, concurrently when a pool is open, and returns the
     * results in index order. Callers must merge results in ascending index order so ties resolve
     * exactly as in the sequential loop. Exceptions from tasks propagate to the caller.
     */
    protected <R> List<R> evaluateAll(int count, IntFunction<R> evaluation) {
        ExecutorService pool = evaluationPool;
        if (maxThreads <= 1 || pool == null || count <= 1) {
            List<R> results = new ArrayList<R>(count);
            for (int i = 0; i < count; i++) {
                results.add(evaluation.apply(i));
            }
            return results;
        }
        List<Future<R>> futures = new ArrayList<Future<R>>(count);
        for (int i = 0; i < count; i++) {
            int index = i;
            futures.add(pool.submit(() -> evaluation.apply(index)));
        }
        List<R> results = new ArrayList<R>(count);
        for (int i = 0; i < count; i++) {
            try {
                results.add(futures.get(i).get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel candidate evaluation was interrupted.", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) throw runtimeException;
                if (cause instanceof Error error) throw error;
                throw new RuntimeException("Parallel candidate evaluation failed.", cause);
            }
        }
        return results;
    }

    /** One population valuation: the valuation object and the objective value it produces. */
    private record ValuationResult<TV>(TV valuation, float value) {
    }

    /** One direct-upgrade candidate; {@code found} is false when the candidate was invalid. */
    record UpgradeResult<TI, TV>(TI individual, TV valuation, float value, boolean found) {
    }

    protected abstract void reportProgress(int progressPercentage, float bestValue);

    private static final NetRandom randSeed = new NetRandom();

    private NetRandom rand;

    /**
     * Deterministic RNG seed for tests (Phase 7 verification). The port runs valuations
     * sequentially (single-thread/fixed order); setting a seed additionally removes the
     * time-seeded randomness so repeated runs of the same optimizer inputs are identical.
     */
    private Integer rndSeedOverride;

    /**
     * Pins the RNG seed, discarding any RNG previously created for this optimizer. With a seed set
     * and single-threaded evaluation, repeated runs over the same inputs are identical.
     */
    public void setRndSeed(int seed) {
        rndSeedOverride = seed;
        rand = null;
    }

    /**
     * Returns this run's RNG, creating it lazily from the pinned seed or from the shared,
     * time-seeded static generator. All draws must stay on the calling thread for the parallel
     * evaluation mode to remain seed-deterministic.
     */
    protected NetRandom getRnd() {
        NetRandom r = rand;
        if (r == null) {
            int seed;
            if (rndSeedOverride != null) {
                seed = rndSeedOverride;
            } else {
                synchronized (randSeed) {
                    seed = randSeed.next();
                }
            }
            rand = r = new NetRandom(seed);
        }
        return r;
    }

    /**
     * Runs the configured search without a caller-supplied individual.
     */
    protected OptimizationResult<TIndividual, TValuation> optimize() {
        return optimize(null, 0.0f);
    }

    /**
     * Runs the configured search with a caller-supplied individual whose score is computed first.
     */
    protected OptimizationResult<TIndividual, TValuation> optimize(TIndividual injectIndividual) {
        return optimize(injectIndividual, injectIndividual != null ? getOptimizationValue(injectIndividual) : 0.0f);
    }

    /**
     * Common entry point: opens the parallel evaluation scope when configured and reuses an
     * already-open pool for nested calls.
     */
    protected OptimizationResult<TIndividual, TValuation> optimize(TIndividual injectIndividual, float injectValue) {
        if (isParallelEvaluation() && evaluationPool == null) {
            return withEvaluationPool(() -> optimizeImpl(injectIndividual, injectValue));
        }
        return optimizeImpl(injectIndividual, injectValue);
    }

    private OptimizationResult<TIndividual, TValuation> optimizeImpl(TIndividual injectIndividual, float injectValue) {
        boolean oldVolatility = Item.isOptimizerManagedVolatiliy();
        try {
            Item.setOptimizerManagedVolatiliy(true);
            switch (optimizationMethod) {
                case GeneticAlgorithm:
                    return optimizeGA(injectIndividual, injectValue);
                case SimulatedAnnealing:
                    return optimizeSA();
            }
            return new OptimizationResult<>(null, 0.0f, null, false);
        } finally {
            Item.setOptimizerManagedVolatiliy(oldVolatility);
        }
    }

    /**
     * Optimization function based on simulated annealing
     * http://en.wikipedia.org/wiki/Simulated_annealing
     * Author: ebo
     */
    private OptimizationResult<TIndividual, TValuation> optimizeSA() {
        NetRandom rand = getRnd();

        TIndividual currentIndividual = buildRandomIndividual(null);
        double currentValue = getOptimizationValue(currentIndividual);

        TIndividual bestIndividual = currentIndividual;
        double bestValue = currentValue;

        int maxCycles = thoroughness * thoroughness;

        // http://research.microsoft.com/constraint-reasoning/workshops/autonomous-cp07/papers/2.pdf

        double temp = 10;
        double acceptRate = 0.5;
        double lamRate = 0.5;

        for (int cycle = 0; cycle < maxCycles; cycle++) {
            if (cancellationPending) {
                return new OptimizationResult<>(null, NO_SEARCH_SCORE, null, false);
            }
            reportProgress((int) Math.rint((float) cycle / ((float) (maxCycles / 100f))), (float) bestValue);

            // Generate new character
            TIndividual nextIndividual = generatorBuildSACharacter(currentIndividual);

            double nextValue = getOptimizationValue(nextIndividual);

            // Save best character
            if (nextValue > bestValue) {
                bestIndividual = nextIndividual;
                bestValue = nextValue;
            }

            if (nextValue > currentValue) {
                // Better solution. Accept move
                currentIndividual = nextIndividual;
                currentValue = nextValue;
                acceptRate = 1.0 / 500.0 * (499.0 * acceptRate + 1);
            } else {
                if (saAcceptance(currentValue, nextValue, temp, rand)) {
                    // accept move
                    currentIndividual = nextIndividual;
                    currentValue = nextValue;
                    acceptRate = 1.0 / 500.0 * (499.0 * acceptRate + 1);
                } else {
                    // reject move
                    acceptRate = 1.0 / 500.0 * (499.0 * acceptRate);
                }
            }

            // tune acceptRate
            double part = (double) cycle / maxCycles;
            if (part < 0.15) {
                lamRate = 0.44 + 0.56 * Math.pow(560, -cycle / (maxCycles * 0.15));
            } else if (part < 0.65) {
                lamRate = 0.44;
            } else {
                lamRate = 0.44 * Math.pow(440, -((double) cycle / (double) maxCycles - 0.65) / 0.35);
            }

            if (acceptRate > lamRate) {
                temp *= 0.999;
            } else {
                temp /= 0.999;
            }
        }

        return new OptimizationResult<>(postProcess(bestIndividual), (float) bestValue,
                getValuation(bestIndividual), false);
    }

    /**
     * Number of consecutive generations/cycles without a global-best improvement; the GA stops once
     * this reaches {@code thoroughness}. It is reset to zero by an improvement, by planting the
     * caller-supplied individual, and by a successful direct-upgrade pass.
     */
    protected int noImprove;

    /** Best objective value found in the current run. */
    protected float bestValue;

    /** Valuation belonging to {@link #bestValue}; null until a candidate is evaluated. */
    protected TValuation bestValuation;

    /** Winning individual found so far; a subclass may recycle this object between runs. */
    protected TIndividual bestIndividual;

    /** Current GA population (null outside a run). */
    protected TIndividual[] population;
    private float[] values;
    private int islandSize;
    private int popSize;
    private TIndividual[] individualIsland;
    private int[] islandNoImprove;
    private int islandStagnationLimit;
    private int islandCount;
    private TIndividual[] popCopy;
    private float[] share;

    /**
     * The genetic-algorithm loop described in the class documentation. The returned record carries
     * the best individual after {@link #postProcess}, the pre-hook score and valuation, and whether
     * the injected individual was planted; the individual is null when cancelled.
     */
    private OptimizationResult<TIndividual, TValuation> optimizeGA(TIndividual injectIndividual, float injectValue) {
        // Begin Genetic
        int i1, i2;
        float resultBestValue = NO_SEARCH_SCORE;
        TValuation resultBestValuation = null;
        boolean resultInjected = false;
        this.bestValue = resultBestValue;
        this.bestValuation = resultBestValuation;
        // verify inject individual
        if (injectIndividual != null && !isIndividualValid(injectIndividual)) {
            injectIndividual = null;
        }

        popSize = thoroughness;
        islandSize = 20;
        islandStagnationLimit = 50;
        islandCount = (popSize - 1) / islandSize + 1;
        int cycleLimit = thoroughness;
        population = newIndividualArray(popSize);
        popCopy = newIndividualArray(popSize);
        values = new float[popSize];
        float[] minIsland = new float[islandCount];
        float[] maxIsland = new float[islandCount];
        float[] bestIsland = new float[islandCount];
        individualIsland = newIndividualArray(islandCount);
        islandNoImprove = new int[islandCount];
        for (int i = 0; i < islandCount; i++) {
            bestIsland[i] = NO_SEARCH_SCORE;
        }
        share = new float[popSize];
        float s, sum;
        bestIndividual = null;
        NetRandom rand = getRnd();

        if (thoroughness > 1) {
            for (int i = 0; i < popSize; i++) {
                population[i] = buildRandomIndividual(null);
            }
        } else {
            bestIndividual = injectIndividual;
            if (bestIndividual == null) {
                bestIndividual = buildRandomIndividual(null);
            }
            this.bestValue = getOptimizationValue(bestIndividual);
        }

        noImprove = 0;
        while (noImprove < cycleLimit) {
            if (thoroughness > 1) {
                if (cancellationPending) {
                    population = null;
                    values = null;
                    return new OptimizationResult<>(null, resultBestValue, resultBestValuation, resultInjected);
                }
                reportProgress((int) Math.rint((float) noImprove / ((float) cycleLimit / 100f)), this.bestValue);

                for (int i = 0; i < islandCount; i++) {
                    minIsland[i] = MAX_SEARCH_SCORE;
                    maxIsland[i] = NO_SEARCH_SCORE;
                    islandNoImprove[i]++;
                }
                // minv = MAX_SEARCH_SCORE;
                // maxv = NO_SEARCH_SCORE;
                // Candidate valuations may run on the evaluation pool; the merge below stays in
                // ascending index order so ties and the recycled best individual match the
                // sequential loop exactly (see evaluateAll/DECISIONS #58).
                List<ValuationResult<TValuation>> evaluations = evaluateAll(popSize, i -> {
                    TValuation valuation = getValuation(population[i]);
                    return new ValuationResult<TValuation>(valuation, getOptimizationValue(population[i], valuation));
                });
                for (int i = 0; i < popSize; i++) {
                    ValuationResult<TValuation> evaluation = evaluations.get(i);
                    values[i] = evaluation.value();
                    if (values[i] > this.bestValue) {
                        this.bestValue = values[i];
                        this.bestValuation = evaluation.valuation();
                        bestIndividual = buildCopyIndividual(population[i], bestIndividual);
                        noImprove = -1;
                    }
                }
                for (int i = 0; i < popSize; i++) {
                    int island = i / islandSize;
                    if (values[i] < minIsland[island]) minIsland[island] = values[i];
                    if (values[i] > maxIsland[island]) {
                        maxIsland[island] = values[i];
                        individualIsland[island] = population[i];
                    }
                    if (values[i] > bestIsland[island]) {
                        bestIsland[island] = values[i];
                        islandNoImprove[island] = 0;
                    }
                }
                for (int island = 0; island < islandCount; island++) {
                    sum = 0;
                    // it seems a lot of values are huge negative due to missing constraints which can distort the values and make everything look the same
                    float delta = maxIsland[island] - minIsland[island];
                    for (int i = island * islandSize; i < Math.min(popSize, (island + 1) * islandSize); i++) {
                        float value = (float) (Math.pow((values[i] - minIsland[island]) / delta, 10.0) + 0.1);
                        sum += value;
                        share[i] = value;
                    }
                    if (sum == 0) {
                        for (int i = island * islandSize; i < Math.min(popSize, (island + 1) * islandSize); i++) {
                            share[i] = 1f / (Math.min(popSize, (island + 1) * islandSize) - island * islandSize);
                        }
                    } else {
                        for (int i = island * islandSize; i < Math.min(popSize, (island + 1) * islandSize); i++) {
                            share[i] /= sum;
                        }
                    }
                }
            }

            noImprove++;

            if (thoroughness > 1 && noImprove < cycleLimit) {
                TIndividual[] swap = population;
                population = popCopy;
                popCopy = swap;
                for (int i = 0; i < popSize; i++) {
                    int island = i / islandSize;
                    if (i % islandSize == 0) {
                        if (individualIsland[island] == null) {
                            population[i] = buildRandomIndividual(population[i]);
                        } else {
                            population[i] = buildCopyIndividual(individualIsland[island], population[i]);
                        }
                    } else if (rand.nextDouble() < 0.05d) {
                        // completely random
                        population[i] = buildRandomIndividual(population[i]);
                    } else if (rand.nextDouble() < 0.4d) {
                        int transplant = island;
                        if (islandNoImprove[island] > islandStagnationLimit) transplant = rand.next(islandCount);
                        // crossover
                        s = (float) rand.nextDouble();
                        sum = 0;
                        for (i1 = transplant * islandSize; i1 < Math.min(popSize, (transplant + 1) * islandSize) - 1; i1++) {
                            sum += share[i1];
                            if (sum >= s) break;
                        }
                        s = (float) rand.nextDouble();
                        sum = 0;
                        for (i2 = island * islandSize; i2 < Math.min(popSize, (island + 1) * islandSize) - 1; i2++) {
                            sum += share[i2];
                            if (sum >= s) break;
                        }
                        population[i] = buildChildIndividual(popCopy[i1], popCopy[i2], population[i]);
                    } else {
                        int transplant = island;
                        if (islandNoImprove[island] > islandStagnationLimit && rand.nextDouble() < 1.0 / islandSize) transplant = rand.next(islandCount);
                        // mutate
                        s = (float) rand.nextDouble();
                        sum = 0;
                        for (i1 = transplant * islandSize; i1 < Math.min(popSize, (transplant + 1) * islandSize) - 1; i1++) {
                            sum += share[i1];
                            if (sum >= s) break;
                        }
                        population[i] = buildMutantIndividual(popCopy[i1], population[i]);
                    }
                }
            } else if (thoroughness > 1 && injectIndividual != null && !resultInjected && injectValue > this.bestValue) {
                population[popSize - 1] = buildCopyIndividual(injectIndividual, population[popSize - 1]);
                noImprove = 0;
                resultInjected = true;
            } else {
                // last try, look for single direct upgrades
                lookForDirectItemUpgrades();
            }
        }

        resultBestValuation = this.bestValuation;
        resultBestValue = this.bestValue;
        TIndividual ret = bestIndividual;

        population = null;
        values = null;
        bestIndividual = null;
        this.bestValuation = null;
        individualIsland = null;
        islandNoImprove = null;
        popCopy = null;
        share = null;

        return new OptimizationResult<>(postProcess(ret), resultBestValue, resultBestValuation, resultInjected);
    }

    @SuppressWarnings("unchecked")
    private TIndividual[] newIndividualArray(int size) {
        return (TIndividual[]) new Object[size];
    }

    /**
     * Hook called once on the winning individual before it is returned, after {@link #bestValue}
     * and {@link #bestValuation} have been written. The default returns the argument unchanged;
     * subclasses sanitize the result here (for example normalizing gems or dropping a disabled
     * off-hand). A subclass that changes the individual must recompute values itself if needed,
     * because the recorded best value still belongs to the pre-hook individual.
     */
    protected TIndividual postProcess(TIndividual bestIndividual) {
        return bestIndividual;
    }

    /**
     * Greedy final pass over all slots: for each slot, ask the per-slot form for the best single
     * replacement and adopt it when it strictly improves {@link #bestValue}. On the first
     * improvement the "generations without improvement" counter resets, which lets the GA loop
     * continue when this is called as its terminating step. Requires {@link #slotItems} unless the
     * subclass overrides the per-slot form.
     */
    protected void lookForDirectItemUpgrades() {
        for (int slot = 0; slot < slotCount; slot++) {
            // bestIndividual could potentially be null here... need to validate.
            if (bestIndividual == null) {
                // error Do we want to do some special handling here?
            } else {
                DirectUpgradeResult<TIndividual, TValuation> result =
                        lookForDirectItemUpgrades(slotItems[slot], slot, this.bestValue, bestIndividual, null);
                if (result.bestValue() > this.bestValue) {
                    this.bestValue = result.bestValue();
                    this.bestValuation = result.valuation();
                    bestIndividual = result.individual();
                    noImprove = 0;
                    population[0] = bestIndividual;
                }
            }
        }
    }

    /**
     * Evaluates every candidate in {@code items} as a replacement for {@code slot} of
     * {@code bestIndividual} and returns a pair of the best strict improvement over
     * {@code best} and its individual, or {@code (NEGATIVE_INFINITY, null)} when nothing improves.
     * When {@code itemList} is null the individual's gene array is cloned; each candidate then gets
     * its own copy so parallel evaluation cannot alias, and the winner is chosen by ascending
     * candidate index exactly like the sequential loop. The result carries the best score, the
     * winning individual and that winner's valuation. Candidates that fail
     * {@link #isIndividualValid} are skipped.
     */
    protected DirectUpgradeResult<TIndividual, TValuation> lookForDirectItemUpgrades(List<TItem> items, int slot, float best, TIndividual bestIndividual, TItem[] itemList) {
        TValuation bestValuation = null;
        boolean foundUpgrade = false;
        if (itemList == null) {
            itemList = getItems(bestIndividual).clone();
        }
        TItem[] template = itemList;
        // Each candidate builds its own array so concurrent evaluations do not share mutation; the
        // merged result follows the sequential strict-inequality order by ascending item index.
        List<UpgradeResult<TIndividual, TValuation>> evaluations = evaluateAll(items.size(), i -> {
            TItem[] candidate = template.clone();
            candidate[slot] = items.get(i);
            if (!isIndividualValid(candidate)) {
                return new UpgradeResult<TIndividual, TValuation>(null, null, 0f, false);
            }
            TIndividual swappedIndividual = generateIndividual(candidate, false, null);
            TValuation valuation = getValuation(swappedIndividual);
            return new UpgradeResult<TIndividual, TValuation>(swappedIndividual, valuation,
                    getOptimizationValue(swappedIndividual, valuation), true);
        });
        for (int i = 0; i < items.size(); i++) {
            UpgradeResult<TIndividual, TValuation> evaluation = evaluations.get(i);
            if (evaluation.found() && evaluation.value() > best) {
                best = evaluation.value();
                bestValuation = evaluation.valuation();
                bestIndividual = evaluation.individual();
                foundUpgrade = true;
            }
        }
        if (foundUpgrade)
            return new DirectUpgradeResult<>(best, bestIndividual, bestValuation);
        return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
    }

    protected float getOptimizationValue(TIndividual individual) {
        return getOptimizationValue(individual, getValuation(individual));
    }

    /**
     * Uniformly picks one item from the slot's pool. The {@code items} context array (the genes
     * selected so far) is offered to subclasses that need it; this default ignores it. Returns null
     * when the slot has no candidates.
     */
    // items can be used as a context to limit the choices if not all in the list are valid
    protected TItem getRandomItem(int slot, TItem[] items) {
        List<TItem> list = slotItems[slot];
        int count = list.size();
        if (count == 0) return null;
        return list.get(getRnd().next(count));
    }

    protected boolean isIndividualValid(TItem[] items) {
        for (OptimizerRangeValidatorBase<TItem> validator : validators) {
            if (!validator.isValid(items)) return false;
        }
        return true;
    }

    protected boolean isIndividualValid(TIndividual individual) {
        return isIndividualValid(getItems(individual));
    }

    /**
     * Evaluates one individual by running the model's calculations. This is the expensive part of
     * the search; the resulting valuation is the input of the objective-value computation.
     */
    protected abstract TValuation getValuation(TIndividual individual);

    protected abstract float getOptimizationValue(TIndividual individual, TValuation valuation);

    protected abstract TItem getItem(TIndividual individual, int slot);

    /** The returned array will be treated as readonly. */
    protected abstract TItem[] getItems(TIndividual individual);

    /** If you need to store the array make a copy unless canUseArray is true, because it might be reused and modified otherwise. */
    protected abstract TIndividual generateIndividual(TItem[] items, boolean canUseArray, TIndividual recycledIndividual);

    /** Similar to getItems, but only return the array if it is readily available and can be reused. */
    protected TItem[] getRecycledItems(TIndividual recycledIndividual) {
        return null;
    }

    @FunctionalInterface
    protected interface GeneratorItemSelector<TItem> {
        TItem select(int slot, TItem[] items);
    }

    /**
     * Generates a valid individual by selecting each slot in order with {@code itemSelector} and
     * running the validators as their {@code endSlot} is reached. When a validator rejects, the
     * builder rewinds to that validator's {@code startSlot} and re-selects from there, so the
     * returned individual always satisfies {@link #validators}. Reuses the recycled individual's
     * gene array when the subclass provides one.
     */
    protected TIndividual generatorBuildIndividual(GeneratorItemSelector<TItem> itemSelector, TIndividual recycledIndividual) {
        TItem[] item = getRecycledItems(recycledIndividual);
        if (item == null) item = newItemArray(slotCount);
        int validatorIndex = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            item[slot] = itemSelector.select(slot, item);
            // check if we have to validate
            while (validatorIndex < validators.size() && validators.get(validatorIndex).endSlot == slot) {
                // validate
                if (!validators.get(validatorIndex).isValid(item)) {
                    // rewind
                    slot = validators.get(validatorIndex).startSlot - 1; // for will increment it
                    validatorIndex = -1;
                    for (int i = 0; i < validators.size(); i++) {
                        if (validators.get(i).endSlot > slot) {
                            validatorIndex = i;
                            break;
                        }
                    }
                    if (validatorIndex == -1) validatorIndex = validators.size();
                    break;
                } else {
                    validatorIndex++; // move to next validator
                }
            }
        }

        return generateIndividual(item, true, recycledIndividual);
    }

    @SuppressWarnings("unchecked")
    private TItem[] newItemArray(int size) {
        return (TItem[]) new Object[size];
    }

    /**
     * Copies an individual. Subclasses that support recycling return a refilled recycled object;
     * otherwise the very same reference is returned, which is safe only because such subclasses
     * never mutate an existing individual in place.
     */
    protected TIndividual buildCopyIndividual(TIndividual individual, TIndividual recycledIndividual) {
        if (supportsRecycling) {
            TItem[] item = getRecycledItems(recycledIndividual);
            if (item == null) item = newItemArray(slotCount);
            System.arraycopy(getItems(individual), 0, item, 0, slotCount);
            return generateIndividual(item, true, recycledIndividual);
        } else {
            return individual;
        }
    }

    /** Builds a random valid individual. */
    protected TIndividual buildRandomIndividual(TIndividual recycledIndividual) {
        return generatorBuildIndividual(this::getRandomItem, recycledIndividual);
    }

    /**
     * Crossover: each slot is taken from the father or the mother with equal probability and
     * re-validated while the individual is assembled. Subclasses override this to add domain rules
     * (for example the jewelcrafter-gem preservation in {@link ItemInstanceOptimizer}).
     */
    protected TIndividual buildChildIndividual(TIndividual father, TIndividual mother, TIndividual recycledIndividual) {
        return generatorBuildIndividual(
                (slot, items) -> getRnd().nextDouble() < 0.5d ? getItem(father, slot) : getItem(mother, slot),
                recycledIndividual);
    }

    /**
     * This function decides whether we take a new character or drop it.
     * Author: ebo
     */
    private boolean saAcceptance(double e, double enew, double T, NetRandom R) {
        // Always accept character if its better
        if (enew > e) {
            return true;
        } else {
            // Accept based on difference and temperature
            // higher temperature means bigger differences possible (or likely)
            // see http://en.wikipedia.org/wiki/Simulated_annealing
            double chance = Math.exp((enew - e) / T);
            return chance > R.nextDouble();
        }
    }

    /**
     * This function clones a character and changes one item and based on a probability one enchant.
     * Author: ebo
     */
    protected TIndividual generatorBuildSACharacter(TIndividual parent) {
        TItem[] item = newItemArray(slotCount);

        for (int slot = 0; slot < slotCount; slot++) {
            item[slot] = getItem(parent, slot);
        }
        NetRandom rand = getRnd();
        double r = rand.nextDouble();
        boolean successfull = false;

        r = rand.nextDouble();

        while (!successfull) {
            // Make sure to change one item
            // There are better methods to make sure to change one item (shuffled list of all slots) but this works
            int slot = rand.next(slotCount);
            TItem newitem = getRandomItem(slot, item);
            TItem oldItem = item[slot];
            item[slot] = newitem;
            if (isIndividualValid(item)) {
                successfull = true;
            } else {
                item[slot] = oldItem;
            }
        }

        return generateIndividual(item, true, null);
    }

    /**
     * Mutation: copies the parent and re-randomizes a fraction of its slots. The target number of
     * mutated slots starts at 2 and grows while a 0.75-probability coin keeps succeeding (capped at
     * 32); each slot is then re-randomized with probability {@code targetMutations / 32}, otherwise
     * it keeps the parent's item.
     */
    protected TIndividual buildMutantIndividual(TIndividual parent, TIndividual recycledIndividual) {
        NetRandom rand = getRnd();
        int targetMutations = 2;
        while (targetMutations < 32 && rand.nextDouble() < 0.75d) targetMutations++;
        double mutationChance = (double) targetMutations / 32d;

        return generatorBuildIndividual(
                (slot, items) -> {
                    if (getRnd().nextDouble() < mutationChance) {
                        return getRandomItem(slot, items);
                    } else {
                        return getItem(parent, slot);
                    }
                },
                recycledIndividual);
    }

    /**
     * Creates a copy of {@code baseIndividual} with one slot replaced. The per-item upgrade scans
     * use this to evaluate a locked candidate against the rest of the gear.
     */
    protected TIndividual buildSingleItemSwapIndividual(TIndividual baseIndividual, int replaceSlot, TItem replaceItem) {
        TItem[] item = newItemArray(slotCount);
        System.arraycopy(getItems(baseIndividual), 0, item, 0, slotCount);
        item[replaceSlot] = replaceItem;
        return generateIndividual(item, true, null);
    }
}
