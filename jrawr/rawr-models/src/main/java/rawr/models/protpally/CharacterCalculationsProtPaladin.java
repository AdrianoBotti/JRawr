// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CharacterCalculationsProtPaladin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use rawr.models.mage.NetFormat.netFloatToString for the .NET default float
// ToString (G7, invariant) and rawr.base.util.NetFormat.percent for the `0.00%`/`0.0000%` custom
// formats; the `{0:0}` formats on the double-valued crit-deficit numbers use a G15-prerounded
// BigDecimal (netDoubleFixed), matching Mono's custom double format.
package rawr.models.protpally;

import rawr.base.character.Buff;
import rawr.base.character.CharacterClass;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CharacterCalculationsProtPaladin extends CharacterCalculationsBase {
    public Stats basicStats;
    public List<Buff> activeBuffs;
    public AbilityModelList abilities;

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getSurvivalPoints() {
        return subPoints[0];
    }

    public void setSurvivalPoints(float value) {
        subPoints[0] = value;
    }

    public float getMitigationPoints() {
        return subPoints[1];
    }

    public void setMitigationPoints(float value) {
        subPoints[1] = value;
    }

    public float getThreatPoints() {
        return subPoints[2];
    }

    public void setThreatPoints(float value) {
        subPoints[2] = value;
    }

    // Target Info
    public int targetLevel;
    public int targetArmor;
    public float effectiveTargetArmor;
    public float targetArmorDamageReduction;
    public float effectiveTargetArmorDamageReduction;

    // Ranking Modes
    public int rankingMode;
    public float threatScale;

    //Basic Tank Defensive Stats
    public float defense;
    public float defenseRating;
    public float dodge;
    public float parry;
    public float miss;
    public float critReduction;
    public float critVulnerability;
    public float critVulnerabilityWithoutHolyShield;
    public boolean usingHolyShield;
    public float armorReduction;
    public float guaranteedReduction;
    public float dodgePlusMissPlusParry;
    public float totalMitigation;
    public float baseAttackerSpeed;
    public float attackerSpeed;
    public float damageTaken;
    public float dpsTaken;
    public float damageTakenPerHit;
    public float damageTakenPerCrit;

    public float tankPoints;
    public float burstTime;

    // Shield Tank Defensive Stats
    public float block;
    public float activeBlockValue;
    public float staticBlockValue;
    public float dodgePlusMissPlusParryPlusBlock;
    public float damageTakenPerBlock;

    // Magic Defensive Stats
    public float[] resistanceTable;
    public float natureReduction;
    public float frostReduction;
    public float fireReduction;
    public float shadowReduction;
    public float arcaneReduction;
    public float natureSurvivalPoints;
    public float frostSurvivalPoints;
    public float fireSurvivalPoints;
    public float shadowSurvivalPoints;
    public float arcaneSurvivalPoints;

    // Basic Offensive Stats
    public float missedAttacks;
    public float avoidedAttacks;
    public float dodgedAttacks;
    public float parriedAttacks;
    public float glancingAttacks;
    public float glancingReduction;
    public float blockedAttacks;
    public float hit;
    public float spellHit;
    public float crit;
    public float spellCrit;
    public float expertise;
    public float physicalHaste;
    public float spellHaste;
    public float armorPenetration;
    public float armorPenetrationFromRating;
    public float effectiveArmorPenetration;
    public float effectiveArmorPenetrationRating;
    public float armorPenetrationCap;
    public float weaponSpeed;
    public float totalDamagePerSecond;

    public float threatPerSecond;
    public String threatModel;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        dictValues.put("Health", netFloat(basicStats.getHealth()));
        dictValues.put("Mana", netFloat(basicStats.getMana()));
        dictValues.put("Strength", netFloat(basicStats.getStrength()));
        dictValues.put("Agility", netFloat(basicStats.getAgility()));
        dictValues.put("Stamina", netFloat(basicStats.getStamina()) + "*Increases Health by " + netFloat((basicStats.getStamina() - 20f) * 10f + 20f));
        dictValues.put("Intellect", netFloat(basicStats.getIntellect()));
        dictValues.put("Armor", netFloat(basicStats.getArmor()) + "*Reduces physical damage taken by " + NetFormat.percent(armorReduction, 2) + "\n" +
                "Armor Damage Reduction depends on Attacker Level.");
        dictValues.put("Defense", netFloat(defense) + "*Defense Rating " + netFloat(basicStats.getDefenseRating()));
        dictValues.put("Dodge", NetFormat.percent(dodge, 4) + "*Dodge Rating " + netFloat(basicStats.getDodgeRating()));
        dictValues.put("Parry", NetFormat.percent(parry, 4) + "*Parry Rating " + netFloat(basicStats.getParryRating()));
        dictValues.put("Block", NetFormat.percent(block, 4) + "*Block Rating " + netFloat(basicStats.getBlockRating()));
        dictValues.put("Miss", NetFormat.percent(miss, 4));
        dictValues.put("Block Value", netFloat(staticBlockValue) + "*" + netFloat(activeBlockValue) + " Active Block Value (Libram etc)");
        dictValues.put("Guaranteed Reduction", NetFormat.percent(guaranteedReduction, 2));
        dictValues.put("Avoidance", NetFormat.percent(dodgePlusMissPlusParry, 4) + "*Avoidance Points " + netFloat(dodgePlusMissPlusParry * 10000f));
        dictValues.put("Avoidance + Block", NetFormat.percent(dodgePlusMissPlusParryPlusBlock, 4));
        dictValues.put("Total Mitigation", NetFormat.percent(totalMitigation, 2));

        if (attackerSpeed == baseAttackerSpeed)
            dictValues.put("Attacker Speed", NetFormat.fixed(attackerSpeed, 2) + "s");
        else
            dictValues.put("Attacker Speed", NetFormat.fixed(attackerSpeed, 2) + "s*Base speed of " + NetFormat.fixed(baseAttackerSpeed, 2) + "s (reduced by parry haste)");

        dictValues.put("Damage Taken",
                NetFormat.fixed(dpsTaken, 1) + " DPS*" + NetFormat.fixed(damageTakenPerHit, 0) + " damage per normal attack\n" +
                        NetFormat.fixed(damageTakenPerBlock, 0) + " damage per blocked attack\n" +
                        NetFormat.fixed(damageTakenPerCrit, 0) + " damage per critical attack\n" +
                        "On Average, you take\n" +
                        NetFormat.percent(damageTaken, 2) + " of incoming damage");

        dictValues.put("Resilience",
                netFloat(basicStats.getResilience()) + "*Reduces periodic damage and chance to be critically hit by " + NetFormat.percent(StatConversion.getCritReductionFromResilience(basicStats.getResilience(), CharacterClass.Paladin), 2) + ".\n" +
                        "Reduces the effect of mana-drains and the damage of critical strikes by " + NetFormat.percent(StatConversion.getCritReductionFromResilience(basicStats.getResilience(), CharacterClass.Paladin) * 2f, 2) + ".");

        if (critVulnerability > 0.0001f) {
            double defenseNeeded = Math.ceil((100 * critVulnerability / StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER) * StatConversion.RATING_PER_DEFENSE);
            double resilienceNeeded = Math.ceil(critVulnerability * StatConversion.RATING_PER_RESILIENCE);
            dictValues.put("Chance to be Crit",
                    NetFormat.percent(critVulnerability, 2) + "*CRITTABLE! Short by approximately " + netDoubleFixed(defenseNeeded, 0) + " defense rating or approximately " + netDoubleFixed(resilienceNeeded, 0) + " resilience rating to be uncrittable.");
        } else
            dictValues.put("Chance to be Crit", NetFormat.percent(critVulnerability, 2) + "*Chance to be crit reduced by " + NetFormat.percent(critReduction, 2));

        if (usingHolyShield) {
            if (critVulnerabilityWithoutHolyShield > 0.0001f) {
                double defenseNeeded = Math.ceil((100 * critVulnerabilityWithoutHolyShield / StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER) * StatConversion.RATING_PER_DEFENSE);
                double resilienceNeeded = Math.ceil(critVulnerability * StatConversion.RATING_PER_RESILIENCE);
                dictValues.put("...Without Holy Shield",
                        NetFormat.percent(critVulnerabilityWithoutHolyShield, 2) + "*CRITTABLE! Short by approximately " + netDoubleFixed(defenseNeeded, 0) + " defense rating or approximately " + netDoubleFixed(resilienceNeeded, 0) + " resilience rating to be uncrittable without holy shield.");
            } else
                dictValues.put("...Without Holy Shield", NetFormat.percent(critVulnerabilityWithoutHolyShield, 2) + "*Chance to be crit reduced by " + NetFormat.percent(critReduction, 2));
        } else {
            dictValues.put("...Without Holy Shield", "N/A*Not applicable");
        }

        dictValues.put("Nature Resist", NetFormat.fixed(basicStats.getNatureResistance(), 0) + "*" + NetFormat.percent(natureReduction, 2) + " Total Reduction");
        dictValues.put("Arcane Resist", NetFormat.fixed(basicStats.getArcaneResistance(), 0) + "*" + NetFormat.percent(arcaneReduction, 2) + " Total Reduction");
        dictValues.put("Frost Resist", NetFormat.fixed(basicStats.getFrostResistance(), 0) + "*" + NetFormat.percent(frostReduction, 2) + " Total Reduction");
        dictValues.put("Fire Resist", NetFormat.fixed(basicStats.getFireResistance(), 0) + "*" + NetFormat.percent(fireReduction, 2) + " Total Reduction");
        dictValues.put("Shadow Resist", NetFormat.fixed(basicStats.getShadowResistance(), 0) + "*" + NetFormat.percent(shadowReduction, 2) + " Total Reduction");
        dictValues.put("Nature Survival", netFloat(natureSurvivalPoints));
        dictValues.put("Frost Survival", netFloat(frostSurvivalPoints));
        dictValues.put("Fire Survival", netFloat(fireSurvivalPoints));
        dictValues.put("Shadow Survival", netFloat(shadowSurvivalPoints));
        dictValues.put("Arcane Survival", netFloat(arcaneSurvivalPoints));

        dictValues.put("Weapon Speed", NetFormat.fixed(weaponSpeed, 2) + "*" + NetFormat.percent(physicalHaste, 2) + " Haste");
        dictValues.put("Attack Power", netFloat(basicStats.getAttackPower()));
        dictValues.put("Spell Power", netFloat(basicStats.getSpellPower()));
        dictValues.put("Hit", NetFormat.percent(hit, 2) + "*Hit Rating " + netFloat(basicStats.getHitRating()) + "\nAgainst a Target of Level " + targetLevel);
        dictValues.put("Spell Hit", NetFormat.percent(spellHit, 2) + "*Hit Rating " + netFloat(basicStats.getHitRating()) + "\nAgainst a Target of Level " + targetLevel);
        dictValues.put("Expertise",
                NetFormat.fixed(StatConversion.getExpertiseFromRating(basicStats.getExpertiseRating(), CharacterClass.Paladin) + basicStats.getExpertise(), 2) + "*Expertise Rating " + netFloat(basicStats.getExpertiseRating()) + "\n" +
                        "Reduces chance to be dodged or parried by " + NetFormat.percent(expertise, 2) + ".");
        dictValues.put("Physical Haste", NetFormat.percent(physicalHaste, 2) + "*Haste Rating " + NetFormat.fixed(basicStats.getHasteRating(), 2));
        dictValues.put("Effective Target Armor",
                netFloat((float) Math.floor(effectiveTargetArmor)) + "*Reduces the physical damage you deal by " + NetFormat.percent(effectiveTargetArmorDamageReduction, 2) + "\n\n" +
                        "Effective Armor depends on Armor ignore debuffs,\n" +
                        "your Armor Penetration buffs and talents.\n" +
                        "Enemy Damage Reduction depends on your Level.");

        dictValues.put("Effective Armor Penetration",
                NetFormat.percent(effectiveArmorPenetration, 2) + "*Armor Penetration Rating " + netFloat(basicStats.getArmorPenetrationRating()) + " = " + NetFormat.percent(armorPenetrationFromRating, 2) + ".\n\n" +
                        "Your Armor Penetration Cap is " + netFloat((float) Math.floor(armorPenetrationCap)) + " Armor, this is\n" +
                        "the highest amount your Arp Rating will ignore.\n" +
                        "Your Rating currently reduces Enemy Armor by " + netFloat((float) Math.floor(armorPenetrationCap * armorPenetrationFromRating)) + ".\n\n" +
                        "Your Arp Rating is " + NetFormat.percent(effectiveArmorPenetrationRating, 2) + " effective (GC).\n" +
                        "Note that debuffs like Sunder reduce the Cap.");

        dictValues.put("Crit", NetFormat.percent(crit, 2) + "*Crit Rating " + netFloat(basicStats.getCritRating()) + "\nAgainst a Target of Level " + targetLevel);
        dictValues.put("Spell Crit", NetFormat.percent(spellCrit, 2) + "*Crit Rating " + netFloat(basicStats.getCritRating()) + "\nAgainst a Target of Level " + targetLevel);
        dictValues.put("Weapon Damage", NetFormat.fixed(basicStats.getWeaponDamage(), 2) + "*As average damage per " + Lookup.name(Ability.None) + "\nBefore Armor");
        dictValues.put("Missed Attacks",
                NetFormat.percent(avoidedAttacks, 2) + "*Attacks Missed: " + NetFormat.percent(missedAttacks, 2) + "\nAttacks Dodged: " + NetFormat.percent(dodgedAttacks, 2) + "\n" +
                        "Attacks Parried: " + NetFormat.percent(parriedAttacks, 2));
        dictValues.put("Glancing Attacks", NetFormat.percent(glancingAttacks, 2) + "*" + NetFormat.percent(1.0f - glancingReduction, 2) + " Reduction\n" +
                "Against a Target of Level " + targetLevel);
        dictValues.put("Total Damage/sec", NetFormat.fixed(totalDamagePerSecond, 1) + "*" + threatModel);
        dictValues.put("Threat/sec", NetFormat.fixed(threatPerSecond, 1) + "*" + threatModel);

        switch (rankingMode) {
            case 2:
                dictValues.put("Ranking Mode", "TankPoints*The average amount of unmitigated damage which can be taken before dying");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Effective Health");
                break;
            case 3:
                dictValues.put("Ranking Mode", "Burst Time*The average amount of time between events which have a chance to result in a burst death");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*" + NetFormat.fixed(getSurvivalPoints() / 100.0f, 2) + " seconds between events");
                break;
            case 4:
                dictValues.put("Ranking Mode", "Damage Output*The average amount of DPS which can be produced");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Survival is not weighted in this mode");
                break;
            case 5:
                dictValues.put("Ranking Mode", "ProtWarr Mode*Average mitigated damage vs Effective Health");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Effective Health");
                break;
            case 6:
                dictValues.put("Ranking Mode", "Damage Taken*Average taken damage vs. Effective Health");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Effective Health");
                break;
            case 7:
                dictValues.put("Ranking Mode", "Ranking Mode 7*Dummy Placeholder");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Survival Placeholder");
                break;
            case 8:
                dictValues.put("Ranking Mode", "Ranking Mode 8*Dummy Placeholder");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Survival Placeholder");
                break;
            default:
                dictValues.put("Ranking Mode", "Mitigation Scale*Customizable scale which allows you to weight mitigation vs. effective health.");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Effective Health");
                break;
        }
        dictValues.put("Overall Points", NetFormat.fixed(overallPoints, 0));
        dictValues.put("Mitigation Points", NetFormat.fixed(getMitigationPoints(), 0));
        dictValues.put("Threat Points", NetFormat.fixed(getThreatPoints(), 0));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "% Guaranteed Reduction": return guaranteedReduction * 100.0f;
            case "% Total Mitigation": return totalMitigation * 100.0f;
            case "Avoidance Points": return dodgePlusMissPlusParry * 10000.0f;
            case "% Avoid + Block Attacks": return dodgePlusMissPlusParryPlusBlock * 100.0f;
            case "% Chance to be Crit": return ((float) (Math.rint((double) (critVulnerability * 100.0f) * 100.0) / 100.0));
            case "% Chance to be Crit Without Holy Shield": return ((float) (Math.rint((double) (critVulnerabilityWithoutHolyShield * 100.0f) * 100.0) / 100.0));
            case "Block Value": return staticBlockValue;
            case "% Block Chance": return block * 100.0f;
            case "% Chance to be Avoided": return avoidedAttacks * 100.0f;
            case "% Chance to be Missed": return missedAttacks * 100.0f;
            case "% Chance to be Dodged": return dodgedAttacks * 100.0f;
            case "% Chance to be Parried": return parriedAttacks * 100.0f;
            // case "% Hit Chance": return Hit * 100.0f;
            case "% Spell Hit Chance": return spellHit * 100.0f;
            case "Burst Time": return burstTime;
            case "TankPoints": return tankPoints;
            case "Nature Survival": return natureSurvivalPoints;
            case "Fire Survival": return fireSurvivalPoints;
            case "Frost Survival": return frostSurvivalPoints;
            case "Shadow Survival": return shadowSurvivalPoints;
            case "Arcane Survival": return arcaneSurvivalPoints;
            case "Threat Per Second": return threatPerSecond;
        }
        return 0.0f;
    }

    /** .NET default float ToString (G7, invariant), via the Mage display helper. */
    private static String netFloat(float value) {
        return rawr.models.mage.NetFormat.netFloatToString(value);
    }

    /** .NET `{0:0}`-style custom format on a Double (Mono pre-rounds to 15 significant digits). */
    private static String netDoubleFixed(double value, int decimals) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal(value).round(new MathContext(15, RoundingMode.HALF_UP));
        return bd.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }
}
