// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class IcyTouch extends RuneAbility {
    public IcyTouch(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setGcdType(GCDType.Spell);
        setRunes(RunesUsed.Frost);
        setTalents(talents);
        setName("Icy Touch");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = 236f + ((0.1d * (1d + talents.getImpurity() * .04d)) * stats.getAttackPower()) + stats.getBonusIcyTouchDamage();
        double itCritDmgMult = .5f * (2f + stats.getCritBonusDamage() + stats.getBonusCritMultiplier());
        double itCrit = 1f + (Math.min((combatTable.spellCrits + (.05f * (float) talents.getRime())), 1f) * itCritDmgMult);
        damage *= itCrit;
        return damage * getDamageMod();
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
