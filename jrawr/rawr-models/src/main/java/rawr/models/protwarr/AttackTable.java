// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.HitResult;
import rawr.base.stats.Stats;

public class AttackTable extends CombatTable {
    @Override
    protected void calculate() {
        float tableSize = 0.0f;
        float bonusHit = Lookup.bonusHitPercentage(character, stats);
        float bonusExpertise = Lookup.bonusExpertisePercentage(character, stats);

        // Miss
        setMiss(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Miss, options.getTargetLevel()) - bonusHit)));
        tableSize += getMiss();
        // Avoidance
        if (Lookup.isAvoidable(ability)) {
            // Dodge
            setDodge(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Dodge, options.getTargetLevel()) - bonusExpertise)));
            tableSize += getDodge();
            // Parry
            setParry(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Parry, options.getTargetLevel()) - bonusExpertise)));
            tableSize += getParry();
        }
        if (ability == Ability.None) {
            // White Glancing Blow
            setGlance(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Glance, options.getTargetLevel()))));
            tableSize += getGlance();

            // White Critical Hits
            setCritical(Math.max(0.0f, Math.min(1.0f - tableSize, Lookup.bonusCritPercentage(character, stats, ability, options.getTargetLevel()))
                    - Lookup.targetAvoidanceChance(character, stats, HitResult.Crit, options.getTargetLevel())));
            tableSize += getCritical();
        } else {
            // Yellow Critical Hits
            setCritical(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.bonusCritPercentage(character, stats, ability, options.getTargetLevel())
                    - Lookup.targetAvoidanceChance(character, stats, HitResult.Crit, options.getTargetLevel()))));
            tableSize += getCritical();
        }
        // Normal Hit
        setHit(Math.max(0.0f, 1.0f - tableSize));
    }

    public AttackTable(Character character, Stats stats, CalculationOptionsProtWarr options) {
        initialize(character, stats, options, Ability.None);
    }

    public AttackTable(Character character, Stats stats, CalculationOptionsProtWarr options, Ability ability) {
        initialize(character, stats, options, ability);
    }
}
