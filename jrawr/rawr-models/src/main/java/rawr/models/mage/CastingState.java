// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CastingState.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL (staged): state fields/properties and lazy sub-states are ported; Initialize/Clone/GetCycle/
// GetSpell and the cycle-generation methods are appended in the next slice (marker __REST__).
package rawr.models.mage;

import rawr.base.talents.MageTalents;
import rawr.base.stats.Stats;

public class CastingState {
    public CastingState referenceCastingState; // state from where we take base spell calculations for adjusting

    private Solver solver;

    public Solver getSolver() {
        return this.solver;
    }

    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    public CalculationOptionsMage calculationOptions;
    public MageTalents mageTalents;
    public Stats baseStats;


    public CalculationOptionsMage getCalculationOptions() {
        return this.calculationOptions;
    }

    public MageTalents getMageTalents() {
        return this.mageTalents;
    }

    public Stats getBaseStats() {
        return this.baseStats;
    }


    public boolean getArcanePower() {
        return this.arcanePower;
    }

    public boolean getMoltenFury() {
        return this.moltenFury;
    }

    public boolean getCombustion() {
        return this.combustion;
    }

    public boolean getIcyVeins() {
        return this.icyVeins;
    }

    public boolean getHeroism() {
        return this.heroism;
    }

    public boolean getPowerInfusion() {
        return this.powerInfusion;
    }

    public boolean getFrozen() {
        return this.frozen;
    }

    public void setFrozen(boolean value) {
        this.frozen = value;
    }

    public float getSpellHit() {
        return this.solver.getBaseSpellHit();
    }

    public float getArcaneHitRate() {
        return this.solver.getBaseArcaneHitRate();
    }

    public float getFireHitRate() {
        return this.solver.getBaseFireHitRate();
    }

    public float getFrostHitRate() {
        return this.solver.getBaseFrostHitRate();
    }

    public float getNatureHitRate() {
        return this.solver.getBaseNatureHitRate();
    }

    public float getShadowHitRate() {
        return this.solver.getBaseShadowHitRate();
    }

    public float getFrostFireHitRate() {
        return this.solver.getBaseFrostFireHitRate();
    }

    public float getHolyHitRate() {
        return this.solver.getBaseHolyHitRate();
    }

    public float getArcaneThreatMultiplier() {
        return this.solver.getArcaneThreatMultiplier();
    }

    public float getFireThreatMultiplier() {
        return this.solver.getFireThreatMultiplier();
    }

    public float getFrostThreatMultiplier() {
        return this.solver.getFrostThreatMultiplier();
    }

    public float getNatureThreatMultiplier() {
        return this.solver.getNatureThreatMultiplier();
    }

    public float getShadowThreatMultiplier() {
        return this.solver.getShadowThreatMultiplier();
    }

    public float getFrostFireThreatMultiplier() {
        return this.solver.getFrostFireThreatMultiplier();
    }

    public float getHolyThreatMultiplier() {
        return this.solver.getHolyThreatMultiplier();
    }

    public float castingSpeed;

    public float getCastingSpeed() {
        return this.castingSpeed;
    }

    public void setCastingSpeed(float castingSpeed) {
        this.castingSpeed = castingSpeed;
    }

    public float stateSpellPower;

    public float getStateSpellPower() {
        return this.stateSpellPower;
    }

    public void setStateSpellPower(float stateSpellPower) {
        this.stateSpellPower = stateSpellPower;
    }

    public float getArcaneSpellPower() {
        return this.solver.getBaseArcaneSpellPower() + this.stateSpellPower;
    }

    public float getFireSpellPower() {
        return this.solver.getBaseFireSpellPower() + this.stateSpellPower + (this.flameCap ? 80.0f : 0.0f);
    }

    public float getFrostSpellPower() {
        return this.solver.getBaseFrostSpellPower() + this.stateSpellPower;
    }

    public float getNatureSpellPower() {
        return this.solver.getBaseNatureSpellPower() + this.stateSpellPower;
    }

    public float getShadowSpellPower() {
        return this.solver.getBaseShadowSpellPower() + this.stateSpellPower;
    }

    public float getFrostFireSpellPower() {
        return Math.max(getFrostSpellPower(), getFireSpellPower());
    }

    public float getHolySpellPower() {
        return this.solver.getBaseHolySpellPower() + this.stateSpellPower;
    }

    public float getSpiritRegen() {
        return this.solver.getSpiritRegen();
    }

    public float getManaRegen() {
        return this.solver.getManaRegen();
    }

    public float getManaRegen5SR() {
        return this.solver.getManaRegen5SR();
    }

    public float getManaRegenDrinking() {
        return this.solver.getManaRegenDrinking();
    }

    public float getHealthRegen() {
        return this.solver.getHealthRegen();
    }

