// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class BlizzardTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.74 * BaseMana.get(70)), 2184, 2184, 1.1429f, 0, 0);
        spellData[1] = makeSpellData((int)(0.74 * BaseMana.get(71)), 2192, 2192, 1.1429f, 0, 0);
        spellData[2] = makeSpellData((int)(0.74 * BaseMana.get(72)), 2192, 2192, 1.1429f, 0, 0);
        spellData[3] = makeSpellData((int)(0.74 * BaseMana.get(73)), 2200, 2200, 1.1429f, 0, 0);
        spellData[4] = makeSpellData((int)(0.74 * BaseMana.get(74)), 2800, 2800, 1.1429f, 0, 0);
        spellData[5] = makeSpellData((int)(0.74 * BaseMana.get(75)), 2800, 2800, 1.1429f, 0, 0);
        spellData[6] = makeSpellData((int)(0.74 * BaseMana.get(76)), 2808, 2808, 1.1429f, 0, 0);
        spellData[7] = makeSpellData((int)(0.74 * BaseMana.get(77)), 2808, 2808, 1.1429f, 0, 0);
        spellData[8] = makeSpellData((int)(0.74 * BaseMana.get(78)), 2816, 2816, 1.1429f, 0, 0);
        spellData[9] = makeSpellData((int)(0.74 * BaseMana.get(79)), 2816, 2816, 1.1429f, 0, 0);
        spellData[10] = makeSpellData((int)(0.74 * BaseMana.get(80)), 3408, 3408, 1.1429f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.74 * BaseMana.get(80)), 4 * 402, 4 * 402, 4 * 0.14426f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.74 * BaseMana.get(81)), 4 * 411, 4 * 411, 4 * 0.14426f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.74 * BaseMana.get(82)), 4 * 420, 4 * 420, 4 * 0.14426f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.74 * BaseMana.get(83)), 4 * 429, 4 * 429, 4 * 0.14426f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.74 * BaseMana.get(84)), 4 * 438, 4 * 438, 4 * 0.14426f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.74 * BaseMana.get(85)), 4 * 447, 4 * 447, 4 * 0.14426f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public void initialize(Solver solver) {
        setName("Blizzard");
        initializeCastTime(true, false, 8, 0);
        initializeDamage(solver, true, 30, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()), 4, 1, 0);
        if (solver.getMageTalents().getImprovedBlizzard() > 0) {
            float fof = (solver.getMageTalents().getFingersOfFrost() == 2 ? 0.15f : 0.07f * solver.getMageTalents().getFingersOfFrost());
            fof = Math.max(fof, 0.05f * solver.getMageTalents().getFrostbite() * solver.getCalculationOptions().getFrostbiteUtilization());
            setBaseCritRate(getBaseCritRate() + (1.0f - (1.0f - fof) * (1.0f - fof)) * (solver.getMageTalents().getShatter() == 3 ? 0.5f : 0.17f * solver.getMageTalents().getShatter()));
        }
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        setDirty(false);
    }
}
