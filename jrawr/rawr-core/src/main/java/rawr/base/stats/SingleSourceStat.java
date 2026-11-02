// New design (not ported from C#): Java-only fifth combination category. In C# the equivalent
// values are subclass properties of StatsCat/StatsBear; this port models them explicitly as a
// non-combining storage category instead (see docs/dev/DECISIONS.md).
package rawr.base.stats;

/**
 * Stats that are assigned by exactly one source per calculation and are therefore never combined
 * by merge operations: the model computes them once (from talents or options) and reads them back.
 *
 * <p>This is the fifth <i>combination category</i> next to {@link AdditiveStat},
 * {@link MultiplicativeStat}, {@link InverseMultiplicativeStat} and {@link NonStackingStat}. It
 * exists because the original C# engine keeps these values as subclass properties of
 * {@code StatsCat}/{@code StatsBear}, which the base {@code Stats.Accumulate} cannot see. Modelling
 * them as an explicit category keeps every stat in one place while preserving that behavior:</p>
 *
 * <ul>
 *   <li>{@code accumulate} ignores the category entirely &mdash; a source's value is never merged
 *       into a target (same as C# subclass properties).</li>
 *   <li>{@code add}/{@code subtract}/{@code multiply} leave it at zero &mdash; C#'s operators
 *       build a fresh base {@code Stats} and lose subclass fields.</li>
 *   <li>{@code copy()} copies it &mdash; C# {@code Clone} uses {@code MemberwiseClone}, which keeps
 *       subclass fields.</li>
 *   <li>equality and comparison ignore it, like the C# {@code operator ==}.</li>
 *   <li>iteration ({@code PROPERTY_INFO}, {@code values()}, {@code getStatNames()}, XML load/save)
 *       includes it, because these are real stats the oracle dump reports by runtime type.</li>
 * </ul>
 *
 * <p>The current members are the Cat/Bear model constants from the C# {@code StatsCat} and
 * {@code StatsBear} classes. {@code BonusMangleDamageMultiplier} is shared by both models; each
 * model sets it on its own Stats object, so a single property is harmless.</p>
 */
public enum SingleSourceStat {
    // Cat (Rawr.Cat/StatsCat.cs)
    RakeCostReduction(0),
    ShredCostReduction(1),
    BonusCPOnCrit(2),
    BonusEnergyOnTigersFury(3),
    FinisherEnergyOnAvoid(4),
    BonusMangleDamageMultiplier(5),
    BonusRakeDamageMultiplier(6),
    BonusShredDamageMultiplier(7),
    BonusFerociousBiteDamageMultiplier(8),
    // Bear (Rawr.Bear/StatsBear.cs)
    BonusMaulDamageMultiplier(9),
    BonusEnrageDamageMultiplier(10),
    MangleCooldownReduction(11),
    BonusSwipeDamageMultiplier(12);

    private final int value;

    SingleSourceStat(int value) {
        this.value = value;
    }

    /** Index of this stat in the single-source {@code float[]} of {@link Stats}. */
    public int value() {
        return value;
    }
}
