// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CharacterCalculationsHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the System.Windows/Skills.* properties (Whites, Explosive, Steady, ... Ready) are omitted:
// the ported calculation path never assigns them and the golden dump does not compare object members
// (they are dead members of the original without the Rotation/Skills calculator classes).
package rawr.models.hunter;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.models.hunter.HunterEnums.Shots;

import java.util.List;
import java.util.Map;

public class CharacterCalculationsHunter extends CharacterCalculationsBase {
    private float overallPoints = 0f;
    private float[] subPoints = new float[] { 0f, 0f, 0f, 0f };
    private Stats basicStats;
    private float baseAttackSpeed;
    private float autoshotDPS;
    private float bonusAttackProcsDPS;
    private float wildQuiverDPS;
    public float specProcDPS;
    private float customDPS;
    public Character character = null;
    public CalculationOptionsHunter calcOpts = null;
    public BossOptions bossOpts = null;

    private double piercingShotsDPS;
    private double piercingShotsDPSSteadyShot;
    private double piercingShotsDPSAimedShot;
    private double piercingShotsDPSChimeraShot;

    public float baseHealth;

    public PetCalculations pet;

    // Pet Stats
    public float petKillCommandDPS;
    public float petKillCommandMPS;
    public float petWhiteDPS;
    public float petSpecialDPS;
    public float petArmorDebuffs;
    public float petTargetArmorReduction;
    public float petTargetDodge;
    public float ferociousInspirationDamageAdjust;

    // Pet Hit
    public float petHitTotal;
    public float petHitSpellTotal;

    // Pet Crit
    public float petCritFromBase;
    public float petCritFromAgility;
    public float petCritFromSpidersBite;
    public float petCritFromFerocity;
    public float petCritFromGear;
    public float petCritFromBuffs;
    public float petCritFromTargetDebuffs;
    public float petCritFromDepression;
    public float petCritFromCobraStrikes;
    public float petCritTotalMelee;
    public float petCritTotalSpecials;

    // Pet AP
    public float petAPFromStrength;
    public float petAPFromHunterVsWild;
    public float petAPFromTier9;
    public float petAPFromBuffs;
    public float petAPFromHunterRAP;
    public float petAPFromRabidProc;
    public float petAPFromSerenityDust;
    public float petAPFromOutsideBuffs;
    public float petAPFromAnimalHandler;
    public float petAPFromAspectOfTheBeast;

    // Debuffs
    public float targetDebuffsCrit;
    public float targetDebuffsArmor;
    public float targetDebuffsNature;
    public float targetDebuffsPetDamage;

    // Haste Stats
    public float hasteFromTalentsStatic;
    public float hasteFromRapidFire;
    public float hasteFromProcs;
    public float hasteFromBase;
    public float hasteFromRating;
    public float hasteFromRangedBuffs;
    public float hasteFromRacial;
    public float hasteStaticTotal;
    public float hasteDynamicTotal;
    public float hasteEffectsTotal;

    // RAP Stats
    public float apTotal;
    public float apSelfBuffed;
    public float apFromBase;
    public float apFromAGI;
    public float apFromGear;

    // Hit Stats
    public float hitOverall;
    public float hitFromBase;
    public float hitFromRating;
    public float hitFromTalents;
    public float hitFromBuffs;
    public float hitFromTargetDebuffs;
    public float hitFromLevelAdjustment;

    // Crit Stats
    public float critRateOverall;
    public float critBase;
    public float critFromRacial;
    public float critFromAgi;
    public float critFromRating;
    public float critFromProcRating;
    public float critFromLethalShots;
    public float critFromKillerInstincts;
    public float critFromMasterMarksman;
    public float critFromMasterTactician;
    public float critFromBuffs;
    public float critFromDepression;

    // Shots Per Second
    public float shotsPerSecondCritting;

    public float damageReductionFromArmor;
    public float baseMana;

    // stuff for rotation test
    public float autoShotStaticSpeed;

