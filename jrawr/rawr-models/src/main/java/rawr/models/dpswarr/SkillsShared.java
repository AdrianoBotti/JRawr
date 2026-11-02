// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Skills.Shared.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ability classes shared by the Arms/Fury rotations. C# `{ get; protected set; }` properties are
// public fields; virtual properties are methods. Non-Silverlight branches only.
package rawr.models.dpswarr;

import rawr.base.stats.BaseStats;
import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

class DeepWounds extends DoT {
    public DeepWounds(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Deep Wounds";
        description = "Your critical strikes cause the opponent to bleed, dealing (16*Pts)% of your melee weapon's average damage over 6 sec.";
        reqTalent = true;
        talent2ChksValue = talents().getDeepWounds();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        canCrit = false;
        duration = 6f; // In Seconds
        timeBtwnTicks = 1f; // In Seconds
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        mhActivates = ohActivates = 0f;
        useHitTable = false;
        usesGCD = false;
        initialize();
    }

    private float mhActivates, ohActivates;

    public void setAllAbilityActivates(float mh, float oh) {
        mhActivates = mh;
        ohActivates = oh;
    }

    @Override
    protected float activatesOverride() {
        return mhActivates + ohActivates;
    }

    @Override
    public float tickSize() {
        if (!validated() || (mhActivates + ohActivates) <= 0f) {
            return 0f;
        }

        float damage = combatFactors.avgMhWeaponDmgUnhasted() * (0.16f * talents().getDeepWounds()) * mhActivates / (mhActivates + ohActivates)
                + combatFactors.avgOhWeaponDmgUnhasted() * (0.16f * talents().getDeepWounds()) * ohActivates / (mhActivates + ohActivates);

        damage *= (1f + statS.getBonusBleedDamageMultiplier());
        damage *= combatFactors.damageBonus();

        damage /= duration * timeBtwnTicks;

        damage *= (mhActivates + ohActivates) * duration / fightDuration();

        return damage;
    }

    @Override
    public float getDPS(float acts) {
        return tickSize();
    }

    @Override
    public float dps() {
        return tickSize();
    }
}

// BuffEffects
class BerserkerRage extends BuffEffect {
    public BerserkerRage(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Berserker Rage";
        description = "The warrior enters a berserker rage, becoming immune to Fear, Sap and Incapacitate effects and generating extra tage when taking damage. Lasts 10 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.BerserkerRage_.index;
        cd = 30f * (1f - 1f / 9f * talents().getIntensifyRage()); // In Seconds
        rageCost = 0f - (talents().getImprovedBerserkerRage() * 10f); // This is actually reversed in the rotation
        stanceOkArms = stanceOkDef = stanceOkFury = true;
        useHitTable = false;
        useReact = true;
        initialize();
    }
}

class EnragedRegeneration extends BuffEffect {
    public EnragedRegeneration(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Enraged Regeneration";
        description = "You regenerate 30% of your total health over 10 sec. This ability requires an Enrage effect, consumes all Enrage effects and prevents any from affecting you for the full duration.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.EnragedRegeneration_.index;
        cd = 3f * 60f; // In Seconds
        rageCost = 15f;
        stanceOkArms = stanceOkDef = stanceOkFury = true;
        healingBase = statS.getHealth() * (0.30f + (talents().isGlyphOfEnragedRegeneration() ? 0.10f : 0f));
        useHitTable = false;
        useReact = true;
        initialize();
    }
}

class LastStand extends BuffEffect {
}

class Bloodrage extends BuffEffect {
    public Bloodrage(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Bloodrage";
        description = "Generates 10 rage at the cost of health and then generates an additional 10 rage over 10 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Bloodrage_.index;
        cd = 60f * (1f - 1f / 9f * talents().getIntensifyRage()); // In Seconds
        duration = 10f; // In Seconds
        // Rage is actually reversed in the rotation
        rageCost = -(20f // Base
                + 10f) // Over Time
                * (1f + talents().getImprovedBloodrage() * 0.25f); // Talent Bonus
        stanceOkArms = stanceOkDef = stanceOkFury = true;
        Stats base = BaseStats.getBaseStats(char_.getLevel(), CharacterClass.Warrior, char_.getRace());
        float baseHealth = base.getHealth() + StatConversion.getHealthFromStamina(base.getStamina(), CharacterClass.Warrior);
        healingBase = -1f * (float) Math.floor(baseHealth) * 0.16f;
        healingBonus = (talents().isGlyphOfBloodrage() ? 0f : 1f);
        useHitTable = false;
        usesGCD = false;
        useReact = true;
        initialize();
    }
}

