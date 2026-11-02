package rawr.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip tests for the user-saved talent-spec side-car: user specs are written with the legacy
 * {@code SavedTalentSpec} element schema and the disabled (excluded-from-optimizer) selection is
 * preserved, so a restart reloads the same library.
 */
class SavedTalentSpecStoreTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Test
    void roundTripsUserSpecsRenameDeleteAndDisabledKeys(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("jrawr.talents.xml");
        SavedTalentSpecStore store = new SavedTalentSpecStore(file);
        store.load(REPO_ROOT.resolve("jrawr/data/Talents.xml"));

        int shippedCount = store.shippedCount();
        assertThat(shippedCount).isPositive();
        assertThat(store.userSpecs()).isEmpty();

        SavedTalentSpec arcane = SavedTalentSpec.specsFor(CharacterClass.Mage).stream()
                .filter(spec -> "Arcane".equals(spec.name))
                .findFirst()
                .orElseThrow();
        SavedTalentSpec copy = new SavedTalentSpec();
        copy.name = "My Arcane";
        copy.characterClass = arcane.characterClass;
        copy.spec = arcane.spec;
        copy.tree1 = arcane.tree1;
        copy.tree2 = arcane.tree2;
        copy.tree3 = arcane.tree3;
        store.saveCurrent(copy);

        assertThat(store.userSpecs()).hasSize(1);
        assertThat(store.userSpecs().get(0).name).isEqualTo("My Arcane");
        assertThat(Files.exists(file)).as("side-car written on save").isTrue();

        String key = SavedTalentSpecStore.key(copy.characterClass, copy.spec);
        store.setEnabled(key, false);
        assertThat(store.disabledKeys()).containsExactly(key);
        assertThat(store.isEnabled(copy)).isFalse();

        store.rename(0, "Renamed Arcane");
        assertThat(store.userSpecs().get(0).name).isEqualTo("Renamed Arcane");

        // Restart: a fresh store over the same file reloads the merged registry.
        SavedTalentSpecStore reloaded = new SavedTalentSpecStore(file);
        reloaded.load(REPO_ROOT.resolve("jrawr/data/Talents.xml"));
        assertThat(reloaded.shippedCount()).isEqualTo(shippedCount);
        assertThat(reloaded.userSpecs()).hasSize(1);
        assertThat(reloaded.userSpecs().get(0).name).isEqualTo("Renamed Arcane");
        assertThat(reloaded.disabledKeys()).containsExactly(key);
        assertThat(SavedTalentSpec.getAllSpecs()).hasSize(shippedCount + 1);

        reloaded.delete(0);
        assertThat(reloaded.userSpecs()).isEmpty();
        assertThat(reloaded.disabledKeys()).isEmpty();
        assertThat(SavedTalentSpec.getAllSpecs()).hasSize(shippedCount);
    }

    @Test
    void reSavingTheSameAllocationReplacesRatherThanDuplicates(@TempDir Path tempDir) {
        Path file = tempDir.resolve("jrawr.talents.xml");
        SavedTalentSpecStore store = new SavedTalentSpecStore(file);
        store.load(REPO_ROOT.resolve("jrawr/data/Talents.xml"));

        SavedTalentSpec arcane = SavedTalentSpec.specsFor(CharacterClass.Mage).stream()
                .filter(spec -> "Arcane".equals(spec.name))
                .findFirst()
                .orElseThrow();

        SavedTalentSpec first = new SavedTalentSpec();
        first.name = "First";
        first.characterClass = arcane.characterClass;
        first.spec = arcane.spec;
        store.saveCurrent(first);

        SavedTalentSpec second = new SavedTalentSpec();
        second.name = "Second";
        second.characterClass = arcane.characterClass;
        second.spec = arcane.spec;
        store.saveCurrent(second);

        assertThat(store.userSpecs()).hasSize(1);
        assertThat(store.userSpecs().get(0).name).isEqualTo("Second");
    }
}
