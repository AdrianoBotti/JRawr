package rawr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for the character editor: create a session character, equip a real item
 * from the oracle fixtures, run the model, and check the import flow against the golden JSON.
 * Offline: only repository fixtures/goldens and the game-data caches are read.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CharacterPageTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MAGE_TALENTS = Pattern.compile("<MageTalents>([^<]*)</MageTalents>");
    private static final Pattern HEAD_SLOT = Pattern.compile("<Head>([^<]*)</Head>");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

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
    void editorCreatesCharacterEquipsFixtureItemAndRunsRatings() throws Exception {
        String fixture = fixture("arcane_277_raid.xml");
        Matcher talents = MAGE_TALENTS.matcher(fixture);
        assertThat(talents.find()).as("Mage talents in the fixture").isTrue();
        Matcher head = HEAD_SLOT.matcher(fixture);
        assertThat(head.find()).as("Head slot in the fixture").isTrue();
        int itemId = Integer.parseInt(head.group(1).split("\\.")[0]);
        Item item = ItemCache.findItemById(itemId);
        assertThat(item).as("fixture head item " + itemId + " in the item cache").isNotNull();

        MockHttpSession session = new MockHttpSession();

        MvcResult firstVisit = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(firstVisit)).contains("data-state=\"incomplete\"");

        mockMvc.perform(post("/character/meta")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(post("/character/talents")
                        .param("model", "Mage")
                        .param("talents", talents.group(1))
                        .session(session))
                .andExpect(status().isOk());

        MvcResult equip = mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(itemId))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(equip)).contains(item.getName(), "data-state=\"ok\"");

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String pageHtml = html(page);
        assertThat(pageHtml).contains(item.getName(), "quick-overall", "data-state=\"ok\"");
        assertThat(pageHtml).contains("workspace", "build-column", "charts-column",
                "id=\"charts-panel\"", "hx-trigger=\"load\"");
        assertThat(pageHtml).contains("Item pool", "hx-get=\"/character/availability\"");
        assertThat(pageHtml).doesNotContain("data-state=\"error\"");

        MvcResult items = mockMvc.perform(get("/items")
                        .param("slot", "Head")
                        .param("q", item.getName().substring(0, Math.min(6, item.getName().length()))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(items)).contains(item.getName()).doesNotContain("<html");

        MvcResult removed = mockMvc.perform(post("/character/slot/remove")
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(removed)).doesNotContain(item.getName()).contains("data-state=\"incomplete\"");
    }

    @Test
    void importsFixtureIntoEditorAndShowsGoldenOverallPoints() throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        JsonNode golden = MAPPER.readTree(
                REPO_ROOT.resolve("oracle/golden/mage/arcane_277_raid.json").toFile());
        String expectedOverallPoints = RatingFormat.format(
                (float) golden.get("ratings").get("OverallPoints").asDouble());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "arcane_277_raid.xml", "text/xml", xml))
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String pageHtml = html(page);
        assertThat(pageHtml).contains("quick-value", expectedOverallPoints);
        assertThat(pageHtml).doesNotContain("data-state=\"error\"");

        MvcResult fragment = mockMvc.perform(get("/character/ratings").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(fragment)).contains("Hit Rate", "data-state=\"ok\"").doesNotContain("<html");

        mockMvc.perform(get("/character").session(session)).andExpect(redirectedUrl("/"));
    }

    @Test
    void appJsShipsTheDirtyDotHtmxHook() throws Exception {
        MvcResult js = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(js)).contains("menu-dot", "htmx:afterRequest", "HX-Redirect",
                "/character/slot", "/character/save", "getElementById('item-picker')",
                "htmx:confirm", "confirm-modal", "data-confirm-ok",
                "data-tab-panel", "aria-selected", "data-optimize-setup-summary",
                "talentMode", "talents-only-button", "availability-checkbox",
                "data-availability-import-mode");
    }

    @Test
    void disablingBaseBuffAlsoDisablesItsImprovements() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/character/buff")
                        .param("name", "Demoralizing Shout")
                        .param("active", "true")
                        .session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/character/buff")
                        .param("name", "Improved Demoralizing Shout")
                        .param("active", "true")
                        .session(session))
                .andExpect(status().isOk());

        MvcResult off = mockMvc.perform(post("/character/buff")
                        .param("name", "Demoralizing Shout")
                        .param("active", "false")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(off))
                .doesNotContain("data-buff=\"Demoralizing Shout\" aria-pressed=\"true\"")
                .doesNotContain("data-buff=\"Improved Demoralizing Shout\" aria-pressed=\"true\"");
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        assertThat(character.activeBuffsContains("Improved Demoralizing Shout")).isFalse();
    }

    @Test
    void resetBuffButtonClearsSelectedBuffs() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/character/buff")
                        .param("name", "Arcane Intellect")
                        .param("active", "true")
                        .session(session))
                .andExpect(status().isOk());

        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(page).contains("/character/buffs/reset", "Reset buffs",
                "hx-confirm=\"Reset all buffs for this character?\"",
                "data-buff-toggle-all", "Collapse all");

        String reset = html(mockMvc.perform(post("/character/buffs/reset").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(reset).doesNotContain("data-buff=\"Arcane Intellect\" aria-pressed=\"true\"");
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        assertThat(character.activeBuffsContains("Arcane Intellect")).isFalse();
    }

    @Test
    void togglesBuffUpdatesMetaAndReportsTalentParseErrors() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        MvcResult toggled = mockMvc.perform(post("/character/buff")
                        .param("name", "Arcane Intellect")
                        .param("active", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(toggled))
                .contains("data-buff=\"Arcane Intellect\" aria-pressed=\"true\"")
                .contains("hx-swap-oob=\"true\"")
                .contains("Choose one buff per group", "class=\"buff-group is-active\"",
                        "data-buff-group=\"Intellect\"", "data-tip-host=\"true\"",
                        "data-buff-locked=\"true\"", "talent-tip-text")
                .doesNotContain("class=\"count\"");

        // htmx-driven meta change returns the editor fragment instead of a redirect
        mockMvc.perform(post("/character/meta")
                        .header("HX-Request", "true")
                        .param("name", "Smoke")
                        .param("race", "Troll")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("id=\"paperdoll\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("meta-form")));

        mockMvc.perform(post("/character/meta")
                        .param("name", "Smoke")
                        .param("race", "Troll")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("Smoke", "Troll");

        MvcResult badTalents = mockMvc.perform(post("/character/talents")
                        .param("model", "Mage")
                        .param("talents", "x".repeat(86))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(badTalents)).contains("data-talent-error=\"true\"");

        mockMvc.perform(post("/character/buff")
                        .param("name", "No Such Buff")
                        .param("active", "true")
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void copiedBuffCacheOmitsNonStandardCustomBuffs() {
        assertThat(Buff.getBuffByName("Camisa potenciadora de Aura")).isNull();
        assertThat(Buff.getBuffByName("Empuñadura de Titán Sin Reducción")).isNull();
        assertThat(Buff.getBuffByName("Arcane Intellect")).isNotNull();
    }

    @Test
    void characterShellListsEveryPortedModelRaceAndProfession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(page).contains("Class / model",
                "Primary profession", "Secondary profession", "identity-strip");
        assertThat(page).contains("class=\"topbar\"", "topbar-brand", "topbar-modes")
                .doesNotContain("<footer", "paperdoll-scorebar", "side-rail");
        assertThat(page).contains("optgroup label=\"Death Knight\"", "optgroup label=\"Druid\"",
                "optgroup label=\"Shaman\"", "optgroup label=\"Warrior\"");
        for (String model : CalculationService.MODELS) {
            assertThat(page).as("model option " + model).contains("value=\"" + model + "\"");
        }
        assertThat(page).contains("value=\"Alchemy\"", "value=\"Blacksmithing\"",
                "value=\"Inscription\"", "value=\"Jewelcrafting\"", "value=\"Mining\"",
                "value=\"Skinning\"", "value=\"Tailoring\"");
        assertThat(page).contains("Night Elf", "Blood Elf", "Death Knight");
        assertThat(page).doesNotContain("Choose a model (Mage or RestoSham)",
                "JRawr pilot — Mage and RestoSham");
    }

    @Test
    void metaProfessionsUpdateTheCharacterAndItsAutomaticBuffs() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        MvcResult updated = mockMvc.perform(post("/character/meta")
                        .header("HX-Request", "true")
                        .param("name", "Miner")
                        .param("race", "Dwarf")
                        .param("model", "Mage")
                        .param("primaryProfession", "Mining")
                        .param("secondaryProfession", "Skinning")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String response = html(updated);
        assertThat(response).contains("id=\"paperdoll\"", "value=\"Miner\"");
        // Profession changes out-of-band refresh the buff panel: both automatic buffs activate.
        assertThat(response).contains("data-buff=\"Toughness\" aria-pressed=\"true\"",
                "data-buff=\"Master of Anatomy\" aria-pressed=\"true\"");

        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(page).contains("value=\"Miner\"");
        assertThat(page).containsPattern("value=\"Mining\"[^>]*selected");
        assertThat(page).containsPattern("value=\"Skinning\"[^>]*selected");
    }

    @Test
    void calculationOptionsToggleCharacterFlagsAndRenderInTheGearSummary() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(page);
        assertThat(html).contains("name=\"enforceGemRequirements\"",
                "name=\"waistBlacksmithingSocket\"", "name=\"wristBlacksmithingSocket\"",
                "name=\"handsBlacksmithingSocket\"",
                "Gear options", "Blacksmithing sockets")
                .doesNotContain("gear-sockets-details");

        MvcResult updated = mockMvc.perform(post("/character/calculation-options")
                        .header("HX-Request", "true")
                        .param("waistBlacksmithingSocket", "true")
                        .param("wristBlacksmithingSocket", "true")
                        .param("handsBlacksmithingSocket", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Character character = editorService.getOrCreate(session);
        assertThat(character.getEnforceGemRequirements()).isFalse();
        assertThat(character.getWaistBlacksmithingSocketEnabled()).isTrue();
        assertThat(character.getWristBlacksmithingSocketEnabled()).isTrue();
        assertThat(character.getHandsBlacksmithingSocketEnabled()).isTrue();
        assertThat(editorService.isDirty(session)).isTrue();
        assertThat(html(updated)).contains("id=\"paperdoll\"", "name=\"enforceGemRequirements\"");
        assertThat(html(updated)).doesNotContainPattern(
                "name=\"enforceGemRequirements\"[^>]*checked");
        assertThat(html(updated)).containsPattern(
                "name=\"waistBlacksmithingSocket\"[^>]*checked");
    }

    private static String fixture(String name) throws Exception {
        return new String(Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures/mage/" + name)),
                StandardCharsets.UTF_8);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
