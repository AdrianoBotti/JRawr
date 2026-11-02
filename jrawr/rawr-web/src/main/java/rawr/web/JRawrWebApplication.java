package rawr.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import rawr.data.ItemCostStore;
import rawr.data.SavedItemFilterStore;
import rawr.data.SavedTalentSpecStore;
import rawr.data.UserItemStore;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class JRawrWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(JRawrWebApplication.class, args);
    }

    /**
     * Side-car store for items fetched at runtime (DECISIONS #31/#55). {@code jrawr.items-file}
     * points at the file; left empty it is {@code jrawr.items.xml} next to {@code
     * jrawr.characters-dir}, so both live in the same user data directory.
     */
    @Bean
    UserItemStore userItemStore(
            @Value("${jrawr.items-file:}") String itemsFile,
            @Value("${jrawr.characters-dir:characters}") String charactersDir) {
        Path file;
        if (itemsFile == null || itemsFile.isBlank()) {
            file = Paths.get(charactersDir, UserItemStore.DEFAULT_FILE_NAME);
        } else {
            file = Paths.get(itemsFile);
        }
        return new UserItemStore(file);
    }

    /**
     * Side-car store for user-saved talent specs and the optimizer include/exclude selection
     * (BACKLOG B-003). {@code jrawr.talents-file} points at the file; left empty it is
     * {@code jrawr.talents.xml} next to {@code jrawr.characters-dir}.
     */
    @Bean
    SavedItemFilterStore savedItemFilterStore(
            @Value("${jrawr.item-filters-file:}") String filtersFile,
            @Value("${jrawr.characters-dir:characters}") String charactersDir) {
        Path file = filtersFile == null || filtersFile.isBlank()
                ? Paths.get(charactersDir, SavedItemFilterStore.DEFAULT_FILE_NAME)
                : Paths.get(filtersFile);
        return new SavedItemFilterStore(file);
    }

    @Bean
    ItemCostStore itemCostStore(
            @Value("${jrawr.item-costs-file:}") String costsFile,
            @Value("${jrawr.characters-dir:characters}") String charactersDir) {
        Path file = costsFile == null || costsFile.isBlank()
                ? Paths.get(charactersDir, ItemCostStore.DEFAULT_FILE_NAME)
                : Paths.get(costsFile);
        return new ItemCostStore(file);
    }

    @Bean
    SavedTalentSpecStore savedTalentSpecStore(
            @Value("${jrawr.talents-file:}") String talentsFile,
            @Value("${jrawr.characters-dir:characters}") String charactersDir) {
        Path file;
        if (talentsFile == null || talentsFile.isBlank()) {
            file = Paths.get(charactersDir, SavedTalentSpecStore.DEFAULT_FILE_NAME);
        } else {
            file = Paths.get(talentsFile);
        }
        return new SavedTalentSpecStore(file);
    }
}
