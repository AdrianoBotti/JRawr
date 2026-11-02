// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Skills.Base.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// WhiteAttacks + the templated Ability base classes. The C# `{ get; protected set; }` properties
// are public fields; cached getters keep their -1 sentinels. Non-Silverlight branches only.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;

public class WhiteAttacks {
    public WhiteAttacks(Character character, Stats stats, CombatFactors cf, CalculationOptionsDPSWarr calcOpts, BossOptions bossOpts) {
        char_ = character;
        statS = stats;
        talents = char_.getWarriorTalents() == null ? new WarriorTalents() : char_.getWarriorTalents();
        combatFactors = cf;
        this.calcOpts = calcOpts;
        this.bossOpts = bossOpts;
        mhAtkTable = new AttackTable(char_, statS, combatFactors, calcOpts, bossOpts, true, false, false);
        ohAtkTable = new AttackTable(char_, statS, combatFactors, calcOpts, bossOpts, false, false, false);
        fightDuration = calcOpts.getDuration();
        hsOverridesOverDur = 0f;
        clOverridesOverDur = 0f;
        slamActsOverDur = 0f;
        this.uwProcValueMh = combatFactors.cMhItemSpeed * talents.getUnbridledWrath() / 20.0f;
        this.uwProcValueOh = combatFactors.cOhItemSpeed * talents.getUnbridledWrath() / 20.0f;
    }

    public void invalidateCache() {
        this.mhDamageOnUse = this.mhSwingRage = this.ohDamageOnUse = this.ohSwingRage = -1f;
    }

    private final Character char_;
    private final Stats statS;
    private final WarriorTalents talents;
    private final CombatFactors combatFactors;
    private final CalculationOptionsDPSWarr calcOpts;
    private final BossOptions bossOpts;
    public final AttackTable mhAtkTable;
    public final AttackTable ohAtkTable;
    private final float uwProcValueMh;
    private final float uwProcValueOh;

    public float mhuwProcValue() {
        return this.uwProcValueMh;
    }

    public float ohuwProcValue() {
        return this.uwProcValueOh;
    }

    private final float fightDuration;

    private float avgTargets() {
        if (calcOpts.getMultipleTargets()) {
            return 1f + (Math.min(calcOpts.getMultipleTargetsMax(), 1f) - 1f) * calcOpts.getMultipleTargetsPerc() / 100f + statS.getBonusTargets();
        } else {
            return 1f;
        }
    }

    public float hsOverridesOverDur;
    public float clOverridesOverDur;
    public float slamActsOverDur;

    private float slamFreqSpdMod() {
        return (slamActsOverDur == 0f ? 0f : ((1.5f - (0.5f * talents.getImprovedSlam())) * (slamActsOverDur / fightDuration)));
    }

    public float mhEffectiveSpeed() {
        return combatFactors.mhSpeed() + slamFreqSpdMod();
    }

    public float mhDamage() {
        return combatFactors.avgMhWeaponDmgUnhasted() * avgTargets();
    }

    private float mhDamageOnUse = -1f;

    public float mhDamageOnUse() {
        if (this.mhDamageOnUse == -1f) {
            float dmg = mhDamage();
            dmg *= combatFactors.damageBonus();
            dmg *= combatFactors.damageReduction();

            float dmgDrop = (1f
                    - mhAtkTable.miss
                    - mhAtkTable.dodge
                    - mhAtkTable.parry
                    - mhAtkTable.glance
                    - mhAtkTable.block
                    - mhAtkTable.crit);

            float dmgGlance = dmg * mhAtkTable.glance * combatFactors.reducWhGlancedDmg();
            float dmgBlock = dmg * mhAtkTable.block * combatFactors.reducWhBlockedDmg();
            float dmgCrit = dmg * mhAtkTable.crit * (1f + combatFactors.bonusWhiteCritDmg());

            dmg *= dmgDrop;

            dmg += dmgGlance + dmgBlock + dmgCrit;

            this.mhDamageOnUse = dmg;
        }
        return this.mhDamageOnUse;
    }

    public float avgMhDamageOnUse() {
        return mhDamageOnUse() * mhActivates();
    }

    public float mhActivates() {
        if (mhEffectiveSpeed() != 0) {
            float f = fightDuration / mhEffectiveSpeed();
            return f - hsOverridesOverDur - clOverridesOverDur;
        } else {
            return 0f;
        }
    }

