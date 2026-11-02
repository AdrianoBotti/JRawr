// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABarAM extends Cycle {
    public ABarAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        float MB;
        name = "ABarAM";

        Spell ABar = castingState.getSpell(SpellId.ArcaneBarrage);
        Spell AM = castingState.getSpell(SpellId.ArcaneMissiles);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);

        MB = 0.04f * castingState.mageTalents.getMissileBarrage();

        if (MB == 0.0) {
            addSpell(needsDisplayCalculations, ABar, 1);
            addSpell(needsDisplayCalculations, AM, 1);

            calculate();
        } else {
            addSpell(needsDisplayCalculations, ABar, 1 - MB);
            addSpell(needsDisplayCalculations, AM, 1 - MB);

            addSpell(needsDisplayCalculations, ABar, MB);
            addSpell(needsDisplayCalculations, MBAM, MB);
            if (ABar.castTime + MBAM.castTime < 3.0) addPause(3.0f - ABar.castTime - MBAM.castTime, MB);

            calculate();
        }
    }
}
