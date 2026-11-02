package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Side-car for user-entered item purchase costs (badge/vendor currency). */
public final class ItemCostStore {
    public static final String DEFAULT_FILE_NAME = "jrawr.item-costs.xml";

    private final Path file;

    public ItemCostStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    public Path getFile() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /** Resets every item cost to zero, then applies the side-car entries (missing file = zeros). */
    public synchronized int load() {
        for (Item item : ItemCache.allItems()) {
            item.setCost(0f);
        }
        if (!exists()) {
            return 0;
        }
        try {
            Document doc = XmlLoader.parse(file);
            Map<Integer, Float> costs = read(doc.getDocumentElement());
            int applied = 0;
            for (Map.Entry<Integer, Float> entry : costs.entrySet()) {
                Item item = ItemCache.findItemById(entry.getKey());
                if (item != null) {
                    item.setCost(entry.getValue());
                    applied++;
                }
            }
            return applied;
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ItemCosts");
            doc.appendChild(root);
            for (Item item : ItemCache.allItems()) {
                if (item.getCost() > 0f) {
                    Element cost = doc.createElement("Cost");
                    cost.setAttribute("ItemId", Integer.toString(item.getId()));
                    cost.setAttribute("Value", Float.toString(item.getCost()));
                    root.appendChild(cost);
                }
            }
            Path temp = Files.createTempFile(parent == null ? Path.of(".") : parent,
                    "jrawr-item-costs-", ".xml");
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                try (OutputStream out = Files.newOutputStream(temp)) {
                    transformer.transform(new DOMSource(doc), new StreamResult(out));
                }
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicFailure) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not save item costs to " + file, e);
        }
    }

    public synchronized void reset() {
        for (Item item : ItemCache.allItems()) {
            item.setCost(0f);
        }
        save();
    }

    public synchronized void setCost(Item item, float cost) {
        if (item != null) {
            item.setCost(Math.max(0f, cost));
        }
    }

    private static Map<Integer, Float> read(Element root) {
        Map<Integer, Float> costs = new LinkedHashMap<>();
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element element = (Element) node;
            if ("Cost".equals(element.getTagName())) {
                add(costs, element.getAttribute("ItemId"), element.getAttribute("Value"));
            } else if ("item".equals(element.getTagName())) {
                String id = childText(element, "key");
                String value = childText(element, "value");
                add(costs, id, value);
            }
        }
        return costs;
    }

    private static void add(Map<Integer, Float> costs, String idText, String valueText) {
        try {
            int id = Integer.parseInt(idText.trim());
            float value = Float.parseFloat(valueText.trim());
            if (id > 0 && value > 0f && Float.isFinite(value)) {
                costs.put(id, value);
            }
        } catch (NumberFormatException ignored) {
            // malformed side-car entries are ignored, like the legacy loader
        }
    }

    private static String childText(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(name)) {
                return node.getTextContent();
            }
        }
        return "";
    }
}
