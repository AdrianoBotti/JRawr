// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class BloodBoil extends RuneAbility {
    public BloodBoil(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Spell);
        setRunes(RunesUsed.Blood);
        setName("Blood Boil");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        if (getCalcOpts().getRotation().getAvgDiseaseMult() > 0) {
            damage = 295d + 0.095d * (1d + talents.getImpurity() * .04d) * stats.getAttackPower();
        } else {
            damage = 200d + 0.06d * (1d + talents.getImpurity() * .04d) * stats.getAttackPower();
        }
        double bloodBoilCritMult = .5d * (2d + stats.getBonusCritMultiplier());
        bloodBoilCritMult = 1d + combatTable.spellCrits * bloodBoilCritMult;
        damage *= bloodBoilCritMult;
        damage *= getDamageMod();
        return damage;
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
