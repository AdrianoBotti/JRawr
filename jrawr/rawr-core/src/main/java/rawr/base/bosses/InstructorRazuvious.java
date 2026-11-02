// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Instructor Razuvious in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus "Disrupting Shout", a raid-wide physical area hit every 15 seconds, and
 * "Jagged Knife" against one ranged target. Nineteen-minute enrage timer and 95% back-attack
 * availability; no adds are modelled.</p>
 */
public class InstructorRazuvious extends MultiDiffBoss {
    public InstructorRazuvious() {
        setName("Instructor Razuvious");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 3349000f, 10110125, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 4, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack shout = new Attack();
            shout.name = "Disrupting Shout";
            shout.damageType = ItemDamageType.Physical;
            shout.setDamagePerHit(new float[] { (4275f + 4725f) / 2f, (7125f + 7825f) / 2f }[i]);
            shout.maxNumTargets = get(i).getMaxPlayers();
            shout.attackSpeed = 15.0f;
            shout.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(shout);
            Attack knife = new Attack();
            knife.name = "Jagged Knife";
            knife.damageType = ItemDamageType.Physical;
            knife.setDamagePerHit(new float[] { 5000 + (10000 / 5 * 5), 5000 + (10000 / 5 * 5) }[i]);
            knife.maxNumTargets = 1;
            knife.attackSpeed = 10.0f;
            knife.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(knife);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
