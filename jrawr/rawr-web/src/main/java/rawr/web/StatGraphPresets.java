package rawr.web;

import java.util.List;

/**
 * Per-model stat lines for the Stat Graph, taken from the legacy Rawr Stats Graph/Scaling chart
 * lists (Mage/Elemental/Moonkin/Tree/RestoSham custom charts, and the DPSWarr/Enhance/Hunter/
 * TankDK/DPSDK option panels). Models without a legacy graph get a short curated list of their
 * primary stats instead of the model's full relevance whitelist, which would include utility
 * stats such as PVP trinket, movement speed and resistances.
 *
 * <p>The weight is the amount of the stat that one x-step represents; the legacy Mage chart used
 * 1.17 Spell Power and 0.4 Mp5, the melee panels used 2 Attack Power. Everything else is 1.</p>
 */
final class StatGraphPresets {
    record Entry(String key, float weight) {
        Entry(String key) {
            this(key, 1f);
        }
    }

    private StatGraphPresets() {
    }

    static List<Entry> forModel(String model) {
        if (model == null) {
            return List.of();
        }
        return switch (model) {
            case "Mage" -> List.of(
                    new Entry("SpellPower", 1.17f), new Entry("Mp5", 0.4f),
                    new Entry("CritRating"), new Entry("HasteRating"), new Entry("HitRating"),
                    new Entry("Intellect"), new Entry("Spirit"));
            case "Elemental" -> List.of(
                    new Entry("SpellPower"), new Entry("Mp5"), new Entry("CritRating"),
                    new Entry("HasteRating"), new Entry("Intellect"), new Entry("Spirit"));
            case "Moonkin" -> List.of(
                    new Entry("SpellPower"), new Entry("Mp5"), new Entry("HitRating"),
                    new Entry("CritRating"), new Entry("HasteRating"), new Entry("Intellect"),
                    new Entry("Spirit"));
            case "Tree" -> List.of(
                    new Entry("SpellPower"), new Entry("Mp5"), new Entry("CritRating"),
                    new Entry("HasteRating"), new Entry("Intellect"), new Entry("Spirit"));
            case "RestoSham", "HealPriest", "Healadin" -> List.of(
                    new Entry("SpellPower"), new Entry("Mp5"), new Entry("CritRating"),
                    new Entry("HasteRating"), new Entry("Intellect"), new Entry("Spirit"));
            case "ShadowPriest", "Warlock" -> List.of(
                    new Entry("SpellPower"), new Entry("Intellect"), new Entry("CritRating"),
                    new Entry("HasteRating"), new Entry("HitRating"), new Entry("Spirit"),
                    new Entry("Mp5"));
            case "DPSWarr" -> List.of(
                    new Entry("Strength"), new Entry("Agility"), new Entry("AttackPower", 2f),
                    new Entry("CritRating"), new Entry("HitRating"), new Entry("ExpertiseRating"),
                    new Entry("HasteRating"), new Entry("ArmorPenetrationRating"));
            case "Enhance" -> List.of(
                    new Entry("Strength"), new Entry("Agility"), new Entry("AttackPower", 2f),
                    new Entry("CritRating"), new Entry("HitRating"), new Entry("ExpertiseRating"),
                    new Entry("HasteRating"), new Entry("ArmorPenetrationRating"),
                    new Entry("SpellPower", 1.15f), new Entry("Intellect"));
            case "Hunter" -> List.of(
                    new Entry("Agility"), new Entry("AttackPower"), new Entry("CritRating"),
                    new Entry("HitRating"), new Entry("HasteRating"),
                    new Entry("ArmorPenetrationRating"));
            case "TankDK" -> List.of(
                    new Entry("Strength"), new Entry("Stamina"), new Entry("Agility"),
                    new Entry("AttackPower", 2f), new Entry("CritRating"), new Entry("HitRating"),
                    new Entry("ExpertiseRating"), new Entry("HasteRating"),
                    new Entry("ArmorPenetrationRating"), new Entry("DefenseRating"),
                    new Entry("DodgeRating"), new Entry("ParryRating"));
            case "DPSDK" -> List.of(
                    new Entry("Strength"), new Entry("Agility"), new Entry("AttackPower", 2f),
                    new Entry("CritRating"), new Entry("HitRating"), new Entry("ExpertiseRating"),
                    new Entry("HasteRating"), new Entry("ArmorPenetrationRating"));
            case "Cat", "Rogue" -> List.of(
                    new Entry("Agility"), new Entry("Strength"), new Entry("AttackPower"),
                    new Entry("CritRating"), new Entry("HitRating"), new Entry("ExpertiseRating"),
                    new Entry("HasteRating"), new Entry("ArmorPenetrationRating"));
            case "Bear" -> List.of(
                    new Entry("Stamina"), new Entry("Agility"), new Entry("Strength"),
                    new Entry("AttackPower"), new Entry("CritRating"), new Entry("HitRating"),
                    new Entry("ExpertiseRating"), new Entry("HasteRating"),
                    new Entry("ArmorPenetrationRating"), new Entry("DefenseRating"),
                    new Entry("DodgeRating"), new Entry("ParryRating"));
            case "ProtWarr" -> List.of(
                    new Entry("Strength"), new Entry("Stamina"), new Entry("Agility"),
                    new Entry("AttackPower"), new Entry("CritRating"), new Entry("HitRating"),
                    new Entry("ExpertiseRating"), new Entry("HasteRating"),
                    new Entry("ArmorPenetrationRating"), new Entry("DefenseRating"),
                    new Entry("DodgeRating"), new Entry("ParryRating"));
            case "ProtPaladin" -> List.of(
                    new Entry("Stamina"), new Entry("Strength"), new Entry("Agility"),
                    new Entry("SpellPower"), new Entry("CritRating"), new Entry("HitRating"),
                    new Entry("ExpertiseRating"), new Entry("HasteRating"),
                    new Entry("DefenseRating"), new Entry("DodgeRating"),
                    new Entry("ParryRating"));
            case "Retribution" -> List.of(
                    new Entry("Strength"), new Entry("Agility"), new Entry("AttackPower"),
                    new Entry("CritRating"), new Entry("HitRating"), new Entry("ExpertiseRating"),
                    new Entry("HasteRating"), new Entry("ArmorPenetrationRating"));
            default -> List.of();
        };
    }
}
