package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterRegex;
import rawr.base.items.ItemFilterRegexList;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemType;
import rawr.data.SavedItemFilterStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Reads and edits the global item filter tree, its per-model item types and the Other flag. */
@Service
public class ItemFilterService {
    private static final int MAX_TEXT = 500;
    private static final int MAX_LEVEL = 1000;

    public record NodeView(String path, int depth, String name, String searchText, boolean enabled,
            boolean effectiveEnabled, boolean additive, boolean otherEnabled, boolean selected,
            boolean expanded, int enabledRules, int totalRules, List<NodeView> children) {
        /** True when the row's checkbox should render checked (the effective state is all on). */
        public boolean isChecked() {
            return children.isEmpty() ? effectiveEnabled : enabledRules == totalRules;
        }

        /** Aggregate row state: all rules on, none on, or a partial mixture. */
        public String checkState() {
            if (children.isEmpty()) {
                return effectiveEnabled ? "all" : "none";
            }
            if (enabledRules == totalRules) {
                return "all";
            }
            return enabledRules == 0 ? "none" : "some";
        }
    }

    public record TypeView(String name, boolean checked) {
    }

    public record EditorView(List<NodeView> includeNodes, List<NodeView> excludeNodes,
            String selectedPath, ItemFilterRegex selected, boolean selectedTopLevel,
            boolean selectedExcludeTree, String modelName, boolean hasModel, boolean otherEnabled,
            List<TypeView> types, List<String> qualities, int totalRules, int enabledRules) {
    }

    private final SavedItemFilterStore store;
    private final CalculationService calculationService;

    public ItemFilterService(SavedItemFilterStore store, CalculationService calculationService) {
        this.store = store;
        this.calculationService = calculationService;
    }

    public EditorView describe(Character character, String path) {
        String selectedPath = blank(path) ? firstPath() : path;
        ItemFilterRegex selected = null;
        try {
            selected = selectedPath == null ? null : nodeAt(selectedPath);
        } catch (IllegalArgumentException e) {
            selectedPath = firstPath();
            selected = selectedPath == null ? null : nodeAt(selectedPath);
        }
        List<NodeView> nodes = buildViews(ItemFilter.getFilterList(), "", 0, selectedPath, true);
        List<NodeView> includeNodes = new ArrayList<>();
        List<NodeView> excludeNodes = new ArrayList<>();
        for (NodeView node : nodes) {
            (node.additive() ? includeNodes : excludeNodes).add(node);
        }
        boolean selectedTopLevel = false;
        boolean selectedExcludeTree = false;
        if (selectedPath != null) {
            int dot = selectedPath.indexOf('.');
            selectedTopLevel = dot < 0;
            int topIndex = Integer.parseInt(dot < 0 ? selectedPath : selectedPath.substring(0, dot));
            selectedExcludeTree = !ItemFilter.getFilterList().get(topIndex).isAdditiveFilter();
        }
        String modelName = character.getCurrentModel();
        boolean hasModel = modelName != null && !modelName.isBlank();
        List<TypeView> types = new ArrayList<>();
        if (hasModel) {
            try {
                calculationService.ensureModelLoaded(character);
                List<ItemType> relevant = ItemFilter.getRelevantItemTypesList(
                        Calculations.getInstance());
                for (ItemType type : ItemType.values()) {
                    types.add(new TypeView(type.name(), relevant.contains(type)));
                }
            } catch (RuntimeException e) {
                hasModel = false;
                for (ItemType type : ItemType.values()) {
                    types.add(new TypeView(type.name(), false));
                }
            }
        } else {
            for (ItemType type : ItemType.values()) {
                types.add(new TypeView(type.name(), false));
            }
        }
        List<String> qualities = new ArrayList<>();
        for (ItemQuality quality : ItemQuality.values()) {
            qualities.add(quality.name());
        }
        return new EditorView(List.copyOf(includeNodes), List.copyOf(excludeNodes), selectedPath,
                selected, selectedTopLevel, selectedExcludeTree, modelName, hasModel,
                ItemFilter.isOtherEnabled(), List.copyOf(types), List.copyOf(qualities),
                countRules(nodes), countEnabledRules(nodes));
    }

