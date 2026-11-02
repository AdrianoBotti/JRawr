package rawr.data;

import rawr.base.items.ItemFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Side-car for user edits to the global item filter tree and per-model item types. */
public final class SavedItemFilterStore {
    public static final String DEFAULT_FILE_NAME = "jrawr.item-filters.xml";

    private final Path file;

    public SavedItemFilterStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    public Path getFile() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /** Loads the side-car over the base filter data; a missing file leaves the base in place. */
    public synchronized boolean load() {
        if (!exists()) {
            return false;
        }
        ItemFilter.load(file);
        return true;
    }

    public synchronized void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent == null ? Path.of(".") : parent,
                    "jrawr-item-filters-", ".xml");
            try {
                ItemFilter.save(temp);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicFailure) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save item filters to " + file, e);
        }
    }
}
