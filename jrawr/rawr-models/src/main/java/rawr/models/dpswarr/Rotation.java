// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Rotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# properties are methods where they are computed (Get/`{ get; }` caches keep their sentinels),
// `Char`/`StatS`/`Talents`/`CombatFactors`/`WhiteAtks`/`CalcOpts`/`BossOpts` are protected fields.
// Non-Silverlight branches only (CalcOpts). Environment.NewLine is "\n" on the Mono oracle.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.bosses.Impedance;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.talents.WarriorTalents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rotation {
    public static class AbilWrapper {
        public AbilWrapper(Ability abil) {
            ability = abil;
            this.cachedIsDamaging = ability.damageOverride() + ability.damageOnUseOverride() > 0f;
        }

        public Ability ability;
        public float numActivates;

        public float rage() {
            return ability.getRageUseOverDur(numActivates);
        }

        public float dps() {
            return ability.getDPS(numActivates);
        }

        public float hps() {
            return ability.getHPS(numActivates);
        }

        public float numActivatesU20;

        public float rageU20() {
            return ability.getRageUseOverDur(numActivatesU20);
        }

        public float dpsU20() {
            return ability.getDPS(numActivatesU20);
        }

        public float hpsU20() {
            return ability.getHPS(numActivatesU20);
        }

        public float allNumActivates() {
            return numActivates + numActivatesU20;
        }

        public float allRage() {
            return rage() + rageU20();
        }

        public float allDPS() {
            return dps() + dpsU20();
        }

        public float allHPS() {
            return hps() + hpsU20();
        }

        protected boolean cachedIsDamaging = false;

        public boolean isDamaging() {
            return this.cachedIsDamaging;
        }
    }

    public Rotation() {
        char_ = null;
        statS = null;
        talents = null;
        combatFactors = null;
        calcOpts = null;
        bossOpts = null;

        abilityList = new LinkedHashMap<Class<?>, AbilWrapper>();
        invalidateCache();
    }

    private LinkedHashMap<Class<?>, AbilWrapper> abilityList;

    public List<AbilWrapper> getDamagingAbilities() {
        List<AbilWrapper> list = new ArrayList<AbilWrapper>();
        for (AbilWrapper e : abilityList.values()) if (e.isDamaging()) list.add(e);
        return list;
    }

    public List<AbilWrapper> getMaintenanceAbilities() {
        List<AbilWrapper> list = new ArrayList<AbilWrapper>();
        for (AbilWrapper e : abilityList.values()) if (e.ability.isMaint) list.add(e);
        return list;
    }

    public List<AbilWrapper> getAbilityList() {
        return new ArrayList<AbilWrapper>(abilityList.values());
    }

    public List<AbilWrapper> getAbilityListThatGCDs() {
        List<AbilWrapper> list = new ArrayList<AbilWrapper>();
        for (AbilWrapper e : abilityList.values()) if (e.ability.usesGCD) list.add(e);
        return list;
    }

    public AbilWrapper getWrapper(Class<? extends Ability> type) {
        return abilityList.get(type);
    }

    public float hpsTtl;
    public float dpsTtl, dpsTtlU20;
    public String GCDUsage = "";
    protected CharacterCalculationsDPSWarr calcs = null;

    public boolean needDisplayCalcs = true;

    protected float TimeLostGCDs;
    protected float RageGainedWhileMoving;
    public float TimesStunned = 0f;
    public float TimesFeared = 0f;
    public float TimesRooted = 0f;
    public float TimesDisarmed = 0f;

    public DeepWounds DW;

    protected Character char_;
    protected WarriorTalents talents;
    protected Stats statS;
    protected CombatFactors combatFactors;
    public WhiteAttacks whiteAtks;
    protected CalculationOptionsDPSWarr calcOpts;
    protected BossOptions bossOpts;

    protected float cachedLatentGcd = 1.5f;

    protected float latentGCD() {
        return this.cachedLatentGcd;
    }

    protected float numGCDs() {
        return this.cachedNumGcds;
    }

    protected float cachedNumGcds = 0f;

    protected float numGCDsU20() {
        return this.cachedNumGcdsU20;
    }

    protected float cachedNumGcdsU20 = 0f;

    protected float gcdsUsed() {
        float gcds = 0f;
        for (AbilWrapper aw : getAbilityListThatGCDs()) {
            gcds += aw.numActivates * aw.ability.useTime() / latentGCD();
        }
        return gcds;
    }

    protected float gcdsUsedU20() {
        float gcds = 0f;
        for (AbilWrapper aw : getAbilityListThatGCDs()) {
            gcds += aw.numActivatesU20 * aw.ability.useTime() / latentGCD();
        }
        return gcds;
    }

    protected float gcdsAvailable() {
        return Math.max(0f, numGCDs() - gcdsUsed() - TimeLostGCDs);
    }

    protected float gcdsAvailableU20() {
        return Math.max(0f, numGCDsU20() - gcdsUsedU20() - TimeLostGCDs);
    }

    public void initialize(CharacterCalculationsDPSWarr calcs) {
        this.calcs = calcs;
        statS = calcs.averageStats;

        initAbilities();

        calcs.whites = whiteAtks;
    }

    public void initialize() {
        initAbilities();
    }

    public void makeRotationAndDoDps(boolean setCalcs, boolean needsDisplayCalculations) {
        this.needDisplayCalcs = needsDisplayCalculations;
    }

    protected void initAbilities() {
        abilityList.clear();
        whiteAtks.invalidateCache();
        addAbility(new AbilWrapper(new HeroicFury(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new EveryManForHimself(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Charge(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Intercept(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Intervene(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new SecondWind(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new BerserkerRage(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Bloodrage(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new BattleShout(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new CommandingShout(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new DemoralizingShout(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new SunderArmor(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new ThunderClap(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Hamstring(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new EnragedRegeneration(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new ShatteringThrow(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new SweepingStrikes(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new DeathWish(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Recklessness(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));

        Slam sl = new Slam(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
        addAbility(new AbilWrapper(sl));
        Ability ww = new WhirlWind(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
        addAbility(new AbilWrapper(ww));

        addAbility(new AbilWrapper(new Cleave(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new HeroicStrike(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        Ability ex = new Execute(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
        addAbility(new AbilWrapper(ex));

        addAbility(new AbilWrapper(new Bladestorm(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts, ww)));
        addAbility(new AbilWrapper(new MortalStrike(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Rend(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        Ability ss = new Swordspec(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
        addAbility(new AbilWrapper(ss));
        addAbility(new AbilWrapper(new OverPower(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts, ss)));
        addAbility(new AbilWrapper(new TasteForBlood(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts)));
        addAbility(new AbilWrapper(new Suddendeath(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts, ex)));

        Ability bt = new BloodThirst(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
        addAbility(new AbilWrapper(bt));
        addAbility(new AbilWrapper(new BloodSurge(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts, sl, ww, bt)));

        DW = new DeepWounds(char_, statS, combatFactors, whiteAtks, calcOpts, bossOpts);
    }

    private void addAbility(AbilWrapper abilWrapper) {
        if (!abilityList.containsKey(abilWrapper.ability.getClass())) {
            abilityList.put(abilWrapper.ability.getClass(), abilWrapper);
        }
    }

    public void doIterations() {
    }

    protected void calcDeepWounds() {
        // NOTE: the working-tree C# source uses CriticalYellowsOverDur (Both/Both) here, but the
        // prebuilt oracle DLL (bin/Release/Data/Rawr.DPSWarr.dll, verified with monodis) uses
        // CriticalYellowsOverDurMH/OH. The golden files came from the DLL, so the oracle's behavior
        // is ported (see docs/dev/DECISIONS.md).
        float mhActivates =
                criticalYellowsOverDurMH() +
                whiteAtks.mhActivates() * whiteAtks.mhAtkTable.crit;

        float ohActivates = (combatFactors.useOH ?
                criticalYellowsOverDurOH() +
                whiteAtks.ohActivates() * whiteAtks.ohAtkTable.crit
                : 0f);

        DW.setAllAbilityActivates(mhActivates, ohActivates);
    }

    public void invalidateCache() {
        for (int i = 0; i < 5; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++)
            this.attackOverDurations[i][j][k] = -1f;
    }

    public enum SwingResult {
        Attempt(0), Land(1), Crit(2), Parry(3), Dodge(4);

        public final int value;

        SwingResult(int value) {
            this.value = value;
        }
    }

    public enum Hand {
        MH(0), OH(1), Both(2);

        public final int value;

        Hand(int value) {
            this.value = value;
        }
    }

    public enum AttackType {
        Yellow(0), White(1), Both(2);

        public final int value;

        AttackType(int value) {
            this.value = value;
        }
    }

    private float[][][] attackOverDurations = new float[5][3][3];

    public float getAttackOverDuration(SwingResult swingResult, Hand hand, AttackType attackType) {
        if (this.attackOverDurations[swingResult.value][hand.value][attackType.value] == -1f) {
            setTable(swingResult, hand, attackType);
        }
        return this.attackOverDurations[swingResult.value][hand.value][attackType.value];
    }

    private void setTable(SwingResult sr, Hand h, AttackType at) {
        float count = 0f;
        float mod;
        CombatTable table;

        if (at != AttackType.White) {
            for (AbilWrapper abil : getDamagingAbilities()) {
                if (!abil.ability.validated()) {
                    continue;
                }
                if (h != Hand.OH) {
                    table = abil.ability.mhAtkTable();
                    mod = getTableFromSwingResult(sr, table);
                    count += abil.allNumActivates() * abil.ability.avgTargets() * abil.ability.swingsPerActivate * mod;
                }
                if (h != Hand.MH && combatFactors.useOH && abil.ability.swingsOffHand) {
                    table = abil.ability.ohAtkTable();
                    mod = getTableFromSwingResult(sr, table);
                    count += abil.allNumActivates() * abil.ability.avgTargets() * mod;
                }
            }
        }
        if (at != AttackType.Yellow) {
            if (h != Hand.OH) {
                table = whiteAtks.mhAtkTable;
                mod = getTableFromSwingResult(sr, table);
                count += whiteAtks.mhActivates() * mod;
            }
            if (h != Hand.MH && combatFactors.useOH) {
                table = whiteAtks.ohAtkTable;
                mod = getTableFromSwingResult(sr, table);
                count += whiteAtks.ohActivates() * mod;
            }
        }

        this.attackOverDurations[sr.value][h.value][at.value] = count;
    }

    private float getTableFromSwingResult(SwingResult sr, CombatTable table) {
        if (table == null) return 0f;
        switch (sr) {
            case Attempt: return 1f;
            case Crit: return table.crit;
            case Dodge: return table.dodge;
            case Land: return table.anyLand();
            case Parry: return table.parry;
            default: return 0f;
        }
    }

    public float attemptedAtksOverDur() {
        return getAttackOverDuration(SwingResult.Attempt, Hand.Both, AttackType.Both);
    }

    public float attemptedYellowsOverDur() {
        return getAttackOverDuration(SwingResult.Attempt, Hand.Both, AttackType.Yellow);
    }

    public float landedAtksOverDur() {
        return getAttackOverDuration(SwingResult.Land, Hand.Both, AttackType.Both);
    }

    public float criticalAtksOverDur() {
        return getAttackOverDuration(SwingResult.Crit, Hand.Both, AttackType.Both);
    }

    public float attemptedAtksOverDurMH() {
        return getAttackOverDuration(SwingResult.Attempt, Hand.MH, AttackType.Both);
    }

    public float landedAtksOverDurMH() {
        return getAttackOverDuration(SwingResult.Land, Hand.MH, AttackType.Both);
    }

    public float attemptedAtksOverDurOH() {
        return getAttackOverDuration(SwingResult.Attempt, Hand.OH, AttackType.Both);
    }

    public float landedAtksOverDurOH() {
        return getAttackOverDuration(SwingResult.Land, Hand.OH, AttackType.Both);
    }

    public float criticalYellowsOverDur() {
        return getAttackOverDuration(SwingResult.Crit, Hand.Both, AttackType.Both);
    }

    public float landedYellowsOverDur() {
        return getAttackOverDuration(SwingResult.Land, Hand.Both, AttackType.Both);
    }

    public float criticalYellowsOverDurMH() {
        return getAttackOverDuration(SwingResult.Crit, Hand.MH, AttackType.Yellow);
    }

    public float criticalYellowsOverDurOH() {
        return getAttackOverDuration(SwingResult.Crit, Hand.OH, AttackType.Yellow);
    }

    public float dodgedYellowsOverDur() {
        return getAttackOverDuration(SwingResult.Dodge, Hand.Both, AttackType.Yellow);
    }

    public float parriedYellowsOverDur() {
        return getAttackOverDuration(SwingResult.Parry, Hand.Both, AttackType.Yellow);
    }

    public float critHsSlamOverDur() {
        AbilWrapper hs = getWrapper(HeroicStrike.class);
        AbilWrapper sl = getWrapper(Slam.class);
        AbilWrapper bs = getWrapper(BloodSurge.class);
        return hs.allNumActivates() * hs.ability.mhAtkTable().crit * hs.ability.avgTargets() +
                sl.allNumActivates() * sl.ability.mhAtkTable().crit * sl.ability.avgTargets() +
                bs.allNumActivates() * bs.ability.mhAtkTable().crit * bs.ability.avgTargets();
    }

    public float maintainCDs() {
        float cds = 0f;
        for (AbilWrapper aw : getMaintenanceAbilities()) cds += aw.allNumActivates();
        return cds;
    }

    protected float rageGenOverDur_IncDmg() {
        if (!calcOpts.getAoETargets() || calcOpts.getAoETargetsFreq() < 1 || calcOpts.getAoETargetsDMG() < 1) {
            return 0f;
        }
        float RageMod = 2.5f / 453.3f;
        float damagePerSec = 0f;
        float freq = calcOpts.getAoETargetsFreq();
        float dmg = calcOpts.getAoETargetsDMG() * (1f + statS.getDamageTakenMultiplier()) + statS.getBossAttackPower() / 14f;
        float acts = fightDuration() / freq;
        float zerkerMOD = 1f;
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.BerserkerRage_.index]) {
            float upTime = seZERKERDUMMY.getAverageUptime(0, 1f, combatFactors.cMhItemSpeed, (calcOpts.getSE_UseDur() ? fightDuration() : 0f));
            zerkerMOD *= (1f + upTime);
        }
        float dmgCap = 100f / (RageMod * zerkerMOD);
        damagePerSec = (acts * Math.min(dmgCap, dmg)) / fightDuration();

        return (damagePerSec * RageMod * zerkerMOD) * fightDuration();
    }

    protected float rageGenOverDur_Anger() {
        return (talents.getAngerManagement() / 3.0f) * fightDuration();
    }

    protected float rageGenOverDur_Other() {
        float rage = rageGenOverDur_Anger()
                + rageGenOverDur_IncDmg()
                + (100f * statS.getManaorEquivRestore())
                + statS.getBonusRageGen();

        for (AbilWrapper aw : getAbilityList()) {
            if (aw.allRage() < 0)
                rage += (-1f) * aw.allRage();
        }

        if (statS.getBonusWarrior_T7_4P_RageProc() != 0f) {
            rage += (statS.getBonusWarrior_T7_4P_RageProc() * 0.1f) * (talents.getDeepWounds() > 0f ? 1f : 0f) * fightDuration();
            rage += (statS.getBonusWarrior_T7_4P_RageProc() * 0.1f) * (!combatFactors.furyStance() && calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Rend_.index] ? 1f / 3f : 0f) * fightDuration();
        }

        return rage;
    }

    public int fightDuration() {
        return (int) calcOpts.getDuration();
    }

    protected float rageNeededOverDur() {
        float rage = 0f;
        for (AbilWrapper aw : getAbilityList()) {
            if (aw.allRage() > 0f)
                rage += aw.allRage();
        }
        return rage;
    }

    protected float doMaintenanceActivates(float totalPercTimeLost) {
        float netRage = 0f;

        for (AbilWrapper aw : getMaintenanceAbilities()) {
            netRage += addMaintenanceAbility(totalPercTimeLost, aw);
        }
        if (netRage != 0f && this.needDisplayCalcs) GCDUsage += DPSWarrFormat.NL;
        return netRage;
    }

    private float addMaintenanceAbility(float totalPercTimeLost, AbilWrapper aw) {
        if (!aw.ability.validated()) return 0f;

        float Abil_GCDs = Math.min(gcdsAvailable(), aw.ability.activates() * (1f - totalPercTimeLost));
        Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(Abil_GCDs) : Abil_GCDs;
        aw.numActivates = Abil_GCDs;
        if (this.needDisplayCalcs && Abil_GCDs > 0)
            GCDUsage += DPSWarrFormat.zeroPadFixed(Abil_GCDs, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " : " + aw.ability.name + (aw.ability.usesGCD ? "\n" : "(Doesn't use GCDs)\n");

        this.hpsTtl += aw.allHPS();
        this.dpsTtl += aw.allDPS();
        return aw.ability.getRageUseOverDur(Abil_GCDs);
    }

    private float emActs;

    protected float calculateTimeLost(Ability MS) {
        this.emActs = 0f;
        TimeLostGCDs = 0;
        RageGainedWhileMoving = 0;

        float percTimeInMovement = calculateMovement(MS);
        float percTimeInFear = calculateFear();
        float percTimeInStun = calculateStun();
        float percTimeInRoot = calculateRoot();
        return Math.min(1f, percTimeInStun + percTimeInMovement + percTimeInFear + percTimeInRoot);
    }

    private float calculateRoot() {
        float percTimeInRoot = 0f;
        if (calcOpts.getRootingTargets() && calcOpts.getRoots().size() > 0) {
            float timelostwhilerooted = 0f;
            float BaseRootDur = 0f, rootActs = 0f, reducedDur = 0f,
                    MaxTimeRegain = 0f,
                    ChanceYouAreRooted = 1f;
            AbilWrapper HF = getWrapper(HeroicFury.class);
            float HFMaxActs = calcOpts.getAllowFlooring() ? (float) Math.floor(HF.ability.activates()) : HF.ability.activates();
            float HFActualActs = 0f;
            AbilWrapper EM = getWrapper(EveryManForHimself.class);
            float EMMaxActs = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
            float EMOldActs = EM.allNumActivates();
            TimesRooted = 0f;
            for (Impedance r : calcOpts.getRoots()) {
                ChanceYouAreRooted = r.chance;
                BaseRootDur = Math.max(0f, (r.duration / 1000f * (1f - statS.getSnareRootDurReduc())));
                rootActs = calcOpts.getAllowFlooring() ? (float) Math.ceil(fightDuration() / r.frequency) : fightDuration() / r.frequency;
                if (rootActs > 0f) {
                    TimesRooted += rootActs;
                    if (HFMaxActs - HFActualActs > 0f) {
                        MaxTimeRegain = Math.max(0f, (BaseRootDur - latentGCD() - calcOpts.getReact() / 1000f));
                        float BZNewActs = Math.min(HFMaxActs - HFActualActs, rootActs);
                        HFActualActs += BZNewActs;
                        reducedDur = Math.max(0f, BaseRootDur - MaxTimeRegain);
                        float percBZdVsUnBZd = BZNewActs / rootActs;
                        timelostwhilerooted += (reducedDur * rootActs * percBZdVsUnBZd * ChanceYouAreRooted)
                                + (BaseRootDur * rootActs * (1f - percBZdVsUnBZd) * ChanceYouAreRooted);
                    } else if (char_.getRace() == CharacterRace.Human && EMMaxActs - EM.allNumActivates() > 0f) {
                        MaxTimeRegain = Math.max(0f, (BaseRootDur - latentGCD() - calcOpts.getReact() / 1000f));

                        float availEMacts = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
                        float EMNewActs = Math.min(rootActs, availEMacts);
                        EM.numActivates += EMNewActs;
                        this.emActs = EM.allNumActivates();

                        reducedDur = Math.max(0f, BaseRootDur - MaxTimeRegain);
                        float percEMdVsUnEMd = EMNewActs / rootActs;
                        timelostwhilerooted += (reducedDur * rootActs * percEMdVsUnEMd * ChanceYouAreRooted)
                                + (BaseRootDur * rootActs * (1f - percEMdVsUnEMd) * ChanceYouAreRooted);
                    } else {
                        timelostwhilerooted += BaseRootDur * rootActs * ChanceYouAreRooted;
                    }
                }
            }
            HF.numActivates = HFActualActs;
            if (this.needDisplayCalcs) {
                GCDUsage += (TimesFeared > 0 ? DPSWarrFormat.zeroPadFixed(TimesFeared, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseRootDur, 2) + "secs : Lost to Fears\n" : "");
                GCDUsage += (HF.allNumActivates() > 0 ? DPSWarrFormat.zeroPadFixed(HF.allNumActivates(), 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseRootDur - reducedDur, 2) + "secs : - " + HF.ability.name + "\n" : "");
                GCDUsage += (EM.allNumActivates() - EMOldActs > 0 ? DPSWarrFormat.zeroPadFixed(EM.allNumActivates() - EMOldActs, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseRootDur - reducedDur, 2) + "secs : - " + EM.ability.name + "\n" : "");
            }
            TimeLostGCDs += Math.min(numGCDs(), (BaseRootDur * TimesFeared) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * HF.allNumActivates()) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * this.emActs) / latentGCD());

            timelostwhilerooted = calcOpts.getAllowFlooring() ? (float) Math.ceil(timelostwhilerooted) : timelostwhilerooted;
            percTimeInRoot = timelostwhilerooted / fightDuration();
        }
        SecondWind SndW = (SecondWind) getWrapper(SecondWind.class).ability;
        SndW.setNumStunsOverDur(SndW.getNumStunsOverDur() + TimesRooted);
        return percTimeInRoot;
    }

    private float calculateStun() {
        float percTimeInStun = 0f;
        if (calcOpts.getStunningTargets() && calcOpts.getStuns().size() > 0) {
            float timelostwhilestunned = 0f;
            float BaseStunDur = 0f, stunActs = 0f, reducedDur = 0f,
                    MaxTimeRegain = 0f,
                    ChanceYouAreStunned = 1f;
            AbilWrapper EM = getWrapper(EveryManForHimself.class);
            float EMMaxActs = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
            float EMOldActs = EM.allNumActivates();
            TimesFeared = 0f;
            for (Impedance s : calcOpts.getStuns()) {
                ChanceYouAreStunned = s.chance;
                BaseStunDur = Math.max(0f, (s.duration / 1000f * (1f - statS.getStunDurReduc())));
                stunActs = calcOpts.getAllowFlooring() ? (float) Math.ceil(fightDuration() / s.frequency) : fightDuration() / s.frequency;
                if (stunActs > 0f) {
                    TimesFeared += stunActs;
                    if (char_.getRace() == CharacterRace.Human && EMMaxActs - EM.allNumActivates() > 0f) {
                        MaxTimeRegain = Math.max(0f, (BaseStunDur - latentGCD() - calcOpts.getReact() / 1000f));

                        float availEMacts = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
                        float EMNewActs = Math.min(stunActs, availEMacts);
                        EM.numActivates += EMNewActs;
                        this.emActs = EM.allNumActivates();

                        reducedDur = Math.max(0f, BaseStunDur - MaxTimeRegain);
                        float percEMdVsUnEMd = EMNewActs / stunActs;
                        timelostwhilestunned += (reducedDur * stunActs * percEMdVsUnEMd * ChanceYouAreStunned)
                                + (BaseStunDur * stunActs * (1f - percEMdVsUnEMd) * ChanceYouAreStunned);
                    } else {
                        timelostwhilestunned += BaseStunDur * stunActs * ChanceYouAreStunned;
                    }
                }
            }
            if (this.needDisplayCalcs) {
                GCDUsage += (TimesFeared > 0 ? DPSWarrFormat.zeroPadFixed(TimesFeared, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseStunDur, 2) + "secs : Lost to Fears\n" : "");
                GCDUsage += (EM.allNumActivates() - EMOldActs > 0 ? DPSWarrFormat.zeroPadFixed(EM.allNumActivates() - EMOldActs, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseStunDur - reducedDur, 2) + "secs : - " + EM.ability.name + "\n" : "");
            }
            TimeLostGCDs += Math.min(numGCDs(), (BaseStunDur * TimesFeared) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * this.emActs) / latentGCD());

            timelostwhilestunned = calcOpts.getAllowFlooring() ? (float) Math.ceil(timelostwhilestunned) : timelostwhilestunned;
            percTimeInStun = timelostwhilestunned / fightDuration();

            SecondWind SndW = (SecondWind) getWrapper(SecondWind.class).ability;
            SndW.setNumStunsOverDur(SndW.getNumStunsOverDur() + stunActs);
        }

        return percTimeInStun;
    }

    private float calculateFear() {
        float percTimeInFear = 0f;
        if (calcOpts.getFearingTargets() && calcOpts.getFears().size() > 0) {
            float timelostwhilefeared = 0f;
            float BaseFearDur = 0f, fearActs = 0f, reducedDur = 0f,
                    MaxTimeRegain = 0f,
                    ChanceYouAreFeared = 1f;
            AbilWrapper BZ = getWrapper(BerserkerRage.class);
            float BZMaxActs = calcOpts.getAllowFlooring() ? (float) Math.floor(BZ.ability.activates()) : BZ.ability.activates();
            float BZActualActs = 0f;
            AbilWrapper EM = getWrapper(EveryManForHimself.class);
            float EMMaxActs = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
            float EMOldActs = EM.allNumActivates();
            TimesFeared = 0f;
            for (Impedance f : calcOpts.getFears()) {
                ChanceYouAreFeared = f.chance;
                BaseFearDur = Math.max(0f, (f.duration / 1000f * (1f - statS.getFearDurReduc())));
                fearActs = calcOpts.getAllowFlooring() ? (float) Math.ceil(fightDuration() / f.frequency) : fightDuration() / f.frequency;
                if (fearActs > 0f) {
                    TimesFeared += fearActs;
                    if (BZMaxActs - BZActualActs > 0f) {
                        MaxTimeRegain = Math.max(0f, (BaseFearDur - latentGCD() - calcOpts.getReact() / 1000f));
                        float BZNewActs = Math.min(BZMaxActs - BZActualActs, fearActs);
                        BZActualActs += BZNewActs;
                        reducedDur = Math.max(0f, BaseFearDur - MaxTimeRegain);
                        float percBZdVsUnBZd = BZNewActs / fearActs;
                        timelostwhilefeared += (reducedDur * fearActs * percBZdVsUnBZd * ChanceYouAreFeared)
                                + (BaseFearDur * fearActs * (1f - percBZdVsUnBZd) * ChanceYouAreFeared);
                    } else if (char_.getRace() == CharacterRace.Human && EMMaxActs - EM.allNumActivates() > 0f) {
                        MaxTimeRegain = Math.max(0f, (BaseFearDur - latentGCD() - calcOpts.getReact() / 1000f));

                        float availEMacts = (calcOpts.getAllowFlooring() ? (float) Math.floor(EM.ability.activates()) : EM.ability.activates()) - EM.allNumActivates();
                        float EMNewActs = Math.min(fearActs, availEMacts);
                        EM.numActivates += EMNewActs;
                        this.emActs = EM.allNumActivates();

                        reducedDur = Math.max(0f, BaseFearDur - MaxTimeRegain);
                        float percEMdVsUnEMd = EMNewActs / fearActs;
                        timelostwhilefeared += (reducedDur * fearActs * percEMdVsUnEMd * ChanceYouAreFeared)
                                + (BaseFearDur * fearActs * (1f - percEMdVsUnEMd) * ChanceYouAreFeared);
                    } else {
                        timelostwhilefeared += BaseFearDur * fearActs * ChanceYouAreFeared;
                    }
                }
            }
            BZ.numActivates = BZActualActs;
            if (this.needDisplayCalcs) {
                GCDUsage += (TimesFeared > 0 ? DPSWarrFormat.zeroPadFixed(TimesFeared, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseFearDur, 2) + "secs : Lost to Fears\n" : "");
                GCDUsage += (BZ.allNumActivates() > 0 ? DPSWarrFormat.zeroPadFixed(BZ.allNumActivates(), 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseFearDur - reducedDur, 2) + "secs : - " + BZ.ability.name + "\n" : "");
                GCDUsage += (EM.allNumActivates() - EMOldActs > 0 ? DPSWarrFormat.zeroPadFixed(EM.allNumActivates() - EMOldActs, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseFearDur - reducedDur, 2) + "secs : - " + EM.ability.name + "\n" : "");
            }
            TimeLostGCDs += Math.min(numGCDs(), (BaseFearDur * TimesFeared) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * BZ.allNumActivates()) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * this.emActs) / latentGCD());

            timelostwhilefeared = calcOpts.getAllowFlooring() ? (float) Math.ceil(timelostwhilefeared) : timelostwhilefeared;
            percTimeInFear = timelostwhilefeared / fightDuration();
        }

        return percTimeInFear;
    }

    private float calculateMovement(Ability MS) {
        float percTimeInMovement = 0f;
        if (calcOpts.getMovingTargets() && calcOpts.getMoves().size() > 0) {
            AbilWrapper HF = getWrapper(HeroicFury.class);
            AbilWrapper CH;
            if (combatFactors.furyStance()) CH = getWrapper(Intercept.class);
            else CH = getWrapper(Charge.class);

            float MovementSpeed = 7f * (1f + statS.getMovementSpeed());
            float BaseMoveDur = 0f, movedActs = 0f, reducedDur = 0f,
                    MinMovementTimeRegain = 0f, MaxMovementTimeRegain = 0f,
                    ChanceYouHaveToMove = 1f;
            float ChargeMaxActs = calcOpts.getAllowFlooring() ? (float) Math.floor(CH.ability.activates()) : CH.ability.activates();
            if (combatFactors.furyStance() && HF.ability.validated()) {
                ChargeMaxActs += HF.ability.activates() - HF.allNumActivates();
            }
            float ChargeActualActs = 0f;
            float timelostwhilemoving = 0f;
            float moveGCDs = 0f;
            for (Impedance m : calcOpts.getMoves()) {
                ChanceYouHaveToMove = m.chance;
                BaseMoveDur = (m.duration / 1000f * (1f - statS.getMovementSpeed()));
                movedActs = calcOpts.getAllowFlooring() ? (float) Math.ceil(fightDuration() / m.frequency) : fightDuration() / m.frequency;
                moveGCDs += movedActs;

                if ((ChargeMaxActs - ChargeActualActs > 0f) && (movedActs > 0f)) {
                    MaxMovementTimeRegain = Math.max(0f,
                            Math.min((BaseMoveDur - calcOpts.getReact() / 1000f),
                                    (CH.ability.maxRange / MovementSpeed - calcOpts.getReact() / 1000f)));
                    MinMovementTimeRegain = Math.max(0f,
                            Math.min((BaseMoveDur - calcOpts.getReact() / 1000f),
                                    (CH.ability.minRange / MovementSpeed - calcOpts.getReact() / 1000f)));
                    if (BaseMoveDur >= MinMovementTimeRegain) {
                        float ChargeNewActs = Math.min(ChargeMaxActs - ChargeActualActs, movedActs);
                        ChargeActualActs += ChargeNewActs;
                        reducedDur = Math.max(0f, BaseMoveDur - MaxMovementTimeRegain);
                        float percChargedVsUncharged = ChargeNewActs / movedActs;
                        timelostwhilemoving += (reducedDur * movedActs * percChargedVsUncharged * ChanceYouHaveToMove)
                                + (BaseMoveDur * movedActs * (1f - percChargedVsUncharged) * ChanceYouHaveToMove);
                    }
                } else if (movedActs > 0f) {
                    timelostwhilemoving += BaseMoveDur * movedActs * ChanceYouHaveToMove;
                }
            }
            float actsCharge = ChargeActualActs;

            TimeLostGCDs += Math.min(numGCDs(), (BaseMoveDur * moveGCDs) / latentGCD());
            TimeLostGCDs -= Math.min(TimeLostGCDs, (reducedDur * actsCharge) / latentGCD());
            CH.numActivates = actsCharge;
            float actsHF = 0f;
            if (CH.allNumActivates() > CH.ability.activates()) actsHF += (CH.allNumActivates() - CH.ability.activates());
            HF.numActivates += actsHF;
            if (this.needDisplayCalcs) {
                GCDUsage += (moveGCDs > 0 ? DPSWarrFormat.zeroPadFixed(moveGCDs, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseMoveDur, 2) + "secs : Lost to Movement\n" : "");
                GCDUsage += (actsCharge > 0 ? DPSWarrFormat.zeroPadFixed(actsCharge, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " x " + DPSWarrFormat.fixed(BaseMoveDur - reducedDur, 2) + "secs : - " + CH.ability.name + "\n" : "");
                GCDUsage += (actsHF > 0 ? DPSWarrFormat.zeroPadFixed(actsHF, 3, calcOpts.getAllowFlooring() ? 0 : 2) + " activates of " + HF.ability.name + " to refresh " + CH.ability.name : "");
            }
            if (talents.getJuggernaut() > 0 && MS != null && CH.ability instanceof Charge) {
                Stats stats = new Stats();
                stats.setBonusWarrior_T8_4P_MSBTCritIncrease(0.25f *
                        (seChargeUse[talents.getJuggernaut()][talents.isGlyphOfRapidCharge() ? 1 : 0]).getAverageUptime(fightDuration() / actsCharge, 1f, combatFactors.cMhItemSpeed, fightDuration()));
                stats.accumulate(statS);
                MS.bonusCritChance = stats.getBonusWarrior_T8_4P_MSBTCritIncrease();
            }
            timelostwhilemoving = (calcOpts.getAllowFlooring() ? (float) Math.ceil(timelostwhilemoving) : timelostwhilemoving);
            percTimeInMovement = timelostwhilemoving / fightDuration();
        }
        return percTimeInMovement;
    }

    private static final SpecialEffect seZERKERDUMMY;

    static {
        seZERKERDUMMY = new SpecialEffect(Trigger.Use, statsWithAgilityMultiplier(1f), 10f, 30f);
    }

    private static Stats statsWithAgilityMultiplier(float value) {
        Stats s = new Stats();
        s.setBonusAgilityMultiplier(value);
        return s;
    }

    private static final SpecialEffect[][] seChargeUse = {
            new SpecialEffect[] {
                    new SpecialEffect(Trigger.Use, null, 10, ((15f + 0 * 5f) * (1f - 0f))),
                    new SpecialEffect(Trigger.Use, null, 10, ((15f + 0 * 5f) * (1f - 0.07f)))},
            new SpecialEffect[] {
                    new SpecialEffect(Trigger.Use, null, 10, ((15f + 1 * 5f) * (1f - 0f))),
                    new SpecialEffect(Trigger.Use, null, 10, ((15f + 1 * 5f) * (1f - 0.07f)))},
    };

    private static final SpecialEffect[][][] seBattleShout;

    private static final SpecialEffect[][][] seCommandingShout;

    static {
        seBattleShout = new SpecialEffect[6][2][3];
        seCommandingShout = new SpecialEffect[6][2][3];
        for (int cp = 0; cp <= 5; cp++) {
            for (int g = 0; g <= 1; g++) {
                for (int bv = 0; bv <= 2; bv++) {
                    float ap = (548f * (1f + cp * 0.05f));
                    float dur = ((2f + (g == 1 ? 2f : 0f)) * 60f * (1f + bv * 0.25f));
                    Stats bs = new Stats();
                    bs.setAttackPower(ap);
                    seBattleShout[cp][g][bv] = new SpecialEffect(Trigger.Use, bs, dur, dur);
                    float health = (2255f * (1f + cp * 0.05f));
                    Stats cs = new Stats();
                    cs.setHealth(health);
                    seCommandingShout[cp][g][bv] = new SpecialEffect(Trigger.Use, cs, dur, dur);
                }
            }
        }
    }

    private static final SpecialEffect[][] seDemoralizingShout;

    static {
        seDemoralizingShout = new SpecialEffect[6][3];
        for (int i = 0; i <= 5; i++) {
            for (int bv = 0; bv <= 2; bv++) {
                int idx = bv < 2 ? i : 0;
                Stats s = new Stats();
                s.setBossAttackPower(410f * (1f + idx * 0.08f) * -1f);
                seDemoralizingShout[i][bv] = new SpecialEffect(Trigger.Use, s, 30f * (1f + 0.25f * bv), 30f * (1f + 0.25f * bv));
            }
        }
    }

    private static LinkedHashMap<Float, SpecialEffect> seRecklessness = new LinkedHashMap<Float, SpecialEffect>();
    private static LinkedHashMap<Float, SpecialEffect> seShatteringThrow = new LinkedHashMap<Float, SpecialEffect>();
    private static LinkedHashMap<Float, SpecialEffect[]> seThunderClap = new LinkedHashMap<Float, SpecialEffect[]>();
    private static LinkedHashMap<Float, SpecialEffect> seSunderArmor = new LinkedHashMap<Float, SpecialEffect>();
    private static LinkedHashMap<Float, SpecialEffect> seSweepingStrikes = new LinkedHashMap<Float, SpecialEffect>();

    public void addValidatedSpecialEffects(Stats statsTotal, WarriorTalents talents) {
        Ability ST = getWrapper(ShatteringThrow.class).ability,
                BTS = getWrapper(BattleShout.class).ability,
                CS = getWrapper(CommandingShout.class).ability,
                DS = getWrapper(DemoralizingShout.class).ability,
                TH = getWrapper(ThunderClap.class).ability,
                SN = getWrapper(SunderArmor.class).ability,
                SW = getWrapper(SweepingStrikes.class).ability,
                RK = getWrapper(Recklessness.class).ability;
        if (BTS.validated()) {
            statsTotal.addSpecialEffect(seBattleShout[talents.getCommandingPresence()][talents.isGlyphOfBattle() ? 0 : 1][talents.getBoomingVoice()]);
        }
        if (CS.validated()) {
            statsTotal.addSpecialEffect(seCommandingShout[talents.getCommandingPresence()][talents.isGlyphOfCommand() ? 0 : 1][talents.getBoomingVoice()]);
        }
        if (DS.validated()) {
            statsTotal.addSpecialEffect(seDemoralizingShout[talents.getImprovedDemoralizingShout()][talents.getBoomingVoice()]);
        }
        if (ST.validated()) {
            float value = roundHalfEven(ST.mhAtkTable().anyLand(), 3);
            if (!seShatteringThrow.containsKey(value)) {
                Stats s = new Stats();
                s.setArmorPenetration(0.20f);
                seShatteringThrow.put(value, new SpecialEffect(Trigger.Use, s, ST.duration, ST.cd, ST.mhAtkTable().anyLand()));
            }
            statsTotal.addSpecialEffect(seShatteringThrow.get(value));
        }
        if (TH.validated()) {
            float value = roundHalfEven(TH.mhAtkTable().anyLand(), 3);
            if (!seThunderClap.containsKey(value)) {
                seThunderClap.put(value, new SpecialEffect[] {
                        thunderClapEffect(0, TH),
                        thunderClapEffect(1, TH),
                        thunderClapEffect(2, TH),
                        thunderClapEffect(3, TH),
                });
            }
            statsTotal.addSpecialEffect(seThunderClap.get(value)[talents.getImprovedThunderClap()]);
        }
        if (SN.validated()) {
            float value = roundHalfEven(SN.mhAtkTable().anyLand(), 3);
            if (!seSunderArmor.containsKey(value)) {
                Stats s = new Stats();
                s.setArmorPenetration(0.04f);
                seSunderArmor.put(value, new SpecialEffect(Trigger.Use, s, SN.duration, SN.cd, SN.mhAtkTable().anyLand(), 5));
            }
            statsTotal.addSpecialEffect(seSunderArmor.get(value));
        }
        float landedAtksInterval = landedAtksOverDur() / fightDuration();
        float critRate = criticalAtksOverDur() / attemptedAtksOverDur();
        if (SW.validated()) {
            float interval = roundHalfEven(landedAtksInterval * 5f, 3);
            if (!seSweepingStrikes.containsKey(interval)) {
                Stats s = new Stats();
                s.setBonusTargets(1f);
                seSweepingStrikes.put(interval, new SpecialEffect(Trigger.Use, s, landedAtksInterval * 5f, SW.cd));
            }
            statsTotal.addSpecialEffect(seSweepingStrikes.get(interval));
        }
        if (RK.validated() && combatFactors.furyStance()) {
            float interval = roundHalfEven(landedAtksInterval * 3f, 3);
            if (!seRecklessness.containsKey(interval)) {
                Stats s = new Stats();
                s.setPhysicalCrit(1f);
                seRecklessness.put(interval, new SpecialEffect(Trigger.Use, s, landedAtksInterval * 3f, RK.cd));
            }
            statsTotal.addSpecialEffect(seRecklessness.get(interval));
        }
    }

    private static SpecialEffect thunderClapEffect(int improvedThunderClap, Ability th) {
        Stats s = new Stats();
        s.setBossAttackSpeedMultiplier(-0.10f * (1f + improvedThunderClap / 30f));
        return new SpecialEffect(Trigger.Use, s, th.duration, th.cd, th.mhAtkTable().anyLand());
    }

    static float roundHalfEven(float value, int digits) {
        if (!Float.isFinite(value)) return value;
        return new BigDecimal((double) value).setScale(digits, RoundingMode.HALF_EVEN).floatValue();
    }

    void resetHitTables() {
        for (AbilWrapper aw : getAbilityList()) {
            if (aw.ability.canCrit) {
                aw.ability.mhAtkTable().reset();
                aw.ability.ohAtkTable().reset();
            }
        }
    }

    static void clearStaticEffectCaches() {
        seZERKERDUMMY.clearInterpolatorCache();
        for (SpecialEffect[] row : seChargeUse) for (SpecialEffect e : row) e.clearInterpolatorCache();
        for (SpecialEffect[][] a : seBattleShout) for (SpecialEffect[] row : a) for (SpecialEffect e : row) e.clearInterpolatorCache();
        for (SpecialEffect[][] a : seCommandingShout) for (SpecialEffect[] row : a) for (SpecialEffect e : row) e.clearInterpolatorCache();
        for (SpecialEffect[] row : seDemoralizingShout) for (SpecialEffect e : row) e.clearInterpolatorCache();
        for (SpecialEffect e : seRecklessness.values()) e.clearInterpolatorCache();
        for (SpecialEffect e : seShatteringThrow.values()) e.clearInterpolatorCache();
        for (SpecialEffect[] a : seThunderClap.values()) for (SpecialEffect e : a) e.clearInterpolatorCache();
        for (SpecialEffect e : seSunderArmor.values()) e.clearInterpolatorCache();
        for (SpecialEffect e : seSweepingStrikes.values()) e.clearInterpolatorCache();
        Suddendeath.clearStaticEffectCaches();
        Swordspec.clearStaticEffectCaches();
        // The value-keyed maps cache effects created with a fixture's exact (unrounded) chance/
        // duration. The oracle starts every fixture in a fresh process, so drop the entries for a
        // user-initiated top-level calculation.
        seRecklessness.clear();
        seShatteringThrow.clear();
        seThunderClap.clear();
        seSunderArmor.clear();
        seSweepingStrikes.clear();
    }
}
