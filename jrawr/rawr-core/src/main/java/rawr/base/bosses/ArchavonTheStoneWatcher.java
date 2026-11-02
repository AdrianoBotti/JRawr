// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Archavon the Stone Watcher in the Vault of Archavon (tier 7; 10- and
 * 25-player normal variants only).
 *
 * <p>Standard melee plus one forced-movement effect: every 30 seconds a 5-second move that can
 * affect three raid members. Five-minute enrage and three-minute speed-kill timers; 75%
 * back-attack availability.</p>
 */
public class ArchavonTheStoneWatcher extends MultiDiffBoss {
    public ArchavonTheStoneWatcher() {
        setName("Archavon The Stone Watcher");
        setInstance("The Vault of Archavon");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2300925f, 9970675f, 0, 0 });
        setBerserkTimer(new int[] { 5 * 60, 5 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.75f, 0.75f, 0, 0 });
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
            Impedance jump = new Impedance();
            jump.frequency = 30f;
            jump.duration = 5f * 1000f;
            jump.chance = 3f / get(i).getMaxPlayers();
            jump.breakable = false;
            get(i).moves.add(jump);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
