// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.character.CharacterSlot;

/**
 * Where an item belongs: one of the character's equipment slots, or &mdash; for gems &mdash; the
 * socket color the gem can fill.
 *
 * <p>The first block is physical equipment ({@code Head}, {@code Neck}, {@code Shoulders},
 * {@code Back}, {@code Chest}, {@code Shirt}, {@code Tabard}, {@code Wrist}, {@code Hands},
 * {@code Waist}, {@code Legs}, {@code Feet}, {@code Finger}, {@code Trinket}, the weapon slots
 * {@code OneHand}/{@code TwoHand}/{@code MainHand}/{@code OffHand}, {@code Ranged},
 * {@code Projectile} and {@code ProjectileBag}); the second block, from {@link #Meta} on,
 * enumerates gem colors ({@code Meta}, {@code Red}, {@code Orange}, {@code Yellow}, {@code Green},
 * {@code Blue}, {@code Purple}, {@code Prismatic}). The same enum serves both purposes because a
 * gem is an {@link Item} whose slot is its color, which is exactly what
 * {@link Item#gemMatchesSlot(Item, ItemSlot)} compares against an item's socket colors. Hybrid
 * gems ({@code Orange}, {@code Green}, {@code Purple}) match two socket colors each, and
 * {@code Prismatic} matches every colored socket but not {@link #Meta}.</p>
 *
 * <p>Do not reorder or insert values: the ordinal is observable. {@link Enchant#findEnchants}
 * encodes a slot as {@code 10000 * ordinal()} in saved availability lists, {@link Enchant}'s
 * hash code mixes in {@code ordinal()}, and {@link Item#getDefaultSlotMap()} relies on enum-map
 * iteration. The C# data files reference these names.</p>
 *
 * @see CharacterSlot
 * @see Item#fitsInSlot(CharacterSlot)
 * @see Item#getDefaultSlotMap()
 */
public enum ItemSlot {
    None,

    Head,
    Neck,
    Shoulders,
    Back,
    Chest,
    Shirt,
    Tabard,
    Wrist,
    Hands,
    Waist,
    Legs,
    Feet,
    Finger,
    Trinket,
    OneHand,
    TwoHand,
    MainHand,
    OffHand,
    Ranged,
    Projectile,
    ProjectileBag,

    Meta,
    Red,
    Orange,
    Yellow,
    Green,
    Blue,
    Purple,
    Prismatic
}
