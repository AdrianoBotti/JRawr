// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the EvaluateUpgradeProgressChanged event (System.ComponentModel.ProgressChangedEventArgs).
package rawr.base.batch;

/**
 * Payload of the {@code EvaluateUpgradeProgressChanged} event
 * ({@link BatchOptimizer.EvaluateUpgradeProgressChangedListener}); the port of .NET's
 * {@code System.ComponentModel.ProgressChangedEventArgs}.
 *
 * <p>Small immutable holder: {@link #progressPercentage()} is the completion of the scan in
 * 0..100, and {@link #userState()} is the caller-supplied state object, which
 * {@link BatchOptimizer} always leaves {@code null} as the original did.</p>
 */
public record ProgressChangedEventArgs(int progressPercentage, Object userState) {
}
