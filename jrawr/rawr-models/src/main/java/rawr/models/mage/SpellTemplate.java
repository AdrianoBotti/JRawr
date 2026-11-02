// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellTemplate.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.talents.MageTalents;
import rawr.base.stats.Stats;
import rawr.base.stats.StatConversion;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;

public abstract class SpellTemplate {
    private String name;
    private boolean channeled;
    private boolean instant;
    private float baseCastTime;
    private float baseUntalentedCastTime;
    private float baseCooldown;
    private float cooldown;
    private boolean areaEffect;
    private int baseCost;
    private MagicSchool magicSchool;
    private float ticks;
    private float castProcs;
    private float castProcs2;
    private float baseMinDamage;
    private float baseMaxDamage;
    private float spellDamageCoefficient;
    private float basePeriodicDamage;
    private float dotDamageCoefficient;
    private float dotDuration;
    private float baseDirectDamageModifier;
    private float baseDotDamageModifier;
    private float baseCostModifier;
    private float baseCostAmplifier;
    private float baseInterruptProtection;
    private float baseSpellModifier;
    private float baseAdditiveSpellModifier;
    private float baseCritRate;
    private float critBonus;
    private float hitRate;
    private float threatMultiplier;
    private float range;
    private float realResistance;
    private float partialResistFactor;
    private float dotTickInterval;
    private float nukeProcs;
    private boolean dirty = true;

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public boolean getChanneled() {
        return this.channeled;
    }

    public void setChanneled(boolean value) {
        this.channeled = value;
    }

    public boolean getInstant() {
        return this.instant;
    }

    public void setInstant(boolean value) {
        this.instant = value;
    }

    public float getBaseCastTime() {
        return this.baseCastTime;
    }

    public void setBaseCastTime(float value) {
        this.baseCastTime = value;
    }

    public float getBaseUntalentedCastTime() {
        return this.baseUntalentedCastTime;
    }

    public void setBaseUntalentedCastTime(float value) {
        this.baseUntalentedCastTime = value;
    }

    public float getBaseCooldown() {
        return this.baseCooldown;
    }

    public void setBaseCooldown(float value) {
        this.baseCooldown = value;
    }

    public float getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(float value) {
        this.cooldown = value;
    }

    public boolean getAreaEffect() {
        return this.areaEffect;
    }

    public void setAreaEffect(boolean value) {
        this.areaEffect = value;
    }

    public int getBaseCost() {
        return this.baseCost;
    }

    public void setBaseCost(int value) {
        this.baseCost = value;
    }

    public MagicSchool getMagicSchool() {
        return this.magicSchool;
    }

    public void setMagicSchool(MagicSchool value) {
        this.magicSchool = value;
    }

    public float getTicks() {
        return this.ticks;
    }

    public void setTicks(float value) {
        this.ticks = value;
    }

    public float getCastProcs() {
        return this.castProcs;
    }

    public void setCastProcs(float value) {
        this.castProcs = value;
    }

    public float getCastProcs2() {
        return this.castProcs2;
    }

    public void setCastProcs2(float value) {
        this.castProcs2 = value;
    }

    public float getBaseMinDamage() {
        return this.baseMinDamage;
    }

    public void setBaseMinDamage(float value) {
        this.baseMinDamage = value;
    }

    public float getBaseMaxDamage() {
        return this.baseMaxDamage;
    }

    public void setBaseMaxDamage(float value) {
        this.baseMaxDamage = value;
    }

    public float getSpellDamageCoefficient() {
        return this.spellDamageCoefficient;
    }

    public void setSpellDamageCoefficient(float value) {
        this.spellDamageCoefficient = value;
    }

    public float getBasePeriodicDamage() {
        return this.basePeriodicDamage;
    }

    public void setBasePeriodicDamage(float value) {
        this.basePeriodicDamage = value;
    }

    public float getDotDamageCoefficient() {
        return this.dotDamageCoefficient;
    }

    public void setDotDamageCoefficient(float value) {
        this.dotDamageCoefficient = value;
    }

