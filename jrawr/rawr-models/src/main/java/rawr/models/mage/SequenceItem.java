// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SequenceItem.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SequenceItem implements Cloneable {
    public static DisplayCalculations calculations;

    private SequenceItem() {
    }

    public SequenceItem(int index, double duration) {
        this(index, duration, null);
    }

    public SequenceItem(int index, double duration, List<SequenceGroup> group) {
        if (group == null) group = new ArrayList<SequenceGroup>();
        this.group = group;
        this.index = index;
        this.variableType = calculations.solutionVariable.get(index).type;
        mps = calculations.solutionVariable.get(index).mps;
        tps = calculations.solutionVariable.get(index).tps;
        this.duration = duration;
        this.cycle = calculations.solutionVariable.get(index).cycle;
        this.castingState = calculations.solutionVariable.get(index).state;
        this.segment = calculations.solutionVariable.get(index).segment;
        if (castingState == null) castingState = calculations.getBaseState();

        minTime = 0.0;
        maxTime = calculations.getCalculationOptions().getFightDuration();

        if (variableType == VariableType.Wand) {
            cycle = calculations.getWand().getCycle();
            mps = cycle.getManaPerSecond();
            tps = cycle.getThreatPerSecond();
        } else if (variableType == VariableType.ManaGem) {
            mps = 0.0;
            tps = calculations.manaGemTps;
        } else if (variableType == VariableType.ManaPotion) {
            mps = 0.0;
            tps = calculations.manaPotionTps;
        } else if (variableType == VariableType.Drinking) {
            maxTime = 0.0;
            mps = -calculations.getBaseState().getManaRegenDrinking();
            tps = 0.0;
        } else if (variableType == VariableType.TimeExtension) {
            minTime = maxTime;
            tps = 0.0;
        } else if (variableType == VariableType.AfterFightRegen) {
            mps = -calculations.getBaseState().getManaRegenDrinking();
            tps = 0.0;
            minTime = maxTime;
        }
    }

    private int index;

    public int getIndex() {
        return index;
    }

    private VariableType variableType;

    public VariableType getVariableType() {
        return variableType;
    }

    public boolean isEvocation() {
        return variableType == VariableType.Evocation || variableType == VariableType.EvocationIV || variableType == VariableType.EvocationHero || variableType == VariableType.EvocationIVHero;
    }

    public boolean isManaPotionOrGem() {
        return variableType == VariableType.ManaPotion || variableType == VariableType.ManaGem;
    }

    private int segment;

    public int getSegment() {
        return segment;
    }

    public void setSegment(int segment) {
        this.segment = segment;
    }

    public double duration;
    public double timestamp;

    private double minTime;

    public double getMinTime() {
        return minTime;
    }

    public void setMinTime(double value) {
        minTime = Math.max(minTime, value);
    }

    private double maxTime;

    public double getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(double value) {
        maxTime = Math.min(maxTime, value);
    }

    public void setTimeConstraint(double minTime, double maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public List<SequenceGroup> group;
    public SequenceGroup superGroup;

    // helper variables
    public int superIndex;
    public List<SequenceGroup> tail;
    public int cooldownHex;
    public int orderIndex;

    private Cycle cycle;

    public Cycle getCycle() {
        return cycle;
    }

    private CastingState castingState;

    public CastingState getCastingState() {
        return castingState;
    }

    private double mps;

    public double getMps() {
        return mps;
    }

    private double tps;

    public double getTps() {
        return tps;
    }

    @Override
    public SequenceItem clone() {
        SequenceItem clone = new SequenceItem();
        clone.index = index;
        clone.variableType = variableType;
        clone.segment = segment;
        clone.duration = duration;
        clone.timestamp = timestamp;
        clone.minTime = minTime;
        clone.maxTime = maxTime;
        clone.superGroup = superGroup;
        clone.superIndex = superIndex;
        clone.tail = tail;
        clone.cooldownHex = cooldownHex;
        clone.orderIndex = orderIndex;
        clone.cycle = cycle;
        clone.castingState = castingState;
        clone.mps = mps;
        clone.tps = tps;
        clone.group = new ArrayList<SequenceGroup>(group);
        return clone;
    }

    @Override
    public String toString() {
        if (cycle == null) return String.format(Locale.ROOT, "%d: %s", getSegment(), getVariableType());
        return String.format(Locale.ROOT, "%d: %s", getSegment(), castingState.getBuffLabel() + "+" + cycle.name);
    }
}
