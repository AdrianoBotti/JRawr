// New loader code (not a line-by-line port): loads the original BuffCache.xml into Buff's registry.
// Deviation from C# LoadBuffs: the code-defined GetDefaultBuffs list is not ported, so every cache entry
// (custom and non-custom) is kept; C# would replace non-custom entries with the code defaults.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.character.Buff;
import rawr.base.character.CharacterClass;
import rawr.base.character.Profession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuffLoader {
    private BuffLoader() {
    }

    public static void load(Path path) throws Exception {
        Document doc = XmlLoader.parse(path);
        Element root = doc.getDocumentElement();
        Map<String, Buff> byName = new LinkedHashMap<String, Buff>();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("Buff")) continue;
            Buff buff = parseBuff(e);
            if (buff.name != null && !byName.containsKey(buff.name)) {
                byName.put(buff.name, buff);
            }
        }
        Buff.load(new ArrayList<Buff>(byName.values()));
    }

    static Buff parseBuff(Element e) {
        Buff buff = new Buff();
        buff.name = XmlLoader.text(e, "Name");
        buff.group = XmlLoader.text(e, "Group");
        Element statsElement = XmlLoader.firstChild(e, "Stats");
        if (statsElement != null) {
            XmlLoader.parseStats(statsElement, buff.stats);
        }
        String setThreshold = XmlLoader.text(e, "SetThreshold");
        if (setThreshold != null) buff.setThreshold = Integer.parseInt(setThreshold);
        Element improvements = XmlLoader.firstChild(e, "Improvements");
        if (improvements != null) {
            for (Node n = improvements.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE && ((Element) n).getTagName().equals("Buff")) {
                    buff.improvements.add(parseBuff((Element) n));
                }
            }
        }
        String isTargetDebuff = XmlLoader.text(e, "IsTargetDebuff");
        if (isTargetDebuff != null) buff.isTargetDebuff = Boolean.parseBoolean(isTargetDebuff);
        String isCustom = XmlLoader.text(e, "IsCustom");
        if (isCustom != null) buff.isCustom = Boolean.parseBoolean(isCustom);
        buff.setName = XmlLoader.text(e, "SetName");
        buff.source = XmlLoader.text(e, "Source");

        Element allowedClasses = XmlLoader.firstChild(e, "AllowedClasses");
        if (allowedClasses != null) {
            List<CharacterClass> classes = new ArrayList<CharacterClass>();
            for (Node n = allowedClasses.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    classes.add(CharacterClass.valueOf(n.getTextContent().trim()));
                }
            }
            if (!classes.isEmpty()) buff.setAllowedClasses(classes);
        }
        Element professions = XmlLoader.firstChild(e, "Professions");
        if (professions != null) {
            List<Profession> professionsList = new ArrayList<Profession>();
            for (Node n = professions.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    professionsList.add(Profession.valueOf(n.getTextContent().trim()));
                }
            }
            if (!professionsList.isEmpty()) buff.setProfessions(professionsList);
        }
        Element conflicting = XmlLoader.firstChild(e, "ConflictingBuffs");
        if (conflicting != null) {
            List<String> conflicts = new ArrayList<String>();
            for (Node n = conflicting.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    conflicts.add(n.getTextContent().trim());
                }
            }
            if (!conflicts.isEmpty()) buff.setConflictingBuffs(conflicts);
        }
        return buff;
    }
}
