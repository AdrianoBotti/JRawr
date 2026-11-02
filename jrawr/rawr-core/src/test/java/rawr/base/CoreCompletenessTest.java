// Golden-vs-legacy checks for the members closed by the Rawr.Base completeness audit
// (tools/core-completeness). Expected values are produced by tools/core-completeness/probe
// against the prebuilt legacy Rawr.Base.dll.
package rawr.base;

import rawr.base.calculations.Calculations;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterFaction;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.character.Profession;
import rawr.base.effects.SpecialFunction;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreCompletenessTest {

    private static List<String> loadExpected() throws Exception {
        InputStream in = CoreCompletenessTest.class.getResourceAsStream("/core-completeness/expected.txt");
        assertNotNull(in, "core-completeness/expected.txt missing");
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    @Test
    void specialFunctionNormalMatchesLegacy() throws Exception {
        int checked = 0;
        for (String line : loadExpected()) {
            String[] parts = line.split(" ");
            if (!parts[0].equals("normal")) continue;
            double x = Double.parseDouble(parts[1]);
            double expected = Double.parseDouble(parts[2]);
            // Commons Math normal() differs from Cephes by a few ULPs (DEVIATIONS D5).
            assertEquals(expected, SpecialFunction.normal(x), Math.abs(expected) * 1e-14, "normal(" + x + ")");
            checked++;
        }
        assertEquals(15, checked);
    }

    @Test
    void statNamesMatchLegacy() throws Exception {
        List<String> expected = new ArrayList<String>();
        for (String line : loadExpected()) {
            if (line.startsWith("statname ")) expected.add(line.substring("statname ".length()));
        }
        List<String> actual = new ArrayList<String>();
        for (String name : Stats.getStatNames()) {
            actual.add(name.replace("\r", "\\r").replace("\n", "\\n"));
        }
        // Every name the shipped DLL exposes must be present...
        List<String> missing = new ArrayList<String>(expected);
        for (String name : actual) missing.remove(name);
        assertEquals(List.of(), missing, "DLL stat names missing from the Java port");
        // ...and the only extras are the C# StatsCat/StatsBear subclass properties, which this port
        // models as the Java-only SingleSourceStat combination category (docs/dev/DECISIONS.md).
        List<String> extra = new ArrayList<String>(actual);
        for (String name : expected) extra.remove(name);
        java.util.Collections.sort(extra);
        assertEquals(KNOWN_EXTRA_STAT_NAMES, extra, "unexpected extra Java stat names");
    }

    // The C# StatsCat/StatsBear properties: single-source model constants that this port keeps in
    // Stats as the SingleSourceStat category instead of a model subclass.
    private static final List<String> KNOWN_EXTRA_STAT_NAMES = java.util.List.of(
            " Bonus C P On Crit",
            " Bonus Energy On Tigers Fury",
            " Finisher Energy On Avoid",
            " Mangle Cooldown Reduction",
            " Rake Cost Reduction",
            " Shred Cost Reduction",
            "% Enrage Dmg",
            "% Ferocious Bite Dmg",
            "% Mangle Dmg",
            "% Maul Dmg",
            "% Rake Dmg",
            "% Shred Dmg",
            "% Swipe Dmg");

    @Test
    void itemSlotByCharacterSlotMatchesLegacy() throws Exception {
        for (String line : loadExpected()) {
            String[] parts = line.split(" ");
            if (!parts[0].equals("itemslot")) continue;
            CharacterSlot slot = CharacterSlot.valueOf(parts[1]);
            assertEquals(ItemSlot.valueOf(parts[2]), Item.getItemSlotByCharacterSlot(slot), "slot " + parts[1]);
        }
    }

    @Test
    void defaultSlotMapMatchesLegacy() throws Exception {
        Map<String, String> expected = new LinkedHashMap<String, String>();
        for (String line : loadExpected()) {
            String[] parts = line.split(" ");
            if (!parts[0].equals("slotmap")) continue;
            expected.put(parts[1], parts[2]);
        }
        Map<ItemSlot, CharacterSlot> actual = Item.getDefaultSlotMap();
        assertEquals(expected.size(), actual.size());
        for (Map.Entry<ItemSlot, CharacterSlot> entry : actual.entrySet()) {
            String want = expected.get(entry.getKey().name());
            assertNotNull(want, "unexpected ItemSlot " + entry.getKey());
            assertEquals(want, entry.getValue().name(), "slotmap " + entry.getKey());
        }
    }

    @Test
    void characterSlotFromIdMatchesLegacy() throws Exception {
        for (String line : loadExpected()) {
            String[] parts = line.split(" ");
            if (!parts[0].equals("slotid")) continue;
            int id = Integer.parseInt(parts[1]);
            assertEquals(CharacterSlot.valueOf(parts[2]), Character.getCharacterSlotFromId(id), "slotid " + id);
        }
    }

    @Test
    void isEquippedOverloads() {
        Character character = new Character();
        Item head = new Item();
        head.setId(1001);
        head.setSlot(ItemSlot.Head);
        ItemInstance headInstance = new ItemInstance(1001, 0, 0, 0, 0);
        headInstance.setItem(head);

        assertFalse(character.isEquipped(headInstance));
        assertFalse(character.isEquipped(head));
        // IsLoading keeps the instance alive across the indexer's invalidation (same as C# callers
        // that hold a reference); the recreate-on-set path is covered by the golden
        // RelevantItemInstancesGoldenTest.cacheInvalidationAndStableOrder.
        character.setIsLoading(true);
        character.set(CharacterSlot.Head, headInstance);
        character.setIsLoading(false);
        assertTrue(character.isEquipped(headInstance, CharacterSlot.Head));
        assertTrue(character.isEquipped(headInstance));
        assertTrue(character.isEquipped(head, CharacterSlot.Head));
        assertTrue(character.isEquipped(head));
        assertFalse(character.isEquipped(headInstance, CharacterSlot.Neck));
    }

    @Test
    void isEquippedCoversFingerPair() {
        Character character = new Character();
        Item ring = new Item();
        ring.setId(2002);
        ring.setSlot(ItemSlot.Finger);
        ItemInstance ringInstance = new ItemInstance(2002, 0, 0, 0, 0);
        ringInstance.setItem(ring);
        character.setIsLoading(true);
        character.set(CharacterSlot.Finger2, ringInstance);
        character.setIsLoading(false);
        assertTrue(character.isEquipped(ringInstance));
        assertTrue(character.isEquipped(ring));
    }

    @Test
    void getAllEquippedAndAvailableGearIds() {
        Character character = new Character();
        Item head = new Item();
        head.setId(1001);
        head.setSlot(ItemSlot.Head);
        ItemInstance headInstance = new ItemInstance(1001, 0, 0, 0, 0);
        headInstance.setItem(head);
        character.set(CharacterSlot.Head, headInstance);
        character.setAvailableItems(new java.util.ArrayList<String>(
                java.util.Arrays.asList("1001", "-678", "999.*.*.*")));
        String[] ids = character.getAllEquippedAndAvailableGearIds();
        assertEquals(java.util.List.of("1001.0.0.0.0", "1001", "999.*.*"), java.util.List.of(ids));
    }

    @Test
    void itemInstanceDisplayDelegatesToItem() {
        Item item = new Item();
        item.setId(3003);
        item.setDisplayId(77);
        item.setDisplaySlot(42);
        ItemInstance instance = new ItemInstance(3003, 0, 0, 0, 0);
        instance.setItem(item);
        assertEquals(77, instance.getDisplayId());
        assertEquals(42, instance.getDisplaySlot());
        assertEquals(0, new ItemInstance(0, 0, 0, 0, 0).getDisplayId());
    }

    @Test
    void itemDeleteFlagsInvalid() {
        Item item = new Item();
        assertFalse(item.isInvalid());
        item.delete();
        assertTrue(item.isInvalid());
    }

    @Test
    void factionFollowsRace() {
        // setRace -> OnCalculationsInvalidated -> RecalculatePassiveBonuses looks up Heroic Presence
        // (C# Character.cs:1876-1882), so the buff registry must exist (empty is fine).
        Buff.load(new java.util.ArrayList<Buff>());
        Character character = new Character();
        assertEquals(CharacterFaction.Alliance, character.getFaction());
        character.setRace(CharacterRace.Orc);
        assertEquals(CharacterFaction.Horde, character.getFaction());
        character.setRace(CharacterRace.Draenei);
        assertEquals(CharacterFaction.Alliance, character.getFaction());
    }

    @Test
    void professionSettersAndValidationTolerateEmptyBuffCache() {
        Character character = new Character();
        // No buff cache is loaded here: setting a profession that triggers a buff lookup would NPE in
        // the original too (Buff.GetBuffByName requires the loaded cache), so only the neutral paths
        // are exercised; the buff mutations are covered by the golden fixtures.
        character.setPrimaryProfession(Profession.None);
        character.setSecondaryProfession(Profession.None);
        character.validateActiveBuffs();
        Calculations.updateProfessions(character);
        assertEquals(Profession.None, character.getPrimaryProfession());
        assertEquals(Profession.None, character.getSecondaryProfession());
    }
}
