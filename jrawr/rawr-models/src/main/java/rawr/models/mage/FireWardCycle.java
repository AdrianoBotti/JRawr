// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class FireWardCycle {
    private FireWardCycle() {
    }

    public static Cycle getCycle(boolean needsDisplayCalculations, CastingState castingState, Cycle baseCycle) {
        Cycle cycle = Cycle.createNew(needsDisplayCalculations, castingState);
        cycle.name = "Fire Ward+" + baseCycle.name;

        Spell fireWard = castingState.getSpell(SpellId.FireWard);

        // 1 ward every 30 seconds
        cycle.addSpell(needsDisplayCalculations, fireWard, 1);
        cycle.addCycle(needsDisplayCalculations, baseCycle, (30 - fireWard.castTime) / baseCycle.castTime);
        cycle.calculate();

        return cycle;
    }
}
