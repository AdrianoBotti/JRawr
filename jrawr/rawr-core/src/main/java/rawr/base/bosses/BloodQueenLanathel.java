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
 * Encounter definition for Blood-Queen Lana'thel in Icecrown Citadel (tier 10; all four
 * variants).
 *
 * <p>One of the most detailed Icecrown encounters here: melee plus off-tank melee, the "Shroud of
 * Sorrow" area pulse, "Delirious Slash" as a damage-over-time effect, "Vampiric Bite" against a
 * growing number of bitten players, "Uncontrollable Frenzy" with a 60-second stun, "Twilight
 * Bloodbolt", "Swarming Shadows" and "Pact of the Darkfallen" (both damage-over-time effects with
 * forced moves) and "Bloodbolt Whirl" with a fear and a move. Back-attack availability is reduced
 * by two 12-second windows for melee (two 4-second windows for ranged); the enrage timer is 320
 * seconds in every variant.</p>
 */
public class BloodQueenLanathel extends MultiDiffBoss {
    public BloodQueenLanathel() {
        setName("Blood-Queen Lana'thel");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 14154175f, 59419644f, 18899660f, 71300784f });
        setBerserkTimer(new int[] { 320, 320, 320, 320 });
        setSpeedKillTimer(new int[] { 210, 210, 270, 270 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 3, 5, 3, 5 });
        setInBackPercMelee(new double[] { (320 - 12 - 12) / 320, (320 - 12 - 12) / 320, (320 - 12 - 12) / 320, (320 - 12 - 12) / 320 });
        setInBackPercRanged(new double[] { (320 - 4 - 4) / 320, (320 - 4 - 4) / 320, (320 - 4 - 4) / 320, (320 - 4 - 4) / 320 });
        int avgbitetargets10 = (1 + 2 + 4 + 8) / 4;
        int avgbitetargets25 = (1 + 2 + 4 + 8 + 16) / 5;
        for (int i = 0; i < 4; i++) {
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit((1 + (new int[] { 0, avgbitetargets10, 0, avgbitetargets25 }[i] * new float[] { 0f, .10f, 0f, .05f }[i])) * BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            melee.maxNumTargets = 1f;
            melee.attackSpeed = 2f;
            melee.missable = true;
            melee.parryable = true;
            melee.dodgable = true;
            melee.blockable = true;
            melee.ignoresOTank = true;
            melee.ignoresTTank = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            melee.isTheDefaultMelee = true;
            get(i).getAttacks().add(melee);
            Attack offTankMelee = new Attack();
            offTankMelee.name = "Off-Tank Melee Damage";
            offTankMelee.damageType = ItemDamageType.Shadow;
            offTankMelee.attackType = AttackTypes.AT_MELEE;
            offTankMelee.setDamagePerHit((1 + (new int[] { 0, avgbitetargets10, 0, avgbitetargets25 }[i] * new float[] { 0f, .10f, 0f, .05f }[i])) * BossHandler.StandardMeleePerHit[get(i).getContent().value()]);
            offTankMelee.maxNumTargets = 1f;
            offTankMelee.attackSpeed = 2f;
            offTankMelee.missable = true;
            offTankMelee.parryable = true;
            offTankMelee.dodgable = true;
            offTankMelee.blockable = true;
            offTankMelee.ignoresMTank = true;
            offTankMelee.ignoresTTank = true;
            offTankMelee.ignoresMeleeDps = true;
            offTankMelee.ignoresRangedDps = true;
            offTankMelee.ignoresHealers = true;
            get(i).getAttacks().add(offTankMelee);
            Attack shroud = new Attack();
            shroud.name = "Shroud of Sorrow";
            shroud.damageType = ItemDamageType.Shadow;
            shroud.attackType = AttackTypes.AT_AOE;
            shroud.setDamagePerHit(new int[] { 4000, 4500, 4500, 4500 }[i]);
            shroud.maxNumTargets = get(i).getMaxPlayers();
            shroud.attackSpeed = 2f;
            get(i).getAttacks().add(shroud);
            DoT deliriousSlash = new DoT();
            deliriousSlash.name = "Delirious Slash";
            deliriousSlash.damageType = ItemDamageType.Physical;
            deliriousSlash.attackType = AttackTypes.AT_MELEE;
            deliriousSlash.tickInterval = 3f;
            deliriousSlash.numTicks = 5f;
            deliriousSlash.setDamagePerHit(BossHandler.StandardMeleePerHit[get(i).getContent().value()] * new float[] { .5f, .5f, .75f, .75f }[i]);
            deliriousSlash.damagePerTick = new int[] { (4500 + 5500), (5250 + 6750), (6125 + 7875), (7000 + 9000) }[i] / 2f;
            deliriousSlash.maxNumTargets = 1f;
            deliriousSlash.attackSpeed = 20f;
            deliriousSlash.missable = true;
            deliriousSlash.parryable = true;
            deliriousSlash.dodgable = true;
            deliriousSlash.blockable = true;
            deliriousSlash.ignoresMTank = true;
            deliriousSlash.ignoresTTank = true;
            deliriousSlash.ignoresMeleeDps = true;
            deliriousSlash.ignoresRangedDps = true;
            deliriousSlash.ignoresHealers = true;
            get(i).getAttacks().add(deliriousSlash);
            Attack vampiricBite = new Attack();
            vampiricBite.name = "Vampiric Bite";
            vampiricBite.damageType = ItemDamageType.Physical;
            vampiricBite.attackType = AttackTypes.AT_AOE;
            vampiricBite.setDamagePerHit(new int[] { (8325 + 9675), (10175 + 11825), (8325 + 9675), (10175 + 11825) }[i] / 2f);
            vampiricBite.maxNumTargets = new int[] { avgbitetargets10, avgbitetargets25, avgbitetargets10, avgbitetargets25 }[i];
            vampiricBite.attackSpeed = new int[] { 75, 60, 75, 60 }[i] + 5f;
            vampiricBite.ignoresMTank = true;
            get(i).getAttacks().add(vampiricBite);
            Impedance biteMove = new Impedance();
            biteMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            biteMove.duration = 3f * 1000f;
            biteMove.chance = 1f / new int[] { avgbitetargets10, avgbitetargets25, avgbitetargets10, avgbitetargets25 }[i];
            biteMove.breakable = false;
            get(i).moves.add(biteMove);
            Attack frenzy = new Attack();
            frenzy.name = "Uncontrollable Frenzy";
            frenzy.damageType = ItemDamageType.Physical;
            frenzy.attackType = AttackTypes.AT_AOE;
            frenzy.setDamagePerHit(1f);
            frenzy.damageIsPerc = true;
            frenzy.maxNumTargets = new int[] { avgbitetargets10, avgbitetargets10, avgbitetargets25, avgbitetargets25 }[i];
            frenzy.attackSpeed = new int[] { 75, 60, 75, 60 }[i] + 5f;
            frenzy.ignoresMTank = true;
            frenzy.interruptable = true;
            get(i).getAttacks().add(frenzy);
            Impedance frenzyStun = new Impedance();
            frenzyStun.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            frenzyStun.duration = 60f * 1000f;
            frenzyStun.chance = 1f / new int[] { avgbitetargets10, avgbitetargets10, avgbitetargets25, avgbitetargets25 }[i];
            frenzyStun.breakable = false;
            get(i).stuns.add(frenzyStun);
            Attack bloodbolt = new Attack();
            bloodbolt.name = "Twilight Bloodbolt";
            bloodbolt.damageType = ItemDamageType.Shadow;
            bloodbolt.attackType = AttackTypes.AT_AOE;
            bloodbolt.setDamagePerHit(new int[] { (9250 + 10750), (11250 + 13750), (9250 + 10750), (11250 + 13750) }[i] / 2f);
            bloodbolt.maxNumTargets = new int[] { 2, 3, 2, 3 }[i];
            bloodbolt.attackSpeed = 20f;
            bloodbolt.ignoresMTank = true;
            bloodbolt.ignoresOTank = true;
            bloodbolt.ignoresMeleeDps = true;
            get(i).getAttacks().add(bloodbolt);
            DoT swarmingShadows = new DoT();
            swarmingShadows.name = "Swarming Shadows";
            swarmingShadows.damageType = ItemDamageType.Shadow;
            swarmingShadows.attackType = AttackTypes.AT_AOE;
            swarmingShadows.numTicks = 6f;
            swarmingShadows.tickInterval = 1f;
            swarmingShadows.damagePerTick = new int[] { (2313 + 2687), (2313 + 2687), (2313 + 2687), (2313 + 2687) }[i] / 2f;
            swarmingShadows.maxNumTargets = new int[] { 1, 1, 1, 1 }[i];
            swarmingShadows.attackSpeed = 30.5f;
            swarmingShadows.ignoresMTank = true;
            swarmingShadows.ignoresOTank = true;
            get(i).getAttacks().add(swarmingShadows);
            Impedance shadowMove = new Impedance();
            shadowMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            shadowMove.duration = 6f * 1000f;
            shadowMove.chance = 1f / (get(i).getMaxPlayers() - get(i).getMinTanks());
            shadowMove.breakable = false;
            get(i).moves.add(shadowMove);
            DoT pact = new DoT();
            pact.name = "Pact of the Darkfallen";
            pact.damageType = ItemDamageType.Shadow;
            pact.attackType = AttackTypes.AT_AOE;
            pact.numTicks = 4f;
            pact.tickInterval = 1f;
            pact.damagePerTick = new int[] { 3500, 3500, 3500, 3500 }[i];
            pact.maxNumTargets = get(i).getMaxPlayers() / 2f;
            pact.attackSpeed = 30.5f;
            get(i).getAttacks().add(pact);
            Impedance pactMove = new Impedance();
            pactMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed;
            pactMove.duration = 4f * 1000f;
            pactMove.chance = new float[] { 2, 3, 2, 3 }[i] / (get(i).getMaxPlayers() - get(i).getMinTanks());
            pactMove.breakable = false;
            get(i).moves.add(pactMove);
            Attack bloodboltWhirl = new Attack();
            bloodboltWhirl.name = "Bloodbolt Whirl";
            bloodboltWhirl.damageType = ItemDamageType.Shadow;
            bloodboltWhirl.attackType = AttackTypes.AT_AOE;
            bloodboltWhirl.setDamagePerHit(new int[] { (9250 + 10750), (12025 + 13975), (14250 + 15750), (16150 + 17850) }[i] / 2f);
            bloodboltWhirl.maxNumTargets = get(i).getMaxPlayers();
            bloodboltWhirl.attackSpeed = (new int[] { 124, 127, 124, 127 }[i] + 100f + 100f) / 3f;
            get(i).getAttacks().add(bloodboltWhirl);
            Impedance whirlFear = new Impedance();
            whirlFear.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed - 6f;
            whirlFear.duration = 4f * 1000;
            whirlFear.chance = get(i).getMaxPlayers() / get(i).getMaxPlayers();
            whirlFear.breakable = true;
            get(i).fears.add(whirlFear);
            Impedance whirlMove = new Impedance();
            whirlMove.frequency = get(i).getAttacks().get(get(i).getAttacks().size() - 1).attackSpeed - 2f;
            whirlMove.duration = 3f * 1000;
            whirlMove.chance = 1f;
            whirlMove.breakable = true;
            get(i).moves.add(whirlMove);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
