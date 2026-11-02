package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc test for the set-bonus fallback: with an empty lookup map (as if the generated
 * artifact had no entry for the set) the tooltip shows the bonus buff's name instead of prose.
 * The override bean replaces the classpath-loaded {@code set-bonus-text.json} only in this context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemTooltipSetBonusFallbackTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @TestConfiguration
    static class EmptySetBonusText {
        @Bean
        @Primary
        ItemTooltipService itemTooltipServiceWithoutSetBonusText(ItemIconService icons) {
            return new ItemTooltipService(icons, Map.of());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void setMissingFromArtifactFallsBackToBonusBuffName() throws Exception {
        MvcResult result = mockMvc.perform(get("/item-tooltip").param("id", "51281"))
                .andExpect(status().isOk())
                .andReturn();
        String body = HtmlUtils.htmlUnescape(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(body)
                .contains("Bloodmage's Regalia")
                .contains("(2) Set: Bloodmage's Regalia 2 Piece")
                .contains("(4) Set: Bloodmage's Regalia 4 Piece")
                .doesNotContain("Your Hot Streak")
                .doesNotContain("Your Mirror Image");
    }
}
