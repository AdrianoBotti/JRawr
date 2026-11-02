// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

/**
 * The multiplicative stats: percentage bonuses that combine as
 * {@code (1+a)*(1+b)-1} instead of adding, so two +10% multipliers are +21%, not +20%.
 *
 * <p>Each constant's {@link #value()} is the index of that stat in the multiplicative
 * {@code float[]} inside {@link Stats}. The list contains the game's percent-style modifiers:
 * attribute and armor multipliers, per-school damage multipliers, haste, threat and damage-taken
 * multipliers, plus class-specific set-bonus multipliers. Values are fractions (0.1 = 10%), and
 * subtracting a Stats snapshot divides by {@code 1+fraction} to invert this combination rule; see
 * {@link Stats} for the authoritative rules.</p>
 */
public enum MultiplicativeStat {
    BonusMangleBearThreat(0),
    BonusAgilityMultiplier(1),
    BonusArcaneDamageMultiplier(2),
    BaseArmorMultiplier(3),
    BonusArmorMultiplier(4),
    BonusBleedDamageMultiplier(5),
    BonusBlockValueMultiplier(6),
    BonusAttackPowerMultiplier(7),
    BonusCritMultiplier(8),
    BonusFireDamageMultiplier(9),
    BonusFrostDamageMultiplier(10),
    BonusIntellectMultiplier(11),
    BonusNatureDamageMultiplier(12),
    BonusPhysicalDamageMultiplier(13),
    BonusDamageMultiplier(14),
    BonusSpellCritMultiplier(15),
    BonusSpellPowerMultiplier(16),
    BonusSpellPowerDemonicPactMultiplier(17),
    BonusSpiritMultiplier(18),
    BonusHealthMultiplier(19),
    BonusManaMultiplier(20),
    BonusCritHealMultiplier(21),
    BonusStaminaMultiplier(22),
    BonusStrengthMultiplier(23),
    BonusBearSwipeDamageMultiplier(24),
    BonusShadowDamageMultiplier(25),
    BonusHolyDamageMultiplier(26),
    BonusDiseaseDamageMultiplier(27),
    ThreatIncreaseMultiplier(28),
    BonusManaregenWhileCastingMultiplier(29),
    PhysicalHaste(30),
    RangedHaste(31),
    SpellHaste(32),
    HealingReceivedMultiplier(33),
    BonusHealingDoneMultiplier(34),
    DamageTakenMultiplier(35),
    SpellDamageTakenMultiplier(36),
    PhysicalDamageTakenMultiplier(37),
    StunDurReduc(38),
    SnareRootDurReduc(39),
    FearDurReduc(40),
    BonusFrostWeaponDamage(41),
    BonusRuneStrikeMultiplier(42),
    BonusBloodStrikeDamageMultiplier(43),
    BonusHeartStrikeDamageMultiplier(44),
    BonusHeartStrikeMultiplier(45),
    BonusScourgeStrikeMultiplier(46),
    BonusObliterateMultiplier(47),
    BonusDeathCoilCrit(48),
    BonusDeathStrikeCrit(49),
    BonusFrostStrikeCrit(50),
    BonusObliterateCrit(51),
    BonusPlagueStrikeCrit(52),
    BonusScourgeStrikeCrit(53),
    BonusPerDiseaseBloodStrikeDamage(54),
    BonusPerDiseaseHeartStrikeDamage(55),
    BonusPerDiseaseObliterateDamage(56),
    BonusPerDiseaseScourgeStrikeDamage(57),
    BonusLacerateDamageMultiplier(58),
    BonusRipDamageMultiplier(59),
    BonusPoHManaCostReductionMultiplier(60),
    BonusGHHealingMultiplier(61),
    BonusMindBlastMultiplier(62),
    BonusLavaBurstDamageMultiplier(63),
    BonusSealOfCorruptionDamageMultiplier(64),
    BonusSealOfRighteousnessDamageMultiplier(65),
    BonusSealOfVengeanceDamageMultiplier(66),
    HammerOfTheRighteousMultiplier(67),
    BonusRangedAttackPowerMultiplier(68),
    BonusPetAttackPowerMultiplier(69),
    BonusPetDamageMultiplier(70),
    BonusHunter_T7_4P_ViperSpeed(71),
    BonusHunter_T8_2P_SerpDmg(72),
    ManaCostPerc(73),
    Warlock2T7(74),
    Warlock2T8(75),
    Warlock4T8(76),
    Warlock2T9(77),
    Warlock4T9(78),
    Warlock2T10(79),
    Warlock4T10(80),
    BonusShieldSlamDamage(81),
    BonusDevastateDamage(82),
    BonusShockwaveDamage(83),
    BonusWarrior_T7_2P_SlamDamage(84),
    BossAttackSpeedMultiplier(85);

    private final int value;

    MultiplicativeStat(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
