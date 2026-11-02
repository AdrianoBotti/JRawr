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
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.data.UserItemStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Offline Item editor tests: a loopback item source serves recorded-shape payloads so parse,
 * live fetch, undo, refresh, list/detail, remove and picker integration all run without the real
 * internet. The source rejects unknown ids; every fetched item lives in the temp user side-car.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemEditorControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path ITEMS_FILE = createItemsFile();
    private static final int HEAD_ITEM = 99001;
    private static final int WAIST_ITEM = 99002;
    private static final int TRINKET_ITEM = 99003;
    private static final int REFRESH_ITEM = 99004;
    private static final int CACHED_ITEM = 99005;
    private static final int MISSING_ITEM = 99888;
    private static final int BROKEN_ITEM = 99889;
    private static final int PLATE_ITEM = 99006;
    private static final int CUSTOM_SOURCE = 40432;
    private static final int CUSTOM_ID = ItemEditorService.CUSTOM_ITEM_ID_MIN;
    private static final AtomicInteger FETCHES = new AtomicInteger();
    private static final AtomicInteger REFRESH_FETCHES = new AtomicInteger();
    private static final HttpServer SERVER;
    private static final int PORT;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/", exchange -> {
                int id = itemId(exchange.getRequestURI().getQuery());
                String body = null;
                if (id == HEAD_ITEM) {
                    body = payload(HEAD_ITEM, "JRawr Test Hood", 1, "Head");
                } else if (id == WAIST_ITEM) {
                    body = payload(WAIST_ITEM, "JRawr Test Belt", 6, "Waist");
                } else if (id == TRINKET_ITEM) {
                    body = procTrinketPayload(TRINKET_ITEM, "JRawr Test Proc Trinket");
                } else if (id == REFRESH_ITEM) {
                    int attempt = REFRESH_FETCHES.incrementAndGet();
                    body = payload(REFRESH_ITEM,
                            attempt == 1 ? "JRawr Test Hood" : "JRawr Test Hood v2", 1, "Head");
                } else if (id == CACHED_ITEM) {
                    body = payload(CACHED_ITEM, "JRawr Test Cached Hood", 1, "Head");
                } else if (id == MISSING_ITEM) {
                    body = "<aowow><error>Item not found!</error></aowow>";
                } else if (id == BROKEN_ITEM) {
                    body = "<aowow><item id=\"99889\"><name>Broken";
                } else if (id == PLATE_ITEM) {
                    body = platePayload(PLATE_ITEM, "JRawr Test Plate Helm");
                }
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                FETCHES.incrementAndGet();
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

    @Autowired
    private UserItemStore userItemStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.items-file", ITEMS_FILE::toString);
    }

    @BeforeEach
    void clearUserItems() {
        dataBootstrap.load();
        for (Item item : userItemStore.items()) {
            ItemCache.deleteItem(item);
            userItemStore.remove(item.getId());
        }
        FETCHES.set(0);
        REFRESH_FETCHES.set(0);
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void fetchParsesCachesAndEquipsThroughTheNormalFlow() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult add = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger",
                        "{\"jrawr:item-cache-changed\":{\"id\":" + HEAD_ITEM + "}}"))
                .andReturn();
        String html = html(add);
        assertThat(html).contains("JRawr Test Hood").contains("Added").contains("data-status=\"added\"");
        assertThat(FETCHES.get()).isEqualTo(1);

        Item cached = ItemCache.findItemById(HEAD_ITEM);
        assertThat(cached).as("item added to the cache").isNotNull();
        assertThat(cached.getStats().getIntellect()).isEqualTo(100f);
        assertThat(cached.getStats().getSpellPower()).isEqualTo(200f);
        assertThat(cached.getStats().getStamina()).isEqualTo(100f);

        MvcResult equip = mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(HEAD_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(equip)).contains("JRawr Test Hood");
    }

    @Test
    void fetchProcTrinketWithEffectsAndEquipsIt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult add = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(TRINKET_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(add))
                .contains("JRawr Test Proc Trinket")
                .contains("Use: ")
                .contains("1215 Attack Power")
                .contains("Equip: ")
                .contains("on Damaging Spell Hit");

        Item cached = ItemCache.findItemById(TRINKET_ITEM);
        assertThat(cached).as("item added to the cache").isNotNull();
        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : cached.getStats().specialEffects()) {
            effects.add(effect);
        }
        assertThat(effects).hasSize(2);
        assertThat(effects.get(0).getTrigger()).isEqualTo(Trigger.Use);
        assertThat(effects.get(0).getStats().getAttackPower()).isEqualTo(1215f);
        assertThat(effects.get(0).getDuration()).isEqualTo(20f);
        assertThat(effects.get(0).getCooldown()).isEqualTo(120f);
        assertThat(effects.get(1).getTrigger()).isEqualTo(Trigger.DamageSpellHit);
        assertThat(effects.get(1).getStats().getSpellPower()).isEqualTo(20f);
        assertThat(effects.get(1).getMaxStack()).isEqualTo(10);

        MvcResult equip = mockMvc.perform(post("/character/slot")
                        .param("slot", "Trinket1")
                        .param("itemId", String.valueOf(TRINKET_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(equip)).contains("JRawr Test Proc Trinket");
    }

    @Test
    void fetchedItemShowsModelRelevanceFeedback() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/character/meta").param("model", "Mage").session(session))
                .andExpect(status().is3xxRedirection());
        MvcResult add = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(PLATE_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(add)).contains("Not relevant to Mage");
    }

    @Test
    void customFormShowsTheSourceAndAllocatesAReservedId() throws Exception {
        MvcResult form = mockMvc.perform(get("/tools/item-editor/custom")
                        .param("source", String.valueOf(CUSTOM_SOURCE)))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(form);
        assertThat(html)
                .contains("Create custom item")
                .contains("Illustration of the Dragon Soul")
                .contains("Save custom item")
                .contains("Add stat")
                .contains("Custom copy")
                .contains("name=\"itemId\" value=\"" + CUSTOM_ID + "\"")
                .contains("effect0Trigger", "effect0StatName")
                .contains("on Spell Cast");
    }

    @Test
    void savesACustomCopyWithEffectsStatsSocketsAndRequiredClasses() throws Exception {
        MvcResult saved = mockMvc.perform(customSaveRequest(null, CUSTOM_ID, "Custom Test Hood"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(saved))
                .contains("Saved custom item.").contains("Custom Test Hood").contains("Custom item");

        Item custom = ItemCache.findItemById(CUSTOM_ID);
        assertThat(custom).as("custom item in the cache").isNotNull();
        assertThat(custom.getQuality()).isEqualTo(ItemQuality.Epic);
        assertThat(custom.getSlot()).isEqualTo(ItemSlot.Head);
        assertThat(custom.getName()).isEqualTo("Custom Test Hood");
        assertThat(custom.getStats().getIntellect()).isEqualTo(100f);
        assertThat(custom.getStats().getStamina()).isEqualTo(50f);
        assertThat(custom.getSocketBonus().getSpellPower()).isEqualTo(9f);
        assertThat(custom.getSocketColor1()).isEqualTo(ItemSlot.Red);
        assertThat(custom.getSocketColor2()).isEqualTo(ItemSlot.Blue);
        assertThat(custom.getRequiredClasses()).isEqualTo("Mage");
        assertThat(userItemStore.contains(CUSTOM_ID)).isTrue();
        assertThat(ItemCache.findItemById(CUSTOM_SOURCE).getName())
                .isEqualTo("Illustration of the Dragon Soul");

        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : custom.getStats().specialEffects()) {
            effects.add(effect);
        }
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getTrigger()).isEqualTo(Trigger.Use);
        assertThat(effects.get(0).getStats().getSpellPower()).isEqualTo(150f);
        assertThat(effects.get(0).getDuration()).isEqualTo(20f);
        assertThat(effects.get(0).getCooldown()).isEqualTo(120f);
    }

    @Test
    void previewBuildsTheDraftWithoutCachingIt() throws Exception {
        int previewId = CUSTOM_ID + 1;
        MvcResult preview = mockMvc.perform(customFormRequest("/tools/item-editor/custom/preview",
                        previewId, "Preview Hood"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(preview)).contains("Preview Hood").contains("100 Intellect");
        assertThat(ItemCache.findItemById(previewId)).isNull();
    }

    @Test
    void editingACustomCopyUpdatesItInPlace() throws Exception {
        mockMvc.perform(customSaveRequest(null, CUSTOM_ID, "Custom Test Hood"))
                .andExpect(status().isOk());

        MvcResult form = mockMvc.perform(get("/tools/item-editor/custom")
                        .param("id", String.valueOf(CUSTOM_ID)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(form)).contains("Edit custom item").contains("Custom Test Hood");

        MvcResult edited = mockMvc.perform(customSaveRequest(String.valueOf(CUSTOM_ID),
                        CUSTOM_ID, "Custom Test Hood v2"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(edited)).contains("Saved custom item.").contains("Custom Test Hood v2");
        assertThat(ItemCache.findItemById(CUSTOM_ID).getName()).isEqualTo("Custom Test Hood v2");
        assertThat(ItemCache.findItemById(CUSTOM_SOURCE).getName())
                .isEqualTo("Illustration of the Dragon Soul");
    }

    @Test
    void editingAnEquippedCustomCopyUpdatesThePaperDoll() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(customSaveRequest(session, null, CUSTOM_ID, "Custom Test Hood"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(CUSTOM_ID))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(customSaveRequest(session, String.valueOf(CUSTOM_ID),
                        CUSTOM_ID, "Custom Test Hood v2"))
                .andExpect(status().isOk());
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("Custom Test Hood v2");
    }

    @Test
    void customIdOverrideMustBeFreeAndReserved() throws Exception {
        mockMvc.perform(customSaveRequest(null, CUSTOM_ID, "Custom Test Hood"))
                .andExpect(status().isOk());

        MvcResult collision = mockMvc.perform(customSaveRequest(null, CUSTOM_ID, "Collision Hood")
                        .param("idOverride", String.valueOf(CUSTOM_ID)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(collision)).contains("already in use");

        MvcResult tooLow = mockMvc.perform(customSaveRequest(null, CUSTOM_ID + 1, "Low Id Hood")
                        .param("idOverride", "123"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(tooLow)).contains("1,000,000 or higher");
        assertThat(ItemCache.findItemById(123)).isNull();
    }

    @Test
    void onlyCustomCopiesCanBeEditedDirectly() throws Exception {
        MvcResult form = mockMvc.perform(get("/tools/item-editor/custom")
                        .param("id", String.valueOf(CUSTOM_SOURCE)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(form)).contains("Only custom copies can be edited.");
    }

    private MockHttpServletRequestBuilder customSaveRequest(String editId, int itemId, String name) {
        return customSaveRequest(null, editId, itemId, name);
    }

    private MockHttpServletRequestBuilder customSaveRequest(MockHttpSession session, String editId,
            int itemId, String name) {
        MockHttpServletRequestBuilder request = customFormRequest("/tools/item-editor/custom/save",
                itemId, name);
        if (editId != null) {
            request.param("editId", editId);
        }
        if (session != null) {
            request.session(session);
        }
        return request;
    }

    private MockHttpServletRequestBuilder customFormRequest(String path, int itemId, String name) {
        return customFormRequest(path, itemId, name, true);
    }

    private MockHttpServletRequestBuilder customFormRequest(String path, int itemId, String name,
            boolean withEffect) {
        MockHttpServletRequestBuilder request = post(path)
                .param("sourceId", String.valueOf(CUSTOM_SOURCE))
                .param("itemId", String.valueOf(itemId))
                .param("name", name)
                .param("itemLevel", "200")
                .param("quality", "Epic")
                .param("slot", "Head")
                .param("type", "Cloth")
                .param("faction", "Neutral")
                .param("bind", "BoP")
                .param("setName", "")
                .param("iconPath", "inv_helmet_150black")
                .param("unique", "true")
                .param("minDamage", "0")
                .param("maxDamage", "0")
                .param("speed", "0")
                .param("damageType", "Physical")
                .param("socket1", "Red")
                .param("socket2", "Blue")
                .param("socket3", "None")
                .param("statName", "Intellect", "Stamina")
                .param("statValue", "100", "50")
                .param("bonusName", "SpellPower")
                .param("bonusValue", "9")
                .param("requiredClass", "Mage");
        if (withEffect) {
            request.param("effect0Trigger", "Use")
                    .param("effect0Duration", "20")
                    .param("effect0Cooldown", "120")
                    .param("effect0Chance", "100")
                    .param("effect0Mode", "percent")
                    .param("effect0Stacks", "1")
                    .param("effect0StatName", "SpellPower")
                    .param("effect0StatValue", "150");
        }
        return request;
    }

    @Test
    void customEffectsCanBeEditedAndRemoved() throws Exception {
        mockMvc.perform(customSaveRequest(null, CUSTOM_ID, "Effect Hood"))
                .andExpect(status().isOk());

        MvcResult edited = mockMvc.perform(customFormRequest("/tools/item-editor/custom/save",
                        CUSTOM_ID, "Effect Hood", false)
                        .param("editId", String.valueOf(CUSTOM_ID))
                        .param("effect0Trigger", "SpellHit")
                        .param("effect0Duration", "10")
                        .param("effect0Cooldown", "0")
                        .param("effect0Chance", "2")
                        .param("effect0Mode", "ppm")
                        .param("effect0Stacks", "3")
                        .param("effect0StatName", "Intellect")
                        .param("effect0StatValue", "15"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(edited)).contains("Saved custom item.");
        List<SpecialEffect> effects = effectsOf(CUSTOM_ID);
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getTrigger()).isEqualTo(Trigger.SpellHit);
        assertThat(effects.get(0).getDuration()).isEqualTo(10f);
        assertThat(effects.get(0).getCooldown()).isEqualTo(0f);
        assertThat(effects.get(0).getChance()).isEqualTo(-2f);
        assertThat(effects.get(0).getMaxStack()).isEqualTo(3);
        assertThat(effects.get(0).getStats().getIntellect()).isEqualTo(15f);

        MvcResult removed = mockMvc.perform(customFormRequest("/tools/item-editor/custom/save",
                        CUSTOM_ID, "Effect Hood", false)
                        .param("editId", String.valueOf(CUSTOM_ID)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(removed)).contains("Saved custom item.");
        assertThat(effectsOf(CUSTOM_ID)).isEmpty();
    }

    private static List<SpecialEffect> effectsOf(int itemId) {
        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        Item item = ItemCache.findItemById(itemId);
        if (item == null || item.getStats() == null) {
            return effects;
        }
        for (SpecialEffect effect : item.getStats().specialEffects()) {
            effects.add(effect);
        }
        return effects;
    }

    @Test
    void customValidationMarksFieldsAndKeepsSubmittedValues() throws Exception {
        MvcResult result = mockMvc.perform(post("/tools/item-editor/custom/save")
                        .param("sourceId", String.valueOf(CUSTOM_SOURCE))
                        .param("itemId", String.valueOf(CUSTOM_ID))
                        .param("name", "")
                        .param("itemLevel", "x")
                        .param("quality", "Epic")
                        .param("slot", "Head")
                        .param("type", "Cloth")
                        .param("faction", "Neutral")
                        .param("bind", "BoP")
                        .param("iconPath", "bad icon")
                        .param("unique", "true")
                        .param("minDamage", "0")
                        .param("maxDamage", "0")
                        .param("speed", "0")
                        .param("damageType", "Physical")
                        .param("socket1", "None")
                        .param("socket2", "None")
                        .param("socket3", "None"))
                .andExpect(status().isOk())
                .andReturn();
        String html = html(result);
        assertThat(html)
                .contains("data-first-error=\"name\"")
                .contains("aria-invalid=\"true\"")
                .contains("Enter an item name.")
                .contains("value=\"x\"")
                .contains("Use an icon path without spaces.")
                .contains("hx-disabled-elt=\"this\"");
    }

    @Test
    void customEffectStatErrorsPointAtTheRow() throws Exception {
        MvcResult result = mockMvc.perform(customFormRequest("/tools/item-editor/custom/save",
                        CUSTOM_ID, "Bad Effect", false)
                        .param("effect0Trigger", "Use")
                        .param("effect0Duration", "20")
                        .param("effect0Cooldown", "120")
                        .param("effect0Chance", "100")
                        .param("effect0Mode", "percent")
                        .param("effect0Stacks", "1")
                        .param("effect0StatName", "NotAStat")
                        .param("effect0StatValue", "5"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result))
                .contains("Unknown effect stat: NotAStat.")
                .contains("data-field-key=\"effect.0.stat.0\"")
                .contains("is-invalid");
    }

    @Test
    void customFormKeepsListFiltersAndSelectsTheSavedItem() throws Exception {
        MvcResult form = mockMvc.perform(get("/tools/item-editor/custom")
                        .param("source", String.valueOf(CUSTOM_SOURCE))
                        .param("q", "Blood")
                        .param("origin", "custom")
                        .param("slot", "Head"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(form))
                .contains("name=\"filterQuery\" value=\"Blood\"")
                .contains("name=\"filterOrigin\" value=\"custom\"")
                .contains("name=\"filterSlot\" value=\"Head\"")
                .contains("q=Blood", "origin=custom", "slot=Head");

        MvcResult saved = mockMvc.perform(customFormRequest("/tools/item-editor/custom/save",
                        CUSTOM_ID, "JRawr Custom Test")
                        .param("filterQuery", "JRawr")
                        .param("filterOrigin", "custom")
                        .param("filterSlot", "Head"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(saved))
                .contains("JRawr Custom Test")
                .contains("is-selected")
                .contains("value=\"JRawr\"");
    }

    @Test
    void parseAcceptsIdsLinksAndUrlsAndReportsInvalidTokens() throws Exception {
        String input = String.valueOf(HEAD_ITEM) + "\n"
                + "|Hitem:" + WAIST_ITEM + ":0:0:0:0:0:0:0:0|h[JRawr Test Belt]|h\n"
                + "https://www.wowhead.com/wotlk/item=" + CACHED_ITEM + "\n"
                + "not-an-id";
        MvcResult result = mockMvc.perform(post("/tools/item-editor/parse")
                        .param("items", input)
                        .param("skipCached", "true")
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String html = html(result);
        assertThat(html)
                .contains("data-item-id=\"" + HEAD_ITEM + "\"")
                .contains("data-item-id=\"" + WAIST_ITEM + "\"")
                .contains("data-item-id=\"" + CACHED_ITEM + "\"")
                .contains("data-fetch=\"true\"")
                .contains("Not an item ID")
                .contains("data-status=\"invalid\"");
        assertThat(FETCHES.get()).as("parse does not contact the source").isZero();
    }

    @Test
    void fetchCachedItemDoesNotContactTheSource() throws Exception {
        MvcResult first = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(CACHED_ITEM))
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(first)).contains("JRawr Test Cached Hood").contains("Added");
        assertThat(FETCHES.get()).isEqualTo(1);

        MvcResult second = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(CACHED_ITEM))
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(second)).contains("Already cached").contains("data-status=\"cached\"");
        assertThat(FETCHES.get()).as("cached items are not fetched again").isEqualTo(1);
    }

    @Test
    void refreshFetchedItemUpdatesTheCacheAndUndoRestores() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(REFRESH_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk());
        assertThat(ItemCache.findItemById(REFRESH_ITEM).getName()).isEqualTo("JRawr Test Hood");
        assertThat(FETCHES.get()).isEqualTo(1);

        MvcResult refreshed = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(REFRESH_ITEM))
                        .param("refresh", "true")
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(refreshed)).contains("JRawr Test Hood v2").contains("Updated")
                .contains("data-status=\"refreshed\"");
        assertThat(ItemCache.findItemById(REFRESH_ITEM).getName()).isEqualTo("JRawr Test Hood v2");
        assertThat(FETCHES.get()).isEqualTo(2);

        MvcResult undone = mockMvc.perform(post("/tools/item-editor/undo")
                        .param("id", String.valueOf(REFRESH_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(undone)).contains("Restored the previous local copy.");
        assertThat(ItemCache.findItemById(REFRESH_ITEM).getName()).isEqualTo("JRawr Test Hood");
        assertThat(userItemStore.contains(REFRESH_ITEM)).isTrue();
    }

    @Test
    void undoRemovesANewlyAddedItem() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(WAIST_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk());
        assertThat(ItemCache.findItemById(WAIST_ITEM)).isNotNull();

        MvcResult undone = mockMvc.perform(post("/tools/item-editor/undo")
                        .param("id", String.valueOf(WAIST_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(undone)).contains("Removed from the local cache.");
        assertThat(ItemCache.findItemById(WAIST_ITEM)).isNull();
        assertThat(userItemStore.contains(WAIST_ITEM)).isFalse();
        assertThat(Files.readString(ITEMS_FILE, StandardCharsets.UTF_8))
                .doesNotContain("JRawr Test Belt");
    }

    @Test
    void missingAndBrokenPayloadsShowFriendlyErrors() throws Exception {
        MvcResult missing = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(MISSING_ITEM))
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(missing)).contains("did not return an item").contains("Not found");
        assertThat(ItemCache.findItemById(MISSING_ITEM)).isNull();

        MvcResult broken = mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(BROKEN_ITEM))
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(broken)).contains("could not be read").contains("Error");
        assertThat(ItemCache.findItemById(BROKEN_ITEM)).isNull();
    }

    @Test
    void removeRefusesEquippedItemsAndRemovesUnequippedUserItems() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(HEAD_ITEM))
                        .session(session))
                .andExpect(status().isOk());

        MvcResult refused = mockMvc.perform(post("/tools/item-editor/remove")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(refused)).contains("Equip or unsocket the item before removing it.");
        assertThat(ItemCache.findItemById(HEAD_ITEM)).isNotNull();

        MvcResult equippedDetail = mockMvc.perform(get("/tools/item-editor/detail")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(equippedDetail))
                .contains("This item is equipped or socketed on the current character")
                .contains("Unequip or unsocket the item first")
                .contains("Remove from cache")
                .contains("disabled");

        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(WAIST_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk());
        MvcResult removed = mockMvc.perform(post("/tools/item-editor/remove")
                        .param("id", String.valueOf(WAIST_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(removed)).contains("Removed JRawr Test Belt from the local cache.");
        assertThat(ItemCache.findItemById(WAIST_ITEM)).isNull();
        assertThat(userItemStore.contains(WAIST_ITEM)).isFalse();

        int baseItem = firstBaseItemId();
        MvcResult baseRefused = mockMvc.perform(post("/tools/item-editor/remove")
                        .param("id", String.valueOf(baseItem))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(baseRefused)).contains("Only items added by you can be removed.");
        assertThat(ItemCache.findItemById(baseItem)).isNotNull();
    }

    @Test
    void editorShowsTheSourceListAndDetail() throws Exception {
        MvcResult editor = mockMvc.perform(get("/tools/item-editor")
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        String editorHtml = html(editor);
        assertThat(editorHtml)
                .contains("Item editor")
                .contains("item-editor-backdrop")
                .contains("Add items")
                .contains("Edit items")
                .contains("Item source")
                .contains("Wowhead (WotLK)")
                .contains("wotlkdb (AoWoW)")
                .contains("EvoWow")
                .contains("Custom URL")
                .contains("custom source")
                .contains("127.0.0.1");

        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .cookie(templateCookie()))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/tools/item-editor/list")
                        .param("q", "JRawr"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(list)).contains("JRawr Test Hood").contains("Fetched");

        MvcResult detail = mockMvc.perform(get("/tools/item-editor/detail")
                        .param("id", String.valueOf(HEAD_ITEM)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(detail))
                .contains("JRawr Test Hood")
                .contains("Refresh from source")
                .contains("Remove from cache")
                .contains("Fetched");

        int baseItem = firstBaseItemId();
        MvcResult baseDetail = mockMvc.perform(get("/tools/item-editor/detail")
                        .param("id", String.valueOf(baseItem)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(baseDetail)).contains("Base").doesNotContain("Remove from cache");
    }

    @Test
    void pickerShowsTheAddedItem() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/tools/item-editor/fetch")
                        .param("id", String.valueOf(HEAD_ITEM))
                        .cookie(templateCookie())
                        .session(session))
                .andExpect(status().isOk());

        MvcResult picker = mockMvc.perform(get("/items")
                        .param("slot", "Head")
                        .param("added", String.valueOf(HEAD_ITEM))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(picker)).contains("JRawr Test Hood").contains("is-added")
                .contains("/tools/item-editor").contains("Add item");
    }

    @Test
    void emptyInputShowsAMessage() throws Exception {
        MvcResult result = mockMvc.perform(post("/tools/item-editor/parse")
                        .param("items", "")
                        .cookie(templateCookie()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(result)).contains("Paste at least one item ID");
    }

    private int firstBaseItemId() {
        for (Item item : ItemCache.allItems()) {
            if (item.getName() != null && !item.getName().isBlank() && item.getSlot() != null
                    && !item.isGem() && !userItemStore.contains(item.getId())) {
                return item.getId();
            }
        }
        throw new IllegalStateException("No base items loaded");
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

    private static String procTrinketPayload(int id, String name) {
        return "<aowow><item id=\"" + id + "\">"
                + "<name><![CDATA[" + name + "]]></name><level>277</level>"
                + "<quality id=\"4\">Epic</quality>"
                + "<class id=\"4\"><![CDATA[Armor]]></class>"
                + "<subclass id=\"0\"><![CDATA[Miscellaneous]]></subclass>"
                + "<icon displayId=\"1\">inv_misc_orb_03</icon>"
                + "<inventorySlot id=\"12\">Trinket</inventorySlot>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up"
                + "<br /><span class=\"q2\">Equip: Each time you deal spell damage to an opponent,"
                + " you gain 20 spell power for the next 10 sec, stacking up to 10 times.</span>"
                + "<br /><span class=\"q2\">Use: Increases attack power by 1215 for 20 sec."
                + " (2 Min Cooldown)</span><br /></td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"displayid\":1,\"id\":" + id
                + ",\"level\":277,\"name\":\"" + name + "\",\"slot\":12,\"subclass\":0]]></json>"
                + "<jsonEquip><![CDATA[\"int\":150]]></jsonEquip>"
                + "</item></aowow>";
    }

    private static String platePayload(int id, String name) {
        return "<wowhead><item id=\"" + id + "\">"
                + "<name><![CDATA[" + name + "]]></name><level>200</level>"
                + "<quality id=\"4\">Epic</quality>"
                + "<class id=\"4\"><![CDATA[Armor]]></class>"
                + "<subclass id=\"4\"><![CDATA[Plate Armor]]></subclass>"
                + "<icon displayId=\"1\">inv_helmet_150black</icon>"
                + "<inventorySlot id=\"1\">Head</inventorySlot>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"displayid\":1,\"id\":" + id
                + ",\"level\":200,\"name\":\"" + name + "\",\"slot\":1,\"subclass\":4]]></json>"
                + "<jsonEquip><![CDATA[\"str\":100,\"sta\":100,\"armor\":1000]]></jsonEquip>"
                + "</item></wowhead>";
    }

    private static String payload(int id, String name, int slotId, String slotName) {
        return "<wowhead><item id=\"" + id + "\">"
                + "<name><![CDATA[" + name + "]]></name><level>200</level>"
                + "<quality id=\"4\">Epic</quality>"
                + "<class id=\"4\"><![CDATA[Armor]]></class>"
                + "<subclass id=\"1\"><![CDATA[Cloth Armor]]></subclass>"
                + "<icon displayId=\"1\">inv_helmet_150black</icon>"
                + "<inventorySlot id=\"" + slotId + "\">" + slotName + "</inventorySlot>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"displayid\":1,\"id\":" + id
                + ",\"level\":200,\"name\":\"" + name + "\",\"slot\":" + slotId + ",\"subclass\":1]]></json>"
                + "<jsonEquip><![CDATA[\"int\":100,\"sta\":100,\"splpwr\":200,\"armor\":1000]]></jsonEquip>"
                + "</item></wowhead>";
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Path createItemsFile() {
        try {
            return Files.createTempDirectory("jrawr-item-editor-test-").resolve("jrawr.items.xml");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
