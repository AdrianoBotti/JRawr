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
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.ItemInstance;
import rawr.data.CharacterFixtureLoader;
import rawr.models.mage.CalculationsMage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the comparison endpoints: chart rendering, the availability diamond
 * and equipping an exact gemmed instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ComparisonControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @Autowired
    private CalculationService calculationService;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void chartPaneRendersBarsAndMarksTheEquippedRow() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare")
                        .param("slot", "Head")
                        .param("limit", "30")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("charts-card", "charts-nav", "role=\"tab\"", "Items",
                        "Relative Stats", "Stat Graph", "data-compare-rows", "compare-row")
                .contains("Equipped", "data-compare-availability", "Equip", "Custom Item Pool",
                        "Item filters", "hx-get=\"/character/item-filters\"")
                .contains("id=\"compare-body\"", "hx-trigger=\"jrawrRefreshCharts from:body\"",
                        "id=\"compare-sort\"", "hx-preserve=\"true\"", "compare-bar",
                        "compare-bar-seg")
                .doesNotContain("<html", "<h2>Charts</h2>", "Item comparison — Head");
    }

    @Test
    void chartBodyEndpointReturnsOnlyTheRefreshableBody() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("slot", "Head")
                        .param("limit", "30")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("id=\"compare-body\"", "data-compare-rows", "compare-row")
                .doesNotContain("charts-card", "<html");
    }

    @Test
    void availabilityDiamondTogglesTheExactInstanceAndSignalsThatRow() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = editorService.getOrCreate(session);
        ItemInstance instance = character.getRelevantItemInstances(CharacterSlot.Head).stream()
                .filter(candidate -> candidate.getItem() != null
                        && (candidate.getGem1Id() != 0 || candidate.getGem2Id() != 0
                        || candidate.getGem3Id() != 0 || candidate.getEnchantId() != 0))
                .findFirst()
                .orElseThrow();
        String gemmedId = instance.getGemmedId();

        MvcResult toggled = mockMvc.perform(post("/character/compare/availability")
                        .param("slot", "Head")
                        .param("gemmedId", gemmedId)
                        .param("available", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger",
                        org.hamcrest.Matchers.containsString("\"gemmedId\":\"" + gemmedId + "\"")))
                .andReturn();
        assertThat(html(toggled))
                .contains("is-available", "data-gemmed-id=\"" + gemmedId + "\"")
                .contains("Available");
        assertThat(character.getAvailableItems()).contains(gemmedId);
        assertThat(character.getAvailableItems())
                .doesNotContain(Integer.toString(instance.getId()));
        assertThat(editorService.isDirty(session)).isTrue();

        mockMvc.perform(post("/character/compare/availability")
                        .param("slot", "Head")
                        .param("gemmedId", gemmedId)
                        .param("available", "false")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getAvailableItems()).doesNotContain(gemmedId);
    }

    @Test
    void equipsTheExactGemmedInstance() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = editorService.getOrCreate(session);
        ItemInstance candidate = character.getRelevantItemInstances(CharacterSlot.Head).get(0);

        MvcResult equipped = mockMvc.perform(post("/character/slot/instance")
                        .param("slot", "Head")
                        .param("gemmedId", candidate.getGemmedId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(equipped))
                .contains("id=\"paperdoll\"", "hx-swap-oob=\"true\"", "id=\"stats-panel\"");

        assertThat(character.get(CharacterSlot.Head).getGemmedId())
                .isEqualTo(candidate.getGemmedId());
        assertThat(editorService.isDirty(session)).isTrue();
    }

    @Test
    void baselineToggleChangesTheBarBaselineAndKeepsItInTheRefreshUrl() throws Exception {
        MockHttpSession session = sessionWithMage();

        MvcResult overall = mockMvc.perform(get("/character/compare/body")
                        .param("slot", "Head")
                        .param("limit", "30")
                        .param("baseline", "overall")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(overall))
                .contains("baseline=overall", ">Absolute<", "vs equipped ", "compare-subpoint")
                .doesNotContain(">absolute ");

        MvcResult equipped = mockMvc.perform(get("/character/compare/body")
                        .param("slot", "Head")
                        .param("limit", "30")
                        .param("baseline", "equipped")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(equipped))
                .contains("baseline=equipped", ">Current Build<", ">absolute ", "compare-subpoint")
                .doesNotContain("vs equipped ");
    }

    @Test
    void switchingTheModelInvalidatesTheRelevantItemCaches() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = editorService.getOrCreate(session);
        calculationService.ensureModelLoaded(character);

        assertThat(character.getRelevantItemInstances(CharacterSlot.Head))
                .anyMatch(instance -> instance.getItem() != null
                        && "Sanctified Bloodmage Hood".equals(instance.getItem().getName()));

        editorService.applyMeta(character, null, null, "DPSDK");
        calculationService.ensureModelLoaded(character);

        assertThat(character.getRelevantItemInstances(CharacterSlot.Head))
                .isNotEmpty()
                .noneMatch(instance -> instance.getItem() != null
                        && "Sanctified Bloodmage Hood".equals(instance.getItem().getName()));
    }

    @Test
    void compareNeedsAChosenModel() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mockMvc.perform(get("/character/compare")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result)).contains("Choose a model before comparing items.");
    }

    @Test
    void chartPaneOffersTheRelativeStatsType() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("role=\"tab\"", "Items", "Relative Stats", "Stat Graph",
                        "name=\"chartType\"")
                .contains("chartType=items")
                .contains("Custom Item Pool");

        MvcResult stats = mockMvc.perform(get("/character/compare")
                        .param("chartType", "stats")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(stats))
                .contains("Filter by stat name…", "data-compare-view", "data-compare-histogram",
                        "Custom Item Pool");
    }

    @Test
    void relativeStatsBodyRendersStatWeightsInsteadOfItems() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "stats")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("compare-stat-row", "data-compare-rows", "data-compare-list",
                        "data-compare-view", ">Rows<", ">Histogram<", "data-compare-histogram",
                        "Crit Rating", "Spell Power", "rating per point of each stat")
                .contains("chartType=stats", "Custom Item Pool")
                .doesNotContain("data-compare-availability", "Click the diamond");
    }

    @Test
    void relativeStatsWithoutAModelExplainsWhatIsMissing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "stats")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result)).contains("Choose a model before comparing stats.");
    }


    @Test
    void statGraphBodyRendersTheLegacyMultilineData() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "statgraph")
                        .param("graphMode", "stats")
                        .param("graphSteps", "5")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("data-stat-graph", "data-stat-series", "name=\"graphMode\"",
                        ">Stat change<", ">Scaling<", "data-visible")
                .doesNotContain("compare-lines-menu")
                .contains("Stat Graph");
        assertThat(html).doesNotContain("data-compare-availability");
    }

    @Test
    void scalingGraphComparesSelectedStatsAgainstTheChosenBaseStat() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "statgraph")
                        .param("graphMode", "scaling")
                        .param("graphBase", "SpellPower")
                        .param("graphStats", "HitRating", "HasteRating", "CritRating")
                        .param("graphSteps", "5")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains(">Base stat<", ">Spell Power<", "Change in Spell Power",
                        "data-stat-graph", "data-stat-series")
                .contains("Hit Rating", "Haste Rating", "Crit Rating");
    }


    @Test
    void statGraphCanPlotOneSubPoint() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "statgraph")
                        .param("graphMode", "stats")
                        .param("graphValue", "0")
                        .param("graphSteps", "5")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result)).contains("data-y-label=\"Dps", ">Dps<");
    }


    @Test
    void directUpgradesBodyRendersPositiveUpgradesAcrossSlots() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "upgrades")
                        .param("limit", "5")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Upgrades", "Top per slot", "data-compare-rows", "compare-row",
                        "data-compare-availability", "Equip", "upgrades")
                .doesNotContain("<h2>Charts</h2>");
    }


    @Test
    void enchantBodyRendersTheSlotCandidatesAndApplyAction() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "enchants")
                        .param("enchantSlot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Enchants", "name=\"enchantSlot\"", "data-compare-rows",
                        "No enchant", "Current", "Apply", "Filter by enchant name…",
                        "Custom Item Pool")
                .doesNotContain("<h2>Charts</h2>");
    }


    @Test
    void talentAndGlyphBodiesRenderTheirCharts() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult talents = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "talents")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(talents))
                .contains("Talents", "talents", "data-compare-rows", "Rank ", "Maxed",
                        "Filter by talent name…", "Custom Item Pool")
                .doesNotContain("<h2>Charts</h2>");

        MvcResult glyphs = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "glyphs")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(glyphs))
                .contains("Glyphs", "glyphs", "data-compare-rows", "Major glyph",
                        "Filter by glyph name…", "Custom Item Pool")
                .doesNotContain("<h2>Charts</h2>");
    }


    @Test
    void buffBodyRendersTheGroupFilterAndBuffRows() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "buffs")
                        .param("buffFilter", "Current")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Buffs", "name=\"buffFilter\"", "data-compare-rows", "Current",
                        "Filter by buff name…", "group: Current", "Custom Item Pool")
                .doesNotContain("<h2>Charts</h2>");
    }


    @Test
    void gemBodyRendersTheColorFilterAndGemRows() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/compare/body")
                        .param("chartType", "gems")
                        .param("gemColor", "Meta")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Gems", "name=\"gemColor\"", "data-compare-rows", "color: Meta",
                        "Filter by gem name…", "Custom Item Pool")
                .doesNotContain("<h2>Charts</h2>");
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

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
