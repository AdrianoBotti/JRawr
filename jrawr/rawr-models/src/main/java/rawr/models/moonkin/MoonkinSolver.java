// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/MoonkinSolver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.talents.DruidTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The interface public class to the rest of Rawr.  Provides a single Solve method that runs all the calculations.
public class MoonkinSolver {
    private static final int NUM_SPELL_DETAILS = 14;
    // A list of all currently active proc effects.
    public List<ProcEffect> procEffects;
    // A list of all the damage spells
    private Spell[] spellData = null;

    private Spell[] spellData() {
        if (this.spellData == null) {
            Spell sf = new Spell();
            sf.name = "SF";
            sf.baseDamage = (1038.0f + 1222.0f) / 2.0f;
            sf.spellDamageModifier = 1.0f;
            sf.baseCastTime = 3.5f;
            sf.castTime = 3.5f;
            sf.baseManaCost = (float) (int) (CalculationsMoonkin.BaseMana * 0.16f);
            sf.dotEffect = null;
            sf.school = SpellSchool.Arcane;

            Spell mf = new Spell();
            mf.name = "MF";
            mf.baseDamage = (406.0f + 476.0f) / 2.0f;
            mf.spellDamageModifier = (1.5f / 3.5f) * (1.5f / 3.5f) / (1.5f / 3.5f + 12f / 15f);
            mf.baseCastTime = 1.5f;
            mf.castTime = 1.5f;
            mf.baseManaCost = (float) (int) (CalculationsMoonkin.BaseMana * 0.21f);
            mf.dotEffect = new DotEffect();
            mf.dotEffect.duration = 12.0f;
            mf.dotEffect.tickLength = 3.0f;
            mf.dotEffect.tickDamage = 200.0f;
            mf.dotEffect.spellDamageModifierPerTick = (12f / 15f) * (12f / 15f) / (1.5f / 3.5f + 12f / 15f) / (12.0f / 3.0f);
            mf.school = SpellSchool.Arcane;

            Spell w = new Spell();
            w.name = "W";
            w.baseDamage = (558.0f + 628.0f) / 2.0f;
            w.spellDamageModifier = 2.0f / 3.5f;
            w.baseCastTime = 2.0f;
            w.castTime = 1.5f;
            w.baseManaCost = (float) (int) (CalculationsMoonkin.BaseMana * 0.11f);
            w.dotEffect = null;
            w.school = SpellSchool.Nature;

            Spell is = new Spell();
            is.name = "IS";
            is.baseDamage = 0.0f;
            is.spellDamageModifier = 0.0f;
            is.baseCastTime = 1.5f;
            is.castTime = 1.5f;
            is.baseManaCost = (float) (int) (CalculationsMoonkin.BaseMana * 0.08f);
            is.dotEffect = new DotEffect();
            is.dotEffect.duration = 12.0f;
            is.dotEffect.tickLength = 2.0f;
            is.dotEffect.tickDamage = 1290.0f / 6.0f;
            is.dotEffect.spellDamageModifierPerTick = 0.2f;
            is.school = SpellSchool.Nature;

            this.spellData = new Spell[] { sf, mf, w, is };
        }
        return this.spellData;
    }

    public Spell getStarfire() {
        return spellData()[0];
    }

    public Spell getMoonfire() {
        return spellData()[1];
    }

    public Spell getWrath() {
        return spellData()[2];
    }

    public Spell getInsectSwarm() {
        return spellData()[3];
    }

    public void resetSpellList() {
        // Since the property rebuilding the array is based on this variable being null, this effectively forces a refresh
        this.spellData = null;
    }

    public Spell findSpell(String name) {
        switch (name) {
            case "SF":
                return getStarfire();
            case "MF":
                return getMoonfire();
            case "IS":
                return getInsectSwarm();
            case "W":
                return getWrath();
            default:
                return null;
        }
    }

    public float naturesGrace = 0.0f;

    // The spell rotations themselves.
    public List<SpellRotation> rotations = null;

    // Results data from the calculations, which will be sent to the UI.
    Map<String, RotationData> cachedResults = new LinkedHashMap<String, RotationData>();

    public float getSpellHit(CharacterCalculationsMoonkin calcs) {
        float baseHit = 1.0f;
        switch (calcs.targetLevel) {
            case 80:
                baseHit -= 0.04f;
                break;
            case 81:
                baseHit -= 0.05f;
                break;
            case 82:
                baseHit -= 0.06f;
                break;
            case 83:
                baseHit -= 0.17f;
                break;
            default:
                baseHit -= 0.17f;
                break;
        }
        baseHit = Math.min(1.0f, baseHit + calcs.spellHit);
        return baseHit;
    }

