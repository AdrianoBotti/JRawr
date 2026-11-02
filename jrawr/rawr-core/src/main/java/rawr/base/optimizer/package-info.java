// New documentation (not ported from C#): package-level guide.
/**
 * The gear optimizer: searching the space of legal gear combinations for the best result.
 *
 * <h2>The search in one paragraph</h2>
 *
 * <p>An "individual" is one complete gear setup (one item per slot, with gems and enchant). The
 * optimizer starts from the character's current gear (or a random point), evaluates it with the
 * selected model, then improves it with a genetic algorithm: it keeps a population, lets the best
 * individuals reproduce (crossover), mutates some genes (swap an item, regem, re-enchant),
 * occasionally transplants a parent from another population "island" to escape local optima, and
 * periodically scans every slot for a direct single-slot upgrade. The user-facing
 * <i>thoroughness</i> dial scales population size and generation budget.</p>
 *
 * <h2>Classes</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.optimizer.OptimizerBase} &mdash; the generic search skeleton: population,
 *       crossover, mutation, elite retention, direct upgrades, termination, the deterministic
 *       evaluation pool ({@code setMaxThreads}) and the {@link rawr.base.optimizer.NetRandom}
 *       generator. Read its class doc for the exact algorithm.</li>
 *   <li>{@link rawr.base.optimizer.ItemInstanceOptimizer} &mdash; the main optimizer; individuals
 *       are arrays of equipped copies including gems and enchants. Supports optimize-to-best,
 *       per-item upgrade lists and single-upgrade evaluation.</li>
 *   <li>{@link rawr.base.optimizer.ItemOptimizer} &mdash; the simpler optimizer that searches
 *       item ids only, without gem/enchant variations.</li>
 *   <li>{@link rawr.base.optimizer.AvailableItemGenerator} &mdash; builds the candidate pool per
 *       slot: relevant items from {@link rawr.base.items.ItemCache}, expanded into gemmed and
 *       enchanted instances, honoring availability strings, socket bonuses and template gemming.
 *       Its helper types are {@link rawr.base.optimizer.ItemAvailabilityInformation} and
 *       {@link rawr.base.optimizer.DirectUpgradeEntry}.</li>
 *   <li>{@link rawr.base.optimizer.OptimizerCharacter} &mdash; the recycled working copy of a
 *       character used as one individual (the "gene array").</li>
 *   <li>{@link rawr.base.optimizer.NetRandom} &mdash; the .NET-compatible random generator; a
 *       fixed seed makes the whole search reproducible.</li>
 * </ul>
 *
 * <p>Roster-scale (batch) runs build on this package: see {@link rawr.base.batch}.</p>
 *
 * <p><b>Verification contract:</b> the C# and Java searches may choose different item sets because
 * tie-breaking and RNG differ. The golden optimizer tests therefore compare the <i>best score</i>
 * only: Java must reach at least the C# score minus a tolerance on the same fixed item pool. See
 * {@code docs/dev/DEVIATIONS.md} D2.</p>
 */
package rawr.base.optimizer;
