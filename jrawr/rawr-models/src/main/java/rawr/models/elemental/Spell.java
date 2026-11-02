// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator helpers add()/multiply(), the virtual Clone()/MemberwiseClone and
// ApplyEM() (throws NotImplementedException in C# and is never called) are omitted; nothing in the
// model uses them.
package rawr.models.elemental;

import rawr.base.stats.StatConversion;

public abstract class Spell {
    protected float baseMinDamage = 0f;
    protected float baseMaxDamage = 0f;
    protected float baseCastTime = 0f;
    protected float castTime = 0f;
    protected float periodicTick = 0f;
    protected float periodicTicks = 0f;
    protected float periodicTickTime = 3f;
    protected float manaCost = 0f;
    protected float gcd = 1.5f;
    protected float crit = 0f;
    protected float critModifier = 1f;
    protected float cooldown = 0f;
    protected float missChance = .17f;

    protected float totalCoef = 1f;
    protected float directCoefBonus = 0f;
    protected float baseCoef = 1f;
    protected float spCoef = 0f;
    protected float dotBaseCoef = 1f;
    protected float dotSpCoef = 0f;
    protected float dotCanCrit = 0f;
    protected float dotCritModifier = 1f;

    protected float spellPower = 0f;

    protected float latencyGcd = .15f;
    protected float latencyCast = .075f;

    /**
     * This Constructor calls SetBaseValues.
     */
    public Spell() {
        setBaseValues();
    }

    protected void setBaseValues() {
        baseMinDamage = 0f;
        baseMaxDamage = 0f;
        baseCastTime = 0f;
        castTime = 0f;
        periodicTick = 0f;
        periodicTicks = 0f;
        periodicTickTime = 3f;
        manaCost = 0f;
        gcd = 1.5f;
        crit = 0f;
        critModifier = 1f;
        cooldown = 0f;
        missChance = .17f;

        totalCoef = 1f;
        baseCoef = 1f;
        spCoef = 0f;
        dotBaseCoef = 1f;
        dotSpCoef = 0f;
        dotCritModifier = 1f;
        directCoefBonus = 0f;
        dotCanCrit = 0f;
        spellPower = 0f;
    }

    public void update(ISpellArgs args) {
        setBaseValues();
        initialize(args);
    }

    protected String shortName = "Spell";

    public float getMinHit() {
        return (totalCoef + directCoefBonus) * (baseMinDamage * baseCoef + spellPower * spCoef);
    }

    public float getMinCrit() {
        return getMinHit() * (1 + critModifier);
    }

    public float getMaxHit() {
        return (totalCoef + directCoefBonus) * (baseMaxDamage * baseCoef + spellPower * spCoef);
    }

    public float getMaxCrit() {
        return getMaxHit() * (1 + critModifier);
    }

    public float getAvgHit() {
        return (getMinHit() + getMaxHit()) / 2;
    }

    public float getAvgCrit() {
        return (getMinCrit() + getMaxCrit()) / 2;
    }

    public float getAvgDamage() {
        return (1f - getCritChance()) * getAvgHit() + getCritChance() * getAvgCrit();
    }

    public float getMinDamage() {
        return (1f - getCritChance()) * getMinHit() + getCritChance() * getMinCrit();
    }

    public float getMaxDamage() {
        return (1f - getCritChance()) * getMaxHit() + getCritChance() * getMaxCrit();
    }

    /**
     * This is to ensure that the constraints on the GCD are met on abilities that have Gcd
     */
    public float getGcd() {
        return (gcd >= 1 ? gcd : 1);
    }

    /**
     * The effective Cast Time. Taking GCD and latency into account.
     */
    public float getCastTime() {
        if (gcd == 0 && castTime == 0)
            return 0;
        if (castTime > getGcd())
            return castTime + getLatency();
        else
            return getGcd() + getLatency();
    }

    /**
     * The effective Latency of this spell effecting the start cast time of the next one.
     */
    public float getLatency() {
        if (gcd == 0 && castTime == 0)
            return 0;
        if (castTime >= gcd)
            return latencyCast;
        else
            return latencyGcd;
    }

    public float getBaseCastTime() {
        return Math.max(baseCastTime, 1.5f);
    }

    public float getCastTimeWithoutGCD() {
        return castTime;
    }

    public float getCritChance() {
        return Math.min(1f, crit);
    }

