// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the Silverlight/RAWR3 PetTalentsBase/PetTalents/PetTalentDataAttribute path and the
// PetTalent icon loading (ItemIconsPet, UI) are omitted; only the desktop PetTalentTreeData path is
// ported.
package rawr.models.hunter;

import java.util.ArrayList;
import java.util.List;

class PetTalentData {
    /** A Pet Talent */
    public PetTalentData() {
        id = -1;
        name = "Invalid";
        max = 1;
        desc = new String[] { "" };
        icon = "";
    }

    /** A Pet Talent */
    public PetTalentData(int id, String name, int max, String[] desc, String icon) {
        this.id = id;
        this.name = name;
        this.max = max;
        this.desc = desc;
        this.icon = icon;
    }

    /** The Unique ID of this Talent */
    public int id;
    /** The Name of the Talent */
    public String name;
    /** The Maximum Number of Ranks of the Talent */
    public int max;
    /**
     * List of Descriptions, each point in the array is
     * the description of the matching Rank number.
     */
    public String[] desc;
    /** The Icon */
    public String icon;

    @Override
    public String toString() {
        try {
            return name + " [" + max + "]\r\n" + desc[0];
        } catch (Exception e) {
            return "Failed to convert PetTalentData to string.";
        }
    }
}

class PetTalent extends PetTalentData {
    /** A Pet Talent */
    public PetTalent() {
        value = 0;
    }

    /** A Pet Talent */
    public PetTalent(PetTalentData base, int value) {
        id = base.id;
        name = base.name;
        max = base.max;
        desc = base.desc;
        icon = base.icon;

        this.value = value;
    }

    /** A Pet Talent */
    public PetTalent(int id, String name, int value, int max, String[] desc, String icon) {
        this.id = id;
        this.name = name;
        this.max = max;
        this.desc = desc;
        this.icon = icon;

        this.value = value;
    }

    /** The Current Rank of the Talent */
    public int value;

    @Override
    public String toString() {
        try {
            return name + " [" + value + "/" + max + "]\r\n" + desc[0];
        } catch (Exception e) {
            return "Failed to convert PetTalent to string.";
        }
    }
}

