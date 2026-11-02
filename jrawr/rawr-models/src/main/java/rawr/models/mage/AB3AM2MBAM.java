// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AB3AM2MBAM extends Cycle {
    public AB3AM2MBAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        name = "AB3AM2MBAM";

        // S0: no proc
        // AB0-AB1-AB2-AM3    => S0   (1 - MB) * (1 - MB) * (1 - MB)
        // AB0-AB1-AB2-MBAM3   => S0   (1 - MB) * (1 - (1 - MB) * (1 - MB)) * (1 - T8)
        // AB0-AB1-AB2-MBAM3   => S1   (1 - MB) * (1 - (1 - MB) * (1 - MB)) * T8
        // AB0-AB1-MBAM2   => S0   MB * (1 - T8)
        // AB0-AB1-MBAM2   => S1   MB * T8

        // S1: proc
        // AB0-AB1-MBAM2   => S0   (1 - T8)
        // AB0-AB1-MBAM2   => S1   T8

        // S1 = S0 * (1 - (1 - MB) * (1 - MB) * (1 - MB)) * T8 + S1 * T8
        // S0 + S1 = 1

        Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
        Spell AM3 = castingState.getSpell(SpellId.ArcaneMissiles3);
        Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        float T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        float K1 = (1 - MB) * (1 - MB) * (1 - MB);
        float K2 = (1 - MB) * (1 - (1 - MB) * (1 - MB));
        float S0 = (1 - T8) / (1 - K1 * T8);

        addSpell(needsDisplayCalculations, AB0, 1);
        addSpell(needsDisplayCalculations, AB1, 1);
        addSpell(needsDisplayCalculations, AB2, S0 * (1 - MB));
        addSpell(needsDisplayCalculations, AM3, S0 * K1);
        addSpell(needsDisplayCalculations, MBAM3, S0 * K2);
        addSpell(needsDisplayCalculations, MBAM2, 1 - S0 * (1 - MB));

        calculate();
    }
}
