// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Thaddius in Naxxramas (tier 7; 10- and 25-player normal variants only).
 *
 * <p>Two phases: health is the sum of both phases. Attacks modelled: standard melee, "Chain
 * Lightning" and "Static Field". Polarity swaps are a 3-second move on a 30-second cycle during
 * phase 2, and a final run move is added at the end. Back-attack availability is weighted by the
 * phase lengths (100% in phase 1, 50% in phase 2). Seven-minute enrage timer.</p>
 */
public class Thaddius extends MultiDiffBoss {
    public Thaddius() {
        setName("Thaddius");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 3850000f + 838300f, 3834875 + 838300f, 0, 0 });
        float[] phase1Length = { 1 * 60, 1 * 60, 0, 0 };
        float[] phase2Length = { 6 * 60, 6 * 60, 0, 0 };
        setBerserkTimer(new int[] { 7 * 60, 7 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        float[] percDurInPhase1 = { ((getBerserkTimer()[0] / phase1Length[0]) * phase2Length[0]) / getBerserkTimer()[0], ((getBerserkTimer()[1] / phase1Length[1]) * phase2Length[1]) / getBerserkTimer()[1] };
        float[] percDurInPhase2 = { 1f - percDurInPhase1[0], 1f - percDurInPhase1[1] };
        setInBackPercMelee(new double[] { 1.00f * percDurInPhase1[0] + 0.50f * percDurInPhase2[0], 1.00f * percDurInPhase1[1] + 0.50f * percDurInPhase2[1], 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack chain = new Attack();
            chain.name = "Chain Lightning (Thaddius)";
            chain.damageType = ItemDamageType.Nature;
            chain.setDamagePerHit(new float[] { (3600f + 4400f) / 2f, (6938f + 8062f) / 2f }[i]);
            chain.maxNumTargets = 5f;
            chain.attackSpeed = 15.0f;
            chain.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(chain);
            Attack field = new Attack();
            field.name = "Static Field (Feugen)";
            field.damageType = ItemDamageType.Nature;
            field.setDamagePerHit(new float[] { 2500f, 3500f }[i]);
            field.maxNumTargets = 5f;
            field.attackSpeed = 15.0f;
            field.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(field);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance polarity = new Impedance();
            polarity.frequency = 30f;
            polarity.duration = 3f * 1000f;
            polarity.chance = percDurInPhase2[i] * 0.50f;
            polarity.breakable = true;
            get(i).moves.add(polarity);
            Impedance run = new Impedance();
            run.frequency = get(i).getBerserkTimer() - 1;
            run.duration = 5f * 1000f;
            run.chance = 1.00f;
            run.breakable = true;
            get(i).moves.add(run);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
