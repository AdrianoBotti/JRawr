// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/CalculationOptionsMoonkin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order observed in the oracle fixtures (TargetLevel, Latency, ExternalLatency,
// FightLength, Innervate, InnervateDelay, AldorScryer, ReplenishmentUptime, TreantLifespan,
// UserRotation, PTRMode).
package rawr.models.moonkin;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

public class CalculationOptionsMoonkin implements ICalculationOptionBase {
    private int targetLevel = 83;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private float latency = 0.2f;

    public float getLatency() {
        return latency;
    }

    public void setLatency(float value) {
        latency = value;
    }

    public float getExternalLatency() {
        return latency * 1000f;
    }

    public void setExternalLatency(float value) {
        latency = value / 1000f;
    }

    private float fightLength = 5;

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    private boolean innervate = false;

    public boolean getInnervate() {
        return innervate;
    }

    public void setInnervate(boolean value) {
        innervate = value;
    }

    private float innervateDelay = 1;

    public float getInnervateDelay() {
        return innervateDelay;
    }

    public void setInnervateDelay(float value) {
        innervateDelay = value;
    }

    private String aldorScryer = "Aldor";

    public String getAldorScryer() {
        return aldorScryer;
    }

    public void setAldorScryer(String value) {
        aldorScryer = value;
    }

    private float replenishmentUptime = 1.0f;

    public float getReplenishmentUptime() {
        return replenishmentUptime;
    }

    public void setReplenishmentUptime(float value) {
        replenishmentUptime = value;
    }

    private float treantLifespan = 1.0f;

    public float getTreantLifespan() {
        return treantLifespan;
    }

    public void setTreantLifespan(float value) {
        treantLifespan = value;
    }

    private String userRotation = "None";

    public String getUserRotation() {
        return userRotation;
    }

    public void setUserRotation(String value) {
        userRotation = value;
    }

    private boolean ptrMode = false;

    public boolean getPTRMode() {
        return ptrMode;
    }

    public void setPTRMode(boolean value) {
        ptrMode = value;
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsMoonkin xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<Latency>").append(NetFormat.floatR(latency)).append("</Latency>");
        sb.append("<ExternalLatency>").append(NetFormat.floatR(getExternalLatency())).append("</ExternalLatency>");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<Innervate>").append(innervate ? "true" : "false").append("</Innervate>");
        sb.append("<InnervateDelay>").append(NetFormat.floatR(innervateDelay)).append("</InnervateDelay>");
        sb.append("<AldorScryer>").append(aldorScryer).append("</AldorScryer>");
        sb.append("<ReplenishmentUptime>").append(NetFormat.floatR(replenishmentUptime)).append("</ReplenishmentUptime>");
        sb.append("<TreantLifespan>").append(NetFormat.floatR(treantLifespan)).append("</TreantLifespan>");
        sb.append("<UserRotation>").append(userRotation).append("</UserRotation>");
        sb.append("<PTRMode>").append(ptrMode ? "true" : "false").append("</PTRMode>");
        sb.append("</CalculationOptionsMoonkin>");
        return sb.toString();
    }
}
