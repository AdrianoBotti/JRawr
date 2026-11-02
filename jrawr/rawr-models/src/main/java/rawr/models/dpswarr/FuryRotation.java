// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Rotation.Fury.cs @ 46f190bee32954cc8df5fe853d95b8a343f5aa23
// Non-Silverlight branches only. The protected-but-reachable no-arg Initialize() override is ported;
// the compiled-but-never-called `new_doIterations()` and the unused ROTATION_LENGTH/FREE_GCDS
// constants are omitted (they have no effect on any calculation).
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;

public class FuryRotation extends Rotation {
    public FuryRotation(Character character, Stats stats, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = character;
        statS = stats;
        talents = char_ == null || char_.getWarriorTalents() == null ? new WarriorTalents() : char_.getWarriorTalents();
        combatFactors = cf;
        calcOpts = (co == null ? new CalculationOptionsDPSWarr() : co);
        bossOpts = (bo == null ? new BossOptions() : bo);
        whiteAtks = wa;

        cachedLatentGcd = 1.5f + calcOpts.getLatency() + calcOpts.getAllowedReact();
        cachedNumGcds = calcOpts.getAllowFlooring() ? (float) Math.floor(fightDuration() / latentGCD()) : fightDuration() / latentGCD();
    }

    private float percHS, percCL;
    private float timeLostPerc = 0f;

