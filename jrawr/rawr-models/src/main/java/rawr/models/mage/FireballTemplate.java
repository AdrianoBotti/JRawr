// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FireballTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.19 * BaseMana.get(70)), 717, 913, 3.5f / 3.5f, 92, 0);
        spellData[1] = makeSpellData((int)(0.19 * BaseMana.get(71)), 721, 918, 3.5f / 3.5f, 92, 0);
        spellData[2] = makeSpellData((int)(0.19 * BaseMana.get(72)), 725, 922, 3.5f / 3.5f, 92, 0);
        spellData[3] = makeSpellData((int)(0.19 * BaseMana.get(73)), 729, 926, 3.5f / 3.5f, 92, 0);
        spellData[4] = makeSpellData((int)(0.19 * BaseMana.get(74)), 783, 997, 3.5f / 3.5f, 100, 0);
        spellData[5] = makeSpellData((int)(0.19 * BaseMana.get(75)), 787, 1002, 3.5f / 3.5f, 100, 0);
        spellData[6] = makeSpellData((int)(0.19 * BaseMana.get(76)), 792, 1007, 3.5f / 3.5f, 100, 0);
        spellData[7] = makeSpellData((int)(0.19 * BaseMana.get(77)), 796, 1011, 3.5f / 3.5f, 100, 0);
        spellData[8] = makeSpellData((int)(0.19 * BaseMana.get(78)), 888, 1132, 3.5f / 3.5f, 116, 0);
        spellData[9] = makeSpellData((int)(0.19 * BaseMana.get(79)), 893, 1138, 3.5f / 3.5f, 116, 0);
        spellData[10] = makeSpellData((int)(0.19 * BaseMana.get(80)), 898, 1143, 3.5f / 3.5f, 116, 0);
        spellDataBeta[0] = makeSpellData((int)(0.19 * BaseMana.get(80)), 741, 943, 1.000f, 116, 0);
        spellDataBeta[1] = makeSpellData((int)(0.19 * BaseMana.get(81)), 758, 966, 1.000f, 120, 0);
        spellDataBeta[2] = makeSpellData((int)(0.19 * BaseMana.get(82)), 775, 987, 1.000f, 124, 0);
        spellDataBeta[3] = makeSpellData((int)(0.19 * BaseMana.get(83)), 792, 1012, 1.000f, 128, 0);
        spellDataBeta[4] = makeSpellData((int)(0.19 * BaseMana.get(84)), 809, 1035, 1.000f, 132, 0);
        spellDataBeta[5] = makeSpellData((int)(0.19 * BaseMana.get(85)), 826, 1058, 1.000f, 136, 0);
    }

    private float tormentTheWeak;

    public Spell getSpell(CastingState castingState, boolean pom, boolean brainFreeze) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        if (brainFreeze) {
            spell.costAmplifier = 0;
        }
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.calculateDerivedStats(castingState, false, pom || brainFreeze, true);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Fireball");
        initializeCastTime(false, false, 3.5f, 0);
        initializeDamage(solver, false, 35, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        if (solver.getMageTalents().isGlyphOfFireball()) {
            setBasePeriodicDamage(0.0f);
            setBaseCastTime(getBaseCastTime() - 0.15f);
        }
        setBaseCritRate(getBaseCritRate() + 0.01f * solver.getMageTalents().getImprovedScorch() + 0.05f * solver.getBaseStats().getMage4T9());
        setDotDuration(8);
        setDotTickInterval(2);
        setBaseCastTime(getBaseCastTime() - 0.1f * solver.getMageTalents().getImprovedFireball());
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.05f * solver.getMageTalents().getEmpoweredFire());
        tormentTheWeak = 0.04f * solver.getMageTalents().getTormentTheWeak();
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setNukeProcs(1);
        setDirty(false);
    }
}
