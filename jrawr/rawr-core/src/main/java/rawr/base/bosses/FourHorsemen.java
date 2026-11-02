// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for the Four Horsemen in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Health is four times a single horseman's health (781,000 each in 10-player, 2,370,650 each in
 * 25-player). Modelled as standard melee plus one signature attack per rider: Korth'azz's Meteor
 * (fire against eight targets), Rivendare's Unholy Shadow, Blaumeux's Shadow Bolt and Zeliek's
 * Holy Bolt. A move every 30 seconds at 40% chance covers the horseman switching. Nineteen-minute
 * enrage timer.</p>
 */
public class FourHorsemen extends MultiDiffBoss {
    public FourHorsemen() {
        setName("Four Horsemen");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 781000f * 4f, 2370650f * 4f, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.75f, 0.75f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 3, 3, 0, 0 });
        setMinHealers(new int[] { 3, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack meteor = new Attack();
            meteor.name = "Korth'azz's Meteor";
            meteor.damageType = ItemDamageType.Fire;
            meteor.setDamagePerHit(new float[] { (13775f + 15225f) / 2f, (13775f + 15225f) / 2f }[i]);
            meteor.maxNumTargets = 8;
            meteor.attackSpeed = 15.0f;
            meteor.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(meteor);
            Attack unholy = new Attack();
            unholy.name = "Rivendare's Unholy Shadow";
            unholy.damageType = ItemDamageType.Shadow;
            unholy.setDamagePerHit(new float[] { (2160f + 2640f) / 2f + (4800 / 2 * 4), (2160f + 2640f) / 2f + (4800 / 2 * 4) }[i]);
            unholy.maxNumTargets = 8;
            unholy.attackSpeed = 15.0f;
            unholy.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(unholy);
            Attack shadowBolt = new Attack();
            shadowBolt.name = "Blaumeux's Shadow Bolt";
            shadowBolt.damageType = ItemDamageType.Shadow;
            shadowBolt.setDamagePerHit(new float[] { (2357f + 2643f) / 2f, (2357f + 2643f) / 2f }[i]);
            shadowBolt.maxNumTargets = 1;
            shadowBolt.attackSpeed = 2.0f;
            shadowBolt.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(shadowBolt);
            Attack holyBolt = new Attack();
            holyBolt.name = "Zeliek's Holy Bolt";
            holyBolt.damageType = ItemDamageType.Holy;
            holyBolt.setDamagePerHit(new float[] { (2357f + 2643f) / 2f, (2357f + 2643f) / 2f }[i]);
            holyBolt.maxNumTargets = 1;
            holyBolt.attackSpeed = 2.0f;
            holyBolt.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(holyBolt);
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
            move.frequency = 30f;
            move.duration = 10f * 1000f;
            move.chance = 0.40f;
            move.breakable = false;
            get(i).moves.add(move);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
