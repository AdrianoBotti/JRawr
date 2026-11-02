package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.character.Profession;
import rawr.base.talents.DruidTalents;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemQuality;
import rawr.base.talents.HunterTalents;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.WarriorTalents;
import rawr.base.talents.WarlockTalents;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rawr.data.CharacterFixtureLoader;

/**
 * Session-scoped character editor state. Builds the view data for the paper-doll, the item
 * picker, the buff list and the talent editor, mutates the live {@link Character} held in the
 * session, and tracks unsaved changes (persistence is {@link CharacterStore}'s job).
 */
@Service
public class EditorService {
    static final String SESSION_CHARACTER = "jrawr.character";
    /** Last submitted optimizer form; cleared whenever the session character is replaced. */
    public static final String SESSION_OPTIMIZER_SETTINGS = "jrawr.optimizer.settings";
    static final String SESSION_DIRTY = "jrawr.characterDirty";
    static final String SESSION_FILE = "jrawr.characterFile";

    public static final List<CharacterSlot> LEFT_SLOTS = List.of(
            CharacterSlot.Head,
            CharacterSlot.Neck,
            CharacterSlot.Shoulders,
            CharacterSlot.Back,
            CharacterSlot.Chest,
            CharacterSlot.Shirt,
            CharacterSlot.Tabard,
            CharacterSlot.Wrist);
    public static final List<CharacterSlot> RIGHT_SLOTS = List.of(
            CharacterSlot.Hands,
            CharacterSlot.Waist,
            CharacterSlot.Legs,
            CharacterSlot.Feet,
            CharacterSlot.Finger1,
            CharacterSlot.Finger2,
            CharacterSlot.Trinket1,
            CharacterSlot.Trinket2);
    public static final List<CharacterSlot> BOTTOM_SLOTS = List.of(
            CharacterSlot.MainHand,
            CharacterSlot.OffHand,
            CharacterSlot.Ranged);

    private static final Map<CharacterSlot, String> SLOT_LABELS = slotLabels();

    public Character getOrCreate(HttpSession session) {
        Character character = (Character) session.getAttribute(SESSION_CHARACTER);
        if (character == null) {
            character = newCharacter();
            session.setAttribute(SESSION_CHARACTER, character);
        }
        return character;
    }

    public Character newCharacter() {
        Character character = new Character();
        character.setName("New character");
        character.setRace(CharacterRace.Human);
        character.setCharacterClass(CharacterClass.Mage);
        character.setCurrentModel("");
        return character;
    }

    /** Replaces the session character with a fresh empty one and clears the dirty flag. */
    public void startNew(HttpSession session) {
        session.setAttribute(SESSION_CHARACTER, newCharacter());
        session.removeAttribute(SESSION_FILE);
        session.removeAttribute(SESSION_OPTIMIZER_SETTINGS);
        clearDirty(session);
    }

    /** Puts a loaded character in the session and clears the dirty flag. */
    public void openCharacter(HttpSession session, Character character, String fileName) {
        session.setAttribute(SESSION_CHARACTER, character);
        session.setAttribute(SESSION_FILE, fileName);
        session.removeAttribute(SESSION_OPTIMIZER_SETTINGS);
        clearDirty(session);
    }

    // ------------------------------------------------------------------ dirty tracking

