// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

public class NullCombatTable extends CombatTable {
    public NullCombatTable() {
        block = crit = hit = dodge = glance = miss = parry = 0;
    }
}
