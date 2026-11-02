// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory download + token-name lookup) is omitted.
package rawr.base.items.sources;

import java.util.LinkedHashMap;
import rawr.base.character.ReputationLevel;

import java.util.Locale;

/**
 * A location whose item is bought from a faction vendor and requires a reputation level with that
 * faction.
 *
 * <p>Fields: {@link #getFactionName()}, {@link #getLevel()} (a {@link ReputationLevel} from
 * Hated to Exalted), {@link #getCost()} in copper (100 copper = 1 silver, 10000 copper = 1 gold)
 * and {@link #getTokenMap()}, extra currencies keyed by token name. {@link #getCostString()}
 * renders the copper amount as e.g. {@code 12g34s56c}; {@link #getDescription()} combines tokens,
 * cost and reputation requirement. The C# armory download ({@code Fill}) is omitted;
 * {@link #construct()} serves {@link LocationFactory}.</p>
 */
public class FactionItem extends ItemLocation {
    private LinkedHashMap<String, Integer> tokenMap = new LinkedHashMap<String, Integer>();
    private String factionName;
    private ReputationLevel level = ReputationLevel.Hated;
    private int cost;

    public FactionItem() {
        setSource(ItemSource.Faction);
    }

    public String getFactionName() {
        return factionName;
    }

    public void setFactionName(String factionName) {
        this.factionName = factionName;
    }

    public ReputationLevel getLevel() {
        return level;
    }

    public void setLevel(ReputationLevel level) {
        this.level = level;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public LinkedHashMap<String, Integer> getTokenMap() {
        return tokenMap;
    }

    public void setTokenMap(LinkedHashMap<String, Integer> value) {
        tokenMap = value;
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
        }
        return String.format(Locale.ROOT, "%dc", total);
    }

    @Override
    public String getDescription() {
        if (tokenMap.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String key : tokenMap.keySet()) {
                sb.append(tokenMap.get(key)).append(" [").append(key).append("] ");
            }
            if (cost > 0) {
                return String.format(Locale.ROOT, "Purchasable for %s%s and requires %s - %s", sb, getCostString(), factionName, level);
            } else {
                return String.format(Locale.ROOT, "Purchasable for %s and requires %s - %s", sb, factionName, level);
            }
        } else if (cost > 0) {
            return String.format(Locale.ROOT, "Purchasable for %s and requires %s - %s", getCostString(), factionName, level);
        } else {
            return String.format(Locale.ROOT, "Purchasable and requires %s - %s", factionName, level);
        }
    }

    public static ItemLocation construct() {
        return new FactionItem();
    }
}
