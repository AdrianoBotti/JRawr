// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/Lookup.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.stats.HitResult;
import rawr.base.items.ItemInstance;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.talents.PaladinTalents;

public final class Lookup {
    private Lookup() {
    }

    public static float getEffectiveTargetArmor(int attackerLevel, float targetArmor,
            float armorIgnoreDebuffs, float armorIgnoreBuffs, float armorPenetrationRating) {
        float armorConstant = 400 + 85 * attackerLevel + 4.5f * 85 * (attackerLevel - 59);
        targetArmor *= (1f - armorIgnoreDebuffs) * (1f - armorIgnoreBuffs);
        float arPCap = Math.min((targetArmor + armorConstant) / 3f, targetArmor);
        float amount = StatConversion.getArmorPenetrationFromRating(armorPenetrationRating);
        targetArmor -= arPCap * Math.min(1.0f, amount);

        return targetArmor;
    }

    public static float getArmorPenetrationCap(int attackerLevel, float targetArmor,
            float armorIgnoreDebuffs, float armorIgnoreBuffs, float armorPenetrationRating) {
        float armorConstant = 400 + 85 * attackerLevel + 4.5f * 85 * (attackerLevel - 59);
        targetArmor *= (1f - armorIgnoreDebuffs) * (1f - armorIgnoreBuffs);
        float arPCap = Math.min((targetArmor + armorConstant) / 3f, targetArmor);

        return arPCap;
    }

