// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.calculations.CalculationsBase;
import rawr.base.character.CharacterRace;

/**
 * Which player faction can use an item: {@code Neutral(0)} (both), {@code Alliance(1)} or
 * {@code Horde(2)}.
 *
 * <p>Read by {@link Item#fitsFaction(CharacterRace)} and by
 * {@link ItemCache#getUnfilteredRelevantItems(CalculationsBase, CharacterRace)}; the game data
 * uses the same numbering. {@link #value()} returns the stored number.</p>
 */
public enum ItemFaction {
    Neutral(0),
    Alliance(1),
    Horde(2);

    private final int value;

    ItemFaction(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
