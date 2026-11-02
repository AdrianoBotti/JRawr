// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AB2ABar3C extends Cycle {
    public AB2ABar3C(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float K1, K2;
        name = "AB2ABar3C";

        // S0: no proc at start
        // AB0-AB1-ABar2              => S0     (1-MB)*(1-MB)*(1-MB)
        //                            => S1     (1-MB)*(1 - (1-MB)*(1-MB))
        // AB0-AB1-AB2-MBAM3-ABar     => S0     MB*(1-T8)*(1-MB)
        //                            => S1     MB*(1 - (1-T8)*(1-MB))
        // S1: proc at start
        // AB0-AB1-AB2-MBAM3-ABar     => S0     (1-T8)*(1-MB)
        //                            => S1     1 - (1-T8)*(1-MB)

        // P1 = (1-MB)*(1-MB)
        // P2 = (1-T8)*(1-MB)
        // S0 = S0 * ((1-MB)*P1 + MB*P2) + S1 * P2

        // S0 = P2/(1 + (1-MB)*(P2 - P1))

        Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell ABar2 = castingState.getSpell(SpellId.ArcaneBarrage2);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float P1 = (1 - MB) * (1 - MB);
        float P2 = (1 - T8) * (1 - MB);
        float S0 = P2 / (1 + (1 - MB) * (P2 - P1));
        float S1 = 1 - S0;
        K1 = S0 * (1 - MB);
        K2 = S0 * MB + S1;

        addSpell(needsDisplayCalculations, AB0, K1 + K2);
        addSpell(needsDisplayCalculations, AB1, K1 + K2);
        addSpell(needsDisplayCalculations, ABar2, K1);
        addSpell(needsDisplayCalculations, AB2, K2);
        addSpell(needsDisplayCalculations, MBAM3, K2);
        addSpell(needsDisplayCalculations, ABar, K2);

        calculate();
    }
}
