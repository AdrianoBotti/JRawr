package rawr.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;
import rawr.base.talents.GlyphDataAttribute;
import rawr.base.talents.TalentDataAttribute;
import rawr.base.talents.TalentsBase;
import rawr.base.util.Extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import java.util.SortedMap;

/**
 * Builds item comparison charts for the web editor. A chart rates exact gemmed/enchanted
 * instances against an empty slot (the original {@code GetItemCalculations} semantics) and
 * exposes whether each exact row is present in the character's {@code AvailableItems} pool, so the
 * diamond edits that exact instance in the same custom pool the optimizer consumes.
 *
 * <p>Charts are computed under {@link CalculationService#withCalculationLock} because the model
 * instance and the item/enchant caches are process-wide (docs/dev/STATICS_AUDIT.md). Candidates are
 * the equipped instance, the character's available instances, then the highest item-level
 * relevant instances up to the requested limit; limit {@code 0} means every relevant instance.</p>
 */
@Service
public class ComparisonService {
    public static final int DEFAULT_LIMIT = 0;
    public static final int DEFAULT_UPGRADE_LIMIT = 0;
    public static final List<Integer> LIMIT_CHOICES = List.of(30, 60, 120, 0);
    public static final List<Integer> STAT_GRAPH_STEPS = List.of(5, 10, 20);
    public static final int DEFAULT_STAT_GRAPH_STEPS = 10;
    private static final String[] STAT_COLORS = new String[] {
            "#ff6b6b", "#62a8ff", "#f4c95d", "#63d68f", "#b78cff", "#ff9f43",
            "#f06fb5", "#55d6d2", "#c9d36b", "#8fa3b8", "#ffd166", "#a0e57c"
    };

    private final CalculationService calculationService;
    private final ItemIconService icons;
    private final GlobalSettingsService globalSettings;

    @Autowired
    public ComparisonService(CalculationService calculationService, ItemIconService icons,
            GlobalSettingsService globalSettings) {
        this.calculationService = calculationService;
        this.icons = icons;
        this.globalSettings = globalSettings;
    }

    public ComparisonService(CalculationService calculationService, ItemIconService icons) {
        this(calculationService, icons, new GlobalSettingsService());
    }

    /** One sub-point value of a chart row, labelled and coloured like the rating bar. */
    public record SubPointView(int index, String label, String color, float value, String text) {
    }

    /** One segment of a row's stacked bar; {@code weight} sets its relative share. */
    public record BarSegment(String label, String color, int weight) {
    }

    /**
     * The graphical bar of one row. {@code percent} is the overall gain relative to the biggest
     * candidate in the chart (0..100), {@code negative} marks a loss, and {@code segments} splits
     * the bar by sub-point so its composition is visible.
     */
    public record Bar(int percent, boolean negative, List<BarSegment> segments) {
    }


    /** One rated item instance in a chart. */
    public record Row(
            String gemmedId,
            int itemId,
            String name,
            String qualityKey,
            int itemLevel,
            String iconPath,
            String iconUrl,
            String initials,
            String metaText,
            List<String> gemColors,
            boolean equipped,
            boolean exactAvailable,
            float overall,
            String overallText,
            String overallState,
            float equippedDelta,
            String equippedDeltaText,
            String equippedDeltaState,
            List<SubPointView> subPoints,
            List<SubPointView> equippedDeltaSubPoints) {
    }

    public enum Baseline {
        /** Bars show the gain over an empty slot (the classic comparison baseline). */
        EMPTY_SLOT,
        /** Bars show the gain or loss relative to the currently equipped item. */
        EQUIPPED
    }

    /** The chart modes offered by the persistent charts pane. */
    public enum ChartType {
        ITEMS,
        DIRECT_UPGRADES,
        ENCHANTS,
        TALENTS,
        GLYPHS,
        BUFFS,
        GEMS,
        RELATIVE_STATS,
        STAT_GRAPH
    }

    /** The two legacy multiline graph flavors ({@code Graph.UpdateStatsGraph}/{@code UpdateScalingGraph}). */
    public enum StatGraphMode {
        STAT_CHANGE,
        SCALING
    }

