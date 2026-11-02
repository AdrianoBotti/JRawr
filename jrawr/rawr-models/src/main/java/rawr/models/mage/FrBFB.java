// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrBFB {
    private FrBFB() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        Spell FrB;
        float K;
        cycle.name = "FrBFB";

        FrB = castingState.getSpell(SpellId.FrostboltFOF);
        Spell FB = castingState.getSpell(SpellId.FireballBF);

        // FrB      1 - brainFreeze
        // FrB-FB   brainFreeze

        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        K = 0.05f * castingState.mageTalents.getBrainFreeze() / (1 - T8);

        cycle.addSpell(needsDisplayCalculations, FrB, 1);
        cycle.addSpell(needsDisplayCalculations, FB, K);
        cycle.calculate();
        return cycle;
    }
}
