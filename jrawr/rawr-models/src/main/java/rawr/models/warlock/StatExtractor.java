// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/StatUtils.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.warlock;

import rawr.base.stats.Stats;

@FunctionalInterface
public interface StatExtractor {
    float extract(Stats stats);
}
