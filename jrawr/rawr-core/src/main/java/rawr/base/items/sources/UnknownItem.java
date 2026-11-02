// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items.sources;

/**
 * A location placeholder meaning "the item was not found on the armory": source
 * {@link ItemSource#NotFound}, description fixed to {@code "Not found on armory"}.
 *
 * <p>Distinct from {@link NoSource}, which means the item was found but the armory knows no
 * acquisition method. Used when a location lookup cannot identify the item at all.</p>
 */
public class UnknownItem extends ItemLocation {
    public UnknownItem() {
        super("");
        setSource(ItemSource.NotFound);
    }

    @Override
    public String getDescription() {
        return "Not found on armory";
    }

    public static ItemLocation construct() {
        return new UnknownItem();
    }
}
