// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Character.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: item slots, gem counting, gem-requirement flags, buffs, talents for every class,
// professions, BossOptions (field, clone, fast-copy and save-side XML) and the save-side
// XmlSerializer mapping (the load side lives in rawr-data/CharacterFixtureLoader).
// Gemming templates (CurrentGemmingTemplates/SaveGemmingTemplateOverrides/GenerateGemmingTemplates),
// item-filter overrides (SaveItemFilterEnabledOverride/LoadItemFilterEnabledOverride) and the
// relevant-items cluster (InvalidateItemInstances/GetRelevantItemInstances/ClearRelevantGems/
// GetRelevantItems/CustomItemInstances) are ported. ArmoryPets is omitted (network import).
// Exports.cs (clipboard/loot-rank) is UI and omitted.
package rawr.base.character;

import rawr.base.bosses.BossOptions;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemAvailability;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterEnabledOverride;
import rawr.base.items.ItemFilterRegex;
import rawr.base.items.ItemFilterRegexList;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.optimizer.ItemOptimizer;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.talents.DruidTalents;
import rawr.base.talents.HunterTalents;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.TalentsBase;
import rawr.base.talents.WarlockTalents;
import rawr.base.talents.WarriorTalents;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static rawr.base.util.XmlDom.appendElementIfNotNull;

/**
 * The player character: the hub value object that ties together gear, buffs, talents,
 * professions, the boss selection and per-model options, and the input to every model
 * calculation.
 *
 * <p>A {@code Character} is what the user edits and what the engine consumes: a model turns it,
 * plus the item database, into ratings such as DPS or HPS. It holds no per-model simulation
 * state; the simulation lives in a {@code CalculationsBase} subclass selected by
 * {@link #getCurrentModel()} / {@link #getCurrentCalculations()}. The class is entirely
 * per-instance state; only static final constants (slot counts and the XML slot order) exist.</p>
 *
 * <h2>Gear slots and gemmed ids</h2>
 *
 * <p>Gear is an {@link ItemInstance} array of {@link #SlotCount} = 21 entries indexed by
 * {@link CharacterSlot#value()} (0 = projectile through 20 = shirt); the first
 * {@link #OptimizableSlotCount} = 19 are the ones the optimizer may replace, while tabard and
 * shirt are never optimized. Every equipped item is identified by its "gemmed id" string
 * {@code itemId.gem1.gem2.gem3.enchantId}. Saving writes the slots in {@code XML_SLOT_ORDER} (the
 * C# declaration order: head, neck, shoulders, back, chest, shirt, tabard, wrist, hands, waist,
 * legs, feet, finger1, finger2, trinket1, trinket2, main hand, off hand, ranged, projectile,
 * projectile bag), so old files round-trip with the original element ordering. Setting a slot or
 * an enchant invalidates the character (see {@link #onCalculationsInvalidated()}).</p>
 *
 * <h2>Active buffs</h2>
 *
 * <p>{@link #getActiveBuffs()} is a list of {@link Buff} definitions, mixing three kinds of
 * entries: buffs the user selected, buffs required by race or profession
 * ({@link #validateActiveBuffs()}: Heroic Presence for Draenei, Toughness for Mining, Master of
 * Anatomy for Skinning), and set bonuses implied by equipped gear
 * ({@link #recalculateSetBonuses()}: count the equipped pieces per item-set name, then activate
 * every set-bonus buff whose {@code setThreshold} is satisfied). Conflicts are expressed by the
 * buffs themselves ({@link Buff#getConflictingBuffs()}), not here.
 * {@link #getDisableBuffAutoActivation()} lets a model freeze the list while it explores
 * variations without triggering buff re-selection.</p>
 *
 * <h2>Talents</h2>
 *
 * <p>Talents are kept in one lazily created field per class ({@code mageTalents}, ...); reading
 * a talent field of an otherwise untouched character creates an empty build rather than returning
 * null. {@link #getCurrentTalents()} returns the one matching {@link #getCharacterClass()}, and
 * {@link #assignAllTalentsFromCharacter(Character, boolean)} copies or shares all ten at once
 * (the flag selects deep copy).</p>
 *
 * <h2>Professions, boss and options</h2>
 *
 * <p>{@link #getPrimaryProfession()} / {@link #getSecondaryProfession()} gate profession buffs
 * and restrictions; their setters call {@code Calculations.updateProfessions(this)} and re-run
 * {@link #validateActiveBuffs()}. The encounter selection is a lazily created
 * {@link BossOptions}; the XML-facing accessors ({@link #getSerializableBoss()} /
 * {@link #setSerializableBoss(BossOptions)}) clone on assignment the way the original property
 * did, while {@link #setBossOptions(BossOptions)} stores the reference. Per-model options
 * objects live in the map behind {@link #getCalculationOptions()}, keyed by
 * {@link #getCurrentModel()}; on first use they are deserialized from the XML strings in the
 * serialized-options dictionary and re-bound to this character, and
 * {@link #serializeCalculationOptions()} writes them back.</p>
 *
 * <h2>Caches and invalidation</h2>
 *
 * <p>Several derived values are cached and cleared together by
 * {@link #onCalculationsInvalidated()}: the gem counts and gem-requirement violation counters,
 * the item-set counter map, the per-slot relevant-item and relevant-instance caches, and the
 * per-model gemming templates. Any mutation calls the invalidator, which first marks the gem
 * counts stale and re-creates the item instances, then, unless the {@code isLoading} flag is set,
 * recomputes set and passive bonuses and notifies the registered
 * {@link #addCalculationsInvalidatedListener(Runnable)} listeners. During a character-file load
 * {@code isLoading} suppresses the recomputation so the loader can install gear first and
 * validate once at the end. "Invalidating" an item instance means replacing it with a fresh
 * equal object, because object identity is used by other caches.</p>
 *
 * <h2>Save/load and copies</h2>
 *
 * <p>This class owns the save side only: {@link #save(Path)} serializes to the original
 * {@code Character} XML schema over DOM (a Java binding could not reproduce the .NET
 * {@code XmlSerializer} element order and default handling). The load side lives in
 * {@code rawr-data}. {@link #clone()} deep-copies item instances but shares the talents, boss
 * options, calculation-options maps and the {@code Buff} definitions; it does not copy faction,
 * the invalidation listener list, or the optimizer-related collection fields (available items,
 * custom item instances, gemming templates, item-filter overrides). The Blacksmithing socket
 * flags and the calculation-to-optimize/requirements settings are copied, matching the C# clone.
 * The constructors
 * {@link #Character(Character, Object[], int)} and {@link #initializeCharacter(Object[], int)}
 * are the optimizer's fast paths: they graft a replacement item array onto a base character
 * without re-creating anything else.</p>
 */
public class Character {
    public static final int SlotCount = 21;
    public static final int OptimizableSlotCount = 19;

    // Declaration order of the gemmed-id properties in the C# class (Character.cs:95-136).
    private static final CharacterSlot[] XML_SLOT_ORDER = new CharacterSlot[] {
        CharacterSlot.Head, CharacterSlot.Neck, CharacterSlot.Shoulders, CharacterSlot.Back,
        CharacterSlot.Chest, CharacterSlot.Shirt, CharacterSlot.Tabard, CharacterSlot.Wrist,
        CharacterSlot.Hands, CharacterSlot.Waist, CharacterSlot.Legs, CharacterSlot.Feet,
        CharacterSlot.Finger1, CharacterSlot.Finger2, CharacterSlot.Trinket1, CharacterSlot.Trinket2,
        CharacterSlot.MainHand, CharacterSlot.OffHand, CharacterSlot.Ranged, CharacterSlot.Projectile,
        CharacterSlot.ProjectileBag,
    };

    ItemInstance[] equippedItems = new ItemInstance[SlotCount];

    private String name;
    private String realm;
    private CharacterRegion region = CharacterRegion.US;
    private CharacterRace race = CharacterRace.NightElf;
    private CharacterFaction faction = CharacterFaction.Alliance;
    private CharacterClass characterClass = CharacterClass.Druid;
    private String currentModel = "";

    private boolean enforceGemRequirements = true;

    private Profession primaryProfession = Profession.None;
    private Profession secondaryProfession = Profession.None;

    // C# Character.cs:310-312; consumed by the optimizer (Optimizer.cs).
    private String calculationToOptimize;
    private List<OptimizationRequirement> optimizationRequirements;

    private List<Buff> activeBuffs = new ArrayList<Buff>();
    private Map<String, Integer> setBonusCount = null;

    // C# Character.cs:519-521. The XmlSerializer-only load flag also guards item-instance
    // invalidation while loading (C# SetGemmedId does not invalidate at all).
    private boolean isLoading;

    private MageTalents mageTalents = null;

    // C# Character.cs:428 lazy-creating WarriorTalents property (added for the ProtWarr model).
    private WarriorTalents warriorTalents = null;

    // C# Character.cs:428 lazy-creating ShamanTalents property (added for the RestoSham model).
    private ShamanTalents shamanTalents = null;

    // C# Character.cs:436 lazy-creating DruidTalents property (added for the Cat model).
    private DruidTalents druidTalents = null;

    // C# Character.cs:433 lazy-creating PaladinTalents property (added for the Healadin model).
    private PaladinTalents paladinTalents = null;

    // C# Character.cs:426 lazy-creating PriestTalents property (added for the HealPriest model).
    private PriestTalents priestTalents = null;

    // C# Character.cs:422 lazy-creating HunterTalents property (added for the Hunter model).
    private HunterTalents hunterTalents = null;

    // C# Character.cs:424 lazy-creating RogueTalents property (added for the Rogue model).
    private RogueTalents rogueTalents = null;

    // C# Character.cs:436 lazy-creating DeathKnightTalents property (added for the TankDK model).
    private DeathKnightTalents deathKnightTalents = null;

    // C# Character.cs:433 lazy-creating WarlockTalents property (added for the Warlock model).
    private WarlockTalents warlockTalents = null;
    private BossOptions bossOptions = null;
    private LinkedHashMap<String, String> serializedCalculationOptions;
    private Map<String, ICalculationOptionBase> calculationOptions = new LinkedHashMap<String, ICalculationOptionBase>();
    private List<String> activeBuffsXml = null;
    private boolean disableBuffAutoActivation = false;
    private final List<Runnable> calculationsInvalidatedListeners = new ArrayList<Runnable>();

    // C# Character.cs:139-140/1172: the available-item restriction strings, itemid[.gem1.gem2.gem3[.enchant]]
    private List<String> availableItems = new ArrayList<String>();

