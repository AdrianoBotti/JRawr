// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchOptimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# OptimizeBatchCompleted/ProgressChanged, ComputeUpgradesCompleted/ProgressChanged and
// EvaluateUpgradeCompleted/ProgressChanged events are ported as listener interfaces fired on the
// calling thread (AGENTS §8). The Asynchronous Pattern region (AsyncOperation, SendOrPostCallback,
// ThreadPool, the *Async entry points and the IsBusy async guards) is omitted; the synchronous
// OptimizeCharacter/ComputeUpgrades/EvaluateUpgrade entry points are kept, and they raise the
// Completed events at the point the async thread start would have posted them and the ProgressChanged
// events from reportProgress where the original posted them. Per-item cancellation and progress
// reporting semantics are preserved.
package rawr.base.batch;

import rawr.base.util.KeyValuePair;

import rawr.base.optimizer.NetRandom;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.calculations.OptimizerRangeValidatorBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.optimizer.AvailableItemGenerator;
import rawr.base.optimizer.ItemInstanceOptimizer;
import rawr.base.optimizer.OptimizerBase;
import rawr.base.optimizer.OptimizerValues;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Genetic optimizer for a whole batch (roster) of characters: one search chooses items, gems and
 * enchants for every character so that the weighted sum of their optimization values is
 * maximized.
 *
 * <p>This is the joint counterpart of the single-character {@link ItemInstanceOptimizer}. It
 * extends {@link OptimizerBase}; an individual is a {@link BatchIndividual} whose gene array holds
 * one {@link ItemInstance} gene per item in the shared candidate pool (the gem/enchant
 * combination for that item), one reserved gene for the single item an upgrade-list scan is
 * evaluating, and one {@link Item} gene for every character and equipment slot that decides
 * whether that character equips the item. Because the gemmed instance is stored once per pool
 * item, all characters that choose the same item share its gemming; the per-character validators
 * still enforce the unique-equipped rules of the finger, trinket and weapon pairs. A
 * {@link BatchValuation} carries the per-character values and their weighted total.</p>
 *
 * <h2>Entry points</h2>
 *
 * <ul>
 *   <li>{@link #optimizeCharacter(int, BatchValuation[])} - full search; the caller's one-element
 *       array receives the best valuation.</li>
 *   <li>{@link #computeUpgrades(int, Item)} - evaluates every relevant item (or one given item) as
 *       an addition to the starting equipment and returns per-slot upgrade lists.</li>
 *   <li>{@link #evaluateUpgrade(int, ItemInstance)} - scores one concrete gemmed item instance as
 *       an addition and returns the value delta.</li>
 * </ul>
 *
 * <p>Each entry point runs synchronously and refuses to start while another operation is in
 * flight ({@link #isBusy()}). It fires the matching completed listener just before returning and
 * rethrows a captured error wrapped in {@link RuntimeException} after the event; normal
 * cancellation is reported through the event and typically yields a {@code null} result (or a
 * zero value) instead of throwing. {@link #cancelAsync()} requests cancellation.</p>
 *
 * <h2>Event surface</h2>
 *
 * <p>The C# asynchronous pattern (events posted back to the UI thread, the {@code *Async} wrappers
 * and their busy guards) is not part of the port; the events survive as the listener interfaces
 * declared here, fired on the calling thread from the synchronous methods.
 * {@link #reportProgress(int, float)} dispatches the per-operation progress listeners while the
 * search evaluates candidates and once with 100 at the end; the completed listeners fire with the
 * result, the captured error and the cancellation flag. Listener lists are
 * {@link java.util.concurrent.CopyOnWriteArrayList}s, so subscribing or unsubscribing during a
 * notification is safe.</p>
 *
 * <h2>Faithfully ported details</h2>
 *
 * <ul>
 *   <li>The upgrade scan passes the two progress percentages to
 *       {@link ComputeUpgradesProgressChangedEventArgs} in the original's swapped order
 *       (SUSPECTED_BUGS #39); see {@link #reportProgress(int, float)}.</li>
 *   <li>The batch value in {@link OptimizeBatchCompletedEventArgs} is computed only when at least
 *       one completed listener is registered; the original synchronous entry point never computed
 *       it, and computing it eagerly would add a model evaluation to the search.</li>
 *   <li>{@link #buildMutantIndividual(BatchIndividual, BatchIndividual)} mostly delegates to the
 *       base mutation, but with a 10% chance runs a two-socket gem-swap mutation that also tries
 *       to repair the jewelcrafting extra-gem count of characters that gained or lost a jeweled
 *       item (heuristic, ported as written).</li>
 * </ul>
 */
public class BatchOptimizer extends OptimizerBase<Object, BatchIndividual, BatchValuation> {
    /** Replaces the C# `OptimizeBatchProgressChanged` event (OptimizeCharacterProgressChangedEventHandler). */
    @FunctionalInterface
    public interface OptimizeBatchProgressChangedListener {
        void optimizeBatchProgressChanged(BatchOptimizer sender, OptimizeCharacterProgressChangedEventArgs e);
    }

    /** Replaces the C# `OptimizeBatchCompleted` event. */
    @FunctionalInterface
    public interface OptimizeBatchCompletedListener {
        void optimizeBatchCompleted(BatchOptimizer sender, OptimizeBatchCompletedEventArgs e);
    }

    /** Replaces the C# `ComputeUpgradesProgressChanged` event. */
    @FunctionalInterface
    public interface ComputeUpgradesProgressChangedListener {
        void computeUpgradesProgressChanged(BatchOptimizer sender, ComputeUpgradesProgressChangedEventArgs e);
    }

    /** Replaces the C# `ComputeUpgradesCompleted` event. */
    @FunctionalInterface
    public interface ComputeUpgradesCompletedListener {
        void computeUpgradesCompleted(BatchOptimizer sender, ComputeUpgradesCompletedEventArgs e);
    }

    /** Replaces the C# `EvaluateUpgradeProgressChanged` event (ProgressChangedEventHandler). */
    @FunctionalInterface
    public interface EvaluateUpgradeProgressChangedListener {
        void evaluateUpgradeProgressChanged(BatchOptimizer sender, ProgressChangedEventArgs e);
    }

    /** Replaces the C# `EvaluateUpgradeCompleted` event. */
    @FunctionalInterface
    public interface EvaluateUpgradeCompletedListener {
        void evaluateUpgradeCompleted(BatchOptimizer sender, EvaluateUpgradeCompletedEventArgs e);
    }

    private final List<OptimizeBatchProgressChangedListener> optimizeBatchProgressChangedListeners = new CopyOnWriteArrayList<OptimizeBatchProgressChangedListener>();
    private final List<OptimizeBatchCompletedListener> optimizeBatchCompletedListeners = new CopyOnWriteArrayList<OptimizeBatchCompletedListener>();
    private final List<ComputeUpgradesProgressChangedListener> computeUpgradesProgressChangedListeners = new CopyOnWriteArrayList<ComputeUpgradesProgressChangedListener>();
    private final List<ComputeUpgradesCompletedListener> computeUpgradesCompletedListeners = new CopyOnWriteArrayList<ComputeUpgradesCompletedListener>();
    private final List<EvaluateUpgradeProgressChangedListener> evaluateUpgradeProgressChangedListeners = new CopyOnWriteArrayList<EvaluateUpgradeProgressChangedListener>();
    private final List<EvaluateUpgradeCompletedListener> evaluateUpgradeCompletedListeners = new CopyOnWriteArrayList<EvaluateUpgradeCompletedListener>();

    public void addOptimizeBatchProgressChangedListener(OptimizeBatchProgressChangedListener listener) {
        optimizeBatchProgressChangedListeners.add(listener);
    }

    public void removeOptimizeBatchProgressChangedListener(OptimizeBatchProgressChangedListener listener) {
        optimizeBatchProgressChangedListeners.remove(listener);
    }

    public void addOptimizeBatchCompletedListener(OptimizeBatchCompletedListener listener) {
        optimizeBatchCompletedListeners.add(listener);
    }

    public void removeOptimizeBatchCompletedListener(OptimizeBatchCompletedListener listener) {
        optimizeBatchCompletedListeners.remove(listener);
    }

    public void addComputeUpgradesProgressChangedListener(ComputeUpgradesProgressChangedListener listener) {
        computeUpgradesProgressChangedListeners.add(listener);
    }

    public void removeComputeUpgradesProgressChangedListener(ComputeUpgradesProgressChangedListener listener) {
        computeUpgradesProgressChangedListeners.remove(listener);
    }

    public void addComputeUpgradesCompletedListener(ComputeUpgradesCompletedListener listener) {
        computeUpgradesCompletedListeners.add(listener);
    }

    public void removeComputeUpgradesCompletedListener(ComputeUpgradesCompletedListener listener) {
        computeUpgradesCompletedListeners.remove(listener);
    }

    public void addEvaluateUpgradeProgressChangedListener(EvaluateUpgradeProgressChangedListener listener) {
        evaluateUpgradeProgressChangedListeners.add(listener);
    }

    public void removeEvaluateUpgradeProgressChangedListener(EvaluateUpgradeProgressChangedListener listener) {
        evaluateUpgradeProgressChangedListeners.remove(listener);
    }

    public void addEvaluateUpgradeCompletedListener(EvaluateUpgradeCompletedListener listener) {
        evaluateUpgradeCompletedListeners.add(listener);
    }

    public void removeEvaluateUpgradeCompletedListener(EvaluateUpgradeCompletedListener listener) {
        evaluateUpgradeCompletedListeners.remove(listener);
    }

    /** Raises OptimizeBatchProgressChanged on the calling thread (no-op without listeners). */
    protected void onOptimizeBatchProgressChanged(OptimizeCharacterProgressChangedEventArgs e) {
        for (OptimizeBatchProgressChangedListener listener : optimizeBatchProgressChangedListeners) {
            listener.optimizeBatchProgressChanged(this, e);
        }
    }

    /** Raises OptimizeBatchCompleted on the calling thread. */
    protected void onOptimizeBatchCompleted(OptimizeBatchCompletedEventArgs e) {
        for (OptimizeBatchCompletedListener listener : optimizeBatchCompletedListeners) {
            listener.optimizeBatchCompleted(this, e);
        }
    }

    /** Raises ComputeUpgradesProgressChanged on the calling thread. */
    protected void onComputeUpgradesProgressChanged(ComputeUpgradesProgressChangedEventArgs e) {
        for (ComputeUpgradesProgressChangedListener listener : computeUpgradesProgressChangedListeners) {
            listener.computeUpgradesProgressChanged(this, e);
        }
    }

    /** Raises ComputeUpgradesCompleted on the calling thread. */
    protected void onComputeUpgradesCompleted(ComputeUpgradesCompletedEventArgs e) {
        for (ComputeUpgradesCompletedListener listener : computeUpgradesCompletedListeners) {
            listener.computeUpgradesCompleted(this, e);
        }
    }

    /** Raises EvaluateUpgradeProgressChanged on the calling thread. */
    protected void onEvaluateUpgradeProgressChanged(ProgressChangedEventArgs e) {
        for (EvaluateUpgradeProgressChangedListener listener : evaluateUpgradeProgressChangedListeners) {
            listener.evaluateUpgradeProgressChanged(this, e);
        }
    }

    /** Raises EvaluateUpgradeCompleted on the calling thread. */
    protected void onEvaluateUpgradeCompleted(EvaluateUpgradeCompletedEventArgs e) {
        for (EvaluateUpgradeCompletedListener listener : evaluateUpgradeCompletedListeners) {
            listener.evaluateUpgradeCompleted(this, e);
        }
    }

    private List<Character> batchList;
    private List<CalculationsBase> modelList;
    private List<Float> weightList;

    private static final int characterSlots = 19;

    private BatchIndividual startIndividual;

    private static class UniqueItemValidator extends OptimizerRangeValidatorBase<Object> {
        @Override
        public boolean isValid(Object[] items) {
            if (items[startSlot] != null && items[endSlot] != null) {
                Item itema = (Item) items[startSlot];
                Item itemb = (Item) items[endSlot];
                return !(itema.isUnique() && (itema.getId() == itemb.getId() || (itema.getUniqueId() != null && itema.getUniqueId().contains(itemb.getId()))));
            } else {
                return true;
            }
        }
    }

    /**
     * Builds the optimizer for a roster: every character is cloned so the search never mutates
     * the caller's instances, its model is resolved from the character's current model name, and
     * its weight is kept. The candidate pool is generated from the first character's availability
     * list but covers all characters and models. Throws {@link IllegalArgumentException} when the
     * roster is null, empty or its first character is null.
     */
    public BatchOptimizer(List<KeyValuePair<Character, Float>> batchList, boolean overrideRegem, boolean overrideReenchant, boolean templateGemsEnabled) {
        if (batchList == null || batchList.isEmpty() || batchList.get(0).key() == null) throw new IllegalArgumentException("Batch list must have at least one element.");
        this.batchList = new ArrayList<Character>();
        this.modelList = new ArrayList<CalculationsBase>();
        this.weightList = new ArrayList<Float>();
        for (KeyValuePair<Character, Float> kvp : batchList) {
            this.batchList.add(kvp.key().clone());
            this.modelList.add(Calculations.getModel(kvp.key().getCurrentModel()));
            this.weightList.add(kvp.value());
        }

        initializeItemCache(batchList.get(0).key().getAvailableItems(), overrideRegem, overrideReenchant, templateGemsEnabled);
    }

    private void initializeItemCache(List<String> availableItems, boolean overrideRegem, boolean overrideReenchant, boolean templateGemsEnabled) {
        populateAvailableIds(availableItems, templateGemsEnabled, overrideRegem, overrideReenchant);
    }

    private enum OptimizationOperation {
        OptimizeCharacter,
        ComputeUpgrades,
        EvaluateUpgrade
    }

    private OptimizationOperation currentOperation;

    private boolean isBusy;

    public boolean isBusy() {
        return isBusy;
    }

    @Override
    /**
     * Dispatches progress to the listener of the operation in progress: the batch value while a
     * full search runs, the item-scan counters while upgrade lists are computed (with the
     * original's swapped percentage arguments, see the inline note and SUSPECTED_BUGS #39), and a
     * plain percentage while a single upgrade is evaluated. Silently does nothing with no active
     * operation or after cancellation was requested.
     */
    protected void reportProgress(int progressPercentage, float bestValue) {
        if (cancellationPending || currentOperation == null) return;
        switch (currentOperation) {
            case OptimizeCharacter:
                onOptimizeBatchProgressChanged(new OptimizeCharacterProgressChangedEventArgs(progressPercentage, bestValue));
                break;
            case ComputeUpgrades:
                // The C# call site passes (itemProgressPercentage, progressPercentage, currentItem) to a
                // ctor declared as (progressPercentage, itemProgressPercentage, currentItem), so the two
                // percentages come out swapped in the event args; preserved faithfully (SUSPECTED_BUGS #39).
                onComputeUpgradesProgressChanged(new ComputeUpgradesProgressChangedEventArgs(itemProgressPercentage, progressPercentage, currentItem));
                break;
            case EvaluateUpgrade:
                onEvaluateUpgradeProgressChanged(new ProgressChangedEventArgs(progressPercentage, null));
                break;
            default:
                break;
        }
    }



    /**
     * Runs the full genetic search over the batch and returns the best individual found, or
     * {@code null} when the search failed or was cancelled; the one-element {@code bestValuation}
     * array receives the best {@link BatchValuation}. The completed event fires before this
     * method returns or throws. Throws {@link IllegalStateException} when busy, and
     * {@link RuntimeException} wrapping the captured search error after the event.
     */
    public BatchIndividual optimizeCharacter(int thoroughness, BatchValuation[] bestValuation) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        OptimizationResult<BatchIndividual, BatchValuation> result = privateOptimizeBatch(thoroughness, error);
        BatchIndividual optimizedBatch = result.individual();
        bestValuation[0] = result.bestValuation();
        // The C# async thread start computes the batch value for the event payload; only do the extra
        // valuation when a listener actually consumes it so the synchronous search behavior is
        // unchanged (the sync C# entry point never computed it).
        float optimizedBatchValue = 0.0f;
        if (optimizedBatch != null && !optimizeBatchCompletedListeners.isEmpty()) {
            optimizedBatchValue = getOptimizationValue(optimizedBatch);
        }
        boolean cancelled = cancellationPending;
        isBusy = false;
        onOptimizeBatchCompleted(new OptimizeBatchCompletedEventArgs(optimizedBatch, bestValuation[0], optimizedBatchValue, error[0], cancelled));
        if (error[0] != null) throw new RuntimeException(error[0]);
        return optimizedBatch;
    }

    private OptimizationResult<BatchIndividual, BatchValuation> privateOptimizeBatch(int thoroughness, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.thoroughness = thoroughness;

        currentOperation = OptimizationOperation.OptimizeCharacter;
        OptimizationResult<BatchIndividual, BatchValuation> result = null;
        slotItems[itemList.size()].clear();
        upgradeItems = null;

        try {
            result = optimize(startIndividual, 0.0f);
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, result != null ? result.bestValue() : 0f);
        return result != null ? result : new OptimizationResult<>(null, 0f, null, false);
    }

    /**
     * Evaluates every relevant item (or the single item passed in, when non-null) as an addition
     * to the batch's starting equipment and returns a map from gear slot to the upgrades found
     * for it; the result may be {@code null} or partial when the scan was cancelled or aborted.
     * The completed event carries the same map before this method returns or rethrows. Throws
     * {@link IllegalStateException} when busy and {@link RuntimeException} for a captured error.
     */
    public Map<CharacterSlot, List<ComparisonCalculationUpgrades>> computeUpgrades(int thoroughness, Item singleItemUpgrades) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = privateComputeUpgrades(thoroughness, singleItemUpgrades, error);
        boolean cancelled = cancellationPending;
        isBusy = false;
        onComputeUpgradesCompleted(new ComputeUpgradesCompletedEventArgs(upgrades, error[0], cancelled));
        if (error[0] != null) throw new RuntimeException(error[0]);
        return upgrades;
    }

    private int itemProgressPercentage = 0;
    private String currentItem = "";

    private Map<CharacterSlot, List<ComparisonCalculationUpgrades>> privateComputeUpgrades(int thoroughness, Item singleItemUpgrades, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.thoroughness = thoroughness;

        currentOperation = OptimizationOperation.ComputeUpgrades;
        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = null;
        try {
            upgrades = new LinkedHashMap<CharacterSlot, List<ComparisonCalculationUpgrades>>();

            Item[] items = ItemCache.getRelevantItems(modelList.get(0), batchList.get(0).getRace());
            CharacterSlot[] slots = new CharacterSlot[] { CharacterSlot.Back, CharacterSlot.Chest, CharacterSlot.Feet, CharacterSlot.Finger1, CharacterSlot.Hands, CharacterSlot.Head, CharacterSlot.Legs, CharacterSlot.MainHand, CharacterSlot.Neck, CharacterSlot.OffHand, CharacterSlot.Projectile, CharacterSlot.ProjectileBag, CharacterSlot.Ranged, CharacterSlot.Shoulders, CharacterSlot.Trinket1, CharacterSlot.Waist, CharacterSlot.Wrist };
            for (CharacterSlot slot : slots)
                upgrades.put(slot, new ArrayList<ComparisonCalculationUpgrades>());

            slotItems[itemList.size()].clear();
            upgradeItems = null;

            float baseValue = getOptimizationValue(startIndividual);

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
                boolean relevantSlot = false;
                CharacterSlot itemSlot = Character.getCharacterSlotByItemSlot(item.getSlot());
                for (CharacterSlot slot : slots) {
                    if (slot == itemSlot) {
                        relevantSlot = true;
                        break;
                    }
                }
                if (relevantSlot) {
                    reportProgress(0, 0);
                    upgradeItems = itemGenerator.getPossibleGemmedItemsForItem(item, Integer.toString(item.getId()));
                    populateUpgradeItems(item);
                    Object[] genes = startIndividual.items.clone();
                    genes[itemList.size()] = upgradeItems.get(0);
                    BatchIndividual batch = generateIndividual(genes, true, null);
                    OptimizationResult<BatchIndividual, BatchValuation> result = optimize(batch, getOptimizationValue(batch));
                    BatchIndividual bestCharacter = result.individual();
                    float best = result.bestValue();
                    if (best > baseValue) {
                        ItemInstance bestItem = bestCharacter.availableItem[itemList.size()];
                        ComparisonCalculationUpgrades itemCalc = new ComparisonCalculationUpgrades();
                        itemCalc.setItemInstance(bestItem);
                        itemCalc.setName(item.getName());
                        itemCalc.setEquipped(false);
                        itemCalc.setOverallPoints(best - baseValue);

                        for (CharacterSlot slot : slots) {
                            if (item.fitsInSlot(slot, batchList.get(0))) {
                                upgrades.get(slot).add(itemCalc);
                            }
                        }
                    }
                    removeUpgradeItems(item);
                }
            }
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, 0f);
        return upgrades;
    }

    /**
     * Scores one concrete gemmed item instance ({@code upgrade}) as an addition to the batch's
     * starting equipment and returns the value delta (best value minus base value) in the same
     * model-defined units as the batch score. The completed event fires before this method
     * returns; its comparison payload is always {@code null}, matching the original call site.
     * Throws {@link IllegalStateException} when busy and {@link RuntimeException} for a captured
     * error.
     */
    public float evaluateUpgrade(int thoroughness, ItemInstance upgrade) {
        if (isBusy) throw new IllegalStateException("Optimizer is working on another operation.");
        isBusy = true;
        cancellationPending = false;
        Exception[] error = new Exception[1];
        float upgradeValue = privateEvaluateUpgrade(thoroughness, upgrade, error);
        boolean cancelled = cancellationPending;
        isBusy = false;
        onEvaluateUpgradeCompleted(new EvaluateUpgradeCompletedEventArgs(upgradeValue, null, error[0], cancelled));
        if (error[0] != null) throw new RuntimeException(error[0]);
        return upgradeValue;
    }

    private float privateEvaluateUpgrade(int thoroughness, ItemInstance upgrade, Exception[] error) {
        if (!itemCacheInitialized) throw new IllegalStateException("Optimization item cache was not initialized.");
        error[0] = null;
        this.thoroughness = thoroughness;

        currentOperation = OptimizationOperation.EvaluateUpgrade;
        float upgradeValue = 0f;
        try {
            float baseValue = getOptimizationValue(startIndividual);

            ItemInstance item = upgrade;
            upgradeItems = new ArrayList<ItemInstance>();
            upgradeItems.add(item);
            populateUpgradeItems(item.getItem());
            Object[] genes = startIndividual.items.clone();
            genes[itemList.size()] = upgradeItems.get(0);
            BatchIndividual batch = generateIndividual(genes, true, null);
            OptimizationResult<BatchIndividual, BatchValuation> result = optimize(batch, getOptimizationValue(batch));
            float best = result.bestValue();
            removeUpgradeItems(item.getItem());
            upgradeValue = best - baseValue;
        } catch (Exception ex) {
            error[0] = ex;
        }

        reportProgress(100, 0f);
        return upgradeValue;
    }

    private boolean itemCacheInitialized;

    private List<ItemInstance> upgradeItems;
    private List<Item> itemList;
    private List<Object>[] slotItemList;
    private Map<Integer, Integer> indexFromId;

    private AvailableItemGenerator itemGenerator;

    private void populateUpgradeItems(Item item) {
        slotItems[itemList.size()] = new ArrayList<Object>(upgradeItems);
        for (int i = 0; i < characterSlots; i++) {
            if (item.fitsInSlot(CharacterSlot.fromValue(i), batchList.get(0))) {
                for (int c = 0; c < batchList.size(); c++) {
                    slotItems[itemList.size() + 1 + c * characterSlots + i].add(item);
                }
            }
        }
    }

    private void removeUpgradeItems(Item item) {
        upgradeItems = null;
        slotItems[itemList.size()].clear();
        for (int i = 0; i < characterSlots; i++) {
            if (item.fitsInSlot(CharacterSlot.fromValue(i), batchList.get(0))) {
                for (int c = 0; c < batchList.size(); c++) {
                    List<Object> list = slotItems[itemList.size() + 1 + c * characterSlots + i];
                    list.remove(list.size() - 1);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateAvailableIds(List<String> availableItems, boolean templateGemsEnabled, boolean overrideRegem, boolean overrideReenchant) {
        itemGenerator = new AvailableItemGenerator(availableItems, false, templateGemsEnabled, overrideRegem, overrideReenchant, false, batchList.toArray(new Character[0]), modelList.toArray(new CalculationsBase[0]));
        List<ItemInstance>[] slotList = itemGenerator.getSlotItems();
        slotItemList = new List[characterSlots];

        Set<Integer> seenItemIds = new LinkedHashSet<Integer>();
        itemList = new ArrayList<Item>();
        indexFromId = new LinkedHashMap<Integer, Integer>();
        for (int i = 0; i < slotList.length; i++) {
            Set<Integer> seenSlotItemIds = new LinkedHashSet<Integer>();
            slotItemList[i] = new ArrayList<Object>();
            for (ItemInstance itemInstance : slotList[i]) {
                if (itemInstance != null && itemInstance.getItem() != null) {
                    Item item = itemInstance.getItem();
                    if (seenSlotItemIds.add(itemInstance.getId())) {
                        slotItemList[i].add(item);
                    }
                    if (seenItemIds.add(itemInstance.getId())) {
                        indexFromId.put(item.getId(), itemList.size());
                        itemList.add(item);
                    }
                }
            }
        }

        slotCount = itemList.size() + 1 + batchList.size() * characterSlots;

        slotItems = new List[slotCount];

        for (int i = 0; i < itemList.size(); i++) {
            slotItems[i] = new ArrayList<Object>(itemList.get(i).getAvailabilityInformation().itemList);
        }
        slotItems[itemList.size()] = new ArrayList<Object>();
        for (int c = 0; c < batchList.size(); c++) {
            for (int i = 0; i < characterSlots; i++) {
                slotItems[itemList.size() + 1 + c * characterSlots + i] = slotItemList[i];
            }
        }

        validators = new ArrayList<OptimizerRangeValidatorBase<Object>>();

        for (int c = 0; c < batchList.size(); c++) {
            int offset = itemList.size() + 1 + c * characterSlots;
            UniqueItemValidator fingerValidator = new UniqueItemValidator();
            fingerValidator.startSlot = offset + CharacterSlot.Finger1.value();
            fingerValidator.endSlot = offset + CharacterSlot.Finger2.value();
            validators.add(fingerValidator);
            UniqueItemValidator trinketValidator = new UniqueItemValidator();
            trinketValidator.startSlot = offset + CharacterSlot.Trinket1.value();
            trinketValidator.endSlot = offset + CharacterSlot.Trinket2.value();
            validators.add(trinketValidator);
            UniqueItemValidator weaponValidator = new UniqueItemValidator();
            weaponValidator.startSlot = offset + CharacterSlot.MainHand.value();
            weaponValidator.endSlot = offset + CharacterSlot.OffHand.value();
            validators.add(weaponValidator);
        }

        Object[] items = new Object[slotCount];
        for (int i = 0; i < itemList.size(); i++) {
            items[i] = itemList.get(i).getAvailabilityInformation().itemList.get(0);
        }
        for (int c = 0; c < batchList.size(); c++) {
            for (int i = 0; i < characterSlots; i++) {
                ItemInstance itemInstance = batchList.get(c).getItem(i);
                if (itemInstance != null && itemInstance.getId() != 0) {
                    Integer index = indexFromId.get(itemInstance.getId());
                    if (index != null) {
                        // only set the item if it's marked as available, otherwise leave it empty
                        items[index] = itemInstance;
                        items[itemList.size() + 1 + c * characterSlots + i] = itemInstance.getItem();
                    }
                }
            }
        }
        startIndividual = new BatchIndividual(items, itemList.size(), indexFromId, null, batchList);

        itemCacheInitialized = true;
    }

    @Override
    protected float getOptimizationValue(BatchIndividual individual, BatchValuation valuation) {
        return valuation.optimizedValue;
    }

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





    @Override
    /**
     * Computes the batch score for an individual: the sum over characters of the roster weight
     * times the character's model value for the calculation-to-optimize, with unmet optimization
     * requirements and invalid gem requirements subtracted as penalties so the search avoids
     * infeasible gear rather than being forbidden to explore it. Also fills the returned
     * {@link BatchValuation} with the per-character values and the total.
     */
    protected BatchValuation getValuation(BatchIndividual individual) {
        BatchValuation valuation = new BatchValuation();
        valuation.optimizedBatchValue = new ArrayList<Float>();
        float value = 0f;
        for (int i = 0; i < batchList.size(); i++) {
            float characterValue = getCalculationsValue(individual.character.get(i), modelList.get(i).getCharacterCalculations(individual.character.get(i), null, false, false, false), batchList.get(i).getCalculationToOptimize(), batchList.get(i).getOptimizationRequirements());
            valuation.optimizedBatchValue.add(characterValue);
            value += weightList.get(i) * characterValue;
        }
        valuation.optimizedValue = value;
        return valuation;
    }

    @Override
    protected Object getItem(BatchIndividual individual, int slot) {
        return individual.items[slot];
    }

    @Override
    protected Object[] getItems(BatchIndividual individual) {
        return individual.items;
    }

    @Override
    protected BatchIndividual generateIndividual(Object[] items, boolean canUseArray, BatchIndividual recycledIndividual) {
        return new BatchIndividual(canUseArray ? items : items.clone(), itemList.size(), indexFromId, upgradeItems != null ? upgradeItems.get(0).getItem() : null, batchList);
    }

    @Override
    protected BatchIndividual buildMutantIndividual(BatchIndividual parent, BatchIndividual recycledIndividual) {
        boolean[] successful = new boolean[1];
        BatchIndividual mutant = null;
        if (getRnd().nextDouble() < 0.9) {
            return super.buildMutantIndividual(parent, recycledIndividual);
        } else {
            mutant = buildSwapGemMutantCharacter(parent, successful);
            if (!successful[0]) {
                return super.buildMutantIndividual(parent, recycledIndividual);
            }
        }
        return mutant;
    }

    private static class GemInformation {
        public int slot;
        public int index;
        public Item gem;
        public ItemSlot socket;
    }

    private ItemInstance replaceGem(ItemInstance item, int index, Item gem) {
        ItemInstance copy = new ItemInstance(item.getItem(), item.getGem1(), item.getGem2(), item.getGem3(), item.getEnchant());
        copy.setGem(index, gem);
        return copy;
    }

    private BatchIndividual buildSwapGemMutantCharacter(BatchIndividual parent, boolean[] successful) {
        Object[] items = getItems(parent).clone();
        successful[0] = false;

        // do the work
        int c;
        Character character;
        GemInformation mutation1 = new GemInformation();
        GemInformation mutation2 = new GemInformation();
        NetRandom rand = getRnd();
        int tries = 0;
        do {
            c = rand.next(batchList.size());
            character = parent.character.get(c);
            // build a list of possible mutation points
            // make sure not to do meta gem swaps
            List<GemInformation> locationList = new ArrayList<GemInformation>();
            for (int slot = 0; slot < characterSlots; slot++) {
                if (character.getItem(slot) != null) {
                    for (int i = 1; i <= 3; i++) {
                        Item gem = character.getItem(slot).getGem(i);
                        if (gem != null && gem.getSlot() != ItemSlot.Meta) {
                            GemInformation gemInformation = new GemInformation();
                            gemInformation.slot = slot;
                            gemInformation.index = i;
                            gemInformation.gem = gem;
                            gemInformation.socket = character.getItem(slot).getItem().getSocketColor(i);
                            locationList.add(gemInformation);
                        }
                    }
                }
            }

            if (locationList.size() > 1) {
                // randomly select mutation point
                boolean promising;
                do {
                    promising = true;
                    int mutationIndex1 = rand.next(locationList.size());
                    int mutationIndex2 = rand.next(locationList.size());
                    mutation1 = locationList.get(mutationIndex1);
                    mutation2 = locationList.get(mutationIndex2);
                    if (mutation1.gem.getSlot() == mutation2.gem.getSlot()) promising = false;
                    if (mutation1.socket == mutation2.socket) promising = false;
                    int matchNow = 0;
                    if (Item.gemMatchesSlot(mutation1.gem, mutation1.socket)) matchNow++;
                    if (Item.gemMatchesSlot(mutation2.gem, mutation2.socket)) matchNow++;
                    if (matchNow == 2) promising = false;
                    int matchThen = 0;
                    if (Item.gemMatchesSlot(mutation1.gem, mutation2.socket)) matchThen++;
                    if (Item.gemMatchesSlot(mutation2.gem, mutation1.socket)) matchThen++;
                    if (tries < 50) {
                        if (matchThen <= matchNow) promising = false;
                    } else {
                        // allow 1 to 1 trade, because the other socket bonus might be better
                        if (matchThen < matchNow || matchThen == 0) promising = false;
                    }
                    tries++;
                } while (tries % 10 != 0 && !promising);

                // mutate
                if (promising) {
                    ItemInstance item1 = replaceGem(character.getItem(mutation1.slot), mutation1.index, mutation2.gem);
                    ItemInstance item2 = replaceGem(character.getItem(mutation2.slot), mutation2.index, mutation1.gem);
                    if (itemAvailable(item1) && itemAvailable(item2)) {
                        successful[0] = true;
                        items[(upgradeItems != null && item1.getId() == upgradeItems.get(0).getId()) ? itemList.size() : indexFromId.get(item1.getId())] = item1;
                        items[(upgradeItems != null && item2.getId() == upgradeItems.get(0).getId()) ? itemList.size() : indexFromId.get(item2.getId())] = item2;
                    }
                }
            }
        } while (tries < 100 && !successful[0]);

        if (successful[0] && (mutation1.gem.isJewelersGem() || mutation2.gem.isJewelersGem())) {
            // jeweler preserving mutation
            // by making the swap the character at c will remain at 3 jewelers, but characters that don't have that item
            // might now be left with a suboptimal count
            // heuristic we use is that jeweler gems are better than any other
            // and that by replacing an existing gem will increase value, specially if we can do it at
            // a spot that doesn't match color
            if (mutation2.gem.isJewelersGem()) {
                GemInformation tmp = mutation1;
                mutation1 = mutation2;
                mutation2 = tmp;
            }
            // we're moving jeweler gem from mutation1 to mutation2
            // problematic cases are characters that have item1, but not item2 or have item2 but not item1
            // in that case we'll either have too many or not enough
            List<Character> fixedList = new ArrayList<Character>();
            List<Character> oneDownList = new ArrayList<Character>();
            List<Character> oneUpList = new ArrayList<Character>();
            for (Character affectedCharacter : parent.character) {
                boolean eq1 = isEquipped(affectedCharacter, character.getItem(mutation1.slot));
                boolean eq2 = isEquipped(affectedCharacter, character.getItem(mutation2.slot));
                if (eq1 && !eq2 && affectedCharacter.getJewelersGemCount() <= 3) {
                    oneDownList.add(affectedCharacter);
                } else if (!eq1 && eq2 && affectedCharacter.getJewelersGemCount() >= 3) {
                    oneUpList.add(affectedCharacter);
                } else {
                    fixedList.add(affectedCharacter);
                }
            }
            // not to complicate things too much we can only fix characters if it does not affect
            // those that are already fixed

            for (int i = 0; i < oneUpList.size(); i++) {
                Character affectedCharacter = oneUpList.get(i);
                // see if it has an item with a jeweler gem that does not appear on fixed list
                for (int slot = 0; slot < 2 * characterSlots; slot++) {
                    ItemInstance originalitem = affectedCharacter.getItem(rand.next(characterSlots));
                    ItemInstance item = originalitem;
                    if (item != null && itemHasJewelerGem(item)) {
                        boolean ok = true;
                        for (Character ch : fixedList) {
                            if (isEquipped(ch, item)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            // we're clear, we can remove the meta gem
                            // anything will be better than breaking the jeweler constraint
                            // pick from available instances that don't have jewelers gems
                            List<ItemInstance> list = new ArrayList<ItemInstance>();
                            List<ItemInstance> availableList = (upgradeItems != null && item.getId() == upgradeItems.get(0).getId()) ? upgradeItems : item.getItem().getAvailabilityInformation().itemList;
                            for (ItemInstance it : availableList) {
                                if (!itemHasJewelerGem(it)) {
                                    list.add(it);
                                }
                            }
                            if (list.size() > 0) {
                                item = list.get(rand.next(list.size()));
                                // we got it
                                items[(upgradeItems != null && item.getId() == upgradeItems.get(0).getId()) ? itemList.size() : indexFromId.get(item.getId())] = item;
                                // any item in up list that had it can be removed
                                for (int j = oneUpList.size() - 1; j > i; j--) {
                                    if (isEquipped(oneUpList.get(j), originalitem)) {
                                        fixedList.add(oneUpList.get(j));
                                        oneUpList.remove(j);
                                    }
                                }
                                fixedList.add(oneUpList.get(i));
                                oneUpList.remove(i);
                                i--;
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < oneDownList.size(); i++) {
                Character affectedCharacter = oneDownList.get(i);
                // see if it has an item with a free gem that does not appear on fixed list
                for (int slot = 0; slot < 2 * characterSlots; slot++) {
                    ItemInstance originalitem = affectedCharacter.getItem(rand.next(characterSlots));
                    ItemInstance item = originalitem;
                    if (item != null && itemHasNonJewelerGem(item)) {
                        boolean ok = true;
                        for (Character ch : fixedList) {
                            if (isEquipped(ch, item)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            // we're clear, we can remove the meta gem
                            // anything will be better than breaking the jeweler constraint
                            // pick from available instances that don't have jewelers gems
                            int pickTries = 0;
                            if (originalitem.getItem().getAvailabilityInformation() == null) {
                                itemGenerator.generateItemAvailabilityInformation(originalitem.getItem());
                            }
                            do {
                                item = originalitem.clone();
                                int g = 1 + rand.next(originalitem.getItem().getAvailabilityInformation().gemCount);
                                item.setGem(g, mutation1.gem);
                                pickTries++;
                            } while (pickTries <= 5 && !itemAvailable(item));
                            if (pickTries <= 5) {
                                // we got it
                                items[(upgradeItems != null && item.getId() == upgradeItems.get(0).getId()) ? itemList.size() : indexFromId.get(item.getId())] = item;
                                // any item in down list that had it can be removed
                                for (int j = oneDownList.size() - 1; j > i; j--) {
                                    if (isEquipped(oneDownList.get(j), originalitem)) {
                                        fixedList.add(oneDownList.get(j));
                                        oneDownList.remove(j);
                                    }
                                }
                                fixedList.add(oneDownList.get(i));
                                oneDownList.remove(i);
                                i--;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (successful[0]) {
            return generateIndividual(items, true, null);
        }
        return null;
    }

    private boolean itemAvailable(ItemInstance item) {
        return ((upgradeItems != null && item.getId() == upgradeItems.get(0).getId() && upgradeItems.contains(item)) || ((upgradeItems == null || item.getId() != upgradeItems.get(0).getId()) && item.getItem().getAvailabilityInformation().itemAvailable.containsKey(item.getGemmedId())));
    }

    private static boolean itemHasJewelerGem(ItemInstance item) {
        for (int g = 1; g <= 3; g++) {
            Item gem = item.getGem(g);
            if (gem != null && gem.isJewelersGem()) {
                return true;
            }
        }
        return false;
    }

    private static boolean itemHasJewelerGem(ItemInstance item, Item jewelerGem) {
        for (int g = 1; g <= 3; g++) {
            Item gem = item.getGem(g);
            if (gem != null && gem.getId() == jewelerGem.getId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean itemHasNonJewelerGem(ItemInstance item) {
        boolean ok = false;
        for (int g = 1; g <= 3; g++) {
            Item gem = item.getGem(g);
            if (gem != null && gem.isJewelersGem()) {
                return false;
            }
            if (gem != null && gem.getSlot() != ItemSlot.Meta && !gem.isJewelersGem()) {
                ok = true;
            }
        }
        return ok;
    }

    private static boolean isEquipped(Character character, ItemInstance itemToBeChecked) {
        CharacterSlot slot = Character.getCharacterSlotByItemSlot(itemToBeChecked.getSlot());
        if (slot == CharacterSlot.Finger1) {
            return isEquipped(character, itemToBeChecked, CharacterSlot.Finger1) || isEquipped(character, itemToBeChecked, CharacterSlot.Finger2);
        } else if (itemToBeChecked.getSlot() == ItemSlot.OneHand) {
            return isEquipped(character, itemToBeChecked, CharacterSlot.MainHand) || isEquipped(character, itemToBeChecked, CharacterSlot.OffHand);
        } else if (itemToBeChecked.getSlot() == ItemSlot.Trinket) {
            return isEquipped(character, itemToBeChecked, CharacterSlot.Trinket1) || isEquipped(character, itemToBeChecked, CharacterSlot.Trinket2);
        } else {
            return isEquipped(character, itemToBeChecked, slot);
        }
    }

    private static boolean isEquipped(Character character, ItemInstance itemToBeChecked, CharacterSlot slot) {
        return itemToBeChecked == character.get(slot);
    }

    @Override
    protected BatchIndividual buildChildIndividual(BatchIndividual father, BatchIndividual mother, BatchIndividual recycledIndividual) {
        return generatorBuildIndividual(
                (slot, items) -> {
                    if (slot <= itemList.size() && (father.usedCount[slot] > 0 || mother.usedCount[slot] > 0)) {
                        double chance = father.usedCount[slot] / (father.usedCount[slot] + mother.usedCount[slot]);
                        return getRnd().nextDouble() < chance ? getItem(father, slot) : getItem(mother, slot);
                    } else {
                        return getRnd().nextDouble() < 0.5d ? getItem(father, slot) : getItem(mother, slot);
                    }
                },
                recycledIndividual);
    }
}
