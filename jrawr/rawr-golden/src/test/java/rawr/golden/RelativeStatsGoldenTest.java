package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.character.Character;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.models.cat.CalculationOptionsCat;
import rawr.models.cat.CalculationsCat;
import rawr.models.enhance.CalculationOptionsEnhance;
import rawr.models.enhance.CalculationsEnhance;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.CalculationsMage;
import rawr.models.tankdk.CalculationOptionsTankDK;
import rawr.models.tankdk.CalculationsTankDK;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Golden gate for the Relative Stats chart: {@code CalculationsBase.getRelativeStatValues} must
 * match the original C# engine for representative fixtures, including models that normalize the
 * list to a base stat (Enhance uses Attack Power, TankDK uses Stamina).
 */
@Tag("relative-stats")
class RelativeStatsGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path repoRoot;
    private static Path fixturesDir;
    private static Path goldenDir;

    @BeforeAll
    static void setUp() throws Exception {
        repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures");
        goldenDir = repoRoot.resolve("oracle/golden/relative-stats");

        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
    }

    static List<String> fixtures() throws Exception {
        String only = System.getProperty("jrawr.relativeFixture", "");
        if (!only.isEmpty()) {
            return List.of(only.split(","));
        }
        try (Stream<Path> files = Files.walk(goldenDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> goldenDir.relativize(p).toString().replace('\\', '/'))
                    .map(p -> p.substring(0, p.length() - 5))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void relativeStatsMatchGolden(String modelAndFixture) throws Exception {
        int slash = modelAndFixture.indexOf('/');
        String model = modelAndFixture.substring(0, slash);
        String fixture = modelAndFixture.substring(slash + 1);

        CalculationsBase calculations = loadModel(model);
        Character character = CharacterFixtureLoader.load(
                fixturesDir.resolve(model).resolve(fixture + ".xml"));
        character.recalculateSetBonuses();
        ICalculationOptionBase options = character.getCalculationOptions();
        if (options == null) {
            character.setCalculationOptions(defaultOptions(model));
        }

        CalculationOptionsMage mageOptions = character.getCalculationOptions() instanceof CalculationOptionsMage
                ? (CalculationOptionsMage) character.getCalculationOptions() : null;
        if (mageOptions != null) {
            mageOptions.setCharacter(character);
        }

        JsonNode root = MAPPER.readTree(goldenDir.resolve(model).resolve(fixture + ".json").toFile());
        JsonNode expected = root.get("relativeStats");
        ComparisonCalculationBase[] actual = CalculationsBase.getRelativeStatValues(character);

        assertThat(actual).as("%s/%s entries", model, fixture).hasSameSizeAs(expected);
        for (int index = 0; index < expected.size(); index++) {
            JsonNode entry = expected.get(index);
            ComparisonCalculationBase ccb = actual[index];
            String field = "relativeStats[" + index + "]";
            assertThat(ccb.getName()).as("%s %s name", modelAndFixture, field)
                    .isEqualTo(entry.get("name").asText());
            assertThat(ccb.getDescription()).as("%s %s description", modelAndFixture, field)
                    .isEqualTo(entry.get("description").asText());
            String baseStat = entry.get("baseStat").isNull() ? null : entry.get("baseStat").asText();
            assertThat(ccb.getBaseStat()).as("%s %s baseStat", modelAndFixture, field)
                    .isEqualTo(baseStat);
            assertFloatClose(modelAndFixture, field + ".overallPoints",
                    entry.get("overallPoints").asDouble(), ccb.getOverallPoints());
            JsonNode subPoints = entry.get("subPoints");
            assertThat(ccb.getSubPoints()).as("%s %s subPoints", modelAndFixture, field)
                    .hasSameSizeAs(subPoints);
            for (int sub = 0; sub < subPoints.size(); sub++) {
                assertFloatClose(modelAndFixture, field + ".subPoints[" + sub + "]",
                        subPoints.get(sub).asDouble(), ccb.getSubPoints()[sub]);
            }
        }
    }

    private static CalculationsBase loadModel(String model) {
        switch (model) {
            case "mage":
                CalculationsMage.register();
                Calculations.loadModel(CalculationsMage.class);
                return CalculationsMage.getInstance();
            case "cat":
                CalculationsCat.register();
                Calculations.loadModel(CalculationsCat.class);
                return CalculationsCat.getInstance();
            case "enhance":
                CalculationsEnhance.register();
                Calculations.loadModel(CalculationsEnhance.class);
                return CalculationsEnhance.getInstance();
            case "tankdk":
                CalculationsTankDK.register();
                Calculations.loadModel(CalculationsTankDK.class);
                return CalculationsTankDK.getInstance();
            default:
                throw new IllegalArgumentException("no relative-stat golden loader for model " + model);
        }
    }

    private static ICalculationOptionBase defaultOptions(String model) {
        switch (model) {
            case "mage":
                return new CalculationOptionsMage();
            case "cat":
                return new CalculationOptionsCat();
            case "enhance":
                return new CalculationOptionsEnhance();
            case "tankdk":
                return new CalculationOptionsTankDK();
            default:
                return null;
        }
    }

    private static void assertFloatClose(String fixture, String field, double expected, double actual) {
        float expectedFloat = (float) expected;
        if (Float.isNaN(expectedFloat) || Float.isInfinite(expectedFloat)) {
            assertThat(Float.floatToIntBits((float) actual)).as("%s %s", fixture, field)
                    .isEqualTo(Float.floatToIntBits(expectedFloat));
            return;
        }
        double tolerance = Math.max(1e-12, Math.abs(expectedFloat) * 1e-9);
        assertThat(actual)
                .as("%s %s expected=%s actual=%s", fixture, field, expectedFloat, actual)
                .isCloseTo(expectedFloat, within(tolerance));
    }
}