    // C# Character.cs:825-826/947-951: custom item-instance list and the per-slot relevant caches.
    private List<ItemInstance> customItemInstances = new ArrayList<ItemInstance>();
    private Map<CharacterSlot, List<ItemInstance>> relevantItemInstances = new LinkedHashMap<CharacterSlot, List<ItemInstance>>();
    private Map<CharacterSlot, List<Item>> relevantItems = new LinkedHashMap<CharacterSlot, List<Item>>();

    // C# Character.cs:147-167 Gemming Templates region (GemmingTemplateOverrides is kept so
    // CurrentGemmingTemplates can apply it exactly like the original).
    private List<GemmingTemplate> customGemmingTemplates = new ArrayList<GemmingTemplate>();
    private List<GemmingTemplate> gemmingTemplateOverrides = new ArrayList<GemmingTemplate>();
    private String gemmingTemplateModel;
    private List<GemmingTemplate> currentGemmingTemplates;

    // C# Character.cs:222. Like the auto-property it stays null when the saved file has no element;
    // Save() rebuilds it from the global ItemFilter state (C# SaveItemFilterEnabledOverride).
    private List<ItemFilterEnabledOverride> itemFilterEnabledOverride;

    // C# Character.cs:840-884 blacksmithing socket flags
    private boolean waistBlacksmithingSocketEnabled;
    private boolean handsBlacksmithingSocketEnabled;
    private boolean wristBlacksmithingSocketEnabled;

    // cache gem counts as this takes the most time of accumulating item stats
    private boolean gemCountValid;
    private int redGemCount;
    private int yellowGemCount;
    private int blueGemCount;
    private int jewelersGemCount;
    private int gemRequirementsInvalid;
    private int nonjewelerGemRequirementsInvalid;

    public Character() {
    }

    public MageTalents getMageTalents() {
        if (this.mageTalents == null) {
            this.mageTalents = new MageTalents();
        }
        return this.mageTalents;
    }

    public void setMageTalents(MageTalents mageTalents) {
        this.mageTalents = mageTalents;
    }

    public WarriorTalents getWarriorTalents() {
        if (this.warriorTalents == null) {
            this.warriorTalents = new WarriorTalents();
        }
        return this.warriorTalents;
    }

    public void setWarriorTalents(WarriorTalents warriorTalents) {
        this.warriorTalents = warriorTalents;
    }

    public ShamanTalents getShamanTalents() {
        if (this.shamanTalents == null) {
            this.shamanTalents = new ShamanTalents();
        }
        return this.shamanTalents;
    }

    public void setShamanTalents(ShamanTalents shamanTalents) {
        this.shamanTalents = shamanTalents;
    }

    public DruidTalents getDruidTalents() {
        if (this.druidTalents == null) {
            this.druidTalents = new DruidTalents();
        }
        return this.druidTalents;
    }

    public void setDruidTalents(DruidTalents druidTalents) {
        this.druidTalents = druidTalents;
    }

    public PaladinTalents getPaladinTalents() {
        if (this.paladinTalents == null) {
            this.paladinTalents = new PaladinTalents();
        }
        return this.paladinTalents;
    }

    public void setPaladinTalents(PaladinTalents paladinTalents) {
        this.paladinTalents = paladinTalents;
    }

    public PriestTalents getPriestTalents() {
        if (this.priestTalents == null) {
            this.priestTalents = new PriestTalents();
        }
        return this.priestTalents;
    }

    public void setPriestTalents(PriestTalents priestTalents) {
        this.priestTalents = priestTalents;
    }

    public HunterTalents getHunterTalents() {
        if (this.hunterTalents == null) {
            this.hunterTalents = new HunterTalents();
        }
        return this.hunterTalents;
    }

    public void setHunterTalents(HunterTalents hunterTalents) {
        this.hunterTalents = hunterTalents;
    }

    public RogueTalents getRogueTalents() {
        if (this.rogueTalents == null) {
            this.rogueTalents = new RogueTalents();
        }
        return this.rogueTalents;
    }

    public void setRogueTalents(RogueTalents rogueTalents) {
        this.rogueTalents = rogueTalents;
    }

    public DeathKnightTalents getDeathKnightTalents() {
        if (this.deathKnightTalents == null) {
            this.deathKnightTalents = new DeathKnightTalents();
        }
        return this.deathKnightTalents;
    }

    public void setDeathKnightTalents(DeathKnightTalents deathKnightTalents) {
        this.deathKnightTalents = deathKnightTalents;
    }

    public WarlockTalents getWarlockTalents() {
        if (this.warlockTalents == null) {
            this.warlockTalents = new WarlockTalents();
        }
        return this.warlockTalents;
    }

    public void setWarlockTalents(WarlockTalents warlockTalents) {
        this.warlockTalents = warlockTalents;
    }

    // C# Character.cs:439-478 CurrentTalents. The switch over every Class is ported (the DPSWarr
    // model checks `CurrentTalents is WarriorTalents` for Titan's Grip off-hand handling).
    /**
     * The typed talents object for {@link #getCharacterClass()}, creating an empty one on first
     * access. The returned object is one of the per-class fields (never a copy), so mutating it
     * mutates the character.
     */
    public TalentsBase getCurrentTalents() {
        switch (characterClass) {
            case Warrior: return getWarriorTalents();
            case Paladin: return getPaladinTalents();
            case Hunter: return getHunterTalents();
            case Rogue: return getRogueTalents();
            case Priest: return getPriestTalents();
            case Shaman: return getShamanTalents();
            case Mage: return getMageTalents();
            case Warlock: return getWarlockTalents();
            case Druid: return getDruidTalents();
            case DeathKnight: return getDeathKnightTalents();
            default: return getDruidTalents();
        }
    }

    // C# Character.cs:354-366 SerializableBoss/BossOptions.
    /**
     * The XML-facing view of the boss selection: reading is the same as {@link #getBossOptions()};
     * writing replaces the options with a clone of the argument, matching the original serializer
     * property.
     */
    public BossOptions getSerializableBoss() {
        return getBossOptions();
    }

    public void setSerializableBoss(BossOptions value) {
        setBossOptions(value.clone());
    }

    public BossOptions getBossOptions() {
        if (bossOptions == null) {
            bossOptions = new BossOptions();
        }
        return bossOptions;
    }

    public void setBossOptions(BossOptions value) {
        bossOptions = value;
    }

    /**
     * Stores a talents object in the matching per-class field. A null value clears the Mage field
     * (the historical behaviour); every concrete {@code *Talents} type is handled so the optimizer
     * can select and mutate specs for all ten classes.
     */
    public void setCurrentTalents(TalentsBase value) {
        if (value == null) {
            this.mageTalents = null;
        } else if (value instanceof MageTalents mageTalents) {
            this.mageTalents = mageTalents;
        } else if (value instanceof WarriorTalents warriorTalents) {
            this.warriorTalents = warriorTalents;
        } else if (value instanceof ShamanTalents shamanTalents) {
            this.shamanTalents = shamanTalents;
        } else if (value instanceof DruidTalents druidTalents) {
            this.druidTalents = druidTalents;
        } else if (value instanceof PaladinTalents paladinTalents) {
            this.paladinTalents = paladinTalents;
        } else if (value instanceof PriestTalents priestTalents) {
            this.priestTalents = priestTalents;
        } else if (value instanceof HunterTalents hunterTalents) {
            this.hunterTalents = hunterTalents;
        } else if (value instanceof RogueTalents rogueTalents) {
            this.rogueTalents = rogueTalents;
        } else if (value instanceof DeathKnightTalents deathKnightTalents) {
            this.deathKnightTalents = deathKnightTalents;
        } else if (value instanceof WarlockTalents warlockTalents) {
            this.warlockTalents = warlockTalents;
        }
    }

    // C# Character.cs:1129-1165 AssignAllTalentsFromCharacter. PARTIAL: Mage, Warrior, Shaman, Druid, Paladin, Priest, Hunter, Rogue, DeathKnight and Warlock only.
    /**
     * Copies all per-class talents from another character. With {@code clone} true each non-null
     * build is deep-copied; otherwise the source's objects are shared by reference. Used by
     * {@link #clone()} and by the optimizer when it builds working copies.
     */
    public void assignAllTalentsFromCharacter(Character character, boolean clone) {
        if (clone) {
            this.mageTalents = character.mageTalents != null ? character.mageTalents.clone() : null;
            this.warriorTalents = character.warriorTalents != null ? character.warriorTalents.clone() : null;
            this.shamanTalents = character.shamanTalents != null ? character.shamanTalents.clone() : null;
            this.druidTalents = character.druidTalents != null ? character.druidTalents.clone() : null;
            this.paladinTalents = character.paladinTalents != null ? character.paladinTalents.clone() : null;
            this.priestTalents = character.priestTalents != null ? character.priestTalents.clone() : null;
            this.hunterTalents = character.hunterTalents != null ? character.hunterTalents.clone() : null;
            this.rogueTalents = character.rogueTalents != null ? character.rogueTalents.clone() : null;
            this.deathKnightTalents = character.deathKnightTalents != null ? character.deathKnightTalents.clone() : null;
            this.warlockTalents = character.warlockTalents != null ? character.warlockTalents.clone() : null;
        } else {
            this.mageTalents = character.mageTalents;
            this.warriorTalents = character.warriorTalents;
            this.shamanTalents = character.shamanTalents;
            this.druidTalents = character.druidTalents;
            this.paladinTalents = character.paladinTalents;
            this.priestTalents = character.priestTalents;
            this.hunterTalents = character.hunterTalents;
            this.rogueTalents = character.rogueTalents;
            this.deathKnightTalents = character.deathKnightTalents;
            this.warlockTalents = character.warlockTalents;
        }
    }

    public String getCalculationToOptimize() {
        return calculationToOptimize;
    }

    public void setCalculationToOptimize(String value) {
        calculationToOptimize = value;
    }

    public List<OptimizationRequirement> getOptimizationRequirements() {
        return optimizationRequirements;
    }

    public void setOptimizationRequirements(List<OptimizationRequirement> value) {
        optimizationRequirements = value;
    }

    public LinkedHashMap<String, String> getSerializedCalculationOptions() {
        return this.serializedCalculationOptions;
    }

    public void setSerializedCalculationOptions(LinkedHashMap<String, String> serializedCalculationOptions) {
        this.serializedCalculationOptions = serializedCalculationOptions;
    }

