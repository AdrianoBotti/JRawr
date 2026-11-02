// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABar2C extends Cycle {
    public ABABar2C(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float K1, K2;
        name = "ABABar2C";

        // S0: no proc at start
        // AB0-ABar1                  => S0     (1-MB)*(1-MB)
        //                            => S1     (1 - (1-MB)*(1-MB))
        // S1: proc at start
        // AB0-AB1-MBAM2-ABar         => S0     (1-T8)*(1-MB)
        //                            => S1     (1 - (1-T8)*(1-MB))

        // P1 = (1-MB)*(1-MB)
        // P2 = (1-T8)*(1-MB)
        // S0 = S0 * P1 + S1 * P2

        // S0 = P2/(P2-P1+1)

        Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell ABar1 = castingState.getSpell(SpellId.ArcaneBarrage1);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float P1 = (1 - MB) * (1 - MB);
        float P2 = (1 - T8) * (1 - MB);
        float S0 = P2 / (P2 - P1 + 1);
        float S1 = 1 - S0;
        K1 = S0;
        K2 = S1;

        addSpell(needsDisplayCalculations, AB0, K1 + K2);
        if (AB0.castTime + ABar1.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB0.castTime - ABar1.castTime, K1);
        addSpell(needsDisplayCalculations, ABar1, K1);
        addSpell(needsDisplayCalculations, AB1, K2);
        addSpell(needsDisplayCalculations, MBAM2, K2);
        addSpell(needsDisplayCalculations, ABar, K2);

        calculate();
    }
}
