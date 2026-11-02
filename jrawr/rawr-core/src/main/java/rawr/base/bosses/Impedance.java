// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.util.NetFormat;

/**
 * One movement or control effect the boss imposes on the player: a forced move, stun, fear, root
 * or disarm.
 *
 * <p>Models translate these into lost playing time: a moving, stunned or feared player is not
 * casting or attacking, so the effect duration is subtracted from the fight. Fields:</p>
 *
 * <ul>
 *   <li>{@link #frequency} &mdash; seconds between occurrences of the effect. A negative value
 *       marks "never happens".</li>
 *   <li>{@link #duration} &mdash; milliseconds the effect lasts; {@code 5000} is five seconds.
 *       (Note the unit difference from {@link Attack#attackSpeed}, which is seconds.)</li>
 *   <li>{@link #chance} &mdash; probability in {@code [0, 1]} that an occurrence affects the
 *       modelled player; many effects hit only some raid members.</li>
 *   <li>{@link #breakable} &mdash; whether the player can reduce or break the effect's duration
 *       (for example a movement effect shortened by a movement-speed bonus or a charge-like
 *       ability). A scripted stun that cannot be broken has this false. Models still charge the
 *       full duration for lost time; the flag is stored and shown in the UI.</li>
 * </ul>
 *
 * <p>The default constructor describes an effect every 2 minutes lasting 5 seconds at full
 * chance. The copy constructor creates an independent entry with the same values (the original
 * used it to avoid sharing entries between boss variants); {@link #toString()} is the compact
 * display form.</p>
 */
public class Impedance {
    public float frequency;
    public float duration;
    public float chance;
    public boolean breakable;

    public Impedance() {
        frequency = 120f;
        duration = 5000f;
        chance = 1.00f;
        breakable = true;
    }

    public Impedance(float frequency, float duration, float chance, boolean breakable) {
        this.frequency = frequency;
        this.duration = duration;
        this.chance = chance;
        this.breakable = breakable;
    }

    public Impedance(Impedance i) {
        frequency = i.frequency;
        duration = i.duration;
        chance = i.chance;
        breakable = i.breakable;
    }

    @Override
    public String toString() {
        if (frequency < 0) return "None";
        return "F: " + NetFormat.fixed(frequency, 0) + "s D: " + NetFormat.fixed(duration, 0) + "ms C: "
                + NetFormat.percent(chance, 1) + (breakable ? " : B" : "");
    }
}
