// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ArcaneExplosionTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.22 * BaseMana.get(70)), 377, 407, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.22 * BaseMana.get(71)), 378, 409, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.22 * BaseMana.get(72)), 380, 411, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.22 * BaseMana.get(73)), 381, 412, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.22 * BaseMana.get(74)), 383, 414, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.22 * BaseMana.get(75)), 385, 415, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.22 * BaseMana.get(76)), 481, 519, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.22 * BaseMana.get(77)), 483, 521, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.22 * BaseMana.get(78)), 485, 523, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.22 * BaseMana.get(79)), 487, 525, 1.5f / 3.5f * 0.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.22 * BaseMana.get(80)), 538, 582, 1.5f / 3.5f * 0.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.22 * BaseMana.get(80)), 171, 185, 0.097f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.22 * BaseMana.get(81)), 175, 189, 0.097f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.22 * BaseMana.get(82)), 179, 193, 0.097f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.22 * BaseMana.get(83)), 183, 197, 0.097f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.22 * BaseMana.get(84)), 187, 201, 0.097f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.22 * BaseMana.get(85)), 191, 205, 0.097f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public void initialize(Solver solver) {
        setName("Arcane Explosion");
        initializeCastTime(false, true, 0, 0);
        initializeDamage(solver, true, 0, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()));
        if (solver.getMageTalents().isGlyphOfArcaneExplosion()) setBaseCostAmplifier(getBaseCostAmplifier() * 0.9f);
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setDirty(false);
    }
}