    public float mhActivatesNoHS() {
        if (mhEffectiveSpeed() != 0) {
            return fightDuration / mhEffectiveSpeed();
        } else {
            return 0f;
        }
    }

    public float mhDPS() {
        return avgMhDamageOnUse() / fightDuration;
    }

    public float ohEffectiveSpeed() {
        return combatFactors.ohSpeed() + slamFreqSpdMod();
    }

    public float ohDamage() {
        return combatFactors.avgOhWeaponDmgUnhasted() * avgTargets();
    }

    private float ohDamageOnUse = -1f;

    public float ohDamageOnUse() {
        if (this.ohDamageOnUse == -1f) {
            float dmg = ohDamage();
            dmg *= combatFactors.damageBonus();
            dmg *= combatFactors.damageReduction();

            float dmgDrop = (1f
                    - ohAtkTable.miss
                    - ohAtkTable.dodge
                    - ohAtkTable.parry
                    - ohAtkTable.glance
                    - ohAtkTable.block
                    - ohAtkTable.crit);

            float dmgGlance = dmg * ohAtkTable.glance * combatFactors.reducWhGlancedDmg();
            float dmgBlock = dmg * ohAtkTable.block * combatFactors.reducWhBlockedDmg();
            float dmgCrit = dmg * ohAtkTable.crit * (1f + combatFactors.bonusWhiteCritDmg());

            dmg *= dmgDrop;

            dmg += dmgGlance + dmgBlock + dmgCrit;

            this.ohDamageOnUse = dmg;
        }
        return this.ohDamageOnUse;
    }

    public float avgOhDamageOnUse() {
        return ohDamageOnUse() * ohActivates();
    }

    public float ohActivates() {
        if (ohEffectiveSpeed() > 0f) return fightDuration / ohEffectiveSpeed();
        else return 0;
    }

    public float ohDPS() {
        return avgOhDamageOnUse() / fightDuration;
    }

    private float mhSwingRage = -1f;

    public float mhSwingRage() {
        if (this.mhSwingRage == -1f) {
            float s = combatFactors.cMhItemSpeed;
            float based = combatFactors.avgMhWeaponDmgUnhasted() * combatFactors.damageBonus() * combatFactors.damageReduction();

            this.mhSwingRage = rageFormula(based, 3.5f * s) * (mhAtkTable.hit + mhAtkTable.dodge + mhAtkTable.parry)
                    + rageFormula(based * combatFactors.reducWhGlancedDmg(), 3.5f * s) * (mhAtkTable.glance)
                    + rageFormula(based * (1f + combatFactors.bonusWhiteCritDmg()), 7.0f * s) * mhAtkTable.crit;
        }
        return this.mhSwingRage;
    }

    private float ohSwingRage = -1f;

    public float ohSwingRage() {
        if (this.ohSwingRage == -1f) {
            float s = combatFactors.cOhItemSpeed;
            float based = combatFactors.avgOhWeaponDmgUnhasted() * combatFactors.damageBonus() * combatFactors.damageReduction();
            this.ohSwingRage = rageFormula(based, 1.75f * s) * (ohAtkTable.hit + ohAtkTable.dodge + ohAtkTable.parry)
                    + rageFormula(based * combatFactors.reducWhGlancedDmg(), 1.75f * s) * ohAtkTable.glance
                    + rageFormula(based * (1f + combatFactors.bonusWhiteCritDmg()), 3.5f * s) * ohAtkTable.crit;
        }
        return this.ohSwingRage;
    }

    public float mhRageGenOverDur() {
        return mhActivates() * (mhSwingRage() + this.uwProcValueMh);
    }

    public float mhRageGenOverDurNoHS() {
        return mhActivatesNoHS() * (mhSwingRage() + this.uwProcValueMh);
    }

    public float ohRageGenOverDur() {
        if (combatFactors.useOH) {
            return ohActivates() * (ohSwingRage() + this.uwProcValueOh);
        }
        return 0f;
    }

    private float mhRageRatio() {
        return mhRageGenOverDur() / (mhRageGenOverDur() + ohRageGenOverDur());
    }

    public float whiteRageGenOverDur() {
        return mhRageGenOverDur() + ohRageGenOverDur();
    }

    public float whiteRageGenOverDurNoHS() {
        return mhRageGenOverDurNoHS() + ohRageGenOverDur();
    }

