package rawr.golden;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rawr.base.optimizer.AvailableItemGenerator;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterRegex;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemType;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring of the character gemming-template and item-filter overrides: the loader must
 * carry them into the engine, the optimizer's {@link AvailableItemGenerator} must consume the
 * gemming templates, and applying a saved filter override must change which items the item cache
 * reports as relevant (the pool the optimizer searches). Uses a test model with known templates and
 * the shipped ItemCache; no network.
 */
class CharacterOverrideEffectTest {
    private static final String MODEL = "OverrideEffectTest";
    private static final int HEAD = 51281; // Sanctified Bloodmage Hood, Meta + Blue sockets, Cloth
    private static final int RUNED = 40113; // default-enabled template gem
    private static final int BOLD = 40111; // only reachable through the override

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
        Calculations.registerModel(MODEL, OverrideEffectModel.class);
        Calculations.loadModel(OverrideEffectModel.class);
    }

    @AfterAll
    static void tearDown() {
        ItemFilter.getFilterList().clear();
        ItemFilter.setOtherEnabled(true);
        ItemCache.onItemsChanged();
    }

    @Test
    void gemmingTemplateOverrideChangesGeneratedAvailableItems() {
        Character plain = character();
        Character overridden = character();
        overridden.setGemmingTemplateOverrides(List.of(template("Disabled Group", true, BOLD)));

        AvailableItemGenerator plainGenerator = generator(plain);
        AvailableItemGenerator overriddenGenerator = generator(overridden);

        // The template lists are identical except for the override-enabled group.
        List<GemmingTemplate> plainTemplates = plain.getCurrentGemmingTemplates();
        List<GemmingTemplate> overriddenTemplates = overridden.getCurrentGemmingTemplates();
        assertThat(plainTemplates).hasSameSizeAs(overriddenTemplates);
        assertThat(plainTemplates.get(0).getEnabled()).isTrue();
        assertThat(plainTemplates.get(1).getEnabled()).isFalse();
        assertThat(overriddenTemplates.get(0).getEnabled()).isTrue();
        assertThat(overriddenTemplates.get(1).getEnabled()).isTrue();

        // AvailableItemGenerator amplifies the template gem ids; the override adds the Bold gem.
        assertThat(gemIds(plainGenerator.getGemItems())).containsExactly(RUNED);
        assertThat(gemIds(plainGenerator.getMetaGemItems())).containsExactly(41285);
        assertThat(gemIds(overriddenGenerator.getGemItems())).containsExactly(RUNED, BOLD);
        assertThat(gemIds(overriddenGenerator.getMetaGemItems())).containsExactly(41285);

        // Bit-exact gemmed variants for the head item (enchant pinned to 0 so only gems vary).
        Item head = ItemCache.findItemById(HEAD);
        assertThat(plainGenerator.getPossibleGemmedItemsForItem(head, "51281.*.*.0.0")
                        .stream().map(ItemInstance::getGemmedId).toList())
                .containsExactly("51281.41285.40113.0.0");
        assertThat(overriddenGenerator.getPossibleGemmedItemsForItem(head, "51281.*.*.0.0")
                        .stream().map(ItemInstance::getGemmedId).toList())
                .containsExactly("51281.41285.40113.0.0", "51281.41285.40111.0.0");
    }

    @Test
    void savedItemFilterOverrideDoesNotChangeRelevantItemsOnLoad() throws Exception {
        ItemFilterRegex filter = new ItemFilterRegex();
        filter.setName("OverrideEffectTestFilter");
        filter.setEnabled(false);
        ItemFilter.getFilterList().add(filter);
        ItemFilter.setOtherEnabled(true);
        Path saved = Files.createTempFile("jrawr-filter-override", ".xml");
        try {
            Character character = character();
            character.save(saved); // captures filter disabled

            filter.setEnabled(true);
            Item[] before = ItemCache.getRelevantItems(Calculations.getModel(MODEL), CharacterRace.Troll);
            assertThat(ids(before)).contains(HEAD);

            // JRawr keeps filters global: the character's saved snapshot is not applied on load.
            CharacterFixtureLoader.load(saved);
            assertThat(filter.isEnabled()).isTrue();
            Item[] after = ItemCache.getRelevantItems(Calculations.getModel(MODEL), CharacterRace.Troll);
            assertThat(ids(after)).contains(HEAD);
        } finally {
            Files.deleteIfExists(saved);
            ItemFilter.getFilterList().clear();
            ItemFilter.setOtherEnabled(true);
            ItemCache.onItemsChanged();
        }
    }

    private static AvailableItemGenerator generator(Character character) {
        return new AvailableItemGenerator(new ArrayList<>(List.of(Integer.toString(HEAD))),
                false, true, false, false, false, character, Calculations.getModel(MODEL));
    }

    private static Character character() {
        Character character = new Character();
        character.setName("OverrideEffect");
        character.setRealm("Test");
        character.setCharacterClass(CharacterClass.Mage);
        character.setRace(CharacterRace.Troll);
        character.setCurrentModel(MODEL);
        return character;
    }

    private static List<Integer> gemIds(Item[] items) {
        List<Integer> ids = new ArrayList<>();
        for (Item item : items) {
            ids.add(item == null ? 0 : item.getId());
        }
        return ids;
    }

    private static List<Integer> ids(Item[] items) {
        List<Integer> ids = new ArrayList<>();
        for (Item item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    private static GemmingTemplate template(String group, boolean enabled, int gem) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel(MODEL);
        template.setGroup(group);
        template.setRedId(gem);
        template.setYellowId(gem);
        template.setBlueId(gem);
        template.setPrismaticId(gem);
        template.setMetaId(41285);
        template.setEnabled(enabled);
        return template;
    }

    public static class OverrideEffectModel extends TestMageModel {
        @Override
        public List<GemmingTemplate> getDefaultGemmingTemplates() {
            List<GemmingTemplate> templates = new ArrayList<>();
            templates.add(template("Enabled Group", true, RUNED));
            templates.add(template("Disabled Group", false, BOLD));
            return templates;
        }

        @Override
        public List<ItemType> getRelevantItemTypes() {
            return new ArrayList<>(List.of(ItemType.Cloth));
        }
    }
}
