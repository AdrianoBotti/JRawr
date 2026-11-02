// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialDamageProcs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// ErrorBox reporting in the catch blocks is a WinForms class and is omitted (the original swallowed
// the error after showing the box, so the Java code just ignores the exception). CombatTable,
// NullCombatTable and AttackTable are the namespace-level classes declared in the same C# file and
// are ported as nested classes.
package rawr.base.effects;

import rawr.base.bosses.Attack;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemDamageType;
import rawr.base.items.ItemInstance;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// <summary>
/// The intention of this class is to calculate all Damage Procs such
/// as Hand-Mounted Pyro Rocket, Bandit's Insignia, Icebreaker etc by
/// feeding in your pre-calculated Character Stats. The damage amounts
/// returned should be with the inclusions of all factors such as
/// Spell Crit and BonusXDamageMultipliers.
/// </summary>
/**
 * Values the damage-dealing procs carried by a character's gear, for example a "chance on hit for
 * extra fire damage" weapon enchant or an on-use damaging trinket, given the character's already
 * computed stats.
 *
 * <p>{@link SpecialEffect}s whose stats contain a flat damage field ({@code physicalDamage},
 * {@code fireDamage}, ...) are procs of this kind. The caller supplies the environment through the
 * constructor: the character and its stats, the fight length, the target level delta (0 for a
 * level 80 target, up to 3 for 83), the list of effects to value, and per-{@link Trigger} maps
 * giving the average interval between triggering events and the chance an event actually triggers.
 * The constructor builds, per {@link ItemDamageType}, the global damage multiplier, the crit
 * multiplier, the resistance factor and an attack table; {@link #calculate} and
 * {@link #calculateAll} then return damage per second and accumulate totals into the public
 * maps.</p>
 *
 * <h2>How a proc is valued</h2>
 *
 * <ol>
 *   <li>{@link SpecialEffect#getAverageProcsPerSecond} turns the trigger interval, trigger chance
 *       and fight duration into an average proc rate. Weapon procs prefer the main-hand or
 *       off-hand {@link Trigger} entry depending on which hand carries the effect, and an effect
 *       carried by both hands counts as dual-wield only after its first main-hand evaluation (the
 *       {@code dualWieldProcs} latch).</li>
 *   <li>The expected number of procs is multiplied by the damage of a single proc, which is the
 *       base damage scaled by {@code 1 + globalMultiplier} and {@code 1 - resistance} and then
 *       passed through the attack table: misses, dodges and parries deal nothing, glancing blows
 *       and blocks deal 70%, critical strikes add the crit multiplier and the rest is a normal
 *       hit.</li>
 *   <li>The totals are accumulated per damage type in the public {@code totalDamage},
 *       {@code totalDamagePerSec}, {@code totalNumProcs} and {@code totalNumProcsPerSec} maps.</li>
 * </ol>
 *
 * <p>Because the maps accumulate, call {@link #reset} between independent runs, and note that
 * {@link #calculate} returns the damage per second of the last matching effect rather than the sum
 * of the matching effects; the accumulated public maps are the reliable output. The nested
 * {@code CombatTable}/{@code AttackTable} types are the local combat model used here; all three
 * were declared in the same C# file and are ported as nested classes.</p>
 */
public class SpecialDamageProcs {
    private Character character = null;
    private Stats stats = null;
    private int levelDelta = 0;
    private float fightDuration = 0f;
    private float armorDmgReduc = 0f;
    public Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
    public Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
    public List<SpecialEffect> effectsList = new ArrayList<SpecialEffect>();
    public Map<ItemDamageType, Float> damageMultipliers = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, Float> damageCritMultipliers = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, Float> damageResistances = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, AttackTable> attackTables = new LinkedHashMap<ItemDamageType, AttackTable>();
    // Results
    public Map<ItemDamageType, Float> totalDamage = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, Float> totalDamagePerSec = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, Float> totalNumProcs = new LinkedHashMap<ItemDamageType, Float>();
    public Map<ItemDamageType, Float> totalNumProcsPerSec = new LinkedHashMap<ItemDamageType, Float>();

    private List<SpecialEffect> mainHandEffects = new ArrayList<SpecialEffect>();
    private List<SpecialEffect> offHandEffects = new ArrayList<SpecialEffect>();
    private boolean dualWieldProcs = false;

