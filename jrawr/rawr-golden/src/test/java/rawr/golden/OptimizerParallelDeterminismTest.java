package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.optimizer.ItemInstanceOptimizer;
import rawr.base.optimizer.OptimizerBase;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.models.mage.CalculationOptionsMage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DECISIONS #58: opt-in parallel candidate evaluation must be bit-identical to sequential for every
 * model it is enabled on. One fixture per fresh JVM runs the same recorded seed and a smaller
 * recorded pool sequentially (threads = 1) and in parallel (threads = 3); the chosen items and the
 * objective score must match bit for bit, parallel batches are counted (so the test cannot silently
 * fall back) and no evaluation worker thread may outlive the run.
 *
 * <p>A sequential warm-up run precedes the two compared runs: models still carry first-run-per-JVM
 * state independent of threading (the reason golden fixtures run one per JVM, DECISIONS #23), and
 * the warm-up keeps this test about parallel evaluation only.
 */
@Tag("optimizer-parallel")
class OptimizerParallelDeterminismTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int THOROUGHNESS = 10;
    private static final int SMALL_ITEMS_PER_SLOT = 3;
    private static final int CHARACTER_SLOTS = 19;
    private static final List<String> DEFAULT_FIXTURES = List.of("dpsdk_unholy_277",
            "protwarr_277_raid");

    private static Path repoRoot;
    private static Path goldenDir;

    @BeforeAll
    static void setUp() throws Exception {
        repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        goldenDir = repoRoot.resolve("oracle/golden/optimizer");
        Path gameData = repoRoot.resolve("jrawr/data");

        ItemCache.clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        Enchant.getAllEnchants().clear();
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
    }

    static List<String> fixtureNames() throws Exception {
        String only = System.getProperty("jrawr.fixture", "");
        if (!only.isEmpty()) {
            return List.of(only.split(","));
        }
        try (Stream<Path> files = Files.list(goldenDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().substring(0,
                            p.getFileName().toString().length() - 5))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String packageFor(String model) {
        switch (model) {
            case "Bear": return "bear";
            case "Cat": return "cat";
            case "DPSDK": return "dpsdk";
            case "DPSWarr": return "dpswarr";
            case "Elemental": return "elemental";
            case "Enhance": return "enhance";
            case "Healadin": return "healadin";
            case "HealPriest": return "healpriest";
            case "Hunter": return "hunter";
            case "Mage": return "mage";
            case "Moonkin": return "moonkin";
            case "ProtPaladin": return "protpally";
            case "ProtWarr": return "protwarr";
            case "RestoSham": return "restosham";
            case "Retribution": return "retribution";
            case "Rogue": return "rogue";
            case "ShadowPriest": return "shadowpriest";
            case "TankDK": return "tankdk";
            case "Tree": return "tree";
            case "Warlock": return "warlock";
            default: return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static CalculationsBase registerAndLoadModel(String model) {
        String packageName = packageFor(model);
        if (packageName == null) {
            throw new IllegalArgumentException("parallel determinism: no Java optimizer wiring for model " + model);
        }
        try {
            Class<? extends CalculationsBase> type = (Class<? extends CalculationsBase>) Class.forName(
                    "rawr.models." + packageName + ".Calculations" + model);
            Calculations.registerModel(model, type);
            Calculations.loadModel(type);
            return Calculations.getInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("parallel determinism: cannot load " + model, e);
        }
    }

    private static void ensureCalculationOptions(Character character, String model) {
        ICalculationOptionBase current = character.getCalculationOptions();
        if (current == null) {
            String packageName = packageFor(model);
            try {
                Class<?> optionType = Class.forName("rawr.models." + packageName
                        + ".CalculationOptions" + model);
                current = (ICalculationOptionBase) optionType.getDeclaredConstructor().newInstance();
                character.setCalculationOptions(current);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("parallel determinism: cannot create options for " + model, e);
            }
        }
        if (current instanceof ICharacterCalculationOptions characterOptions) {
            characterOptions.setCharacter(character);
        }
        if (current instanceof CalculationOptionsMage mageOptions) {
            mageOptions.setCharacter(character);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureNames")
    void parallelMatchesSequentialBitForBit(String fixture) throws Exception {
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());
        String modelName = root.get("model").asText();
        String fixtureDir = packageFor(modelName);
        assertThat(fixtureDir).as("%s: model %s has Java optimizer wiring", fixture, modelName).isNotNull();
        CalculationsBase model = registerAndLoadModel(modelName);

        // First-run-per-JVM model state (DECISIONS #23): normalize before the compared runs.
        run(fixture, fixtureDir, model, modelName, root, 1);

        Run sequential = run(fixture, fixtureDir, model, modelName, root, 1);
        Run parallel = run(fixture, fixtureDir, model, modelName, root, 3);

        assertThat(parallel.parallelBatches).as("%s: parallel batches engaged", fixture).isPositive();
        assertThat(parallel.items).as("%s: chosen items", fixture).isEqualTo(sequential.items);
        assertThat(Float.floatToIntBits(parallel.score))
                .as("%s: score bits (sequential %s, parallel %s)", fixture, sequential.score, parallel.score)
                .isEqualTo(Float.floatToIntBits(sequential.score));
        assertThat(liveEvaluationThreads()).as("%s: evaluation threads released", fixture).isZero();
    }

    private static Run run(String fixture, String fixtureDir, CalculationsBase model, String modelName,
            JsonNode root, int threads) throws Exception {
        int seed = root.get("driving").get("javaSeed").asInt();
        Character character = CharacterFixtureLoader.load(
                repoRoot.resolve("oracle/fixtures/" + fixtureDir + "/" + fixture + ".xml"));
        ensureCalculationOptions(character, modelName);
        character.recalculateSetBonuses();

        CountingOptimizer optimizer = new CountingOptimizer();
        optimizer.optimizationMethod = OptimizationMethod.GeneticAlgorithm;
        optimizer.greedyOptimizationMethod = GreedyOptimizationMethod.AllCombinations;
        optimizer.setRndSeed(seed);
        optimizer.setMaxThreads(threads);
        optimizer.initializeItemCache(character, smallPool(root, character), false, false, true, model,
                false, false, false, null, false);

        Character optimized = optimizer.optimizeCharacter(character, "[Overall]", new ArrayList<>(),
                THOROUGHNESS, false);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < CHARACTER_SLOTS; i++) {
            ItemInstance item = optimized.get(CharacterSlot.fromValue(i));
            items.add(item == null ? "empty" : item.getGemmedId() + "/" + item.getEnchantId());
        }
        return new Run(optimizer.getBest(), items, optimizer.parallelBatches.get());
    }

    /** The first few recorded per-slot items plus every equipped item; keeps the runs small. */
    private static List<String> smallPool(JsonNode root, Character character) {
        Set<Integer> ids = new LinkedHashSet<>();
        JsonNode slots = root.get("pool").get("slots");
        for (Iterator<JsonNode> slotIt = slots.elements(); slotIt.hasNext(); ) {
            JsonNode slotItems = slotIt.next();
            for (int i = 0; i < slotItems.size() && i < SMALL_ITEMS_PER_SLOT; i++) {
                ids.add(slotItems.get(i).asInt());
            }
        }
        for (int i = 0; i < CHARACTER_SLOTS; i++) {
            ItemInstance equipped = character.get(CharacterSlot.fromValue(i));
            if (equipped != null && equipped.getItem() != null) {
                ids.add(equipped.getId());
            }
        }
        List<String> pool = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            pool.add(Integer.toString(id));
        }
        return pool;
    }

    private static long liveEvaluationThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith(OptimizerBase.EVALUATION_THREAD_PREFIX))
                .count();
    }

    private record Run(float score, List<String> items, int parallelBatches) {
    }

    /** Counts parallel evaluation batches so the test cannot silently fall back to sequential. */
    private static final class CountingOptimizer extends ItemInstanceOptimizer {
        final AtomicInteger parallelBatches = new AtomicInteger();

        float getBest() {
            return bestValue;
        }

        @Override
        protected <R> List<R> evaluateAll(int count, IntFunction<R> evaluation) {
            if (isParallelEvaluation() && count > 1) {
                parallelBatches.incrementAndGet();
            }
            return super.evaluateAll(count, evaluation);
        }
    }
}
