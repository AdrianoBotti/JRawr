// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABABarCSc extends Cycle {
    public ABABarCSc(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        Cycle ABABarC;
        Spell Sc;
        float X;
        name = "ABABarCSc";
        providesScorch = true;

        ABABarC = castingState.getCycle(CycleId.ABABar1C);
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

        X = gap * Sc.castTime / (gap * Sc.castTime + ABABarC.castTime * (1 - gap));

        addCycle(needsDisplayCalculations, ABABarC, X);
        addSpell(needsDisplayCalculations, Sc, 1 - X);
        calculate();
    }
}
