// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FBPyro {
    private FBPyro() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        float K;
        cycle.name = "FBPyro";

        Spell FB = castingState.getSpell(SpellId.Fireball);
        Spell Pyro = castingState.getSpell(SpellId.PyroblastPOMDotUptime);

        // S0: no proc, 0 count
        // FB   => S0   1-c
        //      => S1   c
        // S1: no proc, 1 count
        // FB   => S0   1-c
        //      => S2   c
        // S2: proc, 0 count
        // Pyro  => S0  (1-T8)
        //       => S2  T8

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        K = FB.critRate * FB.critRate / (1.0f + FB.critRate) * castingState.mageTalents.getHotStreak() / 3.0f / (1 - T8);
        if (castingState.mageTalents.getPyroblast() == 0) K = 0.0f;

        float hasteFactor = 1.0f;
        if (castingState.baseStats.getMage2T10() > 0) {
            // p = K / (1 + K)
            // N = (5 * 1.12 - Pyro.CastTime) * (1 + K) / (FB.CastTime * X + LB.CastTime * (1-X) + Pyro.CastTime * K)
            hasteFactor = 1.12f / ((float) Math.pow(1 - K / (1.0f + K), (5 * 1.12 - Pyro.castTime) * (1 + K) / (FB.castTime + Pyro.castTime * K) + 0.5f) * 0.12f + 1f);
        }


        // dot uptime

        // for one spell case we evaluate the dot tick uptime exactly, for others we use approximation using an average "filler" spell
        // if this turns out to be computationally expensive add it as an accuracy option

        //k0 = 0
        //k1 = 0
        //k2 = c^2 * HS
        //kn+2 = c*(1-c*HS) * kn + (1-c) * kn+1

        float averageTicks = 0f;
        float C = FB.critRate;
        float HS = castingState.mageTalents.getHotStreak() / 3.0f;
        float k1 = 0;
        float k2 = C * C * HS;
        float totalChance = k2;
        int n = 2;

        averageTicks += Math.min((int) (Pyro.castTime / hasteFactor / 3.0f), 4) * T8;
        averageTicks += Math.min((int) ((Pyro.castTime + n * FB.castTime) / hasteFactor / 3.0f), 4) * (1 - T8) * k2;

        while ((Pyro.castTime + n * FB.castTime) / hasteFactor < 12) {
            float tmp = k1;
            k1 = k2;
            k2 = C * (1 - C * HS) * tmp + (1 - C) * k1;
            totalChance += k2;
            n++;
            averageTicks += Math.min((int) ((Pyro.castTime + n * FB.castTime) / hasteFactor / 3.0f), 4) * (1 - T8) * k2;
        }
        averageTicks += 4 * (1 - T8) * (1 - totalChance);

        // ignite ticks

        cycle.addSpell(needsDisplayCalculations, FB, 1);
        cycle.addSpell(needsDisplayCalculations, Pyro, K, averageTicks / 4.0f);
        cycle.castTime /= hasteFactor;
        cycle.calculate();
        return cycle;
    }
}
