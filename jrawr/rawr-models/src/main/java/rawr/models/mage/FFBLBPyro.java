// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FFBLBPyro {
    private FFBLBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FFB;
        Spell LB;
        Spell Pyro;
        float X;
        float K;
        cycle.name = "FFBLBPyro";

        FFB = castingState.getSpell(SpellId.FrostfireBolt);
        LB = castingState.getSpell(SpellId.LivingBomb);
        Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        float FFBcrit = FFB.critRate;
        float LBcrit = LB.critRate;
        float LBDotCrit = castingState.getFireCritRate();
        float H = castingState.mageTalents.getHotStreak() / 3.0f;
        float C;
        if (castingState.mageTalents.getPyroblast() == 0) H = 0.0f;

        float A2 = (FFBcrit - LBcrit) * (LB.castTime - FFB.castTime - 12) - (FFBcrit - LBcrit) * (FFBcrit - LBcrit) * Pyro.castTime / (1 - T8) * H;
        float A1 = (FFBcrit - LBcrit) * (12 - LB.castTime) + (LB.castTime - FFB.castTime - 12) * (1 + LBcrit) - Pyro.castTime / (1 - T8) * H * 2 * LBcrit * (FFBcrit - LBcrit);
        float A0 = (1 + LBcrit) * (12 - LB.castTime) - Pyro.castTime / (1 - T8) * H * LBcrit * LBcrit;
        if (Math.abs(A2) < 0.00001) {
            X = -A0 / A1;
        } else {
            X = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
        }
        C = LBcrit + X * (FFBcrit - LBcrit);
        K = H * C * C / (1 + C) / (1 - T8);

        float hasteFactor = 1.0f;
        if (castingState.baseStats.getMage2T10() > 0) {
            // p = K / (1 + K)
            // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
            hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FFB.castTime * X + LB.castTime * (1 - X) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
        }

        float LBrecastInterval = 12 + 0.5f * ((FFB.castTime * FFB.castTime * X + Pyro.castTime * Pyro.castTime * K) / (FFB.castTime * X + Pyro.castTime * K)) / hasteFactor;

        A2 = (FFBcrit - LBcrit) * (LB.castTime / hasteFactor - FFB.castTime / hasteFactor - LBrecastInterval) - (FFBcrit - LBcrit) * (FFBcrit - LBcrit) * Pyro.castTime / hasteFactor / (1 - T8) * H;
        A1 = (FFBcrit - LBcrit) * (LBrecastInterval - LB.castTime / hasteFactor) + (LB.castTime / hasteFactor - FFB.castTime / hasteFactor - LBrecastInterval) * (1 + LBcrit) - Pyro.castTime / hasteFactor / (1 - T8) * H * 2 * LBcrit * (FFBcrit - LBcrit);
        A0 = (1 + LBcrit) * (LBrecastInterval - LB.castTime / hasteFactor) - Pyro.castTime / hasteFactor / (1 - T8) * H * LBcrit * LBcrit;
        if (Math.abs(A2) < 0.00001) {
            X = -A0 / A1;
        } else {
            X = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
        }
        C = LBcrit + X * (FFBcrit - LBcrit);
        K = H * C * C / (1 + C) / (1 - T8);

        // pyro dot uptime

        float averageTicks = 0f;
        float k1 = 0;
        float k2 = C * C * H;
        float totalChance = k2;
        float averageCastTime = LB.castTime + X * (FFB.castTime - LB.castTime);
        int n = 2;

        averageTicks += Math.min((int) (Pyro.castTime / hasteFactor / 3.0f), 4) * T8;
        averageTicks += Math.min((int) ((Pyro.castTime + n * averageCastTime) / hasteFactor / 3.0f), 4) * (1 - T8) * k2;

        while ((Pyro.castTime + n * averageCastTime) / hasteFactor < 12) {
            float tmp = k1;
            k1 = k2;
            k2 = C * (1 - C * H) * tmp + (1 - C) * k1;
            totalChance += k2;
            n++;
            averageTicks += Math.min((int) ((Pyro.castTime + n * averageCastTime) / hasteFactor / 3.0f), 4) * (1 - T8) * k2;
        }
        averageTicks += 4 * (1 - T8) * (1 - totalChance);

        cycle.addSpell(needsDisplayCalculations, FFB, X);
        cycle.addSpell(needsDisplayCalculations, LB, 1 - X);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
