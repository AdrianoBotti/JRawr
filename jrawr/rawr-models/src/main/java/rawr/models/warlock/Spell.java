// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/Spells.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the C# reflection over Assembly.GetExecutingAssembly().GetTypes() for
// ALL_SPELLS and Type.GetType("Rawr.Warlock."+name) in CharacterCalculationsWarlock.GetSpell is
// replaced by an explicit ALL_SPELLS list and a switch factory in the port.
package rawr.models.warlock;

import rawr.base.stats.MagicSchool;
import rawr.base.util.NetFormat;
import rawr.base.util.Utilities;
import rawr.base.stats.WeightedStat;
import rawr.base.talents.WarlockTalents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Spell {

    public enum SpellTree { Affliction, Demonology, Destruction }

    public static final List<String> ALL_SPELLS = new ArrayList<String>(Arrays.asList(
            "Chaos Bolt",
            "Conflagrate",
            "Corruption",
            "Curse Of Agony",
            "Curse Of Doom",
            "Curse Of The Elements",
            "Haunt",
            "Immolate",
            "Immolation Aura",
            "Incinerate",
            "Incinerate (Under Backdraft)",
            "Incinerate (Under Molten Core)",
            "Life Tap",
            "Shadow Bolt",
            "Shadow Bolt (Instant)",
            "Soul Fire",
            "Unstable Affliction"));

    public static float calcUprate(float procRate, float duration, float triggerPeriod) {
        return 1f - (float) Math.pow(1f - procRate, duration / triggerPeriod);
    }

    public static float getTimeUsed(
            float baseCastTime,
            float gcdBonus,
            List<WeightedStat> haste,
            float lag) {

        float minGCD = 1f - gcdBonus;
        float unhasted = Math.max(baseCastTime, 1.5f - gcdBonus);

        float avgHasted = 0f;
        for (WeightedStat h : haste) {
            avgHasted += h.chance() * Math.max(minGCD, unhasted / h.value());
        }
        return avgHasted + lag;
    }

    public CharacterCalculationsWarlock mommy;
    public MagicSchool magicSchool;
    public SpellTree mySpellTree;
    public float manaCost;
    public float baseCastTime;
    public float gcdBonus;
    public boolean canMiss;
    public float baseDamage;
    public float directCoefficient;
    public float baseTickDamage;
    public float tickCoefficient;
    public boolean canTickCrit;

    // set via constructor but only used for internal optimizations
    public boolean recordMissesSeparately = false;

    // set via constructor but sometimes modified via RecordCollisionDelay()
    public float cooldown;
    public float recastPeriod;
    public float numTicks;
    public SpellModifiers spellModifiers;

    // set via RecordCollisionDelay()
    public Map<String, SimulatedStat> simulatedStats;

    // set via SetDamageStats()
    public float avgDirectHit;
    public float avgDirectCrit;
    public float avgTickHit;
    public float avgTickCrit;
    public float avgDamagePerCast;

    // cached values (can be determined from the values in other props)
    protected float numCasts;
    protected float avgDelay;

    public Spell(
            CharacterCalculationsWarlock mommy,
            MagicSchool magicSchool,
            SpellTree spellTree,
            float percentBaseMana,
            float baseCastTime,
            float cooldown,
            float recastPeriod,
            boolean canMiss) {
        this(
                mommy,
                magicSchool,
                spellTree,
                percentBaseMana,
                baseCastTime,
                cooldown,
                recastPeriod,
                canMiss,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                false,
                0f,
                0f);
    }

    public Spell(
            CharacterCalculationsWarlock mommy,
            MagicSchool magicSchool,
            SpellTree spellTree,
            float percentBaseMana,
            float baseCastTime,
            float cooldown,
            float recastPeriod,
            float lowDirectDamage,
            float highDirectDamage,
            float directCoefficient,
            float addedDirectMultiplier,
            float bonusCritChance,
            float bonusCritMultiplier) {
        this(
                mommy,
                magicSchool,
                spellTree,
                percentBaseMana,
                baseCastTime,
                cooldown,
                recastPeriod,
                true,
                lowDirectDamage,
                highDirectDamage,
                directCoefficient,
                addedDirectMultiplier,
                0f,
                0f,
                0f,
                0f,
                false,
                bonusCritChance,
                bonusCritMultiplier);
    }

    public Spell(
            CharacterCalculationsWarlock mommy,
            MagicSchool magicSchool,
            SpellTree spellTree,
            float percentBaseMana,
            float baseCastTime,
            float cooldown,
            float recastPeriod,
            float baseTickDamage,
            float numTicks,
            float tickCoefficient,
            float addedTickMultiplier,
            boolean canTickCrit,
            float bonusCritChance,
            float bonusCritMultiplier) {
        this(
                mommy,
                magicSchool,
                spellTree,
                percentBaseMana,
                baseCastTime,
                cooldown,
                recastPeriod,
                true,
                0f,
                0f,
                0f,
                0f,
                baseTickDamage,
                numTicks,
                tickCoefficient,
                addedTickMultiplier,
                canTickCrit,
                bonusCritChance,
                bonusCritMultiplier);
    }

    public Spell(
            CharacterCalculationsWarlock mommy,
            MagicSchool magicSchool,
            SpellTree spellTree,
            float percentBaseMana,
            float baseCastTime,
            float cooldown,
            float recastPeriod,
            boolean canMiss,
            float lowDirectDamage,
            float highDirectDamage,
            float directCoefficient,
            float addedDirectMultiplier,
            float baseTickDamage,
            float numTicks,
            float tickCoefficient,
            float addedTickMultiplier,
            boolean canTickCrit,
            float bonusCritChance,
            float bonusCritMultiplier) {

        this.mommy = mommy;
        this.magicSchool = magicSchool;
        this.mySpellTree = spellTree;
        manaCost = mommy.baseMana * percentBaseMana;
        this.baseCastTime = baseCastTime;
        baseDamage = (lowDirectDamage + highDirectDamage) / 2f;
        this.cooldown = cooldown;
        this.recastPeriod = recastPeriod;
        this.canMiss = canMiss;
        this.directCoefficient = directCoefficient;
        this.baseTickDamage = baseTickDamage;
        this.numTicks = numTicks;
        this.tickCoefficient = tickCoefficient;
        this.canTickCrit = canTickCrit;

        simulatedStats = new LinkedHashMap<String, SimulatedStat>();

        spellModifiers = new SpellModifiers();
        spellModifiers.addAdditiveDirectMultiplier(addedDirectMultiplier);
        spellModifiers.addAdditiveTickMultiplier(addedTickMultiplier);
        spellModifiers.addCritChance(bonusCritChance);
        spellModifiers.addCritBonusMultiplier(bonusCritMultiplier);
        if (spellTree == SpellTree.Destruction) {
            float[] talentAffects = { 0f, .04f, .07f, .1f };
            manaCost *= 1 - talentAffects[mommy.talents.getCataclysm()];
        } else if (spellTree == SpellTree.Affliction) {
            manaCost *= 1 - mommy.talents.getSuppression() * .01f;
        }
    }

    protected Rotation getRotation() {
        return mommy.options.getActiveRotation();
    }

    protected void applyImprovedSoulLeech() {
        float reductionOnProc = mommy.calcMana() * mommy.talents.getImprovedSoulLeech() * .01f;
        manaCost -= .3f * reductionOnProc;
    }

    public boolean isCastable() {
        return true;
    }

    public String getToolTip() {
        float numCasts = getNumCasts();
        float castTime = getAvgTimeUsed() - mommy.options.getLatency();

        String toolTip;
        if (avgDamagePerCast > 0) {
            float dps = numCasts * avgDamagePerCast / mommy.options.getDuration();
            toolTip = NetFormat.fixed(dps, 1) + " dps ("
                    + NetFormat.percent(dps / mommy.getPersonalDps(), 1) + ")*";
        } else {
            toolTip = NetFormat.fixed(numCasts, 1) + " casts*";
        }
        if (avgDirectHit > 0) {
            toolTip += NetFormat.fixed(avgDirectHit, 1) + "\tAverage Hit\r\n"
                    + NetFormat.fixed(avgDirectCrit, 1) + "\tAverage Crit\r\n";
        }
        if (avgTickHit > 0) {
            toolTip += NetFormat.fixed(avgTickHit, 1) + "\tAverage Tick\r\n"
                    + NetFormat.fixed(avgTickCrit, 1) + "\tAverage Tick Crit\r\n"
                    + NetFormat.fixed(numTicks, 1) + "\tTicks Per Cast\r\n";
        }
        if (avgDamagePerCast > 0) {
            toolTip += "\r\n";
        }
        if (avgDirectHit > 0) {
            toolTip += NetFormat.fixed(100 * directCoefficient, 1) + "%\tDirect Coefficient\r\n";
        }
        if (avgTickHit > 0) {
            toolTip += NetFormat.fixed(100 * tickCoefficient, 1) + "%\tPer Tick Coefficient\r\n";
        }
        if (avgDirectHit > 0) {
            toolTip += NetFormat.fixed(100 * spellModifiers.getFinalDirectMultiplier(), 1) + "%\tDirect Multiplier\r\n";
        }
        if (avgTickHit > 0) {
            toolTip += NetFormat.fixed(100 * spellModifiers.getFinalTickMultiplier(), 1) + "%\tPer Tick Multiplier\r\n";
        }
        if (avgDirectCrit > avgDirectHit || avgTickCrit > avgTickHit) {
            toolTip += NetFormat.fixed(100 * spellModifiers.getCritChance(), 1) + "%\tCrit Chance\r\n"
                    + NetFormat.fixed(100 * spellModifiers.getFinalCritMultiplier(), 0) + "%\tCrit Multiplier\r\n";
        }
        if (avgDamagePerCast > 0) {
            toolTip += NetFormat.fixed(100 * getResist(), 1) + "%\tResisted\r\n\r\n";
        }
        toolTip += NetFormat.fixed(castTime, 2) + "s\tCast Time\r\n"
                + NetFormat.fixed(manaCost, 1) + "\tMana\r\n";
        if (cooldown > 0) {
            toolTip += NetFormat.fixed(cooldown, 1) + "s\tCooldown\r\n";
        }
        toolTip += NetFormat.fixed(getAvgTimeBetweenCasts(), 1) + "s\tBetween Casts (Average)\r\n";
        if (avgDamagePerCast > 0) {
            toolTip += "\r\n"
                    + NetFormat.fixed(avgDamagePerCast, 1) + "\tDPC\r\n"
                    + NetFormat.fixed(avgDamagePerCast / castTime, 1) + "\tDPCT\r\n"
                    + NetFormat.fixed(avgDamagePerCast / manaCost, 1) + "\tDPM\r\n"
                    + NetFormat.fixed(numCasts, 1) + "\tCasts";
        }
        return toolTip;
    }

    public void spam(float timeRemaining) {
        numCasts += timeRemaining / getAvgTimeUsed();
        if (canGetPyroclasm()) {
            Spell conflagrate = mommy.castSpells.get("Conflagrate");
            CastingState fillerState = new CastingState(mommy, null, 1f);
            recordSimulatedStat(
                    "Pyroclasm Uprate",
                    conflagrate.getUprate(fillerState, this),
                    1f);
        }
    }

    public float getNumCasts() {
        if (numCasts == 0) {
            float duration = mommy.options.getDuration();
            if (!isCastDuringExecute()) {
                duration *= 1 - mommy.getExecutePercentage();
            }
            float delay = simulatedStats.get("delay").getValue();
            numCasts = duration / (getAvgRequeueTime() + delay);
        }
        return numCasts;
    }

    public boolean isCastDuringExecute() {
        return true;
    }

    private boolean isBinary() {
        return false;
    }

    public float getQueueProbability(CastingState state) {
        if (state.cooldowns.containsKey(this)) {
            if (state.cooldowns.get(this) <= 0) {
                return 1f;
            } else {
                return 0f;
            }
        } else {
            float maxQueued = state.getMaxTimeQueued(this);
            float unqueuable = state.elapsed - maxQueued;
            return Math.min(maxQueued / (getAvgRequeueTime() - unqueuable), 1f);
        }
    }

    public float getAvgRequeueTime() {
        // TODO this should really not be averaged (for the cases this
        // method is used).  Instead it should return all possible cooldowns
        // and their probabilities (and calling methods should be modified
        // accordingly).

        float period = Math.max(recastPeriod, cooldown);
        float hitChance = mommy.hitChance;
        if (isBinary()) {
            hitChance -= getResist();
        }
        if (canMiss && hitChance < 1 && cooldown < period) {
            // If a spell misses, and it can be recast sooner than it
            // normally would otherwise, it will instead wait for either its
            // cooldown (if it has one), or for one spell to be cast
            // inbetween (to allow for travel time + reaction time).
            float missDelay = Math.max(cooldown + getCastTime(null), mommy.avgTimeUsed);
            period = Utilities.getWeightedSum(period, hitChance, missDelay, 1 - hitChance);
        }
        return period;
    }

    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        // record stats about this spellcast
        float p = chanceOfCast * stateBeforeCast.probability;
        float timeUsed = getTimeUsed(stateBeforeCast);
        recordSimulatedStat("delay", stateBeforeCast.getMaxTimeQueued(this) / 2f, p);
        recordSimulatedStat("time used", timeUsed, p);
        if (canGetPyroclasm()) {
            Spell conflagrate = mommy.castSpells.get("Conflagrate");
            recordSimulatedStat(
                    "Pyroclasm Uprate",
                    conflagrate.getUprate(stateBeforeCast, this),
                    p);
        }

        // construct the casting state(s) that can result from this cast
        List<CastingState> results = new ArrayList<CastingState>();
        float hitChance = mommy.hitChance;
        if (isBinary()) {
            hitChance -= getResist();
        }
        float newCooldown = cooldown - timeUsed + getCastTime(stateBeforeCast);
        if (canMiss
                && hitChance < 1f
                && (recordMissesSeparately || cooldown < recastPeriod)) {

            // state when spell hits
            populateNextState(
                    results,
                    stateBeforeCast,
                    timeUsed,
                    Math.max(recastPeriod - timeUsed, newCooldown),
                    hitChance * chanceOfCast,
                    true);

            // state when spell misses
            if (newCooldown <= 0) {
                // ensure at least 1 spell is cast before this one is
                // requeued, to allow for travel time + reaction time for
                // the player to detect the miss
                newCooldown = .0001f;
            }
            populateNextState(
                    results,
                    stateBeforeCast,
                    timeUsed,
                    newCooldown,
                    (1 - hitChance) * chanceOfCast,
                    false);
        } else {
            populateNextState(
                    results,
                    stateBeforeCast,
                    timeUsed,
                    Math.max(newCooldown, recastPeriod - timeUsed),
                    chanceOfCast,
                    true);
        }

        return results;
    }

    public void recordSimulatedStat(String statName, float value, float weight) {
        if (!simulatedStats.containsKey(statName)) {
            simulatedStats.put(statName, new SimulatedStat());
        }
        simulatedStats.get(statName).addSample(value, weight);
    }

    private void populateNextState(
            List<CastingState> results,
            CastingState stateBeforeCast,
            float timeUsed,
            float cooldownAfterAdvance,
            float p,
            boolean isHit) {

        CastingState nextState = new CastingState(stateBeforeCast);
        nextState.probability *= p;
        nextState.addSpell(this, timeUsed, isHit);
        nextState.cooldowns.put(this, cooldownAfterAdvance);

        if (mySpellTree == SpellTree.Destruction
                && nextState.backdraftCharges > 0) {
            --nextState.backdraftCharges;
        }

        results.add(nextState);
    }

    public float getTimeUsed(CastingState state) {
        return maybeApplyBackdraft(
                getTimeUsed(baseCastTime, gcdBonus, mommy.haste, mommy.options.getLatency()),
                state);
    }

    public float getCastTime(CastingState state) {
        if (baseCastTime == 0f) {
            return 0f;
        }

        float avg = 0f;
        for (WeightedStat h : mommy.haste) {
            avg += h.chance() * baseCastTime / h.value();
        }
        return maybeApplyBackdraft(avg, state);
    }

    private float maybeApplyBackdraft(float time, CastingState state) {
        if (state != null
                && state.backdraftCharges > 0
                && mySpellTree == SpellTree.Destruction) {
            time /= 1f + mommy.talents.getBackdraft() * .1f;
        }
        return time;
    }

    public float getAvgTimeUsed() {
        if (simulatedStats.containsKey("time used")) {
            return simulatedStats.get("time used").getValue();
        } else {
            return getTimeUsed(baseCastTime, gcdBonus, mommy.haste, mommy.options.getLatency());
        }
    }

    public float getTotalWeight() {
        return simulatedStats.get("delay").getTotalWeight();
    }

    public float getAvgTimeBetweenCasts() {
        return mommy.options.getDuration() / getNumCasts();
    }

    /**
     * Assumes the effect == requeue time.
     */
    public float getUprate(CastingState state, Spell spell) {
        float castTime = spell.getCastTime(state);
        if (state.cooldowns.containsKey(this)) {
            float cooldown = state.cooldowns.get(this) - castTime;
            if (cooldown <= 0 || !state.lastCastHit(this)) {
                return 0f;
            } else {
                return 1f;
            }
        }

        float maxQueued = castTime;
        if (mommy.isPriorityOrdered(spell, this)) {
            maxQueued += state.getMaxTimeQueued(this);
        }
        float unqueuable = (state.elapsed + castTime) - maxQueued;
        float chanceQueued = Math.min(maxQueued / (getAvgRequeueTime() - unqueuable), 1f);
        return 1 - chanceQueued;
    }

    public void adjustAfterCastingIsSet() {
        // here for subclasses to override if desired.
    }

    public void finalizeSpellModifiers() {
        spellModifiers.accumulate(mommy.spellModifiers);
        if (mommy.options.getImbue().equals("Grand Spellstone")) {
            if (!getClass().getSimpleName().startsWith("Curse")) {
                spellModifiers.addAdditiveTickMultiplier(.01f);
            }
        } else {
            spellModifiers.addAdditiveDirectMultiplier(.01f);
        }
        if (magicSchool == MagicSchool.Shadow) {
            mommy.addShadowModifiers(spellModifiers);
        } else if (magicSchool == MagicSchool.Fire) {
            mommy.addFireModifiers(spellModifiers);
        }
        if (mySpellTree == SpellTree.Destruction) {
            spellModifiers.addCritBonusMultiplier(mommy.talents.getRuin() * .2f);
            spellModifiers.addCritChance(mommy.talents.getDevastation() * .05f);
        }
        if (canGetPyroclasm()) {
            Spell conflagrate = mommy.castSpells.get("Conflagrate");
            spellModifiers.addMultiplicativeMultiplier(
                    conflagrate.spellModifiers.getCritChance()
                            * simulatedStats.get("Pyroclasm Uprate").getValue()
                            * mommy.talents.getPyroclasm() * .02f);
        }
        if (mommy.talents.getEmpoweredImp() > 0 && mommy.pet instanceof Imp) {
            spellModifiers.addCritChance(
                    getAvgTimeUsed()
                            * ((Imp) mommy.pet).getCritsPerSec()
                            * mommy.talents.getEmpoweredImp()
                            / 3f);
        }
    }

    public void setDamageStats(float baseSpellPower) {
        avgDirectHit = (baseDamage + directCoefficient * baseSpellPower)
                * spellModifiers.getFinalDirectMultiplier()
                * (1 - getResist());
        avgTickHit = (baseTickDamage + tickCoefficient * baseSpellPower)
                * spellModifiers.getFinalTickMultiplier()
                * (1 - getResist());

        // crit direct damage
        float critChance = spellModifiers.getCritChance();
        float critMultiplier = spellModifiers.getFinalCritMultiplier();

        // crit direct damage
        avgDirectCrit = avgDirectHit * critMultiplier;
        float avgDirectDamage = Utilities.getWeightedSum(
                avgDirectCrit,
                critChance,
                avgDirectHit,
                1 - critChance);

        // crit tick damage (if possible)
        float avgTickDamage;
        if (canTickCrit) {
            avgTickCrit = avgTickHit * critMultiplier;
            avgTickDamage = Utilities.getWeightedSum(
                    avgTickHit, 1 - critChance, avgTickCrit, critChance);
        } else {
            avgTickCrit = avgTickHit;
            avgTickDamage = avgTickHit;
        }

        // overall damage
        avgDamagePerCast = mommy.hitChance
                * (avgDirectDamage + numTicks * avgTickDamage);
    }

    protected float getResist() {
        return rawr.base.stats.StatConversion.getAverageResistance(
                80, mommy.options.getTargetLevel(), 0f, 0f);
    }

    private boolean canGetPyroclasm() {
        return mommy.talents.getPyroclasm() > 0
                && !(this instanceof Conflagrate)
                && (magicSchool == MagicSchool.Fire || magicSchool == MagicSchool.Shadow)
                && mommy.castSpells.containsKey("Conflagrate");
    }
}

