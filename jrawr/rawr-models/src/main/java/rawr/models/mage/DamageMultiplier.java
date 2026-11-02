// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Encounter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class DamageMultiplier {
    public float multiplier;
    public float startTime;
    public float endTime;
    public boolean relativeTime;

    public DamageMultiplier() {
        relativeTime = true;
        startTime = 0.0f;
        endTime = 1.0f;
        multiplier = 1.0f;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public boolean getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(boolean relativeTime) {
        this.relativeTime = relativeTime;
    }
}
