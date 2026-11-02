// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.Map;

public class Spell {
    public SpellId spellId; // set in CastingState.GetSpell
    private SpellTemplate template; // set in constructor/Initialize

    public SpellTemplate getSpellTemplate() {
        return template;
    }

    public void initialize(SpellTemplate template) {
        this.template = template;
        cycle = null;
    }

    // Variables that have to be initialized in Calculate and can be modified between Calculate and CalculateDerivedStats
    CastingState castingState;
    public float baseCastTime;
    public float costModifier;
    public float costAmplifier;
    public float spellModifier;
    public float additiveSpellModifier;
    public float directDamageModifier;
    public float dotDamageModifier;
    public float critRate;
    public float critBonus;
    public float rawSpellDamage;
    public float interruptProtection;

    // Variables that have to be initialized in CalculateDerivedStats and can be modified after
    public boolean spammedDot;
    public boolean pom;
    public float ticks;
    public float castProcs;
    public float hitProcs;
    public float critProcs;
    public float igniteProcs;
    public float targetProcs;
    public float dotProcs;
    public float channelReduction;
    public float castTime;
    public float OO5SR;
    public float averageDamage;
    public float averageThreat;
    public float averageCost;
    public float absorb; // max absorb on single impact
    public float totalAbsorb; // total absorb with combined warding negates
    public float igniteDamage;
    public float igniteDamagePerSpellPower;
    public float damagePerSpellPower;
    // stats valid for dot spells only
    public float dotAverageDamage;
    public float dotAverageThreat;
    public float dotDamagePerSpellPower;

    public float getDamagePerSecond() {
        return this.averageDamage / this.castTime;
    }

    public float getThreatPerSecond() {
        return this.averageThreat / this.castTime;
    }

    public float getCostPerSecond() {
        return this.averageCost / this.castTime;
    }

    public String label;

    // Properties pulling data directly from template
    public String getName() {
        return template.getName();
    }

    public boolean getAreaEffect() {
        return template.getAreaEffect();
    }

    public boolean getChanneled() {
        return template.getChanneled();
    }

    public float getCastProcs2() {
        return template.getCastProcs2();
    }

    public float getNukeProcs() {
        return template.getNukeProcs();
    }

    public boolean getInstant() {
        return template.getInstant();
    }

    public int getBaseCost() {
        return template.getBaseCost();
    }

    public float getBaseCooldown() {
        return template.getBaseCooldown();
    }

    public MagicSchool getMagicSchool() {
        return template.getMagicSchool();
    }

    public float getBaseMinDamage() {
        return template.getBaseMinDamage();
    }

    public float getBaseMaxDamage() {
        return template.getBaseMaxDamage();
    }

    public float getBasePeriodicDamage() {
        return template.getBasePeriodicDamage();
    }

    public float getSpellDamageCoefficient() {
        return template.getSpellDamageCoefficient();
    }

    public float getDotDamageCoefficient() {
        return template.getDotDamageCoefficient();
    }

    public float getDotDuration() {
        return template.getDotDuration();
    }

    public float getDotTickInterval() {
        return template.getDotTickInterval();
    }

    public float getRange() {
        return template.getRange();
    }

    public float getRealResistance() {
        return template.getRealResistance();
    }

    public float getThreatMultiplier() {
        return template.getThreatMultiplier();
    }

    public float getHitRate() {
        return template.getHitRate();
    }

    public float getPartialResistFactor() {
        return template.getPartialResistFactor();
    }

    public float getCooldown() {
        return template.getCooldown();
    }

    public float getCost() {
        return (float) Math.floor(getBaseCost() * this.costAmplifier * this.costModifier);
    }

    public float getABCost() {
        return (float) Math.floor(Math.rint(getBaseCost() * this.costAmplifier) * this.costModifier);
    }

    public float getLatency() {
        if (this.baseCastTime <= 1.5f || getInstant()) {
            return castingState.getCalculationOptions().getLatencyGCD();
        } else if (getChanneled()) {
            return castingState.getCalculationOptions().getLatencyChannel();
        } else {
            return castingState.getCalculationOptions().getLatencyCast();
        }
    }

