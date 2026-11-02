// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import rawr.base.util.Utilities;

import java.util.ArrayList;
import java.util.List;

public abstract class Rotation {

    public static Iterable<Ability[]> getAllRotations() {
        List<Ability> abilities = new ArrayList<Ability>();

        for (int ability = 0; ability <= Ability.values().length - 1; ability++)
            abilities.add(Ability.values()[ability]);

        return Utilities.getDifferentElementPermutations(abilities);
    }

    protected Rotation(CombatStats combats) {
        if (combats == null)
            throw new IllegalArgumentException("combats");

        this.combats = combats;
        cs = combats.getTalents().getCrusaderStrike() == 0 ? (Skill) new NullCrusaderStrike(combats) : (Skill) new CrusaderStrike(combats);
        ds = combats.getTalents().getDivineStorm() == 0 ? (Skill) new NullCrusaderStrike(combats) : (Skill) new DivineStorm(combats);
        exo = new Exorcism(combats);
        how = new HammerOfWrath(combats);
        cons = new Consecration(combats);
        white = new White(combats);
        hor = new HandOfReckoning(combats);

        switch (combats.getCalcOpts().getSeal()) {
            case Righteousness:
                seal = new SealOfRighteousness(combats);
                sealDot = new NullSealDoT(combats);
                judge = new JudgementOfRighteousness(combats);
                break;

            case Command:
                if (combats.getTalents().getSealOfCommand() == 0) {
                    seal = new NullSeal(combats);
                    sealDot = new NullSealDoT(combats);
                    judge = new NullJudgement(combats);
                    break;
                }
                seal = new SealOfCommand(combats);
                sealDot = new NullSealDoT(combats);
                judge = new JudgementOfCommand(combats);
                break;

            case Vengeance: {
                float stack = averageSoVStackSize();
                seal = new SealOfVengeance(combats, stack);
                sealDot = new SealOfVengeanceDoT(combats, stack);
                judge = new JudgementOfVengeance(combats, stack);
                break;
            }

            default:
                seal = new NullSeal(combats);
                sealDot = new NullSealDoT(combats);
                judge = new NullJudgement(combats);
                break;
        }
    }

    public final Skill cs;
    public final Skill judge;
    public final Skill ds;
    public final Skill exo;
    public final Skill how;
    public final Skill cons;
    public final Skill seal;
    public final Skill sealDot;
    public final Skill hor;
    public final White white;
    public final CombatStats combats;

    public abstract void setCharacterCalculations(CharacterCalculationsRetribution calc);

    public void setDPS(CharacterCalculationsRetribution calc) {
        setCharacterCalculations(calc);

        calc.averageSoVStack = averageSoVStackSize();
        calc.soVOvertake = soVOvertakeTime();

        calc.whiteDPS = white.whiteDPS();
        calc.sealDPS = sealDPS(seal, sealDot);
        calc.judgementDPS = getAbilityDps(judge);
        calc.divineStormDPS = getAbilityDps(ds);
        calc.crusaderStrikeDPS = getAbilityDps(cs);
        calc.consecrationDPS = getAbilityDps(cons);
        calc.exorcismDPS = getAbilityDps(exo);
        calc.hammerOfWrathDPS = getAbilityDps(how);
        calc.handOfReckoningDPS = handOfReckoningDPS(hor);

        calc.whiteSkill = white;
        calc.sealSkill = seal;
        calc.judgementSkill = judge;
        calc.divineStormSkill = ds;
        calc.crusaderStrikeSkill = cs;
        calc.consecrationSkill = cons;
        calc.exorcismSkill = exo;
        calc.hammerOfWrathSkill = how;
        calc.handOfReckoningSkill = hor;

        calc.setDPSPoints(
                calc.whiteDPS +
                        calc.sealDPS +
                        calc.judgementDPS +
                        calc.crusaderStrikeDPS +
                        calc.divineStormDPS +
                        calc.exorcismDPS +
                        calc.handOfReckoningDPS +
                        calc.consecrationDPS +
                        calc.hammerOfWrathDPS +
                        calc.otherDPS);
    }

    public float dps() {
        return
                white.whiteDPS() +
                        sealDPS(seal, sealDot) +
                        getAbilityDps(judge) +
                        getAbilityDps(ds) +
                        getAbilityDps(cs) +
                        getAbilityDps(cons) +
                        getAbilityDps(exo) +
                        getAbilityDps(how) +
                        handOfReckoningDPS(hor);
    }

    public float sealProcsPerSec(Skill seal) {
        if (seal instanceof SealOfVengeance)
            return getMeleeAttacksPerSec();
        else
            return getMeleeAttacksPerSec() + getAbilityHitsPerSecond(judge);
    }

    public float soVOvertakeTime() {
        float sov0dps = getAbilityDps(new JudgementOfVengeance(combats, 0));
        float sov5dps = getAbilityDps(new JudgementOfVengeance(combats, 5))
                + sealDPS(new SealOfVengeance(combats, 5), new SealOfVengeanceDoT(combats, 5));
        float sordps = getAbilityDps(new JudgementOfRighteousness(combats))
                + sealDPS(new SealOfRighteousness(combats), new NullSealDoT(combats));

        if (sordps > sov0dps) {
            float averageStack = (sordps - sov0dps) / (sov5dps - sov0dps) * 5f;
            float timeToMaxStack = combats.attackSpeed * 4f;
            return 2.5f * timeToMaxStack / (5f - averageStack);
        } else {
            return 0;
        }
    }

