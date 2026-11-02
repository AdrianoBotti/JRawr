// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/StatusMessaging.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the StatusUpdate/StatusError C# events are mapped to listener lists with
// add/removeStatusUpdateListener and add/removeStatusErrorListener.
package rawr.base.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A process-wide notification bus for long-running operations: optimizer and batch jobs report
 * progress through it, and the UI (or a test) subscribes as a listener.
 *
 * <p>There are two independent channels. Status updates ({@link #updateStatus(String, String)} and
 * {@link #updateStatusFinished(String)}) carry a progress message plus a "done" flag; errors
 * ({@link #reportError(String, Exception, String)}) carry the exception and a user-friendly message.
 * Every event has a caller-chosen {@code key} (for example the character or batch item being
 * processed) so subscribers can route messages to the right progress display. The public
 * {@link #ready} flag is a global gate: while it is false all events are dropped silently, which lets
 * callers publish unconditionally before any UI exists.</p>
 *
 * <p>Listeners live in {@code CopyOnWriteArrayList}s, so add/remove are safe from any thread and
 * iteration cannot throw {@code ConcurrentModificationException}; notifications are delivered
 * synchronously on the publishing thread in registration order. The listener lists are static,
 * process-wide state and are never cleared automatically, so long-lived subscribers must remove
 * themselves.</p>
 *
 * <p>The C# static events {@code StatusUpdate}/{@code StatusError} are represented as
 * add/remove-listener method pairs instead.</p>
 */
public final class StatusMessaging {
    public static boolean ready = false;

    @FunctionalInterface
    public interface StatusErrorDelegate {
        void invoke(StatusErrorEventArgs args);
    }

    @FunctionalInterface
    public interface StatusUpdateDelegate {
        void invoke(StatusEventArgs args);
    }

    private static final List<StatusErrorDelegate> statusErrorListeners = new CopyOnWriteArrayList<StatusErrorDelegate>();
    private static final List<StatusUpdateDelegate> statusUpdateListeners = new CopyOnWriteArrayList<StatusUpdateDelegate>();

    private StatusMessaging() {
    }

    public static void addStatusErrorListener(StatusErrorDelegate listener) {
        statusErrorListeners.add(listener);
    }

    public static void removeStatusErrorListener(StatusErrorDelegate listener) {
        statusErrorListeners.remove(listener);
    }

    public static void addStatusUpdateListener(StatusUpdateDelegate listener) {
        statusUpdateListeners.add(listener);
    }

    public static void removeStatusUpdateListener(StatusUpdateDelegate listener) {
        statusUpdateListeners.remove(listener);
    }

    /**
     * Publishes a progress message; the event is marked not-done and is dropped while
     * {@link #ready} is false.
     */
    public static void updateStatus(String key, String description) {
        if (ready) {
            for (StatusUpdateDelegate del : statusUpdateListeners) {
                del.invoke(new StatusEventArgs(key, description));
            }
        }
    }

    /**
     * Publishes an error with both the exception and a friendly message; dropped while
     * {@link #ready} is false.
     */
    public static void reportError(String key, Exception ex, String friendlyMessage) {
        if (ready) {
            for (StatusErrorDelegate del : statusErrorListeners) {
                del.invoke(new StatusErrorEventArgs(key, ex, friendlyMessage));
            }
        }
    }

    /**
     * Convenience completion event: publishes the fixed description "Done!" with the done flag set.
     */
    public static void updateStatusFinished(String key) {
        if (ready) {
            for (StatusUpdateDelegate del : statusUpdateListeners) {
                del.invoke(new StatusEventArgs(key, "Done!", true));
            }
        }
    }
}