    public void solve(Character character, CharacterCalculationsMoonkin calcs) {
        CalculationOptionsMoonkin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsMoonkin
                ? (CalculationOptionsMoonkin) character.getCalculationOptions()
                : new CalculationOptionsMoonkin();
        DruidTalents talents = character.getDruidTalents();
        procEffects = new ArrayList<ProcEffect>();
        recreateSpells(talents, calcs);
        cachedResults = new LinkedHashMap<String, RotationData>();

        float trinketDPS = 0.0f;
        float baseSpellPower = calcs.spellPower;
        float baseHit = getSpellHit(calcs);
        float baseCrit = calcs.spellCrit;
        float baseHaste = calcs.spellHaste;

        buildProcList(calcs);

        float maxDamageDone = 0.0f;
        float maxBurstDamageDone = 0.0f;
        SpellRotation maxBurstRotation = rotations.get(0);
        SpellRotation maxRotation = rotations.get(0);

        float manaPool = getEffectiveManaPool(character, calcOpts, calcs);

        // Do tree calculations: Calculate damage per cast.
        float treeDamage = (talents.getForceOfNature() == 1) ? doTreeCalcs(baseSpellPower, calcs.basicStats.getPhysicalHaste(), calcs.basicStats.getArmorPenetration(), calcs.basicStats.getPhysicalCrit(), calcOpts.getTreantLifespan(), character.getDruidTalents().getBrambles()) : 0.0f;
        // Extend that to number of casts per fight.
        float treeCasts = (float) Math.floor(calcs.fightLength / 3) + 1.0f;
        // Partial cast: If the fight lasts 3.x minutes and x is less than 0.5 (30 sec tree duration), calculate a partial cast
        if ((int) calcs.fightLength % 3 == 0 && calcs.fightLength - (int) calcs.fightLength < 0.5)
            treeCasts += (calcs.fightLength - (int) calcs.fightLength) / 0.5f - 1.0f;
        treeDamage *= treeCasts;
        // Multiply by raid-wide damage increases.
        treeDamage *= (1 + calcs.basicStats.getBonusDamageMultiplier()) * (1 + calcs.basicStats.getBonusPhysicalDamageMultiplier());
        // Calculate the DPS averaged over the fight length.
        float treeDPS = treeDamage / (calcs.fightLength * 60.0f);
        // Calculate mana usage for trees.
        float treeManaUsage = (float) Math.ceil(treeCasts) * CalculationsMoonkin.BaseMana * 0.12f;
        manaPool -= talents.getForceOfNature() == 1 ? treeManaUsage : 0.0f;

        // Do Starfall calculations.
        boolean starfallGlyph = talents.isGlyphOfStarfall();
        Buff tier102PieceBuff = null;
        for (Buff theBuff : character.getActiveBuffs()) {
            if (theBuff.name.equals("Lasherweave Regalia (T10) 2 Piece Bonus")) {
                tier102PieceBuff = theBuff;
                break;
            }
        }
        float[] numberOfStarHitsHolder = new float[1];
        float starfallDamage = (talents.getStarfall() == 1) ? doStarfallCalcs(baseSpellPower, baseHit, baseCrit,
                (1 + calcs.basicStats.getBonusDamageMultiplier()) *
                        (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) *
                        (1 + calcs.basicStats.getBonusArcaneDamageMultiplier()) *
                        (1 + (talents.isGlyphOfFocus() ? 0.1f : 0.0f)), getWrath().criticalDamageModifier, numberOfStarHitsHolder) : 0.0f;
        float numberOfStarHits = numberOfStarHitsHolder[0];
        float starfallCD = 1.5f - (starfallGlyph ? 0.5f : 0.0f);
        float numStarfallCasts = (float) Math.floor(calcs.fightLength / starfallCD) + 1.0f;
        // Partial cast: If the difference between fight length and total starfall CD time is less than 10 seconds (duration),
        // calculate a partial cast
        float starfallDiff = calcs.fightLength * 60.0f - (numStarfallCasts - 1) * starfallCD * 60.0f;
        if (starfallDiff > 0 && starfallDiff < 10)
            numStarfallCasts += starfallDiff / 60.0f / (1.0f / 6.0f) - 1.0f;
        starfallDamage *= numStarfallCasts;
        float starfallManaUsage = (float) Math.ceil(numStarfallCasts) * CalculationsMoonkin.BaseMana * 0.39f;
        manaPool -= talents.getStarfall() == 1 ? starfallManaUsage : 0.0f;

        // Simple faerie fire mana calc
        float faerieFireCasts = (float) Math.floor(calcs.fightLength / 5) + (calcs.fightLength % 5 != 0 ? 1.0f : 0.0f);
        float faerieFireMana = faerieFireCasts * CalculationsMoonkin.BaseMana * 0.08f;
        if (talents.getImprovedFaerieFire() > 0)
            manaPool -= faerieFireMana;

        // Calculate effect of casting Starfall/Treants/ImpFF (regular FF is assumed to be provided by a feral)
        float globalCooldown = 1.5f / (1 + baseHaste) + calcs.latency;
        float treantTime = (talents.getForceOfNature() == 1) ? globalCooldown * (float) Math.ceil(treeCasts) : 0.0f;
        float starfallTime = (talents.getStarfall() == 1) ? globalCooldown * (float) Math.ceil(numStarfallCasts) : 0.0f;
        float faerieFireTime = (talents.getImprovedFaerieFire() > 0) ? globalCooldown * faerieFireCasts : 0.0f;

        float totalTimeInRotation = calcs.fightLength * 60.0f - (treantTime + starfallTime + faerieFireTime);
        float percentTimeInRotation = totalTimeInRotation / (calcs.fightLength * 60.0f);

        float manaGained = manaPool - calcs.basicStats.getMana();

        float oldArcaneMultiplier = calcs.basicStats.getBonusArcaneDamageMultiplier();
        float oldNatureMultiplier = calcs.basicStats.getBonusNatureDamageMultiplier();

        for (SpellRotation rot : rotations) {
            rot.solver = this;
            float accumulatedDamage = 0.0f;
            float totalUpTime = 0.0f;
            float[] spellDetails = new float[NUM_SPELL_DETAILS];
            List<ProcEffect> activatedEffects = new ArrayList<ProcEffect>();
            List<ProcEffect> alwaysUpEffects = new ArrayList<ProcEffect>();

            // Pre-calculate rotational variables with base stats
            rot.damageDone(talents, calcs, baseSpellPower, baseHit, baseCrit, baseHaste);

            // Reset variables modified in the pre-loop to base values
            float currentSpellPower = baseSpellPower;
            float currentCrit = baseCrit;
            float currentHaste = baseHaste;
            calcs.basicStats.setBonusArcaneDamageMultiplier(oldArcaneMultiplier);
            calcs.basicStats.setBonusNatureDamageMultiplier(oldNatureMultiplier);
            // Calculate spell power/spell damage modifying trinkets in a separate pre-loop
            for (ProcEffect proc : procEffects) {
                if (proc.effect.getStats().getSpellPower() > 0 || proc.effect.getStats().getSpirit() > 0) {
                    float procSpellPower = proc.effect.getStats().getSpellPower();
                    if (proc.effect.getStats().getSpirit() > 0)
                        procSpellPower += proc.effect.getStats().getSpirit() * (0.1f * talents.getImprovedMoonkinForm());

                    float triggerInterval = 0.0f;
                    float triggerChance = 1.0f;
                    switch (proc.effect.getTrigger()) {
                        case DamageDone:
                        case DamageOrHealingDone:
                            triggerInterval = ((rot.duration / rot.castCount) + (rot.duration / (rot.moonfireTicks + rot.insectSwarmTicks))) / 2.0f;
                            break;
                        case Use:
                            break;
                        case SpellHit:
                        case DamageSpellHit:
                            triggerInterval = rot.duration / rot.castCount;
                            triggerChance = getSpellHit(calcs);
                            break;
                        case SpellCrit:
                        case DamageSpellCrit:
                            triggerInterval = rot.duration / (rot.castCount - rot.insectSwarmCasts);
                            triggerChance = baseCrit;
                            break;
                        case SpellCast:
                        case DamageSpellCast:
                            triggerInterval = rot.duration / rot.castCount;
                            break;
                        case MoonfireCast:
                            triggerInterval = rot.duration / rot.moonfireCasts;
                            break;
                        case DoTTick:
                        case InsectSwarmOrMoonfireTick:
                            triggerInterval = rot.duration / (rot.insectSwarmTicks + rot.moonfireTicks);
                            break;
                        case MoonfireTick:
                            triggerInterval = rot.duration / rot.moonfireTicks;
                            break;
                        case InsectSwarmTick:
                            triggerInterval = rot.duration / rot.insectSwarmTicks;
                            break;
                        default:
                            triggerChance = 0.0f;
                            break;
                    }
                    if (triggerChance > 0)
                        currentSpellPower += (proc.effect.getMaxStack() > 1 ? proc.effect.getAverageStackSize(triggerInterval, triggerChance, 3.0f, calcs.fightLength * 60.0f) : 1f) *
                                proc.effect.getAverageUptime(triggerInterval, triggerChance) * procSpellPower;
                }
                // 2T10 (both if statements, which is why it isn't else-if)
                if (proc.effect.getStats().getBonusArcaneDamageMultiplier() > 0) {
                    calcs.basicStats.setBonusArcaneDamageMultiplier(calcs.basicStats.getBonusArcaneDamageMultiplier() + proc.effect.getAverageUptime(rot.duration / rot.castCount, 1f) * proc.effect.getStats().getBonusArcaneDamageMultiplier());
                }
                if (proc.effect.getStats().getBonusNatureDamageMultiplier() > 0) {
                    calcs.basicStats.setBonusNatureDamageMultiplier(calcs.basicStats.getBonusNatureDamageMultiplier() + proc.effect.getAverageUptime(rot.duration / rot.castCount, 1f) * proc.effect.getStats().getBonusNatureDamageMultiplier());
                }
                if (proc.effect.getStats().getRawSpecialEffectDataSize() > 0) {
                    SpecialEffect childEffect = proc.effect.getStats().getRawSpecialEffectData()[0];
                    // Nevermelting Ice Crystal
                    if (childEffect.getStats().getCritRating() != 0) {
                        float maxStack = proc.effect.getStats().getCritRating();
                        float numNegativeStacks = childEffect.getAverageStackSize(rot.duration / (rot.castCount - rot.insectSwarmCasts), baseCrit, 3.0f, proc.effect.getDuration());
                        float averageNegativeValue = childEffect.getStats().getCritRating() * numNegativeStacks;
                        float averageCritRating = maxStack + averageNegativeValue;
                        currentCrit += StatConversion.getSpellCritFromRating(averageCritRating * proc.effect.getAverageUptime(0f, 1f));
                    }
                    // Fetish of Volatile Power
                    else if (childEffect.getStats().getHasteRating() != 0) {
                        currentHaste += StatConversion.getSpellHasteFromRating(childEffect.getStats().getHasteRating() * childEffect.getAverageStackSize(rot.duration / rot.castCount, 1f, 3.0f, proc.effect.getDuration()) * proc.effect.getAverageUptime(0f, 1f));
                    }
                }
            }
            // Calculate damage and mana contributions for non-stat-boosting trinkets
            // Separate stat-boosting proc trinkets into their own list
            ProcEffect.StatRefs refs = new ProcEffect.StatRefs();
            for (ProcEffect proc : procEffects) {
                refs.spellPower = currentSpellPower;
                refs.spellHit = baseHit;
                refs.spellCrit = currentCrit;
                refs.spellHaste = currentHaste;
                if (proc.calculateDPS != null) {
                    accumulatedDamage += proc.calculateDPS.apply(rot, calcs, currentSpellPower, baseHit, currentCrit, currentHaste) * rot.duration;
                } else if (proc.activate != null) {
                    float upTime = proc.upTime.apply(rot, calcs);
                    // Procs with 100% uptime should be activated and not put into the combination loop
                    if (upTime == 1) {
                        alwaysUpEffects.add(proc);
                        proc.activate.apply(character, calcs, refs);
                        currentSpellPower = refs.spellPower;
                        currentCrit = refs.spellCrit;
                        currentHaste = refs.spellHaste;
                    }
                    // Procs with uptime 0 < x < 100 should be activated
                    else if (upTime > 0)
                        activatedEffects.add(proc);
                } else if (proc.calculateMP5 != null) {
                    manaGained += proc.calculateMP5.apply(rot, calcs, currentSpellPower, baseHit, currentCrit, currentHaste) / 5.0f * calcs.fightLength * 60.0f;
                }
            }
            // Calculate stat-boosting trinkets, taking into effect interactions with other stat-boosting procs
            int sign = 1;
            Map<Integer, Float> cachedDamages = new LinkedHashMap<Integer, Float>();
            Map<Integer, Float> cachedUptimes = new LinkedHashMap<Integer, Float>();
            Map<Integer, float[]> cachedDetails = new LinkedHashMap<Integer, float[]>();
            // Iterate over the entire set of trinket combinations (each trinket by itself, 2 at a time, ...)
            for (int i = 1; i <= activatedEffects.size(); ++i) {
                // Create a new combination generator for this "level" of trinket interaction
                CombinationGenerator gen = new CombinationGenerator(activatedEffects.size(), i);
                // Iterate over all combinations
                while (gen.hasNext()) {
                    float tempUpTime = 1.0f;
                    int[] vals = gen.getNext();
                    int pairs = 0;
                    int lengthCounter = 0;
                    // Activate the trinkets, calculate the damage and uptime, then deactivate them
                    for (int idx : vals) {
                        pairs |= 1 << idx;
                        ++lengthCounter;
                        refs.spellPower = currentSpellPower;
                        refs.spellHit = baseHit;
                        refs.spellCrit = currentCrit;
                        refs.spellHaste = currentHaste;
                        activatedEffects.get(idx).activate.apply(character, calcs, refs);
                        currentSpellPower = refs.spellPower;
                        currentCrit = refs.spellCrit;
                        currentHaste = refs.spellHaste;
                    }
                    float tempDPS = rot.damageDone(talents, calcs, currentSpellPower, baseHit, currentCrit, currentHaste) / rot.duration;
                    spellDetails[0] = getStarfire().damagePerHit;
                    spellDetails[1] = getWrath().damagePerHit;
                    spellDetails[2] = getMoonfire().damagePerHit + getMoonfire().dotEffect.damagePerHit;
                    spellDetails[3] = getInsectSwarm().dotEffect.damagePerHit;
                    spellDetails[4] = getStarfire().castTime;
                    spellDetails[5] = getWrath().castTime;
                    spellDetails[6] = getMoonfire().castTime;
                    spellDetails[7] = getInsectSwarm().castTime;
                    spellDetails[8] = getStarfire().ngCastTime;
                    spellDetails[9] = getWrath().ngCastTime;
                    spellDetails[10] = getStarfire().manaCost;
                    spellDetails[11] = getWrath().manaCost;
                    spellDetails[12] = getMoonfire().manaCost;
                    spellDetails[13] = getInsectSwarm().manaCost;
                    for (int idx : vals) {
                        tempUpTime *= activatedEffects.get(idx).upTime.apply(rot, calcs);
                        refs.spellPower = currentSpellPower;
                        refs.spellHit = baseHit;
                        refs.spellCrit = currentCrit;
                        refs.spellHaste = currentHaste;
                        activatedEffects.get(idx).deactivate.apply(character, calcs, refs);
                        currentSpellPower = refs.spellPower;
                        currentCrit = refs.spellCrit;
                        currentHaste = refs.spellHaste;
                    }
                    if (tempUpTime == 0) continue;
                    // Adjust previous probability tables by the current factor
                    // At the end of the algorithm, this ensures that the probability table will contain the individual
                    // probabilities of each effect or set of effects.
                    // These adjustments only need to be made for higher levels of the table, and if the current probability is > 0.
                    if (lengthCounter > 1) {
                        List<Integer> keys = new ArrayList<Integer>(cachedUptimes.keySet());
                        for (int subset : keys) {
                            // Truly a subset?
                            if ((pairs & subset) != subset) {
                                continue;
                            }

                            // Calculate the "layer" of the current subset by getting the set bit count.
                            int subsetLength = 0;
                            for (int j = subset; j > 0; ++subsetLength) {
                                j &= --j;
                            }

                            // Set the sign of the operation: Evenly separated layers are added, oddly separated layers are subtracted
                            int newSign = ((lengthCounter - subsetLength) % 2 == 0) ? 1 : -1;

                            // Adjust by current uptime * sign of operation.
                            cachedUptimes.put(subset, cachedUptimes.get(subset) + newSign * tempUpTime);
                        }
                    }
                    // Cache the results to be calculated later
                    cachedUptimes.put(pairs, tempUpTime);
                    cachedDamages.put(pairs, tempDPS);
                    cachedDetails.put(pairs, new float[NUM_SPELL_DETAILS]);
                    System.arraycopy(spellDetails, 0, cachedDetails.get(pairs), 0, NUM_SPELL_DETAILS);
                    totalUpTime += sign * tempUpTime;
                }
                sign = -sign;
            }
            float accumulatedDPS = 0.0f;
            Arrays.fill(spellDetails, 0f);
            // Apply the above-calculated probabilities to the previously stored damage calculations and add to the result.
            for (Map.Entry<Integer, Float> kvp : cachedUptimes.entrySet()) {
                if (kvp.getValue() == 0) continue;
                accumulatedDPS += kvp.getValue() * cachedDamages.get(kvp.getKey());
                for (int i = 0; i < NUM_SPELL_DETAILS; ++i) {
                    spellDetails[i] += kvp.getValue() * cachedDetails.get(kvp.getKey())[i];
                }
            }
            float damageDone = rot.damageDone(talents, calcs, currentSpellPower, baseHit, currentCrit, currentHaste);
            accumulatedDPS += (1 - totalUpTime) * damageDone / rot.duration;
            spellDetails[0] += (1 - totalUpTime) * getStarfire().damagePerHit;
            spellDetails[1] += (1 - totalUpTime) * getWrath().damagePerHit;
            spellDetails[2] += (1 - totalUpTime) * getMoonfire().damagePerHit + getMoonfire().dotEffect.damagePerHit;
            spellDetails[3] += (1 - totalUpTime) * getInsectSwarm().dotEffect.damagePerHit;
            spellDetails[4] += (1 - totalUpTime) * getStarfire().castTime;
            spellDetails[5] += (1 - totalUpTime) * getWrath().castTime;
            spellDetails[6] += (1 - totalUpTime) * getMoonfire().castTime;
            spellDetails[7] += (1 - totalUpTime) * getInsectSwarm().castTime;
            spellDetails[8] += (1 - totalUpTime) * getStarfire().ngCastTime;
            spellDetails[9] += (1 - totalUpTime) * getWrath().ngCastTime;
            spellDetails[10] += (1 - totalUpTime) * getStarfire().manaCost;
            spellDetails[11] += (1 - totalUpTime) * getWrath().manaCost;
            spellDetails[12] += (1 - totalUpTime) * getMoonfire().manaCost;
            spellDetails[13] += (1 - totalUpTime) * getInsectSwarm().manaCost;

            accumulatedDamage += accumulatedDPS * rot.duration;

            float burstDPS = accumulatedDamage / rot.duration * percentTimeInRotation;
            float sustainedDPS = burstDPS;
            float timeToOOM = (manaPool / rot.rotationData.manaUsed) * rot.duration;
            if (timeToOOM <= 0) timeToOOM = calcs.fightLength * 60.0f;   // Happens when ManaUsed is less than 0
            if (timeToOOM < calcs.fightLength * 60.0f) {
                rot.rotationData.timeToOOM = (int) (timeToOOM / 60) * 60 + (int) (timeToOOM % 60);
                sustainedDPS = burstDPS * timeToOOM / (calcs.fightLength * 60.0f);
            }
            float t10StarfallDamage = starfallDamage;
            // Approximate the effect of the 2T10 set bonus
            if (tier102PieceBuff != null && character.getDruidTalents().getOmenOfClarity() == 1) {
                Stats.SpecialEffectEnumerator enumerator = tier102PieceBuff.getTotalStats().specialEffects();
                enumerator.hasNext();
                SpecialEffect effect = enumerator.next();
                float upTime = effect.getAverageUptime(rot.duration / rot.castCount, 1f);
                t10StarfallDamage = upTime * (starfallDamage * (1 + effect.getStats().getBonusArcaneDamageMultiplier())) + (1 - upTime) * starfallDamage;
            }
            float starfallDPS = t10StarfallDamage / (calcs.fightLength * 60.0f);
            burstDPS += trinketDPS + treeDPS + starfallDPS;
            sustainedDPS += trinketDPS + treeDPS + starfallDPS;

            rot.starfallDamage = t10StarfallDamage / numStarfallCasts;
            rot.starfallStars = numberOfStarHits;
            rot.rotationData.burstDPS = burstDPS;
            rot.rotationData.dps = sustainedDPS;
            rot.starfireAvgHit = spellDetails[0];
            rot.wrathAvgHit = spellDetails[1];
            rot.moonfireAvgHit = spellDetails[2];
            rot.insectSwarmAvgHit = spellDetails[3];
            rot.starfireAvgCast = spellDetails[4];
            rot.wrathAvgCast = spellDetails[5];
            rot.moonfireCastTime = spellDetails[6];
            rot.insectSwarmCastTime = spellDetails[7];
            rot.starfireNGCastTime = spellDetails[8];
            rot.wrathNGCastTime = spellDetails[9];
            rot.starfireManaCost = spellDetails[10];
            rot.wrathManaCost = spellDetails[11];
            rot.moonfireManaCost = spellDetails[12];
            rot.insectSwarmManaCost = spellDetails[13];

            // Update the sustained DPS rotation if any one of the following three cases is true:
            // 1) No user rotation is selected and sustained DPS is maximum
            // 2) A user rotation is selected, Eclipse is not present, and the user rotation matches the current rotation
            // 3) A user rotation is selected, Eclipse is present, and the user rotation's dot spells matches this rotation's
            if ((calcOpts.getUserRotation().equals("None") && sustainedDPS > maxDamageDone) ||
                    (character.getDruidTalents().getEclipse() == 0 && calcOpts.getUserRotation().equals(rot.name)) ||
                    (character.getDruidTalents().getEclipse() > 0 && (calcOpts.getUserRotation().equals(rot.name.replace("Filler", "SF")) ||
                            calcOpts.getUserRotation().equals(rot.name.replace("Filler", "W"))))) {
                maxDamageDone = sustainedDPS;
                maxRotation = rot;
            }
            if (burstDPS > maxBurstDamageDone) {
                maxBurstDamageDone = burstDPS;
                maxBurstRotation = rot;
            }
            rot.manaGained += manaGained / (calcs.fightLength * 60.0f) * rot.duration;
            rot.rotationData.manaGained += manaGained / (calcs.fightLength * 60.0f) * rot.duration;
            if (rot.name.contains("Filler")) {
                cachedResults.put(rot.name.replace("Filler", "SF"), rot.rotationData);
                cachedResults.put(rot.name.replace("Filler", "W"), rot.rotationData);
            } else
                cachedResults.put(rot.name, rot.rotationData);

            // Deactivate always-up procs
            for (ProcEffect proc : alwaysUpEffects) {
                refs.spellPower = currentSpellPower;
                refs.spellHit = baseHit;
                refs.spellCrit = currentCrit;
                refs.spellHaste = currentHaste;
                proc.deactivate.apply(character, calcs, refs);
                currentSpellPower = refs.spellPower;
                currentCrit = refs.spellCrit;
                currentHaste = refs.spellHaste;
            }
        }
        // Present the findings to the user.
        calcs.treantDamage = treeDamage / treeCasts;
        calcs.starfallMana = starfallManaUsage / numStarfallCasts;
        calcs.selectedRotation = maxRotation;
        calcs.burstDPSRotation = maxBurstRotation;
        calcs.setSubPoints(new float[] { maxDamageDone, maxBurstDamageDone });
        calcs.setOverallPoints(calcs.getSubPoints()[0] + calcs.getSubPoints()[1]);
        calcs.rotations = cachedResults;
    }

