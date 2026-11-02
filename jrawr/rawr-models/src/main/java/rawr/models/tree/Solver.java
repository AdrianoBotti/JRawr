// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Tree/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tree;

import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;

class SpellMix {
    private Character cacheCharacter;
    private Stats cacheStats;

    public SpellMix(SpellMix copy) {
        cacheCharacter = copy.cacheCharacter;
        cacheStats = copy.cacheStats;

        // Setup spells
        regrowth = new Regrowth(cacheCharacter, cacheStats, false, false);
        regrowthFresh = new Regrowth(cacheCharacter, cacheStats, false, false);
        regrowthAgain = new Regrowth(cacheCharacter, cacheStats, true, true);
        regrowthClipped = new Regrowth(cacheCharacter, cacheStats, false, true);
        lifebloom = new Lifebloom(cacheCharacter, cacheStats);
        lifebloomSlowStack = new Lifebloom(cacheCharacter, cacheStats, 3, false);
        lifebloomFastStack = new Lifebloom(cacheCharacter, cacheStats, 3, true);
        lifebloomSlow2Stack = new Lifebloom(cacheCharacter, cacheStats, 2, false);
        lifebloomFast2Stack = new Lifebloom(cacheCharacter, cacheStats, 2, true);
        lifebloomRollingStack = new LifebloomStack(cacheCharacter, cacheStats);

        rejuvenate = new Rejuvenation(cacheCharacter, cacheStats);
        nourish = new Nourish[5];
        nourish[0] = new Nourish(cacheCharacter, cacheStats, 0);
        nourish[1] = new Nourish(cacheCharacter, cacheStats, 1);
        nourish[2] = new Nourish(cacheCharacter, cacheStats, 2);
        nourish[3] = new Nourish(cacheCharacter, cacheStats, 3);
        nourish[4] = new Nourish(cacheCharacter, cacheStats, 4);
        healingTouch = new HealingTouch(cacheCharacter, cacheStats);
        wildGrowth = new WildGrowth(cacheCharacter, cacheStats);

        RejuvCF = copy.RejuvCF;
        ManagedRejuvCF = copy.ManagedRejuvCF;
        setRegrowthChainCast(copy.getRegrowthChainCast());
        setManagedRegrowthChainCast(copy.getManagedRegrowthChainCast());
        RegrowthCF = copy.RegrowthCF;
        ManagedRegrowthCF = copy.ManagedRegrowthCF;
        LifebloomCF = copy.LifebloomCF;
        NourishCF = copy.NourishCF;
        nourishRawHPCT = copy.nourishRawHPCT;

        setLifebloomStackType(copy.getLifebloomStackType());
        lifebloomStacks = copy.lifebloomStacks;
        lifebloomStackCF = copy.lifebloomStackCF;
        setWildGrowthCF(copy.getWildGrowthCF());

        ValAnyrShield = copy.ValAnyrShield;

        createSwiftmend();
        setSwiftmendCPM(copy.getSwiftmendCPM());
    }

    public SpellMix(Character character, Stats stats) {
        cacheCharacter = character;
        cacheStats = stats;

        // Setup spells
        regrowth = new Regrowth(character, stats, false, false);
        regrowthFresh = new Regrowth(character, stats, false, false);
        regrowthAgain = new Regrowth(character, stats, true, true);
        regrowthClipped = new Regrowth(character, stats, false, true);
        lifebloom = new Lifebloom(character, stats);
        lifebloomSlowStack = new Lifebloom(character, stats, 3, false);
        lifebloomFastStack = new Lifebloom(character, stats, 3, true);
        lifebloomSlow2Stack = new Lifebloom(character, stats, 2, false);
        lifebloomFast2Stack = new Lifebloom(character, stats, 2, true);
        lifebloomRollingStack = new LifebloomStack(character, stats);

        rejuvenate = new Rejuvenation(character, stats);
        nourish = new Nourish[5];
        nourish[0] = new Nourish(character, stats, 0);
        nourish[1] = new Nourish(character, stats, 1);
        nourish[2] = new Nourish(character, stats, 2);
        nourish[3] = new Nourish(character, stats, 3);
        nourish[4] = new Nourish(character, stats, 4);
        healingTouch = new HealingTouch(character, stats);
        wildGrowth = new WildGrowth(character, stats);

        RejuvCF = 0f;
        ManagedRejuvCF = 0f;
        setRegrowthChainCast(false);
        RegrowthCF = 0f;
        setManagedRegrowthChainCast(false);
        ManagedRegrowthCF = 0f;
        LifebloomCF = 0f;
        NourishCF = 0f;
        nourishRawHPCT = 0f;

        setLifebloomStackType(LifeBloomType.Slow);
        lifebloomStacks = 0f;
        lifebloomStackCF = 0f;
        setWildGrowthCF(0f);

        if (stats.getShieldFromHealed() > 0) ValAnyrShield = stats.getShieldFromHealed();

        createSwiftmend();
        setSwiftmendCPM(0f);
        RevitalizeChance = stats.getRevitalizeChance();
    }

    public void applyCombatStats(Stats stats) {
        cacheStats = stats;
        rejuvenate.applyStats(stats);
        regrowth.applyStats(stats);
        regrowthFresh.applyStats(stats);
        regrowthAgain.applyStats(stats);
        lifebloom.applyStats(stats);
        lifebloomSlowStack.applyStats(stats);
        lifebloomFastStack.applyStats(stats);
        lifebloomRollingStack.applyStats(stats);
        nourish[0].applyStats(stats);
        nourish[1].applyStats(stats);
        nourish[2].applyStats(stats);
        nourish[3].applyStats(stats);
        nourish[4].applyStats(stats);
        healingTouch.applyStats(stats);
        wildGrowth.applyStats(stats);
        createSwiftmend();
    }

    // Spells
    public Rejuvenation rejuvenate;
    public Regrowth regrowthFresh;
    public Regrowth regrowthClipped;
    public Regrowth regrowthAgain;
    public Regrowth regrowth;
    public Lifebloom lifebloom;
    public Lifebloom lifebloomSlowStack;
    public Lifebloom lifebloomFastStack;
    public Lifebloom lifebloomSlow2Stack;
    public Lifebloom lifebloomFast2Stack;
    public LifebloomStack lifebloomRollingStack;

    public Nourish[] nourish;
    public HealingTouch healingTouch;
    public WildGrowth wildGrowth;
    public Swiftmend swiftmend = null;

    public float getHPS() {
        return getHotsHPS() + getWildGrowthHPS() + getSwiftmendHPS() + getNourishHPS() + getValAnyrHPS();
    }

    public float getHPM() {
        return getHPS() / getMPS();
    }

    public float getMPS() {
        return getHotsMPS() + getWildGrowthMPS() + getSwiftmendMPS() + getNourishMPS();
    }

    public float getCastsPerMinute() {
        return getHotsCastsPerMinute() + getWildGrowthCPM() + getSwiftmendCPM() + getNourishCPM();
    }

    public float getHealsPerMinute() {
        return getHotsHealsPerMinute() + getWildGrowthHealsPerMinute() + getSwiftmendHealsPerMinute() + getNourishHealsPerMinute();
    }

    public float getCritsPerMinute() {
        return getHotsCritsPerMinute() + getSwiftmendCritsPerMinute() + getNourishCritsPerMinute();
    }

    // Hots
    public float getHotsCF() {
        return RejuvCF + RegrowthCF + ManagedRejuvCF + ManagedRegrowthCF + LifebloomCF + getLifebloomStackCF();
    }

    public float getHotsHPS() {
        return getRejuvHPS() + getRegrowthHPS() + getManagedRejuvHPS() + getManagedRegrowthHPS() + getLifebloomHPS() + getLifebloomStackHPS();
    }

    public float getHotsMPS() {
        return getRejuvMPS() + getRegrowthMPS() + getManagedRejuvMPS() + getManagedRegrowthMPS() + getLifebloomMPS() + getLifebloomStackMPS();
    }

    public float getHotsCastsPerMinute() {
        return getRejuvCPM() + getRegrowthCPM() + getManagedRejuvCPM() + getManagedRegrowthCPM() + getLifebloomCPM() + getLifebloomStackCPM();
    }

    public float getHotsHealsPerMinute() {
        return getRejuvHealsPerMinute() + getRegrowthHealsPerMinute() + getManagedRegrowthHealsPerMinute() + getLifebloomHealsPerMinute() + getLifebloomStackHealsPerMinute();
    }

    public float getHotsCritsPerMinute() {
        return getRegrowthCritsPerMinute() + getManagedRegrowthCritsPerMinute() + getLifebloomCritsPerMinute() + getLifebloomStackCritsPerMinute();
    }

    // Rejuvenation
    public float RejuvCF = 0f;

    public float getRejuvCPS() {
        return RejuvCF / rejuvenate.getCastTime();
    }

    public float getRejuvCPM() {
        return 60f * RejuvCF / rejuvenate.getCastTime();
    }

    public float getRejuvAvg() {
        return getRejuvCPS() * rejuvenate.getDuration();
    }

    public float getRejuvHPS() {
        return RejuvCF * rejuvenate.getHPCT();
    }

    public float getRejuvMPS() {
        return getRejuvCPS() * rejuvenate.getManaCost();
    }

    public float getRejuvHealsPerSecond() {
        return getRejuvCPS() * rejuvenate.getPeriodicTicks();
    }

    public float getRejuvCritsPerSecond() {
        return getRejuvCPS() * rejuvenate.getPeriodicTicks() * rejuvenate.getCritHoTPercent() / 100f;
    }