    public float getHealthRegenCombat() {
        return this.solver.getHealthRegenCombat();
    }

    public float getHealthRegenEating() {
        return this.solver.getHealthRegenEating();
    }

    public float getMeleeMitigation() {
        return this.solver.getMeleeMitigation();
    }

    public float getDefense() {
        return this.solver.getDefense();
    }

    public float getPhysicalCritReduction() {
        return this.solver.getPhysicalCritReduction();
    }

    public float getSpellCritReduction() {
        return this.solver.getSpellCritReduction();
    }

    public float getCritDamageReduction() {
        return this.solver.getCritDamageReduction();
    }

    public float getDodge() {
        return this.solver.getDodge();
    }

    public float stateSpellModifier;

    public float getStateSpellModifier() {
        return this.stateSpellModifier;
    }

    public void setStateSpellModifier(float stateSpellModifier) {
        this.stateSpellModifier = stateSpellModifier;
    }

    public float stateAdditiveSpellModifier;

    public float getStateAdditiveSpellModifier() {
        return this.stateAdditiveSpellModifier;
    }

    public void setStateAdditiveSpellModifier(float stateAdditiveSpellModifier) {
        this.stateAdditiveSpellModifier = stateAdditiveSpellModifier;
    }

    public float getArcaneCritBonus() {
        return this.solver.getBaseArcaneCritBonus();
    }

    public float getFireCritBonus() {
        return this.combustion ? this.solver.getCombustionFireCritBonus() : this.solver.getBaseFireCritBonus();
    }

    public float getFrostCritBonus() {
        return this.solver.getBaseFrostCritBonus();
    }

    public float getNatureCritBonus() {
        return this.solver.getBaseNatureCritBonus();
    }

    public float getShadowCritBonus() {
        return this.solver.getBaseShadowCritBonus();
    }

    public float getFrostFireCritBonus() {
        return this.combustion ? this.solver.getCombustionFrostFireCritBonus() : this.solver.getBaseFrostFireCritBonus();
    }

    public float getHolyCritBonus() {
        return this.solver.getBaseHolyCritBonus();
    }

    public float stateCritRate;

    public float getStateCritRate() {
        return this.stateCritRate;
    }

    public void setStateCritRate(float stateCritRate) {
        this.stateCritRate = stateCritRate;
    }

    public float getCritRate() {
        return this.stateCritRate + this.solver.getBaseCritRate();
    }

    public float getArcaneCritRate() {
        return this.stateCritRate + this.solver.getBaseArcaneCritRate();
    }

    public float getFireCritRate() {
        return this.combustion ? 3 / this.combustionDuration : this.stateCritRate + this.solver.getBaseFireCritRate();
    }

    public float getFrostCritRate() {
        return this.stateCritRate + this.solver.getBaseFrostCritRate();
    }

    public float getNatureCritRate() {
        return this.stateCritRate + this.solver.getBaseNatureCritRate();
    }

    public float getShadowCritRate() {
        return this.stateCritRate + this.solver.getBaseShadowCritRate();
    }

    public float getFrostFireCritRate() {
        return this.combustion ? 3 / this.combustionDuration : this.stateCritRate + this.solver.getBaseFrostFireCritRate();
    }

    public float getHolyCritRate() {
        return this.stateCritRate + this.solver.getBaseHolyCritRate();
    }

    public float snaredTime;

    public float getSnaredTime() {
        return this.snaredTime;
    }

    public void setSnaredTime(float snaredTime) {
        this.snaredTime = snaredTime;
    }

    public boolean effectsActive(int effects) {
        return (effects & getEffects()) == effects;
    }

    public boolean evocation;
    public boolean arcanePower;
    public boolean icyVeins;
    public boolean moltenFury;
    public boolean heroism;
    public boolean potionOfWildMagic;
    public boolean potionOfSpeed;
    public boolean flameCap;
    public boolean manaGemEffect;
    public boolean berserking;
    public boolean combustion;
    public boolean waterElemental;
    public boolean mirrorImage;
    public boolean powerInfusion;
    public boolean frozen;
    public boolean useFireWard;
    public boolean useFrostWard;

    private int effects;

    public int getEffects() {
        return effects;
    }

