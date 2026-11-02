// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/SpellRotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

import rawr.base.talents.DruidTalents;

import java.util.List;

// Our old friend the spell rotation.
public class SpellRotation {
    public MoonkinSolver solver;
    public List<String> spellsUsed;
    public RotationData rotationData = new RotationData();
    public String name;
    public float duration;
    public float manaUsed;
    public float manaGained;
    public float castCount;
    public float dotTicks;
    public float wrathCount;
    public float wrathAvgHit;
    public float wrathAvgCast;
    public float wrathNGCastTime;
    public float wrathManaCost;
    public float starfireCount;
    public float starfireAvgHit;
    public float starfireAvgCast;
    public float starfireNGCastTime;
    public float starfireNonEclipseCrit;
    public float starfireEclipseCrit;
    public float starfireManaCost;
    public float insectSwarmTicks;
    public float insectSwarmCasts;
    public float insectSwarmAvgHit;
    public float insectSwarmDuration;
    public float insectSwarmCastTime;
    public float insectSwarmManaCost;
    public float moonfireCasts;
    public float moonfireTicks;
    public float moonfireAvgHit;
    public float moonfireDuration;
    public float moonfireCastTime;
    public float moonfireManaCost;
    public float starfallDamage;
    public float starfallStars;

    // Calculate damage and casting time for a single, direct-damage spell.
    private void doMainNuke(DruidTalents talents, CharacterCalculationsMoonkin calcs, Spell mainNuke, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        float latency = calcs.latency;
        int naturesGrace = talents.getNaturesGrace();
        int starlightWrath = talents.getStarlightWrath();

        float overallDamageModifier = mainNuke.allDamageModifier * (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) * (1 + calcs.basicStats.getBonusDamageMultiplier());
        overallDamageModifier *= mainNuke.school == SpellSchool.Arcane ? (1 + calcs.basicStats.getBonusArcaneDamageMultiplier()) : (1 + calcs.basicStats.getBonusNatureDamageMultiplier());
        overallDamageModifier *= 1 - 0.02f * (calcs.targetLevel - 80);

        float gcd = 1.5f / (1.0f + spellHaste);
        float instantCast = Math.max(gcd, 1.0f) + latency;
        float ngGCD = Math.max(gcd / 1.2f, 1.0f);
        float instantCastNG = ngGCD + latency;

        mainNuke.castTime = mainNuke.baseCastTime - 0.1f * starlightWrath;
        float totalCritChance = spellCrit + mainNuke.criticalChanceModifier;
        float normalCastTime = Math.max(mainNuke.castTime / (1 + spellHaste), instantCast);
        mainNuke.ngCastTime = Math.max(mainNuke.castTime / (1 + spellHaste) / (1 + 0.2f * naturesGrace / 3.0f), instantCastNG);
        float ngProcChance = totalCritChance * naturesGrace / 3.0f;
        float ngUptime = 1.0f - (float) Math.pow(1.0f - ngProcChance, Math.floor(3.0f / mainNuke.ngCastTime) + 1.0f);
        mainNuke.castTime = (1 - ngUptime) * normalCastTime + ngUptime * mainNuke.ngCastTime;
        // Damage calculations
        float damagePerNormalHit = (mainNuke.baseDamage + mainNuke.spellDamageModifier * (spellPower + mainNuke.idolExtraSpellPower)) * overallDamageModifier;
        float damagePerCrit = damagePerNormalHit * mainNuke.criticalDamageModifier * (1 + calcs.basicStats.getMoonkinT10CritDot());
        mainNuke.damagePerHit = (totalCritChance * damagePerCrit + (1 - totalCritChance) * damagePerNormalHit) * spellHit;
    }