    public static float targetArmorReduction(Character character, Stats stats, int targetArmor) {
        float damageReduction = StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor,
                stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating());
        return damageReduction;
    }

    public static float effectiveTargetArmorReduction(Character character, Stats stats, int targetArmor, int targetLevel) {
        float effectiveTargetArmor = getEffectiveTargetArmor(character.getLevel(), targetArmor, 0.0f, stats.getArmorPenetration(), stats.getArmorPenetrationRating());
        float damageReduction = StatConversion.getArmorDamageReduction(targetLevel, effectiveTargetArmor, 0f, 0f, 0f);

        return damageReduction;
    }

    public static float targetCritChance(Character character, Stats stats, int targetLevel) {
        return Math.max(0.0f, 0.05f - avoidanceChance(character, stats, HitResult.Crit, targetLevel));
    }

    public static float targetAvoidanceChance(Character character, Stats stats, HitResult avoidanceType, int targetLevel) {
        switch (avoidanceType) {
            case Miss: return StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80];
            case Dodge: return StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80];
            case Parry: return StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80];
            case Glance: return StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80];
            case Block: return StatConversion.WHITE_BLOCK_CHANCE_CAP[targetLevel - 80];
            case Resist:
                // Partial resists don't belong in the combat table, they are a damage multiplier (reduction)
                // The Chance to get any Partial Resist
                float partialChance = 1.0f - StatConversion.getResistanceTable(character.getLevel(), targetLevel, 0.0f, stats.getSpellPenetration())[0];
                return partialChance;
            default: return 0.0f;
        }
    }

    // Creature Type Damage Bonus from Crusade
    public static float creatureTypeDamageMultiplier(Character character, String targetType) {
        switch (targetType) {
            case "Humanoid": case "Demon": case "Elemental": return (1f + character.getPaladinTalents().getCrusade() * 0.01f);
            case "Undead": return (1f + character.getPaladinTalents().getCrusade() * 0.01f) * (1f + (character.getPaladinTalents().isGlyphOfSenseUndead() ? 0.01f : 0f));
            default: return 1f;
        }
    }

    public static float stanceDamageMultipler(Character character, Stats stats) {
        return (1.0f + stats.getBonusDamageMultiplier());
    }

    public static float stanceThreatMultipler(Character character, Stats stats) {
        return (1.42f * (1.0f + stats.getThreatIncreaseMultiplier()));
    }

    public static float stanceDamageReduction(Character character, Stats stats) {
        return stanceDamageReduction(character, stats, DamageType.Physical);
    }

    public static float stanceDamageReduction(Character character, Stats stats, DamageType damageType) {
        PaladinTalents talents = character.getPaladinTalents();

        float damageTaken = 1.0f * (1.0f + stats.getDamageTakenMultiplier());
        //Talents
        damageTaken *= (1f - talents.getImprovedRighteousFury() * 0.02f) * (1f - talents.getShieldOfTheTemplar() * 0.01f);
        if (talents.isGlyphOfDivinePlea()) { damageTaken *= (1f - 0.03f); }

        switch (damageType) {
            case Arcane:
            case Fire:
            case Frost:
            case Nature:
            case Shadow:
            case Holy:
                return damageTaken * (1.0f - talents.getGuardedByTheLight() * 0.03f);
            default:
                return damageTaken;
        }
    }

    public static float bonusArmorPenetrationPercentage(Character character, Stats stats) {
        return StatConversion.getArmorPenetrationFromRating(stats.getArmorPenetrationRating(), CharacterClass.Paladin);
    }

    public static float bonusExpertisePercentage(Character character, Stats stats) {
        return StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Paladin) + stats.getExpertise(), CharacterClass.Paladin);
    }

    public static float bonusPhysicalHastePercentage(Character character, Stats stats) {
        return StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.Paladin) + stats.getPhysicalHaste();
    }

    public static float bonusSpellHastePercentage(Character character, Stats stats) {
        return StatConversion.getSpellHasteFromRating(stats.getHasteRating(), CharacterClass.Paladin) + stats.getSpellHaste();
    }

    public static float hitChance(Character character, Stats stats, int targetLevel) {
        float physicalHit = StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Paladin) + stats.getPhysicalHit();
        return Math.min(1f, (1f - StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80]) + physicalHit);
    }

    public static float spellHitChance(Character character, Stats stats, int targetLevel) {
        float spellHit = StatConversion.getSpellHitFromRating(stats.getHitRating(), CharacterClass.Paladin) + stats.getSpellHit();

        int deltaLevel = targetLevel - character.getLevel();

        if (deltaLevel == 3) return Math.min(1f, 0.83f + spellHit); // 83% chance to hit
        else if (deltaLevel == 2) return Math.min(1f, 0.94f + spellHit); // 94% chance to hit
        else if (deltaLevel == 1) return Math.min(1f, 0.95f + spellHit); // 95% chance to hit
        else return Math.min(1f, 0.96f + spellHit); // 96% chance to hit
    }

    public static float critChance(Character character, Stats stats, int targetLevel) {
        return Math.max(0f, Math.min(1f, StatConversion.getCritFromRating(stats.getCritRating(), CharacterClass.Paladin)
                + StatConversion.getCritFromAgility(stats.getAgility(), CharacterClass.Paladin)
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80]
                + stats.getPhysicalCrit()));
    }

    public static float spellCritChance(Character character, Stats stats, int targetLevel) {
        float spellCrit = Math.min(1f, StatConversion.getSpellCritFromRating(stats.getCritRating(), CharacterClass.Paladin) + StatConversion.getSpellCritFromIntellect(stats.getIntellect(), CharacterClass.Paladin) + stats.getSpellCrit() + stats.getSpellCritOnTarget());

        return spellCrit * spellHitChance(character, stats, targetLevel);
    }

    public static float bonusCritPercentage(Character character, Stats stats, Ability ability, int targetLevel, String targetType) {
        // Grab base melee crit chance before adding ability-specific crit chance
        float abilityCritChance = critChance(character, stats, targetLevel);
        float spellCritChance = spellCritChance(character, stats, targetLevel);

        switch (ability) {
            case Consecration:
            case HolyShield:
            case HolyVengeance:
            case SealOfRighteousness:
            case RetributionAura:
            case RighteousDefense:
                abilityCritChance = 0.0f;// can't crit
                break;
            case None:
            case JudgementOfRighteousness:
            case JudgementOfVengeance:
            case AvengersShield:
            case HammerOfTheRighteous:
            case ShieldOfRighteousness:
            case HammerOfWrath:
                abilityCritChance *= 1.0f;// crit chance = melee
                break;
            case HolyWrath:
            case SealOfVengeance:
            case HandOfReckoning:
                abilityCritChance = spellCritChance;// crit chance = spell
                break;
            case Exorcism:
                if ("Undead".equals(targetType) || "Demon".equals(targetType)) {
                    // 100% chance the spell will crit, if it hits.
                    abilityCritChance = spellHitChance(character, stats, targetLevel);
                    break;
                }
                abilityCritChance = spellCritChance;// crit chance = spell
                break;
        }
        return Math.min(1.0f, abilityCritChance);
    }

    public static float weaponDamage(Character character, Stats stats, boolean normalized) {
        float weaponSpeed = 0.0f;
        float weaponMinDamage = 0.0f;
        float weaponMaxDamage = 0.0f;

        ItemInstance mainHand = character.get(rawr.base.character.CharacterSlot.MainHand);
        if (mainHand == null) // unarmed
        {
            weaponSpeed = 2.0f;
            weaponMinDamage = 1.0f;
            weaponMaxDamage = 2.0f;
        } else {
            weaponSpeed = mainHand.getSpeed();
            weaponMinDamage = mainHand.getMinDamage();
            weaponMaxDamage = mainHand.getMaxDamage();
        }
        // Non-Normalized Hits
        if (!normalized)
            return ((weaponMinDamage + weaponMaxDamage) / 2.0f + (weaponSpeed * stats.getAttackPower() / 14.0f));
            // Normalized Hits
            // Protection paladins currently do not have normalized instant attacks.
        else
            return ((weaponMinDamage + weaponMaxDamage) / 2.0f + (2.4f * stats.getAttackPower() / 14.0f));
    }

    public static float weaponSpeed(Character character, Stats stats) {
        ItemInstance mainHand = character.get(rawr.base.character.CharacterSlot.MainHand);
        if (mainHand != null)
            return Math.max(1.0f, mainHand.getSpeed() / (1.0f + bonusPhysicalHastePercentage(character, stats)));
        else
            return Math.max(1.0f, 2.0f / (1.0f + bonusPhysicalHastePercentage(character, stats)));
    }

    public static float glancingReduction(Character character, int targetLevel) {
        // The character is a melee class, lowEnd is element of [0.01, 0.91]
        float lowEnd = Math.max(0.01f, Math.min(0.91f, 1.3f - (0.05f * (float) (targetLevel - character.getLevel()) * 5.0f)));
        // The character is a melee class, highEnd is element of [0.20, 0.99]
        float highEnd = Math.max(0.20f, Math.min(0.99f, 1.2f - (0.03f * (float) (targetLevel - character.getLevel()) * 5.0f)));

        return (lowEnd + highEnd) / 2.0f;
    }

    public static float ardentDefenderReduction(Character character) {
        return character.getPaladinTalents().getArdentDefender() * 0.20f / 3;
    }

    public static float armorReduction(Character character, Stats stats, int targetLevel) // incoming damage
    {
        return Math.max(0.0f, Math.min(0.75f, stats.getArmor() / (stats.getArmor() + (467.5f * targetLevel - 22167.5f))));
    }

    public static float activeBlockReduction(Character character, Stats stats) {
        // This formula assumes judging on cd, and needs to be modified as soon as we support custom rotations.
        return (stats.getBlockValue() + stats.getHolyShieldBlockValue() + stats.getJudgementBlockValue() + stats.getShieldOfRighteousnessBlockValue());
    }

    public static float magicReduction(Character character, Stats stats, DamageType school, int targetLevel) {
        float damageReduction = Lookup.stanceDamageReduction(character, stats, school);
        float totalResist = 0.0f;
        float resistScale = 0.0f;

        switch (school) {
            case Arcane: totalResist += stats.getArcaneResistance(); break;
            case Fire: totalResist += stats.getFireResistance(); break;
            case Frost: totalResist += stats.getFrostResistance(); break;
            case Nature: totalResist += stats.getNatureResistance(); break;
            case Shadow: totalResist += stats.getShadowResistance(); break;
            default: break;
        }

        if ((targetLevel - character.getLevel()) == 0) resistScale = 400.0f;
        else if ((targetLevel - character.getLevel()) == 1) resistScale = 405.0f;
        else if ((targetLevel - character.getLevel()) == 2) resistScale = 410.0f;
        else
            // This number is still being tested by many and may be slightly higher
            // update: it seems 510 is a more realistic value
            // NB: resistScale is the resistance constant for 50% Mean Average Damage depending on Level difference.
            resistScale = 510.0f;

        return Math.max(0.0f, (1.0f - (totalResist / (resistScale + totalResist))) * damageReduction);
    }

    public static float avoidanceChance(Character character, Stats stats, HitResult avoidanceType, int targetLevel) {
        return StatConversion.getDRAvoidanceChance(character, stats, avoidanceType, targetLevel);
    }

    // Combination nCk
    public static float nComb(float n, float k) {
        float result = 1;

        for (float i = Math.max(k, n - k) + 1; i <= n; ++i)
            result *= i;

        for (float i = 2; i <= Math.min(k, n - k); ++i)
            result /= i;

        return result;
    }

    public static float getConsecrationTickChances(float ticks, float tickDamage, float miss) {
        // 10+0 ticks
        float[] consecrationTable = new float[11];// Debug Array
        float p = 1.0f - miss;
        int n = (int) Math.floor(ticks); // Number of possible ticks, backwards compatible to Ticks as time.
        int k;
        float damage = 0.0f;

        for (k = n; k > -1; k--) {   // The probability P(X=k) that k out of n possible ticks hit :
            float probabilityOfTicks = nComb(n, k) * (float) Math.pow(p, k) * (float) Math.pow((1 - p), (n - k));
            // The damage those ticks deal
            damage += tickDamage * probabilityOfTicks * k;

            consecrationTable[k] += probabilityOfTicks;// Debug Array
        }
        // Total average damage over all probabilities
        return damage;
    }

    public static boolean isAvoidable(Ability ability) {
        switch (ability) {
            case None:
            case HammerOfTheRighteous:
                //case Ability.SealOfCommand:
                //case Ability.SealOfBlood:
                return true;
            default:
                return false;
        }
    }

    public static boolean hasPartials(Ability ability) {
        switch (ability) {
            case ShieldOfRighteousness:
            case HammerOfTheRighteous:
            case SealOfVengeance:
            case HolyVengeance:
            case JudgementOfVengeance:
            case SealOfRighteousness:
            case JudgementOfRighteousness:
            case Exorcism:
            case HammerOfWrath:
            case AvengersShield:
            case HolyShield:
            case RetributionAura:
            case Consecration:
            case HolyWrath:
                return true;
            default:
                return false;
        }
    }

    public static boolean canCrit(Ability ability) {
        switch (ability) {
            case Consecration:
            case HolyShield:
            case HolyVengeance:
            case SealOfRighteousness:
            case RetributionAura:
            case RighteousDefense:
                return false;
            default:
                return true;
        }
    }

    public static boolean isSpell(Ability ability) {
        switch (ability) {
            case SealOfVengeance:
            case HolyVengeance:
            case Exorcism:
            case HolyWrath:
            case HolyShield:
            case RetributionAura:
            case Consecration:
            case HandOfReckoning:
            case RighteousDefense:
                //case Ability.SealOfRighteousness:
                return true;
            default:
                return false;
        }
    }

    public static String name(Ability ability) {
        switch (ability) {
            case None: return "Swing";
            case HolyWrath: return "Holy Wrath";
            case HandOfReckoning: return "Hand of Reckoning";
            case ShieldOfRighteousness: return "Shield of Righteousness";
            case HammerOfTheRighteous: return "Hammer of the Righteous";
            case SealOfVengeance: return "Seal of Vengeance";
            case HolyVengeance: return "Holy Vengeance";
            case JudgementOfVengeance: return "Judgement of Vengeance";
            case SealOfRighteousness: return "Seal of Righteousness";
            case JudgementOfRighteousness: return "Judgement of Righteousness";
            case Exorcism: return "Exorcism";
            case HammerOfWrath: return "Hammer of Wrath";
            case AvengersShield: return "Avenger's Shield";
            case HolyShield: return "Holy Shield";
            case RetributionAura: return "Retribution Aura";
            case Consecration: return "Consecration";
            default: return "";
        }
    }
}
