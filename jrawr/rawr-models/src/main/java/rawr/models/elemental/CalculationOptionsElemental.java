// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/CalculationOptionsElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port). getXml writes the
// XmlSerializer element order observed in the oracle fixtures (FightDuration, BSRatio,
// UseThunderstorm, RotationType, LatencyGcd, LatencyCast, NumberOfTargets, UseFireNova,
// UseChainLightning, UseDpsTotem, UseFireEle).
package rawr.models.elemental;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

public class CalculationOptionsElemental implements ICalculationOptionBase {
    public String getXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        xml.append("<CalculationOptionsElemental xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        xml.append("<FightDuration>").append(fightDuration).append("</FightDuration>");
        xml.append("<BSRatio>").append(bsRatio).append("</BSRatio>");
        xml.append("<UseThunderstorm>").append(useThunderstorm ? "true" : "false").append("</UseThunderstorm>");
        xml.append("<RotationType>").append(rotationType).append("</RotationType>");
        xml.append("<LatencyGcd>").append(NetFormat.floatR(latencyGcd)).append("</LatencyGcd>");
        xml.append("<LatencyCast>").append(NetFormat.floatR(latencyCast)).append("</LatencyCast>");
        xml.append("<NumberOfTargets>").append(numberOfTargets).append("</NumberOfTargets>");
        xml.append("<UseFireNova>").append(useFireNova ? "true" : "false").append("</UseFireNova>");
        xml.append("<UseChainLightning>").append(useChainLightning ? "true" : "false").append("</UseChainLightning>");
        xml.append("<UseDpsTotem>").append(useDpsTotem ? "true" : "false").append("</UseDpsTotem>");
        xml.append("<UseFireEle>").append(useFireEle ? "true" : "false").append("</UseFireEle>");
        xml.append("</CalculationOptionsElemental>");
        return xml.toString();
    }

    public CharacterCalculationsElemental calculatedStats = null;

    private int fightDuration = 300; //5 Minutes

    public int getFightDuration() {
        return fightDuration;
    }

    public void setFightDuration(int value) {
        fightDuration = value;
    }

    private int bsRatio = 75; // goes from 0 to 100

    public int getBSRatio() {
        return bsRatio;
    }

    public void setBSRatio(int value) {
        bsRatio = value;
    }

    private boolean useThunderstorm = true;

    public boolean getUseThunderstorm() {
        return useThunderstorm;
    }

    public void setUseThunderstorm(boolean value) {
        useThunderstorm = value;
    }

    private int rotationType = 0;

    public int getRotationType() {
        return rotationType;
    }

    public void setRotationType(int value) {
        rotationType = value;
    }

    //latency in s
    private float latencyGcd = .15f;

    public float getLatencyGcd() {
        return latencyGcd;
    }

    public void setLatencyGcd(float value) {
        latencyGcd = value;
    }

    private float latencyCast = .075f;

    public float getLatencyCast() {
        return latencyCast;
    }

    public void setLatencyCast(float value) {
        latencyCast = value;
    }

    private int numberOfTargets = 1;

    public int getNumberOfTargets() {
        return numberOfTargets;
    }

    public void setNumberOfTargets(int value) {
        numberOfTargets = value;
    }

    private boolean useFireNova = true;

    public boolean getUseFireNova() {
        return useFireNova;
    }

    public void setUseFireNova(boolean value) {
        useFireNova = value;
    }

    private boolean useChainLightning = true;

    public boolean getUseChainLightning() {
        return useChainLightning;
    }

    public void setUseChainLightning(boolean value) {
        useChainLightning = value;
    }

    private boolean useDpsTotem = false;

    public boolean getUseDpsTotem() {
        return useDpsTotem;
    }

    public void setUseDpsTotem(boolean value) {
        useDpsTotem = value;
    }

    private boolean useFireEle = false;

    public boolean getUseFireEle() {
        return useFireEle;
    }

    public void setUseFireEle(boolean value) {
        useFireEle = value;
    }
}