    public float getMinHitDamage() {
        return (getBaseMinDamage() + this.rawSpellDamage * getSpellDamageCoefficient()) * this.spellModifier * this.directDamageModifier / template.getTicks();
    }

    public float getMaxHitDamage() {
        return (getBaseMaxDamage() + this.rawSpellDamage * getSpellDamageCoefficient()) * this.spellModifier * this.directDamageModifier / template.getTicks();
    }

    public float getMinCritDamage() {
        return getMinHitDamage() * this.critBonus;
    }

    public float getMaxCritDamage() {
        return getMaxHitDamage() * this.critBonus;
    }

    public float getDotDamage() {
        return (getBasePeriodicDamage() + getDotDamageCoefficient() * this.rawSpellDamage) * this.spellModifier * this.dotDamageModifier;
    }

    public static final float GlobalCooldownLimit = 1.0f;
    public static final float MaxHitRate = 1.0f;

    private Cycle cycle;

    public Cycle getCycle() {
        if (cycle == null) {
            boolean needsDisplayCalculations = castingState.getSolver().getNeedsDisplayCalculations();
            cycle = Cycle.createNew(needsDisplayCalculations, castingState);
            cycle.addSpell(needsDisplayCalculations, this, 1.0f);
            cycle.calculate();
            if (getAreaEffect()) {
                cycle.areaEffect = true;
                cycle.aoeSpell = this;
            }
        }
        return cycle;
    }

    public Spell() {
    }

    public Spell(SpellTemplate template) {
        this.template = template;
    }

    public static Spell createNew(SpellTemplate template, Solver solver) {
        if (solver.getNeedsDisplayCalculations() || solver.getArraySet() == null) {
            return new Spell(template);
        } else {
            Spell spell = solver.getArraySet().newSpell();
            spell.initialize(template);
            return spell;
        }
    }

    public static Spell newFromReference(Spell reference, CastingState castingState) {
        Spell s = createNew(reference.template, castingState.getSolver());
        s.castingState = castingState;

        s.baseCastTime = reference.baseCastTime;
        s.costModifier = reference.costModifier;
        s.costAmplifier = reference.costAmplifier;
        s.spellModifier = reference.spellModifier;
        s.additiveSpellModifier = reference.additiveSpellModifier;
        s.directDamageModifier = reference.directDamageModifier;
        s.dotDamageModifier = reference.dotDamageModifier;
        s.critRate = reference.critRate;
        s.critBonus = reference.critBonus;
        s.rawSpellDamage = reference.rawSpellDamage;
        s.interruptProtection = reference.interruptProtection;

        s.spammedDot = reference.spammedDot;
        s.pom = reference.pom;
        s.ticks = reference.ticks;
        s.castProcs = reference.castProcs;
        s.hitProcs = reference.hitProcs;
        s.critProcs = reference.critProcs;
        s.igniteProcs = reference.igniteProcs;
        s.targetProcs = reference.targetProcs;
        s.dotProcs = reference.dotProcs;
        s.channelReduction = reference.channelReduction;
        s.castTime = reference.castTime;
        s.OO5SR = reference.OO5SR;
        s.averageDamage = reference.averageDamage;
        s.averageThreat = reference.averageThreat;
        s.averageCost = reference.averageCost;
        s.igniteDamage = reference.igniteDamage;
        s.igniteDamagePerSpellPower = reference.igniteDamagePerSpellPower;
        s.damagePerSpellPower = reference.damagePerSpellPower;
        s.absorb = reference.absorb;
        s.totalAbsorb = reference.totalAbsorb;
        s.dotAverageDamage = reference.dotAverageDamage;
        s.dotAverageThreat = reference.dotAverageThreat;
        s.dotDamagePerSpellPower = reference.dotDamagePerSpellPower;

        s.recalculateCastTime(castingState);

        return s;
    }

