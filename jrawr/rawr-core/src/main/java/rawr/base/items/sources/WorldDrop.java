// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory XML parsing) is omitted.
package rawr.base.items.sources;

import java.util.Locale;

/**
 * A location whose item drops randomly from ordinary enemies ("trash") in an area rather than
 * from a specific boss.
 *
 * <p>Fields {@link #getLocation()} and {@link #isHeroic()}; the description is
 * {@code "Trash drop in [Heroic] <location>"}, or {@code "World Drop"} when the location is
 * unknown.</p>
 */
public class WorldDrop extends ItemLocation {
    private String location;
    private boolean heroic;

    public WorldDrop() {
        setSource(ItemSource.WorldDrop);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isHeroic() {
        return heroic;
    }

    public void setHeroic(boolean heroic) {
        this.heroic = heroic;
    }

    @Override
    public String getDescription() {
        if (location != null) {
            return String.format(Locale.ROOT, "Trash drop in %s%s", heroic ? "Heroic " : "", location);
        }
        return "World Drop";
    }

    public static ItemLocation construct() {
        return new WorldDrop();
    }
}
