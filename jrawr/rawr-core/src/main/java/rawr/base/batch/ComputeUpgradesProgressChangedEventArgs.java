// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Payload of the ComputeUpgradesProgressChanged event.
package rawr.base.batch;

/**
 * Payload of the {@code ComputeUpgradesProgressChanged} event
 * ({@link BatchOptimizer.ComputeUpgradesProgressChangedListener}), fired once per item examined
 * by an upgrade scan.
 *
 * <p>{@link #currentItem()} is the display name of the item being scanned.
 * {@link #progressPercentage()} and {@link #itemProgressPercentage()} are the two progress
 * counters; the original call site passed them in the reverse of the constructor's declared order
 * (SUSPECTED_BUGS #39) and the port preserves that, so in practice the "progress percentage" field
 * carries the item-scan progress (0..100 across the item list) and the "item progress percentage"
 * field carries the operation-level hook value (0 while scanning, 100 at the end).</p>
 */
public record ComputeUpgradesProgressChangedEventArgs(
        int progressPercentage,
        int itemProgressPercentage,
        String currentItem) {
}
