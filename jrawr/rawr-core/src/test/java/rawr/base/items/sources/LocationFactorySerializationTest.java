package rawr.base.items.sources;

import rawr.base.util.XmlCanon;

import rawr.base.character.ReputationLevel;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ContainerItem;
import rawr.base.items.sources.CraftedItem;
import rawr.base.items.sources.FactionItem;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.sources.ItemLocationDictionary;
import rawr.base.items.sources.ItemSource;
import rawr.base.items.sources.LocationFactory;
import rawr.base.items.sources.NoSource;
import rawr.base.items.sources.PvpItem;
import rawr.base.items.sources.QuestItem;
import rawr.base.items.sources.StaticDrop;
import rawr.base.items.sources.UnknownItem;
import rawr.base.items.sources.VendorItem;
import rawr.base.items.sources.WorldDrop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LocationFactory save/load: the writer must emit the original XmlSerializer ItemLocationDictionary
 * schema (compared canonically against {@code tools/core-format-probe} output) and the reader must
 * rebuild the same location table so that read -> write is stable.
 */
class LocationFactorySerializationTest {

    @TempDir
    Path tempDir;

    private LinkedHashMap<String, ItemLocation[]> previousTable;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void snapshot() throws Exception {
        Field field = LocationFactory.class.getDeclaredField("ALL_LOCATIONS");
        field.setAccessible(true);
        previousTable = new LinkedHashMap<String, ItemLocation[]>((Map<String, ItemLocation[]>) field.get(null));
    }

    @AfterEach
    @SuppressWarnings("unchecked")
    void restore() throws Exception {
        Field field = LocationFactory.class.getDeclaredField("ALL_LOCATIONS");
        field.setAccessible(true);
        Map<String, ItemLocation[]> table = (Map<String, ItemLocation[]>) field.get(null);
        table.clear();
        table.putAll(previousTable);
    }

    @Test
    void writerMatchesTheLegacyXmlSerializerSchema() throws Exception {
        LocationFactory.clear();
        for (Map.Entry<String, ItemLocation[]> entry : probeTable().entrySet()) {
            LocationFactory.add(entry.getKey(), entry.getValue(), true);
        }
        Path out = tempDir.resolve("ItemSource.xml");
        LocationFactory.save(out);

        assertThat(XmlCanon.canonical(out))
                .isEqualTo(XmlCanon.canonical(probeFile("core-format/item-source.xml")));
    }

    @Test
    void loadRestoresEveryLocationTypeAndRoundTrips() throws Exception {
        Path oracle = probeFile("core-format/item-source.xml");
        LocationFactory.load(oracle);

        ItemLocation[] vendor = LocationFactory.lookup(1);
        assertThat(vendor[0]).isInstanceOf(VendorItem.class);
        assertThat(vendor[0].getNote()).isEqualTo("Probe Note");
        assertThat(((VendorItem) vendor[0]).getCost()).isEqualTo(12345);
        assertThat(((VendorItem) vendor[0]).getTokenMap()).containsEntry("Probe Token", 10);
        assertThat(vendor[1]).isNull();

        ItemLocation[] pair = LocationFactory.lookup("2");
        assertThat(pair[0]).isInstanceOf(StaticDrop.class);
        assertThat(((StaticDrop) pair[0]).getBoss()).isEqualTo("Auriaya");
        assertThat(((StaticDrop) pair[0]).isHeroic()).isTrue();
        assertThat(pair[1]).isInstanceOf(WorldDrop.class);
        assertThat(((WorldDrop) pair[1]).getLocation()).isEqualTo("Wintergrasp");

        CraftedItem crafted = (CraftedItem) LocationFactory.lookup(3)[0];
        assertThat(crafted.getSkill()).isEqualTo("Blacksmithing");
        assertThat(crafted.getLevel()).isEqualTo(450);
        assertThat(crafted.getBind()).isEqualTo(BindsOn.BoP);
        assertThat(crafted.getSpellName()).isEqualTo("Probe Spell");
        assertThat(crafted.getBopMats()).isEmpty();

        QuestItem quest = (QuestItem) LocationFactory.lookup(4)[0];
        assertThat(quest.getQuest()).isEqualTo("Probe Quest");
        assertThat(quest.getMinLevel()).isEqualTo(80);
        assertThat(quest.getParty()).isEqualTo(3);
        assertThat(quest.getType()).isEqualTo("g");

        PvpItem pvp = (PvpItem) LocationFactory.lookup(5)[0];
        assertThat(pvp.getPointType()).isEqualTo("Honor");
        assertThat(pvp.getPointsA()).isEqualTo(50);
        assertThat(pvp.getRequiredArenaRating()).isEqualTo(1500);

        FactionItem faction = (FactionItem) LocationFactory.lookup(6)[0];
        assertThat(faction.getFactionName()).isEqualTo("Probe Faction");
        assertThat(faction.getLevel()).isEqualTo(ReputationLevel.Exalted);
        assertThat(faction.getTokenMap()).containsEntry("Probe Faction Token", 1);

        ContainerItem container = (ContainerItem) LocationFactory.lookup(7)[0];
        assertThat(container.isHeroic()).isTrue();
        assertThat(container.getContainer()).isEqualTo("Cache of Living Stone");
        assertThat(container.getParty()).isEqualTo(2);

        assertThat(LocationFactory.lookup(8)[0]).isInstanceOf(NoSource.class);
        assertThat(LocationFactory.lookup(8)[1]).isInstanceOf(UnknownItem.class);
        assertThat(LocationFactory.lookup(9)[0].getClass()).isEqualTo(ItemLocation.class);
        assertThat(LocationFactory.lookup(9)[0].getNote()).isEqualTo("Plain Note");

        assertThat(LocationFactory.lookup(424242)[0].getDescription())
                .isEqualTo("Unknown Location, please refresh");

        Path out = tempDir.resolve("ItemSource-roundtrip.xml");
        LocationFactory.save(out);
        assertThat(XmlCanon.canonical(out)).isEqualTo(XmlCanon.canonical(oracle));
    }