    public void calculate(CastingState castingState) {
        this.castingState = castingState;

        this.baseCastTime = template.getBaseCastTime();
        this.costModifier = template.getBaseCostModifier();
        this.costAmplifier = template.getBaseCostAmplifier();
        this.directDamageModifier = template.getBaseDirectDamageModifier();
        this.dotDamageModifier = template.getBaseDotDamageModifier();
        if (castingState.getPowerInfusion()) this.costModifier -= 0.2f;
        if (castingState.getArcanePower()) this.costModifier += 0.2f;
        this.interruptProtection = template.getBaseInterruptProtection();

        this.spellModifier = template.getBaseSpellModifier() * castingState.getStateSpellModifier();
        this.additiveSpellModifier = template.getBaseAdditiveSpellModifier() + castingState.getStateAdditiveSpellModifier();
        this.critBonus = template.getCritBonus();
        this.critRate = template.getBaseCritRate() + castingState.getStateCritRate();
        if (castingState.getCombustion() && (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire)) {
            this.critRate = 3 / castingState.getCombustionDuration();
            if (getMagicSchool() == MagicSchool.Fire) {
                this.critBonus = castingState.getSolver().getCombustionFireCritBonus();
            } else if (getMagicSchool() == MagicSchool.FrostFire) {
                this.critBonus = castingState.getSolver().getCombustionFrostFireCritBonus();
            }
        }

        switch (getMagicSchool()) {
            case Arcane:
                this.rawSpellDamage = castingState.getArcaneSpellPower();
                break;
            case Fire:
                this.rawSpellDamage = castingState.getFireSpellPower();
                break;
            case FrostFire:
                this.rawSpellDamage = castingState.getFrostFireSpellPower();
                break;
            case Frost:
                this.rawSpellDamage = castingState.getFrostSpellPower();
                break;
            case Nature:
                this.rawSpellDamage = castingState.getNatureSpellPower();
                break;
            case Shadow:
                this.rawSpellDamage = castingState.getShadowSpellPower();
                break;
            case Holy:
                this.rawSpellDamage = castingState.getHolySpellPower();
                break;
        }
    }

    public void calculateManualClearcasting(boolean manualClearcasting, boolean clearcastingAveraged, boolean clearcastingActive) {
        if (manualClearcasting && !clearcastingAveraged) {
            this.critRate -= 0.15f * 0.02f * castingState.getMageTalents().getArcaneConcentration() * castingState.getMageTalents().getArcanePotency();
            if (clearcastingActive) this.critRate += 0.15f * castingState.getMageTalents().getArcanePotency();
        }
    }

    public static float procBuffUp(float procChance, float buffDuration, float triggerInterval) {
        if (triggerInterval <= 0)
            return 0;
        else
            return 1 - (float) Math.pow(1 - procChance, buffDuration / triggerInterval);
    }

    public void calculateDerivedStats(CastingState castingState) {
        calculateDerivedStats(castingState, false, false, true, false, false, false);
    }

    public void calculateDerivedStats(CastingState castingState, boolean outOfFiveSecondRule, boolean pom, boolean spammedDot) {
        calculateDerivedStats(castingState, outOfFiveSecondRule, pom, spammedDot, false, false, false);
    }

    public void calculateDerivedStats(CastingState castingState, boolean outOfFiveSecondRule, boolean pom, boolean spammedDot, boolean round, boolean forceHit, boolean forceMiss) {
        calculateDerivedStats(castingState, outOfFiveSecondRule, pom, spammedDot, round, forceHit, forceMiss, false);
    }

