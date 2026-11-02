// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the OptimizeCharacterProgressChanged / OptimizeBatchProgressChanged events.
package rawr.base.batch;

/**
 * Payload of the {@code OptimizeBatchProgressChanged} event
 * ({@link BatchOptimizer.OptimizeBatchProgressChangedListener}); named after the single-character
 * event it was shared with in the original.
 *
 * <p>Small immutable holder: {@link #progressPercentage()} is the completion of the current search
 * in 0..100 and {@link #bestValue()} is the best batch score found so far, in the same
 * model-defined units as the batch score (0 until the first evaluation).</p>
 */
public record OptimizeCharacterProgressChangedEventArgs(int progressPercentage, float bestValue) {
}
