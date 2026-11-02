// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Kel'Thuzad in Naxxramas (tier 7; 10- and 25-player normal variants
 * only).
 *
 * <p>Two phases: the first lasts 228 seconds and the boss is not attackable for a share of the
 * fight (the phase share is written to the invulnerability-time field); the second covers the rest
 * of the 19-minute enrage timer. Attacks modelled: standard melee, single-target Frostbolt,
 * Frostbolt volley, Arcane Mana Detonation and "Frost Blast", a percentage-health hit that ignores
 * the main tank in 10-player. A fissure move is added for the second phase.</p>
 */
public class KelThuzad extends MultiDiffBoss {
    public KelThuzad() {
        setName("Kel'Thuzad");
        setInstance("Naxxramas");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 2230000f, 14000000f, 0, 0 });
        float[] phase1Length = { 3 * 60 + 48, 3 * 60 + 48, 0, 0 };
        float[] phase2Length = { 19 * 60 - phase1Length[0], 19 * 60 - phase1Length[1], 0, 0 };
        setBerserkTimer(new int[] { ((int) phase1Length[0] + (int) phase2Length[0]), ((int) phase1Length[1] + (int) phase2Length[1]), 0, 0 });
        setSpeedKillTimer(new int[] { (int) phase1Length[0] + 3 * 60, (int) phase1Length[1] + 3 * 60, 0, 0 });
        float[] percDurInPhase1 = { ((getBerserkTimer()[0] / phase1Length[0]) * phase2Length[0]) / getBerserkTimer()[0], ((getBerserkTimer()[1] / phase1Length[1]) * phase2Length[1]) / getBerserkTimer()[1] };
        float[] percDurInPhase2 = { 1f - percDurInPhase1[0], 1f - percDurInPhase1[1] };
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 3, 3, 0, 0 });
        setMinHealers(new int[] { 3, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack single = new Attack();
            single.name = "Frostbolt (Single)";
            single.damageType = ItemDamageType.Frost;
            single.setDamagePerHit(new float[] { (10063f + 12937f) / 2f, (29250f + 30750f) / 2f }[i]);
            single.maxNumTargets = 1;
            single.attackSpeed = 40.0f;
            single.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(single);
            Attack volley = new Attack();
            volley.name = "Frostbolt (Volley)";
            volley.damageType = ItemDamageType.Frost;
            volley.setDamagePerHit(new float[] { (4500f + 5500f) / 2f, (7200f + 8800f) / 2f }[i]);
            volley.maxNumTargets = get(i).getMaxPlayers();
            volley.attackSpeed = 40.0f;
            volley.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(volley);
            Attack detonation = new Attack();
            detonation.name = "Mana Detonation";
            detonation.damageType = ItemDamageType.Arcane;
            detonation.setDamagePerHit(new float[] { (10000f + 25000f), (12500f + 30000f) }[i] / 2f);
            detonation.maxNumTargets = 1;
            detonation.attackSpeed = 30.0f;
            detonation.attackType = AttackTypes.AT_RANGED;
            get(i).getAttacks().add(detonation);
            Attack blast = new Attack();
            blast.name = "Frost Blast";
            blast.damageType = ItemDamageType.Frost;
            blast.setDamagePerHit(1.04f);
            blast.damageIsPerc = true;
            blast.maxNumTargets = 1;
            blast.attackSpeed = 45.0f;
            blast.attackType = AttackTypes.AT_RANGED;
            blast.ignoresMTank = new boolean[] { true, false }[i];
            get(i).getAttacks().add(blast);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance fissure = new Impedance();
            fissure.frequency = (15f + 20f) / 2f;
            fissure.duration = 6f * 1000f;
            fissure.chance = 1f / get(i).getMaxPlayers() * percDurInPhase2[i];
            fissure.breakable = false;
            get(i).moves.add(fissure);
        }
        setTimeBossIsInvuln(new float[] { percDurInPhase1[0], percDurInPhase1[1], 0, 0 });
    }
}
