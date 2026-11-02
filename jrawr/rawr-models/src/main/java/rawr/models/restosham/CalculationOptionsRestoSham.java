// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/CalculationOptionsRestoSham.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); NumericField (only used
// by the UI options panel) is omitted; getXml writes the XmlSerializer element order by hand.
package rawr.models.restosham;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

public class CalculationOptionsRestoSham implements ICalculationOptionBase {
    /**
     * Fight length, in minutes.
     */
    private float fightLength = 5.0f;

    /**
     * Whether a Mana Tide totem is placed every time the cooldown is up.
     */
    private boolean manaTideEveryCD = true;

    /**
     * Count of Innervates you get.
     */
    private float innervates = 0f;

    /**
     * Whether we keep Water Shield up or not (could use Earth Shield during some fights).
     */
    private boolean waterShield = true;

    /**
     * Earth shield use.
     */
    private boolean earthShield = true;

    /**
     * Your style of healing.
     */
    private String burstStyle = "RT+HW";

    /**
     * Your style of healing.
     */
    private String sustStyle = "RT+CH";

    /**
     * Targets.
     */
    private String targets = "Raid";

    /**
     * Will you or someone else use heroism?
     */
    private String heroism = "Me";

    /**
     * The percentage of time that Replenishment is "up".
     */
    private float replenishmentPercentage = 90f;

    /**
     * The number of times you use Cleanse Spirit.
     */
    private float decurse = 0f;

    /**
     * The latency in milliseconds.
     */
    private float latency = 60f;

    /**
     * The percentage of usefulness survival is to you.
     */
    private float survivalPerc = 2f;

    /**
     * The percentage of time you are active.
     */
    private float activityPerc = 85f;

    /**
     * The number of times water shield pops per minute.
     */
    private float wsPops = 0f;

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    public boolean getManaTideEveryCD() {
        return manaTideEveryCD;
    }

    public void setManaTideEveryCD(boolean value) {
        manaTideEveryCD = value;
    }

    public float getInnervates() {
        return innervates;
    }

    public void setInnervates(float value) {
        innervates = value;
    }

    public boolean getWaterShield() {
        return waterShield;
    }

    public void setWaterShield(boolean value) {
        waterShield = value;
    }

    public boolean getEarthShield() {
        return earthShield;
    }

    public void setEarthShield(boolean value) {
        earthShield = value;
    }

    public String getBurstStyle() {
        return burstStyle;
    }

    public void setBurstStyle(String value) {
        burstStyle = value;
    }

    public String getSustStyle() {
        return sustStyle;
    }

    public void setSustStyle(String value) {
        sustStyle = value;
    }

    public String getTargets() {
        return targets;
    }

    public void setTargets(String value) {
        targets = value;
    }

    public String getHeroism() {
        return heroism;
    }

    public void setHeroism(String value) {
        heroism = value;
    }

    public float getReplenishmentPercentage() {
        return replenishmentPercentage;
    }

    public void setReplenishmentPercentage(float value) {
        replenishmentPercentage = value;
    }

    public float getDecurse() {
        return decurse;
    }

    public void setDecurse(float value) {
        decurse = value;
    }

    public float getLatency() {
        return latency;
    }

    public void setLatency(float value) {
        latency = value;
    }

    public float getSurvivalPerc() {
        return survivalPerc;
    }

    public void setSurvivalPerc(float value) {
        survivalPerc = value;
    }

    public float getActivityPerc() {
        return activityPerc;
    }

    public void setActivityPerc(float value) {
        activityPerc = value;
    }

    public float getWSPops() {
        return wsPops;
    }

    public void setWSPops(float value) {
        wsPops = value;
    }

    // C# CalculationOptionsRestoSham.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsRestoSham xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<ManaTideEveryCD>").append(manaTideEveryCD ? "true" : "false").append("</ManaTideEveryCD>");
        sb.append("<Innervates>").append(NetFormat.floatR(innervates)).append("</Innervates>");
        sb.append("<WaterShield>").append(waterShield ? "true" : "false").append("</WaterShield>");
        sb.append("<EarthShield>").append(earthShield ? "true" : "false").append("</EarthShield>");
        sb.append("<BurstStyle>").append(burstStyle).append("</BurstStyle>");
        sb.append("<SustStyle>").append(sustStyle).append("</SustStyle>");
        sb.append("<Targets>").append(targets).append("</Targets>");
        sb.append("<Heroism>").append(heroism).append("</Heroism>");
        sb.append("<ReplenishmentPercentage>").append(NetFormat.floatR(replenishmentPercentage)).append("</ReplenishmentPercentage>");
        sb.append("<Decurse>").append(NetFormat.floatR(decurse)).append("</Decurse>");
        sb.append("<Latency>").append(NetFormat.floatR(latency)).append("</Latency>");
        sb.append("<SurvivalPerc>").append(NetFormat.floatR(survivalPerc)).append("</SurvivalPerc>");
        sb.append("<ActivityPerc>").append(NetFormat.floatR(activityPerc)).append("</ActivityPerc>");
        sb.append("<WSPops>").append(NetFormat.floatR(wsPops)).append("</WSPops>");
        sb.append("</CalculationOptionsRestoSham>");
        return sb.toString();
    }
}
