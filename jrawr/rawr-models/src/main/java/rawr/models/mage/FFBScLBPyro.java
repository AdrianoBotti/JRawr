// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FFBScLBPyro {
    private FFBScLBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FFB;
        Spell Sc;
        Spell LB;
        Spell Pyro;
        float K;
        float X;
        float Y;
        cycle.name = "FFBScLBPyro";
        cycle.providesScorch = true;

        FFB = castingState.getSpell(SpellId.FrostfireBolt);
        Sc = castingState.getSpell(SpellId.Scorch);
        LB = castingState.getSpell(SpellId.LivingBomb);
        Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        int averageScorchesNeeded = (int) Math.ceil(3f / (float) castingState.mageTalents.getImprovedScorch());
        int extraScorches = 0;

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        float gap = (30.0f - (averageScorchesNeeded + extraScorches) * Sc.castTime) / (30.0f - extraScorches * Sc.castTime);
        if (castingState.mageTalents.getImprovedScorch() == 0) {
            cycle.providesScorch = false;
            gap = 1.0f;
        }
        float hasteFactor = 1.0f;
        float C, H, averageCastTime;

        if (gap == 1.0f) {
            Y = 0.0f;
            float FFBcrit = FFB.critRate;
            float LBcrit = LB.critRate;
            H = castingState.mageTalents.getHotStreak() / 3.0f;
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
            averageCastTime = LB.castTime + X * (FFB.castTime - LB.castTime);
            K = H * C * C / (1 + C) / (1 - T8);
        } else {
            float P = 1.0f + Sc.castTime / (12.0f * (1.0f - gap));
            float FFBcrit = FFB.critRate;
            float SCcrit = Sc.critRate;
            float LBcrit = LB.critRate;
            H = castingState.mageTalents.getHotStreak() / 3.0f;
            if (castingState.mageTalents.getPyroblast() == 0) H = 0.0f;
            float T1 = Sc.castTime - P * FFB.castTime + (P - 1) * LB.castTime - 12 * (P - 1);
            float CY = SCcrit - FFBcrit * P + LBcrit * (P - 1);

            float A2 = CY * T1 + Pyro.castTime / (1 - T8) * H * CY * CY;
            float A1 = CY * FFB.castTime + T1 + FFBcrit * T1 + 2 * Pyro.castTime / (1 - T8) * H * FFBcrit * CY;
            float A0 = FFB.castTime + FFBcrit * FFB.castTime + Pyro.castTime / (1 - T8) * H * FFBcrit * FFBcrit;
            if (Math.abs(A2) < 0.00001) {
                Y = -A0 / A1;
            } else {
                Y = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            X = 1 - P * Y;
            C = (FFBcrit * X + SCcrit * Y + LBcrit * (1 - X - Y));
            K = H * C * C / (1 + C) / (1 - T8);

            if (castingState.baseStats.getMage2T10() > 0) {
                // p = K / (1 + K)
                // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
                hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FFB.castTime * X + Sc.castTime * Y + LB.castTime * (1 - X - Y) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
            }

            float LBrecastInterval = 12 + 0.5f * ((FFB.castTime * FFB.castTime * X + Sc.castTime * Sc.castTime * Y + Pyro.castTime * Pyro.castTime * K) / (FFB.castTime * X + Sc.castTime * Y + Pyro.castTime * K)) / hasteFactor;

            P = 1.0f + Sc.castTime / hasteFactor / (LBrecastInterval * (1.0f - gap));
            T1 = Sc.castTime / hasteFactor - P * FFB.castTime / hasteFactor + (P - 1) * LB.castTime / hasteFactor - LBrecastInterval * (P - 1);
            CY = SCcrit - FFBcrit * P + LBcrit * (P - 1);

            A2 = CY * T1 + Pyro.castTime / hasteFactor / (1 - T8) * H * CY * CY;
            A1 = CY * FFB.castTime / hasteFactor + T1 + FFBcrit * T1 + 2 * Pyro.castTime / hasteFactor / (1 - T8) * H * FFBcrit * CY;
            A0 = FFB.castTime / hasteFactor + FFBcrit * FFB.castTime / hasteFactor + Pyro.castTime / hasteFactor / (1 - T8) * H * FFBcrit * FFBcrit;
            if (Math.abs(A2) < 0.00001) {
                Y = -A0 / A1;
            } else {
                Y = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            X = 1 - P * Y;
            C = (FFBcrit * X + SCcrit * Y + LBcrit * (1 - X - Y));
            averageCastTime = (FFB.castTime * X + Sc.castTime * Y + LB.castTime * (1 - X - Y));
            K = H * C * C / (1 + C) / (1 - T8);
        }

        // pyro dot uptime

        float averageTicks = 0f;
        float k1 = 0;
        float k2 = C * C * H;
        float totalChance = k2;
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
        cycle.addSpell(needsDisplayCalculations, Sc, Y);
        cycle.addSpell(needsDisplayCalculations, LB, 1 - X - Y);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
