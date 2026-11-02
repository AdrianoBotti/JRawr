// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Gothik the Harvester in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>A two-phase fight: during the 274-second first phase the boss is invulnerable while three add
 * groups (trainees, death knights and riders) arrive on their own timers; the boss is attackable
 * during the short second phase. The only attack modelled is the boss's Shadowbolt. The enrage
 * timer is 19 minutes minus the first phase (866 seconds); 95% back-attack availability.</p>
 */
public class GothiktheHarvester extends MultiDiffBoss {
    public GothiktheHarvester() {
        setName("Gothik the Harvester");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 836700f, 2510100f, 0, 0 });
        float[] phase1Length = { 274, 274, 0, 0 };
        float[] phase2Length = { 45, 45, 0, 0 };
        setBerserkTimer(new int[] { 19 * 60 - 274, 19 * 60 - 274, 0, 0 });
        setSpeedKillTimer(new int[] { (int) phase1Length[0] + (int) phase2Length[0], (int) phase1Length[0] + (int) phase2Length[0], 0, 0 });
        float[] percDurInPhase1 = { phase1Length[0] / getBerserkTimer()[0], phase1Length[1] / getBerserkTimer()[1] };
        float[] percDurInPhase2 = { 1f - percDurInPhase1[0], 1f - percDurInPhase1[1] };
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup trainees = new TargetGroup();
            trainees.frequency = phase1Length[i] / (24 / 2);
            trainees.duration = 10 * 1000;
            trainees.chance = percDurInPhase1[i];
            trainees.numTargs = 2;
            trainees.nearBoss = false;
            get(i).targets.add(trainees);
            TargetGroup deathKnights = new TargetGroup();
            deathKnights.frequency = phase1Length[i] / (7);
            deathKnights.duration = 10 * 1000;
            deathKnights.chance = 1.00f;
            deathKnights.numTargs = 1;
            deathKnights.nearBoss = false;
            get(i).targets.add(deathKnights);
            TargetGroup riders = new TargetGroup();
            riders.frequency = phase1Length[i] / (4);
            riders.duration = 10 * 1000;
            riders.chance = percDurInPhase1[i];
            riders.numTargs = 1;
            riders.nearBoss = false;
            get(i).targets.add(riders);
            Attack a = new Attack();
            a.name = "Shadowbolt";
            a.damageType = ItemDamageType.Shadow;
            a.setDamagePerHit(new float[] { (2880f + 3520f) / 2f, (4500f + 5500f) / 2f }[i]);
            a.maxNumTargets = 1;
            a.attackSpeed = 1.0f;
            a.attackType = AttackTypes.AT_MELEE;
            get(i).getAttacks().add(a);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        setTimeBossIsInvuln(new float[] { percDurInPhase1[0], percDurInPhase1[1], 0, 0 });
    }
}