    private static final float c_const = 0.016545334215751158173395102581072f; //7.5f / 453.3f;
    private static final float c_const2 = 0.033090668431502316346790205162144f; // 2*c_const
    private static final float c_const3 = 0.049636002647253474520185307743216f; // 3*c_const

    private float rageFormula(float d, float fs) {
        return (4f + talents.getEndlessRage()) / 4f * ((fs > c_const3 * d) ? (c_const2 * d) : (c_const * d + fs) / 2.0f);
    }

    public float landedAtksOverDur() {
        return landedAtksOverDurMH() + landedAtksOverDurOH();
    }

    public float landedAtksOverDurMH() {
        return mhActivates() * mhAtkTable.anyLand();
    }

    public float landedAtksOverDurOH() {
        return (combatFactors.useOH ? ohActivates() * ohAtkTable.anyLand() : 0f);
    }

    public float criticalAtksOverDurMH() {
        return mhActivates() * mhAtkTable.crit;
    }

    public float criticalAtksOverDurOH() {
        return (combatFactors.useOH ? ohActivates() * ohAtkTable.crit : 0f);
    }

    public float rageSlip(float abilInterval, float rageCost) {
        if (!combatFactors.useOH && mhActivates() <= 0f) {
            return 0f;
        }
        return (mhAtkTable.miss * rageCost) / (abilInterval * ((mhActivates() * (mhSwingRage() + this.uwProcValueMh) + (combatFactors.useOH ? ohActivates() * (ohSwingRage() + this.uwProcValueOh) : 0f)) / fightDuration));
    }

    public float getXActs(AttackTableSelector i, float acts, boolean isMH) {
        AttackTable table = (isMH ? mhAtkTable : ohAtkTable);
        float retVal = 0f;
        switch (i) {
            case Missed: { retVal = acts * table.miss; break; }
            case Dodged: { retVal = acts * table.dodge; break; }
            case Parried: { retVal = acts * table.parry; break; }
            case Blocked: { retVal = acts * table.block; break; }
            case Crit: { retVal = acts * table.crit; break; }
            case Glance: { retVal = acts * table.glance; break; }
            case Hit: { retVal = acts * table.hit; break; }
            default: { break; }
        }
        return retVal;
    }

