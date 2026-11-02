// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enchant.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Id/Name/Slot/Stats, slot fitting, lookups, ShortName/ReallyShortName, equals/hashCode, toString,
// the code-defined getDefaultEnchants() list and Path-based loadEnchants/saveEnchants. The C# static
// constructor and _SaveFilePath need System.Windows.Forms.Application.ExecutablePath; the port takes
// an explicit Path instead. rawr-data's EnchantLoader is unchanged and still loads EnchantCache.xml.
package rawr.base.items;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.stats.Stats;

import static rawr.base.stats.StatFactory.makeStats;

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
import java.util.ArrayList;
import java.util.List;
import static rawr.base.util.XmlDom.appendElement;
import static rawr.base.util.XmlDom.firstChild;
import static rawr.base.util.XmlDom.text;

/**
 * A permanent stat bonus applied to one equipment slot (or weapon), identified by the game's
 * enchant id.
 *
 * <p>In-game enchants come from enchanters, inscriptions, armor kits, scopes and spellthreads;
 * for the engine they are simply a slot, a name and a {@link Stats} object, which may itself
 * contain a {@link SpecialEffect} (e.g. an on-use haste boost or a proc). The catalog is a
 * process-wide static list, {@link #getAllEnchants()}, populated from {@code EnchantCache.xml}
 * by {@code rawr-data}'s enchant loader; {@link #loadEnchants(Path)} additionally merges the
 * code-defined {@link #getDefaultEnchants()} so a missing or stale cache still has the common
 * enchants and the canonical id {@code 0} ("No Enchant") entry.</p>
 *
 * <p>{@link #findEnchant(int, ItemSlot, Character)} resolves the id stored in a saved character
 * or an {@link ItemInstance}. When the id is unknown it returns {@code ALL_ENCHANTS.get(0)},
 * i.e. no enchant, rather than failing; the two-hand special case additionally lets a caller
 * querying {@link ItemSlot#OneHand} find a two-hand-only enchant. {@link #findEnchants} builds
 * the list an item picker shows for one slot; entries are filtered by the models' own
 * {@code isEnchantRelevant} checks and by {@code availableIds}, portable enchant identifiers
 * encoded as {@code -1 * (id + 10000 * slot.ordinal())} &mdash; the negative integer form saved
 * character files use for per-slot availability. Slot fitting is permissive as in the original
 * (see {@link #fitsInSlot(ItemSlot)}).</p>
 *
 * <p>{@link #getShortName()} and {@link #getReallyShortName()} are display helpers with
 * hardcoded string substitutions, kept for parity; {@code equals}/{@code hashCode} compare
 * name, id, slot and stats (the hash mixes the id and the slot ordinal). {@link #saveEnchants}
 * and {@link #loadEnchants} reproduce the {@code ArrayOfEnchant} XML, including the
 * {@code INF}/{@code -INF}/{@code NaN} special float spellings and the legacy
 * {@code "Weapon"} slot name, which maps to {@link ItemSlot#MainHand} when loading. The C#
 * static constructor's executable-relative paths are replaced by explicit {@link Path}
 * arguments; a malformed file aborts the parse at the offending entry, after which the merge
 * below repopulates everything the defaults define.</p>
 *
 * @see ItemInstance
 * @see Stats
 */
public class Enchant {
    public int id;
    public String name;
    public ItemSlot slot = ItemSlot.Head;
    public Stats stats = new Stats();

    private static final List<Enchant> ALL_ENCHANTS = new ArrayList<Enchant>();

    public Enchant() {
    }

