// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.Base.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;
import rawr.base.stats.StatConversion;

/**
 * This class will be the base class for abilities.
 */
abstract class AbilityDK_Base {
    // Constants
    public static final int MIN_GCD_MS = 1000;
    public static final int INSTANT = 1;
    public static final int MELEE_RANGE = 5;
    public static final float THREAT_FROST_PRESENCE = 2.0735f;

    public AbilityDK_Base() {
        this.name = "";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 0;
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 0;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 0;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 0;
        this.minDamage = 0;
        this.maxDamage = 0;
        this.range = MELEE_RANGE;
        this.damageType = ItemDamageType.Physical;
        this.setCooldown(1500); // GCD
        this.setCastTime(INSTANT);
        this.setTickRate(INSTANT);
        this.setDuration(INSTANT);
    }

    public AbilityDK_Base(String name) {
        this.name = name;
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 0;
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 0;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 0;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 0;
        this.minDamage = 0;
        this.maxDamage = 0;
        this.range = MELEE_RANGE;
        this.damageType = ItemDamageType.Physical;
        this.setCooldown(1500); // GCD
        this.setCastTime(INSTANT);
        this.setTickRate(INSTANT);
        this.setDuration(INSTANT);
    }

    public AbilityDK_Base(CombatState cs) {
        this.cState = cs;
        this.name = "";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 0;
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 0;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 0;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 0;
        this.minDamage = 0;
        this.maxDamage = 0;
        this.range = MELEE_RANGE;
        this.damageType = ItemDamageType.Physical;
        this.setCooldown(1500); // GCD
        this.setCastTime(INSTANT);
        this.setTickRate(INSTANT);
        this.setDuration(INSTANT);
    }

    /** The state of combat at the time the ability is used. */
    protected CombatState cState;

    /**
     * Any DK ability triggered by this ability.
     * Should not be recursive.
     * This would mean FF when using IT or Glyphed HB.
     */
    public AbilityDK_Base triggeredAbility;

    /** Name of the ability. */
    public String name;

    /**
     * What is the cost of the ability?
     * Use the DKCostTypes ordinal for placement.
     * Negative costs mean they grant that item.
     */
    public int[] abilityCost = new int[DKCostTypes.NumCostTypes.ordinal()];

    /** What min damage does the ability cause? For DOTs damage is on a per-tick basis. */
    public int minDamage;

    /** What max damage does the ability cause? For DOTs damage is on a per-tick basis. */
    public int maxDamage;

    /**
     * Get the average value between Max and Min damage
     * For DOTs damage is on a per-tick basis.
     */
    public int getBaseDamage() {
        int avgDam = (this.minDamage + this.maxDamage) / 2;
        int wDam = 0;
        // Handle non-weapon based effects:
        if (null != this.mh) {
            wDam = (int) (this.mh.damage * this.weaponDamageModifier);
        }
        // Average out the min & max damage, then add in baseDamage from the weapon.
        // Factor in miss rate based on HIT
        float chanceMiss = StatConversion.WHITE_MISS_CHANCE_CAP[3];
        chanceMiss -= cState.m_Stats.getPhysicalHit();
        chanceMiss = Math.max(0f, chanceMiss);

        return (int) ((avgDam + wDam) * (1 - chanceMiss));
    }

    public void setBaseDamage(int value) {
        // Setup so that we can just set a base damage w/o having to
        // manually set Min & Max all the time.
        maxDamage = minDamage = value;
    }

    /** What's the range of a given ability? */
    public int range;

    /** What's the size of the area of a given ability? */
    public int area;

    /** Is this an AOE ability? */
    public boolean aoe;

    /** What type of damage does this ability do? */
    public ItemDamageType damageType;

    // Time Based Items
    /** How long does it take to cast in ms? 1 == instant */
    public int getCastTime() {
        return abilityCost[DKCostTypes.CastTime.ordinal()];
    }

    public void setCastTime(int value) {
        abilityCost[DKCostTypes.CastTime.ordinal()] = value;
    }

    /**
     * Cooldown in seconds
     * Default = 1500 millisecs == Global Cooldown
     * GCD min == 1000 millisecs.
     */
    public int getCooldown() {
        if (this.triggersGcd)
            return Math.max(MIN_GCD_MS, abilityCost[DKCostTypes.CooldownTime.ordinal()]);
        return abilityCost[DKCostTypes.CooldownTime.ordinal()];
    }

    public void setCooldown(int value) {
        abilityCost[DKCostTypes.CooldownTime.ordinal()] = value;
    }

    /** Does this ability trigger the GCD? */
    public boolean triggersGcd;

    /** How long does the effect last? This is in millisecs. */
    public int getDuration() {
        return Math.max(INSTANT, abilityCost[DKCostTypes.DurationTime.ordinal()]);
    }

    public void setDuration(int value) {
        abilityCost[DKCostTypes.DurationTime.ordinal()] = Math.max(INSTANT, value);
    }

    private int tickRate;

    /**
     * How often does the effect proc for?
     * Tick rate is millisecs.
     * Ensure that we don't have a 0 value.
     * 1 ms == instant.
     */
    public int getTickRate() {
        return Math.max(INSTANT, this.tickRate);
    }

    public void setTickRate(int value) {
        this.tickRate = Math.max(INSTANT, value);
    }

    // Weapon related Items
    public boolean weaponRequired;
    public Weapon mh, oh;
    public float weaponDamageModifier;

