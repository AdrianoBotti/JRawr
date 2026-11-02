// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CalculationOptionsHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import java.util.ArrayList;
import java.util.List;

public class ShotGroup {
    public ShotGroup(String name) {
        this.name = name;
    }

    public ShotGroup(String name, ShotGroup sg) {
        this.name = name;
        this.shotList = sg.shotList;
    }

    public ShotGroup(String name, List<Shot> shotList) {
        this.name = name;
        this.shotList = shotList;
    }

    public String name = "Invalid";
    public List<Shot> shotList = null;

    public List<Shot> getShotList() {
        return shotList != null ? shotList : new ArrayList<Shot>();
    }

    public void setShotList(List<Shot> value) {
        shotList = value;
    }

    public boolean equals(List<Shot> shots2Compare) {
        return (shotList == shots2Compare);
    }
}