    public Enchant(int id, String name, ItemSlot slot, Stats stats) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.stats = stats;
    }

    // C# Enchant.cs:125-131 ToString.
    @Override
    public String toString() {
        String summary = name + ": ";
        summary += stats.toString();
        int end = summary.length();
        while (end > 0 && (summary.charAt(end - 1) == ',' || summary.charAt(end - 1) == ' ' || summary.charAt(end - 1) == ':')) {
            end--;
        }
        return summary.substring(0, end);
    }

    public String getShortName() {
        String shortName = name.replace("Arcanum of the ", "").replace("Arcanum of ", "").replace("Inscription of the ", "")
                .replace("Inscription of ", "").replace("Greater", "Great").replace("Exceptional", "Excep").replace("Defense", "Def")
                .replace("Armor Kit", "").replace("Arcanum of ", "").replace(" Leg Armor", "").replace(" Scope", "")
                .replace(" Spellthread", "").replace("Lining - ", "").replace("Strength", "Str").replace("Agility", "Agi")
                .replace("Stamina", "Sta").replace("Intellect", "Int").replace("Spirit", "Spr").replace("Heavy", "Hvy")
                .replace("Jormungar Leg Reinforcements", "Jorm Reinf").replace(" Leg Reinforcements", "")
                .replace("Powerful", "Power").replace("Swiftness", "Swift").replace("Knothide", "Knot")
                .replace("Savage", "Sav").replace("Mighty Armor", "Mighty Arm").replace("Shadow Armor", "Shadow Arm")
                .replace("Attack Power", "AP").replace("Rune of the ", "").replace(" Gargoyle", "")
                .replace("speed Accelerators", "").replace(" Mysteries", "").replace(" Embroidery", "")
                .replace("Mana Restoration", "Mp5").replace("Restore Mana", "Mp5").replace("Vengeance", "Veng.")
                .replace("Reticulated Armor ", "").replace("Titanium Weapon Chain", "Titnm.W.Chn");
        return shortName.substring(0, Math.min(shortName.length(), 12));
    }

    public String getReallyShortName() {
        if (id == 0) return "None";
        String[] split = name.split(" ", -1);
        if (split.length > 1) {
            String ret = "";
            for (String s : split) {
                ret += s.substring(0, 1);
            }
            return ret;
        }
        return name.substring(0, 5);
    }

    public static List<Enchant> getAllEnchants() {
        return ALL_ENCHANTS;
    }

    /**
     * Structural slot fitting: an exact slot match, or the permissive weapon cases the original
     * allowed (a one-hand enchant also fits the off-hand, main-hand and two-hand slots; a
     * two-hand enchant also fits main-hand).
     */
    public boolean fitsInSlot(ItemSlot slot) {
        return (this.slot == slot ||
                (this.slot == ItemSlot.OneHand && (slot == ItemSlot.OffHand || slot == ItemSlot.MainHand || slot == ItemSlot.TwoHand)) ||
                (this.slot == ItemSlot.TwoHand && (slot == ItemSlot.MainHand)));
    }

    public boolean fitsInSlot(ItemSlot slot, Character character) {
        return Calculations.enchantFitsInSlot(this, character, slot);
    }

    /**
     * First enchant in catalog order with the requested id that fits the slot (see the class
     * comment for the two-hand/one-hand special case). When nothing matches, returns the first
     * catalog entry, which by convention is id {@code 0} "No Enchant"; this silent fallback is
     * deliberate, so an unknown id never throws.
     */
    public static Enchant findEnchant(int id, ItemSlot slot, Character character) {
        for (Enchant enchant : ALL_ENCHANTS) {
            if ((enchant.id == id) && (enchant.fitsInSlot(slot, character) ||
                    (enchant.slot == ItemSlot.TwoHand && slot == ItemSlot.OneHand))) {
                return enchant;
            }
        }
        return ALL_ENCHANTS.get(0);
    }

    // C# Enchant.cs:250-269
    /**
     * The enchant list for the item picker of one slot. An enchant is kept when it is relevant
     * to at least one of the supplied (character, model) pairs and fits the slot, or when the
     * queried slot is {@link ItemSlot#None}; slotless enchants ({@link ItemSlot#None}) are always
     * candidates. The per-slot availability is then enforced through {@code availableIds}, whose
     * entries encode {@code -1 * (id + 10000 * slot.ordinal())} (the first catalog entry, id 0,
     * always passes). {@code characters} and {@code models} are parallel arrays; the slot is
     * evaluated per pair.
     */
    public static List<Enchant> findEnchants(ItemSlot slot, Character[] characters, List<String> availableIds, CalculationsBase[] models) {
        List<Enchant> enchantments = new ArrayList<Enchant>();
        for (Enchant enchant : ALL_ENCHANTS) {
            boolean isRelevant = false;
            for (int i = 0; i < models.length; i++) {
                if (models[i].isEnchantRelevant(enchant, characters[i]) && (enchant.fitsInSlot(slot, characters[i]) || slot == ItemSlot.None)) {
                    isRelevant = true;
                    break;
                }
            }
            if (((isRelevant || enchant.slot == ItemSlot.None)
                    && availableIds.contains(Integer.toString(-1 * (enchant.id + (10000 * enchant.slot.ordinal())))))
                    || enchant.id == 0) {
                enchantments.add(enchant);
            }
        }
        return enchantments;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Enchant other
                && name.equals(other.name) && id == other.id && slot == other.slot
                && Stats.equalsStats(stats, other.stats);
    }

    @Override
    public int hashCode() {
        return (id << 5) | slot.ordinal();
    }

    /**
     * Serializes the whole catalog as the {@code ArrayOfEnchant} XML, writing only non-zero stat
     * values (float {@code INF}/{@code -INF}/{@code NaN} are preserved) and refusing to fail:
     * errors are swallowed as in the original, because the file is a cache, not authoritative
     * data.
     */
    public static void saveEnchants(Path path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ArrayOfEnchant");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(root);
            for (Enchant enchant : ALL_ENCHANTS) {
                Element element = doc.createElement("Enchant");
                root.appendChild(element);
                appendElement(doc, element, "Id", Integer.toString(enchant.id));
                appendElement(doc, element, "Name", enchant.name);
                appendElement(doc, element, "Slot", enchant.slot.name());
                Element statsElement = doc.createElement("Stats");
                element.appendChild(statsElement);
                writeStats(doc, statsElement, enchant.stats);
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(path)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (Exception e) {
            // C# SaveEnchants swallows serialization errors
        }
    }

    /**
     * Replaces the catalog with the parsed file and then merges {@link #getDefaultEnchants()}
     * into it: for every default, an entry with the same id, slot and name has its stats
     * replaced by the default's (or is removed when the default has {@code null} stats); a
     * default without a matching entry is appended. A missing file therefore yields exactly the
     * defaults, and a file that fails mid-parse keeps the entries read before the failure and
     * gains every default that is not among them. The legacy {@code "Weapon"} slot name maps to
     * {@link ItemSlot#MainHand}; unparseable entries are aborted by the enclosing catch.
     */
    public static void loadEnchants(Path path) {
        List<Enchant> loaded = new ArrayList<Enchant>();
        try {
            if (Files.exists(path)) {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
                Element root = doc.getDocumentElement();
                for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                    Element e = (Element) n;
                    if (!e.getTagName().equals("Enchant")) continue;
                    Enchant enchant = new Enchant();
                    enchant.id = Integer.parseInt(text(e, "Id"));
                    enchant.name = text(e, "Name");
                    String slot = text(e, "Slot");
                    if ("Weapon".equals(slot)) slot = "MainHand";
                    enchant.slot = ItemSlot.valueOf(slot);
                    Element statsElement = firstChild(e, "Stats");
                    if (statsElement != null) {
                        readStats(statsElement, enchant.stats);
                    }
                    loaded.add(enchant);
                }
            }
        } catch (Exception e) {
            // C# LoadEnchants ignores a problem with the file
        }
        ALL_ENCHANTS.clear();
        ALL_ENCHANTS.addAll(loaded);

        List<Enchant> defaultEnchants = getDefaultEnchants();
        for (int defaultEnchantIndex = 0; defaultEnchantIndex < defaultEnchants.size(); defaultEnchantIndex++) {
            boolean found = false;
            for (int allEnchantIndex = 0; allEnchantIndex < ALL_ENCHANTS.size(); allEnchantIndex++) {
                if (defaultEnchants.get(defaultEnchantIndex).id == ALL_ENCHANTS.get(allEnchantIndex).id &&
                        defaultEnchants.get(defaultEnchantIndex).slot == ALL_ENCHANTS.get(allEnchantIndex).slot &&
                        defaultEnchants.get(defaultEnchantIndex).name.equals(ALL_ENCHANTS.get(allEnchantIndex).name)) {
                    if (Stats.notEquals(defaultEnchants.get(defaultEnchantIndex).stats, ALL_ENCHANTS.get(allEnchantIndex).stats)) {
                        if (defaultEnchants.get(defaultEnchantIndex).stats == null) {
                            ALL_ENCHANTS.remove(allEnchantIndex);
                        } else {
                            ALL_ENCHANTS.get(allEnchantIndex).stats = defaultEnchants.get(defaultEnchantIndex).stats;
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (!found && defaultEnchants.get(defaultEnchantIndex).stats != null) {
                ALL_ENCHANTS.add(defaultEnchants.get(defaultEnchantIndex));
            }
        }
    }

    private static void readStats(Element statsElement, Stats stats) {
        for (Node n = statsElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            String name = e.getTagName();
            if (name.equals("SpecialEffects")) {
                readSpecialEffects(e, stats);
            } else if (name.equals("SpecialEffectCount")) {
                // implied by the number of SpecialEffect elements
            } else {
                Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
                if (property == null) {
                    throw new IllegalArgumentException("Unknown stat element: " + name);
                }
                String value = e.getTextContent().trim();
                property.mutator().set(stats, value.isEmpty() ? 0f : parseFloat(value));
            }
        }
    }

    private static void readSpecialEffects(Element specialEffectsElement, Stats stats) {
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
                        readStats(child, effect.getStats());
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

    private static void writeStats(Document doc, Element statsElement, Stats stats) {
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            float value = property.accessor().get(stats);
            if (value != 0) {
                appendElement(doc, statsElement, property.name(), formatFloat(value));
            }
        }
        if (stats.getRawSpecialEffectDataSize() > 0) {
            Element specialEffects = doc.createElement("SpecialEffects");
            statsElement.appendChild(specialEffects);
            for (SpecialEffect effect : stats.specialEffects()) {
                Element effectElement = doc.createElement("SpecialEffect");
                specialEffects.appendChild(effectElement);
                appendElement(doc, effectElement, "Trigger", effect.getTrigger().name());
                Element effectStats = doc.createElement("Stats");
                effectElement.appendChild(effectStats);
                writeStats(doc, effectStats, effect.getStats());
                appendElement(doc, effectElement, "Duration", formatFloat(effect.getDuration()));
                appendElement(doc, effectElement, "Cooldown", formatFloat(effect.getCooldown()));
                appendElement(doc, effectElement, "Chance", formatFloat(effect.getChance()));
                appendElement(doc, effectElement, "MaxStack", Integer.toString(effect.getMaxStack()));
            }
            appendElement(doc, statsElement, "SpecialEffectCount", Integer.toString(stats.getRawSpecialEffectDataSize()));
        }
    }

    private static float parseFloat(String text) {
        if (text.equals("INF")) return Float.POSITIVE_INFINITY;
        if (text.equals("-INF")) return Float.NEGATIVE_INFINITY;
        if (text.equals("NaN")) return Float.NaN;
        return Float.parseFloat(text);
    }

    private static String formatFloat(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (value == Float.POSITIVE_INFINITY) return "INF";
        if (value == Float.NEGATIVE_INFINITY) return "-INF";
        String text = Float.toString(value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }




    /**
     * The code-defined enchant catalog used when no cache is available and merged into a cache
     * that is missing entries (see {@link #loadEnchants(Path)}). Built fresh on every call, so
     * callers own the returned list. Entry 0 is always the id-0 "No Enchant"; the rest are the
     * WotLK enchantments, inscriptions, armor kits and scopes the original hardcoded. The list is
     * long and data-like; edit it only the way the original did.
     */
    public static List<Enchant> getDefaultEnchants() {
        return EnchantDefaults.getDefaultEnchants();
    }
}
