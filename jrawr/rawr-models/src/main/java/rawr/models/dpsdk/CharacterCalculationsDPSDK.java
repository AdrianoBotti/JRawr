// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/CharacterCalculationsDPSDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use rawr.base.util.NetFormat (N0/N2/F0/0/0.00 custom formats) and DPSDKFormat.percent
// for the `P` standard format (Mono invariant writes a space before %). Ratings members are
// camelCase public fields; the golden test resolves the C# property names case-insensitively.
package rawr.models.dpsdk;

import rawr.base.character.Buff;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CharacterCalculationsDPSDK extends CharacterCalculationsBase {
    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getDPSPoints() {
        return this.subPoints[0];
    }

    public void setDPSPoints(float value) {
        this.subPoints[0] = value;
    }

    public float whiteDPS;
    public float necrosisDPS;
    public float bcbDPS;
    public float deathCoilDPS;
    public float icyTouchDPS;
    public float plagueStrikeDPS;
    public float frostFeverDPS;
    public float bloodPlagueDPS;
    public float scourgeStrikeDPS;
    public float unholyBlightDPS;
    public float bloodwormsDPS;
    public float otherDPS;
    public float frostStrikeDPS;
    public float howlingBlastDPS;
    public float obliterateDPS;
    public float deathStrikeDPS;
    public float bloodStrikeDPS;
    public float heartStrikeDPS;
    public float gargoyleDPS;
    public float drwDPS;
    public float wanderingPlagueDPS;
    public int targetLevel;
    public float mhWeaponDamage;
    public float ohWeaponDamage;
    public float mhAttackSpeed;
    public float ohAttackSpeed;
    public float critChance;
    public float spellCritChance;
    public float avoidedAttacks;
    public float dodgedMHAttacks;
    public float dodgedOHAttacks;
    public float dodgedAttacks;
    public float missedAttacks;
    public float enemyMitigation;
    public float effectiveArmor;
    public float mhExpertise;
    public float ohExpertise;
    public float ghoulDPS;
    public String drwStats = "";

    public Stats basicStats;

    public final List<Buff> activeBuffs;

    public CharacterCalculationsDPSDK() {
        activeBuffs = new java.util.ArrayList<Buff>();
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        float critRating = basicStats.getCritRating();

        float hitRating = basicStats.getHitRating();

        float armorPenetrationRating = basicStats.getArmorPenetrationRating();

        float attackPower = basicStats.getAttackPower();
        if (activeBuffs.contains(Buff.getBuffByName("Improved Hunter's Mark"))) {
            attackPower -= 110f * (1f + basicStats.getBonusAttackPowerMultiplier());
        }

        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        dictValues.put("Health", NetFormat.grouped(basicStats.getHealth(), 0));
        dictValues.put("Strength", NetFormat.grouped(basicStats.getStrength(), 0));
        dictValues.put("Agility", NetFormat.fixed(basicStats.getAgility(), 0) + "*Provides "
                + DPSDKFormat.percent(StatConversion.getCritFromAgility(basicStats.getAgility(), CharacterClass.DeathKnight), 2) + " crit chance");
        dictValues.put("Attack Power", NetFormat.grouped(attackPower, 0));
        dictValues.put("Crit Rating", NetFormat.fixed(critRating, 0) + "*Provides "
                + DPSDKFormat.percent(StatConversion.getCritFromRating(critRating, CharacterClass.DeathKnight), 2) + " crit chance");
        dictValues.put("Hit Rating", NetFormat.fixed(hitRating, 0) + "*Negates "
                + DPSDKFormat.percent(StatConversion.getPhysicalHitFromRating(hitRating, CharacterClass.DeathKnight), 2) + " melee miss / "
                + DPSDKFormat.percent(StatConversion.getSpellHitFromRating(hitRating, CharacterClass.DeathKnight), 2) + " spell miss");
        dictValues.put("Expertise", NetFormat.fixed(mhExpertise, 2) + " / " + NetFormat.fixed(ohExpertise, 2)
                + "*Negates " + DPSDKFormat.percent(StatConversion.getDodgeParryReducFromExpertise(mhExpertise), 2)
                + " / " + DPSDKFormat.percent(StatConversion.getDodgeParryReducFromExpertise(ohExpertise), 2) + " dodge chance");
        dictValues.put("Haste Rating", NetFormat.fixed(basicStats.getHasteRating(), 0) + "*Increases attack speed by "
                + DPSDKFormat.percent(StatConversion.getHasteFromRating(basicStats.getHasteRating(), CharacterClass.DeathKnight), 2));
        dictValues.put("Armor Penetration Rating", NetFormat.grouped(armorPenetrationRating, 0));
        dictValues.put("Armor", NetFormat.grouped(basicStats.getArmor(), 0));
        dictValues.put("Resilience", NetFormat.fixed(basicStats.getResilience(), 0));

        dictValues.put("Weapon Damage", NetFormat.grouped(mhWeaponDamage, 2) + " / " + NetFormat.grouped(ohWeaponDamage, 2));
        dictValues.put("Attack Speed", NetFormat.grouped(mhAttackSpeed, 2) + " / " + NetFormat.grouped(ohAttackSpeed, 2));
        dictValues.put("Crit Chance", DPSDKFormat.percent(critChance, 2));
        dictValues.put("Avoided Attacks", DPSDKFormat.percent(avoidedAttacks, 2) + "*" + DPSDKFormat.percent(dodgedAttacks, 2)
                + " Dodged, " + DPSDKFormat.percent(missedAttacks, 2) + " Missed");
        dictValues.put("Enemy Mitigation", DPSDKFormat.percent(enemyMitigation, 2) + "*" + NetFormat.fixed(effectiveArmor, 0) + " effective enemy armor");

        dictValues.put("BCB", NetFormat.grouped(bcbDPS, 2) + "*" + DPSDKFormat.percent((float) bcbDPS / getDPSPoints(), 2));
        dictValues.put("Blood Plague", NetFormat.grouped(bloodPlagueDPS, 2) + "*" + DPSDKFormat.percent((float) bloodPlagueDPS / getDPSPoints(), 2));
        dictValues.put("Blood Strike", NetFormat.grouped(bloodStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) bloodStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Death Coil", NetFormat.grouped(deathCoilDPS, 2) + "*" + DPSDKFormat.percent((float) deathCoilDPS / getDPSPoints(), 2));
        dictValues.put("DRW", NetFormat.grouped(drwDPS, 2) + "*" + DPSDKFormat.percent((float) drwDPS / getDPSPoints(), 2) + ", wait for " + drwStats + " proc");
        dictValues.put("Frost Fever", NetFormat.grouped(frostFeverDPS, 2) + "*" + DPSDKFormat.percent((float) frostFeverDPS / getDPSPoints(), 2));
        dictValues.put("Frost Strike", NetFormat.grouped(frostStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) frostStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Gargoyle", NetFormat.grouped(gargoyleDPS, 2) + "*" + DPSDKFormat.percent((float) gargoyleDPS / getDPSPoints(), 2));
        dictValues.put("Heart Strike", NetFormat.grouped(heartStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) heartStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Howling Blast", NetFormat.grouped(howlingBlastDPS, 2) + "*" + DPSDKFormat.percent((float) howlingBlastDPS / getDPSPoints(), 2));
        dictValues.put("Icy Touch", NetFormat.grouped(icyTouchDPS, 2) + "*" + DPSDKFormat.percent((float) icyTouchDPS / getDPSPoints(), 2));
        dictValues.put("Necrosis", NetFormat.grouped(necrosisDPS, 2) + "*" + DPSDKFormat.percent((float) necrosisDPS / getDPSPoints(), 2));
        dictValues.put("Obliterate", NetFormat.grouped(obliterateDPS, 2) + "*" + DPSDKFormat.percent((float) obliterateDPS / getDPSPoints(), 2));
        dictValues.put("Death Strike", NetFormat.grouped(deathStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) deathStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Plague Strike", NetFormat.grouped(plagueStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) plagueStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Scourge Strike", NetFormat.grouped(scourgeStrikeDPS, 2) + "*" + DPSDKFormat.percent((float) scourgeStrikeDPS / getDPSPoints(), 2));
        dictValues.put("Unholy Blight", NetFormat.grouped(unholyBlightDPS, 2) + "*" + DPSDKFormat.percent((float) unholyBlightDPS / getDPSPoints(), 2));
        dictValues.put("Wandering Plague", NetFormat.grouped(wanderingPlagueDPS, 2) + "*" + DPSDKFormat.percent((float) wanderingPlagueDPS / getDPSPoints(), 2));
        dictValues.put("White", NetFormat.grouped(whiteDPS, 2) + "*" + DPSDKFormat.percent((float) whiteDPS / getDPSPoints(), 2));
        dictValues.put("Ghoul", NetFormat.grouped(ghoulDPS, 2) + "*" + DPSDKFormat.percent((float) ghoulDPS / getDPSPoints(), 2));
        dictValues.put("Bloodworms", NetFormat.grouped(bloodwormsDPS, 2) + "*" + DPSDKFormat.percent((float) bloodwormsDPS / getDPSPoints(), 2));
        dictValues.put("Other", NetFormat.grouped(otherDPS, 2) + "*" + DPSDKFormat.percent((float) otherDPS / getDPSPoints(), 2));
        dictValues.put("Total DPS", NetFormat.grouped(getDPSPoints(), 2));

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health":
                return basicStats.getHealth();
            case "Nature Resistance":
                return basicStats.getNatureResistance();
            case "Fire Resistance":
                return basicStats.getFireResistance();
            case "Frost Resistance":
                return basicStats.getFrostResistance();
            case "Shadow Resistance":
                return basicStats.getShadowResistance();
            case "Arcane Resistance":
                return basicStats.getArcaneResistance();
            case "Crit Rating":
                return basicStats.getCritRating();
            case "Expertise Rating":
                return basicStats.getExpertiseRating();
            case "Hit Rating":
                return basicStats.getHitRating();
            case "Haste Rating":
                return basicStats.getHasteRating();
            case "Target Miss %":
                return missedAttacks * 100f;
            case "Target Dodge %":
                return dodgedAttacks * 100f;
            case "Resilience":
                return basicStats.getResilience();
            case "Spell Penetration":
                return basicStats.getSpellPenetration();
            default:
                break;
        }
        return 0;
    }
}
