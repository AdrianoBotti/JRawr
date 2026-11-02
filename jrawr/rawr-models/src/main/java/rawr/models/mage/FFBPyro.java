// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FFBPyro {
    private FFBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FFB;
        float K;
        cycle.name = "FFBPyro";

        FFB = castingState.getSpell(SpellId.FrostfireBoltFOF);
        Spell Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        // no Pyro
        // FB => no Pyro 1 - c*c/(1+c)
        //    => Pyro c*c/(1+c)
        // Pyro
        // Pyro => no Pyro

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        K = FFB.critRate * FFB.critRate / (1.0f + FFB.critRate) * castingState.mageTalents.getHotStreak() / 3.0f / (1 - T8);
        if (castingState.mageTalents.getPyroblast() == 0) K = 0.0f;

        float hasteFactor = 1.0f;
        if (castingState.baseStats.getMage2T10() > 0) {
            // p = K / (1 + K)
            // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
            hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FFB.castTime + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
        }

        // pyro dot uptime

        float averageTicks = 0f;
        float C = FFB.critRate;
        float H = castingState.mageTalents.getHotStreak() / 3.0f;
        float k1 = 0;
        float k2 = C * C * H;
        float totalChance = k2;
        float averageCastTime = FFB.castTime;
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

        cycle.addSpell(needsDisplayCalculations, FFB, 1);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
