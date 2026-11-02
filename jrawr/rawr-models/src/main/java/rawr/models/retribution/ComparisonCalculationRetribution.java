// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/ComparisonCalculationRetribution.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationRetribution extends ComparisonCalculationBase {

    private String name = "";
    private String description = "";
    private float overallPoints = 0f;
    private float[] subPoints = new float[] { 0f };
    private Item item = null;
    private ItemInstance itemInstance = null;
    private boolean equipped = false;
    private boolean partEquipped = false;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String value) {
        this.description = value;
    }

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getDPSPoints() {
        return this.subPoints[0];
    }

    public void setDPSPoints(float value) {
        this.subPoints[0] = value;
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public void setItem(Item value) {
        this.item = value;
    }

    @Override
    public ItemInstance getItemInstance() {
        return this.itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance value) {
        this.itemInstance = value;
    }

    @Override
    public boolean getEquipped() {
        return this.equipped;
    }

    @Override
    public void setEquipped(boolean value) {
        this.equipped = value;
    }

    @Override
    public boolean getPartEquipped() {
        return this.partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        this.partEquipped = value;
    }
}
