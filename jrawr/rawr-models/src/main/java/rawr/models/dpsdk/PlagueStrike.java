// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class PlagueStrike extends RuneAbility {
    public PlagueStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.Unholy);
        setName("Plague Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = (combatTable.MH.baseDamage + ((stats.getAttackPower() / 14f)
                * combatTable.normalizationFactor) + 378f) * 0.5f;
        damage += ((combatTable.DW ? ((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14f)
                * combatTable.normalizationFactor) + 378f) * 0.5f) : 0d) * talents.getThreatOfThassarian() / 3d)
                * (0.5d * 1d + talents.getNervesOfColdSteel() * .0833333333d);
        double psCritDmgMult = 1d + (.15d * talents.getViciousStrikes()) + stats.getBonusCritMultiplier();
        psCritDmgMult = 1d + ((combatTable.physCrits + (.03d * talents.getViciousStrikes())
                + stats.getBonusPlagueStrikeCrit()) * psCritDmgMult);
        damage *= psCritDmgMult;
        return damage * getDamageMod();
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
