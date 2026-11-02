// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Skills.Arms.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Arms ability classes. Non-Silverlight branches only.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

class MortalStrike extends Ability {
    public MortalStrike(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Mortal Strike";
        description = "A vicious strike that deals weapon damage plus 380 and wounds the target, reducing the effectiveness of any healing by 50% for 10 sec.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.MortalStrike_.index;
        reqTalent = true;
        talent2ChksValue = talents().getMortalStrike();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = 6f - (talents().getImprovedMortalStrike() / 3f); // In Seconds
        rageCost = 30f - (talents().getFocusedRage() * 1f);
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        damageBase = combatFactors.normalizedMhWeaponDmg() + 380f;
        damageBonus = (1f + talents().getImprovedMortalStrike() / 3f * 0.1f)
                * (1f + (talents().isGlyphOfMortalStrike() ? 0.1f : 0f));
        bonusCritChance = statS.getBonusWarrior_T8_4P_MSBTCritIncrease();
        initialize();
    }
}

class Suddendeath extends Ability {
    public Suddendeath(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo, Ability ex) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Sudden Death";
        description = "Your melee hits have a (3*Pts)% chance of allowing the use of Execute regardless of the target's Health state. This Execute only uses up to 30 total rage. In addition, you keep at least (3/7/10) rage after using Execute.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.SuddenDeath_.index;
        exec = (Execute) ex;
        rageCost = exec.rageCost;
        reqTalent = true;
        talent2ChksValue = talents().getSuddenDeath();
        reqMeleeWeap = exec.reqMeleeWeap;
        reqMeleeRange = exec.reqMeleeRange;
        cd = exec.cd;
        if (s.getBonusWarrior_T10_4P_BSSDProcChange() > 0f) {
            // 20% proc rate, so 20% of the time we get 2 SDs over 2*(GCD-0.5)
            float procRate = 0.2f;
            float numSDActivates = 1f + procRate;
            float activatesHasted = procRate * 2f; // assuming both activates have the 0.5sec GCD redux
            float percActivatesHasted = activatesHasted / numSDActivates;
            float percActivatesUnhasted = 1f - percActivatesHasted;
            setGCDTime(((cd - 0.5f) * percActivatesHasted) + percActivatesUnhasted * cd);
        }
        stanceOkArms = true;
        useReact = true;
        initialize();
    }

    public Execute exec;

    public float getFreeRage() {
        return exec.getFreeRage();
    }

    public void setFreeRage(float value) {
        exec.setFreeRage(value);
    }

    public float getUsedExtraRage() {
        return exec.usedExtraRage;
    }

    public void setUsedExtraRage(float value) {
        exec.usedExtraRage = value;
    }

    private static final SpecialEffect[][] buff = {
            { new SpecialEffect(Trigger.MeleeHit, new Stats(), (10f), 0f, 0 * 0.03f), new SpecialEffect(Trigger.MeleeHit, new Stats(), (20f), 0f, 0 * 0.03f), },
            { new SpecialEffect(Trigger.MeleeHit, new Stats(), (10f), 0f, 1 * 0.03f), new SpecialEffect(Trigger.MeleeHit, new Stats(), (20f), 0f, 1 * 0.03f), },
            { new SpecialEffect(Trigger.MeleeHit, new Stats(), (10f), 0f, 2 * 0.03f), new SpecialEffect(Trigger.MeleeHit, new Stats(), (20f), 0f, 2 * 0.03f), },
            { new SpecialEffect(Trigger.MeleeHit, new Stats(), (10f), 0f, 3 * 0.03f), new SpecialEffect(Trigger.MeleeHit, new Stats(), (20f), 0f, 3 * 0.03f), },
    };

    static void clearStaticEffectCaches() {
        for (SpecialEffect[] row : buff) {
            for (SpecialEffect e : row) if (e != null) e.clearInterpolatorCache();
        }
    }

    protected SpecialEffect buff() {
        return buff[talents().getSuddenDeath()][statS.getBonusWarrior_T10_4P_BSSDProcChange() > 0 ? 1 : 0];
    }

    public float getActivates(float landedatksoverdur) {
        if (abilIterater != -1 && !calcOpts.getMaintenance()[abilIterater]) {
            return 0f;
        }
        float acts = buff().getAverageProcsPerSecond(landedatksoverdur / fightDuration(), 1f/*MHAtkTable.AnyLand*/, combatFactors.cMhItemSpeed, fightDuration());
        acts *= fightDuration();
        if (statS.getBonusWarrior_T10_4P_BSSDProcChange() > 0) {
            acts *= 1.20f;
        }

        return acts * (1f - whiteattacks.rageSlip(fightDuration() / acts, rageCost + getUsedExtraRage()));
    }

    @Override
    public float damageOverride() {
        return exec.damageOverride();
    }

    @Override
    public float getRageUseOverDur(float acts) {
        if (!validated()) {
            return 0f;
        }
        return acts * (rageCost + getUsedExtraRage() - this.sDRageReduc[talents().getSuddenDeath()]);
    }

    private final float[] sDRageReduc = { 0, 3, 7, 10 };

    private float[] sDRageReduc() {
        return this.sDRageReduc;
    }
}

