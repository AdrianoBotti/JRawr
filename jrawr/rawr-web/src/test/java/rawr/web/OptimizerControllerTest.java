package rawr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.character.Character;
import rawr.base.character.Profession;
import rawr.base.character.CharacterSlot;
import rawr.data.CharacterFixtureLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the optimizer endpoints: panel/start/cancel wiring, status polling and
 * the SSE stream. Jobs on the fast paths are started through the service so the tests stay tiny;
 * the start endpoint is exercised for its response fragment and its one-job-per-session rejection.
 * Nothing asserts wall-clock timing - the tests poll with a bound.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OptimizerControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private OptimizerJobService optimizerJobs;

    @Autowired
    private CharacterStore characterStore;

    @Autowired
    private EditorService editorService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ItemIconService itemIcons;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void pageCarriesOptimizeButtonAndPanelRenders() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult page = mockMvc.perform(get("/").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(html(page)).contains("data-mode=\"optimize\"", "id=\"optimize-mode\"");

        // The custom item pool is a charts-pane action now (the pane is lazy-loaded from /).
        MvcResult charts = mockMvc.perform(get("/character/compare")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(charts)).contains("Custom Item Pool",
                "hx-get=\"/character/availability?slot=Head\"");

        MvcResult panel = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(panel))
                .contains("id=\"optimize-live\"", "data-job-state=\"idle\"", "name=\"thoroughness\"")
                .contains("id=\"thoroughness-level\"", "id=\"thoroughness-value\"",
                        "data-thoroughness-values=\"1,50,150,275,400\"",
                        ">Minimal<", ">Quick<", ">Balanced<", ">Thorough<", ">Maximum<")
                .contains("name=\"slot\"", "name=\"calculation\"", "name=\"poolMode\"")
                .contains("value=\"ALL_RELEVANT\" selected")
                .contains("name=\"method\"", "name=\"greedy\"", "name=\"overrideReenchant\" checked")
                .contains("name=\"templateGemsEnabled\"", "name=\"talentMode\"",
                        "value=\"none\" checked", "value=\"spec\"", "value=\"mutate\"",
                        "name=\"talentsOnly\"", "id=\"talents-only-button\"", "data-tip-host",
                        "Try every legal gemming of the selected items")
                .doesNotContain("<select name=\"thoroughness\"")
                .doesNotContain("Manage availability", "<html");
    }

    @Test
    void optimizerResultGroupsConsumablesAndShowsTheirStats() {
        OptimizerController.ConsumableGroupView food = new OptimizerController.ConsumableGroupView(
                "Food",
                List.of(new OptimizerController.ConsumableItemView("Fish Feast",
                        "+80 Attack Power, +46 Spell Power, +40 Stamina")),
                List.of());
        OptimizerController.ConsumableGroupView elixirs = new OptimizerController.ConsumableGroupView(
                "Elixirs & Flasks",
                List.of(new OptimizerController.ConsumableItemView("Flask of the Frost Wyrm",
                        "+125 Spell Power")),
                List.of(new OptimizerController.ConsumableItemView("Flask of the North",
                        "+60 Spell Power")));
        OptimizerController.OptimizerResultView result = new OptimizerController.OptimizerResultView(
                "Overall rating", "100.00", "110.00", "+10.00", 0, List.of(), List.of(),
                List.of(food, elixirs), false, "0/0/0", "0/0/0", "", "",
                List.of(), List.of());

        Context context = new Context();
        context.setVariable("optimizerResult", result);
        context.setVariable("icons", itemIcons);
        String html = templateEngine.process(
                "fragments/optimize", Set.of("optimizeResultView"), context);

        assertThat(html)
                .contains("Consumables", ">Food<", "Elixirs &amp; Flasks",
                        "Fish Feast", "+80 Attack Power, +46 Spell Power, +40 Stamina",
                        "Flask of the Frost Wyrm", "+125 Spell Power",
                        "Flask of the North", "+60 Spell Power", "Added", "Removed");
    }

    @Test
    void optimizerResultTalentAndGlyphRowsCarryIconsAndEffectTooltips() {
        TalentPanelService.TalentChange talent = new TalentPanelService.TalentChange(
                "Arcane Concentration", 2, 4, 5, "spell_arcane_arcane01",
                "Gives you a 4% chance to gain Clearcasting.", 4);
        TalentPanelService.GlyphChange glyph = new TalentPanelService.GlyphChange(
                "Glyph of Fireball", true, false, true,
                "Reduces the casting time of your Fireball spell by 0.15 sec.");
        OptimizerController.OptimizerResultView result = new OptimizerController.OptimizerResultView(
                "Overall rating", "100.00", "110.00", "+10.00", 0, List.of(), List.of(),
                List.of(), true, "51/20/0", "50/21/0", "spec before", "spec after",
                List.of(talent), List.of(glyph));

        Context context = new Context();
        context.setVariable("optimizerResult", result);
        context.setVariable("icons", itemIcons);
        String html = templateEngine.process(
                "fragments/optimize", Set.of("optimizeResultView"), context);

        assertThat(html)
                .contains("data-tip-host=\"true\"")
                .contains("optimize-talent-change-icon")
                .contains("spell_arcane_arcane01")
                .contains("Gives you a 4% chance to gain Clearcasting.")
                .contains("Reduces the casting time of your Fireball spell by 0.15 sec.")
                .contains("Major glyph")
                .doesNotContain("<html");
    }

    @Test
    void optimizerFormIsSplitIntoTabsWithOneStartAction() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult panel = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(panel);

        assertThat(html)
                .contains("role=\"tablist\"", "aria-label=\"Optimizer options\"")
                .contains("id=\"optimize-panel-gear\"", "id=\"optimize-panel-talents\"",
                        "id=\"optimize-panel-constraints\"",
                        "id=\"optimize-panel-advanced\"")
                .contains("data-optimize-setup-summary", "Current setup", ">Constraints<")
                .doesNotContain("<html");
        assertThat(count(html, "data-tab=\"")).isEqualTo(4);
        assertThat(count(html, "data-tab-panel=\"")).isEqualTo(4);
        assertThat(count(html, ">Start<")).isEqualTo(1);

        assertThat(tabPanel(html, "gear"))
                .contains("name=\"calculation\"", "name=\"slot\"", "name=\"poolMode\"")
                .contains("class=\"hint hint-list\"", "<strong>All relevant items</strong>",
                        "<strong>Custom item pool</strong>")
                .contains("name=\"overrideRegem\"", "name=\"overrideReenchant\"",
                        "name=\"templateGemsEnabled\"")
                .contains("Gear-only upgrade list", "Find per-slot gear upgrades",
                        "name=\"upgradeItemsPerSlot\"", "name=\"upgradeThoroughness\"",
                        "name=\"upgradeItem\"", "hx-post=\"/character/upgrades\"")
                .contains("Manage custom item pool", "hx-get=\"/character/availability\"")
                .doesNotContain("name=\"method\"", "name=\"talentMode\"");
        assertThat(tabPanel(html, "talents"))
                .contains("name=\"talentMode\"", "value=\"none\" checked", "value=\"spec\"",
                        "value=\"mutate\"", "name=\"talentsOnly\"", "id=\"talents-only-button\"",
                        "name=\"optimizeFood\"", "name=\"optimizeElixirs\"",
                        "data-mixology-info")
                .contains(">No talent search<", ">Best existing spec<",
                        ">Best spec plus point tweaks<", ">Talent &amp; glyphs only search<",
                        "Run talents &amp; glyphs only", ">Talent &amp; glyph search<", ">Consumable search<",
                        ">Current spec<", "class=\"hint\"")
                .doesNotContain("name=\"method\"", "name=\"greedy\"");
        assertThat(tabPanel(html, "constraints"))
                .contains("id=\"optimizer-requirements\"", "name=\"requirementCalculation\"",
                        "name=\"requirementDirection\"", "name=\"requirementValue\"",
                        "data-add-requirement", "requirement-row-template", "Add constraint")
                .doesNotContain("name=\"calculation\"", "name=\"slot\"");
        assertThat(tabPanel(html, "advanced"))
                .contains("name=\"method\"", "name=\"greedy\"", "name=\"thoroughness\"",
                        "<strong>Genetic algorithm</strong>",
                        "<strong>Simulated annealing</strong>",
                        "<strong>All combinations</strong>",
                        "<strong>Single gem/enchant changes</strong>",
                        "<strong>Greedy best gem/enchant</strong>")
                .contains("Performance", "useMultithreading", "maxThreads",
                        "/settings/optimizer")
                .doesNotContain("name=\"slot\"", "name=\"requirementCalculation\"",
                        "name=\"overrideRegem\"");
    }

    @Test
    void optimizerFormKeepsTheSubmittedSettings() throws Exception {
        MockHttpSession session = sessionWithFixture();
        mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .param("calculation", "[Overall]")
                        .param("overrideRegem", "true")
                        .param("overrideReenchant", "false")
                        .param("templateGemsEnabled", "false")
                        .param("optimizeFood", "true")
                        .param("optimizeElixirs", "false")
                        .param("talentMode", "spec")
                        .param("poolMode", "ALL_RELEVANT")
                        .param("method", "SimulatedAnnealing")
                        .param("greedy", "GreedyBest")
                        .param("upgradeItemsPerSlot", "10")
                        .param("upgradeThoroughness", "50")
                        .session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/character/optimize/cancel").session(session))
                .andExpect(status().isOk());

        MvcResult live = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(live))
                .contains("name=\"overrideRegem\" checked")
                .contains("name=\"optimizeFood\" checked")
                .contains("value=\"Head\" checked")
                .doesNotContain("value=\"Neck\" checked")
                .contains("value=\"spec\" checked")
                .doesNotContain("value=\"none\" checked")
                .containsPattern("value=\"SimulatedAnnealing\"\\s+selected=\"selected\"")
                .containsPattern("value=\"GreedyBest\"\\s+selected=\"selected\"")
                .contains("value=\"10\" selected")
                .contains("value=\"50\" selected")
                .doesNotContain("name=\"overrideReenchant\" checked")
                .doesNotContain("name=\"templateGemsEnabled\" checked")
                .doesNotContain("name=\"optimizeElixirs\" checked");
    }

    @Test
    void startThenPollStatusThenFetchResult() throws Exception {
        MockHttpSession session = sessionWithFixture();

        MvcResult started = mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(started)).contains("id=\"optimize-live\"");

        JsonNode status = pollStatusUntilDone(session, 240_000);
        assertThat(status.get("state").asText()).isEqualTo("done");
        assertThat(status.get("progress").asInt()).isEqualTo(100);
        assertThat(status.get("active").asBoolean()).isFalse();
        assertThat(status.get("afterPoints").asDouble())
                .isGreaterThanOrEqualTo(status.get("beforePoints").asDouble() - 0.01);
        assertThat(optimizerJobs.activeJobCount()).isZero();

        MvcResult applied = mockMvc.perform(post("/character/optimize/apply").session(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("HX-Redirect", "/"))
                .andReturn();
        assertThat(html(applied)).doesNotContain("<html");

        MvcResult result = mockMvc.perform(get("/character/optimize/result").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result))
                .contains("id=\"paperdoll\"")
                .contains("id=\"stats-panel\"")
                .doesNotContain("<html");

        MvcResult donePanel = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(donePanel))
                .contains("data-job-state=\"done\"", "data-optimize-summary", "optimize-result")
                .contains("Optimized:", "Load optimized", "Keep existing")
                .contains("Run settings", ">Slots<", "1 of 17 slots", "All relevant items",
                        "No talent search", ">Consumables<", ">None<", "Minimal (1)",
                        "Genetic algorithm")
                .contains("Save as separate character", "name=\"name\"");

        String saveName = "optimizer-result-test-" + java.util.UUID.randomUUID();
        MvcResult saved = mockMvc.perform(post("/character/optimize/save-as")
                        .param("name", saveName)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String savedFile = CharacterStore.sanitizeSaveAsName(saveName);
        assertThat(savedFile).isNotNull();
        assertThat(html(saved)).contains("Saved optimized setup as " + savedFile);
        try {
            assertThat(characterStore.exists(savedFile)).isTrue();
            Character savedCharacter = characterStore.load(savedFile);
            assertThat(savedCharacter.getCurrentModel()).isEqualTo("Mage");
        } catch (Exception e) {
            throw new AssertionError("saving the optimized setup failed", e);
        } finally {
            characterStore.delete(savedFile);
        }
    }

    @Test
    void talentCandidatesContainTheCurrentAndSaved71PointSpecs() throws Exception {
        MockHttpSession session = sessionWithFixture();
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        List<rawr.base.talents.TalentsBase> specs = OptimizerJobService.buildTalentSpecs(character);
        assertThat(specs).isNotEmpty();
        assertThat(specs.get(0).toString()).isEqualTo(character.getCurrentTalents().toString());
        for (rawr.base.talents.TalentsBase spec : specs) {
            int points = 0;
            for (int value : spec.getData()) {
                points += value;
            }
            assertThat(points).as("candidate spec point total").isEqualTo(character.getLevel() - 9);
        }
    }

    @Test
    void unchangedOptimizerSettingsDoNotMarkTheCharacterDirty() throws Exception {
        MockHttpSession session = sessionWithFixture();
        assertThat(editorService.isDirty(session)).isFalse();

        mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(editorService.isDirty(session))
                .as("starting a run with the default target/requirements changes nothing")
                .isFalse();
        OptimizerJobService.Job first = optimizerJobs.currentJob(session).orElse(null);
        if (first != null) {
            awaitTerminal(first, 120_000);
        }

        mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .param("calculation", "[SubPoint 0]")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger",
                        org.hamcrest.Matchers.containsString("jrawrDirty")));
        assertThat(editorService.isDirty(session)).isTrue();
        assertThat(((Character) session.getAttribute(EditorService.SESSION_CHARACTER))
                .getCalculationToOptimize()).isEqualTo("[SubPoint 0]");
        optimizerJobs.cancel(session);
        OptimizerJobService.Job second = optimizerJobs.currentJob(session).orElse(null);
        if (second != null) {
            awaitTerminal(second, 120_000);
        }
    }

    @Test
    void secondJobIsRejectedAndCancelReturnsToIdle() throws Exception {
        MockHttpSession session = sessionWithFixture();
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        OptimizerJobService.Job job = optimizerJobs.start(session, character,
                new OptimizerJobService.OptimizerParams(1,
                        List.of(CharacterSlot.Head, CharacterSlot.Chest), 2, 1));

        MvcResult rejected = mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(rejected)).contains("already in progress");

        MvcResult cancelled = mockMvc.perform(post("/character/optimize/cancel").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(cancelled)).contains("id=\"optimize-live\"");

        awaitTerminal(job, 120_000);
        assertThat(job.getState()).isIn("cancelled", "done");
        assertThat(optimizerJobs.activeJobCount()).isZero();
    }

    @Test
    void mixologyIsAutomaticOnlyWithAlchemy() throws Exception {
        MockHttpSession session = sessionWithFixture();
        MvcResult withoutAlchemy = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(withoutAlchemy))
                .contains("Not included: this character does not have Alchemy.")
                .doesNotContain("Included automatically for Alchemy.");

        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        character.setPrimaryProfession(Profession.Alchemy);

        MvcResult withAlchemy = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(withAlchemy))
                .contains("Included automatically for Alchemy.")
                .doesNotContain("Not included: this character does not have Alchemy.");
    }

    @Test
    void talentsOnlyWithoutATalentSearchIsRejectedWithAHelpfulError() throws Exception {
        MockHttpSession session = sessionWithFixture();

        MvcResult panel = mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .param("talentsOnly", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(html(panel))
                .contains("Choose a talent mode for a talent-only run.")
                .contains("id=\"optimize-live\"");
        assertThat(optimizerJobs.currentJob(session)).isEmpty();
    }

    @Test
    void panelRendersStoredRequirementsAndStartPersistsSubmittedOnes() throws Exception {
        MockHttpSession session = sessionWithFixture();
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        OptimizationRequirement stored = new OptimizationRequirement();
        stored.calculation = "[Overall]";
        stored.lessThan = true;
        stored.value = 12345.5f;
        character.setOptimizationRequirements(List.of(stored));

        MvcResult panel = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(panel))
                .contains("requirement-row", "name=\"requirementCalculation\"")
                .contains("name=\"requirementDirection\"", "name=\"requirementValue\"")
                .contains("value=\"12345.5\"");

        MvcResult started = mockMvc.perform(post("/character/optimize")
                        .param("thoroughness", "1")
                        .param("slot", "Head")
                        .param("requirementCalculation", "[Overall]")
                        .param("requirementDirection", "lte")
                        .param("requirementValue", "1000")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(started)).contains("id=\"optimize-live\"");
        assertThat(character.getOptimizationRequirements()).hasSize(1);
        assertThat(character.getOptimizationRequirements().get(0).lessThan).isTrue();
        assertThat(character.getOptimizationRequirements().get(0).value).isEqualTo(1000f);

        optimizerJobs.cancel(session);
        awaitTerminal(optimizerJobs.currentJob(session).orElseThrow(), 120_000);
    }

    @Test
    void statusEndpointIsIdleWithoutAJob() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/character/optimize/status").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"state\":\"idle\"")));
    }

    @Test
    void streamEmitsDoneEventForAFinishedJob() throws Exception {
        MockHttpSession session = sessionWithFixture();
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        OptimizerJobService.Job job = optimizerJobs.start(session, character,
                new OptimizerJobService.OptimizerParams(1,
                        List.of(CharacterSlot.Head, CharacterSlot.Chest), 2, 1));

        MvcResult stream = mockMvc.perform(get("/character/optimize/stream").session(session))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = pollContent(stream, "event:done", 120_000);
        assertThat(body).contains("event:done").contains("\"state\":\"done\"");
        awaitTerminal(job, 120_000);
        assertThat(optimizerJobs.activeJobCount()).isZero();
        try {
            stream.getAsyncResult(1000);
        } catch (Exception ignored) {
            // the emitter completes the mock async request; nothing to clean up if it already did
        }
    }

    // ------------------------------------------------------------------ helpers


    @Test
    void optimizerGearTabOffersTheGearOnlyUpgradeList() throws Exception {
        MockHttpSession session = sessionWithFixture();
        MvcResult panel = mockMvc.perform(get("/character/optimize/live").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(panel))
                .contains("Gear-only upgrade list", "Find per-slot gear upgrades",
                        "Evaluate one item", "name=\"upgradeItem\"",
                        "name=\"upgradeItemsPerSlot\"", "name=\"upgradeThoroughness\"")
                .doesNotContain("id=\"optimize-tab-upgrades\"", "Build upgrade list");
    }

    @Test
    void optimizerOutputRestoresTheResultPanelAfterTheUpgradeView() throws Exception {
        MockHttpSession session = sessionWithFixture();
        MvcResult output = mockMvc.perform(get("/character/optimize/output").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(output))
                .contains("id=\"optimize-output-body\"", "No run yet")
                .doesNotContain("optimize-form", "id=\"optimize-live\"");
    }

    @Test
    void upgradeEndpointsReturnTheirResultViews() throws Exception {
        MockHttpSession session = sessionWithFixture();
        MvcResult started = mockMvc.perform(post("/character/upgrades")
                        .param("slot", "Head")
                        .param("calculation", "[Overall]")
                        .param("poolMode", "ALL_RELEVANT")
                        .param("method", "GeneticAlgorithm")
                        .param("greedy", "AllCombinations")
                        .param("templateGemsEnabled", "true")
                        .param("upgradeItemsPerSlot", "1")
                        .param("upgradeThoroughness", "1")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(started)).contains("Upgrade list");

        String html = "";
        long deadline = System.currentTimeMillis() + 60_000L;
        while (System.currentTimeMillis() < deadline) {
            MvcResult status = mockMvc.perform(get("/character/upgrades/status").session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            html = html(status);
            if (html.contains("Back") && !html.contains("Cancel")) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(html).contains("Upgrade list", "Back");

        MvcResult evaluation = mockMvc.perform(post("/character/upgrades/evaluate")
                        .param("slot", "Head")
                        .param("calculation", "[Overall]")
                        .param("poolMode", "ALL_RELEVANT")
                        .param("method", "GeneticAlgorithm")
                        .param("greedy", "AllCombinations")
                        .param("templateGemsEnabled", "true")
                        .param("upgradeItemsPerSlot", "1")
                        .param("upgradeThoroughness", "1")
                        .param("upgradeItem", "51281")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(evaluation)).contains("Evaluate upgrade", "Back");
    }

    private MockHttpSession sessionWithFixture() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        MockHttpSession session = new MockHttpSession();
        editorService.openCharacter(session, character, null);
        return session;
    }

    private JsonNode pollStatusUntilDone(MockHttpSession session, long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        JsonNode status = null;
        while (System.nanoTime() < deadline) {
            MvcResult result = mockMvc.perform(get("/character/optimize/status").session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            status = MAPPER.readTree(html(result));
            if (!status.get("active").asBoolean()) {
                return status;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("optimizer job did not finish within " + timeoutMillis + " ms: " + status);
    }

    private String pollContent(MvcResult result, String needle, long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        String body = "";
        while (System.nanoTime() < deadline) {
            body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            if (body.contains(needle)) {
                return body;
            }
            Thread.sleep(25);
        }
        return body;
    }

    private static void awaitTerminal(OptimizerJobService.Job job, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (job.isActive() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(job.isActive())
                .as("job finished within %d ms (state %s)", timeoutMillis, job.getState())
                .isFalse();
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String tabPanel(String html, String name) {
        String marker = "data-tab-panel=\"" + name + "\"";
        int start = html.indexOf(marker);
        assertThat(start).as("tab panel %s is rendered", name).isGreaterThanOrEqualTo(0);
        int end = html.indexOf("</section>", start);
        assertThat(end).as("tab panel %s is closed", name).isGreaterThan(start);
        return html.substring(start, end);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
