// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items.sources;

import java.util.LinkedHashMap;

/**
 * The serialized item-location table: item id (as a string) mapped to the array of
 * {@link ItemLocation}s for that item.
 *
 * <p>An insertion-ordered {@link LinkedHashMap} so save files keep the original item order. The
 * legacy XML shape (root {@code <dictionary>}, one {@code <item>} per key with the id in
 * {@code <key>/<string>} and an {@code ArrayOfItemLocation} value) is written and read by
 * {@link LocationFactory}. The live instance is {@code LocationFactory}'s private table; this
 * type names the round-trip shape.</p>
 */
public class ItemLocationDictionary extends LinkedHashMap<String, ItemLocation[]> {
}
