// Java-only codec extracted from the former SerializableDictionary XML methods.
package rawr.base.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes the .NET {@code XmlSerializer} dictionary shape used by the character file's
 * {@code CalculationOptions} element: a sequence of
 * {@code <item><key><string>k</string></key><value><string>v</string></value></item>} entries.
 *
 * <p>The maps are ordinary insertion-ordered {@link LinkedHashMap}s; only the XML wrapping is
 * specific to the legacy schema. Writing a non-string key or value fails with
 * {@link IllegalArgumentException}; reading skips items that are missing a key or value element,
 * matching the original serializer behavior.</p>
 */
public final class XmlMap {
    private XmlMap() {
    }

    public static void writeStringMap(Document doc, Element parent, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Element item = doc.createElement("item");
            parent.appendChild(item);

            Element key = doc.createElement("key");
            item.appendChild(key);
            appendString(doc, key, entry.getKey());

            Element value = doc.createElement("value");
            item.appendChild(value);
            appendString(doc, value, entry.getValue());
        }
    }

    public static LinkedHashMap<String, String> readStringMap(Element parent) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element item = (Element) node;
            if (!item.getTagName().equals("item")) {
                continue;
            }
            Element key = XmlDom.firstChild(item, "key");
            Element value = XmlDom.firstChild(item, "value");
            if (key == null || value == null) {
                continue;
            }
            map.put(readString(key), readString(value));
        }
        return map;
    }

    private static void appendString(Document doc, Element parent, String value) {
        Element stringElement = doc.createElement("string");
        stringElement.setTextContent(value);
        parent.appendChild(stringElement);
    }

    private static String readString(Element parent) {
        Element stringElement = XmlDom.firstChild(parent, "string");
        return stringElement == null ? null : stringElement.getTextContent();
    }
}
