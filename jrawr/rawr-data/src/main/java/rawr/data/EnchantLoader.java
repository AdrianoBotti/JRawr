// New loader code (not a line-by-line port): loads the original EnchantCache.xml into Enchant's registry.
// PARTIAL: the C# LoadEnchants also merges GetDefaultEnchants() from code; that list is omitted for now
// (the cache file contains the enchants used by the fixtures).
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.Enchant;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;

import java.nio.file.Path;

public final class EnchantLoader {
    private EnchantLoader() {
    }

    public static void load(Path path) throws Exception {
        Document doc = XmlLoader.parse(path);
        Element root = doc.getDocumentElement();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("Enchant")) continue;
            Enchant enchant = new Enchant();
            enchant.id = Integer.parseInt(XmlLoader.text(e, "Id"));
            enchant.name = XmlLoader.text(e, "Name");
            enchant.slot = ItemSlot.valueOf(XmlLoader.text(e, "Slot"));
            Element statsElement = XmlLoader.firstChild(e, "Stats");
            if (statsElement != null) {
                XmlLoader.parseStats(statsElement, enchant.stats);
            }
            Enchant.getAllEnchants().add(enchant);
        }
    }
}
