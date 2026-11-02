// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT8.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.items.ItemDamageType;

/**
 * Encounter definition for XT-002 Deconstructor in Ulduar (tier 8; 10- and 25-player normal
 * variants only).
 *
 * <p>Standard melee plus "Gravity Bomb" and "Searing Light", area hits that each force a move on
 * one raid member, and "Tympanic Tantrum", a raid-wide physical hit. Ten-minute enrage and
 * three-minute speed-kill timers; the target can be attacked from behind at all times.</p>
 */
public class XT002Deconstructor extends MultiDiffBoss {
    public XT002Deconstructor() {
        setName("XT-002 Deconstructor");
        setInstance("Ulduar");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5, BossHandler.TierLevels.T8_0, BossHandler.TierLevels.T8_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 5000008f, 6763325f, 0, 0 });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 1.00f, 1.00f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 3, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            {
                Attack a = new Attack();
                a.name = "Gravity Bomb";
                a.damageType = ItemDamageType.Shadow;
                a.setDamagePerHit(12000f);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = 25;
                get(i).getAttacks().add(a);
                Impedance move = new Impedance();
                move.frequency = a.attackSpeed;
                move.duration = (9f + 5f) * 1000f;
                move.chance = 1f / get(i).getMaxPlayers();
                move.breakable = false;
                get(i).moves.add(move);
            }
            {
                Attack a = new Attack();
                a.name = "Searing Light";
                a.damageType = ItemDamageType.Holy;
                a.setDamagePerHit(3000f);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = 25;
                get(i).getAttacks().add(a);
                Impedance move = new Impedance();
                move.frequency = a.attackSpeed;
                move.duration = (9f + 5f) * 1000f;
                move.chance = 1f / get(i).getMaxPlayers();
                move.breakable = false;
                get(i).moves.add(move);
            }
            {
                Attack a = new Attack();
                a.name = "Tympanic Tantrum";
                a.damageType = ItemDamageType.Physical;
                a.setDamagePerHit(22000f * 0.10f * 8f);
                a.maxNumTargets = get(i).getMaxPlayers();
                a.attackSpeed = 25;
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