    public float getDotDuration() {
        return this.dotDuration;
    }

    public void setDotDuration(float value) {
        this.dotDuration = value;
    }

    public float getBaseDirectDamageModifier() {
        return this.baseDirectDamageModifier;
    }

    public void setBaseDirectDamageModifier(float value) {
        this.baseDirectDamageModifier = value;
    }

    public float getBaseDotDamageModifier() {
        return this.baseDotDamageModifier;
    }

    public void setBaseDotDamageModifier(float value) {
        this.baseDotDamageModifier = value;
    }

    public float getBaseCostModifier() {
        return this.baseCostModifier;
    }

    public void setBaseCostModifier(float value) {
        this.baseCostModifier = value;
    }

    public float getBaseCostAmplifier() {
        return this.baseCostAmplifier;
    }

    public void setBaseCostAmplifier(float value) {
        this.baseCostAmplifier = value;
    }

    public float getBaseInterruptProtection() {
        return this.baseInterruptProtection;
    }

    public void setBaseInterruptProtection(float value) {
        this.baseInterruptProtection = value;
    }

    public float getBaseSpellModifier() {
        return this.baseSpellModifier;
    }

    public void setBaseSpellModifier(float value) {
        this.baseSpellModifier = value;
    }

    public float getBaseAdditiveSpellModifier() {
        return this.baseAdditiveSpellModifier;
    }

    public void setBaseAdditiveSpellModifier(float value) {
        this.baseAdditiveSpellModifier = value;
    }

    public float getBaseCritRate() {
        return this.baseCritRate;
    }

    public void setBaseCritRate(float value) {
        this.baseCritRate = value;
    }

    public float getCritBonus() {
        return this.critBonus;
    }

    public void setCritBonus(float value) {
        this.critBonus = value;
    }

    public float getHitRate() {
        return this.hitRate;
    }

    public void setHitRate(float value) {
        this.hitRate = value;
    }

    public float getThreatMultiplier() {
        return this.threatMultiplier;
    }

    public void setThreatMultiplier(float value) {
        this.threatMultiplier = value;
    }

    public float getRange() {
        return this.range;
    }

    public void setRange(float value) {
        this.range = value;
    }

    public float getRealResistance() {
        return this.realResistance;
    }

    public void setRealResistance(float value) {
        this.realResistance = value;
    }

    public float getPartialResistFactor() {
        return this.partialResistFactor;
    }

    public void setPartialResistFactor(float value) {
        this.partialResistFactor = value;
    }

    public float getDotTickInterval() {
        return this.dotTickInterval;
    }

    public void setDotTickInterval(float value) {
        this.dotTickInterval = value;
    }

    public float getNukeProcs() {
        return this.nukeProcs;
    }

    public void setNukeProcs(float value) {
        this.nukeProcs = value;
    }

    public boolean getDirty() {
        return this.dirty;
    }

    public void setDirty(boolean value) {
        this.dirty = value;
    }

    public static final java.util.Map<Integer, Integer> BaseMana = new java.util.LinkedHashMap<Integer, Integer>();

    static {
        BaseMana.put(70, 2241);
        BaseMana.put(71, 2343);
        BaseMana.put(72, 2446);
        BaseMana.put(73, 2549);
        BaseMana.put(74, 2652);
        BaseMana.put(75, 2754);
        BaseMana.put(76, 2857);
        BaseMana.put(77, 2960);
        BaseMana.put(78, 3063);
        BaseMana.put(79, 3166);
        BaseMana.put(80, 3268);
        BaseMana.put(81, 4567);
        BaseMana.put(82, 6382);
        BaseMana.put(83, 6382);
        BaseMana.put(84, 6382);
        BaseMana.put(85, 6382);
    }

    public Spell getSpell(CastingState castingState) {
        if (getAreaEffect()) {
            AoeSpell spell = new AoeSpell(this);
            spell.calculate(castingState);
            spell.calculateDerivedStats(castingState);
            return spell;
        } else {
            Spell spell = Spell.createNew(this, castingState.getSolver());
            spell.calculate(castingState);
            spell.calculateDerivedStats(castingState);
            return spell;
        }
    }

