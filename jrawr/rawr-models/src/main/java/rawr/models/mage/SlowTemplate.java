// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class SlowTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.12 * BaseMana.get(70)), 0, 0, 0, 0, 0);
        spellData[1] = makeSpellData((int)(0.12 * BaseMana.get(71)), 0, 0, 0, 0, 0);
        spellData[2] = makeSpellData((int)(0.12 * BaseMana.get(72)), 0, 0, 0, 0, 0);
        spellData[3] = makeSpellData((int)(0.12 * BaseMana.get(73)), 0, 0, 0, 0, 0);
        spellData[4] = makeSpellData((int)(0.12 * BaseMana.get(74)), 0, 0, 0, 0, 0);
        spellData[5] = makeSpellData((int)(0.12 * BaseMana.get(75)), 0, 0, 0, 0, 0);
        spellData[6] = makeSpellData((int)(0.12 * BaseMana.get(76)), 0, 0, 0, 0, 0);
        spellData[7] = makeSpellData((int)(0.12 * BaseMana.get(77)), 0, 0, 0, 0, 0);
        spellData[8] = makeSpellData((int)(0.12 * BaseMana.get(78)), 0, 0, 0, 0, 0);
        spellData[9] = makeSpellData((int)(0.12 * BaseMana.get(79)), 0, 0, 0, 0, 0);
        spellData[10] = makeSpellData((int)(0.12 * BaseMana.get(80)), 0, 0, 0, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.12 * BaseMana.get(80)), 0, 0, 0, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.12 * BaseMana.get(81)), 0, 0, 0, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.12 * BaseMana.get(82)), 0, 0, 0, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.12 * BaseMana.get(83)), 0, 0, 0, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.12 * BaseMana.get(84)), 0, 0, 0, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.12 * BaseMana.get(85)), 0, 0, 0, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        return spellData[options.getPlayerLevel() - 70];
    }

    public void initialize(Solver solver) {
        setName("Slow");
        initializeCastTime(false, true, 0f, 0f);
        initializeDamage(solver, false, 30, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()));
        setDirty(false);
    }
}
