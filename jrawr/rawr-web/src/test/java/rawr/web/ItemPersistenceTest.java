package rawr.web;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemSlot;
import rawr.base.items.sources.StaticDrop;
import rawr.data.UserItemStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline persistence tests for Add-by-ID: items fetched from a loopback source are written to the
 * {@code jrawr.items-file} side-car, a fresh {@link DataBootstrap} over cleared static caches
 * reloads them (restart simulation) and a character equipping the reloaded item still calculates.
 * A corrupt side-car is ignored at startup instead of failing the bootstrap.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemPersistenceTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path DATA_DIR =
            REPO_ROOT.resolve("jrawr/data");
    private static final Path ITEMS_DIR = createTempDir("jrawr-items-persist-test-");
    private static final Path ITEMS_FILE = ITEMS_DIR.resolve("jrawr.items.xml");
    private static final Path CHARACTERS_DIR = createTempDir("jrawr-characters-persist-test-");
    private static final int HEAD_ITEM = 99501;
    private static final String ITEM_NAME = "JRawr Persistence Hood";
    private static final HttpServer SERVER;
    private static final int PORT;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/", exchange -> {
                if (itemId(exchange.getRequestURI().getQuery()) != HEAD_ITEM) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                byte[] bytes = payload(HEAD_ITEM, ITEM_NAME).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            SERVER.start();
            PORT = SERVER.getAddress().getPort();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", DATA_DIR::toString);
        registry.add("jrawr.items-file", ITEMS_FILE::toString);
        registry.add("jrawr.characters-dir", CHARACTERS_DIR::toString);
    }

    @BeforeEach
    void resetSideCar() throws IOException {
        dataBootstrap.load();
        Files.deleteIfExists(ITEMS_FILE);
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void itemsAddedByIdSurviveARestartAndStillCalculate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/character/meta").param("model", "Mage").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        MvcResult add = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(add)).contains(ITEM_NAME);
        assertThat(ItemCache.findItemById(HEAD_ITEM)).as("item in the live cache").isNotNull();

        assertThat(ITEMS_FILE).as("side-car written by Add-by-ID").exists();
        String written = Files.readString(ITEMS_FILE, StandardCharsets.UTF_8);
        assertThat(written).contains("<Name>" + ITEM_NAME + "</Name>")
                .contains("<ItemSource>")
                .contains("xsi:type=\"StaticDrop\"");

        // Restart: a fresh DataBootstrap over cleared static caches, as on a cold start.
        new DataBootstrap(DATA_DIR.toString(), new UserItemStore(ITEMS_FILE)).load();

        Item restored = ItemCache.findItemById(HEAD_ITEM);
        assertThat(restored).as("item survived the restart").isNotNull();
        assertThat(restored.getName()).isEqualTo(ITEM_NAME);
        assertThat(restored.getStats().getIntellect()).isEqualTo(100f);
        assertThat(restored.getStats().getSpellPower()).isEqualTo(200f);
        assertThat(restored.getSocketBonus().getSpellPower()).isEqualTo(9f);
        assertThat(restored.getSocketColor1()).isEqualTo(ItemSlot.Red);
        assertThat(restored.getSocketColor2()).isEqualTo(ItemSlot.Blue);
        assertThat(restored.getLocationInfo()[0]).isInstanceOf(StaticDrop.class);
        assertThat(((StaticDrop) restored.getLocationInfo()[0]).getBoss()).isEqualTo("JRawr Test Boss");

        // A character that uses the reloaded item still calculates.
        MockHttpSession restarted = new MockHttpSession();
        mockMvc.perform(post("/character/meta").param("model", "Mage").session(restarted))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(HEAD_ITEM))
                        .session(restarted))
                .andExpect(status().isOk());
        MvcResult page = mockMvc.perform(get("/").session(restarted))
                .andExpect(status().isOk())
                .andReturn();
        String pageHtml = html(page);
        assertThat(pageHtml).contains(ITEM_NAME)
                .contains("data-state=\"ok\"")
                .contains("quick-overall");
        assertThat(pageHtml).doesNotContain("data-state=\"error\"");
    }

    @Test
    void corruptItemFileIsIgnoredAtStartup() throws Exception {
        Files.writeString(ITEMS_FILE, "<ArrayOfItem><Item><Name>truncated");

        assertThatCode(() -> new DataBootstrap(DATA_DIR.toString(), new UserItemStore(ITEMS_FILE)).load())
                .as("a corrupt side-car must not fail startup")
                .doesNotThrowAnyException();

        assertThat(ItemCache.findItemById(HEAD_ITEM)).isNull();
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    private static Cookie templateCookie() {
        return new Cookie(ItemSourceService.TEMPLATE_COOKIE,
                "http://127.0.0.1:" + PORT + "/?item={id}&xml");
    }

    private static int itemId(String query) {
        if (query == null) {
            return -1;
        }
        for (String part : query.split("&")) {
            if (part.startsWith("item=")) {
                try {
                    return Integer.parseInt(part.substring("item=".length()));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String payload(int id, String name) {
        return "<aowow><item id=\"" + id + "\">"
                + "<name><![CDATA[" + name + "]]></name><level>277</level>"
                + "<quality id=\"4\">Epic</quality>"
                + "<class id=\"4\"><![CDATA[Armor]]></class>"
                + "<subclass id=\"1\"><![CDATA[Cloth Armor]]></subclass>"
                + "<icon displayId=\"64897\">inv_helmet_150black</icon>"
                + "<inventorySlot id=\"1\">Head</inventorySlot>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up"
                + "<br /><span class=\"socket-red\"></span><span class=\"socket-blue\"></span>"
                + "<br />Socket Bonus: +9 Spell Power"
                + "<br /><span class=\"q2\">Equip: Each time you deal spell damage to an opponent,"
                + " you gain 20 spell power for the next 10 sec, stacking up to 10 times.</span>"
                + "</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"displayid\":64897,\"id\":" + id
                + ",\"level\":277,\"name\":\"" + name + "\",\"slot\":1,\"subclass\":1"
                + ",\"source\":[1],\"sourcemore\":[{\"n\":\"JRawr Test Boss\",\"t\":1,\"z\":\"4812\"}]]]></json>"
                + "<jsonEquip><![CDATA[\"int\":100,\"sta\":100,\"splpwr\":200,\"armor\":1000]]></jsonEquip>"
                + "</item></aowow>";
    }

    private static Path createTempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
