// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CalculationOptionsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class CooldownStackingCacheEntry {
    public double effect1Duration;
    public double effect1Cooldown;
    public double effect2Duration;
    public double effect2Cooldown;
    public double maximumStackingDuration;

    public double getEffect1Duration() {
        return effect1Duration;
    }

    public void setEffect1Duration(double effect1Duration) {
        this.effect1Duration = effect1Duration;
    }

    public double getEffect1Cooldown() {
        return effect1Cooldown;
    }

    public void setEffect1Cooldown(double effect1Cooldown) {
        this.effect1Cooldown = effect1Cooldown;
    }

    public double getEffect2Duration() {
        return effect2Duration;
    }

    public void setEffect2Duration(double effect2Duration) {
        this.effect2Duration = effect2Duration;
    }

    public double getEffect2Cooldown() {
        return effect2Cooldown;
    }

    public void setEffect2Cooldown(double effect2Cooldown) {
        this.effect2Cooldown = effect2Cooldown;
    }

    public double getMaximumStackingDuration() {
        return maximumStackingDuration;
    }

    public void setMaximumStackingDuration(double maximumStackingDuration) {
        this.maximumStackingDuration = maximumStackingDuration;
    }
}
