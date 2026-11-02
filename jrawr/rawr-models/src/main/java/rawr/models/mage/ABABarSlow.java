// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABarSlow extends Cycle {
    public ABABarSlow(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float MB;
        float X;
        float S0;
        float S1;
        name = "ABABarSlow";

        Spell AB = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell ABar = castingState.getMaintainSnareState().getSpell(SpellId.ArcaneBarrage);
        Spell MBAM = castingState.getMaintainSnareState().getSpell(SpellId.ArcaneMissilesMB);
        Spell Slow = castingState.getSpell(SpellId.Slow);

        MB = 0.04f * castingState.mageTalents.getMissileBarrage();

        S0 = (1 - MB) / (1 + (1 - MB) * MB);
        S1 = (2 - MB) * MB / (1 + (1 - MB) * MB);
        float Pause = 0.0f;
        if (AB.castTime + ABar.castTime < 3.0) Pause = 3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime;
        X = (S0 * (AB.castTime + ABar.castTime) + S1 * (MBAM.castTime + ABar.castTime) + Pause) / (15.0f - Slow.castTime + Pause);

        addSpell(needsDisplayCalculations, ABar, (1 - X) * S0);
        addSpell(needsDisplayCalculations, AB, (1 - X) * S0);
        if (AB.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime, (1 - X) * S0);

        addSpell(needsDisplayCalculations, ABar, (1 - X) * S1);
        addSpell(needsDisplayCalculations, MBAM, (1 - X) * S1);
        if (MBAM.castTime + ABar.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - MBAM.castTime - ABar.castTime, (1 - X) * S1);

        addSpell(needsDisplayCalculations, ABar, X * S0);
        addSpell(needsDisplayCalculations, AB, X * S0);
        addSpell(needsDisplayCalculations, Slow, X * S0);
        if (AB.castTime + ABar.castTime + Slow.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - AB.castTime - ABar.castTime - Slow.castTime, X * S0);

        addSpell(needsDisplayCalculations, ABar, X * S1);
        addSpell(needsDisplayCalculations, MBAM, X * S1);
        addSpell(needsDisplayCalculations, Slow, X * S1);
        if (MBAM.castTime + ABar.castTime + Slow.castTime < 3.0) addPause(3.0f + castingState.calculationOptions.getLatencyGCD() - MBAM.castTime - ABar.castTime - Slow.castTime, X * S1);

        calculate();
    }
}
