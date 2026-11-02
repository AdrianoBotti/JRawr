// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/CharacterCalculationsRetribution.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET N0/N2 and P formats via rawr.base.util.NetFormat (NetFormat.percent is the
// custom `0%` variant; the standard `P` pattern appends " %" with invariant culture).
package rawr.models.retribution;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CharacterCalculationsRetribution extends CharacterCalculationsBase {

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f };

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

    public RotationSolution solution;
    public Ability[] rotation;

    public float whiteDPS;
    public float sealDPS;
    public float crusaderStrikeDPS;
    public float divineStormDPS;
    public float judgementDPS;
    public float consecrationDPS;
    public float exorcismDPS;
    public float handOfReckoningDPS;
    public float hammerOfWrathDPS;
    public float otherDPS;

    public Skill whiteSkill;
    public Skill sealSkill;
    public Skill crusaderStrikeSkill;
    public Skill divineStormSkill;
    public Skill judgementSkill;
    public Skill consecrationSkill;
    public Skill exorcismSkill;
    public Skill handOfReckoningSkill;
    public Skill hammerOfWrathSkill;

    public float toMiss;
    public float toBeDodged;
    public float toBeParried;
    public float toBeResisted;

    public float averageSoVStack;
    public float soVOvertake;
    public float weaponDamage;
    public float attackSpeed;
    public Stats basicStats;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        // Status text
        dictValues.put("Status", String.format(Locale.ROOT, "%s dps", NetFormat.grouped(getDPSPoints(), 0)));

        // Basic stats
        dictValues.put("Health", NetFormat.grouped(basicStats.getHealth(), 0));
        dictValues.put("Mana", NetFormat.grouped(basicStats.getMana(), 0));
        dictValues.put("Strength", NetFormat.grouped(basicStats.getStrength(), 0));
        dictValues.put("Agility", NetFormat.fixed(basicStats.getAgility(), 0));
        dictValues.put("Attack Power", NetFormat.grouped(basicStats.getAttackPower(), 0));
        dictValues.put("Crit Chance", netPercent(basicStats.getPhysicalCrit()) + "*" + NetFormat.fixed(basicStats.getCritRating(), 0) + " crit rating");
        dictValues.put("Miss Chance", netPercent(toMiss) + "*" + netPercent(basicStats.getPhysicalHit()) + " hit (" + NetFormat.fixed(basicStats.getHitRating(), 0) + " rating)\n");
        dictValues.put("Dodge Chance", netPercent(toBeDodged) + "*" + netPercent(basicStats.getExpertise() * .0025f) + " expertise (" + NetFormat.fixed(basicStats.getExpertiseRating(), 0) + " rating)");
        dictValues.put("Melee Haste", netPercent(basicStats.getPhysicalHaste()) + "*" + NetFormat.fixed(basicStats.getHasteRating(), 0) + " haste rating");
        dictValues.put("Weapon Damage", NetFormat.grouped(weaponDamage, 2));
        dictValues.put("Attack Speed", NetFormat.grouped(attackSpeed, 2));

        // DPS Breakdown
        dictValues.put("Total DPS", NetFormat.grouped(overallPoints, 0));
        dictValues.put("White", NetFormat.grouped(whiteDPS, 0) + "*" + whiteSkill.toString());
        dictValues.put("Seal", NetFormat.grouped(sealDPS, 0) + "*" + sealSkill.toString());
        dictValues.put("Crusader Strike", NetFormat.grouped(crusaderStrikeDPS, 0) + "*" + crusaderStrikeSkill.toString());
        dictValues.put("Judgement", NetFormat.grouped(judgementDPS, 0) + "*" + judgementSkill.toString());
        dictValues.put("Consecration", NetFormat.grouped(consecrationDPS, 0) + "*" + consecrationSkill.toString());
        dictValues.put("Exorcism", NetFormat.grouped(exorcismDPS, 0) + "*" + exorcismSkill.toString());
        dictValues.put("Divine Storm", NetFormat.grouped(divineStormDPS, 0) + "*" + divineStormSkill.toString());
        dictValues.put("Hammer of Wrath", NetFormat.grouped(hammerOfWrathDPS, 0) + "*" + hammerOfWrathSkill.toString());
        dictValues.put("Hand of Reckoning", NetFormat.grouped(handOfReckoningDPS, 0) + "*" + handOfReckoningSkill.toString());
        dictValues.put("Other", NetFormat.grouped(otherDPS, 0));

        // Rotation Info:
        dictValues.put("Chosen Rotation", rotation == null ?
                "n/a" :
                SimulatorParameters.shortRotationString(rotation));  // TODO: Remove dependancy on SimulatorParameters.
        dictValues.put("Average SoV Stack", NetFormat.grouped(averageSoVStack, 2));
        dictValues.put("SoV Overtake", String.format(Locale.ROOT, "%s sec", NetFormat.grouped(soVOvertake, 2)));
        dictValues.put("Crusader Strike CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.CrusaderStrike), 2));
        dictValues.put("Judgement CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.Judgement), 2));
        dictValues.put("Consecration CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.Consecration), 2));
        dictValues.put("Exorcism CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.Exorcism), 2));
        dictValues.put("Divine Storm CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.DivineStorm), 2));
        dictValues.put("Hammer of Wrath CD",
                NetFormat.grouped(solution.getAbilityEffectiveCooldown(Ability.HammerOfWrath), 2));

        return dictValues;
    }

    /** .NET standard `P` format on a Single: G7 pre-round, x100, 2 decimals, invariant " %" suffix. */
    private static String netPercent(float value) {
        return NetFormat.percent(value, 2).replace("%", " %");
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "% Chance to Miss (Melee)": return toMiss * 100f;  // White and Melee hit for ret are identical since we can't dual wield.
            case "% Chance to Miss (Spells)": return toBeResisted * 100f;
            case "% Chance to be Dodged": return toBeDodged * 100f;
            case "% Chance to be Parried": return toBeParried * 100f;
            case "% Chance to be Avoided (Yellow/Dodge)": return (toMiss + toBeDodged) * 100f;
        }
        return 0f;
    }
}
