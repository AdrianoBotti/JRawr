// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.Locale;

public class CycleControlledStateTransition extends CycleStateTransition {
    private float rawProbability;
    private int controlIndex;
    private int controlValue;
    private int[] controlStates;

    public void setControls(int controlIndex, int[] controlStates, int controlValue) {
        this.controlIndex = controlIndex;
        this.controlStates = controlStates;
        this.controlValue = controlValue;
    }

    public float getTransitionProbability() {
        return (controlStates[controlIndex] == controlValue) ? rawProbability : 0.0f;
    }

    public void setTransitionProbability(float value) {
        rawProbability = value;
    }

    @Override
    public String toString() {
        if (spell != null) {
            return String.format(Locale.ROOT, "%s => %s : %.2f%%", spell.getName(), targetState, 100 * rawProbability);
        } else if (cycle != null) {
            return String.format(Locale.ROOT, "%s => %s : %.2f%%", cycle.name, targetState, 100 * rawProbability);
        } else if (pause > 0) {
            return String.format(Locale.ROOT, "%.2f sec => %s : %.2f%%", pause, targetState, 100 * rawProbability);
        }
        return super.toString();
    }
}
