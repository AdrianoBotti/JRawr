// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBIL {
    private FrBIL() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB, FrBS, ILS;
        float KFrB, KFrBS, KILS;
        cycle.name = "FrBIL";

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        // S00: FOF0
        // FrB => S20    fof
        //        S00    (1-fof)

        // S10: FOF1, BF0
        // FrBS-ILS => S10    fof
        //             S00    (1-fof)

        // S20: FOF0, BF0
        // FrBS => S20    fof
        //         S10    (1-fof)


        // S00 = (1-fof) * S00 + (1-fof) * S10
        // S10 = fof * S10 + (1-fof) * S20
        // S20 = fof * S00 + fof * S20
        // S00 + S10 + S20 = 1

        float fof = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());

        float S00 = (1 - fof) / (1 + fof);
        float S10 = fof / (1 + fof);
        float S20 = fof / (1 + fof);

        KFrB = S00;
        KFrBS = S10 + S20;
        KILS = S10;

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        ILS = castingState.getFrozenState().getSpell(SpellId.IceLance);

        cycle.addSpell(needsDisplayCalculations, FrB, KFrB);
        cycle.addSpell(needsDisplayCalculations, FrBS, KFrBS);
        cycle.addSpell(needsDisplayCalculations, ILS, KILS);
        cycle.calculate();
        return cycle;
    }
}
