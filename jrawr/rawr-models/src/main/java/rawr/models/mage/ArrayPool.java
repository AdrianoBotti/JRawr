// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ArrayPool.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;

public final class ArrayPool {
    private static List<ArraySet> pool = new ArrayList<ArraySet>();

    private static int createdArraySets = 0;
    private static int waiters = 0;
    // C# desktop: Environment.ProcessorCount
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors();

    private ArrayPool() {
    }

    public static int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public static void setMaximumPoolSize(int value) {
        maximumPoolSize = value;
    }

    public static ArraySet requestArraySet(boolean largeArraysHint) {
        synchronized (pool) {
            if (pool.size() == 0) {
                if (createdArraySets < maximumPoolSize) {
                    pool.add(new ArraySet());
                    createdArraySets++;
                } else {
                    do {
                        waiters++;
                        try {
                            pool.wait();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ex);
                        } finally {
                            waiters--;
                        }
                    } while (pool.size() == 0);
                }
            }
            // find desirable size
            int bestIndex;
            if (largeArraysHint) {
                bestIndex = 0;
                for (int i = 1; i < pool.size(); i++) {
                    if (pool.get(i).getMaxSize() > pool.get(bestIndex).getMaxSize()) {
                        bestIndex = i;
                    }
                }
            } else {
                bestIndex = pool.size() - 1;
            }
            ArraySet result = pool.get(bestIndex);
            result.spellIndex = 0;
            result.cycleIndex = 0;
            result.stateIndex = 0;
            result.effectIndex = 0;
            pool.remove(bestIndex);
            return result;
        }
    }

    public static void releaseArraySet(ArraySet arraySet) {
        synchronized (pool) {
            if (pool.size() >= maximumPoolSize) {
                createdArraySets--;
            } else {
                pool.add(arraySet);
                if (waiters > 0) {
                    pool.notify();
                }
            }
        }
    }
}
