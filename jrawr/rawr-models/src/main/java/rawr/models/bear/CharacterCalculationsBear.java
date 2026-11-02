// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Bear/CalculationsBear.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (CharacterCalculationsBear lives in CalculationsBear.cs in the original).
// Display values: plain float ToString() and string.Format float/double arguments use the .NET
// formatters (NetFormat G7/G15); custom numeric format strings "0.000%" and "0.000" are reproduced
// by percent3/fixed (Mono pre-rounds the Single to 7 significant digits, half away from zero).
package rawr.models.bear;

import rawr.base.stats.Stats;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.models.mage.NetFormat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsBear extends CharacterCalculationsBase {
    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getMitigationPoints() {
        return this.subPoints[0];
    }

    public void setMitigationPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getSurvivalPoints() {
        return this.subPoints[1];
    }

    public void setSurvivalPoints(float value) {
        this.subPoints[1] = value;
    }

    public float getThreatPoints() {
        return this.subPoints[2];
    }

    public void setThreatPoints(float value) {
        this.subPoints[2] = value;
    }

    public Stats basicStats;
    public int targetLevel;
    public float dodge;
    public float miss;
    public float constantDamageReduction;
    public float avoidancePreDR;
    public float avoidancePostDR;
    public float totalMitigation;
    public float savageDefenseChance;
    public float savageDefenseValue;
    public float savageDefensePercent;
    public float damageTaken;
    public float critReduction;
    public float cappedCritReduction;
    public float survivalPointsRaw;

    public float natureSurvivalPoints;
    public float frostSurvivalPoints;
    public float fireSurvivalPoints;
    public float shadowSurvivalPoints;
    public float arcaneSurvivalPoints;

    public float attackSpeed;
    public float mangleCooldown;

    public float missedAttacks;
    public float dodgedAttacks;
    public float parriedAttacks;

    public float getAvoidedAttacks() {
        return missedAttacks + dodgedAttacks + parriedAttacks;
    }

    public float meleeThreatRaw;
    public float maulThreatRaw;
    public float mangleThreatRaw;
    public float swipeThreatRaw;
    public float faerieFireThreatRaw;
    public float lacerateThreatRaw;
    public float lacerateDotThreatRaw;

    public float meleeThreatAverage;
    public float maulThreatAverage;
    public float mangleThreatAverage;
    public float swipeThreatAverage;
    public float faerieFireThreatAverage;
    public float lacerateThreatAverage;
    public float lacerateDotThreatAverage;

    public float meleeDamageRaw;
    public float maulDamageRaw;
    public float mangleDamageRaw;
    public float swipeDamageRaw;
    public float faerieFireDamageRaw;
    public float lacerateDamageRaw;
    public float lacerateDotDamageRaw;

    public float meleeDamageAverage;
    public float maulDamageAverage;
    public float mangleDamageAverage;
    public float swipeDamageAverage;
    public float faerieFireDamageAverage;
    public float lacerateDamageAverage;
    public float lacerateDotDamageAverage;

    public float maulTPR;
    public float maulDPR;
    public float mangleTPR;
    public float mangleDPR;
    public float swipeTPR;
    public float swipeDPR;
    public float lacerateTPR;
    public float lacerateDPR;

    public BearRotationCalculator.BearRotationCalculation highestDPSRotation;
    public BearRotationCalculator.BearRotationCalculation highestTPSRotation;
    public BearRotationCalculator.BearRotationCalculation swipeRotation;
    public BearRotationCalculator.BearRotationCalculation customRotation;

    /** .NET "0.000%" custom format on a Single (Mono pre-rounds to 7 significant digits). */
    private static String percent3(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal g7 = new BigDecimal(value).round(new MathContext(7, RoundingMode.HALF_UP));
        return g7.multiply(new BigDecimal(100)).setScale(3, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /** .NET "0.000" custom format on a Single. */
    private static String fixed3(float value) {
        return rawr.base.util.NetFormat.fixed(value, 3);
    }

    private static String roundDouble(double value) {
        return NetFormat.netDoubleToString(Math.rint(value));
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        float baseMiss = StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - basicStats.getPhysicalHit();
        float baseDodge = StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - StatConversion.getDodgeParryReducFromExpertise(basicStats.getExpertise());
        float baseParry = StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - StatConversion.getDodgeParryReducFromExpertise(basicStats.getExpertise());
        float capMiss = (float) Math.ceil((double) (baseMiss * StatConversion.RATING_PER_PHYSICALHIT));
        float capDodge = (float) Math.ceil((double) (baseDodge * 400f * StatConversion.RATING_PER_EXPERTISE));
        float capParry = (float) Math.ceil((double) (baseParry * 400f * StatConversion.RATING_PER_EXPERTISE));

        String tipMiss;
        if (basicStats.getHitRating() > capMiss)
            tipMiss = "*Over the cap by " + NetFormat.netFloatToString(basicStats.getHitRating() - capMiss) + " Hit Rating";
        else if (basicStats.getHitRating() < capMiss)
            tipMiss = "*Under the cap by " + NetFormat.netFloatToString(capMiss - basicStats.getHitRating()) + " Hit Rating";
        else
            tipMiss = "*Exactly at the cap";

        String tipDodgeParry;
        if (basicStats.getExpertiseRating() > capDodge)
            tipDodgeParry = "*Over the dodge cap by " + NetFormat.netFloatToString(basicStats.getExpertiseRating() - capDodge) + " Expertise Rating\r\n";
        else if (basicStats.getExpertiseRating() < capDodge)
            tipDodgeParry = "*Under the dodge cap by " + NetFormat.netFloatToString(capDodge - basicStats.getExpertiseRating()) + " Expertise Rating\r\n";
        else
            tipDodgeParry = "*Exactly at the dodge cap";

        if (basicStats.getExpertiseRating() > capParry)
            tipDodgeParry += "Over the parry cap by " + NetFormat.netFloatToString(basicStats.getExpertiseRating() - capParry) + " Expertise Rating";
        else if (basicStats.getExpertiseRating() < capParry)
            tipDodgeParry += "Under the parry cap by " + NetFormat.netFloatToString(capParry - basicStats.getExpertiseRating()) + " Expertise Rating";
        else
            tipDodgeParry += "Exactly at the parry cap";

        int armorCap = (int) Math.ceil((double) ((1402.5f * targetLevel) - 66502.5f));
        float levelDifference = 0.002f * (targetLevel - 80);
        float targetCritReduction = StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        int defToCap = 0, resToCap = 0;
        if (critReduction < targetCritReduction) {
            while (((float) Math.floor(StatConversion.getDefenseFromRating(basicStats.getDefenseRating() + defToCap)) * 0.0004f)
                    + StatConversion.getCritReductionFromResilience(basicStats.getResilience()) + basicStats.getCritChanceReduction() < targetCritReduction)
                defToCap++;
            while (((float) Math.floor(StatConversion.getDefenseFromRating(basicStats.getDefenseRating())) * 0.0004f)
                    + StatConversion.getCritReductionFromResilience(basicStats.getResilience() + resToCap) + basicStats.getCritChanceReduction() < targetCritReduction)
                resToCap++;
        } else if (critReduction > targetCritReduction) {
            while (((float) Math.floor(StatConversion.getDefenseFromRating(basicStats.getDefenseRating() + defToCap)) * 0.0004f)
                    + StatConversion.getCritReductionFromResilience(basicStats.getResilience()) + basicStats.getCritChanceReduction() > targetCritReduction)
                defToCap--;
            while (((float) Math.floor(StatConversion.getDefenseFromRating(basicStats.getDefenseRating())) * 0.0004f)
                    + StatConversion.getCritReductionFromResilience(basicStats.getResilience() + resToCap) + basicStats.getCritChanceReduction() > targetCritReduction)
                resToCap--;
            defToCap++;
            resToCap++;
        }

        String tipResist;
        tipResist = StatConversion.getResistanceTableString(targetLevel, 80, basicStats.getNatureResistance(), 0);
        dictValues.put("Nature Resist", NetFormat.netFloatToString(basicStats.getNatureResistance()) + "*" + tipResist);
        tipResist = StatConversion.getResistanceTableString(targetLevel, 80, basicStats.getArcaneResistance(), 0);
        dictValues.put("Arcane Resist", NetFormat.netFloatToString(basicStats.getArcaneResistance()) + "*" + tipResist);
        tipResist = StatConversion.getResistanceTableString(targetLevel, 80, basicStats.getFrostResistance(), 0);
        dictValues.put("Frost Resist", NetFormat.netFloatToString(basicStats.getFrostResistance()) + "*" + tipResist);
        tipResist = StatConversion.getResistanceTableString(targetLevel, 80, basicStats.getFireResistance(), 0);
        dictValues.put("Fire Resist", NetFormat.netFloatToString(basicStats.getFireResistance()) + "*" + tipResist);
        tipResist = StatConversion.getResistanceTableString(targetLevel, 80, basicStats.getShadowResistance(), 0);
        dictValues.put("Shadow Resist", NetFormat.netFloatToString(basicStats.getShadowResistance()) + "*" + tipResist);

        dictValues.put("Health", NetFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Agility", NetFormat.netFloatToString(basicStats.getAgility()));
        dictValues.put("Armor", NetFormat.netFloatToString(basicStats.getArmor()));
        dictValues.put("Stamina", NetFormat.netFloatToString(basicStats.getStamina()));
        dictValues.put("Dodge Rating", NetFormat.netFloatToString(basicStats.getDodgeRating()));
        dictValues.put("Defense Rating", NetFormat.netFloatToString(basicStats.getDefenseRating()));
        dictValues.put("Resilience", NetFormat.netFloatToString(basicStats.getResilience()));
        dictValues.put("Dodge", percent3(dodge));
        dictValues.put("Miss", percent3(miss));
        if (basicStats.getArmor() == armorCap)
            dictValues.put("Mitigation", percent3(constantDamageReduction)
                    + "*Exactly at the armor cap against level " + targetLevel + " mobs.");
        else if (basicStats.getArmor() > armorCap)
            dictValues.put("Mitigation", percent3(constantDamageReduction)
                    + "*Over the armor cap by " + NetFormat.netFloatToString(basicStats.getArmor() - armorCap) + " armor.");
        else
            dictValues.put("Mitigation", percent3(constantDamageReduction)
                    + "*Short of the armor cap by " + NetFormat.netFloatToString(armorCap - basicStats.getArmor()) + " armor.");
        dictValues.put("Avoidance PreDR", percent3(avoidancePreDR));
        dictValues.put("Avoidance PostDR", percent3(avoidancePostDR));
        dictValues.put("Total Mitigation", percent3(totalMitigation));
        dictValues.put("Damage Taken", percent3(damageTaken));
        dictValues.put("Savage Defense", percent3(savageDefenseChance) + "  ~ " + NetFormat.netFloatToString(savageDefenseValue)
                + "*" + percent3(savageDefenseChance) + " chance to absorb incoming hit\r\n" + NetFormat.netFloatToString(savageDefenseValue)
                + " absorbed per hit\r\n" + percent3(savageDefensePercent) + " of incoming damage absorbed");
        dictValues.put("Chance to be Crit", percent3((0.05f + levelDifference) - critReduction));
        dictValues.put("Overall Points", NetFormat.netFloatToString(getOverallPoints()));
        dictValues.put("Mitigation Points", NetFormat.netFloatToString(getMitigationPoints()));
        dictValues.put("Survival Points", NetFormat.netFloatToString(getSurvivalPoints()) + "*" + NetFormat.netFloatToString(survivalPointsRaw) + " Before Soft Cap");
        dictValues.put("Threat Points", NetFormat.netFloatToString(getThreatPoints()));

        dictValues.put("Nature Survival", NetFormat.netFloatToString(natureSurvivalPoints));
        dictValues.put("Frost Survival", NetFormat.netFloatToString(frostSurvivalPoints));
        dictValues.put("Fire Survival", NetFormat.netFloatToString(fireSurvivalPoints));
        dictValues.put("Shadow Survival", NetFormat.netFloatToString(shadowSurvivalPoints));
        dictValues.put("Arcane Survival", NetFormat.netFloatToString(arcaneSurvivalPoints));

        dictValues.put("Strength", NetFormat.netFloatToString(basicStats.getStrength()));
        dictValues.put("Attack Power", NetFormat.netFloatToString(basicStats.getAttackPower()));
        dictValues.put("Crit Rating", NetFormat.netFloatToString(basicStats.getCritRating()));
        dictValues.put("Hit Rating", NetFormat.netFloatToString(basicStats.getHitRating()) + tipMiss);
        dictValues.put("Expertise Rating", NetFormat.netFloatToString(basicStats.getExpertiseRating()) + tipDodgeParry);
        dictValues.put("Haste Rating", NetFormat.netFloatToString(basicStats.getHasteRating()) + "*" + fixed3(attackSpeed) + "sec Attack Speed");
        dictValues.put("Armor Penetration Rating", NetFormat.netFloatToString(basicStats.getArmorPenetrationRating()));

        dictValues.put("Avoided Attacks", percent3(getAvoidedAttacks()) + "*" + percent3(missedAttacks) + " Missed\r\n"
                + percent3(dodgedAttacks) + " Dodged\r\n" + percent3(parriedAttacks) + " Parried");

        dictValues.put("Highest DPS Rotation", roundDouble(highestDPSRotation.dps) + " DPS, " + roundDouble(highestDPSRotation.tps) + " TPS*" + getRotationTooltip(highestDPSRotation.name));
        dictValues.put("Highest TPS Rotation", roundDouble(highestTPSRotation.dps) + " DPS, " + roundDouble(highestTPSRotation.tps) + " TPS*" + getRotationTooltip(highestTPSRotation.name));
        dictValues.put("Swipe Rotation", roundDouble(swipeRotation.dps) + " DPS, " + roundDouble(swipeRotation.tps) + " TPS*" + getRotationTooltip(swipeRotation.name));
        dictValues.put("Custom Rotation", roundDouble(customRotation.dps) + " DPS, " + roundDouble(customRotation.tps) + " TPS*" + getRotationTooltip(customRotation.name));

        dictValues.put("Melee", attackText(meleeDamageRaw, meleeThreatRaw, meleeDamageAverage, meleeThreatAverage));
        dictValues.put("Maul", attackTextWithRage(maulDamageRaw, maulThreatRaw, maulDamageAverage, maulThreatAverage, maulTPR, maulDPR));
        dictValues.put("Mangle", attackTextWithRage(mangleDamageRaw, mangleThreatRaw, mangleDamageAverage, mangleThreatAverage, mangleTPR, mangleDPR));
        dictValues.put("Swipe", attackTextWithRage(swipeDamageRaw, swipeThreatRaw, swipeDamageAverage, swipeThreatAverage, swipeTPR, swipeDPR));
        dictValues.put("Faerie Fire", attackText(faerieFireDamageRaw, faerieFireThreatRaw, faerieFireDamageAverage, faerieFireThreatAverage));
        dictValues.put("Lacerate", attackTextWithRage(lacerateDamageRaw, lacerateThreatRaw, lacerateDamageAverage, lacerateThreatAverage, lacerateTPR, lacerateDPR));
        dictValues.put("Lacerate DoT Tick", attackText(lacerateDotDamageRaw, lacerateDotThreatRaw, lacerateDotDamageAverage, lacerateDotThreatAverage)
                .replace("Swing", "Tick"));

        return dictValues;
    }

    private static String attackText(float damageRaw, float threatRaw, float damageAverage, float threatAverage) {
        return NetFormat.netFloatToString(damageRaw) + " Dmg, " + NetFormat.netFloatToString(threatRaw)
                + " Threat*Per Hit: " + NetFormat.netFloatToString(damageRaw) + " Damage, " + NetFormat.netFloatToString(threatRaw)
                + " Threat\r\nPer Average Swing: " + NetFormat.netFloatToString(damageAverage) + " Damage, "
                + NetFormat.netFloatToString(threatAverage) + " Threat";
    }

    private static String attackTextWithRage(float damageRaw, float threatRaw, float damageAverage, float threatAverage,
            float threatPerRage, float damagePerRage) {
        return attackText(damageRaw, threatRaw, damageAverage, threatAverage)
                + "\r\nThreat Per Rage: " + NetFormat.netFloatToString(threatPerRage)
                + "\r\nDamage Per Rage: " + NetFormat.netFloatToString(damagePerRage);
    }

    private String getRotationTooltip(String name) {
        StringBuilder tooltip = new StringBuilder("Ability Priority: \r\n");
        if (name.contains("Maul")) tooltip.append("Maul>");
        if (name.contains("Mangle")) tooltip.append("Mangle>");
        if (name.contains("Faerie")) tooltip.append("Faerie Fire>");
        if (name.contains("Swipe") && name.contains("Lacerate"))
            tooltip.append("Lacerate(+)>Swipe\r\n(+) means to only stack and refresh the debuff.");
        else if (name.contains("Swipe")) tooltip.append("Swipe");
        else if (name.contains("Lacerate")) tooltip.append("Lacerate");

        return tooltip.toString();
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "Hit Rating": return basicStats.getHitRating();
            case "Expertise Rating": return basicStats.getExpertiseRating();
            case "Haste Rating": return basicStats.getHasteRating();
            case "Avoided Attacks %": return getAvoidedAttacks() * 100f;
            case "Mitigation % from Armor": return constantDamageReduction * 100f;
            case "Avoidance %": return avoidancePostDR * 100f;
            case "% Chance to be Crit": return ((5f + (0.2f * (targetLevel - 80))) - critReduction * 100f);
            case "Nature Survival": return natureSurvivalPoints;
            case "Fire Survival": return fireSurvivalPoints;
            case "Frost Survival": return frostSurvivalPoints;
            case "Shadow Survival": return shadowSurvivalPoints;
            case "Arcane Survival": return arcaneSurvivalPoints;
            case "Nature Resist": return basicStats.getNatureResistance();
            case "Fire Resist": return basicStats.getFireResistance();
            case "Frost Resist": return basicStats.getFrostResistance();
            case "Shadow Resist": return basicStats.getShadowResistance();
            case "Arcane Resist": return basicStats.getArcaneResistance();
            case "Highest DPS": return highestDPSRotation.dps;
            case "Highest TPS": return highestTPSRotation.tps;
            case "Swipe DPS": return swipeRotation.dps;
            case "Swipe TPS": return swipeRotation.tps;
            case "Custom Rotation DPS": return customRotation.dps;
            case "Custom Rotation TPS": return customRotation.tps;
        }
        return 0f;
    }
}
