// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The filter model, the in-memory matching entry points, Compile() and the Path-based
// Save/Load pair over the original XmlSerializer schema. Load replaces the global data on
// any parse problem, like the original. The ItemFilterEnabledOverride snapshot/apply members
// ported from Character.cs:224-303 live on Character.
package rawr.base.items;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;

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
import java.util.List;
import java.util.Map;
import static rawr.base.util.XmlDom.appendElement;
import static rawr.base.util.XmlDom.firstChild;
import static rawr.base.util.XmlDom.text;

/**
 * The item filter: the user's rule set deciding which items count as "relevant" beyond what a
 * class model says it can use, plus the static entry points models and the item picker call.
 *
 * <p>Filtering has two tiers. First, an item must have a type the model cares about (the
 * {@link ItemTypeList} stored in {@link ItemFilterData#relevantItemTypes}). Second, the regex
 * tree is consulted ({@link ItemFilterRegex}): top-level <i>additive</i> rules include an item
 * whose location text and bind/level/quality traits they match, but only if the rule (or one of
 * its enabled children) is enabled; when no additive rule matches, the global
 * {@link #isOtherEnabled() Other} catch-all decides. Finally, top-level non-additive rules act
 * as exclusions and veto an otherwise relevant item. See
 * {@link #isItemRelevant(CalculationsBase, Item)} for the exact order.</p>
 *
 * <p>The live data is one static {@link ItemFilterData} loaded from {@code ItemFilter.xml} by
 * {@code rawr-data}, and {@link #load(Path)} replaces it wholesale; a parse failure installs a
 * fresh default object, matching the original. {@link #compile()} forces every matching path to
 * run once over the real cache (the original used it to warm up lazily compiled regexes);
 * {@link #isLoading()} is a UI edit-state flag kept for parity.</p>
 *
 * <p>Per-character customization: a saved character file stores a snapshot of the tree's enabled
 * flags as {@link ItemFilterEnabledOverride}s; {@code Character} rebuilds the snapshot on save
 * and applies it on load, which is also why it calls {@link ItemCache#onItemsChanged()} after
 * applying. The Path-based save/load reproduce the original {@code ItemFilterData} XML shape.
 * This class itself is stateless apart from the filter data; it does not own the rules, it
 * interprets them.</p>
 *
 * @see ItemFilterRegex
 * @see ItemFilterData
 * @see ItemCache#getRelevantItemsInternal
 */
public final class ItemFilter {
    private static ItemFilterData data = new ItemFilterData();

    private ItemFilter() {
    }

    /// Returns a list that can be used to set relevant item types list.
    /**
     * The mutable list of item types a model is allowed to use, seeded on first use from
     * {@code CalculationsBase.getRelevantItemTypes()} and cached per model name. Because the
     * cached list is returned by reference (and is also stored in the serialized filter data),
     * callers editing it change the model's relevance set.
     */
    public static List<ItemType> getRelevantItemTypesList(CalculationsBase model) {
        ItemTypeList list = data.relevantItemTypes.get(model.getName());
        if (list == null) {
            list = new ItemTypeList(model.getRelevantItemTypes());
            data.relevantItemTypes.put(model.getName(), list);
        }
        return list;
    }

    public static ItemFilterRegexList getFilterList() {
        return data.regexList;
    }

    public static boolean isOtherEnabled() {
        return data.otherRegexEnabled;
    }

    public static void setOtherEnabled(boolean value) {
        data.otherRegexEnabled = value;
    }

