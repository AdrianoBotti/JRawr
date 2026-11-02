// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AB3ABar3MBAM extends Cycle {
    public AB3ABar3MBAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float K1, K2;
        name = "AB3ABar3MBAM";

        // S0: no proc at start
        // AB0-AB1-AB2-ABar3          => S0     (1-MB)*(1-MB)*(1-MB)*(1-MB)
        //                            => S1     (1-MB)*(1-MB)*(1 - (1-MB)*(1-MB))
        // AB0-AB1-AB2-MBAM3          => S0     (1 - (1-MB)*(1-MB))*(1-T8)
        //                            => S1     (1 - (1-MB)*(1-MB))*T8
        // S1: proc at start
        // AB0-AB1-AB2-MBAM3          => S0     1-T8
        //                            => S1     T8

        // P1 = (1-MB)*(1-MB)
        // P2 = (1-T8)

        // S0 = S0 * (P1 * P1 + (1 - P1) * P2) + S1 * P2

        // S0 = P2/(P1*(P2-P1)+1)

        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell ABar3 = castingState.getSpell(SpellId.ArcaneBarrage3);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float P1 = (1 - MB) * (1 - MB);
        float P2 = (1 - T8);
        float S0 = P2 / (P1 * (P2 - P1) + 1);
        float S1 = 1 - S0;
        K1 = S0 * P1;
        K2 = S0 * (1 - P1) + S1;

        if (needsDisplayCalculations) {
            Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
            addSpell(needsDisplayCalculations, AB0, K1 + K2);
            addSpell(needsDisplayCalculations, AB1, K1 + K2);
            addSpell(needsDisplayCalculations, AB2, K1 + K2);
        } else {
            Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
            castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), this, AB, K1 + K2, K1 + K2, K1 + K2, 0);
        }
        addSpell(needsDisplayCalculations, ABar3, K1);
        addSpell(needsDisplayCalculations, MBAM3, K2);

        calculate();
    }
}