    public float ManagedRejuvCF = 0f;

    public float getManagedRejuvCPS() {
        return ManagedRejuvCF / rejuvenate.getCastTime();
    }

    public float getManagedRejuvCPM() {
        return 60f * ManagedRejuvCF / rejuvenate.getCastTime();
    }

    public float getManagedRejuvAvg() {
        return getManagedRejuvCPS() * rejuvenate.getDuration();
    }

    public void setManagedRejuvAvg(float value) {
        ManagedRejuvCF = value * rejuvenate.getCastTime() / rejuvenate.getDuration();
    }

    public float getManagedRejuvHPS() {
        return ManagedRejuvCF * rejuvenate.getHPCT();
    }

    public float getManagedRejuvMPS() {
        return getManagedRejuvCPS() * rejuvenate.getManaCost();
    }

    public float getManagedRejuvHealsPerSecond() {
        return getManagedRejuvCPS() * rejuvenate.getPeriodicTicks();
    }

    public float getManagedRejuvCritsPerSecond() {
        return getManagedRejuvCPS() * rejuvenate.getPeriodicTicks() * rejuvenate.getCritHoTPercent() / 100f;
    }

    public float getRejuvHealsPerMinute() {
        return (getManagedRejuvCPM() + getRejuvCPM()) * rejuvenate.getPeriodicTicks();
    }

    // Regrowth
    public float RegrowthCF = 0f;
    private boolean regrowthChainCast = false;

    public boolean getRegrowthChainCast() {
        return regrowthChainCast;
    }

    public void setRegrowthChainCast(boolean value) {
        regrowthChainCast = value;
        regrowthSpell = value ? regrowthAgain : regrowth;
    }

    private Spell regrowthSpell = null;

    public Spell getRegrowthSpell() {
        return regrowthSpell;
    }

    public float getRegrowthCPS() {
        return RegrowthCF / regrowthSpell.getCastTime();
    }

    public float getRegrowthCPM() {
        return 60f * RegrowthCF / regrowthSpell.getCastTime();
    }

    public float getRegrowthAvg() {
        return getRegrowthCPS() * regrowthSpell.getDuration();
    }

    public float getRegrowthHPS() {
        return RegrowthCF * (regrowthChainCast ? regrowthSpell.getHPCT_DH() : regrowthSpell.getHPCT());
    }

    public float getRegrowthMPS() {
        return getRegrowthCPS() * regrowthSpell.getManaCost();
    }

    public float getRegrowthHotTicksPerMinute() {
        return getRegrowthCPM() * (regrowthChainCast ? 0 : regrowthSpell.getPeriodicTicks());
    }

    public float getRegrowthHealsPerMinute() {
        return getRegrowthCPM() + getRegrowthHotTicksPerMinute();
    }

    public float getRegrowthCritsPerMinute() {
        return getRegrowthCPM() * regrowthSpell.getCritPercent() / 100f;
    }

    public float ManagedRegrowthCF = 0f;
    private boolean managedRegrowthChainCast = false;

    public boolean getManagedRegrowthChainCast() {
        return managedRegrowthChainCast;
    }

    public void setManagedRegrowthChainCast(boolean value) {
        managedRegrowthChainCast = value;
        managedRegrowthSpell = value ? regrowthAgain : regrowthClipped;
    }

    private Spell managedRegrowthSpell = null;

    public Spell getManagedRegrowthSpell() {
        return managedRegrowthSpell;
    }

    public float getManagedRegrowthCPS() {
        return ManagedRegrowthCF / managedRegrowthSpell.getCastTime();
    }

    public float getManagedRegrowthCPM() {
        return 60f * ManagedRegrowthCF / managedRegrowthSpell.getCastTime();
    }

    public float getManagedRegrowthAvg() {
        return getManagedRegrowthCPS() * managedRegrowthSpell.getDuration();
    }

    public void setManagedRegrowthAvg(float value) {
        ManagedRegrowthCF = value * managedRegrowthSpell.getCastTime() / managedRegrowthSpell.getDuration();
    }

    public float getManagedRegrowthHPS() {
        return ManagedRegrowthCF * (managedRegrowthChainCast ? managedRegrowthSpell.getHPCT_DH() : managedRegrowthSpell.getHPCT());
    }

    public float getManagedRegrowthMPS() {
        return getManagedRegrowthCPS() * managedRegrowthSpell.getManaCost();
    }

    public float getManagedRegrowthHotTicksPerMinute() {
        return getManagedRegrowthCPM() * (managedRegrowthChainCast ? 0 : managedRegrowthSpell.getPeriodicTicks());
    }

    public float getManagedRegrowthHealsPerMinute() {
        return getManagedRegrowthCPM() + getManagedRegrowthHotTicksPerMinute();
    }

    public float getManagedRegrowthCritsPerMinute() {
        return getManagedRegrowthCPM() * managedRegrowthSpell.getCritPercent() / 100f;
    }

    // Lifebloom
    public float LifebloomCF = 0f;

    public float getLifebloomCPS() {
        return LifebloomCF / lifebloom.getCastTime();
    }

    public float getLifebloomCPM() {
        return 60f * LifebloomCF / lifebloom.getCastTime();
    }

    public float getLifebloomAvg() {
        return getLifebloomCPS() * lifebloom.getDuration();
    }

    public float getLifebloomHPS() {
        return LifebloomCF * lifebloom.getHPCT();
    }

    public float getLifebloomMPS() {
        return getLifebloomCPS() * lifebloom.getManaCost();
    }

    public float getLifebloomHotTicksPerMinute() {
        return getLifebloomCPM() * lifebloom.getPeriodicTicks();
    }

    public float getLifebloomHealsPerMinute() {
        return getLifebloomCPM() + getLifebloomHotTicksPerMinute();
    }

    public float getLifebloomCritsPerMinute() {
        return getLifebloomCPM() * lifebloom.getCritPercent() / 100f;
    }

    // Nourish
    public float NourishCF = 0f;

    public float getNourishHPS() {
        return NourishCF * nourishRawHPCT;
    }

    public float getNourishMPS() {
        return getNourishCPS() * nourish[0].getManaCost();
    }

    public float getNourishCPS() {
        return NourishCF / nourish[0].getCastTime();
    }

    public float getNourishCPM() {
        return 60f * NourishCF / nourish[0].getCastTime();
    }

    public float getNourishHealsPerMinute() {
        return getNourishCPM();
    }

    public float getNourishCritsPerMinute() {
        return getNourishHealsPerMinute() * nourish[0].getCritPercent() / 100f;
    }

    private float nourishRawHPCT = 0f;

    public void calculateNourishHPS(float hots0, float hots1, float hots2, float hots3, float hots4) {
        nourishRawHPCT = (hots0 * nourish[0].getHPCT_DH()
                + hots1 * nourish[1].getHPCT_DH()
                + hots2 * nourish[2].getHPCT_DH()
                + hots3 * nourish[3].getHPCT_DH()
                + hots4 * nourish[4].getHPCT_DH());
    }

    // Lifebloom stacks
    private LifeBloomType lifebloomStackType;

    public LifeBloomType getLifebloomStackType() {
        return lifebloomStackType;
    }

    public void setLifebloomStackType(LifeBloomType value) {
        lifebloomStackType = value;
        switch (value) {
            case Slow:
                lifebloomStackSpell = lifebloomSlowStack;
                lifebloomStackDuration = lifebloomStackSpell.getDuration() + 1f; // need a second before reapplying
                break;
            case Fast:
                lifebloomStackSpell = lifebloomFastStack;
                lifebloomStackDuration = lifebloomStackSpell.getDuration() + 1f; // need a second before reapplying
                break;
            case Slow2:
                lifebloomStackSpell = lifebloomSlow2Stack;
                lifebloomStackDuration = lifebloomStackSpell.getDuration() + 1f; // need a second before reapplying
                break;
            case Fast2:
                lifebloomStackSpell = lifebloomFast2Stack;
                lifebloomStackDuration = lifebloomStackSpell.getDuration() + 1f; // need a second before reapplying
                break;
            default:
                lifebloomStackSpell = lifebloomRollingStack;
                lifebloomStackDuration = lifebloomStackSpell.getDuration();
                break;
        }
        lifebloomStackCF = -1f;
    }

    private Spell lifebloomStackSpell = null;

    public Spell getLifebloomStackSpell() {
        return lifebloomStackSpell;
    }

    private float lifebloomStackDuration;

    private float lifebloomStacks = 0;

    public float getLifebloomStacks() {
        return lifebloomStacks;
    }

    public void setLifebloomStacks(float value) {
        lifebloomStacks = value;
        lifebloomStackCF = -1f;
    }

    private float lifebloomStackCF = -1f;

    public float getLifebloomStackCF() {
        if (lifebloomStackCF != -1f) return lifebloomStackCF;
        return lifebloomStackSpell.getCastTime() * getLifebloomStacks() / lifebloomStackDuration;
    }

    public void setLifebloomStackCF(float value) {
        lifebloomStackCF = value;
    }

    public float getLifebloomStackCPS() {
        return lifebloomStackSpell.getNumberOfCasts() * getLifebloomStackCF() / lifebloomStackSpell.getCastTime();
    }

    public float getLifebloomStackCPM() {
        return 60f * getLifebloomStackCPS();
    }

    public float getLifebloomStackAvg() {
        return getLifebloomStackCPS() / lifebloomStackSpell.getNumberOfCasts() * lifebloomStackSpell.getDuration();
    }

