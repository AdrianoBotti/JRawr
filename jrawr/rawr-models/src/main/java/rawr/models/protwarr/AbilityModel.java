// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/AbilityModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;

public class AbilityModel {
    private Character character;
    private Stats stats;
    private WarriorTalents talents;
    private CalculationOptionsProtWarr options;

    public final AttackTable attackTable;

    private Ability ability;

    public Ability getAbility() {
        return ability;
    }

    private String name;

    public String getName() {
        return name;
    }

    private float damage;

    public float getDamage() {
        return damage;
    }

    private float threat;

    public float getThreat() {
        return threat;
    }

    private float damageMultiplier;

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    private float armorReduction;

    public float getArmorReduction() {
        return armorReduction;
    }

    public float getCritPercentage() {
        return attackTable.getCritical();
    }

    public float getHitPercentage() {
        return attackTable.getAnyHit();
    }

    private boolean isAvoidable;

    public boolean getIsAvoidable() {
        return isAvoidable;
    }

    private boolean isWeaponAttack;

    public boolean getIsWeaponAttack() {
        return isWeaponAttack;
    }

    private void calculateDamage() {
        float baseDamage = 0.0f;
        float critMultiplier = 1.0f + Lookup.bonusCritMultiplier(character, stats, ability);

        switch (ability) {
            // White Damage
            case None:
                baseDamage = Lookup.weaponDamage(character, stats, false);
                break;
            case Cleave:
                baseDamage = Lookup.weaponDamage(character, stats, false) + (222.0f * (1.0f + talents.getImprovedCleave() * 0.4f));
                break;
            case ConcussionBlow:
                baseDamage = stats.getAttackPower() * 0.38f;
                break;
            case DamageShield:
                baseDamage = stats.getBlockValue() * (talents.getDamageShield() * 0.1f);
                break;
            case DeepWounds:
                baseDamage = Lookup.weaponDamage(character, stats, false) * (talents.getDeepWounds() * 0.16f);
                damageMultiplier *= (1.0f + stats.getBonusBleedDamageMultiplier());
                armorReduction = 0.0f;
                break;
            case Devastate:
                // Assumes 5 stacks of Sunder Armor debuff
                baseDamage = (Lookup.weaponDamage(character, stats, true) + (202.0f * 5.0f)) * 1.2f;
                damageMultiplier *= (1.0f + stats.getBonusDevastateDamage());
                break;
            case HeroicStrike:
                baseDamage = Lookup.weaponDamage(character, stats, false) + 495.0f;
                break;
            case HeroicThrow:
                baseDamage = 12.0f + (stats.getAttackPower() * 0.5f);
                break;
            case MockingBlow:
                baseDamage = Lookup.weaponDamage(character, stats, true);
                if (talents.isGlyphOfMockingBlow())
                    damageMultiplier *= 1.25f;
                break;
            case Rend:
                baseDamage = 380.0f + Lookup.weaponDamage(character, stats, false);
                if (talents.isGlyphOfRending())
                    baseDamage *= 1.4f;
                damageMultiplier *= (1.0f + talents.getImprovedRend() * 0.2f) * (1.0f + stats.getBonusBleedDamageMultiplier());
                armorReduction = 0.0f;
                break;
            case Revenge:
                baseDamage = (1816.5f * (1.0f + talents.getImprovedRevenge() * 0.3f)) + (stats.getAttackPower() * 0.31f);
                damageMultiplier *= (1.0f + talents.getUnrelentingAssault() * 0.1f);
                break;
            case ShieldSlam:
                float softCap = 24.5f * character.getLevel();
                float hardCap = 39.5f * character.getLevel();
                if (stats.getBlockValue() < softCap)
                    baseDamage = 1015.0f + stats.getBlockValue();
                else
                    baseDamage = 1015.0f + softCap
                            + (0.98f * (Math.min(stats.getBlockValue(), hardCap) - softCap))
                            - (0.00073885f * (float) Math.pow((double) (Math.min(stats.getBlockValue(), hardCap) - softCap), 2.0d));
                damageMultiplier *= (1.0f + stats.getBonusShieldSlamDamage());
                break;
            case Shockwave:
                baseDamage = stats.getAttackPower() * 0.75f;
                damageMultiplier *= (1.0f + stats.getBonusShockwaveDamage());
                break;
            case Slam:
                baseDamage = Lookup.weaponDamage(character, stats, false) + 250.0f;
                break;
            case ThunderClap:
                baseDamage = 300.0f + (stats.getAttackPower() * 0.12f);
                damageMultiplier *= (1.0f + talents.getImprovedThunderClap() * 0.1f);
                break;
            case Vigilance:
                baseDamage = 0.0f;
                break;
        }

        // All damage multipliers
        baseDamage *= damageMultiplier;
        // Armor reduction
        baseDamage *= (1.0f - armorReduction);
        // Combat table adjustments
        baseDamage *=
                attackTable.getHit()
                        + attackTable.getCritical() * critMultiplier
                        + attackTable.getGlance() * Lookup.glancingReduction(character, options.getTargetLevel());

        damage = baseDamage;
    }

