// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Bear/CalculationOptionsBear.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsCat).
package rawr.models.bear;

import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

public class CalculationOptionsBear implements ICalculationOptionBase, ICharacterCalculationOptions {
    private float threatScale = 10f;

    public float getThreatScale() {
        return threatScale;
    }

    public void setThreatScale(float value) {
        threatScale = value;
    }

    private int survivalSoftCap = 160000;

    public int getSurvivalSoftCap() {
        return survivalSoftCap;
    }

    public void setSurvivalSoftCap(int value) {
        survivalSoftCap = value;
    }

    private float temporarySurvivalScale = 1f;

    public float getTemporarySurvivalScale() {
        return temporarySurvivalScale;
    }

    public void setTemporarySurvivalScale(float value) {
        temporarySurvivalScale = value;
    }

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

    private int targetDamage = 65000;

    public int getTargetDamage() {
        return targetDamage;
    }

    public void setTargetDamage(int value) {
        targetDamage = value;
    }

    private float targetAttackSpeed = 2.0f;

    public float getTargetAttackSpeed() {
        return targetAttackSpeed;
    }

    public void setTargetAttackSpeed(float value) {
        targetAttackSpeed = value;
    }

    private boolean targetParryHastes = true;

    public boolean getTargetParryHastes() {
        return targetParryHastes;
    }

    public void setTargetParryHastes(boolean value) {
        targetParryHastes = value;
    }

    private boolean customUseMaul = false;

    public boolean getCustomUseMaul() {
        return customUseMaul;
    }

    public void setCustomUseMaul(boolean value) {
        customUseMaul = value;
    }

    private boolean customUseMangle = false;

    public boolean getCustomUseMangle() {
        return customUseMangle;
    }

    public void setCustomUseMangle(boolean value) {
        customUseMangle = value;
    }

    private boolean customUseSwipe = false;

    public boolean getCustomUseSwipe() {
        return customUseSwipe;
    }

    public void setCustomUseSwipe(boolean value) {
        customUseSwipe = value;
    }

    private boolean customUseFaerieFire = false;

    public boolean getCustomUseFaerieFire() {
        return customUseFaerieFire;
    }

    public void setCustomUseFaerieFire(boolean value) {
        customUseFaerieFire = value;
    }

    private boolean customUseLacerate = false;

    public boolean getCustomUseLacerate() {
        return customUseLacerate;
    }

    public void setCustomUseLacerate(boolean value) {
        customUseLacerate = value;
    }

    private Character character;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    // C# CalculationOptionsBear.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsBear xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<ThreatScale>").append(NetFormat.floatR(threatScale)).append("</ThreatScale>");
        sb.append("<SurvivalSoftCap>").append(survivalSoftCap).append("</SurvivalSoftCap>");
        sb.append("<TemporarySurvivalScale>").append(NetFormat.floatR(temporarySurvivalScale)).append("</TemporarySurvivalScale>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(targetArmor).append("</TargetArmor>");
        sb.append("<TargetDamage>").append(targetDamage).append("</TargetDamage>");
        sb.append("<TargetAttackSpeed>").append(NetFormat.floatR(targetAttackSpeed)).append("</TargetAttackSpeed>");
        sb.append("<TargetParryHastes>").append(targetParryHastes ? "true" : "false").append("</TargetParryHastes>");
        sb.append("<CustomUseMaul>").append(customUseMaul ? "true" : "false").append("</CustomUseMaul>");
        sb.append("<CustomUseMangle>").append(customUseMangle ? "true" : "false").append("</CustomUseMangle>");
        sb.append("<CustomUseSwipe>").append(customUseSwipe ? "true" : "false").append("</CustomUseSwipe>");
        sb.append("<CustomUseFaerieFire>").append(customUseFaerieFire ? "true" : "false").append("</CustomUseFaerieFire>");
        sb.append("<CustomUseLacerate>").append(customUseLacerate ? "true" : "false").append("</CustomUseLacerate>");
        sb.append("</CalculationOptionsBear>");
        return sb.toString();
    }
}
