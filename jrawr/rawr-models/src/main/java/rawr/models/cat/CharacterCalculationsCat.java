// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Cat/CharacterCalculationsCat.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values: plain float ToString() uses the Mage display helper (G7), string.Format is
// String.format(Locale.ROOT, ...) with the ported CatAbilityStats formatters.
package rawr.models.cat;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.models.mage.NetFormat;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsCat extends CharacterCalculationsBase {
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

    public float getDPSPoints() {
        return this.subPoints[0];
    }

    public void setDPSPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getSurvivabilityPoints() {
        return this.subPoints[1];
    }

    public void setSurvivabilityPoints(float value) {
        this.subPoints[1] = value;
    }

    public Stats basicStats;
    public int targetLevel;

    public float avoidedAttacks;
    public float dodgedAttacks;
    public float parriedAttacks;
    public float missedAttacks;
    public float critChance;
    public float attackSpeed;
    public float armorMitigation;
    public float duration;

    public CatAbilityStats meleeStats;
    public CatAbilityStats mangleStats;
    public CatAbilityStats shredStats;
    public CatAbilityStats rakeStats;
    public CatAbilityStats ripStats;
    public CatAbilityStats roarStats;
    public CatAbilityStats biteStats;

    public CatRotationCalculator.CatRotationCalculation highestDPSRotation;
    public CatRotationCalculator.CatRotationCalculation customRotation;

    public String rotations;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        dictValues.put("Overall Points", NetFormat.netFloatToString(getOverallPoints()));
        dictValues.put("DPS Points", NetFormat.netFloatToString(getDPSPoints()));
        dictValues.put("Survivability Points", NetFormat.netFloatToString(getSurvivabilityPoints()));

        float baseMiss = StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - basicStats.getPhysicalHit();
        float baseDodge = StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - StatConversion.getDodgeParryReducFromExpertise(basicStats.getExpertise());
        float baseParry = 0f;// StatConversion.WHITE_PARRY_CHANCE_CAP[TargetLevel - 80] - StatConversion.GetDodgeParryReducFromExpertise(BasicStats.Expertise);
        float capMiss = (float) Math.ceil(baseMiss * 100f * 32.78998947f);
        float capDodge = (float) Math.ceil(baseDodge * 100f * 32.78998947f);
        float capParry = (float) Math.ceil(baseParry * 100f * 32.78998947f); // TODO: Check this value

        String tipMiss;
        if (basicStats.getHitRating() > capMiss)
            tipMiss = "*Over the cap by " + NetFormat.netFloatToString(basicStats.getHitRating() - capMiss) + " Hit Rating";
        else if (basicStats.getHitRating() < capMiss)
            tipMiss = "*Under the cap by " + NetFormat.netFloatToString(capMiss - basicStats.getHitRating()) + " Hit Rating";
        else
            tipMiss = "*Exactly at the cap";

        String tipDodge;
        if (basicStats.getExpertiseRating() > capDodge)
            tipDodge = "*Over the cap by " + NetFormat.netFloatToString(basicStats.getExpertiseRating() - capDodge) + " Expertise Rating";
        else if (basicStats.getExpertiseRating() < capDodge)
            tipDodge = "*Under the cap by " + NetFormat.netFloatToString(capDodge - basicStats.getExpertiseRating()) + " Expertise Rating";
        else
            tipDodge = "*Exactly at the cap";

        dictValues.put("Health", NetFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Attack Power", NetFormat.netFloatToString(basicStats.getAttackPower()));
        dictValues.put("Agility", NetFormat.netFloatToString(basicStats.getAgility()));
        dictValues.put("Strength", NetFormat.netFloatToString(basicStats.getStrength()));
        dictValues.put("Crit Rating", NetFormat.netFloatToString(basicStats.getCritRating()));
        dictValues.put("Hit Rating", NetFormat.netFloatToString(basicStats.getHitRating()) + tipMiss);
        dictValues.put("Expertise Rating", NetFormat.netFloatToString(basicStats.getExpertiseRating()) + tipDodge);
        dictValues.put("Haste Rating", NetFormat.netFloatToString(basicStats.getHasteRating()));
        dictValues.put("Armor Penetration Rating", NetFormat.netFloatToString(basicStats.getArmorPenetrationRating()));
        dictValues.put("Weapon Damage", "+" + NetFormat.netFloatToString(basicStats.getWeaponDamage()));

        dictValues.put("Avoided Attacks", NetFormat.netFloatToString(avoidedAttacks) + "%*" + NetFormat.netFloatToString(dodgedAttacks)
                + "% Dodged, " + NetFormat.netFloatToString(missedAttacks) + "% Missed");
        dictValues.put("Crit Chance", NetFormat.netFloatToString(critChance) + "%");
        dictValues.put("Attack Speed", NetFormat.netFloatToString(attackSpeed) + "s");
        dictValues.put("Armor Mitigation", NetFormat.netFloatToString(armorMitigation) + "%");

        dictValues.put("Optimal Rotation", highestDPSRotation.toString());
        dictValues.put("Optimal Rotation DPS", NetFormat.netFloatToString(highestDPSRotation.dps));
        dictValues.put("Custom Rotation DPS", NetFormat.netFloatToString(customRotation.dps));

        float chanceNonAvoided = 1f - (avoidedAttacks / 100f);
        dictValues.put("Melee", meleeStats.getStatsTexts(highestDPSRotation.meleeCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Mangle", mangleStats.getStatsTexts(highestDPSRotation.mangleCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Shred", shredStats.getStatsTexts(highestDPSRotation.shredCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Rake", rakeStats.getStatsTexts(highestDPSRotation.rakeCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Rip", ripStats.getStatsTexts(highestDPSRotation.ripCount, 0, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Roar", roarStats.getStatsTexts(highestDPSRotation.roarCount, highestDPSRotation.roarCP, highestDPSRotation.totalDamage, chanceNonAvoided, duration));
        dictValues.put("Bite", biteStats.getStatsTexts(highestDPSRotation.biteCount, highestDPSRotation.biteCP, highestDPSRotation.totalDamage, chanceNonAvoided, duration));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "Avoided Attacks %": return avoidedAttacks;
            case "Nature Resist": return basicStats.getNatureResistance();
            case "Fire Resist": return basicStats.getFireResistance();
            case "Frost Resist": return basicStats.getFrostResistance();
            case "Shadow Resist": return basicStats.getShadowResistance();
            case "Arcane Resist": return basicStats.getArcaneResistance();
            case "Custom Rotation DPS": return customRotation.dps;
        }
        return 0f;
    }
}
