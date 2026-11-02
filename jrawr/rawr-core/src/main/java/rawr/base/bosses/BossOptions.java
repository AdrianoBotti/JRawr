// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (reported): OnPropertyChanged raises (WPF-bound INotifyPropertyChanged).
package rawr.base.bosses;

import rawr.base.util.NetFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The encounter settings a character carries: which boss from {@link BossList} is simulated and
 * which of that boss's mechanics the selected model should take into account.
 *
 * <p>{@code BossOptions} extends {@link BossHandler}, so it stores the complete boss description
 * (see that class for the meaning and units of every field). On top of that it holds:</p>
 *
 * <ul>
 *   <li><b>Selection</b> &mdash; {@link #getFilterType()}, {@link #getFilter()} and
 *       {@link #getBossName()} record how the boss was chosen in the browser
 *       ({@link BossList.FilterType}: by content tier, instance, version or name), and
 *       {@link #cloneThis(BossHandler)} copies a canonical {@link BossHandler} definition into
 *       this object. The saved character file serializes this copy; {@link BossList} resolves the
 *       name back to the canonical definition when the file is loaded.</li>
 *   <li><b>Feature toggles</b> &mdash; one boolean per mechanic group says whether the model
 *       should include it: {@link #getInBack()} (may the player attack from behind?),
 *       {@link #getMultiTargs()} (are add waves relevant?), {@link #getStunningTargs()},
 *       {@link #getMovingTargs()}, {@link #getFearingTargs()}, {@link #getRootingTargs()},
 *       {@link #getDisarmingTargs()} and {@link #getDamagingTargs()} (should the boss's damage be
 *       modelled?). A model that cannot express a mechanic for its role turns the flag off
 *       (e.g. a ranged model ignores the melee rear-position fraction).</li>
 * </ul>
 *
 * <p>{@link #getUnder35Perc()} and {@link #getUnder20Perc()} are the fractions of the fight the
 * boss spends below 35% / 20% health, the so-called "execute range". Abilities that only work, or
 * work better, against a low-health target multiply their value by these fractions. They are
 * user-chosen estimates, not derived from the boss's actual health curve.</p>
 *
 * <p>{@link #getMyModelSupportsThis()} maps each simulation model's name (such as {@code "Mage"},
 * {@code "ProtWarr"} or {@code "HealPriest"}) to the subset of encounter sections that model can
 * use; the UI uses it to disable irrelevant controls. The map is built once from
 * {@link #duplicateDefaultSupports()} and then customized per role (melee damage dealers, ranged
 * damage dealers, tanks, healers).</p>
 *
 * <p>{@link #writeXml(org.w3c.dom.Document, org.w3c.dom.Element)} writes the boss description
 * (via {@link BossHandler#writeXml(org.w3c.dom.Document, org.w3c.dom.Element)}) plus the toggles,
 * execute-range fractions and selection fields.</p>
 */
public class BossOptions extends BossHandler {
    private static final Map<String, Boolean> DefaultSupports = new LinkedHashMap<String, Boolean>();

    static {
        DefaultSupports.put("Level", true);
        DefaultSupports.put("Armor", true);
        DefaultSupports.put("Timers", true);
        DefaultSupports.put("Health", true);
        DefaultSupports.put("TimeSub35", true);
        DefaultSupports.put("TimeSub20", true);
        DefaultSupports.put("InBack_Melee", true);
        DefaultSupports.put("InBack_Ranged", true);
        DefaultSupports.put("RaidSize", true);
        DefaultSupports.put("TargetGroups", true);
        DefaultSupports.put("Attacks", true);
        DefaultSupports.put("Defensive", true);
        DefaultSupports.put("Moves", true);
        DefaultSupports.put("Stuns", true);
        DefaultSupports.put("Fears", true);
        DefaultSupports.put("Roots", true);
        DefaultSupports.put("Disarms", true);
        DefaultSupports.put("Invulnerables", true);
    }

    protected static Map<String, Boolean> duplicateDefaultSupports() {
        Map<String, Boolean> retVal = new LinkedHashMap<String, Boolean>();
        for (String key : DefaultSupports.keySet()) {
            retVal.put(key, DefaultSupports.get(key));
        }
        return retVal;
    }

    private static Map<String, Map<String, Boolean>> myModelSupportsThis = null;

    public static Map<String, Map<String, Boolean>> getMyModelSupportsThis() {
        if (myModelSupportsThis == null) {
            myModelSupportsThis = new LinkedHashMap<String, Map<String, Boolean>>();
            { // Melee DPS
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("InBack_Ranged", false);
                myModelSupportsThis.put("Cat", custom);
                myModelSupportsThis.put("DPSDK", custom);
                myModelSupportsThis.put("Enhance", custom);
                myModelSupportsThis.put("Rogue", custom);
                myModelSupportsThis.put("Retribution", custom);
            }
            { // Ranged DPS
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("InBack_Melee", false);
                myModelSupportsThis.put("Hunter", custom);
                myModelSupportsThis.put("Elemental", custom);
                myModelSupportsThis.put("Mage", custom);
                myModelSupportsThis.put("ShadowPriest", custom);
                myModelSupportsThis.put("Warlock", custom);
            }
            { // Tanks
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("InBack_Melee", false);
                custom.put("InBack_Ranged", false);
                myModelSupportsThis.put("Bear", custom);
                myModelSupportsThis.put("ProtWarr", custom);
                myModelSupportsThis.put("TankDK", custom);
            }
            { // Heals
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("InBack_Melee", false);
                custom.put("InBack_Ranged", false);
                custom.put("Level", false);
                custom.put("Armor", false);
                custom.put("Defensive", false);
                custom.put("Invulnerables", false);
                custom.put("TimeSub35", false);
                custom.put("TimeSub20", false);
                myModelSupportsThis.put("HealPriest", custom);
                myModelSupportsThis.put("RestoSham", custom);
                myModelSupportsThis.put("Tree", custom);
            }
            { // DPSWarr
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("InBack_Ranged", false);
                custom.put("TimeSub35", false);
                myModelSupportsThis.put("DPSWarr", custom);
            }
            { // ProtPaladin
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("Timers", false);
                custom.put("Health", false);
                custom.put("TimeSub35", false);
                custom.put("TimeSub20", false);
                custom.put("InBack_Melee", false);
                custom.put("InBack_Ranged", false);
                custom.put("RaidSize", false);
                custom.put("TargetGroups", false);
                custom.put("Attacks", false);
                custom.put("Defensive", false);
                custom.put("Moves", false);
                custom.put("Stuns", false);
                custom.put("Fears", false);
                custom.put("Roots", false);
                custom.put("Disarms", false);
                custom.put("Invulnerables", false);
                myModelSupportsThis.put("ProtPaladin", custom);
            }
            { // Healadin
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("Level", false);
                custom.put("Armor", false);
                custom.put("Health", false);
                custom.put("TimeSub35", false);
                custom.put("TimeSub20", false);
                custom.put("InBack_Melee", false);
                custom.put("InBack_Ranged", false);
                custom.put("RaidSize", false);
                custom.put("TargetGroups", false);
                custom.put("Attacks", false);
                custom.put("Defensive", false);
                custom.put("Moves", false);
                custom.put("Stuns", false);
                custom.put("Fears", false);
                custom.put("Roots", false);
                custom.put("Disarms", false);
                custom.put("Invulnerables", false);
                myModelSupportsThis.put("Healadin", custom);
            }
            { // Moonkin
                Map<String, Boolean> custom = duplicateDefaultSupports();
                custom.put("Health", false);
                custom.put("TimeSub35", false);
                custom.put("TimeSub20", false);
                custom.put("InBack_Melee", false);
                custom.put("InBack_Ranged", false);
                custom.put("RaidSize", false);
                custom.put("TargetGroups", false);
                custom.put("Attacks", false);
                custom.put("Defensive", false);
                custom.put("Roots", false);
                custom.put("Disarms", false);
                myModelSupportsThis.put("Moonkin", custom);
            }
        }
        return myModelSupportsThis;
    }

    private boolean inBack = false;
    private boolean multiTargs = false;
    private boolean stunningTargs = false;
    private boolean movingTargs = false;
    private boolean fearingTargs = false;
    private boolean rootingTargs = false;
    private boolean disarmingTargs = false;
    private boolean damagingTargs = false;

    private double under35Perc = 0.10d;
    private double under20Perc = 0.15d;

    private BossList.FilterType filterType = BossList.FilterType.Content;
    private String filter = "";
    private String bossName = "";

    public BossOptions() {
    }

    @Override
    public BossOptions clone() {
        return (BossOptions) memberwiseClone();
    }

    /**
     * Copies a canonical boss definition into this options object and re-derives the mechanic
     * toggles from it: the toggle is on when the copied boss actually has the corresponding
     * targets/moves/stuns/fears/roots/disarms/attacks defined. The boss's own lists are taken
     * over by reference from a clone of the source.
     */
    public BossOptions cloneThis(BossHandler toClone) {
        BossHandler clone = toClone.clone();
        setName(clone.getName());
        setContent(clone.getContent());
        setInstance(clone.getInstance());
        setVersion(clone.getVersion());
        setComment(clone.getComment());
        setLevel(clone.getLevel());
        setArmor(clone.getArmor());
        setHealth(clone.getHealth());
        setBerserkTimer(clone.getBerserkTimer());
        setSpeedKillTimer(clone.getSpeedKillTimer());
        setInBackPercMelee(clone.getInBackPercMelee());
        setInBackPercRanged(clone.getInBackPercRanged());
        setInBack(getInBackPercMelee() + getInBackPercRanged() > 0f);
        setMaxPlayers(clone.getMaxPlayers());
        setMinHealers(clone.getMinHealers());
        setMinTanks(clone.getMinTanks());
        targets = clone.targets;
        setMultiTargs(targets != null && targets.size() > 0);
        setDoTs(clone.getDoTs());
        setAttacks(clone.getAttacks());
        setDamagingTargs(getAttacks() != null && getAttacks().size() > 0);
        setResistPhysical(clone.getResistPhysical());
        setResistFrost(clone.getResistFrost());
        setResistFire(clone.getResistFire());
        setResistNature(clone.getResistNature());
        setResistArcane(clone.getResistArcane());
        setResistShadow(clone.getResistShadow());
        setResistHoly(clone.getResistHoly());
        moves = clone.moves;
        setMovingTargs(moves != null && moves.size() > 0);
        stuns = clone.stuns;
        setStunningTargs(stuns != null && stuns.size() > 0);
        fears = clone.fears;
        setFearingTargs(fears != null && fears.size() > 0);
        roots = clone.roots;
        setRootingTargs(roots != null && roots.size() > 0);
        disarms = clone.disarms;
        setDisarmingTargs(disarms != null && disarms.size() > 0);
        setTimeBossIsInvuln(clone.getTimeBossIsInvuln());
        return this;
    }

    public boolean getInBack() {
        return inBack;
    }

    public void setInBack(boolean value) {
        inBack = value;
    }

    public boolean getMultiTargs() {
        return multiTargs;
    }

    public void setMultiTargs(boolean value) {
        multiTargs = value;
    }

    public boolean getStunningTargs() {
        return stunningTargs;
    }

    public void setStunningTargs(boolean value) {
        stunningTargs = value;
    }

    public boolean getMovingTargs() {
        return movingTargs;
    }

    public void setMovingTargs(boolean value) {
        movingTargs = value;
    }

    public boolean getFearingTargs() {
        return fearingTargs;
    }

    public void setFearingTargs(boolean value) {
        fearingTargs = value;
    }

    public boolean getRootingTargs() {
        return rootingTargs;
    }

    public void setRootingTargs(boolean value) {
        rootingTargs = value;
    }

    public boolean getDisarmingTargs() {
        return disarmingTargs;
    }

    public void setDisarmingTargs(boolean value) {
        disarmingTargs = value;
    }

    public boolean getDamagingTargs() {
        return damagingTargs;
    }

    public void setDamagingTargs(boolean value) {
        damagingTargs = value;
    }

    public double getUnder35Perc() {
        return under35Perc;
    }

    public void setUnder35Perc(double value) {
        under35Perc = value;
    }

    public double getUnder20Perc() {
        return under20Perc;
    }

    public void setUnder20Perc(double value) {
        under20Perc = value;
    }

    public BossList.FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(BossList.FilterType value) {
        filterType = value;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String value) {
        filter = value;
    }

    public String getBossName() {
        return bossName;
    }

    public void setBossName(String value) {
        bossName = value;
    }

    @Override
    public void writeXml(Document doc, Element element) {
        super.writeXml(doc, element);
        appendElement(doc, element, "InBack", Boolean.toString(inBack));
        appendElement(doc, element, "MultiTargs", Boolean.toString(multiTargs));
        appendElement(doc, element, "StunningTargs", Boolean.toString(stunningTargs));
        appendElement(doc, element, "MovingTargs", Boolean.toString(movingTargs));
        appendElement(doc, element, "FearingTargs", Boolean.toString(fearingTargs));
        appendElement(doc, element, "RootingTargs", Boolean.toString(rootingTargs));
        appendElement(doc, element, "DisarmingTargs", Boolean.toString(disarmingTargs));
        appendElement(doc, element, "DamagingTargs", Boolean.toString(damagingTargs));
        appendElement(doc, element, "Under35Perc", NetFormat.doubleR(under35Perc));
        appendElement(doc, element, "Under20Perc", NetFormat.doubleR(under20Perc));
        appendElement(doc, element, "FilterType", filterType.name());
        appendElement(doc, element, "Filter", filter);
        appendElement(doc, element, "BossName", bossName);
    }
}
