// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the XmlSerializer attributes are omitted (rawr-data owns XML). RegexOptions.Compiled has
// no Java equivalent and is ignored; matching semantics use CASE_INSENSITIVE|UNICODE_CASE|DOTALL.
package rawr.base.items;

import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ItemLocation;

import java.util.regex.Pattern;

/**
 * One node of the item filter tree: a named condition (regular expression plus bind, item-level
 * and quality constraints) that decides whether an item is included or excluded by the user's
 * relevance rules.
 *
 * <h2>Matching</h2>
 *
 * <p>{@link #isMatch(Item)} gates on the item-level and quality ranges first, then on the
 * pattern: an empty pattern matches everything, otherwise the compiled pattern is searched
 * ({@code find}, a substring match, not a full match) in the item's location description and
 * note. When the item has two location sources, the concatenation of both descriptions is also
 * searched with {@code "Purchasable with"} removed from the second one &mdash; a faithful port
 * of the original's formatting hack. The bind rule is applied last: the item's
 * {@link BindsOn} value must correspond to one of the enabled {@code BoA}/{@code BoE}/
 * {@code BoP}/{@code BoU}/{@code BoN} toggles. Porting quirk kept: a non-additive (exclusion)
 * rule whose {@code BoN} toggle is <i>off</i> still matches items that do not bind, because that
 * combination is checked before the regular bind chain.</p>
 *
 * <p>{@link #appliesTo(Item)} restricts a rule to items, gems or both; {@link #isEnabled()} is
 * the rule's own switch, while {@link #isEnabled(Item)} resolves the tree semantics: if any
 * child applies and matches, the node's decision is "at least one matching child is enabled",
 * otherwise the node's {@link #otherRegexEnabled} catch-all flag applies. The root list's rules
 * are evaluated by {@link ItemFilter#isItemRelevant}.</p>
 *
 * <h2>Pattern cache and global state</h2>
 *
 * <p>{@link #getRegex()} lazily compiles the pattern with
 * {@code CASE_INSENSITIVE|UNICODE_CASE|DOTALL} (C# {@code RegexOptions.Compiled} has no Java
 * equivalent and is ignored); setting a new pattern clears it, so pattern syntax is Java regex
 * syntax. The static
 * {@link #isRegexCompiled()}/{@link #setRegexCompiled(boolean)} pair mirrors the original's
 * global "precompile all rules" switch: toggling it resets every top-level rule's compiled
 * pattern through {@link ItemFilter#getFilterList()}. Defaults are fully permissive (all
 * quality tiers and bind types, levels 0 through 1000, additive, applies to items and gems,
 * enabled), so a freshly constructed rule filters nothing until edited.</p>
 */
public class ItemFilterRegex {
    private String name;
    private String pattern;
    private boolean boA;
    private boolean boE;
    private boolean boP;
    private boolean boU;
    private boolean boN;
    private int minItemLevel;
    private int maxItemLevel;
    private ItemQuality minItemQuality;
    private ItemQuality maxItemQuality;
    private boolean additiveFilter;
    private boolean appliesToItems;
    private boolean appliesToGems;
    private boolean enabled;

    public ItemFilterRegexList regexList = new ItemFilterRegexList();
    public boolean otherRegexEnabled = true;

    private Pattern regex;
    private static boolean regexCompiled;

    public ItemFilterRegex() {
        name = "New Filter";
        enabled = true;
        minItemLevel = 0;
        maxItemLevel = 1000;
        minItemQuality = ItemQuality.Temp;
        maxItemQuality = ItemQuality.Heirloom;
        boA = true;
        boE = true;
        boP = true;
        boU = true;
        boN = true;
        additiveFilter = true;
        appliesToItems = true;
        appliesToGems = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String value) {
        this.pattern = value;
        this.regex = null;
    }

    public boolean isBoA() {
        return boA;
    }

    public void setBoA(boolean boA) {
        this.boA = boA;
    }

    public boolean isBoE() {
        return boE;
    }

    public void setBoE(boolean boE) {
        this.boE = boE;
    }

    public boolean isBoP() {
        return boP;
    }

    public void setBoP(boolean boP) {
        this.boP = boP;
    }

    public boolean isBoU() {
        return boU;
    }

    public void setBoU(boolean boU) {
        this.boU = boU;
    }

    public boolean isBoN() {
        return boN;
    }

    public void setBoN(boolean boN) {
        this.boN = boN;
    }

    public int getMinItemLevel() {
        return minItemLevel;
    }

    public void setMinItemLevel(int minItemLevel) {
        this.minItemLevel = minItemLevel;
    }

    public int getMaxItemLevel() {
        return maxItemLevel;
    }

    public void setMaxItemLevel(int maxItemLevel) {
        this.maxItemLevel = maxItemLevel;
    }

    public ItemQuality getMinItemQuality() {
        return minItemQuality;
    }

    public void setMinItemQuality(ItemQuality minItemQuality) {
        this.minItemQuality = minItemQuality;
    }

    public ItemQuality getMaxItemQuality() {
        return maxItemQuality;
    }

