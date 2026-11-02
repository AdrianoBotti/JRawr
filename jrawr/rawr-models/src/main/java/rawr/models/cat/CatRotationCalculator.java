// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Cat/CatRotationCalculator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Arithmetic order and float widths kept exactly; commented-out C# members are omitted.
package rawr.models.cat;

import rawr.base.stats.Stats;
public class CatRotationCalculator {
    public Stats stats;
    public float duration;
    public float cpPerCPG;
    public boolean maintainMangle;
    public boolean glyphOfShred;
    public float attackSpeed;
    public float chanceExtraCPPerHit;
    public boolean omenOfClarity;
    public float avoidedAttacks;
    public float cpgEnergyCostMultiplier;
    public float clearcastOnBleedChance;
    public float berserkDuration;

    public CatAbilityStats meleeStats;
    public CatAbilityStats mangleStats;
    public CatAbilityStats shredStats;
    public CatAbilityStats rakeStats;
    public CatAbilityStats ripStats;
    public CatAbilityStats biteStats;
    public CatAbilityStats roarStats;

    private final float[] chanceExtraCP = new float[5];

    public CatRotationCalculator(Stats stats, float duration, float cpPerCPG, boolean maintainMangle,
            float berserkDuration, float attackSpeed, boolean omenOfClarity, boolean glyphOfShred, float avoidedAttacks,
            float chanceExtraCPPerHit, float cpgEnergyCostMultiplier, float clearcastOnBleedChance,
            CatAbilityStats meleeStats, CatAbilityStats mangleStats, CatAbilityStats shredStats, CatAbilityStats rakeStats,
            CatAbilityStats ripStats, CatAbilityStats biteStats, CatAbilityStats roarStats) {
        this.stats = stats;
        this.duration = duration;
        this.cpPerCPG = cpPerCPG;
        this.maintainMangle = maintainMangle;
        this.attackSpeed = attackSpeed;
        this.omenOfClarity = omenOfClarity;
        this.glyphOfShred = glyphOfShred;
        this.avoidedAttacks = avoidedAttacks;
        this.chanceExtraCPPerHit = chanceExtraCPPerHit;
        this.cpgEnergyCostMultiplier = cpgEnergyCostMultiplier;
        this.clearcastOnBleedChance = clearcastOnBleedChance;
        this.berserkDuration = berserkDuration;

        this.meleeStats = meleeStats;
        this.mangleStats = mangleStats;
        this.shredStats = shredStats;
        this.rakeStats = rakeStats;
        this.ripStats = ripStats;
        this.biteStats = biteStats;
        this.roarStats = roarStats;

        float c = chanceExtraCPPerHit, h = (1f - chanceExtraCPPerHit);
        this.chanceExtraCP[0] = c;
        this.chanceExtraCP[1] = c * h;
        this.chanceExtraCP[2] = c * c + c * h * h;
        this.chanceExtraCP[3] = 2 * c * c * h + c * h * h * h;
        this.chanceExtraCP[4] = c * c * c + 3 * c * c * h * h + c * h * h * h * h;
    }

