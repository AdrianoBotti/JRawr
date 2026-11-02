// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The playable character classes of this game version, keyed by the numeric class id used in
 * saved character files and by the Armory.
 *
 * <p>The ids follow the game's own numbering: {@code Warrior 1}, {@code Paladin 2},
 * {@code Hunter 3}, {@code Rogue 4}, {@code Priest 5}, {@code DeathKnight 6}, {@code Shaman 7},
 * {@code Mage 8}, {@code Warlock 9}, {@code Druid 11} (there is no 10). The engine uses the class
 * to pick the model, to dispatch {@link Character#getCurrentTalents()} and to gate
 * class-restricted buffs ({@link Buff#getAllowedClasses()}); the numeric values are only read
 * back from serialized data.</p>
 */
public enum CharacterClass {
    Warrior(1),
    Paladin(2),
    Hunter(3),
    Rogue(4),
    Priest(5),
    DeathKnight(6),
    Shaman(7),
    Mage(8),
    Warlock(9),
    Druid(11);

    private final int value;

    CharacterClass(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
