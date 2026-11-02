package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemSlot;
import rawr.data.CharacterFixtureLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for the optimizer input state: the availability/optimizer XML fields and the
 * pool builder's treatment of the character availability list, fallback, equipped gear/enchants
 * and the reenchant override.
 */
@SpringBootTest
class OptimizerPoolTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
        Calculations.loadModel(Calculations.getModel("Mage").getClass());
    }

    @Test
    void savesAndLoadsAvailabilityAndOptimizerSettings(@TempDir Path tempDir) throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>(List.of(
                "51281", "-3820", "51281.41285.40133.0.3820")));
        character.setCalculationToOptimize("[SubPoint 0]");
        OptimizationRequirement requirement = new OptimizationRequirement();
        requirement.calculation = "[Overall]";
        requirement.lessThan = true;
        requirement.value = 12345.5f;
        character.setOptimizationRequirements(new ArrayList<OptimizationRequirement>(List.of(requirement)));

        Path saved = tempDir.resolve("optimizer-state.xml");
        character.save(saved);

        Character loaded = CharacterFixtureLoader.load(saved);
        assertThat(loaded.getAvailableItems())
                .containsExactly("51281", "-3820", "51281.41285.40133.0.3820");
        assertThat(loaded.getCalculationToOptimize()).isEqualTo("[SubPoint 0]");
        assertThat(loaded.getOptimizationRequirements()).hasSize(1);
        assertThat(loaded.getOptimizationRequirements().get(0).calculation).isEqualTo("[Overall]");
        assertThat(loaded.getOptimizationRequirements().get(0).lessThan).isTrue();
        assertThat(loaded.getOptimizationRequirements().get(0).value).isEqualTo(12345.5f);
    }

    @Test
    void cloneCopiesBlacksmithingAndOptimizerSettings() throws Exception {
        Character character = fixture();
        character.setWaistBlacksmithingSocketEnabled(true);
        character.setHandsBlacksmithingSocketEnabled(true);
        character.setWristBlacksmithingSocketEnabled(true);
        character.setCalculationToOptimize("[SubPoint 1]");
        OptimizationRequirement requirement = new OptimizationRequirement();
        requirement.calculation = "[Overall]";
        requirement.value = 1f;
        character.setOptimizationRequirements(List.of(requirement));

        Character clone = character.clone();
        assertThat(clone.getWaistBlacksmithingSocketEnabled()).isTrue();
        assertThat(clone.getHandsBlacksmithingSocketEnabled()).isTrue();
        assertThat(clone.getWristBlacksmithingSocketEnabled()).isTrue();
        assertThat(clone.getCalculationToOptimize()).isEqualTo("[SubPoint 1]");
        assertThat(clone.getOptimizationRequirements()).isEqualTo(character.getOptimizationRequirements());
    }

    @Test
    void buildsPoolFromAvailableItemsAndKeepsEquippedGear() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>(List.of("51281", "-3820")));

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), false, false));

        assertThat(pool).contains("51281", "-3820");
        assertThat(pool).contains("51281.41285.40133.0.3820");
    }

    @Test
    void fallsBackToRelevantItemsWhenAvailabilityIsEmpty() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>());

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), false, false));

        assertThat(pool).isNotEmpty();
        assertThat(pool).contains("51281.41285.40133.0.3820");
    }

    @Test
    void overrideReenchantAddsRelevantEnchantAvailabilityTokens() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>());

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), true, false));

        Enchant arcanum = Enchant.findEnchant(3820, ItemSlot.Head, character);
        String token = Integer.toString(-1 * (arcanum.id + (10000 * arcanum.slot.ordinal())));
        assertThat(pool).contains(token);
    }

    @Test
    void allRelevantModeIgnoresTheAvailabilityList() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>(List.of("51281")));

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), false, true));

        assertThat(pool.size()).isGreaterThan(1);
        assertThat(pool).contains("51281.41285.40133.0.3820");
    }

    @Test
    void allRelevantPoolExcludesGearRestrictedToOtherClasses() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>());

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), false, true));

        assertThat(pool)
                .as("the Mage pool keeps its own tier head")
                .contains("51158")
                .as("Warlock tier head (Sanctified Dark Coven Hood) must be filtered out")
                .doesNotContain("51208");
    }

    @Test
    void allRelevantPoolHonorsThePerSlotCap() throws Exception {
        Character character = fixture();
        character.setAvailableItems(new ArrayList<String>());
        int cap = 5;

        List<String> pool = OptimizerJobService.buildPool(character,
                Calculations.getModel("Mage"), params(List.of(CharacterSlot.Head), false, true, cap));

        int itemIds = 0;
        for (String entry : pool) {
            if (entry.isEmpty() || entry.charAt(0) == '-' || entry.indexOf('.') >= 0) {
                continue;
            }
            itemIds++;
        }
        assertThat(itemIds).as("capped head items plus the equipped item id").isLessThanOrEqualTo(cap + 1);
        assertThat(pool).contains("51281.41285.40133.0.3820");
    }

    private static OptimizerJobService.OptimizerParams params(List<CharacterSlot> slots,
            boolean overrideReenchant, boolean allRelevant) {
        return params(slots, overrideReenchant, allRelevant, 0);
    }

    private static OptimizerJobService.OptimizerParams params(List<CharacterSlot> slots,
            boolean overrideReenchant, boolean allRelevant, int itemsPerSlot) {
        return new OptimizerJobService.OptimizerParams(1, slots, itemsPerSlot, 1, null, null,
                false, overrideReenchant, true, false, false, false, allRelevant,
                OptimizationMethod.GeneticAlgorithm, GreedyOptimizationMethod.AllCombinations,
                false, false, false, false);
    }

    private static Character fixture() throws Exception {
        return CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
    }
}
