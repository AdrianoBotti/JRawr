// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/RotationTest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the C# Debug.Flush() engine call is omitted (debug output only).
package rawr.models.hunter;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.models.hunter.HunterEnums.PetFamily;
import rawr.models.hunter.HunterEnums.Shots;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

class RotationTest {
    // passed in by the caller
    private Character character;
    private CharacterCalculationsHunter calculatedStats;
    private CalculationOptionsHunter calcOpts;
    private BossOptions bossOpts;

    // used for calculation
    private Map<Shots, RotationShotInfo> shotData;
    private RotationShotInfo[] shotTable;
    private Random rand = new Random();

    // the output
    public float testTimeElapsed = 0;
    public int lalProcCount = 0;
    public int issProcsAimed = 0;
    public int issProcsArcane = 0;
    public int issProcsChimera = 0;
    public float issAimedUptime = 0;
    public float issArcaneUptime = 0;
    public float issChimeraUptime = 0;
    public int iAotHProcs = 0;
    public float iAotHTime = 0;
    public float iAotHUptime = 0;

    public RotationTest(Character character, CharacterCalculationsHunter calculatedStats, CalculationOptionsHunter calcOpts, BossOptions bossOpts) {
        this.character = character;
        this.calcOpts = calcOpts;
        this.bossOpts = bossOpts;
        this.calculatedStats = calculatedStats;
        // 091109 Drizz: Added
        this.calculatedStats.sequence = "Time  :Shot          :CastTime:Done   :CD Until" + System.lineSeparator();
    }

    // 091109 Drizz: Added to be able to pull infor from "outside"
    public RotationShotInfo[] getRotationTable() {
        return shotTable;
    }

