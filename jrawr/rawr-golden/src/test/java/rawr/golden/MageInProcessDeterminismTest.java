package rawr.golden;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.CalculationsMage;
import rawr.models.mage.CharacterCalculationsMage;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures a shared JVM is deterministic across characters: the shared SpecialEffect interpolation
 * caches are invalidated per top-level calculation (SUSPECTED_BUGS #15), so running another fixture
 * first must not change a character's rating.
 */
class MageInProcessDeterminismTest {
    private static Path fixturesDir;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/mage");

        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));

        CalculationsMage.register();
        Calculations.loadModel(CalculationsMage.class);
    }

    @Test
    void repeatedCalculationInSameJvmIsStableAcrossPriorFixtures() throws Exception {
        float firstFire = calculate("fire_277_raid");
        calculate("frost_264_raid");
        float secondFire = calculate("fire_277_raid");
        assertThat(secondFire).as("fire_277_raid OverallPoints after frost").isEqualTo(firstFire);
    }

    private static float calculate(String fixture) throws Exception {
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        CalculationOptionsMage options = (CalculationOptionsMage) character.getCalculationOptions();
        if (options == null) {
            options = new CalculationOptionsMage();
            character.setCalculationOptions(options);
        }
        character.recalculateSetBonuses();
        CharacterCalculationsMage ret = (CharacterCalculationsMage) CalculationsMage.getInstance()
                .getCharacterCalculations(character, null, true, true, false);
        return ret.getOverallPoints();
    }
}
