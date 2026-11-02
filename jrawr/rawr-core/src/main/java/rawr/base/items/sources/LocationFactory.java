// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Create (armory/network) is omitted; Lookup/Add over the in-memory table, the
// Writer for ItemSource.xml and the Load reader are ported. The C# Construct delegate is replaced
// by java.util.function.Supplier, and Application.ExecutablePath by explicit Path arguments.
package rawr.base.items.sources;

import java.util.LinkedHashMap;
import rawr.base.character.ReputationLevel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import static rawr.base.util.XmlDom.appendElement;
import static rawr.base.util.XmlDom.firstChild;
import static rawr.base.util.XmlDom.text;

/**
 * The item-location service: maps an item id to where that item comes from, and reads/writes the
 * location table.
 *
 * <p>The live data is a process-wide, synchronized {@link ItemLocationDictionary} (item id
 * string to a two-element {@code ItemLocation[]}: the primary source and an optional second
 * source, e.g. normal vs. heroic or alliance vs. horde). {@code rawr-data}'s
 * {@code ItemSourceLoader} parses the original {@code ItemSource.xml} and calls
 * {@link #add(String, ItemLocation)} for every entry; {@link #lookup(int)} never returns
 * {@code null} &mdash; an unknown id yields the shared {@code UNKNOWN_LOCATION} placeholder whose
 * description asks the user to refresh.</p>
 *
 * <p>{@link #save(Path)} / {@link #load(Path)} reproduce the C# {@code XmlSerializer} mapping of
 * {@code ItemLocation.xml}, including the per-subclass {@code xsi:type} attribute and the
 * legacy {@code <item>/<key>/<value>} encoding of the token maps. The
 * original save/load derived the file path from the executable directory; this port takes an
 * explicit {@link Path}. A parse failure replaces the table with an empty one, matching the
 * original's "fail to defaults" behavior, and save errors are swallowed as in C#. The armory
 * {@code Create} path (downloading locations) is not ported; the port is offline (see the
 * {@code PARTIAL} note).</p>
 *
 * <p>The {@code sourceType.*} to constructor registry is a {@code TreeMap}, so enumeration is
 * deterministic; the constructors are the static {@code construct()} factories of the location
 * classes. Porting note: {@link #add(String, ItemLocation[], boolean)} ignores its
 * {@code allow2ndsource} argument exactly as the original did.</p>
 *
 * @see ItemLocation
 * @see ItemSource
 */
public final class LocationFactory {
    private static final Map<String, Supplier<ItemLocation>> LOCATION_FACTORY = new TreeMap<String, Supplier<ItemLocation>>();
    private static final ItemLocationDictionary ALL_LOCATIONS = new ItemLocationDictionary();
    private static final ItemLocation[] UNKNOWN_LOCATION = { new ItemLocation("Unknown Location, please refresh"), null };

    static {
        LOCATION_FACTORY.put("sourceType.worldDrop", WorldDrop::construct);
        LOCATION_FACTORY.put("sourceType.creatureDrop", StaticDrop::construct);
        LOCATION_FACTORY.put("sourceType.pvpReward", PvpItem::construct);
        LOCATION_FACTORY.put("sourceType.factionReward", FactionItem::construct);
        LOCATION_FACTORY.put("sourceType.vendor", VendorItem::construct);
        LOCATION_FACTORY.put("sourceType.createdByPlans", CraftedItem::construct);
        LOCATION_FACTORY.put("sourceType.createdBySpell", CraftedItem::construct);
        LOCATION_FACTORY.put("sourceType.questReward", QuestItem::construct);
        LOCATION_FACTORY.put("sourceType.gameObjectDrop", ContainerItem::construct);
        LOCATION_FACTORY.put("sourceType.none", NoSource::construct);
    }

    private LocationFactory() {
    }

    /**
     * The location entry for an item id. Returning the internal array is intentional (the
     * original did); never {@code null}, but callers must not mutate the result. An id that has
     * never been loaded yields the shared "unknown location" placeholder.
     */
    public static ItemLocation[] lookup(int id) {
        return lookup(Integer.toString(id));
    }

    public static ItemLocation[] lookup(String id) {
        ItemLocation[] item = ALL_LOCATIONS.get(id);
        if (item != null) {
            return item;
        }
        return UNKNOWN_LOCATION;
    }

