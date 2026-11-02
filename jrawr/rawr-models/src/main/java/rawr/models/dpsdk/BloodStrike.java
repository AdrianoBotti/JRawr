// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class BloodStrike extends RuneAbility {
    public BloodStrike(Character c, Stats s, CalculationOptionsDPSDK calcOpts, CombatTable table, DeathKnightTalents talents) {
        setCharacter(c);
        setStats(s);
        setCalcOpts(calcOpts);
        setCombatTable(table);
        setGcdType(GCDType.Melee);
        setRunes(RunesUsed.Blood);
        setTalents(talents);
        setName("Blood Strike");
    }

    private double damage;

    @Override
    public double getDamage() {
        Stats stats = getStats();
        CombatTable combatTable = getCombatTable();
        DeathKnightTalents talents = getTalents();
        double bsDmg = ((combatTable.MH.baseDamage + ((stats.getAttackPower() / 14d) * combatTable.normalizationFactor) + 764d)
                * 0.4d) + stats.getBonusBloodStrikeDamage();
        if (combatTable.DW) {
            double bsDmgOH = ((combatTable.OH.baseDamage + ((stats.getAttackPower() / 14d)
                    * combatTable.normalizationFactor) + 764d) * 0.4d);
            bsDmgOH *= 0.5d;
            bsDmgOH += stats.getBonusBloodStrikeDamage();
            bsDmgOH *= 1d + 0.08333333333d * (double) talents.getNervesOfColdSteel();
            bsDmgOH *= (talents.getThreatOfThassarian() * (1d / 3d));
            bsDmg += bsDmgOH;
        }
        bsDmg *= 1d + 0.125d * (double) getCalcOpts().getRotation().getAvgDiseaseMult() * (1d + stats.getBonusPerDiseaseBloodStrikeDamage());
        damage = bsDmg;
        double bsCritDmgMult = 1d + (.15d * (double) talents.getMightOfMograine());
        bsCritDmgMult += (.15d * (double) talents.getGuileOfGorefiend()) + stats.getBonusCritMultiplier();
        double bsCrit = 1d + ((combatTable.physCrits + (.03f * (double) talents.getSubversion())) * bsCritDmgMult);
        damage = (damage) * bsCrit;
        return damage * getDamageMod();
    }

    @Override
    public double getSecondaryDamage() {
        return 0;
    }
}
