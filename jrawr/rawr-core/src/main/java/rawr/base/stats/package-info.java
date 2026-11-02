// New documentation (not ported from C#): package-level guide.
/**
 * The stat model: what a stat is in this engine, how two stat contributions combine, and how game
 * ratings turn into percentages.
 *
 * <p>Everything a character gains is eventually a {@link rawr.base.stats.Stats} object: a bag of
 * several hundred named {@code float}s. The defining design decision of the engine is that each
 * stat belongs to one of five <i>combination categories</i>, and merging two snapshots applies the
 * category's rule:</p>
 *
 * <ul>
 *   <li><b>Additive</b> ({@link rawr.base.stats.AdditiveStat}) &mdash; {@code a + b}; most stats
 *       (attributes, spell power, ratings, ...).</li>
 *   <li><b>Multiplicative</b> ({@link rawr.base.stats.MultiplicativeStat}) &mdash;
 *       {@code (1+a)*(1+b) - 1}; percentage increases never simply add.</li>
 *   <li><b>Inverse multiplicative</b> ({@link rawr.base.stats.InverseMultiplicativeStat}) &mdash;
 *       {@code 1 - (1-a)*(1-b)}; percentage reductions.</li>
 *   <li><b>Non-stacking</b> ({@link rawr.base.stats.NonStackingStat}) &mdash; {@code max(a,b)};
 *       effects that do not stack.</li>
 *   <li><b>Single-source</b> ({@link rawr.base.stats.SingleSourceStat}) &mdash; set once per
 *       calculation and never combined; the Java shape of the C# {@code StatsCat}/{@code StatsBear}
 *       subclass properties (see the enum for the exact merge/copy/equality contract).</li>
 * </ul>
 *
 * <h2>Classes</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.stats.Stats} &mdash; the value type; five backing arrays, special-effect
 *       list, sparse-cache optimization, accumulation API and the {@code PROPERTY_INFO} metadata
 *       table. Read this first.</li>
 *   <li>{@link rawr.base.stats.StatConversion} &mdash; every level-80 conversion formula:
 *       rating&rarr;percent, attribute&rarr;derived stat, armor mitigation, resistance tables and
 *       the melee {@code CombatTable}.</li>
 *   <li>{@link rawr.base.stats.BaseStats} &mdash; the race/class inherent stats and racials a
 *       character has before gear.</li>
 *   <li>{@link rawr.base.stats.StatFilter} &mdash; a predicate over a stat value, used when
 *       filtering/accumulating only relevant stats.</li>
 *   <li>{@link rawr.base.stats.WeightedStat} &mdash; a (value, probability) pair, used when a proc
 *       or buff has several possible values.</li>
 *   <li>{@link rawr.base.stats.HitResult} &mdash; the attack-outcome enum (hit/miss/dodge/parry/
 *       glance/block/crit).</li>
 *   <li>{@link rawr.base.stats.MagicSchool} &mdash; the spell-school enum with its damage
 *       multipliers.</li>
 *   <li>The five {@code *Stat} enums &mdash; define the storage slot and combination rule of each
 *       stat; their javadoc lists the values.</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 *
 * <ul>
 *   <li>{@code Stats} is the <b>single source of truth for every stat</b>: shared stats,
 *       gear-granted stats and model-specific stats all live here, including the model-only
 *       {@link rawr.base.stats.SingleSourceStat} values. Do not introduce per-model {@code Stats}
 *       subclasses or per-model stat containers; add a slot and a combination category instead.
 *       {@link rawr.base.stats.BaseStats} is the only other stats-related class, and it is a
 *       provider of the race/class base tables, not a second stat store.</li>
 *   <li>Stats are {@code float} and arithmetic order is observable; never widen or regroup.</li>
 *   <li>Mutating a {@code Stats} value invalidates its sparse cache
 *       ({@link rawr.base.stats.Stats#invalidateSparseData()}); see the class doc for the hot-path
 *       API.</li>
 *   <li>Proc-based contributions are not flattened into numbers here; they stay as
 *       {@link rawr.base.effects.SpecialEffect}s until a model averages them.</li>
 * </ul>
 */
package rawr.base.stats;
