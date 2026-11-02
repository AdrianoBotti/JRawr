// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/StatusMessaging.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.util;

/**
 * Immutable payload of an error published through {@link StatusMessaging}: a {@code key}
 * identifying the failing operation, the {@link Exception} itself for diagnostics, and a
 * user-friendly message suitable for display. The exception may be {@code null} when only a message
 * is available, and the friendly message may be {@code null} when only the exception is.
 */
public record StatusErrorEventArgs(String key, Exception ex, String friendlyMessage) {
}
