// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

import rawr.base.effects.SpecialEffect;
import rawr.base.util.ArrayUtils;
import rawr.base.util.Extensions;
import rawr.base.util.NetFormat;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * A snapshot of every character stat Rawr tracks: a bag of several hundred named {@code float}s
 * with fixed rules for how two snapshots combine.
 *
 * <p>This is the central value type of the engine. Gear, buffs, consumables, racials and model
 * effects all contribute to {@code Stats} objects; the model's simulation reads them back. The
 * public surface is the generated-looking getter/setter pairs ({@code getArmor}, {@code setArmor},
 * {@code getHitRating}, ...), not a map, because the original C# engine used one field per stat and
 * result compatibility depends on that exact layout.</p>
 *
 * <h2>Storage: five arrays plus special effects</h2>
 *
 * <p>Each stat belongs to exactly one <i>combination category</i>, chosen by which enum indexes its
 * slot:</p>
 * <ul>
 *   <li>{@link AdditiveStat} &mdash; plain addition ({@code a+b}). Most stats live here.</li>
 *   <li>{@link MultiplicativeStat} &mdash; percentage bonuses combine as
 *       {@code (1+a)*(1+b)-1}.</li>
 *   <li>{@link InverseMultiplicativeStat} &mdash; reductions combine as
 *       {@code 1-(1-a)*(1-b)}.</li>
 *   <li>{@link NonStackingStat} &mdash; "strongest wins", {@code max(a,b)}.</li>
 *   <li>{@link SingleSourceStat} &mdash; single-writer stats that no merge operation combines; the
 *       Java replacement for the C# {@code StatsCat}/{@code StatsBear} subclass properties (the
 *       enum documents the full contract).</li>
 * </ul>
 *
 * <p>The backing storage is one {@code float[]} per category ({@code rawAdditiveData}, ...) indexed
 * by the enum's {@code value()}. Procs and other duration/cooldown effects are <b>not</b> flattened
 * here: they are held as {@link SpecialEffect} references in {@code rawSpecialEffectData} so a
 * model can decide how to average them (see {@link SpecialEffect#accumulateAverageStats}).</p>
 *
 * <h2>Combining snapshots</h2>
 *
 * <ul>
 *   <li>{@link #accumulate(Stats)} / {@link #accumulate(Stats, float)} &mdash; merge another
 *       snapshot into this one, applying the per-category rules and appending its special effects;
 *       the float overload scales every contribution by that weight. This is what models call in a
 *       loop over gear and buffs.</li>
 *   <li>{@link #add(Stats, Stats)}, {@link #subtract(Stats, Stats)},
 *       {@link #multiply(Stats, float)} &mdash; allocate a new snapshot; subtraction inverts the
 *       multiplicative rules with division.</li>
 *   <li>{@link #copy()} &mdash; deep copy (C# {@code Clone()}): independent arrays, cloned effect
 *       array, shared sparse index; {@link SingleSourceStat} values are carried over.</li>
 *   <li>{@link #accumulateUnsafe(Stats)} / {@link #accumulateUnsafe(Stats, boolean)} &mdash; hot-loop
 *       variant whose flag can force the source's sparse index to be built before accumulating
 *       (the C# pointer fast path itself is not needed in Java; the name comes from the original).</li>
 * </ul>
 *
 * <h2>Sparse-data optimization</h2>
 *
 * <p>Most snapshots have very few non-zero entries, so {@link #generateSparseData()} caches the
 * indices of the non-zero slots in the four combining arrays and {@code accumulate} walks only
 * those. {@link SingleSourceStat} values are never merged, so they need no sparse bookkeeping.
 * <b>Every setter that changes a value must invalidate that cache</b>
 * ({@link #invalidateSparseData()}); the code relies on this. The cache is lazily rebuilt on the
 * next {@code generateSparseData()} call. Do not bypass this invariant in new code.</p>
 *
 * <h2>Metadata, comparison and equality</h2>
 *
 * <ul>
 *   <li>{@link StatProperty} + {@link #PROPERTY_INFO} &mdash; for every stat: its programmatic name,
 *       UI display name, category, whether it is a percentage and a minimum sensible value, plus
 *       accessor/mutator lambdas. {@link #values(StatFilter)}, {@link #getStatNames()} and the
 *       comparison helpers are built on it; the golden test suite uses it to iterate all stats.</li>
 *   <li>{@link #compare(Stats)} / {@link #greaterOrEqual(Stats, Stats)} and friends &mdash;
 *       partial ordering: a snapshot is "greater" only if every stat is greater, hence the
 *       {@link ArrayUtils.CompareResult} result.</li>
 *   <li>{@link #structurallyEquals(Stats)} &mdash; the C# {@code operator ==}: reference equality,
 *       or exact array equality when neither side carries special effects. Java's
 *       {@code equals}/{@code hashCode} are overridden to the same structural semantics.</li>
 * </ul>
 *
 * <p><b>Numeric fidelity:</b> values are {@code float} and arithmetic order is observable. Do not
 * widen to {@code double}, regroup sums or replace loops with streams.</p>
 *
 * @see AdditiveStat
 * @see SingleSourceStat
 * @see SpecialEffect
 * @see StatConversion
 */
public class Stats {
    static final int ADDITIVE_STAT_COUNT = AdditiveStat.values().length;
    static final int MULTIPLICATIVE_STAT_COUNT = MultiplicativeStat.values().length;
    static final int INVERSE_MULTIPLICATIVE_STAT_COUNT = InverseMultiplicativeStat.values().length;
    static final int NON_STACKING_STAT_COUNT = NonStackingStat.values().length;
    static final int SINGLE_SOURCE_STAT_COUNT = SingleSourceStat.values().length;

    final float[] rawAdditiveData = new float[ADDITIVE_STAT_COUNT];
    final float[] rawMultiplicativeData = new float[MULTIPLICATIVE_STAT_COUNT];
    final float[] rawInverseMultiplicativeData = new float[INVERSE_MULTIPLICATIVE_STAT_COUNT];
    final float[] rawNoStackData = new float[NON_STACKING_STAT_COUNT];
    // Never combined by accumulate/add/subtract/multiply; copied by copy() (see SingleSourceStat).
    final float[] rawSingleSourceData = new float[SINGLE_SOURCE_STAT_COUNT];

    private SpecialEffect[] rawSpecialEffectData = null;
    static final int DEFAULT_SPECIAL_EFFECT_DATA_CAPACITY = 4;
    private int rawSpecialEffectDataSize;

    int[] sparseIndices;
    int sparseAdditiveCount;
    int sparseMultiplicativeCount;
    int sparseInverseMultiplicativeCount;
    int sparseNoStackCount;

    public Stats() {
    }

    /** Reads one stat from a {@code Stats} instance; stored per entry in {@link #PROPERTY_INFO}. */
    @FunctionalInterface
    public interface StatAccessor {
        float get(Stats stats);
    }

    /** Writes one stat on a {@code Stats} instance; stored per entry in {@link #PROPERTY_INFO}. */
    @FunctionalInterface
    public interface StatMutator {
        void set(Stats stats, float value);
    }

    /**
     * Metadata for one stat: its programmatic name (the serialization key), optional UI display
     * name, UI/model category, whether the value is a percentage and its sensible minimum, plus the
     * <b>unbound</b> accessor/mutator that read and write the stat on any {@code Stats} instance.
     * {@link #PROPERTY_INFO} is the ordered list of these entries; the golden harness iterates it.
     *
     * @param name programmatic name used by {@link #PROPERTY_BY_NAME}, the XML loaders and the
     *             oracle JSON keys
     * @param displayName UI label, or {@code null} to fall back to the name
     * @param category grouping used by the UI and the coverage audit (model or "Base Stats"/...)
     * @param percentage true when the value is a fraction that is displayed as a percentage
     * @param minRange minimum sensible value for range validation ({@code -1f} = unconstrained)
     * @param accessor reads the stat from a given {@code Stats}
     * @param mutator writes the stat on a given {@code Stats}
     */
    public record StatProperty(String name, String displayName, String category, boolean percentage, float minRange,
                               StatAccessor accessor, StatMutator mutator) {
    }

    /**
     * Ordered metadata for every stat in C# declaration order (see {@link StatProperty}); used for
     * iteration ({@link #values(StatFilter)}, {@link #getStatNames()}, {@code toString}) and by
     * {@link #PROPERTY_BY_NAME}. The stat accessor sections further down are grouped by category.
     */
    public static final StatProperty[] PROPERTY_INFO = new StatProperty[] {
        new StatProperty("Armor", null, "Base Stats", false, 80f, Stats::getArmor, Stats::setArmor),
        new StatProperty("BonusArmor", null, "Base Stats", false, 80f, Stats::getBonusArmor, Stats::setBonusArmor),
        new StatProperty("Health", null, "Base Stats", false, 0f, Stats::getHealth, Stats::setHealth),
        new StatProperty("Mana", null, "Base Stats", false, -1f, Stats::getMana, Stats::setMana),
        new StatProperty("Agility", null, "Base Stats", false, 10f, Stats::getAgility, Stats::setAgility),
        new StatProperty("BaseAgility", null, "Misc", false, -1f, Stats::getBaseAgility, Stats::setBaseAgility),
        new StatProperty("Stamina", null, "Base Stats", false, 0f, Stats::getStamina, Stats::setStamina),
        new StatProperty("AttackPower", null, "Base Stats", false, 20f, Stats::getAttackPower, Stats::setAttackPower),
        new StatProperty("AttackPowerDemons", null, "Misc", false, -1f, Stats::getAttackPowerDemons, Stats::setAttackPowerDemons),
        new StatProperty("Strength", null, "Base Stats", false, 20f, Stats::getStrength, Stats::setStrength),
        new StatProperty("BossAttackPower", null, "Buffs / Debuffs", false, -1f, Stats::getBossAttackPower, Stats::setBossAttackPower),
        new StatProperty("WeaponDamage", null, "Misc", false, -1f, Stats::getWeaponDamage, Stats::setWeaponDamage),
        new StatProperty("ScopeDamage", null, "Misc", false, 0f, Stats::getScopeDamage, Stats::setScopeDamage),
        new StatProperty("ArmorPenetrationRating", "Armor Penetration Rating", "Base Stats", false, 0f, Stats::getArmorPenetrationRating, Stats::setArmorPenetrationRating),
        new StatProperty("Intellect", null, "Base Stats", false, 10f, Stats::getIntellect, Stats::setIntellect),
        new StatProperty("Spirit", null, "Base Stats", false, 10f, Stats::getSpirit, Stats::setSpirit),
        new StatProperty("SpellPower", "Spell Power", "Base Stats", false, 0f, Stats::getSpellPower, Stats::setSpellPower),
        new StatProperty("SpellPenetration", "Spell Penetration", "Base Stats", false, -1f, Stats::getSpellPenetration, Stats::setSpellPenetration),
        new StatProperty("CritRating", "Crit Rating", "Base Stats", false, 0f, Stats::getCritRating, Stats::setCritRating),
        new StatProperty("HitRating", "Hit Rating", "Base Stats", false, 0f, Stats::getHitRating, Stats::setHitRating),
        new StatProperty("MasteryRating", "Mastery Rating", "Base Stats", false, 0f, Stats::getMasteryRating, Stats::setMasteryRating),
        new StatProperty("DodgeRating", "Dodge Rating", "Base Stats", false, 0f, Stats::getDodgeRating, Stats::setDodgeRating),
        new StatProperty("ParryRating", "Parry Rating", "Base Stats", false, 0f, Stats::getParryRating, Stats::setParryRating),
        new StatProperty("BlockRating", "Block Rating", "Base Stats", false, 0f, Stats::getBlockRating, Stats::setBlockRating),
        new StatProperty("BlockValue", "Block Value", "Base Stats", false, 0f, Stats::getBlockValue, Stats::setBlockValue),
        new StatProperty("DefenseRating", "Defense Rating", "Base Stats", false, 0f, Stats::getDefenseRating, Stats::setDefenseRating),
        new StatProperty("Resilience", "Resilience", "Base Stats", false, 0f, Stats::getResilience, Stats::setResilience),
        new StatProperty("ExpertiseRating", "Expertise Rating", "Base Stats", false, 0f, Stats::getExpertiseRating, Stats::setExpertiseRating),
        new StatProperty("HasteRating", "Haste Rating", "Base Stats", false, 10f, Stats::getHasteRating, Stats::setHasteRating),
        new StatProperty("Mp5", "Mana per 5 sec", "Base Stats", false, 0f, Stats::getMp5, Stats::setMp5),
        new StatProperty("Hp5", "Health per 5 sec", "Base Stats", false, 0f, Stats::getHp5, Stats::setHp5),
        new StatProperty("FrostResistance", "Frost Res", "Resistances", false, -1f, Stats::getFrostResistance, Stats::setFrostResistance),
        new StatProperty("NatureResistance", "Nature Res", "Resistances", false, -1f, Stats::getNatureResistance, Stats::setNatureResistance),
        new StatProperty("FireResistance", "Fire Res", "Resistances", false, -1f, Stats::getFireResistance, Stats::setFireResistance),
        new StatProperty("ShadowResistance", "Shadow Res", "Resistances", false, -1f, Stats::getShadowResistance, Stats::setShadowResistance),
        new StatProperty("ArcaneResistance", "Arcane Res", "Resistances", false, -1f, Stats::getArcaneResistance, Stats::setArcaneResistance),
        new StatProperty("HealingReceivedMultiplier", "% Healing Received", "Buffs / Debuffs", true, -1f, Stats::getHealingReceivedMultiplier, Stats::setHealingReceivedMultiplier),
        new StatProperty("BonusHealingDoneMultiplier", "% Healing Done", "Buffs / Debuffs", true, -1f, Stats::getBonusHealingDoneMultiplier, Stats::setBonusHealingDoneMultiplier),
        new StatProperty("ManaRestoreFromBaseManaPPM", "% Base Mana / Minute (PPM)", "Buffs / Debuffs", true, -1f, Stats::getManaRestoreFromBaseManaPPM, Stats::setManaRestoreFromBaseManaPPM),
        new StatProperty("ManaRestoreFromMaxManaPerSecond", "% Max Mana / Sec", "Buffs / Debuffs", true, -1f, Stats::getManaRestoreFromMaxManaPerSecond, Stats::setManaRestoreFromMaxManaPerSecond),
        new StatProperty("BonusFireDamageMultiplier", "% Fire Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusFireDamageMultiplier, Stats::setBonusFireDamageMultiplier),
        new StatProperty("BonusShadowDamageMultiplier", "% Shadow Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusShadowDamageMultiplier, Stats::setBonusShadowDamageMultiplier),
        new StatProperty("BonusHolyDamageMultiplier", "% Holy Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusHolyDamageMultiplier, Stats::setBonusHolyDamageMultiplier),
        new StatProperty("BonusArcaneDamageMultiplier", "% Arcane Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusArcaneDamageMultiplier, Stats::setBonusArcaneDamageMultiplier),
        new StatProperty("BonusNatureDamageMultiplier", "% Nature Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusNatureDamageMultiplier, Stats::setBonusNatureDamageMultiplier),
        new StatProperty("BonusFrostDamageMultiplier", "% Frost Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusFrostDamageMultiplier, Stats::setBonusFrostDamageMultiplier),
        new StatProperty("BonusDiseaseDamageMultiplier", "% Disease Damage", "Buffs / Debuffs", true, -1f, Stats::getBonusDiseaseDamageMultiplier, Stats::setBonusDiseaseDamageMultiplier),
        new StatProperty("BonusSpiritMultiplier", "% Spirit", "Buffs / Debuffs", true, -1f, Stats::getBonusSpiritMultiplier, Stats::setBonusSpiritMultiplier),
        new StatProperty("BonusCritMultiplier", "% Crit Dmg", "Buffs / Debuffs", true, -1f, Stats::getBonusCritMultiplier, Stats::setBonusCritMultiplier),
        new StatProperty("BonusSpellCritMultiplier", "% Spell Crit Dmg", "Buffs / Debuffs", true, -1f, Stats::getBonusSpellCritMultiplier, Stats::setBonusSpellCritMultiplier),
        new StatProperty("BonusPhysicalDamageMultiplier", "% Physical Dmg", "Buffs / Debuffs", true, -1f, Stats::getBonusPhysicalDamageMultiplier, Stats::setBonusPhysicalDamageMultiplier),
        new StatProperty("BonusDamageMultiplier", "% Dmg", "Buffs / Debuffs", true, -1f, Stats::getBonusDamageMultiplier, Stats::setBonusDamageMultiplier),
        new StatProperty("ManaCostPerc", "% of Base Ability Mana Cost", "Buffs / Debuffs", true, -1f, Stats::getManaCostPerc, Stats::setManaCostPerc),
        new StatProperty("ArmorPenetration", "% Armor Penetration", "Combat Values", true, -1f, Stats::getArmorPenetration, Stats::setArmorPenetration),
        new StatProperty("SpellCrit", "% Spell Crit", "Combat Values", true, -1f, Stats::getSpellCrit, Stats::setSpellCrit),
        new StatProperty("SpellCritOnTarget", "% Spell Crit on target", "Combat Values", true, -1f, Stats::getSpellCritOnTarget, Stats::setSpellCritOnTarget),
        new StatProperty("SpellHit", "% Spell Hit", "Combat Values", true, -1f, Stats::getSpellHit, Stats::setSpellHit),
        new StatProperty("SpellHaste", "% Spell Haste", "Combat Values", true, -1f, Stats::getSpellHaste, Stats::setSpellHaste),
        new StatProperty("SpellCombatManaRegeneration", "Combat Mana Regeneration", "Combat Values", false, -1f, Stats::getSpellCombatManaRegeneration, Stats::setSpellCombatManaRegeneration),
        new StatProperty("PhysicalCrit", "% Physical Crit", "Combat Values", true, -1f, Stats::getPhysicalCrit, Stats::setPhysicalCrit),
        new StatProperty("Parry", "% Parry", "Combat Values", true, -1f, Stats::getParry, Stats::setParry),
        new StatProperty("Block", "% Block", "Combat Values", true, -1f, Stats::getBlock, Stats::setBlock),
        new StatProperty("PhysicalHit", "% Hit", "Combat Values", true, -1f, Stats::getPhysicalHit, Stats::setPhysicalHit),
        new StatProperty("Dodge", "% Dodge", "Combat Values", true, -1f, Stats::getDodge, Stats::setDodge),
        new StatProperty("Defense", null, "Combat Values", false, -1f, Stats::getDefense, Stats::setDefense),
        new StatProperty("Expertise", null, "Combat Values", false, -1f, Stats::getExpertise, Stats::setExpertise),
        new StatProperty("PhysicalHaste", "% Physical Haste", "Combat Values", true, -1f, Stats::getPhysicalHaste, Stats::setPhysicalHaste),
        new StatProperty("RangedHaste", "% Ranged Haste", "Combat Values", true, -1f, Stats::getRangedHaste, Stats::setRangedHaste),
        new StatProperty("Miss", "% Miss", "Combat Values", true, -1f, Stats::getMiss, Stats::setMiss),
        new StatProperty("SpellShadowDamageRating", "Spell Shadow Damage", "Spell Combat Ratings", false, 0f, Stats::getSpellShadowDamageRating, Stats::setSpellShadowDamageRating),
        new StatProperty("SpellFireDamageRating", "Spell Fire Damage", "Spell Combat Ratings", false, 0f, Stats::getSpellFireDamageRating, Stats::setSpellFireDamageRating),
        new StatProperty("SpellFrostDamageRating", "Spell Frost Damage", "Spell Combat Ratings", false, 0f, Stats::getSpellFrostDamageRating, Stats::setSpellFrostDamageRating),
        new StatProperty("SpellArcaneDamageRating", "Spell Arcane Damage", "Spell Combat Ratings", false, 0f, Stats::getSpellArcaneDamageRating, Stats::setSpellArcaneDamageRating),
        new StatProperty("SpellNatureDamageRating", "Spell Nature Damage", "Spell Combat Ratings", false, 0f, Stats::getSpellNatureDamageRating, Stats::setSpellNatureDamageRating),
        new StatProperty("CritBonusDamage", "% Crit Bonus Damage", "Equipment Effects", true, -1f, Stats::getCritBonusDamage, Stats::setCritBonusDamage),
        new StatProperty("InterruptProtection", null, "Misc", false, -1f, Stats::getInterruptProtection, Stats::setInterruptProtection),
        new StatProperty("RuptureDamageBonus", null, "Rogue", false, -1f, Stats::getRuptureDamageBonus, Stats::setRuptureDamageBonus),
        new StatProperty("ComboMoveEnergyReduction", null, "Rogue", false, -1f, Stats::getComboMoveEnergyReduction, Stats::setComboMoveEnergyReduction),
        new StatProperty("BonusEnergyFromDP", null, "Rogue", false, -1f, Stats::getBonusEnergyFromDP, Stats::setBonusEnergyFromDP),
        new StatProperty("RuptureCrit", null, "Rogue", false, -1f, Stats::getRuptureCrit, Stats::setRuptureCrit),
        new StatProperty("ReduceEnergyCostFromRupture", null, "Rogue", false, -1f, Stats::getReduceEnergyCostFromRupture, Stats::setReduceEnergyCostFromRupture),
        new StatProperty("BonusCPGCritChance", null, "Rogue", false, -1f, Stats::getBonusCPGCritChance, Stats::setBonusCPGCritChance),
        new StatProperty("BonusToTTEnergy", null, "Rogue", false, -1f, Stats::getBonusToTTEnergy, Stats::setBonusToTTEnergy),
        new StatProperty("ChanceOn3CPOnFinisher", null, "Rogue", false, -1f, Stats::getChanceOn3CPOnFinisher, Stats::setChanceOn3CPOnFinisher),
        new StatProperty("RPp5", "Runic Power per 5 Seconds", "Death Knight", false, -1f, Stats::getRPp5, Stats::setRPp5),
        new StatProperty("BonusObliterateDamage", "Bonus Obliterate Damage", "Death Knight", false, -1f, Stats::getBonusObliterateDamage, Stats::setBonusObliterateDamage),
        new StatProperty("DiseasesCanCrit", "Diseases Can Crit", "Death Knight", false, -1f, Stats::getDiseasesCanCrit, Stats::setDiseasesCanCrit),
        new StatProperty("BonusHeartStrikeMultiplier", "% Bonus Heart Strike Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusHeartStrikeMultiplier, Stats::setBonusHeartStrikeMultiplier),
        new StatProperty("BonusScourgeStrikeMultiplier", "% Bonus Scourge Strike Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusScourgeStrikeMultiplier, Stats::setBonusScourgeStrikeMultiplier),
        new StatProperty("BonusObliterateMultiplier", "% Bonus Obliterate Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusObliterateMultiplier, Stats::setBonusObliterateMultiplier),
        new StatProperty("BonusScourgeStrikeDamage", "Bonus Scourge Strike Damage", "Death Knight", false, -1f, Stats::getBonusScourgeStrikeDamage, Stats::setBonusScourgeStrikeDamage),
        new StatProperty("BonusBloodStrikeDamage", null, null, false, -1f, Stats::getBonusBloodStrikeDamage, Stats::setBonusBloodStrikeDamage),
        new StatProperty("BonusHeartStrikeDamage", null, null, false, -1f, Stats::getBonusHeartStrikeDamage, Stats::setBonusHeartStrikeDamage),
        new StatProperty("BonusDeathCoilDamage", "Bonus Death Coil Damage", "Death Knight", false, -1f, Stats::getBonusDeathCoilDamage, Stats::setBonusDeathCoilDamage),
        new StatProperty("BonusFrostStrikeDamage", "Bonus Frost Strike Damage", "Death Knight", false, -1f, Stats::getBonusFrostStrikeDamage, Stats::setBonusFrostStrikeDamage),
        new StatProperty("BonusHowlingBlastDamage", "Bonus Howling Blast Damage", "Death Knight", false, -1f, Stats::getBonusHowlingBlastDamage, Stats::setBonusHowlingBlastDamage),
        new StatProperty("BonusFrostWeaponDamage", "% White Damage as Frost Damage", "Death Knight", true, -1f, Stats::getBonusFrostWeaponDamage, Stats::setBonusFrostWeaponDamage),
        new StatProperty("CinderglacierProc", "Cinderglacier Proc", "Death Knight", false, -1f, Stats::getCinderglacierProc, Stats::setCinderglacierProc),
        new StatProperty("BonusDeathStrikeDamage", "Bonus Death Strike Damage", "Death Knight", false, -1f, Stats::getBonusDeathStrikeDamage, Stats::setBonusDeathStrikeDamage),
        new StatProperty("BonusIcyTouchDamage", "Bonus Icy Touch Damage", "Death Knight", false, -1f, Stats::getBonusIcyTouchDamage, Stats::setBonusIcyTouchDamage),
        new StatProperty("BonusAntiMagicShellDamageReduction", "Bonus AMS Damage Reduction", "Death Knight", false, -1f, Stats::getBonusAntiMagicShellDamageReduction, Stats::setBonusAntiMagicShellDamageReduction),
        new StatProperty("BonusDeathCoilCrit", "% Bonus Death Coil Crit", "Death Knight", true, -1f, Stats::getBonusDeathCoilCrit, Stats::setBonusDeathCoilCrit),
        new StatProperty("BonusDeathStrikeCrit", "% Bonus Death Strike Crit", "Death Knight", true, -1f, Stats::getBonusDeathStrikeCrit, Stats::setBonusDeathStrikeCrit),
        new StatProperty("BonusFrostStrikeCrit", "% Bonus Frost Strike Crit", "Death Knight", true, -1f, Stats::getBonusFrostStrikeCrit, Stats::setBonusFrostStrikeCrit),
        new StatProperty("BonusIceboundFortitudeDuration", "Bonus IBF Duration", "Death Knight", false, -1f, Stats::getBonusIceboundFortitudeDuration, Stats::setBonusIceboundFortitudeDuration),
        new StatProperty("BonusObliterateCrit", "% Obliterate Crit", "Death Knight", true, -1f, Stats::getBonusObliterateCrit, Stats::setBonusObliterateCrit),
        new StatProperty("BonusPerDiseaseBloodStrikeDamage", "% Bonus Damage Per Disease For Blood Strike", "Death Knight", true, -1f, Stats::getBonusPerDiseaseBloodStrikeDamage, Stats::setBonusPerDiseaseBloodStrikeDamage),
        new StatProperty("BonusPerDiseaseHeartStrikeDamage", "% Bonus Damage Per Disease For Heart Strike", "Death Knight", true, -1f, Stats::getBonusPerDiseaseHeartStrikeDamage, Stats::setBonusPerDiseaseHeartStrikeDamage),
        new StatProperty("BonusPerDiseaseObliterateDamage", "% Bonus Damage Per Disease For Obliterate", "Death Knight", true, -1f, Stats::getBonusPerDiseaseObliterateDamage, Stats::setBonusPerDiseaseObliterateDamage),
        new StatProperty("BonusPerDiseaseScourgeStrikeDamage", "% Bonus Damage Per Disease For Scourge Strike", "Death Knight", true, -1f, Stats::getBonusPerDiseaseScourgeStrikeDamage, Stats::setBonusPerDiseaseScourgeStrikeDamage),
        new StatProperty("BonusPlagueStrikeCrit", "% Bonus Plague Strike Crit", "Death Knight", true, -1f, Stats::getBonusPlagueStrikeCrit, Stats::setBonusPlagueStrikeCrit),
        new StatProperty("BonusRPFromDeathStrike", "Bonus RP From Death Strike", "Death Knight", false, -1f, Stats::getBonusRPFromDeathStrike, Stats::setBonusRPFromDeathStrike),
        new StatProperty("BonusRPFromObliterate", "Bonus RP From Obliterate", "Death Knight", false, -1f, Stats::getBonusRPFromObliterate, Stats::setBonusRPFromObliterate),
        new StatProperty("BonusRPFromScourgeStrike", "Bonus RP From Scourge Strike", "Death Knight", false, -1f, Stats::getBonusRPFromScourgeStrike, Stats::setBonusRPFromScourgeStrike),
        new StatProperty("BonusRuneStrikeMultiplier", "% Rune Strike Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusRuneStrikeMultiplier, Stats::setBonusRuneStrikeMultiplier),
        new StatProperty("BonusBloodStrikeDamageMultiplier", "% Blood Strike Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusBloodStrikeDamageMultiplier, Stats::setBonusBloodStrikeDamageMultiplier),
        new StatProperty("BonusHeartStrikeDamageMultiplier", "% Heart Strike Damage Multiplier", "Death Knight", true, -1f, Stats::getBonusHeartStrikeDamageMultiplier, Stats::setBonusHeartStrikeDamageMultiplier),
        new StatProperty("BonusScourgeStrikeCrit", "% Bonus Scourge Strike Crit", "Death Knight", true, -1f, Stats::getBonusScourgeStrikeCrit, Stats::setBonusScourgeStrikeCrit),
        new StatProperty("TankDK_T10_2pc", "% Increase damage by DnD", "Death Knight", true, -1f, Stats::getTankDK_T10_2pc, Stats::setTankDK_T10_2pc),
        new StatProperty("TankDK_T10_4pc", "% Damage Reduction by Blood Tap", "Death Knight", true, -1f, Stats::getTankDK_T10_4pc, Stats::setTankDK_T10_4pc),
        new StatProperty("TerrorProc", null, "Feral", false, -1f, Stats::getTerrorProc, Stats::setTerrorProc),
        new StatProperty("MangleCatCostReduction", null, "Feral", false, -1f, Stats::getMangleCatCostReduction, Stats::setMangleCatCostReduction),
        new StatProperty("RipCostReduction", null, "Feral", false, -1f, Stats::getRipCostReduction, Stats::setRipCostReduction),
        new StatProperty("RakeCostReduction", null, "Feral", false, -1f, Stats::getRakeCostReduction, Stats::setRakeCostReduction),
        new StatProperty("ShredCostReduction", null, "Feral", false, -1f, Stats::getShredCostReduction, Stats::setShredCostReduction),
        new StatProperty("BonusCPOnCrit", null, "Feral", false, -1f, Stats::getBonusCPOnCrit, Stats::setBonusCPOnCrit),
        new StatProperty("BonusEnergyOnTigersFury", null, "Feral", false, -1f, Stats::getBonusEnergyOnTigersFury, Stats::setBonusEnergyOnTigersFury),
        new StatProperty("FinisherEnergyOnAvoid", null, "Feral", false, -1f, Stats::getFinisherEnergyOnAvoid, Stats::setFinisherEnergyOnAvoid),
        new StatProperty("BonusShredDamage", null, "Feral", false, -1f, Stats::getBonusShredDamage, Stats::setBonusShredDamage),
        new StatProperty("BonusRipDamagePerCPPerTick", "Bonus Rip Damage Per Combo Point Per Tick", "Feral", false, -1f, Stats::getBonusRipDamagePerCPPerTick, Stats::setBonusRipDamagePerCPPerTick),
        new StatProperty("BonusRakeDuration", "Bonus Rake Duration", "Feral", false, -1f, Stats::getBonusRakeDuration, Stats::setBonusRakeDuration),
        new StatProperty("BonusRipDuration", "Bonus Rip Duration", "Feral", false, -1f, Stats::getBonusRipDuration, Stats::setBonusRipDuration),
        new StatProperty("BonusSavageRoarDuration", "Bonus Savage Roar Duration", "Feral", false, -1f, Stats::getBonusSavageRoarDuration, Stats::setBonusSavageRoarDuration),
        new StatProperty("ClearcastOnBleedChance", "% chance to proc Clearcast On Bleed Damage", "Feral", true, -1f, Stats::getClearcastOnBleedChance, Stats::setClearcastOnBleedChance),
        new StatProperty("WindfuryAPBonus", "Windfury", "Deprecated", false, -1f, Stats::getWindfuryAPBonus, Stats::setWindfuryAPBonus),
        new StatProperty("SpellDamageFromIntellectPercentage", null, "Misc", false, -1f, Stats::getSpellDamageFromIntellectPercentage, Stats::setSpellDamageFromIntellectPercentage),
        new StatProperty("SpellDamageFromSpiritPercentage", null, "Misc", false, -1f, Stats::getSpellDamageFromSpiritPercentage, Stats::setSpellDamageFromSpiritPercentage),
        new StatProperty("CritChanceReduction", null, "Deprecated", false, -1f, Stats::getCritChanceReduction, Stats::setCritChanceReduction),
        new StatProperty("ManaorEquivRestore", "% Mana or Equivalent Restore", "Equipment Effects", true, -1f, Stats::getManaorEquivRestore, Stats::setManaorEquivRestore),
        new StatProperty("HealthRestore", "Health Restored", "Equipment Effects", false, -1f, Stats::getHealthRestore, Stats::setHealthRestore),
        new StatProperty("HealthRestoreFromMaxHealth", "% of Max Health Restored", "Equipment Effects", true, -1f, Stats::getHealthRestoreFromMaxHealth, Stats::setHealthRestoreFromMaxHealth),
        new StatProperty("ManaRestore", "Mana Restored", "Equipment Effects", false, -1f, Stats::getManaRestore, Stats::setManaRestore),
        new StatProperty("PVPTrinket", "PvP Trinket", "Equipment Effects", false, -1f, Stats::getPVPTrinket, Stats::setPVPTrinket),
        new StatProperty("BonusHealingReceived", "Bonus Healing Received", "Equipment Effects", false, -1f, Stats::getBonusHealingReceived, Stats::setBonusHealingReceived),
        new StatProperty("ExtractOfNecromanticPowerProc", "Extract of Necromantic Power proc", "Equipment Effects", false, -1f, Stats::getExtractOfNecromanticPowerProc, Stats::setExtractOfNecromanticPowerProc),
        new StatProperty("DarkmoonCardDeathProc", "Darkmoon Card: Death proc", "Equipment Effects", false, -1f, Stats::getDarkmoonCardDeathProc, Stats::setDarkmoonCardDeathProc),
        new StatProperty("ShieldFromHealed", "% Shield from Heal Amount", "Equipment Effects", true, -1f, Stats::getShieldFromHealed, Stats::setShieldFromHealed),
        new StatProperty("DeathbringerProc", null, "Equipment Effects", false, -1f, Stats::getDeathbringerProc, Stats::setDeathbringerProc),
        new StatProperty("ZodProc", "% Chance to proc an extra half damage shot", "Equipment Effects", true, -1f, Stats::getZodProc, Stats::setZodProc),
        new StatProperty("BattlemasterHealth", "Temporary Health", "Equipment Effects", false, -1f, Stats::getBattlemasterHealth, Stats::setBattlemasterHealth),
        new StatProperty("HighestStat", "Highest Stat", "Equipment Effects", false, -1f, Stats::getHighestStat, Stats::setHighestStat),
        new StatProperty("Paragon", "Strength or Agility", "Equipment Effects", false, -1f, Stats::getParagon, Stats::setParagon),
        new StatProperty("Healed", "Healed", "Equipment Effects", false, -1f, Stats::getHealed, Stats::setHealed),
        new StatProperty("DamageAbsorbed", "Damage Absorbed", "Equipment Effects", false, 0f, Stats::getDamageAbsorbed, Stats::setDamageAbsorbed),
        new StatProperty("PhysicalDamage", "Physical Damage", "Equipment Effects", false, -1f, Stats::getPhysicalDamage, Stats::setPhysicalDamage),
        new StatProperty("ArcaneDamage", "Arcane Damage", "Equipment Effects", false, -1f, Stats::getArcaneDamage, Stats::setArcaneDamage),
        new StatProperty("ShadowDamage", "Shadow Damage", "Equipment Effects", false, -1f, Stats::getShadowDamage, Stats::setShadowDamage),
        new StatProperty("FireDamage", "Fire Damage", "Equipment Effects", false, -1f, Stats::getFireDamage, Stats::setFireDamage),
        new StatProperty("FrostDamage", "Frost Damage", "Equipment Effects", false, -1f, Stats::getFrostDamage, Stats::setFrostDamage),
        new StatProperty("HolyDamage", "Holy Damage", "Equipment Effects", false, -1f, Stats::getHolyDamage, Stats::setHolyDamage),
        new StatProperty("NatureDamage", "Nature Damage", "Equipment Effects", false, -1f, Stats::getNatureDamage, Stats::setNatureDamage),
        new StatProperty("ValkyrDamage", "Valkyr Damage", "Equipment Effects", false, -1f, Stats::getValkyrDamage, Stats::setValkyrDamage),
        new StatProperty("SpellsManaReduction", "Spells Mana Cost Reduction", "Equipment Effects", false, -1f, Stats::getSpellsManaReduction, Stats::setSpellsManaReduction),
        new StatProperty("HealingOmenProc", "Next Healing Spell Mana Cost Reduction", "Equipment Effects", false, -1f, Stats::getHealingOmenProc, Stats::setHealingOmenProc),
        new StatProperty("MoteOfAnger", "% Weapon Swing", "Equipment Effects", true, -1f, Stats::getMoteOfAnger, Stats::setMoteOfAnger),
        new StatProperty("ManaRestoreOnCast_5_15", "Mana Restore On Cast (5%)", "Old Equipment Procs", false, -1f, Stats::getManaRestoreOnCast_5_15, Stats::setManaRestoreOnCast_5_15),
        new StatProperty("ManacostReduceWithin15OnHealingCast", "Mana cost of next spell reduce (within 15 sec.)", "Old Equipment Procs", false, -1f, Stats::getManacostReduceWithin15OnHealingCast, Stats::setManacostReduceWithin15OnHealingCast),
        new StatProperty("BonusTargets", "Bonus Targets", "Warrior", false, -1f, Stats::getBonusTargets, Stats::setBonusTargets),
        new StatProperty("BonusRageGen", "Bonus Rage Generated", "Warrior", false, -1f, Stats::getBonusRageGen, Stats::setBonusRageGen),
        new StatProperty("Mage4T8", "Kirin Tor Garb 4 Piece Bonus", "Mage", false, -1f, Stats::getMage4T8, Stats::setMage4T8),
        new StatProperty("Mage2T9", "Khadgar's Regalia 2 Piece Bonus", "Mage", false, -1f, Stats::getMage2T9, Stats::setMage2T9),
        new StatProperty("Mage4T9", "Khadgar's Regalia 4 Piece Bonus", "Mage", false, -1f, Stats::getMage4T9, Stats::setMage4T9),
        new StatProperty("Mage2T10", "Mage T10 2 Piece Bonus", "Mage", false, -1f, Stats::getMage2T10, Stats::setMage2T10),
        new StatProperty("Mage4T10", "Mage T10 4 Piece Bonus", "Mage", false, -1f, Stats::getMage4T10, Stats::setMage4T10),
        new StatProperty("BonusManaGem", "Mana Gem Effect", "Mage", false, -1f, Stats::getBonusManaGem, Stats::setBonusManaGem),
        new StatProperty("MageIceArmor", "Ice Armor", "Mage", false, -1f, Stats::getMageIceArmor, Stats::setMageIceArmor),
        new StatProperty("MageMageArmor", "Mage Armor", "Mage", false, -1f, Stats::getMageMageArmor, Stats::setMageMageArmor),
        new StatProperty("MageMoltenArmor", "Molten Armor", "Mage", false, -1f, Stats::getMageMoltenArmor, Stats::setMageMoltenArmor),
        new StatProperty("CatFormStrength", "Strength in Cat Form", "Feral", false, -1f, Stats::getCatFormStrength, Stats::setCatFormStrength),
        new StatProperty("SealMultiplier", "% Seal Damage", "Retribution", true, -1f, Stats::getSealMultiplier, Stats::setSealMultiplier),
        new StatProperty("JudgementMultiplier", "% Judgement Damage", "Retribution", true, -1f, Stats::getJudgementMultiplier, Stats::setJudgementMultiplier),
        new StatProperty("DivineStormRefresh", "% DS CD Reset on Melee", "Retribution", true, -1f, Stats::getDivineStormRefresh, Stats::setDivineStormRefresh),
        new StatProperty("CrusaderStrikeDamage", "Crusader Strike Damage", "Retribution", false, -1f, Stats::getCrusaderStrikeDamage, Stats::setCrusaderStrikeDamage),
        new StatProperty("ConsecrationSpellPower", "Consecration Spell Power", "Retribution", false, -1f, Stats::getConsecrationSpellPower, Stats::setConsecrationSpellPower),
        new StatProperty("DivineStormDamage", "Divine Storm Damage", "Retribution", false, -1f, Stats::getDivineStormDamage, Stats::setDivineStormDamage),
        new StatProperty("DivineStormMultiplier", "% Divine Storm Damage", "Retribution", true, -1f, Stats::getDivineStormMultiplier, Stats::setDivineStormMultiplier),
        new StatProperty("CrusaderStrikeMultiplier", "% Crusader Strike Damage", "Retribution", true, -1f, Stats::getCrusaderStrikeMultiplier, Stats::setCrusaderStrikeMultiplier),
        new StatProperty("ExorcismMultiplier", "% Exorcism Damage", "Retribution", true, -1f, Stats::getExorcismMultiplier, Stats::setExorcismMultiplier),
        new StatProperty("HammerOfWrathMultiplier", "% Hammer of Wrath Damage", "Retribution", true, -1f, Stats::getHammerOfWrathMultiplier, Stats::setHammerOfWrathMultiplier),
        new StatProperty("CrusaderStrikeCrit", "% Crusader Strike Crit", "Retribution", true, -1f, Stats::getCrusaderStrikeCrit, Stats::setCrusaderStrikeCrit),
        new StatProperty("DivineStormCrit", "% Divine Storm Crit", "Retribution", true, -1f, Stats::getDivineStormCrit, Stats::setDivineStormCrit),
        new StatProperty("JudgementCDReduction", "sec Judgement CD Reduction", "Retribution", false, -1f, Stats::getJudgementCDReduction, Stats::setJudgementCDReduction),
        new StatProperty("RighteousVengeanceCanCrit", "Righteous Vengeance Can Crit", "Retribution", false, -1f, Stats::getRighteousVengeanceCanCrit, Stats::setRighteousVengeanceCanCrit),
        new StatProperty("JudgementCrit", "% Judgement Crit", "Retribution", true, -1f, Stats::getJudgementCrit, Stats::setJudgementCrit),
        new StatProperty("FlashOfLightSpellPower", "Flash of Light Spell Power", "Healadin", false, -1f, Stats::getFlashOfLightSpellPower, Stats::setFlashOfLightSpellPower),
        new StatProperty("SacredShieldICDReduction", "secs off Sacred Shield ICD", "Healadin", false, -1f, Stats::getSacredShieldICDReduction, Stats::setSacredShieldICDReduction),
        new StatProperty("HolyShockHoTOnCrit", "% HoT on Holy Shock Crit", "Healadin", true, -1f, Stats::getHolyShockHoTOnCrit, Stats::setHolyShockHoTOnCrit),
        new StatProperty("FlashOfLightMultiplier", "% Flash of Light Healing", "Healadin", true, -1f, Stats::getFlashOfLightMultiplier, Stats::setFlashOfLightMultiplier),
        new StatProperty("FlashOfLightCrit", "% Flash of Light Crit", "Healadin", true, -1f, Stats::getFlashOfLightCrit, Stats::setFlashOfLightCrit),
        new StatProperty("HolyLightSpellPower", "Holy Light Spell Power", "Healadin", false, -1f, Stats::getHolyLightSpellPower, Stats::setHolyLightSpellPower),
        new StatProperty("HolyLightPercentManaReduction", "% Holy Light Cost Reduction", "Healadin", true, -1f, Stats::getHolyLightPercentManaReduction, Stats::setHolyLightPercentManaReduction),
        new StatProperty("HolyLightManaCostReduction", "Holy Light Mana Cost Reduction", "Healadin", false, -1f, Stats::getHolyLightManaCostReduction, Stats::setHolyLightManaCostReduction),
        new StatProperty("HolyLightCrit", "% Holy Light Crit", "Healadin", true, -1f, Stats::getHolyLightCrit, Stats::setHolyLightCrit),
        new StatProperty("HolyShockCrit", "% Holy Shock Crit", "Healadin", true, -1f, Stats::getHolyShockCrit, Stats::setHolyShockCrit),
        new StatProperty("BonusJudgementDuration", "sec Increased Judgement Duration", "Healadin", false, -1f, Stats::getBonusJudgementDuration, Stats::setBonusJudgementDuration),
        new StatProperty("FlashOfLightHoTMultiplier", "% Flash of Light HoT", "Healadin", true, -1f, Stats::getFlashOfLightHoTMultiplier, Stats::setFlashOfLightHoTMultiplier),
        new StatProperty("DivineIlluminationHealingMultiplier", "% Healing with Divine Illumination Active", "Healadin", true, -1f, Stats::getDivineIlluminationHealingMultiplier, Stats::setDivineIlluminationHealingMultiplier),
        new StatProperty("HolyLightCastTimeReductionFromHolyShock", "sec Holy Light Cast Time Reduction after Holy Shock", "Healadin", false, -1f, Stats::getHolyLightCastTimeReductionFromHolyShock, Stats::setHolyLightCastTimeReductionFromHolyShock),
        new StatProperty("Warlock4T7", "Bonus Spirit after Life Tap", "Warlock", false, -1f, Stats::getWarlock4T7, Stats::setWarlock4T7),
        new StatProperty("RangedCritRating", "Ranged Crit Rating", "Hunter", false, 0f, Stats::getRangedCritRating, Stats::setRangedCritRating),
        new StatProperty("RangedHitRating", "Ranged Hit Rating", "Hunter", false, 0f, Stats::getRangedHitRating, Stats::setRangedHitRating),
        new StatProperty("RangedHasteRating", "Ranged Haste Rating", "Hunter", false, 0f, Stats::getRangedHasteRating, Stats::setRangedHasteRating),
        new StatProperty("RangedAttackPower", "Ranged Attack Power", "Hunter", false, 0f, Stats::getRangedAttackPower, Stats::setRangedAttackPower),
        new StatProperty("BonusRangedAttackPowerMultiplier", null, "Hunter", true, 0f, Stats::getBonusRangedAttackPowerMultiplier, Stats::setBonusRangedAttackPowerMultiplier),
        new StatProperty("BonusPetAttackPowerMultiplier", null, "Hunter", true, 0f, Stats::getBonusPetAttackPowerMultiplier, Stats::setBonusPetAttackPowerMultiplier),
        new StatProperty("BonusHunter_T7_4P_ViperSpeed", "% Ranged Attack Speed Increase while Aspect of the Viper is active", "Hunter", true, -1f, Stats::getBonusHunter_T7_4P_ViperSpeed, Stats::setBonusHunter_T7_4P_ViperSpeed),
        new StatProperty("BonusHunter_T8_2P_SerpDmg", "% Damage increase for Serpent Sting", "Hunter", true, -1f, Stats::getBonusHunter_T8_2P_SerpDmg, Stats::setBonusHunter_T8_2P_SerpDmg),
        new StatProperty("BonusHunter_PvP_4pc", "second cooldown reduction for Traps", "Hunter", false, -1f, Stats::getBonusHunter_PvP_4pc, Stats::setBonusHunter_PvP_4pc),
        new StatProperty("BonusHunter_T9_2P_SerpCanCrit", "Your Serpent Stings can now crit", "Hunter", false, -1f, Stats::getBonusHunter_T9_2P_SerpCanCrit, Stats::setBonusHunter_T9_2P_SerpCanCrit),
        new StatProperty("BonusHunter_T9_4P_SteadyShotPetAPProc", "Steady shot has a chance to increase pet's attack power", "Hunter", false, -1f, Stats::getBonusHunter_T9_4P_SteadyShotPetAPProc, Stats::setBonusHunter_T9_4P_SteadyShotPetAPProc),
        new StatProperty("BonusPetCritChance", "% Extra Pet Crit Chance", "Hunter", true, -1f, Stats::getBonusPetCritChance, Stats::setBonusPetCritChance),
        new StatProperty("PetStamina", "Pet Stamina", "Hunter", false, -1f, Stats::getPetStamina, Stats::setPetStamina),
        new StatProperty("PetStrength", "Pet Strength", "Hunter", false, -1f, Stats::getPetStrength, Stats::setPetStrength),
        new StatProperty("PetAttackPower", "Pet Attack Power", "Hunter", false, -1f, Stats::getPetAttackPower, Stats::setPetAttackPower),
        new StatProperty("PetSpirit", "Pet Spirit", "Hunter", false, -1f, Stats::getPetSpirit, Stats::setPetSpirit),
        new StatProperty("BonusPetDamageMultiplier", "% Bonus Pet Damage", "Hunter", true, -1f, Stats::getBonusPetDamageMultiplier, Stats::setBonusPetDamageMultiplier),
        new StatProperty("LifebloomCostReduction", "Lifebloom cost reduction", "Tree", false, -1f, Stats::getLifebloomCostReduction, Stats::setLifebloomCostReduction),
        new StatProperty("NourishBonusPerHoT", "Nourish bonus per HoT", "Tree", false, -1f, Stats::getNourishBonusPerHoT, Stats::setNourishBonusPerHoT),
        new StatProperty("SwiftmendBonus", "Bonus Healing by Swiftmend", "Tree", false, -1f, Stats::getSwiftmendBonus, Stats::setSwiftmendBonus),
        new StatProperty("RejuvenationInstantTick", "Bonus Instant Tick by Rejuvenation", "Tree", false, -1f, Stats::getRejuvenationInstantTick, Stats::setRejuvenationInstantTick),
        new StatProperty("NourishCritBonus", "Bonus Crit Chance for Nourish", "Tree", false, -1f, Stats::getNourishCritBonus, Stats::setNourishCritBonus),
        new StatProperty("RejuvenationCrit", "Rejuvenation Crit Multiplier", "Tree", false, -1f, Stats::getRejuvenationCrit, Stats::setRejuvenationCrit),
        new StatProperty("WildGrowthLessReduction", "Wild Growth Reduction over Time Reduction", "Tree", false, -1f, Stats::getWildGrowthLessReduction, Stats::setWildGrowthLessReduction),
        new StatProperty("RejuvJumpChance", "Rejuvenation jump chance", "Tree", false, -1f, Stats::getRejuvJumpChance, Stats::setRejuvJumpChance),
        new StatProperty("RejuvenationHealBonus", "Healing by Rejuvenation", "Tree", false, -1f, Stats::getRejuvenationHealBonus, Stats::setRejuvenationHealBonus),
        new StatProperty("ReduceRejuvenationCost", "Reduced Rejuvenation Mana Cost", "Tree", false, -1f, Stats::getReduceRejuvenationCost, Stats::setReduceRejuvenationCost),
        new StatProperty("LifebloomTickHealBonus", "Healing by Lifebloom Ticks", "Tree", false, -1f, Stats::getLifebloomTickHealBonus, Stats::setLifebloomTickHealBonus),
        new StatProperty("HealingTouchFinalHealBonus", "Healing Touch Final Heal", "Tree", false, -1f, Stats::getHealingTouchFinalHealBonus, Stats::setHealingTouchFinalHealBonus),
        new StatProperty("NourishSpellpower", "Nourish spell power", "Tree", false, -1f, Stats::getNourishSpellpower, Stats::setNourishSpellpower),
        new StatProperty("SwiftmendCdReduc", "Swiftmend Cooldown Reduction", "Tree", false, -1f, Stats::getSwiftmendCdReduc, Stats::setSwiftmendCdReduc),
        new StatProperty("RevitalizeChance", "Chance to proc Revitalize", "Tree", false, -1f, Stats::getRevitalizeChance, Stats::setRevitalizeChance),
        new StatProperty("BonusSSDamage", null, "Enhance", false, -1f, Stats::getBonusSSDamage, Stats::setBonusSSDamage),
        new StatProperty("BonusWFAttackPower", null, "Enhance", false, -1f, Stats::getBonusWFAttackPower, Stats::setBonusWFAttackPower),
        new StatProperty("Enhance4T7", "Enhance T7 4 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance4T7, Stats::setEnhance4T7),
        new StatProperty("Enhance2T7", "Enhance T7 2 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance2T7, Stats::setEnhance2T7),
        new StatProperty("Enhance2T8", "Enhance T8 2 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance2T8, Stats::setEnhance2T8),
        new StatProperty("Enhance4T8", "Enhance T8 4 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance4T8, Stats::setEnhance4T8),
        new StatProperty("Enhance2T9", "Enhance T9 2 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance2T9, Stats::setEnhance2T9),
        new StatProperty("Enhance4T9", "Enhance T9 4 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance4T9, Stats::setEnhance4T9),
        new StatProperty("Enhance2T10", "Enhance T10 2 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance2T10, Stats::setEnhance2T10),
        new StatProperty("Enhance4T10", "Enhance T10 4 Piece Bonus", "Enhance", false, -1f, Stats::getEnhance4T10, Stats::setEnhance4T10),
        new StatProperty("BonusCritChance", null, "Elemental", false, -1f, Stats::getBonusCritChance, Stats::setBonusCritChance),
        new StatProperty("BonusLavaBurstCritDamage", "% bonus critical strike damage for Lava Burst", "Elemental", false, -1f, Stats::getBonusLavaBurstCritDamage, Stats::setBonusLavaBurstCritDamage),
        new StatProperty("BonusFlameShockDuration", null, "Elemental", false, -1f, Stats::getBonusFlameShockDuration, Stats::setBonusFlameShockDuration),
        new StatProperty("BonusFlameShockDoTDamage", null, "Elemental", false, -1f, Stats::getBonusFlameShockDoTDamage, Stats::setBonusFlameShockDoTDamage),
        new StatProperty("LightningBoltCostReduction", null, "Elemental", false, -1f, Stats::getLightningBoltCostReduction, Stats::setLightningBoltCostReduction),
        new StatProperty("LightningBoltDamageModifier", null, "Elemental", false, -1f, Stats::getLightningBoltDamageModifier, Stats::setLightningBoltDamageModifier),
        new StatProperty("LightningSpellPower", null, "Elemental", false, -1f, Stats::getLightningSpellPower, Stats::setLightningSpellPower),
        new StatProperty("LavaBurstBonus", "Lava Burst Bonus Base Damage", "Elemental", false, -1f, Stats::getLavaBurstBonus, Stats::setLavaBurstBonus),
        new StatProperty("LightningBoltCritDamageModifier", null, "Elemental", false, -1f, Stats::getLightningBoltCritDamageModifier, Stats::setLightningBoltCritDamageModifier),
        new StatProperty("ManaSpringMp5Increase", "Mp5 increase for Mana Spring", "RestoSham", false, -1f, Stats::getManaSpringMp5Increase, Stats::setManaSpringMp5Increase),
        new StatProperty("RTCDDecrease", "second off Riptide Cooldown", "RestoSham", false, -1f, Stats::getRTCDDecrease, Stats::setRTCDDecrease),
        new StatProperty("CHCTDecrease", "tenths of a second cast time off Chain Heal", "RestoSham", false, -1f, Stats::getCHCTDecrease, Stats::setCHCTDecrease),
        new StatProperty("Earthliving", "Instances of Earthliving Weapon active", "RestoSham", false, -1f, Stats::getEarthliving, Stats::setEarthliving),
        new StatProperty("TotemCHBaseHeal", "Base Healing to CH", "RestoSham", false, -1f, Stats::getTotemCHBaseHeal, Stats::setTotemCHBaseHeal),
        new StatProperty("TotemHWBaseCost", "HW Cost Reduction:", "RestoSham", false, -1f, Stats::getTotemHWBaseCost, Stats::setTotemHWBaseCost),
        new StatProperty("TotemCHBaseCost", "CH Cost Reduction:", "RestoSham", false, -1f, Stats::getTotemCHBaseCost, Stats::setTotemCHBaseCost),
        new StatProperty("TotemHWSpellpower", "HW Spellpower added:", "RestoSham", false, -1f, Stats::getTotemHWSpellpower, Stats::setTotemHWSpellpower),
        new StatProperty("TotemLHWSpellpower", "LHW Spellpower added:", "RestoSham", false, -1f, Stats::getTotemLHWSpellpower, Stats::setTotemLHWSpellpower),
        new StatProperty("TotemThunderhead", "Thunderhead Effect", "RestoSham", false, -1f, Stats::getTotemThunderhead, Stats::setTotemThunderhead),
        new StatProperty("JudgementBlockValue", "Judgement Block Value", "ProtPaladin", false, -1f, Stats::getJudgementBlockValue, Stats::setJudgementBlockValue),
        new StatProperty("ShieldOfRighteousnessBlockValue", "ShoR Block Value", "ProtPaladin", false, -1f, Stats::getShieldOfRighteousnessBlockValue, Stats::setShieldOfRighteousnessBlockValue),
        new StatProperty("StarfireDmg", "Starfire damage bonus", "Moonkin", false, -1f, Stats::getStarfireDmg, Stats::setStarfireDmg),
        new StatProperty("UnseenMoonDamageBonus", "Spell Damage (10 sec on Moonfire)", "Moonkin", false, -1f, Stats::getUnseenMoonDamageBonus, Stats::setUnseenMoonDamageBonus),
        new StatProperty("InsectSwarmDmg", "Insect Swarm damage bonus", "Moonkin", false, -1f, Stats::getInsectSwarmDmg, Stats::setInsectSwarmDmg),
        new StatProperty("MoonfireDmg", "Moonfire damage bonus", "Moonkin", false, -1f, Stats::getMoonfireDmg, Stats::setMoonfireDmg),
        new StatProperty("WrathDmg", "Wrath damage bonus", "Moonkin", false, -1f, Stats::getWrathDmg, Stats::setWrathDmg),
        new StatProperty("InnervateCooldownReduction", "Innervate CD Reduction", "Moonkin", false, -1f, Stats::getInnervateCooldownReduction, Stats::setInnervateCooldownReduction),
        new StatProperty("StarfireBonusWithDot", "% Starfire Crit Bonus", "Moonkin", true, -1f, Stats::getStarfireBonusWithDot, Stats::setStarfireBonusWithDot),
        new StatProperty("MoonfireExtension", "Moonfire Extension", "Moonkin", false, -1f, Stats::getMoonfireExtension, Stats::setMoonfireExtension),
        new StatProperty("StarfireCritChance", "% Starfire Crit", "Moonkin", true, -1f, Stats::getStarfireCritChance, Stats::setStarfireCritChance),
        new StatProperty("BonusInsectSwarmDamage", "% Insect Swarm Damage", "Moonkin", true, -1f, Stats::getBonusInsectSwarmDamage, Stats::setBonusInsectSwarmDamage),
        new StatProperty("BonusNukeCritChance", "% SF/W Crit", "Moonkin", true, -1f, Stats::getBonusNukeCritChance, Stats::setBonusNukeCritChance),
        new StatProperty("EclipseBonus", "% Eclipse Bonus", "Moonkin", true, -1f, Stats::getEclipseBonus, Stats::setEclipseBonus),
        new StatProperty("StarfireProc", "Starfire Proc", "Moonkin", false, -1f, Stats::getStarfireProc, Stats::setStarfireProc),
        new StatProperty("MoonfireDotCrit", "Moonfire Dot Crit", "Moonkin", false, -1f, Stats::getMoonfireDotCrit, Stats::setMoonfireDotCrit),
        new StatProperty("BonusMoonkinNukeDamage", "% SF/W Damage", "Moonkin", true, -1f, Stats::getBonusMoonkinNukeDamage, Stats::setBonusMoonkinNukeDamage),
        new StatProperty("MoonkinT10CritDot", "% Crit Damage As Dot", "Moonkin", true, -1f, Stats::getMoonkinT10CritDot, Stats::setMoonkinT10CritDot),
        new StatProperty("ThreatIncreaseMultiplier", "% Threat Increase", "Buffs / Debuffs", true, -1f, Stats::getThreatIncreaseMultiplier, Stats::setThreatIncreaseMultiplier),
        new StatProperty("BossAttackSpeedMultiplier", "% Boss Attack Speed Reduction", "Buffs / Debuffs", true, -1f, Stats::getBossAttackSpeedMultiplier, Stats::setBossAttackSpeedMultiplier),
        new StatProperty("BonusManaMultiplier", "% Mana", "Equipment Effects", true, -1f, Stats::getBonusManaMultiplier, Stats::setBonusManaMultiplier),
        new StatProperty("BonusCritHealMultiplier", "% Crit Heal", "Equipment Effects", true, -1f, Stats::getBonusCritHealMultiplier, Stats::setBonusCritHealMultiplier),
        new StatProperty("BonusBleedDamageMultiplier", "% Bonus Bleed Damage Multiplier", "Buffs / Debuffs", true, -1f, Stats::getBonusBleedDamageMultiplier, Stats::setBonusBleedDamageMultiplier),
        new StatProperty("BonusAgilityMultiplier", "% Agility", "Buffs / Debuffs", true, -1f, Stats::getBonusAgilityMultiplier, Stats::setBonusAgilityMultiplier),
        new StatProperty("BonusStrengthMultiplier", "% Strength", "Buffs / Debuffs", true, -1f, Stats::getBonusStrengthMultiplier, Stats::setBonusStrengthMultiplier),
        new StatProperty("BonusStaminaMultiplier", "% Stamina", "Buffs / Debuffs", true, -1f, Stats::getBonusStaminaMultiplier, Stats::setBonusStaminaMultiplier),
        new StatProperty("BonusHealthMultiplier", "% Health", "Buffs / Debuffs", true, -1f, Stats::getBonusHealthMultiplier, Stats::setBonusHealthMultiplier),
        new StatProperty("BonusIntellectMultiplier", "% Int", "Buffs / Debuffs", true, -1f, Stats::getBonusIntellectMultiplier, Stats::setBonusIntellectMultiplier),
        new StatProperty("BonusArmorMultiplier", "% Armor", "Buffs / Debuffs", true, -1f, Stats::getBonusArmorMultiplier, Stats::setBonusArmorMultiplier),
        new StatProperty("BaseArmorMultiplier", "% Base Armor", "Buffs / Debuffs", true, -1f, Stats::getBaseArmorMultiplier, Stats::setBaseArmorMultiplier),
        new StatProperty("BonusBlockValueMultiplier", "% Block Value", "Buffs / Debuffs", true, -1f, Stats::getBonusBlockValueMultiplier, Stats::setBonusBlockValueMultiplier),
        new StatProperty("BonusAttackPowerMultiplier", "% AP", "Buffs / Debuffs", true, -1f, Stats::getBonusAttackPowerMultiplier, Stats::setBonusAttackPowerMultiplier),
        new StatProperty("BonusSpellPowerMultiplier", "% SP", "Buffs / Debuffs", true, -1f, Stats::getBonusSpellPowerMultiplier, Stats::setBonusSpellPowerMultiplier),
        new StatProperty("Warlock2T7", "% increase to the critical strike chance of your next Shadowbolt or Incinerate spell", "Warlock", true, -1f, Stats::getWarlock2T7, Stats::setWarlock2T7),
        new StatProperty("Warlock2T8", "% damage bonus on Unstable Affliction and 10% on Immolate", "Warlock", true, -1f, Stats::getWarlock2T8, Stats::setWarlock2T8),
        new StatProperty("Warlock4T8", "% crit chance on Shadowbolt and Incinerate", "Warlock", true, -1f, Stats::getWarlock4T8, Stats::setWarlock4T8),
        new StatProperty("Warlock2T9", "% increase to your pet's critical strike chance with its abilities", "Warlock", true, -1f, Stats::getWarlock2T9, Stats::setWarlock2T9),
        new StatProperty("Warlock4T9", "% increase to damage done by your Immolate, Corruption and Unstable Affliction spells", "Warlock", true, -1f, Stats::getWarlock4T9, Stats::setWarlock4T9),
        new StatProperty("Warlock2T10", "% increased critical strike chance to your Shadowbolt, Incinerate, Soulfire & Corruption spells", "Warlock", true, -1f, Stats::getWarlock2T10, Stats::setWarlock2T10),
        new StatProperty("Warlock4T10", "% chance for you & your pet to gain 10% increased damage for 10 seconds\\r\\neach time your Immolate and Unstable Affliction spells deals periodic damage.", "Warlock", true, -1f, Stats::getWarlock4T10, Stats::setWarlock4T10),
        new StatProperty("BonusSpellPowerDemonicPactMultiplier", "% Warlock Spell Power", "Warlock", true, -1f, Stats::getBonusSpellPowerDemonicPactMultiplier, Stats::setBonusSpellPowerDemonicPactMultiplier),
        new StatProperty("BonusRageOnCrit", "Bonus Rage on Crit", "Warrior", false, -1f, Stats::getBonusRageOnCrit, Stats::setBonusRageOnCrit),
        new StatProperty("BonusShieldSlamDamage", "% Shield Slam Damage", "Warrior", true, -1f, Stats::getBonusShieldSlamDamage, Stats::setBonusShieldSlamDamage),
        new StatProperty("DevastateCritIncrease", "% increased critical strike chance on Devastate", "Warrior", true, -1f, Stats::getDevastateCritIncrease, Stats::setDevastateCritIncrease),
        new StatProperty("BonusDevastateDamage", "% Devastate Damage", "Warrior", true, -1f, Stats::getBonusDevastateDamage, Stats::setBonusDevastateDamage),
        new StatProperty("BonusShockwaveDamage", "% Shockwave Damage", "Warrior", true, -1f, Stats::getBonusShockwaveDamage, Stats::setBonusShockwaveDamage),
        new StatProperty("BonusWarrior_T7_2P_SlamDamage", "% Slam Damage", "Warrior", true, -1f, Stats::getBonusWarrior_T7_2P_SlamDamage, Stats::setBonusWarrior_T7_2P_SlamDamage),
        new StatProperty("BonusWarrior_T7_4P_RageProc", "Rage Cost Reduction Proc", "Warrior", false, -1f, Stats::getBonusWarrior_T7_4P_RageProc, Stats::setBonusWarrior_T7_4P_RageProc),
        new StatProperty("BonusWarrior_T8_2P_HasteProc", "bonus haste rating chance on Heroic Strike and Slam crits", "Warrior", false, -1f, Stats::getBonusWarrior_T8_2P_HasteProc, Stats::setBonusWarrior_T8_2P_HasteProc),
        new StatProperty("BonusWarrior_T8_4P_MSBTCritIncrease", "% increased critical strike chance on Mortal Strike and Bloodthirst", "Warrior", true, -1f, Stats::getBonusWarrior_T8_4P_MSBTCritIncrease, Stats::setBonusWarrior_T8_4P_MSBTCritIncrease),
        new StatProperty("BonusWarrior_T9_2P_Crit", "% increased Berserker Stance Crit Bonus", "Warrior", true, -1f, Stats::getBonusWarrior_T9_2P_Crit, Stats::setBonusWarrior_T9_2P_Crit),
        new StatProperty("BonusWarrior_T9_2P_ArP", "% increased Battle Stance ArP Bonus", "Warrior", true, -1f, Stats::getBonusWarrior_T9_2P_ArP, Stats::setBonusWarrior_T9_2P_ArP),
        new StatProperty("BonusWarrior_T9_4P_SLHSCritIncrease", "% increased critical strike chance on Slam and Heroic Strike", "Warrior", true, -1f, Stats::getBonusWarrior_T9_4P_SLHSCritIncrease, Stats::setBonusWarrior_T9_4P_SLHSCritIncrease),
        new StatProperty("BonusWarrior_T10_2P_DWAPProc", " 3% chance on Deep Wounds damage to grant 16% Bonus Attack Power for 10 sec.", "Warrior", false, -1f, Stats::getBonusWarrior_T10_2P_DWAPProc, Stats::setBonusWarrior_T10_2P_DWAPProc),
        new StatProperty("BonusWarrior_T10_4P_BSSDProcChange", "extra Sudden Death or Blood Surge charge per proc and 0.5 GCD reduction on Slams/Executes and double effect SD/BS durations", "Warrior", false, -1f, Stats::getBonusWarrior_T10_4P_BSSDProcChange, Stats::setBonusWarrior_T10_4P_BSSDProcChange),
        new StatProperty("BonusWarrior_PvP_4P_InterceptCDReduc", "second Intercept Cooldown Reduction", "Warrior", false, -1f, Stats::getBonusWarrior_PvP_4P_InterceptCDReduc, Stats::setBonusWarrior_PvP_4P_InterceptCDReduc),
        new StatProperty("BonusMangleBearThreat", "% Mangle (Bear) Threat", "Feral", true, -1f, Stats::getBonusMangleBearThreat, Stats::setBonusMangleBearThreat),
        new StatProperty("BonusLacerateDamageMultiplier", "% Lacerate Dmg", "Feral", true, -1f, Stats::getBonusLacerateDamageMultiplier, Stats::setBonusLacerateDamageMultiplier),
        new StatProperty("BonusRipDamageMultiplier", "% Rip Dmg", "Feral", true, -1f, Stats::getBonusRipDamageMultiplier, Stats::setBonusRipDamageMultiplier),
        new StatProperty("BonusMangleDamageMultiplier", "% Mangle Dmg", "Feral", true, -1f, Stats::getBonusMangleDamageMultiplier, Stats::setBonusMangleDamageMultiplier),
        new StatProperty("BonusRakeDamageMultiplier", "% Rake Dmg", "Feral", true, -1f, Stats::getBonusRakeDamageMultiplier, Stats::setBonusRakeDamageMultiplier),
        new StatProperty("BonusShredDamageMultiplier", "% Shred Dmg", "Feral", true, -1f, Stats::getBonusShredDamageMultiplier, Stats::setBonusShredDamageMultiplier),
        new StatProperty("BonusFerociousBiteDamageMultiplier", "% Ferocious Bite Dmg", "Feral", true, -1f, Stats::getBonusFerociousBiteDamageMultiplier, Stats::setBonusFerociousBiteDamageMultiplier),
        new StatProperty("BonusRipCrit", "Bonus Rip Crit Chance", "Feral", false, -1f, Stats::getBonusRipCrit, Stats::setBonusRipCrit),
        new StatProperty("BonusBearSwipeDamageMultiplier", "% Swipe (Bear) Dmg", "Feral", true, -1f, Stats::getBonusBearSwipeDamageMultiplier, Stats::setBonusBearSwipeDamageMultiplier),
        new StatProperty("BonusMaulDamageMultiplier", "% Maul Dmg", "Feral", true, -1f, Stats::getBonusMaulDamageMultiplier, Stats::setBonusMaulDamageMultiplier),
        new StatProperty("BonusEnrageDamageMultiplier", "% Enrage Dmg", "Feral", true, -1f, Stats::getBonusEnrageDamageMultiplier, Stats::setBonusEnrageDamageMultiplier),
        new StatProperty("MangleCooldownReduction", "Mangle Cooldown Reduction", "Feral", false, -1f, Stats::getMangleCooldownReduction, Stats::setMangleCooldownReduction),
        new StatProperty("BonusSwipeDamageMultiplier", "% Swipe Dmg", "Feral", true, -1f, Stats::getBonusSwipeDamageMultiplier, Stats::setBonusSwipeDamageMultiplier),
        new StatProperty("BonusRakeCrit", "Bonus Rake Bleed Can Crit", "Feral", false, -1f, Stats::getBonusRakeCrit, Stats::setBonusRakeCrit),
        new StatProperty("BonusFerociousBiteCrit", "Bonus Ferocious Bite Crit Chance", "Feral", false, -1f, Stats::getBonusFerociousBiteCrit, Stats::setBonusFerociousBiteCrit),
        new StatProperty("TigersFuryCooldownReduction", "Tiger's Fury Cooldown Reduction", "Feral", false, -1f, Stats::getTigersFuryCooldownReduction, Stats::setTigersFuryCooldownReduction),
        new StatProperty("PriestInnerFire", "Inner Fire (Priest)", "Priest", false, -1f, Stats::getPriestInnerFire, Stats::setPriestInnerFire),
        new StatProperty("SWPDurationIncrease", "seconds increased duration of Shadow Word: Pain (Priest)", "Priest", false, -1f, Stats::getSWPDurationIncrease, Stats::setSWPDurationIncrease),
        new StatProperty("BonusMindBlastMultiplier", "% increased damage from Mind Blast (Priest)", "Priest", true, -1f, Stats::getBonusMindBlastMultiplier, Stats::setBonusMindBlastMultiplier),
        new StatProperty("MindBlastCostReduction", "% reduced cost on Mind Blast (Priest)", "Priest", true, -1f, Stats::getMindBlastCostReduction, Stats::setMindBlastCostReduction),
        new StatProperty("ShadowWordDeathCritIncrease", "% increased critical hit chance on Shadow Word: Death (Priest)", "Priest", true, -1f, Stats::getShadowWordDeathCritIncrease, Stats::setShadowWordDeathCritIncrease),
        new StatProperty("DevouringPlagueBonusDamage", "% increased damage on Devouring Plague. (Priest)", "Priest", true, -1f, Stats::getDevouringPlagueBonusDamage, Stats::setDevouringPlagueBonusDamage),
        new StatProperty("MindBlastHasteProc", "Extra haste rating for 4 seconds after casting Mind Blast. (Priest)", "Priest", false, -1f, Stats::getMindBlastHasteProc, Stats::setMindBlastHasteProc),
        new StatProperty("PriestDPS_T9_2pc", " seconds increased duration of Vampiric Touch. (Priest)", "Priest", false, -1f, Stats::getPriestDPS_T9_2pc, Stats::setPriestDPS_T9_2pc),
        new StatProperty("PriestDPS_T9_4pc", "% increased critical hit chance on Mind Flay. (Priest)", "Priest", true, -1f, Stats::getPriestDPS_T9_4pc, Stats::setPriestDPS_T9_4pc),
        new StatProperty("PriestDPS_T10_2pc", "% increased critical hit chance on Shadow Word: Pain, Vampiric Touch and Devouring Plague. (Priest)", "Priest", true, -1f, Stats::getPriestDPS_T10_2pc, Stats::setPriestDPS_T10_2pc),
        new StatProperty("PriestDPS_T10_4pc", " seconds reduced cast time on Mind Flay. (Priest)", "Priest", false, -1f, Stats::getPriestDPS_T10_4pc, Stats::setPriestDPS_T10_4pc),
        new StatProperty("BonusPoHManaCostReductionMultiplier", "% reduce the mana cost of PoH (Priest)", "Priest", true, -1f, Stats::getBonusPoHManaCostReductionMultiplier, Stats::setBonusPoHManaCostReductionMultiplier),
        new StatProperty("BonusGHHealingMultiplier", "% increased healing from Greater Heal (Priest)", "Priest", true, -1f, Stats::getBonusGHHealingMultiplier, Stats::setBonusGHHealingMultiplier),
        new StatProperty("RenewDurationIncrease", "seconds increased duration of Renew (Priest)", "Priest", false, -1f, Stats::getRenewDurationIncrease, Stats::setRenewDurationIncrease),
        new StatProperty("ManaGainOnGreaterHealOverheal", "Mana gained on Greater Heal Overheals (Priest)", "Priest", false, -1f, Stats::getManaGainOnGreaterHealOverheal, Stats::setManaGainOnGreaterHealOverheal),
        new StatProperty("PrayerOfMendingExtraJumps", " extra jumps on Prayer of Mending (Priest)", "Priest", false, -1f, Stats::getPrayerOfMendingExtraJumps, Stats::setPrayerOfMendingExtraJumps),
        new StatProperty("GreaterHealCostReduction", "% reduced cost of Greater Heal (Priest)", "Priest", true, -1f, Stats::getGreaterHealCostReduction, Stats::setGreaterHealCostReduction),
        new StatProperty("WeakenedSoulDurationDecrease", "seconds reduced duration of Weakened Soul (Priest)", "Priest", false, -1f, Stats::getWeakenedSoulDurationDecrease, Stats::setWeakenedSoulDurationDecrease),
        new StatProperty("PrayerOfHealingExtraCrit", "% increased critical chance on Prayer of Healing. (Priest)", "Priest", true, -1f, Stats::getPrayerOfHealingExtraCrit, Stats::setPrayerOfHealingExtraCrit),
        new StatProperty("PWSBonusSpellPowerProc", "Bonus spell power for 5 seconds after casting Power Word: Shield. (Priest)", "Priest", false, -1f, Stats::getPWSBonusSpellPowerProc, Stats::setPWSBonusSpellPowerProc),
        new StatProperty("PriestHeal_T9_2pc", "% increased healing on Prayer of Mending. (Priest)", "Priest", true, -1f, Stats::getPriestHeal_T9_2pc, Stats::setPriestHeal_T9_2pc),
        new StatProperty("PriestHeal_T9_4pc", "% increased effect on Divine Aegis and Empowered Renew. (Priest)", "Priest", true, -1f, Stats::getPriestHeal_T9_4pc, Stats::setPriestHeal_T9_4pc),
        new StatProperty("PriestHeal_T10_2pc", "% chance of causing to heal for 33% of healed amount over 9 sec. (Priest)", "Priest", true, -1f, Stats::getPriestHeal_T10_2pc, Stats::setPriestHeal_T10_2pc),
        new StatProperty("PriestHeal_T10_4pc", "% increased effect from Renew and Power Word: Shield. (Priest)", "Priest", true, -1f, Stats::getPriestHeal_T10_4pc, Stats::setPriestHeal_T10_4pc),
        new StatProperty("BonusLavaBurstDamageMultiplier", "% bonus damage for Lava Burst", "Elemental", true, -1f, Stats::getBonusLavaBurstDamageMultiplier, Stats::setBonusLavaBurstDamageMultiplier),
        new StatProperty("Elemental2T10", "Your Lightning Bolt and Chain Lightning spells reduce the remaining cooldown on your Elemental Mastery talent by 2 sec.", "Elemental", false, -1f, Stats::getElemental2T10, Stats::setElemental2T10),
        new StatProperty("Elemental4T10", "Your Lava Burst spell causes your Flame Shock effect on the target to deal at least two additional periodic damage ticks before expiring.", "Elemental", false, -1f, Stats::getElemental4T10, Stats::setElemental4T10),
        new StatProperty("CHHWHealIncrease", "Increases the healing done by your Chain Heal and Healing Wave by 5%", "RestoSham", true, -1f, Stats::getCHHWHealIncrease, Stats::setCHHWHealIncrease),
        new StatProperty("WaterShieldIncrease", "Your Water Shield is 10% stronger", "RestoSham", true, -1f, Stats::getWaterShieldIncrease, Stats::setWaterShieldIncrease),
        new StatProperty("LHWManaReduction", "Reduce Lesser Healing Wave mana cost by 5%", "RestoSham", false, -1f, Stats::getLHWManaReduction, Stats::setLHWManaReduction),
        new StatProperty("CHManaReduction", "Reduce Chain Heal mana cost by 10%", "RestoSham", false, -1f, Stats::getCHManaReduction, Stats::setCHManaReduction),
        new StatProperty("CHHealIncrease", "Increase healing done by Chain Heal by 5%", "RestoSham", false, -1f, Stats::getCHHealIncrease, Stats::setCHHealIncrease),
        new StatProperty("RestoSham2T9", "Increases the healing done by your Riptide spell by 20%.", "RestoSham", false, -1f, Stats::getRestoSham2T9, Stats::setRestoSham2T9),
        new StatProperty("RestoSham4T9", "Increases the critical strike chance of your Chain Heal spell by 5%.", "RestoSham", false, -1f, Stats::getRestoSham4T9, Stats::setRestoSham4T9),
        new StatProperty("RestoSham2T10", "Your Riptide spell grants 20% spell haste for your next spellcast within 10 sec.", "RestoSham", false, -1f, Stats::getRestoSham2T10, Stats::setRestoSham2T10),
        new StatProperty("RestoSham4T10", "Your Chain Heal critical strikes cause the target to heal for 25% of the healed amount over 9 sec.", "RestoSham", false, -1f, Stats::getRestoSham4T10, Stats::setRestoSham4T10),
        new StatProperty("RestoShamRelicT9", "Each time you cast Chain Heal, you have a chance to gain 234 spell power for 15 sec.", "RestoSham", false, -1f, Stats::getRestoShamRelicT9, Stats::setRestoShamRelicT9),
        new StatProperty("RestoShamRelicT10", "Your Riptide spell grants 85 spell power for 15 sec.  Stacks up to 3 times.", "RestoSham", false, -1f, Stats::getRestoShamRelicT10, Stats::setRestoShamRelicT10),
        new StatProperty("BonusHammerOfTheRighteousMultiplier", "% Hammer of the Righteous Damage", "ProtPaladin", true, -1f, Stats::getBonusHammerOfTheRighteousMultiplier, Stats::setBonusHammerOfTheRighteousMultiplier),
        new StatProperty("BonusSealOfCorruptionDamageMultiplier", "% Seal of Corruption Damage", "ProtPaladin", true, -1f, Stats::getBonusSealOfCorruptionDamageMultiplier, Stats::setBonusSealOfCorruptionDamageMultiplier),
        new StatProperty("BonusSealOfRighteousnessDamageMultiplier", "% Seal of Righteousness Damage", "ProtPaladin", true, -1f, Stats::getBonusSealOfRighteousnessDamageMultiplier, Stats::setBonusSealOfRighteousnessDamageMultiplier),
        new StatProperty("BonusSealOfVengeanceDamageMultiplier", "% Seal of Vengeance Damage", "ProtPaladin", true, -1f, Stats::getBonusSealOfVengeanceDamageMultiplier, Stats::setBonusSealOfVengeanceDamageMultiplier),
        new StatProperty("ThreatReductionMultiplier", "% Threat Reduction", "Buffs / Debuffs", true, -1f, Stats::getThreatReductionMultiplier, Stats::setThreatReductionMultiplier),
        new StatProperty("DamageTakenMultiplier", "% Damage Taken", "Buffs / Debuffs", true, -1f, Stats::getDamageTakenMultiplier, Stats::setDamageTakenMultiplier),
        new StatProperty("SpellDamageTakenMultiplier", "% Spell Damage Taken", "Buffs / Debuffs", true, -1f, Stats::getSpellDamageTakenMultiplier, Stats::setSpellDamageTakenMultiplier),
        new StatProperty("PhysicalDamageTakenMultiplier", "% Physical Damage Taken", "Buffs / Debuffs", true, -1f, Stats::getPhysicalDamageTakenMultiplier, Stats::setPhysicalDamageTakenMultiplier),
        new StatProperty("StunDurReduc", "% Stun Duration Reduction", "Buffs / Debuffs", true, -1f, Stats::getStunDurReduc, Stats::setStunDurReduc),
        new StatProperty("SnareRootDurReduc", "% Snare/Root Duration Reduction", "Buffs / Debuffs", true, -1f, Stats::getSnareRootDurReduc, Stats::setSnareRootDurReduc),
        new StatProperty("FearDurReduc", "% Fear Duration Reduction", "Buffs / Debuffs", true, -1f, Stats::getFearDurReduc, Stats::setFearDurReduc),
        new StatProperty("BonusManaPotion", "% Increased Mana Potion", "Equipment Effects", true, -1f, Stats::getBonusManaPotion, Stats::setBonusManaPotion),
        new StatProperty("FrostResistanceBuff", "Frost Res", "Resistances", false, -1f, Stats::getFrostResistanceBuff, Stats::setFrostResistanceBuff),
        new StatProperty("NatureResistanceBuff", "Nature Res", "Resistances", false, -1f, Stats::getNatureResistanceBuff, Stats::setNatureResistanceBuff),
        new StatProperty("FireResistanceBuff", "Fire Res", "Resistances", false, -1f, Stats::getFireResistanceBuff, Stats::setFireResistanceBuff),
        new StatProperty("ShadowResistanceBuff", "Shadow Res", "Resistances", false, -1f, Stats::getShadowResistanceBuff, Stats::setShadowResistanceBuff),
        new StatProperty("ArcaneResistanceBuff", "Arcane Res", "Resistances", false, -1f, Stats::getArcaneResistanceBuff, Stats::setArcaneResistanceBuff),
        new StatProperty("MovementSpeed", "% Increased Movement Speed", "Misc", true, -1f, Stats::getMovementSpeed, Stats::setMovementSpeed),
        new StatProperty("SpellReflectChance", "% Chance to Reflect Spell", "Equipment Effects", true, -1f, Stats::getSpellReflectChance, Stats::setSpellReflectChance),
        new StatProperty("BonusShieldOfRighteousnessDamage", "Bonus ShoR Damage", "ProtPaladin", false, -1f, Stats::getBonusShieldOfRighteousnessDamage, Stats::setBonusShieldOfRighteousnessDamage),
        new StatProperty("DivineProtectionDurationBonus", "Divine Protection Duration Bonus", "ProtPaladin", false, -1f, Stats::getDivineProtectionDurationBonus, Stats::setDivineProtectionDurationBonus),
        new StatProperty("HolyShieldBlockValue", "Holy Shield Block Value", "ProtPaladin", false, -1f, Stats::getHolyShieldBlockValue, Stats::setHolyShieldBlockValue),
    };

    public static final java.util.Map<String, StatProperty> PROPERTY_BY_NAME = buildPropertyMap();

    /** Builds the name to metadata lookup for {@link #PROPERTY_BY_NAME}. */
    private static java.util.Map<String, StatProperty> buildPropertyMap() {
        java.util.Map<String, StatProperty> map = new java.util.HashMap<>();
        for (StatProperty p : PROPERTY_INFO) map.put(p.name(), p);
        return map;
    }

    /**
     * Returns the programmatic stat name to value map of the entries accepted by {@code filter}
     * (sorted by name), iterating {@link #PROPERTY_INFO}; used for display and relevance checks.
     */
    public SortedMap<String, Float> values(StatFilter filter) {
        TreeMap<String, Float> dict = new TreeMap<>();
        for (StatProperty info : PROPERTY_INFO) {
            float value = info.accessor().get(this);
            if (filter.test(value)) {
                dict.put(info.name(), value);
            }
        }
        return dict;
    }

    /**
     * Resets the object: all five arrays (including the {@link SingleSourceStat} values, unlike the
     * C# subclass fields) and the special-effect list.
     */
    public void clear() {
        java.util.Arrays.fill(rawAdditiveData, 0f);
        java.util.Arrays.fill(rawMultiplicativeData, 0f);
        java.util.Arrays.fill(rawInverseMultiplicativeData, 0f);
        java.util.Arrays.fill(rawNoStackData, 0f);
        java.util.Arrays.fill(rawSingleSourceData, 0f);
        rawSpecialEffectDataSize = 0;
    }

    /** Invalidates the cached non-zero index; every value-changing setter must call this. */
    public void invalidateSparseData() {
        sparseIndices = null;
    }

    /**
     * Returns the live backing array of additive stats, indexed by {@link AdditiveStat#value()},
     * for hot-path readers (the proc-combination math in {@code SpecialEffect}).
     *
     * <p>Callers must respect the sparse-cache invariant: any write that changes a value needs a
     * later {@link #invalidateSparseData()}, as {@code Stats} itself does. Do not keep a reference
     * beyond the operation that needs it.</p>
     */
    public float[] getRawAdditiveData() {
        return rawAdditiveData;
    }

    /** Live multiplicative-value array; see {@link #getRawAdditiveData()} for the contract. */
    public float[] getRawMultiplicativeData() {
        return rawMultiplicativeData;
    }

    /**
     * Builds the cached list of non-zero slots of the four combining arrays (see the class doc's
     * "Sparse-data optimization"). Idempotent, synchronized on the additive array, and invalidated
     * by every value-changing setter.
     */
    public void generateSparseData() {
        if (sparseIndices != null) return;
        synchronized (rawAdditiveData) {
            if (sparseIndices == null) {
                java.util.List<Integer> indices = new java.util.ArrayList<Integer>();
                sparseAdditiveCount = 0;
                for (int i = 0; i < rawAdditiveData.length; i++) {
                    if (rawAdditiveData[i] != 0.0f) {
                        sparseAdditiveCount++;
                        indices.add(i);
                    }
                }
                sparseMultiplicativeCount = 0;
                for (int i = 0; i < rawMultiplicativeData.length; i++) {
                    if (rawMultiplicativeData[i] != 0.0f) {
                        sparseMultiplicativeCount++;
                        indices.add(i);
                    }
                }
                sparseInverseMultiplicativeCount = 0;
                for (int i = 0; i < rawInverseMultiplicativeData.length; i++) {
                    if (rawInverseMultiplicativeData[i] != 0.0f) {
                        sparseInverseMultiplicativeCount++;
                        indices.add(i);
                    }
                }
                sparseNoStackCount = 0;
                for (int i = 0; i < rawNoStackData.length; i++) {
                    if (rawNoStackData[i] != 0.0f) {
                        sparseNoStackCount++;
                        indices.add(i);
                    }
                }
                sparseIndices = new int[indices.size()];
                for (int i = 0; i < indices.size(); i++) {
                    sparseIndices[i] = indices.get(i);
                }
            }
        }
    }

    /** Grows {@code rawSpecialEffectData} to hold at least {@code min} entries (C# policy: at
     *  least {@code DEFAULT_SPECIAL_EFFECT_DATA_CAPACITY}, otherwise double; copies only live entries). */
    private void ensureSpecialEffectCapacity(int min) {
        if (rawSpecialEffectData != null && rawSpecialEffectData.length >= min) return;
        int capacity = (rawSpecialEffectData == null || rawSpecialEffectData.length == 0)
                ? DEFAULT_SPECIAL_EFFECT_DATA_CAPACITY : (rawSpecialEffectData.length * 2);
        if (capacity < min) capacity = min;
        SpecialEffect[] grown = new SpecialEffect[capacity];
        if (rawSpecialEffectDataSize > 0) {
            System.arraycopy(rawSpecialEffectData, 0, grown, 0, rawSpecialEffectDataSize);
        }
        rawSpecialEffectData = grown;
    }

    /**
     * C#'s {@code operator +}: combines the four combining categories and appends special effects.
     * {@link SingleSourceStat} values are not copied (the C# operator builds a fresh base
     * {@code Stats}, which loses the {@code StatsCat}/{@code StatsBear} subclass fields); use
     * {@link #copy()} when an exact copy is needed.
     */
    public static Stats add(Stats a, Stats b) {
        Stats c = new Stats();

        int i = c.rawAdditiveData.length;
        while (--i >= 0) {
            c.rawAdditiveData[i] = a.rawAdditiveData[i] + b.rawAdditiveData[i];
        }        i = c.rawMultiplicativeData.length;
        while (--i >= 0) {
            c.rawMultiplicativeData[i] = (1 + a.rawMultiplicativeData[i]) * (1 + b.rawMultiplicativeData[i]) - 1;
        }
        i = c.rawInverseMultiplicativeData.length;
        while (--i >= 0) {
            c.rawInverseMultiplicativeData[i] = 1 - (1 - a.rawInverseMultiplicativeData[i]) * (1 - b.rawInverseMultiplicativeData[i]);
        }

        i = c.rawNoStackData.length;
        while (--i >= 0) {
            c.rawNoStackData[i] = Math.max(a.rawNoStackData[i], b.rawNoStackData[i]);
        }
        int specialEffectCount = a.rawSpecialEffectDataSize + b.rawSpecialEffectDataSize;
        if (specialEffectCount > 0) {
            c.rawSpecialEffectData = new SpecialEffect[specialEffectCount];
            c.rawSpecialEffectDataSize = specialEffectCount;
            if (a.rawSpecialEffectDataSize > 0) {
                System.arraycopy(a.rawSpecialEffectData, 0, c.rawSpecialEffectData, 0, a.rawSpecialEffectDataSize);
            }
            if (b.rawSpecialEffectDataSize > 0) {
                System.arraycopy(b.rawSpecialEffectData, 0, c.rawSpecialEffectData, a.rawSpecialEffectDataSize, b.rawSpecialEffectDataSize);
            }
        }
        return c;
    }

    /**
     * C# Stats.cs:5388-5402 `Equals`/`operator ==` semantics: reference equality, or (when neither
     * side carries special effects) exact equality of the additive, multiplicative and no-stack
     * arrays. `Stats.Equals` in C# binds to the overloaded operator, so ported models that compare
     * effects must use this instead of Java reference equality. {@link SingleSourceStat} values are
     * excluded, matching the C# operator (subclass fields were not compared).
     */
    public boolean structurallyEquals(Stats other) {
        return equalsStats(this, other);
    }

    /**
     * C#'s {@code operator -}. Like {@link #add}, {@link SingleSourceStat} values are not carried
     * into the result.
     */
    public static Stats subtract(Stats a, Stats b) {
        Stats c = new Stats();

        int i = c.rawAdditiveData.length;
        while (--i >= 0) {
            c.rawAdditiveData[i] = a.rawAdditiveData[i] - b.rawAdditiveData[i];
        }
        i = c.rawMultiplicativeData.length;
        while (--i >= 0) {
            c.rawMultiplicativeData[i] = (1 + a.rawMultiplicativeData[i]) / (1 + b.rawMultiplicativeData[i]) - 1;
        }
        i = c.rawInverseMultiplicativeData.length;
        while (--i >= 0) {
            c.rawInverseMultiplicativeData[i] = 1 - (1 - a.rawInverseMultiplicativeData[i]) / (1 - b.rawInverseMultiplicativeData[i]);
        }

        return c;
    }

    /**
     * C#'s {@code operator *} (and the "undefined for special effects" note that comes with it).
     * {@link SingleSourceStat} values are not scaled or copied into the result.
     */
    public static Stats multiply(Stats a, float b) {
        Stats c = new Stats();

        int i = c.rawAdditiveData.length;
        while (--i >= 0) {
            c.rawAdditiveData[i] = a.rawAdditiveData[i] * b;
        }
        i = c.rawMultiplicativeData.length;
        while (--i >= 0) {
            c.rawMultiplicativeData[i] = a.rawMultiplicativeData[i] * b;
        }
        i = c.rawInverseMultiplicativeData.length;
        while (--i >= 0) {
            c.rawInverseMultiplicativeData[i] = a.rawInverseMultiplicativeData[i] * b;
        }

        i = c.rawNoStackData.length;
        while (--i >= 0) {
            c.rawNoStackData[i] = a.rawNoStackData[i] * b;
        }
        return c;
    }

    /** Appends an effect reference to this snapshot's ordered effect list (never deduplicated). */
    public void addSpecialEffect(SpecialEffect specialEffect) {
        ensureSpecialEffectCapacity(rawSpecialEffectDataSize + 1);
        rawSpecialEffectData[rawSpecialEffectDataSize] = specialEffect;
        rawSpecialEffectDataSize++;
    }

    /** Removes the first effect with the same reference ({@code ==}); the rest keep their order. */
    /** Live backing array of special effects; may contain {@code null} slots past the count. */
    public SpecialEffect[] getRawSpecialEffectData() {
        return rawSpecialEffectData;
    }

    /** Number of live entries in {@link #getRawSpecialEffectData()}. */
    public int getRawSpecialEffectDataSize() {
        return rawSpecialEffectDataSize;
    }

    /** Drops all special effects without touching the stat values (the stats stay intact). */
    public void clearSpecialEffects() {
        for (int i = 0; i < rawSpecialEffectDataSize; i++) {
            rawSpecialEffectData[i] = null;
        }
        rawSpecialEffectDataSize = 0;
    }

    public void removeSpecialEffect(SpecialEffect specialEffect) {
        boolean found = false;
        for (int i = 0; i < rawSpecialEffectDataSize; i++) {
            if (found) {
                rawSpecialEffectData[i - 1] = rawSpecialEffectData[i];
            } else if (specialEffect == rawSpecialEffectData[i]) {
                found = true;
            }
        }
        if (found) {
            rawSpecialEffectDataSize--;
            rawSpecialEffectData[rawSpecialEffectDataSize] = null;
        }
    }

    /**
     * Single-use iterator over this snapshot's effect list, optionally filtered; it reads the live
     * array, so effects added or removed during iteration are not protected against.
     */
    public static final class SpecialEffectEnumerator implements Iterable<SpecialEffect>, java.util.Iterator<SpecialEffect> {
        Stats stats;
        private int index;
        private SpecialEffect current;
        Predicate<SpecialEffect> match;

        @Override
        public boolean hasNext() {
            while (index < stats.rawSpecialEffectDataSize) {
                current = stats.rawSpecialEffectData[index];
                index++;
                if (match == null || match.test(current)) return true;
            }
            index = stats.rawSpecialEffectDataSize + 1;
            current = null;
            return false;
        }

        @Override
        public SpecialEffect next() {
            if (current == null) {
                throw new java.util.NoSuchElementException();
            }
            SpecialEffect result = current;
            current = null;
            return result;
        }

        @Override
        public java.util.Iterator<SpecialEffect> iterator() {
            return this;
        }
    }

    /** Single-use iterator over the effect list; see {@link SpecialEffectEnumerator}. */
    public SpecialEffectEnumerator specialEffects() {
        SpecialEffectEnumerator enumerator = new SpecialEffectEnumerator();
        enumerator.stats = this;
        return enumerator;
    }

    /** Single-use iterator over the effects matching {@code match}. */
    public SpecialEffectEnumerator specialEffects(Predicate<SpecialEffect> match) {
        SpecialEffectEnumerator enumerator = new SpecialEffectEnumerator();
        enumerator.stats = this;
        enumerator.match = match;
        return enumerator;
    }

    /** True when any attached effect matches. */
    public boolean containsSpecialEffect(Predicate<SpecialEffect> match) {
        for (int i = 0; i < rawSpecialEffectDataSize; i++) {
            SpecialEffect effect = rawSpecialEffectData[i];
            if (match.test(effect)) return true;
        }
        return false;
    }

    /** True when at least one special effect is attached. */
    public boolean containsSpecialEffect() {
        return rawSpecialEffectDataSize > 0;
    }

    /**
     * Merges {@code data} into this snapshot, using {@code data}'s sparse index when it already
     * has one and a full scan otherwise, then appends its special effects (C# {@code
     * Accumulate(Stats)}).
     */
    public void accumulate(Stats data) {
        accumulateStats(data, false);
    }

    /**
     * Weighted merge: each contributed value is multiplied by {@code weight} before the category
     * rule (used for proc uptime/stack factors). Special effects are appended unweighted, like the
     * C# overload.
     */
    public void accumulate(Stats data, float weight) {
        if (data.sparseIndices != null) {
            int i = 0;
            for (int a = 0; a < data.sparseAdditiveCount; a++, i++) {
                int index = data.sparseIndices[i];
                rawAdditiveData[index] += weight * data.rawAdditiveData[index];
            }
            for (int a = 0; a < data.sparseMultiplicativeCount; a++, i++) {
                int index = data.sparseIndices[i];
                rawMultiplicativeData[index] = (1 + rawMultiplicativeData[index]) * (1 + weight * data.rawMultiplicativeData[index]) - 1;
            }
            for (int a = 0; a < data.sparseInverseMultiplicativeCount; a++, i++) {
                int index = data.sparseIndices[i];
                rawInverseMultiplicativeData[index] = 1 - (1 - rawInverseMultiplicativeData[index]) * (1 - weight * data.rawInverseMultiplicativeData[index]);
            }
            for (int a = 0; a < data.sparseNoStackCount; a++, i++) {
                int index = data.sparseIndices[i];
                float value = weight * data.rawNoStackData[index];
                if (value > rawNoStackData[index]) rawNoStackData[index] = value;
            }
        } else {
            float[] add = data.rawAdditiveData;
            for (int i = 0; i < rawAdditiveData.length; i++) {
                rawAdditiveData[i] += weight * add[i];
            }
            add = data.rawMultiplicativeData;
            for (int i = 0; i < rawMultiplicativeData.length; i++) {
                rawMultiplicativeData[i] = (1 + rawMultiplicativeData[i]) * (1 + weight * add[i]) - 1;
            }
            add = data.rawInverseMultiplicativeData;
            for (int i = 0; i < rawInverseMultiplicativeData.length; i++) {
                rawInverseMultiplicativeData[i] = 1 - (1 - rawInverseMultiplicativeData[i]) * (1 - weight * add[i]);
            }
            add = data.rawNoStackData;
            for (int i = 0; i < rawNoStackData.length; i++) {
                if (weight * add[i] > rawNoStackData[i]) rawNoStackData[i] = weight * add[i];
            }
        }
        if (data.rawSpecialEffectDataSize > 0) {
            ensureSpecialEffectCapacity(rawSpecialEffectDataSize + data.rawSpecialEffectDataSize);
            System.arraycopy(data.rawSpecialEffectData, 0, rawSpecialEffectData, rawSpecialEffectDataSize, data.rawSpecialEffectDataSize);
            rawSpecialEffectDataSize += data.rawSpecialEffectDataSize;
        }
    }

    /** Legacy-named variant of {@link #accumulate(Stats)} (C# {@code AccumulateUnsafe}). */
    public void accumulateUnsafe(Stats data) {
        accumulateUnsafe(data, false);
    }

    /**
     * Legacy-named variant that can force {@code data.generateSparseData()} first
     * ({@code generateSparseIfNeeded}); sparse and dense accumulation are not numerically
     * identical, so callers keep the original's choice. C# {@code AccumulateUnsafe(Stats, bool)}.
     */
    public void accumulateUnsafe(Stats data, boolean generateSparseIfNeeded) {
        accumulateStats(data, generateSparseIfNeeded);
    }

    /**
     * Shared accumulation core for {@link #accumulate(Stats)} and the {@code accumulateUnsafe}
     * overloads: walks the source's sparse index when it exists (building it first when
     * {@code generateSparseIfNeeded} is set), otherwise every slot, then appends special effects.
     */
    private void accumulateStats(Stats data, boolean generateSparseIfNeeded) {
        if (generateSparseIfNeeded && data.sparseIndices == null) data.generateSparseData();
        if (data.sparseIndices != null) {
            int i = 0;
            int limit = data.sparseAdditiveCount;
            for (; i < limit; i++) {
                int index = data.sparseIndices[i];
                rawAdditiveData[index] += data.rawAdditiveData[index];
            }
            limit += data.sparseMultiplicativeCount;
            for (; i < limit; i++) {
                int index = data.sparseIndices[i];
                rawMultiplicativeData[index] = (1 + rawMultiplicativeData[index]) * (1 + data.rawMultiplicativeData[index]) - 1;
            }
            limit += data.sparseInverseMultiplicativeCount;
            for (; i < limit; i++) {
                int index = data.sparseIndices[i];
                rawInverseMultiplicativeData[index] = 1 - (1 - rawInverseMultiplicativeData[index]) * (1 - data.rawInverseMultiplicativeData[index]);
            }
            limit += data.sparseNoStackCount;
            for (; i < limit; i++) {
                int index = data.sparseIndices[i];
                float value = data.rawNoStackData[index];
                if (value > rawNoStackData[index]) rawNoStackData[index] = value;
            }
        } else {
            accumulateDense(data);
        }
        if (data.rawSpecialEffectDataSize > 0) {
            ensureSpecialEffectCapacity(rawSpecialEffectDataSize + data.rawSpecialEffectDataSize);
            if (data.rawSpecialEffectDataSize == 1) {
                rawSpecialEffectData[rawSpecialEffectDataSize++] = data.rawSpecialEffectData[0];
            } else {
                System.arraycopy(data.rawSpecialEffectData, 0, rawSpecialEffectData, rawSpecialEffectDataSize, data.rawSpecialEffectDataSize);
                rawSpecialEffectDataSize += data.rawSpecialEffectDataSize;
            }
        }
    }

    private void accumulateDense(Stats data) {
        float[] add = data.rawAdditiveData;
        for (int i = 0; i < ADDITIVE_STAT_COUNT; i++) {
            rawAdditiveData[i] += add[i];
        }
        add = data.rawMultiplicativeData;
        for (int i = 0; i < MULTIPLICATIVE_STAT_COUNT; i++) {
            rawMultiplicativeData[i] = (1 + rawMultiplicativeData[i]) * (1 + add[i]) - 1;
        }
        float[] arr = data.rawInverseMultiplicativeData;
        for (int i = 0; i < rawInverseMultiplicativeData.length; i++) {
            rawInverseMultiplicativeData[i] = 1 - (1 - rawInverseMultiplicativeData[i]) * (1 - arr[i]);
        }
        add = data.rawNoStackData;
        for (int i = 0; i < NON_STACKING_STAT_COUNT; i++) {
            if (add[i] > rawNoStackData[i]) rawNoStackData[i] = add[i];
        }
    }

    /**
     * C# {@code operator ==}: null-safe reference equality, otherwise exact equality of the
     * additive, multiplicative and no-stack arrays (the inverse-multiplicative array is not
     * compared); any special effect on either side makes the snapshots unequal.
     */
    public static boolean equalsStats(Stats x, Stats y) {
        if (x == y || (x == null && y == null))
            return true;
        if (x == null || y == null)
            return false;
        if (x.rawSpecialEffectDataSize > 0 || y.rawSpecialEffectDataSize > 0) return false;
        return ArrayUtils.allEqual(x.rawAdditiveData, y.rawAdditiveData) &&
                ArrayUtils.allEqual(x.rawMultiplicativeData, y.rawMultiplicativeData) &&
                ArrayUtils.allEqual(x.rawNoStackData, y.rawNoStackData);
    }

    // C# `operator !=`.
    public static boolean notEquals(Stats x, Stats y) {
        return !equalsStats(x, y);
    }

    // C# Stats.cs:5388-5391 `Equals(object)` is `this == (obj as Stats)`.
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Stats other && equalsStats(this, other);
    }

    // Deliberate deviation from C# (Stats.cs:5383-5386 hashed the additive array by reference):
    // Java requires equals/hashCode to agree, so hash exactly the arrays equalsStats compares.
    // See docs/dev/DEVIATIONS.md D4.
    @Override
    public int hashCode() {
        int result = Arrays.hashCode(rawAdditiveData);
        result = 31 * result + Arrays.hashCode(rawMultiplicativeData);
        result = 31 * result + Arrays.hashCode(rawNoStackData);
        return result;
    }

    // C# Stats.cs:5547-5560 (PropertyInfoCache -> PROPERTY_INFO; Extensions.DisplayName).
    public static String[] getStatNames() {
        String[] names = new String[PROPERTY_INFO.length];
        for (int i = 0; i < PROPERTY_INFO.length; i++) {
            names[i] = Extensions.displayName(PROPERTY_INFO[i]);
        }
        Arrays.sort(names);
        return names;
    }

    /** True when every combining slot of {@code x} is greater than or equal to {@code y}'s. */
    public static boolean greaterOrEqual(Stats x, Stats y) {
        return allCompare(x, y, ArrayUtils.CompareOption.GREATER_THAN | ArrayUtils.CompareOption.EQUAL);
    }

    /** True when every combining slot of {@code x} is less than or equal to {@code y}'s. */
    public static boolean lessOrEqual(Stats x, Stats y) {
        return allCompare(x, y, ArrayUtils.CompareOption.LESS_THAN | ArrayUtils.CompareOption.EQUAL);
    }

    /** C# {@code operator >}: {@link #greaterOrEqual} plus not {@link #equalsStats}-equal. */
    public static boolean greater(Stats x, Stats y) {
        return greaterOrEqual(x, y) && notEquals(x, y);
    }

    /** C# {@code operator <}: {@link #lessOrEqual} plus not {@link #equalsStats}-equal. */
    public static boolean less(Stats x, Stats y) {
        return lessOrEqual(x, y) && notEquals(x, y);
    }

    /**
     * Partial order: {@code Equal} when every compared slot agrees, {@code GreaterThan} /
     * {@code LessThan} when every differing slot points the same way, {@code Unequal} when they
     * are mixed or either side carries special effects (which are not compared). Named
     * {@code compare}, not {@code compareTo}: a partial order must not implement
     * {@link Comparable}. C# {@code Stats.CompareTo} (Stats.cs:5132); a {@code null} other maps to
     * {@code LessThan}, the enum's zero value, matching the original.
     */
    public ArrayUtils.CompareResult compare(Stats other) {
        if (other == null)
            return ArrayUtils.CompareResult.LessThan;
        if (this.rawSpecialEffectDataSize > 0 || other.rawSpecialEffectDataSize > 0) {
            return ArrayUtils.CompareResult.Unequal; // not sure if we need to actually go through and compare each
        }
        if (this.sparseIndices != null && other.sparseIndices != null) {
            return compareSparse(other);
        }
        return ArrayUtils.and(ArrayUtils.allCompare(this.rawAdditiveData, other.rawAdditiveData), ArrayUtils.and(
                ArrayUtils.allCompare(this.rawMultiplicativeData, other.rawMultiplicativeData),
                ArrayUtils.allCompare(this.rawNoStackData, other.rawNoStackData)));
    }

    /**
     * Merges the two sparse index lists category by category (each list is sorted and
     * concatenated in category order), tracking whether any differing slot favoured either side.
     * The dense fallback in {@link #compare(Stats)} intentionally compares three arrays and the
     * sparse path four; that asymmetry is the original's.
     */
    private ArrayUtils.CompareResult compareSparse(Stats other) {
        boolean haveGreaterThan = false, haveLessThan = false;
        int j = 0;
        int i = 0;
        for (int category = 0; category < 4; category++) {
            float[] mine;
            float[] theirs;
            int myCount;
            int otherCount;
            switch (category) {
                case 0:
                    mine = rawAdditiveData; theirs = other.rawAdditiveData;
                    myCount = sparseAdditiveCount; otherCount = other.sparseAdditiveCount;
                    break;
                case 1:
                    mine = rawMultiplicativeData; theirs = other.rawMultiplicativeData;
                    myCount = sparseMultiplicativeCount; otherCount = other.sparseMultiplicativeCount;
                    break;
                case 2:
                    mine = rawInverseMultiplicativeData; theirs = other.rawInverseMultiplicativeData;
                    myCount = sparseInverseMultiplicativeCount; otherCount = other.sparseInverseMultiplicativeCount;
                    break;
                default:
                    mine = rawNoStackData; theirs = other.rawNoStackData;
                    myCount = sparseNoStackCount; otherCount = other.sparseNoStackCount;
                    break;
            }
            int b = 0;
            for (int a = 0; a < otherCount; a++, i++) {
                int index = other.sparseIndices[i];
                while (b < myCount && sparseIndices[j] < index) {
                    b++;
                    j++;
                    haveGreaterThan = true;
                    if (haveLessThan) {
                        return ArrayUtils.CompareResult.Unequal;
                    }
                }
                if (b < myCount && sparseIndices[j] == index) {
                    int val = compareSingle(mine[index], theirs[index]);
                    if (val < 0) {
                        haveLessThan = true;
                        if (haveGreaterThan) {
                            return ArrayUtils.CompareResult.Unequal;
                        }
                    } else if (val > 0) {
                        haveGreaterThan = true;
                        if (haveLessThan) {
                            return ArrayUtils.CompareResult.Unequal;
                        }
                    }
                    b++;
                    j++;
                } else {
                    haveLessThan = true;
                    if (haveGreaterThan) {
                        return ArrayUtils.CompareResult.Unequal;
                    }
                }
            }
            while (b < myCount) {
                b++;
                j++;
                haveGreaterThan = true;
                if (haveLessThan) {
                    return ArrayUtils.CompareResult.Unequal;
                }
            }
        }
        if (haveGreaterThan && haveLessThan) return ArrayUtils.CompareResult.Unequal;
        else if (haveGreaterThan) return ArrayUtils.CompareResult.GreaterThan;
        else if (haveLessThan) return ArrayUtils.CompareResult.LessThan;
        else return ArrayUtils.CompareResult.Equal;
    }

    // .NET Single.CompareTo: NaN compares less than every value (Java Float.compare puts NaN last).
    private static int compareSingle(float x, float y) {
        if (x < y) return -1;
        if (x > y) return 1;
        if (x == y) return 0;
        if (Float.isNaN(x)) return Float.isNaN(y) ? 0 : -1;
        return 1;
    }

    /** True when {@code x} and {@code y} satisfy {@code comparison} in every combining slot. */
    private static boolean allCompare(Stats x, Stats y, int comparison) {
        java.util.Objects.requireNonNull(x, "x");
        java.util.Objects.requireNonNull(y, "y");
        if (x.rawSpecialEffectDataSize > 0 || y.rawSpecialEffectDataSize > 0) return false;
        return ArrayUtils.allCompare(x.rawAdditiveData, y.rawAdditiveData, comparison)
                && ArrayUtils.allCompare(x.rawMultiplicativeData, y.rawMultiplicativeData, comparison)
                && ArrayUtils.allCompare(x.rawInverseMultiplicativeData, y.rawInverseMultiplicativeData, comparison)
                && ArrayUtils.allCompare(x.rawNoStackData, y.rawNoStackData, comparison);
    }

    // C# Stats.cs:5436-5459 ToString / NET. TrimEnd(' ', ',', ':').
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StatProperty info : PROPERTY_INFO) {
            float value = info.accessor().get(this);
            if (value != 0) {
                if (info.percentage()) {
                    value *= 100;
                }
                value = (float) Math.rint((double) (value * 100f)) / 100f;
                sb.append(NetFormat.netFloatToString(value)).append(Extensions.displayName(info)).append(", ");
            }
        }
        for (SpecialEffect effect : specialEffects()) {
            sb.append(effect).append(", ");
        }
        return trimEnd(sb.toString());
    }

    private static String trimEnd(String value) {
        int end = value.length();
        while (end > 0 && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == ',' || value.charAt(end - 1) == ':')) {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * Deep copy: a new {@code Stats} with independent arrays and a cloned special-effect array; the
     * lazy sparse-index cache is shared because every setter invalidates it on the copy.
     *
     * <p>Port of the C# {@code Stats.Clone()} ({@code MemberwiseClone} + array {@code Clone()}s).
     * Named {@code copy} rather than {@code clone} on purpose: Java's {@code Object.clone()} is a
     * protected {@code Cloneable} protocol with checked exceptions and allocates without calling a
     * constructor, none of which fits this method; overriding it only to bypass it would be
     * misleading.</p>
     */
    public Stats copy() {
        Stats copy = new Stats();
        System.arraycopy(rawAdditiveData, 0, copy.rawAdditiveData, 0, rawAdditiveData.length);
        System.arraycopy(rawMultiplicativeData, 0, copy.rawMultiplicativeData, 0, rawMultiplicativeData.length);
        System.arraycopy(rawInverseMultiplicativeData, 0, copy.rawInverseMultiplicativeData, 0, rawInverseMultiplicativeData.length);
        System.arraycopy(rawNoStackData, 0, copy.rawNoStackData, 0, rawNoStackData.length);
        // C# Clone() uses MemberwiseClone, which preserves the StatsCat/StatsBear subclass fields;
        // the single-source category is the port's equivalent and is copied here for the same reason.
        System.arraycopy(rawSingleSourceData, 0, copy.rawSingleSourceData, 0, rawSingleSourceData.length);
        copy.rawSpecialEffectDataSize = rawSpecialEffectDataSize;
        if (rawSpecialEffectData != null) copy.rawSpecialEffectData = rawSpecialEffectData.clone();
        copy.sparseIndices = sparseIndices;
        copy.sparseAdditiveCount = sparseAdditiveCount;
        copy.sparseMultiplicativeCount = sparseMultiplicativeCount;
        copy.sparseInverseMultiplicativeCount = sparseInverseMultiplicativeCount;
        copy.sparseNoStackCount = sparseNoStackCount;
        return copy;
    }

    // == Stat accessors (reference, grouped by category) #################################################

    // == Base Stats ======================================================================================

    public float getArmor() {
        return rawAdditiveData[AdditiveStat.Armor.value()];
    }

    public void setArmor(float value) {
        rawAdditiveData[AdditiveStat.Armor.value()] = value;
    }

    public float getBonusArmor() {
        return rawAdditiveData[AdditiveStat.BonusArmor.value()];
    }

    public void setBonusArmor(float value) {
        rawAdditiveData[AdditiveStat.BonusArmor.value()] = value;
    }

    public float getHealth() {
        return rawAdditiveData[AdditiveStat.Health.value()];
    }

    public void setHealth(float value) {
        rawAdditiveData[AdditiveStat.Health.value()] = value;
    }

    public float getMana() {
        return rawAdditiveData[AdditiveStat.Mana.value()];
    }

    public void setMana(float value) {
        rawAdditiveData[AdditiveStat.Mana.value()] = value;
    }

    public float getAgility() {
        return rawAdditiveData[AdditiveStat.Agility.value()];
    }

    public void setAgility(float value) {
        rawAdditiveData[AdditiveStat.Agility.value()] = value;
    }

    public float getStamina() {
        return rawAdditiveData[AdditiveStat.Stamina.value()];
    }

    public void setStamina(float value) {
        rawAdditiveData[AdditiveStat.Stamina.value()] = value;
    }

    public float getAttackPower() {
        return rawAdditiveData[AdditiveStat.AttackPower.value()];
    }

    public void setAttackPower(float value) {
        rawAdditiveData[AdditiveStat.AttackPower.value()] = value;
    }

    public float getStrength() {
        return rawAdditiveData[AdditiveStat.Strength.value()];
    }

    public void setStrength(float value) {
        rawAdditiveData[AdditiveStat.Strength.value()] = value;
    }

    public float getArmorPenetrationRating() {
        return rawAdditiveData[AdditiveStat.ArmorPenetrationRating.value()];
    }

    public void setArmorPenetrationRating(float value) {
        rawAdditiveData[AdditiveStat.ArmorPenetrationRating.value()] = value;
    }

    public float getIntellect() {
        return rawAdditiveData[AdditiveStat.Intellect.value()];
    }

    public void setIntellect(float value) {
        rawAdditiveData[AdditiveStat.Intellect.value()] = value;
    }

    public float getSpirit() {
        return rawAdditiveData[AdditiveStat.Spirit.value()];
    }

    public void setSpirit(float value) {
        rawAdditiveData[AdditiveStat.Spirit.value()] = value;
    }

    public float getSpellPower() {
        return rawAdditiveData[AdditiveStat.SpellPower.value()];
    }

    public void setSpellPower(float value) {
        rawAdditiveData[AdditiveStat.SpellPower.value()] = value;
    }

    public float getSpellPenetration() {
        return rawAdditiveData[AdditiveStat.SpellPenetration.value()];
    }

    public void setSpellPenetration(float value) {
        rawAdditiveData[AdditiveStat.SpellPenetration.value()] = value;
    }

    public float getCritRating() {
        return rawAdditiveData[AdditiveStat.CritRating.value()];
    }

    public void setCritRating(float value) {
        rawAdditiveData[AdditiveStat.CritRating.value()] = value;
    }

    public float getHitRating() {
        return rawAdditiveData[AdditiveStat.HitRating.value()];
    }

    public void setHitRating(float value) {
        rawAdditiveData[AdditiveStat.HitRating.value()] = value;
    }

    public float getMasteryRating() {
        return rawAdditiveData[AdditiveStat.MasteryRating.value()];
    }

    public void setMasteryRating(float value) {
        rawAdditiveData[AdditiveStat.MasteryRating.value()] = value;
    }

    public float getDodgeRating() {
        return rawAdditiveData[AdditiveStat.DodgeRating.value()];
    }

    public void setDodgeRating(float value) {
        rawAdditiveData[AdditiveStat.DodgeRating.value()] = value;
    }

    public float getParryRating() {
        return rawAdditiveData[AdditiveStat.ParryRating.value()];
    }

    public void setParryRating(float value) {
        rawAdditiveData[AdditiveStat.ParryRating.value()] = value;
    }

    public float getBlockRating() {
        return rawAdditiveData[AdditiveStat.BlockRating.value()];
    }

    public void setBlockRating(float value) {
        rawAdditiveData[AdditiveStat.BlockRating.value()] = value;
    }

    public float getBlockValue() {
        return rawAdditiveData[AdditiveStat.BlockValue.value()];
    }

    public void setBlockValue(float value) {
        rawAdditiveData[AdditiveStat.BlockValue.value()] = value;
    }

    public float getDefenseRating() {
        return rawAdditiveData[AdditiveStat.DefenseRating.value()];
    }

    public void setDefenseRating(float value) {
        rawAdditiveData[AdditiveStat.DefenseRating.value()] = value;
    }

    public float getResilience() {
        return rawAdditiveData[AdditiveStat.Resilience.value()];
    }

    public void setResilience(float value) {
        rawAdditiveData[AdditiveStat.Resilience.value()] = value;
    }

    public float getExpertiseRating() {
        return rawAdditiveData[AdditiveStat.ExpertiseRating.value()];
    }

    public void setExpertiseRating(float value) {
        rawAdditiveData[AdditiveStat.ExpertiseRating.value()] = value;
    }

    public float getHasteRating() {
        return rawAdditiveData[AdditiveStat.HasteRating.value()];
    }

    public void setHasteRating(float value) {
        rawAdditiveData[AdditiveStat.HasteRating.value()] = value;
    }

    public float getMp5() {
        return rawAdditiveData[AdditiveStat.Mp5.value()];
    }

    public void setMp5(float value) {
        rawAdditiveData[AdditiveStat.Mp5.value()] = value;
    }

    public float getHp5() {
        return rawAdditiveData[AdditiveStat.Hp5.value()];
    }

    public void setHp5(float value) {
        rawAdditiveData[AdditiveStat.Hp5.value()] = value;
    }

    // == Combat Values ===================================================================================

    public float getArmorPenetration() {
        return rawInverseMultiplicativeData[InverseMultiplicativeStat.ArmorPenetration.value()];
    }

    public void setArmorPenetration(float value) {
        rawInverseMultiplicativeData[InverseMultiplicativeStat.ArmorPenetration.value()] = value;
    }

    public float getSpellCrit() {
        return rawAdditiveData[AdditiveStat.SpellCrit.value()];
    }

    public void setSpellCrit(float value) {
        rawAdditiveData[AdditiveStat.SpellCrit.value()] = value;
    }

    public float getSpellCritOnTarget() {
        return rawAdditiveData[AdditiveStat.SpellCritOnTarget.value()];
    }

    public void setSpellCritOnTarget(float value) {
        rawAdditiveData[AdditiveStat.SpellCritOnTarget.value()] = value;
    }

    public float getSpellHit() {
        return rawAdditiveData[AdditiveStat.SpellHit.value()];
    }

    public void setSpellHit(float value) {
        rawAdditiveData[AdditiveStat.SpellHit.value()] = value;
    }

    public float getSpellHaste() {
        return rawMultiplicativeData[MultiplicativeStat.SpellHaste.value()];
    }

    public void setSpellHaste(float value) {
        rawMultiplicativeData[MultiplicativeStat.SpellHaste.value()] = value;
    }

    public float getSpellCombatManaRegeneration() {
        return rawAdditiveData[AdditiveStat.SpellCombatManaRegeneration.value()];
    }

    public void setSpellCombatManaRegeneration(float value) {
        rawAdditiveData[AdditiveStat.SpellCombatManaRegeneration.value()] = value;
    }

    public float getPhysicalCrit() {
        return rawAdditiveData[AdditiveStat.PhysicalCrit.value()];
    }

    public void setPhysicalCrit(float value) {
        rawAdditiveData[AdditiveStat.PhysicalCrit.value()] = value;
    }

    public float getParry() {
        return rawAdditiveData[AdditiveStat.Parry.value()];
    }

    public void setParry(float value) {
        rawAdditiveData[AdditiveStat.Parry.value()] = value;
    }

    public float getBlock() {
        return rawAdditiveData[AdditiveStat.Block.value()];
    }

    public void setBlock(float value) {
        rawAdditiveData[AdditiveStat.Block.value()] = value;
    }

    public float getPhysicalHit() {
        return rawAdditiveData[AdditiveStat.PhysicalHit.value()];
    }

    public void setPhysicalHit(float value) {
        rawAdditiveData[AdditiveStat.PhysicalHit.value()] = value;
    }

    public float getDodge() {
        return rawAdditiveData[AdditiveStat.Dodge.value()];
    }

    public void setDodge(float value) {
        rawAdditiveData[AdditiveStat.Dodge.value()] = value;
    }

    public float getDefense() {
        return rawAdditiveData[AdditiveStat.Defense.value()];
    }

    public void setDefense(float value) {
        rawAdditiveData[AdditiveStat.Defense.value()] = value;
    }

    public float getExpertise() {
        return rawAdditiveData[AdditiveStat.Expertise.value()];
    }

    public void setExpertise(float value) {
        rawAdditiveData[AdditiveStat.Expertise.value()] = value;
    }

    public float getPhysicalHaste() {
        return rawMultiplicativeData[MultiplicativeStat.PhysicalHaste.value()];
    }

    public void setPhysicalHaste(float value) {
        rawMultiplicativeData[MultiplicativeStat.PhysicalHaste.value()] = value;
    }

    public float getRangedHaste() {
        return rawMultiplicativeData[MultiplicativeStat.RangedHaste.value()];
    }

    public void setRangedHaste(float value) {
        rawMultiplicativeData[MultiplicativeStat.RangedHaste.value()] = value;
    }

    public float getMiss() {
        return rawAdditiveData[AdditiveStat.Miss.value()];
    }

    public void setMiss(float value) {
        rawAdditiveData[AdditiveStat.Miss.value()] = value;
    }

    // == Spell Combat Ratings ============================================================================

    public float getSpellShadowDamageRating() {
        return rawAdditiveData[AdditiveStat.SpellShadowDamageRating.value()];
    }

    public void setSpellShadowDamageRating(float value) {
        rawAdditiveData[AdditiveStat.SpellShadowDamageRating.value()] = value;
    }

    public float getSpellFireDamageRating() {
        return rawAdditiveData[AdditiveStat.SpellFireDamageRating.value()];
    }

    public void setSpellFireDamageRating(float value) {
        rawAdditiveData[AdditiveStat.SpellFireDamageRating.value()] = value;
    }

    public float getSpellFrostDamageRating() {
        return rawAdditiveData[AdditiveStat.SpellFrostDamageRating.value()];
    }

    public void setSpellFrostDamageRating(float value) {
        rawAdditiveData[AdditiveStat.SpellFrostDamageRating.value()] = value;
    }

    public float getSpellArcaneDamageRating() {
        return rawAdditiveData[AdditiveStat.SpellArcaneDamageRating.value()];
    }

    public void setSpellArcaneDamageRating(float value) {
        rawAdditiveData[AdditiveStat.SpellArcaneDamageRating.value()] = value;
    }

    public float getSpellNatureDamageRating() {
        return rawAdditiveData[AdditiveStat.SpellNatureDamageRating.value()];
    }

    public void setSpellNatureDamageRating(float value) {
        rawAdditiveData[AdditiveStat.SpellNatureDamageRating.value()] = value;
    }

    // == Resistances =====================================================================================

    public float getFrostResistance() {
        return rawAdditiveData[AdditiveStat.FrostResistance.value()];
    }

    public void setFrostResistance(float value) {
        rawAdditiveData[AdditiveStat.FrostResistance.value()] = value;
    }

    public float getNatureResistance() {
        return rawAdditiveData[AdditiveStat.NatureResistance.value()];
    }

    public void setNatureResistance(float value) {
        rawAdditiveData[AdditiveStat.NatureResistance.value()] = value;
    }

    public float getFireResistance() {
        return rawAdditiveData[AdditiveStat.FireResistance.value()];
    }

    public void setFireResistance(float value) {
        rawAdditiveData[AdditiveStat.FireResistance.value()] = value;
    }

    public float getShadowResistance() {
        return rawAdditiveData[AdditiveStat.ShadowResistance.value()];
    }

    public void setShadowResistance(float value) {
        rawAdditiveData[AdditiveStat.ShadowResistance.value()] = value;
    }

    public float getArcaneResistance() {
        return rawAdditiveData[AdditiveStat.ArcaneResistance.value()];
    }

    public void setArcaneResistance(float value) {
        rawAdditiveData[AdditiveStat.ArcaneResistance.value()] = value;
    }

    public float getFrostResistanceBuff() {
        return rawNoStackData[NonStackingStat.FrostResistanceBuff.value()];
    }

    public void setFrostResistanceBuff(float value) {
        rawNoStackData[NonStackingStat.FrostResistanceBuff.value()] = value;
    }

    public float getNatureResistanceBuff() {
        return rawNoStackData[NonStackingStat.NatureResistanceBuff.value()];
    }

    public void setNatureResistanceBuff(float value) {
        rawNoStackData[NonStackingStat.NatureResistanceBuff.value()] = value;
    }

    public float getFireResistanceBuff() {
        return rawNoStackData[NonStackingStat.FireResistanceBuff.value()];
    }

    public void setFireResistanceBuff(float value) {
        rawNoStackData[NonStackingStat.FireResistanceBuff.value()] = value;
    }

    public float getShadowResistanceBuff() {
        return rawNoStackData[NonStackingStat.ShadowResistanceBuff.value()];
    }

    public void setShadowResistanceBuff(float value) {
        rawNoStackData[NonStackingStat.ShadowResistanceBuff.value()] = value;
    }

    public float getArcaneResistanceBuff() {
        return rawNoStackData[NonStackingStat.ArcaneResistanceBuff.value()];
    }

    public void setArcaneResistanceBuff(float value) {
        rawNoStackData[NonStackingStat.ArcaneResistanceBuff.value()] = value;
    }

    // == Buffs / Debuffs =================================================================================

    public float getBossAttackPower() {
        return rawAdditiveData[AdditiveStat.BossAttackPower.value()];
    }

    public void setBossAttackPower(float value) {
        rawAdditiveData[AdditiveStat.BossAttackPower.value()] = value;
    }

    public float getHealingReceivedMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.HealingReceivedMultiplier.value()];
    }

    public void setHealingReceivedMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.HealingReceivedMultiplier.value()] = value;
    }

    public float getBonusHealingDoneMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHealingDoneMultiplier.value()];
    }

    public void setBonusHealingDoneMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHealingDoneMultiplier.value()] = value;
    }

    public float getManaRestoreFromBaseManaPPM() {
        return rawAdditiveData[AdditiveStat.ManaRestoreFromBaseManaPPM.value()];
    }

    public void setManaRestoreFromBaseManaPPM(float value) {
        rawAdditiveData[AdditiveStat.ManaRestoreFromBaseManaPPM.value()] = value;
    }

    public float getManaRestoreFromMaxManaPerSecond() {
        return rawAdditiveData[AdditiveStat.ManaRestoreFromMaxManaPerSecond.value()];
    }

    public void setManaRestoreFromMaxManaPerSecond(float value) {
        rawAdditiveData[AdditiveStat.ManaRestoreFromMaxManaPerSecond.value()] = value;
    }

    public float getBonusFireDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusFireDamageMultiplier.value()];
    }

    public void setBonusFireDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusFireDamageMultiplier.value()] = value;
    }

    public float getBonusShadowDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusShadowDamageMultiplier.value()];
    }

    public void setBonusShadowDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusShadowDamageMultiplier.value()] = value;
    }

    public float getBonusHolyDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHolyDamageMultiplier.value()];
    }

    public void setBonusHolyDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHolyDamageMultiplier.value()] = value;
    }

    public float getBonusArcaneDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusArcaneDamageMultiplier.value()];
    }

    public void setBonusArcaneDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusArcaneDamageMultiplier.value()] = value;
    }

    public float getBonusNatureDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusNatureDamageMultiplier.value()];
    }

    public void setBonusNatureDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusNatureDamageMultiplier.value()] = value;
    }

    public float getBonusFrostDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusFrostDamageMultiplier.value()];
    }

    public void setBonusFrostDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusFrostDamageMultiplier.value()] = value;
    }

    public float getBonusDiseaseDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusDiseaseDamageMultiplier.value()];
    }

    public void setBonusDiseaseDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusDiseaseDamageMultiplier.value()] = value;
    }

    public float getBonusSpiritMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSpiritMultiplier.value()];
    }

    public void setBonusSpiritMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSpiritMultiplier.value()] = value;
    }

    public float getBonusCritMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusCritMultiplier.value()];
    }

    public void setBonusCritMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusCritMultiplier.value()] = value;
    }

    public float getBonusSpellCritMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSpellCritMultiplier.value()];
    }

    public void setBonusSpellCritMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSpellCritMultiplier.value()] = value;
    }

    public float getBonusPhysicalDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPhysicalDamageMultiplier.value()];
    }

    public void setBonusPhysicalDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPhysicalDamageMultiplier.value()] = value;
    }

    public float getBonusDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusDamageMultiplier.value()];
    }

    public void setBonusDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusDamageMultiplier.value()] = value;
    }

    public float getManaCostPerc() {
        return rawMultiplicativeData[MultiplicativeStat.ManaCostPerc.value()];
    }

    public void setManaCostPerc(float value) {
        rawMultiplicativeData[MultiplicativeStat.ManaCostPerc.value()] = value;
    }

    public float getThreatIncreaseMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.ThreatIncreaseMultiplier.value()];
    }

    public void setThreatIncreaseMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.ThreatIncreaseMultiplier.value()] = value;
    }

    public float getBossAttackSpeedMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BossAttackSpeedMultiplier.value()];
    }

    public void setBossAttackSpeedMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BossAttackSpeedMultiplier.value()] = value;
    }

    public float getBonusBleedDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusBleedDamageMultiplier.value()];
    }

    public void setBonusBleedDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusBleedDamageMultiplier.value()] = value;
    }

    public float getBonusAgilityMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusAgilityMultiplier.value()];
    }

    public void setBonusAgilityMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusAgilityMultiplier.value()] = value;
    }

    public float getBonusStrengthMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusStrengthMultiplier.value()];
    }

    public void setBonusStrengthMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusStrengthMultiplier.value()] = value;
    }

    public float getBonusStaminaMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusStaminaMultiplier.value()];
    }

    public void setBonusStaminaMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusStaminaMultiplier.value()] = value;
    }

    public float getBonusHealthMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHealthMultiplier.value()];
    }

    public void setBonusHealthMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHealthMultiplier.value()] = value;
    }

    public float getBonusIntellectMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusIntellectMultiplier.value()];
    }

    public void setBonusIntellectMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusIntellectMultiplier.value()] = value;
    }

    public float getBonusArmorMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusArmorMultiplier.value()];
    }

    public void setBonusArmorMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusArmorMultiplier.value()] = value;
    }

    public float getBaseArmorMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BaseArmorMultiplier.value()];
    }

    public void setBaseArmorMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BaseArmorMultiplier.value()] = value;
    }

    public float getBonusBlockValueMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusBlockValueMultiplier.value()];
    }

    public void setBonusBlockValueMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusBlockValueMultiplier.value()] = value;
    }

    public float getBonusAttackPowerMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusAttackPowerMultiplier.value()];
    }

    public void setBonusAttackPowerMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusAttackPowerMultiplier.value()] = value;
    }

    public float getBonusSpellPowerMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSpellPowerMultiplier.value()];
    }

    public void setBonusSpellPowerMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSpellPowerMultiplier.value()] = value;
    }

    public float getThreatReductionMultiplier() {
        return rawInverseMultiplicativeData[InverseMultiplicativeStat.ThreatReductionMultiplier.value()];
    }

    public void setThreatReductionMultiplier(float value) {
        rawInverseMultiplicativeData[InverseMultiplicativeStat.ThreatReductionMultiplier.value()] = value;
    }

    public float getDamageTakenMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.DamageTakenMultiplier.value()];
    }

    public void setDamageTakenMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.DamageTakenMultiplier.value()] = value;
    }

    public float getSpellDamageTakenMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.SpellDamageTakenMultiplier.value()];
    }

    public void setSpellDamageTakenMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.SpellDamageTakenMultiplier.value()] = value;
    }

    public float getPhysicalDamageTakenMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.PhysicalDamageTakenMultiplier.value()];
    }

    public void setPhysicalDamageTakenMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.PhysicalDamageTakenMultiplier.value()] = value;
    }

    public float getStunDurReduc() {
        return rawMultiplicativeData[MultiplicativeStat.StunDurReduc.value()];
    }

    public void setStunDurReduc(float value) {
        rawMultiplicativeData[MultiplicativeStat.StunDurReduc.value()] = value;
    }

    public float getSnareRootDurReduc() {
        return rawMultiplicativeData[MultiplicativeStat.SnareRootDurReduc.value()];
    }

    public void setSnareRootDurReduc(float value) {
        rawMultiplicativeData[MultiplicativeStat.SnareRootDurReduc.value()] = value;
    }

    public float getFearDurReduc() {
        return rawMultiplicativeData[MultiplicativeStat.FearDurReduc.value()];
    }

    public void setFearDurReduc(float value) {
        rawMultiplicativeData[MultiplicativeStat.FearDurReduc.value()] = value;
    }

    // == Equipment Effects ===============================================================================

    public float getCritBonusDamage() {
        return rawAdditiveData[AdditiveStat.CritBonusDamage.value()];
    }

    public void setCritBonusDamage(float value) {
        rawAdditiveData[AdditiveStat.CritBonusDamage.value()] = value;
    }

    public float getManaorEquivRestore() {
        return rawAdditiveData[AdditiveStat.ManaorEquivRestore.value()];
    }

    public void setManaorEquivRestore(float value) {
        rawAdditiveData[AdditiveStat.ManaorEquivRestore.value()] = value;
    }

    public float getHealthRestore() {
        return rawAdditiveData[AdditiveStat.HealthRestore.value()];
    }

    public void setHealthRestore(float value) {
        rawAdditiveData[AdditiveStat.HealthRestore.value()] = value;
    }

    public float getHealthRestoreFromMaxHealth() {
        return rawAdditiveData[AdditiveStat.HealthRestoreFromMaxHealth.value()];
    }

    public void setHealthRestoreFromMaxHealth(float value) {
        rawAdditiveData[AdditiveStat.HealthRestoreFromMaxHealth.value()] = value;
    }

    public float getManaRestore() {
        return rawAdditiveData[AdditiveStat.ManaRestore.value()];
    }

    public void setManaRestore(float value) {
        rawAdditiveData[AdditiveStat.ManaRestore.value()] = value;
    }

    public float getPVPTrinket() {
        return rawAdditiveData[AdditiveStat.PVPTrinket.value()];
    }

    public void setPVPTrinket(float value) {
        rawAdditiveData[AdditiveStat.PVPTrinket.value()] = value;
    }

    public float getBonusHealingReceived() {
        return rawAdditiveData[AdditiveStat.BonusHealingReceived.value()];
    }

    public void setBonusHealingReceived(float value) {
        rawAdditiveData[AdditiveStat.BonusHealingReceived.value()] = value;
    }

    public float getExtractOfNecromanticPowerProc() {
        return rawAdditiveData[AdditiveStat.ExtractOfNecromanticPowerProc.value()];
    }

    public void setExtractOfNecromanticPowerProc(float value) {
        rawAdditiveData[AdditiveStat.ExtractOfNecromanticPowerProc.value()] = value;
    }

    public float getDarkmoonCardDeathProc() {
        return rawAdditiveData[AdditiveStat.DarkmoonCardDeathProc.value()];
    }

    public void setDarkmoonCardDeathProc(float value) {
        rawAdditiveData[AdditiveStat.DarkmoonCardDeathProc.value()] = value;
    }

    public float getShieldFromHealed() {
        return rawNoStackData[NonStackingStat.ShieldFromHealed.value()];
    }

    public void setShieldFromHealed(float value) {
        rawNoStackData[NonStackingStat.ShieldFromHealed.value()] = value;
    }

    public float getDeathbringerProc() {
        return rawNoStackData[NonStackingStat.DeathbringerProc.value()];
    }

    public void setDeathbringerProc(float value) {
        rawNoStackData[NonStackingStat.DeathbringerProc.value()] = value;
    }

    public float getZodProc() {
        return rawNoStackData[NonStackingStat.ZodProc.value()];
    }

    public void setZodProc(float value) {
        rawNoStackData[NonStackingStat.ZodProc.value()] = value;
    }

    public float getBattlemasterHealth() {
        return rawNoStackData[NonStackingStat.BattlemasterHealth.value()];
    }

    public void setBattlemasterHealth(float value) {
        rawNoStackData[NonStackingStat.BattlemasterHealth.value()] = value;
    }

    public float getHighestStat() {
        return rawNoStackData[NonStackingStat.HighestStat.value()];
    }

    public void setHighestStat(float value) {
        rawNoStackData[NonStackingStat.HighestStat.value()] = value;
    }

    public float getParagon() {
        return rawAdditiveData[AdditiveStat.Paragon.value()];
    }

    public void setParagon(float value) {
        rawAdditiveData[AdditiveStat.Paragon.value()] = value;
    }

    public float getHealed() {
        return rawAdditiveData[AdditiveStat.Healed.value()];
    }

    public void setHealed(float value) {
        rawAdditiveData[AdditiveStat.Healed.value()] = value;
    }

    public float getDamageAbsorbed() {
        return rawAdditiveData[AdditiveStat.DamageAbsorbed.value()];
    }

    public void setDamageAbsorbed(float value) {
        rawAdditiveData[AdditiveStat.DamageAbsorbed.value()] = value;
    }

    public float getPhysicalDamage() {
        return rawAdditiveData[AdditiveStat.PhysicalDamage.value()];
    }

    public void setPhysicalDamage(float value) {
        rawAdditiveData[AdditiveStat.PhysicalDamage.value()] = value;
    }

    public float getArcaneDamage() {
        return rawAdditiveData[AdditiveStat.ArcaneDamage.value()];
    }

    public void setArcaneDamage(float value) {
        rawAdditiveData[AdditiveStat.ArcaneDamage.value()] = value;
    }

    public float getShadowDamage() {
        return rawAdditiveData[AdditiveStat.ShadowDamage.value()];
    }

    public void setShadowDamage(float value) {
        rawAdditiveData[AdditiveStat.ShadowDamage.value()] = value;
    }

    public float getFireDamage() {
        return rawAdditiveData[AdditiveStat.FireDamage.value()];
    }

    public void setFireDamage(float value) {
        rawAdditiveData[AdditiveStat.FireDamage.value()] = value;
    }

    public float getFrostDamage() {
        return rawAdditiveData[AdditiveStat.FrostDamage.value()];
    }

    public void setFrostDamage(float value) {
        rawAdditiveData[AdditiveStat.FrostDamage.value()] = value;
    }

    public float getHolyDamage() {
        return rawAdditiveData[AdditiveStat.HolyDamage.value()];
    }

    public void setHolyDamage(float value) {
        rawAdditiveData[AdditiveStat.HolyDamage.value()] = value;
    }

    public float getNatureDamage() {
        return rawAdditiveData[AdditiveStat.NatureDamage.value()];
    }

    public void setNatureDamage(float value) {
        rawAdditiveData[AdditiveStat.NatureDamage.value()] = value;
    }

    public float getValkyrDamage() {
        return rawAdditiveData[AdditiveStat.ValkyrDamage.value()];
    }

    public void setValkyrDamage(float value) {
        rawAdditiveData[AdditiveStat.ValkyrDamage.value()] = value;
    }

    public float getSpellsManaReduction() {
        return rawAdditiveData[AdditiveStat.SpellsManaReduction.value()];
    }

    public void setSpellsManaReduction(float value) {
        rawAdditiveData[AdditiveStat.SpellsManaReduction.value()] = value;
    }

    public float getHealingOmenProc() {
        return rawAdditiveData[AdditiveStat.HealingOmenProc.value()];
    }

    public void setHealingOmenProc(float value) {
        rawAdditiveData[AdditiveStat.HealingOmenProc.value()] = value;
    }

    public float getMoteOfAnger() {
        return rawAdditiveData[AdditiveStat.MoteOfAnger.value()];
    }

    public void setMoteOfAnger(float value) {
        rawAdditiveData[AdditiveStat.MoteOfAnger.value()] = value;
    }

    public float getBonusManaMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusManaMultiplier.value()];
    }

    public void setBonusManaMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusManaMultiplier.value()] = value;
    }

    public float getBonusCritHealMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusCritHealMultiplier.value()];
    }

    public void setBonusCritHealMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusCritHealMultiplier.value()] = value;
    }

    public float getBonusManaPotion() {
        return rawNoStackData[NonStackingStat.BonusManaPotion.value()];
    }

    public void setBonusManaPotion(float value) {
        rawNoStackData[NonStackingStat.BonusManaPotion.value()] = value;
    }

    public float getSpellReflectChance() {
        return rawAdditiveData[AdditiveStat.SpellReflectChance.value()];
    }

    public void setSpellReflectChance(float value) {
        rawAdditiveData[AdditiveStat.SpellReflectChance.value()] = value;
    }

    // == Old Equipment Procs =============================================================================

    public float getManaRestoreOnCast_5_15() {
        return rawAdditiveData[AdditiveStat.ManaRestoreOnCast_5_15.value()];
    }

    public void setManaRestoreOnCast_5_15(float value) {
        rawAdditiveData[AdditiveStat.ManaRestoreOnCast_5_15.value()] = value;
    }

    public float getManacostReduceWithin15OnHealingCast() {
        return rawAdditiveData[AdditiveStat.ManacostReduceWithin15OnHealingCast.value()];
    }

    public void setManacostReduceWithin15OnHealingCast(float value) {
        rawAdditiveData[AdditiveStat.ManacostReduceWithin15OnHealingCast.value()] = value;
    }

    // == Death Knight ====================================================================================

    public float getRPp5() {
        return rawAdditiveData[AdditiveStat.RPp5.value()];
    }

    public void setRPp5(float value) {
        rawAdditiveData[AdditiveStat.RPp5.value()] = value;
    }

    public float getBonusObliterateDamage() {
        return rawAdditiveData[AdditiveStat.BonusObliterateDamage.value()];
    }

    public void setBonusObliterateDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusObliterateDamage.value()] = value;
    }

    public float getDiseasesCanCrit() {
        return rawAdditiveData[AdditiveStat.DiseasesCanCrit.value()];
    }

    public void setDiseasesCanCrit(float value) {
        rawAdditiveData[AdditiveStat.DiseasesCanCrit.value()] = value;
    }

    public float getBonusHeartStrikeMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHeartStrikeMultiplier.value()];
    }

    public void setBonusHeartStrikeMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHeartStrikeMultiplier.value()] = value;
    }

    public float getBonusScourgeStrikeMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusScourgeStrikeMultiplier.value()];
    }

    public void setBonusScourgeStrikeMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusScourgeStrikeMultiplier.value()] = value;
    }

    public float getBonusObliterateMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusObliterateMultiplier.value()];
    }

    public void setBonusObliterateMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusObliterateMultiplier.value()] = value;
    }

    public float getBonusScourgeStrikeDamage() {
        return rawAdditiveData[AdditiveStat.BonusScourgeStrikeDamage.value()];
    }

    public void setBonusScourgeStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusScourgeStrikeDamage.value()] = value;
    }

    public float getBonusDeathCoilDamage() {
        return rawAdditiveData[AdditiveStat.BonusDeathCoilDamage.value()];
    }

    public void setBonusDeathCoilDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusDeathCoilDamage.value()] = value;
    }

    public float getBonusFrostStrikeDamage() {
        return rawAdditiveData[AdditiveStat.BonusFrostStrikeDamage.value()];
    }

    public void setBonusFrostStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusFrostStrikeDamage.value()] = value;
    }

    public float getBonusHowlingBlastDamage() {
        return rawAdditiveData[AdditiveStat.BonusHowlingBlastDamage.value()];
    }

    public void setBonusHowlingBlastDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusHowlingBlastDamage.value()] = value;
    }

    public float getBonusFrostWeaponDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusFrostWeaponDamage.value()];
    }

    public void setBonusFrostWeaponDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusFrostWeaponDamage.value()] = value;
    }

    public float getCinderglacierProc() {
        return rawAdditiveData[AdditiveStat.CinderglacierProc.value()];
    }

    public void setCinderglacierProc(float value) {
        rawAdditiveData[AdditiveStat.CinderglacierProc.value()] = value;
    }

    public float getBonusDeathStrikeDamage() {
        return rawAdditiveData[AdditiveStat.BonusDeathStrikeDamage.value()];
    }

    public void setBonusDeathStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusDeathStrikeDamage.value()] = value;
    }

    public float getBonusIcyTouchDamage() {
        return rawAdditiveData[AdditiveStat.BonusIcyTouchDamage.value()];
    }

    public void setBonusIcyTouchDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusIcyTouchDamage.value()] = value;
    }

    public float getBonusAntiMagicShellDamageReduction() {
        return rawAdditiveData[AdditiveStat.BonusAntiMagicShellDamageReduction.value()];
    }

    public void setBonusAntiMagicShellDamageReduction(float value) {
        rawAdditiveData[AdditiveStat.BonusAntiMagicShellDamageReduction.value()] = value;
    }

    public float getBonusDeathCoilCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusDeathCoilCrit.value()];
    }

    public void setBonusDeathCoilCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusDeathCoilCrit.value()] = value;
    }

    public float getBonusDeathStrikeCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusDeathStrikeCrit.value()];
    }

    public void setBonusDeathStrikeCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusDeathStrikeCrit.value()] = value;
    }

    public float getBonusFrostStrikeCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusFrostStrikeCrit.value()];
    }

    public void setBonusFrostStrikeCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusFrostStrikeCrit.value()] = value;
    }

    public float getBonusIceboundFortitudeDuration() {
        return rawAdditiveData[AdditiveStat.BonusIceboundFortitudeDuration.value()];
    }

    public void setBonusIceboundFortitudeDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusIceboundFortitudeDuration.value()] = value;
    }

    public float getBonusObliterateCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusObliterateCrit.value()];
    }

    public void setBonusObliterateCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusObliterateCrit.value()] = value;
    }

    public float getBonusPerDiseaseBloodStrikeDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseBloodStrikeDamage.value()];
    }

    public void setBonusPerDiseaseBloodStrikeDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseBloodStrikeDamage.value()] = value;
    }

    public float getBonusPerDiseaseHeartStrikeDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseHeartStrikeDamage.value()];
    }

    public void setBonusPerDiseaseHeartStrikeDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseHeartStrikeDamage.value()] = value;
    }

    public float getBonusPerDiseaseObliterateDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseObliterateDamage.value()];
    }

    public void setBonusPerDiseaseObliterateDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseObliterateDamage.value()] = value;
    }

    public float getBonusPerDiseaseScourgeStrikeDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseScourgeStrikeDamage.value()];
    }

    public void setBonusPerDiseaseScourgeStrikeDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPerDiseaseScourgeStrikeDamage.value()] = value;
    }

    public float getBonusPlagueStrikeCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPlagueStrikeCrit.value()];
    }

    public void setBonusPlagueStrikeCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPlagueStrikeCrit.value()] = value;
    }

    public float getBonusRPFromDeathStrike() {
        return rawAdditiveData[AdditiveStat.BonusRPFromDeathStrike.value()];
    }

    public void setBonusRPFromDeathStrike(float value) {
        rawAdditiveData[AdditiveStat.BonusRPFromDeathStrike.value()] = value;
    }

    public float getBonusRPFromObliterate() {
        return rawAdditiveData[AdditiveStat.BonusRPFromObliterate.value()];
    }

    public void setBonusRPFromObliterate(float value) {
        rawAdditiveData[AdditiveStat.BonusRPFromObliterate.value()] = value;
    }

    public float getBonusRPFromScourgeStrike() {
        return rawAdditiveData[AdditiveStat.BonusRPFromScourgeStrike.value()];
    }

    public void setBonusRPFromScourgeStrike(float value) {
        rawAdditiveData[AdditiveStat.BonusRPFromScourgeStrike.value()] = value;
    }

    public float getBonusRuneStrikeMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusRuneStrikeMultiplier.value()];
    }

    public void setBonusRuneStrikeMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusRuneStrikeMultiplier.value()] = value;
    }

    public float getBonusBloodStrikeDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusBloodStrikeDamageMultiplier.value()];
    }

    public void setBonusBloodStrikeDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusBloodStrikeDamageMultiplier.value()] = value;
    }

    public float getBonusHeartStrikeDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHeartStrikeDamageMultiplier.value()];
    }

    public void setBonusHeartStrikeDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHeartStrikeDamageMultiplier.value()] = value;
    }

    public float getBonusScourgeStrikeCrit() {
        return rawMultiplicativeData[MultiplicativeStat.BonusScourgeStrikeCrit.value()];
    }

    public void setBonusScourgeStrikeCrit(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusScourgeStrikeCrit.value()] = value;
    }

    public float getTankDK_T10_2pc() {
        return rawAdditiveData[AdditiveStat.TankDK_T10_2pc.value()];
    }

    public void setTankDK_T10_2pc(float value) {
        rawAdditiveData[AdditiveStat.TankDK_T10_2pc.value()] = value;
    }

    public float getTankDK_T10_4pc() {
        return rawAdditiveData[AdditiveStat.TankDK_T10_4pc.value()];
    }

    public void setTankDK_T10_4pc(float value) {
        rawAdditiveData[AdditiveStat.TankDK_T10_4pc.value()] = value;
    }

    // ############################ Druid ############################

    // == Feral ===========================================================================================

    public float getTerrorProc() {
        return rawAdditiveData[AdditiveStat.TerrorProc.value()];
    }

    public void setTerrorProc(float value) {
        rawAdditiveData[AdditiveStat.TerrorProc.value()] = value;
    }

    public float getMangleCatCostReduction() {
        return rawAdditiveData[AdditiveStat.MangleCostReduction.value()];
    }

    public void setMangleCatCostReduction(float value) {
        rawAdditiveData[AdditiveStat.MangleCostReduction.value()] = value;
    }

    public float getRipCostReduction() {
        return rawAdditiveData[AdditiveStat.RipCostReduction.value()];
    }

    public void setRipCostReduction(float value) {
        rawAdditiveData[AdditiveStat.RipCostReduction.value()] = value;
    }

    public float getBonusShredDamage() {
        return rawAdditiveData[AdditiveStat.BonusShredDamage.value()];
    }

    public void setBonusShredDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusShredDamage.value()] = value;
    }

    public float getBonusRipDamagePerCPPerTick() {
        return rawAdditiveData[AdditiveStat.BonusRipDamagePerCPPerTick.value()];
    }

    public void setBonusRipDamagePerCPPerTick(float value) {
        rawAdditiveData[AdditiveStat.BonusRipDamagePerCPPerTick.value()] = value;
    }

    public float getBonusRakeDuration() {
        return rawAdditiveData[AdditiveStat.BonusRakeDuration.value()];
    }

    public void setBonusRakeDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusRakeDuration.value()] = value;
    }

    public float getBonusRipDuration() {
        return rawAdditiveData[AdditiveStat.BonusRipDuration.value()];
    }

    public void setBonusRipDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusRipDuration.value()] = value;
    }

    public float getBonusSavageRoarDuration() {
        return rawAdditiveData[AdditiveStat.BonusSavageRoarDuration.value()];
    }

    public void setBonusSavageRoarDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusSavageRoarDuration.value()] = value;
    }

    public float getClearcastOnBleedChance() {
        return rawAdditiveData[AdditiveStat.ClearcastOnBleedChance.value()];
    }

    public void setClearcastOnBleedChance(float value) {
        rawAdditiveData[AdditiveStat.ClearcastOnBleedChance.value()] = value;
    }

    public float getCatFormStrength() {
        return rawAdditiveData[AdditiveStat.CatFormStrength.value()];
    }

    public void setCatFormStrength(float value) {
        rawAdditiveData[AdditiveStat.CatFormStrength.value()] = value;
    }

    public float getBonusMangleBearThreat() {
        return rawMultiplicativeData[MultiplicativeStat.BonusMangleBearThreat.value()];
    }

    public void setBonusMangleBearThreat(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusMangleBearThreat.value()] = value;
    }

    public float getBonusLacerateDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusLacerateDamageMultiplier.value()];
    }

    public void setBonusLacerateDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusLacerateDamageMultiplier.value()] = value;
    }

    public float getBonusRipDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusRipDamageMultiplier.value()];
    }

    public void setBonusRipDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusRipDamageMultiplier.value()] = value;
    }

    // --- SingleSourceStat members: begin (13 accessor pairs, one per SingleSourceStat value) ------
    // Model constants set once per calculation by the Cat/Bear models from talents; merge operations
    // never combine them (see SingleSourceStat for the full contract). BonusMangleDamageMultiplier
    // is shared by both models, each setting it on its own Stats object.

    public float getRakeCostReduction() {
        return rawSingleSourceData[SingleSourceStat.RakeCostReduction.value()];
    }

    public void setRakeCostReduction(float value) {
        rawSingleSourceData[SingleSourceStat.RakeCostReduction.value()] = value;
    }

    public float getShredCostReduction() {
        return rawSingleSourceData[SingleSourceStat.ShredCostReduction.value()];
    }

    public void setShredCostReduction(float value) {
        rawSingleSourceData[SingleSourceStat.ShredCostReduction.value()] = value;
    }

    public float getBonusCPOnCrit() {
        return rawSingleSourceData[SingleSourceStat.BonusCPOnCrit.value()];
    }

    public void setBonusCPOnCrit(float value) {
        rawSingleSourceData[SingleSourceStat.BonusCPOnCrit.value()] = value;
    }

    public float getBonusEnergyOnTigersFury() {
        return rawSingleSourceData[SingleSourceStat.BonusEnergyOnTigersFury.value()];
    }

    public void setBonusEnergyOnTigersFury(float value) {
        rawSingleSourceData[SingleSourceStat.BonusEnergyOnTigersFury.value()] = value;
    }

    public float getFinisherEnergyOnAvoid() {
        return rawSingleSourceData[SingleSourceStat.FinisherEnergyOnAvoid.value()];
    }

    public void setFinisherEnergyOnAvoid(float value) {
        rawSingleSourceData[SingleSourceStat.FinisherEnergyOnAvoid.value()] = value;
    }

    public float getBonusMangleDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusMangleDamageMultiplier.value()];
    }

    public void setBonusMangleDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusMangleDamageMultiplier.value()] = value;
    }

    public float getBonusRakeDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusRakeDamageMultiplier.value()];
    }

    public void setBonusRakeDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusRakeDamageMultiplier.value()] = value;
    }

    public float getBonusShredDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusShredDamageMultiplier.value()];
    }

    public void setBonusShredDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusShredDamageMultiplier.value()] = value;
    }

    public float getBonusFerociousBiteDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusFerociousBiteDamageMultiplier.value()];
    }

    public void setBonusFerociousBiteDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusFerociousBiteDamageMultiplier.value()] = value;
    }

    public float getBonusMaulDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusMaulDamageMultiplier.value()];
    }

    public void setBonusMaulDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusMaulDamageMultiplier.value()] = value;
    }

    public float getBonusEnrageDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusEnrageDamageMultiplier.value()];
    }

    public void setBonusEnrageDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusEnrageDamageMultiplier.value()] = value;
    }

    public float getMangleCooldownReduction() {
        return rawSingleSourceData[SingleSourceStat.MangleCooldownReduction.value()];
    }

    public void setMangleCooldownReduction(float value) {
        rawSingleSourceData[SingleSourceStat.MangleCooldownReduction.value()] = value;
    }

    public float getBonusSwipeDamageMultiplier() {
        return rawSingleSourceData[SingleSourceStat.BonusSwipeDamageMultiplier.value()];
    }

    public void setBonusSwipeDamageMultiplier(float value) {
        rawSingleSourceData[SingleSourceStat.BonusSwipeDamageMultiplier.value()] = value;
    }

    // --- SingleSourceStat members: end ------------------------------------------------------------

    public float getBonusRipCrit() {
        return rawAdditiveData[AdditiveStat.BonusRipCrit.value()];
    }

    public void setBonusRipCrit(float value) {
        rawAdditiveData[AdditiveStat.BonusRipCrit.value()] = value;
    }

    public float getBonusBearSwipeDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusBearSwipeDamageMultiplier.value()];
    }

    public void setBonusBearSwipeDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusBearSwipeDamageMultiplier.value()] = value;
    }

    public float getBonusRakeCrit() {
        return rawAdditiveData[AdditiveStat.BonusRakeCrit.value()];
    }

    public void setBonusRakeCrit(float value) {
        rawAdditiveData[AdditiveStat.BonusRakeCrit.value()] = value;
    }

    public float getBonusFerociousBiteCrit() {
        return rawAdditiveData[AdditiveStat.BonusFerociousBiteCrit.value()];
    }

    public void setBonusFerociousBiteCrit(float value) {
        rawAdditiveData[AdditiveStat.BonusFerociousBiteCrit.value()] = value;
    }

    public float getTigersFuryCooldownReduction() {
        return rawAdditiveData[AdditiveStat.TigersFuryCooldownReduction.value()];
    }

    public void setTigersFuryCooldownReduction(float value) {
        rawAdditiveData[AdditiveStat.TigersFuryCooldownReduction.value()] = value;
    }

    // == Moonkin =========================================================================================

    public float getStarfireDmg() {
        return rawAdditiveData[AdditiveStat.StarfireDmg.value()];
    }

    public void setStarfireDmg(float value) {
        rawAdditiveData[AdditiveStat.StarfireDmg.value()] = value;
    }

    public float getUnseenMoonDamageBonus() {
        return rawAdditiveData[AdditiveStat.UnseenMoonDamageBonus.value()];
    }

    public void setUnseenMoonDamageBonus(float value) {
        rawAdditiveData[AdditiveStat.UnseenMoonDamageBonus.value()] = value;
    }

    public float getInsectSwarmDmg() {
        return rawAdditiveData[AdditiveStat.InsectSwarmDmg.value()];
    }

    public void setInsectSwarmDmg(float value) {
        rawAdditiveData[AdditiveStat.InsectSwarmDmg.value()] = value;
    }

    public float getMoonfireDmg() {
        return rawAdditiveData[AdditiveStat.MoonfireDmg.value()];
    }

    public void setMoonfireDmg(float value) {
        rawAdditiveData[AdditiveStat.MoonfireDmg.value()] = value;
    }

    public float getWrathDmg() {
        return rawAdditiveData[AdditiveStat.WrathDmg.value()];
    }

    public void setWrathDmg(float value) {
        rawAdditiveData[AdditiveStat.WrathDmg.value()] = value;
    }

    public float getInnervateCooldownReduction() {
        return rawAdditiveData[AdditiveStat.InnervateCooldownReduction.value()];
    }

    public void setInnervateCooldownReduction(float value) {
        rawAdditiveData[AdditiveStat.InnervateCooldownReduction.value()] = value;
    }

    public float getStarfireBonusWithDot() {
        return rawAdditiveData[AdditiveStat.StarfireBonusWithDot.value()];
    }

    public void setStarfireBonusWithDot(float value) {
        rawAdditiveData[AdditiveStat.StarfireBonusWithDot.value()] = value;
    }

    public float getMoonfireExtension() {
        return rawAdditiveData[AdditiveStat.MoonfireExtension.value()];
    }

    public void setMoonfireExtension(float value) {
        rawAdditiveData[AdditiveStat.MoonfireExtension.value()] = value;
    }

    public float getStarfireCritChance() {
        return rawAdditiveData[AdditiveStat.StarfireCritChance.value()];
    }

    public void setStarfireCritChance(float value) {
        rawAdditiveData[AdditiveStat.StarfireCritChance.value()] = value;
    }

    public float getBonusInsectSwarmDamage() {
        return rawAdditiveData[AdditiveStat.BonusInsectSwarmDamage.value()];
    }

    public void setBonusInsectSwarmDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusInsectSwarmDamage.value()] = value;
    }

    public float getBonusNukeCritChance() {
        return rawAdditiveData[AdditiveStat.BonusNukeCritChance.value()];
    }

    public void setBonusNukeCritChance(float value) {
        rawAdditiveData[AdditiveStat.BonusNukeCritChance.value()] = value;
    }

    public float getEclipseBonus() {
        return rawAdditiveData[AdditiveStat.EclipseBonus.value()];
    }

    public void setEclipseBonus(float value) {
        rawAdditiveData[AdditiveStat.EclipseBonus.value()] = value;
    }

    public float getStarfireProc() {
        return rawAdditiveData[AdditiveStat.StarfireProc.value()];
    }

    public void setStarfireProc(float value) {
        rawAdditiveData[AdditiveStat.StarfireProc.value()] = value;
    }

    public float getMoonfireDotCrit() {
        return rawAdditiveData[AdditiveStat.MoonfireDotCrit.value()];
    }

    public void setMoonfireDotCrit(float value) {
        rawAdditiveData[AdditiveStat.MoonfireDotCrit.value()] = value;
    }

    public float getBonusMoonkinNukeDamage() {
        return rawAdditiveData[AdditiveStat.BonusMoonkinNukeDamage.value()];
    }

    public void setBonusMoonkinNukeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusMoonkinNukeDamage.value()] = value;
    }

    public float getMoonkinT10CritDot() {
        return rawAdditiveData[AdditiveStat.MoonkinT10CritDot.value()];
    }

    public void setMoonkinT10CritDot(float value) {
        rawAdditiveData[AdditiveStat.MoonkinT10CritDot.value()] = value;
    }

    // == Tree ============================================================================================

    public float getLifebloomCostReduction() {
        return rawAdditiveData[AdditiveStat.LifebloomCostReduction.value()];
    }

    public void setLifebloomCostReduction(float value) {
        rawAdditiveData[AdditiveStat.LifebloomCostReduction.value()] = value;
    }

    public float getNourishBonusPerHoT() {
        return rawAdditiveData[AdditiveStat.NourishBonusPerHoT.value()];
    }

    public void setNourishBonusPerHoT(float value) {
        rawAdditiveData[AdditiveStat.NourishBonusPerHoT.value()] = value;
    }

    public float getSwiftmendBonus() {
        return rawAdditiveData[AdditiveStat.SwiftmendBonus.value()];
    }

    public void setSwiftmendBonus(float value) {
        rawAdditiveData[AdditiveStat.SwiftmendBonus.value()] = value;
    }

    public float getRejuvenationInstantTick() {
        return rawAdditiveData[AdditiveStat.RejuvenationInstantTick.value()];
    }

    public void setRejuvenationInstantTick(float value) {
        rawAdditiveData[AdditiveStat.RejuvenationInstantTick.value()] = value;
    }

    public float getNourishCritBonus() {
        return rawAdditiveData[AdditiveStat.NourishCritBonus.value()];
    }

    public void setNourishCritBonus(float value) {
        rawAdditiveData[AdditiveStat.NourishCritBonus.value()] = value;
    }

    public float getRejuvenationCrit() {
        return rawAdditiveData[AdditiveStat.RejuvenationCrit.value()];
    }

    public void setRejuvenationCrit(float value) {
        rawAdditiveData[AdditiveStat.RejuvenationCrit.value()] = value;
    }

    public float getWildGrowthLessReduction() {
        return rawAdditiveData[AdditiveStat.WildGrowthLessReduction.value()];
    }

    public void setWildGrowthLessReduction(float value) {
        rawAdditiveData[AdditiveStat.WildGrowthLessReduction.value()] = value;
    }

    public float getRejuvJumpChance() {
        return rawAdditiveData[AdditiveStat.RejuvJumpChance.value()];
    }

    public void setRejuvJumpChance(float value) {
        rawAdditiveData[AdditiveStat.RejuvJumpChance.value()] = value;
    }

    public float getRejuvenationHealBonus() {
        return rawAdditiveData[AdditiveStat.RejuvenationHealBonus.value()];
    }

    public void setRejuvenationHealBonus(float value) {
        rawAdditiveData[AdditiveStat.RejuvenationHealBonus.value()] = value;
    }

    public float getReduceRejuvenationCost() {
        return rawAdditiveData[AdditiveStat.ReduceRejuvenationCost.value()];
    }

    public void setReduceRejuvenationCost(float value) {
        rawAdditiveData[AdditiveStat.ReduceRejuvenationCost.value()] = value;
    }

    public float getLifebloomTickHealBonus() {
        return rawAdditiveData[AdditiveStat.LifebloomTickHealBonus.value()];
    }

    public void setLifebloomTickHealBonus(float value) {
        rawAdditiveData[AdditiveStat.LifebloomTickHealBonus.value()] = value;
    }

    public float getHealingTouchFinalHealBonus() {
        return rawAdditiveData[AdditiveStat.HealingTouchFinalHealBonus.value()];
    }

    public void setHealingTouchFinalHealBonus(float value) {
        rawAdditiveData[AdditiveStat.HealingTouchFinalHealBonus.value()] = value;
    }

    public float getNourishSpellpower() {
        return rawAdditiveData[AdditiveStat.NourishSpellpower.value()];
    }

    public void setNourishSpellpower(float value) {
        rawAdditiveData[AdditiveStat.NourishSpellpower.value()] = value;
    }

    public float getSwiftmendCdReduc() {
        return rawAdditiveData[AdditiveStat.SwiftmendCdReduc.value()];
    }

    public void setSwiftmendCdReduc(float value) {
        rawAdditiveData[AdditiveStat.SwiftmendCdReduc.value()] = value;
    }

    public float getRevitalizeChance() {
        return rawAdditiveData[AdditiveStat.RevitalizeChance.value()];
    }

    public void setRevitalizeChance(float value) {
        rawAdditiveData[AdditiveStat.RevitalizeChance.value()] = value;
    }

    // == Hunter ==========================================================================================

    public float getRangedCritRating() {
        return rawAdditiveData[AdditiveStat.RangedCritRating.value()];
    }

    public void setRangedCritRating(float value) {
        rawAdditiveData[AdditiveStat.RangedCritRating.value()] = value;
    }

    public float getRangedHitRating() {
        return rawAdditiveData[AdditiveStat.RangedHitRating.value()];
    }

    public void setRangedHitRating(float value) {
        rawAdditiveData[AdditiveStat.RangedHitRating.value()] = value;
    }

    public float getRangedHasteRating() {
        return rawAdditiveData[AdditiveStat.RangedHasteRating.value()];
    }

    public void setRangedHasteRating(float value) {
        rawAdditiveData[AdditiveStat.RangedHasteRating.value()] = value;
    }

    public float getRangedAttackPower() {
        return rawAdditiveData[AdditiveStat.RangedAttackPower.value()];
    }

    public void setRangedAttackPower(float value) {
        rawAdditiveData[AdditiveStat.RangedAttackPower.value()] = value;
    }

    public float getBonusRangedAttackPowerMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusRangedAttackPowerMultiplier.value()];
    }

    public void setBonusRangedAttackPowerMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusRangedAttackPowerMultiplier.value()] = value;
    }

    public float getBonusPetAttackPowerMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPetAttackPowerMultiplier.value()];
    }

    public void setBonusPetAttackPowerMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPetAttackPowerMultiplier.value()] = value;
    }

    public float getBonusHunter_T7_4P_ViperSpeed() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHunter_T7_4P_ViperSpeed.value()];
    }

    public void setBonusHunter_T7_4P_ViperSpeed(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHunter_T7_4P_ViperSpeed.value()] = value;
    }

    public float getBonusHunter_T8_2P_SerpDmg() {
        return rawMultiplicativeData[MultiplicativeStat.BonusHunter_T8_2P_SerpDmg.value()];
    }

    public void setBonusHunter_T8_2P_SerpDmg(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusHunter_T8_2P_SerpDmg.value()] = value;
    }

    public float getBonusHunter_PvP_4pc() {
        return rawAdditiveData[AdditiveStat.BonusHunter_PvP_4pc.value()];
    }

    public void setBonusHunter_PvP_4pc(float value) {
        rawAdditiveData[AdditiveStat.BonusHunter_PvP_4pc.value()] = value;
    }

    public float getBonusHunter_T9_2P_SerpCanCrit() {
        return rawAdditiveData[AdditiveStat.BonusHunter_T9_2P_SerpCanCrit.value()];
    }

    public void setBonusHunter_T9_2P_SerpCanCrit(float value) {
        rawAdditiveData[AdditiveStat.BonusHunter_T9_2P_SerpCanCrit.value()] = value;
    }

    public float getBonusHunter_T9_4P_SteadyShotPetAPProc() {
        return rawAdditiveData[AdditiveStat.BonusHunter_T9_4P_SteadyShotPetAPProc.value()];
    }

    public void setBonusHunter_T9_4P_SteadyShotPetAPProc(float value) {
        rawAdditiveData[AdditiveStat.BonusHunter_T9_4P_SteadyShotPetAPProc.value()] = value;
    }

    public float getBonusPetCritChance() {
        return rawAdditiveData[AdditiveStat.BonusPetCritChance.value()];
    }

    public void setBonusPetCritChance(float value) {
        rawAdditiveData[AdditiveStat.BonusPetCritChance.value()] = value;
    }

    public float getPetStamina() {
        return rawAdditiveData[AdditiveStat.PetStamina.value()];
    }

    public void setPetStamina(float value) {
        rawAdditiveData[AdditiveStat.PetStamina.value()] = value;
    }

    public float getPetStrength() {
        return rawAdditiveData[AdditiveStat.PetStrength.value()];
    }

    public void setPetStrength(float value) {
        rawAdditiveData[AdditiveStat.PetStrength.value()] = value;
    }

    public float getPetAttackPower() {
        return rawAdditiveData[AdditiveStat.PetAttackPower.value()];
    }

    public void setPetAttackPower(float value) {
        rawAdditiveData[AdditiveStat.PetAttackPower.value()] = value;
    }

    public float getPetSpirit() {
        return rawAdditiveData[AdditiveStat.PetSpirit.value()];
    }

    public void setPetSpirit(float value) {
        rawAdditiveData[AdditiveStat.PetSpirit.value()] = value;
    }

    public float getBonusPetDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPetDamageMultiplier.value()];
    }

    public void setBonusPetDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPetDamageMultiplier.value()] = value;
    }

    // == Mage ============================================================================================

    public float getMage4T8() {
        return rawAdditiveData[AdditiveStat.Mage4T8.value()];
    }

    public void setMage4T8(float value) {
        rawAdditiveData[AdditiveStat.Mage4T8.value()] = value;
    }

    public float getMage2T9() {
        return rawAdditiveData[AdditiveStat.Mage2T9.value()];
    }

    public void setMage2T9(float value) {
        rawAdditiveData[AdditiveStat.Mage2T9.value()] = value;
    }

    public float getMage4T9() {
        return rawAdditiveData[AdditiveStat.Mage4T9.value()];
    }

    public void setMage4T9(float value) {
        rawAdditiveData[AdditiveStat.Mage4T9.value()] = value;
    }

    public float getMage2T10() {
        return rawAdditiveData[AdditiveStat.Mage2T10.value()];
    }

    public void setMage2T10(float value) {
        rawAdditiveData[AdditiveStat.Mage2T10.value()] = value;
    }

    public float getMage4T10() {
        return rawAdditiveData[AdditiveStat.Mage4T10.value()];
    }

    public void setMage4T10(float value) {
        rawAdditiveData[AdditiveStat.Mage4T10.value()] = value;
    }

    public float getBonusManaGem() {
        return rawAdditiveData[AdditiveStat.BonusManaGem.value()];
    }

    public void setBonusManaGem(float value) {
        rawAdditiveData[AdditiveStat.BonusManaGem.value()] = value;
    }

    public float getMageIceArmor() {
        return rawAdditiveData[AdditiveStat.MageIceArmor.value()];
    }

    public void setMageIceArmor(float value) {
        rawAdditiveData[AdditiveStat.MageIceArmor.value()] = value;
    }

    public float getMageMageArmor() {
        return rawAdditiveData[AdditiveStat.MageMageArmor.value()];
    }

    public void setMageMageArmor(float value) {
        rawAdditiveData[AdditiveStat.MageMageArmor.value()] = value;
    }

    public float getMageMoltenArmor() {
        return rawAdditiveData[AdditiveStat.MageMoltenArmor.value()];
    }

    public void setMageMoltenArmor(float value) {
        rawAdditiveData[AdditiveStat.MageMoltenArmor.value()] = value;
    }

    // ############################ Paladin ############################

    // == Healadin ========================================================================================

    public float getFlashOfLightSpellPower() {
        return rawAdditiveData[AdditiveStat.FlashOfLightSpellPower.value()];
    }

    public void setFlashOfLightSpellPower(float value) {
        rawAdditiveData[AdditiveStat.FlashOfLightSpellPower.value()] = value;
    }

    public float getSacredShieldICDReduction() {
        return rawAdditiveData[AdditiveStat.SacredShieldICDReduction.value()];
    }

    public void setSacredShieldICDReduction(float value) {
        rawAdditiveData[AdditiveStat.SacredShieldICDReduction.value()] = value;
    }

    public float getHolyShockHoTOnCrit() {
        return rawAdditiveData[AdditiveStat.HolyShockHoTOnCrit.value()];
    }

    public void setHolyShockHoTOnCrit(float value) {
        rawAdditiveData[AdditiveStat.HolyShockHoTOnCrit.value()] = value;
    }

    public float getFlashOfLightMultiplier() {
        return rawAdditiveData[AdditiveStat.FlashOfLightMultiplier.value()];
    }

    public void setFlashOfLightMultiplier(float value) {
        rawAdditiveData[AdditiveStat.FlashOfLightMultiplier.value()] = value;
    }

    public float getFlashOfLightCrit() {
        return rawAdditiveData[AdditiveStat.FlashOfLightCrit.value()];
    }

    public void setFlashOfLightCrit(float value) {
        rawAdditiveData[AdditiveStat.FlashOfLightCrit.value()] = value;
    }

    public float getHolyLightSpellPower() {
        return rawAdditiveData[AdditiveStat.HolyLightSpellPower.value()];
    }

    public void setHolyLightSpellPower(float value) {
        rawAdditiveData[AdditiveStat.HolyLightSpellPower.value()] = value;
    }

    public float getHolyLightPercentManaReduction() {
        return rawAdditiveData[AdditiveStat.HolyLightPercentManaReduction.value()];
    }

    public void setHolyLightPercentManaReduction(float value) {
        rawAdditiveData[AdditiveStat.HolyLightPercentManaReduction.value()] = value;
    }

    public float getHolyLightManaCostReduction() {
        return rawAdditiveData[AdditiveStat.HolyLightManaReduction.value()];
    }

    public void setHolyLightManaCostReduction(float value) {
        rawAdditiveData[AdditiveStat.HolyLightManaReduction.value()] = value;
    }

    public float getHolyLightCrit() {
        return rawAdditiveData[AdditiveStat.HolyLightCrit.value()];
    }

    public void setHolyLightCrit(float value) {
        rawAdditiveData[AdditiveStat.HolyLightCrit.value()] = value;
    }

    public float getHolyShockCrit() {
        return rawAdditiveData[AdditiveStat.HolyShockCrit.value()];
    }

    public void setHolyShockCrit(float value) {
        rawAdditiveData[AdditiveStat.HolyShockCrit.value()] = value;
    }

    public float getBonusJudgementDuration() {
        return rawAdditiveData[AdditiveStat.BonusJudgementDuration.value()];
    }

    public void setBonusJudgementDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusJudgementDuration.value()] = value;
    }

    public float getFlashOfLightHoTMultiplier() {
        return rawAdditiveData[AdditiveStat.FlashOfLightHoTMultiplier.value()];
    }

    public void setFlashOfLightHoTMultiplier(float value) {
        rawAdditiveData[AdditiveStat.FlashOfLightHoTMultiplier.value()] = value;
    }

    public float getDivineIlluminationHealingMultiplier() {
        return rawAdditiveData[AdditiveStat.DivineIlluminationHealingMultiplier.value()];
    }

    public void setDivineIlluminationHealingMultiplier(float value) {
        rawAdditiveData[AdditiveStat.DivineIlluminationHealingMultiplier.value()] = value;
    }

    public float getHolyLightCastTimeReductionFromHolyShock() {
        return rawAdditiveData[AdditiveStat.HolyLightCastTimeReductionFromHolyShock.value()];
    }

    public void setHolyLightCastTimeReductionFromHolyShock(float value) {
        rawAdditiveData[AdditiveStat.HolyLightCastTimeReductionFromHolyShock.value()] = value;
    }

    // == ProtPaladin =====================================================================================

    public float getJudgementBlockValue() {
        return rawAdditiveData[AdditiveStat.JudgementBlockValue.value()];
    }

    public void setJudgementBlockValue(float value) {
        rawAdditiveData[AdditiveStat.JudgementBlockValue.value()] = value;
    }

    public float getShieldOfRighteousnessBlockValue() {
        return rawAdditiveData[AdditiveStat.ShieldOfRighteousnessBlockValue.value()];
    }

    public void setShieldOfRighteousnessBlockValue(float value) {
        rawAdditiveData[AdditiveStat.ShieldOfRighteousnessBlockValue.value()] = value;
    }

    public float getBonusHammerOfTheRighteousMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.HammerOfTheRighteousMultiplier.value()];
    }

    public void setBonusHammerOfTheRighteousMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.HammerOfTheRighteousMultiplier.value()] = value;
    }

    public float getBonusSealOfCorruptionDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSealOfCorruptionDamageMultiplier.value()];
    }

    public void setBonusSealOfCorruptionDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSealOfCorruptionDamageMultiplier.value()] = value;
    }

    public float getBonusSealOfRighteousnessDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSealOfRighteousnessDamageMultiplier.value()];
    }

    public void setBonusSealOfRighteousnessDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSealOfRighteousnessDamageMultiplier.value()] = value;
    }

    public float getBonusSealOfVengeanceDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSealOfVengeanceDamageMultiplier.value()];
    }

    public void setBonusSealOfVengeanceDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSealOfVengeanceDamageMultiplier.value()] = value;
    }

    public float getBonusShieldOfRighteousnessDamage() {
        return rawAdditiveData[AdditiveStat.BonusShieldOfRighteousnessDamage.value()];
    }

    public void setBonusShieldOfRighteousnessDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusShieldOfRighteousnessDamage.value()] = value;
    }

    public float getDivineProtectionDurationBonus() {
        return rawAdditiveData[AdditiveStat.DivineProtectionDurationBonus.value()];
    }

    public void setDivineProtectionDurationBonus(float value) {
        rawAdditiveData[AdditiveStat.DivineProtectionDurationBonus.value()] = value;
    }

    public float getHolyShieldBlockValue() {
        return rawAdditiveData[AdditiveStat.HolyShieldBlockValue.value()];
    }

    public void setHolyShieldBlockValue(float value) {
        rawAdditiveData[AdditiveStat.HolyShieldBlockValue.value()] = value;
    }

    // == Retribution =====================================================================================

    public float getSealMultiplier() {
        return rawAdditiveData[AdditiveStat.SealMultiplier.value()];
    }

    public void setSealMultiplier(float value) {
        rawAdditiveData[AdditiveStat.SealMultiplier.value()] = value;
    }

    public float getJudgementMultiplier() {
        return rawAdditiveData[AdditiveStat.JudgementMultiplier.value()];
    }

    public void setJudgementMultiplier(float value) {
        rawAdditiveData[AdditiveStat.JudgementMultiplier.value()] = value;
    }

    public float getDivineStormRefresh() {
        return rawAdditiveData[AdditiveStat.DivineStormRefresh.value()];
    }

    public void setDivineStormRefresh(float value) {
        rawAdditiveData[AdditiveStat.DivineStormRefresh.value()] = value;
    }

    public float getCrusaderStrikeDamage() {
        return rawAdditiveData[AdditiveStat.CrusaderStrikeDamage.value()];
    }

    public void setCrusaderStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.CrusaderStrikeDamage.value()] = value;
    }

    public float getConsecrationSpellPower() {
        return rawAdditiveData[AdditiveStat.ConsecrationSpellPower.value()];
    }

    public void setConsecrationSpellPower(float value) {
        rawAdditiveData[AdditiveStat.ConsecrationSpellPower.value()] = value;
    }

    public float getDivineStormDamage() {
        return rawAdditiveData[AdditiveStat.DivineStormDamage.value()];
    }

    public void setDivineStormDamage(float value) {
        rawAdditiveData[AdditiveStat.DivineStormDamage.value()] = value;
    }

    public float getDivineStormMultiplier() {
        return rawAdditiveData[AdditiveStat.DivineStormMultiplier.value()];
    }

    public void setDivineStormMultiplier(float value) {
        rawAdditiveData[AdditiveStat.DivineStormMultiplier.value()] = value;
    }

    public float getCrusaderStrikeMultiplier() {
        return rawAdditiveData[AdditiveStat.CrusaderStrikeMultiplier.value()];
    }

    public void setCrusaderStrikeMultiplier(float value) {
        rawAdditiveData[AdditiveStat.CrusaderStrikeMultiplier.value()] = value;
    }

    public float getExorcismMultiplier() {
        return rawAdditiveData[AdditiveStat.ExorcismMultiplier.value()];
    }

    public void setExorcismMultiplier(float value) {
        rawAdditiveData[AdditiveStat.ExorcismMultiplier.value()] = value;
    }

    public float getHammerOfWrathMultiplier() {
        return rawAdditiveData[AdditiveStat.HammerOfWrathMultiplier.value()];
    }

    public void setHammerOfWrathMultiplier(float value) {
        rawAdditiveData[AdditiveStat.HammerOfWrathMultiplier.value()] = value;
    }

    public float getCrusaderStrikeCrit() {
        return rawAdditiveData[AdditiveStat.CrusaderStrikeCrit.value()];
    }

    public void setCrusaderStrikeCrit(float value) {
        rawAdditiveData[AdditiveStat.CrusaderStrikeCrit.value()] = value;
    }

    public float getDivineStormCrit() {
        return rawAdditiveData[AdditiveStat.DivineStormCrit.value()];
    }

    public void setDivineStormCrit(float value) {
        rawAdditiveData[AdditiveStat.DivineStormCrit.value()] = value;
    }

    public float getJudgementCDReduction() {
        return rawAdditiveData[AdditiveStat.JudgementCDReduction.value()];
    }

    public void setJudgementCDReduction(float value) {
        rawAdditiveData[AdditiveStat.JudgementCDReduction.value()] = value;
    }

    public float getRighteousVengeanceCanCrit() {
        return rawNoStackData[NonStackingStat.RighteousVengeanceCanCrit.value()];
    }

    public void setRighteousVengeanceCanCrit(float value) {
        rawNoStackData[NonStackingStat.RighteousVengeanceCanCrit.value()] = value;
    }

    public float getJudgementCrit() {
        return rawAdditiveData[AdditiveStat.JudgementCrit.value()];
    }

    public void setJudgementCrit(float value) {
        rawAdditiveData[AdditiveStat.JudgementCrit.value()] = value;
    }

    // == Priest ==========================================================================================

    public float getPriestInnerFire() {
        return rawAdditiveData[AdditiveStat.PriestInnerFire.value()];
    }

    public void setPriestInnerFire(float value) {
        rawAdditiveData[AdditiveStat.PriestInnerFire.value()] = value;
    }

    public float getSWPDurationIncrease() {
        return rawAdditiveData[AdditiveStat.SWPDurationIncrease.value()];
    }

    public void setSWPDurationIncrease(float value) {
        rawAdditiveData[AdditiveStat.SWPDurationIncrease.value()] = value;
    }

    public float getBonusMindBlastMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusMindBlastMultiplier.value()];
    }

    public void setBonusMindBlastMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusMindBlastMultiplier.value()] = value;
    }

    public float getMindBlastCostReduction() {
        return rawAdditiveData[AdditiveStat.MindBlastCostReduction.value()];
    }

    public void setMindBlastCostReduction(float value) {
        rawAdditiveData[AdditiveStat.MindBlastCostReduction.value()] = value;
    }

    public float getShadowWordDeathCritIncrease() {
        return rawAdditiveData[AdditiveStat.ShadowWordDeathCritIncrease.value()];
    }

    public void setShadowWordDeathCritIncrease(float value) {
        rawAdditiveData[AdditiveStat.ShadowWordDeathCritIncrease.value()] = value;
    }

    public float getDevouringPlagueBonusDamage() {
        return rawAdditiveData[AdditiveStat.DevouringPlagueBonusDamage.value()];
    }

    public void setDevouringPlagueBonusDamage(float value) {
        rawAdditiveData[AdditiveStat.DevouringPlagueBonusDamage.value()] = value;
    }

    public float getMindBlastHasteProc() {
        return rawAdditiveData[AdditiveStat.MindBlastHasteProc.value()];
    }

    public void setMindBlastHasteProc(float value) {
        rawAdditiveData[AdditiveStat.MindBlastHasteProc.value()] = value;
    }

    public float getPriestDPS_T9_2pc() {
        return rawAdditiveData[AdditiveStat.PriestDPS_T9_2pc.value()];
    }

    public void setPriestDPS_T9_2pc(float value) {
        rawAdditiveData[AdditiveStat.PriestDPS_T9_2pc.value()] = value;
    }

    public float getPriestDPS_T9_4pc() {
        return rawAdditiveData[AdditiveStat.PriestDPS_T9_4pc.value()];
    }

    public void setPriestDPS_T9_4pc(float value) {
        rawAdditiveData[AdditiveStat.PriestDPS_T9_4pc.value()] = value;
    }

    public float getPriestDPS_T10_2pc() {
        return rawAdditiveData[AdditiveStat.PriestDPS_T10_2pc.value()];
    }

    public void setPriestDPS_T10_2pc(float value) {
        rawAdditiveData[AdditiveStat.PriestDPS_T10_2pc.value()] = value;
    }

    public float getPriestDPS_T10_4pc() {
        return rawAdditiveData[AdditiveStat.PriestDPS_T10_4pc.value()];
    }

    public void setPriestDPS_T10_4pc(float value) {
        rawAdditiveData[AdditiveStat.PriestDPS_T10_4pc.value()] = value;
    }

    public float getBonusPoHManaCostReductionMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusPoHManaCostReductionMultiplier.value()];
    }

    public void setBonusPoHManaCostReductionMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusPoHManaCostReductionMultiplier.value()] = value;
    }

    public float getBonusGHHealingMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusGHHealingMultiplier.value()];
    }

    public void setBonusGHHealingMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusGHHealingMultiplier.value()] = value;
    }

    public float getRenewDurationIncrease() {
        return rawAdditiveData[AdditiveStat.RenewDurationIncrease.value()];
    }

    public void setRenewDurationIncrease(float value) {
        rawAdditiveData[AdditiveStat.RenewDurationIncrease.value()] = value;
    }

    public float getManaGainOnGreaterHealOverheal() {
        return rawAdditiveData[AdditiveStat.ManaGainOnGreaterHealOverheal.value()];
    }

    public void setManaGainOnGreaterHealOverheal(float value) {
        rawAdditiveData[AdditiveStat.ManaGainOnGreaterHealOverheal.value()] = value;
    }

    public float getPrayerOfMendingExtraJumps() {
        return rawAdditiveData[AdditiveStat.PrayerOfMendingExtraJumps.value()];
    }

    public void setPrayerOfMendingExtraJumps(float value) {
        rawAdditiveData[AdditiveStat.PrayerOfMendingExtraJumps.value()] = value;
    }

    public float getGreaterHealCostReduction() {
        return rawAdditiveData[AdditiveStat.GreaterHealCostReduction.value()];
    }

    public void setGreaterHealCostReduction(float value) {
        rawAdditiveData[AdditiveStat.GreaterHealCostReduction.value()] = value;
    }

    public float getWeakenedSoulDurationDecrease() {
        return rawAdditiveData[AdditiveStat.WeakenedSoulDurationDecrease.value()];
    }

    public void setWeakenedSoulDurationDecrease(float value) {
        rawAdditiveData[AdditiveStat.WeakenedSoulDurationDecrease.value()] = value;
    }

    public float getPrayerOfHealingExtraCrit() {
        return rawAdditiveData[AdditiveStat.PrayerOfHealingExtraCrit.value()];
    }

    public void setPrayerOfHealingExtraCrit(float value) {
        rawAdditiveData[AdditiveStat.PrayerOfHealingExtraCrit.value()] = value;
    }

    public float getPWSBonusSpellPowerProc() {
        return rawAdditiveData[AdditiveStat.PWSBonusSpellPowerProc.value()];
    }

    public void setPWSBonusSpellPowerProc(float value) {
        rawAdditiveData[AdditiveStat.PWSBonusSpellPowerProc.value()] = value;
    }

    public float getPriestHeal_T9_2pc() {
        return rawAdditiveData[AdditiveStat.PriestHeal_T9_2pc.value()];
    }

    public void setPriestHeal_T9_2pc(float value) {
        rawAdditiveData[AdditiveStat.PriestHeal_T9_2pc.value()] = value;
    }

    public float getPriestHeal_T9_4pc() {
        return rawAdditiveData[AdditiveStat.PriestHeal_T9_4pc.value()];
    }

    public void setPriestHeal_T9_4pc(float value) {
        rawAdditiveData[AdditiveStat.PriestHeal_T9_4pc.value()] = value;
    }

    public float getPriestHeal_T10_2pc() {
        return rawAdditiveData[AdditiveStat.PriestHeal_T10_2pc.value()];
    }

    public void setPriestHeal_T10_2pc(float value) {
        rawAdditiveData[AdditiveStat.PriestHeal_T10_2pc.value()] = value;
    }

    public float getPriestHeal_T10_4pc() {
        return rawAdditiveData[AdditiveStat.PriestHeal_T10_4pc.value()];
    }

    public void setPriestHeal_T10_4pc(float value) {
        rawAdditiveData[AdditiveStat.PriestHeal_T10_4pc.value()] = value;
    }

    // == Rogue ===========================================================================================

    public float getRuptureDamageBonus() {
        return rawAdditiveData[AdditiveStat.RuptureDamageBonus.value()];
    }

    public void setRuptureDamageBonus(float value) {
        rawAdditiveData[AdditiveStat.RuptureDamageBonus.value()] = value;
    }

    public float getComboMoveEnergyReduction() {
        return rawAdditiveData[AdditiveStat.ComboMoveEnergyReduction.value()];
    }

    public void setComboMoveEnergyReduction(float value) {
        rawAdditiveData[AdditiveStat.ComboMoveEnergyReduction.value()] = value;
    }

    public float getBonusEnergyFromDP() {
        return rawAdditiveData[AdditiveStat.BonusEnergyFromDP.value()];
    }

    public void setBonusEnergyFromDP(float value) {
        rawAdditiveData[AdditiveStat.BonusEnergyFromDP.value()] = value;
    }

    public float getRuptureCrit() {
        return rawAdditiveData[AdditiveStat.RuptureCrit.value()];
    }

    public void setRuptureCrit(float value) {
        rawAdditiveData[AdditiveStat.RuptureCrit.value()] = value;
    }

    public float getReduceEnergyCostFromRupture() {
        return rawAdditiveData[AdditiveStat.ReduceEnergyCostFromRupture.value()];
    }

    public void setReduceEnergyCostFromRupture(float value) {
        rawAdditiveData[AdditiveStat.ReduceEnergyCostFromRupture.value()] = value;
    }

    public float getBonusCPGCritChance() {
        return rawAdditiveData[AdditiveStat.BonusCPGCritChance.value()];
    }

    public void setBonusCPGCritChance(float value) {
        rawAdditiveData[AdditiveStat.BonusCPGCritChance.value()] = value;
    }

    public float getBonusToTTEnergy() {
        return rawAdditiveData[AdditiveStat.BonusToTTEnergy.value()];
    }

    public void setBonusToTTEnergy(float value) {
        rawAdditiveData[AdditiveStat.BonusToTTEnergy.value()] = value;
    }

    public float getChanceOn3CPOnFinisher() {
        return rawAdditiveData[AdditiveStat.ChanceOn3CPOnFinisher.value()];
    }

    public void setChanceOn3CPOnFinisher(float value) {
        rawAdditiveData[AdditiveStat.ChanceOn3CPOnFinisher.value()] = value;
    }

    // ############################ Shaman ############################

    // == Elemental =======================================================================================

    public float getBonusCritChance() {
        return rawAdditiveData[AdditiveStat.BonusCritChance.value()];
    }

    public void setBonusCritChance(float value) {
        rawAdditiveData[AdditiveStat.BonusCritChance.value()] = value;
    }

    public float getBonusLavaBurstCritDamage() {
        return rawAdditiveData[AdditiveStat.BonusLavaBurstCritDamage.value()];
    }

    public void setBonusLavaBurstCritDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusLavaBurstCritDamage.value()] = value;
    }

    public float getBonusFlameShockDuration() {
        return rawAdditiveData[AdditiveStat.BonusFlameShockDuration.value()];
    }

    public void setBonusFlameShockDuration(float value) {
        rawAdditiveData[AdditiveStat.BonusFlameShockDuration.value()] = value;
    }

    public float getBonusFlameShockDoTDamage() {
        return rawAdditiveData[AdditiveStat.BonusFlameShockDoTDamage.value()];
    }

    public void setBonusFlameShockDoTDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusFlameShockDoTDamage.value()] = value;
    }

    public float getLightningBoltCostReduction() {
        return rawAdditiveData[AdditiveStat.LightningBoltCostReduction.value()];
    }

    public void setLightningBoltCostReduction(float value) {
        rawAdditiveData[AdditiveStat.LightningBoltCostReduction.value()] = value;
    }

    public float getLightningBoltDamageModifier() {
        return rawAdditiveData[AdditiveStat.LightningBoltDamageModifier.value()];
    }

    public void setLightningBoltDamageModifier(float value) {
        rawAdditiveData[AdditiveStat.LightningBoltDamageModifier.value()] = value;
    }

    public float getLightningSpellPower() {
        return rawAdditiveData[AdditiveStat.LightningSpellPower.value()];
    }

    public void setLightningSpellPower(float value) {
        rawAdditiveData[AdditiveStat.LightningSpellPower.value()] = value;
    }

    public float getLavaBurstBonus() {
        return rawAdditiveData[AdditiveStat.LavaBurstBonus.value()];
    }

    public void setLavaBurstBonus(float value) {
        rawAdditiveData[AdditiveStat.LavaBurstBonus.value()] = value;
    }

    public float getLightningBoltCritDamageModifier() {
        return rawAdditiveData[AdditiveStat.LightningBoltCritDamageModifier.value()];
    }

    public void setLightningBoltCritDamageModifier(float value) {
        rawAdditiveData[AdditiveStat.LightningBoltCritDamageModifier.value()] = value;
    }

    public float getBonusLavaBurstDamageMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusLavaBurstDamageMultiplier.value()];
    }

    public void setBonusLavaBurstDamageMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusLavaBurstDamageMultiplier.value()] = value;
    }

    public float getElemental2T10() {
        return rawAdditiveData[AdditiveStat.Elemental2T10.value()];
    }

    public void setElemental2T10(float value) {
        rawAdditiveData[AdditiveStat.Elemental2T10.value()] = value;
    }

    public float getElemental4T10() {
        return rawAdditiveData[AdditiveStat.Elemental4T10.value()];
    }

    public void setElemental4T10(float value) {
        rawAdditiveData[AdditiveStat.Elemental4T10.value()] = value;
    }

    // == Enhance =========================================================================================

    public float getBonusSSDamage() {
        return rawAdditiveData[AdditiveStat.BonusSSDamage.value()];
    }

    public void setBonusSSDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusSSDamage.value()] = value;
    }

    public float getBonusWFAttackPower() {
        return rawAdditiveData[AdditiveStat.BonusWFAttackPower.value()];
    }

    public void setBonusWFAttackPower(float value) {
        rawAdditiveData[AdditiveStat.BonusWFAttackPower.value()] = value;
    }

    public float getEnhance4T7() {
        return rawAdditiveData[AdditiveStat.Enhance4T7.value()];
    }

    public void setEnhance4T7(float value) {
        rawAdditiveData[AdditiveStat.Enhance4T7.value()] = value;
    }

    public float getEnhance2T7() {
        return rawAdditiveData[AdditiveStat.Enhance2T7.value()];
    }

    public void setEnhance2T7(float value) {
        rawAdditiveData[AdditiveStat.Enhance2T7.value()] = value;
    }

    public float getEnhance2T8() {
        return rawAdditiveData[AdditiveStat.Enhance2T8.value()];
    }

    public void setEnhance2T8(float value) {
        rawAdditiveData[AdditiveStat.Enhance2T8.value()] = value;
    }

    public float getEnhance4T8() {
        return rawAdditiveData[AdditiveStat.Enhance4T8.value()];
    }

    public void setEnhance4T8(float value) {
        rawAdditiveData[AdditiveStat.Enhance4T8.value()] = value;
    }

    public float getEnhance2T9() {
        return rawAdditiveData[AdditiveStat.Enhance2T9.value()];
    }

    public void setEnhance2T9(float value) {
        rawAdditiveData[AdditiveStat.Enhance2T9.value()] = value;
    }

    public float getEnhance4T9() {
        return rawAdditiveData[AdditiveStat.Enhance4T9.value()];
    }

    public void setEnhance4T9(float value) {
        rawAdditiveData[AdditiveStat.Enhance4T9.value()] = value;
    }

    public float getEnhance2T10() {
        return rawAdditiveData[AdditiveStat.Enhance2T10.value()];
    }

    public void setEnhance2T10(float value) {
        rawAdditiveData[AdditiveStat.Enhance2T10.value()] = value;
    }

    public float getEnhance4T10() {
        return rawAdditiveData[AdditiveStat.Enhance4T10.value()];
    }

    public void setEnhance4T10(float value) {
        rawAdditiveData[AdditiveStat.Enhance4T10.value()] = value;
    }

    // == RestoSham =======================================================================================

    public float getManaSpringMp5Increase() {
        return rawAdditiveData[AdditiveStat.ManaSpringMp5Increase.value()];
    }

    public void setManaSpringMp5Increase(float value) {
        rawAdditiveData[AdditiveStat.ManaSpringMp5Increase.value()] = value;
    }

    public float getRTCDDecrease() {
        return rawAdditiveData[AdditiveStat.RTCDDecrease.value()];
    }

    public void setRTCDDecrease(float value) {
        rawAdditiveData[AdditiveStat.RTCDDecrease.value()] = value;
    }

    public float getCHCTDecrease() {
        return rawAdditiveData[AdditiveStat.CHCTDecrease.value()];
    }

    public void setCHCTDecrease(float value) {
        rawAdditiveData[AdditiveStat.CHCTDecrease.value()] = value;
    }

    public float getEarthliving() {
        return rawAdditiveData[AdditiveStat.Earthliving.value()];
    }

    public void setEarthliving(float value) {
        rawAdditiveData[AdditiveStat.Earthliving.value()] = value;
    }

    public float getTotemCHBaseHeal() {
        return rawAdditiveData[AdditiveStat.TotemCHBaseHeal.value()];
    }

    public void setTotemCHBaseHeal(float value) {
        rawAdditiveData[AdditiveStat.TotemCHBaseHeal.value()] = value;
    }

    public float getTotemHWBaseCost() {
        return rawAdditiveData[AdditiveStat.TotemHWBaseCost.value()];
    }

    public void setTotemHWBaseCost(float value) {
        rawAdditiveData[AdditiveStat.TotemHWBaseCost.value()] = value;
    }

    public float getTotemCHBaseCost() {
        return rawAdditiveData[AdditiveStat.TotemCHBaseCost.value()];
    }

    public void setTotemCHBaseCost(float value) {
        rawAdditiveData[AdditiveStat.TotemCHBaseCost.value()] = value;
    }

    public float getTotemHWSpellpower() {
        return rawAdditiveData[AdditiveStat.TotemHWSpellpower.value()];
    }

    public void setTotemHWSpellpower(float value) {
        rawAdditiveData[AdditiveStat.TotemHWSpellpower.value()] = value;
    }

    public float getTotemLHWSpellpower() {
        return rawAdditiveData[AdditiveStat.TotemLHWSpellpower.value()];
    }

    public void setTotemLHWSpellpower(float value) {
        rawAdditiveData[AdditiveStat.TotemLHWSpellpower.value()] = value;
    }

    public float getTotemThunderhead() {
        return rawAdditiveData[AdditiveStat.TotemThunderhead.value()];
    }

    public void setTotemThunderhead(float value) {
        rawAdditiveData[AdditiveStat.TotemThunderhead.value()] = value;
    }

    public float getCHHWHealIncrease() {
        return rawAdditiveData[AdditiveStat.CHHWHealIncrease.value()];
    }

    public void setCHHWHealIncrease(float value) {
        rawAdditiveData[AdditiveStat.CHHWHealIncrease.value()] = value;
    }

    public float getWaterShieldIncrease() {
        return rawAdditiveData[AdditiveStat.WaterShieldIncrease.value()];
    }

    public void setWaterShieldIncrease(float value) {
        rawAdditiveData[AdditiveStat.WaterShieldIncrease.value()] = value;
    }

    public float getLHWManaReduction() {
        return rawAdditiveData[AdditiveStat.LHWManaReduction.value()];
    }

    public void setLHWManaReduction(float value) {
        rawAdditiveData[AdditiveStat.LHWManaReduction.value()] = value;
    }

    public float getCHManaReduction() {
        return rawAdditiveData[AdditiveStat.CHManaReduction.value()];
    }

    public void setCHManaReduction(float value) {
        rawAdditiveData[AdditiveStat.CHManaReduction.value()] = value;
    }

    public float getCHHealIncrease() {
        return rawAdditiveData[AdditiveStat.CHHealIncrease.value()];
    }

    public void setCHHealIncrease(float value) {
        rawAdditiveData[AdditiveStat.CHHealIncrease.value()] = value;
    }

    public float getRestoSham2T9() {
        return rawAdditiveData[AdditiveStat.RestoSham2T9.value()];
    }

    public void setRestoSham2T9(float value) {
        rawAdditiveData[AdditiveStat.RestoSham2T9.value()] = value;
    }

    public float getRestoSham4T9() {
        return rawAdditiveData[AdditiveStat.RestoSham4T9.value()];
    }

    public void setRestoSham4T9(float value) {
        rawAdditiveData[AdditiveStat.RestoSham4T9.value()] = value;
    }

    public float getRestoSham2T10() {
        return rawAdditiveData[AdditiveStat.RestoSham2T10.value()];
    }

    public void setRestoSham2T10(float value) {
        rawAdditiveData[AdditiveStat.RestoSham2T10.value()] = value;
    }

    public float getRestoSham4T10() {
        return rawAdditiveData[AdditiveStat.RestoSham4T10.value()];
    }

    public void setRestoSham4T10(float value) {
        rawAdditiveData[AdditiveStat.RestoSham4T10.value()] = value;
    }

    public float getRestoShamRelicT9() {
        return rawAdditiveData[AdditiveStat.RestoShamRelicT9.value()];
    }

    public void setRestoShamRelicT9(float value) {
        rawAdditiveData[AdditiveStat.RestoShamRelicT9.value()] = value;
    }

    public float getRestoShamRelicT10() {
        return rawAdditiveData[AdditiveStat.RestoShamRelicT10.value()];
    }

    public void setRestoShamRelicT10(float value) {
        rawAdditiveData[AdditiveStat.RestoShamRelicT10.value()] = value;
    }

    // == Warlock =========================================================================================

    public float getWarlock4T7() {
        return rawAdditiveData[AdditiveStat.Warlock4T7.value()];
    }

    public void setWarlock4T7(float value) {
        rawAdditiveData[AdditiveStat.Warlock4T7.value()] = value;
    }

    public float getWarlock2T7() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock2T7.value()];
    }

    public void setWarlock2T7(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock2T7.value()] = value;
    }

    public float getWarlock2T8() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock2T8.value()];
    }

    public void setWarlock2T8(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock2T8.value()] = value;
    }

    public float getWarlock4T8() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock4T8.value()];
    }

    public void setWarlock4T8(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock4T8.value()] = value;
    }

    public float getWarlock2T9() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock2T9.value()];
    }

    public void setWarlock2T9(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock2T9.value()] = value;
    }

    public float getWarlock4T9() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock4T9.value()];
    }

    public void setWarlock4T9(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock4T9.value()] = value;
    }

    public float getWarlock2T10() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock2T10.value()];
    }

    public void setWarlock2T10(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock2T10.value()] = value;
    }

    public float getWarlock4T10() {
        return rawMultiplicativeData[MultiplicativeStat.Warlock4T10.value()];
    }

    public void setWarlock4T10(float value) {
        rawMultiplicativeData[MultiplicativeStat.Warlock4T10.value()] = value;
    }

    public float getBonusSpellPowerDemonicPactMultiplier() {
        return rawMultiplicativeData[MultiplicativeStat.BonusSpellPowerDemonicPactMultiplier.value()];
    }

    public void setBonusSpellPowerDemonicPactMultiplier(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusSpellPowerDemonicPactMultiplier.value()] = value;
    }

    // == Warrior =========================================================================================

    public float getBonusTargets() {
        return rawAdditiveData[AdditiveStat.BonusTargets.value()];
    }

    public void setBonusTargets(float value) {
        rawAdditiveData[AdditiveStat.BonusTargets.value()] = value;
    }

    public float getBonusRageGen() {
        return rawAdditiveData[AdditiveStat.BonusRageGen.value()];
    }

    public void setBonusRageGen(float value) {
        rawAdditiveData[AdditiveStat.BonusRageGen.value()] = value;
    }

    public float getBonusRageOnCrit() {
        return rawAdditiveData[AdditiveStat.BonusRageOnCrit.value()];
    }

    public void setBonusRageOnCrit(float value) {
        rawAdditiveData[AdditiveStat.BonusRageOnCrit.value()] = value;
    }

    public float getBonusShieldSlamDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusShieldSlamDamage.value()];
    }

    public void setBonusShieldSlamDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusShieldSlamDamage.value()] = value;
    }

    public float getDevastateCritIncrease() {
        return rawAdditiveData[AdditiveStat.DevastateCritIncrease.value()];
    }

    public void setDevastateCritIncrease(float value) {
        rawAdditiveData[AdditiveStat.DevastateCritIncrease.value()] = value;
    }

    public float getBonusDevastateDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusDevastateDamage.value()];
    }

    public void setBonusDevastateDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusDevastateDamage.value()] = value;
    }

    public float getBonusShockwaveDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusShockwaveDamage.value()];
    }

    public void setBonusShockwaveDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusShockwaveDamage.value()] = value;
    }

    public float getBonusWarrior_T7_2P_SlamDamage() {
        return rawMultiplicativeData[MultiplicativeStat.BonusWarrior_T7_2P_SlamDamage.value()];
    }

    public void setBonusWarrior_T7_2P_SlamDamage(float value) {
        rawMultiplicativeData[MultiplicativeStat.BonusWarrior_T7_2P_SlamDamage.value()] = value;
    }

    public float getBonusWarrior_T7_4P_RageProc() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T7_4P_RageProc.value()];
    }

    public void setBonusWarrior_T7_4P_RageProc(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T7_4P_RageProc.value()] = value;
    }

    public float getBonusWarrior_T8_2P_HasteProc() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T8_2P_HasteProc.value()];
    }

    public void setBonusWarrior_T8_2P_HasteProc(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T8_2P_HasteProc.value()] = value;
    }

    public float getBonusWarrior_T8_4P_MSBTCritIncrease() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T8_4P_MSBTCritIncrease.value()];
    }

    public void setBonusWarrior_T8_4P_MSBTCritIncrease(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T8_4P_MSBTCritIncrease.value()] = value;
    }

    public float getBonusWarrior_T9_2P_Crit() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T9_2P_Crit.value()];
    }

    public void setBonusWarrior_T9_2P_Crit(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T9_2P_Crit.value()] = value;
    }

    public float getBonusWarrior_T9_2P_ArP() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T9_2P_ArP.value()];
    }

    public void setBonusWarrior_T9_2P_ArP(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T9_2P_ArP.value()] = value;
    }

    public float getBonusWarrior_T9_4P_SLHSCritIncrease() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T9_4P_SLHSCritIncrease.value()];
    }

    public void setBonusWarrior_T9_4P_SLHSCritIncrease(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T9_4P_SLHSCritIncrease.value()] = value;
    }

    public float getBonusWarrior_T10_2P_DWAPProc() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T10_2P_DWAPProc.value()];
    }

    public void setBonusWarrior_T10_2P_DWAPProc(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T10_2P_DWAPProc.value()] = value;
    }

    public float getBonusWarrior_T10_4P_BSSDProcChange() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_T10_4P_BSSDProcChange.value()];
    }

    public void setBonusWarrior_T10_4P_BSSDProcChange(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_T10_4P_BSSDProcChange.value()] = value;
    }

    public float getBonusWarrior_PvP_4P_InterceptCDReduc() {
        return rawAdditiveData[AdditiveStat.BonusWarrior_PvP_4P_InterceptCDReduc.value()];
    }

    public void setBonusWarrior_PvP_4P_InterceptCDReduc(float value) {
        rawAdditiveData[AdditiveStat.BonusWarrior_PvP_4P_InterceptCDReduc.value()] = value;
    }

    // == Misc ============================================================================================

    public float getBaseAgility() {
        return rawAdditiveData[AdditiveStat.BaseAgility.value()];
    }

    public void setBaseAgility(float value) {
        rawAdditiveData[AdditiveStat.BaseAgility.value()] = value;
    }

    public float getAttackPowerDemons() {
        return rawAdditiveData[AdditiveStat.AttackPowerDemons.value()];
    }

    public void setAttackPowerDemons(float value) {
        rawAdditiveData[AdditiveStat.AttackPowerDemons.value()] = value;
    }

    public float getWeaponDamage() {
        return rawAdditiveData[AdditiveStat.WeaponDamage.value()];
    }

    public void setWeaponDamage(float value) {
        rawAdditiveData[AdditiveStat.WeaponDamage.value()] = value;
    }

    public float getScopeDamage() {
        return rawAdditiveData[AdditiveStat.ScopeDamage.value()];
    }

    public void setScopeDamage(float value) {
        rawAdditiveData[AdditiveStat.ScopeDamage.value()] = value;
    }

    public float getInterruptProtection() {
        return rawAdditiveData[AdditiveStat.InterruptProtection.value()];
    }

    public void setInterruptProtection(float value) {
        rawAdditiveData[AdditiveStat.InterruptProtection.value()] = value;
    }

    public float getSpellDamageFromIntellectPercentage() {
        return rawAdditiveData[AdditiveStat.SpellDamageFromIntellectPercentage.value()];
    }

    public void setSpellDamageFromIntellectPercentage(float value) {
        rawAdditiveData[AdditiveStat.SpellDamageFromIntellectPercentage.value()] = value;
    }

    public float getSpellDamageFromSpiritPercentage() {
        return rawAdditiveData[AdditiveStat.SpellDamageFromSpiritPercentage.value()];
    }

    public void setSpellDamageFromSpiritPercentage(float value) {
        rawAdditiveData[AdditiveStat.SpellDamageFromSpiritPercentage.value()] = value;
    }

    public float getMovementSpeed() {
        return rawNoStackData[NonStackingStat.MovementSpeed.value()];
    }

    public void setMovementSpeed(float value) {
        rawNoStackData[NonStackingStat.MovementSpeed.value()] = value;
    }

    // == Deprecated ======================================================================================

    public float getWindfuryAPBonus() {
        return rawAdditiveData[AdditiveStat.WindfuryAPBonus.value()];
    }

    public void setWindfuryAPBonus(float value) {
        rawAdditiveData[AdditiveStat.WindfuryAPBonus.value()] = value;
    }

    public float getCritChanceReduction() {
        return rawAdditiveData[AdditiveStat.CritChanceReduction.value()];
    }

    public void setCritChanceReduction(float value) {
        rawAdditiveData[AdditiveStat.CritChanceReduction.value()] = value;
    }

    // == Uncategorized ===================================================================================

    public float getBonusBloodStrikeDamage() {
        return rawAdditiveData[AdditiveStat.BonusBloodStrikeDamage.value()];
    }

    public void setBonusBloodStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusBloodStrikeDamage.value()] = value;
    }

    public float getBonusHeartStrikeDamage() {
        return rawAdditiveData[AdditiveStat.BonusHeartStrikeDamage.value()];
    }

    public void setBonusHeartStrikeDamage(float value) {
        rawAdditiveData[AdditiveStat.BonusHeartStrikeDamage.value()] = value;
    }

}
