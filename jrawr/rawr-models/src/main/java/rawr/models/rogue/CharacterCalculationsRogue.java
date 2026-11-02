// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Rogue/CharacterCalculationsRogue.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.rogue;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.models.rogue.RogueRotationCalculator.RogueRotationCalculation;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsRogue extends CharacterCalculationsBase {

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getDPSPoints() {
        return subPoints[0];
    }

    public void setDPSPoints(float value) {
        subPoints[0] = value;
    }

    public float getSurvivabilityPoints() {
        return subPoints[1];
    }

    public void setSurvivabilityPoints(float value) {
        subPoints[1] = value;
    }

    public Stats basicStats;
    public int targetLevel;

    public float avoidedWhiteMHAttacks;
    public float avoidedWhiteOHAttacks;
    public float avoidedAttacks;
    public float avoidedFinisherAttacks;
    public float avoidedPoisonAttacks;
    public float dodgedMHAttacks;
    public float dodgedOHAttacks;
    public float parriedAttacks;
    public float missedWhiteAttacks;
    public float missedAttacks;
    public float missedPoisonAttacks;
    public float critChanceYellow;
    public float critChanceMHTotal;
    public float critChanceMH;
    public float critChanceOHTotal;
    public float critChanceOH;
    public float mainHandSpeed;
    public float offHandSpeed;
    public float armorMitigationMH;
    public float armorMitigationOH;
    public float duration;

    public RogueAbilityStats mainHandStats;
    public RogueAbilityStats offHandStats;
    public RogueAbilityStats backstabStats;
    public RogueAbilityStats hemoStats;
    public RogueAbilityStats sStrikeStats;
    public RogueAbilityStats mutiStats;
    public RogueAbilityStats ruptStats;
    public RogueAbilityStats snDStats;
    public RogueAbilityStats evisStats;
    public RogueAbilityStats envenomStats;
    public RogueAbilityStats iPStats;
    public RogueAbilityStats dPStats;
    public RogueAbilityStats wPStats;
    public RogueAbilityStats aPStats;

    public RogueRotationCalculation highestDPSRotation;
    public RogueRotationCalculation customRotation;

    public String rotations;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        dictValues.put("Overall Points", RogueFormat.netFloatToString(overallPoints));
        dictValues.put("DPS Points", RogueFormat.netFloatToString(getDPSPoints()));
        dictValues.put("Survivability Points", RogueFormat.netFloatToString(getSurvivabilityPoints()));

        float baseMiss = StatConversion.WHITE_MISS_CHANCE_CAP_DW[targetLevel - 80] - basicStats.getPhysicalHit();
        float baseYellowMiss = StatConversion.YELLOW_MISS_CHANCE_CAP[targetLevel - 80] - basicStats.getPhysicalHit();
        float basePoisonMiss = StatConversion.getSpellMiss(80 - targetLevel, false) - basicStats.getSpellHit();
        float baseDodge = StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - StatConversion.getDodgeParryReducFromExpertise(basicStats.getExpertise());
        float baseParry = 0f;// StatConversion.WHITE_PARRY_CHANCE_CAP[TargetLevel - 80] - StatConversion.GetDodgeParryReducFromExpertise(BasicStats.Expertise);
        float baseWhiteMHCrit = critChanceMHTotal;
        float baseWhiteOHCrit = critChanceOHTotal;
        float capMiss = (float) Math.ceil(baseMiss * 100f * 32.78998947f);
        float capYellowMiss = (float) Math.ceil(baseYellowMiss * 100f * 32.78998947f);
        float capPoisonMiss = (float) Math.ceil(basePoisonMiss * 100f * 26.23f);
        float capDodge = (float) Math.ceil(baseDodge * 100f * 32.78998947f);
        float capParry = (float) Math.ceil(baseParry * 100f * 32.78998947f); // TODO: Check this value
        float capWhiteMHCrit = 100 - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * 100 - missedWhiteAttacks - dodgedMHAttacks;
        float capWhiteOHCrit = 100 - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * 100 - missedWhiteAttacks - dodgedOHAttacks;

        String tipMiss = "*White: ";
        if (basicStats.getHitRating() > capMiss)
            tipMiss += "Over the cap by " + RogueFormat.netFloatToString(basicStats.getHitRating() - capMiss) + " Hit Rating";
        else if (basicStats.getHitRating() < capMiss)
            tipMiss += "Under the cap by " + RogueFormat.netFloatToString(capMiss - basicStats.getHitRating()) + " Hit Rating";
        else
            tipMiss += "Exactly at the cap";

        tipMiss += "\r\nYellow: ";
        if (basicStats.getHitRating() > capYellowMiss)
            tipMiss += "Over the cap by " + RogueFormat.netFloatToString(basicStats.getHitRating() - capYellowMiss) + " Hit Rating";
        else if (basicStats.getHitRating() < capYellowMiss)
            tipMiss += "Under the cap by " + RogueFormat.netFloatToString(capYellowMiss - basicStats.getHitRating()) + " Hit Rating";
        else
            tipMiss += "Exactly at the cap";

        tipMiss += "\r\nPoison: ";
        if (basicStats.getHitRating() > capPoisonMiss)
            tipMiss += "Over the cap by " + RogueFormat.netFloatToString(basicStats.getHitRating() - capPoisonMiss) + " Hit Rating";
        else if (basicStats.getHitRating() < capPoisonMiss)
            tipMiss += "Under the cap by " + RogueFormat.netFloatToString(capPoisonMiss - basicStats.getHitRating()) + " Hit Rating";
        else
            tipMiss += "Exactly at the cap";

        String tipDodge = "";
        if (basicStats.getExpertiseRating() > capDodge)
            tipDodge = "*Over the cap by " + RogueFormat.netFloatToString(basicStats.getExpertiseRating() - capDodge) + " Expertise Rating";
        else if (basicStats.getExpertiseRating() < capDodge)
            tipDodge = "*Under the cap by " + RogueFormat.netFloatToString(capDodge - basicStats.getExpertiseRating()) + " Expertise Rating";
        else
            tipDodge = "*Exactly at the cap";

        String tipCrit = "Mainhand: " + RogueFormat.netFloatToString(critChanceMH) + ", ";
        if (critChanceMHTotal > capWhiteMHCrit)
            tipCrit += "over the Crit cap by " + RogueFormat.netFloatToString(critChanceMHTotal - capWhiteMHCrit) + "%";
        else if (critChanceMHTotal < capWhiteMHCrit)
            tipCrit += "under the Crit cap by " + RogueFormat.netFloatToString(capWhiteMHCrit - critChanceMHTotal) + "%";
        else tipCrit += "exactly at the Crit cap";

        tipCrit += "\nOffhand: " + RogueFormat.netFloatToString(critChanceOH) + ", ";
        if (critChanceOHTotal > capWhiteOHCrit)
            tipCrit += "over the Crit cap by " + RogueFormat.netFloatToString(critChanceOHTotal - capWhiteOHCrit) + "%";
        else if (critChanceOHTotal < capWhiteOHCrit)
            tipCrit += "under the Crit cap by " + RogueFormat.netFloatToString(capWhiteOHCrit - critChanceOHTotal) + "%";
        else tipCrit += "exactly at the Crit cap";

        dictValues.put("Health", RogueFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Attack Power", RogueFormat.netFloatToString(basicStats.getAttackPower()));
        dictValues.put("Agility", RogueFormat.netFloatToString(basicStats.getAgility()));
        dictValues.put("Strength", RogueFormat.netFloatToString(basicStats.getStrength()));
        dictValues.put("Crit Rating", RogueFormat.netFloatToString(basicStats.getCritRating()));
        dictValues.put("Hit Rating", RogueFormat.netFloatToString(basicStats.getHitRating()) + tipMiss);
        dictValues.put("Expertise Rating", RogueFormat.netFloatToString(basicStats.getExpertiseRating()) + tipDodge);
        dictValues.put("Haste Rating", RogueFormat.netFloatToString(basicStats.getHasteRating()));
        dictValues.put("Armor Penetration Rating", RogueFormat.netFloatToString(basicStats.getArmorPenetrationRating()));
        dictValues.put("Weapon Damage", "+" + RogueFormat.netFloatToString(basicStats.getWeaponDamage()));

        dictValues.put("Avoided White Attacks", RogueFormat.netFloatToString(avoidedWhiteMHAttacks) + "% / "
                + RogueFormat.netFloatToString(avoidedWhiteOHAttacks) + "%*Mainhand: "
                + RogueFormat.netFloatToString(dodgedMHAttacks) + "% Dodged, "
                + RogueFormat.netFloatToString(missedWhiteAttacks) + "% Missed\n   Offhand: "
                + RogueFormat.netFloatToString(dodgedOHAttacks) + "% Dodged, "
                + RogueFormat.netFloatToString(missedWhiteAttacks) + "% Missed");
        dictValues.put("Avoided Yellow Attacks", RogueFormat.netFloatToString(avoidedAttacks) + "%*"
                + RogueFormat.netFloatToString(dodgedMHAttacks) + "% Dodged, "
                + RogueFormat.netFloatToString(missedAttacks) + "% Missed");
        dictValues.put("Avoided Poison Attacks", RogueFormat.netFloatToString(avoidedPoisonAttacks) + "%*"
                + RogueFormat.netFloatToString(missedPoisonAttacks) + "% Missed");
        dictValues.put("Crit Chance", RogueFormat.netFloatToString(critChanceYellow) + "%*" + tipCrit);
        dictValues.put("MainHand Speed", RogueFormat.netFloatToString(mainHandSpeed) + "s");
        dictValues.put("OffHand Speed", RogueFormat.netFloatToString(offHandSpeed) + "s");
        dictValues.put("Armor Mitigation MainHand", RogueFormat.netFloatToString(armorMitigationMH) + "%");
        dictValues.put("Armor Mitigation OffHand", RogueFormat.netFloatToString(armorMitigationOH) + "%");

        dictValues.put("Optimal Rotation", highestDPSRotation.toString());
        dictValues.put("Optimal Rotation DPS", RogueFormat.netFloatToString(highestDPSRotation.dps));
        dictValues.put("Custom Rotation DPS", RogueFormat.netFloatToString(customRotation.dps));

        float chanceWhiteMHNonAvoided = 1f - (avoidedWhiteMHAttacks / 100f);
        float chanceWhiteOHNonAvoided = 1f - (avoidedWhiteOHAttacks / 100f);
        float chanceNonAvoided = 1f - (avoidedAttacks / 100f);
        float chancePoisonNonAvoided = 1f - (avoidedPoisonAttacks / 100f);
        dictValues.put("MainHand", mainHandStats.getStatsTexts(highestDPSRotation.mainHandCount, 0, highestDPSRotation.totalDamage, chanceWhiteMHNonAvoided, duration));
        dictValues.put("OffHand", offHandStats.getStatsTexts(highestDPSRotation.offHandCount, 0, highestDPSRotation.totalDamage, chanceWhiteOHNonAvoided, duration));
        dictValues.put("Backstab", backstabStats.getStatsTexts(highestDPSRotation.backstabCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Hemorrhage", hemoStats.getStatsTexts(highestDPSRotation.hemoCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Sinister Strike", sStrikeStats.getStatsTexts(highestDPSRotation.sStrikeCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Mutilate", mutiStats.getStatsTexts(highestDPSRotation.mutiCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Rupture", ruptStats.getStatsTexts(highestDPSRotation.ruptCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Slice and Dice", snDStats.getStatsTexts(highestDPSRotation.snDCount, highestDPSRotation.snDCP, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Eviscerate", evisStats.getStatsTexts(highestDPSRotation.evisCount, Math.max(highestDPSRotation.evisCP, highestDPSRotation.envenomCP), highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Envenom", envenomStats.getStatsTexts(highestDPSRotation.envenomCount, Math.max(highestDPSRotation.evisCP, highestDPSRotation.envenomCP), highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Instant Poison", iPStats.getStatsTexts(highestDPSRotation.iPCount, 0, highestDPSRotation.totalDamage, chancePoisonNonAvoided, duration));
        dictValues.put("Deadly Poison", dPStats.getStatsTexts(highestDPSRotation.dPCount, 0, highestDPSRotation.totalDamage, chancePoisonNonAvoided, duration));
        dictValues.put("Wound Poison", wPStats.getStatsTexts(highestDPSRotation.wPCount, 0, highestDPSRotation.totalDamage, chancePoisonNonAvoided, duration));
        dictValues.put("Anesthetic Poison", aPStats.getStatsTexts(highestDPSRotation.aPCount, 0, highestDPSRotation.totalDamage, chancePoisonNonAvoided, duration));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Avoided Yellow Attacks %": return avoidedAttacks;
            case "Custom Rotation DPS": return customRotation.dps;
        }
        return 0f;
    }
}