class OverPower extends Ability {
    public OverPower(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo, Ability ss) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Overpower";
        description = "Instantly overpower the enemy, causing weapon damage plus 125. Only usable after the target dodges. The Overpower cannot be blocked, dodged or parried.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Overpower_.index;
        ss_ = ss;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        canBeDodged = false;
        canBeParried = false;
        canBeBlocked = false;
        cd = 5f - (2f * talents().getUnrelentingAssault()); // In Seconds
        setGCDTime(Math.min(1.5f, cd));
        rageCost = 5f - (talents().getFocusedRage() * 1f);
        stanceOkArms = true;
        damageBase = combatFactors.normalizedMhWeaponDmg();
        damageBonus = 1f + (0.1f * talents().getUnrelentingAssault());
        bonusCritChance = 0.25f * talents().getImprovedOverpower();
        useReact = true; // can't plan for this
        initialize();
    }

    private Ability ss_;

    public float getActivates(float yellowAttacksThatDodgeOverDur, float yellowAttacksThatParryOverDur, float ssActs) {
        if (abilIterater != -1 && !calcOpts.getMaintenance()[abilIterater]) {
            return 0f;
        }

        float acts = 0f;
        float latentGCD = (1.5f + calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact()));

        float dodge = whiteattacks.mhAtkTable.dodge;
        float parry = (talents().isGlyphOfOverpower() ? whiteattacks.mhAtkTable.parry : 0f);

        // Chance to activate: Dodges + (if glyphed) Parries
        if (dodge + parry > 0f) {
            float whtHitsOverDur = fightDuration() / whiteattacks.mhEffectiveSpeed()
                    + (combatFactors.useOH ? fightDuration() / whiteattacks.ohEffectiveSpeed() : 0f)
                    + ssActs;

            float dodgesoverDur = 0f
                    + whtHitsOverDur * (dodge + parry)
                    + (dodge > 0 ? yellowAttacksThatDodgeOverDur : 0)
                    + (parry > 0 ? yellowAttacksThatParryOverDur : 0);

            acts += Math.max(0f, dodgesoverDur * (1f - whiteattacks.rageSlip(fightDuration() / dodgesoverDur, rageCost)));
        }

        return acts;
    }
}

class TasteForBlood extends Ability {
    public TasteForBlood(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Taste for Blood";
        description = "Instantly overpower the enemy, causing weapon damage plus 125. Only usable after the target takes Rend Damage. The Overpower cannot be blocked, dodged or parried.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.TasteForBlood_.index;
        reqTalent = true;
        talent2ChksValue = talents().getTasteForBlood();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        canBeDodged = false;
        canBeParried = false;
        canBeBlocked = false;
        setGCDTime(Math.min(1.5f, 5f - (2f * talents().getUnrelentingAssault())));
        cd = 6f; // In Seconds
        rageCost = 5f - (talents().getFocusedRage() * 1f);
        stanceOkArms = true;
        damageBase = combatFactors.normalizedMhWeaponDmg();
        damageBonus = 1f + (0.1f * talents().getUnrelentingAssault());
        bonusCritChance = 0.25f * talents().getImprovedOverpower();
        initialize();
    }

    @Override
    protected float activatesOverride() {
        float acts = 0f;

        // Chance to activate Requires Rend
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Rend_.index]) {
            acts = super.activatesOverride() * (1f / 3f * talents().getTasteForBlood());
        }

        return acts;
    }
}

