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
 * Encounter definition for Anub'Rekhan in Naxxramas (tier 7; 10- and 25-player normal variants
 * only).
 *
 * <p>Standard melee plus "Impale", an area attack every 40 seconds that hits the whole raid except
 * the main tank; each Impale occurrence reduces the modelled back-attack availability by a fixed
 * two seconds. Two add groups (1 and 2 targets in 10-player, 2 and 4 in 25-player) and a boss move
 * are modelled on the same ~100-second cycle.</p>
 */
public class AnubRekhan extends MultiDiffBoss {
    public AnubRekhan() {
        setName("Anub'Rekhan");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2231200, 6763325f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 3, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            {
                Attack a = new Attack();
                a.name = "Impale";
                a.damageType = ItemDamageType.Physical;
                a.setDamagePerHit(new float[] { (4813f + 6187f), (5688f + 7312f) }[i] / 2f);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = 40.0f;
                a.attackType = AttackTypes.AT_AOE;
                a.ignoresMTank = true;
                get(i).getAttacks().add(a);
                float time = (get(i).getBerserkTimer() / a.attackSpeed) * 2f;
                get(i).setInBackPercMelee(get(i).getInBackPercMelee() - time / get(i).getBerserkTimer());
                get(i).setInBackPercRanged(get(i).getInBackPercRanged() - time / get(i).getBerserkTimer());
            }
        }
        for (int i = 0; i < 2; i++) {
            Impedance m;
            get(i).moves.add(m = new Impedance());
            m.frequency = (80f + 120f) / 2f;
            m.duration = (16f + 4f) * 1000f;
            m.chance = 1.00f;
            m.breakable = false;
            TargetGroup tg1 = new TargetGroup();
            tg1.frequency = m.frequency;
            tg1.duration = 10 * 1000f;
            tg1.chance = 1.00f;
            tg1.nearBoss = false;
            tg1.numTargs = new float[] { 1, 2 }[i];
            get(i).targets.add(tg1);
            TargetGroup tg2 = new TargetGroup();
            tg2.frequency = m.frequency;
            tg2.duration = 8 * 1000f;
            tg2.chance = 1.00f;
            tg2.nearBoss = false;
            tg2.numTargs = new float[] { 2, 4 }[i];
            get(i).targets.add(tg2);
        }
    }
}
