// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SequenceGroup.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;

public class SequenceGroup {
    public boolean unavailableForMinManaCorrections;
    public double mana;
    public double threat;
    public double duration;

    public double getMps() {
        if (duration > 0.0) {
            return mana / duration;
        } else {
            return 0.0;
        }
    }

    public double getTps() {
        if (duration > 0.0) {
            return threat / duration;
        } else {
            return 0.0;
        }
    }

    public boolean getManaGemActivation() {
        for (SequenceItem item : item) {
            if (item.getCastingState().manaGemEffect) return true;
        }
        return false;
    }

    public List<SequenceItem> item = new ArrayList<SequenceItem>();
    public List<CooldownConstraint> constraint = new ArrayList<CooldownConstraint>();

    public double getMinTime() {
        int mini = Integer.MAX_VALUE;
        double min = 0;
        for (SequenceItem item : item) {
            if (item.superIndex < mini) {
                mini = item.superIndex;
                min = item.getMinTime();
            }
        }
        return min;
    }

    public void setMinTime(double value) {
        for (SequenceItem item : item) {
            item.setMinTime(value);
        }
    }

    public double getMaxTime() {
        double t = 0;
        double max = SequenceItem.calculations.getCalculationOptions().getFightDuration();
        for (SequenceItem item : item) {
            max = Math.min(max, item.getMaxTime() - t);
            t += item.duration;
        }
        return max;
    }

    public void setMaxTime(double value) {
        for (SequenceItem item : item) {
            item.setMaxTime(value);
        }
    }

    public int getSegment() {
        if (item.size() == 0) return -1;
        int seg = Integer.MAX_VALUE;
        for (SequenceItem item : item) {
            seg = Math.min(seg, item.getSegment());
        }
        return seg;
    }

    public void add(SequenceItem item) {
        mana += item.getMps() * item.duration;
        threat += item.getTps() * item.duration;
        duration += item.duration;
        this.item.add(item);
    }

    public void addRange(Iterable<SequenceItem> collection) {
        for (SequenceItem item : collection) {
            add(item);
        }
    }

    public void sortByMps(double minMps, double maxMps) {
        List<SequenceItem> Item2 = new ArrayList<SequenceItem>(item);
        List<SequenceItem> sorted = new ArrayList<SequenceItem>();
        while (Item2.size() > 0) {
            for (SequenceItem item : Item2) {
                item.tail = new ArrayList<SequenceGroup>(item.group);
            }

            for (SequenceItem item : Item2) {
                for (SequenceItem tailitem : Item2) {
                    List<SequenceGroup> intersect = ListUtils.intersect(item.group, tailitem.group);
                    if (intersect.size() > 0) {
                        item.tail = ListUtils.intersect(intersect, item.tail);
                    }
                }
            }

            SequenceItem best = null;
            for (SequenceItem item : Item2) {
                if (best == null || compare(item, best, (sorted.size() > 0) ? sorted.get(sorted.size() - 1).group : null, minMps, maxMps) < 0) {
                    best = item;
                }
            }
            Item2.remove(best);
            sorted.add(best);
        }
        //Item = sorted;
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).superIndex = i;
        }
    }

    public void setSuperIndex() {
        for (int i = 0; i < item.size(); i++) {
            item.get(i).superIndex = i;
        }
    }

    private int compare(SequenceItem x, SequenceItem y, List<SequenceGroup> tail, double minMps, double maxMps) {
        boolean xsingletail = x.tail.size() > 0;
        boolean ysingletail = y.tail.size() > 0;
        int compare = Boolean.compare(ysingletail, xsingletail);
        if (compare != 0) return compare;
        int xintersect = (tail == null) ? 0 : ListUtils.intersect(x.group, tail).size();
        int yintersect = (tail == null) ? 0 : ListUtils.intersect(y.group, tail).size();
        compare = Integer.compare(yintersect, xintersect);
        if (compare != 0) return compare;
        return Sequence.compareMps(x.getMps(), y.getMps(), minMps, maxMps);
    }

    // helper variables
    public int orderIndex;
}
