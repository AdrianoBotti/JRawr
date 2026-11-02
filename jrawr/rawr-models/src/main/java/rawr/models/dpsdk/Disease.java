// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class Disease extends RuneAbility {
    public Disease(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.None);
        setRunes(RunesUsed.None);
        setName("A Disease");
    }

    private int duration = 0;

    public int getDuration() {
        return (duration == 0 ? getTalents().getEpidemic() * 3 + 15 : duration);
    }

    public void setDuration(int value) {
        duration = value;
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        DeathKnightTalents talents = getTalents();
        damage = stats.getAttackPower() * .055 * 1.15 * (1d + talents.getImpurity() * .04d);
        damage *= getDamageMod();
        return damage;
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
