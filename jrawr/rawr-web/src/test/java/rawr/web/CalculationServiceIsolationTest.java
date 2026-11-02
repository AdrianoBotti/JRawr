package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import rawr.base.character.Character;
import rawr.data.CharacterFixtureLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Long-lived process isolation: a top-level calculation must not depend on a previous
 * calculation of the same model. The models keep carry-over state; the web service now creates a
 * fresh model instance per calculation, and the same character file is calculated twice here.
 */
@SpringBootTest
class CalculationServiceIsolationTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;
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
    void repeatedTopLevelCalculationsAreIndependentForEveryModel() throws Exception {
        for (String model : CalculationService.MODELS) {
            Character first = fixtureFor(model);
            WebCalculationResult one = calculationService.calculate(first);

            Character second = fixtureFor(model);
            WebCalculationResult two = calculationService.calculate(second);
            assertSameResult(model + " fresh character", one, two);

            WebCalculationResult three = calculationService.calculate(first);
            assertSameResult(model + " repeated character", one, three);
        }
    }

    private static void assertSameResult(String label, WebCalculationResult expected, WebCalculationResult actual) {
        assertThat(actual.overallPoints()).as(label + " overall")
                .isCloseTo(expected.overallPoints(), within(Math.max(1e-3f, Math.abs(expected.overallPoints()) * 1e-6f)));
        assertThat(actual.subPoints()).as(label + " sub-points").hasSameSizeAs(expected.subPoints());
        for (int i = 0; i < expected.subPoints().length; i++) {
            float tolerance = Math.max(1e-3f, Math.abs(expected.subPoints()[i]) * 1e-6f);
            assertThat(actual.subPoints()[i]).as(label + " sub-point " + i)
                    .isCloseTo(expected.subPoints()[i], within(tolerance));
        }
    }

    private static Character fixtureFor(String model) throws Exception {
        String folder = "ProtPaladin".equals(model) ? "protpally" : model.toLowerCase(Locale.ROOT);
        Path dir = REPO_ROOT.resolve("oracle/fixtures/" + folder);
        try (Stream<Path> files = Files.list(dir)) {
            Path fixture = files.filter(path -> path.toString().endsWith(".xml"))
                    .sorted()
                    .findFirst()
                    .orElseThrow();
            Character character = CharacterFixtureLoader.load(fixture);
            character.setCurrentModel(model);
            return character;
        }
    }
}
