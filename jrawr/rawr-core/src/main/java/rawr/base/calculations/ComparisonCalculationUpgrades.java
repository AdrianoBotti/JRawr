// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

/**
 * {@link ComparisonCalculationBase} implementation used by the optimizer's upgrade search: one
 * candidate item, with its gems and enchant, together with the value of swapping it into the
 * character's gear.
 *
 * <p>Unlike a chart entry, which usually carries only a plain {@link Item}, an upgrade entry
 * is built around an {@link ItemInstance}, because the optimizer must keep the exact gem and
 * enchant choices that produced the points. The instance is the single source of truth:
 * {@link #getItem()} reads the template out of it, and {@link #setItem(Item)} is unsupported,
 * since assigning a bare template would silently discard the gem and enchant information that
 * distinguishes one upgrade candidate from another. The remaining properties (name,
 * description, overall points, sub-points, equipped and part-equipped) are plain mutable
 * fields that default to null or zero and are filled in by the optimizer before the entry is
 * handed to the UI.</p>
 *
 * <p>Entries are produced by the optimizers' upgrade computation and consumed by the batch
 * tools and the comparison UI. Their overall points usually hold the gain over the current
 * character rather than an absolute rating.</p>
 */
public class ComparisonCalculationUpgrades extends ComparisonCalculationBase {
    private String name;
    private String description;
    private float overallPoints;
    private float[] subPoints;
    private ItemInstance itemInstance;
    private boolean equipped;
    private boolean partEquipped;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float overallPoints) {
        this.overallPoints = overallPoints;
    }

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] subPoints) {
        this.subPoints = subPoints;
    }

    /**
     * The concrete candidate item, including the gems and enchant that were evaluated, or
     * {@code null} until the optimizer sets it.
     */
    @Override
    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance itemInstance) {
        this.itemInstance = itemInstance;
    }

    // C# `[XmlIgnore] public override Item Item` reads ItemInstance.Item and rejects assignment.
    /**
     * The item template of {@link #getItemInstance()}, or {@code null} when no instance has
     * been set. There is no separate item field.
     */
    @Override
    public Item getItem() {
        if (itemInstance != null) return itemInstance.getItem();
        return null;
    }

    /**
     * Always throws {@code UnsupportedOperationException}: an upgrade always carries its gems
     * and enchant, so the item must be assigned through
     * {@link #setItemInstance(ItemInstance)} instead. The method exists only because the
     * inherited comparison contract requires it.
     */
    @Override
    public void setItem(Item value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getEquipped() {
        return equipped;
    }

    @Override
    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    @Override
    public boolean getPartEquipped() {
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean partEquipped) {
        this.partEquipped = partEquipped;
    }
}
