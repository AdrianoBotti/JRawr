// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT9.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for the Northrend Beasts in the Trial of the Crusader (tier 9; all four
 * variants).
 *
 * <p>Health is written as the sum of four separate values. Only standard melee is modelled; the
 * encounter's special mechanics are not. The enrage timer is 19 minutes on normal and 10 minutes
 * on heroic, with a five-minute speed-kill timer.</p>
 */
public class NorthrendBeasts extends MultiDiffBoss {
    public NorthrendBeasts() {
        setName("Northrend Beasts");
        setInstance("Trial of the Crusader");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T9_0, BossHandler.TierLevels.T9_5, BossHandler.TierLevels.T9_0, BossHandler.TierLevels.T9_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { (2230000 + 1255050 + 1255050 + 3486250), (8924800 + 5020200 + 5020200 + 13247750), (2789000 + 1673400 + 1673400 + 4648300), (11853250 + 6693600 + 6693600 + 18128500) });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 10 * 60, 10 * 60 });
        setSpeedKillTimer(new int[] { 5 * 60, 5 * 60, 5 * 60, 5 * 60 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, .95f, .95f });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0.00f, 0.00f });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 2, 4, 2, 5 });
        for (int i = 0; i < 4; i++) {
            Attack a = new Attack();
            a.name = "Melee";
            a.damageType = ItemDamageType.Physical;
            a.attackType = AttackTypes.AT_MELEE;
            a.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            a.maxNumTargets = 1f;
            a.attackSpeed = 2f;
            a.dodgable = true;
            a.missable = true;
            a.parryable = true;
            a.blockable = true;
            a.ignoresMeleeDps = true;
            a.ignoresRangedDps = true;
            a.ignoresHealers = true;
            a.isTheDefaultMelee = true;
            get(i).getAttacks().add(a);
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
