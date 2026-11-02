// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items.sources;

/**
 * A location placeholder for items the armory reports as {@code sourceType.none}: the item exists
 * but its acquisition method is unknown, so the description tells the user to check Wowhead.
 *
 * <p>Source {@link ItemSource#None}; unlike {@link UnknownItem} the item was found, there is just
 * no source data.</p>
 */
public class NoSource extends ItemLocation {
    public NoSource() {
        super("");
        setSource(ItemSource.None);
    }

    @Override
    public String getDescription() {
        return "Armory reports no source, try Wowhead";
    }

    public static ItemLocation construct() {
        return new NoSource();
    }
}