    public float getEffectAverageDamage(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        float[] damagePerSpellPower = new float[1];
        float[] igniteDamage = new float[1];
        float[] igniteDamagePerSpellPower = new float[1];
        return spell.calculateAverageDamage(castingState.getSolver(), 0, false, false, damagePerSpellPower, igniteDamage, igniteDamagePerSpellPower);
    }

    protected SpellTemplate() {
    }

    protected static SpellData makeSpellData(int cost, float minDamage, float maxDamage, float spellDamageCoefficient, float periodicDamage, float dotDamageCoefficient) {
        SpellData data = new SpellData();
        data.cost = cost;
        data.minDamage = minDamage;
        data.maxDamage = maxDamage;
        data.spellDamageCoefficient = spellDamageCoefficient;
        data.periodicDamage = periodicDamage;
        data.dotDamageCoefficient = dotDamageCoefficient;
        return data;
    }

    public void initializeCastTime(boolean channeled, boolean instant, float castTime, float cooldown) {
        this.channeled = channeled;
        this.instant = instant;
        this.baseCastTime = castTime;
        this.baseUntalentedCastTime = instant ? 1.5f : castTime;
        this.baseCooldown = cooldown;
        this.cooldown = cooldown;
    }

    public void initializeDamage(Solver solver, boolean areaEffect, int range, MagicSchool magicSchool, SpellData spellData) {
        initializeDamage(solver, areaEffect, range, magicSchool, spellData, 1, 1, 0);
    }

