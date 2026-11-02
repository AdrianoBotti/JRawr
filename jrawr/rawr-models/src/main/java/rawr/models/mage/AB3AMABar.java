// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class AB3AMABar {
    private AB3AMABar() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "AB3AMABar";

        Spell ab0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell ab1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell ab2 = castingState.getSpell(SpellId.ArcaneBlast2);
        Spell am3 = castingState.getSpell(SpellId.ArcaneMissiles3);
        Spell mbam3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell abar = castingState.getSpell(SpellId.ArcaneBarrage);

        float mb = 0.04f * castingState.mageTalents.getMissileBarrage();
        float t8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float k1 = (1 - mb) * (1 - mb) * (1 - mb) * (1 - mb);
        float s0 = (1 - t8) / (1 - k1 * t8);

        cycle.addSpell(needsDisplayCalculations, ab0, 1);
        cycle.addSpell(needsDisplayCalculations, ab1, 1);
        cycle.addSpell(needsDisplayCalculations, ab2, 1);
        cycle.addSpell(needsDisplayCalculations, am3, s0 * k1);
        cycle.addSpell(needsDisplayCalculations, mbam3, 1 - s0 * k1);
        cycle.addSpell(needsDisplayCalculations, abar, 1);

        cycle.calculate();
        return cycle;
    }
}