    public float getLifebloomStackBPS() {
        return lifebloomStackType == LifeBloomType.Rolling ? 0 : getLifebloomStackCPS() / lifebloomStackSpell.getNumberOfCasts();
    }

    public float getLifebloomStackHPS() {
        return getLifebloomStackAvg() * lifebloomStackSpell.getHPS_HOT() + getLifebloomStackBPS() * lifebloomStackSpell.getAverageHealingwithCrit();
    }

    public float getLifebloomStackMPS() {
        return getLifebloomStackCPS() * lifebloomStackSpell.getManaCost();
    }

    public float getLifebloomStackHotTicksPerMinute() {
        return getLifebloomStackAvg() * 60f / lifebloomStackSpell.getPeriodicTickTime();
    }

    public float getLifebloomStackHealsPerMinute() {
        return 60f * getLifebloomStackBPS() + getLifebloomStackHotTicksPerMinute();
    }

    public float getLifebloomStackCritsPerMinute() {
        return lifebloomStackType == LifeBloomType.Rolling ? 0 :
                60f * getLifebloomStackBPS() * lifebloomStackSpell.getCritPercent() / 100f;
    }

    public float getLifebloomStackHPS_DH() {
        return getLifebloomStackBPS() * lifebloomStackSpell.getAverageHealingwithCrit();
    }

    public float getLifebloomStackHPS_HOT() {
        return getLifebloomStackAvg() * lifebloomStackSpell.getHPS_HOT();
    }

    // Wild Growth
    private float wildgrowthCF = 0f;

    public float getWildGrowthCF() {
        return wildgrowthCF;
    }

    public void setWildGrowthCF(float value) {
        if (cacheCharacter.getDruidTalents().getWildGrowth() > 0) wildgrowthCF = value;
        else wildgrowthCF = 0;
    }

    public float getWildGrowthCPS() {
        return getWildGrowthCF() / wildGrowth.getCastTime();
    }

    public float getWildGrowthCPM() {
        return 60f * getWildGrowthCPS();
    }

    public void setWildGrowthCPM(float value) {
        setWildGrowthCF(value * (float) wildGrowth.getCastTime() / 60f);
    }

    public float getWildGrowthAvg() {
        return getWildGrowthCPS() * wildGrowth.getDuration();
    }

    public float getWildGrowthHPS() {
        return getWildGrowthAvg() * wildGrowth.maxTargets * wildGrowth.getPeriodicTick();
    }

    public float getWildGrowthMPS() {
        return getWildGrowthCPS() * wildGrowth.getManaCost();
    }

    public float getWildGrowthHealsPerMinute() {
        return getWildGrowthCPM() * wildGrowth.maxTargets * wildGrowth.getPeriodicTicks();
    }

    // Swiftmend
    public void createSwiftmend() {
        swiftmend = new Swiftmend(cacheCharacter, cacheStats, RejuvCF > 0 ? rejuvenate : null, RegrowthCF > 0 ? regrowth : null);
    }

    public float getSwiftmendCF() {
        if (swiftmend == null) return 0;
        float cf = getSwiftmendCPS() * swiftmend.getCastTime();
        // cf += swiftmend.rejuvUseChance * swiftmend.rejuvTicksLost / rejuvenate.PeriodicTicks * rejuvenate.CastTime * SwiftmendCPS;
        // cf += swiftmend.regrowthUseChance * swiftmend.regrowthTicksLost / regrowth.PeriodicTicks * regrowth.CastTime * SwiftmendCPS;
        return cf;
    }

    public void setSwiftmendCF(float value) {
        setSwiftmendCPM(60f * value / swiftmend.getCastTime());
    }

    public float getSwiftmendCPS() {
        return getSwiftmendCPM() / 60f;
    }

    private float swiftmendCPM;

    public float getSwiftmendCPM() {
        return swiftmendCPM;
    }

    public void setSwiftmendCPM(float value) {
        if (cacheCharacter.getDruidTalents().getSwiftmend() > 0) swiftmendCPM = value;
        else swiftmendCPM = 0;
    }

    public float getSwiftmendAvg() {
        return swiftmend == null ? 0 : getSwiftmendCPS() * swiftmend.getDuration();
    }

    public float getSwiftmendHPS() {
        if (swiftmend == null) return 0;
        float healing = swiftmend.getTotalAverageHealing();
        healing -= swiftmend.regrowthUseChance * swiftmend.regrowthTicksLost * regrowth.getPeriodicTick();
        healing -= swiftmend.rejuvUseChance * swiftmend.rejuvTicksLost * rejuvenate.getPeriodicTick();
        return healing * getSwiftmendCPS();
        // float hps = swiftmend.TotalAverageHealing * SwiftmendCPS;
        // hps += swiftmend.regrowthUseChance * swiftmend.regrowthTicksLost / regrowth.PeriodicTicks * regrowth.AverageHealingwithCrit * SwiftmendCPS;
        // return hps;
    }

    public float getSwiftmendMPS() {
        if (swiftmend == null) return 0;
        float mps = getSwiftmendCPS() * swiftmend.getManaCost();
        mps += swiftmend.rejuvUseChance * swiftmend.rejuvTicksLost / rejuvenate.getPeriodicTicks() * rejuvenate.getManaCost() * getSwiftmendCPS();
        mps += swiftmend.regrowthUseChance * swiftmend.regrowthTicksLost / regrowth.getPeriodicTicks() * regrowth.getManaCost() * getSwiftmendCPS();
        return mps;
    }

    public float getSwiftmendHealsPerMinute() {
        return getSwiftmendCPM();
    }

    public float getSwiftmendCritsPerMinute() {
        return swiftmend == null ? 0 : getSwiftmendCPM() * swiftmend.getCritPercent() / 100.0f;
    }

    public float getTotalCF() {
        return (float) TreeFormat.round(getHotsCF() + getWildGrowthCF() + getSwiftmendCF() + NourishCF, 4);
    }

    public float getIdleCF() {
        return 1f - getTotalCF();
    }

    public float ValAnyrShield = 0;

    public float getValAnyrHPS() {
        return ValAnyrShield * (getHotsHPS() + getWildGrowthHPS() + getSwiftmendHPS() + getNourishHPS());
    }

    public float getRejuvenationHealsPerMinute() {
        return getRejuvHealsPerMinute();
    }

    public float RevitalizeChance = 0f;

    public float getRevitalizeProcsPerMinute() {
        return RevitalizeChance * (getRejuvCPM() * rejuvenate.getPeriodicTicks() + getWildGrowthCPM() * wildGrowth.getPeriodicTicks() * wildGrowth.maxTargets);
    }
}

enum HealingSpell {
    Rejuv,
    Regrowth,
    Lifebloom,
    ManagedRejuv,
    ManagedRegrowth,
    ManagedLifebloom,
    WildGrowth,
    Swiftmend,
    Nourish,
    HealingTouch // should always be last
}

class CombatFactor {
    public float HealingPerSecond;
    public float ManaPerSecond;
    public float HotTicksPerSecond;
    public float DirectHealsPerSecond;
    public float HotCritsPerSecond;
    public float DirectCritsPerSecond;
    public float CritsPerSecond;
    public float HealsPerSecond;
    public float CastsPerSecond;
    public float CastFraction;

    public void compute() {
        CritsPerSecond = DirectCritsPerSecond + HotCritsPerSecond;
        HealsPerSecond = DirectHealsPerSecond + HotTicksPerSecond;
    }

    public CombatFactor() {
        HealingPerSecond = 0;
        ManaPerSecond = 0;
        HotTicksPerSecond = 0;
        DirectHealsPerSecond = 0;
        HotCritsPerSecond = 0;
        DirectCritsPerSecond = 0;
        CritsPerSecond = 0;
        HealsPerSecond = 0;
        CastsPerSecond = 0;
        CastFraction = 0;
    }

    public CombatFactor clone() {
        CombatFactor result = new CombatFactor();
        result.HealingPerSecond = HealingPerSecond;
        result.ManaPerSecond = ManaPerSecond;
        result.HotTicksPerSecond = HotTicksPerSecond;
        result.DirectHealsPerSecond = DirectHealsPerSecond;
        result.HotCritsPerSecond = HotCritsPerSecond;
        result.DirectCritsPerSecond = DirectCritsPerSecond;
        result.HealsPerSecond = HealsPerSecond;
        result.CritsPerSecond = CritsPerSecond;
        result.CastsPerSecond = CastsPerSecond;
        result.CastFraction = CastFraction;
        return result;
    }

    public void accumulate(CombatFactor cf, float weight) {
        HealingPerSecond += cf.HealingPerSecond * weight;
        ManaPerSecond += cf.ManaPerSecond * weight;
        HotTicksPerSecond += cf.HotTicksPerSecond * weight;
        DirectHealsPerSecond += cf.DirectHealsPerSecond * weight;
        HotCritsPerSecond += cf.HotCritsPerSecond * weight;
        DirectCritsPerSecond += cf.DirectCritsPerSecond * weight;
        HealsPerSecond += cf.HealsPerSecond * weight;
        CritsPerSecond += cf.CritsPerSecond * weight;
        CastsPerSecond += cf.CastsPerSecond * weight;
        CastFraction += cf.CastFraction * weight;
    }
}

class CombatFactors {
    public CombatFactor[] factors;

    public float HealingPerSecond;
    public float ManaPerSecond;
    public float HotTicksPerSecond;
    public float DirectHealsPerSecond;
    public float HotCritsPerSecond;
    public float DirectCritsPerSecond;
    public float CritsPerSecond;
    public float HealsPerSecond;
    public float CastsPerSecond;
    public float CastFraction;

