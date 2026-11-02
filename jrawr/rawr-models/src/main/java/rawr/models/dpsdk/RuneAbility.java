// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public abstract class RuneAbility {
    private Character character;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    private double damageMod;

    public double getDamageMod() {
        return damageMod;
    }

    public void setDamageMod(double value) {
        damageMod = value;
    }

    private double secondaryDamageMod;

    public double getSecondaryDamageMod() {
        return secondaryDamageMod;
    }

    public void setSecondaryDamageMod(double value) {
        secondaryDamageMod = value;
    }

    public abstract double getDamage();

    public abstract double getSecondaryDamage();

    private double staticThreat;

    public double getStaticThreat() {
        return staticThreat;
    }

    public void setStaticThreat(double value) {
        staticThreat = value;
    }

    private double threatMod;

    public double getThreatMod() {
        return threatMod;
    }

    public void setThreatMod(double value) {
        threatMod = value;
    }

    private Stats stats;

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats value) {
        stats = value;
    }

    private CombatTable combatTable;

    public CombatTable getCombatTable() {
        return combatTable;
    }

    public void setCombatTable(CombatTable value) {
        combatTable = value;
    }

    private RunesUsed runes;

    public RunesUsed getRunes() {
        return runes;
    }

    public void setRunes(RunesUsed value) {
        runes = value;
    }

    private GCDType gcdType;

    public GCDType getGcdType() {
        return gcdType;
    }

    public void setGcdType(GCDType value) {
        gcdType = value;
    }

    private CalculationOptionsDPSDK calcOpts;

    public CalculationOptionsDPSDK getCalcOpts() {
        return calcOpts;
    }

    public void setCalcOpts(CalculationOptionsDPSDK value) {
        calcOpts = value;
    }

    private DeathKnightTalents talents;

    public DeathKnightTalents getTalents() {
        return talents;
    }

    public void setTalents(DeathKnightTalents value) {
        talents = value;
    }

    private double threat;

    public double getThreat() {
        threat = getDamage() + getSecondaryDamage();
        threat += staticThreat;
        threat *= threatMod;
        if (calcOpts.getRotation().getPresence() != CalculationOptionsDPSDK.Presence.Frost) {
            threat *= 0.8f;
            threat *= 1f - (talents.getSubversion() * 25d / 3d);
        } else {
            threat *= 1.45f;
        }
        return threat;
    }

    public RuneAbility() {
        character = null;
        stats = null;
        combatTable = null;
        runes = RunesUsed.Blood;
        gcdType = GCDType.Melee;
        calcOpts = null;
    }

    @Override
    public String toString() {
        return name;
    }
}
