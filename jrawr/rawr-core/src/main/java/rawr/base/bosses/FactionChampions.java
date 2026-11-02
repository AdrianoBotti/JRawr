// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT9.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for the Faction Champions in the Trial of the Crusader (tier 9; all four
 * variants).
 *
 * <p>Health is computed from a weighted mix of lesser and greater champion health values rather
 * than a single number. Only standard melee is modelled; the champions' abilities are not.
 * Nineteen-minute enrage timer and a five-minute speed-kill timer.</p>
 */
public class FactionChampions extends MultiDiffBoss {
    public FactionChampions() {
        setName("Faction Champions");
        setInstance("Trial of the Crusader");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T9_0, BossHandler.TierLevels.T9_5, BossHandler.TierLevels.T9_0, BossHandler.TierLevels.T9_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { ((322560 * 4) + (403200 * 10)) * 6 / 14, ((1935360 * 4) + (2419000 * 10)) * 10 / 14, ((483840 * 4) + (604800 * 10)) * 6 / 14, ((2580480 * 4) + (3225600 * 10)) * 10 / 14 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 19 * 60, 19 * 60 });
        setSpeedKillTimer(new int[] { 5 * 60, 5 * 60, 5 * 60, 5 * 60 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, .95f, .95f });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0.00f, 0.00f });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 2, 4, 2, 5 });
        for (int i = 0; i < 4; i++) {
            Attack a = new Attack();
            a.name = "Melee";
            a.damageType = ItemDamageType.Physical;
            a.attackType = AttackTypes.AT_MELEE;
            a.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            a.maxNumTargets = 1f;
            a.attackSpeed = 2f;
            a.dodgable = true;
            a.missable = true;
            a.parryable = true;
            a.blockable = true;
            a.ignoresMeleeDps = true;
            a.ignoresRangedDps = true;
            a.ignoresHealers = true;
            a.isTheDefaultMelee = true;
            get(i).getAttacks().add(a);
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
