// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import java.util.LinkedHashMap;

/**
 * The persisted root of the item filter: per-model relevant item types, the top-level regex rule
 * tree and the "Other" catch-all flag.
 *
 * <p>One instance lives statically inside {@link ItemFilter}; it is what {@code ItemFilter.xml}
 * serializes. {@link #relevantItemTypes} maps a model name (e.g. {@code "Mage"}) to the
 * {@link ItemTypeList} that model can use, {@link #regexList} is the root
 * {@link ItemFilterRegexList}, and {@link #otherRegexEnabled} is the default decision for items
 * no additive rule matches ({@code true} = include). All three are public mutable fields because
 * the original's serializer needed the exact shape; do not replace the objects, mutate them.</p>
 */
public class ItemFilterData {
    public LinkedHashMap<String, ItemTypeList> relevantItemTypes = new LinkedHashMap<String, ItemTypeList>();
    public ItemFilterRegexList regexList = new ItemFilterRegexList();
    public boolean otherRegexEnabled = true;
}
