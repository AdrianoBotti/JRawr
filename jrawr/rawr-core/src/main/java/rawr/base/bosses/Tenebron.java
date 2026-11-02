// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Tenebron in The Obsidian Sanctum (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus "Shadow Fissure" and "Shadow Breath", a six-target add group every 40
 * seconds for 16 seconds, and two forced moves (the add run and the fissure). Nineteen-minute
 * enrage and three-minute speed-kill timers; 95% back-attack availability.</p>
 */
public class Tenebron extends MultiDiffBoss {
    public Tenebron() {
        setName("Tenebron");
        setInstance("The Obsidian Sanctum");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 976150f, 2231200, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack fissure = new Attack();
            fissure.name = "Shadow Fissure";
            fissure.damageType = ItemDamageType.Shadow;
            fissure.setDamagePerHit(new float[] { (6188f + 8812f) / 2f, (9488f + 13512f) / 2f }[i]);
            fissure.maxNumTargets = 1;
            fissure.attackSpeed = 40.0f;
            fissure.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(fissure);
            Attack breath = new Attack();
            breath.name = "Shadow Breath";
            breath.damageType = ItemDamageType.Shadow;
            breath.setDamagePerHit(new float[] { (6938f + 8062f) / 2f, (8788f + 10212f) / 2f }[i]);
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
            {
                TargetGroup adds = new TargetGroup();
                adds.frequency = 40;
                adds.chance = 1.00f;
                adds.duration = 16 * 1000;
                adds.numTargs = 6;
                adds.nearBoss = false;
                get(i).targets.add(adds);
                Impedance move = new Impedance();
                move.frequency = 30f + 20f;
                move.duration = (2 + 16 + 2) * 1000f;
                move.chance = 1f;
                move.breakable = false;
                get(i).moves.add(move);
            }
            {
                Impedance move = new Impedance();
                move.frequency = get(i).getAttacks().get(1).attackSpeed;
                move.duration = (5f + 1f) * 1000f;
                move.chance = 1f / get(i).getMaxPlayers();
                move.breakable = false;
                get(i).moves.add(move);
            }
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
