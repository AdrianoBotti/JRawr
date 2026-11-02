// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/ArrayPool.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Java cannot express `static class ArrayPool<T> where T : new()` with per-closed-type static state;
// the only user in the repo is MarkovProcess (LU.ArraySet), so the pool is specialized to that type.
package rawr.base.algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * A small static pool of {@link LU.ArraySet} scratch buffers, so repeated {@link LU} factorizations
 * do not allocate fresh arrays every time.
 *
 * <p>An {@code ArraySet} bundles the {@code double[]}/{@code int[]} arrays one {@link LU} instance
 * works on (the dense upper factor, the sparse eta file, permutations and scratch columns). The pool
 * lends one out with {@link #requestArraySet()} and takes it back with
 * {@link #releaseArraySet(LU.ArraySet)}. A borrower must always return its set (typically in a
 * {@code finally} block), otherwise the sets run out and later borrowers block forever.</p>
 *
 * <h2>Capacity and blocking</h2>
 *
 * <p>At most {@code maximumPoolSize} sets exist at once (default 2). {@code requestArraySet} creates
 * a new set while fewer than that many have been created; otherwise it waits on the pool monitor
 * until another thread releases one. {@code releaseArraySet} puts the set back and wakes one waiter,
 * or, when the pool is already full (for example because the maximum was lowered after this set was
 * created), drops the set and decrements the created count so it can be garbage-collected. Retrieval
 * takes the last element of the internal list (LIFO), not the first. An interrupted wait restores
 * the interrupt flag and retries; because a set interrupt flag makes every later wait throw
 * immediately, an interrupted borrower effectively spins until a set is released (a faithful port of
 * the original's behavior).</p>
 *
 * <h2>Porting notes</h2>
 *
 * <p>All state is static and guarded by the internal list's monitor. The C# original is a generic
 * {@code ArrayPool<T> where T : new()} whose static fields exist once per closed type; Java has no
 * per-type static state, and the only user here is {@link MarkovProcess} with {@code LU.ArraySet},
 * so this port is specialized to that type. This is shared process-wide state: it is safe for
 * concurrent borrowers, but it is also why the optimizer's thread count is tied to the pool size.</p>
 *
 * @see LU.ArraySet
 * @see MarkovProcess
 */
public final class ArrayPool {
    private ArrayPool() {
    }

    private static final List<LU.ArraySet> pool = new ArrayList<LU.ArraySet>();

    private static int createdArraySets = 0;
    private static int maximumPoolSize = 2;

    public static int getMaximumPoolSize() {
        synchronized (pool) {
            return maximumPoolSize;
        }
    }

    public static void setMaximumPoolSize(int value) {
        synchronized (pool) {
            maximumPoolSize = value;
        }
    }

    /**
     * Borrows a set, creating one if capacity allows; blocks while all sets are lent out. The caller
     * owns the returned set until it passes it to {@link #releaseArraySet(LU.ArraySet)}.
     */
    public static LU.ArraySet requestArraySet() {
        synchronized (pool) {
            if (pool.isEmpty() && createdArraySets < maximumPoolSize) {
                pool.add(new LU.ArraySet());
                createdArraySets++;
            } else {
                while (pool.isEmpty()) {
                    try {
                        pool.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            int bestIndex = pool.size() - 1;
            LU.ArraySet result = pool.get(bestIndex);
            pool.remove(bestIndex);
            return result;
        }
    }

    /**
     * Returns a borrowed set. The set must have been obtained from {@link #requestArraySet()} of
     * this pool and must not be used afterwards; the method does not validate the argument.
     */
    public static void releaseArraySet(LU.ArraySet arraySet) {
        synchronized (pool) {
            if (pool.size() >= maximumPoolSize) {
                createdArraySets--;
            } else {
                pool.add(arraySet);
                pool.notify();
            }
        }
    }
}
