// New code (not a line-by-line port): side-car store for items fetched at runtime by id
// (DECISIONS #31/#35). The item half is written with the exact legacy ItemCache.xml element
// schema so ItemCacheLoader reads it back unchanged; the source half reuses the ItemSource.xml
// item-location shape (xsi:type + the fields ItemSourceLoader reads). The file is never the
// legacy cache: only items registered through add() are written.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ContainerItem;
import rawr.base.items.sources.CraftedItem;
import rawr.base.items.sources.FactionItem;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemDamageType;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.ItemSlot;
import rawr.base.items.sources.LocationFactory;
import rawr.base.items.sources.NoSource;
import rawr.base.items.sources.PvpItem;
import rawr.base.items.sources.QuestItem;
import rawr.base.effects.SpecialEffect;
import rawr.base.items.sources.StaticDrop;
import rawr.base.stats.Stats;
import rawr.base.items.sources.UnknownItem;
import rawr.base.items.sources.VendorItem;
import rawr.base.items.sources.WorldDrop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists items added at runtime (Add-by-ID / import) so they survive restarts and old character
 * files referencing them can be reloaded. The store writes an {@code ArrayOfItem} document: one
 * legacy-schema {@code <Item>} per registered item plus an {@code <ItemSource>} entry describing the
 * item's location ({@link LocationFactory}); both halves are read back by the existing loaders
 * (corrupt files are ignored with a warning). Writes go to a temp file and are moved atomically.
 *
 * <p>Only items registered through {@link #add(Item)} are written; the legacy item cache is never
 * rewritten. {@link #load()} adds the stored items to {@link ItemCache} and restores their
 * locations, and is meant to run after the legacy caches at startup.
 */
public final class UserItemStore {
    /** Default file name; the rawr-web layer resolves it next to {@code jrawr.characters-dir}. */
    public static final String DEFAULT_FILE_NAME = "jrawr.items.xml";

    private static final System.Logger LOG = System.getLogger(UserItemStore.class.getName());

    private final Path file;
    private final Map<Integer, Item> items = new LinkedHashMap<Integer, Item>();
    private final Map<Integer, ItemLocation[]> locations = new LinkedHashMap<Integer, ItemLocation[]>();
    private boolean dirty;

    public UserItemStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    /** Resolved side-car file (exposed for diagnostics/tests). */
    public Path getFile() {
        return file;
    }

    /**
     * Loads the side-car into {@link ItemCache} and {@link LocationFactory}. A missing file is not
     * an error; a corrupt file is ignored with a warning, leaving the legacy caches untouched.
     *
     * @return the number of items loaded
     */
    public synchronized int load() {
        if (!Files.isRegularFile(file)) {
            dirty = false;
            return 0;
        }
        try {
            Document document = XmlLoader.parse(file);
            List<Item> parsedItems = ItemCacheLoader.read(document);
            applyUniqueIds(document, parsedItems);
            Map<Integer, ItemLocation[]> parsedLocations = readLocations(document);
            items.clear();
            locations.clear();
            for (Item item : parsedItems) {
                items.put(item.getId(), item);
                ItemCache.addItem(item);
            }
            for (Map.Entry<Integer, ItemLocation[]> entry : parsedLocations.entrySet()) {
                locations.put(entry.getKey(), entry.getValue());
                LocationFactory.add(Integer.toString(entry.getKey()), entry.getValue(), true);
            }
            dirty = false;
            return parsedItems.size();
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING,
                    "Ignoring corrupt item side-car " + file + ": " + e.getMessage());
            items.clear();
            locations.clear();
            dirty = false;
            return 0;
        }
    }

    /**
     * Registers one user-added item (replacing an earlier entry with the same id) and persists the
     * side-car. The caller is responsible for adding the item to {@link ItemCache}; a failed write
     * only logs a warning, the in-memory item stays usable.
     */
    public synchronized void add(Item item) {
        if (item == null) {
            return;
        }
        items.put(item.getId(), item);
        ItemLocation[] location = LocationFactory.lookup(item.getId());
        if (isRealLocation(location)) {
            locations.put(item.getId(), location);
        } else {
            locations.remove(item.getId());
        }
        dirty = true;
        save();
    }

    /** Snapshot of the items registered through {@link #add(Item)} or {@link #load()}. */
    public synchronized List<Item> items() {
        return List.copyOf(items.values());
    }

    /** True when this side-car owns an item with the given id. */
    public synchronized boolean contains(int id) {
        return items.containsKey(id);
    }

    /**
     * Removes one user item from the side-car and persists. The caller is responsible for removing
     * it from {@link ItemCache}; the side-car never touches the item registry itself.
     *
     * @return true when an entry was removed
     */
    public synchronized boolean remove(int id) {
        Item removed = items.remove(id);
        locations.remove(id);
        if (removed == null) {
            return false;
        }
        dirty = true;
        save();
        return true;
    }

    /** Rewrites the side-car when it differs from disk; no-op when nothing changed. */
    public synchronized void save() {
        if (!dirty) {
            return;
        }
        Path parent = file.getParent();
        Path temp = null;
        try {
            Path directory = parent != null ? parent : Paths.get(".");
            Files.createDirectories(directory);
            temp = Files.createTempFile(directory, "jrawr.items", ".tmp");
            Files.writeString(temp, toXml(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "Could not persist item side-car " + file + ": " + e.getMessage());
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    /** A location is real when the factory actually has one (the fallback is a plain ItemLocation). */
    private static boolean isRealLocation(ItemLocation[] location) {
        return location != null && location.length > 0 && location[0] != null
                && location[0].getClass() != ItemLocation.class;
    }

    // ------------------------------------------------------------------ reading

    /**
     * Restores the unique-id lists. The legacy schema writes one scalar {@code <UniqueId>id</UniqueId>}
     * per entry (verified against the shipped ItemCache.xml), but {@code ItemCacheLoader}'s case for
     * that element expects a nested list and yields an empty list; the loader's legacy behavior is
     * left untouched, so the side-car re-reads the scalars here.
     */
    private static void applyUniqueIds(Document document, List<Item> parsedItems) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        int index = 0;
        for (Node n = root.getFirstChild(); n != null && index < parsedItems.size(); n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element itemElement = (Element) n;
            if (!itemElement.getTagName().equals("Item")) continue;
            List<Integer> uniqueIds = new ArrayList<Integer>();
            for (Node c = itemElement.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                Element child = (Element) c;
                if (!child.getTagName().equals("UniqueId")) continue;
                String text = child.getTextContent().trim();
                if (!text.isEmpty()) {
                    uniqueIds.add(Integer.parseInt(text));
                }
            }
            parsedItems.get(index).setUniqueId(uniqueIds.isEmpty() ? null : uniqueIds);
            index++;
        }
    }

    private static Map<Integer, ItemLocation[]> readLocations(Document document) {
        Map<Integer, ItemLocation[]> result = new LinkedHashMap<Integer, ItemLocation[]>();
        Element root = document.getDocumentElement();
        if (root == null) {
            return result;
        }
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element source = (Element) n;
            if (!source.getTagName().equals("ItemSource")) continue;
            String id = XmlLoader.text(source, "Id");
            if (id == null || id.isEmpty()) continue;
            List<ItemLocation> parsed = new ArrayList<ItemLocation>();
            for (Node c = source.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                Element locationElement = (Element) c;
                if (!locationElement.getTagName().equals("ItemLocation")) continue;
                if ("true".equals(locationElement.getAttribute("xsi:nil"))) continue;
                ItemLocation location = ItemSourceLoader.parseLocation(locationElement);
                if (location != null) {
                    parsed.add(location);
                }
            }
            if (!parsed.isEmpty()) {
                result.put(Integer.parseInt(id), new ItemLocation[] {
                        parsed.get(0),
                        parsed.size() > 1 ? parsed.get(1) : null });
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ writing

    private String toXml() {
        StringBuilder xml = new StringBuilder(8192);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<ArrayOfItem xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"")
                .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        for (Item item : items.values()) {
            writeItem(xml, item);
        }
        for (Map.Entry<Integer, ItemLocation[]> entry : locations.entrySet()) {
            writeSource(xml, entry.getKey(), entry.getValue());
        }
        xml.append("</ArrayOfItem>\n");
        return xml.toString();
    }

    private static void writeItem(StringBuilder xml, Item item) {
        xml.append("  <Item>\n");
        element(xml, "    ", "ItemLevel", item.getItemLevel());
        element(xml, "    ", "DisplayId", item.getDisplayId());
        element(xml, "    ", "DisplaySlot", item.getDisplaySlot());
        if (item.getIconPath() != null) {
            element(xml, "    ", "IconPath", item.getIconPath());
        }
        writeStats(xml, "    ", "Stats", item.getStats());
        element(xml, "    ", "Quality", String.valueOf(item.getQuality()));
        if (item.getSetName() != null) {
            element(xml, "    ", "SetName", item.getSetName());
        }
        element(xml, "    ", "Type", String.valueOf(item.getType()));
        element(xml, "    ", "Faction", String.valueOf(item.getFaction()));
        if (item.getMinDamage() != 0) {
            element(xml, "    ", "MinDamage", item.getMinDamage());
        }
        if (item.getMaxDamage() != 0) {
            element(xml, "    ", "MaxDamage", item.getMaxDamage());
        }
        if (item.getDamageType() != ItemDamageType.Physical) {
            element(xml, "    ", "DamageType", String.valueOf(item.getDamageType()));
        }
        if (item.getSpeed() != 0f) {
            element(xml, "    ", "Speed", netFloat(item.getSpeed()));
        }
        if (item.getRequiredClasses() != null) {
            element(xml, "    ", "RequiredClasses", item.getRequiredClasses());
        }
        if (item.isUnique()) {
            element(xml, "    ", "Unique", "true");
        }
        if (item.getBind() != BindsOn.None) {
            element(xml, "    ", "Bind", String.valueOf(item.getBind()));
        }
        if (item.getSocketColor1() != null && item.getSocketColor1() != ItemSlot.None) {
            element(xml, "    ", "SocketColor1", String.valueOf(item.getSocketColor1()));
        }
        if (item.getSocketColor2() != null && item.getSocketColor2() != ItemSlot.None) {
            element(xml, "    ", "SocketColor2", String.valueOf(item.getSocketColor2()));
        }
        if (item.getSocketColor3() != null && item.getSocketColor3() != ItemSlot.None) {
            element(xml, "    ", "SocketColor3", String.valueOf(item.getSocketColor3()));
        }
        writeStats(xml, "    ", "SocketBonus", item.getSocketBonus());
        if (item.getUniqueId() != null) {
            for (Integer uniqueId : item.getUniqueId()) {
                element(xml, "    ", "UniqueId", uniqueId);
            }
        }
        if (item.getLocalizedName() != null) {
            element(xml, "    ", "LocalizedName", item.getLocalizedName());
        }
        element(xml, "    ", "Name", item.getName() == null ? "" : item.getName());
        element(xml, "    ", "Id", item.getId());
        element(xml, "    ", "Slot", String.valueOf(item.getSlot()));
        xml.append("  </Item>\n");
    }

    private static void writeSource(StringBuilder xml, int id, ItemLocation[] location) {
        xml.append("  <ItemSource>\n");
        element(xml, "    ", "Id", id);
        if (location != null) {
            for (ItemLocation single : location) {
                if (single != null) {
                    writeLocation(xml, "    ", single);
                }
            }
        }
        xml.append("  </ItemSource>\n");
    }

    private static void writeLocation(StringBuilder xml, String indent, ItemLocation location) {
        String type = location.getClass().getSimpleName();
        xml.append(indent).append("<ItemLocation xsi:type=\"").append(type).append("\">\n");
        String child = indent + "  ";
        if (location instanceof VendorItem vendor) {
            writeIntMap(xml, child, "TokenMap", vendor.getTokenMap());
            element(xml, child, "Cost", vendor.getCost());
            if (vendor.getVendorName() != null) {
                element(xml, child, "VendorName", vendor.getVendorName());
            }
            if (vendor.getVendorArea() != null) {
                element(xml, child, "VendorArea", vendor.getVendorArea());
            }
        } else if (location instanceof StaticDrop drop) {
            if (drop.getArea() != null) {
                element(xml, child, "Area", drop.getArea());
            }
            element(xml, child, "Heroic", String.valueOf(drop.isHeroic()));
            if (drop.getBoss() != null) {
                element(xml, child, "Boss", drop.getBoss());
            }
        } else if (location instanceof WorldDrop drop) {
            if (drop.getLocation() != null) {
                element(xml, child, "Location", drop.getLocation());
            }
            element(xml, child, "Heroic", String.valueOf(drop.isHeroic()));
        } else if (location instanceof ContainerItem container) {
            element(xml, child, "Heroic", String.valueOf(container.isHeroic()));
            if (container.getArea() != null) {
                element(xml, child, "Area", container.getArea());
            }
            if (container.getContainer() != null) {
                element(xml, child, "Container", container.getContainer());
            }
            element(xml, child, "MinLevel", container.getMinLevel());
            element(xml, child, "Party", container.getParty());
        } else if (location instanceof QuestItem quest) {
            if (quest.getArea() != null) {
                element(xml, child, "Area", quest.getArea());
            }
            if (quest.getQuest() != null) {
                element(xml, child, "Quest", quest.getQuest());
            }
            element(xml, child, "MinLevel", quest.getMinLevel());
            element(xml, child, "Party", quest.getParty());
            if (quest.getType() != null) {
                element(xml, child, "Type", quest.getType());
            }
        } else if (location instanceof CraftedItem crafted) {
            if (crafted.getSkill() != null) {
                element(xml, child, "Skill", crafted.getSkill());
            }
            element(xml, child, "Level", crafted.getLevel());
            element(xml, child, "Bind", String.valueOf(crafted.getBind()));
            if (crafted.getSpellName() != null) {
                element(xml, child, "SpellName", crafted.getSpellName());
            }
            writeIntMap(xml, child, "BopMats", crafted.getBopMats());
        } else if (location instanceof PvpItem pvp) {
            if (pvp.getPointType() != null) {
                element(xml, child, "PointType", pvp.getPointType());
            }
            element(xml, child, "Points", pvp.getPoints());
            if (pvp.getTokenType() != null) {
                element(xml, child, "TokenType", pvp.getTokenType());
            }
            element(xml, child, "TokenCount", pvp.getTokenCount());
            if (pvp.getPointTypeA() != null) {
                element(xml, child, "PointTypeA", pvp.getPointTypeA());
            }
            element(xml, child, "PointsA", pvp.getPointsA());
            element(xml, child, "RequiredArenaRating", pvp.getRequiredArenaRating());
            if (pvp.getVendorName() != null) {
                element(xml, child, "VendorName", pvp.getVendorName());
            }
            if (pvp.getVendorArea() != null) {
                element(xml, child, "VendorArea", pvp.getVendorArea());
            }
        } else if (location instanceof FactionItem faction) {
            if (faction.getFactionName() != null) {
                element(xml, child, "FactionName", faction.getFactionName());
            }
            element(xml, child, "Level", String.valueOf(faction.getLevel()));
            element(xml, child, "Cost", faction.getCost());
            writeIntMap(xml, child, "TokenMap", faction.getTokenMap());
        } else if (location instanceof NoSource || location instanceof UnknownItem) {
            // No fields.
        }
        xml.append(indent).append("</ItemLocation>\n");
    }

    private static void writeIntMap(
            StringBuilder xml, String indent, String tag, LinkedHashMap<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            xml.append(indent).append('<').append(tag).append(" />\n");
            return;
        }
        xml.append(indent).append('<').append(tag).append(">\n");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            xml.append(indent).append("  <item>\n");
            xml.append(indent).append("    <key>\n");
            element(xml, indent + "      ", "string", entry.getKey());
            xml.append(indent).append("    </key>\n");
            xml.append(indent).append("    <value>\n");
            element(xml, indent + "      ", "int", entry.getValue());
            xml.append(indent).append("    </value>\n");
            xml.append(indent).append("  </item>\n");
        }
        xml.append(indent).append("</").append(tag).append(">\n");
    }

    private static void writeStats(StringBuilder xml, String indent, String tag, Stats stats) {
        if (stats == null) {
            xml.append(indent).append('<').append(tag).append(" />\n");
            return;
        }
        StringBuilder children = new StringBuilder();
        String child = indent + "  ";
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            float value = property.accessor().get(stats);
            if (Float.floatToRawIntBits(value) != 0) {
                element(children, child, property.name(), netFloat(value));
            }
        }
        int effectCount = 0;
        StringBuilder effects = new StringBuilder();
        for (SpecialEffect effect : stats.specialEffects()) {
            writeEffect(effects, child + "  ", effect);
            effectCount++;
        }
        if (effectCount > 0) {
            children.append(child).append("<SpecialEffects>\n");
            children.append(effects);
            children.append(child).append("</SpecialEffects>\n");
            element(children, child, "SpecialEffectCount", effectCount);
        }
        if (children.length() == 0) {
            xml.append(indent).append('<').append(tag).append(" />\n");
        } else {
            xml.append(indent).append('<').append(tag).append(">\n");
            xml.append(children);
            xml.append(indent).append("</").append(tag).append(">\n");
        }
    }

    private static void writeEffect(StringBuilder xml, String indent, SpecialEffect effect) {
        xml.append(indent).append("<SpecialEffect>\n");
        String child = indent + "  ";
        if (effect.getTrigger() != null) {
            element(xml, child, "Trigger", String.valueOf(effect.getTrigger()));
        }
        if (effect.getStats() != null) {
            writeStats(xml, child, "Stats", effect.getStats());
        }
        element(xml, child, "Duration", netFloat(effect.getDuration()));
        element(xml, child, "Cooldown", netFloat(effect.getCooldown()));
        element(xml, child, "Chance", netFloat(effect.getChance()));
        element(xml, child, "MaxStack", effect.getMaxStack());
        xml.append(indent).append("</SpecialEffect>\n");
    }

    /** .NET {@code XmlConvert.ToString(float)} style: INF/-INF/NaN, otherwise shortest round-trip. */
    private static String netFloat(float value) {
        if (Float.isNaN(value)) {
            return "NaN";
        }
        if (value == Float.POSITIVE_INFINITY) {
            return "INF";
        }
        if (value == Float.NEGATIVE_INFINITY) {
            return "-INF";
        }
        return Float.toString(value);
    }

    private static void element(StringBuilder xml, String indent, String tag, String value) {
        xml.append(indent).append('<').append(tag).append('>')
                .append(escape(value))
                .append("</").append(tag).append(">\n");
    }

    private static void element(StringBuilder xml, String indent, String tag, int value) {
        xml.append(indent).append('<').append(tag).append('>')
                .append(value)
                .append("</").append(tag).append(">\n");
    }

    /**
     * XML 1.0 text escaping (same entities XmlSerializer emits). Characters the XML 1.0 grammar
     * forbids (payload control codes) are dropped so the side-car always parses back.
     */
    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '\r' -> escaped.append("&#xD;");
                default -> {
                    if (isValidXmlChar(c)) {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static boolean isValidXmlChar(char c) {
        return c == '\t' || c == '\n' || c >= 0x20;
    }
}
