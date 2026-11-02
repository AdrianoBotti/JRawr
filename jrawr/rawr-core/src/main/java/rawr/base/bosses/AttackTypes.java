// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

/**
 * Classification of an incoming boss {@link Attack} by delivery method.
 *
 * <ul>
 *   <li>{@link #AT_MELEE} &mdash; contact attacks: auto-attacks and weapon strikes that require
 *       the boss to be in melee range of its victim.</li>
 *   <li>{@link #AT_RANGED} &mdash; projectile or cast attacks aimed at one or a few targets.</li>
 *   <li>{@link #AT_AOE} &mdash; area-of-effect attacks that hit many raid members at once.</li>
 * </ul>
 *
 * <p>{@link #value()} is the ordinal stored in the original data and used to index per-type
 * arrays; the values must not change. {@link BossHandler#getFilteredAttackList(AttackTypes)}
 * and {@link BossHandler#getDpsByType(AttackTypes, float, float)} group attacks by this type.</p>
 */
public enum AttackTypes {
    AT_MELEE(0),
    AT_RANGED(1),
    AT_AOE(2);

    private final int value;

    AttackTypes(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
