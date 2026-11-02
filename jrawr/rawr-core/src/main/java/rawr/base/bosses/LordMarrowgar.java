// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Lord Marrowgar in Icecrown Citadel (tier 10; all four variants).
 *
 * <p>Standard melee plus "Saber Lash" against the tanks, the "Coldflame" frost damage-over-time
 * area, "Bone Spike Graveyard" (a target group, a stun and percentage damage that ignores the
 * tanks) and the 20- to 30-second "Bone Storm" with a forced move. Back-attack availability is
 * 70/90 in 10-player and 60/90 in 25-player. Ten-minute enrage timer.</p>
 */
public class LordMarrowgar extends MultiDiffBoss {
    public LordMarrowgar() {
        setName("Lord Marrowgar");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 6972500f, 23706500f, 10458750f, 31376250f });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 10 * 60, 10 * 60 });
        setSpeedKillTimer(new int[] { 120, 180, 120, 150 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 3, 2, 3 });
        setMinHealers(new int[] { 2, 5, 3, 5 });
        setInBackPercMelee(new double[] { 70 / 90, 70 / 90, 60 / 90, 60 / 90 });
        setInBackPercRanged(new double[] { 70 / 90, 70 / 90, 60 / 90, 60 / 90 });
        for (int i = 0; i < 4; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = new float[] { (90 / (70 / 2)), (90 / (70 / 2)), (90 / (60 / 2)), (90 / (60 / 2)) }[i];
            melee.dodgable = true;
            melee.missable = true;
            melee.parryable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
            Attack saber = new Attack();
            saber.name = "Saber Lash";
            saber.damageType = ItemDamageType.Physical;
            saber.attackType = AttackTypes.AT_MELEE;
            saber.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            saber.maxNumTargets = get(i).getMinTanks();
            saber.attackSpeed = new float[] { (90 / (70 / 2)), (90 / (70 / 2)), (90 / (60 / 2)), (90 / (60 / 2)) }[i];
            saber.dodgable = true;
            saber.missable = true;
            saber.parryable = true;
            saber.blockable = true;
            saber.ignoresMeleeDps = true;
            saber.ignoresRangedDps = true;
            saber.ignoresHealers = true;
            get(i).getAttacks().add(saber);
            DoT coldflame = new DoT();
            coldflame.name = "Coldflame";
            coldflame.damageType = ItemDamageType.Frost;
            coldflame.attackType = AttackTypes.AT_AOE;
            coldflame.numTicks = 3;
            coldflame.tickInterval = 1;
            coldflame.damagePerTick = new int[] { 6000, 8000, 9000, 11000 }[i];
            coldflame.maxNumTargets = get(i).getMaxPlayers();
            coldflame.attackSpeed = 6.0f;
            get(i).getAttacks().add(coldflame);
            DoT spikes = new DoT();
            spikes.name = "Bone Spike Graveyard";
            spikes.damageType = ItemDamageType.Physical;
            spikes.attackType = AttackTypes.AT_RANGED;
            spikes.numTicks = 3f;
            spikes.setDamagePerHit(9000f);
            spikes.damagePerTick = new float[] { 0.10f, 0.10f, 0.10f, 0.10f }[i];
            spikes.damageIsPerc = true;
            spikes.maxNumTargets = new int[] { 1, 1, 3, 3 }[i];
            spikes.attackSpeed = new float[] { (float) (90 / 4), (float) (90 / 4), 18, 18 }[i];
            spikes.ignoresMTank = true;
            spikes.ignoresOTank = true;
            spikes.ignoresTTank = true;
            get(i).getAttacks().add(spikes);
            TargetGroup spikeTargets = new TargetGroup();
            spikeTargets.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            spikeTargets.chance = get(i).getAttacks().get(get(i).getAttacks().size() - 1).maxNumTargets / (get(i).getMaxPlayers() - get(i).getMinTanks());
            spikeTargets.duration = 3f * 1000f;
            spikeTargets.numTargs = get(i).getAttacks().get(get(i).getAttacks().size() - 1).maxNumTargets;
            spikeTargets.nearBoss = false;
            get(i).targets.add(spikeTargets);
            Impedance spikeStun = new Impedance();
            spikeStun.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            spikeStun.duration = 3f * 1000f;
            spikeStun.chance = get(i).getAttacks().get(get(i).getAttacks().size() - 1).maxNumTargets / (get(i).getMaxPlayers() - get(i).getMinTanks());
            spikeStun.breakable = false;
            get(i).stuns.add(spikeStun);
            DoT boneStorm = new DoT();
            boneStorm.name = "Bone Storm";
            boneStorm.damageType = ItemDamageType.Physical;
            boneStorm.attackType = AttackTypes.AT_AOE;
            boneStorm.numTicks = new int[] { 20, 20, 30, 30 }[i] / 2f;
            boneStorm.tickInterval = 2f;
            boneStorm.damagePerTick = new int[] { 6000, 12000, 12000, 14000 }[i] * 0.25f;
            boneStorm.maxNumTargets = get(i).getMaxPlayers();
            boneStorm.attackSpeed = 90f;
            boneStorm.dodgable = true;
            boneStorm.missable = true;
            boneStorm.parryable = true;
            boneStorm.blockable = true;
            get(i).getAttacks().add(boneStorm);
            Impedance stormMove = new Impedance();
            stormMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            stormMove.duration = new int[] { 20, 30, 20, 30 }[i] * 1000f;
            stormMove.chance = 1f;
            stormMove.breakable = false;
            get(i).moves.add(stormMove);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