    // Modified version of above function specifically for use in calculating Moonkin 4T8 proc.
    public void doSpecialStarfire(CharacterCalculationsMoonkin calcs, Spell mainNuke, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        float latency = calcs.latency;
        float overallDamageModifier = mainNuke.allDamageModifier * (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) * (1 + calcs.basicStats.getBonusDamageMultiplier()) * (1 + calcs.basicStats.getBonusArcaneDamageMultiplier());
        overallDamageModifier *= 1 - 0.02f * (calcs.targetLevel - 80);

        mainNuke.castTime = mainNuke.baseCastTime;
        float totalCritChance = spellCrit + mainNuke.criticalChanceModifier;
        float normalCastTime = Math.max(1.0f, mainNuke.castTime / (1 + spellHaste)) + latency;
        mainNuke.ngCastTime = Math.max(1.0f, mainNuke.castTime / (1 + spellHaste) / (1 + 0.2f * solver.naturesGrace / 3.0f)) + latency;
        float ngProcChance = totalCritChance * solver.naturesGrace / 3.0f;
        float ngUptime = 1.0f - (float) Math.pow(1.0f - ngProcChance, Math.floor(3.0f / mainNuke.ngCastTime) + 1.0f);
        mainNuke.castTime = (1 - ngUptime) * normalCastTime + ngUptime * mainNuke.ngCastTime;
        // Damage calculations
        float damagePerNormalHit = (mainNuke.baseDamage + mainNuke.spellDamageModifier * (spellPower + mainNuke.idolExtraSpellPower)) * overallDamageModifier;
        float damagePerCrit = damagePerNormalHit * mainNuke.criticalDamageModifier * (1 + calcs.basicStats.getMoonkinT10CritDot());
        mainNuke.damagePerHit = (totalCritChance * damagePerCrit + (1 - totalCritChance) * damagePerNormalHit) * spellHit;
    }

    // Calculate damage and casting time for a damage-over-time effect.
    private void doDotSpell(DruidTalents talents, CharacterCalculationsMoonkin calcs, Spell dotSpell, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        if (dotSpell.name.equals("MF"))
            doMoonfire(talents, calcs, dotSpell, spellPower, spellHit, spellCrit, spellHaste);
        else
            doInsectSwarm(talents, calcs, dotSpell, spellPower, spellHit, spellCrit, spellHaste);
    }

    // Calculate damage and casting time for the Moonfire effect.
    private void doMoonfire(DruidTalents talents, CharacterCalculationsMoonkin calcs, Spell dotSpell, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        float latency = calcs.latency;

        float naturesGrace = talents.getNaturesGrace();

        float overallDamageModifier = dotSpell.allDamageModifier * (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) * (1 + calcs.basicStats.getBonusDamageMultiplier()) * (1 + calcs.basicStats.getBonusArcaneDamageMultiplier());
        overallDamageModifier *= 1 - 0.02f * (calcs.targetLevel - 80);

        float dotEffectDamageModifier = dotSpell.dotEffect.allDamageModifier * (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) * (1 + calcs.basicStats.getBonusDamageMultiplier()) * (1 + calcs.basicStats.getBonusArcaneDamageMultiplier());
        dotEffectDamageModifier *= 1 - 0.02f * (calcs.targetLevel - 80);

        float gcd = 1.5f / (1.0f + spellHaste);
        float instantCast = Math.max(gcd, 1.0f) + latency;
        float ngGCD = Math.max(gcd / 1.2f, 1.0f);
        float instantCastNG = ngGCD + latency;
        float normalCastTime = Math.max(dotSpell.castTime / (1 + spellHaste), instantCast);
        float ngCastTime = Math.max(dotSpell.castTime / (1 + spellHaste) / (1 + 0.2f * naturesGrace / 3.0f), instantCastNG);
        float ngProcChance = spellCrit * naturesGrace / 3.0f;
        float ngUptime = 1.0f - (float) Math.pow(1.0f - ngProcChance, Math.floor(3.0f / normalCastTime) + 1.0f);
        dotSpell.castTime = (1 - ngUptime) * normalCastTime + ngUptime * ngCastTime;

        float mfDirectDamage = (dotSpell.baseDamage + dotSpell.spellDamageModifier * (spellPower + dotSpell.idolExtraSpellPower)) * overallDamageModifier;
        float mfCritDamage = mfDirectDamage * dotSpell.criticalDamageModifier;
        float totalCritChance = spellCrit + dotSpell.criticalChanceModifier;
        dotSpell.damagePerHit = (totalCritChance * mfCritDamage + (1 - totalCritChance) * mfDirectDamage) * spellHit;
        float normalDamagePerTick = dotSpell.dotEffect.tickDamage + dotSpell.dotEffect.spellDamageModifierPerTick * spellPower;
        float damagePerTick = 0.0f;
        if (dotSpell.dotEffect.canCrit) {
            float critDamagePerTick = normalDamagePerTick * dotSpell.criticalDamageModifier;
            damagePerTick = (totalCritChance * critDamagePerTick + (1 - totalCritChance) * normalDamagePerTick) * dotEffectDamageModifier;
        } else {
            damagePerTick = normalDamagePerTick * dotEffectDamageModifier;
        }
        dotSpell.dotEffect.damagePerHit = dotSpell.dotEffect.getNumberOfTicks() * damagePerTick * spellHit;
    }