    public void setMaxItemQuality(ItemQuality maxItemQuality) {
        this.maxItemQuality = maxItemQuality;
    }

    public boolean isAdditiveFilter() {
        return additiveFilter;
    }

    public void setAdditiveFilter(boolean additiveFilter) {
        this.additiveFilter = additiveFilter;
    }

    public boolean isAppliesToItems() {
        return appliesToItems;
    }

    public void setAppliesToItems(boolean appliesToItems) {
        this.appliesToItems = appliesToItems;
    }

    public boolean isAppliesToGems() {
        return appliesToGems;
    }

    public void setAppliesToGems(boolean appliesToGems) {
        this.appliesToGems = appliesToGems;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static boolean isRegexCompiled() {
        return regexCompiled;
    }

    /**
     * Global "precompile regexes" switch (a UI setting in the original). Toggling it clears the
     * compiled pattern on every top-level rule through {@link ItemFilter#getFilterList()}, so the
     * next match recompiles with the new intent; the value itself does not change matching
     * results, only when compilation happens.
     */
    public static void setRegexCompiled(boolean value) {
        if (regexCompiled != value) {
            regexCompiled = value;
            if (ItemFilter.getFilterList() != null) {
                for (ItemFilterRegex regex : ItemFilter.getFilterList()) {
                    regex.resetRegex();
                }
            }
        }
    }

    private void resetRegex() {
        this.regex = null;
        for (ItemFilterRegex regex : regexList) {
            regex.resetRegex();
        }
    }

    /**
     * The compiled pattern, created on first use and cached; the error-prone options
     * (case-insensitive, unicode-aware case folding, dot matches newline) are hardcoded to the
     * original's regex behavior. A {@code null} pattern is treated as the empty pattern. The
     * pattern is not anchored: matching uses {@link java.util.regex.Matcher#find()}.
     */
    public Pattern getRegex() {
        if (this.regex == null) {
            if (this.pattern == null) this.pattern = "";
            int options = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL;
            this.regex = Pattern.compile(this.pattern, options);
        }
        return this.regex;
    }

    private boolean regexIsMatch(String text) {
        return !isNullOrEmpty(text) && getRegex().matcher(text).find();
    }

    private static boolean isNullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * Whether this rule selects the item: level and quality ranges, then pattern against the
     * location text (see the class comment), then the bind toggles. Used by both inclusion and
     * exclusion tiers.
     */
    public boolean isMatch(Item item) {
        // all checks must pass for match to pass, so do the fast checks first
        if (item.getItemLevel() >= minItemLevel && item.getItemLevel() <= maxItemLevel) {
            if (item.getQuality().value() >= minItemQuality.value() && item.getQuality().value() <= maxItemQuality.value()) {
                ItemLocation[] locationInfo = item.getLocationInfo();
                if (isNullOrEmpty(this.pattern)
                        || (regexIsMatch(locationInfo[0].getDescription()))
                        || (regexIsMatch(locationInfo[0].getNote()))
                        || ((locationInfo[0] != null && locationInfo[1] != null)
                            && (!isNullOrEmpty(locationInfo[0].getDescription()) && !isNullOrEmpty(locationInfo[1].getDescription()))
                            && getRegex().matcher(locationInfo[0].getDescription() + " and" + locationInfo[1].getDescription().replace("Purchasable with", "")).find())
                ) {
                    if (!boN && !additiveFilter && item.getBind() == BindsOn.None) return true;
                    else if (boA && item.getBind() == BindsOn.BoA) return true;
                    else if (boE && item.getBind() == BindsOn.BoE) return true;
                    else if (boP && item.getBind() == BindsOn.BoP) return true;
                    else if (boU && item.getBind() == BindsOn.BoU) return true;
                    else if (boN && item.getBind() == BindsOn.None) return true;
                }
            }
        }
        return false;
    }

    /**
     * Scope check: a gem is only subject to the rule when {@link #isAppliesToGems()} is set, a
     * non-gem item only when {@link #isAppliesToItems()} is set.
     */
    public boolean appliesTo(Item item) {
        return (item.isGem() && appliesToGems) || (!item.isGem() && appliesToItems);
    }

    /**
     * Tree-aware enabled decision for an item: a disabled node is always off; a leaf (no child
     * rules) is on; otherwise, if any child applies and matches, the result is whether at least
     * one matching child is recursively enabled, and if no child matches, the node's
     * {@link #otherRegexEnabled} catch-all flag decides. {@link ItemFilter#isItemRelevant} calls
     * this for the top-level rules.
     */
    public boolean isEnabled(Item item) {
        if (!enabled) return false;
        if (regexList.size() == 0) return true;
        boolean enabledMatch = false;
        boolean anyMatch = false;
        for (ItemFilterRegex regex : regexList) {
            if (regex.appliesTo(item) && regex.isMatch(item)) {
                anyMatch = true;
                if (regex.isEnabled(item)) {
                    enabledMatch = true;
                    break;
                }
            }
        }
        if (anyMatch) {
            return enabledMatch;
        } else {
            return otherRegexEnabled;
        }
    }
}