class ChaosBolt extends Spell {

    public ChaosBolt(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Destruction,
                .07f,
                2.5f - mommy.talents.getBane() * .1f,
                mommy.talents.isGlyphChaosBolt() ? 10f : 12f,
                0f,
                1429f,
                1813f,
                (1 + mommy.talents.getShadowAndFlame() * .04f) * .7142f,
                0f,
                0f,
                0f);
        applyImprovedSoulLeech();
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getChaosBolt() > 0;
    }

    @Override
    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        if (mommy.talents.getFireAndBrimstone() > 0) {
            ((Immolate) mommy.getSpell("Immolate")).recordUpChance(this, stateBeforeCast);
        }
        return super.simulateCast(stateBeforeCast, chanceOfCast);
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        if (mommy.talents.getFireAndBrimstone() > 0) {
            float uprate = simulatedStats.get("immolate up-chance").getValue();
            float fullBonus = mommy.talents.getFireAndBrimstone() * .02f;
            spellModifiers.addMultiplicativeMultiplier(uprate * fullBonus);
        }
    }
}

class Conflagrate extends Spell {

    public static final float COOLDOWN = 10f;

    public static boolean willBeCast(CharacterCalculationsWarlock mommy) {
        return mommy.options.getActiveRotation().contains("Conflagrate")
                && mommy.talents.getConflagrate() > 0;
    }