    public SpecialDamageProcs(Character c, Stats charStats, int levelDelta,
            List<SpecialEffect> effectsList,
            Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances,
            float fightDuration, float armorDmgReduc) {
        character = c;
        stats = charStats;
        this.levelDelta = levelDelta;
        this.effectsList = effectsList;
        this.triggerIntervals = triggerIntervals;
        this.triggerChances = triggerChances;
        this.fightDuration = fightDuration;
        this.armorDmgReduc = armorDmgReduc;
        createDictionaries();
        if (triggerIntervals.containsKey(Trigger.OffHandHit) || triggerIntervals.containsKey(Trigger.MainHandHit)) {
            ItemInstance mainHand = c.get(CharacterSlot.MainHand);
            if (mainHand != null) {
                Enchant mainHandEnchant = mainHand.getEnchant();
                if (mainHandEnchant != null) {
                    Stats.SpecialEffectEnumerator e = mainHandEnchant.stats.specialEffects();
                    while (e.hasNext()) mainHandEffects.add(e.next());
                }
            }
            if (mainHand != null && mainHand.getItem() != null) {
                Stats.SpecialEffectEnumerator e = mainHand.getItem().getStats().specialEffects();
                while (e.hasNext()) mainHandEffects.add(e.next());
            }
            ItemInstance offHand = c.get(CharacterSlot.OffHand);
            if (offHand != null) {
                Enchant offHandEnchant = offHand.getEnchant();
                if (offHandEnchant != null) {
                    Stats.SpecialEffectEnumerator e = offHandEnchant.stats.specialEffects();
                    while (e.hasNext()) offHandEffects.add(e.next());
                }
            }
            if (offHand != null && offHand.getItem() != null) {
                Stats.SpecialEffectEnumerator e = offHand.getItem().getStats().specialEffects();
                while (e.hasNext()) offHandEffects.add(e.next());
            }
        }
    }

    /// <summary>
    /// Resets the Dictionaries, you will need to run CreateDictionaries after this action.
    /// </summary>
    public void reset() {
        totalDamage.clear();
        damageMultipliers.clear();
        damageCritMultipliers.clear();
        damageResistances.clear();
        attackTables.clear();
        totalDamagePerSec.clear();
        totalNumProcs.clear();
        totalNumProcsPerSec.clear();
        dualWieldProcs = false;
    }

    /** Registers one damage type's multiplier, resistance, attack table and zeroed totals. */
    private void registerDamageType(ItemDamageType type, boolean useSpellHit, float damageMultiplier,
            float critMultiplier, float resistance) {
        AttackTable table = new AttackTable(character, stats, useSpellHit, false, levelDelta);
        damageMultipliers.put(type, damageMultiplier);
        damageCritMultipliers.put(type, critMultiplier);
        damageResistances.put(type, resistance);
        attackTables.put(type, table);
        totalDamage.put(type, 0f);
        totalDamagePerSec.put(type, 0f);
        totalNumProcs.put(type, 0f);
        totalNumProcsPerSec.put(type, 0f);
    }

