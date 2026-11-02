package rawr.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.data.ItemSourceTemplates;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline tests for the Item editor's source selector: presets write the known template cookie,
 * the Custom option stores a user URL template, and invalid choices leave the current source
 * untouched. No network access.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemSourceControlTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path ITEMS_FILE = createItemsFile();
    private static final int THIRTY_DAYS_SECONDS = 30 * 24 * 60 * 60;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.items-file", ITEMS_FILE::toString);
    }

    @Test
    void editorDefaultsToTheWotlkdbPresetAndOffersEverySource() throws Exception {
        MvcResult editor = mockMvc.perform(get("/tools/item-editor"))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(editor);
        assertThat(html)
                .contains("Wowhead (WotLK)")
                .contains("wotlkdb (AoWoW)")
                .contains("EvoWow")
                .contains("Custom URL")
                .contains("wotlkdb.com")
                .contains("preset")
                .contains("/tools/item-editor/source")
                .doesNotContain("Use source")
                .doesNotContain("Change in Settings");
    }

    @Test
    void presetWritesItsTemplateCookie() throws Exception {
        MvcResult posted = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "wowhead"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = posted.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE);
        assertThat(cookie).as("source cookie").isNotNull();
        assertThat(cookie.getValue()).isEqualTo(
                "https://www.wowhead.com/wotlk/?item={id}&xml");
        assertThat(cookie.getMaxAge()).isEqualTo(THIRTY_DAYS_SECONDS);

        String html = html(posted);
        assertThat(html)
                .contains("www.wowhead.com")
                .contains("Wowhead (WotLK)")
                .contains("preset");
    }

    @Test
    void evowowPresetWritesItsTemplateCookie() throws Exception {
        MvcResult posted = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "evowow"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(posted.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE).getValue())
                .isEqualTo("https://wotlk.evowow.com/?item={id}&xml");
        assertThat(html(posted)).contains("wotlk.evowow.com");
    }

    @Test
    void customSourceStoresTheTemplateAndSaysWhoIsResponsible() throws Exception {
        String template = "https://db.example/item/{id}.xml";
        MvcResult posted = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "custom")
                        .param("template", template))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = posted.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE);
        assertThat(cookie).as("custom source cookie").isNotNull();
        assertThat(cookie.getValue()).isEqualTo(template);

        String html = html(posted);
        assertThat(html)
                .contains("db.example")
                .contains("custom source")
                .contains("your responsibility");
    }

    @Test
    void invalidCustomSourceShowsAnErrorAndKeepsThePreviousSource() throws Exception {
        MvcResult posted = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "custom")
                        .param("template", "https://db.example/item"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(posted.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE)).isNull();
        String html = html(posted);
        assertThat(html)
                .contains("Custom source must be an absolute http(s) URL template containing {id}")
                .contains("https://db.example/item")
                .contains("wotlkdb.com");

        MvcResult withPrevious = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "custom")
                        .param("template", "ftp://db.example/{id}")
                        .cookie(new Cookie(ItemSourceService.TEMPLATE_COOKIE,
                                "https://db.example/item/{id}.xml")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(withPrevious.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE)).isNull();
        assertThat(html(withPrevious))
                .contains("Custom source must be an absolute http(s) URL template containing {id}")
                .contains("ftp://db.example/{id}")
                .contains("db.example")
                .contains("custom source");
    }

    @Test
    void unknownPresetShowsAnError() throws Exception {
        MvcResult posted = mockMvc.perform(post("/tools/item-editor/source")
                        .param("preset", "missing"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(posted.getResponse().getCookie(ItemSourceService.TEMPLATE_COOKIE)).isNull();
        assertThat(html(posted)).contains("Choose an item source.").contains("wotlkdb.com");
    }

    @Test
    void settingsOnlyPointsAtTheItemEditor() throws Exception {
        MvcResult settings = mockMvc.perform(get("/settings").param("tab", "sources"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(settings))
                .doesNotContain("name=\"itemSourceTemplate\"", "Item lookup source", "Open Item editor")
                .contains("Icon source");
    }

    @Test
    void presetsExposeTheirCanonicalTemplates() {
        assertThat(ItemSourceTemplates.preset("wotlkdb").template())
                .isEqualTo(ItemSourceTemplates.DEFAULT_TEMPLATE);
        assertThat(ItemSourceTemplates.isValidTemplate(
                ItemSourceTemplates.preset("wowhead").template())).isTrue();
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Path createItemsFile() {
        try {
            return Files.createTempDirectory("jrawr-source-test-").resolve("jrawr.items.xml");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
