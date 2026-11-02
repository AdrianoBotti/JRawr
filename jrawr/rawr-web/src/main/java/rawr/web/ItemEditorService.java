package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemDamageType;
import rawr.base.items.ItemFaction;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.stats.Stats;
import rawr.data.ItemXmlParseException;
import rawr.data.UserItemStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Item editor's data layer. It parses batch input, fetches items from the configured source,
 * keeps the undo snapshots for the current session, and builds the Add and Edit tab view models.
 *
 * <p>Only items that are missing from the shipped cache are stored in {@link UserItemStore}; base
 * items are never shadowed, so refreshing and removing only applies to user-owned items. Items in
 * the reserved custom-id range ({@link #CUSTOM_ITEM_ID_MIN}+) are treated as custom copies and are
 * not refreshed from the source.</p>
 */
@Service
public class ItemEditorService {

    /** First id of the reserved range used by custom item copies. */
    public static final int CUSTOM_ITEM_ID_MIN = 1_000_000;

    static final String UNDO_SESSION = "jrawr.itemEditorUndo";
    static final int MAX_UNDO = 50;
    static final int MAX_LIST_ROWS = 100;

    private final ItemSourceService itemSources;
    private final UserItemStore userItems;
    private final ItemTooltipService tooltips;
    private final ItemIconService icons;

    public ItemEditorService(ItemSourceService itemSources, UserItemStore userItems,
            ItemTooltipService tooltips, ItemIconService icons) {
        this.itemSources = itemSources;
        this.userItems = userItems;
        this.tooltips = tooltips;
        this.icons = icons;
    }

    /** Outcome of one add/fetch row. */
    public enum AddStatus {
        QUEUED("is-fetching", "Fetching"),
        ADDED("is-added", "Added"),
        CACHED("is-cached", "Already cached"),
        REFRESHED("is-refreshed", "Updated"),
        NOT_FOUND("is-missing", "Not found"),
        ERROR("is-missing", "Error"),
        INVALID("is-invalid", "Invalid"),
        UNDONE("is-undone", "Removed");

        private final String cssClass;
        private final String label;

        AddStatus(String cssClass, String label) {
            this.cssClass = cssClass;
            this.label = label;
        }

        public String cssClass() {
            return cssClass;
        }

        public String label() {
            return label;
        }
    }

    /** Where an item in the local cache came from. */
    public enum Origin {
        BASE("is-base", "Base"),
        FETCHED("is-fetched", "Fetched"),
        CUSTOM("is-custom", "Custom");

        private final String cssClass;
        private final String label;

        Origin(String cssClass, String label) {
            this.cssClass = cssClass;
            this.label = label;
        }

        public String cssClass() {
            return cssClass;
        }

        public String label() {
            return label;
        }
    }

    /** One row in the Add tab, from the initial queue through a completed or failed fetch. */
    public record AddRow(
            int itemId,
            String token,
            AddStatus status,
            boolean fetch,
            boolean refresh,
            String name,
            String qualityKey,
            int itemLevel,
            String slotText,
            String meta,
            String iconUrl,
            String initials,
            ItemTooltip tooltip,
            List<String> badges,
            String message,
            boolean canUndo,
            boolean canRefresh,
            boolean canOpenPicker,
            String pickerSlot) {

        public boolean isQueued() {
            return status == AddStatus.QUEUED;
        }

        public boolean isAdded() {
            return status == AddStatus.ADDED;
        }

        public boolean isCached() {
            return status == AddStatus.CACHED;
        }

        public boolean isRefreshed() {
            return status == AddStatus.REFRESHED;
        }

        public boolean isNotFound() {
            return status == AddStatus.NOT_FOUND;
        }

        public boolean isError() {
            return status == AddStatus.ERROR;
        }

        public boolean isInvalid() {
            return status == AddStatus.INVALID;
        }

        public boolean isUndone() {
            return status == AddStatus.UNDONE;
        }

        public boolean canRetry() {
            return itemId > 0 && (status == AddStatus.NOT_FOUND || status == AddStatus.ERROR
                    || status == AddStatus.UNDONE);
        }

        public boolean hasPreview() {
            return tooltip != null;
        }

        public boolean expandedPreview() {
            return status == AddStatus.ADDED || status == AddStatus.REFRESHED;
        }

        public String statusKey() {
            return status.name().toLowerCase(Locale.ROOT);
        }

        public String statusClass() {
            return status.cssClass();
        }

        public String statusLabel() {
            return status.label();
        }

        public String qualityClass() {
            return qualityKey == null || qualityKey.isBlank() ? "" : "q-" + qualityKey;
        }

        public String iconClass() {
            String quality = qualityClass();
            return (quality.isEmpty() ? "q-common" : quality)
                    + (iconUrl == null || iconUrl.isBlank() ? " icon-missing" : "");
        }

        public String iconInitial() {
            return initials == null || initials.isBlank() ? "?" : initials;
        }

        public String displayName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            if (token != null && !token.isBlank()) {
                return token;
            }
            return "Item " + itemId;
        }
    }

    /** The parsed batch rendered before the live per-item fetches start. */
    public record QueueView(List<AddRow> rows, int total, boolean truncated, String sourceHost,
            String message) {
        public boolean hasRows() {
            return !rows.isEmpty();
        }

        public boolean hasMessage() {
            return message != null && !message.isBlank();
        }

        public String summary() {
            int fetching = 0;
            int cached = 0;
            int invalid = 0;
            for (AddRow row : rows) {
                switch (row.status()) {
                    case QUEUED -> fetching++;
                    case CACHED -> cached++;
                    case INVALID -> invalid++;
                    default -> {
                    }
                }
            }
            List<String> parts = new ArrayList<String>();
            if (fetching > 0) {
                parts.add(fetching + " to fetch");
            }
            if (cached > 0) {
                parts.add(cached + " cached");
            }
            if (invalid > 0) {
                parts.add(invalid + " invalid");
            }
            return parts.isEmpty() ? "No items" : String.join(" · ", parts);
        }
    }

    /** One row in the Edit tab list. */
    public record ListRow(int itemId, String name, String qualityKey, int itemLevel,
            String slotText, String slotName, String iconUrl, String initials, Origin origin) {
        public String qualityClass() {
            return qualityKey == null || qualityKey.isBlank() ? "" : "q-" + qualityKey;
        }

        public String originClass() {
            return origin.cssClass();
        }

        public String originLabel() {
            return origin.label();
        }

        public String iconClass() {
            String quality = qualityClass();
            return (quality.isEmpty() ? "q-common" : quality)
                    + (iconUrl == null || iconUrl.isBlank() ? " icon-missing" : "");
        }

        public String iconInitial() {
            return initials == null || initials.isBlank() ? "?" : initials;
        }

        public String meta() {
            return "ilvl " + itemLevel + " · " + slotText;
        }
    }

    /** The Edit tab list and its filter state. */
    public record ListView(List<ListRow> rows, int total, String query, String origin, String slot,
            int fetchedCount, int baseCount, int customCount, String message, String error) {
        public boolean hasRows() {
            return !rows.isEmpty();
        }

        public boolean hasMessage() {
            return message != null && !message.isBlank();
        }

        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        public String summary() {
            List<String> parts = new ArrayList<String>();
            parts.add(rows.size() + " shown");
            if (fetchedCount > 0) {
                parts.add(fetchedCount + " fetched");
            }
            if (customCount > 0) {
                parts.add(customCount + " custom");
            }
            if (baseCount > 0) {
                parts.add(baseCount + " base");
            }
            return String.join(" · ", parts);
        }

        public ListView withMessage(String message) {
            return new ListView(rows, total, query, origin, slot, fetchedCount, baseCount,
                    customCount, message, error);
        }

        public ListView withError(String error) {
            return new ListView(rows, total, query, origin, slot, fetchedCount, baseCount,
                    customCount, message, error);
        }
    }

    /** One item's detail panel. */
    public record DetailView(int itemId, String name, String qualityKey, int itemLevel,
            String slotText, ItemTooltip tooltip, Origin origin, String meta,
            List<String> badges, boolean canRefresh, boolean canRemove, boolean inUse,
            boolean canOpenPicker, String pickerSlot, String message, String error) {
        public boolean available() {
            return itemId > 0 && tooltip != null;
        }

        public boolean hasBadges() {
            return badges != null && !badges.isEmpty();
        }

        public boolean hasMessage() {
            return message != null && !message.isBlank();
        }

        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        public String qualityClass() {
            return qualityKey == null || qualityKey.isBlank() ? "" : "q-" + qualityKey;
        }

        public String originClass() {
            return origin.cssClass();
        }

        public String originLabel() {
            return origin.label();
        }

        public boolean isBase() {
            return origin == Origin.BASE;
        }

        public boolean isFetched() {
            return origin == Origin.FETCHED;
        }

        public boolean isCustom() {
            return origin == Origin.CUSTOM;
        }

        public DetailView withMessage(String message) {
            return new DetailView(itemId, name, qualityKey, itemLevel, slotText, tooltip, origin,
                    meta, badges, canRefresh, canRemove, inUse, canOpenPicker, pickerSlot, message,
                    error);
        }

        public DetailView withError(String error) {
            return new DetailView(itemId, name, qualityKey, itemLevel, slotText, tooltip, origin,
                    meta, badges, canRefresh, canRemove, inUse, canOpenPicker, pickerSlot, message,
                    error);
        }
    }

    /** Result of removing a user-owned item. */
    public record RemoveResult(boolean removed, String name, String message) {
    }

    private record UndoEntry(Item previousUserItem) {
    }

    /** One selectable enum value in the custom-item form. */
    public record Option(String value, String label) {
    }

    /** One selectable stat with its display label and percentage flag. */
    public record StatOption(String name, String label, boolean percentage) {
    }

    /** Stats grouped by their metadata category. */
    public record StatGroup(String label, List<StatOption> options) {
    }

    /** One editable stat row: the metadata name and the display value (percent for % stats). */
    public record StatRow(String name, String value, boolean percentage) {
    }

    /** One required-class checkbox. */
    public record ClassOption(String name, String label, boolean checked) {
    }

    /** One editable special effect in the custom-item form. */
    public record EffectForm(int index, String trigger, String duration, String cooldown,
            String chance, String chanceMode, String stacks, List<StatRow> stats) {
    }

    /** The custom-copy editor's view model. */
    public record CustomItemView(
            int itemId, int sourceId, int editId, boolean isNew, String sourceName,
            String name, String itemLevel, String quality, String slot, String type,
            String faction, String bind, String setName, String iconPath, boolean unique,
            String minDamage, String maxDamage, String speed, String damageType,
            String socket1, String socket2, String socket3,
            List<StatRow> stats, List<StatRow> socketBonus,
            List<ClassOption> classOptions, List<EffectForm> effects,
            ItemTooltip preview, String advancedId, boolean advancedEditable, String error,
            Map<String, String> fieldErrors,
            List<Option> qualityOptions, List<Option> slotOptions, List<Option> typeOptions,
            List<Option> factionOptions, List<Option> bindOptions,
            List<Option> damageTypeOptions, List<Option> socketOptions,
            List<StatGroup> statGroups, List<Option> triggerOptions) {
        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        public boolean hasPreview() {
            return preview != null;
        }

        public boolean hasFieldError(String field) {
            return fieldErrors != null && fieldErrors.containsKey(field);
        }

        public String fieldError(String field) {
            return fieldErrors == null ? null : fieldErrors.get(field);
        }

        public String firstErrorField() {
            return fieldErrors == null || fieldErrors.isEmpty() ? null
                    : fieldErrors.keySet().iterator().next();
        }
    }

    /** The live preview response: a tooltip, or a validation message. */
    public record CustomPreview(ItemTooltip tooltip, String error) {
    }

    /** The save response: the new id on success, or the form with its error. */
    public record CustomSaveResult(int itemId, CustomItemView form, String error) {
    }

    private record CustomInput(
            int sourceId, int editId, int itemId, String idOverride,
            String name, String itemLevel, String quality, String slot, String type,
            String faction, String bind, String setName, String iconPath, String unique,
            String minDamage, String maxDamage, String speed, String damageType,
            String socket1, String socket2, String socket3,
            List<String> statNames, List<String> statValues,
            List<String> bonusNames, List<String> bonusValues,
            List<String> requiredClasses, List<EffectInput> effects) {
    }

    private record EffectInput(int index, String trigger, String duration, String cooldown,
            String chance, String chanceMode, String stacks,
            List<String> statNames, List<String> statValues) {
    }

    private record BuildResult(Item item, Map<String, String> errors) {
        String summary() {
            return errors.isEmpty() ? null : String.join(" ", errors.values());
        }
    }

    private static final List<Option> QUALITY_OPTIONS = enumOptions(ItemQuality.values());
    private static final List<Option> SLOT_OPTIONS = enumOptions(ItemSlot.values());
    private static final List<Option> TYPE_OPTIONS = enumOptions(ItemType.values());
    private static final List<Option> FACTION_OPTIONS = enumOptions(ItemFaction.values());
    private static final List<Option> BIND_OPTIONS = enumOptions(BindsOn.values());
    private static final List<Option> DAMAGE_TYPE_OPTIONS = enumOptions(ItemDamageType.values());
    private static final List<Option> SOCKET_OPTIONS = List.of(
            new Option("None", "None"),
            new Option("Meta", "Meta"),
            new Option("Red", "Red"),
            new Option("Orange", "Orange"),
            new Option("Yellow", "Yellow"),
            new Option("Green", "Green"),
            new Option("Blue", "Blue"),
            new Option("Purple", "Purple"),
            new Option("Prismatic", "Prismatic"));
    private static final List<StatGroup> STAT_GROUPS = buildStatGroups();
    private static final List<Option> TRIGGER_OPTIONS = enumOptions(Trigger.values());

    public QueueView parse(String input, boolean skipCached, Character character) {
        ItemIdParser.Parsed parsed = ItemIdParser.parse(input);
        List<AddRow> rows = new ArrayList<AddRow>();
        for (Integer id : parsed.ids()) {
            rows.add(queueRow(id, skipCached, character));
        }
        for (String token : parsed.invalidTokens()) {
            rows.add(new AddRow(0, token, AddStatus.INVALID, false, false, null, null, 0, null,
                    null, null, null, null, List.of(), "Not an item ID, item link or Wowhead URL.",
                    false, false, false, null));
        }
        String message = parsed.ids().isEmpty() && parsed.invalidTokens().isEmpty()
                ? "Paste at least one item ID, item link or URL." : null;
        return new QueueView(List.copyOf(rows), rows.size(), parsed.truncated(),
                itemSources.sourceInfo().host(), message);
    }

    private AddRow queueRow(int id, boolean skipCached, Character character) {
        Item cached = ItemCache.findItemById(id);
        if (cached == null) {
            return new AddRow(id, Integer.toString(id), AddStatus.QUEUED, true, false, null, null,
                    0, null, "Item " + id, null, null, null, List.of(),
                    "Waiting for the item source.", false, false, false, null);
        }
        boolean userItem = userItems.contains(id);
        if (!skipCached && userItem) {
            AddRow row = cachedRow(cached, character);
            return new AddRow(row.itemId(), row.token(), AddStatus.QUEUED, true, true, row.name(),
                    row.qualityKey(), row.itemLevel(), row.slotText(), row.meta(), row.iconUrl(),
                    row.initials(), row.tooltip(), row.badges(),
                    "Refreshing this fetched item from the item source.", false, false,
                    row.canOpenPicker(), row.pickerSlot());
        }
        return cachedRow(cached, character);
    }

    private AddRow cachedRow(Item item, Character character) {
        ItemTooltip tooltip = tooltips.forItem(item);
        Origin origin = origin(item);
        List<String> badges = new ArrayList<String>();
        badges.add(origin.label());
        badges.addAll(fitBadges(item, character));
        return new AddRow(item.getId(), Integer.toString(item.getId()), AddStatus.CACHED, false,
                false, item.getName(), qualityKey(item), item.getItemLevel(),
                tooltip == null ? null : tooltip.slotText(), meta(item, tooltip), iconUrl(item),
                icons.initials(item.getName()), tooltip, List.copyOf(badges),
                "This item is already in the local cache. Nothing was fetched.", false,
                origin == Origin.FETCHED, canOpenPicker(item), pickerSlot(item));
    }

    public AddRow fetch(int itemId, boolean refresh, HttpSession session, Character character) {
        if (itemId <= 0) {
            return errorRow(itemId, "Enter a positive numeric item ID.");
        }
        Item cached = ItemCache.findItemById(itemId);
        boolean userItem = userItems.contains(itemId);
        if (cached != null && !refresh) {
            return cachedRow(cached, character);
        }
        if (cached != null && !userItem) {
            return cachedRow(cached, character);
        }
        Item previousUserItem = userItem ? cached : null;
        Item item;
        try {
            item = itemSources.fetchItem(itemId);
        } catch (IOException | ItemXmlParseException | RuntimeException e) {
            return errorRow(itemId, friendlyError(itemId, e));
        }
        if (item == null) {
            return new AddRow(itemId, Integer.toString(itemId), AddStatus.NOT_FOUND, false, false,
                    null, null, 0, null, "Item " + itemId, null, null, null, List.of(),
                    "The configured item source did not return an item for this ID.",
                    false, false, false, null);
        }
        ItemCache.addItem(item);
        userItems.add(item);
        rememberUndo(session, itemId, previousUserItem);
        if (character != null) {
            character.onCalculationsInvalidated();
        }
        ItemTooltip tooltip = tooltips.forItem(item);
        AddStatus status = previousUserItem != null ? AddStatus.REFRESHED : AddStatus.ADDED;
        return new AddRow(item.getId(), Integer.toString(itemId), status, false, false,
                item.getName(), qualityKey(item), item.getItemLevel(),
                tooltip == null ? null : tooltip.slotText(), meta(item, tooltip), iconUrl(item),
                icons.initials(item.getName()), tooltip, fitBadges(item, character),
                previousUserItem != null ? "Updated from the item source." : null,
                true, false, canOpenPicker(item), pickerSlot(item));
    }

    public AddRow undo(int itemId, HttpSession session, Character character) {
        UndoEntry entry = takeUndo(session, itemId);
        if (entry == null) {
            return errorRow(itemId, "There is nothing to undo for this item.");
        }
        if (isInUse(character, itemId)) {
            rememberUndo(session, itemId, entry.previousUserItem());
            return errorRow(itemId, "Equip or unsocket the item before undoing the change.");
        }
        if (entry.previousUserItem() != null) {
            Item previous = entry.previousUserItem();
            previous.setInvalid(false);
            userItems.add(previous);
            ItemCache.addItem(previous);
            if (character != null) {
                character.onCalculationsInvalidated();
            }
            AddRow restored = cachedRow(previous, character);
            return new AddRow(restored.itemId(), restored.token(), AddStatus.CACHED, false, false,
                    restored.name(), restored.qualityKey(), restored.itemLevel(),
                    restored.slotText(), restored.meta(), restored.iconUrl(), restored.initials(),
                    restored.tooltip(), restored.badges(),
                    "Restored the previous local copy.", false, true,
                    restored.canOpenPicker(), restored.pickerSlot());
        }
        Item removed = ItemCache.findItemById(itemId);
        userItems.remove(itemId);
        if (removed != null) {
            ItemCache.deleteItem(removed);
        }
        if (character != null) {
            character.onCalculationsInvalidated();
        }
        String name = removed == null ? "Item " + itemId : removed.getName();
        return new AddRow(itemId, Integer.toString(itemId), AddStatus.UNDONE, false, false, name,
                removed == null ? null : qualityKey(removed), removed == null ? 0 : removed.getItemLevel(),
                null, removed == null ? "Item " + itemId : meta(removed, tooltips.forItem(removed)),
                removed == null ? null : iconUrl(removed),
                removed == null ? null : icons.initials(removed.getName()), null, List.of(),
                "Removed from the local cache. Fetch it again to restore it.", false, false, false,
                null);
    }

    public ListView list(String query, String originFilter, String slotFilter) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String originKey = originFilter == null || originFilter.isBlank()
                ? "all" : originFilter.trim().toLowerCase(Locale.ROOT);
        String slotKey = slotFilter == null || slotFilter.isBlank()
                ? "all" : slotFilter.trim().toLowerCase(Locale.ROOT);
        List<Item> matches = new ArrayList<Item>();
        for (Item item : ItemCache.allItems()) {
            if (item.getName() == null || !matchesQuery(item, needle) || !matchesOrigin(item, originKey)
                    || !matchesSlot(item, slotKey)) {
                continue;
            }
            matches.add(item);
        }
        matches.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Item::getId));
        int fetched = 0;
        int base = 0;
        int custom = 0;
        for (Item item : matches) {
            Origin origin = origin(item);
            switch (origin) {
                case FETCHED -> fetched++;
                case CUSTOM -> custom++;
                default -> base++;
            }
        }
        List<ListRow> rows = new ArrayList<ListRow>();
        for (Item item : matches) {
            if (rows.size() >= MAX_LIST_ROWS) {
                break;
            }
            rows.add(listRow(item));
        }
        return new ListView(List.copyOf(rows), matches.size(), query == null ? "" : query,
                originKey, slotKey, fetched, base, custom, null, null);
    }

    private static boolean matchesQuery(Item item, String needle) {
        return needle.isEmpty() || item.getName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean matchesOrigin(Item item, String originKey) {
        Origin origin = origin(item);
        return switch (originKey) {
            case "base" -> origin == Origin.BASE;
            case "fetched" -> origin == Origin.FETCHED;
            case "custom" -> origin == Origin.CUSTOM;
            default -> true;
        };
    }

    private static boolean matchesSlot(Item item, String slotKey) {
        return "all".equals(slotKey) || (item.getSlot() != null
                && item.getSlot().name().toLowerCase(Locale.ROOT).equals(slotKey));
    }

    private ListRow listRow(Item item) {
        return new ListRow(item.getId(), item.getName(), qualityKey(item), item.getItemLevel(),
                slotLabel(item.getSlot()), item.getSlot() == null ? "" : item.getSlot().name(),
                iconUrl(item), icons.initials(item.getName()), origin(item));
    }

    public DetailView detail(int itemId, Character character) {
        Item item = ItemCache.findItemById(itemId);
        if (item == null) {
            return new DetailView(0, null, null, 0, null, null, Origin.BASE, null, List.of(),
                    false, false, false, false, null, null,
                    "Item " + itemId + " is not in the local cache.");
        }
        ItemTooltip tooltip = tooltips.forItem(item);
        Origin origin = origin(item);
        boolean userItem = origin != Origin.BASE;
        boolean canRefresh = origin == Origin.FETCHED;
        boolean inUse = isInUse(character, itemId);
        boolean canRemove = userItem && !inUse;
        List<String> badges = fitBadges(item, character);
        return new DetailView(item.getId(), item.getName(), qualityKey(item), item.getItemLevel(),
                tooltip == null ? null : tooltip.slotText(), tooltip, origin,
                meta(item, tooltip), badges, canRefresh, canRemove, inUse, canOpenPicker(item),
                pickerSlot(item), null, null);
    }

    public DetailView refresh(int itemId, HttpSession session, Character character) {
        AddRow result = fetch(itemId, true, session, character);
        DetailView detail = detail(itemId, character);
        if (result.status() == AddStatus.REFRESHED || result.status() == AddStatus.ADDED) {
            return new DetailView(detail.itemId(), detail.name(), detail.qualityKey(),
                    detail.itemLevel(), detail.slotText(), detail.tooltip(), detail.origin(),
                    detail.meta(), detail.badges(), detail.canRefresh(), detail.canRemove(),
                    detail.inUse(), detail.canOpenPicker(), detail.pickerSlot(),
                    "Refreshed from " + sourceHost() + ".", null);
        }
        return new DetailView(detail.itemId(), detail.name(), detail.qualityKey(),
                detail.itemLevel(), detail.slotText(), detail.tooltip(), detail.origin(),
                detail.meta(), detail.badges(), detail.canRefresh(), detail.canRemove(),
                detail.inUse(), detail.canOpenPicker(), detail.pickerSlot(), null,
                result.message() == null ? "The item could not be refreshed." : result.message());
    }

    public RemoveResult remove(int itemId, HttpSession session, Character character) {
        Item item = ItemCache.findItemById(itemId);
        if (item == null || !userItems.contains(itemId)) {
            return new RemoveResult(false, null, "Only items added by you can be removed.");
        }
        if (isInUse(character, itemId)) {
            return new RemoveResult(false, item.getName(),
                    "Equip or unsocket the item before removing it.");
        }
        userItems.remove(itemId);
        ItemCache.deleteItem(item);
        clearUndo(session, itemId);
        if (character != null) {
            character.onCalculationsInvalidated();
        }
        return new RemoveResult(true, item.getName(),
                "Removed " + item.getName() + " from the local cache.");
    }

    // ------------------------------------------------------------- custom item editor

    /** The form for a new copy of {@code sourceId}, or for editing the custom item {@code editId}. */
    public CustomItemView customForm(Integer sourceId, Integer editId) {
        if (editId != null && editId > 0) {
            Item item = ItemCache.findItemById(editId);
            if (item == null) {
                return emptyCustomView(editId, 0, false,
                        "Item " + editId + " is not in the local cache.");
            }
            if (origin(item) != Origin.CUSTOM) {
                return emptyCustomView(editId, 0, false, "Only custom copies can be edited.");
            }
            return viewFromItem(item, false, item.getId(), item.getId(), item.getId(), null);
        }
        if (sourceId == null || sourceId <= 0) {
            return emptyCustomView(0, 0, true, "Choose an item to copy.");
        }
        Item source = ItemCache.findItemById(sourceId);
        if (source == null) {
            return emptyCustomView(0, sourceId, true,
                    "Item " + sourceId + " is not in the local cache.");
        }
        return viewFromItem(source, true, nextCustomId(), source.getId(), 0, null);
    }

    /** Builds the live preview without caching anything. */
    public CustomPreview previewCustom(MultiValueMap<String, String> params) {
        CustomInput input = parseInput(params);
        Item edit = input.editId > 0 ? ItemCache.findItemById(input.editId) : null;
        Item source = input.sourceId > 0 ? ItemCache.findItemById(input.sourceId) : null;
        BuildResult result = buildItem(input, source, edit, false);
        if (!result.errors().isEmpty()) {
            return new CustomPreview(null, result.summary());
        }
        return new CustomPreview(tooltips.forItem(result.item()), null);
    }

    /** Validates and saves a custom copy (new or edited), returning the new id or the form error. */
    public CustomSaveResult saveCustom(MultiValueMap<String, String> params, Character character) {
        CustomInput input = parseInput(params);
        Item edit = input.editId > 0 ? ItemCache.findItemById(input.editId) : null;
        Item source = input.sourceId > 0 ? ItemCache.findItemById(input.sourceId) : null;
        if (input.editId > 0 && edit == null) {
            String error = "The custom item is no longer in the local cache.";
            return new CustomSaveResult(0,
                    viewFromInput(input, source, null, Map.of("source", error)), error);
        }
        BuildResult result = buildItem(input, source, edit, true);
        if (!result.errors().isEmpty()) {
            return new CustomSaveResult(0,
                    viewFromInput(input, source, edit, result.errors()), result.summary());
        }
        Item item = result.item();
        ItemCache.addItem(item);
        userItems.add(item);
        if (character != null) {
            character.onCalculationsInvalidated();
        }
        return new CustomSaveResult(item.getId(), null, null);
    }

    private CustomItemView viewFromItem(Item item, boolean isNew, int itemId, int sourceId,
            int editId, String error) {
        ItemTooltip preview = tooltips.forItem(item);
        String name = isNew && item.getName() != null ? item.getName() + " (custom)"
                : item.getName();
        return new CustomItemView(itemId, sourceId, editId, isNew, item.getName(),
                name == null ? "" : name, Integer.toString(item.getItemLevel()),
                enumName(item.getQuality()), enumName(item.getSlot()), enumName(item.getType()),
                enumName(item.getFaction()), enumName(item.getBind()),
                item.getSetName() == null ? "" : item.getSetName(),
                item.getIconPath() == null ? "" : item.getIconPath(), item.isUnique(),
                Integer.toString(item.getMinDamage()), Integer.toString(item.getMaxDamage()),
                formatStatValue(item.getSpeed(), false), enumName(item.getDamageType()),
                socketName(item.getSocketColor1()), socketName(item.getSocketColor2()),
                socketName(item.getSocketColor3()), statRows(item.getStats()),
                statRows(item.getSocketBonus()), classOptions(item.getRequiredClasses()),
                effectForms(item.getStats()), preview, "", isNew, error, Map.of(),
                QUALITY_OPTIONS, SLOT_OPTIONS, TYPE_OPTIONS, FACTION_OPTIONS, BIND_OPTIONS,
                DAMAGE_TYPE_OPTIONS, SOCKET_OPTIONS, STAT_GROUPS, TRIGGER_OPTIONS);
    }

    private CustomItemView viewFromInput(CustomInput input, Item source, Item edit,
            Map<String, String> fieldErrors) {
        Item display = edit != null ? edit : source;
        BuildResult draft = buildItem(input, source, edit, false);
        ItemTooltip preview = draft.errors().isEmpty() ? tooltips.forItem(draft.item())
                : (display == null ? null : tooltips.forItem(display));
        String error = fieldErrors.isEmpty() ? null : String.join(" ", fieldErrors.values());
        String sourceName = display == null ? "Item" : display.getName();
        int itemId = edit != null ? edit.getId()
                : (input.itemId > 0 ? input.itemId : nextCustomId());
        return new CustomItemView(itemId, input.sourceId, input.editId, edit == null,
                sourceName, valueOr(input.name, ""),
                valueOr(input.itemLevel, display == null ? "0"
                        : Integer.toString(display.getItemLevel())),
                valueOr(input.quality, display == null ? "" : enumName(display.getQuality())),
                valueOr(input.slot, display == null ? "" : enumName(display.getSlot())),
                valueOr(input.type, display == null ? "" : enumName(display.getType())),
                valueOr(input.faction, display == null ? "" : enumName(display.getFaction())),
                valueOr(input.bind, display == null ? "" : enumName(display.getBind())),
                valueOr(input.setName, display == null || display.getSetName() == null
                        ? "" : display.getSetName()),
                valueOr(input.iconPath, display == null || display.getIconPath() == null
                        ? "" : display.getIconPath()),
                "true".equalsIgnoreCase(trim(input.unique)),
                valueOr(input.minDamage, display == null ? "0"
                        : Integer.toString(display.getMinDamage())),
                valueOr(input.maxDamage, display == null ? "0"
                        : Integer.toString(display.getMaxDamage())),
                valueOr(input.speed, display == null ? "0"
                        : formatStatValue(display.getSpeed(), false)),
                valueOr(input.damageType, display == null ? "" : enumName(display.getDamageType())),
                socketValue(input.socket1, display == null ? null : display.getSocketColor1()),
                socketValue(input.socket2, display == null ? null : display.getSocketColor2()),
                socketValue(input.socket3, display == null ? null : display.getSocketColor3()),
                statRows(input.statNames, input.statValues),
                statRows(input.bonusNames, input.bonusValues),
                classOptions(input.requiredClasses),
                effectForms(input.effects), preview,
                input.idOverride == null ? "" : input.idOverride, edit == null, error,
                fieldErrors, QUALITY_OPTIONS, SLOT_OPTIONS, TYPE_OPTIONS, FACTION_OPTIONS,
                BIND_OPTIONS, DAMAGE_TYPE_OPTIONS, SOCKET_OPTIONS, STAT_GROUPS, TRIGGER_OPTIONS);
    }

    private CustomItemView emptyCustomView(int editId, int sourceId, boolean isNew, String error) {
        return new CustomItemView(0, sourceId, editId, isNew, "Item", "", "0", "Epic", "Head",
                "None", "Neutral", "None", "", "", false, "0", "0", "0", "Physical",
                socketName(null), socketName(null), socketName(null), List.of(), List.of(),
                classOptions(Set.of()), List.of(), null, "", isNew, error, Map.of(),
                QUALITY_OPTIONS, SLOT_OPTIONS, TYPE_OPTIONS, FACTION_OPTIONS, BIND_OPTIONS,
                DAMAGE_TYPE_OPTIONS, SOCKET_OPTIONS, STAT_GROUPS, TRIGGER_OPTIONS);
    }

    private BuildResult buildItem(CustomInput input, Item source, Item edit,
            boolean requireFreeId) {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        Item display = edit != null ? edit : source;
        if (display == null) {
            return new BuildResult(null, Map.of("source", "Choose an item to copy."));
        }
        if (edit != null && origin(edit) != Origin.CUSTOM) {
            return new BuildResult(null, Map.of("source", "Only custom copies can be edited."));
        }
        int id;
        if (edit != null) {
            id = edit.getId();
        } else {
            id = input.itemId > 0 ? input.itemId : nextCustomId();
            String override = trim(input.idOverride);
            if (!override.isEmpty()) {
                try {
                    id = Integer.parseInt(override);
                } catch (NumberFormatException e) {
                    addError(errors, "idOverride", "The custom item ID must be a number.");
                }
            }
            if (id < CUSTOM_ITEM_ID_MIN) {
                addError(errors, "idOverride", "Custom items use IDs 1,000,000 or higher.");
            } else if (requireFreeId && !isFreeCustomId(id)) {
                addError(errors, "idOverride", "Item ID " + id + " is already in use.");
            }
        }

        String name = trim(input.name);
        if (name.isEmpty()) {
            addError(errors, "name", "Enter an item name.");
        }
        Integer itemLevel = parseInt(input.itemLevel, 0, 1_000_000);
        if (itemLevel == null) {
            addError(errors, "itemLevel", "Enter a whole number for the item level.");
        }
        Integer minDamage = parseInt(input.minDamage, 0, 1_000_000);
        if (minDamage == null) {
            addError(errors, "minDamage", "Enter a whole number for the minimum damage.");
        }
        Integer maxDamage = parseInt(input.maxDamage, 0, 1_000_000);
        if (maxDamage == null) {
            addError(errors, "maxDamage", "Enter a whole number for the maximum damage.");
        }
        if (minDamage != null && maxDamage != null && minDamage > maxDamage) {
            addError(errors, "maxDamage", "Maximum damage must be at least the minimum.");
        }
        Float speed = parseFloat(input.speed);
        if (speed == null || speed < 0f || !Float.isFinite(speed)) {
            addError(errors, "speed", "Enter a non-negative speed.");
        }

        ItemQuality quality = parseEnum(ItemQuality.class, input.quality, "quality", errors,
                "quality");
        ItemSlot slot = parseEnum(ItemSlot.class, input.slot, "slot", errors, "slot");
        ItemType type = parseEnum(ItemType.class, input.type, "type", errors, "type");
        ItemFaction faction = parseEnum(ItemFaction.class, input.faction, "faction", errors,
                "faction");
        BindsOn bind = parseEnum(BindsOn.class, input.bind, "bind", errors, "bind");
        ItemDamageType damageType = parseEnum(ItemDamageType.class, input.damageType,
                "damage type", errors, "damageType");
        ItemSlot socket1 = parseSocket(input.socket1, "socket1", errors);
        ItemSlot socket2 = parseSocket(input.socket2, "socket2", errors);
        ItemSlot socket3 = parseSocket(input.socket3, "socket3", errors);

        Stats stats = new Stats();
        Stats socketBonus = new Stats();
        fillStats(stats, input.statNames, input.statValues, "stat", "item", errors);
        fillStats(socketBonus, input.bonusNames, input.bonusValues, "bonus", "socket bonus",
                errors);
        buildEffects(input.effects, stats, errors);
        String requiredClasses = requiredClasses(input.requiredClasses, errors);
        String setName = trim(input.setName);
        String iconPath = trim(input.iconPath);
        if (!iconPath.isEmpty() && !isValidIconPath(iconPath)) {
            addError(errors, "iconPath", "Use an icon path without spaces.");
        }
        if (!errors.isEmpty()) {
            return new BuildResult(null, errors);
        }

        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setItemLevel(itemLevel);
        item.setQuality(quality);
        item.setSlot(slot);
        item.setType(type);
        item.setFaction(faction);
        item.setBind(bind);
        item.setUnique("true".equalsIgnoreCase(trim(input.unique)));
        item.setSetName(setName);
        item.setIconPath(iconPath.isEmpty() && display.getIconPath() != null
                ? display.getIconPath() : iconPath);
        item.setMinDamage(minDamage);
        item.setMaxDamage(maxDamage);
        item.setSpeed(speed);
        item.setDamageType(damageType);
        item.setStats(stats);
        item.setSocketBonus(socketBonus);
        item.setSocketColor1(socket1);
        item.setSocketColor2(socket2);
        item.setSocketColor3(socket3);
        item.setRequiredClasses(requiredClasses);
        item.setDisplayId(display.getDisplayId());
        item.setDisplaySlot(display.getDisplaySlot());
        return new BuildResult(item, Map.of());
    }

    private static void addError(Map<String, String> errors, String field, String message) {
        errors.putIfAbsent(field, message);
    }

    private static CustomInput parseInput(MultiValueMap<String, String> params) {
        return new CustomInput(
                intParam(params, "sourceId"), intParam(params, "editId"), intParam(params, "itemId"),
                first(params, "idOverride"), first(params, "name"), first(params, "itemLevel"),
                first(params, "quality"), first(params, "slot"), first(params, "type"),
                first(params, "faction"), first(params, "bind"), first(params, "setName"),
                first(params, "iconPath"), first(params, "unique"), first(params, "minDamage"),
                first(params, "maxDamage"), first(params, "speed"), first(params, "damageType"),
                first(params, "socket1"), first(params, "socket2"), first(params, "socket3"),
                values(params, "statName"), values(params, "statValue"),
                values(params, "bonusName"), values(params, "bonusValue"),
                values(params, "requiredClass"), parseEffects(params));
    }

    private static String first(MultiValueMap<String, String> params, String key) {
        return params.getFirst(key);
    }

    private static List<String> values(MultiValueMap<String, String> params, String key) {
        List<String> values = params.get(key);
        return values == null ? List.of() : List.copyOf(values);
    }

    private static int intParam(MultiValueMap<String, String> params, String key) {
        String value = first(params, key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void fillStats(Stats stats, List<String> names, List<String> values,
            String prefix, String kind, Map<String, String> errors) {
        Set<String> seen = new HashSet<String>();
        int count = Math.min(names.size(), values.size());
        for (int i = 0; i < count; i++) {
            String name = trim(names.get(i));
            String valueText = trim(values.get(i));
            String field = prefix + "." + i;
            if (name.isEmpty() && valueText.isEmpty()) {
                continue;
            }
            if (name.isEmpty()) {
                addError(errors, field, "Choose a stat.");
                continue;
            }
            if (valueText.isEmpty()) {
                addError(errors, field, "Enter a value.");
                continue;
            }
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
            if (property == null) {
                addError(errors, field, "Unknown " + kind + " stat: " + name + ".");
                continue;
            }
            if (!seen.add(name)) {
                addError(errors, field, "Duplicate " + kind + " stat: " + name + ".");
                continue;
            }
            float value;
            try {
                value = Float.parseFloat(valueText);
            } catch (NumberFormatException e) {
                addError(errors, field, "Invalid value for " + name + ".");
                continue;
            }
            if (!Float.isFinite(value)) {
                addError(errors, field, "Invalid value for " + name + ".");
                continue;
            }
            property.mutator().set(stats, property.percentage() ? value / 100f : value);
        }
    }

    private static String requiredClasses(List<String> values, Map<String, String> errors) {
        Set<String> required = new LinkedHashSet<String>();
        for (String value : values) {
            String name = trim(value);
            if (name.isEmpty()) {
                continue;
            }
            try {
                required.add(CharacterClass.valueOf(name).name());
            } catch (IllegalArgumentException e) {
                addError(errors, "requiredClasses", "Unknown required class: " + name + ".");
            }
        }
        return String.join("|", required);
    }

    private static List<EffectInput> parseEffects(MultiValueMap<String, String> params) {
        Set<Integer> indices = new java.util.TreeSet<Integer>();
        for (String key : params.keySet()) {
            if (key == null || !key.startsWith("effect") || !key.endsWith("Trigger")) {
                continue;
            }
            String middle = key.substring("effect".length(), key.length() - "Trigger".length());
            try {
                indices.add(Integer.valueOf(middle));
            } catch (NumberFormatException e) {
                // Ignore malformed effect keys.
            }
        }
        List<EffectInput> effects = new ArrayList<EffectInput>();
        for (Integer index : indices) {
            String prefix = "effect" + index;
            effects.add(new EffectInput(index, first(params, prefix + "Trigger"),
                    first(params, prefix + "Duration"), first(params, prefix + "Cooldown"),
                    first(params, prefix + "Chance"), first(params, prefix + "Mode"),
                    first(params, prefix + "Stacks"), values(params, prefix + "StatName"),
                    values(params, prefix + "StatValue")));
        }
        return List.copyOf(effects);
    }

    private static List<EffectForm> effectForms(List<EffectInput> inputs) {
        List<EffectForm> forms = new ArrayList<EffectForm>();
        int index = 0;
        for (EffectInput input : inputs) {
            forms.add(new EffectForm(index++, input.trigger(), input.duration(), input.cooldown(),
                    input.chance(), input.chanceMode(), input.stacks(),
                    statRows(input.statNames(), input.statValues())));
        }
        return List.copyOf(forms);
    }

    private static List<EffectForm> effectForms(Stats stats) {
        if (stats == null) {
            return List.of();
        }
        List<EffectForm> forms = new ArrayList<EffectForm>();
        int index = 0;
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect == null) {
                continue;
            }
            boolean ppm = effect.getChance() < 0;
            String chance = ppm ? formatStatValue(-effect.getChance(), false)
                    : formatStatValue(effect.getChance(), true);
            forms.add(new EffectForm(index++, enumName(effect.getTrigger()),
                    formatEffectTime(effect.getDuration()), formatEffectTime(effect.getCooldown()),
                    chance, ppm ? "ppm" : "percent", Integer.toString(effect.getMaxStack()),
                    statRows(effect.getStats())));
        }
        return List.copyOf(forms);
    }

    private static void buildEffects(List<EffectInput> inputs, Stats target,
            Map<String, String> errors) {
        int position = 0;
        for (EffectInput input : inputs) {
            String field = "effect." + position;
            Trigger trigger = parseEnum(Trigger.class, input.trigger(), "trigger", errors, field);
            Float duration = parseEffectTime(input.duration(), errors, field);
            Float cooldown = parseEffectTime(input.cooldown(), errors, field);
            Float chance = parseFloat(input.chance());
            if (chance == null || chance < 0f || !Float.isFinite(chance)) {
                addError(errors, field, "Enter a non-negative chance.");
            }
            int stacks = parseStacks(input.stacks(), errors, field);
            Stats effectStats = new Stats();
            fillStats(effectStats, input.statNames(), input.statValues(),
                    field + ".stat", "effect", errors);
            if (trigger != null && duration != null && cooldown != null && chance != null
                    && chance >= 0f && Float.isFinite(chance) && stacks >= 1) {
                float actualChance = "ppm".equalsIgnoreCase(trim(input.chanceMode()))
                        ? -chance : chance / 100f;
                target.addSpecialEffect(new SpecialEffect(trigger, effectStats, duration, cooldown,
                        actualChance, stacks));
            }
            position++;
        }
    }

    private static int parseStacks(String value, Map<String, String> errors, String field) {
        try {
            int stacks = Integer.parseInt(trim(value));
            if (stacks < 1) {
                addError(errors, field, "Stacks must be at least 1.");
                return 0;
            }
            return stacks;
        } catch (NumberFormatException e) {
            addError(errors, field, "Enter a stack count.");
            return 0;
        }
    }

    private static Float parseEffectTime(String value, Map<String, String> errors,
            String field) {
        String text = trim(value);
        if ("-1".equals(text)) {
            return Float.POSITIVE_INFINITY;
        }
        Float parsed = parseFloat(text);
        if (parsed == null || parsed < 0f || !Float.isFinite(parsed)) {
            addError(errors, field, "Enter a non-negative time (or -1 for infinite).");
            return null;
        }
        return parsed;
    }

    private static String formatEffectTime(float value) {
        return Float.isInfinite(value) ? "-1" : formatStatValue(value, false);
    }

    private int nextCustomId() {
        int max = CUSTOM_ITEM_ID_MIN - 1;
        for (Item item : ItemCache.allItems()) {
            if (item.getId() >= CUSTOM_ITEM_ID_MIN) {
                max = Math.max(max, item.getId());
            }
        }
        for (Item item : userItems.items()) {
            if (item.getId() >= CUSTOM_ITEM_ID_MIN) {
                max = Math.max(max, item.getId());
            }
        }
        int candidate = max + 1;
        while (!isFreeCustomId(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private boolean isFreeCustomId(int id) {
        return id >= CUSTOM_ITEM_ID_MIN && ItemCache.findItemById(id) == null
                && !userItems.contains(id);
    }

    private static boolean isSocketColor(ItemSlot slot) {
        return slot == ItemSlot.None || slot == ItemSlot.Meta || slot == ItemSlot.Red
                || slot == ItemSlot.Orange || slot == ItemSlot.Yellow || slot == ItemSlot.Green
                || slot == ItemSlot.Blue || slot == ItemSlot.Purple || slot == ItemSlot.Prismatic;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label,
            Map<String, String> errors, String field) {
        if (value == null || value.isBlank()) {
            addError(errors, field, "Choose the " + label + ".");
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            addError(errors, field, "Unknown " + label + ": " + value + ".");
            return null;
        }
    }

    private static ItemSlot parseSocket(String value, String field, Map<String, String> errors) {
        ItemSlot socket = parseEnum(ItemSlot.class, value, "socket color", errors, field);
        if (socket != null && !isSocketColor(socket)) {
            addError(errors, field, "Unknown socket color: " + socket.name() + ".");
            return null;
        }
        return socket;
    }

    private static Integer parseInt(String value, int min, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed < min || parsed > max ? null : parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float parseFloat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isValidIconPath(String path) {
        return path.length() <= 200 && path.matches("[A-Za-z0-9_./-]+");
    }

    private static List<StatRow> statRows(Stats stats) {
        if (stats == null) {
            return List.of();
        }
        List<StatRow> rows = new ArrayList<StatRow>();
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            if (property.category() == null || "Deprecated".equals(property.category())) {
                continue;
            }
            float value = property.accessor().get(stats);
            if (value == 0f || !Float.isFinite(value)) {
                continue;
            }
            rows.add(new StatRow(property.name(), formatStatValue(value, property.percentage()),
                    property.percentage()));
        }
        return List.copyOf(rows);
    }

    private static List<StatRow> statRows(List<String> names, List<String> values) {
        List<StatRow> rows = new ArrayList<StatRow>();
        int count = Math.min(names.size(), values.size());
        for (int i = 0; i < count; i++) {
            String name = trim(names.get(i));
            String value = trim(values.get(i));
            if (name.isEmpty() && value.isEmpty()) {
                continue;
            }
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
            rows.add(new StatRow(name, value, property != null && property.percentage()));
        }
        return List.copyOf(rows);
    }

    private static List<ClassOption> classOptions(String requiredClasses) {
        Set<String> required = new HashSet<String>();
        if (requiredClasses != null) {
            for (String name : requiredClasses.split("\\|")) {
                if (!name.isBlank()) {
                    required.add(name.replace(" ", "").trim());
                }
            }
        }
        return classOptions(required);
    }

    private static List<ClassOption> classOptions(List<String> requiredClasses) {
        Set<String> required = new HashSet<String>();
        for (String name : requiredClasses) {
            if (name != null && !name.isBlank()) {
                required.add(name.replace(" ", "").trim());
            }
        }
        return classOptions(required);
    }

    private static List<ClassOption> classOptions(Set<String> required) {
        List<ClassOption> options = new ArrayList<ClassOption>();
        for (CharacterClass characterClass : CharacterClass.values()) {
            options.add(new ClassOption(characterClass.name(),
                    displayLabel(characterClass.name()), required.contains(characterClass.name())));
        }
        return List.copyOf(options);
    }

    private static List<StatGroup> buildStatGroups() {
        Map<String, List<StatOption>> groups = new LinkedHashMap<String, List<StatOption>>();
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            if (property.category() == null || "Deprecated".equals(property.category())) {
                continue;
            }
            String label = property.displayName() != null
                    ? property.displayName() : displayLabel(property.name());
            groups.computeIfAbsent(property.category(), key -> new ArrayList<StatOption>())
                    .add(new StatOption(property.name(), label, property.percentage()));
        }
        List<StatGroup> result = new ArrayList<StatGroup>();
        for (Map.Entry<String, List<StatOption>> entry : groups.entrySet()) {
            result.add(new StatGroup(entry.getKey(), List.copyOf(entry.getValue())));
        }
        return List.copyOf(result);
    }

    private static List<Option> enumOptions(Enum<?>[] values) {
        List<Option> options = new ArrayList<Option>();
        for (Enum<?> value : values) {
            options.add(new Option(value.name(), displayLabel(value.name())));
        }
        return List.copyOf(options);
    }

    private static String displayLabel(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static String socketName(ItemSlot socket) {
        return socket == null ? "None" : socket.name();
    }

    private static String socketValue(String input, ItemSlot fallback) {
        String value = trim(input);
        return value.isEmpty() ? socketName(fallback) : value;
    }

    private static String valueOr(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String formatStatValue(float value, boolean percentage) {
        double scaled = percentage ? value * 100.0 : value;
        double rounded = Math.rint(scaled * 100.0) / 100.0;
        String text = String.format(Locale.ROOT, "%.2f", rounded);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> fitBadges(Item item, Character character) {
        List<String> badges = new ArrayList<String>();
        if (item.isUnique()) {
            badges.add("Unique");
        }
        if (character == null) {
            return badges;
        }
        String required = item.getRequiredClasses();
        if (required != null && !required.isBlank() && character.getCharacterClass() != null) {
            String normalized = required.replace(" ", "");
            if (!normalized.contains(character.getCharacterClass().name())) {
                badges.add("Class restricted: " + required.replace("|", ", "));
            }
        }
        if (item.getFaction() != null && character.getRace() != null
                && !item.fitsFaction(character.getRace())) {
            badges.add("Faction: " + item.getFaction().name());
        }
        addRelevanceBadge(badges, item, character);
        return badges;
    }

    /**
     * Adds a model-relevance or item-filter warning when the item would not appear in the current
     * model's relevant list. Relevance is advisory: a failure to resolve the model is ignored.
     */
    private static void addRelevanceBadge(List<String> badges, Item item, Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || modelName.isBlank()) {
            return;
        }
        try {
            CalculationsBase model = character.getCurrentCalculations();
            if (model == null) {
                return;
            }
            if (!model.isItemRelevant(item)) {
                badges.add("Not relevant to " + modelName);
            } else if (!ItemFilter.isItemRelevant(model, item)) {
                badges.add("Excluded by item filters");
            }
        } catch (RuntimeException e) {
            // The badge is advisory; never fail a fetch or the detail over it.
        }
    }

    private Origin origin(Item item) {
        if (!userItems.contains(item.getId())) {
            return Origin.BASE;
        }
        return item.getId() >= CUSTOM_ITEM_ID_MIN ? Origin.CUSTOM : Origin.FETCHED;
    }

    private String meta(Item item, ItemTooltip tooltip) {
        StringBuilder meta = new StringBuilder();
        if (item.getItemLevel() > 0) {
            meta.append("ilvl ").append(item.getItemLevel()).append(" · ");
        }
        if (tooltip != null && tooltip.slotText() != null && !tooltip.slotText().isBlank()) {
            meta.append(tooltip.slotText()).append(" · ");
        }
        meta.append("Item ").append(item.getId());
        return meta.toString();
    }

    private String iconUrl(Item item) {
        return item.getIconPath() == null || item.getIconPath().isBlank()
                ? null : icons.url(item.getIconPath());
    }

    private static String qualityKey(Item item) {
        return item.getQuality() == null ? "common"
                : item.getQuality().name().toLowerCase(Locale.ROOT);
    }

    /** Display label for an item slot ("TwoHand" becomes "Two Hand"). */
    public static String slotLabel(ItemSlot slot) {
        if (slot == null) {
            return "Unknown";
        }
        return slot.name().replaceAll("([A-Z])", " $1").trim();
    }

    private static String pickerSlot(Item item) {
        CharacterSlot slot = characterSlot(item);
        return slot == null ? null : slot.name();
    }

    private static boolean canOpenPicker(Item item) {
        return characterSlot(item) != null;
    }

    private static CharacterSlot characterSlot(Item item) {
        if (item.getSlot() == null) {
            return null;
        }
        CharacterSlot slot = Character.getCharacterSlotByItemSlot(item.getSlot());
        return slot == CharacterSlot.Gems || slot == CharacterSlot.Metas || slot == CharacterSlot.None
                ? null : slot;
    }

    private static boolean isInUse(Character character, int itemId) {
        if (character == null) {
            return false;
        }
        for (ItemInstance instance : character.getItems()) {
            if (instance == null) {
                continue;
            }
            Item item = instance.getItem();
            if (item != null && item.getId() == itemId) {
                return true;
            }
            for (int index = 1; index <= 3; index++) {
                Item gem = instance.getGem(index);
                if (gem != null && gem.getId() == itemId) {
                    return true;
                }
            }
        }
        return false;
    }

    private String sourceHost() {
        String host = itemSources.sourceInfo().host();
        return host == null || host.isBlank() ? "the item source" : host;
    }

    private String friendlyError(int itemId, Exception e) {
        String source = sourceHost();
        if (e instanceof ItemXmlParseException) {
            return "The item source returned an item that could not be read. Check the item source "
                    + "template in Settings.";
        }
        String message = e.getMessage() == null ? "" : e.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("http 404")) {
            return "The item source returned HTTP 404 for item " + itemId + ".";
        }
        if (lower.contains("http ")) {
            return "The item source returned an error for item " + itemId + " (" + shortMessage(e)
                    + ").";
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return "The item source did not respond in time. Check the source in Settings.";
        }
        if (e instanceof IOException) {
            return "Could not reach " + source + ". Check the item source in Settings.";
        }
        return "Could not fetch item " + itemId + ": " + shortMessage(e);
    }

    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }

    private static AddRow errorRow(int itemId, String message) {
        return new AddRow(itemId, itemId > 0 ? Integer.toString(itemId) : "", AddStatus.ERROR,
                false, false, null, null, 0, null, itemId > 0 ? "Item " + itemId : null,
                null, null, null, List.of(), message, false, false, false, null);
    }

    @SuppressWarnings("unchecked")
    private void rememberUndo(HttpSession session, int itemId, Item previousUserItem) {
        Map<Integer, UndoEntry> undo = (Map<Integer, UndoEntry>) session.getAttribute(UNDO_SESSION);
        if (undo == null) {
            undo = new LinkedHashMap<Integer, UndoEntry>();
            session.setAttribute(UNDO_SESSION, undo);
        }
        synchronized (undo) {
            undo.remove(itemId);
            undo.put(itemId, new UndoEntry(previousUserItem));
            while (undo.size() > MAX_UNDO) {
                Integer eldest = undo.keySet().iterator().next();
                undo.remove(eldest);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static UndoEntry takeUndo(HttpSession session, int itemId) {
        Map<Integer, UndoEntry> undo = (Map<Integer, UndoEntry>) session.getAttribute(UNDO_SESSION);
        if (undo == null) {
            return null;
        }
        synchronized (undo) {
            return undo.remove(itemId);
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearUndo(HttpSession session, int itemId) {
        Map<Integer, UndoEntry> undo = (Map<Integer, UndoEntry>) session.getAttribute(UNDO_SESSION);
        if (undo != null) {
            synchronized (undo) {
                undo.remove(itemId);
            }
        }
    }
}
