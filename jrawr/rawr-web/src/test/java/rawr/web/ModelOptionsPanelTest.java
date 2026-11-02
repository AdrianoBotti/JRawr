package rawr.web;

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
import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.models.dpsdk.CalculationOptionsDPSDK;
import rawr.models.dpswarr.CalculationOptionsDPSWarr;
import rawr.models.hunter.CalculationOptionsHunter;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.protwarr.CalculationOptionsProtWarr;
import rawr.models.restosham.CalculationOptionsRestoSham;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the per-model options panel: the curated Mage/RestoSham allowlists,
 * the reflective fallback used by the other 18 models (fields, sections, apply/recalc, reopen,
 * validation), inline validation that keeps the current options, and Reset to defaults. Runs
 * against the committed oracle fixtures; no network.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModelOptionsPanelTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Pattern QUICK_VALUE =
            Pattern.compile("class=\"quick-value\"[^>]*>([^<]+)<");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

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
    void magePanelListsAllowlistedFieldsGroupedBySection() throws Exception {
        MockHttpSession session = importFixture("mage/arcane_277_raid.xml", "Mage");

        MvcResult panel = mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(panel);
        assertThat(html).doesNotContain("<html").contains("id=\"options-panel\"");
        for (String key : List.of("fightDuration", "targetLevel", "playerLevel", "tpsLimit", "targetDamage",
                "latencyCast", "latencyGCD", "latencyChannel", "movementFrequency", "movementDuration",
                "heroismAvailable", "powerInfusionAvailable", "flameCap", "manaPotionEnabled",
                "manaGemEnabled", "evocationEnabled", "innervate", "maxUseAssumption",
                "additionalSegmentSplits", "cooldownRestrictions")) {
            assertThat(html).as("field %s", key).contains("data-option=\"" + key + "\"");
        }
        assertThat(html)
                .contains(">Fight<")
                .contains(">Movement &amp; Latency<")
                .contains(">Cooldowns &amp; Consumables<")
                .contains(">Advanced<");
        String fightDuration = region(html, "data-option=\"fightDuration\"", "data-option=\"targetLevel\"");
        assertThat(fightDuration).contains("value=\"300\"", "data-default=\"true\"");
        String playerLevel = region(html, "data-option=\"playerLevel\"", "data-option=\"tpsLimit\"");
        assertThat(playerLevel).contains("value=\"80\"", "data-default=\"true\"");

        // The editor page offers the modal from the header instead of embedding the panel.
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page))
                .contains("hx-get=\"/character/options\"", "options-open")
                .doesNotContain("id=\"options-panel\"", "data-option=\"fightDuration\"");
    }

    @Test
    void dpsWarrExposesStatWeightCheckboxes() throws Exception {
        MockHttpSession session = importFixture("dpswarr/dpswarr_fury_277.xml", "DPSWarr");

        String html = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        for (String key : List.of("statsStrength", "statsAgility", "statsAttackPower", "statsCrit",
                "statsHit", "statsExpertise", "statsHaste", "statsArmorPenetration")) {
            assertThat(html).as("field %s", key).contains("data-option=\"" + key + "\"");
        }
        assertThat(fieldHtml(html, "statsStrength")).contains("type=\"checkbox\"", "checked");

        mockMvc.perform(post("/character/options")
                        .param("model", "DPSWarr")
                        .param("statsStrength", "false")
                        .param("statsAgility", "true")
                        .session(session))
                .andExpect(status().isOk());
        CalculationOptionsDPSWarr options = (CalculationOptionsDPSWarr) options(session);
        assertThat(options.getStatsList()[0]).isFalse();
        assertThat(options.getStatsList()[1]).isTrue();

        String reopened = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(fieldHtml(reopened, "statsStrength")).doesNotContain("checked");
        assertThat(fieldHtml(reopened, "statsAgility")).contains("checked");
    }

    @Test
    void restoShamPanelUsesItsOwnAllowlist() throws Exception {
        MockHttpSession session = importFixture("restosham/resto_264_raid.xml", "RestoSham");

        String html = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        for (String key : List.of("fightLength", "targets", "heroism", "latency", "manaTideEveryCD",
                "waterShield", "earthShield", "innervates", "burstStyle", "sustStyle",
                "replenishmentPercentage", "survivalPerc", "activityPerc", "wsPops")) {
            assertThat(html).as("field %s", key).contains("data-option=\"" + key + "\"");
        }
        assertThat(html).doesNotContain("data-option=\"fightDuration\"");
        String targets = region(html, "data-option=\"targets\"", "data-option=\"heroism\"");
        assertThat(targets).contains("Heavy Raid", "selected=\"selected\"");
    }

    @Test
    void panelShowsHintWhenNoModelChosen() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String html = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(html).contains("data-options-hint=\"true\"", "Choose a model");
        assertThat(html).doesNotContain("data-option=");
    }

    @Test
    void headerOptionsButtonIsDisabledWithoutAModel() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        String button = region(page, "class=\"menu-item options-open\"", "</button>");
        assertThat(button).contains("disabled=\"disabled\"");
    }

    @Test
    void fallbackPanelRendersFieldsForEveryPortedNonCuratedModel() throws Exception {
        for (String[] fixture : FALLBACK_FIXTURES) {
            MockHttpSession session = importFixture(fixture[0], fixture[1]);
            String html = html(mockMvc.perform(get("/character/options").session(session))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(html).as("model %s", fixture[1])
                    .contains("data-option=")
                    .doesNotContain("data-options-hint");
        }
    }

    @Test
    void fallbackProtWarrTargetLevelUpdatesOptionsAndRating() throws Exception {
        MockHttpSession session = importFixture("protwarr/protwarr_277_raid.xml", "ProtWarr");
        assertFallbackApply(session, "ProtWarr", "targetLevel", "80");
        assertThat(((CalculationOptionsProtWarr) options(session)).getTargetLevel()).isEqualTo(80);
    }

    @Test
    void fallbackDPSDKFightLengthUpdatesOptionsAndRating() throws Exception {
        MockHttpSession session = importFixture("dpsdk/dpsdk_unholy_277.xml", "DPSDK");
        assertFallbackApply(session, "DPSDK", "fightLength", "2");
        assertThat(((CalculationOptionsDPSDK) options(session)).getFightLength()).isEqualTo(2f);
    }

    @Test
    void fallbackHunterTargetLevelUpdatesOptionsAndRating() throws Exception {
        MockHttpSession session = importFixture("hunter/hunter_264_raid.xml", "Hunter");
        assertFallbackApply(session, "Hunter", "targetLevel", "80");
        assertThat(((CalculationOptionsHunter) options(session)).targetLevel).isEqualTo(80);
    }

    @Test
    void fallbackInvalidInputShowsInlineErrorAndKeepsTheCharacterIntact() throws Exception {
        MockHttpSession session = importFixture("protwarr/protwarr_277_raid.xml", "ProtWarr");
        String before = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));

        MvcResult notANumber = mockMvc.perform(post("/character/options")
                        .param("model", "ProtWarr")
                        .param("targetLevel", "abc")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(notANumber))
                .contains("data-options-error=\"true\"", "Target Level", "id=\"options-panel\"")
                .doesNotContain("hx-swap-oob");

        MvcResult outOfRange = mockMvc.perform(post("/character/options")
                        .param("model", "ProtWarr")
                        .param("targetLevel", "999")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(outOfRange)).contains("data-options-error=\"true\"",
                "enter a number between 1 and 83");

        assertThat(((CalculationOptionsProtWarr) options(session)).getTargetLevel()).isEqualTo(83);
        String after = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));
        assertThat(after).isEqualTo(before);
    }

    @Test
    void metaHtmxResponseEnablesTheHeaderOptionsButtonForTheNewModel() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String html = html(mockMvc.perform(post("/character/meta")
                        .header("HX-Request", "true")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(html)
                .contains("hx-get=\"/character/options\"", "options-open")
                .doesNotContain("data-option=", "id=\"options-panel\"");
    }

    @Test
    void applyingFightDurationAndTargetLevelUpdatesOptionsAndRatings() throws Exception {
        MockHttpSession session = importFixture("mage/arcane_277_raid.xml", "Mage");
        String before = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));

        MvcResult applied = mockMvc.perform(post("/character/options")
                        .param("model", "Mage")
                        .param("fightDuration", "120")
                        .param("targetLevel", "80")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String appliedHtml = html(applied);
        assertThat(appliedHtml).contains("id=\"options-panel\"", "hx-swap-oob=\"true\"");
        String after = quickValue(appliedHtml);
        assertThat(after).isNotEqualTo(before);

        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        CalculationOptionsMage options = (CalculationOptionsMage) character.getCalculationOptions();
        assertThat(options.getFightDuration()).isEqualTo(120f);
        assertThat(options.getTargetLevel()).isEqualTo(80);

        String reopened = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        String fightDuration = region(reopened, "data-option=\"fightDuration\"", "data-option=\"targetLevel\"");
        assertThat(fightDuration).contains("value=\"120\"", "data-default=\"false\"");
        String targetLevel = region(reopened, "data-option=\"targetLevel\"", "data-option=\"playerLevel\"");
        assertThat(targetLevel).contains("value=\"80\"", "selected=\"selected\"");
        String playerLevel = region(reopened, "data-option=\"playerLevel\"", "data-option=\"tpsLimit\"");
        assertThat(playerLevel).contains("data-default=\"true\"");
    }

    @Test
    void invalidInputShowsInlineErrorAndKeepsTheCharacterIntact() throws Exception {
        MockHttpSession session = importFixture("mage/arcane_277_raid.xml", "Mage");
        String before = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));

        MvcResult notANumber = mockMvc.perform(post("/character/options")
                        .param("model", "Mage")
                        .param("fightDuration", "abc")
                        .param("targetLevel", "83")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(notANumber))
                .contains("data-options-error=\"true\"", "Fight duration", "id=\"options-panel\"")
                .doesNotContain("hx-swap-oob");

        mockMvc.perform(post("/character/options")
                        .param("model", "Mage")
                        .param("fightDuration", "-5")
                        .param("targetLevel", "83")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("data-options-error=\"true\"")));

        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        assertThat(((CalculationOptionsMage) character.getCalculationOptions()).getFightDuration())
                .isEqualTo(300f);

        String after = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));
        assertThat(after).isEqualTo(before);
    }

    @Test
    void resetRestoresModelDefaults() throws Exception {
        MockHttpSession session = importFixture("mage/arcane_277_raid.xml", "Mage");
        MvcResult applied = mockMvc.perform(post("/character/options")
                        .param("model", "Mage")
                        .param("fightDuration", "120")
                        .param("targetLevel", "80")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String changed = quickValue(html(applied));

        MvcResult reset = mockMvc.perform(post("/character/options/reset").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String resetHtml = html(reset);
        assertThat(resetHtml).contains("id=\"options-panel\"", "hx-swap-oob=\"true\"");

        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        CalculationOptionsMage options = (CalculationOptionsMage) character.getCalculationOptions();
        assertThat(options.getFightDuration()).isEqualTo(300f);
        assertThat(options.getTargetLevel()).isEqualTo(83);

        String reopened = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        String fightDuration = region(reopened, "data-option=\"fightDuration\"", "data-option=\"targetLevel\"");
        assertThat(fightDuration).contains("value=\"300\"", "data-default=\"true\"");

        assertThat(quickValue(resetHtml)).isNotEqualTo(changed);
    }

    @Test
    void resetRestoShamLeavesTheRestoOptionsObjectConsistent() throws Exception {
        MockHttpSession session = importFixture("restosham/resto_264_raid.xml", "RestoSham");
        mockMvc.perform(post("/character/options")
                        .param("model", "RestoSham")
                        .param("fightLength", "12")
                        .param("targets", "Tank")
                        .session(session))
                .andExpect(status().isOk());

        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        CalculationOptionsRestoSham options = (CalculationOptionsRestoSham) character.getCalculationOptions();
        assertThat(options.getFightLength()).isEqualTo(12f);
        assertThat(options.getTargets()).isEqualTo("Tank");

        mockMvc.perform(post("/character/options/reset").session(session))
                .andExpect(status().isOk());
        CalculationOptionsRestoSham reset = (CalculationOptionsRestoSham) character.getCalculationOptions();
        assertThat(reset.getFightLength()).isEqualTo(5f);
        assertThat(reset.getTargets()).isEqualTo("Raid");
    }

    // ------------------------------------------------------------------ helpers

    private static final List<String[]> FALLBACK_FIXTURES = List.of(
            new String[] { "cat/cat_277_raid.xml", "Cat" },
            new String[] { "bear/bear_277_raid.xml", "Bear" },
            new String[] { "moonkin/moonkin_264_raid.xml", "Moonkin" },
            new String[] { "elemental/elemental_264_raid.xml", "Elemental" },
            new String[] { "enhance/enhance_264_raid.xml", "Enhance" },
            new String[] { "healadin/healadin_264_nobuffs.xml", "Healadin" },
            new String[] { "healpriest/healpriest_264_raid.xml", "HealPriest" },
            new String[] { "shadowpriest/shadowpriest_264_raid.xml", "ShadowPriest" },
            new String[] { "protwarr/protwarr_277_raid.xml", "ProtWarr" },
            new String[] { "retribution/ret_264_raid.xml", "Retribution" },
            new String[] { "protpally/protpally_264_nobuffs.xml", "ProtPaladin" },
            new String[] { "hunter/hunter_264_raid.xml", "Hunter" },
            new String[] { "rogue/rogue_combat_277.xml", "Rogue" },
            new String[] { "tree/tree_264_raid.xml", "Tree" },
            new String[] { "tankdk/tankdk_blood_264_nobuffs.xml", "TankDK" },
            new String[] { "warlock/warlock_affliction_264.xml", "Warlock" },
            new String[] { "dpsdk/dpsdk_unholy_277.xml", "DPSDK" },
            new String[] { "dpswarr/dpswarr_arms_264.xml", "DPSWarr" });

    /** Applies a fallback field, then checks the panel, the session options and the rating. */
    private void assertFallbackApply(
            MockHttpSession session, String model, String field, String value) throws Exception {
        String before = quickValue(html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn()));
        String panel = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(panel).contains("data-option=\"" + field + "\"", ">Fight<");

        MvcResult applied = mockMvc.perform(post("/character/options")
                        .param("model", model)
                        .param(field, value)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String appliedHtml = html(applied);
        assertThat(appliedHtml).contains("id=\"options-panel\"", "hx-swap-oob=\"true\"");
        assertThat(quickValue(appliedHtml)).as("rating for %s.%s", model, field).isNotEqualTo(before);

        String reopened = html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(fieldHtml(reopened, field))
                .contains("value=\"" + value + "\"", "data-default=\"false\"");
    }

    private static ICalculationOptionBase options(MockHttpSession session) {
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        return character.getCalculationOptions();
    }

    private static String fieldHtml(String html, String key) {
        Matcher matcher = Pattern.compile("data-option=\"" + Pattern.quote(key) + "\"[^>]*>(.*?)</div>",
                Pattern.DOTALL).matcher(html);
        assertThat(matcher.find()).as("field %s in %s", key, html).isTrue();
        return matcher.group();
    }

    private MockHttpSession importFixture(String fixture, String model) throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures").resolve(fixture));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "fixture.xml", "text/xml", xml))
                        .param("model", model)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        return session;
    }

    private static String quickValue(String html) {
        Matcher matcher = QUICK_VALUE.matcher(html);
        assertThat(matcher.find()).as("quick rating in %s", html).isTrue();
        return matcher.group(1);
    }

    private static String region(String html, String start, String end) {
        int from = html.indexOf(start);
        int to = html.indexOf(end, from);
        assertThat(from).as("region start %s", start).isGreaterThanOrEqualTo(0);
        assertThat(to).as("region end %s", end).isGreaterThan(from);
        return html.substring(from, to);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
