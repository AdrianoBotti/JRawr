// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/CalculationCache.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Thread-safe calculation cache: a result is calculated once per equal parameter set and shared.
// The waiting protocol of the original is collapsed into a synchronized section (the delegated
// calculation never re-enters the cache), which is observationally equivalent for callers.
package rawr.base.algorithms;

import java.util.HashMap;
import java.util.Map;

/**
 * A thread-safe memoizer: it wraps a pure calculation and caches its result per equal parameter set,
 * so the same expensive sub-calculation is only performed once per parameter set.
 *
 * <p>Models use this when one evaluation asks for the same derived value repeatedly (for example a
 * rotation solution for a given set of simulator parameters). Parameters are looked up by
 * {@code equals}/{@code hashCode}, so they must implement both consistently; the cached result is
 * returned as-is, never copied.</p>
 *
 * <h2>Semantics and invariants</h2>
 *
 * <ul>
 *   <li>{@link #getResult(Object)} is {@code synchronized}, so on a cold entry the first caller
 *       computes while others wait, and each parameter set is computed at most once concurrently.
 *       The wrapped calculation must not call back into the same cache (the original's wait
 *       protocol had the same restriction, which is why {@link Calculation} has no way to signal
 *       "in progress").</li>
 *   <li>A calculation that returns {@code null} is not remembered: the port performs a plain map
 *       lookup and treats a null value as "not cached", so such a result is recomputed on every
 *       request. A null parameter is rejected.</li>
 *   <li>The cache never evicts and has no size bound; its lifetime is that of the owning object
 *       (models typically keep one per static or per-simulation context).</li>
 *   <li>The backing {@code HashMap} is only read/written by key, never iterated, so its ordering
 *       cannot influence results.</li>
 * </ul>
 */
public class CalculationCache<TParameters, TResult> {

    /**
     * The pure function to memoize: one argument in, one result out. It must be deterministic for
     * the cache to be meaningful and must not re-enter the owning cache.
     */
    public interface Calculation<TParameters, TResult> {
        TResult calculate(TParameters parameters);
    }

    private final Calculation<TParameters, TResult> calculation;
    private final Map<TParameters, TResult> cachedResults = new HashMap<TParameters, TResult>();

    public CalculationCache(Calculation<TParameters, TResult> calculation) {
        if (calculation == null) {
            throw new IllegalArgumentException("calculation");
        }
        this.calculation = calculation;
    }

    /**
     * Returns the cached result for {@code parameters}, computing and remembering it on the first
     * request. Synchronized, so concurrent first requests collapse into a single computation.
     *
     * @throws IllegalArgumentException if {@code parameters} is null (C# threw
     *         {@code ArgumentNullException})
     */
    public synchronized TResult getResult(TParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters");
        }
        TResult result = cachedResults.get(parameters);
        if (result == null) {
            result = calculation.calculate(parameters);
            cachedResults.put(parameters, result);
        }
        return result;
    }
}
