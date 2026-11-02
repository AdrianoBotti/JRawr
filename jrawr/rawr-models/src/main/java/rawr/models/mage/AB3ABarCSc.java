// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AB3ABarCSc extends Cycle {
    public AB3ABarCSc(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        Cycle AB3ABarC;
        Spell Sc;
        float X;
        name = "AB3ABarCSc";
        providesScorch = true;

        AB3ABarC = castingState.getCycle(CycleId.AB3ABar3C);
        Sc = castingState.getSpell(SpellId.Scorch);

        int averageScorchesNeeded = (int) Math.ceil(3f / (float) castingState.mageTalents.getImprovedScorch());
        int extraScorches = 1;
        if (Sc.getHitRate() >= 1.0) extraScorches = 0;
        if (castingState.mageTalents.isGlyphOfImprovedScorch()) {
            averageScorchesNeeded = 1;
            extraScorches = 0;
        }

        float gap = (30.0f - (averageScorchesNeeded + extraScorches) * Sc.castTime) / (30.0f - extraScorches * Sc.castTime);
        if (castingState.mageTalents.getImprovedScorch() == 0) {
            providesScorch = false;
            gap = 1.0f;
        }

        X = gap * Sc.castTime / (gap * Sc.castTime + AB3ABarC.castTime * (1 - gap));

        addCycle(needsDisplayCalculations, AB3ABarC, X);
        addSpell(needsDisplayCalculations, Sc, 1 - X);
        calculate();
    }
}
