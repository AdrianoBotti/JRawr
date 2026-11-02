// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Encounter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;

public class Encounter {
    public List<DamageMultiplier> globalMultipliers;
    public List<TargetGroup> targetGroups;

    public Encounter() {
        globalMultipliers = new ArrayList<DamageMultiplier>();
        targetGroups = new ArrayList<TargetGroup>();
    }

    public List<DamageMultiplier> getGlobalMultipliers() {
        return globalMultipliers;
    }

    public void setGlobalMultipliers(List<DamageMultiplier> globalMultipliers) {
        this.globalMultipliers = globalMultipliers;
    }

    public List<TargetGroup> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<TargetGroup> targetGroups) {
        this.targetGroups = targetGroups;
    }
}
