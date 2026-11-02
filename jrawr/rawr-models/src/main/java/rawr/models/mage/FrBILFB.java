// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBILFB {
    private FrBILFB() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB, FrBS, FB, ILS;
        float KFrB, KFrBS, KFB, KILS;
        cycle.name = "FrBILFB";

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        // S00: FOF0, BF0
        // FrB => S21    fof * bf
        //        S20    fof * (1-bf)
        //        S01    (1-fof) * bf
        //        S00    (1-fof)*(1-bf)

        // S01: FOF0, BF1
        // FrB => S22    fof
        //        S02    (1-fof)

        // S02: FOF0, BF2
        // FB => S00    (1-T8)
        //    => S02    T8

        // S10: FOF1, BF0
        // FrBS-ILS => S12    fof * bf
        //             S10    fof * (1-bf)
        //             S02    (1-fof) * bf
        //             S00    (1-fof)*(1-bf)

        // S11: FOF1, BF1
        // FrBS-ILS => S12    fof
        //             S02    (1-fof)

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

        // S00 = (1-fof)*(1-bf) * S00 + S02*(1-T8) + (1-fof)*(1-bf) * S10
        // S01 = (1-fof) * bf * S00
        // S02 = (1-fof) * S01 + T8 * S02 + (1-fof) * bf * S10 + (1-fof) * S11
        // S10 = fof * (1-bf) * S10 + (1-fof)*(1-bf) * S20
        // S11 = fof * bf * S10 + fof * S11 + (1-fof) * bf * S20 + (1-fof) * S21
        // S20 = fof * (1-bf) * S00 + fof * (1-bf) * S20
        // S21 = fof * bf * S00 + fof * S01 + fof * bf * S20 + fof * S21
        // S00 + S01 + S02 + S10 + S11 + S20 + S21 = 1

        // solved symbolically

        float bf = 0.05f * castingState.mageTalents.getBrainFreeze();
        float fof = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());

        float S00 = (((bf * bf - 2 * bf + 1) * fof * fof * fof + (-bf * bf + 4 * bf - 3) * fof * fof + (3 - 2 * bf) * fof - 1) * T8 + (-bf * bf + 2 * bf - 1) * fof * fof * fof + (bf * bf - 4 * bf + 3) * fof * fof + (2 * bf - 3) * fof + 1);
        float S01 = -(((bf * bf * bf - 2 * bf * bf + bf) * fof * fof * fof * fof + (-2 * bf * bf * bf + 6 * bf * bf - 4 * bf) * fof * fof * fof + (bf * bf * bf - 6 * bf * bf + 6 * bf) * fof * fof + (2 * bf * bf - 4 * bf) * fof + bf) * T8 + (-bf * bf * bf + 2 * bf * bf - bf) * fof * fof * fof * fof + (2 * bf * bf * bf - 6 * bf * bf + 4 * bf) * fof * fof * fof + (-bf * bf * bf + 6 * bf * bf - 6 * bf) * fof * fof + (4 * bf - 2 * bf * bf) * fof - bf);
        float S02 = ((bf * bf * bf - 2 * bf * bf + bf) * fof * fof * fof + (-2 * bf * bf * bf + 3 * bf * bf - bf) * fof * fof + (bf * bf * bf - bf * bf - bf) * fof + bf);
        float S10 = -(((bf * bf - 2 * bf + 1) * fof * fof * fof + (-2 * bf * bf + 4 * bf - 2) * fof * fof + (bf * bf - 2 * bf + 1) * fof) * T8 + (-bf * bf + 2 * bf - 1) * fof * fof * fof + (2 * bf * bf - 4 * bf + 2) * fof * fof + (-bf * bf + 2 * bf - 1) * fof);
        float S11 = (((bf * bf * bf - 2 * bf * bf + bf) * fof * fof * fof * fof + (-bf * bf * bf + 4 * bf * bf - 3 * bf) * fof * fof * fof + (5 * bf - 4 * bf * bf) * fof * fof + (bf * bf - 3 * bf) * fof) * T8 + (-bf * bf * bf + 2 * bf * bf - bf) * fof * fof * fof * fof + (bf * bf * bf - 4 * bf * bf + 3 * bf) * fof * fof * fof + (4 * bf * bf - 5 * bf) * fof * fof + (3 * bf - bf * bf) * fof);
        float S20 = -(((bf * bf - 2 * bf + 1) * fof * fof * fof + (-bf * bf + 3 * bf - 2) * fof * fof + (1 - bf) * fof) * T8 + (-bf * bf + 2 * bf - 1) * fof * fof * fof + (bf * bf - 3 * bf + 2) * fof * fof + (bf - 1) * fof);
        float S21 = (((bf * bf * bf - 2 * bf * bf + bf) * fof * fof * fof * fof + (-bf * bf * bf + 4 * bf * bf - 3 * bf) * fof * fof * fof + (4 * bf - 3 * bf * bf) * fof * fof - 2 * bf * fof) * T8 + (-bf * bf * bf + 2 * bf * bf - bf) * fof * fof * fof * fof + (bf * bf * bf - 4 * bf * bf + 3 * bf) * fof * fof * fof + (3 * bf * bf - 4 * bf) * fof * fof + 2 * bf * fof);

        float div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

        KFrB = (S00 + S01) / div;
        KFB = S02 / div;
        KFrBS = (S10 + S11 + S20 + S21) / div;
        KILS = (S10 + S11) / div;

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        FB = castingState.getSpell(SpellId.FireballBF);
        ILS = castingState.getFrozenState().getSpell(SpellId.IceLance);

        cycle.addSpell(needsDisplayCalculations, FrB, KFrB);
        cycle.addSpell(needsDisplayCalculations, FB, KFB);
        cycle.addSpell(needsDisplayCalculations, FrBS, KFrBS);
        cycle.addSpell(needsDisplayCalculations, ILS, KILS);
        cycle.calculate();
        return cycle;
    }
}
