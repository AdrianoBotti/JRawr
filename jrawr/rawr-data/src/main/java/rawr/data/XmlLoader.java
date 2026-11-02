// New loader code (not a line-by-line port): parses the original Rawr XML formats into rawr-core types.
// Element names/order follow the XmlSerializer output used by the legacy app (see oracle/docs).
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.items.ItemSlot;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

public final class XmlLoader {
    private XmlLoader() {
    }

    public static Document parse(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(path.toFile());
    }

    public static Element firstChild(Element parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && ((Element) n).getTagName().equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    public static String text(Element parent, String name) {
        Element e = firstChild(parent, name);
        return e == null ? null : e.getTextContent().trim();
    }

    public static void parseStats(Element statsElement, Stats stats) {
        for (Node n = statsElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            String name = e.getTagName();
            if (name.equals("SpecialEffects")) {
                parseSpecialEffects(e, stats);
            } else if (name.equals("SpecialEffectCount")) {
                // implied by the number of SpecialEffect elements
            } else {
                Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
                if (property == null) {
                    throw new IllegalArgumentException("Unknown stat element: " + name);
                }
                String text = e.getTextContent().trim();
                float value = text.isEmpty() ? 0f : parseFloat(text);
                property.mutator().set(stats, value);
            }
        }
    }

    private static void parseSpecialEffects(Element specialEffectsElement, Stats stats) {
        for (Node n = specialEffectsElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("SpecialEffect")) continue;
            SpecialEffect effect = new SpecialEffect();
            for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                Element child = (Element) c;
                String name = child.getTagName();
                String value = child.getTextContent().trim();
                switch (name) {
                    case "Trigger":
                        effect.setTrigger(Trigger.valueOf(value));
                        break;
                    case "Stats":
                        if (effect.getStats() == null) effect.setStats(new Stats());
                        parseStats(child, effect.getStats());
                        break;
                    case "Duration":
                        effect.setDuration(parseFloat(value));
                        break;
                    case "Cooldown":
                        effect.setCooldown(parseFloat(value));
                        break;
                    case "Chance":
                        effect.setChance(parseFloat(value));
                        break;
                    case "MaxStack":
                        effect.setMaxStack(Integer.parseInt(value));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown SpecialEffect element: " + name);
                }
            }
            stats.addSpecialEffect(effect);
        }
    }

    public static float parseFloat(String text) {
        if (text.equals("INF")) return Float.POSITIVE_INFINITY;
        if (text.equals("-INF")) return Float.NEGATIVE_INFINITY;
        if (text.equals("NaN")) return Float.NaN;
        return Float.parseFloat(text);
    }

    public static ItemSlot itemSlot(String value) {
        return value == null ? ItemSlot.None : ItemSlot.valueOf(value);
    }
}
