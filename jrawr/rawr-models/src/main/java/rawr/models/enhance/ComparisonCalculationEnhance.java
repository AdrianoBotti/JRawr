// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/ComparisonCalculationsEnhance.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

import rawr.base.character.Character;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

public class ComparisonCalculationEnhance extends ComparisonCalculationBase {
    private String name = "";
    private String desc = "";
    private float overallPoints = 0f;
    private float[] subPoints = new float[] { 0f };
    private Item item = null;
    private ItemInstance itemInstance = null;
    private boolean equipped = false;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {
        name = value;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public void setDescription(String value) {
        desc = value;
    }

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getDPSPoints() {
        return subPoints[0];
    }

    public void setDPSPoints(float value) {
        subPoints[0] = value;
    }

    @Override
    public Item getItem() {
        return item;
    }

    @Override
    public void setItem(Item value) {
        item = value;
    }

    @Override
    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    @Override
    public void setItemInstance(ItemInstance value) {
        itemInstance = value;
    }

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

    private boolean partEquipped = false;

    @Override
    public String getBaseStat() {
        return " Attack Power";
    }

    @Override
    public boolean getBaseStatOption(Character character) {
        CalculationOptionsEnhance calcOpts = (CalculationOptionsEnhance) character.getCalculationOptions();
        return calcOpts.getBaseStatOption();
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s: (%dO %dDPS)", name, (long) Math.rint(overallPoints), (long) Math.rint(getDPSPoints()));
    }
}
