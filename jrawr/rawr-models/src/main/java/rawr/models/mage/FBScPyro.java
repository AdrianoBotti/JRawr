// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FBScPyro {
    private FBScPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FB;
        Spell Sc;
        Spell Pyro;
        float K;
        float X;
        cycle.name = "FBScPyro";
        cycle.providesScorch = true;

        FB = castingState.getSpell(SpellId.Fireball);
        Sc = castingState.getSpell(SpellId.Scorch);
        Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);
        Spell PyroSpam = castingState.getSpell(SpellId.PyroblastPOMSpammed);

        int averageScorchesNeeded = (int) Math.ceil(3f / (float) castingState.mageTalents.getImprovedScorch());
        int extraScorches = 0;

        // proportion of time casting non-scorch spells has to be less than gap := (30 - (averageScorchesNeeded + extraScorches)) / (30 - extraScorches)
        // 0 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X
        //        => 1 HS charge    FBcrit * X
        // Sc     => 0 HS charge    (1 - SCcrit) * (1 - X)
        //           1 HS charge    SCcrit * (1 - X)
        // 1 HS charge:
        // FB     => 0 HS charge    (1 - FBcrit) * X + (1 - H) * FBcrit * X
        // FBPyro => 0 HS charge    H * FBcrit * X
        // Sc     => 0 HS charge    (1 - SCcrit) * (1 - X) + (1 - H) * SCcrit * (1 - X)
        // ScPyro => 0 HS charge    H * SCcrit * (1 - X)

        // A2 * X * X + A1 * X + A0 = 0
        // X = [- A1 +/- sqrt[A1 * A1 - 4 * A2 * A0]] / (2 * A2)

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        float gap = (30.0f - (averageScorchesNeeded + extraScorches) * Sc.castTime) / (30.0f - extraScorches * Sc.castTime);
        if (castingState.mageTalents.getImprovedScorch() == 0) {
            cycle.providesScorch = false;
            gap = 1.0f;
        }
        float FBcrit = FB.critRate;
        float SCcrit = Sc.critRate;
        float H = castingState.mageTalents.getHotStreak() / 3.0f;
        if (castingState.mageTalents.getPyroblast() == 0) H = 0.0f;
        float A2 = (FBcrit - SCcrit) * FB.castTime * (1 - gap) + (FBcrit - SCcrit) * (FBcrit - SCcrit) * Pyro.castTime / (1 - T8) * H * (1 - gap) + gap * (FBcrit - SCcrit) * Sc.castTime;
        float A1 = FB.castTime * (1 - gap) * (1 + SCcrit) + Pyro.castTime / (1 - T8) * H * (2 * (FBcrit - SCcrit) * SCcrit * (1 - gap)) + Sc.castTime * gap * (1 + 2 * SCcrit - FBcrit);
        float A0 = SCcrit * SCcrit * Pyro.castTime / (1 - T8) * H * (1 - gap) - gap * Sc.castTime * (1 + SCcrit);
        if (Math.abs(A2) < 0.00001) {
            X = -A0 / A1;
        } else {
            X = (float) ((-A1 + Math.sqrt(A1 * A1 - 4 * A2 * A0)) / (2 * A2));
        }
        if (gap == 1.0f) X = 1.0f; //avoid rounding errors
        float C = X * (FBcrit - SCcrit) + SCcrit;
        K = H * C * C / (1 + C) / (1 - T8);

        float hasteFactor = 1.0f;
        if (castingState.baseStats.getMage2T10() > 0) {
            // p = K / (1 + K)
            // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
            hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FB.castTime * X + Sc.castTime * (1 - X) + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
        }


        // pyro dot uptime

        float averageTicks = 0f;
        float k1 = 0;
        float k2 = C * C * H;
        float totalChance = k2;
        float averageCastTime = X * (FB.castTime - Sc.castTime) + Sc.castTime;
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
        cycle.addSpell(needsDisplayCalculations, Sc, 1 - X);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
