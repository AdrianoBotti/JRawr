// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.HitResult;
import rawr.base.items.ItemType;
import rawr.base.stats.Stats;

public class DefendTable extends CombatTable {
    @Override
    protected void calculate() {
        float tableSize = 0.0f;

        // Miss
        setMiss(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, HitResult.Miss, options.getTargetLevel())));
        tableSize += getMiss();
        // Dodge
        setDodge(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, HitResult.Dodge, options.getTargetLevel())));
        tableSize += getDodge();
        // Parry
        setParry(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, HitResult.Parry, options.getTargetLevel())));
        tableSize += getParry();
        // Block
        if (character.get(rawr.base.character.CharacterSlot.OffHand) != null
                && character.get(rawr.base.character.CharacterSlot.OffHand).getType() == ItemType.Shield) {
            setBlock(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, HitResult.Block, options.getTargetLevel())));
            tableSize += getBlock();
        }
        // Critical Hit
        setCritical(Math.min(1.0f - tableSize, Lookup.targetCritChance(character, stats, options.getTargetLevel())));
        tableSize += getCritical();
        // Normal Hit
        setHit(Math.max(0.0f, 1.0f - tableSize));
    }

    public DefendTable(Character character, Stats stats, CalculationOptionsProtWarr options) {
        initialize(character, stats, options, Ability.None);
    }
}
