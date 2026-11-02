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
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.CalculationsMage;
import rawr.models.mage.CharacterCalculationsMage;
import rawr.models.mage.Cycle;
import rawr.models.mage.CycleId;
import rawr.models.mage.DisplayCalculations;
import rawr.models.mage.EffectCooldown;
import rawr.models.mage.Segment;
import rawr.models.mage.SolutionVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Phase 3 gate: Mage ratings, calculated stats and display values must match the golden files
 * produced by the original C# engine (oracle/golden/mage).
 */
@Tag("mage-golden")
class MageGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path fixturesDir;
    private static Path goldenDir;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/mage");
        goldenDir = repoRoot.resolve("oracle/golden/mage");

        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));

        CalculationsMage.register();
        Calculations.loadModel(CalculationsMage.class);
        rawr.models.mage.Sequence.timeLimitEnabled = false;
    }

    static List<String> fixtureNames() throws Exception {
        String only = System.getProperty("jrawr.fixture", "");
        if (!only.isEmpty()) {
            return List.of(only.split(","));
        }
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        try (Stream<Path> files = Files.list(repoRoot.resolve("oracle/fixtures/mage"))) {
            return files
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 4))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureNames")
    void mageMatchesGolden(String fixture) throws Exception {
        long t0 = System.nanoTime();
        System.out.println("[mage] " + fixture + " start");
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        CalculationOptionsMage options = (CalculationOptionsMage) character.getCalculationOptions();
        if (options == null) {
            options = new CalculationOptionsMage();
            character.setCalculationOptions(options);
        }
        character.recalculateSetBonuses();
        CharacterCalculationsMage ret = (CharacterCalculationsMage) CalculationsMage.getInstance()
                .getCharacterCalculations(character, null, true, true, true);
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());

        JsonNode ratings = root.get("ratings");
        assertFloatClose(fixture, "ratings.OverallPoints", ratings.get("OverallPoints").asDouble(), ret.getOverallPoints());
        assertFloatClose(fixture, "ratings.DpsRating", ratings.get("DpsRating").asDouble(), ret.getDpsRating());
        assertFloatClose(fixture, "ratings.SurvivabilityRating", ratings.get("SurvivabilityRating").asDouble(), ret.getSurvivabilityRating());

        JsonNode sub = ratings.get("SubPoints");
        float[] actualSub = ret.getSubPoints();
        assertThat(actualSub).as("%s: subpoints length", fixture).hasSize(sub.size());
        for (int i = 0; i < sub.size(); i++) {
            assertFloatClose(fixture, "ratings.SubPoints[" + i + "]", sub.get(i).asDouble(), actualSub[i]);
        }

        Stats stats = CalculationsMage.getInstance().getCharacterStats(character, null);
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
        int displayCompared = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = expectedDisplay.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            String expectedValue = entry.getValue().asText();
            String actualValue = actualDisplay.get(key);
            if ("Sequence".equals(key)) {
                // The reconstruction search is wall-clock bounded (5 s) in the original; the oracle was
                // dumped under mono --interp and hit that cap, so the body is a cross-runtime artifact
                // (see docs/dev/DEVIATIONS.md). Require the same header and an at-least-as-good score.
                assertThat(actualValue).as("%s: display Sequence present", fixture).isNotBlank();
                assertThat(firstNonEmptyLine(actualValue)).as("%s: display Sequence header", fixture)
                        .isEqualTo(firstNonEmptyLine(expectedValue));
                Double expectedScore = parseSequenceScore(expectedValue);
                Double actualScore = parseSequenceScore(actualValue);
                if (expectedScore != null && actualScore != null) {
                    assertThat(actualScore).as("%s: sequence reconstruction score", fixture)
                            .isGreaterThanOrEqualTo(expectedScore - 1e-3);
                }
                displayCompared++;
                continue;
            }
            assertThat(actualValue)
                    .as("%s: display %s", fixture, key)
                    .isEqualTo(expectedValue);
            displayCompared++;
        }
        assertThat(displayCompared).as("%s: compared display values", fixture).isGreaterThan(50);
        compareSolver(fixture, root, ret, options);
        compareComparison(fixture, root, character);
        System.out.println("[mage] " + fixture + " done in " + ((System.nanoTime() - t0) / 1_000_000) + " ms");
    }

    private static String firstNonEmptyLine(String value) {
        for (String line : value.split("\r?\n")) {
            if (!line.trim().isEmpty()) return line;
        }
        return "";
    }

    private static Double parseSequenceScore(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Score: ([0-9]+(?:\\.[0-9]+)?)").matcher(value);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
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

    private void compareSolver(String fixture, JsonNode root, CharacterCalculationsMage ret, CalculationOptionsMage options) {
        JsonNode exp = root.get("solver");
        DisplayCalculations dc = ret.getDisplayCalculations();
        assertThat(dc).as("%s: display calculations", fixture).isNotNull();

        assertFloatClose(fixture, "solver.fightDuration", exp.get("fightDuration").asDouble(), options.getFightDuration());
        assertThat(options.getTargetLevel()).as("%s: solver.targetLevel", fixture).isEqualTo(exp.get("targetLevel").asInt());

        for (Iterator<Map.Entry<String, JsonNode>> it = exp.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            if (name.equals("fightDuration") || name.equals("targetLevel") || name.equals("segments")
                    || name.equals("cooldowns") || name.equals("solution") || name.equals("solutionVariables")
                    || name.equals("candidateCycles") || name.equals("sequence")) {
                continue;
            }
            Object actual = readSimpleMember(dc, name);
            assertThat(actual).as("%s: solver.%s member", fixture, name).isNotNull();
            assertJsonValue(fixture, "solver." + name, entry.getValue(), actual);
        }

        JsonNode expSegments = exp.get("segments");
        List<Segment> segments = dc.getSegmentList();
        assertThat(segments).as("%s: solver.segments", fixture).hasSize(expSegments.size());
        for (int i = 0; i < expSegments.size(); i++) {
            JsonNode e = expSegments.get(i);
            assertThat(segments.get(i).index).as("%s: segments[%d].index", fixture, i).isEqualTo(e.get("index").asInt());
            assertDoubleClose(fixture, "segments[" + i + "].duration", e.get("duration").asDouble(), segments.get(i).duration);
            assertDoubleClose(fixture, "segments[" + i + "].timeStart", e.get("timeStart").asDouble(), segments.get(i).timeStart);
        }

        JsonNode expCooldowns = exp.get("cooldowns");
        List<EffectCooldown> cooldowns = dc.getCooldownList();
        assertThat(cooldowns).as("%s: solver.cooldowns", fixture).hasSize(expCooldowns.size());
        for (int i = 0; i < expCooldowns.size(); i++) {
            JsonNode e = expCooldowns.get(i);
            EffectCooldown actual = cooldowns.get(i);
            assertThat(actual.name).as("%s: cooldowns[%d].name", fixture, i).isEqualTo(e.get("name").asText());
            assertThat(actual.mask).as("%s: cooldowns[%d].mask", fixture, i).isEqualTo(e.get("mask").asInt());
            assertFloatClose(fixture, "cooldowns[" + i + "].cooldown", e.get("cooldown").asDouble(), actual.cooldown);
            assertFloatClose(fixture, "cooldowns[" + i + "].duration", e.get("duration").asDouble(), actual.duration);
            assertFloatClose(fixture, "cooldowns[" + i + "].maximumDuration", e.get("maximumDuration").asDouble(), actual.maximumDuration);
            assertThat(actual.itemBased).as("%s: cooldowns[%d].itemBased", fixture, i).isEqualTo(e.get("itemBased").asBoolean());
        }

        JsonNode expSolution = exp.get("solution");
        double[] solution = dc.solution;
        if (expSolution.size() == 0) {
            assertThat(solution == null || solution.length == 0).as("%s: solver.solution", fixture).isTrue();
        } else {
            assertThat(solution).as("%s: solver.solution", fixture).hasSize(expSolution.size());
            for (int i = 0; i < expSolution.size(); i++) {
                assertDoubleClose(fixture, "solution[" + i + "]", expSolution.get(i).asDouble(), solution[i]);
            }
        }

        JsonNode expVars = exp.get("solutionVariables");
        List<SolutionVariable> vars = dc.solutionVariable;
        assertThat(vars).as("%s: solver.solutionVariables", fixture).hasSize(expVars.size());
        for (int i = 0; i < expVars.size(); i++) {
            JsonNode e = expVars.get(i);
            SolutionVariable actual = vars.get(i);
            assertThat(actual.segment).as("%s: vars[%d].segment", fixture, i).isEqualTo(e.get("segment").asInt());
            assertThat(actual.manaSegment).as("%s: vars[%d].manaSegment", fixture, i).isEqualTo(e.get("manaSegment").asInt());
            assertThat(actual.type.toString()).as("%s: vars[%d].type", fixture, i).isEqualTo(e.get("type").asText());
            assertDoubleClose(fixture, "vars[" + i + "].dps", e.get("dps").asDouble(), actual.dps);
            assertDoubleClose(fixture, "vars[" + i + "].mps", e.get("mps").asDouble(), actual.mps);
            assertDoubleClose(fixture, "vars[" + i + "].tps", e.get("tps").asDouble(), actual.tps);
            String expectedCycle = e.get("cycle").isNull() ? null : e.get("cycle").asText();
            String actualCycle = actual.cycle == null ? null : actual.cycle.name;
            assertThat(actualCycle).as("%s: vars[%d].cycle", fixture, i).isEqualTo(expectedCycle);
            if (e.get("state").isNull()) {
                assertThat(actual.state).as("%s: vars[%d].state", fixture, i).isNull();
            } else {
                assertThat(actual.state.getEffects()).as("%s: vars[%d].state", fixture, i).isEqualTo(e.get("state").asInt());
            }
        }

        JsonNode expCycles = exp.get("candidateCycles");
        Map<String, Cycle> actualCycles = new java.util.LinkedHashMap<String, Cycle>();
        for (CycleId cid : CycleId.values()) {
            if (cid == CycleId.None) continue;
            try {
                Cycle c = dc.getBaseState().getCycle(cid);
                if (c != null) actualCycles.put(cid.toString(), c);
            } catch (Exception ignored) {
            }
        }
        assertThat(actualCycles).as("%s: solver.candidateCycles count", fixture).hasSize(expCycles.size());
        for (JsonNode e : expCycles) {
            String cycleId = e.get("cycleId").asText();
            Cycle actual = actualCycles.get(cycleId);
            assertThat(actual).as("%s: candidateCycles %s", fixture, cycleId).isNotNull();
            String expectedName = e.get("name").isNull() ? null : e.get("name").asText();
            assertThat(actual.name).as("%s: candidateCycles %s name", fixture, cycleId).isEqualTo(expectedName);
            assertFloatClose(fixture, "candidateCycles." + cycleId + ".dps", e.get("dps").asDouble(), actual.getDamagePerSecond());
            assertFloatClose(fixture, "candidateCycles." + cycleId + ".mps", e.get("mps").asDouble(), actual.getManaPerSecond());
            assertFloatClose(fixture, "candidateCycles." + cycleId + ".castTime", e.get("castTime").asDouble(), actual.castTime);
        }

        JsonNode expSequence = exp.get("sequence");
        if (!expSequence.isNull()) {
            String expectedSequence = expSequence.asText();
            String actualSequence = dc.ReconstructSequence();
            assertThat(actualSequence).as("%s: solver.sequence", fixture).isNotBlank();
            assertThat(firstNonEmptyLine(actualSequence)).as("%s: solver.sequence header", fixture)
                    .isEqualTo(firstNonEmptyLine(expectedSequence));
            Double expectedScore = parseSequenceScore(expectedSequence);
            Double actualScore = parseSequenceScore(actualSequence);
            if (expectedScore != null && actualScore != null) {
                assertThat(actualScore).as("%s: solver.sequence score", fixture)
                        .isGreaterThanOrEqualTo(expectedScore - 1e-3);
            }
        }
    }

    private void compareComparison(String fixture, JsonNode root, Character character) {
        JsonNode exp = root.get("comparison");
        CalculationsMage calc = CalculationsMage.getInstance();
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

    private static Object readSimpleMember(Object target, String name) {
        String camel = name.substring(0, 1).toLowerCase(java.util.Locale.ROOT) + name.substring(1);
        String upper = name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1);
        for (String fieldName : new String[] { name, camel }) {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                } catch (IllegalAccessException ex) {
                    return null;
                }
            }
        }
        for (String getterName : new String[] { "get" + name, "get" + upper, "is" + upper }) {
            try {
                java.lang.reflect.Method getter = target.getClass().getMethod(getterName);
                return getter.invoke(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void assertJsonValue(String fixture, String field, JsonNode expected, Object actual) {
        if (expected.isTextual()) {
            String text = expected.asText();
            if (text.equals("NaN") || text.equals("Infinity") || text.equals("-Infinity")) {
                assertThat(String.valueOf(actual)).as("%s %s", fixture, field).isEqualTo(text);
                return;
            }
            assertThat(actual).as("%s %s", fixture, field).isEqualTo(text);
            return;
        }
        if (expected.isBoolean()) {
            assertThat(actual).as("%s %s", fixture, field).isEqualTo(expected.asBoolean());
            return;
        }
        if (expected.isNumber()) {
            if (actual instanceof Float) {
                float expectedFloat = (float) expected.asDouble();
                assertThat(Float.floatToIntBits((Float) actual)).as("%s %s expected=%s actual=%s", fixture, field, expectedFloat, actual)
                        .isEqualTo(Float.floatToIntBits(expectedFloat));
                return;
            }
            if (actual instanceof Double) {
                assertDoubleClose(fixture, field, expected.asDouble(), (Double) actual);
                return;
            }
            if (actual instanceof Integer || actual instanceof Long || actual instanceof Short) {
                assertThat(((Number) actual).longValue()).as("%s %s", fixture, field).isEqualTo(expected.asLong());
                return;
            }
        }
        assertThat(actual).as("%s %s", fixture, field).isEqualTo(expected.asText());
    }

    private static void assertDoubleClose(String fixture, String field, double expected, double actual) {
        if (Double.isNaN(expected) || Double.isInfinite(expected)) {
            assertThat(Double.doubleToLongBits(actual)).as("%s %s", fixture, field)
                    .isEqualTo(Double.doubleToLongBits(expected));
            return;
        }
        double tolerance = Math.max(1e-12, Math.abs(expected) * 1e-9);
        assertThat(actual).as("%s %s expected=%s actual=%s", fixture, field, expected, actual)
                .isCloseTo(expected, within(tolerance));
    }
}
