// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Sequence.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Sequence implements java.util.Comparator<SequenceItem> {
    /** Faithful default: the original caps the group search at 5s. Disable for deterministic runs. */
    public static boolean timeLimitEnabled = true;

    List<SequenceItem> sequence = new ArrayList<SequenceItem>();

    int ColumnManaGem;
    int ColumnManaPotion;

    public void add(SequenceItem item) {
        sequence.add(item);
    }

    List<SequenceGroup> superGroup = new ArrayList<SequenceGroup>();

    private void calculateSuperGroups() {
        superGroup.clear();
        SequenceGroup group = null;
        List<SequenceGroup> lastGroup = null;
        for (int i = 0; i < sequence.size(); i++) {
            if (lastGroup == null || (!sequence.get(i).isManaPotionOrGem() && ListUtils.intersect(lastGroup, sequence.get(i).group).size() == 0)) {
                group = new SequenceGroup();
                superGroup.add(group);
            }
            if (!sequence.get(i).isManaPotionOrGem()) lastGroup = sequence.get(i).group;
            group.add(sequence.get(i));
            sequence.get(i).superGroup = group;
        }
    }

    private boolean sortByMps = true;
    private boolean preserveCooldowns = true;
    private double sortMaxMps = Double.POSITIVE_INFINITY;
    private double sortMinMps = Double.NEGATIVE_INFINITY;
    private double sortStartTime;
    private double sortTargetTime;

    public void computeTimestamps() {
        double t = 0;
        for (int i = 0; i < sequence.size(); i++) {
            double d = sequence.get(i).duration;
            if (sequence.get(i).isManaPotionOrGem()) d = 0;
            sequence.get(i).timestamp = t;
            t += d;
        }
    }

    private double minTime(SequenceGroup superGroup, int placedUpTo) {
        final double eps = 0.000001;
        double minTime = superGroup.getMinTime();
        for (SequenceGroup group : getAllGroups(superGroup.item)) {
            double diff = group.getMinTime() - superGroup.getMinTime();
            for (CooldownConstraint constraint : group.constraint) {
                for (int j = 0; j <= placedUpTo; j++) {
                    if (sequence.get(j).group.contains(constraint.group)) {
                        if (!constraint.coldSnap || (group.getMinTime() - sequence.get(j).getMinTime() >= SequenceItem.calculations.icyVeinsCooldown - eps)) // make sure to ignore coldsnapped constraints
                        {
                            minTime = Math.max(minTime, sequence.get(j).timestamp + constraint.cooldown - diff);
                        }
                        break;
                    }
                }
            }
        }
        return minTime;
    }

    private double minTime(int i, int placedUpTo) {
        if (placedUpTo >= i) placedUpTo = i - 1;
        return minTime(sequence.get(i).superGroup, placedUpTo) + sequence.get(i).getMinTime() - sequence.get(i).superGroup.getMinTime();
    }

    public void sortByMps(boolean preserveCooldowns, double minMps, double maxMps, double maxTps, double startTime, double targetTime, double extraMana, double startMana) {
        final double eps = 0.000001;
        if (minMps > maxMps) maxMps = minMps;
        double limitTime = sequence.get(sequence.size() - 1).timestamp + sequence.get(sequence.size() - 1).duration;
        if (targetTime > limitTime) targetTime = limitTime;

        this.sortByMps = true;
        this.preserveCooldowns = preserveCooldowns;
        this.sortMaxMps = maxMps;
        this.sortMinMps = minMps;
        this.sortStartTime = startTime;
        this.sortTargetTime = targetTime;

        calculateSuperGroups();

        double maxMana = maxMps * (targetTime - startTime);
        double minMana = minMps * (targetTime - startTime);
        double mana = 0;
        double threat = 0;

        double t = 0;
        int i;
        SequenceGroup lastGroup = null;
        for (i = 0; i < sequence.size(); i++) {
            double d = sequence.get(i).duration;
            if (sequence.get(i).isManaPotionOrGem()) d = 0;
            if (d > 0 && t + eps >= startTime) {
                if (lastGroup != sequence.get(i).superGroup) break;
                else {
                    threat += sequence.get(i).getTps() * sequence.get(i).duration;
                    mana += sequence.get(i).getMps() * d;
                }
            } else {
                if (t + d > startTime) mana += sequence.get(i).getMps() * (t + d - startTime);
                threat += sequence.get(i).getTps() * sequence.get(i).duration;
                lastGroup = sequence.get(i).superGroup;
            }
            t += d;
        }
        if (extraMana == 0) {
            for (SequenceGroup group : superGroup) {
                //group.sortByMps(minMps, maxMps);
                group.setSuperIndex();
            }
            sequence.subList(i, sequence.size()).sort(this);
            computeTimestamps();
        }
        if (targetTime < t) return; // there is nothing we can do at this point
        double T = t;
        double Mana = mana;
        double Threat = threat;
        retry:
        while (true) {
            int j;
            int lastHigh = i;
            double tLastHigh = t;
            double overflowMana = startMana - Mana; // for overflow calculations assume there are no mana consumables placed after start time yet, if we skipped a super group since startTime then we have to adjust starting mana
            if (overflowMana > getBaseStats().getMana()) {
                overflowMana = getBaseStats().getMana(); // can't have more mana than max
            }
            double overflowLimit = getBaseStats().getMana(); // was maxMana before, but I think when we have splittable group we can insert just enough to not go over
            SequenceGroup lastSuper = null;
            for (j = i; j < sequence.size(); j++) {
                double d = sequence.get(j).duration;
                if (sequence.get(j).isManaPotionOrGem()) d = 0;
                double overflowBuffer = 0.0;
                double threatBuffer = 0.0;
                if (lastSuper != sequence.get(j).superGroup) {
                    overflowLimit = getBaseStats().getMana() - overflowMana;
                    lastSuper = sequence.get(j).superGroup;
                    // analize if the group overflows
                    double projectMana = overflowMana;
                    double projectThreat = threat;
                    double projectTime = t;
                    for (int k = 0; k < sequence.get(j).superGroup.item.size(); k++) {
                        SequenceItem kitem = sequence.get(j).superGroup.item.get(k);
                        double kd = kitem.duration;
                        if (kitem.getCastingState().manaGemEffect && (k == 0 || (!sequence.get(j).superGroup.item.get(k - 1).getCastingState().manaGemEffect && sequence.get(j).superGroup.item.get(k - 1).getVariableType() != VariableType.ManaGem && sequence.get(j).superGroup.item.get(k - 1).getVariableType() != VariableType.ManaPotion))) {
                            projectMana += SequenceItem.calculations.manaGemValue * (1 + getBaseStats().getBonusManaGem());
                            if (projectMana - getBaseStats().getMana() > overflowBuffer) {
                                sequence.get(j).superGroup.unavailableForMinManaCorrections = true;
                                overflowBuffer = projectMana - getBaseStats().getMana();
                            }
                        }
                        if (projectTime + kd > targetTime) {
                            projectMana -= kitem.getMps() * (targetTime - projectTime);
                            projectThreat += kitem.getTps() * (targetTime - projectTime);
                            projectTime = targetTime;
                            break;
                        } else {
                            projectMana -= kitem.getMps() * kd;
                            projectThreat += kitem.getTps() * kd;
                            projectTime += kd;
                        }
                        // don't care if idle regen is placed at start if we have threat limitations
                        // don't mark as problem if we were overflowed already, can't fix that
                        if (!(maxTps < 5000.0 && kitem.getVariableType() == VariableType.IdleRegen && kitem.getSegment() == 0) && (projectMana - getBaseStats().getMana() > overflowBuffer) && (overflowMana <= overflowBuffer + eps)) {
                            sequence.get(j).superGroup.unavailableForMinManaCorrections = true;
                            overflowBuffer = projectMana - getBaseStats().getMana();
                        }
                        // only buffer items that are above tps limit; if we're above limit from before start time
                        // then this would try buffering items that were used to buffer other items, it gets ugly
                        if (kitem.getTps() > maxTps && projectThreat - maxTps * projectTime > threatBuffer) {
                            threatBuffer = projectThreat - maxTps * projectTime;
                        }
                    }
                }
                double minTimeJ = minTime(j, j - 1);
                if (t < minTimeJ - eps || overflowBuffer > eps || threatBuffer > eps) {
                    // sequence positioned too early, we have to buffer up with something that can
                    // be positioned at t and is either small enough not to disrupt max time
                    // or is splittable
                    boolean updated = false;
                    double minbuffer = minTimeJ - t;
                    double buffer = sequence.get(j).getMaxTime() - t;
                    int k;
                    for (k = j + 1; k < sequence.size() && (minbuffer > eps || overflowBuffer > eps || threatBuffer > eps) && buffer > eps; k++) {
                        if (sequence.get(k).superGroup != lastSuper) // intra super ordering not allowed
                        {
                            if (minTime(k, j - 1) <= t + eps) {
                                if (sequence.get(k).group.size() == 0 && (minbuffer > eps || (overflowBuffer > eps && sequence.get(k).getMps() > 0 && !sequence.get(k).getCastingState().manaGemEffect) || (threatBuffer > eps && sequence.get(k).getTps() < maxTps))) {
                                    // if we're not fixing cooldown constraints then make sure not to break threat limits by moving stuff around
                                    if (minbuffer < eps && sequence.get(k).getTps() > maxTps) {
                                        // threat + duration * tps <= maxTps * t + maxTps * duration
                                        // duration <= (maxTps * t - threat) / (tps - maxTps)
                                        double maxSplit = (maxTps * t - threat) / (sequence.get(k).getTps() - maxTps);
                                        if (maxSplit < eps) {
                                            continue;
                                        }
                                        if (maxSplit < buffer) {
                                            buffer = maxSplit;
                                        }
                                    }
                                    if (minbuffer > eps && sequence.get(k).duration > minbuffer + eps) {
                                        splitAt(k, minbuffer);
                                    } else if (overflowBuffer > eps && sequence.get(k).duration > overflowBuffer / sequence.get(k).getMps()) {
                                        splitAt(k, Math.min(buffer, overflowBuffer / sequence.get(k).getMps()));
                                    } else if (threatBuffer > eps && (maxTps - sequence.get(k).getTps()) * sequence.get(k).duration > threatBuffer) {
                                        splitAt(k, Math.min(buffer, threatBuffer / (maxTps - sequence.get(k).getTps())));
                                    } else if (sequence.get(k).duration > buffer + eps) {
                                        splitAt(k, buffer);
                                    }
                                    SequenceItem copy = sequence.get(k);
                                    sequence.remove(k);
                                    sequence.add(j, copy);
                                    computeTimestamps();
                                    minbuffer -= copy.duration;
                                    buffer -= copy.duration;
                                    if (overflowBuffer > eps) overflowBuffer -= copy.getMps() * copy.duration;
                                    if (threatBuffer > eps) threatBuffer -= (maxTps - copy.getTps()) * copy.duration;
                                    t += copy.duration;
                                    updated = true;
                                    j++;
                                    k = j;
                                } else if (sequence.get(k).superGroup.duration <= buffer && (minbuffer > eps || (overflowBuffer > eps && sequence.get(k).superGroup.getMps() > 0 && !sequence.get(k).superGroup.getManaGemActivation()) || (threatBuffer > eps && sequence.get(k).superGroup.getTps() < maxTps))) {
                                    int l;
                                    for (l = k + 1; l < sequence.size(); l++) {
                                        if (sequence.get(l).superGroup != sequence.get(k).superGroup) break;
                                    }
                                    List<SequenceItem> copy = new ArrayList<SequenceItem>(sequence.subList(k, l));
                                    sequence.subList(k, l).clear();
                                    sequence.addAll(j, copy);
                                    computeTimestamps();
                                    minbuffer -= copy.get(0).superGroup.duration;
                                    buffer -= copy.get(0).superGroup.duration;
                                    if (overflowBuffer > eps) overflowBuffer -= copy.get(0).superGroup.getMps() * copy.get(0).superGroup.duration;
                                    if (threatBuffer > eps) threatBuffer -= (maxTps - copy.get(0).superGroup.getTps()) * copy.get(0).superGroup.duration;
                                    t += copy.get(0).superGroup.duration;
                                    updated = true;
                                    j += copy.size();
                                    k = j;
                                }
                            }
                        }
                    }
                    if (updated) {
                        t = T;
                        mana = Mana;
                        threat = Threat;
                        continue retry;
                    }
                }
                if (d > 0 && sequence.get(j).superGroup.getMps() > maxMps) {
                    lastHigh = j;
                    tLastHigh = t;
                }
                if (t + d > targetTime) {
                    mana += sequence.get(j).getMps() * (targetTime - t);
                    threat += sequence.get(j).getTps() * (targetTime - t);
                    overflowMana -= sequence.get(j).getMps() * (targetTime - t);
                    break;
                } else {
                    mana += sequence.get(j).getMps() * d;
                    threat += sequence.get(j).getMps() * d;
                    overflowMana -= sequence.get(j).getMps() * d;
                    if (maxTps < 5000.0 && overflowMana > getBaseStats().getMana()) overflowMana = getBaseStats().getMana();
                    t += d;
                }
            }
            // verify max time constraints
            double tt = T;
            int a;
            lastSuper = null;
            for (a = i; a < sequence.size(); a++) {
                double d = sequence.get(a).duration;
                if (sequence.get(a).isManaPotionOrGem()) d = 0;
                if (lastSuper != sequence.get(a).superGroup) {
                    lastSuper = sequence.get(a).superGroup;
                    if (tt > lastSuper.getMaxTime() + eps && tt > T && tt > minTime(lastSuper, a - 1) + eps) // there might be other cases where it is impossible to move back without breaking others, double check for infinite cycles
                    {
                        // compute buffer of items that can be moved way back
                        double buffer = 0;
                        int b;
                        for (b = i; b < a; b++) {
                            if (sequence.get(b).getMaxTime() >= tt + lastSuper.duration) buffer += sequence.get(b).duration;
                        }
                        // place it at max time, but move back over non-splittable super groups
                        // if move breaks constraint on some other group cancel
                        double t3 = tt;
                        boolean updated = false;
                        for (b = a - 1; b >= i; b--) {
                            t3 -= sequence.get(b).duration;
                            if (t3 <= lastSuper.getMaxTime() + eps) {
                                // possible insert point
                                if (sequence.get(b).group.size() == 0) {
                                    if (sequence.get(b).getMaxTime() >= lastSuper.getMaxTime() + lastSuper.duration - buffer) {
                                        // splittable, make a split at max time
                                        if (lastSuper.getMaxTime() > t3 + eps) {
                                            splitAt(b, lastSuper.getMaxTime() - t3);
                                            a++;
                                            b++;
                                        }
                                        sequence.addAll(b, removeSuperGroup(a));
                                        computeTimestamps();
                                        updated = true;
                                    }
                                } else {
                                    // we are in super group, move to start
                                    SequenceGroup sg = sequence.get(b).superGroup;
                                    while (b >= i && sequence.get(b).superGroup == sg) {
                                        b--;
                                        if (b >= 0) t3 -= sequence.get(b).duration;
                                    }
                                    if (b >= 0) t3 += sequence.get(b).duration;
                                    b++;
                                    if (sg.getMaxTime() >= t3 + lastSuper.duration - buffer) {
                                        sequence.addAll(b, removeSuperGroup(a));
                                        computeTimestamps();
                                        updated = true;
                                    }
                                }
                                break;
                            } else {
                                // make sure we wouldn't push it out of max
                                if (sequence.get(b).getMaxTime() < t3 + lastSuper.duration - buffer) {
                                    break;
                                }
                            }
                        }
                        if (updated) {
                            t = T;
                            mana = Mana;
                            threat = Threat;
                            continue retry;
                        }
                    }
                }
                tt += d;
            }
            boolean extraMode = extraMana > 0;
            if (mana > maxMana + eps || extraMana > 0) {
                // [i....j]XXX[k....]
                int k;
                // [i..|jj..j]XXX[k..|kk.]
                int jj = j; double jT = targetTime - t;
                double tjj = t;
                double tkk = tLastHigh;
                if (jT <= 0.00001) {
                    jj--;
                    if (jj >= 0 && jj < sequence.size()) {
                        if (jj < sequence.size() - 1 && sequence.get(jj).superGroup != sequence.get(jj + 1).superGroup) overflowLimit -= sequence.get(jj).superGroup.mana;
                        jT += sequence.get(jj).duration;
                        tjj -= sequence.get(jj).duration;
                    }
                }
                double maxPush = Double.POSITIVE_INFINITY;
                if (lastHigh <= jj && jj < sequence.size() - 1) // it only makes sense if we move from after target time and move in front
                {
                    if (sequence.get(jj).group.size() == 0) lastHigh = jj;
                    else lastHigh = jj + 1;
                }
                // if jj > lastHigh then jj actually won't be pushed back as the swap will occur before it (it might actually be moving toward start)
                if (jj < sequence.size() && jj <= lastHigh) maxPush = sequence.get(jj).getMaxTime() - tjj; // you can assume jj won't be split
                for (k = lastHigh; k < sequence.size(); k++) {
                    // make sure item is low mps and can be moved back
                    // the only thing really required to have mana drop is for mps to be lower than at jj
                    if (sequence.get(k).superGroup != sequence.get(jj).superGroup && sequence.get(k).superGroup.getMps() < sequence.get(jj).getMps() - eps && (minTime(k, j) + eps < sequence.get(k).timestamp)) break;
                    // everything we skip will have to be pushed so make sure there is space
                    maxPush = Math.min(maxPush, sequence.get(k).getMaxTime() - tkk);
                    tkk += sequence.get(k).duration;
                }
                if (k < sequence.size() && maxPush > eps) {
                    int kk = k; double kT = 0;
                    double currentPush = 0;
                    do {
                        double nextT = Math.min(jT, sequence.get(kk).duration - kT);
                        double mpsdiff = sequence.get(jj).getMps() - sequence.get(kk).getMps();
                        if (mpsdiff < -eps) {
                            break; // if we go on we'll actually make it worse, so stop here and hope that we can do some swapping
                        }
                        if (mana - mpsdiff * nextT <= minMana) {
                            nextT = (mana - minMana) / mpsdiff;
                        }
                        if (sequence.get(jj).group.size() > 0) {
                            if (overflowLimit + sequence.get(kk).getMps() * nextT < 0) {
                                nextT = -overflowLimit / sequence.get(kk).getMps();
                            }
                        } else {
                            if (mpsdiff > 0 && overflowLimit + sequence.get(jj).getMps() * jT - mpsdiff * nextT < 0) {
                                nextT = (overflowLimit + sequence.get(jj).getMps() * jT) / mpsdiff;
                            }
                        }
                        if (currentPush + nextT > maxPush) {
                            nextT = maxPush - currentPush;
                        }
                        mana -= mpsdiff * nextT;
                        overflowLimit += sequence.get(kk).getMps() * nextT;
                        currentPush += nextT;
                        if (extraMode) extraMana += sequence.get(kk).getMps() * nextT;
                        jT -= nextT;
                        if (jT <= 0) {
                            SequenceGroup currentSuper = sequence.get(jj).superGroup;
                            jj--;
                            if (jj >= 0) {
                                jT += sequence.get(jj).duration;
                                tjj -= sequence.get(jj).duration;
                                maxPush = Math.min(maxPush, sequence.get(jj).getMaxTime() - tjj);
                                if (sequence.get(jj).superGroup != sequence.get(jj + 1).superGroup) overflowLimit -= sequence.get(jj).superGroup.mana;
                                // seems like this never really applies
                                // if we don't have enough mana for long sequence then pushing it out
                                // won't create new opportunities for mana consumables
                                //if (extraMode && sequence[jj].SuperGroup != currentSuper) extraMana = double.NegativeInfinity;
                            } else {
                                if (extraMode) extraMana = Double.NEGATIVE_INFINITY;
                            }
                        }
                        kT += nextT;
                        if (kT >= sequence.get(kk).duration - eps) {
                            kT -= sequence.get(kk).duration;
                            kk++;
                        }
                        if (((mana <= maxMana && (extraMana <= eps || mana <= minMana)) || (sequence.get(jj).group.size() > 0 && overflowLimit <= 0)) && (kk >= sequence.size() || sequence.get(kk).group.size() == 0 || (kT < eps && (kk == 0 || sequence.get(kk).superGroup != sequence.get(kk - 1).superGroup)))) // make sure not to force a split of super group, if you actually have a low mps super then you have to move it as a whole
                        {
                            break;
                        }
                        if (nextT == 0.0) break; // if we get stopped in the middle of super group then abort
                    } while (jj >= i && kk < sequence.size() && minTime(k, jj) <= tjj + jT && minTime(kk, jj) <= tjj + jT + currentPush - kT && currentPush < maxPush);
                    // [i..[k..||jj..j]XXXkk.]
                    if (kk >= sequence.size() || sequence.get(kk).group.size() == 0 || (kT < eps && (kk == 0 || sequence.get(kk).superGroup != sequence.get(kk - 1).superGroup))) // if require super split, then abort, consider restarting at higher lastHigh
                    {
                        if (kT > 0) {
                            splitAt(kk, kT);
                            kk++;
                        }
                        // if k has negative mps, then just placing it at the end won't work
                        // we're breaking max mana constraint, this means we're most likely (but not necessarily) oom
                        // placing -mps at the end won't help us get from negative
                        // we have to place it before it gets to that point (but don't go breaking maxtime constraints)
                        // when we're filling with extra mana this does not apply
                        List<SequenceItem> copy = new ArrayList<SequenceItem>(sequence.subList(k, kk));
                        double totalmana = 0;
                        for (SequenceItem item : copy)
                            totalmana += item.getMps() * item.duration;
                        while (jj >= i && currentPush <= maxPush && !extraMode && totalmana < 0) {
                            if (sequence.get(jj).getMps() * jT > -totalmana) {
                                jT += totalmana / sequence.get(jj).getMps();
                                break;
                            }
                            totalmana += sequence.get(jj).getMps() * jT;
                            jj--;
                            jT = sequence.get(jj).duration;
                            tjj -= sequence.get(jj).duration;
                            maxPush = Math.min(maxPush, sequence.get(jj).getMaxTime() - tjj);
                        }
                        if (jj < 0) jj = 0; // TODO investigate
                        if (jT >= sequence.get(jj).duration) {
                            jT -= sequence.get(jj).duration;
                            jj++;
                        }
                        if (jj >= sequence.size()) return;
                        // don't split into supergroup, make a clean cut
                        if (sequence.get(jj).group.size() > 0 && currentPush <= maxPush && (jT > 0 || (jj > 0 && sequence.get(jj).superGroup == sequence.get(jj - 1).superGroup))) // if we're mid super group, and we can push the end we can push the whole super group
                        {
                            // move to start of super group
                            jT = 0;
                            SequenceGroup sg = sequence.get(jj).superGroup;
                            while (jj >= 0 && sequence.get(jj).superGroup == sg) {
                                jj--;
                            }
                            jj++;
                        }
                        // final split and reinsert
                        if (jj >= i) {
                            sequence.subList(k, kk).clear();
                            if (jj < sequence.size() && jT > eps) // only happens in release, wasn't able to track it down
                            {
                                splitAt(jj, jT);
                                jj++;
                                kk++;
                            }
                            if (jj >= sequence.size()) sequence.addAll(copy); // only happens in release, wasn't able to track it down
                            else sequence.addAll(jj, copy);
                        }
                        computeTimestamps();
                    }
                }
            } else if (mana < minMana) {
                // no high burn sequence is available yet
                // take first super group with enough burn and place it as soon as possible
                // TODO: this needs a complete redesign, need some logic to ensure the changes made actually result in increase of mana consumption
                tt = T;
                lastSuper = null;
                for (a = i; a < sequence.size(); a++) {
                    double d = sequence.get(a).duration;
                    if (sequence.get(a).isManaPotionOrGem()) d = 0;
                    if (lastSuper != sequence.get(a).superGroup) {
                        lastSuper = sequence.get(a).superGroup;
                        double minLastSuper = minTime(lastSuper, a - 1);
                        if (!lastSuper.unavailableForMinManaCorrections && tt + lastSuper.duration > targetTime && lastSuper.getMps() > minMps && tt > T && tt > minLastSuper + eps && minLastSuper < targetTime - eps) {
                            // compute buffer of items that can be moved way back
                            double buffer = 0;
                            int b;
                            for (b = i; b < a; b++) {
                                if (sequence.get(b).getMaxTime() >= tt + lastSuper.duration) buffer += sequence.get(b).duration;
                            }
                            // place it at min time, but move forward over non-splittable super groups
                            // if move breaks constraint on some other group cancel
                            int lastSafeInsert = a;
                            double t3 = tt;
                            boolean updated = false;
                            boolean manaTest = false;
                            for (b = a - 1; b >= i; b--) {
                                if (b == 0 || sequence.get(b).superGroup != sequence.get(b - 1).superGroup) lastSafeInsert = b;
                                if (lastSuper.getMps() > sequence.get(b).superGroup.getMps()) manaTest = true;
                                t3 -= sequence.get(b).duration;
                                if (t3 <= minLastSuper + eps) {
                                    if (!manaTest) break;
                                    // possible insert point
                                    if (sequence.get(b).group.size() == 0) {
                                        if (sequence.get(b).getMaxTime() >= minLastSuper + lastSuper.duration - buffer) {
                                            // splittable, make a split at max time
                                            if (minTime(lastSuper, a - 1) > t3 + eps) {
                                                splitAt(b, minLastSuper - t3);
                                                a++;
                                                b++;
                                            }
                                            sequence.addAll(b, removeSuperGroup(a));
                                            computeTimestamps();
                                            updated = true;
                                        }
                                    } else {
                                        // we are in super group, use last safe insert
                                        if (lastSafeInsert < a) {
                                            sequence.addAll(lastSafeInsert, removeSuperGroup(a));
                                            computeTimestamps();
                                            updated = true;
                                        }
                                    }
                                    break;
                                } else {
                                    // make sure we wouldn't push it out of max
                                    if (sequence.get(b).getMaxTime() < t3 + lastSuper.duration - buffer) {
                                        break;
                                    }
                                }
                            }
                            if (updated) {
                                t = T;
                                mana = Mana;
                                threat = Threat;
                                continue retry;
                            }
                        }
                    }
                    tt += d;
                }
            }
            break;
        }
    }

    private static int compareDotNet(double x, double y) {
        if (x < y) return -1;
        if (x > y) return 1;
        if (x == y) return 0;
        if (Double.isNaN(x)) return Double.isNaN(y) ? 0 : -1;
        return 1;
    }

    @Override
    public int compare(SequenceItem x, SequenceItem y) {
        if (sortByMps) {
            if (preserveCooldowns) {
                if (x.superGroup == y.superGroup) {
                    return Integer.compare(x.superIndex, y.superIndex);
                } else {
                    int compare = Integer.compare(x.superGroup.getSegment(), y.superGroup.getSegment());
                    if (compare != 0) return compare;
                    boolean xcritical = x.superGroup.getMaxTime() <= sortStartTime;
                    boolean ycritical = y.superGroup.getMaxTime() <= sortStartTime;
                    compare = Boolean.compare(ycritical, xcritical);
                    if (compare != 0) return compare;
                    boolean xsplit = x.group.size() == 0;
                    boolean ysplit = y.group.size() == 0;
                    compare = Boolean.compare(xsplit, ysplit);
                    if (compare != 0) return compare;
                    if (x.group.size() == 0 && y.group.size() == 0) {
                        // only sort by mps for normal casting without cooldowns
                        // if both are in super groups we have to sort in the way as prescribed by SortGroups solution
                        // only sort by mps if both are splittable (otherwise this would not necessarily be an ordering)
                        compare = compareByMps(x.superGroup, y.superGroup);
                        if (compare != 0) return compare;
                    }
                    // if two super groups have same mps make sure to group by super group
                    return compareDotNet(x.superGroup.getMinTime(), y.superGroup.getMinTime()); // is min time unique???
                }
            } else {
                return compareByMps(x, y);
            }
        }
        return 0;
    }

    private int compareByMps(SequenceItem x, SequenceItem y) {
        return compareMps(x.getMps(), y.getMps(), sortMinMps, sortMaxMps);
    }

    private int compareByMps(SequenceGroup x, SequenceGroup y) {
        return compareMps(x.getMps(), y.getMps(), sortMinMps, sortMaxMps);
    }

    public static int compareMps(double x, double y, double minMps, double maxMps) {
        int xrange, yrange;
        if (x < maxMps && x >= minMps) xrange = 0;
        else if (x >= maxMps) xrange = 1;
        else xrange = 2;
        if (y < maxMps && y >= minMps) yrange = 0;
        else if (y >= maxMps) yrange = 1;
        else yrange = 2;
        int compare = Integer.compare(xrange, yrange);
        if (compare != 0) return compare;
        if (xrange == 0 || xrange == 2) return compareDotNet(y, x);
        else return compareDotNet(x, y);
    }

    private void splitAt(double time) {
        double t = 0;
        for (int i = 0; i < sequence.size(); i++) {
            double d = sequence.get(i).duration;
            if (sequence.get(i).isManaPotionOrGem()) d = 0;
            if (t + d > time) {
                if (time > t) {
                    splitAt(i, time - t);
                }
                return;
            }
            t += d;
        }
    }

    private List<SequenceItem> removeSuperGroup(int index) {
        int i;
        SequenceGroup superGroup = sequence.get(index).superGroup;
        for (i = index; i < sequence.size(); i++) {
            if (sequence.get(i).superGroup != superGroup) break;
        }
        List<SequenceItem> copy = new ArrayList<SequenceItem>(sequence.subList(index, i));
        sequence.subList(index, i).clear();
        return copy;
    }

    private void splitAt(int index, double time) {
        if (time > 0.00001 && time < sequence.get(index).duration - 0.00001) {
            double d = sequence.get(index).duration;
            sequence.add(index, sequence.get(index).clone());
            sequence.get(index).duration = time;
            sequence.get(index + 1).duration = d - time;
            if (sequence.get(index).group.size() == 0) {
                SequenceGroup newSuperGroup = new SequenceGroup();
                newSuperGroup.add(sequence.get(index));
                sequence.get(index).superGroup = newSuperGroup;
                newSuperGroup = new SequenceGroup();
                newSuperGroup.add(sequence.get(index + 1));
                sequence.get(index + 1).superGroup = newSuperGroup;
                sequence.get(index).setTimeConstraint(sequence.get(index).getMinTime(), sequence.get(index).getMaxTime() + sequence.get(index + 1).duration);
                sequence.get(index + 1).setTimeConstraint(sequence.get(index + 1).getMinTime(), sequence.get(index + 1).getMaxTime() + sequence.get(index).duration);
            } else if (sequence.get(index).superGroup != null) {
                sequence.get(index).superGroup.item.add(sequence.get(index).superGroup.item.indexOf(sequence.get(index + 1)), sequence.get(index));
            }
        }
    }

    private SequenceItem split(SequenceItem item, double time) {
        int index = sequence.indexOf(item);
        splitAt(index, time);
        return sequence.get(index);
    }

    public double removeIndex(int index) {
        return removeIndex(index, 0);
    }

    private double removeIndex(int index, int startAt) {
        double ret = 0;
        for (int i = startAt; i < sequence.size(); i++) {
            if (sequence.get(i).getIndex() == index) {
                ret += sequence.get(i).duration;
                sequence.remove(i);
                i--;
            }
        }
        return ret;
    }

    public double removeIndex(VariableType type) {
        return removeIndex(type, 0);
    }

    private double removeIndex(VariableType type, int startAt) {
        double ret = 0;
        for (int i = startAt; i < sequence.size(); i++) {
            if (sequence.get(i).getVariableType() == type) {
                ret += sequence.get(i).duration;
                sequence.remove(i);
                i--;
            }
        }
        return ret;
    }

    private SequenceItem insertIndex(int index, double duration, double time) {
        SequenceItem item = new SequenceItem(index, duration);
        splitAt(time);
        insertIndex(item, time);
        return item;
    }

    private void insertIndex(SequenceItem item, double time) {
        final double eps = 0.000001;
        double t = 0;
        for (int i = 0; i < sequence.size(); i++) {
            double d = sequence.get(i).duration;
            if (sequence.get(i).isManaPotionOrGem()) d = 0;
            if (t + d > time + eps && time <= t + eps) {
                // splitting should have been done beforehand
                sequence.add(i, item);
                return;
            }
            t += d;
        }
    }

    public enum EvaluationMode {
        Unexplained,
        ManaBelow,
        ManaAtTime,
        CooldownBreak,
    }

    private DisplayCalculations getCalculations() {
        return SequenceItem.calculations;
    }

    private Stats getBaseStats() {
        return SequenceItem.calculations.getBaseStats();
    }

    private double getEvocationRegen() {
        return SequenceItem.calculations.evocationRegen;
    }

    private double getEvocationDuration() {
        return SequenceItem.calculations.evocationDuration;
    }

    private double getFightDuration() {
        return SequenceItem.calculations.getCalculationOptions().getFightDuration();
    }

    private double getManaGemEffectDuration() {
        return SequenceItem.calculations.manaGemEffectDuration;
    }

    public List<SequenceGroup> groupSpecialEffect(EffectCooldown type) {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().effectsActive(type.mask)) list.add(item);
        }
        return groupCooldown(list, type.duration, type.cooldown, type);
    }

    public List<SequenceGroup> groupManaGemEffect() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().manaGemEffect) list.add(item);
        }
        if (list.size() > 0) return groupCooldown(list, getManaGemEffectDuration(), 120f, getCalculations().getEffectCooldown().get(StandardEffect.ManaGemEffect));
        return null;
    }

    public void constrainTrinkets(List<SequenceGroup> t1, List<SequenceGroup> t2) {
        if (t1.size() == 0 || t2.size() == 0) return;
        for (SequenceGroup g1 : t1) {
            for (SequenceGroup g2 : t2) {
                CooldownConstraint constraint1 = new CooldownConstraint();
                constraint1.group = g2;
                constraint1.cooldown = g2.duration;
                g1.constraint.add(constraint1);
                CooldownConstraint constraint2 = new CooldownConstraint();
                constraint2.group = g1;
                constraint2.cooldown = g2.duration;
                g2.constraint.add(constraint2);
            }
        }
    }

    public void groupCombustion() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().combustion) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, 0, SequenceItem.calculations.combustionCooldown + 15.0, true, false, getCalculations().getEffectCooldown().get(StandardEffect.Combustion), VariableType.None, 0.0);
    }

    public void groupArcanePower() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().arcanePower) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.arcanePowerDuration, SequenceItem.calculations.arcanePowerCooldown, getCalculations().getEffectCooldown().get(StandardEffect.ArcanePower));
    }

    public void groupPowerInfusion() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().powerInfusion) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.powerInfusionDuration, SequenceItem.calculations.powerInfusionCooldown, getCalculations().getEffectCooldown().get(StandardEffect.PowerInfusion));
    }

    public void groupEvocation() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getVariableType() == VariableType.EvocationIV) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.evocationDurationIV, SequenceItem.calculations.evocationCooldown, getCalculations().getEffectCooldown().get(StandardEffect.Evocation));
        list.clear();
        for (SequenceItem item : sequence) {
            if (item.getVariableType() == VariableType.EvocationHero) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.evocationDurationHero, SequenceItem.calculations.evocationCooldown, getCalculations().getEffectCooldown().get(StandardEffect.Evocation));
        list.clear();
        for (SequenceItem item : sequence) {
            if (item.getVariableType() == VariableType.EvocationIVHero) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.evocationDurationIVHero, SequenceItem.calculations.evocationCooldown, getCalculations().getEffectCooldown().get(StandardEffect.Evocation));
        list.clear();
        for (SequenceItem item : sequence) {
            if (item.getVariableType() == VariableType.Evocation) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.evocationDuration, SequenceItem.calculations.evocationCooldown, getCalculations().getEffectCooldown().get(StandardEffect.Evocation));
    }

    public void groupIcyVeins() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().icyVeins) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, 20.0, SequenceItem.calculations.icyVeinsCooldown, false, SequenceItem.calculations.getCharacter().getMageTalents().getColdSnap() == 1, getCalculations().getEffectCooldown().get(StandardEffect.IcyVeins), VariableType.None, 0.0);
    }

    public void groupWaterElemental() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().waterElemental) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.waterElementalDuration, SequenceItem.calculations.waterElementalCooldown, false, SequenceItem.calculations.getCharacter().getMageTalents().getColdSnap() == 1, getCalculations().getEffectCooldown().get(StandardEffect.WaterElemental), VariableType.SummonWaterElemental, SequenceItem.calculations.getBaseGlobalCooldown());
    }

    public void groupMirrorImage() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().mirrorImage) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, SequenceItem.calculations.mirrorImageDuration, SequenceItem.calculations.mirrorImageCooldown, false, false, getCalculations().getEffectCooldown().get(StandardEffect.MirrorImage), VariableType.SummonMirrorImage, SequenceItem.calculations.getBaseGlobalCooldown());
    }

    public List<SequenceGroup> groupFlameCap() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().flameCap) list.add(item);
        }
        if (list.size() > 0) return groupCooldown(list, 60, 180, getCalculations().getEffectCooldown().get(StandardEffect.FlameCap));
        return null;
    }

    public void groupPotionOfWildMagic() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().potionOfWildMagic) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, 15, 120, getCalculations().getEffectCooldown().get(StandardEffect.PotionOfWildMagic));
    }

    public void groupPotionOfSpeed() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().potionOfSpeed) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, 15, 120, getCalculations().getEffectCooldown().get(StandardEffect.PotionOfSpeed));
    }

    public void groupBerserking() {
        List<SequenceItem> list = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().berserking) list.add(item);
        }
        if (list.size() > 0) groupCooldown(list, 10, 180, false, false, getCalculations().getEffectCooldown().get(StandardEffect.Berserking), VariableType.None, 0.0);
    }

    private List<SequenceGroup> groupCooldown(List<SequenceItem> cooldownItems, double maxDuration, double cooldown, EffectCooldown type) {
        return groupCooldown(cooldownItems, maxDuration, cooldown, false, false, type, VariableType.None, 0.0);
    }

    private boolean itemsCompatible(List<SequenceItem> item1, List<SequenceItem> item2, double maxCooldown) {
        return constraintsCompatible(getAllGroups(item1), getAllGroups(item2), maxCooldown);
    }

    private boolean itemsCompatible(List<SequenceItem> item1, SequenceItem item2, double maxCooldown) {
        return constraintsCompatible(getAllGroups(item1), item2.group, maxCooldown);
    }

    private List<SequenceGroup> getAllGroups(List<SequenceItem> items) {
        List<SequenceGroup> result = new ArrayList<SequenceGroup>();
        for (SequenceItem item : items) {
            for (SequenceGroup group : item.group) {
                if (!result.contains(group)) result.add(group);
            }
        }
        return result;
    }

    private boolean constraintsCompatible(List<SequenceGroup> group1, List<SequenceGroup> group2, double maxCooldown) {
        for (SequenceGroup group : group1) {
            for (CooldownConstraint constraint : group.constraint) {
                if (constraint.cooldown > maxCooldown && group2.contains(constraint.group)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean inActivationDistance(int seg1, int seg2, double effectDuration) {
        if (seg1 > seg2) {
            int tmp = seg1;
            seg1 = seg2;
            seg2 = tmp;
        }
        return SequenceItem.calculations.getSegmentList().get(seg2).getTimeStart() - SequenceItem.calculations.getSegmentList().get(seg1).getTimeEnd() < effectDuration - 0.00001;
    }

    private List<SequenceGroup> groupCooldown(List<SequenceItem> cooldownItems, double maxDuration, double cooldown, boolean combustionMode, boolean coldSnapMode, EffectCooldown type, VariableType activation, double activationDuration) {
        final double eps = 0.00001;
        List<SequenceGroup> existingGroup = new ArrayList<SequenceGroup>();
        List<SequenceGroup> unresolvedGroup = new ArrayList<SequenceGroup>();
        for (SequenceItem item : cooldownItems) {
            for (SequenceGroup group : item.group) {
                if (!existingGroup.contains(group)) existingGroup.add(group);
            }
        }
        List<List<SequenceItem>> chains = new ArrayList<List<SequenceItem>>();
        for (SequenceGroup group : existingGroup) {
            // if group duration is less than cooldown then all must be contiguous
            if (group.duration <= cooldown) {
                // check if any of the items is already an existing chain
                List<SequenceItem> chain = null;
                for (SequenceItem item : group.item) {
                    if (cooldownItems.contains(item)) {
                        for (int i = 0; i < chains.size(); i++) {
                            List<SequenceItem> c = chains.get(i);
                            if (c.contains(item)) {
                                if (chain != null && chain != c) {
                                    // merge chains
                                    chain.addAll(c);
                                    chains.remove(i);
                                    i--;
                                } else {
                                    chain = c;
                                }
                            }
                        }
                    }
                }
                // add new items to chain
                if (chain == null) {
                    chain = new ArrayList<SequenceItem>();
                    chains.add(chain);
                }
                for (SequenceItem item : group.item) {
                    if (cooldownItems.contains(item)) {
                        if (!chain.contains(item)) chain.add(item);
                    }
                }
            } else {
                unresolvedGroup.add(group);
            }
        }
        List<SequenceItem> unchained = new ArrayList<SequenceItem>();
        double totalDuration = 0;
        double combustionCount = 0;
        for (SequenceItem item : cooldownItems) {
            totalDuration += item.duration;
            if (combustionMode) combustionCount += item.duration / (item.getCastingState().combustionDuration * item.getCycle().castTime / item.getCycle().castProcs);
            boolean chained = false;
            for (List<SequenceItem> chain : chains) {
                if (chain.contains(item)) {
                    chained = true;
                    break;
                }
            }
            if (!chained) unchained.add(item);
        }
        // at this point we have a number of chains and remaining unchained items
        // chains cannot be split, unchained items can
        int maxChains = 0;
        if (combustionMode) {
            maxChains = (int) Math.ceil(combustionCount - eps);
        } else {
            maxChains = (int) Math.ceil((totalDuration - eps) / maxDuration);
        }
        List<SequenceGroup> partialGroups = new ArrayList<SequenceGroup>();
        for (List<SequenceItem> chain : chains) {
            SequenceGroup group = new SequenceGroup();
            group.addRange(chain);
            partialGroups.add(group);
        }
        partialGroups.sort((x, y) -> compareDotNet(y.duration, x.duration));
        if (partialGroups.size() < maxChains) {
            int addCount = maxChains - partialGroups.size();
            for (int i = 0; i < addCount; i++) {
                partialGroups.add(new SequenceGroup());
            }
        }

        for (int i = 0; i < partialGroups.size(); i++) {
            SequenceGroup group = partialGroups.get(i);
            double gap = 0.0;
            double activationGap = 0.0;
            if (combustionMode) {
                double tempSum = 0;
                for (SequenceItem item : group.item)
                    tempSum += item.duration / (item.getCastingState().combustionDuration * item.getCycle().castTime / item.getCycle().castProcs);
                gap = 1 - tempSum;
            } else {
                gap = maxDuration - group.duration;
                if (activationDuration > 0.0) {
                    activationGap = activationDuration;
                    for (SequenceItem item : group.item) {
                        if (item.getVariableType() == activation) activationGap -= item.duration;
                    }
                }
            }
            if (gap < -eps && coldSnapMode) {
                gap += maxDuration;
                activationGap += activationDuration;
            }
            if (gap > eps) {
                //int maxSegDistance = (int)Math.Ceiling(maxDuration / 30.0) + 1;
                //if (combustionMode) maxSegDistance = (int)Math.Ceiling(15.0 / 30.0) + 1;
                double allowedDistance = maxDuration;
                if (combustionMode) allowedDistance = 15.0;
                for (int j = i + 1; j < partialGroups.size(); j++) {
                    SequenceGroup subgroup = partialGroups.get(j);
                    double gapReduction = 0.0;
                    double activationGapReduction = 0.0;
                    if (combustionMode) {
                        double tempSum = 0;
                        for (SequenceItem item : subgroup.item)
                            tempSum += item.duration / (item.getCastingState().combustionDuration * item.getCycle().castTime / item.getCycle().castProcs);
                        gapReduction = tempSum;
                    } else {
                        gapReduction = subgroup.duration;
                        if (activationDuration > 0.0) {
                            for (SequenceItem item : group.item) {
                                if (item.getVariableType() == activation) activationGapReduction += item.duration;
                            }
                        }
                    }
                    if (subgroup.duration > 0 && gapReduction <= gap + eps && activationGapReduction <= activationGap + eps && gap - gapReduction >= activationGap - activationGapReduction - eps && inActivationDistance(group.getSegment(), subgroup.getSegment(), allowedDistance) && itemsCompatible(group.item, subgroup.item, 0)) {
                        gap -= gapReduction;
                        activationGap -= activationGapReduction;
                        group.addRange(subgroup.item);
                        partialGroups.remove(j);
                        j--;
                        if (partialGroups.size() < maxChains) {
                            int addCount = maxChains - partialGroups.size();
                            for (int k = 0; k < addCount; k++) {
                                partialGroups.add(new SequenceGroup());
                            }
                        }
                        if (gap < eps) break;
                    }
                }
                if (gap > eps) {
                    for (int j = 0; j < unchained.size(); j++) {
                        SequenceItem item = unchained.get(j);
                        if (group.getSegment() == -1 || inActivationDistance(group.getSegment(), item.getSegment(), allowedDistance)) {
                            double gapReduction = 0.0;
                            double activationGapReduction = 0.0;
                            if (combustionMode) {
                                gapReduction = item.duration / (item.getCastingState().combustionDuration * item.getCycle().castTime / item.getCycle().castProcs);
                            } else {
                                gapReduction = item.duration;
                                if (activationDuration > 0.0 && item.getVariableType() == activation) activationGapReduction = item.duration;
                            }
                            if (gapReduction <= gap + eps && activationGapReduction <= activationGap + eps && gap - gapReduction >= activationGap - activationGapReduction - eps) {
                                gap -= gapReduction;
                                activationGap -= activationGapReduction;
                                group.add(item);
                                unchained.remove(j);
                                j--;
                                if (gap < eps) break;
                            } else if ((activationGapReduction > eps && activationGap > eps) || (activationGapReduction == 0.0 && gap > activationGap + eps)) {
                                double split = 0;
                                if (combustionMode) {
                                    split = gap * (item.getCastingState().combustionDuration * item.getCycle().castTime / item.getCycle().castProcs);
                                } else if (activationGapReduction > eps && activationGap > eps) {
                                    split = activationGap;
                                } else {
                                    split = gap - activationGap;
                                }
                                group.add(split(item, split));
                                if (activationDuration > 0.0 && item.getVariableType() == activation) {
                                    activationGap -= split;
                                    gap -= split;
                                } else {
                                    gap = activationGap;
                                    if (gap < eps) break;
                                }
                            }
                        }
                    }
                }
            }
        }

        partialGroups.removeIf(group -> group.duration == 0);
        // finalize groups
        for (int i = 0; i < partialGroups.size(); i++) {
            SequenceGroup group = partialGroups.get(i);
            for (SequenceItem item : group.item) {
                item.group.add(group);
            }
            for (int j = 0; j < partialGroups.size(); j++) {
                if (i != j) {
                    CooldownConstraint constraint = new CooldownConstraint();
                    constraint.cooldown = cooldown;
                    constraint.duration = maxDuration;
                    constraint.group = partialGroups.get(j);
                    constraint.coldSnap = coldSnapMode;
                    constraint.effectCooldown = type;
                    group.constraint.add(constraint);
                }
            }
        }
        return partialGroups;
    }

    public SequenceGroup groupHeroism() {
        SequenceGroup group = new SequenceGroup();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().heroism) {
                group.add(item);
                item.group.add(group);
            }
        }
        return group;
    }

    private double moltenFuryStart = 0;

    public void groupMoltenFury() {
        SequenceGroup group = new SequenceGroup();
        for (SequenceItem item : sequence) {
            if (item.getCastingState().moltenFury) {
                group.add(item);
                item.group.add(group);
            }
        }
        moltenFuryStart = SequenceItem.calculations.getCalculationOptions().getFightDuration() - group.duration;
        group.setMinTime(moltenFuryStart);
    }

    List<SequenceItem> compactItems;
    List<Double> compactTime;
    double compactTotalTime;
    int compactGroupSplits;
    int compactInversions;
    //double compactLastDestro;

    public boolean sortGroups(Solver solver) {
        final double eps = 0.000001;
        List<SequenceItem> groupedItems = new ArrayList<SequenceItem>();
        for (SequenceItem item : sequence) {
            if (item.group.size() > 0) {
                groupedItems.add(item);
            }
        }
        compactTotalTime = Double.POSITIVE_INFINITY;
        compactGroupSplits = Integer.MAX_VALUE;
        compactInversions = Integer.MAX_VALUE;
        //compactLastDestro = double.NegativeInfinity;
        //sortGroups_AddRemainingItems(new List<SequenceItem>(), new List<double>(), groupedItems);
        groupedItems.sort((x, y) -> Integer.compare(x.getSegment(), y.getSegment()));
        if (getCalculations().getCalculationOptions().getDisplaySegmentCooldowns()) {
            sortGroups_ComputeSegmented(groupedItems, solver);
        } else {
            sortGroups_Compute(groupedItems, solver);
        }
        if (compactItems == null) {
            //return false;
        } else /*if (compactItems != null)*/ {
            for (int i = 0; i < compactItems.size(); i++) {
                compactItems.get(i).setMinTime(compactTime.get(i));
            }

            // compute max time
            double time = getFightDuration();
            for (int i = compactItems.size() - 1; i >= 0; i--) {
                SequenceItem item = compactItems.get(i);
                time = Math.min(time - item.duration, item.getMaxTime());
                // check constraints
                for (SequenceGroup group : item.group) {
                    // only compute max for first item in group
                    if (i == 0 || !compactItems.get(i - 1).group.contains(group)) {
                        for (CooldownConstraint constraint : group.constraint) {
                            for (int j = i + 1; j < compactItems.size(); j++) {
                                // skip cooldown constraints that are coldsnapped in the solution
                                if (compactItems.get(j).group.contains(constraint.group) && (!constraint.coldSnap || (compactItems.get(j).getMinTime() - item.getMinTime() >= SequenceItem.calculations.icyVeinsCooldown - eps))) {
                                    time = Math.min(time, compactItems.get(j).getMaxTime() - constraint.cooldown);
                                    break;
                                }
                            }
                        }
                    }
                }
                item.setMaxTime(time);
                double t = time;
                for (int j = i + 1; j < compactItems.size() && compactItems.get(j).superIndex == compactItems.get(j - 1).superIndex; j++) {
                    t += compactItems.get(j - 1).duration;
                    compactItems.get(j).setMaxTime(t);
                }
            }
        }

        sequence.sort((x, y) -> {
            boolean xgrouped = x.group.size() > 0;
            boolean ygrouped = y.group.size() > 0;
            int compare = Boolean.compare(xgrouped, ygrouped);
            if (compare != 0) return compare;
            compare = compareDotNet(x.getMinTime(), y.getMinTime());
            if (compare != 0) return compare;
            compare = Integer.compare(x.getSegment(), y.getSegment());
            return compare;
        });
        return true;
    }

    private int sortGroups_Compare(SequenceItem x, SequenceItem y, List<SequenceGroup> tail) {
        boolean xsingletail = x.tail.size() > 0;
        boolean ysingletail = y.tail.size() > 0;
        int compare = Boolean.compare(ysingletail, xsingletail);
        if (compare != 0) return compare;
        int xintersect = (tail == null) ? 0 : ListUtils.intersect(x.group, tail).size();
        int yintersect = (tail == null) ? 0 : ListUtils.intersect(y.group, tail).size();
        return Integer.compare(yintersect, xintersect);
    }

    private int hexCount(int hex) {
        int count = 0;
        while (hex > 0) {
            count += (hex & 1);
            hex >>= 1;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private void sortGroups_Compute(List<SequenceItem> itemList, Solver solver) {
        final double eps = 0.000001;
        int N = itemList.size();
        if (N == 0) return;
        List<Double> constructionTime = new ArrayList<Double>();
        List<Double>[] constructionTimeHistory = new List[N];
        boolean[] used = new boolean[N];
        int[] index = new int[N];
        int[] coldsnap = new int[N];
        double[] coldsnapTime = new double[N];
        int[] maxIntersect = new int[N];
        for (int j = 0; j < N; j++) itemList.get(j).superIndex = -1;
        int superIndex = -1;
        for (int j = 0; j < N; j++) {
            if (itemList.get(j).superIndex == -1) {
                superIndex++;
                itemList.get(j).superIndex = superIndex;
                List<SequenceItem> superList = new ArrayList<SequenceItem>();
                superList.add(itemList.get(j));
                boolean more = false;
                do {
                    more = false;
                    for (int k = j + 1; k < N; k++) {
                        if (itemList.get(k).superIndex == -1) {
                            for (SequenceItem item : superList) {
                                if (ListUtils.intersect(item.group, itemList.get(k).group).size() > 0) {
                                    itemList.get(k).superIndex = superIndex;
                                    superList.add(itemList.get(k));
                                    more = true;
                                    break;
                                }
                            }
                        }
                    }
                } while (more);
                List<SequenceGroup> superGroups = new ArrayList<SequenceGroup>();
                for (SequenceItem item : superList) {
                    int hex = 0;
                    for (SequenceGroup group : item.group) {
                        if (!superGroups.contains(group)) superGroups.add(group);
                        hex |= (1 << superGroups.indexOf(group));
                    }
                    item.cooldownHex = hex;
                }
            }
        }
        List<SequenceGroup> groupList = getAllGroups(itemList);
        superIndex++;
        int[] superLeft = new int[superIndex];
        for (int j = 0; j < N; j++) {
            superLeft[itemList.get(j).superIndex]++;
            for (SequenceGroup group : itemList.get(j).group) {
                group.orderIndex = -1;
            }
        }
        int maxSegment = 0;
        for (int j = 0; j < N; j++) {
            if (itemList.get(j).getSegment() > maxSegment) {
                maxSegment = itemList.get(j).getSegment();
            }
        }
        int[] earlierCount = new int[maxSegment + 1];
        for (int j = 0; j < N; j++) {
            for (int k = itemList.get(j).getSegment() + 1; k <= maxSegment; k++) {
                earlierCount[k]++;
            }
        }

        int i = 0;
        index[0] = 0;
        constructionTimeHistory[0] = constructionTime;
        coldsnap[0] = 2;
        long tickLimit = timeLimitEnabled ? System.currentTimeMillis() + 5000 : Long.MAX_VALUE;
        do {
            if (i == N) {
                double time = 0;
                if (constructionTime.size() > 0) time = constructionTime.get(constructionTime.size() - 1) + itemList.get(index[N - 1]).duration;
                // compute group splits
                int groupSplits = 0;
                for (SequenceGroup group : groupList) {
                    int minIndex = N - 1;
                    int maxIndex = 0;
                    for (SequenceItem item : group.item) {
                        if (item.orderIndex < minIndex) minIndex = item.orderIndex;
                        if (item.orderIndex > maxIndex) maxIndex = item.orderIndex;
                    }
                    groupSplits += (maxIndex - minIndex + 1) - group.item.size();
                }
                if (groupSplits < compactGroupSplits || (groupSplits == compactGroupSplits && time < compactTotalTime)) {
                    compactGroupSplits = groupSplits;
                    compactTotalTime = time;
                    compactTime = new ArrayList<Double>(constructionTime);
                    compactItems = new ArrayList<SequenceItem>();
                    for (int j = 0; j < N; j++) {
                        compactItems.add(itemList.get(index[j]));
                    }
                }
                i--;
                if (i >= 0) {
                    constructionTime = constructionTimeHistory[i];
                    used[index[i]] = false;
                    superLeft[itemList.get(index[i]).superIndex]++;
                    for (SequenceGroup group : itemList.get(index[i]).group) {
                        if (group.orderIndex == i) {
                            group.orderIndex = -1;
                        }
                    }
                }
            } else {
                coldsnap[i]--;
                if (coldsnap[i] < 0 || used[index[i]]) {
                    coldsnap[i] = 1;
                    do {
                        index[i]++;
                    } while (index[i] < N && used[index[i]]);
                }
                if (index[i] == N) {
                    i--;
                    if (i >= 0) {
                        constructionTime = constructionTimeHistory[i];
                        used[index[i]] = false;
                        superLeft[itemList.get(index[i]).superIndex]++;
                        for (SequenceGroup group : itemList.get(index[i]).group) {
                            if (group.orderIndex == i) {
                                group.orderIndex = -1;
                            }
                        }
                    }
                } else {
                    // check if valid
                    SequenceItem item = itemList.get(index[i]);
                    // if we have segmentation data take a more directed search, respect segmentation ordering (jumps should be eliminated, but check back in case something looks strange)
                    if (i == 0 || ((item.superIndex == itemList.get(index[i - 1]).superIndex || superLeft[itemList.get(index[i - 1]).superIndex] == 0) && item.getSegment() >= itemList.get(index[i - 1]).getSegment() && i >= earlierCount[item.getSegment()])) {
                        int tail = item.cooldownHex;
                        int activeTail = 0;
                        int intersectHex = 0;
                        if (i > 0 && item.superIndex == itemList.get(index[i - 1]).superIndex) {
                            activeTail = itemList.get(index[i - 1]).cooldownHex;
                            intersectHex = hexCount(tail & activeTail);
                            if (intersectHex > maxIntersect[i]) maxIntersect[i] = intersectHex;
                        }
                        if (intersectHex >= maxIntersect[i]) {
                            used[index[i]] = true;
                            itemList.get(index[i]).orderIndex = i;
                            superLeft[itemList.get(index[i]).superIndex]--;
                            for (SequenceGroup group : itemList.get(index[i]).group) {
                                if (group.orderIndex == -1) {
                                    // first item in this group
                                    group.orderIndex = i;
                                }
                            }
                            // skip tests for coldsnap == 0
                            if (superLeft[itemList.get(index[i]).superIndex] > 0) {
                                for (int j = 0; j < used.length; j++) {
                                    if (!used[j]) {
                                        if (itemList.get(j).superIndex == item.superIndex) {
                                            // make sure activations are placed before use
                                            if (item.getCastingState().waterElemental && item.getVariableType() != VariableType.SummonWaterElemental && itemList.get(j).getVariableType() == VariableType.SummonWaterElemental) {
                                                tail = 0;
                                                break;
                                            }
                                            if (item.getCastingState().mirrorImage && item.getVariableType() != VariableType.SummonMirrorImage && itemList.get(j).getVariableType() == VariableType.SummonMirrorImage) {
                                                tail = 0;
                                                break;
                                            }
                                            if (i > 0 && item.superIndex == itemList.get(index[i - 1]).superIndex) {
                                                int intersectHexJ = hexCount(itemList.get(j).cooldownHex & activeTail);
                                                if (intersectHexJ > intersectHex) {
                                                    if (j > index[i] && (i == 0 || itemList.get(j).getSegment() >= itemList.get(index[i - 1]).getSegment())) {
                                                        // anything up to j is not valid, so skip ahead
                                                        used[index[i]] = false;
                                                        for (SequenceGroup group : itemList.get(index[i]).group) {
                                                            if (group.orderIndex == i) {
                                                                group.orderIndex = -1;
                                                            }
                                                        }
                                                        superLeft[itemList.get(index[i]).superIndex]++;
                                                        index[i] = j;
                                                        used[j] = true;
                                                        itemList.get(j).orderIndex = i;
                                                        superLeft[itemList.get(index[i]).superIndex]--;
                                                        for (SequenceGroup group : itemList.get(j).group) {
                                                            if (group.orderIndex == -1) {
                                                                // first item in this group
                                                                group.orderIndex = i;
                                                            }
                                                        }
                                                        intersectHex = intersectHexJ;
                                                        maxIntersect[i] = intersectHex;
                                                        item = itemList.get(j);
                                                        tail = item.cooldownHex;
                                                        j = -1;
                                                        continue;
                                                    } else {
                                                        // invalidate
                                                        tail = 0;
                                                        break;
                                                    }
                                                }
                                            }
                                            if ((itemList.get(j).cooldownHex & activeTail & ~item.cooldownHex) != 0) {
                                                // we'll have to place something from active tail that is being removed
                                                // from active tail
                                                tail = 0;
                                                break;
                                            }
                                            int intersect = item.cooldownHex & itemList.get(j).cooldownHex;
                                            if (intersect > 0) {
                                                tail = intersect & tail;
                                                if (tail == 0) break;
                                            }
                                        }
                                    }
                                }
                            }
                            double time = 0;
                            if (constructionTime.size() > 0) time = constructionTime.get(constructionTime.size() - 1) + itemList.get(index[i - 1]).duration;
                            time = Math.max(time, item.getMinTime());
                            double minTotalTime = time + item.duration;
                            //for (int j = 0; j < N; j++) // too expensive
                            //{
                            //    if (!used[j]) minTotalTime += itemList[j].Duration;
                            //}
                            if (tail > 0 && minTotalTime < compactTotalTime) {
                                // check constraints
                                List<Integer> icyVeinsStarts = new ArrayList<Integer>();
                                List<Integer> waterElementalStarts = new ArrayList<Integer>();
                                SequenceGroup icyVeinsGroup = null;
                                SequenceGroup waterElementalGroup = null;
                                for (SequenceGroup group : item.group) {
                                    for (CooldownConstraint constraint : group.constraint) {
                                        if (!constraint.coldSnap) {
                                            int j = constraint.group.orderIndex;
                                            if (j >= 0) {
                                                time = Math.max(time, constructionTime.get(j) + constraint.cooldown);
                                            }
                                        } else {
                                            // if we're in group with already placed item then no need to redo all this
                                            if (i > 0 && itemList.get(index[i - 1]).group.contains(group)) continue;
                                            if (constraint.effectCooldown.mask == StandardEffect.IcyVeins) icyVeinsGroup = group;
                                            if (constraint.effectCooldown.mask == StandardEffect.WaterElemental) waterElementalGroup = group;
                                            int minIndex = i;
                                            for (SequenceItem coldsnapItem : constraint.group.item) {
                                                if (coldsnapItem.orderIndex >= 0 && coldsnapItem.orderIndex < N && index[coldsnapItem.orderIndex] < N && itemList.get(index[coldsnapItem.orderIndex]) == coldsnapItem && used[index[coldsnapItem.orderIndex]] && coldsnapItem.orderIndex < minIndex) {
                                                    minIndex = coldsnapItem.orderIndex;
                                                }
                                            }
                                            if (minIndex < i) {
                                                if (constraint.effectCooldown.mask == StandardEffect.IcyVeins) icyVeinsStarts.add(minIndex);
                                                if (constraint.effectCooldown.mask == StandardEffect.WaterElemental) waterElementalStarts.add(minIndex);
                                            }
                                        }
                                    }
                                }
                                boolean valid = true;
                                // we absolutely can't come faster than time
                                // now check coldsnap constraints
                                if (icyVeinsGroup != null || waterElementalGroup != null) {
                                    // this is only called for first coldsnap item in group
                                    icyVeinsStarts.sort(null);
                                    waterElementalStarts.sort(null);
                                    int lastColdsnap = -1;
                                    for (int j = 0; j < i; j++) {
                                        if (coldsnap[j] == 1) lastColdsnap = j;
                                    }
                                    if ((icyVeinsGroup != null && icyVeinsGroup.duration > 20.0 + eps) || (waterElementalGroup != null && waterElementalGroup.duration > SequenceItem.calculations.waterElementalDuration + eps)) {
                                        // we need internal coldsnap
                                        if (coldsnap[i] == 1) {
                                            double normalTime = time;
                                            if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                            if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                            double coldsnapReady = 0;
                                            if (lastColdsnap >= 0) coldsnapReady = coldsnapTime[lastColdsnap] + SequenceItem.calculations.coldsnapCooldown;
                                            // we have to do first one on normal time and have coldsnap ready in the middle
                                            time = normalTime;
                                            if (icyVeinsGroup != null) time = Math.max(time, coldsnapReady - 20.0);
                                            if (waterElementalGroup != null) time = Math.max(time, coldsnapReady - SequenceItem.calculations.waterElementalDuration);
                                            coldsnapTime[i] = Math.max(time, coldsnapReady);
                                        } else {
                                            // can't do without coldsnap
                                            valid = false;
                                        }
                                    } else if ((icyVeinsGroup == null || icyVeinsStarts.size() == 0 || time - constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) >= SequenceItem.calculations.icyVeinsCooldown - eps) && (waterElementalGroup == null || waterElementalStarts.size() == 0 || time - constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) >= SequenceItem.calculations.waterElementalCooldown - eps)) {
                                        // don't need coldsnap and can start right at time
                                        coldsnap[i] = 0;
                                    } else if (coldsnap[i] == 1) {
                                        // use coldsnap
                                        double normalTime = time;
                                        if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                        if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                        double coldsnapReady = 0;
                                        if (lastColdsnap >= 0) coldsnapReady = coldsnapTime[lastColdsnap] + SequenceItem.calculations.coldsnapCooldown;
                                        if (coldsnapReady >= normalTime) {
                                            // coldsnap won't be ready until IV/WE will be back anyway, so we don't actually need it
                                            coldsnap[i] = 0;
                                            time = normalTime;
                                        } else {
                                            // go now or when coldsnap is ready
                                            time = Math.max(coldsnapReady, time);
                                            coldsnapTime[i] = coldsnapReady;
                                            if (icyVeinsStarts.size() > 0) coldsnapTime[i] = Math.max(coldsnapTime[i], constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)));
                                            if (waterElementalStarts.size() > 0) coldsnapTime[i] = Math.max(coldsnapTime[i], constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)));
                                        }
                                    } else {
                                        // we are not allowed to use coldsnap even if we could
                                        // make sure to adjust by coldsnap constraints
                                        double normalTime = time;
                                        if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                        if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                        time = normalTime;
                                    }
                                } else {
                                    // no coldsnap constraints active
                                    coldsnap[i] = 0;
                                }
                                if (valid) {
                                    List<Double> adjustedConstructionTime = new ArrayList<Double>(constructionTime);
                                    adjustedConstructionTime.add(time);
                                    // adjust min time of items in same super group
                                    for (int j = adjustedConstructionTime.size() - 2; j >= 0 && itemList.get(index[j]).superIndex == item.superIndex; j--) {
                                        time -= itemList.get(index[j]).duration;
                                        adjustedConstructionTime.set(j, time);
                                    }
                                    constructionTimeHistory[i] = constructionTime;
                                    constructionTime = adjustedConstructionTime;
                                    i++;
                                    if (i < N) {
                                        index[i] = 0;
                                        maxIntersect[i] = 0;
                                        coldsnap[i] = 2;
                                    }
                                } else {
                                    used[index[i]] = false;
                                    superLeft[itemList.get(index[i]).superIndex]++;
                                    for (SequenceGroup group : itemList.get(index[i]).group) {
                                        if (group.orderIndex == i) {
                                            group.orderIndex = -1;
                                        }
                                    }
                                }
                            } else {
                                used[index[i]] = false;
                                superLeft[itemList.get(index[i]).superIndex]++;
                                for (SequenceGroup group : itemList.get(index[i]).group) {
                                    if (group.orderIndex == i) {
                                        group.orderIndex = -1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } while (i >= 0 && (solver == null || !solver.getCancellationPending()) && System.currentTimeMillis() < tickLimit);
    }

    @SuppressWarnings("unchecked")
    private void sortGroups_ComputeSegmented(List<SequenceItem> itemList, Solver solver) {
        final double eps = 0.000001;
        int N = itemList.size();
        if (N == 0) return;
        List<Double> constructionTime = new ArrayList<Double>();
        List<Double>[] constructionTimeHistory = new List[N];
        boolean[] used = new boolean[N];
        int[] index = new int[N];
        int[] inversions = new int[N];
        int[] coldsnap = new int[N];
        double[] coldsnapTime = new double[N];
        int[] maxIntersect = new int[N];
        for (int j = 0; j < N; j++) itemList.get(j).superIndex = -1;
        int superIndex = -1;
        for (int j = 0; j < N; j++) {
            if (itemList.get(j).superIndex == -1) {
                superIndex++;
                itemList.get(j).superIndex = superIndex;
                List<SequenceItem> superList = new ArrayList<SequenceItem>();
                superList.add(itemList.get(j));
                boolean more = false;
                do {
                    more = false;
                    for (int k = j + 1; k < N; k++) {
                        if (itemList.get(k).superIndex == -1) {
                            for (SequenceItem item : superList) {
                                if (ListUtils.intersect(item.group, itemList.get(k).group).size() > 0) {
                                    itemList.get(k).superIndex = superIndex;
                                    superList.add(itemList.get(k));
                                    more = true;
                                    break;
                                }
                            }
                        }
                    }
                } while (more);
                List<SequenceGroup> superGroups = new ArrayList<SequenceGroup>();
                for (SequenceItem item : superList) {
                    int hex = 0;
                    for (SequenceGroup group : item.group) {
                        if (!superGroups.contains(group)) superGroups.add(group);
                        hex |= (1 << superGroups.indexOf(group));
                    }
                    item.cooldownHex = hex;
                }
            }
        }
        List<SequenceGroup> groupList = getAllGroups(itemList);
        superIndex++;
        int[] superLeft = new int[superIndex];
        for (int j = 0; j < N; j++) {
            superLeft[itemList.get(j).superIndex]++;
            for (SequenceGroup group : itemList.get(j).group) {
                group.orderIndex = -1;
            }
        }
        int maxSegment = 0;
        for (int j = 0; j < N; j++) {
            if (itemList.get(j).getSegment() > maxSegment) {
                maxSegment = itemList.get(j).getSegment();
            }
        }
        int[] earlierCount = new int[maxSegment + 1];
        for (int j = 0; j < N; j++) {
            for (int k = itemList.get(j).getSegment() + 1; k <= maxSegment; k++) {
                earlierCount[k]++;
            }
        }

        int i = 0;
        index[0] = 0;
        constructionTimeHistory[0] = constructionTime;
        int maxColdsnap = getCalculations().getMageTalents().getColdSnap() + 1;
        coldsnap[0] = maxColdsnap;
        do {
            if (i == N) {
                //double time = 0;
                //if (constructionTime.Count > 0) time = constructionTime[constructionTime.Count - 1] + itemList[index[N - 1]].Duration;
                // compute group splits
                int groupSplits = 0;
                for (SequenceGroup group : groupList) {
                    int minIndex = N - 1;
                    int maxIndex = 0;
                    for (SequenceItem item : group.item) {
                        if (item.orderIndex < minIndex) minIndex = item.orderIndex;
                        if (item.orderIndex > maxIndex) maxIndex = item.orderIndex;
                    }
                    groupSplits += (maxIndex - minIndex + 1) - group.item.size();
                }
                if (inversions[N - 1] < compactInversions || (inversions[N - 1] == compactInversions && groupSplits < compactGroupSplits)) {
                    compactInversions = inversions[N - 1];
                    compactGroupSplits = groupSplits;
                    //compactTotalTime = time;
                    compactTime = new ArrayList<Double>(constructionTime);
                    compactItems = new ArrayList<SequenceItem>();
                    for (int j = 0; j < N; j++) {
                        compactItems.add(itemList.get(index[j]));
                    }
                }
                i--;
                if (i >= 0) {
                    constructionTime = constructionTimeHistory[i];
                    used[index[i]] = false;
                    superLeft[itemList.get(index[i]).superIndex]++;
                    for (SequenceGroup group : itemList.get(index[i]).group) {
                        if (group.orderIndex == i) {
                            group.orderIndex = -1;
                        }
                    }
                }
            } else {
                coldsnap[i]--;
                if (coldsnap[i] < 0 || used[index[i]]) {
                    coldsnap[i] = maxColdsnap - 1;
                    do {
                        index[i]++;
                    } while (index[i] < N && used[index[i]]);
                }
                if (index[i] == N) {
                    i--;
                    if (i >= 0) {
                        constructionTime = constructionTimeHistory[i];
                        used[index[i]] = false;
                        superLeft[itemList.get(index[i]).superIndex]++;
                        for (SequenceGroup group : itemList.get(index[i]).group) {
                            if (group.orderIndex == i) {
                                group.orderIndex = -1;
                            }
                        }
                    }
                } else {
                    if (i == 0) {
                        inversions[0] = 0;
                    } else {
                        inversions[i] = inversions[i - 1];
                    }
                    // check if valid
                    SequenceItem item = itemList.get(index[i]);
                    // if we have segmentation data take a more directed search, respect segmentation ordering (jumps should be eliminated, but check back in case something looks strange)
                    if (i == 0 || (item.getSegment() >= itemList.get(index[i - 1]).getSegment() && i >= earlierCount[item.getSegment()])) {
                        if (i > 0) {
                            if (!(item.superIndex == itemList.get(index[i - 1]).superIndex || superLeft[itemList.get(index[i - 1]).superIndex] == 0)) {
                                inversions[i]++; // this is actually just a group split, but should be fine i think
                            }
                        }
                        int tail = item.cooldownHex;
                        int activeTail = 0;
                        int intersectHex = 0;
                        if (i > 0 && item.superIndex == itemList.get(index[i - 1]).superIndex) {
                            activeTail = itemList.get(index[i - 1]).cooldownHex;
                            intersectHex = hexCount(tail & activeTail);
                            if (intersectHex > maxIntersect[i]) maxIntersect[i] = intersectHex;
                        }
                        if (intersectHex < maxIntersect[i]) {
                            inversions[i]++;
                        }
                        used[index[i]] = true;
                        itemList.get(index[i]).orderIndex = i;
                        superLeft[itemList.get(index[i]).superIndex]--;
                        for (SequenceGroup group : itemList.get(index[i]).group) {
                            if (group.orderIndex == -1) {
                                // first item in this group
                                group.orderIndex = i;
                            }
                        }
                        // skip tests for coldsnap == 0
                        if (superLeft[itemList.get(index[i]).superIndex] > 0) // if we don't have enough constraints not all of these might be enforced
                        {
                            for (int j = 0; j < used.length; j++) {
                                if (!used[j]) {
                                    if (itemList.get(j).superIndex == item.superIndex) {
                                        // make sure activations are placed before use
                                        if (item.getCastingState().waterElemental && item.getVariableType() != VariableType.SummonWaterElemental && itemList.get(j).getVariableType() == VariableType.SummonWaterElemental) {
                                            inversions[i]++;
                                            //tail = 0;
                                            //break;
                                        }
                                        if (item.getCastingState().mirrorImage && item.getVariableType() != VariableType.SummonMirrorImage && itemList.get(j).getVariableType() == VariableType.SummonMirrorImage) {
                                            inversions[i]++;
                                            //tail = 0;
                                            //break;
                                        }
                                        if (i > 0 && item.superIndex == itemList.get(index[i - 1]).superIndex) {
                                            int intersectHexJ = hexCount(itemList.get(j).cooldownHex & activeTail);
                                            if (intersectHexJ > intersectHex) {
                                                inversions[i]++;
                                            }
                                        }
                                        if ((itemList.get(j).cooldownHex & activeTail & ~item.cooldownHex) != 0) // strong requirement, if converting for reconstruction on nonsegmented data might have to remove this
                                        {
                                            // we'll have to place something from active tail that is being removed
                                            // from active tail
                                            inversions[i]++;
                                            //tail = 0;
                                            //break;
                                        }
                                        int intersect = item.cooldownHex & itemList.get(j).cooldownHex;
                                        if (intersect > 0) {
                                            tail = intersect & tail;
                                            if (tail == 0) {
                                                inversions[i]++;
                                                //break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        double time = 0;
                        if (constructionTime.size() > 0) time = constructionTime.get(constructionTime.size() - 1) + itemList.get(index[i - 1]).duration;
                        time = Math.max(time, item.getMinTime());
                        double minTotalTime = time + item.duration;
                        //for (int j = 0; j < N; j++) // too expensive
                        //{
                        //    if (!used[j]) minTotalTime += itemList[j].Duration;
                        //}
                        //if (tail > 0 && minTotalTime < compactTotalTime)
                        if (inversions[i] < compactInversions) {
                            // check constraints
                            List<Integer> icyVeinsStarts = new ArrayList<Integer>();
                            List<Integer> waterElementalStarts = new ArrayList<Integer>();
                            SequenceGroup icyVeinsGroup = null;
                            SequenceGroup waterElementalGroup = null;
                            for (SequenceGroup group : item.group) {
                                for (CooldownConstraint constraint : group.constraint) {
                                    if (!constraint.coldSnap) {
                                        int j = constraint.group.orderIndex;
                                        if (j >= 0) {
                                            time = Math.max(time, constructionTime.get(j) + constraint.cooldown);
                                        }
                                    } else {
                                        // if we're in group with already placed item then no need to redo all this
                                        if (i > 0 && itemList.get(index[i - 1]).group.contains(group)) continue;
                                        if (constraint.effectCooldown.mask == StandardEffect.IcyVeins) icyVeinsGroup = group;
                                        if (constraint.effectCooldown.mask == StandardEffect.WaterElemental) waterElementalGroup = group;
                                        int minIndex = i;
                                        for (SequenceItem coldsnapItem : constraint.group.item) {
                                            if (coldsnapItem.orderIndex >= 0 && coldsnapItem.orderIndex < N && index[coldsnapItem.orderIndex] < N && itemList.get(index[coldsnapItem.orderIndex]) == coldsnapItem && used[index[coldsnapItem.orderIndex]] && coldsnapItem.orderIndex < minIndex) {
                                                minIndex = coldsnapItem.orderIndex;
                                            }
                                        }
                                        if (minIndex < i) {
                                            if (constraint.effectCooldown.mask == StandardEffect.IcyVeins) icyVeinsStarts.add(minIndex);
                                            if (constraint.effectCooldown.mask == StandardEffect.WaterElemental) waterElementalStarts.add(minIndex);
                                        }
                                    }
                                }
                            }
                            boolean valid = true;
                            // we absolutely can't come faster than time
                            // now check coldsnap constraints
                            if (icyVeinsGroup != null || waterElementalGroup != null) {
                                // this is only called for first coldsnap item in group
                                icyVeinsStarts.sort(null);
                                waterElementalStarts.sort(null);
                                int lastColdsnap = -1;
                                for (int j = 0; j < i; j++) {
                                    if (coldsnap[j] == 1) lastColdsnap = j;
                                }
                                if ((icyVeinsGroup != null && icyVeinsGroup.duration > 20.0 + eps) || (waterElementalGroup != null && waterElementalGroup.duration > SequenceItem.calculations.waterElementalDuration + eps)) {
                                    // we need internal coldsnap
                                    if (coldsnap[i] == 1) {
                                        double normalTime = time;
                                        if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                        if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                        double coldsnapReady = 0;
                                        if (lastColdsnap >= 0) coldsnapReady = coldsnapTime[lastColdsnap] + SequenceItem.calculations.coldsnapCooldown;
                                        // we have to do first one on normal time and have coldsnap ready in the middle
                                        time = normalTime;
                                        if (icyVeinsGroup != null) time = Math.max(time, coldsnapReady - 20.0);
                                        if (waterElementalGroup != null) time = Math.max(time, coldsnapReady - SequenceItem.calculations.waterElementalDuration);
                                        coldsnapTime[i] = Math.max(time, coldsnapReady);
                                    } else {
                                        // can't do without coldsnap
                                        valid = false;
                                    }
                                } else if ((icyVeinsGroup == null || icyVeinsStarts.size() == 0 || time - constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) >= SequenceItem.calculations.icyVeinsCooldown - eps) && (waterElementalGroup == null || waterElementalStarts.size() == 0 || time - constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) >= SequenceItem.calculations.waterElementalCooldown - eps)) {
                                    // don't need coldsnap and can start right at time
                                    coldsnap[i] = 0;
                                } else if (coldsnap[i] == 1) {
                                    // use coldsnap
                                    double normalTime = time;
                                    if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                    if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                    double coldsnapReady = 0;
                                    if (lastColdsnap >= 0) coldsnapReady = coldsnapTime[lastColdsnap] + SequenceItem.calculations.coldsnapCooldown;
                                    if (coldsnapReady >= normalTime) {
                                        // coldsnap won't be ready until IV/WE will be back anyway, so we don't actually need it
                                        coldsnap[i] = 0;
                                        time = normalTime;
                                    } else {
                                        // go now or when coldsnap is ready
                                        time = Math.max(coldsnapReady, time);
                                        coldsnapTime[i] = coldsnapReady;
                                        if (icyVeinsStarts.size() > 0) coldsnapTime[i] = Math.max(coldsnapTime[i], constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)));
                                        if (waterElementalStarts.size() > 0) coldsnapTime[i] = Math.max(coldsnapTime[i], constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)));
                                    }
                                } else {
                                    // we are not allowed to use coldsnap even if we could
                                    // make sure to adjust by coldsnap constraints
                                    double normalTime = time;
                                    if (icyVeinsGroup != null && icyVeinsStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(icyVeinsStarts.get(icyVeinsStarts.size() - 1)) + SequenceItem.calculations.icyVeinsCooldown);
                                    if (waterElementalGroup != null && waterElementalStarts.size() > 0) normalTime = Math.max(normalTime, constructionTime.get(waterElementalStarts.get(waterElementalStarts.size() - 1)) + SequenceItem.calculations.waterElementalCooldown);
                                    time = normalTime;
                                }
                            } else {
                                // no coldsnap constraints active
                                coldsnap[i] = 0;
                            }
                            if (valid) {
                                List<Double> adjustedConstructionTime = new ArrayList<Double>(constructionTime);
                                adjustedConstructionTime.add(time);
                                // adjust min time of items in same super group
                                for (int j = adjustedConstructionTime.size() - 2; j >= 0 && itemList.get(index[j]).superIndex == item.superIndex; j--) {
                                    time -= itemList.get(index[j]).duration;
                                    adjustedConstructionTime.set(j, time);
                                }
                                constructionTimeHistory[i] = constructionTime;
                                constructionTime = adjustedConstructionTime;
                                i++;
                                if (i < N) {
                                    index[i] = 0;
                                    maxIntersect[i] = 0;
                                    coldsnap[i] = maxColdsnap;
                                }
                            } else {
                                used[index[i]] = false;
                                superLeft[itemList.get(index[i]).superIndex]++;
                                for (SequenceGroup group : itemList.get(index[i]).group) {
                                    if (group.orderIndex == i) {
                                        group.orderIndex = -1;
                                    }
                                }
                            }
                        } else {
                            used[index[i]] = false;
                            superLeft[itemList.get(index[i]).superIndex]++;
                            for (SequenceGroup group : itemList.get(index[i]).group) {
                                if (group.orderIndex == i) {
                                    group.orderIndex = -1;
                                }
                            }
                        }
                    }
                }
            }
        } while (i >= 0 && CalculationsMage.isSolverEnabled(solver));
    }

    private void sortGroups_AddRemainingItems(List<SequenceItem> constructionList, List<Double> constructionTime, List<SequenceItem> remainingList) {
        if (remainingList.size() == 0) {
            double time = 0;
            if (constructionTime.size() > 0) time = constructionTime.get(constructionTime.size() - 1) + constructionList.get(constructionTime.size() - 1).duration;
            if (time < compactTotalTime) {
                compactTotalTime = time;
                compactTime = new ArrayList<Double>(constructionTime);
                compactItems = new ArrayList<SequenceItem>(constructionList);
            }
        } else {
            for (SequenceItem item : remainingList) {
                item.tail = new ArrayList<SequenceGroup>(item.group);
            }

            for (SequenceItem item : remainingList) {
                for (SequenceItem tailitem : remainingList) {
                    List<SequenceGroup> intersect = ListUtils.intersect(item.group, tailitem.group);
                    if (intersect.size() > 0) {
                        item.tail = ListUtils.intersect(intersect, item.tail);
                    }
                }
            }

            List<SequenceGroup> tail = (constructionList.size() > 0) ? constructionList.get(constructionList.size() - 1).group : null;
            SequenceItem best = null;
            for (SequenceItem item : remainingList) {
                if (best == null || sortGroups_Compare(item, best, tail) < 0) {
                    best = item;
                }
            }

            for (int i = 0; i < remainingList.size(); i++) {
                SequenceItem item = remainingList.get(i);
                if (sortGroups_Compare(item, best, tail) == 0) {
                    remainingList.remove(i);
                    constructionList.add(item);
                    double time = 0;
                    if (constructionTime.size() > 0) time = constructionTime.get(constructionTime.size() - 1) + constructionList.get(constructionTime.size() - 1).duration;
                    time = Math.max(time, item.getMinTime());
                    // check constraints
                    for (SequenceGroup group : item.group) {
                        for (CooldownConstraint constraint : group.constraint) {
                            for (int j = 0; j < constructionList.size(); j++) {
                                if (constructionList.get(j).group.contains(constraint.group)) {
                                    time = Math.max(time, constructionTime.get(j) + constraint.cooldown);
                                    break;
                                }
                            }
                        }
                    }
                    List<Double> adjustedConstructionTime = new ArrayList<Double>(constructionTime);
                    adjustedConstructionTime.add(time);
                    // adjust min time of items in same super group
                    for (int j = adjustedConstructionTime.size() - 2; j >= 0 && ListUtils.intersect(constructionList.get(j).group, constructionList.get(j + 1).group).size() > 0; j--) {
                        time -= constructionList.get(j).duration;
                        adjustedConstructionTime.set(j, time);
                    }
                    sortGroups_AddRemainingItems(constructionList, adjustedConstructionTime, remainingList);
                    //constructionTime.RemoveAt(constructionTime.Count - 1);
                    constructionList.remove(constructionList.size() - 1);
                    remainingList.add(i, item);
                }
            }
        }
    }

    public void compact(boolean compactGroups) {
        for (int i = 0; i + 1 < sequence.size(); i++) {
            if (sequence.get(i).getIndex() == sequence.get(i + 1).getIndex() && (sequence.get(i).group.size() == 0 || compactGroups)) {
                sequence.get(i).duration += sequence.get(i + 1).duration;
                if (sequence.get(i).group.size() == 0) {
                    // when merging noncooldowns make sure to correct the max time
                    sequence.get(i).setTimeConstraint(sequence.get(i).getMinTime(), sequence.get(i).getMaxTime() - sequence.get(i + 1).duration);
                }
                sequence.remove(i + 1);
                i--;
            }
        }
    }

    public void repositionManaConsumption() {
        final double eps = 0.000001;
        if (sequence.size() == 0) return;
        double ghostMana = Math.max(0, -manaCheck());
        double fight = getFightDuration();
        double potTime = removeIndex(VariableType.ManaPotion);
        double gemTime = removeIndex(VariableType.ManaGem);
        //double evoTime = removeIndex(VariableType.Evocation);
        boolean gemActivated = SequenceItem.calculations.getManaGemEffect();
        double gemValue = SequenceItem.calculations.manaGemValue;
        double gemMaxValue = SequenceItem.calculations.maxManaGemValue;
        double potValue = SequenceItem.calculations.manaPotionValue;
        double potMaxValue = SequenceItem.calculations.maxManaPotionValue;
        double time = 0;
        double nextGem = 0;
        double nextPot = 0;
        //double nextEvo = 0;
        double maxTps = 50000.0;
        //double evocationFactor = 1.0;
        if (SequenceItem.calculations.getCalculationOptions().getTpsLimit() > 0.0) {
            maxTps = SequenceItem.calculations.getCalculationOptions().getTpsLimit();
        }
        if (sequence.get(0).getVariableType() == VariableType.Drinking) {
            time += sequence.get(0).duration;
        }
        computeTimestamps();
        int phase = 0;
        double mana = 0;
        double maxMps = Double.POSITIVE_INFINITY;
        double minMps = Double.NEGATIVE_INFINITY;
        double targetTime = 0;
        double lastTargetMana = -1;
        double extraMana = 0;
        double oomtime = Double.POSITIVE_INFINITY;
        double t = 0;
        int i;
        phaseLoop:
        do {
            if (phase == 0) {
                // DoStart:
                mana = evaluate(null, EvaluationMode.ManaAtTime, time);
                maxMps = Double.POSITIVE_INFINITY;
                if (!((potTime > 0 && nextPot == 0) || (gemTime > 0 && nextGem == 0)/* || (evoTime > 0 && nextEvo == 0)*/)) {
                    double m = mana + ghostMana;
                    for (double _potTime = potTime; _potTime > eps; _potTime -= 1.0) {
                        m += (1 + getBaseStats().getBonusManaPotion()) * potValue;
                    }
                    for (double _gemTime = gemTime; _gemTime > eps; _gemTime -= 1.0) {
                        m += (1 + getBaseStats().getBonusManaGem()) * gemValue;
                    }
                    //m += EvocationRegen * evoTime;
                    maxMps = m / (fight - time);
                }
                minMps = Double.NEGATIVE_INFINITY;
                targetTime = sequence.get(sequence.size() - 1).timestamp + sequence.get(sequence.size() - 1).duration;
                if (nextGem > time && gemTime > 0) {
                    double m = mana;
                    if (potTime > 0 && nextPot < nextGem) m += (1 + getBaseStats().getBonusManaPotion()) * potValue;
                    //if (evoTime > 0 && nextEvo < nextGem) m += EvocationRegen * Math.Min(evoTime, EvocationDuration * evocationFactor);
                    double mps = m / (nextGem - time);
                    if (mps < maxMps) {
                        maxMps = mps;
                        targetTime = nextGem;
                    }
                }
                if (nextPot > time && potTime > 0) {
                    double m = mana;
                    if (gemTime > 0 && nextGem < nextPot) m += (1 + getBaseStats().getBonusManaGem()) * gemValue;
                    //if (evoTime > 0 && nextEvo < nextPot) m += EvocationRegen * Math.Min(evoTime, EvocationDuration * evocationFactor);
                    double mps = m / (nextPot - time);
                    if (mps < maxMps) {
                        maxMps = mps;
                        targetTime = nextPot;
                    }
                }
                if (potTime > 0 && (nextPot <= nextGem || gemTime <= 0 || nextPot == 0) /*&& (nextPot <= nextEvo || nextPot == 0 || evoTime <= 0)*/) {
                    if (nextPot <= time) {
                        minMps = maxMps;
                    } else {
                        targetTime = nextPot;
                        minMps = ((1 + getBaseStats().getBonusManaPotion()) * potMaxValue - (getBaseStats().getMana() - mana)) / (targetTime - time);
                    }
                } else if (gemTime > 0 /*&& (nextGem <= nextEvo || nextGem == 0 || evoTime <= 0)*/) {
                    if (nextGem <= time) {
                        minMps = maxMps;
                    } else {
                        targetTime = nextGem;
                        minMps = ((1 + getBaseStats().getBonusManaGem()) * gemMaxValue - (getBaseStats().getMana() - mana)) / (targetTime - time);
                    }
                }
                if (potTime <= 0 && gemTime <= 0/* && evoTime <= 0*/) {
                    maxMps = mana / (targetTime - time);
                    minMps = -(getBaseStats().getMana() - mana) / (targetTime - time);
                }
                if (maxMps < minMps) maxMps = minMps; // if we have min mps constraint then at that point we'll be full on mana, whatever max mana has to be handled will have to deal with it later
                lastTargetMana = -1;
                extraMana = 0;
                oomtime = Double.POSITIVE_INFINITY;
            }
            if (phase != 2) {
                // Retry:
                sortByMps(true, minMps, maxMps, maxTps, time, Math.min(oomtime, targetTime), extraMana, mana);
            }
            phase = 0;
            // VerifyOOM:
            boolean gotoSwapRecovery = false;
            verifyOOM:
            {
                compact(false);
                // guard against oom
                //double targetmana = evaluate(null, EvaluationMode.ManaAtTime, targetTime);
                double targetmana = mana;
                t = 0;
                for (i = 0; i < sequence.size(); i++) {
                    double d = sequence.get(i).duration;
                    if (sequence.get(i).isManaPotionOrGem()) d = 0;
                    if (d > 0 && t + d > targetTime) {
                        targetmana -= sequence.get(i).getMps() * (targetTime - t);
                        // if we are not in super group and we run out of mana this means that Sort ran into boundaries
                        // resort to swapping in this case
                        // the other handler takes care only of trailing oom, we have to make sure here that we
                        // get to the point where the pot/gem can be used (targetTime is always for next consumable or end of fight)
                        if (targetmana < -eps && !((nextPot <= time && potTime > 0) || (nextGem <= time && gemTime > 0)/* || (nextEvo <= time && evoTime > 0)*/)) {
                            // only split if it is splittable
                            if (sequence.get(i).group.size() == 0) {
                                splitAt(i, targetTime - t);
                            }
                            extraMana = -targetmana;
                            gotoSwapRecovery = true;
                            break verifyOOM;
                        }
                        break;
                    } else if (d > 0 && t + d > time) {
                        targetmana -= sequence.get(i).getMps() * sequence.get(i).duration;
                        if (i < sequence.size() - 1 && targetmana < -eps && !((nextPot <= time && potTime > 0) || (nextGem <= time && gemTime > 0)/* || (nextEvo <= time && evoTime > 0)*/)) // only worry if we already started all mana cooldowns and we're not at last item (at which point we can't do anything and it's only ghost mana left)
                        {
                            // we run oom during construction
                            if (oomtime < Double.POSITIVE_INFINITY && Math.abs(t + d - oomtime) < eps && Math.abs(lastTargetMana - targetmana) < eps) {
                                // we were not successful in recovering from oom
                                // go into swap mode
                                // extraMana might not be set
                                extraMana = -targetmana;
                                gotoSwapRecovery = true;
                                break verifyOOM;
                            }
                            oomtime = t + d;
                            minMps = -(getBaseStats().getMana() - mana) / (oomtime - time);
                            extraMana = -targetmana;
                            lastTargetMana = targetmana;
                            phase = 1;
                            continue phaseLoop;
                        }
                    }
                    t += d;
                }
                if (oomtime < Double.POSITIVE_INFINITY) lastTargetMana = -1;
                double tmana = targetmana;
                oomtime = Double.POSITIVE_INFINITY;
                if (!(i >= sequence.size() - 1 || sequence.get(i).group.size() == 0 || (targetTime <= t && (i == 0 || sequence.get(i - 1).superGroup != sequence.get(i).superGroup)))) {
                    SequenceGroup sg = sequence.get(i).superGroup;
                    if (targetmana != lastTargetMana) {
                        // count mana till end of super group
                        // account for to be used consumables (don't assume evo during super group unless we haven't placed the first one, in that case it will actually be placed before the super group)
                        if (sequence.get(sequence.size() - 1).superGroup == sg) targetmana += ghostMana;
                        double _potTime = potTime;
                        double _nextPot = nextPot;
                        double _gemTime = gemTime;
                        double _nextGem = nextGem;
                        if (targetmana - sequence.get(i).getMps() * (sequence.get(i).duration - (targetTime - t)) < -eps) {
                            // targetmana / sequence[i].Mps = timeFromTargetTime
                            double oomt = targetTime + targetmana / sequence.get(i).getMps();
                            while (_potTime > eps && _nextPot < oomt + eps && targetmana - sequence.get(i).getMps() * (sequence.get(i).duration - (targetTime - t)) < -eps) {
                                targetmana += (1 + getBaseStats().getBonusManaPotion()) * potValue;
                                _potTime -= 1.0;
                                _nextPot += 120.0;
                                oomt = targetTime + targetmana / sequence.get(i).getMps();
                            }
                            while (_gemTime > eps && _nextGem < oomt + eps && targetmana - sequence.get(i).getMps() * (sequence.get(i).duration - (targetTime - t)) < -eps) {
                                targetmana += (1 + getBaseStats().getBonusManaGem()) * gemValue;
                                _gemTime -= 1.0;
                                _nextGem += 120.0;
                                oomt = targetTime + targetmana / sequence.get(i).getMps();
                            }
                            if (targetmana - sequence.get(i).getMps() * (sequence.get(i).duration - (targetTime - t)) < -eps) {
                                double regenTime = t + sequence.get(i).duration;
                                if (_potTime > eps && _nextPot < regenTime) regenTime = _nextPot;
                                if (_gemTime > eps && _nextGem < regenTime) regenTime = _nextGem;
                                // we run oom and regen options are not ready
                                minMps = -(getBaseStats().getMana() - mana) / (targetTime - time);
                                extraMana = -(targetmana - sequence.get(i).getMps() * (regenTime - targetTime));
                                lastTargetMana = tmana;
                                phase = 1;
                                continue phaseLoop;
                            }
                        }
                        targetmana -= sequence.get(i).getMps() * (sequence.get(i).duration - (targetTime - t));
                        t += sequence.get(i).duration;
                        i++;
                        while (i < sequence.size() && sequence.get(i).superGroup == sg) {
                            if (targetmana - sequence.get(i).getMps() * sequence.get(i).duration < -eps) {
                                // targetmana / sequence[i].Mps = timeFromTargetTime
                                double oomt = t + targetmana / sequence.get(i).getMps();
                                while (_potTime > eps && _nextPot < oomt + eps && targetmana - sequence.get(i).getMps() * sequence.get(i).duration < -eps) {
                                    targetmana += (1 + getBaseStats().getBonusManaPotion()) * potValue;
                                    _potTime -= 1.0;
                                    _nextPot += 120.0;
                                    oomt = t + targetmana / sequence.get(i).getMps();
                                }
                                while (_gemTime > eps && _nextGem < oomt + eps && targetmana - sequence.get(i).getMps() * sequence.get(i).duration < -eps) {
                                    targetmana += (1 + getBaseStats().getBonusManaGem()) * gemValue;
                                    _gemTime -= 1.0;
                                    _nextGem += 120.0;
                                    oomt = t + targetmana / sequence.get(i).getMps();
                                }
                                if (targetmana - sequence.get(i).getMps() * sequence.get(i).duration < -eps) {
                                    double regenTime = t + sequence.get(i).duration;
                                    if (_potTime > eps && _nextPot < regenTime) regenTime = _nextPot;
                                    if (_gemTime > eps && _nextGem < regenTime) regenTime = _nextGem;
                                    // we run oom and regen options are not ready
                                    minMps = -(getBaseStats().getMana() - mana) / (targetTime - time);
                                    extraMana = -(targetmana - sequence.get(i).getMps() * (regenTime - t));
                                    lastTargetMana = tmana;
                                    phase = 1;
                                    continue phaseLoop;
                                }
                            }
                            targetmana -= sequence.get(i).getMps() * sequence.get(i).duration;
                            t += sequence.get(i).duration;
                            i++;
                        }
                    } else {
                        gotoSwapRecovery = true;
                        break verifyOOM;
                    }
                }
            }
            if (gotoSwapRecovery) {
                // SwapRecovery:
                // we run out of mana in a long super group and most likely we've placed
                // some mana consumables inside the group already or the group is at its
                // minimum allowed time, preventing us to move things in front of it
                // resort to finding a non-cooldown high mps item before group and low mps
                // item after group and swap to gain extraMana mana
                // evaluate how much space we have with already placed mana consumables
                // if gap is less than extraMana then the super group is probably too expensive
                // for our mana pool, consider starting with full mana before super group
                // and delaying mana consumables to be used inside the super group
                int superIndex = i;
                double superTime = t;
                int lowestMpsIndex = 0;
                while (extraMana > eps && lowestMpsIndex >= 0) {
                    i = superIndex + 1;
                    t = superTime;
                    lowestMpsIndex = -1;
                    for (; i < sequence.size(); i++) {
                        if (sequence.get(i).group.size() == 0 && (lowestMpsIndex == -1 || sequence.get(i).getMps() < sequence.get(lowestMpsIndex).getMps())) {
                            lowestMpsIndex = i;
                        }
                    }
                    if (lowestMpsIndex > -1) {
                        boolean swapped = false;
                        // we've found a suitable candidate for swap
                        // find first suitable non-cooldown with higher mps to perform swap
                        // and validate mana gap as we go over items
                        for (i = superIndex; i >= 0; i--) {
                            double mpsdiff = sequence.get(i).getMps() - sequence.get(lowestMpsIndex).getMps();
                            boolean good = sequence.get(i).group.size() == 0 && (!sequence.get(i).isManaPotionOrGem() && !sequence.get(i).isEvocation()) && mpsdiff > 0;
                            // don't allow swaps if the non-cooldown has time restrictions that won't be met after swap
                            if (good) {
                                if (sequence.get(lowestMpsIndex).timestamp < sequence.get(i).getMinTime() - eps || sequence.get(lowestMpsIndex).timestamp > sequence.get(i).getMaxTime() + eps) {
                                    good = false;
                                }
                            }
                            if (good) {
                                // we can do the swap, this will happen at i < superIndex
                                // this can happen at i == superIndex also if there are items later that can't be pushed back
                                double neededSwap = extraMana / mpsdiff;
                                if (neededSwap > sequence.get(i).duration) neededSwap = sequence.get(i).duration;
                                if (neededSwap > sequence.get(lowestMpsIndex).duration) neededSwap = sequence.get(lowestMpsIndex).duration;
                                if (neededSwap < sequence.get(i).duration - eps) {
                                    splitAt(i, sequence.get(i).duration - neededSwap);
                                    superIndex++;
                                    i++;
                                    lowestMpsIndex++;
                                }
                                if (neededSwap < sequence.get(lowestMpsIndex).duration - eps) {
                                    splitAt(lowestMpsIndex, neededSwap);
                                }
                                // make the swap and recalc timestamps
                                SequenceItem tmp = sequence.get(i);
                                sequence.set(i, sequence.get(lowestMpsIndex));
                                sequence.set(lowestMpsIndex, tmp);
                                computeTimestamps();
                                extraMana -= neededSwap * mpsdiff;
                                swapped = true;
                                break;
                            } else {
                                double imana = evaluate(null, EvaluationMode.ManaAtTime, t);
                                if (getBaseStats().getMana() - imana < extraMana) {
                                    // we cannot perform the swap, but let's at least do a partial swap if possible
                                    // so that it is clear in the output that even starting from full mana our mana
                                    // pool is too shallow to go through this sequence
                                    if (getBaseStats().getMana() - imana > eps) {
                                        extraMana = getBaseStats().getMana() - imana;
                                    } else {
                                        // goto Abort
                                        lowestMpsIndex = -1;
                                        break;
                                    }
                                }
                                if (i > 0) {
                                    double d = sequence.get(i - 1).duration;
                                    if (sequence.get(i - 1).isManaPotionOrGem()) d = 0;
                                    t -= d;
                                }
                            }
                        }
                        if (!swapped) {
                            lowestMpsIndex = -1; // if the lowest can't be swapped anywhere don't go into infinite loop
                        }
                    }
                }
                if (lowestMpsIndex != -1) {
                    mana = evaluate(null, EvaluationMode.ManaAtTime, time);
                    // goto VerifyOOM
                    phase = 2;
                    continue phaseLoop;
                }
            }
            // Abort:
            double gem = nextGem;
            double pot = nextPot;
            //double evo = nextEvo;
            //bool evoMoved = false;
            if (gemTime > 0) gem = evaluate(null, EvaluationMode.ManaBelow, getBaseStats().getMana() - (1 + getBaseStats().getBonusManaGem()) * gemMaxValue, Math.max(time, nextGem), 4);
            if (potTime > 0) pot = evaluate(null, EvaluationMode.ManaBelow, getBaseStats().getMana() - (1 + getBaseStats().getBonusManaPotion()) * potMaxValue, Math.max(time, nextPot), 3);
            // use pot & gem before evo, they need tighter packing
            // start with pot because pot needs more buffer than gem usually
            // verify timing requirements for flame cap, destro pot
            boolean forceGem = false;
            boolean forcePot = false;
            t = 0;
            //double nextFlameCap = double.PositiveInfinity;
            //double nextFlameCapMin = double.PositiveInfinity;
            double nextEffectPotion = Double.POSITIVE_INFINITY;
            double nextEffectPotionMin = Double.POSITIVE_INFINITY;
            for (i = 0; i < sequence.size(); i++) {
                double d = sequence.get(i).duration;
                if (sequence.get(i).isManaPotionOrGem()) d = 0;
                if (d > 0 && t >= pot) {
                    if (sequence.get(i).getCastingState() != null && (sequence.get(i).getCastingState().potionOfWildMagic || sequence.get(i).getCastingState().potionOfSpeed)) {
                        nextEffectPotion = Math.min(nextEffectPotion, sequence.get(i).getMaxTime());
                        nextEffectPotionMin = Math.min(nextEffectPotionMin, sequence.get(i).getMinTime());
                    }
                }
                t += d;
            }
            if (potTime > 0 && nextEffectPotion != Double.POSITIVE_INFINITY) {
                if (pot > nextEffectPotion - 120.0 + eps && pot < nextEffectPotion) {
                    nextPot = nextEffectPotionMin + 120.0;
                    pot = evaluate(null, EvaluationMode.ManaBelow, getBaseStats().getMana() - (1 + getBaseStats().getBonusManaPotion()) * potMaxValue, Math.max(time, nextPot), 3);
                    if (nextPot > fight) {
                        nextPot = fight;
                        potTime = 0.0;
                    }
                }
            }
            if (potTime > 0 && gemTime > 0) {
                // very special case for now, revisit later
                if (nextEffectPotion != Double.POSITIVE_INFINITY) {
                    if (pot <= nextEffectPotion - 120.0 + eps && gem > pot - 30.0/* && double.IsPositiveInfinity(nextFlameCap)*/) forcePot = true;
                }
            }
            // if gem is activated then check for activations
            if (gemActivated && gemTime > 0) {
                double maxtime = fight;
                // don't wait for pot unless we'll still have room for gem after, just heuristic for now, do something more sophisticated
                if (potTime > 0 && pot < maxtime && (gem < pot || gem - pot > 20)) maxtime = pot;
                //if (evoTime > 0 && evo < maxtime) maxtime = evo;
                if (time == 0 && gemTime > 1 + eps) maxtime = fight; // assume that at start we want to activate SCB as soon as possible, in most cases we have to insert burn before activation to prevent overflow and that can cause pot to activate earlier recausing overflow
                t = 0;
                for (i = 0; i < sequence.size(); i++) {
                    double d = sequence.get(i).duration;
                    if (sequence.get(i).isManaPotionOrGem()) d = 0;
                    if (d > 0 && t >= time && t < maxtime && sequence.get(i).getCastingState().manaGemEffect && (i == 0 || (!sequence.get(i - 1).getCastingState().manaGemEffect && sequence.get(i - 1).getVariableType() != VariableType.ManaGem && sequence.get(i - 1).getVariableType() != VariableType.ManaPotion))) {
                        // make sure that we have verified time up to here (give some extra room to make sure overflow is accounted for)
                        if (t + d * 0.1 > targetTime + eps) {
                            targetTime = t + d * 0.1;
                            lastTargetMana = -1;
                            extraMana = 0;
                            oomtime = Double.POSITIVE_INFINITY;
                            // goto Retry
                            phase = 1;
                            continue phaseLoop;
                        }
                        // insert gem
                        insertIndex(ColumnManaGem, Math.min(1.0, gemTime), t);
                        time = Math.min(t + 0.01, t + d / 2); // block activation from moving
                        nextGem = t + 120;
                        gemTime -= 1.0;
                        if (gemTime <= eps) {
                            nextGem = fight;
                            gemTime = 0.0;
                        }
                        // goto DoStart
                        continue phaseLoop;
                    }
                    t += d;
                }
            }
            if (potTime > 0 && (((forcePot && !forceGem) || gemActivated || pot <= gem || gemTime <= 0 || (nextPot == 0 && pot < gem + 30 && potTime >= gemTime)) && (forcePot || !forceGem))/* && ((pot <= evo && nextEvo > SequenceItem.Calculations.EvocationCooldown) || evoTime <= 0)*/) {
                if (pot > targetTime + 0.00001) {
                    targetTime = pot;
                    lastTargetMana = -1;
                    extraMana = 0;
                    oomtime = Double.POSITIVE_INFINITY;
                    // goto Retry
                    phase = 1;
                    continue phaseLoop;
                }
                insertIndex(ColumnManaPotion, Math.min(1.0, potTime), pot);
                computeTimestamps();
                time = pot;
                nextPot = pot + 120;
                potTime -= 1.0;
                if (potTime <= eps || nextPot > fight) {
                    nextPot = fight;
                    potTime = 0.0;
                }
            } else if (!gemActivated && gemTime > 0/* && (gem <= evo || (nextGem == 0 && gem < evo + 30) || evoTime <= 0)*/) {
                if (gem > targetTime + 0.00001) {
                    targetTime = gem;
                    lastTargetMana = -1;
                    extraMana = 0;
                    oomtime = Double.POSITIVE_INFINITY;
                    // goto Retry
                    phase = 1;
                    continue phaseLoop;
                }
                insertIndex(ColumnManaGem, Math.min(1.0, gemTime), gem);
                computeTimestamps();
                time = gem;
                nextGem = gem + 120;
                gemTime -= 1.0;
                if (gemTime <= eps || nextGem > fight) {
                    nextGem = fight;
                    gemTime = 0.0;
                }
            } else {
                break;
            }
        } while (true);
    }

    public double manaCheck() {
        final double eps = 0.000001;
        double mana = SequenceItem.calculations.getStartingMana();
        for (int i = 0; i < sequence.size(); i++) {
            VariableType type = sequence.get(i).getVariableType();
            double duration = sequence.get(i).duration;
            double mps = sequence.get(i).getMps();
            if (type == VariableType.ManaPotion) {
                for (double _potTime = duration; _potTime > eps; _potTime -= 1.0) {
                    mana += (1 + getBaseStats().getBonusManaPotion()) * SequenceItem.calculations.manaPotionValue;
                }
            } else if (type == VariableType.ManaGem) {
                for (double _gemTime = duration; _gemTime > eps; _gemTime -= 1.0) {
                    mana += (1 + getBaseStats().getBonusManaGem()) * SequenceItem.calculations.manaGemValue;
                }
            } else if (type == VariableType.Evocation) {
                mana += getEvocationRegen() * sequence.get(i).duration;
            } else {
                mana -= mps * duration;
            }
        }
        return mana;
    }

    public enum ReportMode {
        Listing,
        Compact
    }

    private static String timeFormat(double time) {
        java.time.Duration span = java.time.Duration.ofNanos((long) (round(time, 2) / 0.0000001) * 100L);
        return String.format(Locale.ROOT, "%02d:%02d.%03d", span.toMinutesPart(), span.toSecondsPart(), span.toMillisPart());
    }

    private static double round(double value, int digits) {
        if (value == 0.0 || Double.isInfinite(value) || Double.isNaN(value)) return value;
        if (Math.abs(value) < 1E15) {
            double power = Math.pow(10, digits);
            double temp = value * power;
            if (temp >= 1E16) return value;
            temp = Math.rint(temp);
            return temp / power;
        }
        return value;
    }

    private static String formatRounded(double value) {
        return String.format(Locale.ROOT, "%.0f", Math.rint(value));
    }

    private static class CooldownData {
        public EffectCooldown effect;
        public double cooldown;
        public double time = Double.NEGATIVE_INFINITY;
        public boolean active;
        public boolean warning;
    }

    public double evaluate(StringBuilder timing, EvaluationMode mode, double... data) {
        final double eps = 0.00001;
        double time = 0;
        double mana = SequenceItem.calculations.getStartingMana();

        ReportMode reportMode = ReportMode.Compact;

        boolean coldsnap = SequenceItem.calculations.getCharacter().getMageTalents().getColdSnap() == 1;
        double coldsnapCooldownDuration = SequenceItem.calculations.coldsnapCooldown;
        boolean gemActivated = SequenceItem.calculations.getManaGemEffect();

        int gemCount = 0;
        double potionCooldown = 0;
        double gemCooldown = 0;
        double flameCapCooldown = 0;
        boolean heroismUsed = false;
        double evocationCooldown = 0;
        double berserkingCooldown = 0;
        double apCooldown = 0;
        double piCooldown = 0;
        double ivCooldown = 0;
        double weCooldown = 0;
        double miCooldown = 0;
        double combustionCooldown = 0;

        double flameCapTime = Double.NEGATIVE_INFINITY;
        double berserkingTime = Double.NEGATIVE_INFINITY;
        double potionOfWildMagicTime = Double.NEGATIVE_INFINITY;
        double potionOfSpeedTime = Double.NEGATIVE_INFINITY;
        double combustionTime = Double.NEGATIVE_INFINITY;
        double moltenFuryTime = Double.NEGATIVE_INFINITY;
        double heroismTime = Double.NEGATIVE_INFINITY;
        double apTime = Double.NEGATIVE_INFINITY;
        double piTime = Double.NEGATIVE_INFINITY;
        double ivTime = Double.NEGATIVE_INFINITY;
        double weTime = Double.NEGATIVE_INFINITY;
        double miTime = Double.NEGATIVE_INFINITY;
        double manaGemEffectTime = Double.NEGATIVE_INFINITY;

        boolean flameCapActive = false;
        boolean berserkingActive = false;
        boolean potionOfWildMagicActive = false;
        boolean potionOfSpeedActive = false;
        boolean combustionActive = false;
        boolean moltenFuryActive = false;
        boolean heroismActive = false;
        boolean apActive = false;
        boolean piActive = false;
        boolean manaGemEffectActive = false;
        boolean ivActive = false;
        boolean weActive = false;
        boolean miActive = false;

        double coldsnapTimeMin = Double.NEGATIVE_INFINITY;
        double coldsnapTimeMax = Double.NEGATIVE_INFINITY;

        boolean potionWarning = false;
        //boolean gemWarning = false;
        boolean flameCapWarning = false;
        boolean apWarning = false;
        boolean piWarning = false;
        boolean ivWarning = false;
        boolean weWarning = false;
        boolean miWarning = false;
        boolean combustionWarning = false;
        boolean berserkingWarning = false;
        boolean manaWarning = false;

        CooldownData[] itemData = new CooldownData[getCalculations().getItemBasedEffectCooldowns().length];
        for (int i = 0; i < itemData.length; i++) {
            itemData[i] = new CooldownData();
            itemData[i].effect = getCalculations().getItemBasedEffectCooldowns()[i];
        }

        double combustionLeft = 0;

        double unexplained = 0;

        if (timing != null) timing.setLength(0);
        if (timing != null) timing.append("*");

        // for each cooldown compute how much time is unexplainable
        for (int i = 0; i < sequence.size(); i++) {
            int index = sequence.get(i).getIndex();
            VariableType type = sequence.get(i).getVariableType();
            double duration = sequence.get(i).duration;
            Cycle cycle = sequence.get(i).getCycle();
            CastingState state = sequence.get(i).getCastingState();
            double mps = sequence.get(i).getMps();
            if (sequence.get(i).isManaPotionOrGem()) duration = 0;
            double manabefore = mana;
            boolean cooldownContinuation = false;
            if (berserkingActive || flameCapActive || potionOfWildMagicActive || potionOfSpeedActive || heroismActive || moltenFuryActive || combustionActive || apActive || ivActive || manaGemEffectActive || piActive) {
                cooldownContinuation = true;
            }
            for (CooldownData d : itemData) {
                if (d.active) {
                    cooldownContinuation = true;
                    break;
                }
            }
            // Mana
            if (mode == EvaluationMode.ManaBelow) {
                if (data[0] < 0) data[0] = 0.0;
                if (data.length > 2) {
                    if (data[2] == 3) {
                        if (potionCooldown > 0) data[1] = Math.max(data[1], time + potionCooldown);
                    } else if (data[2] == 4) {
                        if (gemCooldown > 0) data[1] = Math.max(data[1], time + gemCooldown);
                    }
                }
            }
            if (mps > 0) {
                double maxtime = mana / mps;
                if (duration > maxtime + eps) {
                    // allow some leeway due to rounding errors from LP solver
                    if (!(i == sequence.size() - 1 && mps * duration < mana + 200)) {
                        unexplained += duration - maxtime;
                        if (timing != null && !manaWarning) timing.append("WARNING: Will run out of mana!").append("\r\n");
                        manaWarning = true;
                    }
                }
                if (mode == EvaluationMode.ManaBelow) {
                    double limit = data[0];
                    double aftertime = data[1];
                    double timetolimit = (mana - limit) / mps;
                    if (time + duration > aftertime + eps) {
                        if (time >= aftertime && mana < limit) return time;
                        if (time + timetolimit >= aftertime && timetolimit < duration) return time + timetolimit;
                        if (timetolimit < duration) return aftertime;
                    }
                }
            } else {
                if (mode == EvaluationMode.ManaBelow) {
                    double limit = data[0];
                    double aftertime = data[1];
                    if (time + duration > aftertime + eps) {
                        if (time >= aftertime && mana < limit) return time;
                        if (aftertime > time && mana - mps * (aftertime - time) < limit) return aftertime;
                    }
                }
            }
            if (mode == EvaluationMode.ManaAtTime && duration > 0) {
                double evalTime = data[0];
                if (time + duration > evalTime + eps) {
                    return mana - mps * (evalTime - time);
                }
            }
            mana -= mps * duration;
            // Mana Potion
            if (type == VariableType.ManaPotion) {
                if (potionCooldown > eps) {
                    unexplained += sequence.get(i).duration;
                    if (timing != null) timing.append("WARNING: Potion cooldown not ready!").append("\r\n");
                } else {
                    if (sequence.get(i).duration > 1.0) {
                        unexplained += sequence.get(i).duration - 1.0;
                        if (timing != null) timing.append("WARNING: Potion ammount too big!").append("\r\n");
                    }
                    if (timing != null) timing.append(timeFormat(time) + ": Mana Potion (" + formatRounded(mana) + " mana)").append("\r\n");
                    mana += (1 + getBaseStats().getBonusManaPotion()) * SequenceItem.calculations.manaPotionValue;
                    potionCooldown = 120;
                    potionWarning = false;
                }
            }
            // Mana Gem
            if (type == VariableType.ManaGem) {
                if (gemCooldown > eps) {
                    unexplained += sequence.get(i).duration;
                    if (timing != null) timing.append("WARNING: Gem cooldown not ready!").append("\r\n");
                } else {
                    if (sequence.get(i).duration > 1.0) {
                        unexplained += sequence.get(i).duration - 1.0;
                        if (timing != null) timing.append("WARNING: Gem ammount too big!").append("\r\n");
                    }
                    if (timing != null) timing.append(timeFormat(time) + ": Mana Gem (" + formatRounded(mana) + " mana)").append("\r\n");
                    mana += (1 + getBaseStats().getBonusManaGem()) * SequenceItem.calculations.manaGemValue;
                    gemCount++;
                    gemCooldown = 120;
                    //gemWarning = false;
                    if (gemActivated) {
                        manaGemEffectActive = true;
                        manaGemEffectTime = time;
                    }
                }
            }
            // Evocation
            if (type == VariableType.Evocation) {
                if (i == 0 || sequence.get(i - 1).getVariableType() != VariableType.Evocation) {
                    if (evocationCooldown > eps) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation cooldown not ready!").append("\r\n");
                    } else {
                        if (duration > getEvocationDuration()) {
                            unexplained += duration - getEvocationDuration();
                            if (timing != null) timing.append("WARNING: Evocation duration too long!").append("\r\n");
                        }
                        if (timing != null) timing.append(timeFormat(time) + ": Evocation (" + formatRounded(mana) + " mana)").append("\r\n");
                        mana += Math.min(getEvocationDuration(), duration) * getEvocationRegen();
                        evocationCooldown = SequenceItem.calculations.evocationCooldown;
                    }
                } else {
                    mana += Math.min(getEvocationDuration(), duration) * getEvocationRegen();
                }
            }
            if (type == VariableType.EvocationIV) {
                if (i == 0 || sequence.get(i - 1).getVariableType() != VariableType.EvocationIV) {
                    if (!(sequence.get(i).getCastingState() != null && sequence.get(i).getCastingState().effectsActive(StandardEffect.IcyVeins) || (i > 0 && sequence.get(i - 1).getCastingState() != null && sequence.get(i - 1).getCastingState().effectsActive(StandardEffect.IcyVeins)))) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation (Icy Veins) without Icy Veins to activate it!").append("\r\n");
                    } else if (evocationCooldown > eps) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation cooldown not ready!").append("\r\n");
                    } else {
                        if (duration > SequenceItem.calculations.evocationDurationIV) {
                            unexplained += duration - SequenceItem.calculations.evocationDurationIV;
                            if (timing != null) timing.append("WARNING: Evocation duration too long!").append("\r\n");
                        }
                        if (timing != null) timing.append(timeFormat(time) + ": Evocation (Icy Veins) (" + formatRounded(mana) + " mana)").append("\r\n");
                        mana += Math.min(SequenceItem.calculations.evocationDurationIV, duration) * SequenceItem.calculations.evocationRegenIV;
                        evocationCooldown = SequenceItem.calculations.evocationCooldown;
                    }
                } else {
                    mana += Math.min(SequenceItem.calculations.evocationDurationIV, duration) * SequenceItem.calculations.evocationRegenIV;
                }
            }
            if (type == VariableType.EvocationHero) {
                if (i == 0 || sequence.get(i - 1).getVariableType() != VariableType.EvocationHero) {
                    if (!(sequence.get(i).getCastingState() != null && sequence.get(i).getCastingState().effectsActive(StandardEffect.Heroism) || (i > 0 && sequence.get(i - 1).getCastingState() != null && sequence.get(i - 1).getCastingState().effectsActive(StandardEffect.Heroism)))) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation (Heroism) without Heroism to activate it!").append("\r\n");
                    } else if (evocationCooldown > eps) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation cooldown not ready!").append("\r\n");
                    } else {
                        if (duration > SequenceItem.calculations.evocationDurationHero) {
                            unexplained += duration - SequenceItem.calculations.evocationDurationHero;
                            if (timing != null) timing.append("WARNING: Evocation duration too long!").append("\r\n");
                        }
                        if (timing != null) timing.append(timeFormat(time) + ": Evocation (Heroism) (" + formatRounded(mana) + " mana)").append("\r\n");
                        mana += Math.min(SequenceItem.calculations.evocationDurationHero, duration) * SequenceItem.calculations.evocationRegenHero;
                        evocationCooldown = SequenceItem.calculations.evocationCooldown;
                    }
                } else {
                    mana += Math.min(SequenceItem.calculations.evocationDurationHero, duration) * SequenceItem.calculations.evocationRegenHero;
                }
            }
            if (type == VariableType.EvocationIVHero) {
                if (i == 0 || sequence.get(i - 1).getVariableType() != VariableType.EvocationIVHero) {
                    if (!(sequence.get(i).getCastingState() != null && sequence.get(i).getCastingState().effectsActive(StandardEffect.IcyVeins | StandardEffect.Heroism) || (i > 0 && sequence.get(i - 1).getCastingState() != null && sequence.get(i - 1).getCastingState().effectsActive(StandardEffect.IcyVeins | StandardEffect.Heroism)))) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation (Icy Veins+Heroism) without Icy Veins+Heroism to activate it!").append("\r\n");
                    } else if (evocationCooldown > eps) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Evocation cooldown not ready!").append("\r\n");
                    } else {
                        if (duration > SequenceItem.calculations.evocationDurationIVHero) {
                            unexplained += duration - SequenceItem.calculations.evocationDurationIVHero;
                            if (timing != null) timing.append("WARNING: Evocation duration too long!").append("\r\n");
                        }
                        if (timing != null) timing.append(timeFormat(time) + ": Evocation (Icy Veins+Heroism) (" + formatRounded(mana) + " mana)").append("\r\n");
                        mana += Math.min(SequenceItem.calculations.evocationDurationIVHero, duration) * SequenceItem.calculations.evocationRegenIVHero;
                        evocationCooldown = SequenceItem.calculations.evocationCooldown;
                    }
                } else {
                    mana += Math.min(SequenceItem.calculations.evocationDurationIVHero, duration) * SequenceItem.calculations.evocationRegenIVHero;
                }
            }
            if (mana < 0) mana = 0;
            if (mana > getBaseStats().getMana() + eps) {
                if (timing != null) timing.append("INFO: Mana overflow!").append("\r\n");
                mana = getBaseStats().getMana();
            }
            if (mana > 0) manaWarning = false;
            // Berserking
            if (berserkingActive) {
                if (state != null && state.berserking) {
                    if (time + duration > berserkingTime + 10 + eps) {
                        unexplained += time + duration - berserkingTime - 10;
                        if (timing != null) timing.append("WARNING: Berserking duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && 10 - (time - berserkingTime) > eps) {
                    //unexplained += Math.Min(duration, 10 - (time - berserkingTime));
                    if (timing != null) timing.append("INFO: Berserking is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.berserking) {
                    if (berserkingCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !berserkingWarning) timing.append("WARNING: Berserking cooldown not ready!").append("\r\n");
                        berserkingWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Berserking (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        berserkingCooldown = 180.0f;
                        berserkingTime = time;
                        berserkingWarning = false;
                        berserkingActive = true;
                    }
                }
            }
            // Flame Cap
            if (flameCapActive) {
                if (state != null && state.flameCap) {
                    if (time + duration > flameCapTime + 60 + eps) {
                        unexplained += time + duration - flameCapTime - 60;
                        if (timing != null) timing.append("WARNING: Flame Cap duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && 60 - (time - flameCapTime) > eps) {
                    //unexplained += Math.Min(duration, 15 - (time - apTime));
                    if (timing != null) timing.append("INFO: Flame Cap is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.flameCap) {
                    if (flameCapCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !flameCapWarning) timing.append("WARNING: Flame Cap cooldown not ready!").append("\r\n");
                        flameCapWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Flame Cap (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        flameCapCooldown = 180;
                        flameCapTime = time;
                        flameCapWarning = false;
                        flameCapActive = true;
                    }
                }
            }
            // Mana Gem Effect
            if (manaGemEffectActive) {
                if (state != null && state.manaGemEffect) {
                    if (time + duration > manaGemEffectTime + SequenceItem.calculations.manaGemEffectDuration + eps) {
                        unexplained += time + duration - manaGemEffectTime - SequenceItem.calculations.manaGemEffectDuration;
                        if (timing != null) timing.append("WARNING: Mana Gem Effect duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && SequenceItem.calculations.manaGemEffectDuration - (time - manaGemEffectTime) > eps) {
                    //unexplained += Math.Min(duration, 15 - (time - apTime));
                    if (timing != null) timing.append("INFO: Mana Gem Effect is still up!").append("\r\n");
                }
            }
            // Potion of Wild Magic
            if (potionOfWildMagicActive) {
                if (state != null && state.potionOfWildMagic) {
                    if (time + duration > potionOfWildMagicTime + 15 + eps) {
                        unexplained += time + duration - potionOfWildMagicTime - 15;
                        if (timing != null) timing.append("WARNING: Potion of Wild Magic duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && 15 - (time - potionOfWildMagicTime) > eps) {
                    //unexplained += Math.Min(duration, 15 - (time - apTime));
                    if (timing != null) timing.append("INFO: Potion of Wild Magic is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.potionOfWildMagic) {
                    if (potionCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !potionWarning) timing.append("WARNING: Potion of Wild Magic cooldown not ready!").append("\r\n");
                        potionWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Potion of Wild Magic (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        potionCooldown = 120;
                        potionOfWildMagicTime = time;
                        potionWarning = false;
                        potionOfWildMagicActive = true;
                    }
                }
            }
            // Potion of Speed
            if (potionOfSpeedActive) {
                if (state != null && state.potionOfSpeed) {
                    if (time + duration > potionOfSpeedTime + 15 + eps) {
                        unexplained += time + duration - potionOfSpeedTime - 15;
                        if (timing != null) timing.append("WARNING: Potion of Speed duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && 15 - (time - potionOfSpeedTime) > eps) {
                    //unexplained += Math.Min(duration, 15 - (time - apTime));
                    if (timing != null) timing.append("INFO: Potion of Speed is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.potionOfSpeed) {
                    if (potionCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !potionWarning) timing.append("WARNING: Potion of Speed cooldown not ready!").append("\r\n");
                        potionWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Potion of Speed (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        potionCooldown = 120;
                        potionOfSpeedTime = time;
                        potionWarning = false;
                        potionOfSpeedActive = true;
                    }
                }
            }
            // items
            for (CooldownData d : itemData) {
                if (d.active) {
                    if (state != null && state.effectsActive(d.effect.mask)) {
                        if (time + duration > d.time + d.effect.duration + eps) {
                            unexplained += time + duration - d.time - d.effect.duration;
                            if (timing != null) timing.append("WARNING: " + d.effect.name + " duration too long!").append("\r\n");
                        }
                    } else if (duration > 0 && d.effect.duration - (time - d.time) > eps) {
                        //unexplained += Math.Min(duration, Trinket1Duration - (time - trinket1time));
                        if (timing != null) timing.append("INFO: " + d.effect.name + " is still up!").append("\r\n");
                    }
                } else {
                    if (state != null && state.effectsActive(d.effect.mask)) {
                        if (d.cooldown > eps) {
                            unexplained += duration;
                            if (timing != null && !d.warning) timing.append("WARNING: " + d.effect.name + " cooldown not ready!").append("\r\n");
                            d.warning = true;
                        } else {
                            if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": " + d.effect.name + " (" + formatRounded(manabefore) + " mana)").append("\r\n");
                            d.cooldown = d.effect.cooldown;
                            d.time = time;
                            d.warning = false;
                            d.active = true;
                        }
                    }
                }
            }
            // Heroism
            if (heroismActive) {
                if (state != null && state.heroism) {
                    if (time + duration > heroismTime + 40 + eps) {
                        unexplained += time + duration - heroismTime - 40;
                        if (timing != null) timing.append("WARNING: Heroism duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && 40 - (time - heroismTime) > eps) {
                    //unexplained += Math.Min(duration, 40 - (time - heroismTime));
                    if (timing != null) timing.append("INFO: Heroism is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.heroism) {
                    if (heroismUsed) {
                        unexplained += duration;
                        if (timing != null) timing.append("WARNING: Heroism cooldown not ready!").append("\r\n");
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Heroism (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        heroismUsed = true;
                        heroismTime = time;
                        heroismActive = true;
                    }
                }
            }
            // Molten Fury
            if (moltenFuryActive) {
                if (!(state != null && state.moltenFury) && duration > 0) {
                    //unexplained += duration;
                    if (timing != null) timing.append("INFO: Molten Fury is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.moltenFury) {
                    if (time < moltenFuryStart - eps) {
                        unexplained += Math.min(duration, moltenFuryStart - time);
                        if (timing != null) timing.append("WARNING: Molten Fury is not available yet!").append("\r\n");
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Molten Fury (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        moltenFuryTime = time;
                        moltenFuryActive = true;
                    }
                }
            }
            // Combustion
            if (combustionActive) {
                if (state != null && state.combustion) {
                    if (duration / (state.combustionDuration * cycle.castTime / cycle.castProcs) >= combustionLeft + eps) {
                        unexplained += time + duration - combustionTime - (state.combustionDuration * cycle.castTime / cycle.castProcs);
                        if (timing != null) timing.append("WARNING: Combustion duration too long!").append("\r\n");
                    }
                    combustionLeft -= duration / (state.combustionDuration * cycle.castTime / cycle.castProcs);
                } else if (cycle != null && duration > 0 && combustionLeft > eps) {
                    //unexplained += Math.Min(duration, 15 - (time - apTime));
                    combustionTime = -1;
                    combustionLeft = 0;
                    combustionActive = false;
                    if (timing != null) timing.append("INFO: Combustion is still up!").append("\r\n");
                } else if (duration > 0) // do not cancel for gems/pots
                {
                    combustionTime = -1;
                    combustionLeft = 0;
                    combustionActive = false;
                }
            } else {
                if (state != null && state.combustion) {
                    if (combustionCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !combustionWarning) timing.append("WARNING: Combustion cooldown not ready!").append("\r\n");
                        combustionWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Combustion (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        combustionLeft = 1;
                        combustionCooldown = SequenceItem.calculations.combustionCooldown + (state.combustionDuration * cycle.castTime / cycle.castProcs);
                        combustionTime = time;
                        combustionWarning = false;
                        combustionLeft -= duration / (state.combustionDuration * cycle.castTime / cycle.castProcs);
                        combustionActive = true;
                    }
                }
            }
            // Arcane Power
            if (apActive) {
                if (state != null && state.arcanePower) {
                    if (time + duration > apTime + SequenceItem.calculations.arcanePowerDuration + eps) {
                        unexplained += time + duration - apTime - SequenceItem.calculations.arcanePowerDuration;
                        if (timing != null) timing.append("WARNING: Arcane Power duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && SequenceItem.calculations.arcanePowerDuration - (time - apTime) > eps) {
                    //unexplained += Math.Min(duration, SequenceItem.Calculations.ArcanePowerDuration - (time - apTime));
                    if (timing != null) timing.append("INFO: Arcane Power is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.arcanePower) {
                    if (apCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !apWarning) timing.append("WARNING: Arcane Power cooldown not ready!").append("\r\n");
                        apWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Arcane Power (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        apCooldown = SequenceItem.calculations.arcanePowerCooldown;
                        apTime = time;
                        apWarning = false;
                        apActive = true;
                    }
                }
            }
            // Power Infusion
            if (piActive) {
                if (state != null && state.powerInfusion) {
                    if (time + duration > piTime + SequenceItem.calculations.powerInfusionDuration + eps) {
                        unexplained += time + duration - piTime - SequenceItem.calculations.powerInfusionDuration;
                        if (timing != null) timing.append("WARNING: Power Infusion duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && SequenceItem.calculations.powerInfusionDuration - (time - piTime) > eps) {
                    //unexplained += Math.Min(duration, SequenceItem.Calculations.ArcanePowerDuration - (time - apTime));
                    if (timing != null) timing.append("INFO: Power Infusion is still up!").append("\r\n");
                }
            } else {
                if (state != null && state.powerInfusion) {
                    if (piCooldown > eps) {
                        unexplained += duration;
                        if (timing != null && !piWarning) timing.append("WARNING: Power Infusion cooldown not ready!").append("\r\n");
                        apWarning = true;
                    } else {
                        if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Power Infusion (" + formatRounded(manabefore) + " mana)").append("\r\n");
                        piCooldown = SequenceItem.calculations.powerInfusionCooldown;
                        piTime = time;
                        piWarning = false;
                        piActive = true;
                    }
                }
            }
            // Icy Veins
            if (!ivActive && state != null && state.icyVeins) {
                if (ivCooldown > eps && coldsnap) {
                    // we need to coldsnap somewhere from [ivTime] and [time]
                    double restrictedColdsnapMin = Math.max(coldsnapTimeMin, ivTime);
                    double restrictedColdsnapMax = Math.min(coldsnapTimeMax, time);
                    if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                        // we can reuse last coldsnap
                        coldsnapTimeMin = restrictedColdsnapMin;
                        coldsnapTimeMax = restrictedColdsnapMax;
                        ivCooldown = 0.0;
                    } else if (coldsnapCooldownDuration - (time - coldsnapTimeMin) <= eps) {
                        // coldsnap is ready
                        coldsnapTimeMin = Math.max(coldsnapTimeMin + coldsnapCooldownDuration, ivTime);
                        coldsnapTimeMax = time;
                        ivCooldown = 0.0;
                    }
                }
                if (ivCooldown > eps) {
                    unexplained += duration;
                    if (timing != null && !ivWarning) timing.append("WARNING: Icy Veins cooldown not ready!").append("\r\n");
                    ivWarning = true;
                } else {
                    if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Icy Veins (" + formatRounded(manabefore) + " mana)").append("\r\n");
                    ivCooldown = SequenceItem.calculations.icyVeinsCooldown;
                    ivTime = time;
                    ivWarning = false;
                    ivActive = true;
                }
            }
            if (ivActive) {
                if (state != null && state.icyVeins) {
                    if (time + duration > ivTime + 20 + eps) {
                        if (coldsnap) {
                            // we need to coldsnap somewhere from [ivTime] and [ivTime + 20]
                            double restrictedColdsnapMin = Math.max(coldsnapTimeMin, ivTime);
                            double restrictedColdsnapMax = Math.min(coldsnapTimeMax, ivTime + 20.0);
                            if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                                // we can reuse last coldsnap
                                coldsnapTimeMin = restrictedColdsnapMin;
                                coldsnapTimeMax = restrictedColdsnapMax;
                                ivTime += 20.0;
                                ivCooldown += 20.0;
                            } else if (coldsnapCooldownDuration + coldsnapTimeMin <= ivTime + 20 + eps) {
                                coldsnapTimeMin = Math.max(coldsnapTimeMin + coldsnapCooldownDuration, ivTime);
                                coldsnapTimeMax = ivTime + 20.0;
                                ivTime += 20.0;
                                ivCooldown += 20.0;
                            }
                        }
                        if (time + duration > ivTime + 20 + eps) {
                            unexplained += time + duration - ivTime - 20;
                            if (timing != null) timing.append("WARNING: Icy Veins duration too long!").append("\r\n");
                        }
                    }
                } else if (duration > 0 && 20 - (time - ivTime) > eps) {
                    //unexplained += Math.Min(duration, 20 - (time - ivTime));
                    if (timing != null) timing.append("INFO: Icy Veins is still up!").append("\r\n");
                }
            }
            // Water Elemental
            if (!weActive && state != null && state.waterElemental) {
                if (weCooldown > eps && coldsnap) {
                    // we need to coldsnap somewhere from [weTime] and [time]
                    double restrictedColdsnapMin = Math.max(coldsnapTimeMin, weTime);
                    double restrictedColdsnapMax = Math.min(coldsnapTimeMax, time);
                    if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                        // we can reuse last coldsnap
                        coldsnapTimeMin = restrictedColdsnapMin;
                        coldsnapTimeMax = restrictedColdsnapMax;
                        weCooldown = 0.0;
                    } else if (coldsnapCooldownDuration - (time - coldsnapTimeMin) <= eps) {
                        // coldsnap is ready
                        coldsnapTimeMin = Math.max(coldsnapTimeMin + coldsnapCooldownDuration, weTime);
                        coldsnapTimeMax = time;
                        weCooldown = 0.0;
                    }
                }
                if (weCooldown > eps) {
                    unexplained += duration;
                    if (timing != null && !weWarning) timing.append("WARNING: Water Elemental cooldown not ready!").append("\r\n");
                    weWarning = true;
                } else {
                    if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Water Elemental (" + formatRounded(manabefore) + " mana)").append("\r\n");
                    weCooldown = SequenceItem.calculations.waterElementalCooldown;
                    weTime = time;
                    weWarning = false;
                    weActive = true;
                }
            }
            if (weActive) {
                if (state != null && state.waterElemental) {
                    if (time + duration > weTime + SequenceItem.calculations.waterElementalDuration + eps) {
                        if (coldsnap) {
                            // we need to coldsnap somewhere from [weTime] and [weTime + 45]
                            double restrictedColdsnapMin = Math.max(coldsnapTimeMin, weTime);
                            double restrictedColdsnapMax = Math.min(coldsnapTimeMax, weTime + SequenceItem.calculations.waterElementalDuration);
                            if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                                // we can reuse last coldsnap
                                coldsnapTimeMin = restrictedColdsnapMin;
                                coldsnapTimeMax = restrictedColdsnapMax;
                                weTime += SequenceItem.calculations.waterElementalDuration;
                                weCooldown += SequenceItem.calculations.waterElementalDuration;
                            } else if (coldsnapCooldownDuration - (time - coldsnapTimeMin) <= (weTime + SequenceItem.calculations.waterElementalDuration - time) + eps) {
                                coldsnapTimeMin = Math.max(coldsnapTimeMin + coldsnapCooldownDuration, weTime);
                                coldsnapTimeMax = weTime + SequenceItem.calculations.waterElementalDuration;
                                weTime += SequenceItem.calculations.waterElementalDuration;
                                weCooldown += SequenceItem.calculations.waterElementalDuration;
                            }
                        }
                        if (time + duration > weTime + SequenceItem.calculations.waterElementalDuration + eps) {
                            unexplained += time + duration - weTime - SequenceItem.calculations.waterElementalDuration;
                            if (timing != null) timing.append("WARNING: Water Elemental duration too long!").append("\r\n");
                        }
                    }
                } else if (duration > 0 && SequenceItem.calculations.waterElementalDuration - (time - weTime) > eps) {
                    //unexplained += Math.Min(duration, 45 - (time - weTime));
                    if (timing != null) timing.append("INFO: Water Elemental is still up!").append("\r\n");
                }
            }
            // Mirror Image
            if (!miActive && state != null && state.mirrorImage) {
                if (miCooldown > eps) {
                    unexplained += duration;
                    if (timing != null && !miWarning) timing.append("WARNING: Mirror Image cooldown not ready!").append("\r\n");
                    miWarning = true;
                } else {
                    if (timing != null && reportMode == ReportMode.Listing) timing.append(timeFormat(time) + ": Mirror Image (" + formatRounded(manabefore) + " mana)").append("\r\n");
                    miCooldown = SequenceItem.calculations.mirrorImageCooldown;
                    miTime = time;
                    miWarning = false;
                    miActive = true;
                }
            }
            if (miActive) {
                if (state != null && state.mirrorImage) {
                    if (time + duration > miTime + SequenceItem.calculations.mirrorImageDuration + eps) {
                        unexplained += time + duration - miTime - SequenceItem.calculations.mirrorImageDuration;
                        if (timing != null) timing.append("WARNING: Mirror Image duration too long!").append("\r\n");
                    }
                } else if (duration > 0 && SequenceItem.calculations.mirrorImageDuration - (time - miTime) > eps) {
                    //unexplained += Math.Min(duration, 45 - (time - weTime));
                    if (timing != null) timing.append("INFO: Mirror Image is still up!").append("\r\n");
                }
            }
            // Move time forward
            if (mode == EvaluationMode.CooldownBreak) {
                double aftertime = data[0];
                if (aftertime >= time && aftertime <= time + duration) {
                    if (!berserkingActive && !flameCapActive && !potionOfWildMagicActive && !heroismActive && !moltenFuryActive && !combustionActive && !apActive && !ivActive && !manaGemEffectActive) {
                        boolean valid = true;
                        for (CooldownData d : itemData) {
                            if (d.active) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            return aftertime;
                        }
                    }
                }
                if (time >= aftertime && !cooldownContinuation) {
                    return time;
                }
            }
            String label = null;
            if (type == VariableType.IdleRegen) {
                label = "Idle Regen";
            } else if (type == VariableType.Wand) {
                label = "Wand";
            } else if (type == VariableType.SummonWaterElemental) {
                label = "Summon";
            } else if (type == VariableType.SummonMirrorImage) {
                label = "Summon";
            } else if (type == VariableType.Drinking) {
                label = "Drink";
            } else if (type == VariableType.TimeExtension) {
                label = "End";
            } else if (type == VariableType.AfterFightRegen) {
                label = "Drink";
            } else if (type == VariableType.ConjureManaGem) {
                label = "Conjure Mana Gem";
            } else if (type == VariableType.Spell) {
                label = cycle.name;
            }
            if (reportMode == ReportMode.Listing) {
                if (timing != null && label != null && (i == 0 || cycle == null || sequence.get(i).getCastingState() != sequence.get(i - 1).getCastingState() || sequence.get(i).getCycle() != sequence.get(i - 1).getCycle())) timing.append(timeFormat(time) + ": " + label + " (" + formatRounded(manabefore) + " mana)").append("\r\n");
            } else if (reportMode == ReportMode.Compact) {
                if (timing != null && label != null && (i == 0 || sequence.get(i).getVariableType() != sequence.get(i - 1).getVariableType() || sequence.get(i).getCastingState() != sequence.get(i - 1).getCastingState() || sequence.get(i).getCycle() != sequence.get(i - 1).getCycle())) {
                    timing.append(timeFormat(time) + ": " + (((state != null && state.getBuffLabel() != null && !state.getBuffLabel().isEmpty()) ? (state.getBuffLabel() + "+") : "") ) + label + " (" + formatRounded(manabefore) + " mana)").append("\r\n");
                }
            }
            time += duration;
            apCooldown -= duration;
            piCooldown -= duration;
            ivCooldown -= duration;
            weCooldown -= duration;
            miCooldown -= duration;
            potionCooldown -= duration;
            gemCooldown -= duration;
            flameCapCooldown -= duration;
            for (CooldownData d : itemData) {
                d.cooldown -= duration;
            }
            combustionCooldown -= duration;
            berserkingCooldown -= duration;
            evocationCooldown -= duration;
            if (apActive && SequenceItem.calculations.arcanePowerDuration - (time - apTime) <= eps) apActive = false;
            if (piActive && SequenceItem.calculations.powerInfusionDuration - (time - piTime) <= eps) piActive = false;
            if (ivActive && 20 - (time - ivTime) <= eps) ivActive = false;
            if (weActive && SequenceItem.calculations.waterElementalDuration - (time - weTime) <= eps) weActive = false;
            if (miActive && SequenceItem.calculations.mirrorImageDuration - (time - miTime) <= eps) miActive = false;
            if (heroismActive && 40 - (time - heroismTime) <= eps) heroismActive = false;
            if (potionOfWildMagicActive && 15 - (time - potionOfWildMagicTime) <= eps) potionOfWildMagicActive = false;
            if (potionOfSpeedActive && 15 - (time - potionOfSpeedTime) <= eps) potionOfSpeedActive = false;
            if (flameCapActive && 60 - (time - flameCapTime) <= eps) flameCapActive = false;
            for (CooldownData d : itemData) {
                if (d.active && d.effect.duration - (time - d.time) <= eps) d.active = false;
            }
            if (berserkingActive && 10 - (time - berserkingTime) <= 0) berserkingActive = false;
            if (manaGemEffectActive && SequenceItem.calculations.manaGemEffectDuration - (time - manaGemEffectTime) <= eps) manaGemEffectActive = false;
        }
        if (timing != null && unexplained > 0) {
            timing.append("\r\n");
            timing.append(String.format(Locale.ROOT, "Score: %.2f", Math.max(0, 100 - 100 * unexplained / getFightDuration()))).append("\r\n");
        }

        switch (mode) {
            case Unexplained:
                return unexplained;
            case ManaBelow:
                return getFightDuration();
            case CooldownBreak:
                return getFightDuration();
        }
        return unexplained;
    }
}
