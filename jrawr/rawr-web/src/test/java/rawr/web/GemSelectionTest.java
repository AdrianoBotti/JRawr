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
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline MockMvc tests for gem sockets: socket pips on the paper doll, the meta/colored gem
 * picker, applying/clearing gems (including off-color gems), and the Blacksmithing extra socket.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GemSelectionTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final int HEAD_ITEM = 51281;
    private static final int META_GEM = 41285;
    private static final int BLUE_GEM = 40133;
    private static final int RED_GEM = 40113;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GemService gemService;

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
    void paperDollShowsFilledSocketsForTheEquippedItem() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult result = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = html(result);

        assertThat(body)
                .contains("class=\"slot-sockets\"")
                .contains("slot-socket-frame")
                .contains("socket-tip")
                .contains("pip-meta")
                .contains("pip-blue")
                .contains("Chaotic Skyflare Diamond")
                .contains("21 Crit Rating")
                .contains("Purified Dreadstone");
    }

    @Test
    void gemPickerSplitsMetaAndColoredSockets() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult meta = mockMvc.perform(get("/gems")
                        .param("slot", "Head")
                        .param("socket", "1")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(meta))
                .contains("Meta socket")
                .contains("Chaotic Skyflare Diamond")
                .contains("No gem")
                .doesNotContain("Purified Dreadstone")
                .doesNotContain("Runed Cardinal Ruby")
                .doesNotContain("<html");

        MvcResult blue = mockMvc.perform(get("/gems")
                        .param("slot", "Head")
                        .param("socket", "2")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(blue))
                .contains("Blue socket")
                .contains("Purified Dreadstone")
                .contains("Runed Cardinal Ruby")
                .contains("off-color")
                .doesNotContain("Chaotic Skyflare Diamond");
    }

    @Test
    void clearingAndApplyingAGemUpdatesThePaperDollAndTooltip() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult cleared = mockMvc.perform(post("/character/slot/gem")
                        .param("slot", "Head")
                        .param("socket", "1")
                        .param("gemId", "0")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(cleared))
                .contains("pip-meta is-empty")
                .doesNotContain("Chaotic Skyflare Diamond");

        MvcResult clearedTooltip = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(clearedTooltip))
                .contains("tt-socket-bonus is-inactive")
                .contains("Socket bonus: 9 Spell Power (inactive)");

        MvcResult applied = mockMvc.perform(post("/character/slot/gem")
                        .param("slot", "Head")
                        .param("socket", "1")
                        .param("gemId", String.valueOf(META_GEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(applied)).contains("Chaotic Skyflare Diamond");

        MvcResult tooltip = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(tooltip))
                .contains("Chaotic Skyflare Diamond")
                .contains("tt-socket-bonus is-active")
                .contains("Socket bonus: 9 Spell Power (active)");
    }

    @Test
    void metaAndColoredGemsStayInTheirSocketTypes() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        mockMvc.perform(post("/character/slot/gem")
                        .param("slot", "Head")
                        .param("socket", "1")
                        .param("gemId", String.valueOf(BLUE_GEM))
                        .session(session))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/character/slot/gem")
                        .param("slot", "Head")
                        .param("socket", "2")
                        .param("gemId", String.valueOf(META_GEM))
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void offColorGemIsAcceptedAndDisablesTheSocketBonus() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");

        MvcResult applied = mockMvc.perform(post("/character/slot/gem")
                        .param("slot", "Head")
                        .param("socket", "2")
                        .param("gemId", String.valueOf(RED_GEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(applied)).contains("Runed Cardinal Ruby");

        MvcResult tooltip = mockMvc.perform(get("/item-tooltip")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .param("slot", "Head")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(tooltip))
                .contains("Runed Cardinal Ruby")
                .contains("tt-socket-bonus is-inactive")
                .contains("Socket bonus: 9 Spell Power (inactive)");
    }

    @Test
    void blacksmithingAddsAPrismaticSocketToTheFirstEmptySocket() {
        Item belt = ItemCache.findItemById(50613);
        assertThat(belt).as("Crushing Coldwraith Belt").isNotNull();
        ItemInstance instance = new ItemInstance(belt, null, null, null, null);
        Character character = new Character();
        character.set(CharacterSlot.Waist, instance);

        List<GemService.SocketView> normal = GemService.sockets(character, CharacterSlot.Waist, instance);
        assertThat(normal).hasSize(2);
        assertThat(normal).extracting(GemService.SocketView::colorKey).containsExactly("blue", "red");

        character.setWaistBlacksmithingSocketEnabled(true);
        List<GemService.SocketView> extra = GemService.sockets(character, CharacterSlot.Waist, instance);
        assertThat(extra).hasSize(3);
        assertThat(extra.get(2).colorKey()).isEqualTo("prismatic");
        assertThat(extra.get(2).empty()).isTrue();
    }

    @Test
    void gemPickerSortsByQualityDescendingThenName() throws Exception {
        MockHttpSession session = importFixture("arcane_277_raid.xml");
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);

        List<GemService.GemChoice> ranked = gemService.choices(character, CharacterSlot.Head, 2, null, "quality")
                .stream().filter(choice -> !choice.none()).toList();
        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).qualityKey()).isEqualTo("epic");
        for (int i = 1; i < ranked.size(); i++) {
            assertThat(qualityValue(ranked.get(i).qualityKey()))
                    .isLessThanOrEqualTo(qualityValue(ranked.get(i - 1).qualityKey()));
        }

        List<GemService.GemChoice> byName = gemService.choices(character, CharacterSlot.Head, 2, null, "name")
                .stream().filter(choice -> !choice.none()).toList();
        assertThat(byName).isNotEmpty();
        for (int i = 1; i < byName.size(); i++) {
            assertThat(byName.get(i).name().toLowerCase(Locale.ROOT))
                    .isGreaterThanOrEqualTo(byName.get(i - 1).name().toLowerCase(Locale.ROOT));
        }
    }

    private static int qualityValue(String key) {
        if (key == null) {
            return -1;
        }
        return switch (key) {
            case "epic" -> 4;
            case "rare" -> 3;
            case "uncommon" -> 2;
            case "common" -> 1;
            default -> 0;
        };
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
