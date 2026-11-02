// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/ShotData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.models.hunter.HunterEnums.Shots;

public class ShotData {
    public static final float GCD = 1.5f;

    public Shots type = Shots.None;
    public float damage = 0f;
    public float damageNormal = 0f;
    public float damageOnCrit = 0f;
    public float manaCost = 0f;
    public float cd = 0f;
    public float duration = 0f;
    public boolean triggersGCD = false;
    public boolean canCrit = false;
    private float critChance = 0f;

    public float getCritChance() {
        return canCrit ? Math.min(1f, Math.max(0f, this.critChance)) : 0f;
    }

    public void setCritChance(float value) {
        this.critChance = value;
    }

    public CombatTable attackTable;

    public boolean doesNotDoDamage = false;

    public boolean failReasonSteadyBefore = false; // used in tooltip display
    public boolean failReasonSharedCooldownUsed = false; // used in tooltip display
    public boolean failReasonLackTalent = false; // used in tooltip display
    public boolean failReasonRequiresMultiTargs = false; // used in tooltip display
    public boolean failReasonAlreadyUsed = false; // for de-duping the priority list

    public float rotationCooldown = 0f;
    public float timeUsed = 0f;
    public float ratio = 0f;
    public boolean isRefreshed = false;
    public float freq = 0f;
    public float dps = 0f;
    public float mps = 0f;

    // used in the 4 intermediate stages of calculations (start, inbet, lal, final)
    public float startFreq = 0f;
    private float startGcdLeft = 0f;
    private float startGcdNeeded = 0f;
    private float startGcdUsed = 0f;

    public float inbetFreq = 0f;
    private int stingCount = 0;

    public float lalFreq = 0;
    private float lalGcdLeft = 0f;
    private float lalGcdNeeded = 0f;
    private float lalGcdUsed = 0f;

    public float finalFreq = 0f; // the rotation test can set this
    private float finalRatio = 0f;

    public float critsPerSec = 0f;
    public float critsRatio = 0f;
    public float critsComposite = 0f;

    public ShotData(Shots aType, boolean doesntdodamage, boolean aCritProcs, boolean aGcd) {
        doesNotDoDamage = doesntdodamage;
        type = aType;
        canCrit = aCritProcs;
        triggersGCD = aGcd;
    }

    public void initializeTimings(ShotPriority priority) {
        float castLag = priority.calcOpts.getLatency();

        // rotation cooldown
        rotationCooldown = cd;
        if (type == Shots.SerpentSting) {
            rotationCooldown = duration;
        }

        // time used
        switch (type) {
            case None: {
                timeUsed = 0f;
                break;
            }
            case ImmolationTrap: {
                timeUsed = priority.calcOpts.gcdsToLayImmoTrap * GCD;
                break;
            }
            case ExplosiveTrap: {
                timeUsed = priority.calcOpts.gcdsToLayImmoTrap * GCD;
                break;
            }
            case FreezingTrap: {
                timeUsed = priority.calcOpts.gcdsToLayImmoTrap * GCD;
                break;
            }
            case FrostTrap: {
                timeUsed = priority.calcOpts.gcdsToLayImmoTrap * GCD;
                break;
            }
            case Volley: {
                timeUsed = priority.calcOpts.gcdsToVolley * GCD;
                break;
            }
            case SteadyShot: {
                timeUsed = cd < GCD ? GCD : cd;
                break;
            }
            default: {
                timeUsed = triggersGCD ? GCD : 0f;
                break;
            }
        }

        if (type != Shots.None) {
            float lag = triggersGCD ? castLag : 0.001f; // non GCD = 1ms
            timeUsed += lag;
        }
    }