    public Conflagrate(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Destruction,
                .16f,
                0f,
                COOLDOWN,
                6f,
                true,
                .6f * 785f,
                .6f * 785f,
                .6f * .2f * 5f,
                0f,
                .4f * 785f / 3f,
                3f,
                .4f * .2f * 5f / 3f,
                0f,
                true,
                mommy.talents.getFireAndBrimstone() * .05f,
                0f);
        applyImprovedSoulLeech();
        recordMissesSeparately = true;
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getConflagrate() > 0;
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();

        // For some reason I may never understand, firestone's 1% bonus gets
        // 1% of Shadow and Flame and 1% of Emberstorm subtracted from it.
        // Also, that modifier becomes multiplicitive instead of additive.
        float direct = spellModifiers.getAdditiveDirectMultiplier();
        spellModifiers.addAdditiveDirectMultiplier(-direct);
        direct -= direct * mommy.talents.getEmberstorm() * .03f;
        spellModifiers.addMultiplicativeDirectMultiplier(direct);

        // Also account for improvements to immolate, which in turn improve
        // conflagrate
        spellModifiers.addAdditiveMultiplier(
                mommy.talents.getImprovedImmolate() * .1f
                        + (mommy.talents.isGlyphImmolate() ? .1f : 0f)
                        + mommy.talents.getAftermath() * .03f);
        if (mommy.stats.getWarlock2T8() > 0) {
            spellModifiers.addAdditiveMultiplier(.1f);
        }
    }

    @Override
    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        List<CastingState> states = super.simulateCast(stateBeforeCast, chanceOfCast);

        CastingState stateOnHit = states.get(0);
        if (mommy.talents.getBackdraft() > 0) {
            stateOnHit.backdraftCharges = 3;
        }
        if (!mommy.talents.isGlyphConflag()) {
            for (CastingState state : states) {
                state.cooldowns.put(mommy.getSpell("Immolate"), -getTimeUsed(stateBeforeCast));
            }
        }
        return states;
    }
}

