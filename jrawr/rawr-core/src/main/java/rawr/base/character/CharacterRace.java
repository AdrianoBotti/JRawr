// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The playable races of this game version, keyed by the numeric race id used in saved character
 * files and by the Armory.
 *
 * <p>The ids are the game's: {@code Human 1}, {@code Orc 2}, {@code Dwarf 3}, {@code NightElf 4},
 * {@code Undead 5}, {@code Tauren 6}, {@code Gnome 7}, {@code Troll 8}, {@code BloodElf 10},
 * {@code Draenei 11} (there is no 9). Race determines the faction
 * ({@link Character#setRace}) and a few racial effects; in particular only Draenei get "Heroic
 * Presence", which {@link Character#validateActiveBuffs()} adds or removes automatically.</p>
 */
public enum CharacterRace {
    Human(1),
    Orc(2),
    Dwarf(3),
    NightElf(4),
    Undead(5),
    Tauren(6),
    Gnome(7),
    Troll(8),
    BloodElf(10),
    Draenei(11);

    private final int value;

    CharacterRace(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
