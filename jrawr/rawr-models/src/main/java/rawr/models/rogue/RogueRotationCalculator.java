// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Rogue/RogueRotationCalculator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.rogue;

import rawr.base.character.Character;
import rawr.base.items.ItemType;
import rawr.base.stats.Stats;

public class RogueRotationCalculator {
    private Character character;
    private Stats stats;
    private CalculationOptionsRogue calcOpts;
    private float duration;
    private boolean maintainBleed;
    private float mainHandSpeed;
    private float offHandSpeed;
    private float mainHandSpeedNorm;
    private float offHandSpeedNorm;
    private float chanceExtraCPPerHit;
    private float chanceExtraCPPerMutiHit;
    private float avoidedWhiteMHAttacks;
    private float avoidedWhiteOHAttacks;
    private float avoidedMHAttacks;
    private float avoidedOHAttacks;
    private float avoidedFinisherAttacks;
    private float avoidedPoisonAttacks;

    private RogueAbilityStats mainHandStats;
    private RogueAbilityStats offHandStats;
    private RogueAbilityStats backstabStats;
    private RogueAbilityStats hemoStats;
    private RogueAbilityStats mutiStats;
    private RogueAbilityStats sStrikeStats;
    private RogueAbilityStats ruptStats;
    private RogueAbilityStats envenomStats;
    private RogueAbilityStats evisStats;
    private RogueAbilityStats snDStats;
    private RogueAbilityStats iPStats;
    private RogueAbilityStats dPStats;
    private RogueAbilityStats wPStats;
    private RogueAbilityStats aPStats;

    private float bonusDamageMultiplierHFB;
    private float bonusEnergyRegen;
    private float bonusEnergyRegenMultiplier;
    private float bonusFlurryHaste;
    private float bonusHemoDamageMultiplier;
    private float bonusIPFrequencyMultiplier;
    private float bonusMaxEnergy;
    private float bonusStealthEnergyRegen;
    private float chanceOnCPOnSSCrit;
    private float chanceOnEnergyOnCrit;
    private float chanceOnEnergyOnOHAttack;
    private float chanceOnEnergyPerCPFinisher;
    private float chanceOnMHAttackOnSwordAxeHit;
    private float chanceOnNoDPConsumeOnEnvenom;
    private float chanceOnSnDResetOnEnvenom;
    private float cPOnFinisher;
    private float flurryCostReduction;
    private float toTTCDReduction;
    private float toTTCostReduction;
    private float vanishCDReduction;

    private float[] averageNormalCP = new float[6];
    private float[] averageSStrikeCP = new float[6];
    private float[] averageMutiCP = new float[6];
    private float[] avgMutiNeeded = new float[6];

