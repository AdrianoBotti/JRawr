// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/AbilityHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

public class AbilityHandler {
    Stats stats;
    CombatTable combatTable;
    DeathKnightTalents talents;
    rawr.base.character.Character character;
    CalculationOptionsDPSDK calcOpts;

    public AbilityHandler(Stats s, CombatTable t, rawr.base.character.Character c, CalculationOptionsDPSDK opts) {
        stats = new Stats();
        stats.accumulate(s);
        combatTable = t;
        character = c;
        calcOpts = opts;
        talents = c.getDeathKnightTalents();
        initialize();
    }

    public void initialize() {
        BS = new BloodStrike(character, stats, calcOpts, combatTable, talents);
        DC = new DeathCoil(character, stats, calcOpts, combatTable, talents);
        IT = new IcyTouch(character, stats, calcOpts, combatTable, talents);
        PS = new PlagueStrike(character, stats, calcOpts, combatTable, talents);
        SS = new ScourgeStrike(character, stats, calcOpts, combatTable, talents);
        HB = new HowlingBlast(character, stats, calcOpts, combatTable, talents);
        FS = new FrostStrike(character, stats, calcOpts, combatTable, talents);
        OB = new Obliterate(character, stats, calcOpts, combatTable, talents);
        HS = new HeartStrike(character, stats, calcOpts, combatTable, talents);
        DS = new DeathStrike(character, stats, calcOpts, combatTable, talents);
        BB = new BloodBoil(character, stats, calcOpts, combatTable, talents);
        FF = new Disease(character, stats, calcOpts, combatTable, talents);
        BP = new Disease(character, stats, calcOpts, combatTable, talents);
        BS.setDamageMod(1);
        DC.setDamageMod(1);
        IT.setDamageMod(1);
        PS.setDamageMod(1);
        SS.setDamageMod(1);
        SS.setSecondaryDamageMod(1);
        HB.setDamageMod(1);
        FS.setDamageMod(1);
        OB.setDamageMod(1);
        HS.setDamageMod(1);
        DS.setDamageMod(1);
        BB.setDamageMod(1);
        FF.setDamageMod(1);
        BP.setDamageMod(1);

        double PhysicalMod = stats.getBonusPhysicalDamageMultiplier();
        double FrostMod = stats.getBonusFrostDamageMultiplier();
        double ShadowMod = stats.getBonusShadowDamageMultiplier();
        if (character.activeBuffsContains("Crypt Fever")) {
            BP.setDamageMod(BP.getDamageMod() * 1.3d);
            FF.setDamageMod(FF.getDamageMod() * 1.3d);
        } else {
            BP.setDamageMod(BP.getDamageMod() * (1d + talents.getCryptFever() * 0.1d));
            FF.setDamageMod(FF.getDamageMod() * (1d + talents.getCryptFever() * 0.1d));
        }
        if (stats.getDiseasesCanCrit() > 0) {
            float DiseaseCritDmgMult = 0.5f * (2f + stats.getBonusCritMultiplier());
            float DiseaseCrit = 1f + combatTable.spellCrits;
            BP.setDamageMod(BP.getDamageMod() * DiseaseCrit * DiseaseCritDmgMult);
        }
        if (talents.getWanderingPlague() > 0) {
            float modifier = 1f + (combatTable.physCrits * (talents.getWanderingPlague() / 3f));
            BP.setDamageMod(BP.getDamageMod() * modifier);
            FF.setDamageMod(FF.getDamageMod() * modifier);
        }
        BS.setDamageMod(BS.getDamageMod()
                * combatTable.physicalMitigation
                * PhysicalMod
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodOfTheNorth() * .10 / 3d)
                * (1d + talents.getBloodyStrikes() * .05d)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getHysteria() * 0.2 / 180)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0)));
        DC.setDamageMod(DC.getDamageMod()
                * (1d + talents.getBlackIce() * .02d)
                * ShadowMod
                * .94
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + (talents.isGlyphofDarkDeath() ? .15d : 0d))
                * (1d + talents.getMorbidity() * .05d)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d - combatTable.spellResist));
        IT.setDamageMod(IT.getDamageMod()
                * (1d + talents.getBlackIce() * .02d)
                * FrostMod
                * .94
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getGlacierRot() * .2 / 3)
                * (1d + talents.getImprovedIcyTouch() * .05d)
                * (1d + talents.getMercilessCombat() * .06 * 0.3)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d));
        PS.setDamageMod(PS.getDamageMod()
                * combatTable.physicalMitigation
                * PhysicalMod
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getHysteria() * .2d / 180)
                * (1d + talents.getOutbreak() * .1d)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0)));
        SS.setDamageMod(SS.getDamageMod()
                * combatTable.physicalMitigation
                * PhysicalMod
                * (1d + talents.getOutbreak() * 0.2d / 3)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getTwoHandedWeaponSpecialization() * .02d)
                * (1d + stats.getBonusScourgeStrikeMultiplier()));
        SS.setSecondaryDamageMod(SS.getSecondaryDamageMod()
                * 0.94d // Partial Resist
                * ShadowMod
                * (1d + (calcOpts.getRotation().getPresence() == CalculationOptionsDPSDK.Presence.Blood ? .15d : 0d)
                + (talents.getDesolation() * .05d)
                + (talents.getBoneShield() * .02d)
                + (talents.getBlackIce() * .02d))
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0)));
        HB.setDamageMod(HB.getDamageMod()
                * FrostMod
                * .94
                * (1d + talents.getBlackIce() * .02d)
                * (1d + talents.getGlacierRot() * .2 / 3)
                * (1d + talents.getMercilessCombat() * .06 * .3)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d - combatTable.spellResist));
        FS.setDamageMod(FS.getDamageMod()
                * FrostMod
                * .94
                * (1d + talents.getBlackIce() * .02d)
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodOfTheNorth() * .1 / 3)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getGlacierRot() * .2 / 3)
                * (1d + talents.getMercilessCombat() * .06 * .3)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0))
                * (1d - combatTable.missedSpecial - combatTable.dodgedSpecial));
        OB.setDamageMod(OB.getDamageMod()
                * combatTable.physicalMitigation
                * PhysicalMod
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getHysteria() * 0.2d / 180)
                * (1d + talents.getMercilessCombat() * .06 * .3)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0))
                * (1d + stats.getBonusObliterateMultiplier()));
        HS.setDamageMod(HS.getDamageMod()
                * PhysicalMod
                * combatTable.physicalMitigation
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodyStrikes() * .15d)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getHysteria() * .2d / 180)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0))
                * (1d + stats.getBonusHeartStrikeMultiplier()));
        DS.setDamageMod(DS.getDamageMod()
                * PhysicalMod
                * combatTable.physicalMitigation
                * (1d + (talents.isGlyphofDeathStrike() ? 0.25 : 0))
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBloodyVengeance() * .03d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getHysteria() * 0.2d / 180)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d)
                * (1d + talents.getImprovedDeathStrike() * .15d)
                * (1d + (!combatTable.DW ? talents.getTwoHandedWeaponSpecialization() * .02d : 0)));
        BB.setDamageMod(BB.getDamageMod()
                * ShadowMod
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBlackIce() * .02d)
                * (1d + talents.getBloodyStrikes() * .1d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .02d));
        FF.setDamageMod(FF.getDamageMod()
                * FrostMod
                * .94
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBlackIce() * .02d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + (talents.isGlyphofIcyTouch() ? 0.2 : 0))
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d));
        BP.setDamageMod(BP.getDamageMod()
                * ShadowMod
                * .94
                * (1d + talents.getBloodGorged() * .02d)
                * (1d + talents.getBlackIce() * .02d)
                * (1d + talents.getBoneShield() * .02d)
                * (1d + talents.getDesolation() * .01d)
                * (1d + talents.getRageOfRivendare() * .02d)
                * (1d + talents.getTundraStalker() * .03d));
    }

    public RuneAbility BS;
    public RuneAbility DC;
    public RuneAbility IT;
    public RuneAbility PS;
    public RuneAbility SS;
    public RuneAbility HB;
    public RuneAbility FS;
    public RuneAbility OB;
    public RuneAbility HS;
    public RuneAbility DS;
    public RuneAbility BB;
    public RuneAbility FF;
    public RuneAbility BP;
}
