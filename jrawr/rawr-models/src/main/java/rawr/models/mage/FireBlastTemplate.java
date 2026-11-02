// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FireBlastTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.21 * BaseMana.get(71)), 664, 786, 1.5f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.21 * BaseMana.get(71)), 667, 790, 1.5f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.21 * BaseMana.get(72)), 671, 794, 1.5f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.21 * BaseMana.get(73)), 675, 798, 1.5f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.21 * BaseMana.get(74)), 760, 900, 1.5f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.21 * BaseMana.get(75)), 764, 904, 1.5f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.21 * BaseMana.get(76)), 768, 908, 1.5f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.21 * BaseMana.get(77)), 772, 912, 1.5f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.21 * BaseMana.get(78)), 776, 916, 1.5f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.21 * BaseMana.get(79)), 780, 920, 1.5f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.21 * BaseMana.get(80)), 925, 1095, 1.5f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.21 * BaseMana.get(80)), 855, 1013, 0.429f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.21 * BaseMana.get(81)), 874, 1036, 0.429f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.21 * BaseMana.get(82)), 894, 1060, 0.429f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.21 * BaseMana.get(83)), 912, 1082, 0.429f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.21 * BaseMana.get(84)), 931, 1105, 0.429f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.21 * BaseMana.get(85)), 950, 1128, 0.429f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public void initialize(Solver solver) {
        setName("Fire Blast");
        initializeCastTime(false, true, 0, 8);
        initializeDamage(solver, false, 20, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        setCooldown(getCooldown() - 1.0f * solver.getMageTalents().getImprovedFireBlast());
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getIncineration());
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setDirty(false);
    }
}
