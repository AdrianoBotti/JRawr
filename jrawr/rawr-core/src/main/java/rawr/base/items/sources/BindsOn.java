// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items.sources;

import rawr.base.items.Item;
import rawr.base.items.ItemFilterRegex;

/**
 * When an item becomes permanently bound to its owner, as recorded by the item data.
 *
 * <p>Values: {@code None} (never binds), {@code BoP} (bind on pickup: cannot be traded once
 * looted), {@code BoE} (bind on equip: tradeable until worn), {@code BoA} (bind on account:
 * heirlooms, shareable between the owner's characters) and {@code BoU} (bind on use: binds when
 * its on-use effect is activated). {@link Item#getBind()} holds the value; {@link ItemFilterRegex}
 * exposes one toggle per value ({@code BoP}, {@code BoE}, ...), and {@link CraftedItem} repeats
 * the bind rule for its trade-skill context.</p>
 */
public enum BindsOn {
    None,
    BoP,
    BoE,
    BoA,
    BoU,
}
