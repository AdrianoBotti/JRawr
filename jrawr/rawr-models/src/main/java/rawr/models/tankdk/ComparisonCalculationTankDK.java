// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/ComparisonCalculationTankDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.character.Character;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

class ComparisonCalculationTankDK extends ComparisonCalculationBase {
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

    public float getSurvival() {
        return this.subPoints[0];
    }

    public void setSurvival(float value) {
        this.subPoints[0] = value;
        this.overallPoints = getSurvival() + getMitigation() + getThreat();
    }

    public float getMitigation() {
        return this.subPoints[1];
    }

    public void setMitigation(float value) {
        this.subPoints[1] = value;
        this.overallPoints = getSurvival() + getMitigation() + getThreat();
    }

    public float getThreat() {
        return this.subPoints[2];
    }

    public void setThreat(float value) {
        this.subPoints[2] = value;
        this.overallPoints = getSurvival() + getMitigation() + getThreat();
    }

    public float getBurstTime() {
        return this.subPoints[0];
    }

    public void setBurstTime(float value) {
        this.subPoints[0] = value;
    }

    public float getReactionTime() {
        return this.subPoints[1];
    }

    public void setReactionTime(float value) {
        this.subPoints[1] = value;
    }

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        // Reminder: this is for an individual item.  Do not apply weighting to this class.
        if (this.overallPoints == 0f) {
            this.overallPoints = getSurvival() + getMitigation() + getThreat();
        }
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
        this.overallPoints = getSurvival() + getMitigation() + getThreat();
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
        return partEquipped;
    }

    @Override
    public void setPartEquipped(boolean value) {
        partEquipped = value;
    }

    /**
     * Name of the Stat to set to 1.00 for relative stats calcs
     */
    @Override
    public String getBaseStat() {
        return " Stamina";
    }

    /**
     * User Option whether to use the Base Stat feature for relative stats calcs
     */
    @Override
    public boolean getBaseStatOption(Character character) {
        return true;
    }

    @Override
    public String toString() {
        // So that the ToString() function can be used as a base-line comparison if all else fails.
        return String.format("%s: (%sO = %sM %sS %sT)", getName(), getOverallPoints(), getMitigation(), getSurvival(), getThreat());
    }
}
