// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/CalculationsElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (ComparisonCalculationElemental, C# lines 807-876).
package rawr.models.elemental;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.models.mage.NetFormat;

public class ComparisonCalculationElemental extends ComparisonCalculationBase {
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

    private boolean equipped = false;

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
        return String.format(java.util.Locale.ROOT, "%s: (%sO %sBurst %sSustained)", name,
                NetFormat.netDoubleToString(Math.rint(overallPoints)),
                NetFormat.netDoubleToString(Math.rint(getBurstPoints())),
                NetFormat.netDoubleToString(Math.rint(getSustainedPoints())));
    }
}