    /** Empties the in-memory location table (rawr-data's ItemSourceLoader reloads it). */
    public static void clear() {
        synchronized (ALL_LOCATIONS) {
            ALL_LOCATIONS.clear();
        }
    }

    public static void add(String itemId, ItemLocation itemLocation) {
        synchronized (ALL_LOCATIONS) {
            ALL_LOCATIONS.put(itemId, new ItemLocation[] { itemLocation, null });
        }
    }

    /**
     * Registers a full two-element location array for an id, replacing any previous entry. The
     * {@code allow2ndsource} argument is unused (a faithful port of the original); either slot
     * may be {@code null} to mean "no source there".
     */
    public static void add(String itemId, ItemLocation[] itemLocation, boolean allow2ndsource) {
        synchronized (ALL_LOCATIONS) {
            ALL_LOCATIONS.put(itemId, itemLocation);
        }
    }

    // C# ItemLocation.cs:863-893 Save(string fileName)/Load(string fileName) with the executable
    // directory replaced by an explicit path. Schema is the XmlSerializer ItemLocationDictionary
    // mapping (root <dictionary>, one <item> per id, values in <ArrayOfItemLocation>).
    public static void save(Path path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("dictionary");
            doc.appendChild(root);
            synchronized (ALL_LOCATIONS) {
                for (Map.Entry<String, ItemLocation[]> entry : ALL_LOCATIONS.entrySet()) {
                    Element item = doc.createElement("item");
                    root.appendChild(item);
                    Element key = doc.createElement("key");
                    item.appendChild(key);
                    appendElement(doc, key, "string", entry.getKey());
                    Element value = doc.createElement("value");
                    item.appendChild(value);
                    Element array = doc.createElement("ArrayOfItemLocation");
                    array.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
                    array.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                    value.appendChild(array);
                    for (ItemLocation location : entry.getValue()) {
                        if (location == null) {
                            Element nil = doc.createElement("ItemLocation");
                            nil.setAttribute("xsi:nil", "true");
                            array.appendChild(nil);
                        } else {
                            writeLocation(doc, array, location);
                        }
                    }
                }
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(path)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (Exception e) {
            // C# LocationFactory.Save does not catch; the DOM path is only used by tooling/tests.
        }
    }

    public static void load(Path path) {
        ItemLocationDictionary parsed = new ItemLocationDictionary();
        if (Files.exists(path)) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
                Element root = doc.getDocumentElement();
                for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                    Element item = (Element) n;
                    if (!item.getTagName().equals("item")) continue;
                    Element key = firstChild(item, "key");
                    Element value = firstChild(item, "value");
                    if (key == null || value == null) continue;
                    String id = text(key, "string");
                    Element array = firstChild(value, "ArrayOfItemLocation");
                    if (id == null || array == null) continue;
                    parsed.put(id, parseLocations(array));
                }
            } catch (Exception e) {
                // C# leaves the table cleared when the file cannot be deserialized
                parsed = new ItemLocationDictionary();
            }
        }
        synchronized (ALL_LOCATIONS) {
            ALL_LOCATIONS.clear();
            ALL_LOCATIONS.putAll(parsed);
        }
    }

    private static void writeLocation(Document doc, Element parent, ItemLocation location) {
        Element element = doc.createElement("ItemLocation");
        parent.appendChild(element);
        if (location.getClass() != ItemLocation.class) {
            element.setAttribute("xsi:type", location.getClass().getSimpleName());
        }
        if (location.getNote() != null) {
            appendElement(doc, element, "Note", location.getNote());
        }
        switch (location) {
            case VendorItem vendor -> {
                writeIntMap(doc, element, "TokenMap", vendor.getTokenMap());
                appendElement(doc, element, "Cost", Integer.toString(vendor.getCost()));
                if (vendor.getVendorName() != null) appendElement(doc, element, "VendorName", vendor.getVendorName());
                if (vendor.getVendorArea() != null) appendElement(doc, element, "VendorArea", vendor.getVendorArea());
            }
            case StaticDrop drop -> {
                if (drop.getArea() != null) appendElement(doc, element, "Area", drop.getArea());
                appendElement(doc, element, "Heroic", Boolean.toString(drop.isHeroic()));
                if (drop.getBoss() != null) appendElement(doc, element, "Boss", drop.getBoss());
            }
            case WorldDrop world -> {
                if (world.getLocation() != null) appendElement(doc, element, "Location", world.getLocation());
                appendElement(doc, element, "Heroic", Boolean.toString(world.isHeroic()));
            }
            case ContainerItem container -> {
                appendElement(doc, element, "Heroic", Boolean.toString(container.isHeroic()));
                if (container.getArea() != null) appendElement(doc, element, "Area", container.getArea());
                if (container.getContainer() != null) appendElement(doc, element, "Container", container.getContainer());
                appendElement(doc, element, "MinLevel", Integer.toString(container.getMinLevel()));
                appendElement(doc, element, "Party", Integer.toString(container.getParty()));
            }
            case QuestItem quest -> {
                if (quest.getArea() != null) appendElement(doc, element, "Area", quest.getArea());
                if (quest.getQuest() != null) appendElement(doc, element, "Quest", quest.getQuest());
                appendElement(doc, element, "MinLevel", Integer.toString(quest.getMinLevel()));
                appendElement(doc, element, "Party", Integer.toString(quest.getParty()));
                if (quest.getType() != null) appendElement(doc, element, "Type", quest.getType());
            }
            case CraftedItem crafted -> {
                if (crafted.getSkill() != null) appendElement(doc, element, "Skill", crafted.getSkill());
                appendElement(doc, element, "Level", Integer.toString(crafted.getLevel()));
                appendElement(doc, element, "Bind", crafted.getBind().name());
                if (crafted.getSpellName() != null) appendElement(doc, element, "SpellName", crafted.getSpellName());
                // BopMats is read-only in the original and not serialized by XmlSerializer
            }
            case PvpItem pvp -> {
                if (pvp.getPointType() != null) appendElement(doc, element, "PointType", pvp.getPointType());
                appendElement(doc, element, "Points", Integer.toString(pvp.getPoints()));
                if (pvp.getTokenType() != null) appendElement(doc, element, "TokenType", pvp.getTokenType());
                appendElement(doc, element, "TokenCount", Integer.toString(pvp.getTokenCount()));
                if (pvp.getPointTypeA() != null) appendElement(doc, element, "PointTypeA", pvp.getPointTypeA());
                appendElement(doc, element, "PointsA", Integer.toString(pvp.getPointsA()));
                appendElement(doc, element, "RequiredArenaRating", Integer.toString(pvp.getRequiredArenaRating()));
                if (pvp.getVendorName() != null) appendElement(doc, element, "VendorName", pvp.getVendorName());
                if (pvp.getVendorArea() != null) appendElement(doc, element, "VendorArea", pvp.getVendorArea());
            }
            case FactionItem faction -> {
                if (faction.getFactionName() != null) appendElement(doc, element, "FactionName", faction.getFactionName());
                appendElement(doc, element, "Level", faction.getLevel().name());
                appendElement(doc, element, "Cost", Integer.toString(faction.getCost()));
                writeIntMap(doc, element, "TokenMap", faction.getTokenMap());
            }
            default -> {
            }
        }
    }

    private static void writeIntMap(Document doc, Element parent, String name, LinkedHashMap<String, Integer> map) {
        Element mapElement = doc.createElement(name);
        parent.appendChild(mapElement);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            Element item = doc.createElement("item");
            mapElement.appendChild(item);
            Element key = doc.createElement("key");
            item.appendChild(key);
            appendElement(doc, key, "string", entry.getKey());
            Element value = doc.createElement("value");
            item.appendChild(value);
            appendElement(doc, value, "int", Integer.toString(entry.getValue()));
        }
    }

    private static ItemLocation[] parseLocations(Element array) {
        ItemLocation first = null;
        ItemLocation second = null;
        for (Node n = array.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("ItemLocation")) continue;
            if ("true".equals(e.getAttribute("xsi:nil"))) continue;
            ItemLocation location = parseLocation(e);
            if (location == null) continue;
            if (first == null) {
                first = location;
            } else if (second == null) {
                second = location;
            }
        }
        return new ItemLocation[] { first, second };
    }

    private static ItemLocation parseLocation(Element e) {
        String type = e.getAttribute("xsi:type");
        ItemLocation location;
        switch (type) {
            case "VendorItem": {
                VendorItem vendor = new VendorItem();
                vendor.setTokenMap(parseIntMap(e, "TokenMap"));
                vendor.setCost(intValue(e, "Cost", 0));
                vendor.setVendorName(text(e, "VendorName"));
                vendor.setVendorArea(text(e, "VendorArea"));
                location = vendor;
                break;
            }
            case "StaticDrop": {
                StaticDrop drop = new StaticDrop();
                drop.setArea(text(e, "Area"));
                drop.setHeroic(booleanValue(e, "Heroic"));
                drop.setBoss(text(e, "Boss"));
                location = drop;
                break;
            }
            case "WorldDrop": {
                WorldDrop world = new WorldDrop();
                world.setLocation(text(e, "Location"));
                world.setHeroic(booleanValue(e, "Heroic"));
                location = world;
                break;
            }
            case "ContainerItem": {
                ContainerItem container = new ContainerItem();
                container.setHeroic(booleanValue(e, "Heroic"));
                container.setArea(text(e, "Area"));
                container.setContainer(text(e, "Container"));
                container.setMinLevel(intValue(e, "MinLevel", 0));
                container.setParty(intValue(e, "Party", 0));
                location = container;
                break;
            }
            case "QuestItem": {
                QuestItem quest = (QuestItem) QuestItem.construct();
                quest.setArea(text(e, "Area"));
                quest.setQuest(text(e, "Quest"));
                quest.setMinLevel(intValue(e, "MinLevel", 0));
                quest.setParty(intValue(e, "Party", 0));
                quest.setType(text(e, "Type"));
                location = quest;
                break;
            }
            case "CraftedItem": {
                CraftedItem crafted = (CraftedItem) CraftedItem.construct();
                crafted.setSkill(text(e, "Skill"));
                crafted.setLevel(intValue(e, "Level", 0));
                String bind = text(e, "Bind");
                if (bind != null) crafted.setBind(BindsOn.valueOf(bind));
                crafted.setSpellName(text(e, "SpellName"));
                crafted.getBopMats().putAll(parseIntMap(e, "BopMats"));
                location = crafted;
                break;
            }
            case "PvpItem": {
                PvpItem pvp = new PvpItem();
                pvp.setPointType(text(e, "PointType"));
                pvp.setPoints(intValue(e, "Points", 0));
                pvp.setTokenType(text(e, "TokenType"));
                pvp.setTokenCount(intValue(e, "TokenCount", 0));
                pvp.setPointTypeA(text(e, "PointTypeA"));
                pvp.setPointsA(intValue(e, "PointsA", 0));
                pvp.setRequiredArenaRating(intValue(e, "RequiredArenaRating", 0));
                pvp.setVendorName(text(e, "VendorName"));
                pvp.setVendorArea(text(e, "VendorArea"));
                location = pvp;
                break;
            }
            case "FactionItem": {
                FactionItem faction = new FactionItem();
                faction.setFactionName(text(e, "FactionName"));
                String level = text(e, "Level");
                if (level != null) faction.setLevel(ReputationLevel.valueOf(level));
                faction.setCost(intValue(e, "Cost", 0));
                faction.setTokenMap(parseIntMap(e, "TokenMap"));
                location = faction;
                break;
            }
            case "NoSource":
                location = new NoSource();
                break;
            case "UnknownItem":
                location = new UnknownItem();
                break;
            default:
                location = new ItemLocation();
                break;
        }
        String note = text(e, "Note");
        if (note != null) location.setNote(note);
        return location;
    }

    /** Legacy dictionary encoding: &lt;item&gt;&lt;key&gt;&lt;string&gt;k&lt;/string&gt;&lt;/key&gt;&lt;value&gt;&lt;int&gt;n&lt;/int&gt;&lt;/value&gt;&lt;/item&gt;. */
    private static LinkedHashMap<String, Integer> parseIntMap(Element parent, String childName) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        Element mapElement = firstChild(parent, childName);
        if (mapElement == null) return map;
        for (Node n = mapElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element item = (Element) n;
            if (!item.getTagName().equals("item")) continue;
            Element key = firstChild(item, "key");
            Element value = firstChild(item, "value");
            if (key == null || value == null) continue;
            String keyText = text(key, "string");
            String valueText = text(value, "int");
            if (keyText != null && valueText != null) {
                map.put(keyText, Integer.parseInt(valueText));
            }
        }
        return map;
    }

    private static int intValue(Element parent, String name, int fallback) {
        String text = text(parent, name);
        return text == null || text.isEmpty() ? fallback : Integer.parseInt(text);
    }

    private static boolean booleanValue(Element parent, String name) {
        return Boolean.parseBoolean(text(parent, name));
    }



}
