// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.Locale;

public class CycleStateTransition {
    public CycleState targetState;
    public Cycle cycle;
    public Spell spell;
    public float pause;
    private float transitionProbability;

    public float getTransitionProbability() {
        return transitionProbability;
    }

    public void setTransitionProbability(float value) {
        transitionProbability = value;
    }

    @Override
    public String toString() {
        if (spell != null) {
            return String.format(Locale.ROOT, "%s => %s : %.2f%%", spell.getName(), targetState, 100 * transitionProbability);
        } else if (cycle != null) {
            return String.format(Locale.ROOT, "%s => %s : %.2f%%", cycle.name, targetState, 100 * transitionProbability);
        } else if (pause > 0) {
            return String.format(Locale.ROOT, "%.2f sec => %s : %.2f%%", pause, targetState, 100 * transitionProbability);
        }
        return super.toString();
    }
}
