// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Rotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (the non-RAWR3 branch: FightLength = CalcOpts.Length * 60f).
package rawr.models.healadin;

import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class Rotation {
    private Character character;

    public Character getCharacter() {
        return this.character;
    }

    public void setCharacter(Character value) {
        this.character = value;
        this.talents = this.character.getPaladinTalents();

        this.calcOpts = this.character.getCalculationOptions() instanceof CalculationOptionsHealadin
                ? (CalculationOptionsHealadin) this.character.getCalculationOptions()
                : new CalculationOptionsHealadin();
    }

    private PaladinTalents talents;

    public PaladinTalents getTalents() {
        return this.talents;
    }

    private CalculationOptionsHealadin calcOpts;

    public CalculationOptionsHealadin getCalcOpts() {
        return this.calcOpts;
    }

    private Stats stats;

    public Stats getStats() {
        return this.stats;
    }

    public void setStats(Stats value) {
        this.stats = value;
    }

    public float divinePleas;
    public float fightLength;
    public float foLCasts;

    private FlashOfLight fol;
    private HolyLight hl;
    private HolyShock hs;
    private SacredShield ss;
    private JudgementsOfThePure jotp;
    private BeaconOfLight bol;

    public Rotation(Character character, Stats stats) {
        setCharacter(character);
        fightLength = this.calcOpts.getLength() * 60f;
        this.stats = stats;
        fol = new FlashOfLight(this);
        hl = new HolyLight(this);
        hs = new HolyShock(this);
        ss = new SacredShield(this);
        jotp = new JudgementsOfThePure(this, this.calcOpts.getJudgement());
        bol = new BeaconOfLight(this);
    }

    public float manaPool(CharacterCalculationsHealadin calc) {
        divinePleas = (float) Math.ceil((fightLength - 60f) / (60f * this.calcOpts.getDivinePlea()));

        calc.manaBase = this.stats.getMana();
        calc.manaLayOnHands = 1950 * ((this.talents.isGlyphOfDivinity() ? 1 : 0) + (this.calcOpts.getLoHSelf() ? 1 : 0)) * (this.talents.isGlyphOfDivinity() ? 2f : 1f);
        calc.manaArcaneTorrent = (this.character.getRace() == CharacterRace.BloodElf ? this.stats.getMana() * .06f * (float) Math.ceil(fightLength / 120f - .25f) : 0);
        calc.manaDivinePlea = this.stats.getMana() * .25f * divinePleas;
        calc.manaMp5 = fightLength * this.stats.getMp5() / 5f;
        calc.manaReplenishment = this.stats.getManaRestoreFromMaxManaPerSecond() * this.stats.getMana() * fightLength * this.calcOpts.getReplenishment();
        calc.manaOther += this.stats.getManaRestore();
        if (this.stats.getHighestStat() > 0) {
            float greatnessMana = this.stats.getHighestStat() * StatConversion.RATING_PER_MANA;
            calc.manaReplenishment += this.stats.getManaRestoreFromMaxManaPerSecond() * fightLength * greatnessMana * this.calcOpts.getReplenishment(); // Replenishment
            calc.manaDivinePlea += divinePleas * greatnessMana * .25f; // Divine Plea
        }
        return calc.manaBase + calc.manaDivinePlea + calc.manaMp5 + calc.manaOther +
                calc.manaReplenishment + calc.manaLayOnHands;
    }

    public static float getHolyLightCastsPerSec(CharacterCalculationsHealadin calc) {
        return (calc.rotationHL / calc.hl.castTime()) / calc.fightLength;
    }

    public static float getHealingCastsPerSec(CharacterCalculationsHealadin calc) {
        return (calc.rotationHL / calc.hl.castTime()
                + calc.rotationFoL / calc.foL.castTime()
                + calc.rotationHS / calc.hS.castTime()) / calc.fightLength;
    }

    public static float getSpellCastsPerSec(CharacterCalculationsHealadin calc) {
        return getHealingCastsPerSec(calc)
                + (calc.rotationBoL / calc.boL.castTime()
                + calc.rotationJotP / calc.jotP.castTime()
                + calc.rotationSS / calc.sS.castTime()) / calc.fightLength;
    }

    public static float getSpellCritsPerSec(CharacterCalculationsHealadin calc) {
        return getHealingCritsPerSec(calc);
    }

    public static float getHealingCritsPerSec(CharacterCalculationsHealadin calc) {
        return (calc.rotationHL / calc.hl.castTime() * calc.hl.chanceToCrit()
                + calc.rotationFoL / calc.foL.castTime() * calc.foL.chanceToCrit()
                + calc.rotationHS / calc.hS.castTime() * calc.hS.chanceToCrit()) / calc.fightLength;
    }

    public float calculateFightHealing(CharacterCalculationsHealadin calc) {
        float fol_BaseHealed = 0f;

        // region Copying Stuff to Calc
        calc.fightLength = fightLength;

        calc.hl = hl;
        calc.foL = fol;
        calc.hS = hs;
        calc.sS = ss;
        calc.jotP = jotp;
        calc.boL = bol;

        calc.rotationJotP = jotp.time();
        calc.usageJotP = jotp.usage();

        calc.rotationSS = ss.time();
        calc.usageSS = ss.usage();
        calc.healedSS = ss.totalAborb();

        calc.rotationBoL = bol.time();
        calc.usageBoL = bol.usage();

        calc.rotationHS = hs.time();
        calc.healedHS = hs.healed();
        calc.usageHS = hs.usage();
        // endregion

        // region Divine Illumination
        if (this.talents.getDivineIllumination() > 0) {
            DivineIllumination di = new DivineIllumination(this);
            calc.rotationHL += di.time();
            calc.usageHL += di.usage();
            calc.healedHL += di.healed();
        }
        // endregion

        // region Divine Favor
        if (this.talents.getDivineFavor() > 0) {
            DivineFavor df = new DivineFavor(this);
            calc.rotationHL += df.time();
            calc.usageHL += df.usage();
            calc.healedHL += df.healed();
        }
        // endregion

        // region Infusion of Light

        float iol_hlcasts = 0;
        float iol_folcasts = 0;
        if (this.calcOpts.getInfusionOfLight()) {
            float iol_count = hs.casts() * hs.chanceToCrit();

            HolyLight hl_iol = new HolyLight(this);
            hl_iol.setExtraCritChance(.1f * this.talents.getInfusionOfLight());
            if (this.stats.getHolyLightCastTimeReductionFromHolyShock() > 0) {
                hl_iol.setCastTimeReductionFromHolyShock(true);
            }

            iol_hlcasts = hs.casts() * this.calcOpts.getIoLHolyLight() * hs.chanceToCrit();

            calc.usageHL += iol_hlcasts * hl_iol.averageCost();
            calc.rotationHL += iol_hlcasts * hl_iol.castTime();
            calc.healedHL += iol_hlcasts * hl_iol.averageHealed();

            iol_folcasts = hs.casts() * (1f - this.calcOpts.getIoLHolyLight()) * hs.chanceToCrit();
        }

        // endregion

        float remainingMana = calc.totalMana = manaPool(calc);
        remainingMana -= calc.usageJotP + calc.usageBoL + calc.usageHS + calc.usageHL + calc.usageFoL + calc.usageSS;

        float remainingTime = fightLength * this.calcOpts.getActivity();
        remainingTime -= calc.rotationJotP + calc.rotationBoL + calc.rotationSS + calc.rotationHS + calc.rotationFoL + calc.rotationHL;

        foLCasts = 0f;
        if (remainingMana > 0) {
            if (this.stats.getHolyLightCastTimeReductionFromHolyShock() > 0) { // Calculations for Holy Light with cost reduction from Holy Shock
                // Get the Holy Light model for cast time reduction from Holy Shock
                HolyLight hl_hs = new HolyLight(this);
                hl_hs.setCastTimeReductionFromHolyShock(true);

                // Calculate how much time we have available to cast Holy Lights after Holy Shock
                float hl_hs_time_available = Math.min(remainingTime, Math.max(0f, (remainingMana - (remainingTime * fol.mps())) / (hl_hs.mps() - fol.mps())));

                // Calculate the maximum number of casts available with the cast time reduction
                float hs_hlcasts = hs.casts() - iol_hlcasts;

                // Calculate the amount of time needed for all of the casts
                float hl_hs_time_needed = hs_hlcasts * hl_hs.castTime();

                float manaCost = 0f;
                float timeCost = 0f;
                if (hl_hs_time_available > 0 && hl_hs_time_available < hl_hs_time_needed) { // We have enough time to cast the Holy Lights needed, cast them all
                    // Calculate mana and time cost
                    manaCost = hs_hlcasts * hl_hs.averageCost();
                    timeCost = hs_hlcasts * hl_hs.castTime();

                    // Update Holy Light stats
                    calc.usageHL += manaCost;
                    calc.rotationHL += timeCost;
                    calc.healedHL += hs_hlcasts * hl_hs.averageHealed();
                } else if (hl_hs_time_available >= remainingTime) { // There's not enough time to cast the Holy Lights needed, so we'll only cast what we can
                    // Calculate mana and time cost
                    float remainingCasts = remainingTime / hl_hs.castTime();
                    manaCost = remainingCasts * hl_hs.averageCost();
                    timeCost = remainingCasts * hl_hs.castTime();

                    // Update Holy Light stats
                    calc.usageHL += manaCost;
                    calc.rotationHL += timeCost;
                    calc.healedHL += remainingCasts * hl_hs.averageHealed();
                }
                // Update remaining mana and remaining time
                remainingMana -= manaCost;
                remainingTime -= timeCost;
            }

            // Calculate how much time we have available to cast regular Holy Lights
            float hl_time_available = Math.min(remainingTime, Math.max(0f, (remainingMana - (remainingTime * fol.mps())) / (hl.mps() - fol.mps())));

            // The rest of the time will be for Flash of Light
            float fol_time = remainingTime - hl_time_available;

            if (hl_time_available == 0) { // If we didn't have any time available for Holy Lights, check to see when we run out of mana while casting Flash of Light
                fol_time = Math.min(remainingTime, remainingMana / fol.mps());
            } else { // If we do have time available for Holy Lights, update the Holy Light stats
                calc.healedHL += hl.hps() * hl_time_available;
                calc.usageHL += hl.mps() * hl_time_available;
                calc.rotationHL += hl_time_available;
            }

            // Calculate Flash of Light data
            if (iol_folcasts > 0) { // We are using Infusion of Light, so we must calculate it differently than normal
                // Get the Flash of Light model for Infusion of Light
                FlashOfLight fol_iol = new FlashOfLight(this);
                fol_iol.setInfusionOfLight(true);

                // If there is less time remaining than it would take to cast all of the
                // Infusion of Light procs, we are forced to drop some of them.  Note
                // that this should *never* happen, but is here for a sanity check.
                if (fol_time <= iol_folcasts * fol_iol.castTime()) {
                    // Determine how many total casts we are going to be able to do
                    foLCasts = fol_time / fol_iol.castTime();

                    // Adding Flash of Light with Infusion of Light stats
                    calc.usageFoL += foLCasts * fol_iol.averageCost();
                    calc.rotationFoL += foLCasts * fol_iol.castTime();
                    calc.healedFoL += foLCasts * fol_iol.averageHealed();

                    fol_BaseHealed += foLCasts * fol_iol.baseAverageHealed();
                } else {
                    // Adding Flash of Light with Infusion of Light
                    calc.usageFoL += iol_folcasts * fol_iol.averageCost();
                    calc.rotationFoL += iol_folcasts * fol_iol.castTime();
                    calc.healedFoL += iol_folcasts * fol_iol.averageHealed();

                    fol_BaseHealed += iol_folcasts * fol_iol.baseAverageHealed();

                    // Determine how much time we have left to cast Flash of Light without the procs
                    fol_time = fol_time - iol_folcasts * fol.castTime();

                    // Determine how many total casts, with and without Infusion of Light
                    float remaining_folcasts = fol_time / fol.castTime();
                    foLCasts = iol_folcasts + remaining_folcasts;

                    // Adding Flash of Light stats
                    calc.rotationFoL += fol_time;
                    calc.usageFoL += fol.mps() * fol_time;
                    calc.healedFoL += fol.hps() * fol_time;

                    fol_BaseHealed += remaining_folcasts * fol.baseAverageHealed();
                }
            } else { // We don't use Infusion of Light, so just calculate normal data for Flash of Light
                // Determine how many total casts, with and without Infusion of Light
                foLCasts = fol_time / fol.castTime();

                // Adding Flash of Light stats
                calc.rotationFoL += fol_time;
                calc.usageFoL += fol.mps() * fol_time;
                calc.healedFoL += fol.hps() * fol_time;

                fol_BaseHealed += foLCasts * fol.baseAverageHealed();
            }
        }

        calc.totalHealed = calc.healedFoL + calc.healedHL + calc.healedHS;

        if (this.talents.getBeaconOfLight() > 0) {
            calc.totalHealed += calc.healedBoL = bol.healingDone(fol_BaseHealed + calc.healedHL + calc.healedHS);
        }

        calc.totalHealed += calc.healedGHL = hl.glyphOfHolyLight(calc.healedHL);

        calc.healedOther = this.stats.getHealed();
        calc.healedOther += calc.totalHealed * this.stats.getShieldFromHealed();

        calc.totalHealed += calc.healedOther;
        calc.totalHealed += calc.healedSS;

        calc.avgHPS = calc.totalHealed / fightLength;
        calc.avgHPM = calc.totalHealed / calc.totalMana;

        return calc.avgHPS * (1f - this.calcOpts.getBurstScale());
    }

    public float calculateBurstHealing(CharacterCalculationsHealadin calc) {
        return hl.hps() * this.calcOpts.getBurstScale();
    }
}
