// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Log.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Show (WinForms MessageBox) omitted; Write format uses ISO-like date instead of ToShortDateString.
package rawr.base.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The engine's tiny diagnostic log: it appends timestamped lines to {@code Rawrlog.txt} in the
 * current working directory.
 *
 * <p>Writes go through a dedicated {@link java.util.logging.Logger} whose handler opens the file in
 * append mode for each record and closes it again, so no file handle is kept open and the original
 * one-line-per-call behavior is preserved. The line starts with a
 * {@code yyyy-MM-dd H:mm:ss.SSS} timestamp followed by a colon and the message. Input/output
 * failures are swallowed, exactly as in the C# original, so logging can never take a calculation
 * down; an unwritable directory simply means no log line. The WinForms {@code Show} method (a
 * message box) is omitted, which is why this port only offers {@link #write(String)}.</p>
 */
public final class Log {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss.SSS");
    private static final Logger LOGGER = Logger.getLogger("rawr.base.Log");

    static {
        LOGGER.setUseParentHandlers(false);
    }

    private Log() {
    }

    public static void write(String log) {
        write(Path.of("Rawrlog.txt"), log);
    }

    /** Package-private path variant used by tests; the public API always writes Rawrlog.txt. */
    static void write(Path path, String log) {
        synchronized (LOGGER) {
            RawrFileHandler handler = new RawrFileHandler(path);
            LOGGER.addHandler(handler);
            try {
                LOGGER.log(Level.INFO, log);
            } finally {
                LOGGER.removeHandler(handler);
                handler.close();
            }
        }
    }

    private static final class RawrFileHandler extends Handler {
        private final Path path;

        RawrFileHandler(Path path) {
            this.path = path;
        }

        @Override
        public void publish(LogRecord record) {
            try (FileWriter writer = new FileWriter(path.toFile(), true)) {
                writer.write(FORMAT.format(LocalDateTime.now()) + ": " + record.getMessage()
                        + System.lineSeparator());
            } catch (IOException e) {
                // original swallows write failures as well
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
