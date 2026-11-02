// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class HeartStrike extends RuneAbility {
    public HeartStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setTalents(talents);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.Blood);
        setName("Heart Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        damage = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14f) * combatTable.normalizationFactor) + 736f)
                * 0.5f) + stats.getBonusHeartStrikeDamage();
        damage *= 1d + 0.1d * getCalcOpts().getRotation().getAvgDiseaseMult() * (1d + stats.getBonusPerDiseaseHeartStrikeDamage());
        double hsCritDmgMult = 1d + (.15d * talents.getMightOfMograine()) + stats.getBonusCritMultiplier();
        hsCritDmgMult = 1d + ((combatTable.physCrits + (.03d * talents.getSubversion())) * hsCritDmgMult);
        damage *= hsCritDmgMult;
        damage *= getDamageMod();
        return damage;
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
