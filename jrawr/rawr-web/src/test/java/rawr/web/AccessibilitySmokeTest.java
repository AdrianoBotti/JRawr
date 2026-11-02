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
import rawr.base.character.Character;
import rawr.data.CharacterFixtureLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Markup-level accessibility smoke checks for the shell, character tabs and modal entry points.
 * This is not a full WCAG audit; it locks the landmarks, tab semantics and labelled controls that
 * the rest of the UI work must preserve.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessibilitySmokeTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void shellHasLandmarksLabelsAndTabSemantics() throws Exception {
        MockHttpSession session = session();
        String html = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk()).andReturn());

        assertThat(html)
                .contains("<html lang=\"en\"", "<main", "role=\"tablist\"", "role=\"tab\"",
                        "aria-selected=", "aria-label=\"Charts\"")
                .contains("aria-controls=\"build-mode\"", "aria-controls=\"optimize-mode\"")
                .contains("id=\"build-mode\"", "role=\"tabpanel\"",
                        "aria-labelledby=\"mode-tab-build\"")
                .contains("aria-label=\"Character\"", "aria-label=\"About\"",
                        "aria-label=\"Settings\"");
    }

    @Test
    void settingsDialogHasModalSemantics() throws Exception {
        MockHttpSession session = session();
        String html = html(mockMvc.perform(get("/").param("open", "settings").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(html)
                .contains("role=\"dialog\"", "aria-modal=\"true\"", "aria-labelledby=")
                .contains("data-modal-close")
                .contains("aria-label=\"Close\"");
    }

    private MockHttpSession session() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        MockHttpSession session = new MockHttpSession();
        editorService.openCharacter(session, character, null);
        return session;
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
