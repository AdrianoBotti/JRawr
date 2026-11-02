// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory XML parsing) is omitted.
package rawr.base.items.sources;

import java.util.Locale;

/**
 * A location whose item is a reward from a quest.
 *
 * <p>Fields: {@link #getQuest()} the quest name, {@link #getArea()} the zone,
 * {@link #getType()} the quest's category text (rendered when no group size is set),
 * {@link #getMinLevel()} the minimum level, and {@link #getParty()} the required group size
 * (rendered with a {@code g} suffix when non-zero). Description format:
 * {@code "Reward from [<minLevel><g|type><party>] <quest> in <area>"}. The constructor is
 * private; {@link #construct()} is the factory for {@link LocationFactory}.</p>
 */
public class QuestItem extends ItemLocation {
    private String area;
    private String quest;
    private int minLevel;
    private int party;
    private String type;

    private QuestItem() {
        setSource(ItemSource.Quest);
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getQuest() {
        return quest;
    }

    public void setQuest(String quest) {
        this.quest = quest;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getDescription() {
        return String.format(Locale.ROOT, "Reward from [%s%s%s] %s in %s",
                minLevel > 0 ? Integer.toString(minLevel) : "",
                party > 0 ? "g" : type,
                party > 0 ? Integer.toString(party) : "",
                quest,
                area);
    }

    public static ItemLocation construct() {
        return new QuestItem();
    }
}
