package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.data.CharacterFixtureLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-model chart gate: every comparison-chart backend must run for every model on a real
 * fixture. The detailed values are covered by the chart/model goldens; this catches model-specific
 * option, talent, buff or stat assumptions that would otherwise only appear when a user opens the
 * chart tab.
 */
@SpringBootTest
class AllModelsChartSmokeTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private ComparisonService comparisonService;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void everyModelRunsEveryChartBackend() throws Exception {
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
            character.recalculateSetBonuses();

            assertThat(comparisonService.gearChart(character, CharacterSlot.Head, 3))
                    .as("%s item chart", model).isNotNull();
            assertThat(comparisonService.relativeStatsChart(character))
                    .as("%s relative stats", model).isNotNull();
            assertThat(comparisonService.statGraph(character, ComparisonService.StatGraphMode.STAT_CHANGE,
                    null, null, -1, 1)).as("%s stat graph", model).isNotNull();
            assertThat(comparisonService.enchantChart(character, "Head"))
                    .as("%s enchant chart", model).isNotNull();
            assertThat(comparisonService.talentChart(character).rows())
                    .as("%s talent chart", model).isNotEmpty();
            assertThat(comparisonService.glyphChart(character).rows())
                    .as("%s glyph chart", model).isNotEmpty();
            assertThat(comparisonService.buffChart(character, "Current"))
                    .as("%s buff chart", model).isNotNull();
            assertThat(comparisonService.gemChart(character, "Meta"))
                    .as("%s gem chart", model).isNotNull();
        }
    }
}
