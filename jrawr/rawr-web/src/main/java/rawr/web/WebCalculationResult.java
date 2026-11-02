package rawr.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * View model for one calculated character: the ratings a model exposes plus its display map.
 * The overall label and the sub-point labels come from the additive model metadata API
 * ({@code CalculationsBase.getOverallRatingLabel()/getSubPointNameColors()}); models that do not
 * override it fall back to {@code "Overall"} and {@code "Sub-point N"}. Sub-point colours come from
 * {@link RatingColors}, a curated high-contrast palette for the dark web UI.
 *
 * <p>The rating bar shows the overall score; {@link #subPointRows()} feeds its sub-rating pills.
 * The stats panel renders {@link #displayGroups()}: the model's display map ordered and
 * grouped by {@code CalculationsBase.getCharacterDisplayCalculationLabels()} ({@code "Group:Label"}
 * entries, optional {@code "*Tooltip"} suffix). Keys the label list does not cover keep the map
 * order and go last under {@value #DEFAULT_DISPLAY_GROUP}. {@link #baseGroups()} holds the static
 * character stats (groups whose name contains {@code "Stats"}), {@link #advancedGroups()} the
 * derived values (rotations, sequences, aggregates and unmapped keys). A display value may carry a
 * {@code '*'} that starts a secondary block and newlines that split lines; both are rendered as
 * separate lines.
 */
public record WebCalculationResult(
        String overallRatingLabel,
        float overallPoints,
        float[] subPoints,
        Map<String, String> subPointNameColors,
        Map<String, String> displayValues,
        List<String> displayLabels) {

    /** Group for display keys that no entry of the model's label list maps. */
    public static final String DEFAULT_DISPLAY_GROUP = "Other";

    /** A sub-point is "negligible" below this fraction of the overall score (and overall != 0). */
    private static final float NEGLIGIBLE_FRACTION = 0.01f;

    /** Convenience constructor for callers without display-label metadata (labels default empty). */
    public WebCalculationResult(
            String overallRatingLabel,
            float overallPoints,
            float[] subPoints,
            Map<String, String> subPointNameColors,
            Map<String, String> displayValues) {
        this(overallRatingLabel, overallPoints, subPoints, subPointNameColors, displayValues, List.of());
    }

    public record SubPointRow(int index, String label, String color, float value, boolean negligible) {
        public String text() {
            return RatingFormat.format(value);
        }
    }

    /** One sidebar row: label, value lines, and an optional explanation from the label metadata. */
    public record DisplayRow(String label, String primary, List<String> secondary, String tooltip) {
    }

    /**
     * A named sidebar section. {@code advanced} marks derived values (rotations, sequences,
     * aggregates); the template renders those collapsed behind a {@code <details>} element.
     */
    public record DisplayGroup(String name, boolean advanced, List<DisplayRow> rows) {
        public boolean isAdvanced() {
            return advanced;
        }
    }

    /** One parsed {@code "Group:Label*Tooltip"} entry of the model's display-label list. */
    private record LabelRef(String group, int order, String tooltip) {
    }

    /** A display-map row with the position assigned by the label list (unmatched rows go last). */
    private record OrderedRow(String group, int order, DisplayRow row) {
    }

    public String overallPointsText() {
        return RatingFormat.format(overallPoints);
    }

    /** All sub-points in {@code getSubPoints()} order, labelled from the metadata and coloured by the web palette. */
    public List<SubPointRow> subPointRows() {
        List<Map.Entry<String, String>> metadata = subPointNameColors == null
                ? List.<Map.Entry<String, String>>of()
                : new ArrayList<>(subPointNameColors.entrySet());
        List<SubPointRow> rows = new ArrayList<>(subPoints.length);
        for (int i = 0; i < subPoints.length; i++) {
            boolean hasMetadata = i < metadata.size();
            String label = hasMetadata ? metadata.get(i).getKey() : "Sub-point " + (i + 1);
            rows.add(new SubPointRow(i, label, RatingColors.forLabel(label, i), subPoints[i],
                    isNegligible(subPoints[i])));
        }
        return rows;
    }

    /**
     * The display map ordered and grouped by the model's label list: entries the labels cover take
     * the label's group and position, everything else keeps the map order and goes last under
     * {@value #DEFAULT_DISPLAY_GROUP}. Base-stat groups (name contains {@code "Stats"}) and
     * advanced groups (rotations, sequences, aggregates) keep their relative label order.
     */
    public List<DisplayGroup> displayGroups() {
        Map<String, LabelRef> byLabel = labelIndex();
        List<OrderedRow> ordered = new ArrayList<>();
        int unmatchedOrder = byLabel.size();
        for (Map.Entry<String, String> entry : displayValues().entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            int colon = key.indexOf(':');
            String label = colon > 0 ? key.substring(colon + 1).trim() : key;
            LabelRef ref = byLabel.get(key);
            if (ref == null) {
                ref = byLabel.get(label);
            }
            boolean mapped = ref != null;
            String group = mapped ? ref.group() : DEFAULT_DISPLAY_GROUP;
            int order = mapped ? ref.order() : unmatchedOrder++;
            String tooltip = mapped ? ref.tooltip() : null;
            ordered.add(new OrderedRow(group, order, parseDisplayRow(label, entry.getValue(), tooltip)));
        }
        ordered.sort((a, b) -> Integer.compare(a.order(), b.order()));

        Map<String, List<DisplayRow>> grouped = new LinkedHashMap<>();
        Map<String, Boolean> advancedByGroup = new HashMap<>();
        for (OrderedRow orderedRow : ordered) {
            grouped.computeIfAbsent(orderedRow.group(), name -> new ArrayList<>())
                    .add(orderedRow.row());
            advancedByGroup.putIfAbsent(orderedRow.group(), !orderedRow.group().contains("Stats"));
        }
        List<DisplayGroup> result = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<DisplayRow>> group : grouped.entrySet()) {
            result.add(new DisplayGroup(group.getKey(), advancedByGroup.get(group.getKey()),
                    List.copyOf(group.getValue())));
        }
        return result;
    }

    /** Base-stat groups (group name contains {@code "Stats"}), in the model's label order. */
    public List<DisplayGroup> baseGroups() {
        return groupsBySection(false);
    }

    /** Advanced groups (rotations, sequences, aggregates and unmapped keys), label order. */
    public List<DisplayGroup> advancedGroups() {
        return groupsBySection(true);
    }

    public boolean hasBaseGroups() {
        return !baseGroups().isEmpty();
    }

    public boolean hasAdvancedGroups() {
        return !advancedGroups().isEmpty();
    }

    private List<DisplayGroup> groupsBySection(boolean advanced) {
        List<DisplayGroup> section = new ArrayList<>();
        for (DisplayGroup group : displayGroups()) {
            if (group.advanced() == advanced) {
                section.add(group);
            }
        }
        return List.copyOf(section);
    }

    private Map<String, LabelRef> labelIndex() {
        Map<String, LabelRef> index = new HashMap<>();
        if (displayLabels == null) {
            return index;
        }
        int order = 0;
        for (String spec : displayLabels) {
            if (spec == null) {
                continue;
            }
            int star = spec.indexOf('*');
            String base = star >= 0 ? spec.substring(0, star) : spec;
            int colon = base.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String group = base.substring(0, colon).trim();
            String label = base.substring(colon + 1).trim();
            String tooltip = star >= 0 ? spec.substring(star + 1).trim() : null;
            if (tooltip != null && tooltip.isEmpty()) {
                tooltip = null;
            }
            index.putIfAbsent(label, new LabelRef(group, order++, tooltip));
        }
        return index;
    }

    private static DisplayRow parseDisplayRow(String label, String raw, String tooltip) {
        String value = raw == null ? "" : raw;
        int star = value.indexOf('*');
        String head = star >= 0 ? value.substring(0, star) : value;
        String tail = star >= 0 ? value.substring(star + 1) : null;
        List<String> headLines = splitLines(head);
        String primary = headLines.isEmpty() ? "" : headLines.get(0).strip();
        List<String> secondary = new ArrayList<>();
        for (int i = 1; i < headLines.size(); i++) {
            addLine(secondary, headLines.get(i));
        }
        if (tail != null) {
            for (String line : splitLines(tail)) {
                addLine(secondary, line);
            }
        }
        return new DisplayRow(label, primary, List.copyOf(secondary), tooltip);
    }

    private static void addLine(List<String> lines, String line) {
        String stripped = line.strip();
        if (!stripped.isEmpty()) {
            lines.add(stripped);
        }
    }

    private static List<String> splitLines(String text) {
        return text.isEmpty() ? List.of() : List.of(text.split("\r\n|\n|\r", -1));
    }

    private boolean isNegligible(float value) {
        return overallPoints != 0f && Math.abs(value) < Math.abs(overallPoints) * NEGLIGIBLE_FRACTION;
    }
}
