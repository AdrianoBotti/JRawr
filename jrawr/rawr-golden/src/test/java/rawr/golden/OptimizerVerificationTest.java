package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.items.Enchant;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.items.ItemCache;
import rawr.base.optimizer.ItemInstanceOptimizer;
import rawr.base.calculations.OptimizationMethod;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.models.mage.CalculationOptionsMage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7 optimizer verification: for the fixed item pool recorded by the C# oracle
 * ({@code oracle/golden/optimizer/<fixture>.json}), the Java optimizer must reach at least the C#
 * best score minus the documented tolerance. The chosen item set is deliberately not compared
 * (search order and tie-breaking differ); only the objective value is.
 *
 * <p>Covers all 20 ported models (one representative fixture each; two or more for the original
 * pilot models). The model is read from the golden JSON and dispatched to the matching
 * {@code Calculations<Model>} class by reflection; the runtime setup (item/enchant/buff caches) is
 * the same for every model.
 *
 * <p>The tag is excluded from the default {@code test} task and runs one fixture per fresh JVM via
 * the {@code optimizerGolden_<fixture>} tasks (the C# oracle dumped one fixture per process).
 */
@Tag("optimizer")
class OptimizerVerificationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path repoRoot;
    private static Path goldenDir;

    @BeforeAll
    static void setUp() throws Exception {
        repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        goldenDir = repoRoot.resolve("oracle/golden/optimizer");

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
        Path root = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        try (Stream<Path> files = Files.list(root.resolve("oracle/golden/optimizer"))) {
            return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 5))
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

    private static String fixtureDirFor(String model) {
        return packageFor(model);
    }

    @SuppressWarnings("unchecked")
    private static CalculationsBase registerAndLoadModel(String model) {
        String packageName = packageFor(model);
        if (packageName == null) {
            throw new IllegalArgumentException("optimizer verification: no Java optimizer wiring for model " + model);
        }
        try {
            Class<? extends CalculationsBase> type = (Class<? extends CalculationsBase>) Class.forName(
                    "rawr.models." + packageName + ".Calculations" + model);
            Calculations.registerModel(model, type);
            Calculations.loadModel(type);
            return Calculations.getInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("optimizer verification: cannot load " + model, e);
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
                throw new IllegalArgumentException("optimizer verification: cannot create options for " + model, e);
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
    void javaOptimizerNotWorseThanCsharp(String fixture) throws Exception {
        long t0 = System.nanoTime();
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());
        String modelName = root.get("model").asText();
        String fixtureDir = fixtureDirFor(modelName);
        assertThat(fixtureDir).as("%s: model %s has Java optimizer wiring", fixture, modelName).isNotNull();
        CalculationsBase calc = registerAndLoadModel(modelName);

        JsonNode driving = root.get("driving");
        int thoroughness = driving.get("thoroughness").asInt();
        int javaSeed = driving.get("javaSeed").asInt();
        String calculationToOptimize = driving.get("calculationToOptimize").asText();
        boolean templateGemsEnabled = driving.get("templateGemsEnabled").asBoolean();

        Character character = CharacterFixtureLoader.load(
                repoRoot.resolve("oracle/fixtures/" + fixtureDir + "/" + fixture + ".xml"));
        ensureCalculationOptions(character, modelName);
        character.recalculateSetBonuses();

        List<String> availableItems = new ArrayList<>();
        for (Iterator<JsonNode> it = root.get("pool").get("availableItemIds").elements(); it.hasNext(); ) {
            availableItems.add(Integer.toString(it.next().asInt()));
        }
        assertThat(availableItems).as("%s: pool size", fixture).isNotEmpty();

        ItemInstanceOptimizer optimizer = new ItemInstanceOptimizer();
        optimizer.optimizationMethod = OptimizationMethod.GeneticAlgorithm;
        optimizer.greedyOptimizationMethod = GreedyOptimizationMethod.AllCombinations;
        optimizer.setRndSeed(javaSeed);
        optimizer.initializeItemCache(character, availableItems, false, false, templateGemsEnabled, calc,
                false, false, false, null, false);

        Character optimized = optimizer.optimizeCharacter(character, calculationToOptimize,
                new ArrayList<>(), thoroughness, false);
        assertThat(optimized).as("%s: optimized character", fixture).isNotNull();

        CharacterCalculationsBase ret = calc.getCharacterCalculations(optimized, null, false, false, false);
        float javaBest = ItemInstanceOptimizer.getOptimizationValue(optimized, ret, calculationToOptimize,
                new ArrayList<>());

        double csharpBest = root.get("csharp").get("bestOverallPoints").asDouble();
        double relative = driving.get("tolerance").get("relative").asDouble();
        double absoluteFloor = driving.get("tolerance").get("absoluteFloor").asDouble();
        double tolerance = Math.max(absoluteFloor, Math.abs(csharpBest) * relative);

        System.out.println("[optimizer] " + modelName + "/" + fixture + " csharp=" + csharpBest + " java=" + javaBest
                + " tolerance=" + tolerance + " in " + ((System.nanoTime() - t0) / 1_000_000) + " ms");
        assertThat((double) javaBest)
                .as("%s: java optimizer best score must be at least the %s best minus tolerance", fixture, modelName)
                .isGreaterThanOrEqualTo(csharpBest - tolerance);
    }
}
