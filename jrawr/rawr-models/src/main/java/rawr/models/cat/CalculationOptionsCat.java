// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Cat/CalculationOptionsCat.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsRestoSham).
package rawr.models.cat;

import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

public class CalculationOptionsCat implements ICalculationOptionBase, ICharacterCalculationOptions {
    private int targetLevel = 83;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private int targetArmor = (int) StatConversion.NPC_ARMOR[83 - 80];

    public int getTargetArmor() {
        return targetArmor;
    }

    public void setTargetArmor(int value) {
        targetArmor = value;
    }

    private boolean customUseShred = false;

    public boolean getCustomUseShred() {
        return customUseShred;
    }

    public void setCustomUseShred(boolean value) {
        customUseShred = value;
    }

    private boolean customUseRip = false;

    public boolean getCustomUseRip() {
        return customUseRip;
    }

    public void setCustomUseRip(boolean value) {
        customUseRip = value;
    }

    private boolean customUseRake = false;

    public boolean getCustomUseRake() {
        return customUseRake;
    }

    public void setCustomUseRake(boolean value) {
        customUseRake = value;
    }

    private int customCPFerociousBite = 0;

    public int getCustomCPFerociousBite() {
        return customCPFerociousBite;
    }

    public void setCustomCPFerociousBite(int value) {
        customCPFerociousBite = value;
    }

    private int customCPSavageRoar = 5;

    public int getCustomCPSavageRoar() {
        return customCPSavageRoar;
    }

    public void setCustomCPSavageRoar(int value) {
        customCPSavageRoar = value;
    }

    private int duration = 300;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int value) {
        duration = value;
    }

    private float trinketOffset = 0f;

    public float getTrinketOffset() {
        return trinketOffset;
    }

    public void setTrinketOffset(float value) {
        trinketOffset = value;
    }

    private int lagVariance = 200;

    public int getLagVariance() {
        return lagVariance;
    }

    public void setLagVariance(int value) {
        lagVariance = value;
    }

    private Character character;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    // C# CalculationOptionsCat.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsCat xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(targetArmor).append("</TargetArmor>");
        sb.append("<CustomUseShred>").append(customUseShred ? "true" : "false").append("</CustomUseShred>");
        sb.append("<CustomUseRip>").append(customUseRip ? "true" : "false").append("</CustomUseRip>");
        sb.append("<CustomUseRake>").append(customUseRake ? "true" : "false").append("</CustomUseRake>");
        sb.append("<CustomCPFerociousBite>").append(customCPFerociousBite).append("</CustomCPFerociousBite>");
        sb.append("<CustomCPSavageRoar>").append(customCPSavageRoar).append("</CustomCPSavageRoar>");
        sb.append("<Duration>").append(duration).append("</Duration>");
        sb.append("<TrinketOffset>").append(NetFormat.floatR(trinketOffset)).append("</TrinketOffset>");
        sb.append("<LagVariance>").append(lagVariance).append("</LagVariance>");
        sb.append("</CalculationOptionsCat>");
        return sb.toString();
    }
}