    public void initializeDamage(Solver solver, boolean areaEffect, int range, MagicSchool magicSchool, SpellData spellData, float hitProcs, float castProcs, float dotDuration) {
        Stats baseStats = solver.getBaseStats();
        MageTalents mageTalents = solver.getMageTalents();
        CalculationOptionsMage calculationOptions = solver.getCalculationOptions();

        this.areaEffect = areaEffect;
        this.baseCost = spellData.cost - (int) baseStats.getSpellsManaReduction();
        this.magicSchool = magicSchool;
        this.ticks = hitProcs;
        this.castProcs = castProcs;
        this.castProcs2 = castProcs;
        this.baseMinDamage = spellData.minDamage;
        this.baseMaxDamage = spellData.maxDamage;
        this.spellDamageCoefficient = spellData.spellDamageCoefficient;
        this.basePeriodicDamage = spellData.periodicDamage;
        this.dotDamageCoefficient = spellData.dotDamageCoefficient;
        this.dotDuration = dotDuration;

        this.baseDirectDamageModifier = 1.0f;
        this.baseDotDamageModifier = 1.0f;
        this.baseCostModifier = 1.0f;

        float baseCostAmplifier = calculationOptions.getEffectCostMultiplier();
        baseCostAmplifier *= (1.0f - 0.01f * mageTalents.getPrecision());
        if (mageTalents.getFrostChanneling() > 0)
            baseCostAmplifier *= (1.0f - 0.01f - 0.03f * mageTalents.getFrostChanneling());
        if (this.magicSchool == MagicSchool.Arcane) baseCostAmplifier *= (1.0f - 0.01f * mageTalents.getArcaneFocus());
        this.baseCostAmplifier = baseCostAmplifier;

        float baseInterruptProtection = baseStats.getInterruptProtection();
        if (this.magicSchool == MagicSchool.Fire || this.magicSchool == MagicSchool.FrostFire) {
            baseInterruptProtection += 0.35f * mageTalents.getBurningSoul();
        }
        this.baseInterruptProtection = baseInterruptProtection;

        float realResistance;
        switch (this.magicSchool) {
            case Arcane:
                this.baseSpellModifier = solver.getBaseArcaneSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseArcaneAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseArcaneCritRate();
                this.critBonus = solver.getBaseArcaneCritBonus();
                this.hitRate = solver.getBaseArcaneHitRate();
                this.threatMultiplier = solver.getArcaneThreatMultiplier();
                realResistance = calculationOptions.getArcaneResist();
                if (range != 0) {
                    this.range = range + mageTalents.getMagicAttunement() * 3;
                }
                break;
            case Fire:
                this.baseSpellModifier = solver.getBaseFireSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFireAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFireCritRate();
                this.critBonus = solver.getBaseFireCritBonus();
                this.hitRate = solver.getBaseFireHitRate();
                this.threatMultiplier = solver.getFireThreatMultiplier();
                realResistance = calculationOptions.getFireResist();
                if (range != 0) {
                    this.range = range + mageTalents.getFlameThrowing() * 3;
                }
                break;
            case FrostFire:
                this.baseSpellModifier = solver.getBaseFrostFireSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFrostFireAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFrostFireCritRate();
                this.critBonus = solver.getBaseFrostFireCritBonus();
                this.hitRate = solver.getBaseFrostFireHitRate();
                this.threatMultiplier = solver.getFrostFireThreatMultiplier();
                if (calculationOptions.getFireResist() == -1) {
                    realResistance = calculationOptions.getFrostResist();
                } else if (calculationOptions.getFrostResist() == -1) {
                    realResistance = calculationOptions.getFireResist();
                } else {
                    realResistance = Math.min(calculationOptions.getFireResist(), calculationOptions.getFrostResist());
                }
                this.range = range;
                break;
            case Frost:
                this.baseSpellModifier = solver.getBaseFrostSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFrostAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFrostCritRate();
                this.critBonus = solver.getBaseFrostCritBonus();
                this.hitRate = solver.getBaseFrostHitRate();
                this.threatMultiplier = solver.getFrostThreatMultiplier();
                realResistance = calculationOptions.getFrostResist();
                this.range = range * (1 + mageTalents.getArcticReach() * 0.1f);
                break;
            case Nature:
                this.baseSpellModifier = solver.getBaseNatureSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseNatureAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseNatureCritRate();
                this.critBonus = solver.getBaseNatureCritBonus();
                this.hitRate = solver.getBaseNatureHitRate();
                this.threatMultiplier = solver.getNatureThreatMultiplier();
                realResistance = calculationOptions.getNatureResist();
                this.range = range;
                break;
            case Shadow:
                this.baseSpellModifier = solver.getBaseShadowSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseShadowAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseShadowCritRate();
                this.critBonus = solver.getBaseShadowCritBonus();
                this.hitRate = solver.getBaseShadowHitRate();
                this.threatMultiplier = solver.getShadowThreatMultiplier();
                realResistance = calculationOptions.getShadowResist();
                this.range = range;
                break;
            case Holy:
            default:
                this.baseSpellModifier = solver.getBaseHolySpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseHolyAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseHolyCritRate();
                this.critBonus = solver.getBaseHolyCritBonus();
                this.hitRate = solver.getBaseHolyHitRate();
                this.threatMultiplier = solver.getHolyThreatMultiplier();
                realResistance = calculationOptions.getHolyResist();
                this.range = range;
                break;
        }

        int playerLevel = calculationOptions.getPlayerLevel();
        int targetLevel;

        if (areaEffect) {
            targetLevel = calculationOptions.getAoeTargetLevel();
            float hitRate = ((targetLevel <= playerLevel + 2) ? (0.96f - (targetLevel - playerLevel) * 0.01f) : (0.94f - (targetLevel - playerLevel - 2) * 0.11f)) + solver.getBaseSpellHit();
            if (this.magicSchool == MagicSchool.Arcane) hitRate += 0.01f * mageTalents.getArcaneFocus();
            if (hitRate > Spell.MaxHitRate) hitRate = Spell.MaxHitRate;
            this.hitRate = hitRate;
        } else {
            targetLevel = calculationOptions.getTargetLevel();
        }

        this.realResistance = realResistance;
        this.partialResistFactor = (realResistance == -1) ? 0 : (1 - StatConversion.getAverageResistance(playerLevel, targetLevel, realResistance, baseStats.getSpellPenetration()));
    }

