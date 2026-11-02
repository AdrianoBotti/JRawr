// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Item.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: identity, gems/enchant resolution, socket bonus matching, total stats accumulation and the
// delegating accessors. IComparable/ToString and the IdsChanged event are omitted (not needed by the
// Mage closure), and the network fallback of Item.LoadFromId is not ported (cache-only, see DECISIONS).
package rawr.base.items;

import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.stats.Stats;

/**
 * One concrete copy of an {@link Item} worn in a character slot: the item id plus the ids of up
 * to three inserted gems and one applied {@link Enchant}.
 *
 * <p>This is the unit that characters, saved files and the optimizer actually manipulate. Two
 * instances of the same item can produce different stats because the gems and the enchant differ
 * (and because gem colors decide whether the socket bonus applies), so result identity is defined
 * on instances, not on items. The canonical textual form, used in saved character files and as
 * the identity for {@link #equals(Object)}, is the <i>gemmed id</i>
 * {@code itemId.gem1Id.gem2Id.gem3Id.enchantId}; an absent gem or enchant is the id {@code 0}
 * (e.g. {@code "51310.0.0.0.0"}).</p>
 *
 * <h2>Total-stats resolution</h2>
 *
 * <p>{@link #accumulateTotalStats(Character, Stats)} is the heart of gear resolution. It sums,
 * in this order:</p>
 * <ol>
 *   <li>the item's base stats,</li>
 *   <li>each gem's stats, but only for gems that {@link Item#meetsRequirements(Character,
 *       boolean[]) meet the character's gem requirements},</li>
 *   <li>the item's socket bonus, but only when every socket is matched by its gem (an empty or
 *       {@link ItemSlot#None} socket counts as matched; see
 *       {@link Item#gemMatchesSlot(Item, ItemSlot)}),</li>
 *   <li>the enchant's stats, but only when the enchant's slot is eligible for this item
 *       (ordinary same-slot enchants; one-hand enchants also fit main-hand, two-hand and
 *       non-shield off-hand weapons; an off-hand enchant only fits shields).</li>
 * </ol>
 *
 * <h2>Caching and volatility</h2>
 *
 * <p>The summed stats are cached together with the current time, and the cache is reused while
 * the item template has not changed ({@code item.getLastChange() <= cachedTime}); changing any
 * id clears it through {@code onIdsChanged()}, and {@link ItemCache#invalidateCachedStats()}
 * clears it globally. When any inserted gem has <i>volatile</i> requirements (meta gems,
 * Jewelcrafter gems, unique gems: the answer depends on the character's whole gem layout), the
 * total must not be cached; in that case, if the caller passed an accumulator for an in-progress
 * batch, the parts are accumulated into it directly and {@code null} is returned so no stale
 * cache can be observed. Otherwise the freshly built stats are cached, tagged with sparse data
 * and returned (and copied into the accumulator, when one was given).</p>
 *
 * <p>Item, gem and enchant objects are resolved lazily through {@link Item#loadFromId(int)} and
 * {@link Enchant#findEnchant(int, ItemSlot, Character)} and re-resolved when the cached object's
 * id no longer matches or the cached item was flagged {@link Item#isInvalid() invalid}.</p>
 *
 * @see Item
 * @see Enchant
 * @see Character
 */
