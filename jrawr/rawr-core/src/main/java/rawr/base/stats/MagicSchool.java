// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

/**
 * The magical damage schools known to the base layer, with their in-game numeric school ids:
 * {@code Fire 2}, {@code Nature 3}, {@code Frost 4}, {@code Shadow 5}, {@code Arcane 6}.
 *
 * <p>Physical (0) and Holy (1) are not part of this enum. The base enum is kept for API parity
 * with the C# class; the ported Mage model defines its own richer
 * {@code rawr.models.mage.MagicSchool} (which also has {@code Physical}, {@code Holy} and
 * {@code FrostFire}) and passes that type through its calculations, so this base-school list is
 * currently unused by the ported engine. The numeric values match the game's school numbering,
 * not declaration order.</p>
 */
public enum MagicSchool {
    Fire(2),
    Nature(3),
    Frost(4),
    Shadow(5),
    Arcane(6);

    private final int value;

    MagicSchool(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