class Bladestorm extends Ability {
    public Bladestorm(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo, Ability ww) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        ww_ = ww;
        name = "Bladestorm";
        description = "Instantly Whirlwind up to 4 nearby targets and for the next 6 sec you will perform a whirlwind attack every 1 sec. While under the effects of Bladestorm, you can move but cannot perform any other abilities but you do not feel pity or remorse or fear and you cannot be stopped unless killed.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Bladestorm_.index;
        reqTalent = true;
        talent2ChksValue = talents().getBladestorm();
        reqMeleeWeap = true;
        reqMeleeRange = true;
        maxRange = ww_.maxRange; // In Yards
        targets = ww_.targets; // Handled in WW
        damageBase = ww_.damageBase;
        cd = 90f - (talents().isGlyphOfBladestorm() ? 15f : 0f); // In Seconds
        rageCost = 25f - (talents().getFocusedRage() * 1f);
        setCastTime(6f); // In Seconds // Channeled
        setGCDTime(castTime());
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        swingsOffHand = true;
        swingsPerActivate = 7f;
        initialize();
    }

    public Ability ww_;

    @Override
    public float damageOnUseOverride() {
        if (!validated()) {
            return 0f;
        }
        float damage = ww_.damageOnUseOverride(); // WW.DamageOnUseOverride;
        return damage * 7f; // it WW's 7 times
    }
}

class Swordspec extends Ability {
    public Swordspec(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Sword Specialization";
        description = "Gives a (1*Pts)% chance to get an extra attack on the same target after hitting your target with your Sword. This effect cannot occur more than once every 6 seconds.";
        reqTalent = true;
        talent2ChksValue = talents().getSwordSpecialization();
        cd = 6f; // In Seconds
        stanceOkFury = stanceOkArms = stanceOkDef = true;
        damageBase = combatFactors.avgMhWeaponDmgUnhasted();
        rageCost = -whiteattacks.mhSwingRage();
        usesGCD = false;
        initialize();
        setMhAtkTable(whiteattacks.mhAtkTable);
    }

    @Override
    public boolean validated() {
        return super.validated()
                && (combatFactors.cMhItemType == ItemType.TwoHandSword
                        || combatFactors.cMhItemType == ItemType.OneHandSword);
    }

    private static final SpecialEffect[] seSwordSpec = {
            null,
            new SpecialEffect(Trigger.MeleeHit, null, 0f, 6, 1 * 0.02f),
            new SpecialEffect(Trigger.MeleeHit, null, 0f, 6, 2 * 0.02f),
            new SpecialEffect(Trigger.MeleeHit, null, 0f, 6, 3 * 0.02f),
            new SpecialEffect(Trigger.MeleeHit, null, 0f, 6, 4 * 0.02f),
            new SpecialEffect(Trigger.MeleeHit, null, 0f, 6, 5 * 0.02f),
    };

    static void clearStaticEffectCaches() {
        for (SpecialEffect e : seSwordSpec) if (e != null) e.clearInterpolatorCache();
    }

    public float getActivates(float yellowsThatLandOverDur, float heroic, float cleave) {
        if (combatFactors.cMhItemType != ItemType.TwoHandSword && combatFactors.cMhItemType != ItemType.OneHandSword) {
            return 0.0f;
        }
        // This attack doesnt consume GCDs and doesn't affect the swing timer
        whiteattacks.hsOverridesOverDur = heroic;
        whiteattacks.clOverridesOverDur = cleave;
        float rawActs = (yellowsThatLandOverDur + whiteattacks.landedAtksOverDur()) / fightDuration();
        float effectActs = seSwordSpec[talents().getSwordSpecialization()].getAverageProcsPerSecond(rawActs, 1f, combatFactors.cMhItemSpeed, fightDuration());
        effectActs *= fightDuration();
        return effectActs;
    }

