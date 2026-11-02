// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the ComputeUpgradesCompleted event.
package rawr.base.batch;

import rawr.base.calculations.ComparisonCalculationUpgrades;
import rawr.base.character.CharacterSlot;

import java.util.List;
import java.util.Map;

/**
 * Payload of the {@code ComputeUpgradesCompleted} event
 * ({@link BatchOptimizer.ComputeUpgradesCompletedListener}).
 *
 * <p>{@link #upgrades()} maps each {@link CharacterSlot} to the upgrades found for that slot,
 * each a {@link ComparisonCalculationUpgrades} holding the gain and per-character sub-points; it
 * may be {@code null} or partial when the scan failed or was cancelled. {@link #error()} carries
 * the captured exception (the synchronous entry point also rethrows it) and {@link #cancelled()}
 * reports whether cancellation stopped the scan.</p>
 */
public record ComputeUpgradesCompletedEventArgs(
        Map<CharacterSlot, List<ComparisonCalculationUpgrades>> upgrades,
        Exception error,
        boolean cancelled) {
}