public class ItemInstance {
    private int id;
    private int gem1Id;
    private int gem2Id;
    private int gem3Id;
    private int enchantId;

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
        onIdsChanged();
    }

    public int getGem1Id() {
        return this.gem1Id;
    }

    public void setGem1Id(int value) {
        this.gem1Id = value;
        onIdsChanged();
    }

    public int getGem2Id() {
        return this.gem2Id;
    }

    public void setGem2Id(int value) {
        this.gem2Id = value;
        onIdsChanged();
    }

    public int getGem3Id() {
        return this.gem3Id;
    }

    public void setGem3Id(int value) {
        this.gem3Id = value;
        onIdsChanged();
    }

    public int getEnchantId() {
        return this.enchantId;
    }

    public void setEnchantId(int value) {
        this.enchantId = value;
        onIdsChanged();
    }

    private void updateJewelerCount() {
        int jewelerCount = 0;
        if (Item.isJewelersGemId(this.gem1Id)) jewelerCount++;
        if (Item.isJewelersGemId(this.gem2Id)) jewelerCount++;
        if (Item.isJewelersGemId(this.gem3Id)) jewelerCount++;
        this.jewelerCount = jewelerCount;
    }

    private int jewelerCount;

    private boolean forceDisplay;

    public int getJewelerCount() {
        return jewelerCount;
    }

    public boolean isForceDisplay() {
        return forceDisplay;
    }

    public void setForceDisplay(boolean forceDisplay) {
        this.forceDisplay = forceDisplay;
    }

    private void onIdsChanged() {
        this.gemmedId = "";
        invalidateCachedData();
        updateJewelerCount();
    }

    private Item itemCached = null;

    /**
     * The item template, resolved through the cache and cached locally. Re-resolves when the id
     * changed behind the cache's back or when the cached object was flagged invalid; returns
     * {@code null} for id {@code 0} (an empty instance) or an unknown id.
     */
    public Item getItem() {
        if (this.id <= 0) return null;
        if (itemCached == null || itemCached.getId() != this.id || itemCached.isInvalid()) {
            itemCached = Item.loadFromId(this.id);
        }
        return itemCached;
    }

    // C# Item.cs:1425-1441.
    public int getDisplayId() {
        Item item = getItem();
        return item == null ? 0 : item.getDisplayId();
    }

    public int getDisplaySlot() {
        Item item = getItem();
        return item == null ? 0 : item.getDisplaySlot();
    }

    public void setItem(Item value) {
        itemCached = value;
        if (value == null)
            setId(0);
        else
            setId(value.getId());
    }

    private Item gem1Cached = null;

    public Item getGem1() {
        if (this.gem1Id == 0) return null;
        if (gem1Cached == null || gem1Cached.getId() != this.gem1Id || gem1Cached.isInvalid()) {
            gem1Cached = Item.loadFromId(this.gem1Id);
        }
        return gem1Cached;
    }

    public void setGem1(Item value) {
        gem1Cached = value;
        if (value == null)
            setGem1Id(0);
        else
            setGem1Id(value.getId());
    }

    private Item gem2Cached = null;

    public Item getGem2() {
        if (this.gem2Id == 0) return null;
        if (gem2Cached == null || gem2Cached.getId() != this.gem2Id || gem2Cached.isInvalid()) {
            gem2Cached = Item.loadFromId(this.gem2Id);
        }
        return gem2Cached;
    }

    public void setGem2(Item value) {
        gem2Cached = value;
        if (value == null)
            setGem2Id(0);
        else
            setGem2Id(value.getId());
    }

    private Item gem3Cached = null;

    public Item getGem3() {
        if (this.gem3Id == 0) return null;
        if (gem3Cached == null || gem3Cached.getId() != this.gem3Id || gem3Cached.isInvalid()) {
            gem3Cached = Item.loadFromId(this.gem3Id);
        }
        return gem3Cached;
    }

    public void setGem3(Item value) {
        gem3Cached = value;
        if (value == null)
            setGem3Id(0);
        else
            setGem3Id(value.getId());
    }

    private Enchant enchantCached = null;

    /**
     * The applied enchant, resolved from the catalog for this item's slot and cached. Because
     * {@link Enchant#findEnchant(int, ItemSlot, Character)} falls back to the first enchant when
     * the id is unknown, this never returns {@code null}; an instance with enchant id {@code 0}
     * gets the canonical "No Enchant" entry (whose stats are empty).
     */
    public Enchant getEnchant() {
        if (enchantCached == null || enchantCached.id != this.enchantId) {
            enchantCached = Enchant.findEnchant(this.enchantId, getItem() != null ? getItem().getSlot() : ItemSlot.None, null);
        }
        return enchantCached;
    }

    public void setEnchant(Enchant value) {
        enchantCached = value;
        if (value == null)
            setEnchantId(0);
        else
            setEnchantId(value.id);
    }

    // 1-based index
    public Item getGem(int index) {
        switch (index) {
            case 1:
                return getGem1();
            case 2:
                return getGem2();
            case 3:
                return getGem3();
            default:
                return null;
        }
    }

    public void setGem(int index, Item value) {
        switch (index) {
            case 1:
                setGem1(value);
                break;
            case 2:
                setGem2(value);
                break;
            case 3:
                setGem3(value);
                break;
        }
    }

    private String gemmedId = "";

    /**
     * The gemmed id string, built lazily on first use and cached. Format:
     * {@code itemId.gem1Id.gem2Id.gem3Id.enchantId}, each missing part written as {@code 0}. This
     * is the instance's identity for {@link #equals(Object)} and the persisted form.
     */
    public String getGemmedId() {
        if (this.gemmedId.isEmpty()) {
            this.gemmedId = this.id + "." + this.gem1Id + "." + this.gem2Id + "." + this.gem3Id + "." + this.enchantId;
        }
        return this.gemmedId;
    }

    /**
     * Parses a gemmed id (the saved-file form, see {@link #getGemmedId()}) into the id fields and
     * refreshes the derived state. Missing trailing parts are treated as {@code 0}. The
     * {@code onIdsChanged()} call then clears the cached string and the cached item/gem/enchant
     * objects, so the next reads re-resolve everything from the parsed numbers.
     */
    public void setGemmedId(String value) {
        if (value == null) this.gemmedId = "";
        else this.gemmedId = value;
        String[] ids = this.gemmedId.split("\\.");
        this.id = Integer.parseInt(ids[0]);
        this.gem1Id = ids.length > 1 ? Integer.parseInt(ids[1]) : 0;
        this.gem2Id = ids.length > 2 ? Integer.parseInt(ids[2]) : 0;
        this.gem3Id = ids.length > 3 ? Integer.parseInt(ids[3]) : 0;
        this.enchantId = ids.length > 4 ? Integer.parseInt(ids[4]) : 0;
        onIdsChanged();
    }

    public ItemInstance() {
    }

    public ItemInstance(String gemmedId) {
        String[] ids = gemmedId.split("\\.");
        this.id = Integer.parseInt(ids[0]);
        this.gem1Id = ids.length > 1 ? Integer.parseInt(ids[1]) : 0;
        this.gem2Id = ids.length > 2 ? Integer.parseInt(ids[2]) : 0;
        this.gem3Id = ids.length > 3 ? Integer.parseInt(ids[3]) : 0;
        this.enchantId = ids.length > 4 ? Integer.parseInt(ids[4]) : 0;
        updateJewelerCount();
    }

    public ItemInstance(int id, int gem1Id, int gem2Id, int gem3Id, int enchantId) {
        this.id = id;
        this.gem1Id = gem1Id;
        this.gem2Id = gem2Id;
        this.gem3Id = gem3Id;
        this.enchantId = enchantId;
        updateJewelerCount();
    }

    public ItemInstance(Item item, Item gem1, Item gem2, Item gem3, Enchant enchant) {
        itemCached = item;
        gem1Cached = gem1;
        gem2Cached = gem2;
        gem3Cached = gem3;
        enchantCached = enchant;
        this.id = item != null ? item.getId() : 0;
        this.gem1Id = gem1 != null ? gem1.getId() : 0;
        this.gem2Id = gem2 != null ? gem2.getId() : 0;
        this.gem3Id = gem3 != null ? gem3.getId() : 0;
        this.enchantId = enchant != null ? enchant.id : 0;
        onIdsChanged();
    }

    // C# ItemInstance.Clone() (Item.cs:1386)
    /**
     * An independent copy: creates a new instance and points it at the same resolved item, gems
     * and enchant (the templates are shared; only the instance is new). Note that it clones what
     * the getters currently resolve, so if a gem id is unknown the shared "no enchant"
     * placeholder or a {@code null} gem is copied rather than the raw ids.
     */
    public ItemInstance clone() {
        ItemInstance clone = new ItemInstance();
        clone.setItem(getItem());
        clone.setGem1(getGem1());
        clone.setGem2(getGem2());
        clone.setGem3(getGem3());
        clone.setEnchant(getEnchant());
        return clone;
    }

    /**
     * True when every socket's gem color matches that socket (see
     * {@link Item#gemMatchesSlot(Item, ItemSlot)}), i.e. when the item's socket bonus would be
     * granted. A colored socket without a gem does not match; an item with no sockets at all
     * matches trivially. {@link #accumulateTotalStats(Character, Stats)} recomputes the same
     * condition for its own eligibility flag. A missing item template has no sockets and matches.
     */
    public boolean matchesSocketBonus() {
        Item item = getItem();
        if (item == null) return true;
        return Item.gemMatchesSlot(getGem1(), item.getSocketColor1()) &&
                Item.gemMatchesSlot(getGem2(), item.getSocketColor2()) &&
                Item.gemMatchesSlot(getGem3(), item.getSocketColor3());
    }

    private Stats cachedTotalStats = null;
    private long cachedTime = Long.MIN_VALUE;

    public void invalidateCachedData() {
        cachedTotalStats = null;
    }

    public Stats getTotalStats() {
        return accumulateTotalStats(null, null);
    }

    public Stats getTotalStats(Character character) {
        return accumulateTotalStats(character, null);
    }

    /**
     * Resolves the instance's total stats (item + qualifying gems + socket bonus + qualifying
     * enchant; see the class comment for the exact order and eligibility rules). Uses and
     * refreshes the per-instance cache unless a gem has volatile requirements.
     *
     * <p>If {@code unsafeStatsAccumulator} is non-null, the result is also accumulated into it
     * using {@code Stats}' unsafe batch API, so callers that pass an accumulator save the extra
     * copy. In the volatile case the method accumulates the parts into the accumulator itself and
     * returns {@code null} (that is not an error); with no accumulator it returns the uncached
     * sum. Returns empty {@link Stats} when the item template is unknown (a missing import item).</p>
     */
    public Stats accumulateTotalStats(Character character, Stats unsafeStatsAccumulator) {
        Item item = getItem();
        if (item == null) {
            return new Stats();
        }
        if (cachedTotalStats != null && item.getLastChange() <= cachedTime) {
            if (unsafeStatsAccumulator != null) {
                unsafeStatsAccumulator.accumulateUnsafe(cachedTotalStats);
            }
            return cachedTotalStats;
        }
        Item g1 = getGem1();
        Item g2 = getGem2();
        Item g3 = getGem3();
        Enchant enchant = getEnchant();
        boolean volatileItem = false;
        boolean gem1 = false;
        boolean gem2 = false;
        boolean gem3 = false;
        boolean eligibleForSocketBonus = Item.gemMatchesSlot(g1, item.getSocketColor1()) &&
                Item.gemMatchesSlot(g2, item.getSocketColor2()) &&
                Item.gemMatchesSlot(g3, item.getSocketColor3());
        if (g1 != null) {
            Item.RequirementCheck check = g1.checkRequirements(character);
            gem1 = check.meets();
            volatileItem |= check.volatileRequirements();
        }
        if (g2 != null) {
            Item.RequirementCheck check = g2.checkRequirements(character);
            gem2 = check.meets();
            volatileItem |= check.volatileRequirements();
        }
        if (g3 != null) {
            Item.RequirementCheck check = g3.checkRequirements(character);
            gem3 = check.meets();
            volatileItem |= check.volatileRequirements();
        }
        if (volatileItem && unsafeStatsAccumulator != null) {
            unsafeStatsAccumulator.accumulateUnsafe(item.getStats(), true);
            if (gem1) unsafeStatsAccumulator.accumulateUnsafe(g1.getStats(), true);
            if (gem2) unsafeStatsAccumulator.accumulateUnsafe(g2.getStats(), true);
            if (gem3) unsafeStatsAccumulator.accumulateUnsafe(g3.getStats(), true);
            if (eligibleForSocketBonus) unsafeStatsAccumulator.accumulateUnsafe(item.getSocketBonus(), true);
            if (isEnchantEligible(enchant)) unsafeStatsAccumulator.accumulateUnsafe(enchant.stats, true);
            return null;
        } else {
            Stats totalItemStats = new Stats();
            totalItemStats.accumulateUnsafe(item.getStats(), true);
            if (gem1) totalItemStats.accumulateUnsafe(g1.getStats(), true);
            if (gem2) totalItemStats.accumulateUnsafe(g2.getStats(), true);
            if (gem3) totalItemStats.accumulateUnsafe(g3.getStats(), true);
            if (eligibleForSocketBonus) totalItemStats.accumulateUnsafe(item.getSocketBonus(), true);
            if (isEnchantEligible(enchant)) totalItemStats.accumulateUnsafe(enchant.stats, true);
            if (!volatileItem) {
                cachedTime = System.currentTimeMillis();
                cachedTotalStats = totalItemStats;
                cachedTotalStats.generateSparseData();
                if (unsafeStatsAccumulator != null) {
                    unsafeStatsAccumulator.accumulateUnsafe(cachedTotalStats);
                }
            }

            return totalItemStats;
        }
    }

    public boolean fitsInSlot(CharacterSlot characterSlot) {
        Item item = getItem();
        return item != null && item.fitsInSlot(characterSlot);
    }

    public ItemSlot getSlot() {
        if (getItem() == null) return ItemSlot.None;
        return getItem().getSlot();
    }

    public ItemType getType() {
        if (getItem() == null) return ItemType.None;
        return getItem().getType();
    }

    /**
     * Whether {@code enchant}'s stats actually apply to this instance. This is the eligibility
     * rule used by {@link #accumulateTotalStats(Character, Stats)}: ordinary enchants need the
     * exact item slot, one-hand enchants also fit two-hand/main-hand weapons and non-shield
     * off-hand weapons, and off-hand enchants only fit shields.
     */
    public boolean isEnchantEligible(Enchant enchant) {
        if (enchant == null) return false;
        if (enchant.slot == ItemSlot.OneHand) {
            return getSlot() == ItemSlot.OneHand
                    || (getSlot() == ItemSlot.OffHand
                            && getType() != ItemType.Shield
                            && getType() != ItemType.None)
                    || getSlot() == ItemSlot.MainHand
                    || getSlot() == ItemSlot.TwoHand;
        }
        if (enchant.slot == ItemSlot.OffHand) {
            return getType() == ItemType.Shield;
        }
        return enchant.slot == getSlot();
    }

    public int getMinDamage() {
        if (getItem() == null) return 0;
        return getItem().getMinDamage();
    }

    public int getMaxDamage() {
        if (getItem() == null) return 0;
        return getItem().getMaxDamage();
    }

    public ItemDamageType getDamageType() {
        if (getItem() == null) return ItemDamageType.Physical;
        return getItem().getDamageType();
    }

    public float getSpeed() {
        if (getItem() == null) return 0;
        return getItem().getSpeed();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemInstance other && getGemmedId().equals(other.getGemmedId());
    }

    @Override
    public int hashCode() {
        return getGemmedId().hashCode();
    }
}
