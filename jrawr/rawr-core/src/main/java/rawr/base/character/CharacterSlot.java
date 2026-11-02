// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * Every equipment slot a character can hold, plus the pseudo-slots the item browser uses.
 *
 * <p>The real gear slots are {@code Projectile} (0) through {@code Shirt} (20); a character has
 * {@link Character#SlotCount} = 21 of them and the item-instance array inside {@link Character}
 * is indexed by {@link #value()}. The first {@link Character#OptimizableSlotCount} = 19 (through
 * {@code ProjectileBag}) are the ones the optimizer may swap; {@code Tabard} and {@code Shirt}
 * are worn but never optimized.</p>
 *
 * <p>The remaining constants are not equipment positions: {@code None} = -1 means "no slot",
 * {@code Gems}/{@code Metas} are pseudo-slots used while browsing gems, and {@code AutoSelect}
 * tells the optimizer to choose the slot itself. The numeric values preserve the C# enum and the
 * game's inventory layout (head = 1, ...), so they are explicit rather than ordinal;
 * {@link #fromValue(int)} reverses the mapping and returns {@code None} for unknown ids.</p>
 */
public enum CharacterSlot {
    None(-1),
    Projectile(0),
    Head(1),
    Neck(2),
    Shoulders(3),
    Chest(4),
    Waist(5),
    Legs(6),
    Feet(7),
    Wrist(8),
    Hands(9),
    Finger1(10),
    Finger2(11),
    Trinket1(12),
    Trinket2(13),
    Back(14),
    MainHand(15),
    OffHand(16),
    Ranged(17),
    ProjectileBag(18),
    Tabard(19),
    Shirt(20),

    Gems(100),
    Metas(101),
    AutoSelect(1000);

    private final int value;

    CharacterSlot(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static CharacterSlot fromValue(int value) {
        for (CharacterSlot slot : values()) {
            if (slot.value == value) return slot;
        }
        return None;
    }
}