    @Override
    public String genTooltip(float acts, float ttldpsperc) {
        float misses = getXActs(AttackTableSelector.Missed, acts), missesPerc = (acts == 0f ? 0f : misses / acts);
        float dodges = getXActs(AttackTableSelector.Dodged, acts), dodgesPerc = (acts == 0f ? 0f : dodges / acts);
        float parrys = getXActs(AttackTableSelector.Parried, acts), parrysPerc = (acts == 0f ? 0f : parrys / acts);
        float blocks = getXActs(AttackTableSelector.Blocked, acts), blocksPerc = (acts == 0f ? 0f : blocks / acts);
        float glance = getXActs(AttackTableSelector.Glance, acts), glancePerc = (acts == 0f ? 0f : glance / acts);
        float crits = getXActs(AttackTableSelector.Crit, acts), critsPerc = (acts == 0f ? 0f : crits / acts);
        float hits = getXActs(AttackTableSelector.Hit, acts), hitsPerc = (acts == 0f ? 0f : hits / acts);

        boolean showmisss = misses > 0f;
        boolean showdodge = canBeDodged && dodges > 0f;
        boolean showparry = canBeParried && parrys > 0f;
        boolean showblock = canBeBlocked && blocks > 0f;
        boolean showglance = true && glance > 0f;
        boolean showcrits = canCrit && crits > 0f;

        String tooltip = "*" + name
                + DPSWarrFormat.NL + "Cast Time: " + (castTime() != -1 ? rawr.models.mage.NetFormat.netFloatToString(castTime()) : "Instant")
                + ", CD: " + (cd != -1 ? rawr.models.mage.NetFormat.netFloatToString(cd) : "None")
                + ", Rage Generated: " + (rageCost != -1 ? rawr.models.mage.NetFormat.netFloatToString(-1f * rageCost) : "None")
                + DPSWarrFormat.NL + DPSWarrFormat.NL + DPSWarrFormat.zeroPadFixed(acts, 3, 2) + " Activates over Attack Table:"
                + (showmisss ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(misses, 3, 2) + " : " + DPSWarrFormat.percentCustom(missesPerc, 2, 2) + " : Missed " : "")
                + (showdodge ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(dodges, 3, 2) + " : " + DPSWarrFormat.percentCustom(dodgesPerc, 2, 2) + " : Dodged " : "")
                + (showparry ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(parrys, 3, 2) + " : " + DPSWarrFormat.percentCustom(parrysPerc, 2, 2) + " : Parried " : "")
                + (showblock ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(blocks, 3, 2) + " : " + DPSWarrFormat.percentCustom(blocksPerc, 2, 2) + " : Blocked " : "")
                + (showglance ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(glance, 3, 2) + " : " + DPSWarrFormat.percentCustom(glancePerc, 2, 2) + " : Glanced " : "")
                + (showcrits ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(crits, 3, 2) + " : " + DPSWarrFormat.percentCustom(critsPerc, 2, 2) + " : Crit " : "")
                + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(hits, 3, 2) + " : " + DPSWarrFormat.percentCustom(hitsPerc, 2, 2) + " : Hit "
                + DPSWarrFormat.NL
                + DPSWarrFormat.NL + "Targets Hit: " + (targets != -1 ? DPSWarrFormat.fixed(avgTargets(), 2) : "None")
                + DPSWarrFormat.NL + "DPS: " + (getDPS(acts) > 0 ? DPSWarrFormat.fixed(getDPS(acts), 2) : "None")
                + DPSWarrFormat.NL + "Percentage of Total DPS: " + (ttldpsperc > 0 ? DPSWarrFormat.percentCustom(ttldpsperc, 2, 2) : "None");

        return tooltip;
    }
}

class Execute extends Ability {
    public Execute(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Execute";
        description = "Attempt to finish off a wounded foe, causing (1456+AP*0.2) damage and converting each extra point of rage into 38 additional damage. Only usable on enemies that have less than 20% health.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.ExecuteSpam_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = 1.5f;
        rageCost = 15f - (talents().getImprovedExecute() * 2.5f) - (talents().getFocusedRage() * 1f);
        setFreeRage(0f);
        stanceOkFury = stanceOkArms = true;
        percTimeUnder20 = 0.17f;
        initialize();
    }

    public boolean getReqMeleeWeap() {
        return this.reqMeleeWeap;
    }

    public boolean getReqMeleeRange() {
        return this.reqMeleeRange;
    }

    private float FREERAGE;

    public float getFreeRage() {
        return FREERAGE;
    }

    public void setFreeRage(float value) {
        FREERAGE = value;
        usedExtraRage = Math.max(0f, Math.min(30f, getFreeRage()));
    }

    public float usedExtraRage;
    public float percTimeUnder20;

    @Override
    protected float activatesOverride() {
        return super.activatesOverride() * percTimeUnder20;
    }

    @Override
    public float damageOverride() {
        float executeRage = usedExtraRage + (talents().isGlyphOfExecution() ? 10.00f : 0.00f);

        float damage = 1456f + statS.getAttackPower() * 0.2f + executeRage * 38f;

        return damage * avgTargets();
    }