    public void calculateDerivedStats(CastingState castingState, boolean outOfFiveSecondRule, boolean pom, boolean spammedDot, boolean round, boolean forceHit, boolean forceMiss, boolean dotUptime) {
        rawr.base.talents.MageTalents mageTalents = castingState.getMageTalents();
        Stats baseStats = castingState.getBaseStats();
        CalculationOptionsMage calculationOptions = castingState.getCalculationOptions();

        this.spellModifier *= this.additiveSpellModifier;

        if (this.critRate < 0.0f) this.critRate = 0.0f;
        if (this.critRate > 1.0f) this.critRate = 1.0f;

        this.ticks = template.getTicks();
        this.castProcs = template.getCastProcs();
        this.hitProcs = this.ticks * getHitRate();
        this.critProcs = this.hitProcs * this.critRate;
        if ((getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire) && mageTalents.getIgnite() > 0) {
            this.igniteProcs = this.critProcs;
        } else {
            this.igniteProcs = 0;
        }
        this.targetProcs = this.hitProcs;

        this.pom = pom;
        if (getInstant()) this.interruptProtection = 1;

        float[] channelReduction = new float[1];
        this.castTime = template.calculateCastTime(castingState, this.interruptProtection, this.critRate, pom, this.baseCastTime, channelReduction);
        this.channelReduction = channelReduction[0];

        for (int i = 0; i < castingState.getSolver().getStackingNonHasteEffectCooldownsCount(); i++) {
            EffectCooldown effectCooldown = castingState.getSolver().getStackingNonHasteEffectCooldowns()[i];
            if (castingState.effectsActive(effectCooldown.mask)) {
                Stats stats = effectCooldown.specialEffect.getStats();
                for (int j = 0; j < stats.getRawSpecialEffectDataSize(); j++) {
                    SpecialEffect effect = stats.getRawSpecialEffectData()[j];
                    if (effect.getChance() == 1f && effect.getCooldown() == 0f && (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit)) {
                        if (effect.getStats().getCritRating() < 0 && effectCooldown.specialEffect.getStats().getCritRating() > 0) {
                            float critScale = castingState.getCalculationOptions().getLevelScalingFactor() / 1400f;
                            this.critRate += SpecialEffect.getAverageStackingCritRate(this.castTime, effectCooldown.specialEffect.getDuration(), getHitRate(), this.critRate, effectCooldown.specialEffect.getStats().getCritRating() * critScale, effect.getStats().getCritRating() * critScale, effect.getMaxStack());
                            if (this.critRate > 1.0f) this.critRate = 1.0f;
                        }
                    }
                }
            }
        }

        if (getDotTickInterval() > 0) {
            if (spammedDot) {
                this.dotProcs = (float) Math.floor(Math.min(this.castTime, getDotDuration()) / getDotTickInterval());
            } else {
                this.dotProcs = getDotDuration() / getDotTickInterval();
            }
        } else {
            this.dotProcs = 0;
        }

        this.spammedDot = spammedDot;
        if (this.ticks > 0 && !forceMiss) {
            if (dotUptime) {
                float[] damagePerSpellPower = new float[1];
                float[] igniteDamage = new float[1];
                float[] igniteDamagePerSpellPower = new float[1];
                this.averageDamage = calculateDirectAverageDamage(castingState.getSolver(), this.rawSpellDamage, forceHit, damagePerSpellPower, igniteDamage, igniteDamagePerSpellPower);
                this.damagePerSpellPower = damagePerSpellPower[0];
                this.igniteDamage = igniteDamage[0];
                this.igniteDamagePerSpellPower = igniteDamagePerSpellPower[0];
                this.averageThreat = this.averageDamage * getThreatMultiplier();

                float[] dotDamagePerSpellPower = new float[1];
                this.dotAverageDamage = calculateDotAverageDamage(baseStats, calculationOptions, this.rawSpellDamage, forceHit, dotDamagePerSpellPower);
                this.dotDamagePerSpellPower = dotDamagePerSpellPower[0];
                this.dotAverageThreat = this.dotAverageDamage * getThreatMultiplier();
            } else {
                float[] damagePerSpellPower = new float[1];
                float[] igniteDamage = new float[1];
                float[] igniteDamagePerSpellPower = new float[1];
                this.averageDamage = calculateAverageDamage(castingState.getSolver(), this.rawSpellDamage, spammedDot, forceHit, damagePerSpellPower, igniteDamage, igniteDamagePerSpellPower);
                this.damagePerSpellPower = damagePerSpellPower[0];
                this.igniteDamage = igniteDamage[0];
                this.igniteDamagePerSpellPower = igniteDamagePerSpellPower[0];
                this.averageThreat = this.averageDamage * getThreatMultiplier();
            }
        } else {
            this.averageDamage = 0;
            this.averageThreat = 0;
            this.damagePerSpellPower = 0;
            this.igniteDamage = 0;
            this.igniteDamagePerSpellPower = 0;
            if (dotUptime) {
                this.dotAverageDamage = 0;
                this.dotAverageThreat = 0;
                this.dotDamagePerSpellPower = 0;
            }
        }
        if (this.channelReduction != 0) {
            this.ticks *= (1 - this.channelReduction);
            this.hitProcs *= (1 - this.channelReduction);
            this.critProcs *= (1 - this.channelReduction);
            this.targetProcs *= (1 - this.channelReduction);
            this.castProcs = getCastProcs2() + (this.castProcs - getCastProcs2()) * (1 - this.channelReduction);
            this.averageDamage *= (1 - this.channelReduction);
            this.averageThreat *= (1 - this.channelReduction);
            this.damagePerSpellPower *= (1 - this.channelReduction);
        }
        this.averageCost = calculateCost(castingState.getSolver(), round);

        this.absorb = 0;
        this.totalAbsorb = 0;

        if (outOfFiveSecondRule) {
            OO5SR = 1;
        } else {
            OO5SR = 0;
        }
    }

