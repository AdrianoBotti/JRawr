// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class ABSpam0234MBAM {
    private ABSpam0234MBAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        float K1, K2, K3, K4, K5, K6, K7, S0, S1;
        cycle.name = "ABSpam0234MBAM";

        if (castingState.baseStats.getMage2T10() > 0) {
            // 5 - channelLatency - n * ABT > 0
            // (5 - channelLatency) / ABT > n

            // S0:
            // AB0-AB1-MBAM2 => S0                  MB                               first AB procs
            // AB0-AB1-AB2-MBAM3 => S0              (1-MB)*MB                        second AB procs
            // AB0-AB1-AB2-AB3-MBAM4 => S0          (1-MB)*(1-MB)*MB                 third AB procs
            // AB0-AB1-AB2-AB3-AB4-MBAM4 => S0      (1-MB)*(1-MB)*(1-MB)*MB          fourth AB procs
            // AB0-AB1-AB2-AB3-AB4-AB4-MBAM4 => S0  (1-MB)*(1-MB)*(1-MB)*(1-MB)*MB   fifth AB procs
            // ...
            // AB0-...-AB4 => S1                    (1-MB)*...*(1-MB)                 no procs in first n,4
            // S1:
            // AB4-AB4-MBAM4 => S0   MB      proc
            // AB4 => S1            (1-MB)  no proc

            // K0 = (1-MB)^n

            // S0 = (1 - K0) * S0 + MB * S1
            // S1 = K0 * S0 + (1-MB) * S1
            // S0 + S1 = 1

            // S0 = MB / K0 * S1
            // S1 = K0 / (K0 + MB)
            // S0 = MB / (K0 + MB)

            float MB = 0.08f * castingState.mageTalents.getMissileBarrage();

            if (needsDisplayCalculations) {
                float m2T10time = 5.0f - castingState.calculationOptions.getLatencyChannel();
                Spell AB0 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast0) : castingState.getSpell(SpellId.ArcaneBlast0);
                m2T10time -= AB0.castTime;
                Spell AB1 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast1) : castingState.getSpell(SpellId.ArcaneBlast1);
                m2T10time -= AB1.castTime;
                Spell AB2 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast2) : castingState.getSpell(SpellId.ArcaneBlast2);
                Spell MBAM2 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB2) : castingState.getSpell(SpellId.ArcaneMissilesMB2);
                m2T10time -= AB2.castTime;
                Spell AB3 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast3) : castingState.getSpell(SpellId.ArcaneBlast3);
                Spell MBAM3 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB3) : castingState.getSpell(SpellId.ArcaneMissilesMB3);
                m2T10time -= AB2.castTime;
                Spell AB4 = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast4) : castingState.getSpell(SpellId.ArcaneBlast4);
                Spell ABn = AB4;
                Spell MBAM4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);
                Spell MBAM4T = MBAM4;
                if (m2T10time > 0.0f) {
                    ABn = castingState.getSpell(SpellId.ArcaneBlast4);
                    MBAM4T = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB4);
                }
                float m = (float) Math.ceil((5f - castingState.calculationOptions.getLatencyChannel()) / AB0.castTime);
                float n = Math.max(4, m);
                float K0 = (float) Math.pow(1 - MB, n);
                S0 = MB / (MB + K0);
                S1 = K0 / (MB + K0);

                //n=8:

                //AB0-AB1-MBAM2T                                 MB
                //AB0-AB1-AB2-MBAM3T                             (1-MB)^1*MB
                //AB0-AB1-AB2-AB3-MBAM4T                         (1-MB)^2*MB
                //AB0-AB1-AB2-AB3-AB4-MBAM4T                     (1-MB)^3*MB
                //AB0-AB1-AB2-AB3-AB4-AB4-MBAM4T                 (1-MB)^4*MB
                //AB0-AB1-AB2-AB3-AB4-AB4-AB4-MBAM4T             (1-MB)^5*MB
                //AB0-AB1-AB2-AB3-AB4-AB4-AB4-AB4-MBAM4          (1-MB)^6*MB
                //AB0-AB1-AB2-AB3-AB4-AB4-AB4-AB4-AB4n-MBAM4     (1-MB)^7*MB
                //AB0-AB1-AB2-AB3-AB4-AB4-AB4-AB4                (1-MB)^8

                //AB4n: S0 * (1-MB)^(n-1)*MB
                //MBAM4T: S0 * (1-(1-MB)^3 + (1-MB)^3*MB + ... + (1-MB)^(n-3)*MB)
                //        = S0 * (2 - (1-MB)^3 - (1-MB)^(n-5))
                //MBAM4: S0 * ((1-MB)^(n-2)*MB + (1-MB)^(n-1)*MB)
                //        = S0 * (1-MB)^(n-2)*MB * (2-MB)
                //AB4: S0 * (k^3 - k^(n-1))/MB

                if (n <= 4) {
                    cycle.addSpell(needsDisplayCalculations, AB0, S0);
                    cycle.addSpell(needsDisplayCalculations, AB1, S0);
                    cycle.addSpell(needsDisplayCalculations, AB2, S0 * (1 - MB));
                    cycle.addSpell(needsDisplayCalculations, AB3, S0 * (1 - MB - MB * (1 - MB)));
                    cycle.addSpell(needsDisplayCalculations, ABn, S0 * (1 - MB) * (1 - MB) * (1 - MB) * MB + S1 * (1 + MB));
                    cycle.addSpell(needsDisplayCalculations, MBAM4, S0 * (1 - K0 - MB - MB * (1 - MB)) + S1 * MB);
                    cycle.addSpell(needsDisplayCalculations, MBAM3, S0 * MB * (1 - MB));
                    cycle.addSpell(needsDisplayCalculations, MBAM2, S0 * MB);
                } else {
                    cycle.addSpell(needsDisplayCalculations, AB0, S0);
                    cycle.addSpell(needsDisplayCalculations, AB1, S0);
                    cycle.addSpell(needsDisplayCalculations, AB2, S0 * (1 - MB));
                    cycle.addSpell(needsDisplayCalculations, AB3, S0 * (1 - MB - MB * (1 - MB)));
                    cycle.addSpell(needsDisplayCalculations, AB4, S0 * (float) (Math.pow(1 - MB, 3) - Math.pow(1 - MB, n - 1)) / MB);
                    cycle.addSpell(needsDisplayCalculations, ABn, S0 * (float) Math.pow(1 - MB, n - 1) * MB + S1 * (1 + MB));
                    cycle.addSpell(needsDisplayCalculations, MBAM4, S0 * (float) Math.pow(1 - MB, n - 2) * MB * (2 - MB) + S1 * MB);
                    cycle.addSpell(needsDisplayCalculations, MBAM4T, S0 * (float) (2 - Math.pow(1 - MB, 3) - Math.pow(1 - MB, n - 5) - MB - MB * (1 - MB)));
                    cycle.addSpell(needsDisplayCalculations, MBAM3, S0 * MB * (1 - MB));
                    cycle.addSpell(needsDisplayCalculations, MBAM2, S0 * MB);
                }
            } else {
                Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
                Spell ABT = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlastRaw);
                Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);
                Spell MBAMT = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB);
                float m = (float) Math.ceil((5f - castingState.calculationOptions.getLatencyChannel()) / ABT.castTime);
                float n = Math.max(4, m);
                float K0 = (float) Math.pow(1 - MB, n);
                S0 = MB / (MB + K0);
                S1 = K0 / (MB + K0);

                Solver solver = castingState.getSolver();
                solver.getArcaneBlastTemplate().addToCycle(solver, cycle, AB, (m < 1) ? S0 : 0, (m < 2) ? S0 : 0, (m < 3) ? S0 * (1 - MB) : 0, (m < 4) ? S0 * (1 - MB - MB * (1 - MB)) : 0, (m <= 4) ? (S0 * (1 - MB) * (1 - MB) * (1 - MB) * MB + S1 * (1 + MB)) : (float) (S0 * Math.pow(1 - MB, n - 1) * MB + S1 * (1 + MB)));
                solver.getArcaneBlastTemplate().addToCycle(solver, cycle, ABT, (m >= 1) ? S0 : 0, (m >= 2) ? S0 : 0, (m >= 3) ? S0 * (1 - MB) : 0, (m >= 4) ? S0 * (1 - MB - MB * (1 - MB)) : 0, (m <= 4) ? 0 : (float) (S0 * (Math.pow(1 - MB, 3) - Math.pow(1 - MB, n - 1)) / MB));

                if (n <= 4) {
                    solver.getArcaneMissilesTemplate().addToCycle(solver, cycle, MBAM, 0, 0, (m < 3) ? S0 * MB : 0, (m < 4) ? S0 * MB * (1 - MB) : 0, S0 * (1 - K0 - MB - MB * (1 - MB)) + S1 * MB);
                    solver.getArcaneMissilesTemplate().addToCycle(solver, cycle, MBAMT, 0, 0, (m >= 3) ? S0 * MB : 0, (m >= 4) ? S0 * MB * (1 - MB) : 0, 0);
                } else {
                    solver.getArcaneMissilesTemplate().addToCycle(solver, cycle, MBAM, 0, 0, 0, 0, S0 * (float) Math.pow(1 - MB, n - 2) * MB * (2 - MB) + S1 * MB);
                    solver.getArcaneMissilesTemplate().addToCycle(solver, cycle, MBAMT, 0, 0, S0 * MB, S0 * MB * (1 - MB), S0 * (float) (2 - Math.pow(1 - MB, 3) - Math.pow(1 - MB, n - 5) - MB - MB * (1 - MB)));
                }
            }
        } else {

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
            // MBAM0 => S0       (1-T8)     
            // MBAM0 => S2       T8     

            // K0 = (1-MB)*(1-MB)*(1-MB)*(1-MB)

            // S0 = (1 - K0) * (1 - T8) * S0 + MB * (1 - T8) * S1 + (1 - T8) * S2
            // S1 = K0 * S0 + (1-MB) * S1
            // S2 = (1 - K0) * T8 * S0 + MB * T8 * S1 + T8 * S2
            // S0 + S1 + S2 = 1

            Spell MBAM0 = castingState.getSpell(SpellId.ArcaneMissilesMB);

            float MB = 0.08f * castingState.mageTalents.getMissileBarrage();
            float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
            float K0 = (1 - MB) * (1 - MB) * (1 - MB) * (1 - MB);
            S0 = (MB * T8 - MB) / (K0 * T8 - MB - K0);
            S1 = (K0 * T8 - K0) / (K0 * T8 - MB - K0);
            float S2 = -(MB * T8) / (K0 * T8 - MB - K0);
            K6 = S0 * MB;
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

                Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
                Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
                Spell MBAM4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);
                cycle.addSpell(needsDisplayCalculations, MBAM0, S2);
                cycle.addSpell(needsDisplayCalculations, MBAM2, K6);
                cycle.addSpell(needsDisplayCalculations, MBAM3, K7);
                cycle.addSpell(needsDisplayCalculations, MBAM4, K1 + K2 + K4);
            } else {
                Spell AB = castingState.getSpell(SpellId.ArcaneBlastRaw);
                Solver solver = castingState.getSolver();
                solver.getArcaneBlastTemplate().addToCycle(solver, cycle, AB, K1 + K2 + K3 + K6 + K7, K1 + K2 + K3 + K6 + K7, K1 + K2 + K3 + K7, K1 + K2 + K3, K2 + 2 * K4 + K5);
                solver.getArcaneMissilesTemplate().addToCycle(solver, cycle, MBAM0, S2, 0, K6, K7, K1 + K2 + K4);
            }
        }

        cycle.calculate();
        return cycle;
    }
}
