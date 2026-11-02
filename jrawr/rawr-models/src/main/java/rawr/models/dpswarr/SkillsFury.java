// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Skills.Fury.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Fury ability classes. Non-Silverlight branches only.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.Stats;

class BloodThirst extends Ability {
    public BloodThirst(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Bloodthirst";
        description = "Instantly attack the target causing [AP*50/100] damage. In addition, the next 3 successful melee attacks will restore 1% health. This effect lasts 8 sec. Damage is based on your attack power.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Bloodthirst_.index;
        reqTalent = true;
        talent2ChksValue = talents().getBloodthirst();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = 4f; // In Seconds
        rageCost = 20f - (talents().getFocusedRage() * 1f);
        stanceOkFury = true;
        damageBase = statS.getAttackPower() * 50f / 100f;
        damageBonus = 1f + talents().getUnendingFury() * 0.02f;
        bonusCritChance = statS.getBonusWarrior_T8_4P_MSBTCritIncrease();
        healingBase = statS.getHealth() / 100.0f * 3f * (talents().isGlyphOfBloodthirst() ? 2f : 1f);
        initialize();
    }
}

class WhirlWind extends Ability {
    public WhirlWind(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Whirlwind";
        description = "In a whirlwind of steel you attack up to 4 enemies in 8 yards, causing weapon damage from both melee weapons to each enemy.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Whirlwind_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        maxRange = 8f; // In Yards
        cd = 10f - (talents().isGlyphOfWhirlwind() ? 2f : 0f); // In Seconds
        targets += (calcOpts.getMultipleTargets() ? 3f : 0f);
        rageCost = 25f - (talents().getFocusedRage() * 1f);
        stanceOkFury = true;
        damageBonus = (1f + talents().getImprovedWhirlwind() * 0.10f) * (1f + talents().getUnendingFury() * 0.02f);
        swingsOffHand = true;
        initialize();
    }

    // Whirlwind while dual wielding executes two separate attacks; assume no offhand in base case
    @Override
    public float damageOverride() {
        return getDamage(false) + getDamage(true);
    }

    private float getDamage(boolean isOffHand) {
        float damage;
        if (isOffHand) {
            damage = combatFactors.normalizedOhWeaponDmg();
        } else {
            damage = combatFactors.normalizedMhWeaponDmg();
        }
        return damage * damageBonus;
    }

    @Override
    public float damageOnUseOverride() {
        // ==== MAIN HAND ====
        float damageMH = getDamage(false); // Base Damage
        damageMH *= combatFactors.damageBonus(); // Global Damage Bonuses
        damageMH *= combatFactors.damageReduction(); // Global Damage Penalties

        // Work the Attack Table
        float dmgDrop = (1f
                - mhAtkTable().miss
                - mhAtkTable().dodge
                - mhAtkTable().parry
                - mhAtkTable().glance
                - mhAtkTable().block
                - mhAtkTable().crit);

        float dmgGlance = damageMH * mhAtkTable().glance * combatFactors.reducWhGlancedDmg();
        float dmgBlock = damageMH * mhAtkTable().block * combatFactors.reducYwBlockedDmg();
        float dmgCrit = damageMH * mhAtkTable().crit * (1f + combatFactors.bonusYellowCritDmg());

        damageMH *= dmgDrop;

        damageMH += dmgGlance + dmgBlock + dmgCrit;

        // ==== OFF HAND ====
        float damageOH = getDamage(true); // Base Damage
        damageOH *= combatFactors.damageBonus(); // Global Damage Bonuses
        damageOH *= combatFactors.damageReduction(); // Global Damage Penalties

        // Work the Attack Table
        dmgDrop = (1f
                - ohAtkTable().miss
                - ohAtkTable().dodge
                - ohAtkTable().parry
                - ohAtkTable().glance
                - ohAtkTable().block
                - ohAtkTable().crit);

        dmgGlance = damageOH * ohAtkTable().glance * combatFactors.reducWhGlancedDmg();
        dmgBlock = damageOH * ohAtkTable().block * combatFactors.reducYwBlockedDmg();
        dmgCrit = damageOH * ohAtkTable().crit * (1f + combatFactors.bonusYellowCritDmg());

        damageOH *= dmgDrop;

        damageOH += dmgGlance + dmgBlock + dmgCrit;

        // ==== RESULT ====
        float damage = damageMH + damageOH;
        return damage * avgTargets();
    }
}

