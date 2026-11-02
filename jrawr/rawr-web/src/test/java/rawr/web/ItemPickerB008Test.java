package rawr.web;

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
import rawr.base.items.Item;
import rawr.base.items.ItemCache;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B-008: model-aware item picker, ratings, relevance filters and sorting controls. */
@SpringBootTest
@AutoConfigureMockMvc
class ItemPickerB008Test {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final int MAGE_HEAD = 51281;
    private static final int WARLOCK_HEAD = 51208;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void defaultsToRelevantItemsWithRatingsAndControls() throws Exception {
        MockHttpSession session = importFixture();
        String html = picker(session, null, true, "overall");

        assertThat(html).contains(
                "id=\"item-picker\"",
                "name=\"relevantOnly\"",
                "name=\"sort\"",
                "gain vs equipped",
                "class=\"item-rating",
                "itemId=" + MAGE_HEAD,
                "item-tooltip");
        assertThat(html).doesNotContain("itemId=" + WARLOCK_HEAD);
    }

    @Test
    void allItemsStillHonorClassRestrictions() throws Exception {
        MockHttpSession session = importFixture();
        String html = picker(session, null, false, "overall");

        assertThat(html).contains("itemId=" + MAGE_HEAD);
        assertThat(html).doesNotContain("itemId=" + WARLOCK_HEAD);
    }

    @Test
    void searchFiltersByNameAndItemLevelSortIsAvailable() throws Exception {
        MockHttpSession session = importFixture();
        Item head = ItemCache.findItemById(MAGE_HEAD);
        assertThat(head).isNotNull();
        String needle = head.getName().substring(0, Math.min(8, head.getName().length()));

        MvcResult search = mockMvc.perform(get("/items")
                        .param("slot", "Head")
                        .param("q", needle)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String results = html(search);
        assertThat(results).contains("id=\"item-results\"", head.getName())
                .doesNotContain("<html");

        String sorted = picker(session, null, true, "itemLevel");
        assertThat(sorted).contains("value=\"overall\"", "value=\"name\"", "value=\"itemLevel\"");
    }

    private String picker(MockHttpSession session, String query, boolean relevantOnly, String sort)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/items")
                        .param("slot", "Head")
                        .param("relevantOnly", Boolean.toString(relevantOnly))
                        .param("sort", sort)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        return html(result);
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

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