    public void initializeEffectDamage(Solver solver, MagicSchool magicSchool, float minDamage, float maxDamage) {
        Stats baseStats = solver.getBaseStats();
        MageTalents mageTalents = solver.getMageTalents();
        CalculationOptionsMage calculationOptions = solver.getCalculationOptions();

        this.magicSchool = magicSchool;
        this.baseMinDamage = minDamage;
        this.baseMaxDamage = maxDamage;

        this.baseDirectDamageModifier = 1.0f;
        this.baseDotDamageModifier = 1.0f;
        this.baseCostModifier = 1.0f;

        float realResistance;
        switch (this.magicSchool) {
            case Arcane:
                this.baseSpellModifier = solver.getBaseArcaneSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseArcaneAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseArcaneCritRate();
                this.critBonus = solver.getBaseArcaneCritBonus();
                this.hitRate = solver.getBaseArcaneHitRate();
                this.threatMultiplier = solver.getArcaneThreatMultiplier();
                realResistance = calculationOptions.getArcaneResist();
                break;
            case Fire:
                this.baseSpellModifier = solver.getBaseFireSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFireAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFireCritRate();
                this.critBonus = solver.getBaseFireCritBonus();
                this.hitRate = solver.getBaseFireHitRate();
                this.threatMultiplier = solver.getFireThreatMultiplier();
                realResistance = calculationOptions.getFireResist();
                break;
            case FrostFire:
                this.baseSpellModifier = solver.getBaseFrostFireSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFrostFireAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFrostFireCritRate();
                this.critBonus = solver.getBaseFrostFireCritBonus();
                this.hitRate = solver.getBaseFrostFireHitRate();
                this.threatMultiplier = solver.getFrostFireThreatMultiplier();
                if (calculationOptions.getFireResist() == -1) {
                    realResistance = calculationOptions.getFrostResist();
                } else if (calculationOptions.getFrostResist() == -1) {
                    realResistance = calculationOptions.getFireResist();
                } else {
                    realResistance = Math.min(calculationOptions.getFireResist(), calculationOptions.getFrostResist());
                }
                break;
            case Frost:
                this.baseSpellModifier = solver.getBaseFrostSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseFrostAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseFrostCritRate();
                this.critBonus = solver.getBaseFrostCritBonus();
                this.hitRate = solver.getBaseFrostHitRate();
                this.threatMultiplier = solver.getFrostThreatMultiplier();
                realResistance = calculationOptions.getFrostResist();
                break;
            case Nature:
                this.baseSpellModifier = solver.getBaseNatureSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseNatureAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseNatureCritRate();
                this.critBonus = solver.getBaseNatureCritBonus();
                this.hitRate = solver.getBaseNatureHitRate();
                this.threatMultiplier = solver.getNatureThreatMultiplier();
                realResistance = calculationOptions.getNatureResist();
                break;
            case Shadow:
                this.baseSpellModifier = solver.getBaseShadowSpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseShadowAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseShadowCritRate();
                this.critBonus = solver.getBaseShadowCritBonus();
                this.hitRate = solver.getBaseShadowHitRate();
                this.threatMultiplier = solver.getShadowThreatMultiplier();
                realResistance = calculationOptions.getShadowResist();
                break;
            case Holy:
            default:
                this.baseSpellModifier = solver.getBaseHolySpellModifier();
                this.baseAdditiveSpellModifier = solver.getBaseHolyAdditiveSpellModifier();
                this.baseCritRate = solver.getBaseHolyCritRate();
                this.critBonus = solver.getBaseHolyCritBonus();
                this.hitRate = solver.getBaseHolyHitRate();
                this.threatMultiplier = solver.getHolyThreatMultiplier();
                realResistance = calculationOptions.getHolyResist();
                break;
        }

        int playerLevel = calculationOptions.getPlayerLevel();
        int targetLevel = calculationOptions.getTargetLevel();

        this.realResistance = realResistance;
        this.partialResistFactor = (realResistance == -1) ? 0 : (1 - StatConversion.getAverageResistance(playerLevel, targetLevel, realResistance, baseStats.getSpellPenetration()));
    }

