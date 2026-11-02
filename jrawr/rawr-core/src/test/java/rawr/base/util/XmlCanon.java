package rawr.base.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Canonical element/text/attribute dump of an XML file for schema comparisons (whitespace and attribute order free). */
public final class XmlCanon {
    private XmlCanon() {
    }

    public static String canonical(Path path) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
        StringBuilder sb = new StringBuilder();
        walk(doc.getDocumentElement(), sb);
        return sb.toString();
    }

    private static void walk(Element element, StringBuilder sb) {
        sb.append('<').append(element.getTagName());
        NamedNodeMap attributes = element.getAttributes();
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < attributes.getLength(); i++) {
            names.add(attributes.item(i).getNodeName());
        }
        Collections.sort(names);
        for (String name : names) {
            sb.append(' ').append(name).append("=\"").append(element.getAttribute(name)).append('"');
        }
        List<Node> children = new ArrayList<Node>();
        StringBuilder text = new StringBuilder();
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                children.add(n);
            } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                String value = n.getNodeValue().trim();
                if (!value.isEmpty()) {
                    text.append(value);
                }
            }
        }
        if (children.isEmpty() && text.length() == 0) {
            sb.append("/>");
            return;
        }
        sb.append('>');
        if (text.length() > 0) {
            sb.append(text);
        }
        for (Node n : children) {
            walk((Element) n, sb);
        }
        sb.append("</").append(element.getTagName()).append('>');
    }
}
