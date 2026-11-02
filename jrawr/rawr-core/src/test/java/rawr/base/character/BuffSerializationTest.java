package rawr.base.character;

import rawr.base.util.XmlCanon;

import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Buff save/load: the writer must emit the original XmlSerializer BuffCache schema (compared
 * canonically against {@code tools/core-format-probe} output) and {@link Buff#loadBuffs} must
 * reproduce the C# LoadBuffs merge (custom file entries first, then defaults).
 */
class BuffSerializationTest {

    @TempDir
    Path tempDir;

    private List<Buff> previousRegistry;

    @BeforeEach
    void snapshot() {
        previousRegistry = Buff.getAllBuffs();
    }

    @AfterEach
    void restore() {
        Buff.load(previousRegistry == null ? new ArrayList<Buff>() : previousRegistry);
    }

    @Test
    void writerMatchesTheLegacyXmlSerializerSchema() throws Exception {
        Buff.load(probeBuffList());
        Path out = tempDir.resolve("BuffCache.xml");
        Buff.saveBuffs(out);

        assertThat(XmlCanon.canonical(out))
                .isEqualTo(XmlCanon.canonical(probeFile("core-format/buff-cache.xml")));
    }

    @Test
    void loadBuffsKeepsCustomEntriesAndReplacesNonCustomOnesWithDefaults() throws Exception {
        List<Buff> list = new ArrayList<Buff>();
        Buff custom = new Buff();
        custom.name = "Probe Custom";
        custom.group = "Probe Group";
        custom.isCustom = true;
        custom.stats = new Stats();
        custom.stats.setSpellPower(123f);
        list.add(custom);
        Buff stale = new Buff();
        stale.name = "Battle Shout";
        stale.stats = new Stats();
        stale.stats.setAttackPower(1f);
        list.add(stale);
        Buff.load(list);
        Path cache = tempDir.resolve("BuffCache.xml");
        Buff.saveBuffs(cache);

        Buff.loadBuffs(cache);

        assertThat(Buff.getBuffByName("Probe Custom")).isNotNull();
        assertThat(Buff.getBuffByName("Probe Custom").stats.getSpellPower()).isEqualTo(123f);
        assertThat(Buff.getBuffByName("Battle Shout").stats.getAttackPower()).isEqualTo(548f);
        assertThat(Buff.getAllBuffs()).hasSize(distinctDefaultNames() + 1);
    }

    @Test
    void loadBuffsWithoutAFileFallsBackToTheDefaults() throws Exception {
        Path missing = tempDir.resolve("no-such-BuffCache.xml");
        Buff.loadBuffs(missing);

        assertThat(Buff.getAllBuffs()).hasSize(distinctDefaultNames());
        assertThat(Buff.getBuffByName("Battle Shout")).isNotNull();
    }

    private static int distinctDefaultNames() {
        return new java.util.LinkedHashSet<String>(Buff.getDefaultBuffs().stream().map(b -> b.name).toList()).size();
    }

    @Test
    void loadBuffsWithDuplicateCustomNamesLeavesTheRegistryUntouched() throws Exception {
        Path cache = tempDir.resolve("duplicates.xml");
        Files.writeString(cache,
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<ArrayOfBuff xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "  <Buff><Name>Dup</Name><Stats /><SetThreshold>0</SetThreshold><Improvements />"
                        + "<IsTargetDebuff>false</IsTargetDebuff><IsCustom>true</IsCustom></Buff>\n"
                        + "  <Buff><Name>Dup</Name><Stats /><SetThreshold>0</SetThreshold><Improvements />"
                        + "<IsTargetDebuff>false</IsTargetDebuff><IsCustom>true</IsCustom></Buff>\n"
                        + "</ArrayOfBuff>\n");

        Buff.load(probeBuffList());
        List<Buff> before = Buff.getAllBuffs();
        Buff.loadBuffs(cache);

        assertThat(Buff.getAllBuffs()).isSameAs(before);
    }

    @Test
    void readerHandlesNilConflictingBuffs() throws Exception {
        Path cache = tempDir.resolve("nil-conflict.xml");
        Files.writeString(cache,
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<ArrayOfBuff xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "  <Buff><Name>NilConflict</Name><Stats /><SetThreshold>0</SetThreshold><Improvements />"
                        + "<IsTargetDebuff>false</IsTargetDebuff><IsCustom>true</IsCustom>"
                        + "<ConflictingBuffs><string xsi:nil=\"true\" /></ConflictingBuffs></Buff>\n"
                        + "</ArrayOfBuff>\n");

        Buff.loadBuffs(cache);

        Buff loaded = Buff.getBuffByName("NilConflict");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getConflictingBuffs()).containsExactly((String) null);
    }

    static List<Buff> probeBuffList() {
        List<Buff> list = new ArrayList<Buff>();
        Buff buff = new Buff();
        buff.name = "Probe Buff";
        buff.group = "Probe Group";
        buff.source = "Probe Source";
        buff.setName = "Probe Set";
        buff.setThreshold = 2;
        buff.stats = stats("Agility=10,SpellPower=50,BonusDamageMultiplier=0.03");
        buff.stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, stats("HasteRating=250"), 10f, 35f, 0.35f, 1));
        Buff improvement = new Buff();
        improvement.name = "Probe Improvement";
        improvement.stats = stats("Intellect=5");
        improvement.setAllowedClasses(new ArrayList<CharacterClass>(List.of(CharacterClass.Mage)));
        improvement.setProfessions(new ArrayList<Profession>(List.of(Profession.Alchemy)));
        improvement.setConflictingBuffs(new ArrayList<String>(List.of("Probe Group", "Other")));
        improvement.isTargetDebuff = true;
        improvement.isCustom = true;
        buff.improvements.add(improvement);
        list.add(buff);

        Buff sparse = new Buff();
        sparse.name = "Sparse";
        list.add(sparse);
        return list;
    }

    static Stats stats(String spec) {
        Stats stats = new Stats();
        for (String pair : spec.split(",")) {
            String[] kv = pair.split("=");
            Stats.PROPERTY_BY_NAME.get(kv[0]).mutator().set(stats, Float.parseFloat(kv[1]));
        }
        return stats;
    }

    static Path probeFile(String resource) throws Exception {
        return Path.of(BuffSerializationTest.class.getClassLoader().getResource(resource).toURI());
    }
}
