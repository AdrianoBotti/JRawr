// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.models.hunter.HunterEnums.PetAttacks;

public class PetAttackTable extends CombatTable {
    @Override
    protected void calculate(float[] avoidChances) {
        float tableSize = 0f;

        // Miss
        miss = Math.min(1f - tableSize, (useSpellHit ? avoidChances[1] : avoidChances[0]));
        tableSize += miss;
        // Dodge
        if (isWhite || true) { // Abil.CanBeDodged
            dodge = Math.min(1f - tableSize, avoidChances[2]);
            tableSize += dodge;
        } else {
            dodge = 0f;
        }
        // Parry
        if (isWhite || true) { // Abil.CanBeParried
            parry = 0f;
            tableSize += parry;
        } else {
            parry = 0f;
        }
        // Block
        if (isWhite || false) { // Abil.CanBeBlocked
            block = 0f;
            tableSize += block;
        } else {
            block = 0f;
        }
        // Glancing Blow
        if (isWhite) {
            glance = Math.min(1f - tableSize, 0.25f);
            tableSize += glance;
        } else {
            glance = 0f;
        }
        // Critical Hit
        if (isWhite) {
            crit = Math.min(1f - tableSize, statS.getPhysicalCrit());
        } else if (true) { // Abil.CanCrit
            crit = Math.min(1f - tableSize, statS.getPhysicalCrit());
        }
        tableSize += crit;
        // Normal Hit
        hit = Math.max(0f, 1f - tableSize);
        super.calculate();
    }

    protected void initialize(Character character, Stats stats, CalculationOptionsHunter co,
            float[] avoidChances, PetAttacks ability, boolean useSpellHit, boolean alwaysHit) {
        this.character = character;
        this.statS = stats;
        this.calcOpts = co;
        this.combatFactors = null;
        this.petAbil = ability;
        this.isWhite = (petAbil == PetAttacks.None);
        this.useSpellHit = useSpellHit;
        // Start a calc
        if (alwaysHit) calculateAlwaysHit();
        else calculate(avoidChances);
    }

    public PetAttackTable(Character character, Stats stats, CalculationOptionsHunter co,
            float[] avoidChances, boolean useSpellHit, boolean alwaysHit) {
        initialize(character, stats, co, avoidChances, PetAttacks.None, useSpellHit, alwaysHit);
    }

    public PetAttackTable(Character character, Stats stats, CalculationOptionsHunter co,
            float[] avoidChances, PetAttacks ability, boolean useSpellHit, boolean alwaysHit) {
        initialize(character, stats, co, avoidChances, ability, useSpellHit, alwaysHit);
    }
}
