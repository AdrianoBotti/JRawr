// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory XML parsing) is omitted.
package rawr.base.items.sources;

import java.util.Locale;

/**
 * A location whose item is found inside a container object in the world &mdash; a chest, a
 * lockbox, or the reward bag of a specific encounter.
 *
 * <p>Fields: {@link #getContainer()} the object or bag name, {@link #getArea()} the zone or
 * instance, {@link #isHeroic()} the difficulty flag, plus any {@link #getMinLevel() minimum
 * level} and {@link #getParty() group size} requirement. The description renders as
 * {@code "Found in <container> in [Heroic] <area>"}. The armory parsing ({@code Fill}) is
 * omitted; instances are built from the location data file through {@link #construct()}.</p>
 */
public class ContainerItem extends ItemLocation {
    private boolean heroic;
    private String area;
    private String container;
    private int minLevel;
    private int party;

    public ContainerItem() {
        setSource(ItemSource.Container);
    }

    public boolean isHeroic() {
        return heroic;
    }

    public void setHeroic(boolean heroic) {
        this.heroic = heroic;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getParty() {
        return party;
    }

    public void setParty(int party) {
        this.party = party;
    }

    @Override
    public String getDescription() {
        return String.format(Locale.ROOT, "Found in %s in %s%s", container, heroic ? "Heroic " : "", area);
    }

    public static ItemLocation construct() {
        return new ContainerItem();
    }
}
