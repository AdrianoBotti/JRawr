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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-model coverage gate: every model exposed by the web calculation service must have a
 * fixture, a golden directory, and a successful calculation through the same service the editor
 * uses. The detailed numeric comparison remains in rawr-golden; this catches registration,
 * fixture/options plumbing and model-switch breakage across the whole set.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AllModelsSmokeTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private CalculationService calculationService;

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
    void everyModelOptionsPanelRenders() throws Exception {
        for (String model : CalculationService.MODELS) {
            String fixtureDir = "ProtPaladin".equals(model) ? "protpally"
                    : model.toLowerCase(java.util.Locale.ROOT);
            Path fixture;
            Path fixtures = REPO_ROOT.resolve("oracle/fixtures").resolve(fixtureDir);
            try (Stream<Path> files = Files.list(fixtures)) {
                fixture = files.filter(path -> path.toString().endsWith(".xml"))
                        .sorted().findFirst().orElseThrow();
            }
            Character character = CharacterFixtureLoader.load(fixture);
            character.setCurrentModel(model);
            MockHttpSession session = new MockHttpSession();
            editorService.openCharacter(session, character, null);
            MvcResult result = mockMvc.perform(get("/character/options").session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).as("%s options panel", model)
                    .contains("id=\"options-panel\"")
                    .doesNotContain("data-options-error");
        }
    }

    @Test
    void everyExposedModelHasAFixtureGoldenAndCalculation() throws Exception {
        assertThat(CalculationService.MODELS).hasSize(20);
        for (String model : CalculationService.MODELS) {
            String fixtureDir = "ProtPaladin".equals(model) ? "protpally"
                    : model.toLowerCase(java.util.Locale.ROOT);
            Path fixtures = REPO_ROOT.resolve("oracle/fixtures").resolve(fixtureDir);
            Path golden = REPO_ROOT.resolve("oracle/golden").resolve(fixtureDir);
            assertThat(Files.isDirectory(fixtures)).as("%s fixture directory", model).isTrue();
            assertThat(Files.isDirectory(golden)).as("%s golden directory", model).isTrue();

            Path fixture;
            try (Stream<Path> files = Files.list(fixtures)) {
                fixture = files.filter(path -> path.toString().endsWith(".xml"))
                        .sorted().findFirst()
                        .orElseThrow(() -> new AssertionError(model + " has no fixture"));
            }
            Character character = CharacterFixtureLoader.load(fixture);
            character.setCurrentModel(model);
            WebCalculationResult result = calculationService.calculate(character);
            assertThat(result).as("%s calculation", model).isNotNull();
            assertThat(result.overallPoints()).as("%s overall", model).isFinite();
            assertThat(result.subPoints()).as("%s sub-points", model).hasSizeGreaterThan(0);
        }
    }
}
