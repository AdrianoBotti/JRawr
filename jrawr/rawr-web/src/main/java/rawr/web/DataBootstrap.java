package rawr.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemFilter;
import rawr.base.items.sources.LocationFactory;
import rawr.data.BuffLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.data.ItemCostStore;
import rawr.data.SavedItemFilterStore;
import rawr.data.ItemSourceLoader;
import rawr.data.SavedTalentSpecStore;
import rawr.data.UserItemStore;
import rawr.models.bear.CalculationsBear;
import rawr.models.cat.CalculationsCat;
import rawr.models.elemental.CalculationsElemental;
import rawr.models.enhance.CalculationsEnhance;
import rawr.models.retribution.CalculationsRetribution;
import rawr.models.protpally.CalculationsProtPaladin;
import rawr.models.healadin.CalculationsHealadin;
import rawr.models.healpriest.CalculationsHealPriest;
import rawr.models.hunter.CalculationsHunter;
import rawr.models.rogue.CalculationsRogue;
import rawr.models.shadowpriest.CalculationsShadowPriest;
import rawr.models.mage.CalculationsMage;
import rawr.models.moonkin.CalculationsMoonkin;
import rawr.models.protwarr.CalculationsProtWarr;
import rawr.models.restosham.CalculationsRestoSham;
import rawr.models.tankdk.CalculationsTankDK;
import rawr.models.tree.CalculationsTree;
import rawr.models.dpsdk.CalculationsDPSDK;
import rawr.models.dpswarr.CalculationsDPSWarr;
import rawr.models.warlock.CalculationsWarlock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Loads the original game-data caches (item/buff/enchant) and registers the ported models at
 * startup. Fails fast with a clear message when the data directory or one of the cache files is
 * missing. The directory is configurable through {@code jrawr.data-dir}.
 */
@Component
public class DataBootstrap implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DataBootstrap.class);
    private static final List<String> CACHE_FILES =
            List.of("ItemCache.xml", "BuffCache.xml", "EnchantCache.xml");

    private final Path dataDir;
    private final UserItemStore userItemStore;
    private final SavedTalentSpecStore talentSpecStore;
    private final SavedItemFilterStore itemFilterStore;
    private final ItemCostStore itemCostStore;
    private boolean loaded;

    @org.springframework.beans.factory.annotation.Autowired
    public DataBootstrap(
            @Value("${jrawr.data-dir:data}") String dataDir,
            UserItemStore userItemStore,
            SavedTalentSpecStore talentSpecStore,
            SavedItemFilterStore itemFilterStore,
            ItemCostStore itemCostStore) {
        this.dataDir = Paths.get(dataDir).toAbsolutePath().normalize();
        this.userItemStore = userItemStore;
        this.talentSpecStore = talentSpecStore;
        this.itemFilterStore = itemFilterStore;
        this.itemCostStore = itemCostStore;
    }

    /** Convenience constructor for tests that do not exercise the saved side-cars. */
    DataBootstrap(String dataDir, UserItemStore userItemStore) {
        this(dataDir, userItemStore,
                new SavedTalentSpecStore(Paths.get(dataDir, "jrawr.talents.xml")),
                new SavedItemFilterStore(Paths.get(dataDir, SavedItemFilterStore.DEFAULT_FILE_NAME)),
                new ItemCostStore(Paths.get(dataDir, ItemCostStore.DEFAULT_FILE_NAME)));
    }

    @Override
    public void run(ApplicationArguments args) {
        load();
    }

    public synchronized void load() {
        if (loaded) {
            return;
        }
        failFastIfMissing();
        try {
            ItemCache.clear();
            Enchant.getAllEnchants().clear();
            ItemCacheLoader.load(dataDir.resolve("ItemCache.xml"));
            BuffLoader.load(dataDir.resolve("BuffCache.xml"));
            EnchantLoader.load(dataDir.resolve("EnchantCache.xml"));
            // Optional (legacy File.Exists check): without it items just report "Unknown Location".
            Path itemSource = dataDir.resolve("ItemSource.xml");
            LocationFactory.clear();
            if (Files.isRegularFile(itemSource)) {
                ItemSourceLoader.load(itemSource);
            }
            // C# ItemCache.Load() loads Data/ItemFilter.xml right after ItemSource.xml. The saved
            // per-character filter overrides (Character.loadItemFilterEnabledOverride) apply to this
            // state when a character is opened.
            Path itemFilter = dataDir.resolve("ItemFilter.xml");
            if (Files.isRegularFile(itemFilter)) {
                ItemFilter.load(itemFilter);
            }
            itemFilterStore.load();
            // Optional saved talent specs (the original app's Talents.xml). The shipped list is
            // loaded first, then the user side-car (saved specs + optimizer include/exclude) is
            // appended on top of it. The optimizer reads the merged list per class; without the
            // file only the current spec can be optimized/mutated.
            Path talents = dataDir.resolve("Talents.xml");
            talentSpecStore.load(Files.isRegularFile(talents) ? talents : null);
            // User-fetched items live in the side-car (never in the legacy cache); load them after
            // the legacy data so their locations win over the shipped ItemSource.xml entries.
            userItemStore.load();
            itemCostStore.load();
            CalculationsMage.register();
            CalculationsRestoSham.register();
            CalculationsCat.register();
            CalculationsBear.register();
            CalculationsMoonkin.register();
            CalculationsElemental.register();
            CalculationsEnhance.register();
            CalculationsHealadin.register();
            CalculationsHealPriest.register();
            CalculationsShadowPriest.register();
            CalculationsHunter.register();
            CalculationsProtWarr.register();
            CalculationsRetribution.register();
            CalculationsProtPaladin.register();
            CalculationsRogue.register();
            CalculationsTankDK.register();
            CalculationsTree.register();
            CalculationsWarlock.register();
            CalculationsDPSDK.register();
            CalculationsDPSWarr.register();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JRawr game data from " + dataDir, e);
        }
        loaded = true;
        LOG.info("Loaded game data from {}; registered models Mage, RestoSham, Cat, Bear, Moonkin, Elemental, Enhance, Healadin, HealPriest, ShadowPriest, ProtWarr, Retribution, ProtPaladin, Hunter, Rogue, Tree, TankDK, Warlock, DPSDK, DPSWarr", dataDir);
    }

    /** Cheap safety net: a save after Add-by-ID can only be retried when it failed earlier. */
    @PreDestroy
    public void saveUserItems() {
        userItemStore.save();
    }

    private void failFastIfMissing() {
        if (!Files.isDirectory(dataDir)) {
            throw new IllegalStateException("JRawr data directory not found: " + dataDir
                    + " (configure jrawr.data-dir; default data)");
        }
        for (String name : CACHE_FILES) {
            Path file = dataDir.resolve(name);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("JRawr data file not found: " + file
                        + " (configure jrawr.data-dir; default data)");
            }
        }
    }
}