    public static float procBuffUp(float procChance, float buffDuration, float triggerInterval) {
        if (triggerInterval <= 0)
            return 0;
        else
            return 1 - (float) Math.pow(1 - procChance, buffDuration / triggerInterval);
    }

    public float calculateCastTime(CastingState castingState, float interruptProtection, float critRate, boolean pom, float baseCastTime, float[] channelReduction) {
        CalculationOptionsMage calculationOptions = castingState.getCalculationOptions();
        float castingSpeed = castingState.getCastingSpeed();
        float spellHasteRating = castingState.getSpellHasteRating();
        float levelScalingFactor = calculationOptions.getLevelScalingFactor();
        float hasteFactor = levelScalingFactor / 1000f;
        float rootCastingSpeed = castingSpeed / (1 + spellHasteRating * hasteFactor);

        float InterruptFactor = 0f;
        float maxPushback = 0f;
        if (calculationOptions.getInterruptFrequency() > 0f) {
            InterruptFactor = Math.min(calculationOptions.getInterruptFrequency(), 2 * castingSpeed / baseCastTime);
            if (castingState.getIcyVeins()) interruptProtection = 1;
            maxPushback = 0.5f * Math.max(0, 1 - interruptProtection);
            if (getChanneled()) maxPushback = 0.0f;
        }

        if (pom) baseCastTime = 0.0f;

        float globalCooldown = Math.max(Spell.GlobalCooldownLimit, 1.5f / castingSpeed);
        float latency;
        if (baseCastTime <= 1.5f || getInstant()) {
            latency = calculationOptions.getLatencyGCD();
        } else if (getChanneled()) {
            latency = calculationOptions.getLatencyChannel();
        } else {
            latency = calculationOptions.getLatencyCast();
        }
        float averageTicks = getTicks();
        float castTime = baseCastTime / castingSpeed;
        if (calculationOptions.getBeta() && getChanneled()) {
            float tickCastTime = castTime / getTicks();
            averageTicks = (float) Math.floor(baseCastTime / tickCastTime);
            castTime = averageTicks * tickCastTime;
        }
        if (InterruptFactor > 0) {
            castTime = castTime * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f) * maxPushback * InterruptFactor;
        }
        castTime += latency;
        float gcdcap = globalCooldown + calculationOptions.getLatencyGCD();
        if (castTime < gcdcap) castTime = gcdcap;
        channelReduction[0] = 0.0f;