    public CatRotationCalculation getRotationCalculations(boolean useRake, boolean useShred, boolean useRip, int biteCP, int roarCP) {
        float totalEnergyAvailable = 100f + (10f * duration);
        totalEnergyAvailable += ((float) Math.ceil((duration - 10f) / (30f - stats.getTigersFuryCooldownReduction())) * stats.getBonusEnergyOnTigersFury());
        if (berserkDuration > 0)
            totalEnergyAvailable += (float) Math.ceil((duration - 10f) / 180f) * (berserkDuration + 7f) * 10f; //Assume 70 energy when you activate Berserk
        if (omenOfClarity) {
            float oocProcs = ((3.5f * (duration / 60f)) / attackSpeed) * (1f - avoidedAttacks); //Counts all OOCs as being used on the CPG. Should be made more accurate than that, but that's close at least
            if (clearcastOnBleedChance > 0) {
                float dotTicks = (1f / 3f + 1f / 2f) * duration;
                oocProcs += dotTicks * clearcastOnBleedChance;
            }
            float cpgEnergyRaw = (useShred ? shredStats.energyCost : mangleStats.energyCost) / cpgEnergyCostMultiplier;
            totalEnergyAvailable += oocProcs * (cpgEnergyRaw * (1f - avoidedAttacks) + cpgEnergyRaw * avoidedAttacks * 0.2f);
        }

        float totalCPAvailable = 0f;
        float averageGCD = 1f / (1f - avoidedAttacks);
        float ripDurationUptime = ripStats.durationUptime + (glyphOfShred && useShred ? 6f : 0f);
        float ripDurationAverage = ripStats.durationAverage + (glyphOfShred && useShred ? 6f : 0f);
        float averageFinisherCP = 5f + this.chanceExtraCP[4];

        // region Melee
        float meleeCount = duration / attackSpeed;
        // endregion

        // region Rake
        float rakeCount = 0;
        float rakeTotalEnergy = 0;
        float rakeCP = 0;
        if (useRake) {
            //When maintaining Mangle, lose 2 GCDs at the start of the fight to Mangle, Roar.
            float durationRakeable = duration - (maintainMangle ? 2f * averageGCD : 0f);
            //Lose some time due to Rip/Rake conflicts
            float rakeRipConflict = (1f / ripDurationAverage) * 0.5f * averageGCD;

            rakeCount = durationRakeable / (rakeStats.durationAverage + rakeRipConflict);
            rakeTotalEnergy = rakeCount * rakeStats.energyCost;
            rakeCP = rakeCount * cpPerCPG;
            totalCPAvailable += rakeCP;
            totalEnergyAvailable -= rakeTotalEnergy;
        }
        // endregion

        // region Mangle
        float mangleCount = 0f;
        float mangleTotalEnergy = 0f;
        float mangleCP = 0f;
        if (maintainMangle) {
            //Lose some time due to Mangle/Rake and Mangle/Rip conflicts
            float mangleRakeConflict = (1f / rakeStats.durationAverage) * 0.5f * averageGCD;
            float mangleRipConflict = (1f / ripDurationAverage) * 0.5f * averageGCD;

            mangleCount = duration / (mangleStats.durationAverage - mangleRakeConflict - mangleRipConflict);
            mangleTotalEnergy = mangleCount * mangleStats.energyCost;
            mangleCP = mangleCount * cpPerCPG;
            totalCPAvailable += mangleCP;
            totalEnergyAvailable -= mangleTotalEnergy;
        }
        // endregion

        // region Combo Point Generator
        float cpgCount = 0f;
        float cpgEnergy = useShred ? shredStats.energyCost : mangleStats.energyCost;
        float shredCount = 0f;
        // endregion

        // region Savage Roar
        float averageRoarCP = ((float) roarCP + 1f) * this.chanceExtraCP[roarCP - 1]
                + ((float) roarCP) * (1f - this.chanceExtraCP[roarCP - 1]);

        //Lose some time due to Roar/Rake, Roar/Mangle, and Roar/Rip conflicts
        float roarRakeConflict = (1f / rakeStats.durationAverage) * 0.5f * averageGCD;
        float roarMangleConflict = (1f / mangleStats.durationAverage) * 0.5f * averageGCD;
        float roarRipConflict = (1f / ripDurationAverage) * 0.5f * (averageGCD * averageFinisherCP / cpPerCPG);

        float roarDuration = roarStats.durationAverage + 5f * Math.min(5f, averageRoarCP)
                - roarRakeConflict - roarMangleConflict - roarRipConflict;
        float roarCount = duration / roarDuration;
        float roarTotalEnergy = roarCount * roarStats.energyCost;
        float roarCPRequired = roarCount * averageRoarCP;
        if (totalCPAvailable < roarCPRequired) {
            float cpToGenerate = roarCPRequired - totalCPAvailable;
            float cpgToUse = cpToGenerate / cpPerCPG;
            cpgCount += cpgToUse;
            totalEnergyAvailable -= cpgToUse * cpgEnergy;
            totalCPAvailable += cpToGenerate;
        }
        totalCPAvailable -= roarCPRequired;
        totalEnergyAvailable -= roarTotalEnergy;
        // endregion

        // region Damage Finishers
        float ripCount = 0f;
        float biteCount = 0f;
        if (useRip) {
            // region Rip
            //Lose GCDs at the start of the fight to Mangle/Rake, Roar, and enough CPGs to get 5CPG.
            float durationRipable = duration - 2f * averageGCD - (averageGCD * (averageFinisherCP / cpPerCPG));

            float ripCountMax = durationRipable / ripDurationAverage;
            float ripsFromAvailableCP = Math.min(ripCountMax, totalCPAvailable / averageFinisherCP);
            ripCount += ripsFromAvailableCP;
            totalCPAvailable -= averageFinisherCP * ripsFromAvailableCP;
            totalEnergyAvailable -= ripStats.energyCost * ripsFromAvailableCP;

            float ripCycleEnergy = (averageFinisherCP / cpPerCPG) * cpgEnergy + ripStats.energyCost;
            float ripsFromNewCP = Math.min(ripCountMax - ripsFromAvailableCP, totalEnergyAvailable / ripCycleEnergy);

            ripCount += ripsFromNewCP;
            cpgCount += (averageFinisherCP / cpPerCPG) * ripsFromNewCP;
            totalEnergyAvailable -= ripCycleEnergy * ripsFromNewCP;
            // endregion
        }
        if (biteCP > 0) {
            // region Ferocious Bite
            float averageBiteCP = ((float) biteCP + 1f) * this.chanceExtraCP[biteCP - 1]
                    + ((float) biteCP) * (1f - this.chanceExtraCP[biteCP - 1]);
            float bitesFromAvailableCP = totalCPAvailable / averageBiteCP;
            biteCount += bitesFromAvailableCP;
            totalCPAvailable = 0;
            totalEnergyAvailable -= biteStats.energyCost * bitesFromAvailableCP;

            float biteCycleEnergy = (averageBiteCP / cpPerCPG) * cpgEnergy + biteStats.energyCost;
            float bitesFromNewCP = totalEnergyAvailable / biteCycleEnergy;

            biteCount += bitesFromNewCP;
            cpgCount += bitesFromNewCP * (averageBiteCP / cpPerCPG);
            totalEnergyAvailable = 0f;
            // endregion
        }
        // endregion

        // region Extra Energy turned into Combo Point Generators
        if (totalEnergyAvailable > 0) {
            cpgCount += totalEnergyAvailable / cpgEnergy;
            totalEnergyAvailable = 0f;
        }
        // endregion

        // region Damage Totals
        if (useShred) shredCount += cpgCount;
        else mangleCount += cpgCount;

        float meleeDamageTotal = meleeCount * meleeStats.damagePerSwing;
        float mangleDamageTotal = mangleCount * mangleStats.damagePerSwing;
        float rakeDamageTotal = rakeCount * rakeStats.damagePerSwing;
        float shredDamageTotal = shredCount * shredStats.damagePerSwing;
        float ripDamageTotal = ripCount * ripStats.damagePerSwing * (ripDurationUptime / 12f);
        float biteDamageTotal = biteCount * (biteStats.damagePerSwing + biteStats.damagePerSwingPerCP * biteCP);

        float damageTotal = meleeDamageTotal + mangleDamageTotal + rakeDamageTotal + shredDamageTotal + ripDamageTotal + biteDamageTotal;
        // endregion

        CatRotationCalculation calculation = new CatRotationCalculation();
        calculation.dps = damageTotal / duration;
        calculation.totalDamage = damageTotal;

        calculation.meleeCount = meleeCount;
        calculation.mangleCount = mangleCount;
        calculation.rakeCount = rakeCount;
        calculation.shredCount = shredCount;
        calculation.ripCount = ripCount;
        calculation.biteCount = biteCount;
        calculation.roarCount = roarCount;

        calculation.roarCP = roarCP;
        calculation.biteCP = biteCP;
        return calculation;
    }

