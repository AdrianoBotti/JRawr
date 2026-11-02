// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.AvailableItemGenerator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Split out of AvailableItemGenerator.java when rawr-base was reorganized into sub-packages: the
// C# type is public, and Item in the items package references it.
package rawr.base.optimizer;

import rawr.base.items.Enchant;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-{@link rawr.base.items.Item} cache of everything the generator computed for it: how many gems
 * it can hold, which gemmed and enchanted instances are available (keyed by gemmed id), the default
 * (currently equipped) instance, the full instance list, the direct-upgrade trees, the gem/enchant
 * combinations that can be used in generated upgrades, and the slots the item fits. The data is
 * shared process-wide through the item; the generator clears and rebuilds it on every pool
 * initialization and can snapshot it for later restore.
 */
public class ItemAvailabilityInformation {
    public int gemCount;
    public Map<String, Boolean> itemAvailable = new LinkedHashMap<String, Boolean>();
    public ItemInstance defaultItemInstance;
    public List<ItemInstance> itemList = new ArrayList<ItemInstance>();
    public List<DirectUpgradeEntry> matchingDirectUpgradeList;
    public List<DirectUpgradeEntry> nonMatchingDirectUpgradeList;
    public List<DirectUpgradeEntry> singleDirectUpgradeList;
    public Map<String, DirectUpgradeEntry> matchingMap;
    public Map<String, DirectUpgradeEntry> nonMatchingMap;
    public List<Enchant> generativeEnchants;
    public int[] validSlots;
    public boolean positiveCostItem;
}
