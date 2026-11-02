// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/ComparisonCalculationHealPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.healpriest;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationHealPriest extends ComparisonCalculationBase {
    public ComparisonCalculationHealPriest() {
    }

    public ComparisonCalculationHealPriest(String name) {
        this.name = name;
    }

    private String name = "";

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String value) {
        this.name = value;
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

    @Override
    public float getOverallPoints() {
        float f = 0f;
        for (float f2 : this.subPoints)
            f += f2;
        return f;
    }

    @Override
    public void setOverallPoints(float value) {
        float val = value;
        for (int x = 1; x < this.subPoints.length; x++)
            val -= this.subPoints[x];
        this.subPoints[0] = val;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        for (int x = 0; x < this.subPoints.length; x++)
            if (this.subPoints[x] < 0f)
                this.subPoints[x] = 0f;
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getHealPoints() {
        return this.subPoints[0];
    }

    public void setHealPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getRegenPoints() {
        return this.subPoints[1];
    }

    public void setRegenPoints(float value) {
        this.subPoints[1] = value;
    }

    public float getHastePoints() {
        return this.subPoints[2];
    }

    public void setHastePoints(float value) {
        this.subPoints[2] = value;
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

    private boolean equipped = false;

    @Override
    public boolean getEquipped() {
        return this.equipped;
    }

    @Override
    public void setEquipped(boolean value) {
        this.equipped = value;
    }

    private boolean partEquipped = false;

    @Override
    public boolean getPartEquipped() {
        return this.partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        this.partEquipped = value;
    }
}
