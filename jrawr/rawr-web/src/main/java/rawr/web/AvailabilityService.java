package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.items.sources.ItemLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * View and edit model for the character's {@code AvailableItems} list, the candidate universe the
 * optimizer uses in Custom item pool mode. Item-level entries ({@code itemId}) allow every
 * gemming/enchant; exact entries ({@code itemId.gem1.gem2.gem3.enchant}) allow one instance, with
 * {@code *} for wildcard gems/enchants. Ordering is preserved for saved-character round trips.
 *
 * <p>The list shown for a slot is the model-relevant items plus any available item that fits the
 * slot (even when the current filter would hide it, so stale entries can be removed). Every edit
 * invalidates {@link Character#invalidateItemInstances()} like the legacy availability toggles.</p>
 */
@Service
public class AvailabilityService {
    private static final int MAX_SEARCH_RESULTS = 100;

    private final CalculationService calculationService;
    private final ItemIconService icons;

    public AvailabilityService(CalculationService calculationService, ItemIconService icons) {
        this.calculationService = calculationService;
        this.icons = icons;
    }

    /** One item row in the manager. */
    public record Row(
            int itemId,
            String name,
            String qualityKey,
            int itemLevel,
            String itemLevelText,
            String iconUrl,
            String initials,
            String sourceText,
            boolean equipped,
            boolean itemLevelAvailable,
            boolean relevant,
            int exactCount) {
    }

    /** A pool entry that cannot be shown as an item row or exact slot entry. */
    public record OtherEntryView(String entry, String description) {
    }

    /** One exact gemming/enchant availability entry for the selected slot. */
    public record ExactEntryView(
            String entry,
            int itemId,
            String itemName,
            String qualityKey,
            String iconUrl,
            String initials,
            String description,
            boolean equipped) {
    }

    /** Everything the availability modal needs for one slot. */
    public record View(
            CharacterSlot slot,
            String slotLabel,
            String query,
            List<Row> rows,
            List<ExactEntryView> exactEntries,
            List<OtherEntryView> otherEntries,
            int itemLevelCount,
            int exactCount,
            int availabilityCount,
            String equippedEntry,
            boolean equippedEntryAvailable) {
    }

    public record ImportResult(int added, int total) {
    }

    /** Builds the manager view for {@code slot}; {@code query} filters by name or item id. */
    public View view(Character character, CharacterSlot slot, String query) {
        requireModel(character);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        Map<Integer, Item> items = new LinkedHashMap<Integer, Item>();
        Set<Integer> relevantIds = new HashSet<Integer>();
        for (Item item : character.getRelevantItems(slot, ItemSlot.None)) {
            relevantIds.add(item.getId());
            items.put(item.getId(), item);
        }

        Set<Integer> itemLevelIds = new LinkedHashSet<Integer>();
        Map<Integer, List<ParsedEntry>> exactByItem = new LinkedHashMap<Integer, List<ParsedEntry>>();
        List<ParsedEntry> exactEntries = new ArrayList<ParsedEntry>();
        for (String raw : character.getAvailableItems()) {
            ParsedEntry entry = parse(raw);
            if (entry == null) {
                continue;
            }
            Item item = ItemCache.findItemById(entry.itemId());
            if (item != null && !item.fitsInSlot(slot, character)) {
                continue;
            }
            if (item != null) {
                items.putIfAbsent(item.getId(), item);
            }
            if (entry.exact()) {
                exactEntries.add(entry);
                exactByItem.computeIfAbsent(entry.itemId(), id -> new ArrayList<ParsedEntry>())
                        .add(entry);
            } else {
                itemLevelIds.add(entry.itemId());
            }
        }

        if (!needle.isEmpty()) {
            CalculationsBase model = Calculations.getModel(character.getCurrentModel());
            int added = 0;
            for (Item item : ItemCache.allItems()) {
                if (added >= MAX_SEARCH_RESULTS) {
                    break;
                }
                if (item == null || item.getName() == null || items.containsKey(item.getId())) {
                    continue;
                }
                if (!matches(needle, item) || !item.fitsInSlot(slot, character)
                        || !classAndFactionCompatible(item, model, character)) {
                    continue;
                }
                items.put(item.getId(), item);
                added++;
            }
        }

        ItemInstance equipped = character.get(slot);
        int equippedId = equipped != null && equipped.getItem() != null ? equipped.getId() : 0;
        String equippedEntry = equipped != null && equipped.getItem() != null
                ? availabilityEntry(equipped) : null;

        List<Item> ordered = new ArrayList<Item>(items.values());
        ordered.sort(Comparator
                .comparingInt((Item item) -> item.getId() == equippedId ? 0 : 1)
                .thenComparing(Comparator.comparingInt(Item::getItemLevel).reversed())
                .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Item::getId));

        List<Row> rows = new ArrayList<Row>();
        for (Item item : ordered) {
            if (!matches(needle, item)) {
                continue;
            }
            rows.add(new Row(
                    item.getId(),
                    item.getName(),
                    item.getQuality().name().toLowerCase(Locale.ROOT),
                    item.getItemLevel(),
                    "ilvl " + item.getItemLevel(),
                    icons.url(item.getIconPath()),
                    icons.initials(item.getName()),
                    sourceText(item),
                    item.getId() == equippedId,
                    itemLevelIds.contains(item.getId()),
                    relevantIds.contains(item.getId()),
                    exactByItem.getOrDefault(item.getId(), List.of()).size()));
        }

        List<ExactEntryView> exactViews = new ArrayList<ExactEntryView>();
        for (ParsedEntry entry : exactEntries) {
            Item item = ItemCache.findItemById(entry.itemId());
            if (item == null || !item.fitsInSlot(slot, character)) {
                continue;
            }
            exactViews.add(new ExactEntryView(
                    entry.entry(),
                    item.getId(),
                    item.getName(),
                    item.getQuality().name().toLowerCase(Locale.ROOT),
                    icons.url(item.getIconPath()),
                    icons.initials(item.getName()),
                    exactDescription(entry, item, character),
                    equippedEntry != null && equippedEntry.equals(entry.entry())));
        }
        exactViews.sort(Comparator
                .comparing(ExactEntryView::itemName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ExactEntryView::entry));

        List<OtherEntryView> otherEntries = otherEntries(character);

        return new View(slot, EditorService.slotLabel(slot),
                query == null ? "" : query.trim(),
                List.copyOf(rows), List.copyOf(exactViews), otherEntries,
                itemLevelIds.size(), exactViews.size(), character.getAvailableItems().size(),
                equippedEntry, equippedEntry != null && character.getAvailableItems().contains(equippedEntry));
    }

    /** Empty view used when the manager cannot build the real one (no model selected). */
    public View emptyView(CharacterSlot slot, String query) {
        return new View(slot, EditorService.slotLabel(slot), query == null ? "" : query.trim(),
                List.of(), List.of(), List.of(), 0, 0, 0, null, false);
    }

    /** Adds/removes the item-level entry; removing also drops every exact entry for the item. */
    public void setItemLevelAvailable(Character character, int itemId, boolean available) {
        List<String> entries = character.getAvailableItems();
        entries.removeIf(entry -> entryItemId(entry) == itemId);
        if (available) {
            entries.add(Integer.toString(itemId));
        }
        character.invalidateItemInstances();
    }

    /** Adds an exact entry when it is not already present; returns false for a duplicate. */
    public boolean addExact(Character character, String entry) {
        ParsedEntry parsed = parse(entry);
        if (parsed == null || !parsed.exact()) {
            throw new IllegalArgumentException("Not an exact gemming/enchant entry: " + entry);
        }
        if (character.getAvailableItems().contains(parsed.entry())) {
            return false;
        }
        character.getAvailableItems().add(parsed.entry());
        character.invalidateItemInstances();
        return true;
    }

    /** Removes one exact entry (all copies of the same string). */
    public boolean removeExact(Character character, String entry) {
        ParsedEntry parsed = parse(entry);
        if (parsed == null || !parsed.exact()) {
            throw new IllegalArgumentException("Not an exact gemming/enchant entry: " + entry);
        }
        boolean removed = character.getAvailableItems().removeIf(parsed.entry()::equals);
        if (removed) {
            character.invalidateItemInstances();
        }
        return removed;
    }

    /** Adds the exact entry for the equipped item in {@code slot}; returns false when absent. */
    public boolean addEquippedExact(Character character, CharacterSlot slot) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            return false;
        }
        return addExact(character, availabilityEntry(instance));
    }

    /** Runs one bulk operation. Returns a short user-facing message. */
    public String bulk(Character character, CharacterSlot slot, String action) {
        if (action == null) {
            throw new IllegalArgumentException("Choose a bulk action.");
        }
        switch (action) {
            case "EQUIPPED":
                replaceWithEquipped(character);
                return "Replaced the custom item pool with the equipped items.";
            case "ALL_RELEVANT":
                replaceWithAllRelevant(character);
                return "Replaced the custom item pool with every relevant item.";
            case "CLEAR_SLOT":
                clearSlot(character, slot);
                return "Removed every custom-pool entry for " + EditorService.slotLabel(slot) + ".";
            case "CLEAR_ALL":
                character.getAvailableItems().clear();
                character.invalidateItemInstances();
                return "Cleared the custom item pool.";
            default:
                throw new IllegalArgumentException("Unknown bulk action: " + action);
        }
    }

    /**
     * Replaces the list with the exported text. Lines are one entry each; blank lines are skipped,
     * duplicates are removed and the input order is preserved. Returns the number added.
     */
    public ImportResult importEntries(Character character, String text, boolean append) {
        List<String> parsed = parseImport(text);
        List<String> entries = character.getAvailableItems();
        if (!append) {
            entries.clear();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<String>(entries);
        int added = 0;
        for (String entry : parsed) {
            if (seen.add(entry)) {
                entries.add(entry);
                added++;
            }
        }
        character.invalidateItemInstances();
        return new ImportResult(added, entries.size());
    }

    /** The full custom item pool, one entry per line. */
    public String exportText(Character character) {
        return String.join("\n", character.getAvailableItems());
    }

    /** The addon-compatible availability entry for an instance (bare id or wildcarded exact). */
    public static String availabilityEntry(ItemInstance instance) {
        boolean noGems = instance.getGem1Id() == 0 && instance.getGem2Id() == 0
                && instance.getGem3Id() == 0;
        if (noGems && instance.getEnchantId() == 0) {
            return Integer.toString(instance.getId());
        }
        return instance.getId() + "."
                + (instance.getGem1Id() == 0 ? "*" : Integer.toString(instance.getGem1Id())) + "."
                + (instance.getGem2Id() == 0 ? "*" : Integer.toString(instance.getGem2Id())) + "."
                + (instance.getGem3Id() == 0 ? "*" : Integer.toString(instance.getGem3Id())) + "."
                + (instance.getEnchantId() == 0 ? "*" : Integer.toString(instance.getEnchantId()));
    }

    /** True when the item may legally be used by this character's class and faction. */
    public boolean isUsableItem(Character character, Item item) {
        if (item == null) {
            return false;
        }
        requireModel(character);
        CalculationsBase model = Calculations.getModel(character.getCurrentModel());
        return classAndFactionCompatible(item, model, character);
    }

    /** True when {@code entry} is an exact entry for a usable item fitting {@code slot}. */
    public boolean entryFitsSlot(Character character, String entry, CharacterSlot slot) {
        ParsedEntry parsed = parse(entry);
        if (parsed == null || !parsed.exact()) {
            return false;
        }
        Item item = ItemCache.findItemById(parsed.itemId());
        return item != null && isUsableItem(character, item) && item.fitsInSlot(slot, character);
    }

    /** Removes any raw availability entry (exact, negative enchant token or malformed). */
    public boolean removeEntry(Character character, String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        String trimmed = entry.trim();
        boolean removed = character.getAvailableItems()
                .removeIf(raw -> raw != null && raw.trim().equals(trimmed));
        if (removed) {
            character.invalidateItemInstances();
        }
        return removed;
    }

    // ------------------------------------------------------------------ helpers

    private List<OtherEntryView> otherEntries(Character character) {
        List<OtherEntryView> entries = new ArrayList<OtherEntryView>();
        Set<String> seen = new LinkedHashSet<String>();
        for (String raw : character.getAvailableItems()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String entry = raw.trim();
            if (entry.charAt(0) == '-') {
                if (seen.add(entry)) {
                    entries.add(new OtherEntryView(entry, enchantTokenDescription(entry)));
                }
                continue;
            }
            ParsedEntry parsed = parse(entry);
            if (parsed == null) {
                if (seen.add(entry)) {
                    entries.add(new OtherEntryView(entry, "Unrecognized availability entry"));
                }
            } else if (ItemCache.findItemById(parsed.itemId()) == null && seen.add(entry)) {
                entries.add(new OtherEntryView(entry,
                        "Item " + parsed.itemId() + " is not in the item cache"));
            }
        }
        entries.sort(Comparator.comparing(OtherEntryView::entry));
        return List.copyOf(entries);
    }

    private static boolean classAndFactionCompatible(Item item, CalculationsBase model,
            Character character) {
        String required = item.getRequiredClasses();
        if (required != null && !required.isEmpty()
                && !required.replace(" ", "").contains(model.getTargetClass().toString())) {
            return false;
        }
        return item.fitsFaction(character.getRace());
    }

    private static String enchantTokenDescription(String entry) {
        try {
            int raw = -Integer.parseInt(entry);
            int enchantId = raw % 10000;
            int slotOrdinal = raw / 10000;
            String slot = slotOrdinal >= 0 && slotOrdinal < ItemSlot.values().length
                    ? ItemSlot.values()[slotOrdinal].name() : "slot " + slotOrdinal;
            Enchant found = null;
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id == enchantId && enchant.slot.ordinal() == slotOrdinal) {
                    found = enchant;
                    break;
                }
            }
            return "Enchant availability · "
                    + (found == null ? "enchant " + enchantId : found.name) + " (" + slot + ")";
        } catch (NumberFormatException e) {
            return "Enchant availability token";
        }
    }

    private void requireModel(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("Choose a model before managing availability.");
        }
        calculationService.ensureModelLoaded(character);
    }

    private void replaceWithEquipped(Character character) {
        LinkedHashSet<String> entries = new LinkedHashSet<String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                entries.add(availabilityEntry(instance));
            }
        }
        character.getAvailableItems().clear();
        character.getAvailableItems().addAll(entries);
        character.invalidateItemInstances();
    }

    private void replaceWithAllRelevant(Character character) {
        requireModel(character);
        String modelName = character.getCurrentModel();
        CalculationsBase model = Calculations.getModel(modelName);
        LinkedHashSet<String> entries = new LinkedHashSet<String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            Set<Integer> ids = new TreeSet<Integer>();
            for (Item item : ItemCache.getRelevantItems(model, character.getRace())) {
                if (item.fitsInSlot(slot, character)) {
                    ids.add(item.getId());
                }
            }
            for (int id : ids) {
                entries.add(Integer.toString(id));
            }
        }
        character.getAvailableItems().clear();
        character.getAvailableItems().addAll(entries);
        character.invalidateItemInstances();
    }

    private void clearSlot(Character character, CharacterSlot slot) {
        character.getAvailableItems().removeIf(entry -> {
            ParsedEntry parsed = parse(entry);
            if (parsed == null) {
                return false;
            }
            Item item = ItemCache.findItemById(parsed.itemId());
            return item != null && item.fitsInSlot(slot, character);
        });
        character.invalidateItemInstances();
    }

    private static List<String> parseImport(String text) {
        List<String> entries = new ArrayList<String>();
        if (text == null || text.isBlank()) {
            return entries;
        }
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.charAt(0) == '-') {
                try {
                    if (Integer.parseInt(line) < 0) {
                        entries.add(line);
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                    // fall through to the error below
                }
            } else {
                ParsedEntry parsed = parse(line);
                if (parsed != null) {
                    entries.add(parsed.entry());
                    continue;
                }
            }
            throw new IllegalArgumentException("Line " + (i + 1)
                    + " is not a valid availability entry: " + line);
        }
        return entries;
    }

    private static boolean matches(String needle, Item item) {
        if (needle.isEmpty()) {
            return true;
        }
        return item.getName().toLowerCase(Locale.ROOT).contains(needle)
                || Integer.toString(item.getId()).contains(needle);
    }

    private static int entryItemId(String entry) {
        ParsedEntry parsed = parse(entry);
        return parsed == null ? Integer.MIN_VALUE : parsed.itemId();
    }

    /** Parsed positive availability entry; negative enchant tokens are preserved but not shown. */
    private record ParsedEntry(String entry, int itemId, boolean exact) {
    }

    static ParsedEntry parse(String raw) {
        if (raw == null) {
            return null;
        }
        String entry = raw.trim();
        if (entry.isEmpty() || entry.charAt(0) == '-') {
            return null;
        }
        String[] parts = entry.split("\\.", -1);
        if (parts.length != 1 && parts.length != 5) {
            return null;
        }
        int itemId;
        try {
            itemId = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (itemId <= 0) {
            return null;
        }
        if (parts.length == 5) {
            for (int i = 1; i < 5; i++) {
                if ("*".equals(parts[i])) {
                    continue;
                }
                try {
                    if (Integer.parseInt(parts[i]) < 0) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return new ParsedEntry(entry, itemId, parts.length == 5);
    }

    private static String exactDescription(ParsedEntry entry, Item item, Character character) {
        String[] parts = entry.entry().split("\\.", -1);
        if (parts.length != 5) {
            return "Item-level entry";
        }
        List<String> gems = new ArrayList<String>(3);
        for (int i = 1; i <= 3; i++) {
            if ("*".equals(parts[i])) {
                gems.add("any gem");
            } else {
                int gemId = Integer.parseInt(parts[i]);
                if (gemId != 0) {
                    Item gem = ItemCache.findItemById(gemId);
                    gems.add(gem == null ? "gem " + gemId : gem.getName());
                }
            }
        }
        String enchant;
        if ("*".equals(parts[4])) {
            enchant = "any enchant";
        } else {
            int enchantId = Integer.parseInt(parts[4]);
            if (enchantId == 0) {
                enchant = "";
            } else {
                Enchant found = Enchant.findEnchant(enchantId, item.getSlot(), character);
                enchant = found == null ? "enchant " + enchantId : found.name;
            }
        }
        StringBuilder text = new StringBuilder();
        if (gems.isEmpty()) {
            text.append("no gems");
        } else {
            text.append(String.join(", ", gems));
        }
        if (!enchant.isEmpty()) {
            text.append(" · ").append(enchant);
        }
        return text.toString();
    }

    private static String sourceText(Item item) {
        for (ItemLocation location : item.getLocationInfo()) {
            if (location == null) {
                continue;
            }
            String description = location.getDescription();
            if (description == null) {
                continue;
            }
            String cleaned = description.replaceAll("\\bnull\\b", "")
                    .replaceAll("\\s+", " ").trim();
            if (!cleaned.isEmpty() && !"Sold by at".equals(cleaned)) {
                return cleaned;
            }
        }
        return "";
    }
}
