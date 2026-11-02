// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Professor Putricide in Icecrown Citadel (tier 10; all four variants).
 *
 * <p>Standard melee only: no adds, movement, control effects or resistances are modelled.
 * Ten-minute enrage timer and speed-kill timers of 240, 270, 300 or 330 seconds depending on the
 * variant.</p>
 */
public class ProfessorPutricide extends MultiDiffBoss {
    public ProfessorPutricide() {
        setName("Professor Putricide");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 9761500f, 41835000f, 13666100f, 50202000f });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 10 * 60, 10 * 60 });
        setSpeedKillTimer(new int[] { 240, 270, 300, 330 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 3, 2, 3 });
        setMinHealers(new int[] { 3, 5, 3, 5 });
        for (int i = 0; i < 4; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 2f;
            melee.attackType = AttackTypes.AT_MELEE;
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
