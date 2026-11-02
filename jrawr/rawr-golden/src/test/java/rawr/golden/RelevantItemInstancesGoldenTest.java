package rawr.golden;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.data.ItemSourceLoader;
import rawr.models.mage.CalculationsMage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-master probe for the Character relevant-items cluster
 * (GetRelevantItemInstances/GetRelevantItems/ClearRelevantGems/InvalidateItemInstances/
 * CustomItemInstances): the expected sorted gemmed ids / item ids were produced by the shipped
 * Rawr.Base.dll via tools/core-completeness/probe/RelevantItemsProbe.cs. Fixtures are opened the
 * way the C# app does (apply the saved ItemFilter override tree, then RecalculateSetBonuses) and
 * the shipped ItemFilter.xml/ItemSource.xml are loaded like ItemCache.Load() does.
 */
class RelevantItemInstancesGoldenTest {
    private static final CharacterSlot[] INSTANCE_SLOTS = {
            CharacterSlot.Head, CharacterSlot.Waist, CharacterSlot.Finger1,
            CharacterSlot.MainHand, CharacterSlot.OffHand, CharacterSlot.Gems,
    };
    private static final ItemSlot[] COLOURS = { ItemSlot.None, ItemSlot.Red, ItemSlot.Yellow, ItemSlot.Blue };

    private static Path fixturesDir;
    private static Path gameData;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/mage");

        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
        // The C# probe runs the shipped ItemCache.Load() which also loads Data/ItemSource.xml and
        // Data/ItemFilter.xml (the filter regexes match against Item.LocationInfo).
        ItemSourceLoader.load(gameData.resolve("ItemSource.xml"));
        ItemFilter.load(gameData.resolve("ItemFilter.xml"));

