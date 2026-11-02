// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetCalculations.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.talents.HunterTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.models.hunter.HunterEnums.PetAttacks;
import rawr.models.hunter.HunterEnums.PetFamily;
import rawr.models.hunter.HunterEnums.PetHappiness;
import rawr.models.hunter.HunterEnums.PetSkillType;
import rawr.models.hunter.HunterEnums.Shots;

import java.util.ArrayList;
import java.util.List;

public class PetCalculations {
    // Variables
    Character character;
    CharacterCalculationsHunter calculatedStats;
    CalculationOptionsHunter calcOpts;
    BossOptions bossOpts;
    PetTalentTreeData petTalents;
    HunterTalents talents;
    Stats hunterStats;
    Stats statsPetBuffs;
    public Stats petStats;
    public PetSkillPriorityRotation priorityRotation;

    public float ferociousInspirationUptime;

    // things we save earlier for later DPS calcs
    private float killCommandCooldown;
    private float critSpecialsAdjust;

    private float petChanceToMiss = StatConversion.YELLOW_MISS_CHANCE_CAP[83 - 80];
    private float petChanceToSpellMiss = StatConversion.getSpellMiss(83 - 80, false);
    private float petChanceToBeDodged = StatConversion.YELLOW_DODGE_CHANCE_CAP[83 - 80];

    public PetCalculations(Character character, CharacterCalculationsHunter calculatedStats, CalculationOptionsHunter calcopts, BossOptions bossOpts,
            Stats hunterStats, Stats statsPetBuffs) {
        this.character = character;
        this.calculatedStats = calculatedStats;
        this.calcOpts = calcopts;
        this.bossOpts = bossOpts;
        this.petTalents = calcopts.getPetTalents();
        this.talents = character.getHunterTalents();
        this.hunterStats = hunterStats;
        this.statsPetBuffs = statsPetBuffs;

        petStats = new Stats();
    }

    private Stats basePetStats = null;

    private Stats basePetStats() {
        if (this.basePetStats == null) {
            this.basePetStats = new Stats();
            this.basePetStats.setAgility(113);
            this.basePetStats.setStrength(331);
            this.basePetStats.setStamina(361);
            this.basePetStats.setIntellect(65);
            this.basePetStats.setSpirit(10);
        }
        return this.basePetStats;
    }

    private void calculateTimings() {
        // Focus Regen
        float focusRegenBasePer4 = 20f;
        float focusRegenBestialDiscipline = focusRegenBasePer4 * 0.5f * talents.getBestialDiscipline();

        float critHitsPer4 = calculatedStats.shotsPerSecondCritting * calculatedStats.priorityRotation.critsCompositeSum * 4f;
        float goForTheThroatPerCrit = 25 * talents.getGoForTheThroat();
        float focusRegenGoForTheThroat = critHitsPer4 * goForTheThroatPerCrit;

        float focusRegenPerSecond = (focusRegenBasePer4 + focusRegenBestialDiscipline + focusRegenGoForTheThroat) / 4f;

        float owlsFocusEffect = 0f;
        if (petTalents.owlsFocus.value > 0) {
            owlsFocusEffect = 1f / (1f / (petTalents.owlsFocus.value * 0.15f) + 1f);
        }

        if (priorityRotation == null) {
            priorityRotation = new PetSkillPriorityRotation(character, calcOpts);
            priorityRotation.addSkill(calcOpts.petPriority1);
            priorityRotation.addSkill(calcOpts.petPriority2);
            priorityRotation.addSkill(calcOpts.petPriority3);
            priorityRotation.addSkill(calcOpts.petPriority4);
            priorityRotation.addSkill(calcOpts.petPriority5);
            priorityRotation.addSkill(calcOpts.petPriority6);
            priorityRotation.addSkill(calcOpts.petPriority7);
        }

        priorityRotation.owlsFocus = owlsFocusEffect;
        priorityRotation.fpsGained = focusRegenPerSecond;

        priorityRotation.calculateTimings();
    }

