// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Loatheb in Naxxramas (tier 7; 10- and 25-player normal variants only).
 *
 * <p>Standard melee plus "Deathbloom" (ranged, one target) and "Inevitable Doom" (ranged, ten
 * targets). A 10-second forced move occurs every 90 seconds. Five-minute enrage and three-minute
 * speed-kill timers; the target can be attacked from behind at all times.</p>
 */
public class Loatheb extends MultiDiffBoss {
    public Loatheb() {
        setName("Loatheb");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 6693600f, 20220250f, 0, 0 });
        setBerserkTimer(new int[] { 5 * 60, 5 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 1, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack deathbloom = new Attack();
            deathbloom.name = "Deathbloom";
            deathbloom.damageType = ItemDamageType.Nature;
            deathbloom.setDamagePerHit(new float[] { (200f / 1f * 6f) + (1200f), (400f / 1f * 6f) + (1500f) }[i]);
            deathbloom.maxNumTargets = 1;
            deathbloom.attackSpeed = 30.0f;
            deathbloom.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(deathbloom);
            Attack doom = new Attack();
            doom.name = "Inevitable Doom";
            doom.damageType = ItemDamageType.Shadow;
            doom.setDamagePerHit(new float[] { 4000f / 30f * 120f, 5000f / 30f * 120f }[i]);
            doom.maxNumTargets = 10;
            doom.attackSpeed = 120.0f;
            doom.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(doom);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance move = new Impedance();
            move.frequency = 90f;
            move.duration = 10f * 1000f;
            move.chance = 1f;
            move.breakable = false;
            get(i).moves.add(move);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
