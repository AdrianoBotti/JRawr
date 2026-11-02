// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AB2ABarMBAM extends Cycle {
    public AB2ABarMBAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float K1, K2, K3, K4;
        name = "AB2ABarMBAM";

        Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell ABar2 = castingState.getSpell(SpellId.ArcaneBarrage2);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);
        Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);

        float MB = 0.04f * castingState.mageTalents.getMissileBarrage();

        K1 = (1 - MB) * (1 - MB) * (1 - MB);
        K2 = (1 - MB) * (1 - (1 - MB) * (1 - MB));
        K3 = MB * (1 - MB);
        K4 = MB * MB;

        addSpell(needsDisplayCalculations, AB0, K1);
        addSpell(needsDisplayCalculations, AB1, K1);
        if (AB0.castTime + AB1.castTime + ABar2.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB0.castTime - AB1.castTime - ABar2.castTime, K1);
        addSpell(needsDisplayCalculations, ABar2, K1);

        addSpell(needsDisplayCalculations, AB0, K2);
        addSpell(needsDisplayCalculations, AB1, K2);
        if (AB0.castTime + AB1.castTime + ABar2.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB0.castTime - AB1.castTime - ABar2.castTime, K2);
        addSpell(needsDisplayCalculations, ABar2, K2);
        addSpell(needsDisplayCalculations, MBAM, K2);

        addSpell(needsDisplayCalculations, AB0, K3);
        addSpell(needsDisplayCalculations, AB1, K3);
        addSpell(needsDisplayCalculations, MBAM2, K3);
        if (AB0.castTime + AB1.castTime + MBAM2.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB0.castTime - AB1.castTime - ABar.castTime - MBAM2.castTime, K3);
        addSpell(needsDisplayCalculations, ABar, K3);

        addSpell(needsDisplayCalculations, AB0, K4);
        addSpell(needsDisplayCalculations, AB1, K4);
        addSpell(needsDisplayCalculations, MBAM2, K4);
        if (AB0.castTime + AB1.castTime + MBAM2.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB0.castTime - AB1.castTime - ABar.castTime - MBAM2.castTime, K4);
        addSpell(needsDisplayCalculations, ABar, K4);
        addSpell(needsDisplayCalculations, MBAM, K4);

        calculate();
    }
}