    public String genTooltip(float ttldpsMH, float ttldpsOH, float ttldps) {
        float acts = mhActivates();
        float misses = getXActs(AttackTableSelector.Missed, acts, true), missesPerc = (acts == 0f ? 0f : misses / acts);
        float dodges = getXActs(AttackTableSelector.Dodged, acts, true), dodgesPerc = (acts == 0f ? 0f : dodges / acts);
        float parrys = getXActs(AttackTableSelector.Parried, acts, true), parrysPerc = (acts == 0f ? 0f : parrys / acts);
        float blocks = getXActs(AttackTableSelector.Blocked, acts, true), blocksPerc = (acts == 0f ? 0f : blocks / acts);
        float crits = getXActs(AttackTableSelector.Crit, acts, true), critsPerc = (acts == 0f ? 0f : crits / acts);
        float glncs = getXActs(AttackTableSelector.Glance, acts, true), glncsPerc = (acts == 0f ? 0f : glncs / acts);
        float hits = getXActs(AttackTableSelector.Hit, acts, true), hitsPerc = (acts == 0f ? 0f : hits / acts);

        boolean showmisss = misses > 0f;
        boolean showdodge = dodges > 0f;
        boolean showparry = parrys > 0f;
        boolean showblock = blocks > 0f;
        boolean showcrits = crits > 0f;

        String tooltip = "*" + "White Damage (Main Hand)"
                + DPSWarrFormat.NL + "Cast Time: Instant"
                + ", CD: " + (mhEffectiveSpeed() != -1 ? DPSWarrFormat.fixed(mhEffectiveSpeed(), 2) : "None")
                + ", Rage Generated: " + (mhSwingRage() != -1 ? DPSWarrFormat.fixed(mhSwingRage(), 2) : "None")
                + DPSWarrFormat.NL + DPSWarrFormat.NL + DPSWarrFormat.zeroPadFixed(acts, 3, 2) + " Activates over Attack Table:"
                + (showmisss ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(misses, 3, 2) + " : " + DPSWarrFormat.percentCustom(missesPerc, 2, 2) + " : Missed " : "")
                + (showdodge ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(dodges, 3, 2) + " : " + DPSWarrFormat.percentCustom(dodgesPerc, 2, 2) + " : Dodged " : "")
                + (showparry ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(parrys, 3, 2) + " : " + DPSWarrFormat.percentCustom(parrysPerc, 2, 2) + " : Parried " : "")
                + (showblock ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(blocks, 3, 2) + " : " + DPSWarrFormat.percentCustom(blocksPerc, 2, 2) + " : Blocked " : "")
                + (showcrits ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(crits, 3, 2) + " : " + DPSWarrFormat.percentCustom(critsPerc, 2, 2) + " : Crit " : "")
                + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(glncs, 3, 2) + " : " + DPSWarrFormat.percentCustom(glncsPerc, 2, 2) + " : Glanced "
                + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(hits, 3, 2) + " : " + DPSWarrFormat.percentCustom(hitsPerc, 2, 2) + " : Hit "
                + DPSWarrFormat.NL
                + DPSWarrFormat.NL + "Targets Hit: " + DPSWarrFormat.fixed(avgTargets(), 2)
                + DPSWarrFormat.NL + "DPS: " + (ttldpsMH > 0 ? DPSWarrFormat.fixed(ttldpsMH, 2) : "None")
                + DPSWarrFormat.NL + "Percentage of Total DPS: " + (ttldpsMH > 0 ? DPSWarrFormat.percentCustom(ttldpsMH / ttldps, 2, 2) : "None");

        if (combatFactors.useOH) {
            acts = ohActivates();
            misses = getXActs(AttackTableSelector.Missed, acts, false); missesPerc = (acts == 0f ? 0f : misses / acts);
            dodges = getXActs(AttackTableSelector.Dodged, acts, false); dodgesPerc = (acts == 0f ? 0f : dodges / acts);
            parrys = getXActs(AttackTableSelector.Parried, acts, false); parrysPerc = (acts == 0f ? 0f : parrys / acts);
            blocks = getXActs(AttackTableSelector.Blocked, acts, false); blocksPerc = (acts == 0f ? 0f : blocks / acts);
            crits = getXActs(AttackTableSelector.Crit, acts, false); critsPerc = (acts == 0f ? 0f : crits / acts);
            glncs = getXActs(AttackTableSelector.Glance, acts, false); glncsPerc = (acts == 0f ? 0f : glncs / acts);
            hits = getXActs(AttackTableSelector.Hit, acts, false); hitsPerc = (acts == 0f ? 0f : hits / acts);

            showmisss = misses > 0f;
            showdodge = dodges > 0f;
            showparry = parrys > 0f;
            showblock = blocks > 0f;
            showcrits = crits > 0f;

            tooltip += DPSWarrFormat.NL + DPSWarrFormat.NL + "White Damage (Off Hand)"
                    + DPSWarrFormat.NL + "Cast Time: Instant"
                    + ", CD: " + (ohEffectiveSpeed() != -1 ? DPSWarrFormat.fixed(ohEffectiveSpeed(), 2) : "None")
                    + ", Rage Generated: " + (ohSwingRage() != -1 ? DPSWarrFormat.fixed(ohSwingRage(), 2) : "None")
                    + DPSWarrFormat.NL + DPSWarrFormat.NL + DPSWarrFormat.zeroPadFixed(acts, 3, 2) + " Activates over Attack Table:"
                    + (showmisss ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(misses, 3, 2) + " : " + DPSWarrFormat.percentCustom(missesPerc, 2, 2) + " : Missed " : "")
                    + (showdodge ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(dodges, 3, 2) + " : " + DPSWarrFormat.percentCustom(dodgesPerc, 2, 2) + " : Dodged " : "")
                    + (showparry ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(parrys, 3, 2) + " : " + DPSWarrFormat.percentCustom(parrysPerc, 2, 2) + " : Parried " : "")
                    + (showblock ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(blocks, 3, 2) + " : " + DPSWarrFormat.percentCustom(blocksPerc, 2, 2) + " : Blocked " : "")
                    + (showcrits ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(crits, 3, 2) + " : " + DPSWarrFormat.percentCustom(critsPerc, 2, 2) + " : Crit " : "")
                    + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(glncs, 3, 2) + " : " + DPSWarrFormat.percentCustom(glncsPerc, 2, 2) + " : Glanced "
                    + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(hits, 3, 2) + " : " + DPSWarrFormat.percentCustom(hitsPerc, 2, 2) + " : Hit "
                    + DPSWarrFormat.NL
                    + DPSWarrFormat.NL + "Targets Hit: " + DPSWarrFormat.fixed(avgTargets(), 2)
                    + DPSWarrFormat.NL + "DPS: " + (ttldpsOH > 0 ? DPSWarrFormat.fixed(ttldpsOH, 2) : "None")
                    + DPSWarrFormat.NL + "Percentage of Total DPS: " + (ttldpsOH > 0 ? DPSWarrFormat.percentCustom(ttldpsOH / ttldps, 2, 2) : "None");
        }
        return tooltip;
    }
}

