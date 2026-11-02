// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Buff.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Identity, allowed classes/professions/conflicts, set-bonus caches, lookups, the code-defined
// getDefaultBuffs() list, toString (via Stats.toString), the relevance caches and the Path-based
// saveBuffs/loadBuffs pair over the original XmlSerializer BuffCache schema. Buff.load(...) is
// unchanged (DECISIONS #19): rawr-data keeps every cache entry as loaded and getDefaultBuffs() is
// available additively, it is not merged by load. loadBuffs(Path) is the faithful C# LoadBuffs.
package rawr.base.character;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.stats.Stats;

import static rawr.base.stats.StatFactory.makeStats;
import rawr.base.util.NetFormat;

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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static rawr.base.util.XmlDom.appendElement;
import static rawr.base.util.XmlDom.firstChild;
import static rawr.base.util.XmlDom.text;

/**
 * The definition of one buff or debuff that can be active in a calculation, plus the registry of
 * every definition the engine knows.
 *
 * <p>A buff is a named game effect that contributes stats or special effects: raid buffs granted
 * by other players ("Arcane Brilliance"), consumables (flasks, elixirs, food, scrolls, potions),
 * automatic racial and profession effects ("Heroic Presence", "Toughness"), and set bonuses. A
 * {@code Buff} is only a <i>definition</i>; the {@link Character} keeps a list of the definitions
 * that are currently active and the models accumulate the stats of those. Definitions are
 * normally immutable after construction and the same instance is shared by every character.</p>
 *
 * <h2>Anatomy of a buff</h2>
 * <ul>
 *   <li>{@link #name}, {@link #group} (free-form UI category, see {@link BuffGroup}),
 *       {@link #source} (who grants it) and {@link #stats} (the stat contribution, including any
 *       {@link SpecialEffect} procs).</li>
 *   <li>{@link #improvements} - alternative versions of the same buff that supersede it when
 *       their condition holds; the condition is a profession list on the improvement (for example
 *       "Elixir of Mighty Mageblood (Mixology)" for Alchemists gives more than the base elixir).
 *       {@link #getTotalStats()} returns this buff's stats <b>including</b> those of its
 *       improvements.</li>
 *   <li>{@link #setName} / {@link #setThreshold} - set-bonus metadata: the item set the buff
 *       belongs to and the number of set pieces required.
 *       {@link Character#recalculateSetBonuses()} counts equipped pieces and activates the
 *       matching entries.</li>
 *   <li>{@link #isTargetDebuff} - the buff describes an effect on the target ("Curse of the
 *       Elements") rather than on the character, so models apply it on the enemy side.</li>
 *   <li>{@link #isCustom} - an entry loaded from the user's buff cache instead of being defined
 *       in code; custom entries survive {@link #loadBuffs(Path)} and shadow same-named
 *       defaults.</li>
 *   <li>{@link #getAllowedClasses()} / {@link #getProfessions()} /
 *       {@link #getConflictingBuffs()} - applicability and exclusivity. All three are lazily
 *       created and default to "everything": all classes, all professions, and the buff's own
 *       {@link #group} as its conflict set, so a definition only sets the lists that restrict
 *       it.</li>
 * </ul>
 *
 * <h2>Default definitions</h2>
 *
 * <p>{@link #getDefaultBuffs()} builds a fresh list of every code-defined buff through the three
 * {@link BuffDefaults} catalog builders (raid buffs and target debuffs first, then
 * consumables/food/scrolls and set bonuses, then the remaining set bonuses and temporary
 * buffs). It is a generator, not a cache: each call creates new objects. The original load path
 * is {@link #loadBuffs(Path)}, which merges custom entries from the buff cache file with these
 * defaults; the plain {@link #load(List)} installs a list wholesale, which is what rawr-data
 * uses. Both refresh the derived caches. A buff is only fully functional after a load, because
 * the name lookup and set-bonus tables are built from the installed list.</p>
 *
 * <h2>Registries and caches</h2>
 *
 * <p>All caches are process-wide static state derived from the installed list, rebuilt by
 * {@link #load(List)}: the name lookup (including improvement entries), the set-bonus-by-name
 * table, the flat set-bonus list, and the shared model-relevance filters
 * {@link #getRelevantBuffs()} / {@link #getRelevantSetBonuses()} (keyed by the registered
 * calculations class, invalidated by {@link #invalidateBuffs()}). They are safe for concurrent
 * readers once loaded, but not for concurrent mutation; the loaders run once at startup.</p>
 *
 * <h2>Serialization</h2>
 *
 * <p>{@link #saveBuffs(Path)} / {@link #loadBuffs(Path)} read and write the original
 * {@code ArrayOfBuff} XML (the {@code BuffCache.xml} schema) by hand over DOM, because the .NET
 * {@code XmlSerializer} shape could not be reproduced with a Java binding. Float values are
 * written with .NET "R" formatting via {@link NetFormat#floatR(float)}, so reloading is
 * lossless.</p>
 */
