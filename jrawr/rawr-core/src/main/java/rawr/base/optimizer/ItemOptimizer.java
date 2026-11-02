// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (UI/concurrency, reported): the Asynchronous Pattern region (event args, delegates,
// BackgroundWorker/AsyncOperation/SendOrPostCallback, IsBusy async guards), the ReportProgress event
// posting (reportProgress is a no-op hook) and the Model setter's multithreading toggle.
// BossOptions are not ported in the Java core, so the two C# copies of _character.BossOptions are
// omitted from GenerateIndividual/LookForDirectItemUpgrades.
package rawr.base.optimizer;

import rawr.base.util.KeyValuePair;

import rawr.base.bosses.BossOptions;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.calculations.OptimizerRangeValidatorBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The older, gem-agnostic gear optimizer: an individual is a plain {@link Character} and each of
 * the 19 equipment slots contributes five genes &mdash; the item, its three gem ids and its
 * enchant id &mdash; for a total of 95 genes.
 *
 * <p>Unlike {@link ItemInstanceOptimizer}, this optimizer does not work with precomputed
 * {@link ItemInstance} pools or a reusable working character. It takes the ordinary
 * {@link Character} as the individual, rebuilds a fresh character from the genes on every
 * evaluation, and lets each item keep whatever gemming the availability rules permit: gem genes
 * are chosen from the item's availability list and validated as a whole by
 * {@link ItemAvailableValidator}, which reconstructs the
 * {@code itemId.gem1.gem2.gem3.enchantId} key and checks it against the item's availability map.</p>
 *
 * <h2>Initialization and entry points</h2>
 *
 * <p>{@link #initializeItemCache} builds an {@link AvailableItemGenerator} from the character's
 * available-item strings and reduces its per-slot instances to distinct items for the item genes.
 * {@link #optimizeCharacter}, {@link #computeUpgrades} and {@link #evaluateUpgrade} mirror the
 * {@code ItemInstanceOptimizer} entry points; the upgrade scans lock a candidate item into its slot
 * through the private locked-item list and run the direct-upgrade scan over its gemmings before the
 * optional full search. Currently equipped items that the user did not select are registered by
 * {@code markEquippedItemsAsValid} so a search starting from the current gear can keep it.</p>
 *
 * <h2>Fixed-pool verification</h2>
 *
 * <p>As with {@link ItemInstanceOptimizer}, the oracle comparison fixes the item pool, the
 * objective and the seed and requires only that the Java best score is not worse than the C# best;
 * it never requires the chosen item set to match.</p>
 */
public class ItemOptimizer extends OptimizerBase<Object, Character, CharacterCalculationsBase> {
    private Character character;
    private String calculationToOptimize;
    private List<OptimizationRequirement> requirements;
    private CalculationsBase model;

    public CalculationsBase getModel() {
        return model;
    }

    public void setModel(CalculationsBase model) {
        this.model = model;
    }

    /**
     * Rejects an individual whose paired slots (finger pair, trinket pair or weapon pair) hold the
     * same unique item twice, also honoring the item's unique-equip group list. The check consults
     * the unique flag and group of the first item only, as in the ported original.
     */
    private static class UniqueItemValidator extends OptimizerRangeValidatorBase<Object> {
        @Override
        public boolean isValid(Object[] items) {
            Item item1 = (Item) items[startSlot];
            Item item2 = (Item) items[endSlot];
            return !(item1 != null && item2 != null && item1.isUnique() && (item1.getId() == item2.getId() || (item1.getUniqueId() != null && item1.getUniqueId().contains(item2.getId()))));
        }
    }

    /**
     * Validates the five genes of one character slot against the item's availability map: the
     * gemmed id is reconstructed from the item, gem and enchant genes (the enchant position only
     * for enchantable slots) and must have been registered as available by the pool generator.
     */
    private static class ItemAvailableValidator extends OptimizerRangeValidatorBase<Object> {
        private final boolean enchantable;

        @Override
        public boolean isValid(Object[] items) {
            Item item = (Item) items[startSlot];
            Item gem1 = (Item) items[startSlot + 1];
            Item gem2 = (Item) items[startSlot + 2];
            Item gem3 = (Item) items[startSlot + 3];
            Enchant enchant = (Enchant) items[startSlot + 4];
            int gemCount = item.getAvailabilityInformation().gemCount;
            String key = String.format(Locale.ROOT, "%d.%d.%d.%d.%d",
                    item != null ? item.getId() : 0,
                    gem1 != null && gemCount >= 1 ? gem1.getId() : 0,
                    gem2 != null && gemCount >= 2 ? gem2.getId() : 0,
                    gem3 != null && gemCount >= 3 ? gem3.getId() : 0,
                    enchantable && enchant != null ? enchant.id : 0);
            return item.getAvailabilityInformation().itemAvailable.containsKey(key);
        }

        ItemAvailableValidator(boolean enchantable, int slot) {
            this.enchantable = enchantable;
            this.startSlot = slot;
            if (enchantable) {
                endSlot = slot + 4;
            } else {
                endSlot = slot + 3;
            }
        }
    }

    private static final int characterSlots = 19;

    @SuppressWarnings("unchecked")
    public ItemOptimizer() {
        slotCount = characterSlots * 5;
        slotItems = new List[slotCount];
        validators = new ArrayList<OptimizerRangeValidatorBase<Object>>();
        UniqueItemValidator fingerValidator = new UniqueItemValidator();
        fingerValidator.startSlot = 5 * CharacterSlot.Finger1.value();
        fingerValidator.endSlot = 5 * CharacterSlot.Finger2.value();
        UniqueItemValidator trinketValidator = new UniqueItemValidator();
        trinketValidator.startSlot = 5 * CharacterSlot.Trinket1.value();
        trinketValidator.endSlot = 5 * CharacterSlot.Trinket2.value();
        UniqueItemValidator weaponValidator = new UniqueItemValidator();
        weaponValidator.startSlot = 5 * CharacterSlot.MainHand.value();
        weaponValidator.endSlot = 5 * CharacterSlot.OffHand.value();
        validators.add(fingerValidator);
        validators.add(trinketValidator);
        validators.add(weaponValidator);
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.Projectile.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Head.value()));
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.Neck.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Shoulders.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Chest.value()));
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.Waist.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Legs.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Feet.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Wrist.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Hands.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Finger1.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Finger2.value()));
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.Trinket1.value()));
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.Trinket2.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Back.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.MainHand.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.OffHand.value()));
        validators.add(new ItemAvailableValidator(true, 5 * CharacterSlot.Ranged.value()));
        validators.add(new ItemAvailableValidator(false, 5 * CharacterSlot.ProjectileBag.value()));
    }

    /**
     * Builds the candidate pools from the available-item strings. The generator's gemmed instances
     * are reduced to one gene per distinct item id for the item positions; gem and enchant genes
     * later sample the item's full availability list.
     */
    public void initializeItemCache(Character character, List<String> availableItems, boolean overrideRegem, boolean overrideReenchant, boolean templateGemsEnabled, CalculationsBase model) {
        this.character = character;
        setModel(model);
        populateAvailableIds(availableItems, templateGemsEnabled, overrideRegem, overrideReenchant);
    }

    private boolean isBusy;

    public boolean isBusy() {
        return isBusy;
    }

    /** UI progress reporting omitted; kept as the optimizer hook (reported). */
    @Override
    protected void reportProgress(int progressPercentage, float bestValue) {
    }

    /**
     * Runs the optimization and returns the best character found, or null on cancellation. The
     * character argument supplies the baseline (and, with {@code injectCharacter} or thoroughness
     * 1, enters the search as a candidate). Throws when another operation is running or the item
     * cache was never initialized.
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
        OptimizationResult<Character, CharacterCalculationsBase> result = null;
        lockedSlot = CharacterSlot.None;

        try {
            if (this.thoroughness == 1) {
                // if we just start from current character and look for direct upgrades
                // then we have to deal with items that are currently equipped, but are not
                // currently available
                markEquippedItemsAsValid(this.character);
            }

            result = (injectCharacter || this.thoroughness == 1) ? optimize(character) : optimize();
            optimizedCharacter = result != null ? result.individual() : null;
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, result != null ? result.bestValue() : 0f);
        return optimizedCharacter;
    }

    /**
     * Computes the per-slot upgrade chart: for every relevant item (or the single
     * {@code singleItemUpgrades} item) that fits a slot, locks it into the slot, chooses its best
     * gemming through the direct-upgrade scan and, after the optional full search, records the gain
     * over the character's own value. Returns an insertion-ordered map keyed by the 17 optimizable
     * slots, or null when cancelled. The character field is saved and restored around the scan.
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
     * Registers the character's currently equipped items that are missing from the item gene pools
     * and adds their gemmed instance to the availability map, so the search can keep the current
     * gear even when the user did not mark it available.
     */
    private void markEquippedItemsAsValid(Character character) {
        for (int i = 0; i < slotCount; i++) {
            ItemInstance item = character.get(CharacterSlot.fromValue(i));
            if (item != null && item.getId() != 0) {
                if (!slotItems[5 * i].contains(item.getItem())) {
                    slotItems[5 * i].add(item.getItem());
                }
                Item it = item.getItem();
                if (it != null) {
                    if (it.getAvailabilityInformation() == null) {
                        itemGenerator.generateItemAvailabilityInformation(it);
                    }
                    if (!it.getAvailabilityInformation().itemAvailable.containsKey(item.getGemmedId())) {
                        it.getAvailabilityInformation().itemList.add(item);
                        it.getAvailabilityInformation().itemAvailable.put(item.getGemmedId(), true);
                    }
                }
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

        Character saveCharacter = this.character;
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
            float baseValue = getOptimizationValue(this.character, baseCalculations);
            Map<Integer, Item> itemById = new LinkedHashMap<Integer, Item>();
            for (Item item : items) {
                itemById.put(item.getId(), item);
            }

            if (singleItemUpgrades != null) {
                items = new Item[] { singleItemUpgrades };
            } else {
                items = itemById.values().toArray(new Item[0]);
            }

            for (int i = 0; i < items.length; i++) {
                Item item = items[i];
                currentItem = item.getName();
                itemProgressPercentage = (int) Math.rint((float) i / ((float) items.length / 100f));
                if (cancellationPending) {
                    return null;
                }
                reportProgress(0, 0);
                for (CharacterSlot slot : slots) {
                    if (item.fitsInSlot(slot, this.character)) {
                        List<ComparisonCalculationUpgrades> comparisons = upgrades.get(slot);
                        populateLockedItems(item);
                        lockedSlot = slot;
                        if (lockedSlot == CharacterSlot.Finger1 && item.isUnique() && this.character.get(CharacterSlot.Finger2) != null && (this.character.get(CharacterSlot.Finger2).getId() == item.getId() || (item.getUniqueId() != null && item.getUniqueId().contains(this.character.get(CharacterSlot.Finger2).getId())))) {
                            lockedSlot = CharacterSlot.Finger2;
                        }
                        if (lockedSlot == CharacterSlot.Trinket1 && item.isUnique() && this.character.get(CharacterSlot.Trinket2) != null && (this.character.get(CharacterSlot.Trinket2).getId() == item.getId() || (item.getUniqueId() != null && item.getUniqueId().contains(this.character.get(CharacterSlot.Trinket2).getId())))) {
                            lockedSlot = CharacterSlot.Trinket2;
                        }
                        this.character = buildSingleItemSwapIndividual(this.character, lockedSlot.value(), lockedItems.get(0));
                        // instead of just putting in the first gemming on the list select the best one
                        float best = NO_SEARCH_SCORE;
                        Character bestCharacter;
                        if (lockedItems.size() > 1) {
                            Character directUpgradeCharacter = lookForDirectItemUpgrades(null, lockedSlot, best, this.character).individual();
                            if (directUpgradeCharacter != null) {
                                this.character = directUpgradeCharacter;
                            }
                        }
                        if (this.thoroughness > 1) {
                            int saveThoroughness = this.thoroughness;
                            this.thoroughness = 1;
                            OptimizationResult<Character, CharacterCalculationsBase> injected = optimize(this.character, 0f);
                            this.thoroughness = saveThoroughness;
                            OptimizationResult<Character, CharacterCalculationsBase> result = optimize(injected.individual(), injected.bestValue());
                            bestCharacter = result.individual();
                            best = result.bestValue();
                        } else {
                            OptimizationResult<Character, CharacterCalculationsBase> result = optimize(this.character, 0f);
                            bestCharacter = result.individual();
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
                        this.character = saveCharacter;
                    }
                }
            }
        } catch (Exception ex) {
            error[0] = ex;
        } finally {
            this.character = saveCharacter;
        }

        reportProgress(100, 0f);
        return upgrades;
    }

    /**
     * Evaluates one concrete gemmed instance as a replacement in every slot it fits and returns the
     * gain over the character's value. A negative gain for an item that is not the one currently
     * equipped is clamped to zero. Throws when the item cache is not initialized or another
     * operation is running.
     */
    public float evaluateUpgrade(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, ItemInstance upgrade) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        float upgradeValue = privateEvaluateUpgrade(character, calculationToOptimize, requirements, thoroughness, upgrade, error);
        if (error[0] != null) throw new RuntimeException(error[0]);
        isBusy = false;
        return upgradeValue;
    }

    private float privateEvaluateUpgrade(Character character, String calculationToOptimize, List<OptimizationRequirement> requirements, int thoroughness, ItemInstance upgrade, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.character = character;
        setModel(Calculations.getModel(character.getCurrentModel()));
        this.calculationToOptimize = calculationToOptimize;
        this.requirements = requirements;
        this.thoroughness = thoroughness;

        Character saveCharacter = this.character;
        float upgradeValue = 0f;
        try {
            // make equipped gear/enchant valid
            // this is currently only called after calculate upgrades already marks items as valid, but we might have to do this here also if things change
            // MarkEquippedItemsAsValid(_character);

            CharacterSlot[] slots = new CharacterSlot[] { CharacterSlot.Back, CharacterSlot.Chest, CharacterSlot.Feet, CharacterSlot.Finger1, CharacterSlot.Hands, CharacterSlot.Head, CharacterSlot.Legs, CharacterSlot.MainHand, CharacterSlot.Neck, CharacterSlot.OffHand, CharacterSlot.Projectile, CharacterSlot.ProjectileBag, CharacterSlot.Ranged, CharacterSlot.Shoulders, CharacterSlot.Trinket1, CharacterSlot.Waist, CharacterSlot.Wrist };
            CharacterCalculationsBase baseCalculations = model.getCharacterCalculations(this.character);
            float baseValue = getOptimizationValue(this.character, baseCalculations);

            ItemInstance item = upgrade;
            for (CharacterSlot slot : slots) {
                if (item.getItem().fitsInSlot(slot, this.character)) {
                    lockedItems = new ArrayList<ItemInstance>();
                    lockedItems.add(item);
                    lockedSlot = slot;
                    if (lockedSlot == CharacterSlot.Finger1 && item.getItem().isUnique() && this.character.get(CharacterSlot.Finger2) != null && (this.character.get(CharacterSlot.Finger2).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.Finger2).getId())))) {
                        lockedSlot = CharacterSlot.Finger2;
                    }
                    if (lockedSlot == CharacterSlot.Trinket1 && item.getItem().isUnique() && this.character.get(CharacterSlot.Trinket2) != null && (this.character.get(CharacterSlot.Trinket2).getId() == item.getId() || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(this.character.get(CharacterSlot.Trinket2).getId())))) {
                        lockedSlot = CharacterSlot.Trinket2;
                    }
                    this.character = buildSingleItemSwapIndividual(this.character, lockedSlot.value(), upgrade);
                    float best;
                    Character bestCharacter;
                    if (this.thoroughness > 1) {
                        int saveThoroughness = this.thoroughness;
                        this.thoroughness = 1;
                        OptimizationResult<Character, CharacterCalculationsBase> injected = optimize(this.character, 0f);
                        this.thoroughness = saveThoroughness;
                        OptimizationResult<Character, CharacterCalculationsBase> result = optimize(injected.individual(), injected.bestValue());
                        bestCharacter = result.individual();
                        best = result.bestValue();
                    } else {
                        OptimizationResult<Character, CharacterCalculationsBase> result = optimize(this.character, 0f);
                        bestCharacter = result.individual();
                        best = result.bestValue();
                    }
                    if (bestCharacter.get(lockedSlot) == null || bestCharacter.get(lockedSlot).getId() != item.getId()) throw new RuntimeException("There was an internal error in Optimizer when evaluating upgrade.");
                    upgradeValue = best - baseValue;
                    if (upgradeValue < 0 && (saveCharacter.get(lockedSlot) == null || saveCharacter.get(lockedSlot).getId() != item.getId())) upgradeValue = 0f;
                    this.character = saveCharacter;
                }
            }
        } catch (Exception ex) {
            error[0] = ex;
        } finally {
            this.character = saveCharacter;
        }

        reportProgress(100, 0f);
        return upgradeValue;
    }

    private boolean itemCacheInitialized;

    private int[] pairSlotMap;
    private List<ItemInstance>[] slotList;
    private List<ItemInstance> lockedItems;
    private CharacterSlot lockedSlot = CharacterSlot.None;
    private AvailableItemGenerator itemGenerator;

    private void populateLockedItems(Item item) {
        lockedItems = itemGenerator.getPossibleGemmedItemsForItem(item, Integer.toString(item.getId()));
    }

    @SuppressWarnings("unchecked")
    private void populateAvailableIds(List<String> availableItems, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant) {
        itemGenerator = new AvailableItemGenerator(availableItems, false, templateGemsEnabled, overrideRegem, overrideReenchant, true, this.character, model);
        slotList = itemGenerator.getSlotItems();

        for (int i = 0; i < characterSlots; i++) {
            Set<Integer> seenItemIds = new LinkedHashSet<Integer>();
            slotItems[5 * i] = new ArrayList<Object>();
            for (ItemInstance itemInstance : slotList[i]) {
                int itemId = 0;
                if (itemInstance != null) {
                    itemId = itemInstance.getId();
                }
                if (seenItemIds.add(itemId)) {
                    slotItems[5 * i].add(itemInstance != null ? itemInstance.getItem() : null);
                }
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

    /**
     * Returns a warning prompt for empty or oversized candidate pools (per-slot item counts above
     * 512, gem/meta gem counts above 8), or null when the pools look reasonable. Pure string
     * building; the caller decides whether to ask the user to continue.
     */
    public String getWarningPromptIfNeeded() {
        int gemLimit = 8;
        int itemLimit = 512;
        // int enchantLimit = 8;

        List<String> emptyList = new ArrayList<String>();
        List<String> tooManyList = new ArrayList<String>();

        calculateWarnings(itemGenerator.getGemItems(), "Gems", emptyList, tooManyList, gemLimit);
        calculateWarnings(itemGenerator.getMetaGemItems(), "Meta Gems", emptyList, tooManyList, gemLimit);

        calculateWarnings(slotList[CharacterSlot.Head.value()], "Head Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Neck.value()], "Neck Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Shoulders.value()], "Shoulder Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Back.value()], "Back Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Chest.value()], "Chest Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Wrist.value()], "Wrist Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Hands.value()], "Hands Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Waist.value()], "Waist Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Legs.value()], "Legs Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Feet.value()], "Feet Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Finger1.value()], "Finger Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Trinket1.value()], "Trinket Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.MainHand.value()], "Main Hand Items", emptyList, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.OffHand.value()], "Offhand Items", null, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Ranged.value()], "Ranged Items", null, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.Projectile.value()], "Projectile Items", null, tooManyList, itemLimit);
        calculateWarnings(slotList[CharacterSlot.ProjectileBag.value()], "Projectile Bag Items", null, tooManyList, itemLimit);

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
                return "You have not selected any of the following:" + System.lineSeparator() + System.lineSeparator() + "\t" + String.join(System.lineSeparator() + "\t", emptyList) + System.lineSeparator() + System.lineSeparator() + "Do you want to continue with the optimization?";
            } else if (emptyList.size() == 0) {
                return "The following slots have a very large number of items selected :" + System.lineSeparator() + System.lineSeparator() + "\t" + String.join(System.lineSeparator() + "\t", tooManyList) + System.lineSeparator() + System.lineSeparator() + "Do you want to continue with the optimization?";
            } else {
                return "You have not selected any of the following:" + System.lineSeparator() + System.lineSeparator() + "\t" + String.join(System.lineSeparator() + "\t", emptyList) + System.lineSeparator() + System.lineSeparator() + "The following slots have a very large number of items selected :" + System.lineSeparator() + System.lineSeparator() + "\t" + String.join(System.lineSeparator() + "\t", tooManyList) + System.lineSeparator() + System.lineSeparator() + "Do you want to continue with the optimization?";
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

    public static float getOptimizationValue(Character character, CalculationsBase model) {
        return getCalculationsValue(character, model.getCharacterCalculations(character), character.getCalculationToOptimize(), character.getOptimizationRequirements());
    }

    @Override
    protected float getOptimizationValue(Character individual, CharacterCalculationsBase valuation) {
        return getCalculationsValue(individual, valuation, calculationToOptimize, requirements);
    }

    /**
     * Runs the model's calculations for the genome's character. The item-stats volatility flag is
     * switched to optimizer-managed for the duration so the model skips the gem requirement
     * bookkeeping that the search enforces through its validators.
     */
    @Override
    protected CharacterCalculationsBase getValuation(Character individual) {
        boolean oldVolatility = Item.isOptimizerManagedVolatiliy();
        try {
            Item.setOptimizerManagedVolatiliy(true);
            return model.getCharacterCalculations(individual);
        } finally {
            Item.setOptimizerManagedVolatiliy(oldVolatility);
        }
    }



    @Override
    protected Object getItem(Character individual, int slot) {
        int characterSlot = slot / 5;
        ItemInstance itemInstance = individual.get(CharacterSlot.fromValue(characterSlot));
        if (itemInstance == null) return null;
        switch (slot % 5) {
            case 0:
                return itemInstance.getItem();
            case 1:
                return itemInstance.getGem1();
            case 2:
                return itemInstance.getGem2();
            case 3:
                return itemInstance.getGem3();
            case 4:
                return itemInstance.getEnchant();
        }
        return null;
    }

    @Override
    protected Object[] getItems(Character individual) {
        Object[] items = new Object[slotCount];
        for (int i = 0; i < characterSlots; i++) {
            ItemInstance itemInstance = individual.get(CharacterSlot.fromValue(i));
            if (itemInstance != null) {
                items[i * 5] = itemInstance.getItem();
                items[i * 5 + 1] = itemInstance.getGem1();
                items[i * 5 + 2] = itemInstance.getGem2();
                items[i * 5 + 3] = itemInstance.getGem3();
                items[i * 5 + 4] = itemInstance.getEnchant();
            }
        }
        return items;
    }

    /**
     * Rebuilds the character from the 95 genes: for every slot the item gene, up to
     * {@code gemCount} gem genes and the enchant gene are turned back into an {@link ItemInstance}.
     * The new character copies the name, realm, region, race, buffs, model, calculation options,
     * class, talents and gem-requirement flag from the optimizer's base character; the recycled
     * argument is ignored because this optimizer always allocates a fresh character.
     */
    @Override
    protected Character generateIndividual(Object[] items, boolean canUseArray, Character recycledIndividual) {
        Item[] gems = new Item[3];
        ItemInstance[] itemInstances = new ItemInstance[characterSlots];
        for (int i = 0; i < characterSlots; i++) {
            Item item = (Item) items[i * 5];
            if (item != null) {
                for (int j = 0; j < 3; j++) gems[j] = null;
                for (int j = 0; j < item.getAvailabilityInformation().gemCount; j++) {
                    gems[j] = (Item) items[i * 5 + 1 + j];
                }
                itemInstances[i] = new ItemInstance(item, gems[0], gems[1], gems[2], (Enchant) items[i * 5 + 4]);
            }
        }
        // character.RecalculateSetBonuses();
        return copyCharacterWithItems(itemInstances);
    }

    private Character copyCharacterWithItems(ItemInstance[] items) {
        Character copy = new Character(this.character.getName(), this.character.getRealm(), this.character.getRegion(), this.character.getRace(),
                items, this.character.getActiveBuffs(), this.character.getCurrentModel());
        copy.setCalculationOptions(this.character.getCalculationOptions());
        copy.setCharacterClass(this.character.getCharacterClass());
        copy.assignAllTalentsFromCharacter(this.character, false);
        copy.setEnforceGemRequirements(this.character.getEnforceGemRequirements());
        return copy;
    }

    /**
     * Computes the objective value of one valuation: the requested calculation when no requirement
     * is violated, otherwise the (negative) total shortfall of the violated requirements. Invalid
     * gem requirements contribute a 100000 penalty per condition.
     */
    private static float getCalculationsValue(Character character, CharacterCalculationsBase calcs, String calculation, List<OptimizationRequirement> requirements) {
        float gemValue = -GEM_REQUIREMENT_PENALTY * character.getGemRequirementsInvalid();
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

        if (ret < 0) return ret + gemValue;
        else return OptimizerValues.getCalculationValue(character, calcs, calculation) + gemValue;
    }



    // C# string.TrimEnd(']')


    /**
     * Random gene selection: item genes ({@code slot % 5 == 0}) use the base uniform pick, the gem
     * and enchant genes of an item sample the chosen item's availability list. For the currently
     * locked character slot all five genes come from a randomly chosen locked gemmed instance.
     */
    @Override
    protected Object getRandomItem(int slot, Object[] items) {
        int characterSlot = slot / 5;
        if (lockedSlot.value() == characterSlot) {
            ItemInstance itemInstance = lockedItems.get(getRnd().next(lockedItems.size()));
            switch (slot % 5) {
                case 0:
                    return itemInstance != null ? itemInstance.getItem() : null;
                case 1:
                    return itemInstance != null ? itemInstance.getGem1() : null;
                case 2:
                    return itemInstance != null ? itemInstance.getGem2() : null;
                case 3:
                    return itemInstance != null ? itemInstance.getGem3() : null;
                case 4:
                    return itemInstance != null ? itemInstance.getEnchant() : null;
            }
        } else {
            Item item;
            switch (slot % 5) {
                case 0:
                    return super.getRandomItem(slot, items);
                case 1:
                    item = (Item) items[5 * characterSlot];
                    if (item != null) {
                        List<ItemInstance> list = item.getAvailabilityInformation().itemList;
                        return list.get(getRnd().next(list.size())).getGem1();
                    } else {
                        return null;
                    }
                case 2:
                    item = (Item) items[5 * characterSlot];
                    if (item != null) {
                        List<ItemInstance> list = item.getAvailabilityInformation().itemList;
                        return list.get(getRnd().next(list.size())).getGem2();
                    } else {
                        return null;
                    }
                case 3:
                    item = (Item) items[5 * characterSlot];
                    if (item != null) {
                        List<ItemInstance> list = item.getAvailabilityInformation().itemList;
                        return list.get(getRnd().next(list.size())).getGem3();
                    } else {
                        return null;
                    }
                case 4:
                    item = (Item) items[5 * characterSlot];
                    if (item != null) {
                        List<ItemInstance> list = item.getAvailabilityInformation().itemList;
                        return list.get(getRnd().next(list.size())).getEnchant();
                    } else {
                        return null;
                    }
            }
            return null;
        }
        return null;
    }

    /**
     * Direct upgrades are only meaningful for item genes ({@code slot % 5 == 0}); gem and enchant
     * genes fall through to the per-slot item scan, which re-optimizes the whole item instance.
     * Returns no upgrade for the other gene positions.
     */
    @Override
    protected DirectUpgradeResult<Character, CharacterCalculationsBase> lookForDirectItemUpgrades(List<Object> items, int slot, float best, Character bestIndividual, Object[] itemList) {
        if (slot % 5 != 0) {
            return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
        }
        int characterSlot = slot / 5;
        return lookForDirectItemUpgrades(slotList[characterSlot], CharacterSlot.fromValue(characterSlot), best, bestIndividual);
    }

    /**
     * Evaluates every gemmed instance in {@code items} (or the locked list when the slot is locked)
     * as a full replacement for the slot and returns the best strict improvement over {@code best}
     * with its character, or {@code (NEGATIVE_INFINITY, null)} when nothing improves. Candidates
     * that equal the currently equipped gemmed id or duplicate a unique item in the paired slot are
     * skipped; each candidate gets its own gene array so parallel evaluation cannot alias, and the
     * winner is chosen by ascending candidate index like the sequential loop.
     */
    private DirectUpgradeResult<Character, CharacterCalculationsBase> lookForDirectItemUpgrades(List<ItemInstance> items, CharacterSlot slot, float best, Character bestCharacter) {
        CharacterCalculationsBase bestValuation = null;
        boolean foundUpgrade = false;
        if (slot == lockedSlot) items = lockedItems;
        List<ItemInstance> candidates = items;
        Character baseCharacter = bestCharacter;
        ItemInstance[] itemList = bestCharacter.getRawItems().clone();
        // Candidates may run on the evaluation pool; each gets its own item array and the merge below
        // keeps the sequential strict-inequality order by ascending item index (DECISIONS #58).
        List<UpgradeResult<Character, CharacterCalculationsBase>> evaluations = evaluateAll(candidates.size(), i -> {
            ItemInstance item = candidates.get(i);
            int pairSlot = pairSlotMap[slot.value()];
            if (item == null || (baseCharacter.get(slot) != null && baseCharacter.get(slot).getGemmedId().equals(item.getGemmedId()))
                    || (pairSlot >= 0 && baseCharacter.get(CharacterSlot.fromValue(pairSlot)) != null && item.getItem().isUnique()
                            && (baseCharacter.get(CharacterSlot.fromValue(pairSlot)).getId() == item.getId()
                                    || (item.getItem().getUniqueId() != null && item.getItem().getUniqueId().contains(baseCharacter.get(CharacterSlot.fromValue(pairSlot)).getId()))))) {
                return new UpgradeResult<Character, CharacterCalculationsBase>(null, null, 0f, false);
            }
            ItemInstance[] candidateList = itemList.clone();
            candidateList[slot.value()] = item;
            Character charSwap = copyCharacterWithItems(candidateList);
            CharacterCalculationsBase calculations = model.getCharacterCalculations(charSwap);
            float value = getOptimizationValue(charSwap, calculations);
            return new UpgradeResult<Character, CharacterCalculationsBase>(charSwap, calculations, value, true);
        });
        for (int i = 0; i < candidates.size(); i++) {
            UpgradeResult<Character, CharacterCalculationsBase> evaluation = evaluations.get(i);
            if (evaluation.found() && evaluation.value() > best) {
                best = evaluation.value();
                bestValuation = evaluation.valuation();
                bestCharacter = evaluation.individual();
                foundUpgrade = true;
            }
        }
        if (foundUpgrade)
            return new DirectUpgradeResult<>(best, bestCharacter, bestValuation);
        return new DirectUpgradeResult<>(Float.NEGATIVE_INFINITY, null, null);
    }
}
