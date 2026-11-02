// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

/**
 * The two stats that combine as <i>inverse multiplicative</i> (also called diminishing
 * combination): the result of combining fractions {@code a} and {@code b} is
 * {@code 1-(1-a)*(1-b)} instead of {@code a+b}.
 *
 * <p>{@code ArmorPenetration} and {@code ThreatReductionMultiplier} are fractions (0.2 = 20%)
 * that cut something down; two 10% reductions leave 81% remaining, not 80%. Values are stored in
 * the {@code rawInverseMultiplicativeData} array of {@link Stats}, indexed by {@link #value()};
 * the authoritative combination rules and the accumulate/subtract behavior for this category are
 * documented on {@link Stats}.</p>
 */
public enum InverseMultiplicativeStat {
    ArmorPenetration(0),
    ThreatReductionMultiplier(1);

    private final int value;

    InverseMultiplicativeStat(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
