// New documentation (not ported from C#): package-level guide.
/**
 * The gear model: item templates, equipped copies with gems and enchants, the process-wide item
 * database and the item filter/relevance system.
 *
 * <h2>Item vs. ItemInstance</h2>
 *
 * <p>{@link rawr.base.items.Item} is the immutable <i>template</i> as it exists in the game
 * database: base stats, socket colors, socket bonus, class/armor type, restrictions and source.
 * {@link rawr.base.items.ItemInstance} is one concrete copy in a character's slot: the item id
 * plus up to three gem ids and one enchant id, written as the "gemmed id" string
 * {@code itemId.gem1.gem2.gem3.enchantId} in save files. The same template can therefore produce
 * different stats for different characters (gems, socket bonus activation, enchant). Stats resolve
 * in a fixed order: item base stats, qualifying gems, socket bonus when the gems match, then the
 * enchant.</p>
 *
 * <h2>Classes</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.items.Item}, {@link rawr.base.items.ItemInstance} &mdash; the two core
 *       types; read their class docs for the resolution rules and caching.</li>
 *   <li>{@link rawr.base.items.ItemCache} &mdash; the process-wide item registry: load, add
 *       (including user-fetched items), delete, and the per-character "relevant items" subsets
 *       the UI and optimizer use.</li>
 *   <li>{@link rawr.base.items.Enchant}, {@link rawr.base.items.GemmingTemplate} &mdash; enchant
 *       catalog and the per-color gem-choice templates the optimizer expands.</li>
 *   <li>{@link rawr.base.items.ItemSlot}, {@link rawr.base.items.ItemType},
 *       {@link rawr.base.items.ItemTypeList}, {@link rawr.base.items.ItemQuality},
 *       {@link rawr.base.items.ItemDamageType}, {@link rawr.base.items.ItemFaction},
 *       {@link rawr.base.items.ItemAvailability} &mdash; the gear enums.</li>
 *   <li>{@link rawr.base.items.ItemFilter}, {@link rawr.base.items.ItemFilterData},
 *       {@link rawr.base.items.ItemFilterRegex},
 *       {@link rawr.base.items.ItemFilterRegexList},
 *       {@link rawr.base.items.ItemFilterEnabledOverride} &mdash; the rule tree that decides which
 *       items are relevant for a model.</li>
 * </ul>
 *
 * <p>Where an item comes from (vendor, quest, drop, PvP, crafting) lives in
 * {@link rawr.base.items.sources}.</p>
 */
package rawr.base.items;
