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
 * Encounter definition for Maexxna in Naxxramas (tier 7; 10- and 25-player normal variants only).
 *
 * <p>An add group appears every 40 seconds for 8 seconds; "Web Wrap" hits one (10-player) or two
 * (25-player) targets, "Web Spray" is a raid-wide area hit with a 6-second stun and "Poison Shock"
 * is a single-target nature hit. Nineteen-minute enrage and three-minute speed-kill timers; the
 * target can be attacked from behind at all times.</p>
 */
public class Maexxna extends MultiDiffBoss {
    public Maexxna() {
        setName("Maexxna");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2510000f, 7600000f, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 1, 1, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup adds = new TargetGroup();
            adds.frequency = 40;
            adds.chance = 1.00f;
            adds.duration = 8 * 1000;
            adds.numTargs = 8;
            adds.nearBoss = true;
            get(i).targets.add(adds);
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            {
                Attack a = new Attack();
                a.name = "Web Wrap";
                a.damageType = ItemDamageType.Nature;
                a.setDamagePerHit(new float[] { (2925f + 3575f) / 2f, (2925f + 3575f) / 2f }[i]);
                a.maxNumTargets = new int[] { 1, 2 }[i];
                a.attackSpeed = 40.0f;
                a.attackType = AttackTypes.AT_RANGED;
                get(i).getAttacks().add(a);
                float initial = 20f;
                float freq = a.attackSpeed;
                float chance = 1f + a.maxNumTargets / (get(i).getMaxPlayers() - get(i).getMinTanks());
                Impedance stun = new Impedance();
                stun.frequency = freq * (get(i).getBerserkTimer() / (get(i).getBerserkTimer() - initial)) * chance;
                stun.duration = 5f * 1000f;
                stun.chance = 1f / (get(i).getMaxPlayers() - get(i).getMinTanks());
                stun.breakable = false;
                get(i).stuns.add(stun);
            }
            {
                Attack a = new Attack();
                a.name = "Web Spray";
                a.damageType = ItemDamageType.Nature;
                a.setDamagePerHit(new float[] { (2188f + 2812f) / 2f, (5225f + 5775f) / 2f }[i]);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = 40.0f;
                a.attackType = AttackTypes.AT_AOE;
                get(i).getAttacks().add(a);
                float initial = 0f;
                float freq = a.attackSpeed;
                float chance = 1f + a.maxNumTargets / get(i).getMaxPlayers();
                Impedance stun = new Impedance();
                stun.frequency = freq * (get(i).getBerserkTimer() / (get(i).getBerserkTimer() - initial));
                stun.duration = 6f * 1000f;
                stun.chance = 1f;
                stun.breakable = false;
                get(i).stuns.add(stun);
            }
            {
                Attack a = new Attack();
                a.name = "Poison Shock";
                a.damageType = ItemDamageType.Nature;
                a.setDamagePerHit(new float[] { (3500f + 4500f) / 2f, (4550f + 5850f) / 2f }[i]);
                a.maxNumTargets = 1;
                a.attackSpeed = 40.0f;
                a.attackType = AttackTypes.AT_MELEE;
                get(i).getAttacks().add(a);
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
