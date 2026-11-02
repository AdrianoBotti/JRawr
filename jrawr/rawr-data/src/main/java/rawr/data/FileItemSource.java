package rawr.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Item source backed by a directory of recorded payloads ({@code <itemId>.xml}). Offline by
 * design: used by tests and by deployments that pre-download the items they need.
 */
public final class FileItemSource implements ItemSourceProvider {

    private final Path directory;

    public FileItemSource(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public String fetchXml(int itemId) throws IOException {
        Path file = directory.resolve(itemId + ".xml").normalize();
        if (!file.startsWith(directory)) {
            throw new IOException("Invalid item id " + itemId);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("No item payload for " + itemId + " in " + directory);
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
