// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffectCombination.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

import rawr.base.effects.SpecialEffect;

/**
 * A value paired with the probability of that value occurring: one entry of a discrete
 * distribution.
 *
 * <p>Used by the overlapping-proc mathematics of {@link SpecialEffect}, whose
 * {@code getAverageCombinedUptimeCombinations*} methods return arrays of these pairs: one entry
 * per combination of simultaneously active effects, with {@link #value()} the combined stat bonus
 * of that combination and {@link #chance()} the probability of seeing it during the fight. Callers
 * use the distribution to average functions that are not linear in the number of active effects.
 * {@code chance} is a probability in [0, 1] and the chances over one returned distribution sum to
 * 1, so a plain weighted average gives the expected value.</p>
 *
 * <p>An immutable record: the C# original is a struct, so value semantics (generated
 * {@code equals}/{@code hashCode}) match its role, and no caller mutates an instance after
 * construction.</p>
 *
 * @param chance probability of {@code value} (0..1)
 * @param value the stat bonus for this combination
 */
public record WeightedStat(float chance, float value) {
}
