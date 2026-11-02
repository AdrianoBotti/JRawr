// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.BossHandler;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Valathria Dreamwalker in Icecrown Citadel (tier 10; all four variants).
 * The name keeps the original's spelling on purpose; it is part of the identifier used by save
 * files.
 *
 * <p>This definition carries identity, health and composition data only: no attacks, adds,
 * movement or resistances are defined (the constructor's per-variant loop is empty) and health is
 * written as half of the underlying values. Seven-minute enrage timer and 180- or 240-second
 * speed-kill timers.</p>
 */
public class ValathriaDreamwalker extends MultiDiffBoss {
    public ValathriaDreamwalker() {
        setName("Valathria Dreamwalker");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 12000007f / 2f, 36002816f / 2f, 12000007f / 2f, 35999996f / 2f });
        setBerserkTimer(new int[] { 7 * 60, 7 * 60, 7 * 60, 7 * 60 });
        setSpeedKillTimer(new int[] { 180, 240, 240, 240 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 3 });
        setMinHealers(new int[] { 3, 5, 3, 6 });
        for (int i = 0; i < 4; i++) {
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