class BattleShout extends BuffEffect {
    public BattleShout(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Battle Shout";
        description = "The warrior shouts, increasing attack power of all raid and party members within 20 yards by 548. Lasts 2 min.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.BattleShout_.index;
        maxRange = 30f * (1f + talents().getBoomingVoice() * 0.25f); // In Yards
        duration = (2f + (talents().isGlyphOfBattle() ? 2f : 0f)) * 60f * (1f + talents().getBoomingVoice() * 0.25f);
        cd = duration;
        rageCost = 10f;
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        useHitTable = false;
        initialize();
    }

    @Override
    public Stats averageStats() {
        if (!validated()) {
            return new Stats();
        }
        Stats bonus = effect.getAverageStats(1f);
        return bonus;
    }
}

class CommandingShout extends BuffEffect {
    public CommandingShout(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Commanding Shout";
        description = "The warrior shouts, increasing the maximum health of all raid and party members within 20 yards by 2255. Lasts 2 min.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.CommandingShout_.index;
        maxRange = 30f * (1f + talents().getBoomingVoice() * 0.25f); // In Yards
        duration = (2f + (talents().isGlyphOfCommand() ? 2f : 0f)) * 60f * (1f + talents().getBoomingVoice() * 0.25f);
        cd = duration;
        rageCost = 10f;
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        useHitTable = false;
        initialize();
    }

    @Override
    public Stats averageStats() {
        if (!validated()) {
            return new Stats();
        }
        Stats bonus = effect.getAverageStats(1f);
        return bonus;
    }
}

class DeathWish extends BuffEffect {
    public DeathWish(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Death Wish";
        description = "When activated you become enraged, increasing your physical damage by 20% but increasing all damage taken by 5%. Lasts 30 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.DeathWish_.index;
        reqTalent = true;
        talent2ChksValue = talents().getDeathWish();
        cd = 3f * 60f * (1f - 1f / 9f * talents().getIntensifyRage()); // In Seconds
        duration = 30f;
        rageCost = 10f;
        stanceOkArms = stanceOkFury = true;
        useHitTable = false;
        initialize();
    }
}

class Recklessness extends BuffEffect {
    public Recklessness(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Recklessness";
        description = "Your next 3 special ability attacks have an additional 100% to critically hit but all damage taken is increased by 20%. Lasts 12 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Recklessness_.index;
        cd = (5f * 60f - talents().getImprovedDisciplines() * 30f) * (1f - 1f / 9f * talents().getIntensifyRage()); // In Seconds
        duration = 12f; // In Seconds
        stanceOkFury = true;
        useHitTable = false;
        initialize();
    }
}

class SweepingStrikes extends BuffEffect {
    public SweepingStrikes(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Sweeping Strikes";
        description = "Your next 5 melee attacks strike an additional nearby opponent.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.SweepingStrikes_.index;
        reqTalent = true;
        talent2ChksValue = talents().getSweepingStrikes();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        reqMultiTargs = true;
        cd = calcOpts.getMultipleTargetsPerc() != 0 ? 30f / (calcOpts.getMultipleTargetsPerc() / 100f) : fightDuration() + (1.5f + calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact())); // In Seconds
        duration = 30f;
        rageCost = 30f - (talents().getFocusedRage() * 1f);
        rageCost = (talents().isGlyphOfSweepingStrikes() ? 0f : rageCost);
        stanceOkFury = stanceOkArms = true;
        useHitTable = false;
        initialize();
    }
}

class SecondWind extends BuffEffect {
    public SecondWind(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Second Wind";
        description = "Whenever you are struck by a Stun of Immoblize effect you will generate 10*Pts Rage and (5*Pts)% of your total health over 10 sec.";
        reqTalent = true;
        talent2ChksValue = talents().getSecondWind();
        cd = -1f;
        NUMSTUNSOVERDUR = 0f;
        duration = 10f; // Using 4 seconds to sim consume time
        rageCost = -10f * talents().getSecondWind();
        stanceOkDef = stanceOkFury = stanceOkArms = true;
        healingBase = statS.getHealth() * 0.05f * talents().getSecondWind();
        useHitTable = false;
        usesGCD = false;
        initialize();
    }

    private float NUMSTUNSOVERDUR;

    public float getNumStunsOverDur() {
        return NUMSTUNSOVERDUR;
    }

    public void setNumStunsOverDur(float value) {
        NUMSTUNSOVERDUR = value;
        cd = (fightDuration() / NUMSTUNSOVERDUR);
        if (effect != null) {
            effect.setCooldown(cd);
        }
    }
}

