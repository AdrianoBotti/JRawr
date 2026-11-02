// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class ABSpam234MBAM {
    private ABSpam234MBAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        float K1, K2, K3, K4, K5, K6, K7, S0, S1;
        cycle.name = "ABSpam234MBAM";

        // always ramp up to 4 AB before using MBAM

        // S0:
        // AB0-AB1-MBAM2 => S0               MB*(1-T8)                         first AB procs
        // AB0-AB1-MBAM2 => S2               MB*T8                             first AB procs
        // AB0-AB1-AB2-MBAM3 => S0           (1-MB)*MB*(1-T8)                  2nd AB procs
        // AB0-AB1-AB2-MBAM3 => S2           (1-MB)*MB*T8                      2nd AB procs
        // AB0-AB1-AB2-AB3-MBAM4 => S0       (1-MB)*(1-MB)*MB*(1-T8)           3rd AB procs
        // AB0-AB1-AB2-AB3-MBAM4 => S2       (1-MB)*(1-MB)*MB*T8               3rd AB procs
        // AB0-AB1-AB2-AB3-AB4-MBAM4 => S0   (1-MB)*(1-MB)*(1-MB)*MB*(1-T8)    fourth AB procs
        // AB0-AB1-AB2-AB3-AB4-MBAM4 => S2   (1-MB)*(1-MB)*(1-MB)*MB*T8        fourth AB procs
        // AB0-AB1-AB2-AB3 => S1             (1-MB)*(1-MB)*(1-MB)*(1-MB)       no procs
        // S1:
        // AB4-AB4-MBAM4 => S0           MB*(1-T8)                     proc
        // AB4-AB4-MBAM4 => S2           MB*T8                     proc
        // AB4 => S1                    (1-MB)                 no proc
        // S2:
        // AB0-AB1-MBAM2 => S0       (1-T8)     
        // AB0-AB1-MBAM2 => S2       T8     

        // K0 = (1-MB)*(1-MB)*(1-MB)*(1-MB)

        // S0 = (1 - K0) * (1 - T8) * S0 + MB * (1 - T8) * S1 + (1 - T8) * S2
        // S1 = K0 * S0 + (1-MB) * S1
        // S2 = (1 - K0) * T8 * S0 + MB * T8 * S1 + T8 * S2
        // S0 + S1 + S2 = 1

        Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell MBAM4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);

        float MB = 0.08f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float K0 = (1 - MB) * (1 - MB) * (1 - MB) * (1 - MB);
        S0 = (MB * T8 - MB) / (K0 * T8 - MB - K0);
        S1 = (K0 * T8 - K0) / (K0 * T8 - MB - K0);
        float S2 = -(MB * T8) / (K0 * T8 - MB - K0);
        K6 = S0 * MB + S2;
        K7 = S0 * MB * (1 - MB);
        K1 = S0 * MB * (1 - MB) * (1 - MB);
        K2 = S0 * MB * (1 - MB) * (1 - MB) * (1 - MB);
        K3 = S0 * K0;
        K4 = S1 * MB;
        K5 = S1 * (1 - MB);

        if (needsDisplayCalculations) {
            Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
            Spell AB3 = castingState.getSpell(SpellId.ArcaneBlast3);
            Spell AB4 = castingState.getSpell(SpellId.ArcaneBlast4);
            cycle.addSpell(needsDisplayCalculations, AB0, K1 + K2 + K3 + K6 + K7);
            cycle.addSpell(needsDisplayCalculations, AB1, K1 + K2 + K3 + K6 + K7);
            cycle.addSpell(needsDisplayCalculations, AB2, K1 + K2 + K3 + K7);
            cycle.addSpell(needsDisplayCalculations, AB3, K1 + K2 + K3);
            cycle.addSpell(needsDisplayCalculations, AB4, K2 + 2 * K4 + K5);
        } else {
            Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
            castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, AB, K1 + K2 + K3 + K6 + K7, K1 + K2 + K3 + K6 + K7, K1 + K2 + K3 + K7, K1 + K2 + K3, K2 + 2 * K4 + K5);
        }
        cycle.addSpell(needsDisplayCalculations, MBAM2, K6);
        cycle.addSpell(needsDisplayCalculations, MBAM3, K7);
        cycle.addSpell(needsDisplayCalculations, MBAM4, K1 + K2 + K4);

        cycle.calculate();
        return cycle;
    }
}
