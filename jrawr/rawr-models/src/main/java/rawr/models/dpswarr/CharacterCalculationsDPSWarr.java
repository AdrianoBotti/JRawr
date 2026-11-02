// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/CharacterCalculationsDPSWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// C# auto-properties are public fields (the golden test resolves the C# names case-insensitively);
// TotalDPS/Survivability keep their property semantics as getters/setters over subPoints. The \r\n
// literals in the display strings are the original literals (only Environment.NewLine -> "\n").
package rawr.models.dpswarr;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.util.NetFormat;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsDPSWarr extends CharacterCalculationsBase {
    public Stats averageStats;
    public Stats maximumStats;
    public Stats unbuffedStats;
    public Stats buffedStats;
    public Stats buffsStats; // The actual stats that come from Buffs
    public CombatFactors combatFactors;
    public Rotation rot;

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getTotalDPS() {
        return this.subPoints[0];
    }

    public void setTotalDPS(float value) {
        this.subPoints[0] = value;
    }

    public float totalHPS;

    public float getSurvivability() {
        return this.subPoints[1];
    }

    public void setSurvivability(float value) {
        this.subPoints[1] = value;
    }

    public int targetLevel;
    public float duration;
    public String floorstring;

    // Attack Table
    public float miss;
    public float hitRating;
    public float expertiseRating;
    public float expertise;
    public float mhExpertise;
    public float ohExpertise;
    public float weapMastPerc;
    public float agilityCritBonus;
    public float critRating;
    public float critPercent;
    public float mhCrit;
    public float ohCrit;

    // Offensive
    public float bonusCritPercPoleAxeSpec;
    public float armorPenetrationMaceSpec;
    public float armorPenetrationStance;
    public float armorPenetrationRating;
    public float armorPenetrationRating2Perc;
    public float armorPenetration;
    public float maxArmorPenetration;
    public float buffedArmorPenetration;
    public float hasteRating;
    public float hastePercent;
    public float weaponSpeed;
    public float teethBonus;

    // DPS
    public WhiteAttacks whites;
    public float whiteDPS;
    public float whiteDmg;
    public float whiteDPSMH;
    public float whiteDPSOH;
    public float specProcDPS;
    public float totalDamagePerSecond;

    // Markov Work
    public FakeWhite fw;

    // Neutral
    public float baseHealth;
    public float whiteRage;
    public float otherRage;
    public float needyRage;
    public float freeRage;
    public float stamina;
    public float health;

    // Defensive
    public float armor;
    public float critReduction;
    public float armorReduction;
    public float guaranteedReduction;
    public float damageReduc;
    public float missedAttacks;
    public float avoidedAttacks;
    public float dodgedAttacks;
    public float parriedAttacks;
    public float blockedAttacks;

    private String genFormattedString(String[] passiveContrs) {
        return genFormattedString(passiveContrs, false, false, false);
    }

    private String genFormattedString(String[] passiveContrs, boolean hasCap, boolean unprocisOverCap, boolean procisOverCap) {
        int formIter = 2;
        String theFormat = "";

        theFormat += "{0:00.00%} : {1}*";
        theFormat += "The Pane shows Averaged Values";
        theFormat += "\r\n";
        if (passiveContrs.length > 0) {
            theFormat += "\r\n= Your Passive Contributions =";
            for (String passiveContr : passiveContrs) {
                theFormat += "\r\n{" + formIter + ":00.00%} : " + passiveContr;
                formIter++;
            }
            theFormat += "\r\n";
        }
        theFormat += "\r\n= UnProc'd =";
        theFormat += "\r\n{" + formIter + ":0.#} : Rating";
        formIter++;
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n" + (unprocisOverCap ? "{" + formIter + ":0.#} Rating Over Cap"
                    : "{" + formIter + ":0.#} Rating Under Cap");
            formIter++;
        }
        theFormat += "\r\n";
        theFormat += "\r\n= Proc'd =";
        theFormat += "\r\n{" + formIter + ":0.#} : Rating";
        formIter++;
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n" + (procisOverCap ? "{" + formIter + ":0.#} Rating Over Cap"
                    : "{" + formIter + ":0.#} Rating Under Cap");
            formIter++;
        }

        return theFormat;
    }

    private String genFormattedString(String[] passiveContrs, boolean hasCap,
            boolean whunprocisOverCap, boolean whprocisOverCap,
            boolean ywunprocisOverCap, boolean ywprocisOverCap) {
        int formIter = 2;
        String theFormat = "";

        theFormat += "{0:00.00%} : {1}*";
        theFormat += "The Pane shows Averaged Values";
        theFormat += "\r\n";
        if (passiveContrs.length > 0) {
            theFormat += "\r\n= Your Passive Contributions =";
            for (String passiveContr : passiveContrs) {
                theFormat += "\r\n{" + formIter + ":00.00%} : " + passiveContr;
                formIter++;
            }
            theFormat += "\r\n";
        }
        theFormat += "\r\n= UnProc'd =";
        theFormat += "\r\n{" + formIter + ":0.#} : Rating";
        formIter++;
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n{" + formIter + ":0.#} Rating "
                    + (whunprocisOverCap ? "Over White Cap" : "Under White Cap");
            formIter++;
        }
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n{" + formIter + ":0.#} Rating "
                    + (ywunprocisOverCap ? "Over Yellow Cap" : "Under Yellow Cap");
            formIter++;
        }
        theFormat += "\r\n";
        theFormat += "\r\n= Proc'd =";
        theFormat += "\r\n{" + formIter + ":0.#} : Rating";
        formIter++;
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n{" + formIter + ":0.#} Rating "
                    + (whprocisOverCap ? "Over White Cap" : "Under White Cap");
            formIter++;
        }
        theFormat += "\r\n{" + formIter + ":00.00%} : Percent";
        formIter++;
        if (hasCap) {
            theFormat += "\r\n{" + formIter + ":0.#} Rating "
                    + (ywprocisOverCap ? "Over Yellow Cap" : "Under Yellow Cap");
            formIter++;
        }

        return theFormat;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        String format;
        int LevelDif = combatFactors.levelDif();

        // Base Stats
        dictValues.put("Health and Stamina", DPSWarrFormat.format("{0:##,##0} : {1:##,##0}*" +
                        "{2:00,000} : Base Health" +
                        "\r\n{3:00,000} : Stam Bonus",
                averageStats.getHealth(), averageStats.getStamina(),
                baseHealth,
                StatConversion.getHealthFromStamina(averageStats.getStamina())));
        dictValues.put("Armor", DPSWarrFormat.format("{0}*Increases Attack Power by {1}", armor, teethBonus));
        // Strength
        {
            int formIter = 1;
            String theFormat = "";

            float[] passiveContrsVals = new float[] {
                    combatFactors.char_.getWarriorTalents().getStrengthOfArms() * 0.02f,
                    (combatFactors.furyStance() ? combatFactors.char_.getWarriorTalents().getImprovedBerserkerStance() * 0.04f : 0f),
                    buffsStats.getStrength(),
                    buffsStats.getBonusStrengthMultiplier(),
            };

            String[] passiveContrs = new String[] {
                    "Strength of Arms",
                    "Improved Berserker Stance",
                    "Buffs : Simple",
                    "Buffs : Multi",
            };

            theFormat += "{0:0.#}*";
            theFormat += "The Pane shows Averaged Values";
            theFormat += "\r\n";
            theFormat += "\r\n= Your Passive Contributions =";
            theFormat += "\r\n{" + formIter + ":00.#%} : " + passiveContrs[0];
            formIter++;
            theFormat += "\r\n{" + formIter + ":00.#%} : " + passiveContrs[1];
            formIter++;
            theFormat += "\r\n{" + formIter + ":0.#} : " + passiveContrs[2];
            formIter++;
            theFormat += "\r\n{" + formIter + ":00.#%} : " + passiveContrs[3];
            formIter++;
            theFormat += "\r\n";
            theFormat += "\r\n= UnProc'd =";
            theFormat += "\r\nValue: {" + formIter + ":0.#}";
            formIter++;
            theFormat += "\r\nIncreases Attack Power by {" + formIter + ":0.#}";
            formIter++;
            theFormat += "\r\n";
            theFormat += "\r\n= Proc'd =";
            theFormat += "\r\nValue: {" + formIter + ":0.#}";
            formIter++;
            theFormat += "\r\nIncreases Attack Power by {" + formIter + ":0.#}";

            dictValues.put("Strength", DPSWarrFormat.format(theFormat,
                    averageStats.getStrength(),
                    passiveContrsVals[0], passiveContrsVals[1], passiveContrsVals[2], passiveContrsVals[3],
                    buffedStats.getStrength(),
                    buffedStats.getStrength() * 2f,
                    maximumStats.getStrength(),
                    maximumStats.getStrength() * 2f
            ));
        }
        dictValues.put("Attack Power", DPSWarrFormat.format("{0}*" +
                        "Increases White DPS by {1:0.0}\r\n" +
                        "\r\n" +
                        "Buffed: {2:0}\r\n" +
                        "Proc'd: {3:0}\r\n", (int) averageStats.getAttackPower(), averageStats.getAttackPower() / 14f,
                buffedStats.getAttackPower(), maximumStats.getAttackPower()));
        // Agility
        {
            int formIter = 1;
            String theFormat = "";

            float[] passiveContrsVals = new float[] {
                    buffsStats.getAgility(),
                    buffsStats.getBonusAgilityMultiplier(),
            };

            String[] passiveContrs = new String[] { "Buffs : Simple", "Buffs : Multi" };

            theFormat += "{0:0.#}*";
            theFormat += "The Pane shows Averaged Values";
            theFormat += "\r\n";
            theFormat += "\r\n= Your Passive Contributions =";
            theFormat += "\r\n{" + formIter + ":0.#} : " + passiveContrs[0];
            formIter++;
            theFormat += "\r\n{" + formIter + ":00.#%} : " + passiveContrs[1];
            formIter++;
            theFormat += "\r\n";
            theFormat += "\r\n= UnProc'd =";
            theFormat += "\r\nIncreases Crit by {" + formIter + ":0.#%}";
            formIter++;
            theFormat += "\r\nIncreases Armor by {" + formIter + ":0.#}";
            formIter++;
            theFormat += "\r\n";
            theFormat += "\r\n= Proc'd =";
            theFormat += "\r\nIncreases Crit by {" + formIter + ":0.#%}";
            formIter++;
            theFormat += "\r\nIncreases Armor by {" + formIter + ":0.#}";

            dictValues.put("Agility", DPSWarrFormat.format(theFormat,
                    averageStats.getAgility(),
                    passiveContrsVals[0], passiveContrsVals[1],
                    StatConversion.getCritFromAgility(buffedStats.getAgility(), CharacterClass.Warrior),
                    StatConversion.getArmorFromAgility(buffedStats.getAgility()),
                    StatConversion.getCritFromAgility(maximumStats.getAgility(), CharacterClass.Warrior),
                    StatConversion.getArmorFromAgility(maximumStats.getAgility())
            ));
        }
        // Crit
        {
            float WhCritCap = 1f;
            if (combatFactors.useOH) {
                WhCritCap -= (whites.ohAtkTable.glance + whites.ohAtkTable.anyNotLand());
            } else {
                WhCritCap -= (whites.mhAtkTable.glance + whites.mhAtkTable.anyNotLand());
            }
            float useRamp = 0f;
            if (combatFactors.char_.getWarriorTalents().getRampage() > 0f) useRamp = 0.05f;
            float[] passiveContrsVals = new float[] {
                    0.03192f,
                    agilityCritBonus,
                    StatConversion.getCritFromRating(buffedStats.getCritRating() + buffedStats.getDeathbringerProc(), CharacterClass.Warrior),
                    combatFactors.char_.getWarriorTalents().getCruelty() * 0.01f,
                    (combatFactors.furyStance() ? 0.03f : 0f),
                    (combatFactors.furyStance() ? averageStats.getBonusWarrior_T9_2P_Crit() : 0f),
                    bonusCritPercPoleAxeSpec,
                    buffsStats.getPhysicalCrit() + useRamp,
            };
            float passiveContrsTtlVal = passiveContrsVals[0] + passiveContrsVals[1] + passiveContrsVals[2]
                    + passiveContrsVals[3] + passiveContrsVals[4] + passiveContrsVals[5]
                    + passiveContrsVals[6] + passiveContrsVals[7];
            String[] passiveContrs = new String[] { "Base Crit", "From Agility", "From Crit Rating", "Cruelty",
                    "Berserker Stance", "T9 2P Set Bonus", "Poleaxe Specialization",
                    "Buffs" };

            float WhProcdCrit = StatConversion.getCritFromRating(maximumStats.getCritRating() + maximumStats.getDeathbringerProc() - buffedStats.getCritRating() - buffedStats.getDeathbringerProc(), CharacterClass.Warrior);
            boolean isWhUnProcdOverCap = passiveContrsTtlVal > WhCritCap;
            boolean isWhProcdOverCap = passiveContrsTtlVal + WhProcdCrit > WhCritCap;
            float amountWhUnProcdOverCap = Math.abs(StatConversion.getRatingFromCrit(WhCritCap - passiveContrsTtlVal, CharacterClass.Warrior));
            float amountWhProcdOverCap = Math.abs(StatConversion.getRatingFromCrit(WhCritCap - (passiveContrsTtlVal + WhProcdCrit), CharacterClass.Warrior));

            String theFormat = genFormattedString(passiveContrs, true,
                    isWhUnProcdOverCap, isWhProcdOverCap);

            dictValues.put("Crit", DPSWarrFormat.format(theFormat,
                    critPercent, averageStats.getCritRating(),
                    passiveContrsVals[0], passiveContrsVals[1], passiveContrsVals[2],
                    passiveContrsVals[3], passiveContrsVals[4], passiveContrsVals[5],
                    passiveContrsVals[6], passiveContrsVals[7],
                    buffedStats.getCritRating() + buffedStats.getDeathbringerProc(),
                    Math.min(WhCritCap, passiveContrsTtlVal), amountWhUnProcdOverCap,
                    maximumStats.getCritRating() + maximumStats.getDeathbringerProc(),
                    Math.min(WhCritCap, passiveContrsTtlVal + WhProcdCrit), amountWhProcdOverCap
            ));
        }
        // Armor Penetration
        {
            float ArPCap = 1.00f;
            float[] passiveContrsVals = new float[] {
                    (!combatFactors.furyStance() ? 0.10f : 0f),
                    (!combatFactors.furyStance() ? averageStats.getBonusWarrior_T9_2P_ArP() : 0f),
                    armorPenetrationMaceSpec
            };
            float passiveContrsTtlVal = passiveContrsVals[0] + passiveContrsVals[1] + passiveContrsVals[2];
            String[] passiveContrs = new String[] { "Battle Stance", "T9 2P Set Bonus", "Mace Specialization" };
            float UnProcdArP = StatConversion.getArmorPenetrationFromRating(buffedStats.getArmorPenetrationRating());
            float ProcdArP = StatConversion.getArmorPenetrationFromRating(maximumStats.getArmorPenetrationRating());
            boolean isUnProcdOverCap = passiveContrsTtlVal + UnProcdArP > ArPCap;
            boolean isProcdOverCap = passiveContrsTtlVal + ProcdArP > ArPCap;
            float amountUnProcdOverCap = Math.abs(StatConversion.getRatingFromArmorPenetration(ArPCap - (passiveContrsTtlVal + UnProcdArP)));
            float amountProcdOverCap = Math.abs(StatConversion.getRatingFromArmorPenetration(ArPCap - (passiveContrsTtlVal + ProcdArP)));
            String theFormat = genFormattedString(passiveContrs, true, isUnProcdOverCap, isProcdOverCap);
            dictValues.put("Armor Penetration", DPSWarrFormat.format(theFormat,
                    armorPenetration, averageStats.getArmorPenetrationRating(),
                    passiveContrsVals[0], passiveContrsVals[1], passiveContrsVals[2],
                    buffedStats.getArmorPenetrationRating(),
                    Math.min(ArPCap, passiveContrsTtlVal + UnProcdArP),
                    amountUnProcdOverCap,
                    maximumStats.getArmorPenetrationRating(),
                    Math.min(ArPCap, passiveContrsTtlVal + ProcdArP),
                    amountProcdOverCap
            ));
        }
        // Haste
        {
            float heroism = 0f;
            if (buffsStats.getRawSpecialEffectData() != null) {
                for (SpecialEffect effect : buffsStats.specialEffects()) {
                    if (effect != null && effect.getStats().getPhysicalHaste() > 0) {
                        heroism = effect.getAverageStats().getPhysicalHaste();
                    }
                }
            }
            float[] passiveContrsVals = new float[] {
                    combatFactors.char_.getWarriorTalents().getBloodFrenzy() * 0.05f,
                    buffsStats.getPhysicalHaste(),
                    heroism,
            };
            float passiveContrsTtlVal = (1f + passiveContrsVals[0])
                    * (1f + passiveContrsVals[1])
                    * (1f + passiveContrsVals[2])
                    - 1f;
            String[] passiveContrs = new String[] { "Blood Frenzy", "Buffs", "Heroism (Averaged)" };
            float UnProcdHaste = StatConversion.getHasteFromRating(buffedStats.getHasteRating(), CharacterClass.Warrior);
            float ProcdHaste = StatConversion.getHasteFromRating(maximumStats.getHasteRating(), CharacterClass.Warrior);
            String theFormat = genFormattedString(passiveContrs);

            dictValues.put("Haste", DPSWarrFormat.format(theFormat,
                    hastePercent, averageStats.getHasteRating(),
                    passiveContrsVals[0], passiveContrsVals[1], passiveContrsVals[2],
                    buffedStats.getHasteRating(),
                    (1f + passiveContrsTtlVal) * (1f + UnProcdHaste) - 1f,
                    maximumStats.getHasteRating(),
                    (1f + passiveContrsTtlVal) * (1f + ProcdHaste) - 1f
            ));
        }
        // Hit
        {
            float HitPercent = StatConversion.getHitFromRating(hitRating, CharacterClass.Warrior);
            float HitPercBonus = averageStats.getPhysicalHit();
            float capA1 = StatConversion.WHITE_MISS_CHANCE_CAP[LevelDif];
            float convcapA1 = (float) Math.ceil(StatConversion.getRatingFromHit(capA1));
            float sec2lastNumA1 = (convcapA1 - StatConversion.getRatingFromHit(HitPercent) - StatConversion.getRatingFromHit(HitPercBonus)) * -1;
            float capA2 = StatConversion.WHITE_MISS_CHANCE_CAP_DW[LevelDif];
            float convcapA2 = (float) Math.ceil(StatConversion.getRatingFromHit(capA2));
            float sec2lastNumA2 = (convcapA2 - StatConversion.getRatingFromHit(HitPercent) - StatConversion.getRatingFromHit(HitPercBonus)) * -1;
            dictValues.put("Hit",
                    DPSWarrFormat.format("{0:00.00%} : {1}*" + "{2:0.00%} : From Other Bonuses" +
                                    "\r\n{3:0.00%} : Total Hit % Bonus" +
                                    "\r\n\r\nWhite Two-Hander Cap: " +
                                    (sec2lastNumA1 > 0 ? "You can free {4:0} Rating"
                                            : "You need {4:0} more Rating") +
                                    "\r\nWhite Dual Wield Cap: " +
                                    (sec2lastNumA2 > 0 ? "You can free {5:0} Rating"
                                            : "You need {5:0} more Rating"),
                            StatConversion.getHitFromRating(averageStats.getHitRating(), CharacterClass.Warrior),
                            averageStats.getHitRating(),
                            HitPercBonus,
                            HitPercent + HitPercBonus,
                            (sec2lastNumA1 > 0 ? sec2lastNumA1 : sec2lastNumA1 * -1),
                            (sec2lastNumA2 > 0 ? sec2lastNumA2 : sec2lastNumA2 * -1)
                    ));
        }
        // Expertise
        {
            float capB1 = StatConversion.YELLOW_DODGE_CHANCE_CAP[LevelDif] - weapMastPerc;
            float convcapB1 = (float) Math.ceil(StatConversion.getExpertiseFromDodgeParryReduc(capB1));
            float sec2lastNumB1 = (convcapB1 - Math.min(mhExpertise, (ohExpertise != 0 ? ohExpertise : mhExpertise))) * -1;
            float lastNumB1 = StatConversion.getRatingFromExpertise((convcapB1 - weapMastPerc - Math.min(mhExpertise, (ohExpertise != 0 ? ohExpertise : mhExpertise))) * -1);
            float capB2 = StatConversion.YELLOW_PARRY_CHANCE_CAP[LevelDif];
            float convcapB2 = (float) Math.ceil(StatConversion.getExpertiseFromDodgeParryReduc(capB2));
            float sec2lastNumB2 = (convcapB2 - Math.min(mhExpertise, (ohExpertise != 0 ? ohExpertise : mhExpertise))) * -1;
            float lastNumB2 = StatConversion.getRatingFromExpertise((convcapB2 - Math.min(mhExpertise, (ohExpertise != 0 ? ohExpertise : mhExpertise))) * -1);
            dictValues.put("Expertise",
                    DPSWarrFormat.format("{0:00.00%} : {1:00.00} : {2}*" +
                                    "Following includes Racial bonus and Strength of Arms" +
                                    "\r\n{3:00.00%} Weapon Mastery (Dodge Only)" +
                                    "\r\n{4:00.00%} : {5:00.00} : MH" +
                                    "\r\n{6:00.00%} : {7:00.00} : OH" +
                                    "\r\n\r\n" + "Dodge Cap: " +
                                    (lastNumB1 > 0 ? "You can free {8:0} Expertise ({9:0} Rating)"
                                            : "You need {8:0} more Expertise ({9:0} Rating)") +
                                    "\r\n" + "Parry Cap: " +
                                    (lastNumB2 > 0 ? "You can free {10:0} Expertise ({11:0} Rating)"
                                            : "You need {10:0} more Expertise ({11:0} Rating)"),
                            StatConversion.getDodgeParryReducFromExpertise(expertise),
                            expertise,
                            averageStats.getExpertiseRating(),
                            weapMastPerc,
                            StatConversion.getDodgeParryReducFromExpertise(mhExpertise), mhExpertise,
                            StatConversion.getDodgeParryReducFromExpertise(ohExpertise), ohExpertise,
                            (sec2lastNumB1 > 0 ? sec2lastNumB1 : sec2lastNumB1 * -1), (lastNumB1 > 0 ? lastNumB1 : lastNumB1 * -1),
                            (sec2lastNumB2 > 0 ? sec2lastNumB2 : sec2lastNumB2 * -1), (lastNumB2 > 0 ? lastNumB2 : lastNumB2 * -1)
                    ));
        }

        dictValues.put("Description", DPSWarrFormat.format("DPS : PerHit : #ActsD"));
        // DPS Abilities
        format = "{0:0000} : {1:00000} : {2:000.00}";
        if (getTotalDPS() < 0f) {
            setTotalDPS(0f);
        }
        for (Rotation.AbilWrapper aw : rot.getAbilityList()) {
            if (!aw.ability.name.equals("Invalid")) {
                dictValues.put(aw.ability.name, DPSWarrFormat.format(format, aw.allDPS(), aw.ability.damageOnUse(), aw.allNumActivates())
                        + aw.ability.genTooltip(aw.allNumActivates(), aw.allDPS() / getTotalDPS()));
            }
        }
        // DPS General
        dictValues.put("White DPS", DPSWarrFormat.format("{0:0000} : {1:00000}", whiteDPS, whiteDmg) + whites.genTooltip(whiteDPSMH, whiteDPSOH, getTotalDPS()));
        dictValues.put("Deep Wounds", DPSWarrFormat.format("{0:0000}*{1:00.0%} of DPS", rot.DW.tickSize(), rot.DW.tickSize() / getTotalDPS()));
        dictValues.put("Special DMG Procs", DPSWarrFormat.format("{0:0000}*{1:00.0%} of DPS", specProcDPS, specProcDPS / getTotalDPS()));
        dictValues.put("Total DPS", DPSWarrFormat.format("{0:#,##0} : {1:#,###,##0}*", getTotalDPS(), getTotalDPS() * duration) + rot.GCDUsage);
        // Rage
        format = "{0:0000}";
        dictValues.put("Total Generated Rage", DPSWarrFormat.format("{0:00} = {1:0} + {2:0}", whiteRage + otherRage, whiteRage, otherRage));
        dictValues.put("Needed Rage for Abilities", DPSWarrFormat.format(format, needyRage));
        dictValues.put("Available Free Rage", DPSWarrFormat.format(format, freeRage));
        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return averageStats.getHealth();
            case "Armor": return averageStats.getArmor() + averageStats.getBonusArmor();
            case "Strength": return averageStats.getStrength();
            case "Attack Power": return averageStats.getAttackPower();
            case "Agility": return averageStats.getAgility();
            case "Crit %": return combatFactors.cMhYCrit * 100f;
            case "Haste %": return combatFactors.totalHaste() * 100f;
            case "ArP %": return averageStats.getArmorPenetration() * 100f;
            case "% Chance to Miss (White)": return combatFactors.cWMiss * 100f;
            case "% Chance to Miss (Yellow)": return combatFactors.cYMiss * 100f;
            case "% Chance to be Dodged": return combatFactors.cMhDodge * 100f;
            case "% Chance to be Parried": return combatFactors.cMhParry * 100f;
            case "% Chance to be Avoided (Yellow/Dodge)":
                return combatFactors.cYMiss * 100f + combatFactors.cMhDodge * 100f;
            default:
                break;
        }
        return 0.0f;
    }
}
