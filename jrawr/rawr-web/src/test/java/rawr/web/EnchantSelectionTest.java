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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for enchant selection: the picker lists relevant fitting enchants, the
 * paper doll exposes one pill per enchantable slot, and applying/removing an enchant updates both
 * the slot and the item tooltip.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnchantSelectionTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final int HEAD_ITEM = 51281;
    private static final int HEAD_ENCHANT = 3820;
    private static final int BACK_ITEM = 50628;
    private static final int BACK_ENCHANT = 3728;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.icon-base-url", () -> "https://icons.test");
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void pickerListsFittingRelevantEnchantsAndMarksTheEquippedOne() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult result = mockMvc.perform(get("/enchants").param("slot", "Head").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = html(result);

        assertThat(body)
                .contains("id=\"item-picker\"")
                .contains("No enchant")
                .contains("Arcanum of Burning Mysteries")
                .contains("30 Spell Power")
                .contains("20 Crit Rating")
                .contains("is-equipped")
                .doesNotContain("Darkglow Embroidery")
                .doesNotContain("<html");
    }

    @Test
    void searchingThePickerFiltersByName() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult result = mockMvc.perform(get("/enchants")
                        .param("slot", "Head")
                        .param("q", "burning")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = html(result);

        assertThat(body)
                .contains("id=\"enchant-results\"")
                .contains("Arcanum of Burning Mysteries")
                .doesNotContain("No enchant")
                .doesNotContain("<html");
    }

    @Test
    void applyingAndRemovingAnEnchantUpdatesThePaperDollAndTooltip() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult cleared = mockMvc.perform(post("/character/slot/enchant")
                        .param("slot", "Head")
                        .param("enchantId", "0")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(cleared))
                .contains("Add enchant")
                .doesNotContain("Arcanum of Burning Mysteries");

        MvcResult noEnchantTooltip = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(noEnchantTooltip))
                .doesNotContain("Arcanum of Burning Mysteries")
                .doesNotContain("tt-extras tt-enchant");

        MvcResult applied = mockMvc.perform(post("/character/slot/enchant")
                        .param("slot", "Head")
                        .param("enchantId", String.valueOf(HEAD_ENCHANT))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(applied))
                .contains("Arcanum of Burning Mysteries")
                .contains("slot-enchant-mark");

        MvcResult enchantedTooltip = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(enchantedTooltip))
                .contains("Arcanum of Burning Mysteries")
                .contains("tt-extras tt-enchant")
                .contains("30 Spell Power")
                .contains("20 Crit Rating");
    }

    @Test
    void tooltipShowsSpecialEffectDetailsOfProcEnchants() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult result = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(BACK_ITEM))
                        .param("slot", "Back")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = html(result);

        assertThat(body)
                .contains("Darkglow Embroidery")
                .contains("Mana Restored");
    }

    @Test
    void rejectsAnEnchantThatDoesNotFitTheItem() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        mockMvc.perform(post("/character/slot/enchant")
                        .param("slot", "Head")
                        .param("enchantId", String.valueOf(BACK_ENCHANT))
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession importFixture(String fixtureName) throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures/mage/" + fixtureName));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", fixtureName, "text/xml", xml))
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
