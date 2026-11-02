// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for The Lich King in Icecrown Citadel (tier 10; all four variants).
 *
 * <p>Health is written as 89.6% of the base hit-point values. Standard melee only: no adds,
 * movement, control effects or resistances are modelled. Fifteen-minute enrage timer and 8- or
 * 13-minute speed-kill timers depending on raid size.</p>
 */
public class TheLichKing extends MultiDiffBoss {
    public TheLichKing() {
        setName("The Lich King");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 17431250f * .896f, 61009376f * .896f, 29458812f * .896f, 103151168f * .896f });
        setBerserkTimer(new int[] { 15 * 60, 15 * 60, 15 * 60, 15 * 60 });
        setSpeedKillTimer(new int[] { 8 * 60, 13 * 60, 8 * 60, 13 * 60 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 3, 5, 3, 5 });
        for (int i = 0; i < 4; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 2f;
            melee.dodgable = true;
            melee.missable = true;
            melee.parryable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