    public RogueRotationCalculator(Character character, Stats stats, CalculationOptionsRogue calcOpts, boolean maintainBleed,
            float mainHandSpeed, float offHandSpeed, float mainHandSpeedNorm, float offHandSpeedNorm,
            float avoidedWhiteMHAttacks, float avoidedWhiteOHAttacks, float avoidedMHAttacks, float avoidedOHAttacks,
            float avoidedFinisherAttacks, float avoidedPoisonAttacks,
            float chanceExtraCPPerHit, float chanceExtraCPPerMutiHit,
            RogueAbilityStats mainHandStats, RogueAbilityStats offHandStats, RogueAbilityStats backstabStats,
            RogueAbilityStats hemoStats, RogueAbilityStats sStrikeStats,
            RogueAbilityStats mutiStats, RogueAbilityStats ruptStats, RogueAbilityStats evisStats,
            RogueAbilityStats envenomStats, RogueAbilityStats snDStats,
            RogueAbilityStats iPStats, RogueAbilityStats dPStats, RogueAbilityStats wPStats, RogueAbilityStats aPStats) {
        this.character = character;
        this.stats = stats;
        this.calcOpts = calcOpts;
        duration = calcOpts.getDuration();
        this.maintainBleed = maintainBleed;
        this.mainHandSpeed = mainHandSpeed;
        this.offHandSpeed = offHandSpeed;
        this.mainHandSpeedNorm = mainHandSpeedNorm;
        this.offHandSpeedNorm = offHandSpeedNorm;
        this.avoidedWhiteMHAttacks = avoidedWhiteMHAttacks;
        this.avoidedWhiteOHAttacks = avoidedWhiteOHAttacks;
        this.avoidedMHAttacks = avoidedMHAttacks;
        this.avoidedOHAttacks = avoidedOHAttacks;
        this.avoidedFinisherAttacks = avoidedFinisherAttacks;
        this.avoidedPoisonAttacks = avoidedPoisonAttacks;
        this.chanceExtraCPPerHit = chanceExtraCPPerHit;
        this.chanceExtraCPPerMutiHit = chanceExtraCPPerMutiHit;

        this.mainHandStats = mainHandStats;
        this.offHandStats = offHandStats;
        this.backstabStats = backstabStats;
        this.hemoStats = hemoStats;
        this.mutiStats = mutiStats;
        this.sStrikeStats = sStrikeStats;
        this.ruptStats = ruptStats;
        this.envenomStats = envenomStats;
        this.evisStats = evisStats;
        this.snDStats = snDStats;
        this.iPStats = iPStats;
        this.dPStats = dPStats;
        this.wPStats = wPStats;
        this.aPStats = aPStats;

        // Talent bonuses
        bonusFlurryHaste = 0.2f * character.getRogueTalents().getBladeFlurry();
        bonusEnergyRegen = (15 + (character.getRogueTalents().isGlyphOfAdrenalineRush() ? 5f : 0f)) * character.getRogueTalents().getAdrenalineRush();
        bonusEnergyRegenMultiplier = 0.08f * character.getRogueTalents().getVitality();
        bonusHemoDamageMultiplier = 0.1f * character.getRogueTalents().getSurpriseAttacks() + 0.02f * character.getRogueTalents().getSinisterCalling();
        bonusIPFrequencyMultiplier = 0.1f * character.getRogueTalents().getImprovedPoisons();
        bonusMaxEnergy = (10 + (character.getRogueTalents().isGlyphOfVigor() ? 10 : 0)) * character.getRogueTalents().getVigor();
        bonusStealthEnergyRegen = 0.3f * character.getRogueTalents().getOverkill();
        chanceOnCPOnSSCrit = character.getRogueTalents().isGlyphOfSinisterStrike() ? 0.5f : 0f;
        chanceOnEnergyOnCrit = 2f * (character.getRogueTalents().getFocusedAttacks() > 2 ? 1f : (0.33f * character.getRogueTalents().getFocusedAttacks()));
        chanceOnEnergyOnOHAttack = 3 * 0.2f * character.getRogueTalents().getCombatPotency();
        chanceOnEnergyPerCPFinisher = 0.04f * character.getRogueTalents().getRelentlessStrikes();
        chanceOnMHAttackOnSwordAxeHit = 0.01f * character.getRogueTalents().getHackAndSlash();
        chanceOnNoDPConsumeOnEnvenom = character.getRogueTalents().getMasterPoisoner() == 3 ? 1f : (0.33f * character.getRogueTalents().getMasterPoisoner());
        chanceOnSnDResetOnEnvenom = 0.2f * character.getRogueTalents().getCutToTheChase();
        cPOnFinisher = 0.2f * character.getRogueTalents().getRuthlessness() + 3f * stats.getChanceOn3CPOnFinisher();
        flurryCostReduction = character.getRogueTalents().isGlyphOfBladeFlurry() ? 25 : 0;
        toTTCDReduction = 5 * character.getRogueTalents().getFilthyTricks();
        toTTCostReduction = 5 * character.getRogueTalents().getFilthyTricks();
        vanishCDReduction = 30 * character.getRogueTalents().getElusiveness();

        // Probability tables
        float c = chanceExtraCPPerHit;
        float h = (1f - c);
        float f = cPOnFinisher;
        float nf = (1f - f);
        averageNormalCP[1] = 1 * (f + nf * h) + 2 * (nf * c);
        averageNormalCP[2] = 2 * (f * h + nf * c + nf * h * h) + 3 * (f * c);
        averageNormalCP[3] = 3 * (f * c + f * h * h + 2 * nf * c * h + nf * h * h * h) + 4 * (f * h * c + nf * c * c + nf * h * h * c);
        averageNormalCP[4] = 4 * (2 * f * c * h + f * h * h * h + nf * c * c + 3 * nf * c * h * h + nf * h * h * h * h) + 5 * (f * c * c + f * h * h * c + 2 * nf * c * h * c + nf * h * h * h * c);
        averageNormalCP[5] = 5 * (f * c * c + 3 * f * c * h * h + f * h * h * h * h + 3 * nf * c * c * h + 4 * nf * c * h * h * h + nf * h * h * h * h * h) + 6 * (2 * f * c * h * c + f * h * h * h * c + nf * c * c * c + 3 * nf * c * h * h * c + nf * h * h * h * h * c);

        c = chanceExtraCPPerHit + chanceOnCPOnSSCrit * sStrikeStats.getCritChance();
        h = (1f - c);
        averageSStrikeCP[1] = 1 * (f + nf * h) + 2 * (nf * c);
        averageSStrikeCP[2] = 2 * (f * h + nf * c + nf * h * h) + 3 * (f * c + nf * h * c);
        averageSStrikeCP[3] = 3 * (f * c + f * h * h + 2 * nf * c * h + nf * h * h * h) + 4 * (f * h * c + nf * c * c + nf * h * h * c);
        averageSStrikeCP[4] = 4 * (2 * f * c * h + f * h * h * h + nf * c * c + 3 * nf * c * h * h + nf * h * h * h * h) + 5 * (f * c * c + f * h * h * c + 2 * nf * c * h * c + nf * h * h * h * c);
        averageSStrikeCP[5] = 5 * (f * c * c + 3 * f * c * h * h + f * h * h * h * h + 3 * nf * c * c * h + 4 * nf * c * h * h * h + nf * h * h * h * h * h) + 6 * (2 * f * c * h * c + f * h * h * h * c + nf * c * c * c + 3 * nf * c * h * h * c + nf * h * h * h * h * c);

        c = chanceExtraCPPerMutiHit;
        h = (1f - c);
        averageMutiCP[1] = 1 * (f) + 2 * (nf * h) + 3 * (nf * c);
        averageMutiCP[2] = 2 * (nf * h) + 3 * (f * h + nf * c) + 4 * (f * c);
        averageMutiCP[3] = 3 * (f * h + nf * c) + 4 * (f * c + nf * h * h) + 5 * (nf * h * c);
        averageMutiCP[4] = 4 * (f * c + nf * h * h) + 5 * (f * h * h + 2 * nf * h * c) + 5 * (f * h * c + nf * c * c);
        averageMutiCP[5] = 5 * (f * h * h + 2 * nf * h * c) + 6 * (2 * f * h * c + nf * h * h * h + nf * c * c) + 7 * (f * c * c + nf * h * h * c);

        avgMutiNeeded[1] = 1 * (nf);
        avgMutiNeeded[2] = 1 * (f * h + f * c + nf * h + nf * c);
        avgMutiNeeded[3] = 1 * (f * h + f * c + nf * c) + 2 * (nf * h);
        avgMutiNeeded[4] = 1 * (f * c) + 2 * (f * h * h + f * h * c + nf * h * h + nf * h * c + nf * c);
        avgMutiNeeded[5] = 2 * (f + nf * h * c + nf * c) + 3 * (nf * h * h);
    }

