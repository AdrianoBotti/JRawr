// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Saviana Ragefire in the Ruby Sanctum (tier 10; only the 10- and
 * 25-player normal entries are filled, the heroic slots are empty).
 *
 * <p>Standard melee plus "Flame Breath", the interruptable "Enrage" damage-over-time against the
 * tanks and "Fire Nova" area damage. A conflagration stun affects two (10-player) or five
 * (25-player) players roughly every 50 seconds for 6 seconds. Ten-minute enrage and two-minute
 * speed-kill timers.</p>
 */
public class SavianaRagefire extends MultiDiffBoss {
    public SavianaRagefire() {
        setName("Saviana Ragefire");
        setInstance("Ruby Sanctum");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 4183000f, 13945000f, 0f, 0f });
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
            Attack flameBreath = new Attack();
            flameBreath.name = "Flame Breath";
            flameBreath.damageType = ItemDamageType.Fire;
            flameBreath.setDamagePerHit(new int[] { (24500 + 31500), (33687 + 43312), 0, 0 }[i] / 2f);
            flameBreath.maxNumTargets = 1f;
            flameBreath.attackSpeed = (get(i).getBerserkTimer() / ((get(i).getBerserkTimer() - 12f) / 25f));
            flameBreath.attackType = AttackTypes.AT_MELEE;
            flameBreath.ignoresMeleeDps = true;
            flameBreath.ignoresRangedDps = true;
            flameBreath.ignoresHealers = true;
            get(i).getAttacks().add(flameBreath);
            DoT enrage = new DoT();
            enrage.name = "Enrage";
            enrage.damageType = ItemDamageType.Physical;
            enrage.numTicks = 10f / (2f / 1.5f);
            enrage.tickInterval = (2f / 1.5f);
            enrage.damagePerTick = BossHandler.StandardMeleePerHit[get(i).getContent().value()];
            enrage.maxNumTargets = get(i).getMinTanks();
            enrage.attackSpeed = 30f;
            enrage.attackType = AttackTypes.AT_MELEE;
            enrage.interruptable = true;
            enrage.missable = true;
            enrage.parryable = true;
            enrage.dodgable = true;
            enrage.blockable = true;
            enrage.ignoresMeleeDps = true;
            enrage.ignoresRangedDps = true;
            enrage.ignoresHealers = true;
            get(i).getAttacks().add(enrage);
            Attack fireNova = new Attack();
            fireNova.name = "Fire Nova";
            fireNova.damageType = ItemDamageType.Fire;
            fireNova.setDamagePerHit(7600f);
            fireNova.maxNumTargets = get(i).getMaxPlayers();
            fireNova.attackSpeed = 15f;
            fireNova.attackType = AttackTypes.AT_AOE;
            get(i).getAttacks().add(fireNova);
        }
        for (int i = 0; i < 4; i++) {
            Impedance conflagration = new Impedance();
            conflagration.frequency = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() - 32f) / 50f);
            conflagration.duration = 6f * 1000f;
            conflagration.chance = new float[] { 2, 5, 2, 5 }[i] / get(i).getMaxPlayers();
            conflagration.breakable = false;
            get(i).stuns.add(conflagration);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