class Corruption extends Spell {

    public static float getTickPeriod(CharacterCalculationsWarlock mommy) {
        float period = 3.1f; // total guess
        if (mommy.talents.isGlyphQuickDecay()) {
            period /= mommy.avgHaste;
        }
        return period;
    }

    public Corruption(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Affliction,
                .14f,
                0f,
                0f,
                18f,
                1080f / 6f,
                6f,
                (1.2f + mommy.talents.getEmpoweredCorruption() * .12f) / 6f
                        + mommy.talents.getEverlastingAffliction() * .01f,
                mommy.talents.getImprovedCorruption() * .02f
                        + mommy.talents.getContagion() * .01f
                        + mommy.talents.getSiphonLife() * .05f,
                mommy.talents.getPandemic() > 0,
                (mommy.talents.getMalediction() * .03f + mommy.stats.getWarlock2T10())
                        * mommy.talents.getPandemic(),
                mommy.talents.getPandemic());

        if (mommy.talents.isGlyphQuickDecay()) {
            recastPeriod /= mommy.avgHaste;
        }

        // region rolling corruption
        // Malficus solved the average duration of a rolling Corruption, see
        // http://rawr.codeplex.com/Thread/View.aspx?ThreadId=203628
        //
        // D = the non-rolling duration
        // T = the time between reset triggers
        // P = the chance a trigger will actually reset the duration
        // TC = the time between corruption ticks
        float d = recastPeriod;
        float t = guessRollingTriggerFrequency();
        float p = mommy.hitChance * mommy.talents.getEverlastingAffliction() * .2f;
        float tc = d / 6f;
        if (p == 1 && t <= d) {
            recastPeriod = mommy.options.getDuration();
        } else if (p > 0 && t > 0) {
            float fightLen = mommy.options.getDuration();
            int maxTriggers = (int) (fightLen * t);
            int maxTicks = (int) (fightLen / tc);

            recastPeriod = tc
                    * maleficusDuration(
                            new float[maxTicks + 1][maxTriggers + 1],
                            tc,
                            p,
                            1f / t,
                            (int) (fightLen / tc),
                            6,
                            0);
        }
        numTicks = recastPeriod / tc;
        // endregion
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        spellModifiers.addMultiplicativeMultiplier(mommy.stats.getWarlock4T9());
    }

    @Override
    public void setDamageStats(float baseSpellPower) {
        // stack crit trinkets at the front of a rolling corruption
        if (numCasts < 2) {
            spellModifiers.addCritChance(mommy.extraCritAtMax);
        }

        super.setDamageStats(baseSpellPower);
    }

    private static float maleficusDuration(
            float[][] cache,
            float tc,
            float p,
            float t,
            int maxTicks,
            int accumTicks,
            int triggerIndex) {

        if (accumTicks >= maxTicks) {
            return maxTicks;
        }
        double now = triggerIndex * t;
        double curLen = accumTicks * tc;
        if (now > curLen) {
            return accumTicks;
        }

        if (cache[accumTicks][triggerIndex] > 0) {
            return cache[accumTicks][triggerIndex];
        }

        float procDuration = maleficusDuration(
                cache,
                tc,
                p,
                t,
                maxTicks,
                (int) (now / tc) + 7,
                triggerIndex + 1);
        float nonProcDuration = maleficusDuration(
                cache,
                tc,
                p,
                t,
                maxTicks,
                accumTicks,
                triggerIndex + 1);
        float res = procDuration * p + nonProcDuration * (1 - p);

        cache[accumTicks][triggerIndex] = res;
        return res;
    }

    private float guessRollingTriggerFrequency() {
        float freq = 0f;
        if (getRotation().contains("Shadow Bolt")) {
            // assume about 1/2 the time will be spent spamming shadow bolt
            freq += .5f
                    / getTimeUsed(
                            ShadowBolt.getBaseCastTime(mommy),
                            0f,
                            mommy.haste,
                            mommy.options.getLatency());
        }
        if (getRotation().contains("Haunt") && mommy.talents.getHaunt() > 0) {
            // assume 11 seconds between haunt casts, on average
            freq += 1f / 11f;
        }
        return freq;
    }
}

