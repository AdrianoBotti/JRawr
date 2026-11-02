// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CharacterCalculationsProtWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET custom formats (rawr.base.util.NetFormat.fixed/percent) and the plain float
// ToString of the original (rawr.models.mage.NetFormat.netFloatToString, G7).
package rawr.models.protwarr;

import rawr.base.character.Buff;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CharacterCalculationsProtWarr extends CharacterCalculationsBase {
    public Stats basicStats;
    public List<Buff> activeBuffs;
    public AbilityModelList abilities;

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float getOverallPoints() {
        return (this.subPoints[0] + this.subPoints[1] + this.subPoints[2]);
    }

    @Override
    public void setOverallPoints(float value) {
    }

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getSurvivalPoints() {
        return this.subPoints[0];
    }

    public void setSurvivalPoints(float value) {
        this.subPoints[0] = Math.min(value, Float.MAX_VALUE);
    }

    public float getMitigationPoints() {
        return this.subPoints[1];
    }

    public void setMitigationPoints(float value) {
        this.subPoints[1] = Math.min(value, Float.MAX_VALUE);
    }

    public float getThreatPoints() {
        return this.subPoints[2];
    }

    public void setThreatPoints(float value) {
        this.subPoints[2] = Math.min(value, Float.MAX_VALUE);
    }

    public int targetLevel;
    public int rankingMode;
    public float threatScale;

    public float defense;
    public float dodge;
    public float parry;
    public float block;
    public float blockValue;
    public float miss;
    public float critReduction;
    public float critVulnerability;
    public float defenseRatingNeeded;
    public float armorReduction;
    public float guaranteedReduction;
    public float dodgePlusMissPlusParry;
    public float dodgePlusMissPlusParryPlusBlock;
    public float totalMitigation;
    public float baseAttackerSpeed;
    public float attackerSpeed;
    public float damageTaken;
    public float damageTakenPerHit;
    public float damageTakenPerBlock;
    public float damageTakenPerCritBlock;
    public float damageTakenPerCrit;

    public float tankPoints;
    public float burstTime;

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

    public float missedAttacks;
    public float avoidedAttacks;
    public float dodgedAttacks;
    public float parriedAttacks;
    public float blockedAttacks;
    public float hit;
    public float crit;
    public float expertise;
    public float haste;
    public float armorPenetration;
    public float weaponSpeed;
    public float totalDamagePerSecond;

    public float threatPerSecond;
    public float heroicStrikeFrequency;
    public String threatModel;
    public String threatModelName;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        dictValues.put("Health", rawr.models.mage.NetFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Strength", rawr.models.mage.NetFormat.netFloatToString(basicStats.getStrength()));
        dictValues.put("Agility", rawr.models.mage.NetFormat.netFloatToString(basicStats.getAgility()));
        dictValues.put("Stamina", rawr.models.mage.NetFormat.netFloatToString(basicStats.getStamina()));
        dictValues.put("Armor", rawr.models.mage.NetFormat.netFloatToString(basicStats.getArmor()) + "*Reduces physical damage taken by " + NetFormat.percent(armorReduction, 2));
        dictValues.put("Defense", rawr.models.mage.NetFormat.netFloatToString(defense) + "*Defense Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getDefenseRating()));
        dictValues.put("Dodge", NetFormat.percent(dodge, 2) + "*Dodge Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getDodgeRating()));
        dictValues.put("Parry", NetFormat.percent(parry, 2) + "*Parry Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getParryRating()));
        dictValues.put("Block", NetFormat.percent(block, 2) + "*Block Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getBlockRating()));
        dictValues.put("Miss", NetFormat.percent(miss, 2));
        dictValues.put("Block Value", rawr.models.mage.NetFormat.netFloatToString(blockValue));
        dictValues.put("Guaranteed Reduction", NetFormat.percent(guaranteedReduction, 2));
        dictValues.put("Avoidance", NetFormat.percent(dodgePlusMissPlusParry, 2) + " (+Block " + NetFormat.percent(dodgePlusMissPlusParryPlusBlock, 2) + ")");
        dictValues.put("Total Mitigation", NetFormat.percent(totalMitigation, 2));

        if (attackerSpeed == baseAttackerSpeed)
            dictValues.put("Attacker Speed", NetFormat.fixed(attackerSpeed, 2) + "s");
        else
            dictValues.put("Attacker Speed", NetFormat.fixed(attackerSpeed, 2) + "s*Base speed of " + NetFormat.fixed(baseAttackerSpeed, 2) + "s (modified by haste)");

        dictValues.put("Damage Taken",
                NetFormat.fixed(damageTaken, 1) + " DPS*" + NetFormat.fixed(damageTakenPerHit, 0) + " damage per normal attack\n"
                        + NetFormat.fixed(damageTakenPerBlock, 0) + " damage per blocked attack\n"
                        + NetFormat.fixed(damageTakenPerCritBlock, 0) + " damage per critically blocked attack\n"
                        + NetFormat.fixed(damageTakenPerCrit, 0) + " damage per critical attack");

        dictValues.put("Resilience",
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getResilience())
                        + "*Reduces the damage of critical strikes and chance to be critically hit by "
                        + rawr.models.mage.NetFormat.netFloatToString(StatConversion.getCritReductionFromResilience(basicStats.getResilience(), CharacterClass.Warrior)) + "%.");

        if (critVulnerability > 0.0001f) {
            float resilienceNeeded = (float) Math.ceil((double) (StatConversion.RATING_PER_RESILIENCE * critVulnerability));
            dictValues.put("Chance to be Crit",
                    NetFormat.percent(critVulnerability, 2) + "*CRITTABLE! Short by " + NetFormat.fixed(defenseRatingNeeded, 0) + " defense or " + NetFormat.fixed(resilienceNeeded, 0) + " resilience to be uncrittable.");
        } else
            dictValues.put("Chance to be Crit", NetFormat.percent(critVulnerability, 2) + "*Chance to crit reduced by " + NetFormat.percent(critReduction, 2));

        dictValues.put("Nature Resist", NetFormat.fixed(basicStats.getNatureResistance(), 0) + "*" + NetFormat.percent(natureReduction, 2) + " Total Reduction in Defensive Stance");
        dictValues.put("Arcane Resist", NetFormat.fixed(basicStats.getArcaneResistance(), 0) + "*" + NetFormat.percent(arcaneReduction, 2) + " Total Reduction in Defensive Stance");
        dictValues.put("Frost Resist", NetFormat.fixed(basicStats.getFrostResistance(), 0) + "*" + NetFormat.percent(frostReduction, 2) + " Total Reduction in Defensive Stance");
        dictValues.put("Fire Resist", NetFormat.fixed(basicStats.getFireResistance(), 0) + "*" + NetFormat.percent(fireReduction, 2) + " Total Reduction in Defensive Stance");
        dictValues.put("Shadow Resist", NetFormat.fixed(basicStats.getShadowResistance(), 0) + "*" + NetFormat.percent(shadowReduction, 2) + " Total Reduction in Defensive Stance");
        dictValues.put("Nature Survival", rawr.models.mage.NetFormat.netFloatToString(natureSurvivalPoints));
        dictValues.put("Frost Survival", rawr.models.mage.NetFormat.netFloatToString(frostSurvivalPoints));
        dictValues.put("Fire Survival", rawr.models.mage.NetFormat.netFloatToString(fireSurvivalPoints));
        dictValues.put("Shadow Survival", rawr.models.mage.NetFormat.netFloatToString(shadowSurvivalPoints));
        dictValues.put("Arcane Survival", rawr.models.mage.NetFormat.netFloatToString(arcaneSurvivalPoints));

        dictValues.put("Weapon Speed", NetFormat.fixed(weaponSpeed, 2) + "*" + NetFormat.percent(haste, 2) + " Haste");
        dictValues.put("Attack Power", rawr.models.mage.NetFormat.netFloatToString(basicStats.getAttackPower()));
        dictValues.put("Hit", NetFormat.percent(hit, 2) + "*Hit Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getHitRating()));
        dictValues.put("Expertise",
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint((double) (StatConversion.getExpertiseFromRating(basicStats.getExpertiseRating(), CharacterClass.Warrior) + basicStats.getExpertise())))
                        + "*Expertise Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getExpertiseRating())
                        + "\nReduces chance to be dodged or parried by " + NetFormat.percent(expertise, 2) + ".");
        dictValues.put("Haste", NetFormat.percent(haste, 2) + "*Haste Rating " + NetFormat.fixed(basicStats.getHasteRating(), 2));
        dictValues.put("Armor Penetration",
                NetFormat.percent(armorPenetration, 2) + "*Armor Penetration Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getArmorPenetrationRating())
                        + "\nArmor Reduction " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getArmorPenetration()));
        dictValues.put("Crit", NetFormat.percent(crit, 2) + "*Crit Rating " + rawr.models.mage.NetFormat.netFloatToString(basicStats.getCritRating()));
        // Never really used in current WoW itemization, just taking up space
        // dictValues.Add("Weapon Damage", string.Format("{0}", BasicStats.WeaponDamage));
        dictValues.put("Missed Attacks",
                NetFormat.percent(avoidedAttacks, 2) + "*Attacks Missed: " + NetFormat.percent(missedAttacks, 2) + "\nAttacks Dodged: " + NetFormat.percent(dodgedAttacks, 2) + "\n"
                        + "Attacks Parried: " + NetFormat.percent(parriedAttacks, 2));
        dictValues.put("Total Damage/sec", NetFormat.fixed(totalDamagePerSecond, 1));
        dictValues.put("Total Threat/sec", NetFormat.fixed(threatPerSecond, 1) + "*" + NetFormat.percent(heroicStrikeFrequency, 0) + " of Swings converted to Heroic Strikes");
        dictValues.put("Rotation", threatModelName + "*" + threatModel);

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
            default:
                dictValues.put("Ranking Mode", "Mitigation Scale*Customizable scale which allows you to weight mitigation vs. effective health.");
                dictValues.put("Survival Points", NetFormat.fixed(getSurvivalPoints(), 0) + "*Effective Health");
                break;
        }
        dictValues.put("Overall Points", NetFormat.fixed(getOverallPoints(), 0));
        dictValues.put("Mitigation Points", NetFormat.fixed(getMitigationPoints(), 0));
        dictValues.put("Threat Points", NetFormat.fixed(getThreatPoints(), 0));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health":
                return basicStats.getHealth();

            case "% Total Mitigation":
                return totalMitigation * 100.0f;
            case "% Guaranteed Reduction":
                return guaranteedReduction * 100.0f;
            case "% Chance to be Crit":
                return (float) round(critVulnerability * 100.0f, 2);
            case "% Avoidance":
                return dodgePlusMissPlusParry * 100.0f;
            case "% Avoidance+Block":
                return dodgePlusMissPlusParryPlusBlock * 100.0f;
            case "Block Value":
                return blockValue;

            case "Threat/sec":
                return threatPerSecond;
            case "% Chance to be Avoided":
                return avoidedAttacks * 100.0f;
            case "% Chance to be Parried":
                return parriedAttacks * 100.0f;
            case "% Chance to be Dodged":
                return dodgedAttacks * 100.0f;
            case "% Chance to Miss":
                return missedAttacks * 100.0f;

            case "Burst Time":
                return burstTime;
            case "Nature Survival":
                return natureSurvivalPoints;
            case "Fire Survival":
                return fireSurvivalPoints;
            case "Frost Survival":
                return frostSurvivalPoints;
            case "Shadow Survival":
                return shadowSurvivalPoints;
            case "Arcane Survival":
                return arcaneSurvivalPoints;
        }
        return 0.0f;
    }

    // C# Math.Round(double, 2) (banker's): scale, round half to even, unscale.
    private static double round(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }
}
