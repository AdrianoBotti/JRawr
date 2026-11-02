// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/ComparisonCalculationMoonkin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationMoonkin extends ComparisonCalculationBase {
    private String name = "";

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

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getDamagePoints() {
        return subPoints[0];
    }

    public void setDamagePoints(float value) {
        subPoints[0] = value;
    }

    public float getRawDamagePoints() {
        return subPoints[1];
    }

    public void setRawDamagePoints(float value) {
        subPoints[1] = value;
    }

    private Item item = null;

    @Override
    public Item getItem() {
        return item;
    }

    @Override
    public void setItem(Item value) {
        item = value;
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

    boolean equipped = false;

    @Override
    public boolean getEquipped() {
        return equipped;
    }

    @Override
    public void setEquipped(boolean value) {
        equipped = value;
    }

    @Override
    public boolean getPartEquipped() {
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        partEquipped = value;
    }

    private boolean partEquipped;
}
