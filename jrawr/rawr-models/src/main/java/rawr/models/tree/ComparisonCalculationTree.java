// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Tree/ComparisonCalculationTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tree;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.util.NetFormat;

public class ComparisonCalculationTree extends ComparisonCalculationBase {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {
        name = value;
    }

    private String description = "";

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String value) {
        this.description = value;
    }

    private float overallPoints;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getBurstPoints() {
        return subPoints[0];
    }

    public void setBurstPoints(float value) {
        subPoints[0] = value;
    }

    public float getSustainedPoints() {
        return subPoints[1];
    }

    public void setSustainedPoints(float value) {
        subPoints[1] = value;
    }

    public float getSurvivalPoints() {
        return subPoints[2];
    }

    public void setSurvivalPoints(float value) {
        subPoints[2] = value;
    }

    private Item item = null;

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public void setItem(Item value) {
        this.item = value;
    }

    private ItemInstance itemInstance = null;

    @Override
    public ItemInstance getItemInstance() {
        return this.itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance value) {
        this.itemInstance = value;
    }

    private boolean equipped;

    @Override
    public boolean getEquipped() {
        return equipped;
    }

    @Override
    public void setEquipped(boolean value) {
        equipped = value;
    }

    private boolean partEquipped;

    @Override
    public boolean getPartEquipped() {
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        partEquipped = value;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s: (%sO %sH %sM %sS )", getName(),
                NetFormat.fixed(getOverallPoints(), 1),
                NetFormat.fixed(getBurstPoints(), 1),
                NetFormat.fixed(getSustainedPoints(), 1),
                NetFormat.fixed(getSurvivalPoints(), 1));
    }
}
