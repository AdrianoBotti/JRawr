package rawr.base.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the Rawrlog.txt line format and append behavior after moving Log onto java.util.logging. */
class LogTest {

    @Test
    void appendsOneTimestampedLinePerWrite(@TempDir Path directory) throws Exception {
        Path log = directory.resolve("Rawrlog.txt");
        Log.write(log, "first");
        Log.write(log, "second");

        List<String> lines = Files.readAllLines(log);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).matches("\\d{4}-\\d{2}-\\d{2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}: first");
        assertThat(lines.get(1)).matches("\\d{4}-\\d{2}-\\d{2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}: second");
    }

    @Test
    void unwritablePathIsSilentlyIgnored(@TempDir Path directory) {
        Path missingParent = directory.resolve("missing").resolve("Rawrlog.txt");
        Log.write(missingParent, "ignored");
        assertThat(Files.exists(missingParent)).isFalse();
    }
}
