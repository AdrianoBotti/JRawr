// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/CharacterCalculationsTankDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use rawr.base.util.NetFormat.fixed for the `F0`/`F1`/`F2` and `{0:0.0}` custom formats
// (Mono pre-rounds to 7 significant digits) and rawr.models.mage.NetFormat.f2 for the float
// ToString("F2") calls.
package rawr.models.tankdk;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsTankDK extends CharacterCalculationsBase {
    public CalculationType cType = CalculationType.SMT;

    @Override
    public float getOverallPoints() {
        if (cType == CalculationType.Burst) {
            return (BurstTime + ReactionTime);
        } else {
            return ((Survival * SurvivalWeight) + (Mitigation * MitigationWeight) + (Threat * ThreatWeight));
        }
    }

    @Override
    public void setOverallPoints(float value) {
    }

    public float PhysicalSurvival;
    public float BleedSurvival;
    public float MagicSurvival;

    public Stats BasicStats;
    public int TargetLevel;

    public float Dodge;
    public float Miss;
    public float Parry;

    public float Survival;
    public float Mitigation;
    public float Threat;

    public float SurvivalWeight;
    public float MitigationWeight;
    public float ThreatWeight;

    public float MagicDamageReduction;
    public float ArmorDamageReduction;
    public float Armor;

    public float Crit;
    public float Defense;
    public float Resilience;
    public float DefenseRating;
    public float DefenseRatingNeeded;

    public float TargetMiss;
    public float TargetDodge;
    public float TargetParry;

    public float BurstTime;
    public float ReactionTime;

    public float Expertise;

    // Subpoints
    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        if (cType == CalculationType.Burst) {
            return new float[] { BurstTime, ReactionTime, 0f };
        } else {
            return new float[] { Survival * SurvivalWeight, Mitigation * MitigationWeight, Threat * ThreatWeight };
        }
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    // Experimental Threat & DPS info
    public float DPS;
    public int RSLimit;
    public int TotalThreat;

    // Costs
    public float RotationTime;
    public int Blood;
    public int Unholy;
    public int Frost;
    public int Death;
    public int RP;

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Chance to be Crit": return Crit; // Def cap chance to be critted by boss.  For optimization this needs to be  <= 0
            case "Avoidance %": return (Miss + Parry + Dodge); // Another duplicate math location?
            case "% Chance to Hit": return (1f - TargetMiss) * 100.0f; // +Hit related
            case "Target Parry %": return TargetParry * 100.0f; // Expertise related.
            case "Target Dodge %": return TargetDodge * 100.0f; // Expertise related.
            case "Damage Reduction %": return ArmorDamageReduction * 100.0f; // % Damage reduction by Armor
            case "Armor": return Armor; // Raw Armor
            case "Health": return BasicStats.getHealth();
            case "Hit Rating": return BasicStats.getHitRating(); // Raw Hit Rating
            case "Burst Time": return BurstTime;
            case "Reaction Time": return ReactionTime;
            case "Resilience": return Resilience;
            case "Spell Penetration": return BasicStats.getSpellPenetration();
            default: return 0.0f;
        }
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dict = new LinkedHashMap<String, String>();

        dict.put("Miss", f2(Miss) + "%");
        dict.put("Dodge", f2(Dodge) + "%");
        dict.put("Parry", f2(Parry) + "%");
        dict.put("Armor Damage Reduction", f2(ArmorDamageReduction * 100.0f) + "%");
        dict.put("Magic Damage Reduction", f2(MagicDamageReduction * 100.0f) + "%"
                + "*Arcane: " + rawr.base.util.NetFormat.fixed(BasicStats.getArcaneResistance(), 0) + "\n"
                + "Fire: " + rawr.base.util.NetFormat.fixed(BasicStats.getFireResistance(), 0) + "\n"
                + "Frost: " + rawr.base.util.NetFormat.fixed(BasicStats.getFrostResistance(), 0) + "\n"
                + "Nature: " + rawr.base.util.NetFormat.fixed(BasicStats.getNatureResistance(), 0) + "\n"
                + "Shadow: " + rawr.base.util.NetFormat.fixed(BasicStats.getShadowResistance(), 0));

        dict.put("Total Avoidance", f2(Miss + Parry + Dodge) + "%"); // Another duplicate math location.
        dict.put("Burst Time", rawr.base.util.NetFormat.fixed(BurstTime, 1) + " sec");
        dict.put("Reaction Time", rawr.base.util.NetFormat.fixed(ReactionTime, 1) + " sec");

        dict.put("Health", rawr.base.util.NetFormat.fixed(BasicStats.getHealth(), 0));
        dict.put("Armor", rawr.base.util.NetFormat.fixed(BasicStats.getArmor(), 0));
        dict.put("Strength", rawr.base.util.NetFormat.fixed(BasicStats.getStrength(), 0));
        dict.put("Agility", rawr.base.util.NetFormat.fixed(BasicStats.getAgility(), 0));
        dict.put("Stamina", rawr.base.util.NetFormat.fixed(BasicStats.getStamina(), 0));
        dict.put("Hit Rating", rawr.base.util.NetFormat.fixed(BasicStats.getHitRating(), 0));
        dict.put("Haste Rating", rawr.base.util.NetFormat.fixed(BasicStats.getHasteRating(), 0));
        dict.put("Crit Rating", rawr.base.util.NetFormat.fixed(BasicStats.getCritRating(), 0));
        dict.put("Physical Crit", f2(BasicStats.getPhysicalCrit() * 100f));
        dict.put("Expertise", rawr.base.util.NetFormat.fixed(Expertise, 0));
        dict.put("Attack Power", rawr.base.util.NetFormat.fixed(BasicStats.getAttackPower(), 0));
        dict.put("Armor Penetration", f2(BasicStats.getArmorPenetration() * 100f) + "%");
        dict.put("Armor Penetration Rating", rawr.base.util.NetFormat.fixed(BasicStats.getArmorPenetrationRating(), 0));

        dict.put("DPS", rawr.base.util.NetFormat.fixed(DPS, 0));
        dict.put("Rotation Time", rawr.base.util.NetFormat.fixed((RotationTime / 1000), 2) + " sec");
        dict.put("Total Threat", rawr.base.util.NetFormat.fixed(TotalThreat, 0));

        // Rune Strike Limit
        dict.put("RS Limited", "");
        if (RSLimit == RSState.Good.ordinal())
            dict.put("RS Limited", "none");
        else {
            if (1 == (RSLimit & RSState.TimeStarved.ordinal()))
                dict.put("RS Limited", dict.get("RS Limited") + "Swing Starved ");
            if (1 == (RSLimit & RSState.RPStarved.ordinal()))
                dict.put("RS Limited", dict.get("RS Limited") + "RP Starved ");
        }
        // Ability Costs
        dict.put("Blood", rawr.base.util.NetFormat.fixed(Blood, 0));
        dict.put("Frost", rawr.base.util.NetFormat.fixed(Frost, 0));
        dict.put("Unholy", rawr.base.util.NetFormat.fixed(Unholy, 0));
        dict.put("Death", rawr.base.util.NetFormat.fixed(Death, 0));
        dict.put("Runic Power", rawr.base.util.NetFormat.fixed(RP, 0));

        dict.put("Overall Points", rawr.base.util.NetFormat.fixed(getOverallPoints(), 1));
        dict.put("Mitigation Points", rawr.base.util.NetFormat.fixed((Mitigation * MitigationWeight), 1)); // Modified Mitigation.
        dict.put("Survival Points", rawr.base.util.NetFormat.fixed((Survival * SurvivalWeight), 1)
                + "*Physical:" + rawr.base.util.NetFormat.fixed((PhysicalSurvival * SurvivalWeight), 1) + "\n"
                + "Bleed:" + rawr.base.util.NetFormat.fixed((BleedSurvival * SurvivalWeight), 1) + "\n"
                + "Magic:" + rawr.base.util.NetFormat.fixed((MagicSurvival * SurvivalWeight), 1)); // Modified Survival
        dict.put("Threat Points", rawr.base.util.NetFormat.fixed((Threat * ThreatWeight), 1)); // Modified Threat

        dict.put("Crit", f2(Crit));
        dict.put("Defense", rawr.base.util.NetFormat.fixed(Defense, 0));
        dict.put("Resilience", rawr.base.util.NetFormat.fixed(Resilience, 0));
        dict.put("Defense Rating", rawr.base.util.NetFormat.fixed(DefenseRating, 0));
        dict.put("Defense Rating needed", rawr.base.util.NetFormat.fixed(DefenseRatingNeeded, 0));

        dict.put("Target Miss", rawr.base.util.NetFormat.fixed((TargetMiss * 100.0f), 1) + "%");
        dict.put("Target Dodge", rawr.base.util.NetFormat.fixed((TargetDodge * 100.0f), 1) + "%");
        dict.put("Target Parry", rawr.base.util.NetFormat.fixed((TargetParry * 100.0f), 1) + "%");

        return dict;
    }

    /** .NET float ToString("F2") via the Mage display helper. */
    private static String f2(float value) {
        return rawr.models.mage.NetFormat.f2(value);
    }
}