    // Calculate damage and casting time for the Insect Swarm effect.
    private void doInsectSwarm(DruidTalents talents, CharacterCalculationsMoonkin calcs, Spell dotSpell, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        float latency = calcs.latency;

        float naturesGrace = talents.getNaturesGrace();

        float dotEffectDamageModifier = dotSpell.dotEffect.allDamageModifier * (1 + calcs.basicStats.getBonusSpellPowerMultiplier()) * (1 + calcs.basicStats.getBonusDamageMultiplier()) * (1 + calcs.basicStats.getBonusNatureDamageMultiplier());
        dotEffectDamageModifier *= 1 - 0.02f * (calcs.targetLevel - 80);

        float gcd = 1.5f / (1.0f + spellHaste);
        float instantCast = Math.max(gcd, 1.0f) + latency;
        float ngGCD = Math.max(gcd / 1.2f, 1.0f);
        float instantCastNG = ngGCD + latency;
        float normalCastTime = Math.max(dotSpell.castTime / (1 + spellHaste), instantCast);
        float ngCastTime = Math.max(dotSpell.castTime / (1 + spellHaste) / (1 + 0.2f * naturesGrace / 3.0f), instantCastNG);
        float ngProcChance = spellCrit * naturesGrace / 3.0f;
        float ngUptime = 1.0f - (float) Math.pow(1.0f - ngProcChance, Math.floor(3.0f / normalCastTime) + 1.0f);
        dotSpell.castTime = (1 - ngUptime) * normalCastTime + ngUptime * ngCastTime;
        float damagePerTick = (dotSpell.dotEffect.tickDamage + dotSpell.dotEffect.spellDamageModifierPerTick * (spellPower + dotSpell.idolExtraSpellPower)) * dotEffectDamageModifier;
        dotSpell.dotEffect.damagePerHit = dotSpell.dotEffect.getNumberOfTicks() * damagePerTick * spellHit;
    }

