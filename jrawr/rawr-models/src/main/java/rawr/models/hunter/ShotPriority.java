// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/ShotPriority.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.base.talents.HunterTalents;
import rawr.models.hunter.HunterEnums.Shots;

public class ShotPriority {
    public ShotData[] priorities = new ShotData[10];

    public CalculationOptionsHunter calcOpts;
    public rawr.base.bosses.BossOptions bossOpts;

    public boolean chimeraRefreshesSerpent = false;
    public boolean chimeraRefreshesViper = false;
    public float lalExplosiveFrequency = 0f;
    public float lalArcaneFrequency = 0f;
    public boolean useKillShot = false; // TODO
    public float viperDamagePenalty = 0f;
    public float noManaPenalty = 0f;

    public float specialShotsPerSecond = 0f;
    public float critSpecialShotsPerSecond = 0f;

    public float critsRatioSum = 0f;
    public float critsCompositeSum = 0f;

    public float dps = 0f;
    public float mps = 0f;

    public ShotPriority(CalculationOptionsHunter options) {
        this.calcOpts = options;
    }

    public void validateShots(HunterTalents talents) {
        // check each shot in the priority is allowed to be there.
        // this means removing dupes and anything after steady shot.
        // it also means removing shots which have already had their cooldowns used

        boolean removeAll = false;
        boolean used_arcane_explosive = false;
        boolean used_aimed_multi = false;
        boolean used_black_immo = false;

        for (int i = 0; i < priorities.length; i++) {
            // we've already seen steadyShot - remove everything else
            if (priorities[i] != null && removeAll) {
                priorities[i].failReasonSteadyBefore = true;
                priorities[i] = null;
            }

            // this shot has been used aleady
            if (priorities[i] != null && priorities[i].failReasonAlreadyUsed) priorities[i] = null;

            // we've already used a shot which shares cooldown with this one
            if (priorities[i] != null && (priorities[i].type == Shots.ArcaneShot && used_arcane_explosive)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.ExplosiveShot && used_arcane_explosive)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.AimedShot && used_aimed_multi)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.MultiShot && used_aimed_multi)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.BlackArrow && used_black_immo)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.ImmolationTrap && used_black_immo)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.ExplosiveTrap && used_black_immo)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.FreezingTrap && used_black_immo)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && (priorities[i].type == Shots.FrostTrap && used_black_immo)) {
                priorities[i].failReasonSharedCooldownUsed = true;
                priorities[i] = null;
            }

            // Requires Multiple Targets
            if (priorities[i] != null && (priorities[i].type == Shots.Volley && (!calcOpts.multipleTargets || (calcOpts.multipleTargets && calcOpts.multipleTargetsPerc == 0)))) {
                priorities[i].failReasonRequiresMultiTargs = true;
                priorities[i] = null;
            }

            // shots which require talents
            if (priorities[i] != null && priorities[i].type == Shots.BlackArrow && talents.getBlackArrow() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && priorities[i].type == Shots.ChimearaShot && talents.getChimeraShot() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && priorities[i].type == Shots.AimedShot && talents.getAimedShot() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && priorities[i].type == Shots.ExplosiveShot && talents.getExplosiveShot() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && priorities[i].type == Shots.BestialWrath && talents.getBestialWrath() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }
            if (priorities[i] != null && priorities[i].type == Shots.SilencingShot && talents.getSilencingShot() == 0) {
                priorities[i].failReasonLackTalent = true;
                priorities[i] = null;
            }

            if (priorities[i] != null) {
                priorities[i].failReasonAlreadyUsed = true;
                if (priorities[i].type == Shots.SteadyShot) removeAll = true;

                if (priorities[i].type == Shots.ArcaneShot) used_arcane_explosive = true;
                if (priorities[i].type == Shots.ExplosiveShot) used_arcane_explosive = true;
                if (priorities[i].type == Shots.AimedShot) used_aimed_multi = true;
                if (priorities[i].type == Shots.MultiShot) used_aimed_multi = true;
                if (priorities[i].type == Shots.BlackArrow) used_black_immo = true;
                if (priorities[i].type == Shots.ImmolationTrap) used_black_immo = true;
                if (priorities[i].type == Shots.ExplosiveTrap) used_black_immo = true;
                if (priorities[i].type == Shots.FreezingTrap) used_black_immo = true;
                if (priorities[i].type == Shots.FrostTrap) used_black_immo = true;
            }
        }

        // store some derived facts about the rotation for later use
        chimeraRefreshesSerpent = false;
        chimeraRefreshesViper = false;
        if (containsShot(Shots.ChimearaShot)) {
            if (containsShot(Shots.SerpentSting)) chimeraRefreshesSerpent = true;
            if (containsShot(Shots.ViperSting)) chimeraRefreshesViper = true;
        }
    }

    public void initializeTimings() {
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] != null) priorities[i].initializeTimings(this);
        }
    }

    public boolean containsShot(Shots aType) {
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] != null) {
                if (priorities[i].type == aType) return true;
            }
        }

        return false;
    }

    public ShotData getShotInRotation(Shots aType) {
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] != null) {
                if (priorities[i].type == aType) return priorities[i];
            }
        }

        return null;
    }

    public void calculateFrequencies() {
        // This function calculates the frequencies for each shot.
        // We have already calculated all shot cooldowns and validated the rotation by this point.

        // THIS FUNCTION IS CALLED THREE TIMES - TRY TO REMEMBER THIS!
        // 1) When we have mana and basic timings
        // 2) When we have LAL proc info
        // 3) When we have steady shot speed

        ShotData prevShot = null;

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] == null) continue;
            ShotData s = priorities[i];
            s.calculateTimings(this, prevShot);

            prevShot = s;
        }
    }

    public void recalculateRatios() {
        // this function is called when we're using the rotation test.
        // we call it once we know the cast time for steady shot, to
        // get the correct ratio numbers.

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] == null) continue;
            priorities[i].finishCalculateTimings(this);
        }
    }

    public void calculateFrequencySums() {
        specialShotsPerSecond = 0f;
        critSpecialShotsPerSecond = 0f;
        critsRatioSum = 0f;

        // 31-10-2009 Drizz: Updated to follow 92b
        // I still don't understand the method of how critSpecialShotsPerSecond is calculated.

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] == null) continue;
            ShotData s = priorities[i];

            if (s.finalFreq > 0) {
                specialShotsPerSecond += 1f / s.finalFreq;

                if (s.type == Shots.SerpentSting) {
                    critSpecialShotsPerSecond += 1f / (s.finalFreq / s.rotationCooldown * 3f);
                } else {
                    critSpecialShotsPerSecond += 1f / (s.finalFreq);
                }
            }
            critsRatioSum += s.critsRatio;
        }
    }

    public void calculateCrits() {
        // called after we find out shot crit rates.
        // we calculated frequencies and ratios in earlier calls.

        critsCompositeSum = 0f;

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] == null) continue;
            ShotData s = priorities[i];

            s.calculateComposites(this);
            critsCompositeSum += s.critsComposite;
        }
    }

    public void calculateLALProcs(Character character) {
        float lal_trigger_freq = 0f;
        float lal_trigger_duration = 0f;

        ShotData proc_shot = null;
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.BlackArrow);
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.ImmolationTrap);
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.ExplosiveTrap);
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.FreezingTrap);
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.FrostTrap);
        if (proc_shot == null) proc_shot = getShotInRotation(Shots.Volley);

        if (proc_shot != null) {
            lal_trigger_freq = proc_shot.lalFreq;
            lal_trigger_duration = proc_shot.duration;
        }

        float lal_proc_chance = 0.02f * character.getHunterTalents().getLockAndLoad();
        float lal_proc_freq = 0f;

        if (calcOpts.useRotationTest) {
            // the spreadsheet gets the LAL proc frequency from the rotation test,
            // but it doesn't really matter because it's only used to calculate some
            // stats we'll throw away (final_freq via lal_freq).
        } else {
            if (lal_trigger_freq > 0 && lal_proc_chance > 0) {
                lal_proc_freq = (lal_trigger_duration > lal_trigger_freq ? 1f : lal_trigger_freq / lal_trigger_duration) * 3f / lal_proc_chance;
            }
        }

        ShotData lalExplosive = getShotInRotation(Shots.ExplosiveShot);
        ShotData lalArcane = getShotInRotation(Shots.ArcaneShot);

        float pre_lal_explosive_freq = (lalExplosive != null && calcOpts.lalShotToUse == Shots.ExplosiveShot) ? lalExplosive.inbetFreq : 0;
        float pre_lal_arcane_freq = (lalArcane != null && calcOpts.lalShotToUse == Shots.ArcaneShot) ? lalArcane.inbetFreq : 0;

        lalExplosiveFrequency = 0;
        if (lal_proc_freq > 0 && pre_lal_explosive_freq > 0) {
            lalExplosiveFrequency = 1f / (1f / (lal_proc_freq / (pre_lal_explosive_freq / (pre_lal_arcane_freq + pre_lal_explosive_freq) * calcOpts.lalShotsReplaced)) + 1f / pre_lal_explosive_freq);
        }

        lalArcaneFrequency = 0;
        if (lal_proc_freq > 0 && pre_lal_arcane_freq > 0) {
            lalArcaneFrequency = 1f / (1f / (lal_proc_freq / (pre_lal_arcane_freq / (pre_lal_explosive_freq + pre_lal_arcane_freq) * calcOpts.lalShotsReplaced)) + 1f / pre_lal_arcane_freq);
        }
    }

    public void calculateRotationMPS() {
        mps = 0;
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] != null) {
                ShotData s = priorities[i];

                s.calculateMPS(this);

                mps += s.mps;
            }
        }
    }

    public void calculateRotationDPS() {
        dps = 0;

        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i] != null) {
                ShotData s = priorities[i];

                s.calculateDPS(this);

                dps += s.dps;
            }
        }
    }
}
