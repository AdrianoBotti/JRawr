// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import rawr.base.items.sources.FactionItem;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.sources.LocationFactory;

/**
 * The eight reputation tiers of the game, ordered from most hostile to most friendly:
 * {@code Hated}, {@code Hostile}, {@code Unfriendly}, {@code Neutral}, {@code Friendly},
 * {@code Honored}, {@code Revered}, {@code Exalted}.
 *
 * <p>An item's source can require a minimum standing with a faction (see {@link FactionItem},
 * {@link LocationFactory} and the rawr-data source loaders). The declaration order matches the
 * in-game progression, so "at least Honored" can be checked by comparing ordinals, and the
 * constant names are also the strings written to and parsed from item-source XML.</p>
 */
public enum ReputationLevel {
    Hated,
    Hostile,
    Unfriendly,
    Neutral,
    Friendly,
    Honored,
    Revered,
    Exalted,
}
