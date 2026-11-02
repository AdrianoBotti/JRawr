// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT8.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Emalon the Storm Watcher in the Vault of Archavon (tier 8; 10- and
 * 25-player normal variants only).
 *
 * <p>Standard melee only, with two forced-movement windows on the same 63-second cycle: one for
 * the add handling and one for the nova. Note the 10-player enrage timer is written as
 * {@code 6 * 6} (36 seconds), far shorter than the 25-player 19 minutes. Three-minute speed-kill
 * timer and full back-attack availability.</p>
 */
public class EmalonTheStormWatcher extends MultiDiffBoss {
    public EmalonTheStormWatcher() {
        setName("Emalon the Storm Watcher");
        setInstance("The Vault of Archavon");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5, BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2789000f, 11156000f, 0, 0 });
        setBerserkTimer(new int[] { 6 * 6, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
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
            Impedance add = new Impedance();
            add.frequency = 45f + 18f;
            add.duration = (18f + 5f + 5f) * 1000f;
            add.chance = 1f;
            add.breakable = false;
            get(i).moves.add(add);
            Impedance nova = new Impedance();
            nova.frequency = 45f + 18f;
            nova.duration = (4f + 4f) * 1000f;
            nova.chance = 1f;
            nova.breakable = false;
            get(i).moves.add(nova);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
