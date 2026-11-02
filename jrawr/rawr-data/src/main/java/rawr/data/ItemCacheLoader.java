// New loader code (not a line-by-line port): loads the original ItemCache.xml into rawr-core's ItemCache.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemDamageType;
import rawr.base.items.ItemFaction;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ItemCacheLoader {
    private ItemCacheLoader() {
    }

    public static void load(Path path) throws Exception {
        for (Item item : read(path)) {
            ItemCache.addItem(item);
        }
    }

    /** Parses the {@code <Item>} children without touching the cache (used by the side-car store). */
    public static List<Item> read(Path path) throws Exception {
        return read(XmlLoader.parse(path));
    }

    /** Parses the {@code <Item>} children of an already-parsed document; unknown root elements
     * (for example the item-source sections of the side-car) are ignored. */
    public static List<Item> read(Document doc) {
        Element root = doc.getDocumentElement();
        List<Item> items = new ArrayList<Item>();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element itemElement = (Element) n;
            if (!itemElement.getTagName().equals("Item")) continue;
            items.add(parseItem(itemElement));
        }
        return items;
    }

    private static Item parseItem(Element itemElement) {
        Item item = new Item();
        for (Node n = itemElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            String name = e.getTagName();
            String text = e.getTextContent().trim();
            switch (name) {
                case "ItemLevel":
                    item.setItemLevel(Integer.parseInt(text));
                    break;
                case "DisplayId":
                    item.setDisplayId(Integer.parseInt(text));
                    break;
                case "DisplaySlot":
                    item.setDisplaySlot(Integer.parseInt(text));
                    break;
                case "IconPath":
                    item.setIconPath(text);
                    break;
                case "Stats":
                    XmlLoader.parseStats(e, item.getStats());
                    break;
                case "Quality":
                    item.setQuality(ItemQuality.valueOf(text));
                    break;
                case "Type":
                    item.setType(ItemType.valueOf(text));
                    break;
                case "Faction":
                    item.setFaction(ItemFaction.valueOf(text));
                    break;
                case "SocketBonus":
                    XmlLoader.parseStats(e, item.getSocketBonus());
                    break;
                case "Name":
                    item.setName(text);
                    break;
                case "Id":
                    item.setId(Integer.parseInt(text));
                    break;
                case "Slot":
                    item.setSlot(ItemSlot.valueOf(text));
                    break;
                case "SetName":
                    item.setSetName(text);
                    break;
                case "Speed":
                    item.setSpeed(XmlLoader.parseFloat(text));
                    break;
                case "MinDamage":
                    item.setMinDamage(Integer.parseInt(text));
                    break;
                case "MaxDamage":
                    item.setMaxDamage(Integer.parseInt(text));
                    break;
                case "DamageType":
                    item.setDamageType(ItemDamageType.valueOf(text));
                    break;
                case "Bind":
                    item.setBind(BindsOn.valueOf(text));
                    break;
                case "Unique":
                    item.setUnique(Boolean.parseBoolean(text));
                    break;
                case "RequiredClasses":
                    item.setRequiredClasses(text);
                    break;
                case "LocalizedName":
                    item.setLocalizedName(text);
                    break;
                case "SocketColor1":
                    item.setSocketColor1(ItemSlot.valueOf(text));
                    break;
                case "SocketColor2":
                    item.setSocketColor2(ItemSlot.valueOf(text));
                    break;
                case "SocketColor3":
                    item.setSocketColor3(ItemSlot.valueOf(text));
                    break;
                case "UniqueId": {
                    List<Integer> uniqueIds = new ArrayList<Integer>();
                    for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if (c.getNodeType() == Node.ELEMENT_NODE) {
                            uniqueIds.add(Integer.parseInt(c.getTextContent().trim()));
                        }
                    }
                    item.setUniqueId(uniqueIds);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Item element: " + name);
            }
        }
        return item;
    }
}