    // Create proc effect calculations for proc-based trinkets.
    private void buildProcList(CharacterCalculationsMoonkin calcs) {
        // Implement a new handler for each special effect in the calculations stats
        for (SpecialEffect effect : calcs.basicStats.specialEffects()) {
            procEffects.add(new ProcEffect(effect));
        }
    }

    // Non-rotation-specific mana calculations
    private float getEffectiveManaPool(Character character, CalculationOptionsMoonkin calcOpts, CharacterCalculationsMoonkin calcs) {
        float fightLength = calcs.fightLength * 60.0f;

        float innervateCooldown = 360 - calcs.basicStats.getInnervateCooldownReduction();

        // Mana/5 calculations
        float totalManaRegen = calcs.manaRegen5SR * fightLength;

        // Mana pot calculations
        float manaRestoredByPots = 0.0f;
        for (Buff b : character.getActiveBuffs()) {
            if (b.getTotalStats().getManaRestore() > 0) {
                manaRestoredByPots = b.getTotalStats().getManaRestore();
                break;
            }
        }

        // Innervate calculations
        float innervateDelay = calcOpts.getInnervateDelay() * 60.0f;
        int numInnervates = (calcOpts.getInnervate() && fightLength - innervateDelay > 0) ? ((int) (fightLength - innervateDelay) / (int) innervateCooldown + 1) : 0;
        float totalInnervateMana = numInnervates * CalculationsMoonkin.BaseMana * (4.5f + (character.getDruidTalents().isGlyphOfInnervate() ? 0.9f : 0.0f));

        // Replenishment calculations
        float replenishmentPerTick = calcs.basicStats.getMana() * calcs.basicStats.getManaRestoreFromMaxManaPerSecond();
        float replenishmentMana = calcOpts.getReplenishmentUptime() * replenishmentPerTick * calcs.fightLength * 60;

        return calcs.basicStats.getMana() + totalInnervateMana + totalManaRegen + manaRestoredByPots + replenishmentMana;
    }

