// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT8.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Ignis the Furnace Master in Ulduar (tier 8; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus two fire area attacks, "Flame Jets" and "Scorch"; Scorch also forces a
 * 5-second move. Nineteen-minute enrage and three-minute speed-kill timers; 90% back-attack
 * availability.</p>
 */
public class IgnistheFurnaceMaster extends MultiDiffBoss {
    public IgnistheFurnaceMaster() {
        setName("Ignis the Furnace Master");
        setInstance("Ulduar");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5, BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 5578000f, 6763325f, 0, 0 });
        setBerserkTimer(new int[] { 19 * 60, 19 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.90f, 0.90f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 3, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            {
                Attack a = new Attack();
                a.name = "Flame Jets";
                a.attackSpeed = 25f;
                a.attackType = AttackTypes.AT_AOE;
                a.setDamagePerHit((5655f - 6345f) / 2f);
                a.damageType = ItemDamageType.Fire;
                a.maxNumTargets = get(i).getMaxPlayers();
                get(i).getAttacks().add(a);
            }
            {
                Attack a = new Attack();
                a.name = "Scorch";
                a.attackSpeed = 25f;
                a.attackType = AttackTypes.AT_AOE;
                a.setDamagePerHit((2357f - 2643f) / 2f);
                a.damageType = ItemDamageType.Fire;
                a.maxNumTargets = get(i).getMaxPlayers();
                get(i).getAttacks().add(a);
                Impedance move = new Impedance();
                move.frequency = a.attackSpeed;
                move.duration = 5f * 1000f;
                move.chance = 1f;
                move.breakable = false;
                get(i).moves.add(move);
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
