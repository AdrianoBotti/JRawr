// New documentation (not ported from C#): module-level guide to the shared engine.
/**
 * The shared engine of JRawr: everything that is not specific to one character specialization,
 * split into one sub-package per concern. This package only contains documentation; start here to
 * find the class you need.
 *
 * <p>JRawr is a port of the C# World of Warcraft 3.3.5 gear optimizer <i>Rawr</i> to Java 25. It
 * answers one question: <b>"given a character, a set of gear and a fight, which gear produces the
 * best result?"</b> The per-specialization simulation models (Mage, Hunter, Death Knight, ...)
 * live in the {@code rawr.models.*} packages of the {@code rawr-models} module; the packages below
 * provide the data model, the stat math, the caches, the encounter model and the search algorithms
 * every model shares.</p>
 *
 * <h2>Where to start</h2>
 *
 * <table border="1">
 *   <caption>Sub-package map</caption>
 *   <tr><th>Package</th><th>Contents</th></tr>
 *   <tr>
 *     <td>{@link rawr.base.stats}</td>
 *     <td>The stat model: {@link rawr.base.stats.Stats} (the bag of several hundred stats and its
 *         combination rules), rating/percentage conversions, base stats, caps, diminishing
 *         returns and the melee combat table. <b>Start here</b> to understand what a "stat" is in
 *         this engine.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.effects}</td>
 *     <td>Procs and conditional effects: {@link rawr.base.effects.SpecialEffect} (trigger,
 *         duration, cooldown, chance, stacks and the uptime mathematics),
 *         {@link rawr.base.effects.SpecialFunction} (the numerical functions behind it) and the
 *         tooltip parsers.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.items}</td>
 *     <td>Gear: {@link rawr.base.items.Item} templates, equipped copies
 *         ({@link rawr.base.items.ItemInstance}), gems/sockets, enchants, the item database
 *         ({@link rawr.base.items.ItemCache}) and the item filter/relevance system.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.items.sources}</td>
 *     <td>Where items come from: vendors, quests, drops, PvP, crafting and the source tree used
 *         to display and filter them.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.character}</td>
 *     <td>{@link rawr.base.character.Character} itself plus buffs, professions, saved
 *         talent/buff sets and the class/race/slot enums.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.talents}</td>
 *     <td>One talent/glyph table per class and the reflective metadata the model layer uses to
 *         read them.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.calculations}</td>
 *     <td>The model contract: {@link rawr.base.calculations.CalculationsBase} (what every model
 *         implements), the explicit registry replacing C# reflection, the result types
 *         ({@link rawr.base.calculations.CharacterCalculationsBase}) and optimization
 *         goals/constraints.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.optimizer}</td>
 *     <td>Gear search: the genetic-algorithm skeleton, the item-instance and item optimizers, the
 *         candidate-pool generator and their working types.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.batch}</td>
 *     <td>Roster-scale runs: many characters at once, upgrade lists and the listener-based
 *         progress event surface.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.bosses}</td>
 *     <td>The encounter database: what a boss does (health, timers, attacks, movement, adds) and
 *         the concrete definition of every WotLK boss.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.algorithms}</td>
 *     <td>Model-agnostic numerics: sparse LU decomposition, Markov chains, state-space
 *         enumeration, array pooling and result caching.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link rawr.base.util}</td>
 *     <td>Cross-cutting helpers: formatting shims for .NET-compatible output
 *         ({@link rawr.base.util.NetFormat}), logging, status messages, array/enum helpers and
 *         small generic containers.</td>
 *   </tr>
 * </table>
 *
 * <h2>How a calculation flows</h2>
 *
 * <ol>
 *   <li><b>Data load</b> ({@code rawr-data}) &mdash; the XML files shipped with Rawr are parsed
 *       once into {@link rawr.base.items.ItemCache}, enchant defaults
 *       ({@link rawr.base.items.Enchant}) and buff defaults ({@link rawr.base.character.Buff}).
 *       Nothing here reads XML at calculation time.</li>
 *   <li><b>Character assembly</b> &mdash; a saved character file (or the web editor) fills a
 *       {@link rawr.base.character.Character}: 21 slots of
 *       {@link rawr.base.items.ItemInstance}, active buffs, professions, per-class talents
 *       ({@link rawr.base.talents}) and {@link rawr.base.bosses.BossOptions} (fight length,
 *       target level, mechanics).</li>
 *   <li><b>Base stats</b> &mdash; {@link rawr.base.stats.BaseStats} produces the level/class/race
 *       inherent stats; gear resolution adds each item's stats plus gems, enchant and socket bonus
 *       ({@code ItemInstance.accumulateTotalStats}).</li>
 *   <li><b>Buff accumulation</b> &mdash; active buffs add stats and register their special
 *       effects; {@link rawr.base.calculations.CalculationsBase} exposes the shared helpers.</li>
 *   <li><b>Model simulation</b> &mdash; the model ({@code Calculations<Model>} in
 *       {@code rawr-models}) turns the resolved stats into a
 *       {@link rawr.base.calculations.CharacterCalculationsBase}: ratings (DPS/HPS/...), the final
 *       stats, display strings and comparison charts.</li>
 *   <li><b>Consumption</b> &mdash; the UI displays the ratings and
 *       {@link rawr.base.optimizer.ItemInstanceOptimizer} re-runs steps 3-6 over thousands of gear
 *       combinations.</li>
 * </ol>
 *
 * <h2>A short glossary for non-WoW readers</h2>
 *
 * <ul>
 *   <li><b>Rating</b> &mdash; a level-scaled stat that converts into a percentage (at level 80,
 *       32.79 hit rating = 1% hit). See {@link rawr.base.stats.StatConversion}.</li>
 *   <li><b>Cap</b> &mdash; a threshold after which a stat stops helping ("hit cap").</li>
 *   <li><b>Proc</b> &mdash; a randomly triggered effect ("on spell hit, 10% chance for +500 spell
 *       power for 10 s"). See {@link rawr.base.effects.SpecialEffect}.</li>
 *   <li><b>Uptime</b> &mdash; the average fraction of a fight during which a proc is active.</li>
 *   <li><b>PPM</b> &mdash; procs per minute; a negative {@code chance} encodes it.</li>
 *   <li><b>Buff / debuff</b> &mdash; a temporary beneficial/harmful effect, usually from a raid
 *       member.</li>
 *   <li><b>Talent / glyph / spec</b> &mdash; character customization; a spec is one allocation of
 *       talent points and has its own model.</li>
 *   <li><b>DPS / HPS / TPS</b> &mdash; damage / healing / threat per second; the ratings models
 *       optimize.</li>
 * </ul>
 *
 * <h2>Porting conventions and invariants</h2>
 *
 * <ul>
 *   <li>Every ported file starts with
 *       {@code // Ported from legacy/<path> @ <legacy-commit-hash>} and may carry a
 *       {@code PARTIAL}/{@code OMITTED} note listing deliberately skipped members.</li>
 *   <li>Method and field names are camelCase versions of the C# names; class names are unchanged.
 *       The original C# packed everything into one {@code Rawr.Base} namespace and one assembly;
 *       Java has no assembly scope, so this port splits that namespace into the sub-packages above
 *       and widens a handful of {@code internal} members to public accessors where a split would
 *       otherwise break compilation (see {@code docs/dev/DECISIONS.md}).</li>
 *   <li>Formulas, float widths and operation order are part of the observable behavior and must
 *       not be "cleaned up". Equality with the C# oracle is enforced by the {@code rawr-golden}
 *       module (relative 1e-9 for doubles, bit-exact floats).</li>
 *   <li>Iteration order can leak into results: collections the original enumerates use
 *       {@code LinkedHashMap}/{@code LinkedHashSet} or insertion-ordered lists on purpose.</li>
 *   <li>{@code Math.rint} replicates C# {@code Math.Round} (half-to-even); Java's
 *       {@code Math.round} is half-up and must not be used for it.</li>
 * </ul>
 *
 * @see rawr.base.stats
 * @see rawr.base.items
 * @see rawr.base.character
 * @see rawr.base.calculations
 * @see rawr.base.optimizer
 */
package rawr.base;
