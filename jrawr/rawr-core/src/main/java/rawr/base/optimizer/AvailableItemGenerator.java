// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/AvailableItemGenerator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Generates candidate items for the optimizer from the item cache, availability strings, gemming
// templates and sockets. The C# file also declares the public helper types ItemAvailabilityInformation
// and DirectUpgradeEntry; they live in their own Java files in this package so the items and batch
// packages can reference them.
// The static FilterList overloads are renamed (filterBuffs/filterEnchants/filterItems/
// filterItemInstances) because Java type erasure does not allow List<T> overloads to coexist.
package rawr.base.optimizer;

import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;
import rawr.base.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides means to generate available items based on available item strings from character.
 */
/**
 * Builds the candidate item pool the optimizers search: for every item a character may use, the
 * gemmed and enchanted {@link ItemInstance}s that are legal under the user's availability
 * settings, grouped by the equipment slot they fit.
 *
 * <p>Availability is expressed as strings in the character file, one per entry of the character's
 * available-item list:</p>
 * <ul>
 *   <li>{@code itemId.gem1.gem2.gem3.enchantId} selects one exact gemmed/enchanted instance;</li>
 *   <li>a {@code *} in any position means "any"; the generator expands wildcard positions over the
 *       allowed gems and enchants;</li>
 *   <li>a bare {@code itemId} means the item with all gemmings;</li>
 *   <li>a negative id encodes an enchant availability entry
 *       ({@code -1 * (enchantId + 10000 * slotOrdinal)}).</li>
 * </ul>
 *
 * <p>The constructor first merges in the gem ids of the character's enabled gemming templates
 * (when {@code templateGemsEnabled}) and then runs {@code populateAvailableIds}: it clears and
 * rebuilds the {@link Item} availability structures in the global {@link ItemCache}, collects the
 * available gems, meta gems and per-slot enchants, expands every item id into its legal gemmed
 * instances, filters dominated instances, adds null (no item) candidates for the paired and
 * optional slots and remembers the currently equipped instance as the item's default. Wildcard
 * gemmings additionally produce the "direct upgrade" trees used by the greedy search modes.</p>
 *
 * <h2>Constructor parameters</h2>
 *
 * <ul>
 *   <li>{@code availableItems} &mdash; the availability strings above (the caller's list is copied
 *       before template gems are merged in).</li>
 *   <li>{@code generateDirectUpgrades} &mdash; also build the per-item gem/enchant trees that
 *       {@link ItemInstanceOptimizer} uses in its SingleChanges and GreedyBest scans.</li>
 *   <li>{@code overrideRegem} &mdash; allow all gemmings of every selected item, not just the
 *       selected ones.</li>
 *   <li>{@code overrideReenchant} &mdash; allow all legal enchants of every selected item.</li>
 *   <li>{@code slotFiltering} &mdash; drop dominated gemmed instances per slot (without it, only
 *       the global per-item reduction runs).</li>
 *   <li>{@code positiveCostItemsAvailable} &mdash; add items that have a purchase cost even when
 *       the user did not select them, unless a required-class restriction excludes the
 *       character.</li>
 *   <li>{@code character}/{@code models} or the plural arrays &mdash; the characters the pool is
 *       built for (batch construction unions their slots) and the models that define the relevant
 *       item types and available enchant lists.</li>
 * </ul>
 *
 * <h2>Other services</h2>
 *
 * <ul>
 *   <li>{@code normalizeCharacter} rewrites a character's gems and enchants into a stat-equivalent
 *       instance as close as possible to each item's default gemming, preserving the matched
 *       socket bonuses (with a feasibility check on the remaining sockets).</li>
 *   <li>{@code isCharacterValid} checks that every equipped gemmed id is available and builds a
 *       warning message naming what the user must change; {@code regularizeCharacter} replaces
 *       unavailable items with the first available instance.</li>
 *   <li>{@link #saveAvailabilityInformation} and {@link #restoreAvailabilityInformation} snapshot
 *       and restore the shared availability data around restriction passes, and
 *       {@code addItemRestriction} locks one item to exactly one gemmed instance.</li>
 *   <li>The static {@code filter...} methods remove dominated candidates using
 *       {@link StatsColors}, a partial-order comparison that also accounts for socket colors, set
 *       membership, unique and jewelcrafter gem status and positive-cost flags.</li>
 * </ul>
 */
public class AvailableItemGenerator {
    private Map<String, Boolean> itemAvailable = new LinkedHashMap<String, Boolean>();
    private List<String> availableItems;
    private boolean overrideRegem;
    private boolean overrideReenchant;
    private boolean slotFiltering;
    private boolean generateDirectUpgrades;
    private Character[] characters;
    private CalculationsBase[] models;

    private static final int slotCount = 19;
    private List<ItemInstance>[] slotItems = new List[slotCount];
    private Item[] metaGemItems;
    private Item[] gemItems;
    private Enchant[][] slotAvailableEnchants = new Enchant[slotCount][];
    private List<List<DirectUpgradeEntry>>[] slotDirectUpgrades = new List[slotCount];
    private List<Item>[] slotRawItems = new List[slotCount];

