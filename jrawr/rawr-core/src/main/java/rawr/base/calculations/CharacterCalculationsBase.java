// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Calculations.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.items.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of one model calculation: the rating points, the display values and the auxiliary
 * data that the user interface and the optimizer consume.
 *
 * <p>Every call to
 * {@link CalculationsBase#getCharacterCalculations(Character, Item, boolean, boolean, boolean)}
 * returns a concrete subclass of this class; models define their own because the points are
 * model-specific. The central values are:</p>
 *
 * <ul>
 *   <li>{@link #getOverallPoints()} &mdash; the overall rating, a single {@code float} score
 *       for the whole character. Its meaning is model-defined: damage per second (DPS) for a
 *       damage model, healing per second (HPS) for a healer, a weighted
 *       survivability/threat score for a tank. The optimizer maximizes this number.</li>
 *   <li>{@link #getSubPoints()} &mdash; the same score broken into a fixed list of dimensions
 *       (different damage schools, or threat and survival, and so on). The order matches the
 *       model's sub-point labels; per the original contract the entries should sum to the
 *       overall points.</li>
 *   <li>{@link #getCharacterDisplayCalculationValues()} &mdash; the label-to-text entries the
 *       UI shows in the stats panel, optionally carrying a tooltip suffix.</li>
 * </ul>
 *
 * <p>The optimizer and the comparison charts need only the numbers; the UI also needs the
 * display map, so a model may be asked to skip building it
 * ({@code needsDisplayCalculations} false on the calculation entry point).</p>
 *
 * <h2>Asynchronous display and auto-activated buffs</h2>
 *
 * <p>A slow model can defer the human-readable part: when
 * {@link #getRequiresAsynchronousDisplayCalculation()} is true the application first shows
 * {@link #getCharacterDisplayCalculationValues()}, starts the expensive work in the
 * background, and later replaces the display with
 * {@link #getAsynchronousCharacterDisplayCalculationValues()}; a pending run can be aborted
 * with {@link #cancelAsynchronousCharacterDisplayCalculation()}. Models record any buffs they
 * switched on themselves in {@link #getAutoActivatedBuffs()} so the caller can keep the
 * character's buff list in sync, and they expose extra named objective quantities through
 * {@link #getOptimizableCalculationValue(String)}.</p>
 *
 * @see CalculationsBase
 * @see ComparisonCalculationBase
 */
public abstract class CharacterCalculationsBase {
    /**
     * The overall rating of the finished calculation: the single score the optimizer
     * maximizes. Units are model-defined (DPS, HPS, threat, a weighted tank score, ...).
     */
    public abstract float getOverallPoints();

    /**
     * Sets the overall rating while the model builds the result. The value should stay
     * consistent with {@link #getSubPoints()}.
     */
    public abstract void setOverallPoints(float overallPoints);

    /**
     * The overall rating split into sub-points, one per model rating dimension, in the same
     * order as the model's sub-point labels. Callers index the array directly; per the
     * original contract the entries should sum to {@link #getOverallPoints()}.
     */
    public abstract float[] getSubPoints();

    /**
     * Replaces the sub-point array while the model builds the result. Whether the array is
     * stored as-is or copied into the model's fields is up to the implementation.
     */
    public abstract void setSubPoints(float[] subPoints);

    /**
     * The values shown in the stats panel: a map from display label (matching the model's
     * label list, see {@link CalculationsBase#getCharacterDisplayCalculationLabels()}) to the
     * text to show. A value may contain {@code '*' + suffix}; everything after the first
     * {@code '*'} is tooltip text, the part before it is the visible value.
     */
    public abstract Map<String, String> getCharacterDisplayCalculationValues();

    /**
     * Whether the model wants the two-phase display: the application shows
     * {@link #getCharacterDisplayCalculationValues()} immediately and then replaces it with
     * the result of {@link #getAsynchronousCharacterDisplayCalculationValues()}. Defaults to
     * false, meaning one synchronous phase is enough.
     */
    public boolean getRequiresAsynchronousDisplayCalculation() {
        return false;
    }

    /**
     * The display values produced after the expensive part of the calculation finished; see
     * {@link #getRequiresAsynchronousDisplayCalculation()}. The default forwards to
     * {@link #getCharacterDisplayCalculationValues()}. May return {@code null} when
     * {@link #cancelAsynchronousCharacterDisplayCalculation()} aborted the work.
     */
    public Map<String, String> getAsynchronousCharacterDisplayCalculationValues() {
        return getCharacterDisplayCalculationValues();
    }

    /**
     * Asks a model that requires asynchronous display to stop the outstanding background
     * calculation as soon as possible; see
     * {@link #getRequiresAsynchronousDisplayCalculation()}. The default does nothing.
     */
    public void cancelAsynchronousCharacterDisplayCalculation() {
    }

    private final List<Buff> autoActivatedBuffs = new ArrayList<Buff>();

    /**
     * Buffs the model activated by itself during the calculation (for example a stance or
     * self-buff the spec always has). The caller can mirror them onto the character; a model
     * that turns buffs on or off appends them here while building the result. The list is
     * live and mutable.
     */
    public List<Buff> getAutoActivatedBuffs() {
        return autoActivatedBuffs;
    }

    /**
     * Value of a model-specific named quantity that the optimizer may target by name (see
     * {@link OptimizationRequirement} and the character's "optimize for" settings). Unknown
     * names return 0. The default always returns 0; models with extra objective quantities
     * override it.
     */
    public float getOptimizableCalculationValue(String calculation) {
        return 0f;
    }
}
