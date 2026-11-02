// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ConjureManaGemTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];

    static {
        spellData[0] = makeSpellData((int)(0.75 * BaseMana.get(70)), 0, 0, 0, 0, 0);
        spellData[1] = makeSpellData((int)(0.75 * BaseMana.get(71)), 0, 0, 0, 0, 0);
        spellData[2] = makeSpellData((int)(0.75 * BaseMana.get(72)), 0, 0, 0, 0, 0);
        spellData[3] = makeSpellData((int)(0.75 * BaseMana.get(73)), 0, 0, 0, 0, 0);
        spellData[4] = makeSpellData((int)(0.75 * BaseMana.get(74)), 0, 0, 0, 0, 0);
        spellData[5] = makeSpellData((int)(0.75 * BaseMana.get(75)), 0, 0, 0, 0, 0);
        spellData[6] = makeSpellData((int)(0.75 * BaseMana.get(76)), 0, 0, 0, 0, 0);
        spellData[7] = makeSpellData((int)(0.75 * BaseMana.get(77)), 0, 0, 0, 0, 0);
        spellData[8] = makeSpellData((int)(0.75 * BaseMana.get(78)), 0, 0, 0, 0, 0);
        spellData[9] = makeSpellData((int)(0.75 * BaseMana.get(79)), 0, 0, 0, 0, 0);
        spellData[10] = makeSpellData((int)(0.75 * BaseMana.get(80)), 0, 0, 0, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        return spellData[options.getPlayerLevel() - 70];
    }

    public void initialize(Solver solver) {
        setName("Conjure Mana Gem");
        initializeCastTime(false, false, 3, 0);
        initializeDamage(solver, false, 0, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()), 0, 1, 0);
        setDirty(false);
    }
}
