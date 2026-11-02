// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class DeathStrike extends RuneAbility {
    public DeathStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.UnholyFrost);
        setName("Death Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d) * combatTable.normalizationFactor) + 297) * 0.75d) + stats.getBonusDeathStrikeDamage();
        damage += ((!combatTable.DW ? 0 : (((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14d) * combatTable.normalizationFactor) + 297) * 0.75d) * 0.5d) + stats.getBonusDeathStrikeDamage())
                * (1d + (talents.getNervesOfColdSteel() * .0833333333333d))) * talents.getThreatOfThassarian() / 3d;
        double dsCritDmgMult = 1d + (.15d * talents.getMightOfMograine()) + stats.getBonusCritMultiplier();
        dsCritDmgMult = 1d + (combatTable.physCrits
                + .03d * talents.getImprovedDeathStrike()
                + stats.getBonusDeathStrikeCrit()) * dsCritDmgMult;
        damage *= dsCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