    public float averageSoVStackSize() {
        float averageTimeOnMob = combats.getCalcOpts().getFightLength() * 60f / (combats.getCalcOpts().getTargetSwitches() + 1);
        float timeToMaxStack = combats.attackSpeed * 4f;
        if (averageTimeOnMob > timeToMaxStack) {
            return (2.5f * timeToMaxStack + 5f * (averageTimeOnMob - timeToMaxStack)) / averageTimeOnMob;
        } else {
            return 2.5f * averageTimeOnMob / timeToMaxStack;
        }
    }

    public float sealDPS(Skill seal, Skill sealdot) {
        return sealdot.averageDamage() / 3f + seal.averageDamage() * sealProcsPerSec(seal);
    }

    public float handOfReckoningDPS(Skill hor) {
        return hor.averageDamage() / 8f * combats.getCalcOpts().getHoREff();
    }

    public float getAbilityDps(Skill skill) {
        return skill.averageDamage() * getAbilityUsagePerSecond(skill);
    }

    public float getAbilityHitsPerSecond(Skill skill) {
        return
                getAbilityUsagePerSecond(skill) *
                        skill.chanceToLand() *
                        skill.targets() *
                        skill.tickCount();
    }

    public float getAbilityCritsPerSecond(Skill skill) {
        return
                getAbilityUsagePerSecond(skill) *
                        skill.chanceToCrit() *
                        skill.targets() *
                        skill.tickCount();
    }

    public float getCrusaderStrikeCD() {
        return 1 / getAbilityUsagePerSecond(cs);
    }

    public float getJudgementCD() {
        return 1 / getAbilityUsagePerSecond(judge);
    }

    public abstract float getAbilityUsagePerSecond(Skill skill);

    public float getMeleeAttacksPerSec() {
        // Melee hit procs can be triggered by:
        // - Crusader Strike hits
        // - Divine Storm hits
        // - Weapon swing hits
        // - Tiny Abomination in a Jar releasing attack hits
        // (2 multiplier needs to be moved to another place)
        // - Judgement hits

        return
                getAbilityHitsPerSecond(cs) +
                        getAbilityHitsPerSecond(ds) +
                        white.chanceToLand() / combats.attackSpeed +
                        combats.getStats().getMoteOfAnger() * 2 * white.chanceToLand() +
                        getAbilityHitsPerSecond(judge);
    }

    public float getPhysicalAttacksPerSec() {
        // Physical hit procs can be triggered by:
        // - Crusader Strike hits
        // - Divine Storm hits
        // - Weapon swing hits
        // - Tiny Abomination in a Jar releasing attack hits
        // (2 multiplier needs to be moved to another place)
        // - Judgement hits
        // - Hammer of Wrath hits

        return
                getAbilityHitsPerSecond(cs) +
                        getAbilityHitsPerSecond(ds) +
                        white.chanceToLand() / combats.attackSpeed +
                        combats.getStats().getMoteOfAnger() * 2 * white.chanceToLand() +
                        getAbilityHitsPerSecond(judge) +
                        getAbilityHitsPerSecond(how);
    }

    public float getSpellAttacksPerSec() {
        // Spell hit procs can be triggered by:
        // - Exorcism hits
        // - Consecration hits (first tick)
        return
                getAbilityHitsPerSecond(exo) +
                        getAbilityHitsPerSecond(cons) / cons.tickCount();
    }

    public float getMeleeCritsPerSec() {
        // Melee crit procs can be triggered by:
        // - Crusader Strike crits
        // - Divine Storm crits on each target
        // - Weapon swing crits
        // - Tiny Abomination in a Jar releasing attack crits
        // (2 multiplier needs to be moved to another place)
        // - Judgement crits

        return
                getAbilityCritsPerSecond(cs) +
                        getAbilityCritsPerSecond(ds) +
                        white.chanceToCrit() / combats.attackSpeed +
                        combats.getStats().getMoteOfAnger() * 2 * white.chanceToCrit() +
                        getAbilityCritsPerSecond(judge);
    }

    public float getPhysicalCritsPerSec() {
        // Physical crit procs can be triggered by:
        // - Crusader Strike crits
        // - Divine Storm crits on each target
        // - Weapon swing crits
        // - Tiny Abomination in a Jar releasing attack crits
        // (2 multiplier needs to be moved to another place)
        // - Judgement crits
        // - Hammer of Wrath crits

        return
                getAbilityCritsPerSecond(cs) +
                        getAbilityCritsPerSecond(ds) +
                        white.chanceToCrit() / combats.attackSpeed +
                        combats.getStats().getMoteOfAnger() * 2 * white.chanceToCrit() +
                        getAbilityCritsPerSecond(judge) +
                        getAbilityCritsPerSecond(how);
    }

    public float getSpellCritsPerSec() {
        // Spell hit procs can be triggered by:
        // - Exorcism crits
        // - Consecration crits (first tick)
        return
                getAbilityCritsPerSecond(exo) +
                        getAbilityCritsPerSecond(cons) / cons.tickCount();
    }

    public float getAttacksPerSec() {
        // Damage done procs and damage or healing done procs can be triggered by:
        // - Crusader Strike hits
        // - Divine Storm hits
        // - Weapon swing hits
        // - Tiny Abomination in a Jar releasing attack hits
        // (2 multiplier needs to be moved to another place)
        // - Judgement hits
        // - Hammer of Wrath hits
        // - Consecration damage ticks
        // - Exorcism hits

        return
                getAbilityHitsPerSecond(cs) +
                        getAbilityHitsPerSecond(ds) +
                        white.chanceToLand() / combats.attackSpeed +
                        combats.getStats().getMoteOfAnger() * 2 * white.chanceToLand() +
                        getAbilityHitsPerSecond(judge) +
                        getAbilityHitsPerSecond(how) +
                        getAbilityHitsPerSecond(cons) +
                        getAbilityHitsPerSecond(exo);
    }
}
