// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.List;

public class CycleState {
    public List<CycleStateTransition> transitions;
    public int index;
    public String name;

    @Override
    public String toString() {
        return name;
    }
}
