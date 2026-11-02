// Java replacement for the .NET KeyValuePair<TKey, TValue> used by the optimizer APIs.
package rawr.base.util;

/**
 * Immutable key/value pair returned by the optimizer's direct-upgrade scans.
 *
 * <p>The original C# code used .NET's {@code KeyValuePair<TKey, TValue>}; this record provides the
 * same two components without mutable public fields. The optimizer uses it to return
 * "item slot -&gt; candidate individual" results without allocating maps.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public record KeyValuePair<K, V>(K key, V value) {
}