    private void calculateThreat() {
        // Base threat is always going to be the damage of the ability, if it is damaging
        float abilityThreat = damage;

        switch (ability) {
            case Cleave:
                abilityThreat += 225.0f;
                break;
            case ConcussionBlow:
                abilityThreat *= 2.0f;
                break;
            case Devastate:
                // Glyph of Devastate doubles bonus threat
                if (talents.isGlyphOfDevastate())
                    abilityThreat += (315 + (stats.getAttackPower() * 0.05f)) * 2.0f;
                else
                    abilityThreat += 315 + (stats.getAttackPower() * 0.05f);
                break;
            case HeroicStrike:
                abilityThreat += 259.0f;
                break;
            case HeroicThrow:
                abilityThreat *= 1.5f;
                break;
            case MockingBlow:
                if (talents.isGlyphOfBarbaricInsults())
                    abilityThreat *= 6.0f;
                else
                    abilityThreat *= 3.0f;
                break;
            case Revenge:
                abilityThreat += 121.0f;
                break;
            case ShieldBash:
                abilityThreat += 36.0f;
                break;
            case ShieldSlam:
                abilityThreat += 770.0f;
                abilityThreat *= 1.3f;
                break;
            case Slam:
                abilityThreat += 140.0f;
                break;
            case SunderArmor:
                abilityThreat += 345.0f + (stats.getAttackPower() * 0.05f);
                break;
            case ThunderClap:
                abilityThreat *= 1.85f;
                break;
            case Vigilance:
                if (talents.isGlyphOfVigilance())
                    abilityThreat = (options.getVigilanceValue() * 0.15f) * talents.getVigilance();
                else
                    abilityThreat = (options.getVigilanceValue() * 0.1f) * talents.getVigilance();
                break;
        }

        // All abilities other than Vigilance are affected by Defensive Stance
        if (ability != Ability.Vigilance)
            abilityThreat *= Lookup.stanceThreatMultipler(character, stats);

        threat = abilityThreat;
    }

    public AbilityModel(Character character, Stats stats, CalculationOptionsProtWarr options, Ability ability) {
        this.character = character;
        this.stats = stats;
        this.ability = ability;
        this.options = options;
        this.talents = character.getWarriorTalents();
        this.attackTable = new AttackTable(character, stats, options, ability);

        name = Lookup.name(ability);
        armorReduction = Lookup.targetArmorReduction(character, stats, options.getTargetArmor());
        damageMultiplier = Lookup.stanceDamageMultipler(character, stats);
        isAvoidable = Lookup.isAvoidable(ability);
        isWeaponAttack = Lookup.isWeaponAttack(ability);

        calculateDamage();
        calculateThreat();
    }
}
