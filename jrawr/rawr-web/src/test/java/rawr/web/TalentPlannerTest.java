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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the tabbed paper-doll and the WoW-style talent/glyph planner: the Gear/
 * Talents/Glyphs tabs, the always-visible rating strip, metadata, the 71-point/row/prerequisite
 * rules, glyph limits, reset and the compact-string fallback.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TalentPlannerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
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
    void paperDollUsesGearTalentsAndGlyphsTabs() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        int paperDoll = page.indexOf("id=\"paperdoll\"");
        int talents = page.indexOf("id=\"talent-panel\"");
        int glyphs = page.indexOf("id=\"glyph-panel\"");
        assertThat(paperDoll).isGreaterThanOrEqualTo(0);
        assertThat(talents).isGreaterThan(paperDoll);
        assertThat(glyphs).isGreaterThan(talents);
        assertThat(page)
                .contains("data-paperdoll-tab=\"gear\"")
                .contains("data-paperdoll-tab=\"talents\"")
                .contains("data-paperdoll-tab=\"glyphs\"")
                .contains("data-paperdoll-tab=\"buffs\"")
                .contains("data-paperdoll-panel=\"gear\"")
                .contains("data-paperdoll-panel=\"talents\"")
                .contains("data-paperdoll-panel=\"glyphs\"")
                .contains("data-paperdoll-panel=\"buffs\"")
                .contains("id=\"paperdoll-rating\"")
                .doesNotContain("Open talent planner");
    }

    @Test
    void magePlannerRendersWowTreesIconsDescriptionsAndGlyphs() throws Exception {
        MockHttpSession session = new MockHttpSession();
        importFixture(session, "oracle/fixtures/mage/arcane_277_raid.xml");

        String planner = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(planner)
                .contains("id=\"talent-panel\"")
                .contains("id=\"glyph-panel\"")
                .contains("Arcane", "Fire", "Frost")
                .contains("class=\"talent-tree spec-arcane\"", "class=\"talent-tree spec-fire\"",
                        "class=\"talent-tree spec-frost\"")
                .doesNotContain("Click a slot to change its item.")
                .contains("data-talent-index=\"0\"")
                .contains("Presence of Mind")
                .contains("spell_holy_dispelmagic") // icon metadata from the legacy attributes
                .contains("Reduces the chance your helpful spells") // description metadata
                .contains("Points spent")
                .contains("Major Glyphs", "Minor Glyphs")
                .contains("Rawr does not model minor glyphs for this class")
                .contains("71");
        assertThat(talentString(session)).startsWith("2300051331");
    }

    @Test
    void warriorPlannerShowsMinorGlyphsInThePicker() throws Exception {
        MockHttpSession session = new MockHttpSession();
        importFixture(session, "oracle/fixtures/dpswarr/dpswarr_arms_277.xml");

        String planner = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(planner)
                .contains("Arms", "Fury", "Protection")
                .contains("Glyph of Command") // a modeled minor Warrior glyph
                .doesNotContain("Rawr does not model minor glyphs for this class");
    }

    @Test
    void addingAndRemovingTalentsEnforcesMaxRankAndThePointBudget() throws Exception {
        MockHttpSession session = mageSession();

        String added = adjust(session, 0, 1);
        assertThat(talentString(session)).startsWith("1");
        assertThat(added).contains("is-learned", "1/2");
        assertThat(added).as("empty glyph slots carry no index").doesNotContain("data-glyph-index=\"\"");

        added = adjust(session, 0, 1);
        assertThat(talentString(session)).startsWith("2");
        assertThat(added).contains("is-maxed", "2/2");

        String tooMany = adjust(session, 0, 1);
        assertThat(tooMany).contains("data-talent-error=\"true\"", "already at maximum rank");
        assertThat(talentString(session)).startsWith("2");

        String removed = adjust(session, 0, -1);
        assertThat(talentString(session)).startsWith("1");
        removed = adjust(session, 0, -1);
        assertThat(talentString(session)).startsWith("0");
        String none = adjust(session, 0, -1);
        assertThat(none).contains("no points to remove");
    }

    @Test
    void treeRowsAndPrerequisitesAreValidated() throws Exception {
        MockHttpSession session = mageSession();

        // Arcane Fortitude (index 3) is row 2 and needs 5 points in Arcane first.
        String locked = adjust(session, 3, 1);
        assertThat(locked).contains("Requires 5 points in Arcane");

        // Fill two rank-1 talents to open the row.
        adjust(session, 0, 1);
        adjust(session, 0, 1);
        adjust(session, 1, 1);
        adjust(session, 1, 1);
        adjust(session, 1, 1);
        String opened = adjust(session, 3, 1);
        assertThat(talentString(session)).contains("1");
        assertThat(opened).doesNotContain("Requires 5 points in Arcane");
    }

    @Test
    void removingPrerequisitePointsCascadesToDependentTalents() throws Exception {
        MockHttpSession session = new MockHttpSession();
        importFixture(session, "oracle/fixtures/mage/arcane_277_raid.xml");

        // Presence of Mind (15) is maxed and Arcane Instability (18) is learned behind it.
        String removed = adjust(session, 15, -1);
        assertThat(removed).doesNotContain("data-talent-error=\"true\"");
        int[] data = mageTalentData(session);
        assertThat(data[18]).as("dependent talent reset").isZero();
    }

    @Test
    void removingRowPointsCascadesAndClosesLaterRows() throws Exception {
        MockHttpSession session = mageSession();

        // Arcane row 1 has two talents (indices 0 and 1); spend exactly five points.
        adjust(session, 0, 1);
        adjust(session, 0, 1);
        adjust(session, 1, 1);
        adjust(session, 1, 1);
        adjust(session, 1, 1);
        adjust(session, 3, 1); // row 2 talent, now legal
        assertThat(mageTalentData(session)[3]).isEqualTo(1);

        String removed = adjust(session, 1, -1); // previous row drops to 4: row 2 closes
        assertThat(removed).doesNotContain("data-talent-error=\"true\"");
        int[] data = mageTalentData(session);
        assertThat(data[3]).as("row 2 talent reset when previous rows drop below five points").isZero();
        assertThat(data[1]).as("the clicked row 1 point was still removed").isEqualTo(2);
        assertThat(removed).contains("data-talent-index=\"3\"", "data-can-add=\"false\"");
    }

    @Test
    void glyphsToggleAndRespectThreeMajorSlots() throws Exception {
        MockHttpSession session = mageSession();

        String one = toggleGlyph(session, 0);
        assertThat(one).contains("is-selected");
        assertThat(glyphPart(session).charAt(0)).isEqualTo('1');

        toggleGlyph(session, 1);
        String three = toggleGlyph(session, 2);
        assertThat(glyphPart(session).chars().filter(value -> value == '1').count()).isEqualTo(3);
        assertThat(three)
                .contains("data-glyph-locked=\"true\"")
                .contains("All major glyph slots are full. Remove one first.");

        String full = toggleGlyph(session, 3);
        assertThat(full).contains("All major glyph slots are full.");

        toggleGlyph(session, 1);
        assertThat(glyphPart(session).charAt(1)).isEqualTo('0');
        assertThat(glyphPart(session).chars().filter(value -> value == '1').count()).isEqualTo(2);
    }

    @Test
    void resetButtonsClearOnlyTheirOwnTab() throws Exception {
        MockHttpSession session = mageSession();
        adjust(session, 0, 1);
        toggleGlyph(session, 0);

        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(page)
                .contains("/character/talents/reset", "Reset talents",
                        "hx-confirm=\"Reset all talents for this character?\"")
                .contains("/character/glyphs/reset", "Reset glyphs",
                        "hx-confirm=\"Reset all glyphs for this character?\"");

        String glyphReset = html(mockMvc.perform(post("/character/glyphs/reset").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(glyphPart(session).chars().filter(value -> value == '1').count()).isZero();
        assertThat(talentString(session).split("\\.")[0])
                .as("glyph reset leaves talents untouched")
                .contains("1");

        String talentReset = html(mockMvc.perform(post("/character/talents/reset").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(talentString(session)).matches("0+\\.[0-9]+");
        assertThat(glyphPart(session).chars().filter(value -> value == '1').count()).isZero();
        assertThat(talentReset).contains("Points spent");
    }

    private MockHttpSession mageSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/character/meta")
                        .param("model", "Mage")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    private void importFixture(MockHttpSession session, String relativePath) throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve(relativePath));
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "character.xml", "text/xml", xml))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    private String adjust(MockHttpSession session, int index, int delta) throws Exception {
        MvcResult result = mockMvc.perform(post("/character/talents/adjust")
                        .param("index", Integer.toString(index))
                        .param("delta", Integer.toString(delta))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        return html(result);
    }

    private String toggleGlyph(MockHttpSession session, int index) throws Exception {
        MvcResult result = mockMvc.perform(post("/character/glyphs/toggle")
                        .param("index", Integer.toString(index))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        return html(result);
    }

    private static String talentString(MockHttpSession session) {
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        assertThat(character).as("session character").isNotNull();
        return character.getCurrentTalents().toString();
    }

    private static int[] mageTalentData(MockHttpSession session) {
        Character character = (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
        assertThat(character).as("session character").isNotNull();
        return character.getMageTalents().getData();
    }

    private static String glyphPart(MockHttpSession session) {
        String value = talentString(session);
        int dot = value.indexOf('.');
        return dot < 0 ? "" : value.substring(dot + 1);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