    @Test
    void loadWithoutAFileLeavesTheTableEmpty() throws Exception {
        LocationFactory.clear();
        LocationFactory.load(tempDir.resolve("no-such-ItemSource.xml"));
        assertThat(LocationFactory.lookup(1)[0].getDescription())
                .isEqualTo("Unknown Location, please refresh");
    }

    static LinkedHashMap<String, ItemLocation[]> probeTable() {
        LinkedHashMap<String, ItemLocation[]> table = new LinkedHashMap<String, ItemLocation[]>();
        VendorItem vendor = new VendorItem();
        vendor.getTokenMap().put("Probe Token", 10);
        vendor.setCost(12345);
        vendor.setVendorName("Probe Vendor");
        vendor.setVendorArea("Dalaran");
        vendor.setNote("Probe Note");
        table.put("1", new ItemLocation[] { vendor, null });

        StaticDrop drop = new StaticDrop();
        drop.setArea("Ulduar (25)");
        drop.setHeroic(true);
        drop.setBoss("Auriaya");
        WorldDrop world = new WorldDrop();
        world.setLocation("Wintergrasp");
        table.put("2", new ItemLocation[] { drop, world });

        CraftedItem crafted = (CraftedItem) CraftedItem.construct();
        crafted.setSkill("Blacksmithing");
        crafted.setLevel(450);
        crafted.setBind(BindsOn.BoP);
        crafted.setSpellName("Probe Spell");
        crafted.getBopMats().put("Probe Mat", 4);
        table.put("3", new ItemLocation[] { crafted, null });

        QuestItem quest = (QuestItem) QuestItem.construct();
        quest.setArea("Icecrown");
        quest.setQuest("Probe Quest");
        quest.setMinLevel(80);
        quest.setParty(3);
        quest.setType("g");
        table.put("4", new ItemLocation[] { quest, null });

        PvpItem pvp = new PvpItem();
        pvp.setPointType("Honor");
        pvp.setPoints(100);
        pvp.setTokenType("Mark");
        pvp.setTokenCount(2);
        pvp.setPointTypeA("Arena");
        pvp.setPointsA(50);
        pvp.setRequiredArenaRating(1500);
        pvp.setVendorName("Probe PvP");
        pvp.setVendorArea("Dalaran");
        table.put("5", new ItemLocation[] { pvp, null });

        FactionItem faction = new FactionItem();
        faction.setFactionName("Probe Faction");
        faction.setLevel(ReputationLevel.Exalted);
        faction.setCost(55000);
        faction.getTokenMap().put("Probe Faction Token", 1);
        table.put("6", new ItemLocation[] { faction, null });

        ContainerItem container = new ContainerItem();
        container.setHeroic(true);
        container.setArea("Ulduar (10)");
        container.setContainer("Cache of Living Stone");
        container.setMinLevel(75);
        container.setParty(2);
        table.put("7", new ItemLocation[] { container, null });

        table.put("8", new ItemLocation[] { new NoSource(), new UnknownItem() });

        ItemLocation plain = new ItemLocation();
        plain.setNote("Plain Note");
        table.put("9", new ItemLocation[] { plain, null });
        return table;
    }

    static Path probeFile(String resource) throws Exception {
        return Path.of(LocationFactorySerializationTest.class.getClassLoader().getResource(resource).toURI());
    }
}