    @Override
    public void initialize() {
        initAbilities();
        boolean hsok = calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.HeroicStrike_.index];
        boolean clok = calcOpts.getMultipleTargets()
                && calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Cleave_.index];

        percHS = (hsok ? 1f : 0f);
        if (clok) {
            percHS -= calcOpts.getMultipleTargetsPerc() / 100f;
        }
        percCL = (clok ? 1f - percHS : 0f);
    }

    @Override
    public void initialize(CharacterCalculationsDPSWarr calcs) {
        super.initialize(calcs);

        boolean hsok = calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.HeroicStrike_.index];
        boolean clok = calcOpts.getMultipleTargets()
                && calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Cleave_.index];
        BloodSurge _BS = (BloodSurge) getWrapper(BloodSurge.class).ability;
        percHS = (hsok ? 1f : 0f);
        if (clok) {
            percHS -= (float) calcOpts.getMultipleTargetsPerc() / 100f;
        }
        percCL = (clok ? 1f - percHS : 0f);
        if (_BS != null) _BS.maintainActs = maintainCDs();
    }

    @Override
    protected void calcDeepWounds() {
        // Main Hand
        float mhActivates =
                criticalYellowsOverDurMH() +
                whiteAtks.mhActivates() * (1f - timeLostPerc) * whiteAtks.mhAtkTable.crit;

        // Off Hand
        float ohActivates = (combatFactors.useOH ?
                criticalYellowsOverDurOH() +
                whiteAtks.ohActivates() * (1f - timeLostPerc) * whiteAtks.ohAtkTable.crit
                : 0f);

        DW.setAllAbilityActivates(mhActivates, ohActivates);
    }

    @Override
    public void doIterations() {
        super.doIterations();
        BloodSurge BS = (BloodSurge) getWrapper(BloodSurge.class).ability;
        AbilWrapper HS = getWrapper(HeroicStrike.class);
        AbilWrapper CL = getWrapper(Cleave.class);
        OnAttack _HS = (OnAttack) HS.ability;
        OnAttack _CL = (OnAttack) CL.ability;

        float ovdRage, hsRageUsed, clRageUsed;
        float oldBS, oldHS, oldCL;
        do {
            oldBS = BS.activates();
            oldHS = HS.allNumActivates();
            oldCL = CL.allNumActivates();

            ovdRage = freeRageOverDur();
            hsRageUsed = ovdRage * percHS;
            clRageUsed = ovdRage * percCL;
            HS.numActivates = Math.min(hsRageUsed / _HS.fullRageCost(), whiteAtks.mhActivatesNoHS());
            whiteAtks.hsOverridesOverDur = HS.numActivates;
            CL.numActivates = Math.min(clRageUsed / _CL.fullRageCost(), whiteAtks.mhActivatesNoHS() - whiteAtks.hsOverridesOverDur);
            whiteAtks.clOverridesOverDur = CL.numActivates;
            BS.hsActivates = HS.allNumActivates();
        } while (Math.abs(1f - (BS.activates() != 0 ? oldBS / BS.activates() : 1f)) > 0.005f ||
                Math.abs(1f - (HS.allNumActivates() <= 0 ? oldHS / HS.allNumActivates() : 1f)) > 0.005f ||
                Math.abs(1f - (CL.allNumActivates() <= 0 ? oldCL / CL.allNumActivates() : 1f)) > 0.005f);
    }

    @Override
    public void makeRotationAndDoDps(boolean setCalcs, boolean needsDisplayCalcs) {
        super.makeRotationAndDoDps(setCalcs, needsDisplayCalcs);
        // Starting Numbers
        float DPS_TTL = 0f, HPS_TTL = 0f;
        float LatentGCD = 1.5f + calcOpts.getLatency();
        if (needDisplayCalcs) GCDUsage += "NumGCDs: " + rawr.models.mage.NetFormat.netFloatToString(numGCDs()) + "\n\n";
        float GCDsused = 0f;
        float availGCDs = Math.max(0f, numGCDs() - GCDsused);
        float availRage = 0f;
        float rageadd = 0f;

        if (char_.getMainHand() == null) {
            return;
        }

        // ==== Rage Generation Priorities ========
        availRage += rageGenOverDur_Other();

        float otherTimeLost = calculateTimeLost(null);
        doMaintenanceActivates(otherTimeLost);

        float timeLostPerc = otherTimeLost;

        AbilWrapper WW = getWrapper(WhirlWind.class);
        AbilWrapper BT = getWrapper(BloodThirst.class);
        AbilWrapper BS = getWrapper(BloodSurge.class);
        // Priority 1 : Whirlwind on every CD
        float WW_GCDs = Math.min(availGCDs, WW.ability.activates()) * (1f - timeLostPerc);
        WW.numActivates = WW_GCDs;
        GCDsused += Math.min(numGCDs(), WW_GCDs);
        if (needDisplayCalcs) GCDUsage += WW.ability.name + ": " + rawr.models.mage.NetFormat.netFloatToString(WW_GCDs) + "\n";
        availGCDs = Math.max(0f, numGCDs() - GCDsused);
        DPS_TTL += WW.dps();
        HPS_TTL += WW.hps();
        rageadd = WW.allRage();
        availRage -= rageadd;

        // Priority 2 : Bloodthirst on every CD
        float BT_GCDs = Math.min(availGCDs, BT.ability.activates()) * (1f - timeLostPerc);
        BT.numActivates = BT_GCDs;
        GCDsused += Math.min(numGCDs(), BT_GCDs);
        if (needDisplayCalcs) GCDUsage += BT.ability.name + ": " + rawr.models.mage.NetFormat.netFloatToString(BT_GCDs) + "\n";
        availGCDs = Math.max(0f, numGCDs() - GCDsused);
        DPS_TTL += BT.dps();
        HPS_TTL += BT.hps();
        rageadd = BT.allRage();
        availRage -= rageadd;

        doIterations();
        // Priority 3 : Bloodsurge Blood Proc (Do an Instant Slam) if available
        float BS_GCDs = Math.min(availGCDs, BS.ability.activates()) * (1f - timeLostPerc);
        BS.numActivates = BS_GCDs;
        GCDsused += Math.min(numGCDs(), BS_GCDs);
        if (needDisplayCalcs) GCDUsage += BS.ability.name + ": " + rawr.models.mage.NetFormat.netFloatToString(BS_GCDs) + "\n";
        availGCDs = Math.max(0f, numGCDs() - GCDsused);
        DPS_TTL += BS.dps();
        HPS_TTL += BS.hps();
        rageadd = BS.allRage();
        availRage -= rageadd;

        invalidateCache();

        // Priority 4 : Heroic Strike, when there is rage to do so, handled by the Heroic Strike class
        availRage += whiteAtks.mhRageGenOverDur() + whiteAtks.ohRageGenOverDur();

        whiteAtks.slamActsOverDur = 0f;

        DPS_TTL += whiteAtks.mhDPS() * (1f - timeLostPerc) + whiteAtks.ohDPS() * (1f - timeLostPerc);
        DPS_TTL += getWrapper(HeroicStrike.class).dps();
        DPS_TTL += getWrapper(Cleave.class).dps();

        calcDeepWounds();
        DPS_TTL += DW.dps();
        if (needDisplayCalcs) GCDUsage += "\nAvail: " + rawr.models.mage.NetFormat.netFloatToString(availGCDs);

        // Return result
        hpsTtl = HPS_TTL;

        if (setCalcs) {
            this.calcs.setTotalDPS(DPS_TTL);
            this.calcs.whiteDPS = (whiteAtks.mhDPS() + whiteAtks.ohDPS()) * (1f - timeLostPerc);

            this.calcs.whiteRage = whiteAtks.mhRageGenOverDur() + whiteAtks.ohRageGenOverDur();
            this.calcs.otherRage = rageGenOverDur_Other();
            this.calcs.needyRage = rageNeededOverDur();
            this.calcs.freeRage = calcs.whiteRage + calcs.otherRage - calcs.needyRage;
            this.calcs.whiteDPSMH = this.whiteAtks.mhDPS();
            this.calcs.whiteDPSOH = this.whiteAtks.ohDPS();
            this.calcs.whiteDmg = this.whiteAtks.mhDamageOnUse() + this.whiteAtks.ohDamageOnUse();
        }
    }

    protected float freeRageOverDur() {
        return whiteAtks.whiteRageGenOverDurNoHS() * (1f - timeLostPerc) +
                rageGenOverDur_Other() -
                rageNeededOverDur();
    }
}