class CurseOfAgony extends Spell {

    public CurseOfAgony(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Affliction,
                .1f,
                0f,
                0f,
                mommy.talents.isGlyphCoA() ? 28f : 24f,
                // Glyph of Curse of Agony raises the *average* tick to
                // 8/7 its unglyphed value.
                1740f / 12f * (mommy.talents.isGlyphCoA() ? 8f / 7f : 1f),
                mommy.talents.isGlyphCoA() ? 14f : 12f,
                1.2f / 12f,
                // Notice that spellstone is NOT applied to Curse of Agony.
                // Why?  Ask Blizzard, I have no idea.
                mommy.talents.getImprovedCurseOfAgony() * .05f
                        + mommy.talents.getContagion() * .01f,
                false,
                0f,
                0f);

        gcdBonus = mommy.talents.getAmplifyCurse() * .5f;
    }
}

class CurseOfDoom extends Spell {

    public CurseOfDoom(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Affliction,
                .15f,
                0f,
                60f,
                0f,
                7300f,
                1f,
                2f,
                0f,
                false,
                0f,
                0f);

        gcdBonus = mommy.talents.getAmplifyCurse() * .5f;
    }
}

class CurseOfTheElements extends Spell {

    public CurseOfTheElements(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                null,
                SpellTree.Affliction,
                .1f,
                0f,
                0f,
                300f,
                true);

