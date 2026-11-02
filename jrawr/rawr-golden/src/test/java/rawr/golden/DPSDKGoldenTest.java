package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.stats.Stats;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.models.dpsdk.CalculationOptionsDPSDK;
import rawr.models.dpsdk.CalculationsDPSDK;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Phase 6 gate: DPSDK ratings, calculated stats, display values and item comparison must match
 * the golden files produced by the original C# engine (oracle/golden/dpsdk).
 */
@Tag("dpsdk-golden")
class DPSDKGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path fixturesDir;
    private static Path goldenDir;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/dpsdk");
        goldenDir = repoRoot.resolve("oracle/golden/dpsdk");

        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));

        CalculationsDPSDK.register();
        Calculations.loadModel(CalculationsDPSDK.class);
    }

    static List<String> fixtureNames() throws Exception {
        String only = System.getProperty("jrawr.fixture", "");
        if (!only.isEmpty()) {
            return List.of(only.split(","));
        }
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        try (Stream<Path> files = Files.list(repoRoot.resolve("oracle/fixtures/dpsdk"))) {
            return files
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 4))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureNames")
    void dpsDkMatchesGolden(String fixture) throws Exception {
        long t0 = System.nanoTime();
        System.out.println("[dpsdk] " + fixture + " start");
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        CalculationOptionsDPSDK options = character.getCalculationOptions() instanceof CalculationOptionsDPSDK
                ? (CalculationOptionsDPSDK) character.getCalculationOptions()
                : null;
        if (options == null) {
            options = new CalculationOptionsDPSDK();
            character.setCalculationOptions(options);
        }
        character.recalculateSetBonuses();
        CalculationsDPSDK calc = CalculationsDPSDK.getInstance();
        rawr.models.dpsdk.CharacterCalculationsDPSDK ret = (rawr.models.dpsdk.CharacterCalculationsDPSDK) calc
                .getCharacterCalculations(character, null, true, true, true);
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());

        compareRatings(fixture, root, ret);

        Stats stats = calc.getCharacterStats(character, null);
        JsonNode expectedStats = root.get("calculatedStats");
        int compared = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = expectedStats.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
            assertThat(property).as("%s: unknown stat %s", fixture, name).isNotNull();
            assertFloatClose(fixture, "calculatedStats." + name, entry.getValue().asDouble(), property.accessor().get(stats));
            compared++;
        }
        assertThat(compared).as("%s: compared stats", fixture).isGreaterThan(300);

        JsonNode expectedDisplay = root.get("display");
        Map<String, String> actualDisplay = ret.getCharacterDisplayCalculationValues();
        Set<String> expectedKeys = new LinkedHashSet<String>();
        for (Iterator<Map.Entry<String, JsonNode>> it = expectedDisplay.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            expectedKeys.add(entry.getKey());
            assertThat(actualDisplay.get(entry.getKey()))
                    .as("%s: display %s", fixture, entry.getKey())
                    .isEqualTo(entry.getValue().asText());
        }
        assertThat(actualDisplay.keySet()).as("%s: display keys", fixture).isEqualTo(expectedKeys);

        compareComparison(fixture, root, character);
        System.out.println("[dpsdk] " + fixture + " done in " + ((System.nanoTime() - t0) / 1_000_000) + " ms");
    }

    private static void compareRatings(String fixture, JsonNode root, rawr.models.dpsdk.CharacterCalculationsDPSDK ret) {
        JsonNode ratings = root.get("ratings");
        assertFloatClose(fixture, "ratings.OverallPoints", ratings.get("OverallPoints").asDouble(), ret.getOverallPoints());

        JsonNode sub = ratings.get("SubPoints");
        float[] actualSub = ret.getSubPoints();
        assertThat(actualSub).as("%s: subpoints length", fixture).hasSize(sub.size());
        for (int i = 0; i < sub.size(); i++) {
            assertFloatClose(fixture, "ratings.SubPoints[" + i + "]", sub.get(i).asDouble(), actualSub[i]);
        }

        for (Iterator<Map.Entry<String, JsonNode>> it = ratings.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            if (name.equals("OverallPoints") || name.equals("SubPoints")) continue;
            Object actual = readSimpleMember(ret, name);
            assertThat(actual).as("%s: ratings.%s member", fixture, name).isNotNull();
            if (entry.getValue().isTextual()) {
                assertThat(String.valueOf(actual)).as("%s: ratings.%s", fixture, name)
                        .isEqualTo(entry.getValue().asText());
            } else if (entry.getValue().isBoolean()) {
                assertThat(actual).as("%s: ratings.%s", fixture, name).isEqualTo(entry.getValue().asBoolean());
            } else {
                assertFloatClose(fixture, "ratings." + name, entry.getValue().asDouble(), ((Number) actual).doubleValue());
            }
        }
    }

    private static void compareComparison(String fixture, JsonNode root, Character character) {
        JsonNode exp = root.get("comparison");
        CalculationsDPSDK calc = CalculationsDPSDK.getInstance();
        assertThat(exp).as("%s: comparison items", fixture).isNotEmpty();
        for (JsonNode itemNode : exp) {
            int id = itemNode.get("itemId").asInt();
            Item item = ItemCache.findItemById(id);
            assertThat(item).as("%s: comparison item %d", fixture, id).isNotNull();
            ComparisonCalculationBase comp = calc.getItemCalculations(item, character, CharacterSlot.Head);
            assertThat(comp).as("%s: comparison item %d result", fixture, id).isNotNull();
            assertThat(comp.getName()).as("%s: comparison item %d name", fixture, id).isEqualTo(itemNode.get("name").asText());
            assertFloatClose(fixture, "comparison[" + id + "].overallPoints", itemNode.get("overallPoints").asDouble(), comp.getOverallPoints());
            JsonNode sub = itemNode.get("subPoints");
            float[] actualSub = comp.getSubPoints();
            assertThat(actualSub).as("%s: comparison[%d].subPoints", fixture, id).hasSize(sub.size());
            for (int i = 0; i < sub.size(); i++) {
                assertFloatClose(fixture, "comparison[" + id + "].subPoints[" + i + "]", sub.get(i).asDouble(), actualSub[i]);
            }
        }
    }

    /**
     * C# property names are PascalCase; Java members are camelCase. Tries the exact name, the
     * lower-first camel form, a case-insensitive field scan (handles acronym names like DRWStats)
     * and case-insensitive getters/is-getters.
     */
    private static Object readSimpleMember(Object target, String name) {
        String camel = name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
        for (String fieldName : new String[] { name, camel }) {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                } catch (IllegalAccessException ex) {
                    return null;
                }
            }
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(name)) {
                    try {
                        field.setAccessible(true);
                        return field.get(target);
                    } catch (IllegalAccessException ex) {
                        return null;
                    }
                }
            }
        }
        String upper = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0) continue;
            String methodName = method.getName();
            if (methodName.equals("get" + name) || methodName.equals("get" + upper) || methodName.equals("is" + upper)
                    || methodName.equalsIgnoreCase("get" + name) || methodName.equalsIgnoreCase("is" + name)) {
                try {
                    return method.invoke(target);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /** Golden floats were written with .NET shortest round-trip formatting; compare the same float bits. */
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