enum AttackTableSelector {
    Missed(0), Dodged(1), Parried(2), Blocked(3), Crit(4), Glance(5), Hit(6);

    public final int value;

    AttackTableSelector(int value) {
        this.value = value;
    }
}

abstract class Ability {
    public Ability() {
        char_ = null;
        statS = null;
        combatFactors = null;
        setMhAtkTable(null);
        setOhAtkTable(null);
        whiteattacks = null;
        calcOpts = null;
        bossOpts = null;
        name = "Invalid";
        description = "Invalid";
        reqTalent = false;
        canBeDodged = true;
        canBeParried = true;
        canBeBlocked = true;
        canCrit = true;
        talent2ChksValue = 0;
        abilIterater = -1;
        reqMeleeWeap = false;
        reqMeleeRange = false;
        reqMultiTargs = false;
        targets = 1f;
        maxRange = 5f; // In Yards
        cd = -1f; // In Seconds
        duration = 0; // In Seconds
        rageCost = 0;
        setCastTime(-1f); // In Seconds
        setGCDTime(1.5f); // default GCD size
        stanceOkFury = false;
        stanceOkArms = false;
        stanceOkDef = false;
        useReact = false;
        damageBase = 0f;
        damageBonus = 1f;
        healingBase = 0f;
        healingBonus = 1f;
        bonusCritChance = 0.00f;
        useSpellHit = false;
        useHitTable = true;
        validatedSet = false;
        swingsOffHand = false;
        swingsPerActivate = 1f;
        usesGCD = true;
    }

    public static final Ability NULL = new NullAbility();

    public int abilIterater;

    public String name;
    public String description;
    protected boolean reqTalent;
    protected int talent2ChksValue;
    public boolean reqMeleeWeap;
    public boolean reqMeleeRange;
    protected boolean reqMultiTargs;
    private float avgTargets = -1f;

    public float avgTargets() {
        if (this.avgTargets == -1f) {
            this.avgTargets = 1f + (calcOpts.getMultipleTargets() ? statS.getBonusTargets() + calcOpts.getMultipleTargetsPerc() / 100f * (Math.min(calcOpts.getMultipleTargetsMax(), targets) - 1f) : 0f);
        }
        return this.avgTargets;
    }

    public float targets;
    public boolean canBeDodged;
    public boolean canBeParried;
    public boolean canBeBlocked;
    public boolean canCrit;
    public float minRange; // In Yards
    public float maxRange; // In Yards
    public float cd;
    public float duration; // In Seconds
    public float rageCost;
    protected float cachedCastTime = -1f;

    public float castTime() { // In Seconds
        return this.cachedCastTime;
    }

