// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class BlastWaveTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.07 * BaseMana.get(70)), 616, 724, 0.1929f, 0, 0);
        spellData[1] = makeSpellData((int)(0.07 * BaseMana.get(71)), 616, 724, 0.1929f, 0, 0);
        spellData[2] = makeSpellData((int)(0.07 * BaseMana.get(72)), 616, 724, 0.1929f, 0, 0);
        spellData[3] = makeSpellData((int)(0.07 * BaseMana.get(73)), 616, 724, 0.1929f, 0, 0);
        spellData[4] = makeSpellData((int)(0.07 * BaseMana.get(74)), 616, 724, 0.1929f, 0, 0);
        spellData[5] = makeSpellData((int)(0.07 * BaseMana.get(75)), 882, 1038, 0.1929f, 0, 0);
        spellData[6] = makeSpellData((int)(0.07 * BaseMana.get(76)), 882, 1038, 0.1929f, 0, 0);
        spellData[7] = makeSpellData((int)(0.07 * BaseMana.get(77)), 882, 1038, 0.1929f, 0, 0);
        spellData[8] = makeSpellData((int)(0.07 * BaseMana.get(78)), 882, 1038, 0.1929f, 0, 0);
        spellData[9] = makeSpellData((int)(0.07 * BaseMana.get(79)), 882, 1038, 0.1929f, 0, 0);
        spellData[10] = makeSpellData((int)(0.07 * BaseMana.get(80)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.07 * BaseMana.get(80)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.07 * BaseMana.get(81)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.07 * BaseMana.get(82)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.07 * BaseMana.get(83)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.07 * BaseMana.get(84)), 1047, 1233, 0.1929f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.07 * BaseMana.get(85)), 1047, 1233, 0.1929f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public void initialize(Solver solver) {
        setName("Blast Wave");
        initializeCastTime(false, true, 0, 30);
        initializeDamage(solver, true, 0, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        setDirty(false);
    }
}
