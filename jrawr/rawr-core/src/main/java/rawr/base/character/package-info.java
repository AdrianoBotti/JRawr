// New documentation (not ported from C#): package-level guide.
/**
 * The player character and everything attached to it: gear slots, buffs, professions, saved
 * talent/buff sets and the class/race/region enums.
 *
 * <ul>
 *   <li>{@link rawr.base.character.Character} &mdash; the hub value object: 21
 *       {@link rawr.base.items.ItemInstance} slots, active {@link rawr.base.character.Buff}s,
 *       per-class talents, professions, the encounter selection
 *       ({@link rawr.base.bosses.BossOptions}) and the per-model option map. It is the input to
 *       every model calculation and the unit of save/load. Its class doc covers the gemmed-id
 *       format, buff auto-activation, set-bonus recalculation, cache invalidation and the
 *       save/load split (writer here, reader in {@code rawr-data}).</li>
 *   <li>{@link rawr.base.character.Buff}, {@link rawr.base.character.BuffGroup} &mdash; buff
 *       definitions and their registry: stats, improvements (e.g. Mixology), set bonuses,
 *       conflicts, custom entries and the shared relevance caches.</li>
 *   <li>{@link rawr.base.character.Profs}, {@link rawr.base.character.Profession} &mdash;
 *       professions and the buffs/restrictions they imply.</li>
 *   <li>{@link rawr.base.character.SavedTalentSpec},
 *       {@link rawr.base.character.SavedTalentSpecList},
 *       {@link rawr.base.character.SavedBuffSet},
 *       {@link rawr.base.character.SavedBuffSetList} &mdash; named snapshots the user can store
 *       and reload (also the format the models use to identify a spec).</li>
 *   <li>{@link rawr.base.character.CharacterSlot}, {@link rawr.base.character.CharacterClass},
 *       {@link rawr.base.character.CharacterRace}, {@link rawr.base.character.CharacterFaction},
 *       {@link rawr.base.character.CharacterRegion} &mdash; the core character enums; the slot
 *       enum documents the numeric layout save files rely on.</li>
 *   <li>{@link rawr.base.character.PlayerRoles}, {@link rawr.base.character.TargetGroup},
 *       {@link rawr.base.character.ReputationLevel}, {@link rawr.base.character.PossibleLevels}
 *       &mdash; support enums used by encounters, items and the UI.</li>
 * </ul>
 */
package rawr.base.character;
