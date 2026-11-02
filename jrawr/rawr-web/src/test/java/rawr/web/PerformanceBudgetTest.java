package rawr.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.data.CharacterFixtureLoader;
import rawr.data.UserItemStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generous wall-clock budgets for the main editor/optimizer paths. The defaults are deliberately
 * far above the recorded baselines so a slow machine does not make the build flaky; override them
 * with -Djrawr.budget.itemPickerMs / -Djrawr.budget.gearChartMs / -Djrawr.budget.optimizerJobMs.
 */
class PerformanceBudgetTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final long ITEM_PICKER_BUDGET_MS =
            Long.getLong("jrawr.budget.itemPickerMs", 20_000L);
    private static final long GEAR_CHART_BUDGET_MS =
            Long.getLong("jrawr.budget.gearChartMs", 30_000L);
    private static final long OPTIMIZER_JOB_BUDGET_MS =
            Long.getLong("jrawr.budget.optimizerJobMs", 120_000L);

    private static DataBootstrap bootstrap;
    private static CalculationService calculations;
    private static EditorService editor;
    private static OptimizerJobService jobs;
    private static ItemPickerService pickers;
    private static ComparisonService comparisons;

    @BeforeAll
    static void setUp() {
        bootstrap = new DataBootstrap(
                REPO_ROOT.resolve("jrawr/data").toString(),
                new UserItemStore(REPO_ROOT.resolve("jrawr/rawr-web/build/tmp/jrawr.performance.items.xml")));
        bootstrap.load();
        calculations = new CalculationService(bootstrap);
        editor = new EditorService();
        jobs = new OptimizerJobService(calculations, editor);
        ItemIconService icons = new ItemIconService("https://wow.zamimg.com");
        pickers = new ItemPickerService(calculations, icons);
        comparisons = new ComparisonService(calculations, icons);
    }

    @AfterAll
    static void tearDown() {
        jobs.shutdown();
    }

    @Test
    void itemPickerRatingsStayWithinBudget() throws Exception {
        Character character = fixture();
        calculations.calculate(character);
        long start = System.nanoTime();
        ItemPickerService.View view = pickers.search(character, CharacterSlot.Head, null, true,
                "overall", null);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        assertThat(view.rows()).isNotEmpty();
        assertThat(elapsed).as("item picker ratings ms").isLessThan(ITEM_PICKER_BUDGET_MS);
    }

    @Test
    void gearChartStaysWithinBudget() throws Exception {
        Character character = fixture();
        calculations.calculate(character);
        long start = System.nanoTime();
        ComparisonService.Chart chart = comparisons.gearChart(character, CharacterSlot.Head, 40);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        assertThat(chart.rows()).isNotEmpty();
        assertThat(elapsed).as("gear chart ms").isLessThan(GEAR_CHART_BUDGET_MS);
    }

    @Test
    void tinyOptimizerJobStaysWithinBudget() throws Exception {
        Character character = fixture();
        MockHttpSession session = new MockHttpSession();
        editor.openCharacter(session, character, null);
        long start = System.nanoTime();
        OptimizerJobService.Job job = jobs.start(session, character,
                new OptimizerJobService.OptimizerParams(1,
                        List.of(CharacterSlot.Head, CharacterSlot.Chest), 2, 1));
        awaitTerminal(job, OPTIMIZER_JOB_BUDGET_MS);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        assertThat(job.getState()).as("job state").isEqualTo("done");
        assertThat(elapsed).as("tiny optimizer job ms").isLessThan(OPTIMIZER_JOB_BUDGET_MS);
    }

    private static void awaitTerminal(OptimizerJobService.Job job, long budgetMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + budgetMs;
        while (job.isActive() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
        }
        assertThat(job.isActive()).as("optimizer job finished within the budget").isFalse();
    }

    private static Character fixture() throws Exception {
        return CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
    }
}