    public void apply(String path, Map<String, String> params) {
        ItemFilterRegex node = nodeAt(path);
        String name = text(params, "name", node.getName());
        String pattern = text(params, "pattern", node.getPattern() == null ? "" : node.getPattern());
        validatePattern(pattern);
        int minLevel = integer(params, "minItemLevel", node.getMinItemLevel(), 0, MAX_LEVEL,
                "Minimum item level");
        int maxLevel = integer(params, "maxItemLevel", node.getMaxItemLevel(), 0, MAX_LEVEL,
                "Maximum item level");
        if (minLevel > maxLevel) {
            throw new IllegalArgumentException("Minimum item level cannot exceed maximum item level.");
        }
        ItemQuality minQuality = quality(params.get("minItemQuality"), node.getMinItemQuality());
        ItemQuality maxQuality = quality(params.get("maxItemQuality"), node.getMaxItemQuality());
        if (minQuality.ordinal() > maxQuality.ordinal()) {
            throw new IllegalArgumentException("Minimum quality cannot exceed maximum quality.");
        }
        node.setName(name);
        node.setPattern(pattern.isBlank() ? null : pattern);
        node.setMinItemLevel(minLevel);
        node.setMaxItemLevel(maxLevel);
        node.setMinItemQuality(minQuality);
        node.setMaxItemQuality(maxQuality);
        node.setBoA(booleanValue(params, "boA"));
        node.setBoE(booleanValue(params, "boE"));
        node.setBoP(booleanValue(params, "boP"));
        node.setBoU(booleanValue(params, "boU"));
        node.setBoN(booleanValue(params, "boN"));
        // The include/exclude flag is only editable for top-level rules; child rules keep theirs
        // (it still feeds the original BoN matching quirk).
        if (parentPath(path).isEmpty()) {
            node.setAdditiveFilter(booleanValue(params, "additiveFilter"));
        }
        node.setAppliesToItems(booleanValue(params, "appliesToItems"));
        node.setAppliesToGems(booleanValue(params, "appliesToGems"));
        // Groups have no raw Enabled checkbox: their switch is the tree's deep checkbox. Saving
        // other group fields must not clear the group's own flag. Leaves still use the form field.
        if (node.regexList.isEmpty()) {
            node.setEnabled(booleanValue(params, "enabled"));
            if (node.isEnabled()) {
                enableAncestors(path);
            }
        }
        // The catch-all only matters for groups; a leaf keeps its stored value.
        if (!node.regexList.isEmpty()) {
            node.otherRegexEnabled = booleanValue(params, "otherRegexEnabled");
        }
        changed();
    }

    public String add(String parentPath) {
        ItemFilterRegex node = new ItemFilterRegex();
        node.setName("New Filter");
        if (blank(parentPath)) {
            ItemFilter.getFilterList().add(node);
            changed();
            return Integer.toString(ItemFilter.getFilterList().size() - 1);
        }
        ItemFilterRegex parent = nodeAt(parentPath);
        parent.regexList.add(node);
        changed();
        return parentPath + "." + (parent.regexList.size() - 1);
    }

    public String delete(String path) {
        ItemFilterRegexList list = listFor(path);
        int index = index(path);
        list.remove(index);
        changed();
        return nextPath(path);
    }

    public void setOther(boolean enabled) {
        ItemFilter.setOtherEnabled(enabled);
        changed();
    }

    public void setEnabled(String path, boolean enabled, boolean deep) {
        ItemFilterRegex node = nodeAt(path);
        boolean changed;
        if (deep) {
            changed = setEnabledDeep(node, enabled);
        } else {
            changed = setNodeEnabled(node, enabled);
        }
        if (enabled) {
            changed |= enableAncestors(path);
        }
        if (changed) {
            changed();
        }
    }

    /**
     * Enables the parent chain of a filter. An item/filter under a disabled parent is effectively
     * off, so turning the filter on must also turn its ancestors on for the click to have an
     * effect and for the tree's aggregate flags to stay coherent.
     */
    private static boolean enableAncestors(String path) {
        boolean changed = false;
        String[] parts = path.split("\\.");
        ItemFilterRegexList list = ItemFilter.getFilterList();
        for (int i = 0; i < parts.length - 1; i++) {
            ItemFilterRegex ancestor = list.get(Integer.parseInt(parts[i]));
            changed |= setNodeEnabled(ancestor, true);
            list = ancestor.regexList;
        }
        return changed;
    }

    private static boolean setNodeEnabled(ItemFilterRegex node, boolean enabled) {
        if (node.isEnabled() == enabled) {
            return false;
        }
        node.setEnabled(enabled);
        return true;
    }

    private static boolean setEnabledDeep(ItemFilterRegex node, boolean enabled) {
        boolean changed = setNodeEnabled(node, enabled);
        for (ItemFilterRegex child : node.regexList) {
            changed |= setEnabledDeep(child, enabled);
        }
        return changed;
    }

