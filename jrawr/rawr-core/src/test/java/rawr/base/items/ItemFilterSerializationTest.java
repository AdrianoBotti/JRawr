package rawr.base.items;

import rawr.base.util.XmlCanon;

import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterData;
import rawr.base.items.ItemFilterEnabledOverride;
import rawr.base.items.ItemFilterRegex;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemType;
import rawr.base.items.ItemTypeList;
import rawr.base.items.sources.BindsOn;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ItemFilter save/load/compile: the writer must emit the original XmlSerializer ItemFilterData
 * schema (compared canonically against {@code tools/core-format-probe} output), the reader must
 * restore the same tree, and the enabled-override helpers must toggle the runtime filter flags
 * used by {@link ItemFilter#isItemRelevant}.
 */
class ItemFilterSerializationTest {

    @TempDir
    Path tempDir;

    private ItemFilterData previousData;

    @BeforeEach
    void snapshot() throws Exception {
        previousData = data();
    }

    @AfterEach
    void restore() throws Exception {
        setData(previousData);
    }

    @Test
    void writerMatchesTheLegacyXmlSerializerSchema() throws Exception {
        setData(probeFilterData());
        Path out = tempDir.resolve("ItemFilter.xml");
        ItemFilter.save(out);

        assertThat(XmlCanon.canonical(out))
                .isEqualTo(XmlCanon.canonical(probeFile("core-format/item-filter.xml")));
    }

    @Test
    void loadRestoresTheTreeAndRoundTripsThroughSave() throws Exception {
        Path oracle = probeFile("core-format/item-filter.xml");
        ItemFilter.load(oracle);

        assertThat(ItemFilter.getFilterList()).hasSize(2);
        ItemFilterRegex first = ItemFilter.getFilterList().get(0);
        assertThat(first.getName()).isEqualTo("Probe Filter");
        assertThat(first.getPattern()).isEqualTo("Ulduar");
        assertThat(first.getMinItemLevel()).isEqualTo(200);
        assertThat(first.getMaxItemLevel()).isEqualTo(300);
        assertThat(first.isBoP()).isFalse();
        assertThat(first.isAppliesToGems()).isFalse();
        assertThat(first.regexList).hasSize(1);
        ItemFilterRegex sub = first.regexList.get(0);
        assertThat(sub.getName()).isEqualTo("Probe Sub");
        assertThat(sub.isEnabled()).isFalse();
        assertThat(sub.isAdditiveFilter()).isFalse();
        assertThat(sub.otherRegexEnabled).isFalse();
        assertThat(ItemFilter.getFilterList().get(1).getPattern()).isNull();
        assertThat(ItemFilter.isOtherEnabled()).isFalse();
        assertThat(data().relevantItemTypes.get("ProbeModel")).containsExactly(ItemType.None, ItemType.Cloth, ItemType.Staff);

        Path out = tempDir.resolve("ItemFilter-roundtrip.xml");
        ItemFilter.save(out);
        assertThat(XmlCanon.canonical(out)).isEqualTo(XmlCanon.canonical(oracle));
    }

    @Test
    void enabledOverrideApplicationTogglesTheFilters() throws Exception {
        ItemFilter.load(probeFile("core-format/item-filter.xml"));

        List<ItemFilterEnabledOverride> overrides = new ArrayList<ItemFilterEnabledOverride>();
        ItemFilterEnabledOverride first = new ItemFilterEnabledOverride();
        first.setName("Probe Filter");
        first.setEnabled(false);
        List<ItemFilterEnabledOverride> subs = new ArrayList<ItemFilterEnabledOverride>();
        ItemFilterEnabledOverride sub = new ItemFilterEnabledOverride();
        sub.setName("Probe Sub");
        sub.setEnabled(true);
        subs.add(sub);
        ItemFilterEnabledOverride subOther = new ItemFilterEnabledOverride();
        subOther.setName("Other");
        subOther.setEnabled(false);
        subs.add(subOther);
        first.setSubFilterOverride(subs);
        overrides.add(first);
        ItemFilterEnabledOverride second = new ItemFilterEnabledOverride();
        second.setName("Second");
        second.setEnabled(true);
        overrides.add(second);
        ItemFilterEnabledOverride other = new ItemFilterEnabledOverride();
        other.setName("Other");
        other.setEnabled(true);
        overrides.add(other);

        Character character = new Character();
        character.setItemFilterEnabledOverride(overrides);
        assertThat(character.loadItemFilterEnabledOverride()).isTrue();

        ItemFilterRegex probeFilter = ItemFilter.getFilterList().get(0);
        assertThat(probeFilter.isEnabled()).isFalse();
        assertThat(probeFilter.regexList.get(0).isEnabled()).isTrue();
        assertThat(probeFilter.otherRegexEnabled).isFalse();
        assertThat(ItemFilter.isOtherEnabled()).isTrue();

        // second application changes nothing
        assertThat(character.loadItemFilterEnabledOverride()).isFalse();
    }

    @Test
    void relevanceRespectsTheEnabledOverrides() throws Exception {
        ItemFilterData filterData = new ItemFilterData();
        filterData.relevantItemTypes.put("ProbeModel", new ItemTypeList(List.of(ItemType.None, ItemType.Cloth, ItemType.Staff)));
        ItemFilterRegex regex = new ItemFilterRegex();
        regex.setName("Location Filter");
        regex.setPattern("Ulduar");
        regex.setEnabled(false);
        filterData.regexList.add(regex);
        filterData.otherRegexEnabled = false;
        setData(filterData);

        CalculationsBase model = probeModel();
        Item item = new Item();
        item.setId(987654321);
        item.setType(ItemType.Cloth);
        item.setQuality(ItemQuality.Rare);
        item.setItemLevel(200);
        item.setBind(BindsOn.BoP);

        assertThat(ItemFilter.isItemRelevant(model, item)).isFalse();

        List<ItemFilterEnabledOverride> overrides = new ArrayList<ItemFilterEnabledOverride>();
        ItemFilterEnabledOverride override = new ItemFilterEnabledOverride();
        override.setName("Location Filter");
        override.setEnabled(true);
        overrides.add(override);
        ItemFilterEnabledOverride other = new ItemFilterEnabledOverride();
        other.setName("Other");
        other.setEnabled(true);
        overrides.add(other);
        Character character = new Character();
        character.setItemFilterEnabledOverride(overrides);
        character.loadItemFilterEnabledOverride();

        assertThat(ItemFilter.isItemRelevant(model, item)).isTrue();
    }

    @Test
    void compileRunsTheRelevantItemsPass() {
        ItemCache.clear();
        ItemFilter.compile();
        assertThat(ItemCache.allItems()).isEmpty();
    }

    static CalculationsBase probeModel() {
        return new CalculationsBase() {
            {
                name = "ProbeModel";
            }

            @Override
            public List<ItemType> getRelevantItemTypes() {
                return List.of(ItemType.None, ItemType.Cloth, ItemType.Staff);
            }

            @Override
            public ComparisonCalculationBase createNewComparisonCalculation() {
                return null;
            }

            @Override
            public List<GemmingTemplate> getDefaultGemmingTemplates() {
                return null;
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
                return null;
            }
        };
    }

    static ItemFilterData probeFilterData() {
        ItemFilterData filterData = new ItemFilterData();
        filterData.relevantItemTypes.put("ProbeModel", new ItemTypeList(List.of(ItemType.None, ItemType.Cloth, ItemType.Staff)));
        ItemFilterRegex regex = new ItemFilterRegex();
        regex.setName("Probe Filter");
        regex.setPattern("Ulduar");
        regex.setMinItemLevel(200);
        regex.setMaxItemLevel(300);
        regex.setMinItemQuality(ItemQuality.Temp);
        regex.setMaxItemQuality(ItemQuality.Heirloom);
        regex.setBoP(false);
        regex.setAppliesToGems(false);
        ItemFilterRegex sub = new ItemFilterRegex();
        sub.setName("Probe Sub");
        sub.setPattern("Hard Mode");
        sub.setEnabled(false);
        sub.setAdditiveFilter(false);
        sub.otherRegexEnabled = false;
        regex.regexList.add(sub);
        filterData.regexList.add(regex);
        ItemFilterRegex second = new ItemFilterRegex();
        second.setName("Second");
        filterData.regexList.add(second);
        filterData.otherRegexEnabled = false;
        return filterData;
    }

    static ItemFilterData data() throws Exception {
        Field field = ItemFilter.class.getDeclaredField("data");
        field.setAccessible(true);
        return (ItemFilterData) field.get(null);
    }

    static void setData(ItemFilterData value) throws Exception {
        Field field = ItemFilter.class.getDeclaredField("data");
        field.setAccessible(true);
        field.set(null, value);
    }

    static Path probeFile(String resource) throws Exception {
        return Path.of(ItemFilterSerializationTest.class.getClassLoader().getResource(resource).toURI());
    }
}
