// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/Priority.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

public class Priority implements Comparable<Priority> {
    private EnhanceAbility abilityType;
    private String priorityName;
    private String description;
    private boolean inUse;
    private int priority;
    private String enhSimName;

    public Priority() {
        abilityType = EnhanceAbility.None;
        priorityName = "None";
        description = "Empty Priority";
        inUse = false;
        priority = 0;
        enhSimName = "XX";
    }

    public Priority(String priorityName, EnhanceAbility abilityType, String description, boolean onByDefault, int priority, String enhSimName) {
        this.priorityName = priorityName;
        this.abilityType = abilityType;
        this.description = description;
        this.inUse = onByDefault;
        this.priority = priority;
        this.enhSimName = enhSimName;
    }

    public String getPriorityName() {
        return priorityName;
    }

    public void setPriorityName(String value) {
        priorityName = value;
    }

    public EnhanceAbility getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(EnhanceAbility value) {
        abilityType = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public int getPriorityValue() {
        return priority;
    }

    public void setPriorityValue(int value) {
        priority = value;
    }

    public boolean getChecked() {
        return inUse;
    }

    public void setChecked(boolean value) {
        inUse = value;
    }

    public String getEnhSimName() {
        return enhSimName;
    }

    @Override
    public String toString() {
        return priorityName;
    }

    @Override
    public int compareTo(Priority other) {
        return Integer.compare(priority, other.priority);
    }
}
