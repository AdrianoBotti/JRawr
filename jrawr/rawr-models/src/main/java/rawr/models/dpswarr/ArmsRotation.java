// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Rotation.Arms.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// `out float PercFailRage` becomes a one-element float[] out parameter. Non-Silverlight branches only.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;

public class ArmsRotation extends Rotation {
    public ArmsRotation(Character character, Stats stats, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        char_ = character;
        statS = stats;
        talents = char_ == null || char_.getWarriorTalents() == null ? new WarriorTalents() : char_.getWarriorTalents();
        combatFactors = cf;
        calcOpts = (co == null ? new CalculationOptionsDPSWarr() : co);
        bossOpts = (bo == null ? new BossOptions() : bo);
        whiteAtks = wa;

        cachedLatentGcd = 1.5f + calcOpts.getLatency() + calcOpts.getAllowedReact();
        cachedNumGcds = calcOpts.getAllowFlooring() ? (float) Math.floor(fightDuration() / latentGCD()) : fightDuration() / latentGCD();
        cachedNumGcdsU20 = calcOpts.getAllowFlooring() ? (float) Math.floor(fightDuration() / latentGCD() * calcOpts.getUnder20Perc()) : fightDuration() / latentGCD() * calcOpts.getUnder20Perc();
    }

    public FakeWhite FW;

    @Override
    public void initialize(CharacterCalculationsDPSWarr calcs) {
        super.initialize(calcs);
        calcs.fw = FW;
    }

    @Override
    protected void calcDeepWounds() {
        super.calcDeepWounds();
    }