    // new shots data
    public ShotData aimedShot = new ShotData(Shots.AimedShot, false, true, true);
    public ShotData arcaneShot = new ShotData(Shots.ArcaneShot, false, true, true);
    public ShotData multiShot = new ShotData(Shots.MultiShot, false, true, true);
    public ShotData serpentSting = new ShotData(Shots.SerpentSting, false, true, true);
    public ShotData scorpidSting = new ShotData(Shots.ScorpidSting, false, false, true);
    public ShotData viperSting = new ShotData(Shots.ViperSting, false, false, true);
    public ShotData silencingShot = new ShotData(Shots.SilencingShot, false, true, false);
    public ShotData steadyShot = new ShotData(Shots.SteadyShot, false, true, true);
    public ShotData killShot = new ShotData(Shots.KillShot, false, true, true);
    public ShotData explosiveShot = new ShotData(Shots.ExplosiveShot, false, true, true);
    public ShotData blackArrow = new ShotData(Shots.BlackArrow, false, false, true);
    public ShotData immolationTrap = new ShotData(Shots.ImmolationTrap, false, false, true);
    public ShotData explosiveTrap = new ShotData(Shots.ExplosiveTrap, false, false, true);
    public ShotData freezingTrap = new ShotData(Shots.FreezingTrap, true, false, true);
    public ShotData frostTrap = new ShotData(Shots.FrostTrap, true, false, true);
    public ShotData volley = new ShotData(Shots.Volley, false, true, true);
    public ShotData chimeraShot = new ShotData(Shots.ChimearaShot, false, true, true);
    public ShotData rapidFire = new ShotData(Shots.RapidFire, true, false, false);
    public ShotData readiness = new ShotData(Shots.Readiness, true, false, true);
    public ShotData bestialWrath = new ShotData(Shots.BestialWrath, true, false, false);

    public ShotPriority priorityRotation = null;

    // 091109 Drizz: Added
    public boolean collectSequence = false;
    public String sequence = "";

    // Mana Regen
    public float manaRegenGearBuffs;
    public float manaRegenConstantViper;
    public float manaRegenRoarOfRecovery;
    public float manaRegenRapidRecuperation;
    public float manaRegenChimeraViperProc;
    public float manaRegenInvigoration;
    public float manaRegenHuntingParty;
    public float manaRegenTargetDebuffs;
    public float manaRegenFromPots;
    public float manaRegenTotal;

    public float manaRegenViper;

    // Mana Usage
    public float manaUsageRotation;
    public float manaUsageKillCommand;
    public float manaUsageTotal;

    public float manaChangeDuringViper;
    public float manaChangeDuringNormal;
    public float manaTimeToFull;
    public float manaTimeToOOM;

    // Kill shot used sub-20%
    public float killShotSub20NewSteadyFreq;
    public float killShotSub20NewDPS;
    public float killShotSub20NewSteadyDPS;
    public float killShotSub20Gain;
    public float killShotSub20TimeSpent;
    public float killShotSub20FinalGain;

    // Aspect uptime/penalties/bonuses
    public float aspectUptimeHawk;
    public float aspectUptimeViper;
    public float aspectUptimeBeast;
    public float aspectBeastLostDPS;
    public float aspectViperPenalty;
    public float aspectBonusAPBeast;
    public float noManaDPSDownTimePerc;

    public float getBaseAttackSpeed() {
        return this.baseAttackSpeed;
    }

    public void setBaseAttackSpeed(float value) {
        this.baseAttackSpeed = value;
    }

    public float getAutoshotDPS() {
        return this.autoshotDPS;
    }

    public void setAutoshotDPS(float value) {
        this.autoshotDPS = value;
    }

    public float getBonusAttackProcsDPS() {
        return this.bonusAttackProcsDPS;
    }

    public void setBonusAttackProcsDPS(float value) {
        this.bonusAttackProcsDPS = value;
    }

