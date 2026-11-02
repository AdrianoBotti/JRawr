// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ComparisonCalculationMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

import java.util.Locale;

public class ComparisonCalculationMage extends ComparisonCalculationBase {
    private String name = "";
    private String description = "";
    private float overallPoints = 0f;
    private float[] subPoints = new float[]{0f, 0f};
    private Item item = null;
    private ItemInstance itemInstance = null;
    private boolean equipped = false;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float overallPoints) {
        this.overallPoints = overallPoints;
    }

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] subPoints) {
        this.subPoints = subPoints;
    }

    public float getDpsRating() {
        return this.subPoints[0];
    }

    public float getSurvivabilityRating() {
        return this.subPoints[1];
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public ItemInstance getItemInstance() {
        return this.itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance itemInstance) {
        this.itemInstance = itemInstance;
    }

    @Override
    public boolean getEquipped() {
        return this.equipped;
    }

    @Override
    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    private boolean partEquipped;

    @Override
    public boolean getPartEquipped() {
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean partEquipped) {
        this.partEquipped = partEquipped;
    }

    @Override
    public String toString() {
        if (this.subPoints.length == 2) {
            return String.format(Locale.ROOT, "%s: (%.2f Dps, %.2f Survivability)", getName(), Math.rint(getDpsRating()), Math.rint(getSurvivabilityRating()));
        }
        return null;
    }
}
