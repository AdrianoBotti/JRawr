// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The tab categories of the original buffs window: a coarse classification of a {@link Buff} used
 * to group buff choices for display.
 *
 * <p>{@code All} and {@code Current} are summary views (every buff; the buffs currently active on
 * a character). The remaining constants mirror the window's sections: {@code Food},
 * {@code ElixirsAndFlasks} (raid flasks plus battle and guardian elixirs), {@code Scrolls},
 * {@code Potions}, {@code RaidBuffs} (buffs granted by other raid members) and
 * {@code RaidDeBuffs} (effects the raid applies to the target). Buffs are actually grouped in the
 * engine and UI by the free-form {@link Buff#group} string, and this enum is not consumed by the
 * ported engine; it is kept for parity with the C# declaration and for UI mapping. The numeric
 * values start at 0 and follow the C# declaration order.
 */
public enum BuffGroup {
    All(0),
    Current(1),
    Food(2),
    ElixirsAndFlasks(3),
    Scrolls(4),
    Potions(5),
    RaidBuffs(6),
    RaidDeBuffs(7);

    private final int value;

    BuffGroup(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
