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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.items.ItemCache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline tests for the missing-item network importer: a character XML referencing items absent
 * from {@link ItemCache} is held back, the Import tab lists the IDs, and the fetch endpoint pulls
 * them from the configured item source (a loopback server here), caches/persists them and imports
 * the character. Failures keep the pending import so a retry only fetches what is still missing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MissingItemImportTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path ITEMS_FILE = createItemsFile();

    private static final int HEAD_ITEM = 99011;
    private static final int WEAPON_ITEM = 99012;
    private static final int PARTIAL_ITEM = 99021;
    private static final int FAILED_ITEM = 99022;
    private static final int GEM_ITEM = 99031;
    private static final int CACHED_HEAD = 51281;

    private static final Set<Integer> SERVED = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, AtomicInteger> FETCHES = new ConcurrentHashMap<>();
    private static final HttpServer SERVER;
    private static final int PORT;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/", exchange -> {
                int id = itemId(exchange.getRequestURI().getQuery());
                FETCHES.computeIfAbsent(id, key -> new AtomicInteger()).incrementAndGet();
                String body = SERVED.contains(id) ? payload(id) : null;
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.items-file", ITEMS_FILE::toString);
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
        FETCHES.clear();
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void holdsBackUnknownItemsThenFetchesPersistsAndImports() throws Exception {
        SERVED.add(HEAD_ITEM);
        SERVED.add(WEAPON_ITEM);
        String xml = characterXml("Missingmage",
                "<Head>" + HEAD_ITEM + ".0.0.0.0</Head>",
                "<MainHand>" + WEAPON_ITEM + ".0.0.0.0</MainHand>");
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "missing.xml", "text/xml",
                                xml.getBytes(StandardCharsets.UTF_8)))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=import&pending=1"));

        assertThat(ItemCache.findItemById(HEAD_ITEM)).isNull();
        assertThat(ItemCache.findItemById(WEAPON_ITEM)).isNull();
        assertThat(FETCHES).isEmpty();

        MvcResult pending = mockMvc.perform(get("/").param("open", "import")
                        .param("pending", "1").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String pendingHtml = html(pending);
        assertThat(pendingHtml)
                .contains("Missing items")
                .contains("pending-ids\">" + HEAD_ITEM + ", " + WEAPON_ITEM)
                .contains("Fetch and import")
                .contains("Import without fetching");

        mockMvc.perform(post("/character/fetch-missing")
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?fetched=2"));

        assertThat(ItemCache.findItemById(HEAD_ITEM)).isNotNull();
        assertThat(ItemCache.findItemById(WEAPON_ITEM)).isNotNull();
        assertThat(FETCHES.get(HEAD_ITEM).get()).isEqualTo(1);
        assertThat(FETCHES.get(WEAPON_ITEM).get()).isEqualTo(1);
        assertThat(Files.readString(ITEMS_FILE))
                .contains("<Id>" + HEAD_ITEM + "</Id>", "<Id>" + WEAPON_ITEM + "</Id>");

        String editor = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(editor).contains("Missingmage", "JRawr Fetched " + HEAD_ITEM,
                "JRawr Fetched " + WEAPON_ITEM);
    }

    @Test
    void partialFailureKeepsOnlyTheStillMissingIdsAndRetriesThem() throws Exception {
        SERVED.add(PARTIAL_ITEM);
        String xml = characterXml("Partialmage",
                "<Head>" + PARTIAL_ITEM + ".0.0.0.0</Head>",
                "<MainHand>" + FAILED_ITEM + ".0.0.0.0</MainHand>");
        MockHttpSession session = new MockHttpSession();

        importXml(session, xml);
        mockMvc.perform(post("/character/fetch-missing")
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=import&pending=1"));

        assertThat(ItemCache.findItemById(PARTIAL_ITEM)).isNotNull();
        assertThat(ItemCache.findItemById(FAILED_ITEM)).isNull();

        String pending = html(mockMvc.perform(get("/").param("open", "import")
                        .param("pending", "1").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(pending)
                .contains("Could not fetch 1 item: " + FAILED_ITEM)
                .contains("pending-ids\">" + FAILED_ITEM)
                .doesNotContain("pending-ids\">" + PARTIAL_ITEM);

        SERVED.add(FAILED_ITEM);
        mockMvc.perform(post("/character/fetch-missing")
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?fetched=1"));

        assertThat(ItemCache.findItemById(FAILED_ITEM)).isNotNull();
        assertThat(FETCHES.get(PARTIAL_ITEM).get()).isEqualTo(1);
        assertThat(FETCHES.get(FAILED_ITEM).get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void importsWithoutFetchingWhenTheUserChoosesThat() throws Exception {
        int item = 99041;
        SERVED.add(item);
        MockHttpSession session = new MockHttpSession();
        importXml(session, characterXml("Skipmage", "<Head>" + item + ".0.0.0.0</Head>"));

        mockMvc.perform(post("/character/import-pending").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?imported=1"));

        assertThat(ItemCache.findItemById(item)).isNull();
        assertThat(FETCHES).isEmpty();
        String editor = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(editor).contains("Skipmage")
                .doesNotContain("Calculation error", "Calculation failed", "getSocketColor");
    }

    @Test
    void cancellingThePendingImportLeavesTheCurrentCharacterUntouched() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/character/meta")
                        .param("name", "Original")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        int item = 99051;
        SERVED.add(item);
        importXml(session, characterXml("Discardedmage", "<Head>" + item + ".0.0.0.0</Head>"));

        mockMvc.perform(post("/character/cancel-pending").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=import"));

        assertThat(ItemCache.findItemById(item)).isNull();
        String editor = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(editor).contains("Original").doesNotContain("Discardedmage");
    }

    @Test
    void missingGemsAreFetchedAsItemsToo() throws Exception {
        assertThat(ItemCache.findItemById(CACHED_HEAD)).as("cached head item").isNotNull();
        assertThat(ItemCache.findItemById(GEM_ITEM)).isNull();
        SERVED.add(GEM_ITEM);

        MockHttpSession session = new MockHttpSession();
        importXml(session, characterXml("Gemmage",
                "<Head>" + CACHED_HEAD + "." + GEM_ITEM + ".0.0.0</Head>"));

        mockMvc.perform(post("/character/fetch-missing")
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?fetched=1"));

        assertThat(ItemCache.findItemById(GEM_ITEM)).isNotNull();
        assertThat(FETCHES.get(GEM_ITEM).get()).isEqualTo(1);
        assertThat(FETCHES.get(CACHED_HEAD)).isNull();
    }

    @Test
    void fetchWithoutAPendingImportRedirectsWithAnError() throws Exception {
        mockMvc.perform(post("/character/fetch-missing").session(new MockHttpSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=import&error=missing-character-xml"));
    }

    private void importXml(MockHttpSession session, String xml) throws Exception {
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "missing.xml", "text/xml",
                                xml.getBytes(StandardCharsets.UTF_8)))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=import&pending=1"));
    }

    private static String characterXml(String name, String... slots) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                .append("<Character>\n")
                .append("  <Name>").append(name).append("</Name>\n")
                .append("  <Realm>TestRealm</Realm>\n")
                .append("  <Region>US</Region>\n")
                .append("  <Race>Human</Race>\n")
                .append("  <Class>Mage</Class>\n")
                .append("  <CurrentModel>Mage</CurrentModel>\n")
                .append("  <EnforceMetagemRequirements>true</EnforceMetagemRequirements>\n");
        for (String slot : slots) {
            xml.append("  ").append(slot).append('\n');
        }
        xml.append("</Character>\n");
        return xml.toString();
    }

    private static String payload(int id) {
        String name = "JRawr Fetched " + id;
        if (id == GEM_ITEM) {
            return "<aowow><item id=\"" + id + "\">"
                    + "<name><![CDATA[" + name + "]]></name><level>80</level>"
                    + "<quality id=\"4\">Epic</quality>"
                    + "<class id=\"3\"><![CDATA[Gems]]></class>"
                    + "<subclass id=\"4\"><![CDATA[Purple Gems]]></subclass>"
                    + "<icon displayId=\"1\">inv_jewelcrafting_gem_01</icon>"
                    + "<inventorySlot id=\"0\"/>"
                    + "<htmlTooltip><![CDATA[<table><tr><td>Matches a red or blue socket.</td></tr></table>]]></htmlTooltip>"
                    + "<json><![CDATA[\"classs\":3,\"displayid\":1,\"id\":" + id
                    + ",\"level\":80,\"name\":\"" + name + "\",\"slot\":0,\"subclass\":4]]></json>"
                    + "<jsonEquip><![CDATA[\"int\":20]]></jsonEquip>"
                    + "</item></aowow>";
        }
        return "<wowhead><item id=\"" + id + "\">"
                + "<name><![CDATA[" + name + "]]></name><level>200</level>"
                + "<quality id=\"4\">Epic</quality>"
                + "<class id=\"4\"><![CDATA[Armor]]></class>"
                + "<subclass id=\"1\"><![CDATA[Cloth Armor]]></subclass>"
                + "<icon displayId=\"1\">inv_helmet_150black</icon>"
                + "<inventorySlot id=\"1\">Head</inventorySlot>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up"
                + "<br /><span class=\"q2\">Equip: Improves hit rating by 10.</span>"
                + "</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"displayid\":1,\"id\":" + id
                + ",\"level\":200,\"name\":\"" + name + "\",\"slot\":1,\"subclass\":1]]></json>"
                + "<jsonEquip><![CDATA[\"int\":100,\"sta\":100,\"splpwr\":200,\"armor\":1000,\"hitrtng\":10]]></jsonEquip>"
                + "</item></wowhead>";
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

    private static Path createItemsFile() {
        try {
            return Files.createTempDirectory("jrawr-items-missing-test-").resolve("jrawr.items.xml");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
