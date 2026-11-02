package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import rawr.base.character.Character;
import rawr.data.CharacterFixtureLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency gate around the global engine lock: concurrent editor calculations must serialize,
 * complete, and return the same rating as a single calculation. This exercises the shared model,
 * item and buff caches that the web server touches from multiple request threads.
 */
@SpringBootTest
class CalculationConcurrencyTest {
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
    void concurrentCalculationsAreSerializedAndDeterministic() throws Exception {
        Character first = load();
        float expected = calculationService.calculate(first).overallPoints();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Float>> futures = new ArrayList<Future<Float>>();
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> calculationService.calculate(load()).overallPoints()));
            }
            for (Future<Float> future : futures) {
                assertThat(future.get(60, TimeUnit.SECONDS)).isEqualTo(expected);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static Character load() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        return character;
    }
}
