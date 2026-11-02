// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/Lookup.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.stats.HitResult;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public final class Lookup {
    private Lookup() {
    }

    public static float globalCooldownSpeed(Character character, boolean withLatency) {
        float globalCooldownSpeed = 1.5f;

        if (character.getWarriorTalents().getUnrelentingAssault() == 2)
            globalCooldownSpeed -= 0.5f;

        if (withLatency)
            globalCooldownSpeed += 0.1f;

        return globalCooldownSpeed;
    }

    public static float levelModifier(Character character, int targetLevel) {
        return (targetLevel - character.getLevel()) * 0.2f;
    }

    public static float targetArmorReduction(Character character, Stats stats, int targetArmor) {
        float ignoreArmor = 0.0f;
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        if (mainHand != null && (mainHand.getType() == ItemType.OneHandMace))
            ignoreArmor += character.getWarriorTalents().getMaceSpecialization() * 0.03f;

        return StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor, stats.getArmorPenetration(), ignoreArmor, stats.getArmorPenetrationRating());
    }

    public static float targetCritChance(Character character, Stats stats, int targetLevel) {
        return Math.max(0.0f, 0.05f - avoidanceChance(character, stats, HitResult.Crit, targetLevel));
    }

    public static float targetAvoidanceChance(Character character, Stats stats, HitResult avoidanceType, int targetLevel) {
        switch (avoidanceType) {
            case Miss:
                return StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80];
            case Dodge:
                return StatConversion.YELLOW_DODGE_CHANCE_CAP[targetLevel - 80] - (0.01f * character.getWarriorTalents().getWeaponMastery());
            case Parry:
                return StatConversion.YELLOW_PARRY_CHANCE_CAP[targetLevel - 80];
            case Glance:
                return 0.06f + ((targetLevel - character.getLevel()) * 0.06f);
            case Crit:
                return -StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - character.getLevel()]; // StatConversion returns as negative
            default:
                return 0.0f;
        }
    }

    public static float stanceDamageMultipler(Character character, Stats stats) {
        // In Defensive Stance
        return (0.95f * (1.0f + character.getWarriorTalents().getImprovedDefensiveStance() * 0.05f) * (1.0f + stats.getBonusDamageMultiplier()));
    }

    public static float stanceThreatMultipler(Character character, Stats stats) {
        // In Defensive Stance
        return (2.0735f * (1.0f + stats.getThreatIncreaseMultiplier()));
    }

    public static float stanceDamageReduction(Character character, Stats stats) {
        return stanceDamageReduction(character, stats, DamageType.Physical);
    }

    public static float stanceDamageReduction(Character character, Stats stats, DamageType damageType) {
        // In Defensive Stance
        float damageTaken = 0.9f * (1.0f + stats.getDamageTakenMultiplier());

        switch (damageType) {
            case Arcane:
            case Fire:
            case Frost:
            case Nature:
            case Shadow:
            case Holy:
                return damageTaken * (1.0f - character.getWarriorTalents().getImprovedDefensiveStance() * 0.03f);
            default:
                return damageTaken;
        }
    }

    public static float bonusArmorPenetrationPercentage(Character character, Stats stats) {
        return StatConversion.getArmorPenetrationFromRating(stats.getArmorPenetrationRating(), CharacterClass.Warrior);
    }

    public static float bonusExpertisePercentage(Character character, Stats stats) {
        return StatConversion.getDodgeParryReducFromExpertise(stats.getExpertise()
                + StatConversion.getExpertiseFromRating(stats.getExpertiseRating()), CharacterClass.Warrior);
    }

    public static float bonusHastePercentage(Character character, Stats stats) {
        return StatConversion.getPhysicalHasteFromRating(stats.getHasteRating(), CharacterClass.Warrior) + stats.getPhysicalHaste();
    }

    public static float bonusHitPercentage(Character character, Stats stats) {
        return StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Warrior) + stats.getPhysicalHit();
    }

    public static float bonusCritMultiplier(Character character, Stats stats, Ability ability) {
        if (ability == Ability.None)
            return (2.0f * (1.0f + stats.getBonusCritMultiplier()) - 1.0f);
        else
            return (2.0f * (1.0f + stats.getBonusCritMultiplier()) - 1.0f) * (1.0f + character.getWarriorTalents().getImpale() * 0.1f);
    }

    public static float bonusCritPercentage(Character character, Stats stats, int targetLevel) {
        return Math.max(0.0f, Math.min(1.0f,
                StatConversion.getPhysicalCritFromRating(stats.getCritRating(), CharacterClass.Warrior)
                        + StatConversion.getPhysicalCritFromAgility(stats.getAgility(), CharacterClass.Warrior)
                        + stats.getPhysicalCrit()));
    }

    public static float bonusCritPercentage(Character character, Stats stats, Ability ability, int targetLevel) {
        // Grab base melee crit chance before adding ability-specific crit chance
        float abilityCritChance = bonusCritPercentage(character, stats, targetLevel);

        switch (ability) {
            case Cleave:
                abilityCritChance += character.getWarriorTalents().getIncite() * 0.05f;
                break;
            case Devastate:
                abilityCritChance += stats.getDevastateCritIncrease();
                break;
            case HeroicStrike:
                abilityCritChance += character.getWarriorTalents().getIncite() * 0.05f;
                break;
            case ShieldSlam:
                abilityCritChance += character.getWarriorTalents().getCriticalBlock() * 0.05f;
                break;
            case ThunderClap:
                abilityCritChance += character.getWarriorTalents().getIncite() * 0.05f;
                break;
            case DamageShield:
            case DeepWounds:
            case Rend:
            case ShieldBash:
            case SunderArmor:
                abilityCritChance = 0.0f;
                break;
        }

        return Math.min(1.0f, abilityCritChance);
    }

    public static float weaponDamage(Character character, Stats stats, boolean normalized) {
        float weaponDamage = 1.0f;

        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        if (mainHand != null) {
            float weaponSpeed = mainHand.getSpeed();
            float weaponMinDamage = mainHand.getMinDamage();
            float weaponMaxDamage = mainHand.getMaxDamage();
            float normalizedSpeed = 1.0f;
            if (mainHand.getType() == ItemType.Dagger)
                normalizedSpeed = 1.7f;
            else
                normalizedSpeed = 2.4f;

            // Non-Normalized Hits
            if (!normalized)
                weaponDamage = ((weaponMinDamage + weaponMaxDamage) / 2.0f + (weaponSpeed * stats.getAttackPower() / 14.0f)) + stats.getWeaponDamage();
                // Normalized Hits
            else
                weaponDamage = ((weaponMinDamage + weaponMaxDamage) / 2.0f + (normalizedSpeed * stats.getAttackPower() / 14.0f)) + stats.getWeaponDamage();
        }

        return weaponDamage;
    }

    public static float weaponSpeed(Character character, Stats stats) {
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        if (mainHand != null)
            return Math.max(1.0f, mainHand.getSpeed() / (1.0f + bonusHastePercentage(character, stats)));
        else
            return 1.0f;
    }

    public static float glancingReduction(Character character, int targetLevel) {
        return (Math.min(0.91f, 1.3f - (0.05f * (targetLevel - character.getLevel()) * 5.0f))
                + Math.max(0.99f, 1.2f - (0.03f * (targetLevel - character.getLevel()) * 5.0f))) / 2;
    }

    public static float armorReduction(Character character, Stats stats, int targetLevel) {
        return StatConversion.getArmorDamageReduction(targetLevel, stats.getArmor(), 0.0f, 0.0f, 0.0f);
    }

    public static float magicReduction(Character character, Stats stats, DamageType school, int targetLevel) {
        float totalResist = 0.0f;
        switch (school) {
            case Arcane:
                totalResist = stats.getArcaneResistance();
                break;
            case Fire:
                totalResist = stats.getFireResistance();
                break;
            case Frost:
                totalResist = stats.getFrostResistance();
                break;
            case Nature:
                totalResist = stats.getNatureResistance();
                break;
            case Shadow:
                totalResist = stats.getShadowResistance();
                break;
        }

        float damageReduction = Lookup.stanceDamageReduction(character, stats, school);
        float averageResistance = StatConversion.getAverageResistance(targetLevel, character.getLevel(), totalResist, 0.0f);

        return Math.max(0.0f, (1.0f - averageResistance) * damageReduction);
    }

    public static float avoidanceChance(Character character, Stats stats, HitResult avoidanceType, int targetLevel) {
        if (avoidanceType == HitResult.Crit)
            return StatConversion.getDRAvoidanceChance(character, stats, avoidanceType, targetLevel);
        else
            return Math.max(0.0f, StatConversion.getDRAvoidanceChance(character, stats, avoidanceType, targetLevel));
    }

    public static boolean isAvoidable(Ability ability) {
        switch (ability) {
            case DamageShield:
            case DeepWounds:
            case Shockwave:
            case ThunderClap:
                return false;
            default:
                return true;
        }
    }

    public static boolean isWeaponAttack(Ability ability) {
        switch (ability) {
            case None:
            case Cleave:
            case ConcussionBlow:
            case Devastate:
            case HeroicStrike:
            case MockingBlow:
            case Revenge:
            case ShieldSlam:
            case Shockwave:
            case Slam:
                return true;
            default:
                return false;
        }
    }

    public static String name(Ability ability) {
        switch (ability) {
            case None: return "Swing";
            case Cleave: return "Cleave";
            case ConcussionBlow: return "Concussion Blow";
            case DamageShield: return "Damage Shield";
            case DeepWounds: return "Deep Wounds";
            case Devastate: return "Devastate";
            case HeroicStrike: return "Heroic Strike";
            case HeroicThrow: return "Heroic Throw";
            case MockingBlow: return "Mocking Blow";
            case Rend: return "Rend";
            case Revenge: return "Revenge";
            case ShieldBash: return "Shield Bash";
            case ShieldSlam: return "Shield Slam";
            case Shockwave: return "Shockwave";
            case Slam: return "Slam";
            case SunderArmor: return "Sunder Armor";
            case ThunderClap: return "Thunder Clap";
            default: return "";
        }
    }
}