    // Now returns damage per cast to allow adjustments for fight length
    private float doTreeCalcs(float effectiveNatureDamage, float meleeHaste, float armorPen, float meleeCrit, float treantLifespan, int bramblesLevel) {
        // 642 = base AP, 57% spell power scaling
        float attackPower = 642.0f + (float) Math.floor(0.57f * effectiveNatureDamage);
        // 398.8 = base DPS, 1.7 = best observed swing speed
        float damagePerHit = (398.8f + attackPower / 14.0f) * 1.7f;
        float critRate = 0.05f + meleeCrit;
        float glancingRate = 0.2f;
        float bossArmor = StatConversion.NPC_ARMOR[83 - 80] * (1.0f - armorPen);
        float damageReduction = bossArmor / (bossArmor + 15232.5f);
        damagePerHit *= 1.0f - damageReduction;
        damagePerHit = (critRate * damagePerHit * 2.0f) + (glancingRate * damagePerHit * 0.75f) + ((1 - critRate - glancingRate) * damagePerHit);
        float attackSpeed = 1.7f / (1 + meleeHaste);
        float damagePerTree = (treantLifespan * 30.0f / attackSpeed) * damagePerHit * (1 + 0.05f * bramblesLevel);
        return 3 * damagePerTree;
    }

    // Starfall
    private float doStarfallCalcs(float effectiveArcaneDamage, float spellHit, float spellCrit, float hitDamageModifier, float critDamageModifier, float[] numberOfStarHits) {
        float baseDamagePerStar = (563.0f + 653.0f) / 2.0f;
        float mainStarCoefficient = 0.30f;
        float baseSplashDamage = 101.0f;
        float splashCoefficient = 0.13f;

        // TODO: Right now, calculating single-target only, which is 10 stars with splash damage.
        // AOE situations gives 20 stars.
        // CORRECTION 2010/06/12: single-target damage DOES do splash, if the star hits.

        float damagePerBigStarHit = (baseDamagePerStar + effectiveArcaneDamage * mainStarCoefficient) * hitDamageModifier;
        float damagePerSmallStarHit = (baseSplashDamage + effectiveArcaneDamage * splashCoefficient) * hitDamageModifier;

        float critDamagePerBigStarHit = damagePerBigStarHit * critDamageModifier;
        float critDamagePerSmallStarHit = damagePerSmallStarHit * critDamageModifier;

        float averageDamagePerBigStar = spellCrit * critDamagePerBigStarHit + (1 - spellCrit) * damagePerBigStarHit;
        float averageDamagePerSmallStar = spellCrit * critDamagePerSmallStarHit + (1 - spellCrit) * damagePerSmallStarHit;

        numberOfStarHits[0] = 10.0f;

        float avgNumBigStarsHit = spellHit * numberOfStarHits[0];
        float avgNumSmallStarsHit = avgNumBigStarsHit * spellHit;

        return avgNumBigStarsHit * averageDamagePerBigStar + avgNumSmallStarsHit * averageDamagePerSmallStar;
    }

