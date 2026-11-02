// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The eleven professions of the game plus {@code None}, each carrying the numeric profession id
 * the Armory reported (for example {@code Alchemy 171}, {@code Blacksmithing 164}). These values
 * are identifiers, not indices; use {@link Profs} for conversions to and from the 1-based index
 * used elsewhere.
 *
 * <p>A {@link Character} has one primary and one secondary profession slot. Professions matter to
 * the engine because some confer permanent stat bonuses (the "profession buffs" such as Mining's
 * Toughness and Skinning's Master of Anatomy), which
 * {@link Character#validateActiveBuffs()} activates automatically, and because some items,
 * enchants and extras are gated on having a given profession.</p>
 */
public enum Profession {
    None(0),
    Alchemy(171),
    Enchanting(333),
    Engineering(202),
    Herbalism(182),
    Inscription(773),
    Jewelcrafting(755),
    Leatherworking(165),
    Skinning(393),
    Tailoring(197),
    Blacksmithing(164),
    Mining(186);

    private final int value;

    Profession(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
