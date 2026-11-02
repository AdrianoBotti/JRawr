// New documentation (not ported from C#): package-level guide.
/**
 * Model-agnostic numerical algorithms shared by the simulation models.
 *
 * <p>Nothing here knows about World of Warcraft. These are general-purpose building blocks that a
 * model can use when its combat logic is better expressed as linear algebra or as a random process
 * than as a hand-written formula.</p>
 *
 * <h2>{@link rawr.base.algorithms.LU} &mdash; solving systems of linear equations</h2>
 *
 * <p>A huge part of combat modelling ends up as a system of linear equations: "the total time spent
 * in each state equals the time we arrive in that state", or "the total damage of an ability equals
 * the damage of its direct hit plus the damage of what it triggers". Writing such a system as a
 * matrix equation {@code A * x = b} lets a model solve every unknown at once instead of hand-rolling
 * recursive formulas.</p>
 *
 * <p>{@code LU} factors a square matrix into a lower-triangular matrix {@code L} and an
 * upper-triangular matrix {@code U}, with row/column permutations for stability (the sparse
 * partial-pivoting "LU decomposition with update" from the original). Once factored, any number of
 * right-hand sides can be solved cheaply by forward/backward substitution
 * ({@link rawr.base.algorithms.LU#fSolve(double[])}, {@link rawr.base.algorithms.LU#bSolve(double[])}).
 * The matrix is stored sparsely (only non-zeros are kept, in a column-compressed-like layout), which
 * matters because rotation state spaces are large but very sparse.</p>
 *
 * <p>Two API styles exist for historical reasons: the normal construction path
 * ({@code new LU(size)}, mutate via {@code beginSafe()}/{@code decompose()}) and the "unsafe" path
 * ({@link rawr.base.algorithms.LU#beginUnsafe(double[], double[], int[], int[], int[], int[], int[], double[], double[])})
 * used by {@link rawr.base.algorithms.MarkovProcess}, where the caller passes in the underlying
 * arrays from the pool so that no garbage is produced in hot loops. The unsafe path is a
 * performance port of C# {@code fixed} pointers; it must be paired with
 * {@link rawr.base.algorithms.LU#endUnsafe()} before the arrays are released.</p>
 *
 * <h2>{@link rawr.base.algorithms.MarkovProcess} &mdash; averaging a random rotation</h2>
 *
 * <p>Some builds (classes with unpredictable proc-based rotations) are best modelled as a
 * <b>Markov process</b>: a system that jumps between a finite set of <i>states</i> (for example
 * "no proc active", "one proc active", "two procs active"). From each state, fixed-probability
 * <i>transitions</i> lead to other states; each transition is associated with an ability and a
 * duration.</p>
 *
 * <p>Given such a state space, {@code MarkovProcess} computes the long-run fraction of time spent
 * in each state (the <i>stationary distribution</i>) by solving one linear system, then uses those
 * weights to produce the average ability frequencies and the average transition duration over a
 * fight. In game terms: it answers "how often, on average, does this rotation cast each ability?"
 * without simulating every swing.</p>
 *
 * <p>Input is built by a {@link rawr.base.algorithms.StateSpaceGenerator} subclass (the model
 * implements {@link rawr.base.algorithms.StateSpaceGenerator#getInitialState()} and
 * {@link rawr.base.algorithms.StateSpaceGenerator#getStateTransitions(rawr.base.algorithms.State)},
 * and the base class walks the graph to enumerate every reachable
 * {@link rawr.base.algorithms.State}). A {@link rawr.base.algorithms.StateTransition} carries the
 * target state, the ability it represents, its duration and its probability.</p>
 *
 * <h2>{@link rawr.base.algorithms.ArrayPool} &mdash; reusing scratch memory</h2>
 *
 * <p>{@code LU} needs several sizable {@code double[]} and {@code int[]} arrays. Allocating them per
 * solve would dominate the cost when the optimizer runs thousands of simulations, so
 * {@link rawr.base.algorithms.LU.ArraySet} bundles them and {@code ArrayPool} lends sets out.
 * {@link rawr.base.algorithms.ArrayPool#requestArraySet()} blocks when the pool is empty (the
 * original allows at most {@code maximumPoolSize} sets to exist); every requester must return its
 * set with {@link rawr.base.algorithms.ArrayPool#releaseArraySet(rawr.base.algorithms.LU.ArraySet)}
 * in a {@code finally}-style discipline. The pool is the reason the optimizer supports a bounded
 * number of concurrent evaluation threads &mdash; more borrowers than sets simply wait.</p>
 *
 * <h2>{@link rawr.base.algorithms.CalculationCache} &mdash; compute-once memoization</h2>
 *
 * <p>A generic, thread-safe memoizer: wrap a pure calculation
 * ({@link rawr.base.algorithms.CalculationCache.Calculation}) and results are cached per parameter
 * set. Models use it when the same expensive sub-calculation (e.g. a spell's damage for a given
 * stat snapshot) is requested repeatedly during one evaluation. The cache is synchronized, so it
 * also serializes concurrent first-computations.</p>
 */
package rawr.base.algorithms;
