// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Rogue/CalculationOptionsRogue.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: INotifyPropertyChanged is not ported (UI-only); GetXml writes the
// XmlSerializer element order by hand and deserializeDataObject lives in CalculationsRogue.
package rawr.models.rogue;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

public class CalculationOptionsRogue implements ICalculationOptionBase {

    private int targetLevel = 83;

    private int targetArmor = (int) StatConversion.NPC_ARMOR[83 - 80];

    private boolean targetPoisonable = true;

    private boolean bleedIsUp = true;

    private int duration = 300;

    private int lagVariance = 200;

    private int customCPG = 1;

    private boolean forceCustom = false;

    private boolean customUseRupt = false;

    private boolean customUseExpose = false;

    private boolean customUseTotT = false;

    private int customFinisher = 1;

    private int customCPFinisher = 5;

    private int customCPSnD = 5;

    private int customMHPoison = 1;

    private int customOHPoison = 2;

    private boolean enableMuti = true;

    private boolean enableSS = true;

    private boolean enableBS = true;

    private boolean enableHemo = true;

    private boolean enableIP = true;

    private boolean enableDP = true;

    private boolean enableWP = true;

    private boolean enableRupt = true;

    private boolean enableEvis = true;

    private boolean enableEnvenom = true;

    private float trinketOffset = 0f;

    private boolean ptrMode = false;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    public int getTargetArmor() {
        return targetArmor;
    }

    public void setTargetArmor(int value) {
        targetArmor = value;
    }

    public boolean getTargetPoisonable() {
        return targetPoisonable;
    }

    public void setTargetPoisonable(boolean value) {
        targetPoisonable = value;
    }

    public boolean getBleedIsUp() {
        return bleedIsUp;
    }

    public void setBleedIsUp(boolean value) {
        bleedIsUp = value;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int value) {
        duration = value;
    }

    public int getLagVariance() {
        return lagVariance;
    }

    public void setLagVariance(int value) {
        lagVariance = value;
    }

    public int getCustomCPG() {
        return customCPG;
    }

    public void setCustomCPG(int value) {
        customCPG = value;
    }

    public boolean getForceCustom() {
        return forceCustom;
    }

    public void setForceCustom(boolean value) {
        forceCustom = value;
    }

    public boolean getCustomUseRupt() {
        return customUseRupt;
    }

    public void setCustomUseRupt(boolean value) {
        customUseRupt = value;
    }

    public boolean getCustomUseExpose() {
        return customUseExpose;
    }

    public void setCustomUseExpose(boolean value) {
        customUseExpose = value;
    }

    public boolean getCustomUseTotT() {
        return customUseTotT;
    }

    public void setCustomUseTotT(boolean value) {
        customUseTotT = value;
    }

    public int getCustomFinisher() {
        return customFinisher;
    }

    public void setCustomFinisher(int value) {
        customFinisher = value;
    }

    public int getCustomCPFinisher() {
        return customCPFinisher;
    }

    public void setCustomCPFinisher(int value) {
        customCPFinisher = value;
    }

    public int getCustomCPSnD() {
        return customCPSnD;
    }

    public void setCustomCPSnD(int value) {
        customCPSnD = value;
    }

    public int getCustomMHPoison() {
        return customMHPoison;
    }

    public void setCustomMHPoison(int value) {
        customMHPoison = value;
    }

    public int getCustomOHPoison() {
        return customOHPoison;
    }

    public void setCustomOHPoison(int value) {
        customOHPoison = value;
    }

    public boolean getEnableMuti() {
        return enableMuti;
    }

    public void setEnableMuti(boolean value) {
        enableMuti = value;
    }

    public boolean getEnableSS() {
        return enableSS;
    }

    public void setEnableSS(boolean value) {
        enableSS = value;
    }

    public boolean getEnableBS() {
        return enableBS;
    }

    public void setEnableBS(boolean value) {
        enableBS = value;
    }

    public boolean getEnableHemo() {
        return enableHemo;
    }

    public void setEnableHemo(boolean value) {
        enableHemo = value;
    }

    public boolean getEnableIP() {
        return enableIP;
    }

    public void setEnableIP(boolean value) {
        enableIP = value;
    }

    public boolean getEnableDP() {
        return enableDP;
    }

    public void setEnableDP(boolean value) {
        enableDP = value;
    }

    public boolean getEnableWP() {
        return enableWP;
    }

    public void setEnableWP(boolean value) {
        enableWP = value;
    }

    public boolean getEnableRupt() {
        return enableRupt;
    }

    public void setEnableRupt(boolean value) {
        enableRupt = value;
    }

    public boolean getEnableEvis() {
        return enableEvis;
    }

    public void setEnableEvis(boolean value) {
        enableEvis = value;
    }

    public boolean getEnableEnvenom() {
        return enableEnvenom;
    }

    public void setEnableEnvenom(boolean value) {
        enableEnvenom = value;
    }

    public float getTrinketOffset() {
        return trinketOffset;
    }

    public void setTrinketOffset(float value) {
        trinketOffset = value;
    }

    public boolean getPTRMode() {
        return ptrMode;
    }

    public void setPTRMode(boolean value) {
        ptrMode = value;
    }

    // C# GetXml: writes the XmlSerializer element order for CalculationOptionsRogue.
    @Override
    public String getXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>\r\n");
        xml.append("<CalculationOptionsRogue xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n");
        element(xml, "TargetLevel", Integer.toString(targetLevel));
        element(xml, "TargetArmor", Integer.toString(targetArmor));
        element(xml, "TargetPoisonable", Boolean.toString(targetPoisonable));
        element(xml, "BleedIsUp", Boolean.toString(bleedIsUp));
        element(xml, "Duration", Integer.toString(duration));
        element(xml, "LagVariance", Integer.toString(lagVariance));
        element(xml, "CustomCPG", Integer.toString(customCPG));
        element(xml, "ForceCustom", Boolean.toString(forceCustom));
        element(xml, "CustomUseRupt", Boolean.toString(customUseRupt));
        element(xml, "CustomUseExpose", Boolean.toString(customUseExpose));
        element(xml, "CustomUseTotT", Boolean.toString(customUseTotT));
        element(xml, "CustomFinisher", Integer.toString(customFinisher));
        element(xml, "CustomCPFinisher", Integer.toString(customCPFinisher));
        element(xml, "CustomCPSnD", Integer.toString(customCPSnD));
        element(xml, "CustomMHPoison", Integer.toString(customMHPoison));
        element(xml, "CustomOHPoison", Integer.toString(customOHPoison));
        element(xml, "EnableMuti", Boolean.toString(enableMuti));
        element(xml, "EnableSS", Boolean.toString(enableSS));
        element(xml, "EnableBS", Boolean.toString(enableBS));
        element(xml, "EnableHemo", Boolean.toString(enableHemo));
        element(xml, "EnableIP", Boolean.toString(enableIP));
        element(xml, "EnableDP", Boolean.toString(enableDP));
        element(xml, "EnableWP", Boolean.toString(enableWP));
        element(xml, "EnableRupt", Boolean.toString(enableRupt));
        element(xml, "EnableEvis", Boolean.toString(enableEvis));
        element(xml, "EnableEnvenom", Boolean.toString(enableEnvenom));
        element(xml, "TrinketOffset", NetFormat.floatR(trinketOffset));
        element(xml, "PTRMode", Boolean.toString(ptrMode));
        xml.append("</CalculationOptionsRogue>");
        return xml.toString();
    }

    private static void element(StringBuilder xml, String name, String value) {
        xml.append("  <").append(name).append('>').append(value).append("</").append(name).append(">\r\n");
    }
}