    public float getWildQuiverDPS() {
        return this.wildQuiverDPS;
    }

    public void setWildQuiverDPS(float value) {
        this.wildQuiverDPS = value;
    }

    public double getPiercingShotsDPS() {
        return this.piercingShotsDPS;
    }

    public void setPiercingShotsDPS(double value) {
        this.piercingShotsDPS = value;
    }

    public double getPiercingShotsDPSSteadyShot() {
        return this.piercingShotsDPSSteadyShot;
    }

    public void setPiercingShotsDPSSteadyShot(double value) {
        this.piercingShotsDPSSteadyShot = value;
    }

    public double getPiercingShotsDPSAimedShot() {
        return this.piercingShotsDPSAimedShot;
    }

    public void setPiercingShotsDPSAimedShot(double value) {
        this.piercingShotsDPSAimedShot = value;
    }

    public double getPiercingShotsDPSChimeraShot() {
        return this.piercingShotsDPSChimeraShot;
    }

    public void setPiercingShotsDPSChimeraShot(double value) {
        this.piercingShotsDPSChimeraShot = value;
    }

    public float getCustomDPS() {
        return this.customDPS;
    }

    public void setCustomDPS(float value) {
        this.customDPS = value;
    }

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getHunterDpsPoints() {
        return this.subPoints[0];
    }

    public void setHunterDpsPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getPetDpsPoints() {
        return this.subPoints[1];
    }

    public void setPetDpsPoints(float value) {
        this.subPoints[1] = value;
    }

    public float getHunterSurvPoints() {
        return this.subPoints[2];
    }

    public void setHunterSurvPoints(float value) {
        this.subPoints[2] = value;
    }

    public float getPetSurvPoints() {
        return this.subPoints[3];
    }

    public void setPetSurvPoints(float value) {
        this.subPoints[3] = value;
    }

    public Stats getBasicStats() {
        return this.basicStats;
    }

    public void setBasicStats(Stats value) {
        this.basicStats = value;
    }

    public Stats averageStats;
    public Stats maximumStats;
    public Stats unbuffedStats;
    public Stats buffedStats;
    public Stats buffsStats; // The actual stats that come from Buffs

    public List<String> activeBuffs;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new java.util.LinkedHashMap<String, String>();
        calcOpts = (CalculationOptionsHunter) character.getCalculationOptions();

