// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Sapphiron in Naxxramas (tier 7; 10- and 25-player normal variants
 * only).
 *
 * <p>The only encounter here with a nonzero school resistance: 75% frost resistance. Attacks
 * modelled: standard melee, "Cleave" against up to ten targets, "Tail Sweep", the constant "Frost
 * Aura" area damage and a heavy "Life Drain" hit on two targets. An air phase forced move is
 * modelled every 75 seconds for 30 seconds at 90% chance. Fifteen-minute enrage timer.</p>
 */
public class Sapphiron extends MultiDiffBoss {
    public Sapphiron() {
        setName("Sapphiron");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 4183500, 13038575, 0, 0 });
        setBerserkTimer(new int[] { 15 * 60, 15 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 1, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack cleave = new Attack();
            cleave.name = "Cleave";
            cleave.damageType = ItemDamageType.Physical;
            cleave.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()] + 50);
            cleave.maxNumTargets = 10;
            cleave.attackSpeed = 10.0f;
            cleave.attackType = AttackTypes.AT_MELEE;
            get(i).getAttacks().add(cleave);
            Attack sweep = new Attack();
            sweep.name = "Tail Sweep";
            sweep.damageType = ItemDamageType.Physical;
            sweep.setDamagePerHit(new float[] { 1500f + 2500f, 2188f + 2812f }[i] / 2f);
            sweep.maxNumTargets = 10;
            sweep.attackSpeed = 10.0f;
            sweep.attackType = AttackTypes.AT_MELEE;
            get(i).getAttacks().add(sweep);
            Attack aura = new Attack();
            aura.name = "Frost Aura";
            aura.damageType = ItemDamageType.Frost;
            aura.setDamagePerHit(new float[] { 1200, 1600 }[i]);
            aura.maxNumTargets = get(i).getMaxPlayers();
            aura.attackSpeed = 2.0f;
            aura.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(aura);
            Attack drain = new Attack();
            drain.name = "Life Drain";
            drain.damageType = ItemDamageType.Shadow;
            drain.setDamagePerHit(new float[] { (((4376f + 5624f) / 2f) * 3f) * 4f, (((4376f + 5624f) / 2f) * 3f) * 4f }[i]);
            drain.maxNumTargets = 2;
            drain.attackSpeed = 24.0f;
            drain.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(drain);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.75f, 0.75f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance air = new Impedance();
            air.frequency = 45f + 30f;
            air.duration = 30f * 1000f;
            air.chance = 0.90f;
            air.breakable = false;
            get(i).moves.add(air);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
