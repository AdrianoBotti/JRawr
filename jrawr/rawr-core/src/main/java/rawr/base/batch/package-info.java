// New documentation (not ported from C#): package-level guide.
/**
 * Batch (roster-scale) runs: optimizing or computing upgrades for many characters at once, with a
 * progress/completion event surface.
 *
 * <p>A raid roster is a list of characters; the batch tools run the optimizer for each of them and
 * produce per-character best gear and/or upgrade lists. This is the machinery behind "optimize my
 * whole raid" and "what is each character's next upgrade?".</p>
 *
 * <h2>Classes</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.batch.BatchCharacter} &mdash; one roster entry: a character plus its file
 *       path, weight, model and unsaved-changes state. Changing its properties notifies
 *       {@code BatchCharacter.PropertyChanged} listeners.</li>
 *   <li>{@link rawr.base.batch.BatchCharacterList} &mdash; the roster: load/save and the weighted
 *       list the optimizer consumes.</li>
 *   <li>{@link rawr.base.batch.BatchTools} &mdash; orchestration of batch optimize/upgrade runs:
 *       status messages, progress and completion callbacks, cancellation and configuration.</li>
 *   <li>{@link rawr.base.batch.BatchOptimizer} &mdash; the optimizer loop over a batch, including
 *       shared gemming and the per-run upgrade computation.</li>
 *   <li>{@link rawr.base.batch.BatchValuation} &mdash; how an individual's per-character values
 *       become one weighted objective for the batch.</li>
 *   <li>{@link rawr.base.batch.BatchIndividual} &mdash; the gene type for a whole roster (a shared
 *       item pool with per-character usage counts).</li>
 * </ul>
 *
 * <h2>Event surface</h2>
 *
 * <p>The original C# code used {@code BackgroundWorker} and .NET events raised from worker
 * threads. This port runs the work synchronously on the calling thread (the web layer wraps it in
 * its own executor) and exposes small listener interfaces instead:
 * {@code StatusUpdated}, {@code OperationCompleted}, {@code UpgradeListCompleted} on
 * {@code BatchTools}; {@code PropertyChanged} on {@code BatchCharacter}; and the
 * {@code OptimizeBatchCompleted}/{@code ComputeUpgradesCompleted}/{@code EvaluateUpgradeCompleted}
 * plus {@code ProgressChanged} pairs on {@code BatchOptimizer}. Each event-args class in this
 * package is a small payload holder; listeners fire at the same points the C# raise sites did.</p>
 */
package rawr.base.batch;
