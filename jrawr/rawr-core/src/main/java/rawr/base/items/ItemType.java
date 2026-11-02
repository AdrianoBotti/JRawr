// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.calculations.CalculationsBase;

/**
 * The material or weapon family of an item: what armor it is made of and which kind of weapon it
 * is.
 *
 * <p>Values fall into five groups: armor materials ({@code Cloth}, {@code Leather}, {@code Mail},
 * {@code Plate}); melee weapons and shields ({@code Dagger}, {@code FistWeapon},
 * {@code OneHandAxe}, {@code TwoHandAxe}, {@code OneHandMace}, {@code TwoHandMace},
 * {@code OneHandSword}, {@code TwoHandSword}, {@code Polearm}, {@code Staff}, {@code Shield});
 * ranged and throwing weapons ({@code Bow}, {@code Crossbow}, {@code Gun}, {@code Wand},
 * {@code Thrown}); class relics ({@code Idol}, {@code Libram}, {@code Totem}, {@code Sigil}); and
 * ammunition/containers ({@code Arrow}, {@code Bullet}, {@code Quiver}, {@code AmmoPouch}).</p>
 *
 * <p>Each model declares which types it cares about
 * ({@code CalculationsBase.getRelevantItemTypes}); the resulting {@link ItemTypeList} is the
 * first gate in {@link ItemFilter#isItemRelevant} and in {@link ItemCache}'s relevant-item
 * views. The enum has no behavior, and because the filter XML writes type names, the ordinal is
 * not persisted.</p>
 *
 * @see ItemTypeList
 * @see ItemFilter
 */
public enum ItemType {
    None,

    Cloth,
    Leather,
    Mail,
    Plate,

    Dagger,
    FistWeapon,
    OneHandAxe,
    TwoHandAxe,
    OneHandMace,
    TwoHandMace,
    OneHandSword,
    TwoHandSword,
    Polearm,
    Staff,
    Shield,

    Bow,
    Crossbow,
    Gun,
    Wand,
    Thrown,

    Idol,
    Libram,
    Totem,
    Sigil,

    Arrow,
    Bullet,
    Quiver,
    AmmoPouch
}