    @Override
    public float getRageUseOverDur(float acts) {
        if (!validated()) {
            return 0f;
        }
        return acts * (rageCost + usedExtraRage);
    }
}

class Slam extends Ability {
    public Slam(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Slam";
        description = "Slams the opponent, causing weapon damage plus 250.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Slam_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = 1.5f;
        rageCost = 15f - (talents().getFocusedRage() * 1f);
        setCastTime((1.5f - (talents().getImprovedSlam() * 0.5f))); // In Seconds
        stanceOkArms = stanceOkDef = true;
        damageBase = combatFactors.avgMhWeaponDmgUnhasted() + 250f;
        damageBonus = (1f + talents().getUnendingFury() * 0.02f) * (1f + statS.getBonusWarrior_T7_2P_SlamDamage());
        bonusCritChance = statS.getBonusWarrior_T9_4P_SLHSCritIncrease();
        initialize();
    }
}

class Rend extends DoT {
    public Rend(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "Rend";
        description = "Wounds the target causing them to bleed for 380 damage plus an additional (0.2*5*MWB+mwb/2+AP/14*MWS) (based on weapon damage) over 15 sec. If used while your target is above 75% health, Rend does 35% more damage.";
        abilIterater = CalculationOptionsDPSWarr.Maintenances.Rend_.index;
        reqMeleeWeap = true;
        reqMeleeRange = true;
        canCrit = false;
        duration = 15f + (talents().isGlyphOfRending() ? 6f : 0f); // In Seconds
        cd = duration + 3f;
        timeBtwnTicks = 3f; // In Seconds
        rageCost = 10f - (talents().getFocusedRage() * 1f);
        stanceOkArms = stanceOkDef = true;
        damageBase = 380f;
        damageBonus = (1f + 0.10f * talents().getImprovedRend());
        initialize();
    }

    protected float addMisses;
    protected float addDodges;
    protected float addParrys;

    @Override
    protected float activatesOverride() {
        float result = 0f;
        float base = super.activatesOverride();
        addMisses = (mhAtkTable().miss > 0) ? base * mhAtkTable().miss : 0f;
        addDodges = (mhAtkTable().dodge > 0) ? base * mhAtkTable().dodge : 0f;
        addParrys = (mhAtkTable().parry > 0) ? base * mhAtkTable().parry : 0f;

        result = base + addMisses + addDodges + addParrys;

        return result;
    }

    @Override
    public float tickSize() {
        if (!validated()) {
            return 0f;
        }

        float dmgBonusBase = (statS.getAttackPower() * combatFactors.cMhItemSpeed) / 14f
                + (combatFactors.MH.getMaxDamage() + combatFactors.MH.getMinDamage()) / 2f;
        float dmgBonusU75 = 0.75f * 1.00f;
        float dmgBonusO75 = 0.25f * 1.35f;
        float dmgMod = (1f + statS.getBonusBleedDamageMultiplier())
                * (1f + statS.getBonusDamageMultiplier())
                * damageBonus;
        float glyphMOD = talents().isGlyphOfRending() ? 7f / 5f : 1f;

        float damageUnder75 = (damageBase + dmgBonusBase) * dmgBonusU75;
        float damageOver75 = (damageBase + dmgBonusBase) * dmgBonusO75;

        float theDamage = (damageUnder75 + damageOver75) * dmgMod;

        float tickSize = (theDamage * glyphMOD) / numTicks();

        return tickSize;
    }

    @Override
    public float getDPS(float acts) {
        float dmgonuse = tickSize();
        float numticks = numTicks() * (acts - addMisses - addDodges - addParrys);
        float result = getDmgOverTickingTime(acts) / fightDuration();
        return result;
    }
}

class FakeWhite extends Ability {
    public FakeWhite(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = c; statS = s; combatFactors = cf; whiteattacks = wa; calcOpts = co; bossOpts = bo;
        name = "MH White Swing";
        description = "White Damage";
        reqMeleeWeap = true;
        reqMeleeRange = true;
        cd = whiteattacks.mhEffectiveSpeed();
        rageCost = whiteattacks.mhSwingRage();
        stanceOkArms = stanceOkFury = stanceOkDef = true;
        damageBase = whiteattacks.mhDamageOnUse();
        initialize();
        setMhAtkTable(whiteattacks.mhAtkTable);
    }
}