    /// <summary>
    /// Generates the necessary Damage Multipliers and Attack Tables.
    /// This function is automatically called by the constructor.
    /// </summary>
    public void createDictionaries() {
        reset();
        // Partial Spell Resists
        float averageResist = (levelDelta) * 0.02f;
        float resist10 = 5.0f * averageResist;
        float resist20 = 2.5f * averageResist;
        float partialResist = (resist10 * 0.1f + resist20 * 0.2f);

        registerDamageType(ItemDamageType.Physical, false, stats.getBonusPhysicalDamageMultiplier(), stats.getBonusCritMultiplier(), 1f - armorDmgReduc);
        registerDamageType(ItemDamageType.Arcane, true, stats.getBonusArcaneDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
        registerDamageType(ItemDamageType.Holy, true, stats.getBonusHolyDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
        registerDamageType(ItemDamageType.Nature, true, stats.getBonusNatureDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
        registerDamageType(ItemDamageType.Shadow, true, stats.getBonusShadowDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
        registerDamageType(ItemDamageType.Fire, true, stats.getBonusFireDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
        registerDamageType(ItemDamageType.Frost, true, stats.getBonusFrostDamageMultiplier(), stats.getBonusSpellCritMultiplier(), partialResist);
    }

    /** Per-proc damage of the effect for one damage type. */
    private static float damageFor(Stats stats, ItemDamageType type) {
        return switch (type) {
            case Physical -> stats.getPhysicalDamage();
            case Arcane -> stats.getArcaneDamage();
            case Holy -> stats.getHolyDamage();
            case Nature -> stats.getNatureDamage();
            case Shadow -> stats.getShadowDamage();
            case Fire -> stats.getFireDamage();
            case Frost -> stats.getFrostDamage();
        };
    }

    /// <summary>
    /// Performs the meat and potatoes intention of this class.
    /// All the DPS, etc variables will be populated.
    /// Do not run Calculate multiple times for the same ItemDamageType without resetting.
    /// </summary>
    public float calculate(ItemDamageType type) {
        float retVal = 0f;
        try {
            for (SpecialEffect effect : effectsList) {
                if (effect == null
                    || effect.getStats() == null
                    || !triggerChances.containsKey(effect.getTrigger())
                    || !triggerIntervals.containsKey(effect.getTrigger())) {
                } else {
                    float damage = damageFor(effect.getStats(), type);
                    if (damage > 0) retVal = calculateTotalDamagePerSecond(effect, damage, type);
                }
            }
        } catch (RuntimeException e) {
            // ErrorBox (WinForms) omitted
        }
        return retVal;
    }

    /// <summary>
    /// Functional equivalent of calling Calculate() over all possible ItemDamageTypes, with less perf
    /// implication. Note that you should only call this on a new object or after calling reset.
    /// </summary>
    public float calculateAll() {
        float retVal = 0f;

        for (SpecialEffect effect : effectsList) {
            if (!triggerChances.containsKey(effect.getTrigger()) || effect.getStats() == null) continue;
            float physical = damageFor(effect.getStats(), ItemDamageType.Physical);
            if (physical > 0) retVal += calculateTotalDamagePerSecond(effect, physical, ItemDamageType.Physical);
            float arcane = damageFor(effect.getStats(), ItemDamageType.Arcane);
            if (arcane > 0) retVal += calculateTotalDamagePerSecond(effect, arcane, ItemDamageType.Arcane);
            float holy = damageFor(effect.getStats(), ItemDamageType.Holy);
            if (holy > 0) retVal += calculateTotalDamagePerSecond(effect, holy, ItemDamageType.Holy);
            float nature = damageFor(effect.getStats(), ItemDamageType.Nature);
            if (nature > 0) retVal += calculateTotalDamagePerSecond(effect, nature, ItemDamageType.Nature);
            float shadow = damageFor(effect.getStats(), ItemDamageType.Shadow);
            if (shadow > 0) retVal += calculateTotalDamagePerSecond(effect, shadow, ItemDamageType.Shadow);
            float fire = damageFor(effect.getStats(), ItemDamageType.Fire);
            if (fire > 0) retVal += calculateTotalDamagePerSecond(effect, fire, ItemDamageType.Fire);
            float frost = damageFor(effect.getStats(), ItemDamageType.Frost);
            if (frost > 0) retVal += calculateTotalDamagePerSecond(effect, frost, ItemDamageType.Frost);
        }

        return retVal;
    }

    /**
     * Averages one proc: resolves the trigger interval and chance (preferring the main-hand or
     * off-hand entry for weapon procs), converts them to procs per second, multiplies by the
     * per-proc damage from {@link #calculateThisDamage} and accumulates the totals into the public
     * maps. Returns the damage per second of this effect. Exceptions are swallowed (the C# ErrorBox
     * is UI) and yield zero.
     */
    private float calculateTotalDamagePerSecond(SpecialEffect effect, float baseDamage, ItemDamageType type) {
        float totalDamage = 0f;
        float totalDamagePerSec = 0f;
        float totalNumProcs = 0f;
        float totalNumProcsPerSec = 0f;
        try {
            float triggerInterval = triggerIntervals.get(effect.getTrigger());
            float triggerChance = triggerChances.get(effect.getTrigger());
            if (triggerChances.containsKey(Trigger.MainHandHit) && mainHandEffects.contains(effect) && (!offHandEffects.contains(effect) || !dualWieldProcs)) {
                triggerInterval = triggerIntervals.get(Trigger.MainHandHit);
                triggerChance = triggerChances.get(Trigger.MainHandHit);
                if (offHandEffects.contains(effect)) dualWieldProcs = true;
            } else if (triggerChances.containsKey(Trigger.OffHandHit) && offHandEffects.contains(effect)) {
                triggerInterval = triggerIntervals.get(Trigger.OffHandHit);
                triggerChance = triggerChances.get(Trigger.OffHandHit);
            }
            // Process the Effects
            totalNumProcsPerSec = effect.getAverageProcsPerSecond(triggerInterval, triggerChance, character.get(CharacterSlot.MainHand).getSpeed(), fightDuration);
            totalNumProcs = totalNumProcsPerSec * fightDuration;
            totalDamage = totalNumProcs * calculateThisDamage(type, baseDamage);
            totalDamagePerSec = totalDamage / fightDuration;

            // Set our Results into the Dictionaries
            this.totalDamage.put(type, this.totalDamage.get(type) + totalDamage);
            this.totalDamagePerSec.put(type, this.totalDamagePerSec.get(type) + totalDamagePerSec);
            this.totalNumProcs.put(type, this.totalNumProcs.get(type) + totalNumProcs);
            this.totalNumProcsPerSec.put(type, this.totalNumProcsPerSec.get(type) + totalNumProcsPerSec);
        } catch (RuntimeException e) {
            // ErrorBox (WinForms) omitted
        }

        return totalDamagePerSec;
    }

    /**
     * Damage of a single proc after global bonuses, resistance and the attack table: base damage is
     * multiplied by {@code 1 + global damage multiplier} and {@code 1 - resistance}, then split
     * into non-landing outcomes (nothing), glancing blows and blocks (70%), critical strikes (crit
     * multiplier added) and normal hits. Exceptions are swallowed and return zero.
     */
    private float calculateThisDamage(ItemDamageType type, float baseDamage) {
        float dmg = 0;
        try {
            AttackTable MHAtkTable = attackTables.get(type);
            dmg = baseDamage; // Base Damage
            dmg *= 1f + damageMultipliers.get(type);     // Global Damage Bonuses
            dmg *= 1f - damageResistances.get(type);     // Global Damage Penalties

            // Work the Attack Table, note: some of these points will always be zero
            float dmgDrop = (1f
                - MHAtkTable.miss   // no damage when being missed
                - MHAtkTable.dodge  // no damage when being dodged
                - MHAtkTable.parry  // no damage when being parried
                - MHAtkTable.glance // glancing handled below
                - MHAtkTable.block  // blocked  handled below
                - MHAtkTable.crit); // crits    handled below

            float dmgGlance = dmg * MHAtkTable.glance * 0.70f; // Partial Damage when glancing
            float dmgBlock = dmg * MHAtkTable.block * 0.70f;   // Partial damage when blocked
            float dmgCrit = dmg * MHAtkTable.crit * (1f + damageCritMultipliers.get(type)); // Bonus Damage when critting

            dmg *= dmgDrop;

            dmg += dmgGlance + dmgBlock + dmgCrit;
        } catch (RuntimeException e) {
            // ErrorBox (WinForms) omitted
        }

        return dmg;
    }

    /**
     * Minimal single-weapon outcome table used by the proc damage calculation: the chances of miss,
     * dodge, parry, block, glancing blow, crit and normal hit, plus {@link #critProcs}, the
     * weighted crit-chance distribution (a single 100% entry by default). {@code reset} recomputes
     * the table; the always-hit flavor leaves a 100% normal hit. {@link NullCombatTable} is the
     * all-zero instance available as {@link #NULL}.
     */
    public abstract static class CombatTable {
        public static final CombatTable NULL = new NullCombatTable();

        protected Character character;
        protected Stats stats;
        protected boolean useSpellHit = false;
        protected int levelDelta = 0;
        public WeightedStat[] critProcs;

        public float miss;
        public float dodge;
        public float parry;
        public float block;
        public float glance;
        public float crit;
        public float hit;

        private float anyLand = 0f;
        private float anyNotLand = 1f;
        public float getAnyLand() {
            return anyLand;
        }
        public float getAnyNotLand() {
            return anyNotLand;
        }
        private boolean alwaysHit = false;

        protected void calculate() {
            anyNotLand = dodge + parry + miss;
            anyLand = 1f - anyNotLand;
        }

        protected void calculateAlwaysHit() {
            alwaysHit = true;
            miss = dodge = parry = block = glance = crit = 0f;
            hit = 1f;
            anyLand = 1f;
            anyNotLand = 0f;
        }

        protected void initialize(Character character, Stats stats, boolean useSpellHit, boolean alwaysHit, int delta) {
            this.character = character;
            this.stats = stats;
            this.useSpellHit = useSpellHit;
            this.levelDelta = delta;
            critProcs = new WeightedStat[] { new WeightedStat(1f, 0f) };
            reset(alwaysHit);
        }

        protected void reset(boolean alwaysHit) {
            if (alwaysHit) calculateAlwaysHit();
            else calculate();
        }

        public void reset() {
            if (alwaysHit) return;
            reset(false);
        }
    }

    /** All-zero table: everything lands as a normal hit with no crit. */
    public static class NullCombatTable extends CombatTable {
        public NullCombatTable() {
            block = crit = hit = dodge = glance = miss = parry = 0;
        }
    }

    /**
     * Outcome table built from the character's stats with {@link StatConversion}: physical tables
     * use the yellow (special attack) miss, dodge and parry caps plus physical hit; spell tables
     * use the spell miss formula and spell hit. Block and glancing blow are not modeled (left at
     * zero, as in the ported original); crit is the stat crit scaled by the chance to land and
     * averaged over {@link #critProcs}.
     */
    public static class AttackTable extends CombatTable {
        @Override
        protected void calculate() {
            float tableSize = 0f;

            // Miss
            if (useSpellHit) {
                miss = Math.min(1f - tableSize, Math.max(0f, StatConversion.getSpellMiss(-levelDelta, false) - stats.getSpellHit() - StatConversion.getSpellHitFromRating(stats.getHitRating())));
            } else {
                miss = Math.min(1f - tableSize, Math.max(0f, StatConversion.YELLOW_MISS_CHANCE_CAP[levelDelta] - stats.getPhysicalHit() - StatConversion.getSpellHitFromRating(stats.getHitRating())));
            }
            tableSize += miss;
            // Dodge
            if (!useSpellHit) {
                dodge = Math.min(1f - tableSize, Math.max(0f, StatConversion.YELLOW_DODGE_CHANCE_CAP[levelDelta]
                                 - StatConversion.getDodgeParryReducFromExpertise(
                                    StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), character.getCharacterClass()), character.getCharacterClass()))
                                );
                tableSize += dodge;
            } else {
                dodge = 0f;
            }
            // Parry
            if (!useSpellHit) {
                parry = Math.min(1f - tableSize, Math.max(0f, StatConversion.YELLOW_PARRY_CHANCE_CAP[levelDelta]
                                 - StatConversion.getDodgeParryReducFromExpertise(
                                    StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), character.getCharacterClass()), character.getCharacterClass()))
                                );
                tableSize += parry;
            } else {
                parry = 0f;
            }
            // Block
            //if (isWhite || Abil.CanBeBlocked) {
                //Block = Math.Min(1f - tableSize, isMH ?  combatFactors._c_mhblock : combatFactors._c_ohblock);
                //tableSize += Block;
            //} else { Block = 0f; }
            // Glancing Blow
            //if (isWhite) {
                //Glance = Math.Min(1f - tableSize, combatFactors._c_glance);
                //tableSize += Glance;
            //} else { Glance = 0f; }
            // Critical Hit
            crit = 0;
            /*if (isWhite) {
                float critValueToUse = (isMH ? combatFactors._c_mhwcrit : combatFactors._c_ohwcrit);
                foreach (WeightedStat ws in combatFactors.critProcs)
                {
                    float modCritChance = Math.Min(1f - tableSize, critValueToUse + StatConversion.GetCritFromRating(ws.Value, Char.Class))
                        + StatConversion.NPC_LEVEL_CRIT_MOD[calcOpts.TargetLevel - Char.Level];
                    Crit += ws.Chance * modCritChance;
                }
                tableSize += Crit;
            } else if (Abil.CanCrit) {*/
                float critValueToUse = StatConversion.NPC_LEVEL_CRIT_MOD[levelDelta]
                    + (useSpellHit ? stats.getSpellCrit() : stats.getPhysicalCrit());
                for (WeightedStat ws : critProcs) {
                    float modCritChance = Math.min(1f - tableSize, (critValueToUse + StatConversion.getCritFromRating(ws.value(), character.getCharacterClass())) * (1f - dodge - miss));
                    crit += ws.chance() * modCritChance;
                }
                if (crit < 0) crit = 0f;
                tableSize += crit;
            //}
            // Normal Hit
            hit = Math.max(0f, 1f - tableSize);
            super.calculate();
        }

        public AttackTable() {
        }

        public AttackTable(Character character, Stats stats, boolean useSpellHit, boolean alwaysHit, int delta) {
            initialize(character, stats, useSpellHit, alwaysHit, delta);
        }
    }
}
