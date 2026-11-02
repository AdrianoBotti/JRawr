// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class Obliterate extends RuneAbility {
    public Obliterate(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.UnholyFrost);
        setName("Obliterate");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d) * combatTable.normalizationFactor) + 584d)
                * (talents.isGlyphofObliterate() ? 1 : 0.8d)) + stats.getBonusObliterateDamage();
        damage += ((!combatTable.DW ? 0 : (((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14d) * combatTable.normalizationFactor) + 584f)
                * (talents.isGlyphofObliterate() ? 1 : 0.8d)) * 0.5d) + stats.getBonusObliterateDamage())
                * (1d + (talents.getNervesOfColdSteel() * .08333333333d))) * talents.getThreatOfThassarian() / 3d;
        damage *= 1f + (talents.isGlyphofObliterate() ? 0.1d : 0.125d) * getCalcOpts().getRotation().getAvgDiseaseMult() * (1f + stats.getBonusPerDiseaseObliterateDamage());
        double oblitCritDmgMult = 1d + (.15d * talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier();
        oblitCritDmgMult = 1d + (combatTable.physCrits
                + .03d * talents.getSubversion()
                + .05d * talents.getRime()
                + stats.getBonusObliterateCrit()) * oblitCritDmgMult;
        damage *= oblitCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
