// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CalculationOptionsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.models.mage.statedescription.StateDescriptionDelegate;

public class CooldownRestriction {
    public double timeStart;
    public double timeEnd;
    public StateDescriptionDelegate isMatch;

    public double getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(double timeStart) {
        this.timeStart = timeStart;
    }

    public double getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(double timeEnd) {
        this.timeEnd = timeEnd;
    }

    public StateDescriptionDelegate isMatch() {
        return isMatch;
    }

    public void setIsMatch(StateDescriptionDelegate isMatch) {
        this.isMatch = isMatch;
    }
}
