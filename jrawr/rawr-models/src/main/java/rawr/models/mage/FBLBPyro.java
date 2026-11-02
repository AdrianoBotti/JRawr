// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FBLBPyro {
    private FBLBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FB;
        Spell LB;
        Spell Pyro;
        float X;
        float K;
        float C;
        cycle.name = "FBLBPyro";

        FB = castingState.getSpell(SpellId.Fireball);
        LB = castingState.getSpell(SpellId.LivingBomb);
        Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float H = castingState.mageTalents.getHotStreak() / 3.0f;

        // 3.0.8 calcs

        // 0 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X
        //        => 1 HS charge    FBcrit * X
        // LB     => 0 HS charge    (1 - LBcrit) * (1 - X)
        //        => 1 HS charge    LBcrit * (1 - X)
        // 1 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X + (1 - H) * FBcrit * X
        // FBPyro => 0 HS charge    H * FBcrit * X
        // LB     => 0 HS charge    (1 - LBcrit) * (1 - X) + (1 - H) * LBcrit * (1 - X)
        // LBPyro => 0 HS charge    H * LBcrit * (1 - X)

        // S0 + S1 = 1
        // S0 = S0 * [(1 - FBcrit) * X + (1 - LBcrit) * (1 - X)] + S1
        // S1 = S0 * [FBcrit * X + LBcrit * (1 - X)]

        // 1 - S0 = S0 * [FBcrit * X + LBcrit * (1 - X)]
        // S0 = 1 / (1 + FBcrit * X + LBcrit * (1 - X))
        // C := FBcrit * X + LBcrit * (1 - X) = LBcrit + X * (FBcrit - LBcrit)
        // S0 = 1 / (1 + C)
        // S1 = C / (1 + C)

        // value = S0 * (X * value(FB) + (1 - X) * value(LB)) + S1 * (X * value(FB) + (1 - X) * value(LB) + H * C * value(Pyro))
        //       = X * value(FB) + (1 - X) * value(LB) + H * C * C / (1 + C) / (1 - T8) * value(Pyro)

        // time(LB) * (1 - X) / [time(FB) * X + time(LB) * (1 - X) + time(Pyro) * H * C * C / (1 + C) / (1 - T8)] = time(LB) / 12
        // 12 * (1 - X) = time(FB) * X + time(LB) * (1 - X) + time(Pyro) * H * C * C / (1 + C) / (1 - T8)

        // (1 + C) * (12 * (1 - X) - time(FB) * X - time(LB) * (1 - X)) = time(Pyro) / (1 - T8) * H * C * C
        // (1 + C) * (12 - 12 * X - time(FB) * X - time(LB) + time(LB) * X) = time(Pyro) / (1 - T8) * H * C * C
        // (1 + LBcrit + X * (FBcrit - LBcrit)) * (12 - time(LB) + X * (time(LB) - time(FB) - 12)) = time(Pyro) / (1 - T8) * H * C * C
        // [(1 + LBcrit) * (12 - time(LB)) - time(Pyro) / (1 - T8) * H * LBcrit * LBcrit] + X * [(FBcrit - LBcrit) * (12 - time(LB)) + (time(LB) - time(FB) - 12) * (1 + LBcrit) - time(Pyro) / (1 - T8) * H * 2 * LBcrit * (FBcrit - LBcrit)] + X * X * [(FBcrit - LBcrit) * (time(LB) - time(FB) - 12) - (FBcrit - LBcrit) * (FBcrit - LBcrit) * time(Pyro) / (1 - T8) * H] = 0

        // more accurate LB uptime model
        // LB is cast every 12 + half of average non LB spell duration
        // K := H * C * C / (1 + C) / (1 - T8)
        // average non lb cast time = (time(FB) * X + time(Pyro) * K) / (X + K)

        // time(LB) * (1 - X) / [time(FB) * X + time(LB) * (1 - X) + time(Pyro) * K] = time(LB) / (12 + 0.5 * ((time(FB) * X + time(Pyro) * K) / (X + K)))
        // 12 - 12 * X + 0.5 * (1 - X) * ((time(FB) * X + time(Pyro) * K) / (X + K)) = time(FB) * X + time(LB) * (1 - X) + time(Pyro) * K

        // solving this exactly is ... polynomial order 4 with very very ugly coefficients
        // so let's do an approximation instead
        // we'll use the max uptime assumption to get an approximation for K
        // then we'll compute the new X under the new uptime equation, which basically means we'll replace the 12 by the adjusted value

        float FBcrit = FB.critRate;
        float LBcrit = LB.critRate;
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

        // 2T10 doesn't affect the state space, so we can calculate X ignoring 2T10
        // then we can expand the state to include haste to get the actual 2T10 haste distribution

        float hasteFactor = 1.0f;
        if (castingState.baseStats.getMage2T10() > 0) {
            // p = K / (1 + K)
            // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
            hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FB.castTime * X + LB.castTime * (1 - X) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
        }

        // first order correction for lower LB uptime

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
        K = H * C * C / (1 + C) / (1 - T8);

        // pyro dot uptime

        float averageTicks = 0f;
        float k1 = 0;
        float k2 = C * C * H;
        float totalChance = k2;
        float averageCastTime = (LB.castTime + X * (FB.castTime - LB.castTime)) / hasteFactor;
        int n = 2;

        averageTicks += Math.min((int) (Pyro.castTime / hasteFactor / 3.0f), 4) * T8;
        averageTicks += Math.min((int) ((Pyro.castTime / hasteFactor + n * averageCastTime) / 3.0f), 4) * (1 - T8) * k2;

        while (Pyro.castTime / hasteFactor + n * averageCastTime < 12) {
            float tmp = k1;
            k1 = k2;
            k2 = C * (1 - C * H) * tmp + (1 - C) * k1;
            totalChance += k2;
            n++;
            averageTicks += Math.min((int) ((Pyro.castTime / hasteFactor + n * averageCastTime) / 3.0f), 4) * (1 - T8) * k2;
        }
        averageTicks += 4 * (1 - T8) * (1 - totalChance);

        cycle.addSpell(needsDisplayCalculations, FB, X);
        cycle.addSpell(needsDisplayCalculations, LB, 1 - X);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
