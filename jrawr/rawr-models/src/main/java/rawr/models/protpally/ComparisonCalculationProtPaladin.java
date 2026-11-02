// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/ComparisonCalculationProtPaladin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

import java.util.Locale;

public class ComparisonCalculationProtPaladin extends ComparisonCalculationBase {
    private String name = "";
    private String description = "";
    private float overallPoints = 0f;
    private float[] subPoints = new float[] { 0f, 0f, 0f };
    private Item item = null;
    private ItemInstance itemInstance = null;
    private boolean equipped = false;
    private boolean partEquipped = false;

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String value) { name = value; }

    @Override
    public String getDescription() { return description; }

    @Override
    public void setDescription(String value) { description = value; }

    @Override
    public float getOverallPoints() { return overallPoints; }

    @Override
    public void setOverallPoints(float value) { overallPoints = value; }

    @Override
    public float[] getSubPoints() { return subPoints; }

    @Override
    public void setSubPoints(float[] value) { subPoints = value; }

    public float getSurvivalPoints() { return subPoints[0]; }
    public void setSurvivalPoints(float value) { subPoints[0] = value; }

    public float getMitigationPoints() { return subPoints[1]; }
    public void setMitigationPoints(float value) { subPoints[1] = value; }

    public float getThreatPoints() { return subPoints[2]; }
    public void setThreatPoints(float value) { subPoints[2] = value; }

    @Override
    public Item getItem() { return item; }

    @Override
    public void setItem(Item value) { item = value; }

    @Override
    public ItemInstance getItemInstance() { return itemInstance; }

    @Override
    public void setItemInstance(ItemInstance value) { itemInstance = value; }

    @Override
    public boolean getEquipped() { return equipped; }

    @Override
    public void setEquipped(boolean value) { equipped = value; }

    @Override
    public boolean getPartEquipped() { return partEquipped; }

    @Override
    public void setPartEquipped(boolean value) { partEquipped = value; }

    @Override
    public String toString() {
        // C# Math.Round(double) on the four float ratings; banker's rounding via Math.rint, then
        // the .NET default double ToString.
        return String.format(Locale.ROOT, "%s: (%sO %sM %sS %sT)", name,
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(overallPoints)),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getMitigationPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getSurvivalPoints())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.rint(getThreatPoints())));
    }
}
