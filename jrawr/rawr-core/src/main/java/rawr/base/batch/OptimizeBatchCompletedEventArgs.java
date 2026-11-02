// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchOptimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the OptimizeBatchCompleted event.
package rawr.base.batch;

/**
 * Payload of the {@code OptimizeBatchCompleted} event
 * ({@link BatchOptimizer.OptimizeBatchCompletedListener}).
 *
 * <p>{@link #optimizedBatch()} is the best batch found ({@code null} on failure or cancellation);
 * {@link #optimizedBatchValuation()} is its {@link BatchValuation} with the per-character values
 * and weighted total; {@link #optimizedBatchValue()} is that total, computed only when a completed
 * listener is registered (0 otherwise, since the original synchronous entry point never computed
 * it). {@link #error()} carries the captured exception (the synchronous entry point also rethrows
 * it) and {@link #cancelled()} reports whether cancellation stopped the search.</p>
 */
public record OptimizeBatchCompletedEventArgs(
        BatchIndividual optimizedBatch,
        BatchValuation optimizedBatchValuation,
        float optimizedBatchValue,
        Exception error,
        boolean cancelled) {
}
