package rawr.golden;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-integrity gate for every committed character fixture: all equipped item, gem and enchant
 * ids must resolve from the offline caches, and every model fixture directory must be present.
 * This is separate from the numeric goldens; it catches missing/renamed data and fixture drift.
 */
class AllFixturesIntegrityTest {
    private static Path repoRoot;
    private static Path fixturesDir;

    @BeforeAll
    static void setUp() throws Exception {
        repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        fixturesDir = repoRoot.resolve("oracle/fixtures");
        Path gameData = repoRoot.resolve("jrawr/data");
        ItemCache.clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        Enchant.getAllEnchants().clear();
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
    }

    @Test
    void everyFixtureResolvesAllItemsGemsAndEnchants() throws Exception {
        List<Path> fixtureDirs;
        try (Stream<Path> paths = Files.list(fixturesDir)) {
            fixtureDirs = paths.filter(Files::isDirectory).sorted().toList();
        }
        assertThat(fixtureDirs).hasSize(20);

        int fixtures = 0;
        int items = 0;
        int gems = 0;
        int enchants = 0;
        for (Path dir : fixtureDirs) {
            List<Path> files;
            try (Stream<Path> paths = Files.list(dir)) {
                files = paths.filter(path -> path.toString().endsWith(".xml")).sorted().toList();
            }
            assertThat(files).as("%s fixtures", dir.getFileName()).isNotEmpty();
            for (Path file : files) {
                Character character = CharacterFixtureLoader.load(file);
                assertThat(character).as("%s", file).isNotNull();
                for (int i = 0; i < Character.SlotCount; i++) {
                    CharacterSlot slot = CharacterSlot.fromValue(i);
                    ItemInstance instance = character.get(slot);
                    if (instance == null) {
                        continue;
                    }
                    Item item = instance.getItem();
                    assertThat(item).as("%s %s item", file, slot).isNotNull();
                    assertThat(item.getId()).as("%s %s item id", file, slot).isPositive();
                    assertThat(ItemCache.findItemById(item.getId()))
                            .as("%s %s item %s in cache", file, slot, item.getId()).isNotNull();
                    items++;

                    for (int socket = 1; socket <= 3; socket++) {
                        int gemId = socket == 1 ? instance.getGem1Id()
                                : socket == 2 ? instance.getGem2Id() : instance.getGem3Id();
                        if (gemId == 0) {
                            continue;
                        }
                        Item gem = instance.getGem(socket);
                        assertThat(gem).as("%s %s socket %d gem", file, slot, socket).isNotNull();
                        assertThat(gem.isGem()).as("%s %s socket %d %s is a gem",
                                file, slot, socket, gem.getId()).isTrue();
                        assertThat(ItemCache.findItemById(gem.getId()))
                                .as("%s %s socket %d gem %s in cache", file, slot, socket, gem.getId())
                                .isNotNull();
                        gems++;
                    }

                    if (instance.getEnchantId() != 0) {
                        Enchant enchant = Enchant.getAllEnchants().stream()
                                .filter(candidate -> candidate.id == instance.getEnchantId())
                                .findFirst().orElse(null);
                        assertThat(enchant).as("%s %s enchant %s resolves", file, slot,
                                instance.getEnchantId()).isNotNull();
                        boolean fits = enchant.fitsInSlot(item.getSlot())
                                || (enchant.slot == rawr.base.items.ItemSlot.TwoHand
                                && item.getSlot() == rawr.base.items.ItemSlot.OneHand);
                        assertThat(fits).as("%s %s enchant %s fits %s", file, slot,
                                instance.getEnchantId(), item.getSlot()).isTrue();
                        enchants++;
                    }
                }
                fixtures++;
            }
        }
        assertThat(fixtures).isGreaterThanOrEqualTo(100);
        assertThat(items).isGreaterThan(fixtures * 10);
        assertThat(gems).isPositive();
        assertThat(enchants).isPositive();
    }
}
