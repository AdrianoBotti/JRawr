// New documentation (not ported from C#): package-level guide.
/**
 * Where items come from: the source/location model behind "vendor", "quest", "drop" and the item
 * browser's source tree.
 *
 * <p>{@link rawr.base.items.sources.ItemSource} is a tree of {@link rawr.base.items.sources.ItemLocation}
 * nodes. Each location describes one way to obtain an item &mdash; a vendor (with cost), a quest
 * (with the quest name/level), a drop from a boss or mob, a PvP reward (points/tokens), a crafted
 * recipe or a random world/container drop &mdash; and carries a description string plus optional
 * cost. {@link rawr.base.items.sources.LocationFactory} reads and writes the original
 * {@code ItemSource.xml} schema, {@link rawr.base.items.sources.ItemLocationDictionary} indexes
 * locations by id, and the concrete location classes each model one acquisition type:
 * {@link rawr.base.items.sources.VendorItem}, {@link rawr.base.items.sources.QuestItem},
 * {@link rawr.base.items.sources.StaticDrop}, {@link rawr.base.items.sources.WorldDrop},
 * {@link rawr.base.items.sources.PvpItem}, {@link rawr.base.items.sources.CraftedItem},
 * {@link rawr.base.items.sources.ContainerItem}, {@link rawr.base.items.sources.FactionItem},
 * {@link rawr.base.items.sources.NoSource} and {@link rawr.base.items.sources.UnknownItem}.</p>
 *
 * <p>{@link rawr.base.items.sources.BindsOn} is the bind-on-pickup/equip/use enum used by both
 * items and vendors' cost rules. Models that price gear (cost-aware optimization) and the UI's
 * item browser read this package; it has no effect on combat math.</p>
 */
package rawr.base.items.sources;
