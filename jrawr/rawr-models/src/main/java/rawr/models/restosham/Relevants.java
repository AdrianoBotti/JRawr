// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/RestoSham.Base.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

import rawr.base.items.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains various lists of relevant stats, armor types, etc
 */
public final class Relevants {
    private Relevants() {
    }

    public static final String[] RelevantStats = new String[] {
            "Stamina",
            "Intellect",
            "SpellPower",
            "CritRating",
            "HasteRating",
            "SpellCrit",
            "SpellHaste",
            "Mana",
            "Mp5",
            "Earthliving",
            "ManacostReduceWithin15OnHealingCast",
            "ManaRestoreFromMaxManaPerSecond",
            //"BonusManaPotion",                            Mana pots are not currently modeled by RestoSham
            "WaterShieldIncrease",
            "CHHWHealIncrease",
            "CHCTDecrease",
            "RTCDDecrease",
            "TotemCHBaseHeal",
            "ManaRestoreFromMaxManaPerSecond",
            "TotemHWBaseCost",
            "TotemCHBaseCost",
            "TotemHWSpellpower",
            "TotemLHWSpellpower",
            "TotemThunderhead",
            "ManaRestore",
            "BonusCritHealMultiplier",
            "BonusHealingDoneMultiplier",
            "BonusManaMultiplier",
            "BonusIntellectMultiplier",
            "RestoSham2T9",
            "RestoSham4T9",
            "RestoSham2T10",
            "RestoSham4T10",
            "RestoShamRelicT9",
            "RestoShamRelicT10",
            "Healed",
            "Hp5",
            "MovementSpeed"
    };

    public static final List<String> RelevantGlyphs = new ArrayList<String>(List.of(
            "Glyph of Healing Wave",
            "Glyph of Water Shield",
            "Glyph of Water Mastery",
            "Glyph of Chain Heal",
            "Glyph of Earth Shield",
            "Glyph of Lesser Healing Wave",
            "Glyph of Earthliving Weapon",
            "Glyph of Mana Tide Totem",
            "Glyph of Healing Stream Totem",
            "Glyph of Riptide"
    ));

    public static final List<ItemType> RelevantItemTypes = new ArrayList<ItemType>(List.of(
            ItemType.None,
            ItemType.Cloth,
            ItemType.Leather,
            ItemType.Mail,
            ItemType.Totem,
            ItemType.OneHandMace,
            ItemType.OneHandAxe,
            ItemType.Shield,
            ItemType.Staff,
            ItemType.FistWeapon,
            ItemType.Dagger
    ));
}
