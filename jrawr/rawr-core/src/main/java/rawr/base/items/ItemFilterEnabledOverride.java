// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.character.Character;

import java.util.List;

/**
 * A per-character snapshot of one filter rule's enabled state, including its children, so a
 * saved character can remember which filters that character had turned on or off.
 *
 * <p>{@code Character} save builds a parallel tree of these from the live {@link ItemFilterRegex}
 * tree ({@link #getName()} identifies the rule, {@link #isEnabled()} its state,
 * {@link #getSubFilterOverride()} the descendants, plus a synthetic entry for the node's
 * {@code otherRegexEnabled} flag), and load walks the live tree and applies them back. The class
 * is a plain mutable bean with no behavior of its own; nesting mirrors the filter tree.</p>
 */
public class ItemFilterEnabledOverride {
    private String name;
    private boolean enabled;
    private List<ItemFilterEnabledOverride> subFilterOverride;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ItemFilterEnabledOverride> getSubFilterOverride() {
        return subFilterOverride;
    }

    public void setSubFilterOverride(List<ItemFilterEnabledOverride> subFilterOverride) {
        this.subFilterOverride = subFilterOverride;
    }
}
