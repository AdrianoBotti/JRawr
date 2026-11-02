// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABSpam03C extends Cycle {
    public ABSpam03C(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float K1, K2, K3, K4, K5, K6, S0, S1;
        name = "ABSpam03C";

        // S0: (we always enter S0 with ABar, take into account)

        // ABar-MBAM => S0                   MB*(1-T8)                     ABar procs
        // ABar-MBAM => S2                   MB*T8                         ABar procs
        // ABar-AB0-AB1-AB2-MBAM => S0       (1-MB)*(1 - (1-MB)*(1-MB))*(1-T8)    one of the first two AB procs
        // ABar-AB0-AB1-AB2-MBAM => S2       (1-MB)*(1 - (1-MB)*(1-MB))*T8    one of the first two AB procs
        // ABar-AB0-AB1-AB2-AB3-MBAM => S0   (1-MB)*(1-MB)*(1-MB)*MB*(1-T8)       third AB procs
        // ABar-AB0-AB1-AB2-AB3-MBAM => S2   (1-MB)*(1-MB)*(1-MB)*MB*T8       third AB procs
        // ABar-AB0-AB1-AB2 => S1            (1-MB)*(1-MB)*(1-MB)*(1-MB)   no procs
        // S1:
        // AB3-AB3-MBAM => S0           MB*(1-T8)                     proc
        // AB3-AB3-MBAM => S2           MB*T8                     proc
        // AB3 => S1                    (1-MB)                 no proc
        // S2:
        // ABar-MBAM => S0                   (1-T8)                        
        // ABar-MBAM => S2                   T8                            

        // K0 = (1-MB)*(1-MB)*(1-MB)*(1-MB)
        // S0 = (1 - K0) * (1 - T8) * S0 + MB * (1 - T8) * S1 + (1 - T8) * S2
        // S1 = K0 * S0 + (1-MB) * S1
        // S2 = (1 - K0) * T8 * S0 + MB * T8 * S1 + T8 * S2
        // S0 + S1 + S2 = 1


        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float K0 = (1 - MB) * (1 - MB) * (1 - MB) * (1 - MB);
        S0 = (MB * T8 - MB) / (K0 * T8 - MB - K0);
        S1 = (K0 * T8 - K0) / (K0 * T8 - MB - K0);
        float S2 = -(MB * T8) / (K0 * T8 - MB - K0);

        K6 = S0 * MB + S2;
        K1 = S0 * (1 - MB) * (1 - (1 - MB) * (1 - MB));
        K2 = S0 * (1 - MB) * (1 - MB) * (1 - MB) * MB;
        K3 = S0 * (1 - MB) * (1 - MB) * (1 - MB) * (1 - MB);
        K4 = S1 * MB;
        K5 = S1 * (1 - MB);

        if (needsDisplayCalculations) {
            Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
            Spell AB3 = castingState.getSpell(SpellId.ArcaneBlast3);
            addSpell(needsDisplayCalculations, AB0, K1 + K2 + K3);
            addSpell(needsDisplayCalculations, AB1, K1 + K2 + K3);
            addSpell(needsDisplayCalculations, AB2, K1 + K2 + K3);
            addSpell(needsDisplayCalculations, AB3, K2 + 2 * K4 + K5);
        } else {
            Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
            castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), this, AB, K1 + K2 + K3, K1 + K2 + K3, K1 + K2 + K3, K2 + 2 * K4 + K5);
        }
        addSpell(needsDisplayCalculations, MBAM, K6);
        if (MBAM.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - MBAM.castTime - ABar.castTime, K6);
        addSpell(needsDisplayCalculations, MBAM3, K1 + K2 + K4);
        addSpell(needsDisplayCalculations, ABar, K1 + K2 + K4 + K6);

        calculate();
    }
}
