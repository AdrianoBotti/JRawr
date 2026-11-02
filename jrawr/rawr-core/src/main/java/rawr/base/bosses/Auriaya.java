// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT8.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Auriaya in Ulduar (tier 8; 10- and 25-player normal variants only).
 *
 * <p>Standard melee plus a fear every 45 seconds lasting 5 seconds (flagged breakable). No adds or
 * resistances are modelled, and the target cannot be attacked from behind (0% back-attack
 * availability). Ten-minute enrage and three-minute speed-kill timers.</p>
 */
public class Auriaya extends MultiDiffBoss {
    public Auriaya() {
        setName("Auriaya");
        setInstance("Ulduar");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5, BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 3137625f, 16734000f, 0, 0 });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.00f, 0.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance fear = new Impedance();
            fear.frequency = 45f;
            fear.duration = 5f * 1000f;
            fear.chance = 1f;
            fear.breakable = true;
            get(i).fears.add(fear);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
