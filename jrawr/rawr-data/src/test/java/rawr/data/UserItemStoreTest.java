package rawr.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemDamageType;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.ItemSlot;
import rawr.base.items.sources.LocationFactory;
import rawr.base.effects.SpecialEffect;
import rawr.base.items.sources.StaticDrop;
import rawr.base.stats.Stats;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline round-trip tests for the user-item side-car: an item with stats, special effects, sockets
 * and a source is written with the legacy ItemCache.xml schema, read back by the existing
 * {@link ItemCacheLoader} and by {@link UserItemStore#load()}; every Stats float, effect field and
 * item field must match bit-exactly. Corrupt files are ignored and repairable.
 */
class UserItemStoreTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void clearCaches() {
        ItemCache.clear();
        LocationFactory.clear();
    }

    @Test
    void roundTripsStatsEffectsSocketsAndSourceBitExactly() throws Exception {
        Item parsed = parse("payloads/aowow_trinket_99010.xml");
        Item hood = parse("payloads/aowow_hood_51281.xml");
        // One item carrying every feature: trinket stats/effects/source + hood sockets/socket bonus.
        parsed.setSocketColor1(hood.getSocketColor1());
        parsed.setSocketColor2(hood.getSocketColor2());
        parsed.setSocketColor3(hood.getSocketColor3());
        parsed.setSocketBonus(hood.getSocketBonus());
        parsed.setUnique(true);
        parsed.setUniqueId(new ArrayList<Integer>(List.of(99010, 99011)));
        parsed.setRequiredClasses("Mage");

        Path file = tempDir.resolve("jrawr.items.xml");
        UserItemStore store = new UserItemStore(file);
        store.add(parsed);
        assertThat(file).exists();

        // The written file is readable by the existing ItemCacheLoader, stats bit-exact.
        ItemCache.clear();
        ItemCacheLoader.load(file);
        Item fromLoader = ItemCache.findItemById(99010);
        assertThat(fromLoader).as("item loaded by the legacy loader").isNotNull();
        assertStatsBitExact(parsed.getStats(), fromLoader.getStats());

        // Full restart: fresh statics, load through the store (as DataBootstrap does).
        ItemCache.clear();
        LocationFactory.clear();
        UserItemStore reloaded = new UserItemStore(file);
        assertThat(reloaded.load()).isEqualTo(1);

        Item restored = ItemCache.findItemById(99010);
        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("JRawr Test Spyglass");
        assertThat(restored.getItemLevel()).isEqualTo(parsed.getItemLevel());
        assertThat(restored.getDisplayId()).isEqualTo(parsed.getDisplayId());
        assertThat(restored.getDisplaySlot()).isEqualTo(parsed.getDisplaySlot());
        assertThat(restored.getIconPath()).isEqualTo(parsed.getIconPath());
        assertThat(restored.getQuality()).isEqualTo(parsed.getQuality());
        assertThat(restored.getType()).isEqualTo(parsed.getType());
        assertThat(restored.getFaction()).isEqualTo(parsed.getFaction());
        assertThat(restored.getSetName()).isEqualTo(parsed.getSetName());
        assertThat(restored.getMinDamage()).isEqualTo(parsed.getMinDamage());
        assertThat(restored.getMaxDamage()).isEqualTo(parsed.getMaxDamage());
        assertThat(restored.getDamageType()).isEqualTo(ItemDamageType.Physical);
        assertThat(Float.floatToRawIntBits(restored.getSpeed()))
                .isEqualTo(Float.floatToRawIntBits(parsed.getSpeed()));
        assertThat(restored.getRequiredClasses()).isEqualTo("Mage");
        assertThat(restored.isUnique()).isTrue();
        assertThat(restored.getUniqueId()).containsExactly(99010, 99011);
        assertThat(restored.getBind()).isEqualTo(parsed.getBind());
        assertThat(restored.getSocketColor1()).isEqualTo(ItemSlot.Meta);
        assertThat(restored.getSocketColor2()).isEqualTo(ItemSlot.Blue);
        assertThat(restored.getSocketColor3()).isEqualTo(ItemSlot.None);
        assertThat(restored.getSlot()).isEqualTo(ItemSlot.Trinket);
        assertStatsBitExact(parsed.getSocketBonus(), restored.getSocketBonus());
        assertStatsBitExact(parsed.getStats(), restored.getStats());

        // Source: the StaticDrop registered by ItemXmlParser survives through the ItemSource part.
        ItemLocation location = restored.getLocationInfo()[0];
        assertThat(location).isInstanceOf(StaticDrop.class);
        assertThat(((StaticDrop) location).getBoss()).isEqualTo("JRawr Test Spyglass");
        assertThat(((StaticDrop) location).getArea()).isEqualTo("Icecrown Citadel");
    }

    @Test
    void writesOnlyRegisteredItemsNeverTheLegacyCache() throws Exception {
        Item legacy = new Item();
        legacy.setId(1);
        legacy.setName("Mithril Shirt");
        legacy.setSlot(ItemSlot.Shirt);
        ItemCache.addItem(legacy);

        Item first = parse("payloads/aowow_hood_51281.xml");
        Path file = tempDir.resolve("jrawr.items.xml");
        UserItemStore store = new UserItemStore(file);
        store.add(first);

        String written = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(written).contains("<Name>Sanctified Bloodmage Hood</Name>")
                .contains("<ArrayOfItem xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        assertThat(written).doesNotContain("Mithril Shirt");
        assertThat(ItemCache.findItemById(1)).as("legacy item untouched").isNotNull();
    }

    @Test
    void corruptFileIsIgnoredAndRepairedByTheNextAdd() throws Exception {
        Path file = tempDir.resolve("jrawr.items.xml");
        Files.writeString(file, "<ArrayOfItem><Item><Name>truncated");

        UserItemStore store = new UserItemStore(file);
        assertThat(store.load()).as("corrupt file ignored").isZero();

        Item item = parse("payloads/aowow_hood_51281.xml");
        store.add(item);
        assertThat(Files.readString(file, StandardCharsets.UTF_8))
                .contains("<Name>Sanctified Bloodmage Hood</Name>");

        ItemCache.clear();
        LocationFactory.clear();
        assertThat(new UserItemStore(file).load()).isEqualTo(1);
        assertThat(ItemCache.findItemById(51281)).isNotNull();
    }

    @Test
    void listsContainsAndRemovesRegisteredItems() throws Exception {
        Item hood = parse("payloads/aowow_hood_51281.xml");
        Item trinket = parse("payloads/aowow_trinket_99010.xml");
        Path file = tempDir.resolve("jrawr.items.xml");
        UserItemStore store = new UserItemStore(file);
        store.add(hood);
        store.add(trinket);

        assertThat(store.contains(hood.getId())).isTrue();
        assertThat(store.contains(12345)).isFalse();
        assertThat(store.items()).extracting(Item::getId)
                .containsExactly(hood.getId(), trinket.getId());

        assertThat(store.remove(hood.getId())).isTrue();
        assertThat(store.contains(hood.getId())).isFalse();
        assertThat(store.items()).extracting(Item::getId).containsExactly(trinket.getId());
        assertThat(Files.readString(file, StandardCharsets.UTF_8))
                .doesNotContain("Sanctified Bloodmage Hood");
        assertThat(store.remove(hood.getId())).as("second remove is a no-op").isFalse();

        ItemCache.clear();
        LocationFactory.clear();
        assertThat(new UserItemStore(file).load()).isEqualTo(1);
        assertThat(ItemCache.findItemById(trinket.getId())).isNotNull();
        assertThat(ItemCache.findItemById(hood.getId())).isNull();
    }

    // ------------------------------------------------------------------ helpers

    static void assertStatsBitExact(Stats expected, Stats actual) {
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            float e = property.accessor().get(expected);
            float a = property.accessor().get(actual);
            assertThat(Float.floatToRawIntBits(a))
                    .as("stat %s (expected %s, actual %s)", property.name(), e, a)
                    .isEqualTo(Float.floatToRawIntBits(e));
        }
        List<SpecialEffect> expectedEffects = list(expected.specialEffects());
        List<SpecialEffect> actualEffects = list(actual.specialEffects());
        assertThat(actualEffects).hasSameSizeAs(expectedEffects);
        for (int i = 0; i < expectedEffects.size(); i++) {
            SpecialEffect e = expectedEffects.get(i);
            SpecialEffect a = actualEffects.get(i);
            assertThat(a.getTrigger()).as("effect %d trigger", i).isEqualTo(e.getTrigger());
            assertThat(Float.floatToRawIntBits(a.getDuration()))
                    .isEqualTo(Float.floatToRawIntBits(e.getDuration()));
            assertThat(Float.floatToRawIntBits(a.getCooldown()))
                    .isEqualTo(Float.floatToRawIntBits(e.getCooldown()));
            assertThat(Float.floatToRawIntBits(a.getChance()))
                    .isEqualTo(Float.floatToRawIntBits(e.getChance()));
            assertThat(a.getMaxStack()).isEqualTo(e.getMaxStack());
            if (e.getStats() != null || a.getStats() != null) {
                assertStatsBitExact(e.getStats(), a.getStats());
            }
        }
    }

    private static List<SpecialEffect> list(Iterable<SpecialEffect> effects) {
        List<SpecialEffect> result = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : effects) {
            result.add(effect);
        }
        return result;
    }

    private static Item parse(String resource) throws Exception {
        try (InputStream in = UserItemStoreTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("resource %s", resource).isNotNull();
            return ItemXmlParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
