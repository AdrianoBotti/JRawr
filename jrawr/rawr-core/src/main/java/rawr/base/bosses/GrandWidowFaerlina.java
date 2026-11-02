// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Grand Widow Faerlina in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus a Worshipper add group that appears once near the end of the fight for 20
 * seconds (10-player) or 30 seconds (25-player), a "Poison Bolt Volley" ranged damage-over-time
 * against three targets and "Rain of Fire", an area attack that also forces a 3-second move.
 * Nineteen-minute enrage timer.</p>
 */
public class GrandWidowFaerlina extends MultiDiffBoss {
    public GrandWidowFaerlina() {
        setName("Grand Widow Faerlina");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2231200f, 6763325f, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 3, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup worshippers = new TargetGroup();
            worshippers.frequency = get(i).getBerserkTimer() - 1;
            worshippers.chance = 1.00f;
            worshippers.nearBoss = false;
            worshippers.duration = new float[] { 4, 6 }[i] * 5 * 1000;
            worshippers.numTargs = 1;
            get(i).targets.add(worshippers);
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            DoT poisonBolt = new DoT();
            poisonBolt.name = "Poison Bolt Volley";
            poisonBolt.damageType = ItemDamageType.Nature;
            poisonBolt.setDamagePerHit(new float[] { (2625f + 3375f) / 2.0f, (3755f + 4125f) / 2.0f }[i]);
            poisonBolt.damagePerTick = new float[] { BossHandler.CalcADotTick(1480f, 1720f, 8f, 2f), BossHandler.CalcADotTick(1900f, 2100f, 8f, 2f) }[i];
            poisonBolt.tickInterval = 2f;
            poisonBolt.numTicks = 4;
            poisonBolt.maxNumTargets = 3;
            poisonBolt.attackSpeed = (7.0f + 15.0f) / 2.0f;
            poisonBolt.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(poisonBolt);
            {
                Attack a = new Attack();
                a.name = "Rain of Fire";
                a.damageType = ItemDamageType.Fire;
                a.setDamagePerHit(new float[] { BossHandler.CalcADotTick(1750f, 2750f, 6f, 2f), BossHandler.CalcADotTick(3700f, 4300f, 6f, 2f) }[i]);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = (6.0f + 18.0f) / 2.0f;
                a.attackType = AttackTypes.AT_AOE;
                get(i).getAttacks().add(a);
                Impedance move = new Impedance();
                move.frequency = a.attackSpeed;
                move.duration = 3f * 1000f;
                move.chance = 1f;
                move.breakable = false;
                get(i).moves.add(move);
            }
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