    // C# SerializeCalculationOptions (Character.cs:1443): serialize the current model's options into
    // the dictionary that Save() writes out.
    /**
     * Serializes the current model's options into the dictionary that {@link #save(Path)} writes,
     * under the model's name. Called as the first step of a save; a no-op when the model has no
     * options object.
     */
    public void serializeCalculationOptions() {
        ICalculationOptionBase options = getCalculationOptions();
        if (options != null) {
            if (this.serializedCalculationOptions == null) {
                this.serializedCalculationOptions = new LinkedHashMap<String, String>();
            }
            this.serializedCalculationOptions.put(getCurrentModel(), options.getXml());
        }
    }

    // C# Save(string path) (Character.cs:2285). The RAWR3 Save(Stream) overload is Silverlight-only
    // and omitted; the two save-preparation calls run in the original order.
    /**
     * Writes this character as the original {@code Character} XML file. Before serializing it
     * refreshes the derived data that is part of the file: the current model's options, the
     * gemming-template and item-filter overrides, and the flat list of active buff names. The
     * matching reader is in {@code rawr-data}.
     *
     * @throws IOException if the target file cannot be created or written
     */
    public void save(Path path) throws IOException {
        serializeCalculationOptions();
        saveGemmingTemplateOverrides();
        saveItemFilterEnabledOverride();
        activeBuffsXml = new ArrayList<String>();
        for (Buff buff : this.activeBuffs) {
            activeBuffsXml.add(buff.name);
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("Character");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(root);
            writeXml(doc, root);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(path)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to save character to " + path, e);
        }
    }

    // XmlSerializer members in declaration order, restricted to the members this slice ports
    // (Character.cs fields at 14-29/139-144 and the slot/talent/profession properties). Null strings
    // are omitted exactly like XmlSerializer; value types are always written.
    private void writeXml(Document doc, Element root) {
        appendElementIfNotNull(doc, root, "Name", name);
        appendElementIfNotNull(doc, root, "Realm", realm);
        appendElementIfNotNull(doc, root, "Region", region.name());
        appendElementIfNotNull(doc, root, "Race", race.name());
        appendElementIfNotNull(doc, root, "Class", characterClass.name());
        if (activeBuffsXml != null) {
            for (String buffName : activeBuffsXml) {
                appendElementIfNotNull(doc, root, "ActiveBuffs", buffName);
            }
        }
        if (availableItems != null) {
            for (String availableItem : availableItems) {
                appendElementIfNotNull(doc, root, "AvailableItems", availableItem);
            }
        }
        appendElementIfNotNull(doc, root, "CurrentModel", currentModel);
        appendElementIfNotNull(doc, root, "EnforceMetagemRequirements", Boolean.toString(enforceGemRequirements));
        appendElementIfNotNull(doc, root, "CalculationToOptimize", calculationToOptimize);
        appendOptimizationRequirements(doc, root);
        if (this.serializedCalculationOptions != null) {
            Element optionsElement = doc.createElement("CalculationOptions");
            root.appendChild(optionsElement);
            rawr.base.util.XmlMap.writeStringMap(doc, optionsElement, this.serializedCalculationOptions);
        }
        for (CharacterSlot slot : XML_SLOT_ORDER) {
            ItemInstance item = equippedItems[slot.value()];
            if (item != null) {
                appendElementIfNotNull(doc, root, slot.name(), item.getGemmedId());
            }
        }
        appendGemmingTemplates(doc, root, "CustomGemmingTemplates", customGemmingTemplates);
        appendGemmingTemplates(doc, root, "GemmingTemplateOverrides", gemmingTemplateOverrides);
        appendItemFilterOverrides(doc, root, itemFilterEnabledOverride);
        Element bossElement = doc.createElement("Boss");
        root.appendChild(bossElement);
        getBossOptions().writeXml(doc, bossElement);
        appendElementIfNotNull(doc, root, "MageTalents", (this.mageTalents != null ? this.mageTalents : new MageTalents()).toString());
        appendElementIfNotNull(doc, root, "WarriorTalents", (this.warriorTalents != null ? this.warriorTalents : new WarriorTalents()).toString());
        appendElementIfNotNull(doc, root, "DruidTalents", (this.druidTalents != null ? this.druidTalents : new DruidTalents()).toString());
        appendElementIfNotNull(doc, root, "PaladinTalents", (this.paladinTalents != null ? this.paladinTalents : new PaladinTalents()).toString());
        appendElementIfNotNull(doc, root, "PriestTalents", (this.priestTalents != null ? this.priestTalents : new PriestTalents()).toString());
        appendElementIfNotNull(doc, root, "HunterTalents", (this.hunterTalents != null ? this.hunterTalents : new HunterTalents()).toString());
        appendElementIfNotNull(doc, root, "RogueTalents", (this.rogueTalents != null ? this.rogueTalents : new RogueTalents()).toString());
        appendElementIfNotNull(doc, root, "DeathKnightTalents", (this.deathKnightTalents != null ? this.deathKnightTalents : new DeathKnightTalents()).toString());
        appendElementIfNotNull(doc, root, "WarlockTalents", (this.warlockTalents != null ? this.warlockTalents : new WarlockTalents()).toString());
        appendCustomItemInstances(doc, root);
        appendElementIfNotNull(doc, root, "PrimaryProfession", primaryProfession.name());
        appendElementIfNotNull(doc, root, "SecondaryProfession", secondaryProfession.name());
    }

    // XmlSerializer shape of List<OptimizationRequirement>: a wrapper named after the property and
    // one <OptimizationRequirement> per entry.
    private void appendOptimizationRequirements(Document doc, Element root) {
        if (optimizationRequirements == null) return;
        Element wrapper = doc.createElement("OptimizationRequirements");
        root.appendChild(wrapper);
        for (OptimizationRequirement requirement : optimizationRequirements) {
            Element element = doc.createElement("OptimizationRequirement");
            appendElementIfNotNull(doc, element, "Calculation", requirement.calculation);
            appendElementIfNotNull(doc, element, "LessThan", Boolean.toString(requirement.lessThan));
            appendElementIfNotNull(doc, element, "Value", Float.toString(requirement.value));
            wrapper.appendChild(element);
        }
    }

    // XmlSerializer shape of the public CustomItemInstances property (declared after the talents):
    // one <ItemInstance> per entry, the public fields as elements (Id always, the rest omitted when
    // zero per [DefaultValue(0)]) and ForceDisplay only when true.
    private void appendCustomItemInstances(Document doc, Element root) {
        if (customItemInstances == null) return;
        Element wrapper = doc.createElement("CustomItemInstances");
        root.appendChild(wrapper);
        for (ItemInstance instance : customItemInstances) {
            Element element = doc.createElement("ItemInstance");
            appendElementIfNotNull(doc, element, "Id", Integer.toString(instance.getId()));
            if (instance.getGem1Id() != 0) appendElementIfNotNull(doc, element, "Gem1Id", Integer.toString(instance.getGem1Id()));
            if (instance.getGem2Id() != 0) appendElementIfNotNull(doc, element, "Gem2Id", Integer.toString(instance.getGem2Id()));
            if (instance.getGem3Id() != 0) appendElementIfNotNull(doc, element, "Gem3Id", Integer.toString(instance.getGem3Id()));
            if (instance.getEnchantId() != 0) appendElementIfNotNull(doc, element, "EnchantId", Integer.toString(instance.getEnchantId()));
            if (instance.isForceDisplay()) appendElementIfNotNull(doc, element, "ForceDisplay", "true");
            wrapper.appendChild(element);
        }
    }


    // XmlSerializer writes the list wrapper even when the list is empty (C# Initialize() creates the
    // lists); items are <GemmingTemplate> with the fields in GemmingTemplate declaration order.
    private static void appendGemmingTemplates(Document doc, Element root, String wrapperName, List<GemmingTemplate> templates) {
        if (templates == null) return;
        Element wrapper = doc.createElement(wrapperName);
        root.appendChild(wrapper);
        for (GemmingTemplate template : templates) {
            Element element = doc.createElement("GemmingTemplate");
            appendElementIfNotNull(doc, element, "Id", template.getId());
            appendElementIfNotNull(doc, element, "Model", template.getModel());
            appendElementIfNotNull(doc, element, "Enabled", Boolean.toString(template.getEnabled()));
            appendElementIfNotNull(doc, element, "Group", template.getGroup());
            appendElementIfNotNull(doc, element, "RedId", Integer.toString(template.getRedId()));
            appendElementIfNotNull(doc, element, "YellowId", Integer.toString(template.getYellowId()));
            appendElementIfNotNull(doc, element, "BlueId", Integer.toString(template.getBlueId()));
            appendElementIfNotNull(doc, element, "MetaId", Integer.toString(template.getMetaId()));
            appendElementIfNotNull(doc, element, "PrismaticId", Integer.toString(template.getPrismaticId()));
            wrapper.appendChild(element);
        }
    }

    // XmlSerializer shape of List<ItemFilterEnabledOverride>: wrapper and items are both named
    // <ItemFilterEnabledOverride>; SubFilterOverride is written when non-null (the save prep only
    // creates it for filters that have sub-filters).
    private static void appendItemFilterOverrides(Document doc, Element root, List<ItemFilterEnabledOverride> overrides) {
        if (overrides == null) return;
        Element wrapper = doc.createElement("ItemFilterEnabledOverride");
        root.appendChild(wrapper);
        for (ItemFilterEnabledOverride override : overrides) {
            appendItemFilterOverride(doc, wrapper, override);
        }
    }

    private static void appendItemFilterOverride(Document doc, Element parent, ItemFilterEnabledOverride override) {
        Element element = doc.createElement("ItemFilterEnabledOverride");
        appendElementIfNotNull(doc, element, "Name", override.getName());
        appendElementIfNotNull(doc, element, "Enabled", Boolean.toString(override.isEnabled()));
        if (override.getSubFilterOverride() != null) {
            Element sub = doc.createElement("SubFilterOverride");
            element.appendChild(sub);
            for (ItemFilterEnabledOverride child : override.getSubFilterOverride()) {
                appendItemFilterOverride(doc, sub, child);
            }
        }
        parent.appendChild(element);
    }

    /**
     * The options object for the current model, deserializing it from the saved XML string on
     * first use (and binding it to this character), or null when the model has neither live nor
     * saved options. The result is cached in the per-model map.
     */
    public ICalculationOptionBase getCalculationOptions() {
        ICalculationOptionBase ret = calculationOptions.get(getCurrentModel());
        if (ret != null) {
            return ret;
        }
        return loadCalculationOptions();
    }

    /** Stores the options object for the current model; storing null makes the next read fall back to the saved XML. */
    public void setCalculationOptions(ICalculationOptionBase value) {
        calculationOptions.put(getCurrentModel(), value);
    }