    public float RejuvTicksPerSecond;
    public float HealingPerMana;

    public float TotalHealing;

    public CombatFactors() {
        int count = HealingSpell.values().length;
        factors = new CombatFactor[count];
        for (int i = 0; i < count; i++) {
            factors[i] = new CombatFactor();
        }
        TotalHealing = 0;
    }

    public CombatFactors(CombatFactors copyOf) {
        int count = HealingSpell.values().length;
        factors = new CombatFactor[count];
        for (int i = 0; i < count; i++) {
            factors[i] = copyOf.factors[i].clone();
        }
        TotalHealing = 0;
    }

    public CombatFactors clone() {
        return new CombatFactors(this);
    }

    public void accumulate(CombatFactors cfs, float weight) {
        int count = HealingSpell.values().length;
        for (int i = 0; i < count; i++) {
            factors[i].accumulate(cfs.factors[i], weight);
        }
        TotalHealing += cfs.TotalHealing * weight;
    }

    public void compute() {
        HealingPerSecond = 0;
        ManaPerSecond = 0;
        HotTicksPerSecond = 0;
        DirectHealsPerSecond = 0;
        HotCritsPerSecond = 0;
        DirectCritsPerSecond = 0;
        CritsPerSecond = 0;
        HealsPerSecond = 0;
        CastsPerSecond = 0;
        CastFraction = 0;
        RejuvTicksPerSecond = 0;

        int count = HealingSpell.values().length;
        for (int i = 0; i < count; i++) {
            HealingPerSecond += factors[i].HealingPerSecond;
            ManaPerSecond += factors[i].ManaPerSecond;
            HotTicksPerSecond += factors[i].HotTicksPerSecond;
            DirectHealsPerSecond += factors[i].DirectHealsPerSecond;
            HotCritsPerSecond += factors[i].HotCritsPerSecond;
            DirectCritsPerSecond += factors[i].DirectCritsPerSecond;
            CritsPerSecond += factors[i].CritsPerSecond;
            HealsPerSecond += factors[i].HealsPerSecond;
            CastsPerSecond += factors[i].CastsPerSecond;
            CastFraction += factors[i].CastFraction;
            if (i == HealingSpell.Rejuv.ordinal() || i == HealingSpell.ManagedRejuv.ordinal()) {
                RejuvTicksPerSecond += factors[i].HotTicksPerSecond;
            }
        }

        HealingPerMana = ManaPerSecond > 0 ? HealingPerSecond / ManaPerSecond : 0;
    }
}

class SustainedResult {
    public SustainedResult(CharacterCalculationsTree calculatedStats, Stats stats) {
        spellMix = new SpellMix(calculatedStats.getLocalCharacter(), stats);

        // Setup mana regen
        SpiritMPS = StatConversion.getSpiritRegenSec(stats.getIntellect(), stats.getSpirit());
        replenishment = stats.getManaRestoreFromMaxManaPerSecond();
        Mana = stats.getMana();
        GearMPS = stats.getMp5() / 5f;
        SpiritInCombatFraction = stats.getSpellCombatManaRegeneration();
        ProcsMPS = stats.getManaRestore();
    }

    public void applyCombatStats(Stats stats) {
        spellMix.applyCombatStats(stats);
    }

    public SpellMix spellMix;

    // Passive mana regen
    public float Mana; // set by constructor
    public float SpiritMPS; // set by constructor
    public float GearMPS; // set by constructor
    public float SpiritInCombatFraction; // set by constructor
    private float replenishment; // set by constructor
    public float ReplenishmentUptime = 1f;

    public float getReplenishmentMPS() {
        return Mana * replenishment * ReplenishmentUptime;
    }

    public float RevitalizePPM = 0f;

    public float getRevitalizeMPS() {
        return RevitalizePPM * 0.01f * Mana;
    }

    public float ProcsMPS; // set by constructor

    public float getMPSInFSR() {
        return getRevitalizeMPS() + ProcsMPS + GearMPS + getReplenishmentMPS() + SpiritMPS * SpiritInCombatFraction;
    }

    public float getMPSOutFSR() {
        return getRevitalizeMPS() + ProcsMPS + GearMPS + getReplenishmentMPS() + SpiritMPS;
    }

    public float OutOfCombatFraction = 0;

    public float getManaRegen() {
        return OutOfCombatFraction * getMPSOutFSR() + (1f - OutOfCombatFraction) * getMPSInFSR();
    }

    // Active mana regen
    public float getExtraMana() {
        return getInnervateMana();
    }

    public float getInnervateMana() {
        return ManaPerInnervate * NumberOfInnervates;
    }

    public float getInnervateMPS() {
        return getInnervateMana() / TotalTime;
    }

    public float ManaPerInnervate = 0f;
    public float NumberOfInnervates = 0f;

    // Saved unreduced cast fractions
    public float RejuvCF_unreduced = 0f;
    public float RegrowthCF_unreduced = 0f;
    public float ManagedRejuvCF_unreduced = 0f;
    public float ManagedRegrowthCF_unreduced = 0f;
    public float LifebloomCF_unreduced = 0f;
    public float LifebloomStackCF_unreduced = 0f;
    public float NourishCF_unreduced = 0f;
    public float WildGrowthCF_unreduced = 0f;
    public float SwiftmendCF_unreduced = 0f;
    public float IdleCF_unreduced = 1f;

    public float RejuvCF_unreducedOOM = 0f;
    public float RegrowthCF_unreducedOOM = 0f;
    public float ManagedRejuvCF_unreducedOOM = 0f;
    public float ManagedRegrowthCF_unreducedOOM = 0f;
    public float LifebloomCF_unreducedOOM = 0f;
    public float NourishCF_unreducedOOM = 0f;
    public float WildGrowthCF_unreducedOOM = 0f;
    public float IdleCF_unreducedOOM = 1f;

    public float TotalTime;

    public float getTimeToOOM() {
        return getEffMPS() > 0 ? Math.min((getExtraMana() + Mana) / getEffMPS(), TotalTime) : TotalTime;
    }

    public float TimeToOOM_unreduced = 0f;

    public float getTimeAfterOOM() {
        return TotalTime - getTimeToOOM();
    }

    public float getEffMPS() {
        return spellMix.getMPS() - getManaRegen();
    }

    public float getTotalModifier() {
        return TotalTime > getTimeToOOM() ? (getTimeToOOM() + getBurnRegenFraction() * getTimeAfterOOM()) / TotalTime : 1f;
    }

    public float getTimeToRegenAll() {
        return Mana / getMPSOutFSR();
    }

    public float getTimeToBurnAll() {
        return Mana / getEffMPS();
    }

    public float getBurnRegenFraction() {
        return getTimeToBurnAll() / (getTimeToRegenAll() + getTimeToBurnAll());
    }

    public float getTotalHealing() {
        return spellMix.getHPS() * TotalTime * getTotalModifier();
    }

    public float getTotalCastsPerMinute() {
        return spellMix.getCastsPerMinute() * getTotalModifier();
    }

    public float getTotalCritsPerMinute() {
        return spellMix.getCritsPerMinute() * getTotalModifier();
    }

    public float getTotalHealsPerMinute() {
        return spellMix.getHealsPerMinute() * getTotalModifier();
    }

    public RotationSettings rotSettings;