// DeBuff Abilities
class ThunderClap extends BuffEffect {
    public ThunderClap(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Thunder Clap";
        description = "Blasts nearby enemies increasing the time between their attacks by 10% for 30 sec and doing [300+AP*0.12] damage to them. Damage increased by attack power. This ability causes additional threat.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.ThunderClap_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        targets += (calcOpts.getMultipleTargets() ? (calcOpts.getMultipleTargetsMax() - 1f) : 0f);
        maxRange = 5f + (talents().isGlyphOfThunderClap() ? 2f : 0f); // In Yards
        cd = 6f; // In Seconds
        duration = 30f; // In Seconds
        float cost = 0f;
        switch (talents().getImprovedThunderClap()) {
            case 1: { cost = 1f; break; }
            case 2: { cost = 2f; break; }
            case 3: { cost = 4f; break; }
            default: { cost = 0f; break; }
        }
        rageCost = 20f - cost - (talents().isGlyphOfResonatingPower() ? 5f : 0f) - (talents().getFocusedRage() * 1f);
        stanceOkArms = stanceOkDef = true;
        damageBase = 300f + statS.getAttackPower() * 0.12f;
        damageBonus = 1f + talents().getImprovedThunderClap() * 0.10f;
        bonusCritChance = talents().getIncite() * 0.05f;
        useSpellHit = true;
        canBeDodged = canBeParried = false;
        initialize();
    }

    @Override
    protected float activatesOverride() {
        float result = 0f;
        float base = super.activatesOverride();
        addMisses = (mhAtkTable().miss > 0) ? base * mhAtkTable().miss : 0f;

        if (calcOpts.getAllowFlooring()) {
            addMisses = (float) Math.ceil(addMisses);
        }

        result = base + addMisses + addDodges + addParrys;

        return result;
    }
}

class SunderArmor extends BuffEffect {
    public SunderArmor(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Sunder Armor";
        description = "Sunders the target's armor, reducing it by 4% per Sunder Armor and causes a high amount of threat.  Threat increased by attack power.  Can be applied up to 5 times.  Lasts 30 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.SunderArmor_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        duration = 30f; // In Seconds
        cd = 1.5f;
        canCrit = false;
        rageCost = 15f - (talents().getFocusedRage() * 1f) - (talents().getPuncture() * 1f);
        targets = 1f + (talents().isGlyphOfSunderArmor() ? 1f : 0f);
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        initialize();
    }

    @Override
    protected float activatesOverride() {
        float result = 0f;
        float base = super.activatesOverride();
        addMisses = (mhAtkTable().miss > 0) ? base * mhAtkTable().miss : 0f;
        addDodges = (mhAtkTable().dodge > 0) ? base * mhAtkTable().dodge : 0f;
        addParrys = (mhAtkTable().parry > 0) ? base * mhAtkTable().parry : 0f;

        if (calcOpts.getAllowFlooring()) {
            addMisses = (float) Math.ceil(addMisses);
            addDodges = (float) Math.ceil(addDodges);
            addParrys = (float) Math.ceil(addParrys);
        }

        result = base + addMisses + addDodges + addParrys;

        return result;
    }
}

class ShatteringThrow extends BuffEffect {
    public ShatteringThrow(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Shattering Throw";
        description = "Throws your weapon at the enemy causing (12+AP*0.50) damage (based on attack power), reducing the armor on the target by 20% for 10 sec or removing any invulnerabilities.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.ShatteringThrow_.index;
        reqMeleeWeap = true;
        reqMeleeRange = false;
        maxRange = 30f; // In Yards
        cd = 5f * 60f; // In Seconds
        duration = 10f;
        setCastTime(1.5f); // In Seconds
        rageCost = 25f - (talents().getFocusedRage() * 1f);
        stanceOkArms = true;
        damageBase = 12f + statS.getAttackPower() * 0.50f;
        initialize();
    }
}

class DemoralizingShout extends BuffEffect {
    public DemoralizingShout(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Demoralizing Shout";
        description = "Reduces the melee attack power of all enemies within 10 yards by 411 for 30 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.DemoralizingShout_.index;
        reqMeleeWeap = false;
        reqMeleeRange = false;
        maxRange = 10f; // In Yards
        duration = 30f * (1f + 0.05f * talents().getBoomingVoice());
        rageCost = 10f - (talents().getFocusedRage() * 1f);
        stanceOkArms = stanceOkFury = true;
        useSpellHit = true;
        initialize();
    }

    @Override
    protected float activatesOverride() {
        float result = 0f;
        float base = super.activatesOverride();
        addMisses = (mhAtkTable().miss > 0) ? base * mhAtkTable().miss : 0f;

        if (calcOpts.getAllowFlooring()) {
            addMisses = (float) Math.ceil(addMisses);
        }

        result = base + addMisses + addDodges + addParrys;

        return result;
    }
}

