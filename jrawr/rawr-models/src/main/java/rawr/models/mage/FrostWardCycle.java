// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FrostWardCycle {
    private FrostWardCycle() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState, Cycle baseCycle) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "Frost Ward+" + baseCycle.name;

        Spell frostWard = castingState.getSpell(SpellId.FrostWard);

        // 1 ward every 30 seconds
        cycle.addSpell(needsDisplayCalculations, frostWard, 1);
        cycle.addCycle(needsDisplayCalculations, baseCycle, (30 - frostWard.castTime) / baseCycle.castTime);
        cycle.calculate();

        return cycle;
    }
}
