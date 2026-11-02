// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class FrostStrike extends RuneAbility {
    public FrostStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.RunicPower);
        setName("Frost Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d)
                * combatTable.normalizationFactor)) + 250d) * 0.55d + stats.getBonusFrostStrikeDamage();
        damage += ((!combatTable.DW ? 0 : (((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14d)
                * combatTable.normalizationFactor)) + 250d) * 0.55d * 0.5d) + stats.getBonusFrostStrikeDamage())
                * (1d + (talents.getNervesOfColdSteel() * .083333333333d))) * talents.getThreatOfThassarian() / 3d;
        double fsCritDmgMult = 1d + .15d * talents.getGuileOfGorefiend() + stats.getBonusCritMultiplier();
        fsCritDmgMult = 1d + (Math.min((combatTable.physCrits + stats.getBonusFrostStrikeCrit()), 1d) * fsCritDmgMult);
        damage *= fsCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    private double secondaryDamage;

    @Override
    public double getSecondaryDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        secondaryDamage = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d)
                * combatTable.normalizationFactor)) + 250d) * 0.55f + stats.getBonusFrostStrikeDamage();
        secondaryDamage += ((!combatTable.DW ? 0 : (((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14d)
                * combatTable.normalizationFactor)) + 250d) * 0.55d * 0.5d) + stats.getBonusFrostStrikeDamage())
                * (1d + (talents.getNervesOfColdSteel() * .08333333333333d))) * talents.getThreatOfThassarian() / 3d;
        double fsCritDmgMult = 1d + .15d * talents.getGuileOfGorefiend() + stats.getBonusCritMultiplier();
        fsCritDmgMult = 1d + fsCritDmgMult;
        secondaryDamage *= fsCritDmgMult;
        secondaryDamage *= getDamageMod();
        return secondaryDamage;
    }
}
