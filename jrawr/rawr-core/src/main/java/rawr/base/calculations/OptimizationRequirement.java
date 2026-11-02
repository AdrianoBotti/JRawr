// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.character.Character;

/**
 * A user-configured constraint on what the optimizer should accept: a named rating must be at
 * least (or at most) a threshold value. A list of these is stored on the {@link Character}
 * as its "optimize for" settings and is passed to the optimizers.
 *
 * <p>The requirement is a soft penalty, not a hard filter. While scoring a candidate gear set
 * the optimizer evaluates every requirement and adds the deficit to the score: a violated
 * "at least" requirement contributes {@code calcValue - value} (negative), a violated
 * "at most" requirement contributes {@code value - calcValue} (negative), and the deficits of
 * all violated requirements are summed. If any requirement is unmet that (negative) penalty
 * is the score; only when all are satisfied is the actual objective value used. A set that
 * violates nothing therefore always beats one that violates something. The optimizer applies
 * its own (also negative) penalties for invalid gem requirements separately. An empty list
 * means plain maximization of the selected objective.</p>
 *
 * <p>The fields are mutable and are populated when a saved character file is deserialized;
 * see {@link #calculation} for the names that may be constrained.</p>
 */
public class OptimizationRequirement {
    /**
     * Name of the rating the requirement constrains. Recognized forms are {@code "[Overall]"},
     * {@code "[SubPoint N]"} (index into the model's sub-point array), {@code "[Talent N]"}
     * and {@code "[Glyph N]"} (the current spec's talent and glyph data); any other name is
     * looked up through
     * {@link CharacterCalculationsBase#getOptimizableCalculationValue(String)}.
     */
    public String calculation;
    /**
     * Direction of the constraint: true requires the rating to be less than or equal to
     * {@link #value}, false requires it to be greater than or equal to {@link #value}.
     */
    public boolean lessThan;
    /**
     * The threshold the rating is compared against, in the units of the named rating.
     */
    public float value;
}
