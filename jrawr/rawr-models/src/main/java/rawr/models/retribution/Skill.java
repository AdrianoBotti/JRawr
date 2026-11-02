// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (the abstract Skill base; the subclasses live in their own files).
package rawr.models.retribution;

import rawr.base.talents.PaladinTalents;
import rawr.base.stats.Stats;
import rawr.models.mage.NetFormat;

public abstract class Skill {

    protected Stats stats;

    public Stats getStats() {
        return stats;
    }

    protected CalculationOptionsRetribution calcOpts;

    public CalculationOptionsRetribution getCalcOpts() {
        return calcOpts;
    }

    protected PaladinTalents talents;

    public PaladinTalents getTalents() {
        return talents;
    }

    protected CombatStats combats;

    public CombatStats getCombats() {
        return combats;
    }

    public void setCombats(CombatStats value) {
        combats = value;
        stats = value.getStats();
        calcOpts = value.getCalcOpts();
        talents = value.getTalents();
    }

    public AbilityType abilityType;
    public DamageType damageType;
    public boolean usesWeapon;
    public boolean righteousVengeance;

    public boolean isUsableBefore20PercentHealth() {
        return true;
    }

    public boolean isUsableAfter20PercentHealth() {
        return true;
    }

    public Ability getRotationAbility() {
        return null;
    }

    public Skill(CombatStats combats, AbilityType abilityType, DamageType damageType, boolean usesWeapon, boolean righteousVengeance) {
        setCombats(combats);
        this.abilityType = abilityType;
        this.damageType = damageType;
        this.usesWeapon = usesWeapon;
        this.righteousVengeance = righteousVengeance;
    }

    public float averageDamage() {
        return hitDamage() * ((1f - critChance()) + critChance() * critBonus()) * chanceToLand() * targets();
    }

    public float critChance() {
        if (abilityType == AbilityType.Spell) {
            return (float) Math.max(Math.min(1f, stats.getSpellCrit() + abilityCritChance()), 0);
        } else {
            return (float) Math.max(Math.min(1f, stats.getPhysicalCrit() + abilityCritChance()), 0);
        }
    }

    public float chanceToLand() {
        if (abilityType == AbilityType.Melee) {
            return 1 - (combats.getMeleeMissChance() + combats.getToBeDodgedChance() + combats.getToBeParriedChance() * calcOpts.getInFront());
        } else if (abilityType == AbilityType.Range) {
            return 1 - combats.getRangedMissChance();
        } else { // Spell
            return 1 - combats.getSpellMissChance();
        }
    }

    public float chanceToCrit() {
        return critChance() * chanceToLand();
    }

    public float critBonus() {
        float rightVen = 1;
        if (righteousVengeance) {
            if (stats.getRighteousVengeanceCanCrit() != 0) {
                rightVen += .1f * talents.getRighteousVengeance() * (1f + stats.getPhysicalCrit());
            } else {
                rightVen += .1f * talents.getRighteousVengeance();
            }
        }
        if (abilityType == AbilityType.Spell) {
            return 1.5f * (1f + stats.getBonusSpellCritMultiplier()) * rightVen;
        } else {
            return 2f * (1f + stats.getBonusCritMultiplier()) * rightVen;
        }
    }

    public float hitDamage() {
        float damage = abilityDamage();
        if (damageType == DamageType.Physical) {
            damage *= combats.armorReduction;
            damage *= (1f + stats.getBonusPhysicalDamageMultiplier());
        } else { // Holy Damage
            damage *= combats.partialResist;
            damage *= (1f + stats.getBonusHolyDamageMultiplier());
        }
        damage *= 1f + stats.getBonusDamageMultiplier();
        if (damageType != DamageType.Magic) damage *= 1f + .03f * talents.getVengeance();
        if (usesWeapon) damage *= 1f + .02f * talents.getTwoHandedWeaponSpecialization();
        damage *= (1f + .01f * talents.getCrusade());
        if (calcOpts.getMob() != MobType.Other) damage *= (1f + .01f * talents.getCrusade());
        damage *= combats.avengingWrathMulti;
        damage *= (talents.isGlyphOfSenseUndead() && calcOpts.getMob() == MobType.Undead ? 1.01f : 1f);
        return damage;
    }

    public abstract float abilityDamage();

    public float abilityCritChance() {
        return 0;
    }

    public float targets() {
        return 1f;
    }

    public float tickCount() {
        return 1;
    }

    @Override
    public String toString() {
        return "Average Damage: " + rawr.base.util.NetFormat.grouped(averageDamage(), 0)
                + "\nAverage Hit: " + rawr.base.util.NetFormat.grouped(hitDamage(), 0)
                + "\nCrit Chance: " + NetFormat.netDoubleToString(netRound(chanceToCrit() * 100, 2))
                + "%\nAvoid Chance: " + NetFormat.netDoubleToString(netRound((1f - chanceToLand()) * 100, 2)) + "%";
    }

    /** .NET Math.Round(double, digits) behaves as rescaled banker's rounding on these inputs. */
    private static double netRound(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }
}
