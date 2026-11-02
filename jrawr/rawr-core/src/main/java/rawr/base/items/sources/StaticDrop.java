// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory XML parsing) is omitted.
package rawr.base.items.sources;

import java.util.Locale;

/**
 * A location whose item drops from a specific named creature (a boss or rare) in an area,
 * possibly only on heroic difficulty.
 *
 * <p>Fields {@link #getBoss()}, {@link #getArea()} and {@link #isHeroic()}; the description is
 * {@code "Drops from <boss> in [Heroic] <area>"}.</p>
 */
public class StaticDrop extends ItemLocation {
    private String area;
    private boolean heroic;
    private String boss;

    public StaticDrop() {
        setSource(ItemSource.StaticDrop);
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public boolean isHeroic() {
        return heroic;
    }

    public void setHeroic(boolean heroic) {
        this.heroic = heroic;
    }

    public String getBoss() {
        return boss;
    }

    public void setBoss(String boss) {
        this.boss = boss;
    }

    @Override
    public String getDescription() {
        return String.format(Locale.ROOT, "Drops from %s in %s%s", boss, heroic ? "Heroic " : "", area);
    }

    public static ItemLocation construct() {
        return new StaticDrop();
    }
}
