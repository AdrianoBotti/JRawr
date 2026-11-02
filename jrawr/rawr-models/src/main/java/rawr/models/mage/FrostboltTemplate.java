// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FrostboltTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.11 * BaseMana.get(70)), 630, 680, 3.0f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.11 * BaseMana.get(71)), 633, 684, 3.0f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.11 * BaseMana.get(72)), 637, 688, 3.0f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.11 * BaseMana.get(73)), 641, 692, 3.0f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.11 * BaseMana.get(74)), 645, 696, 3.0f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.11 * BaseMana.get(75)), 702, 758, 3.0f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.11 * BaseMana.get(76)), 706, 763, 3.0f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.11 * BaseMana.get(77)), 710, 767, 3.0f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.11 * BaseMana.get(78)), 714, 771, 3.0f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.11 * BaseMana.get(79)), 799, 861, 3.0f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.11 * BaseMana.get(80)), 803, 866, 3.0f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.11 * BaseMana.get(80)), 1029, 1111, 0.715f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.11 * BaseMana.get(81)), 1052, 1136, 0.715f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.11 * BaseMana.get(82)), 1076, 1162, 0.715f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.11 * BaseMana.get(83)), 1098, 1186, 0.715f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.11 * BaseMana.get(84)), 1121, 1211, 0.715f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.11 * BaseMana.get(85)), 1144, 1236, 0.715f, 0, 0);
    }

    private float fingersOfFrostCritRate;
    private float tormentTheWeak;

    public Spell getSpell(CastingState castingState, boolean manualClearcasting, boolean clearcastingActive, boolean pom, boolean averageFingersOfFrost) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        if (manualClearcasting) spell.calculateManualClearcasting(true, false, clearcastingActive);
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        if (averageFingersOfFrost) {
            spell.critRate += fingersOfFrostCritRate;
        }
        spell.calculateDerivedStats(castingState, false, pom, false);
        if (manualClearcasting) spell.calculateManualClearcastingCost(castingState.getSolver(), false, true, false, clearcastingActive);
        return spell;
    }

    public Spell getSpell(CastingState castingState, boolean averageFingersOfFrost) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        if (averageFingersOfFrost) {
            spell.critRate += fingersOfFrostCritRate;
        }
        spell.calculateDerivedStats(castingState);
        return spell;
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        return getSpell(castingState, false);
    }

    public void initialize(Solver solver) {
        setName("Frostbolt");
        initializeCastTime(false, false, 3, 0);
        initializeDamage(solver, false, 30, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()));
        if (solver.getMageTalents().isGlyphOfFrostbolt()) {
            setBaseDirectDamageModifier(getBaseDirectDamageModifier() * 1.05f);
        }
        setBaseCritRate(getBaseCritRate() + 0.01f * solver.getMageTalents().getWintersChill());
        setBaseCastTime(getBaseCastTime() - 0.1f * solver.getMageTalents().getImprovedFrostbolt());
        setBaseCastTime(getBaseCastTime() - 0.1f * solver.getMageTalents().getEmpoweredFrostbolt());
        setBaseCritRate(getBaseCritRate() + 0.05f * solver.getBaseStats().getMage4T9());
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.05f * solver.getMageTalents().getEmpoweredFrostbolt());
        setBaseSpellModifier(getBaseSpellModifier() * (1 + 0.01f * solver.getMageTalents().getChilledToTheBone()));
        float fof = (solver.getMageTalents().getFingersOfFrost() == 2 ? 0.15f : 0.07f * solver.getMageTalents().getFingersOfFrost());
        fingersOfFrostCritRate = (1.0f - (1.0f - fof) * (1.0f - fof)) * (solver.getMageTalents().getShatter() == 3 ? 0.5f : 0.17f * solver.getMageTalents().getShatter());
        tormentTheWeak = 0.04f * solver.getMageTalents().getTormentTheWeak();
        setNukeProcs(1);
        setDirty(false);
    }
}
