// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchOptimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.batch;

import java.util.List;

/**
 * Objective snapshot of one {@link BatchIndividual} in {@link BatchOptimizer}, the search that
 * gears several characters at once.
 *
 * <p>The batch objective is a weighted sum of the individual characters' values: {@link
 * #optimizedValue} holds that sum, the value the search maximizes, and {@link #optimizedBatchValue}
 * holds the per-character values in batch order. {@code BatchOptimizer} fills both when it valuates
 * an individual; the public fields mirror the C# data holder.</p>
 */
public class BatchValuation {
    public List<Float> optimizedBatchValue;
    public float optimizedValue;
}
