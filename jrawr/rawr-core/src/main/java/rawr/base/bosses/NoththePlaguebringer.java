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
 * Encounter definition for Noth the Plaguebringer in Naxxramas (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Two phases: the boss is invulnerable during the second phase. Plague warrior adds appear in
 * phase 1 and champion adds in phase 2, both on 30-second cycles. Attacks modelled: standard melee
 * and the "Curse of the Plaguebringer" ranged hit; a blink move is added while the boss is
 * attackable. The enrage timer is three times the 180-second phase pair.</p>
 */
public class NoththePlaguebringer extends MultiDiffBoss {
    public NoththePlaguebringer() {
        setName("Noth the Plaguebringer");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2789000f, 8436725f, 0, 0 });
        float[] phase1Length = { 110, 110, 0, 0 };
        float[] phase2Length = { 70, 70, 0, 0 };
        setBerserkTimer(new int[] { ((int) phase1Length[0] + (int) phase2Length[0]) * 3, ((int) phase1Length[1] + (int) phase2Length[1]) * 3, 0, 0 });
        setSpeedKillTimer(new int[] { (int) phase1Length[0], (int) phase1Length[1], 0, 0 });
        float[] percDurInPhase1 = { ((getBerserkTimer()[0] / phase1Length[0]) * phase2Length[0]) / getBerserkTimer()[0], ((getBerserkTimer()[1] / phase1Length[1]) * phase2Length[1]) / getBerserkTimer()[1] };
        float[] percDurInPhase2 = { 1f - percDurInPhase1[0], 1f - percDurInPhase1[1] };
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup warriors = new TargetGroup();
            warriors.frequency = 30;
            warriors.chance = percDurInPhase1[i];
            warriors.duration = 20 * 1000;
            warriors.numTargs = new int[] { 2, 3 }[i];
            warriors.nearBoss = false;
            get(i).targets.add(warriors);
            TargetGroup champions = new TargetGroup();
            champions.frequency = 30;
            champions.chance = percDurInPhase2[i];
            champions.duration = 20 * 1000;
            champions.numTargs = new int[] { 2, 4 }[i];
            champions.nearBoss = false;
            get(i).targets.add(champions);
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack a = new Attack();
            a.name = "Curse of the Plaguebringer";
            a.attackSpeed = 45;
            a.attackType = AttackTypes.AT_RANGED;
            a.setDamagePerHit(new float[] { (3700f + 4300f), (5550f + 6450f) }[i] / 2f);
            a.damageType = ItemDamageType.Shadow;
            a.maxNumTargets = new float[] { 3, 10 }[i];
            a.missable = false;
            a.dodgable = false;
            a.parryable = false;
            a.blockable = false;
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
            Impedance blink = new Impedance();
            blink.frequency = (20f + 30f) / 2f;
            blink.duration = 2 * 1000f;
            blink.chance = percDurInPhase1[i];
            blink.breakable = true;
            get(i).moves.add(blink);
        }
        setTimeBossIsInvuln(new float[] { percDurInPhase2[0], percDurInPhase2[1], 0, 0 });
    }
}