    public void calculateTimings(ShotPriority priority, ShotData prevShot) {
        float castLag = priority.calcOpts.getLatency();

        // Starting Calculations
        startFreq = (rotationCooldown > timeUsed) ? (float) Math.ceil((rotationCooldown - timeUsed) / (GCD + castLag)) * (GCD + castLag) + timeUsed : GCD + castLag;

        startGcdNeeded = (startFreq > 0) ? (timeUsed > startFreq ? 1f : timeUsed / startFreq) : 0f;

        if (priority.chimeraRefreshesViper && type == Shots.ViperSting) startGcdNeeded = 0f;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) startGcdNeeded = 0f;
        if (!priority.useKillShot && type == Shots.KillShot) startGcdNeeded = 0f;

        startGcdLeft = 1f;
        if (prevShot != null) {
            startGcdLeft = prevShot.startGcdLeft - prevShot.startGcdUsed;
        }

        startGcdUsed = 1f - (startGcdLeft - startGcdNeeded);
        if (prevShot != null) {
            startGcdUsed = startGcdLeft > startGcdNeeded ? startGcdNeeded : startGcdLeft;
        }

        // In-Between Calculations
        inbetFreq = (startFreq > 0 && startGcdUsed > 0) ? timeUsed / startGcdUsed : 0;

        if (prevShot != null) {
            if (type == Shots.SerpentSting || type == Shots.ScorpidSting) {
                stingCount = prevShot.stingCount < 2 ? prevShot.stingCount + 1 : prevShot.stingCount;
            } else {
                stingCount = prevShot.stingCount == 2 ? 3 : prevShot.stingCount;
            }
        } else {
            stingCount = (type == Shots.SerpentSting || type == Shots.ScorpidSting) ? 1 : 0;
        }

        // Lock-and-Load Calculations
        lalFreq = inbetFreq;
        if (type == Shots.ExplosiveShot && priority.lalExplosiveFrequency > 0f) lalFreq = priority.lalExplosiveFrequency;
        if (type == Shots.ArcaneShot && priority.lalArcaneFrequency > 0f) lalFreq = priority.lalArcaneFrequency;
        if (startFreq == 0f || stingCount == 2) lalFreq = 0f;

        lalGcdLeft = 1;
        if (prevShot != null) {
            lalGcdLeft = prevShot.lalGcdLeft - prevShot.lalGcdUsed;
        }

