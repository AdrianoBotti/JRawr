// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBFBIL {
    private FrBFBIL() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB, FrBS, FB, FBS, ILS;
        float KFrB, KFrBS, KFB, KFBS, KILS;
        cycle.name = "FrBFBIL";

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
        // FrBS-FBS => S10    fof*(1-T8)
        //             S11    fof*T8
        //             S00    (1-fof)*(1-T8)
        //             S02    (1-fof)*T8

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

        // S00 = (1-fof)*(1-bf) * S00 + S02*(1-T8) + (1-fof)*(1-bf) * S10 + (1-fof)*(1-T8) * S11
        // S01 = (1-fof) * bf * S00
        // S02 = (1-fof) * S01 + T8 * S02 + (1-fof) * bf * S10 + (1-fof)*T8 * S11
        // S10 = fof * (1-bf) * S10 + fof*(1-T8) * S11 + (1-fof)*(1-bf) * S20
        // S11 = fof * bf * S10 + fof*T8 * S11 + (1-fof) * bf * S20 + (1-fof) * S21
        // S20 = fof * (1-bf) * S00 + fof * (1-bf) * S20
        // S21 = fof * bf * S00 + fof * S01 + fof * bf * S20 + fof * S21
        // S00 + S01 + S02 + S10 + S11 + S20 + S21 = 1

        // solved symbolically

        float bf = 0.05f * castingState.mageTalents.getBrainFreeze();
        float fof = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());

        float S00 = (fof - 1) * ((bf - 1) * fof + 1) * (T8 - 1) * (fof * (T8 - bf) - 1);
        float S01 = -bf * (fof - 1) * (fof - 1) * ((bf - 1) * fof + 1) * (T8 - 1) * (fof * (T8 - bf) - 1);
        float S02 = -bf * (fof - 1) * (fof * ((fof - 1) * ((bf - 1) * fof - bf + 2) * T8 - fof * ((bf - 1) * fof - bf * bf + 2) - bf * bf + 2) - 1);
        float S10 = fof * (T8 - 1) * (fof * ((bf * (fof - 1) - 1) * ((bf - 1) * fof + 1) * T8 - bf * (fof * ((bf - 1) * fof - 2 * bf + 3) + 2 * bf - 5) - 1) + (bf - 1) * (bf - 1));
        float S11 = -bf * fof * (fof * ((bf - 1) * fof * ((bf - 1) * fof - bf + 3) - 4 * bf + 5) + bf - 3) * (T8 - 1);
        float S20 = -(bf - 1) * (fof - 1) * fof * (T8 - 1) * (fof * (T8 - bf) - 1);
        float S21 = bf * fof * (fof * ((bf - 1) * fof - bf + 2) - 2) * (T8 - 1) * (fof * (T8 - bf) - 1);

        float div = S00 + S01 + S02 + S10 + S11 + S20 + S21;

        KFrB = (S00 + S01) / div;
        KFB = S02 / div;
        KFrBS = (S10 + S11 + S20 + S21) / div;
        KFBS = S11 / div;
        KILS = S10 / div;

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        FB = castingState.getSpell(SpellId.FireballBF);
        FBS = castingState.getFrozenState().getSpell(SpellId.FireballBF);
        ILS = castingState.getFrozenState().getSpell(SpellId.IceLance);

        cycle.addSpell(needsDisplayCalculations, FrB, KFrB);
        cycle.addSpell(needsDisplayCalculations, FB, KFB);
        cycle.addSpell(needsDisplayCalculations, FrBS, KFrBS);
        cycle.addSpell(needsDisplayCalculations, FBS, KFBS);
        cycle.addSpell(needsDisplayCalculations, ILS, KILS);
        cycle.calculate();
        return cycle;
    }
}