    protected void setCastTime(float value) {
        this.cachedCastTime = value;
        this.cachedUseTime = (calcOpts == null ? 0f : calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact()))
                + Math.min(Math.max(1.5f, value), this.cachedGcdTime);
    }

    protected float cachedGcdTime = 1.5f;

    public float gcdTime() { // In Seconds
        return this.cachedGcdTime;
    }

    protected void setGCDTime(float value) {
        this.cachedGcdTime = value;
        this.cachedUseTime = (calcOpts != null ? calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact()) : 0f)
                + Math.min(Math.max(1.5f, this.cachedCastTime), value);
    }

    protected float cachedUseTime = 0;

    public float useTime() {
        return this.cachedUseTime;
    }

    public float damageBase; // Base Damage Value (500 = 500.00 Damage)
    protected float damageBonus; // Percentage Based Damage Bonus (1.5 = 150% damage)
    protected float healingBase;
    protected float healingBonus;
    public float bonusCritChance;
    protected boolean stanceOkFury;
    protected boolean stanceOkArms;
    protected boolean stanceOkDef;
    protected boolean useReact;
    protected Character char_;
    protected WarriorTalents talents() {
        return char_.getWarriorTalents();
    }
    protected Stats statS;
    protected CombatFactors combatFactors;
    private CombatTable mhAtkTable;
    private CombatTable ohAtkTable;

    public CombatTable mhAtkTable() {
        return this.mhAtkTable;
    }

    protected void setMhAtkTable(CombatTable value) {
        this.mhAtkTable = value;
    }

    public CombatTable ohAtkTable() {
        return this.ohAtkTable;
    }

    protected void setOhAtkTable(CombatTable value) {
        this.ohAtkTable = value;
    }

    public WhiteAttacks whiteattacks;
    public CalculationOptionsDPSWarr calcOpts;
    protected BossOptions bossOpts;

    public float rageUseOverDur() {
        return (!validated() ? 0f : activates() * rageCost);
    }

    public boolean swingsOffHand;
    public float swingsPerActivate;

    protected float fightDuration() {
        return calcOpts.getDuration();
    }

    protected boolean useSpellHit;
    protected boolean useHitTable;
    public boolean isMaint;
    public boolean usesGCD;
    private boolean validatedSet = false;

    public boolean validated() {
        return validatedSet;
    }

    private boolean setValidation() {
        if (abilIterater != -1 && !calcOpts.getMaintenance()[abilIterater]) {
            validatedSet = false;
        } else if (reqTalent && talent2ChksValue < 1) {
            validatedSet = false;
        } else if (reqMeleeWeap && (char_.getMainHand() == null || char_.getMainHand().getMaxDamage() <= 0)) {
            validatedSet = false;
        } else if (reqMultiTargs && (!calcOpts.getMultipleTargets() || calcOpts.getMultipleTargetsPerc() == 0)) {
            validatedSet = false;
        } else if ((combatFactors.furyStance() && !stanceOkFury)
                || (!combatFactors.furyStance() && !stanceOkArms)) {
            validatedSet = false;
        } else validatedSet = true;

        return validatedSet;
    }

    public float activates() {
        return !validated() ? 0f : activatesOverride();
    }

    protected float activatesOverride() {
        float latentGCD = 1.5f + calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact());
        float gcdPerc = latentGCD / ((duration > cd ? duration : cd) + calcOpts.getLatency() + (useReact ? calcOpts.getReact() / 1000f : calcOpts.getAllowedReact()));
        if (rageCost > 0f) {
            return Math.max(0f, fightDuration() / (latentGCD / gcdPerc) * (1f - whiteattacks.rageSlip(latentGCD / gcdPerc, rageCost)));
        } else return fightDuration() / (latentGCD / gcdPerc);
    }

    protected float healing() {
        return !validated() ? 0f : healingBase * healingBonus;
    }

    protected float healingOnUse() {
        return healing() * combatFactors.healthBonus();
    }

    protected float damage() {
        return !validated() ? 0f : damageOverride();
    }

    public float damageOverride() {
        return Math.max(0f, damageBase * damageBonus * avgTargets());
    }

    public float damageOnUse() {
        return (validated() ? damageOnUseOverride() : 0f);
    }

    public float damageOnUseOverride() {
        float dmg = damage();
        dmg *= combatFactors.damageBonus();
        dmg *= combatFactors.damageReduction();

        float dmgDrop = (1f
                - mhAtkTable().miss
                - mhAtkTable().dodge
                - mhAtkTable().parry
                - mhAtkTable().glance
                - mhAtkTable().block
                - mhAtkTable().crit);

        float dmgGlance = dmg * mhAtkTable().glance * combatFactors.reducWhGlancedDmg();
        float dmgBlock = dmg * mhAtkTable().block * combatFactors.reducYwBlockedDmg();
        float dmgCrit = dmg * mhAtkTable().crit * (1f + combatFactors.bonusYellowCritDmg());

        dmg *= dmgDrop;

        dmg += /*dmgGlance +*/ dmgBlock + dmgCrit;

        return dmg;
    }

    protected void initialize() {
        if (!useSpellHit && useHitTable && canBeDodged && canCrit && bonusCritChance == 0f) {
            setMhAtkTable(combatFactors.attackTableBasicMH());
            setOhAtkTable(combatFactors.attackTableBasicOH());
        } else {
            setMhAtkTable(new AttackTable(char_, statS, combatFactors, calcOpts, bossOpts, this, true, useSpellHit, !useHitTable));
            setOhAtkTable(new AttackTable(char_, statS, combatFactors, calcOpts, bossOpts, this, false, useSpellHit, !useHitTable));
        }
        setValidation();
    }

    public float getRageUseOverDur(float acts) {
        if (!validated()) {
            return 0f;
        }
        return acts * rageCost;
    }

    public float getHealing() {
        if (!validated()) {
            return 0f;
        }
        return 0f;
    }

    public float getAvgDamageOnUse(float acts) {
        if (!validated()) {
            return 0f;
        }
        return damageOnUse() * acts;
    }

    public float getDPS(float acts) {
        if (!validated()) {
            return 0f;
        }
        return getAvgDamageOnUse(acts) / fightDuration();
    }

    public float getDPS(float acts, float perc) {
        if (!validated()) {
            return 0f;
        }
        return getAvgDamageOnUse(acts) / (fightDuration() * perc);
    }

    public float getAvgHealingOnUse(float acts) {
        if (!validated()) {
            return 0f;
        }
        return healingOnUse() * acts;
    }

    public float getHPS(float acts) {
        if (!validated()) {
            return 0f;
        }
        return getAvgHealingOnUse(acts) / fightDuration();
    }

    protected float getXActs(AttackTableSelector i, float acts) {
        float retVal = 0f;
        switch (i) {
            case Missed: { retVal = acts * mhAtkTable().miss; break; }
            case Dodged: { retVal = acts * mhAtkTable().dodge; break; }
            case Parried: { retVal = acts * mhAtkTable().parry; break; }
            case Blocked: { retVal = acts * mhAtkTable().block; break; }
            case Glance: { retVal = acts * mhAtkTable().glance; break; }
            case Crit: { retVal = acts * mhAtkTable().crit; break; }
            case Hit: { retVal = acts * mhAtkTable().hit; break; }
            default: { break; }
        }
        return retVal;
    }

    public String genTooltip(float acts, float ttldpsperc) {
        float misses = getXActs(AttackTableSelector.Missed, acts), missesPerc = (acts == 0f ? 0f : misses / acts);
        float dodges = getXActs(AttackTableSelector.Dodged, acts), dodgesPerc = (acts == 0f ? 0f : dodges / acts);
        float parrys = getXActs(AttackTableSelector.Parried, acts), parrysPerc = (acts == 0f ? 0f : parrys / acts);
        float blocks = getXActs(AttackTableSelector.Blocked, acts), blocksPerc = (acts == 0f ? 0f : blocks / acts);
        float crits = getXActs(AttackTableSelector.Crit, acts), critsPerc = (acts == 0f ? 0f : crits / acts);
        float hits = getXActs(AttackTableSelector.Hit, acts), hitsPerc = (acts == 0f ? 0f : hits / acts);

        boolean showmisss = misses > 0f;
        boolean showdodge = canBeDodged && dodges > 0f;
        boolean showparry = canBeParried && parrys > 0f;
        boolean showblock = canBeBlocked && blocks > 0f;
        boolean showcrits = canCrit && crits > 0f;

        String tooltip = "*" + name
                + DPSWarrFormat.NL + "Cast Time: " + (castTime() != -1 ? rawr.models.mage.NetFormat.netFloatToString(castTime()) : "Instant")
                + ", CD: " + (cd != -1 ? rawr.models.mage.NetFormat.netFloatToString(cd) : "None")
                + ", RageCost: " + (rageCost != -1 ? rawr.models.mage.NetFormat.netFloatToString(rageCost) : "None")
                + DPSWarrFormat.NL + DPSWarrFormat.NL + DPSWarrFormat.zeroPadFixed(acts, 3, 2) + " Activates over Attack Table:"
                + (showmisss ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(misses, 3, 2) + " : " + DPSWarrFormat.percentCustom(missesPerc, 2, 2) + " : Missed " : "")
                + (showdodge ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(dodges, 3, 2) + " : " + DPSWarrFormat.percentCustom(dodgesPerc, 2, 2) + " : Dodged " : "")
                + (showparry ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(parrys, 3, 2) + " : " + DPSWarrFormat.percentCustom(parrysPerc, 2, 2) + " : Parried " : "")
                + (showblock ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(blocks, 3, 2) + " : " + DPSWarrFormat.percentCustom(blocksPerc, 2, 2) + " : Blocked " : "")
                + (showcrits ? DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(crits, 3, 2) + " : " + DPSWarrFormat.percentCustom(critsPerc, 2, 2) + " : Crit " : "")
                + DPSWarrFormat.NL + "- " + DPSWarrFormat.zeroPadFixed(hits, 3, 2) + " : " + DPSWarrFormat.percentCustom(hitsPerc, 2, 2) + " : Hit "
                + DPSWarrFormat.NL
                + DPSWarrFormat.NL + "Targets Hit: " + (targets != -1 ? DPSWarrFormat.fixed(avgTargets(), 2) : "None")
                + DPSWarrFormat.NL + "DPS: " + (getDPS(acts) > 0 ? DPSWarrFormat.fixed(getDPS(acts), 2) : "None")
                + DPSWarrFormat.NL + "Percentage of Total DPS: " + (ttldpsperc > 0 ? DPSWarrFormat.percentCustom(ttldpsperc, 2, 2) : "None");

        return tooltip;
    }
}