    public void setEffects(int value) {
        effects = value;
        this.evocation = (value & StandardEffect.Evocation) != 0;
        this.arcanePower = (value & StandardEffect.ArcanePower) != 0;
        this.icyVeins = (value & StandardEffect.IcyVeins) != 0;
        this.moltenFury = (value & StandardEffect.MoltenFury) != 0;
        this.heroism = (value & StandardEffect.Heroism) != 0;
        this.potionOfWildMagic = (value & StandardEffect.PotionOfWildMagic) != 0;
        this.potionOfSpeed = (value & StandardEffect.PotionOfSpeed) != 0;
        this.flameCap = (value & StandardEffect.FlameCap) != 0;
        this.manaGemEffect = (value & StandardEffect.ManaGemEffect) != 0;
        this.berserking = (value & StandardEffect.Berserking) != 0;
        this.combustion = (value & StandardEffect.Combustion) != 0;
        this.waterElemental = (value & StandardEffect.WaterElemental) != 0;
        this.mirrorImage = (value & StandardEffect.MirrorImage) != 0;
        this.powerInfusion = (value & StandardEffect.PowerInfusion) != 0;
    }

    public float combustionDuration;

    public float getCombustionDuration() {
        return this.combustionDuration;
    }

    public void setCombustionDuration(float combustionDuration) {
        this.combustionDuration = combustionDuration;
    }

    public float spellHasteRating;

    public float getSpellHasteRating() {
        return this.spellHasteRating;
    }

    public void setSpellHasteRating(float spellHasteRating) {
        this.spellHasteRating = spellHasteRating;
    }

    public float procHasteRating;

    public float getProcHasteRating() {
        return this.procHasteRating;
    }

    public void setProcHasteRating(float procHasteRating) {
        this.procHasteRating = procHasteRating;
    }

    private String buffLabel;

    public String getBuffLabel() {
        if (buffLabel == null) {
            buffLabel = this.solver.effectsDescription(getEffects());
        }
        return buffLabel;
    }

    @Override
    public String toString() {
        return getBuffLabel();
    }

    private static float computeCombustion(float critRate) {
        float c0 = 1, c1 = 0, c2 = 0, c3 = 0;
        float duration = 0;

        for (int cast = 1; cast <= 13; cast++) {
            c3 = critRate * c2;
            c2 = c2 * (1 - critRate) + c1 * critRate;
            c1 = c1 * (1 - critRate) + c0 * critRate;
            c0 = c0 * (1 - critRate);
            critRate = Math.min(critRate + 0.1f, 1f);
            duration += c3 * cast;
        }
        return duration;
    }

    private CastingState maintainSnareState;

    public CastingState getMaintainSnareState() {
        if (maintainSnareState == null) {
            if (this.snaredTime == 1.0f) {
                maintainSnareState = this;
            } else {
                maintainSnareState = clone();
                maintainSnareState.snaredTime = 1.0f;
            }
        }
        return maintainSnareState;
    }

    private CastingState frozenState;

    public CastingState getFrozenState() {
        if (frozenState == null) {
            if (this.frozen) {
                frozenState = this;
            } else {
                frozenState = clone();
                frozenState.frozen = true;
                frozenState.stateCritRate += (this.mageTalents.getShatter() == 3 ? 0.5f : 0.17f * this.mageTalents.getShatter());
                if (this.combustion) {
                    frozenState.combustionDuration = computeCombustion(this.solver.getBaseFireCritRate() + frozenState.stateCritRate);
                }
            }
        }
        return frozenState;
    }

    private CastingState tier10TwoPieceState;

    public CastingState getTier10TwoPieceState() {
        if (tier10TwoPieceState == null) {
            tier10TwoPieceState = clone();
            tier10TwoPieceState.castingSpeed *= 1.12f;
            tier10TwoPieceState.referenceCastingState = this;
        }
        return tier10TwoPieceState;
    }

    private CastingState[] hasteProcs;

    public CastingState() {
    }

    public CastingState(Solver solver, int effects, boolean frozen, float procHasteRating) {
        initialize(solver, effects, frozen, procHasteRating);
    }

    public static CastingState createNew(Solver solver, int effects, boolean frozen, float procHasteRating) {
        CastingState state;
        if (solver.getNeedsDisplayCalculations() || solver.getArraySet() == null) {
            state = new CastingState();
        } else {
            state = solver.getArraySet().newCastingState();
        }
        state.initialize(solver, effects, frozen, procHasteRating);
        return state;
    }

    public static CastingState newRaw(Solver solver, int effects) {
        CastingState state = solver.getNeedsDisplayCalculations() ? new CastingState() : solver.getArraySet().newCastingState();
        state.solver = solver;
        state.referenceCastingState = null;
        state.useFireWard = false;
        state.useFrostWard = false;
        state.setEffects(effects);
        state.buffLabel = null;
        state.spellsCount = 0;
        state.cyclesCount = 0;
        state.procHasteRating = 0;
        return state;
    }

    private Spell[] spells = new Spell[8];
    private int spellsCount;
    private Cycle[] cycles = new Cycle[8];
    private int cyclesCount;

