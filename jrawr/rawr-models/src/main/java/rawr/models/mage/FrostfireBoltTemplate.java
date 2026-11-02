// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FrostfireBoltTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.14 * BaseMana.get(70)), 0, 0, 0f, 0, 0);
        spellData[1] = makeSpellData((int)(0.14 * BaseMana.get(71)), 0, 0, 0f, 0, 0);
        spellData[2] = makeSpellData((int)(0.14 * BaseMana.get(72)), 0, 0, 0f, 0, 0);
        spellData[3] = makeSpellData((int)(0.14 * BaseMana.get(73)), 0, 0, 0f, 0, 0);
        spellData[4] = makeSpellData((int)(0.14 * BaseMana.get(74)), 0, 0, 0f, 0, 0);
        spellData[5] = makeSpellData((int)(0.14 * BaseMana.get(75)), 629, 731, 3.0f / 3.5f, 60, 0);
        spellData[6] = makeSpellData((int)(0.14 * BaseMana.get(76)), 632, 735, 3.0f / 3.5f, 60, 0);
        spellData[7] = makeSpellData((int)(0.14 * BaseMana.get(77)), 636, 739, 3.0f / 3.5f, 60, 0);
        spellData[8] = makeSpellData((int)(0.14 * BaseMana.get(78)), 640, 743, 3.0f / 3.5f, 60, 0);
        spellData[9] = makeSpellData((int)(0.14 * BaseMana.get(79)), 644, 747, 3.0f / 3.5f, 60, 0);
        spellData[10] = makeSpellData((int)(0.14 * BaseMana.get(80)), 722, 838, 3.0f / 3.5f, 90, 0);
        spellDataBeta[0] = makeSpellData((int)(0.14 * BaseMana.get(80)), 581, 675, 0.857f, 81, 0);
        spellDataBeta[1] = makeSpellData((int)(0.14 * BaseMana.get(81)), 594, 690, 0.857f, 82, 0);
        spellDataBeta[2] = makeSpellData((int)(0.14 * BaseMana.get(82)), 608, 706, 0.857f, 84, 0);
        spellDataBeta[3] = makeSpellData((int)(0.14 * BaseMana.get(83)), 620, 720, 0.857f, 85, 0);
        spellDataBeta[4] = makeSpellData((int)(0.14 * BaseMana.get(84)), 633, 735, 0.857f, 87, 0);
        spellDataBeta[5] = makeSpellData((int)(0.14 * BaseMana.get(85)), 646, 750, 0.857f, 88, 0);
    }

    private float tormentFactor;
    private float fingersOfFrostCritRate;

    public Spell getSpell(CastingState castingState, boolean pom, boolean averageFingersOfFrost, boolean brainFreeze) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.spellModifier *= (1 + tormentFactor * castingState.getSnaredTime());
        if (averageFingersOfFrost) {
            spell.critRate += fingersOfFrostCritRate;
        }
        if (brainFreeze) {
            spell.costAmplifier = 0;
        }
        spell.calculateDerivedStats(castingState, false, pom || brainFreeze, true);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Frostfire Bolt");
        initializeCastTime(false, false, 3.0f, 0);
        initializeDamage(solver, false, 40, MagicSchool.FrostFire, getMaxRankSpellData(solver.getCalculationOptions()));
        if (solver.getMageTalents().isGlyphOfFrostfire()) {
            setBaseCritRate(getBaseCritRate() + 0.02f);
            setBaseDirectDamageModifier(getBaseDirectDamageModifier() * 1.02f);
        }
        setBaseCritRate(getBaseCritRate() + 0.01f * solver.getMageTalents().getImprovedScorch() + 0.05f * solver.getBaseStats().getMage4T9());
        tormentFactor = 0.04f * solver.getMageTalents().getTormentTheWeak();
        setBaseSpellModifier(getBaseSpellModifier() * (1 + 0.01f * solver.getMageTalents().getChilledToTheBone()));
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.05f * solver.getMageTalents().getEmpoweredFire());
        setDotDuration(9);
        setDotTickInterval(3);
        float fof = (solver.getMageTalents().getFingersOfFrost() == 2 ? 0.15f : 0.07f * solver.getMageTalents().getFingersOfFrost());
        fingersOfFrostCritRate = (1.0f - (1.0f - fof) * (1.0f - fof)) * (solver.getMageTalents().getShatter() == 3 ? 0.5f : 0.17f * solver.getMageTalents().getShatter());
        setNukeProcs(1);
        setDirty(false);
    }
}
