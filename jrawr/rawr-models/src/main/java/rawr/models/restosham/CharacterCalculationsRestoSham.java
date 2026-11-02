// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/CharacterCalculationsRestoSham.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: `Math.Round(x, 0).ToString()` / `Math.Round(x, 2)` use the
// rawr.models.mage.NetFormat formatters and the local scale-rint helper (verified against Mono's
// Math.Round(double, int), see docs/dev/DECISIONS.md); string.Format is String.format(Locale.ROOT, ...).
package rawr.models.restosham;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.models.mage.NetFormat;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CharacterCalculationsRestoSham extends CharacterCalculationsBase {
    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    private Stats basicStats;

    public Stats getBasicStats() {
        return basicStats;
    }

    public void setBasicStats(Stats value) {
        basicStats = value;
    }

    // region Displayed Calculations
    private float spellCrit;
    private float spellHaste;
    private float totalManaPool;
    private float fightHPS;
    private String sustainedSequence;
    private String burstSequence;
    private float maps;
    private float manaUsed;
    private float hstHeals;
    private float eshps;
    private float hwSpamHPS;
    private float hwSpamMPS;
    private float lhwSpamHPS;
    private float lhwSpamMPS;
    private float chSpamHPS;
    private float chSpamMPS;
    private float rthwHPS;
    private float rthwMPS;
    private float rtlhwHPS;
    private float rtlhwMPS;
    private float rtchHPS;
    private float rtchMPS;
    private float realHWCast;
    private float realLHWCast;
    private float realCHCast;
    private float lbCast;
    private float burstHPS;
    private float sustainedHPS;
    private float mups;
    private float survival;
    // endregion

    public float getSpellCrit() {
        return spellCrit;
    }

    public void setSpellCrit(float value) {
        spellCrit = value;
    }

    public float getSpellHaste() {
        return spellHaste;
    }

    public void setSpellHaste(float value) {
        spellHaste = value;
    }

    public float getTotalManaPool() {
        return totalManaPool;
    }

    public void setTotalManaPool(float value) {
        totalManaPool = value;
    }

    public float getFightHPS() {
        return fightHPS;
    }

    public void setFightHPS(float value) {
        fightHPS = value;
    }

    public String getSustainedSequence() {
        return sustainedSequence;
    }

    public void setSustainedSequence(String value) {
        sustainedSequence = value;
    }

    public String getBurstSequence() {
        return burstSequence;
    }

    public void setBurstSequence(String value) {
        burstSequence = value;
    }

    public float getMAPS() {
        return maps;
    }

    public void setMAPS(float value) {
        maps = value;
    }

    public float getManaUsed() {
        return manaUsed;
    }

    public void setManaUsed(float value) {
        manaUsed = value;
    }

    public float getHSTHeals() {
        return hstHeals;
    }

    public void setHSTHeals(float value) {
        hstHeals = value;
    }

    public float getESHPS() {
        return eshps;
    }

    public void setESHPS(float value) {
        eshps = value;
    }

    public float getHWSpamHPS() {
        return hwSpamHPS;
    }

    public void setHWSpamHPS(float value) {
        hwSpamHPS = value;
    }

    public float getHWSpamMPS() {
        return hwSpamMPS;
    }

    public void setHWSpamMPS(float value) {
        hwSpamMPS = value;
    }

    public float getLHWSpamHPS() {
        return lhwSpamHPS;
    }

    public void setLHWSpamHPS(float value) {
        lhwSpamHPS = value;
    }

    public float getLHWSpamMPS() {
        return lhwSpamMPS;
    }

    public void setLHWSpamMPS(float value) {
        lhwSpamMPS = value;
    }

    public float getCHSpamHPS() {
        return chSpamHPS;
    }

    public void setCHSpamHPS(float value) {
        chSpamHPS = value;
    }

    public float getCHSpamMPS() {
        return chSpamMPS;
    }

    public void setCHSpamMPS(float value) {
        chSpamMPS = value;
    }

    public float getRTHWHPS() {
        return rthwHPS;
    }

    public void setRTHWHPS(float value) {
        rthwHPS = value;
    }

    public float getRTHWMPS() {
        return rthwMPS;
    }

    public void setRTHWMPS(float value) {
        rthwMPS = value;
    }

    public float getRTLHWHPS() {
        return rtlhwHPS;
    }

    public void setRTLHWHPS(float value) {
        rtlhwHPS = value;
    }

    public float getRTLHWMPS() {
        return rtlhwMPS;
    }

    public void setRTLHWMPS(float value) {
        rtlhwMPS = value;
    }

    public float getRTCHHPS() {
        return rtchHPS;
    }

    public void setRTCHHPS(float value) {
        rtchHPS = value;
    }

    public float getRTCHMPS() {
        return rtchMPS;
    }

    public void setRTCHMPS(float value) {
        rtchMPS = value;
    }

    public float getRealHWCast() {
        return realHWCast;
    }

    public void setRealHWCast(float value) {
        realHWCast = value;
    }

    public float getRealLHWCast() {
        return realLHWCast;
    }

    public void setRealLHWCast(float value) {
        realLHWCast = value;
    }

    public float getRealCHCast() {
        return realCHCast;
    }

    public void setRealCHCast(float value) {
        realCHCast = value;
    }

    public float getLBCast() {
        return lbCast;
    }

    public void setLBCast(float value) {
        lbCast = value;
    }

    public float getBurstHPS() {
        return burstHPS;
    }

    public void setBurstHPS(float value) {
        burstHPS = value;
    }

    public float getSustainedHPS() {
        return sustainedHPS;
    }

    public void setSustainedHPS(float value) {
        sustainedHPS = value;
    }

    public float getMUPS() {
        return mups;
    }

    public void setMUPS(float value) {
        mups = value;
    }

    public float getSurvival() {
        return survival;
    }

    public void setSurvival(float value) {
        survival = value;
    }

    // .NET Math.Round(value, digits) on this model's inputs behaves as rescaled banker's rounding
    // (verified against Mono 6.14: 0.145->0.14, 2.135->2.13, 51.625->51.62).
    private static double netRound(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> values = new LinkedHashMap<String, String>();

        values.put("HPS - Burst", NetFormat.netDoubleToString(netRound(burstHPS, 0)));
        values.put("HPS - Sustained", NetFormat.netDoubleToString(netRound(sustainedHPS, 0)));
        values.put("Survival", NetFormat.netDoubleToString(netRound(survival, 0)));
        values.put("Health", NetFormat.netDoubleToString(netRound(basicStats.getHealth(), 0)));
        values.put("Stamina", NetFormat.netDoubleToString(netRound(basicStats.getStamina(), 0)));
        values.put("Intellect", NetFormat.netDoubleToString(netRound(basicStats.getIntellect(), 0)));
        values.put("Spell Power", NetFormat.netDoubleToString(netRound(basicStats.getSpellPower(), 0)));
        values.put("Mana", NetFormat.netDoubleToString(netRound(basicStats.getMana(), 0)));
        values.put("MP5", NetFormat.netFloatToString(basicStats.getMp5()));
        values.put("Mana Available", NetFormat.netDoubleToString(netRound(manaUsed, 0)));
        values.put("Heal Spell Crit", String.format(Locale.ROOT, "%s%%*%s spell crit rating",
                NetFormat.netDoubleToString(netRound((double) (spellCrit * 100), 2)), NetFormat.netFloatToString(basicStats.getCritRating())));
        values.put("Spell Haste", String.format(Locale.ROOT, "%s%%*%s spell haste rating",
                NetFormat.netDoubleToString(netRound((double) (spellHaste * 100), 2)), NetFormat.netFloatToString(basicStats.getHasteRating())));
        values.put("Burst Sequence", burstSequence);
        values.put("Sustained Sequence", sustainedSequence);
        values.put("Mana Available per Second", NetFormat.netDoubleToString(netRound(maps, 0)));
        values.put("Mana Used per Second", NetFormat.netDoubleToString(netRound(mups, 0)));
        values.put("Healing Stream HPS", NetFormat.netDoubleToString(netRound(hstHeals, 0)));
        values.put("Earth Shield HPS", NetFormat.netDoubleToString(netRound(eshps, 0)));
        values.put("RT+HW HPS", NetFormat.netDoubleToString(netRound(rthwHPS, 0)));
        values.put("RT+LHW HPS", NetFormat.netDoubleToString(netRound(rtlhwHPS, 0)));
        values.put("RT+CH HPS", NetFormat.netDoubleToString(netRound(rtchHPS, 0)));
        values.put("HW Spam HPS", NetFormat.netDoubleToString(netRound(hwSpamHPS, 0)));
        values.put("LHW Spam HPS", NetFormat.netDoubleToString(netRound(lhwSpamHPS, 0)));
        values.put("CH Spam HPS", NetFormat.netDoubleToString(netRound(chSpamHPS, 0)));
        values.put("Global Cooldown", String.format(Locale.ROOT, "%ss*%s haste rating to cap",
                NetFormat.f2(Math.max(1.5f / (1f + spellHaste), 1f)),
                // FIXME: I'm not entirely certain if this is accurate. Please double-check.
                NetFormat.netDoubleToString(Math.ceil(((1.5f / (1f + spellHaste)) - 1f) * StatConversion.RATING_PER_SPELLHASTE))));

        // These all use string.Format() so they always have 2 digits after the decimal
        values.put("Healing Wave", String.format(Locale.ROOT, "%ss / %ss", NetFormat.f2(realHWCast), NetFormat.d2(realHWCast * 0.7)));
        values.put("Lesser Healing Wave", String.format(Locale.ROOT, "%ss", NetFormat.f2(realLHWCast)));
        values.put("Chain Heal", String.format(Locale.ROOT, "%ss", NetFormat.f2(realCHCast)));
        values.put("Lightning Bolt", String.format(Locale.ROOT, "%ss", NetFormat.f2(lbCast)));

        return values;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Mana Usable per Second":
                return maps;
            case "Health":
                return basicStats.getHealth();
            case "Haste %":
                return (float) netRound((double) (spellHaste * 100), 2);
            case "Crit %":
                return (float) netRound((double) (spellCrit * 100), 2);
        }
        return 0f;
    }
}
