// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import rawr.base.bosses.BossHandler;
import rawr.base.util.NetFormat;

/**
 * One group of additional enemies an encounter can spawn or phase into ("adds"), as used by the
 * multi-target damage calculation of the models.
 *
 * <p>{@code frequency} is the average interval between occurrences in seconds; a value of zero or
 * less means the group never happens (what {@link #toString()} shows as "None"). {@code duration}
 * is how long the group stays active, in <b>milliseconds</b> (the constructor's default is
 * 20000). {@code chance} is the probability that an occurrence actually produces the group
 * (1.0 = every time), {@code numTargs} the number of simultaneous targets, and {@code nearBoss}
 * whether the group can be reached by melee without moving. The copy constructor duplicates every
 * field, so edits to a copy do not affect the original.</p>
 */
public class TargetGroup {
    public float frequency = -1;
    public float duration = 20 * 1000;
    public float chance = 1.00f;
    public float numTargs = 2;
    public boolean nearBoss = false;

    public TargetGroup() {
        frequency = -1;
        duration = 20 * 1000f;
        chance = 1.00f;
        numTargs = 2;
        nearBoss = false;
    }

    public TargetGroup(float frequency, float duration, float chance, float numTargs, boolean nearBoss) {
        this.frequency = frequency;
        this.duration = duration;
        this.chance = chance;
        this.numTargs = numTargs;
        this.nearBoss = nearBoss;
    }

    public TargetGroup(TargetGroup i) {
        frequency = i.frequency;
        duration = i.duration;
        chance = i.chance;
        numTargs = i.numTargs;
        nearBoss = i.nearBoss;
    }

    @Override
    public String toString() {
        if (frequency <= 0) return "None";
        return "#T: " + NetFormat.fixed(numTargs, 2) + " F: " + NetFormat.fixed(frequency, 1) + "s D: "
                + NetFormat.fixed(duration / 1000f, 2) + "s C: " + NetFormat.percent(chance, 0)
                + (nearBoss ? " : NB" : "");
    }
}