    /** Get the single instance damage of this ability. */
    public int getTickDamage() {
        // Start w/ getting the base damage values.
        int iDamage = this.getBaseDamage();
        // Apply modifiers.
        iDamage += this.getDamageAdditiveModifier();
        iDamage = (int) Math.floor((float) iDamage * (1 + getDamageMultiplierModifier()));
        return iDamage;
    }

    /** Get the full effect over the lifetime of the ability. */
    public int getTotalDamage() {
        // Start w/ getting the base damage values.
        int iDamage = this.getTickDamage();
        // Assuming full duration, or standard impact.
        // Also need to decide if I want this to be whole ticks, or if partial ticks will be allowed.
        float fDamageCount = (float) (this.getDuration() / this.getTickRate());
        // To prevent divide by 0 errors.
        if (Float.isNaN(fDamageCount)) {
            // Ensure that the Damage counts off at least once.
            fDamageCount = 1;
        }
        iDamage = (int) ((float) iDamage * fDamageCount);
        if (aoe == true) {
            iDamage = (int) ((float) iDamage * this.cState.m_NumberOfTargets);
        }
        return iDamage;
    }

    private int damageAdditiveModifier;

    /** Setup the modifier formula for a given ability. */
    public int getDamageAdditiveModifier() {
        return this.damageAdditiveModifier;
    }

    public void setDamageAdditiveModifier(int value) {
        this.damageAdditiveModifier = value;
    }

    private float damageMultiplierModifier;

    /** Setup the modifier formula for a given ability. */
    public float getDamageMultiplierModifier() {
        return this.damageMultiplierModifier;
    }

    public void setDamageMultiplierModifier(float value) {
        this.damageMultiplierModifier = value;
    }

    /** How much to multiply the damage by to generate threat. */
    private float threatMultiplier;

    public float getThreatMultiplier() {
        return this.threatMultiplier;
    }

    public void setThreatMultiplier(float value) {
        this.threatMultiplier = value;
    }

    private float threatAdditiveModifier;

    /** Get the full effect of threat over the lifetime of the ability. */
    public float getTotalThreat() {
        return getTotalThreatValue();
    }

    public float getTotalThreatValue() {
        return (StatConversion.applyMultiplier(getTotalDamage(), getThreatMultiplier()) + this.threatAdditiveModifier) * THREAT_FROST_PRESENCE;
    }

    public void setTotalThreat(float value) {
        this.threatAdditiveModifier = value;
    }

    // Comparisons
    public static int compareByThreatPerCost(AbilityDK_Base a, AbilityDK_Base b, DKCostTypes t) {
        int ic = 0;
        float aRunes = 1;
        float bRunes = 1;

        // Sum of cost:
        switch (t) {
            case Blood:
                aRunes = a.abilityCost[DKCostTypes.Blood.ordinal()];
                bRunes = b.abilityCost[DKCostTypes.Blood.ordinal()];
                break;
            case Frost:
                aRunes = a.abilityCost[DKCostTypes.Frost.ordinal()];
                bRunes = b.abilityCost[DKCostTypes.Frost.ordinal()];
                break;
            case UnHoly:
                aRunes = a.abilityCost[DKCostTypes.UnHoly.ordinal()];
                bRunes = b.abilityCost[DKCostTypes.UnHoly.ordinal()];
                break;
            case Death:
                aRunes = a.abilityCost[DKCostTypes.Blood.ordinal()] + a.abilityCost[DKCostTypes.Frost.ordinal()] + a.abilityCost[DKCostTypes.UnHoly.ordinal()];
                bRunes = b.abilityCost[DKCostTypes.Blood.ordinal()] + b.abilityCost[DKCostTypes.Frost.ordinal()] + b.abilityCost[DKCostTypes.UnHoly.ordinal()];
                break;
            case CastTime:
            case CooldownTime:
            case DurationTime:
                aRunes = a.abilityCost[t.ordinal()] / 1000;
                bRunes = b.abilityCost[t.ordinal()] / 1000;
                break;
            case RunicPower:
                aRunes = a.abilityCost[t.ordinal()];
                bRunes = b.abilityCost[t.ordinal()];
                break;
            default:
                aRunes = 1;
                bRunes = 1;
                break;
        }
        if (aRunes != 0 || bRunes != 0) {
            if (aRunes != 0 && bRunes != 0) {
                float avalue = a.getTotalThreat() / aRunes;
                float bvalue = b.getTotalThreat() / bRunes;
                if (avalue != bvalue) {
                    // This is setup where we want a descending order.
                    if (avalue > bvalue)
                        ic = -1;
                    else
                        ic = 1;
                }
            } else // one of them are 0
            {
                if (aRunes > bRunes)
                    ic = -1;
                else
                    ic = 1;
            }
        }
        return ic;
    }

    public static int compareByTotalThreat(AbilityDK_Base a, AbilityDK_Base b) {
        return compareByThreatPerCost(a, b, DKCostTypes.NumCostTypes);
    }

    public static int compareThreatByCooldown(AbilityDK_Base a, AbilityDK_Base b) {
        return compareByThreatPerCost(a, b, DKCostTypes.CooldownTime);
    }

    public static int compareThreatByRunes(AbilityDK_Base a, AbilityDK_Base b) {
        return compareByThreatPerCost(a, b, DKCostTypes.Death);
    }

    public static int compareByRP(AbilityDK_Base a, AbilityDK_Base b) {
        return compareByThreatPerCost(a, b, DKCostTypes.RunicPower);
    }
}