    public CombatFactors getCombatFactors() {
        CombatFactors result = new CombatFactors();
        result.factors[HealingSpell.Rejuv.ordinal()].HealingPerSecond = spellMix.getRejuvHPS();
        result.factors[HealingSpell.Rejuv.ordinal()].ManaPerSecond = spellMix.getRejuvMPS();
        result.factors[HealingSpell.Rejuv.ordinal()].HotTicksPerSecond = spellMix.getRejuvHealsPerSecond();
        result.factors[HealingSpell.Rejuv.ordinal()].DirectHealsPerSecond = 0;
        result.factors[HealingSpell.Rejuv.ordinal()].HotCritsPerSecond = spellMix.getRejuvCritsPerSecond();
        result.factors[HealingSpell.Rejuv.ordinal()].DirectCritsPerSecond = 0;
        result.factors[HealingSpell.Rejuv.ordinal()].CastsPerSecond = spellMix.getRejuvCPS();
        result.factors[HealingSpell.Rejuv.ordinal()].CastFraction = spellMix.RejuvCF;
        result.factors[HealingSpell.Rejuv.ordinal()].compute();

        result.factors[HealingSpell.Regrowth.ordinal()].HealingPerSecond = spellMix.getRegrowthHPS();
        result.factors[HealingSpell.Regrowth.ordinal()].ManaPerSecond = spellMix.getRegrowthMPS();
        result.factors[HealingSpell.Regrowth.ordinal()].HotTicksPerSecond = spellMix.getRegrowthHotTicksPerMinute() / 60f;
        result.factors[HealingSpell.Regrowth.ordinal()].DirectHealsPerSecond = spellMix.getRegrowthCPS();
        result.factors[HealingSpell.Regrowth.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.Regrowth.ordinal()].DirectCritsPerSecond = spellMix.getRegrowthCritsPerMinute() / 60f;
        result.factors[HealingSpell.Regrowth.ordinal()].CastsPerSecond = spellMix.getRegrowthCPS();
        result.factors[HealingSpell.Regrowth.ordinal()].CastFraction = spellMix.RegrowthCF;
        result.factors[HealingSpell.Regrowth.ordinal()].compute();

        result.factors[HealingSpell.Lifebloom.ordinal()].HealingPerSecond = spellMix.getLifebloomHPS();
        result.factors[HealingSpell.Lifebloom.ordinal()].ManaPerSecond = spellMix.getLifebloomMPS();
        result.factors[HealingSpell.Lifebloom.ordinal()].HotTicksPerSecond = spellMix.getLifebloomHotTicksPerMinute() / 60f;
        result.factors[HealingSpell.Lifebloom.ordinal()].DirectHealsPerSecond = spellMix.getLifebloomCPS();
        result.factors[HealingSpell.Lifebloom.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.Lifebloom.ordinal()].DirectCritsPerSecond = spellMix.getLifebloomCritsPerMinute() / 60f;
        result.factors[HealingSpell.Lifebloom.ordinal()].CastsPerSecond = spellMix.getLifebloomCPS();
        result.factors[HealingSpell.Lifebloom.ordinal()].CastFraction = spellMix.LifebloomCF;
        result.factors[HealingSpell.Lifebloom.ordinal()].compute();

        result.factors[HealingSpell.ManagedRejuv.ordinal()].HealingPerSecond = spellMix.getManagedRejuvHPS();
        result.factors[HealingSpell.ManagedRejuv.ordinal()].ManaPerSecond = spellMix.getManagedRejuvMPS();
        result.factors[HealingSpell.ManagedRejuv.ordinal()].HotTicksPerSecond = spellMix.getManagedRejuvHealsPerSecond();
        result.factors[HealingSpell.ManagedRejuv.ordinal()].DirectHealsPerSecond = 0;
        result.factors[HealingSpell.ManagedRejuv.ordinal()].HotCritsPerSecond = spellMix.getManagedRejuvCritsPerSecond();
        result.factors[HealingSpell.ManagedRejuv.ordinal()].DirectCritsPerSecond = 0;
        result.factors[HealingSpell.ManagedRejuv.ordinal()].CastsPerSecond = spellMix.getManagedRejuvCPS();
        result.factors[HealingSpell.ManagedRejuv.ordinal()].CastFraction = spellMix.ManagedRejuvCF;
        result.factors[HealingSpell.ManagedRejuv.ordinal()].compute();

        result.factors[HealingSpell.ManagedRegrowth.ordinal()].HealingPerSecond = spellMix.getManagedRegrowthHPS();
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].ManaPerSecond = spellMix.getManagedRegrowthMPS();
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].HotTicksPerSecond = spellMix.getManagedRegrowthHotTicksPerMinute() / 60f;
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].DirectHealsPerSecond = spellMix.getManagedRegrowthCPS();
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].DirectCritsPerSecond = spellMix.getManagedRegrowthCritsPerMinute() / 60f;
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].CastsPerSecond = spellMix.getManagedRegrowthCPS();
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].CastFraction = spellMix.ManagedRegrowthCF;
        result.factors[HealingSpell.ManagedRegrowth.ordinal()].compute();

        result.factors[HealingSpell.ManagedLifebloom.ordinal()].HealingPerSecond = spellMix.getLifebloomStackHPS();
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].ManaPerSecond = spellMix.getLifebloomStackMPS();
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].HotTicksPerSecond = spellMix.getLifebloomStackHotTicksPerMinute() / 60f;
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].DirectHealsPerSecond = spellMix.getLifebloomStackBPS();
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].DirectCritsPerSecond = spellMix.getLifebloomStackCritsPerMinute() / 60f;
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].CastsPerSecond = spellMix.getLifebloomStackCPS();
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].CastFraction = spellMix.getLifebloomStackCF();
        result.factors[HealingSpell.ManagedLifebloom.ordinal()].compute();

        result.factors[HealingSpell.WildGrowth.ordinal()].HealingPerSecond = spellMix.getWildGrowthHPS();
        result.factors[HealingSpell.WildGrowth.ordinal()].ManaPerSecond = spellMix.getWildGrowthMPS();
        result.factors[HealingSpell.WildGrowth.ordinal()].HotTicksPerSecond = spellMix.getWildGrowthHealsPerMinute() / 60f;
        result.factors[HealingSpell.WildGrowth.ordinal()].DirectHealsPerSecond = 0;
        result.factors[HealingSpell.WildGrowth.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.WildGrowth.ordinal()].DirectCritsPerSecond = 0;
        result.factors[HealingSpell.WildGrowth.ordinal()].CastsPerSecond = spellMix.getWildGrowthCPS();
        result.factors[HealingSpell.WildGrowth.ordinal()].CastFraction = spellMix.getWildGrowthCF();
        result.factors[HealingSpell.WildGrowth.ordinal()].compute();

        result.factors[HealingSpell.Swiftmend.ordinal()].HealingPerSecond = spellMix.getSwiftmendHPS();
        result.factors[HealingSpell.Swiftmend.ordinal()].ManaPerSecond = spellMix.getSwiftmendMPS();
        result.factors[HealingSpell.Swiftmend.ordinal()].HotTicksPerSecond = 0;
        result.factors[HealingSpell.Swiftmend.ordinal()].DirectHealsPerSecond = spellMix.getSwiftmendHealsPerMinute() / 60f;
        result.factors[HealingSpell.Swiftmend.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.Swiftmend.ordinal()].DirectCritsPerSecond = spellMix.getSwiftmendCritsPerMinute() / 60f;
        result.factors[HealingSpell.Swiftmend.ordinal()].CastsPerSecond = spellMix.getSwiftmendCPS();
        result.factors[HealingSpell.Swiftmend.ordinal()].CastFraction = spellMix.getSwiftmendCF();
        result.factors[HealingSpell.Swiftmend.ordinal()].compute();

        result.factors[HealingSpell.Nourish.ordinal()].HealingPerSecond = spellMix.getNourishHPS();
        result.factors[HealingSpell.Nourish.ordinal()].ManaPerSecond = spellMix.getNourishMPS();
        result.factors[HealingSpell.Nourish.ordinal()].HotTicksPerSecond = 0;
        result.factors[HealingSpell.Nourish.ordinal()].DirectHealsPerSecond = spellMix.getNourishHealsPerMinute() / 60f;
        result.factors[HealingSpell.Nourish.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.Nourish.ordinal()].DirectCritsPerSecond = spellMix.getNourishCritsPerMinute() / 60f;
        result.factors[HealingSpell.Nourish.ordinal()].CastsPerSecond = spellMix.getNourishCPS();
        result.factors[HealingSpell.Nourish.ordinal()].CastFraction = spellMix.NourishCF;
        result.factors[HealingSpell.Nourish.ordinal()].compute();

        result.factors[HealingSpell.HealingTouch.ordinal()].HealingPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].ManaPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].HotTicksPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].DirectHealsPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].HotCritsPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].DirectCritsPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].CastsPerSecond = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].CastFraction = 0;
        result.factors[HealingSpell.HealingTouch.ordinal()].compute();

        result.TotalHealing = getTotalHealing();

        return result;
    }
}

enum SingleTargetBurstRotations {
    AutoSelect,
    AutoSelectForceSwiftmend,
    RjN,
    RjLbN,
    RjRgN,
    RjRgLbN,
    RjRg,
    RjLbRg,
    Rg,
    RgN,
    N,
    RjN_s,
    RjLbN_s,
    RjRgN_s,
    RjRgLbN_s,
    RjRg_s,
    RjLbRg_s,
    Rg_s,
    RgN_s
}

enum HealTargetTypes {
    TankHealing,
    RaidHealing
}

enum SpellList {
    HealingTouch,
    Nourish,
    Regrowth,
    Rejuvenation
}

enum LifeBloomType {
    Slow,
    Fast,
    Rolling,
    Slow2,
    Fast2
}

class RotationSettings {
    public float averageLifebloomStacks, averageRejuv, averageRegrowth;
    public LifeBloomType lifeBloomType;
    public int SwiftmendPerMin, WildGrowthPerMin;
    public float RejuvFraction, RegrowthFraction, LifebloomFraction, NourishFraction;
    public float nourish0, nourish1, nourish2, nourish3, nourish4;
    public float livingSeedEfficiency;
    public HealTargetTypes healTarget;
}

class SingleTargetBurstResult {
    public SpellMix spellMix;

    public float getHPS() {
        return spellMix.getHPS();
    }

    public float getHPM() {
        return spellMix.getHPM();
    }

    public SingleTargetBurstRotations rotation;

    public SingleTargetBurstResult(SingleTargetBurstRotations rotation, SpellMix spells,
                                   float rjCF, float rgCF, float lbCF, float smCPM, float nCF, boolean rgAgain) {
        spellMix = new SpellMix(spells);
        spellMix.setRegrowthChainCast(rgAgain);

        spellMix.RejuvCF = rjCF;
        spellMix.RegrowthCF = rgCF;
        spellMix.setLifebloomStackCF(lbCF);

        spellMix.createSwiftmend();
        spellMix.setSwiftmendCPM(smCPM);

        spellMix.NourishCF = nCF;

        int numberOfHots = 0
                + (rjCF > 0 ? 1 : 0)
                + (rgCF > 0 ? 1 : 0)
                + (lbCF > 0 ? 1 : 0);
        if (numberOfHots == 0) spellMix.calculateNourishHPS(1, 0, 0, 0, 0);
        if (numberOfHots == 1) spellMix.calculateNourishHPS(0, 1, 0, 0, 0);
        if (numberOfHots == 2) spellMix.calculateNourishHPS(0, 0, 1, 0, 0);
        if (numberOfHots == 3) spellMix.calculateNourishHPS(0, 0, 0, 1, 0);
        if (numberOfHots == 4) spellMix.calculateNourishHPS(0, 0, 0, 0, 1);

        this.rotation = rotation;
    }
}

