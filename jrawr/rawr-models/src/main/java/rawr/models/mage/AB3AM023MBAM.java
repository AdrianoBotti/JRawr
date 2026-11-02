// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class AB3AM023MBAM {
    private AB3AM023MBAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "AB3AM023MBAM";

        if (castingState.baseStats.getMage2T10() > 0) {
            // doesn't support haste transferring over several loops

            Spell mbam3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
            Spell am3 = castingState.getSpell(SpellId.ArcaneMissiles3);
            Spell mbam2 = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB2);

            float m2T10time = 5.0f - castingState.calculationOptions.getLatencyChannel();
            Spell ab0A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast0) : castingState.getSpell(SpellId.ArcaneBlast0);
            m2T10time -= ab0A.castTime;
            Spell ab1A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast1) : castingState.getSpell(SpellId.ArcaneBlast1);
            m2T10time -= ab1A.castTime;
            Spell mbam2A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB2) : castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell ab2A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast2) : castingState.getSpell(SpellId.ArcaneBlast2);
            m2T10time -= ab2A.castTime;
            Spell mbam3A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB3) : castingState.getSpell(SpellId.ArcaneMissilesMB3);
            Spell am3A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissiles3) : castingState.getSpell(SpellId.ArcaneMissiles3);

            Spell ab0C = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell ab1C = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell mbam2C = castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell ab2C = castingState.getSpell(SpellId.ArcaneBlast2);
            Spell mbam3C = castingState.getSpell(SpellId.ArcaneMissilesMB3);
            Spell am3C = castingState.getSpell(SpellId.ArcaneMissiles3);

            float mb = 0.08f * castingState.mageTalents.getMissileBarrage();
            float mc = 1 - mb;
            float k0 = (1 - mb) * (1 - mb) * (1 - mb);
            float k2 = (1 - mb) * (1 - (1 - mb) * (1 - mb));

            float s0 = 1 - k0;
            float s1 = k0;

            cycle.addSpell(needsDisplayCalculations, ab0A, s0);
            cycle.addSpell(needsDisplayCalculations, ab1A, s0);
            cycle.addSpell(needsDisplayCalculations, ab2A, s0 * mc);
            cycle.addSpell(needsDisplayCalculations, mbam2A, s0 * mb);
            cycle.addSpell(needsDisplayCalculations, mbam3A, s0 * k2);
            cycle.addSpell(needsDisplayCalculations, am3A, s0 * k0);

            cycle.addSpell(needsDisplayCalculations, ab0C, s1);
            cycle.addSpell(needsDisplayCalculations, ab1C, s1);
            cycle.addSpell(needsDisplayCalculations, ab2C, s1 * mc);
            cycle.addSpell(needsDisplayCalculations, mbam2C, s1 * mb);
            cycle.addSpell(needsDisplayCalculations, mbam3C, s1 * k2);
            cycle.addSpell(needsDisplayCalculations, am3C, s1 * k0);
        } else {
            Spell am3 = castingState.getSpell(SpellId.ArcaneMissiles3);
            Spell mbam0 = castingState.getSpell(SpellId.ArcaneMissilesMB);
            Spell mbam2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell mbam3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);

            float mb = 0.08f * castingState.mageTalents.getMissileBarrage();
            float t8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
            float k0 = (1 - mb) * (1 - mb) * (1 - mb);
            float s0 = (1 - t8) / (1 - k0 * t8);
            float s1 = (1 - k0) * t8 / (1 - k0 * t8);
            float k1 = s0 * k0;
            float k2 = s0 * (1 - mb) * (1 - (1 - mb) * (1 - mb));
            float k4 = s0 * mb;

            if (needsDisplayCalculations) {
                Spell ab0 = castingState.getSpell(SpellId.ArcaneBlast0);
                Spell ab1 = castingState.getSpell(SpellId.ArcaneBlast1);
                Spell ab2 = castingState.getSpell(SpellId.ArcaneBlast2);
                cycle.addSpell(needsDisplayCalculations, ab0, s0);
                cycle.addSpell(needsDisplayCalculations, ab1, s0);
                cycle.addSpell(needsDisplayCalculations, ab2, k1 + k2);
            } else {
                Spell ab = castingState.getSpell(SpellId.ArcaneBlastRaw);
                castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, ab, s0, s0, k1 + k2, 0);
            }
            cycle.addSpell(needsDisplayCalculations, am3, k1);
            cycle.addSpell(needsDisplayCalculations, mbam3, k2);
            cycle.addSpell(needsDisplayCalculations, mbam2, k4);
            cycle.addSpell(needsDisplayCalculations, mbam0, s1);
        }

        cycle.calculate();
        return cycle;
    }
}
