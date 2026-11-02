// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemCache.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the item registry/lookup/duplicate handling/unique-id rules are ported. Omitted:
// - RelevantItems/GetRelevantItems/GetUnfilteredRelevantItems/GetRelevantItemsInternal (need
//   CalculationsBase.IsItemRelevant/HasRelevantStats/TargetClass and Buff.RelevantSetBonuses, not
//   ported yet);
// - Save/Load (XML file I/O; rawr-data ItemCacheLoader populates this registry, DECISIONS #15);
// - SaveItemCost/LoadItemCost/ResetItemCost/LoadTokenItemCost (WinForms FormMain-driven, Item.Cost
//   is not ported);
// - ItemsChanged event and the Calculations.ModelChanged hook (UI notification only).
package rawr.base.items;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The process-wide registry of every item Rawr knows about: the in-memory game database that the
 * model calculations, the item picker and the optimizer read from.
 *
 * <p>Items are loaded once from {@code ItemCache.xml} by {@code rawr-data}'s item loader and
 * inserted with {@link #addItem(Item)}; there is no per-character item list. The registry is a
 * {@code TreeMap} keyed by item id, so {@link #allItems()} is ordered by id (the original
 * enumerates it in that order; keep it). Lookups and mutations are synchronized on the map.</p>
 *
 * <h2>Lifecycles and caches</h2>
 *
 * <ul>
 *   <li>{@link #allItems()} &mdash; lazily snapshotted array of all items, held in a volatile
 *       field under double-checked locking and rebuilt on the next call after
 *       {@link #onItemsChanged()}.</li>
 *   <li>{@link #addItem(Item)} / {@link #deleteItem(Item)} &mdash; mutate the registry and then
 *       invalidate every derived cache. Replacing or deleting an item first flags the old object
 *       {@link Item#setInvalid(boolean) invalid} so instances still holding it re-resolve their
 *       lazy item reference; deletion never unloads the object from memory.</li>
 *   <li>{@link #onItemsChanged()} &mdash; clears the {@code allItems} snapshot and the
 *       relevant-items caches. Called by add/delete and after any change that can affect
 *       relevance (e.g. loading a character's filter overrides).</li>
 *   <li>{@link #invalidateCachedStats()} &mdash; walks every item and drops its item-level stat
 *       caches; used after a global change such as loading new gem data.</li>
 *   <li>{@link #clear()} &mdash; empties the registry and every cache (data reload/tests).</li>
 * </ul>
 *
 * <h2>Relevant items</h2>
 *
 * <p>The optimizer and the item picker need only the subset of the database a model can use.
 * Three views exist, all iterating {@link #allItems()} in id order: the model's own
 * {@code isItemRelevant} test plus faction fit ({@link #getUnfilteredRelevantItems}), the same
 * plus the user's {@link ItemFilter} tree ({@link #getRelevantItems(CalculationsBase,
 * CharacterRace)}), and the current model's view ({@link #relevantItems()}). Only
 * {@code getRelevantItems} caches, and only a single (model, race) pair at a time: a different
 * model or race recomputes it. Every cache dies in {@link #onItemsChanged()} and
 * {@link #clear()}.</p>
 *
 * <h2>Unique id groups</h2>
 *
 * <p>{@link #autoSetUniqueId(Item)} maintains {@link Item#getUniqueId()} for unique items whose
 * drop exists in normal/heroic or 10-/25-player versions under the same name: it groups
 * same-name unique trinkets, rings and one-hand weapons by their item level (251 and 264 for
 * 10-player ICC, 264 and 277 for 25-player ICC, 258 and 271 for 10-player RS, 271 and 284 for
 * 25-player RS) and assigns the mutually-exclusive id lists, plus the hardcoded Ashen Verdict
 * ring families. Because 264 appears in two pairs and 271 in two pairs, a later assignment
 * overwrites an earlier one for those item levels; that overwrite is the original's behavior and
 * is kept. The name bucket is never pruned, so deleted items remain in it, but only entries
 * still present in {@link #getItems()} participate.</p>
 *
 * <p>Porting note: {@link #SAVED_FILE_NAME} is the original file name; the XML I/O itself lives
 * in {@code rawr-data} (see the {@code PARTIAL} note at the top of this file).</p>
 *
 * @see Item
 * @see ItemFilter
 */
public final class ItemCache {
    public static final String SAVED_FILE_NAME = "ItemCache.xml";

    private static final TreeMap<Integer, Item> ITEMS = new TreeMap<Integer, Item>();
    private static final Map<String, List<Item>> UNIQUE_ITEM_BY_NAME = new HashMap<String, List<Item>>();

    private static volatile Item[] allItems = null;

    private ItemCache() {
    }

    public static TreeMap<Integer, Item> getItems() {
        return ITEMS;
    }

    public static void invalidateCachedStats() {
        synchronized (ITEMS) {
            for (Item item : ITEMS.values()) {
                item.invalidateCachedData();
            }
        }
    }

    public static Item findItemById(int id) {
        if (id > 0) {
            synchronized (ITEMS) {
                return ITEMS.get(id);
            }
        }
        return null;
    }

    public static boolean containsItemId(int id) {
        if (id > 0) {
            synchronized (ITEMS) {
                return ITEMS.containsKey(id);
            }
        }
        return false;
    }

    public static Item addItem(Item item) {
        return addItem(item, true);
    }

    /**
     * Inserts (or replaces) an item and returns it. A previously registered item with the same
     * id is flagged invalid before being replaced, the new item's
     * {@link Item#getLastChange() lastChange} is stamped with the current time, and
     * {@link #autoSetUniqueId(Item)} is run. Unless {@code raiseEvent} is false, this ends with
     * {@link #onItemsChanged()}, so it also invalidates the relevant-item caches.
     */
    public static Item addItem(Item item, boolean raiseEvent) {
        if (item == null) return null;
        synchronized (ITEMS) {
            Item cachedItem = ITEMS.get(item.getId());
            if (cachedItem != null) {
                cachedItem.setInvalid(true);
            }
            item.setLastChange(System.currentTimeMillis());
            ITEMS.put(item.getId(), item);
            autoSetUniqueId(item);
        }
        if (raiseEvent) {
            onItemsChanged();
        }
        return item;
    }

    public static void deleteItem(Item item) {
        deleteItem(item, true);
    }

    /**
     * Removes the item with the same id from the registry after flagging it invalid, then
     * invalidates the caches. A no-op when the id is not registered. The {@code null} check is
     * the caller's; a {@code null} argument does nothing.
     */
    public static void deleteItem(Item item, boolean raiseEvent) {
        if (item != null) {
            synchronized (ITEMS) {
                Item cachedItem = ITEMS.get(item.getId());
                if (cachedItem != null) {
                    cachedItem.setInvalid(true);
                    ITEMS.remove(item.getId());
                }
            }
        }
        if (raiseEvent) {
            onItemsChanged();
        }
    }

    /**
     * Snapshot of all registered items, ordered by id. The returned array is shared with the
     * cache and replaced wholesale after any change; callers must not mutate its elements'
     * registration, and must re-fetch after calling a mutating method.
     */
    public static Item[] allItems() {
        Item[] cached = allItems;
        if (cached == null) {
            synchronized (ITEMS) {
                cached = allItems;
                if (cached == null) {
                    cached = ITEMS.values().toArray(new Item[0]);
                    allItems = cached;
                }
            }
        }
        return cached;
    }

    /**
     * Invalidates every derived item list: the {@code allItems} snapshot and the relevant-items
     * caches (including the current model's {@link #relevantItems()}). Call this after anything
     * that changes which items exist or how they filter; {@link #addItem(Item)} and
     * {@link #deleteItem(Item)} do it for you.
     */
    public static void onItemsChanged() {
        synchronized (ITEMS) {
            allItems = null;
            // C# ItemCache.OnItemsChanged also drops the cached filtered list (filter settings may
            // have changed, e.g. Character.loadItemFilterEnabledOverride).
            invalidateRelevantCachesLocked();
        }
    }

    /**
     * Drops the model/race-derived relevant-item caches without rebuilding the full item snapshot.
     * Call this when the current model changes, because the relevant list is model-specific.
     */
    public static void invalidateRelevantCaches() {
        synchronized (ITEMS) {
            invalidateRelevantCachesLocked();
        }
    }

    private static void invalidateRelevantCachesLocked() {
        cachedRelevantItems = null;
        cachedRelevantItemsModel = null;
        cachedRelevantItemsRace = null;
        relevantItems = null;
    }

    // C# ItemCache.cs:240-252 RelevantItems (the current model's own relevance + ItemFilter).
    private static volatile Item[] relevantItems = null;

    public static Item[] relevantItems() {
        if (relevantItems == null) {
            relevantItems = getRelevantItemsInternal(Calculations.getInstance());
        }
        return relevantItems;
    }

    // C# ItemCache.cs:253-266/268-290 GetUnfilteredRelevantItems/GetRelevantItems.
    private static CalculationsBase cachedRelevantItemsModel;
    private static CharacterRace cachedRelevantItemsRace;
    private static Item[] cachedRelevantItems;

    public static Item[] getUnfilteredRelevantItems(CalculationsBase model, CharacterRace race) {
        List<Item> itemList = new ArrayList<Item>();
        for (Item item : allItems()) {
            if (model.isItemRelevant(item) && item.fitsFaction(race)) {
                itemList.add(item);
            }
        }
        return itemList.toArray(new Item[0]);
    }

    /**
     * Relevant items for a model and race, with the user's {@link ItemFilter} applied. The result
     * is cached, but only for the (model, race) pair of the last call; a different pair rebuilds
     * it. Callers must treat the array as read-only.
     */
    public static Item[] getRelevantItems(CalculationsBase model, CharacterRace race) {
        if (cachedRelevantItems == null || cachedRelevantItemsModel != model || cachedRelevantItemsRace != race) {
            synchronized (ITEMS) {
                if (cachedRelevantItems == null || cachedRelevantItemsModel != model || cachedRelevantItemsRace != race) {
                    List<Item> itemList = new ArrayList<Item>();
                    for (Item item : allItems()) {
                        if (model.isItemRelevant(item) && ItemFilter.isItemRelevant(model, item) && item.fitsFaction(race)) {
                            itemList.add(item);
                        }
                    }
                    cachedRelevantItems = itemList.toArray(new Item[0]);
                    cachedRelevantItemsModel = model;
                    cachedRelevantItemsRace = race;
                }
            }
        }
        return cachedRelevantItems;
    }

    // C# ItemCache.cs:292-296 GetRelevantItemsInternal.
    public static Item[] getRelevantItemsInternal(CalculationsBase model) {
        List<Item> itemList = new ArrayList<Item>();
        for (Item item : allItems()) {
            if (model.isItemRelevant(item) && ItemFilter.isItemRelevant(model, item)) {
                itemList.add(item);
            }
        }
        return itemList.toArray(new Item[0]);
    }

    public static void clear() {
        synchronized (ITEMS) {
            ITEMS.clear();
            UNIQUE_ITEM_BY_NAME.clear();
            allItems = null;
            relevantItems = null;
            cachedRelevantItems = null;
            cachedRelevantItemsModel = null;
            cachedRelevantItemsRace = null;
        }
    }

    /// <summary>
    /// Matches the item against known rules for unique item groups and sets the UniqueId list.
    /// </summary>
    /**
     * Groups unique items that are the same drop in different versions under one
     * {@link Item#getUniqueId()} list, keyed by item name. Handles the ICC/Ruby Sanctum
     * normal/heroic pairs by item level (251/264, 264/277, 258/271, 271/284, later pairs
     * overwriting earlier ones) and the hardcoded Ashen Verdict ring families by id. Only unique
     * trinkets, rings and one-hand weapons are considered. The name bucket grows monotonically
     * (it is not pruned on delete) but stale entries are ignored because only items still present
     * in the registry take part in the pairing.
     */
    public static void autoSetUniqueId(Item item) {
        if (item.isUnique() && (item.getSlot() == ItemSlot.Trinket || item.getSlot() == ItemSlot.Finger || item.getSlot() == ItemSlot.OneHand)) {
            Item item251 = null;
            Item item258 = null;
            Item item264 = null;
            Item item271 = null;
            Item item277 = null;
            Item item284 = null;

            synchronized (ITEMS) {
                List<Item> list = UNIQUE_ITEM_BY_NAME.get(item.getName());
                if (list == null) {
                    list = new ArrayList<Item>();
                    UNIQUE_ITEM_BY_NAME.put(item.getName(), list);
                }
                list.add(item);

                for (Item i : list) {
                    Item validItem = ITEMS.get(i.getId());
                    if (i == validItem) {
                        if (i.getItemLevel() == 251) {
                            item251 = i;
                        } else if (i.getItemLevel() == 258) {
                            item258 = i;
                        } else if (i.getItemLevel() == 264) {
                            item264 = i;
                        } else if (i.getItemLevel() == 271) {
                            item271 = i;
                        } else if (i.getItemLevel() == 277) {
                            item277 = i;
                        } else if (i.getItemLevel() == 284) {
                            item284 = i;
                        }
                    }
                }
            }

            // normal/heroic pair 10 man ICC with same name
            if (item251 != null && item264 != null) {
                setUniquePair(item251, item264);
            }

            // normal/heroic pair 25 man ICC with same name
            if (item264 != null && item277 != null) {
                setUniquePair(item264, item277);
            }

            // normal/heroic pair 10 man RS with same name
            if (item258 != null && item271 != null) {
                setUniquePair(item258, item271);
            }

            // normal/heroic pair 25 man RS with same name
            if (item271 != null && item284 != null) {
                setUniquePair(item271, item284);
            }

            // special rules for Ashen Verdict Rings
            // Ashen Band of Courage
            if (item.getId() == 50375 || item.getId() == 50404 || item.getId() == 50388 || item.getId() == 50403) {
                setUniqueGroup(item, 50375, 50404, 50388, 50403);
            }
            // Ashen Band of Destruction
            else if (item.getId() == 50377 || item.getId() == 50398 || item.getId() == 50384 || item.getId() == 50397) {
                setUniqueGroup(item, 50377, 50398, 50384, 50397);
            }
            // Ashen Band of Might
            else if (item.getId() == 52572 || item.getId() == 52570 || item.getId() == 52569 || item.getId() == 52571) {
                setUniqueGroup(item, 52572, 52570, 52569, 52571);
            }
            // Ashen Band of Vengeance
            else if (item.getId() == 50402 || item.getId() == 50387 || item.getId() == 50401 || item.getId() == 50376) {
                setUniqueGroup(item, 50402, 50387, 50401, 50376);
            }
            // Ashen Band of Wisdom
            else if (item.getId() == 50400 || item.getId() == 50386 || item.getId() == 50399 || item.getId() == 50378) {
                setUniqueGroup(item, 50400, 50386, 50399, 50378);
            }
        }
    }

    private static void setUniquePair(Item first, Item second) {
        first.setUniqueId(new ArrayList<Integer>(Arrays.asList(first.getId(), second.getId())));
        second.setUniqueId(new ArrayList<Integer>(Arrays.asList(first.getId(), second.getId())));
    }

    private static void setUniqueGroup(Item item, int... ids) {
        List<Integer> uniqueIds = new ArrayList<Integer>();
        for (int id : ids) {
            uniqueIds.add(id);
        }
        item.setUniqueId(uniqueIds);
    }
}
