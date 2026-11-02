// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class CombatTable {
    public Character character;
    public CharacterCalculationsDPSDK calcs;
    public DeathKnightTalents talents;
    public Stats stats;
    public CalculationOptionsDPSDK calcOpts;

    public Weapon MH;
    public Weapon OH;
    public boolean DW;

    public float combinedSwingTime;

    public float physCrits;
    public float hitBonus;
    public float missedSpecial;
    public float dodgedSpecial;
    public float spellCrits;
    public float spellResist;
    public float totalMHMiss;
    public float totalOHMiss;
    public float realDuration;
    public float totalMeleeAbilities;
    public float whiteMiss;
    public float totalSpellAbilities;
    public float normalizationFactor;
    public float physicalMitigation;

    public Item additionalItem;

    public CombatTable(Character c, Stats stats, CalculationOptionsDPSDK calcOpts) {
        this(c, new CharacterCalculationsDPSDK(), stats, calcOpts);
    }

    public CombatTable(Character c, CharacterCalculationsDPSDK calcs, Stats stats, CalculationOptionsDPSDK calcOpts) {
        character = c;
        this.calcs = calcs;
        talents = c.getDeathKnightTalents();
        this.calcOpts = calcOpts;
        this.stats = stats;
        this.additionalItem = null;
        totalMeleeAbilities = 0f;
        totalSpellAbilities = 0f;

        float DWExtraHits = 1f; // +talents.ThreatOfThassarian / 3f;
        totalMeleeAbilities = (float) (calcOpts.getRotation().getPlagueStrike() * DWExtraHits + calcOpts.getRotation().getScourgeStrike()
                + calcOpts.getRotation().getObliterate() * DWExtraHits + calcOpts.getRotation().getBloodStrike() * DWExtraHits + calcOpts.getRotation().getHeartStrike()
                + calcOpts.getRotation().getFrostStrike() * DWExtraHits + calcOpts.getRotation().getDeathStrike() * DWExtraHits);

        totalSpellAbilities = (float) (calcOpts.getRotation().getDeathCoil() + calcOpts.getRotation().getIcyTouch() + calcOpts.getRotation().getHowlingBlast()
                + calcOpts.getRotation().getPestilence() + calcOpts.getRotation().getHorn() + calcOpts.getRotation().getGhoulFrenzy());

        hitBonus = .01f * (float) talents.getNervesOfColdSteel();
        Weapons();
        CritsAndResists();
    }

    public void CritsAndResists() {
        // Crits, Resists
        {
            // Attack Rolltable (DW):
            // 27.0% miss     (8.0% with 2H)
            //  6.5% dodge
            // 24.0% glancing (75% hit-dmg)
            // xx.x% crit
            // remaining = hit

            float targetArmor = calcOpts.getBossArmor();
            float totalArP = stats.getArmorPenetration();

            float arpBuffs = talents.getBloodGorged() * 2f / 100;

            physicalMitigation = 1f - StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor,
                    stats.getArmorPenetration(), arpBuffs, stats.getArmorPenetrationRating());

            calcs.enemyMitigation = 1f - physicalMitigation;
            calcs.effectiveArmor = physicalMitigation;

            // Crit: Base .65%
            physCrits = .0065f;
            physCrits += StatConversion.getPhysicalCritFromRating(stats.getCritRating());
            physCrits += StatConversion.getPhysicalCritFromAgility(stats.getAgility(), rawr.base.character.CharacterClass.DeathKnight);
            physCrits += .01f * (float) (talents.getDarkConviction() + talents.getEbonPlaguebringer() + talents.getAnnihilation());
            physCrits += stats.getPhysicalCrit();
            calcs.critChance = physCrits;

            float chanceAvoided = 0.335f;

            float chanceDodged = StatConversion.WHITE_DODGE_CHANCE_CAP[calcOpts.getTargetLevel() - 80];

            calcs.dodgedMHAttacks = MH.chanceDodged;
            calcs.dodgedOHAttacks = OH.chanceDodged;

            if (character.getMainHand() != null) {
                chanceDodged = MH.chanceDodged;
            }

            if (character.getOffHand() != null) {
                if (DW) {
                    chanceDodged += OH.chanceDodged;
                    chanceDodged /= 2;
                } else if (character.getMainHand() == null) {
                    chanceDodged = OH.chanceDodged;
                }
            }

            calcs.dodgedAttacks = chanceDodged;
            // Process White hits:
            float chanceMiss = DW ? StatConversion.WHITE_MISS_CHANCE_CAP_DW[calcOpts.getTargetLevel() - 80] : StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80];
            chanceMiss -= StatConversion.getPhysicalHitFromRating(stats.getHitRating());
            chanceMiss -= hitBonus;
            chanceMiss -= stats.getPhysicalHit();
            // Cap the Miss rate at 0%
            chanceMiss = Math.max(chanceMiss, 0f);
            calcs.missedAttacks = chanceMiss;
            whiteMiss = chanceMiss;
            chanceAvoided = chanceDodged + chanceMiss;
            calcs.avoidedAttacks = chanceDodged + chanceMiss;

            // Process Yellow hits
            chanceMiss = StatConversion.YELLOW_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80];
            chanceMiss -= StatConversion.getPhysicalHitFromRating(stats.getHitRating());
            chanceMiss -= hitBonus;
            chanceMiss -= stats.getPhysicalHit();
            chanceMiss = Math.max(chanceMiss, 0f);
            chanceDodged = MH.chanceDodged;
            missedSpecial = chanceMiss;
            dodgedSpecial = chanceDodged;

            spellCrits = 0f;
            spellCrits += StatConversion.getSpellCritFromRating(stats.getCritRating());
            spellCrits += stats.getSpellCrit() + stats.getSpellCritOnTarget();
            spellCrits += .01f * (float) (talents.getDarkConviction() + talents.getEbonPlaguebringer());
            calcs.spellCritChance = spellCrits;

            // Resists: Base 17%
            spellResist = .17f;
            spellResist -= StatConversion.getSpellHitFromRating(stats.getHitRating());
            spellResist -= .01f * talents.getVirulence();
            spellResist -= stats.getSpellHit();
            if (spellResist < 0f) spellResist = 0f;

            // Total physical misses
            totalMHMiss = calcs.dodgedMHAttacks + whiteMiss;
            totalOHMiss = calcs.dodgedOHAttacks + whiteMiss;
            double spellGCD = (calcOpts.getRotation().getPresence() == CalculationOptionsDPSDK.Presence.Blood ? 1.5d / ((1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), rawr.base.character.CharacterClass.DeathKnight))) * (1d + stats.getSpellHaste())) < 1d ? 1d : 1.5d / ((1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), rawr.base.character.CharacterClass.DeathKnight))) * (1d + stats.getSpellHaste())) : 1d);
            double physicalGCD = (calcOpts.getRotation().getPresence() == CalculationOptionsDPSDK.Presence.Blood ? 1.5d : 1d);
            float minDuration = totalMeleeAbilities * (float) physicalGCD
                    + totalSpellAbilities * (float) spellGCD;

            realDuration = (float) Math.max(minDuration, calcOpts.getRotation().getCurRotationDuration());

            float dodgeMissPerRotation = (float) (totalMeleeAbilities - calcOpts.getRotation().getFrostStrike());
            chanceAvoided = chanceDodged + chanceMiss;
            double GChanceAvoided = (1 / (1 - chanceAvoided)) - 1;
            double GSpellResist = (1 / (1 - spellResist)) - 1;
            double ProbableGCDLossPerRotation = dodgeMissPerRotation * physicalGCD * GChanceAvoided
                    + (calcOpts.getRotation().getIcyTouch() + calcOpts.getRotation().getPestilence()) * spellGCD * GSpellResist;

            realDuration += (float) (((minDuration + ProbableGCDLossPerRotation) / realDuration < 1 ? (minDuration + ProbableGCDLossPerRotation) / realDuration : 1) * ProbableGCDLossPerRotation);
            // This last line is a bit hackish, but basically the extra GCD is more inconvenient the closer we are to having a GCD-capped rotation; once we're GCD-capped, they cost the full value.
        }
    }

    public void Weapons() {
        float MHExpertise = stats.getExpertise();
        float OHExpertise = stats.getExpertise();

        if (character.getRace() == CharacterRace.Dwarf) {
            if (character.getMainHand() != null
                    && (character.getMainHand().getItem().getType() == ItemType.OneHandMace
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandMace)) {
                MHExpertise += 5f;
            }

            if (character.getOffHand() != null && character.getOffHand().getItem().getType() == ItemType.OneHandMace) {
                OHExpertise += 5f;
            }
        } else if (character.getRace() == CharacterRace.Orc) {
            if (character.getMainHand() != null
                    && (character.getMainHand().getItem().getType() == ItemType.OneHandAxe
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandAxe)) {
                MHExpertise += 5f;
            }

            if (character.getOffHand() != null && character.getOffHand().getItem().getType() == ItemType.OneHandAxe) {
                OHExpertise += 5f;
            }
        }
        if (character.getRace() == CharacterRace.Human) {
            if (character.getMainHand() != null
                    && (character.getMainHand().getItem().getType() == ItemType.OneHandSword
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandSword
                    || character.getMainHand().getItem().getType() == ItemType.OneHandMace
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandMace)) {
                MHExpertise += 3f;
            }

            if (character.getOffHand() != null
                    && (character.getOffHand().getItem().getType() == ItemType.OneHandSword
                    || character.getOffHand().getItem().getType() == ItemType.OneHandMace)) {
                OHExpertise += 3f;
            }
        }

        MH = new Weapon(null, stats, calcOpts, talents, 0f);
        OH = new Weapon(null, null, null, talents, 0f);

        DW = character.getMainHand() != null && character.getOffHand() != null
                && character.getMainHand().getSlot() != ItemSlot.TwoHand;

        if (character.getMainHand() != null) {
            MH = new Weapon(character.getMainHand().getItem(), stats, calcOpts, talents, MHExpertise);
            calcs.mhAttackSpeed = MH.hastedSpeed;
            calcs.mhWeaponDamage = MH.damage;
            calcs.mhExpertise = MH.effectiveExpertise;
        }

        if (character.getOffHand() != null && DW) {
            OH = new Weapon(character.getOffHand().getItem(), stats, calcOpts, talents, OHExpertise);

            calcs.ohAttackSpeed = OH.hastedSpeed;
            calcs.ohWeaponDamage = OH.damage;
            calcs.ohExpertise = OH.effectiveExpertise;
        }

        if (additionalItem != null && ((additionalItem.getSlot() == ItemSlot.MainHand) || (additionalItem.getSlot() == ItemSlot.TwoHand) || additionalItem.getSlot() == ItemSlot.OneHand)) {
            MH = new Weapon(additionalItem, stats, calcOpts, talents, MHExpertise);
            calcs.mhAttackSpeed = MH.hastedSpeed;
            calcs.mhWeaponDamage = MH.damage;
            calcs.mhExpertise = MH.effectiveExpertise;
        } else if (additionalItem != null && (additionalItem.getSlot() == ItemSlot.OffHand || additionalItem.getSlot() == ItemSlot.OneHand)) {
            OH = new Weapon(additionalItem, stats, calcOpts, talents, OHExpertise);
            calcs.ohAttackSpeed = OH.hastedSpeed;
            calcs.ohWeaponDamage = OH.damage;
            calcs.ohExpertise = OH.effectiveExpertise;
        }

        // MH-only
        if ((character.getMainHand() != null) && (!DW)) {
            if (character.getMainHand().getItem().getType() == ItemType.TwoHandAxe
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandMace
                    || character.getMainHand().getItem().getType() == ItemType.TwoHandSword
                    || character.getMainHand().getItem().getType() == ItemType.Polearm) {
                normalizationFactor = 3.3f;
                MH.damage *= 1f + .02f * talents.getTwoHandedWeaponSpecialization();
            } else {
                normalizationFactor = 2.4f;
            }

            combinedSwingTime = MH.hastedSpeed;
            calcs.ohAttackSpeed = 0f;
            calcs.ohWeaponDamage = 0f;
            calcs.ohExpertise = 0f;
        }
        // DW or no MH
        else if (character.getOffHand() != null) {
            // need this for weapon swing procs
            combinedSwingTime = (MH.hastedSpeed + OH.hastedSpeed) / 4;
            normalizationFactor = 2.4f;
        }
        // Unarmed
        else if (character.getMainHand() == null && character.getOffHand() == null) {
            combinedSwingTime = 2f;
            normalizationFactor = 2.4f;
        }
    }
}
