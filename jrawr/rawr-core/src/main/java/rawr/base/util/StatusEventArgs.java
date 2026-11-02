// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/StatusMessaging.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.util;

/**
 * Immutable payload of a status update published through {@link StatusMessaging}: a {@code key}
 * identifying the operation, a human-readable {@code description} and whether the operation has
 * finished. The two-argument constructor creates a not-finished event (the common progress case);
 * the three-argument constructor sets the flag explicitly and is what
 * {@link StatusMessaging#updateStatusFinished(String)} uses.
 */
public record StatusEventArgs(String key, String description, boolean done) {
    public StatusEventArgs(String key, String description) {
        this(key, description, false);
    }
}