    // Redo the spell calculations
    private void recreateSpells(DruidTalents talents, CharacterCalculationsMoonkin calcs) {
        resetSpellList();
        if (talents.getEclipse() == 0) {
            rotations = new ArrayList<SpellRotation>(List.of(
                    spellRotation("MF/SF", "MF", "SF"),
                    spellRotation("MF/W", "MF", "W"),
                    spellRotation("IS/SF", "IS", "SF"),
                    spellRotation("IS/W", "IS", "W"),
                    spellRotation("IS/MF/SF", "IS", "MF", "SF"),
                    spellRotation("IS/MF/W", "IS", "MF", "W"),
                    spellRotation("SF Spam", "SF"),
                    spellRotation("W Spam", "W")
            ));
        } else {
            rotations = new ArrayList<SpellRotation>(List.of(
                    spellRotation("W Spam", "W"),
                    spellRotation("SF Spam", "SF"),
                    spellRotation("MF/Filler", "MF", "SF"),
                    spellRotation("IS/Filler", "IS", "W"),
                    spellRotation("IS/MF/Filler", "IS", "MF", "SF")
            ));
        }

        updateSpells(talents, calcs);
    }

    private static SpellRotation spellRotation(String name, String... spells) {
        SpellRotation rot = new SpellRotation();
        rot.name = name;
        rot.spellsUsed = new ArrayList<String>(List.of(spells));
        return rot;
    }

