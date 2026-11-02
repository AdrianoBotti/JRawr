// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemLocation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Fill (armory download + material bind lookup) is omitted.
package rawr.base.items.sources;

import java.util.LinkedHashMap;

import java.util.Locale;

/**
 * A location whose item is produced by a profession (its "created by spell") rather than dropped
 * or sold.
 *
 * <p>Fields: {@link #getSkill()} the profession name, {@link #getLevel()} the skill level
 * required to craft it, {@link #getBind()} the bind rule, {@link #getSpellName()} the crafting
 * spell, and {@link #getBopMats()} a map of bind-on-pickup material name to count (materials
 * that cannot simply be bought). The description uses those fields, e.g.
 * {@code "BoP crafted Tailoring(400) using 2 Frozen Orbs"}; a skill literally named
 * {@code "Unknown"} switches to {@code "Created via <spell>"}. The constructor is private;
 * {@link #construct()} is the factory {@link LocationFactory} registers.</p>
 */
public class CraftedItem extends ItemLocation {
    private String skill;
    private int level;
    private BindsOn bind = BindsOn.None;
    private String spellName;
    private LinkedHashMap<String, Integer> bopMats = new LinkedHashMap<String, Integer>();

    private CraftedItem() {
        setSource(ItemSource.Crafted);
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public BindsOn getBind() {
        return bind;
    }

    public void setBind(BindsOn bind) {
        this.bind = bind;
    }

    public String getSpellName() {
        return spellName;
    }

    public void setSpellName(String spellName) {
        this.spellName = spellName;
    }

    public LinkedHashMap<String, Integer> getBopMats() {
        return bopMats;
    }

    @Override
    public String getDescription() {
        if ("Unknown".equals(skill)) {
            if (spellName != null) {
                return String.format(Locale.ROOT, "Created via %s", spellName);
            }
            return "Crafted";
        }
        StringBuilder basic = new StringBuilder();

        if (bind != BindsOn.None) {
            if (level != 0) {
                basic.append(bind).append(" crafted ").append(skill).append("(").append(level).append(") ");
            } else {
                basic.append(bind).append(" crafted ").append(skill).append(" ");
            }
        } else {
            if (level != 0) {
                basic.append("Crafted ").append(skill).append("(").append(level).append(")");
            } else {
                basic.append("Crafted ").append(skill);
            }
        }
        if (bopMats.size() > 0) {
            basic.append(" using ");
            for (String key : bopMats.keySet()) {
                basic.append(bopMats.get(key)).append(" ").append(key);
            }
        }
        return basic.toString();
    }

    public static ItemLocation construct() {
        return new CraftedItem();
    }
}
