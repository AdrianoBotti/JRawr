package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.character.Character;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;
import rawr.data.CharacterFixtureLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MockMvc coverage for the boss/encounter options modal and its persistence. */
@SpringBootTest
@AutoConfigureMockMvc
class BossOptionsControllerTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private EditorService editorService;

    @Autowired
    private BossPresetService bossPresetService;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
    }

    @Test
    void headerOffersBossOptionsAndModalRendersFields() throws Exception {
        MockHttpSession session = session();
        String page = html(mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(page).contains("hx-get=\"/character/boss\"", "Boss / encounter options");

        String modal = html(mockMvc.perform(get("/character/boss").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal)
                .contains("role=\"dialog\"", "aria-labelledby=\"boss-title\"", "id=\"boss-panel\"",
                        "modal-body boss-body", "dialog-section-title", "boss-intro",
                        "boss-form-actions", "name=\"level\"", "name=\"armor\"", "name=\"health\"",
                        "name=\"berserkTimer\"", "name=\"resistPhysical\"", "name=\"inBack\"",
                        "name=\"multiTargs\"", "Load a canonical encounter",
                        "Save encounter", "Reset defaults");
    }

    @Test
    void loadingACanonicalPresetCopiesItsDefinition() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        BossPresetService.Preset preset = bossPresetService.presets().stream()
                .filter(p -> p.name().equals("Patchwerk"))
                .findFirst()
                .orElseThrow();
        String response = html(mockMvc.perform(post("/character/boss/preset")
                        .param("preset", Integer.toString(preset.index()))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(response).contains("id=\"boss-panel\"");
        assertThat(character.getBossOptions().getName()).isEqualTo("Patchwerk");
        assertThat(character.getBossOptions().getBossName()).isEqualTo("Patchwerk");
        assertThat(character.getBossOptions().getAttacks()).isNotEmpty();
    }

    @Test
    void saveAppliesFieldsAndRefreshesPaperDoll() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        String response = html(mockMvc.perform(post("/character/boss")
                        .param("name", "UI Probe")
                        .param("content", "T8_5")
                        .param("instance", "Ulduar")
                        .param("version", "V_25H")
                        .param("comment", "ui test")
                        .param("level", "82")
                        .param("armor", "12000")
                        .param("health", "900000")
                        .param("berserkTimer", "420")
                        .param("speedKillTimer", "210")
                        .param("maxPlayers", "25")
                        .param("minHealers", "5")
                        .param("minTanks", "2")
                        .param("timeBossIsInvuln", "7.5")
                        .param("inBackPercMelee", "0.25")
                        .param("inBackPercRanged", "0.5")
                        .param("under35Perc", "0.3")
                        .param("under20Perc", "0.1")
                        .param("resistPhysical", "0.1")
                        .param("resistFrost", "0.2")
                        .param("resistFire", "0.3")
                        .param("resistNature", "0.4")
                        .param("resistArcane", "0.5")
                        .param("resistShadow", "0.6")
                        .param("resistHoly", "0.7")
                        .param("inBack", "true")
                        .param("multiTargs", "true")
                        .param("movingTargs", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(response).contains("id=\"boss-panel\"", "hx-swap-oob=\"true\"");

        BossOptions boss = character.getBossOptions();
        assertThat(boss.getName()).isEqualTo("UI Probe");
        assertThat(boss.getInstance()).isEqualTo("Ulduar");
        assertThat(boss.getLevel()).isEqualTo(82);
        assertThat(boss.getArmor()).isEqualTo(12000);
        assertThat(boss.getHealth()).isEqualTo(900000f);
        assertThat(boss.getResistFire()).isEqualTo(0.3d);
        assertThat(boss.getInBack()).isTrue();
        assertThat(boss.getMultiTargs()).isTrue();
        assertThat(boss.getMovingTargs()).isTrue();
        assertThat(boss.getStunningTargs()).isFalse();
        assertThat(editorService.isDirty(session)).isTrue();

        String persisted = html(mockMvc.perform(get("/character/boss").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(persisted).contains("value=\"UI Probe\"", "value=\"Ulduar\"", "value=\"82\"");
    }

    @Test
    void invalidFieldReRendersThePanelWithoutChangingOptions() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        int level = character.getBossOptions().getLevel();
        String response = html(mockMvc.perform(post("/character/boss")
                        .param("level", "99")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(response).contains("id=\"boss-panel\"", "Level must be between");
        assertThat(character.getBossOptions().getLevel()).isEqualTo(level);
    }

    @Test
    void resetRestoresDefaults() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        character.getBossOptions().setName("Changed");
        String response = html(mockMvc.perform(post("/character/boss/reset").session(session))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(response).contains("id=\"boss-panel\"");
        assertThat(character.getBossOptions().getName()).isEqualTo("Generic");
    }

    @TempDir
    Path tempDir;

    @Test
    void modalRendersTargetGroupAndAttackEditors() throws Exception {
        MockHttpSession session = session();
        String modal = html(mockMvc.perform(get("/character/boss").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(modal)
                .contains("id=\"boss-lists\"", "Target groups",
                        "Extra enemies (adds) that spawn during the fight",
                        "Add target group", "Attacks and DoTs", "Add attack", "Add DoT");
    }

    @Test
    void addUpdateAndDeleteTargetGroupRoundTrips() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);

        String added = html(mockMvc.perform(post("/character/boss/targets/add").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(added).contains("id=\"boss-lists\"", "hx-swap-oob=\"true\"",
                        "name=\"frequency\"", "name=\"nearBoss\"", "open=\"open\"")
                .doesNotContain("class=\"boss-form\"");
        assertThat(character.getBossOptions().targets).hasSize(1);
        assertThat(editorService.isDirty(session)).isTrue();

        html(mockMvc.perform(post("/character/boss/targets/update")
                        .param("index", "0")
                        .param("frequency", "30")
                        .param("duration", "15000")
                        .param("chance", "75")
                        .param("numTargs", "3")
                        .param("nearBoss", "on")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        TargetGroup group = character.getBossOptions().targets.get(0);
        assertThat(group.frequency).isEqualTo(30f);
        assertThat(group.duration).isEqualTo(15000f);
        assertThat(group.chance).isEqualTo(0.75f);
        assertThat(group.numTargs).isEqualTo(3f);
        assertThat(group.nearBoss).isTrue();

        html(mockMvc.perform(post("/character/boss/targets/delete")
                        .param("index", "0").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(character.getBossOptions().targets).isEmpty();
    }

    @Test
    void dotAddedThroughTheEditorSurvivesSaveAndReload() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        html(mockMvc.perform(post("/character/boss/attacks/add")
                        .param("kind", "DOT").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(character.getBossOptions().getAttacks().get(0)).isInstanceOf(DoT.class);

        String updated = html(mockMvc.perform(attackUpdateRequest("0").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(updated).contains("id=\"boss-lists\"", "boss-row-dot",
                "name=\"damageType\"", "name=\"attackType\"", "name=\"damagePerTick\"",
                "name=\"tickInterval\"", "name=\"ignoresMeleeDps\"");

        Attack attack = character.getBossOptions().getAttacks().get(0);
        assertThat(attack).isInstanceOf(DoT.class);
        assertThat(attack.name).isEqualTo("Editor DoT");
        assertThat(attack.damageType).isEqualTo(ItemDamageType.Shadow);
        assertThat(attack.attackType).isEqualTo(AttackTypes.AT_AOE);
        assertThat(attack.getDamagePerHit()).isEqualTo(4321f);
        assertThat(attack.maxNumTargets).isEqualTo(3f);
        assertThat(attack.attackSpeed).isEqualTo(2.5f);
        assertThat(attack.missable).isTrue();
        assertThat(attack.ignoresMeleeDps).isTrue();
        DoT dot = (DoT) attack;
        assertThat(dot.damagePerTick).isEqualTo(500f);
        assertThat(dot.numTicks).isEqualTo(4f);
        assertThat(dot.tickInterval).isEqualTo(3f);

        Path saved = tempDir.resolve("boss-dot.xml");
        character.save(saved);
        Character loaded = CharacterFixtureLoader.load(saved);
        assertThat(loaded.getBossOptions().getAttacks()).hasSize(1);
        DoT loadedDot = (DoT) loaded.getBossOptions().getAttacks().get(0);
        assertThat(loadedDot.name).isEqualTo("Editor DoT");
        assertThat(loadedDot.damagePerTick).isEqualTo(500f);
        assertThat(loadedDot.numTicks).isEqualTo(4f);
        assertThat(loadedDot.tickInterval).isEqualTo(3f);
        assertThat(loadedDot.ignoresMeleeDps).isTrue();
    }

    @Test
    void invalidAttackUpdateReRendersTheErrorWithoutChangingTheRow() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        html(mockMvc.perform(post("/character/boss/attacks/add").session(session))
                .andExpect(status().isOk()).andReturn());
        String originalName = character.getBossOptions().getAttacks().get(0).name;

        String response = html(mockMvc.perform(post("/character/boss/attacks/update")
                        .param("index", "0")
                        .param("damageType", "Bogus")
                        .session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(response).contains("Damage type is not recognized.");
        assertThat(character.getBossOptions().getAttacks().get(0).name).isEqualTo(originalName);
    }

    @Test
    void unsupportedModelHidesListEditorsButKeepsFieldValues() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        character.setCurrentModel("Healadin");

        String healer = html(mockMvc.perform(get("/character/boss").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(healer)
                .contains("does not use boss attacks", "does not use target groups",
                        "boss-unsupported", "name=\"level\"")
                .doesNotContain("hx-post=\"/character/boss/attacks/add\"",
                        "hx-post=\"/character/boss/targets/add\"");

        character.setCurrentModel("Mage");
        String mage = html(mockMvc.perform(get("/character/boss").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(mage).contains("hx-post=\"/character/boss/attacks/add\"",
                "hx-post=\"/character/boss/targets/add\"");
    }

    @Test
    void legacySavedDotsArePreservedWhenEditingAttacks() throws Exception {
        MockHttpSession session = session();
        Character character = character(session);
        DoT legacy = new DoT();
        legacy.name = "Legacy DoT";
        character.getBossOptions().setDoTs(new ArrayList<DoT>(List.of(legacy)));

        String response = html(mockMvc.perform(post("/character/boss/attacks/add")
                        .param("kind", "ATTACK").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(response).contains("legacy saved DoT entries");
        assertThat(character.getBossOptions().getDoTs()).hasSize(1);
        assertThat(character.getBossOptions().getDoTs().get(0).name).isEqualTo("Legacy DoT");
    }

    private static MockHttpServletRequestBuilder attackUpdateRequest(String index) {
        return post("/character/boss/attacks/update")
                .param("index", index)
                .param("name", "Editor DoT")
                .param("damageType", "Shadow")
                .param("attackType", "AT_AOE")
                .param("damagePerHit", "4321")
                .param("maxNumTargets", "3")
                .param("attackSpeed", "2.5")
                .param("damageIsPerc", "on")
                .param("useParryHaste", "on")
                .param("isDualWielding", "on")
                .param("interruptable", "on")
                .param("missable", "on")
                .param("dodgable", "on")
                .param("parryable", "on")
                .param("blockable", "on")
                .param("ignoresMTank", "on")
                .param("ignoresOTank", "on")
                .param("ignoresHealers", "on")
                .param("ignoresMeleeDps", "on")
                .param("damagePerTick", "500")
                .param("numTicks", "4")
                .param("tickInterval", "3");
    }

    private MockHttpSession session() throws Exception {
        Character character = CharacterFixtureLoader.load(
                REPO_ROOT.resolve("oracle/fixtures/mage/arcane_277_raid.xml"));
        character.setCurrentModel("Mage");
        character.recalculateSetBonuses();
        MockHttpSession session = new MockHttpSession();
        editorService.openCharacter(session, character, null);
        return session;
    }

    private static Character character(MockHttpSession session) {
        return (Character) session.getAttribute(EditorService.SESSION_CHARACTER);
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
