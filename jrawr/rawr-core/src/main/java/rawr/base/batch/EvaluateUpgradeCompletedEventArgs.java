// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the EvaluateUpgradeCompleted event.
package rawr.base.batch;

import rawr.base.calculations.ComparisonCalculationUpgrades;

/**
 * Payload of the {@code EvaluateUpgradeCompleted} event
 * ({@link BatchOptimizer.EvaluateUpgradeCompletedListener}).
 *
 * <p>{@link #upgradeValue()} is the score delta of the evaluated item versus the batch's starting
 * equipment. {@link #upgrade()} would carry the item comparison, but both the original asynchronous
 * call site and this port pass {@code null}. {@link #error()} carries a captured exception (the
 * synchronous entry point also rethrows it) and {@link #cancelled()} reports whether cancellation
 * stopped the evaluation.</p>
 */
public record EvaluateUpgradeCompletedEventArgs(
        float upgradeValue,
        ComparisonCalculationUpgrades upgrade,
        Exception error,
        boolean cancelled) {
}