    // Perform damage and mana calculations for all spells in the given rotation.  Returns damage done over the total duration.
    public float damageDone(DruidTalents talents, CharacterCalculationsMoonkin calcs, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        if (talents.getEclipse() > 0) {
            return doEclipseCalcs(talents, calcs, solver, spellPower, spellHit, spellCrit, spellHaste);
        }
        float latency = calcs.latency;

        float moonkinFormProc = (talents.getMoonkinForm() == 1) ? 0.02f * calcs.basicStats.getMana() : 0.0f;
        boolean starfireGlyph = talents.isGlyphOfStarfire();
        int impInsectSwarm = talents.getImprovedInsectSwarm();

        switch (spellsUsed.size()) {
            // Nuke only
            case 1: {
                Spell mainNuke = solver.findSpell(spellsUsed.get(0));
                doMainNuke(talents, calcs, mainNuke, spellPower, spellHit, spellCrit, spellHaste);

                float omenProcChance = talents.getOmenOfClarity() == 1 ? 0.06f : 0;
                mainNuke.manaCost = mainNuke.baseManaCost - mainNuke.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana * spellHit - (spellCrit + mainNuke.criticalChanceModifier) * moonkinFormProc - mainNuke.baseManaCost * omenProcChance * spellHit;
                duration = mainNuke.castTime;
                rotationData.manaUsed = manaUsed = mainNuke.manaCost;
                rotationData.manaGained = manaGained = mainNuke.baseManaCost - mainNuke.manaCost;
                rotationData.dpm = mainNuke.damagePerHit / mainNuke.manaCost;
                castCount = 1.0f;
                dotTicks = 0.0f;
                wrathCount = mainNuke.name.equals("W") ? 1.0f : 0.0f;
                starfireCount = mainNuke.name.equals("SF") ? 1.0f : 0.0f;

                return mainNuke.damagePerHit;
            }
            // Nuke + 1 DotEffect
            case 2: {
                // Find the spells
                Spell dotEffectSpell = solver.findSpell(spellsUsed.get(0));
                Spell mainNuke = solver.findSpell(spellsUsed.get(1));
                // Do Starfire glyph calculations, if applicable; then do DoT spell calculations
                if (starfireGlyph && mainNuke.name.equals("SF") && dotEffectSpell.name.equals("MF"))
                    dotEffectSpell.dotEffect.duration += 9.0f;
                doDotSpell(talents, calcs, dotEffectSpell, spellPower, spellHit, spellCrit, spellHaste);

                // Do iIS calculations, if applicable
                if (impInsectSwarm > 0) {
                    if (mainNuke.name.equals("SF") && dotEffectSpell.name.equals("MF"))
                        mainNuke.criticalChanceModifier += 0.01f * impInsectSwarm;
                    else if (mainNuke.name.equals("W") && dotEffectSpell.name.equals("IS"))
                        mainNuke.allDamageModifier *= 1 + 0.01f * impInsectSwarm;
                }
                // Calculate main nuke damage
                doMainNuke(talents, calcs, mainNuke, spellPower, spellHit, spellCrit, spellHaste);

                // Set rotation duration
                duration = dotEffectSpell.dotEffect.duration;

                // Calculate mana usage and damage done for this rotation
                float timeSpentCastingNuke = duration - dotEffectSpell.castTime;
                float nukeDamageDone = mainNuke.damagePerHit / mainNuke.castTime * timeSpentCastingNuke;

                float numNukeCasts = timeSpentCastingNuke / mainNuke.castTime;
                float nukeManaSpent = mainNuke.baseManaCost * numNukeCasts;
                float totalManaSpent = nukeManaSpent + dotEffectSpell.baseManaCost;
                castCount = numNukeCasts + 1.0f;
                wrathCount = mainNuke.name.equals("W") ? numNukeCasts : 0.0f;
                starfireCount = mainNuke.name.equals("SF") ? numNukeCasts : 0.0f;
                dotTicks = dotEffectSpell.dotEffect.getNumberOfTicks();
                if (dotEffectSpell.name.equals("IS"))
                    insectSwarmTicks = dotTicks;
                else if (dotEffectSpell.name.equals("MF")) {
                    moonfireTicks = dotTicks;
                    moonfireCasts = 1f;
                }

                float manaFromJoW = (mainNuke.manaCost - mainNuke.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana) * numNukeCasts;
                manaFromJoW += dotEffectSpell.manaCost - 1.5f / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana;
                float manaFromOoC = ((0.06f) * mainNuke.baseManaCost
                        + (numNukeCasts - 1) * (0.06f) * mainNuke.baseManaCost
                        + (0.06f) * dotEffectSpell.baseManaCost) * spellHit;
                float manaFromMoonkin = moonkinFormProc * spellHit * ((spellCrit + mainNuke.criticalChanceModifier) * numNukeCasts + dotEffectSpell.criticalChanceModifier);

                float actualManaSpent = totalManaSpent - manaFromJoW - manaFromMoonkin - (talents.getOmenOfClarity() == 1 ? manaFromOoC : 0.0f);

                rotationData.manaUsed = manaUsed = actualManaSpent;
                rotationData.manaGained = manaGained = totalManaSpent - actualManaSpent;
                rotationData.dpm = (nukeDamageDone + dotEffectSpell.damagePerHit + dotEffectSpell.dotEffect.damagePerHit) / totalManaSpent;

                // Undo iIS, if applicable
                if (impInsectSwarm > 0) {
                    if (mainNuke.name.equals("SF") && dotEffectSpell.name.equals("MF"))
                        mainNuke.criticalChanceModifier -= 0.01f * impInsectSwarm;
                    else if (mainNuke.name.equals("W") && dotEffectSpell.name.equals("IS"))
                        mainNuke.allDamageModifier /= 1 + 0.01f * impInsectSwarm;
                }
                // Undo SF glyph, if applicable
                if (starfireGlyph && mainNuke.name.equals("SF") && dotEffectSpell.name.equals("MF"))
                    dotEffectSpell.dotEffect.duration -= 9.0f;
                // Return the damage done per rotation
                return nukeDamageDone + dotEffectSpell.damagePerHit + dotEffectSpell.dotEffect.damagePerHit;
            }
            // Nuke + both DotEffects
            case 3: {
                // Find the spells
                Spell moonFire = solver.findSpell(spellsUsed.get(0));
                Spell insectSwarm = solver.findSpell(spellsUsed.get(1));
                Spell mainNuke = solver.findSpell(spellsUsed.get(2));
                // Do Starfire glyph calculations, if applicable; then do DoT spell calculations
                if (starfireGlyph && mainNuke.name.equals("SF")) moonFire.dotEffect.duration += 9.0f;
                doDotSpell(talents, calcs, moonFire, spellPower, spellHit, spellCrit, spellHaste);
                doDotSpell(talents, calcs, insectSwarm, spellPower, spellHit, spellCrit, spellHaste);

                // Do iIS calculations, if applicable
                if (impInsectSwarm > 0) {
                    if (mainNuke.name.equals("SF"))
                        mainNuke.criticalChanceModifier += 0.01f * impInsectSwarm;
                    else if (mainNuke.name.equals("W"))
                        mainNuke.allDamageModifier *= 1 + 0.01f * impInsectSwarm;
                }
                // Calculate main nuke damage
                doMainNuke(talents, calcs, mainNuke, spellPower, spellHit, spellCrit, spellHaste);

                // Set rotation duration
                duration = moonFire.dotEffect.duration;

                // Calculate mana usage and damage done for this rotation
                float timeSpentCastingIS = insectSwarm.castTime * moonFire.dotEffect.duration / insectSwarm.dotEffect.duration;
                float insectSwarmDamage = insectSwarm.dotEffect.damagePerHit * moonFire.dotEffect.duration / insectSwarm.dotEffect.duration;
                float timeSpentCastingNuke = duration - timeSpentCastingIS - moonFire.castTime;
                float nukeDamageDone = mainNuke.damagePerHit / mainNuke.castTime * timeSpentCastingNuke;

                float numNukeCasts = timeSpentCastingNuke / mainNuke.castTime;
                float numISCasts = timeSpentCastingIS / insectSwarm.castTime;
                float nukeManaSpent = mainNuke.baseManaCost * numNukeCasts;
                float totalManaSpent = nukeManaSpent + moonFire.baseManaCost + numISCasts * insectSwarm.baseManaCost;
                castCount = numNukeCasts + numISCasts + 1.0f;
                wrathCount = mainNuke.name.equals("W") ? numNukeCasts : 0.0f;
                starfireCount = mainNuke.name.equals("SF") ? numNukeCasts : 0.0f;
                dotTicks = moonFire.dotEffect.getNumberOfTicks() + numISCasts * insectSwarm.dotEffect.getNumberOfTicks();
                insectSwarmTicks = numISCasts * insectSwarm.dotEffect.getNumberOfTicks();
                moonfireTicks = moonFire.dotEffect.getNumberOfTicks();
                moonfireCasts = 1.0f;

                float manaFromJoW = numNukeCasts * (mainNuke.manaCost - mainNuke.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana)
                        + (moonFire.manaCost - 1.5f / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana)
                        + numISCasts * (insectSwarm.manaCost - 1.5f / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana);
                float manaFromOoC = ((0.06f) * mainNuke.baseManaCost
                        + (numNukeCasts - 1 - numISCasts) * (0.06f) * mainNuke.baseManaCost
                        + (0.06f) * moonFire.baseManaCost
                        + (0.06f) * numISCasts * insectSwarm.baseManaCost
                        + (0.06f) * numISCasts * mainNuke.baseManaCost) * spellHit;
                float manaFromMoonkin = moonkinFormProc * spellHit * ((spellCrit + mainNuke.criticalChanceModifier) * numNukeCasts + moonFire.criticalChanceModifier);

                float actualManaSpent = totalManaSpent - manaFromJoW - manaFromMoonkin - (talents.getOmenOfClarity() == 1 ? manaFromOoC : 0.0f);

                rotationData.manaUsed = manaUsed = actualManaSpent;
                rotationData.manaGained = manaGained = totalManaSpent - actualManaSpent;
                rotationData.dpm = (nukeDamageDone + moonFire.damagePerHit + moonFire.dotEffect.damagePerHit + insectSwarm.dotEffect.damagePerHit) / totalManaSpent;

                // Undo iIS, if applicable
                if (impInsectSwarm > 0) {
                    if (mainNuke.name.equals("SF"))
                        mainNuke.criticalChanceModifier -= 0.01f * impInsectSwarm;
                    else if (mainNuke.name.equals("W"))
                        mainNuke.allDamageModifier /= 1 + 0.01f * impInsectSwarm;
                }
                // Undo SF glyph
                if (starfireGlyph && mainNuke.name.equals("SF")) moonFire.dotEffect.duration -= 9.0f;
                // Return the damage done per rotation
                return nukeDamageDone + moonFire.damagePerHit + moonFire.dotEffect.damagePerHit + insectSwarmDamage;
            }
            default:
                throw new RuntimeException("Invalid rotation specified in rotation solver.");
        }
    }

