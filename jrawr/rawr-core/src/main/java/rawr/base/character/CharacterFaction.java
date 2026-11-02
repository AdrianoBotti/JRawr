// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The two player factions of the game, with the numeric ids of the C# enum.
 *
 * <p>A faction is never chosen directly: {@link Character#setRace} derives it from the race
 * (Alliance for Draenei, Dwarf, Gnome, Human and NightElf; Horde otherwise) and uses it to add or
 * remove faction-locked racial buffs. It also gates faction-restricted items and vendors.</p>
 */
public enum CharacterFaction {
    Alliance(1),
    Horde(2);

    private final int value;

    CharacterFaction(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
