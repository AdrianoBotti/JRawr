package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline checks for the Appearance modal: every palette group is rendered, the shell links to it,
 * and the browser theme engine ships with the documented tokens and presets.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppearanceTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void appearanceModalRendersPaletteGroupsAndFonts() throws Exception {
        String html = html(mockMvc.perform(get("/appearance"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(html)
                .contains("data-modal=\"appearance\"", "appearance-backdrop")
                .contains("role=\"dialog\"", "aria-modal=\"true\"", "aria-labelledby=\"appearance-title\"")
                .contains("data-theme-editor")
                .contains("appearance-pinned", "appearance-scroll")
                .contains("theme-preview-rail", "theme-preview-main", "theme-preview-chart")
                .contains("data-theme-library", "data-theme-name", "data-theme-save")
                .contains("data-theme-library-status")
                .contains("data-theme-preset=\"default\"")
                .contains("data-theme-preset=\"midnight\"")
                .contains("data-theme-preset=\"parchment\"")
                .contains("data-theme-preset=\"contrast\"")
                .contains("data-theme-token=\"--bg\"")
                .contains("data-theme-token=\"--accent\"")
                .contains("data-theme-token=\"--q-epic\"")
                .contains("data-theme-token=\"--class-mage\"")
                .contains("data-theme-token=\"--gem-red\"")
                .contains("data-theme-token=\"--spec-arcane\"")
                .contains("data-theme-font=\"--font-body\"")
                .contains("data-theme-font=\"--font-display\"")
                .contains("data-theme-font=\"--font-mono\"")
                .contains("data-theme-reset", "Reset palette")
                .doesNotContain("<html");
    }

    @Test
    void shellExposesAppearanceAsATopLevelEntryPoint() throws Exception {
        String shell = html(mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn());
        assertThat(shell)
                .contains("aria-label=\"Settings\"", "hx-get=\"/settings\"")
                .contains("aria-label=\"Appearance\"", "hx-get=\"/appearance\"")
                .contains("/js/theme.js")
                .contains("/css/tokens.css");
        assertThat(shell.indexOf("/js/theme.js"))
                .as("theme.js loads before the stylesheet to avoid a palette flash")
                .isLessThan(shell.indexOf("/css/tokens.css"));

        String settings = html(mockMvc.perform(get("/settings").param("tab", "appearance"))
                .andExpect(status().isOk()).andReturn());
        assertThat(settings)
                .doesNotContain("data-tab=\"appearance\"", "data-theme-editor")
                .doesNotContain("data-theme-preset=\"default\"");
    }

    @Test
    void stylesheetAndThemeEngineExposeEveryToken() throws Exception {
        String tokens = resource("/static/css/tokens.css");
        assertThat(tokens)
                .contains("--bg-top:", "--bg-deep:", "--panel:", "--card-highlight:")
                .contains("--text-faint:", "--on-accent:", "--on-solid:")
                .contains("--border-strong:", "--border-deep:")
                .contains("--info:", "--warning:", "--highlight-strong:")
                .contains("--font-mono:", "--class-mage:", "--gem-red:", "--spec-arcane:")
                .contains("color-mix(in srgb, var(--accent)");

        String components = resource("/static/css/components.css");
        assertThat(components)
                .contains(".theme-grid", ".theme-token", ".theme-preset", ".theme-preview")
                .contains(".modal-backdrop.appearance-backdrop", "background: transparent")
                .contains(".theme-preview-rail", ".theme-preview-chart", ".appearance-pinned")
                .contains(".theme-library-item", ".theme-library-swatch", ".theme-library-actions");

        String script = resource("/static/js/theme.js");
        assertThat(script)
                .contains("jrawr.palette.v1")
                .contains("PRESETS")
                .contains("'--bg'", "'--accent'")
                .contains("data-theme-token", "data-theme-hex", "data-theme-font")
                .contains("data-theme-preset", "data-theme-reset")
                .contains("data-theme-save", "data-theme-name", "data-theme-apply-theme",
                        "data-theme-delete-theme")
                .contains("jrawr.themes.v1", "readThemes", "saveCustomTheme", "deleteCustomTheme")
                .contains("localStorage.setItem", "style.setProperty");
    }

    private static String resource(String path) throws Exception {
        try (InputStream in = AppearanceTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path + " on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
