// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory download + token-name lookup) is omitted.
package rawr.base.items.sources;

import rawr.base.items.Item;

import java.util.Locale;

/**
 * A location whose item is bought with player-versus-player currencies, typically from an arena
 * or battleground vendor.
 *
 * <p>Fields: {@link #getPointType()}/{@link #getPoints()} the main currency (e.g. arena points),
 * {@link #getPointTypeA()}/{@link #getPointsA()} an optional second currency,
 * {@link #getTokenType()}/{@link #getTokenCount()} an optional token payment, and
 * {@link #getRequiredArenaRating()} the arena rating needed to buy it;
 * {@link #getVendorName()}/{@link #getVendorArea()} locate the seller. The description picks the
 * matching combination of those. Porting quirk kept: in the "points plus tokens" branch the
 * original's format string prints the point type where the token type would be expected, and
 * this port reproduces that text exactly.</p>
 */
public class PvpItem extends ItemLocation {
    private String pointType;
    private int points;
    private String tokenType;
    private int tokenCount;
    private String pointTypeA;
    private int pointsA;
    private int requiredArenaRating;
    private String vendorName;
    private String vendorArea;

    public PvpItem() {
        setSource(ItemSource.PVP);
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getPointTypeA() {
        return pointTypeA;
    }

    public void setPointTypeA(String pointTypeA) {
        this.pointTypeA = pointTypeA;
    }

    public int getPointsA() {
        return pointsA;
    }

    public void setPointsA(int pointsA) {
        this.pointsA = pointsA;
    }

    public int getRequiredArenaRating() {
        return requiredArenaRating;
    }

    public void setRequiredArenaRating(int requiredArenaRating) {
        this.requiredArenaRating = requiredArenaRating;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorArea() {
        return vendorArea;
    }

    public void setVendorArea(String vendorArea) {
        this.vendorArea = vendorArea;
    }

    @Override
    public String getDescription() {
        String rar = requiredArenaRating > 0 ? " and Requires an Arena Rating of " + requiredArenaRating : "";
        if (tokenCount > 0 && pointsA > 0 && points > 0) {
            return String.format(Locale.ROOT, "Purchasable for %d %s Points, %d %s Points and %d [%s]%s", points, pointType, pointsA, pointTypeA, tokenCount, tokenType, rar);
        } else if (tokenCount > 0 && points > 0) {
            return String.format(Locale.ROOT, "Purchasable for %d %s Points and %s [%d]%s", points, pointType, pointType, tokenCount, rar);
        } else if (tokenCount > 0) {
            return String.format(Locale.ROOT, "Purchasable for %d [%s]%s", tokenCount, tokenType, rar);
        } else if (pointsA > 0 && points > 0) {
            return String.format(Locale.ROOT, "Purchasable for %d %s Points and %d %s Points%s", points, pointType, pointsA, pointTypeA, rar);
        } else if (points > 0) {
            return String.format(Locale.ROOT, "Purchasable for %d %s Points%s", points, pointType, rar);
        } else if (vendorName != null) {
            return String.format(Locale.ROOT, "PvP item sold by %s at %s%s", vendorName, vendorArea, rar);
        }
        return "Purchasable PvP Item" + rar;
    }

    public static ItemLocation construct() {
        return new PvpItem();
    }
}