        CalculationsMage.register();
        Calculations.loadModel(CalculationsMage.class);
    }

    @Test
    void relevantItemsMatchLegacyProbe() throws Exception {
        Map<String, List<String>> expected = readExpected();
        List<String> fixtures = new ArrayList<String>();
        for (String key : expected.keySet()) {
            String fixture = key.substring(0, key.indexOf('|'));
            if (!fixtures.contains(fixture)) fixtures.add(fixture);
        }

        for (String fixture : fixtures) {
            Character character = probeCharacter(fixture);
            List<String> actualRelevant = Arrays.stream(ItemCache.relevantItems())
                    .map(item -> Integer.toString(item.getId())).sorted().toList();
            assertIdsMatch(fixture, "relevant", expected.get(fixture + "|relevant"), actualRelevant);
            for (CharacterSlot slot : INSTANCE_SLOTS) {
                List<String> actual = character.getRelevantItemInstances(slot).stream()
                        .map(ItemInstance::getGemmedId).sorted().toList();
                assertIdsMatch(fixture, "instances " + slot, expected.get(fixture + "|instances " + slot), actual);
            }
            for (ItemSlot colour : COLOURS) {
                character.clearRelevantGems();
                List<String> actual = character.getRelevantItems(CharacterSlot.Gems, colour).stream()
                        .map(item -> Integer.toString(item.getId())).sorted().toList();
                assertIdsMatch(fixture, "gems " + colour, expected.get(fixture + "|gems " + colour), actual);
            }
            List<String> actual = character.getRelevantItems(CharacterSlot.Head).stream()
                    .map(item -> Integer.toString(item.getId())).sorted().toList();
            assertIdsMatch(fixture, "items Head", expected.get(fixture + "|items Head"), actual);
        }
    }

    @Test
    void cacheInvalidationAndStableOrder() throws Exception {
        Character character = probeCharacter("arcane_277_raid");
        List<ItemInstance> first = character.getRelevantItemInstances(CharacterSlot.Head);
        List<String> firstIds = sortedGemmedIds(first);

        // Repeated calls return the cached list in a stable order.
        assertThat(character.getRelevantItemInstances(CharacterSlot.Head)).isSameAs(first);
        assertThat(sortedGemmedIds(character.getRelevantItemInstances(CharacterSlot.Head))).isEqualTo(firstIds);

        // Mutating the backing AvailableItems list does not invalidate (same as C#: the UI calls
        // InvalidateItemInstances); the explicit invalidation rebuilds and picks up the new entry.
        int headId = character.get(CharacterSlot.Head).getId();
        character.getAvailableItems().add(headId + ".0.0.0.999");
        assertThat(character.getRelevantItemInstances(CharacterSlot.Head)).isSameAs(first);
        character.invalidateItemInstances();
        List<ItemInstance> afterInvalidate = character.getRelevantItemInstances(CharacterSlot.Head);
        assertThat(afterInvalidate).isNotSameAs(first);
        assertThat(sortedGemmedIds(afterInvalidate)).contains(headId + ".0.0.0.999");

        // The CustomItemInstances setter invalidates and the custom entry (force-displayed) shows up.
        ItemInstance custom = new ItemInstance(headId, 0, 0, 0, 0);
        custom.setForceDisplay(true);
        character.setCustomItemInstances(new ArrayList<ItemInstance>(List.of(custom)));
        List<ItemInstance> withCustom = character.getRelevantItemInstances(CharacterSlot.Head);
        assertThat(withCustom).isNotSameAs(afterInvalidate);
        assertThat(sortedGemmedIds(withCustom)).contains(headId + ".0.0.0.0");
        assertThat(character.getCustomItemInstances()).containsExactly(custom);

        // Custom items round-trip through save/load with their gems and ForceDisplay (the writer
        // matches the XmlSerializer shape: Id always, zero gems/enchant omitted, ForceDisplay when true).
        Path saved = Files.createTempFile("jrawr-custom-items", ".xml");
        try {
            character.save(saved);
            Character reloaded = CharacterFixtureLoader.load(saved);
            assertThat(reloaded.getCustomItemInstances()).hasSize(1);
            ItemInstance reloadedCustom = reloaded.getCustomItemInstances().get(0);
            assertThat(reloadedCustom.getGemmedId()).isEqualTo(custom.getGemmedId());
            assertThat(reloadedCustom.isForceDisplay()).isTrue();
        } finally {
            Files.deleteIfExists(saved);
        }
    }

    // Mirrors RelevantItemsProbe.cs: the same deterministic extra state on top of the fixture.
    private static Character probeCharacter(String fixture) throws Exception {
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        character.recalculateSetBonuses();
        character.setWaistBlacksmithingSocketEnabled(true);
        int headId = character.get(CharacterSlot.Head).getId();
        ItemInstance custom = new ItemInstance(headId, 0, 0, 0, 0);
        custom.setForceDisplay(true);
        character.getCustomItemInstances().add(custom);
        character.getAvailableItems().add(Integer.toString(headId));
        character.getAvailableItems().add(headId + ".*.*.*");
        character.getAvailableItems().add(headId + ".0.0.0.0");
        character.getAvailableItems().add(headId + ".0.0.0.*");
        return character;
    }

    private static List<String> sortedGemmedIds(List<ItemInstance> instances) {
        return instances.stream().map(ItemInstance::getGemmedId).sorted().toList();
    }

    private static void assertIdsMatch(String fixture, String section, List<String> expected, List<String> actual) {
        assertThat(expected).as("%s %s: expected section present", fixture, section).isNotNull();
        int firstDiff = -1;
        int common = Math.min(expected.size(), actual.size());
        for (int i = 0; i < common; i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                firstDiff = i;
                break;
            }
        }
        if (firstDiff < 0 && expected.size() == actual.size()) {
            return;
        }
        StringBuilder message = new StringBuilder(String.format(
                "%s %s: expected %d ids, actual %d ids", fixture, section, expected.size(), actual.size()));
        if (firstDiff >= 0) {
            message.append(String.format("; first diff at %d: expected %s, actual %s",
                    firstDiff, expected.get(firstDiff), actual.get(firstDiff)));
        } else {
            message.append("; tail differs");
        }
        throw new AssertionError(message.toString());
    }

    private static Map<String, List<String>> readExpected() throws Exception {
        InputStream in = RelevantItemInstancesGoldenTest.class
                .getResourceAsStream("/core-completeness/relevant-items.txt");
        assertThat(in).as("core-completeness/relevant-items.txt missing").isNotNull();
        Map<String, List<String>> expected = new LinkedHashMap<String, List<String>>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String fixture = null;
            String section = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("fixture ")) {
                    fixture = line.substring("fixture ".length());
                } else if (line.startsWith("relevant ") || line.startsWith("instances ")
                        || line.startsWith("gems ") || line.startsWith("items ")) {
                    section = line.startsWith("relevant ")
                            ? "relevant" : line.substring(0, line.lastIndexOf(' '));
                    expected.put(fixture + "|" + section, new ArrayList<String>());
                } else if (line.startsWith("relevantid ")) {
                    expected.get(fixture + "|relevant").add(line.substring("relevantid ".length()));
                } else if (line.startsWith("id ")) {
                    expected.get(fixture + "|" + section).add(line.substring("id ".length()));
                }
            }
        }
        return expected;
    }
}