class PetTalentsBaseStatic {
    /// <summary>Increases your pet's total Stamina by [2*Pts]% and increases all healing effects on your pet by [20*Pts]%.</summary>
    public static final PetTalentData BloodOfTheRhino = new PetTalentData(0, "Blood Of The Rhino", 2, new String[] {
            "Increases your pet's total Stamina by [2*Pts]% and increases all healing effects on your pet by [20*Pts]%.",
            "Increases your pet's total Stamina by 2% and increases all healing effects on your pet by 20%.",
            "Increases your pet's total Stamina by 4% and increases all healing effects on your pet by 40%." }, "spell_shadow_lifedrain");
    /// <summary>Your pet's attacks have a [10*Pts]% chance to increase its happiness by 5% and heal 5% of its total health.</summary>
    public static final PetTalentData Bloodthirsty = new PetTalentData(1, "Bloodthirsty", 2, new String[] {
            "Your pet's attacks have a [10*Pts]% chance to increase its happiness by 5% and heal 5% of its total health.",
            "Your pet's attacks have a 10% chance to increase its happiness by 5% and heal 5% of its total health.",
            "Your pet's attacks have a 20% chance to increase its happiness by 5% and heal 5% of its total health." }, "ability_druid_primaltenacity");
    /// <summary>Increases your pet's movement speed by 30%.</summary>
    public static final PetTalentData BoarsSpeed = new PetTalentData(2, "Boar's Speed", 1, new String[] {
            "Increases your pet's movement speed by 30%.",
            "Increases your pet's movement speed by 30%." }, "ability_hunter_pet_boar");
    /// <summary>Removes all movement impairing effects and all effects which cause loss of control of your pet, and reduces damage done to your pet by 20% for 12 sec.</summary>
    public static final PetTalentData Bullheaded = new PetTalentData(3, "Bullheaded", 1, new String[] {
            "Removes all movement impairing effects and all effects which cause loss of control of your pet, and reduces damage done to your pet by 20% for 12 sec.",
            "Removes all movement impairing effects and all effects which cause loss of control of your pet, and reduces damage done to your pet by 20% for 12 sec." }, "ability_warrior_bullrush");
    /// <summary>Your pet roars, increasing your pet's and your melee and ranged attack power by 10%. Lasts 20 sec. 5 min cooldown.</summary>
    public static final PetTalentData CallOfTheWild = new PetTalentData(4, "Call Of The Wild", 1, new String[] {
            "Your pet roars, increasing your pet's and your melee and ranged attack power by 10%. Lasts 20 sec. 5 min cooldown.",
            "Your pet roars, increasing your pet's and your melee and ranged attack power by 10%. Lasts 20 sec. 5 min cooldown." }, "ability_druid_kingofthejungle");
    /// <summary>Your pet can generate health and happiness by eating a corpse. Will not work on the remains of elemental or mechanical creatures.</summary>
    public static final PetTalentData CarrionFeeder = new PetTalentData(5, "Carrion Feeder", 1, new String[] {
            "Your pet can generate health and happiness by eating a corpse. Will not work on the remains of elemental or mechanical creatures.",
            "Your pet can generate health and happiness by eating a corpse. Will not work on the remains of elemental or mechanical creatures." }, "ability_racial_cannibalize");
    /// <summary>Your pet charges an enemy, immobilizing the target for 1 sec, and increasing the pet's melee attack power by 25% for its next attack.</summary>
    public static final PetTalentData ChargeSwoop = new PetTalentData(6, "Charge/Swoop", 1, new String[] {
            "Your pet charges an enemy, immobilizing the target for 1 sec, and increasing the pet's melee attack power by 25% for its next attack.",
            "Your pet charges an enemy, immobilizing the target for 1 sec, and increasing the pet's melee attack power by 25% for its next attack." }, "ability_hunter_pet_bear");
    /// <summary>Increases your pet's attack speed by [15*Pts]%. Your pet will hit faster but each hit will do less damage.</summary>
    public static final PetTalentData CobraReflexes = new PetTalentData(7, "Cobra Reflexes", 2, new String[] {
            "Increases your pet's attack speed by [15*Pts]%. Your pet will hit faster but each hit will do less damage.",
            "Increases your pet's attack speed by 15%. Your pet will hit faster but each hit will do less damage.",
            "Increases your pet's attack speed by 30%. Your pet will hit faster but each hit will do less damage." }, "spell_nature_guardianward");
    /// <summary>When at less than 35% health, your pet does [25*Pts]% more damage and has a [30*Pts]% reduced chance to eb critically hit.</summary>
    public static final PetTalentData Cornered = new PetTalentData(8, "Cornered", 2, new String[] {
            "When at less than 35% health, your pet does [25*Pts]% more damage and has a [30*Pts]% reduced chance to be critically hit.",
            "When at less than 35% health, your pet does 25% more damage and has a 30% reduced chance to be critically hit.",
            "When at less than 35% health, your pet does 50% more damage and has a 60% reduced chance to be critically hit." }, "ability_hunter_survivalinstincts");
    /// <summary>Increases pet and hunter damage by [1*Pts]% for 10 seconds each time the pet deals a critical strike with Claw, Bite, or Smack.</summary>
    public static final PetTalentData CullingTheHerd = new PetTalentData(9, "Culling The Herd", 3, new String[] {
            "Increases pet and hunter damage by [1*Pts]% for 10 seconds each time the pet deals a critical strike with Claw, Bite, or Smack.",
            "Increases pet and hunter damage by 1% for 10 seconds each time the pet deals a critical strike with Claw, Bite, or Smack.",
            "Increases pet and hunter damage by 2% for 10 seconds each time the pet deals a critical strike with Claw, Bite, or Smack.",
            "Increases pet and hunter damage by 3% for 10 seconds each time the pet deals a critical strike with Claw, Bite, or Smack." }, "inv_misc_monsterhorn_06");
    /// <summary>Increases your pet's movement speed by 80% for 16 sec.</summary>
    public static final PetTalentData DiveDash = new PetTalentData(10, "Dive/Dash", 1, new String[] {
            "Increases your pet's movement speed by 80% for 16 sec.",
            "Increases your pet's movement speed by 80% for 16 sec." }, "spell_shadow_burningspirit");
    /// <summary>Your pet does [8*Pts]% additional damage to targets with less than 35% health.</summary>
    public static final PetTalentData FeedingFrenzy = new PetTalentData(11, "Feeding Frenzy", 2, new String[] {
            "Your pet does [8*Pts]% additional damage to targets with less than 35% health.",
            "Your pet does 8% additional damage to targets with less than 35% health.",
            "Your pet does 16% additional damage to targets with less than 35% health." }, "inv_misc_fish_48");
    /// <summary>Reduces the chance your pet will be critically hit by melee attacks by [2*Pts]%.</summary>
    public static final PetTalentData GraceOfTheMantis = new PetTalentData(12, "Grace Of The Mantis", 2, new String[] {
            "Reduces the chance your pet will be critically hit by melee attacks by [2*Pts]%.",
            "Reduces the chance your pet will be critically hit by melee attacks by 2%.",
            "Reduces the chance your pet will be critically hit by melee attacks by 4%." }, "inv_misc_ahnqirajtrinket_02");
    /// <summary>Your pet takes [5*Pts]% less damage from Arcane, Fire, Frost, Nature and Shadow magic.</summary>
    public static final PetTalentData GreatResistance = new PetTalentData(13, "Great Resistance", 3, new String[] {
            "Your pet takes [5*Pts]% less damage from Arcane, Fire, Frost, Nature and Shadow magic.",
            "Your pet takes 5% less damage from Arcane, Fire, Frost, Nature and Shadow magic.",
            "Your pet takes 10% less damage from Arcane, Fire, Frost, Nature and Shadow magic.",
            "Your pet takes 15% less damage from Arcane, Fire, Frost, Nature and Shadow magic." }, "spell_nature_resistnature");
    /// <summary>Increases your pet's total Stamina by [4*Pts]%.</summary>
    public static final PetTalentData GreatStamina = new PetTalentData(14, "Great Stamina", 3, new String[] {
            "Increases your pet's total Stamina by [4*Pts]%.",
            "Increases your pet's total Stamina by 4%.",
            "Increases your pet's total Stamina by 8%.",
            "Increases your pet's total Stamina by 12%." }, "spell_nature_unyeildingstamina");
    /// <summary>Your pet's Growl generates [10*Pts]% additional threat and 10% of it's total happiness.</summary>
    public static final PetTalentData GuardDog = new PetTalentData(15, "Guard Dog", 2, new String[] {
            "Your pet's Growl generates [10*Pts]% additional threat and 10% of it's total happiness.",
            "Your pet's Growl generates 10% additional threat and 10% of it's total happiness.",
            "Your pet's Growl generates 20% additional threat and 10% of it's total happiness." }, "ability_physical_taunt");
    /// <summary>When your pet dies, it will miraculously return to life with full health.</summary>
    public static final PetTalentData HeartOfThePhoenix = new PetTalentData(16, "Heart Of The Phoenix", 1, new String[] {
            "When your pet dies, it will miraculously return to life with full health.",
            "When your pet dies, it will miraculously return to life with full health." }, "inv_misc_pheonixpet_01");
    /// <summary>Your pet's Cower also decreases damage taken by [10*Pts]% for the next 10 sec.</summary>
    public static final PetTalentData ImprovedCower = new PetTalentData(17, "Improved Cower", 2, new String[] {
            "Your pet's Cower also decreases damage taken by [10*Pts]% for the next 10 sec.",
            "Your pet's Cower also decreases damage taken by 10% for the next 10 sec.",
            "Your pet's Cower also decreases damage taken by 20% for the next 10 sec." }, "ability_druid_cower");
    /// <summary>Your pet runs at high speed towards a group member, intercepting the next melee or ranged attack made against them.</summary>
    public static final PetTalentData Intervene = new PetTalentData(18, "Intervene", 1, new String[] {
            "Your pet runs at high speed towards a group member, intercepting the next melee or ranged attack made against them.",
            "Your pet runs at high speed towards a group member, intercepting the next melee or ranged attack made against them." }, "ability_hunter_pet_turtle");
    /// <summary>Your pet temporarily gains 30% of it's maximum health for 20 sec. After the effect expires, the health is lost.</summary>
    public static final PetTalentData LastStand = new PetTalentData(19, "Last Stand", 1, new String[] {
            "Your pet temporarily gains 30% of it's maximum health for 20 sec. After the effect expires, the health is lost.",
            "Your pet temporarily gains 30% of it's maximum health for 20 sec. After the effect expires, the health is lost." }, "spell_nature_shamanrage");
    /// <summary>Your pet heals itself for 100% of its total health over 5 sec while channeling.</summary>
    public static final PetTalentData LickYourWounds = new PetTalentData(20, "Lick Your Wounds", 1, new String[] {
            "Your pet heals itself for 100% of its total health over 5 sec while channeling.",
            "Your pet heals itself for 100% of its total health over 5 sec while channeling." }, "ability_hunter_mendpet");
    /// <summary>Reduces the duration of all Stun and Fear effects used against your pet by [15*Pts]%.</summary>
    public static final PetTalentData Lionhearted = new PetTalentData(21, "Lionhearted", 2, new String[] {
            "Reduces the duration of all Stun and Fear effects used against your pet by [15*Pts]%.",
            "Reduces the duration of all Stun and Fear effects used against your pet by 15%.",
            "Reduces the duration of all Stun and Fear effects used against your pet by 30%." }, "inv_bannerpvp_02");
    /// <summary>Reduces the cooldown on your pet's Dive/Dash ability by [8*Pts] sec.</summary>
    public static final PetTalentData Mobility = new PetTalentData(22, "Mobility", 2, new String[] {
            "Reduces the cooldown on your pet's Dive/Dash ability by [8*Pts] sec.",
            "Reduces the cooldown on your pet's Dive/Dash ability by 8 sec.",
            "Reduces the cooldown on your pet's Dive/Dash ability by 16 sec." }, "ability_hunter_animalhandler");
    /// <summary>Increases your pet's armor by [5*Pts]%.</summary>
    public static final PetTalentData NaturalArmor = new PetTalentData(23, "Natural Armor", 2, new String[] {
            "Increases your pet's armor by [5*Pts]%.",
            "Increases your pet's armor by 5%.",
            "Increases your pet's armor by 10%." }, "spell_nature_spiritarmor");
    /// <summary>Your pet has a [15*Pts]% chance after using an ability that the next ability will cost no Focus if used within 8 sec.</summary>
    public static final PetTalentData OwlsFocus = new PetTalentData(24, "Owl's Focus", 2, new String[] {
            "Your pet has a [15*Pts]% chance after using an ability that the next ability will cost no Focus if used within 8 sec.",
            "Your pet has a 15% chance after using an ability that the next ability will cost no Focus if used within 8 sec.",
            "Your pet has a 30% chance after using an ability that the next ability will cost no Focus if used within 8 sec." }, "ability_hunter_pet_owl");
    /// <summary>Increases your pet's armor by [5*Pts]% and chance to Dodge by [1*Pts]%.</summary>
    public static final PetTalentData PetBarding = new PetTalentData(25, "Pet Barding", 2, new String[] {
            "Increases your pet's armor by [5*Pts]% and chance to Dodge by [1*Pts]%.",
            "Increases your pet's armor by 5% and chance to Dodge by 1%.",
            "Increases your pet's armor by 10% and chance to Dodge by 2%." }, "inv_helmet_94");
    /// <summary>Your pet goes into a killing frenzy. Successful attacks have a chance to increase attack power by 5%. This effect will stack up to 5 times. Lasts 20 sec.</summary>
    public static final PetTalentData Rabid = new PetTalentData(26, "Rabid", 1, new String[] {
            "Your pet goes into a killing frenzy. Successful attacks have a chance to increase attack power by 5%. This effect will stack up to 5 times. Lasts 20 sec.",
            "Your pet goes into a killing frenzy. Successful attacks have a chance to increase attack power by 5%. This effect will stack up to 5 times. Lasts 20 sec." }, "ability_druid_berserk");
    /// <summary>Your pet's inspiring roar restores 30% of your total mana over 9 sec.</summary>
    public static final PetTalentData RoarOfRecovery = new PetTalentData(27, "Roar Of Recovery", 1, new String[] {
            "Your pet's inspiring roar restores 30% of your total mana over 9 sec.",
            "Your pet's inspiring roar restores 30% of your total mana over 9 sec." }, "ability_druid_mastershapeshifter");
    /// <summary>Protects a friendly target from critical strikes, making attacks against that target unable to be critical strikes, but 20% of all damage taken by that target is also taken by the pet. Lasts 12 sec.</summary>
    public static final PetTalentData RoarOfSacrifice = new PetTalentData(28, "Roar Of Sacrifice", 1, new String[] {
            "Protects a friendly target from critical strikes, making attacks against that target unable to be critical strikes, but 20% of all damage taken by that target is also taken by the pet. Lasts 12 sec.",
            "Protects a friendly target from critical strikes, making attacks against that target unable to be critical strikes, but 20% of all damage taken by that target is also taken by the pet. Lasts 12 sec." }, "ability_druid_demoralizingroar");
    /// <summary>Your pet does an additional [3*Pts]% damage with all attacks.</summary>
    public static final PetTalentData SharkAttack = new PetTalentData(29, "Shark Attack", 2, new String[] {
            "Your pet does an additional [3*Pts]% damage with all attacks.",
            "Your pet does an additional 3% damage with all attacks.",
            "Your pet does an additional 6% damage with all attacks." }, "inv_misc_fish_35");
    /// <summary>Your pet's Growl also heals it for [1*Pts]% of its total health.</summary>
    public static final PetTalentData Silverback = new PetTalentData(30, "Silverback", 2, new String[] {
            "Your pet's Growl also heals it for [1*Pts]% of its total health.",
            "Your pet's Growl also heals it for 1% of its total health.",
            "Your pet's Growl also heals it for 2% of its total health." }, "ability_hunter_pet_gorilla");
    /// <summary>Increases the critical strike chance of your pet by [3*Pts]%.</summary>
    public static final PetTalentData SpidersBite = new PetTalentData(31, "Spider's Bite", 3, new String[] {
            "Increases the critical strike chance of your pet by [3*Pts]%.",
            "Increases the critical strike chance of your pet by 3%.",
            "Increases the critical strike chance of your pet by 6%.",
            "Increases the critical strike chance of your pet by 9%." }, "ability_hunter_pet_spider");
    /// <summary>Your pet does an additional [3*Pts]% damage with all attacks.</summary>
    public static final PetTalentData SpikedCollar = new PetTalentData(32, "Spiked Collar", 3, new String[] {
            "Your pet does an additional [3*Pts]% damage with all attacks.",
            "Your pet does an additional 3% damage with all attacks.",
            "Your pet does an additional 6% damage with all attacks.",
            "Your pet does an additional 9% damage with all attacks." }, "inv_jewelry_necklace_22");
    /// <summary>Your pet taunts the target to attack it for 3 sec.</summary>
    public static final PetTalentData Taunt = new PetTalentData(33, "Taunt", 1, new String[] {
            "Your pet taunts the target to attack it for 3 sec.",
            "Your pet taunts the target to attack it for 3 sec." }, "spell_nature_reincarnation");
    /// <summary>Shakes the ground with thundering force, doing 3 to 5 Nature damage to all enemeies within 8 yards. This ability causes a moderate amount of additional threat.</summary>
    public static final PetTalentData Thunderstomp = new PetTalentData(34, "Thunderstomp", 1, new String[] {
            "Shakes the ground with thundering force, doing 3 to 5 Nature damage to all enemeies within 8 yards. This ability causes a moderate amount of additional threat.",
            "Shakes the ground with thundering force, doing 3 to 5 Nature damage to all enemeies within 8 yards. This ability causes a moderate amount of additional threat." }, "ability_golemthunderclap");
    /// <summary>Increases the contribution your pet gets from your Stamina by [20*Pts]% and attack power by [15*Pts]%.</summary>
    public static final PetTalentData WildHunt = new PetTalentData(35, "Wild Hunt", 2, new String[] {
            "Increases the contribution your pet gets from your Stamina by [20*Pts]% and attack power by [15*Pts]%.",
            "Increases the contribution your pet gets from your Stamina by 20% and attack power by 15%.",
            "Increases the contribution your pet gets from your Stamina by 40% and attack power by 30%." }, "inv_misc_horn_04");
    /// <summary>A fierce attack causing 5 damage, modified by pet level, that your pet can use after its target dodges. Cannot be dodged, blocked or parried.</summary>
    public static final PetTalentData WolverineBite = new PetTalentData(36, "Wolverine Bite", 1, new String[] {
            "A fierce attack causing 5 damage, modified by pet level, that your pet can use after its target dodges. Cannot be dodged, blocked or parried.",
            "A fierce attack causing 5 damage, modified by pet level, that your pet can use after its target dodges. Cannot be dodged, blocked or parried." }, "ability_druid_lacerate");
}