class NullAbility extends Ability {
    public NullAbility() {
    }

    @Override
    public CombatTable mhAtkTable() {
        return CombatTable.NULL;
    }

    @Override
    public CombatTable ohAtkTable() {
        return CombatTable.NULL;
    }

    @Override
    public float rageUseOverDur() {
        return 0;
    }

    @Override
    protected float activatesOverride() {
        return 0;
    }

    @Override
    public float damageOnUseOverride() {
        return 0;
    }

    @Override
    public float damageOverride() {
        return 0;
    }

    @Override
    public String genTooltip(float acts, float ttldpsperc) {
        return "";
    }

    @Override
    public float getRageUseOverDur(float acts) {
        return 0;
    }

    @Override
    public boolean validated() {
        return false;
    }

    @Override
    public float activates() {
        return 0;
    }

    @Override
    public float getDPS(float acts) {
        return 0;
    }
}

class OnAttack extends Ability {
    public OnAttack() {
        overridesOverDur = 0f;
        usesGCD = false;
    }

    public float overridesOverDur;

    public float fullRageCost() {
        return rageCost + whiteattacks.mhSwingRage() - whiteattacks.mhuwProcValue() * mhAtkTable().anyLand();
    }

    @Override
    protected float activatesOverride() {
        if (!validated() || overridesOverDur <= 0f) {
            return 0f;
        }
        return overridesOverDur * (1f - whiteattacks.rageSlip(fightDuration() / overridesOverDur, rageCost));
    }

