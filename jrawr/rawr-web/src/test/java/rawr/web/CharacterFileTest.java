package rawr.web;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import rawr.base.character.Character;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.data.CharacterFixtureLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for the File menu flows: new/open/save/save-as/delete, dirty tracking and the
 * unsaved-changes prompt. Characters are written to a throwaway {@code jrawr.characters-dir},
 * cleaned before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CharacterFileTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
    private static final Path CHARACTERS_DIR = createTempDir();
    private static final String SAVED_PREFIX = "/?saved=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir",
                () -> REPO_ROOT.resolve("jrawr/data").toString());
        registry.add("jrawr.characters-dir", CHARACTERS_DIR::toString);
    }

    @BeforeEach
    void loadData() throws IOException {
        dataBootstrap.load();
        if (Files.isDirectory(CHARACTERS_DIR)) {
            try (Stream<Path> paths = Files.list(CHARACTERS_DIR)) {
                for (Path path : paths.toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void downloadReturnsTheCurrentCharacterAsAttachmentXml() throws Exception {
        MockHttpSession session = newMageSession("Download Probe");
        equipHead(session);
        assertThat(editorService.isDirty(session)).isTrue();

        MvcResult download = mockMvc.perform(get("/character/download").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String disposition = download.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).contains("attachment").contains("Download-Probe.xml");
        byte[] bytes = download.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();

        Path saved = Files.createTempFile("jrawr-download-test-", ".xml");
        try {
            Files.write(saved, bytes);
            Character loaded = CharacterFixtureLoader.load(saved);
            assertThat(loaded.getName()).isEqualTo("Download Probe");
            assertThat(loaded.getCurrentModel()).isEqualTo("Mage");
            assertThat(loaded.get(rawr.base.character.CharacterSlot.Head)).isNotNull();
            assertThat(loaded.get(rawr.base.character.CharacterSlot.Head).getId()).isEqualTo(51281);
        } finally {
            Files.deleteIfExists(saved);
        }
        assertThat(editorService.isDirty(session)).as("download does not save the editor").isTrue();
    }

    @Test
    void equippingItemMarksDirtyAndNewReturnsSavePrompt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        // Fresh character: New just replaces it and redirects to the editor.
        mockMvc.perform(get("/character/new").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));

        equipHead(session);

        MvcResult prompt = mockMvc.perform(get("/character/new").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(prompt)).contains("data-modal=\"new-confirm\"", "Save &amp; New",
                "Discard &amp; New", "Cancel");
    }

    @Test
    void dirtyDotFollowsEquipSaveAsAndDirectSave() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult fresh = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(fresh))
                .contains("menu-dot is-hidden")
                .doesNotContain("class=\"menu-dot\"");

        equipHead(session);
        MvcResult dirty = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(dirty))
                .contains("class=\"menu-dot\"")
                .doesNotContain("menu-dot is-hidden");

        String fileName = saveAs(session, "Flag Hero");
        assertThat(fileName).isEqualTo("Flag-Hero.xml");
        MvcResult saved = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(saved)).contains("menu-dot is-hidden", fileName);

        mockMvc.perform(post("/character/slot/remove").param("slot", "Head").session(session))
                .andExpect(status().isOk());
        MvcResult edited = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(edited)).contains("class=\"menu-dot\"").doesNotContain("menu-dot is-hidden");

        MvcResult overwrite = mockMvc.perform(post("/character/save").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(overwrite)).contains("data-toast=\"saved\"");
        MvcResult cleared = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(cleared)).contains("menu-dot is-hidden");
    }

    @Test
    void discardActionStartsEmptyPaperDoll() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        String itemName = equipHead(session);

        mockMvc.perform(post("/character/new").param("action", "discard").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("New character", "data-state=\"incomplete\"")
                .doesNotContain(itemName);
    }

    @Test
    void saveAsMenuOpensModalWithSanitizedName() throws Exception {
        MockHttpSession session = newMageSession("Menu Hero");

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("hx-get=\"/character/save-as\"");

        MvcResult modal = mockMvc.perform(get("/character/save-as").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(modal)).contains("data-modal=\"save-as\"", "value=\"Menu-Hero\"");
    }

    @Test
    void saveWithoutCurrentFilePromptsForNameAndCreatesChosenFile() throws Exception {
        MockHttpSession session = newMageSession("Save Hero");
        equipHead(session);

        MvcResult prompt = mockMvc.perform(post("/character/save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Retarget", "#modal"))
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(prompt)).contains("data-modal=\"save-as\"", "name=\"name\"",
                "value=\"Save-Hero\"");
        assertThat(storedFiles()).isEmpty();

        String fileName = saveAs(session, "My Hero");
        assertThat(fileName).isEqualTo("My-Hero.xml");
        Path saved = CHARACTERS_DIR.resolve(fileName);
        assertThat(saved).exists();
        assertThat(Files.readString(saved, StandardCharsets.UTF_8))
                .contains("<Character", "<Name>Save Hero</Name>", "<CurrentModel>Mage</CurrentModel>");

        // Dirty was cleared, the current file shows in the File menu and New no longer asks.
        MvcResult page = mockMvc.perform(get("/?saved=" + fileName).session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("class=\"menu-file\"", fileName, "Saved as");
        mockMvc.perform(get("/character/new").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));
    }

    @Test
    void secondSaveWithoutChangesReportsNoChangesAndWritesNothing() throws Exception {
        MockHttpSession session = newMageSession("Quiet Hero");
        equipHead(session);
        String fileName = saveAs(session, "Quiet");
        String before = Files.readString(CHARACTERS_DIR.resolve(fileName), StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(post("/character/save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(result)).contains("data-toast=\"no-changes\"", "No changes to save");
        assertThat(storedFiles()).containsExactly(fileName);
        assertThat(Files.readString(CHARACTERS_DIR.resolve(fileName), StandardCharsets.UTF_8))
                .isEqualTo(before);
    }

    @Test
    void saveAfterEditOverwritesTheSameFileWithoutSuffix() throws Exception {
        MockHttpSession session = newMageSession("Before");
        equipHead(session);
        String fileName = saveAs(session, "Only One");
        assertThat(fileName).isEqualTo("Only-One.xml");

        mockMvc.perform(post("/character/meta")
                        .param("name", "After")
                        .param("race", "Human")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        MvcResult saved = mockMvc.perform(post("/character/save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(saved)).contains("data-toast=\"saved\"", "data-file=\"" + fileName + "\"");
        assertThat(storedFiles()).containsExactly(fileName);
        assertThat(Files.readString(CHARACTERS_DIR.resolve(fileName), StandardCharsets.UTF_8))
                .contains("<Name>After</Name>");
    }

    @Test
    void saveAsWithExplicitNameCreatesThatFileAndSubsequentSaveTargetsIt() throws Exception {
        MockHttpSession session = newMageSession("Twin");
        equipHead(session);

        String first = saveAs(session, "First");
        String second = saveAs(session, "Second");
        assertThat(first).isEqualTo("First.xml");
        assertThat(second).isEqualTo("Second.xml");

        mockMvc.perform(post("/character/meta")
                        .param("name", "Twin Edited")
                        .param("race", "Human")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/character/save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"));

        assertThat(storedFiles()).containsExactly("First.xml", "Second.xml");
        assertThat(Files.readString(CHARACTERS_DIR.resolve("First.xml"), StandardCharsets.UTF_8))
                .contains("<Name>Twin</Name>");
        assertThat(Files.readString(CHARACTERS_DIR.resolve("Second.xml"), StandardCharsets.UTF_8))
                .contains("<Name>Twin Edited</Name>");
    }

    @Test
    void saveAsExistingNameRequiresConfirmToOverwrite() throws Exception {
        MockHttpSession firstSession = newMageSession("Taken One");
        equipHead(firstSession);
        saveAs(firstSession, "Taken");

        MockHttpSession secondSession = newMageSession("Taken Two");
        equipHead(secondSession);

        MvcResult prompt = mockMvc.perform(post("/character/save-as")
                        .param("name", "Taken")
                        .session(secondSession))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(prompt)).contains("data-modal=\"save-as\"", "already exists",
                "Overwrite", "name=\"confirm\"");
        assertThat(Files.readString(CHARACTERS_DIR.resolve("Taken.xml"), StandardCharsets.UTF_8))
                .contains("<Name>Taken One</Name>");

        MvcResult overwrite = mockMvc.perform(post("/character/save-as")
                        .param("name", "Taken")
                        .param("confirm", "true")
                        .session(secondSession))
                .andExpect(status().isOk())
                .andReturn();
        String redirect = overwrite.getResponse().getHeader("HX-Redirect");
        assertThat(redirect).isNotNull();
        assertThat(redirect).startsWith(SAVED_PREFIX);
        assertThat(storedFiles()).containsExactly("Taken.xml");
        assertThat(Files.readString(CHARACTERS_DIR.resolve("Taken.xml"), StandardCharsets.UTF_8))
                .contains("<Name>Taken Two</Name>");
    }

    @Test
    void saveAsRejectsEmptyAndIllegalNamesInline() throws Exception {
        MockHttpSession session = newMageSession("Bad Name");
        equipHead(session);

        for (String name : new String[]{"   ", "///", "..."}) {
            MvcResult result = mockMvc.perform(post("/character/save-as")
                            .param("name", name)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("HX-Redirect"))
                    .andReturn();
            assertThat(html(result)).contains("data-modal=\"save-as\"", "inline-error");
        }
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void saveAndNewWithoutCurrentFileFallsBackToPrompt() throws Exception {
        MockHttpSession session = newMageSession("Prompt Me");
        equipHead(session);

        MvcResult prompt = mockMvc.perform(post("/character/new").param("action", "save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("HX-Redirect"))
                .andReturn();
        assertThat(html(prompt)).contains("data-modal=\"save-as\"", "value=\"new\"");

        String fileName = saveAsThen(session, "Fresh Start", "new");
        assertThat(fileName).isEqualTo("Fresh-Start.xml");
        assertThat(storedFiles()).containsExactly(fileName);

        // Save & New ended with a new, clean character and no current file.
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("New character").doesNotContain("class=\"menu-file\"");
        mockMvc.perform(get("/character/new").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));
    }

    @Test
    void saveAndNewWithCurrentFileOverwritesAndStartsNew() throws Exception {
        MockHttpSession session = newMageSession("Keep Me");
        equipHead(session);
        String fileName = saveAs(session, "Keep");
        assertThat(fileName).isEqualTo("Keep.xml");

        mockMvc.perform(post("/character/meta")
                        .param("name", "Keep Me Edited")
                        .param("race", "Human")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        MvcResult action = mockMvc.perform(post("/character/new").param("action", "save").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", SAVED_PREFIX + "Keep.xml"))
                .andReturn();
        assertThat(html(action)).doesNotContain("data-modal");
        assertThat(storedFiles()).containsExactly("Keep.xml");
        assertThat(Files.readString(CHARACTERS_DIR.resolve("Keep.xml"), StandardCharsets.UTF_8))
                .contains("<Name>Keep Me Edited</Name>");

        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains("New character");
    }

    @Test
    void openRestoresSavedItemFromXml() throws Exception {
        MockHttpSession saver = newMageSession("Open Hero");
        String itemName = equipHead(saver);
        String fileName = saveAs(saver, "Open Hero");

        MockHttpSession opener = new MockHttpSession();
        mockMvc.perform(get("/").session(opener)).andExpect(status().isOk());
        mockMvc.perform(post("/character/open").param("file", fileName).session(opener))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));

        MvcResult page = mockMvc.perform(get("/").session(opener))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page)).contains(itemName, "Open Hero", "class=\"menu-file\"");
        assertThat(CHARACTERS_DIR.resolve(fileName)).exists();

        // Opening clears dirty.
        mockMvc.perform(get("/character/new").session(opener))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"));
    }

    @Test
    void deleteRemovesSavedCharacterFromOpenPanel() throws Exception {
        MockHttpSession session = newMageSession("Delete Me");
        String fileName = saveAs(session, "Delete Me");
        assertThat(fileName).isEqualTo("Delete-Me.xml");

        MvcResult list = mockMvc.perform(get("/characters").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(list)).contains(fileName, "/character/open").contains("hx-confirm");

        MvcResult deleted = mockMvc.perform(post("/character/delete").param("file", fileName).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/"))
                .andReturn();
        assertThat(CHARACTERS_DIR.resolve(fileName)).doesNotExist();
        assertThat(html(deleted)).doesNotContain(fileName);

        // Deleting the open character resets the whole editor to a fresh character.
        MvcResult page = mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(page))
                .contains("New character")
                .doesNotContain("Delete Me", "Delete-Me.xml", "class=\"menu-file\"");
    }

    @Test
    void openPanelSelectsAndOpensAFileInOneStep() throws Exception {
        MockHttpSession session = newMageSession("Open Probe");
        String list = html(mockMvc.perform(get("/characters").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(list)
                .contains("Choose file…", "Choosing a file opens it immediately",
                        "id=\"open-file\"", "enctype=\"multipart/form-data\"")
                .doesNotContain("open-upload-btn", ">Open file</button>");
    }

    @Test
    void openPanelHidesAppSidecarFiles() throws Exception {
        MockHttpSession session = newMageSession("Sidecar Probe");
        String character = saveAs(session, "Sidecar Probe");
        for (String sideCar : List.of("jrawr.item-filters.xml", "jrawr.item-costs.xml",
                "jrawr.talents.xml", "jrawr.items.xml")) {
            Files.writeString(CHARACTERS_DIR.resolve(sideCar), "<side-car/>");
        }

        MvcResult list = mockMvc.perform(get("/characters").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(html(list)).contains(character)
                .doesNotContain("jrawr.item-filters.xml", "jrawr.item-costs.xml",
                        "jrawr.talents.xml", "jrawr.items.xml");
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpSession newMageSession(String name) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/character/meta")
                        .param("name", name)
                        .param("race", "Human")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    private String equipHead(MockHttpSession session) throws Exception {
        String fixture = new String(
                Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml")),
                StandardCharsets.UTF_8);
        java.util.regex.Matcher head =
                java.util.regex.Pattern.compile("<Head>([^<]*)</Head>").matcher(fixture);
        assertThat(head.find()).as("Head slot in the fixture").isTrue();
        int itemId = Integer.parseInt(head.group(1).split("\\.")[0]);
        Item item = ItemCache.findItemById(itemId);
        assertThat(item).as("fixture head item " + itemId + " in the item cache").isNotNull();
        mockMvc.perform(post("/character/slot")
                        .param("slot", "Head")
                        .param("itemId", String.valueOf(itemId))
                        .session(session))
                .andExpect(status().isOk());
        return item.getName();
    }

    private String saveAs(MockHttpSession session, String name) throws Exception {
        return saveAsThen(session, name, null);
    }

    private String saveAsThen(MockHttpSession session, String name, String then) throws Exception {
        MockHttpServletRequestBuilder request = post("/character/save-as").param("name", name);
        if (then != null) {
            request = request.param("then", then);
        }
        MvcResult result = mockMvc.perform(request.session(session))
                .andExpect(status().isOk())
                .andReturn();
        String redirect = result.getResponse().getHeader("HX-Redirect");
        assertThat(redirect).as("save-as redirect for " + name).isNotNull();
        assertThat(redirect).startsWith(SAVED_PREFIX);
        return URLDecoder.decode(redirect.substring(SAVED_PREFIX.length()), StandardCharsets.UTF_8);
    }

    private static List<String> storedFiles() throws IOException {
        try (Stream<Path> paths = Files.list(CHARACTERS_DIR)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("jrawr-characters-test-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
