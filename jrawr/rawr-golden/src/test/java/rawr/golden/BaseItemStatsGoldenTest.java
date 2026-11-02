package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.stats.Stats;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;

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
 * Phase 2 gate: the resolved per-item stats (item + gems + socket bonus + enchant, incl. requirements)
 * must match the golden files produced by the original engine.
 */
class BaseItemStatsGoldenTest {
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

        Calculations.registerModel("Mage", TestMageModel.class);
        Calculations.loadModel(TestMageModel.class);
    }

    static List<String> fixtureNames() throws Exception {
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
    void itemTotalStatsMatchGolden(String fixture) throws Exception {
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());

        int compared = 0;
        int itemsCompared = 0;
        for (JsonNode itemNode : root.get("character").get("items")) {
            CharacterSlot slot = CharacterSlot.valueOf(itemNode.get("slot").asText());
            ItemInstance instance = character.get(slot);
            assertThat(instance).as("%s: slot %s", fixture, slot).isNotNull();
            assertThat(instance.getId()).as("%s: %s item id", fixture, slot).isEqualTo(itemNode.get("id").asInt());
            assertThat(instance.getEnchantId()).as("%s: %s enchant", fixture, slot).isEqualTo(itemNode.get("enchantId").asInt());
            assertThat(instance.getGem1Id()).as("%s: %s gem1", fixture, slot).isEqualTo(itemNode.get("gem1Id").asInt());
            assertThat(instance.getGem2Id()).as("%s: %s gem2", fixture, slot).isEqualTo(itemNode.get("gem2Id").asInt());
            assertThat(instance.getGem3Id()).as("%s: %s gem3", fixture, slot).isEqualTo(itemNode.get("gem3Id").asInt());
            assertThat(instance.matchesSocketBonus()).as("%s: %s socket bonus", fixture, slot).isEqualTo(itemNode.get("matchesSocketBonus").asBoolean());

            itemsCompared++;
            Stats stats = instance.getTotalStats(character);
            JsonNode totalStats = itemNode.get("totalStats");
            for (Iterator<Map.Entry<String, JsonNode>> it = totalStats.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String statName = entry.getKey();
                Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(statName);
                assertThat(property).as("%s: unknown stat %s", fixture, statName).isNotNull();
                double expected = entry.getValue().asDouble();
                double actual = property.accessor().get(stats);
                double tolerance = Math.max(1e-12, Math.abs(expected) * 1e-9);
                assertThat(actual)
                        .as("%s %s.%s expected=%s actual=%s", fixture, slot, statName, expected, actual)
                        .isCloseTo(expected, within(tolerance));
                compared++;
            }
        }
        assertThat(itemsCompared).as("%s: item count", fixture).isEqualTo(root.get("character").get("items").size());
    }
}
