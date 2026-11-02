// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABar0MBAM extends Cycle {
    public ABABar0MBAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        name = "ABABar0MBAM";

        Spell AB = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell ABar1 = castingState.getSpell(SpellId.ArcaneBarrage1);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        // S0
        // AB-ABar1 => S0                  (1-MB)*(1-MB)
        //          => S1                  1 - (1-MB)*(1-MB)

        // S1
        // MBAM => S0             (1-T8)
        //      => S1             T8

        // S0 = S0 * (1 - MB) * (1 - MB) + S1 * (1 - T8) * (1 - MB)
        // S0 + S1 = 1

        float K1 = (1 - MB) * (1 - MB);
        float K2 = (1 - T8);
        float S0 = K2 / (K2 - K1 + 1);
        float S1 = 1 - S0;

        addSpell(needsDisplayCalculations, AB, S0);
        if (AB.castTime + ABar1.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar1.castTime, S0);
        addSpell(needsDisplayCalculations, ABar1, S0);

        addSpell(needsDisplayCalculations, MBAM, S1);

        calculate();
    }
}