public class Solver {
    public static SustainedResult simulateHealing(CharacterCalculationsTree calculatedStats, Stats stats, CalculationOptionsTree calcOpts, RotationSettings rotSettings) {
        CalculationOptionsTree.SpellProfile profile = calcOpts.getCurrent();

        SustainedResult rot = new SustainedResult(calculatedStats, stats);

        // Setup spell mix
        rot.spellMix.setLifebloomStackType(rotSettings.lifeBloomType);
        rot.spellMix.setLifebloomStacks(rotSettings.averageLifebloomStacks);
        rot.spellMix.RejuvCF = rotSettings.RejuvFraction;
        rot.spellMix.setManagedRejuvAvg(rotSettings.averageRejuv);
        rot.spellMix.RegrowthCF = rotSettings.RegrowthFraction;
        rot.spellMix.setManagedRegrowthAvg(rotSettings.averageRegrowth);
        rot.spellMix.LifebloomCF = rotSettings.LifebloomFraction;
        rot.spellMix.NourishCF = rotSettings.NourishFraction;
        rot.spellMix.setWildGrowthCPM(rotSettings.WildGrowthPerMin);
        rot.spellMix.createSwiftmend();
        rot.spellMix.setSwiftmendCPM(rotSettings.SwiftmendPerMin);
        rot.spellMix.calculateNourishHPS(rotSettings.nourish0, rotSettings.nourish1,
                rotSettings.nourish2, rotSettings.nourish3, rotSettings.nourish4);

        rot.rotSettings = rotSettings;

        rot.TotalTime = profile.getFightDuration();

        // Mana regeneration
        rot.ReplenishmentUptime = profile.getReplenishmentUptime() / 100f;
        rot.OutOfCombatFraction = 0f;
        rot.RevitalizePPM = (float) profile.getRevitalizePPM() / 100f;

        // Innervates
        // 3 min CD, takes 15 seconds to apply
        rot.NumberOfInnervates = (float) Math.ceil((profile.getFightDuration() - 15f) / 180f);
        rot.ManaPerInnervate = 2.25f * profile.getInnervates(); // Use self innervate?
        if (calculatedStats.getLocalCharacter().getDruidTalents().isGlyphOfInnervate()) rot.ManaPerInnervate += 0.45f;
        rot.ManaPerInnervate *= TreeConstants.BaseMana;

        // Correct cast fractions
        rot.RejuvCF_unreduced = rot.spellMix.RejuvCF;
        rot.RegrowthCF_unreduced = rot.spellMix.RegrowthCF;
        rot.ManagedRejuvCF_unreduced = rot.spellMix.ManagedRejuvCF;
        rot.ManagedRegrowthCF_unreduced = rot.spellMix.ManagedRegrowthCF;
        rot.LifebloomCF_unreduced = rot.spellMix.LifebloomCF;
        rot.NourishCF_unreduced = rot.spellMix.NourishCF;
        rot.WildGrowthCF_unreduced = rot.spellMix.getWildGrowthCF();
        rot.SwiftmendCF_unreduced = rot.spellMix.getSwiftmendCF();
        rot.LifebloomStackCF_unreduced = rot.spellMix.getLifebloomStackCF();

        float IdleCF = profile.getIdleCastTimePercent() / 100f;
        rot.IdleCF_unreduced = IdleCF;

        for (int i = 0; i < 9 && rot.spellMix.getTotalCF() + IdleCF > 1f; i++) {
            List<Float> gains = new ArrayList<Float>();
            if (profile.getAdjustTimeRejuvOrder() == i) gains.add(profile.getAdjustTimeRejuv() / 100f * rot.spellMix.RejuvCF);
            else gains.add(0f);
            if (profile.getAdjustTimeRegrowthOrder() == i) gains.add(profile.getAdjustTimeRegrowth() / 100f * rot.spellMix.RegrowthCF);
            else gains.add(0f);
            if (profile.getAdjustTimeNourishOrder() == i) gains.add(profile.getAdjustTimeNourish() / 100f * rot.spellMix.NourishCF);
            else gains.add(0f);
            if (profile.getAdjustTimeLifebloomOrder() == i) gains.add(profile.getAdjustTimeLifebloom() / 100f * rot.spellMix.LifebloomCF);
            else gains.add(0f);
            if (profile.getAdjustTimeSwiftmendOrder() == i) gains.add(rot.spellMix.getSwiftmendCF());
            else gains.add(0f);
            if (profile.getAdjustTimeWildGrowthOrder() == i) gains.add(profile.getAdjustTimeWildGrowth() / 100f * rot.spellMix.getWildGrowthCF());
            else gains.add(0f);
            if (profile.getAdjustTimeIdleOrder() == i) gains.add(profile.getAdjustTimeIdle() / 100f * IdleCF);
            else gains.add(0f);
            if (profile.getAdjustTimeManagedRejuvOrder() == i) gains.add(profile.getAdjustTimeManagedRejuv() / 100f * rot.spellMix.ManagedRejuvCF);
            else gains.add(0f);
            if (profile.getAdjustTimeManagedRegrowthOrder() == i) gains.add(profile.getAdjustTimeManagedRegrowth() / 100f * rot.spellMix.ManagedRegrowthCF);
            else gains.add(0f);
            if (profile.getAdjustTimeManagedLifebloomStackOrder() == i) gains.add(profile.getAdjustTimeManagedLifebloomStack() / 100f * rot.spellMix.getLifebloomStackCF());
            else gains.add(0f);

            float GainingThisRound = 0f;
            for (float ff : gains) GainingThisRound += ff;
            if (GainingThisRound == 0) continue;
            float Factor = Math.min(GainingThisRound, rot.spellMix.getTotalCF() + IdleCF - 1f) / GainingThisRound;

            if (profile.getAdjustTimeRejuvOrder() == i)
                rot.spellMix.RejuvCF = (float) TreeFormat.round(rot.spellMix.RejuvCF - gains.get(0) * Factor, 6);
            if (profile.getAdjustTimeRegrowthOrder() == i)
                rot.spellMix.RegrowthCF = (float) TreeFormat.round(rot.spellMix.RegrowthCF - gains.get(1) * Factor, 6);
            if (profile.getAdjustTimeNourishOrder() == i)
                rot.spellMix.NourishCF = (float) TreeFormat.round(rot.spellMix.NourishCF - gains.get(2) * Factor, 6);
            if (profile.getAdjustTimeLifebloomOrder() == i)
                rot.spellMix.LifebloomCF = (float) TreeFormat.round(rot.spellMix.LifebloomCF - gains.get(3) * Factor, 6);
            if (profile.getAdjustTimeSwiftmendOrder() == i) {
                // you can't just reduce cast fraction here, because 'recasting lost hots' is calculated
                // alternative version would be to slower swiftmend's healing by the amount of healing expected lost...
                // rot.spellMix.SwiftmendCPM = 0;
                rot.spellMix.setSwiftmendCF((float) TreeFormat.round(rot.spellMix.getSwiftmendCF() - gains.get(4) * Factor, 6));
            }
            if (profile.getAdjustTimeWildGrowthOrder() == i)
                rot.spellMix.setWildGrowthCF((float) TreeFormat.round(rot.spellMix.getWildGrowthCF() - gains.get(5) * Factor, 6));
            if (profile.getAdjustTimeIdleOrder() == i)
                IdleCF = (float) TreeFormat.round(IdleCF - gains.get(6) * Factor, 6);
            if (profile.getAdjustTimeManagedRejuvOrder() == i)
                rot.spellMix.ManagedRejuvCF = (float) TreeFormat.round(rot.spellMix.ManagedRejuvCF - gains.get(7) * Factor, 6);
            if (profile.getAdjustTimeManagedRegrowthOrder() == i)
                rot.spellMix.ManagedRegrowthCF = (float) TreeFormat.round(rot.spellMix.ManagedRegrowthCF - gains.get(8) * Factor, 6);
            if (profile.getAdjustTimeManagedLifebloomStackOrder() == i)
                rot.spellMix.setLifebloomStackCF((float) TreeFormat.round(rot.spellMix.getLifebloomStackCF() - gains.get(9) * Factor, 6));
        }

        if (TreeFormat.round(rot.spellMix.getTotalCF() + IdleCF, 4) > 1f) {
            // float Factor = (1f - rot.spellMix.SwiftmendCF) / (rot.spellMix.TotalCF + IdleCF - rot.spellMix.SwiftmendCF);
            float Factor = 1f / (rot.spellMix.getTotalCF() + IdleCF);
            rot.spellMix.setLifebloomStackCF(rot.spellMix.getLifebloomStackCF() * Factor);
            rot.spellMix.RejuvCF *= Factor;
            rot.spellMix.RegrowthCF *= Factor;
            rot.spellMix.NourishCF *= Factor;
            rot.spellMix.LifebloomCF *= Factor;
            rot.spellMix.setSwiftmendCF(rot.spellMix.getSwiftmendCF() * Factor);
            rot.spellMix.setWildGrowthCF(rot.spellMix.getWildGrowthCF() * Factor);
            rot.spellMix.ManagedRejuvCF *= Factor;
            rot.spellMix.ManagedRegrowthCF *= Factor;
            rot.spellMix.setLifebloomStackCF(rot.spellMix.getLifebloomStackCF() * Factor);
            // Now, rot.MaxPrimaryCF *must* be 1f exactly.
        }

        // Correct going OOM
        rot.TimeToOOM_unreduced = rot.getTimeToOOM();
        rot.IdleCF_unreducedOOM = rot.spellMix.getIdleCF();
        rot.RejuvCF_unreducedOOM = rot.spellMix.RejuvCF;
        rot.RegrowthCF_unreducedOOM = rot.spellMix.RegrowthCF;
        rot.ManagedRejuvCF_unreducedOOM = rot.spellMix.ManagedRejuvCF;
        rot.ManagedRegrowthCF_unreducedOOM = rot.spellMix.ManagedRegrowthCF;
        rot.LifebloomCF_unreducedOOM = rot.spellMix.LifebloomCF;
        rot.NourishCF_unreducedOOM = rot.spellMix.NourishCF;
        rot.WildGrowthCF_unreducedOOM = rot.spellMix.getWildGrowthCF();

        // If you cast less, you will also see less returns. This is calculated elsewhere
        // in the Special Effects. Repeated calculations are necessary.
        // We assume this effect is convergent.

        float f = Math.max(0.0f, (rot.getTimeToOOM() * (rot.spellMix.getMPS() - rot.getManaRegen()) + rot.TotalTime * rot.getManaRegen()) / (rot.spellMix.getMPS() * rot.TotalTime));
        // We want our MPS to become f*MPS

        float MPStoGain = rot.spellMix.getMPS() * (1f - f);

        for (int i = 0; i < 5 && MPStoGain > 0; i++) {
            List<Float> gains = new ArrayList<Float>();
            if (profile.getReduceOOMRejuvOrder() == i) {
                float r = Math.min(1f, (MPStoGain / rot.spellMix.rejuvenate.getManaCost() * rot.spellMix.rejuvenate.getCastTime()));
                float s = Math.max(rot.spellMix.RejuvCF * (1f - profile.getReduceOOMRejuv() / 100f), rot.spellMix.RejuvCF - r);
                gains.add(rot.spellMix.rejuvenate.getManaCost() * (rot.RejuvCF_unreducedOOM - s) / rot.spellMix.rejuvenate.getCastTime());
            } else gains.add(0f);
            if (profile.getReduceOOMRegrowthOrder() == i) {
                float r = Math.min(1f, (MPStoGain / rot.spellMix.regrowth.getManaCost() * rot.spellMix.regrowth.getCastTime()));
                float s = Math.max(rot.spellMix.RegrowthCF * (1f - profile.getReduceOOMRegrowth() / 100f), rot.spellMix.RegrowthCF - r);
                gains.add(rot.spellMix.regrowth.getManaCost() * (rot.RegrowthCF_unreducedOOM - s) / rot.spellMix.regrowth.getCastTime());
            } else gains.add(0f);
            if (profile.getReduceOOMLifebloomOrder() == i) {
                float r = Math.min(1f, (MPStoGain / rot.spellMix.lifebloom.getManaCost() * rot.spellMix.lifebloom.getCastTime()));
                float s = Math.max(rot.spellMix.LifebloomCF * (1f - profile.getReduceOOMLifebloom() / 100f), rot.spellMix.LifebloomCF - r);
                gains.add(rot.spellMix.lifebloom.getManaCost() * (rot.LifebloomCF_unreducedOOM - s) / rot.spellMix.lifebloom.getCastTime());
            } else gains.add(0f);
            if (profile.getReduceOOMNourishOrder() == i) {
                float r = Math.min(1f, (MPStoGain / rot.spellMix.nourish[0].getManaCost() * rot.spellMix.nourish[0].getCastTime()));
                float s = Math.max(rot.spellMix.NourishCF * (1f - profile.getReduceOOMNourish() / 100f), rot.spellMix.NourishCF - r);
                gains.add(rot.spellMix.nourish[0].getManaCost() * (rot.NourishCF_unreducedOOM - s) / rot.spellMix.nourish[0].getCastTime());
            } else gains.add(0f);
            if (profile.getReduceOOMWildGrowthOrder() == i) {
                float r = Math.min(1f, (MPStoGain / rot.spellMix.wildGrowth.getManaCost() * rot.spellMix.wildGrowth.getCastTime()));
                float s = Math.max(rot.spellMix.getWildGrowthCF() * (1f - profile.getReduceOOMWildGrowth() / 100f), rot.spellMix.getWildGrowthCF() - r);
                gains.add(rot.spellMix.wildGrowth.getManaCost() * (rot.WildGrowthCF_unreducedOOM - s) / rot.spellMix.wildGrowth.getCastTime());
            } else gains.add(0f);

            float GainingThisRound = 0f;
            for (float ff : gains) GainingThisRound += ff;
            if (GainingThisRound == 0) continue;
            float Factor = Math.min(GainingThisRound, MPStoGain) / GainingThisRound;
            MPStoGain -= Math.min(GainingThisRound, MPStoGain);

            if (profile.getReduceOOMRejuvOrder() == i) {
                rot.spellMix.RejuvCF -= gains.get(0) * Factor / rot.spellMix.rejuvenate.getManaCost() * rot.spellMix.rejuvenate.getCastTime();
                rot.spellMix.RejuvCF = (float) TreeFormat.round(rot.spellMix.RejuvCF, 6);
            }
            if (profile.getReduceOOMRegrowthOrder() == i) {
                rot.spellMix.RegrowthCF -= gains.get(1) * Factor / rot.spellMix.regrowth.getManaCost() * rot.spellMix.regrowth.getCastTime();
                rot.spellMix.RegrowthCF = (float) TreeFormat.round(rot.spellMix.RegrowthCF, 6);
            }
            if (profile.getReduceOOMLifebloomOrder() == i) {
                rot.spellMix.LifebloomCF -= gains.get(2) * Factor / rot.spellMix.lifebloom.getManaCost() * rot.spellMix.lifebloom.getCastTime();
                rot.spellMix.LifebloomCF = (float) TreeFormat.round(rot.spellMix.LifebloomCF, 6);
            }
            if (profile.getReduceOOMNourishOrder() == i) {
                rot.spellMix.NourishCF -= gains.get(3) * Factor / rot.spellMix.nourish[0].getManaCost() * rot.spellMix.nourish[0].getCastTime();
                rot.spellMix.NourishCF = (float) TreeFormat.round(rot.spellMix.NourishCF, 6);
            }
            if (profile.getReduceOOMWildGrowthOrder() == i) {
                rot.spellMix.setWildGrowthCF(rot.spellMix.getWildGrowthCF() - gains.get(4) * Factor / rot.spellMix.wildGrowth.getManaCost() * rot.spellMix.wildGrowth.getCastTime());
                rot.spellMix.setWildGrowthCF((float) TreeFormat.round(rot.spellMix.getWildGrowthCF(), 6));
            }
        }

        return rot;
    }

