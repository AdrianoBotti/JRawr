// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/DRW.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class DRW {
    public float dpsDancingRuneWeapon = 0f;

    public DRW(CombatTable combatTable, CharacterCalculationsDPSDK calcs, CalculationOptionsDPSDK calcOpts, Stats stats,
               rawr.base.character.Character character, DeathKnightTalents talents) {
        boolean DW = false;

        float dpsWhite = 0f;
        float dpsBCB = 0f;
        float dpsNecrosis = 0f;
        float dpsDeathCoil = 0f;
        float dpsIcyTouch = 0f;
        float dpsPlagueStrike = 0f;
        float dpsFrostFever = 0f;
        float dpsBloodPlague = 0f;
        float dpsDeathStrike = 0f;
        float dpsHeartStrike = 0f;
        float dpsWPFromFF = 0f;
        float dpsWPFromBP = 0f;
        float dpsWhiteMinusGlancing = 0f;
        float dpsWhiteBeforeArmor = 0f;

        float IcyTouchAPMult = 0.1f;
        float FrostFeverAPMult = 0.055f;
        float BloodPlagueAPMult = 0.055f;
        float DeathCoilAPMult = 0.15f;

        float OHMult = 0.5f;

        int targetLevel = calcOpts.getTargetLevel();
        float arpBuffs = talents.getBloodGorged() * 2f / 100;
        float mitigation = StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getBossArmor(), stats.getArmorPenetration(), arpBuffs, stats.getArmorPenetrationRating());
        mitigation = 1 - mitigation;
        float MHDPS = 0f, OHDPS = 0f;

        // White Dmg
        {
            // Main Hand
            {
                float dpsMHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * combatTable.MH.DPS) * 0.7f;
                float dpsMHBeforeArmor = ((combatTable.MH.DPS * (1f - calcs.avoidedAttacks - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80])) * (1f + combatTable.physCrits)) + dpsMHglancing;
                dpsWhiteMinusGlancing = dpsMHBeforeArmor - dpsMHglancing;
                dpsWhiteBeforeArmor = dpsMHBeforeArmor;
                MHDPS = dpsMHBeforeArmor * mitigation;
            }

            // Off Hand
            if (DW || (character.getMainHand() == null && character.getOffHand() != null)) {
                float dpsOHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * combatTable.OH.DPS) * 0.7f;
                float dpsOHBeforeArmor = ((combatTable.OH.DPS * (1f - calcs.avoidedAttacks - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80])) * (1f + combatTable.physCrits)) + dpsOHglancing;
                dpsOHBeforeArmor *= OHMult;
                dpsWhiteMinusGlancing += dpsOHBeforeArmor - dpsOHglancing;
                dpsWhiteBeforeArmor += dpsOHBeforeArmor;
                OHDPS = dpsOHBeforeArmor * mitigation;
            }

            dpsWhite = MHDPS + OHDPS;
        }

        // Necrosis
        {
            dpsNecrosis = dpsWhiteMinusGlancing * (.04f * (float) talents.getNecrosis()); // doesn't proc off Glancings
        }

        // Blood Caked Blade
        {
            float dpsMHBCB = 0f;
            if (combatTable.MH.damage != 0) {
                float MHBCBDmg = (float) (combatTable.MH.damage * (.25f + .125f * calcOpts.getRotation().getAvgDiseaseMult()));
                dpsMHBCB = MHBCBDmg / combatTable.MH.hastedSpeed;
            }
            dpsBCB = dpsMHBCB;
            dpsBCB *= .1f * (float) talents.getBloodCakedBlade();
        }

        // Death Coil
        {
            if (calcOpts.getRotation().getDeathCoil() > 0f) {
                float DCCD = 1;
                float DCDmg = 443f + (DeathCoilAPMult * stats.getAttackPower());
                dpsDeathCoil = DCDmg / DCCD;
                float DCCritDmgMult = .5f * (2f + stats.getBonusCritMultiplier());
                float DCCrit = 1f + ((combatTable.spellCrits + stats.getBonusDeathCoilCrit()) * DCCritDmgMult);
                dpsDeathCoil *= DCCrit;

                dpsDeathCoil *= 1f + (.05f * (float) talents.getMorbidity());
            }
        }

        // Icy Touch
        {
            if (calcOpts.getRotation().getIcyTouch() > 0f) {
                float ITCD = 1;
                float ITDmg = 236f + (IcyTouchAPMult * stats.getAttackPower()) + stats.getBonusIcyTouchDamage();
                ITDmg *= 1f + .1f * (float) talents.getImprovedIcyTouch();
                dpsIcyTouch = ITDmg / ITCD;
                float ITCritDmgMult = .5f * (2f + stats.getCritBonusDamage() + stats.getBonusCritMultiplier());
                float ITCrit = 1f + (Math.min((combatTable.spellCrits + (.05f * (float) talents.getRime())), 1f) * ITCritDmgMult);
                dpsIcyTouch *= ITCrit;
            }
        }

        // Plague Strike
        {
            if (calcOpts.getRotation().getPlagueStrike() > 0f) {
                float PSCD = 1;
                float PSDmg = (combatTable.MH.baseDamage + ((stats.getAttackPower() / 14f)
                        * combatTable.normalizationFactor)) * 0.5f + 189f;
                dpsPlagueStrike = PSDmg / PSCD;
                float PSCritDmgMult = 1f + (.15f * (float) talents.getViciousStrikes()) + stats.getBonusCritMultiplier();
                float PSCrit = 1f + ((combatTable.physCrits + (.03f * (float) talents.getViciousStrikes())
                        + stats.getBonusPlagueStrikeCrit()) * PSCritDmgMult);

                dpsPlagueStrike *= PSCrit;
                dpsPlagueStrike *= 1f + (.1f * (float) talents.getOutbreak());
            }
        }

        // Frost Fever
        {
            if (calcOpts.getRotation().getIcyTouch() > 0f
                    || (talents.isGlyphofHowlingBlast() && calcOpts.getRotation().getHowlingBlast() > 0f)
                    || (talents.isGlyphofDisease() && calcOpts.getRotation().getPestilence() > 0f)) {
                float FFDmg = FrostFeverAPMult * stats.getAttackPower() + 25.6f;
                dpsFrostFever = FFDmg;
                dpsFrostFever *= 1.15f;    // Patch 3.2: Diseases hit 15% harder.
                dpsWPFromFF = dpsFrostFever * combatTable.physCrits;
            }
        }

        // Blood Plague
        {
            if (calcOpts.getRotation().getPlagueStrike() > 0f || talents.isGlyphofDisease()) {
                float BPDmg = BloodPlagueAPMult * stats.getAttackPower() + 31.1f;
                dpsBloodPlague = BPDmg;
                dpsBloodPlague *= 1.15f; // Patch 3.2: Diseases hit 15% harder.
                dpsWPFromBP = dpsBloodPlague * combatTable.physCrits;
            }
        }

        // Death Strike
        {
            if (calcOpts.getRotation().getDeathStrike() > 0f) {
                float DSCD = 1;
                float DSDmg = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14f) * combatTable.normalizationFactor)) * 0.75f) + 222.75f;
                DSDmg *= 1f + 0.15f * (float) talents.getImprovedDeathStrike();
                dpsDeathStrike = DSDmg / DSCD;
                float DSCritDmgMult = 1f + (.15f * (float) talents.getMightOfMograine()) + stats.getBonusCritMultiplier();
                float DSCrit = 1f + ((combatTable.physCrits
                        + (.03f * (float) talents.getImprovedDeathStrike())
                        + stats.getBonusDeathStrikeCrit()) * DSCritDmgMult);
                dpsDeathStrike *= DSCrit;
            }
        }

        // Heart Strike
        {
            if (talents.getHeartStrike() > 0 && calcOpts.getRotation().getHeartStrike() > 0f) {
                float HSCD = 1;
                float HSDmg = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14f) * combatTable.normalizationFactor))
                        * 0.5f) + 368f;
                HSDmg *= 1f + 0.1f * (float) calcOpts.getRotation().getAvgDiseaseMult() * (1f + stats.getBonusPerDiseaseHeartStrikeDamage());
                dpsHeartStrike = HSDmg / HSCD;
                float HSCritDmgMult = 1f + (.15f * (float) talents.getMightOfMograine()) + stats.getBonusCritMultiplier();
                float HSCrit = 1f + ((combatTable.physCrits + (.03f * (float) talents.getSubversion())) * HSCritDmgMult);
                dpsHeartStrike = (dpsHeartStrike) * HSCrit;
                dpsHeartStrike *= 1f + (.15f * (float) talents.getBloodyStrikes());
            }
        }

        float BCBMult = 1f;
        float BloodPlagueMult = 1f;
        float DeathCoilMult = 1f;
        float FrostFeverMult = 1f;
        float HeartStrikeMult = 1f;
        float IcyTouchMult = 1f;
        float NecrosisMult = 1f;
        float DeathStrikeMult = 1f;
        float PlagueStrikeMult = 1f;
        float WhiteMult = 1f;

        float spellPowerMult = 1f + stats.getBonusSpellPowerMultiplier();
        float frostSpellPowerMult = 1f + stats.getBonusSpellPowerMultiplier() + Math.max((stats.getBonusFrostDamageMultiplier() - stats.getBonusShadowDamageMultiplier()), 0f);
        float partialResist = 0.94f;
        float physPowerMult = 1f;

        // Apply Physical Mitigation
        {
            float physMit = mitigation;
            physMit *= 1f + (!DW ? .02f * talents.getTwoHandedWeaponSpecialization() : 0f);

            dpsBCB *= physMit;
            dpsHeartStrike *= physMit;
            dpsDeathStrike *= physMit;
            dpsPlagueStrike *= physMit;
            WhiteMult += physPowerMult - 1f;
            BCBMult += physPowerMult - 1f;
            HeartStrikeMult += physPowerMult - 1f;
            DeathStrikeMult += physPowerMult - 1f;
            PlagueStrikeMult += physPowerMult - 1f;
        }

        // Apply Magical Mitigation
        {
            // some of this applies to necrosis, I wonder if it is ever accounted for
            float magicMit = partialResist; // combatTable.spellResist not applied

            dpsNecrosis *= magicMit;
            dpsBloodPlague *= magicMit;
            dpsDeathCoil *= magicMit * (1f - combatTable.spellResist);
            dpsFrostFever *= magicMit;
            dpsIcyTouch *= magicMit;

            NecrosisMult += spellPowerMult - 1f;
            BloodPlagueMult += spellPowerMult - 1f;
            DeathCoilMult += spellPowerMult - 1f;
            FrostFeverMult += frostSpellPowerMult - 1f;
            IcyTouchMult += frostSpellPowerMult - 1f;
        }

        // Apply Multi-Ability Talent Multipliers
        {
            float BloodyVengeanceMult = .03f * (float) talents.getBloodyVengeance();
            BCBMult *= 1 + BloodyVengeanceMult;
            HeartStrikeMult *= 1 + BloodyVengeanceMult;
            DeathStrikeMult *= 1 + BloodyVengeanceMult;
            PlagueStrikeMult *= 1 + BloodyVengeanceMult;
            WhiteMult *= 1 + BloodyVengeanceMult;

            float HysteriaCoeff = .2f / 2f; // current uptime is 50% for now
            float HysteriaMult = HysteriaCoeff * (float) talents.getHysteria();
            BCBMult *= 1 + HysteriaMult;
            HeartStrikeMult *= 1 + HysteriaMult;
            DeathStrikeMult *= 1 + HysteriaMult;
            PlagueStrikeMult *= 1 + HysteriaMult;
            WhiteMult *= 1 + HysteriaMult;

            float BlackIceMult = .02f * (float) talents.getBlackIce();
            FrostFeverMult *= 1 + BlackIceMult;
            IcyTouchMult *= 1 + BlackIceMult;
            DeathCoilMult *= 1 + BlackIceMult;
            BloodPlagueMult *= 1 + BlackIceMult;

            float CryptFeverMult = .1f * (float) talents.getCryptFever();
            float CryptFeverBuff = stats.getBonusDiseaseDamageMultiplier();
            CryptFeverMult = Math.max(CryptFeverMult, CryptFeverBuff);
            FrostFeverMult *= 1 + CryptFeverMult;
            BloodPlagueMult *= 1 + CryptFeverMult;
        }

        dpsDancingRuneWeapon = dpsWhite * WhiteMult * 17f
                + dpsBCB * BCBMult * 17f
                + dpsNecrosis * NecrosisMult * 17f
                + dpsDeathCoil * DeathCoilMult
                + dpsIcyTouch * IcyTouchMult
                + dpsPlagueStrike * PlagueStrikeMult
                + dpsFrostFever * (5f + talents.getEpidemic()) * FrostFeverMult
                + dpsBloodPlague * (5f + talents.getEpidemic()) * BloodPlagueMult
                + dpsDeathStrike * DeathStrikeMult * (talents.isGlyphofDancingRuneWeapon() ? 1f : 0f)
                + dpsHeartStrike * HeartStrikeMult * (5f + (talents.isGlyphofDancingRuneWeapon() ? 2f : 0f));
        dpsDancingRuneWeapon *= 0.5f;
    }
}