    public void setTypes(Character character, List<String> rawTypes) {
        String modelName = character.getCurrentModel();
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Choose a model before editing its item types.");
        }
        calculationService.ensureModelLoaded(character);
        Set<ItemType> selected = new LinkedHashSet<>();
        if (rawTypes != null) {
            for (String raw : rawTypes) {
                if (raw == null || raw.isBlank()) continue;
                try {
                    selected.add(ItemType.valueOf(raw.trim()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unknown item type: " + raw);
                }
            }
        }
        List<ItemType> list = ItemFilter.getRelevantItemTypesList(Calculations.getInstance());
        list.clear();
        list.addAll(selected);
        changed();
    }

    public void invalidate(Character character) {
        calculationService.withCalculationLock(() -> {
            character.onCalculationsInvalidated();
            return null;
        });
    }

    private void changed() {
        calculationService.withCalculationLock(() -> {
            store.save();
            ItemCache.onItemsChanged();
            return null;
        });
    }

    private static String firstPath() {
        return ItemFilter.getFilterList().isEmpty() ? null : "0";
    }

    private static List<NodeView> buildViews(List<ItemFilterRegex> list, String prefix, int depth,
            String selectedPath, boolean parentEnabled) {
        List<NodeView> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String path = prefix.isEmpty() ? Integer.toString(i) : prefix + "." + i;
            ItemFilterRegex node = list.get(i);
            boolean effectiveEnabled = parentEnabled && node.isEnabled();
            List<NodeView> children = buildViews(node.regexList, path, depth + 1, selectedPath,
                    effectiveEnabled);
            int totalRules;
            int enabledRules;
            if (children.isEmpty()) {
                totalRules = 1;
                enabledRules = effectiveEnabled ? 1 : 0;
            } else {
                totalRules = 0;
                enabledRules = 0;
                for (NodeView child : children) {
                    totalRules += child.totalRules();
                    enabledRules += child.enabledRules();
                }
                if (!effectiveEnabled) {
                    enabledRules = 0;
                }
            }
            String name = node.getName();
            String pattern = node.getPattern();
            String searchText = ((name == null ? "" : name) + " "
                    + (pattern == null ? "" : pattern)).trim();
            boolean expanded = selectedPath != null && selectedPath.startsWith(path + ".");
            out.add(new NodeView(path, depth, name, searchText, node.isEnabled(),
                    effectiveEnabled, node.isAdditiveFilter(), node.otherRegexEnabled,
                    path.equals(selectedPath), expanded, enabledRules, totalRules,
                    List.copyOf(children)));
        }
        return out;
    }

    private static int countRules(List<NodeView> nodes) {
        int count = 0;
        for (NodeView node : nodes) {
            count += node.totalRules();
        }
        return count;
    }

    private static int countEnabledRules(List<NodeView> nodes) {
        int count = 0;
        for (NodeView node : nodes) {
            count += node.enabledRules();
        }
        return count;
    }

    private static ItemFilterRegex nodeAt(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Choose a filter first.");
        }
        String[] parts = path.split("\\.");
        ItemFilterRegexList list = ItemFilter.getFilterList();
        ItemFilterRegex node = null;
        for (String part : parts) {
            int index;
            try {
                index = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Choose a valid filter.");
            }
            if (index < 0 || index >= list.size()) {
                throw new IllegalArgumentException("That filter no longer exists.");
            }
            node = list.get(index);
            list = node.regexList;
        }
        return node;
    }

    private static ItemFilterRegexList listFor(String path) {
        String parent = parentPath(path);
        if (parent.isEmpty()) {
            return ItemFilter.getFilterList();
        }
        return nodeAt(parent).regexList;
    }

    private static int index(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Choose a filter first.");
        }
        int dot = path.lastIndexOf('.');
        try {
            return Integer.parseInt(dot < 0 ? path : path.substring(dot + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Choose a valid filter.");
        }
    }

    private static String parentPath(String path) {
        int dot = path == null ? -1 : path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    private static String nextPath(String path) {
        String parent = parentPath(path);
        ItemFilterRegexList list = listFor(path);
        if (list.isEmpty()) {
            return "";
        }
        int index = Math.min(index(path), list.size() - 1);
        return (parent.isEmpty() ? "" : parent + ".") + index;
    }

    private static void validatePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        try {
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    | Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid pattern: " + e.getDescription());
        }
    }

    private static ItemQuality quality(String raw, ItemQuality fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return ItemQuality.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown item quality: " + raw);
        }
    }

    private static String text(Map<String, String> params, String key, String fallback) {
        String value = params.get(key);
        if (value == null) return fallback;
        value = value.trim();
        if (value.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Text fields must be at most " + MAX_TEXT + " characters.");
        }
        return value;
    }

    private static int integer(Map<String, String> params, String key, int fallback, int min,
            int max, String label) {
        String value = params.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private static boolean booleanValue(Map<String, String> params, String key) {
        return params.containsKey(key) && ("on".equalsIgnoreCase(params.get(key))
                || "true".equalsIgnoreCase(params.get(key)));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
