// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/CalculationsElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (CharacterCalculationsElemental, C# lines 677-805). Display values use the .NET number formats
// via rawr.models.mage.NetFormat (float ToString -> G7/G15, Math.Round -> banker's rounding,
// Math.Round(x, n) -> rescaled banker's rounding), plus the literal "\n" separators.
package rawr.models.elemental;

import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.models.mage.NetFormat;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsElemental extends CharacterCalculationsBase {

    public Stats basicStats;
    public Stats combatStats;
    public int targetLevel;

    public Spell lightningBolt;
    public Spell chainLightning;
    public Spell lavaBurst;
    public Spell flameShock;
    public Spell earthShock;
    public Spell frostShock;
    public Spell fireNova;
    public Spell searingTotem;
    public Spell magmaTotem;

    public float manaRegenInFSR;
    public float manaRegenOutFSR;
    public float replenishMP5;

    public float timeToOOM;
    public float castRegenFraction;
    public float rotationDPS;
    public float totalDPS;
    public float rotationMPS;
    public float castsPerSecond;
    public float critsPerSecond;
    public float missesPerSecond;

    public float lBPerSecond;
    public float lvBPerSecond;
    public float fSPerSecond;

    public float latencyPerSecond;

    public float clearCast_FlameShock;
    public float clearCast_LavaBurst;
    public float clearCast_LightningBolt;

    public String rotation;
    public String rotationDetails;

    public Character localCharacter;

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getBurstPoints() {
        return subPoints[0];
    }

    public void setBurstPoints(float value) {
        subPoints[0] = value;
    }

    public float getSustainedPoints() {
        return subPoints[1];
    }

    public void setSustainedPoints(float value) {
        subPoints[1] = value;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        dictValues.put("Overall Points", NetFormat.netFloatToString(overallPoints));
        dictValues.put("Burst Points", NetFormat.netFloatToString(getBurstPoints()));
        dictValues.put("Sustained Points", NetFormat.netFloatToString(getSustainedPoints()));

        float BasicUntilGCDCap = (float) Math.ceil((1.5f / (1f + basicStats.getSpellHaste()) - 1) * StatConversion.RATING_PER_SPELLHASTE
                - basicStats.getHasteRating());
        float BasicUntilLBCap = (float) Math.ceil((2f / (1f + basicStats.getSpellHaste()) - 1) * StatConversion.RATING_PER_SPELLHASTE
                - basicStats.getHasteRating());

        dictValues.put("Health", NetFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Mana", NetFormat.netFloatToString(basicStats.getMana()));
        dictValues.put("Stamina", NetFormat.netFloatToString(basicStats.getStamina()));
        dictValues.put("Intellect", NetFormat.netFloatToString(basicStats.getIntellect()));
        dictValues.put("Spell Power", NetFormat.netFloatToString(basicStats.getSpellPower()));
        dictValues.put("Hit Rating", NetFormat.netFloatToString(basicStats.getHitRating()));
        dictValues.put("Crit Rating", NetFormat.netFloatToString(basicStats.getCritRating()));
        dictValues.put("Haste Rating", NetFormat.netFloatToString(basicStats.getHasteRating()) + "*" + NetFormat.netFloatToString(BasicUntilGCDCap) + " Haste Rating until GCD cap\n" + NetFormat.netFloatToString(BasicUntilLBCap) + " Haste Rating until LB cap");
        dictValues.put("Mana Regen", NetFormat.netDoubleToString(Math.rint(manaRegenInFSR)) + " / " + NetFormat.netDoubleToString(Math.rint(manaRegenOutFSR)) + " + " + NetFormat.netDoubleToString(Math.rint(replenishMP5)) + "*All values are Mana per 5 seconds.\nMP5 while casting / MP5 while not casting + Replenishment");

        float CombatUntilGCDCap = (float) Math.ceil((1.5f / (1f + combatStats.getSpellHaste()) - 1) * StatConversion.RATING_PER_SPELLHASTE
                - combatStats.getHasteRating());
        float CombatUntilLBCap = (float) Math.ceil((2f / (1f + combatStats.getSpellHaste()) - 1) * StatConversion.RATING_PER_SPELLHASTE
                - combatStats.getHasteRating());

        dictValues.put("Average Health", NetFormat.netFloatToString(combatStats.getHealth()));
        dictValues.put("Average Mana", NetFormat.netFloatToString(combatStats.getMana()));
        dictValues.put("Average Stamina", NetFormat.netFloatToString(combatStats.getStamina()));
        dictValues.put("Average Intellect", NetFormat.netFloatToString(combatStats.getIntellect()));
        dictValues.put("Average Spell Power", NetFormat.netFloatToString(combatStats.getSpellPower()));
        dictValues.put("Average Hit Rating", NetFormat.netFloatToString(combatStats.getHitRating()));
        dictValues.put("Average Crit Rating", NetFormat.netFloatToString(combatStats.getCritRating()));
        dictValues.put("Average Haste Rating", NetFormat.netFloatToString(combatStats.getHasteRating()) + "*" + NetFormat.netFloatToString(CombatUntilGCDCap) + " Haste Rating until GCD cap\n" + NetFormat.netFloatToString(CombatUntilLBCap) + " Haste Rating until LB cap");
        dictValues.put("Average Mana Regen", NetFormat.netDoubleToString(Math.rint(manaRegenInFSR)) + " / " + NetFormat.netDoubleToString(Math.rint(manaRegenOutFSR)) + " + " + NetFormat.netDoubleToString(Math.rint(replenishMP5)) + "*All values are Mana per 5 seconds.\nMP5 while casting / MP5 while not casting + Replenishment");

        dictValues.put("Lightning Bolt", NetFormat.netDoubleToString(Math.rint(lightningBolt.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(lightningBolt.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(lightningBolt.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(lightningBolt.getMaxCrit())) + "*Mana cost: " + NetFormat.netDoubleToString(Math.rint(lightningBolt.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * lightningBolt.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * lightningBolt.getMissChance(), 2)) + " %\nCast time: " + NetFormat.netDoubleToString(netRound(lightningBolt.getCastTime(), 2)) + " sec.\nClearcast uptime: " + NetFormat.netDoubleToString(netRound(100f * clearCast_LightningBolt, 2)) + " %");
        dictValues.put("Chain Lightning", NetFormat.netDoubleToString(Math.rint(chainLightning.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(chainLightning.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(chainLightning.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(chainLightning.getMaxCrit())) + "*Targets: " + (((ChainLightning) chainLightning).getAdditionalTargets() + 1) + "\nMana cost: " + NetFormat.netDoubleToString(Math.rint(chainLightning.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * chainLightning.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * chainLightning.getMissChance(), 2)) + " %\nCast time: " + NetFormat.netDoubleToString(netRound(chainLightning.getCastTime(), 2)) + " sec.");
        dictValues.put("Lava Burst", NetFormat.netDoubleToString(Math.rint(lavaBurst.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(lavaBurst.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(lavaBurst.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(lavaBurst.getMaxCrit())) + "*Mana cost: " + NetFormat.netDoubleToString(Math.rint(lavaBurst.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * lavaBurst.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * lavaBurst.getMissChance(), 2)) + " %\nCast time: " + NetFormat.netDoubleToString(netRound(lavaBurst.getCastTime(), 2)) + " sec.\nClearcast uptime: " + NetFormat.netDoubleToString(netRound(100f * clearCast_LavaBurst, 2)) + " %");
        dictValues.put("Flame Shock", NetFormat.netDoubleToString(Math.rint(flameShock.getAvgHit())) + " / " + NetFormat.netDoubleToString(Math.rint(flameShock.getAvgCrit())) + " + " + NetFormat.netDoubleToString(Math.rint(flameShock.getPeriodicTick())) + " (dot)*Duration: " + NetFormat.netFloatToString(flameShock.getDuration()) + "\nTicks every " + NetFormat.netDoubleToString(netRound(flameShock.getPeriodicTickTime(), 2)) + "s\nMana cost: " + NetFormat.netDoubleToString(Math.rint(flameShock.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * flameShock.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * flameShock.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(flameShock.getCastTime(), 2)) + " sec.\nClearcast uptime: " + NetFormat.netDoubleToString(netRound(100f * clearCast_FlameShock, 2)) + " %");
        dictValues.put("Earth Shock", NetFormat.netDoubleToString(Math.rint(earthShock.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(earthShock.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(earthShock.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(earthShock.getMaxCrit())) + "*Mana cost: " + NetFormat.netDoubleToString(Math.rint(earthShock.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * earthShock.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * earthShock.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(earthShock.getCastTime(), 2)) + " sec.\n");
        dictValues.put("Frost Shock", NetFormat.netDoubleToString(Math.rint(frostShock.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(frostShock.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(frostShock.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(frostShock.getMaxCrit())) + "*Mana cost: " + NetFormat.netDoubleToString(Math.rint(frostShock.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * frostShock.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * frostShock.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(frostShock.getCastTime(), 2)) + " sec.\n");
        dictValues.put("Fire Nova", NetFormat.netDoubleToString(Math.rint(fireNova.getMinHit())) + "-" + NetFormat.netDoubleToString(Math.rint(fireNova.getMaxHit())) + " / " + NetFormat.netDoubleToString(Math.rint(fireNova.getMinCrit())) + "-" + NetFormat.netDoubleToString(Math.rint(fireNova.getMaxCrit())) + "*Targets: " + (((FireNova) fireNova).getAdditionalTargets() + 1) + "\nMana cost: " + NetFormat.netDoubleToString(Math.rint(fireNova.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * fireNova.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * fireNova.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(fireNova.getCastTime(), 2)) + " sec.\n");
        dictValues.put("Searing Totem", NetFormat.netDoubleToString(Math.rint(searingTotem.getPeriodicTick())) + "*Mana cost: " + NetFormat.netDoubleToString(Math.rint(searingTotem.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * searingTotem.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * searingTotem.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(searingTotem.getCastTime(), 2)) + " sec.\n");
        dictValues.put("Magma Totem", NetFormat.netDoubleToString(Math.rint(magmaTotem.getPeriodicTick())) + "*Targets: " + (((MagmaTotem) magmaTotem).getAdditionalTargets() + 1) + "\nMana cost: " + NetFormat.netDoubleToString(Math.rint(magmaTotem.getManaCost())) + "\nCrit chance: " + NetFormat.netDoubleToString(netRound(100f * magmaTotem.getCritChance(), 2)) + " %\nMiss chance: " + NetFormat.netDoubleToString(netRound(100f * magmaTotem.getMissChance(), 2)) + " %\nGCD: " + NetFormat.netDoubleToString(netRound(magmaTotem.getCastTime(), 2)) + " sec.\n");

        dictValues.put("Simulation", NetFormat.netDoubleToString(Math.rint(rotationDPS)) + ((Math.abs(rotationDPS - totalDPS) >= 1) ? (" (" + NetFormat.netDoubleToString(Math.rint(totalDPS)) + ")") : "") + " DPS*OOM after " + NetFormat.netDoubleToString(Math.rint(timeToOOM)) + " sec.\nDPS until OOM: " + NetFormat.netDoubleToString(Math.rint(rotationDPS)) + "\nMPS until OOM: " + NetFormat.netDoubleToString(Math.rint(rotationMPS)) + "\nCast vs regen fraction after OOM: " + NetFormat.netDoubleToString(netRound(castRegenFraction, 4)) + "\n" + NetFormat.netDoubleToString(netRound(60f * castsPerSecond, 1)) + " casts per minute\n" + NetFormat.netDoubleToString(netRound(60f * critsPerSecond, 1)) + " crits per minute\n" + NetFormat.netDoubleToString(netRound(60f * missesPerSecond, 1)) + " misses per minute\n" + NetFormat.netDoubleToString(netRound(60f * lvBPerSecond, 1)) + " Lava Bursts per minute\n" + NetFormat.netDoubleToString(netRound(60f * fSPerSecond, 1)) + " Flame Shocks per minute\n" + NetFormat.netDoubleToString(netRound(60f * lBPerSecond, 1)) + " Lightning Bolts per minute\n" + NetFormat.netDoubleToString(netRound(60f * latencyPerSecond, 1)) + " seconds lost to latency per minute\n");
        dictValues.put("Rotation", rotation + "*" + rotationDetails);

        return dictValues;
    }

    /** .NET Math.Round(double, digits) behaves as rescaled banker's rounding on these inputs. */
    private static double netRound(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }
}