    public void recalculateCastTime(CastingState castingState) {
        if (this.channelReduction != 0) {
            this.ticks /= (1 - this.channelReduction);
            this.hitProcs /= (1 - this.channelReduction);
            this.critProcs /= (1 - this.channelReduction);
            this.targetProcs /= (1 - this.channelReduction);
            this.castProcs = template.getCastProcs();
            this.averageDamage /= (1 - this.channelReduction);
            this.averageThreat /= (1 - this.channelReduction);
            this.damagePerSpellPower /= (1 - this.channelReduction);
        }
        float[] channelReduction = new float[1];
        this.castTime = template.calculateCastTime(castingState, this.interruptProtection, this.critRate, this.pom, this.baseCastTime, channelReduction);
        this.channelReduction = channelReduction[0];
        if (this.channelReduction != 0) {
            this.ticks *= (1 - this.channelReduction);
            this.hitProcs *= (1 - this.channelReduction);
            this.critProcs *= (1 - this.channelReduction);
            this.targetProcs *= (1 - this.channelReduction);
            this.castProcs = template.getCastProcs2() + (template.getCastProcs() - template.getCastProcs2()) * (1 - this.channelReduction);
            this.averageDamage *= (1 - this.channelReduction);
            this.averageThreat *= (1 - this.channelReduction);
            this.damagePerSpellPower *= (1 - this.channelReduction);
        }
    }

    public float calculateAverageDamage(Solver solver, float spellPower, boolean spammedDot, boolean forceHit, float[] damagePerSpellPower, float[] igniteDamage, float[] igniteDamagePerSpellPower) {
        float baseAverage = (getBaseMinDamage() + getBaseMaxDamage()) / 2f;
        float critMultiplier = 1 + (this.critBonus - 1) * Math.max(0, this.critRate);
        float resistMultiplier = (forceHit ? 1.0f : getHitRate()) * getPartialResistFactor();
        float commonMultiplier = this.spellModifier * resistMultiplier;
        float nukeMultiplier = commonMultiplier * this.directDamageModifier * critMultiplier;
        float averageDamage = baseAverage * nukeMultiplier;
        damagePerSpellPower[0] = getSpellDamageCoefficient() * nukeMultiplier;
        if (solver.getNeedsDisplayCalculations() && (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire) && solver.getMageTalents().getIgnite() > 0) {
            float igniteMultiplier = commonMultiplier * this.directDamageModifier * this.critBonus * solver.getIgniteFactor() / (1 + solver.getIgniteFactor()) * Math.max(0, this.critRate);
            igniteDamage[0] = (baseAverage + getSpellDamageCoefficient() * spellPower) * igniteMultiplier;
            igniteDamagePerSpellPower[0] = getSpellDamageCoefficient() * igniteMultiplier;
        } else {
            igniteDamage[0] = 0;
            igniteDamagePerSpellPower[0] = 0;
        }
        if (getBasePeriodicDamage() > 0.0f) {
            float dotFactor = commonMultiplier * this.dotDamageModifier;
            if (spammedDot) {
                dotFactor *= Math.min((float) Math.floor(this.castTime / getDotTickInterval()) * getDotTickInterval() / getDotDuration(), 1.0f);
            }
            averageDamage += getBasePeriodicDamage() * dotFactor;
            damagePerSpellPower[0] += getDotDamageCoefficient() * dotFactor;
        }
        return averageDamage + damagePerSpellPower[0] * spellPower;
    }

