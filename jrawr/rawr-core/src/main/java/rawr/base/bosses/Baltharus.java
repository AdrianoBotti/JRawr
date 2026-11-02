// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Baltharus in the Ruby Sanctum (tier 10; only the 10- and 25-player
 * normal entries are filled, the two heroic slots are empty).
 *
 * <p>A multi-attacker fight: besides the boss's own melee it models the slower "Cloned Melee" of
 * the clones, the arcane "Siphoned Might" tank hit, "Blade Tempest" against the tanks every 1.5
 * seconds, "Repelling Wave", an area hit that also stuns and moves those it hits, and a "Cleave"
 * against three targets. Ten-minute enrage and two-minute speed-kill timers.</p>
 */
public class Baltharus extends MultiDiffBoss {
    public Baltharus() {
        setName("Baltharus");
        setInstance("Ruby Sanctum");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 3486000f, 11156000f, 0f, 0f });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 120, 120, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 3, 5, 0, 0 });
        for (int i = 0; i < 2; i++) {
            int cloneattackspeed10man = (int) (getBerserkTimer()[i] / (getBerserkTimer()[i] * (.5f * .5f)));
            int cloneattackspeed25man = (int) (getBerserkTimer()[i] / ((getBerserkTimer()[i] * (.66f * .5f)) + (getBerserkTimer()[i] * (.33f * .5f))));
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 3f;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.missable = true;
            melee.parryable = true;
            melee.dodgable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
            Attack clonedMelee = new Attack();
            clonedMelee.name = "Cloned Melee";
            clonedMelee.damageType = ItemDamageType.Physical;
            clonedMelee.attackType = AttackTypes.AT_MELEE;
            clonedMelee.setDamagePerHit(new int[] { 1, (1 + 2) / 2, 0, 0 }[i] * BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            clonedMelee.maxNumTargets = 1f;
            clonedMelee.attackSpeed = new int[] { cloneattackspeed10man, cloneattackspeed25man, cloneattackspeed10man, cloneattackspeed25man }[i];
            clonedMelee.missable = true;
            clonedMelee.parryable = true;
            clonedMelee.dodgable = true;
            clonedMelee.blockable = true;
            clonedMelee.ignoresMTank = true;
            clonedMelee.ignoresMeleeDps = true;
            clonedMelee.ignoresRangedDps = true;
            clonedMelee.ignoresHealers = true;
            get(i).getAttacks().add(clonedMelee);
            Attack siphonedMight = new Attack();
            siphonedMight.name = "Siphoned Might";
            siphonedMight.damageType = ItemDamageType.Arcane;
            siphonedMight.attackType = AttackTypes.AT_MELEE;
            siphonedMight.setDamagePerHit(((1f + (1f + (.02f * 20f / 2f))) / 2f) * BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            siphonedMight.maxNumTargets = 1f;
            siphonedMight.attackSpeed = (30f - 10f) / 3f;
            siphonedMight.ignoresOTank = true;
            siphonedMight.ignoresTTank = true;
            siphonedMight.ignoresMeleeDps = true;
            siphonedMight.ignoresRangedDps = true;
            siphonedMight.ignoresHealers = true;
            get(i).getAttacks().add(siphonedMight);
            Attack bladeTempest = new Attack();
            bladeTempest.name = "Blade Tempest";
            bladeTempest.damageType = ItemDamageType.Physical;
            bladeTempest.attackType = AttackTypes.AT_MELEE;
            bladeTempest.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()] * .7f);
            bladeTempest.maxNumTargets = get(i).getMinTanks();
            bladeTempest.attackSpeed = 1.5f;
            bladeTempest.missable = true;
            bladeTempest.parryable = true;
            bladeTempest.dodgable = true;
            bladeTempest.blockable = true;
            bladeTempest.ignoresMeleeDps = true;
            bladeTempest.ignoresRangedDps = true;
            bladeTempest.ignoresHealers = true;
            get(i).getAttacks().add(bladeTempest);
            Attack repellingWave = new Attack();
            repellingWave.name = "Repelling Wave";
            repellingWave.damageType = ItemDamageType.Physical;
            repellingWave.attackType = AttackTypes.AT_AOE;
            repellingWave.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()] * .7f);
            repellingWave.maxNumTargets = get(i).getMaxPlayers() - get(i).getMinHealers();
            repellingWave.ignoresOTank = true;
            repellingWave.ignoresTTank = true;
            repellingWave.ignoresHealers = true;
            repellingWave.ignoresRangedDps = true;
            get(i).getAttacks().add(repellingWave);
            Impedance waveStun = new Impedance();
            waveStun.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            waveStun.duration = 3f * 1000f;
            waveStun.chance = 1f / (get(i).getMaxPlayers() - get(i).getMinHealers());
            waveStun.breakable = false;
            get(i).stuns.add(waveStun);
            Impedance waveMove = new Impedance();
            waveMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            waveMove.duration = 3f * 1000f;
            waveMove.chance = 1f / (get(i).getMaxPlayers() - get(i).getMinHealers());
            waveMove.breakable = false;
            get(i).moves.add(waveMove);
            Attack cleave = new Attack();
            cleave.name = "Cleave";
            cleave.damageType = ItemDamageType.Physical;
            cleave.attackType = AttackTypes.AT_MELEE;
            cleave.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()] * 1.1f);
            cleave.maxNumTargets = 3f;
            cleave.attackSpeed = 20f;
            cleave.missable = true;
            cleave.parryable = true;
            cleave.dodgable = true;
            cleave.blockable = true;
            cleave.ignoresMeleeDps = true;
            cleave.ignoresRangedDps = true;
            cleave.ignoresHealers = true;
            get(i).getAttacks().add(cleave);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
