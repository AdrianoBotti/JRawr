// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (UI/concurrency, reported): the whole Asynchronous Pattern region (ProgressChanged/
// Completed event args + delegates, BackgroundWorker/AsyncOperation/SendOrPostCallback, IsBusy
// async guards), the ReportProgress event posting (reportProgress is a no-op hook),
// ThreadPoolDirectUpgradeValuationSingleChanges/EffectiveMaxConcurrencyLevel/ThreadPoolValuation
// branches (single-changes valuation runs sequentially) and the Model setter's multithreading
// toggle. GetWarningPromptIfNeeded/CheckOneHandedWeaponUniqueness stay (pure string builders).
package rawr.base.optimizer;

import rawr.base.util.KeyValuePair;

import rawr.base.batch.BatchTools;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.calculations.OptimizerRangeValidatorBase;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.talents.TalentDataAttribute;
import rawr.base.talents.TalentsBase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main gear optimizer: it searches combinations of {@link ItemInstance}s (an item plus its
 * gems and enchant) across the 19 equipment slots of a {@link Character}.
 *
 * <p>One individual of the genetic algorithm is an {@link OptimizerCharacter}: a gene array whose
 * first 19 entries hold the chosen {@code ItemInstance} (or null for an empty slot) and, when the
 * corresponding options are enabled, four extra entries for a food buff, a battle elixir or flask,
 * a guardian elixir and a talent spec. The working {@link Character} object is rebuilt in place
 * between generations ({@link #supportsRecycling} is true), so an individual must be copied
 * explicitly when it is carried from one generation to the next.</p>
 *
 * <h2>Item pool and initialization</h2>
 *
 * <p>Before a search the optimizer must be initialized with the candidate items. The
 * {@code initializeItemCache} overloads either accept an already built
 * {@link AvailableItemGenerator} (the form batch tools use when one pool serves several characters)
 * or build one from a character's list of available-item strings plus the regem/reenchant, food,
 * elixir, mixology and talent options. The generator expands every available item into its possible
 * gemmed/enchanted instances and splits them per slot; the optimizer adds three "unique item"
 * validators (two identical unique items are forbidden in the finger pair, the trinket pair and
 * the main-hand/off-hand pair). Until initialization the searches throw.</p>
 *
 * <h2>Entry points</h2>
 *
 * <ul>
 *   <li>{@link #optimizeCharacter} runs the full search. With thoroughness 1 it first marks the
 *       character's currently equipped items as valid (so gear outside the pool is kept), then
 *       performs only the greedy direct-upgrade passes; with a higher thoroughness it runs the
 *       genetic algorithm and can inject the supplied character as a candidate.</li>
 *   <li>{@link #computeUpgrades} produces the per-slot upgrade lists shown in the item comparison
 *       charts: for every relevant item and every slot it fits, the item is locked into that slot
 *       (best gemming chosen by the direct-upgrade scan, then an optional full search) and kept
 *       only when the resulting objective beats the unequipped baseline.</li>
 *   <li>{@link #evaluateUpgrade} evaluates one concrete gemmed instance the same way and reports
 *       its value through a {@link ComparisonCalculationUpgrades}.</li>
 * </ul>
 *
 * <h2>Direct upgrades and greedy modes</h2>
 *
 * <p>When the GA stalls, the base class calls {@link #lookForDirectItemUpgrades}. The override
 * dispatches on {@link #greedyOptimizationMethod}: {@code AllCombinations}, plus any extra slot
 * such as food or talents, enumerates the full candidate list; {@code SingleChanges} walks the
 * per-item gem/enchant trees built by the generator (best child at each level) instead of the full
 * cartesian product; {@code GreedyBest} first finds the best gem per socket color, the best
 * jewelcrafter gem, the best meta gem and the best enchant by evaluating each in isolation through
 * the temporary {@link OptimizerCharacter#extraItem} slot, then constructs item instances from
 * those winners.</p>
 *
 * <h2>Objective value and requirements</h2>
 *
 * <p>The objective is the value named by {@code calculationToOptimize} &mdash; overall points, a
 * named sub-point, a talent or glyph count, or an optimizable value exposed by the model &mdash;
 * reduced by penalties: failing an {@link OptimizationRequirement} subtracts the shortfall and an
 * invalid gem requirement subtracts 100000 per condition, so an invalid individual cannot win while
 * a valid one exists.</p>
 *
 * <h2>Verification contract with the C# oracle</h2>
 *
 * <p>The golden verification tests do not require the Java search to pick the same item set as the
 * C# engine; search order, tie-breaking and threading legitimately differ. For a fixture both
 * engines receive the same fixed item pool and objective, the Java side additionally receives the
 * recorded seed ({@link #setRndSeed(int)}), and the test requires the Java best score to be at
 * least the C# best minus the documented tolerance. See {@code OptimizerVerificationTest} and
 * DECISIONS #58.</p>
 */
public class ItemInstanceOptimizer extends OptimizerBase<Object, OptimizerCharacter, CharacterCalculationsBase> {
    private Character character;
    private String calculationToOptimize;
    private List<OptimizationRequirement> requirements;
    private CalculationsBase model;

    private static final int characterSlots = 19;

    public GreedyOptimizationMethod greedyOptimizationMethod = GreedyOptimizationMethod.AllCombinations;

    /**
     * Rejects an individual whose paired slots (finger pair, trinket pair or weapon pair) hold the
     * same unique item twice, also honoring the item's unique-equip group list (an item may be
     * unique only together with certain other items). The check consults the unique flag and group
     * of the first item only, as in the ported original.
     */
    private static class UniqueItemValidator extends OptimizerRangeValidatorBase<Object> {
        @Override
        public boolean isValid(Object[] items) {
            if (items[startSlot] != null && items[endSlot] != null) {
                Item itema = ((ItemInstance) items[startSlot]).getItem();
                Item itemb = ((ItemInstance) items[endSlot]).getItem();
                return !(itema.isUnique() && (itema.getId() == itemb.getId() || (itema.getUniqueId() != null && itema.getUniqueId().contains(itemb.getId()))));
            }
            return true;
        }
    }

    private boolean optimizeFood;
    private boolean optimizeElixirs;
    private boolean optimizeTalents;
    private boolean mutateTalents;
    private boolean mixology;

    @SuppressWarnings("unchecked")
    public ItemInstanceOptimizer() {
        slotCount = characterSlots;
        slotItems = new List[characterSlots + 4];
        validators = new ArrayList<OptimizerRangeValidatorBase<Object>>();
        UniqueItemValidator fingerValidator = new UniqueItemValidator();
        fingerValidator.startSlot = CharacterSlot.Finger1.value();
        fingerValidator.endSlot = CharacterSlot.Finger2.value();
        UniqueItemValidator trinketValidator = new UniqueItemValidator();
        trinketValidator.startSlot = CharacterSlot.Trinket1.value();
        trinketValidator.endSlot = CharacterSlot.Trinket2.value();
        UniqueItemValidator weaponValidator = new UniqueItemValidator();
        weaponValidator.startSlot = CharacterSlot.MainHand.value();
        weaponValidator.endSlot = CharacterSlot.OffHand.value();
        validators.add(fingerValidator);
        validators.add(trinketValidator);
        validators.add(weaponValidator);

        slotItemsRandom = new List[characterSlots];
        minJeweler = new int[characterSlots];
        supportsRecycling = true;
    }

    /**
     * Initializes the candidate pools from an already built {@link AvailableItemGenerator}. This is
     * the preferred form for batch tools: the same generator instance can be reused across runs and
     * characters, and its availability data can be saved and restored around restriction passes.
     * The {@code character} supplies the base object new individuals are rebuilt from.
     */
    public void initializeItemCache(Character character, CalculationsBase model, AvailableItemGenerator itemGenerator) {
        this.character = character;
        setModel(model);
        slotCount = characterSlots;
        populateAvailableIds(itemGenerator);
    }

    /** Same as the twelve-argument form with {@code positiveCostItemsAvailable} false. */
    public void initializeItemCache(Character character, List<String> availableItems, boolean overrideRegem, boolean overrideReenchant, boolean templateGemsEnabled, CalculationsBase model, boolean optimizeFood, boolean optimizeElixirs, boolean mixology, List<TalentsBase> talentSpecs, boolean mutateTalents) {
        initializeItemCache(character, availableItems, overrideRegem, overrideReenchant, templateGemsEnabled, model, optimizeFood, optimizeElixirs, mixology, talentSpecs, mutateTalents, false);
    }

    /**
     * Initializes from a character's available-item strings. Food, battle-elixir/flask and
     * guardian-elixir buffs relevant to the model are collected into the three extra consume slots
     * (flasks are merged into the battle list because a flask conflicts with both elixir types);
     * when {@code talentSpecs} is non-empty the talent slot is enabled and the spec's annotated
     * talent methods are indexed for mutation. When any of the three features is on, the gene array
     * grows from 19 to 23 slots. {@code positiveCostItemsAvailable} additionally admits items that
     * have a cost (badge/vendor gear) even when the user did not select them.
     */
    public void initializeItemCache(Character character, List<String> availableItems, boolean overrideRegem, boolean overrideReenchant, boolean templateGemsEnabled, CalculationsBase model, boolean optimizeFood, boolean optimizeElixirs, boolean mixology, List<TalentsBase> talentSpecs, boolean mutateTalents, boolean positiveCostItemsAvailable) {
        this.character = character;
        setModel(model);

        List<Buff> food = new ArrayList<Buff>();
        List<Buff> battle = new ArrayList<Buff>();
        List<Buff> guardian = new ArrayList<Buff>();
        List<Buff> flask = new ArrayList<Buff>();
        for (Buff buff : Buff.getAllBuffs()) {
            if (!model.isBuffRelevant(buff, character)) continue;
            if ("Elixirs and Flasks".equals(buff.group)) {
                boolean isbattle = buff.getConflictingBuffs().contains("Battle Elixir");
                boolean isguardian = buff.getConflictingBuffs().contains("Guardian Elixir");
                if (isbattle && isguardian) {
                    flask.add(buff);
                } else if (isbattle) {
                    battle.add(buff);
                } else if (isguardian) {
                    guardian.add(buff);
                }
            } else if ("Food".equals(buff.group)) {
                food.add(buff);
            }
        }
        food = AvailableItemGenerator.filterBuffs(food);
        battle = AvailableItemGenerator.filterBuffs(battle);
        guardian = AvailableItemGenerator.filterBuffs(guardian);
        flask = AvailableItemGenerator.filterBuffs(flask);
        battle.addAll(flask);
        slotItems[characterSlots] = new ArrayList<Object>(food);
        slotItems[characterSlots + 1] = new ArrayList<Object>(battle);
        slotItems[characterSlots + 2] = new ArrayList<Object>(guardian);

        this.optimizeFood = optimizeFood;
        this.optimizeElixirs = optimizeElixirs;
        this.mixology = mixology;
        this.optimizeTalents = (talentSpecs != null && talentSpecs.size() > 0);
        this.mutateTalents = mutateTalents;

        if (optimizeTalents) {
            slotItems[characterSlots + 3] = new ArrayList<Object>(talentSpecs);
            talentItem = new TalentItem[100];
            TalentsBase talents = talentSpecs.get(0);
            for (Method method : talents.getClass().getMethods()) {
                TalentDataAttribute td = method.getAnnotation(TalentDataAttribute.class);
                if (td != null) {
                    talentItem[td.index()] = new TalentItem(td);
                    if (td.index() + 1 > talentItemCount) {
                        talentItemCount = td.index() + 1;
                    }
                }
            }
            for (int i = 0; i < talentItemCount; i++) {
                if (talentItem[i].talentData.prerequisite() >= 0) {
                    talentItem[talentItem[i].talentData.prerequisite()].childList.add(i);
                }
            }
        } else {
            slotItems[characterSlots + 3] = new ArrayList<Object>();
        }

        if (optimizeFood || optimizeElixirs || optimizeTalents) {
            slotCount = characterSlots + 4;
        } else {
            slotCount = characterSlots;
        }

        populateAvailableIds(availableItems, templateGemsEnabled, overrideRegem, overrideReenchant, positiveCostItemsAvailable);
    }

    public CalculationsBase getModel() {
        return model;
    }

    public void setModel(CalculationsBase model) {
        this.model = model;
    }

    /**
     * Initializes the pools directly from a list of gemmed item instances, sorting each instance
     * into every slot it fits. A null candidate is added to finger, trinket, main-hand and
     * off-hand (and to any empty slot) so the search can leave those slots bare. Also builds the
     * random-sampling index and the pair-slot map.
     */
    @SuppressWarnings("unchecked")
    public void initializeItemCache(List<ItemInstance> availableItems) {
        List<ItemInstance>[] slotItems = new List[characterSlots];
        for (int i = 0; i < slotCount; i++) {
            slotItems[i] = new ArrayList<ItemInstance>();
        }

        for (ItemInstance gemmedItem : availableItems) {
            if (gemmedItem != null) {
                Item item = gemmedItem.getItem();
                if (item != null) {
                    for (int i = 0; i < slotCount; i++) {
                        if (item.fitsInSlot(CharacterSlot.fromValue(i), this.character)) slotItems[i].add(gemmedItem);
                    }
                }
            }
        }

        for (int i = 0; i < slotCount; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            if (slot == CharacterSlot.Finger1 || slot == CharacterSlot.Finger2 || slot == CharacterSlot.Trinket1 || slot == CharacterSlot.Trinket2 || slot == CharacterSlot.MainHand || slot == CharacterSlot.OffHand || slotItems[i].size() == 0) {
                slotItems[i].add(null);
            }
        }

        for (int i = 0; i < slotCount; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            if (slot != CharacterSlot.Finger1 && slot != CharacterSlot.Finger2 && slot != CharacterSlot.Trinket1 && slot != CharacterSlot.Trinket2) {
                slotItems[i] = AvailableItemGenerator.filterItemInstances(slotItems[i], false);
            }
        }

        for (int i = 0; i < slotCount; i++) {
            this.slotItems[i] = new ArrayList<Object>(slotItems[i]);
        }

        itemAvailable = new LinkedHashMap<String, Boolean>();

        // populate the list for random sampling
        for (int slot = 0; slot < slotCount; slot++) {
            slotItemsRandom[slot] = new ArrayList<KeyedList<KeyedList<ItemInstance>>>();
            for (ItemInstance itemInstance : slotItems[slot]) {
                String gemmedId = (itemInstance == null) ? "0.0.0.0.0" : itemInstance.getGemmedId();
                String key1 = (itemInstance == null) ? "0" : Integer.toString(itemInstance.getId());
                String key2 = (itemInstance == null) ? "0" : Integer.toString(itemInstance.getEnchantId());
                itemAvailable.put(gemmedId, true);
                KeyedList<KeyedList<ItemInstance>> list1 = findKeyed(slotItemsRandom[slot], key1);
                if (list1 == null) {
                    list1 = new KeyedList<KeyedList<ItemInstance>>();
                    list1.key = key1;
                    slotItemsRandom[slot].add(list1);
                }
                KeyedList<ItemInstance> list2 = findKeyed(list1, key2);
                if (list2 == null) {
                    list2 = new KeyedList<ItemInstance>();
                    list2.key = key2;
                    list1.add(list2);
                }
                list2.add(itemInstance);
            }
        }

        pairSlotMap = new int[slotCount];
        pairSlotMap[CharacterSlot.Back.value()] = -1;
        pairSlotMap[CharacterSlot.Chest.value()] = -1;
        pairSlotMap[CharacterSlot.Feet.value()] = -1;
        pairSlotMap[CharacterSlot.Finger1.value()] = CharacterSlot.Finger2.value();
        pairSlotMap[CharacterSlot.Finger2.value()] = CharacterSlot.Finger1.value();
        pairSlotMap[CharacterSlot.Hands.value()] = -1;
        pairSlotMap[CharacterSlot.Head.value()] = -1;
        pairSlotMap[CharacterSlot.Legs.value()] = -1;
        pairSlotMap[CharacterSlot.MainHand.value()] = CharacterSlot.OffHand.value();
        pairSlotMap[CharacterSlot.OffHand.value()] = CharacterSlot.MainHand.value();
        pairSlotMap[CharacterSlot.Neck.value()] = -1;
        pairSlotMap[CharacterSlot.Projectile.value()] = -1;
        pairSlotMap[CharacterSlot.ProjectileBag.value()] = -1;
        pairSlotMap[CharacterSlot.Ranged.value()] = -1;
        pairSlotMap[CharacterSlot.Shoulders.value()] = -1;
        pairSlotMap[CharacterSlot.Trinket1.value()] = CharacterSlot.Trinket2.value();
        pairSlotMap[CharacterSlot.Trinket2.value()] = CharacterSlot.Trinket1.value();
        pairSlotMap[CharacterSlot.Waist.value()] = -1;
        pairSlotMap[CharacterSlot.Wrist.value()] = -1;

        itemCacheInitialized = true;
    }

    private boolean isBusy;

    public boolean isBusy() {
        return isBusy;
    }

    /**
     * Port of the {@code injected} out-parameter of the original asynchronous completion event
     * (OptimizeCharacterCompletedEventArgs.CurrentCharacterInjected); BatchTools reads it to decide
     * when an optimization round has converged.
     */
    private boolean lastInjected;

    public boolean getLastInjected() {
        return lastInjected;
    }

    /** UI progress reporting omitted; kept as the optimizer hook (reported). */
    @Override
    protected void reportProgress(int progressPercentage, float bestValue) {
    }

    /**
     * Runs the optimization and returns the best character found, or null on cancellation. The
     * character argument is the baseline; with {@code injectCharacter} (or thoroughness 1) it also
     * enters the search as a candidate and {@link #getLastInjected()} reports whether it was
     * adopted. Throws when another operation is running or the item cache was never initialized.
     */
    public Character optimizeCharacter(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, boolean injectCharacter) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        Character optimizedCharacter = privateOptimizeCharacter(character, calculationToOptimize, requirements, thoroughness, injectCharacter, error);
        if (error[0] != null) throw new RuntimeException(error[0]);
        isBusy = false;
        return optimizedCharacter;
    }

    private Character privateOptimizeCharacter(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, boolean injectCharacter, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.character = character;
        setModel(Calculations.getModel(character.getCurrentModel()));
        this.calculationToOptimize = calculationToOptimize;
        this.requirements = requirements;
        this.thoroughness = thoroughness;

        Character optimizedCharacter = null;
        OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> result = null;
        lockedSlot = CharacterSlot.None;

        try {
            if (this.thoroughness == 1) {
                // if we just start from current character and look for direct upgrades
                // then we have to deal with items that are currently equipped, but are not
                // currently available
                markEquippedItemsAsValid(this.character);
            }

            result = (injectCharacter || this.thoroughness == 1)
                    ? optimize(new OptimizerCharacter(character, optimizeFood, optimizeElixirs, optimizeTalents))
                    : optimize();
            OptimizerCharacter optCharacter = result != null ? result.individual() : null;
            optimizedCharacter = optCharacter != null ? optCharacter.character : null;
        } catch (Exception ex) {
            error[0] = ex;
        }

        lastInjected = result != null && result.injected();
        reportProgress(100, result != null ? result.bestValue() : 0f);
        return optimizedCharacter;
    }

    /**
     * Computes the per-slot upgrade chart: for every relevant item (or the single {@code
     * singleItemUpgrades} item) that fits a slot, runs the search with that item locked into the
     * slot and records the gain over the character's own value. Returns an insertion-ordered map
     * keyed by the 17 optimizable slots, or null when cancelled. One evaluation pool spans the
     * whole scan; the item cache must already be initialized.
     */
    public Map<CharacterSlot, List<ComparisonCalculationUpgrades>> computeUpgrades(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, Item singleItemUpgrades) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        // One evaluation pool for the whole upgrade scan (the direct gemming upgrade below and the
        // per-item optimize runs); shut down before returning.
        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = withEvaluationPool(
                () -> privateComputeUpgrades(character, calculationToOptimize, requirements, thoroughness, singleItemUpgrades, error));
        if (error[0] != null) throw new RuntimeException(error[0]);
        isBusy = false;
        return upgrades;
    }

    private int itemProgressPercentage = 0;
    private String currentItem = "";

    /**
     * Adds the character's currently equipped gemmed instances to the pools when they are missing,
     * so that a search starting from the current gear cannot be forced to give it up just because
     * the user did not mark it available.
     */
    private void markEquippedItemsAsValid(Character character) {
        for (int i = 0; i < characterSlots; i++) {
            ItemInstance item = character.get(CharacterSlot.fromValue(i));
            if (item != null && item.getId() != 0 && !slotItems[i].contains(item)) {
                slotItems[i].add(item);
                itemAvailable.put(item.getGemmedId(), true);
                KeyedList<KeyedList<ItemInstance>> list1 = findKeyed(slotItemsRandom[i], Integer.toString(item.getId()));
                if (list1 == null) {
                    list1 = new KeyedList<KeyedList<ItemInstance>>();
                    list1.key = Integer.toString(item.getId());
                    slotItemsRandom[i].add(list1);
                }
                KeyedList<ItemInstance> list2 = findKeyed(list1, Integer.toString(item.getEnchantId()));
                if (list2 == null) {
                    list2 = new KeyedList<ItemInstance>();
                    list2.key = Integer.toString(item.getEnchantId());
                    list1.add(list2);
                }
                list2.add(item);
            }
        }
    }

    private Map<CharacterSlot, List<ComparisonCalculationUpgrades>> privateComputeUpgrades(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, Item singleItemUpgrades, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.character = character;
        setModel(Calculations.getModel(character.getCurrentModel()));
        this.calculationToOptimize = calculationToOptimize;
        this.requirements = requirements;
        this.thoroughness = thoroughness;

        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = null;
        try {
            // make equipped gear/enchant valid
            markEquippedItemsAsValid(this.character);

            upgrades = new LinkedHashMap<CharacterSlot, List<ComparisonCalculationUpgrades>>();

            Item[] items = ItemCache.getRelevantItems(model, this.character.getRace());
            CharacterSlot[] slots = new CharacterSlot[] { CharacterSlot.Back, CharacterSlot.Chest, CharacterSlot.Feet, CharacterSlot.Finger1, CharacterSlot.Hands, CharacterSlot.Head, CharacterSlot.Legs, CharacterSlot.MainHand, CharacterSlot.Neck, CharacterSlot.OffHand, CharacterSlot.Projectile, CharacterSlot.ProjectileBag, CharacterSlot.Ranged, CharacterSlot.Shoulders, CharacterSlot.Trinket1, CharacterSlot.Waist, CharacterSlot.Wrist };
            for (CharacterSlot slot : slots)
                upgrades.put(slot, new ArrayList<ComparisonCalculationUpgrades>());

            CharacterCalculationsBase baseCalculations = model.getCharacterCalculations(this.character);
            float baseValue = getOptimizationValue(this.character, baseCalculations, calculationToOptimize, requirements);
            Map<Integer, Item> itemById = new LinkedHashMap<Integer, Item>();
            for (Item item : items) {
                itemById.put(item.getId(), item);
            }

            if (singleItemUpgrades != null) {
                items = new Item[] { singleItemUpgrades };
            } else {
                items = itemById.values().toArray(new Item[0]);
            }

            OptimizerCharacter __baseCharacter = new OptimizerCharacter(this.character, optimizeFood, optimizeElixirs, optimizeTalents);
            OptimizerCharacter __character;
            for (int i = 0; i < items.length; i++) {
                Item item = items[i];
                currentItem = item.getName();
                itemProgressPercentage = (int) Math.rint((float) i / ((float) items.length / 100f));
                if (cancellationPending) {
                    return null;
                }
                reportProgress(0, 0);
                for (CharacterSlot slot : slots) {
                    if (item.fitsInSlot(slot, this.character, true)) {
                        List<ComparisonCalculationUpgrades> comparisons = upgrades.get(slot);
                        populateLockedItems(item);
                        lockedSlot = slot;
                        List<Object> savedItems = slotItems[lockedSlot.value()];
                        slotItems[lockedSlot.value()] = lockedItems;
                        if (lockedSlot == CharacterSlot.Finger1 && Item.itemsAreConsideredUniqueEqual(this.character.get(CharacterSlot.Finger2).getItem(), item)) {
                            lockedSlot = CharacterSlot.Finger2;
                        }
                        if (lockedSlot == CharacterSlot.Trinket1 && Item.itemsAreConsideredUniqueEqual(this.character.get(CharacterSlot.Trinket2).getItem(), item)) {
                            lockedSlot = CharacterSlot.Trinket2;
                        }
                        __character = buildSingleItemSwapIndividual(__baseCharacter, lockedSlot.value(), lockedItems.get(0));
                        if (lockedSlot == CharacterSlot.MainHand && this.character.get(CharacterSlot.OffHand) != null && Item.itemsAreConsideredUniqueEqual(this.character.get(CharacterSlot.OffHand).getItem(), item)) {
                            // can't dual wield unique items, so make the other slot empty
                            __character = buildSingleItemSwapIndividual(__character, CharacterSlot.OffHand.value(), null);
                        }
                        if (lockedSlot == CharacterSlot.OffHand && Item.itemsAreConsideredUniqueEqual(this.character.get(CharacterSlot.MainHand).getItem(), item)) {
                            // can't dual wield unique items, so make the other slot empty
                            __character = buildSingleItemSwapIndividual(__character, CharacterSlot.MainHand.value(), null);
                        }
                        // instead of just putting in the first gemming on the list select the best one
                        float best = NO_SEARCH_SCORE;
                        Character bestCharacter;
                        if (lockedItems.size() > 1) {
                            OptimizerCharacter directUpgradeCharacter = lookForDirectItemUpgrades(lockedItems, lockedSlot.value(), best, __character, null).individual();
                            if (directUpgradeCharacter != null) {
                                __character = directUpgradeCharacter;
                            }
                        }
                        if (this.thoroughness > 1) {
                            int saveThoroughness = this.thoroughness;
                            this.thoroughness = 1;
                            OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> injected = optimize(__character, 0f);
                            this.thoroughness = saveThoroughness;
                            OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> result = optimize(injected.individual(), injected.bestValue());
                            best = result.bestValue();
                            OptimizerCharacter OC = result.individual();
                            if (null == OC) {
                                // Optimize can return null, but none of the calls that depend on bestCharacter
                                // can handle bestCharacter when it is null.
                                bestCharacter = new Character();
                            } else {
                                bestCharacter = OC.character;
                            }
                        } else {
                            OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> result = optimize(__character, 0f);
                            bestCharacter = result.individual().character;
                            best = result.bestValue();
                        }
                        if (best > baseValue) {
                            ItemInstance bestItem = bestCharacter.get(lockedSlot);
                            ComparisonCalculationUpgrades itemCalc = new ComparisonCalculationUpgrades();
                            itemCalc.setItemInstance(bestItem);
                            itemCalc.setCharacterItems(bestCharacter.getItems());
                            itemCalc.setName(item.getName());
                            itemCalc.setEquipped(false);
                            itemCalc.setOverallPoints(best - baseValue);

                            comparisons.add(itemCalc);
                        }
                        slotItems[slot.value()] = savedItems;
                    }
                }
            }
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, 0f);
        return upgrades;
    }

    /**
     * Evaluates one concrete gemmed instance as a replacement in every slot it fits and returns the
     * gain over the character's value; {@code comparisonUpgrade[0]} receives the matching
     * {@link ComparisonCalculationUpgrades} (or stays null). A negative gain for an item that is
     * not the one currently equipped is clamped to zero. Throws when the item cache is not
     * initialized or another operation is running.
     */
    public float evaluateUpgrade(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, ItemInstance upgrade, ComparisonCalculationUpgrades[] comparisonUpgrade) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        float upgradeValue = privateEvaluateUpgrade(character, calculationToOptimize, requirements, thoroughness, upgrade, error, comparisonUpgrade);
        if (error[0] != null) throw new RuntimeException(error[0]);
        isBusy = false;
        return upgradeValue;
    }

    private float privateEvaluateUpgrade(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, ItemInstance upgrade, Exception[] error, ComparisonCalculationUpgrades[] comparisonUpgrade) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.character = character;
        setModel(Calculations.getModel(character.getCurrentModel()));
        this.calculationToOptimize = calculationToOptimize;
        this.requirements = requirements;
        this.thoroughness = thoroughness;

        comparisonUpgrade[0] = null;
        float upgradeValue = 0f;
        try {
            // make equipped gear/enchant valid
            // this is currently only called after calculate upgrades already marks items as valid, but we might have to do this here also if things change
            // MarkEquippedItemsAsValid(_character);

            CharacterSlot[] slots = new CharacterSlot[] { CharacterSlot.Back, CharacterSlot.Chest, CharacterSlot.Feet, CharacterSlot.Finger1, CharacterSlot.Hands, CharacterSlot.Head, CharacterSlot.Legs, CharacterSlot.MainHand, CharacterSlot.Neck, CharacterSlot.OffHand, CharacterSlot.Projectile, CharacterSlot.ProjectileBag, CharacterSlot.Ranged, CharacterSlot.Shoulders, CharacterSlot.Trinket1, CharacterSlot.Waist, CharacterSlot.Wrist };
            CharacterCalculationsBase baseCalculations = model.getCharacterCalculations(this.character);
            float baseValue = getOptimizationValue(this.character, baseCalculations, calculationToOptimize, requirements);

            OptimizerCharacter __baseCharacter = new OptimizerCharacter(this.character, optimizeFood, optimizeElixirs, optimizeTalents);
            OptimizerCharacter __character;
            ItemInstance item = upgrade;
            for (CharacterSlot slot : slots) {
                if (item.getItem().fitsInSlot(slot, this.character, true)) {
                    lockedItems = new ArrayList<Object>();
                    lockedItems.add(item);
                    lockedSlot = slot;
                    if (lockedSlot == CharacterSlot.Finger1 && item.getItem().isUnique() && this.character.get(CharacterSlot.Finger2) != null && (this.character.get(CharacterSlot.Finger2).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.Finger2).getId())))) {
                        lockedSlot = CharacterSlot.Finger2;
                    }
                    if (lockedSlot == CharacterSlot.Trinket1 && item.getItem().isUnique() && this.character.get(CharacterSlot.Trinket2) != null && (this.character.get(CharacterSlot.Trinket2).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.Trinket2).getId())))) {
                        lockedSlot = CharacterSlot.Trinket2;
                    }
                    List<Object> savedItems = slotItems[lockedSlot.value()];
                    slotItems[lockedSlot.value()] = lockedItems;
                    __character = buildSingleItemSwapIndividual(__baseCharacter, lockedSlot.value(), upgrade);
                    if (lockedSlot == CharacterSlot.MainHand && item.getItem().isUnique() && this.character.get(CharacterSlot.OffHand) != null && (this.character.get(CharacterSlot.OffHand).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.OffHand).getId())))) {
                        // can't dual wield unique items, so make the other slot empty
                        __character = buildSingleItemSwapIndividual(__character, CharacterSlot.OffHand.value(), null);
                    }
                    if (lockedSlot == CharacterSlot.OffHand && item.getItem().isUnique() && this.character.get(CharacterSlot.MainHand) != null && (this.character.get(CharacterSlot.MainHand).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.MainHand).getId())))) {
                        // can't dual wield unique items, so make the other slot empty
                        __character = buildSingleItemSwapIndividual(__character, CharacterSlot.MainHand.value(), null);
                    }
                    float best;
                    Character bestCharacter;
                    if (this.thoroughness > 1) {
                        int saveThoroughness = this.thoroughness;
                        this.thoroughness = 1;
                        OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> injected = optimize(__character, 0f);
                        this.thoroughness = saveThoroughness;
                        OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> result = optimize(injected.individual(), injected.bestValue());
                        bestCharacter = result.individual().character;
                        best = result.bestValue();
                    } else {
                        OptimizationResult<OptimizerCharacter, CharacterCalculationsBase> result = optimize(__character, 0f);
                        bestCharacter = result.individual().character;
                        best = result.bestValue();
                    }
                    if (bestCharacter.get(lockedSlot) == null || bestCharacter.get(lockedSlot).getId() != item.getId()) throw new RuntimeException("There was an internal error in Optimizer when evaluating upgrade.");
                    upgradeValue = best - baseValue;
                    if (upgradeValue < 0 && (this.character.get(lockedSlot) == null || this.character.get(lockedSlot).getId() != item.getId())) upgradeValue = 0f;

                    comparisonUpgrade[0] = new ComparisonCalculationUpgrades();
                    comparisonUpgrade[0].setItemInstance(upgrade);
                    comparisonUpgrade[0].setCharacterItems(bestCharacter.getItems());
                    comparisonUpgrade[0].setName(upgrade.getItem().getName());
                    comparisonUpgrade[0].setEquipped(false);
                    comparisonUpgrade[0].setOverallPoints(upgradeValue);

                    slotItems[lockedSlot.value()] = savedItems;
                }
            }
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, 0f);
        return upgradeValue;
    }

    private boolean itemCacheInitialized;

    private int[] pairSlotMap;
    private Map<String, Boolean> itemAvailable = new LinkedHashMap<String, Boolean>();

    private static class KeyedList<T> extends ArrayList<T> {
        public String key;
    }

    private static <T> KeyedList<T> findKeyed(List<? extends KeyedList<T>> list, String key) {
        for (KeyedList<T> entry : list) {
            if (key.equals(entry.key)) return entry;
        }
        return null;
    }

    private List<KeyedList<KeyedList<ItemInstance>>>[] slotItemsRandom;
    private int[] minJeweler;
    private List<Object> lockedItems;
    private CharacterSlot lockedSlot = CharacterSlot.None;
    private AvailableItemGenerator itemGenerator;

    public AvailableItemGenerator getItemGenerator() {
        return itemGenerator;
    }

    private void populateLockedItems(Item item) {
        lockedItems = new ArrayList<Object>(itemGenerator.getPossibleGemmedItemsForItem(item, Integer.toString(item.getId())));
    }

    private void populateAvailableIds(List<String> availableItems, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant) {
        populateAvailableIds(availableItems, templateGemsEnabled, overrideRegem, overrideReenchant, false);
    }

    private void populateAvailableIds(List<String> availableItems, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant, boolean positiveCostItemsAvailable) {
        populateAvailableIds(new AvailableItemGenerator(availableItems, greedyOptimizationMethod != GreedyOptimizationMethod.AllCombinations, templateGemsEnabled, overrideRegem, overrideReenchant, true, positiveCostItemsAvailable, character, model));
    }

    private void populateAvailableIds(AvailableItemGenerator itemGenerator) {
        this.itemGenerator = itemGenerator;
        for (int slot = 0; slot < characterSlots; slot++) {
            slotItems[slot] = new ArrayList<Object>(itemGenerator.getSlotItems()[slot]);
        }

        itemAvailable = itemGenerator.getItemAvailable();

        // populate the list for random sampling
        for (int slot = 0; slot < characterSlots; slot++) {
            slotItemsRandom[slot] = new ArrayList<KeyedList<KeyedList<ItemInstance>>>();
            int minJeweler = slotItems[slot].size() > 0 ? 3 : 0;
            for (Object o : slotItems[slot]) {
                ItemInstance itemInstance = (ItemInstance) o;
                // jeweler data
                int jewelerCount = itemInstance == null ? 0 : itemInstance.getJewelerCount();
                if (jewelerCount < minJeweler) {
                    minJeweler = jewelerCount;
                }

                // random sampling
                String gemmedId = (itemInstance == null) ? "0.0.0.0.0" : itemInstance.getGemmedId();
                String key1 = (itemInstance == null) ? "0" : Integer.toString(itemInstance.getId());
                String key2 = (itemInstance == null) ? "0" : Integer.toString(itemInstance.getEnchantId());
                KeyedList<KeyedList<ItemInstance>> list1 = findKeyed(slotItemsRandom[slot], key1);
                if (list1 == null) {
                    list1 = new KeyedList<KeyedList<ItemInstance>>();
                    list1.key = key1;
                    slotItemsRandom[slot].add(list1);
                }
                KeyedList<ItemInstance> list2 = findKeyed(list1, key2);
                if (list2 == null) {
                    list2 = new KeyedList<ItemInstance>();
                    list2.key = key2;
                    list1.add(list2);
                }
                list2.add(itemInstance);
            }

            this.minJeweler[slot] = minJeweler;
        }

        pairSlotMap = new int[characterSlots];
        pairSlotMap[CharacterSlot.Back.value()] = -1;
        pairSlotMap[CharacterSlot.Chest.value()] = -1;
        pairSlotMap[CharacterSlot.Feet.value()] = -1;
        pairSlotMap[CharacterSlot.Finger1.value()] = CharacterSlot.Finger2.value();
        pairSlotMap[CharacterSlot.Finger2.value()] = CharacterSlot.Finger1.value();
        pairSlotMap[CharacterSlot.Hands.value()] = -1;
        pairSlotMap[CharacterSlot.Head.value()] = -1;
        pairSlotMap[CharacterSlot.Legs.value()] = -1;
        pairSlotMap[CharacterSlot.MainHand.value()] = CharacterSlot.OffHand.value();
        pairSlotMap[CharacterSlot.OffHand.value()] = CharacterSlot.MainHand.value();
        pairSlotMap[CharacterSlot.Neck.value()] = -1;
        pairSlotMap[CharacterSlot.Projectile.value()] = -1;
        pairSlotMap[CharacterSlot.ProjectileBag.value()] = -1;
        pairSlotMap[CharacterSlot.Ranged.value()] = -1;
        pairSlotMap[CharacterSlot.Shoulders.value()] = -1;
        pairSlotMap[CharacterSlot.Trinket1.value()] = CharacterSlot.Trinket2.value();
        pairSlotMap[CharacterSlot.Trinket2.value()] = CharacterSlot.Trinket1.value();
        pairSlotMap[CharacterSlot.Waist.value()] = -1;
        pairSlotMap[CharacterSlot.Wrist.value()] = -1;

        itemCacheInitialized = true;
    }

    /**
     * Returns a warning prompt for empty or oversized candidate pools (per-slot item counts above
     * 512, gem counts above 12, enchant counts above 8), or null when the pools look reasonable.
     * Pure string building; the caller decides whether to ask the user to continue.
     */
    public String getWarningPromptIfNeeded() {
        int gemLimit = 12;
        int itemLimit = 512;
        int enchantLimit = 8;

        List<String> emptyList = new ArrayList<String>();
        List<String> tooManyList = new ArrayList<String>();

        calculateWarnings(itemGenerator.getGemItems(), "Gems", emptyList, tooManyList, gemLimit);
        calculateWarnings(itemGenerator.getMetaGemItems(), "Meta Gems", emptyList, tooManyList, gemLimit);

        calculateWarnings(slotItems[CharacterSlot.Head.value()], "Head Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Neck.value()], "Neck Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Shoulders.value()], "Shoulder Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Back.value()], "Back Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Chest.value()], "Chest Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Wrist.value()], "Wrist Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Hands.value()], "Hands Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Waist.value()], "Waist Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Legs.value()], "Legs Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Feet.value()], "Feet Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Finger1.value()], "Finger Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Trinket1.value()], "Trinket Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.MainHand.value()], "Main Hand Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.OffHand.value()], "Offhand Items", null, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Ranged.value()], "Ranged Items", null, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.Projectile.value()], "Projectile Items", null, tooManyList, itemLimit);
        calculateWarnings(slotItems[CharacterSlot.ProjectileBag.value()], "Projectile Bag Items", null, tooManyList, itemLimit);

        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Back.value()], "Back Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Chest.value()], "Chest Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Feet.value()], "Feet Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Finger1.value()], "Finger Enchants", null, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Hands.value()], "Hands Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Head.value()], "Head Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Legs.value()], "Legs Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Shoulders.value()], "Shoulder Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.MainHand.value()], "Main Hand Enchants", emptyList, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.OffHand.value()], "Offhand Enchants", null, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Ranged.value()], "Ranged Enchants", null, null, enchantLimit);
        calculateWarnings(itemGenerator.getSlotEnchants()[CharacterSlot.Wrist.value()], "Wrist Enchants", emptyList, null, enchantLimit);

        if (emptyList.size() + tooManyList.size() > 0) {
            if (emptyList.size() > 5) {
                emptyList.subList(5, emptyList.size()).clear();
                emptyList.add("...");
            }
            if (tooManyList.size() > 5) {
                tooManyList.subList(5, tooManyList.size()).clear();
                tooManyList.add("...");
            }
            if (tooManyList.size() == 0) {
                // good sizes but some are empty
                return "You have not selected any of the following:\r\n\r\n\t"
                        + String.join("\r\n\t", emptyList)
                        + "\r\n\r\nTo select the items, go to their related slot charts and mark them with a Green Diamond. For more information, see the Rawr Documenation pages at rawr.codeplex.com"
                        + "\r\n\r\nDo you want to continue with the optimization?";
            } else if (emptyList.size() == 0) {
                return "The following slots have a very large number of items selected:\r\n\r\n\t"
                        + String.join("\r\n\t", tooManyList)
                        + "\r\n\r\nDo you want to continue with the optimization?";
            } else {
                return "You have not selected any of the following:\r\n\r\n\t"
                        + String.join("\r\n\t", emptyList)
                        + "\r\n\r\nTo select the items, go to their related slot charts and mark them with a Green Diamond. For more information, see the Rawr Documenation pages at rawr.codeplex.com"
                        + "\r\n\r\nThe following slots have a very large number of items selected:\r\n\r\n\t"
                        + String.join("\r\n\t", tooManyList)
                        + "\r\n\r\nDo you want to continue with the optimization?";
            }
        }
        return null;
    }

    // C# signature takes System.Collections.IList; Java accepts List or array.
    private void calculateWarnings(Object listObj, String group, List<String> emptyList, List<String> tooManyList, int tooManyLimit) {
        int count;
        Object el0;
        if (listObj instanceof List<?> list) {
            count = list.size();
            el0 = count > 0 ? list.get(0) : null;
        } else {
            Object[] array = (Object[]) listObj;
            count = array.length;
            el0 = count > 0 ? array[0] : null;
        }
        if (emptyList != null && (count == 0 || (count == 1 && (el0 == null || (el0 instanceof Enchant enchant && enchant.id == 0))))) emptyList.add(group);
        if (tooManyList != null && count > tooManyLimit) tooManyList.add(group);
    }

    /**
     * Warns when a non-unique one-handed weapon is available for both the main hand and the off
     * hand, because the optimizer would then implicitly assume the player owns two copies. Returns
     * null when the setup is unambiguous.
     */
    public String checkOneHandedWeaponUniqueness() {
        boolean nonUniqueOneHander = false;
        for (Object o : slotItems[CharacterSlot.MainHand.value()]) {
            ItemInstance item = (ItemInstance) o;
            if (item != null && item.getItem() != null) {
                if ((item.getItem().getType() == ItemType.OneHandAxe || item.getItem().getType() == ItemType.OneHandMace
                        || item.getItem().getType() == ItemType.OneHandSword || item.getItem().getType() == ItemType.FistWeapon) && item.getItem().getSlot() == ItemSlot.OneHand && !item.getItem().isUnique()) {
                    if (slotItems[CharacterSlot.OffHand.value()].contains(item)) {
                        nonUniqueOneHander = true;
                        break;
                    }
                }
            }
        }
        return nonUniqueOneHander ? "You have a one-handed weapon marked available that is not unique." + System.lineSeparator() +
                "The optimizer will assume you have two of these until you mark it unique." + System.lineSeparator() + System.lineSeparator() +
                "Do you want to continue and let the optimizer assume you have two available?"
                : null;
    }

    public static float getOptimizationValue(Character character, CalculationsBase model) {
        float[] ignore = new float[1];
        return getCalculationsValue(character, model.getCharacterCalculations(character), character.getCalculationToOptimize(), character.getOptimizationRequirements(), ignore);
    }

    public static float getOptimizationValue(Character character, CalculationsBase model, boolean referenceCalculation) {
        float[] ignore = new float[1];
        return getCalculationsValue(character, model.getCharacterCalculations(character, null, referenceCalculation, false, false), character.getCalculationToOptimize(), character.getOptimizationRequirements(), ignore);
    }

    public static float getOptimizationValue(Character character, CharacterCalculationsBase valuation) {
        float[] ignore = new float[1];
        return getCalculationsValue(character, valuation, character.getCalculationToOptimize(), character.getOptimizationRequirements(), ignore);
    }

    public static float getOptimizationValue(Character character, CharacterCalculationsBase valuation, String calculation, List<OptimizationRequirement> requirements) {
        float[] ignore = new float[1];
        return getCalculationsValue(character, valuation, calculation, requirements, ignore);
    }

    @Override
    protected float getOptimizationValue(OptimizerCharacter individual, CharacterCalculationsBase valuation) {
        float[] ignore = new float[1];
        return getCalculationsValue(individual.character, valuation, calculationToOptimize, requirements, ignore);
    }

    protected float getOptimizationValue(OptimizerCharacter individual, CharacterCalculationsBase valuation, float[] nonJewelerValue) {
        return getCalculationsValue(individual.character, valuation, calculationToOptimize, requirements, nonJewelerValue);
    }

    /**
     * Valuates the individual's character, with an optional temporary "extra item" (the gem or
     * enchant under test in the greedy scan) and optional talent calculation. The item-stats
     * volatility flag is switched to optimizer-managed for the duration so the model does not
     * re-run the expensive gem requirement bookkeeping the search handles through validators.
     */
    @Override
    protected CharacterCalculationsBase getValuation(OptimizerCharacter individual) {
        boolean oldVolatility = Item.isOptimizerManagedVolatiliy();
        try {
            Item.setOptimizerManagedVolatiliy(true);
            return model.getCharacterCalculations(individual.character, individual.extraItem, false, optimizeTalents, false);
        } finally {
            Item.setOptimizerManagedVolatiliy(oldVolatility);
        }
    }



    @Override
    protected Object getItem(OptimizerCharacter individual, int slot) {
        return individual.items[slot];
    }

    @Override
    protected Object[] getItems(OptimizerCharacter individual) {
        return individual.items;
    }

    @Override
    protected Object[] getRecycledItems(OptimizerCharacter recycledIndividual) {
        if (recycledIndividual == null) return null;
        return recycledIndividual.items;
    }

    @Override
    protected OptimizerCharacter generateIndividual(Object[] items, boolean canUseArray, OptimizerCharacter recycledIndividual) {
        Character character;
        if (recycledIndividual == null) {
            character = new Character(this.character, items, characterSlots);
        } else {
            character = recycledIndividual.character;
            character.initializeCharacter(items, characterSlots);
        }
        if (optimizeFood) {
            Buff food = (Buff) items[characterSlots];
            if (food != null && !character.getActiveBuffs().contains(food)) {
                character.activeBuffsAdd(food);
                Calculations.removeConflictingBuffs(character.getActiveBuffs(), food);
            }
        }
        if (optimizeElixirs) {
            Buff battle = (Buff) items[characterSlots + 1];
            Buff guardian = (Buff) items[characterSlots + 2];
            if (battle != null && battle.getConflictingBuffs().contains("Guardian Elixir")) {
                // flask
                if (battle != null) {
                    if (!character.getActiveBuffs().contains(battle)) {
                        character.activeBuffsAdd(battle);
                        if (mixology && battle.improvements.size() > 0) {
                            // flask of the north doesn't have an improvement (mixology only)
                            character.activeBuffsAdd(battle.improvements.get(0));
                        }
                        Calculations.removeConflictingBuffs(character.getActiveBuffs(), battle);
                    } else if (mixology) {
                        // make sure we have all improvements
                        for (Buff improvement : battle.improvements) {
                            if (!character.getActiveBuffs().contains(improvement)) {
                                character.activeBuffsAdd(improvement);
                            }
                        }
                    }
                }
            } else {
                if (battle != null) {
                    if (!character.getActiveBuffs().contains(battle)) {
                        character.activeBuffsAdd(battle);
                        if (mixology) character.activeBuffsAdd(battle.improvements.get(0));
                        Calculations.removeConflictingBuffs(character.getActiveBuffs(), battle);
                    } else if (mixology) {
                        // make sure we have all improvements
                        for (Buff improvement : battle.improvements) {
                            if (!character.getActiveBuffs().contains(improvement)) {
                                character.activeBuffsAdd(improvement);
                            }
                        }
                    }
                }
                if (guardian != null) {
                    if (!character.getActiveBuffs().contains(guardian)) {
                        character.activeBuffsAdd(guardian);
                        if (mixology) character.activeBuffsAdd(guardian.improvements.get(0));
                        Calculations.removeConflictingBuffs(character.getActiveBuffs(), guardian);
                    } else if (mixology) {
                        // make sure we have all improvements
                        for (Buff improvement : guardian.improvements) {
                            if (!character.getActiveBuffs().contains(improvement)) {
                                character.activeBuffsAdd(improvement);
                            }
                        }
                    }
                }
            }
        }
        if (optimizeTalents) {
            character.setCurrentTalents((TalentsBase) items[characterSlots + 3]);
        }
        if (recycledIndividual == null) {
            OptimizerCharacter ret = new OptimizerCharacter();
            ret.character = character;
            ret.items = canUseArray ? items : items.clone();
            return ret;
        } else {
            recycledIndividual.character = character;
            if (canUseArray) {
                recycledIndividual.items = items;
            } else {
                if (recycledIndividual.items != null) {
                    System.arraycopy(items, 0, recycledIndividual.items, 0, items.length);
                } else {
                    recycledIndividual.items = items.clone();
                }
            }
            return recycledIndividual;
        }
    }

    /**
     * Computes the objective value of one valuation: the requested calculation when no requirement
     * is violated, otherwise the (negative) total shortfall of the violated requirements. Invalid
     * gem requirements contribute a 100000 penalty per condition. {@code nonJewelerValue[0]} is an
     * out parameter of the original greedy-search API that receives the same value computed with
     * the jewelcrafter-gem penalties removed; the ported GreedyBest pass fills it but never reads
     * it.
     */
    private static float getCalculationsValue(Character character, CharacterCalculationsBase calcs, String calculation, List<OptimizationRequirement> requirements, float[] nonJewelerValue) {
        float gemValue = -GEM_REQUIREMENT_PENALTY * character.getGemRequirementsInvalid();
        float nonJewelerGemValue = -GEM_REQUIREMENT_PENALTY * character.getNonjewelerGemRequirementsInvalid();
        float ret = 0;
        for (OptimizationRequirement requirement : requirements) {
            float calcValue = OptimizerValues.getCalculationValue(character, calcs, requirement.calculation);
            if (requirement.lessThan) {
                if (!(calcValue <= requirement.value))
                    ret += requirement.value - calcValue;
            } else {
                if (!(calcValue >= requirement.value))
                    ret += calcValue - requirement.value;
            }
        }

        if (ret < 0) {
            nonJewelerValue[0] = ret + nonJewelerGemValue;
            return ret + gemValue;
        } else {
            float value = OptimizerValues.getCalculationValue(character, calcs, calculation);
            nonJewelerValue[0] = value + nonJewelerGemValue;
            return value + gemValue;
        }
    }



    // C# string.TrimEnd(']')


    /**
     * Sanitizes the winner: rearranges gems and enchants to a stat-equivalent instance as close as
     * possible to the item's default gemming and, unless the off-hand slot was the one under
     * evaluation, empties the off-hand when the model does not count it (for example for a
     * two-handed weapon).
     */
    @Override
    protected OptimizerCharacter postProcess(OptimizerCharacter bestIndividual) {
        if (bestIndividual != null) {
            itemGenerator.normalizeCharacter(bestIndividual.character, false); // this makes it nonnormal, but i don't think it should create any problems
            if (lockedSlot != CharacterSlot.OffHand && !bestIndividual.character.getCurrentCalculations().includeOffHandInCalculations(bestIndividual.character)) {
                bestIndividual.character.set(CharacterSlot.OffHand, null);
            }
        }
        return bestIndividual;
    }

    /**
     * Dispatches the direct-upgrade scan for one slot according to {@link #greedyOptimizationMethod}.
     * Extra slots (food, elixirs, talents) and the slot currently being evaluated always use the
     * full candidate enumeration; otherwise SingleChanges and GreedyBest use their cheaper scans.
     */
    @Override
    protected DirectUpgradeResult<OptimizerCharacter, CharacterCalculationsBase> lookForDirectItemUpgrades(List<Object> items, int slot, float best, OptimizerCharacter bestIndividual, Object[] itemList) {
        if (slot >= characterSlots || greedyOptimizationMethod == GreedyOptimizationMethod.AllCombinations || (greedyOptimizationMethod == GreedyOptimizationMethod.SingleChanges && slot == lockedSlot.value())) {
            return super.lookForDirectItemUpgrades(items, slot, best, bestIndividual, null);
        } else if (greedyOptimizationMethod == GreedyOptimizationMethod.SingleChanges) {
            return lookForDirectItemUpgradesSingleChanges(slot, best, bestIndividual);
        } else if (greedyOptimizationMethod == GreedyOptimizationMethod.GreedyBest) {
            return lookForDirectItemUpgradesGreedyBest(slot, best, bestIndividual);
        }
        return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
    }

    /**
     * GreedyBest direct-upgrade scan: first ranks gems and enchants by valuating the individual
     * with each candidate planted in the {@link OptimizerCharacter#extraItem} slot (meta gems, one
     * best gem per color, the best jewelcrafter gem, the best non-limited gem and the best enchant
     * per slot), then builds a small set of item instances per raw item from those winners and
     * feeds them to the full enumeration. Jeweler gems are inserted one at a time into the socket
     * where they add the most value.
     */
    private DirectUpgradeResult<OptimizerCharacter, CharacterCalculationsBase> lookForDirectItemUpgradesGreedyBest(int slot, float best, OptimizerCharacter bestIndividual) {
        OptimizerCharacter swappedIndividual;
        CharacterCalculationsBase bestValuation = null;
        float value;

        // we'll look what is the best gem for each color, separating the limited gems
        // then we'll construct a few item instances for each item and evaluate them
        if (itemGenerator.getSlotItems()[slot].size() > 1 || (itemGenerator.getSlotItems()[slot].size() == 1 && itemGenerator.getSlotItems()[slot].get(0) != null) || slot == lockedSlot.value()) {
            Object[] itemList = getItems(bestIndividual).clone();
            itemList[slot] = null;
            swappedIndividual = generateIndividual(itemList, true, null);
            float bestBlueValue = Float.NEGATIVE_INFINITY;
            Item bestBlueGem = null;
            float bestYellowValue = Float.NEGATIVE_INFINITY;
            Item bestYellowGem = null;
            float bestRedValue = Float.NEGATIVE_INFINITY;
            Item bestRedGem = null;
            float bestBlueJewelerValue = Float.NEGATIVE_INFINITY;
            Item bestBlueJewelerGem = null;
            float bestYellowJewelerValue = Float.NEGATIVE_INFINITY;
            Item bestYellowJewelerGem = null;
            float bestRedJewelerValue = Float.NEGATIVE_INFINITY;
            Item bestRedJewelerGem = null;
            float bestJewelerValue = Float.NEGATIVE_INFINITY;
            Item bestJewelerGem = null;
            float bestMetaValue = Float.NEGATIVE_INFINITY;
            Item bestMetaGem = null;
            float bestNonLimitedValue = Float.NEGATIVE_INFINITY;
            Item bestNonLimitedGem = null;
            float bestEnchantValue = Float.NEGATIVE_INFINITY;
            Enchant bestEnchant = null;
            float bestOneHandEnchantValue = Float.NEGATIVE_INFINITY;
            Enchant bestOneHandEnchant = null;
            if (slot == CharacterSlot.Head.value()) {
                for (Item gem : itemGenerator.getMetaGemItems()) {
                    swappedIndividual.extraItem = gem;
                    CharacterCalculationsBase valuation;
                    float[] nonJewelerValue = new float[1];
                    value = getOptimizationValue(swappedIndividual, valuation = getValuation(swappedIndividual), nonJewelerValue);
                    if (value > bestMetaValue) {
                        bestMetaValue = value;
                        bestMetaGem = gem;
                    }
                }
            }
            for (Item gem : itemGenerator.getGemItems()) {
                swappedIndividual.extraItem = gem;
                CharacterCalculationsBase valuation;
                float[] nonJewelerValue = new float[1];
                value = getOptimizationValue(swappedIndividual, valuation = getValuation(swappedIndividual), nonJewelerValue);
                if (Item.gemMatchesSlot(gem, ItemSlot.Blue) && !gem.isLimitedGem() && value > bestBlueValue) {
                    bestBlueValue = value;
                    bestBlueGem = gem;
                }
                if (Item.gemMatchesSlot(gem, ItemSlot.Red) && !gem.isLimitedGem() && value > bestRedValue) {
                    bestRedValue = value;
                    bestRedGem = gem;
                }
                if (Item.gemMatchesSlot(gem, ItemSlot.Yellow) && !gem.isLimitedGem() && value > bestYellowValue) {
                    bestYellowValue = value;
                    bestYellowGem = gem;
                }
                if (gem.isJewelersGem() && Item.gemMatchesSlot(gem, ItemSlot.Blue) && value > bestBlueJewelerValue) {
                    bestBlueJewelerValue = value;
                    bestBlueJewelerGem = gem;
                }
                if (gem.isJewelersGem() && Item.gemMatchesSlot(gem, ItemSlot.Yellow) && value > bestYellowJewelerValue) {
                    bestYellowJewelerValue = value;
                    bestYellowJewelerGem = gem;
                }
                if (gem.isJewelersGem() && Item.gemMatchesSlot(gem, ItemSlot.Red) && value > bestRedJewelerValue) {
                    bestRedJewelerValue = value;
                    bestRedJewelerGem = gem;
                }
                if (gem.isJewelersGem() && value > bestJewelerValue) {
                    bestJewelerValue = value;
                    bestJewelerGem = gem;
                }
                if (!gem.isLimitedGem() && value > bestNonLimitedValue) {
                    bestNonLimitedValue = value;
                    bestNonLimitedGem = gem;
                }
            }
            if (itemGenerator.getSlotEnchants()[slot] != null) {
                for (Enchant enchant : itemGenerator.getSlotEnchants()[slot]) {
                    Item extra = new Item();
                    extra.setStats(enchant.stats);
                    swappedIndividual.extraItem = extra;
                    CharacterCalculationsBase valuation;
                    float[] nonJewelerValue = new float[1];
                    value = getOptimizationValue(swappedIndividual, valuation = getValuation(swappedIndividual), nonJewelerValue);
                    if (value > bestEnchantValue) {
                        bestEnchantValue = value;
                        bestEnchant = enchant;
                    }
                    if (enchant.slot == ItemSlot.OneHand && value > bestOneHandEnchantValue) {
                        bestOneHandEnchantValue = value;
                        bestOneHandEnchant = enchant;
                    }
                }
            }

            List<Object> list = new ArrayList<Object>();
            List<Item> rawItems = itemGenerator.getSlotRawItems()[slot];
            if (slot == lockedSlot.value()) {
                List<Item> lockedRawItems = new ArrayList<Item>();
                lockedRawItems.add(((ItemInstance) lockedItems.get(0)).getItem());
                rawItems = lockedRawItems;
            }
            for (Item item : rawItems) {
                if (item.getAvailabilityInformation() == null || item.getAvailabilityInformation().generativeEnchants.size() > 0) {
                    Enchant enchant = bestEnchant;
                    if (enchant != null) {
                        if (enchant.slot == ItemSlot.OffHand) {
                            if (item.getType() != ItemType.Shield) {
                                enchant = bestOneHandEnchant;
                            }
                        } else if (enchant.slot == ItemSlot.TwoHand) {
                            if (item.getSlot() != ItemSlot.TwoHand) {
                                enchant = bestOneHandEnchant;
                            }
                        }
                    }
                    int gemCount = itemGenerator.getItemGemCount(item);
                    // first generate best nonlimited without matching sockets
                    Item[] gems = new Item[4];
                    float[] values = new float[4];
                    boolean matches = true;
                    for (int g = 1; g <= gemCount; g++) {
                        if (item.getSocketColor(g) == ItemSlot.Meta) {
                            gems[g] = bestMetaGem;
                        } else {
                            gems[g] = bestNonLimitedGem;
                            if (!Item.gemMatchesSlot(bestNonLimitedGem, item.getSocketColor(g))) {
                                matches = false;
                            }
                        }
                    }
                    ItemInstance itemInstance = new ItemInstance(item, gems[1], gems[2], gems[3], enchant);
                    list.add(itemInstance);
                    // add jewelers one by one into worst slot
                    for (int i = 0; i < gemCount; i++) {
                        int score = 0;
                        int bestg = 0;
                        for (int g = 1; g <= gemCount; g++) {
                            if (item.getSocketColor(g) != ItemSlot.Meta && !gems[g].isJewelersGem() && bestJewelerValue > bestNonLimitedValue) {
                                if (score < 1) {
                                    score = 1;
                                    bestg = g;
                                }
                            }
                        }
                        if (score > 0) {
                            gems[bestg] = bestJewelerGem;
                            itemInstance = new ItemInstance(item, gems[1], gems[2], gems[3], enchant);
                            list.add(itemInstance);
                        } else {
                            break;
                        }
                    }
                    // now generate best nonlimited with matching sockets
                    // but only if the nonmatching actually does not match sockets
                    if (!matches) {
                        for (int g = 1; g <= gemCount; g++) {
                            switch (item.getSocketColor(g)) {
                                case Meta:
                                    gems[g] = bestMetaGem;
                                    values[g] = bestMetaValue;
                                    break;
                                case Red:
                                    gems[g] = bestRedGem;
                                    values[g] = bestRedValue;
                                    break;
                                case Yellow:
                                    gems[g] = bestYellowGem;
                                    values[g] = bestYellowValue;
                                    break;
                                case Blue:
                                    gems[g] = bestBlueGem;
                                    values[g] = bestBlueValue;
                                    break;
                                case Prismatic:
                                    gems[g] = bestNonLimitedGem;
                                    values[g] = bestNonLimitedValue;
                                    break;
                                default:
                                    break;
                            }
                        }
                        itemInstance = new ItemInstance(item, gems[1], gems[2], gems[3], enchant);
                        list.add(itemInstance);
                        // add jewelers one by one into worst slot
                        for (int i = 0; i < gemCount; i++) {
                            float score = 0;
                            int bestg = 0;
                            for (int g = 1; g <= gemCount; g++) {
                                Item jewelerGem;
                                float jewelerValue = 0.0f;
                                switch (item.getSocketColor(g)) {
                                    case Blue:
                                        jewelerGem = bestBlueJewelerGem;
                                        jewelerValue = bestBlueJewelerValue;
                                        break;
                                    case Yellow:
                                        jewelerGem = bestYellowJewelerGem;
                                        jewelerValue = bestYellowJewelerValue;
                                        break;
                                    case Red:
                                        jewelerGem = bestRedJewelerGem;
                                        jewelerValue = bestRedJewelerValue;
                                        break;
                                    case Prismatic:
                                        jewelerGem = bestJewelerGem;
                                        jewelerValue = bestJewelerValue;
                                        break;
                                    default:
                                        jewelerGem = null;
                                        break;
                                }
                                if (item.getSocketColor(g) != ItemSlot.Meta && !gems[g].isJewelersGem() && jewelerValue > values[g]) {
                                    float newScore = jewelerValue - values[g];
                                    if (newScore > score) {
                                        score = newScore;
                                        bestg = g;
                                    }
                                }
                            }
                            if (score > 0) {
                                gems[bestg] = bestJewelerGem;
                                values[bestg] = bestJewelerValue;
                                itemInstance = new ItemInstance(item, gems[1], gems[2], gems[3], enchant);
                                list.add(itemInstance);
                            } else {
                                break;
                            }
                        }
                    }
                }
                if (item.getAvailabilityInformation() != null && item.getAvailabilityInformation().generativeEnchants.size() == 0) {
                    for (DirectUpgradeEntry entry : item.getAvailabilityInformation().singleDirectUpgradeList) {
                        list.add(entry.itemInstance);
                    }
                }
            }
            return super.lookForDirectItemUpgrades(list, slot, best, bestIndividual, itemList);
        }
        return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
    }

    /**
     * SingleChanges direct-upgrade scan: walks the generator's per-item trees (the first level is
     * one gem, the next levels add gem two, gem three and the enchant) and, at each level, moves
     * into the subtree of the locally best candidate, so only a few instances per item are
     * evaluated instead of the full gem cartesian product.
     */
    private DirectUpgradeResult<OptimizerCharacter, CharacterCalculationsBase> lookForDirectItemUpgradesSingleChanges(int slot, float best, OptimizerCharacter bestIndividual) {
        OptimizerCharacter swappedIndividual;
        CharacterCalculationsBase bestValuation = null;
        float value;
        boolean foundUpgrade = false;
        Object[] itemList = getItems(bestIndividual).clone();

        // we don't want to look at all possible gem/enchant combinations
        // instead do a greedy search within each item for gems/enchants
        List<List<DirectUpgradeEntry>> map = itemGenerator.getSlotDirectUpgrades()[slot];
        for (List<DirectUpgradeEntry> list : map) {
            // find best within the list and then recurse on child list
            List<DirectUpgradeEntry> childList = list;
            while (childList != null) {
                List<DirectUpgradeEntry> bestList = null;
                float bestTemp = Float.NEGATIVE_INFINITY;
                for (DirectUpgradeEntry entry : childList) {
                    itemList[slot] = entry.itemInstance;
                    if (isIndividualValid(itemList)) {
                        swappedIndividual = generateIndividual(itemList, false, null);
                        CharacterCalculationsBase valuation;
                        value = getOptimizationValue(swappedIndividual, valuation = getValuation(swappedIndividual));
                        if (value > best) {
                            best = value;
                            bestValuation = valuation;
                            bestIndividual = swappedIndividual;
                            foundUpgrade = true;
                        }
                        if (value > bestTemp) {
                            bestTemp = value;
                            bestList = entry.directUpgradeList;
                        }
                    }
                }
                childList = bestList;
            }
        }
        if (foundUpgrade)
            return new DirectUpgradeResult<>(best, bestIndividual, bestValuation);
        return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
    }

    /**
     * Random gene selection with two extra rules over the base implementation: the currently
     * locked slot only offers the locked gemmings, and item selection respects the three-gem limit
     * on jewelcrafter gems by first computing the minimum number already forced by the other slots
     * and rejecting candidates beyond the remaining budget (up to 10 retries, then accept).
     */
    @Override
    protected Object getRandomItem(int slot, Object[] items) {
        if (slot >= characterSlots) {
            return super.getRandomItem(slot, items);
        } else if (lockedSlot.value() == slot) {
            return lockedItems.get(getRnd().next(lockedItems.size()));
        } else {
            // select random item such that jeweler count won't exceed maximum
            // first count how many jewelers at minimum we will have assuming what we have so far and the minimum available from the rest of slots
            int min = 0;
            for (int s = 0; s < slot; s++) {
                ItemInstance item = (ItemInstance) items[s];
                if (item != null) {
                    min += item.getJewelerCount();
                }
            }
            for (int s = slot + 1; s < characterSlots; s++) {
                min += minJeweler[s];
            }
            int max = 3 - min; // we can use at most this many if we want to be possible to be feasible
            ItemInstance result = null;
            int count = 0;
            NetRandom rand = getRnd();
            do {
                KeyedList<KeyedList<ItemInstance>> list1 = slotItemsRandom[slot].get(rand.next(slotItemsRandom[slot].size()));
                KeyedList<ItemInstance> list2 = list1.get(rand.next(list1.size()));
                result = list2.get(rand.next(list2.size()));
            } while (count++ < 10 && result != null && result.getJewelerCount() > max);
            return result;
        }
    }

    /**
     * Jewelcrafter-preserving crossover: before taking a parent's item for a slot, the builder
     * computes how many jewelcrafter gems are already forced by earlier picks and the minimum the
     * remaining slots of both parents can contribute, and falls back to the other parent when the
     * first choice could exceed the three-gem limit. Because the greedy minimum can underestimate,
     * a unique-item guard prevents an infinite loop.
     */
    @Override
    protected OptimizerCharacter buildChildIndividual(OptimizerCharacter father, OptimizerCharacter mother, OptimizerCharacter recycledIndividual) {
        // jewelcrafter preserving crossover
        // an alternative option would be to add optimizer constraints that restarted character construction if jewelcrafter
        // gems exceeded limit, it is not clear without testing which option is better
        return generatorBuildIndividual(
                (slot, items) -> {
                    if (slot < characterSlots) {
                        int min = 0;
                        for (int s = 0; s < slot; s++) {
                            ItemInstance item = (ItemInstance) items[s];
                            if (item != null) {
                                min += item.getJewelerCount();
                            }
                        }
                        for (int s = slot + 1; s < characterSlots; s++) {
                            ItemInstance item1 = (ItemInstance) father.items[s];
                            int c1 = item1 == null ? 0 : item1.getJewelerCount();
                            ItemInstance item2 = (ItemInstance) mother.items[s];
                            int c2 = item2 == null ? 0 : item2.getJewelerCount();
                            min += Math.min(c1, c2);
                        }
                        int max = 3 - min;
                        ItemInstance f = (ItemInstance) father.items[slot];
                        int fc = f == null ? 0 : f.getJewelerCount();
                        ItemInstance m = (ItemInstance) mother.items[slot];
                        int mc = m == null ? 0 : m.getJewelerCount();
                        if (fc > max) {
                            // we need a unique check otherwise we can end in a dead loop because the min computed above underestimates
                            if (m != null && m.getItem().isUnique() && slot > 0 && items[slot - 1] != null && (((ItemInstance) items[slot - 1]).getId() == m.getId() || (m.getItem().getUniqueId() != null && m.getItem().getUniqueId().contains(((ItemInstance) items[slot - 1]).getId())))) {
                                return f;
                            }
                            return m;
                        } else if (mc > max) {
                            if (f != null && f.getItem().isUnique() && slot > 0 && items[slot - 1] != null && (((ItemInstance) items[slot - 1]).getId() == f.getId() || (f.getItem().getUniqueId() != null && f.getItem().getUniqueId().contains(((ItemInstance) items[slot - 1]).getId())))) {
                                return m;
                            }
                            return f;
                        } else {
                            return getRnd().nextDouble() < 0.5d ? f : m;
                        }
                    } else {
                        return getRnd().nextDouble() < 0.5d ? getItem(father, slot) : getItem(mother, slot);
                    }
                },
                recycledIndividual);
    }

    private ItemInstance replaceGem(ItemInstance item, int index, Item gem) {
        ItemInstance copy = new ItemInstance(item.getItem(), item.getGem1(), item.getGem2(), item.getGem3(), item.getEnchant());
        copy.setGem(index, gem);
        return copy;
    }

    private static class GemInformation {
        public CharacterSlot slot;
        public int index;
        public Item gem;
        public ItemSlot socket;
    }

    private OptimizerCharacter buildReplaceGemMutantCharacter(OptimizerCharacter parent, OptimizerCharacter recycledIndividual, boolean[] successful) {
        Object[] items = getRecycledItems(recycledIndividual);
        if (items == null) items = new Object[slotCount];
        System.arraycopy(parent.items, 0, items, 0, slotCount);
        successful[0] = false;

        // do the work

        // build a list of possible mutation points
        List<GemInformation> locationList = new ArrayList<GemInformation>();
        for (int slot = 0; slot < characterSlots; slot++) {
            if (items[slot] != null) {
                for (int i = 1; i <= 3; i++) {
                    Item gem = ((ItemInstance) items[slot]).getGem(i);
                    if (gem != null) {
                        GemInformation info = new GemInformation();
                        info.slot = CharacterSlot.fromValue(slot);
                        info.index = i;
                        info.gem = gem;
                        info.socket = ((ItemInstance) items[slot]).getItem().getSocketColor(i);
                        locationList.add(info);
                    }
                }
            }
        }

        NetRandom rand = getRnd();
        if (locationList.size() > 0) {
            int numberMutations = rand.next(1, 2);
            for (int i = 0; i < numberMutations; i++) {
                // randomly select mutation point
                int mutationIndex = rand.next(locationList.size());

                // mutate
                GemInformation mutation = locationList.get(mutationIndex);
                Item newGem;
                if (mutation.socket == ItemSlot.Meta) {
                    newGem = itemGenerator.getMetaGemItems()[rand.next(itemGenerator.getMetaGemItems().length)];
                } else {
                    newGem = itemGenerator.getGemItems()[rand.next(itemGenerator.getGemItems().length)];
                }
                ItemInstance newItem = replaceGem((ItemInstance) items[mutation.slot.value()], mutation.index, newGem);
                // make sure the item and item-enchant combo is allowed
                if ((lockedSlot == mutation.slot && lockedItems.contains(newItem)) || (lockedSlot != mutation.slot && itemAvailable.containsKey(newItem.getGemmedId()))) {
                    items[mutation.slot.value()] = newItem;
                    successful[0] = true;
                }
            }
        }

        // create character
        return generateIndividual(items, true, recycledIndividual);
    }

    private OptimizerCharacter buildSwapGemMutantCharacter(OptimizerCharacter parent, OptimizerCharacter recycledIndividual, boolean[] successful) {
        Object[] items = getRecycledItems(recycledIndividual);
        if (items == null) items = new Object[slotCount];
        System.arraycopy(parent.items, 0, items, 0, slotCount);
        successful[0] = false;

        // do the work

        // build a list of possible mutation points
        // make sure not to do meta gem swaps
        List<GemInformation> locationList = new ArrayList<GemInformation>();
        for (int slot = 0; slot < characterSlots; slot++) {
            if (items[slot] != null) {
                for (int i = 1; i <= 3; i++) {
                    Item gem = ((ItemInstance) items[slot]).getGem(i);
                    if (gem != null && gem.getSlot() != ItemSlot.Meta) {
                        GemInformation info = new GemInformation();
                        info.slot = CharacterSlot.fromValue(slot);
                        info.index = i;
                        info.gem = gem;
                        info.socket = ((ItemInstance) items[slot]).getItem().getSocketColor(i);
                        locationList.add(info);
                    }
                }
            }
        }

        NetRandom rand = getRnd();
        if (locationList.size() > 1) {
            GemInformation mutation1;
            GemInformation mutation2;
            int tries = 0;
            // randomly select mutation point
            boolean promising;
            do {
                promising = true;
                int mutationIndex1 = rand.next(locationList.size());
                int mutationIndex2 = rand.next(locationList.size());
                mutation1 = locationList.get(mutationIndex1);
                mutation2 = locationList.get(mutationIndex2);
                if (mutation1.gem.getSlot() == mutation2.gem.getSlot()) promising = false;

                int matchNow = 0;
                if (Item.gemMatchesSlot(mutation1.gem, mutation1.socket)) matchNow++;
                if (Item.gemMatchesSlot(mutation2.gem, mutation2.socket)) matchNow++;
                if (matchNow == 2) promising = false;
                int matchThen = 0;
                if (Item.gemMatchesSlot(mutation1.gem, mutation2.socket)) matchThen++;
                if (Item.gemMatchesSlot(mutation2.gem, mutation1.socket)) matchThen++;
                if (tries < 50) {
                    if (mutation1.socket == mutation2.socket) promising = false;
                    if (matchThen <= matchNow) promising = false;
                } else {
                    // allow 1 to 1 trade, because the other socket bonus might be better
                    if (mutation1.socket == mutation2.socket && mutation1.gem.isLimitedGem() == mutation2.gem.isLimitedGem()) promising = false;
                    if (matchThen < matchNow || matchThen == 0) promising = false;
                }
                tries++;
            } while (tries < 100 && !promising);

            if (promising) {
                // mutate
                ItemInstance item1 = replaceGem((ItemInstance) items[mutation1.slot.value()], mutation1.index, mutation2.gem);
                ItemInstance item2 = replaceGem((ItemInstance) items[mutation2.slot.value()], mutation2.index, mutation1.gem);
                if (((lockedSlot == mutation1.slot && lockedItems.contains(item1)) || (lockedSlot != mutation1.slot && itemAvailable.containsKey(item1.getGemmedId()))) && ((lockedSlot == mutation2.slot && lockedItems.contains(item2)) || (lockedSlot != mutation2.slot && itemAvailable.containsKey(item2.getGemmedId())))) {
                    successful[0] = true;
                    items[mutation1.slot.value()] = item1;
                    items[mutation2.slot.value()] = item2;
                }
            }
        }

        // create character

        if (successful[0]) {
            return generateIndividual(items, true, recycledIndividual);
        }
        return null;
    }

    /**
     * Mutation with three behavior-specific variants: talents are mutated half of the time when
     * talent optimization and mutation are enabled; otherwise the base item mutation is used
     * (always, when no item generator is available, and with probability 0.9 otherwise); the
     * remaining cases try a gem-level mutation, either replacing one gem or swapping two gems.
     * Failed gem mutations fall back to the base mutation. Resulting gemmed ids must exist in the
     * availability map (or in the locked list for the locked slot).
     */
    @Override
    protected OptimizerCharacter buildMutantIndividual(OptimizerCharacter parent, OptimizerCharacter recycledIndividual) {
        boolean[] successful = new boolean[1];
        OptimizerCharacter mutant = null;
        NetRandom rand = getRnd();
        if (optimizeTalents && mutateTalents && rand.nextDouble() < 0.5) {
            return buildMutateTalentsCharacter(parent, recycledIndividual);
        } else if (itemGenerator == null || rand.nextDouble() < 0.9) {
            return super.buildMutantIndividual(parent, recycledIndividual);
        } else if (rand.nextDouble() < 0.5) {
            mutant = buildReplaceGemMutantCharacter(parent, recycledIndividual, successful);
            if (!successful[0]) {
                return super.buildMutantIndividual(parent, recycledIndividual);
            }
        } else {
            mutant = buildSwapGemMutantCharacter(parent, recycledIndividual, successful);
            if (!successful[0]) {
                return super.buildMutantIndividual(parent, recycledIndividual);
            }
        }
        return mutant;
    }

    private static class TalentItem {
        public TalentDataAttribute talentData;
        public List<Integer> childList = new ArrayList<Integer>();

        TalentItem(TalentDataAttribute talentData) {
            this.talentData = talentData;
        }
    }

    private TalentItem[] talentItem = new TalentItem[100];
    private int talentItemCount;

    private OptimizerCharacter buildMutateTalentsCharacter(OptimizerCharacter parent, OptimizerCharacter recycledIndividual) {
        OptimizerCharacter optCharacter = generateIndividual(parent.items, false, recycledIndividual);
        Object[] items = optCharacter.items;
        Character character = optCharacter.character;
        TalentsBase talents = ((TalentsBase) items[characterSlots + 3]).clone();
        items[characterSlots + 3] = talents;
        int[][] treeCount = new int[3][11];
        for (int j = 0; j < talentItemCount; j++) {
            treeCount[talentItem[j].talentData.tree()][talentItem[j].talentData.row() - 1] += talents.getData()[j];
        }
        // add the talent somewhere
        boolean talentAdded = false;
        NetRandom rand = getRnd();
        do {
            int index = rand.next(talentItemCount);
            if (talents.getData()[index] == talentItem[index].talentData.maxPoints()) continue;
            int p = talentItem[index].talentData.prerequisite();
            if (p >= 0 && talents.getData()[p] < talentItem[p].talentData.maxPoints()) continue;
            int points = 0;
            for (int k = 0; k < talentItem[index].talentData.row() - 1; k++) {
                points += treeCount[talentItem[index].talentData.tree()][k];
            }
            if (points < 5 * (talentItem[index].talentData.row() - 1)) continue;
            // we're good, we can add the talent point
            talents.getData()[index]++;
            treeCount[talentItem[index].talentData.tree()][talentItem[index].talentData.row() - 1]++;
            talentAdded = true;
        } while (!talentAdded);
        // pick a talent with some points invested that we can take points out of
        boolean talentRemoved = false;
        do {
            int index = rand.next(talentItemCount);
            if (talents.getData()[index] == 0) continue;
            boolean locked = false;
            for (int child : talentItem[index].childList) {
                if (talents.getData()[child] > 0) {
                    locked = true;
                    break;
                }
            }
            if (!locked) {
                int i = 1;
                int pts = 0;
                int talentRow = talentItem[index].talentData.row() - 1;
                int tree = talentItem[index].talentData.tree();
                for (i = 0; i <= talentRow; i++) pts += treeCount[tree][i];
                pts = pts - 1;
                for (i = talentRow + 1; i < 11; i++) {
                    if (treeCount[tree][i] > 0) {
                        if (pts >= i * 5) {
                            pts += treeCount[tree][i];
                        } else {
                            i = -1;
                            break;
                        }
                    }
                }
                if (i >= 0) {
                    // we're good, we can remove the talent point
                    talents.getData()[index]--;
                    treeCount[talentItem[index].talentData.tree()][talentItem[index].talentData.row() - 1]--;
                    talentRemoved = true;
                }
            }
        } while (!talentRemoved);
        character.setCurrentTalents(talents);
        return optCharacter;
    }
}
