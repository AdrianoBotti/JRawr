// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class Segment {
    public int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int value) {
        index = value;
    }

    public double duration;

    public double getDuration() {
        return duration;
    }

    public void setDuration(double value) {
        duration = value;
    }

    public double timeStart;

    public double getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(double value) {
        timeStart = value;
    }

    public double getTimeEnd() {
        return timeStart + duration;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%d (%s - %s)", index, CalculationsMage.timeFormat(timeStart), CalculationsMage.timeFormat(getTimeEnd()));
    }
}