    public Stats getSpecialEffectsStats(Character character,
            float[] attemptedAtkIntervals,
            float[] hitRates, float[] critRates, float bleedHitInterval, float dmgDoneInterval,
            Stats statsTotal, Stats statsToProcess) {
        Stats statsProcs = new Stats();
        float fightDuration = calcOpts.duration;
        float atkspeed = attemptedAtkIntervals[1];

        for (rawr.base.effects.SpecialEffect effect : (statsToProcess != null ? statsToProcess.specialEffects() : statsTotal.specialEffects())) {
            switch (effect.getTrigger()) {
                case Use: {
                    Stats _stats = new Stats();
                    if (effect.getStats().getRawSpecialEffectDataSize() == 1 && statsToProcess == null) {
                        float uptime = effect.getAverageUptime(0f, 1f, atkspeed, fightDuration);
                        _stats.addSpecialEffect(effect.getStats().getRawSpecialEffectData()[0]);
                        Stats _stats2 = getSpecialEffectsStats(character,
                                attemptedAtkIntervals,
                                hitRates, critRates, bleedHitInterval, dmgDoneInterval, statsTotal, _stats);
                        _stats = Stats.multiply(_stats2, uptime);
                    } else {
                        _stats = effect.getAverageStats(0f, 1f, atkspeed, fightDuration);
                    }
                    statsProcs.accumulate(_stats);
                    break;
                }
                case MeleeHit:
                case PhysicalHit: {
                    if (attemptedAtkIntervals[0] > 0f) {
                        Stats add = effect.getAverageStats(attemptedAtkIntervals[0], hitRates[0], atkspeed, fightDuration);
                        statsProcs.accumulate(add);
                    }
                    break;
                }
                case MeleeCrit:
                case PhysicalCrit: {
                    if (attemptedAtkIntervals[0] > 0f) {
                        Stats add = effect.getAverageStats(attemptedAtkIntervals[0], critRates[0], atkspeed, fightDuration);
                        statsProcs.accumulate(add);
                    }
                    break;
                }
                case DoTTick: {
                    if (bleedHitInterval > 0f) {
                        statsProcs.accumulate(effect.getAverageStats(bleedHitInterval, 1f, atkspeed, fightDuration));
                    } // 1/sec DeepWounds, 1/3sec Rend
                    break;
                }
                case DamageDone: { // physical and dots
                    if (dmgDoneInterval > 0f) {
                        statsProcs.accumulate(effect.getAverageStats(dmgDoneInterval, 1f, atkspeed, fightDuration));
                    }
                    break;
                }
                case PetClawBiteSmackCrit: {
                    if (attemptedAtkIntervals[3] > 0f) {
                        Stats add = effect.getAverageStats(attemptedAtkIntervals[3], critRates[1], atkspeed, fightDuration); // this needs to be fixed to read steady shot frequencies
                        statsProcs.accumulate(add);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return statsProcs;
    }

    // Speeds and Intervals
    private float attackSpeedEffective = 2f;
    private float compositeSpeed = 2f;
    public float petClawBiteSmackInterval = 0f;

    protected float petAttackSpeed(Stats currentStats) {
        float attackSpeedBase = calcOpts.petFamily == PetFamily.None ? 0.0f : 2.0f;
        float attackSpeedAdjust = (1f + currentStats.getPhysicalHaste());
        float attackSpeedAdjusted = attackSpeedBase / attackSpeedAdjust;

        return attackSpeedAdjusted;
    }

    protected float genPetFullAttackSpeed(Stats currentStats) {
        attackSpeedEffective = petAttackSpeed(currentStats);
        calculateTimings();
        float petSpecialFreq = (priorityRotation.petSpecialFrequency > 0 ? priorityRotation.petSpecialFrequency : 0);
        compositeSpeed = 1f / (1f / attackSpeedEffective + (petSpecialFreq > 0 ? 1f / petSpecialFreq : 0));
        return compositeSpeed;
    }

    public float petWhiteInterval() {
        return petAttackSpeed(petStats);
    }

    public float petYellowInterval() {
        calculateTimings();
        return (priorityRotation.petSpecialFrequency > 0 ? priorityRotation.petSpecialFrequency : 0);
    }

    public float petCompInterval() {
        return genPetFullAttackSpeed(petStats);
    }

    public PetAttackTable whAtkTable;
    public PetAttackTable ywAtkTable;

    rawr.base.effects.SpecialEffect frenzy = null;

    public void genPetStats() {
        // Initial Variables
        int levelDiff = calcOpts.targetLevel - character.getLevel();
        Stats petStatsBase = basePetStats();
        // From Hunter
        Stats petStatsFromHunter = new Stats();
        petStatsFromHunter.setAttackPower(hunterStats.getRangedAttackPower() * 0.22f * (1f + petTalents.wildHunt.value * 0.15f));
        petStatsFromHunter.setSpellPower(hunterStats.getRangedAttackPower() * 0.1287f * (1f + petTalents.wildHunt.value * 0.15f));
        petStatsFromHunter.setStamina(hunterStats.getStamina() * 0.45f * (1f + petTalents.wildHunt.value * 0.20f)
                + hunterStats.getPetStamina());
        petStatsFromHunter.setStrength(hunterStats.getPetStrength());
        petStatsFromHunter.setSpirit(hunterStats.getPetSpirit());
        petStatsFromHunter.setArcaneResistance(hunterStats.getArcaneResistance() * 0.4f);
        petStatsFromHunter.setFireResistance(hunterStats.getFireResistance() * 0.4f);
        petStatsFromHunter.setNatureResistance(hunterStats.getNatureResistance() * 0.4f);
        petStatsFromHunter.setShadowResistance(hunterStats.getShadowResistance() * 0.4f);
        petStatsFromHunter.setFrostResistance(hunterStats.getFrostResistance() * 0.4f);
        petStatsFromHunter.setArmor(hunterStats.getArmor() * 0.35f);
        petStatsFromHunter.setSpellPenetration(hunterStats.getSpellPenetration());
        petStatsFromHunter.setResilience(hunterStats.getResilience());
        petStatsFromHunter.setBonusDamageMultiplier((1f + (hunterStats.getBonusDamageMultiplier()))
                * (1f + (character.getRace() == CharacterRace.Orc ? 0.05f : 0f))
                - 1f);
        petStatsFromHunter.setBonusPetDamageMultiplier(hunterStats.getBonusPetDamageMultiplier());
        petStatsFromHunter.setBonusBleedDamageMultiplier(hunterStats.getBonusBleedDamageMultiplier());
        petStatsFromHunter.setBonusPetAttackPowerMultiplier(hunterStats.getBonusPetAttackPowerMultiplier());
        petStatsFromHunter.setPetAttackPower(hunterStats.getPetAttackPower());

        // From Buffs
        Stats petStatsBuffs = statsPetBuffs;
        petStatsBuffs.setStamina(petStatsBuffs.getStamina() + petStatsBuffs.getPetStamina());
        petStatsBuffs.setStrength(petStatsBuffs.getStrength() + petStatsBuffs.getPetStrength());
        petStatsBuffs.setSpirit(petStatsBuffs.getSpirit() + petStatsBuffs.getPetSpirit());

        // From Talents (Pet or Hunter)
        Stats petStatsTalents = new Stats();
        petStatsTalents.setBonusStaminaMultiplier((1f + petTalents.bloodOfTheRhino.value * 0.02f)
                * (1f + petTalents.greatStamina.value * 0.04f)
                - 1f);
        petStatsTalents.setMovementSpeed(petTalents.boarsSpeed.value * 0.30f);
        petStatsTalents.setPhysicalHaste((1f + petTalents.cobraReflexes.value * 0.15f)
                * (1f + talents.getSerpentsSwiftness() * 0.04f)
                - 1f);
        petStatsTalents.setPhysicalCrit(petTalents.spidersBite.value * 0.03f
                + talents.getFerocity() * 0.02f);
        petStatsTalents.setBaseArmorMultiplier((1f + petTalents.naturalArmor.value * 0.05f)
                * (1f + petTalents.petBarding.value * 0.05f)
                - 1f);
        petStatsTalents.setArcaneResistance(petTalents.greatResistance.value * 0.05f);
        petStatsTalents.setFireResistance(petTalents.greatResistance.value * 0.05f);
        petStatsTalents.setNatureResistance(petTalents.greatResistance.value * 0.05f);
        petStatsTalents.setShadowResistance(petTalents.greatResistance.value * 0.05f);
        petStatsTalents.setFrostResistance(petTalents.greatResistance.value * 0.05f);
        petStatsTalents.setFearDurReduc(petTalents.lionhearted.value * 0.15f);
        petStatsTalents.setStunDurReduc(petTalents.lionhearted.value * 0.15f);
        petStatsTalents.setDodge(talents.getCatlikeReflexes() * 0.03f + petTalents.petBarding.value * 0.01f);
        petStatsTalents.setBonusDamageMultiplier((1f + talents.getUnleashedFury() * 0.03f)
                * (1f + petTalents.spikedCollar.value * 0.03f)
                * (1f + talents.getKindredSpirits() * 0.04f)
                * (1f + petTalents.sharkAttack.value * 0.03f)
                - 1f);
        petStatsTalents.setBonusAttackPowerMultiplier((1f + talents.getAnimalHandler() * 0.05f)
                * (1f + calculatedStats.aspectBonusAPBeast)
                - 1f);
        petStatsTalents.setBonusHealthMultiplier(talents.getEnduranceTraining() * 0.02f);

        float LongevityCdAdjust = 1f - talents.getLongevity() * 0.10f;
        if (petTalents.rabid.value > 0) {
            float rabidCooldown = 45f * LongevityCdAdjust;
            rawr.base.effects.SpecialEffect primary = new rawr.base.effects.SpecialEffect(Trigger.Use, new Stats(), 20f, rabidCooldown);
            Stats secondaryStats = new Stats();
            secondaryStats.setBonusAttackPowerMultiplier(0.05f);
            rawr.base.effects.SpecialEffect secondary = new rawr.base.effects.SpecialEffect(Trigger.MeleeHit,
                    secondaryStats, 20f, 0f, 0.50f, 5);
            primary.getStats().addSpecialEffect(secondary);
            petStatsTalents.addSpecialEffect(primary);
        }
        if (talents.getFrenzy() > 0) {
            if (frenzy == null) {
                Stats frenzyStats = new Stats();
                frenzyStats.setPhysicalHaste(0.30f);
                frenzy = new rawr.base.effects.SpecialEffect(Trigger.MeleeCrit, frenzyStats, 8f, 1f, talents.getFrenzy() * 0.20f);
            }
            petStatsTalents.addSpecialEffect(frenzy);
        }
        if (petTalents.lastStand.value > 0) {
            Stats lastStandStats = new Stats();
            lastStandStats.setBonusHealthMultiplier(0.30f);
            rawr.base.effects.SpecialEffect laststand = new rawr.base.effects.SpecialEffect(Trigger.Use, lastStandStats, 20f, (1f * 60f) * LongevityCdAdjust);
            petStatsTalents.addSpecialEffect(laststand);
        }

        // From Options
        Stats petStatsOptionsPanel = new Stats();
        petStatsOptionsPanel.setPhysicalCrit(StatConversion.NPC_LEVEL_CRIT_MOD[levelDiff]);
        petStatsOptionsPanel.setBonusStaminaMultiplier(0.05f);
        petStatsOptionsPanel.setBonusDamageMultiplier((calcOpts.petHappinessLevel == PetHappiness.Happy ? 1.25f
                : calcOpts.petHappinessLevel == PetHappiness.Content ? 1f
                : 0.75f)
                - 1f);
        calculateTimings();
        if (priorityRotation.getSkillFrequency(PetAttacks.SerenityDust) > 0) {
            // TODO: Need to make sure this freq actually works
            float freq = priorityRotation.getSkillFrequency(PetAttacks.SerenityDust);
            Stats serenityDustStats = new Stats();
            serenityDustStats.setBonusAttackPowerMultiplier(0.10f);
            rawr.base.effects.SpecialEffect serenitydust = new rawr.base.effects.SpecialEffect(Trigger.Use,
                    serenityDustStats,
                    15f, calcOpts.duration / freq);
            petStatsOptionsPanel.addSpecialEffect(serenitydust);
            petStatsOptionsPanel.setHealthRestore(petStatsOptionsPanel.getHealthRestore() + (calcOpts.duration / freq) * 825f);
        }

        // Totals
        Stats petStatsGearEnchantsBuffs = new Stats();
        petStatsGearEnchantsBuffs.accumulate(petStatsBuffs);
        Stats petStatsTotal = new Stats();
        petStatsTotal.accumulate(petStatsBase);
        petStatsTotal.accumulate(petStatsFromHunter);
        petStatsTotal.accumulate(petStatsBuffs);
        petStatsTotal.accumulate(petStatsTalents);
        petStatsTotal.accumulate(petStatsOptionsPanel);
        Stats petStatsProcs = new Stats();

        // Stamina & Health
        float totalBSTAM = petStatsTotal.getBonusStaminaMultiplier();
        petStatsTotal.setStamina((float) Math.floor((1f + totalBSTAM) * petStatsTotal.getStamina()));

        // Health | Pets dont get Health from their Base Stam, all pets have a 5% Bonus Stam
        petStatsTotal.setHealth(petStatsTotal.getHealth() + StatConversion.getHealthFromStamina(petStatsTotal.getStamina() - petStatsBase.getStamina()));
        petStatsTotal.setHealth(petStatsTotal.getHealth() * (1f + petStatsTotal.getBonusHealthMultiplier()));

        if (petTalents.bloodthirsty.value > 0) {
            Stats bloodthirstyStats = new Stats();
            bloodthirstyStats.setHealthRestore(petStatsTotal.getHealth() * 0.05f);
            rawr.base.effects.SpecialEffect bloodthirsty = new rawr.base.effects.SpecialEffect(Trigger.MeleeHit,
                    bloodthirstyStats,
                    0f, 0f, petTalents.bloodthirsty.value * 0.10f);
            petStatsTotal.addSpecialEffect(bloodthirsty);
        }

        // Strength
        float totalBSTRM = petStatsTotal.getBonusStrengthMultiplier();
        petStatsTotal.setStrength((1f + totalBSTRM) * petStatsTotal.getStrength());

        // Agility
        float totalBAGIM = petStatsTotal.getBonusAgilityMultiplier();
        petStatsTotal.setAgility((1f + totalBAGIM) * petStatsTotal.getAgility());

        // Attack Power
        petStatsTotal.setBonusAttackPowerMultiplier(petStatsTotal.getBonusAttackPowerMultiplier() * (1f + petStatsTotal.getBonusPetAttackPowerMultiplier()));
        float totalBAPM = petStatsTotal.getBonusAttackPowerMultiplier();

        float apFromBase = (1f + totalBAPM) * (petStatsTotal.getAttackPower() - petStatsFromHunter.getAttackPower());
        float apFromBonus = (1f + totalBAPM) * (petStatsTotal.getPetAttackPower());

        float apFromHunter = (1f + totalBAPM) * (petStatsFromHunter.getAttackPower());
        float apFromSTR = (1f + totalBAPM) * (petStatsTotal.getStrength() - 10f) * 2f;
        float apFromHvW = (1f + totalBAPM) * (hunterStats.getStamina() * 0.10f * talents.getHunterVsWild());

        petStatsTotal.setAttackPower(apFromBase + apFromBonus + apFromHunter + apFromSTR + apFromHvW);

        // Haste
        // (empty in the original)

        // Armor
        petStatsTotal.setArmor((float) Math.floor(petStatsTotal.getArmor() * (1f + petStatsTotal.getBaseArmorMultiplier())));
        petStatsTotal.setBonusArmor(petStatsTotal.getBonusArmor() + petStatsTotal.getAgility() * 2f);
        petStatsTotal.setBonusArmor((float) Math.floor(petStatsTotal.getBonusArmor() * (1f + petStatsTotal.getBonusArmorMultiplier())));
        petStatsTotal.setArmor(petStatsTotal.getArmor() + petStatsTotal.getBonusArmor());

        // Hit/Dodge Chances
        // This is tied directly to the Hunter's chance to miss
        petChanceToMiss = Math.max(0f, StatConversion.YELLOW_MISS_CHANCE_CAP[levelDiff] - hunterStats.getPhysicalHit());
        petChanceToSpellMiss = -1f * Math.min(0f, StatConversion.getSpellMiss(levelDiff, false) - hunterStats.getSpellHit());

        calculatedStats.petHitTotal = hunterStats.getPhysicalHit();
        calculatedStats.petHitSpellTotal = hunterStats.getSpellHit();

        petStatsTotal.setPhysicalHit(hunterStats.getPhysicalHit());
        petStatsTotal.setSpellHit(hunterStats.getSpellHit());

        // If the Hunter is Hit Capped, the pet is also Exp Capped
        // If not, Pet is proportionately lower based on Hunter's Hit
        // Expertise itself doesn't factor in at all
        petChanceToBeDodged = StatConversion.YELLOW_DODGE_CHANCE_CAP[levelDiff]
                * Math.min(1f, (petChanceToMiss / StatConversion.YELLOW_MISS_CHANCE_CAP[levelDiff]));
        calculatedStats.petTargetDodge = petChanceToBeDodged;

        float[] avoidChances = { petChanceToMiss, petChanceToSpellMiss, petChanceToBeDodged };

        // Crit Chance
        petStatsTotal.setPhysicalCrit(petStatsTotal.getPhysicalCrit() + StatConversion.getCritFromAgility(petStatsTotal.getAgility(), CharacterClass.Warrior)
                + 0.032f
                + StatConversion.getCritFromRating(petStatsTotal.getCritRating()));

        calculatedStats.petCritTotalMelee = petStatsTotal.getPhysicalCrit();

        // Cobra Strikes
        calculatedStats.petCritFromCobraStrikes = 0;
        float cobraStrikesProc = talents.getCobraStrikes() * 0.2f;
        if (cobraStrikesProc > 0) {
            float cobraStrikesCritFreqSteady = calculatedStats.steadyShot.freq * (1f / calculatedStats.steadyShot.getCritChance());
            float cobraStrikesCritFreqArcane = calculatedStats.arcaneShot.freq * (1f / calculatedStats.arcaneShot.getCritChance());
            float cobraStrikesCritFreqKill = calculatedStats.killShot.freq * (1f / calculatedStats.killShot.getCritChance());

            float cobraStrikesCritFreqSteadyInv = cobraStrikesCritFreqSteady > 0 ? 1f / cobraStrikesCritFreqSteady : 0;
            float cobraStrikesCritFreqArcaneInv = cobraStrikesCritFreqArcane > 0 ? 1f / cobraStrikesCritFreqArcane : 0;
            float cobraStrikesCritFreqKillInv = cobraStrikesCritFreqKill > 0 ? 1f / cobraStrikesCritFreqKill : 0;
            float cobraStrikesCritFreqInv = cobraStrikesCritFreqSteadyInv + cobraStrikesCritFreqArcaneInv + cobraStrikesCritFreqKillInv;
            float cobraStrikesCritFreq = cobraStrikesCritFreqInv > 0 ? 1f / cobraStrikesCritFreqInv : 0;
            float cobraStrikesProcAdjust = cobraStrikesCritFreq / cobraStrikesProc;

            float cobraStrikesFreqSteadyInv = calculatedStats.steadyShot.freq > 0 ? 1f / calculatedStats.steadyShot.freq : 0;
            float cobraStrikesFreqArcaneInv = calculatedStats.arcaneShot.freq > 0 ? 1f / calculatedStats.arcaneShot.freq : 0;
            float cobraStrikesFreqKillInv = calculatedStats.killShot.freq > 0 ? 1f / calculatedStats.killShot.freq : 0;
            float cobraStrikesFreqInv = cobraStrikesFreqSteadyInv + cobraStrikesFreqArcaneInv + cobraStrikesFreqKillInv;

            float cobraStrikesTwoSpecials = 2f * priorityRotation.petSpecialFrequency;
            float cobraStrikesUptime = 1f - (float) Math.pow(1f - calculatedStats.steadyShot.getCritChance() * cobraStrikesProc, cobraStrikesFreqInv * cobraStrikesTwoSpecials);

            calculatedStats.petCritFromCobraStrikes = (cobraStrikesUptime + (1f - cobraStrikesUptime) * calculatedStats.petCritTotalMelee) - calculatedStats.petCritTotalMelee;
        }

        calculatedStats.petCritTotalSpecials = petStatsTotal.getPhysicalCrit() + calculatedStats.petCritFromCobraStrikes; // PetCritChance
        critSpecialsAdjust = calculatedStats.petCritTotalSpecials * 1.5f + 1f;

        // Handle Special Effects
        calculateTimings();
        whAtkTable = new PetAttackTable(character, petStatsTotal, calcOpts, avoidChances, false, false);
        ywAtkTable = new PetAttackTable(character, petStatsTotal, calcOpts, avoidChances, PetAttacks.Claw, false, false);

        float AllAttemptedAtksInterval = genPetFullAttackSpeed(petStatsTotal);
        float WhtAttemptedAtksInterval = petAttackSpeed(petStatsTotal);
        float YlwAttemptedAtksInterval = AllAttemptedAtksInterval - WhtAttemptedAtksInterval;

        float[] hitRates = { whAtkTable.getAnyLand(),   // Whites
                ywAtkTable.getAnyLand() }; // Yellows
        float[] critRates = { whAtkTable.crit, // Whites
                ywAtkTable.crit + calculatedStats.petCritFromCobraStrikes }; // Yellows

        float bleedHitInterval = 0f;
        float rakefreq = priorityRotation.getSkillFrequency(PetAttacks.Rake);
        if (rakefreq > 0) {
            bleedHitInterval += rakefreq;
        }

        float dmgDoneInterval = 1f; // Need to Fix this

        float clawbitesmackinterval = 0f;
        float clawfreq = priorityRotation.getSkillFrequency(PetAttacks.Claw);
        if (clawfreq > 0) {
            clawbitesmackinterval += clawfreq;
        }
        float bitefreq = priorityRotation.getSkillFrequency(PetAttacks.Bite);
        if (bitefreq > 0) {
            clawbitesmackinterval += bitefreq;
        }
        float smakfreq = priorityRotation.getSkillFrequency(PetAttacks.Smack);
        if (smakfreq > 0) {
            clawbitesmackinterval += smakfreq;
        }
        petClawBiteSmackInterval = clawbitesmackinterval;

        float[] AttemptedAtkIntervals = {
                AllAttemptedAtksInterval, // All
                WhtAttemptedAtksInterval, // Whites
                YlwAttemptedAtksInterval, // Yellows
                petClawBiteSmackInterval, // ClawBiteSmack
        };

        petStatsProcs.accumulate(getSpecialEffectsStats(character, AttemptedAtkIntervals, hitRates, critRates,
                bleedHitInterval, dmgDoneInterval, petStatsTotal, null));

        // Stat Results of Special Effects
        // Base Stats
        petStatsProcs.setStamina((float) Math.floor(petStatsProcs.getStamina() * (1f + totalBSTAM) * (1f + petStatsProcs.getBonusStaminaMultiplier())));
        petStatsProcs.setStrength((float) Math.floor(petStatsProcs.getStrength() * (1f + totalBSTRM) * (1f + petStatsProcs.getBonusStrengthMultiplier())));
        petStatsProcs.setAgility((float) Math.floor(petStatsProcs.getAgility() * (1f + totalBAGIM) * (1f + petStatsProcs.getBonusAgilityMultiplier())));
        petStatsProcs.setHealth(petStatsProcs.getHealth() + (float) Math.floor(petStatsProcs.getStamina() * 10f));
        petStatsProcs.setHealth(petStatsProcs.getHealth() + (float) Math.floor((petStatsProcs.getHealth() + petStatsTotal.getHealth()) * petStatsProcs.getBonusHealthMultiplier()));

        // Armor
        petStatsProcs.setArmor((float) Math.floor(petStatsProcs.getArmor() * (1f + petStatsTotal.getBaseArmorMultiplier() + petStatsProcs.getBaseArmorMultiplier())));
        petStatsProcs.setBonusArmor(petStatsProcs.getBonusArmor() + petStatsProcs.getAgility() * 2f);
        petStatsProcs.setBonusArmor((float) Math.floor(petStatsProcs.getBonusArmor() * (1f + petStatsTotal.getBonusArmorMultiplier() + petStatsProcs.getBonusArmorMultiplier())));
        petStatsProcs.setArmor(petStatsProcs.getArmor() + petStatsProcs.getBonusArmor());
        petStatsProcs.setBonusArmor(0); //it's been added to Armor so kill it

        // Attack Power
        petStatsProcs.setBonusAttackPowerMultiplier(petStatsProcs.getBonusAttackPowerMultiplier() * (1f + petStatsProcs.getBonusPetAttackPowerMultiplier()));
        float totalBAPMProcs = (1f + totalBAPM) * (1f + petStatsProcs.getBonusAttackPowerMultiplier()) - 1f;
        float apFromSTRProcs = (1f + totalBAPMProcs) * (petStatsProcs.getStrength() * 2f);
        float apBonusOtherProcs = (1f + totalBAPMProcs) * (petStatsProcs.getAttackPower() + petStatsProcs.getPetAttackPower());
        float apBonusFromBasetoNewMulti = (petStatsProcs.getBonusAttackPowerMultiplier()) * (petStatsTotal.getAttackPower());
        petStatsProcs.setAttackPower((float) Math.floor(apFromSTRProcs + apBonusOtherProcs + apBonusFromBasetoNewMulti));

        // Crit
        petStatsProcs.setPhysicalCrit(petStatsProcs.getPhysicalCrit() + StatConversion.getCritFromAgility(petStatsProcs.getAgility(), CharacterClass.Warrior));

        petStatsTotal.accumulate(petStatsProcs);

        genPetFullAttackSpeed(petStatsTotal);
        calculateTimings();
        clawbitesmackinterval = 0f;
        clawfreq = priorityRotation.getSkillFrequency(PetAttacks.Claw);
        if (clawfreq > 0) {
            clawbitesmackinterval += clawfreq;
        }
        bitefreq = priorityRotation.getSkillFrequency(PetAttacks.Bite);
        if (bitefreq > 0) {
            clawbitesmackinterval += bitefreq;
        }
        smakfreq = priorityRotation.getSkillFrequency(PetAttacks.Smack);
        if (smakfreq > 0) {
            clawbitesmackinterval += smakfreq;
        }
        petClawBiteSmackInterval = clawbitesmackinterval;

        petStats = petStatsTotal;

        // Special Abilities Priority Rotation
        calculateTimings();

        // Kill Command MPS
        calculatedStats.petKillCommandMPS = 0;
        killCommandCooldown = 0;
        if (calcOpts.useKillCommand) {
            float killCommandManaCost = 0.03f * calculatedStats.baseMana;

            float killCommandReadinessFactor = calculatedStats.priorityRotation.containsShot(Shots.Readiness) ? 1.0f / 180f : 0f;
            float killCommandCooldownBase = 1.0f / (60f - talents.getCatlikeReflexes() * 10f);

            killCommandCooldown = 1.0f / (killCommandCooldownBase + killCommandReadinessFactor);

            calculatedStats.petKillCommandMPS = killCommandCooldown > 0 ? killCommandManaCost / killCommandCooldown : 0;
        }

        // Target Debuffs
        float armorDebuffSporeCloud = 0;
        float sporeCloudFrequency = priorityRotation.getSkillFrequency(PetAttacks.SporeCloud);
        if (sporeCloudFrequency > 0) {
            float sporeCloudDuration = 9f;
            float sporeCloudUptime = sporeCloudDuration > sporeCloudFrequency ? 1f : sporeCloudDuration / sporeCloudFrequency;

            armorDebuffSporeCloud = sporeCloudUptime * 0.03f;
        }

        float armorDebuffAcidSpit = 0;
        float acidSpitFrequency = priorityRotation.getSkillFrequency(PetAttacks.AcidSpit); // AcidSpitEffectiveRate
        if (acidSpitFrequency > 0) {
            float acidSpitCalcFreq = priorityRotation.getSkillCooldown(PetAttacks.AcidSpit);
            float acidSpitDuration = 30;

            float acidSpitChanceToApply = 1f - petChanceToMiss - petChanceToBeDodged; // V45
            float acidSpitChancesToMaintain = (float) Math.floor((acidSpitDuration - 1f) / acidSpitFrequency); // V46
            float acidSpitChanceToApplyFirst = acidSpitChancesToMaintain == 0 ? 0 : acidSpitChanceToApply; // V47
            float acidSpitChanceToStop = 1f - acidSpitChanceToApplyFirst; // AcidSpitChanceToStop
            float acidSpitAverageTimeToInc = acidSpitChanceToApplyFirst > 0 ? acidSpitFrequency : 0; // AcidSpitTimeToInc
            float acidSpitTimeSpentAtMax = acidSpitAverageTimeToInc + (acidSpitDuration * acidSpitChanceToStop); // AcidSpitAverageStackTime

            PetSkillStack[] stacks = new PetSkillStack[3];
            stacks[0] = new PetSkillStack();
            stacks[1] = new PetSkillStack();
            stacks[2] = new PetSkillStack();

            stacks[0].timeToReach = 0;
            stacks[1].timeToReach = acidSpitAverageTimeToInc * 1;
            stacks[2].timeToReach = acidSpitAverageTimeToInc * 2;

            stacks[0].chanceToMax = 0;
            stacks[1].chanceToMax = acidSpitChanceToStop == 1 ? 0 : acidSpitChanceToStop;
            stacks[2].chanceToMax = acidSpitChanceToStop == 1 ? 0 : 1 - (stacks[0].chanceToMax + stacks[1].chanceToMax);

            stacks[0].timeSpent = stacks[1].timeToReach;
            stacks[1].timeSpent = stacks[1].timeToReach == 0 ? 0 : acidSpitTimeSpentAtMax * (1 - stacks[0].chanceToMax);
            stacks[2].timeSpent = stacks[1].timeToReach == 0 ? 0 : acidSpitChanceToStop == 0 ? 1 : 1 / acidSpitChanceToStop * acidSpitCalcFreq * (1 - (stacks[0].chanceToMax + stacks[1].chanceToMax));

            float acidSpitTotalTime = stacks[0].timeSpent + stacks[1].timeSpent + stacks[2].timeSpent;

            stacks[0].percentTime = acidSpitTotalTime == 0 ? 1 : stacks[0].timeSpent / acidSpitTotalTime;
            stacks[1].percentTime = acidSpitTotalTime == 0 ? 1 : stacks[1].timeSpent / acidSpitTotalTime;
            stacks[2].percentTime = acidSpitTotalTime == 0 ? 1 : stacks[2].timeSpent / acidSpitTotalTime;

            stacks[0].total = stacks[0].percentTime * 0.0f;
            stacks[1].total = stacks[0].percentTime * 0.1f;
            stacks[2].total = stacks[0].percentTime * 0.2f;

            armorDebuffAcidSpit = stacks[0].total + stacks[1].total + stacks[2].total;
        }

        float armorDebuffSting = 0;
        float stingFrequency = priorityRotation.getSkillFrequency(PetAttacks.Sting);
        if (stingFrequency > 0) {
            float stingUseFreq = priorityRotation.getSkillFrequency(PetAttacks.Sting);
            float stingDuration = 20;
            float stingUptime = stingDuration > stingUseFreq ? 1 : stingDuration / stingUseFreq;

            armorDebuffSting = stingUptime * 0.05f;
        }

        // these local buffs can be overridden
        if (character.activeBuffsConflictingBuffContains("Sting")) {
            armorDebuffSporeCloud = 0;
            armorDebuffSting = 0;
        }
        if (character.activeBuffsConflictingBuffContains("Acid Spit")) {
            armorDebuffAcidSpit = 0;
        }

        calculatedStats.petArmorDebuffs = 0f - (1f - armorDebuffSporeCloud) * (1f - armorDebuffAcidSpit) * (1f - armorDebuffSting) + 1;

        // Hunter Effects
        // Ferocious Inspiraion
        // (Same as above)
        calculatedStats.ferociousInspirationDamageAdjust = 1;
        if (character.getHunterTalents().getFerociousInspiration() > 0) {
            if (calcOpts.petFamily != PetFamily.None) {
                float ferociousInspirationSpecialsEffect = priorityRotation.petSpecialFrequency == 0 ? 0 : 10f / priorityRotation.petSpecialFrequency;
                float ferociousInspirationUptime = 1f - (float) Math.pow(1f - calculatedStats.petCritTotalSpecials, (10f / attackSpeedEffective) + ferociousInspirationSpecialsEffect);
                float ferociousInspirationEffect = 0.01f * character.getHunterTalents().getFerociousInspiration();

                calculatedStats.ferociousInspirationDamageAdjust = 1.0f + ferociousInspirationUptime * ferociousInspirationEffect;
            }
        }

        // Roar of Recovery
        calculatedStats.manaRegenRoarOfRecovery = 0;
        float roarOfRecoveryFreq = priorityRotation.getSkillFrequency(PetAttacks.RoarOfRecovery);
        if (roarOfRecoveryFreq > 0) {
            float roarOfRecoveryUseCount = (float) Math.ceil(calcOpts.duration / roarOfRecoveryFreq);
            float roarOfRecoveryManaRestored = hunterStats.getMana() * 0.30f * roarOfRecoveryUseCount; // E129
            calculatedStats.manaRegenRoarOfRecovery = roarOfRecoveryUseCount > 0 ? roarOfRecoveryManaRestored / calcOpts.duration : 0;
        }

        //Invigoration
        float invigorationProcChance = talents.getInvigoration() * 0.50f; // C32
        if (invigorationProcChance > 0) {
            float invigorationProcFreq = (priorityRotation.petSpecialFrequency / calculatedStats.petCritTotalSpecials) / invigorationProcChance; //C35
            float invigorationEffect = talents.getInvigoration() > 0 ? 0.01f : 0;
            float invigorationManaGainedPercent = invigorationProcFreq > 0 ? 60f / invigorationProcFreq * invigorationEffect : 0; // C36
            float invigorationManaPerMinute = invigorationProcFreq > 0 ? 60f / invigorationProcFreq * invigorationEffect * hunterStats.getMana() : 0; // C37
            calculatedStats.manaRegenInvigoration = invigorationManaPerMinute / 60f;
        }

        // Target Armor Effect
        //31-10-2009 Drizz: added Armor effect
        double petEffectiveArmor = calcOpts.targetArmor * (1f - calculatedStats.petArmorDebuffs);
        calculatedStats.petTargetArmorReduction = StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.targetArmor, calculatedStats.petArmorDebuffs, 0, 0);
    }

    public void calculateDPS() {
        if (calcOpts.petFamily == PetFamily.None) return;

        int levelDifference = calcOpts.targetLevel - character.getLevel();

        // setup
        // Attack Power
        float damageBonusMeleeFromAP = 0.07f * petStats.getAttackPower(); // PetAPBonus
        float damageBonusSpellsFromAP = (1.5f / 35f) * petStats.getAttackPower(); // PetSpellScaling * PetAP

        // Spell Hit
        // Full Resists (Spell Hit)
        float levelResistFactor = 0.04f;
        if (levelDifference == 1) levelResistFactor = 0.05f;
        if (levelDifference == 2) levelResistFactor = 0.06f;
        if (levelDifference == 3) levelResistFactor = 0.08f;

        float fullResist = levelResistFactor - (1f - petChanceToSpellMiss);
        if (fullResist < 0) fullResist = 0;
        float fullResistDamageAdjust = 1f - fullResist;

        // Partial Resists (Spell Hit)
        float averageResist = (calcOpts.targetLevel - character.getLevel()) * 0.02f;
        float resist10 = 5.0f * averageResist;
        float resist20 = 2.5f * averageResist;
        float partialResistDamageAdjust = 1f - (resist10 * 0.1f + resist20 * 0.2f);

        // Damage Adjustments
        // Dodge adjust can't help us, only hinder
        float damageAdjustDodge = Math.min(1f, 1f - petChanceToBeDodged);

        float damageAdjustHitCritMelee = (2f * calculatedStats.petCritTotalMelee) + (1f - (petChanceToMiss + petChanceToBeDodged) - calculatedStats.petCritTotalMelee);
        float damageAdjustHitCritSpecials = (2f * calculatedStats.petCritTotalSpecials) + (1f - (petChanceToMiss + petChanceToBeDodged) - calculatedStats.petCritTotalSpecials); // CritAdjustments

        float damageAdjustFerociousInspiration = calculatedStats.ferociousInspirationDamageAdjust;
        float damageAdjustTargetDebuffs = calculatedStats.targetDebuffsPetDamage;
        float damageAdjustPetFamily = 1.05f;
        float damageAdjustMarkedForDeath = 1f + (talents.getMarkedForDeath() * 0.02f);
        float damageAdjustCobraReflexes = 1f - (petTalents.cobraReflexes.value * 0.075f); // this is a negative effect!

        // Feeding Frenzy
        float damageAdjustFeedingFrenzy = 1;
        if (petTalents.feedingFrenzy.value > 0) {
            float feedingFrenzyTimeSpent = calcOpts.timeSpentSub20 + calcOpts.timeSpent35To20;
            float feedingFrenzyUptime = feedingFrenzyTimeSpent > 0 ? feedingFrenzyTimeSpent / calcOpts.duration : 0;
            damageAdjustFeedingFrenzy = 1f + feedingFrenzyUptime * petTalents.feedingFrenzy.value * 0.08f;
        }

        // Glancing Blows
        float glancingBlowsSkillDiff = (calcOpts.targetLevel * 5) - (calcOpts.petLevel * 5); // F55
        if (glancingBlowsSkillDiff < 0) glancingBlowsSkillDiff = 0;
        float glancingBlowsChance = glancingBlowsSkillDiff > 15 ? 0.25f : 0.1f + glancingBlowsSkillDiff * 0.01f; // F56
        float glancingBlowsLowEnd = (float) Math.min(1.3f - 0.05f * glancingBlowsSkillDiff, 0.91f); // F57
        float glancingBlowsHighEnd = (float) Math.min(1.2f - 0.03f * glancingBlowsSkillDiff, 0.99f); // F58
        float damageAdjustGlancingBlows = 1 - (glancingBlowsChance * (1 - ((glancingBlowsLowEnd + glancingBlowsHighEnd) / 2)));

        // Savage Rend
        float damageAdjustSavageRend = 1;
        float savageRendFrequency = priorityRotation.getSkillFrequency(PetAttacks.SavageRend);
        if (savageRendFrequency > 0) {
            float savageRendTimeBetweenProcs = savageRendFrequency * (1f / calculatedStats.petCritTotalSpecials);
            float savageRendUptime = savageRendTimeBetweenProcs > 0 ? 30f / savageRendTimeBetweenProcs : 0;
            damageAdjustSavageRend = 1f + 0.1f * savageRendUptime;
        }

        // Monstrous Bite
        float damageAdjustMonstrousBite = 1;
        float monstrousBiteFrequency = priorityRotation.getSkillFrequency(PetAttacks.MonstrousBite);
        if (monstrousBiteFrequency > 0) {
            float monstrousBiteUseFreq = monstrousBiteFrequency;
            float monstrousBiteDuration = 12;

            float monstrousBiteChanceToApply = 1f - petChanceToMiss - petChanceToBeDodged;
            float monstrousBiteChancesToMaintain = 1;

            float monstrousBiteChanceToApplyFirst = monstrousBiteChancesToMaintain == 0 ? 0 : monstrousBiteChanceToApply;
            float monstrousBiteChanceToStop = 1f - monstrousBiteChanceToApplyFirst;

            float monstrousBiteAverageIncTime = 1f - monstrousBiteChanceToStop == 0 ? 0 : monstrousBiteUseFreq;
            float monstrousBiteAverageStackTime = monstrousBiteAverageIncTime + monstrousBiteDuration * monstrousBiteChanceToStop;

            PetSkillStack[] stacks = new PetSkillStack[4];
            stacks[0] = new PetSkillStack();
            stacks[1] = new PetSkillStack();
            stacks[2] = new PetSkillStack();
            stacks[3] = new PetSkillStack();

            stacks[0].timeToReach = 0;
            stacks[1].timeToReach = 1 * monstrousBiteAverageIncTime;
            stacks[2].timeToReach = 2 * monstrousBiteAverageIncTime;
            stacks[3].timeToReach = 3 * monstrousBiteAverageIncTime;

            stacks[0].chanceToMax = 0;
            stacks[1].chanceToMax = monstrousBiteChanceToStop == 1 ? 0 : monstrousBiteChanceToStop;
            stacks[2].chanceToMax = monstrousBiteChanceToStop == 1 ? 0 : monstrousBiteChanceToStop * (1 - stacks[1].chanceToMax);
            stacks[3].chanceToMax = monstrousBiteChanceToStop == 1 ? 0 : 1 - (stacks[0].chanceToMax + stacks[1].chanceToMax + stacks[2].chanceToMax);

            stacks[0].timeSpent = stacks[3].chanceToMax == 1 ? 0 : stacks[1].timeToReach;
            stacks[1].timeSpent = stacks[3].chanceToMax == 1 ? 0 : (stacks[1].timeToReach == 0 ? 0 : monstrousBiteAverageStackTime) * (1 - (stacks[0].chanceToMax));
            stacks[2].timeSpent = stacks[3].chanceToMax == 1 ? 0 : (stacks[2].timeToReach == 0 ? 0 : monstrousBiteAverageStackTime) * (1 - (stacks[0].chanceToMax + stacks[1].chanceToMax));
            stacks[3].timeSpent = monstrousBiteChanceToStop == 0 ? 1 : stacks[3].timeToReach == 0 ? 0 : 1 / monstrousBiteChanceToStop * monstrousBiteUseFreq * (1 - (stacks[0].chanceToMax + stacks[1].chanceToMax + stacks[2].chanceToMax));

            float monstrousBiteTotalTime = stacks[0].timeSpent + stacks[1].timeSpent + stacks[2].timeSpent + stacks[3].timeSpent;

            stacks[0].percentTime = monstrousBiteTotalTime == 0 ? 1 : stacks[0].timeSpent / monstrousBiteTotalTime;
            stacks[1].percentTime = monstrousBiteTotalTime == 0 ? 0 : stacks[1].timeSpent / monstrousBiteTotalTime;
            stacks[2].percentTime = monstrousBiteTotalTime == 0 ? 0 : stacks[2].timeSpent / monstrousBiteTotalTime;
            stacks[3].percentTime = monstrousBiteTotalTime == 0 ? 0 : stacks[3].timeSpent / monstrousBiteTotalTime;

            stacks[0].total = 0f;
            stacks[1].total = 0.03f * stacks[1].percentTime;
            stacks[2].total = 0.06f * stacks[1].percentTime;
            stacks[3].total = stacks[3].chanceToMax == 1f ? 0.09f : 0.09f * stacks[1].percentTime;

            float monstrousBiteProcEffect = stacks[0].total + stacks[1].total + stacks[2].total + stacks[3].total;

            damageAdjustMonstrousBite = 1f + monstrousBiteProcEffect;
        }
        float damageAdjustMangle = 1f + petStats.getBonusBleedDamageMultiplier();

        // Pets don't get ArP Ratings passed, spreadsheet agrees and no mention of it in the community
        float damageAdjustMitigation = 1f - StatConversion.getArmorDamageReduction(calcOpts.targetLevel, calcOpts.targetArmor, statsPetBuffs.getArmorPenetration(), 0f, 0f);

        float damageAdjustBase = 1f
                * (1f + petStats.getBonusDamageMultiplier())
                * (1f + petStats.getBonusPetDamageMultiplier())
                * damageAdjustFerociousInspiration
                * damageAdjustMonstrousBite
                * damageAdjustSavageRend
                * damageAdjustFeedingFrenzy
                * damageAdjustTargetDebuffs
                * damageAdjustPetFamily;

        float damageAdjustDots = damageAdjustBase; // BasePetModifier
        float damageAdjustWhite = damageAdjustBase * damageAdjustHitCritMelee;
        float damageAdjustMelee = damageAdjustBase * damageAdjustHitCritSpecials; // MeleeAttackAdjustment
        float damageAdjustSpecials = damageAdjustBase * damageAdjustHitCritSpecials * damageAdjustMarkedForDeath; // DamageAdjustment
        float damageAdjustMagic = damageAdjustBase * damageAdjustMarkedForDeath / damageAdjustTargetDebuffs; // MagicDamageAdjustments

        // damage
        // White Damage
        float whiteDamageBase = (52f + 78f) / 2f;
        float whiteDamageFromAP = (float) Math.floor(petStats.getAttackPower() / 14f * 2f);
        float whiteDamageNormal = whiteDamageBase + whiteDamageFromAP;
        float whiteDamageAdjust = damageAdjustWhite * damageAdjustCobraReflexes * damageAdjustMitigation * damageAdjustGlancingBlows;
        float whiteDamageReal = whiteDamageNormal * whiteDamageAdjust;

        float whiteDPS = whiteDamageReal / attackSpeedEffective;

        calculatedStats.petWhiteDPS = whiteDPS;

        // Priority Rotation
        // loop over each skill, figuring out the damage value
        for (PetSkillInstance S : priorityRotation.skills) {
            S.damage = 0;

            // Skill Groups
            if (S.skillData.type == PetSkillType.FocusDump) {
                float focusDumpDamageAverage = ((118f + 168f) / 2f) + damageBonusMeleeFromAP;
                S.damage = focusDumpDamageAverage * damageAdjustSpecials * damageAdjustMitigation;
            }
            if (S.skillData.type == PetSkillType.SpecialMelee) {
                float meleeDamageAverage = S.skillData.average + damageBonusMeleeFromAP;
                S.damage = meleeDamageAverage * damageAdjustSpecials * damageAdjustMitigation;
            }
            if (S.skillData.type == PetSkillType.SpecialSpell) {
                float spellDamageAverage = S.skillData.average + damageBonusSpellsFromAP;
                S.damage = spellDamageAverage * critSpecialsAdjust * fullResistDamageAdjust * partialResistDamageAdjust
                        * damageAdjustMagic * calculatedStats.targetDebuffsNature;
            }

            // Unique Skills
            if (S.skillType == PetAttacks.Rake) {
                float rakeDamageFromAP = petStats.getAttackPower() * 0.0175f;
                float rakeAverageDamage = ((47 + 67) / 2) + rakeDamageFromAP;
                float rakeAverageDamageDot = ((19 + 25) / 2) + rakeDamageFromAP;

                float rakeInitialHitDamage = rakeAverageDamage * damageAdjustSpecials * damageAdjustMangle; // spreadsheet doesn't add armor mitigation, appears to be wow bug?
                float rakeDotDamage = rakeAverageDamageDot * damageAdjustMangle * damageAdjustDots;
                float rakeDots = S.frequency > 9 ? 3 : 2;

                S.damage = rakeInitialHitDamage + rakeDotDamage * rakeDots;
            } else if (S.skillType == PetAttacks.FireBreath) {
                float fireBreathDamageAverage = ((43 + 57) / 2) + damageBonusSpellsFromAP;
                float fireBreathDamageInitial = fireBreathDamageAverage * fullResistDamageAdjust * partialResistDamageAdjust * damageAdjustMagic;
                float fireBreathDamageDot = 50 + damageBonusSpellsFromAP;
                S.damage = fireBreathDamageInitial + fireBreathDamageDot;
            } else if (S.skillType == PetAttacks.SavageRend) {
                float savageRendAverageHitDamage = ((59 + 83) / 2) + damageBonusMeleeFromAP;
                float savageRendAverageBleedDamage = ((21 + 27) / 2) + damageBonusMeleeFromAP;

                float savageRendHitDamage = savageRendAverageHitDamage * damageAdjustSpecials * damageAdjustMangle * damageAdjustMitigation;
                float savageRendBleedDamage = savageRendAverageBleedDamage * damageAdjustDots * damageAdjustMangle;

                S.damage = savageRendHitDamage + 3 * savageRendBleedDamage;
            } else if (S.skillType == PetAttacks.ScorpidPoison) {
                float scorpidPoisionDamageAverage = ((100 + 130) / 2) + damageBonusSpellsFromAP;
                float scorpidPoisionDamageNormal = scorpidPoisionDamageAverage * fullResistDamageAdjust * partialResistDamageAdjust
                        * damageAdjustMagic * calculatedStats.targetDebuffsNature;
                float scorpidPoisionDamageTick = scorpidPoisionDamageNormal / 5;
                float scorpidPoisionTicks = (float) Math.floor(S.cooldown / 2f);
                S.damage = scorpidPoisionDamageTick * scorpidPoisionTicks;
            } else if (S.skillType == PetAttacks.PoisonSpit) {
                float poisonSpitDamageAverage = ((104 + 136) / 2) + damageBonusSpellsFromAP;
                float poisonSpitDamageNormal = poisonSpitDamageAverage * fullResistDamageAdjust * partialResistDamageAdjust
                        * damageAdjustMagic * calculatedStats.targetDebuffsNature;
                float poisonSpitDamageTick = poisonSpitDamageNormal / 3;
                float poisonSpitTicks = S.cooldown >= 9 ? 3 : 2;
                S.damage = poisonSpitDamageTick * poisonSpitTicks;
            } else if (S.skillType == PetAttacks.VenomWebSpray) {
                float venomWebSprayDamageAverage = ((46 + 68) / 2) + damageBonusSpellsFromAP;
                float venomWebSprayDamageNormal = venomWebSprayDamageAverage * fullResistDamageAdjust * partialResistDamageAdjust
                        * damageAdjustMagic * calculatedStats.targetDebuffsNature;
                S.damage = venomWebSprayDamageNormal * 4;
            } else if (S.skillType == PetAttacks.SpiritStrike) {
                float spiritStrikeAdjust = fullResistDamageAdjust * partialResistDamageAdjust * damageAdjustMagic
                        * calculatedStats.targetDebuffsNature;

                float spiritStrikeInitialDamageAverage = ((49 + 65) / 2) + damageBonusSpellsFromAP;
                float spiritStrikeInitialDamageNormal = spiritStrikeInitialDamageAverage * spiritStrikeAdjust * damageAdjustHitCritSpecials;

                float spiritStrikeDotDamageAverage = ((49 + 65) / 2) + damageBonusSpellsFromAP;
                float spiritStrikeDotDamageNormal = spiritStrikeDotDamageAverage * spiritStrikeAdjust;

                S.damage = spiritStrikeInitialDamageNormal + spiritStrikeDotDamageNormal;
            } else if (S.skillType == PetAttacks.SporeCloud) {
                float spellDamageAverage = S.skillData.average + damageBonusSpellsFromAP;
                S.damage = spellDamageAverage * fullResistDamageAdjust * partialResistDamageAdjust
                        * damageAdjustMagic * calculatedStats.targetDebuffsNature;
            } else if (S.skillType == PetAttacks.AcidSpit) {
                float acidSpitAverage = S.skillData.average + damageBonusSpellsFromAP;
                S.damage = acidSpitAverage * (calculatedStats.petCritTotalSpecials * 1.5f + 1) * fullResistDamageAdjust * partialResistDamageAdjust * damageAdjustMagic * calculatedStats.targetDebuffsNature;
            }

            S.calculateDPS();
        }
        // now add everything up...
        priorityRotation.calculateDPS();

        // Kill Command

        float killCommandDPS = 0;

        if (killCommandCooldown > 0) {
            float killCommandDamagePerSpecial = priorityRotation.petSpecialFrequency > 0 ? priorityRotation.kcDps / (1 / priorityRotation.petSpecialFrequency) : 0;

            float killCommandBonusDamage = killCommandDamagePerSpecial * 1.2f;

            float killCommandDPSOverCooldown = killCommandCooldown > 0 ? killCommandBonusDamage / killCommandCooldown : 0;

            float killCommandFocusedFireCritBonus = character.getHunterTalents().getFocusedFire() * 0.1f;
            // 31-10-2009 Drizz: Remade the AdjustedBonus Calculation from the one below
            float killCommandAdjustedBonus = killCommandFocusedFireCritBonus
                    * (2f - 1f)
                    * (1f - calculatedStats.petTargetArmorReduction);

            killCommandDPS = killCommandDPSOverCooldown * (1f + killCommandAdjustedBonus);
        }

        calculatedStats.petKillCommandDPS = killCommandDPS;

        calculatedStats.petSpecialDPS = priorityRotation.dps;

        calculatedStats.setPetDpsPoints((float) (calculatedStats.petWhiteDPS
                + calculatedStats.petSpecialDPS
                + calculatedStats.petKillCommandDPS));
    }
}
