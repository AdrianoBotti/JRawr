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

class FireProbeTest {
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
    void probe() throws Exception {
        String first = System.getProperty("jrawr.probe.first", "");
        if (!first.isEmpty()) calculate(first);
        if (Boolean.getBoolean("jrawr.probe.prodclear")) {
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        if (Boolean.getBoolean("jrawr.probe.touch")) {
            for (rawr.base.character.Buff b : rawr.base.character.Buff.getAllBuffs()) { rawr.base.stats.Stats st = b.stats; }
            for (rawr.base.items.Item it : rawr.base.items.ItemCache.allItems()) { rawr.base.stats.Stats st = it.getStats(); }
            for (rawr.base.items.Enchant e : rawr.base.items.Enchant.getAllEnchants()) { rawr.base.stats.Stats st = e.stats; }
        }
        if (Boolean.getBoolean("jrawr.probe.clearfx")) {
            java.util.List<rawr.base.items.Item> items = new java.util.ArrayList<rawr.base.items.Item>();
            for (rawr.base.items.Item it : rawr.base.items.ItemCache.allItems()) items.add(it);
            for (rawr.base.character.Buff b : rawr.base.character.Buff.getAllBuffs()) clearEffects(b.stats);
            for (rawr.base.items.Item it : items) clearEffects(it.getStats());
            for (rawr.base.items.Enchant e : rawr.base.items.Enchant.getAllEnchants()) clearEffects(e.stats);
        }
        if (Boolean.getBoolean("jrawr.probe.freshmodel")) {
            new CalculationsMage();
            Calculations.loadModel(CalculationsMage.class);
        }
        if (Boolean.getBoolean("jrawr.probe.clear")) {
            for (String name : new String[] { "cooldownStackingCacheMap", "cooldownStackingCacheMapNoMaxUseAssumption" }) {
                java.lang.reflect.Field f = CalculationOptionsMage.class.getDeclaredField(name);
                f.setAccessible(true);
                ((java.util.Map<?, ?>) f.get(null)).clear();
            }
        }
        System.out.println("[probe] fire_277_raid = " + calculate("fire_277_raid"));
    }

    private static void clearEffects(rawr.base.stats.Stats stats) {
        if (stats == null || stats.getRawSpecialEffectData() == null) return;
        for (rawr.base.effects.SpecialEffect effect : stats.getRawSpecialEffectData()) {
            if (effect == null) continue;
            try {
                java.lang.reflect.Field f = rawr.base.effects.SpecialEffect.class.getDeclaredField("interpolator");
                f.setAccessible(true);
                ((java.util.Map<?, ?>) f.get(effect)).clear();
            } catch (Exception ignored) {
            }
        }
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
                .getCharacterCalculations(character, null, true, true, true);
        return ret.getOverallPoints();
    }
}