    private static <T> boolean arrayContains(T[] array, Predicate<T> predicate) {
        for (T element : array) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    public List<List<DirectUpgradeEntry>>[] getSlotDirectUpgrades() {
        return slotDirectUpgrades;
    }

    public Enchant[][] getSlotEnchants() {
        return slotAvailableEnchants;
    }

    public Map<String, Boolean> getItemAvailable() {
        return itemAvailable;
    }

    public List<ItemInstance>[] getSlotItems() {
        return slotItems;
    }

    public List<Item>[] getSlotRawItems() {
        return slotRawItems;
    }

    public Item[] getMetaGemItems() {
        return metaGemItems;
    }

    public Item[] getGemItems() {
        return gemItems;
    }

    /**
     * Replaces gems/enchants on the character to minimize the number of changes from root items while preserving total stats.
     */
    private static Map<Item, Integer> countGems(ItemInstance itemInstance) {
        Map<Item, Integer> gemCount = new LinkedHashMap<Item, Integer>();
        for (int gem = 1; gem <= 3; gem++) {
            Item g = itemInstance.getGem(gem);
            if (g != null) {
                Integer count = gemCount.get(g);
                gemCount.put(g, (count == null ? 0 : count) + 1);
            }
        }
        return gemCount;
    }

    private static int gemDifference(ItemInstance defaultItem, Map<Item, Integer> gemCount) {
        if (defaultItem == null) {
            return 0; // nothing to base from, we can make any gemming we want
        }
        int diff = 0;
        for (int gem = 1; gem <= 3; gem++) {
            Item g = defaultItem.getGem(gem);
            if (g != null) {
                Integer count = gemCount.get(g);
                if (count != null && count > 0) {
                    gemCount.put(g, count - 1);
                } else {
                    diff++;
                }
            }
        }
        return diff;
    }

    public void normalizeCharacter(Character character, boolean invalidate) {
        // main assumption is that gems and enchants have fixed stat allocation
        // meaning that the matched socket bonuses must be maintained in order to preserve total stats

        // we're only looking at gem shuffling
        // it's possible that gem splits happen like 20+20 => 10/10+10/10
        // we don't handle those currently, to fully support things like that we'd need to add
        // logic that makes sure overall uniqeness is maintained and meta gem requirements don't break

        // we'll be doing a bunch of Stats comparisons, this is a potentially expensive operation

        // first check which slots are nondefault
        // and get a list of socket bonuses that are matched

        Stats zeroStats = new Stats();
        List<Stats> matchedSocketBonuses = new ArrayList<Stats>();
        List<Integer> matchedSocketBonusCount = new ArrayList<Integer>();
        int matchedBonusTotal = 0;
        Map<Item, Integer> gems = new LinkedHashMap<Item, Integer>();

        boolean[] slotDefault = new boolean[Character.OptimizableSlotCount];
        boolean[] slotFilled = new boolean[Character.OptimizableSlotCount];
        int[] socketBonusIndex = new int[Character.OptimizableSlotCount];
        boolean[] hasSocketBonus = new boolean[Character.OptimizableSlotCount];

        ItemInstance[] items = new ItemInstance[Character.OptimizableSlotCount];

        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            ItemInstance item = character.getItem(slot);
            if (item != null && item.getItem() != null && item.getItem().getAvailabilityInformation() != null) {
                slotDefault[slot] = (item == item.getItem().getAvailabilityInformation().defaultItemInstance);
                if (!slotDefault[slot]) {
                    for (int gem = 1; gem <= 3; gem++) {
                        Item g = item.getGem(gem);
                        if (g != null) {
                            Integer count = gems.get(g);
                            gems.put(g, (count == null ? 0 : count) + 1);
                        }
                    }
                    if (Stats.greater(item.getItem().getSocketBonus(), zeroStats)) {
                        hasSocketBonus[slot] = true;
                        if (item.matchesSocketBonus()) {
                            matchedBonusTotal++;
                            // check if we have it in list already
                            boolean found = false;
                            for (int i = 0; i < matchedSocketBonuses.size(); i++) {
                                if (Stats.equalsStats(matchedSocketBonuses.get(i), item.getItem().getSocketBonus())) {
                                    matchedSocketBonusCount.set(i, matchedSocketBonusCount.get(i) + 1);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                matchedSocketBonuses.add(item.getItem().getSocketBonus());
                                matchedSocketBonusCount.add(1);
                            }
                        }
                    }
                } else {
                    items[slot] = item;
                    slotFilled[slot] = true;
                }
            } else {
                slotDefault[slot] = true;
                slotFilled[slot] = true;
                items[slot] = item;
            }
        }

        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            if (!slotFilled[slot]) {
                if (hasSocketBonus[slot]) {
                    ItemInstance item = character.getItem(slot);
                    for (int i = 0; i < matchedSocketBonuses.size(); i++) {
                        if (Stats.equalsStats(matchedSocketBonuses.get(i), item.getItem().getSocketBonus())) {
                            socketBonusIndex[slot] = i;
                            break;
                        }
                    }
                }
            }
        }

        // from now on we only have to care about the slots that are nondefault (note we know all nondefault slots are not null)

        // now what we'll do is first make sure we meet all needed socket bonuses in some way

        // just going greedy about it doesn't always work
        // it can happen that by going for minimizing differences you run out of options
        // to fulfill socket bonuses, so it's necessary to keep track of colors you have left
        // to make sure you don't lock yourself out

        for (int bonusCounter = 0; bonusCounter < matchedBonusTotal; bonusCounter++) {
            // find an item with this socket bonus, with an item instance that has gems that are still available
            boolean found = false;
            int bestSlot = -1;
            int bestBonusIndex = -1;
            int differentGems = 4;
            ItemInstance bestItem = null;
            FILLSLOT: {
                for (int bonusIndex = 0; bonusIndex < matchedSocketBonuses.size(); bonusIndex++) {
                    if (matchedSocketBonusCount.get(bonusIndex) > 0) {
                        Stats socketBonus = matchedSocketBonuses.get(bonusIndex);
                        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
                            if (!slotFilled[slot]) {
                                ItemInstance item = character.getItem(slot);
                                ItemInstance defaultItem = item.getItem().getAvailabilityInformation().defaultItemInstance;
                                if (Stats.equalsStats(item.getItem().getSocketBonus(), socketBonus)) {
                                    // check all available items
                                    // only select those that match socket bonus and enchant and compare how much it differs from the default
                                    for (ItemInstance itemInstance : item.getItem().getAvailabilityInformation().itemList) {
                                        if (itemInstance.matchesSocketBonus() && itemInstance.getEnchant() == item.getEnchant()) {
                                            Map<Item, Integer> gemCount = countGems(itemInstance);
                                            // make sure the needed counts are still available
                                            boolean available = true;
                                            for (Map.Entry<Item, Integer> kvp : gemCount.entrySet()) {
                                                Integer count = gems.get(kvp.getKey());
                                                if (count == null || count < kvp.getValue()) {
                                                    available = false;
                                                    break;
                                                }
                                            }
                                            if (available && remainingSocketBonusesCanBeMatched(character, slotFilled, slot, matchedSocketBonusCount, hasSocketBonus, socketBonusIndex, bonusIndex, gemCount, gems)) // we could move this check further down, but make sure to make a copy of gemCount
                                            {
                                                // we found an item instance with matched enchant that matches socket bonus and has all needed gems at disposal
                                                found = true;
                                                // how much does it differ from default?
                                                int diff = gemDifference(defaultItem, gemCount);
                                                if (diff < differentGems) {
                                                    bestSlot = slot;
                                                    bestBonusIndex = bonusIndex;
                                                    bestItem = itemInstance;
                                                    differentGems = diff;
                                                    if (diff == 0) {
                                                        break FILLSLOT;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (found) {
                items[bestSlot] = bestItem;
                slotFilled[bestSlot] = true;
                matchedSocketBonusCount.set(bestBonusIndex, matchedSocketBonusCount.get(bestBonusIndex) - 1);
                // remove used up gems
                for (int gem = 1; gem <= 3; gem++) {
                    Item g = bestItem.getGem(gem);
                    if (g != null) {
                        Integer count = gems.get(g);
                        gems.put(g, (count == null ? 0 : count) - 1);
                    }
                }
            } else {
                // we were not able to find any available item that would match the bonus and retain enchant
                // which means we failed to find stat equivalent solution
                // we have to return the input as we can't find anything better
                return;
            }
        }

        // we matched all socket bonuses
        // now the easy stuff, just throw in the gems wherever you fill like it
        // just need to be careful about only using available items

        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            if (!slotFilled[slot]) {
                ItemInstance item = character.getItem(slot);
                ItemInstance defaultItem = item.getItem().getAvailabilityInformation().defaultItemInstance;
                boolean found = false;
                int differentGems = 4;
                ItemInstance bestItem = null;
                // check all available items
                // only select those that match enchant and compare how much it differs from the default
                for (ItemInstance itemInstance : item.getItem().getAvailabilityInformation().itemList) {
                    if (itemInstance.getEnchant() == item.getEnchant()) {
                        Map<Item, Integer> gemCount = countGems(itemInstance);
                        // make sure the needed counts are still available
                        boolean available = true;
                        for (Map.Entry<Item, Integer> kvp : gemCount.entrySet()) {
                            Integer count = gems.get(kvp.getKey());
                            if (count == null || count < kvp.getValue()) {
                                available = false;
                                break;
                            }
                        }
                        if (available) {
                            // we found an item instance with matched enchant that has all needed gems at disposal
                            found = true;
                            // how much does it differ from default?
                            int diff = gemDifference(defaultItem, gemCount);
                            if (diff < differentGems) {
                                bestItem = itemInstance;
                                differentGems = diff;
                                if (diff == 0) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (found) {
                    items[slot] = bestItem;
                    slotFilled[slot] = true;
                    // remove used up gems and slots
                    for (int gem = 1; gem <= 3; gem++) {
                        Item g = bestItem.getGem(gem);
                        if (g != null) {
                            Integer count = gems.get(g);
                            gems.put(g, (count == null ? 0 : count) - 1);
                        }
                    }
                } else {
                    // probably a problem with availability
                    // we don't have the right gems anymore so we can't construct a character that maintains total stats
                    return;
                }
            }
        }

        // we were able to fill all slots, that is greedy min diff character
        character.setItems(items, invalidate);
    }

    private boolean remainingSocketBonusesCanBeMatched(Character character, boolean[] slotFilled, int slot, List<Integer> matchedSocketBonusCount, boolean[] hasSocketBonus, int[] socketBonusIndex, int bonusIndex, Map<Item, Integer> gemCount, Map<Item, Integer> gems) {
        // we need this wrapper because we can have several instances of same socket bonus, but we only need some of them matched, and they need not all have same sockets
        // we have to do an exhaustive enumeration of which items are actually used to fill sockets
        // only if all are bad the whole thing is bad

        boolean[] hasMatchingSocketBonus = new boolean[Character.OptimizableSlotCount];
        int[] bonusCount = new int[matchedSocketBonusCount.size()];
        int[] bonusCountActual = new int[matchedSocketBonusCount.size()];
        int matchCount = 0;
        for (int i = 0; i < matchedSocketBonusCount.size(); i++) {
            bonusCount[i] = matchedSocketBonusCount.get(i);
            matchCount += bonusCount[i];
        }
        bonusCount[bonusIndex]--;
        matchCount--;

        List<Integer> matchingIndex = new ArrayList<Integer>();
        for (int s = 0; s < Character.OptimizableSlotCount; s++) {
            if (!slotFilled[s] && s != slot) {
                if (hasSocketBonus[s]) {
                    int sbindex = socketBonusIndex[s];
                    if (bonusCount[sbindex] > 0) {
                        matchingIndex.add(s);
                        hasMatchingSocketBonus[s] = true;
                    }
                }
            }
        }

        // we could do decomposition on type of actual bonus and do a cross product on components
        // but we'll just do a simpler thing and do combinations of matchCount out of matchingIndex.Count

        boolean[] selected = new boolean[matchingIndex.size()];
        for (int i = matchingIndex.size() - matchCount; i < matchingIndex.size(); i++) {
            selected[i] = true;
        }

        do {
            // check if this combination is a valid selection of bonuses
            Arrays.fill(bonusCountActual, 0);
            int R = 0, Y = 0, B = 0;
            for (int k = 0; k < selected.length; k++) {
                if (selected[k]) {
                    bonusCountActual[socketBonusIndex[matchingIndex.get(k)]]++;
                    ItemInstance item = character.getItem(matchingIndex.get(k));
                    for (int gem = 1; gem <= 3; gem++) {
                        switch (item.getItem().getSocketColor(gem)) {
                            case Red:
                                R++;
                                break;
                            case Blue:
                                B++;
                                break;
                            case Yellow:
                                Y++;
                                break;
                        }
                    }
                }
            }
            boolean valid = true;
            for (int k = 0; k < matchedSocketBonusCount.size(); k++) {
                if (bonusCountActual[k] != bonusCount[k]) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                if (remainingSocketsCanBeMatched(gems, gemCount, R, B, Y)) {
                    return true;
                }
            }

            // move to next combination
            int i = selected.length - 1;
            if (i == 0) {
                // we enumerated all combinations, time to fail
                return false;
            }

            while (selected[i - 1] || !selected[i]) {
                i = i - 1;
                if (i == 0) {
                    // we enumerated all combinations, time to fail
                    return false;
                }
            }

            int j = selected.length;

            while (!selected[j - 1] || selected[i - 1]) j = j - 1;

            // swap values at positions (i-1) and (j-1)
            boolean tmp = selected[i - 1];
            selected[i - 1] = selected[j - 1];
            selected[j - 1] = tmp;

            i++;
            j = selected.length;

            while (i < j) {
                // swap values at positions (i-1) and (j-1)
                tmp = selected[i - 1];
                selected[i - 1] = selected[j - 1];
                selected[j - 1] = tmp;
                i++;
                j--;
            }
        } while (true);
    }

    /**
     * Checks if we can still match the socket colors.
     */
    private boolean remainingSocketsCanBeMatched(Map<Item, Integer> gemsAll, Map<Item, Integer> gemCount, int R, int B, int Y) {
        // we'll do this by first matching pure colors, then bicolor and prismatic

        // init gems
        Map<Item, Integer> gems = new LinkedHashMap<Item, Integer>(gemsAll);
        for (Map.Entry<Item, Integer> kvp : gemCount.entrySet()) {
            Integer v = gems.get(kvp.getKey());
            gems.put(kvp.getKey(), (v == null ? 0 : v) - kvp.getValue());
        }

        // pure pass
        int N = 0, O = 0, P = 0, G = 0;
        for (Map.Entry<Item, Integer> kvp : gems.entrySet()) {
            ItemSlot color = kvp.getKey().getSlot();
            switch (color) {
                case Red:
                    R = Math.max(0, R - kvp.getValue());
                    break;
                case Yellow:
                    Y = Math.max(0, Y - kvp.getValue());
                    break;
                case Blue:
                    B = Math.max(0, B - kvp.getValue());
                    break;
                case Green:
                    G += kvp.getValue();
                    break;
                case Purple:
                    P += kvp.getValue();
                    break;
                case Orange:
                    O += kvp.getValue();
                    break;
                case Prismatic:
                    N += kvp.getValue();
                    break;
            }
        }

        // let's do some math
        // we have socket color values to fill and bicolor gem counts
        // each bicolor if used will be in one of the two colors, split the variable in two based on which one
        // vars: SocketRed, SocketBlue, SocketYellow
        //       GemPurple, GemGreen, GemOrange, GemPrismatic
        //       GemPurpleRed, GemPurpleBlue, GemGreenYellow, GemGreenBlue, GemOrangeYellow, GemOrangeRed
        //       GemPrismaticRed, GemPrismaticYellow, GemPrismaticBlue
        // GemPurple >= GemPurpleRed + GemPurpleBlue
        // GemGreen >= GemGreenYellow + GemGreenBlue
        // GemOrange >= GemOrangeYellow + GemOrangeRed
        // GemPrismatic >= GemPrismaticRed + GemPrismaticYellow + GemPrismaticBlue
        // if we can match the sockets then the following equations must hold
        // GemPurpleRed + GemOrangeRed + GemPrismaticRed >= SocketRed
        // GemPurpleBlue + GemGreenBlue + GemPrismaticBlue >= SocketBlue
        // GemGreenYellow + GemOrangeYellow + GemPrismaticYellow >= SocketYellow

        // this code is prone to copy/paste error
        // it tries to see if there is a solution in a way reminiscing phase I LP
        // if someone has a more elegant solution please replace
        int PR = P, PB = 0, GY = G, GB = 0, OY = O, OR = 0, NR = N, NY = 0, NB = 0;
        do {
            int Rdiff = R - PR - OR - NR;
            int Bdiff = B - PB - GB - NB;
            int Ydiff = Y - GY - OY - NY;
            if (Rdiff <= 0 && Bdiff <= 0 && Ydiff <= 0) {
                // we can match all constraints
                return true;
            }
            if (Rdiff > 0 && Bdiff < 0) {
                // move from blue to red
                if (NB > 0) {
                    int move = Math.min(NB, Rdiff);
                    NB -= move;
                    NR += move;
                    continue;
                }
                if (PB > 0) {
                    int move = Math.min(PB, Rdiff);
                    PB -= move;
                    PR += move;
                    continue;
                }
                // via yellow
                if (GB > 0 && OY > 0) {
                    int move = Math.min(Math.min(GB, Rdiff), OY);
                    GB -= move;
                    GY += move;
                    OY -= move;
                    OR += move;
                    continue;
                }
                if (GB > 0 && NY > 0) {
                    int move = Math.min(Math.min(GB, Rdiff), NY);
                    GB -= move;
                    GY += move;
                    NY -= move;
                    NR += move;
                    continue;
                }
            }
            if (Rdiff > 0 && Ydiff < 0) {
                // move from yellow to red
                if (NY > 0) {
                    int move = Math.min(NY, Rdiff);
                    NY -= move;
                    NR += move;
                    continue;
                }
                if (OY > 0) {
                    int move = Math.min(OY, Rdiff);
                    OY -= move;
                    OR += move;
                    continue;
                }
                // via blue
                if (GY > 0 && PB > 0) {
                    int move = Math.min(Math.min(GY, Rdiff), PB);
                    GY -= move;
                    GB += move;
                    PB -= move;
                    PR += move;
                    continue;
                }
                if (GY > 0 && NB > 0) {
                    int move = Math.min(Math.min(GY, Rdiff), NB);
                    GY -= move;
                    GB += move;
                    NB -= move;
                    NR += move;
                    continue;
                }
            }
            if (Bdiff > 0 && Rdiff < 0) {
                // move from red to blue
                if (NR > 0) {
                    int move = Math.min(NR, Bdiff);
                    NR -= move;
                    NB += move;
                    continue;
                }
                if (PR > 0) {
                    int move = Math.min(PR, Bdiff);
                    PR -= move;
                    PB += move;
                    continue;
                }
                // via yellow
                if (OR > 0 && GY > 0) {
                    int move = Math.min(Math.min(OR, Bdiff), GY);
                    OR -= move;
                    OY += move;
                    GY -= move;
                    GB += move;
                    continue;
                }
                if (OR > 0 && NY > 0) {
                    int move = Math.min(Math.min(OR, Bdiff), NY);
                    OR -= move;
                    OY += move;
                    NY -= move;
                    NB += move;
                    continue;
                }
            }
            if (Bdiff > 0 && Ydiff < 0) {
                // move from yellow to blue
                if (NY > 0) {
                    int move = Math.min(NY, Bdiff);
                    NY -= move;
                    NB += move;
                    continue;
                }
                if (GY > 0) {
                    int move = Math.min(GY, Bdiff);
                    GY -= move;
                    GB += move;
                    continue;
                }
                // via red
                if (OY > 0 && PR > 0) {
                    int move = Math.min(Math.min(OY, Bdiff), PR);
                    OY -= move;
                    OR += move;
                    PR -= move;
                    PB += move;
                    continue;
                }
                if (OY > 0 && NR > 0) {
                    int move = Math.min(Math.min(OY, Bdiff), NR);
                    OY -= move;
                    OR += move;
                    NR -= move;
                    NB += move;
                    continue;
                }
            }
            if (Ydiff > 0 && Bdiff < 0) {
                // move from blue to yellow
                if (NB > 0) {
                    int move = Math.min(NB, Ydiff);
                    NB -= move;
                    NY += move;
                    continue;
                }
                if (GB > 0) {
                    int move = Math.min(GB, Ydiff);
                    GB -= move;
                    GY += move;
                    continue;
                }
                // via red
                if (PB > 0 && OR > 0) {
                    int move = Math.min(Math.min(PB, Ydiff), OR);
                    PB -= move;
                    PR += move;
                    OR -= move;
                    OY += move;
                    continue;
                }
                if (PB > 0 && NR > 0) {
                    int move = Math.min(Math.min(PB, Ydiff), NR);
                    PB -= move;
                    PR += move;
                    NR -= move;
                    NY += move;
                    continue;
                }
            }
            if (Ydiff > 0 && Rdiff < 0) {
                // move from red to yellow
                if (NR > 0) {
                    int move = Math.min(NR, Ydiff);
                    NR -= move;
                    NY += move;
                    continue;
                }
                if (OR > 0) {
                    int move = Math.min(OR, Ydiff);
                    OR -= move;
                    OY += move;
                    continue;
                }
                // via blue
                if (PR > 0 && GB > 0) {
                    int move = Math.min(Math.min(PR, Ydiff), GB);
                    PR -= move;
                    PB += move;
                    GB -= move;
                    GY += move;
                    continue;
                }
                if (PR > 0 && NB > 0) {
                    int move = Math.min(Math.min(PR, Ydiff), NB);
                    PR -= move;
                    PB += move;
                    NB -= move;
                    NY += move;
                    continue;
                }
            }
            // if we found no valid move this means the system is not feasible
            return false;
        } while (true);
    }

    /**
     * Checks whether the equipped items on the character are marked as available.
     */
    public boolean isCharacterValid(Character character, String[] warning, boolean explain) {
        StringBuilder s = new StringBuilder();
        s.append("The following currently equipped items are not available").append(System.lineSeparator());
        s.append(System.lineSeparator());
        String line;
        List<String> warnings = new ArrayList<String>();
        boolean valid = true;
        // if item is not available pick the one that is available
        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            ItemInstance item = character.getItem(slot);
            if (item != null && item.getItem() != null) {
                if (item.getItem().getAvailabilityInformation() != null) {
                    if (!itemAvailable.containsKey(item.getGemmedId())) {
                        // gemming/enchant is not available
                        if (explain) {
                            // try to determine what they have to do to make it available
                            switch (character.getItemAvailability(item)) {
                                case Available:
                                    // shouldn't happen
                                    break;
                                case AvailableWithEnchantRestrictions:
                                    // they have marked this specific gemmming available, but it doesn't allow this enchant
                                    // warn about the enchant
                                    if (item.getEnchantId() != 0) {
                                        line = item.getEnchant().name + " is not available on " + item.getItem().getName();
                                        if (!warnings.contains(line)) {
                                            warnings.add(line);
                                            s.append(line).append(System.lineSeparator());
                                        }
                                        valid = false;
                                    }
                                    break;
                                case RegemmingAllowed:
                                    // all gemmings/enchants are available so the ones that are available must not be sufficient
                                    for (int gem = 1; gem <= 3; gem++) {
                                        Item g = item.getGem(gem);
                                        if (g != null) {
                                            // ignore if we have something strictly better marked
                                            if ((g.getSlot() == ItemSlot.Meta && !arrayContains(metaGemItems, gg -> Stats.greaterOrEqual(gg.getStats(), g.getStats()))) ||
                                                    (g.getSlot() != ItemSlot.Meta && !arrayContains(gemItems, gg -> gg.getId() == g.getId() || (Stats.greaterOrEqual(gg.getStats(), g.getStats()) && !gg.isLimitedGem())))) {
                                                // gem is not available
                                                line = g.getName() + " is not available";
                                                if (!warnings.contains(line)) {
                                                    warnings.add(line);
                                                    s.append(line).append(System.lineSeparator());
                                                }
                                                valid = false;
                                            }
                                        }
                                    }
                                    Enchant enchant = item.getEnchant();
                                    if (enchant != null && enchant.id != 0) {
                                        // ignore if we have something strictly better marked
                                        if (!arrayContains(slotAvailableEnchants[slot], e -> e.id == enchant.id || Stats.greaterOrEqual(e.stats, enchant.stats))) {
                                            // enchant is not available
                                            line = item.getEnchant().name + " is not available";
                                            if (!warnings.contains(line)) {
                                                warnings.add(line);
                                                s.append(line).append(System.lineSeparator());
                                            }
                                            valid = false;
                                        }
                                    }
                                    break;
                                case RegemmingAllowedWithEnchantRestrictions:
                                    // all gemmings are available so the ones that are available must not be sufficient
                                    for (int gem = 1; gem <= 3; gem++) {
                                        Item g = item.getGem(gem);
                                        if (g != null) {
                                            // ignore if we have something strictly better marked
                                            if ((g.getSlot() == ItemSlot.Meta && !arrayContains(metaGemItems, gg -> Stats.greaterOrEqual(gg.getStats(), g.getStats()))) ||
                                                    (g.getSlot() != ItemSlot.Meta && !arrayContains(gemItems, gg -> gg.getId() == g.getId() || (Stats.greaterOrEqual(gg.getStats(), g.getStats()) && !gg.isLimitedGem())))) {
                                                // gem is not available
                                                line = g.getName() + " is not available";
                                                if (!warnings.contains(line)) {
                                                    warnings.add(line);
                                                    s.append(line).append(System.lineSeparator());
                                                }
                                                valid = false;
                                            }
                                        }
                                    }
                                    if (item.getEnchantId() != 0 && !character.getAvailableItems().contains(item.getId() + ".*.*.*." + item.getEnchantId())) {
                                        // this specific enchant is not valid
                                        line = item.getEnchant().name + " is not available on " + item.getItem().getName();
                                        if (!warnings.contains(line)) {
                                            warnings.add(line);
                                            s.append(line).append(System.lineSeparator());
                                        }
                                        valid = false;
                                    }
                                    break;
                                case NotAvailable:
                                    // they could have some other gemming/enchant marked as available, but not in general
                                    line = item.getItem().getName() + " gemming/enchant is not available";
                                    if (!warnings.contains(line)) {
                                        warnings.add(line);
                                        s.append(line).append(System.lineSeparator());
                                    }
                                    valid = false;
                                    break;
                            }
                        } else {
                            line = item.getItem().getName() + " gemming/enchant is not available";
                            if (!warnings.contains(line)) {
                                warnings.add(line);
                                s.append(line).append(System.lineSeparator());
                            }
                            valid = false;
                        }
                    }
                } else {
                    if (slot == CharacterSlot.Projectile.value()) {
                        if (!character.getCurrentCalculations().canUseAmmo()) {
                            continue;
                        }
                    }
                    // item itself is not available
                    line = item.getItem().getName() + " is not available";
                    if (!warnings.contains(line)) {
                        warnings.add(line);
                        s.append(line).append(System.lineSeparator());
                    }
                    valid = false;
                }
            }
        }
        if (!valid) {
            s.append(System.lineSeparator());
            s.append("Do you want to continue with the optimization?").append(System.lineSeparator());
            warning[0] = s.toString();
        } else {
            warning[0] = null;
        }
        return valid;
    }

    /** Applies {@link #addItemRestriction} to every slot, optionally including the off-hand. */
    public void addItemRestrictions(ItemInstance[] items, boolean includeOffHand) {
        for (int slot = 0; slot < items.length; slot++) {
            if (slot != CharacterSlot.OffHand.value() || includeOffHand) {
                addItemRestriction(items[slot]);
            }
        }
    }

    /**
     * Applies {@link #addItemRestriction} to every equipped slot, skipping the off-hand when the
     * character's current model does not include it in calculations.
     */
    public void addItemRestrictions(Character character) {
        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            if (slot != CharacterSlot.OffHand.value() || character.getCurrentCalculations().includeOffHandInCalculations(character)) {
                addItemRestriction(character.getItem(slot));
            }
        }
    }

    /**
     * Locks an item to exactly one gemmed instance: every other instance of the item is removed
     * from the per-slot and direct-upgrade lists, the item's own availability list is reduced to
     * this instance and all other gemmed ids are dropped from the global availability map. Used to
     * pin a gemming when unique-equip or enchant restrictions require it.
     */
    public void addItemRestriction(ItemInstance item) {
        if (item == null || item.getItem() == null) return;
        ItemAvailabilityInformation iai = item.getItem().getAvailabilityInformation();
        if (iai == null) return;
        // make all other gemmings/enchantings of this item unavailable
        iai.itemList.clear();
        iai.itemList.add(item);
        DirectUpgradeEntry singleEntry = null;
        if (generateDirectUpgrades) {
            iai.matchingDirectUpgradeList.clear();
            iai.nonMatchingDirectUpgradeList.clear();
            iai.singleDirectUpgradeList.clear();
            singleEntry = new DirectUpgradeEntry();
            singleEntry.itemInstance = item;
            iai.singleDirectUpgradeList.add(singleEntry);
            iai.generativeEnchants.clear();
        }
        // we know that itemAvailable has to contain exactly the values from
        // item.AvailabilityInformation.ItemAvailable
        for (String key : new ArrayList<String>(iai.itemAvailable.keySet())) {
            if (!key.equals(item.getGemmedId())) {
                itemAvailable.remove(key);
            }
        }
        iai.itemAvailable.clear();
        iai.itemAvailable.put(item.getGemmedId(), true);
        for (int slot : iai.validSlots) {
            slotItems[slot].removeIf(i -> i != null && i.getId() == item.getId() && !i.getGemmedId().equals(item.getGemmedId()));
            if (generateDirectUpgrades) {
                slotDirectUpgrades[slot].get(0).removeIf(i -> i.itemInstance.getId() == item.getId());
                if (slotDirectUpgrades[slot].contains(iai.matchingDirectUpgradeList)) {
                    slotDirectUpgrades[slot].get(0).add(singleEntry);
                }
            }
        }
    }

    // saved availability data
    List<ItemInstance>[] savedSlotItems;
    List<DirectUpgradeEntry>[] savedSingleDirectUpgrades;
    Map<String, Boolean> savedItemAvailable;
    Map<Item, ItemAvailabilityInformation> savedAvailabilityInformation;

    /**
     * Snapshots the generated pool, the direct-upgrade lists and every item's availability
     * structure, so they can be restored after a temporary restriction pass (for example while a
     * batch member's gear is held fixed).
     */
    public void saveAvailabilityInformation() {
        savedSlotItems = new List[slotCount];
        if (generateDirectUpgrades) {
            savedSingleDirectUpgrades = new List[slotCount];
        }
        for (int slot = 0; slot < slotItems.length; slot++) {
            savedSlotItems[slot] = new ArrayList<ItemInstance>(slotItems[slot]);
            if (generateDirectUpgrades) {
                savedSingleDirectUpgrades[slot] = new ArrayList<DirectUpgradeEntry>(slotDirectUpgrades[slot].get(0));
            }
        }
        savedItemAvailable = new LinkedHashMap<String, Boolean>(itemAvailable);
        savedAvailabilityInformation = new LinkedHashMap<Item, ItemAvailabilityInformation>();
        synchronized (ItemCache.getItems()) {
            for (Item citem : ItemCache.getItems().values()) {
                if (citem.getAvailabilityInformation() != null) {
                    ItemAvailabilityInformation iai = new ItemAvailabilityInformation();
                    savedAvailabilityInformation.put(citem, iai);
                    iai.itemAvailable = new LinkedHashMap<String, Boolean>(citem.getAvailabilityInformation().itemAvailable);
                    iai.itemList = new ArrayList<ItemInstance>(citem.getAvailabilityInformation().itemList);
                    if (generateDirectUpgrades) {
                        iai.matchingDirectUpgradeList = new ArrayList<DirectUpgradeEntry>(citem.getAvailabilityInformation().matchingDirectUpgradeList);
                        iai.nonMatchingDirectUpgradeList = new ArrayList<DirectUpgradeEntry>(citem.getAvailabilityInformation().nonMatchingDirectUpgradeList);
                        iai.singleDirectUpgradeList = new ArrayList<DirectUpgradeEntry>(citem.getAvailabilityInformation().singleDirectUpgradeList);
                        iai.generativeEnchants = new ArrayList<Enchant>(citem.getAvailabilityInformation().generativeEnchants);
                    }
                }
            }
        }
    }

    /**
     * Restores availability information that was saved previously. If the generator was used
     * in an optimizer it is necessary to reinitialize the optimizer item cache
     */
    public void restoreAvailabilityInformation() {
        for (int slot = 0; slot < slotItems.length; slot++) {
            slotItems[slot].clear();
            slotItems[slot].addAll(savedSlotItems[slot]);
            if (generateDirectUpgrades) {
                slotDirectUpgrades[slot].get(0).clear();
                slotDirectUpgrades[slot].get(0).addAll(savedSingleDirectUpgrades[slot]);
            }
        }
        itemAvailable = new LinkedHashMap<String, Boolean>(savedItemAvailable);
        for (Map.Entry<Item, ItemAvailabilityInformation> kvp : savedAvailabilityInformation.entrySet()) {
            ItemAvailabilityInformation iai = kvp.getKey().getAvailabilityInformation();
            iai.itemAvailable = new LinkedHashMap<String, Boolean>(kvp.getValue().itemAvailable);
            iai.itemList.clear();
            iai.itemList.addAll(kvp.getValue().itemList);
            if (generateDirectUpgrades) {
                iai.matchingDirectUpgradeList.clear();
                iai.matchingDirectUpgradeList.addAll(kvp.getValue().matchingDirectUpgradeList);
                iai.nonMatchingDirectUpgradeList.clear();
                iai.nonMatchingDirectUpgradeList.addAll(kvp.getValue().nonMatchingDirectUpgradeList);
                iai.singleDirectUpgradeList.clear();
                iai.singleDirectUpgradeList.addAll(kvp.getValue().singleDirectUpgradeList);
                iai.generativeEnchants.clear();
                iai.generativeEnchants.addAll(kvp.getValue().generativeEnchants);
            }
        }
    }

    /**
     * Replaces unavailable items with available ones
     */
    public void regularizeCharacter(Character character) {
        // if item is not available pick the one that is available
        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            ItemInstance item = character.getItem(slot);
            if (item != null && item.getItem() != null && item.getItem().getAvailabilityInformation() != null) {
                if (!itemAvailable.containsKey(item.getGemmedId())) {
                    character.setItem(slot, item.getItem().getAvailabilityInformation().itemList.get(0));
                }
            }
        }
    }

    /** Single-character form of the full constructor, without positive-cost items. */
    public AvailableItemGenerator(List<String> availableItems, boolean generateDirectUpgrades, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant, boolean slotFiltering, Character character, CalculationsBase model) {
        this(availableItems, generateDirectUpgrades, templateGemsEnabled, overrideRegem, overrideReenchant, slotFiltering, false, new Character[] { character }, new CalculationsBase[] { model });
    }

    /** Batch form of the full constructor, without positive-cost items. */
    public AvailableItemGenerator(List<String> availableItems, boolean generateDirectUpgrades, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant, boolean slotFiltering, Character[] characters, CalculationsBase[] models) {
        this(availableItems, generateDirectUpgrades, templateGemsEnabled, overrideRegem, overrideReenchant, slotFiltering, false, characters, models);
    }

    /** Single-character form of the full constructor. */
    public AvailableItemGenerator(List<String> availableItems, boolean generateDirectUpgrades, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant, boolean slotFiltering, boolean positiveCostItemsAvailable, Character character, CalculationsBase model) {
        this(availableItems, generateDirectUpgrades, templateGemsEnabled, overrideRegem, overrideReenchant, slotFiltering, positiveCostItemsAvailable, new Character[] { character }, new CalculationsBase[] { model });
    }

    /**
     * Builds the pool; see the class documentation for the meaning of every parameter. The
     * character array allows one pool to serve a batch of characters (a slot is populated when any
     * of them can use an item). The constructor takes ownership of the volatile item statistics
     * while it runs and always rebuilds the global availability data.
     */
    public AvailableItemGenerator(List<String> availableItems, boolean generateDirectUpgrades, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant, boolean slotFiltering, boolean positiveCostItemsAvailable, Character[] characters, CalculationsBase[] models) {
        this.availableItems = availableItems;
        if (templateGemsEnabled) {
            this.availableItems = new ArrayList<String>(availableItems);
            List<String> templateGems = new ArrayList<String>();
            // this could actually be empty, but in practice they will populate it at least once before
            // however as a sanity check if it is null fetch the template from the model
            for (int index = 0; index < characters.length; index++) {
                for (GemmingTemplate template : characters[index].getCurrentGemmingTemplates()) {
                    if (template.getEnabled()) {
                        if (!templateGems.contains(Integer.toString(template.getRedId()))) templateGems.add(Integer.toString(template.getRedId()));
                        if (!templateGems.contains(Integer.toString(template.getYellowId()))) templateGems.add(Integer.toString(template.getYellowId()));
                        if (!templateGems.contains(Integer.toString(template.getBlueId()))) templateGems.add(Integer.toString(template.getBlueId()));
                        if (!templateGems.contains(Integer.toString(template.getPrismaticId()))) templateGems.add(Integer.toString(template.getPrismaticId()));
                        if (!templateGems.contains(Integer.toString(template.getMetaId()))) templateGems.add(Integer.toString(template.getMetaId()));
                    }
                }
                for (GemmingTemplate template : characters[index].getCustomGemmingTemplates()) {
                    if (template.getEnabled() && Objects.equals(template.getModel(), models[index].getName())) {
                        if (!templateGems.contains(Integer.toString(template.getRedId()))) templateGems.add(Integer.toString(template.getRedId()));
                        if (!templateGems.contains(Integer.toString(template.getYellowId()))) templateGems.add(Integer.toString(template.getYellowId()));
                        if (!templateGems.contains(Integer.toString(template.getBlueId()))) templateGems.add(Integer.toString(template.getBlueId()));
                        if (!templateGems.contains(Integer.toString(template.getPrismaticId()))) templateGems.add(Integer.toString(template.getPrismaticId()));
                        if (!templateGems.contains(Integer.toString(template.getMetaId()))) templateGems.add(Integer.toString(template.getMetaId()));
                    }
                }
            }
            for (String gem : templateGems) {
                if (!this.availableItems.contains(gem)) this.availableItems.add(gem);
            }
        }
        this.overrideRegem = overrideRegem;
        this.overrideReenchant = overrideReenchant;
        this.slotFiltering = slotFiltering;
        this.characters = characters;
        this.models = models;
        this.generateDirectUpgrades = generateDirectUpgrades;

        boolean oldVolatility = Item.isOptimizerManagedVolatiliy();
        try {
            Item.setOptimizerManagedVolatiliy(true);
            populateAvailableIds(positiveCostItemsAvailable);
        } finally {
            Item.setOptimizerManagedVolatiliy(oldVolatility);
        }
    }

    /**
     * Number of gems the item can hold: one per colored socket, plus one extra socket when the
     * character enabled a blacksmithing socket on a waist, hands or wrist item. Used to size gem
     * arrays and to build gemmed-id strings.
     */
    public int getItemGemCount(Item item) {
        int gemCount = 0;
        boolean blacksmithingSocket = (item.getSlot() == ItemSlot.Waist && characters[0].getWaistBlacksmithingSocketEnabled()) || (item.getSlot() == ItemSlot.Hands && characters[0].getHandsBlacksmithingSocketEnabled()) || (item.getSlot() == ItemSlot.Wrist && characters[0].getWristBlacksmithingSocketEnabled());
        switch (item.getSocketColor1()) {
            case Meta:
            case Red:
            case Orange:
            case Yellow:
            case Green:
            case Blue:
            case Purple:
            case Prismatic:
                gemCount++;
                break;
            default:
                if (blacksmithingSocket) {
                    gemCount++;
                    blacksmithingSocket = false;
                }
                break;
        }
        switch (item.getSocketColor2()) {
            case Meta:
            case Red:
            case Orange:
            case Yellow:
            case Green:
            case Blue:
            case Purple:
            case Prismatic:
                gemCount++;
                break;
            default:
                if (blacksmithingSocket) {
                    gemCount++;
                    blacksmithingSocket = false;
                }
                break;
        }
        switch (item.getSocketColor3()) {
            case Meta:
            case Red:
            case Orange:
            case Yellow:
            case Green:
            case Blue:
            case Purple:
            case Prismatic:
                gemCount++;
                break;
            default:
                if (blacksmithingSocket) {
                    gemCount++;
                    blacksmithingSocket = false;
                }
                break;
        }
        return gemCount;
    }

    /**
     * Creates the item's availability structure (a fresh {@code ItemAvailabilityInformation} plus
     * the empty upgrade collections when those are being generated). Called for every relevant item
     * of the pool and by batch tools for items that were not selected.
     */
    public void generateItemAvailabilityInformation(Item item) {
        item.setAvailabilityInformation(new ItemAvailabilityInformation());
        item.getAvailabilityInformation().gemCount = getItemGemCount(item);
        if (generateDirectUpgrades) {
            item.getAvailabilityInformation().matchingDirectUpgradeList = new ArrayList<DirectUpgradeEntry>();
            item.getAvailabilityInformation().matchingMap = new LinkedHashMap<String, DirectUpgradeEntry>();
            item.getAvailabilityInformation().nonMatchingDirectUpgradeList = new ArrayList<DirectUpgradeEntry>();
            item.getAvailabilityInformation().nonMatchingMap = new LinkedHashMap<String, DirectUpgradeEntry>();
            item.getAvailabilityInformation().singleDirectUpgradeList = new ArrayList<DirectUpgradeEntry>();
            item.getAvailabilityInformation().generativeEnchants = new ArrayList<Enchant>();
        }
    }

    private void populateAvailableIds(boolean positiveCostItemsAvailable) {
        synchronized (ItemCache.getItems()) {
            for (Item citem : ItemCache.getItems().values()) {
                citem.setAvailabilityInformation(null);
            }
        }

        List<String> itemIds = new ArrayList<String>(availableItems);
        List<String> removeIds = new ArrayList<String>();
        List<Item> metaGemItemList = new ArrayList<Item>();
        List<Item> gemItemList = new ArrayList<Item>();
        for (String xid : availableItems) {
            int dot = xid.indexOf('.');
            int id = Integer.parseInt((dot >= 0) ? xid.substring(0, dot) : xid);
            if (id > 0) {
                Item availableItem;
                availableItem = ItemCache.findItemById(id);
                if (availableItem != null) {
                    switch (availableItem.getSlot()) {
                        case Meta:
                            metaGemItemList.add(availableItem);
                            removeIds.add(xid);
                            break;
                        case Red:
                        case Orange:
                        case Yellow:
                        case Green:
                        case Blue:
                        case Purple:
                        case Prismatic:
                            gemItemList.add(availableItem);
                            removeIds.add(xid);
                            break;
                    }
                }
            }
        }
        if (gemItemList.isEmpty()) gemItemList.add(null);
        if (metaGemItemList.isEmpty()) metaGemItemList.add(null);
        itemIds.removeIf(x -> x.startsWith("-") || removeIds.contains(x));

        metaGemItems = metaGemItemList.toArray(new Item[0]);
        gemItems = filterItems(gemItemList);

        for (int i = 0; i < slotCount; i++) {
            slotItems[i] = new ArrayList<ItemInstance>();
            slotRawItems[i] = new ArrayList<Item>();
        }

        slotAvailableEnchants[CharacterSlot.Back.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Back, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Chest.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Chest, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Feet.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Feet, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Finger1.value()] = slotAvailableEnchants[CharacterSlot.Finger2.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Finger, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Hands.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Hands, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Head.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Head, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Legs.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Legs, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Shoulders.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Shoulders, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.MainHand.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.MainHand, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.OffHand.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.OffHand, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Ranged.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Ranged, characters, availableItems, models));
        slotAvailableEnchants[CharacterSlot.Wrist.value()] = filterEnchants(Enchant.findEnchants(ItemSlot.Wrist, characters, availableItems, models));

        Item item = null;
        List<ItemInstance> possibleGemmedItems = null;
        List<String> gemmedIds = new ArrayList<String>();
        Map<String, Map<String, Boolean>> gemmedIdMap = new LinkedHashMap<String, Map<String, Boolean>>();
        for (String xid : itemIds) {
            int dot = xid.lastIndexOf('.');
            int dot2 = xid.indexOf('.');
            String id = ((dot2 >= 0) ? xid.substring(0, dot2) : xid);
            Map<String, Boolean> map = gemmedIdMap.get(id);
            if (map == null) {
                map = new LinkedHashMap<String, Boolean>();
                gemmedIdMap.put(id, map);
            }
            String gemmedId = (dot >= 0) ? xid.substring(0, dot) : (xid + ".*.*.*");
            String restriction = (dot >= 0) ? xid.substring(dot + 1) : "*";
            map.put(gemmedId + "." + restriction, true);
            if (overrideReenchant) map.put(gemmedId + ".*", true);

            if (overrideRegem) {
                gemmedId = id + ".*.*.*";
                map.put(gemmedId + "." + restriction, true);
                if (overrideReenchant) map.put(gemmedId + ".*", true);
            }
        }
        if (generateDirectUpgrades) {
            for (int i = 0; i < slotCount; i++) {
                slotDirectUpgrades[i] = new ArrayList<List<DirectUpgradeEntry>>();
                slotDirectUpgrades[i].add(new ArrayList<DirectUpgradeEntry>()); // add a list for all singles
            }
        }
        if (positiveCostItemsAvailable) {
            // add items that have positive cost and are not available yet in any form
            // but DONT add the item if it requires a different class
            synchronized (ItemCache.getItems()) {
                for (Item citem : ItemCache.getItems().values()) {
                    if (citem.getCost() > 0.0f && (citem.getRequiredClasses() == null || (citem.getRequiredClasses() != null && citem.getRequiredClasses().contains(characters[0].getCharacterClass().name())))) {
                        String key = Integer.toString(citem.getId());
                        if (!gemmedIdMap.containsKey(key)) {
                            Map<String, Boolean> map = new LinkedHashMap<String, Boolean>();
                            gemmedIdMap.put(key, map);
                            map.put("C" + key + ".*.*.*.*", true);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Map<String, Boolean>> keyMap : gemmedIdMap.entrySet()) {
            int itemId = Integer.parseInt(keyMap.getKey());
            item = ItemCache.findItemById(itemId);

            // disallow non-equippable items that may appear in malformed legacy availability data

            boolean isRelevant = false;
            for (CalculationsBase model : models) {
                // have to use the relevant types from the model, so we get types that can be equipped at all
                //if (item != null && ItemFilter.GetRelevantItemTypesList(model).Contains(item.Type))
                if (item != null && model.getRelevantItemTypes().contains(item.getType())) {
                    isRelevant = true;
                    break;
                }
            }

            if (isRelevant) {
                int slot = Character.getCharacterSlotByItemSlot(item.getSlot()).value();
                if (slot < 0 || slot >= slotCount) continue;

                if (item.getAvailabilityInformation() == null) {
                    generateItemAvailabilityInformation(item);
                }

                gemmedIds = new ArrayList<String>(keyMap.getValue().keySet());
                Set<String> seenGemmedIds = new LinkedHashSet<String>();
                possibleGemmedItems = new ArrayList<ItemInstance>();
                for (String gid : gemmedIds) {
                    if (gid.startsWith("C")) {
                        item.getAvailabilityInformation().positiveCostItem = true;
                    }
                    String ggid = gid;
                    while (ggid.startsWith("C")) {
                        ggid = ggid.substring(1);
                    }
                    String[] idTokens = ggid.split("\\.");
                    boolean blueDiamond = (idTokens.length > 1 && !idTokens[1].equals("*"));
                    for (ItemInstance gemmedItem : getPossibleGemmedItemsForItem(item, ggid, item.getAvailabilityInformation())) {
                        if (item.getAvailabilityInformation().defaultItemInstance == null && blueDiamond) {
                            item.getAvailabilityInformation().defaultItemInstance = gemmedItem;
                        }
                        if (seenGemmedIds.add(gemmedItem.getGemmedId())) {
                            possibleGemmedItems.add(gemmedItem);
                        }
                    }
                }
                for (Map.Entry<String, Boolean> kvp : item.getAvailabilityInformation().itemAvailable.entrySet()) {
                    itemAvailable.put(kvp.getKey(), kvp.getValue());
                }
                possibleGemmedItems = filterItemInstances(possibleGemmedItems, false);
                item.getAvailabilityInformation().itemList = possibleGemmedItems;
                List<Integer> slotList = null;
                if (item.getAvailabilityInformation().validSlots == null) {
                    slotList = new ArrayList<Integer>();
                }
                for (int i = 0; i < slotCount; i++) {
                    boolean fits = false;
                    for (Character character : characters) {
                        if (item.fitsInSlot(CharacterSlot.fromValue(i), character, true)) {
                            fits = true;
                            break;
                        }
                    }
                    if (fits) {
                        if (slotList != null) {
                            slotList.add(i);
                        }
                        slotItems[i].addAll(possibleGemmedItems);
                        slotRawItems[i].add(item);
                        if (generateDirectUpgrades) {
                            slotDirectUpgrades[i].get(0).addAll(item.getAvailabilityInformation().singleDirectUpgradeList);
                            slotDirectUpgrades[i].add(item.getAvailabilityInformation().matchingDirectUpgradeList);
                            slotDirectUpgrades[i].add(item.getAvailabilityInformation().nonMatchingDirectUpgradeList);
                        }
                    }
                }
                if (slotList != null) {
                    item.getAvailabilityInformation().validSlots = new int[slotList.size()];
                    for (int i = 0; i < slotList.size(); i++) {
                        item.getAvailabilityInformation().validSlots[i] = slotList.get(i);
                    }
                }
            }
        }

        for (int i = 0; i < slotCount; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            if (slot == CharacterSlot.Finger1 || slot == CharacterSlot.Finger2 || slot == CharacterSlot.Trinket1 || slot == CharacterSlot.Trinket2 || slot == CharacterSlot.MainHand || slot == CharacterSlot.OffHand || slotItems[i].isEmpty()) {
                slotItems[i].add(null);
            }
        }

        if (slotFiltering) {
            for (int i = 0; i < slotCount; i++) {
                CharacterSlot slot = CharacterSlot.fromValue(i);
                if (slot != CharacterSlot.Finger1 && slot != CharacterSlot.Finger2 && slot != CharacterSlot.Trinket1 && slot != CharacterSlot.Trinket2) {
                    slotItems[i] = filterItemInstances(slotItems[i], true);
                }
            }
        }

        // mark currently equipped items as default if they're not overriden by blue diamond
        for (Character character : characters) {
            for (int i = 0; i < slotCount; i++) {
                ItemInstance itemInstance = character.getItem(i);
                if (itemInstance != null && itemInstance.getItem() != null && itemInstance.getItem().getAvailabilityInformation() != null && itemInstance.getItem().getAvailabilityInformation().defaultItemInstance == null) {
                    itemInstance.getItem().getAvailabilityInformation().defaultItemInstance = itemInstance;
                }
            }
        }
    }

    /** Expands an availability string without recording it on the item's availability structure. */
    public List<ItemInstance> getPossibleGemmedItemsForItem(Item item, String gemmedId) {
        return getPossibleGemmedItemsForItem(item, gemmedId, null);
    }

    /**
     * Expands one availability string of the form
     * {@code itemId[.gem1[.gem2[.gem3[.enchant]]]]}, where each position may be a concrete id or a
     * wildcard, into the list of concrete item instances. Combinations that would break unique-gem
     * limits are skipped. When wildcard gems do not match their socket color only the canonically
     * ordered gem permutation is kept, so the dominance filter sees each gemming once. When
     * {@code availability} is supplied, every produced gemmed id is registered as available and the
     * direct-upgrade trees (plus the single-entry list for fully specified strings) are populated.
     */
    public List<ItemInstance> getPossibleGemmedItemsForItem(Item item, String gemmedId, ItemAvailabilityInformation availability) {
        List<ItemInstance> possibleGemmedItems = new ArrayList<ItemInstance>();
        String[] ids = gemmedId.split("\\.");
        Item[] possibleGem1s, possibleGem2s, possibleGem3s = null;
        Enchant[] possibleEnchants = null;
        boolean blacksmithingSocket = (item.getSlot() == ItemSlot.Waist && characters[0].getWaistBlacksmithingSocketEnabled()) || (item.getSlot() == ItemSlot.Hands && characters[0].getHandsBlacksmithingSocketEnabled()) || (item.getSlot() == ItemSlot.Wrist && characters[0].getWristBlacksmithingSocketEnabled());

        if (ids.length <= 1 || (ids.length > 1 && ids[1].equals("*"))) {
            switch (item.getSocketColor1()) {
                case Meta:
                    possibleGem1s = metaGemItems;
                    break;
                case Red:
                case Orange:
                case Yellow:
                case Green:
                case Blue:
                case Purple:
                case Prismatic:
                    possibleGem1s = gemItems;
                    break;
                default:
                    if (blacksmithingSocket) {
                        possibleGem1s = gemItems;
                        blacksmithingSocket = false;
                    } else {
                        possibleGem1s = new Item[] { null };
                    }
                    break;
            }
        } else {
            possibleGem1s = new Item[] { ItemCache.findItemById(Integer.parseInt(ids[1])) };
        }

        if (ids.length <= 2 || (ids.length > 2 && ids[2].equals("*"))) {
            switch (item.getSocketColor2()) {
                case Meta:
                    possibleGem2s = metaGemItems;
                    break;
                case Red:
                case Orange:
                case Yellow:
                case Green:
                case Blue:
                case Purple:
                case Prismatic:
                    possibleGem2s = gemItems;
                    break;
                default:
                    if (blacksmithingSocket) {
                        possibleGem2s = gemItems;
                        blacksmithingSocket = false;
                    } else {
                        possibleGem2s = new Item[] { null };
                    }
                    break;
            }
        } else {
            possibleGem2s = new Item[] { ItemCache.findItemById(Integer.parseInt(ids[2])) };
        }

        if (ids.length <= 3 || (ids.length > 3 && ids[3].equals("*"))) {
            switch (item.getSocketColor3()) {
                case Meta:
                    possibleGem3s = metaGemItems;
                    break;
                case Red:
                case Orange:
                case Yellow:
                case Green:
                case Blue:
                case Purple:
                case Prismatic:
                    possibleGem3s = gemItems;
                    break;
                default:
                    if (blacksmithingSocket) {
                        possibleGem3s = gemItems;
                        blacksmithingSocket = false;
                    } else {
                        possibleGem3s = new Item[] { null };
                    }
                    break;
            }
        } else {
            possibleGem3s = new Item[] { ItemCache.findItemById(Integer.parseInt(ids[3])) };
        }

        if (ids.length <= 4 || (ids.length > 4 && ids[4].equals("*"))) {
            int slotIndex = Character.getCharacterSlotByItemSlot(item.getSlot()).value();
            if (slotIndex < slotAvailableEnchants.length)
                possibleEnchants = slotAvailableEnchants[slotIndex];
            if (possibleEnchants == null) possibleEnchants = new Enchant[] { null };
        } else {
            possibleEnchants = new Enchant[] { Enchant.findEnchant(Integer.parseInt(ids[4]), item.getSlot(), null) };
        }

        boolean generative = (ids.length <= 1 || (ids.length > 1 && ids[1].equals("*"))) && (ids.length <= 2 || (ids.length > 2 && ids[2].equals("*"))) && (ids.length <= 3 || (ids.length > 3 && ids[3].equals("*")));
        if (generative && availability != null) {
            if (availability.gemCount == 0) {
                generative = false;
            }
        }

        if (generative && availability != null && generateDirectUpgrades) {
            for (Enchant enchant : possibleEnchants) {
                if (!availability.generativeEnchants.contains(enchant)) {
                    availability.generativeEnchants.add(enchant);
                }
            }
        }

        for (Item gem1 : possibleGem1s) {
            for (Item gem2 : possibleGem2s) {
                // prevent combinations that break unique constraints
                if (gem1 != null && !gem1.isJewelersGem() && gem1.isUnique() && gem2 != null && gem1.getId() == gem2.getId()) continue;
                for (Item gem3 : possibleGem3s) {
                    if (gem3 != null && !gem3.isJewelersGem() && gem3.isUnique()) {
                        if (gem1 != null && gem1.getId() == gem3.getId()) continue;
                        if (gem2 != null && gem2.getId() == gem3.getId()) continue;
                    }
                    for (Enchant enchant : possibleEnchants) {
                        if (availability != null) {
                            // any combination is actually available
                            gemmedId = item.getId() + "." + (gem1 != null ? gem1.getId() : 0) + "." + (gem2 != null ? gem2.getId() : 0) + "." + (gem3 != null ? gem3.getId() : 0) + "." + (enchant != null ? enchant.id : 0);
                            availability.itemAvailable.put(gemmedId, true);
                        }
                        // if gems do not match socket colors then it does not matter in what order they are placed (except for meta)
                        // make it easy on filtering and only add one canon version of the item in which normal gem ids are nondecreasing
                        // if that happens to be an ordering that matches colors it doesn't matter since socket bonuses only add value
                        // either all gems are * or all are specified, obviously if all are specified we can't do this
                        boolean add = true;
                        if (!Item.gemMatchesSlot(gem1, item.getSocketColor1()) || !Item.gemMatchesSlot(gem2, item.getSocketColor2()) || !Item.gemMatchesSlot(gem3, item.getSocketColor3())) {
                            if (generative) {
                                List<Integer> gemOrder = new ArrayList<Integer>();
                                if (gem1 != null && gem1.getSlot() != ItemSlot.Meta) gemOrder.add(gem1.getId());
                                if (gem2 != null && gem2.getSlot() != ItemSlot.Meta) gemOrder.add(gem2.getId());
                                if (gem3 != null && gem3.getSlot() != ItemSlot.Meta) gemOrder.add(gem3.getId());
                                for (int i = 0; i < gemOrder.size() - 1; i++) {
                                    if (gemOrder.get(i) > gemOrder.get(i + 1)) {
                                        add = false;
                                        break;
                                    }
                                }
                                if (availability != null && generateDirectUpgrades) {
                                    Map<String, DirectUpgradeEntry> map = availability.nonMatchingMap;
                                    int g1 = gem1 != null ? gem1.getId() : 0;
                                    int g2 = gem2 != null ? gem2.getId() : 0;
                                    int g3 = gem3 != null ? gem3.getId() : 0;
                                    int en = enchant != null ? enchant.id : 0;
                                    String gemId1 = g1 + ".0.0.0";
                                    String gemId2 = g1 + "." + g2 + ".0.0";
                                    String gemId3 = g1 + "." + g2 + "." + g3 + ".0";
                                    String gemId4 = g1 + "." + g2 + "." + g3 + "." + en;
                                    List<DirectUpgradeEntry> list = availability.nonMatchingDirectUpgradeList;
                                    if (gem1 != null) {
                                        DirectUpgradeEntry entry1 = map.get(gemId1);
                                        if (entry1 == null) {
                                            entry1 = new DirectUpgradeEntry();
                                            entry1.itemInstance = new ItemInstance(item, gem1, null, null, null);
                                            entry1.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                            map.put(gemId1, entry1);
                                            list.add(entry1);
                                        }
                                        list = entry1.directUpgradeList;
                                    }
                                    if (gem2 != null) {
                                        DirectUpgradeEntry entry2 = map.get(gemId2);
                                        if (entry2 == null) {
                                            entry2 = new DirectUpgradeEntry();
                                            entry2.itemInstance = new ItemInstance(item, gem1, gem2, null, null);
                                            entry2.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                            map.put(gemId2, entry2);
                                            list.add(entry2);
                                        }
                                        list = entry2.directUpgradeList;
                                    }
                                    if (gem3 != null) {
                                        DirectUpgradeEntry entry3 = map.get(gemId3);
                                        if (entry3 == null) {
                                            entry3 = new DirectUpgradeEntry();
                                            entry3.itemInstance = new ItemInstance(item, gem1, gem2, gem3, null);
                                            entry3.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                            map.put(gemId3, entry3);
                                            list.add(entry3);
                                        }
                                        list = entry3.directUpgradeList;
                                    }
                                    DirectUpgradeEntry entry4 = map.get(gemId4);
                                    if (entry4 == null) {
                                        entry4 = new DirectUpgradeEntry();
                                        entry4.itemInstance = new ItemInstance(item, gem1, gem2, gem3, enchant);
                                        map.put(gemId4, entry4);
                                        list.add(entry4);
                                    }
                                }
                            }
                        } else {
                            if (generative && availability != null && generateDirectUpgrades) {
                                Map<String, DirectUpgradeEntry> map = availability.matchingMap;
                                int g1 = gem1 != null ? gem1.getId() : 0;
                                int g2 = gem2 != null ? gem2.getId() : 0;
                                int g3 = gem3 != null ? gem3.getId() : 0;
                                int en = enchant != null ? enchant.id : 0;
                                String gemId1 = g1 + ".0.0.0";
                                String gemId2 = g1 + "." + g2 + ".0.0";
                                String gemId3 = g1 + "." + g2 + "." + g3 + ".0";
                                String gemId4 = g1 + "." + g2 + "." + g3 + "." + en;
                                List<DirectUpgradeEntry> list = availability.matchingDirectUpgradeList;
                                if (gem1 != null) {
                                    DirectUpgradeEntry entry1 = map.get(gemId1);
                                    if (entry1 == null) {
                                        entry1 = new DirectUpgradeEntry();
                                        entry1.itemInstance = new ItemInstance(item, gem1, null, null, null);
                                        entry1.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                        map.put(gemId1, entry1);
                                        list.add(entry1);
                                    }
                                    list = entry1.directUpgradeList;
                                }
                                if (gem2 != null) {
                                    DirectUpgradeEntry entry2 = map.get(gemId2);
                                    if (entry2 == null) {
                                        entry2 = new DirectUpgradeEntry();
                                        entry2.itemInstance = new ItemInstance(item, gem1, gem2, null, null);
                                        entry2.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                        map.put(gemId2, entry2);
                                        list.add(entry2);
                                    }
                                    list = entry2.directUpgradeList;
                                }
                                if (gem3 != null) {
                                    DirectUpgradeEntry entry3 = map.get(gemId3);
                                    if (entry3 == null) {
                                        entry3 = new DirectUpgradeEntry();
                                        entry3.itemInstance = new ItemInstance(item, gem1, gem2, gem3, null);
                                        entry3.directUpgradeList = new ArrayList<DirectUpgradeEntry>();
                                        map.put(gemId3, entry3);
                                        list.add(entry3);
                                    }
                                    list = entry3.directUpgradeList;
                                }
                                DirectUpgradeEntry entry4 = map.get(gemId4);
                                if (entry4 == null) {
                                    entry4 = new DirectUpgradeEntry();
                                    entry4.itemInstance = new ItemInstance(item, gem1, gem2, gem3, enchant);
                                    map.put(gemId4, entry4);
                                    list.add(entry4);
                                }
                            }
                        }
                        if (add) {
                            ItemInstance instance = new ItemInstance(item, gem1, gem2, gem3, enchant);
                            possibleGemmedItems.add(instance);
                            if (availability != null && !generative && generateDirectUpgrades) {
                                boolean addSingle = true;
                                for (DirectUpgradeEntry entry : availability.singleDirectUpgradeList) {
                                    if (entry.itemInstance == instance) {
                                        addSingle = false;
                                        break;
                                    }
                                }
                                if (addSingle) {
                                    DirectUpgradeEntry single = new DirectUpgradeEntry();
                                    single.itemInstance = instance;
                                    availability.singleDirectUpgradeList.add(single);
                                }
                            }
                        }
                    }
                }
            }
        }

        return possibleGemmedItems;
    }

    /**
     * Dominance filter for consumes: keeps a buff only when no already-kept buff has
     * greater-or-equal stats, and removes kept buffs that a new one strictly dominates. Null
     * entries pass through; the relative order of the survivors is preserved.
     */
    public static List<Buff> filterBuffs(List<Buff> unfilteredList) {
        List<Buff> filteredList = new ArrayList<Buff>();
        for (Buff buff : unfilteredList) {
            if (buff == null) {
                filteredList.add(buff);
                continue;
            }

            boolean addBuff = true;
            List<Buff> removeBuffs = new ArrayList<Buff>();
            for (Buff buff2 : filteredList) {
                ArrayUtils.CompareResult compare = buff.stats.compare(buff2.stats);
                if (compare == ArrayUtils.CompareResult.GreaterThan) //A>B
                {
                    removeBuffs.add(buff2);
                } else if (compare == ArrayUtils.CompareResult.Equal || compare == ArrayUtils.CompareResult.LessThan) {
                    addBuff = false;
                    break;
                }
            }
            for (Buff removeBuff : removeBuffs)
                filteredList.remove(removeBuff);
            if (addBuff) filteredList.add(buff);
        }
        return filteredList;
    }

    /**
     * Dominance filter for one slot's enchant list; only enchants for the same slot are compared.
     * The "no enchant" entry (id 0) is discarded unless it is the only candidate. Returns a new
     * array in encounter order.
     */
    public static Enchant[] filterEnchants(List<Enchant> unfilteredList) {
        List<Enchant> filteredList = new ArrayList<Enchant>();
        for (Enchant enchant : unfilteredList) {
            if (enchant.id == 0) {
                // only add no enchant if it is the only enchant
                // it won't be filtered because it has a null slot
                if (unfilteredList.size() == 1) {
                    filteredList.add(enchant);
                }
                continue;
            }

            boolean addEnchant = true;
            List<Enchant> removeEnchants = new ArrayList<Enchant>();
            for (Enchant enchant2 : filteredList) {
                if (enchant.slot == enchant2.slot) {
                    ArrayUtils.CompareResult compare = enchant.stats.compare(enchant2.stats);
                    if (compare == ArrayUtils.CompareResult.GreaterThan) //A>B
                    {
                        removeEnchants.add(enchant2);
                    } else if (compare == ArrayUtils.CompareResult.Equal || compare == ArrayUtils.CompareResult.LessThan) {
                        addEnchant = false;
                        break;
                    }
                }
            }
            for (Enchant removeEnchant : removeEnchants)
                filteredList.remove(removeEnchant);
            if (addEnchant) filteredList.add(enchant);
        }
        return filteredList.toArray(new Enchant[0]);
    }

    /**
     * Dominance filter for candidate items (gems, mostly): an item survives only if no kept item
     * is at least as good, where comparison also considers the set name and the jewelcrafter and
     * unique gem flags. Null entries pass through; returns a new array.
     */
    public static Item[] filterItems(List<Item> unfilteredList) {
        List<Item> filteredList = new ArrayList<Item>();
        List<StatsColors> filteredStatsColors = new ArrayList<StatsColors>();
        for (Item item : unfilteredList) {
            if (item == null) {
                filteredList.add(item);
                continue;
            }

            StatsColors statsColorsA = new StatsColors();
            statsColorsA.item = item;
            statsColorsA.itemIsJewelersGem = item.isJewelersGem();
            statsColorsA.ignore = item.isGem() && !item.isJewelersGem() && item.isUnique();
            statsColorsA.setSetName(item.getSetName());
            statsColorsA.stats = item.getStats();
            boolean addItem = true;
            List<StatsColors> removeItems = new ArrayList<StatsColors>();
            for (StatsColors statsColorsB : filteredStatsColors) {
                ArrayUtils.CompareResult compare = statsColorsA.compareTo(statsColorsB);
                if (compare == ArrayUtils.CompareResult.GreaterThan) //A>B
                {
                    removeItems.add(statsColorsB);
                } else if (compare == ArrayUtils.CompareResult.Equal || compare == ArrayUtils.CompareResult.LessThan) {
                    addItem = false;
                    break;
                }
            }
            for (StatsColors removeItem : removeItems)
                filteredStatsColors.remove(removeItem);
            if (addItem) filteredStatsColors.add(statsColorsA);
        }
        for (StatsColors statsColors : filteredStatsColors) {
            filteredList.add(statsColors.item);
        }
        return filteredList.toArray(new Item[0]);
    }

    /**
     * Dominance filter for gemmed item instances. Besides the total stats, the comparison tracks
     * the count of matching socket colors and of jewelcrafter gems (a gemming that activates more
     * sockets or fits the three-jewelcrafter budget is better even at equal stats), the set name
     * and the positive-cost flag. With {@code itemsFilteredHint} true, instances of the same item
     * are never compared against each other, so an item keeps all of its gemmings when the raw
     * item list was already reduced globally. Null entries pass through.
     */
    public static List<ItemInstance> filterItemInstances(List<ItemInstance> unfilteredList, boolean itemsFilteredHint) {
        List<ItemInstance> filteredList = new ArrayList<ItemInstance>();
        List<StatsColors> filteredStatsColors = new ArrayList<StatsColors>();
        for (ItemInstance gemmedItem : unfilteredList) {
            if (gemmedItem == null) {
                filteredList.add(gemmedItem);
                continue;
            }
            int meta = 0, red = 0, yellow = 0, blue = 0, jeweler = 0;
            boolean ignore = false;
            for (Item gem : new Item[] { gemmedItem.getGem1(), gemmedItem.getGem2(), gemmedItem.getGem3() })
                if (gem != null) {
                    switch (gem.getSlot()) {
                        case Meta: meta++; break;
                        case Red: red++; break;
                        case Orange: red++; yellow++; break;
                        case Yellow: yellow++; break;
                        case Green: yellow++; blue++; break;
                        case Blue: blue++; break;
                        case Purple: blue++; red++; break;
                        case Prismatic: red++; yellow++; blue++; break;
                    }
                    if (gem.isJewelersGem()) {
                        jeweler++;
                    } else if (gem.isUnique()) {
                        ignore = true;
                    }
                }

            StatsColors statsColorsA = new StatsColors();
            statsColorsA.itemInstance = gemmedItem;
            statsColorsA.item = gemmedItem.getItem();
            statsColorsA.setSetName(gemmedItem.getItem().getSetName());
            statsColorsA.stats = gemmedItem.getTotalStats();
            statsColorsA.meta = meta;
            statsColorsA.red = red;
            statsColorsA.yellow = yellow;
            statsColorsA.blue = blue;
            statsColorsA.jeweler = jeweler;
            statsColorsA.ignore = ignore;
            boolean addItem = true;
            List<StatsColors> removeItems = new ArrayList<StatsColors>();
            for (StatsColors statsColorsB : filteredStatsColors) {
                if (!itemsFilteredHint || statsColorsA.item.getId() != statsColorsB.item.getId()) {
                    ArrayUtils.CompareResult compare = statsColorsA.compareTo(statsColorsB);
                    if (compare == ArrayUtils.CompareResult.GreaterThan) //A>B
                    {
                        removeItems.add(statsColorsB);
                    } else if (compare == ArrayUtils.CompareResult.Equal || compare == ArrayUtils.CompareResult.LessThan) {
                        addItem = false;
                        break;
                    }
                }
            }
            for (StatsColors removeItem : removeItems)
                filteredStatsColors.remove(removeItem);
            if (addItem) filteredStatsColors.add(statsColorsA);
        }
        for (StatsColors statsColors : filteredStatsColors) {
            filteredList.add(statsColors.itemInstance);
        }
        return filteredList;
    }

    /**
     * Comparison record used by the dominance filters: the candidate plus the properties that
     * matter beyond raw stats (socket color counts, jewelcrafter gem count, item set name,
     * positive-cost flag, two-hand versus one-hand). Comparison is a partial order, mirroring
     * {@link Stats#compareTo(Stats)}: a candidate is greater, less, equal or incomparable.
     */
    private static class StatsColors {
        public ItemInstance itemInstance;
        public Item item;
        public Stats stats;
        public int meta;
        public int red;
        public int yellow;
        public int blue;
        public int jeweler;
        public boolean ignore;

        private String setName;

        public String getSetName() {
            return setName;
        }

        public void setSetName(String value) {
            setName = value;
            if (setName == null) setName = "";
        }

        public boolean itemIsJewelersGem;

        /**
         * Partial-order comparison. Candidates are incomparable when they differ in set name,
         * jewelcrafter/unique status or socket color counts, or when both sides have both a
         * strictly better and a strictly worse property. Equal means interchangeable for the
         * dominance filter (same socket colors, same stats, same set, both two-hand or both
         * one-hand).
         */
        public ArrayUtils.CompareResult compareTo(StatsColors other) {
            if (ignore || other.ignore) return ArrayUtils.CompareResult.Unequal;
            if (itemIsJewelersGem != other.itemIsJewelersGem) return ArrayUtils.CompareResult.Unequal;
            if (jeweler != other.jeweler) return ArrayUtils.CompareResult.Unequal;
            if (item != null && other.item != null && item.getAvailabilityInformation() != null && other.item.getAvailabilityInformation() != null && item.getAvailabilityInformation().positiveCostItem != other.item.getAvailabilityInformation().positiveCostItem) return ArrayUtils.CompareResult.Unequal;

            if (!Objects.equals(this.setName, other.setName)) return ArrayUtils.CompareResult.Unequal;

            int compare = Integer.compare(meta, other.meta);
            boolean haveLessThan = compare < 0;
            boolean haveGreaterThan = compare > 0;
            if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;

            compare = Integer.compare(red, other.red);
            haveLessThan |= compare < 0;
            haveGreaterThan |= compare > 0;
            if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;

            compare = Integer.compare(yellow, other.yellow);
            haveLessThan |= compare < 0;
            haveGreaterThan |= compare > 0;
            if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;

            compare = Integer.compare(blue, other.blue);
            haveLessThan |= compare < 0;
            haveGreaterThan |= compare > 0;
            if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;

            ArrayUtils.CompareResult compareResult = stats.compare(other.stats);
            if (compareResult == ArrayUtils.CompareResult.Unequal) return ArrayUtils.CompareResult.Unequal;
            haveLessThan |= compareResult == ArrayUtils.CompareResult.LessThan;
            haveGreaterThan |= compareResult == ArrayUtils.CompareResult.GreaterThan;

            if (item != null && other.item != null) {
                compare = Integer.compare(item.getMinDamage(), other.item.getMinDamage());
                haveLessThan |= compare < 0;
                haveGreaterThan |= compare > 0;
                if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;
            }

            if (item != null && (item.getSlot() == ItemSlot.MainHand || item.getSlot() == ItemSlot.OneHand || item.getSlot() == ItemSlot.TwoHand)) {
                if (item.getSlot() == ItemSlot.TwoHand && other.item.getSlot() != ItemSlot.TwoHand && haveGreaterThan) {
                    return ArrayUtils.CompareResult.Unequal;
                }
                if (item.getSlot() != ItemSlot.TwoHand && other.item.getSlot() == ItemSlot.TwoHand && haveLessThan) {
                    return ArrayUtils.CompareResult.Unequal;
                }
            }

            if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;
            else if (haveGreaterThan) {
                if (item != null && item.isGem() && !item.isJewelersGem() && item.isUnique()) return ArrayUtils.CompareResult.Unequal;
                return ArrayUtils.CompareResult.GreaterThan;
            } else if (haveLessThan) {
                if (other.item != null && other.item.isGem() && !other.item.isJewelersGem() && other.item.isUnique()) return ArrayUtils.CompareResult.Unequal;
                return ArrayUtils.CompareResult.LessThan;
            } else {
                return ArrayUtils.CompareResult.Equal;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StatsColors other
                    && meta == other.meta && red == other.red && yellow == other.yellow
                    && blue == other.blue && jeweler == other.jeweler && Stats.equalsStats(stats, other.stats);
        }

        @Override
        public int hashCode() {
            return stats.hashCode() ^ item.hashCode();
        }
    }
}