    public float dps() {
        return damageOnUse() * activatesOverride() / fightDuration();
    }
}

class DoT extends Ability {
    public DoT() {
    }

    public float timeBtwnTicks; // In Seconds

    public float tickSize() {
        return 0f;
    }

    public float ttlTickingTime() {
        return duration;
    }

    public float tickLength() {
        return timeBtwnTicks;
    }

    public float numTicks() {
        return ttlTickingTime() / tickLength();
    }

    public float dmgOverTickingTime() {
        return tickSize() * numTicks();
    }

    public float getDmgOverTickingTime(float acts) {
        return tickSize() * (numTicks() * acts);
    }

    @Override
    public float getDPS(float acts) {
        return getDmgOverTickingTime(acts) / fightDuration();
    }

    public float dps() {
        return tickSize() / tickLength();
    }
}

class BuffEffect extends Ability {
    public BuffEffect() {
        isMaint = true;
        effect = null;
        effect2 = null;
    }

    protected float addMisses;
    protected float addDodges;
    protected float addParrys;

    public SpecialEffect effect;
    public SpecialEffect effect2;

    public Stats averageStats() {
        if (!validated()) {
            return new Stats();
        }
        Stats bonus = (effect == null) ? new Stats() : effect.getAverageStats(0f, mhAtkTable().hit + mhAtkTable().crit, whiteattacks.mhEffectiveSpeed(), fightDuration());
        Stats b2 = (effect2 == null) ? new Stats() : effect2.getAverageStats(0f, mhAtkTable().hit + mhAtkTable().crit, whiteattacks.mhEffectiveSpeed(), fightDuration());
        return Stats.add(bonus, b2);
    }
}

// Unused (kept for parity with the original source file).
class VictoryRush extends Ability {
}

class HeroicThrow extends Ability {
}
