// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

/**
 * A damage-over-time aura ("DoT"): damage applied repeatedly to its victim over a duration, one
 * "tick" at a time. Boss definitions use it for effects that keep hurting the player after the
 * initial application.
 *
 * <p>{@code DoT} extends {@link Attack} (name, damage school, target count and so on, see that
 * class) and adds the repeating part:</p>
 *
 * <ul>
 *   <li>{@link #damagePerTick} &mdash; damage applied on each tick.</li>
 *   <li>{@link #numTicks} &mdash; how many ticks occur.</li>
 *   <li>{@link #tickInterval} &mdash; seconds between ticks.</li>
 * </ul>
 *
 * <p>{@link #getDuration()} returns {@code tickInterval * numTicks}, the total lifetime in
 * seconds. The inherited {@link Attack#getDamagePerHit()} is the initial damage of the aura,
 * separate from the over-time portion; the inherited {@link Attack#attackSpeed} is how many
 * seconds pass between applications of the aura (how often the boss can re-apply it), not the
 * tick rate. Encounter definitions register their DoTs in {@link BossHandler#getAttacks()},
 * because a DoT is an Attack; the separate {@link BossHandler#getDoTs()} list exists so saved
 * files and UI code can read them independently.</p>
 */
public class DoT extends Attack {
    public float damagePerTick;
    public float numTicks;
    public float tickInterval;

    /**
     * Total lifetime of the aura in seconds: {@link #numTicks} times {@link #tickInterval}.
     */
    public float getDuration() {
        return tickInterval * numTicks;
    }
}