        gcdBonus = mommy.talents.getAmplifyCurse() * .5f;
    }

    @Override
    public boolean isCastable() {
        return mommy.stats.getBonusShadowDamageMultiplier() == 0;
    }
}

class Haunt extends Spell {

    private float avgBonus;

    public Haunt(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Affliction,
                .12f,
                1.5f,
                8f,
                0f,
                645f,
                753f,
                .4266f,
                0f,
                0f,
                mommy.talents.getPandemic());
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getHaunt() > 0;
    }

    public float getAvgTickBonus() {
        if (avgBonus > 0) {
            return avgBonus;
        }

        // Consider the window from cast time until the next (actual, not
        // planned) cast time.  This can be broken into three cases, when
        // it hits, when it misses & the previous cast hit, and when it
        // misses & the previous cast missed. Calculate the average uprate
        // in each case, then combine them with a weighted average to get
        // the overall uprate.

        float uprate;
        float tolerance = 12f - recastPeriod;
        SimulatedStat delayStats = simulatedStats.get("delay");
        SimulatedStat hitUprate = new SimulatedStat();
        SimulatedStat missFollowingHitUprate = new SimulatedStat();
        for (int i = delayStats.values.size(); --i >= 0; ) {
            float collisionDelay = delayStats.values.get(i);
            float probability = delayStats.weights.get(i);

            // CASE 1: this cast hits.
            // uprate = duration / window
            // duration = 12
            // window = RecastPeriod + collisionDelay
            uprate = Math.min(1f, 12f / (recastPeriod + collisionDelay));
            hitUprate.addSample(uprate, probability);

            // CASE 2: this cast misses, previous cast hit.
            // uprate = leftoverUptime / window
            // leftoverUptime = tolerance - collisionDelay
            // window = Cooldown + collisionDelay
            uprate = Math.max(tolerance - collisionDelay, 0)
                    / (cooldown + delayStats.getValue());
            missFollowingHitUprate.addSample(uprate, probability);

            // CASE 3: this cast misses, previous cast missed.
            // This case will always yeild zero uptime/uprate.
        }

        // average them all together for the overall uprate
        float hitChance = mommy.hitChance;
        float missChance = 1 - hitChance;
        uprate = Utilities.getWeightedSum(
                hitUprate.getValue(),
                hitChance,
                missFollowingHitUprate.getValue(),
                missChance * hitChance,
                0f,
                missChance * missChance);

        avgBonus = (mommy.talents.isGlyphHaunt() ? .23f : .2f) * uprate;
        return avgBonus;
    }
}

class Immolate extends Spell {

    public static float getRecastPeriod(CharacterCalculationsWarlock mommy) {
        return 15f + mommy.talents.getMoltenCore() * 3f;
    }

    public static boolean isClippedByConflagrate(CharacterCalculationsWarlock mommy) {
        return Conflagrate.willBeCast(mommy) && !mommy.talents.isGlyphConflag();
    }

    public Immolate(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Destruction,
                .17f,
                2f - mommy.talents.getBane() * .1f,
                0f,
                getRecastPeriod(mommy),
                true,
                460f,
                460f,
                .2f,
                mommy.talents.getImprovedImmolate() * .1f,
                785f / 5f,
                5f + mommy.talents.getMoltenCore(),
                .2f,
                (mommy.talents.isGlyphImmolate() ? .1f : 0f)
                        + mommy.talents.getImprovedImmolate() * .1f
                        + mommy.talents.getAftermath() * .03f,
                true,
                0f,
                0f);