public class Buff {
    public String name;
    public String group;
    public Stats stats = new Stats();
    private List<CharacterClass> allowedClasses = null;
    private List<Profession> professions = null;
    public String setName;
    public String source;
    public int setThreshold = 0;
    public List<Buff> improvements = new ArrayList<Buff>();
    public boolean isTargetDebuff = false;
    public boolean isCustom = false;
    private List<String> conflictingBuffs = null;

    //washing virgin halo
    public Buff() {
    }

    public List<CharacterClass> getAllowedClasses() {
        if (this.allowedClasses == null) {
            this.allowedClasses = new ArrayList<CharacterClass>(Arrays.asList(
                    CharacterClass.DeathKnight,
                    CharacterClass.Druid,
                    CharacterClass.Hunter,
                    CharacterClass.Mage,
                    CharacterClass.Paladin,
                    CharacterClass.Priest,
                    CharacterClass.Rogue,
                    CharacterClass.Shaman,
                    CharacterClass.Warlock,
                    CharacterClass.Warrior));
        }
        return this.allowedClasses;
    }

    public void setAllowedClasses(List<CharacterClass> allowedClasses) {
        this.allowedClasses = allowedClasses;
    }

    public List<Profession> getProfessions() {
        if (this.professions == null) {
            this.professions = new ArrayList<Profession>(Arrays.asList(
                    Profession.None,
                    Profession.Alchemy,
                    Profession.Blacksmithing,
                    Profession.Enchanting,
                    Profession.Engineering,
                    Profession.Herbalism,
                    Profession.Inscription,
                    Profession.Jewelcrafting,
                    Profession.Leatherworking,
                    Profession.Mining,
                    Profession.Skinning,
                    Profession.Tailoring));
        }
        return this.professions;
    }

    public void setProfessions(List<Profession> professions) {
        this.professions = professions;
    }

    public List<String> getConflictingBuffs() {
        if (this.conflictingBuffs == null) {
            this.conflictingBuffs = new ArrayList<String>(Arrays.asList(this.group));
        }
        return this.conflictingBuffs;
    }

    public void setConflictingBuffs(List<String> conflictingBuffs) {
        this.conflictingBuffs = conflictingBuffs;
    }

    private static List<Buff> allBuffs = null;

    public static List<Buff> getAllBuffs() {
        return allBuffs;
    }

    /**
     * Installs the buff registry, replacing any previous list, and rebuilds every derived cache:
     * name lookup, set-bonus index and relevance filters. Called once by the rawr-data loader
     * (and by {@link #loadBuffs(Path)}).
     */
    public static void load(List<Buff> buffs) {
        allBuffs = buffs;
        allBuffsByNameCache = null;
        allSetBonuses = null;
        relevantBuffs = null;
        relevantSetBonuses = null;
        setBonusesByName.clear();
        cacheSetBonuses();
    }

    private static void cacheSetBonuses() {
        Map<String, List<Buff>> listDict = new LinkedHashMap<String, List<Buff>>();
        for (Buff buff : getAllBuffs()) {
            String setName = buff.setName;
            if (setName != null && !setName.isEmpty()) {
                List<Buff> setBonuses = listDict.get(setName);
                if (setBonuses == null) {
                    setBonuses = new ArrayList<Buff>();
                    listDict.put(setName, setBonuses);
                }
                setBonuses.add(buff);
            }
        }
        for (Map.Entry<String, List<Buff>> kvp : listDict.entrySet()) {
            setBonusesByName.put(kvp.getKey(), kvp.getValue().toArray(new Buff[0]));
        }
    }

    /**
     * Name lookup over the installed registry, including improvement entries; null when the name
     * is unknown or nothing has been loaded.
     */
    public static Buff getBuffByName(String name) {
        return allBuffsByName().get(name);
    }

    private static List<Buff> allSetBonuses = null;

    /**
     * All installed set-bonus buffs (those with a non-empty {@code setName}), in registry order.
     * Cached on first access and invalidated by {@link #load(List)}.
     */
    public static List<Buff> getAllSetBonuses() {
        if (allSetBonuses == null) {
            allSetBonuses = new ArrayList<Buff>();
            for (Buff buff : getAllBuffs()) {
                if (buff.setName != null && !buff.setName.isEmpty()) {
                    allSetBonuses.add(buff);
                }
            }
        }
        return allSetBonuses;
    }

