package rawr.web;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterSlot;
import rawr.base.character.SavedTalentSpec;
import rawr.base.character.SavedTalentSpecList;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.optimizer.ItemInstanceOptimizer;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.optimizer.OptimizerCharacter;
import rawr.base.talents.TalentsBase;
import rawr.data.SavedTalentSpecStore;
import rawr.models.mage.CalculationOptionsMage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background optimizer runs for the web editor (AGENTS section 6, Phase 8).
 *
 * <p>A job owns a clone of the session character and runs the ported
 * {@link ItemInstanceOptimizer} inside {@link CalculationService#withCalculationLock}
 * (the engine keeps process-wide mutable state; see docs/dev/STATICS_AUDIT.md); the GA/search order is
 * always sequential and {@code maxThreads} only parallelizes candidate evaluations of the models on
 * {@link #PARALLEL_SAFE_MODELS} (index-ordered merges, DECISIONS #58), so those results stay
 * bit-identical to a sequential run. The item pool is built from the character's
 * {@code AvailableItems} list (or every model-relevant/filtered item when that list is empty), with
 * the currently equipped instances always kept as candidates; the GA uses {@link #JAVA_SEED}
 * (DECISIONS #48) so repeated runs on the same input are deterministic.
 *
 * <p>Runs are capped at one active job per session. Jobs execute on a small bounded executor; when
 * its queue is full {@link #start} rejects instead of piling up work. Progress comes from the
 * optimizer's {@code reportProgress} hook (subclass override, no rawr-core change) and cancellation
 * from its public {@code cancelAsync()} cooperative flag.
 */
@Service
public class OptimizerJobService {
    private static final Logger LOG = LoggerFactory.getLogger(OptimizerJobService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Phase 7 oracle parity: fixed Java GA seed (DECISIONS #48). */
    public static final int JAVA_SEED = 123456789;

    /**
     * Models whose candidate evaluations are verified deterministic under the parallel evaluation
     * pool (rawr-golden {@code OptimizerParallelDeterminismTest}, DECISIONS #58), the Java
     * counterpart of the original's {@code model.SupportsMultithreading} gate. Jobs for every other
     * model stay on the sequential path: Mage and RestoSham share model caches whose concurrent
     * population is not reproducible, so a parallel run is not guaranteed to match the sequential
     * score. Talent searches remain sequential for every model because talent genes are not part
     * of the parallel-safety verification.
     */
    public static final Set<String> PARALLEL_SAFE_MODELS = Set.of(
            "Bear", "Cat", "DPSDK", "DPSWarr", "Elemental", "Enhance", "Healadin",
            "HealPriest", "Hunter", "Moonkin", "ProtPaladin", "ProtWarr", "Retribution",
            "Rogue", "ShadowPriest", "TankDK", "Tree", "Warlock");
    /** 0 means no cap; the web uses the full custom-pool/relevant pool. */
    public static final int ITEMS_PER_SLOT = 0;
    public static final int MIN_THOROUGHNESS = 1;
    public static final int MAX_THOROUGHNESS = 400;
    public static final int DEFAULT_THOROUGHNESS = 150;
    /** The 17 gear slots offered in the optimizer panel (shirt/tabard/ammo excluded). */
    public static final List<CharacterSlot> OPTIMIZE_SLOTS = List.of(
            CharacterSlot.Head, CharacterSlot.Neck, CharacterSlot.Shoulders, CharacterSlot.Back,
            CharacterSlot.Chest, CharacterSlot.Wrist, CharacterSlot.Hands, CharacterSlot.Waist,
            CharacterSlot.Legs, CharacterSlot.Feet, CharacterSlot.Finger1, CharacterSlot.Finger2,
            CharacterSlot.Trinket1, CharacterSlot.Trinket2, CharacterSlot.MainHand,
            CharacterSlot.OffHand, CharacterSlot.Ranged);

    private static final int POOL_SIZE = 2;
    private static final int QUEUE_CAPACITY = 8;
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final long SSE_THROTTLE_NANOS = 100_000_000L;
    private static final int OPTIMIZER_CHARACTER_SLOTS = 19;

    private final CalculationService calculationService;
    private final EditorService editorService;
    private final SavedTalentSpecStore talentSpecStore;
    private final ExecutorService executor;
    private final Map<String, Job> jobsBySession = new ConcurrentHashMap<String, Job>();
    private final AtomicInteger activeJobs = new AtomicInteger();

    @org.springframework.beans.factory.annotation.Autowired
    public OptimizerJobService(
            CalculationService calculationService,
            EditorService editorService,
            SavedTalentSpecStore talentSpecStore) {
        this(calculationService, editorService, talentSpecStore, defaultExecutor());
    }

    OptimizerJobService(CalculationService calculationService, EditorService editorService,
            ExecutorService executor) {
        this(calculationService, editorService, null, executor);
    }

    OptimizerJobService(CalculationService calculationService, EditorService editorService) {
        this(calculationService, editorService, null, defaultExecutor());
    }

    OptimizerJobService(CalculationService calculationService, EditorService editorService,
            SavedTalentSpecStore talentSpecStore, ExecutorService executor) {
        this.calculationService = calculationService;
        this.editorService = editorService;
        this.talentSpecStore = talentSpecStore;
        this.executor = executor;
    }

    private static ExecutorService defaultExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "optimizer-job");
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------ job lifecycle

    /** Which availability universe the pool is built from. */
    public enum PoolMode {
        /** Use Character.AvailableItems when present; otherwise fall back to all relevant items. */
        CHARACTER_AVAILABILITY,
        /** Ignore the character's availability list and use every model-relevant/filtered item. */
        ALL_RELEVANT
    }

    /**
     * Optimizer parameters. {@code slots} are the slots the user chose to optimize and
     * {@code itemsPerSlot} is an optional per-slot cap (0 = no cap; tests use a small cap for
     * speed). {@code calculationToOptimize}/{@code requirements} are null when the values stored on
     * the character should be used. {@code maxThreads} opts into deterministic parallel candidate
     * evaluation (DECISIONS #58).
     */
    public record OptimizerParams(
            int thoroughness,
            List<CharacterSlot> slots,
            int itemsPerSlot,
            int maxThreads,
            String calculationToOptimize,
            List<OptimizationRequirement> requirements,
            boolean overrideRegem,
            boolean overrideReenchant,
            boolean templateGemsEnabled,
            boolean optimizeFood,
            boolean optimizeElixirs,
            boolean mixology,
            boolean poolAllRelevant,
            OptimizationMethod optimizationMethod,
            GreedyOptimizationMethod greedyOptimizationMethod,
            boolean optimizeTalents,
            boolean mutateTalents,
            boolean talentsOnly,
            boolean positiveCostItemsAvailable) {

        public OptimizerParams(int thoroughness, List<CharacterSlot> slots, int itemsPerSlot, int maxThreads) {
            this(thoroughness, slots, itemsPerSlot, maxThreads, null, null,
                    false, false, true, false, false, false, true,
                    OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                    false, false, false, false);
        }

        public OptimizerParams(int thoroughness, List<CharacterSlot> slots, int itemsPerSlot) {
            this(thoroughness, slots, itemsPerSlot, defaultMaxThreads());
        }

        public PoolMode poolMode() {
            return poolAllRelevant ? PoolMode.ALL_RELEVANT : PoolMode.CHARACTER_AVAILABILITY;
        }
    }

    /** One less than the available cores, capped at 4; never below 1 (DECISIONS #58). */
    public static int defaultMaxThreads() {
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));
    }

    /**
     * Starts a job for the session. At most one job may be active per session; a rejected start
     * throws {@link IllegalStateException} with a user-facing message.
     */
    public synchronized Job start(HttpSession session, Character character, OptimizerParams params) {
        String sessionId = session.getId();
        Job existing = jobsBySession.get(sessionId);
        if (existing != null && existing.isActive()) {
            throw new IllegalStateException("An optimizer run is already in progress for this character.");
        }
        validate(character, params);
        List<TalentsBase> talentSpecs = null;
        if (params.talentsOnly() && !params.optimizeTalents() && !params.mutateTalents()) {
            throw new IllegalStateException(
                    "Choose a talent mode for a talent-only run.");
        }
        if (params.optimizeTalents() || params.mutateTalents() || params.talentsOnly()) {
            Set<String> disabled = talentSpecStore == null
                    ? Set.of() : talentSpecStore.disabledKeys();
            talentSpecs = buildTalentSpecs(character, disabled);
            if (talentSpecs.isEmpty()) {
                throw new IllegalStateException(
                        "No valid 71-point talent specs are available for this class.");
            }
        }
        // C# optimizes the open character itself; the Java job clones for lock/session isolation, so
        // the gemming-template state must travel with the clone (the C# Clone() deliberately does not
        // copy it, but here the clone stands in for the original character). The optimizer's
        // AvailableItemGenerator reads CurrentGemmingTemplates/CustomGemmingTemplates off it.
        Character working = calculationService.withCalculationLock(() -> {
            Character clone = character.cloneForCalculation();
            // Character.clone() intentionally mirrors the C# clone and does not carry the
            // gemming templates/availability list; the web job works on a clone, so copy the
            // optimizer state here.
            copyOptimizerState(character, clone);
            // Give the job its own options object; the optimizer mutates model scratch state.
            ICalculationOptionBase sourceOptions = character.getCalculationOptions();
            if (sourceOptions != null) {
                try {
                    ICalculationOptionBase jobOptions = character.getCurrentCalculations()
                            .deserializeDataObject(sourceOptions.getXml());
                    if (jobOptions != null) {
                        if (jobOptions instanceof rawr.base.calculations.ICharacterCalculationOptions options) {
                            options.setCharacter(clone);
                        }
                        clone.setCalculationOptions(jobOptions);
                    }
                } catch (RuntimeException e) {
                    LOG.warn("Could not copy optimizer calculation options; using the character copy", e);
                }
            }
            return clone;
        });
        Job job = new Job(sessionId, session, character, working, params, character.getCurrentModel(), talentSpecs);
        jobsBySession.put(sessionId, job);
        activeJobs.incrementAndGet();
        try {
            executor.execute(job::run);
        } catch (RejectedExecutionException e) {
            jobsBySession.remove(sessionId);
            activeJobs.decrementAndGet();
            throw new IllegalStateException("The optimizer queue is full; try again in a moment.");
        }
        return job;
    }

    private void validate(Character character, OptimizerParams params) {
        String modelName = character.getCurrentModel();
        if (modelName == null || !CalculationService.MODELS.contains(modelName)) {
            throw new IllegalStateException("Choose a model before optimizing.");
        }
        if (!editorService.hasAnyItem(character)) {
            throw new IllegalStateException("Equip at least one item before optimizing.");
        }
        if (params.slots() == null || params.slots().isEmpty()) {
            throw new IllegalStateException("Select at least one slot to optimize.");
        }
        if (params.thoroughness() < MIN_THOROUGHNESS || params.thoroughness() > MAX_THOROUGHNESS) {
            throw new IllegalStateException("Thoroughness must be between " + MIN_THOROUGHNESS
                    + " and " + MAX_THOROUGHNESS + ".");
        }
        if (params.itemsPerSlot() < 0) {
            throw new IllegalStateException("The item pool per slot must not be negative.");
        }
        if (params.maxThreads() < 1) {
            throw new IllegalStateException("The optimizer thread count must be at least 1.");
        }
    }

    public Optional<Job> currentJob(HttpSession session) {
        return Optional.ofNullable(jobsBySession.get(session.getId()));
    }

    /** Requests cooperative cancellation of the session's active job; no-op when idle. */
    public boolean cancel(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        if (job == null || !job.isActive()) {
            return false;
        }
        job.cancel();
        return true;
    }

    /** Applies the last finished result after the user confirms it. */
    public boolean apply(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        return job != null && job.applyResult(session);
    }

    /** The finished result of the session's last job, or empty when there is no completed run. */
    public Optional<FinishedResult> finishedResult(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        if (job == null || !"done".equals(job.state) || job.optimized == null) {
            return Optional.empty();
        }
        return Optional.of(new FinishedResult(job.beforeSnapshot, job.optimized,
                job.beforePoints, job.afterPoints, job.changedSlots, job.calculation,
                job.slotDeltas, job.params));
    }

    /** Drops a finished job so the optimizer panel can start from a clean form. */
    public synchronized void dismissFinished(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        if (job != null && !job.isActive()) {
            jobsBySession.remove(session.getId());
        }
    }

    /**
     * Builds a character for the separate "Save optimized setup" file: the current character's
     * identity, options, talents and optimizer state, with the finished gear and active buffs from
     * the result. The session character is not modified.
     */
    public Character optimizedSaveCopy(HttpSession session, Character current) {
        Job job = jobsBySession.get(session.getId());
        if (job == null || !"done".equals(job.state) || job.optimized == null || current == null) {
            return null;
        }
        Character save = current.clone();
        copyOptimizerState(current, save);
        save.setCustomItemInstances(new ArrayList<>(current.getCustomItemInstances()));
        save.setItemFilterEnabledOverride(current.getItemFilterEnabledOverride());
        for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            save.set(slot, job.optimized.get(slot));
        }
        save.setActiveBuffs(new ArrayList<Buff>(job.optimized.getActiveBuffs()));
        TalentsBase optimizedTalents = job.optimized.getCurrentTalents();
        if (optimizedTalents != null) {
            save.setCurrentTalents(optimizedTalents.clone());
        }
        save.recalculateSetBonuses();
        return save;
    }

    /**
     * Valid candidate talent specs for the character: the current 71-point spec first, then the
     * shipped/user saved specs of the same class that spend exactly {@code level - 9} points.
     * Duplicate spec strings are removed. The first entry also supplies the talent annotations used
     * by mutation, so the current spec is the natural mutation base.
     */
    /**
     * Exact equipped gemmed ids for a talents-only run: every gear slot has exactly one candidate,
     * so the optimizer can only change the talent gene (gear, gems and enchants stay fixed).
     */
    static List<String> buildTalentOnlyPool(Character character) {
        List<String> pool = new ArrayList<String>();
        for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                pool.add(instance.getGemmedId());
            }
        }
        return pool;
    }

    static List<TalentsBase> buildTalentSpecs(Character character) {
        return buildTalentSpecs(character, Set.of());
    }

    static List<TalentsBase> buildTalentSpecs(Character character, Set<String> disabledKeys) {
        int expectedPoints = character.getLevel() - 9;
        List<TalentsBase> specs = new ArrayList<TalentsBase>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        addTalentSpec(specs, seen, character.getCurrentTalents(), expectedPoints);
        SavedTalentSpecList saved = SavedTalentSpec.getAllSpecs();
        if (saved != null) {
            for (SavedTalentSpec spec : saved) {
                if (spec.characterClass != character.getCharacterClass()) {
                    continue;
                }
                if (disabledKeys.contains(SavedTalentSpecStore.key(spec.characterClass, spec.spec))) {
                    continue;
                }
                try {
                    addTalentSpec(specs, seen, spec.talentSpec(), expectedPoints);
                } catch (RuntimeException e) {
                    LOG.warn("Skipping invalid saved talent spec {}", spec.name, e);
                }
            }
        }
        return specs;
    }

    private static void addTalentSpec(List<TalentsBase> specs, LinkedHashSet<String> seen,
            TalentsBase talents, int expectedPoints) {
        if (talents == null || talentPoints(talents) != expectedPoints) {
            return;
        }
        if (seen.add(talents.toString())) {
            specs.add(talents);
        }
    }

    private static int talentPoints(TalentsBase talents) {
        int points = 0;
        for (int value : talents.getData()) {
            points += value;
        }
        return points;
    }

    /** Copies the optimizer-related state that {@code Character.clone()} deliberately omits. */
    static void copyOptimizerState(Character source, Character target) {
        target.setCustomGemmingTemplates(new ArrayList<>(source.getCustomGemmingTemplates()));
        target.setGemmingTemplateOverrides(new ArrayList<>(source.getGemmingTemplateOverrides()));
        target.setAvailableItems(source.getAvailableItems() == null
                ? new ArrayList<>() : new ArrayList<>(source.getAvailableItems()));
        target.setCalculationToOptimize(source.getCalculationToOptimize());
        target.setOptimizationRequirements(source.getOptimizationRequirements());
        target.setWaistBlacksmithingSocketEnabled(source.getWaistBlacksmithingSocketEnabled());
        target.setHandsBlacksmithingSocketEnabled(source.getHandsBlacksmithingSocketEnabled());
        target.setWristBlacksmithingSocketEnabled(source.getWristBlacksmithingSocketEnabled());
    }

    /** Number of queued or running jobs (all sessions); used by tests and diagnostics. */
    public int activeJobCount() {
        return activeJobs.get();
    }

    // ------------------------------------------------------------------ SSE

    /** Attaches an SSE client to the session's job; completes immediately when there is none. */
    public void attach(HttpSession session, SseEmitter emitter) {
        Job job = jobsBySession.get(session.getId());
        if (job == null) {
            try {
                emitter.send(SseEmitter.event().name("done").data(toJson(JobStatus.idle()), MediaType.TEXT_PLAIN));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return;
        }
        job.addEmitter(emitter);
    }

    private static String toJson(JobStatus status) {
        try {
            return MAPPER.writeValueAsString(status);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize optimizer job status", e);
        }
    }
    // ------------------------------------------------------------------ pool

    /**
     * Builds the candidate pool. With {@link PoolMode#CHARACTER_AVAILABILITY} a non-empty
     * {@code Character.AvailableItems} list is used (filtered to the selected slots); otherwise
     * every model-relevant/filtered item that fits a selected slot is used. Equipped gemmed ids
     * are always kept so a run cannot silently discard current gear or its enchant. When
     * {@code overrideReenchant} is on, every model-relevant enchant is added in the negative
     * availability form understood by {@link Enchant#findEnchants}. {@code itemsPerSlot > 0} caps
     * only the fallback/relevant item list (a small deterministic pool for tests).
     */
    static List<String> buildPool(Character character, CalculationsBase model, OptimizerParams params) {
        List<CharacterSlot> slots = params.slots();
        Set<String> pool = new LinkedHashSet<String>();
        List<String> available = character.getAvailableItems();
        if (params.poolMode() == PoolMode.ALL_RELEVANT || available == null || available.isEmpty()) {
            addRelevantItems(character, model, slots, params.itemsPerSlot(), pool);
        } else {
            for (String entry : available) {
                if (availableEntryFits(character, entry, slots)) {
                    pool.add(entry);
                }
            }
        }
        if (!hasPositiveItemForSelectedSlot(character, pool, slots)) {
            addRelevantItems(character, model, slots, params.itemsPerSlot(), pool);
        }
        // Current gear (including its exact gems and enchant) is always a legal candidate. Only
        // selected slots also get the bare item id, so unselected slots keep their exact gemming
        // and enchant and cannot be changed by the search.
        for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                pool.add(instance.getGemmedId());
                if (slots.contains(slot)) {
                    pool.add(Integer.toString(instance.getId()));
                }
            }
        }
        if (params.overrideReenchant()) {
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id != 0 && model.isEnchantRelevant(enchant, character)) {
                    pool.add(Integer.toString(-1 * (enchant.id + (10000 * enchant.slot.ordinal()))));
                }
            }
        }
        return new ArrayList<String>(pool);
    }

    private static void addRelevantItems(Character character, CalculationsBase model,
            List<CharacterSlot> slots, int itemsPerSlot, Set<String> pool) {
        Item[] relevant = ItemCache.getRelevantItems(model, character.getRace());
        for (CharacterSlot slot : slots) {
            List<Item> candidates = new ArrayList<Item>();
            for (Item item : relevant) {
                if (item.fitsInSlot(slot, character)) {
                    candidates.add(item);
                }
            }
            candidates.sort(Comparator.comparingInt(Item::getItemLevel).reversed()
                    .thenComparingInt(Item::getId));
            int limit = itemsPerSlot > 0 ? Math.min(itemsPerSlot, candidates.size())
                    : candidates.size();
            for (int i = 0; i < limit; i++) {
                pool.add(Integer.toString(candidates.get(i).getId()));
            }
        }
    }

    /**
     * A positive entry is relevant when its item is a gem (the generator needs gem availability) or
     * fits one of the selected slots. Negative entries encode enchant availability and are kept.
     */
    private static boolean availableEntryFits(Character character, String entry, List<CharacterSlot> slots) {
        int end = entry.indexOf('.');
        String idText = end < 0 ? entry : entry.substring(0, end);
        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            return false;
        }
        if (id <= 0) {
            return true;
        }
        Item item = ItemCache.findItemById(id);
        if (item == null) {
            return true;
        }
        if (item.isGem()) {
            return true;
        }
        for (CharacterSlot slot : slots) {
            if (item.fitsInSlot(slot, character)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPositiveItemForSelectedSlot(Character character, Set<String> pool, List<CharacterSlot> slots) {
        for (String entry : pool) {
            if (entry.isEmpty() || entry.charAt(0) == '-') continue;
            int end = entry.indexOf('.');
            String idText = end < 0 ? entry : entry.substring(0, end);
            int id;
            try {
                id = Integer.parseInt(idText);
            } catch (NumberFormatException e) {
                continue;
            }
            Item item = ItemCache.findItemById(id);
            if (item == null || item.isGem()) continue;
            for (CharacterSlot slot : slots) {
                if (item.fitsInSlot(slot, character)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ status

    /** JSON-serializable progress snapshot; also the SSE payload. */
    public record JobStatus(
            String id,
            String state,
            boolean active,
            int progress,
            float beforePoints,
            float afterPoints,
            List<String> changedSlots,
            String error) {

        static JobStatus idle() {
            return new JobStatus("", "idle", false, 0, 0f, 0f, List.of(), null);
        }
    }

    /**
     * Immutable result snapshot for the result preview. {@code before} is the character as it was
     * when the job started; {@code optimized} is the winning gear set. The points are for the
     * calculation that was optimized.
     */
    public record FinishedResult(
            Character before,
            Character optimized,
            float beforePoints,
            float afterPoints,
            List<String> changedSlots,
            String calculation,
            Map<CharacterSlot, Float> slotDeltas,
            OptimizerParams params) {
    }

    // ------------------------------------------------------------------ job

    /** A single optimizer run. State is read by HTTP threads, written by the job thread. */
    public final class Job {
        private final String id = UUID.randomUUID().toString();
        private final String sessionId;
        private final OptimizerParams params;
        private final String modelName;
        private final Character source;
        private final Character working;
        private final Character beforeSnapshot;
        private final List<TalentsBase> talentSpecs;
        private final List<SseEmitter> emitters = new CopyOnWriteArrayList<SseEmitter>();

        private volatile HttpSession session;
        private volatile String state = "queued";
        private volatile int progress;
        private volatile float beforePoints;
        private volatile float afterPoints;
        private volatile List<String> changedSlots = List.of();
        private volatile Map<CharacterSlot, Float> slotDeltas = Map.of();
        private volatile String calculation;
        private volatile String error;
        private volatile boolean cancelRequested;
        private volatile ItemInstanceOptimizer optimizer;
        private volatile Character optimized;
        private volatile boolean applied;
        private volatile boolean finished;
        private long lastEventNanos;

        Job(String sessionId, HttpSession session, Character source, Character working,
                OptimizerParams params, String modelName, List<TalentsBase> talentSpecs) {
            this.sessionId = sessionId;
            this.session = session;
            this.source = source;
            this.working = working;
            this.beforeSnapshot = working.clone();
            this.params = params;
            this.modelName = modelName;
            this.talentSpecs = talentSpecs;
        }

        public String getId() {
            return id;
        }

        public boolean isActive() {
            return "queued".equals(state) || "running".equals(state);
        }

        public JobStatus status() {
            return new JobStatus(id, state, isActive(), progress, beforePoints, afterPoints, changedSlots, error);
        }

        public String getState() {
            return state;
        }

        public int getProgress() {
            return progress;
        }

        public float getBeforePoints() {
            return beforePoints;
        }

        public float getAfterPoints() {
            return afterPoints;
        }

        public String getError() {
            return error;
        }

        /** Cooperative cancel: sets the flag and tells the running optimizer to stop. */
        public void cancel() {
            cancelRequested = true;
            ItemInstanceOptimizer opt = optimizer;
            if (opt != null) {
                opt.cancelAsync();
            }
        }

        void addEmitter(SseEmitter emitter) {
            emitters.add(emitter);
            emitter.onCompletion(() -> emitters.remove(emitter));
            emitter.onTimeout(() -> {
                emitters.remove(emitter);
                emitter.complete();
            });
            emitter.onError(e -> emitters.remove(emitter));
            sendTo(emitter, "status", toJson(status()));
            if (finished) {
                sendTo(emitter, "done", toJson(status()));
                emitter.complete();
            }
        }

        void run() {
            try {
                if (cancelRequested) {
                    finish("cancelled", null);
                    return;
                }
                state = "running";
                publishStatus();
                calculationService.withCalculationLock(() -> {
                    runOptimization();
                    return null;
                });
            } catch (Throwable t) {
                LOG.warn("Optimizer job {} failed", id, t);
                finish("failed", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            }
        }

        private void runOptimization() {
            // A job gets a fresh model instance: the shared singleton keeps per-calculation
            // carry-over state that otherwise changes the next job's scores in this process.
            CalculationsBase model = Calculations.loadFreshModel(modelName);
            ICalculationOptionBase options = working.getCalculationOptions();
            if (options == null) {
                options = CalculationService.createDefaultOptions(modelName);
                working.setCalculationOptions(options);
            }
            CalculationOptionsMage mageOptions = options instanceof CalculationOptionsMage
                    ? (CalculationOptionsMage) options : null;
            Character previousMageCharacter = mageOptions != null ? mageOptions.getCharacter() : null;
            if (mageOptions != null) {
                mageOptions.setCharacter(working);
            }
            try {
                String calculation = params.calculationToOptimize();
                if (calculation == null || calculation.isBlank()) {
                    calculation = working.getCalculationToOptimize();
                }
                if (calculation == null || calculation.isBlank()) {
                    calculation = "[Overall]";
                }
                List<OptimizationRequirement> requirements = params.requirements() != null
                        ? params.requirements() : working.getOptimizationRequirements();
                if (requirements == null) {
                    requirements = List.of();
                }
                working.setCalculationToOptimize(calculation);
                working.setOptimizationRequirements(requirements);
                this.calculation = calculation;

                boolean talentSearch = params.optimizeTalents() || params.mutateTalents()
                        || params.talentsOnly();
                CharacterCalculationsBase beforeCalc = model.getCharacterCalculations(
                        working, null, false, talentSearch, false);
                beforePoints = ItemInstanceOptimizer.getOptimizationValue(working, beforeCalc, calculation, requirements);

                List<String> pool = params.talentsOnly()
                        ? buildTalentOnlyPool(working) : buildPool(working, model, params);
                ProgressReportingOptimizer opt = new ProgressReportingOptimizer(this);
                optimizer = opt;
                opt.optimizationMethod = params.optimizationMethod() != null
                        ? params.optimizationMethod() : OptimizationMethod.GeneticAlgorithm;
                opt.greedyOptimizationMethod = params.greedyOptimizationMethod() != null
                        ? params.greedyOptimizationMethod() : GreedyOptimizationMethod.AllCombinations;
                opt.setRndSeed(JAVA_SEED);
                // Talent genes are not part of the parallel-safety verification; keep runs
                // sequential when talents are in the search.
                opt.setMaxThreads(talentSearch ? 1
                        : (PARALLEL_SAFE_MODELS.contains(modelName) ? params.maxThreads() : 1));
                if (cancelRequested) {
                    opt.cancelAsync();
                }
                boolean overrideRegem = !params.talentsOnly() && params.overrideRegem();
                boolean overrideReenchant = !params.talentsOnly() && params.overrideReenchant();
                boolean templateGems = !params.talentsOnly() && params.templateGemsEnabled();
                boolean optimizeFood = !params.talentsOnly() && params.optimizeFood();
                boolean optimizeElixirs = !params.talentsOnly() && params.optimizeElixirs();
                boolean mixology = !params.talentsOnly() && params.mixology();
                opt.initializeItemCache(working, pool, overrideRegem, overrideReenchant,
                        templateGems, model, optimizeFood, optimizeElixirs,
                        mixology, talentSpecs, params.mutateTalents(),
                        params.positiveCostItemsAvailable());
                Character best = opt.optimizeCharacter(working, calculation, requirements,
                        params.thoroughness(), false);
                if (best == null || cancelRequested) {
                    finish("cancelled", null);
                    return;
                }
                if (params.talentsOnly()) {
                    // Paired slots (rings/trinkets) can appear in both slot pools even when only
                    // exact equipped instances are available. Put the original gear back so a
                    // talents-only run cannot reorder or change items; only the talent gene moves.
                    for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
                        CharacterSlot slot = CharacterSlot.fromValue(i);
                        best.set(slot, working.get(slot));
                    }
                    best.recalculateSetBonuses();
                }
                CharacterCalculationsBase afterCalc = model.getCharacterCalculations(
                        best, null, false, talentSearch, false);
                afterPoints = ItemInstanceOptimizer.getOptimizationValue(best, afterCalc, calculation, requirements);
                optimized = best;
                changedSlots = changedSlots(source, best);
                slotDeltas = computeSlotDeltas(beforeSnapshot, best, calculation, requirements, model, talentSearch);
                finish("done", null);
            } finally {
                optimizer = null;
                if (mageOptions != null && mageOptions.getCharacter() != previousMageCharacter) {
                    mageOptions.setCharacter(previousMageCharacter);
                }
            }
        }

        /** Applies the finished result to the session character when the user confirms it. */
        synchronized boolean applyResult(HttpSession currentSession) {
            if (!"done".equals(state) || optimized == null || applied) {
                return false;
            }
            Character current = currentSession == null ? null
                    : (Character) currentSession.getAttribute(EditorService.SESSION_CHARACTER);
            if (current != source) {
                return false;
            }
            for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
                CharacterSlot slot = CharacterSlot.fromValue(i);
                if (!sameInstance(source.get(slot), optimized.get(slot))) {
                    source.set(slot, optimized.get(slot));
                }
            }
            source.setActiveBuffs(new ArrayList<Buff>(optimized.getActiveBuffs()));
            TalentsBase sourceTalents = source.getCurrentTalents();
            TalentsBase optimizedTalents = optimized.getCurrentTalents();
            if (optimizedTalents != null && (sourceTalents == null
                    || !sourceTalents.toString().equals(optimizedTalents.toString()))) {
                source.setCurrentTalents(optimizedTalents.clone());
            }
            source.recalculateSetBonuses();
            editorService.markDirty(currentSession);
            applied = true;
            return true;
        }

        /**
         * Marginal value of each changed slot: the before character with that one after-item
         * installed, scored against the original before value. Buff changes are not attributed to
         * a slot, so these values are informative, not additive.
         */
        private Map<CharacterSlot, Float> computeSlotDeltas(Character before, Character after,
                String calculation, List<OptimizationRequirement> requirements, CalculationsBase model,
                boolean significantChange) {
            Map<CharacterSlot, Float> deltas = new LinkedHashMap<CharacterSlot, Float>();
            if (before == null || after == null) {
                return deltas;
            }
            for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
                CharacterSlot slot = CharacterSlot.fromValue(i);
                if (sameInstance(before.get(slot), after.get(slot))) {
                    continue;
                }
                try {
                    // Replace the final slot with the old item: the difference to the final score is
                    // that slot's contribution in the selected setup (talents included).
                    Character candidate = after.clone();
                    candidate.set(slot, before.get(slot));
                    CharacterCalculationsBase calc = model.getCharacterCalculations(
                            candidate, null, false, significantChange, false);
                    float value = ItemInstanceOptimizer.getOptimizationValue(
                            candidate, calc, calculation, requirements);
                    deltas.put(slot, afterPoints - value);
                } catch (RuntimeException e) {
                    LOG.warn("Could not compute the value delta for slot {}", slot, e);
                }
            }
            return deltas;
        }

        private static List<String> changedSlots(Character source, Character optimized) {
            List<String> changed = new ArrayList<String>();
            for (int i = 0; i < OPTIMIZER_CHARACTER_SLOTS; i++) {
                CharacterSlot slot = CharacterSlot.fromValue(i);
                if (!sameInstance(source.get(slot), optimized.get(slot))) {
                    changed.add(EditorService.slotLabel(slot));
                }
            }
            return changed;
        }

        private static boolean sameInstance(ItemInstance a, ItemInstance b) {
            if (a == null || b == null) {
                return a == b;
            }
            return a.getId() == b.getId()
                    && a.getGem1Id() == b.getGem1Id()
                    && a.getGem2Id() == b.getGem2Id()
                    && a.getGem3Id() == b.getGem3Id()
                    && a.getEnchantId() == b.getEnchantId();
        }

        private void onProgress(int percentage, float value) {
            int clamped = Math.max(0, Math.min(100, percentage));
            if (clamped > progress) {
                progress = clamped;
            }
            long now = System.nanoTime();
            if (now - lastEventNanos >= SSE_THROTTLE_NANOS) {
                lastEventNanos = now;
                publishStatus();
            }
        }

        private void publishStatus() {
            if (emitters.isEmpty()) {
                return;
            }
            String json = toJson(status());
            for (SseEmitter emitter : emitters) {
                sendTo(emitter, "status", json);
            }
        }

        private void sendTo(SseEmitter emitter, String event, String json) {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event().name(event).data(json, MediaType.TEXT_PLAIN));
                }
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }

        private synchronized void finish(String newState, String failure) {
            if (finished) {
                return;
            }
            finished = true;
            if ("done".equals(newState)) {
                progress = 100;
            }
            state = newState;
            error = failure;
            session = null;
            activeJobs.decrementAndGet();
            String json = toJson(status());
            for (SseEmitter emitter : emitters) {
                sendTo(emitter, "done", json);
                emitter.complete();
            }
            emitters.clear();
        }

        /** Session key (used by tests asserting per-session cleanup). */
        String getSessionId() {
            return sessionId;
        }
    }

    /** Reports optimizer progress into the job (subclass override; no rawr-core hook needed). */
    private static final class ProgressReportingOptimizer extends ItemInstanceOptimizer {
        private final Job job;
        private volatile boolean cancelRequested;
        private final boolean talentsOnly;

        ProgressReportingOptimizer(Job job) {
            this.talentsOnly = job.params.talentsOnly();
            this.job = job;
        }

        @Override
        public void cancelAsync() {
            // optimizeCharacter() clears OptimizerBase.cancellationPending when the run starts; keep our
            // own flag so a cancel during item-cache initialization survives and is re-asserted below.
            cancelRequested = true;
            super.cancelAsync();
        }

        @Override
        protected OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> optimize(
                OptimizerCharacter injectIndividual, float injectValue) {
            if (cancelRequested) {
                super.cancelAsync();
            }
            return super.optimize(injectIndividual, injectValue);
        }

        @Override
        protected OptimizerCharacter postProcess(OptimizerCharacter bestIndividual) {
            // A talents-only run must not normalize/rearrange the fixed gear.
            return talentsOnly ? bestIndividual : super.postProcess(bestIndividual);
        }

        @Override
        protected void reportProgress(int progressPercentage, float bestValue) {
            job.onProgress(progressPercentage, bestValue);
        }
    }
}