    public static String singleTargetRotationToText(SingleTargetBurstRotations rotation) {
        switch (rotation) {
            case AutoSelect:
                return "Autoselect";
            case AutoSelectForceSwiftmend:
                return "Autoselect + Swiftmend";
            case RjN:
                return "RjN*";
            case RjLbN:
                return "RjLbN*";
            case RjRgN:
                return "RjRgN*";
            case RjRgLbN:
                return "RjRgLbN*";
            case RjRg:
                return "RjRg*";
            case RjLbRg:
                return "RjLbRg*";
            case Rg:
                return "Rg*";
            case RgN:
                return "RgN*";
            case N:
                return "N*";
            case RjN_s:
                return "RjN*+S";
            case RjLbN_s:
                return "RjLbN*+S";
            case RjRgN_s:
                return "RjRgN*+S";
            case RjRgLbN_s:
                return "RjRgLbN*+S";
            case RjRg_s:
                return "RjRg*+S";
            case RjLbRg_s:
                return "RjLbRg*+S";
            case Rg_s:
                return "Rg*+S";
            case RgN_s:
                return "RgN*+S";
            default:
                return "Unknown";
        }
    }

    public static SingleTargetBurstRotations singleTargetIndexToRotation(int index) {
        switch (index) {
            case 1:
                return SingleTargetBurstRotations.AutoSelectForceSwiftmend;
            case 2:
                return SingleTargetBurstRotations.RjN;
            case 3:
                return SingleTargetBurstRotations.RjLbN;
            case 4:
                return SingleTargetBurstRotations.RjRgN;
            case 5:
                return SingleTargetBurstRotations.RjRgLbN;
            case 6:
                return SingleTargetBurstRotations.RjRg;
            case 7:
                return SingleTargetBurstRotations.RjLbRg;
            case 8:
                return SingleTargetBurstRotations.Rg;
            case 9:
                return SingleTargetBurstRotations.RgN;
            case 10:
                return SingleTargetBurstRotations.N;
            case 11:
                return SingleTargetBurstRotations.RjN_s;
            case 12:
                return SingleTargetBurstRotations.RjLbN_s;
            case 13:
                return SingleTargetBurstRotations.RjRgN_s;
            case 14:
                return SingleTargetBurstRotations.RjRgLbN_s;
            case 15:
                return SingleTargetBurstRotations.RjRg_s;
            case 16:
                return SingleTargetBurstRotations.RjLbRg_s;
            case 17:
                return SingleTargetBurstRotations.Rg_s;
            case 18:
                return SingleTargetBurstRotations.RgN_s;
            case 0:
            default:
                return SingleTargetBurstRotations.AutoSelect;
        }
    }

