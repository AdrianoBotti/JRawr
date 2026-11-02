package rawr.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.optimizer.ItemInstanceOptimizer;
import rawr.models.mage.CalculationOptionsMage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Web-facing wrapper around the ported optimizer upgrade APIs. {@code computeUpgrades} performs
 * the per-item gemming/enchant search, so it is much more expensive than the chart's direct
 * upgrade ratings; this service keeps the candidate pool bounded (custom pool, or the top
 * {@code itemsPerSlot} relevant items per slot) and exposes both the full per-slot list and a
 * single-item evaluation.
 */
@Service
public class UpgradeListService {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeListService.class);

    private final CalculationService calculationService;
    private final ComparisonService comparisonService;
    private final ItemIconService icons;

    public UpgradeListService(CalculationService calculationService,
            ComparisonService comparisonService, ItemIconService icons) {
        this.calculationService = calculationService;
        this.comparisonService = comparisonService;
        this.icons = icons;
    }

    /** Progress callback used by the background upgrade job. */
    public interface Progress {
        void update(int percent, String item);

        boolean cancelled();

        Progress NONE = new Progress() {
            @Override
            public void update(int percent, String item) {
            }

            @Override
            public boolean cancelled() {
                return false;
            }
        };
    }

    private record CandidatePool(List<Item> items, List<String> pool) {
    }

    /** Thrown when the background job is cancelled; caught by the job runner. */
    public static final class CancelledException extends RuntimeException {
        public CancelledException() {
            super("Upgrade list cancelled");
        }
    }

    public record Options(
            int thoroughness,
            List<CharacterSlot> slots,
            int itemsPerSlot,
            String calculation,
            List<OptimizationRequirement> requirements,
            boolean overrideRegem,
            boolean overrideReenchant,
            boolean templateGems,
            boolean poolAllRelevant,
            OptimizationMethod method,
            GreedyOptimizationMethod greedy) {
    }

    /** Another slot that the optimizer changed to reach this row's value. */
    public record GearChangeView(String slotLabel, String beforeName, String afterName,
            boolean sameItem) {
    }

    public record UpgradeItemView(
            CharacterSlot slot,
            String slotLabel,
            int itemId,
            String gemmedId,
            String name,
            String qualityKey,
            int itemLevel,
            String iconUrl,
            String initials,
            String metaText,
            List<String> gemColors,
            boolean available,
            boolean equipped,
            float gain,
            String gainText,
            List<ComparisonService.SubPointView> subPoints,
            List<GearChangeView> alsoChanges) {
    }

    public record SlotView(CharacterSlot slot, String slotLabel, List<UpgradeItemView> rows) {
    }

    public record UpgradeResult(
            List<SlotView> slots,
            int count,
            String calculation,
            long elapsedMillis) {
    }

    public record Evaluation(
            UpgradeItemView item,
            String calculation,
            long elapsedMillis) {
    }

    public UpgradeResult compute(Character source, Options options) {
        return compute(source, options, Progress.NONE);
    }

    public UpgradeResult compute(Character source, Options options, Progress progress) {
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            if (source.getCurrentModel() == null || source.getCurrentModel().isBlank()) {
                throw new IllegalStateException("Choose a model before building an upgrade list.");
            }
            CandidatePool candidates = buildCandidatePool(source, options);
            if (candidates.items().isEmpty()) {
                return new UpgradeResult(List.of(), 0, options.calculation(), 0L);
            }
            Prepared prepared = prepare(source, options, candidates.pool(), progress,
                    candidates.items().size());
            try {
                List<String> labels = subPointLabels(prepared.model);
                Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades =
                        new LinkedHashMap<CharacterSlot, List<ComparisonCalculationUpgrades>>();
                int total = candidates.items().size();
                int index = 0;
                for (Item item : candidates.items()) {
                    if (progress.cancelled()) {
                        return null;
                    }
                    prepared.optimizer.setContext(index, total, item.getName());
                    Map<CharacterSlot, List<ComparisonCalculationUpgrades>> partial =
                            prepared.optimizer.computeUpgrades(prepared.character,
                                    options.calculation(), options.requirements(),
                                    options.thoroughness(), item);
                    if (partial != null) {
                        for (Map.Entry<CharacterSlot, List<ComparisonCalculationUpgrades>> entry
                                : partial.entrySet()) {
                            upgrades.computeIfAbsent(entry.getKey(),
                                    key -> new ArrayList<ComparisonCalculationUpgrades>())
                                    .addAll(entry.getValue());
                        }
                    }
                    index++;
                    progress.update((int) Math.rint(index * 100.0 / total), item.getName());
                }
                if (progress.cancelled()) {
                    return null;
                }
                List<SlotView> slots = new ArrayList<SlotView>();
                int count = 0;
                for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
                    if (!options.slots().contains(slot)) {
                        continue;
                    }
                    List<ComparisonCalculationUpgrades> rows = upgrades.get(slot);
                    if (rows == null || rows.isEmpty()) {
                        continue;
                    }
                    List<UpgradeItemView> views = new ArrayList<UpgradeItemView>();
                    for (ComparisonCalculationUpgrades row : rows) {
                        UpgradeItemView view = toView(source, slot, row, labels);
                        if (view != null) {
                            views.add(view);
                            count++;
                        }
                    }
                    views.sort(Comparator.comparing(UpgradeItemView::gain).reversed()
                            .thenComparing(UpgradeItemView::name, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(UpgradeItemView::gemmedId));
                    if (!views.isEmpty()) {
                        slots.add(new SlotView(slot, EditorService.slotLabel(slot), List.copyOf(views)));
                    }
                }
                long elapsed = (System.nanoTime() - start) / 1_000_000L;
                return new UpgradeResult(List.copyOf(slots), count, options.calculation(), elapsed);
            } finally {
                prepared.close();
            }
        });
    }

    public Evaluation evaluate(Character source, Options options, String upgradeText) {
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            if (source.getCurrentModel() == null || source.getCurrentModel().isBlank()) {
                throw new IllegalStateException("Choose a model before evaluating an upgrade.");
            }
            String text = upgradeText == null ? "" : upgradeText.trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Enter an item id or gemmed id to evaluate.");
            }
            ItemInstance probe = text.indexOf('.') >= 0 ? new ItemInstance(text) : null;
            Item itemProbe = probe != null ? probe.getItem() : ItemCache.findItemById(Integer.parseInt(text));
            String extraId = itemProbe == null ? null : Integer.toString(itemProbe.getId());
            List<String> pool = buildPool(source, options, extraId);
            Prepared prepared = prepare(source, options, pool, Progress.NONE, 1);
            try {
                List<String> labels = subPointLabels(prepared.model);
                ItemInstance instance;
                Item item = null;
                if (text.indexOf('.') >= 0) {
                    instance = new ItemInstance(text);
                    item = instance.getItem();
                } else {
                    int id = Integer.parseInt(text);
                    item = ItemCache.findItemById(id);
                    if (item == null) {
                        throw new IllegalArgumentException("Unknown item id " + id + ".");
                    }
                    instance = new ItemInstance(id, 0, 0, 0, 0);
                }
                if (item == null) {
                    throw new IllegalArgumentException("The item is not present in the item cache.");
                }
                ComparisonCalculationUpgrades[] out = new ComparisonCalculationUpgrades[1];
                float value;
                if (text.indexOf('.') >= 0) {
                    value = prepared.optimizer.evaluateUpgrade(prepared.character,
                            options.calculation(), options.requirements(), options.thoroughness(),
                            instance, out);
                } else {
                    // A bare item has no chosen gems/enchant yet. Let the shared single-item
                    // upgrade path pick the best gemming and return that concrete instance.
                    Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades =
                            prepared.optimizer.computeUpgrades(prepared.character,
                                    options.calculation(), options.requirements(),
                                    options.thoroughness(), item);
                    ComparisonCalculationUpgrades best = null;
                    for (List<ComparisonCalculationUpgrades> rows : upgrades.values()) {
                        for (ComparisonCalculationUpgrades row : rows) {
                            if (best == null || row.getOverallPoints() > best.getOverallPoints()) {
                                best = row;
                            }
                        }
                    }
                    if (best == null) {
                        throw new IllegalArgumentException("This item is not an upgrade for any slot.");
                    }
                    out[0] = best;
                    value = best.getOverallPoints();
                }
                ComparisonCalculationUpgrades result = out[0];
                ItemInstance resultInstance = result != null ? result.getItemInstance() : instance;
                if (resultInstance == null) {
                    resultInstance = instance;
                }
                CharacterSlot slot = resultInstance.getId() != 0 ? findSlot(source, resultInstance) : CharacterSlot.None;
                UpgradeItemView view = toView(source, slot, resultInstance,
                        result != null ? result.getOverallPoints() : value, labels, null,
                        result != null ? result.getCharacterItems() : null);
                long elapsed = (System.nanoTime() - start) / 1_000_000L;
                return new Evaluation(view, options.calculation(), elapsed);
            } finally {
                prepared.close();
            }
        });
    }


    private static CandidatePool buildCandidatePool(Character character, Options options) {
        java.util.Set<String> pool = new java.util.LinkedHashSet<String>();
        LinkedHashMap<Integer, Item> items = new LinkedHashMap<Integer, Item>();
        List<String> available = character.getAvailableItems();
        boolean custom = !options.poolAllRelevant() && available != null && !available.isEmpty();
        if (custom) {
            for (String entry : available) {
                if (entry == null || entry.isEmpty() || entry.charAt(0) == '-') {
                    continue;
                }
                pool.add(entry);
                int dot = entry.indexOf('.');
                try {
                    int id = Integer.parseInt(dot < 0 ? entry : entry.substring(0, dot));
                    Item item = ItemCache.findItemById(id);
                    if (item != null) {
                        items.putIfAbsent(id, item);
                    }
                } catch (NumberFormatException ignored) {
                    // malformed availability entries stay in the pool but have no item template
                }
            }
        } else {
            for (CharacterSlot slot : options.slots()) {
                List<ItemInstance> instances = new ArrayList<ItemInstance>(
                        character.getRelevantItemInstances(slot));
                instances.sort(Comparator
                        .comparingInt((ItemInstance instance) -> instance.getItem() != null
                                ? instance.getItem().getItemLevel() : 0).reversed()
                        .thenComparingInt(ItemInstance::getId));
                int limit = options.itemsPerSlot() > 0
                        ? options.itemsPerSlot() : instances.size();
                java.util.Set<Integer> ids = new java.util.LinkedHashSet<Integer>();
                for (ItemInstance instance : instances) {
                    if (instance.getItem() != null) {
                        ids.add(instance.getId());
                    }
                    if (ids.size() >= limit) {
                        break;
                    }
                }
                for (Integer id : ids) {
                    Item item = ItemCache.findItemById(id);
                    if (item != null) {
                        items.putIfAbsent(id, item);
                        pool.add(Integer.toString(id));
                    }
                }
            }
        }
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                pool.add(instance.getGemmedId());
                if (options.slots().contains(slot)) {
                    pool.add(Integer.toString(instance.getId()));
                }
            }
        }
        if (options.overrideReenchant()) {
            CalculationsBase model = Calculations.getModel(character.getCurrentModel());
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id != 0 && model.isEnchantRelevant(enchant, character)) {
                    pool.add(Integer.toString(-1 * (enchant.id + (10000 * enchant.slot.ordinal()))));
                }
            }
        }
        return new CandidatePool(List.copyOf(items.values()), List.copyOf(pool));
    }

    private static List<String> buildPool(Character character, Options options, String extraItemId) {
        java.util.Set<String> pool = new java.util.LinkedHashSet<String>();
        List<String> available = character.getAvailableItems();
        boolean custom = !options.poolAllRelevant() && available != null && !available.isEmpty();
        if (custom) {
            pool.addAll(available);
        } else {
            for (CharacterSlot slot : options.slots()) {
                List<ItemInstance> instances = new ArrayList<ItemInstance>(
                        character.getRelevantItemInstances(slot));
                instances.sort(Comparator
                        .comparingInt((ItemInstance instance) -> instance.getItem() != null
                                ? instance.getItem().getItemLevel() : 0).reversed()
                        .thenComparingInt(ItemInstance::getId));
                int limit = options.itemsPerSlot() > 0
                        ? options.itemsPerSlot() : instances.size();
                java.util.Set<Integer> ids = new java.util.LinkedHashSet<Integer>();
                for (ItemInstance instance : instances) {
                    if (instance.getItem() != null) {
                        ids.add(instance.getId());
                    }
                    if (ids.size() >= limit) {
                        break;
                    }
                }
                for (Integer id : ids) {
                    pool.add(Integer.toString(id));
                }
            }
        }
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                pool.add(instance.getGemmedId());
                if (options.slots().contains(slot)) {
                    pool.add(Integer.toString(instance.getId()));
                }
            }
        }
        if (extraItemId != null) {
            pool.add(extraItemId);
        }
        if (options.overrideReenchant()) {
            CalculationsBase model = Calculations.getModel(character.getCurrentModel());
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id != 0 && model.isEnchantRelevant(enchant, character)) {
                    pool.add(Integer.toString(-1 * (enchant.id + (10000 * enchant.slot.ordinal()))));
                }
            }
        }
        return new ArrayList<String>(pool);
    }

    private Prepared prepare(Character source, Options options, List<String> pool,
            Progress progress, int totalItems) {
        Character working = source.cloneForCalculation();
        OptimizerJobService.copyOptimizerState(source, working);
        String modelName = source.getCurrentModel();
        CalculationsBase model = Calculations.loadFreshModel(modelName);
        ICalculationOptionBase sourceOptions = source.getCalculationOptions();
        if (sourceOptions != null) {
            try {
                ICalculationOptionBase jobOptions = source.getCurrentCalculations()
                        .deserializeDataObject(sourceOptions.getXml());
                if (jobOptions != null) {
                    if (jobOptions instanceof ICharacterCalculationOptions characterOptions) {
                        characterOptions.setCharacter(working);
                    }
                    working.setCalculationOptions(jobOptions);
                }
            } catch (RuntimeException e) {
                LOG.warn("Could not copy upgrade-list calculation options", e);
            }
        }
        if (working.getCalculationOptions() == null) {
            working.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
        }
        if (working.getCalculationOptions() instanceof CalculationOptionsMage mageOptions) {
            mageOptions.setCharacter(working);
        }

        ProgressOptimizer optimizer = new ProgressOptimizer(progress, totalItems);
        optimizer.optimizationMethod = options.method();
        optimizer.greedyOptimizationMethod = options.greedy();
        optimizer.setRndSeed(OptimizerJobService.JAVA_SEED);
        optimizer.setMaxThreads(1);
        boolean positiveCost = "[Cost]".equals(working.getCalculationToOptimize());
        if (!positiveCost && working.getOptimizationRequirements() != null) {
            for (rawr.base.calculations.OptimizationRequirement requirement
                    : working.getOptimizationRequirements()) {
                if (requirement != null && "[Cost]".equals(requirement.calculation)) {
                    positiveCost = true;
                    break;
                }
            }
        }
        optimizer.initializeItemCache(working, pool, options.overrideRegem(),
                options.overrideReenchant(), options.templateGems(), model, false, false, false,
                null, false, positiveCost);
        return new Prepared(working, optimizer, model);
    }

    /** Optimizer that adds an outer item index to the core's per-search progress. */
    private static final class ProgressOptimizer extends ItemInstanceOptimizer {
        private final Progress progress;
        private final int totalItems;
        private int itemIndex;
        private String itemName = "";

        ProgressOptimizer(Progress progress, int totalItems) {
            this.progress = progress;
            this.totalItems = Math.max(1, totalItems);
        }

        void setContext(int itemIndex, int totalItems, String itemName) {
            this.itemIndex = itemIndex;
            this.itemName = itemName == null ? "" : itemName;
        }

        @Override
        protected void reportProgress(int progressPercentage, float bestValue) {
            if (progress.cancelled()) {
                cancelAsync();
                return;
            }
            int within = Math.max(0, Math.min(100, progressPercentage));
            int overall = (int) Math.rint((itemIndex + within / 100.0) * 100.0 / totalItems);
            progress.update(Math.max(0, Math.min(100, overall)), itemName);
        }
    }

    private UpgradeItemView toView(Character source, CharacterSlot slot,
            ComparisonCalculationUpgrades row, List<String> labels) {
        if (row == null || row.getItemInstance() == null) {
            return null;
        }
        return toView(source, slot, row.getItemInstance(), row.getOverallPoints(), labels,
                row.getSubPoints(), row.getCharacterItems());
    }

    private UpgradeItemView toView(Character source, CharacterSlot slot, ItemInstance instance,
            float gain, List<String> labels) {
        return toView(source, slot, instance, gain, labels, null, null);
    }

    private UpgradeItemView toView(Character source, CharacterSlot slot, ItemInstance instance,
            float gain, List<String> labels, float[] subPointValues,
            ItemInstance[] characterItems) {
        Item item = instance.getItem();
        if (item == null) {
            return null;
        }
        String gemmedId = instance.getGemmedId();
        String gainText = (gain > 0f ? "+" : "") + RatingFormat.format(gain);
        ItemInstance current = slot == CharacterSlot.None ? null : source.get(slot);
        boolean equipped = current != null && gemmedId.equals(current.getGemmedId());
        return new UpgradeItemView(slot, EditorService.slotLabel(slot), item.getId(), gemmedId,
                item.getName(), item.getQuality().name().toLowerCase(Locale.ROOT),
                item.getItemLevel(), icons.url(item.getIconPath()), icons.initials(item.getName()),
                metaText(source, instance, item), GemService.appliedGemColors(instance),
                comparisonService.isAvailable(source, instance), equipped, gain, gainText,
                subPoints(subPointValues, labels), changes(source, characterItems, slot));
    }

    private static List<ComparisonService.SubPointView> subPoints(float[] values, List<String> labels) {
        if (values == null) {
            return List.of();
        }
        List<ComparisonService.SubPointView> result = new ArrayList<ComparisonService.SubPointView>();
        for (int index = 0; index < values.length; index++) {
            String label = index < labels.size() ? labels.get(index) : "Sub-point " + (index + 1);
            float value = values[index];
            result.add(new ComparisonService.SubPointView(index, label,
                    RatingColors.forLabel(label, index), value,
                    (value > 0f ? "+" : "") + RatingFormat.format(value)));
        }
        return List.copyOf(result);
    }

    private static List<String> subPointLabels(CalculationsBase model) {
        return new ArrayList<String>(model.getSubPointNameColors().keySet());
    }

    private static List<GearChangeView> changes(Character source, ItemInstance[] characterItems,
            CharacterSlot forcedSlot) {
        if (characterItems == null) {
            return List.of();
        }
        List<GearChangeView> changes = new ArrayList<GearChangeView>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            if (slot == forcedSlot) {
                continue;
            }
            ItemInstance before = source.get(slot);
            ItemInstance after = characterItems[slot.value()];
            String beforeId = before == null ? null : before.getGemmedId();
            String afterId = after == null ? null : after.getGemmedId();
            if (java.util.Objects.equals(beforeId, afterId)) {
                continue;
            }
            boolean sameItem = before != null && after != null
                    && before.getItem() != null && after.getItem() != null
                    && before.getId() == after.getId();
            changes.add(new GearChangeView(EditorService.slotLabel(slot),
                    itemName(before), itemName(after), sameItem));
        }
        return List.copyOf(changes);
    }

    private static String itemName(ItemInstance instance) {
        return instance == null || instance.getItem() == null
                ? "Empty" : instance.getItem().getName();
    }

    private static String metaText(Character character, ItemInstance instance, Item item) {
        int gems = 0;
        for (int index = 1; index <= 3; index++) {
            if (instance.getGem(index) != null) {
                gems++;
            }
        }
        StringBuilder text = new StringBuilder("ilvl ").append(item.getItemLevel());
        if (gems > 0) {
            text.append(" · ").append(gems).append(gems == 1 ? " gem" : " gems");
        }
        if (instance.getEnchantId() != 0) {
            Enchant enchant = Enchant.findEnchant(instance.getEnchantId(), item.getSlot(), character);
            if (enchant != null && enchant.id == instance.getEnchantId()) {
                text.append(" · ").append(enchant.name);
            }
        }
        return text.toString();
    }

    private static CharacterSlot findSlot(Character source, ItemInstance instance) {
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            if (instance.getItem() != null && instance.getItem().fitsInSlot(slot, source)) {
                return slot;
            }
        }
        return CharacterSlot.None;
    }

    private record Prepared(Character character, ProgressOptimizer optimizer,
            CalculationsBase model) {
        void close() {
            // The optimizer marks itself busy during a call; nothing to release after the
            // synchronous operation, but keep this hook so the caller cannot leak a clone.
        }
    }
}
