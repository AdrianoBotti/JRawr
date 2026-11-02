// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ArcaneBlastTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.07 * BaseMana.get(70)), 668, 772, 2.5f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.07 * BaseMana.get(71)), 690, 800, 2.5f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.07 * BaseMana.get(72)), 695, 806, 2.5f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.07 * BaseMana.get(73)), 700, 811, 2.5f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.07 * BaseMana.get(74)), 705, 816, 2.5f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.07 * BaseMana.get(75)), 711, 822, 2.5f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.07 * BaseMana.get(76)), 805, 935, 2.5f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.07 * BaseMana.get(77)), 811, 942, 2.5f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.07 * BaseMana.get(78)), 817, 948, 2.5f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.07 * BaseMana.get(79)), 823, 954, 2.5f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.07 * BaseMana.get(80)), 1185, 1377, 2.5f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.07 * BaseMana.get(80)), 1068, 1240, 0.714f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.07 * BaseMana.get(81)), 1092, 1268, 0.714f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.07 * BaseMana.get(82)), 1117, 1297, 0.714f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.07 * BaseMana.get(83)), 1140, 1324, 0.714f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.07 * BaseMana.get(84)), 1164, 1352, 0.714f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.07 * BaseMana.get(85)), 1188, 1380, 0.714f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    public Spell getSpell(CastingState castingState, int debuff, boolean manualClearcasting, boolean clearcastingActive, boolean pom) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        if (manualClearcasting) spell.calculateManualClearcasting(true, false, clearcastingActive);
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * debuff;
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.costModifier += 1.75f * debuff;
        spell.calculateDerivedStats(castingState, false, pom, false, true, false, false);
        if (manualClearcasting) spell.calculateManualClearcastingCost(castingState.getSolver(), false, true, false, clearcastingActive);
        return spell;
    }

    public Spell getSpell(CastingState castingState, int debuff, boolean forceHit) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * debuff;
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.costModifier += 1.75f * debuff;
        spell.calculateDerivedStats(castingState, false, false, false, true, forceHit, !forceHit);
        return spell;
    }

    public Spell getSpell(CastingState castingState, int debuff) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * debuff;
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.costModifier += 1.75f * debuff;
        spell.calculateDerivedStats(castingState, false, false, false, true, false, false);
        return spell;
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.calculateDerivedStats(castingState, false, false, false, true, false, false);
        return spell;
    }

    public void addToCycle(Solver solver, Cycle cycle, Spell rawSpell, float weight0, float weight1, float weight2, float weight3) {
        rawr.base.talents.MageTalents mageTalents = solver.getMageTalents();
        float weight = weight0 + weight1 + weight2 + weight3;
        cycle.castTime += weight * rawSpell.castTime;
        cycle.castProcs += weight * rawSpell.castProcs;
        cycle.castProcs2 += weight * rawSpell.getCastProcs2();
        cycle.nukeProcs += weight * rawSpell.getNukeProcs();
        cycle.ticks += weight * rawSpell.ticks;
        cycle.hitProcs += weight * rawSpell.hitProcs;
        cycle.critProcs += weight * rawSpell.critProcs;
        cycle.targetProcs += weight * rawSpell.targetProcs;
        cycle.damageProcs += weight * rawSpell.hitProcs;

        double roundCost = Math.rint(rawSpell.getBaseCost() * rawSpell.costAmplifier);
        cycle.costPerSecond += (1 - 0.02f * mageTalents.getArcaneConcentration()) * (weight0 * (float) Math.floor(roundCost * rawSpell.costModifier) + weight1 * (float) Math.floor(roundCost * (rawSpell.costModifier + 1.75f)) + weight2 * (float) Math.floor(roundCost * (rawSpell.costModifier + 3.50f)) + weight3 * (float) Math.floor(roundCost * (rawSpell.costModifier + 5.25f)));
        cycle.costPerSecond -= weight * rawSpell.critRate * rawSpell.getBaseCost() * 0.1f * mageTalents.getMasterOfElements();
        cycle.costPerSecond -= weight * getBaseUntalentedCastTime() / 60f * solver.getBaseStats().getManaRestoreFromBaseManaPPM() * 3268;

        float multiplier = (weight * rawSpell.additiveSpellModifier + arcaneBlastDamageMultiplier * (weight1 + 2 * weight2 + 3 * weight3)) / rawSpell.additiveSpellModifier;
        cycle.dpsPerSpellPower += multiplier * rawSpell.damagePerSpellPower;
        cycle.damagePerSecond += multiplier * rawSpell.averageDamage;
        cycle.threatPerSecond += multiplier * rawSpell.averageThreat;
    }

    public void addToCycle(Solver solver, Cycle cycle, Spell rawSpell, float weight0, float weight1, float weight2, float weight3, float weight4) {
        rawr.base.talents.MageTalents mageTalents = solver.getMageTalents();
        float weight = weight0 + weight1 + weight2 + weight3 + weight4;
        cycle.castTime += weight * rawSpell.castTime;
        cycle.castProcs += weight * rawSpell.castProcs;
        cycle.castProcs2 += weight * rawSpell.getCastProcs2();
        cycle.nukeProcs += weight * rawSpell.getNukeProcs();
        cycle.ticks += weight * rawSpell.ticks;
        cycle.hitProcs += weight * rawSpell.hitProcs;
        cycle.critProcs += weight * rawSpell.critProcs;
        cycle.targetProcs += weight * rawSpell.targetProcs;
        cycle.damageProcs += weight * rawSpell.hitProcs;

        double roundCost = Math.rint(rawSpell.getBaseCost() * rawSpell.costAmplifier);
        cycle.costPerSecond += (1 - 0.02f * mageTalents.getArcaneConcentration()) * (weight0 * (float) Math.floor(roundCost * rawSpell.costModifier) + weight1 * (float) Math.floor(roundCost * (rawSpell.costModifier + 1.75f)) + weight2 * (float) Math.floor(roundCost * (rawSpell.costModifier + 3.50f)) + weight3 * (float) Math.floor(roundCost * (rawSpell.costModifier + 5.25f)) + weight4 * (float) Math.floor(roundCost * (rawSpell.costModifier + 7.00f)));
        cycle.costPerSecond -= weight * rawSpell.critRate * rawSpell.getBaseCost() * 0.1f * mageTalents.getMasterOfElements();
        cycle.costPerSecond -= weight * getBaseUntalentedCastTime() / 60f * solver.getBaseStats().getManaRestoreFromBaseManaPPM() * 3268;

        float multiplier = (weight * rawSpell.additiveSpellModifier + arcaneBlastDamageMultiplier * (weight1 + 2 * weight2 + 3 * weight3 + 4 * weight4)) / rawSpell.additiveSpellModifier;
        cycle.dpsPerSpellPower += multiplier * rawSpell.damagePerSpellPower;
        cycle.damagePerSecond += multiplier * rawSpell.averageDamage;
        cycle.threatPerSecond += multiplier * rawSpell.averageThreat;
    }

    private float arcaneBlastDamageMultiplier;
    private float tormentTheWeak;

    public void initialize(Solver solver) {
        setName("Arcane Blast");
        initializeCastTime(false, false, 2.5f, 0);
        initializeDamage(solver, false, 30, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()));
        Stats baseStats = solver.getBaseStats();
        rawr.base.talents.MageTalents mageTalents = solver.getMageTalents();
        setBaseInterruptProtection(getBaseInterruptProtection() + 0.2f * mageTalents.getArcaneStability());
        setBaseCritRate(getBaseCritRate() + 0.05f * solver.getBaseStats().getMage4T9());
        arcaneBlastDamageMultiplier = mageTalents.isGlyphOfArcaneBlast() ? 0.18f : 0.15f;
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * mageTalents.getSpellImpact());
        tormentTheWeak = 0.04f * mageTalents.getTormentTheWeak();
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.03f * mageTalents.getArcaneEmpowerment());
        setBaseCritRate(getBaseCritRate() + 0.02f * mageTalents.getIncineration());
        setNukeProcs(1);
        setDirty(false);
    }
}
