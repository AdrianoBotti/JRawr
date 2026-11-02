// New loader code: parses the original Talents.xml saved-spec list into SavedTalentSpec.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.character.SavedTalentSpecList;

import java.nio.file.Path;

/**
 * Loads the legacy {@code Talents.xml} saved-spec list. The file is the shipped default list plus,
 * in the original app, the user's saved specs. The parsed entries are installed into
 * {@link SavedTalentSpec#load(SavedTalentSpecList)}; the optimizer reads them per class.
 */
public final class TalentSpecLoader {
    private TalentSpecLoader() {
    }

    public static void load(Path path) throws Exception {
        Document doc = XmlLoader.parse(path);
        Element root = doc.getDocumentElement();
        SavedTalentSpecList specs = new SavedTalentSpecList();
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if (!"SavedTalentSpec".equals(element.getTagName())) {
                continue;
            }
            SavedTalentSpec spec = parseSpec(element);
            if (spec != null) {
                specs.add(spec);
            }
        }
        SavedTalentSpec.load(specs);
    }

    private static SavedTalentSpec parseSpec(Element element) {
        String className = XmlLoader.text(element, "Class");
        if (className == null) {
            return null;
        }
        SavedTalentSpec spec = new SavedTalentSpec();
        spec.name = XmlLoader.text(element, "Name");
        try {
            spec.characterClass = CharacterClass.valueOf(className);
        } catch (IllegalArgumentException e) {
            return null;
        }
        spec.spec = XmlLoader.text(element, "Spec");
        spec.tree1 = intValue(element, "Tree1");
        spec.tree2 = intValue(element, "Tree2");
        spec.tree3 = intValue(element, "Tree3");
        return spec;
    }

    private static int intValue(Element element, String name) {
        String text = XmlLoader.text(element, name);
        return text == null || text.isEmpty() ? 0 : Integer.parseInt(text);
    }
}
