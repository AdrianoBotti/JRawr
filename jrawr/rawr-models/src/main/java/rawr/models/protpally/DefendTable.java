// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.items.ItemType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class DefendTable extends CombatTable {
    private boolean useHolyShield;

    @Override
    protected void calculate() {
        // Hack to not count holy shield when we are trying to calculate crit chance without it
        if (!useHolyShield && calcOpts.getUseHolyShield()) {
            Stats negativeBlock = new Stats();
            negativeBlock.setBlock(-0.3f);
            stats.accumulate(negativeBlock);
        }

        float tableSize = 0.0f;

        int targetLevel = calcOpts.getTargetLevel();

        // Miss
        setMiss(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, rawr.base.stats.HitResult.Miss, targetLevel)));
        tableSize += getMiss();
        // Dodge
        setDodge(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, rawr.base.stats.HitResult.Dodge, targetLevel)));
        tableSize += getDodge();
        // Parry
        setParry(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, rawr.base.stats.HitResult.Parry, targetLevel)));
        tableSize += getParry();
        // Block
        rawr.base.items.ItemInstance offHand = character.get(rawr.base.character.CharacterSlot.OffHand);
        if (offHand != null && offHand.getType() == ItemType.Shield) {
            setBlock(Math.min(1.0f - tableSize, Lookup.avoidanceChance(character, stats, rawr.base.stats.HitResult.Block, targetLevel)));
            tableSize += getBlock();
        }
        // Critical Hit
        setCritical(Math.min(1.0f - tableSize, Lookup.targetCritChance(character, stats, targetLevel)));
        tableSize += getCritical();
        // Normal Hit
        setHit(Math.max(0.0f, 1.0f - tableSize));
        // Partial Resists don't belong in the combat table
        setResist(1.0f - StatConversion.getResistanceTable(targetLevel, character.getLevel(), stats.getFrostResistance(), 0.0f)[0]);

        // Hack to put back holy shield when we are trying to calculate crit chance without it
        if (!useHolyShield && calcOpts.getUseHolyShield()) {
            Stats positiveBlock = new Stats();
            positiveBlock.setBlock(0.3f);
            stats.accumulate(positiveBlock);
        }
    }

    public DefendTable(Character character, Stats stats, CalculationOptionsProtPaladin calcOpts, boolean useHolyShield) {
        this.useHolyShield = useHolyShield;
        initialize(character, stats, Ability.None, calcOpts);
    }
}
