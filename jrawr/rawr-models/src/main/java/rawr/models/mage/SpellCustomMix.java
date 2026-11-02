// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class SpellCustomMix extends Cycle {
    public SpellCustomMix(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        name = "Custom Mix";
        if (castingState.calculationOptions.getCustomSpellMix() == null) return;
        for (int i = 0; i < castingState.calculationOptions.getCustomSpellMix().size(); i++) {
            SpellWeight spellWeight = castingState.calculationOptions.getCustomSpellMix().get(i);
            addSpell(needsDisplayCalculations, castingState.getSpell(spellWeight.getSpell()), spellWeight.getWeight());
        }
        calculate();
    }
}
