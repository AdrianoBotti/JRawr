// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

class CombatTable {
    public boolean dw;
    public Character character;
    public DeathKnightTalents talents;
    public Stats stats;
    public CalculationOptionsTankDK calcOpts;

    public int getMBloodRunes() {
        return (int) this.calcOpts.getMRotation().AbilityCost[DKCostTypes.Blood.ordinal()];
    }

    public int getMFrostRunes() {
        return (int) this.calcOpts.getMRotation().AbilityCost[DKCostTypes.Frost.ordinal()];
    }

    public int getMUnholyRunes() {
        return (int) this.calcOpts.getMRotation().AbilityCost[DKCostTypes.UnHoly.ordinal()];
    }

    public int getMDeathRunes() {
        return (int) this.calcOpts.getMRotation().AbilityCost[DKCostTypes.Death.ordinal()];
    }

    public Weapon MH, OH;

    public float combinedSwingTime;

    public float physCrits,
            missedSpecial, dodgedSpecial,
            spellCrits, spellResist,
            totalMHMiss, totalOHMiss,
            realDuration, totalMeleeAbilities, totalParryableAbilities,
            totalSpellAbilities, normalizationFactor,
            chanceAvoided, chanceMissed, chanceDodged, chanceParried;
    /**
     * Used as Total GCD time - usually hasted, but need to confirm that.
     */
    private float fDuration;
    public float m_fRSCount = 0f;
    public Rotation.Type rotType;

    public CombatTable(Character c, Stats stats, CalculationOptionsTankDK calcOpts) {
        this(c, new CharacterCalculationsTankDK(), stats, calcOpts);
    }

    public CombatTable(Character c, CharacterCalculationsTankDK calcs, Stats stats, CalculationOptionsTankDK calcOpts) {
        dw = false;
        this.character = c;
        this.calcOpts = calcOpts;
        this.calcOpts.setTalents(character.getDeathKnightTalents());
        this.talents = character.getDeathKnightTalents();
        this.stats = stats;
        totalMeleeAbilities = 0f;
        totalSpellAbilities = 0f;

        totalMeleeAbilities = calcOpts.getMRotation().PlagueStrike + calcOpts.getMRotation().ScourgeStrike +
                calcOpts.getMRotation().Obliterate + calcOpts.getMRotation().BloodStrike + calcOpts.getMRotation().HeartStrike +
                calcOpts.getMRotation().FrostStrike + calcOpts.getMRotation().DeathStrike;

        totalSpellAbilities = calcOpts.getMRotation().DeathCoil + calcOpts.getMRotation().IcyTouch + calcOpts.getMRotation().HowlingBlast
                + calcOpts.getMRotation().Pestilence + calcOpts.getMRotation().HornOfWinter + calcOpts.getMRotation().DeathNDecay
                + calcOpts.getMRotation().BoneShield;

        totalParryableAbilities = calcOpts.getMRotation().PlagueStrike + calcOpts.getMRotation().ScourgeStrike +
                calcOpts.getMRotation().Obliterate + calcOpts.getMRotation().BloodStrike + calcOpts.getMRotation().HeartStrike;

        weapons();
        critsAndResists();
        fDuration = calcOpts.getMRotation().getGCDTime();

        rotType = calcOpts.getMRotation().curRotationType;
    }

    public void critsAndResists() {
        // Crits, Resists
        {
            // Attack Rolltable (DW):
            // 27.0% miss     (8.0% with 2H)
            //  6.5% dodge
            // 24.0% glancing (75% hit-dmg)
            // xx.x% crit
            // remaining = hit

            // Crit: Base .65%
            physCrits = .0065f;
            physCrits += stats.getPhysicalCrit();

            chanceAvoided = 0.335f;

            chanceDodged = StatConversion.WHITE_DODGE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()];

            if (character.getMainHand() != null) {
                chanceDodged = MH.chanceDodged;
            }

            if (character.getOffHand() != null) {
                if (character.getMainHand() != null && character.getMainHand().getSlot() != ItemSlot.TwoHand) {
                    dw = true;
                    chanceDodged += OH.chanceDodged;
                    chanceDodged /= 2;
                } else {
                    chanceDodged = OH.chanceDodged;
                }
            }

            float chanceMiss = StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()];
            if (dw) {
                chanceMiss = StatConversion.WHITE_MISS_CHANCE_CAP_DW[calcOpts.getTargetLevel() - character.getLevel()];
            }
            chanceMiss -= stats.getPhysicalHit();
            chanceMiss = Math.max(0f, chanceMiss);

            chanceAvoided = chanceDodged + chanceMiss;

            missedSpecial = chanceMiss;
            dodgedSpecial = chanceDodged;

            spellCrits = 0f;
            spellCrits += stats.getSpellCrit();

            // Resists: Base 17%
            spellResist = .17f;
            spellResist -= stats.getSpellHit();
            spellResist = Math.max(0f, spellResist);

