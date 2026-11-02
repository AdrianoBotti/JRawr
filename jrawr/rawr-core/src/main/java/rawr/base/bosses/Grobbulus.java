// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Grobbulus in Naxxramas (tier 7; 10- and 25-player normal variants
 * only).
 *
 * <p>Standard melee plus a slime add group every 15 seconds for 5 seconds. Two forced-movement
 * effects are modelled: a 1-second kite move every 8 seconds and an injection every 20 seconds
 * whose chance depends on raid size. The enrage timer is 12 minutes in 10-player and 9 minutes in
 * 25-player.</p>
 */
public class Grobbulus extends MultiDiffBoss {
    public Grobbulus() {
        setName("Grobbulus");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2928000f, 9552325, 0, 0 });
        setBerserkTimer(new int[] { 12 * 60, 9 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup slimes = new TargetGroup();
            slimes.frequency = 15;
            slimes.duration = 5 * 1000;
            slimes.chance = 1.00f;
            slimes.numTargs = 1;
            slimes.nearBoss = false;
            get(i).targets.add(slimes);
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
            Impedance kite = new Impedance();
            kite.frequency = 8f;
            kite.duration = 1f * 1000f;
            kite.chance = 1f;
            kite.breakable = false;
            get(i).moves.add(kite);
            Impedance injection = new Impedance();
            injection.frequency = 20f;
            injection.duration = (10f + (4f + 5f) / 2f) * 1000f;
            injection.chance = 1f / (get(i).getMaxPlayers() - 1f);
            injection.breakable = false;
            get(i).moves.add(injection);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