class BloodSurge extends Ability {
    public BloodSurge(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo,
            Ability slam, Ability whirlwind, Ability bloodthirst) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Bloodsurge";
        description = "Your Heroic Strike, Bloodthirst and Whirlwind hits have a (7%/13%/20%) chance of making your next Slam instant for 5 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Bloodsurge_.index;
        reqTalent = true;
        talent2ChksValue = talents().getBloodsurge();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        duration = 5f; // In Seconds
        rageCost = 15f - (talents().getFocusedRage() * 1f);
        stanceOkFury = true;
        hsActivates = 0.0f;
        sl = slam;
        ww = whirlwind;
        bt = bloodthirst;
        useReact = true;
        initialize();
    }

    public float hsActivates;
    public float maintainActs;
    public Ability sl;
    public Ability ww;
    public Ability bt;

    private float basicFuryRotation(float chanceMHhit, float chanceOHhit, float hsActivates, float procChance) {
        // Assumes one slot to slam every 8 seconds: WW/BT/Slam/BT repeat. Not optimal, but easy to do
        float chanceWeDontProc = 1f;
        float actMod = 8f / fightDuration(); // since we're assuming an 8sec rotation
        boolean is4T10 = (statS.getBonusWarrior_T10_4P_BSSDProcChange() != 0f);

        float bonusProcChance = 0f;

        // We can squeeze in a single slam from 7-8 if we get a proc
        if (is4T10) {
            bonusProcChance = 1f
                    - (1f - actMod * bt.activates() / 2f * procChance * bt.mhAtkTable().anyLand() * 0.2f)
                    * (1f - actMod * hsActivates * 3f / 8f * procChance * mhAtkTable().anyLand() * 0.2f); // 1proc left from this
        }

        // Only WW
        chanceWeDontProc *= (1f - actMod * ww.activates() * procChance * ww.mhAtkTable().anyLand())
                * (1f - actMod * ww.activates() * procChance * ww.ohAtkTable().anyLand());
        // Second BT
        chanceWeDontProc *= (1f - actMod * bt.activates() / 2f * procChance * bt.mhAtkTable().anyLand());
        // Other 5/8s of the HSes
        chanceWeDontProc *= (1f - actMod * hsActivates * 5f / 8f * procChance * mhAtkTable().anyLand());

        // chanceWeDontProc% of the time, we don't proc. But bonusProcChance% of the time, we can use the leftover here
        float numProcs = (1f + chanceWeDontProc) * bonusProcChance;
        numProcs += (1f - chanceWeDontProc) * (is4T10 ? 1.2f : 1f);
        return (numProcs / actMod);
    }

    private float calcSlamProcs(float chanceMHhit, float chanceOHhit, float hsActivates, float procChance) {
        return 0f;
    }

    @Override
    protected float activatesOverride() {
        float chance = talents().getBloodsurge() * 0.20f / 3f;
        float chanceMhHitLands = (1f - mhAtkTable().miss - mhAtkTable().dodge);
        float chanceOhHitLands = (1f - ohAtkTable().miss - ohAtkTable().dodge);

        float procs3 = basicFuryRotation(chanceMhHitLands, chanceOhHitLands, hsActivates, chance);

        procs3 = (maintainActs > procs3) ? 0f : procs3 - maintainActs;

        return procs3 * (1f - whiteattacks.rageSlip(fightDuration() / procs3, rageCost));
    }

    @Override
    public float damageOverride() {
        return sl.damageOverride();
    }
}

// OnAttacks
class HeroicStrike extends OnAttack {
    public HeroicStrike(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Heroic Strike";
        description = "A strong attack that increases melee damage by 495 and causes a high amount of threat. Causes 173.25 additional damage against Dazed targets.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.HeroicStrike_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = /*0f*/(char_.getMainHand() != null ? whiteattacks.mhEffectiveSpeed() : 0f); // In Seconds
        rageCost = 15f - (talents().getImprovedHeroicStrike() * 1f) - (talents().getFocusedRage() * 1f);
        setCastTime(0f); // In Seconds // Replaces a white hit
        setGCDTime(0f);
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        damageBase = whiteattacks.mhDamage() + 495f;
        bonusCritChance = talents().getIncite() * 0.05f + statS.getBonusWarrior_T9_4P_SLHSCritIncrease();
        initialize();
    }

    @Override
    public float fullRageCost() {
        return super.fullRageCost() - (talents().isGlyphOfHeroicStrike() ? 10.0f * mhAtkTable().crit : 0f);
    }
}

class Cleave extends OnAttack {
    public Cleave(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Cleave";
        description = "A sweeping attack that does your weapon damage plus 222 to the target and his nearest ally.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Cleave_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        rageCost = 20f - (talents().getFocusedRage() * 1f);
        targets += (calcOpts.getMultipleTargets() ? 1f + (talents().isGlyphOfCleaving() ? 1f : 0f) : 0f);
        setCastTime(0f); // In Seconds // Replaces a white hit
        setGCDTime(0f);
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        damageBase = whiteattacks.mhDamage() + (222f * (1f + talents().getImprovedCleave() * 0.40f));
        bonusCritChance = talents().getIncite() * 0.05f;
        initialize();
    }
}

// Unused (kept for parity with the original source file).
class Pummel extends Ability {
}
