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
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.talents.TalentsBase;
import rawr.data.CharacterFixtureLoader;
import rawr.data.SavedTalentSpecStore;
import rawr.models.mage.CalculationsMage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for the talent-spec browser/library: the candidate list, save/apply/
 * rename/delete, the per-apply glyph choice, and the optimizer include/exclude selection.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TalentSpecControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path DATA_DIR = REPO_ROOT.resolve("jrawr/data");
    private static final Path CHARACTERS_DIR = createTempDir("jrawr-specs-chars-test-");
    private static final Path TALENTS_FILE =
            createTempDir("jrawr-specs-test-").resolve("jrawr.talents.xml");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @Autowired
    private SavedTalentSpecStore talentSpecStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", DATA_DIR::toString);
        registry.add("jrawr.characters-dir", CHARACTERS_DIR::toString);
        registry.add("jrawr.talents-file", TALENTS_FILE::toString);
    }

    @BeforeEach
    void resetState() throws Exception {
        dataBootstrap.load();
        Files.deleteIfExists(TALENTS_FILE);
        talentSpecStore.load(DATA_DIR.resolve("Talents.xml"));
    }

    @Test
    void modalListsTheCurrentAndShippedMageSpecs() throws Exception {
        MockHttpSession session = sessionWithMage();
        MvcResult result = mockMvc.perform(get("/character/specs").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("Talent Specs", "specs-modal", "id=\"specs-body\"",
                        "Save current build", "Current", "Arcane", "Fireball")
                .contains("specs-save dialog-card", "specs-list-head", "dialog-section-title",
                        "Available specs")
                .contains("spec-source-shipped", "spec-source-current")
                .doesNotContain("<html");
    }

    @Test
    void modalShowsAChooseModelMessageWhenNoModelIsSelected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        editorService.getOrCreate(session);

        MvcResult result = mockMvc.perform(get("/character/specs").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result))
                .contains("Choose a class/model first")
                .doesNotContain("spec-source-", "Save current build");
    }

    @Test
    void saveCurrentAddsACustomSpecAndPersistsIt() throws Exception {
        MockHttpSession session = sessionWithMage();

        MvcResult saved = mockMvc.perform(post("/character/specs/save")
                        .param("name", "My Build")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(saved)).contains("Saved the current spec", "My Build");

        assertThat(TALENTS_FILE).as("side-car written").exists();
        assertThat(talentSpecStore.userSpecs()).hasSize(1);
        SavedTalentSpec spec = talentSpecStore.userSpecs().get(0);
        assertThat(spec.name).isEqualTo("My Build");
        assertThat(spec.characterClass).isEqualTo(CharacterClass.Mage);
        assertThat(spec.spec).isEqualTo(character(session).getCurrentTalents().toString());
    }

    @Test
    void saveCurrentRejectsABlankName() throws Exception {
        MockHttpSession session = sessionWithMage();

        MvcResult result = mockMvc.perform(post("/character/specs/save")
                        .param("name", "   ")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result)).contains("Enter a name for the spec");
        assertThat(talentSpecStore.userSpecs()).isEmpty();
    }

    @Test
    void applyReplacesTalentsAndRespectsTheGlyphChoice() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        SavedTalentSpec arcane = mageSpec("Arcane");
        TalentsBase arcaneTalents = arcane.talentSpec();
        String key = SavedTalentSpecStore.key(CharacterClass.Mage, arcane.spec);
        boolean[] beforeGlyphs = character.getCurrentTalents().getGlyphData().clone();

        mockMvc.perform(post("/character/specs/apply")
                        .param("key", key)
                        .param("includeGlyphs", "false")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getCurrentTalents().getData())
                .containsExactly(arcaneTalents.getData());
        assertThat(character.getCurrentTalents().getGlyphData())
                .containsExactly(beforeGlyphs);
        assertThat(editorService.isDirty(session)).isTrue();

        mockMvc.perform(post("/character/specs/apply")
                        .param("key", key)
                        .param("includeGlyphs", "true")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getCurrentTalents().getGlyphData())
                .containsExactly(arcaneTalents.getGlyphData());
    }

    @Test
    void applyRejectsASpecForAnotherClass() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        String before = character.getCurrentTalents().toString();

        mockMvc.perform(post("/character/specs/apply")
                        .param("key", "Warlock|000")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getCurrentTalents().toString()).isEqualTo(before);
    }

    @Test
    void renameAndDeleteEditOnlyCustomSpecs() throws Exception {
        MockHttpSession session = sessionWithMage();
        mockMvc.perform(post("/character/specs/save").param("name", "My Build").session(session))
                .andExpect(status().isOk());

        MvcResult renamed = mockMvc.perform(post("/character/specs/rename")
                        .param("index", "0")
                        .param("name", "Renamed Build")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(renamed)).contains("Renamed Build");
        assertThat(talentSpecStore.userSpecs().get(0).name).isEqualTo("Renamed Build");

        mockMvc.perform(post("/character/specs/delete")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(talentSpecStore.userSpecs()).isEmpty();
    }

    @Test
    void toggledOffSpecIsExcludedFromTheOptimizerTalentSearch() throws Exception {
        MockHttpSession session = sessionWithMage();
        Character character = character(session);
        SavedTalentSpec arcane = mageSpec("Arcane");
        String canonical = arcane.talentSpec().toString();

        List<TalentsBase> before = OptimizerJobService.buildTalentSpecs(character);
        assertThat(before).anyMatch(talents -> talents.toString().equals(canonical));

        mockMvc.perform(post("/character/specs/toggle")
                        .param("key", SavedTalentSpecStore.key(CharacterClass.Mage, arcane.spec))
                        .param("enabled", "false")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(talentSpecStore.disabledKeys())
                .contains(SavedTalentSpecStore.key(CharacterClass.Mage, arcane.spec));

        List<TalentsBase> after = OptimizerJobService.buildTalentSpecs(
                character, talentSpecStore.disabledKeys());
        assertThat(after).noneMatch(talents -> talents.toString().equals(canonical));
        assertThat(after).isNotEmpty();
    }

    // ------------------------------------------------------------------ helpers

    private static SavedTalentSpec mageSpec(String name) {
        return SavedTalentSpec.specsFor(CharacterClass.Mage).stream()
                .filter(spec -> name.equals(spec.name))
                .findFirst()
                .orElseThrow();
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

    private static Path createTempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
