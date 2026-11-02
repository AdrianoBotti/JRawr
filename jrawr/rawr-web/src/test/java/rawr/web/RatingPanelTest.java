package rawr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rating/stat display: the rating bar shows the overall score and its composition (model metadata
 * labels), while the stats panel renders the model's display map grouped by its
 * {@code "Group:"} prefixes. Offline MockMvc runs against the committed oracle fixtures.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatingPanelTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void mageCenterShowsOnlyOverallWithCompositionAndSidebarShowsDisplayStats() throws Exception {
        String html = renderPanel("mage/arcane_277_raid.xml", "Mage");
        JsonNode golden = MAPPER.readTree(
                REPO_ROOT.resolve("oracle/golden/mage/arcane_277_raid.json").toFile());
        String expectedOverall = RatingFormat.format(
                (float) golden.get("ratings").get("OverallPoints").asDouble());
        String hitRate = golden.get("display").get("Hit Rate").asText();
        String expectedHitRate = hitRate.split("\\*")[0];

        assertThat(html).doesNotContain("data-state=\"error\"");

        // Identity lives in the Build card header; ratings live in the paper-doll tab bar.
        assertThat(html).contains("meta-form", "primaryProfession", "secondaryProfession",
                "identity-strip");

        String score = region(html, "id=\"paperdoll-rating\"", "<section class=\"tab-panel\"");
        assertThat(score).contains("quick-overall", expectedOverall);
        assertThat(countOccurrences(score, "class=\"quick-value\"")).isEqualTo(1);
        assertThat(countOccurrences(score, "class=\"pop-value\"")).isEqualTo(2); // no total line
        assertThat(score).contains("Dps", "Survivability", "is-negligible");
        assertThat(score).doesNotContain("= Overall");

        // Gear tab: only equipment and its summary; neither the identity form nor the ratings.
        String gear = region(html, "data-paperdoll-panel=\"gear\"", "<div id=\"item-picker\"");
        assertThat(gear).contains("data-gear-summary", "Average item level");
        assertThat(gear).doesNotContain("meta-form", "quick-value", "rating-composition");

        // Stats live in the middle of the Gear tab, with no overall rating or sub-point rows.
        String stats = region(html, "<section id=\"stats-panel\"", "<dialog id=\"stats-advanced-dialog\"");
        assertThat(stats).contains("Hit Rate", expectedHitRate);
        // Buff names may contain "Burst" (e.g. "Burst Mana Regeneration"), so match the labels only.
        assertThat(stats).as("stats panel has no overall/sub-point labels")
                .doesNotContain("Overall", ">Burst<", ">Sustained<");
        assertThat(stats).doesNotContain("quick-value", "rating-composition", "rating-hero");

        // Base stats stay in the card; advanced solver output lives in a wide dialog.
        String base = stats;
        String advanced = region(html, "<dialog id=\"stats-advanced-dialog\"", "</dialog>");
        assertThat(base).contains(">Base<", ">Stamina<", ">Hit Rate<", "data-open-advanced-stats");
        assertThat(base).doesNotContain(">Sequence<", ">AB2AM<", ">Dps<");
        assertThat(advanced).contains("Advanced stats", ">Sequence<", ">AB2AM<", ">By Spell<",
                "Cycle sequence reconstruction based on optimum cycles",
                "AB-AB-AM regardless of procs", "Threat per second");
        assertThat(advanced).doesNotContain(">Stamina<", ">Hit Rate<");
        // "Status" is a display key the Mage label array does not map: Advanced / Other.
        assertThat(advanced).contains(">Other<", ">Status<");
    }

    @Test
    void restoShamCompositionUsesItsOwnSubPointMetadata() throws Exception {
        String html = renderPanel("restosham/resto_264_raid.xml", "RestoSham");

        assertThat(html).doesNotContain("data-state=\"error\"");

        String score = region(html, "id=\"paperdoll-rating\"", "<section class=\"tab-panel\"");
        assertThat(countOccurrences(score, "class=\"quick-value\"")).isEqualTo(1);
        assertThat(score).contains("Burst", "Sustained", "Survival").doesNotContain("= Overall");

        String stats = region(html, "<section id=\"stats-panel\"", "<dialog id=\"stats-advanced-dialog\"");
        assertThat(stats).contains("Spell Power").doesNotContain("Overall");
        String base = stats;
        String advanced = region(html, "<dialog id=\"stats-advanced-dialog\"", "</dialog>");
        assertThat(base).contains(">Base<", ">Spell Power<", ">Stamina<");
        assertThat(advanced).contains("Advanced stats", ">RT+HW HPS<", ">Burst Sequence<");
        assertThat(advanced).doesNotContain(">Spell Power<");
    }

    @Test
    void modelsWithoutMetadataFallBackToGenericLabelsAndPaletteColors() {
        WebCalculationResult result = new WebCalculationResult(
                "Overall", 100f, new float[] { 2f, 0.5f }, Map.of(), Map.of());

        assertThat(result.subPointRows())
                .extracting(WebCalculationResult.SubPointRow::label)
                .containsExactly("Sub-point 1", "Sub-point 2");
        assertThat(result.subPointRows())
                .extracting(WebCalculationResult.SubPointRow::color)
                .containsExactly(RatingColors.DPS, RatingColors.BURST);
        assertThat(result.subPointRows())
                .extracting(WebCalculationResult.SubPointRow::negligible).containsExactly(false, true);

        WebCalculationResult zeroOverall = new WebCalculationResult(
                "Overall", 0f, new float[] { 0f, 0f }, Map.of(), Map.of());
        assertThat(zeroOverall.subPointRows())
                .extracting(WebCalculationResult.SubPointRow::negligible).containsExactly(false, false);
    }

    @Test
    void subRatingPaletteStaysReadableOnTheDarkPillBackground() {
        for (String color : RatingColors.PALETTE) {
            assertThat(contrastRatio(color, "#131922"))
                    .as("contrast of " + color)
                    .isGreaterThanOrEqualTo(4.5);
        }
        assertThat(RatingColors.forLabel("Survivability", 0)).isEqualTo(RatingColors.SURVIVAL);
        assertThat(RatingColors.forLabel("Burst DPS", 0)).isEqualTo(RatingColors.BURST);
        assertThat(RatingColors.forLabel("Sustained DPS", 1)).isEqualTo(RatingColors.DPS);
        assertThat(RatingColors.forLabel("Threat", 2)).isEqualTo(RatingColors.THREAT);
        assertThat(RatingColors.forLabel("Mitigation", 0)).isEqualTo(RatingColors.MITIGATION);
        assertThat(RatingColors.forLabel("MP5 Sources", 0)).isEqualTo(RatingColors.MANA);
    }

    @Test
    void displayLabelsOrderGroupsAndSplitValueLines() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Spell Stats:Hit Rate", "20.37%*377 Hit Rating\r\nArcane\t100.00%");
        values.put("Sequence", "abc");
        values.put("Basic Stats:Stamina", "1875");
        List<String> labels = List.of(
                "Basic Stats:Stamina",
                "Spell Stats:Hit Rate",
                "Solution:Sequence*Cycle sequence reconstruction based on optimum cycles");

        WebCalculationResult result = new WebCalculationResult(
                "Overall", 100f, new float[] { 80f, 20f },
                Map.of("Dps", "#0080FF", "Survivability", "#408020"), values, labels);

        // Label order wins over map order; prefixed display keys still match by their label part.
        assertThat(result.displayGroups())
                .extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly("Basic Stats", "Spell Stats", "Solution");

        WebCalculationResult.DisplayGroup basic = result.displayGroups().get(0);
        assertThat(basic.rows()).extracting(WebCalculationResult.DisplayRow::label)
                .containsExactly("Stamina");
        assertThat(basic.rows().get(0).primary()).isEqualTo("1875");
        assertThat(basic.advanced()).isFalse();

        WebCalculationResult.DisplayGroup spell = result.displayGroups().get(1);
        assertThat(spell.rows()).hasSize(1);
        assertThat(spell.rows().get(0).label()).isEqualTo("Hit Rate");
        assertThat(spell.rows().get(0).primary()).isEqualTo("20.37%");
        assertThat(spell.rows().get(0).secondary()).containsExactly("377 Hit Rating", "Arcane\t100.00%");
        assertThat(spell.advanced()).isFalse();

        WebCalculationResult.DisplayGroup solution = result.displayGroups().get(2);
        assertThat(solution.rows()).extracting(WebCalculationResult.DisplayRow::label).containsExactly("Sequence");
        assertThat(solution.rows().get(0).tooltip())
                .isEqualTo("Cycle sequence reconstruction based on optimum cycles");
        assertThat(solution.rows().get(0).primary()).isEqualTo("abc");
        assertThat(solution.rows().get(0).secondary()).isEmpty();
        assertThat(solution.advanced()).isTrue();

        assertThat(result.baseGroups()).extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly("Basic Stats", "Spell Stats");
        assertThat(result.advancedGroups()).extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly("Solution");
    }

    @Test
    void unmappedDisplayKeysGoLastInAdvancedOther() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Mystery", "42");
        values.put("Stamina", "1875");
        List<String> labels = List.of("Basic Stats:Stamina");

        WebCalculationResult result = new WebCalculationResult(
                "Overall", 100f, new float[] { 80f, 20f },
                Map.of(), values, labels);

        // Mapped entries first (label order), unmapped last under Other.
        assertThat(result.displayGroups())
                .extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly("Basic Stats", WebCalculationResult.DEFAULT_DISPLAY_GROUP);
        assertThat(result.baseGroups()).extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly("Basic Stats");
        assertThat(result.advancedGroups()).extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly(WebCalculationResult.DEFAULT_DISPLAY_GROUP);
        assertThat(result.advancedGroups().get(0).rows())
                .extracting(WebCalculationResult.DisplayRow::label).containsExactly("Mystery");

        // Without label metadata the whole map stays in map order under Advanced / Other.
        WebCalculationResult plain = new WebCalculationResult(
                "Overall", 1f, new float[] { 1f }, Map.of(), Map.of("Stamina", "10"));
        assertThat(plain.baseGroups()).isEmpty();
        assertThat(plain.displayGroups()).extracting(WebCalculationResult.DisplayGroup::name)
                .containsExactly(WebCalculationResult.DEFAULT_DISPLAY_GROUP);
        assertThat(plain.advancedGroups()).isNotEmpty();
    }

    private static String region(String html, String start, String end) {
        int from = html.indexOf(start);
        int to = html.indexOf(end, from);
        assertThat(from).as("region start %s", start).isGreaterThanOrEqualTo(0);
        assertThat(to).as("region end %s", end).isGreaterThan(from);
        return html.substring(from, to);
    }

    private static int countOccurrences(String html, String needle) {
        int count = 0;
        for (int i = html.indexOf(needle); i >= 0; i = html.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    private String renderPanel(String fixture, String model) throws Exception {
        Path file = REPO_ROOT.resolve("oracle/fixtures").resolve(fixture);
        byte[] xml = Files.readAllBytes(file);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", file.getFileName().toString(), "text/xml", xml))
                        .param("model", model)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return page.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static double contrastRatio(String foreground, String background) {
        double first = relativeLuminance(foreground);
        double second = relativeLuminance(background);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double relativeLuminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double[] channels = {
                channel((rgb >> 16) & 0xFF),
                channel((rgb >> 8) & 0xFF),
                channel(rgb & 0xFF)
        };
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
    }

    private static double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
