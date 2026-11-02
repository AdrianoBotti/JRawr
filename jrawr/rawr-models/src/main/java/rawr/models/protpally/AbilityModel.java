// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/AbilityModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class AbilityModel {
    private Ability ability;
    private Character character;
    private Stats stats;
    private PaladinTalents talents;
    private CalculationOptionsProtPaladin calcOpts;

    public final AttackTable attackTable;

    private String name;
    private float damage;
    private float threat;
    private float damageMultiplier;
    private float armorReduction;

    public String getName() { return name; }
    public float getDamage() { return damage; }
    public float getThreat() { return threat; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public float getArmorReduction() { return armorReduction; }
    public float getCritPercentage() { return attackTable.getCritical(); }

    private void calculateDamage() {
        float baseDamage = 0.0f;
        float critMultiplier = 0.0f;
        float duration = 0.0f;
        float AP = stats.getAttackPower();
        float SP = stats.getSpellPower();

        int targetLevel = calcOpts.getTargetLevel();

        // region Ability Base Damage
        switch (ability) {
            // White Damage
            case None:
                baseDamage = stats.getWeaponDamage();

                damageMultiplier *= (1.0f + stats.getBonusPhysicalDamageMultiplier())
                        * (1.0f - (Lookup.glancingReduction(character, targetLevel) * attackTable.getGlance()))
                        * (1.0f - armorReduction);

                critMultiplier = 1.0f;
                break;
            case ShieldOfRighteousness:
                float blockValue = stats.getBlockValue() + stats.getShieldOfRighteousnessBlockValue() + stats.getJudgementBlockValue() + stats.getHolyShieldBlockValue();

                float blockValueDRStart = 30 * character.getLevel();

                if (blockValue < blockValueDRStart) {
                    baseDamage = blockValue;
                } else if (blockValue < 39.5 * character.getLevel()) {
                    baseDamage = blockValueDRStart + (0.95f * (blockValue - blockValueDRStart)) - (0.000625f * (float) Math.pow(blockValue - blockValueDRStart, 2));
                } else {
                    baseDamage = blockValueDRStart + (0.95f * 9.5f * character.getLevel()) - (0.000625f * (float) Math.pow(9.5 * character.getLevel(), 2));
                }

                baseDamage += 520f
                        + stats.getBonusShieldOfRighteousnessDamage();

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier());
                critMultiplier = 1.0f;
                break;
            case HammerOfTheRighteous:
                if (talents.getHammerOfTheRighteous() == 0 || character.get(rawr.base.character.CharacterSlot.MainHand) == null) {
                    damage = 0.0f;
                    return;
                }

                baseDamage = (stats.getWeaponDamage() / character.get(rawr.base.character.CharacterSlot.MainHand).getSpeed()) * 4.0f;

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + stats.getBonusHammerOfTheRighteousMultiplier());

                critMultiplier = 1.0f;
                break;
            // Seal of Vengeance is the tiny damage that applies on each swing; Holy Vengeance is the DoT
            // While trivial threat and damage, it's modeled for compatibility with Seal of Righteousness
            case SealOfVengeance:
                baseDamage = stats.getWeaponDamage() * 0.33f;

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + 0.03f * talents.getSealsOfThePure())
                        * (1.0f + stats.getBonusSealOfVengeanceDamageMultiplier());

                critMultiplier = 1.0f;
                break;
            // 5 stacks of Holy Vengeance are assumed
            // TODO: implement stacking mechanic for beginning-of-fight TPS
            case HolyVengeance:
                baseDamage = 5f * (0.016f * SP + 0.032f * AP);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + 0.03f * talents.getSealsOfThePure())
                        * (1.0f + stats.getBonusSealOfVengeanceDamageMultiplier());

                critMultiplier = 0.0f;
                break;
            // Judgement of Vengeance assumes 5 stacks of Holy Vengeance
            case JudgementOfVengeance:
                float holyVengeanceStacks = 5;

                baseDamage = (0.22f * SP + 0.14f * AP) * (1.0f + 0.1f * holyVengeanceStacks);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + 0.03f * talents.getSealsOfThePure())
                        * (talents.isGlyphOfJudgement() ? 1.1f : 1.0f);

                critMultiplier = 1.0f;
                break;
            case SealOfRighteousness:
                baseDamage = Lookup.weaponSpeed(character, stats) * ((0.022f * AP) + (0.044f * SP));

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + 0.03f * talents.getSealsOfThePure())
                        * (1.0f + stats.getBonusSealOfRighteousnessDamageMultiplier())
                        * (talents.isGlyphOfSealOfRighteousness() ? 1.1f : 1.0f);

                critMultiplier = 0.0f;
                break;
            case JudgementOfRighteousness:
                baseDamage = 1.0f + (0.2f * AP) + (0.32f * SP);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + 0.03f * talents.getSealsOfThePure())
                        * (talents.isGlyphOfJudgement() ? 1.1f : 1.0f);

                critMultiplier = 1.0f;
                break;
            case HolyShield:
                if (talents.getHolyShield() == 0) {
                    damage = 0.0f;
                    return;
                }

                baseDamage = (211f + (0.056f * AP) + (0.09f * SP)) * 1.3f;
                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier());
                critMultiplier = 0.0f;
                break;
            case Consecration:
                baseDamage = 113f + (0.04f * (SP + stats.getConsecrationSpellPower())) + (0.04f * AP);
                duration = (talents.isGlyphOfConsecration() ? 10.0f : 8.0f);
                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier());
                critMultiplier = 0.0f;
                break;
            case Exorcism:
                baseDamage = 1087f + (0.15f * SP) + (0.15f * AP);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + talents.getSanctityOfBattle() * 0.05f)
                        * (talents.isGlyphOfExorcism() ? 1.2f : 1.0f);

                critMultiplier = 0.5f;
                break;
            case AvengersShield:
                baseDamage = 1222f + (0.07f * SP) + (0.07f * AP);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (talents.isGlyphOfAvengersShield() ? 2.0f : 1.0f);

                critMultiplier = 1.0f;
                break;
            case HolyWrath:
                baseDamage = 1142f + (AP * 0.07f) + (SP * 0.07f);
                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier());
                critMultiplier = 0.5f;
                break;
            case HammerOfWrath:
                baseDamage = 1198f + (AP * 0.15f) + (SP * 0.15f);
                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier());
                critMultiplier = 1.0f;
                break;
            case RetributionAura:
                baseDamage = 112f + (SP * 0.0666f);

                damageMultiplier *= (1.0f + stats.getBonusHolyDamageMultiplier())
                        * (1.0f + talents.getSanctifiedRetribution() * 0.5f);

                critMultiplier = 0.0f;
                break;
            default:
                break;
        }
        // endregion

        // All damage multipliers, 1HWS, Armor etc...do we need to split buff/debuff ?
        baseDamage *= damageMultiplier;

        // region Miss Chance, Avoidance Chance
        if (Lookup.isSpell(ability)) {
            if (ability == Ability.Consecration)
                // Probability calculation, since each tick can be resisted individually.
                baseDamage = Lookup.getConsecrationTickChances(duration, baseDamage, attackTable.getMiss());
            else
                // Missed spell attacks
                // TODO: expand Ability Model to include a check for damage type, not only spell.
                baseDamage *= (1.0f - attackTable.getMiss());
        } else {
            // Missed attacks
            if (Lookup.isAvoidable(ability))
                baseDamage *= (1.0f - attackTable.anyMiss());
            else
                baseDamage *= (1.0f - attackTable.getMiss());
        }
        // endregion

        // region Partial Resists
        // Partial Resists
        if (Lookup.hasPartials(ability)) {
            // Detailed table of Partial slices.
            float[] partialChanceTable = StatConversion.getResistanceTable(character.getLevel(), targetLevel, 0.0f, stats.getSpellPenetration());

            // Here goes nothing, Damage averaged over the different partial slices that can happen.
            float partialDamage = 0.0f;

            for (int i = 0; i < 11; i++)
                partialDamage += partialChanceTable[i] * (1.0f - 0.1f * (float) i) * baseDamage;

            baseDamage = partialDamage;
        }
        // endregion

        // Average critical strike bonuses
        if (Lookup.canCrit(ability))
            baseDamage += baseDamage * critMultiplier * attackTable.getCritical();

        // Final Damage the Ability deals
        damage = baseDamage;
    }

    private void calculateThreat() {
        // Base threat is always going to be the damage of the ability, if it is damaging
        float abilityThreat = damage;
        float holyThreatModifier = 1.8f;

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
                abilityThreat *= holyThreatModifier;
                break;
            default:
                break;
        }

        abilityThreat *= Lookup.stanceThreatMultipler(character, stats);

        threat = abilityThreat;
    }

    public AbilityModel(Character character, Stats stats, Ability ability, CalculationOptionsProtPaladin calcOpts) {
        this.character = character;
        this.stats = stats;
        this.ability = ability;
        this.calcOpts = calcOpts;

        talents = character.getPaladinTalents();
        attackTable = new AttackTable(character, stats, ability, calcOpts);

        if (!Lookup.isSpell(ability))
            armorReduction = Lookup.effectiveTargetArmorReduction(character, stats, calcOpts.getTargetArmor(), calcOpts.getTargetLevel());

        name = Lookup.name(ability);
        damageMultiplier = Lookup.stanceDamageMultipler(character, stats);
        damageMultiplier *= Lookup.creatureTypeDamageMultiplier(character, calcOpts.getTargetType());

        calculateDamage();
        calculateThreat();
    }
}
