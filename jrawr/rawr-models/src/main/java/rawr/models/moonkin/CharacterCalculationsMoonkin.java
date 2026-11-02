// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/CharacterCalculationsMoonkin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET F/F0/F1/F2 formats (rawr.models.mage.NetFormat.f2,
// rawr.base.util.NetFormat.fixed) and the plain float ToString (rawr.models.mage.NetFormat.netFloatToString),
// plus the newline of Environment.NewLine on the oracle platform (Mono/Linux -> "\n").
package rawr.models.moonkin;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsMoonkin extends CharacterCalculationsBase {
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

    public float spellHit;
    public float spellCrit;
    public float spellHaste;
    public float spellPower;
    public float manaRegen;
    public float manaRegen5SR;
    public float latency;
    public int targetLevel;
    public float fightLength;
    public boolean scryer;
    public SpellRotation selectedRotation;
    public SpellRotation burstDPSRotation;
    public Map<String, RotationData> rotations;
    public Stats basicStats;
    public float starfallMana;
    public float treantDamage;

    public String getRotationName() {
        return selectedRotation.name;
    }

    public String getDpsRotationName() {
        return burstDPSRotation.name;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> retVal = new LinkedHashMap<String, String>();
        retVal.put("Health", rawr.models.mage.NetFormat.netFloatToString(basicStats.getHealth()));
        retVal.put("Mana", rawr.models.mage.NetFormat.netFloatToString(basicStats.getMana()));
        retVal.put("Armor", rawr.models.mage.NetFormat.netFloatToString(basicStats.getArmor()));
        retVal.put("Agility", rawr.models.mage.NetFormat.netFloatToString(basicStats.getAgility()));
        retVal.put("Stamina", rawr.models.mage.NetFormat.netFloatToString(basicStats.getStamina()));
        retVal.put("Intellect", rawr.models.mage.NetFormat.netFloatToString(basicStats.getIntellect()));
        retVal.put("Spirit", rawr.models.mage.NetFormat.netFloatToString(basicStats.getSpirit()));
        retVal.put("Spell Power", rawr.models.mage.NetFormat.netFloatToString(spellPower));
        retVal.put("Spell Hit", rawr.models.mage.NetFormat.f2(100 * spellHit)
                + "%*" + rawr.models.mage.NetFormat.netFloatToString(basicStats.getHitRating())
                + " Hit Rating, " + rawr.models.mage.NetFormat.f2(100 * StatConversion.getSpellHitFromRating(basicStats.getHitRating()))
                + "% Hit From Gear, " + rawr.models.mage.NetFormat.netFloatToString(StatConversion.getRatingFromHit(Math.max(0, 0.17f - spellHit)))
                + " Rating To Cap");
        retVal.put("Spell Crit", rawr.models.mage.NetFormat.f2(100 * spellCrit)
                + "%*" + rawr.models.mage.NetFormat.netFloatToString(basicStats.getCritRating())
                + " Crit Rating, " + rawr.models.mage.NetFormat.f2(100 * StatConversion.getSpellCritFromRating(basicStats.getCritRating()))
                + "% Crit From Gear, " + rawr.models.mage.NetFormat.f2(100 * StatConversion.getSpellCritFromIntellect(basicStats.getIntellect()))
                + "% Crit From Intellect");
        retVal.put("Spell Haste", rawr.models.mage.NetFormat.f2(100 * spellHaste)
                + "%*" + rawr.models.mage.NetFormat.netFloatToString(basicStats.getHasteRating())
                + " Haste Rating, " + rawr.models.mage.NetFormat.f2(100 * StatConversion.getSpellHasteFromRating(basicStats.getHasteRating()))
                + "% Haste From Gear");
        retVal.put("MP5 Not Casting", NetFormat.fixed(manaRegen * 5.0f, 0));
        retVal.put("MP5 While Casting", NetFormat.fixed(manaRegen5SR * 5.0f, 0));
        retVal.put("Total Score", rawr.models.mage.NetFormat.f2(subPoints[0] + subPoints[1]));
        retVal.put("Selected Rotation", selectedRotation.name);
        retVal.put("Selected DPS", rawr.models.mage.NetFormat.f2(selectedRotation.rotationData.dps));
        retVal.put("Selected Time To OOM", selectedRotation.rotationData.timeToOOM > 0
                ? selectedRotation.rotationData.timeToOOMMinutes() + " m " + selectedRotation.rotationData.timeToOOMSeconds() + " s"
                : "Not during fight");
        retVal.put("Selected Cycle Length", NetFormat.fixed(selectedRotation.duration, 1) + " s");

        StringBuilder sb = new StringBuilder("*");
        float rotationDamage = selectedRotation.rotationData.dps * selectedRotation.duration;
        sb.append("Starfire").append(": ").append(rawr.models.mage.NetFormat.f2(100 * selectedRotation.starfireAvgHit * selectedRotation.starfireCount / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(selectedRotation.starfireAvgHit * selectedRotation.starfireCount))
                .append(" damage, ").append(NetFormat.fixed(selectedRotation.starfireCount, 0)).append(" count").append("\n");
        sb.append("Moonfire").append(": ").append(rawr.models.mage.NetFormat.f2(100 * (selectedRotation.moonfireAvgHit) * selectedRotation.moonfireCasts / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2((selectedRotation.moonfireAvgHit) * selectedRotation.moonfireCasts))
                .append(" damage, ").append(NetFormat.fixed(selectedRotation.moonfireCasts, 0)).append(" count").append("\n");
        sb.append("Insect Swarm").append(": ").append(rawr.models.mage.NetFormat.f2(100 * selectedRotation.insectSwarmAvgHit * selectedRotation.insectSwarmCasts / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(selectedRotation.insectSwarmAvgHit * (selectedRotation.insectSwarmCasts)))
                .append(" damage, ").append(NetFormat.fixed(selectedRotation.insectSwarmCasts, 0)).append(" count").append("\n");
        sb.append("Wrath").append(": ").append(rawr.models.mage.NetFormat.f2(100 * selectedRotation.wrathAvgHit * selectedRotation.wrathCount / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(selectedRotation.wrathAvgHit * selectedRotation.wrathCount))
                .append(" damage, ").append(NetFormat.fixed(selectedRotation.wrathCount, 0)).append(" count").append("\n");

        retVal.put("Selected Spell Breakdown", sb.toString());
        retVal.put("Burst Rotation", burstDPSRotation.name);
        retVal.put("Burst DPS", rawr.models.mage.NetFormat.f2(burstDPSRotation.rotationData.burstDPS));
        retVal.put("Burst Time To OOM", burstDPSRotation.rotationData.timeToOOM > 0
                ? burstDPSRotation.rotationData.timeToOOMMinutes() + " m " + burstDPSRotation.rotationData.timeToOOMSeconds() + " s"
                : "Not during fight");
        retVal.put("Burst Cycle Length", NetFormat.fixed(burstDPSRotation.duration, 1) + " s");

        sb = new StringBuilder("*");
        rotationDamage = burstDPSRotation.rotationData.dps * burstDPSRotation.duration;
        sb.append("Starfire").append(": ").append(rawr.models.mage.NetFormat.f2(100 * burstDPSRotation.starfireAvgHit * burstDPSRotation.starfireCount / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(burstDPSRotation.starfireAvgHit * burstDPSRotation.starfireCount))
                .append(" damage, ").append(NetFormat.fixed(burstDPSRotation.starfireCount, 0)).append(" count").append("\n");
        // Ported bug: the Moonfire percentage uses SelectedRotation.MoonfireCasts (see SUSPECTED_BUGS).
        sb.append("Moonfire").append(": ").append(rawr.models.mage.NetFormat.f2(100 * burstDPSRotation.moonfireAvgHit * selectedRotation.moonfireCasts / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2((burstDPSRotation.moonfireAvgHit) * burstDPSRotation.moonfireCasts))
                .append(" damage, ").append(NetFormat.fixed(burstDPSRotation.moonfireCasts, 0)).append(" count").append("\n");
        sb.append("Insect Swarm").append(": ").append(rawr.models.mage.NetFormat.f2(100 * burstDPSRotation.insectSwarmAvgHit * burstDPSRotation.insectSwarmCasts / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(burstDPSRotation.insectSwarmAvgHit * burstDPSRotation.insectSwarmCasts))
                .append(" damage, ").append(NetFormat.fixed(burstDPSRotation.insectSwarmCasts, 0)).append(" count").append("\n");
        sb.append("Wrath").append(": ").append(rawr.models.mage.NetFormat.f2(100 * burstDPSRotation.wrathAvgHit * burstDPSRotation.wrathCount / rotationDamage))
                .append("%, ").append(rawr.models.mage.NetFormat.f2(burstDPSRotation.wrathAvgHit * burstDPSRotation.wrathCount))
                .append(" damage, ").append(NetFormat.fixed(burstDPSRotation.wrathCount, 0)).append(" count").append("\n");

        retVal.put("Burst Spell Breakdown", sb.toString());
        retVal.put("Starfire", rawr.models.mage.NetFormat.f2(selectedRotation.starfireAvgHit / (selectedRotation.starfireAvgCast > 0 ? selectedRotation.starfireAvgCast : 1f))
                + " dps*" + rawr.models.mage.NetFormat.f2(selectedRotation.starfireAvgCast)
                + " s avg\n" + rawr.models.mage.NetFormat.f2(selectedRotation.starfireNGCastTime)
                + " s w/NG\n" + rawr.models.mage.NetFormat.f2(100 * selectedRotation.starfireNonEclipseCrit)
                + "% max non-Eclipse\n" + rawr.models.mage.NetFormat.f2(100 * selectedRotation.starfireEclipseCrit)
                + "% max Eclipse\n" + rawr.models.mage.NetFormat.f2(selectedRotation.starfireAvgHit)
                + " avg hit\n" + NetFormat.fixed(selectedRotation.starfireManaCost, 0)
                + " avg mana");
        retVal.put("Wrath", rawr.models.mage.NetFormat.f2(selectedRotation.wrathAvgHit / (selectedRotation.wrathAvgCast > 0 ? selectedRotation.wrathAvgCast : 1f))
                + " dps*" + rawr.models.mage.NetFormat.f2(selectedRotation.wrathAvgCast)
                + " s avg\n" + rawr.models.mage.NetFormat.f2(selectedRotation.wrathNGCastTime)
                + " s w/NG\n" + rawr.models.mage.NetFormat.f2(selectedRotation.wrathAvgHit)
                + " avg hit\n" + NetFormat.fixed(selectedRotation.wrathManaCost, 0)
                + " avg mana");
        retVal.put("Moonfire", rawr.models.mage.NetFormat.f2(selectedRotation.moonfireAvgHit / (selectedRotation.moonfireDuration > 0 ? selectedRotation.moonfireDuration : 1f))
                + " dps*" + rawr.models.mage.NetFormat.f2(selectedRotation.moonfireCastTime)
                + " s avg\n" + rawr.models.mage.NetFormat.f2((selectedRotation.moonfireAvgHit))
                + " avg hit\n" + NetFormat.fixed(selectedRotation.moonfireManaCost, 0)
                + " avg mana");
        retVal.put("Insect Swarm", rawr.models.mage.NetFormat.f2(selectedRotation.insectSwarmAvgHit / (selectedRotation.insectSwarmDuration > 0 ? selectedRotation.insectSwarmDuration : 1f))
                + " dps*" + rawr.models.mage.NetFormat.f2(selectedRotation.insectSwarmCastTime)
                + " s avg\n" + rawr.models.mage.NetFormat.f2(selectedRotation.insectSwarmAvgHit)
                + " avg hit\n" + NetFormat.fixed(selectedRotation.insectSwarmManaCost, 0)
                + " avg mana");
        retVal.put("Starfall", rawr.models.mage.NetFormat.f2(selectedRotation.starfallDamage / 10.0f)
                + " dps*" + rawr.models.mage.NetFormat.f2(selectedRotation.starfallDamage)
                + " avg per cast\n" + rawr.models.mage.NetFormat.f2(selectedRotation.starfallDamage / (selectedRotation.starfallStars > 0 ? selectedRotation.starfallStars : 1f))
                + " avg per star\n" + NetFormat.fixed(starfallMana, 0)
                + " avg mana");
        retVal.put("Treants", rawr.models.mage.NetFormat.f2(treantDamage / 30.0f)
                + " dps*" + rawr.models.mage.NetFormat.f2(treantDamage)
                + " avg per cast\n" + rawr.models.mage.NetFormat.f2(treantDamage / 3.0f)
                + " avg per tree");

        return retVal;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Hit Rating": return basicStats.getHitRating();
            case "Haste Rating": return basicStats.getHasteRating();
            case "Crit Rating": return basicStats.getCritRating();
        }
        return 0;
    }
}