        recordMissesSeparately = true;
    }

    @Override
    public float getQueueProbability(CastingState state) {
        if (isClippedByConflagrate(mommy) && !state.cooldowns.containsKey(this)) {
            return 0f;
        } else {
            return super.getQueueProbability(state);
        }
    }

    @Override
    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        List<CastingState> states = super.simulateCast(stateBeforeCast, chanceOfCast);
        if (isClippedByConflagrate(mommy)) {
            recordSimulatedStat(
                    "downtime",
                    -stateBeforeCast.cooldowns.get(this),
                    stateBeforeCast.probability);
        }
        return states;
    }

    @Override
    public float getNumCasts() {
        if (isClippedByConflagrate(mommy)) {
            return mommy.castSpells.get("Conflagrate").getNumCasts();
        } else {
            return super.getNumCasts();
        }
    }

    @Override
    public void adjustAfterCastingIsSet() {
        if (isClippedByConflagrate(mommy)) {
            float downtime = simulatedStats.get("downtime").getValue();
            float uptime = getAvgTimeBetweenCasts() - downtime;
            numTicks = uptime / 3f;
        }
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        spellModifiers.addMultiplicativeMultiplier(mommy.stats.getWarlock4T9());
        if (mommy.stats.getWarlock2T8() > 0) {
            spellModifiers.addAdditiveMultiplier(.1f);
        }
    }

    /**
     * Records the chance that a given spell is cast while immolate is on
     * the target.
     */
    public void recordUpChance(Spell spell, CastingState state) {
        float chance;
        if (!state.cooldowns.containsKey(this) && isClippedByConflagrate(mommy)) {
            // this does not actually garuntee immolate is up.  For example,
            // if the priority list starts: CoD > Corr > Chaos > Immo > Conf
            // then it is possible for immolate to fall off while casting
            // CoD + Corr so that the upchance on Chaos < 1.  But even then
            // it's a rare case, and that is not a priority list anyone is
            // actually going to use, so we'll just stick with this
            // approximation.
            chance = 1f;
        } else {
            chance = getUprate(state, spell);
        }
        spell.recordSimulatedStat("immolate up-chance", chance, state.probability);
    }
}

class ImmolationAura extends Spell {

    public ImmolationAura(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Demonology,
                .64f,
                0f,
                180f * (1f - mommy.talents.getNemesis() * .1f),
                0f,
                251f,
                15f,
                2.14f / 15f,
                0f,
                false,
                0f,
                0f);
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getMetamorphosis() > 0;
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();

        // immolation aura always gets the full metamorphosis bonus
        spellModifiers.addMultiplicativeDirectMultiplier(
                1.2f / (1 + mommy.getMetamorphosisBonus()) - 1);
    }
}

class Incinerate extends Spell {

    public Incinerate(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Destruction,
                .14f,
                2.5f - mommy.talents.getEmberstorm() * .05f,
                0f,
                0f,
                582f,
                676f,
                (1 + mommy.talents.getShadowAndFlame() * .04f) * .7143f,
                mommy.talents.isGlyphIncinerate() ? .05f : 0f,
                mommy.stats.getWarlock2T10() + mommy.stats.getWarlock4T8(),
                0f);
        applyImprovedSoulLeech();
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        float immoUp = getImmolateUpRate();
        baseDamage *= 1f + (.25f * immoUp);
        if (mommy.talents.getFireAndBrimstone() > 0) {
            float fullBonus = mommy.talents.getFireAndBrimstone() * .02f;
            spellModifiers.addMultiplicativeMultiplier(immoUp * fullBonus);
        }
    }

    protected float getImmolateUpRate() {
        if (Immolate.isClippedByConflagrate(mommy)) {
            return 1f;
        } else {
            return 1 - getCastTime(null) / Immolate.getRecastPeriod(mommy);
        }
    }
}

class Incinerate_UnderBackdraft extends Incinerate {

    public Incinerate_UnderBackdraft(CharacterCalculationsWarlock mommy) {
        super(mommy);
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getBackdraft() > 0 && Conflagrate.willBeCast(mommy);
    }

    @Override
    public float getQueueProbability(CastingState state) {
        if (state.backdraftCharges > 0) {
            return 1f;
        } else {
            return 0f;
        }
    }

    @Override
    public float getNumCasts() {
        if (numCasts == 0) {
            Spell conflagrate = mommy.castSpells.get("Conflagrate");
            float castsPerConf = getTotalWeight() / conflagrate.getTotalWeight();
            numCasts = castsPerConf * conflagrate.getNumCasts();
        }
        return numCasts;
    }

    @Override
    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        if (mommy.talents.getFireAndBrimstone() > 0) {
            ((Immolate) mommy.getSpell("Immolate")).recordUpChance(this, stateBeforeCast);
        }
        return super.simulateCast(stateBeforeCast, chanceOfCast);
    }

    @Override
    protected float getImmolateUpRate() {
        if (!simulatedStats.containsKey("immolate up-chance")) return 0f;
        return simulatedStats.get("immolate up-chance").getValue();
    }
}

class Incinerate_UnderMoltenCore extends Incinerate {

    public Incinerate_UnderMoltenCore(CharacterCalculationsWarlock mommy) {
        super(mommy);

        baseCastTime /= 1f + mommy.talents.getMoltenCore() * .1f;

        float procChance = mommy.talents.getMoltenCore() * .04f;
        float tickPeriod = Corruption.getTickPeriod(mommy);
        cooldown = tickPeriod / procChance;
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getMoltenCore() > 0
                && getRotation().contains("Corruption");
    }

    @Override
    public boolean isCastDuringExecute() {
        return true;
    }

    @Override
    public float getQueueProbability(CastingState state) {
        if (state.moltenCoreCharges > 0) {
            return 1f;
        } else {
            return super.getQueueProbability(state);
        }
    }

