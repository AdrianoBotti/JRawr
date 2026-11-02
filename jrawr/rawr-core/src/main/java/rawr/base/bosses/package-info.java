// New documentation (not ported from C#): package-level guide.
/**
 * One class per Wrath of the Lich King raid/dungeon boss, describing the encounter for the
 * simulation models.
 *
 * <p>Rawr does not only optimize against a training dummy: a character's best gear depends on the
 * encounter. A fight where you can stand behind the boss, never move and never take damage rewards
 * different stats than a fight with constant area damage, target switches and forced movement. The
 * classes in this package are the <i>encounter database</i>: each one configures a
 * {@link rawr.base.bosses.BossHandler} (via {@link rawr.base.bosses.MultiDiffBoss}) with the numbers the models
 * need.</p>
 *
 * <h2>What every boss definition sets</h2>
 *
 * <ul>
 *   <li><b>Identity</b> &mdash; name, instance, tier (T7...T10) and version. Because raid sizes and
 *       difficulties scale the same boss, each class fills four parallel entries: 10-man normal,
 *       25-man normal, 10-man heroic, 25-man heroic. {@link rawr.base.bosses.MultiDiffBoss} is a list of
 *       four {@code BossHandler}s whose setters write all four entries at once; the UI later picks
 *       the variant the player selects.</li>
 *   <li><b>Vitals and timers</b> &mdash; health, target level (80/81/82/83; level 83 is a "boss
 *       level" target), armor, berserk timer (hard enrage) and the speed-kill timer that defines a
 *       "fast kill" for ranking purposes.</li>
 *   <li><b>Positioning</b> &mdash; the fraction of the fight the player can attack from behind (for
 *       melee) or from range. Behind-the-boss access removes parry/block from the combat table.</li>
 *   <li><b>Attacks</b> &mdash; the boss's own damage events ({@link rawr.base.bosses.Attack}): a standard
 *       melee swing, hateful strikes, breath attacks, etc., with damage per hit, attack speed,
 *       damage type and how many targets they hit. Tank-survival models use these.</li>
 *   <li><b>Damage over time</b> &mdash; auras the boss applies to the player
 *       ({@link rawr.base.bosses.DoT}): damage per tick, tick interval, duration, and school. Healer models
 *       use them to estimate constant incoming damage.</li>
 *   <li><b>Movement and control</b> &mdash; whether the boss stuns, roots, fears, moves or disarms
 *       the player, and how often. Models translate this into lost casting/attacking time.</li>
 *   <li><b>Phases and special mechanics</b> &mdash; multi-target adds (how many, how often, for how
 *       long), damage amplifiers below 35%/20% health ("execute range"), invulnerability windows,
 *       and the timers at which they occur.</li>
 *   <li><b>Resistances</b> &mdash; the boss's resistance to each magic school, used by spell damage
 *       models.</li>
 * </ul>
 *
 * <p>{@link rawr.base.bosses.BossList} collects all of them into the instance tree shown by the UI and
 * provides the reference index used to identify a boss across save files. See
 * {@link rawr.base.bosses.BossOptions} for the per-character selection and
 * {@link rawr.base.bosses.BossHandler} for the full field list with units.</p>
 *
 * <h2>Reading a boss class</h2>
 *
 * <p>The classes have no logic: they are constructors that set fields, often with the four-man-size
 * arrays. A typical file is {@link rawr.base.bosses.Patchwerk}: a standard melee attack plus a
 * "Hateful Strike" that hits one tank hard every second, with no movement and six-minute enrage
 * timers. Pattern-walk the constructor against the list above to see what the encounter models.</p>
 *
 * <p>Odd class names (e.g. {@code ValathriaDreamwalker}) and slightly-off instance spellings are
 * kept exactly as in the original assembly; they are part of the identifiers used by save files and
 * the oracle, so they are not corrected on purpose.</p>
 */
package rawr.base.bosses;