    /**
     * Crit chance for all kind of proc triggers (e.g. Clearcasting). This exists seperately because
     * of Lightning Overload.
     */
    public float getCCCritChance() {
        return getCritChance();
    }

    public float getMissChance() {
        return missChance;
    }

    public float getHitChance() {
        return 1f - missChance;
    }

    public float getDamageFromSpellPower() {
        return spellPower * spCoef * totalCoef;
    }

    public float getPeriodicTick() {
        return totalCoef * (periodicTick * dotBaseCoef + spellPower * dotSpCoef) * (1 + dotCanCrit * dotCritModifier * getCritChance());
    }

    public float getPeriodicTicks() {
        return periodicTicks;
    }

    public void setPeriodicTicks(float value) {
        periodicTicks = value;
        if (periodicTicks < 0)
            periodicTicks = 0;
    }

    public float periodicDamage() {
        return periodicDamage(getDuration());
    }

    public float periodicDamage(float duration) {
        if (getPeriodicTickTime() <= 0 || duration <= 0)
            return 0;
        int effectiveTicks = (int) Math.floor(Math.min(duration, getDuration()) / getPeriodicTickTime());
        return getPeriodicTick() * effectiveTicks;
    }

    public float getTotalDamage() {
        return getAvgDamage() + periodicDamage();
    }

    public float getDirectDpS() {
        return getAvgDamage() / getCastTime();
    }

    public float getPeriodicDpS() {
        return getPeriodicTick() / periodicTickTime;
    }

    public float getPeriodicTickTime() {
        return periodicTickTime;
    }

    public float getDpM() {
        return getTotalDamage() / manaCost;
    }

    public float getDpCT() {
        return getTotalDamage() / getCastTime();
    }

    public float getDpPR() {
        return getTotalDamage() / getPeriodicRefreshTime();
    }

    public float getDpCDR() {
        return getTotalDamage() / getCDRefreshTime();
    }

    public float getCTpDuration() {
        return getDuration() > 0 ? getCastTime() / getDuration() : 1f;
    }

    public float getDuration() {
        return periodicTicks * periodicTickTime;
    }

    public float getCooldown() {
        return cooldown;
    }

    public float getPeriodicRefreshTime() {
        return (getDuration() > getCDRefreshTime() ? getDuration() : getCDRefreshTime());
    }

    public float getCDRefreshTime() {
        return cooldown > getCastTime() ? cooldown + castTime : getCastTime();
    }

    public float getManaCost() {
        return manaCost;
    }

    public void initialize(ISpellArgs args) {
        float speed = (1f + args.getStats().getSpellHaste()) * (1f + StatConversion.getSpellHasteFromRating(args.getStats().getHasteRating()));
        gcd = (float) netRound(gcd / speed, 4);
        castTime = (float) netRound(castTime / speed, 4);
        latencyGcd = args.getLatencyGcd();
        latencyCast = args.getLatencyCast();
        critModifier += .2f * args.getTalents().getElementalFury();
        critModifier *= (float) netRound(1.5f * (1f + args.getStats().getBonusSpellCritMultiplier()) - 1f, 6);
        dotCritModifier += .2f * args.getTalents().getElementalFury();
        dotCritModifier *= (float) netRound(1.5f * (1f + args.getStats().getBonusSpellCritMultiplier()) - 1f, 6);
        //critModifier += 1f;
        spellPower += args.getStats().getSpellPower();
        crit += args.getStats().getSpellCrit();
        missChance -= args.getStats().getSpellHit();
        totalCoef *= 1 + args.getStats().getBonusDamageMultiplier(); //ret + bm buff
        if (missChance < 0) missChance = 0;
        manaCost = (float) Math.floor(manaCost);
        //base resistance by level
        totalCoef *= 1f - StatConversion.getAverageResistance(80, 83, 0, 0);
    }

    public void applyDotHaste(ISpellArgs args) {
        float speed = (1f + args.getStats().getSpellHaste()) * (1f + StatConversion.getSpellHasteFromRating(args.getStats().getHasteRating()));
        periodicTickTime = (float) netRound(periodicTickTime / speed, 4);
    }

    /** .NET Math.Round(double, digits) behaves as rescaled banker's rounding on these inputs. */
    static double netRound(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }

    @Override
    public String toString() {
        return shortName;
    }
}
