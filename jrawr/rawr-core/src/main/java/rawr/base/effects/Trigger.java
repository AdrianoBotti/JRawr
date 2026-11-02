// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffect.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.effects;

/**
 * The event that can activate a {@link SpecialEffect} (a "proc"): the trigger tells the model
 * which action rate to feed into the effect's uptime computation.
 *
 * <p>The list is the union of every event the models ever needed, roughly grouped as: on-use
 * ({@code Use}); generic spell events ({@code SpellHit}, {@code SpellCrit}, {@code SpellCast},
 * {@code SpellMiss}); physical, ranged and melee events, including the per-weapon
 * {@code MainHandHit}, {@code OffHandHit} and {@code CurrentHandHit}; damage taken, avoided and
 * done; damage-over-time ticks ({@code DoTTick}, {@code CorruptionTick}, ...); and class-specific
 * events (death-knight strikes, Druid bleeds and DoTs, shaman shocks, paladin casts, hunter
 * shots, ...). A model maps each trigger to "how often does this happen per second" and passes
 * that interval to {@link SpecialEffect}; the enum itself carries no numbers. The constant names
 * are written to and parsed from the buff-cache XML, so they are persistent identifiers.</p>
 */
public enum Trigger {
    Use,
    SpellHit,
    SpellCrit,
    SpellCast,
    SpellMiss,
    DamageSpellHit,
    DamageSpellCrit,
    DamageSpellCast,
    HealingSpellHit,
    HealingSpellCrit,
    HealingSpellCast,
    PhysicalHit,
    PhysicalCrit,
    MeleeHit,
    MeleeCrit,
    MeleeAttack,
    RangedHit,
    RangedCrit,
    MainHandHit,
    OffHandHit,
    CurrentHandHit,
    DamageTaken,
    DamageAvoided,
    DamageDone,
    DoTTick,
    DamageTakenMagical,
    DamageTakenPhysical,
    DamageOrHealingDone,
    BloodStrikeHit,
    HeartStrikeHit,
    ObliterateHit,
    DeathStrikeHit,
    ScourgeStrikeHit,
    BloodStrikeOrHeartStrikeHit,
    PlagueStrikeHit,
    IcyTouchHit,
    RuneStrikeHit,
    FrostFeverHit,
    InsectSwarmTick,
    MoonfireCast,
    MoonfireTick,
    InsectSwarmOrMoonfireTick,
    RejuvenationTick,
    MangleCatHit,
    MangleBearHit,
    SwipeBearOrLacerateHit,
    MangleCatOrShredHit,
    MangleCatOrShredOrInfectedWoundsHit,
    RakeTick,
    LacerateTick,
    HunterAutoShotHit,
    SteadyShotHit,
    PetClawBiteSmackCrit,
    SerpentWyvernStingsDoDamage,
    ManaGem,
    MageNukeCast,
    JudgementHit,
    HolyShield,
    HammeroftheRighteous,
    ShieldofRighteousness,
    DivinePlea,
    CrusaderStrikeHit,
    SealOfVengeanceTick,
    HolyLightCast,
    HolyShockCast,
    ShamanLightningBolt,
    ShamanLavaLash,
    ShamanShock,
    ShamanFlameShockDoTTick,
    ShamanStormStrike,
    ShamanShamanisticRage,
    HSorSLHit,
    DeepWoundsTick,
    CorruptionTick
}
