package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.ItemInstance;
import rawr.data.CharacterFixtureLoader;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.CalculationsMage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Offline tests for the comparison chart service: rating deltas against the empty slot,
 * deterministic ordering, exact-instance rows, and item-level availability edits.
 */
@SpringBootTest
class ComparisonServiceTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private ComparisonService comparisonService;

    @Autowired
    private UpgradeListService upgradeListService;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void ratesCandidatesAgainstAnEmptySlotAndKeepsTheEquippedInstance() throws Exception {
        Character character = loadCharacter();
        ComparisonService.Chart chart = comparisonService.gearChart(character, CharacterSlot.Head, 10);

        assertThat(chart.rows()).isNotEmpty().hasSizeLessThanOrEqualTo(11);
        assertThat(chart.subPointLabels()).isNotEmpty();
        assertThat(chart.rows()).anyMatch(ComparisonService.Row::equipped);
        ComparisonService.Row equipped = chart.rows().stream()
                .filter(ComparisonService.Row::equipped)
                .findFirst()
                .orElseThrow();
        assertThat(equipped.gemColors()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(chart.overallLabel()).isNotBlank();
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);

        float previous = Float.POSITIVE_INFINITY;
        for (ComparisonService.Row row : chart.rows()) {
            assertThat(row.overall()).isLessThanOrEqualTo(previous);
            previous = row.overall();
            assertThat(row.subPoints()).hasSizeGreaterThan(0);
        }

        ComparisonService.Row top = chart.rows().get(0);
        float expected = manualDelta(character, top.gemmedId());
        assertThat(top.overall()).isCloseTo(expected, within(0.01f));

        assertThat(chart.bars()).hasSize(chart.rows().size());
        ComparisonService.Bar topBar = chart.bars().get(top.gemmedId());
        assertThat(topBar.percent()).isEqualTo(100);
        assertThat(topBar.negative()).isFalse();
        assertThat(topBar.segments()).isNotEmpty();
        int segmentCount = 0;
        for (ComparisonService.Bar bar : chart.bars().values()) {
            assertThat(bar.percent()).isBetween(0, 100);
            assertThat(bar.segments()).isNotEmpty();
            segmentCount += bar.segments().size();
        }
        assertThat(segmentCount).isGreaterThanOrEqualTo(chart.rows().size());
    }

    @Test
    void equippedBaselineBarsMeasureTheDeltaButKeepTheSameOrder() throws Exception {
        Character character = loadCharacter();
        ComparisonService.Chart empty = comparisonService.gearChart(character, CharacterSlot.Head, 10);
        ComparisonService.Chart equippedBaseline = comparisonService.gearChart(
                character, CharacterSlot.Head, 10, ComparisonService.Baseline.EQUIPPED);

        assertThat(equippedBaseline.baseline()).isEqualTo(ComparisonService.Baseline.EQUIPPED);
        assertThat(equippedBaseline.rows()).extracting(ComparisonService.Row::gemmedId)
                .containsExactlyElementsOf(empty.rows().stream()
                        .map(ComparisonService.Row::gemmedId).toList());

        ComparisonService.Row equipped = equippedBaseline.rows().stream()
                .filter(ComparisonService.Row::equipped)
                .findFirst()
                .orElseThrow();
        assertThat(equippedBaseline.bars().get(equipped.gemmedId()).percent()).isZero();
        assertThat(equipped.equippedDeltaSubPoints()).isNotEmpty()
                .allMatch(subPoint -> subPoint.value() == 0f);
        assertThat(equippedBaseline.rows()).anyMatch(row ->
                !row.equipped() && equippedBaseline.bars().get(row.gemmedId()).percent() > 0);

        ComparisonService.Row other = equippedBaseline.rows().stream()
                .filter(row -> !row.equipped())
                .findFirst()
                .orElseThrow();
        assertThat(other.equippedDeltaSubPoints()).hasSameSizeAs(other.subPoints());
        for (int index = 0; index < other.subPoints().size(); index++) {
            assertThat(other.equippedDeltaSubPoints().get(index).value())
                    .isCloseTo(other.subPoints().get(index).value()
                                    - equipped.subPoints().get(index).value(),
                            within(0.0001f));
        }
    }

    @Test
    void mageOptionsIgnoreSerializedIncrementalSetSolverState() {
        CalculationOptionsMage options = (CalculationOptionsMage) CalculationsMage.getInstance()
                .deserializeDataObject("<CalculationOptionsMage>"
                        + "<IncrementalSetStateIndexes></IncrementalSetStateIndexes>"
                        + "<IncrementalSetSegments></IncrementalSetSegments>"
                        + "<IncrementalSetArmor></IncrementalSetArmor>"
                        + "<SmartOptimization>true</SmartOptimization>"
                        + "</CalculationOptionsMage>");
        assertThat(options.getIncrementalSetStateIndexes()).isNull();
        assertThat(options.getIncrementalSetSegments()).isNull();
        assertThat(options.getIncrementalSetArmor()).isNull();
        assertThat(options.getSmartOptimization()).isTrue();
    }

    @Test
    void availableItemsAreAlwaysCandidateRowsAndTheDiamondTogglesTheExactInstance() throws Exception {
        Character character = loadCharacter();
        List<ItemInstance> relevant = character.getRelevantItemInstances(CharacterSlot.Head);
        ItemInstance lowLevel = relevant.stream()
                .filter(instance -> instance.getItem() != null
                        && !instance.getGemmedId().equals(Integer.toString(instance.getId())))
                .min((left, right) -> Integer.compare(
                        left.getItem().getItemLevel(), right.getItem().getItemLevel()))
                .orElseThrow();
        String gemmedId = lowLevel.getGemmedId();

        comparisonService.setExactAvailable(character, gemmedId, true);
        assertThat(character.getAvailableItems()).contains(gemmedId);
        assertThat(character.getAvailableItems())
                .doesNotContain(Integer.toString(lowLevel.getId()));
        assertThat(comparisonService.isExactAvailable(character, gemmedId)).isTrue();
        assertThat(comparisonService.isAvailable(character, lowLevel)).isTrue();

        ComparisonService.Chart chart = comparisonService.gearChart(character, CharacterSlot.Head, 8);
        ComparisonService.Row row = chart.rows().stream()
                .filter(candidate -> candidate.gemmedId().equals(gemmedId))
                .findFirst()
                .orElseThrow();
        assertThat(row.exactAvailable()).isTrue();

        comparisonService.setExactAvailable(character, gemmedId, false);
        assertThat(character.getAvailableItems()).doesNotContain(gemmedId);
        assertThat(comparisonService.isExactAvailable(character, gemmedId)).isFalse();
        assertThat(comparisonService.isAvailable(character, lowLevel)).isFalse();
    }

    @Test
    void exactDiamondRemovalLeavesItemLevelAndOtherVariantEntriesAlone() throws Exception {
        Character character = loadCharacter();
        ItemInstance instance = character.getRelevantItemInstances(CharacterSlot.Head).stream()
                .filter(candidate -> candidate.getItem() != null
                        && !candidate.getGemmedId().equals(Integer.toString(candidate.getId())))
                .findFirst()
                .orElseThrow();
        String gemmed = instance.getGemmedId();
        String itemLevel = Integer.toString(instance.getId());
        String otherVariant = instance.getId() + ".0.0.0.0";
        character.getAvailableItems().add(itemLevel);
        character.getAvailableItems().add(otherVariant);
        character.getAvailableItems().add(gemmed);

        comparisonService.setExactAvailable(character, gemmed, false);

        assertThat(character.getAvailableItems())
                .contains(itemLevel, otherVariant)
                .doesNotContain(gemmed);
    }

    @Test
    void relativeStatsChartRatesEveryRelevantStatAndBuildsBars() throws Exception {
        Character character = loadCharacter();
        ComparisonService.RelativeStatsChart chart = comparisonService.relativeStatsChart(character);

        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.overallLabel()).isNotBlank();
        assertThat(chart.subPointLabels()).isNotEmpty();
        assertThat(chart.normalizedTo()).isNull();
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);
        assertThat(chart.bars()).hasSize(chart.rows().size());
        assertThat(chart.rows()).anyMatch(row -> row.displayName().equals("Spell Power"));
        assertThat(chart.rows()).anyMatch(row -> row.displayName().equals("Crit Rating"));

        float previous = Float.POSITIVE_INFINITY;
        for (ComparisonService.StatRow row : chart.rows()) {
            assertThat(row.overall()).isLessThanOrEqualTo(previous);
            previous = row.overall();
            assertThat(row.displayName()).isNotBlank();
            assertThat(row.description()).startsWith("If you had 1 more");
            assertThat(row.subPoints()).hasSize(chart.subPointLabels().size());
            ComparisonService.Bar bar = chart.bars().get(row.displayName());
            assertThat(bar).isNotNull();
            assertThat(bar.percent()).isBetween(0, 100);
            assertThat(bar.segments()).isNotEmpty();
        }
        assertThat(chart.rows().get(0).overall()).isGreaterThan(0f);
        assertThat(chart.bars().get(chart.rows().get(0).displayName()).percent()).isEqualTo(100);
    }

    @Test
    void relativeStatsNormalizesEnhancementToAttackPower() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/enhance/enhance_277_procs.xml"));
        character.setCurrentModel("Enhance");
        character.recalculateSetBonuses();
        Calculations.loadModel(rawr.models.enhance.CalculationsEnhance.class);

        ComparisonService.RelativeStatsChart chart = comparisonService.relativeStatsChart(character);

        assertThat(chart.normalizedTo()).isEqualTo("Attack Power");
        ComparisonService.StatRow attackPower = chart.rows().stream()
                .filter(row -> row.displayName().equals("Attack Power"))
                .findFirst()
                .orElseThrow();
        assertThat(attackPower.overall()).isCloseTo(1f, within(0.000001f));
        assertThat(chart.rows()).allMatch(row -> row.subPoints().size() == chart.subPointLabels().size());
    }


    @Test
    void statChangeGraphDrawsOneLinePerSelectedStat() throws Exception {
        Character character = loadCharacter();
        ComparisonService.StatGraph graph = comparisonService.statGraph(character,
                ComparisonService.StatGraphMode.STAT_CHANGE,
                List.of("HitRating", "HasteRating", "CritRating"), null, -1, 5);

        assertThat(graph.mode()).isEqualTo(ComparisonService.StatGraphMode.STAT_CHANGE);
        assertThat(graph.xLabel()).isEqualTo("Stat change");
        assertThat(graph.series()).hasSize(7);
        assertThat(graph.series()).extracting(ComparisonService.StatGraphSeries::key)
                .containsExactly("CritRating", "HasteRating", "HitRating", "Intellect", "Mp5",
                        "SpellPower", "Spirit");
        assertThat(graph.statOptions().stream().filter(ComparisonService.StatOption::selected)
                .map(ComparisonService.StatOption::key))
                .containsExactly("CritRating", "HasteRating", "HitRating");
        assertThat(graph.minX()).isEqualTo(-5f);
        assertThat(graph.maxX()).isEqualTo(5f);
        for (ComparisonService.StatGraphSeries line : graph.series()) {
            assertThat(line.pointsCsv().split(" ")).hasSize(11);
            assertThat(line.color()).startsWith("#");
        }
        assertThat(graph.statOptions()).anyMatch(option -> option.key().equals("HitRating") && option.selected());
        assertThat(graph.statOptions()).extracting(ComparisonService.StatOption::key)
                .doesNotContain("PVPTrinket", "MovementSpeed", "Health", "Stamina");
        assertThat(graph.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void scalingGraphUsesTheChosenBaseStatAndSelectedLines() throws Exception {
        Character character = loadCharacter();
        ComparisonService.StatGraph graph = comparisonService.statGraph(character,
                ComparisonService.StatGraphMode.SCALING,
                List.of("HitRating", "HasteRating", "CritRating"), "SpellPower", -1, 5);

        assertThat(graph.mode()).isEqualTo(ComparisonService.StatGraphMode.SCALING);
        assertThat(graph.baseStat()).isEqualTo("SpellPower");
        assertThat(graph.baseStatLabel()).isEqualTo("Spell Power");
        assertThat(graph.xLabel()).isEqualTo("Change in Spell Power");
        assertThat(graph.minX()).isEqualTo(-25f);
        assertThat(graph.maxX()).isEqualTo(25f);
        assertThat(graph.series()).hasSize(7);
        assertThat(graph.statOptions().stream().filter(ComparisonService.StatOption::selected)
                .map(ComparisonService.StatOption::key))
                .containsExactly("CritRating", "HasteRating", "HitRating");
        for (ComparisonService.StatGraphSeries line : graph.series()) {
            String[] points = line.pointsCsv().split(" ");
            assertThat(points).hasSize(11);
            assertThat(points[0]).startsWith("-25.0,");
            assertThat(points[10]).startsWith("25.0,");
        }
    }


    @Test
    void statGraphUsesTheLegacyCuratedListByDefault() throws Exception {
        Character character = loadCharacter();
        ComparisonService.StatGraph graph = comparisonService.statGraph(character,
                ComparisonService.StatGraphMode.STAT_CHANGE, null, null, -1, 5);

        assertThat(graph.series()).hasSize(7);
        assertThat(graph.series()).extracting(ComparisonService.StatGraphSeries::label)
                .containsExactly("Crit Rating", "Haste Rating", "Hit Rating", "Intellect",
                        "Mana per 5 sec", "Spell Power", "Spirit");
        assertThat(graph.statOptions()).extracting(ComparisonService.StatOption::key)
                .containsExactly("CritRating", "HasteRating", "HitRating", "Intellect", "Mp5",
                        "SpellPower", "Spirit");
        assertThat(graph.statOptions()).allMatch(ComparisonService.StatOption::selected);
    }


    @Test
    void directUpgradesReturnsOnlyPositiveGainsAcrossSlots() throws Exception {
        Character character = loadCharacter();
        ComparisonService.UpgradeChart chart = comparisonService.directUpgrades(character, 5);

        assertThat(chart.evaluated()).isGreaterThan(0);
        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.bars()).hasSize(chart.rows().size());
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);
        float previous = Float.POSITIVE_INFINITY;
        for (ComparisonService.UpgradeRow row : chart.rows()) {
            assertThat(row.gain()).isGreaterThan(0f);
            assertThat(row.gain()).isLessThanOrEqualTo(previous);
            previous = row.gain();
            assertThat(row.slot()).isNotEqualTo(CharacterSlot.None);
            assertThat(row.gainText()).startsWith("+");
            ItemInstance equipped = character.get(row.slot());
            if (equipped != null) {
                assertThat(row.gemmedId()).isNotEqualTo(equipped.getGemmedId());
            }
            ComparisonService.Bar bar = chart.bars().get(row.gemmedId());
            assertThat(bar).isNotNull();
            assertThat(bar.segments()).hasSize(1);
        }
    }


    @Test
    void enchantChartRatesCandidatesAgainstTheUnenchantedItem() throws Exception {
        Character character = loadCharacter();
        ComparisonService.EnchantChart chart = comparisonService.enchantChart(character, "Head");

        assertThat(chart.slot()).isEqualTo(CharacterSlot.Head);
        assertThat(chart.slotLabel()).isEqualTo("Head");
        assertThat(chart.slots()).extracting(ComparisonService.EnchantSlotOption::key)
                .contains("Head");
        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.rows()).anyMatch(ComparisonService.EnchantRow::equipped);
        assertThat(chart.rows()).anyMatch(row -> row.enchantId() == 0 && row.name().equals("No enchant"));
        assertThat(chart.bars()).hasSize(chart.rows().size());
        for (ComparisonService.EnchantRow row : chart.rows()) {
            assertThat(row.subPoints()).isNotEmpty();
            assertThat(chart.bars().get(Integer.toString(row.enchantId()))).isNotNull();
        }
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }


    @Test
    void talentChartRatesOnePointChangesFromTheCurrentBuild() throws Exception {
        Character character = loadCharacter();
        ComparisonService.TalentChart chart = comparisonService.talentChart(character);

        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.bars()).hasSize(chart.rows().size());
        assertThat(chart.rows()).anyMatch(ComparisonService.TalentRow::invested);
        assertThat(chart.rows()).anyMatch(ComparisonService.TalentRow::maxed);
        assertThat(chart.rows()).anyMatch(row -> row.rank() > 0 && row.rank() < row.maxPoints());
        for (ComparisonService.TalentRow row : chart.rows()) {
            assertThat(row.maxPoints()).isGreaterThan(0);
            assertThat(row.subPoints()).isNotEmpty();
            assertThat(chart.bars().get(Integer.toString(row.index()))).isNotNull();
        }
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void glyphChartRatesAddingAndRemovingRelevantGlyphs() throws Exception {
        Character character = loadCharacter();
        ComparisonService.GlyphChart chart = comparisonService.glyphChart(character);

        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.bars()).hasSize(chart.rows().size());
        assertThat(chart.rows()).anyMatch(ComparisonService.GlyphRow::major);
        for (ComparisonService.GlyphRow row : chart.rows()) {
            assertThat(row.subPoints()).isNotEmpty();
            assertThat(chart.bars().get(Integer.toString(row.index()))).isNotNull();
        }
    }


    @Test
    void buffChartComparesRelevantBuffsWithCurrentFilter() throws Exception {
        Character character = loadCharacter();
        ComparisonService.BuffChart chart = comparisonService.buffChart(character, "Current");

        assertThat(chart.filter()).isEqualTo("Current");
        assertThat(chart.groups()).contains("All", "Current");
        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.rows()).allMatch(ComparisonService.BuffRow::equipped);
        assertThat(chart.bars()).hasSize(chart.rows().size());
        for (ComparisonService.BuffRow row : chart.rows()) {
            assertThat(row.subPoints()).isNotEmpty();
            assertThat(chart.bars().get(row.name())).isNotNull();
        }
        assertThat(chart.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }


    @Test
    void gemChartRatesModelRelevantGemsForAColor() throws Exception {
        Character character = loadCharacter();
        ComparisonService.GemChart chart = comparisonService.gemChart(character, "Meta");

        assertThat(chart.colors()).contains("All", "Red", "Yellow", "Blue", "Meta");
        assertThat(chart.selectedColor()).isEqualTo("Meta");
        assertThat(chart.rows()).isNotEmpty();
        assertThat(chart.rows()).allMatch(row -> row.colorLabel().equals("Meta"));
        assertThat(chart.bars()).hasSize(chart.rows().size());
        for (ComparisonService.GemRow row : chart.rows()) {
            assertThat(row.subPoints()).isNotEmpty();
            assertThat(chart.bars().get(Integer.toString(row.itemId()))).isNotNull();
        }
    }


    @Test
    void upgradeListRunsTheOptimizerPerItemSearchOnABoundedPool() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/frost_264_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        Calculations.loadModel(CalculationsMage.class);

        UpgradeListService.Options options = new UpgradeListService.Options(
                1, List.of(CharacterSlot.Head), 10, "[Overall]", List.of(),
                false, false, true, true, OptimizationMethod.GeneticAlgorithm,
                GreedyOptimizationMethod.AllCombinations);
        UpgradeListService.UpgradeResult result = upgradeListService.compute(character, options);

        assertThat(result.slots()).isNotEmpty();
        assertThat(result.count()).isGreaterThan(0);
        UpgradeListService.SlotView head = result.slots().stream()
                .filter(slot -> slot.slot() == CharacterSlot.Head)
                .findFirst()
                .orElseThrow();
        assertThat(head.rows()).isNotEmpty();
        UpgradeListService.UpgradeItemView first = head.rows().get(0);
        assertThat(first.gain()).isGreaterThan(0f);
        assertThat(first.gainText()).startsWith("+");
        assertThat(first.itemId()).isPositive();
        assertThat(first.gemmedId()).isNotBlank();

        UpgradeListService.Evaluation evaluation = upgradeListService.evaluate(
                character, options, first.gemmedId());
        assertThat(evaluation.item().itemId()).isEqualTo(first.itemId());
        assertThat(evaluation.item().slot()).isNotEqualTo(CharacterSlot.None);
    }

    @Test
    void upgradeEvaluationMarksAnEquippedInstance() throws Exception {
        Character character = loadCharacter();
        ItemInstance equipped = character.get(CharacterSlot.Head);
        assertThat(equipped).as("fixture has a head item").isNotNull();

        UpgradeListService.Options options = new UpgradeListService.Options(
                1, List.of(CharacterSlot.Head), 10, "[Overall]", List.of(),
                false, false, true, true, OptimizationMethod.GeneticAlgorithm,
                GreedyOptimizationMethod.AllCombinations);
        UpgradeListService.Evaluation evaluation =
                upgradeListService.evaluate(character, options, equipped.getGemmedId());

        assertThat(evaluation.item().equipped()).isTrue();
    }

    private static Character loadCharacter() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        Calculations.loadModel(CalculationsMage.class);
        return character;
    }

    private static float manualDelta(Character character, String gemmedId) {
        CalculationsMage model = CalculationsMage.getInstance();
        model.clearCache();
        Character empty = character.clone();
        empty.set(CharacterSlot.Head, null);
        CharacterCalculationsBase baseline = model.getCharacterCalculations(empty, null, false, false, false);
        Character candidate = character.clone();
        candidate.set(CharacterSlot.Head, new ItemInstance(gemmedId));
        CharacterCalculationsBase result = model.getCharacterCalculations(candidate, null, false, false, false);
        model.clearCache();
        return result.getOverallPoints() - baseline.getOverallPoints();
    }
}