    private ICalculationOptionBase loadCalculationOptions() {
        if (this.serializedCalculationOptions != null && this.serializedCalculationOptions.containsKey(getCurrentModel())) {
            ICalculationOptionBase ret = getCurrentCalculations().deserializeDataObject(this.serializedCalculationOptions.get(getCurrentModel()));
            if (ret instanceof ICharacterCalculationOptions options) {
                options.setCharacter(this);
            }
            calculationOptions.put(getCurrentModel(), ret);
            return ret;
        }
        return null;
    }

    public boolean getDisableBuffAutoActivation() {
        return disableBuffAutoActivation;
    }

    public void setDisableBuffAutoActivation(boolean disableBuffAutoActivation) {
        this.disableBuffAutoActivation = disableBuffAutoActivation;
    }

    /** Registers a callback fired after every {@link #onCalculationsInvalidated()} recompute. */
    public void addCalculationsInvalidatedListener(Runnable listener) {
        calculationsInvalidatedListeners.add(listener);
    }

    // C# Character.cs:1804-1821 OnCalculationsInvalidated (the calculationsInvalidatedListeners
    // stand in for the CalculationsInvalidated event).
    /**
     * The invalidation protocol: marks the gem-count cache stale, drops the relevant-item caches
     * by re-creating all item instances, and, unless a load is in progress, recomputes set and
     * passive bonuses and notifies the listeners. Called by setters; call it directly after
     * mutating exposed state.
     */
    public void onCalculationsInvalidated() {
        gemCountValid = false; // invalidate gem counts
        invalidateItemInstances();
        if (isLoading) return;
        recalculateSetBonuses();
        recalculatePassiveBonuses();
        for (Runnable listener : calculationsInvalidatedListeners) {
            listener.run();
        }
    }

