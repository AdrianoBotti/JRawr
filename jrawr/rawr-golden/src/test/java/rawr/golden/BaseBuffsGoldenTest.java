package rawr.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 gate: active buffs (fixture order + set bonuses) and the set-bonus counts must match the
 * golden files.
 */
class BaseBuffsGoldenTest {
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
    void activeBuffsAndSetBonusesMatchGolden(String fixture) throws Exception {
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        JsonNode root = MAPPER.readTree(goldenDir.resolve(fixture + ".json").toFile());

        List<String> javaBuffs = new ArrayList<String>();
        for (Buff buff : character.getActiveBuffs()) javaBuffs.add(buff.name);
        List<String> goldenBuffs = new ArrayList<String>();
        for (JsonNode buff : root.get("character").get("activeBuffs")) goldenBuffs.add(buff.asText());
        assertThat(javaBuffs).as("%s: active buffs", fixture).isEqualTo(goldenBuffs);

        JsonNode goldenSetBonuses = root.get("character").get("setBonusCount");
        Map<String, Integer> javaSetBonuses = character.getSetBonusCount();
        assertThat(javaSetBonuses).as("%s: set bonus count", fixture).hasSameSizeAs(goldenSetBonuses);
        for (Iterator<Map.Entry<String, JsonNode>> it = goldenSetBonuses.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            assertThat(javaSetBonuses.get(entry.getKey()))
                    .as("%s: set bonus %s", fixture, entry.getKey())
                    .isEqualTo(entry.getValue().asInt());
        }
    }
}
