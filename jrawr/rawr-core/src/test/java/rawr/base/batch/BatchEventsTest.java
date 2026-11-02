package rawr.base.batch;

import rawr.base.util.KeyValuePair;

import rawr.base.batch.BatchCharacter;
import rawr.base.batch.BatchCharacterList;
import rawr.base.batch.BatchOptimizer;
import rawr.base.batch.BatchTools;
import rawr.base.batch.ComputeUpgradesCompletedEventArgs;
import rawr.base.batch.ComputeUpgradesProgressChangedEventArgs;
import rawr.base.batch.EvaluateUpgradeCompletedEventArgs;
import rawr.base.batch.OptimizeBatchCompletedEventArgs;
import rawr.base.batch.OptimizeCharacterProgressChangedEventArgs;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the listener-interface ports of the C# optimizer/batch events (AGENTS §8): the payloads, the
 * firing points that can be driven directly, and the no-listener guards.
 */
class BatchEventsTest {
    public static class DummyModel extends CalculationsBase {
        @Override
        public ComparisonCalculationBase createNewComparisonCalculation() {
            return new ComparisonCalculationUpgrades();
        }

        @Override
        public List<GemmingTemplate> getDefaultGemmingTemplates() {
            return new ArrayList<GemmingTemplate>();
        }

        @Override
        public ICalculationOptionBase deserializeDataObject(String xml) {
            return null;
        }

        @Override
        public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
            return null;
        }

        @Override
        public Stats getCharacterStats(Character character, Item additionalItem) {
            return new Stats();
        }
    }

    @Test
    void batchCharacterPropertyChangedFiresAtThePortedSetters() {
        BatchCharacter character = new BatchCharacter();
        List<String> properties = new ArrayList<String>();
        BatchCharacter.PropertyChangedListener listener = (sender, propertyName) -> properties.add(propertyName);
        character.addPropertyChangedListener(listener);

        character.setRelativePath("fixtures/TestMage.xml");
        character.setUnsavedChanges(true);
        character.setWeight(2f);

        // setRelativePath mirrors the C# order (Score, Character, Name); setUnsavedChanges fires
        // Name, Score, NewScore; Weight has no notification in the original.
        assertThat(properties).containsExactly("Score", "Character", "Name", "Name", "Score", "NewScore");

        character.removePropertyChangedListener(listener);
        character.setUnsavedChanges(false);
        assertThat(properties).hasSize(6);
    }

    @Test
    void batchToolsStatusUpdatedFiresFromUpdateStatusLabel() {
        BatchTools tools = new BatchTools();
        List<BatchTools> fired = new ArrayList<BatchTools>();
        tools.addStatusUpdatedListener(fired::add);

        tools.updateStatusLabel();

        assertThat(fired).containsExactly(tools);
        assertThat(tools.getStatus()).isNotNull();
        assertThat(tools.getProgress()).isZero();
    }

    @Test
    void batchToolsCompletionEventsFireAndAreGuarded() {
        BatchTools tools = new BatchTools();
        int[] operations = new int[1];
        int[] upgradeLists = new int[1];
        List<String> properties = new ArrayList<String>();
        tools.addOperationCompletedListener(sender -> operations[0]++);
        tools.addUpgradeListCompletedListener(sender -> upgradeLists[0]++);
        tools.addPropertyChangedListener((sender, propertyName) -> properties.add(propertyName));

        tools.onOperationCompleted();
        tools.onUpgradeListCompleted();
        tools.setBatchCharacterList(new BatchCharacterList());

        assertThat(operations[0]).isEqualTo(1);
        assertThat(upgradeLists[0]).isEqualTo(1);
        assertThat(properties).containsExactly("BatchCharacterList");
    }

    @Test
    void batchOptimizerEventsFireWithExpectedPayloads() {
        Calculations.registerModel("BatchEventsDummy", DummyModel.class);
        Character character = new Character();
        character.setCurrentModel("BatchEventsDummy");
        List<KeyValuePair<Character, Float>> batch = new ArrayList<KeyValuePair<Character, Float>>();
        batch.add(new KeyValuePair<Character, Float>(character, 1f));
        BatchOptimizer optimizer = new BatchOptimizer(batch, false, false, false);

        List<Float> bestValues = new ArrayList<Float>();
        int[] completions = new int[2];
        List<ComputeUpgradesProgressChangedEventArgs> upgradeProgress = new ArrayList<ComputeUpgradesProgressChangedEventArgs>();
        List<EvaluateUpgradeCompletedEventArgs> evaluations = new ArrayList<EvaluateUpgradeCompletedEventArgs>();
        optimizer.addOptimizeBatchProgressChangedListener((sender, e) -> bestValues.add(e.bestValue()));
        optimizer.addOptimizeBatchCompletedListener((sender, e) -> completions[0]++);
        optimizer.addComputeUpgradesProgressChangedListener((sender, e) -> upgradeProgress.add(e));
        optimizer.addComputeUpgradesCompletedListener((sender, e) -> completions[1]++);
        optimizer.addEvaluateUpgradeProgressChangedListener((sender, e) -> {
        });
        optimizer.addEvaluateUpgradeCompletedListener((sender, e) -> evaluations.add(e));

        // Direct call sites (the batch flow needs a full model/item cache): the protected On* raisers
        // are the same methods reportProgress and the synchronous entry points call.
        optimizer.onOptimizeBatchProgressChanged(new OptimizeCharacterProgressChangedEventArgs(42, 123.5f));
        optimizer.onOptimizeBatchCompleted(new OptimizeBatchCompletedEventArgs(null, null, 9f, null, false));
        // The original call site passes the two percentages in the swapped order
        // (SUSPECTED_BUGS #39); the args keep whatever the caller passed.
        optimizer.onComputeUpgradesProgressChanged(new ComputeUpgradesProgressChangedEventArgs(25, 75, "Item"));
        optimizer.onComputeUpgradesCompleted(new ComputeUpgradesCompletedEventArgs(null, null, false));
        optimizer.onEvaluateUpgradeCompleted(new EvaluateUpgradeCompletedEventArgs(6.5f, null, null, false));

        assertThat(bestValues).containsExactly(123.5f);
        assertThat(completions[0]).isEqualTo(1);
        assertThat(completions[1]).isEqualTo(1);
        assertThat(upgradeProgress).hasSize(1);
        assertThat(upgradeProgress.get(0).progressPercentage()).isEqualTo(25);
        assertThat(upgradeProgress.get(0).itemProgressPercentage()).isEqualTo(75);
        assertThat(upgradeProgress.get(0).currentItem()).isEqualTo("Item");
        assertThat(evaluations).hasSize(1);
        assertThat(evaluations.get(0).upgradeValue()).isEqualTo(6.5f);
    }
}
