// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/ComparisonCalculationHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationHunter extends ComparisonCalculationBase {
    private String name = "";
    private String description = "";

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

    private float[] subPoints = new float[] { 0f, 0f, 0f, 0f };
    private float overallPoints = 0f;

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getHunterDPSPoints() {
        return this.subPoints[0];
    }

    public void setHunterDPSPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getPetDPSPoints() {
        return this.subPoints[1];
    }

    public void setPetDPSPoints(float value) {
        this.subPoints[1] = value;
    }

    public float getHunterSurvPoints() {
        return this.subPoints[2];
    }

    public void setHunterSurvPoints(float value) {
        this.subPoints[2] = value;
    }

    public float getPetSurvPoints() {
        return this.subPoints[3];
    }

    public void setPetSurvPoints(float value) {
        this.subPoints[3] = value;
    }

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
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

    @Override
    public boolean getPartEquipped() {
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        partEquipped = value;
    }

    private boolean partEquipped;

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s: (%sO %sHD %sPD %sHS %sPS)",
                getName(), rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getOverallPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getHunterDPSPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getPetDPSPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getHunterSurvPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getPetSurvPoints())));
    }
}
