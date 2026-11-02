// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchTools.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# StatusUpdated/OperationCompleted/UpgradeListCompleted/PropertyChanged events are ported as
// small listener interfaces fired on the calling thread (AGENTS §8). The
// ItemInstanceOptimizer/BatchOptimizer event subscriptions and all *_ProgressChanged/*_Completed
// callback handlers are still omitted: their state machines are inlined into the synchronous methods
// here (Parallel/ThreadPool scheduling becomes straight sequential loops over characters/items). The
// progress-driven status text updates the original built from those callback args are therefore not
// reproduced; onStatusUpdated fires from updateStatusLabel() (the C# UpdateStatusLabel body) and the
// completion events fire where the C# completion handlers raise them. C# `_item` access is done
// through the Character raw item accessors (getRawItems/getItem/setItem); Character.SetEnchantBySlot is
// inlined because the Java Character slice does not expose it yet.
package rawr.base.batch;

import rawr.base.util.KeyValuePair;

import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.optimizer.AvailableItemGenerator;
import rawr.base.optimizer.ItemInstanceOptimizer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Head-less orchestrator for batch (roster) runs: optimizes every character in a
 * {@link BatchCharacterList} or builds the cross-character upgrade lists the batch UI displays.
 *
 * <p>The batch workflows all work on the roster held by {@link #getBatchCharacterList()} and are
 * entered through this class:</p>
 * <ul>
 *   <li><b>Per-character optimize</b> ({@link #optimize()}) runs the single-character
 *       {@link ItemInstanceOptimizer} on each entry in turn, repeating the search while it keeps
 *       improving the character and moving on after at most {@link #getMaxRounds()} consecutive
 *       non-improving rounds. {@link #progressiveOptimize()} processes the roster in order of
 *       priority, reserving the exact gem/enchant combination each finished character committed so
 *       later characters cannot regem the same item, and skipping entries marked
 *       {@link BatchCharacter#isLocked() locked} (their equipped items are reserved up front).</li>
 *   <li><b>Per-character upgrade list</b> ({@link #buildUpgradeList()}) gathers every candidate
 *       item that helped at least one character, evaluates each candidate on every character and
 *       ranks the items per gear slot by the weight-adjusted average gain, which is the "what
 *       should each character chase next" view. {@link #buildProgressiveUpgradeList()} walks the
 *       candidates one item at a time, committing items as earlier characters adopt them.</li>
 *   <li><b>Joint batch optimize</b> ({@link #batchOptimize()}) lets one {@link BatchOptimizer}
 *       search a single shared item/gem assignment for all characters at once, then copies the
 *       resulting items back into the roster characters. {@link #buildBatchUpgradeList()} uses the
 *       same joint optimizer to produce the upgrade lists.</li>
 * </ul>
 *
 * <h2>Synchronous execution and events</h2>
 *
 * <p>The C# class drove the same computations with a {@code BackgroundWorker}/thread-pool state
 * machine and exposed {@code StatusUpdated}, {@code OperationCompleted},
 * {@code UpgradeListCompleted} and {@code PropertyChanged} events. The port runs the loops
 * synchronously on the calling thread, so an entry point returns only when its run finished (or
 * aborted early), and the events survive as listener interfaces, all fired on the calling thread:
 * {@link StatusUpdatedListener} whenever {@link #updateStatusLabel()} refreshes the status text;
 * {@link OperationCompletedListener} when a run ends, successfully or not;
 * {@link UpgradeListCompletedListener} once {@link #getUpgrades()} and
 * {@link #getCustomSubpoints()} hold the finished list; and {@link PropertyChangedListener} for
 * roster changes. The original's per-item status text updates driven by the callback payloads are
 * not reproduced (see the port note at the top of this file); status is refreshed by
 * {@code updateStatusLabel} at the completion points instead.</p>
 *
 * <p>The per-character entry points refuse to start while another operation is active
 * ({@link #optimize()}, {@link #buildUpgradeList()}, {@link #progressiveOptimize()},
 * {@link #buildProgressiveUpgradeList()}); the joint-batch entry points do not consult that guard,
 * matching the original, and simply replace the operation state. {@link #cancel()} asks the
 * optimizer of the active operation to stop; the run then follows its abort path and fires
 * {@code OperationCompleted}.</p>
 *
 * <h2>Configuration</h2>
 *
 * <p>Options forwarded to the optimizers: {@link #getOptimizationMethod()} /
 * {@link #getGreedyOptimizationMethod()}, {@link #getThoroughness()}, {@link #isOverrideRegem()},
 * {@link #isOverrideReenchant()} and {@link #isTemplateGemsEnabled()}; {@link #getMaxRounds()}
 * (default 3) bounds the per-character retries; {@link #getSingleItemUpgrade()} restricts the
 * upgrade lists to one item; {@link #setMaxThreads(int)} optionally enables deterministic
 * parallel candidate evaluation. {@link #setAvailableGear(List)} assigns a shared availability
 * list to every loaded character and marks them dirty, {@link #replaceUnavailableGear()} swaps
 * equipped items that are no longer available for the closest available variant, and
 * {@link #saveCharacters()}/{@link #saveCharactersAsCopy()} persist dirty characters.</p>
 */
public class BatchTools {
    /** Replaces the C# `StatusUpdated` event. */
    @FunctionalInterface
    public interface StatusUpdatedListener {
        void statusUpdated(BatchTools sender);
    }

    /** Replaces the C# `OperationCompleted` event. */
    @FunctionalInterface
    public interface OperationCompletedListener {
        void operationCompleted(BatchTools sender);
    }

    /** Replaces the C# `UpgradeListCompleted` event. */
    @FunctionalInterface
    public interface UpgradeListCompletedListener {
        void upgradeListCompleted(BatchTools sender);
    }

    /** Replaces the C# `PropertyChanged` event (INotifyPropertyChanged). */
    @FunctionalInterface
    public interface PropertyChangedListener {
        void propertyChanged(BatchTools sender, String propertyName);
    }

    private final List<StatusUpdatedListener> statusUpdatedListeners = new CopyOnWriteArrayList<StatusUpdatedListener>();
    private final List<OperationCompletedListener> operationCompletedListeners = new CopyOnWriteArrayList<OperationCompletedListener>();
    private final List<UpgradeListCompletedListener> upgradeListCompletedListeners = new CopyOnWriteArrayList<UpgradeListCompletedListener>();
    private final List<PropertyChangedListener> propertyChangedListeners = new CopyOnWriteArrayList<PropertyChangedListener>();

    public void addStatusUpdatedListener(StatusUpdatedListener listener) {
        statusUpdatedListeners.add(listener);
    }

    public void removeStatusUpdatedListener(StatusUpdatedListener listener) {
        statusUpdatedListeners.remove(listener);
    }

    public void addOperationCompletedListener(OperationCompletedListener listener) {
        operationCompletedListeners.add(listener);
    }

    public void removeOperationCompletedListener(OperationCompletedListener listener) {
        operationCompletedListeners.remove(listener);
    }

    public void addUpgradeListCompletedListener(UpgradeListCompletedListener listener) {
        upgradeListCompletedListeners.add(listener);
    }

    public void removeUpgradeListCompletedListener(UpgradeListCompletedListener listener) {
        upgradeListCompletedListeners.remove(listener);
    }

    public void addPropertyChangedListener(PropertyChangedListener listener) {
        propertyChangedListeners.add(listener);
    }

    public void removePropertyChangedListener(PropertyChangedListener listener) {
        propertyChangedListeners.remove(listener);
    }

    /** Raises StatusUpdated on the calling thread (guarded by the listener list being non-empty). */
    protected void onStatusUpdated() {
        for (StatusUpdatedListener listener : statusUpdatedListeners) {
            listener.statusUpdated(this);
        }
    }

    /** Raises OperationCompleted on the calling thread. */
    protected void onOperationCompleted() {
        for (OperationCompletedListener listener : operationCompletedListeners) {
            listener.operationCompleted(this);
        }
    }

    /** Raises UpgradeListCompleted on the calling thread. */
    protected void onUpgradeListCompleted() {
        for (UpgradeListCompletedListener listener : upgradeListCompletedListeners) {
            listener.upgradeListCompleted(this);
        }
    }

    /** Raises PropertyChanged on the calling thread. */
    protected void onPropertyChanged(String propertyName) {
        for (PropertyChangedListener listener : propertyChangedListeners) {
            listener.propertyChanged(this, propertyName);
        }
    }

    private ItemInstanceOptimizer optimizer;
    private BatchOptimizer batchOptimizer;

    /**
     * True while the optimizer driving the current run is still executing; false when the tool is
     * idle.
     */
    public boolean isBusy() {
        return optimizer.isBusy() || (batchOptimizer != null && batchOptimizer.isBusy());
    }

    private enum AsyncOperation {
        None,
        Optimize,
        BuildUpgradeList,
        BatchOptimize,
        BuildBatchUpgradeList,
        ProgressiveOptimize,
        BuildProgressiveUpgradeList
    }

    private AsyncOperation currentOperation;
    private int batchIndex;
    private AvailableItemGenerator itemGenerator;
    private Item[] itemList;
    private int itemIndex;
    private ItemInstance optimizedItemInstance;
    private Character workingCharacter;

    // optimize state
    private int optimizerRound;
    private Character bestRoundCharacter;
    private float bestRoundValue;

    // build upgrade list state
    private int upgradeListPhase;

    private static class UpgradeEntry {
        public ItemInstance item;
        public float value;
        private final List<Float> valueList = new ArrayList<Float>();

        public List<Float> getValueList() {
            return valueList;
        }
    }

    private Map<CharacterSlot, LinkedHashMap<String, UpgradeEntry>> upgradeList;

    private BatchCharacterList batchCharacterList;

    public BatchCharacterList getBatchCharacterList() {
        return batchCharacterList;
    }

    /**
     * Replaces the roster and fires the {@code "BatchCharacterList"} property notification.
     */
    public void setBatchCharacterList(BatchCharacterList batchCharacterList) {
        this.batchCharacterList = batchCharacterList;
        onPropertyChanged("BatchCharacterList");
    }

    private BatchCharacter currentBatchCharacter() {
        if (batchIndex < 0 || batchIndex >= batchCharacterList.size()) return null;
        return batchCharacterList.get(batchIndex);
    }

    private boolean batchListReady = false;

    /**
     * Creates an idle tool: empty roster, {@code maxRounds = 3}, sequential evaluation and its
     * own {@link ItemInstanceOptimizer}. No computation starts until an entry point is called.
     */
    public BatchTools() {
        maxRounds = 3;
        batchCharacterList = new BatchCharacterList();
        batchListReady = true;

        optimizer = new ItemInstanceOptimizer();
    }

    private int maxThreads = 1;

    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Opt-in deterministic parallel candidate evaluation (DECISIONS #58); 1 keeps the sequential
     * behavior. Forwarded to the {@link ItemInstanceOptimizer} and {@link BatchOptimizer} this tool
     * creates.
     */
    public void setMaxThreads(int maxThreads) {
        if (maxThreads < 1) throw new IllegalArgumentException("maxThreads must be at least 1.");
        this.maxThreads = maxThreads;
        optimizer.setMaxThreads(maxThreads);
        if (batchOptimizer != null) {
            batchOptimizer.setMaxThreads(maxThreads);
        }
    }

    private Item singleItemUpgrade;

    public Item getSingleItemUpgrade() {
        return singleItemUpgrade;
    }

    public void setSingleItemUpgrade(Item singleItemUpgrade) {
        this.singleItemUpgrade = singleItemUpgrade;
    }

    /**
     * Resolves the "only this item" selection used by the upgrade-list runs: a numeric string is
     * looked up as an item id, any other string is matched exactly against item names. Returns
     * {@code null} for a null/empty argument or when nothing matches.
     */
    public Item getSingleItemUpgrade(String itemIdOrName) {
        if (itemIdOrName == null || itemIdOrName.isEmpty()) {
            return null;
        } else {
            int id;
            try {
                id = Integer.parseInt(itemIdOrName);
            } catch (NumberFormatException e) {
                // try to match by name
                for (Item item : ItemCache.allItems()) {
                    if (item.getName().equals(itemIdOrName)) {
                        return item;
                    }
                }
                return null;
            }
            return ItemCache.findItemById(id);
        }
    }

    private String status;
    private int progress;
    private String[] customSubpoints;
    private Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades;

    public String getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public String[] getCustomSubpoints() {
        return customSubpoints;
    }

    public Map<CharacterSlot, List<ComparisonCalculationUpgrades>> getUpgrades() {
        return upgrades;
    }

    /**
     * Refreshes {@link #getStatus()} to the roster's saved score and new score separated by a
     * slash, resets {@link #getProgress()} to 0 and fires {@link StatusUpdatedListener}. Called
     * from the completion paths of every operation; the guard exists because there is nothing to
     * show before the constructor finished.
     */
    public void updateStatusLabel() {
        if (batchListReady) {
            status = String.format(Locale.ROOT, "%s / %s", batchCharacterList.getScore(), batchCharacterList.getNewScore());
            progress = 0;
            onStatusUpdated();
        }
    }

    private int maxRounds;

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    private int thoroughness;

    public int getThoroughness() {
        return thoroughness;
    }

    public void setThoroughness(int thoroughness) {
        this.thoroughness = thoroughness;
    }

    private boolean overrideRegem;

    public boolean isOverrideRegem() {
        return overrideRegem;
    }

    public void setOverrideRegem(boolean overrideRegem) {
        this.overrideRegem = overrideRegem;
    }

    private boolean overrideReenchant;

    public boolean isOverrideReenchant() {
        return overrideReenchant;
    }

    public void setOverrideReenchant(boolean overrideReenchant) {
        this.overrideReenchant = overrideReenchant;
    }

    private boolean templateGemsEnabled;

    public boolean isTemplateGemsEnabled() {
        return templateGemsEnabled;
    }

    public void setTemplateGemsEnabled(boolean templateGemsEnabled) {
        this.templateGemsEnabled = templateGemsEnabled;
    }

    private OptimizationMethod optimizationMethod;

    public OptimizationMethod getOptimizationMethod() {
        return optimizationMethod;
    }

    public void setOptimizationMethod(OptimizationMethod optimizationMethod) {
        this.optimizationMethod = optimizationMethod;
    }

    private GreedyOptimizationMethod greedyOptimizationMethod;

    public GreedyOptimizationMethod getGreedyOptimizationMethod() {
        return greedyOptimizationMethod;
    }

    public void setGreedyOptimizationMethod(GreedyOptimizationMethod greedyOptimizationMethod) {
        this.greedyOptimizationMethod = greedyOptimizationMethod;
    }

    /**
     * Sets available gear to all characters in BatchCharacterList.
     */
    public void setAvailableGear(List<String> availableGear) {
        for (BatchCharacter character : batchCharacterList) {
            if (character.getCharacter() != null) {
                character.getCharacter().setAvailableItems(new ArrayList<String>(availableGear));
                character.setUnsavedChanges(true);
            }
        }
    }

    /**
     * For each character in BatchCharacterList replaces equipped unavailable gear with the closest available one.
     */
    public void replaceUnavailableGear() {
        for (BatchCharacter batchCharacter : batchCharacterList) {
            if (batchCharacter.getCharacter() != null) {
                for (int slot = 0; slot < 19; slot++) {
                    ItemInstance item = batchCharacter.getCharacter().get(CharacterSlot.fromValue(slot));
                    if (item != null) {
                        String id = Integer.toString(item.getId());
                        String anyGem = id + ".*.*.*";
                        String onlyGemmedId = item.getId() + "." + item.getGem1Id() + "." + item.getGem2Id() + "." + item.getGem3Id();
                        String anyEnchant = item.getId() + "." + item.getGem1Id() + "." + item.getGem2Id() + "." + item.getGem3Id() + ".*";
                        List<String> list = findAllStartingWith(batchCharacter.getCharacter().getAvailableItems(), id);
                        List<String> sublist;
                        if (list.contains(onlyGemmedId + ".*")) {
                            // available
                        } else if ((sublist = findAllStartingWith(list, onlyGemmedId)).size() > 0) {
                            if (sublist.contains(item.getGemmedId())) {
                                // available
                            } else {
                                // have to replace enchant
                                String s = sublist.get(0);
                                setEnchantBySlot(batchCharacter.getCharacter(), CharacterSlot.fromValue(slot), Enchant.findEnchant(Integer.parseInt(s.substring(s.lastIndexOf('.') + 1)), item.getSlot(), batchCharacter.getCharacter()));
                                batchCharacter.setUnsavedChanges(true);
                            }
                        } else if (list.contains(id)) {
                            // available
                        } else if ((sublist = findAllStartingWith(list, anyGem)).size() > 0) {
                            if (sublist.contains(anyGem + "." + item.getEnchantId())) {
                                // available
                            } else {
                                // have to replace enchant
                                String s = sublist.get(0);
                                setEnchantBySlot(batchCharacter.getCharacter(), CharacterSlot.fromValue(slot), Enchant.findEnchant(Integer.parseInt(s.substring(s.lastIndexOf('.') + 1)), item.getSlot(), batchCharacter.getCharacter()));
                                batchCharacter.setUnsavedChanges(true);
                            }
                        } else if (list.size() > 0) {
                            String s = list.get(0);
                            if (s.endsWith("*")) {
                                s = trimEndStars(s) + item.getEnchantId();
                            }
                            item = new ItemInstance(s);
                            batchCharacter.getCharacter().set(CharacterSlot.fromValue(slot), item);
                            batchCharacter.setUnsavedChanges(true);
                        } else {
                            for (String s : batchCharacter.getCharacter().getAvailableItems()) {
                                ItemInstance newItem = null;
                                if (s.indexOf('.') < 0) {
                                    if (!s.startsWith("-")) {
                                        newItem = item.clone();
                                        newItem.setId(Integer.parseInt(s));
                                        if (newItem.getItem().getSocketColor1() == ItemSlot.None) newItem.setGem1(null);
                                        if (newItem.getItem().getSocketColor2() == ItemSlot.None) newItem.setGem2(null);
                                        if (newItem.getItem().getSocketColor3() == ItemSlot.None) newItem.setGem3(null);
                                    }
                                } else {
                                    String[] slist = s.split("\\.");
                                    if (slist[1].equals("*")) {
                                        newItem = item.clone();
                                        newItem.setId(Integer.parseInt(slist[0]));
                                        if (newItem.getItem().getSocketColor1() == ItemSlot.None) newItem.setGem1(null);
                                        if (newItem.getItem().getSocketColor2() == ItemSlot.None) newItem.setGem2(null);
                                        if (newItem.getItem().getSocketColor3() == ItemSlot.None) newItem.setGem3(null);
                                    } else {
                                        newItem = item.clone();
                                        newItem.setId(Integer.parseInt(slist[0]));
                                        newItem.setGem1Id(Integer.parseInt(slist[1]));
                                        newItem.setGem2Id(Integer.parseInt(slist[2]));
                                        newItem.setGem3Id(Integer.parseInt(slist[3]));
                                    }
                                }
                                if (newItem != null && newItem.getItem().fitsInSlot(CharacterSlot.fromValue(slot), batchCharacter.getCharacter())) {
                                    batchCharacter.getCharacter().set(CharacterSlot.fromValue(slot), newItem);
                                    String se = s.substring(s.lastIndexOf('.') + 1);
                                    if (!se.equals("*")) {
                                        setEnchantBySlot(batchCharacter.getCharacter(), CharacterSlot.fromValue(slot), Enchant.findEnchant(Integer.parseInt(se), item.getSlot(), batchCharacter.getCharacter()));
                                    }
                                    batchCharacter.setUnsavedChanges(true);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void setEnchantBySlot(Character character, CharacterSlot slot, Enchant enchant) {
        ItemInstance item = character.get(slot);
        if (item != null) item.setEnchant(enchant);
        character.onCalculationsInvalidated();
    }

    private static List<String> findAllStartingWith(List<String> list, String prefix) {
        List<String> result = new ArrayList<String>();
        for (String x : list) {
            if (x.startsWith(prefix)) result.add(x);
        }
        return result;
    }

    private static String trimEndStars(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '*') {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * Saves all characters in BatchCharacterList.
     */
    public void saveCharacters() throws IOException {
        for (BatchCharacter character : batchCharacterList) {
            if (character.getCharacter() != null && character.isUnsavedChanges()) {
                character.getCharacter().save(Path.of(character.getAbsolutePath()));
                character.setUnsavedChanges(false);
            }
        }
    }

    /**
     * Saves a copy of all characters in BatchCharacterList.
     */
    public void saveCharactersAsCopy() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-M-d H-m"));

        for (BatchCharacter character : batchCharacterList) {
            if (character.getCharacter() != null && character.isUnsavedChanges()) {
                String absolutePath = character.getAbsolutePath();
                int dot = absolutePath.lastIndexOf('.');
                String copyPath = (dot >= 0 ? absolutePath.substring(0, dot) : absolutePath) + " " + timestamp + ".xml";
                try {
                    character.getCharacter().save(Path.of(copyPath));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save character copy " + copyPath, e);
                }
            }
        }
    }

    /**
     * Optimizes each character.
     */
    public void optimize() {
        if (currentOperation != AsyncOperation.None) return;

        currentOperation = AsyncOperation.Optimize;
        optimizer.optimizationMethod = optimizationMethod;
        optimizer.greedyOptimizationMethod = greedyOptimizationMethod;

        batchIndex = 0;
        optimizerRound = 0;
        optimizeCurrentBatchCharacterLoop();
    }

    private void optimizeCurrentBatchCharacterLoop() {
        int maxRounds = this.maxRounds;
        while (batchIndex < batchCharacterList.size()) {
            BatchCharacter batchCharacter = currentBatchCharacter();
            Character character = batchCharacter.getCharacter();
            if (character == null) {
                batchIndex++;
                optimizerRound = 0;
                continue;
            }
            if (optimizerRound == 0) {
                optimizer.initializeItemCache(character, character.getAvailableItems(), overrideRegem, overrideReenchant, templateGemsEnabled, batchCharacter.getModel(), false, false, false, null, false);
            }
            String calculationToOptimize = character.getCalculationToOptimize();
            List<OptimizationRequirement> requirements = character.getOptimizationRequirements();
            Character optimizedCharacter;
            try {
                optimizedCharacter = optimizer.optimizeCharacter(character, calculationToOptimize, requirements, thoroughness, true);
            } catch (RuntimeException ex) {
                // original OptimizeCharacterCompleted handler ends the operation on e.Error
                finishOperation();
                return;
            }
            if (optimizedCharacter == null) {
                finishOperation();
                return;
            }
            float optimizedCharacterValue = ItemInstanceOptimizer.getOptimizationValue(optimizedCharacter, optimizer.getModel().getCharacterCalculations(optimizedCharacter, null, false, false, false), calculationToOptimize, requirements);
            float currentCharacterValue = ItemInstanceOptimizer.getOptimizationValue(character, optimizer.getModel().getCharacterCalculations(character, null, false, false, false), calculationToOptimize, requirements);
            boolean injected = optimizer.getLastInjected();

            if (optimizedCharacterValue > currentCharacterValue + 0.00001f) {
                character.setItems(optimizedCharacter.getItems());
                optimizerRound = 0;
            } else if (Math.abs(optimizedCharacterValue - currentCharacterValue) < 0.00001f && !injected) {
                optimizerRound = maxRounds;
            } else {
                optimizerRound++;
            }
            if (optimizerRound >= maxRounds) {
                do {
                    batchIndex++;
                } while (batchIndex < batchCharacterList.size() && currentBatchCharacter().getCharacter() == null);
                optimizerRound = 0;
            }
        }
        finishOperation();
    }

    /**
     * Cancels current operation.
     */
    public void cancel() {
        if ((currentOperation == AsyncOperation.Optimize || currentOperation == AsyncOperation.BuildUpgradeList || currentOperation == AsyncOperation.ProgressiveOptimize || currentOperation == AsyncOperation.BuildProgressiveUpgradeList) && optimizer.isBusy()) {
            optimizer.cancelAsync();
        }
        if ((currentOperation == AsyncOperation.BatchOptimize || currentOperation == AsyncOperation.BuildBatchUpgradeList) && batchOptimizer != null && batchOptimizer.isBusy()) {
            batchOptimizer.cancelAsync();
        }
    }

    /**
     * Computes upgrade list.
     */
    public void buildUpgradeList() {
        if (currentOperation != AsyncOperation.None) return;

        currentOperation = AsyncOperation.BuildUpgradeList;
        optimizer.optimizationMethod = optimizationMethod;
        optimizer.greedyOptimizationMethod = greedyOptimizationMethod;

        batchIndex = 0;
        upgradeListPhase = 0;
        upgradeList = new LinkedHashMap<CharacterSlot, LinkedHashMap<String, UpgradeEntry>>();

        // compute upgrades for each character
        while (batchIndex < batchCharacterList.size()) {
            BatchCharacter batchCharacter = currentBatchCharacter();
            Character character = batchCharacter.getCharacter();
            if (character != null) {
                optimizer.initializeItemCache(character, character.getAvailableItems(), overrideRegem, overrideReenchant, templateGemsEnabled, batchCharacter.getModel(), false, false, false, null, false);
                Map<CharacterSlot, List<ComparisonCalculationUpgrades>> result;
                try {
                    result = optimizer.computeUpgrades(character, character.getCalculationToOptimize(), character.getOptimizationRequirements(), thoroughness, singleItemUpgrade);
                } catch (RuntimeException ex) {
                    finishOperation();
                    return;
                }
                if (result == null) {
                    finishOperation();
                    return;
                }
                for (Map.Entry<CharacterSlot, List<ComparisonCalculationUpgrades>> kvp : result.entrySet()) {
                    LinkedHashMap<String, UpgradeEntry> map = upgradeList.get(kvp.getKey());
                    if (map == null) {
                        map = new LinkedHashMap<String, UpgradeEntry>();
                        upgradeList.put(kvp.getKey(), map);
                    }
                    for (ComparisonCalculationBase comp : kvp.getValue()) {
                        String key = comp.getItemInstance().getGemmedId();
                        if (!map.containsKey(key)) {
                            UpgradeEntry upgradeEntry = new UpgradeEntry();
                            upgradeEntry.item = comp.getItemInstance();
                            map.put(key, upgradeEntry);
                        }
                    }
                }
            }
            batchIndex++;
        }

        upgradeListPhase = 1;
        batchIndex = 0;
        List<UpgradeEntry> entries = new ArrayList<UpgradeEntry>();
        for (LinkedHashMap<String, UpgradeEntry> map : upgradeList.values()) {
            entries.addAll(map.values());
        }
        if (entries.isEmpty()) {
            // upgrade list is empty, abort
            finishOperation();
            return;
        }

        // evaluate each upgrade on each character
        while (batchIndex < batchCharacterList.size()) {
            BatchCharacter batchCharacter = currentBatchCharacter();
            Character character = batchCharacter.getCharacter();
            if (character == null) {
                batchIndex++;
                continue;
            }
            boolean initializeCache = true;
            for (UpgradeEntry upgradeEntry : entries) {
                if (initializeCache) {
                    optimizer.initializeItemCache(character, character.getAvailableItems(), overrideRegem, overrideReenchant, templateGemsEnabled, batchCharacter.getModel(), false, false, false, null, false);
                    initializeCache = false;
                }
                Character current = batchCharacter.getCharacter();
                ComparisonCalculationUpgrades[] comparisonUpgrade = new ComparisonCalculationUpgrades[1];
                float upgradeValue;
                try {
                    upgradeValue = optimizer.evaluateUpgrade(current, current.getCalculationToOptimize(), current.getOptimizationRequirements(), thoroughness, upgradeEntry.item, comparisonUpgrade);
                } catch (RuntimeException ex) {
                    // the original completion handler for BuildUpgradeList ignores the error and keeps 0
                    upgradeValue = 0f;
                }
                upgradeEntry.value += upgradeValue * batchCharacter.getWeight();
                upgradeEntry.getValueList().add(upgradeValue);
            }
            batchIndex++;
        }

        finishBuildUpgradeList();
    }

    private void finishOperation() {
        currentOperation = AsyncOperation.None;
        updateStatusLabel();
        onOperationCompleted();
    }

    private void finishBuildUpgradeList() {
        finishOperation();

        float totalValue = 0f;
        for (BatchCharacter batchCharacter : batchCharacterList) {
            if (batchCharacter.getCharacter() != null) {
                totalValue += batchCharacter.getWeight();
            }
        }

        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = new LinkedHashMap<CharacterSlot, List<ComparisonCalculationUpgrades>>();

        for (Map.Entry<CharacterSlot, LinkedHashMap<String, UpgradeEntry>> kvp : upgradeList.entrySet()) {
            LinkedHashMap<Integer, UpgradeEntry> filtered = new LinkedHashMap<Integer, UpgradeEntry>();
            for (UpgradeEntry entry : kvp.getValue().values()) {
                UpgradeEntry existingEntry = filtered.get(entry.item.getId());
                if (entry.value > 0 && (existingEntry == null || entry.value > existingEntry.value)) {
                    filtered.put(entry.item.getId(), entry);
                }
            }

            List<ComparisonCalculationUpgrades> list = new ArrayList<ComparisonCalculationUpgrades>();
            upgrades.put(kvp.getKey(), list);
            for (UpgradeEntry entry : filtered.values()) {
                ComparisonCalculationUpgrades itemCalc = new ComparisonCalculationUpgrades();
                itemCalc.setItemInstance(entry.item);
                itemCalc.setCharacterItems(null);
                itemCalc.setName(entry.item.getItem().getName());
                itemCalc.setEquipped(false);
                itemCalc.setOverallPoints(entry.value / totalValue);
                itemCalc.setSubPoints(toFloatArray(entry.getValueList()));

                list.add(itemCalc);
            }
        }
        List<String> customSubpoints = new ArrayList<String>();
        for (BatchCharacter batchCharacter : batchCharacterList) {
            customSubpoints.add(batchCharacter.getName());
        }
        this.customSubpoints = customSubpoints.toArray(new String[0]);
        this.upgrades = upgrades;
        onUpgradeListCompleted();
    }

    /**
     * Optimizes all characters at the same time based on weights.
     */
    public void batchOptimize() {
        int thoroughness = this.thoroughness;
        boolean overrideRegem = this.overrideRegem;
        boolean overrideReenchant = this.overrideReenchant;

        List<KeyValuePair<Character, Float>> batchList = batchCharacterList.getBatchOptimizerList();
        if (!batchList.isEmpty() && batchList.get(0).key() != null) {
            batchOptimizer = new BatchOptimizer(batchList, overrideRegem, overrideReenchant, templateGemsEnabled);
            batchOptimizer.setMaxThreads(maxThreads);

            currentOperation = AsyncOperation.BatchOptimize;

            BatchValuation[] bestValuation = new BatchValuation[1];
            BatchIndividual optimizedBatch = null;
            try {
                optimizedBatch = batchOptimizer.optimizeCharacter(thoroughness, bestValuation);
            } catch (RuntimeException ex) {
                optimizedBatch = null;
            }
            if (optimizedBatch != null) {
                for (int i = 0; i < batchCharacterList.size(); i++) {
                    Character batchCharacter = batchCharacterList.get(i).getCharacter();
                    Character bestCharacter = optimizedBatch.character.get(i);

                    batchCharacter.setItems(bestCharacter.getItems());
                }
            }
            finishOperation();
        }
    }

    /**
     * Computes upgrade list based on batch optimizer.
     */
    public void buildBatchUpgradeList() {
        int thoroughness = this.thoroughness;
        boolean overrideRegem = this.overrideRegem;
        boolean overrideReenchant = this.overrideReenchant;

        List<KeyValuePair<Character, Float>> batchList = batchCharacterList.getBatchOptimizerList();
        if (!batchList.isEmpty() && batchList.get(0).key() != null) {
            batchOptimizer = new BatchOptimizer(batchList, overrideRegem, overrideReenchant, templateGemsEnabled);
            batchOptimizer.setMaxThreads(maxThreads);

            currentOperation = AsyncOperation.BuildBatchUpgradeList;

            Map<CharacterSlot, List<ComparisonCalculationUpgrades>> result = null;
            boolean cancelled = false;
            try {
                result = batchOptimizer.computeUpgrades(thoroughness, singleItemUpgrade);
                cancelled = result == null;
            } catch (RuntimeException ex) {
                result = null;
            }
            finishOperation();
            if (!cancelled) {
                customSubpoints = null;
                upgrades = result;
                onUpgradeListCompleted();
            }
        }
    }

    /**
     * Optimizes all characters, each subsequent character having lower priority.
     */
    public void progressiveOptimize() {
        if (currentOperation != AsyncOperation.None || batchCharacterList.isEmpty() || batchCharacterList.get(0).getCharacter() == null) return;

        currentOperation = AsyncOperation.ProgressiveOptimize;
        optimizer.optimizationMethod = optimizationMethod;
        optimizer.greedyOptimizationMethod = greedyOptimizationMethod;

        batchIndex = 0;
        optimizerRound = 0;
        bestRoundCharacter = null;
        bestRoundValue = Float.NEGATIVE_INFINITY;

        int thoroughness = this.thoroughness;
        if (currentBatchCharacter() != null) {
            createBatchItemGenerator();
            optimizer.initializeItemCache(currentBatchCharacter().getCharacter(), currentBatchCharacter().getModel(), itemGenerator);
            while (currentBatchCharacter() != null && currentBatchCharacter().isLocked()) {
                batchIndex++;
            }
            Character optimized;
            try {
                optimized = optimizer.optimizeCharacter(currentBatchCharacter().getCharacter(), currentBatchCharacter().getCharacter().getCalculationToOptimize(), currentBatchCharacter().getCharacter().getOptimizationRequirements(), thoroughness, true);
            } catch (RuntimeException ex) {
                finishOperation();
                return;
            }
            progressiveOptimizeCompleted(optimized, thoroughness);
        } else {
            currentOperation = AsyncOperation.None;
        }
    }

    private void progressiveOptimizeCompleted(Character optimized, int thoroughness) {
        int maxRounds = this.maxRounds;
        while (true) {
            if (optimized == null) {
                bestRoundCharacter = null;
                break;
            }
            String calculationToOptimize = optimized.getCalculationToOptimize();
            List<OptimizationRequirement> requirements = optimized.getOptimizationRequirements();
            float optimizedCharacterValue = ItemInstanceOptimizer.getOptimizationValue(optimized, optimizer.getModel().getCharacterCalculations(optimized, null, false, false, false), calculationToOptimize, requirements);
            Character inputCharacter = currentBatchCharacter().getCharacter();
            float currentCharacterValue = ItemInstanceOptimizer.getOptimizationValue(inputCharacter, optimizer.getModel().getCharacterCalculations(inputCharacter, null, false, false, false), calculationToOptimize, requirements);
            boolean injected = optimizer.getLastInjected();

            // try to find at least same value, although sometimes it's possible it doesn't exist
            // anymore due to restrictions
            if (optimizedCharacterValue > bestRoundValue + 0.00001f) {
                bestRoundCharacter = optimized;
                bestRoundValue = optimizedCharacterValue;
            }

            if (optimizedCharacterValue > currentCharacterValue + 0.00001f) {
                Character batchCharacter = currentBatchCharacter().getCharacter();
                Character bestCharacter = optimized;
                batchCharacter.setItems(bestCharacter.getItems());

                optimizerRound = 0;
            } else if (Math.abs(optimizedCharacterValue - currentCharacterValue) < 0.00001f && !injected) {
                optimizerRound = maxRounds;
            } else {
                optimizerRound++;
            }
            if (optimizerRound >= maxRounds) {
                // we have to use the best one we get, even if it's lower value than what we had before
                Character batchCharacter = currentBatchCharacter().getCharacter();
                // verify if it's actually different so we don't make it dirty when not necessary
                boolean dirty = false;
                for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
                    if (slot != CharacterSlot.OffHand.value() || bestRoundCharacter.getCurrentCalculations().includeOffHandInCalculations(bestRoundCharacter)) {
                        if (slot == CharacterSlot.Finger1.value() || slot == CharacterSlot.Trinket1.value()) {
                            // ignore if we have 1/2 swap
                            if (!((batchCharacter.getItem(slot) == bestRoundCharacter.getItem(slot) && batchCharacter.getItem(slot + 1) == bestRoundCharacter.getItem(slot + 1)) ||
                                    (batchCharacter.getItem(slot) == bestRoundCharacter.getItem(slot + 1) && batchCharacter.getItem(slot + 1) == bestRoundCharacter.getItem(slot)))) {
                                dirty = true;
                                break;
                            }
                            slot++;
                        } else {
                            if (batchCharacter.getItem(slot) != bestRoundCharacter.getItem(slot)) {
                                dirty = true;
                                break;
                            }
                        }
                    }
                }
                if (dirty) {
                    batchCharacter.setItems(bestRoundCharacter.getItems());
                }
                // we have to perform item restrictions on the active item generator
                // so that we don't regem/reenchant already used items
                itemGenerator.addItemRestrictions(bestRoundCharacter);
                // move to next batch character
                do {
                    batchIndex++;
                } while (batchIndex < batchCharacterList.size() && (currentBatchCharacter().getCharacter() == null || currentBatchCharacter().isLocked()));
                bestRoundCharacter = null;
                bestRoundValue = Float.NEGATIVE_INFINITY;
                optimizerRound = 0;
            }

            if (batchIndex < batchCharacterList.size()) {
                Character character = currentBatchCharacter().getCharacter().clone();
                // regularize character with current item restrictions
                itemGenerator.regularizeCharacter(character);
                optimizer.initializeItemCache(character, currentBatchCharacter().getModel(), itemGenerator);
                try {
                    optimized = optimizer.optimizeCharacter(character, character.getCalculationToOptimize(), character.getOptimizationRequirements(), thoroughness, true);
                } catch (RuntimeException ex) {
                    finishOperation();
                    return;
                }
            } else {
                bestRoundCharacter = null;
                break;
            }
        }
        finishOperation();
    }

    private void createBatchItemGenerator() {
        Character[] characterList = new Character[batchCharacterList.size()];
        CalculationsBase[] modelList = new CalculationsBase[batchCharacterList.size()];
        for (int i = 0; i < batchCharacterList.size(); i++) {
            characterList[i] = batchCharacterList.get(i).getCharacter();
            modelList[i] = batchCharacterList.get(i).getModel();
        }
        itemGenerator = new AvailableItemGenerator(batchCharacterList.get(0).getCharacter().getAvailableItems(), optimizer.greedyOptimizationMethod != GreedyOptimizationMethod.AllCombinations, templateGemsEnabled, overrideRegem, overrideReenchant, false, characterList, modelList);
        // create item restrictions for locked characters
        for (int i = 0; i < batchCharacterList.size(); i++) {
            if (batchCharacterList.get(i).isLocked()) {
                itemGenerator.addItemRestrictions(batchCharacterList.get(i).getCharacter());
            }
        }
    }

    /**
     * Computes upgrade list based on progressive optimizer.
     */
    public void buildProgressiveUpgradeList() {
        if (currentOperation != AsyncOperation.None || batchCharacterList.isEmpty() || batchCharacterList.get(0).getCharacter() == null) return;

        currentOperation = AsyncOperation.BuildProgressiveUpgradeList;
        optimizer.optimizationMethod = optimizationMethod;
        optimizer.greedyOptimizationMethod = greedyOptimizationMethod;

        batchIndex = 0;
        optimizerRound = 0;
        upgradeListPhase = 0;

        int thoroughness = this.thoroughness;
        Character character = currentBatchCharacter().getCharacter();
        LinkedHashMap<Integer, Item> itemById = new LinkedHashMap<Integer, Item>();
        Item single = singleItemUpgrade;
        if (single != null) {
            itemList = new Item[] { single };
        } else {
            for (int i = 0; i < batchCharacterList.size(); i++) {
                CalculationsBase model = batchCharacterList.get(i).getModel();
                Item[] items = ItemCache.getRelevantItems(model, batchCharacterList.get(i).getCharacter().getRace());
                for (Item item : items) {
                    if (item != null && !item.isGem()) {
                        itemById.put(item.getId(), item);
                    }
                }
            }
            itemList = itemById.values().toArray(new Item[0]);
        }
        createBatchItemGenerator();
        itemGenerator.saveAvailabilityInformation();
        optimizer.initializeItemCache(currentBatchCharacter().getCharacter(), currentBatchCharacter().getModel(), itemGenerator);
        itemIndex = 0;
        upgradeList = new LinkedHashMap<CharacterSlot, LinkedHashMap<String, UpgradeEntry>>();
        workingCharacter = character;

        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> result;
        try {
            result = optimizer.computeUpgrades(workingCharacter, workingCharacter.getCalculationToOptimize(), workingCharacter.getOptimizationRequirements(), thoroughness, itemList[itemIndex]);
        } catch (RuntimeException ex) {
            finishOperation();
            return;
        }
        if (result == null) {
            finishOperation();
            return;
        }
        processComputeUpgradesResult(result);

        while (true) {
            // move to next character
            do {
                batchIndex++;
            } while (batchIndex < batchCharacterList.size() && currentBatchCharacter().getCharacter() == null);
            if (batchIndex >= batchCharacterList.size()) {
                // we finished all characters for this item
                // move to next item
                itemIndex++;
                if (itemIndex < itemList.length) {
                    batchIndex = 0;
                    upgradeListPhase = 0;
                    // we have to reinitialize item generator because of the restrictions we made
                    itemGenerator.restoreAvailabilityInformation();
                    optimizer.initializeItemCache(currentBatchCharacter().getCharacter(), currentBatchCharacter().getModel(), itemGenerator);
                    workingCharacter = currentBatchCharacter().getCharacter();
                    try {
                        result = optimizer.computeUpgrades(workingCharacter, workingCharacter.getCalculationToOptimize(), workingCharacter.getOptimizationRequirements(), thoroughness, itemList[itemIndex]);
                    } catch (RuntimeException ex) {
                        finishOperation();
                        return;
                    }
                    if (result == null) {
                        finishOperation();
                        return;
                    }
                    processComputeUpgradesResult(result);
                    continue;
                } else {
                    // we're done
                    wrapUpProgressiveUpgradeList();
                    return;
                }
            }

            if (upgradeListPhase == 0) {
                // so far we haven't made any changes yet
                // we're working under assumption that the starting batch is valid i.e. an item will have the same gemming in all characters
                workingCharacter = currentBatchCharacter().getCharacter();
                optimizer.initializeItemCache(workingCharacter, currentBatchCharacter().getModel(), itemGenerator);
                try {
                    result = optimizer.computeUpgrades(workingCharacter, workingCharacter.getCalculationToOptimize(), workingCharacter.getOptimizationRequirements(), thoroughness, itemList[itemIndex]);
                } catch (RuntimeException ex) {
                    finishOperation();
                    return;
                }
                if (result == null) {
                    finishOperation();
                    return;
                }
                processComputeUpgradesResult(result);
            } else {
                // we made item restrictions, first we have to optimize character without the item
                Character clone = currentBatchCharacter().getCharacter().clone();
                // regularize character with current item restrictions
                itemGenerator.regularizeCharacter(clone);
                optimizer.initializeItemCache(clone, currentBatchCharacter().getModel(), itemGenerator);
                Character optimized;
                try {
                    optimized = optimizer.optimizeCharacter(clone, clone.getCalculationToOptimize(), clone.getOptimizationRequirements(), thoroughness, true);
                } catch (RuntimeException ex) {
                    finishOperation();
                    return;
                }
                if (optimized == null) {
                    finishOperation();
                    return;
                }
                workingCharacter = optimized;
                int effectiveThoroughness = thoroughness;
                // evaluate what we get by using the optimized item
                ComparisonCalculationUpgrades[] comparisonUpgrade = new ComparisonCalculationUpgrades[1];
                float upgradeValue;
                try {
                    upgradeValue = optimizer.evaluateUpgrade(workingCharacter, workingCharacter.getCalculationToOptimize(), workingCharacter.getOptimizationRequirements(), effectiveThoroughness, optimizedItemInstance, comparisonUpgrade);
                } catch (RuntimeException ex) {
                    finishOperation();
                    return;
                }
                processEvaluateUpgradeResult(upgradeValue, comparisonUpgrade[0]);
            }
        }
    }

    private void processComputeUpgradesResult(Map<CharacterSlot, List<ComparisonCalculationUpgrades>> result) {
        boolean foundUpgrade = false;
        for (Map.Entry<CharacterSlot, List<ComparisonCalculationUpgrades>> kvp : result.entrySet()) {
            LinkedHashMap<String, UpgradeEntry> map = upgradeList.get(kvp.getKey());
            if (map == null) {
                map = new LinkedHashMap<String, UpgradeEntry>();
                upgradeList.put(kvp.getKey(), map);
            }
            if (!kvp.getValue().isEmpty()) {
                ComparisonCalculationUpgrades comp = kvp.getValue().get(0);
                upgradeListPhase = 1; // item was used, from now on we do evaluate upgrade on specific item instance only
                optimizedItemInstance = comp.getItemInstance();
                // make item restrictions based on best character
                itemGenerator.addItemRestrictions(comp.getCharacterItems(), workingCharacter.getCurrentCalculations().includeOffHandInCalculations(workingCharacter));
                foundUpgrade = true;
                String key = Integer.toString(comp.getItemInstance().getId());
                UpgradeEntry upgradeEntry = map.get(key);
                if (upgradeEntry == null) {
                    upgradeEntry = new UpgradeEntry();
                    map.put(key, upgradeEntry);
                }
                upgradeEntry.item = comp.getItemInstance();
                upgradeEntry.value += comp.getOverallPoints() * currentBatchCharacter().getWeight();
                upgradeEntry.getValueList().add(comp.getOverallPoints());
                break;
            }
        }
        if (!foundUpgrade) {
            // make item restrictions based on best character without using the item
            itemGenerator.addItemRestrictions(workingCharacter);
            // if we're evaluating an item that was already marked available then we must restrict to that version
            // if it was used in this character
            Item item = itemList[itemIndex];
            CharacterSlot slot = Character.getCharacterSlotByItemSlot(item.getSlot());
            if (upgradeListPhase == 0) {
                for (CharacterSlot s : CharacterSlot.values()) {
                    ItemInstance itemInstance = workingCharacter.get(s);
                    if (itemInstance != null && itemInstance.getId() == item.getId()) {
                        upgradeListPhase = 1;
                        optimizedItemInstance = itemInstance;
                        break;
                    }
                }
            }
            LinkedHashMap<String, UpgradeEntry> map = upgradeList.get(slot);
            if (map == null) {
                map = new LinkedHashMap<String, UpgradeEntry>();
                upgradeList.put(slot, map);
            }
            String key = Integer.toString(itemList[itemIndex].getId());
            UpgradeEntry upgradeEntry = map.get(key);
            if (upgradeEntry == null) {
                upgradeEntry = new UpgradeEntry();
                map.put(key, upgradeEntry);
            }
            if (upgradeListPhase == 1) {
                upgradeEntry.item = optimizedItemInstance;
            }
            upgradeEntry.getValueList().add(0.0f);
        }
    }

    private void processEvaluateUpgradeResult(float upgradeValue, ComparisonCalculationUpgrades upgrade) {
        CharacterSlot slot = Character.getCharacterSlotByItemSlot(optimizedItemInstance.getSlot());
        LinkedHashMap<String, UpgradeEntry> map = upgradeList.get(slot);
        if (map == null) {
            map = new LinkedHashMap<String, UpgradeEntry>();
            upgradeList.put(slot, map);
        }
        String key = Integer.toString(optimizedItemInstance.getId());
        UpgradeEntry upgradeEntry = map.get(key);
        if (upgradeEntry == null) {
            upgradeEntry = new UpgradeEntry();
            upgradeEntry.item = optimizedItemInstance;
            map.put(key, upgradeEntry);
        }
        if (upgradeValue > 0) {
            // make item restrictions based on best character
            itemGenerator.addItemRestrictions(upgrade.getCharacterItems(), workingCharacter.getCurrentCalculations().includeOffHandInCalculations(workingCharacter));
            upgradeEntry.value += upgradeValue * currentBatchCharacter().getWeight();
            upgradeEntry.getValueList().add(upgradeValue);
        } else {
            // make item restrictions based on best character without using the item
            itemGenerator.addItemRestrictions(workingCharacter);
            upgradeEntry.getValueList().add(0.0f);
        }
    }

    private void wrapUpProgressiveUpgradeList() {
        finishOperation();

        float totalValue = 0f;
        for (BatchCharacter batchCharacter : batchCharacterList) {
            if (batchCharacter.getCharacter() != null) {
                totalValue += batchCharacter.getWeight();
            }
        }

        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades = new LinkedHashMap<CharacterSlot, List<ComparisonCalculationUpgrades>>();

        for (Map.Entry<CharacterSlot, LinkedHashMap<String, UpgradeEntry>> kvp : upgradeList.entrySet()) {
            List<ComparisonCalculationUpgrades> list = new ArrayList<ComparisonCalculationUpgrades>();
            upgrades.put(kvp.getKey(), list);
            // the original declares an unused filtered dictionary here and adds every positive entry
            for (UpgradeEntry entry : kvp.getValue().values()) {
                if (entry.value > 0) {
                    ComparisonCalculationUpgrades itemCalc = new ComparisonCalculationUpgrades();
                    itemCalc.setItemInstance(entry.item);
                    itemCalc.setCharacterItems(null);
                    itemCalc.setName(entry.item.getItem().getName());
                    itemCalc.setEquipped(false);
                    itemCalc.setOverallPoints(entry.value / totalValue);
                    itemCalc.setSubPoints(toFloatArray(entry.getValueList()));

                    list.add(itemCalc);
                }
            }
        }
        List<String> customSubpoints = new ArrayList<String>();
        for (BatchCharacter batchCharacter : batchCharacterList) {
            customSubpoints.add(batchCharacter.getName());
        }
        this.customSubpoints = customSubpoints.toArray(new String[0]);
        this.upgrades = upgrades;
        onUpgradeListCompleted();
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
