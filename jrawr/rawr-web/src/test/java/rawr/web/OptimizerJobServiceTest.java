package rawr.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.data.CharacterFixtureLoader;
import rawr.data.UserItemStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline service-level tests for the Phase 8 optimizer jobs: a tiny deterministic pool on a Mage
 * fixture runs to completion, applies the chosen items and cleans up; a second job is rejected while
 * one is active; cancel returns to idle without touching the character. Timing is never asserted -
 * the tests poll with a bound.
 */
class OptimizerJobServiceTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    private static DataBootstrap bootstrap;
    private static CalculationService calculations;
    private static EditorService editor;
    private static OptimizerJobService jobs;

    @BeforeAll
    static void setUp() {
        bootstrap = new DataBootstrap(
                REPO_ROOT.resolve("jrawr/data").toString(),
                new UserItemStore(REPO_ROOT.resolve("jrawr/rawr-web/build/tmp/jrawr.items.xml")));
        bootstrap.load();
        calculations = new CalculationService(bootstrap);
        editor = new EditorService();
        jobs = new OptimizerJobService(calculations, editor);
    }

    @AfterAll
    static void tearDown() {
        jobs.shutdown();
    }

    @Test
    void runsJobAppliesItemsAndCleansUp() throws Exception {
        Character character = fixture();
        MockHttpSession session = new MockHttpSession();
        editor.openCharacter(session, character, null);
        float before = calculations.calculate(character).overallPoints();

        OptimizerJobService.Job job = jobs.start(session, character, tinyParams());
        awaitTerminal(job, 240_000);

        assertThat(job.getState()).as("job state").isEqualTo("done");
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getAfterPoints()).isGreaterThanOrEqualTo(job.getBeforePoints() - 0.01f);
        assertThat(job.getError()).isNull();
        assertThat(editor.isDirty(session)).as("a finished job waits for confirmation").isFalse();
        assertThat(jobs.activeJobCount()).isZero();
        assertThat(job.isActive()).isFalse();

        assertThat(jobs.apply(session)).as("apply accepts a finished result").isTrue();
        assertThat(editor.isDirty(session)).as("apply marked the character dirty").isTrue();
        float after = calculations.calculate(character).overallPoints();
        assertThat(after).as("session character was not made worse").isGreaterThanOrEqualTo(before - 0.01f);
    }

    @Test
    void rejectsSecondJobAndCancelLeavesCharacterUntouched() throws Exception {
        Character character = fixture();
        MockHttpSession session = new MockHttpSession();
        editor.openCharacter(session, character, null);
        int[] idsBefore = snapshot(character);

        ExecutorService gate = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        gate.execute(() -> {
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        OptimizerJobService gated = new OptimizerJobService(calculations, editor, gate);
        try {
            OptimizerJobService.Job job = gated.start(session, character, tinyParams());

            assertThatThrownBy(() -> gated.start(session, character, tinyParams()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already in progress");
            assertThat(gated.activeJobCount()).isEqualTo(1);

            assertThat(gated.cancel(session)).as("active job accepted the cancel").isTrue();
            release.countDown();
            awaitTerminal(job, 60_000);

            assertThat(job.getState()).isEqualTo("cancelled");
            assertThat(gated.activeJobCount()).isZero();
            assertThat(snapshot(character)).as("cancelled job left the character alone").isEqualTo(idsBefore);
            assertThat(editor.isDirty(session)).as("cancelled job did not mark dirty").isFalse();
        } finally {
            gate.shutdownNow();
        }
    }

    @Test
    void talentAwareJobAppliesTheSelectedTalents() throws Exception {
        Character character = fixture();
        MockHttpSession session = new MockHttpSession();
        editor.openCharacter(session, character, null);
        OptimizerJobService.OptimizerParams params = new OptimizerJobService.OptimizerParams(
                10, List.of(CharacterSlot.Head), 2, 1, null, null,
                false, true, true, false, false, false, true,
                OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                true, true, false, false);
        OptimizerJobService.Job job = jobs.start(session, character, params);
        awaitTerminal(job, 240_000);
        assertThat(job.getState()).as("job state").isEqualTo("done");
        OptimizerJobService.FinishedResult result = jobs.finishedResult(session).orElseThrow();
        String selectedTalents = result.optimized().getCurrentTalents().toString();
        Character saveCopy = jobs.optimizedSaveCopy(session, character);
        assertThat(saveCopy.getCurrentTalents().toString())
                .as("the separate optimized character includes the selected talents")
                .isEqualTo(selectedTalents);
        assertThat(jobs.apply(session)).isTrue();
        assertThat(editor.getOrCreate(session).getCurrentTalents().toString())
                .as("apply copies the selected talent spec")
                .isEqualTo(selectedTalents);
    }

    @Test
    void talentsOnlyJobKeepsGearFixedAndAppliesTheSelectedTalents() throws Exception {
        Character character = fixture();
        MockHttpSession session = new MockHttpSession();
        editor.openCharacter(session, character, null);
        String[] beforeGear = gearIds(character);
        OptimizerJobService.OptimizerParams params = new OptimizerJobService.OptimizerParams(
                25, OptimizerJobService.OPTIMIZE_SLOTS, 0, 1, null, null,
                false, false, false, false, false, false, true,
                OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                true, true, true, false);
        OptimizerJobService.Job job = jobs.start(session, character, params);
        awaitTerminal(job, 240_000);
        assertThat(job.getState()).as("job state").isEqualTo("done");
        assertThat(job.status().changedSlots()).as("talents-only keeps every gear slot").isEmpty();
        OptimizerJobService.FinishedResult result = jobs.finishedResult(session).orElseThrow();
        assertThat(gearIds(result.optimized())).as("optimized gear is the original gear")
                .isEqualTo(beforeGear);
        String selectedTalents = result.optimized().getCurrentTalents().toString();
        assertThat(jobs.apply(session)).isTrue();
        assertThat(gearIds(editor.getOrCreate(session))).isEqualTo(beforeGear);
        assertThat(editor.getOrCreate(session).getCurrentTalents().toString()).isEqualTo(selectedTalents);
    }

    private static String[] gearIds(Character character) {
        String[] ids = new String[19];
        for (int i = 0; i < ids.length; i++) {
            rawr.base.items.ItemInstance instance = character.get(CharacterSlot.fromValue(i));
            ids[i] = instance == null ? "" : instance.getGemmedId();
        }
        return ids;
    }

    @Test
    void repeatedJobsInOneProcessDoNotCarryModelState() throws Exception {
        float[] before = new float[2];
        float[] after = new float[2];
        for (int run = 0; run < 2; run++) {
            Character character = dpsdkFixture();
            MockHttpSession session = new MockHttpSession();
            editor.openCharacter(session, character, null);
            OptimizerJobService.OptimizerParams params = new OptimizerJobService.OptimizerParams(
                    3, OptimizerJobService.OPTIMIZE_SLOTS, 0, 1, "[Overall]", List.of(),
                    false, true, true, false, false, false, true,
                    OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                    false, false, false, false);
            OptimizerJobService.Job job = jobs.start(session, character, params);
            awaitTerminal(job, 240_000);
            assertThat(job.getState()).isEqualTo("done");
            before[run] = job.getBeforePoints();
            after[run] = job.getAfterPoints();
        }
        assertThat(before[1]).as("second job before score")
                .isCloseTo(before[0], org.assertj.core.data.Offset.offset(0.01f));
        assertThat(after[1]).as("second job after score")
                .isCloseTo(after[0], org.assertj.core.data.Offset.offset(0.01f));
    }

    @Test
    void startValidationRejectsIncompleteCharacters() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Character empty = new Character();
        empty.setCurrentModel("");
        editor.openCharacter(session, empty, null);
        assertThatThrownBy(() -> jobs.start(session, empty, tinyParams()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Choose a model");

        Character character = fixture();
        editor.openCharacter(session, character, null);
        assertThatThrownBy(() -> jobs.start(session, character,
                new OptimizerJobService.OptimizerParams(1, List.of(), 2, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one slot");
        assertThat(jobs.activeJobCount()).isZero();
    }

    @Test
    void parallelJobIsDeterministicAndReleasesEvaluationThreads() throws Exception {
        // DPSDK is on the verified parallel-safe allowlist (DECISIONS #58); the warm-up normalizes
        // its first-run-per-JVM state so the two compared jobs start from the same baseline.
        Character warmupCharacter = dpsdkFixture();
        MockHttpSession warmupSession = new MockHttpSession();
        editor.openCharacter(warmupSession, warmupCharacter, null);
        OptimizerJobService.Job warmup = jobs.start(warmupSession, warmupCharacter, tinyParams());
        awaitTerminal(warmup, 240_000);
        assertThat(warmup.getState()).isEqualTo("done");

        Character sequentialCharacter = dpsdkFixture();
        MockHttpSession sequentialSession = new MockHttpSession();
        editor.openCharacter(sequentialSession, sequentialCharacter, null);
        OptimizerJobService.Job sequential = jobs.start(sequentialSession, sequentialCharacter, tinyParams());
        awaitTerminal(sequential, 240_000);
        assertThat(sequential.getState()).isEqualTo("done");
        assertThat(jobs.apply(sequentialSession)).isTrue();
        int[] sequentialItems = snapshot(sequentialCharacter);
        float sequentialPoints = sequential.getAfterPoints();

        Character parallelCharacter = dpsdkFixture();
        MockHttpSession parallelSession = new MockHttpSession();
        editor.openCharacter(parallelSession, parallelCharacter, null);
        OptimizerJobService.Job parallel = jobs.start(parallelSession, parallelCharacter,
                new OptimizerJobService.OptimizerParams(1,
                        List.of(CharacterSlot.Head, CharacterSlot.Chest), 2, 3));
        awaitTerminal(parallel, 240_000);
        assertThat(parallel.getState()).isEqualTo("done");
        assertThat(jobs.apply(parallelSession)).isTrue();
        assertThat(snapshot(parallelCharacter)).as("parallel chose the same items").isEqualTo(sequentialItems);
        assertThat(parallel.getAfterPoints()).as("parallel scored identically").isEqualTo(sequentialPoints);
        assertThat(liveEvaluationThreads()).as("evaluation pool threads released").isZero();
    }

    private static long liveEvaluationThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith(rawr.base.optimizer.OptimizerBase.EVALUATION_THREAD_PREFIX))
                .count();
    }

    private static OptimizerJobService.OptimizerParams tinyParams() {
        return new OptimizerJobService.OptimizerParams(1,
                List.of(CharacterSlot.Head, CharacterSlot.Chest), 2, 1);
    }

    private static Character fixture() throws Exception {
        return CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
    }

    private static Character dpsdkFixture() throws Exception {
        return CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/dpsdk/dpsdk_unholy_277.xml"));
    }

    private static int[] snapshot(Character character) {
        int[] ids = new int[21];
        Arrays.fill(ids, -1);
        for (int i = 0; i < ids.length; i++) {
            rawr.base.items.ItemInstance instance = character.get(CharacterSlot.fromValue(i));
            if (instance != null) {
                ids[i] = instance.getId();
            }
        }
        return ids;
    }

    private static void awaitTerminal(OptimizerJobService.Job job, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (job.isActive() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(job.isActive())
                .as("job finished within %d ms (state %s)", timeoutMillis, job.getState())
                .isFalse();
    }
}