    public CastingState clone() {
        CastingState state;
        if (this.solver.getNeedsDisplayCalculations() || this.solver.getArraySet() == null) {
            state = new CastingState();
        } else {
            state = this.solver.getArraySet().newCastingState();
        }
        state.frozenState = null;
        state.useFireWard = this.useFireWard;
        state.useFrostWard = this.useFrostWard;
        state.maintainSnareState = null;
        state.tier10TwoPieceState = null;
        state.referenceCastingState = null;

        state.buffLabel = null;

        state.solver = this.solver;
        state.calculationOptions = this.calculationOptions;
        state.mageTalents = this.mageTalents;
        state.baseStats = this.baseStats;
        state.hasteProcs = null;

        state.snaredTime = this.snaredTime;
        state.procHasteRating = this.procHasteRating;
        state.setEffects(getEffects());
        state.stateSpellPower = this.stateSpellPower;
        state.spellHasteRating = this.spellHasteRating;
        state.stateCritRate = this.stateCritRate;
        state.combustionDuration = this.combustionDuration;
        state.frozen = this.frozen;
        state.castingSpeed = this.castingSpeed;
        state.stateAdditiveSpellModifier = this.stateAdditiveSpellModifier;
        state.stateSpellModifier = this.stateSpellModifier;

        state.spellsCount = 0;
        state.cyclesCount = 0;

        return state;
    }

    public void initialize(Solver solver, int effects, boolean frozen, float procHasteRating) {
        frozenState = null;
        this.useFireWard = false;
        this.useFrostWard = false;
        maintainSnareState = null;
        tier10TwoPieceState = null;
        this.referenceCastingState = null;

        this.stateSpellPower = 0;
        this.stateAdditiveSpellModifier = 0;
        buffLabel = null;

        this.solver = solver;
        this.calculationOptions = solver.getCalculationOptions();
        this.mageTalents = solver.getMageTalents();
        this.baseStats = solver.getBaseStats();

        this.hasteProcs = null;

        float levelScalingFactor = this.calculationOptions.getLevelScalingFactor();

        this.snaredTime = this.calculationOptions.getSnaredTime();
        if (this.calculationOptions.getMaintainSnare()) this.snaredTime = 1.0f;

        float stateCritRating = 0.0f;
        this.procHasteRating = procHasteRating;
        this.spellHasteRating = this.baseStats.getHasteRating() + procHasteRating;

        setEffects(effects);

        if (this.potionOfWildMagic) {
            this.stateSpellPower += 200;
            stateCritRating += 200;
        }
        if (this.potionOfSpeed) {
            this.spellHasteRating += 500;
        }

        java.util.List<EffectCooldown> cooldownList = solver.getCooldownList();
        for (int i = 0; i < cooldownList.size(); i++) {
            EffectCooldown effect = cooldownList.get(i);
            if (effect.specialEffect != null && (effects & effect.mask) == effect.mask) {
                this.stateSpellPower += effect.specialEffect.getStats().getSpellPower();
                this.spellHasteRating += effect.specialEffect.getStats().getHasteRating();
            }
        }

        this.castingSpeed = (1 + this.spellHasteRating / 1000f * levelScalingFactor) * solver.getCastingSpeedMultiplier();

        this.stateCritRate = stateCritRating / 1400f * levelScalingFactor;
        if (frozen) this.stateCritRate += (this.mageTalents.getShatter() == 3 ? 0.5f : 0.17f * this.mageTalents.getShatter());

        if (this.combustion) {
            this.combustionDuration = computeCombustion(solver.getBaseFireCritRate() + this.stateCritRate);
        } else {
            this.combustionDuration = 0;
        }

        this.frozen = frozen;

        if (this.icyVeins) {
            this.castingSpeed *= 1.2f;
        }
        if (this.berserking) {
            this.castingSpeed *= 1.2f;
        }
        if (this.heroism) {
            this.castingSpeed *= 1.3f;
        } else if (this.powerInfusion) {
            this.castingSpeed *= 1.2f;
        }

        this.stateSpellModifier = 1.0f;
        if (this.arcanePower) {
            this.stateAdditiveSpellModifier += 0.2f;
        }
        if (this.moltenFury) {
            this.stateSpellModifier *= (1 + 0.06f * this.mageTalents.getMoltenFury());
        }
        if (this.mirrorImage && this.baseStats.getMage4T10() > 0) {
            this.stateSpellModifier *= 1.18f;
        }

        this.spellsCount = 0;
        this.cyclesCount = 0;
    }

    public float getArcaneBoltAverageDamage() {
        return this.solver.getArcaneBoltTemplate().getEffectAverageDamage(this);
    }

    public float getLightningBoltAverageDamage() {
        return this.solver.getLightningBoltTemplate().getEffectAverageDamage(this);
    }

