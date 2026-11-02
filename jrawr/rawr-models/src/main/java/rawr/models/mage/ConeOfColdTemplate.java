// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ConeOfColdTemplate extends SpellTemplate {
    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.25 * BaseMana.get(70)), 418, 457, 0.2142f, 0, 0);
        spellData[1] = makeSpellData((int)(0.25 * BaseMana.get(71)), 418, 457, 0.2142f, 0, 0);
        spellData[2] = makeSpellData((int)(0.25 * BaseMana.get(72)), 559, 611, 0.2142f, 0, 0);
        spellData[3] = makeSpellData((int)(0.25 * BaseMana.get(73)), 561, 614, 0.2142f, 0, 0);
        spellData[4] = makeSpellData((int)(0.25 * BaseMana.get(74)), 563, 616, 0.2142f, 0, 0);
        spellData[5] = makeSpellData((int)(0.25 * BaseMana.get(75)), 565, 618, 0.2142f, 0, 0);
        spellData[6] = makeSpellData((int)(0.25 * BaseMana.get(76)), 568, 621, 0.2142f, 0, 0);
        spellData[7] = makeSpellData((int)(0.25 * BaseMana.get(77)), 568, 621, 0.2142f, 0, 0);
        spellData[8] = makeSpellData((int)(0.25 * BaseMana.get(78)), 568, 621, 0.2142f, 0, 0);
        spellData[9] = makeSpellData((int)(0.25 * BaseMana.get(79)), 707, 773, 0.2142f, 0, 0);
        spellData[10] = makeSpellData((int)(0.25 * BaseMana.get(80)), 709, 776, 0.2142f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.25 * BaseMana.get(80)), 674, 736, 0.2142f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.25 * BaseMana.get(81)), 688, 752, 0.2142f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.25 * BaseMana.get(82)), 703, 769, 0.2142f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.25 * BaseMana.get(83)), 716, 784, 0.2142f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.25 * BaseMana.get(84)), 730, 800, 0.2142f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.25 * BaseMana.get(85)), 744, 816, 0.2142f, 0, 0);
    }

    public void initialize(Solver solver) {
        setName("Cone of Cold");
        initializeCastTime(false, true, 0, 10);
        initializeDamage(solver, true, 0, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()));
        setCooldown(getCooldown() * (1 - 0.07f * solver.getMageTalents().getIceFloes() + (solver.getMageTalents().getIceFloes() == 3 ? 0.01f : 0.00f)));
        int improvedConeOfCold = solver.getMageTalents().getImprovedConeOfCold();
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setBaseSpellModifier(getBaseSpellModifier() * (1 + ((improvedConeOfCold > 0) ? (0.05f + 0.1f * improvedConeOfCold) : 0)));
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getIncineration());
        setDirty(false);
    }
}