    // Add talented effects to the spells
    private void updateSpells(DruidTalents talents, CharacterCalculationsMoonkin calcs) {
        Stats stats = calcs.basicStats;
        // Add (possibly talented) +spelldmg
        // Starfire: Damage +(0.04 * Wrath of Cenarius)
        // Wrath: Damage +(0.02 * Wrath of Cenarius)
        getWrath().spellDamageModifier += 0.02f * talents.getWrathOfCenarius();
        getStarfire().spellDamageModifier += 0.04f * talents.getWrathOfCenarius();

        // Add spell damage from idols
        //Starfire.IdolExtraSpellPower += stats.StarfireDmg;
        getStarfire().baseDamage += stats.getStarfireDmg();
        //Moonfire.IdolExtraSpellPower += stats.MoonfireDmg;
        getMoonfire().baseDamage += stats.getMoonfireDmg();
        getWrath().baseDamage += stats.getWrathDmg();
        //InsectSwarm.IdolExtraSpellPower += stats.InsectSwarmDmg;
        getInsectSwarm().dotEffect.tickDamage += stats.getInsectSwarmDmg() / getInsectSwarm().dotEffect.getNumberOfTicks();

        float moonfireDDGlyph = talents.isGlyphOfMoonfire() ? -0.9f : 0.0f;
        float moonfireDotGlyph = talents.isGlyphOfMoonfire() ? 0.75f : 0.0f;
        float insectSwarmGlyph = talents.isGlyphOfInsectSwarm() ? 0.3f : 0.0f;
        // Add spell-specific damage
        // Starfire, Moonfire, Wrath: Damage +(0.03 * Moonfury) (Additive with 4T9?)
        // Moonfire: Damage +(0.05 * Imp Moonfire) (Additive with Moonfury/Genesis/Glyph)
        // Moonfire, Insect Swarm: Dot Damage +(0.01 * Genesis) (Additive with Moonfury/Imp. Moonfire/Glyph/Set bonus)
        getWrath().allDamageModifier *= 1 + (float) Math.floor(talents.getMoonfury() * 10 / 3.0f) / 100.0f + stats.getBonusMoonkinNukeDamage();
        getMoonfire().allDamageModifier *= 1 + (float) Math.floor(talents.getMoonfury() * 10 / 3.0f) / 100.0f
                + 0.05f * talents.getImprovedMoonfire()
                + moonfireDDGlyph;
        getMoonfire().dotEffect.allDamageModifier *= 1 + (float) Math.floor(talents.getMoonfury() * 10 / 3.0f) / 100.0f
                + 0.01f * talents.getGenesis()
                + 0.05f * talents.getImprovedMoonfire()
                + moonfireDotGlyph;
        getStarfire().allDamageModifier *= 1 + (float) Math.floor(talents.getMoonfury() * 10 / 3.0f) / 100.0f + stats.getBonusMoonkinNukeDamage();
        getInsectSwarm().dotEffect.allDamageModifier *= 1 + 0.01f * talents.getGenesis()
                + insectSwarmGlyph
                + stats.getBonusInsectSwarmDamage();

        // Moonfire, Insect Swarm: One extra tick (Nature's Splendor)
        getMoonfire().dotEffect.duration += 3.0f * talents.getNaturesSplendor();
        getInsectSwarm().dotEffect.duration += 2.0f * talents.getNaturesSplendor();
        // Moonfire: Crit chance +(0.05 * Imp Moonfire)
        getMoonfire().criticalChanceModifier += 0.05f * talents.getImprovedMoonfire();

        // Add spell-specific crit chance
        // Wrath, Starfire: Crit chance +(0.02 * Nature's Majesty)
        getWrath().criticalChanceModifier += 0.02f * talents.getNaturesMajesty();
        getStarfire().criticalChanceModifier += 0.02f * talents.getNaturesMajesty();

        // Add spell-specific critical strike damage
        // Chaotic Skyfire Diamond
        getStarfire().criticalDamageModifier = stats.getBonusCritMultiplier() > 0 ? 1.5f * (1 + stats.getBonusCritMultiplier()) : 1.5f;
        getWrath().criticalDamageModifier = stats.getBonusCritMultiplier() > 0 ? 1.5f * (1 + stats.getBonusCritMultiplier()) : 1.5f;
        getMoonfire().criticalDamageModifier = stats.getBonusCritMultiplier() > 0 ? 1.5f * (1 + stats.getBonusCritMultiplier()) : 1.5f;
        // Starfire, Moonfire, Wrath: Crit damage +(0.2 * Vengeance)
        getStarfire().criticalDamageModifier = (getStarfire().criticalDamageModifier - 1.0f) * (1 + 0.2f * talents.getVengeance()) + 1.0f;
        getWrath().criticalDamageModifier = (getWrath().criticalDamageModifier - 1.0f) * (1 + 0.2f * talents.getVengeance()) + 1.0f;
        getMoonfire().criticalDamageModifier = (getMoonfire().criticalDamageModifier - 1.0f) * (1 + 0.2f * talents.getVengeance()) + 1.0f;

        // Reduce spell-specific mana costs
        // Starfire, Moonfire, Wrath: Mana cost -(0.03 * Moonglow)
        getStarfire().baseManaCost *= 1.0f - (0.03f * talents.getMoonglow());
        getMoonfire().baseManaCost *= 1.0f - (0.03f * talents.getMoonglow());
        getWrath().baseManaCost *= 1.0f - (0.03f * talents.getMoonglow());

        // Add set bonuses
        // 2T6
        getMoonfire().dotEffect.duration += stats.getMoonfireExtension();
        // 4T6
        getStarfire().criticalChanceModifier += stats.getStarfireCritChance();
        // 4T7
        getStarfire().criticalChanceModifier += stats.getBonusNukeCritChance();
        getWrath().criticalChanceModifier += stats.getBonusNukeCritChance();
        // 2T9
        getMoonfire().dotEffect.canCrit = stats.getMoonfireDotCrit() == 1;

        // Nature's Grace
        naturesGrace = talents.getNaturesGrace();
    }
}