    private static final Map<String, Buff[]> setBonusesByName = new LinkedHashMap<String, Buff[]>();

    /** The set-bonus buffs of one item set, in registry order; null when the set name is unknown. */
    public static Buff[] getSetBonuses(String setName) {
        return setBonusesByName.get(setName);
    }

    // C# Buff.cs:221-236 RelevantBuffs/InvalidateBuffs and Buff.cs:256-267 RelevantSetBonuses.
    // Both caches are keyed by the current model (C# uses Calculations.Instance.ToString()).
    private static String cachedModel = "";
    private static List<Buff> relevantBuffs = new ArrayList<Buff>();
    private static List<Buff> relevantSetBonuses = null;

    private static String modelKey(CalculationsBase instance) {
        return instance.getClass().getName();
    }

    /**
     * The installed buffs considered relevant to the currently registered model, as decided by
     * {@code Calculations.instance.isBuffRelevant(buff, null)}. Cached and keyed by the model
     * class; returns an empty list when no model is registered. Call {@link #invalidateBuffs()}
     * after changing registrations.
     */
    public static List<Buff> getRelevantBuffs() {
        CalculationsBase instance = Calculations.getInstance();
        if (instance == null || !java.util.Objects.equals(cachedModel, modelKey(instance)) || relevantBuffs == null) {
            if (instance != null) {
                cachedModel = modelKey(instance);
                List<Buff> list = new ArrayList<Buff>();
                for (Buff buff : getAllBuffs()) {
                    if (Calculations.isBuffRelevant(buff, null)) {
                        list.add(buff);
                    }
                }
                relevantBuffs = list;
            } else {
                relevantBuffs = new ArrayList<Buff>();
            }
        }
        return relevantBuffs;
    }

    /** Drops the model-relevance caches; the next read rebuilds them for the current model. */
    public static void invalidateBuffs() {
        relevantBuffs = null;
    }

    /**
     * Like {@link #getRelevantBuffs()} but restricted to set bonuses whose stats are relevant to
     * the current model ({@code Calculations.hasRelevantStats} plus a non-empty set name).
     */
    public static List<Buff> getRelevantSetBonuses() {
        CalculationsBase instance = Calculations.getInstance();
        if (instance == null || !java.util.Objects.equals(cachedModel, modelKey(instance)) || relevantSetBonuses == null) {
            if (instance != null) {
                cachedModel = modelKey(instance);
                List<Buff> list = new ArrayList<Buff>();
                for (Buff buff : getAllBuffs()) {
                    if (Calculations.hasRelevantStats(buff.getTotalStats()) && buff.setName != null && !buff.setName.isEmpty()) {
                        list.add(buff);
                    }
                }
                relevantSetBonuses = list;
            } else {
                relevantSetBonuses = new ArrayList<Buff>();
            }
        }
        return relevantSetBonuses;
    }

    private static Map<String, Buff> allBuffsByNameCache = null;

    private static Map<String, Buff> allBuffsByName() {
        if (allBuffsByNameCache == null) {
            allBuffsByNameCache = new LinkedHashMap<String, Buff>();
            for (Buff buff : getAllBuffs()) {
                if (!allBuffsByNameCache.containsKey(buff.name)) {
                    allBuffsByNameCache.put(buff.name, buff);
                    for (Buff improvement : buff.improvements) {
                        if (!allBuffsByNameCache.containsKey(improvement.name)) {
                            allBuffsByNameCache.put(improvement.name, improvement);
                        }
                    }
                }
            }
        }
        return allBuffsByNameCache;
    }

    /**
     * A fresh {@link Stats} holding this buff's stats plus the stats of all its improvements,
     * accumulated in list order. Allocates on every call; the result also carries the special
     * effects of both levels.
     */
    public Stats getTotalStats() {
        Stats ret = this.stats.copy();
        for (Buff buff : this.improvements)
            ret.accumulate(buff.stats);
        return ret;
    }

    /**
     * Builds and returns the full code-defined buff catalogue as new objects. A generator (each
     * call constructs fresh buffs), used by {@link #loadBuffs(Path)} to merge defaults with the
     * persisted custom entries.
     */
    public static List<Buff> getDefaultBuffs() {
        List<Buff> defaultBuffs = new ArrayList<Buff>();
        BuffDefaults.getDefaultBuffs1(defaultBuffs);
        BuffDefaults.getDefaultBuffs2(defaultBuffs);
        BuffDefaults.getDefaultBuffs3(defaultBuffs);
        return defaultBuffs;
    }