    public float calculateDirectAverageDamage(Solver solver, float spellPower, boolean forceHit, float[] damagePerSpellPower, float[] igniteDamage, float[] igniteDamagePerSpellPower) {
        float baseAverage = (getBaseMinDamage() + getBaseMaxDamage()) / 2f;
        float critMultiplier = 1 + (this.critBonus - 1) * Math.max(0, this.critRate);
        float resistMultiplier = (forceHit ? 1.0f : getHitRate()) * getPartialResistFactor();
        float commonMultiplier = this.spellModifier * resistMultiplier;
        float nukeMultiplier = commonMultiplier * this.directDamageModifier * critMultiplier;
        float averageDamage = baseAverage * nukeMultiplier;
        damagePerSpellPower[0] = getSpellDamageCoefficient() * nukeMultiplier;
        if (solver.getNeedsDisplayCalculations() && (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire) && solver.getMageTalents().getIgnite() > 0) {
            float igniteMultiplier = commonMultiplier * this.directDamageModifier * this.critBonus * solver.getIgniteFactor() / (1 + solver.getIgniteFactor()) * Math.max(0, this.critRate);
            igniteDamage[0] = (baseAverage + getSpellDamageCoefficient() * spellPower) * igniteMultiplier;
            igniteDamagePerSpellPower[0] = getSpellDamageCoefficient() * igniteMultiplier;
        } else {
            igniteDamage[0] = 0;
            igniteDamagePerSpellPower[0] = 0;
        }
        return averageDamage + damagePerSpellPower[0] * spellPower;
    }

    public float calculateDotAverageDamage(Stats baseStats, CalculationOptionsMage calculationOptions, float spellPower, boolean forceHit, float[] damagePerSpellPower) {
        float resistMultiplier = (forceHit ? 1.0f : getHitRate()) * getPartialResistFactor();
        float commonMultiplier = this.spellModifier * resistMultiplier;
        float averageDamage = 0.0f;
        damagePerSpellPower[0] = 0.0f;
        if (getBasePeriodicDamage() > 0.0f) {
            float dotFactor = commonMultiplier * this.dotDamageModifier;
            averageDamage = getBasePeriodicDamage() * dotFactor;
            damagePerSpellPower[0] = getDotDamageCoefficient() * dotFactor;
        }
        return averageDamage + damagePerSpellPower[0] * spellPower;
    }

    protected float calculateCost(Solver solver, boolean round) {
        float cost;
        if (round) {
            cost = (float) Math.floor(Math.rint(getBaseCost() * this.costAmplifier) * this.costModifier);
        } else {
            cost = (float) Math.floor(getBaseCost() * this.costAmplifier * this.costModifier);
        }

        if (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire)
            cost += this.critRate * cost * 0.01f * solver.getMageTalents().getBurnout();

        cost *= (1 - 0.02f * solver.getMageTalents().getArcaneConcentration());

        if (!solver.getCalculationOptions().getEffectDisableManaSources()) {
            cost -= this.critRate * getBaseCost() * 0.1f * solver.getMageTalents().getMasterOfElements();
            cost -= template.getBaseUntalentedCastTime() / 60f * solver.getBaseStats().getManaRestoreFromBaseManaPPM() * 3268;
        }
        return cost;
    }

