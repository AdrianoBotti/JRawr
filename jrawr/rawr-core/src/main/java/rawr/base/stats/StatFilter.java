// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

/**
 * Predicate over a single stat <b>value</b> (not a name): returns true when that value matches
 * the caller's criterion.
 *
 * <p>It exists so {@link Stats#values(StatFilter)} can iterate the non-zero stats that satisfy a
 * condition without exposing the stat-enumeration machinery; lambdas such as "value greater than
 * zero" or "value greater than 1" are the intended use. Implementations must not mutate the
 * {@link Stats} object while it is being iterated.</p>
 */
@FunctionalInterface
public interface StatFilter {
    boolean test(float value);
}
