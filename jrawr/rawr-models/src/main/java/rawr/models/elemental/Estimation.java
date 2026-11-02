// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Estimation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the unused private getTrinketStats() (only referenced from commented-out code in the
// original) is omitted.
package rawr.models.elemental;

import rawr.base.stats.BaseStats;
import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.talents.ShamanTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Estimation {
    SpellBox spellbox;

    private Stats baseStats, procStats;
    private ShamanTalents talents;
    private CalculationOptionsElemental calcOpts;

    public Estimation(Stats baseStats, Stats procStats, ShamanTalents talents, CalculationOptionsElemental calcOpts) {
        this.baseStats = baseStats;
        this.procStats = procStats;
        this.talents = talents;
        this.calcOpts = calcOpts;

        Stats addedStats = baseStats.copy();
        addedStats.accumulate(procStats);
        CombatFactors combatFactors = new CombatFactors(talents, addedStats, Math.max(calcOpts.getNumberOfTargets() - 1, 0), calcOpts.getLatencyCast(), calcOpts.getLatencyGcd(), calcOpts.getUseFireNova(), calcOpts.getUseChainLightning(), calcOpts.getUseDpsTotem());
        spellbox = new SpellBox(combatFactors);
    }

    /**
     * Beware when updating: The spells from an earlier returned Rotation are references to the
     * SpellBox from this Estimation.
     */
    public void update(Stats baseStats, Stats procStats, ShamanTalents talents, CalculationOptionsElemental calcOpts) {
        this.baseStats = baseStats;
        this.procStats = procStats;
        this.talents = talents;
        this.calcOpts = calcOpts;

        Stats addedStats = baseStats.copy();
        addedStats.accumulate(procStats);
        CombatFactors combatFactors = new CombatFactors(talents, addedStats, Math.max(calcOpts.getNumberOfTargets() - 1, 0), calcOpts.getLatencyCast(), calcOpts.getLatencyGcd(), calcOpts.getUseFireNova(), calcOpts.getUseChainLightning(), calcOpts.getUseDpsTotem());
        spellbox.update(combatFactors);
    }

    private Rotation getPriorityRotation(int type) {
        // Elemental Mastery: Temporary ignore Elemental Mastery
        // The talent is obviously useful but on a 3 minute cooldown.... so it's not very important for gear evaluation...
        return new Rotation(talents, spellbox, new RotationOptions(calcOpts.getUseFireNova(), calcOpts.getUseChainLightning(), calcOpts.getUseDpsTotem(), calcOpts.getUseFireEle()));
    }

    public static void solve(CharacterCalculationsElemental calculatedStats, CalculationOptionsElemental calcOpts, BossOptions bossOpts) {
        Stats stats = calculatedStats.basicStats;
        Character character = calculatedStats.localCharacter;
        ShamanTalents talents = character.getShamanTalents();

        /* Effects:
         * Clearcasting (-40% mana cost next 2 spells)
         * Glyph of flame shock or not
         * Clearcasting (5/10% more total damage)
         * Elemental Mastery (+15% haste chance, 15 sec/3 min cd)
         * Trinkets
         *
         * Assume LvB used on CD and FS either after LvB, on dot drop or before LvB
         * Filler: LB
         * NYI Optional: use of CL
         */
        Estimation e;
        Rotation rot;
        float damage;
        Stats procStats;
        float FightDuration = calcOpts.getFightDuration();

        // WITHOUT PROCS
        e = new Estimation(stats, new Stats(), talents, calcOpts);
        rot = e.getPriorityRotation(calcOpts.getRotationType());
        // WITH PROCS
        int nPasses = 2, k;
        for (k = 0; k < nPasses; k++) {
            procStats = doSpecialEffects(character, stats, rot, FightDuration);
            //procStats = getTrinketStats(character, stats, calcOpts.FightDuration, rot);
            e.update(stats, procStats, talents, calcOpts);
            rot = e.getPriorityRotation(calcOpts.getRotationType());
        }

        // Thunderstorm usage
        float thunderstormRegen = 0f;
        if (calcOpts.getUseThunderstorm()) {
            float procsPerSecond = Thunderstorm.getProcsPerSecond(talents.isGlyphofThunder(), (int) FightDuration);
            thunderstormRegen += (talents.isGlyphofThunderstorm() ? .1f : .08f) * stats.getMana() * procsPerSecond * 5;
        }

        /* Regen variables: (divide by 5 for regen per second)
         * While casting: ManaRegInFSR
         * During regen: ManaRegOutFSR */
        float spiRegen = 5 * StatConversion.getSpiritRegenSec(stats.getSpirit(), stats.getIntellect());
        float replenishRegen = 5 * stats.getMana() * stats.getManaRestoreFromMaxManaPerSecond();
        float judgementRegen = 5 * rot.getBaseCastTime() / rot.getDuration() * stats.getManaRestoreFromBaseManaPPM() / 60f * BaseStats.getBaseStats(character).getMana();
        float ManaRegInFSR = spiRegen * stats.getSpellCombatManaRegeneration() + stats.getMp5() + replenishRegen + judgementRegen + thunderstormRegen;
        float ManaRegOutFSR = spiRegen + stats.getMp5() + replenishRegen + thunderstormRegen;
        float ManaRegen = ManaRegInFSR;

        // TotalDamage, CastFraction, TimeUntilOOM
        float TimeUntilOOM = 0;
        float effectiveMPS = rot.getMPS() - ManaRegen / 5f;
        if (effectiveMPS <= 0) TimeUntilOOM = FightDuration;
        else TimeUntilOOM = (calculatedStats.basicStats.getMana()) / effectiveMPS;
        if (TimeUntilOOM > FightDuration) TimeUntilOOM = FightDuration;

        // SpecialEffects from procs etc.
        procStats = doSpecialEffects(character, stats, rot, FightDuration);
        //damage procs (Thunder Capacitor etc.) are effected by spellcrit and damage debuffs
        damage = procStats.getArcaneDamage() * (1 + stats.getBonusArcaneDamageMultiplier()) + procStats.getNatureDamage() * (1 + stats.getBonusNatureDamageMultiplier()) + procStats.getFireDamage() * (1 + stats.getBonusFireDamageMultiplier()) + procStats.getShadowDamage() * (1 + stats.getBonusShadowDamageMultiplier());
        if (damage > 0) {
            damage *= (1 + stats.getSpellCrit() * .5f); // but only with the normal 50% dmg bonus
            rot.setDPS(rot.getDPS() + damage);
        }

        float TotalDamage = TimeUntilOOM * rot.getDPS();
        float TimeToRegenFull = 5f * calculatedStats.basicStats.getMana() / ManaRegOutFSR;
        float TimeToBurnAll = calculatedStats.basicStats.getMana() / effectiveMPS;
        float CastFraction = 1f;
        if (ManaRegOutFSR > 0 && FightDuration > TimeUntilOOM) {
            float timeLeft = FightDuration - TimeUntilOOM;
            if (TimeToRegenFull + TimeToBurnAll == 0)
                CastFraction = 0;
            else
                CastFraction = TimeToBurnAll / (TimeToRegenFull + TimeToBurnAll);
            TotalDamage += timeLeft * rot.getDPS() * CastFraction;
        }

        float bsRatio = ((float) calcOpts.getBSRatio()) * 0.01f;
        calculatedStats.setBurstPoints((1f - bsRatio) * 2f * rot.getDPS());
        calculatedStats.setSustainedPoints(bsRatio * 2f * TotalDamage / FightDuration);
        calculatedStats.setOverallPoints(calculatedStats.getBurstPoints() + calculatedStats.getSustainedPoints());

        calculatedStats.combatStats = stats.copy();
        calculatedStats.combatStats.accumulate(procStats);

        calculatedStats.manaRegenInFSR = ManaRegInFSR;
        calculatedStats.manaRegenOutFSR = ManaRegOutFSR;
        calculatedStats.replenishMP5 = replenishRegen;
        calculatedStats.lightningBolt = rot.LB;
        calculatedStats.chainLightning = rot.CL;
        calculatedStats.flameShock = rot.FS;
        calculatedStats.lavaBurst = rot.LvB;
        calculatedStats.earthShock = rot.ES;
        calculatedStats.frostShock = rot.FrS;
        calculatedStats.fireNova = rot.FN;
        calculatedStats.searingTotem = rot.ST;
        calculatedStats.magmaTotem = rot.MT;
        calculatedStats.timeToOOM = TimeUntilOOM;
        calculatedStats.castRegenFraction = CastFraction;
        calculatedStats.castsPerSecond = rot.getCastsPerSecond();
        calculatedStats.critsPerSecond = rot.getWeightedCritchance() * rot.getCastsPerSecond();
        calculatedStats.missesPerSecond = rot.getCastsPerSecond() * (1f - rot.getWeightedHitchance());
        calculatedStats.lvBPerSecond = rot.getCastsPerSecond(LavaBurst.class);
        calculatedStats.lBPerSecond = rot.getCastsPerSecond(LightningBolt.class);
        calculatedStats.fSPerSecond = rot.getCastsPerSecond(FlameShock.class);
        calculatedStats.latencyPerSecond = rot.getLatencyPerSecond();
        calculatedStats.rotationDPS = rot.getDPS();
        calculatedStats.rotationMPS = rot.getMPS();
        calculatedStats.totalDPS = TotalDamage / FightDuration;
        Float ccFs = rot.getClearCasting().get(FlameShock.class);
        calculatedStats.clearCast_FlameShock = ccFs != null ? ccFs : 0f;
        Float ccLvb = rot.getClearCasting().get(LavaBurst.class);
        calculatedStats.clearCast_LavaBurst = ccLvb != null ? ccLvb : 0f;
        Float ccLb = rot.getClearCasting().get(LightningBolt.class);
        calculatedStats.clearCast_LightningBolt = ccLb != null ? ccLb : 0f;
        calculatedStats.rotation = rot.toString();
        calculatedStats.rotationDetails = rot.toDetailedString();
    }

    protected static void calculateTriggers(Rotation rot, LinkedHashMap<Trigger, Float> triggerIntervals, LinkedHashMap<Trigger, Float> triggerChances) {
        float CastInterval = 1f / rot.getCastsPerSecond();
        float WeightedCritChance = rot.getWeightedCritchance();
        float WeightedHitChance = rot.getWeightedHitchance();

        triggerIntervals.put(Trigger.Use, 0f);
        triggerChances.put(Trigger.Use, 1f);

        triggerIntervals.put(Trigger.DamageDone, 1f / (rot.getCastsPerSecond() + 1f / rot.FS.getPeriodicTickTime()));
        // flameshock ticks are not taken into account. the chance would be slightly higher.
        triggerChances.put(Trigger.DamageDone, WeightedHitChance);

        triggerIntervals.put(Trigger.DamageOrHealingDone, 1f / (rot.getCastsPerSecond() + 1f / rot.FS.getPeriodicTickTime()));
        // flameshock ticks are not taken into account. the chance would be slightly higher.
        // Need to add Self-Heals
        triggerChances.put(Trigger.DamageOrHealingDone, WeightedHitChance);

        triggerIntervals.put(Trigger.SpellMiss, CastInterval);
        triggerChances.put(Trigger.SpellMiss, 1f - WeightedHitChance);

        triggerIntervals.put(Trigger.SpellHit, CastInterval);
        triggerChances.put(Trigger.SpellHit, WeightedHitChance);

        triggerIntervals.put(Trigger.DamageSpellHit, CastInterval);
        triggerChances.put(Trigger.DamageSpellHit, WeightedHitChance);

        triggerIntervals.put(Trigger.DoTTick, 1f / rot.FS.getPeriodicTickTime());
        triggerChances.put(Trigger.DoTTick, 1f);

        triggerIntervals.put(Trigger.SpellCast, CastInterval);
        triggerChances.put(Trigger.SpellCast, 1f);

        triggerIntervals.put(Trigger.DamageSpellCast, CastInterval);
        triggerChances.put(Trigger.DamageSpellCast, 1f);

        triggerIntervals.put(Trigger.SpellCrit, CastInterval);
        triggerChances.put(Trigger.SpellCrit, WeightedCritChance);

        triggerIntervals.put(Trigger.DamageSpellCrit, CastInterval);
        triggerChances.put(Trigger.DamageSpellCrit, WeightedCritChance);

        triggerIntervals.put(Trigger.ShamanLightningBolt, 1f / rot.getCastsPerSecond(LightningBolt.class));
        triggerChances.put(Trigger.ShamanLightningBolt, 1f);

        triggerIntervals.put(Trigger.ShamanShock, 1f / rot.getCastsPerSecond(Shock.class));
        triggerChances.put(Trigger.ShamanShock, 1f);

        triggerIntervals.put(Trigger.ShamanFlameShockDoTTick, 1f / rot.getTicksPerSecond(FlameShock.class));
        triggerChances.put(Trigger.ShamanFlameShockDoTTick, 1f);
    }

    protected static Stats doSpecialEffects(Character character, Stats stats, Rotation rot, float FightDuration) {
        // Initialize Triggers
        LinkedHashMap<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        LinkedHashMap<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        calculateTriggers(rot, triggerIntervals, triggerChances);

        Stats procStats = new Stats();

        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : stats.specialEffects()) {
            effect.getStats().generateSparseData();

            // Filter out unhandled effects
            if (!triggerIntervals.containsKey(effect.getTrigger())) continue;

            effects.add(effect);
        }

        accumulateSpecialEffects(character, procStats, FightDuration, triggerIntervals, triggerChances, effects, 1f);
        return procStats;
    }

    protected static void accumulateSpecialEffects(Character character, Stats stats, float FightDuration, LinkedHashMap<Trigger, Float> triggerIntervals, LinkedHashMap<Trigger, Float> triggerChances, List<SpecialEffect> effects, float weight) {
        for (SpecialEffect effect : effects) {
            Stats effectStats = effect.getStats();

            float upTime = 0f;

            if (effect.getTrigger() == Trigger.Use) {
                if (effect.getStats().getRawSpecialEffectDataSize() >= 1) {
                    upTime = effect.getAverageUptime(0f, 1f, 0, FightDuration);
                    List<SpecialEffect> nestedEffect = new ArrayList<SpecialEffect>();
                    for (SpecialEffect nested : effect.getStats().specialEffects()) nestedEffect.add(nested);
                    Stats _stats2 = effectStats.copy();
                    accumulateSpecialEffects(character, _stats2, effect.getDuration(), triggerIntervals, triggerChances, nestedEffect, upTime);
                    effectStats = _stats2;
                } else {
                    upTime = effect.getAverageStackSize(0f, 1f, 0, FightDuration);
                }
            } else if (effect.getDuration() == 0f) {
                upTime = effect.getAverageProcsPerSecond(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()),
                        0, FightDuration);
            } else if (triggerIntervals.containsKey(effect.getTrigger())) {
                upTime = effect.getAverageStackSize(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()),
                        0, FightDuration);
            }

            if (upTime > 0f) {
                stats.accumulate(effectStats, upTime * weight);
            }
        }
    }
}
