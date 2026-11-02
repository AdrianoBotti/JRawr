// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class AB2AM {
    private AB2AM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "AB2AM";

        if (castingState.baseStats.getMage2T10() > 0) {
            // doesn't support haste transferring over several loops

            Spell mbam2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell am2 = castingState.getSpell(SpellId.ArcaneMissiles2);
            Spell mbam2H = castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB2);

            float m2T10time = 5.0f - castingState.calculationOptions.getLatencyChannel();
            Spell ab0A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast0) : castingState.getSpell(SpellId.ArcaneBlast0);
            m2T10time -= ab0A.castTime;
            Spell ab1A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneBlast1) : castingState.getSpell(SpellId.ArcaneBlast1);
            m2T10time -= ab1A.castTime;
            Spell mbam2A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissilesMB2) : castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell am2A = (m2T10time > 0.0f) ? castingState.getTier10TwoPieceState().getSpell(SpellId.ArcaneMissiles2) : castingState.getSpell(SpellId.ArcaneMissiles2);

            Spell ab0C = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell ab1C = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell mbam2C = castingState.getSpell(SpellId.ArcaneMissilesMB2);
            Spell am2C = castingState.getSpell(SpellId.ArcaneMissiles2);

            float mb = 0.08f * castingState.mageTalents.getMissileBarrage();
            float mc = 1 - mb;
            float k0 = (1 - mb) * (1 - mb);

            float s0 = 1 - k0;
            float s1 = k0;

            cycle.addSpell(needsDisplayCalculations, ab0A, s0);
            cycle.addSpell(needsDisplayCalculations, ab1A, s0);
            cycle.addSpell(needsDisplayCalculations, mbam2A, s0 * (1 - k0));
            cycle.addSpell(needsDisplayCalculations, am2A, s0 * k0);

            cycle.addSpell(needsDisplayCalculations, ab0C, s1);
            cycle.addSpell(needsDisplayCalculations, ab1C, s1);
            cycle.addSpell(needsDisplayCalculations, mbam2C, s1 * (1 - k0));
            cycle.addSpell(needsDisplayCalculations, am2C, s1 * k0);
        } else {
            Spell am2 = castingState.getSpell(SpellId.ArcaneMissiles2);
            Spell mbam2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);

            float mb = 0.08f * castingState.mageTalents.getMissileBarrage();
            float t8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
            float k1 = (1 - mb) * (1 - mb);
            float s0 = (1 - t8) / (1 - k1 * t8);

            if (needsDisplayCalculations) {
                Spell ab0 = castingState.getSpell(SpellId.ArcaneBlast0);
                Spell ab1 = castingState.getSpell(SpellId.ArcaneBlast1);
                cycle.addSpell(needsDisplayCalculations, ab0, 1);
                cycle.addSpell(needsDisplayCalculations, ab1, 1);
            } else {
                Spell ab = castingState.getSpell(SpellId.ArcaneBlastRaw);
                castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, ab, 1, 1, 0, 0);
            }
            cycle.addSpell(needsDisplayCalculations, am2, s0 * k1);
            cycle.addSpell(needsDisplayCalculations, mbam2, 1 - s0 * k1);
        }

        cycle.calculate();
        return cycle;
    }
}