        if (!calculationOptions.getAdvancedHasteProcs()) {
            for (int i = 0; i < castingState.getSolver().getHasteRatingEffectsCount(); i++) {
                SpecialEffect effect = castingState.getSolver().getHasteRatingEffects()[i];
                float procs = 0.0f;
                int triggers = 0;
                switch (effect.getTrigger()) {
                    case DamageSpellCast:
                    case SpellCast:
                        procs = getCastProcs();
                        triggers = (int) procs;
                        break;
                    case DamageSpellCrit:
                    case SpellCrit:
                        procs = critRate * averageTicks;
                        triggers = (int) averageTicks;
                        break;
                    case DamageSpellHit:
                    case SpellHit:
                        procs = getHitRate() * averageTicks;
                        triggers = (int) averageTicks;
                        break;
                    default:
                        break;
                }
                if (procs == 0.0f) continue;
                float procHaste = effect.getStats().getHasteRating();
                if (effect.getCooldown() >= effect.getDuration()) {
                    float speed = rootCastingSpeed * (1 + (spellHasteRating + procHaste) * hasteFactor);
                    float gcd = Math.max(Spell.GlobalCooldownLimit, 1.5f / speed);
                    float cast = baseCastTime / speed;
                    if (calculationOptions.getBeta() && getChanneled()) {
                        float tickCastTime = cast / getTicks();
                        cast = (float) Math.floor(baseCastTime / tickCastTime) * tickCastTime;
                    }
                    if (InterruptFactor > 0) {
                        cast = cast * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f) * maxPushback * InterruptFactor;
                    }
                    cast += latency;
                    gcdcap = gcd + calculationOptions.getLatencyGCD();
                    if (cast < gcdcap) cast = gcdcap;

                    float castsAffected = 0;
                    if (triggers > 1) {
                        for (int c = 0; c < triggers; c++)
                            castsAffected += (float) Math.ceil((effect.getDuration() - c * castTime / triggers) / cast);
                        castsAffected /= triggers;
                    } else {
                        castsAffected = (float) Math.ceil(effect.getDuration() / cast);
                    }
                    float effectiveDuration = castsAffected * cast;
                    spellHasteRating += procHaste * (effectiveDuration / effect.getDuration()) * effect.getAverageUptime(castTime / triggers, procs / triggers, 3.0f, calculationOptions.getFightDuration());
                    castingSpeed = rootCastingSpeed * (1 + spellHasteRating * hasteFactor);

                    globalCooldown = Math.max(Spell.GlobalCooldownLimit, 1.5f / castingSpeed);
                    castTime = baseCastTime / castingSpeed;
                    if (calculationOptions.getBeta() && getChanneled()) {
                        float tickCastTime = castTime / getTicks();
                        averageTicks = (float) Math.floor(baseCastTime / tickCastTime);
                        castTime = averageTicks * tickCastTime;
                    }
                    if (InterruptFactor > 0) {
                        castTime = castTime * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f) * maxPushback * InterruptFactor;
                    }
                    castTime += latency;
                    gcdcap = globalCooldown + calculationOptions.getLatencyGCD();
                    if (castTime < gcdcap) castTime = gcdcap;
                } else if (effect.getCooldown() == 0 && (effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellCrit)) {
                    float rawHaste = spellHasteRating;

                    castingSpeed /= (1 + spellHasteRating / 1000f * levelScalingFactor);
                    float proccedSpeed = castingSpeed * (1 + (rawHaste + procHaste) / 1000f * levelScalingFactor);
                    float proccedGcd = Math.max(Spell.GlobalCooldownLimit, 1.5f / proccedSpeed);
                    float proccedCastTime = baseCastTime / proccedSpeed;
                    float proccedTicks = averageTicks;
                    if (calculationOptions.getBeta() && getChanneled()) {
                        float tickCastTime = proccedCastTime / getTicks();
                        proccedTicks = (float) Math.floor(baseCastTime / tickCastTime);
                        castTime = proccedTicks * tickCastTime;
                    }
                    if (InterruptFactor > 0) {
                        proccedCastTime = proccedCastTime * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f) * maxPushback * InterruptFactor;
                    }
                    proccedCastTime += latency;
                    if (proccedCastTime < proccedGcd + calculationOptions.getLatencyGCD())
                        proccedCastTime = proccedGcd + calculationOptions.getLatencyGCD();
                    int chancesToProc = (int) (((int) Math.floor(effect.getDuration() / proccedCastTime) + 1) * proccedTicks);
                    if (!(getInstant() || pom)) chancesToProc -= 1;
                    if (getAreaEffect()) chancesToProc *= calculationOptions.getAoeTargets();
                    spellHasteRating = rawHaste + procHaste * (1 - (float) Math.pow(1 - effect.getChance() * critRate, chancesToProc));
                    castingSpeed *= (1 + spellHasteRating / 1000f * levelScalingFactor);
                    globalCooldown = Math.max(Spell.GlobalCooldownLimit, 1.5f / castingSpeed);
                    castTime = baseCastTime / castingSpeed;
                    if (calculationOptions.getBeta() && getChanneled()) {
                        float tickCastTime = castTime / getTicks();
                        averageTicks = (float) Math.floor(baseCastTime / tickCastTime);
                        castTime = averageTicks * tickCastTime;
                    }
                    if (InterruptFactor > 0) {
                        castTime = castTime * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f + latency) * maxPushback * InterruptFactor;
                    }
                    castTime += latency;
                    if (castTime < globalCooldown + calculationOptions.getLatencyGCD())
                        castTime = globalCooldown + calculationOptions.getLatencyGCD();
                }
            }
            for (int i = 0; i < castingState.getSolver().getStackingHasteEffectCooldownsCount(); i++) {
                EffectCooldown effectCooldown = castingState.getSolver().getStackingHasteEffectCooldowns()[i];
                if (castingState.effectsActive(effectCooldown.mask)) {
                    Stats stats = effectCooldown.specialEffect.getStats();
                    for (int j = 0; j < stats.getRawSpecialEffectDataSize(); j++) {
                        SpecialEffect effect = stats.getRawSpecialEffectData()[j];
                        float procHaste = effect.getStats().getHasteRating();
                        if (procHaste > 0) {
                            float procs = 0.0f;
                            switch (effect.getTrigger()) {
                                case DamageSpellCast:
                                case SpellCast:
                                    procs = getCastProcs();
                                    break;
                                case DamageSpellCrit:
                                case SpellCrit:
                                    procs = critRate * averageTicks;
                                    break;
                                case DamageSpellHit:
                                case SpellHit:
                                    procs = getHitRate() * averageTicks;
                                    break;
                                default:
                                    break;
                            }
                            if (procs == 0.0f) continue;
                            float effectHasteRating;
                            if (procs > averageTicks) {
                                effectHasteRating = effect.getAverageStackSize(castTime / procs, 1.0f, 3.0f, effectCooldown.specialEffect.getDuration()) * procHaste;
                            } else {
                                effectHasteRating = effect.getAverageStackSize(castTime / averageTicks, procs / averageTicks, 3.0f, effectCooldown.specialEffect.getDuration()) * procHaste;
                            }

                            castingSpeed /= (1 + spellHasteRating / 1000f * levelScalingFactor);
                            spellHasteRating += effectHasteRating;
                            castingSpeed *= (1 + spellHasteRating / 1000f * levelScalingFactor);

                            globalCooldown = Math.max(Spell.GlobalCooldownLimit, 1.5f / castingSpeed);
                            castTime = baseCastTime / castingSpeed;
                            if (calculationOptions.getBeta() && getChanneled()) {
                                float tickCastTime = castTime / getTicks();
                                averageTicks = (float) Math.floor(baseCastTime / tickCastTime);
                                castTime = averageTicks * tickCastTime;
                            }
                            if (InterruptFactor > 0) {
                                castTime = castTime * (1 + InterruptFactor * maxPushback) - (maxPushback * 0.5f) * maxPushback * InterruptFactor;
                            }
                            castTime += latency;
                            if (castTime < globalCooldown + calculationOptions.getLatencyGCD())
                                castTime = globalCooldown + calculationOptions.getLatencyGCD();
                        }
                    }
                }
            }
        }

        if (getChanneled() && calculationOptions.getBeta()) {
            channelReduction[0] = 1 - averageTicks / getTicks();
        }

        if (getChanneled() && InterruptFactor > 0) {
            int maxLostTicks = (int) Math.ceil(averageTicks * 0.25f * Math.max(0, 1 - interruptProtection));
            castTime -= latency;
            float tickFactor = 0;
            for (int i = 0; i < maxLostTicks; i++) {
                tickFactor += InterruptFactor * castTime / averageTicks * (i + 1) / averageTicks;
            }
            tickFactor += InterruptFactor * (averageTicks - maxLostTicks) * castTime / averageTicks * maxLostTicks / averageTicks;
            if (calculationOptions.getBeta()) {
                channelReduction[0] = 1 - averageTicks * (1 - tickFactor) / getTicks();
            } else {
                channelReduction[0] = tickFactor;
            }
            castTime = castTime * (1 - tickFactor) + latency;
        }

        return castTime;
    }
}

