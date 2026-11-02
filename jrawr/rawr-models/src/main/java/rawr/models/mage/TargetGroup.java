// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Encounter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;

public class TargetGroup {
    public float entranceTime;
    public float exitTime;
    public boolean relativeTime;
    public List<DamageMultiplier> multipliers;

    public TargetGroup() {
        entranceTime = 0.0f;
        exitTime = 1.0f;
        relativeTime = true;
        multipliers = new ArrayList<DamageMultiplier>();
    }

    public float getEntranceTime() {
        return entranceTime;
    }

    public void setEntranceTime(float entranceTime) {
        this.entranceTime = entranceTime;
    }

    public float getExitTime() {
        return exitTime;
    }

    public void setExitTime(float exitTime) {
        this.exitTime = exitTime;
    }

    public boolean getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(boolean relativeTime) {
        this.relativeTime = relativeTime;
    }

    public List<DamageMultiplier> getMultipliers() {
        return multipliers;
    }

    public void setMultipliers(List<DamageMultiplier> multipliers) {
        this.multipliers = multipliers;
    }
}