    public boolean getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean value) {
        isLoading = value;
    }

    public ItemInstance getRanged() {
        return get(CharacterSlot.Ranged);
    }

    public List<Buff> getActiveBuffs() {
        return this.activeBuffs;
    }

    public void setActiveBuffs(List<Buff> activeBuffs) {
        this.activeBuffs = activeBuffs;
        validateActiveBuffs();
    }

    // C# Character.cs:645-681 ValidateActiveBuffs.
    /**
     * Normalizes the active-buff list: removes duplicates (keeping the last occurrence of each
     * name), adds or removes Heroic Presence depending on race and faction, and enforces the
     * automatic profession buffs (Toughness for Mining, Master of Anatomy for Skinning). Ends by
     * calling {@link #onCalculationsInvalidated()}. Called from {@link #setActiveBuffs} and from
     * the profession setters.
     */
    public void validateActiveBuffs() {
        // First let's check for Duplicate Buffs and remove them
        for (int i = 0; i < this.activeBuffs.size();) {
            Buff cur = this.activeBuffs.get(i);
            if (cur == null) {
                this.activeBuffs.remove(i);
                continue;
            }
            int count = 0;
            for (Buff iter : this.activeBuffs) {
                if (iter.name.equals(cur.name)) count++;
            }
            if (count > 1) {
                this.activeBuffs.remove(i);
                continue;
            }
            i++;
        }

        // Next let's check for Heroic Presence. If you are a Draenei and don't have it, you need it
        if (race == CharacterRace.Draenei && !activeBuffsContains("Heroic Presence")) {
            activeBuffsAdd("Heroic Presence");
        }
        // If you are Horde, you will never have this so let's take it out
        if (faction == CharacterFaction.Horde && activeBuffsContains("Heroic Presence")) {
            this.activeBuffs.remove(Buff.getBuffByName("Heroic Presence"));
        }

        // Finally, let's check Profession Buffs that should be automatically applied
        // Toughness buff from Mining
        if (hasProfession(Profession.Mining) && !activeBuffsContains("Toughness")) {
            activeBuffsAdd("Toughness");
        } else if (!hasProfession(Profession.Mining) && activeBuffsContains("Toughness")) {
            this.activeBuffs.remove(Buff.getBuffByName("Toughness"));
        }
        // Master of Anatomy from Skinning
        if (hasProfession(Profession.Skinning) && !activeBuffsContains("Master of Anatomy")) {
            activeBuffsAdd("Master of Anatomy");
        } else if (!hasProfession(Profession.Skinning) && activeBuffsContains("Master of Anatomy")) {
            this.activeBuffs.remove(Buff.getBuffByName("Master of Anatomy"));
        }

        onCalculationsInvalidated();
    }

    public void activeBuffsAdd(Buff buff) {
        if (buff != null)
            this.activeBuffs.add(buff);
    }

    public void activeBuffsAdd(String buffName) {
        Buff buff = Buff.getBuffByName(buffName);
        if (buff != null && !this.activeBuffs.contains(buff)) {
            this.activeBuffs.add(buff);
        }
    }

    public boolean activeBuffsContains(String buff) {
        if (this.activeBuffs == null)
            return false;
        for (Buff b : this.activeBuffs) {
            if (b.name.equals(buff)) return true;
        }
        return false;
    }

    public boolean activeBuffsConflictingBuffContains(String conflictingBuff) {
        if (this.activeBuffs == null)
            return false;
        for (Buff b : this.activeBuffs) {
            if (b.getConflictingBuffs() != null && b.getConflictingBuffs().contains(conflictingBuff)) return true;
        }
        return false;
    }

    public Map<String, Integer> getSetBonusCount() {
        return setBonusCount;
    }

    /**
     * A semi-deep copy: fresh {@link ItemInstance}s for every equipped item, a fresh active-buff
     * list (sharing the {@code Buff} definitions), but shared talents, boss options and
     * calculation-options maps. Faction, the invalidation-listener list and the
     * optimizer-related fields (available items, custom items, gemming templates, blacksmithing
     * sockets, item-filter overrides) are not copied, so the copy starts without listeners. The
     * copy finishes by recomputing its set-bonus buffs.
     */
    public Character clone() {
        ItemInstance[] clonedItems = new ItemInstance[SlotCount];
        for (int i = 0; i < clonedItems.length; i++) {
            ItemInstance item = equippedItems[i];
            if (item != null) {
                clonedItems[i] = new ItemInstance(item.getId(), item.getGem1Id(), item.getGem2Id(), item.getGem3Id(), item.getEnchantId());
            }
        }
        Character clone = new Character();
        clone.name = name;
        clone.realm = realm;
        clone.region = region;
        clone.race = race;
        System.arraycopy(clonedItems, 0, clone.equippedItems, 0, SlotCount);
        clone.characterClass = characterClass;
        clone.currentModel = currentModel;
        clone.enforceGemRequirements = enforceGemRequirements;
        clone.primaryProfession = primaryProfession;
        clone.secondaryProfession = secondaryProfession;
        clone.activeBuffs = new java.util.ArrayList<Buff>(this.activeBuffs);
        clone.waistBlacksmithingSocketEnabled = waistBlacksmithingSocketEnabled;
        clone.handsBlacksmithingSocketEnabled = handsBlacksmithingSocketEnabled;
        clone.wristBlacksmithingSocketEnabled = wristBlacksmithingSocketEnabled;
        clone.calculationToOptimize = calculationToOptimize;
        clone.optimizationRequirements = optimizationRequirements;
        clone.mageTalents = this.mageTalents;
        clone.warriorTalents = this.warriorTalents;
        clone.shamanTalents = this.shamanTalents;
        clone.druidTalents = this.druidTalents;
        clone.paladinTalents = this.paladinTalents;
        clone.priestTalents = this.priestTalents;
        clone.hunterTalents = this.hunterTalents;
        clone.rogueTalents = this.rogueTalents;
        clone.deathKnightTalents = this.deathKnightTalents;
        clone.warlockTalents = this.warlockTalents;
        clone.bossOptions = getBossOptions();
        clone.calculationOptions = calculationOptions;
        clone.serializedCalculationOptions = this.serializedCalculationOptions;
        clone.disableBuffAutoActivation = disableBuffAutoActivation;
        clone.recalculateSetBonuses();
        return clone;
    }

    /**
     * Clone for one top-level calculation or optimizer job with an independent calculation-options
     * map. The option objects themselves are shared; the caller replaces the current model's
     * option with a fresh copy (serialized/deserialized) so model-internal state cannot leak back
     * into the session character.
     */
    public Character cloneForCalculation() {
        Character clone = clone();
        clone.calculationOptions = new LinkedHashMap<String, ICalculationOptionBase>(calculationOptions);
        clone.serializedCalculationOptions = serializedCalculationOptions == null
                ? null : new LinkedHashMap<String, String>(serializedCalculationOptions);
        return clone;
    }

    /**
     * Rebuilds {@code setBonusCount} from the equipped items (one count per non-empty item-set
     * name) and resynchronizes the active-buff list: every set-bonus entry is removed, then
     * re-added for each set whose equipped-piece count reaches the buff's threshold. The counter
     * map keeps insertion order (order of first appearance while walking the slots) because the
     * original enumerated it the same way. Called by the invalidator and by the clone/optimizer
     * constructors.
     */
    public void recalculateSetBonuses() {
        if (setBonusCount == null) {
            setBonusCount = new LinkedHashMap<String, Integer>();
        } else {
            setBonusCount.clear();
        }
        for (int slot = 0; slot < equippedItems.length; slot++) {
            ItemInstance item = equippedItems[slot];
            if (item != null && item.getItem() != null && item.getItem().getSetName() != null && !item.getItem().getSetName().isEmpty()) {
                String setName = item.getItem().getSetName();
                Integer count = setBonusCount.get(setName);
                setBonusCount.put(setName, (count == null ? 0 : count) + 1);
            }
        }

        this.activeBuffs.removeIf(buff -> buff.setName != null && !buff.setName.isEmpty());
        for (Map.Entry<String, Integer> pair : setBonusCount.entrySet()) {
            Buff[] setBonuses = Buff.getSetBonuses(pair.getKey());
            if (setBonuses != null) {
                for (Buff buff : setBonuses) {
                    if (pair.getValue() >= buff.setThreshold) {
                        this.activeBuffs.add(buff);
                    }
                }
            }
        }
    }

    public ItemInstance get(CharacterSlot slot) {
        int i = slot.value();
        if (i < 0 || i >= SlotCount) return null;
        return equippedItems[i];
    }

    public void set(CharacterSlot slot, ItemInstance value) {
        int i = slot.value();
        if (i < 0 || i >= SlotCount) return;
        equippedItems[i] = value;
        gemCountValid = false;
        onCalculationsInvalidated();
    }

    /**
     * Returns the item instance at a raw slot index ({@code 0..}{@link #SlotCount}{@code -1},
     * matching {@link CharacterSlot#value()}), or {@code null} for an empty slot or an
     * out-of-range index. Unlike {@link #get(CharacterSlot)} this accepts the integer indices the
     * optimizer works with and performs no validation beyond the range check.
     */
    public ItemInstance getItem(int slot) {
        if (slot < 0 || slot >= SlotCount) return null;
        return equippedItems[slot];
    }

    /**
     * Stores an item instance at a raw slot index without the invalidation and gem-count reset of
     * {@link #set(CharacterSlot, ItemInstance)}.
     *
     * <p>This mirrors the original's direct {@code equippedItems[slot] = ...} writes (for example the
     * optimizer's normalization of the character); only call it from code that performs its own
     * invalidation or deliberately wants to bypass it.</p>
     */
    public void setItem(int slot, ItemInstance value) {
        if (slot < 0 || slot >= SlotCount) return;
        equippedItems[slot] = value;
    }

    /**
     * Returns the live item-instance array of length {@link #SlotCount}, indexed by
     * {@link CharacterSlot#value()}.
     *
     * <p>It exists for the optimizer's bulk operations (copying a whole character's gear, aliasing
     * the gene array), which the original performed on the C# {@code internal} field. The returned
     * array is the character's own storage: mutating it changes the character without any cache
     * invalidation. Prefer {@link #getItem(int)} / {@link #setItem(int, ItemInstance)}, or the
     * cloning {@link #getItems()} accessor.</p>
     */
    public ItemInstance[] getRawItems() {
        return equippedItems;
    }

    // C# Character.cs:712-716 MainHand/OffHand/Ranged/RangedEnchant slot shortcuts (added for TankDK).
    public ItemInstance getMainHand() {
        return get(CharacterSlot.MainHand);
    }

    public void setMainHand(ItemInstance value) {
        set(CharacterSlot.MainHand, value);
    }

    public ItemInstance getOffHand() {
        return get(CharacterSlot.OffHand);
    }

    public void setOffHand(ItemInstance value) {
        set(CharacterSlot.OffHand, value);
    }

    // C# Character.cs:750-753 MainHandEnchant/OffHandEnchant + Character.cs:1491-1597
    // GetEnchantBySlot/SetEnchantBySlot.
    public Enchant getMainHandEnchant() {
        return getEnchantBySlot(CharacterSlot.MainHand);
    }

    public Enchant getOffHandEnchant() {
        return getEnchantBySlot(CharacterSlot.OffHand);
    }

    public void setOffHandEnchant(Enchant enchant) {
        setEnchantBySlot(CharacterSlot.OffHand, enchant);
    }

    public Enchant getEnchantBySlot(CharacterSlot slot) {
        ItemInstance item = get(slot);
        if (item == null) return null;
        return item.getEnchant();
    }

    public void setEnchantBySlot(CharacterSlot slot, Enchant enchant) {
        int i = slot.value();
        if (i < 0 || i >= SlotCount) return;
        ItemInstance item = get(slot);
        if (item != null) item.setEnchant(enchant);
        onCalculationsInvalidated();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public CharacterRegion getRegion() {
        return region;
    }

    public void setRegion(CharacterRegion region) {
        this.region = region;
    }

    /** Always 80: this port models the level-80 game, so the level is neither stored nor settable. */
    public int getLevel() {
        return 80;
    }

    public CharacterRace getRace() {
        return race;
    }

    // C# Character.cs:546-560 Race setter + Character.cs:931-946 SetFaction.
    /**
     * Changes race and, when the value actually changes, recomputes the faction (Alliance for
     * Draenei, Dwarf, Gnome, Human and NightElf; Horde otherwise) and invalidates the character.
     */
    public void setRace(CharacterRace race) {
        if (this.race != race) {
            this.race = race;
            setFaction();
            onCalculationsInvalidated();
        }
    }

    // C# Character.cs:586-589.
    public CharacterFaction getFaction() {
        return faction;
    }

    private void setFaction() {
        if (race == CharacterRace.Draenei || race == CharacterRace.Dwarf || race == CharacterRace.Gnome
                || race == CharacterRace.Human || race == CharacterRace.NightElf) {
            faction = CharacterFaction.Alliance;
        } else {
            faction = CharacterFaction.Horde;
            // horde don't get heroic presence muahahaha
            getActiveBuffs().removeIf(b -> b.name.equals("Heroic Presence"));
        }
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(CharacterClass characterClass) {
        this.characterClass = characterClass;
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public CalculationsBase getCurrentCalculations() {
        return Calculations.getModel(currentModel);
    }

    public boolean getEnforceGemRequirements() {
        return enforceGemRequirements;
    }

    public void setEnforceGemRequirements(boolean enforceGemRequirements) {
        this.enforceGemRequirements = enforceGemRequirements;
    }

    public Profession getPrimaryProfession() {
        return primaryProfession;
    }

    // C# Character.cs:882-908: the setters call Calculations.UpdateProfessions and ValidateActiveBuffs
    // when the value changes.
    /**
     * Changes the profession slot, then lets the model adjust derived data
     * ({@code Calculations.updateProfessions}) and re-runs {@link #validateActiveBuffs()}. A
     * no-op when the value is unchanged.
     */
    public void setPrimaryProfession(Profession primaryProfession) {
        if (this.primaryProfession != primaryProfession) {
            this.primaryProfession = primaryProfession;
            Calculations.updateProfessions(this);
            validateActiveBuffs();
        }
    }

    public Profession getSecondaryProfession() {
        return secondaryProfession;
    }

    /**
     * Changes the profession slot, then lets the model adjust derived data
     * ({@code Calculations.updateProfessions}) and re-runs {@link #validateActiveBuffs()}. A
     * no-op when the value is unchanged.
     */
    public void setSecondaryProfession(Profession secondaryProfession) {
        if (this.secondaryProfession != secondaryProfession) {
            this.secondaryProfession = secondaryProfession;
            Calculations.updateProfessions(this);
            validateActiveBuffs();
        }
    }

    public int getRedGemCount() {
        computeGemCount();
        return redGemCount;
    }

    public int getYellowGemCount() {
        computeGemCount();
        return yellowGemCount;
    }

    public int getBlueGemCount() {
        computeGemCount();
        return blueGemCount;
    }

    public int getJewelersGemCount() {
        computeGemCount();
        return jewelersGemCount;
    }

    public int getGemRequirementsInvalid() {
        computeGemCount();
        return gemRequirementsInvalid;
    }

    public int getNonjewelerGemRequirementsInvalid() {
        computeGemCount();
        return nonjewelerGemRequirementsInvalid;
    }

    private void computeGemCount() {
        if (!gemCountValid) {
            redGemCount = 0;
            yellowGemCount = 0;
            blueGemCount = 0;
            jewelersGemCount = 0;
            Set<Integer> uniqueGems = null;
            gemRequirementsInvalid = 0;
            nonjewelerGemRequirementsInvalid = 0;
            for (int slot = 0; slot < OptimizableSlotCount; slot++) {
                if (slot != CharacterSlot.OffHand.value() || getCurrentCalculations().includeOffHandInCalculations(this)) {
                    ItemInstance item = equippedItems[slot];
                    if (item == null) continue;
                    for (int gemIndex = 1; gemIndex <= 3; gemIndex++) {
                        Item gem = item.getGem(gemIndex);
                        if (gem != null) {
                            if (gem.isRedGem()) redGemCount++;
                            if (gem.isYellowGem()) yellowGemCount++;
                            if (gem.isBlueGem()) blueGemCount++;
                            if (gem.isJewelersGem()) jewelersGemCount++;
                            else if (gem.isUnique()) {
                                if (uniqueGems == null) {
                                    uniqueGems = new HashSet<Integer>();
                                }
                                if (!uniqueGems.add(gem.getId())) {
                                    gemRequirementsInvalid++;
                                    nonjewelerGemRequirementsInvalid++;
                                }
                            }
                        }
                    }
                }
            }
            if (jewelersGemCount > 3) {
                gemRequirementsInvalid += jewelersGemCount - 3;
            }

            gemCountValid = true;
        }
    }

    private int getItemGemIdCount(ItemInstance item, int id) {
        int count = 0;
        if (item != null) {
            if (item.getGem1() != null && item.getGem1().getId() == id) count++;
            if (item.getGem2() != null && item.getGem2().getId() == id) count++;
            if (item.getGem3() != null && item.getGem3().getId() == id) count++;
        }
        return count;
    }

    public int getGemIdCount(int id) {
        int count = 0;
        for (int slot = 0; slot < SlotCount; slot++) {
            count += getItemGemIdCount(equippedItems[slot], id);
        }
        return count;
    }

    // C# Character.cs:39-42 GetItems.
    public ItemInstance[] getItems() {
        return equippedItems.clone();
    }

    // C# Character.cs:40-81 SetItems. Clones the instances the same way (ItemInstance.Clone).
    public void setItems(ItemInstance[] items) {
        setItems(items, true);
    }

    /**
     * Replaces the gear array, cloning each non-null instance, for at most the first
     * {@link #OptimizableSlotCount} slots. The {@code invalidate} flag exists for the optimizer,
     * which must not invalidate: re-creating instances would discard its cached item stats. The
     * public one-argument overload always invalidates.
     */
    public void setItems(ItemInstance[] items, boolean invalidate) {
        int max = Math.min(OptimizableSlotCount, items.length);
        for (int slot = 0; slot < max; slot++) {
            equippedItems[slot] = items[slot] == null ? null : items[slot].clone();
        }
        // when called from optimizer we never want to invalidate since that causes creation of new item instances
        // and causes us to lose stats cache
        if (invalidate) {
            onCalculationsInvalidated();
        }
    }

    // C# Character.cs:799-816.
    /**
     * Clears the relevant-item/relevant-instance caches and, unless a load is in progress,
     * replaces every equipped instance with a fresh equal one (other code compares instances by
     * identity, so mutation by sharing is not safe). The per-slot overload deliberately does not
     * null-check, mirroring the original's failure on an empty slot.
     */
    public void invalidateItemInstances() {
        if (relevantItems != null) {
            relevantItems.clear();
            relevantItemInstances.clear();
        }
        if (!isLoading) {
            for (int i = 0; i < equippedItems.length; i++) {
                if (equippedItems[i] != null) {
                    equippedItems[i] = new ItemInstance(equippedItems[i].getId(), equippedItems[i].getGem1Id(), equippedItems[i].getGem2Id(), equippedItems[i].getGem3Id(), equippedItems[i].getEnchantId());
                }
            }
        }
    }

    // C# Character.cs:818-823. Dead in the shipped UI; ported for parity (throws on an empty slot
    // exactly like the C# NullReferenceException).
    public void invalidateItemInstances(CharacterSlot slot) {
        relevantItemInstances.remove(slot);
        int i = slot.value();
        equippedItems[i] = new ItemInstance(equippedItems[i].getId(), equippedItems[i].getGem1Id(), equippedItems[i].getGem2Id(), equippedItems[i].getGem3Id(), equippedItems[i].getEnchantId());
    }

    // C# Character.cs:825-839.
    public List<ItemInstance> getCustomItemInstances() {
        return customItemInstances;
    }

    public void setCustomItemInstances(List<ItemInstance> value) {
        customItemInstances = value;
        invalidateItemInstances();
    }

    // C# Character.cs:953-1101. Expands ItemCache.RelevantItems through the enabled gemming
    // templates (plus the custom instances and the AvailableItems entries) and caches the result per
    // CharacterSlot. Order is significant and mirrors the original exactly.
    /**
     * The cached list of candidate instances for one slot. On a miss it walks
     * {@code ItemCache.relevantItems()}, keeps items fitting the slot and the character's
     * faction, and expands each through the enabled gemming templates (built-in plus
     * model-matching custom ones), honoring the per-slot blacksmithing socket when enabled. It
     * then appends the character's custom instances and the explicit {@code availableItems}
     * entries, in that order, forcing visibility for entries whose exact gem/enchant combination
     * would otherwise be filtered out. The final order is significant for the optimizer, so the
     * construction sequence must not be rearranged. Results are cached per slot;
     * {@link #invalidateItemInstances()} drops the cache.
     */
    public List<ItemInstance> getRelevantItemInstances(CharacterSlot slot) {
        boolean blacksmithingSocket = false;
        if ((slot == CharacterSlot.Waist && waistBlacksmithingSocketEnabled)
                || (slot == CharacterSlot.Hands && handsBlacksmithingSocketEnabled)
                || (slot == CharacterSlot.Wrist && wristBlacksmithingSocketEnabled)) {
            blacksmithingSocket = true;
        }
        List<ItemInstance> items = relevantItemInstances.get(slot);
        if (items == null) {
            Set<Integer> checkedItems = new LinkedHashSet<Integer>();
            items = new ArrayList<ItemInstance>();
            for (Item item : ItemCache.relevantItems()) {
                if (item.fitsInSlot(slot, this) && item.fitsFaction(race)) {
                    checkedItems.add(item.getId());
                    List<ItemInstance> itemInstances = new ArrayList<ItemInstance>();
                    for (GemmingTemplate template : getCurrentGemmingTemplates()) {
                        if (template.getEnabled()) {
                            ItemInstance instance = template.getItemInstance(item, getEnchantBySlot(slot), blacksmithingSocket);
                            if (!itemInstances.contains(instance)) itemInstances.add(instance);
                        }
                    }
                    for (GemmingTemplate template : getCustomGemmingTemplates()) {
                        if (template.getEnabled() && java.util.Objects.equals(template.getModel(), getCurrentModel())) {
                            ItemInstance instance = template.getItemInstance(item, getEnchantBySlot(slot), blacksmithingSocket);
                            if (!itemInstances.contains(instance)) itemInstances.add(instance);
                        }
                    }
                    items.addAll(itemInstances);
                }
            }
            // add custom instances
            for (ItemInstance item : getCustomItemInstances()) {
                if (item.getItem() != null && item.getItem().fitsInSlot(slot, this)) { // item.Item can be null if you're loading character with custom items that are not present on this install
                    // if it's already in make sure to set force visible to true
                    int index = items.indexOf(item);
                    if (index >= 0) {
                        items.set(index, item);
                    } else {
                        items.add(item);
                    }
                }
            }
            // add available instances
            for (String availableItem : getAvailableItems()) {
                String[] ids = availableItem.split("\\.");
                if (ids.length == 1 || "*".equals(ids[1])) {
                    // we have an available item that might be filtered out
                    Item item = ItemCache.findItemById(Integer.parseInt(ids[0]));
                    if (item != null) {
                        if (item.fitsInSlot(slot, this)) {
                            boolean check = checkedItems.contains(item.getId());
                            Enchant enchant;
                            if (ids.length < 5 || "*".equals(ids[4])) {
                                if (check) {
                                    // we've already processed this one
                                    continue;
                                }
                                enchant = getEnchantBySlot(slot);
                            } else {
                                Enchant currentEnchant = getEnchantBySlot(slot);
                                int currentId = currentEnchant != null ? currentEnchant.id : 0;
                                if (check && Integer.parseInt(ids[4]) == currentId) {
                                    continue;
                                }
                                enchant = Enchant.findEnchant(Integer.parseInt(ids[4]), item.getSlot(), this);
                            }
                            List<ItemInstance> itemInstances = new ArrayList<ItemInstance>();
                            for (GemmingTemplate template : getCurrentGemmingTemplates()) {
                                if (template.getEnabled()) {
                                    ItemInstance instance = template.getItemInstance(item, enchant, blacksmithingSocket);
                                    if (!itemInstances.contains(instance)) itemInstances.add(instance);
                                }
                            }
                            for (GemmingTemplate template : getCustomGemmingTemplates()) {
                                if (template.getEnabled() && java.util.Objects.equals(template.getModel(), getCurrentModel())) {
                                    ItemInstance instance = template.getItemInstance(item, enchant, blacksmithingSocket);
                                    if (!itemInstances.contains(instance)) itemInstances.add(instance);
                                }
                            }
                            if (check) {
                                for (ItemInstance instance : itemInstances) {
                                    if (!items.contains(instance)) items.add(instance);
                                }
                            } else {
                                items.addAll(itemInstances);
                            }
                        }
                        checkedItems.add(item.getId());
                    }
                }
            }
            for (String availableItem : getAvailableItems()) {
                // only have to worry about items with gems, others should be visible already
                String[] ids = availableItem.split("\\.");
                if (ids.length > 1 && !"*".equals(ids[1])) {
                    Item item = ItemCache.findItemById(Integer.parseInt(ids[0]));
                    if (item.fitsInSlot(slot, this)) {
                        Enchant slotEnchant = getEnchantBySlot(slot);
                        Enchant enchant = slotEnchant != null ? slotEnchant : new Enchant();
                        ItemInstance instance = new ItemInstance(Integer.parseInt(ids[0]), Integer.parseInt(ids[1]), Integer.parseInt(ids[2]), Integer.parseInt(ids[3]), "*".equals(ids[4]) ? enchant.id : Integer.parseInt(ids[4]));
                        instance.setForceDisplay(true);
                        // we want to force display even if it's already present (might be lower then top N)
                        int index = items.indexOf(instance);
                        if (index < 0) {
                            items.add(instance);
                        } else {
                            items.set(index, instance);
                        }
                    }
                }
            }
            relevantItemInstances.put(slot, items);
        }
        return items;
    }

    // C# Character.cs:1103-1127. The cache is keyed by slot only (the gem colour is part of the
    // first query); ClearRelevantGems drops the Gems entry so a new colour can be requested.
    /**
     * Drops the gem pseudo-slot cache so the next {@link #getRelevantItems(CharacterSlot,
     * ItemSlot)} query with a different gem color can be built (the cache itself is keyed by slot
     * only).
     */
    public void clearRelevantGems() {
        relevantItems.remove(CharacterSlot.Gems);
    }

    public List<Item> getRelevantItems(CharacterSlot slot) {
        return getRelevantItems(slot, ItemSlot.None);
    }

    /**
     * The cached item list for one slot, optionally restricted to a gem color. The color filter
     * applies only on the first query; use {@link #clearRelevantGems()} to switch the color of
     * the gem pseudo-slot. Slot fitting is checked with this character present, so faction and
     * class restrictions apply.
     */
    public List<Item> getRelevantItems(CharacterSlot slot, ItemSlot gemColour) {
        List<Item> items = relevantItems.get(slot);
        if (items == null) {
            items = new ArrayList<Item>();
            for (Item item : ItemCache.relevantItems()) {
                if (item.fitsInSlot(slot, this)) {
                    if ((gemColour == ItemSlot.None)
                            || (gemColour == ItemSlot.Red && item.isRedGem())
                            || (gemColour == ItemSlot.Yellow && item.isYellowGem())
                            || (gemColour == ItemSlot.Blue && item.isBlueGem())) {
                        items.add(item);
                    }
                }
            }
            relevantItems.put(slot, items);
        }
        return items;
    }

    // C# Character.cs:2189-2219, the optimizer's fast copy constructor. PARTIAL: faction is not
    // ported yet, so that C# copy is omitted.
    /**
     * Optimizer fast path: copies identity, model, professions, talents (by reference), the
     * calculation-options maps and boss options from a base character, then takes ownership of
     * the given item array without cloning it. Faction and the base character's custom-instance,
     * gemming and available-items state are intentionally not carried over. Callers pass an array
     * of at least {@code count} entries and a count of at most {@link #SlotCount}.
     */
    public Character(Character baseCharacter, Object[] items, int count) {
        name = baseCharacter.name;
        realm = baseCharacter.realm;
        region = baseCharacter.region;
        race = baseCharacter.race;
        currentModel = baseCharacter.currentModel;
        calculationOptions = baseCharacter.calculationOptions;
        primaryProfession = baseCharacter.primaryProfession;
        secondaryProfession = baseCharacter.secondaryProfession;
        characterClass = baseCharacter.characterClass;
        assignAllTalentsFromCharacter(baseCharacter, false);
        calculationToOptimize = baseCharacter.calculationToOptimize;
        optimizationRequirements = baseCharacter.optimizationRequirements;
        enforceGemRequirements = baseCharacter.enforceGemRequirements;
        bossOptions = baseCharacter.bossOptions;

        equippedItems = new ItemInstance[SlotCount];
        System.arraycopy(items, 0, equippedItems, 0, count);
        this.activeBuffs = new ArrayList<Buff>(baseCharacter.activeBuffs);
        recalculateSetBonuses();
    }

    // C# Character.cs:2220-2225 InitializeCharacter (recycled constructor path).
    /**
     * Recycles this character for another optimizer evaluation: takes the first {@code count}
     * slots from the argument without cloning, marks the gem-count cache stale and rebuilds the
     * set-bonus buffs. Everything else (talents, buffs, options, identity) stays in place.
     */
    public void initializeCharacter(Object[] items, int count) {
        gemCountValid = false;
        System.arraycopy(items, 0, equippedItems, 0, count);
        recalculateSetBonuses();
    }

    // C# Character.cs:2227-2250, used by ItemOptimizer. PARTIAL: no BossOptions in the Java core.
    /**
     * Convenience constructor used by the item optimizer: builds a character from raw fields and
     * an item array (stored as-is, not cloned) without talents, professions or calculation
     * options, then recomputes set bonuses.
     */
    public Character(String name, String realm, CharacterRegion region, CharacterRace race,
                     ItemInstance[] items, List<Buff> activeBuffs, String model) {
        this.name = name;
        this.realm = realm;
        this.region = region;
        this.race = race;
        System.arraycopy(items, 0, equippedItems, 0, items.length);
        this.activeBuffs = new ArrayList<Buff>(activeBuffs);
        currentModel = model;
        recalculateSetBonuses();
    }

    // C# Character.cs:910-915
    public boolean hasProfession(Profession p) {
        if (getPrimaryProfession() == p) return true;
        if (getSecondaryProfession() == p) return true;
        return false;
    }

    // C# Character.cs:917-926 HasProfession(List<Profession>)
    public boolean hasProfession(List<Profession> list) {
        for (Profession p : list) {
            if (hasProfession(p)) return true;
        }
        return false;
    }

    // C# Character.cs:1172 AvailableItems. The C# setter also raises the AvailableItemsChanged
    // event; its only consumers are UI, so the event is not ported.
    public List<String> getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(List<String> value) {
        availableItems = value;
    }

    // C# Character.cs:1262-1296
    /**
     * Where an item stands relative to the character's available-items list, which encodes the
     * "only use items I can actually obtain" restriction. Entries have the form
     * {@code id[.gem1.gem2.gem3[.enchant]]} where a {@code *} segment means "any"; the result
     * distinguishes fully available, available-with-enchant-restrictions, regemming-allowed and
     * unavailable. See {@link ItemAvailability}.
     */
    public ItemAvailability getItemAvailability(Item item) {
        return getItemAvailability(Integer.toString(item.getId()), item.getId() + ".0.0.0", item.getId() + ".0.0.0.0");
    }

    public ItemAvailability getItemAvailability(ItemInstance itemInstance) {
        return getItemAvailability(Integer.toString(itemInstance.getId()),
                itemInstance.getId() + "." + itemInstance.getGem1Id() + "." + itemInstance.getGem2Id() + "." + itemInstance.getGem3Id(),
                itemInstance.getGemmedId());
    }

    private ItemAvailability getItemAvailability(String id, String gemId, String fullId) {
        String anyGem = id + ".*.*.*";
        List<String> list = new ArrayList<String>();
        for (String x : availableItems) {
            if (x.startsWith(id)) list.add(x);
        }
        if (list.contains(gemId + ".*")) {
            return ItemAvailability.Available;
        } else if (startsWithAny(list, gemId)) {
            return ItemAvailability.AvailableWithEnchantRestrictions;
        }
        if (list.contains(id)) {
            return ItemAvailability.RegemmingAllowed;
        } else if (startsWithAny(list, anyGem)) {
            return ItemAvailability.RegemmingAllowedWithEnchantRestrictions;
        } else {
            return ItemAvailability.NotAvailable;
        }
    }

    private static boolean startsWithAny(List<String> list, String prefix) {
        for (String x : list) {
            if (x.startsWith(prefix)) return true;
        }
        return false;
    }

    // C# Character.cs:1217-1260
    /**
     * Maps a database item slot ({@link ItemSlot}, the position an item template occupies) to
     * the concrete character slot it would go into. Finger and trinket always map to the first of
     * their two slots; both one-hand and two-hand weapons map to the main hand; gem colors map to
     * the gem pseudo-slot, meta gems to the meta pseudo-slot, and anything unknown to
     * {@link CharacterSlot#None}.
     */
    public static CharacterSlot getCharacterSlotByItemSlot(ItemSlot slot) {
        // note: When converting ItemSlot.Finger and ItemSlot.Trinket, this will ALWAYS
        // place them in Slot 1 of the 2 possibilities. Items listed as OneHand or TwoHand
        // in their Itemslot profile, will be parsed into the MainHand CharacterSlot.
        switch (slot) {
            case Projectile: return CharacterSlot.Projectile;
            case Head: return CharacterSlot.Head;
            case Neck: return CharacterSlot.Neck;
            case Shoulders: return CharacterSlot.Shoulders;
            case Chest: return CharacterSlot.Chest;
            case Waist: return CharacterSlot.Waist;
            case Legs: return CharacterSlot.Legs;
            case Feet: return CharacterSlot.Feet;
            case Wrist: return CharacterSlot.Wrist;
            case Hands: return CharacterSlot.Hands;
            case Finger: return CharacterSlot.Finger1;
            case Trinket: return CharacterSlot.Trinket1;
            case Back: return CharacterSlot.Back;
            case OneHand: return CharacterSlot.MainHand;
            case TwoHand: return CharacterSlot.MainHand;
            case MainHand: return CharacterSlot.MainHand;
            case OffHand: return CharacterSlot.OffHand;
            case Ranged: return CharacterSlot.Ranged;
            case ProjectileBag: return CharacterSlot.ProjectileBag;
            case Tabard: return CharacterSlot.Tabard;
            case Shirt: return CharacterSlot.Shirt;
            case Red: return CharacterSlot.Gems;
            case Orange: return CharacterSlot.Gems;
            case Yellow: return CharacterSlot.Gems;
            case Green: return CharacterSlot.Gems;
            case Blue: return CharacterSlot.Gems;
            case Purple: return CharacterSlot.Gems;
            case Prismatic: return CharacterSlot.Gems;
            case Meta: return CharacterSlot.Metas;
            default: return CharacterSlot.None;
        }
    }

    // C# Character.cs:1182-1215 IsEquipped overloads. The ItemInstance overloads use reference
    // equality (`itemToBeChecked == this[slot]`), the Item overloads compare ids.
    /**
     * Whether this exact instance object is equipped in the slot(s) allowed by its item template.
     * The overloads taking an explicit slot use reference equality, so an equal-looking but
     * distinct instance is not "equipped"; the {@link Item} overloads compare ids instead.
     */
    public boolean isEquipped(ItemInstance itemToBeChecked) {
        CharacterSlot slot = getCharacterSlotByItemSlot(itemToBeChecked.getSlot());
        if (slot == CharacterSlot.Finger1) {
            return isEquipped(itemToBeChecked, CharacterSlot.Finger1) || isEquipped(itemToBeChecked, CharacterSlot.Finger2);
        } else if (itemToBeChecked.getSlot() == ItemSlot.OneHand) {
            return isEquipped(itemToBeChecked, CharacterSlot.MainHand) || isEquipped(itemToBeChecked, CharacterSlot.OffHand);
        } else if (itemToBeChecked.getSlot() == ItemSlot.Trinket) {
            return isEquipped(itemToBeChecked, CharacterSlot.Trinket1) || isEquipped(itemToBeChecked, CharacterSlot.Trinket2);
        } else {
            return isEquipped(itemToBeChecked, slot);
        }
    }

    public boolean isEquipped(ItemInstance itemToBeChecked, CharacterSlot slot) {
        return itemToBeChecked == get(slot);
    }

    public boolean isEquipped(Item itemToBeChecked) {
        CharacterSlot slot = getCharacterSlotByItemSlot(itemToBeChecked.getSlot());
        if (slot == CharacterSlot.Finger1) {
            return isEquipped(itemToBeChecked, CharacterSlot.Finger1) || isEquipped(itemToBeChecked, CharacterSlot.Finger2);
        } else if (itemToBeChecked.getSlot() == ItemSlot.OneHand) {
            return isEquipped(itemToBeChecked, CharacterSlot.MainHand) || isEquipped(itemToBeChecked, CharacterSlot.OffHand);
        } else if (itemToBeChecked.getSlot() == ItemSlot.Trinket) {
            return isEquipped(itemToBeChecked, CharacterSlot.Trinket1) || isEquipped(itemToBeChecked, CharacterSlot.Trinket2);
        } else {
            return isEquipped(itemToBeChecked, slot);
        }
    }

    public boolean isEquipped(Item itemToBeChecked, CharacterSlot slot) {
        return get(slot) != null && itemToBeChecked.getId() == get(slot).getId();
    }

    // C# Character.cs:1748-1767.
    public boolean isUniqueGemEquipped(Item testGem) {
        for (int slot = 0; slot < OptimizableSlotCount; slot++) {
            if (slot != CharacterSlot.OffHand.value() || getCurrentCalculations().includeOffHandInCalculations(this)) {
                ItemInstance item = equippedItems[slot];
                if (item == null) continue;
                for (int gemIndex = 1; gemIndex <= 3; gemIndex++) {
                    Item gem = item.getGem(gemIndex);
                    if (gem != null && !gem.isJewelersGem()) {
                        if (gem.isUnique() && gem == testGem) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // C# Character.cs:1876-1882.
    public void recalculatePassiveBonuses() {
        if (race == CharacterRace.Draenei) {
            activeBuffsAdd("Heroic Presence");
        }
    }

    // C# Character.cs:1911-1953. The C# body reads the XmlSerializer slot properties, which are
    // the GemmedId of the equipped item in that slot; the order of slots below matches it.
    /**
     * The de-duplicated, insertion-ordered union of every equipped gemmed id and every
     * available-items entry, with the trailing enchant segment of available entries stripped.
     * Used by the loading path to know which item ids a saved character references and must be
     * loaded. Entries starting with {@code "-"} (exclusions) are skipped.
     */
    public String[] getAllEquippedAndAvailableGearIds() {
        Set<String> gearIds = new LinkedHashSet<String>();
        CharacterSlot[] slots = {
            CharacterSlot.Back, CharacterSlot.Chest, CharacterSlot.Feet, CharacterSlot.Finger1,
            CharacterSlot.Finger2, CharacterSlot.Hands, CharacterSlot.Head, CharacterSlot.Legs,
            CharacterSlot.MainHand, CharacterSlot.Neck, CharacterSlot.OffHand, CharacterSlot.Projectile,
            CharacterSlot.ProjectileBag, CharacterSlot.Ranged, CharacterSlot.Shirt, CharacterSlot.Shoulders,
            CharacterSlot.Tabard, CharacterSlot.Trinket1, CharacterSlot.Trinket2, CharacterSlot.Waist,
            CharacterSlot.Wrist,
        };
        for (CharacterSlot slot : slots) {
            ItemInstance item = get(slot);
            if (item != null) gearIds.add(item.getGemmedId());
        }
        for (String xid : getAvailableItems()) {
            if (!xid.startsWith("-")) {
                int dot = xid.lastIndexOf('.');
                gearIds.add(dot >= 0 ? xid.substring(0, dot).replace(".*.*.*", "") : xid);
            }
        }
        return gearIds.toArray(new String[0]);
    }

    // C# Character.cs:1955-2030.
    /**
     * Maps the numeric slot id used by the legacy Armory/profiler importers (head = 1, ..., ranged = 18,
     * projectile bag = 102) to a {@link CharacterSlot}; unknown ids yield
     * {@link CharacterSlot#None}. This numbering is different from {@link CharacterSlot#value()}.
     */
    public static CharacterSlot getCharacterSlotFromId(int slotId) {
        switch (slotId) {
            case -1: return CharacterSlot.None;
            case 1: return CharacterSlot.Head;
            case 2: return CharacterSlot.Neck;
            case 3: return CharacterSlot.Shoulders;
            case 15: return CharacterSlot.Back;
            case 5: return CharacterSlot.Chest;
            case 4: return CharacterSlot.Shirt;
            case 19: return CharacterSlot.Tabard;
            case 9: return CharacterSlot.Wrist;
            case 10: return CharacterSlot.Hands;
            case 6: return CharacterSlot.Waist;
            case 7: return CharacterSlot.Legs;
            case 8: return CharacterSlot.Feet;
            case 11: return CharacterSlot.Finger1;
            case 12: return CharacterSlot.Finger2;
            case 13: return CharacterSlot.Trinket1;
            case 14: return CharacterSlot.Trinket2;
            case 16: return CharacterSlot.MainHand;
            case 17: return CharacterSlot.OffHand;
            case 18: return CharacterSlot.Ranged;
            case 0: return CharacterSlot.Projectile;
            case 102: return CharacterSlot.ProjectileBag;
            default: return CharacterSlot.None;
        }
    }

    // C# Character.cs:148-167
    public List<GemmingTemplate> getCustomGemmingTemplates() {
        return customGemmingTemplates;
    }
    public void setCustomGemmingTemplates(List<GemmingTemplate> customGemmingTemplates) {
        this.customGemmingTemplates = customGemmingTemplates;
    }

    /**
     * The gemming templates of the current model: the built-in templates patched by the saved
     * per-character overrides. The list is cached and regenerated when the current model changes
     * (templates are model-specific); the previous model's overrides are persisted first. Each
     * returned entry is a copy, so callers may change enabled flags without corrupting the global
     * defaults.
     */
    public List<GemmingTemplate> getCurrentGemmingTemplates() {
        if (currentGemmingTemplates == null || !java.util.Objects.equals(getCurrentModel(), gemmingTemplateModel)) {
            saveGemmingTemplateOverrides();
            generateGemmingTemplates();
        }
        return currentGemmingTemplates;
    }

    // C# Character.cs:169-188
    private void saveGemmingTemplateOverrides() {
        if (currentGemmingTemplates == null) return;
        List<GemmingTemplate> defaults = GemmingTemplate.getAllTemplates().get(gemmingTemplateModel);
        gemmingTemplateOverrides.removeIf(template -> java.util.Objects.equals(template.getModel(), gemmingTemplateModel));
        for (GemmingTemplate template : defaults) {
            for (GemmingTemplate overrideTemplate : currentGemmingTemplates) {
                if (sameGemmingTemplate(template, overrideTemplate)) {
                    if (template.getEnabled() != overrideTemplate.getEnabled()) {
                        gemmingTemplateOverrides.add(overrideTemplate);
                        break;
                    }
                }
            }
        }
    }

    // C# Character.cs:190-218
    private void generateGemmingTemplates() {
        List<GemmingTemplate> defaults = GemmingTemplate.getCurrentTemplates();
        currentGemmingTemplates = new ArrayList<GemmingTemplate>();
        for (GemmingTemplate template : defaults) {
            GemmingTemplate toCopy = template;
            for (GemmingTemplate overrideTemplate : gemmingTemplateOverrides) {
                if (sameGemmingTemplate(template, overrideTemplate)) {
                    toCopy = overrideTemplate;
                    break;
                }
            }
            GemmingTemplate copy = new GemmingTemplate();
            copy.setBlueId(toCopy.getBlueId());
            copy.setEnabled(toCopy.getEnabled());
            copy.setGroup(toCopy.getGroup());
            copy.setMetaId(toCopy.getMetaId());
            copy.setModel(toCopy.getModel());
            copy.setPrismaticId(toCopy.getPrismaticId());
            copy.setRedId(toCopy.getRedId());
            copy.setYellowId(toCopy.getYellowId());
            currentGemmingTemplates.add(copy);
        }
        gemmingTemplateModel = getCurrentModel();
    }

    private static boolean sameGemmingTemplate(GemmingTemplate a, GemmingTemplate b) {
        return java.util.Objects.equals(a.getGroup(), b.getGroup())
                && a.getBlueId() == b.getBlueId()
                && a.getMetaId() == b.getMetaId()
                && java.util.Objects.equals(a.getModel(), b.getModel())
                && a.getPrismaticId() == b.getPrismaticId()
                && a.getRedId() == b.getRedId()
                && a.getYellowId() == b.getYellowId();
    }

    // C# Character.cs:222-229. Rebuilds the saved override tree from the global ItemFilter state
    // (called by save()). The "Other" entries carry the per-level Other/OtherRegexEnabled flags.
    private void saveItemFilterEnabledOverride() {
        itemFilterEnabledOverride = new ArrayList<ItemFilterEnabledOverride>();
        for (ItemFilterRegex itemFilter : ItemFilter.getFilterList()) {
            saveItemFilterEnabledOverride(itemFilter, itemFilterEnabledOverride);
        }
        ItemFilterEnabledOverride other = new ItemFilterEnabledOverride();
        other.setName("Other");
        other.setEnabled(ItemFilter.isOtherEnabled());
        itemFilterEnabledOverride.add(other);
    }

    // C# Character.cs:234-249
    private void saveItemFilterEnabledOverride(ItemFilterRegex itemFilter, List<ItemFilterEnabledOverride> list) {
        ItemFilterEnabledOverride filterOverride = new ItemFilterEnabledOverride();
        filterOverride.setName(itemFilter.getName());
        filterOverride.setEnabled(itemFilter.isEnabled());
        if (!itemFilter.regexList.isEmpty()) {
            filterOverride.setSubFilterOverride(new ArrayList<ItemFilterEnabledOverride>());
            for (ItemFilterRegex subFilter : itemFilter.regexList) {
                saveItemFilterEnabledOverride(subFilter, filterOverride.getSubFilterOverride());
            }
            ItemFilterEnabledOverride otherSub = new ItemFilterEnabledOverride();
            otherSub.setName("Other");
            otherSub.setEnabled(itemFilter.otherRegexEnabled);
            filterOverride.getSubFilterOverride().add(otherSub);
        }
        list.add(filterOverride);
    }

    // C# Character.cs:251-273. Applies the loaded override tree to the global ItemFilter state;
    // returns whether any flag changed (the C# app then refreshes the item cache).
    /**
     * Applies the per-character item-filter override tree (read from the save file) to the global
     * item-filter state. Returns true when at least one flag changed, in which case the caller
     * must refresh derived item data.
     */
    public boolean loadItemFilterEnabledOverride() {
        boolean triggerEvent = false;
        if (itemFilterEnabledOverride == null || itemFilterEnabledOverride.isEmpty()) return false;
        ItemFilter.setLoading(true);
        for (ItemFilterEnabledOverride filterOverride : itemFilterEnabledOverride) {
            if (!"Other".equals(filterOverride.getName())) {
                triggerEvent = loadItemFilterEnabledOverride(filterOverride, ItemFilter.getFilterList(), triggerEvent);
            } else {
                if (ItemFilter.isOtherEnabled() != filterOverride.isEnabled()) {
                    ItemFilter.setOtherEnabled(filterOverride.isEnabled());
                    triggerEvent = true;
                }
            }
        }
        ItemFilter.setLoading(false);
        return triggerEvent;
    }

    // C# Character.cs:275-301 (the C# ref bool triggerEvent becomes a returned value).
    private static boolean loadItemFilterEnabledOverride(ItemFilterEnabledOverride filterOverride, ItemFilterRegexList list, boolean triggerEvent) {
        for (ItemFilterRegex itemFilter : list) {
            if (java.util.Objects.equals(itemFilter.getName(), filterOverride.getName())) {
                if (itemFilter.isEnabled() != filterOverride.isEnabled()) {
                    itemFilter.setEnabled(filterOverride.isEnabled());
                    triggerEvent = true;
                }
                if (filterOverride.getSubFilterOverride() != null && !filterOverride.getSubFilterOverride().isEmpty()) {
                    for (ItemFilterEnabledOverride subOverride : filterOverride.getSubFilterOverride()) {
                        if (!"Other".equals(subOverride.getName())) {
                            triggerEvent = loadItemFilterEnabledOverride(subOverride, itemFilter.regexList, triggerEvent);
                        } else {
                            if (itemFilter.otherRegexEnabled != subOverride.isEnabled()) {
                                itemFilter.otherRegexEnabled = subOverride.isEnabled();
                                triggerEvent = true;
                            }
                        }
                    }
                }
                return triggerEvent;
            }
        }
        return triggerEvent;
    }

    // C# Character.cs:220 (auto-property)
    public List<ItemFilterEnabledOverride> getItemFilterEnabledOverride() {
        return itemFilterEnabledOverride;
    }

    public void setItemFilterEnabledOverride(List<ItemFilterEnabledOverride> itemFilterEnabledOverride) {
        this.itemFilterEnabledOverride = itemFilterEnabledOverride;
    }

    // C# Character.cs:148-149 auto-properties
    public List<GemmingTemplate> getGemmingTemplateOverrides() {
        return gemmingTemplateOverrides;
    }

    public void setGemmingTemplateOverrides(List<GemmingTemplate> gemmingTemplateOverrides) {
        this.gemmingTemplateOverrides = gemmingTemplateOverrides;
    }

    // C# Character.cs:840-884
    public boolean getWaistBlacksmithingSocketEnabled() {
        return waistBlacksmithingSocketEnabled;
    }

    public void setWaistBlacksmithingSocketEnabled(boolean value) {
        waistBlacksmithingSocketEnabled = value;
        onCalculationsInvalidated();
    }

    public boolean getHandsBlacksmithingSocketEnabled() {
        return handsBlacksmithingSocketEnabled;
    }

    public void setHandsBlacksmithingSocketEnabled(boolean value) {
        handsBlacksmithingSocketEnabled = value;
        onCalculationsInvalidated();
    }

    public boolean getWristBlacksmithingSocketEnabled() {
        return wristBlacksmithingSocketEnabled;
    }

    public void setWristBlacksmithingSocketEnabled(boolean value) {
        wristBlacksmithingSocketEnabled = value;
        onCalculationsInvalidated();
    }
}