    public static SingleTargetBurstResult[] calculateSingleTargetBurst(CharacterCalculationsTree calculatedStats, Stats stats, CalculationOptionsTree calcOpts, SingleTargetBurstRotations rotation) {
        SpellMix spellMix = new SpellMix(calculatedStats.getLocalCharacter(), stats);

        CalculationOptionsTree.SpellProfile profile = calcOpts.getCurrent();

        switch (profile.getLifebloomStackType()) {
            case 0:
                spellMix.setLifebloomStackType(LifeBloomType.Slow);
                break;
            case 1:
                spellMix.setLifebloomStackType(LifeBloomType.Fast);
                break;
            case 2:
                spellMix.setLifebloomStackType(LifeBloomType.Rolling);
                break;
            case 3:
                spellMix.setLifebloomStackType(LifeBloomType.Slow2);
                break;
            case 4:
                spellMix.setLifebloomStackType(LifeBloomType.Fast2);
                break;
            default:
                // err...
                spellMix.setLifebloomStackType(LifeBloomType.Rolling);
                break;
        }

        float RejuvFraction = spellMix.rejuvenate.getCastTime() / spellMix.rejuvenate.getDuration();
        float RegrowthFraction = spellMix.regrowth.getCastTime() / spellMix.regrowth.getDuration();
        float LifebloomFraction = spellMix.getLifebloomStackSpell().getCastTime() / spellMix.getLifebloomStackSpell().getDuration();
        float SwiftFraction = spellMix.swiftmend.getCastTime() * 4f / 60f;

        // RJ + N1
        SingleTargetBurstResult RjN1 = new SingleTargetBurstResult(SingleTargetBurstRotations.RjN,
                spellMix, RejuvFraction, 0, 0, 0, (1f - RejuvFraction), false);
        // RJ + LB + N2
        SingleTargetBurstResult RjLbN2 = new SingleTargetBurstResult(SingleTargetBurstRotations.RjLbN,
                spellMix, RejuvFraction, 0, LifebloomFraction, 0, (1f - RejuvFraction - LifebloomFraction), false);
        // RJ + RG + N2
        SingleTargetBurstResult RjRgN2 = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRgN,
                spellMix, RejuvFraction, RegrowthFraction, 0, 0, (1f - RejuvFraction - RegrowthFraction), false);
        // RJ + RG + LB + N3
        SingleTargetBurstResult RjRgLbN3 = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRgLbN,
                spellMix, RejuvFraction, RegrowthFraction, LifebloomFraction, 0,
                (1f - RejuvFraction - RegrowthFraction - LifebloomFraction), false);
        // RJ + RG
        SingleTargetBurstResult RjRg = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRg,
                spellMix, RejuvFraction, (1f - RejuvFraction), 0, 0, 0, true);
        // RJ + LB + RG
        SingleTargetBurstResult RjLbRg = new SingleTargetBurstResult(SingleTargetBurstRotations.RjLbRg,
                spellMix, RejuvFraction, (1f - RejuvFraction - LifebloomFraction), LifebloomFraction, 0, 0, true);
        // RG
        SingleTargetBurstResult Rg = new SingleTargetBurstResult(SingleTargetBurstRotations.Rg,
                spellMix, 0, 1f, 0, 0, 0, true);
        // RG + N1
        SingleTargetBurstResult RgN1 = new SingleTargetBurstResult(SingleTargetBurstRotations.RgN,
                spellMix, 0, RegrowthFraction, 0, 0, (1f - RegrowthFraction), false);
        // N0
        SingleTargetBurstResult N0 = new SingleTargetBurstResult(SingleTargetBurstRotations.N,
                spellMix, 0, 0, 0, 0, 1f, false);

        // RJ + N1 S
        SingleTargetBurstResult RjN1S = new SingleTargetBurstResult(SingleTargetBurstRotations.RjN_s,
                spellMix, RejuvFraction, 0, 0, 4, (1f - RejuvFraction - SwiftFraction), false);
        // RJ + LB + N2 S
        SingleTargetBurstResult RjLbN2S = new SingleTargetBurstResult(SingleTargetBurstRotations.RjLbN_s,
                spellMix, RejuvFraction, 0, LifebloomFraction, 4,
                (1f - RejuvFraction - LifebloomFraction - SwiftFraction), false);
        // RJ + RG + N2 S
        SingleTargetBurstResult RjRgN2S = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRgN_s,
                spellMix, RejuvFraction, RegrowthFraction, 0, 4,
                (1f - RejuvFraction - RegrowthFraction - SwiftFraction), false);
        // RJ + RG + LB + N3 S
        SingleTargetBurstResult RjRgLbN3S = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRgLbN_s,
                spellMix, RejuvFraction, RegrowthFraction, LifebloomFraction, 4,
                (1f - RejuvFraction - RegrowthFraction - LifebloomFraction - SwiftFraction), false);
        // RJ + RG S
        SingleTargetBurstResult RjRgS = new SingleTargetBurstResult(SingleTargetBurstRotations.RjRg_s,
                spellMix, RejuvFraction, (1f - RejuvFraction - SwiftFraction), 0, 4, 0, true);
        // RJ + LB + RG S
        SingleTargetBurstResult RjLbRgS = new SingleTargetBurstResult(SingleTargetBurstRotations.RjLbRg_s,
                spellMix, RejuvFraction, (1f - RejuvFraction - LifebloomFraction - SwiftFraction),
                LifebloomFraction, 4, 0, true);
        // RG S
        SingleTargetBurstResult RgS = new SingleTargetBurstResult(SingleTargetBurstRotations.Rg_s,
                spellMix, 0, (1f - SwiftFraction), 0, 4, 0, true);
        // RG + N1 S
        SingleTargetBurstResult RgN1S = new SingleTargetBurstResult(SingleTargetBurstRotations.RgN_s,
                spellMix, 0, RegrowthFraction, 0, 4, (1f - RegrowthFraction - SwiftFraction), false);

        SingleTargetBurstResult result = new SingleTargetBurstResult(SingleTargetBurstRotations.AutoSelect,
                spellMix, 0, 0, 0, 0, 0, false);

        if (rotation == SingleTargetBurstRotations.AutoSelect ||
                rotation == SingleTargetBurstRotations.AutoSelectForceSwiftmend) {
            if (rotation != SingleTargetBurstRotations.AutoSelectForceSwiftmend) {
                if (RjN1.getHPS() > result.getHPS()) result = RjN1;
                if (RjLbN2.getHPS() > result.getHPS()) result = RjLbN2;
                if (RjRgN2.getHPS() > result.getHPS()) result = RjRgN2;
                if (RjRgLbN3.getHPS() > result.getHPS()) result = RjRgLbN3;
                if (RjRg.getHPS() > result.getHPS()) result = RjRg;
                if (RjLbRg.getHPS() > result.getHPS()) result = RjLbRg;
                if (Rg.getHPS() > result.getHPS()) result = Rg;
                if (N0.getHPS() > result.getHPS()) result = N0;
            }

            if (RjN1S.getHPS() > result.getHPS()) result = RjN1S;
            if (RjLbN2S.getHPS() > result.getHPS()) result = RjLbN2S;
            if (RjRgN2S.getHPS() > result.getHPS()) result = RjRgN2S;
            if (RjRgLbN3S.getHPS() > result.getHPS()) result = RjRgLbN3S;
            if (RjRgS.getHPS() > result.getHPS()) result = RjRgS;
            if (RjLbRgS.getHPS() > result.getHPS()) result = RjLbRgS;
            if (RgS.getHPS() > result.getHPS()) result = RgS;
            if (RgN1S.getHPS() > result.getHPS()) result = RgN1S;
        } else {
            switch (rotation) {
                case RjN:
                    result = RjN1;
                    break;
                case RjLbN:
                    result = RjLbN2;
                    break;
                case RjRgN:
                    result = RjRgN2;
                    break;
                case RjRgLbN:
                    result = RjRgLbN3;
                    break;
                case RjRg:
                    result = RjRg;
                    break;
                case RjLbRg:
                    result = RjLbRg;
                    break;
                case Rg:
                    result = Rg;
                    break;
                case RgN:
                    result = RgN1;
                    break;
                case N:
                    result = N0;
                    break;
                case RjN_s:
                    result = RjN1S;
                    break;
                case RjLbN_s:
                    result = RjLbN2S;
                    break;
                case RjRgN_s:
                    result = RjRgN2S;
                    break;
                case RjRgLbN_s:
                    result = RjRgLbN3S;
                    break;
                case RjRg_s:
                    result = RjRgS;
                    break;
                case RjLbRg_s:
                    result = RjLbRgS;
                    break;
                case Rg_s:
                    result = RgS;
                    break;
                case RgN_s:
                    result = RgN1S;
                    break;
                default:
                    break;
            }
        }

        List<SingleTargetBurstResult> rots = new ArrayList<SingleTargetBurstResult>();
        rots.add(result);
        rots.add(result);
        rots.add(RjN1);
        rots.add(RjLbN2);
        rots.add(RjRgN2);
        rots.add(RjRgLbN3);
        rots.add(RjRg);
        rots.add(RjLbRg);
        rots.add(Rg);
        rots.add(RgN1);
        rots.add(N0);
        rots.add(RjN1S);
        rots.add(RjLbN2S);
        rots.add(RjRgN2S);
        rots.add(RjRgLbN3S);
        rots.add(RjRgS);
        rots.add(RjLbRgS);
        rots.add(RgS);
        rots.add(RgN1S);

        return rots.toArray(new SingleTargetBurstResult[0]);
    }
}

final class TreeConstants {
    // Master is now in Base.BaseStats and Base.StatConversion
    public static float BaseMana; // Keep since this is more convenient reference than calling the whole BaseStats function every time
}
