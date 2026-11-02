// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/ComparisonCalculationRestoSham.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationRestoSham extends ComparisonCalculationBase {
    public ComparisonCalculationRestoSham() {
    }

    public ComparisonCalculationRestoSham(String szName) {
        this.name = szName;
    }

    private String name = "";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {
        name = value;
    }

    private String desc = "";

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public void setDescription(String value) {
        desc = value;
    }

    private float overallPoints = 0.0f;

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
        return itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance value) {
        itemInstance = value;
    }

    private boolean bEquipped;

    @Override
    public boolean getEquipped() {
        return bEquipped;
    }

    @Override
    public void setEquipped(boolean value) {
        bEquipped = value;
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
}