        // Basic Stats
        dictValues.put("Health and Stamina", HunterFormat.grouped(this.basicStats.getHealth()) + " : " + HunterFormat.grouped(this.basicStats.getStamina()) + "*"
                + HunterFormat.zeroPadGrouped(baseHealth, 5) + " : Base Health" + "\r\n"
                + HunterFormat.zeroPadGrouped(StatConversion.getHealthFromStamina(this.basicStats.getStamina()), 5) + " : Stam Bonus");
        dictValues.put("Mana", rawr.base.util.NetFormat.fixed(this.basicStats.getMana(), 0));
        dictValues.put("Armor", rawr.base.util.NetFormat.fixed(this.basicStats.getArmor(), 0));
        dictValues.put("Agility", rawr.base.util.NetFormat.fixed(this.basicStats.getAgility(), 0));
        dictValues.put("Ranged Attack Power", HunterFormat.zeroPad0(apTotal, 4) + "*Includes:"
                + "\r\n" + HunterFormat.zeroPad0(apFromBase, 4) + " : Base"
                + "\r\n" + HunterFormat.zeroPad0(apFromAGI, 4) + " : Agility"
                + "\r\n" + HunterFormat.zeroPad0(apFromGear, 4) + " : Gear"
                + "\r\nProcs were averaged out and added");
        dictValues.put("Intellect", rawr.base.util.NetFormat.fixed(this.basicStats.getIntellect(), 0));
        // old
        float hitPercent = StatConversion.getHitFromRating(this.basicStats.getHitRating());
        float hitPercBonus = this.basicStats.getPhysicalHit() - hitPercent;
        // Hit Soft Cap ratings check, how far from it
        float capA1 = StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.targetLevel - character.getLevel()];
        float convcapA1 = (float) Math.ceil(StatConversion.getRatingFromHit(capA1));
        float sec2lastNumA1 = (convcapA1 - StatConversion.getRatingFromHit(hitPercent) - StatConversion.getRatingFromHit(hitPercBonus)) * -1;
        dictValues.put("Hit",
                HunterFormat.percentCustom(StatConversion.getHitFromRating(this.basicStats.getHitRating()), 2, 2) + " : " + HunterFormat.netFloatToString(this.basicStats.getHitRating()) + "*"
                        + HunterFormat.percentCustom(hitPercBonus, 1, 2) + " : From Other Bonuses"
                        + System.lineSeparator() + HunterFormat.percentCustom(hitPercent + hitPercBonus, 1, 2) + " : Total Hit % Bonus"
                        + System.lineSeparator() + System.lineSeparator() + "Ranged Cap: "
                        + (sec2lastNumA1 > 0 ? "You can free " + rawr.base.util.NetFormat.fixed(sec2lastNumA1, 0) + " Rating"
                                : "You need " + rawr.base.util.NetFormat.fixed(sec2lastNumA1 * -1, 0) + " more Rating"));
        dictValues.put("Crit", HunterFormat.percentCustom(critRateOverall, 2, 2) + " : " + HunterFormat.netFloatToString(this.basicStats.getCritRating()) + "*Includes:"
                + "\r\n" + HunterFormat.percentCustom(critBase, 2, 2) + " : Base Crit"
                + "\r\n" + HunterFormat.percentCustom(critFromAgi, 2, 2) + " : Agility"
                + "\r\n" + HunterFormat.percentCustom(critFromRating, 2, 2) + " : Rating"
                + "\r\n" + HunterFormat.percentCustom(critFromRacial, 2, 2) + " : Racial"
                + "\r\n" + HunterFormat.percentCustom(critFromProcRating, 2, 2) + " : Proc Effects"
                + "\r\n" + HunterFormat.percentCustom(critFromLethalShots, 2, 2) + " : Lethal Shots"
                + "\r\n" + HunterFormat.percentCustom(critFromKillerInstincts, 2, 2) + " : Killer Instincts"
                + "\r\n" + HunterFormat.percentCustom(critFromMasterMarksman, 2, 2) + " : Master Marksman"
                + "\r\n" + HunterFormat.percentCustom(critFromMasterTactician, 2, 2) + " : Master Tactician"
                + "\r\n" + HunterFormat.percentCustom(critFromBuffs, 2, 2) + " : Buffs & Debuffs"
                + "\r\n" + HunterFormat.percentCustom(critFromDepression * -1f, 2, 2) + " : Level Adjustment"
                + "\r\n\r\nNote that individual Shots will handle their own crit caps");
        dictValues.put("Armor Penetration", HunterFormat.percentCustom(StatConversion.getArmorPenetrationFromRating(this.basicStats.getArmorPenetrationRating()), 2, 2) + " : " + HunterFormat.netFloatToString(this.basicStats.getArmorPenetrationRating()) + "*Enemy's Damage Reduction from armor: " + HunterFormat.percentCustom(damageReductionFromArmor, 2, 2));
        dictValues.put("Haste", HunterFormat.percentCustom(this.basicStats.getPhysicalHaste(), 2, 2) + " : " + rawr.base.util.NetFormat.fixed(this.basicStats.getHasteRating(), 0) + "*Includes:"
                + "\r\n" + HunterFormat.percentCustom(hasteFromBase, 2, 2) + " : Base"
                + "\r\n" + HunterFormat.percentCustom(hasteFromRating, 2, 2) + " : Rating"
                + "\r\n" + HunterFormat.percentCustom(hasteFromTalentsStatic, 2, 2) + " : Serpent's Swiftness"
                + "\r\n" + HunterFormat.percentCustom(hasteFromRangedBuffs, 2, 2) + " : Buffs"
                + "\r\n" + HunterFormat.percentCustom(hasteFromRapidFire, 2, 2) + " : Rapid Fire"
                + "\r\n" + HunterFormat.percentCustom(hasteFromProcs, 2, 2) + " : Proc Effects");
        dictValues.put("Attack Speed", rawr.base.util.NetFormat.fixed(getBaseAttackSpeed(), 2));

        // Pet Stats
        dictValues.put("Pet Attack Power", rawr.base.util.NetFormat.fixed(pet.petStats.getAttackPower(), 0)
                + "*Full Pet Stats:\r\n"
                + "Strength: " + rawr.base.util.NetFormat.fixed(pet.petStats.getStrength(), 1) + "\r\n"
                + "Agility: " + rawr.base.util.NetFormat.fixed(pet.petStats.getAgility(), 1) + "\r\n"
                + "Hit: " + HunterFormat.percentCustom(pet.petStats.getPhysicalHit(), 1, 2) + "\r\n"
                + "PhysCrit: " + HunterFormat.percentCustom(pet.petStats.getPhysicalCrit(), 1, 2) + "\r\n"
                + "PhysHaste: " + HunterFormat.percentCustom(pet.petStats.getPhysicalHaste(), 1, 2) + "\r\n");
        dictValues.put("Pet Hit %", HunterFormat.p2(petHitTotal));
        dictValues.put("Pet Dodge %", HunterFormat.p2(petTargetDodge));
        dictValues.put("Pet Melee Crit %", HunterFormat.p2(petCritTotalMelee) + "*includes:\n"
                + HunterFormat.p2(petCritFromBase) + " from base\n"
                + HunterFormat.p2(petCritFromAgility) + " from agility\n"
                + HunterFormat.p2(petCritFromSpidersBite) + " from Spider's Bite\n"
                + HunterFormat.p2(petCritFromFerocity) + " from Ferocity\n"
                + HunterFormat.p2(petCritFromGear) + " from gear\n"
                + HunterFormat.p2(petCritFromBuffs) + " from buffs\n"
                + HunterFormat.p2(petCritFromTargetDebuffs) + " from target debuffs\n"
                + HunterFormat.p2(petCritFromDepression) + " from depression");
        dictValues.put("Pet Specials Crit %", HunterFormat.p2(petCritTotalSpecials) + "*includes:\n"
                + HunterFormat.p2(petCritTotalMelee) + " from melee crit\n"
                + HunterFormat.p2(petCritFromCobraStrikes) + " from Cobra Strikes");
        dictValues.put("Pet White DPS", rawr.base.util.NetFormat.fixed(petWhiteDPS, 2));
        dictValues.put("Pet Kill Command DPS", rawr.base.util.NetFormat.fixed(petKillCommandDPS, 2));
        dictValues.put("Pet Specials DPS", rawr.base.util.NetFormat.fixed(petSpecialDPS, 2));

        // Shot Stats
        dictValues.put("Aimed Shot", aimedShot.genTooltip());
        dictValues.put("Arcane Shot", arcaneShot.genTooltip());
        dictValues.put("Multi Shot", multiShot.genTooltip());
        dictValues.put("Silencing Shot", silencingShot.genTooltip());
        dictValues.put("Steady Shot", steadyShot.genTooltip());
        dictValues.put("Kill Shot", killShot.genTooltip());
        dictValues.put("Explosive Shot", explosiveShot.genTooltip());
        dictValues.put("Black Arrow", blackArrow.genTooltip());
        dictValues.put("Volley", volley.genTooltip());
        dictValues.put("Chimera Shot", chimeraShot.genTooltip());

        // Sting Stats
        dictValues.put("Serpent Sting", serpentSting.genTooltip());
        dictValues.put("Scorpid Sting", scorpidSting.genTooltip());
        dictValues.put("Viper Sting", viperSting.genTooltip());

        // Trap Stats
        dictValues.put("Immolation Trap", immolationTrap.genTooltip());
        dictValues.put("Explosive Trap", explosiveTrap.genTooltip());
        dictValues.put("Freezing Trap", freezingTrap.genTooltip());
        dictValues.put("Frost Trap", frostTrap.genTooltip());

        // Mana
        dictValues.put("Mana Usage Per Second", rawr.base.util.NetFormat.fixed(manaUsageTotal, 2) + "*includes:\n"
                + rawr.base.util.NetFormat.fixed(manaUsageRotation, 2) + " from shot rotation\n"
                + rawr.base.util.NetFormat.fixed(manaUsageKillCommand, 2) + " from Kill Command");
        dictValues.put("Mana Regen Per Second", rawr.base.util.NetFormat.fixed(manaRegenTotal, 2) + "*includes:\n"
                + ((manaRegenGearBuffs + manaRegenConstantViper + manaRegenViper + manaRegenRoarOfRecovery
                        + manaRegenRapidRecuperation + manaRegenChimeraViperProc + manaRegenInvigoration
                        + manaRegenHuntingParty + manaRegenTargetDebuffs + manaRegenFromPots > 0f) ?
                        (manaRegenGearBuffs != 0 ? rawr.base.util.NetFormat.fixed(manaRegenGearBuffs, 2) + " from Gear and Buffs\n" : "")
                        + (manaRegenConstantViper != 0 ? rawr.base.util.NetFormat.fixed(manaRegenConstantViper, 2) + " from Constant Aspect of the Viper\n" : "")
                        + (manaRegenViper != 0 ? rawr.base.util.NetFormat.fixed(manaRegenViper, 2) + " from Aspect of the Viper\n" : "")
                        + (manaRegenRoarOfRecovery != 0 ? rawr.base.util.NetFormat.fixed(manaRegenRoarOfRecovery, 2) + " from Roar of Recovery\n" : "")
                        + (manaRegenRapidRecuperation != 0 ? rawr.base.util.NetFormat.fixed(manaRegenRapidRecuperation, 2) + " from Rapid Recuperation\n" : "")
                        + (manaRegenChimeraViperProc != 0 ? rawr.base.util.NetFormat.fixed(manaRegenChimeraViperProc, 2) + " from Chimera Viper String Proc\n" : "")
                        + (manaRegenInvigoration != 0 ? rawr.base.util.NetFormat.fixed(manaRegenInvigoration, 2) + " from Invigoration\n" : "")
                        + (manaRegenHuntingParty != 0 ? rawr.base.util.NetFormat.fixed(manaRegenHuntingParty, 2) + " from Hunting Party\n" : "")
                        + (manaRegenTargetDebuffs != 0 ? rawr.base.util.NetFormat.fixed(manaRegenTargetDebuffs, 2) + " from Target Debuffs\n" : "")
                        + (manaRegenFromPots != 0 ? rawr.base.util.NetFormat.fixed(manaRegenFromPots, 2) + " from Pots" : "")
                        : "Nothing to add"));
        dictValues.put("Normal Change", rawr.base.util.NetFormat.fixed(manaChangeDuringNormal, 2));
        dictValues.put("Change during Viper", rawr.base.util.NetFormat.fixed(manaChangeDuringViper, 2));
        dictValues.put("Time to OOM", rawr.base.util.NetFormat.fixed(manaTimeToOOM, 2));
        dictValues.put("Time to Full", rawr.base.util.NetFormat.fixed(manaTimeToFull, 2));
        dictValues.put("Viper Damage Penalty", HunterFormat.p2(aspectViperPenalty));
        dictValues.put("Viper Uptime", HunterFormat.p2(aspectUptimeViper));
        dictValues.put("No Mana Damage Penalty", HunterFormat.p2(noManaDPSDownTimePerc));

        // Hunter DPS
        dictValues.put("Autoshot DPS", rawr.base.util.NetFormat.fixed(getAutoshotDPS(), 2));
        dictValues.put("Priority Rotation DPS", rawr.base.util.NetFormat.fixed(getCustomDPS(), 2));
        dictValues.put("Wild Quiver DPS", rawr.base.util.NetFormat.fixed(getWildQuiverDPS(), 2));
        dictValues.put("Kill Shot low HP gain", rawr.base.util.NetFormat.fixed(killShotSub20FinalGain, 2) + "*"
                + "Kill Shot freq: " + rawr.base.util.NetFormat.fixed(killShot.freq, 2) + " -> " + rawr.base.util.NetFormat.fixed(killShot.startFreq, 2) + "\n"
                + "Steady Shot freq: " + rawr.base.util.NetFormat.fixed(steadyShot.freq, 2) + " -> " + rawr.base.util.NetFormat.fixed(killShotSub20NewSteadyFreq, 2) + "\n"
                + "Kill Shot DPS: " + rawr.base.util.NetFormat.fixed(killShot.dps, 2) + " -> " + rawr.base.util.NetFormat.fixed(killShotSub20NewDPS, 2) + "\n"
                + "Steady Shot DPS: " + rawr.base.util.NetFormat.fixed(steadyShot.dps, 2) + " -> " + rawr.base.util.NetFormat.fixed(killShotSub20NewSteadyDPS, 2) + "\n"
                + "DPS Gain when switched: " + rawr.base.util.NetFormat.fixed(killShotSub20Gain, 2) + "\n"
                + "Time spent sub-20%: " + HunterFormat.p2(killShotSub20TimeSpent));
        dictValues.put("Aspect Loss", rawr.base.util.NetFormat.fixed(aspectBeastLostDPS, 2) + "*"
                + "Hawk Uptime: " + HunterFormat.p2(aspectUptimeHawk) + "\n"
                + "Viper Uptime: " + HunterFormat.p2(aspectUptimeViper) + "\n"
                + "Beast Uptime: " + HunterFormat.p2(aspectUptimeBeast));
        dictValues.put("Piercing Shots DPS", rawr.models.mage.NetFormat.d2(getPiercingShotsDPS()) + "*"
                + "Steady Shot: " + rawr.models.mage.NetFormat.d2(getPiercingShotsDPSSteadyShot()) + "\n"
                + "Aimed Shot: " + rawr.models.mage.NetFormat.d2(getPiercingShotsDPSAimedShot()) + "\n"
                + "Chimera Shot: " + rawr.models.mage.NetFormat.d2(getPiercingShotsDPSChimeraShot()) + "\n");
        dictValues.put("Special DMG Procs DPS", rawr.base.util.NetFormat.fixed(specProcDPS, 2));

        // Combined DPS
        String zod = (getBonusAttackProcsDPS() != 0 ? "*Includes:\r\nZod's Proc: " + rawr.base.util.NetFormat.fixed(getBonusAttackProcsDPS(), 1) : "");
        dictValues.put("Hunter DPS", rawr.base.util.NetFormat.fixed(getHunterDpsPoints(), 2) + zod);
        dictValues.put("Pet DPS", rawr.base.util.NetFormat.fixed(getPetDpsPoints(), 2));
        dictValues.put("Total DPS", rawr.base.util.NetFormat.fixed(getOverallPoints(), 2));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return this.basicStats.getHealth();
            case "Mana": return this.basicStats.getMana();
            case "Agility": return this.basicStats.getAgility();
            case "Crit %": return this.basicStats.getPhysicalCrit() * 100f;
            case "Haste %": return this.basicStats.getPhysicalHaste() * 100f;
            case "Attack Power": return this.basicStats.getAttackPower();
            case "Armor Penetration %": return this.basicStats.getArmorPenetration() * 100f;
            case "% Chance to Miss (Yellow)": return (StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.targetLevel - character.getLevel()] - this.basicStats.getPhysicalHit()) * 100f;
            default: return 0;
        }
    }
}
