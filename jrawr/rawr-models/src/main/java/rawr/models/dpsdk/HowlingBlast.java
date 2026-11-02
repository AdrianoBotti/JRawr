// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class HowlingBlast extends RuneAbility {
    public HowlingBlast(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Spell);
        setRunes(RunesUsed.UnholyFrost);
        setName("Howling Blast");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = 540 + 0.2d * (1d + talents.getImpurity() * .04d) * stats.getAttackPower();
        double hbCritDmgMult = (0.5d * (2d + (.15d * talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier()));
        hbCritDmgMult = 1d + (Math.min((combatTable.spellCrits), 1d) * hbCritDmgMult);
        damage *= hbCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    private double secondaryDamage;

    @Override
    public double getSecondaryDamage() {
        Stats stats = getStats();
        DeathKnightTalents talents = getTalents();
        secondaryDamage = 540 + 0.2d * (1d + talents.getImpurity() * .04d) * stats.getAttackPower();
        double hbCritDmgMult = (0.5d * (2d + (.15d * talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier()));
        hbCritDmgMult = 1d + hbCritDmgMult;
        secondaryDamage *= hbCritDmgMult;
        secondaryDamage *= getDamageMod();
        return secondaryDamage;
    }
}
