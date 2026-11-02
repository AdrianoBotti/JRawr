// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FBScLBPyro {
    private FBScLBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FB;
        Spell Sc;
        Spell LB;
        Spell Pyro;
        float K;
        float X;
        float Y;
        cycle.name = "FBScLBPyro";
        cycle.providesScorch = true;

        FB = castingState.getSpell(SpellId.Fireball);
        Sc = castingState.getSpell(SpellId.Scorch);
        LB = castingState.getSpell(SpellId.LivingBomb);
        Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        int averageScorchesNeeded = (int) Math.ceil(3f / (float) castingState.mageTalents.getImprovedScorch());
        int extraScorches = 0;

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float C, H, averageCastTime;

        // 3.1 calculations

        // 0 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X
        //        => 1 HS charge    FBcrit * X
        // LB     => 0 HS charge    (1 - LBcrit) * (1 - X - Y)
        //        => 1 HS charge    LBcrit * (1 - X - Y)
        // Sc     => 0 HS charge    (1 - SCcrit) * Y
        //           1 HS charge    SCcrit * Y
        // 1 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X + (1 - H) * FBcrit * X
        // FBPyro => 0 HS charge    H * FBcrit * X
        // LB     => 0 HS charge    (1 - LBcrit) * (1 - X - Y) + (1 - H) * LBcrit * (1 - X - Y)
        // LBPyro => 0 HS charge    H * LBcrit * (1 - X - Y)
        // Sc     => 0 HS charge    (1 - SCcrit) * Y + (1 - H) * SCcrit * Y
        // ScPyro => 0 HS charge    H * SCcrit * Y

        // S0 + S1 = 1
        // C := FBcrit * X + SCcrit * Y + LBcrit * (1 - X - Y)
        // S0 = S0 * (1 - C) + S1
        // S1 = S0 * C
        // 1 - S0 = S0 * C
        // S0 = 1 / (1 + C)
        // S1 = C / (1 + C)

        float gap = (30.0f - (averageScorchesNeeded + extraScorches) * Sc.castTime) / (30.0f - extraScorches * Sc.castTime);
        if (castingState.mageTalents.getImprovedScorch() == 0) {
            cycle.providesScorch = false;
            gap = 1.0f;
        }
        float hasteFactor = 1.0f;

        if (gap == 1.0f) {
            Y = 0.0f;
            float FBcrit = FB.critRate;
            float LBcrit = LB.critRate;
            H = castingState.mageTalents.getHotStreak() / 3.0f;
            if (castingState.mageTalents.getPyroblast() == 0) H = 0.0f;
            float A2 = (FBcrit - LBcrit) * (LB.castTime - FB.castTime - 12) - (FBcrit - LBcrit) * (FBcrit - LBcrit) * Pyro.castTime / (1 - T8) * H;
            float A1 = (FBcrit - LBcrit) * (12 - LB.castTime) + (LB.castTime - FB.castTime - 12) * (1 + LBcrit) - Pyro.castTime / (1 - T8) * H * 2 * LBcrit * (FBcrit - LBcrit);
            float A0 = (1 + LBcrit) * (12 - LB.castTime) - Pyro.castTime / (1 - T8) * H * LBcrit * LBcrit;
            if (Math.abs(A2) < 0.00001) {
                X = -A0 / A1;
            } else {
                X = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            C = LBcrit + X * (FBcrit - LBcrit);
            K = H * C * C / (1 + C) / (1 - T8);

            // first order correction for lower LB uptime
            if (castingState.baseStats.getMage2T10() > 0) {
                // p = K / (1 + K)
                // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
                hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FB.castTime * X + LB.castTime * (1 - X) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
            }

            // LBrecastInterval = 12 + 0.5 * ((time(FB) * X + time(Pyro) * K) / (X + K))
            float LBrecastInterval = 12 + 0.5f * ((FB.castTime * FB.castTime * X + Pyro.castTime * Pyro.castTime * K) / (FB.castTime * X + Pyro.castTime * K)) / hasteFactor;

            // now recalculate the spell distribution under the new uptime assumption
            A2 = (FBcrit - LBcrit) * (LB.castTime / hasteFactor - FB.castTime / hasteFactor - LBrecastInterval) - (FBcrit - LBcrit) * (FBcrit - LBcrit) * Pyro.castTime / hasteFactor / (1 - T8) * H;
            A1 = (FBcrit - LBcrit) * (LBrecastInterval - LB.castTime / hasteFactor) + (LB.castTime / hasteFactor - FB.castTime / hasteFactor - LBrecastInterval) * (1 + LBcrit) - Pyro.castTime / hasteFactor / (1 - T8) * H * 2 * LBcrit * (FBcrit - LBcrit);
            A0 = (1 + LBcrit) * (LBrecastInterval - LB.castTime / hasteFactor) - Pyro.castTime / hasteFactor / (1 - T8) * H * LBcrit * LBcrit;
            if (Math.abs(A2) < 0.00001) {
                X = -A0 / A1;
            } else {
                X = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            C = LBcrit + X * (FBcrit - LBcrit);
            averageCastTime = LB.castTime + X * (FB.castTime - LB.castTime);
            K = H * C * C / (1 + C) / (1 - T8);
        } else {
            float P = 1.0f + Sc.castTime / (12.0f * (1.0f - gap));
            float FBcrit = FB.critRate;
            float SCcrit = Sc.critRate;
            float LBcrit = LB.critRate;
            H = castingState.mageTalents.getHotStreak() / 3.0f;
            if (castingState.mageTalents.getPyroblast() == 0) H = 0.0f;
            float T1 = Sc.castTime - P * FB.castTime + (P - 1) * LB.castTime - 12 * (P - 1);
            float CY = SCcrit - FBcrit * P + LBcrit * (P - 1);

            float A2 = CY * T1 + Pyro.castTime / (1 - T8) * H * CY * CY;
            float A1 = CY * FB.castTime + T1 + FBcrit * T1 + 2 * Pyro.castTime / (1 - T8) * H * FBcrit * CY;
            float A0 = FB.castTime + FBcrit * FB.castTime + Pyro.castTime / (1 - T8) * H * FBcrit * FBcrit;
            if (Math.abs(A2) < 0.00001) {
                Y = -A0 / A1;
            } else {
                Y = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            X = 1 - P * Y;
            C = (FBcrit * X + SCcrit * Y + LBcrit * (1 - X - Y));
            K = H * C * C / (1 + C) / (1 - T8);

            if (castingState.baseStats.getMage2T10() > 0) {
                // p = K / (1 + K)
                // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
                hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FB.castTime * X + Sc.castTime * Y + LB.castTime * (1 - X - Y) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
            }

            float LBrecastInterval = 12 + 0.5f * ((FB.castTime * FB.castTime * X + Sc.castTime * Sc.castTime * Y + Pyro.castTime * Pyro.castTime * K) / (FB.castTime * X + Sc.castTime * Y + Pyro.castTime * K)) / hasteFactor;

            P = 1.0f + Sc.castTime / hasteFactor / (LBrecastInterval * (1.0f - gap));
            T1 = Sc.castTime / hasteFactor - P * FB.castTime / hasteFactor + (P - 1) * LB.castTime / hasteFactor - LBrecastInterval * (P - 1);
            CY = SCcrit - FBcrit * P + LBcrit * (P - 1);

            A2 = CY * T1 + Pyro.castTime / hasteFactor / (1 - T8) * H * CY * CY;
            A1 = CY * FB.castTime / hasteFactor + T1 + FBcrit * T1 + 2 * Pyro.castTime / hasteFactor / (1 - T8) * H * FBcrit * CY;
            A0 = FB.castTime / hasteFactor + FBcrit * FB.castTime / hasteFactor + Pyro.castTime / hasteFactor / (1 - T8) * H * FBcrit * FBcrit;
            if (Math.abs(A2) < 0.00001) {
                Y = -A0 / A1;
            } else {
                Y = (float) ((-A1 - Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
            }
            X = 1 - P * Y;
            C = (FBcrit * X + SCcrit * Y + LBcrit * (1 - X - Y));
            averageCastTime = (FB.castTime * X + Sc.castTime * Y + LB.castTime * (1 - X - Y));
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

        cycle.addSpell(needsDisplayCalculations, FB, X);
        cycle.addSpell(needsDisplayCalculations, Sc, Y);
        cycle.addSpell(needsDisplayCalculations, LB, 1 - X - Y);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
