// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Toravon the Ice Watcher in the Vault of Archavon (tier 10; 10- and
 * 25-player normal variants only).
 *
 * <p>Standard melee plus "Frostbite": a frost hit against the tanks, modelled as a
 * damage-over-time object whose tick fields are unused, so only the direct hit applies. It is
 * unavoidable (no miss, dodge, parry or block). A "Whiteout" frost-damage multiplier is computed
 * in the constructor but not attached to any attack. Five-minute enrage and three-minute
 * speed-kill timers; 95% back-attack availability.</p>
 */
public class ToravonTheIceWatcher extends MultiDiffBoss {
    public ToravonTheIceWatcher() {
        setName("Toravon The Ice Watcher");
        setInstance("The Vault of Archavon");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setHealth(new float[] { 4601850f, 15060600f, 0, 0 });
        setBerserkTimer(new int[] { 5 * 60, 5 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            float frostmultiplierfromwhiteout = (((getBerserkTimer()[i] - 25f) / 38f) * .15f) / 2f;
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            DoT frostbite = new DoT();
            frostbite.name = "Frostbite";
            frostbite.damageType = ItemDamageType.Frost;
            frostbite.attackType = AttackTypes.AT_MELEE;
            frostbite.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            frostbite.maxNumTargets = get(i).getMinTanks();
            frostbite.attackSpeed = new float[] { (90 / (70 / 2)), (90 / (70 / 2)), (90 / (60 / 2)), (90 / (60 / 2)) }[i];
            frostbite.dodgable = false;
            frostbite.missable = false;
            frostbite.parryable = false;
            frostbite.blockable = false;
            frostbite.ignoresMeleeDps = true;
            frostbite.ignoresRangedDps = true;
            frostbite.ignoresHealers = true;
            get(i).getAttacks().add(frostbite);
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
