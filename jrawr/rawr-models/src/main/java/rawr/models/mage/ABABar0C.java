// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABar0C extends Cycle {
    public ABABar0C(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        name = "ABABar0C";

        Spell AB = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell ABar1 = castingState.getSpell(SpellId.ArcaneBarrage1);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        if (MB == 0.0) {
            // if we don't have barrage then this degenerates to AB-ABar
            addSpell(needsDisplayCalculations, AB, 1);
            if (AB.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime, 1);
            addSpell(needsDisplayCalculations, ABar1, 1);

            calculate();
        } else {
            // S0: no proc
            // AB-ABar1    => S0   (1 - MB) * (1 - MB)
            // AB-ABar1    => S1   1 - (1 - MB) * (1 - MB)

            // S1: proc
            // MBAM-ABar  => S0   (1 - T8) * (1 - MB)
            // MBAM-ABar  => S1   1 - (1 - T8) * (1 - MB)

            // S0 = S0 * (1 - MB) * (1 - MB) + S1 * (1 - T8) * (1 - MB)
            // S0 + S1 = 1

            float K1 = (1 - MB) * (1 - MB);
            float K2 = (1 - T8) * (1 - MB);
            float S0 = K2 / (K2 - K1 + 1);

            //AB-ABar 0.8 * 0.8
            addSpell(needsDisplayCalculations, ABar1, S0);
            addSpell(needsDisplayCalculations, AB, S0);
            if (AB.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime, S0);

            //ABar-MBAM
            addSpell(needsDisplayCalculations, ABar, 1 - S0);
            addSpell(needsDisplayCalculations, MBAM, 1 - S0);
            if (MBAM.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - MBAM.castTime - ABar.castTime, 1 - S0);

            calculate();
        }
    }
}