    public void runTest() {
        float fightDuration = calcOpts.duration;
        float latency = calcOpts.getLatency();
        float cdCutoff = calcOpts.cdCutoff;
        float longevity = character.getHunterTalents().getLongevity();
        float gcd = 1.5f;
        float ba = calculatedStats.blackArrow.duration;
        float it = calculatedStats.immolationTrap.duration;
        float et = calculatedStats.explosiveTrap.duration;
        float ft = calculatedStats.freezingTrap.duration;
        float frt = calculatedStats.frostTrap.duration;
        float vly = calculatedStats.volley.duration;
        float lalChance = character.getHunterTalents().getBlackArrow() == 1 ? character.getHunterTalents().getLockAndLoad() * calcOpts.lalProcChance : -1;
        boolean randomProcs = calcOpts.randomizeProcs;
        int iSSfix = 0;
        int iAotHfix = 0;
        int lalFix = 0;
        float autoShotSpeed = 2; // Speed at which we shoot auto-shots
        float iAotHChance = character.getHunterTalents().getImprovedAspectOfTheHawk() > 0 ? 10 : -1;
        float waitForESCS = calcOpts.waitForCooldown;
        boolean interleaveLAL = calcOpts.interleaveLAL;
        boolean arcAimedPrio = calcOpts.prioritiseArcAimedOverSteady;
        float iSSTalent = character.getHunterTalents().getImprovedSteadyShot() * 5;
        float iSSDuration = -1;
        int iSSprocsAimed = 0;
        int iSSprocsArcane = 0;
        int iSSprocsChimera = 0;
        float bossHPPercentage = calcOpts.bossHPPerc * 100;
        float sub20Time = (bossHPPercentage > 20) ? fightDuration - calcOpts.timeSpentSub20 : 0; // time *until* we hit sub-20
        boolean useKillShot = calculatedStats.priorityRotation.containsShot(Shots.KillShot);
        boolean bmSpec = character.getHunterTalents().getBestialWrath() + character.getHunterTalents().getTheBeastWithin() > 0;

        if (calcOpts.petFamily == PetFamily.None) bmSpec = false;

        int currentShot;
        float currentTime;
        float elapsedTime;
        float bwTime;
        float rfTime;
        float volleyTime;
        float bwCD;
        float rfCD;
        float vlyCD;
        float lalTimer;      // timer till current serpent sting expires
        float lalShots;      // amount of free shots left
        float lalDuration;   // time since last Explosive Shot while having L&L up
        float lastLALCheck;  // time since last check for proc
        float lastLALProc;   // time since last proc
        int lalProcs;        // Amount of procs
        Shots lalType;       // Type of L&L proc, i.e. Immolation trap or Black Arrow
        float iAotHDuration; // Time until buff expires
        float lastAutoShotCheck; // Last time we checked for IAotH procs
        int iAotHProcs;
        boolean serpentUsed; // Have we used Serpent Sting yet?

        // Initialize shotData & shotTable
        shotData = new LinkedHashMap<Shots, RotationShotInfo>(); // the shot info (mana, cooldown, etc)
        shotTable = new RotationShotInfo[10]; // the rotation info

        for (int i = 0; i < calculatedStats.priorityRotation.priorities.length; i++) {
            ShotData shot = calculatedStats.priorityRotation.priorities[i];
            if (shot != null && shot.type != Shots.None) {
                shotData.put(shot.type, new RotationShotInfo(shot));
                shotTable[i] = shotData.get(shot.type);
            } else {
                shotTable[i] = null;
            }
        }

        // Shot Timing Adjustments
        // we need to adjust rapid fire cooldown, since we accounted for readiness initially
        if (shotData.containsKey(Shots.RapidFire)) {
            shotData.get(Shots.RapidFire).cooldown = (5 - character.getHunterTalents().getRapidKilling()) * 60f;
        }

        // set Steady Shot cast time so it's only static haste
        if (shotData.containsKey(Shots.SteadyShot)) {
            shotData.get(Shots.SteadyShot).castTime = 2f / (1f + calculatedStats.hasteStaticTotal);
        }

        // set Steady Shot cast time so it's only static haste
        if (shotData.containsKey(Shots.Volley)) {
            shotData.get(Shots.Volley).castTime = 6f;
        }

        // Set Auto Shot Speed to statically hasted value
        autoShotSpeed = calculatedStats.autoShotStaticSpeed;

        // Variable setup
        // these are used by both the frequency rotation and here in the rotation test
        boolean chimeraRefreshesSerpent = containsShot(Shots.ChimearaShot) && containsShot(Shots.SerpentSting);

        currentShot = 1;
        currentTime = 0;
        elapsedTime = 0;

        // Lock And Load variables
        lalTimer = -1;
        lalShots = -1;
        lalDuration = -1;
        lastLALCheck = -1;
        lastLALProc = -50;
        lalType = Shots.None;

        // IAotH variables
        // 021109 Drizz: Updating init variables to align with v92b spreadsheet
        lastAutoShotCheck = 0; // -50;
        iAotHDuration = 0;// -1;
        iAotHProcs = 0;

        serpentUsed = false;

        // some things not initialized in the spreadsheet
        bwTime = 0;
        volleyTime = 0;
        rfTime = 0;
        bwCD = 0;
        rfCD = 0;
        vlyCD = 0;
        currentTime = 0;
        lalProcs = 0;
        float iAotHUptime = 0;

        // The Main Loop
        do {
            boolean haveShot = false;
            Shots thisShot = Shots.None;

            // Find shot - Non-GCD

            // Check shots that are off the GCD
            for (int i = 0; i < shotTable.length && !haveShot; i++) {
                RotationShotInfo s = shotTable[i];
                if (s != null) {
                    if (s.type == Shots.BestialWrath && bwTime > currentTime) {
                        // do nothing if Rapid Fire is still active
                    } else if (s.type == Shots.RapidFire && rfTime > currentTime) {
                        // Check if TBW/BW CD is nearly up if we want to use Rapid Fire
                    } else if (bmSpec && s.type == Shots.Readiness && bwCD < (currentTime + cdCutoff)) {
                        // Wait for Rapid Fire if it's close too
                    } else if (s.type == Shots.Readiness && rfCD < (currentTime + cdCutoff)) {
                        // wait for Rapid Fire
                    } else if (s.type == Shots.BestialWrath && !bmSpec) {
                        // do nothing
                    } else {
                        // check if the shot is off the GCD
                        if (s.noGcd && s.timeUntilOffCd <= currentTime) {
                            thisShot = s.type;
                            haveShot = true;
                        }
                    }
                }
            }

            // Find Shot - Any

            // Check all shots whether they are off CD
            for (int i = 0; i < shotTable.length && !haveShot; i++) {
                RotationShotInfo s = shotTable[i]; // known as thisShot
                if (s != null) {
                    // During L&L proc Explosive Shot gets priority over everything else
                    // Do nothing if L&L just proc'ed and ES is still on CD
                    if (s.timeUntilOffCd > currentTime && lalShots == 3 && s.type == Shots.ExplosiveShot) {
                    }
                    //  First L&L proc shot
                    else if (s.timeUntilOffCd < currentTime && lalShots == 3 && s.type == Shots.ExplosiveShot) {
                        s.timeUntilOffCd = 0;
                        lalShots--;
                        haveShot = true;
                        thisShot = s.type;
                    }
                    // If We're interleaving, make sure the last ES was at least 2 seconds before this
                    else if (interleaveLAL && lalShots < 3 && lalShots > 0 && lalDuration - currentTime > -2 && lalDuration - currentTime <= 0 && s.type == Shots.ExplosiveShot) {
                        currentTime += 0.5f;
                        s.timeUntilOffCd = 0;
                        haveShot = true;
                        thisShot = s.type;
                        lalShots--;
                    }
                    // If we're not interleaving other shots with L&L procs reset ES cooldown and advance timer by 0.5 seconds (to miss the last tick)
                    else if (!interleaveLAL && lalShots < 3 && lalShots > 0 && s.type == Shots.ExplosiveShot) {
                        s.timeUntilOffCd = 0;
                        haveShot = true;
                        thisShot = s.type;

                        lalShots--;
                    }

                    float waitTime = (float) HunterFormat.roundHalfEven(s.timeUntilOffCd - currentTime, 2);

                    // Check if shots with CDs come off CD soon enough to wait for them, excluding stings
                    if (!haveShot && waitTime > 0 && s.timeUntilOffCd > currentTime && s.timeUntilOffCd <= currentTime + waitForESCS
                            && s.type != Shots.SerpentSting && s.type != Shots.BlackArrow) {
                        if (!arcAimedPrio && (thisShot == Shots.AimedShot || thisShot == Shots.ArcaneShot || thisShot == Shots.MultiShot)) {
                            // do nothing if we don't want to prioritise Aimed/Arcane or Multi-Shot
                        }
                        //29-10-2009 Drizz : Added one else if check to align with Spreadsheet v92b
                        else if (s.type == Shots.Readiness && rfCD < (currentTime + cdCutoff + waitTime)) {
                            // Do nothing if we Rapid Fire isn't soon enough of CD
                        }
                        // ************************************************************************
                        else if (s.checkGcd) {
                            boolean result = true;

                            // Check if other shots that are about to come off CD do more damage than this one
                            for (int j = i; j < shotTable.length && result; j++) {
                                RotationShotInfo thatShot = shotTable[j];
                                if (thatShot != null) {
                                    // 021109 Drizz: Removed the last && on this line (align with Worksheet 92b.
                                    if (thatShot.timeUntilOffCd > currentTime && thatShot.timeUntilOffCd <= currentTime + waitForESCS) {
                                        if (compareDamage(thisShot, thatShot.type, waitTime) != thisShot) {
                                            result = false;
                                        }
                                    }
                                }
                            }

                            if (result) {
                                currentTime = s.timeUntilOffCd;
                                thisShot = s.type;
                                haveShot = true;
                            }
                        }
                    }

                    // this is a horrible round hack, i know. the issue probably comes
                    // somewhere from a float->float cast.
                    // the issue is that we have currentTime at (e.g.) 65.35 and steady shot CD is at 65.3500000001
                    // so we skip forward by 0.1 seconds when we didn;t really need to.
                    if (!haveShot && s.timeUntilOffCd <= currentTime + 0.00001) {
                        if (s.type == Shots.SerpentSting && chimeraRefreshesSerpent && serpentUsed) {
                            // do nothing for refreshed Serpent Sting except the first time
                        } else if (s.type == Shots.KillShot && (!useKillShot || currentTime < sub20Time)) {
                            // Do not use Kill Shot if Boss HP is above 20%
                        } else if (s.type == Shots.Volley && volleyTime > currentTime) {
                            // do nothing if Volley is still active
                        } else if (s.type == Shots.BestialWrath && bwTime > currentTime) {
                            // do nothing if TBW or BW is still active
                        } else if (s.type == Shots.RapidFire && rfTime > currentTime) {
                            // do nothing if Rapid Fire is still active
                        } else if (bmSpec && s.type == Shots.Readiness && bwCD < (currentTime + cdCutoff)) {
                            // Check if TBW/BW CD is nearly up if we want to use Rapid Fire
                        } else if (s.type == Shots.Readiness && rfCD < (currentTime + cdCutoff)) {
                            // Wait for Rapid Fire if it's close too
                        } else if (s.type == Shots.SerpentSting && fightDuration - currentTime < s.cooldown) {
                            // do nothing if Rapid Fire is about to come off CD
                            // do nothing if we don't get all the ticks of Serpent Sting anyway
                        } else if (s.type == Shots.BlackArrow && fightDuration - currentTime < ba) {
                            // ditto for Black Arrow
                        } else if (s.type == Shots.BestialWrath && !bmSpec) {
                            // do nothing
                        } else {
                            thisShot = s.type;
                            haveShot = true;
                        }
                    }
                }
            }

            // Execute shot

            if (!haveShot) {
                // If no shot, advance 100 ms
                // This case should almost never happen unless Steady Shot isn't in the rotation
                currentTime += 0.1f;
            } else {
                float currentAutoShotSpeed;

                currentAutoShotSpeed = autoShotSpeed;
                RotationShotInfo thisShotInfo = shotData.get(thisShot);

                if (thisShotInfo.type == Shots.SerpentSting && !serpentUsed) {
                    serpentUsed = true;
                }

                // Adjust AutoShot Speed if Rapid Fire is active
                if (rfTime > currentTime) {
                    currentAutoShotSpeed *= 0.6f;
                }

                // Check if we had an auto shot since the last check
                if (currentTime - lastAutoShotCheck > currentAutoShotSpeed) {
                    // *******************************************************
                    // 021109 Drizz: Added if around LastAutoShotCheck, Updated from v92b (spreadsheet)
                    if (currentTime == 0)
                        lastAutoShotCheck = currentTime;
                    else
                        lastAutoShotCheck += currentAutoShotSpeed;
                    // *******************************************************

                    // 021109 Drizz: Updating to use "LastAutoShotCheck" instead of currentTime (Align with v92b)
                    if (randomProcs) {
                        // 10% proc chance
                        if (randomProc(iAotHChance)) {
                            if (iAotHDuration - lastAutoShotCheck < 0) {
                                // No IAotH proc active
                                iAotHUptime = iAotHUptime + 12;
                            } else {
                                iAotHUptime = iAotHUptime - (iAotHDuration - lastAutoShotCheck) + 12;
                            }
                            iAotHDuration = lastAutoShotCheck + 12;
                            iAotHProcs = iAotHProcs + 1;
                        }
                    } else if (iAotHChance > 0) {
                        iAotHfix = iAotHfix + 1;
                        if (iAotHfix * iAotHChance > 100) {
                            iAotHfix = 0; // reset counter
                            if (iAotHDuration - lastAutoShotCheck < 0) {
                                // No IAotH proc active
                                iAotHUptime = iAotHUptime + 12;
                            } else {
                                iAotHUptime = iAotHUptime - (iAotHDuration - lastAutoShotCheck) + 12;
                            }
                            iAotHDuration = lastAutoShotCheck + 12;
                            iAotHProcs = iAotHProcs + 1;
                        }
                    }
                }

                // count shots
                currentShot++;

                // advance row on the test page
                // row++

                thisShotInfo.countUsed++;

                // Determine Steady Shot haste
                float sSHaste = 1f;
                if (rfTime > currentTime) sSHaste *= 1.4f;

                // Increment cooldown on the shot just fired
                if (thisShot == Shots.SteadyShot && thisShotInfo.castTime * (1f / sSHaste) < gcd + latency) {
                    thisShotInfo.timeUntilOffCd = currentTime + gcd + latency;
                } else if (thisShot == Shots.SteadyShot) {
                    thisShotInfo.timeUntilOffCd = currentTime + thisShotInfo.castTime * (1f / sSHaste) + latency;
                } else if (thisShot == Shots.Volley) {
                    thisShotInfo.timeUntilOffCd = currentTime + Math.max(thisShotInfo.castTime, thisShotInfo.cooldown) + latency;
                } else if (checkGCD(thisShot)) {
                    thisShotInfo.timeUntilOffCd = currentTime + thisShotInfo.cooldown + latency;
                } else {
                    // Shots that are off the GCD do not incur latency
                    thisShotInfo.timeUntilOffCd = currentTime + thisShotInfo.cooldown;
                }

                // Note down shot current time and shot used
                // 091109 Drizz: If we now can get the debug info within Rawr, no need to go by debug option.
                if (calculatedStats.collectSequence) {
                    float timeUsed = 0;
                    float castEnd = currentTime;
                    float onCDUntil = currentTime + thisShotInfo.cooldown;

                    // Note down cast time used, cast end time and time till CD
                    if (!checkGCD(thisShot)) {
                        // These do not cost time
                    } else {
                        // Steady Shot can fire faster than GCD so check
                        if (thisShot == Shots.SteadyShot) {
                            timeUsed = thisShotInfo.castTime * (1 / sSHaste) + latency;
                            castEnd = currentTime + thisShotInfo.castTime * (1 / sSHaste) + latency;
                        } else if (thisShot == Shots.Volley) {
                            timeUsed = thisShotInfo.castTime + latency;
                            castEnd = currentTime + thisShotInfo.castTime + latency;
                        } else {
                            // Other shots fire at GCD + Latency
                            timeUsed = gcd + latency;
                            castEnd = currentTime + gcd + latency;
                        }
                        onCDUntil = thisShotInfo.timeUntilOffCd;
                    }
                    calculatedStats.sequence += String.format(java.util.Locale.ROOT, "%6.2f:%-13s : %7.3f:%7.3f:%7.3f", currentTime, thisShot, timeUsed, castEnd, onCDUntil) + System.lineSeparator();
                }

                // Set L&L Timer after Black Arrow/Immolation Trap was used
                if (thisShot == Shots.BlackArrow
                        || thisShot == Shots.ImmolationTrap
                        || thisShot == Shots.ExplosiveTrap
                        || thisShot == Shots.FreezingTrap
                        || thisShot == Shots.FrostTrap) {
                    switch (thisShot) {
                        case BlackArrow: {
                            lalTimer = currentTime + ba;
                            break;
                        }
                        case ImmolationTrap: {
                            lalTimer = currentTime + it;
                            break;
                        }
                        case ExplosiveTrap: {
                            lalTimer = currentTime + et;
                            break;
                        }
                        case FreezingTrap: {
                            lalTimer = currentTime + ft;
                            break;
                        }
                        default: {
                            lalTimer = currentTime + frt;
                            break;
                        } // Shots.FrostTrap
                    }
                    lalType = thisShot;
                    lastLALCheck = currentTime - 3;
                }

                if (currentTime + thisShotInfo.cooldown > elapsedTime) {
                    if (thisShotInfo.cooldown < gcd + latency) {
                        elapsedTime = currentTime + gcd + latency;
                    } else {
                        elapsedTime = currentTime + thisShotInfo.cooldown;
                    }
                }

                // If we have Improved Steady Shot and have just cast Steady Shot
                if (iSSTalent > 0 && thisShot == Shots.SteadyShot) {
                    if (randomProcs) {
                        if (randomProc(iSSTalent)) {
                            iSSDuration = currentTime + 12;
                        }
                    } else if (iSSTalent > 0) {
                        iSSfix++;
                        if (iSSfix * iSSTalent > 100) {
                            iSSfix = 0;
                            iSSDuration = currentTime + 12;
                        }
                    }
                }

                if (iSSDuration > currentTime) {
                    if (thisShot == Shots.AimedShot) {
                        iSSprocsAimed++;
                        iSSDuration = -1;
                    } else if (thisShot == Shots.ArcaneShot) {
                        iSSprocsArcane++;
                        iSSDuration = -1;
                    } else if (thisShot == Shots.ChimearaShot) {
                        iSSprocsChimera++;
                        iSSDuration = -1;
                    }
                }

                // If we used Explosive Shot, set the duration of the debuff
                if (thisShot == Shots.ExplosiveShot && lalShots > 0) {
                    lalDuration = currentTime + 2;
                }

                // If we used Explosive Shot, set Arcane Shot on CD as well and likewise
                if (thisShot == Shots.ArcaneShot && shotData.containsKey(Shots.ExplosiveShot)) {
                    shotData.get(Shots.ExplosiveShot).timeUntilOffCd = currentTime + shotData.get(Shots.ExplosiveShot).cooldown + latency;
                }
                if (thisShot == Shots.ExplosiveShot && shotData.containsKey(Shots.ArcaneShot)) {
                    shotData.get(Shots.ArcaneShot).timeUntilOffCd = currentTime + shotData.get(Shots.ArcaneShot).cooldown + latency;
                }

                // If we used Multi-Shot set Aimed Shot cooldown as well and vice-versa
                if (thisShot == Shots.MultiShot && shotData.containsKey(Shots.AimedShot)) {
                    shotData.get(Shots.AimedShot).timeUntilOffCd = currentTime + shotData.get(Shots.AimedShot).cooldown + latency;
                }
                if (thisShot == Shots.AimedShot && shotData.containsKey(Shots.MultiShot)) {
                    shotData.get(Shots.MultiShot).timeUntilOffCd = currentTime + shotData.get(Shots.MultiShot).cooldown + latency;
                }

                // If Black Arrow is active (LALTimer) do check a proc every 3 seconds and if no LAL proc has occured within the last 22 seconds
                if (lalTimer > currentTime && currentTime - lastLALCheck > 3 && lalDuration < currentTime && currentTime - lastLALProc > 22) {
                    if (randomProcs) {
                        // And currentTime - LastLALProc > 30 Then --- currently no ICD
                        if (randomProc(lalChance)) {
                            // L&L procs
                            lalShots = 3;
                            lastLALProc = currentTime;
                            lalProcs++;
                        }
                    } else if (lalChance > 0) {
                        lalFix++;
                        if (lalFix * lalChance > 100) {
                            // L&L procs
                            lalShots = 3;
                            lastLALProc = currentTime;
                            lalProcs++;
                            lalFix = 0;
                        }
                    }
                    lastLALCheck = currentTime;
                }

                // If we used Beast Within, set duration to 18 seconds
                if (thisShot == Shots.BestialWrath) {
                    bwTime = currentTime + 10;
                    // also record the time when it comes off CD
                    bwCD = thisShotInfo.timeUntilOffCd;
                }

                // If we used Volley, set duration to 6 seconds
                if (thisShot == Shots.Volley) {
                    volleyTime = currentTime + thisShotInfo.castTime;
                    // also record the time when it comes off CD
                    vlyCD = thisShotInfo.timeUntilOffCd;
                }

                if (thisShot == Shots.RapidFire) {
                    rfTime = currentTime + 15;
                    // also record the time when it comes off CD
                    rfCD = thisShotInfo.timeUntilOffCd;
                }

                // If Readiness is used, reset CD on other shots
                if (thisShot == Shots.Readiness) {
                    // Reset everything but Readiness and Serpent Sting and TBW
                    for (int i = 0; i < shotTable.length; i++) {
                        RotationShotInfo s = shotTable[i];
                        if (s != null) {
                            if (s.type != Shots.Readiness
                                    && s.type != Shots.SerpentSting
                                    && s.type != Shots.BestialWrath
                                    && s.type != Shots.Volley) {
                                s.timeUntilOffCd = 0;
                            }
                        }
                    }
                }

                // Advance time by 0 if the shot is off the GCD, otherwise by GCD+Latency
                if (!checkGCD(thisShot)) {
                    // do nothing
                } else if (thisShot == Shots.SteadyShot && thisShotInfo.castTime * (1f / sSHaste) < gcd + latency) {
                    currentTime += gcd + latency;
                } else if (thisShot == Shots.SteadyShot) {
                    currentTime += thisShotInfo.castTime * (1f / sSHaste) + latency;
                } else if (thisShot == Shots.Volley) {
                    currentTime += thisShotInfo.castTime + latency;
                } else {
                    currentTime += gcd + latency;
                }
                // more here...
            }
        } while (currentTime < fightDuration - 1);

        // Save Results
        for (RotationShotInfo info : shotData.values()) {
            info.frequency = info.countUsed > 0 ? currentTime / info.countUsed : 0;
        }

        for (int i = 0; i < calculatedStats.priorityRotation.priorities.length; i++) {
            ShotData s = calculatedStats.priorityRotation.priorities[i];
            if (s != null) {
                s.setFrequency(calculatedStats.priorityRotation, shotData.containsKey(s.type) ? shotData.get(s.type).frequency : 0);
            }
        }

        this.testTimeElapsed = currentTime;
        this.issProcsAimed = iSSprocsAimed;
        this.issProcsArcane = iSSprocsArcane;
        this.issProcsChimera = iSSprocsChimera;
        this.iAotHProcs = iAotHProcs;
        this.iAotHTime = iAotHUptime;
        this.lalProcCount = lalProcs;

        this.iAotHUptime = this.testTimeElapsed > 0 ? 1.0f * this.iAotHTime / this.testTimeElapsed : 0;
        this.issAimedUptime = this.issProcsAimed > 0 ? 1.0f * this.issProcsAimed / shotData.get(Shots.AimedShot).countUsed : 0;
        this.issArcaneUptime = this.issProcsArcane > 0 ? 1.0f * this.issProcsArcane / shotData.get(Shots.ArcaneShot).countUsed : 0;
        this.issChimeraUptime = this.issProcsChimera > 0 ? 1.0f * this.issProcsChimera / shotData.get(Shots.ChimearaShot).countUsed : 0;
    }

    private boolean randomProc(float chance) {
        if (chance < 0) return false;
        return rand.nextInt(100) < chance;
    }

    private boolean containsShot(Shots shot) {
        for (int i = 0; i < shotTable.length; i++) {
            if (shotTable[i] != null && shotTable[i].type == shot) return true;
        }
        return false;
    }

    private boolean checkGCD(Shots shot) {
        if (shotData.containsKey(shot)) return shotData.get(shot).checkGcd;
        return true;
    }

    private Shots compareDamage(Shots shot1, Shots shot2, float time) {
        // This function determines whether waiting for {shot1} for {time} seconds produces more dps than using {shot2} right now

        if (shot1 == Shots.None) return Shots.None;
        if (shot2 == Shots.None) return Shots.None;
        if (time <= 0) return Shots.None;

        float shot1damage = shotData.containsKey(shot1) ? shotData.get(shot1).damage : 0;
        float shot2damage = shotData.containsKey(shot2) ? shotData.get(shot2).damage : 0;

        float frac = (float) HunterFormat.roundHalfEven(time / 1.5f, 3);

        if (shot1damage - shot2damage > shot2damage * frac) return shot1;
        return shot2;
    }
}