class Hamstring extends BuffEffect {
    public Hamstring(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Hamstring";
        description = "Instant, No cd, 10 Rage, Melee Range, Melee Weapon, (Battle/Zerker) Maims the enemy, reducing movement speed by 50% for 15 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Hamstring_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        duration = 15f; // In Seconds
        rageCost = 10f - (talents().getFocusedRage() * 1f);
        stanceOkFury = stanceOkArms = true;
        initialize();
    }

    @Override
    protected float activatesOverride() {
        float result = 0f;
        float base = super.activatesOverride();
        addMisses = (mhAtkTable().miss > 0) ? base * mhAtkTable().miss : 0f;
        addDodges = (mhAtkTable().dodge > 0) ? base * mhAtkTable().dodge : 0f;
        addParrys = (mhAtkTable().parry > 0) ? base * mhAtkTable().parry : 0f;

        if (calcOpts.getAllowFlooring()) {
            addMisses = (float) Math.ceil(addMisses);
            addDodges = (float) Math.ceil(addDodges);
            addParrys = (float) Math.ceil(addParrys);
        }

        result = base + addMisses + addDodges + addParrys;

        return result;
    }
}

// Anti-Debuff Abilities
class EveryManForHimself extends Ability {
    public EveryManForHimself(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Every Man for Himself";
        description = "Removes all movement impairing effects and all effects which cause loss of control of your character. This effect shares a cooldown with other similar effects.";
        cd = 2f * 60f;
        stanceOkArms = stanceOkFury = stanceOkDef = true;
        useHitTable = false;
        useReact = true;
        initialize();
    }
}

class HeroicFury extends Ability {
    public HeroicFury(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Heroic Fury";
        description = "Removes any Immobilization effects and refreshes the cooldown of your Intercept ability.";
        reqTalent = true;
        talent2ChksValue = talents().getHeroicFury();
        cd = 45f;
        stanceOkArms = stanceOkFury = stanceOkDef = true;
        useHitTable = false;
        useReact = true;
        usesGCD = false;
        initialize();
    }
}

// Movement Abilities
class Charge extends Ability {
    public Charge(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Charge";
        description = "Charge an enemy, generate 15 rage, and stun it for 1.50 sec. Cannot be used in combat.";
        minRange = 8f;
        maxRange = 25f + (talents().isGlyphOfCharge() ? 5f : 0f); // In Yards
        cd = (15f + talents().getJuggernaut() * 5f) * (1f - (talents().isGlyphOfRapidCharge() ? 0.07f : 0f)); // In Seconds
        duration = 1.5f;
        rageCost = -(15f + (talents().getImprovedCharge() * 5f));
        if (talents().getWarbringer() == 1) {
            stanceOkArms = stanceOkFury = stanceOkDef = true;
        } else if (talents().getJuggernaut() == 1) {
            stanceOkArms = true;
        }
        initialize();
    }
}

class Intercept extends Ability {
    public Intercept(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Intercept";
        description = "Charge an enemy, causing 380 damage (based on attack power) and stunning it for 3 sec.";
        minRange = 8f;
        maxRange = 25f; // In Yards
        cd = 30f - (talents().getImprovedIntercept() * 5f) - statS.getBonusWarrior_PvP_4P_InterceptCDReduc(); // In Seconds
        duration = 3f;
        rageCost = 10f - talents().getPrecision() * 1f;
        stanceOkFury = true; stanceOkArms = stanceOkDef = (talents().getWarbringer() == 1);
        damageBase = 380f;
        initialize();
    }
}

class Intervene extends Ability {
    public Intervene(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Intervene";
        description = "Charge an enemy, causing 380 damage (based on attack power) and stunning it for 3 sec.";
        minRange = 8f;
        maxRange = 25f; // In Yards
        cd = 30f * (1f - (talents().getImprovedIntercept() * 5f)); // In Seconds
        rageCost = 10f;
        stanceOkDef = true; stanceOkArms = stanceOkFury = (talents().getWarbringer() == 1);
        useHitTable = false;
        initialize();
    }
}

// Other Abilities
class Retaliation extends Ability {
    public Retaliation(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Retaliation";
        description = "Instantly counterattack any enemy that strikes you in melee for 12 sec. Melee attacks made from behind cannot be counterattacked. A maximum of 20 attacks will cause retaliation.";
        stanceOkArms = true;
        reqMeleeRange = true;
        reqMeleeWeap = true;
        cd = 5f * 60f - talents().getImprovedDisciplines() * 30f;
        duration = 12f;
        stackCap = 20f;
        useHitTable = false;
        initialize();
    }

    public float stackCap;
}
