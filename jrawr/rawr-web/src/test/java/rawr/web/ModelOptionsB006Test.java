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
import rawr.models.dpswarr.CalculationOptionsDPSWarr;
import rawr.models.hunter.CalculationOptionsHunter;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.SpellId;
import rawr.models.mage.SpellWeight;
import rawr.models.retribution.Ability;
import rawr.models.retribution.CalculationOptionsRetribution;
import rawr.models.shadowpriest.CalculationOptionsShadowPriest;
import rawr.models.tree.CalculationOptionsTree;
import rawr.models.tree.CalculationOptionsTree.SpellProfile;
import rawr.models.warlock.CalculationOptionsWarlock;
import rawr.models.warlock.Rotation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Purpose-built B-006 editors: priorities, rotations, profiles, custom mix and maintenance. */
@SpringBootTest
@AutoConfigureMockMvc
class ModelOptionsB006Test {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

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
    void hunterShotPrioritySelectsApply() throws Exception {
        MockHttpSession session = importFixture("hunter/hunter_277_procs.xml", "Hunter");
        String html = options(session);
        assertThat(html).contains("data-option=\"priority1\"", ">Shot priority<",
                "value=\"Arcane Shot\"", "value=\"Steady Shot\"")
                .doesNotContain("data-option=\"priorityIndex1\"");

        apply(session, "Hunter",
                "priority1", "Arcane Shot",
                "priority2", "Steady Shot",
                "priority3", "Kill Shot",
                "priority4", "None",
                "priority5", "None",
                "priority6", "None",
                "priority7", "None",
                "priority8", "None",
                "priority9", "None",
                "priority10", "None");
        CalculationOptionsHunter options =
                (CalculationOptionsHunter) character(session).getCalculationOptions();
        assertThat(options.getPriorityIndexes())
                .containsExactly(2, 8, 9, 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void shadowPriestSpellPriorityAppliesAndRejectsDuplicates() throws Exception {
        MockHttpSession session = importFixture(
                "shadowpriest/shadowpriest_277_procs.xml", "ShadowPriest");
        assertThat(options(session)).contains("data-option=\"spellPriority1\"",
                ">Spell priority<", "Vampiric Touch", "Mind Flay");

        apply(session, "ShadowPriest",
                "spellPriority1", "Mind Blast",
                "spellPriority2", "Vampiric Touch",
                "spellPriority3", "Devouring Plague",
                "spellPriority4", "Shadow Word: Pain",
                "spellPriority5", "Mind Flay",
                "spellPriority6", "Shadow Word: Death");
        CalculationOptionsShadowPriest options =
                (CalculationOptionsShadowPriest) character(session).getCalculationOptions();
        assertThat(options.getSpellPriority()).containsExactly(
                "Mind Blast", "Vampiric Touch", "Devouring Plague",
                "Shadow Word: Pain", "Mind Flay", "Shadow Word: Death");

        String error = applyExpectingError(session, "ShadowPriest",
                "spellPriority1", "Mind Blast",
                "spellPriority2", "Mind Blast",
                "spellPriority3", "Vampiric Touch",
                "spellPriority4", "Shadow Word: Pain",
                "spellPriority5", "Mind Flay",
                "spellPriority6", "Shadow Word: Death");
        assertThat(error).contains("Each spell can only appear once");
        assertThat(options.getSpellPriority()).containsExactly(
                "Mind Blast", "Vampiric Touch", "Devouring Plague",
                "Shadow Word: Pain", "Mind Flay", "Shadow Word: Death");
    }

    @Test
    void warlockRotationEditorAndCollectionActions() throws Exception {
        MockHttpSession session = importFixture("warlock/warlock_demonology_277.xml", "Warlock");
        String html = options(session);
        assertThat(html).contains("data-option=\"rotationName\"",
                "data-option=\"rotationPriority1\"", "Affliction", "Demonology",
                "Destruction", "name=\"kind\" value=\"warlockRotation\"")
                .doesNotContain("data-option=\"activeRotationIndex\"");

        apply(session, "Warlock",
                "rotationName", "Demo Test",
                "rotationFiller", "Shadow Bolt",
                "rotationExecute", "Drain Soul",
                "rotationPriority1", "Haunt",
                "rotationPriority2", "Corruption",
                "rotationPriority3", "Curse Of Agony",
                "rotationPriority4", "(unused)",
                "rotationPriority5", "(unused)",
                "rotationPriority6", "(unused)",
                "rotationPriority7", "(unused)",
                "rotationPriority8", "(unused)",
                "rotationPriority9", "(unused)",
                "rotationPriority10", "(unused)",
                "rotationPriority11", "(unused)",
                "rotationPriority12", "(unused)");
        CalculationOptionsWarlock options =
                (CalculationOptionsWarlock) character(session).getCalculationOptions();
        Rotation active = options.getActiveRotation();
        assertThat(active.name).isEqualTo("Demo Test");
        assertThat(active.filler).isEqualTo("Shadow Bolt");
        assertThat(active.execute).isEqualTo("Drain Soul");
        assertThat(active.spellPriority).containsExactly(
                "Haunt", "Corruption", "Curse Of Agony");

        String error = applyExpectingError(session, "Warlock",
                "rotationName", "Demo Test",
                "rotationFiller", "Shadow Bolt",
                "rotationExecute", "(unused)",
                "rotationPriority1", "Conflagrate",
                "rotationPriority2", "Immolate",
                "rotationPriority3", "(unused)",
                "rotationPriority4", "(unused)",
                "rotationPriority5", "(unused)",
                "rotationPriority6", "(unused)",
                "rotationPriority7", "(unused)",
                "rotationPriority8", "(unused)",
                "rotationPriority9", "(unused)",
                "rotationPriority10", "(unused)",
                "rotationPriority11", "(unused)",
                "rotationPriority12", "(unused)");
        assertThat(error).contains("Conflagrate may only appear after Immolate");
        assertThat(active.spellPriority).containsExactly(
                "Haunt", "Corruption", "Curse Of Agony");

        int before = options.getRotations().size();
        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "warlockRotation")
                        .param("action", "ADD")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getRotations()).hasSize(before + 1);
        assertThat(options.getActiveRotationIndex()).isEqualTo(before);

        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "warlockRotation")
                        .param("action", "SELECT")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getActiveRotationIndex()).isZero();

        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "warlockRotation")
                        .param("action", "DELETE")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getRotations()).hasSize(before);
    }

    @Test
    void retributionRotationAppliesAndRejectsDuplicates() throws Exception {
        MockHttpSession session = importFixture("retribution/ret_277_procs.xml", "Retribution");
        assertThat(options(session)).contains("data-option=\"rotationAbility1\"",
                "Crusader Strike", "Hammer of Wrath", "Judgement")
                .doesNotContain("data-option=\"effectiveCD\"");

        apply(session, "Retribution",
                "simulateRotation", "true",
                "rotationAbility1", "Crusader Strike",
                "rotationAbility2", "Judgement",
                "rotationAbility3", "Divine Storm",
                "rotationAbility4", "(unused)",
                "rotationAbility5", "(unused)",
                "rotationAbility6", "(unused)");
        CalculationOptionsRetribution options =
                (CalculationOptionsRetribution) character(session).getCalculationOptions();
        assertThat(options.getRotations()).isNotEmpty();
        assertThat(options.getRotations().get(0))
                .containsExactly(Ability.CrusaderStrike, Ability.Judgement, Ability.DivineStorm);

        String error = applyExpectingError(session, "Retribution",
                "simulateRotation", "true",
                "rotationAbility1", "Crusader Strike",
                "rotationAbility2", "Crusader Strike",
                "rotationAbility3", "(unused)",
                "rotationAbility4", "(unused)",
                "rotationAbility5", "(unused)",
                "rotationAbility6", "(unused)");
        assertThat(error).contains("Each ability can only appear once");
        assertThat(options.getRotations().get(0))
                .containsExactly(Ability.CrusaderStrike, Ability.Judgement, Ability.DivineStorm);
    }

    @Test
    void treeProfileEditorAndSavedProfileSelection() throws Exception {
        MockHttpSession session = importFixture("tree/tree_277_procs.xml", "Tree");
        assertThat(options(session)).contains("data-option=\"profileName\"",
                "data-option=\"profileFightDuration\"", ">Profile<", "Fight duration");

        apply(session, "Tree",
                "profileName", "Probe",
                "profileFightDuration", "420",
                "profileRejuvFrac", "60");
        CalculationOptionsTree options =
                (CalculationOptionsTree) character(session).getCalculationOptions();
        assertThat(options.getCurrent().getName()).isEqualTo("Probe");
        assertThat(options.getCurrent().getFightDuration()).isEqualTo(420);
        assertThat(options.getCurrent().getRejuvFrac()).isEqualTo(60);
        assertThat(options.getProfiles()).isEmpty();
        assertThat(options(session)).contains("name=\"kind\" value=\"treeProfile\"");

        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "treeProfile")
                        .param("action", "ADD")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getProfiles()).hasSize(1);
        assertThat(options.getProfiles().get(0).getName()).isEqualTo("New Profile");

        SpellProfile alpha = new SpellProfile();
        alpha.setName("Alpha");
        SpellProfile beta = new SpellProfile();
        beta.setName("Beta");
        options.setProfiles(new ArrayList<SpellProfile>(List.of(alpha, beta)));
        options.setCurrent(alpha);
        String withProfiles = options(session);
        assertThat(withProfiles).contains("Alpha", "Beta", "name=\"kind\" value=\"treeProfile\"");

        String switched = html(mockMvc.perform(post("/character/options/collection")
                        .param("kind", "treeProfile")
                        .param("action", "SELECT")
                        .param("index", "1")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(options.getCurrent()).isSameAs(beta);
        assertThat(switched).contains("value=\"Beta\"");

        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "treeProfile")
                        .param("action", "ADD")
                        .param("index", "1")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getProfiles()).hasSize(3);
        assertThat(options.getCurrent().getName()).isEqualTo("New Profile");
        assertThat(options.getCurrent()).isNotSameAs(beta);

        mockMvc.perform(post("/character/options/collection")
                        .param("kind", "treeProfile")
                        .param("action", "DELETE")
                        .param("index", "0")
                        .session(session))
                .andExpect(status().isOk());
        assertThat(options.getProfiles()).hasSize(2);
        assertThat(options.getCurrent()).isSameAs(beta);
    }

    @Test
    void mageCustomSpellMixEditsAndTrimsUnusedRows() throws Exception {
        MockHttpSession session = importFixture("mage/fire_custommix.xml", "Mage");
        String html = options(session);
        assertThat(html).contains("data-option=\"customSpellMixSpell1\"",
                "data-option=\"customSpellMixWeight1\"",
                "data-option=\"customSpellMixEnabled\"", ">Custom spell mix<");
        assertThat(html).doesNotContain("data-option=\"incrementalSet");
        String first = fieldHtml(html, "customSpellMixSpell1");
        assertThat(first).contains("Fireball", "selected=\"selected\"");

        apply(session, "Mage",
                "customSpellMixEnabled", "true",
                "customSpellMixOnly", "true",
                "customSpellMixSpell1", "Pyroblast",
                "customSpellMixWeight1", "0.5",
                "customSpellMixSpell2", "None",
                "customSpellMixSpell3", "None");
        CalculationOptionsMage options =
                (CalculationOptionsMage) character(session).getCalculationOptions();
        assertThat(options.getCustomSpellMix()).hasSize(1);
        SpellWeight weight = options.getCustomSpellMix().get(0);
        assertThat(weight.getSpell()).isEqualTo(SpellId.Pyroblast);
        assertThat(weight.getWeight()).isEqualTo(0.5f);
    }

    @Test
    void dpsWarrMaintenanceIsGroupedAndApplies() throws Exception {
        MockHttpSession session = importFixture("dpswarr/dpswarr_fury_277.xml", "DPSWarr");
        String html = options(session);
        assertThat(html).contains("data-option=\"maintenanceBerserkerRage\"",
                "data-option=\"maintenanceHeroicStrike\"",
                "Maintenance: rage and shouts", "Maintenance: damage dealers",
                "Maintenance: rage dumps");
        assertThat(html).doesNotContain("data-option=\"m_BerserkerRage\"");

        apply(session, "DPSWarr", "maintenanceBerserkerRage", "false");
        CalculationOptionsDPSWarr options =
                (CalculationOptionsDPSWarr) character(session).getCalculationOptions();
        assertThat(options.getMaintenance()[1]).isFalse();
    }

    private String options(MockHttpSession session) throws Exception {
        return html(mockMvc.perform(get("/character/options").session(session))
                .andExpect(status().isOk()).andReturn());
    }

    private void apply(MockHttpSession session, String model, String... values) throws Exception {
        var request = post("/character/options").param("model", model).session(session);
        for (int i = 0; i < values.length; i += 2) {
            request.param(values[i], values[i + 1]);
        }
        String response = html(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
        assertThat(response).doesNotContain("data-state=\"error\"");
    }

    private String applyExpectingError(MockHttpSession session, String model, String... values)
            throws Exception {
        var request = post("/character/options").param("model", model).session(session);
        for (int i = 0; i < values.length; i += 2) {
            request.param(values[i], values[i + 1]);
        }
        String response = html(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
        assertThat(response).contains("data-options-error=\"true\"");
        return response;
    }

    private MockHttpSession importFixture(String fixture, String model) throws Exception {
        byte[] xml = Files.readAllBytes(REPO_ROOT.resolve("oracle/fixtures").resolve(fixture));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(multipart("/character")
                        .file(new MockMultipartFile("file", "fixture.xml", "text/xml", xml))
                        .param("model", model)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        return session;
    }

    private static Character character(MockHttpSession session) {
        return (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
    }

    private static String fieldHtml(String html, String key) {
        Matcher matcher = Pattern.compile("data-option=\"" + Pattern.quote(key)
                + "\"[^>]*>(.*?)</div>", Pattern.DOTALL).matcher(html);
        assertThat(matcher.find()).as("field %s in %s", key, html).isTrue();
        return matcher.group();
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