            // Total physical misses
            totalMHMiss = MH.chanceDodged + chanceMiss;
            if (OH != null)
                totalOHMiss = OH.chanceDodged + chanceMiss;
            realDuration = calcOpts.getMRotation().curRotationDuration;
            realDuration += ((totalMeleeAbilities - calcOpts.getMRotation().FrostStrike) * chanceDodged * (1.5f)) +
                    ((totalMeleeAbilities - calcOpts.getMRotation().FrostStrike) * chanceMiss * (1.5f)) +
                    ((calcOpts.getMRotation().IcyTouch * spellResist * (((1.5f) / (1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.DeathKnight)) + stats.getSpellHaste())) <= 1.0f ? 1.0f : (((1.5f) / (1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.DeathKnight)) + stats.getSpellHaste()))))));
            //still need to implement spellhaste here
        }
    }

    public void weapons() {
        float MHExpertise = stats.getExpertise();
        float OHExpertise = stats.getExpertise();

        Item barehand = new Item();
        barehand.setSpeed(2.0f);
        barehand.setMaxDamage(2);
        barehand.setMinDamage(1);

        MH = new Weapon(barehand, stats, calcOpts, 0f);
        OH = new Weapon(barehand, stats, calcOpts, 0f);

        // Set if we're DW or not in 1 place.
        // Then use that member var to determine all other DW related issues.
        if (character.getMainHand() != null && character.getMainHand().getSlot() != ItemSlot.TwoHand && character.getOffHand() != null) {
            dw = true;
        }

        if (character.getMainHand() != null) {
            MH = new Weapon(character.getMainHand().getItem(), stats, calcOpts, MHExpertise);
            if (character.getMainHand().getSlot() == ItemSlot.TwoHand) {
                normalizationFactor = 3.3f;
                MH.damage *= 1f + .02f * talents.getTwoHandedWeaponSpecialization();
                combinedSwingTime = MH.hastedSpeed;
            } else normalizationFactor = 2.4f;
        }

        if (dw) {
            OH = new Weapon(character.getOffHand().getItem(), stats, calcOpts, OHExpertise);

            // Talent: Nerves of Cold Steel
            float OHMult = .08f * (float) talents.getNervesOfColdSteel();
            OH.damage *= (.5f + OHMult);

            //need this for weapon swing procs
            // in DW situations, weapon swing procs should happen MORE often, not less.
            combinedSwingTime = 1 / (1 / MH.hastedSpeed + 1 / OH.hastedSpeed);
        }

        if (character.getMainHand() == null && character.getOffHand() == null) {
            combinedSwingTime = 2f;
            normalizationFactor = 2.4f;
        }
    }

    public float getTotalThreat() {
        //DPS Subgroups
        float fDamWhite = 0f;
        float fDamBCB = 0f;
        float fDamNecrosis = 0f;
        float fDamDeathCoil = 0f;
        float fDamIcyTouch = 0f;
        float fDamPlagueStrike = 0f;
        float fDamFrostFever = 0f;
        float fDamBloodPlague = 0f;
        float fDamScourgeStrike = 0f;
        float fDamUnholyBlight = 0f;
        float fDamFrostStrike = 0f;
        float fDamHowlingBlast = 0f;
        float fDamDeathNDecay = 0f;
        float fDamObliterate = 0f;
        float fDamDeathStrike = 0f;
        float fDamBloodStrike = 0f;
        float fDamHeartStrike = 0f;
        float fDamDancingRuneWeapon = 0f;
        float fDamWanderingPlague = 0f;
        float fDamWPFromFF = 0f;
        float fDamWPFromBP = 0f;
        float fDamOtherShadow = 0f;
        float fDamOtherArcane = 0f;
        float fDamOtherFrost = 0f;
        float fDamRuneStrike = 0f;

        float missedSpecial = 0f;

        float fDamWhiteBeforeArmor = 0f;
        float fDamWhiteMinusGlancing = 0f;
        float fightDuration = calcOpts.getFightLength() * 60;
        float mitigation;
        float KMRatio = 0f;
        float CinderglacierMultiplier = 1f;

        float MHExpertise = stats.getExpertise();
        float OHExpertise = stats.getExpertise();

        //damage multipliers
        float spellPowerMult = 1f + stats.getBonusSpellPowerMultiplier();
        float frostSpellPowerMult = 1f + stats.getBonusSpellPowerMultiplier() + Math.max((stats.getBonusFrostDamageMultiplier()), 0f); //implement razorice here later
        float shadowSpellPowerMult = 1f + stats.getBonusSpellPowerMultiplier() + Math.max((stats.getBonusShadowDamageMultiplier()), 0f); //implement razorice here later

        float physPowerMult = 1f + stats.getBonusPhysicalDamageMultiplier();
        // Covers all % physical damage increases.  Blood Frenzy, FI.
        float partialResist = 0.94f; // Average of 6% damage lost to partial resists on spells

        //spell AP multipliers, for diseases its per tick
        float BloodPlagueAPMult = 0.055f;
        float DeathCoilAPMult = 0.15f;
        float DeathNDecayAPMult = .0475f;
        float FrostFeverAPMult = 0.055f;
        float GargoyleAPMult = 0.4f;
        float HowlingBlastAPMult = 0.1f;
        float IcyTouchAPMult = 0.1f;

        //for estimating rotation pushback

        calcOpts.getMRotation().avgDiseaseMult = calcOpts.getMRotation().numDisease * (calcOpts.getMRotation().diseaseUptime / 100);
        float commandMult = 0f;

        if (calcOpts.getMRotation().managedRP) {
            calcOpts.getMRotation().getRP(talents, character);
        }

        // Impurity Application
        {
            // TALENT: Impurity
            float impurityMult = 1f + (.05f * (float) talents.getImpurity());

            HowlingBlastAPMult *= impurityMult;
            IcyTouchAPMult *= impurityMult;
            FrostFeverAPMult *= impurityMult;
            BloodPlagueAPMult *= impurityMult;
            DeathCoilAPMult *= impurityMult;
            GargoyleAPMult *= impurityMult;
        }

        // racials
        {
            if (character.getRace() == rawr.base.character.CharacterRace.Orc) {
                commandMult += .05f;
            }
        }

        // Killing Machine
        {
            float KMPpM = (1f * talents.getKillingMachine()) * (1f + stats.getPhysicalHaste()); // KM Procs per Minute (Defined "1 per point" by Blizzard) influenced by Phys. Haste

            float KMPpR = KMPpM / (60 / calcOpts.getMRotation().curRotationDuration);
            float totalAbilities = calcOpts.getMRotation().FrostStrike + calcOpts.getMRotation().IcyTouch + calcOpts.getMRotation().HowlingBlast;
            if (totalAbilities > 0) {
                KMRatio = KMPpR / totalAbilities;
            }
        }

        // Cinderglacier
        {
            float shadowFrostAbilitiesPerSecond = ((calcOpts.getMRotation().DeathCoil + calcOpts.getMRotation().FrostStrike +
                    calcOpts.getMRotation().ScourgeStrike + calcOpts.getMRotation().IcyTouch + calcOpts.getMRotation().HowlingBlast) /
                    this.realDuration);
            if (shadowFrostAbilitiesPerSecond > 0) {
                CinderglacierMultiplier *= 1f + (0.2f / (shadowFrostAbilitiesPerSecond / stats.getCinderglacierProc()));
            }
        }

        // Mitigation
        {
            float targetArmor = calcOpts.getBossArmor();
            float totalArP = stats.getArmorPenetration();

            mitigation = 1f - StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor, stats.getArmorPenetration(), 0f, 0f);
            mitigation = Math.max(0, mitigation);
        }

        // These white damage values need to carry forward beyond just the individaul white damage attacks.
        // BCB matters.
        float MHDam = 0f, OHDam = 0f;
        // White Dmg
        {
            // Main Hand
            {
                float fDamMHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()] * MH.damage) * 0.75f;
                float fDamMHBeforeArmor = ((MH.damage * (1f - chanceAvoided - StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()])) * (1f + physCrits)) + fDamMHglancing;
                fDamWhiteMinusGlancing = fDamMHBeforeArmor - fDamMHglancing;
                fDamWhiteBeforeArmor = fDamMHBeforeArmor;
                MHDam = fDamMHBeforeArmor * mitigation;
                // fDuration = total GCD time.
                MHDam *= fDuration / MH.hastedSpeed;
            }

            // Off Hand
            if (dw) {
                if (this.OH.damage > 0) {
                    float fDamOHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()] * this.OH.damage) * 0.75f;
                    float fDamOHBeforeArmor = ((this.OH.damage * (1f - chanceAvoided - StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()])) * (1f + this.physCrits)) + fDamOHglancing;
                    fDamWhiteMinusGlancing += fDamOHBeforeArmor - fDamOHglancing;
                    fDamWhiteBeforeArmor += fDamOHBeforeArmor;
                    OHDam = fDamOHBeforeArmor * mitigation;
                    OHDam *= fDuration / OH.hastedSpeed;
                } else {
                    OHDam = 0f;
                }
            }

            fDamWhite = MHDam + OHDam;
        }

        // Necrosis
        {
            fDamNecrosis = fDamWhiteMinusGlancing * (.04f * (float) talents.getNecrosis()); // doesn't proc off Glancings
        }

        // Blood Caked Blade
        {
            float fDamMHBCB = 0f;
            float fDamOHBCB = 0f;
            if ((OHDam != 0) && dw) {
                fDamOHBCB = OHDam * (.25f + .125f * calcOpts.getMRotation().avgDiseaseMult);
            }
            if (MHDam != 0) {
                fDamMHBCB = MHDam * (.25f + .125f * calcOpts.getMRotation().avgDiseaseMult);
            }
            fDamBCB = fDamMHBCB + fDamOHBCB;
            fDamBCB *= .1f * (float) talents.getBloodCakedBlade();
        }

        // Death Coil
        {
            if (calcOpts.getMRotation().DeathCoil > 0f) {
                float DCCount = calcOpts.getMRotation().DeathCoil + (0.05f * (float) talents.getSuddenDoom() * (calcOpts.getMRotation().HeartStrike + calcOpts.getMRotation().BloodStrike));
                float DCDmg = 443f + (DeathCoilAPMult * stats.getAttackPower()) + stats.getBonusDeathCoilDamage();
                // Glyph of Dark Death adds 15% damage to each DC.
                DCDmg *= 1f + (talents.isGlyphofDarkDeath() ? .15f : 0f);
                // Mobidity adds 5% per point of damage to each DC.
                DCDmg *= 1f + (.05f * (float) talents.getMorbidity());
                // Unholy Blight causes additional 10% DC damage over 10 sec.
                DCDmg *= 1f + (talents.getUnholyBlight() * 0.1f);
                // Since we're looking at total threat returned, do not divide the damage on a per-second basis.
                fDamDeathCoil = DCDmg * DCCount;
                float DCCritDmgMult = 1.5f * (1f + stats.getBonusSpellCritMultiplier());
                float DCCrit = 1f + ((this.spellCrits + stats.getBonusDeathCoilCrit()) * DCCritDmgMult);
                fDamDeathCoil *= DCCrit;
            }
        }

        // Icy Touch
        // this seems to handle crit strangely.
        // additionally, looks like it's missing some multipliers? maybe they're applied later
        {
            if (calcOpts.getMRotation().IcyTouch > 0f) {
                float addedCritFromKM = KMRatio;
                float ITCount = calcOpts.getMRotation().IcyTouch;
                float ITDmg = 236f + (IcyTouchAPMult * stats.getAttackPower()) + stats.getBonusIcyTouchDamage();
                ITDmg *= 1f + (.1f * (float) talents.getImprovedIcyTouch());
                // Total damage for IT is ITdam * # of ITs
                fDamIcyTouch = ITDmg * ITCount;
                float ITCritDmgMult = 1.5f * (1f + stats.getBonusSpellCritMultiplier());
                float ITCrit = 1f + ((this.spellCrits + addedCritFromKM + (.05f * (float) talents.getRime())) * ITCritDmgMult);
                fDamIcyTouch *= ITCrit;
                fDamIcyTouch *= 7;
            }
        }

        // Plague Strike
        {
            if (calcOpts.getMRotation().PlagueStrike > 0f) {
                float PCCount = calcOpts.getMRotation().PlagueStrike;
                float PSDmg = (MHDam * .5f) + 189f;
                PSDmg += (OHDam * (talents.getThreatOfThassarian() / 3f) * 0.5f);
                fDamPlagueStrike = PSDmg * PCCount;
                float PSCritDmgMult = 2f * (1f + (.15f * (float) talents.getViciousStrikes()) + stats.getBonusCritMultiplier());
                float PSCrit = 1f + ((this.physCrits + (.03f * (float) talents.getViciousStrikes()) + stats.getBonusPlagueStrikeCrit()) * PSCritDmgMult);
                fDamPlagueStrike *= PSCrit;

                fDamPlagueStrike *= (talents.isGlyphofPlagueStrike() ? 1.2f : 1f);

                fDamPlagueStrike *= 1f + (.1f * (float) talents.getOutbreak());
            }
        }

        // Frost Fever
        {
            if (calcOpts.getMRotation().IcyTouch > 0f || (talents.isGlyphofHowlingBlast() && calcOpts.getMRotation().HowlingBlast > 0f)) {
                // Frost Fever is renewed with every Icy Touch/HB and starts a new cd
                // So how many applications of FF do we have through the rotation?
                float FFCount = calcOpts.getMRotation().IcyTouch + (talents.isGlyphofHowlingBlast() ? calcOpts.getMRotation().HowlingBlast : 0f);
                float FFCD = 3f / (calcOpts.getMRotation().diseaseUptime / 100f);
                int tempF = (int) Math.floor(FFCount / FFCD);
                FFCD = ((calcOpts.getMRotation().IcyTouch - ((float) tempF * FFCD)) / ((float) tempF + 1f)) + FFCD;
                // Patch 3.2: Incease Damage by 15%
                float FFDmg = (FrostFeverAPMult * stats.getAttackPower() + 25.6f) * 1.15f;
                // Patch 3.3: Glyph of IT increases damage by 20%
                FFDmg *= 1f + (talents.isGlyphofIcyTouch() ? .2f : 0f);
                if (fDuration > 0) {
                    fDamFrostFever = FFDmg / FFCD * fDuration;
                    if (talents.isGlyphofHowlingBlast() && calcOpts.getMRotation().HowlingBlast > 0f) {
                        // If we're using the glyph of HB, then all targets get FF.
                        fDamFrostFever *= calcOpts.getUNumberTargets();
                    }
                    fDamWPFromFF = fDamFrostFever * this.physCrits * fDuration;
                }
            }
        }

        // Blood Plague
        {
            if (calcOpts.getMRotation().PlagueStrike > 0f) {
                // Blood Plague is renewed with every Plague Strike and starts a new cd
                float PSCD = calcOpts.getMRotation().curRotationDuration / calcOpts.getMRotation().PlagueStrike;
                float BPCD = 3f / (calcOpts.getMRotation().diseaseUptime / 100);
                int tempF = (int) Math.floor(PSCD / BPCD);
                BPCD = ((PSCD - ((float) tempF * BPCD)) / ((float) tempF + 1f)) + BPCD;
                // Patch 3.2: Increase damage by 15%
                float BPDmg = (BloodPlagueAPMult * stats.getAttackPower() + 31.1f) * 1.15f;
                if (fDuration > 0) {
                    fDamBloodPlague = BPDmg / BPCD * fDuration;
                    fDamWPFromBP = fDamBloodPlague * this.physCrits * fDuration;
                }
            }
        }

        // Pestilence
        {
            if (calcOpts.getMRotation().Pestilence > 0f) {
                fDamBloodPlague *= calcOpts.getUNumberTargets();
                fDamFrostFever *= calcOpts.getUNumberTargets();
            }
        }

        // Wandering Plague
        {
            if ((float) talents.getWanderingPlague() > 0) {
                fDamWanderingPlague = fDamWPFromBP + fDamWPFromFF;
                fDamWanderingPlague *= (1f / 3f) * (float) talents.getWanderingPlague();
                // Since the damage is spread across all nearby targets, let's add that in.
                fDamWanderingPlague *= calcOpts.getUNumberTargets();
            } else {
                fDamWanderingPlague = 0f;
            }
        }

        // Scourge Strike
        {
            if (talents.getScourgeStrike() > 0 && calcOpts.getMRotation().ScourgeStrike > 0f) {
                float SSCount = calcOpts.getMRotation().ScourgeStrike;
                float SSDmg = (MHDam * .70f) + 357.188f +
                        stats.getBonusScourgeStrikeDamage();
                SSDmg *= 1f + 0.12f * calcOpts.getMRotation().avgDiseaseMult * (1f + stats.getBonusPerDiseaseScourgeStrikeDamage());
                SSDmg += 120; // Bonus threat
                fDamScourgeStrike = SSDmg * SSCount;
                float SSCritDmgMult = 2f * (1f + (.15f * (float) talents.getViciousStrikes()) + stats.getBonusCritMultiplier());
                float SSCrit = 1f + ((this.physCrits + (.03f * (float) talents.getViciousStrikes()) + (.03f * (float) talents.getSubversion()) + stats.getBonusScourgeStrikeCrit()) * SSCritDmgMult);
                fDamScourgeStrike *= SSCrit;
                fDamScourgeStrike *= 1f + ((2f / 3f) * (float) talents.getOutbreak());
            }
        }

        // Frost Strike
        {
            if (talents.getFrostStrike() > 0 && calcOpts.getMRotation().FrostStrike > 0f) {
                float addedCritFromKM = KMRatio;
                float FSCount = calcOpts.getMRotation().FrostStrike;
                // Patch 3.2: 60% Damage tuned to 55% weapon damage
                float FSDmg = (MHDam * .55f) +
                        150f + stats.getBonusFrostStrikeDamage();
                // Threat of Thassarian:
                FSDmg = ((OHDam * .55f) + 150f + stats.getBonusFrostStrikeDamage()) * (talents.getThreatOfThassarian() / 3f) * .5f;
                fDamFrostStrike = FSDmg * FSCount;
                float FSCritDmgMult = 2f * (1f + (.15f * (float) talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier());
                float FSCrit = 1f + ((this.physCrits + addedCritFromKM + stats.getBonusFrostStrikeCrit()) * FSCritDmgMult);
                fDamFrostStrike *= FSCrit;
                fDamFrostStrike *= 1f + .03f * talents.getBloodOfTheNorth();
            }
        }

        // Trinket direct-damage procs, razorice damage, etc
        {
            fDamOtherArcane = stats.getArcaneDamage();
            fDamOtherShadow = stats.getShadowDamage();

            if (this.MH != null) {
                float dpsMHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()] * MH.DPS) * 0.75f;
                float dpsMHBeforeArmor = ((MH.DPS * (1f - chanceAvoided - StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()])) * (1f + physCrits)) + dpsMHglancing;
                fDamOtherFrost += (dpsMHBeforeArmor - dpsMHglancing);   // presumably doesn't proc off of glancings, like necrosis
            }

            if (this.OH != null) {
                float dpsOHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()] * OH.DPS) * 0.75f;
                float dpsOHBeforeArmor = ((OH.DPS * (1f - chanceAvoided - StatConversion.WHITE_GLANCE_CHANCE_CAP[calcOpts.getTargetLevel() - character.getLevel()])) * (1f + physCrits)) + dpsOHglancing;
                fDamOtherFrost += (dpsOHBeforeArmor - dpsOHglancing);
            }

            fDamOtherFrost += fDamOtherFrost * stats.getBonusFrostWeaponDamage();

            float OtherCritDmgMult = 1.5f * (1f + stats.getBonusSpellCritMultiplier());
            float OtherCrit = 1f + ((this.spellCrits) * OtherCritDmgMult);
            fDamOtherArcane *= OtherCrit;
            fDamOtherShadow *= OtherCrit;
        }

        // Howling Blast
        {
            if ((talents.getHowlingBlast() > 0) && (calcOpts.getMRotation().HowlingBlast > 0f)) {
                float addedCritFromKM = KMRatio;
                float HBCount = calcOpts.getMRotation().HowlingBlast;
                float HBDmg = 540f + stats.getBonusHowlingBlastDamage() + HowlingBlastAPMult * stats.getAttackPower();
                fDamHowlingBlast = HBDmg * HBCount;
                float HBCritDmgMult = 1.5f * (1f + (.15f * (float) talents.getGuileOfGorefiend()) + stats.getBonusSpellCritMultiplier());
                float HBCrit = 1f + ((this.spellCrits + addedCritFromKM) * HBCritDmgMult);
                fDamHowlingBlast *= HBCrit;
                // Multiply this by the number of targets hit.
                fDamHowlingBlast *= calcOpts.getUNumberTargets();
            }
        }

        // Death n Decay
        {
            if (calcOpts.getMRotation().DeathNDecay > 0f) {
                float DNDCD = 30f - (talents.getMorbidity() * 5f); // 30 sec cool down modified by Morbidity
                float DNDDur = 10f; // 10 sec duration
                float DNDDmg = (62 + DeathNDecayAPMult * stats.getAttackPower() * DNDDur) * (talents.isGlyphofDeathandDecay() ? 1.2f : 1f);
                fDamDeathNDecay = DNDDmg * (Math.min(calcOpts.getMRotation().DeathNDecay, (fDuration / DNDCD)));
                float DNDCritDmgMult = 1.5f * (1f + stats.getBonusSpellCritMultiplier());
                float DNDCrit = 1f + (this.spellCrits * DNDCritDmgMult);
                fDamDeathNDecay *= DNDCrit;
                // Threat: damage × 1.90
                fDamDeathNDecay *= 1.9f;
                // Multiply this by the number of targets hit.
                fDamDeathNDecay *= calcOpts.getUNumberTargets();
            }
        }

        // Obliterate
        {
            if (calcOpts.getMRotation().Obliterate > 0f) {
                float OblitCount = calcOpts.getMRotation().Obliterate;
                float OblitDmg = (MHDam * 0.8f) + stats.getBonusObliterateDamage();
                // Threat of Thassarian:
                OblitDmg += ((OHDam * 0.8f) + stats.getBonusObliterateDamage()) * (talents.getThreatOfThassarian() / 3f) * 0.5f;
                OblitDmg *= 1f + 0.125f * (float) calcOpts.getMRotation().avgDiseaseMult * (1f + stats.getBonusPerDiseaseObliterateDamage());
                fDamObliterate = OblitDmg * OblitCount;
                float OblitCritDmgMult = 2f * (1f + (.15f * (float) talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier());
                float OblitCrit = 1f + ((this.physCrits +
                        (.03f * (float) talents.getSubversion()) +
                        (0.05f * (float) talents.getRime()) +
                        stats.getBonusObliterateCrit()) * OblitCritDmgMult);
                fDamObliterate *= OblitCrit;
                fDamObliterate *= (talents.isGlyphofObliterate() ? 1.2f : 1f);
            }
        }

        // Death Strike
        {
            if (calcOpts.getMRotation().DeathStrike > 0f) {
                float DSCount = calcOpts.getMRotation().DeathStrike;
                float DSDmg = (MHDam * 0.75f) + 222.75f + stats.getBonusDeathStrikeDamage();
                // Threat of Thassarian:
                DSDmg += (OHDam * .75f) * (talents.getThreatOfThassarian() / 3f) * 0.5f;
                DSDmg *= 1f + 0.15f * (float) talents.getImprovedDeathStrike();
                DSDmg *= (talents.isGlyphofDeathStrike() ? 1.25f : 1f);
                fDamDeathStrike = DSDmg * DSCount;
                float DSCritDmgMult = 2f * (1f + (.15f * (float) talents.getMightOfMograine()) + stats.getBonusCritMultiplier());
                float DSCrit = 1f + ((this.physCrits +
                        (.03f * (float) talents.getImprovedDeathStrike()) +
                        (0.05f * (float) talents.getRime()) +
                        stats.getBonusDeathStrikeCrit()) * DSCritDmgMult);
                // Factor in threat from damage
                fDamDeathStrike *= DSCrit;
                // Add in threat from Healing.
                fDamDeathStrike += ((stats.getHealth() * .05f * calcOpts.getMRotation().numDisease) / calcOpts.getUNumberTargets() / 2); // threat from Healing divided by number of targets.
                stats.setHealthRestoreFromMaxHealth(stats.getHealthRestoreFromMaxHealth() + .05f * calcOpts.getMRotation().numDisease);
            }
        }

        // Blood Strike
        {
            if (calcOpts.getMRotation().BloodStrike > 0f) {
                float BSCount = calcOpts.getMRotation().BloodStrike;
                float BSDmg = (MHDam * 0.4f) + 305.6f + stats.getBonusBloodStrikeDamage();
                // Threat of Thassarian:
                BSDmg += (OHDam * .4f) * (talents.getThreatOfThassarian() / 3f) * 0.5f;
                BSDmg *= 1f + 0.125f * (float) calcOpts.getMRotation().avgDiseaseMult * (1f + stats.getBonusPerDiseaseBloodStrikeDamage());
                fDamBloodStrike = BSDmg * BSCount;
                float BSCritDmgMult = 2f * (1f + (.15f * (float) talents.getMightOfMograine()) + (.15f * (float) talents.getGuileOfGorefiend()) +
                        stats.getBonusCritMultiplier());
                float BSCrit = 1f + ((this.physCrits + (.03f * (float) talents.getSubversion())) * BSCritDmgMult);
                fDamBloodStrike *= BSCrit;
                fDamBloodStrike *= 1f + (.03f * (float) talents.getBloodOfTheNorth());
                // Patch 3.2: From 15% to 5% per point.
                fDamBloodStrike *= 1f + (.05f * (float) talents.getBloodyStrikes());
            }
        }

        // Heart Strike
        {
            if (talents.getHeartStrike() > 0 && calcOpts.getMRotation().HeartStrike > 0f) {
                float HSCount = calcOpts.getMRotation().HeartStrike;
                float HSDmg = ((this.MH.baseDamage + ((stats.getAttackPower() / 14f) * this.normalizationFactor)) *
                        0.5f) + 368f + stats.getBonusHeartStrikeDamage();
                HSDmg *= 1f + 0.1f * (float) calcOpts.getMRotation().avgDiseaseMult * (1f + stats.getBonusPerDiseaseHeartStrikeDamage());
                fDamHeartStrike = HSDmg * HSCount;
                float HSCritDmgMult = 2f * (1f + (.15f * (float) talents.getMightOfMograine()) + stats.getBonusCritMultiplier());
                float HSCrit = 1f + ((this.physCrits + (.03f * (float) talents.getSubversion())) * HSCritDmgMult);
                fDamHeartStrike *= HSCrit;
                fDamHeartStrike += fDamHeartStrike * (0.5f * (Math.min(2, calcOpts.getUNumberTargets()) - 1));
                // Patch 3.2: From 15% to 5% per point.
                fDamHeartStrike *= 1f + (.05f * (float) talents.getBloodyStrikes());
            }
        }

        // Rune Strike
        {
            float RSDmg = (MHDam + (150 * stats.getAttackPower() * 10 / 10000));
            // Threat of Thassarian
            RSDmg += (OHDam + (150 * stats.getAttackPower() * 10 / 10000)) * (talents.getThreatOfThassarian() / 3f) * 0.5f;
            RSDmg *= (1f + stats.getBonusRuneStrikeMultiplier()); // Two T8.
            // what's the threat modifier?
            RSDmg *= (1.75f); // 3.3 update to RS Threat.
            float RSCritDmgMult = 2f * (1f + (.15f * (float) talents.getMightOfMograine()) + stats.getBonusCritMultiplier() + (talents.isGlyphofRuneStrike() ? .1f : 0f));
            float RSCrit = 1f + ((this.physCrits) * RSCritDmgMult);

            m_fRSCount = calcOpts.getMRotation().RuneStrike;

            fDamRuneStrike = RSDmg * m_fRSCount;
            fDamRuneStrike *= RSCrit;
        }

        float BCBMult = 1f;
        float BloodPlagueMult = 1f;
        float BloodStrikeMult = 1f + stats.getBonusBloodStrikeDamageMultiplier();
        float DeathCoilMult = 1f;
        float DancingRuneWeaponMult = 1f;
        float FrostFeverMult = 1f;
        float FrostStrikeMult = 1f;
        float BloodwormsMult = 1f + commandMult;
        float HeartStrikeMult = 1f + stats.getBonusHeartStrikeDamageMultiplier();
        float HowlingBlastMult = 1f;
        // Adding the multiplier from the T10 set bonus.
        float DeathNDecayMult = 1f + stats.getTankDK_T10_2pc();
        float IcyTouchMult = 1f;
        float NecrosisMult = 1f;
        float ObliterateMult = 1f;
        float DeathStrikeMult = 1f;
        float PlagueStrikeMult = 1f;
        float ScourgeStrikeMult = 1f;
        float UnholyBlightMult = 1f;
        float WhiteMult = 1f;
        float WanderingPlagueMult = 1f;
        float otherShadowMult = 1f;
        float otherArcaneMult = 1f;
        float otherFrostMult = 1f;

        // Apply Physical Mitigation
        {
            float physMit = mitigation;
            physMit *= 1f + (dw ? 0f : .02f * talents.getTwoHandedWeaponSpecialization());

            fDamBCB *= physMit;
            fDamBloodStrike *= physMit;
            fDamHeartStrike *= physMit;
            fDamObliterate *= physMit;
            fDamDeathStrike *= physMit;
            fDamPlagueStrike *= physMit;
            fDamRuneStrike *= physMit;

            WhiteMult += physPowerMult - 1f;
            BCBMult += physPowerMult - 1f;
            BloodStrikeMult += physPowerMult - 1f;
            HeartStrikeMult += physPowerMult - 1f;
            ObliterateMult += physPowerMult - 1f;
            DeathStrikeMult += physPowerMult - 1f;
            PlagueStrikeMult += physPowerMult - 1f;
        }

        // Apply Elemental Strike Mitigation
        {
            float strikeMit = /*missedSpecial **/ partialResist;
            strikeMit *= (dw ? 1f : 1f + .02f * talents.getTwoHandedWeaponSpecialization());

            fDamScourgeStrike *= strikeMit;
            fDamFrostStrike *= strikeMit * (1f - missedSpecial);

            ScourgeStrikeMult += spellPowerMult - 1f;
            FrostStrikeMult += frostSpellPowerMult - 1f;
        }

        // Apply Magical Mitigation
        {
            // some of this applies to necrosis, I wonder if it is ever accounted for
            float magicMit = partialResist /** combatTable.spellResist*/;
            // magicMit = 1f - magicMit;

            fDamNecrosis *= magicMit;
            fDamBloodPlague *= magicMit;
            fDamDeathCoil *= magicMit * (1 - this.spellResist);
            fDamFrostFever *= magicMit;
            fDamHowlingBlast *= magicMit * (1 - this.spellResist);
            fDamIcyTouch *= magicMit;
            fDamUnholyBlight *= magicMit * (1 - this.spellResist);

            NecrosisMult += spellPowerMult - 1f;
            BloodPlagueMult += spellPowerMult - 1f;
            DeathCoilMult += shadowSpellPowerMult - 1f;
            FrostFeverMult += frostSpellPowerMult - 1f;
            HowlingBlastMult += frostSpellPowerMult - 1f;
            IcyTouchMult += frostSpellPowerMult - 1f;
            UnholyBlightMult += shadowSpellPowerMult - 1f;
            otherShadowMult += shadowSpellPowerMult - 1f;
            otherArcaneMult += spellPowerMult - 1f;
            otherFrostMult += frostSpellPowerMult - 1f;
        }

        // Cinderglacier multipliers
        {
            DeathCoilMult *= CinderglacierMultiplier;
            HowlingBlastMult *= CinderglacierMultiplier;
            IcyTouchMult *= CinderglacierMultiplier;
            ScourgeStrikeMult *= CinderglacierMultiplier;
            FrostStrikeMult *= CinderglacierMultiplier;
        }

        // Apply Multi-Ability Talent Multipliers
        {
            float BloodyVengeanceMult = .03f * (float) talents.getBloodyVengeance();
            BCBMult *= 1 + BloodyVengeanceMult;
            BloodStrikeMult *= 1 + BloodyVengeanceMult;
            HeartStrikeMult *= 1 + BloodyVengeanceMult;
            ObliterateMult *= 1 + BloodyVengeanceMult;
            DeathStrikeMult *= 1 + BloodyVengeanceMult;
            PlagueStrikeMult *= 1 + BloodyVengeanceMult;
            WhiteMult *= 1 + BloodyVengeanceMult;

            float BlackIceMult = .02f * (float) talents.getBlackIce();
            FrostFeverMult *= 1 + BlackIceMult;
            HowlingBlastMult *= 1 + BlackIceMult;
            IcyTouchMult *= 1 + BlackIceMult;
            FrostStrikeMult *= 1 + BlackIceMult;
            DeathCoilMult *= 1 + BlackIceMult;
            ScourgeStrikeMult *= 1 + BlackIceMult;
            BloodPlagueMult *= 1 + BlackIceMult;
            otherShadowMult *= 1 + BlackIceMult;
            otherFrostMult *= 1 + BlackIceMult;

            float MercilessCombatMult = .315f * 0.06f * (float) talents.getMercilessCombat();   // The last 35% of a Boss don't take 35% of the fight-time...say .315 (10% faster)
            ObliterateMult *= 1 + MercilessCombatMult;
            HowlingBlastMult *= 1 + MercilessCombatMult;
            IcyTouchMult *= 1 + MercilessCombatMult;
            FrostStrikeMult *= 1 + MercilessCombatMult;

            float GlacierRot = .0666666666666f * (float) talents.getGlacierRot();
            HowlingBlastMult *= 1 + GlacierRot;
            IcyTouchMult *= 1 + GlacierRot;
            FrostStrikeMult *= 1 + GlacierRot;

            float CryptFeverMult = .1f * (float) talents.getCryptFever();
            float CryptFeverBuff = stats.getBonusDiseaseDamageMultiplier();
            CryptFeverMult = Math.max(CryptFeverMult, CryptFeverBuff);
            FrostFeverMult *= 1 + CryptFeverMult;
            BloodPlagueMult *= 1 + CryptFeverMult;
            UnholyBlightMult *= 1 + CryptFeverMult;

            float DesolationMult = .01f * (float) talents.getDesolation();  //the new desolation is basically a flat 1% per point
            BCBMult *= 1 + DesolationMult;
            BloodPlagueMult *= 1 + DesolationMult;
            BloodStrikeMult *= 1 + DesolationMult;
            DeathCoilMult *= 1 + DesolationMult;
            DancingRuneWeaponMult *= 1 + DesolationMult;
            FrostFeverMult *= 1 + DesolationMult;
            FrostStrikeMult *= 1 + DesolationMult;
            HeartStrikeMult *= 1 + DesolationMult;
            HowlingBlastMult *= 1 + DesolationMult;
            DeathNDecayMult *= 1 + DesolationMult;
            IcyTouchMult *= 1 + DesolationMult;
            NecrosisMult *= 1 + DesolationMult;
            ObliterateMult *= 1 + DesolationMult;
            DeathStrikeMult *= 1 + DesolationMult;
            PlagueStrikeMult *= 1 + DesolationMult;
            ScourgeStrikeMult *= 1 + DesolationMult;
            UnholyBlightMult *= 1 + DesolationMult;
            WhiteMult *= 1 + DesolationMult;
            otherShadowMult *= 1 + DesolationMult;
            otherArcaneMult *= 1 + DesolationMult;
            otherFrostMult *= 1 + DesolationMult;

            if ((float) talents.getBoneShield() >= 1f) {
                float BoneMult = .02f;
                BCBMult *= 1 + BoneMult;
                BloodPlagueMult *= 1 + BoneMult;
                BloodStrikeMult *= 1 + BoneMult;
                DeathCoilMult *= 1 + BoneMult;
                DancingRuneWeaponMult *= 1 + BoneMult;
                FrostFeverMult *= 1 + BoneMult;
                FrostStrikeMult *= 1 + BoneMult;
                HeartStrikeMult *= 1 + BoneMult;
                HowlingBlastMult *= 1 + BoneMult;
                DeathNDecayMult *= 1 + BoneMult;
                IcyTouchMult *= 1 + BoneMult;
                NecrosisMult *= 1 + BoneMult;
                ObliterateMult *= 1 + BoneMult;
                DeathStrikeMult *= 1 + BoneMult;
                PlagueStrikeMult *= 1 + BoneMult;
                ScourgeStrikeMult *= 1 + BoneMult;
                UnholyBlightMult *= 1 + BoneMult;
                WhiteMult *= 1 + BoneMult;
                otherShadowMult *= 1 + BoneMult;
                otherArcaneMult *= 1 + BoneMult;
                otherFrostMult *= 1 + BoneMult;
            }
        }

        //feed variables for output
        fDamBCB *= BCBMult;
        fDamBloodPlague *= BloodPlagueMult;
        fDamBloodStrike *= BloodStrikeMult;
        fDamDeathCoil *= DeathCoilMult;
        fDamDancingRuneWeapon *= DancingRuneWeaponMult;
        fDamFrostFever *= FrostFeverMult;
        fDamFrostStrike *= FrostStrikeMult;
        fDamHeartStrike *= HeartStrikeMult;
        fDamHowlingBlast *= HowlingBlastMult;
        fDamDeathNDecay *= DeathNDecayMult;
        fDamIcyTouch *= IcyTouchMult;
        fDamNecrosis *= NecrosisMult;
        fDamObliterate *= ObliterateMult;
        fDamDeathStrike *= DeathStrikeMult;
        fDamPlagueStrike *= PlagueStrikeMult;
        fDamScourgeStrike *= ScourgeStrikeMult;
        fDamUnholyBlight *= UnholyBlightMult;
        fDamWhite *= WhiteMult;
        fDamWanderingPlague *= WanderingPlagueMult;
        fDamRuneStrike += fDamOtherShadow * otherShadowMult +
                fDamOtherArcane * otherArcaneMult +
                fDamOtherFrost * otherFrostMult;

        float DPSPoints = 0;
        DPSPoints += fDamBCB;
        DPSPoints += fDamBloodPlague;
        DPSPoints += fDamBloodStrike;
        DPSPoints += fDamDeathCoil;
        DPSPoints += fDamDancingRuneWeapon;
        DPSPoints += fDamFrostFever;
        DPSPoints += fDamFrostStrike;
        DPSPoints += fDamHeartStrike;
        DPSPoints += fDamHowlingBlast;
        DPSPoints += fDamDeathNDecay;
        DPSPoints += fDamIcyTouch;
        DPSPoints += fDamNecrosis;
        DPSPoints += fDamObliterate;
        DPSPoints += fDamDeathStrike;
        DPSPoints += fDamPlagueStrike;
        DPSPoints += fDamScourgeStrike;
        DPSPoints += fDamUnholyBlight;
        DPSPoints += fDamWhite;
        DPSPoints += fDamWanderingPlague;
        DPSPoints += fDamRuneStrike;

        // Multiply by Frost Presence modifier:
        DPSPoints *= 2.0735f;
        DPSPoints *= 1 + stats.getBonusDamageMultiplier();
        // Figure out how much damage may have been done when factoring in missrate.
        // This currenty is just physicalhit not spell hit.  Need to fix that.
        DPSPoints *= 1f - chanceMissed;

        return DPSPoints;
    }
}
