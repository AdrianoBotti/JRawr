// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class ScourgeStrike extends RuneAbility {
    public ScourgeStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.UnholyFrost);
        setName("Scourge Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = (combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d)
                * combatTable.normalizationFactor)) * 0.7d + 560d + stats.getBonusScourgeStrikeDamage();
        double ssCritDmgMult = 1d + .15d * talents.getViciousStrikes() + stats.getBonusCritMultiplier();
        ssCritDmgMult = 1d + ((combatTable.physCrits + .03d * (talents.getViciousStrikes() + talents.getSubversion())
                + stats.getBonusScourgeStrikeCrit()) * ssCritDmgMult);
        damage *= ssCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    private double secondaryDamage;

    @Override
    public double getSecondaryDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        secondaryDamage = getDamage() * getCalcOpts().getRotation().getAvgDiseaseMult() * .12d;
        double ssCritDmgMult = 1d + .15d * talents.getViciousStrikes() + stats.getBonusCritMultiplier();
        ssCritDmgMult = 1d + ((combatTable.physCrits + .03d * (talents.getViciousStrikes() + talents.getSubversion())
                + stats.getBonusScourgeStrikeCrit()) * ssCritDmgMult);
        ssCritDmgMult = 1d;
        secondaryDamage *= ssCritDmgMult;
        secondaryDamage *= getSecondaryDamageMod();
        return secondaryDamage;
    }
}
