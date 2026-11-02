// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory download + token-name lookup) is omitted.
package rawr.base.items.sources;

import java.util.LinkedHashMap;

import java.util.Locale;

/**
 * A location whose item is sold by an ordinary vendor for money and/or extra currencies.
 *
 * <p>Fields: {@link #getCost()} in copper (100 copper = 1 silver, 10000 copper = 1 gold),
 * {@link #getTokenMap()} extra currencies keyed by token name, and
 * {@link #getVendorName()}/{@link #getVendorArea()} to locate the seller.
 * {@link #getCostString()} renders the copper amount (e.g. {@code 12g34s56c}) and returns an
 * empty string for zero; {@link #getDescription()} lists the tokens first ("and"-joined), then
 * the cost, and falls back to the vendor name when neither is set.</p>
 */
public class VendorItem extends ItemLocation {
    private LinkedHashMap<String, Integer> tokenMap = new LinkedHashMap<String, Integer>();
    private int cost;
    private String vendorName;
    private String vendorArea;

    public VendorItem() {
        setSource(ItemSource.Vendor);
    }

    public LinkedHashMap<String, Integer> getTokenMap() {
        return tokenMap;
    }

    public void setTokenMap(LinkedHashMap<String, Integer> value) {
        tokenMap = value;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
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

    public String getCostString() {
        int total = cost;
        int gold = total / 10000;
        total -= gold * 10000;
        int silver = total / 100;
        total -= silver * 100;

        if (gold > 0) {
            return String.format(Locale.ROOT, "%dg%ds%dc", gold, silver, total);
        } else if (silver > 0) {
            return String.format(Locale.ROOT, "%ds%dc", silver, total);
        } else if (total > 0) {
            return String.format(Locale.ROOT, "%dc", total);
        }
        return "";
    }

    @Override
    public String getDescription() {
        if (tokenMap.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String key : tokenMap.keySet()) {
                if (!first) {
                    sb.append("and ");
                }
                sb.append(tokenMap.get(key)).append(" [").append(key).append("] ");
                first = false;
            }
            if (cost > 0) {
                return "Purchasable with " + sb + "and " + getCostString();
            } else {
                return "Purchasable with " + sb;
            }
        } else if (cost > 0) {
            return "Purchasable with " + getCostString();
        } else {
            return String.format(Locale.ROOT, "Sold by %s at %s", vendorName, vendorArea);
        }
    }

    public static ItemLocation construct() {
        return new VendorItem();
    }
}
