// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABar1C extends Cycle {
    public ABABar1C(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        name = "ABABar1C";

        Spell AB = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell ABar1 = castingState.getSpell(SpellId.ArcaneBarrage1);
        Spell MBAM1 = castingState.getSpell(SpellId.ArcaneMissilesMB1);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        // S0
        // AB-ABar1 => S0                  (1-MB)*(1-MB)
        //          => S1                  1 - (1-MB)*(1-MB)

        // S1
        // AB-MBAM1-ABar => S0             (1-T8)*(1-MB)
        //               => S1             1 - (1-T8)*(1-MB)

        // S0 = S0 * (1 - MB) * (1 - MB) + S1 * (1 - T8) * (1 - MB)
        // S0 + S1 = 1

        float K1 = (1 - MB) * (1 - MB);
        float K2 = (1 - T8) * (1 - MB);
        float S0 = K2 / (K2 - K1 + 1);
        float S1 = 1 - S0;

        //AB-ABar1
        addSpell(needsDisplayCalculations, AB, S0);
        if (AB.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime, S0);
        addSpell(needsDisplayCalculations, ABar1, S0);

        //AB-MBAM1-ABar
        addSpell(needsDisplayCalculations, AB, S1);
        addSpell(needsDisplayCalculations, MBAM1, S1);
        if (AB.castTime + MBAM1.castTime + ABar.castTime < 3.0) {
            addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - MBAM1.castTime - AB.castTime - ABar.castTime, S1);
            addSpell(needsDisplayCalculations, ABar, S1);
        } else {
            addSpell(needsDisplayCalculations, ABar, S1);
        }

        calculate();
    }
}
