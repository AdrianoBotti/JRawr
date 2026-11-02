// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/CharacterCalculationsEnhance.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use .NET F0/F2 formatting via rawr.base.util.NetFormat / rawr.models.mage.NetFormat.
package rawr.models.enhance;

import rawr.base.character.Buff;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CharacterCalculationsEnhance extends CharacterCalculationsBase {

    private float overallPoints = 0f;
    private float[] subPoints = new float[] { 0f, 0f };

    public float getDPSPoints() {
        return subPoints[0];
    }

    public void setDPSPoints(float value) {
        subPoints[0] = value;
    }

    public float getSurvivabilityPoints() {
        return subPoints[1];
    }

    public void setSurvivabilityPoints(float value) {
        subPoints[1] = value;
    }

    private Stats basicStats;

    public Stats getBasicStats() {
        return basicStats;
    }

    public void setBasicStats(Stats value) {
        basicStats = value;
    }

    private Stats enhsimStats;

    public Stats getEnhSimStats() {
        return enhsimStats;
    }

    public void setEnhSimStats(Stats value) {
        enhsimStats = value;
    }

    private Stats buffStats;

    public Stats getBuffStats() {
        return buffStats;
    }

    public void setBuffStats(Stats value) {
        buffStats = value;
    }

    private int targetLevel;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private float attackPower;

    public float getAttackPower() {
        return attackPower;
    }

    public void setAttackPower(float value) {
        attackPower = value;
    }

    private float spellPower;

    public float getSpellPower() {
        return spellPower;
    }

    public void setSpellPower(float value) {
        spellPower = value;
    }

    private float totalExpertiseMH;

    public float getTotalExpertiseMH() {
        return totalExpertiseMH;
    }

    public void setTotalExpertiseMH(float value) {
        totalExpertiseMH = value;
    }

    private float totalExpertiseOH;

    public float getTotalExpertiseOH() {
        return totalExpertiseOH;
    }

    public void setTotalExpertiseOH(float value) {
        totalExpertiseOH = value;
    }

    private float avoidedAttacks;

    public float getAvoidedAttacks() {
        return avoidedAttacks;
    }

    public void setAvoidedAttacks(float value) {
        avoidedAttacks = value;
    }

    private float dodgedAttacks;

    public float getDodgedAttacks() {
        return dodgedAttacks;
    }

    public void setDodgedAttacks(float value) {
        dodgedAttacks = value;
    }

    private float parriedAttacks;

    public float getParriedAttacks() {
        return parriedAttacks;
    }

    public void setParriedAttacks(float value) {
        parriedAttacks = value;
    }

    private float missedAttacks;

    public float getMissedAttacks() {
        return missedAttacks;
    }

    public void setMissedAttacks(float value) {
        missedAttacks = value;
    }

    private float whiteCrit;

    public float getMeleeCrit() {
        return whiteCrit;
    }

    public void setMeleeCrit(float value) {
        whiteCrit = value;
    }

    private float yellowCrit;

    public float getYellowCrit() {
        return yellowCrit;
    }

    public void setYellowCrit(float value) {
        yellowCrit = value;
    }

    private float spellCrit;

    public float getSpellCrit() {
        return spellCrit;
    }

    public void setSpellCrit(float value) {
        spellCrit = value;
    }

    private float whiteHit;

    public float getWhiteHit() {
        return whiteHit;
    }

    public void setWhiteHit(float value) {
        whiteHit = value;
    }

    private float yellowHit;

    public float getYellowHit() {
        return yellowHit;
    }

    public void setYellowHit(float value) {
        yellowHit = value;
    }

    private float spellHit;

    public float getSpellHit() {
        return spellHit;
    }

    public void setSpellHit(float value) {
        spellHit = value;
    }

    private float overSpellHitCap;

    public float getOverSpellHitCap() {
        return overSpellHitCap;
    }

    public void setOverSpellHitCap(float value) {
        overSpellHitCap = value;
    }

    private float overMeleeCritCap;

    public float getOverMeleeCritCap() {
        return overMeleeCritCap;
    }

    public void setOverMeleeCritCap(float value) {
        overMeleeCritCap = value;
    }

    private float armorMitigation;

    public float getArmorMitigation() {
        return armorMitigation;
    }

    public void setArmorMitigation(float value) {
        armorMitigation = value;
    }

    private float edUptime;

    public float getEDUptime() {
        return edUptime;
    }

    public void setEDUptime(float value) {
        edUptime = value;
    }

    private float edBonusCrit;

    public float getEDBonusCrit() {
        return edBonusCrit;
    }

    public void setEDBonusCrit(float value) {
        edBonusCrit = value;
    }

    private float flurryUptime;

    public float getFlurryUptime() {
        return flurryUptime;
    }

    public void setFlurryUptime(float value) {
        flurryUptime = value;
    }

    private float secondsTo5Stack;
    private float mwppm;

    public float getSecondsTo5Stack() {
        return secondsTo5Stack;
    }

    public void setSecondsTo5Stack(float value) {
        secondsTo5Stack = value;
        mwppm = value == 0 ? 0 : 5f * 60f / value;
    }

    private float avMHSpeed;

    public float getAvMHSpeed() {
        return avMHSpeed;
    }

    public void setAvMHSpeed(float value) {
        avMHSpeed = value;
    }

    private float avOHSpeed;

    public float getAvOHSpeed() {
        return avOHSpeed;
    }

    public void setAvOHSpeed(float value) {
        avOHSpeed = value;
    }

    private float meleeDamage;

    public float getMeleeDamage() {
        return meleeDamage;
    }

    public void setMeleeDamage(float value) {
        meleeDamage = value;
    }

    private float glancingBlows;

    public float getGlancingBlows() {
        return glancingBlows;
    }

    public void setGlancingBlows(float value) {
        glancingBlows = value;
    }

    private DPSAnalysis swingDamage;

    public DPSAnalysis getSwingDamage() {
        return swingDamage;
    }

    public void setSwingDamage(DPSAnalysis value) {
        swingDamage = value;
    }

    private DPSAnalysis windfuryAttack;

    public DPSAnalysis getWindfuryAttack() {
        return windfuryAttack;
    }

    public void setWindfuryAttack(DPSAnalysis value) {
        windfuryAttack = value;
    }

    private DPSAnalysis flametongueAttack;

    public DPSAnalysis getFlameTongueAttack() {
        return flametongueAttack;
    }

    public void setFlameTongueAttack(DPSAnalysis value) {
        flametongueAttack = value;
    }

    private DPSAnalysis lightningBolt;

    public DPSAnalysis getLightningBolt() {
        return lightningBolt;
    }

    public void setLightningBolt(DPSAnalysis value) {
        lightningBolt = value;
    }

    private DPSAnalysis earthShock;

    public DPSAnalysis getEarthShock() {
        return earthShock;
    }

    public void setEarthShock(DPSAnalysis value) {
        earthShock = value;
    }

    private DPSAnalysis flameShock;

    public DPSAnalysis getFlameShock() {
        return flameShock;
    }

    public void setFlameShock(DPSAnalysis value) {
        flameShock = value;
    }

    private DPSAnalysis searingMagma;

    public DPSAnalysis getSearingMagma() {
        return searingMagma;
    }

    public void setSearingMagma(DPSAnalysis value) {
        searingMagma = value;
    }

    private DPSAnalysis fireNova;

    public DPSAnalysis getFireNova() {
        return fireNova;
    }

    public void setFireNova(DPSAnalysis value) {
        fireNova = value;
    }

    private FireElemental fireElemental;

    public FireElemental getFireElemental() {
        return fireElemental;
    }

    public void setFireElemental(FireElemental value) {
        fireElemental = value;
    }

    private DPSAnalysis stormstrike;

    public DPSAnalysis getStormstrike() {
        return stormstrike;
    }

    public void setStormstrike(DPSAnalysis value) {
        stormstrike = value;
    }

    private DPSAnalysis spiritWolf;

    public DPSAnalysis getSpiritWolf() {
        return spiritWolf;
    }

    public void setSpiritWolf(DPSAnalysis value) {
        spiritWolf = value;
    }

    private DPSAnalysis lightningShield;

    public DPSAnalysis getLightningShield() {
        return lightningShield;
    }

    public void setLightningShield(DPSAnalysis value) {
        lightningShield = value;
    }

    private DPSAnalysis lavaLash;

    public DPSAnalysis getLavaLash() {
        return lavaLash;
    }

    public void setLavaLash(DPSAnalysis value) {
        lavaLash = value;
    }

    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        version = value;
    }

    private float mhEnchantUptime;

    public float getMHEnchantUptime() {
        return mhEnchantUptime;
    }

    public void setMHEnchantUptime(float value) {
        mhEnchantUptime = value;
    }

    private float ohEnchantUptime;

    public float getOHEnchantUptime() {
        return ohEnchantUptime;
    }

    public void setOHEnchantUptime(float value) {
        ohEnchantUptime = value;
    }

    private float trinket1Uptime;

    public float getTrinket1Uptime() {
        return trinket1Uptime;
    }

    public void setTrinket1Uptime(float value) {
        trinket1Uptime = value;
    }

    private float trinket2Uptime;

    public float getTrinket2Uptime() {
        return trinket2Uptime;
    }

    public void setTrinket2Uptime(float value) {
        trinket2Uptime = value;
    }

    private float fireTotemUptime;

    public float getFireTotemUptime() {
        return fireTotemUptime;
    }

    public void setFireTotemUptime(float value) {
        fireTotemUptime = value;
    }

    private float t10_2Uptime;

    public float getT10_2Uptime() {
        return t10_2Uptime;
    }

    public void setT10_2Uptime(float value) {
        t10_2Uptime = value;
    }

    private float t10_4Uptime;

    public float getT10_4Uptime() {
        return t10_4Uptime;
    }

    public void setT10_4Uptime(float value) {
        t10_4Uptime = value;
    }

    public List<Buff> activeBuffs;

    public List<Buff> getActiveBuffs() {
        return activeBuffs;
    }

    public void setActiveBuffs(List<Buff> value) {
        activeBuffs = value;
    }

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        dictValues.put("Health", NetFormat.fixed(basicStats.getHealth(), 0));
        dictValues.put("Mana", NetFormat.fixed(basicStats.getMana(), 0));
        dictValues.put("Attack Power", NetFormat.fixed(basicStats.getAttackPower(), 0));
        dictValues.put("Agility", NetFormat.fixed(basicStats.getAgility(), 0));
        dictValues.put("Strength", NetFormat.fixed(basicStats.getStrength(), 0));
        dictValues.put("Intellect", NetFormat.fixed(basicStats.getIntellect(), 0));

        dictValues.put("White Hit", f2(whiteHit) + "%");
        if (yellowHit < 100f && totalExpertiseMH < 26) {
            float ratingRequired = (float) Math.ceil(4f * StatConversion.getRatingFromExpertise(100f - yellowHit));
            dictValues.put("Yellow Hit", String.format(Locale.ROOT, "%s%% (Under Cap)*You need %s more expertise to cap specials (WF,SS)",
                    f2(yellowHit),
                    NetFormat.fixed(ratingRequired, 0)));
        } else {
            if (parriedAttacks > 0) {
                float ratingRequired = (float) Math.ceil(4f * StatConversion.getRatingFromExpertise(100f - yellowHit));
                dictValues.put("Yellow Hit", String.format(Locale.ROOT, "%s%%*Being in front of boss allows your attacks to be parried\r\nYou would need %s more expertise to cap specials (WF,SS)",
                        f2(yellowHit),
                        NetFormat.fixed(ratingRequired, 0)));
            } else
                dictValues.put("Yellow Hit", f2(yellowHit) + "%");
        }
        if (overSpellHitCap > 0.38f) // only warn if more than .38% over cap (equivalent to 10 hit rating)
            dictValues.put("Spell Hit", String.format(Locale.ROOT, "%s%% (Over Cap)*Over Spell Hit Cap by %s%%",
                    f2(spellHit),
                    f2(overSpellHitCap)));
        else {
            if (spellHit < 100f) {
                float ratingRequired = (float) Math.ceil(StatConversion.getRatingFromHit(1f - spellHit / 100f));
                dictValues.put("Spell Hit", String.format(Locale.ROOT, "%s%% (Under Cap)*You need %s more hit rating to cap spells (ES, LB etc)",
                        f2(spellHit),
                        NetFormat.fixed(ratingRequired, 0)));
            } else
                dictValues.put("Spell Hit", f2(spellHit) + "%");
        }
        if (overMeleeCritCap > 0.21f) // only warn if more than .21% over cap (equivalent to 10 crit rating)
            dictValues.put("Melee Crit", String.format(Locale.ROOT, "%s%% (Over Cap)*Crit Rating %s (+%s%% crit chance)\r\nOver Soft Cap by %s%%",
                    f2(getMeleeCrit()),
                    NetFormat.fixed(basicStats.getCritRating(), 0),
                    f2(StatConversion.getCritFromRating(basicStats.getCritRating()) * 100f),
                    f2(overMeleeCritCap)));
        else
            dictValues.put("Melee Crit", String.format(Locale.ROOT, "%s%%*Crit Rating %s (+%s%% crit chance)",
                    f2(getMeleeCrit()),
                    NetFormat.fixed(basicStats.getCritRating(), 0),
                    f2(StatConversion.getCritFromRating(basicStats.getCritRating()) * 100f)));

        dictValues.put("Spell Crit", String.format(Locale.ROOT, "%s%%*Crit Rating %s (+%s%% crit chance)",
                f2(spellCrit),
                NetFormat.fixed(basicStats.getCritRating(), 0),
                f2(StatConversion.getSpellCritFromRating(basicStats.getCritRating()) * 100f)));

        dictValues.put("Spellpower", NetFormat.fixed(basicStats.getSpellPower(), 0));
        dictValues.put("Total Expertise", getExpertiseString());
        dictValues.put("Haste Rating", String.format(Locale.ROOT, "%s*%s%% Melee Haste\r\n%s%% Spell Haste",
                NetFormat.fixed(basicStats.getHasteRating(), 0),
                f2(StatConversion.getHasteFromRating(basicStats.getHasteRating(), rawr.base.character.CharacterClass.Shaman) * 100f),
                f2(StatConversion.getSpellHasteFromRating(basicStats.getHasteRating(), rawr.base.character.CharacterClass.Shaman) * 100f)));
        dictValues.put("Hit Rating", String.format(Locale.ROOT, "%s*%s%% Melee Hit\r\n%s%% Spell Hit",
                NetFormat.fixed(basicStats.getHitRating(), 0),
                f2(StatConversion.getHitFromRating(basicStats.getHitRating()) * 100f),
                f2(StatConversion.getSpellHitFromRating(basicStats.getHitRating()) * 100f)));
        dictValues.put("Armour Pen Rating", String.format(Locale.ROOT, "%s*%s%% Armour Penetration",
                NetFormat.fixed(basicStats.getArmorPenetrationRating(), 0),
                f2(StatConversion.getArmorPenetrationFromRating(basicStats.getArmorPenetrationRating()) * 100f)));
        float spellMiss = 100 - spellHit;
        dictValues.put("Avoided Attacks", String.format(Locale.ROOT, "%s%%*%s%% Boss Dodged\r\n%s%% Boss Parried\r\n%s%% Spell Misses\r\n%s%% White Misses",
                f2(avoidedAttacks),
                f2(dodgedAttacks),
                f2(parriedAttacks),
                f2(spellMiss),
                f2(missedAttacks)));
        dictValues.put("Avg MH Speed", f2(avMHSpeed));
        dictValues.put("Avg OH Speed", f2(avOHSpeed));
        dictValues.put("Armor Mitigation", f2(armorMitigation) + "%*Amount of physical damage lost due to boss armor");

        dictValues.put("ED Uptime", String.format(Locale.ROOT, "%s%%*%s%% ED Bonus Crit",
                f2(edUptime),
                f2(edBonusCrit)));
        dictValues.put("Flurry Uptime", f2(flurryUptime) + "%");
        dictValues.put("Avg Time to 5 Stack", String.format(Locale.ROOT, "%s sec*%s PPM",
                f2(secondsTo5Stack),
                f2(mwppm)));
        dictValues.put("MH Enchant Uptime", f2(mhEnchantUptime) + "%");
        dictValues.put("OH Enchant Uptime", f2(ohEnchantUptime) + "%");
        dictValues.put("Trinket 1 Uptime", f2(trinket1Uptime) + "%");
        dictValues.put("Trinket 2 Uptime", f2(trinket2Uptime) + "%");
        dictValues.put("Fire Totem Uptime", f2(fireTotemUptime) + "%");
        dictValues.put("Tier 10 2 pc Uptime", f2(t10_2Uptime) + "%");
        dictValues.put("Tier 10 4 pc Uptime", f2(t10_4Uptime) + "%");

        dictValues.put("DPS Points", f2(getDPSPoints()));
        dictValues.put("Survivability Points", f2(getSurvivabilityPoints()));
        dictValues.put("Overall Points", f2(overallPoints));

        dictValues.put("White Damage", dpsOutputFormat(swingDamage, getDPSPoints(), true));
        dictValues.put("Windfury Attack", dpsOutputFormat(windfuryAttack, getDPSPoints(), true));
        dictValues.put("Flametongue Attack", dpsOutputFormat(flametongueAttack, getDPSPoints(), false));
        dictValues.put("Lightning Bolt", dpsOutputFormat(lightningBolt, getDPSPoints(), false));
        dictValues.put("Earth Shock", dpsOutputFormat(earthShock, getDPSPoints(), false));
        dictValues.put("Flame Shock", dpsOutputFormat(flameShock, getDPSPoints(), false));
        dictValues.put("Searing/Magma Totem", dpsOutputFormat(searingMagma, getDPSPoints(), false));
        dictValues.put("Stormstrike", dpsOutputFormat(stormstrike, getDPSPoints(), true));
        dictValues.put("Spirit Wolf", dpsOutputFormat(spiritWolf, getDPSPoints(), true));
        dictValues.put("Fire Nova", dpsOutputFormat(fireNova, getDPSPoints(), false));
        dictValues.put("Fire Elemental", fireElemental.getDPSOutput());
        dictValues.put("Lightning Shield", dpsOutputFormat(lightningShield, getDPSPoints(), false));
        dictValues.put("Lava Lash", dpsOutputFormat(lavaLash, getDPSPoints(), true));
        dictValues.put("Total DPS", f2(getDPSPoints()));

        dictValues.put("Enhance Version", version);

        dictValues.put("Status", String.format(Locale.ROOT, "Enhance Model : DPS Points %s, Survivability Points %s, Overall Points %s",
                f2(getDPSPoints()),
                f2(getSurvivabilityPoints()),
                f2(overallPoints)));

        return dictValues;
    }

    private String dpsOutputFormat(DPSAnalysis dpsStat, float totaldps, boolean AP) {
        float percent = dpsStat.getDps() / totaldps * 100f;
        String power = AP ? "Av.AP : " + f2(attackPower) :
                "Av.SP : " + f2(spellPower);
        return String.format(Locale.ROOT, "%s\r\n%s%% of total dps\r\nPPM    : %s\r\n%s",
                dpsStat, f2(percent), f2(dpsStat.getPPM()), power);
    }

    private String getExpertiseString() {   // using 26.5 for display purposes as its pointless warning over cap if 26.1 Expertise for example.
        String caps;
        if (totalExpertiseMH == totalExpertiseOH) {
            if (totalExpertiseMH > 26.5f)
                caps = "%s (Over Cap)*%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged\r\n%s%% Parried";
            else if (totalExpertiseMH < 26)
                caps = "%s (Under Cap)*%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged\r\n%s%% Parried";
            else
                caps = "%s*%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged\r\n%s%% Parried";
            return String.format(Locale.ROOT, caps,
                    NetFormat.fixed(totalExpertiseMH, 0),
                    NetFormat.fixed(basicStats.getExpertise(), 0),
                    NetFormat.fixed(basicStats.getExpertiseRating(), 0),
                    f2(dodgedAttacks),
                    f2(parriedAttacks));
        } else {
            if (totalExpertiseMH > 26.5f && totalExpertiseOH > 26.5f)
                caps = "%s/%s (Over Cap)*MH/OH\r\n%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged\r\n%s%% Parried";
            else if (totalExpertiseMH > 26.5f)
                caps = "%s/%s (MH Over Cap)*MH/OH\r\n%s Expertise\r\n%s Expertise Rating\r\n%s%% OH Dodged\r\n%s%% Parried";
            else if (totalExpertiseOH > 26.5f)
                caps = "%s/%s (OH Over Cap)*MH/OH\r\n%s Expertise\r\n%s Expertise Rating\r\n%s%% MH Dodged\r\n%s%% Parried";
            else if (totalExpertiseMH < 26f)
                caps = "%s/%s (MH Under Cap)*MH/OH\r\n%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged";
            else
                caps = "%s/%s*MH/OH\r\n%s Expertise\r\n%s Expertise Rating\r\n%s%% Dodged";
            return String.format(Locale.ROOT, caps,
                    NetFormat.fixed(totalExpertiseMH, 0),
                    NetFormat.fixed(totalExpertiseOH, 0),
                    NetFormat.fixed(basicStats.getExpertise(), 0),
                    NetFormat.fixed(basicStats.getExpertiseRating(), 0),
                    f2(dodgedAttacks),
                    f2(parriedAttacks));
        }
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health":
                return basicStats.getHealth();
            case "DPS Points":
                return getDPSPoints();
            case "Hit Rating":
                return basicStats.getHitRating();
            case "Dodged Attacks":
                return dodgedAttacks;
            case "Parried Attacks":
                return parriedAttacks;
            case "Avoided Attacks":
                return avoidedAttacks;
        }
        return 0f;
    }

    private static String f2(float value) {
        return rawr.models.mage.NetFormat.f2(value);
    }
}