    public boolean isDirty(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_DIRTY));
    }

    public void markDirty(HttpSession session) {
        session.setAttribute(SESSION_DIRTY, Boolean.TRUE);
    }

    public void clearDirty(HttpSession session) {
        session.setAttribute(SESSION_DIRTY, Boolean.FALSE);
    }

    public String currentFile(HttpSession session) {
        return (String) session.getAttribute(SESSION_FILE);
    }

    public void setCurrentFile(HttpSession session, String fileName) {
        session.setAttribute(SESSION_FILE, fileName);
    }

    public Character loadFromXml(String xml) throws Exception {
        Path tempFile = Files.createTempFile("jrawr-web-character-", ".xml");
        try {
            Files.writeString(tempFile, xml, StandardCharsets.UTF_8);
            return CharacterFixtureLoader.load(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ------------------------------------------------------------------ meta

    public void applyMeta(Character character, String name, String raceName, String modelName) {
        boolean itemsChanged = false;
        if (name != null && !name.isBlank()) {
            character.setName(name.trim());
        }
        if (raceName != null && !raceName.isBlank()) {
            try {
                CharacterRace race = CharacterRace.valueOf(raceName);
                if (race != character.getRace()) {
                    character.setRace(race);
                    itemsChanged = true;
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown race: " + raceName);
            }
        }
        if (modelName == null || modelName.isBlank()) {
            // model not chosen yet: keep the current (possibly empty) model
            if (itemsChanged) {
                character.invalidateItemInstances();
            }
            return;
        }
        if (!CalculationService.MODELS.contains(modelName)) {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }
        if (!modelName.equals(character.getCurrentModel())) {
            itemsChanged = true;
        }
        character.setCurrentModel(modelName);
        character.setCharacterClass(classForModel(modelName));
        if (itemsChanged) {
            // Relevant item instances are cached per slot but derived from the model/class/race.
            character.invalidateItemInstances();
        }
    }

    /**
     * Applies the two profession selects. The caller must have loaded the character's model first
     * when one is chosen: {@code Character.setPrimaryProfession} delegates to
     * {@code Calculations.updateProfessions}, which uses the globally loaded model instance.
     */
    public void applyProfessions(Character character, String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            character.setPrimaryProfession(parseProfession(primary));
        }
        if (secondary != null && !secondary.isBlank()) {
            character.setSecondaryProfession(parseProfession(secondary));
        }
    }

    private static Profession parseProfession(String value) {
        try {
            return Profession.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown profession: " + value);
        }
    }

    private static CharacterClass classForModel(String modelName) {
        return switch (modelName) {
            case "Mage" -> CharacterClass.Mage;
            case "RestoSham", "Elemental", "Enhance" -> CharacterClass.Shaman;
            case "Cat", "Bear", "Moonkin", "Tree" -> CharacterClass.Druid;
            case "Healadin", "Retribution", "ProtPaladin" -> CharacterClass.Paladin;
            case "HealPriest", "ShadowPriest" -> CharacterClass.Priest;
            case "ProtWarr", "DPSWarr" -> CharacterClass.Warrior;
            case "TankDK", "DPSDK" -> CharacterClass.DeathKnight;
            case "Hunter" -> CharacterClass.Hunter;
            case "Rogue" -> CharacterClass.Rogue;
            case "Warlock" -> CharacterClass.Warlock;
            default -> throw new IllegalArgumentException("Unknown model: " + modelName);
        };
    }

    // ------------------------------------------------------------------ gear summary

    /**
     * Small, gear-specific summary for the middle of the paper doll: average item level, equipped
     * slot count and active set bonuses. Generic identity data (name, race, class/model and
     * professions) lives in the character header instead.
     */
    public GearSummaryView gearSummary(Character character) {
        character.recalculateSetBonuses();
        int equipped = 0;
        for (CharacterSlot slot : CalculationService.AVERAGE_ITEM_LEVEL_SLOTS) {
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                equipped++;
            }
        }
        List<GearSetView> sets = new ArrayList<GearSetView>();
        Map<String, Integer> counts = character.getSetBonusCount();
        if (counts != null) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int pieces = entry.getValue();
                List<String> activeBonuses = new ArrayList<String>();
                Buff[] definitions = Buff.getSetBonuses(entry.getKey());
                if (definitions != null) {
                    for (Buff definition : definitions) {
                        if (pieces >= definition.setThreshold) {
                            activeBonuses.add(definition.setThreshold + "-piece");
                        }
                    }
                }
                if (!activeBonuses.isEmpty()) {
                    sets.add(new GearSetView(entry.getKey(), pieces, List.copyOf(activeBonuses)));
                }
            }
        }
        return new GearSummaryView(
                CalculationService.averageItemLevel(character),
                equipped,
                CalculationService.AVERAGE_ITEM_LEVEL_SLOTS.length,
                List.copyOf(sets));
    }

    // ------------------------------------------------------------------ slots

    public List<SlotView> slotViews(Character character, List<CharacterSlot> slots) {
        List<SlotView> views = new ArrayList<SlotView>(slots.size());
        for (CharacterSlot slot : slots) {
            views.add(slotView(character, slot));
        }
        return views;
    }

    private SlotView slotView(Character character, CharacterSlot slot) {
        ItemInstance instance = character.get(slot);
        Item item = instance == null ? null : instance.getItem();
        if (item == null) {
            return new SlotView(slot, SLOT_LABELS.get(slot), null, null, null, 0, 0, null, false, List.of(), 0, null);
        }
        int gemCount = 0;
        if (instance.getGem1Id() != 0) gemCount++;
        if (instance.getGem2Id() != 0) gemCount++;
        if (instance.getGem3Id() != 0) gemCount++;
        String enchantName = null;
        if (instance.getEnchantId() != 0) {
            Enchant enchant = EnchantService.find(instance.getEnchantId());
            if (enchant != null && instance.isEnchantEligible(enchant)) {
                enchantName = enchant.name;
            }
        }
        return new SlotView(slot, SLOT_LABELS.get(slot), instance, item.getName(),
                qualityKey(item.getQuality()), item.getItemLevel(), gemCount, enchantName,
                EnchantService.hasOptions(instance), GemService.sockets(character, slot, instance),
                item.getId(), item.getIconPath());
    }

    private static String qualityKey(ItemQuality quality) {
        return quality == null ? "common" : quality.name().toLowerCase(Locale.ROOT);
    }

    public boolean hasAnyItem(Character character) {
        for (ItemInstance item : character.getItems()) {
            if (item != null) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ buffs

    public List<BuffGroupView> buffGroups(Character character) {
        Map<String, List<BuffView>> groups = new LinkedHashMap<String, List<BuffView>>();
        for (Buff buff : Buff.getAllBuffs()) {
            String group = buff.group == null || buff.group.isBlank() ? "Other" : buff.group;
            boolean setBonus = buff.setName != null && !buff.setName.isEmpty() || "Set Bonuses".equals(group);
            if (setBonus && !character.activeBuffsContains(buff.name)) {
                continue;
            }
            boolean parentActive = character.activeBuffsContains(buff.name);
            List<BuffView> views = groups.computeIfAbsent(group, key -> new ArrayList<BuffView>());
            views.add(new BuffView(buff.name, buff.source, parentActive,
                    isLocked(character, buff, setBonus), false, buff.stats.toString()));
            for (Buff improvement : buff.improvements) {
                boolean active = character.activeBuffsContains(improvement.name);
                boolean locked = !parentActive || isLocked(character, improvement, setBonus);
                views.add(new BuffView(improvement.name, improvement.source, active, locked, true,
                        improvement.stats.toString()));
            }
        }
        List<BuffGroupView> result = new ArrayList<BuffGroupView>(groups.size());
        for (Map.Entry<String, List<BuffView>> entry : groups.entrySet()) {
            result.add(new BuffGroupView(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static boolean isLocked(Character character, Buff buff, boolean setBonus) {
        if (setBonus) return true;
        if ("Profession Buffs".equals(buff.group)) return true;
        if ("Heroic Presence".equals(buff.name) && character.getRace() != CharacterRace.Draenei) return true;
        return false;
    }

    public void toggleBuff(Character character, String name, boolean active) {
        Buff buff = Buff.getBuffByName(name);
        if (buff == null) {
            throw new IllegalArgumentException("Unknown buff: " + name);
        }
        List<Buff> activeBuffs = character.getActiveBuffs();
        if (active) {
            List<String> conflicts = buff.getConflictingBuffs();
            List<Buff> conflictingActive = new ArrayList<Buff>();
            for (Buff other : activeBuffs) {
                if (other != buff && intersects(other.getConflictingBuffs(), conflicts)) {
                    conflictingActive.add(other);
                }
            }
            for (Buff other : conflictingActive) {
                removeBuffAndImprovements(activeBuffs, other);
            }
            if (!character.activeBuffsContains(name)) {
                character.activeBuffsAdd(buff);
            }
        } else {
            removeBuffAndImprovements(activeBuffs, buff);
        }
    }

    /**
     * Removes a buff and every upgraded version nested under it. The UI only enables improvements
     * while their base buff is active, so turning the base off must not leave an orphaned
     * improvement in the active list (e.g. Demoralizing Shout -> Improved Demoralizing Shout).
     */
    private static void removeBuffAndImprovements(List<Buff> activeBuffs, Buff buff) {
        activeBuffs.removeIf(other -> buff.name.equals(other.name));
        for (Buff improvement : buff.improvements) {
            removeBuffAndImprovements(activeBuffs, improvement);
        }
    }

    /**
     * Clears every selected buff, then restores the automatic ones (race/profession buffs and set
     * bonuses implied by the equipped gear) so the result is a clean, valid buff state.
     */
    public void resetBuffs(Character character) {
        character.getActiveBuffs().clear();
        character.validateActiveBuffs();
        character.recalculateSetBonuses();
    }

    private static boolean intersects(List<String> left, List<String> right) {
        for (String value : left) {
            if (value != null && right.contains(value)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ talents

    public void applyTalents(Character character, String modelName, String talents) {
        String model = character.getCurrentModel();
        if (modelName != null && !modelName.equals(model)) {
            throw new IllegalArgumentException("Model changed, reload the page");
        }
        String value = talents == null ? "" : talents.trim();
        if ("Mage".equals(model)) {
            character.setMageTalents(new MageTalents(value));
        } else if ("RestoSham".equals(model) || "Elemental".equals(model) || "Enhance".equals(model)) {
            character.setShamanTalents(new ShamanTalents(value));
        } else if ("Cat".equals(model) || "Bear".equals(model) || "Moonkin".equals(model) || "Tree".equals(model)) {
            character.setDruidTalents(new DruidTalents(value));
        } else if ("Healadin".equals(model) || "Retribution".equals(model) || "ProtPaladin".equals(model)) {
            character.setPaladinTalents(new PaladinTalents(value));
        } else if ("HealPriest".equals(model) || "ShadowPriest".equals(model)) {
            character.setPriestTalents(new PriestTalents(value));
        } else if ("ProtWarr".equals(model) || "DPSWarr".equals(model)) {
            character.setWarriorTalents(new WarriorTalents(value));
        } else if ("Hunter".equals(model)) {
            character.setHunterTalents(new HunterTalents(value));
        } else if ("Rogue".equals(model)) {
            character.setRogueTalents(new RogueTalents(value));
        } else if ("TankDK".equals(model) || "DPSDK".equals(model)) {
            character.setDeathKnightTalents(new DeathKnightTalents(value));
        } else if ("Warlock".equals(model)) {
            character.setWarlockTalents(new WarlockTalents(value));
        } else {
            throw new IllegalArgumentException("Unknown model: " + model);
        }
    }

    // ------------------------------------------------------------------ view records

    /** Active set bonus summary: set name, equipped pieces and active thresholds ("2-piece"). */
    public record GearSetView(String name, int pieces, List<String> activeBonuses) {
    }

    /** Gear-only summary rendered in the middle of the paper doll. */
    public record GearSummaryView(
            String averageItemLevel,
            int equipped,
            int slots,
            List<GearSetView> sets) {

        public boolean hasSets() {
            return !sets.isEmpty();
        }
    }

    /** Human label for a slot ("Finger 1", "Main Hand", ...); falls back to the enum name. */
    public static String slotLabel(CharacterSlot slot) {
        String label = SLOT_LABELS.get(slot);
        return label != null ? label : slot.name();
    }

    private static Map<CharacterSlot, String> slotLabels() {
        Map<CharacterSlot, String> labels = new LinkedHashMap<CharacterSlot, String>();
        labels.put(CharacterSlot.Head, "Head");
        labels.put(CharacterSlot.Neck, "Neck");
        labels.put(CharacterSlot.Shoulders, "Shoulders");
        labels.put(CharacterSlot.Back, "Back");
        labels.put(CharacterSlot.Chest, "Chest");
        labels.put(CharacterSlot.Shirt, "Shirt");
        labels.put(CharacterSlot.Tabard, "Tabard");
        labels.put(CharacterSlot.Wrist, "Wrist");
        labels.put(CharacterSlot.Hands, "Hands");
        labels.put(CharacterSlot.Waist, "Waist");
        labels.put(CharacterSlot.Legs, "Legs");
        labels.put(CharacterSlot.Feet, "Feet");
        labels.put(CharacterSlot.Finger1, "Finger 1");
        labels.put(CharacterSlot.Finger2, "Finger 2");
        labels.put(CharacterSlot.Trinket1, "Trinket 1");
        labels.put(CharacterSlot.Trinket2, "Trinket 2");
        labels.put(CharacterSlot.MainHand, "Main Hand");
        labels.put(CharacterSlot.OffHand, "Off Hand");
        labels.put(CharacterSlot.Ranged, "Ranged");
        return Collections.unmodifiableMap(labels);
    }

    public record SlotView(
            CharacterSlot slot,
            String label,
            ItemInstance instance,
            String itemName,
            String qualityKey,
            int itemLevel,
            int gemCount,
            String enchantName,
            boolean enchantable,
            List<GemService.SocketView> sockets,
            int itemId,
            String iconPath) {

        public boolean filled() {
            return instance != null && itemName != null;
        }

        public boolean enchanted() {
            return enchantName != null;
        }

        public String enchantText() {
            return enchantName != null ? enchantName : "Add enchant";
        }

        public String metaText() {
            StringBuilder text = new StringBuilder("ilvl ").append(itemLevel);
            if (gemCount > 0) {
                text.append(" · ").append(gemCount).append(gemCount == 1 ? " gem" : " gems");
            }
            return text.toString();
        }
    }

    public record BuffView(
            String name,
            String source,
            boolean active,
            boolean locked,
            boolean improvement,
            String stats) {
    }

    public record BuffGroupView(String group, List<BuffView> buffs) {
        public boolean hasActive() {
            for (BuffView buff : buffs) {
                if (buff.active()) return true;
            }
            return false;
        }
    }
}
