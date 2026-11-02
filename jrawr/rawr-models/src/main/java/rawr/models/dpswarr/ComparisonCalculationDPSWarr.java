// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/ComparisonCalculationsDPSWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpswarr;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

class ComparisonCalculationDPSWarr extends ComparisonCalculationBase {
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

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

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

    public float getSurvPoints() {
        return this.subPoints[1];
    }

    public void setSurvPoints(float value) {
        this.subPoints[1] = value;
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

    private boolean partEquipped;

    @Override
    public boolean getPartEquipped() {
        return this.partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        this.partEquipped = value;
    }

    @Override
    public String toString() {
        return DPSWarrFormat.format("{0}: ({1}O {2}D {3}S)", this.name, (float) Math.rint(this.overallPoints),
                (float) Math.rint(getDPSPoints()), (float) Math.rint(getSurvPoints()));
    }
}
