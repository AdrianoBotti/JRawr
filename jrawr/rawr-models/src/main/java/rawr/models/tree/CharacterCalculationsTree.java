// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Tree/CharacterCalculationsTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tree;

import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsTree extends CharacterCalculationsBase {
    private Stats basicStats;

    public Stats getBasicStats() {
        return basicStats;
    }

    public void setBasicStats(Stats value) {
        basicStats = value;
    }

    private Stats combatStats;

    public Stats getCombatStats() {
        return combatStats;
    }

    public void setCombatStats(Stats value) {
        combatStats = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getSingleTargetPoints() {
        return subPoints[0];
    }

    public void setSingleTargetPoints(float value) {
        subPoints[0] = value;
    }

    public float getSustainedPoints() {
        return subPoints[1];
    }

    public void setSustainedPoints(float value) {
        subPoints[1] = value;
    }

    public float getSurvivalPoints() {
        return subPoints[2];
    }

    public void setSurvivalPoints(float value) {
        subPoints[2] = value;
    }

    private float overallPoints;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    public SustainedResult Sustained;
    public SingleTargetBurstResult[] SingleTarget;
    private Character localCharacter;

    public Character getLocalCharacter() {
        return localCharacter;
    }

    public void setLocalCharacter(Character value) {
        localCharacter = value;
    }

    public float SingleTargetHPS;
    public float SustainedHPS;

    private static String lifebloomMethodToString(int lbStack, LifeBloomType type) {
        String result;
        if (type == LifeBloomType.Fast) {
            switch (lbStack) {
                case 0: result = "Unused"; break;
                case 1: result = "Single blooms"; break;
                case 2: result = "Fast Double blooms"; break;
                case 3: result = "Fast Triple blooms"; break;
                case 4:
                default: result = "Stack"; break;
            }
        } else if (type == LifeBloomType.Slow) {
            switch (lbStack) {
                case 0: result = "Unused"; break;
                case 1: result = "Single blooms"; break;
                case 2: result = "Slow Double blooms"; break;
                case 3: result = "Slow Triple blooms"; break;
                case 4:
                default: result = "Stack"; break;
            }
        } else result = "Rolling stack";

        return result;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        dictValues.put("Single Target Points", TreeFormat.netFloatToString(getSingleTargetPoints()));
        dictValues.put("Sustained Points", TreeFormat.netFloatToString(getSustainedPoints()));
        dictValues.put("Survival Points", TreeFormat.netFloatToString(getSurvivalPoints()));
        dictValues.put("Overall Points", TreeFormat.netFloatToString(getOverallPoints()));

        dictValues.put("Base Health", TreeFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Base Armor", TreeFormat.roundToString(basicStats.getArmor(), 0) + "*Reduces damage taken by " + TreeFormat.roundToString(StatConversion.getArmorDamageReduction(83, basicStats.getArmor(), 0, 0, 0) * 100.0f, 2) + "%");
        dictValues.put("Base Mana", TreeFormat.netFloatToString(basicStats.getMana()));
        dictValues.put("Base Stamina", TreeFormat.netFloatToString(basicStats.getStamina()));
        dictValues.put("Base Intellect", TreeFormat.netFloatToString(basicStats.getIntellect()));
        dictValues.put("Base Spirit", TreeFormat.netFloatToString(basicStats.getSpirit()));
        dictValues.put("Base Spell Power", TreeFormat.netFloatToString(basicStats.getSpellPower()) + "*" + TreeFormat.netFloatToString(basicStats.getSpirit() * localCharacter.getDruidTalents().getImprovedTreeOfLife() * 0.05f) + " from Improved Tree of Life");
        dictValues.put("Base Spell Crit", TreeFormat.netFloatToString(basicStats.getSpellCrit()));

        float speed_from_hr = (1f + StatConversion.getSpellHasteFromRating(basicStats.getHasteRating()));
        float speed_from_sh = (1f + basicStats.getSpellHaste());
        float speed = speed_from_hr * speed_from_sh;
        float hard = (1.5f / (1f * speed_from_sh) - 1) * StatConversion.RATING_PER_SPELLHASTE;
        float untilcap = hard - basicStats.getHasteRating();

        dictValues.put("Base Spell Haste", TreeFormat.roundToString((speed - 1.0f) * 100.0f, 2) + "%*" + TreeFormat.roundToString((speed_from_sh - 1.0f) * 100.0f, 2) + "% from spell effects and talents\n" + TreeFormat.roundToString((speed_from_hr - 1.0f) * 100.0f, 2) + "% from " + TreeFormat.netFloatToString(basicStats.getHasteRating()) + " haste rating");
        // Use Nourish cast time to equal normal GCD
        Spell spell = new Nourish(localCharacter, basicStats);
        dictValues.put("Base Global CD", TreeFormat.roundToString(spell.gcd, 2) + " sec*" + TreeFormat.ceilingToString(untilcap) + " Haste Rating until hard gcd cap");

        dictValues.put("Health", TreeFormat.netFloatToString(combatStats.getHealth()));
        dictValues.put("Armor", TreeFormat.roundToString(combatStats.getArmor(), 0) + "*Reduces damage taken by " + TreeFormat.roundToString(StatConversion.getArmorDamageReduction(83, combatStats.getArmor(), 0, 0, 0) * 100.0f, 2) + "%");
        dictValues.put("Mana", TreeFormat.netFloatToString(combatStats.getMana()));
        dictValues.put("Stamina", TreeFormat.netFloatToString(combatStats.getStamina()));
        dictValues.put("Intellect", TreeFormat.netFloatToString(combatStats.getIntellect()));
        dictValues.put("Spirit", TreeFormat.netFloatToString(combatStats.getSpirit()));
        dictValues.put("Spell Power", TreeFormat.netFloatToString(combatStats.getSpellPower()) + "*" + TreeFormat.netFloatToString(combatStats.getSpirit() * localCharacter.getDruidTalents().getImprovedTreeOfLife() * 0.05f) + " from Improved Tree of Life");
        dictValues.put("Spell Crit", TreeFormat.netFloatToString(combatStats.getSpellCrit()));

        speed_from_hr = (1f + StatConversion.getSpellHasteFromRating(combatStats.getHasteRating()));
        speed_from_sh = (1f + combatStats.getSpellHaste());
        speed = speed_from_hr * speed_from_sh;
        hard = (1.5f / (1f * speed_from_sh) - 1) * StatConversion.RATING_PER_SPELLHASTE;
        untilcap = hard - combatStats.getHasteRating();

        dictValues.put("Spell Haste", TreeFormat.roundToString((speed - 1.0f) * 100.0f, 2) + "%*" + TreeFormat.roundToString((speed_from_sh - 1.0f) * 100.0f, 2) + "% from spell effects and talents\n" + TreeFormat.roundToString((speed_from_hr - 1.0f) * 100.0f, 2) + "% from " + TreeFormat.netFloatToString(combatStats.getHasteRating()) + " haste rating");
        // Use Nourish cast time to equal normal GCD
        spell = Sustained.spellMix.nourish[0];
        dictValues.put("Global CD", TreeFormat.roundToString(spell.gcd, 2) + " sec*" + TreeFormat.ceilingToString(untilcap) + " Haste Rating until soft gcd cap");

        dictValues.put("Total Time", TreeFormat.roundToString(Sustained.TotalTime, 2));
        dictValues.put("Time until OOM (unreduced)", TreeFormat.roundToString(Sustained.TimeToOOM_unreduced, 2));
        dictValues.put("Time until OOM", TreeFormat.roundToString(Sustained.getTimeToOOM(), 2));
        dictValues.put("Total healing done", TreeFormat.roundToString(Sustained.TotalTime * SustainedHPS, 2)); // Has extra component from procs
        dictValues.put("Sustained HPS", TreeFormat.roundToString(SustainedHPS, 2));
        dictValues.put("Single Target HPS", TreeFormat.roundToString(SingleTargetHPS, 2));
        dictValues.put("Mana regen per second", TreeFormat.roundToString(Sustained.getManaRegen(), 2));
        dictValues.put("Mana from innervates", TreeFormat.roundToString(Sustained.getInnervateMana(), 2));
        dictValues.put("Average casts per minute", TreeFormat.roundToString(Sustained.spellMix.getCastsPerMinute(), 2));
        dictValues.put("Average crits per minute", TreeFormat.roundToString(Sustained.spellMix.getCritsPerMinute(), 2));
        dictValues.put("Average heals per minute", TreeFormat.roundToString(Sustained.spellMix.getHealsPerMinute(), 2));
        dictValues.put("Rejuvenation casts per minute", TreeFormat.roundToString(Sustained.spellMix.getRejuvCPM(), 2));
        dictValues.put("Rejuvenation average up", TreeFormat.roundToString(Sustained.spellMix.getRejuvAvg(), 2));
        dictValues.put("Regrowth casts per minute", TreeFormat.roundToString(Sustained.spellMix.getRegrowthCPM(), 2));
        dictValues.put("Regrowth average up", TreeFormat.roundToString(Sustained.spellMix.getRegrowthAvg(), 2));
        dictValues.put("Lifebloom (stack) casts per minute", TreeFormat.roundToString(Sustained.spellMix.getLifebloomStackCPM(), 2));
        dictValues.put("Lifebloom (stack) average up", TreeFormat.roundToString(Sustained.spellMix.getLifebloomStackAvg(), 2));
        dictValues.put("Lifebloom (stack) method", lifebloomMethodToString(3, Sustained.rotSettings.lifeBloomType));
        dictValues.put("Lifebloom casts per minute", TreeFormat.roundToString(Sustained.spellMix.getLifebloomCPM(), 2));
        dictValues.put("Lifebloom average up", TreeFormat.roundToString(Sustained.spellMix.getLifebloomAvg(), 2));
        dictValues.put("Nourish casts per minute", TreeFormat.roundToString(Sustained.spellMix.getNourishCPM(), 2));
        // dictValues.Add("Healing Touch casts per minute", Math.Round(, 2).ToString());
        dictValues.put("Swiftmend casts per minute", TreeFormat.roundToString(Sustained.spellMix.getSwiftmendCPM(), 2));
        dictValues.put("Wild Growth casts per minute", TreeFormat.roundToString(Sustained.spellMix.getWildGrowthCPM(), 2));
        dictValues.put("Revitalize procs per minute", TreeFormat.roundToString(Sustained.spellMix.getRevitalizeProcsPerMinute(), 2));

        dictValues.put("RJ Heal per tick", TreeFormat.roundToString(Sustained.spellMix.rejuvenate.getPeriodicTickwithCrit(), 2));
        dictValues.put("RJ Tick time", TreeFormat.roundToString(Sustained.spellMix.rejuvenate.getPeriodicTickTime(), 2));
        dictValues.put("RJ HPS", TreeFormat.roundToString(Sustained.spellMix.rejuvenate.getHPS(), 2));
        dictValues.put("RJ HPM", TreeFormat.roundToString(Sustained.spellMix.rejuvenate.getHPM(), 2));
        dictValues.put("RJ HPCT", TreeFormat.roundToString(Sustained.spellMix.rejuvenate.getHPCT(), 2));

        dictValues.put("RG Heal", Sustained.spellMix.regrowth.toString());
        dictValues.put("RG Tick", TreeFormat.roundToString(Sustained.spellMix.regrowth.getPeriodicTickwithCrit(), 2));
        dictValues.put("RG HPS", TreeFormat.roundToString(Sustained.spellMix.regrowth.getHPS(), 2));
        dictValues.put("RG HPS (HoT only)", TreeFormat.roundToString(Sustained.spellMix.regrowth.getHPS_HOT(), 2));
        dictValues.put("RG HPM", TreeFormat.roundToString(Sustained.spellMix.regrowth.getHPM(), 2));
        dictValues.put("RG HPCT", TreeFormat.roundToString(Sustained.spellMix.regrowth.getHPCT(), 2));
        dictValues.put("RG Heal (spam)", Sustained.spellMix.regrowthAgain.toString());
        dictValues.put("RG HPS (spam)", TreeFormat.roundToString(Sustained.spellMix.regrowthAgain.getHPCT_DH(), 2));
        dictValues.put("RG HPM (spam)", TreeFormat.roundToString(Sustained.spellMix.regrowthAgain.getHPM_DH(), 2));
        dictValues.put("RG HPCT (spam)", TreeFormat.roundToString(Sustained.spellMix.regrowthAgain.getHPCT_DH(), 2));

        dictValues.put("LB Tick", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getPeriodicTick(), 2));
        dictValues.put("LB Bloom Heal", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getAverageHealingwithCrit(), 2));
        dictValues.put("LB HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getHPS(), 2));
        dictValues.put("LB HPS (HoT only)", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getHPS_HOT(), 2));
        dictValues.put("LB HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getHPM(), 2));
        dictValues.put("LB HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloom.getHPCT(), 2));

        dictValues.put("LBx2 (fast stack) HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloomFast2Stack.getHPS(), 2));
        dictValues.put("LBx2 (fast stack) HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloomFast2Stack.getHPM(), 2));
        dictValues.put("LBx2 (fast stack) HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloomFast2Stack.getHPCT(), 2));
        dictValues.put("LBx3 (fast stack) HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloomFastStack.getHPS(), 2));
        dictValues.put("LBx3 (fast stack) HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloomFastStack.getHPM(), 2));
        dictValues.put("LBx3 (fast stack) HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloomFastStack.getHPCT(), 2));
        dictValues.put("LBx2 (slow stack) HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlow2Stack.getHPS(), 2));
        dictValues.put("LBx2 (slow stack) HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlow2Stack.getHPM(), 2));
        dictValues.put("LBx2 (slow stack) HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlow2Stack.getHPCT(), 2));
        dictValues.put("LBx3 (slow stack) HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlowStack.getHPS(), 2));
        dictValues.put("LBx3 (slow stack) HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlowStack.getHPM(), 2));
        dictValues.put("LBx3 (slow stack) HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloomSlowStack.getHPCT(), 2));
        dictValues.put("LBx3 (rolling) Tick", TreeFormat.roundToString(Sustained.spellMix.lifebloomRollingStack.getPeriodicTick(), 2));
        dictValues.put("LBx3 (rolling) HPS", TreeFormat.roundToString(Sustained.spellMix.lifebloomRollingStack.getHPS(), 2));
        dictValues.put("LBx3 (rolling) HPM", TreeFormat.roundToString(Sustained.spellMix.lifebloomRollingStack.getHPM(), 2));
        dictValues.put("LBx3 (rolling) HPCT", TreeFormat.roundToString(Sustained.spellMix.lifebloomRollingStack.getHPCT(), 2));

        dictValues.put("HT Heal", Sustained.spellMix.healingTouch.toString());
        dictValues.put("HT HPS", TreeFormat.roundToString(Sustained.spellMix.healingTouch.getHPCT_DH(), 2));
        dictValues.put("HT HPM", TreeFormat.roundToString(Sustained.spellMix.healingTouch.getHPM_DH(), 2));
        dictValues.put("HT HPCT", TreeFormat.roundToString(Sustained.spellMix.healingTouch.getHPCT_DH(), 2));

        dictValues.put("WG first Tick", TreeFormat.roundToString(Sustained.spellMix.wildGrowth.getTick()[0], 2));
        dictValues.put("WG HPS(single target)", TreeFormat.roundToString(Sustained.spellMix.wildGrowth.getHPS(), 2));
        dictValues.put("WG HPM(single target)", TreeFormat.roundToString(Sustained.spellMix.wildGrowth.getHPM(), 2));
        dictValues.put("WG HPS(max)", TreeFormat.roundToString(Sustained.spellMix.wildGrowth.getHPS() * Sustained.spellMix.wildGrowth.maxTargets, 2));
        dictValues.put("WG HPM(max)", TreeFormat.roundToString(Sustained.spellMix.wildGrowth.getHPM() * Sustained.spellMix.wildGrowth.maxTargets, 2));

        dictValues.put("N Heal", Sustained.spellMix.nourish[0].toString());
        dictValues.put("N HPM", TreeFormat.roundToString(Sustained.spellMix.nourish[0].getHPM_DH(), 2));
        dictValues.put("N HPS", TreeFormat.roundToString(Sustained.spellMix.nourish[0].getHPCT_DH(), 2));
        dictValues.put("N HPCT", TreeFormat.roundToString(Sustained.spellMix.nourish[0].getHPCT_DH(), 2));
        dictValues.put("N (1 HoT) Heal", Sustained.spellMix.nourish[1].toString());
        dictValues.put("N (1 HoT) HPM", TreeFormat.roundToString(Sustained.spellMix.nourish[1].getHPM_DH(), 2));
        dictValues.put("N (1 HoT) HPS", TreeFormat.roundToString(Sustained.spellMix.nourish[1].getHPCT_DH(), 2));
        dictValues.put("N (1 HoT) HPCT", TreeFormat.roundToString(Sustained.spellMix.nourish[1].getHPCT_DH(), 2));
        dictValues.put("N (2 HoTs) Heal", Sustained.spellMix.nourish[2].toString());
        dictValues.put("N (2 HoTs) HPM", TreeFormat.roundToString(Sustained.spellMix.nourish[2].getHPM_DH(), 2));
        dictValues.put("N (2 HoTs) HPS", TreeFormat.roundToString(Sustained.spellMix.nourish[2].getHPCT_DH(), 2));
        dictValues.put("N (2 HoTs) HPCT", TreeFormat.roundToString(Sustained.spellMix.nourish[2].getHPCT_DH(), 2));
        dictValues.put("N (3 HoTs) Heal", Sustained.spellMix.nourish[3].toString());
        dictValues.put("N (3 HoTs) HPM", TreeFormat.roundToString(Sustained.spellMix.nourish[3].getHPM_DH(), 2));
        dictValues.put("N (3 HoTs) HPS", TreeFormat.roundToString(Sustained.spellMix.nourish[3].getHPCT_DH(), 2));
        dictValues.put("N (3 HoTs) HPCT", TreeFormat.roundToString(Sustained.spellMix.nourish[3].getHPCT_DH(), 2));
        dictValues.put("N (4 HoTs) Heal", Sustained.spellMix.nourish[4].toString());
        dictValues.put("N (4 HoTs) HPM", TreeFormat.roundToString(Sustained.spellMix.nourish[4].getHPM_DH(), 2));
        dictValues.put("N (4 HoTs) HPS", TreeFormat.roundToString(Sustained.spellMix.nourish[4].getHPCT_DH(), 2));
        dictValues.put("N (4 HoTs) HPCT", TreeFormat.roundToString(Sustained.spellMix.nourish[4].getHPCT_DH(), 2));

        Swiftmend swift = new Swiftmend(localCharacter, combatStats, new Rejuvenation(localCharacter, combatStats), null);
        dictValues.put("SM Rejuv Heal", swift.toString());
        dictValues.put("SM Rejuv HPM", TreeFormat.roundToString(swift.getHPM(), 2));
        dictValues.put("SM Rejuv Lost Ticks", TreeFormat.roundToString(swift.rejuvTicksLost, 2));
        swift = new Swiftmend(localCharacter, basicStats, null, new Regrowth(localCharacter, basicStats));
        dictValues.put("SM Regrowth Heal", swift.toString());
        dictValues.put("SM Regrowth HPM", TreeFormat.roundToString(swift.getHPM(), 2));
        dictValues.put("SM Regrowth Lost Ticks", TreeFormat.roundToString(swift.regrowthTicksLost, 2));
        swift = new Swiftmend(localCharacter, basicStats, new Rejuvenation(localCharacter, basicStats), new Regrowth(localCharacter, basicStats));
        dictValues.put("SM Both Heal", swift.toString());
        dictValues.put("SM Both HPM", TreeFormat.roundToString(swift.getHPM(), 2));
        dictValues.put("SM Both Rejuv Lost Ticks", TreeFormat.roundToString(swift.rejuvTicksLost, 2));
        dictValues.put("SM Both Regrowth Lost Ticks", TreeFormat.roundToString(swift.regrowthTicksLost, 2));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Mana": return combatStats.getMana();
            case "MP5": return Sustained.getMPSInFSR();
            case "Global CD": return (new Nourish(localCharacter, combatStats)).gcd * 1000.0f; // Use Nourish gcd to equal normal GCD
        }
        return 0f;
    }
}
