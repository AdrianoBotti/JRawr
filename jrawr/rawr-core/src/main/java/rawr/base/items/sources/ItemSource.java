// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items.sources;

import rawr.base.items.ItemFilterRegex;

/**
 * The kind of place an item comes from, one constant per {@link ItemLocation} subclass.
 *
 * <p>Values: {@code NotFound} (the item does not exist on the armory), {@code None} (the armory
 * reported {@code sourceType.none}), {@code Unknown} (the source string or its extended data
 * could not be parsed), then the real sources {@code Vendor}, {@code Faction}, {@code PVP},
 * {@code Crafted}, {@code WorldDrop}, {@code StaticDrop}, {@code Quest} and {@code Container}.
 * {@link LocationFactory} maps the {@code sourceType.*} strings of the location data file to
 * these, and {@link ItemFilterRegex} does not look at the source kind at all &mdash; it matches
 * the rendered description.</p>
 */
public enum ItemSource {
    NotFound,           // doesnt exist in the armory
    None,               // sourceType.none
    Unknown,            // Failed to parse the sourcetype or the extended data
    Vendor,
    Faction,
    PVP,
    Crafted,
    WorldDrop,
    StaticDrop,
    Quest,
    Container,
}
