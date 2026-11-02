// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.stats.HitResult;
import rawr.base.stats.Stats;

public class AttackTable extends CombatTable {
    @Override
    protected void calculate() {
        float tableSize = 0.0f;

        int targetLevel = calcOpts.getTargetLevel();

        float spellHitChance = Lookup.spellHitChance(character, stats, targetLevel);
        float bonusExpertise = Lookup.bonusExpertisePercentage(character, stats);

        if (Lookup.isSpell(ability)) {
            // Miss
            setMiss(Math.min(1.0f - tableSize, 1.0f - spellHitChance));// - bonusHit));
            tableSize += getMiss();
            // Crit
            setCritical(Lookup.spellCritChance(character, stats, targetLevel));
            tableSize += getCritical();
        } else {
            float bonusHit = Lookup.hitChance(character, stats, targetLevel);

            // Miss
            // Miss = Math.Min(1.0f - tableSize, Math.Max(0.0f, Lookup.TargetAvoidanceChance(Character, Stats, HitResult.Miss) - bonusHit));
            setMiss(Math.min(1.0f - tableSize, Math.max(0.0f, 1.0f - bonusHit)));
            tableSize += getMiss();
            // Avoidance
            if (Lookup.isAvoidable(ability)) {
                // Dodge
                setDodge(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Dodge, targetLevel) - bonusExpertise)));
                tableSize += getDodge();
                // Parry
                setParry(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Parry, targetLevel) - bonusExpertise)));
                tableSize += getParry();
            }
            // Glancing Blow
            if (ability == Ability.None) {
                setGlance(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Glance, targetLevel))));
                tableSize += getGlance();
            }
            // Block
            if (ability == Ability.None || ability == Ability.HammerOfTheRighteous) {
                setBlock(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Block, targetLevel))));
                tableSize += getBlock();
            }
            // Critical Hit
            setCritical(Math.min(1.0f - tableSize, Lookup.bonusCritPercentage(character, stats, ability, targetLevel, calcOpts.getTargetType())));
            tableSize += getCritical();//FIXME: Tablesize must not change when critchance is negative (boss)
        }

        // Normal Hit
        setHit(Math.max(0.0f, 1.0f - tableSize));

        // Partial Resist TODO: Partial Resists don't belong in the combat table, they're not an avoidance type but a damage multiplier.
        if (Lookup.hasPartials(ability)) {
            setResist(Math.min(1.0f - tableSize, Math.max(0.0f, Lookup.targetAvoidanceChance(character, stats, HitResult.Resist, targetLevel))));
        }
    }

    public AttackTable(Character character, Stats stats, CalculationOptionsProtPaladin calcOpts) {
        initialize(character, stats, Ability.None, calcOpts);
    }

    public AttackTable(Character character, Stats stats, Ability ability, CalculationOptionsProtPaladin calcOpts) {
        initialize(character, stats, ability, calcOpts);
    }
}
