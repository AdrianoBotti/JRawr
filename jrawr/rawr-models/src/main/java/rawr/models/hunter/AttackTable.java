// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the Skills.Ability overload is omitted (see CombatTable.java). Only the white ranged
// weapon table is instantiated; the non-white branch therefore reads the null-ability defaults
// (CanBeDodged/CanBeParried/CanBeBlocked = true, CanCrit = true, ContainCritValue_RW = _c_rwycrit).
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class AttackTable extends CombatTable {
    @Override
    protected void calculate() {
        float tableSize = 0f;

        // Miss
        if (useSpellHit) {
            miss = Math.min(1f - tableSize, Math.max(0.17f - (StatConversion.getHitFromRating(statS.getHitRating(), character.getCharacterClass()) + statS.getSpellHit()), 0f));
        } else {
            miss = Math.min(1f - tableSize, isWhite ? combatFactors.cWhMiss : combatFactors.cYwMiss);
        }
        tableSize += miss;
        // Dodge
        if (isWhite) {
            dodge = 0f;
            tableSize += dodge;
        } else {
            dodge = 0f;
        }
        // Parry
        if (isWhite) {
            parry = 0f;
            tableSize += parry;
        } else {
            parry = 0f;
        }
        // Block
        if (isWhite) {
            block = 0f;
            tableSize += block;
        } else {
            block = 0f;
        }
        // Glancing Blow
        if (isWhite) {
            glance = Math.min(1f - tableSize, combatFactors.cGlance);
            tableSize += glance;
        } else {
            glance = 0f;
        }
        // Critical Hit
        if (isWhite) {
            crit = Math.min(1f - tableSize, combatFactors.cRwYwCrit);
            tableSize += crit;
        } else {
            crit = Math.min(1f - tableSize, combatFactors.cRwYwCrit);
            tableSize += crit;
        }
        // Normal Hit
        hit = Math.max(0f, 1f - tableSize);
        super.calculate();
    }

    public AttackTable(Character character, Stats stats, CombatFactors cf, CalculationOptionsHunter co,
            boolean useSpellHit, boolean alwaysHit) {
        initialize(character, stats, cf, co, useSpellHit, alwaysHit);
    }
}