    // C# Buff.cs:202-207 ToString.
    /**
     * Display form: the buff name, a colon and the stat summary from {@link Stats#toString()},
     * with trailing separators trimmed. Used by charts and tooltips.
     */
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

    // C# Buff.cs:121-135 SaveBuffs to Data/BuffCache.xml, with the executable directory replaced
    // by an explicit path. Schema is the XmlSerializer BuffList mapping.
    /**
     * Writes the installed registry to the {@code ArrayOfBuff} XML cache file. Serialization
     * errors are swallowed (the original did the same); a missing directory or an unwritable path
     * therefore fails silently.
     */
    public static void saveBuffs(Path path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ArrayOfBuff");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(root);
            List<Buff> buffs = getAllBuffs();
            if (buffs != null) {
                for (Buff buff : buffs) {
                    writeBuff(doc, root, buff);
                }
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(path)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (Exception e) {
            // C# SaveBuffs swallows serialization errors
        }
    }

    // C# Buff.cs:137-177 LoadBuffs: keep the custom entries from the file, then append every
    // code-defined default that is not already present, and refresh the registries.
    /**
     * Loads the buff registry the original way: keep the custom entries from the cache file
     * (a missing or unreadable file is tolerated) and append every code-defined default whose
     * name is not already present, then refresh the caches. A duplicate custom name fails inside
     * and leaves the current registry untouched.
     */
    public static void loadBuffs(Path path) {
        try {
            List<Buff> loadedBuffs = new ArrayList<Buff>();
            if (Files.exists(path)) {
                try {
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
                    Element root = doc.getDocumentElement();
                    for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                        if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                        Element e = (Element) n;
                        if (e.getTagName().equals("Buff")) {
                            loadedBuffs.add(parseBuff(e));
                        }
                    }
                } catch (Exception e) {
                    // C# shows an incompatibility message and falls back to the defaults
                }
            }

            Map<String, Buff> allBuffs = new LinkedHashMap<String, Buff>();
            for (Buff loadedBuff : loadedBuffs) {
                if (loadedBuff.isCustom) {
                    if (allBuffs.containsKey(loadedBuff.name)) {
                        throw new IllegalArgumentException("An item with the same key has already been added: " + loadedBuff.name);
                    }
                    allBuffs.put(loadedBuff.name, loadedBuff);
                }
            }
            for (Buff defaultBuff : getDefaultBuffs()) {
                if (!allBuffs.containsKey(defaultBuff.name)) {
                    allBuffs.put(defaultBuff.name, defaultBuff);
                }
            }
            load(new ArrayList<Buff>(allBuffs.values()));
        } catch (Exception e) {
            // C# outer catch: leave the current registry untouched
        }
    }

    private static void writeBuff(Document doc, Element parent, Buff buff) {
        Element element = doc.createElement("Buff");
        parent.appendChild(element);
        if (buff.name != null) {
            appendElement(doc, element, "Name", buff.name);
        }
        if (buff.group != null) {
            appendElement(doc, element, "Group", buff.group);
        }
        Element statsElement = doc.createElement("Stats");
        element.appendChild(statsElement);
        writeStats(doc, statsElement, buff.stats);
        if (buff.setName != null) {
            appendElement(doc, element, "SetName", buff.setName);
        }
        if (buff.source != null) {
            appendElement(doc, element, "Source", buff.source);
        }
        appendElement(doc, element, "SetThreshold", Integer.toString(buff.setThreshold));
        Element improvements = doc.createElement("Improvements");
        element.appendChild(improvements);
        for (Buff improvement : buff.improvements) {
            writeBuff(doc, improvements, improvement);
        }
        appendElement(doc, element, "IsTargetDebuff", Boolean.toString(buff.isTargetDebuff));
        appendElement(doc, element, "IsCustom", Boolean.toString(buff.isCustom));
        Element allowedClasses = doc.createElement("AllowedClasses");
        element.appendChild(allowedClasses);
        for (CharacterClass characterClass : buff.getAllowedClasses()) {
            appendElement(doc, allowedClasses, "CharacterClass", characterClass.name());
        }
        Element professions = doc.createElement("Professions");
        element.appendChild(professions);
        for (Profession profession : buff.getProfessions()) {
            appendElement(doc, professions, "Profession", profession.name());
        }
        Element conflictingBuffs = doc.createElement("ConflictingBuffs");
        element.appendChild(conflictingBuffs);
        for (String conflict : buff.getConflictingBuffs()) {
            if (conflict == null) {
                Element nil = doc.createElement("string");
                nil.setAttribute("xsi:nil", "true");
                conflictingBuffs.appendChild(nil);
            } else {
                appendElement(doc, conflictingBuffs, "string", conflict);
            }
        }
    }

