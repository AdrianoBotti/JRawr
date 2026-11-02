// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Item.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: data fields, socket/gem handling, requirements. XML serialization, the item source loaders,
// ToString/CompareTo, armory/wowhead loaders and UI helpers are omitted.
package rawr.base.items;

import rawr.base.optimizer.ItemAvailabilityInformation;

import rawr.base.calculations.Calculations;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.sources.LocationFactory;
import rawr.base.optimizer.AvailableItemGenerator;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The definition of one piece of gear as it exists in the game database: a template holding base
 * stats, sockets, requirements and a source, shared by every character that can use it.
 *
 * <p>{@code Item} is "what the item is"; {@link ItemInstance} is "one copy of it that a character
 * wears", carrying the gem ids and the enchant id chosen for that copy. Items are loaded from
 * Rawr's {@code ItemCache.xml} into {@link ItemCache}, where they are looked up by numeric id and
 * filtered into the relevant-items subset the models and the optimizer consume. Models do not
 * read an item's raw {@link #getStats() stats} for a character directly: they ask the equipped
 * {@code ItemInstance} for its total stats, which include gems, the socket bonus and the
 * enchant.</p>
 *
 * <h2>Contents</h2>
 *
 * <ul>
 *   <li><b>Identity and display</b> &mdash; {@link #getId() id}, {@link #getName() name},
 *       {@link #getItemLevel() item level}, {@link #getIconPath() icon path} (stored lowercased
 *       with {@code Locale.ROOT}), display id/slot.</li>
 *   <li><b>Stats and quality</b> &mdash; {@link #getStats()} holds only the item's inherent
 *       (ungemmed, unenchanted) stats; {@link #getQuality()} is the game rarity tier
 *       ({@link ItemQuality}). {@link #getSetName()} links the item to a set whose set bonuses
 *       are contributed by the {@code Buff} system, not by this class.</li>
 *   <li><b>Sockets</b> &mdash; up to three socket colors ({@link ItemSlot#Red} and the other gem
 *       colors) plus the {@link #getSocketBonus() socket bonus} stats granted when every socket
 *       holds a matching gem. A socket is only a color, not a gem: a gem is itself an
 *       {@code Item} whose {@code ItemSlot} is its color. There is no dedicated "socket" type.</li>
 *   <li><b>Weapon data</b> &mdash; {@link #getMinDamage()}, {@link #getMaxDamage()},
 *       {@link #getSpeed()} (seconds per swing) and {@link #getDPS()} (average damage per
 *       second).</li>
 *   <li><b>Requirements</b> &mdash; {@link #getRequiredClasses() requiredClasses} (free-form
 *       text from the data file, not parsed here), {@link #getFaction() faction},
 *       {@link #getBind() bind rule} and the {@code unique}/{@code uniqueId} pair.</li>
 *   <li><b>Source</b> &mdash; {@link #getLocationInfo()} asks {@link LocationFactory} where the
 *       item comes from (vendor, quest, drop, ...); the filter UI matches against those
 *       descriptions.</li>
 *   <li><b>Gem flags</b> &mdash; {@link #isGem()}, {@link #isRedGem()} and the others are
 *       <i>derived</i> from the id and slot by {@code updateGemInformation()} whenever the id or
 *       the slot is set; they are never set directly.</li>
 * </ul>
 *
 * <h2>Uniqueness</h2>
 *
 * <p>An item flagged {@link #isUnique() unique} may only be equipped once (rings, trinkets,
 * one-hand weapons). Because the same drop exists in several versions with different ids (normal
 * and heroic, 10- and 25-player), the {@link #getUniqueId()} list names every id that counts as
 * the same item; {@link ItemCache#autoSetUniqueId(Item)} fills it by matching names and item
 * levels, and {@link #itemsAreConsideredUniqueEqual(Item, Item)} applies it. The "one per
 * character" check itself is done against the rest of the character by
 * {@link #fitsInSlot(CharacterSlot, Character)}, which delegates to {@code Calculations}.</p>
 *
 * <h2>Gem requirement checks</h2>
 *
 * <p>{@link #meetsRequirements(Character, boolean[])} answers "may this gem be used by this
 * character?" for meta gems (color-count conditions hardcoded per meta-gem id), Jewelcrafter-only
 * gems (at most three) and unique gems (at most one copy). Its {@code volatileRequirements} out
 * parameter reports whether the answer depends on the character's other gems, in which case
 * {@link ItemInstance} must not cache the item's total stats. A {@code null} character, or a
 * character with gem-requirement enforcement off, disables the checks.</p>
 *
 * <h2>Caching and shared state</h2>
 *
 * <ul>
 *   <li>{@link #invalidateCachedData()} invalidates the sparse-data caches of the stats and the
 *       socket bonus and bumps {@link #getLastChange() lastChange}; {@link ItemInstance} uses
 *       that timestamp to decide whether its own total-stats cache is still valid.</li>
 *   <li>{@link #isOptimizerManagedVolatiliy()} is process-wide static state: when the optimizer's
 *       item generator sets it, {@code meetsRequirements} skips the Jewelcrafter/unique gem
 *       checks because the optimizer enforces them itself while generating candidates (see
 *       {@code AvailableItemGenerator}).</li>
 *   <li>{@link #getAvailabilityInformation()} is transient per-optimizer-run bookkeeping
 *       (availability plus restrictions); it is not part of the saved data model.</li>
 * </ul>
 *
 * <p>A faithful porting quirk to keep: {@link #clone()} copies only the fields the original
 * {@code Item.Clone} copied; item level, display id, faction, bind rule and the derived gem flags
 * keep their constructor defaults on the clone.</p>
 *
 * @see ItemInstance
 * @see ItemCache
 * @see ItemQuality
 * @see LocationFactory
 */
public class Item {
    private String name;
    private int id;
    private int itemLevel;
    private int displayId;
    private int displaySlot;
    private String iconPath;
    private Stats stats = new Stats();
    private ItemQuality quality = ItemQuality.Poor;
    private String setName;
    private ItemType type = ItemType.None;
    private ItemFaction faction = ItemFaction.Neutral;
    private int minDamage = 0;
    private int maxDamage = 0;
    private ItemDamageType damageType = ItemDamageType.Physical;
    private float speed = 0f;
    private String requiredClasses;
    private boolean unique;
    private List<Integer> uniqueId;
    private BindsOn bind = BindsOn.None;
    private ItemSlot socketColor1 = ItemSlot.None;
    private ItemSlot socketColor2 = ItemSlot.None;
    private ItemSlot socketColor3 = ItemSlot.None;
    private Stats socketBonus = new Stats();
    private String localizedName;
    private ItemSlot slot = ItemSlot.None;
    private boolean isGem;
    private boolean isRedGem;
    private boolean isYellowGem;
    private boolean isBlueGem;
    private boolean isJewelersGem;
    private boolean invalid;
    private long lastChange = Long.MIN_VALUE;
    // C# `public static bool OptimizerManagedVolatiliy` (Item.cs:738); static state shared by the
    // optimizer's AvailableItemGenerator while it generates item instances.
    private static boolean optimizerManagedVolatiliy;
    private float cost;
    // C# `internal Optimizer.ItemAvailabilityInformation AvailabilityInformation` (Item.cs:1066).
    ItemAvailabilityInformation availabilityInformation;

    public Item() {
    }

    // C# Item.cs:561-587 Clone (copies the same subset of fields; ItemLevel/DisplayId/Faction/Bind
    // and the gem flags intentionally stay at their defaults, as in the original).
    /**
     * Returns a deep-enough copy for the optimizer's candidate manipulation: name, quality, id,
     * icon, slot, stats, socket bonus/colors, set, type, weapon data, required classes,
     * uniqueness and localized name are copied, while everything else (item level, display,
     * faction, bind, gem flags) keeps the fresh-object defaults. The clone is a distinct object,
     * but its {@code Stats} are cloned too, so mutating them does not affect the original.
     */
    public Item clone() {
        Item item = new Item();
        item.setName(getName());
        item.setQuality(getQuality());
        item.setId(getId());
        item.setIconPath(getIconPath());
        item.setSlot(getSlot());
        item.setStats(getStats().copy());
        item.setSocketBonus(getSocketBonus().copy());
        item.setSocketColor1(getSocketColor1());
        item.setSocketColor2(getSocketColor2());
        item.setSocketColor3(getSocketColor3());
        item.setSetName(getSetName());
        item.setType(getType());
        item.setMinDamage(getMinDamage());
        item.setMaxDamage(getMaxDamage());
        item.setDamageType(getDamageType());
        item.setSpeed(getSpeed());
        item.setRequiredClasses(getRequiredClasses());
        item.setUnique(isUnique());
        item.setUniqueId(new ArrayList<Integer>(getUniqueId()));
        item.setLocalizedName(getLocalizedName());
        return item;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
        updateGemInformation();
    }

    public int getItemLevel() {
        return this.itemLevel;
    }

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public int getDisplayId() {
        return this.displayId;
    }

    public void setDisplayId(int displayId) {
        this.displayId = displayId;
    }

    public int getDisplaySlot() {
        return this.displaySlot;
    }

    public void setDisplaySlot(int displaySlot) {
        this.displaySlot = displaySlot;
    }

    public String getIconPath() {
        return (this.iconPath == null ? null : this.iconPath.toLowerCase(Locale.ROOT));
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath == null ? null : iconPath.toLowerCase(Locale.ROOT);
    }

    public ItemSlot getSlot() {
        return this.slot;
    }

    public void setSlot(ItemSlot slot) {
        this.slot = slot;
        updateGemInformation();
    }

    public Stats getStats() {
        return this.stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public ItemQuality getQuality() {
        return this.quality;
    }

    public void setQuality(ItemQuality quality) {
        this.quality = quality;
    }

    public String getSetName() {
        return this.setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public ItemFaction getFaction() {
        return this.faction;
    }

    public void setFaction(ItemFaction faction) {
        this.faction = faction;
    }

    /**
     * Faction restriction check used everywhere relevant items are enumerated. A
     * {@link ItemFaction#Neutral} item always fits; otherwise the race must belong to the item's
     * faction. The switch only lists the five Alliance races (Draenei, Dwarf, Gnome, Human,
     * NightElf) explicitly, so every other race &mdash; including invalid or future values &mdash;
     * is treated as Horde, exactly as the original did.
     */
    public boolean fitsFaction(CharacterRace race) {
        boolean fitsFaction = true;
        if (this.faction != ItemFaction.Neutral) {
            switch (race) {
                case Draenei:
                case Dwarf:
                case Gnome:
                case Human:
                case NightElf:
                    fitsFaction &= this.faction == ItemFaction.Alliance;
                    break;

                default:
                    fitsFaction &= this.faction == ItemFaction.Horde;
                    break;
            }
        }
        return fitsFaction;
    }

    public ItemType getType() {
        return this.type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public int getMinDamage() {
        return this.minDamage;
    }

    public void setMinDamage(int minDamage) {
        this.minDamage = minDamage;
    }

    public int getMaxDamage() {
        return this.maxDamage;
    }

    public void setMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
    }

    public ItemDamageType getDamageType() {
        return this.damageType;
    }

    public void setDamageType(ItemDamageType damageType) {
        this.damageType = damageType;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    // C# Item.DPS (Item.cs:351-357): (MinDamage + MaxDamage) * 0.5 / Speed, 0 when Speed is 0.
    public float getDPS() {
        if (this.speed == 0f) return 0f;
        return ((float) (this.minDamage + this.maxDamage) * 0.5f) / this.speed;
    }

    public String getRequiredClasses() {
        return this.requiredClasses;
    }

    public void setRequiredClasses(String requiredClasses) {
        this.requiredClasses = requiredClasses;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public List<Integer> getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(List<Integer> uniqueId) {
        this.uniqueId = uniqueId;
    }

    public BindsOn getBind() {
        return this.bind;
    }

    public void setBind(BindsOn bind) {
        this.bind = bind;
    }

    public ItemSlot getSocketColor1() {
        return this.socketColor1;
    }

    public void setSocketColor1(ItemSlot socketColor1) {
        this.socketColor1 = socketColor1;
    }

    public ItemSlot getSocketColor2() {
        return this.socketColor2;
    }

    public void setSocketColor2(ItemSlot socketColor2) {
        this.socketColor2 = socketColor2;
    }

    public ItemSlot getSocketColor3() {
        return this.socketColor3;
    }

    public void setSocketColor3(ItemSlot socketColor3) {
        this.socketColor3 = socketColor3;
    }

    public ItemSlot getSocketColor(int index) {
        switch (index) {
            case 1:
                return getSocketColor1();
            case 2:
                return getSocketColor2();
            case 3:
                return getSocketColor3();
            default:
                return ItemSlot.None;
        }
    }

    public Stats getSocketBonus() {
        return this.socketBonus;
    }

    public void setSocketBonus(Stats socketBonus) {
        this.socketBonus = socketBonus;
    }

    public String getLocalizedName() {
        return this.localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    /**
     * The item-location table entry for this item as managed by {@link LocationFactory}: a
     * two-element array (primary source, optional second source) or the shared "unknown
     * location" placeholder. Never {@code null}; the "unknown" entry is shared, so callers must
     * not mutate it.
     */
    public ItemLocation[] getLocationInfo() {
        return LocationFactory.lookup(getId());
    }

    public boolean isGem() {
        return this.isGem;
    }

    public boolean isRedGem() {
        return this.isRedGem;
    }

    public boolean isYellowGem() {
        return this.isYellowGem;
    }

    public boolean isBlueGem() {
        return this.isBlueGem;
    }

    public boolean isJewelersGem() {
        return this.isJewelersGem;
    }

    /**
     * True for a gem that is either Jewelcrafter-only or unique: such gems are subject to
     * per-character count limits, which is why they count as "limited" here.
     */
    public boolean isLimitedGem() {
        return this.isGem && (this.isJewelersGem || this.unique);
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public long getLastChange() {
        return lastChange;
    }

    public void setLastChange(long lastChange) {
        this.lastChange = lastChange;
    }

    public static boolean isOptimizerManagedVolatiliy() {
        return optimizerManagedVolatiliy;
    }

    public static void setOptimizerManagedVolatiliy(boolean optimizerManagedVolatiliy) {
        Item.optimizerManagedVolatiliy = optimizerManagedVolatiliy;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public ItemAvailabilityInformation getAvailabilityInformation() {
        return availabilityInformation;
    }

    public void setAvailabilityInformation(ItemAvailabilityInformation availabilityInformation) {
        this.availabilityInformation = availabilityInformation;
    }

    /**
     * True for the hardcoded set of Jewelcrafter-only gem ids (Dragon's Eyes and equivalents).
     * The set is hardcoded because the original did not derive Jewelcrafter-only status from the
     * item data; ids are the stable key. {@code updateGemInformation()} uses this to set the
     * {@code isJewelersGem} flag.
     */
    static boolean isJewelersGemId(int id) {
        return (id == 42142 || id == 36766 || id == 42148 || id == 42143 || id == 42152 || id == 42153 || id == 42146 || id == 42158 || id == 42154 || id == 42150 || id == 42156 || id == 42144 || id == 42149 || id == 36767 || id == 42145 || id == 42155 || id == 42151 || id == 42157);
    }

    private void updateGemInformation() {
        this.isGem = this.slot == ItemSlot.Meta || this.slot == ItemSlot.Blue || this.slot == ItemSlot.Green || this.slot == ItemSlot.Orange || this.slot == ItemSlot.Prismatic || this.slot == ItemSlot.Purple || this.slot == ItemSlot.Red || this.slot == ItemSlot.Yellow;
        this.isJewelersGem = isJewelersGemId(this.id);
        this.isRedGem = this.isGem && gemMatchesSlot(this, ItemSlot.Red);
        this.isYellowGem = this.isGem && gemMatchesSlot(this, ItemSlot.Yellow);
        this.isBlueGem = this.isGem && gemMatchesSlot(this, ItemSlot.Blue);
    }

    /**
     * Decides whether a gem can fill a socket of the given color. Hybrid gem colors match two
     * socket colors each, and prismatic gems match all three:
     * <ul>
     *   <li>{@link ItemSlot#Red} socket &mdash; red, orange, purple or prismatic gem;</li>
     *   <li>{@link ItemSlot#Yellow} socket &mdash; yellow, orange, green or prismatic gem;</li>
     *   <li>{@link ItemSlot#Blue} socket &mdash; blue, green, purple or prismatic gem;</li>
     *   <li>{@link ItemSlot#Meta} socket &mdash; meta gem only.</li>
     * </ul>
     * Any other (or {@link ItemSlot#None}) socket color counts as matched by a {@code null} gem
     * or by any non-meta gem; this is what makes items with fewer than three sockets still
     * eligible for their socket bonus. A {@code null} gem matches only that default case.
     */
    public static boolean gemMatchesSlot(Item gem, ItemSlot slotColor) {
        switch (slotColor) {
            case Red:
                return gem != null && (gem.slot == ItemSlot.Red || gem.slot == ItemSlot.Orange || gem.slot == ItemSlot.Purple || gem.slot == ItemSlot.Prismatic);
            case Yellow:
                return gem != null && (gem.slot == ItemSlot.Yellow || gem.slot == ItemSlot.Orange || gem.slot == ItemSlot.Green || gem.slot == ItemSlot.Prismatic);
            case Blue:
                return gem != null && (gem.slot == ItemSlot.Blue || gem.slot == ItemSlot.Green || gem.slot == ItemSlot.Purple || gem.slot == ItemSlot.Prismatic);
            case Meta:
                return gem != null && (gem.slot == ItemSlot.Meta);
            default:
                return gem == null || gem.slot != ItemSlot.Meta;
        }
    }

    /**
     * "Same unique item" test used when two item ids share a drop: true when {@code itema} is
     * unique and either both ids are equal or {@code itemb}'s id appears in {@code itema}'s
     * {@link #getUniqueId() unique id list}. Note that only the first argument's unique flag and
     * list are consulted, matching the original's asymmetric usage (the candidate is passed
     * first).
     */
    public static boolean itemsAreConsideredUniqueEqual(Item itema, Item itemb) {
        return itema != null && itemb != null && itema.unique && (itema.id == itemb.id || (itema.uniqueId != null && itema.uniqueId.contains(itemb.id)));
    }

    public boolean fitsInSlot(CharacterSlot charSlot) {
        switch (charSlot) {
            case Head:
                return this.slot == ItemSlot.Head;
            case Neck:
                return this.slot == ItemSlot.Neck;
            case Shoulders:
                return this.slot == ItemSlot.Shoulders;
            case Back:
                return this.slot == ItemSlot.Back;
            case Chest:
                return this.slot == ItemSlot.Chest;
            case Shirt:
                return this.slot == ItemSlot.Shirt;
            case Tabard:
                return this.slot == ItemSlot.Tabard;
            case Wrist:
                return this.slot == ItemSlot.Wrist;
            case Hands:
                return this.slot == ItemSlot.Hands;
            case Waist:
                return this.slot == ItemSlot.Waist;
            case Legs:
                return this.slot == ItemSlot.Legs;
            case Feet:
                return this.slot == ItemSlot.Feet;
            case Finger1:
            case Finger2:
                return this.slot == ItemSlot.Finger;
            case Trinket1:
            case Trinket2:
                return this.slot == ItemSlot.Trinket;
            case MainHand:
                return this.slot == ItemSlot.TwoHand || this.slot == ItemSlot.OneHand || this.slot == ItemSlot.MainHand;
            case OffHand:
                return this.slot == ItemSlot.OneHand || this.slot == ItemSlot.OffHand;
            case Ranged:
                return this.slot == ItemSlot.Ranged;
            case Projectile:
                return this.slot == ItemSlot.Projectile;
            case ProjectileBag:
                return this.slot == ItemSlot.ProjectileBag;
            case Gems:
                return this.slot == ItemSlot.Red || this.slot == ItemSlot.Blue || this.slot == ItemSlot.Yellow
                        || this.slot == ItemSlot.Purple || this.slot == ItemSlot.Green || this.slot == ItemSlot.Orange
                        || this.slot == ItemSlot.Prismatic;
            case Metas:
                return this.slot == ItemSlot.Meta;
            default:
                return false;
        }
    }

    /**
     * Character-aware slot check: first asks the model ({@code Calculations}) whether the item
     * clashes with an already equipped unique item in the paired slot (the other finger, the
     * other trinket or the other weapon hand), then applies the plain structural
     * {@link #fitsInSlot(CharacterSlot)} test. The model call is skipped when
     * {@code ignoreUnique} is set, which the optimizer uses while generating candidates.
     */
    public boolean fitsInSlot(CharacterSlot charSlot, Character character) {
        return Calculations.itemFitsInSlot(this, character, charSlot, false);
    }

    public boolean fitsInSlot(CharacterSlot charSlot, Character character, boolean ignoreUnique) {
        return Calculations.itemFitsInSlot(this, character, charSlot, ignoreUnique);
    }

    /** Result of {@link #checkRequirements(Character)}: the answer plus its volatility flag. */
    public record RequirementCheck(boolean meets, boolean volatileRequirements) {
    }

    public boolean meetsRequirements(Character character) {
        return checkRequirements(character).meets();
    }

    /** Compatibility overload; prefer {@link #checkRequirements(Character)}. */
    public boolean meetsRequirements(Character character, boolean[] volatileRequirements) {
        RequirementCheck result = checkRequirements(character);
        volatileRequirements[0] = result.volatileRequirements();
        return result.meets();
    }

    /**
     * Gem requirement check with a volatility report. The record's
     * {@link RequirementCheck#volatileRequirements()} flag is true when the answer depends on the
     * character's other gems (meta gems, Jewelcrafter gems, unique gems), meaning the caller must
     * not cache stats derived from it. The {@link RequirementCheck#meets()} flag is true when this
     * item is a non-gem item, when the character is {@code null}, when gem enforcement is off, or
     * when the per-kind rule (meta color counts, at most three Jewelcrafter gems, at most one copy
     * of a unique gem) is satisfied. When the optimizer manages volatility itself
     * ({@link #isOptimizerManagedVolatiliy()}), the Jewelcrafter/unique gem checks are skipped
     * entirely.
     */
    RequirementCheck checkRequirements(Character character) {
        boolean volatileRequirements = false;
        boolean meetsRequirements = true;

        if (this.slot == ItemSlot.Meta) {
            volatileRequirements = true;
            if (character == null
                    || !character.getEnforceGemRequirements()) {
                return new RequirementCheck(true, volatileRequirements);
            }

            int redGems;
            int yellowGems;
            int blueGems;
            if (character != null) {
                redGems = character.getRedGemCount();
                yellowGems = character.getYellowGemCount();
                blueGems = character.getBlueGemCount();
            } else {
                redGems = 0;
                yellowGems = 0;
                blueGems = 0;
            }

            switch (this.id) {
                case 25899:
                case 25890:
                case 25901:
                case 32409:
                case 32410:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 2 && yellowGems >= 2 && blueGems >= 2;
                    break;
                case 41307:
                case 41401:
                case 41400:
                case 41375:
                case 44078:
                case 44089:
                case 41382:
                case 41398:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 1 && yellowGems >= 1 && blueGems >= 1;
                    break;
                case 25897:
                    volatileRequirements = true;
                    meetsRequirements = redGems > blueGems;
                    break;
                case 25895:
                    volatileRequirements = true;
                    meetsRequirements = redGems > yellowGems;
                    break;
                case 25893:
                case 32640:
                    volatileRequirements = true;
                    meetsRequirements = blueGems > yellowGems;
                    break;
                case 34220:
                case 41285:
                    volatileRequirements = true;
                    meetsRequirements = blueGems >= 2;
                    break;
                case 41376:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 2;
                    break;
                case 25896:
                case 44087:
                    volatileRequirements = true;
                    meetsRequirements = blueGems >= 3;
                    break;
                case 25898:
                    volatileRequirements = true;
                    meetsRequirements = blueGems >= 5;
                    break;
                case 32641:
                    volatileRequirements = true;
                    meetsRequirements = yellowGems == 3;
                    break;
                case 41333:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 3;
                    break;
                case 25894:
                case 28557:
                case 28556:
                case 41339:
                case 44076:
                    volatileRequirements = true;
                    meetsRequirements = yellowGems >= 2 && redGems >= 1;
                    break;
                case 35501:
                case 44088:
                    volatileRequirements = true;
                    meetsRequirements = yellowGems >= 1 && blueGems >= 2;
                    break;
                case 41378:
                case 44084:
                case 41381:
                    volatileRequirements = true;
                    meetsRequirements = yellowGems >= 2 && blueGems >= 1;
                    break;
                case 41380:
                case 41377:
                case 44082:
                case 41385:
                    volatileRequirements = true;
                    meetsRequirements = blueGems >= 2 && redGems >= 1;
                    break;
                case 41335:
                case 41389:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 2 && yellowGems >= 1;
                    break;
                case 41379:
                case 44081:
                case 41396:
                case 41395:
                    volatileRequirements = true;
                    meetsRequirements = redGems >= 2 && blueGems >= 1;
                    break;
                default:
                    meetsRequirements = true;
                    break;
            }
        } else if (!optimizerManagedVolatiliy) {
            if (this.isJewelersGem) {
                volatileRequirements = true;
                if (character == null
                        || !character.getEnforceGemRequirements()) {
                    return new RequirementCheck(true, volatileRequirements);
                }
                meetsRequirements = character.getJewelersGemCount() <= 3;
            } else if (this.unique) {
                volatileRequirements = true;
                if (character == null
                        || !character.getEnforceGemRequirements()) {
                    return new RequirementCheck(true, volatileRequirements);
                }
                meetsRequirements = character.getGemIdCount(this.id) <= 1;
            } else {
                volatileRequirements = false;
                meetsRequirements = true;
            }
        }

        return new RequirementCheck(meetsRequirements, volatileRequirements);
    }

    /**
     * Drops the sparse-data caches of {@link #getStats()} and {@link #getSocketBonus()} and
     * records the current time in {@link #getLastChange()}. {@link ItemInstance} compares that
     * timestamp against its own cached-total-stats time, so this is how a change to an item
     * template propagates to every equipped copy; {@link ItemCache#invalidateCachedStats()}
     * calls it for all cached items.
     */
    public void invalidateCachedData() {
        this.stats.invalidateSparseData();
        this.socketBonus.invalidateSparseData();
        lastChange = System.currentTimeMillis();
    }

    /**
     * Cache-only lookup: returns the registered item for the id, or {@code null} when the cache
     * has no such item. The original could fall back to downloading the item from the armory;
     * that network path is deliberately not ported (JRawr is offline).
     */
    public static Item loadFromId(int id) {
        return ItemCache.findItemById(id);
    }

    // C# Item.cs:122-125. There is no removal from the cache here; the item is only flagged invalid.
    /**
     * Marks this item invalid so cached references re-resolve (see {@link ItemInstance}) and
     * filters stop matching it. It does <b>not</b> remove the item from {@link ItemCache}; use
     * {@link ItemCache#deleteItem(Item)} for that, which flags it and removes it.
     */
    public void delete() {
        invalid = true;
    }

    // C# Item.cs:436-477, the reverse of Character.GetCharacterSlotByItemSlot.
    public static ItemSlot getItemSlotByCharacterSlot(CharacterSlot slot) {
        switch (slot) {
            case Projectile: return ItemSlot.Projectile;
            case Head: return ItemSlot.Head;
            case Neck: return ItemSlot.Neck;
            case Shoulders: return ItemSlot.Shoulders;
            case Chest: return ItemSlot.Chest;
            case Waist: return ItemSlot.Waist;
            case Legs: return ItemSlot.Legs;
            case Feet: return ItemSlot.Feet;
            case Wrist: return ItemSlot.Wrist;
            case Hands: return ItemSlot.Hands;
            case Finger1:
            case Finger2: return ItemSlot.Finger;
            case Trinket1:
            case Trinket2: return ItemSlot.Trinket;
            case Back: return ItemSlot.Back;
            case MainHand: return ItemSlot.MainHand;
            case OffHand: return ItemSlot.OffHand;
            case Ranged: return ItemSlot.Ranged;
            case ProjectileBag: return ItemSlot.ProjectileBag;
            case Tabard: return ItemSlot.Tabard;
            case Shirt: return ItemSlot.Shirt;
            case Gems: return ItemSlot.Prismatic;
            case Metas: return ItemSlot.Meta;
            default: return ItemSlot.None;
        }
    }

    // C# Item.cs:613-651 static constructor: every ItemSlot maps to CharacterSlot.None, then the
    // explicit entries override. SortedList<ItemSlot,...> iterates by key (enum value = ordinal),
    // which EnumMap preserves.
    private static final Map<ItemSlot, CharacterSlot> DEFAULT_SLOT_MAP = createDefaultSlotMap();

    private static Map<ItemSlot, CharacterSlot> createDefaultSlotMap() {
        Map<ItemSlot, CharacterSlot> map = new EnumMap<ItemSlot, CharacterSlot>(ItemSlot.class);
        for (ItemSlot itemSlot : ItemSlot.values()) {
            map.put(itemSlot, CharacterSlot.None);
        }
        map.put(ItemSlot.Head, CharacterSlot.Head);
        map.put(ItemSlot.Neck, CharacterSlot.Neck);
        map.put(ItemSlot.Shoulders, CharacterSlot.Shoulders);
        map.put(ItemSlot.Back, CharacterSlot.Back);
        map.put(ItemSlot.Chest, CharacterSlot.Chest);
        map.put(ItemSlot.Shirt, CharacterSlot.Shirt);
        map.put(ItemSlot.Tabard, CharacterSlot.Tabard);
        map.put(ItemSlot.Wrist, CharacterSlot.Wrist);
        map.put(ItemSlot.Hands, CharacterSlot.Hands);
        map.put(ItemSlot.Waist, CharacterSlot.Waist);
        map.put(ItemSlot.Legs, CharacterSlot.Legs);
        map.put(ItemSlot.Feet, CharacterSlot.Feet);
        map.put(ItemSlot.Finger, CharacterSlot.Finger1);
        map.put(ItemSlot.Trinket, CharacterSlot.Trinket1);
        map.put(ItemSlot.OneHand, CharacterSlot.MainHand);
        map.put(ItemSlot.TwoHand, CharacterSlot.MainHand);
        map.put(ItemSlot.MainHand, CharacterSlot.MainHand);
        map.put(ItemSlot.OffHand, CharacterSlot.OffHand);
        map.put(ItemSlot.Ranged, CharacterSlot.Ranged);
        map.put(ItemSlot.Projectile, CharacterSlot.Projectile);
        map.put(ItemSlot.ProjectileBag, CharacterSlot.ProjectileBag);
        return map;
    }

    public static Map<ItemSlot, CharacterSlot> getDefaultSlotMap() {
        return DEFAULT_SLOT_MAP;
    }
}