    private float doEclipseCalcs(DruidTalents talents, CharacterCalculationsMoonkin calcs, MoonkinSolver solver, float spellPower, float spellHit, float spellCrit, float spellHaste) {
        float latency = calcs.latency;

        float omenOfClarityProcChance = talents.getOmenOfClarity() * 0.06f;
        float moonkinFormProc = (talents.getMoonkinForm() == 1) ? 0.02f * calcs.basicStats.getMana() : 0.0f;
        boolean starfireGlyph = talents.isGlyphOfStarfire();
        int impInsectSwarm = talents.getImprovedInsectSwarm();

        float moonfireCasts = spellsUsed.contains("MF") ? 1.0f : 0.0f;
        float insectSwarmCasts = spellsUsed.contains("IS") ? 1.0f : 0.0f;

        Spell moonfire = moonfireCasts > 0 ? solver.findSpell("MF") : null;
        Spell insectSwarm = insectSwarmCasts > 0 ? solver.findSpell("IS") : null;

        // Do SF glyph
        if (starfireGlyph && moonfire != null) moonfire.dotEffect.duration += 9.0f;

        float eclipseMultiplier = 0.4f + calcs.basicStats.getEclipseBonus();

        float eclipseDuration = 15.0f;
        //float eclipseCooldown = 30.0f;

        Spell preLunarCast = solver.findSpell("W");
        // Do improved Insect Swarm
        if (insectSwarm != null)
            preLunarCast.allDamageModifier *= 1 + 0.01f * impInsectSwarm;

        Spell solarEclipseCast = new Spell(preLunarCast);

        // Eclipse bonus and improved Insect Swarm
        // NOTE: Eclipse bonus additive with Moonfury and 4T9; multiplicative with everything else
        solarEclipseCast.allDamageModifier = 1 + (float) Math.floor(talents.getMoonfury() * 10 / 3.0f) / 100.0f + calcs.basicStats.getBonusMoonkinNukeDamage() + eclipseMultiplier;
        if (insectSwarm != null)
            solarEclipseCast.allDamageModifier *= 1 + 0.01f * impInsectSwarm;

        Spell preSolarCast = solver.findSpell("SF");
        if (moonfire != null)
            preSolarCast.criticalChanceModifier += 0.01f * impInsectSwarm;

        Spell lunarEclipseCast = new Spell(preSolarCast);
        lunarEclipseCast.criticalChanceModifier = Math.min(1.0f - spellCrit, lunarEclipseCast.criticalChanceModifier + eclipseMultiplier);

        doMainNuke(talents, calcs, preSolarCast, spellPower, spellHit, spellCrit, spellHaste);
        doMainNuke(talents, calcs, solarEclipseCast, spellPower, spellHit, spellCrit, spellHaste);
        doMainNuke(talents, calcs, preLunarCast, spellPower, spellHit, spellCrit, spellHaste);
        doMainNuke(talents, calcs, lunarEclipseCast, spellPower, spellHit, spellCrit, spellHaste);

        float moonfireRatio = 0.0f;
        float insectSwarmRatio = 0.0f;
        if (moonfire != null) {
            doDotSpell(talents, calcs, moonfire, spellPower, spellHit, spellCrit, spellHaste);
            moonfireRatio = moonfire.castTime / moonfire.dotEffect.duration;
        }
        if (insectSwarm != null) {
            doDotSpell(talents, calcs, insectSwarm, spellPower, spellHit, spellCrit, spellHaste);
            insectSwarmRatio = insectSwarm.castTime / insectSwarm.dotEffect.duration;
        }

        float castRatio = moonfireRatio + (1 - moonfireRatio) * insectSwarmRatio;

        float lunarProcChance = (spellCrit + preLunarCast.criticalChanceModifier) * spellHit * talents.getEclipse() / 3.0f * 0.6f;
        float castsToProcLunar = 1.0f / lunarProcChance;
        float timeToProcLunar = preLunarCast.castTime * (castsToProcLunar - 0.5f);

        float solarProcChance = (spellCrit + preSolarCast.criticalChanceModifier) * spellHit * talents.getEclipse() / 3.0f;
        float castsToProcSolar = 1.0f / solarProcChance;
        float timeToProcSolar = preSolarCast.castTime * (castsToProcSolar - 0.5f);

        float preLunarTime = timeToProcLunar + (preLunarCast.castTime * 0.5f) + preLunarCast.ngCastTime * 1.5f;
        float preLunarDPS = preLunarCast.damagePerHit / preLunarCast.castTime;
        float preLunarManaUsed = preLunarCast.baseManaCost / preLunarCast.castTime * preLunarTime;
        float preLunarManaGained = (preLunarCast.baseManaCost * omenOfClarityProcChance)
                + ((spellCrit + preLunarCast.criticalChanceModifier) * spellHit * moonkinFormProc)
                + (preLunarCast.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana);

        float lunarTime = (eclipseDuration - (preLunarCast.ngCastTime * 1.5f) - lunarEclipseCast.castTime * 0.5f) * (1.0f - castRatio);
        float lunarDPS = lunarEclipseCast.damagePerHit / lunarEclipseCast.castTime;
        float lunarManaUsed = lunarEclipseCast.baseManaCost / lunarEclipseCast.castTime * lunarTime;
        float lunarManaGained = (lunarEclipseCast.baseManaCost * omenOfClarityProcChance)
                + ((spellCrit + lunarEclipseCast.criticalChanceModifier) * spellHit * moonkinFormProc)
                + (lunarEclipseCast.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana);

        float preSolarTime = timeToProcSolar + (lunarEclipseCast.castTime * 0.5f) + preSolarCast.ngCastTime;
        float preSolarDPS = preSolarCast.damagePerHit / preSolarCast.castTime;
        float preSolarManaUsed = preSolarCast.baseManaCost / preSolarCast.castTime * preSolarTime;
        float preSolarManaGained = (preSolarCast.baseManaCost * omenOfClarityProcChance)
                + ((spellCrit + preSolarCast.criticalChanceModifier) * spellHit * moonkinFormProc)
                + (preSolarCast.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana);

        float solarTime = (eclipseDuration - (preSolarCast.ngCastTime) - (preLunarCast.castTime * 0.5f)) * (1.0f - castRatio);
        float solarDPS = solarEclipseCast.damagePerHit / solarEclipseCast.castTime;
        float solarManaUsed = solarEclipseCast.baseManaCost / solarEclipseCast.castTime * solarTime;
        float solarManaGained = (solarEclipseCast.baseManaCost * omenOfClarityProcChance)
                + ((spellCrit + solarEclipseCast.criticalChanceModifier) * spellHit * moonkinFormProc)
                + (solarEclipseCast.baseCastTime / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana);

        float rotationLength = (solarTime + lunarTime + preSolarTime + preLunarTime) / (1 - castRatio);

        float etSpentOnMFMFPriority = rotationLength * moonfireRatio;
        float etSpentOnISMFPriority = rotationLength * castRatio - etSpentOnMFMFPriority;
        float etSpentOnISISPriority = rotationLength * insectSwarmRatio;
        float etSpentOnMFISPriority = rotationLength * castRatio - etSpentOnISISPriority;

        float moonfireDamagePerCastTime = moonfireRatio > 0 ? (moonfire.damagePerHit + moonfire.dotEffect.damagePerHit) / moonfire.castTime : 0.0f;
        float insectSwarmDamagePerCastTime = insectSwarmRatio > 0 ? insectSwarm.dotEffect.damagePerHit / insectSwarm.castTime : 0.0f;

        float dotDamageMoonfirePriority = etSpentOnMFMFPriority * moonfireDamagePerCastTime + etSpentOnISMFPriority * insectSwarmDamagePerCastTime;
        float dotDamageInsectSwarmPriority = etSpentOnISISPriority * insectSwarmDamagePerCastTime + etSpentOnMFISPriority * moonfireDamagePerCastTime;

        float totalDotDamage = 0.0f;

        if (dotDamageMoonfirePriority > dotDamageInsectSwarmPriority) {
            moonfireCasts = moonfireRatio > 0 ? etSpentOnMFMFPriority / moonfire.castTime : 0.0f;
            insectSwarmCasts = insectSwarmRatio > 0 ? etSpentOnISMFPriority / insectSwarm.castTime : 0.0f;
            totalDotDamage = dotDamageMoonfirePriority;
        } else {
            moonfireCasts = moonfireRatio > 0 ? etSpentOnMFISPriority / moonfire.castTime : 0.0f;
            insectSwarmCasts = insectSwarmRatio > 0 ? etSpentOnISISPriority / insectSwarm.castTime : 0.0f;
            totalDotDamage = dotDamageInsectSwarmPriority;
        }

        float moonfireTicks = moonfire != null ? moonfireCasts * moonfire.dotEffect.getNumberOfTicks() : 0.0f;
        float insectSwarmTicks = insectSwarm != null ? insectSwarmCasts * insectSwarm.dotEffect.getNumberOfTicks() : 0.0f;

        float moonfireManaUsed = moonfire != null ? moonfireCasts * moonfire.baseManaCost : 0.0f;
        float insectSwarmManaUsed = insectSwarm != null ? insectSwarmCasts * insectSwarm.baseManaCost : 0.0f;

        float damageDone = preSolarTime * preSolarDPS + solarTime * solarDPS + preLunarTime * preLunarDPS + lunarTime * lunarDPS
                + totalDotDamage;

        duration = rotationLength;
        dotTicks = moonfireTicks + insectSwarmTicks;
        this.insectSwarmTicks = insectSwarmTicks;
        this.moonfireTicks = moonfireTicks;
        this.moonfireCasts = moonfireCasts;
        this.insectSwarmCasts = insectSwarmCasts;
        castCount = castsToProcLunar + (lunarTime / lunarEclipseCast.castTime) + castsToProcSolar + (solarTime / solarEclipseCast.castTime) + moonfireCasts + insectSwarmCasts;

        wrathCount = castsToProcLunar + (solarTime / solarEclipseCast.castTime);
        starfireCount = castsToProcSolar + (lunarTime / lunarEclipseCast.castTime);

        manaUsed = preSolarManaUsed + solarManaUsed + preLunarManaUsed + lunarManaUsed + moonfireManaUsed + insectSwarmManaUsed;
        manaGained = castsToProcSolar * preSolarManaGained + (solarTime / solarEclipseCast.castTime) * solarManaGained + castsToProcLunar * preLunarManaGained + (lunarTime / lunarEclipseCast.castTime) * lunarManaGained;

        if (spellCrit + lunarEclipseCast.criticalChanceModifier > starfireEclipseCrit) {
            starfireEclipseCrit = spellCrit + lunarEclipseCast.criticalChanceModifier;
        }
        if (spellCrit + preSolarCast.criticalChanceModifier > starfireNonEclipseCrit) {
            starfireNonEclipseCrit = spellCrit + preSolarCast.criticalChanceModifier;
        }

        insectSwarmDuration = insectSwarm != null ? insectSwarm.dotEffect.duration : 0.0f;
        moonfireDuration = moonfire != null ? moonfire.dotEffect.duration : 0.0f;

        float mfSavingsFromOoC = moonfire != null ? (moonfire.baseManaCost - (moonfire.baseManaCost
                * (1 - starfireCount / wrathCount * 0.06f - (1 - starfireCount / wrathCount) * 0.06f))) : 0.0f;
        float isSavingsFromOoC = insectSwarm != null ? (insectSwarm.baseManaCost - (insectSwarm.baseManaCost
                * (1 - starfireCount / wrathCount * 0.06f - (1 - starfireCount / wrathCount) * 0.06f))) : 0.0f;
        manaGained += moonfire != null ? (moonfireCasts * (mfSavingsFromOoC
                + ((spellCrit + moonfire.criticalChanceModifier) * moonkinFormProc * spellHit)
                + 1.5f / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana)) : 0.0f;
        manaGained += insectSwarm != null ? (insectSwarmCasts * (isSavingsFromOoC + 1.5f / 60.0f * calcs.basicStats.getManaRestoreFromBaseManaPPM() * CalculationsMoonkin.BaseMana)) : 0.0f;

        rotationData.manaGained = manaGained;
        rotationData.dpm = damageDone / manaUsed;
        manaUsed -= manaGained;
        rotationData.manaUsed = manaUsed;

        // Undo SF glyph
        if (starfireGlyph && moonfire != null) moonfire.dotEffect.duration -= 9.0f;

        // Undo improved Insect Swarm
        if (insectSwarm != null) {
            preLunarCast.allDamageModifier /= 1 + 0.01f * impInsectSwarm;
        }
        if (moonfire != null) {
            preSolarCast.criticalChanceModifier -= 0.01f * impInsectSwarm;
        }

        return damageDone;
    }
}