    @Override
    public List<CastingState> simulateCast(CastingState stateBeforeCast, float chanceOfCast) {
        List<CastingState> states = super.simulateCast(stateBeforeCast, chanceOfCast);
        for (CastingState state : states) {
            state.cooldowns.remove(this);
            if (state.moltenCoreCharges > 0) {
                --state.moltenCoreCharges;
            } else {
                state.moltenCoreCharges = 2;
            }
        }
        return states;
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        spellModifiers.addMultiplicativeMultiplier(mommy.talents.getMoltenCore() * .06f);
    }
}

class LifeTap extends Spell {

    // This exists as a spell object only for the case when we would have
    // enough mana to never life tap, but still need to keep up the gyph. So
    // make it's cooldown 37s (40s duration of the glyph minus just about
    // enough time to make cooldown collisions & such don't let it drop
    // off).

    public LifeTap(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                null,
                SpellTree.Affliction,
                0f,
                0f,
                0f,
                37f,
                false);

        manaCost = (-2000f - mommy.calcSpellPower() * .5f)
                * (1f + mommy.talents.getImprovedLifeTap() * .1f);
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.isGlyphLifeTap();
    }

    public float addCastsForRegen(float timeRemaining, float manaRemaining, Spell spammedSpell) {
        // getting our cast time is not safe until the backdraft multilpier
        // has been set.  fortunetly that's easy to "calculate"
        //BackdraftMultiplier = 1f;

        // The number of needed lifetaps is obtained by solving this
        // system of equations:
        //
        // spamCasts * spamMana + ltCasts * ltMana = manaRemaining
        // spamCasts * spamCast + ltCasts * ltCast = timeRemaining

        float a = spammedSpell.manaCost;
        float b = manaCost;
        float c = manaRemaining;
        float d = spammedSpell.getAvgTimeUsed();
        float e = getAvgTimeUsed();
        float f = timeRemaining;
        float toAdd = Math.max(0f, (c * d - a * f) / (b * d - a * e));

        numCasts += toAdd;

        if (toAdd > 0 && !mommy.castSpells.containsKey("Life Tap")) {
            mommy.castSpells.put("Life Tap", this);
        }

        return toAdd;
    }

    public float getAvgBonusSpellPower() {
        if (!mommy.talents.isGlyphLifeTap()) {
            return 0f;
        }

        float uprate = Math.min(
                1f, (numCasts + .8f) * 40f / mommy.options.getDuration());
        return uprate * mommy.calcSpirit() * .2f;
    }
}

class ShadowBolt extends Spell {

    public static float getBaseCastTime(CharacterCalculationsWarlock mommy) {
        return 3f - mommy.talents.getBane() * .1f;
    }

    public ShadowBolt(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Destruction,
                .17f,
                getBaseCastTime(mommy),
                0f,
                0f,
                694f,
                775f,
                (1 + mommy.talents.getShadowAndFlame() * .04f) * .8571f,
                mommy.talents.getImprovedShadowBolt() * .01f,
                mommy.stats.getWarlock2T10() + mommy.stats.getWarlock4T8(),
                0f);
        applyImprovedSoulLeech();
    }
}

class ShadowBolt_Instant extends ShadowBolt {

    public static boolean isCastable(WarlockTalents talents, List<String> priorities) {
        return priorities.contains("Corruption")
                && (talents.isGlyphCorruption() || talents.getNightfall() > 0);
    }

    public ShadowBolt_Instant(CharacterCalculationsWarlock mommy) {
        super(mommy);

        baseCastTime = 0f;

        // Currently modeled as a spell on a cooldown equal to the
        // average time between procs.  This lengthens the time between
        // casts according to the rules for cooldown collision, which is not
        // completely accurate, but close enough.  To be accurate it
        // should instead factor in the probability that it will proc twice
        // (or more) while casting higher priority spells.
        float procChance = mommy.talents.getNightfall() * .02f;
        if (mommy.talents.isGlyphCorruption()) {
            procChance += .04f;
        }
        float tickPeriod = Corruption.getTickPeriod(mommy);
        cooldown = tickPeriod / procChance;
    }

    @Override
    public boolean isCastable() {
        return isCastable(mommy.talents, getRotation().spellPriority);
    }
}

class SoulFire extends Spell {

    public SoulFire(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Fire,
                SpellTree.Destruction,
                .09f,
                (6f - mommy.talents.getBane() * .4f)
                        * (1 - mommy.talents.getDecimation() * .2f),
                0f,
                0f,
                1323f,
                1657f,
                1.15f,
                0f,
                0f,
                0f);
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getDecimation() > 0;
    }
}

class UnstableAffliction extends Spell {

    public UnstableAffliction(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                MagicSchool.Shadow,
                SpellTree.Affliction,
                .15f,
                mommy.talents.isGlyphUA() ? 1.3f : 1.5f,
                0f,
                15f,
                1150f / 5f,
                5f,
                mommy.talents.getEverlastingAffliction() * .01f + 1f / 5f,
                mommy.talents.getSiphonLife() * .05f,
                mommy.talents.getPandemic() > 0,
                mommy.talents.getMalediction() * .03f * mommy.talents.getPandemic(),
                mommy.talents.getPandemic());

        if (mommy.talents.isGlyphUA()) {
            gcdBonus = .2f;
        }
    }

    @Override
    public boolean isCastable() {
        return mommy.talents.getUnstableAffliction() > 0;
    }

    @Override
    public void finalizeSpellModifiers() {
        super.finalizeSpellModifiers();
        spellModifiers.addMultiplicativeMultiplier(mommy.stats.getWarlock4T9());
        if (mommy.stats.getWarlock2T8() > 0) {
            spellModifiers.addAdditiveMultiplier(.2f);
        }
    }
}

