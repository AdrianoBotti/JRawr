// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class ABSpam4MBAM {
    private ABSpam4MBAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        float K1, K2, K3, K4, K5, S0, S1;
        cycle.name = "ABSpam4MBAM";

        // always ramp up to 4 AB before using MBAM

        // S0:
        // AB0-AB1-AB2-AB3-MBAM => S0       (1 - (1-MB)*(1-MB)*(1-MB))*(1-T8)      one of the first three AB procs
        // AB0-AB1-AB2-AB3-MBAM => S2       (1 - (1-MB)*(1-MB)*(1-MB))*T8      one of the first three AB procs
        // AB0-AB1-AB2-AB3-AB4-MBAM => S0   (1-MB)*(1-MB)*(1-MB)*MB*(1-T8)       fourth AB procs
        // AB0-AB1-AB2-AB3-AB4-MBAM => S2   (1-MB)*(1-MB)*(1-MB)*MB*T8       fourth AB procs
        // AB0-AB1-AB2-AB3 => S1            (1-MB)*(1-MB)*(1-MB)*(1-MB)   no procs
        // S1:
        // AB4-AB4-MBAM => S0           MB*(1-T8)                     proc
        // AB4-AB4-MBAM => S2           MB*T8                     proc
        // AB4 => S1                    (1-MB)                 no proc
        // S2:
        // AB0-AB1-AB2-AB3-MBAM => S0       (1-T8)     
        // AB0-AB1-AB2-AB3-MBAM => S2       T8     

        // K0 = (1-MB)*(1-MB)*(1-MB)*(1-MB)

        // S0 = (1 - K0) * (1 - T8) * S0 + MB * (1 - T8) * S1 + (1 - T8) * S2
        // S1 = K0 * S0 + (1-MB) * S1
        // S2 = (1 - K0) * T8 * S0 + MB * T8 * S1 + T8 * S2
        // S0 + S1 + S2 = 1

        // for 2T10 we assume that the buff always drops by the time we get to 4 stacks

        Spell MBAM4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);

        float MB = 0.08f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float K0 = (1 - MB) * (1 - MB) * (1 - MB) * (1 - MB);
        S0 = (MB * T8 - MB) / (K0 * T8 - MB - K0);
        S1 = (K0 * T8 - K0) / (K0 * T8 - MB - K0);
        float S2 = -(MB * T8) / (K0 * T8 - MB - K0);
        K1 = S0 * (1 - (1 - MB) * (1 - MB) * (1 - MB)) + S2;
        K2 = S0 * (1 - MB) * (1 - MB) * (1 - MB) * MB;
        K3 = S0 * K0;
        K4 = S1 * MB;
        K5 = S1 * (1 - MB);

        if (needsDisplayCalculations) {
            if (castingState.baseStats.getMage2T10() > 0) {
                float m2T10time = 5.0f - MBAM4.castTime;
                Spell AB0 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast0) : castingState.getSpell(SpellId.ArcaneBlast0);
                m2T10time -= AB0.castTime;
                Spell AB1 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast1) : castingState.getSpell(SpellId.ArcaneBlast1);
                m2T10time -= AB1.castTime;
                Spell AB2 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast2) : castingState.getSpell(SpellId.ArcaneBlast2);
                m2T10time -= AB2.castTime;
                Spell AB3 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast3) : castingState.getSpell(SpellId.ArcaneBlast3);
                Spell AB4 = castingState.getSpell(SpellId.ArcaneBlast4);
                cycle.addSpell(needsDisplayCalculations, AB0, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB1, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB2, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB3, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB4, K2 + 2 * K4 + K5);
            } else {
                Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
                Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
                Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
                Spell AB3 = castingState.getSpell(SpellId.ArcaneBlast3);
                Spell AB4 = castingState.getSpell(SpellId.ArcaneBlast4);
                cycle.addSpell(needsDisplayCalculations, AB0, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB1, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB2, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB3, K1 + K2 + K3);
                cycle.addSpell(needsDisplayCalculations, AB4, K2 + 2 * K4 + K5);
            }
        } else {
            if (castingState.baseStats.getMage2T10() > 0) {
                Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
                Spell ABT = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlastRaw);
                float m2T10time = 5.0f - MBAM4.castTime;
                float ab0 = 0.0f, ab1 = 0.0f, ab2 = 0.0f, ab3 = 0.0f, abt0 = 0.0f, abt1 = 0.0f, abt2 = 0.0f, abt3 = 0.0f;
                if (m2T10time > 0.0f) {
                    abt0 = K1 + K2 + K3;
                    m2T10time -= ABT.castTime;
                } else {
                    ab0 = K1 + K2 + K3;
                    m2T10time -= AB.castTime;
                }
                if (m2T10time > 0.0f) {
                    abt1 = K1 + K2 + K3;
                    m2T10time -= ABT.castTime;
                } else {
                    ab1 = K1 + K2 + K3;
                    m2T10time -= AB.castTime;
                }
                if (m2T10time > 0.0f) {
                    abt2 = K1 + K2 + K3;
                    m2T10time -= ABT.castTime;
                } else {
                    ab2 = K1 + K2 + K3;
                    m2T10time -= AB.castTime;
                }
                if (m2T10time > 0.0f) {
                    abt3 = K1 + K2 + K3;
                    m2T10time -= ABT.castTime;
                } else {
                    ab3 = K1 + K2 + K3;
                    m2T10time -= AB.castTime;
                }
                castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, AB, ab0, ab1, ab2, ab3, K2 + 2 * K4 + K5);
                castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, ABT, abt0, abt1, abt2, abt3);
            } else {
                Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
                castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, AB, K1 + K2 + K3, K1 + K2 + K3, K1 + K2 + K3, K1 + K2 + K3, K2 + 2 * K4 + K5);
            }
        }
        cycle.addSpell(needsDisplayCalculations, MBAM4, K1 + K2 + K4);

        cycle.calculate();
        return cycle;
    }
}