    public float getThunderBoltAverageDamage() {
        return this.solver.getThunderBoltTemplate().getEffectAverageDamage(this);
    }

    public float getPendulumOfTelluricCurrentsAverageDamage() {
        return this.solver.getPendulumOfTelluricCurrentsTemplate().getEffectAverageDamage(this);
    }

    public float getArcaneAverageDamage() {
        return this.solver.getArcaneDamageTemplate().getEffectAverageDamage(this);
    }

    public float getFireAverageDamage() {
        return this.solver.getFireDamageTemplate().getEffectAverageDamage(this);
    }

    public float getFrostAverageDamage() {
        return this.solver.getFrostDamageTemplate().getEffectAverageDamage(this);
    }

    public float getShadowAverageDamage() {
        return this.solver.getShadowDamageTemplate().getEffectAverageDamage(this);
    }

    public float getNatureAverageDamage() {
        return this.solver.getNatureDamageTemplate().getEffectAverageDamage(this);
    }

    public float getHolyAverageDamage() {
        return this.solver.getHolyDamageTemplate().getEffectAverageDamage(this);
    }

    public float getValkyrAverageDamage() {
        return this.solver.getValkyrDamageTemplate().multiplier;
    }

    public Cycle getCycle(CycleId cycleId) {
        Cycle c = null;
        for (int i = 0; i < this.cyclesCount; i++) {
            Cycle cycle = this.cycles[i];
            if (cycle.cycleId == cycleId) return cycle;
        }

        if (this.calculationOptions.getAdvancedHasteProcs()) {
            c = getAveragedHasteCycle(cycleId);
        }

        if (c == null) {
            c = getNewCycle(cycleId);
        }
        if (c != null) {
            if (this.useFireWard) {
                c = FireWardCycle.getCycle(this.solver.getNeedsDisplayCalculations(), this, c);
            } else if (this.useFrostWard) {
                c = FrostWardCycle.getCycle(this.solver.getNeedsDisplayCalculations(), this, c);
            }

            c.cycleId = cycleId;
            if (this.cyclesCount >= this.cycles.length) {
                int length = 2 * this.cycles.length;
                Cycle[] destinationArray = new Cycle[length];
                System.arraycopy(this.cycles, 0, destinationArray, 0, this.cyclesCount);
                this.cycles = destinationArray;
            }
            this.cycles[this.cyclesCount++] = c;
        }

        return c;
    }

    private Cycle getAveragedHasteCycle(CycleId cycleId) {
        float baseProcHaste = 0;
        Cycle baseCycle = getNewCycle(cycleId);
        for (int i = 0; i < this.solver.getStackingHasteEffectCooldownsCount(); i++) {
            EffectCooldown effectCooldown = this.solver.getStackingHasteEffectCooldowns()[i];
            if (effectsActive(effectCooldown.mask)) {
                Stats specialStats = effectCooldown.specialEffect.getStats();
                for (int j = 0; j < specialStats.getRawSpecialEffectDataSize(); j++) {
                    rawr.base.effects.SpecialEffect effect = specialStats.getRawSpecialEffectData()[j];
                    if (effect.getStats().getHasteRating() > 0) {
                        float procs = 0.0f;
                        switch (effect.getTrigger()) {
                            case DamageSpellCast:
                            case SpellCast:
                                procs = baseCycle.castProcs;
                                break;
                            case DamageSpellCrit:
                            case SpellCrit:
                                procs = baseCycle.critProcs;
                                break;
                            case DamageSpellHit:
                            case SpellHit:
                                procs = baseCycle.hitProcs;
                                break;
                            default:
                                break;
                        }
                        if (procs == 0.0f) continue;
                        if (procs > baseCycle.ticks) {
                            baseProcHaste += effect.getAverageStackSize(baseCycle.castTime / procs, 1.0f, 3.0f, effectCooldown.specialEffect.getDuration()) * effect.getStats().getHasteRating();
                        } else {
                            baseProcHaste += effect.getAverageStackSize(baseCycle.castTime / baseCycle.ticks, procs / baseCycle.ticks, 3.0f, effectCooldown.specialEffect.getDuration()) * effect.getStats().getHasteRating();
                        }
                    }
                }
            }
        }
        int N = this.solver.getHasteRatingEffectsCount();
        rawr.base.effects.SpecialEffect[] hasteRatingEffects = new rawr.base.effects.SpecialEffect[N];
        System.arraycopy(this.solver.getHasteRatingEffects(), 0, hasteRatingEffects, 0, N);
        if (baseProcHaste == 0 && N == 0) return baseCycle;
        float[] triggerInterval = new float[N];
        float[] triggerChance = new float[N];
        float[] offset = new float[N];
        for (int i = 0; i < N; i++) {
            rawr.base.effects.SpecialEffect effect = hasteRatingEffects[i];
            float procs = 0.0f;
            switch (effect.getTrigger()) {
                case DamageSpellCast:
                case SpellCast:
                    procs = baseCycle.castProcs;
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    procs = baseCycle.critProcs;
                    break;
                case DamageSpellHit:
                case SpellHit:
                    procs = baseCycle.hitProcs;
                    break;
                default:
                    break;
            }
            triggerInterval[i] = baseCycle.castTime / baseCycle.ticks;
            triggerChance[i] = procs / baseCycle.ticks;
        }
        rawr.base.stats.WeightedStat[] result = rawr.base.effects.SpecialEffect.getAverageCombinedUptimeCombinations(hasteRatingEffects, triggerInterval, triggerChance, offset, 3.0f, this.calculationOptions.getFightDuration(), rawr.base.stats.AdditiveStat.HasteRating);
        if (this.hasteProcs == null) {
            this.hasteProcs = new CastingState[result.length];
            for (int i = 0; i < result.length; i++) {
                CastingState subState = CastingState.createNew(this.solver, getEffects(), this.frozen, baseProcHaste + result[i].value());
                subState.referenceCastingState = this;
                this.hasteProcs[i] = subState;
            }
        }
        Cycle c = Cycle.createNew(this.solver.getNeedsDisplayCalculations(), this);
        c.name = baseCycle.name;
        for (int i = 0; i < result.length; i++) {
            Cycle subCycle = null;
            if (this.hasteProcs[i].procHasteRating == 0) {
                subCycle = baseCycle;
            } else {
                subCycle = this.hasteProcs[i].getNewCycle(cycleId);
            }
            c.addCycle(this.solver.getNeedsDisplayCalculations(), subCycle, result[i].chance() / subCycle.castTime);
        }
        if (baseProcHaste > 0) {
            this.hasteProcs = null;
        }
        return c;
    }

