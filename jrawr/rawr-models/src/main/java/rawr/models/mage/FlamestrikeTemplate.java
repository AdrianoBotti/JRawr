// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FlamestrikeTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.53 * BaseMana.get(70)), 480, 585, 0.2427f, 424, 4 * 0.122f);
        spellData[1] = makeSpellData((int)(0.53 * BaseMana.get(71)), 480, 585, 0.2427f, 424, 4 * 0.122f);
        spellData[2] = makeSpellData((int)(0.53 * BaseMana.get(72)), 688, 842, 0.2427f, 620, 4 * 0.122f);
        spellData[3] = makeSpellData((int)(0.53 * BaseMana.get(73)), 690, 845, 0.2427f, 620, 4 * 0.122f);
        spellData[4] = makeSpellData((int)(0.53 * BaseMana.get(74)), 693, 848, 0.2427f, 620, 4 * 0.122f);
        spellData[5] = makeSpellData((int)(0.53 * BaseMana.get(75)), 696, 851, 0.2427f, 620, 4 * 0.122f);
        spellData[6] = makeSpellData((int)(0.53 * BaseMana.get(76)), 699, 854, 0.2427f, 620, 4 * 0.122f);
        spellData[7] = makeSpellData((int)(0.53 * BaseMana.get(77)), 699, 854, 0.2427f, 620, 4 * 0.122f);
        spellData[8] = makeSpellData((int)(0.53 * BaseMana.get(78)), 699, 854, 0.2427f, 620, 4 * 0.122f);
        spellData[9] = makeSpellData((int)(0.53 * BaseMana.get(79)), 873, 1067, 0.2427f, 780, 4 * 0.122f);
        spellData[10] = makeSpellData((int)(0.53 * BaseMana.get(80)), 876, 1071, 0.2427f, 780, 4 * 0.122f);
        spellDataBeta[0] = makeSpellData((int)(0.53 * BaseMana.get(80)), 417, 509, 0.1222f, 343, 0.254f);
        spellDataBeta[1] = makeSpellData((int)(0.53 * BaseMana.get(81)), 426, 520, 0.1222f, 343, 0.254f);
        spellDataBeta[2] = makeSpellData((int)(0.53 * BaseMana.get(82)), 436, 532, 0.1222f, 343, 0.254f);
        spellDataBeta[3] = makeSpellData((int)(0.53 * BaseMana.get(83)), 444, 542, 0.1222f, 343, 0.254f);
        spellDataBeta[4] = makeSpellData((int)(0.53 * BaseMana.get(84)), 453, 553, 0.1222f, 343, 0.254f);
        spellDataBeta[5] = makeSpellData((int)(0.53 * BaseMana.get(85)), 462, 564, 0.1222f, 343, 0.254f);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public Spell getSpell(CastingState castingState, boolean spammedDot) {
        AoeSpell spell = new AoeSpell(this);
        spell.calculate(castingState);
        spell.calculateDerivedStats(castingState, false, false, spammedDot);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Flamestrike");
        initializeCastTime(false, false, 2, 0);
        initializeDamage(solver, true, 30, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()), 1, 1, 8f);
        setDotTickInterval(2);
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        setDirty(false);
    }
}