    public static class CatRotationCalculation {
        public float dps;
        public float totalDamage;

        public float meleeCount;
        public float mangleCount;
        public float shredCount;
        public float rakeCount;
        public float ripCount;
        public float biteCount;
        public float roarCount;

        public int roarCP;
        public int biteCP;

        @Override
        public String toString() {
            StringBuilder rotation = new StringBuilder();
            if (mangleCount > 0) rotation.append("Ma ");
            if (rakeCount > 0) rotation.append("Ra ");
            if (shredCount > 0) rotation.append("Sh ");
            if (ripCount > 0) rotation.append("Ri ");
            if (biteCount > 0) rotation.append("FB").append(biteCP).append(' ');
            rotation.append("Ro").append(roarCP);

            rotation.append("*Keep ").append(roarCP).append("cp Savage Roar up.\r\n");
            if (mangleCount > 0) rotation.append("Keep Mangle up.\r\n");
            if (rakeCount > 0) rotation.append("Keep Rake up.\r\n");
            if (ripCount > 0) rotation.append("Keep 5cp Rip up.\r\n");
            if (biteCount > 0) rotation.append("Use ").append(biteCP).append("cp Ferocious Bites to spend extra combo points.\r\n");
            if (shredCount > 0) rotation.append("Use Shred for combo points.");
            else rotation.append("Use Mangle for combo points.");

            return rotation.toString();
        }
    }
}
