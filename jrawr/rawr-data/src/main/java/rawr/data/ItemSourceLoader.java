// New loader code (not a line-by-line port): loads the original ItemSource.xml (the XmlSerializer
// output of Rawr.Base's ItemLocationDictionary, written by LocationFactory.Save) into rawr-core's
// LocationFactory. Element names follow the C# property names; xsi:type selects the ItemLocation
// subclass. Only the members needed to render a source description are read.
package rawr.data;

import java.util.LinkedHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ContainerItem;
import rawr.base.items.sources.CraftedItem;
import rawr.base.items.sources.FactionItem;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.sources.LocationFactory;
import rawr.base.items.sources.NoSource;
import rawr.base.items.sources.PvpItem;
import rawr.base.items.sources.QuestItem;
import rawr.base.character.ReputationLevel;
import rawr.base.items.sources.StaticDrop;
import rawr.base.items.sources.UnknownItem;
import rawr.base.items.sources.VendorItem;
import rawr.base.items.sources.WorldDrop;

import java.nio.file.Path;

public final class ItemSourceLoader {
    private ItemSourceLoader() {
    }

    public static void load(Path path) throws Exception {
        Document doc = XmlLoader.parse(path);
        Element root = doc.getDocumentElement();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element item = (Element) n;
            if (!item.getTagName().equals("item")) continue;
            Element key = XmlLoader.firstChild(item, "key");
            Element value = XmlLoader.firstChild(item, "value");
            if (key == null || value == null) continue;
            String id = XmlLoader.text(key, "string");
            Element array = XmlLoader.firstChild(value, "ArrayOfItemLocation");
            if (id == null || array == null) continue;
            LocationFactory.add(id, parseLocations(array), true);
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

    /** Parses one {@code <ItemLocation xsi:type="...">} element; returns null for unknown types. */
    public static ItemLocation parseLocation(Element e) {
        switch (e.getAttribute("xsi:type")) {
            case "VendorItem": {
                VendorItem location = new VendorItem();
                location.setTokenMap(parseIntMap(e, "TokenMap"));
                location.setCost(intValue(e, "Cost", 0));
                location.setVendorName(XmlLoader.text(e, "VendorName"));
                location.setVendorArea(XmlLoader.text(e, "VendorArea"));
                return location;
            }
            case "StaticDrop": {
                StaticDrop location = new StaticDrop();
                location.setArea(XmlLoader.text(e, "Area"));
                location.setHeroic(booleanValue(e, "Heroic"));
                location.setBoss(XmlLoader.text(e, "Boss"));
                return location;
            }
            case "WorldDrop": {
                WorldDrop location = new WorldDrop();
                location.setLocation(XmlLoader.text(e, "Location"));
                location.setHeroic(booleanValue(e, "Heroic"));
                return location;
            }
            case "ContainerItem": {
                ContainerItem location = new ContainerItem();
                location.setHeroic(booleanValue(e, "Heroic"));
                location.setArea(XmlLoader.text(e, "Area"));
                location.setContainer(XmlLoader.text(e, "Container"));
                location.setMinLevel(intValue(e, "MinLevel", 0));
                location.setParty(intValue(e, "Party", 0));
                return location;
            }
            case "QuestItem": {
                QuestItem location = (QuestItem) QuestItem.construct();
                location.setArea(XmlLoader.text(e, "Area"));
                location.setQuest(XmlLoader.text(e, "Quest"));
                location.setMinLevel(intValue(e, "MinLevel", 0));
                location.setParty(intValue(e, "Party", 0));
                location.setType(XmlLoader.text(e, "Type"));
                return location;
            }
            case "CraftedItem": {
                CraftedItem location = (CraftedItem) CraftedItem.construct();
                location.setSkill(XmlLoader.text(e, "Skill"));
                location.setLevel(intValue(e, "Level", 0));
                String bind = XmlLoader.text(e, "Bind");
                location.setBind(bind == null ? BindsOn.None : BindsOn.valueOf(bind));
                location.setSpellName(XmlLoader.text(e, "SpellName"));
                location.getBopMats().putAll(parseIntMap(e, "BopMats"));
                return location;
            }
            case "PvpItem": {
                PvpItem location = new PvpItem();
                location.setPointType(XmlLoader.text(e, "PointType"));
                location.setPoints(intValue(e, "Points", 0));
                location.setTokenType(XmlLoader.text(e, "TokenType"));
                location.setTokenCount(intValue(e, "TokenCount", 0));
                location.setPointTypeA(XmlLoader.text(e, "PointTypeA"));
                location.setPointsA(intValue(e, "PointsA", 0));
                location.setRequiredArenaRating(intValue(e, "RequiredArenaRating", 0));
                location.setVendorName(XmlLoader.text(e, "VendorName"));
                location.setVendorArea(XmlLoader.text(e, "VendorArea"));
                return location;
            }
            case "FactionItem": {
                FactionItem location = new FactionItem();
                location.setFactionName(XmlLoader.text(e, "FactionName"));
                String level = XmlLoader.text(e, "Level");
                location.setLevel(level == null ? ReputationLevel.Neutral : ReputationLevel.valueOf(level));
                location.setCost(intValue(e, "Cost", 0));
                location.setTokenMap(parseIntMap(e, "TokenMap"));
                return location;
            }
            case "NoSource":
                return new NoSource();
            case "UnknownItem":
                return new UnknownItem();
            default:
                return null;
        }
    }

    /** Legacy dictionary encoding: &lt;item&gt;&lt;key&gt;&lt;string&gt;k&lt;/string&gt;&lt;/key&gt;&lt;value&gt;&lt;int&gt;n&lt;/int&gt;&lt;/value&gt;&lt;/item&gt;. */
    private static LinkedHashMap<String, Integer> parseIntMap(Element parent, String childName) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        Element mapElement = XmlLoader.firstChild(parent, childName);
        if (mapElement == null) return map;
        for (Node n = mapElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element item = (Element) n;
            if (!item.getTagName().equals("item")) continue;
            Element key = XmlLoader.firstChild(item, "key");
            Element value = XmlLoader.firstChild(item, "value");
            if (key == null || value == null) continue;
            String keyText = XmlLoader.text(key, "string");
            String valueText = XmlLoader.text(value, "int");
            if (keyText != null && valueText != null) {
                map.put(keyText, Integer.parseInt(valueText));
            }
        }
        return map;
    }

    private static int intValue(Element parent, String name, int fallback) {
        String text = XmlLoader.text(parent, name);
        return text == null || text.isEmpty() ? fallback : Integer.parseInt(text);
    }

    private static boolean booleanValue(Element parent, String name) {
        return Boolean.parseBoolean(XmlLoader.text(parent, name));
    }
}
