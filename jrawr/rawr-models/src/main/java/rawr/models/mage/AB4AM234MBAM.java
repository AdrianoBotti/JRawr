// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class AB4AM234MBAM {
    private AB4AM234MBAM() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "AB4AM234MBAM";

        Spell am4 = castingState.getSpell(SpellId.ArcaneMissiles4);
        Spell mbam2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        Spell mbam3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        Spell mbam4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);

        float mb = 0.08f * castingState.mageTalents.getMissileBarrage();
        float t8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float k0 = (1 - mb) * (1 - mb) * (1 - mb) * (1 - mb);
        float s0 = (1 - t8) / (1 - k0 * t8);
        float s1 = (1 - k0) * t8 / (1 - k0 * t8);
        float k1 = s0 * k0;
        float k2 = s0 * (1 - mb) * (1 - mb) * (1 - (1 - mb) * (1 - mb));
        float k3 = s0 * (1 - mb) * mb;
        float k4 = s0 * mb + s1;

        if (needsDisplayCalculations) {
            Spell ab0 = castingState.getSpell(SpellId.ArcaneBlast0);
            Spell ab1 = castingState.getSpell(SpellId.ArcaneBlast1);
            Spell ab2 = castingState.getSpell(SpellId.ArcaneBlast2);
            Spell ab3 = castingState.getSpell(SpellId.ArcaneBlast3);
            cycle.addSpell(needsDisplayCalculations, ab0, 1);
            cycle.addSpell(needsDisplayCalculations, ab1, 1);
            cycle.addSpell(needsDisplayCalculations, ab2, k1 + k2 + k3);
            cycle.addSpell(needsDisplayCalculations, ab3, k1 + k2);
        } else {
            Spell ab = castingState.getSpell(SpellId.ArcaneBlastRaw);
            castingState.getSolver().getArcaneBlastTemplate().addToCycle(castingState.getSolver(), cycle, ab, 1, 1, k1 + k2 + k3, k1 + k2);
        }
        cycle.addSpell(needsDisplayCalculations, am4, k1);
        cycle.addSpell(needsDisplayCalculations, mbam4, k2);
        cycle.addSpell(needsDisplayCalculations, mbam3, k3);
        cycle.addSpell(needsDisplayCalculations, mbam2, k4);

        cycle.calculate();
        return cycle;
    }
}
