// Java-only helper extracted from the identical DOM helpers in the ported XML writers/readers.
package rawr.base.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Small DOM helpers shared by the ported XML writers and readers.
 *
 * <p>The original code repeated these methods in several classes. Two text variants are kept
 * explicitly because the call sites differ: {@link #text(Element, String)} trims surrounding
 * whitespace (the schema-loader behavior), while {@link #rawText(Element, String)} returns the
 * element's text verbatim (the batch list keeps the exact saved value). Likewise
 * {@link #appendElement(Document, Element, String, String)} always writes the element, while
 * {@link #appendElementIfNotNull(Document, Element, String, String)} is the save-side variant that
 * skips {@code null} values. Keeping the variants explicit prevents a refactor from changing the
 * serialized output.</p>
 */
public final class XmlDom {
    private XmlDom() {
    }

    public static void appendElement(Document doc, Element parent, String name, String text) {
        Element element = doc.createElement(name);
        element.setTextContent(text);
        parent.appendChild(element);
    }

    public static void appendElementIfNotNull(Document doc, Element parent, String name, String text) {
        if (text == null) {
            return;
        }
        appendElement(doc, parent, name, text);
    }

    public static Element firstChild(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(name)) {
                return (Element) node;
            }
        }
        return null;
    }

    /** Trimmed text of the first child element named {@code name}; {@code null} when absent. */
    public static String text(Element parent, String name) {
        Element element = firstChild(parent, name);
        return element == null ? null : element.getTextContent().trim();
    }

    /** Unmodified text of the first child element named {@code name}; {@code null} when absent. */
    public static String rawText(Element parent, String name) {
        Element element = firstChild(parent, name);
        return element == null ? null : element.getTextContent();
    }
}