    public float settleAll(float totalPercTimeLost, float percTimeUnder20, float availRage, float[] PercFailRage) {
        float percTimeOver20 = (1f - percTimeUnder20);
        float percTimeInDPS = (1f - totalPercTimeLost);
        float percTimeInDPSAndOver20 = percTimeOver20 * percTimeInDPS;
        availRage -= doMaintenanceActivates(totalPercTimeLost) * percTimeOver20;

        float preloopAvailGCDs = gcdsAvailable(), preloopGCDsUsed = gcdsUsed(), preloopAvailRage = availRage;

        float origNumGCDs = (calcOpts.getAllowFlooring() ? (float) Math.floor(fightDuration() / latentGCD()) : fightDuration() / latentGCD()) * percTimeOver20,
                origavailGCDs = preloopAvailGCDs * percTimeOver20,
                origGCDsused = preloopGCDsUsed * percTimeOver20;
        float oldBLSGCDs = 0f, oldMSGCDs = 0f,
                oldRDGCDs = 0f, oldOPGCDs = 0f, oldTBGCDs = 0f,
                oldSDGCDs = 0f, oldEXGCDs = 0f, oldSLGCDs = 0f,
                oldSSActs = 0f;

        AbilWrapper SL = getWrapper(Slam.class);
        AbilWrapper SD = getWrapper(Suddendeath.class);
        AbilWrapper EX = getWrapper(Execute.class);
        AbilWrapper HS = getWrapper(HeroicStrike.class);
        AbilWrapper CL = getWrapper(Cleave.class);
        AbilWrapper MS = getWrapper(MortalStrike.class);
        AbilWrapper OP = getWrapper(OverPower.class);
        AbilWrapper BLS = getWrapper(Bladestorm.class);
        AbilWrapper RD = getWrapper(Rend.class);
        AbilWrapper TB = getWrapper(TasteForBlood.class);
        AbilWrapper SS = getWrapper(Swordspec.class);

        Execute EX_ability = (Execute) EX.ability;
        Suddendeath SD_ability = (Suddendeath) SD.ability;

        SL.numActivates = origavailGCDs;
        whiteAtks.slamActsOverDur = SL.numActivates;
        SD_ability.setFreeRage(SD_ability.rageCost);
        EX_ability.setFreeRage(EX_ability.rageCost);
        float oldHSActivates = 0f, RageForHS = 0f;
        HS.numActivates = 0f;
        whiteAtks.hsOverridesOverDur = 0f;
        float newHSActivates = 0f;
        float oldCLActivates = 0f, RageForCL = 0f;
        CL.numActivates = 0f;
        whiteAtks.clOverridesOverDur = 0f;
        float newCLActivates = 0f;
        float origAvailRage = preloopAvailRage * percTimeOver20;
        boolean hsok = calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.HeroicStrike_.index];
        boolean clok = calcOpts.getMultipleTargets() && calcOpts.getMultipleTargetsPerc() > 0
                && calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Cleave_.index];
        availRage += whiteAtks.whiteRageGenOverDurNoHS() * percTimeInDPS * percTimeOver20;
        availRage -= SL.rage();
        float repassAvailRage = 0f;
        PercFailRage[0] = 1f;

        int Iterator = 0;
        // >20%
        float MSBaseCd = 6f - talents.getImprovedMortalStrike() / 3f;
        float MS_WeightedValue = MS.ability.damageOnUse() + DW.tickSize() * MS.ability.mhAtkTable().crit,
                SD_WeightedValue = SD.ability.damageOnUse() + DW.tickSize() * SD.ability.mhAtkTable().crit,
                SL_WeightedValue = SL.ability.damageOnUse() + DW.tickSize() * SL.ability.mhAtkTable().crit;
        float OnePt5Plus1 = latentGCD() + (OP.ability.cd + calcOpts.getAllowedReact()),
                Two1pt5 = latentGCD() * 2f,
                Two1pt0 = (OP.ability.cd + calcOpts.getAllowedReact()) * 2f;
        float TasteForBloodMOD = (talents.getTasteForBlood() == 3 ? 1f / 6f : (talents.getTasteForBlood() == 2 ? 0.144209288653733f : (talents.getTasteForBlood() == 1 ? 0.104925207394343f : 0)));
        float OtherMOD = (MSBaseCd + calcOpts.getLatency());
        float SDMOD = 1f - 0.03f * talents.getSuddenDeath();
        float avoid = (1f - combatFactors.cMhDodge - combatFactors.cYMiss);
        float atleast1 = 0f, atleast2 = 0f, atleast3 = 0f, extLength1, extLength2, extLength3, averageTimeBetween,
                OnePt5Plus1_Occurs, Two1pt5_Occurs, Two1PtZero_Occurs;
        float LeavingUntilNextMS_1, MSatExtra1, msNormally1, lengthFor1;
        float LeavingUntilNextMS_2, MSatExtra2, msNormally2, lengthFor2;
        float LeavingUntilNextMS_3, MSatExtra3, msNormally3, lengthFor3;
        float timeInBetween = MSBaseCd - 1.5f;
        float useExeifMSHasMoreThan, useSlamifMSHasMoreThan;
        String canUse1, canUse2, canUse3;
        float HPS;
        // Abilities
        while (
                Iterator < 50 &&
                        (
                                Math.abs(BLS.numActivates - oldBLSGCDs) > 0.1f ||
                                        Math.abs(MS.numActivates - oldMSGCDs) > 0.1f ||
                                        Math.abs(RD.numActivates - oldRDGCDs) > 0.1f ||
                                        Math.abs(OP.numActivates - oldOPGCDs) > 0.1f ||
                                        Math.abs(TB.numActivates - oldTBGCDs) > 0.1f ||
                                        Math.abs(SD.numActivates - oldSDGCDs) > 0.1f ||
                                        Math.abs(SL.numActivates - oldSLGCDs) > 0.1f ||
                                        (percTimeUnder20 > 0
                                                && Math.abs(EX.numActivates - oldEXGCDs) > 0.1f) ||
                                        (talents.getSwordSpecialization() > 0
                                                && (combatFactors.cMhItemType == rawr.base.items.ItemType.TwoHandSword || combatFactors.cMhItemType == rawr.base.items.ItemType.OneHandSword)
                                                && Math.abs(SS.numActivates - oldSSActs) > 0.1f)
                        )
        ) {
            whiteAtks.slamActsOverDur = SL.numActivates;
            oldBLSGCDs = BLS.numActivates;
            oldMSGCDs = MS.numActivates;
            oldRDGCDs = RD.numActivates;
            oldOPGCDs = OP.numActivates;
            oldTBGCDs = TB.numActivates;
            oldSDGCDs = SD.numActivates;
            oldEXGCDs = EX.numActivates;
            oldSLGCDs = SL.numActivates;
            oldSSActs = SS.numActivates;
            BLS.numActivates = 0;
            MS.numActivates = 0;
            RD.numActivates = 0;
            OP.numActivates = 0;
            TB.numActivates = 0;
            SD.numActivates = 0;
            EX.numActivates = 0;
            SL.numActivates = 0;
            availRage = origAvailRage;
            availRage += whiteAtks.whiteRageGenOverDur() * percTimeInDPS * percTimeOver20;

            float acts;
            float Abil_GCDs;
            float RDspace, BLSspace, MSspace, TFBspace, OPspace, SDspace, SLspace;
            // ==== Primary Ability Priorities ====
            // Rend
            if (RD.ability.validated()) {
                acts = Math.min(gcdsAvailable(), RD.ability.activates() * percTimeInDPSAndOver20);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                RD.numActivates = Abil_GCDs;
                availRage -= RD.rage();
            }
            RDspace = RD.numActivates / numGCDs() * RD.ability.useTime() / latentGCD();

            // Reduc abilities due to lack of Rage for maintaining the rotation
            if (repassAvailRage < 0f || PercFailRage[0] != 1f) {
                PercFailRage[0] *= 1f + repassAvailRage / (availRage - repassAvailRage);
            } else {
                PercFailRage[0] = 1f;
            }

            // Bladestorm
            if (BLS.ability.validated()) {
                acts = Math.min(gcdsAvailable(), BLS.ability.activates() * percTimeInDPSAndOver20 * PercFailRage[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                BLS.numActivates = (1f - RDspace) * Abil_GCDs;
                availRage -= BLS.rage();
            }
            BLSspace = BLS.numActivates / numGCDs() * BLS.ability.useTime() / latentGCD();
            // Mortal Strike
            if (MS.ability.validated()) {
                acts = Math.min(gcdsAvailable(), MS.ability.activates() * percTimeInDPSAndOver20 * PercFailRage[0]);
                // ===== Delays in MS from Slam or Execute =====
                if (PercFailRage[0] == 1f) {
                    HPS = landedAtksOverDur();

                    useExeifMSHasMoreThan = latentGCD() * MS_WeightedValue / (MSBaseCd * (SD_WeightedValue / latentGCD() + 0.03f * talents.getSuddenDeath() * HPS * (SD_WeightedValue - SL_WeightedValue)) + MS_WeightedValue);
                    useSlamifMSHasMoreThan = latentGCD() * MS_WeightedValue / (MSBaseCd * (SL_WeightedValue / latentGCD() + 0.03f * talents.getSuddenDeath() * HPS * (SD_WeightedValue - SL_WeightedValue)) + MS_WeightedValue);

                    LeavingUntilNextMS_1 = timeInBetween - OnePt5Plus1;
                    canUse1 = (useSlamifMSHasMoreThan < LeavingUntilNextMS_1 ? "exe or slam" : (useExeifMSHasMoreThan < LeavingUntilNextMS_1 ? "exe" : "nothing"));
                    MSatExtra1 = latentGCD() - LeavingUntilNextMS_1;
                    lengthFor1 = (MSBaseCd + calcOpts.getAllowedReact() + MSatExtra1);
                    msNormally1 = (canUse1.equals("exe or slam") ? lengthFor1 : MSBaseCd + calcOpts.getAllowedReact());

                    LeavingUntilNextMS_2 = timeInBetween - Two1pt5;
                    canUse2 = (useSlamifMSHasMoreThan < LeavingUntilNextMS_2 ? "exe or slam" : (useExeifMSHasMoreThan < LeavingUntilNextMS_2 ? "exe" : "nothing"));
                    MSatExtra2 = latentGCD() - LeavingUntilNextMS_2;
                    lengthFor2 = (MSBaseCd + calcOpts.getAllowedReact() + MSatExtra2);
                    msNormally2 = (canUse2.equals("exe or slam") ? lengthFor2 : MSBaseCd + calcOpts.getAllowedReact());

                    LeavingUntilNextMS_3 = timeInBetween - Two1pt0;
                    canUse3 = (useSlamifMSHasMoreThan < LeavingUntilNextMS_3 ? "exe or slam" : (useExeifMSHasMoreThan < LeavingUntilNextMS_3 ? "exe" : "nothing"));
                    MSatExtra3 = latentGCD() - LeavingUntilNextMS_3;
                    lengthFor3 = (MSBaseCd + calcOpts.getAllowedReact() + MSatExtra3);
                    msNormally3 = (canUse3.equals("exe or slam") ? lengthFor3 : MSBaseCd + calcOpts.getAllowedReact());

                    float Abilities = ((BLS.numActivates + RD.numActivates + SL.numActivates + acts + oldHSActivates + whiteAtks.mhActivates() + SD.numActivates) / fightDuration());

                    OnePt5Plus1_Occurs = 0f;
                    Two1PtZero_Occurs = TasteForBloodMOD
                            * OtherMOD
                            * (
                            (1f - 3f * TasteForBloodMOD)
                                    * Abilities
                                    * whiteAtks.mhAtkTable.dodge
                    )
                            * OtherMOD;
                    OnePt5Plus1_Occurs = (1f - 3f * TasteForBloodMOD)
                            * Abilities
                            * whiteAtks.mhAtkTable.dodge
                            * OtherMOD
                            + TasteForBloodMOD
                            * OtherMOD
                            - Two1PtZero_Occurs;
                    Two1pt5_Occurs = 1f - OnePt5Plus1_Occurs - Two1PtZero_Occurs;

                    // Exec procs in MS
                    atleast1 = (1f - (float) Math.pow(SDMOD, OnePt5Plus1_Occurs * ((canUse1.equals("nothing") ? 1f : 2f) * avoid + 1f * (1f - combatFactors.cYMiss)) + Two1pt5_Occurs * (canUse2.equals("niether") ? 2f : 3f) * avoid + Two1PtZero_Occurs * ((canUse2.equals("nothing") ? 0f : 1f) * avoid + 2f * (1f - combatFactors.cYMiss)) + (MSBaseCd - (1.5f + calcOpts.getAllowedReact())) / combatFactors.cMhItemSpeed));
                    atleast2 = (1f - (float) Math.pow(SDMOD, OnePt5Plus1_Occurs * ((canUse1.equals("nothing") ? 1f : 2f) * avoid + 1f * (1f - combatFactors.cYMiss)) + Two1pt5_Occurs * (canUse2.equals("nothing") ? 2f : 3f) * avoid + (MSBaseCd - (1.5f + calcOpts.getAllowedReact())) / combatFactors.cMhItemSpeed))
                            * (1f - (float) Math.pow(SDMOD, OnePt5Plus1_Occurs * ((canUse1.equals("nothing") ? 0f : 1f) * avoid + 1.5f / combatFactors.cMhItemSpeed) + Two1pt5_Occurs * ((canUse2.equals("nothing") ? 0f : 1f) * avoid + 1.5f / combatFactors.cMhItemSpeed)));
                    atleast3 = (1f - (float) Math.pow(SDMOD, Two1pt5_Occurs * (canUse2.equals("nothing") ? 2f : 3f) * avoid + (MSBaseCd - (1.5f + calcOpts.getAllowedReact())) / combatFactors.cMhItemSpeed))
                            * (1f - (float) Math.pow(SDMOD, Two1pt5_Occurs * ((canUse2.equals("nothing") ? 0f : 1f) * avoid + 1.5f / combatFactors.cMhItemSpeed)))
                            * (1f - (float) Math.pow(SDMOD, Two1pt5_Occurs * ((canUse2.equals("nothing") ? 0f : 1f) * avoid)));

                    extLength1 = (canUse1.equals("exe") ? 0.5f * (atleast1 + atleast2 + atleast3) : (canUse1.equals("exe or slam") ? 1f : 0f));
                    extLength2 = (canUse2.equals("exe") ? 0.5f * (atleast1 + atleast2 + atleast3) : (canUse2.equals("exe or slam") ? 1f : 0f));
                    extLength3 = (canUse3.equals("exe") ? 0.5f * (atleast1 + atleast2 + atleast3) : (canUse3.equals("exe or slam") ? 1f : 0f));

                    averageTimeBetween = OnePt5Plus1_Occurs * (lengthFor1 * extLength1 + msNormally1 * (1f - extLength1))
                            + Two1pt5_Occurs * (lengthFor2 * extLength2 + msNormally2 * (1f - extLength2))
                            + Two1PtZero_Occurs * (lengthFor3 * extLength3 + msNormally3 * (1f - extLength3));
                    MS.ability.cd = averageTimeBetween;
                }
                acts = Math.min(gcdsAvailable(), MS.ability.activates() * percTimeInDPSAndOver20 * PercFailRage[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                MS.numActivates = (1f - BLSspace) * Abil_GCDs;
                availRage -= MS.rage();
            }
            MSspace = MS.numActivates / numGCDs() * MS.ability.useTime() / latentGCD();
            // Taste for Blood
            if (TB.ability.validated()) {
                acts = Math.min(gcdsAvailable(), TB.ability.activates() * percTimeInDPSAndOver20 * PercFailRage[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                TB.numActivates = Abil_GCDs;
                availRage -= TB.rage();
            }
            TFBspace = TB.numActivates / numGCDs() * TB.ability.useTime() / latentGCD();
            // Overpower
            if (OP.ability.validated()) {
                OverPower _OP = (OverPower) OP.ability;
                acts = Math.min(gcdsAvailable(), _OP.getActivates(dodgedYellowsOverDur(), parriedYellowsOverDur(), SS.numActivates) * percTimeInDPSAndOver20 * PercFailRage[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                OP.numActivates = Abil_GCDs * (1f - TFBspace - RDspace - BLSspace - MSspace);
                availRage -= OP.rage();
            }
            OPspace = OP.numActivates / numGCDs() * OP.ability.useTime() / latentGCD();

            // Sudden Death
            if (SD.ability.validated()) {
                acts = Math.min(gcdsAvailable(), SD_ability.getActivates(attemptedAtksOverDur()) * percTimeInDPSAndOver20 * PercFailRage[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                SD.numActivates = (1f - MSspace) * Abil_GCDs;
                SD_ability.setFreeRage(0f);
                availRage -= SD.rage();
            }
            SDspace = (SD.numActivates / numGCDs()) * (SD.ability.useTime() / latentGCD());

            // Slam for remainder of GCDs
            if (SL.ability.validated() && PercFailRage[0] == 1f) {
                acts = Math.min(gcdsAvailable(), gcdsAvailable() * percTimeInDPS);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                if (SL.ability.getRageUseOverDur(Abil_GCDs) > availRage) Abil_GCDs = Math.max(0f, availRage) / SL.ability.rageCost;
                SL.numActivates = Abil_GCDs;
                availRage -= SL.rage();
            } else {
                SL.numActivates = 0f;
            }
            whiteAtks.slamActsOverDur = SL.numActivates;
            SLspace = SL.numActivates / numGCDs() * SL.ability.useTime() / latentGCD();

            // Sword Spec Procs
            if (SS.ability.validated()) {
                Swordspec _SS = (Swordspec) SS.ability;
                SS.numActivates = _SS.getActivates(landedYellowsOverDur(), whiteAtks.hsOverridesOverDur, whiteAtks.clOverridesOverDur) * percTimeOver20;
                availRage -= SS.rage();
            }

            float TotalSpace = (RDspace + BLSspace + MSspace + OPspace + TFBspace + SDspace + SLspace);
            repassAvailRage = availRage;
            invalidateCache();
            Iterator++;
        }
        // OnAttacks
        if (availRage > 0f && PercFailRage[0] == 1f && (hsok || clok)) {
            float savedAvailRage = availRage - whiteAtks.whiteRageGenOverDurNoHS() * percTimeInDPSAndOver20;
            float newSSActivates, oldSSActivates;
            Iterator = 0;
            do {
                OnAttack _HS = (OnAttack) HS.ability;
                OnAttack _CL = (OnAttack) CL.ability;

                availRage = savedAvailRage + whiteAtks.whiteRageGenOverDur() * percTimeInDPSAndOver20;
                oldHSActivates = HS.numActivates;
                oldCLActivates = CL.numActivates;
                oldSSActivates = SS.numActivates;

                availRage += SD.rage();
                float possibleFreeRage = availRage / (fightDuration() * percTimeOver20);
                SD_ability.setFreeRage(possibleFreeRage);
                availRage -= SD.rage();

                // Assign Rage to each ability
                float RageForHSCL = availRage * percTimeOver20;
                RageForCL = clok ? (!hsok ? RageForHSCL : RageForHSCL * (calcOpts.getMultipleTargetsPerc() / 100f)) : 0f;
                RageForHS = hsok ? RageForHSCL - RageForCL : 0f;

                float val1 = (RageForHS / _HS.fullRageCost()), val2 = (RageForCL / _CL.fullRageCost());
                if (calcOpts.getAllowFlooring()) {
                    val1 = (float) Math.floor(val1);
                    val2 = (float) Math.floor(val2);
                }
                HS.numActivates = val1;
                whiteAtks.hsOverridesOverDur = val1;
                CL.numActivates = val2;
                whiteAtks.clOverridesOverDur = val2;
                availRage -= RageForHSCL;

                if (SS.ability.validated()) {
                    Swordspec _SS = (Swordspec) SS.ability;
                    SS.numActivates = _SS.getActivates(landedYellowsOverDur(), whiteAtks.hsOverridesOverDur, whiteAtks.clOverridesOverDur);
                }

                newHSActivates = HS.numActivates;
                newCLActivates = CL.numActivates;
                newSSActivates = SS.numActivates;

                Iterator++;
            } while (Iterator < 50 && (
                    (hsok && Math.abs(newHSActivates - oldHSActivates) > 0.01f) ||
                            (clok && Math.abs(newCLActivates - oldCLActivates) > 0.01f) ||
                            (SS.ability.validated() && Math.abs(newSSActivates - oldSSActivates) > 0.01f)));
        }

        float DPS_TTL = 0f;
        float rageNeeded = 0f, rageGenOther = 0f;
        for (AbilWrapper aw : getAbilityList()) {
            DPS_TTL += aw.dps();
            hpsTtl += aw.hps();
            if (aw.rage() > 0) rageNeeded += aw.rage();
            else rageGenOther -= aw.rage();
        }

        DPS_TTL += (whiteAtks.mhDPS() + (combatFactors.useOH ? whiteAtks.ohDPS() : 0f)) * percTimeInDPSAndOver20;
        return DPS_TTL;
    }

    public float settleAll_U20(float totalPercTimeLost, float percTimeUnder20, float availRage, float[] PercFailRageUnder20) {
        float percTimeOver20 = (1f - percTimeUnder20);
        float percTimeInDPS = (1f - totalPercTimeLost);
        float percTimeInDPSAndUnder20 = percTimeUnder20 * percTimeInDPS;
        availRage -= doMaintenanceActivates(totalPercTimeLost) * percTimeUnder20;

        float preloopAvailGCDs = gcdsAvailable(), preloopGCDsUsed = gcdsUsed(), preloopAvailRage = availRage;

        float origNumGCDs = (calcOpts.getAllowFlooring() ? (float) Math.floor(fightDuration() / latentGCD()) : fightDuration() / latentGCD()) * percTimeUnder20,
                origavailGCDs = preloopAvailGCDs * percTimeUnder20,
                origGCDsused = preloopGCDsUsed * percTimeUnder20;
        float oldBLSGCDs = 0f,
                oldRDGCDs = 0f, oldOPGCDs = 0f, oldTBGCDs = 0f,
                oldSDGCDs = 0f, oldEXGCDs = 0f,
                oldSSActs = 0f;

        AbilWrapper SL = getWrapper(Slam.class);
        AbilWrapper SD = getWrapper(Suddendeath.class);
        AbilWrapper EX = getWrapper(Execute.class);
        AbilWrapper HS = getWrapper(HeroicStrike.class);
        AbilWrapper CL = getWrapper(Cleave.class);
        AbilWrapper MS = getWrapper(MortalStrike.class);
        AbilWrapper OP = getWrapper(OverPower.class);
        AbilWrapper BLS = getWrapper(Bladestorm.class);
        AbilWrapper RD = getWrapper(Rend.class);
        AbilWrapper TB = getWrapper(TasteForBlood.class);
        AbilWrapper SS = getWrapper(Swordspec.class);

        Execute EX_ability = (Execute) EX.ability;
        Suddendeath SD_ability = (Suddendeath) SD.ability;

        EX.numActivatesU20 = origavailGCDs;
        whiteAtks.slamActsOverDur = 0f;
        SD_ability.setFreeRage(SD_ability.rageCost);
        EX_ability.setFreeRage(EX_ability.rageCost);
        float oldHSActivates = 0f, RageForHS = 0f;
        HS.numActivatesU20 = 0f;
        float newHSActivates = 0f;
        float oldCLActivates = 0f, RageForCL = 0f;
        CL.numActivatesU20 = 0f;
        float newCLActivates = 0f;
        float origAvailRage = preloopAvailRage * percTimeUnder20;
        boolean hsok = false;
        boolean clok = calcOpts.getMultipleTargets() && calcOpts.getMultipleTargetsPerc() > 0
                && calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Cleave_.index];
        availRage += whiteAtks.whiteRageGenOverDurNoHS() * percTimeInDPS * percTimeUnder20;
        availRage -= EX.rageU20();
        float repassAvailRage = 0f;
        PercFailRageUnder20[0] = 1f;

        int Iterator = 0;
        // <20%
        while (
                Iterator < 50 &&
                        (
                                Math.abs(BLS.numActivates - oldBLSGCDs) > 0.1f ||
                                        Math.abs(RD.numActivates - oldRDGCDs) > 0.1f ||
                                        Math.abs(OP.numActivates - oldOPGCDs) > 0.1f ||
                                        Math.abs(TB.numActivates - oldTBGCDs) > 0.1f ||
                                        Math.abs(SD.numActivates - oldSDGCDs) > 0.1f ||
                                        (percTimeUnder20 > 0
                                                && Math.abs(EX.numActivates - oldEXGCDs) > 0.1f) ||
                                        (talents.getSwordSpecialization() > 0
                                                && (combatFactors.cMhItemType == rawr.base.items.ItemType.TwoHandSword || combatFactors.cMhItemType == rawr.base.items.ItemType.OneHandSword)
                                                && Math.abs(SS.numActivates - oldSSActs) > 0.1f)
                        )
        ) {
            oldBLSGCDs = BLS.numActivatesU20;
            oldRDGCDs = RD.numActivatesU20;
            oldOPGCDs = OP.numActivatesU20;
            oldTBGCDs = TB.numActivatesU20;
            oldSDGCDs = SD.numActivatesU20;
            oldEXGCDs = EX.numActivatesU20;
            oldSSActs = SS.numActivatesU20;
            BLS.numActivatesU20 = 0;
            RD.numActivatesU20 = 0;
            OP.numActivatesU20 = 0;
            TB.numActivatesU20 = 0;
            SD.numActivatesU20 = 0;
            EX.numActivatesU20 = 0;
            SL.numActivatesU20 = 0;
            availRage = origAvailRage;
            availRage += whiteAtks.whiteRageGenOverDur() * percTimeInDPS * percTimeUnder20;

            float acts;
            float Abil_GCDs;
            float RDspace, BLSspace, TFBspace, OPspace, SDspace, EXspace;
            // ==== Primary Ability Priorities ====
            // Rend
            if (RD.ability.validated()) {
                acts = Math.min(gcdsAvailableU20(), RD.ability.activates() * percTimeInDPSAndUnder20);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                RD.numActivatesU20 = Abil_GCDs;
                availRage -= RD.rageU20();
            }
            RDspace = RD.numActivatesU20 / numGCDs() * RD.ability.useTime() / latentGCD();

            // Reduc abilities due to lack of Rage for maintaining the rotation
            if (repassAvailRage < 0f || PercFailRageUnder20[0] != 1f) {
                PercFailRageUnder20[0] *= 1f + repassAvailRage / (availRage - repassAvailRage);
            } else {
                PercFailRageUnder20[0] = 1f;
            }

            // Bladestorm
            if (BLS.ability.validated()) {
                acts = Math.min(gcdsAvailableU20(), BLS.ability.activates() * percTimeInDPSAndUnder20 * PercFailRageUnder20[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                BLS.numActivatesU20 = (1f - RDspace) * Abil_GCDs;
                availRage -= BLS.rageU20();
            }
            BLSspace = BLS.numActivatesU20 / numGCDs() * BLS.ability.useTime() / latentGCD();
            // Taste for Blood
            if (TB.ability.validated()) {
                acts = Math.min(gcdsAvailableU20(), TB.ability.activates() * percTimeInDPSAndUnder20 * PercFailRageUnder20[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                TB.numActivatesU20 = Abil_GCDs;
                availRage -= TB.rageU20();
            }
            TFBspace = TB.numActivatesU20 / numGCDs() * TB.ability.useTime() / latentGCD();
            // Overpower
            if (OP.ability.validated()) {
                OverPower _OP = (OverPower) OP.ability;
                acts = Math.min(gcdsAvailableU20(), _OP.getActivates(dodgedYellowsOverDur(), parriedYellowsOverDur(), SS.numActivatesU20) * percTimeInDPSAndUnder20 * PercFailRageUnder20[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                OP.numActivatesU20 = Abil_GCDs * (1f - TFBspace - RDspace - BLSspace);
                availRage -= OP.rageU20();
            }
            OPspace = OP.numActivatesU20 / numGCDs() * OP.ability.useTime() / latentGCD();

            // Sudden Death
            if (SD.ability.validated()) {
                acts = Math.min(gcdsAvailableU20(), SD_ability.getActivates(attemptedAtksOverDur()) * percTimeInDPSAndUnder20 * PercFailRageUnder20[0]);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                SD.numActivatesU20 = (1f) * Abil_GCDs;
                SD_ability.setFreeRage(0f);
                availRage -= SD.rageU20();
            }
            SDspace = (SD.numActivatesU20 / numGCDs()) * (SD.ability.useTime() / latentGCD());

            // Execute for remainder of GCDs
            if (EX.ability.validated()) {
                acts = Math.min(gcdsAvailableU20(), gcdsAvailableU20() * percTimeInDPS);
                Abil_GCDs = calcOpts.getAllowFlooring() ? (float) Math.floor(acts) : acts;
                if (EX.ability.getRageUseOverDur(Abil_GCDs) > availRage) Abil_GCDs = Math.max(0f, availRage) / EX.ability.rageCost;
                EX.numActivatesU20 = Abil_GCDs;
                availRage -= EX.rageU20();
            } else {
                EX.numActivatesU20 = 0f;
            }
            EXspace = EX.numActivatesU20 / numGCDs() * EX.ability.useTime() / latentGCD();

            // Sword Spec Procs
            if (SS.ability.validated()) {
                Swordspec _SS = (Swordspec) SS.ability;
                SS.numActivatesU20 = _SS.getActivates(landedYellowsOverDur(), whiteAtks.hsOverridesOverDur, whiteAtks.clOverridesOverDur) * percTimeUnder20;
                availRage -= SS.rageU20();
            }

            float TotalSpace = (RDspace + BLSspace + OPspace + TFBspace + SDspace + EXspace);
            repassAvailRage = availRage;
            invalidateCache();
            Iterator++;
        }
        // OnAttacks (C# `if (false && ...)`, dead branch kept for parity)
        if (false && availRage > 0f && PercFailRageUnder20[0] == 1f && (hsok || clok)) {
            float savedAvailRage = availRage - whiteAtks.whiteRageGenOverDurNoHS();
            float newSSActivates, oldSSActivates;
            Iterator = 0;
            do {
                OnAttack _HS = (OnAttack) HS.ability;
                OnAttack _CL = (OnAttack) CL.ability;

                availRage = savedAvailRage + whiteAtks.whiteRageGenOverDur() * percTimeInDPS * percTimeOver20;
                oldHSActivates = HS.numActivates;
                oldCLActivates = CL.numActivates;
                oldSSActivates = SS.numActivates;

                availRage += SD.rageU20();
                float possibleFreeRage = availRage / (fightDuration() * percTimeOver20);
                SD_ability.setFreeRage(possibleFreeRage);
                availRage -= SD.rageU20();

                float RageForHSCL = availRage * percTimeOver20;
                RageForCL = clok ? (!hsok ? RageForHSCL : RageForHSCL * (calcOpts.getMultipleTargetsPerc() / 100f)) : 0f;
                RageForHS = hsok ? RageForHSCL - RageForCL : 0f;

                float val1 = (RageForHS / _HS.fullRageCost()), val2 = (RageForCL / _CL.fullRageCost());
                if (calcOpts.getAllowFlooring()) {
                    val1 = (float) Math.floor(val1);
                    val2 = (float) Math.floor(val2);
                }
                HS.numActivates = val1;
                whiteAtks.hsOverridesOverDur = val1;
                CL.numActivates = val2;
                whiteAtks.clOverridesOverDur = val2;
                availRage -= RageForHSCL;

                if (SS.ability.validated()) {
                    Swordspec _SS = (Swordspec) SS.ability;
                    SS.numActivates = _SS.getActivates(landedYellowsOverDur(), whiteAtks.hsOverridesOverDur, whiteAtks.clOverridesOverDur);
                }

                newHSActivates = HS.numActivates;
                newCLActivates = CL.numActivates;
                newSSActivates = SS.numActivates;

                Iterator++;
            } while (Iterator < 50 && (
                    (hsok && Math.abs(newHSActivates - oldHSActivates) > 0.01f) ||
                            (clok && Math.abs(newCLActivates - oldCLActivates) > 0.01f) ||
                            (SS.ability.validated() && Math.abs(newSSActivates - oldSSActivates) > 0.01f)));
        }

        float DPS_TTL = 0f;
        float rageNeeded = 0f, rageGenOther = 0f;
        for (AbilWrapper aw : getAbilityList()) {
            DPS_TTL += aw.dpsU20();
            hpsTtl += aw.hpsU20();
            if (aw.rageU20() > 0) rageNeeded += aw.rageU20();
            else rageGenOther -= aw.rageU20();
        }

        DPS_TTL += (whiteAtks.mhDPS() + (combatFactors.useOH ? whiteAtks.ohDPS() : 0f)) * percTimeInDPSAndUnder20;
        return DPS_TTL;
    }

    public void makeRotationAndDoDps(boolean setCalcs, float percTimeUnder20) {
        if (char_.getMainHand() == null) {
            return;
        }
        hpsTtl = 0f;
        if (needDisplayCalcs) GCDUsage += DPSWarrFormat.zeroPadFixed(numGCDs(), 3, 0) + " : Total GCDs\n\n";

        float TotalPercTimeLost = calculateTimeLost(getWrapper(MortalStrike.class).ability);

        if (needDisplayCalcs) {
            GCDUsage += (TotalPercTimeLost != 0f ? "\n" : "");
        }

        // ==== Rage Generation Priorities ========
        float availRage = 0f;
        float[] PercFailRageUnder20 = new float[] { 1f };
        float[] PercFailRage = new float[] { 1f };
        availRage += rageGenOverDur_Other() + RageGainedWhileMoving;

        // ==== Standard Priorities ===============
        dpsTtl = settleAll(TotalPercTimeLost, percTimeUnder20, availRage, PercFailRage);

        if (percTimeUnder20 != 0) {
            dpsTtlU20 = settleAll_U20(TotalPercTimeLost, percTimeUnder20, availRage, PercFailRageUnder20);
        }

        calcDeepWounds();
        dpsTtl += DW.tickSize();

        if (needDisplayCalcs) {
            if (PercFailRage[0] != 1.0f || PercFailRageUnder20[0] != 1.0f) {
                GCDUsage += (PercFailRage[0] < 1.0f ? String.format("WARNING! You are losing %s of your abilities due to Rage Starvation before Exec Spam.\n", DPSWarrFormat.percentCustom(1f - PercFailRage[0], 1, 1)) : "");
                GCDUsage += (PercFailRageUnder20[0] < 1.0f ? String.format("WARNING! You are losing %s of your abilities due to Rage Starvation during Exec Spam.\n", DPSWarrFormat.percentCustom(1f - PercFailRageUnder20[0], 1, 1)) : "");
                GCDUsage += "\n";
            }
            for (AbilWrapper aw : getDamagingAbilities()) {
                if (aw.allNumActivates() > 0)
                    GCDUsage += DPSWarrFormat.zeroPadFixed(aw.allNumActivates(), 3, calcOpts.getAllowFlooring() ? 0 : 2) + " : " + aw.ability.name + (aw.ability.usesGCD ? "\n" : "(Doesn't use GCDs)\n");
            }
            GCDUsage += "\n" + DPSWarrFormat.zeroPadFixed(gcdsAvailable() + gcdsAvailableU20(), 3, 0) + " : Avail GCDs";
        }

        if (setCalcs) {
            this.calcs.setTotalDPS(dpsTtl + dpsTtlU20);
            this.calcs.whiteDPS = whiteAtks.mhDPS() + whiteAtks.ohDPS();
            this.calcs.whiteDPSMH = whiteAtks.mhDPS();
            this.calcs.whiteDmg = this.whiteAtks.mhDamageOnUse();

            this.calcs.whiteRage = whiteAtks.mhRageGenOverDur();
            this.calcs.otherRage = this.rageGenOverDur_Other();
            this.calcs.needyRage = this.rageNeededOverDur();
            this.calcs.freeRage = calcs.whiteRage + calcs.otherRage - calcs.needyRage;
        }
    }

    @Override
    public void makeRotationAndDoDps(boolean setCalcs, boolean needsDisplayCalculations) {
        super.makeRotationAndDoDps(setCalcs, needsDisplayCalculations);
        float PercTimeUnder20 = 0f;
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.ExecuteSpam_.index]) {
            PercTimeUnder20 = calcOpts.getUnder20Perc();
        }
        makeRotationAndDoDps(setCalcs, PercTimeUnder20);
    }
}
