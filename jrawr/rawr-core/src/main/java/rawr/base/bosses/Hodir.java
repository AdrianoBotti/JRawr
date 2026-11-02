// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT8.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Hodir in Ulduar (tier 8; 10- and 25-player normal variants only).
 *
 * <p>Standard melee only, no adds. A freeze root is modelled every 45 seconds for 10 seconds,
 * flagged breakable. Eight-minute enrage and three-minute speed-kill timers; 75% back-attack
 * availability.</p>
 */
public class Hodir extends MultiDiffBoss {
    public Hodir() {
        setName("Hodir");
        setInstance("Ulduar");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5, BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 8115990f, 32477904f, 0, 0 });
        setBerserkTimer(new int[] { 8 * 60, 8 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.75f, 0.75f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 1, 0, 0 });
        setMinHealers(new int[] { 3, 5, 0, 0 });
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
            Impedance freeze = new Impedance();
            freeze.frequency = 45f;
            freeze.duration = 10f * 1000f;
            freeze.chance = 1.00f;
            freeze.breakable = true;
            get(i).roots.add(freeze);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
