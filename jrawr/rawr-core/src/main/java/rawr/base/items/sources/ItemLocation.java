// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the armory/network Fill members are omitted; the data model, descriptions and the
// in-memory location table are ported.
package rawr.base.items.sources;

import rawr.base.items.Item;
import rawr.base.items.ItemFilter;

/**
 * One place an item comes from: a source kind plus a human-readable description, as shown in the
 * item tooltip and matched by the {@link ItemFilter} regex rules.
 *
 * <p>{@link Item#getLocationInfo()} returns an array of up to two locations for an item (the
 * primary source and an optional second one, e.g. the other faction's or difficulty's version).
 * Concrete subclasses &mdash; one per source type ({@link VendorItem}, {@link QuestItem},
 * {@link StaticDrop}, {@link PvpItem}, {@link FactionItem}, {@link CraftedItem},
 * {@link ContainerItem}, {@link WorldDrop}, {@link NoSource}, {@link UnknownItem}) &mdash; add
 * the fields that source needs and override {@link #getDescription()}; each exposes a static
 * {@code construct()} factory that {@link LocationFactory} registers against the data file's
 * {@code sourceType.*} strings.</p>
 *
 * <p>The base description is {@code "Unknown location"} and the base source is
 * {@link ItemSource#Unknown}; the base class itself is used when a saved location entry has no
 * recognizable subtype. A free-form {@link #getNote() note} survives alongside the
 * description.</p>
 *
 * @see LocationFactory
 * @see ItemSource
 */
public class ItemLocation {
    private ItemSource source;
    private String note;
    private String description;

    public ItemLocation() {
        this("Unknown location");
    }

    public ItemLocation(String desc) {
        source = ItemSource.Unknown;
        this.description = desc;
    }

    public ItemSource getSource() {
        return source;
    }

    public void setSource(ItemSource source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDescription() {
        return this.description;
    }

    public static ItemLocation construct() {
        return new ItemLocation("");
    }
}
