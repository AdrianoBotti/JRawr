// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/Charting.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

import rawr.base.stats.Stats;

/**
 * Class used by stat relative weights custom chart.
 */
final class StatRelativeWeight {
    public StatRelativeWeight(String name, Stats stat) {
        this.stat = stat;
        this.name = name;
        this.change = 0f;
    }

    public Stats stat;
    public String name;
    public float change;
}
