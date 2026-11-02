// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Calculations.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.character.Character;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

/**
 * One entry of a comparison: a candidate item, enchant, buff, gem, talent or whole gear set
 * together with the rating points it would produce. A comparison chart, and the optimizer's
 * upgrade list, is a list of these entries.
 *
 * <p>What the points mean depends on the caller. Chart code populates them as differences
 * against a baseline (see the {@code getItemCalculations} and
 * {@code getCharacterComparisonCalculations} methods of {@link CalculationsBase}), while
 * other callers may store absolute or optimizer-specific values. Consumers rank entries by
 * {@link #getOverallPoints()}; the UI additionally renders the name, the tooltip and the
 * equipped flags.</p>
 *
 * <p>This class only defines the shape. Each model supplies a concrete subclass from
 * {@link CalculationsBase#createNewComparisonCalculation()} that stores the fields (often one
 * field or array slot per sub-point).</p>
 *
 * <h2>Fields</h2>
 *
 * <ul>
 *   <li>{@link #getName()} &mdash; the label shown at the left of the chart row.</li>
 *   <li>{@link #getItemInstance()} and {@link #getItem()} &mdash; what was rated. The instance
 *       holds the concrete item plus its gems and enchant; the plain item is used for the
 *       tooltip and may be a blank item carrying only a name and stats for non-gear entries.
 *       A null item means "no tooltip".</li>
 *   <li>{@link #getDescription()} &mdash; free text for non-item tooltips (glyphs, talents).</li>
 *   <li>{@link #getEquipped()} and {@link #getPartEquipped()} &mdash; whether the object is
 *       the one currently worn, or partly worn; the chart highlights those rows.</li>
 *   <li>{@link #getCharacterItems()} &mdash; the complete gear set the points were computed
 *       from, used by optimizer upgrade entries.</li>
 *   <li>{@link #getBaseStat()} and {@link #getBaseStatOption(Character)} &mdash; the
 *       "relative stats" feature: the stat normalized to one unit and the per-character user
 *       option that enables the display.</li>
 * </ul>
 */
public abstract class ComparisonCalculationBase {
    /**
     * The label of this entry in the comparison chart.
     */
    public abstract String getName();

    public abstract void setName(String name);

    /**
     * The overall rating points of this entry, in the same model-defined units as
     * {@link CharacterCalculationsBase#getOverallPoints()} (usually a difference against a
     * baseline; see the class notes).
     */
    public abstract float getOverallPoints();

    public abstract void setOverallPoints(float overallPoints);

    /**
     * The rating points of this entry split into the model's sub-point dimensions, in the
     * same order as {@link CharacterCalculationsBase#getSubPoints()}.
     */
    public abstract float[] getSubPoints();

    public abstract void setSubPoints(float[] subPoints);

    /**
     * The concrete gear piece that was rated, including its gems and enchant, or {@code null}
     * for entries that are not gear (enchants, buffs, and similar).
     */
    public abstract ItemInstance getItemInstance();

    public abstract void setItemInstance(ItemInstance itemInstance);

    /**
     * The item template behind {@link #getItemInstance()}, used to render the tooltip. For
     * non-item entries this may be a blank item carrying just a name and stats, and a
     * {@code null} item means no tooltip is shown.
     */
    public abstract Item getItem();

    public abstract void setItem(Item item);

    /**
     * Free-text description used as the tooltip when the entry has no item (glyphs, talents,
     * and similar).
     */
    public abstract String getDescription();

    public abstract void setDescription(String description);

    /**
     * Whether this entry is the object the character currently has equipped; the chart
     * highlights such rows.
     */
    public abstract boolean getEquipped();

    public abstract void setEquipped(boolean equipped);

    /**
     * Whether only part of the entry's gear set is equipped (used by whole-set entries such
     * as gemming templates); the chart highlights those rows in a lighter color.
     */
    public abstract boolean getPartEquipped();

    public abstract void setPartEquipped(boolean partEquipped);

    private ItemInstance[] characterItems;

    /**
     * The complete set of item instances (one per gear slot) that the points were computed
     * from, or {@code null} for simple chart entries. Used by optimizer upgrade entries so
     * the UI can show the full set behind a change.
     */
    public ItemInstance[] getCharacterItems() {
        return characterItems;
    }

    public void setCharacterItems(ItemInstance[] characterItems) {
        this.characterItems = characterItems;
    }

    private String baseStat;

    /**
     * Name of the stat that the "relative stats" comparison normalizes to one unit, so the
     * chart can show what a single point of that stat is worth. See
     * {@link #getBaseStatOption(Character)}.
     */
    public String getBaseStat() {
        return baseStat;
    }

    public void setBaseStat(String baseStat) {
        this.baseStat = baseStat;
    }

    /**
     * Whether the per-character user option for the relative-stats display is enabled.
     * Defaults to false; a model that supports the display overrides it.
     */
    public boolean getBaseStatOption(Character character) {
        return false;
    }
}
