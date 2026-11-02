package rawr.web;

import jakarta.servlet.http.Cookie;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the settings modal (icon base source URL + advanced template cookies,
 * site-layout resolution and the live example hint). No network access: icon URLs are only
 * asserted as strings.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettingsModalTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final String PROPERTY_SITE = "https://icons.test";
    // Unknown/fake host -> AoWoW layout (ItemIconService.LAYOUT_RULES).
    private static final String AO_WOW_PREFIX = PROPERTY_SITE + "/static/images/wow/icons/large/";
    private static final String EXAMPLE_ICON = "inv_helmet_150black";
    private static final String EXAMPLE_JPG = EXAMPLE_ICON + ".jpg";
    private static final String WOTLKDB_LARGE = "https://wotlkdb.com/static/images/wow/icons/large/";
    private static final int THIRTY_DAYS_SECONDS = 30 * 24 * 60 * 60;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.icon-base-url", () -> PROPERTY_SITE);
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void settingsPostNormalizesSiteBaseAndPaperdollUsesTheMappedLayout() throws Exception {
        MvcResult posted = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://WotLKDB.com/some/path/?q=1#frag"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        Cookie cookie = posted.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE);
        assertThat(cookie).as("settings cookie").isNotNull();
        assertThat(cookie.getValue()).isEqualTo("https://wotlkdb.com");
        assertThat(cookie.getMaxAge()).isEqualTo(THIRTY_DAYS_SECONDS);
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");

        MockHttpSession session = importFixture();
        MvcResult page = mockMvc.perform(get("/").session(session)
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com")))
                .andExpect(status().isOk())
                .andReturn();
        String body = html(page);
        assertThat(body)
                .contains("src=\"" + WOTLKDB_LARGE + EXAMPLE_JPG + "\"")
                .doesNotContain("src=\"" + AO_WOW_PREFIX);
    }

    @Test
    void wowheadBaseResolvesToZamimgLayout() throws Exception {
        MvcResult settings = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wowhead.com")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settings))
                .contains("https://wow.zamimg.com/images/wow/icons/large/" + EXAMPLE_JPG)
                .contains("value=\"https://wowhead.com\"");
    }

    @Test
    void evowowSubdomainUsesAoWowLayout() throws Exception {
        MvcResult settings = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://icons.evowow.com")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settings))
                .contains("https://icons.evowow.com/static/images/wow/icons/large/" + EXAMPLE_JPG);
    }

    @Test
    void unknownHostUsesAoWowLayoutAndRenderCdnUses56Layout() throws Exception {
        MvcResult unknown = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://icons.example.org")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(unknown))
                .contains("https://icons.example.org/static/images/wow/icons/large/" + EXAMPLE_JPG);

        MvcResult render = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://render.worldofwarcraft.com")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(render))
                .contains("https://render.worldofwarcraft.com/icons/56/" + EXAMPLE_JPG);
    }

    @Test
    void advancedTemplateCookieOverridesTheHostLayoutAndCanBeCleared() throws Exception {
        Cookie base = new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com");
        Cookie template = new Cookie(ItemIconService.TEMPLATE_COOKIE, "https://cdn.example/i/{icon}.png");

        MvcResult settings = mockMvc.perform(get("/settings").cookie(base, template))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settings))
                .contains("value=\"https://wotlkdb.com\"")
                .contains("value=\"https://cdn.example/i/{icon}.png\"")
                .contains("https://cdn.example/i/" + EXAMPLE_JPG.replace(".jpg", ".png"));

        MockHttpSession session = importFixture();
        MvcResult page = mockMvc.perform(get("/").session(session).cookie(base, template))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("src=\"https://cdn.example/i/" + EXAMPLE_ICON + ".png\"");

        MvcResult cleared = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://wotlkdb.com")
                        .param("iconTemplate", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        Cookie expired = cleared.getResponse().getCookie(ItemIconService.TEMPLATE_COOKIE);
        assertThat(expired).as("empty template clears the advanced cookie").isNotNull();
        assertThat(expired.getMaxAge()).isZero();
        assertThat(cleared.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE).getValue())
                .isEqualTo("https://wotlkdb.com");
    }

    @Test
    void invalidSubmissionsKeepThePreviousCookies() throws Exception {
        Cookie previousBase = new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com");
        Cookie previousTemplate = new Cookie(ItemIconService.TEMPLATE_COOKIE, "https://cdn.example/i/{icon}.png");

        MvcResult notHttp = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "ftp://cdn.example.com")
                        .cookie(previousBase, previousTemplate))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"))
                .andReturn();
        assertThat(notHttp.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE)).isNull();
        assertThat(notHttp.getResponse().getCookie(ItemIconService.TEMPLATE_COOKIE)).isNull();

        MvcResult noHost = mockMvc.perform(post("/settings").param("iconBaseUrl", "not a url"))
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"))
                .andReturn();
        assertThat(noHost.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE)).isNull();

        MvcResult emptyBase = mockMvc.perform(post("/settings").param("iconBaseUrl", ""))
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"))
                .andReturn();
        assertThat(emptyBase.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE)).isNull();

        MvcResult badTemplate = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://wotlkdb.com")
                        .param("iconTemplate", "https://cdn.example/i.jpg")
                        .cookie(previousBase, previousTemplate))
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"))
                .andReturn();
        assertThat(badTemplate.getResponse().getCookie(ItemIconService.BASE_URL_COOKIE)).isNull();
        assertThat(badTemplate.getResponse().getCookie(ItemIconService.TEMPLATE_COOKIE)).isNull();

        MvcResult nonHttpTemplate = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://wotlkdb.com")
                        .param("iconTemplate", "javascript:alert({icon})"))
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"))
                .andReturn();
        assertThat(nonHttpTemplate.getResponse().getCookie(ItemIconService.TEMPLATE_COOKIE)).isNull();
    }

    @Test
    void legacyFullUrlCookieIsNormalizedToItsOriginOnRead() throws Exception {
        MvcResult aowow = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE,
                                "https://x.example/static/images/wow/icons/large/")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(aowow))
                .contains("value=\"https://x.example\"")
                .contains("https://x.example/static/images/wow/icons/large/" + EXAMPLE_JPG);

        MvcResult zamimg = mockMvc.perform(get("/settings")
                        .cookie(new Cookie(ItemIconService.BASE_URL_COOKIE,
                                "https://wow.zamimg.com/images/wow/icons/large/")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(zamimg))
                .contains("value=\"https://wow.zamimg.com\"")
                .contains("https://wow.zamimg.com/images/wow/icons/large/" + EXAMPLE_JPG);
    }

    @Test
    void settingsModalShowsBaseFieldAdvancedTemplateAndExampleHint() throws Exception {
        MvcResult settingsTab = mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settingsTab))
                .contains("data-modal=\"settings\"")
                .contains("Icon source")
                .contains("name=\"iconBaseUrl\"")
                .contains("zamimg (default)", "wotlkdb", "Wowhead", "EvoWow", "WoW render CDN")
                .contains("value=\"" + PROPERTY_SITE + "\"")
                .contains("id=\"icon-base-hint\"")
                .contains("Example icon URL: " + AO_WOW_PREFIX + EXAMPLE_JPG)
                .contains("<details")
                .contains("name=\"iconTemplate\"")
                .contains("{icon}")
                .contains("data-icon-example=\"" + EXAMPLE_ICON + "\"")
                .contains("data-icon-layouts=\"")
                .doesNotContain("name=\"characterXml\"")
                .doesNotContain("<html");
    }

    @Test
    void settingsRendersCalculationAndIconsSectionsWithoutTabs() throws Exception {
        String settings = html(mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(settings)
                .contains("Calculation", "Icons")
                .contains("settings-section-title")
                .doesNotContain("role=\"tablist\"", "data-tab=\"calculation\"")
                .contains("name=\"hideProfEnchants\"", "name=\"procEffectMode\"",
                        "name=\"combinationEffectMode\"", "name=\"iconBaseUrl\"",
                        "name=\"iconTemplate\"")
                .doesNotContain("name=\"useMultithreading\"", "name=\"maxThreads\"",
                        "name=\"topGemmings\"", "name=\"itemSourceTemplate\"");

        String page = html(mockMvc.perform(get("/").param("open", "settings"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(page)
                .contains("data-modal=\"settings\"", "Calculation", "Icons");
    }

    @Test
    void iconSourceDropdownOffersTheKnownSites() throws Exception {
        MvcResult settings = mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settings))
                .contains("Icon source")
                .contains("zamimg (default)", "wotlkdb", "Wowhead", "EvoWow", "WoW render CDN")
                .contains("name=\"iconBaseUrl\"")
                .doesNotContain("id=\"icon-base-url\" type=\"text\"");
    }

    @Test
    void settingsPostValidationErrorReopensTheSingleSettingsPage() throws Exception {
        mockMvc.perform(post("/settings").param("iconBaseUrl", "not a url"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=settings&settingsError=icons"));
    }

    @Test
    void fileMenuContainsImportAndItOpensTheImportModal() throws Exception {
        MvcResult page = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page))
                .contains("hx-get=\"/character/import\"", ">Import…</button>");

        MvcResult modal = mockMvc.perform(get("/character/import"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(modal))
                .contains("data-modal=\"import\"", "name=\"characterXml\"")
                .doesNotContain("<html");
    }

    @Test
    void indexWithOpenImportFlagRendersTheImportFragment() throws Exception {
        MvcResult page = mockMvc.perform(get("/").param("open", "import"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page))
                .contains("data-modal=\"import\"")
                .contains("name=\"characterXml\"")
                .doesNotContain("name=\"iconBaseUrl\"");

        MvcResult errorPage = mockMvc.perform(get("/").param("open", "import")
                        .param("error", "missing-character-xml"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(errorPage)).contains("Paste the XML or choose a file.");

        MvcResult settingsPage = mockMvc.perform(get("/").param("open", "settings"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settingsPage)).contains("data-modal=\"settings\"");

        mockMvc.perform(get("/import")).andExpect(redirectedUrl("/?open=import"));

        MvcResult importModal = mockMvc.perform(get("/character/import"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(importModal)).contains("data-modal=\"import\"", "name=\"characterXml\"");
    }

    @Test
    void toolsIsATopLevelHubAndChildEditorsReturnToIt() throws Exception {
        MvcResult tools = mockMvc.perform(get("/tools"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(tools))
                .contains("data-modal=\"tools\"", "role=\"dialog\"", "aria-modal=\"true\"",
                        "aria-label=\"Close\"")
                .contains("Item filters", "Gemming templates", "Item costs");

        String settings = html(mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(settings).doesNotContain("data-tab=\"tools\"", "id=\"settings-panel-tools\"");

        String shell = html(mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn());
        assertThat(shell).contains("aria-label=\"Tools\"", "hx-get=\"/tools\"");

        for (String path : new String[] {
                "/character/item-filters", "/character/gemming-templates", "/character/item-costs" }) {
            String editor = html(mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(editor).as("tools back link in " + path)
                    .contains("Back to Tools", "hx-get=\"/tools\"");
        }
    }

    @Test
    void tooltipScriptHasNoCursorPositionerAndCssAnchorsOnHover() throws Exception {
        MvcResult page = mockMvc.perform(get("/").param("open", "settings"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page))
                .contains("/js/app.js")
                .doesNotContain("item-tooltip-box")
                .doesNotContain("floating-tooltip");

        MvcResult script = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(script))
                .doesNotContain("positionTooltip")
                .doesNotContain("item-tooltip-box")
                .doesNotContain("floating-tooltip")
                .doesNotContain("lastPointer")
                .contains("data-tab") // modal/tab handling is kept
                .contains("data-icon-layouts"); // live icon hint

        MvcResult css = mockMvc.perform(get("/css/components.css"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(css))
                .contains(".item-icon-wrap:hover .item-tooltip")
                .contains(".item-icon-wrap:focus-within .item-tooltip")
                .contains(".item-tooltip:empty")
                .contains(".tip-right .item-tooltip")
                .contains(".tip-left .item-tooltip")
                .contains(".tip-top .item-tooltip")
                .contains(".advanced")
                .contains("pointer-events: none");
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpSession importFixture() throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "arcane_277_raid.xml", "text/xml", xml))
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
