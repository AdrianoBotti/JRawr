// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for General Zarithrian in the Ruby Sanctum (tier 10; only the 10- and
 * 25-player normal entries are filled, the heroic slots are empty).
 *
 * <p>Standard melee plus a three-target Flamecaller add wave appearing roughly every 45 seconds
 * for 15 seconds; "Blast Nova" and "Lava Gout" are fire area attacks timed to the wave, and
 * "Roar" stuns the raid roughly every 33 seconds for 4 seconds. Ten-minute enrage and two-minute
 * speed-kill timers.</p>
 */
public class GeneralZarithrian extends MultiDiffBoss {
    public GeneralZarithrian() {
        setName("General Zarithrian");
        setInstance("Ruby Sanctum");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 4141000f, 14098395f, 0f, 0f });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 120, 120, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 3, 5, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 2f;
            melee.missable = true;
            melee.parryable = true;
            melee.dodgable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
            TargetGroup flamecallers = new TargetGroup();
            flamecallers.frequency = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() - 15.5f) / 45.5f);
            flamecallers.chance = 1f;
            flamecallers.duration = 15f * 1000f;
            flamecallers.nearBoss = false;
            flamecallers.numTargs = 3f;
            get(i).targets.add(flamecallers);
            Attack blastNova = new Attack();
            blastNova.name = "Blast Nova";
            blastNova.damageType = ItemDamageType.Fire;
            blastNova.attackType = AttackTypes.AT_AOE;
            blastNova.setDamagePerHit(new int[] { (5688 + 7312), (6563 + 8437), 0, 0 }[i] / 2f);
            blastNova.maxNumTargets = get(i).getMaxPlayers();
            blastNova.attackSpeed = ((get(i).targets.get(get(i).targets.size() - 1).frequency * 1000f) / (get(i).targets.get(get(i).targets.size() - 1).duration / 5f)) / get(i).targets.get(get(i).targets.size() - 1).numTargs;
            blastNova.missable = true;
            get(i).getAttacks().add(blastNova);
            Attack lavaGout = new Attack();
            lavaGout.name = "Lava Gout";
            lavaGout.damageType = ItemDamageType.Fire;
            lavaGout.attackType = AttackTypes.AT_AOE;
            lavaGout.setDamagePerHit(new int[] { (8483 + 9517), (9897 + 9897), 0, 0 }[i] / 2f);
            lavaGout.maxNumTargets = get(i).targets.get(get(i).targets.size() - 1).numTargs;
            lavaGout.attackSpeed = ((get(i).targets.get(get(i).targets.size() - 1).frequency * 1000f) / (get(i).targets.get(get(i).targets.size() - 1).duration / 2f)) / get(i).targets.get(get(i).targets.size() - 1).numTargs;
            lavaGout.interruptable = true;
            lavaGout.missable = true;
            get(i).getAttacks().add(lavaGout);
        }
        for (int i = 0; i < 4; i++) {
            Impedance roar = new Impedance();
            roar.frequency = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() - 14f) / 33f);
            roar.chance = 1f;
            roar.duration = 4f * 1000f;
            roar.breakable = false;
            get(i).stuns.add(roar);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
