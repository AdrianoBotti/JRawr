// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBDFFFB {
    private FrBDFFFB() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB, FrBS, FFB, FFBS, DFS;
        float KFrB, KFrBS, KFFB, KFFBS, KDFS;
        cycle.name = "FrBDFFFB";

        // S00: FOF0, BF0
        // FrB => S21    fof * bf
        //        S20    fof * (1-bf)
        //        S01    (1-fof) * bf
        //        S00    (1-fof)*(1-bf)

        // S01: FOF0, BF1
        // FrB => S22    fof
        //        S02    (1-fof)

        // S02: FOF0, BF2
        // FFB => S20    fof
        //        S00    (1-fof)

        // S10: FOF1, BF0
        // FrBS => S21    X*fof * bf
        //         S20    X*fof * (1-bf)
        //         S01    X*(1-fof) * bf
        //         S00    X*(1-fof)*(1-bf)
        // FrBS-DFS => S12    (1-X)*fof*bf
        //             S10    (1-X)*fof * (1-bf)
        //             S02    (1-X)*(1-fof) * bf
        //             S00    (1-X)*(1-fof)*(1-bf)

        // S11: FOF1, BF1
        // FrBS-FFBS => S10    X*fof*(1-fof)
        //              S00    X*(1-fof)*(1-fof)
        //              S20    X*fof
        // FrBS-DFS => S12    (1-X)*fof
        //             S02    (1-X)*(1-fof)

        // S12 = S11

        // S20: FOF0, BF0
        // FrBS => S21    fof * bf
        //         S20    fof * (1-bf)
        //         S11    (1-fof) * bf
        //         S10    (1-fof)*(1-bf)

        // S21: FOF0, BF1
        // FrBS => S22    fof
        //         S12    (1-fof)

        // S22 = S21

        // S00 = (1-fof)*(1-bf) * S00 + (1-fof) * S02 + (1-fof)*(1-bf) * S10 + X*(1-fof)*(1-fof) * S11
        // S01 = (1-fof) * bf * S00 + X*(1-fof) * bf * S10
        // S02 = (1-fof) * S01 + (1-X)*(1-fof) * bf * S10 + (1-X)*(1-fof) * S11
        // S10 = (1-X)*fof * (1-bf) * S10 + X*fof*(1-fof) * S11 + (1-fof)*(1-bf) * S20
        // S11 = (1-X)*fof*bf * S10 + (1-X)*fof * S11 + (1-fof) * bf * S20 + (1-fof) * S21
        // S20 = fof * (1-bf) * S00 + fof * S02 + X*fof * (1-bf) * S10 + X*fof * S11 + fof * (1-bf) * S20
        // S21 = fof * bf * S00 + fof * S01 + X*fof * bf * S10 + fof * bf * S20 + fof * S21
        // S00 + S01 + S02 + S10 + S11 + S20 + S21 = 1

        // solved symbolically

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        FFB = castingState.getSpell(SpellId.FrostfireBoltBF);
        FFBS = castingState.getFrozenState().getSpell(SpellId.FrostfireBoltBF);
        DFS = castingState.getFrozenState().getSpell(SpellId.DeepFreeze);

        float bf = 0.05f * castingState.mageTalents.getBrainFreeze();
        float fof = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());
        float fof2 = fof * fof;
        float fof3 = fof2 * fof;
        float fof4 = fof3 * fof;
        float bf2 = bf * bf;
        float bf3 = bf2 * bf;

        // shatters until deep freeze ~ Poisson
        // share of shatters that are deep freeze = sum_i=0..inf Pi / sum_i=0..inf (i+1)*Pi = 1 / (1 + mean)

        // crude initial guess
        float X = 1.0f - 1.0f / (1.0f + (DFS.getCooldown() - DFS.castTime) / (FrB.castTime * (1 / fof + 1) + FFBS.castTime));

        float S20 = ((bf2 * fof4 + (bf - 3 * bf2) * fof3 + (2 * bf2 - 1) * fof2) * X * X + (-bf2 * fof4 + (3 * bf2 - bf) * fof3 + (-2 * bf2 - bf + 2) * fof2 + (bf - 2) * fof) * X + (bf - 1) * fof2 + (2 - bf) * fof - 1);
        float S11 = ((bf3 - bf) * fof3 + (-3 * bf3 + bf2 + 2 * bf) * fof2 + (2 * bf3 - 2 * bf) * fof) * X + (bf - bf3) * fof3 + (3 * bf3 - bf2 - 3 * bf) * fof2 + (5 * bf - 2 * bf3) * fof - 3 * bf;
        float S10 = ((fof2 - fof) * S11 * X + ((1 - bf) * fof + bf - 1) * S20) / ((bf - 1) * fof * X + (1 - bf) * fof - 1);
        float S00 = (-(bf * fof3 - 2 * bf * fof2 + bf * fof) * S10 * X - (-fof2 + 2 * fof - 1) * S11 - (-bf * fof2 + (bf + 1) * fof - 1) * S10) / (bf * fof3 - 3 * bf * fof2 + (2 * bf + 1) * fof);
        float S21 = ((bf * fof2 - 2 * bf * fof) * S10 * X - bf * fof * S20 + (bf * fof2 - 2 * bf * fof) * S00) / (fof - 1);
        float S02 = (1 - fof) * (bf * (1 - fof) * S10 * X + bf * (1 - fof) * S00) + (1 - fof) * S11 * (1 - X) + bf * (1 - fof) * S10 * (1 - X);
        float S01 = (1 - fof) * bf * S00 + X * (1 - fof) * bf * S10;

        float div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

        KFrB = (S00 + S01) / div;
        KFFB = S02 / div;
        KFrBS = (S10 + S11 + S20 + S21) / div;
        KFFBS = X * S11 / div;
        KDFS = (1 - X) * (S10 + S11) / div;

        float hasteFactor = 1.0f;

        float T = KFrB * FrB.castTime + KFFB * FFB.castTime + KFrBS * FrBS.castTime + KFFBS * FFBS.castTime + KDFS * DFS.castTime;
        float T0 = KFrBS * FrBS.castTime / 2.0f;
        float T1 = KFFBS * FFBS.castTime + KFFB * FFB.castTime;

        if (castingState.baseStats.getMage2T10() > 0) {
            // we'll make a lot of assumptions here and just assume that 2T10 haste is uniformly distributed over all
            // spells and doesn't have an impact on state space
            // also ignore the possible refresh of 2T10
            // each proc gives 12% haste for 5 sec
            // we have on average one proc every T/T1 * FFB.CastTime
            // we have some feedback loop here, speeding up the cycle increases the rate of procs
            // hastedShare = (5-FFB.CastTimeAverage) / (T/T1 * FFB.CastTimeAverage)
            // hastedCastShare = (5-FFB.CastTimeAverage) / (T/T1 * FFB.CastTime) * 1.12
            // average haste = 1 / (1-hastedCastShare*0.12/1.12)
            hasteFactor = 1.0f / (1.0f - (5 - FFB.castTime) / (T / T1 * FFB.castTime) * 0.12f);
        }

        if (fof > 0) // nothing new here if we don't have fof
        {
            // better estimate for percentage of shatter combos that are deep freeze
            X = 1.0f - 1.0f / (1.0f + (DFS.getCooldown() - DFS.castTime / hasteFactor) / (FrBS.castTime / hasteFactor * T / T0));

            // recalculate shares based on revised estimate
            S20 = ((bf2 * fof4 + (bf - 3 * bf2) * fof3 + (2 * bf2 - 1) * fof2) * X * X + (-bf2 * fof4 + (3 * bf2 - bf) * fof3 + (-2 * bf2 - bf + 2) * fof2 + (bf - 2) * fof) * X + (bf - 1) * fof2 + (2 - bf) * fof - 1);
            S11 = ((bf3 - bf) * fof3 + (-3 * bf3 + bf2 + 2 * bf) * fof2 + (2 * bf3 - 2 * bf) * fof) * X + (bf - bf3) * fof3 + (3 * bf3 - bf2 - 3 * bf) * fof2 + (5 * bf - 2 * bf3) * fof - 3 * bf;
            S10 = ((fof2 - fof) * S11 * X + ((1 - bf) * fof + bf - 1) * S20) / ((bf - 1) * fof * X + (1 - bf) * fof - 1);
            S00 = (-(bf * fof3 - 2 * bf * fof2 + bf * fof) * S10 * X - (-fof2 + 2 * fof - 1) * S11 - (-bf * fof2 + (bf + 1) * fof - 1) * S10) / (bf * fof3 - 3 * bf * fof2 + (2 * bf + 1) * fof);
            S21 = ((bf * fof2 - 2 * bf * fof) * S10 * X - bf * fof * S20 + (bf * fof2 - 2 * bf * fof) * S00) / (fof - 1);
            S02 = (1 - fof) * (bf * (1 - fof) * S10 * X + bf * (1 - fof) * S00) + (1 - fof) * S11 * (1 - X) + bf * (1 - fof) * S10 * (1 - X);
            S01 = (1 - fof) * bf * S00 + X * (1 - fof) * bf * S10;

            div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

            KFrB = (S00 + S01) / div;
            KFFB = S02 / div;
            KFrBS = (S10 + S11 + S20 + S21) / div;
            KFFBS = X * S11 / div;
            KDFS = (1 - X) * (S10 + S11) / div;
        }

        cycle.addSpell(needsDisplayCalculations, FrB, KFrB);
        cycle.addSpell(needsDisplayCalculations, FFB, KFFB);
        cycle.addSpell(needsDisplayCalculations, FrBS, KFrBS);
        cycle.addSpell(needsDisplayCalculations, FFBS, KFFBS);
        cycle.addSpell(needsDisplayCalculations, DFS, KDFS);
        cycle.castTime /= hasteFactor; // ignores latency effects, but it'll have to do for now
        cycle.calculate();
        return cycle;
    }
}
