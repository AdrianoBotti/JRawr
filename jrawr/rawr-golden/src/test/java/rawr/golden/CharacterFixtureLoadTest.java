package rawr.golden;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.data.XmlLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 step 3: the fixture XML must load identically for the parts the Mage model consumes
 * (talents string, per-model calculation options, professions).
 */
class CharacterFixtureLoadTest {
    private static Path fixturesDir;
    private static Path gameData;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/mage");

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
    void talentsAndOptionsLoad(String fixture) throws Exception {
        Path fixturePath = fixturesDir.resolve(fixture + ".xml");
        Character character = CharacterFixtureLoader.load(fixturePath);

        Document doc = XmlLoader.parse(fixturePath);
        Element root = doc.getDocumentElement();
        String expectedTalents = XmlLoader.text(root, "MageTalents");
        assertThat(character.getMageTalents().toString()).as("%s: talents string", fixture).isEqualTo(expectedTalents);

        String options = character.getSerializedCalculationOptions().get("Mage");
        assertThat(options).as("%s: Mage options present", fixture).isNotNull();
        assertThat(options).as("%s: Mage options content", fixture).contains("CalculationOptionsMage");
    }
}
