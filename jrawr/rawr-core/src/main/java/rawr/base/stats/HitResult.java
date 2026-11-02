// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

import rawr.base.character.Character;

/**
 * One possible result of an attack roll, plus two aggregate aliases: the rows of the combat
 * table.
 *
 * <p>The individual outcomes match the melee combat table described on {@link StatConversion}:
 * {@code Miss}, {@code Dodge}, {@code Parry}, {@code Block}, {@code Glance}, {@code Resist}
 * (partial spell resistance) and the two good results {@code Crit} and {@code Hit}.
 * {@code AnyMiss} and {@code AnyHit} are aggregate rows: "all failure outcomes" and "all landing
 * outcomes", used when a caller needs one total instead of the per-outcome probabilities. Tank
 * models pass an outcome to
 * {@link StatConversion#getDRAvoidanceChance(Character, Stats, HitResult, int)} to ask how likely
 * that outcome is against the current target, and attack-table classes fill one probability slot
 * per outcome. The numeric values are the C# declaration order and are not persisted.</p>
 */
public enum HitResult {
    AnyMiss(0),
    AnyHit(1),
    Miss(2),
    Dodge(3),
    Parry(4),
    Block(5),
    Glance(6),
    Resist(7),
    Crit(8),
    Hit(9);

    private final int value;

    HitResult(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
