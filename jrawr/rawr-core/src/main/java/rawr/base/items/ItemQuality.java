// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

/**
 * The game's item rarity tiers, ordered worst to best, with the numeric values the data files
 * use.
 *
 * <p>Values: {@code Temp(-1)}, a non-game sentinel used as "no lower bound" in filter defaults;
 * then the real tiers {@code Poor(0)} (gray), {@code Common(1)} (white), {@code Uncommon(2)}
 * (green), {@code Rare(3)} (blue), {@code Epic(4)} (purple), {@code Legendary(5)},
 * {@code Artifact(6)} and {@code Heirloom(7)} (bind-on-account gear). {@link #value()} is the
 * comparison key, e.g. in {@link ItemFilterRegex}'s quality-range check; the enum ordinal is not
 * used by the engine.</p>
 */
public enum ItemQuality {
    Temp(-1),
    Poor(0),
    Common(1),
    Uncommon(2),
    Rare(3),
    Epic(4),
    Legendary(5),
    Artifact(6),
    Heirloom(7);

    private final int value;

    ItemQuality(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