    private static Buff parseBuff(Element e) {
        Buff buff = new Buff();
        Element statsElement = firstChild(e, "Stats");
        if (statsElement != null) {
            readStats(statsElement, buff.stats);
        }
        String name = text(e, "Name");
        if (name != null) buff.name = name;
        String group = text(e, "Group");
        if (group != null) buff.group = group;
        String setName = text(e, "SetName");
        if (setName != null) buff.setName = setName;
        String source = text(e, "Source");
        if (source != null) buff.source = source;
        String setThreshold = text(e, "SetThreshold");
        if (setThreshold != null) buff.setThreshold = Integer.parseInt(setThreshold);
        String isTargetDebuff = text(e, "IsTargetDebuff");
        if (isTargetDebuff != null) buff.isTargetDebuff = Boolean.parseBoolean(isTargetDebuff);
        String isCustom = text(e, "IsCustom");
        if (isCustom != null) buff.isCustom = Boolean.parseBoolean(isCustom);

        Element improvements = firstChild(e, "Improvements");
        if (improvements != null) {
            for (Node n = improvements.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE && ((Element) n).getTagName().equals("Buff")) {
                    buff.improvements.add(parseBuff((Element) n));
                }
            }
        }
        Element allowedClasses = firstChild(e, "AllowedClasses");
        if (allowedClasses != null) {
            List<CharacterClass> classes = new ArrayList<CharacterClass>();
            for (Node n = allowedClasses.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    classes.add(CharacterClass.valueOf(n.getTextContent().trim()));
                }
            }
            if (!classes.isEmpty()) buff.setAllowedClasses(classes);
        }
        Element professions = firstChild(e, "Professions");
        if (professions != null) {
            List<Profession> professionList = new ArrayList<Profession>();
            for (Node n = professions.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    professionList.add(Profession.valueOf(n.getTextContent().trim()));
                }
            }
            if (!professionList.isEmpty()) buff.setProfessions(professionList);
        }
        Element conflictingBuffs = firstChild(e, "ConflictingBuffs");
        if (conflictingBuffs != null) {
            List<String> conflicts = new ArrayList<String>();
            for (Node n = conflictingBuffs.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element conflict = (Element) n;
                    if ("true".equals(conflict.getAttribute("xsi:nil"))) {
                        conflicts.add(null);
                    } else {
                        conflicts.add(conflict.getTextContent());
                    }
                }
            }
            if (!conflicts.isEmpty()) buff.setConflictingBuffs(conflicts);
        }
        return buff;
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
        if (stats.getRawSpecialEffectData() != null) {
            Element specialEffects = doc.createElement("SpecialEffects");
            statsElement.appendChild(specialEffects);
            for (SpecialEffect effect : stats.specialEffects()) {
                Element effectElement = doc.createElement("SpecialEffect");
                specialEffects.appendChild(effectElement);
                appendElement(doc, effectElement, "Trigger", effect.getTrigger().name());
                Element effectStats = doc.createElement("Stats");
                effectElement.appendChild(effectStats);
                writeStats(doc, effectStats, effect.getStats());
                appendElement(doc, effectElement, "Duration", NetFormat.floatR(effect.getDuration()));
                appendElement(doc, effectElement, "Cooldown", NetFormat.floatR(effect.getCooldown()));
                appendElement(doc, effectElement, "Chance", NetFormat.floatR(effect.getChance()));
                appendElement(doc, effectElement, "MaxStack", Integer.toString(effect.getMaxStack()));
            }
        }
        if (stats.getRawSpecialEffectDataSize() != 0) {
            appendElement(doc, statsElement, "SpecialEffectCount", Integer.toString(stats.getRawSpecialEffectDataSize()));
        }
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            float value = property.accessor().get(stats);
            if (value != 0) {
                appendElement(doc, statsElement, property.name(), NetFormat.floatR(value));
            }
        }
    }

    private static float parseFloat(String text) {
        if (text.equals("INF")) return Float.POSITIVE_INFINITY;
        if (text.equals("-INF")) return Float.NEGATIVE_INFINITY;
        if (text.equals("NaN")) return Float.NaN;
        return Float.parseFloat(text);
    }



}
