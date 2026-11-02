// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Heigan the Unclean in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus the "Decrepit Fever" nature hit. A "dance" forced move is modelled every
 * 135 seconds lasting 25 seconds (part of the second phase). Back-attack availability is 0% in
 * 10-player and 25% in 25-player. Nineteen-minute enrage timer.</p>
 */
public class HeigantheUnclean extends MultiDiffBoss {
    public HeigantheUnclean() {
        setName("Heigan the Unclean");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 3067900f, 9273425f, 0, 0 });
        float[] phase1Length = { 90, 90, 0, 0 };
        float[] phase2Length = { 45, 45, 0, 0 };
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        float[] percDurInPhase1 = { ((getBerserkTimer()[0] / phase1Length[0]) * phase2Length[0]) / getBerserkTimer()[0], ((getBerserkTimer()[1] / phase1Length[1]) * phase2Length[1]) / getBerserkTimer()[1] };
        float[] percDurInPhase2 = { 1f - percDurInPhase1[0], 1f - percDurInPhase1[1] };
        setInBackPercMelee(new double[] { 0.00f, 0.25f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 1, 0, 0 });
        setMinHealers(new int[] { 3, 5, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack a = new Attack();
            a.name = "Decrepit Fever";
            a.damageType = ItemDamageType.Nature;
            a.setDamagePerHit(new float[] { 3000f / 3f * 21f, 4500f / 3f * 21f }[i]);
            a.maxNumTargets = 1;
            a.attackSpeed = 30.0f;
            a.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(a);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance dance = new Impedance();
            dance.frequency = 90f + 45f;
            dance.duration = (45f - 4f * 5f) * 1000f;
            dance.chance = new float[] { percDurInPhase2[i], 1.00f }[i];
            dance.breakable = false;
            get(i).moves.add(dance);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
