// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.stats.MagicSchool;

/**
 * The magic school of a weapon's damage, or {@code Physical} for a plain weapon.
 *
 * <p>The numeric values are the game's spell-school order: {@code Physical(0)}, {@code Holy(1)},
 * {@code Fire(2)}, {@code Nature(3)}, {@code Frost(4)}, {@code Shadow(5)}, {@code Arcane(6)}.
 * It describes the weapon's white damage only; the spell schools a simulation works with come
 * from {@link MagicSchool}. {@link #value()} returns the stored number.</p>
 */
public enum ItemDamageType {
    Physical(0),
    Holy(1),
    Fire(2),
    Nature(3),
    Frost(4),
    Shadow(5),
    Arcane(6);

    private final int value;

    ItemDamageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
