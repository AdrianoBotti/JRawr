// New documentation (not ported from C#): package-level guide.
/**
 * Procs, conditional bonuses and the numerical machinery that averages them over a fight.
 *
 * <p>Many items and buffs grant their benefit only some of the time: "on spell hit, 10% chance to
 * gain 500 spell power for 10 s, once every 45 s". Such an effect cannot be added to a stat total
 * as a constant, because its value depends on how often the character acts and how long the fight
 * is. The engine therefore keeps these effects separate and evaluates them late, once the model
 * knows the fight context.</p>
 *
 * <h2>Classes</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.effects.SpecialEffect} &mdash; the proc definition (trigger, stats,
 *       duration, cooldown, chance, max stacks) and the uptime mathematics that converts it into
 *       an average stat contribution or a probability distribution over values. Negative
 *       {@code chance} encodes PPM (procs per minute).</li>
 *   <li>{@link rawr.base.effects.SpecialFunction} &mdash; the special functions behind the uptime
 *       integrals (gamma, log-gamma, erf/erfc and the regularized incomplete beta function) plus
 *       the precomputed interpolation grid that makes the exact model fast enough for the
 *       optimizer.</li>
 *   <li>{@link rawr.base.effects.Trigger} &mdash; the event enum that says what can set an effect
 *       off (melee crit, spell hit, DoT tick, use, ...).</li>
 *   <li>{@link rawr.base.effects.SpecialEffects} &mdash; static parsers that read item tooltip
 *       text (Wowhead/AoWoW payloads) and turn "Equip:"/"Use:"/"Chance on hit:" lines into
 *       {@code SpecialEffect}s during item import.</li>
 *   <li>{@link rawr.base.effects.SpecialDamageProcs} &mdash; values damage-dealing procs (extra
 *       fire/holy/... damage on hit) against the character's already computed stats.</li>
 * </ul>
 *
 * <h2>Cache lifecycle</h2>
 *
 * <p>{@code SpecialEffect} keeps a per-fight-duration interpolation cache for the exact uptime
 * model. It is only valid for the calculation that built it, so a top-level calculation clears the
 * shared caches first ({@code SpecialEffect.clearInterpolatorCaches()}); model-owned static effects
 * clear their own. Forgetting this makes one character's results leak into another's.</p>
 */
package rawr.base.effects;
