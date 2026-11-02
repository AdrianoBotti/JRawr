// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

/**
 * The stats that combine as <i>non-stacking</i>: when the same stat arrives from several sources
 * only the strongest applies, so {@link Stats#accumulate(Stats)} keeps {@code max(a, b)} rather
 * than adding.
 *
 * <p>The category exists for effects that are, in game terms, exclusive or capped: resistance
 * buffs of the same element (only the best resistance aura counts), {@code MovementSpeed}, potion
 * bonuses such as {@code BonusManaPotion}, and several item procs/effects that must not stack
 * with themselves. Values live in {@code rawNoStackData} of {@link Stats}, indexed by
 * {@link #value()}; see {@link Stats} for the full combination rules.</p>
 */
public enum NonStackingStat {
    BonusManaPotion(0),
    ArcaneResistanceBuff(1),
    FireResistanceBuff(2),
    FrostResistanceBuff(3),
    NatureResistanceBuff(4),
    ShadowResistanceBuff(5),
    MovementSpeed(6),
    GreatnessProc(7),
    HighestStat(8),
    ShieldFromHealed(9),
    RighteousVengeanceCanCrit(10),
    DeathbringerProc(11),
    BattlemasterHealth(12),
    ZodProc(13);

    private final int value;

    NonStackingStat(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
