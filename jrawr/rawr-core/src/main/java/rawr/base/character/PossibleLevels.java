// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import rawr.base.bosses.BossHandler;
import rawr.base.stats.StatConversion;

/**
 * The levels a boss or target can have for combat-table and rating math: 80 (equal to the level
 * cap), 81, 82 and 83.
 *
 * <p>{@link #value()} returns the actual level (80..83), which indexes the per-level arrays in
 * {@link StatConversion} (miss/dodge/parry/glance/block caps, target armor) and in
 * {@link BossHandler}. {@code LVLP0} is the neutral/default target; raid bosses are level 83
 * ({@code LVLP3}), three levels above the player, which is why the hit caps are highest against
 * them.</p>
 */
public enum PossibleLevels {
    LVLP0(80),
    LVLP1(81),
    LVLP2(82),
    LVLP3(83);

    private final int value;

    PossibleLevels(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