        lalGcdNeeded = lalFreq > 0f ? (timeUsed > lalFreq ? 1f : timeUsed / lalFreq) : 0f;
        if (!priority.useKillShot && type == Shots.KillShot) lalGcdNeeded = 0f;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) lalGcdNeeded = 0f;

        lalGcdUsed = 1f - (lalGcdLeft - lalGcdNeeded);
        if (prevShot != null) {
            lalGcdUsed = lalGcdLeft > lalGcdNeeded ? lalGcdNeeded : lalGcdLeft;
        }

        // RotationTest may come in later and override this value,
        // hence the split-function that we call here
        finalFreq = (lalFreq > 0 && lalGcdUsed > 0) ? timeUsed / lalGcdUsed : 0;

        finishCalculateTimings(priority);
    }

    public void finishCalculateTimings(ShotPriority priority) {
        finalRatio = finalFreq > 0f ? (timeUsed > finalFreq ? 1f : timeUsed / finalFreq) : 0f;
        if (!priority.useKillShot && type == Shots.KillShot) finalRatio = 0;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) finalRatio = 0f;

        // Output Calculations
        isRefreshed = false;
        ratio = 0;
        freq = 0;

        if (priority.chimeraRefreshesViper && type == Shots.ViperSting) isRefreshed = true;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) isRefreshed = true;

        if (!isRefreshed) {
            ratio = finalRatio;
            freq = finalFreq;
        }

        critsPerSec = (canCrit && finalFreq > 0f) ? 1f / finalFreq : 0f;
        critsRatio = (canCrit) ? ratio : 0f;
    }

    public void setFrequency(ShotPriority priority, float newFreq) {
        // called by the RotationTest after it calculates real frequencies
        startFreq = newFreq;
        finalFreq = newFreq;
        finishCalculateTimings(priority);
    }

    public void calculateComposites(ShotPriority priority) {
        if (critsRatio == 0) {
            critsComposite = 0;
            return;
        }
        critsComposite = critsRatio > 0 ? getCritChance() * (critsRatio / priority.critsRatioSum) : 0;
    }

    public void calculateMPS(ShotPriority priority) {
        float inbetMps = inbetFreq > 0 ? manaCost / inbetFreq : 0;

        float finalMps = finalFreq > 0 ? manaCost / finalFreq : 0;

        if (priority.chimeraRefreshesViper && type == Shots.ViperSting) finalMps = 0;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) finalMps = 0;

        if (!priority.calcOpts.useRotationTest && (type == Shots.ExplosiveShot || type == Shots.ArcaneShot)) {
            mps = inbetMps;
        } else {
            mps = isRefreshed ? 0 : finalMps;
        }
    }

    public void calculateDPS(ShotPriority priority) {
        // In-Between Calculations
        float inbetDps = inbetFreq > 0 ? damage / inbetFreq : 0;

        if (priority.chimeraRefreshesViper && type == Shots.ViperSting) inbetDps = damage / startFreq;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) inbetDps = damage / startFreq;

        // Final/Actual Calculations
        float finalDps = finalFreq > 0 ? damage / finalFreq : 0;
        if (priority.chimeraRefreshesViper && type == Shots.ViperSting) finalDps = damage / rotationCooldown;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) finalDps = damage / rotationCooldown;

        // Output Calculations
        dps = finalDps;
        if (priority.chimeraRefreshesSerpent && type == Shots.SerpentSting) {
            // immune to AspectOfViper
        } else {
            dps *= (1f - priority.viperDamagePenalty);
        }
        dps *= (1f - priority.noManaPenalty);
    }

    public String genTooltip() {
        String ret = "";
        if (!doesNotDoDamage) {
            ret += HunterFormat.zeroPad0(damage, 5) + " : " + HunterFormat.zeroPadFixed(dps, 4, 2) + " : ";
        }
        ret += HunterFormat.zeroPadFixed(freq, 2, 2) + "*";

        if (!doesNotDoDamage) {
            ret += "Damage: " + rawr.base.util.NetFormat.fixed(damage, 2) + "\n";
        }
        ret += "Mana: " + rawr.base.util.NetFormat.fixed(manaCost, 2) + "\n";
        ret += "Cooldown: " + rawr.base.util.NetFormat.fixed(cd, 2) + "\n";
        if (duration > 0) {
            ret += "Duration: " + rawr.base.util.NetFormat.fixed(duration, 2) + "\n";
        }
        if (!doesNotDoDamage && canCrit) {
            ret += "Crit Chance: " + HunterFormat.p2(getCritChance()) + "\n";
        }

        if (freq > 0) {
            ret += "Rotation Freqency: " + rawr.base.util.NetFormat.fixed(freq, 2) + "\n";
            if (!doesNotDoDamage) {
                ret += "Rotation DPS: " + rawr.base.util.NetFormat.fixed(dps, 2) + "\n";
            }
            ret += "Rotation MPS: " + rawr.base.util.NetFormat.fixed(mps, 2);
        } else if (isRefreshed) {
            ret += "Rotation Freqency: " + rawr.base.util.NetFormat.fixed(freq, 2) + " (Refreshed by Chimera)\n";
            if (!doesNotDoDamage) {
                ret += "Rotation DPS: " + rawr.base.util.NetFormat.fixed(dps, 2) + "\n";
            }
            ret += "Rotation MPS: " + rawr.base.util.NetFormat.fixed(mps, 2) + " (Refreshed by Chimera)";
        } else {
            ret += failReasonSharedCooldownUsed ? "Not being used in rotation:\n- Shares a cooldown with a higher priority shot"
                    : failReasonLackTalent ? "Not being used in rotation:\n- You lack the needed talent"
                    : failReasonSteadyBefore ? "Not being used in rotation:\n- Steady shot has a higher priority"
                    : failReasonRequiresMultiTargs ? "Not being used in rotation:\n- Multiple Targets isn't set"
                    : "(Not in rotation)";
        }

        return ret;
    }
}