    private Cycle getNewCycle(CycleId cycleId) {
        Cycle c = null;
        switch (cycleId) {
            case FrostboltFOF:
                c = getSpell(SpellId.FrostboltFOF).getCycle();
                break;
            case Fireball:
                c = getSpell(SpellId.Fireball).getCycle();
                break;
            case FBPyro:
                c = FBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FBLBPyro:
                c = FBLBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FFBLBPyro:
                c = FFBLBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FBScPyro:
                c = FBScPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FFBPyro:
                c = FFBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FFBScPyro:
                c = FFBScPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FFBScLBPyro:
                c = FFBScLBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrostfireBoltFOF:
                c = getSpell(SpellId.FrostfireBoltFOF).getCycle();
                break;
            case ABABarSc:
                c = new ABABarSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABarCSc:
                c = new ABABarCSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABAMABarSc:
                c = new ABAMABarSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3ABarCSc:
                c = new AB3ABarCSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AMABarSc:
                c = new AB3AMABarSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3MBAMABarSc:
                c = new AB3MBAMABarSc(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ArcaneBlastSpam:
                c = getSpell(SpellId.ArcaneBlast4).getCycle();
                break;
            case ABSpam04MBAM:
                c = ABSpam04MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam024MBAM:
                c = ABSpam024MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam034MBAM:
                c = ABSpam034MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam0234MBAM:
                c = ABSpam0234MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam4MBAM:
                c = ABSpam4MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam24MBAM:
                c = ABSpam24MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam234MBAM:
                c = ABSpam234MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB4AM234MBAM:
                c = AB4AM234MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AM23MBAM:
                c = AB3AM23MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB4AM0234MBAM:
                c = AB4AM0234MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AM023MBAM:
                c = AB3AM023MBAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABarAM:
                c = new ABarAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABAM:
                c = ABAM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpamMBAM:
                c = new ABSpamMBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam3C:
                c = new ABSpam3C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam03C:
                c = new ABSpam03C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB2ABar3C:
                c = new AB2ABar3C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar2C:
                c = new ABABar2C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar2MBAM:
                c = new ABABar2MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar1MBAM:
                c = new ABABar1MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar3C:
                c = new ABABar3C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3ABar3MBAM:
                c = new AB3ABar3MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AM:
                c = AB3AM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB2AM:
                c = AB2AM.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AM2MBAM:
                c = new AB3AM2MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB2ABar2MBAM:
                c = new AB2ABar2MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar0MBAM:
                c = new ABABar0MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam3MBAM:
                c = new ABSpam3MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABSpam03MBAM:
                c = new ABSpam03MBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AMABar:
                c = AB3AMABar.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3AMABar2C:
                c = new AB3AMABar2C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB3ABar3C:
                c = new AB3ABar3C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar0C:
                c = new ABABar0C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABar1C:
                c = new ABABar1C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB2ABar2C:
                c = new AB2ABar2C(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case AB2ABarMBAM:
                c = new AB2ABarMBAM(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBFBIL:
                c = FrBFBIL.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBDFFBIL:
                c = FrBDFFBIL.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBDFFFB:
                c = FrBDFFFB.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBILFB:
                c = FrBILFB.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBIL:
                c = FrBIL.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FrBFB:
                c = FrBFB.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case FBScLBPyro:
                c = FBScLBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ScLBPyro:
                c = ScLBPyro.getCycle(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ABABarSlow:
                c = new ABABarSlow(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case CustomSpellMix:
                c = new SpellCustomMix(this.solver.getNeedsDisplayCalculations(), this);
                break;
            case ArcaneMissiles:
                c = getSpell(SpellId.ArcaneMissiles).getCycle();
                break;
            case Scorch:
                c = getSpell(SpellId.Scorch).getCycle();
                break;
            case ArcaneExplosion:
                c = getSpell(SpellId.ArcaneExplosion).getCycle();
                break;
            case FlamestrikeSpammed:
                c = getSpell(SpellId.FlamestrikeSpammed).getCycle();
                break;
            case FlamestrikeSingle:
                c = getSpell(SpellId.FlamestrikeSingle).getCycle();
                break;
            case Blizzard:
                c = getSpell(SpellId.Blizzard).getCycle();
                break;
            case BlastWave:
                c = getSpell(SpellId.BlastWave).getCycle();
                break;
            case DragonsBreath:
                c = getSpell(SpellId.DragonsBreath).getCycle();
                break;
            case ConeOfCold:
                c = getSpell(SpellId.ConeOfCold).getCycle();
                break;
            default:
                break;
        }
        return c;
    }

    public Spell getSpell(SpellId spellId) {
        Spell s = null;
        for (int i = 0; i < this.spellsCount; i++) {
            Spell spell = this.spells[i];
            if (spell.spellId == spellId) return spell;
        }
        switch (spellId) {
            case Waterbolt:
                s = this.solver.getWaterboltTemplate().getSpell(this);
                break;
            case MirrorImage:
                s = this.solver.getMirrorImageTemplate().getSpell(this);
                break;
            default:
                break;
        }
        if (s == null && this.referenceCastingState != null) {
            Spell reference = this.referenceCastingState.getSpell(spellId);
            if (reference.getClass() == Spell.class) {
                s = Spell.newFromReference(reference, this);
            }
        }
        if (s == null) {
            switch (spellId) {
                case FrostboltFOF:
                    s = this.solver.getFrostboltTemplate().getSpell(this, false, false, false, true);
                    break;
                case FrostfireBoltFOF:
                    s = this.solver.getFrostfireBoltTemplate().getSpell(this, false, true, false);
                    break;
                case FrostfireBoltBFFOF:
                    s = this.solver.getFrostfireBoltTemplate().getSpell(this, false, true, true);
                    break;
                case ArcaneMissiles:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, 0);
                    break;
                case ArcaneMissiles1:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, 1);
                    break;
                case ArcaneMissiles2:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, 2);
                    break;
                case ArcaneMissiles3:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, 3);
                    break;
                case ArcaneMissiles4:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, 4);
                    break;
                case ArcaneMissilesMB:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, true, 0);
                    break;
                case ArcaneMissilesMB1:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, true, 1);
                    break;
                case ArcaneMissilesMB2:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, true, 2);
                    break;
                case ArcaneMissilesMB3:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, true, 3);
                    break;
                case ArcaneMissilesMB4:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, true, 4);
                    break;
                case ArcaneMissilesNoProc:
                    s = this.solver.getArcaneMissilesTemplate().getSpell(this, false, true, false, false, 0, 5);
                    break;
                case Frostbolt:
                    s = this.solver.getFrostboltTemplate().getSpell(this);
                    break;
                case FrostboltNoCC:
                    s = this.solver.getFrostboltTemplate().getSpell(this, true, false, false, false);
                    break;
                case DeepFreeze:
                    s = this.solver.getDeepFreezeTemplate().getSpell(this);
                    break;
                case Fireball:
                    s = this.solver.getFireballTemplate().getSpell(this, false, false);
                    break;
                case FireballBF:
                    s = this.solver.getFireballTemplate().getSpell(this, false, true);
                    break;
                case FrostfireBolt:
                    s = this.solver.getFrostfireBoltTemplate().getSpell(this, false, false, false);
                    break;
                case FrostfireBoltBF:
                    s = this.solver.getFrostfireBoltTemplate().getSpell(this, false, false, true);
                    break;
                case Pyroblast:
                    s = this.solver.getPyroblastTemplate().getSpell(this, false, false);
                    break;
                case FireBlast:
                    s = this.solver.getFireBlastTemplate().getSpell(this);
                    break;
                case Scorch:
                    s = this.solver.getScorchTemplate().getSpell(this);
                    break;
                case ScorchNoCC:
                    s = this.solver.getScorchTemplate().getSpell(this, false);
                    break;
                case ArcaneBarrage:
                    s = this.solver.getArcaneBarrageTemplate().getSpell(this, 0);
                    break;
                case ArcaneBarrage1:
                    s = this.solver.getArcaneBarrageTemplate().getSpell(this, 1);
                    break;
                case ArcaneBarrage2:
                    s = this.solver.getArcaneBarrageTemplate().getSpell(this, 2);
                    break;
                case ArcaneBarrage3:
                    s = this.solver.getArcaneBarrageTemplate().getSpell(this, 3);
                    break;
                case ArcaneBarrage4:
                    s = this.solver.getArcaneBarrageTemplate().getSpell(this, 4);
                    break;
                case ArcaneBlast3:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 3);
                    break;
                case ArcaneBlast4:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 4);
                    break;
                case ArcaneBlast3NoCC:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 3, true, false, false);
                    break;
                case ArcaneBlastRaw:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this);
                    break;
                case ArcaneBlast0:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 0);
                    break;
                case ArcaneBlast0NoCC:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 0, true, false, false);
                    break;
                case ArcaneBlast1:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 1);
                    break;
                case ArcaneBlast1NoCC:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 1, true, false, false);
                    break;
                case ArcaneBlast2:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 2);
                    break;
                case ArcaneBlast2NoCC:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 2, true, false, false);
                    break;
                case ArcaneBlast0Hit:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 0, true);
                    break;
                case ArcaneBlast1Hit:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 1, true);
                    break;
                case ArcaneBlast2Hit:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 2, true);
                    break;
                case ArcaneBlast3Hit:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 3, true);
                    break;
                case ArcaneBlast0Miss:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 0, false);
                    break;
                case ArcaneBlast1Miss:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 1, false);
                    break;
                case ArcaneBlast2Miss:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 2, false);
                    break;
                case ArcaneBlast3Miss:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 3, false);
                    break;
                case IceLance:
                    s = this.solver.getIceLanceTemplate().getSpell(this);
                    break;
                case ArcaneExplosion:
                    s = this.solver.getArcaneExplosionTemplate().getSpell(this);
                    break;
                case FlamestrikeSpammed:
                    s = this.solver.getFlamestrikeTemplate().getSpell(this, true);
                    break;
                case FlamestrikeSingle:
                    s = this.solver.getFlamestrikeTemplate().getSpell(this, false);
                    break;
                case Blizzard:
                    s = this.solver.getBlizzardTemplate().getSpell(this);
                    break;
                case BlastWave:
                    s = this.solver.getBlastWaveTemplate().getSpell(this);
                    break;
                case DragonsBreath:
                    s = this.solver.getDragonsBreathTemplate().getSpell(this);
                    break;
                case ConeOfCold:
                    s = this.solver.getConeOfColdTemplate().getSpell(this);
                    break;
                case ArcaneBlast0POM:
                    s = this.solver.getArcaneBlastTemplate().getSpell(this, 0, false, false, true);
                    break;
                case FireballPOM:
                    s = this.solver.getFireballTemplate().getSpell(this, true, false);
                    break;
                case Slow:
                    s = this.solver.getSlowTemplate().getSpell(this);
                    break;
                case FrostboltPOM:
                    s = this.solver.getFrostboltTemplate().getSpell(this, false, false, true, false);
                    break;
                case PyroblastPOM:
                    s = this.solver.getPyroblastTemplate().getSpell(this, true, false);
                    break;
                case PyroblastPOMSpammed:
                    s = this.solver.getPyroblastTemplate().getSpell(this, true, true);
                    break;
                case PyroblastPOMDotUptime:
                    s = this.solver.getPyroblastTemplate().getSpell(this, true);
                    break;
                case LivingBomb:
                    s = this.solver.getLivingBombTemplate().getSpell(this);
                    break;
                case FireWard:
                    s = this.solver.getFireWardTemplate().getSpell(this);
                    break;
                case FrostWard:
                    s = this.solver.getFrostWardTemplate().getSpell(this);
                    break;
                default:
                    break;
            }
        }

        if (s != null) {
            s.spellId = spellId;
            if (this.spellsCount >= this.spells.length) {
                int length = 2 * this.spells.length;
                Spell[] destinationArray = new Spell[length];
                System.arraycopy(this.spells, 0, destinationArray, 0, this.spellsCount);
                this.spells = destinationArray;
            }
            this.spells[this.spellsCount++] = s;
        }

        return s;
    }
}
