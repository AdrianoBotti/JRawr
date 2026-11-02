// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBDFFBIL {
    private FrBDFFBIL() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB, FrBS, FB, FBS, ILS, DFS;
        float KFrB, KFrBS, KFB, KFBS, KILS, KDFS;
        cycle.name = "FrBDFFBIL";

        // S00: FOF0, BF0
        // FrB => S21    fof * bf
        //        S20    fof * (1-bf)
        //        S01    (1-fof) * bf
        //        S00    (1-fof)*(1-bf)

        // S01: FOF0, BF1
        // FrB => S22    fof
        //        S02    (1-fof)

        // S02: FOF0, BF2
        // FB => S00    1

        // S10: FOF1, BF0
        // FrBS-ILS => S12    X*fof * bf
        //             S10    X*fof * (1-bf)
        //             S02    X*(1-fof) * bf
        //             S00    X*(1-fof)*(1-bf)
        // FrBS-DFS => S12    (1-X)*fof*bf
        //             S10    (1-X)*fof * (1-bf)
        //             S02    (1-X)*(1-fof) * bf
        //             S00    (1-X)*(1-fof)*(1-bf)

        // S11: FOF1, BF1
        // FrBS-FBS => S10    X*fof
        //             S00    X*(1-fof)
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

        // S00 = (1-fof)*(1-bf) * S00 + S02 + (1-fof)*(1-bf) * S10 + X*(1-fof) * S11
        // S01 = (1-fof) * bf * S00
        // S02 = (1-fof) * S01 + (1-fof) * bf * S10 + (1-X)*(1-fof) * S11
        // S10 = fof * (1-bf) * S10 + X*fof * S11 + (1-fof)*(1-bf) * S20
        // S11 = fof * bf * S10 + (1-X)*fof * S11 + (1-fof) * bf * S20 + (1-fof) * S21
        // S20 = fof * (1-bf) * S00 + fof * (1-bf) * S20
        // S21 = fof * bf * S00 + fof * S01 + fof * bf * S20 + fof * S21
        // S00 + S01 + S02 + S10 + S11 + S20 + S21 = 1

        // solved symbolically

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        FB = castingState.getSpell(SpellId.FireballBF);
        FBS = castingState.getFrozenState().getSpell(SpellId.FireballBF);
        ILS = castingState.getFrozenState().getSpell(SpellId.IceLance);
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
        float X = 1.0f - 1.0f / (1.0f + (DFS.getCooldown() - DFS.castTime) / (FrB.castTime * (1 / fof + 1) + ILS.castTime));

        float S00 = (((bf - 1) * fof3 + (2 - bf) * fof2 - fof) * X + (bf2 - 2 * bf + 1) * fof3 + (-bf2 + 4 * bf - 3) * fof2 + (3 - 2 * bf) * fof - 1);
        float S01 = -(((bf2 - bf) * fof4 + (3 * bf - 2 * bf2) * fof3 + (bf2 - 3 * bf) * fof2 + bf * fof) * X + (bf3 - 2 * bf2 + bf) * fof4 + (-2 * bf3 + 6 * bf2 - 4 * bf) * fof3 + (bf3 - 6 * bf2 + 6 * bf) * fof2 + (2 * bf2 - 4 * bf) * fof + bf);
        float S02 = (((bf2 - bf) * fof4 + (4 * bf - 3 * bf2) * fof3 + (3 * bf2 - 5 * bf) * fof2 + (2 * bf - bf2) * fof) * X + (-bf3 + 2 * bf2 - bf) * fof3 + (2 * bf3 - 3 * bf2 + bf) * fof2 + (-bf3 + bf2 + bf) * fof - bf);
        float S10 = (((bf2 - bf) * fof4 + (-bf2 + bf + 1) * fof3 + (-bf - 1) * fof2) * X + (-bf2 + 2 * bf - 1) * fof3 + (2 * bf2 - 4 * bf + 2) * fof2 + (-bf2 + 2 * bf - 1) * fof);
        float S11 = ((bf3 - 2 * bf2 + bf) * fof4 + (-bf3 + 4 * bf2 - 3 * bf) * fof3 + (5 * bf - 4 * bf2) * fof2 + (bf2 - 3 * bf) * fof);
        float S20 = -(((bf - 1) * fof3 + (1 - bf) * fof2) * X + (bf2 - 2 * bf + 1) * fof3 + (-bf2 + 3 * bf - 2) * fof2 + (1 - bf) * fof);
        float S21 = (((bf2 - bf) * fof4 + (2 * bf - bf2) * fof3 - 2 * bf * fof2) * X + (bf3 - 2 * bf2 + bf) * fof4 + (-bf3 + 4 * bf2 - 3 * bf) * fof3 + (4 * bf - 3 * bf2) * fof2 - 2 * bf * fof);

        float div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

        KFrB = (S00 + S01) / div;
        KFB = S02 / div;
        KFrBS = (S10 + S11 + S20 + S21) / div;
        KFBS = X * S11 / div;
        KILS = X * S10 / div;
        KDFS = (1 - X) * (S10 + S11) / div;

        float hasteFactor = 1.0f;

        float T = KFrB * FrB.castTime + KFB * FB.castTime + KFrBS * FrBS.castTime + KFBS * FBS.castTime + KILS * ILS.castTime + KDFS * DFS.castTime;
        float T0 = KFBS * FBS.castTime + KILS * ILS.castTime + KDFS * DFS.castTime;
        float T1 = KFBS * FBS.castTime + KFB * FB.castTime;

        if (castingState.baseStats.getMage2T10() > 0) {
            // we'll make a lot of assumptions here and just assume that 2T10 haste is uniformly distributed over all
            // spells and doesn't have an impact on state space
            // also ignore the possible refresh of 2T10
            // each proc gives 12% haste for 5 sec
            // we have on average one proc every T/T1 * FB.CastTime
            // we have some feedback loop here, speeding up the cycle increases the rate of procs
            // hastedShare = (5-FB.CastTimeAverage) / (T/T1 * FB.CastTimeAverage)
            // hastedCastShare = (5-FB.CastTimeAverage) / (T/T1 * FB.CastTime) * 1.12
            // average haste = 1 / (1-hastedCastShare*0.12/1.12)
            hasteFactor = 1.0f / (1.0f - (5 - FB.castTime) / (T / T1 * FB.castTime) * 0.12f);
        }

        if (fof > 0) // nothing new here if we don't have fof
        {
            // better estimate for percentage of shatter combos that are deep freeze
            X = 1.0f - 1.0f / (1.0f + (DFS.getCooldown() - DFS.castTime / hasteFactor) / (DFS.castTime / hasteFactor * T / T0));

            // recalculate shares based on revised estimate
            S00 = (((bf - 1) * fof3 + (2 - bf) * fof2 - fof) * X + (bf2 - 2 * bf + 1) * fof3 + (-bf2 + 4 * bf - 3) * fof2 + (3 - 2 * bf) * fof - 1);
            S01 = -(((bf2 - bf) * fof4 + (3 * bf - 2 * bf2) * fof3 + (bf2 - 3 * bf) * fof2 + bf * fof) * X + (bf3 - 2 * bf2 + bf) * fof4 + (-2 * bf3 + 6 * bf2 - 4 * bf) * fof3 + (bf3 - 6 * bf2 + 6 * bf) * fof2 + (2 * bf2 - 4 * bf) * fof + bf);
            S02 = (((bf2 - bf) * fof4 + (4 * bf - 3 * bf2) * fof3 + (3 * bf2 - 5 * bf) * fof2 + (2 * bf - bf2) * fof) * X + (-bf3 + 2 * bf2 - bf) * fof3 + (2 * bf3 - 3 * bf2 + bf) * fof2 + (-bf3 + bf2 + bf) * fof - bf);
            S10 = (((bf2 - bf) * fof4 + (-bf2 + bf + 1) * fof3 + (-bf - 1) * fof2) * X + (-bf2 + 2 * bf - 1) * fof3 + (2 * bf2 - 4 * bf + 2) * fof2 + (-bf2 + 2 * bf - 1) * fof);
            S11 = ((bf3 - 2 * bf2 + bf) * fof4 + (-bf3 + 4 * bf2 - 3 * bf) * fof3 + (5 * bf - 4 * bf2) * fof2 + (bf2 - 3 * bf) * fof);
            S20 = -(((bf - 1) * fof3 + (1 - bf) * fof2) * X + (bf2 - 2 * bf + 1) * fof3 + (-bf2 + 3 * bf - 2) * fof2 + (1 - bf) * fof);
            S21 = (((bf2 - bf) * fof4 + (2 * bf - bf2) * fof3 - 2 * bf * fof2) * X + (bf3 - 2 * bf2 + bf) * fof4 + (-bf3 + 4 * bf2 - 3 * bf) * fof3 + (4 * bf - 3 * bf2) * fof2 - 2 * bf * fof);

            div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

            KFrB = (S00 + S01) / div;
            KFB = S02 / div;
            KFrBS = (S10 + S11 + S20 + S21) / div;
            KFBS = X * S11 / div;
            KILS = X * S10 / div;
            KDFS = (1 - X) * (S10 + S11) / div;
        }

        cycle.addSpell(needsDisplayCalculations, FrB, KFrB);
        cycle.addSpell(needsDisplayCalculations, FB, KFB);
        cycle.addSpell(needsDisplayCalculations, FrBS, KFrBS);
        cycle.addSpell(needsDisplayCalculations, FBS, KFBS);
        cycle.addSpell(needsDisplayCalculations, ILS, KILS);
        cycle.addSpell(needsDisplayCalculations, DFS, KDFS);
        cycle.castTime /= hasteFactor; // ignores latency effects, but it'll have to do for now
        cycle.calculate();
        return cycle;
    }
}