    public RogueRotationCalculation getRotationCalculations(int CPG, boolean useRupt, int finisher, int finisherCP,
            int snDCP, int mHPoison, int oHPoison, boolean bleedIsUp, boolean useTotT, boolean PTRMode) {
        float energyRegen = 10f * (1f + bonusEnergyRegenMultiplier);
        float totalEnergyAvailable = 100f + bonusMaxEnergy +
                energyRegen * duration +
                ((duration - 20f) / (180f - vanishCDReduction)) * 20f * energyRegen * bonusStealthEnergyRegen +
                (useTotT ? (stats.getBonusToTTEnergy() > 0 ? stats.getBonusToTTEnergy() : (-15f + toTTCostReduction)) * (duration - 5f) / (30f - toTTCDReduction) : 0f) +
                (useRupt ? 0.02f * (duration / 2f) * stats.getReduceEnergyCostFromRupture() : 0f) +
                energyRegen * 2f * bonusEnergyRegen * (duration / 180f) -
                (bonusFlurryHaste > 0 ? (25f - flurryCostReduction) * duration / 120f : 0f);
        float averageGCD = 1f / (1f - avoidedMHAttacks);
        float averageFinisherGCD = 1f / (1f - avoidedFinisherAttacks);
        float ruptDurationAverage = ruptStats.getDurationAverage();
        float[] averageCP = CPG == 0 ? averageMutiCP : CPG == 1 ? averageSStrikeCP : averageNormalCP;
        float averageFinisherCP = averageCP[5];

        // Melee
        float whiteMHAttacks = duration / mainHandSpeed + 0.5f * 0.5f * stats.getMoteOfAnger() * duration;
        float whiteOHAttacks = duration / offHandSpeed + 0.5f * 0.5f * stats.getMoteOfAnger() * duration;
        totalEnergyAvailable += whiteOHAttacks * (1f - avoidedWhiteOHAttacks) * chanceOnEnergyOnOHAttack +
                chanceOnEnergyOnCrit * whiteMHAttacks * mainHandStats.getCritChance() +
                chanceOnEnergyOnCrit * whiteOHAttacks * offHandStats.getCritChance();

        // Combo Point Generator
        float cpgCount = 0f;
        float cpgEnergy = CPG == 2 ? backstabStats.getEnergyCost() : CPG == 3 ? hemoStats.getEnergyCost() : CPG == 1 ? sStrikeStats.getEnergyCost() : mutiStats.getEnergyCost();
        float cPPerCPG = CPG == 2 ? backstabStats.getCPPerSwing() : CPG == 3 ? hemoStats.getCPPerSwing() : CPG == 1 ? sStrikeStats.getCPPerSwing() : mutiStats.getCPPerSwing();

        // Slice and Dice
        float averageSnDCP = averageCP[snDCP];

        // Lose some time due to SnD/Rupt conflicts
        float snDRuptConflict = (1f / ruptDurationAverage) * 0.5f * (averageGCD * averageFinisherCP / cPPerCPG);

        float snDDuration = snDStats.getDurationAverage() + 3f * Math.min(5f, averageSnDCP)
                - snDRuptConflict;
        float snDCount = duration / snDDuration;
        snDCount = Math.max(1f, (finisher == 2 ? snDCount * (1f - chanceOnSnDResetOnEnvenom) : snDCount));
        float snDTotalEnergy = snDCount * (snDStats.getEnergyCost() - 25f * chanceOnEnergyPerCPFinisher * Math.min(5f, averageSnDCP));
        float snDCPRequired = snDCount * (averageSnDCP - cPOnFinisher);
        float cpgToUse = snDCPRequired / cPPerCPG;
        cpgCount += cpgToUse;
        totalEnergyAvailable -= cpgToUse * cpgEnergy + snDTotalEnergy;

        // Damage Finishers
        float ruptCount = 0f;
        float evisCount = 0f;
        float envenomCount = 0f;
        if (useRupt) {
            // Rupture
            // Lose GCDs at the start of the fight to get SnD up and enough CPGs to get 5CPG.
            float durationRuptable = duration - 2f * averageGCD - (averageGCD * (averageFinisherCP / cPPerCPG));
            float ruptCountMax = durationRuptable / ruptStats.getDurationAverage();
            float ruptCycleEnergy = ((averageFinisherCP - cPOnFinisher) / cPPerCPG) * cpgEnergy + ruptStats.getEnergyCost() - 25f * chanceOnEnergyPerCPFinisher * averageFinisherCP;
            ruptCount = Math.min(ruptCountMax, totalEnergyAvailable / ruptCycleEnergy);
            cpgCount += ((averageFinisherCP - cPOnFinisher) / cPPerCPG) * ruptCount;
            totalEnergyAvailable -= ruptCycleEnergy * ruptCount;
        }
        if (finisher == 1 && finisherCP > 0) {
            // Eviscerate
            float averageEvisCP = averageCP[finisherCP];
            float evisCycleEnergy = ((averageEvisCP - cPOnFinisher) / cPPerCPG) * cpgEnergy + evisStats.getEnergyCost() - 25f * chanceOnEnergyPerCPFinisher * averageEvisCP;
            evisCount = totalEnergyAvailable / evisCycleEnergy;
            cpgCount += evisCount * ((averageEvisCP - cPOnFinisher) / cPPerCPG);
            totalEnergyAvailable = 0f;
        } else if (finisher == 2 && finisherCP > 0) {
            // Envenom
            float averageEnvenomCP = averageCP[finisherCP];
            float envenomCycleEnergy = ((averageEnvenomCP - cPOnFinisher) / cPPerCPG) * cpgEnergy + envenomStats.getEnergyCost() - 25f * chanceOnEnergyPerCPFinisher * averageEnvenomCP;
            envenomCount = totalEnergyAvailable / envenomCycleEnergy;
            cpgCount += envenomCount * ((averageEnvenomCP - cPOnFinisher) / cPPerCPG);
            totalEnergyAvailable = 0f;
        }

        // Heartpierce proc
        float HP264PPS = 0;
        float HP277PPS = 0;
        float HP264TPS = 0;
        float HP277TPS = 0;
        if ("Heartpierce".equals(mainHandStats.getWeapon().getName()))
            if (mainHandStats.getWeapon().getItemLevel() == 264)
                HP264PPS += mainHandSpeedNorm / 60 * (whiteMHAttacks * (1f - avoidedWhiteMHAttacks) + cpgCount + evisCount + envenomCount) / duration;
            else if (mainHandStats.getWeapon().getItemLevel() == 277)
                HP277PPS += mainHandSpeedNorm / 60 * (whiteMHAttacks * (1f - avoidedWhiteMHAttacks) + cpgCount + evisCount + envenomCount) / duration;
        if ("Heartpierce".equals(offHandStats.getWeapon().getName()))
            if (offHandStats.getWeapon().getItemLevel() == 264)
                HP264PPS += offHandSpeedNorm / 60 * (whiteOHAttacks * (1f - avoidedWhiteOHAttacks) + (cpgCount == 0 ? cpgCount : 0)) / duration;
            else if (offHandStats.getWeapon().getItemLevel() == 277)
                HP277PPS += offHandSpeedNorm / 60 * (whiteOHAttacks * (1f - avoidedWhiteOHAttacks) + (cpgCount == 0 ? cpgCount : 0)) / duration;
        if (HP264PPS > 0) {
            float ChanceVar264 = (float) Math.exp(-2 * HP264PPS);
            HP264TPS = HP264PPS * ChanceVar264 * (1 - (float) Math.pow(ChanceVar264, 5)) / (1 - ChanceVar264);
        }
        if (HP277PPS > 0) {
            float ChanceVar277 = (float) Math.exp(-2 * HP277PPS);
            HP277TPS = HP277PPS * ChanceVar277 * (1 - (float) Math.pow(ChanceVar277, 6)) / (1 - ChanceVar277);
        }
        totalEnergyAvailable += (HP264TPS + HP277TPS) * duration * 4;

        // Extra Energy turned into Combo Point Generators
        if (totalEnergyAvailable > 0) {
            cpgCount += totalEnergyAvailable / cpgEnergy;
            totalEnergyAvailable = 0f;
        }

        // Extra Mainhand attacks from Hack and Slash
        if (mainHandStats.getWeapon().getType() == ItemType.OneHandAxe || mainHandStats.getWeapon().getType() == ItemType.OneHandSword) {
            whiteMHAttacks += chanceOnMHAttackOnSwordAxeHit * (whiteMHAttacks * (1f - avoidedWhiteMHAttacks) + cpgCount + evisCount + envenomCount);
        }
        if (offHandStats.getWeapon().getType() == ItemType.OneHandAxe || offHandStats.getWeapon().getType() == ItemType.OneHandSword) {
            whiteMHAttacks += chanceOnMHAttackOnSwordAxeHit * (whiteOHAttacks * (1f - avoidedWhiteOHAttacks) + (cpgCount == 0 ? cpgCount : 0));
        }

        // Poisons
        float mHHitCount = whiteMHAttacks * (1f - avoidedWhiteMHAttacks) + cpgCount + evisCount + envenomCount + snDCount;
        float oHHitCount = whiteOHAttacks * (1f - avoidedWhiteOHAttacks) + (cpgCount == 0 ? cpgCount : 0);
        float iPCount = 0f;
        float dPCount = 0f;
        float wPCount = 0f;
        float aPCount = 0f;
        float iPProcRate = 0.2f * (1f + bonusIPFrequencyMultiplier) / 1.4f;
        float dPApplyChance = 0.3f + 0.04f * character.getRogueTalents().getImprovedPoisons();
        float envenomBuffTime = envenomCount * finisherCP + envenomCount;
        // MainHand Poison
        if (mHPoison == 1)
            iPCount += mHHitCount * mainHandStats.getWeapon().getSpeed() * iPProcRate * ((duration - envenomBuffTime) / duration +
                    1.75f * envenomBuffTime / duration);
        else if (mHPoison == 2 && oHPoison != 2) {
            float dPCountTemp = mHHitCount * dPApplyChance * (1f - avoidedPoisonAttacks) *
                    ((duration - envenomBuffTime) / duration + 1.15f * envenomBuffTime / duration);
            dPCountTemp -= (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * 5 + 5;
            float dPStackTime = 5f / (dPApplyChance * (1f - avoidedPoisonAttacks) * (1f - avoidedWhiteMHAttacks)) * mainHandSpeed;
            dPCount = (duration - dPStackTime - (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * dPStackTime) / 3 * 5 +
                    10 + (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * 10;
            if (oHPoison == 1)
                iPCount += dPCountTemp;
            else if (oHPoison == 3)
                wPCount += dPCountTemp;
            else if (oHPoison == 4)
                aPCount += dPCountTemp;
        } else if (mHPoison == 3)
            wPCount += mHHitCount * mainHandStats.getWeapon().getSpeed() * 21.43f / 60f;
        else if (mHPoison == 4)
            aPCount += mHHitCount * 0.5f;
        // OffHand Poison
        if (oHPoison == 1)
            iPCount += oHHitCount * offHandStats.getWeapon().getSpeed() * iPProcRate * ((duration - envenomBuffTime) / duration +
                    1.75f * envenomBuffTime / duration);
        else if (oHPoison == 2 && mHPoison != 2) {
            float dPCountTemp = oHHitCount * dPApplyChance * (1f - avoidedPoisonAttacks) *
                    ((duration - envenomBuffTime) / duration + 1.15f * envenomBuffTime / duration);
            dPCountTemp -= (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * 10 + 10;
            float dPStackTime = 5f / (dPApplyChance * (1f - avoidedPoisonAttacks) * (1f - avoidedWhiteOHAttacks)) * offHandSpeed;
            dPCount = (duration - dPStackTime - (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * dPStackTime) / 3 * 5 +
                    10 + (1f - chanceOnNoDPConsumeOnEnvenom) * envenomCount * 10;
            if (mHPoison == 1)
                iPCount += dPCountTemp;
            else if (mHPoison == 3)
                wPCount += dPCountTemp;
            else if (mHPoison == 4)
                aPCount += dPCountTemp;
        } else if (oHPoison == 3)
            wPCount += oHHitCount * offHandStats.getWeapon().getSpeed() * 21.43f / 60f;
        else if (oHPoison == 4)
            aPCount += oHHitCount * 0.5f;
        iPCount *= (1f - avoidedPoisonAttacks);
        wPCount *= (1f - avoidedPoisonAttacks);
        aPCount *= (1f - avoidedPoisonAttacks);

        // Killing Spree
        float kSAttacks = 0;
        float kSDuration = 0;
        float kSDmgBonus = 1.2f;
        if (character.getRogueTalents().getKillingSpree() > 0) {
            float kSCount = duration / (120f - (character.getRogueTalents().isGlyphOfKillingSpree() ? 45f : 0f));
            kSDuration = kSCount * 2.5f;
            kSAttacks = 5f * kSCount;
        }

        // Damage Totals
        float mainHandDamageTotal = ((duration - kSDuration) / duration * (whiteMHAttacks - 0.5f * 0.5f * stats.getMoteOfAnger() * duration) +
                kSDmgBonus * kSDuration / duration * (whiteMHAttacks - 0.5f * 0.5f * stats.getMoteOfAnger() * duration) +
                kSDmgBonus * kSAttacks) * mainHandStats.getDamagePerSwing() +
                0.5f * stats.getMoteOfAnger() * duration * 0.5f * mainHandStats.getDamagePerSwing();
        float offHandDamageTotal = ((duration - kSDuration) / duration * (whiteOHAttacks - 0.5f * 0.5f * stats.getMoteOfAnger() * duration) +
                kSDmgBonus * kSDuration / duration * (whiteOHAttacks - 0.5f * 0.5f * stats.getMoteOfAnger() * duration) +
                kSDmgBonus * kSAttacks) * offHandStats.getDamagePerSwing() +
                0.5f * stats.getMoteOfAnger() * duration * 0.5f * offHandStats.getDamagePerSwing();
        float backstabDamageTotal = (CPG == 2 ? cpgCount : 0) * backstabStats.getDamagePerSwing();
        float hemoDamageTotal = (CPG == 3 ? cpgCount : 0) * hemoStats.getDamagePerSwing();
        float sStrikeDamageTotal = (CPG == 1 ? cpgCount : 0) * sStrikeStats.getDamagePerSwing();
        float mutiDamageTotal = (CPG == 0 ? cpgCount : 0) * mutiStats.getDamagePerSwing();
        float ruptDamageTotal = ruptCount * ruptStats.getDamagePerSwing() * (ruptStats.getDurationUptime() / 16f);
        float evisDamageTotal = evisCount * (evisStats.getDamagePerSwing() + evisStats.getDamagePerSwingPerCP() * Math.min(averageCP[finisherCP], 5));
        float envenomDamageTotal = envenomCount * (envenomStats.getDamagePerSwing() + envenomStats.getDamagePerSwingPerCP() * Math.min(averageCP[finisherCP], 5));
        float instantPoisonTotal = iPCount * iPStats.getDamagePerSwing();
        float deadlyPoisonTotal = dPCount * dPStats.getDamagePerSwing();
        float woundPoisonTotal = wPCount * wPStats.getDamagePerSwing();
        float anestheticPoisonTotal = aPCount * aPStats.getDamagePerSwing();

        float damageTotal = mainHandDamageTotal + offHandDamageTotal + backstabDamageTotal + hemoDamageTotal + sStrikeDamageTotal +
                mutiDamageTotal + ruptDamageTotal + evisDamageTotal + envenomDamageTotal + instantPoisonTotal + deadlyPoisonTotal + woundPoisonTotal + anestheticPoisonTotal;
        if (character.getRogueTalents().getHungerForBlood() > 0 && !calcOpts.getBleedIsUp() && ruptCount == 0) damageTotal /= 1.05f + 0.03f * (character.getRogueTalents().isGlyphOfHungerforBlood() ? 1 : 0);

        RogueRotationCalculation ret = new RogueRotationCalculation();
        ret.dps = damageTotal / duration;
        ret.totalDamage = damageTotal;

        ret.mainHandCount = whiteMHAttacks;
        ret.offHandCount = whiteOHAttacks;
        ret.backstabCount = (CPG == 2 ? cpgCount : 0);
        ret.hemoCount = (CPG == 3 ? cpgCount : 0);
        ret.sStrikeCount = (CPG == 1 ? cpgCount : 0);
        ret.mutiCount = (CPG == 0 ? cpgCount : 0);
        ret.ruptCount = ruptCount;
        ret.evisCount = evisCount;
        ret.envenomCount = envenomCount;
        ret.snDCount = snDCount;
        ret.iPCount = iPCount;
        ret.dPCount = dPCount;
        ret.wPCount = wPCount;
        ret.aPCount = aPCount;

        ret.finisherCP = finisherCP;
        ret.evisCP = (finisher == 1 ? Math.min(averageCP[finisherCP], 5) : 0);
        ret.envenomCP = (finisher == 2 ? Math.min(averageCP[finisherCP], 5) : 0);
        ret.snDCP = snDCP;

        ret.mHPoison = mHPoison;
        ret.oHPoison = oHPoison;

        ret.useTotT = useTotT;
        ret.cutToTheChase = chanceOnSnDResetOnEnvenom;
        return ret;
    }

    public static class RogueRotationCalculation {
        public float dps;
        public float totalDamage;

        public float mainHandCount;
        public float offHandCount;
        public float backstabCount;
        public float hemoCount;
        public float sStrikeCount;
        public float mutiCount;
        public float ruptCount;
        public float evisCount;
        public float envenomCount;
        public float snDCount;
        public float iPCount;
        public float dPCount;
        public float wPCount;
        public float aPCount;

        public float finisherCP;
        public float evisCP;
        public float envenomCP;
        public int snDCP;

        public int mHPoison;
        public int oHPoison;

        public boolean useTotT;
        public float cutToTheChase;

        @Override
        public String toString() {
            StringBuilder rotation = new StringBuilder();
            if (backstabCount > 0) rotation.append("BS ");
            if (hemoCount > 0) rotation.append("He ");
            if (sStrikeCount > 0) rotation.append("SS ");
            if (mutiCount > 0) rotation.append("Mu ");
            if (ruptCount > 0) rotation.append("Ru ");
            if (evisCount > 0) rotation.append("Ev" + RogueFormat.netFloatToString(finisherCP) + " ");
            if (envenomCount > 0) rotation.append("En" + RogueFormat.netFloatToString(finisherCP) + " ");
            rotation.append("SnD" + Integer.toString(snDCP));

            if (envenomCount > 0 && cutToTheChase == 1) rotation.append("*Use " + Integer.toString(snDCP) + "cp Slice and Dice, kept up with Envenom.\r\n");
            else if (envenomCount > 0 && cutToTheChase > 0) rotation.append("*Use " + Integer.toString(snDCP) + "cp Slice and Dice, partially kept up with Envenom.\r\n");
            else rotation.append("*Keep " + Integer.toString(snDCP) + "cp Slice and Dice up.\r\n");
            if (ruptCount > 0) rotation.append("Keep 5cp Rupture up.\r\n");
            if (evisCount > 0) rotation.append("Use " + RogueFormat.netFloatToString(finisherCP) + "cp Eviscerates to spend extra combo points.\r\n");
            if (envenomCount > 0) rotation.append("Use " + RogueFormat.netFloatToString(finisherCP) + "cp Envenoms to spend extra combo points.\r\n");
            if (backstabCount > 0) rotation.append("Use Backstab for combo points.\r\n");
            else if (hemoCount > 0) rotation.append("Use Hemorrhage for combo points.\r\n");
            else if (sStrikeCount > 0) rotation.append("Use Sinister Strike for combo points.\r\n");
            else if (mutiCount > 0) rotation.append("Use Mutilate for combo points.\r\n");
            else rotation.append("Error: no CPG used, please create an issue on rawr.codeplex.com including your char file.\r\n");
            if (mHPoison == 1) rotation.append("Use Instant Poison on Mainhand.\r\n");
            else if (mHPoison == 2) rotation.append("Use Deadly Poison on Mainhand.\r\n");
            else if (mHPoison == 3) rotation.append("Use Wound Poison on Mainhand.\r\n");
            if (oHPoison == 1) rotation.append("Use Instant Poison on Offhand.\r\n");
            else if (oHPoison == 2) rotation.append("Use Deadly Poison on Offhand.\r\n");
            else if (oHPoison == 3) rotation.append("Use Wound Poison on Offhand.\r\n");
            if (useTotT) rotation.append("Use Tricks of the Trade every cooldown.");

            return rotation.toString();
        }
    }
}
