// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class ABAM {
    private ABAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "ABAM";

        Spell ab = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell am = castingState.getSpell(SpellId.ArcaneMissiles1);
        Spell mbam = castingState.getSpell(SpellId.ArcaneMissilesMB1);

        float mb = 0.04f * castingState.mageTalents.getMissileBarrage();
        float t8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        if (mb == 0.0) {
            // if we don't have barrage then this degenerates to AB-AM
            cycle.addSpell(needsDisplayCalculations, ab, 1);
            cycle.addSpell(needsDisplayCalculations, am, 1);
            cycle.calculate();
        } else {
            // S0: no proc
            // AB-AM    => S0   (1 - MB)
            // AB-MBAM  => S0   MB * (1 - T8)
            // AB-MBAM  => S1   MB * T8

            // S1: proc
            // AB-MBAM  => S0   (1 - T8)
            // AB-MBAM  => S1   T8

            // S0 = S0 * (1 - MB + MB * (1 - T8)) + S1 * (1 - T8)
            // S1 = S0 * MB * T8 + S1 * T8
            // S0 + S1 = 1

            float s0 = (1 - t8) / (1 - (1 - mb) * t8);
            float k1 = 1 - mb;

            cycle.addSpell(needsDisplayCalculations, ab, 1);
            cycle.addSpell(needsDisplayCalculations, am, s0 * k1);
            cycle.addSpell(needsDisplayCalculations, mbam, 1 - s0 * k1);

            cycle.calculate();
        }
        return cycle;
    }
}