    public void calculateManualClearcastingCost(Solver solver, boolean round, boolean manualClearcasting, boolean clearcastingAveraged, boolean clearcastingActive) {
        float cost;
        if (round) {
            cost = (float) Math.floor(Math.rint(getBaseCost() * this.costAmplifier) * this.costModifier);
        } else {
            cost = (float) Math.floor(getBaseCost() * this.costAmplifier * this.costModifier);
        }

        if (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire)
            cost += this.critRate * cost * 0.01f * solver.getMageTalents().getBurnout();

        if (!manualClearcasting || clearcastingAveraged) {
            cost *= (1 - 0.02f * solver.getMageTalents().getArcaneConcentration());
        } else if (clearcastingActive) {
            cost = 0;
        }

        if (!solver.getCalculationOptions().getEffectDisableManaSources()) {
            cost -= this.critRate * getBaseCost() * 0.1f * solver.getMageTalents().getMasterOfElements();
            cost -= template.getBaseUntalentedCastTime() / 60f * solver.getBaseStats().getManaRestoreFromBaseManaPPM() * 3268;
        }
        this.averageCost = cost;
    }

    public void addSpellContribution(Map<String, SpellContribution> dict, float duration, float dotUptime, float effectSpellPower) {
        SpellContribution contrib = dict.get(getName());
        if (contrib == null) {
            contrib = new SpellContribution();
            contrib.name = getName();
            dict.put(getName(), contrib);
        }
        float igniteContribution = 0;
        float critBonus = this.critBonus;
        if (this.igniteDamage > 0) {
            igniteContribution = (this.igniteDamage + effectSpellPower * this.igniteDamagePerSpellPower) / this.castTime * duration;
            SpellContribution igniteContrib = dict.get("Ignite");
            if (igniteContrib == null) {
                igniteContrib = new SpellContribution();
                igniteContrib.name = "Ignite";
                dict.put("Ignite", igniteContrib);
            }
            igniteContrib.damage += igniteContribution;
            critBonus /= (1 + castingState.getSolver().getIgniteFactor());
        }
        contrib.hits += (this.hitProcs - this.critProcs) * duration / this.castTime;
        contrib.crits += this.critProcs * duration / this.castTime;
        float damage = (this.averageDamage + effectSpellPower * this.damagePerSpellPower) / this.castTime * duration - igniteContribution;
        contrib.damage += damage;
        if (dotUptime > 0) {
            float tickDamage = dotUptime * (this.dotAverageDamage + effectSpellPower * this.dotDamagePerSpellPower) / this.castTime * duration;
            contrib.damage += tickDamage;
            contrib.ticks += dotUptime * (getDotDuration() / getDotTickInterval()) * duration / this.castTime;
            contrib.tickDamage += tickDamage;
        } else {
            if (getDotTickInterval() > 0) {
                contrib.ticks += this.dotProcs * duration / this.castTime;
                float dotFactor = this.dotProcs / (getDotDuration() / getDotTickInterval()) * this.spellModifier * this.dotDamageModifier * getPartialResistFactor();
                float tickDamage = dotFactor * (getBasePeriodicDamage() + (this.rawSpellDamage + effectSpellPower) * getDotDamageCoefficient()) / this.castTime * duration;
                contrib.tickDamage += tickDamage;
                damage -= tickDamage;
            }
        }
        float baseDamage = damage / (1 + (critBonus - 1) * this.critRate);
        contrib.hitDamage += baseDamage * (1 - this.critRate);
        contrib.critDamage += baseDamage * this.critRate * critBonus;
        contrib.range = getRange();
    }

    public void addManaUsageContribution(Map<String, Float> dict, float duration) {
        float contrib = dict.containsKey(getName()) ? dict.get(getName()) : 0f;
        contrib += this.averageCost / this.castTime * duration;
        dict.put(getName(), contrib);
    }
}