    /** One stat in the Relative Stats chart: its rating weight and sub-point breakdown. */
    public record StatRow(
            String name,
            String displayName,
            String description,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /**
     * The Relative Stats chart. {@code normalizedTo} is the stat the model chose as its base
     * (Enhancement: Attack Power, TankDK: Stamina) and therefore reads exactly {@code 1.00};
     * {@code null} when the model does not normalize (Mage and most others).
     */
    public record RelativeStatsChart(
            String overallLabel,
            List<String> subPointLabels,
            List<StatRow> rows,
            Map<String, Bar> bars,
            String normalizedTo,
            long elapsedMillis) {
    }

    /** One selectable stat line in the Stat Graph. */
    public record StatOption(String key, String label, boolean selected) {
    }

    /** One point of one Stat Graph line; {@code pointsCsv} is {@code x,y x,y ...}. */
    public record StatGraphSeries(String key, String label, String color, boolean selected,
            String pointsCsv) {
    }

    /**
     * The legacy multiline Stat Graph: each line is one selected stat, the x-axis is either a
     * change of that stat ({@link StatGraphMode#STAT_CHANGE}) or a change of one chosen base
     * stat ({@link StatGraphMode#SCALING}), and the y-axis is the chosen rating.
     */
    public record StatGraph(
            StatGraphMode mode,
            String xLabel,
            String yLabel,
            String baseStat,
            String baseStatLabel,
            int steps,
            List<String> subPointLabels,
            List<StatGraphSeries> series,
            List<StatOption> statOptions,
            float minX,
            float maxX,
            float minY,
            float maxY,
            long elapsedMillis) {
    }

    /** One positive direct upgrade across all gear slots. */
    public record UpgradeRow(
            CharacterSlot slot,
            String slotLabel,
            String gemmedId,
            int itemId,
            String name,
            String qualityKey,
            int itemLevel,
            String iconPath,
            String iconUrl,
            String initials,
            String metaText,
            List<String> gemColors,
            boolean exactAvailable,
            float gain,
            String gainText,
            String gainState) {
    }

    /** The Direct Upgrades chart: only positive gain instances, all slots, sorted by gain. */
    public record UpgradeChart(
            List<UpgradeRow> rows,
            int evaluated,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** One selectable item slot in the Enchants chart. */
    public record EnchantSlotOption(String key, String label) {
    }

    /** One enchant candidate, rated against the item with no enchant. */
    public record EnchantRow(
            int enchantId,
            String name,
            String statsText,
            boolean equipped,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /** The legacy enchant comparison chart for one equipped item slot. */
    public record EnchantChart(
            CharacterSlot slot,
            String slotLabel,
            List<EnchantSlotOption> slots,
            List<EnchantRow> rows,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** One talent row: the value of the next rank, or of the last rank when maxed. */
    public record TalentRow(
            int index,
            String name,
            int rank,
            int maxPoints,
            boolean maxed,
            boolean invested,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /** The legacy talent comparison chart. */
    public record TalentChart(
            List<TalentRow> rows,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** One glyph row: the value of adding it (or removing it when active). */
    public record GlyphRow(
            int index,
            String name,
            boolean major,
            boolean active,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /** The legacy glyph comparison chart. */
    public record GlyphChart(
            List<GlyphRow> rows,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** One buff comparison row. */
    public record BuffRow(
            String name,
            String group,
            boolean equipped,
            boolean partEquipped,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /** The legacy buff comparison chart. */
    public record BuffChart(
            List<BuffRow> rows,
            List<String> groups,
            String filter,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** One gem comparison row. */
    public record GemRow(
            int itemId,
            String name,
            String qualityKey,
            String iconPath,
            String iconUrl,
            String initials,
            String statsText,
            String colorLabel,
            float overall,
            String overallText,
            String overallState,
            List<SubPointView> subPoints) {
    }

    /** The gem comparison chart (all gems or one socket color). */
    public record GemChart(
            List<GemRow> rows,
            List<String> colors,
            String selectedColor,
            Map<String, Bar> bars,
            long elapsedMillis) {
    }

    /** The complete per-slot chart plus the model's rating labels. */
    public record Chart(
            CharacterSlot slot,
            String slotLabel,
            String overallLabel,
            List<String> subPointLabels,
            List<Row> rows,
            Map<String, Bar> bars,
            Baseline baseline,
            int relevantCount,
            boolean hasAvailability,
            boolean hasEquipped,
            long elapsedMillis) {
    }

    /**
     * Rates the candidate instances for {@code slot}. The returned rows are sorted by overall
     * points descending; the web client can re-sort them without another calculation.
     */
    public Chart gearChart(Character character, CharacterSlot slot, int limit) {
        return gearChart(character, slot, limit, Baseline.EMPTY_SLOT);
    }

    /**
     * Rates the candidate instances for {@code slot} and builds the bars against the chosen
     * baseline. The row values are always the classic empty-slot comparison (the model's
     * {@code getItemCalculations} semantics); only the bar scaling/zero point follows the baseline.
     */
    public Chart gearChart(Character character, CharacterSlot slot, int limit, Baseline baseline) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing items.");
        }
        Baseline selectedBaseline = baseline == null ? Baseline.EMPTY_SLOT : baseline;
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            List<ItemInstance> candidates = candidates(character, slot, limit);
            AvailablePool available = new AvailablePool(character);
            List<Row> rows = new ArrayList<Row>(candidates.size());
            String equippedId = character.get(slot) != null ? character.get(slot).getGemmedId() : null;
            Float equippedOverall = null;
            List<SubPointView> equippedSubPoints = null;

            List<String> labels = subPointLabels(model);
            for (ItemInstance instance : candidates) {
                ComparisonCalculationBase comparison = model.getItemCalculations(instance, character, slot);
                Item item = instance.getItem();
                if (item == null) {
                    continue;
                }
                List<SubPointView> subPoints = new ArrayList<SubPointView>(comparison.getSubPoints().length);
                for (int i = 0; i < comparison.getSubPoints().length; i++) {
                    String label = i < labels.size() ? labels.get(i) : "Sub-point " + (i + 1);
                    float value = comparison.getSubPoints()[i];
                    subPoints.add(new SubPointView(i, label, RatingColors.forLabel(label, i), value,
                            formatSigned(value)));
                }
                float overall = comparison.getOverallPoints();
                boolean equipped = instance.getGemmedId().equals(equippedId);
                if (equipped) {
                    equippedOverall = overall;
                    equippedSubPoints = subPoints;
                }
                rows.add(new Row(
                        instance.getGemmedId(),
                        item.getId(),
                        item.getName(),
                        item.getQuality().name().toLowerCase(Locale.ROOT),
                        item.getItemLevel(),
                        item.getIconPath(),
                        icons.url(item.getIconPath()),
                        icons.initials(item.getName()),
                        metaText(instance, item, character),
                        GemService.appliedGemColors(instance),
                        equipped,
                        available.containsExact(instance.getGemmedId()),
                        overall,
                        formatSigned(overall),
                        overallState(overall),
                        0f,
                        "",
                        "neutral",
                        subPoints,
                        List.of()));
            }

            if (equippedOverall != null) {
                List<Row> withDelta = new ArrayList<Row>(rows.size());
                for (Row row : rows) {
                    float delta = row.overall() - equippedOverall;
                    withDelta.add(new Row(row.gemmedId(), row.itemId(), row.name(), row.qualityKey(),
                            row.itemLevel(), row.iconPath(), row.iconUrl(), row.initials(),
                            row.metaText(), row.gemColors(), row.equipped(), row.exactAvailable(),
                            row.overall(),
                            row.overallText(), row.overallState(), delta, formatSigned(delta),
                            overallState(delta), row.subPoints(),
                            deltaSubPoints(row.subPoints(), equippedSubPoints)));
                }
                rows = withDelta;
            }

            rows.sort(Comparator.comparing(Row::overall).reversed()
                    .thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Row::gemmedId));
            rows = limitGemmingVariants(rows);

            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new Chart(slot, EditorService.slotLabel(slot), model.getOverallRatingLabel(),
                    List.copyOf(labels), List.copyOf(rows), bars(rows, selectedBaseline),
                    selectedBaseline, character.getRelevantItemInstances(slot).size(),
                    available.hasAny(), equippedOverall != null, elapsed);
        });
    }

    /**
     * Computes the model's Relative Stats chart: one row per relevant stat, with the rating
     * change for one point of that stat. The work is done by
     * {@link CalculationsBase#getRelativeStatValues(Character)} under the shared calculation lock;
     * models with a base stat (Enhancement, TankDK) come back normalized so that stat is
     * {@code 1.00}.
     */
    public RelativeStatsChart relativeStatsChart(Character character) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing stats.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            List<String> labels = subPointLabels(model);
            ComparisonCalculationBase[] values = CalculationsBase.getRelativeStatValues(character);
            List<StatRow> rows = new ArrayList<StatRow>(values.length);
            String normalizedTo = null;
            for (ComparisonCalculationBase value : values) {
                String name = value.getName() == null ? "" : value.getName();
                String displayName = name.strip().isEmpty() ? name : name.strip();
                List<SubPointView> subPoints = new ArrayList<SubPointView>(value.getSubPoints().length);
                for (int i = 0; i < value.getSubPoints().length; i++) {
                    String label = i < labels.size() ? labels.get(i) : "Sub-point " + (i + 1);
                    float points = value.getSubPoints()[i];
                    subPoints.add(new SubPointView(i, label, RatingColors.forLabel(label, i), points,
                            formatSigned(points)));
                }
                float overall = value.getOverallPoints();
                rows.add(new StatRow(name, displayName, value.getDescription(), overall,
                        formatSigned(overall), overallState(overall), List.copyOf(subPoints)));
                if (value.getBaseStatOption(character) && name.equals(value.getBaseStat())) {
                    normalizedTo = displayName;
                }
            }

            rows.sort(Comparator.comparing(StatRow::overall).reversed()
                    .thenComparing(StatRow::displayName, String.CASE_INSENSITIVE_ORDER));

            float maxAbs = 0f;
            for (StatRow row : rows) {
                maxAbs = Math.max(maxAbs, Math.abs(row.overall()));
            }
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            for (StatRow row : rows) {
                bars.put(row.displayName(), statBar(row, maxAbs));
            }

            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new RelativeStatsChart(model.getOverallRatingLabel(), List.copyOf(labels),
                    List.copyOf(rows), bars, normalizedTo, elapsed);
        });
    }







    /**
     * Ports the legacy gem comparison chart ({@code ItemComparison.LoadGearBySlot} on the
     * {@code Gems}/{@code Metas} pseudo-slots): every model-relevant gem of the selected color is
     * rated as an additional item, so the result is the socket-independent gain from the gem's
     * stats. Colors follow the original predicates (a red-filter includes hybrid red gems, etc.).
     */
    public GemChart gemChart(Character character, String colorName) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing gems.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            List<String> colors = List.of("All", "Red", "Yellow", "Blue", "Meta");
            String selected = colorName == null || colorName.isBlank() ? "All" : colorName;
            if (!colors.contains(selected)) {
                selected = "All";
            }
            List<String> labels = subPointLabels(model);
            List<GemRow> rows = new ArrayList<GemRow>();
            for (Item item : ItemCache.relevantItems()) {
                if (!item.isGem() || !matchesGemColor(item, selected)
                        || !model.hasRelevantStats(item.getStats())) {
                    continue;
                }
                ComparisonCalculationBase calc = model.getItemCalculations(
                        item, character, CharacterSlot.Gems);
                if (calc == null) {
                    continue;
                }
                rows.add(new GemRow(item.getId(), item.getName(),
                        item.getQuality().name().toLowerCase(Locale.ROOT), item.getIconPath(),
                        icons.url(item.getIconPath()), icons.initials(item.getName()),
                        item.getStats().toString(), item.getSlot().name(), calc.getOverallPoints(),
                        formatSigned(calc.getOverallPoints()), overallState(calc.getOverallPoints()),
                        subPointViews(calc.getSubPoints(), labels)));
            }
            rows.sort(Comparator.comparing(GemRow::overall).reversed()
                    .thenComparing(GemRow::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(GemRow::itemId));
            float max = 0f;
            for (GemRow row : rows) {
                max = Math.max(max, Math.abs(row.overall()));
            }
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            for (GemRow row : rows) {
                bars.put(Integer.toString(row.itemId()), subPointBar(row.overall(),
                        row.subPoints(), max > 0f ? max : 1f));
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new GemChart(List.copyOf(rows), colors, selected, bars, elapsed);
        });
    }

    private static boolean matchesGemColor(Item item, String color) {
        return switch (color) {
            case "Red" -> item.isRedGem();
            case "Yellow" -> item.isYellowGem();
            case "Blue" -> item.isBlueGem();
            case "Meta" -> item.getSlot() == ItemSlot.Meta;
            default -> true;
        };
    }

    /**
     * Ports the legacy {@code CalculationsBase.GetBuffCalculations}: auto-activated buffs from
     * the current calculation are folded into a probe character, then each relevant buff (and its
     * improvements) is compared with and without the buff, removing conflicts. The filter is
     * {@code All}, {@code Current}, or a buff group name.
     */
    public BuffChart buffChart(Character character, String filterName) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing buffs.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            String filter = filterName == null || filterName.isBlank() ? "All" : filterName;
            CharacterCalculationsBase current = model.getCharacterCalculations(
                    character, null, false, true, false);
            Character auto = character.clone();
            for (Buff buff : current.getAutoActivatedBuffs()) {
                if (!auto.getActiveBuffs().contains(buff)) {
                    auto.activeBuffsAdd(buff);
                    Calculations.removeConflictingBuffs(auto.getActiveBuffs(), buff);
                }
            }
            auto.setDisableBuffAutoActivation(true);

            List<Buff> relevant = new ArrayList<Buff>();
            Set<String> groups = new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (Buff buff : Buff.getRelevantBuffs()) {
                if (buff.group != null && !buff.group.isBlank()) {
                    groups.add(buff.group);
                }
                if ("All".equalsIgnoreCase(filter) || "Current".equalsIgnoreCase(filter)
                        || (buff.group != null && buff.group.equalsIgnoreCase(filter))) {
                    relevant.add(buff);
                    relevant.addAll(buff.improvements);
                }
            }
            List<String> groupList = new ArrayList<String>();
            groupList.add("All");
            groupList.add("Current");
            groupList.addAll(groups);

            List<String> labels = subPointLabels(model);
            List<BuffRow> rows = new ArrayList<BuffRow>();
            boolean currentOnly = "Current".equalsIgnoreCase(filter);
            for (Buff buff : relevant) {
                boolean equipped = auto.getActiveBuffs().contains(buff);
                if (currentOnly && !equipped) {
                    continue;
                }
                Character unequipped = auto.clone();
                Character equippedChar = auto.clone();
                unequipped.setDisableBuffAutoActivation(true);
                equippedChar.setDisableBuffAutoActivation(true);
                unequipped.getActiveBuffs().remove(buff);
                if (!equippedChar.getActiveBuffs().contains(buff)) {
                    equippedChar.activeBuffsAdd(buff);
                }
                Calculations.removeConflictingBuffs(equippedChar.getActiveBuffs(), buff);
                Calculations.removeConflictingBuffs(unequipped.getActiveBuffs(), buff);
                CharacterCalculationsBase without = model.getCharacterCalculations(
                        unequipped, null, false, false, false);
                CharacterCalculationsBase with = model.getCharacterCalculations(
                        equippedChar, null, false, false, false);
                float overall = with.getOverallPoints() - without.getOverallPoints();
                float[] withSub = with.getSubPoints();
                float[] withoutSub = without.getSubPoints();
                float[] values = new float[withSub.length];
                for (int index = 0; index < values.length; index++) {
                    values[index] = withSub[index] - withoutSub[index];
                }
                boolean partEquipped = false;
                if (!equipped && buff.getConflictingBuffs() != null && !buff.getConflictingBuffs().isEmpty()) {
                    outer:
                    for (String conflict : buff.getConflictingBuffs()) {
                        if (conflict == null) {
                            continue;
                        }
                        for (Buff active : character.getActiveBuffs()) {
                            if (!active.name.equals(buff.name)
                                    && active.getConflictingBuffs() != null
                                    && active.getConflictingBuffs().contains(conflict)) {
                                partEquipped = true;
                                break outer;
                            }
                        }
                    }
                }
                rows.add(new BuffRow(buff.name, buff.group == null ? "" : buff.group, equipped,
                        partEquipped, overall, formatSigned(overall), overallState(overall),
                        subPointViews(values, labels)));
            }
            rows.sort(Comparator.comparing(BuffRow::overall).reversed()
                    .thenComparing(BuffRow::name, String.CASE_INSENSITIVE_ORDER));
            float max = 0f;
            for (BuffRow row : rows) {
                max = Math.max(max, Math.abs(row.overall()));
            }
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            for (BuffRow row : rows) {
                bars.put(row.name(), subPointBar(row.overall(), row.subPoints(), max > 0f ? max : 1f));
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new BuffChart(List.copyOf(rows), List.copyOf(groupList), filter,
                    bars, elapsed);
        });
    }

    /**
     * Ports the legacy {@code ItemComparison.LoadTalents} comparison chart: for every talent it
     * rates a one-point change from the current build (add the next rank, or evaluate the last
     * rank when already maxed) and reports the rating value of that point.
     */
    public TalentChart talentChart(Character character) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing talents.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            TalentsBase talents = character.getCurrentTalents();
            CharacterCalculationsBase current = model.getCharacterCalculations(
                    character, null, false, true, false);
            List<String> labels = subPointLabels(model);
            List<TalentRow> rows = new ArrayList<TalentRow>();
            for (Method method : talents.getClass().getMethods()) {
                TalentDataAttribute data = method.getAnnotation(TalentDataAttribute.class);
                if (data == null || data.index() < 0 || data.index() >= talents.getData().length) {
                    continue;
                }
                int rank = talents.getData()[data.index()];
                boolean maxed = rank >= data.maxPoints();
                Character probe = character.clone();
                TalentsBase probeTalents = talents.clone();
                probe.setCurrentTalents(probeTalents);
                probeTalents.getData()[data.index()] = maxed ? rank - 1 : rank + 1;
                CharacterCalculationsBase calc = model.getCharacterCalculations(
                        probe, null, false, true, false);
                float[] currentSub = current.getSubPoints();
                float[] probeSub = calc.getSubPoints();
                float overall;
                float[] values = new float[currentSub.length];
                if (maxed) {
                    overall = current.getOverallPoints() - calc.getOverallPoints();
                    for (int index = 0; index < values.length; index++) {
                        values[index] = currentSub[index] - probeSub[index];
                    }
                } else {
                    overall = calc.getOverallPoints() - current.getOverallPoints();
                    for (int index = 0; index < values.length; index++) {
                        values[index] = probeSub[index] - currentSub[index];
                    }
                }
                rows.add(new TalentRow(data.index(), data.name(), rank, data.maxPoints(), maxed,
                        rank != 0, overall, formatSigned(overall), overallState(overall),
                        subPointViews(values, labels)));
            }
            rows.sort(Comparator.comparing(TalentRow::overall).reversed()
                    .thenComparing(TalentRow::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(TalentRow::index));
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            float max = maxAbsTalent(rows);
            for (TalentRow row : rows) {
                bars.put(Integer.toString(row.index()), subPointBar(row.overall(),
                        row.subPoints(), max > 0f ? max : 1f));
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new TalentChart(List.copyOf(rows), bars, elapsed);
        });
    }

    /**
     * Ports the legacy {@code ItemComparison.LoadGlyphs} comparison chart: for every relevant
     * glyph it rates toggling it (adding when inactive, removing when active).
     */
    public GlyphChart glyphChart(Character character) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing glyphs.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            TalentsBase talents = character.getCurrentTalents();
            boolean[] glyphData = talents.getGlyphData();
            if (glyphData == null) {
                return new GlyphChart(List.of(), Map.of(), 0L);
            }
            List<String> relevant = model.getRelevantGlyphs();
            CharacterCalculationsBase current = model.getCharacterCalculations(
                    character, null, false, true, false);
            List<String> labels = subPointLabels(model);
            List<GlyphRow> rows = new ArrayList<GlyphRow>();
            for (Method method : talents.getClass().getMethods()) {
                GlyphDataAttribute data = method.getAnnotation(GlyphDataAttribute.class);
                if (data == null || data.index() < 0 || data.index() >= glyphData.length) {
                    continue;
                }
                if (relevant != null && !relevant.contains(data.name())) {
                    continue;
                }
                boolean active = glyphData[data.index()];
                Character probe = character.clone();
                TalentsBase probeTalents = talents.clone();
                probe.setCurrentTalents(probeTalents);
                probeTalents.getGlyphData()[data.index()] = !active;
                CharacterCalculationsBase calc = model.getCharacterCalculations(
                        probe, null, false, true, false);
                float[] currentSub = current.getSubPoints();
                float[] probeSub = calc.getSubPoints();
                float overall;
                float[] values = new float[currentSub.length];
                if (active) {
                    overall = current.getOverallPoints() - calc.getOverallPoints();
                    for (int index = 0; index < values.length; index++) {
                        values[index] = currentSub[index] - probeSub[index];
                    }
                } else {
                    overall = calc.getOverallPoints() - current.getOverallPoints();
                    for (int index = 0; index < values.length; index++) {
                        values[index] = probeSub[index] - currentSub[index];
                    }
                }
                rows.add(new GlyphRow(data.index(), data.name(), data.major(), active, overall,
                        formatSigned(overall), overallState(overall), subPointViews(values, labels)));
            }
            rows.sort(Comparator.comparing(GlyphRow::overall).reversed()
                    .thenComparing(GlyphRow::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(GlyphRow::index));
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            float max = maxAbsGlyph(rows);
            for (GlyphRow row : rows) {
                bars.put(Integer.toString(row.index()), subPointBar(row.overall(),
                        row.subPoints(), max > 0f ? max : 1f));
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new GlyphChart(List.copyOf(rows), bars, elapsed);
        });
    }

    private static List<SubPointView> subPointViews(float[] values, List<String> labels) {
        List<SubPointView> result = new ArrayList<SubPointView>(values.length);
        for (int index = 0; index < values.length; index++) {
            String label = index < labels.size() ? labels.get(index) : "Sub-point " + (index + 1);
            result.add(new SubPointView(index, label, RatingColors.forLabel(label, index),
                    values[index], formatSigned(values[index])));
        }
        return List.copyOf(result);
    }

    private static float maxAbsTalent(List<TalentRow> rows) {
        float max = 0f;
        for (TalentRow row : rows) {
            max = Math.max(max, Math.abs(row.overall()));
        }
        return max;
    }

    private static float maxAbsGlyph(List<GlyphRow> rows) {
        float max = 0f;
        for (GlyphRow row : rows) {
            max = Math.max(max, Math.abs(row.overall()));
        }
        return max;
    }

    /**
     * Ports the enchant half of the legacy comparison workflow
     * ({@code CalculationsBase.GetEnchantCalculations} with {@code equippedOnly = false}): the
     * baseline is the character with the selected slot's enchant removed, and each fitting,
     * model-relevant enchant is rated against that baseline. "No enchant" is included so the
     * chart can also remove the current enchant.
     */
    public EnchantChart enchantChart(Character character, String slotName) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing enchants.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            List<String> labels = subPointLabels(model);
            List<EnchantSlotOption> slots = new ArrayList<EnchantSlotOption>();
            CharacterSlot selected = null;
            CharacterSlot requested = parseCharacterSlot(slotName);
            for (CharacterSlot slot : CharacterSlot.values()) {
                if (slot == CharacterSlot.None || slot == CharacterSlot.Gems
                        || slot == CharacterSlot.Metas || slot == CharacterSlot.AutoSelect) {
                    continue;
                }
                ItemInstance instance = character.get(slot);
                if (instance == null || instance.getItem() == null) {
                    continue;
                }
                if (!hasEnchantOptions(instance, character, model)) {
                    continue;
                }
                slots.add(new EnchantSlotOption(slot.name(), EditorService.slotLabel(slot)));
                if (requested == slot) {
                    selected = slot;
                }
            }
            if (selected == null && !slots.isEmpty()) {
                selected = CharacterSlot.valueOf(slots.get(0).key());
            }
            long elapsed;
            if (selected == null) {
                elapsed = (System.nanoTime() - start) / 1_000_000L;
                return new EnchantChart(CharacterSlot.None, "", List.copyOf(slots),
                        List.of(), Map.of(), elapsed);
            }

            ItemInstance instance = character.get(selected);
            int currentEnchantId = instance.getEnchantId();
            Character unequipped = character.clone();
            unequipped.setEnchantBySlot(selected, null);
            CharacterCalculationsBase baseline = model.getCharacterCalculations(
                    unequipped, null, false, false, false);
            List<EnchantRow> rows = new ArrayList<EnchantRow>();
            rows.add(enchantRow(0, "No enchant", "No bonus", currentEnchantId == 0,
                    baseline, baseline, labels));
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id == 0 || !instance.isEnchantEligible(enchant)) {
                    continue;
                }
                if (enchant.id != currentEnchantId && !model.isEnchantRelevant(enchant, character)) {
                    continue;
                }
                Character equipped = character.clone();
                equipped.setEnchantBySlot(selected, enchant);
                CharacterCalculationsBase calc = model.getCharacterCalculations(
                        equipped, null, false, false, false);
                String statsText = enchant.stats == null ? "" : enchant.stats.toString();
                rows.add(enchantRow(enchant.id, enchant.name,
                        statsText == null || statsText.isBlank() ? "No bonus" : statsText,
                        enchant.id == currentEnchantId, calc, baseline, labels));
            }

            rows.sort(Comparator.comparing(EnchantRow::overall).reversed()
                    .thenComparing(EnchantRow::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(EnchantRow::enchantId));
            float max = 0f;
            for (EnchantRow row : rows) {
                max = Math.max(max, Math.abs(row.overall()));
            }
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            for (EnchantRow row : rows) {
                bars.put(Integer.toString(row.enchantId()), subPointBar(row.overall(),
                        row.subPoints(), max > 0f ? max : 1f));
            }
            elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new EnchantChart(selected, EditorService.slotLabel(selected),
                    List.copyOf(slots), List.copyOf(rows), bars, elapsed);
        });
    }

    private static EnchantRow enchantRow(int enchantId, String name, String statsText,
            boolean equipped, CharacterCalculationsBase calc, CharacterCalculationsBase baseline,
            List<String> labels) {
        float overall = calc.getOverallPoints() - baseline.getOverallPoints();
        float[] calcSub = calc.getSubPoints();
        float[] baseSub = baseline.getSubPoints();
        List<SubPointView> subPoints = new ArrayList<SubPointView>(calcSub.length);
        for (int index = 0; index < calcSub.length; index++) {
            String label = index < labels.size() ? labels.get(index) : "Sub-point " + (index + 1);
            float value = calcSub[index] - baseSub[index];
            subPoints.add(new SubPointView(index, label, RatingColors.forLabel(label, index), value,
                    formatSigned(value)));
        }
        return new EnchantRow(enchantId, name, statsText, equipped, overall,
                formatSigned(overall), overallState(overall), List.copyOf(subPoints));
    }

    private static boolean hasEnchantOptions(ItemInstance instance, Character character,
            CalculationsBase model) {
        int current = instance.getEnchantId();
        for (Enchant enchant : Enchant.getAllEnchants()) {
            if (enchant.id == 0 || !instance.isEnchantEligible(enchant)) {
                continue;
            }
            if (enchant.id == current || model.isEnchantRelevant(enchant, character)) {
                return true;
            }
        }
        return false;
    }

    private static CharacterSlot parseCharacterSlot(String slotName) {
        if (slotName == null || slotName.isBlank()) {
            return CharacterSlot.None;
        }
        try {
            return CharacterSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            return CharacterSlot.None;
        }
    }

    private static Bar subPointBar(float overall, List<SubPointView> subPoints, float scale) {
        float total = 0f;
        for (SubPointView subPoint : subPoints) {
            total += Math.abs(subPoint.value());
        }
        List<BarSegment> segments = new ArrayList<BarSegment>();
        if (total > 0f) {
            for (SubPointView subPoint : subPoints) {
                int weight = Math.round(Math.abs(subPoint.value()) / total * 10000f);
                if (weight > 0) {
                    segments.add(new BarSegment(subPoint.label(), subPoint.color(), weight));
                }
            }
        }
        if (segments.isEmpty()) {
            segments.add(new BarSegment("", "neutral", 1));
        }
        return bar(overall, scale, segments);
    }

    /**
     * Builds the legacy <b>Direct Upgrades</b> chart ({@code ItemComparison.LoadAvailableGear}).
     * It walks the default item-slot map, chooses the weaker slot for rings/trinkets and the
     * off-hand when the main hand is stronger, and rates each candidate exact instance against
     * the currently equipped item in that slot. Only strictly positive gains are returned; the
     * legacy chart deliberately clears the sub-points and shows the overall gain only.
     *
     * <p>Candidates come from the same per-slot set as the item chart (equipped, the custom
     * availability pool, then highest item-level relevant instances up to {@code limit}); the
     * web default is All, matching the rest of the chart pane.</p>
     */
    public UpgradeChart directUpgrades(Character character, int limit) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing upgrades.");
        }
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            Map<ItemSlot, CharacterSlot> slotMap = new HashMap<ItemSlot, CharacterSlot>();
            if (equippedOverall(character, CharacterSlot.Finger2, model)
                    < equippedOverall(character, CharacterSlot.Finger1, model)) {
                slotMap.put(ItemSlot.Finger, CharacterSlot.Finger2);
            }
            if (equippedOverall(character, CharacterSlot.Trinket2, model)
                    < equippedOverall(character, CharacterSlot.Trinket1, model)) {
                slotMap.put(ItemSlot.Trinket, CharacterSlot.Trinket2);
            }
            if (equippedOverall(character, CharacterSlot.MainHand, model)
                    > equippedOverall(character, CharacterSlot.OffHand, model)) {
                slotMap.put(ItemSlot.OneHand, CharacterSlot.OffHand);
            }

            AvailablePool available = new AvailablePool(character);
            Set<String> seen = new HashSet<String>();
            List<UpgradeRow> rows = new ArrayList<UpgradeRow>();
            int evaluated = 0;
            for (Map.Entry<ItemSlot, CharacterSlot> entry : Item.getDefaultSlotMap().entrySet()) {
                CharacterSlot slot = slotMap.getOrDefault(entry.getKey(), entry.getValue());
                if (slot == CharacterSlot.None) {
                    continue;
                }
                ItemInstance current = character.get(slot);
                float currentOverall = equippedOverall(character, slot, model);
                for (ItemInstance instance : candidates(character, slot, limit)) {
                    if (instance == null || instance.getItem() == null) {
                        continue;
                    }
                    String gemmedId = instance.getGemmedId();
                    if (seen.contains(gemmedId)
                            || (current != null && gemmedId.equals(current.getGemmedId()))) {
                        continue;
                    }
                    if (current != null && current.getItem() != null && current.getItem().isUnique()) {
                        CharacterSlot otherSlot = pairedSlot(slot);
                        ItemInstance other = otherSlot == CharacterSlot.None ? null : character.get(otherSlot);
                        if (other != null && (other.getId() == instance.getId()
                                || (instance.getItem().getUniqueId() != null
                                && instance.getItem().getUniqueId().contains(other.getId())))) {
                            continue;
                        }
                    }
                    evaluated++;
                    ComparisonCalculationBase itemCalc = model.getItemCalculations(instance, character, slot);
                    if (itemCalc == null) {
                        continue;
                    }
                    float difference = itemCalc.getOverallPoints() - currentOverall;
                    if (difference <= 0f) {
                        continue;
                    }
                    seen.add(gemmedId);
                    Item item = instance.getItem();
                    rows.add(new UpgradeRow(slot, EditorService.slotLabel(slot), gemmedId,
                            item.getId(), item.getName(), item.getQuality().name().toLowerCase(Locale.ROOT),
                            item.getItemLevel(), item.getIconPath(), icons.url(item.getIconPath()),
                            icons.initials(item.getName()), metaText(instance, item, character),
                            GemService.appliedGemColors(instance), available.containsExact(gemmedId),
                            difference, formatSigned(difference), overallState(difference)));
                }
            }

            rows.sort(Comparator.comparing(UpgradeRow::gain).reversed()
                    .thenComparing(UpgradeRow::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(UpgradeRow::gemmedId));

            float max = 0f;
            for (UpgradeRow row : rows) {
                max = Math.max(max, Math.abs(row.gain()));
            }
            Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
            for (UpgradeRow row : rows) {
                bars.put(row.gemmedId(), bar(row.gain(), max > 0f ? max : 1f,
                        List.of(new BarSegment("Upgrade", "#62a8ff", 1))));
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            return new UpgradeChart(List.copyOf(rows), evaluated, bars, elapsed);
        });
    }

    private static CharacterSlot pairedSlot(CharacterSlot slot) {
        return switch (slot) {
            case Finger1 -> CharacterSlot.Finger2;
            case Finger2 -> CharacterSlot.Finger1;
            case Trinket1 -> CharacterSlot.Trinket2;
            case Trinket2 -> CharacterSlot.Trinket1;
            case MainHand -> CharacterSlot.OffHand;
            case OffHand -> CharacterSlot.MainHand;
            default -> CharacterSlot.None;
        };
    }

    private static float equippedOverall(Character character, CharacterSlot slot, CalculationsBase model) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            return 0f;
        }
        ComparisonCalculationBase calc = model.getItemCalculations(instance, character, slot);
        return calc == null ? 0f : calc.getOverallPoints();
    }

    /**
     * Computes the legacy multiline Stat Graph (Rawr 3 {@code Graph.UpdateStatsGraph} and
     * {@code UpdateScalingGraph}).
     *
     * <p>In {@link StatGraphMode#STAT_CHANGE} each selected stat gets a line whose x is the amount
     * of that stat added around the current character and whose y is the rating change. In
     * {@link StatGraphMode#SCALING} the x-axis is the amount of one chosen base stat and each line
     * is the relative value of a selected stat at that base-stat level (the original
     * "Scaling vs Spell Power" charts). Both modes use five points of base stat per step and the
     * same relative-stat probe as the original.</p>
     */
    public StatGraph statGraph(Character character, StatGraphMode mode, List<String> requestedStats,
            String baseStatKey, int valueIndex, int steps) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a model before comparing stats.");
        }
        final StatGraphMode selectedMode = mode == null ? StatGraphMode.STAT_CHANGE : mode;
        final int selectedSteps = STAT_GRAPH_STEPS.contains(steps) ? steps : DEFAULT_STAT_GRAPH_STEPS;
        long start = System.nanoTime();
        return calculationService.withCalculationLock(() -> {
            String modelName = character.getCurrentModel();
            CalculationsBase model = Calculations.getModel(modelName);
            Calculations.loadModel(model.getClass());
            if (character.getCalculationOptions() == null) {
                character.setCalculationOptions(CalculationService.createDefaultOptions(modelName));
            }
            character.recalculateSetBonuses();
            model.clearCache();

            List<String> labels = subPointLabels(model);
            int selectedValue = valueIndex >= 0 && valueIndex < labels.size() ? valueIndex : -1;
            String valueLabel = selectedValue < 0 ? model.getOverallRatingLabel() : labels.get(selectedValue);

            List<Stats.StatProperty> relevant = relevantStatProperties();
            Map<String, Stats.StatProperty> relevantByKey = new LinkedHashMap<String, Stats.StatProperty>();
            for (Stats.StatProperty property : relevant) {
                relevantByKey.put(property.name(), property);
            }
            List<Stats.StatProperty> properties = new ArrayList<Stats.StatProperty>();
            Map<String, Float> weights = new LinkedHashMap<String, Float>();
            for (StatGraphPresets.Entry entry : StatGraphPresets.forModel(modelName)) {
                Stats.StatProperty property = relevantByKey.get(entry.key());
                if (property != null) {
                    properties.add(property);
                    weights.put(entry.key(), entry.weight());
                }
            }
            if (properties.isEmpty()) {
                properties.addAll(relevant);
                for (Stats.StatProperty property : properties) {
                    weights.put(property.name(), 1f);
                }
            }
            if (properties.isEmpty()) {
                throw new IllegalArgumentException("This model has no relevant stats to graph.");
            }

            Set<String> requested = new HashSet<String>();
            if (requestedStats != null) {
                for (String key : requestedStats) {
                    if (key != null && !key.isBlank()) {
                        requested.add(key.trim());
                    }
                }
            }
            List<String> selectedKeys = new ArrayList<String>();
            for (Stats.StatProperty property : properties) {
                if (requested.contains(property.name())) {
                    selectedKeys.add(property.name());
                }
            }
            if (selectedKeys.isEmpty()) {
                int limit = Math.min(8, properties.size());
                for (int index = 0; index < limit; index++) {
                    selectedKeys.add(properties.get(index).name());
                }
            }

            String selectedBase = validBaseStat(baseStatKey, properties);
            String baseStatLabel = displayName(properties, selectedBase);
            List<StatOption> options = new ArrayList<StatOption>(properties.size());
            List<Stats.StatProperty> sortedProperties = new ArrayList<Stats.StatProperty>(properties);
            sortedProperties.sort(Comparator.comparing((Stats.StatProperty property) -> displayName(property),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(Stats.StatProperty::name));
            for (Stats.StatProperty property : sortedProperties) {
                options.add(new StatOption(property.name(), displayName(property),
                        selectedKeys.contains(property.name())));
            }

            List<Stats.StatProperty> selected = new ArrayList<Stats.StatProperty>(sortedProperties);

            model.clearCache();
            List<StatGraphSeries> series = new ArrayList<StatGraphSeries>();
            float minX = selectedMode == StatGraphMode.STAT_CHANGE ? -selectedSteps : -selectedSteps * 5f;
            float maxX = selectedMode == StatGraphMode.STAT_CHANGE ? selectedSteps : selectedSteps * 5f;
            float minY = Float.POSITIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;

            if (selectedMode == StatGraphMode.STAT_CHANGE) {
                CharacterCalculationsBase baseCalc = Calculations.getCharacterCalculations(character);
                float baseFigure = calculationValue(baseCalc, selectedValue);
                for (int index = 0; index < selected.size(); index++) {
                    Stats unit = new Stats();
                    selected.get(index).mutator().set(unit,
                            weights.getOrDefault(selected.get(index).name(), 1f));
                    Stats newStats = new Stats();
                    newStats.accumulate(unit, -selectedSteps - 1);
                    StringBuilder points = new StringBuilder();
                    for (int count = -selectedSteps; count <= selectedSteps; count++) {
                        newStats.accumulate(unit);
                        Item item = new Item();
                        item.setStats(newStats);
                        CharacterCalculationsBase calc = Calculations.getCharacterCalculations(
                                character, item, false, false, false);
                        float value = calculationValue(calc, selectedValue) - baseFigure;
                        appendPoint(points, count, value);
                        minY = Math.min(minY, value);
                        maxY = Math.max(maxY, value);
                    }
                    series.add(new StatGraphSeries(selected.get(index).name(),
                            displayName(selected.get(index)),
                            STAT_COLORS[index % STAT_COLORS.length],
                            selectedKeys.contains(selected.get(index).name()), points.toString()));
                }
            } else {
                Stats baseUnit = new Stats();
                Stats.StatProperty baseProperty = propertyByKey(properties, selectedBase);
                baseProperty.mutator().set(baseUnit, 5f);
                float unit = 5f;
                float[][] values = new float[selected.size()][2 * selectedSteps + 1];
                Item item = null;
                for (int count = -selectedSteps; count <= selectedSteps; count++) {
                    Stats newStats = new Stats();
                    newStats.accumulate(baseUnit, count);
                    item = new Item();
                    item.setStats(newStats);
                    Calculations.getCharacterCalculations(character, item, true, false, false);
                    for (int index = 0; index < selected.size(); index++) {
                        ComparisonCalculationBase ccb = CalculationsBase.getRelativeStatValue(
                                character, selected.get(index), item,
                                weights.getOrDefault(selected.get(index).name(), 1f));
                        float value = ccb == null ? 0f : calculationValue(ccb, selectedValue);
                        values[index][count + selectedSteps] = value;
                        minY = Math.min(minY, value);
                        maxY = Math.max(maxY, value);
                    }
                }
                Stats emptyStats = new Stats();
                Item emptyItem = new Item();
                emptyItem.setStats(emptyStats);
                Calculations.getCharacterCalculations(character, emptyItem, true, false, false);
                for (int index = 0; index < selected.size(); index++) {
                    StringBuilder points = new StringBuilder();
                    for (int count = -selectedSteps; count <= selectedSteps; count++) {
                        appendPoint(points, count * unit, values[index][count + selectedSteps]);
                    }
                    series.add(new StatGraphSeries(selected.get(index).name(),
                            displayName(selected.get(index)),
                            STAT_COLORS[index % STAT_COLORS.length],
                            selectedKeys.contains(selected.get(index).name()), points.toString()));
                }
            }

            if (minY == Float.POSITIVE_INFINITY) {
                minY = 0f;
                maxY = 1f;
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            boolean scaling = selectedMode == StatGraphMode.SCALING;
            return new StatGraph(selectedMode,
                    scaling ? "Change in " + baseStatLabel : "Stat change",
                    scaling ? valueLabel : valueLabel + " change",
                    scaling ? selectedBase : null,
                    scaling ? baseStatLabel : null,
                    selectedSteps, List.copyOf(labels), List.copyOf(series), List.copyOf(options),
                    minX, maxX, minY, maxY, elapsed);
        });
    }


    /** All additive stats the current model declares relevant, in stat-name order. */
    private static List<Stats.StatProperty> relevantStatProperties() {
        Stats allStats = new Stats();
        Arrays.fill(allStats.getRawAdditiveData(), 1f);
        SortedMap<String, Float> relevant = Calculations.getRelevantStats(allStats).values(value -> value > 0f);
        List<Stats.StatProperty> properties = new ArrayList<Stats.StatProperty>(relevant.size());
        for (String name : relevant.keySet()) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
            if (property != null) {
                properties.add(property);
            }
        }
        return properties;
    }

    private static String validBaseStat(String requested, List<Stats.StatProperty> properties) {
        if (requested != null) {
            for (Stats.StatProperty property : properties) {
                if (property.name().equals(requested)) {
                    return property.name();
                }
            }
        }
        for (String preferred : List.of("SpellPower", "AttackPower", "Stamina", "Agility", "Intellect")) {
            for (Stats.StatProperty property : properties) {
                if (property.name().equals(preferred)) {
                    return property.name();
                }
            }
        }
        return properties.get(0).name();
    }

    private static Stats.StatProperty propertyByKey(List<Stats.StatProperty> properties, String key) {
        for (Stats.StatProperty property : properties) {
            if (property.name().equals(key)) {
                return property;
            }
        }
        return properties.get(0);
    }

    private static String displayName(List<Stats.StatProperty> properties, String key) {
        for (Stats.StatProperty property : properties) {
            if (property.name().equals(key)) {
                return displayName(property);
            }
        }
        return key;
    }

    private static String displayName(Stats.StatProperty property) {
        String name = Extensions.displayName(property);
        return name.startsWith(" ") ? name.substring(1) : name;
    }

    private static void appendPoint(StringBuilder builder, float x, float y) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(Float.toString(x)).append(',').append(Float.toString(y));
    }

    private static float calculationValue(CharacterCalculationsBase calc, int valueIndex) {
        return valueIndex < 0 ? calc.getOverallPoints() : calc.getSubPoints()[valueIndex];
    }

    private static float calculationValue(ComparisonCalculationBase calc, int valueIndex) {
        return valueIndex < 0 ? calc.getOverallPoints() : calc.getSubPoints()[valueIndex];
    }

    /** Stacked bar for one stat row, scaled to the strongest stat in the chart. */
    private static Bar statBar(StatRow row, float maxAbs) {
        float total = 0f;
        for (SubPointView subPoint : row.subPoints()) {
            total += Math.abs(subPoint.value());
        }
        List<BarSegment> segments = new ArrayList<BarSegment>();
        if (total > 0f) {
            for (SubPointView subPoint : row.subPoints()) {
                int weight = Math.round(Math.abs(subPoint.value()) / total * 10000f);
                if (weight > 0) {
                    segments.add(new BarSegment(subPoint.label(), subPoint.color(), weight));
                }
            }
        }
        if (segments.isEmpty()) {
            segments.add(new BarSegment("", "neutral", 1));
        }
        return bar(row.overall(), maxAbs > 0f ? maxAbs : 1f, segments);
    }

    /**
     * Builds the stacked bars for one baseline. With {@link Baseline#EQUIPPED} the length and the
     * segments are the difference from the equipped row; since that row is a constant offset,
     * the sort order is identical to the empty-slot view.
     */
    private static Map<String, Bar> bars(List<Row> rows, Baseline baseline) {
        Row equipped = null;
        if (baseline == Baseline.EQUIPPED) {
            for (Row row : rows) {
                if (row.equipped()) {
                    equipped = row;
                    break;
                }
            }
        }
        float baseOverall = equipped == null ? 0f : equipped.overall();
        float[] baseSubPoints = equipped == null ? new float[0] : subPointValues(equipped);

        float maxAbs = 0f;
        for (Row row : rows) {
            maxAbs = Math.max(maxAbs, Math.abs(row.overall() - baseOverall));
        }
        float scale = maxAbs > 0f ? maxAbs : 1f;
        Map<String, Bar> bars = new LinkedHashMap<String, Bar>();
        for (Row row : rows) {
            float value = row.overall() - baseOverall;
            List<SubPointView> subPoints = row.subPoints();
            float total = 0f;
            for (int index = 0; index < subPoints.size(); index++) {
                total += Math.abs(subPointDelta(subPoints, baseSubPoints, index));
            }
            List<BarSegment> segments = new ArrayList<BarSegment>();
            if (total > 0f) {
                for (int index = 0; index < subPoints.size(); index++) {
                    int weight = Math.round(Math.abs(subPointDelta(subPoints, baseSubPoints, index))
                            / total * 10000f);
                    if (weight > 0) {
                        segments.add(new BarSegment(subPoints.get(index).label(),
                                subPoints.get(index).color(), weight));
                    }
                }
            }
            if (segments.isEmpty()) {
                segments.add(new BarSegment("", "neutral", 1));
            }
            bars.put(row.gemmedId(), bar(value, scale, segments));
        }
        return bars;
    }

    /** Builds one bar: percentage of {@code scale}, loss flag and per-sub-point segments. */
    private static Bar bar(float value, float scale, List<BarSegment> segments) {
        float fraction = Math.min(1f, Math.abs(value) / scale);
        int percent = fraction > 0f ? Math.max(1, Math.round(fraction * 100f)) : 0;
        return new Bar(percent, value < 0f, List.copyOf(segments));
    }

    private static float[] subPointValues(Row row) {
        List<SubPointView> subPoints = row.subPoints();
        float[] values = new float[subPoints.size()];
        for (int index = 0; index < subPoints.size(); index++) {
            values[index] = subPoints.get(index).value();
        }
        return values;
    }

    private static float subPointDelta(List<SubPointView> subPoints, float[] base, int index) {
        float value = subPoints.get(index).value();
        return value - (index < base.length ? base[index] : 0f);
    }

    /** Per-sub-point difference from the equipped row, for the Current Build reference. */
    private static List<SubPointView> deltaSubPoints(List<SubPointView> subPoints,
            List<SubPointView> base) {
        if (base == null) {
            return List.of();
        }
        List<SubPointView> result = new ArrayList<SubPointView>(subPoints.size());
        for (int index = 0; index < subPoints.size(); index++) {
            SubPointView subPoint = subPoints.get(index);
            float baseValue = index < base.size() ? base.get(index).value() : 0f;
            float delta = subPoint.value() - baseValue;
            result.add(new SubPointView(subPoint.index(), subPoint.label(), subPoint.color(), delta,
                    formatSigned(delta)));
        }
        return List.copyOf(result);
    }

    /** True when the item has an item-level or exact-instance entry in the availability list. */
    public boolean isAvailable(Character character, ItemInstance instance) {
        return new AvailablePool(character).contains(instance);
    }

    /** True when this exact gemmed/enchanted instance is an availability entry. */
    public boolean isExactAvailable(Character character, String gemmedId) {
        return new AvailablePool(character).containsExact(gemmedId);
    }

    /**
     * Adds or removes one exact gemming/enchant entry. This is the comparison diamond's behavior:
     * it never adds or removes item-level entries or other variants of the same item. The per-slot
     * instance cache is invalidated like the legacy UI does after an availability edit.
     */
    public void setExactAvailable(Character character, String gemmedId, boolean available) {
        List<String> entries = character.getAvailableItems();
        entries.removeIf(gemmedId::equals);
        if (available) {
            entries.add(gemmedId);
        }
        character.invalidateItemInstances();
    }

    /**
     * Shows at most the configured number of gemming variants per item. The equipped instance and
     * exact entries in the custom item pool are always kept, matching the legacy picker promise.
     */
    private List<Row> limitGemmingVariants(List<Row> rows) {
        int limit = Math.max(1, globalSettings.current().topGemmings());
        Map<Integer, Integer> keptPerItem = new LinkedHashMap<>();
        List<Row> limited = new ArrayList<>(rows.size());
        for (Row row : rows) {
            int kept = keptPerItem.getOrDefault(row.itemId(), 0);
            if (row.equipped() || row.exactAvailable() || kept < limit) {
                limited.add(row);
                keptPerItem.put(row.itemId(), kept + 1);
            }
        }
        return limited;
    }

    private static List<ItemInstance> candidates(Character character, CharacterSlot slot, int limit) {
        LinkedHashMap<String, ItemInstance> selected = new LinkedHashMap<String, ItemInstance>();
        ItemInstance equipped = character.get(slot);
        if (equipped != null && equipped.getItem() != null) {
            selected.put(equipped.getGemmedId(), equipped);
        }

        List<ItemInstance> relevant = new ArrayList<ItemInstance>(character.getRelevantItemInstances(slot));
        AvailablePool available = new AvailablePool(character);
        for (ItemInstance instance : relevant) {
            if (available.contains(instance)) {
                selected.putIfAbsent(instance.getGemmedId(), instance);
            }
        }
        if (limit > 0) {
            List<ItemInstance> byLevel = new ArrayList<ItemInstance>(relevant);
            byLevel.sort(Comparator.comparingInt((ItemInstance instance) ->
                            instance.getItem() != null ? instance.getItem().getItemLevel() : 0).reversed()
                    .thenComparing(ItemInstance::getGemmedId));
            for (ItemInstance instance : byLevel) {
                if (selected.size() >= limit) {
                    break;
                }
                selected.putIfAbsent(instance.getGemmedId(), instance);
            }
        } else {
            for (ItemInstance instance : relevant) {
                selected.putIfAbsent(instance.getGemmedId(), instance);
            }
        }
        return new ArrayList<ItemInstance>(selected.values());
    }

    private List<String> subPointLabels(CalculationsBase model) {
        List<String> labels = new ArrayList<String>();
        if (model.getSubPointNameColors() != null) {
            labels.addAll(model.getSubPointNameColors().keySet());
        }
        return labels;
    }

    private static String metaText(ItemInstance instance, Item item, Character character) {
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

    private static String formatSigned(float value) {
        if (value > 0f) {
            return "+" + RatingFormat.format(value);
        }
        return RatingFormat.format(value);
    }

    private static String overallState(float value) {
        if (value > 0f) return "positive";
        if (value < 0f) return "negative";
        return "neutral";
    }

    /** Item-level and exact-instance view of {@code Character.AvailableItems}. */
    private static final class AvailablePool {
        private final Set<Integer> itemIds = new HashSet<Integer>();
        private final Set<String> instances = new HashSet<String>();

        private AvailablePool(Character character) {
            for (String entry : character.getAvailableItems()) {
                if (entry == null || entry.isEmpty() || entry.charAt(0) == '-') {
                    continue;
                }
                int dot = entry.indexOf('.');
                if (dot < 0) {
                    try {
                        itemIds.add(Integer.parseInt(entry));
                    } catch (NumberFormatException e) {
                        // ignore malformed availability entries
                    }
                } else {
                    instances.add(entry);
                }
            }
        }

        private boolean contains(ItemInstance instance) {
            return itemIds.contains(instance.getId()) || instances.contains(instance.getGemmedId());
        }

        private boolean containsExact(String gemmedId) {
            return instances.contains(gemmedId);
        }

        private boolean hasAny() {
            return !itemIds.isEmpty() || !instances.isEmpty();
        }
    }
}
