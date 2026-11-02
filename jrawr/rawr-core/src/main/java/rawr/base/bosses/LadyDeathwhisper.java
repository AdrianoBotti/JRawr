// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossesT10.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.items.ItemDamageType;
import rawr.base.bosses.MultiDiffBoss;
import rawr.base.character.TargetGroup;

/**
 * Encounter definition for Lady Deathwhisper in Icecrown Citadel (tier 10; all four variants).
 *
 * <p>Health is written as the sum of two values: the mana barrier plus the boss's health, as the
 * original comment notes. Add waves (reanimated adds, heroic-only reinforcements and shades)
 * appear on 30- to 60-second cycles, each with an associated forced move. Attacks modelled:
 * standard melee, Shadow Bolt, Frostbolt, Frostbolt
 * Volley, the "Death and Decay" damage-over-time area with a move, Deathchill Bolt, Necrotic
 * Strike, Shadow Cleave and the Vengeful Blast (separate normal and heroic variants), plus a
 * "Dominate Mind" stun. Ten-minute enrage timer and speed-kill timers of 150 or 300 seconds.</p>
 */
public class LadyDeathwhisper extends MultiDiffBoss {
    public LadyDeathwhisper() {
        setName("Lady Deathwhisper");
        setInstance("Icecrown Citadel");
        setContent(new BossHandler.TierLevels[] { BossHandler.TierLevels.T10_0, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_5, BossHandler.TierLevels.T10_9 });
        setVersion(new BossHandler.Versions[] { BossHandler.Versions.V_10N, BossHandler.Versions.V_25N, BossHandler.Versions.V_10H, BossHandler.Versions.V_25H });
        setHealth(new float[] { 3264800f + 3346800f, 11193600f + 13387200f, 3264800f + 6693600f, 13992000f + 26774400f });
        setBerserkTimer(new int[] { 10 * 60, 10 * 60, 10 * 60, 10 * 60 });
        setSpeedKillTimer(new int[] { 150, 300, 150, 300 });
        setMaxPlayers(new int[] { 10, 25, 10, 25 });
        setMinTanks(new int[] { 2, 3, 2, 3 });
        setMinHealers(new int[] { 2, 5, 3, 5 });
        for (int i = 0; i < 4; i++) {
            TargetGroup adds = new TargetGroup();
            adds.frequency = new float[] { 60, 60, 45, 45 }[i];
            adds.duration = 35 * 1000;
            adds.numTargs = new float[] { 3, 7, 3, 7 }[i];
            adds.chance = .5f;
            adds.nearBoss = false;
            get(i).targets.add(adds);
            TargetGroup reanimated = new TargetGroup();
            reanimated.frequency = new float[] { 60, 60, 45, 45 }[i];
            reanimated.duration = 35f * 1000;
            reanimated.numTargs = 1f;
            reanimated.chance = .5f * new float[] { 1 / 3, 1 / 7, 1 / 3, 1 / 7 }[i];
            reanimated.nearBoss = false;
            get(i).targets.add(reanimated);
            TargetGroup heroicAdds = new TargetGroup();
            heroicAdds.frequency = 45f;
            heroicAdds.duration = 35f * 1000;
            heroicAdds.numTargs = new float[] { 0, 0, 1, 3 }[i];
            heroicAdds.chance = new float[] { 0, 0, .5f, .5f }[i];
            heroicAdds.nearBoss = false;
            get(i).targets.add(heroicAdds);
            Attack melee = new Attack();
            melee.name = "Melee";
            melee.damageType = ItemDamageType.Physical;
            melee.attackType = AttackTypes.AT_MELEE;
            melee.setDamagePerHit(((BossHandler.StandardMeleePerHit[get(i).getContent().value()] / 1.3f) / 1.3f));
            melee.maxNumTargets = 1f;
            melee.attackSpeed = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() * .5f) / 2f);
            melee.dodgable = true;
            melee.missable = true;
            melee.parryable = true;
            melee.blockable = true;
            melee.ignoresMeleeDps = true;
            melee.ignoresRangedDps = true;
            melee.ignoresHealers = true;
            get(i).getAttacks().add(melee);
            Attack shadowBolt = new Attack();
            shadowBolt.name = "Shadow Bolt";
            shadowBolt.damageType = ItemDamageType.Shadow;
            shadowBolt.attackType = AttackTypes.AT_AOE;
            shadowBolt.setDamagePerHit(new int[] { (7438 + 9562), (9188 + 11812), (9188 + 11812), (11375 + 14625) }[i] / 2f);
            shadowBolt.maxNumTargets = 1f;
            shadowBolt.attackSpeed = 4f;
            get(i).getAttacks().add(shadowBolt);
            Attack frostbolt = new Attack();
            frostbolt.name = "Frostbolt";
            frostbolt.damageType = ItemDamageType.Frost;
            frostbolt.attackType = AttackTypes.AT_MELEE;
            frostbolt.setDamagePerHit(new int[] { (40950 + 43050), (50700 + 53330), (44850 + 47150), (58500 + 61500) }[i] / 2f);
            frostbolt.maxNumTargets = 1f;
            frostbolt.attackSpeed = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() * .5f) / 4f);
            frostbolt.interruptable = true;
            frostbolt.ignoresMeleeDps = true;
            frostbolt.ignoresRangedDps = true;
            frostbolt.ignoresHealers = true;
            get(i).getAttacks().add(frostbolt);
            Attack frostboltVolley = new Attack();
            frostboltVolley.name = "Frostbolt Volley";
            frostboltVolley.damageType = ItemDamageType.Frost;
            frostboltVolley.attackType = AttackTypes.AT_AOE;
            frostboltVolley.setDamagePerHit(new int[] { (8550 + 10450), (10800 + 13200), (10800 + 13200), (14400 + 17600) }[i] / 2f);
            frostboltVolley.maxNumTargets = get(i).getMaxPlayers();
            frostboltVolley.attackSpeed = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() * .5f) / 20f);
            get(i).getAttacks().add(frostboltVolley);
            DoT deathAndDecay = new DoT();
            deathAndDecay.name = "Death and Decay";
            deathAndDecay.damageType = ItemDamageType.Shadow;
            deathAndDecay.attackType = AttackTypes.AT_AOE;
            deathAndDecay.numTicks = 10f;
            deathAndDecay.tickInterval = 1f;
            deathAndDecay.damagePerTick = new int[] { 4500, 4500, 6000, 6000 }[i] * 0.25f;
            deathAndDecay.maxNumTargets = get(i).getMaxPlayers();
            deathAndDecay.attackSpeed = 45f;
            get(i).getAttacks().add(deathAndDecay);
            Impedance move = new Impedance();
            move.frequency = 45f;
            move.duration = 4f * 1000f;
            move.chance = 1 / get(i).getMaxPlayers();
            move.breakable = false;
            get(i).moves.add(move);
            Attack deathchillBolt = new Attack();
            deathchillBolt.name = "Deathchill Bolt";
            deathchillBolt.damageType = ItemDamageType.Shadow;
            deathchillBolt.attackType = AttackTypes.AT_AOE;
            deathchillBolt.setDamagePerHit(new int[] { (6938 + 8062), (8788 + 10212), (8788 + 10212), (11563 + 13437) }[i] / 2f);
            deathchillBolt.maxNumTargets = 1f;
            deathchillBolt.attackSpeed = ((new float[] { 60, 60, 45, 45 }[i] / (35f / 2f)) * new float[] { 1.5f, 3.5f, 1.5f, 3.5f }[i] * .5f) + ((new float[] { 60, 60, 45, 45 }[i] / (35f / 2f)) * new float[] { 0f, 0f, .5f, 1.5f }[i] * .5f);
            get(i).getAttacks().add(deathchillBolt);
            Attack necroticStrike = new Attack();
            necroticStrike.name = "Necrotic Strike";
            necroticStrike.damageType = ItemDamageType.Physical;
            necroticStrike.attackType = AttackTypes.AT_MELEE;
            necroticStrike.setDamagePerHit(((BossHandler.StandardMeleePerHit[get(i).getContent().value()] / 1.3f) / 1.3f) * .7f);
            necroticStrike.maxNumTargets = 1f;
            necroticStrike.attackSpeed = ((new float[] { 60, 60, 45, 45 }[i] / (35f / 20f)) * new float[] { 1.5f, 3.5f, 1.5f, 3.5f }[i] * .5f) + ((new float[] { 60, 60, 45, 45 }[i] / (35f / 20f)) * new float[] { 0f, 0f, .5f, 1.5f }[i] * .5f);
            necroticStrike.ignoresMeleeDps = true;
            necroticStrike.ignoresRangedDps = true;
            necroticStrike.ignoresHealers = true;
            get(i).getAttacks().add(necroticStrike);
            Attack shadowCleave = new Attack();
            shadowCleave.name = "Shadow Cleave";
            shadowCleave.damageType = ItemDamageType.Shadow;
            shadowCleave.attackType = AttackTypes.AT_MELEE;
            shadowCleave.setDamagePerHit(new int[] { (14250 + 15750), (15913 + 17587), (15913 + 17587), (19000 + 21000) }[i] / 2f);
            shadowCleave.maxNumTargets = 1f;
            shadowCleave.attackSpeed = ((new float[] { 60, 60, 45, 45 }[i] / (35f / 6f)) * new float[] { 1.5f, 3.5f, 1.5f, 3.5f }[i] * .5f) + ((new float[] { 60, 60, 45, 45 }[i] / (35f / 6f)) * new float[] { 0f, 0f, .5f, 1.5f }[i] * .5f);
            shadowCleave.dodgable = true;
            shadowCleave.missable = true;
            shadowCleave.parryable = true;
            shadowCleave.blockable = true;
            shadowCleave.ignoresMeleeDps = true;
            shadowCleave.ignoresRangedDps = true;
            shadowCleave.ignoresHealers = true;
            get(i).getAttacks().add(shadowCleave);
            TargetGroup shades = new TargetGroup();
            shades.frequency = 30f;
            shades.duration = 7f * 1000;
            shades.numTargs = new float[] { 1, 3, 1, 3 }[i];
            shades.chance = .5f;
            shades.nearBoss = false;
            get(i).targets.add(shades);
            Attack vengeanceNormal = new Attack();
            vengeanceNormal.name = "Vengeful Blast on Normal";
            vengeanceNormal.damageType = ItemDamageType.Shadow;
            vengeanceNormal.attackType = AttackTypes.AT_AOE;
            vengeanceNormal.setDamagePerHit(new int[] { (17853 + 19147), (19300 + 20700), 0, 0 }[i] / 2f);
            vengeanceNormal.maxNumTargets = new float[] { 1, 3, 1, 3 }[i];
            vengeanceNormal.attackSpeed = 30f;
            get(i).getAttacks().add(vengeanceNormal);
            Attack vengeanceHeroic = new Attack();
            vengeanceHeroic.name = "Vengeful Blast on Heroic";
            vengeanceHeroic.damageType = ItemDamageType.Shadow;
            vengeanceHeroic.attackType = AttackTypes.AT_AOE;
            vengeanceHeroic.setDamagePerHit(new int[] { 0, 0, (17370 + 18630), (23160 + 24840) }[i] / 2f);
            vengeanceHeroic.maxNumTargets = get(i).getMaxPlayers();
            vengeanceHeroic.attackSpeed = 30f;
            vengeanceHeroic.interruptable = true;
            get(i).getAttacks().add(vengeanceHeroic);
            Impedance shadeMove = new Impedance();
            shadeMove.frequency = 30f;
            shadeMove.duration = 7f * 1000;
            shadeMove.chance = new int[] { 0, 0, 1, 3 }[i] / (get(i).getMaxPlayers() - get(i).getMinTanks());
            shadeMove.breakable = false;
            get(i).moves.add(shadeMove);
        }
        for (int i = 0; i < 4; i++) {
            Impedance dominateMind = new Impedance();
            dominateMind.frequency = get(i).getBerserkTimer() / ((get(i).getBerserkTimer() - 30f) / 40f);
            dominateMind.duration = 12f * 1000f;
            dominateMind.chance = new int[] { 0, (1 / (get(i).getMaxPlayers() - get(i).getMinTanks())), (1 / (get(i).getMaxPlayers() - get(i).getMinTanks())), (3 / (get(i).getMaxPlayers() - get(i).getMinTanks())) }[i];
            dominateMind.breakable = false;
            get(i).stuns.add(dominateMind);
        }
        setTimeBossIsInvuln(new float[] { 0.00f, 0.00f, 0.00f, 0.00f });
    }
}
