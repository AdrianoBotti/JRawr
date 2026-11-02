// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT7.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.character.Buff;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Malygos in The Eye of Eternity (tier 7; 10- and 25-player normal
 * variants only).
 *
 * <p>Spark and scion add groups are modelled alongside the boss. Attacks: standard melee plus the
 * arcane "Arcane Breath", "Vortex" and "Arcane Pulse" area hits. A vortex forced move occurs every
 * ~95 seconds for 10 seconds. Ten-minute enrage and three-minute speed-kill timers. The
 * constructor's comment notes that phase 3 and the Power Spark damage zones cannot be modelled and
 * that Arcane Pulse cannot be limited to its phase.</p>
 */
public class Malygos extends MultiDiffBoss {
    public Malygos() {
        setName("Malygos");
        setInstance("The Eye of Eternity");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5, BossHandler.TierLevels.T7_0, BossHandler.TierLevels.T7_5 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10N, BossHandler.Versions.V_25N });
        setComment("ToDo's:\r\n"
                + "* We have no way to model Phase 3, and since it's not based on your actual DPS, who cares!\r\n"
                + "* Need Buff tie-ins for the Power Sparks bonus damage zones in Phase 1\r\n"
                + "* Arcane Pulse should only be occurring in Phase 2, but can't limit it\r\n");
        setHealth(new float[] { 2230000f, 19523000f, 0, 0 });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 0, 0 });
        setSpeedKillTimer(new int[] { 3 * 60, 3 * 60, 0, 0 });
        setInBackPercMelee(new double[] { 0.95f, 0.95f, 0, 0 });
        setInBackPercRanged(new double[] { 0.00f, 0.00f, 0, 0 });
        setMaxPlayers(new int[] { 10, 25, 0, 0 });
        setMinTanks(new int[] { 2, 2, 0, 0 });
        setMinHealers(new int[] { 2, 4, 0, 0 });
        for (int i = 0; i < 2; i++) {
            TargetGroup sparks = new TargetGroup();
            sparks.frequency = 45;
            sparks.duration = 10 * 1000f;
            sparks.chance = 1f / 3f;
            sparks.nearBoss = false;
            sparks.numTargs = 1;
            get(i).targets.add(sparks);
            TargetGroup scions = new TargetGroup();
            scions.frequency = get(i).getBerserkTimer() - 1;
            scions.duration = get(i).getBerserkTimer() / 3f * 1000f;
            scions.chance = 1.00f;
            scions.nearBoss = false;
            scions.numTargs = 2;
            get(i).targets.add(scions);
            get(i).getAttacks().add(genAStandardMelee(get(i).getContent()));
            Attack breath = new Attack();
            breath.name = "Arcane Breath";
            breath.attackSpeed = 40;
            breath.attackType = AttackTypes.AT_AOE;
            breath.setDamagePerHit(new float[] { (18850f + 21150f), (28275f + 31725f) }[i] / 2f);
            breath.damageType = ItemDamageType.Arcane;
            breath.missable = false;
            breath.dodgable = false;
            breath.parryable = false;
            breath.blockable = false;
            breath.ignoresHealers = true;
            breath.ignoresMeleeDps = true;
            breath.ignoresOTank = true;
            breath.ignoresTTank = true;
            breath.ignoresRangedDps = true;
            breath.maxNumTargets = get(i).getMaxPlayers();
            get(i).getAttacks().add(breath);
            Attack vortex = new Attack();
            vortex.name = "Vortex";
            vortex.attackSpeed = 40;
            vortex.attackType = AttackTypes.AT_AOE;
            vortex.setDamagePerHit(new float[] { 4000, 6000 }[i]);
            vortex.damageType = ItemDamageType.Arcane;
            vortex.missable = false;
            vortex.dodgable = false;
            vortex.parryable = false;
            vortex.blockable = false;
            vortex.maxNumTargets = get(i).getMaxPlayers();
            get(i).getAttacks().add(vortex);
            Attack pulse = new Attack();
            pulse.name = "Arcane Pulse";
            pulse.attackSpeed = 1;
            pulse.attackType = AttackTypes.AT_AOE;
            pulse.setDamagePerHit(new float[] { (28275f + 31725f), (28275f + 31725f) }[i] / 2f);
            pulse.damageType = ItemDamageType.Arcane;
            pulse.missable = false;
            pulse.dodgable = false;
            pulse.parryable = false;
            pulse.blockable = false;
            pulse.maxNumTargets = get(i).getMaxPlayers();
            get(i).getAttacks().add(pulse);
        }
        setResistPhysical(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFrost(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistFire(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistNature(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistArcane(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistShadow(new double[] { 0.00f, 0.00f, 0, 0 });
        setResistHoly(new double[] { 0.00f, 0.00f, 0, 0 });
        for (int i = 0; i < 2; i++) {
            Impedance vortex = new Impedance();
            vortex.frequency = (70f + 120f) / 2f;
            vortex.duration = (16f + 4f) * 1000f;
            vortex.chance = 1f / 3f;
            vortex.breakable = false;
            get(i).moves.add(vortex);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0, 0 });
    }
}
