// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Sartharion in The Obsidian Sanctum (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus the "Fire Breath" area hit. A wave forced move occurs every 45 seconds
 * for 10 seconds. Nineteen-minute enrage and three-minute speed-kill timers; 95% back-attack
 * availability. The three drakes are separate encounter classes, not part of this one.</p>
 */
public class Sartharion extends MultiDiffBoss {
    public Sartharion() {
        setName("Sartharion");
        setInstance("The Obsidian Sanctum");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2510100f, 7669750f, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack breath = new Attack();
            breath.name = "Fire Breath";
            breath.damageType = ItemDamageType.Fire;
            breath.setDamagePerHit(new float[] { (8750f + 11250f) / 2f, (10938f + 14062f) / 2f }[i]);
            breath.maxNumTargets = 1;
            breath.attackSpeed = 40.0f;
            breath.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(breath);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance wave = new Impedance();
            wave.frequency = 45f;
            wave.duration = 10f * 1000f;
            wave.chance = 1f;
            wave.breakable = false;
            get(i).moves.add(wave);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
