package rawr.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rawr.base.character.Character;
import rawr.data.CharacterFixtureLoader;
import rawr.data.ItemCostStore;
import rawr.data.SavedItemFilterStore;
import rawr.data.SavedTalentSpecStore;
import rawr.data.UserItemStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persists editor characters as original Rawr character XML (the format {@link Character#save(Path)}
 * writes and {@link CharacterFixtureLoader} reads). Files live in {@code jrawr.characters-dir}
 * (default {@code characters} relative to the working directory), created on demand.
 */
@Service
public class CharacterStore {
    private static final Logger LOG = LoggerFactory.getLogger(CharacterStore.class);

    /** App-owned side-cars that live next to the characters and are not character files. */
    private static final Set<String> SIDE_CARS = Set.of(
            SavedItemFilterStore.DEFAULT_FILE_NAME,
            ItemCostStore.DEFAULT_FILE_NAME,
            SavedTalentSpecStore.DEFAULT_FILE_NAME,
            UserItemStore.DEFAULT_FILE_NAME);

    private final Path directory;

    public CharacterStore(@Value("${jrawr.characters-dir:characters}") String charactersDir) {
        this.directory = Paths.get(charactersDir).toAbsolutePath().normalize();
    }

    /** Resolved storage directory (exposed for diagnostics/tests). */
    public Path getDirectory() {
        return directory;
    }

    /** Default Save As input value: the sanitized character name, without the {@code .xml} suffix. */
    public String defaultName(Character character) {
        return sanitizeBaseName(character.getName());
    }

    /**
     * Writes the character to exactly the given file name (overwriting it when it exists). The
     * caller is responsible for the name already having passed {@link #sanitizeSaveAsName(String)}.
     */
    public String write(String fileName, Character character, String modelName) throws IOException {
        if (modelName != null && !modelName.isBlank()
                && (character.getCurrentModel() == null || character.getCurrentModel().isBlank())) {
            character.setCurrentModel(modelName);
        }
        Files.createDirectories(directory);
        character.save(resolve(fileName));
        return fileName;
    }

    /**
     * Sanitizes a user-entered Save As name: strips any directory parts and illegal path characters
     * and appends {@code .xml}. Returns {@code null} when nothing usable remains.
     */
    public static String sanitizeSaveAsName(String requested) {
        if (requested == null) {
            return null;
        }
        String trimmed = requested.trim();
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        String base = sanitizeBaseNameOrEmpty(trimmed);
        return base.isEmpty() ? null : base + ".xml";
    }

    /** Strips a trailing {@code .xml} from a raw Save As input, for echoing back into the form. */
    public static String saveAsInputBase(String requested) {
        if (requested == null) {
            return "";
        }
        String trimmed = requested.trim();
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed;
    }

    /** Saved character file names, sorted case-insensitively; empty when nothing is stored yet. */
    public List<String> list() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".xml"))
                    .filter(name -> !SIDE_CARS.contains(name.toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Cannot list saved characters in {}", directory, e);
            return List.of();
        }
    }

    /** Loads a saved character through the shared fixture loader. */
    public Character load(String fileName) throws Exception {
        return CharacterFixtureLoader.load(resolve(fileName));
    }

    /** Deletes a saved character; returns whether the file existed. */
    public boolean delete(String fileName) throws IOException {
        return Files.deleteIfExists(resolve(fileName));
    }

    public boolean exists(String fileName) {
        return Files.isRegularFile(resolve(fileName));
    }

    private Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Missing character file name");
        }
        Path name = Paths.get(fileName).getFileName();
        if (name == null) {
            throw new IllegalArgumentException("Invalid character file name: " + fileName);
        }
        Path resolved = directory.resolve(name.toString()).normalize();
        if (!resolved.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid character file name: " + fileName);
        }
        return resolved;
    }

    static String sanitizeBaseName(String name) {
        String base = sanitizeBaseNameOrEmpty(name);
        return base.isEmpty() ? "character" : base;
    }

    private static String sanitizeBaseNameOrEmpty(String name) {
        String trimmed = name == null ? "" : name.trim();
        StringBuilder sanitized = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (java.lang.Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sanitized.append(c);
            } else if (c == ' ' || c == '.') {
                sanitized.append('-');
            }
        }
        return sanitized.toString().replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
    }
}