    /**
     * The full relevance decision for one item, in order:
     * <ol>
     *   <li>the item's {@link ItemType} must be in the model's relevant-type list;</li>
     *   <li>among the top-level <i>additive</i> rules that apply to the item and match it: if any
     *       match, the item is included when at least one matching rule is enabled (recursively,
     *       via {@link ItemFilterRegex#isEnabled(Item)}); if none match, the global
     *       {@link #isOtherEnabled()} flag decides;</li>
     *   <li>if included, any top-level <i>non-additive</i> (exclusion) rule that is enabled,
     *       applies, matches and is recursively enabled vetoes the item;</li>
     *   <li>anything not included (type mismatch or filter rejection) is not relevant.</li>
     * </ol>
     * Called by {@link ItemCache#getRelevantItems(CalculationsBase, CharacterRace)} and
     * {@link ItemCache#getRelevantItemsInternal(CalculationsBase)}.
     */
    public static boolean isItemRelevant(CalculationsBase model, Item item) {
        if (getRelevantItemTypesList(model).contains(item.getType())) {
            boolean added;
            boolean anyMatch = false;
            boolean enabledMatch = false;
            for (ItemFilterRegex regex : data.regexList) {
                if (regex.isAdditiveFilter() && regex.appliesTo(item) && regex.isMatch(item)) {
                    anyMatch = true;
                    if (regex.isEnabled(item)) {
                        enabledMatch = true;
                        break;
                    }
                }
            }
            if (anyMatch) {
                added = enabledMatch;
            } else {
                added = isOtherEnabled();
            }
            if (added) {
                for (ItemFilterRegex regex : data.regexList) {
                    if (!regex.isAdditiveFilter() && regex.isEnabled() && regex.appliesTo(item) && regex.isMatch(item) && regex.isEnabled(item)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isLoading;

    /**
     * Edit-state flag belonging to the original's filter UI: while {@code true} the UI is
     * loading data and suppresses change handling. Kept because the port mirrors the original's
     * surface; it does not affect matching.
     */
    public static boolean isLoading() {
        return isLoading;
    }

    public static void setLoading(boolean value) {
        isLoading = value;
    }

    // C# ItemFilter.cs:314-321 Compile(): exercise all filter code paths on real data.
    /**
     * Warms up the filter by running the relevance computation over every cached item for the
     * current model, which forces each rule's lazily compiled pattern to compile. The return
     * value is discarded on purpose; call it after loading or editing filters when the first
     * picker/optimizer interaction must not pay the compile cost.
     */
    public static void compile() {
        ItemCache.getRelevantItemsInternal(Calculations.getInstance());
    }

    // C# ItemFilter.cs:323-354 Save(string fileName)/Load(string fileName) with the executable
    // directory replaced by an explicit path. Schema is the XmlSerializer ItemFilterData mapping.
    public static void save(Path path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ItemFilterData");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(root);

            Element relevantItemTypes = doc.createElement("RelevantItemTypes");
            root.appendChild(relevantItemTypes);
            for (Map.Entry<String, ItemTypeList> entry : data.relevantItemTypes.entrySet()) {
                Element item = doc.createElement("item");
                relevantItemTypes.appendChild(item);
                Element key = doc.createElement("key");
                item.appendChild(key);
                appendElement(doc, key, "string", entry.getKey());
                Element value = doc.createElement("value");
                item.appendChild(value);
                Element array = doc.createElement("ArrayOfItemType");
                value.appendChild(array);
                for (ItemType type : entry.getValue()) {
                    appendElement(doc, array, "ItemType", type.name());
                }
            }

            Element regexList = doc.createElement("RegexList");
            root.appendChild(regexList);
            for (ItemFilterRegex regex : data.regexList) {
                writeRegex(doc, regexList, regex);
            }

            appendElement(doc, root, "OtherRegexEnabled", Boolean.toString(data.otherRegexEnabled));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(path)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (Exception e) {
            // C# ItemFilter.Save does not catch; the DOM path is only used by tooling/tests.
        }
    }

    /**
     * Replaces the global filter data with the parsed file; a missing file or a parse failure
     * installs a fresh {@link ItemFilterData} (defaults: no per-model type overrides, no regex
     * rules, Other enabled), exactly like the original's XmlSerializer fallback. The Path
     * parameter replaces the original's executable-relative path.
     */
    public static void load(Path path) {
        if (Files.exists(path)) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
                data = readData(doc.getDocumentElement());
            } catch (Exception e) {
                data = new ItemFilterData();
            }
        } else {
            data = new ItemFilterData();
        }
    }

    private static ItemFilterData readData(Element root) {
        ItemFilterData parsed = new ItemFilterData();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            switch (e.getTagName()) {
                case "RelevantItemTypes":
                    readRelevantItemTypes(e, parsed);
                    break;
                case "RegexList":
                    for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if (c.getNodeType() == Node.ELEMENT_NODE && ((Element) c).getTagName().equals("ItemFilterRegex")) {
                            parsed.regexList.add(readRegex((Element) c));
                        }
                    }
                    break;
                case "OtherRegexEnabled":
                    parsed.otherRegexEnabled = Boolean.parseBoolean(e.getTextContent().trim());
                    break;
                default:
                    break;
            }
        }
        return parsed;
    }

    private static void readRelevantItemTypes(Element element, ItemFilterData parsed) {
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element item = (Element) n;
            if (!item.getTagName().equals("item")) continue;
            Element key = firstChild(item, "key");
            Element value = firstChild(item, "value");
            if (key == null || value == null) continue;
            String modelName = text(key, "string");
            Element array = firstChild(value, "ArrayOfItemType");
            if (modelName == null || array == null) continue;
            ItemTypeList list = new ItemTypeList();
            for (Node c = array.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() == Node.ELEMENT_NODE && ((Element) c).getTagName().equals("ItemType")) {
                    list.add(ItemType.valueOf(c.getTextContent().trim()));
                }
            }
            parsed.relevantItemTypes.put(modelName, list);
        }
    }

    private static void writeRegex(Document doc, Element parent, ItemFilterRegex regex) {
        Element element = doc.createElement("ItemFilterRegex");
        parent.appendChild(element);
        Element nested = doc.createElement("RegexList");
        element.appendChild(nested);
        for (ItemFilterRegex sub : regex.regexList) {
            writeRegex(doc, nested, sub);
        }
        appendElement(doc, element, "OtherRegexEnabled", Boolean.toString(regex.otherRegexEnabled));
        if (regex.getName() != null) {
            appendElement(doc, element, "Name", regex.getName());
        }
        if (regex.getPattern() != null) {
            appendElement(doc, element, "Pattern", regex.getPattern());
        }
        appendElement(doc, element, "BoA", Boolean.toString(regex.isBoA()));
        appendElement(doc, element, "BoE", Boolean.toString(regex.isBoE()));
        appendElement(doc, element, "BoP", Boolean.toString(regex.isBoP()));
        appendElement(doc, element, "BoU", Boolean.toString(regex.isBoU()));
        appendElement(doc, element, "BoN", Boolean.toString(regex.isBoN()));
        appendElement(doc, element, "MinItemLevel", Integer.toString(regex.getMinItemLevel()));
        appendElement(doc, element, "MaxItemLevel", Integer.toString(regex.getMaxItemLevel()));
        appendElement(doc, element, "MinItemQuality", regex.getMinItemQuality().name());
        appendElement(doc, element, "MaxItemQuality", regex.getMaxItemQuality().name());
        appendElement(doc, element, "AdditiveFilter", Boolean.toString(regex.isAdditiveFilter()));
        appendElement(doc, element, "AppliesToItems", Boolean.toString(regex.isAppliesToItems()));
        appendElement(doc, element, "AppliesToGems", Boolean.toString(regex.isAppliesToGems()));
        appendElement(doc, element, "Enabled", Boolean.toString(regex.isEnabled()));
    }

    private static ItemFilterRegex readRegex(Element e) {
        ItemFilterRegex regex = new ItemFilterRegex();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element child = (Element) n;
            String text = child.getTextContent().trim();
            switch (child.getTagName()) {
                case "RegexList":
                    for (Node c = child.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if (c.getNodeType() == Node.ELEMENT_NODE && ((Element) c).getTagName().equals("ItemFilterRegex")) {
                            regex.regexList.add(readRegex((Element) c));
                        }
                    }
                    break;
                case "OtherRegexEnabled":
                    regex.otherRegexEnabled = Boolean.parseBoolean(text);
                    break;
                case "Name":
                    regex.setName(text);
                    break;
                case "Pattern":
                    regex.setPattern(text);
                    break;
                case "BoA":
                    regex.setBoA(Boolean.parseBoolean(text));
                    break;
                case "BoE":
                    regex.setBoE(Boolean.parseBoolean(text));
                    break;
                case "BoP":
                    regex.setBoP(Boolean.parseBoolean(text));
                    break;
                case "BoU":
                    regex.setBoU(Boolean.parseBoolean(text));
                    break;
                case "BoN":
                    regex.setBoN(Boolean.parseBoolean(text));
                    break;
                case "MinItemLevel":
                    regex.setMinItemLevel(Integer.parseInt(text));
                    break;
                case "MaxItemLevel":
                    regex.setMaxItemLevel(Integer.parseInt(text));
                    break;
                case "MinItemQuality":
                    regex.setMinItemQuality(ItemQuality.valueOf(text));
                    break;
                case "MaxItemQuality":
                    regex.setMaxItemQuality(ItemQuality.valueOf(text));
                    break;
                case "AdditiveFilter":
                    regex.setAdditiveFilter(Boolean.parseBoolean(text));
                    break;
                case "AppliesToItems":
                    regex.setAppliesToItems(Boolean.parseBoolean(text));
                    break;
                case "AppliesToGems":
                    regex.setAppliesToGems(Boolean.parseBoolean(text));
                    break;
                case "Enabled":
                    regex.setEnabled(Boolean.parseBoolean(text));
                    break;
                default:
                    break;
            }
        }
        return regex;
    }



}
