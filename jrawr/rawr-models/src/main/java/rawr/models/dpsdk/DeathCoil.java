// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class DeathCoil extends RuneAbility {
    public DeathCoil(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setGcdType(GCDType.Spell);
        setRunes(RunesUsed.RunicPower);
        setTalents(talents);
        setName("Death Coil");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = 443f + ((0.15d * (1d + (talents.getImpurity() * .04d))) * stats.getAttackPower()) + stats.getBonusDeathCoilDamage();
        double dcCritDmgMult = 0.5f * (2f + stats.getBonusCritMultiplier());
        double dcCrit = 1d + ((combatTable.spellCrits + stats.getBonusDeathCoilCrit()) * dcCritDmgMult);
        damage *= dcCrit;
        return damage * getDamageMod();
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
