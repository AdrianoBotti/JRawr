// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.AvailableItemGenerator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Split out of AvailableItemGenerator.java when rawr-base was reorganized into sub-packages: the
// C# type is public, and the batch tools reference it from another package.
package rawr.base.optimizer;

import rawr.base.items.ItemInstance;

import java.util.List;

/**
 * One node of a direct-upgrade tree: an item instance assembled from the first N gems (and
 * optionally the enchant) of a gemming, plus the list of entries that add the next gem or the
 * enchant. The greedy direct-upgrade scans descend these trees, evaluating one level at a time.
 */
public class DirectUpgradeEntry {
    public ItemInstance itemInstance;
    public List<DirectUpgradeEntry> directUpgradeList;
}
