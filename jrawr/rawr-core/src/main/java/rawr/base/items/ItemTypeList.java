// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.calculations.CalculationsBase;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list of {@link ItemType} used as a value in the persisted filter data
 * ({@link ItemFilterData#relevantItemTypes}) and returned by
 * {@link ItemFilter#getRelevantItemTypesList(CalculationsBase)}.
 *
 * <p>A named subclass of {@code ArrayList} because the original used a distinct collection type
 * here and the XML schema writes the elements as {@code ArrayOfItemType}; it adds no behavior.
 * The list is also a per-model cache: the first call for a model seeds it from that model's
 * {@code getRelevantItemTypes()}, and later calls return the same mutable list, so editing it
 * edits the model's relevance set (which is how the UI exposes the filter option).</p>
 */
public class ItemTypeList extends ArrayList<ItemType> {
    public ItemTypeList() {
        super();
    }

    public ItemTypeList(Collection<ItemType> collection) {
        super(collection);
    }
}
