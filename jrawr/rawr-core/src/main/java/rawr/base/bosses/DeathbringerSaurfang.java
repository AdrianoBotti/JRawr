// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;

/**
 * Encounter definition for Deathbringer Saurfang in Icecrown Citadel (tier 10; all four
 * variants).
 *
 * <p>Fast 1-second melee, "Boiling Blood" damage over time on one (10-player) or three
 * (25-player) targets and the "Blood Nova" ranged hit; no adds or movement are modelled.
 * Eight-minute enrage timer and 150- or 180-second speed-kill timers depending on the variant.</p>
 */
public class DeathbringerSaurfang extends MultiDiffBoss {
    public DeathbringerSaurfang() {
        setName("Deathbringer Saurfang");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 8785350f, 31376250f, 12299490f, 43926752f });
        setBerserkTimer(new int[] { 8 * 60, 8 * 60, 8 * 60, 8 * 60 });
        setSpeedKillTimer(new int[] { 150, 180, 150, 180 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 2, 5, 3, 5 });
        for (int i = 0; i < 4; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 1f;
            melee.dodgable = true;
            melee.missable = true;
            melee.parryable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
            DoT boilingBlood = new DoT();
            boilingBlood.name = "Boiling Blood";
            boilingBlood.damageType = ItemDamageType.Physical;
            boilingBlood.attackType = AttackTypes.AT_RANGED;
            boilingBlood.tickInterval = 3f;
            boilingBlood.numTicks = 8f;
            boilingBlood.damagePerTick = 5000f;
            boilingBlood.maxNumTargets = new int[] { 1, 3, 1, 3 }[i];
            boilingBlood.attackSpeed = 15.5f;
            get(i).getAttacks().add(boilingBlood);
            Attack bloodNova = new Attack();
            bloodNova.name = "Blood Nova";
            bloodNova.damageType = ItemDamageType.Physical;
            bloodNova.attackType = AttackTypes.AT_RANGED;
            bloodNova.setDamagePerHit(new int[] { (7600 + 8400), (9500 + 10500), (7600 + 8400), (9500 + 10500) }[i] / 2f);
            bloodNova.maxNumTargets = 1f;
            bloodNova.attackSpeed = 20f;
            get(i).getAttacks().add(bloodNova);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
