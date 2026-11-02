package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.ItemInstance;
import rawr.data.CharacterFixtureLoader;
import rawr.models.mage.CalculationsMage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the available-gear manager: the per-slot list, item-level and exact
 * entries, bulk actions, text import/export and the optimizer pool integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AvailabilityControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void modalRendersRelevantItemsAndEmptyExactSection() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/availability")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Custom Item Pool", "availability-backdrop", "availability-modal",
                        "id=\"availability-body\"", "availability-actions-row",
                        "dialog-section-title", "Bulk actions", "btn-danger")
                .contains("Sanctified Bloodmage Hood", "Use equipped gear", "All relevant items")
                .contains("Exact gemming/enchant entries", "No exact entries for this slot")
                .contains("Add exact entry", "Download .txt")
                .doesNotContain("Add by ID", "availability-add")
                .contains("id=\"availability-search\"", "hx-preserve=\"true\"")
                .contains("hx-confirm=", "Replace the entire custom item pool")
                .doesNotContain("<html");
    }

    @Test
    void itemLevelToggleAddsAndRemovesTheItemEntry() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        character.getAvailableItems().add("51281.0.0.0.3820");

        mockMvc.perform(post("/character/availability/item")
                        .param("slot", "Head")
                        .param("itemId", "51281")
                        .param("available", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger",
                        org.hamcrest.Matchers.containsString("jrawrDirty")));
        assertThat(character.getAvailableItems()).containsExactly("51281");
        assertThat(editorService.isDirty(session)).isTrue();

        mockMvc.perform(post("/character/availability/item")
                        .param("slot", "Head")
                        .param("itemId", "51281")
                        .param("available", "false")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).isEmpty();
    }

    @Test
    void exactEntryCanBeAddedFromTheEquippedInstanceAndRemoved() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        String entry = AvailabilityService.availabilityEntry(character.get(CharacterSlot.Head));

        MvcResult added = mockMvc.perform(post("/character/availability/exact")
                        .param("slot", "Head")
                        .param("action", "ADD_EQUIPPED")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(character.getAvailableItems()).containsExactly(entry);
        assertThat(html(added)).contains("Added the equipped instance", entry);

        mockMvc.perform(post("/character/availability/exact")
                        .param("slot", "Head")
                        .param("action", "REMOVE")
                        .param("entry", entry)
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).isEmpty();
    }

    @Test
    void equippedBulkReplacesTheListWithEquippedEntries() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);

        mockMvc.perform(post("/character/availability/bulk")
                        .param("slot", "Head")
                        .param("action", "EQUIPPED")
                        .session(session))
                .andExpect(status().isOk());
        Set<String> expected = new LinkedHashSet<String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                expected.add(AvailabilityService.availabilityEntry(instance));
            }
        }
        assertThat(character.getAvailableItems()).containsExactlyElementsOf(expected);
    }

    @Test
    void allRelevantBulkExcludesOtherClassGearAndClearAllEmptiesTheList() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);

        mockMvc.perform(post("/character/availability/bulk")
                        .param("slot", "Head")
                        .param("action", "ALL_RELEVANT")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).contains("51158").doesNotContain("51208");

        mockMvc.perform(post("/character/availability/bulk")
                        .param("slot", "Head")
                        .param("action", "CLEAR_ALL")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).isEmpty();
    }

    @Test
    void importPreservesOrderAndExportReturnsTheSameLines() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);

        mockMvc.perform(post("/character/availability/import")
                        .param("slot", "Head")
                        .param("mode", "replace")
                        .param("entries", "51281\n51158.0.0.0.0\n")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems())
                .containsExactly("51281", "51158.0.0.0.0");
        assertThat(OptimizerJobService.buildPool(character, Calculations.getModel("Mage"),
                availabilityParams())).contains("51158.0.0.0.0");

        MvcResult export = mockMvc.perform(get("/character/availability/export").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("availability.txt")))
                .andReturn();
        assertThat(html(export)).isEqualTo("51281\n51158.0.0.0.0\n");

        mockMvc.perform(post("/character/availability/import")
                        .param("slot", "Head")
                        .param("mode", "replace")
                        .param("entries", "not-an-entry")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems())
                .containsExactly("51281", "51158.0.0.0.0");
    }

    @Test
    void exactEntryCanBeAddedManuallyAndInvalidTextIsRejected() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);

        MvcResult added = mockMvc.perform(post("/character/availability/exact")
                        .param("slot", "Head")
                        .param("action", "ADD")
                        .param("entry", "51158.0.0.0.0")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(character.getAvailableItems()).containsExactly("51158.0.0.0.0");
        assertThat(html(added)).contains("Added the exact entry");

        mockMvc.perform(post("/character/availability/exact")
                        .param("slot", "Head")
                        .param("action", "ADD")
                        .param("entry", "not-an-entry")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).containsExactly("51158.0.0.0.0");
    }

    @Test
    void otherLegacyAndMissingEntriesAreShownAndRemovable() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        character.getAvailableItems().add("-13820");
        character.getAvailableItems().add("99999999");

        MvcResult body = mockMvc.perform(get("/character/availability")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(body))
                .contains("Other pool entries", "-13820", "99999999",
                        "is not in the item cache");

        mockMvc.perform(post("/character/availability/other")
                        .param("slot", "Head")
                        .param("action", "REMOVE")
                        .param("entry", "-13820")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).contains("99999999").doesNotContain("-13820");
    }

    @Test
    void searchIncludesFilteredOutCachedItemsButNotOtherClassGear() throws Exception {
        MockHttpSession session = sessionWithMage();

        MvcResult filtered = mockMvc.perform(get("/character/availability")
                        .param("slot", "Head")
                        .param("q", "Tarnished Plate Helmet")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(filtered)).contains("Tarnished Plate Helmet", "Filtered out");

        MvcResult wrongClass = mockMvc.perform(get("/character/availability")
                        .param("slot", "Head")
                        .param("q", "Sanctified Dark Coven Hood")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(wrongClass))
                .contains("0 shown")
                .doesNotContain("Sanctified Dark Coven Hood</span>");
    }

    // ------------------------------------------------------------------ helpers

    private static OptimizerJobService.OptimizerParams availabilityParams() {
        return new OptimizerJobService.OptimizerParams(1, List.of(CharacterSlot.Head), 0, 1,
                null, null, false, false, true, false, false, false, false,
                OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                false, false, false, false);
    }

    private MockHttpSession sessionWithMage() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        Calculations.loadModel(CalculationsMage.class);
        MockHttpSession session = new MockHttpSession();
        editorService.openCharacter(session, character, null);
        return session;
    }

    private Character character(MockHttpSession session) {
        return editorService.getOrCreate(session);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
