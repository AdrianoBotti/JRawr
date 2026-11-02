package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterRegex;
import rawr.base.items.ItemType;
import rawr.base.optimizer.ItemAvailabilityInformation;
import rawr.base.optimizer.OptimizerValues;
import rawr.data.SavedItemFilterStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B-010: item filter tree, item-type refinement, gemming templates and item costs. */
@SpringBootTest
@AutoConfigureMockMvc
class B010EditorsTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path BASE_FILTER = REPO_ROOT.resolve("jrawr/data/ItemFilter.xml");
    private static final Path FILTER_FILE = REPO_ROOT.resolve(
            "jrawr/rawr-web/build/tmp/b010-item-filters.xml");
    private static final Path COST_FILE = REPO_ROOT.resolve(
            "jrawr/rawr-web/build/tmp/b010-item-costs.xml");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private GlobalSettingsService globalSettings;

    @Autowired
    private ComparisonService comparisonService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.item-filters-file", FILTER_FILE::toString);
        registry.add("jrawr.item-costs-file", COST_FILE::toString);
    }

    @org.junit.jupiter.api.AfterEach
    void resetSettings() {
        globalSettings.apply(GlobalSettings.defaults());
    }

    @BeforeEach
    void reset() throws Exception {
        dataBootstrap.load();
        Files.deleteIfExists(FILTER_FILE);
        Files.deleteIfExists(COST_FILE);
        if (Files.isRegularFile(BASE_FILTER)) {
            ItemFilter.load(BASE_FILTER);
        }
        for (Item item : ItemCache.allItems()) {
            item.setCost(0f);
        }
    }

    @Test
    void filterEditorAddsUpdatesPersistsAndDeletes() throws Exception {
        MockHttpSession session = importFixture();
        String modal = html(mockMvc.perform(get("/character/item-filters").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal).contains("Item filters", "New filter", "Relevant item types for Mage",
                "Save filter", "Include by default");

        int before = ItemFilter.getFilterList().size();
        String added = html(mockMvc.perform(post("/character/item-filters/add").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(ItemFilter.getFilterList()).hasSize(before + 1);
        String path = Integer.toString(ItemFilter.getFilterList().size() - 1);
        assertThat(added).contains("New Filter", "id=\"filters-panel\"");

        String updated = html(mockMvc.perform(post("/character/item-filters/node")
                        .param("path", path)
                        .param("name", "Probe filter")
                        .param("pattern", "Probe")
                        .param("minItemLevel", "10")
                        .param("maxItemLevel", "200")
                        .param("minItemQuality", "Uncommon")
                        .param("maxItemQuality", "Epic")
                        .param("boP", "on")
                        .param("boE", "on")
                        .param("boN", "on")
                        .param("additiveFilter", "on")
                        .param("appliesToItems", "on")
                        .param("appliesToGems", "on")
                        .param("enabled", "on")
                        .param("otherRegexEnabled", "on")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        ItemFilterRegex probe = ItemFilter.getFilterList().get(ItemFilter.getFilterList().size() - 1);
        assertThat(probe.getName()).isEqualTo("Probe filter");
        assertThat(probe.getPattern()).isEqualTo("Probe");
        assertThat(probe.getMinItemLevel()).isEqualTo(10);
        assertThat(probe.getMaxItemLevel()).isEqualTo(200);
        assertThat(updated).contains("Probe filter");

        mockMvc.perform(post("/character/item-filters/types")
                        .param("type", "Cloth")
                        .param("type", "Staff")
                        .session(session))
                .andExpect(status().isOk());
        Calculations.loadModel(Calculations.getModel("Mage").getClass());
        assertThat(ItemFilter.getRelevantItemTypesList(Calculations.getInstance()))
                .containsExactly(ItemType.Cloth, ItemType.Staff);

        assertThat(FILTER_FILE).exists();
        ItemFilter.load(BASE_FILTER);
        assertThat(new SavedItemFilterStore(FILTER_FILE).load()).isTrue();
        Calculations.loadModel(Calculations.getModel("Mage").getClass());
        assertThat(ItemFilter.getRelevantItemTypesList(Calculations.getInstance()))
                .containsExactly(ItemType.Cloth, ItemType.Staff);
        assertThat(ItemFilter.getFilterList()).anyMatch(rule -> "Probe filter".equals(rule.getName()));

        mockMvc.perform(post("/character/item-filters/delete")
                        .param("path", Integer.toString(ItemFilter.getFilterList().size() - 1))
                        .session(session))
                .andExpect(status().isOk());
        assertThat(ItemFilter.getFilterList()).noneMatch(rule -> "Probe filter".equals(rule.getName()));
    }

    @Test
    void filterTreeGroupsRulesAndTogglesEnabledStateInline() throws Exception {
        MockHttpSession session = importFixture();
        String modal = html(mockMvc.perform(get("/character/item-filters").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal).contains("data-filter-search", "data-filter-status=\"all\"",
                "filter-branch", "Include filters", "Exclude filters", "data-path=\"0\"",
                "Search by name or pattern", "New filter", "New subfilter", "is-selected",
                "data-check-state=\"some\"", "data-own-enabled=", "data-filter-summary",
                "hx-swap=\"none\"");

        ItemFilterRegex first = ItemFilter.getFilterList().get(0);
        ItemFilterRegex child = first.regexList.get(0);
        String toggled = html(mockMvc.perform(post("/character/item-filters/enabled")
                        .param("path", "0")
                        .param("deep", "true")
                        .param("enabled", "false")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(first.isEnabled()).isFalse();
        assertThat(child.isEnabled()).isFalse();
        assertThat(toggled).isBlank();
        assertThat(FILTER_FILE).exists();

        String restored = html(mockMvc.perform(post("/character/item-filters/enabled")
                        .param("path", "0")
                        .param("deep", "true")
                        .param("enabled", "on")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(first.isEnabled()).isTrue();
        assertThat(child.isEnabled()).isTrue();
        assertThat(restored).isBlank();
    }

    @Test
    void enablingAChildFilterTurnsOnItsAncestorChain() throws Exception {
        MockHttpSession session = importFixture();
        mockMvc.perform(post("/character/item-filters/enabled")
                        .param("path", "2")
                        .param("deep", "true")
                        .param("enabled", "false")
                        .session(session))
                .andExpect(status().isOk());
        ItemFilterRegex tier7 = ItemFilter.getFilterList().get(2);
        assertThat(tier7.isEnabled()).isFalse();

        String response = html(mockMvc.perform(post("/character/item-filters/enabled")
                        .param("path", "2.0.0.0.0")
                        .param("enabled", "on")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(response).isBlank();
        ItemFilterRegex tenMan = tier7.regexList.get(0);
        ItemFilterRegex naxx = tenMan.regexList.get(0);
        ItemFilterRegex construct = naxx.regexList.get(0);
        ItemFilterRegex boss = construct.regexList.get(0);
        assertThat(tier7.isEnabled()).isTrue();
        assertThat(tenMan.isEnabled()).isTrue();
        assertThat(naxx.isEnabled()).isTrue();
        assertThat(construct.isEnabled()).isTrue();
        assertThat(boss.isEnabled()).isTrue();
    }

    @Test
    void savingAGroupKeepsItsOwnEnabledFlag() throws Exception {
        MockHttpSession session = importFixture();
        mockMvc.perform(post("/character/item-filters/enabled")
                        .param("path", "0")
                        .param("deep", "true")
                        .param("enabled", "false")
                        .session(session))
                .andExpect(status().isOk());
        ItemFilterRegex group = ItemFilter.getFilterList().get(0);
        assertThat(group.isEnabled()).isFalse();

        mockMvc.perform(post("/character/item-filters/node")
                        .param("path", "0")
                        .param("name", "Dungeons renamed")
                        .param("pattern", "")
                        .param("minItemLevel", "0")
                        .param("maxItemLevel", "1000")
                        .param("minItemQuality", "Temp")
                        .param("maxItemQuality", "Heirloom")
                        .param("additiveFilter", "on")
                        .param("appliesToItems", "on")
                        .param("appliesToGems", "on")
                        .param("otherRegexEnabled", "on")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(group.isEnabled()).isFalse();
    }

    @Test
    void filterEditsChangeTheComparisonCandidateSet() throws Exception {
        MockHttpSession session = importFixture();
        Character character = character(session);
        int before = comparisonService.gearChart(
                character, rawr.base.character.CharacterSlot.Head, 0).rows().size();
        assertThat(before).isPositive();

        mockMvc.perform(post("/character/item-filters/add").session(session))
                .andExpect(status().isOk());
        String path = Integer.toString(ItemFilter.getFilterList().size() - 1);
        mockMvc.perform(post("/character/item-filters/node")
                        .param("path", path)
                        .param("name", "Exclude all items")
                        .param("pattern", "")
                        .param("minItemLevel", "0")
                        .param("maxItemLevel", "1000")
                        .param("minItemQuality", "Temp")
                        .param("maxItemQuality", "Heirloom")
                        .param("boP", "on")
                        .param("boE", "on")
                        .param("boU", "on")
                        .param("boA", "on")
                        .param("boN", "on")
                        .param("appliesToItems", "on")
                        .param("enabled", "on")
                        .session(session))
                .andExpect(status().isOk());

        int after = comparisonService.gearChart(
                character, rawr.base.character.CharacterSlot.Head, 0).rows().size();
        assertThat(after).isLessThan(before);
    }

    @Test
    void childFilterEditorUsesTheTopLevelIncludeExcludeRole() throws Exception {
        MockHttpSession session = importFixture();
        String includeChild = html(mockMvc.perform(post("/character/item-filters/add")
                        .param("parentPath", "0")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(includeChild).contains("Include filter · child")
                .contains("This is a child filter")
                .doesNotContain("Additive filter");

        int excludeIndex = -1;
        for (int i = 0; i < ItemFilter.getFilterList().size(); i++) {
            if (!ItemFilter.getFilterList().get(i).isAdditiveFilter()) {
                excludeIndex = i;
                break;
            }
        }
        assertThat(excludeIndex).isGreaterThanOrEqualTo(0);
        String excludeChild = html(mockMvc.perform(post("/character/item-filters/add")
                        .param("parentPath", Integer.toString(excludeIndex))
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(excludeChild).contains("Exclude filter · child")
                .doesNotContain("Additive filter");
    }

    @Test
    void gemmingTemplateEditorAddsUpdatesTogglesAndPersists() throws Exception {
        MockHttpSession session = importFixture();
        String modal = html(mockMvc.perform(get("/character/gemming-templates").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal).contains("Gemming templates", "Add custom template", "Model: Mage");

        Character character = character(session);
        GemmingTemplate first = character.getCurrentGemmingTemplates().get(0);
        boolean original = first.getEnabled();
        mockMvc.perform(post("/character/gemming-templates/toggle")
                        .param("index", "0")
                        .param("custom", "false")
                        .param("enabled", original ? "false" : "true")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(character.getCurrentGemmingTemplates().get(0).getEnabled()).isNotEqualTo(original);

        mockMvc.perform(post("/character/gemming-templates/add").session(session))
                .andExpect(status().isOk());
        assertThat(character.getCustomGemmingTemplates()).hasSize(1);
        GemmingTemplate custom = character.getCustomGemmingTemplates().get(0);
        String red = Integer.toString(first.getRedId());
        String yellow = Integer.toString(first.getYellowId());
        String blue = Integer.toString(first.getBlueId());
        String meta = Integer.toString(first.getMetaId());
        String prismatic = Integer.toString(first.getPrismaticId());
        mockMvc.perform(post("/character/gemming-templates/update")
                        .param("index", "0")
                        .param("enabled", "on")
                        .param("redId", red)
                        .param("yellowId", yellow)
                        .param("blueId", blue)
                        .param("metaId", meta)
                        .param("prismaticId", prismatic)
                        .session(session))
                .andExpect(status().isOk());
        assertThat(custom.getRedId()).isEqualTo(Integer.parseInt(red));
        assertThat(custom.getEnabled()).isTrue();

        Path saved = REPO_ROOT.resolve("jrawr/rawr-web/build/tmp/b010-character.xml");
        character.save(saved);
        Character reloaded = rawr.data.CharacterFixtureLoader.load(saved);
        assertThat(reloaded.getCustomGemmingTemplates()).hasSize(1);
        assertThat(reloaded.getCustomGemmingTemplates().get(0).getRedId())
                .isEqualTo(Integer.parseInt(red));
        Files.deleteIfExists(saved);

        mockMvc.perform(post("/character/gemming-templates/delete")
                        .param("index", "0").session(session))
                .andExpect(status().isOk());
        assertThat(character.getCustomGemmingTemplates()).isEmpty();
    }

    @Test
    void customGemmingTemplateCanBeCreatedFromTheFooterForm() throws Exception {
        MockHttpSession session = importFixture();
        Character character = character(session);
        int before = character.getCustomGemmingTemplates().size();

        String created = html(mockMvc.perform(post("/character/gemming-templates/create")
                        .param("enabled", "on")
                        .param("redId", "0")
                        .param("yellowId", "0")
                        .param("blueId", "0")
                        .param("prismaticId", "0")
                        .param("metaId", "0")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(character.getCustomGemmingTemplates()).hasSize(before + 1);
        GemmingTemplate template = character.getCustomGemmingTemplates()
                .get(character.getCustomGemmingTemplates().size() - 1);
        assertThat(template.getModel()).isEqualTo("Mage");
        assertThat(template.getGroup()).isEqualTo("Custom");
        assertThat(template.getEnabled()).isTrue();
        assertThat(created).contains("Custom template", "New custom template",
                "data-gemming-create-form");
    }

    @Test
    void topGemmingsLimitsComparisonVariants() throws Exception {
        MockHttpSession session = importFixture();
        Character character = character(session);
        globalSettings.apply(new GlobalSettings(true, GlobalSettings.PROC_INTERPOLATION,
                GlobalSettings.COMBINATION_INTERPOLATED, true, 1, 1));

        ComparisonService.Chart chart = comparisonService.gearChart(
                character, rawr.base.character.CharacterSlot.Head, 0);
        Map<Integer, Long> perItem = chart.rows().stream()
                .filter(row -> !row.equipped())
                .collect(Collectors.groupingBy(ComparisonService.Row::itemId,
                        Collectors.counting()));
        assertThat(perItem.values()).allMatch(count -> count <= 1L);
    }

    @Test
    void gemmingTemplatesOwnTheChartVariantCount() throws Exception {
        MockHttpSession session = importFixture();
        String modal = html(mockMvc.perform(get("/character/gemming-templates").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(modal)
                .contains("Variants per item in charts", "name=\"topGemmings\"",
                        "closest .gemming-top-settings");

        MvcResult saved = mockMvc.perform(post("/character/gemming-templates/top-gemmings")
                        .param("topGemmings", "7"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(saved)).contains("Saved.");
        assertThat(saved.getResponse().getCookie(GlobalSettingsService.TOP_GEMMINGS_COOKIE).getValue())
                .isEqualTo("7");
        assertThat(globalSettings.current().topGemmings()).isEqualTo(7);

        MvcResult invalid = mockMvc.perform(post("/character/gemming-templates/top-gemmings")
                        .param("topGemmings", "0")
                        .cookie(saved.getResponse().getCookie(GlobalSettingsService.TOP_GEMMINGS_COOKIE)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(invalid)).contains("between 1 and");
        assertThat(globalSettings.current().topGemmings()).isEqualTo(7);
    }

    @Test
    void itemCostsPersistAndFeedCostRequirements() throws Exception {
        MockHttpSession session = importFixture();
        Character character = character(session);
        Item head = character.get(rawr.base.character.CharacterSlot.Head).getItem();
        assertThat(head).isNotNull();

        mockMvc.perform(post("/character/item-costs")
                        .param("itemId", Integer.toString(head.getId()))
                        .param("cost", "123.5")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(head.getCost()).isEqualTo(123.5f);
        assertThat(COST_FILE).exists();

        head.setAvailabilityInformation(new ItemAvailabilityInformation());
        head.getAvailabilityInformation().positiveCostItem = true;
        assertThat(OptimizerValues.getCalculationValue(character, null, "[Cost]"))
                .isEqualTo(123.5f);

        head.setCost(0f);
        assertThat(new rawr.data.ItemCostStore(COST_FILE).load()).isPositive();
        assertThat(ItemCache.findItemById(head.getId()).getCost()).isEqualTo(123.5f);

        String modal = html(mockMvc.perform(get("/character/item-costs").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal).contains("Item costs", "Search items by name", head.getName());

        mockMvc.perform(post("/character/item-costs/reset").session(session))
                .andExpect(status().isOk());
        assertThat(ItemCache.findItemById(head.getId()).getCost()).isZero();
    }

    private MockHttpSession importFixture() throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve(
                "oracle/fixtures/mage/arcane_277_raid.xml"));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "fixture.xml", "text/xml", xml))
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        return session;
    }

    private static Character character(HttpSession session) {
        return (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
