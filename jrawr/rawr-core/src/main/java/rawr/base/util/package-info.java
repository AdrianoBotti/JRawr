// New documentation (not ported from C#): package-level guide.
/**
 * Cross-cutting helpers: .NET-compatible formatting, logging/status reporting, small collection
 * and reflection helpers.
 *
 * <ul>
 *   <li>{@link rawr.base.util.NetFormat} &mdash; numeric/string formatting that reproduces Mono/.NET
 *       output exactly (fixed decimals, percent with the space Mono inserts, grouped thousands,
 *       default float/double {@code ToString}). Display strings are part of the golden comparison,
 *       so these shims are locked by tests; do not replace them with {@code String.format}.</li>
 *   <li>{@link rawr.base.util.XmlDom} and {@link rawr.base.util.XmlMap} &mdash; the small JDK-DOM
 *       helpers and the legacy dictionary codec shared by the hand-written save/load paths. The
 *       text helpers keep the trimmed and untrimmed variants explicit so serialized output cannot
 *       change accidentally.</li>
 *   <li>{@link rawr.base.util.KeyValuePair} &mdash; the immutable key/value pair returned by the
 *       optimizer's direct-upgrade scans (a Java stand-in for .NET {@code KeyValuePair}).</li>
 *   <li>{@link rawr.base.util.Log}, {@link rawr.base.util.StatusMessaging},
 *       {@link rawr.base.util.StatusEventArgs}, {@link rawr.base.util.StatusErrorEventArgs}
 *       &mdash; the ported logging and user-status bus (previously WinForms status strips; now
 *       listeners).</li>
 *   <li>{@link rawr.base.util.ArrayUtils}, {@link rawr.base.util.Extensions},
 *       {@link rawr.base.util.Utilities} &mdash; array comparison, stat-name display helpers and
 *       miscellaneous conversions used throughout the engine.</li>
 * </ul>
 */
package rawr.base.util;
