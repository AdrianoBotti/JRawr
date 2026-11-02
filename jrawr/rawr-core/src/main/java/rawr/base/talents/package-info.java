// New documentation (not ported from C#): package-level guide.
/**
 * The talent and glyph tables: one class per playable class, plus the contract and metadata that
 * let the model and import layers read them generically.
 *
 * <p>A <b>talent</b> is a permanent character customization: a named ability upgrade with a rank
 * (0..N) in one of three talent trees; a build is the full allocation of talent points and is what
 * people call a "spec". A <b>glyph</b> is a smaller slot-based enhancement (major/minor glyphs).
 * Models read specific talents and glyphs to decide which abilities and bonuses apply.</p>
 *
 * <ul>
 *   <li>{@link rawr.base.talents.TalentsBase} &mdash; the abstract contract: clone, validate a
 *       point allocation, look up talents/glyphs by name or spell id, load/save the compact
 *       string form used in character files, and the reflective surface described below.</li>
 *   <li>{@link rawr.base.talents.TalentDataAttribute},
 *       {@link rawr.base.talents.GlyphDataAttribute} &mdash; metadata annotations placed on the
 *       getter methods of each talent table (tree, name, max rank, spell id). The import/model
 *       layer enumerates them instead of hard-coding hundreds of names, replacing the original's
 *       reflection with a stable, documented surface.</li>
 *   <li>One table per class: {@link rawr.base.talents.DeathKnightTalents},
 *       {@link rawr.base.talents.DruidTalents}, {@link rawr.base.talents.HunterTalents},
 *       {@link rawr.base.talents.MageTalents}, {@link rawr.base.talents.PaladinTalents},
 *       {@link rawr.base.talents.PriestTalents}, {@link rawr.base.talents.RogueTalents},
 *       {@link rawr.base.talents.ShamanTalents}, {@link rawr.base.talents.WarlockTalents},
 *       {@link rawr.base.talents.WarriorTalents} (plus the unused Cata-era
 *       {@link rawr.base.talents.WarriorTalentsCata}).</li>
 * </ul>
 */
package rawr.base.talents;
