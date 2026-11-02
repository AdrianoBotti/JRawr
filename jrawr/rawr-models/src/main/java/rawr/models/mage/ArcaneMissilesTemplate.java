// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ArcaneMissilesTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.31 * BaseMana.get(70)), 287.9f * 5, 287.9f * 5, 5f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.31 * BaseMana.get(71)), 289.1f * 5, 289.1f * 5, 4.67125f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.31 * BaseMana.get(72)), 290.8f * 5, 290.8f * 5, 4.3425f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.31 * BaseMana.get(73)), 291.9f * 5, 291.9f * 5, 4.01375f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.31 * BaseMana.get(74)), 293.0f * 5, 293.0f * 5, 3.685f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.31 * BaseMana.get(75)), 320.0f * 5, 320.0f * 5, 5f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.31 * BaseMana.get(76)), 321.7f * 5, 321.7f * 5, 5f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.31 * BaseMana.get(77)), 323.0f * 5, 323.0f * 5, 5f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.31 * BaseMana.get(78)), 324.8f * 5, 324.8f * 5, 5f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.31 * BaseMana.get(79)), 360.0f * 5, 360.0f * 5, 5f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.31 * BaseMana.get(80)), 361.9f * 5, 361.9f * 5, 5f / 3.5f, 0, 0);
        spellData[0] = makeSpellData((int)(0.00 * BaseMana.get(80)), 310.0f * 5, 310.0f * 5, 5 * 0.286f, 0, 0);
        spellData[1] = makeSpellData((int)(0.00 * BaseMana.get(81)), 318.0f * 5, 318.0f * 5, 5 * 0.286f, 0, 0);
        spellData[2] = makeSpellData((int)(0.00 * BaseMana.get(82)), 325.0f * 5, 325.0f * 5, 5 * 0.286f, 0, 0);
        spellData[3] = makeSpellData((int)(0.00 * BaseMana.get(83)), 334.0f * 5, 334.0f * 5, 5 * 0.286f, 0, 0);
        spellData[4] = makeSpellData((int)(0.00 * BaseMana.get(84)), 342.0f * 5, 342.0f * 5, 5 * 0.286f, 0, 0);
        spellData[5] = makeSpellData((int)(0.00 * BaseMana.get(85)), 350.0f * 5, 350.0f * 5, 5 * 0.286f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellData[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    public Spell getSpell(CastingState castingState, boolean barrage, boolean clearcastingAveraged, boolean clearcastingActive, boolean clearcastingProccing, int arcaneBlastDebuff, float ticks) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.calculateManualClearcasting(true, clearcastingAveraged, clearcastingActive);
        spell.baseCastTime = ticks;
        if (barrage) {
            spell.baseCastTime *= 0.5f;
            spell.costModifier = Math.max(spell.costModifier - 1, 0);
        }
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * arcaneBlastDebuff;
        spell.spellModifier *= ticks / 5.0f;
        spell.calculateDerivedStats(castingState);
        spell.calculateManualClearcastingCost(castingState.getSolver(), false, true, clearcastingAveraged, clearcastingActive);
        return spell;
    }

    public Spell getSpell(CastingState castingState, boolean barrage, int arcaneBlastDebuff) {
        return getSpell(castingState, barrage, arcaneBlastDebuff, 5);
    }

    public Spell getSpell(CastingState castingState, boolean barrage, int arcaneBlastDebuff, int ticks) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.baseCastTime = ticks;
        if (barrage) {
            spell.baseCastTime *= 0.5f;
            spell.costModifier = Math.max(spell.costModifier - 1, 0);
        }
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * arcaneBlastDebuff;
        spell.spellModifier *= ticks / 5.0f;
        spell.calculateDerivedStats(castingState);
        return spell;
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
        cycle.costPerSecond += weight * rawSpell.averageCost;
        cycle.damageProcs += weight * rawSpell.hitProcs;
        float multiplier = (weight * rawSpell.additiveSpellModifier + arcaneBlastDamageMultiplier * (weight1 + 2 * weight2 + 3 * weight3 + 4 * weight4)) / rawSpell.additiveSpellModifier;
        cycle.dpsPerSpellPower += multiplier * rawSpell.damagePerSpellPower;
        cycle.damagePerSecond += multiplier * rawSpell.averageDamage;
        cycle.threatPerSecond += multiplier * rawSpell.averageThreat;
    }

    private float tormentTheWeak;
    private float arcaneBlastDamageMultiplier;

    public void initialize(Solver solver) {
        setName("Arcane Missiles");
        initializeCastTime(true, false, 5, 0);
        if (solver.getCalculationOptions().getBeta()) {
            initializeDamage(solver, false, 35, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()), 5, 6, 0);
        } else {
            initializeDamage(solver, false, 30, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()), 5, 6, 0);
        }
        setCastProcs2(1);
        if (solver.getMageTalents().isGlyphOfArcaneMissiles()) {
            setCritBonus((1 + (1.5f * (1 + solver.getBaseStats().getBonusSpellCritMultiplier()) - 1) * (1 + 0.25f * solver.getMageTalents().getSpellPower() + 0.1f * solver.getMageTalents().getBurnout() + solver.getBaseStats().getCritBonusDamage() + 0.25f)));
        }
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.15f * solver.getMageTalents().getArcaneEmpowerment());
        tormentTheWeak = 0.04f * solver.getMageTalents().getTormentTheWeak();
        arcaneBlastDamageMultiplier = solver.getMageTalents().isGlyphOfArcaneBlast() ? 0.18f : 0.15f;
        setBaseInterruptProtection(getBaseInterruptProtection() + 0.2f * solver.getMageTalents().getArcaneStability());
        setBaseCritRate(getBaseCritRate() + 0.05f * solver.getBaseStats().getMage4T9());
        setBaseCritRate(getBaseCritRate() - 0.8f * 0.15f * 0.02f * solver.getMageTalents().getArcaneConcentration() * solver.getMageTalents().getArcanePotency());
        setDirty(false);
    }
}
