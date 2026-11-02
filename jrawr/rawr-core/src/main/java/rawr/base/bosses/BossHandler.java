// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (reported): INotifyPropertyChanged/OnPropertyChanged (WPF-bound).
package rawr.base.bosses;

import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.PossibleLevels;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.util.NetFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * One concrete version of a raid encounter: the enemy force the player fights, with its health,
 * armor, timers, incoming attacks, damage-over-time auras, resistances and multi-target /
 * crowd-control mechanics.
 *
 * <p>An <i>encounter</i> (or "boss fight") is the whole combat situation a simulation reproduces:
 * how long the fight lasts, how many enemies share it, how much damage the player takes and
 * whether the player may stand behind the target. Gear has no single best answer across all
 * fights, so models read the encounter before scoring a character. The canonical definitions are
 * one class per WotLK boss in {@link rawr.base.bosses}; {@link BossList} indexes them and
 * {@link BossOptions} stores the per-character choice plus the mechanics a given model can use.</p>
 *
 * <h2>The four variants</h2>
 *
 * <p>The same boss exists as up to four game variants: 10-player normal, 25-player normal,
 * 10-player heroic and 25-player heroic. Heroic modes arrive with the later tiers; earlier bosses
 * leave the two heroic slots empty (zero health, empty attack lists). Each boss class derives from
 * {@link MultiDiffBoss} and fills four parallel {@code BossHandler} instances, so the numbers of
 * one variant never leak into another. {@link #getVersion()} identifies the variant and
 * {@link #getContent()} the raid tier (T7.0 through T10.9).</p>
 *
 * <h2>Vitals, sizes and timers</h2>
 *
 * <ul>
 *   <li>{@link #getHealth()} &mdash; total hit points the raid must remove.</li>
 *   <li>{@link #getBerserkTimer()} &mdash; seconds until the boss berserks (enrages and wipes the
 *       raid). This is the hard time limit and the default fight duration models assume.</li>
 *   <li>{@link #getSpeedKillTimer()} &mdash; seconds that count as a "fast kill"; the UI compares
 *       the character's simulated DPS against the DPS required to finish within this time.</li>
 *   <li>{@link #getLevel()} &mdash; the target's character level. Level 83 is the standard boss
 *       level; a higher level makes the player's attacks and spells miss, dodge or glance more
 *       often.</li>
 *   <li>{@link #getArmor()} &mdash; the target's armor, which reduces physical damage dealt to
 *       it.</li>
 *   <li>{@link #getMaxPlayers()}, {@link #getMinHealers()}, {@link #getMinTanks()} &mdash; intended
 *       raid composition. <i>Tanks</i> absorb the boss's hits, <i>healers</i> restore the raid's
 *       health, and the remaining raid slots are damage dealers ("DPS").
 *       {@link #genInfoString(float, float, float, float, float, float, float)} derives the DPS
 *       needed per role from these values.</li>
 * </ul>
 *
 * <h2>Positioning</h2>
 *
 * <p>{@link #getInBackPercMelee()} and {@link #getInBackPercRanged()} are fractions in
 * {@code [0, 1]}: the share of the fight during which a melee / ranged attacker may attack from
 * behind the target. A target cannot parry or block attacks from behind, so models scale the
 * player's suffered parry and block chance by these fractions.
 * {@link #setInBackPercMelee(double)} and {@link #setInBackPercRanged(double)} clamp their input
 * to this range.</p>
 *
 * <h2>Incoming damage and control</h2>
 *
 * <ul>
 *   <li>{@link #getAttacks()} &mdash; the boss's damage events ({@link Attack}): melee swings,
 *       breaths, volleys and area attacks, each with damage, interval and target count. Tank
 *       survival and healer models evaluate them; see {@link Attack} for the fields.</li>
 *   <li>{@link #getDoTs()} &mdash; damage-over-time auras ({@link DoT}) applied to players.
 *       Encounter definitions register their DoTs in {@link #getAttacks()} (a DoT is an
 *       {@link Attack}); this separate list exists so saved files and UI code can read them
 *       independently.</li>
 *   <li>{@link #getResistance(ItemDamageType)} &mdash; for each damage school, the fraction of
 *       that school's damage the boss resists (0 = none, 0.75 = 75% resisted). Spell damage models
 *       subtract it. {@link #setResistance(ItemDamageType, float)} writes one school.</li>
 *   <li>{@code moves}, {@code stuns}, {@code fears}, {@code roots}, {@code disarms} &mdash; lists of
 *       {@link Impedance}, the boss's movement and control effects: forced movement (the player
 *       runs and cannot cast), stuns (temporarily unable to act), fear (loss of control), roots
 *       (unable to move) and disarms (weapon unusable). Each entry says how often the effect
 *       happens, how long it lasts and how likely it is.</li>
 *   <li>{@link #getTimeBossIsInvuln()} &mdash; seconds of the fight during which the boss cannot be
 *       damaged (phase transitions); subtracted from the timer when computing the DPS required to
 *       win.</li>
 * </ul>
 *
 * <h2>Multiple targets ("adds")</h2>
 *
 * <p>{@code targets} is a list of {@link TargetGroup}: windows during which extra enemies
 * ("adds") are present. Each group says how many targets appear, how often (seconds), for how
 * long (milliseconds) and with what probability. Models that care about area damage summarize
 * these with the {@code getMultiTargs*} helpers: {@link #getMultiTargsFreq()} is the average
 * seconds between add windows, {@link #getMultiTargsTime()} the total seconds of the fight with
 * multiple targets, and so on. The {@code getDynamicCompiler*} methods collapse lists of
 * {@link Impedance} or target groups into a single representative entry; {@link BossList} uses
 * them to build its synthetic "average" bosses.</p>
 *
 * <h2>Derived helpers and persistence</h2>
 *
 * <ul>
 *   <li>{@link #getDpsByType(AttackTypes, float, float, float, float, float, float, float)} sums
 *       the damage per second of every attack of one {@link AttackTypes} after damage
 *       bonuses/penalties and the victim's miss/dodge/parry/block table;
 *       {@link #getDpsSingleTargMelee()}, {@link #getDpsSingleTargRanged()} and
 *       {@link #getDpsAoe()} are the common cases.</li>
 *   <li>{@link #getFilteredAttackList(AttackTypes)} filters the attack list by type and
 *       {@link #getDefaultMeleeAttack()} finds the swing marked
 *       {@link Attack#isTheDefaultMelee}.</li>
 *   <li>{@link #genInfoString()} renders the human-readable fight summary;
 *       {@link #genInfoString(Character)} includes the character's current defensive stats.</li>
 *   <li>{@link #writeXml(org.w3c.dom.Document, org.w3c.dom.Element)} serializes the whole
 *       description into a character file and {@link #clone()} makes an independent copy for
 *       editing; {@link BossOptions} is the copy a saved character actually stores.</li>
 *   <li>{@link #CPf(float)} / {@link #CPd(double)} clamp a value into {@code [0, 1]};
 *       {@link #CalcADotTick(float, float, float, float)} turns a min/max damage range plus a tick
 *       interval into the average damage of one damage-over-time tick.</li>
 * </ul>
 *
 * <p><b>Numeric fidelity:</b> fields keep the C# widths (a mix of {@code float} and {@code double})
 * and arithmetic order; do not widen or regroup them.</p>
 */
public class BossHandler implements Cloneable {
    public static final int NormCharLevel = PossibleLevels.LVLP0.value();

    protected static final String[] BossTierStrings = new String[] {
        "Tier  7.0",
        "Tier  7.5",
        "Tier  8.0",
        "Tier  8.5",
        "Tier  9.0",
        "Tier  9.5",
        "Tier 10.0",
        "Tier 10.5",
        "Tier 10.9",
    };
    protected static final String[] BossVersionStrings = new String[] {
        "10 Man",
        "25 Man",
        "10 Man (H)",
        "25 Man (H)",
    };

    /**
    * The four raid-size/difficulty variants of a boss, used as the index into the parallel entries
    * of a {@link MultiDiffBoss} and into {@link #getVersionString(Versions)}.
    *
    * <ul>
    *   <li>{@link #V_10N} / {@link #V_10} &mdash; 10-player normal (value 0).</li>
    *   <li>{@link #V_25N} / {@link #V_25} &mdash; 25-player normal (value 1).</li>
    *   <li>{@link #V_10H} &mdash; 10-player heroic (value 2).</li>
    *   <li>{@link #V_25H} &mdash; 25-player heroic (value 3).</li>
    * </ul>
    *
    * <p>{@code V_10} and {@code V_25} are aliases of the normal variants kept from the original
    * code. {@link #value()} is the storage index and must keep these numbers.</p>
 */
public enum Versions {
        V_10N(0), V_25N(1), V_10H(2), V_25H(3), V_10(0), V_25(1);

        private final int value;

        Versions(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
    * The raid tier a boss belongs to: {@link #T7_0} (first tier of the expansion) through
    * {@link #T10_9} (the final Icecrown Citadel tier). The tier drives the UI grouping and, in
    * {@link MultiDiffBoss#genAStandardMelee}, the damage of the generic melee swing.
    *
    * <p>{@link #value()} is the index into {@link BossHandler#StandardMeleePerHit},
    * {@link #of(int)} converts a value back to the enum (falling back to {@link #T7_0} for unknown
    * input) and {@link #name()} is written to character files.</p>
 */
public enum TierLevels {
        T7_0(0), T7_5(1), T8_0(2), T8_5(3), T9_0(4), T9_5(5), T10_0(6), T10_5(7), T10_9(8);

        private final int value;

        TierLevels(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static TierLevels of(int value) {
            for (TierLevels t : values()) {
                if (t.value == value) return t;
            }
            return T7_0;
        }
    }

    public static final float[] StandardMeleePerHit = new float[] {
        5000f * 2f,
        10000f * 2f,
        20000f * 2f,
        30000f * 2f,
        40000f * 2f,
        50000f * 2f,
        60000f * 2f,
        70000f * 2f,
        80000f * 2f,
    };

    private String name = "Generic";
    private String instance = "None";
    private String comment = "No comments have been written for this Boss.";
    private TierLevels content = TierLevels.T7_0;
    private Versions version = Versions.V_10N;

    private float health = 1000000f;
    private int berserkTimer = 8 * 60;
    private int speedKillTimer = 3 * 60;
    private int level = PossibleLevels.LVLP3.value();
    private int armor = (int) StatConversion.NPC_ARMOR[3];
    private int maxPlayers = 10;
    private int minHealers = 3;
    private int minTanks = 2;
    private double inBackPercMelee = 0.00d;
    private double inBackPercRanged = 0.00d;

    public List<TargetGroup> targets = new ArrayList<TargetGroup>();
    private List<DoT> dots = new ArrayList<DoT>();
    private List<Attack> attacks = new ArrayList<Attack>();

    private double resistancePhysical = 0;
    private double resistanceFrost = 0;
    private double resistanceFire = 0;
    private double resistanceNature = 0;
    private double resistanceArcane = 0;
    private double resistanceShadow = 0;
    private double resistanceHoly = 0;

    public List<Impedance> moves = new ArrayList<Impedance>();
    public List<Impedance> stuns = new ArrayList<Impedance>();
    public List<Impedance> fears = new ArrayList<Impedance>();
    public List<Impedance> roots = new ArrayList<Impedance>();
    public List<Impedance> disarms = new ArrayList<Impedance>();
    private float timeBossIsInvuln = 0;

    public BossHandler() {
    }

    protected BossHandler memberwiseClone() {
        try {
            return (BossHandler) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A copy whose mutable lists (target groups, attacks, moves, stuns, fears, roots, disarms)
     * are independent of this instance's; scalar fields are copied memberwise.
     */
    public BossHandler clone() {
        BossHandler clone = memberwiseClone();
        clone.targets = new ArrayList<TargetGroup>(this.targets);
        clone.attacks = new ArrayList<Attack>(this.attacks);
        clone.moves = new ArrayList<Impedance>(this.moves);
        clone.stuns = new ArrayList<Impedance>(this.stuns);
        clone.fears = new ArrayList<Impedance>(this.fears);
        clone.roots = new ArrayList<Impedance>(this.roots);
        clone.disarms = new ArrayList<Impedance>(this.disarms);
        return clone;
    }

    protected String getVersionString(Versions v) {
        return BossVersionStrings[v.value()];
    }

    protected String getContentString(TierLevels c) {
        return BossTierStrings[c.value()];
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public TierLevels getContent() {
        return content;
    }

    public void setContent(TierLevels value) {
        content = value;
    }

    public String getContentString() {
        return getContentString(content);
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String value) {
        instance = value;
    }

    public Versions getVersion() {
        return version;
    }

    public void setVersion(Versions value) {
        version = value;
    }

    public String getVersionString() {
        return getVersionString(version);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int value) {
        level = value;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int value) {
        armor = value;
    }

    public int getBerserkTimer() {
        return berserkTimer;
    }

    public void setBerserkTimer(int value) {
        berserkTimer = value;
    }

    public int getSpeedKillTimer() {
        return speedKillTimer;
    }

    public void setSpeedKillTimer(int value) {
        speedKillTimer = value;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float value) {
        health = value;
    }

    public double getInBackPercMelee() {
        return inBackPercMelee;
    }

    public void setInBackPercMelee(double value) {
        inBackPercMelee = CPd(value);
    }

    public double getInBackPercRanged() {
        return inBackPercRanged;
    }

    public void setInBackPercRanged(double value) {
        inBackPercRanged = CPd(value);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int value) {
        maxPlayers = value;
    }

    public int getMinHealers() {
        return minHealers;
    }

    public void setMinHealers(int value) {
        minHealers = value;
    }

    public int getMinTanks() {
        return minTanks;
    }

    public void setMinTanks(int value) {
        minTanks = value;
    }

    /**
     * Collapses the target groups into one synthetic add wave: the frequency derived from the
     * total add time over the berserk timer, the mean duration and chance, the mean target count
     * and whether most groups spawn near the boss. A non-positive frequency or chance yields
     * {@code -1} ("no adds").
     */
    public TargetGroup getDynamicCompilerMultiTargs() {
        TargetGroup retVal = new TargetGroup();
        float time = getMultiTargsTime();
        float dur = getMultiTargsDur();
        float acts = time / (dur / 1000f);
        float freq = berserkTimer / acts;
        float chance = getMultiTargsChance();
        double num = getMultiTargsNum();
        boolean near = getMultiTargsNear();
        retVal.frequency = freq;
        retVal.duration = dur;
        retVal.chance = chance;
        retVal.nearBoss = near;
        retVal.numTargs = (float) num;
        if (retVal.frequency <= 0f || retVal.chance <= 0f) {
            retVal.frequency = -1f;
        }
        return retVal;
    }

    /**
     * Average seconds between add windows, combining every target group's expected number of
     * occurrences across the fight weighted by its chance; 0 when the boss has no adds.
     */
    public float getMultiTargsFreq() {
        if (targets.size() > 0) {
            float numMultiTargsOverDur = 0;
            for (TargetGroup s : targets) {
                numMultiTargsOverDur += (berserkTimer / s.frequency) * s.chance;
            }
            float freq = berserkTimer / numMultiTargsOverDur;
            return freq;
        } else {
            return 0;
        }
    }

    /**
     * Mean duration in milliseconds of an add window across all target groups; 0 when the boss
     * has no adds.
     */
    public float getMultiTargsDur() {
        if (targets.size() > 0) {
            float totalMultiTargDur = 0;
            for (TargetGroup s : targets) {
                totalMultiTargDur += s.duration;
            }
            float dur = totalMultiTargDur / targets.size();
            return dur;
        } else {
            return 0;
        }
    }

    /**
     * Mean number of simultaneous adds across all target groups; 0 when the boss has no adds.
     */
    public double getMultiTargsNum() {
        if (targets.size() > 0) {
            float totalMultiTargAmt = 0;
            for (TargetGroup s : targets) {
                totalMultiTargAmt += s.numTargs;
            }
            float dur = totalMultiTargAmt / targets.size();
            return dur;
        } else {
            return 0;
        }
    }

    /**
     * Mean probability in {@code [0, 1]} that an add window actually occurs, averaged over all
     * target groups; 0 when the boss has no adds.
     */
    public float getMultiTargsChance() {
        if (targets.size() > 0) {
            float totalChance = 0f;
            for (TargetGroup s : targets) {
                totalChance += s.chance;
            }
            float chance = totalChance / (float) targets.size();
            return chance;
        } else {
            return 0;
        }
    }

    /**
     * Total seconds of the fight with adds present: the expected number of add windows (berserk
     * timer divided by the average frequency) times the mean window duration in seconds.
     */
    public float getMultiTargsTime() {
        float time = 0f;
        float freq = getMultiTargsFreq();
        float dur = getMultiTargsDur();
        if (freq > 0f && freq < berserkTimer) {
            time = (berserkTimer / freq) * (dur / 1000f);
        }
        return time;
    }

    /**
     * True when at least half of the add groups spawn near the boss (within melee reach), so
     * melee characters can keep attacking them without walking.
     */
    public boolean getMultiTargsNear() {
        if (targets.size() > 0) {
            int countYes = 0;
            int countNo = 0;
            for (TargetGroup s : targets) {
                if (s.nearBoss) {
                    countYes++;
                } else {
                    countNo++;
                }
            }
            return countYes >= countNo;
        } else {
            return false;
        }
    }

    public List<DoT> getDoTs() {
        return dots;
    }

    public void setDoTs(List<DoT> value) {
        dots = value;
    }

    public List<Attack> getAttacks() {
        return attacks;
    }

    public void setAttacks(List<Attack> value) {
        attacks = value;
    }

    /**
     * Averages {@link #getAttacks()} into one synthetic attack: mean damage per hit, mean target
     * count and mean attack speed, as a physical, fully avoidable melee swing. An empty attack
     * list keeps the default values and speed {@code -1} ("no attacks").
     */
    public Attack getDynamicCompilerAttacks() {
        Attack retVal = new Attack();
        retVal.name = "Dynamic";
        retVal.damageType = ItemDamageType.Physical;
        retVal.setDamagePerHit(80f * 1000f);
        retVal.damageIsPerc = false;
        retVal.maxNumTargets = 1;
        retVal.attackSpeed = 2.0f;
        retVal.attackType = AttackTypes.AT_MELEE;
        retVal.useParryHaste = true;
        retVal.interruptable = false;
        retVal.missable = true;
        retVal.dodgable = true;
        retVal.parryable = true;
        retVal.blockable = true;
        retVal.ignoresMTank = false;
        retVal.ignoresOTank = false;
        retVal.ignoresTTank = false;
        retVal.ignoresHealers = false;
        retVal.ignoresMeleeDps = false;
        retVal.ignoresRangedDps = false;
        if (attacks.size() <= 0) {
            retVal.attackSpeed = -1;
            return retVal;
        }
        int numTargs = 0;
        float speeds = 0;
        float dph = 0;
        for (Attack a : attacks) {
            dph += a.getDamagePerHit();
            numTargs += (int) a.maxNumTargets;
            speeds += (int) a.attackSpeed;
        }
        retVal.setDamagePerHit(dph / (float) attacks.size());
        retVal.maxNumTargets = (int) ((float) numTargs / (float) attacks.size());
        retVal.attackSpeed = (int) (speeds / (float) attacks.size());
        if (retVal.attackSpeed <= 0f) {
            retVal.attackSpeed = -1f;
        }
        return retVal;
    }

    /**
     * Same averaging as {@link #getDynamicCompilerAttacks()} but over a caller-supplied attack
     * list; used to compile a subset such as one tank's incoming attacks.
     */
    public Attack getDynamicCompilerFilteredAttacks(List<Attack> atks) {
        Attack retVal = new Attack();
        retVal.name = "DynamicFiltered";
        retVal.damageType = ItemDamageType.Physical;
        retVal.setDamagePerHit(80f * 1000f);
        retVal.damageIsPerc = false;
        retVal.maxNumTargets = 1;
        retVal.attackSpeed = 2.0f;
        retVal.attackType = AttackTypes.AT_MELEE;
        retVal.useParryHaste = true;
        retVal.interruptable = false;
        retVal.missable = true;
        retVal.dodgable = true;
        retVal.parryable = true;
        retVal.blockable = true;
        retVal.ignoresMTank = false;
        retVal.ignoresOTank = false;
        retVal.ignoresTTank = false;
        retVal.ignoresHealers = false;
        retVal.ignoresMeleeDps = false;
        retVal.ignoresRangedDps = false;
        if (atks.size() <= 0) {
            retVal.attackSpeed = -1;
            return retVal;
        }
        int numTargs = 0;
        float speeds = 0;
        float dph = 0;
        for (Attack a : atks) {
            dph += a.getDamagePerHit();
            numTargs += (int) a.maxNumTargets;
            speeds += (int) a.attackSpeed;
        }
        retVal.setDamagePerHit(dph / (float) atks.size());
        retVal.maxNumTargets = (int) ((float) numTargs / (float) atks.size());
        retVal.attackSpeed = (int) (speeds / (float) atks.size());
        if (retVal.attackSpeed <= 0f) {
            retVal.attackSpeed = -1f;
        }
        return retVal;
    }

    /**
     * The attacks of one {@link AttackTypes} group, in encounter-list order; empty when none
     * match.
     */
    public List<Attack> getFilteredAttackList(AttackTypes type) {
        List<Attack> list = new ArrayList<Attack>();
        if (attacks.size() <= 0) {
            return list;
        }
        for (Attack a : attacks) {
            if (a.attackType == type) {
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Total damage per second of all attacks of one type. Each hit is scaled by the damage bonus
     * and penalty, then reduced by the victim's miss/dodge/parry/block fractions; blocked hits
     * still land for damage minus the block value. Attacks are assumed to repeat every
     * {@link Attack#attackSpeed} seconds for the whole berserk timer.
     */
    public float getDpsByType(AttackTypes type, float bossDamageBonus, float bossDamagePenalty,
                              float p_missPerc, float p_dodgePerc, float p_parryPerc, float p_blockPerc, float p_blockVal) {
        List<Attack> list = getFilteredAttackList(type);
        if (list.size() <= 0) {
            return 0f;
        }
        float retDPS = 0f;
        for (Attack a : list) {
            float damage = a.getDamagePerHit() * (1f + bossDamageBonus) * (1f - bossDamagePenalty),
                  damageOnUse = damage * (1f - p_missPerc - p_dodgePerc - p_parryPerc - p_blockPerc),
                  swing = a.attackSpeed;
            damageOnUse += (damage - p_blockVal) * p_blockPerc;
            float acts = berserkTimer / swing,
                  avgDmg = damageOnUse * acts,
                  dps = avgDmg / berserkTimer;
            retDPS += dps;
        }
        return retDPS;
    }

    public float getDpsByType(AttackTypes type, float bossDamageBonus, float bossDamagePenalty, float p_missPerc) {
        return getDpsByType(type, bossDamageBonus, bossDamagePenalty, p_missPerc, 0, 0, 0, 0);
    }

    public float getDpsByType(AttackTypes type, float bossDamageBonus, float bossDamagePenalty) {
        return getDpsByType(type, bossDamageBonus, bossDamagePenalty, 0, 0, 0, 0, 0);
    }

    public float getDpsSingleTargMelee() {
        float dps = getDpsByType(AttackTypes.AT_MELEE, 0, 0);
        return dps;
    }

    public float getDpsSingleTargRanged() {
        float dps = getDpsByType(AttackTypes.AT_RANGED, 0, 0, 0);
        return dps;
    }

    public float getDpsAoe() {
        float dps = getDpsByType(AttackTypes.AT_AOE, 0, 0);
        return dps;
    }

    /**
     * The attack flagged {@link Attack#isTheDefaultMelee}, or {@code null} when the boss has no
     * attacks or none is flagged.
     */
    public Attack getDefaultMeleeAttack() {
        if (attacks.size() <= 0) {
            return null;
        }
        for (Attack a : attacks) {
            if (a.isTheDefaultMelee) {
                return a;
            }
        }
        return null;
    }

    /**
     * Average seconds between the boss's area attacks ({@link AttackTypes#AT_AOE}), derived from
     * their attack speeds; returns the berserk timer when the boss has none (no area damage).
     */
    public float getAoeTargsFreq() {
        List<Attack> list = getFilteredAttackList(AttackTypes.AT_AOE);
        if (list.size() > 0) {
            float numAoEsOverDur = 0;
            for (Attack s : list) {
                numAoEsOverDur += berserkTimer / s.attackSpeed;
            }
            float freq = berserkTimer / numAoEsOverDur;
            return freq;
        } else {
            return berserkTimer;
        }
    }

    /**
     * Mean damage per hit of the boss's area attacks, or {@code 1500} when the boss has none
     * (the original's neutral default).
     */
    public float getAoeTargsDmg() {
        List<Attack> list = getFilteredAttackList(AttackTypes.AT_AOE);
        if (list.size() > 0) {
            float totalAoEDmg = 0;
            for (Attack s : list) {
                totalAoEDmg += s.getDamagePerHit();
            }
            float dur = totalAoEDmg / list.size();
            return dur;
        } else {
            return 1500f;
        }
    }

    public double getResistPhysical() {
        return resistancePhysical;
    }

    public void setResistPhysical(double value) {
        resistancePhysical = value;
    }

    public double getResistFrost() {
        return resistanceFrost;
    }

    public void setResistFrost(double value) {
        resistanceFrost = value;
    }

    public double getResistFire() {
        return resistanceFire;
    }

    public void setResistFire(double value) {
        resistanceFire = value;
    }

    public double getResistNature() {
        return resistanceNature;
    }

    public void setResistNature(double value) {
        resistanceNature = value;
    }

    public double getResistArcane() {
        return resistanceArcane;
    }

    public void setResistArcane(double value) {
        resistanceArcane = value;
    }

    public double getResistShadow() {
        return resistanceShadow;
    }

    public void setResistShadow(double value) {
        resistanceShadow = value;
    }

    public double getResistHoly() {
        return resistanceHoly;
    }

    public void setResistHoly(double value) {
        resistanceHoly = value;
    }

    /**
     * The boss's resistance to one damage school as a fraction of incoming damage of that school
     * (0 = unresisted, 0.75 = 75% resisted).
     */
    public double getResistance(ItemDamageType type) {
        switch (type) {
            case Physical: return resistancePhysical;
            case Nature:   return resistanceNature;
            case Arcane:   return resistanceArcane;
            case Frost:    return resistanceFrost;
            case Fire:     return resistanceFire;
            case Shadow:   return resistanceShadow;
            case Holy:     return resistanceHoly;
            default: break;
        }
        return 0f;
    }

    /**
     * Writes one school's resistance and returns the stored value (as read back by
     * {@link #getResistance(ItemDamageType)}).
     */
    public double setResistance(ItemDamageType type, float newValue) {
        switch (type) {
            case Physical: setResistPhysical(newValue); return getResistPhysical();
            case Frost:    setResistFrost(newValue);    return getResistFrost();
            case Fire:     setResistFire(newValue);     return getResistFire();
            case Nature:   setResistNature(newValue);   return getResistNature();
            case Arcane:   setResistArcane(newValue);   return getResistArcane();
            case Shadow:   setResistShadow(newValue);   return getResistShadow();
            case Holy:     setResistHoly(newValue);     return getResistHoly();
            default: break;
        }
        return 0f;
    }

    /** Builds one compiled impedance, keeping the original default fields and -1 guard. */
    private Impedance compileImpedance(float time, float dur, float chance) {
        Impedance retVal = new Impedance();
        retVal.frequency = 20f;
        retVal.duration = 1f * 1000f;
        retVal.chance = 1.00f;
        retVal.breakable = true;
        float acts = time / (dur / 1000f);
        float freq = berserkTimer / acts;
        retVal.frequency = freq;
        retVal.duration = dur;
        retVal.chance = chance;
        if (retVal.frequency <= 0f || retVal.chance <= 0f) {
            retVal.frequency = -1f;
        }
        return retVal;
    }

    /**
     * Collapses a list of {@link Impedance} entries into one representative entry: the combined
     * occurrence interval weighted by each entry's chance, the mean duration in milliseconds and
     * the mean chance. Frequency {@code -1} marks "never happens".
     */
    protected Impedance dynamicCompiler(List<Impedance> imps) {
        return compileImpedance(time(imps), dur(imps), chance(imps));
    }

    /** Combined occurrence interval in seconds of a list of {@link Impedance} entries, weighted by their chance; -1 for an empty list. */
    protected float freq(List<Impedance> imps) {
        if (imps == null || imps.size() < 1) return -1;
        float numImpsOverDur = 0f;
        for (Impedance imp : imps) {
            numImpsOverDur += (berserkTimer / imp.frequency) * imp.chance;
        }
        return berserkTimer / numImpsOverDur;
    }

    /** Total seconds of the fight spent under a list of {@link Impedance} effects: expected number of occurrences times mean duration. */
    protected float time(List<Impedance> imps) {
        if (imps == null || imps.size() < 1) return 0;
        float time = 0f;
        float freq = freq(imps);
        float dur = dur(imps) / 1000f;
        if (freq > 0f && freq < berserkTimer) {
            time = (berserkTimer / freq) * dur;
        } else if (freq >= berserkTimer) {
            time = dur;
        }
        return time;
    }

    /** Mean duration in milliseconds of a list of {@link Impedance} entries; 0 for an empty list. */
    protected float dur(List<Impedance> imps) {
        if (imps == null || imps.size() < 1) return 0;
        float totalDur = 0;
        for (Impedance s : imps) {
            totalDur += s.duration;
        }
        return totalDur / (float) imps.size();
    }

    /** Mean chance in {@code [0, 1]} of a list of {@link Impedance} entries; 0 for an empty list. */
    protected float chance(List<Impedance> imps) {
        if (imps == null || imps.size() < 1) return 0;
        float totalChance = 0f;
        for (Impedance s : imps) {
            totalChance += s.chance;
        }
        return totalChance / (float) imps.size();
    }

    /**
     * One synthetic forced-movement {@link Impedance} compiled from {@link #moves}, where the
     * player must run and stops attacking. The default entry (1 second, 100% chance) is
     * overwritten by the combined values; a non-positive frequency or chance yields -1 ("no
     * movement").
     */
    public Impedance getDynamicCompilerMove() {
        return compileImpedance(getMovingTargsTime(), getMovingTargsDur(), getMovingTargsChance());
    }

    public float getMovingTargsFreq() {
        return moves.size() > 0 ? freq(moves) : -1;
    }

    public float getMovingTargsDur() {
        return moves.size() > 0 ? dur(moves) : 0;
    }

    public float getMovingTargsChance() {
        return moves.size() > 0 ? chance(moves) : 0;
    }

    public float getMovingTargsTime() {
        return moves.size() > 0 ? time(moves) : 0;
    }

    /** Compiled stun impedance from {@link #stuns}, in the same form as {@link #getDynamicCompilerMove()}; -1 frequency when the boss never stuns. */
    public Impedance getDynamicCompilerStun() {
        return compileImpedance(getStunningTargsTime(), getStunningTargsDur(), getStunningTargsChance());
    }

    public float getStunningTargsFreq() {
        return stuns.size() > 0 ? freq(stuns) : -1;
    }

    public float getStunningTargsDur() {
        return stuns.size() > 0 ? dur(stuns) : 0;
    }

    public float getStunningTargsChance() {
        return stuns.size() > 0 ? chance(stuns) : 0;
    }

    public float getStunningTargsTime() {
        return stuns.size() > 0 ? time(stuns) : 0;
    }

    /** Compiled fear impedance from {@link #fears}, in the same form as {@link #getDynamicCompilerMove()}; -1 frequency when the boss never fears. */
    public Impedance getDynamicCompilerFear() {
        return compileImpedance(getFearingTargsTime(), getFearingTargsDur(), getFearingTargsChance());
    }

    public float getFearingTargsFreq() {
        return fears.size() > 0 ? freq(fears) : -1;
    }

    public float getFearingTargsDur() {
        return fears.size() > 0 ? dur(fears) : 0;
    }

    public float getFearingTargsChance() {
        return fears.size() > 0 ? chance(fears) : 0;
    }

    public float getFearingTargsTime() {
        return fears.size() > 0 ? time(fears) : 0;
    }

    /** Compiled root impedance from {@link #roots}, in the same form as {@link #getDynamicCompilerMove()}; -1 frequency when the boss never roots. */
    public Impedance getDynamicCompilerRoot() {
        return compileImpedance(getRootingTargsTime(), getRootingTargsDur(), getRootingTargsChance());
    }

    public float getRootingTargsFreq() {
        return roots.size() > 0 ? freq(roots) : -1;
    }

    public float getRootingTargsDur() {
        return roots.size() > 0 ? dur(roots) : 0;
    }

    public float getRootingTargsChance() {
        return roots.size() > 0 ? chance(roots) : 0;
    }

    public float getRootingTargsTime() {
        return roots.size() > 0 ? time(roots) : 0;
    }

    /** Compiled disarm impedance from {@link #disarms}, in the same form as {@link #getDynamicCompilerMove()}; -1 frequency when the boss never disarms. */
    public Impedance getDynamicCompilerDisarm() {
        return compileImpedance(getDisarmingTargsTime(), getDisarmingTargsDur(), getDisarmingTargsChance());
    }

    public float getDisarmingTargsFreq() {
        return disarms.size() > 0 ? freq(disarms) : -1;
    }

    public float getDisarmingTargsDur() {
        return disarms.size() > 0 ? dur(disarms) : 0;
    }

    public float getDisarmingTargsChance() {
        return disarms.size() > 0 ? chance(disarms) : 0;
    }

    public float getDisarmingTargsTime() {
        return disarms.size() > 0 ? time(disarms) : 0;
    }

    public float getTimeBossIsInvuln() {
        return timeBossIsInvuln;
    }

    public void setTimeBossIsInvuln(float value) {
        timeBossIsInvuln = value;
    }

    /** Clamps a value into the unit interval {@code [0, 1]}. */
    public static float CPf(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** Clamps a value into the unit interval {@code [0, 1]}. */
    public static double CPd(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    /**
     * Average damage of one tick of a damage-over-time effect: the midpoint of the min/max damage
     * range times the number of ticks ({@code time / interval}). The 3-argument overload takes
     * the tick count directly.
     */
    public static float CalcADotTick(float mindmg, float maxdmg, float time, float interval) {
        return ((mindmg + maxdmg) / 2.0f) * time / interval;
    }

    public static float CalcADotTick(float mindmg, float maxdmg, float time) {
        return ((mindmg + maxdmg) / 2.0f) * time;
    }

    /**
     * Builds the multi-line fight summary shown in the UI: identity, health, the two timers,
     * expected raid composition, the total and per-role DPS needed to beat each timer, and the
     * boss's damage per second by attack type after the supplied damage bonus/penalty and
     * avoidance values.
     */
    private record DpsSplit(float tankDps, float dpsDps) {
    }

    private DpsSplit splitDps(float totalDpsNeeded, int room) {
        float dpsdps = totalDpsNeeded * ((float) room / ((float) minTanks / 2f + (float) room)) / (float) room;
        float tankdps = (totalDpsNeeded * ((float) minTanks / ((float) minTanks / 2f + (float) room)) / (float) minTanks) / 2f;
        return new DpsSplit(tankdps, dpsdps);
    }

    public String genInfoString(float bossDamageBonus, float bossDamagePenalty,
                               float p_missPerc, float p_dodgePerc, float p_parryPerc, float p_blockPerc, float p_blockVal) {
        int room = maxPlayers - minHealers - minTanks;
        float totalDPSNeeded = health / (berserkTimer - timeBossIsInvuln - getMovingTargsTime());
        DpsSplit firstSplit = splitDps(totalDPSNeeded, room);
        float dpsdps = firstSplit.dpsDps();
        float tankdps = firstSplit.tankDps();
        String retVal = "";
        retVal += "Name: " + name + "\r\nContent: " + getContentString().replace("  ", " ")
                + "\r\nInstance: " + instance + " (" + getVersionString() + ")\r\nHealth: "
                + NetFormat.grouped(health) + "\r\n";
        retVal += "Enrage Timer: " + timeSpanPart(berserkTimer, true) + " min " + timeSpanPart(berserkTimer, false)
                + " sec\r\nSpeed Kill Timer: " + timeSpanPart(speedKillTimer, true) + " min "
                + timeSpanPart(speedKillTimer, false) + " sec\r\n\r\n";
        retVal += "Raid Setup: " + maxPlayers + " man (" + minTanks + " Tanks " + minHealers + " Healers "
                + room + " DPS)\r\n\r\n";
        retVal += "To beat the Enrage Timer you need " + NetFormat.grouped(totalDPSNeeded) + " Total DPS\r\n"
                + NetFormat.grouped(tankdps) + " from each Tank " + NetFormat.grouped(dpsdps)
                + " from each DPS\r\n\r\n";
        totalDPSNeeded = health / (speedKillTimer - timeBossIsInvuln - speedKillTimer * (getMovingTargsTime() / berserkTimer));
        DpsSplit secondSplit = splitDps(totalDPSNeeded, room);
        dpsdps = secondSplit.dpsDps();
        tankdps = secondSplit.tankDps();
        retVal += "To beat the Speed Kill Timer you need " + NetFormat.grouped(totalDPSNeeded) + " Total DPS\r\n"
                + NetFormat.grouped(tankdps) + " from each Tank " + NetFormat.grouped(dpsdps)
                + " from each DPS\r\n\r\n";
        retVal += "This boss does the following Damage Per Second Amounts, factoring Armor, Resistance and Defend Tables where applicable:\r\n"
                + "Single Target Melee: " + NetFormat.fixed(getDpsByType(AttackTypes.AT_MELEE, bossDamageBonus, bossDamagePenalty, p_missPerc, p_dodgePerc, p_parryPerc, p_blockPerc, p_blockVal), 1) + "\r\n"
                + "Single Target Ranged: " + NetFormat.fixed(getDpsByType(AttackTypes.AT_RANGED, bossDamageBonus, bossDamagePenalty, p_missPerc), 1) + "\r\n"
                + "Raid AoE: " + NetFormat.fixed(getDpsByType(AttackTypes.AT_AOE, bossDamageBonus, 0), 1) + "\r\n\r\n";
        retVal += "Comment(s):\r\n" + comment;
        return retVal;
    }

    public String genInfoString() {
        return genInfoString(0, 0, 0, 0, 0, 0, 0);
    }

    // C# BossHandler.cs:1256-1261. The C# `Character.CurrentCalculations` field is null before a model
    // is assigned; the Java registry throws for an unregistered model, which maps to that null case.
    /**
     * {@link #genInfoString(float, float, float, float, float, float, float)} filled with the
     * character's current defensive stats (damage taken multiplier, dodge, parry, block,
     * block value); falls back to the zero-argument summary when no calculation model is
     * available for the character.
     */
    public String genInfoString(Character character) {
        if (character == null) return genInfoString();
        CalculationsBase calculations;
        try {
            calculations = character.getCurrentCalculations();
        } catch (IllegalArgumentException e) {
            return genInfoString();
        }
        if (calculations == null) return genInfoString();
        Stats stats = calculations.getCharacterStats(character);
        return genInfoString(stats.getDamageTakenMultiplier(), 0f,
                stats.getDefense() * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER,
                stats.getDodge(), stats.getParry(), stats.getBlock(), stats.getBlockValue());
    }

    private static String timeSpanPart(int totalSeconds, boolean minutes) {
        int part = minutes ? (totalSeconds / 60) % 60 : totalSeconds % 60;
        return part < 10 ? "0" + part : Integer.toString(part);
    }

    @Override
    public String toString() {
        return content.toString() + " : " + instance + " : (" + version.toString() + ") : " + name;
    }

    /**
     * Writes this encounter to a character file: identity and vitals, target groups, impedances,
     * DoTs, attacks (a {@link DoT} is written with {@code xsi:type="DoT"} and its extra fields)
     * and resistances. Element names and order match the original serializer so old files
     * round-trip.
     */
    public void writeXml(Document doc, Element element) {
        writeTargets(doc, element);
        writeImpedances(doc, element, "Moves", moves);
        writeImpedances(doc, element, "Stuns", stuns);
        writeImpedances(doc, element, "Fears", fears);
        writeImpedances(doc, element, "Roots", roots);
        writeImpedances(doc, element, "Disarms", disarms);
        appendElement(doc, element, "Name", name);
        appendElement(doc, element, "Content", content.name());
        appendElement(doc, element, "Instance", instance);
        appendElement(doc, element, "Version", version.name());
        appendElement(doc, element, "Comment", comment);
        appendElement(doc, element, "Level", Integer.toString(level));
        appendElement(doc, element, "Armor", Integer.toString(armor));
        appendElement(doc, element, "BerserkTimer", Integer.toString(berserkTimer));
        appendElement(doc, element, "SpeedKillTimer", Integer.toString(speedKillTimer));
        appendElement(doc, element, "Health", NetFormat.floatR(health));
        appendElement(doc, element, "InBackPerc_Melee", NetFormat.doubleR(inBackPercMelee));
        appendElement(doc, element, "InBackPerc_Ranged", NetFormat.doubleR(inBackPercRanged));
        appendElement(doc, element, "Max_Players", Integer.toString(maxPlayers));
        appendElement(doc, element, "Min_Healers", Integer.toString(minHealers));
        appendElement(doc, element, "Min_Tanks", Integer.toString(minTanks));
        writeDoTs(doc, element);
        writeAttacks(doc, element);
        appendElement(doc, element, "Resist_Physical", NetFormat.doubleR(resistancePhysical));
        appendElement(doc, element, "Resist_Frost", NetFormat.doubleR(resistanceFrost));
        appendElement(doc, element, "Resist_Fire", NetFormat.doubleR(resistanceFire));
        appendElement(doc, element, "Resist_Nature", NetFormat.doubleR(resistanceNature));
        appendElement(doc, element, "Resist_Arcane", NetFormat.doubleR(resistanceArcane));
        appendElement(doc, element, "Resist_Shadow", NetFormat.doubleR(resistanceShadow));
        appendElement(doc, element, "Resist_Holy", NetFormat.doubleR(resistanceHoly));
        appendElement(doc, element, "TimeBossIsInvuln", NetFormat.floatR(timeBossIsInvuln));
    }

    private void writeTargets(Document doc, Element parent) {
        Element element = doc.createElement("Targets");
        parent.appendChild(element);
        for (TargetGroup t : targets) {
            Element e = doc.createElement("TargetGroup");
            appendElement(doc, e, "Frequency", NetFormat.floatR(t.frequency));
            appendElement(doc, e, "Duration", NetFormat.floatR(t.duration));
            appendElement(doc, e, "Chance", NetFormat.floatR(t.chance));
            appendElement(doc, e, "NumTargs", NetFormat.floatR(t.numTargs));
            appendElement(doc, e, "NearBoss", Boolean.toString(t.nearBoss));
            element.appendChild(e);
        }
    }

    private static void writeImpedances(Document doc, Element parent, String name, List<Impedance> list) {
        Element element = doc.createElement(name);
        parent.appendChild(element);
        for (Impedance i : list) {
            Element e = doc.createElement("Impedance");
            appendElement(doc, e, "Frequency", NetFormat.floatR(i.frequency));
            appendElement(doc, e, "Duration", NetFormat.floatR(i.duration));
            appendElement(doc, e, "Chance", NetFormat.floatR(i.chance));
            appendElement(doc, e, "Breakable", Boolean.toString(i.breakable));
            element.appendChild(e);
        }
    }

    private void writeDoTs(Document doc, Element parent) {
        Element element = doc.createElement("DoTs");
        parent.appendChild(element);
        for (DoT d : dots) {
            Element e = doc.createElement("DoT");
            writeAttackFields(doc, e, d);
            writeDoTFields(doc, e, d);
            element.appendChild(e);
        }
    }

    private void writeAttacks(Document doc, Element parent) {
        Element element = doc.createElement("Attacks");
        parent.appendChild(element);
        for (Attack a : attacks) {
            Element e = doc.createElement("Attack");
            if (a instanceof DoT dot) {
                e.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "DoT");
                writeAttackFields(doc, e, a);
                writeDoTFields(doc, e, dot);
            } else {
                writeAttackFields(doc, e, a);
            }
            element.appendChild(e);
        }
    }

    private static void writeAttackFields(Document doc, Element e, Attack a) {
        appendElement(doc, e, "Name", a.name);
        appendElement(doc, e, "DamageType", a.damageType != null ? a.damageType.name() : null);
        appendElement(doc, e, "IsTheDefaultMelee", Boolean.toString(a.isTheDefaultMelee));
        appendElement(doc, e, "DamageIsPerc", Boolean.toString(a.damageIsPerc));
        appendElement(doc, e, "MaxNumTargets", NetFormat.floatR(a.maxNumTargets));
        appendElement(doc, e, "AttackSpeed", NetFormat.floatR(a.attackSpeed));
        appendElement(doc, e, "AttackType", a.attackType != null ? a.attackType.name() : null);
        appendElement(doc, e, "UseParryHaste", Boolean.toString(a.useParryHaste));
        appendElement(doc, e, "IsDualWielding", Boolean.toString(a.isDualWielding));
        appendElement(doc, e, "Missable", Boolean.toString(a.missable));
        appendElement(doc, e, "Dodgable", Boolean.toString(a.dodgable));
        appendElement(doc, e, "Parryable", Boolean.toString(a.parryable));
        appendElement(doc, e, "Blockable", Boolean.toString(a.blockable));
        appendElement(doc, e, "IgnoresMTank", Boolean.toString(a.ignoresMTank));
        appendElement(doc, e, "IgnoresOTank", Boolean.toString(a.ignoresOTank));
        appendElement(doc, e, "IgnoresTTank", Boolean.toString(a.ignoresTTank));
        appendElement(doc, e, "IgnoresHealers", Boolean.toString(a.ignoresHealers));
        appendElement(doc, e, "IgnoresMeleeDPS", Boolean.toString(a.ignoresMeleeDps));
        appendElement(doc, e, "IgnoresRangedDPS", Boolean.toString(a.ignoresRangedDps));
        appendElement(doc, e, "Interruptable", Boolean.toString(a.interruptable));
        appendElement(doc, e, "DamagePerHit", NetFormat.floatR(a.getDamagePerHit()));
    }

    private static void writeDoTFields(Document doc, Element e, DoT d) {
        appendElement(doc, e, "DamagePerTick", NetFormat.floatR(d.damagePerTick));
        appendElement(doc, e, "NumTicks", NetFormat.floatR(d.numTicks));
        appendElement(doc, e, "TickInterval", NetFormat.floatR(d.tickInterval));
    }

    static void appendElement(Document doc, Element parent, String name, String text) {
        if (text == null) return;
        Element element = doc.createElement(name);
        element.setTextContent(text);
        parent.appendChild(element);
    }
}
