// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FrostNovaTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.07 * BaseMana.get(70)), 100, 113, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[1] = makeSpellData((int)(0.07 * BaseMana.get(71)), 232, 262, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[2] = makeSpellData((int)(0.07 * BaseMana.get(72)), 232, 263, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[3] = makeSpellData((int)(0.07 * BaseMana.get(73)), 233, 263, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[4] = makeSpellData((int)(0.07 * BaseMana.get(74)), 233, 264, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[5] = makeSpellData((int)(0.07 * BaseMana.get(75)), 365, 415, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[6] = makeSpellData((int)(0.07 * BaseMana.get(76)), 365, 416, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[7] = makeSpellData((int)(0.07 * BaseMana.get(77)), 366, 417, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[8] = makeSpellData((int)(0.07 * BaseMana.get(78)), 367, 418, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[9] = makeSpellData((int)(0.07 * BaseMana.get(79)), 368, 419, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellData[10] = makeSpellData((int)(0.07 * BaseMana.get(80)), 368, 419, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.07 * BaseMana.get(80)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.07 * BaseMana.get(81)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.07 * BaseMana.get(82)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.07 * BaseMana.get(83)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.07 * BaseMana.get(84)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.07 * BaseMana.get(85)), 359, 411, 1.5f / 3.5f * 0.5f * 0.13f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public void initialize(Solver solver) {
        setName("Frost Nova");
        initializeCastTime(false, true, 0, 25);
        initializeDamage(solver, true, 0, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()));
        setDirty(false);
    }
}
