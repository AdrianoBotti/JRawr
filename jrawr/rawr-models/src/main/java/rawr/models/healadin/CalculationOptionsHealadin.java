// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/CalculationOptionsHealadin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsCat/Bear).
package rawr.models.healadin;

import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.util.NetFormat;

public class CalculationOptionsHealadin implements ICalculationOptionBase, ICharacterCalculationOptions {
    public CalculationOptionsHealadin() {
        length = 7;
        activity = .85f;
        replenishment = .9f;
        divinePlea = 2f;
        boLUp = 1f;
        holyShock = .15f;
        burstScale = .4f;
        gHL_Targets = 1f;
        infusionOfLight = true;
        ioLHolyLight = .9f;
        jotP = true;
        judgement = true;
        loHSelf = false;
        sSUptime = 1f;
        folOnTank = 0.25f;
        hitIrrelevant = true;
        spiritIrrelevant = true;
    }

    private float length;

    public float getLength() {
        return length;
    }

    public void setLength(float value) {
        length = value;
    }

    private float activity;

    public float getActivity() {
        return activity;
    }

    public void setActivity(float value) {
        activity = value;
    }

    private float replenishment;

    public float getReplenishment() {
        return replenishment;
    }

    public void setReplenishment(float value) {
        replenishment = value;
    }

    private float divinePlea;

    public float getDivinePlea() {
        return divinePlea;
    }

    public void setDivinePlea(float value) {
        divinePlea = value;
    }

    private float boLUp;

    public float getBoLUp() {
        return boLUp;
    }

    public void setBoLUp(float value) {
        boLUp = value;
    }

    private float holyShock;

    public float getHolyShock() {
        return holyShock;
    }

    public void setHolyShock(float value) {
        holyShock = value;
    }

    private float burstScale;

    public float getBurstScale() {
        return burstScale;
    }

    public void setBurstScale(float value) {
        burstScale = value;
    }

    private float gHL_Targets;

    public float getGHL_Targets() {
        return gHL_Targets;
    }

    public void setGHL_Targets(float value) {
        gHL_Targets = value;
    }

    private boolean infusionOfLight;

    public boolean getInfusionOfLight() {
        return infusionOfLight;
    }

    public void setInfusionOfLight(boolean value) {
        infusionOfLight = value;
    }

    private float ioLHolyLight;

    public float getIoLHolyLight() {
        return ioLHolyLight;
    }

    public void setIoLHolyLight(float value) {
        ioLHolyLight = value;
    }

    private boolean jotP;

    public boolean getJotP() {
        return jotP;
    }

    public void setJotP(boolean value) {
        jotP = value;
    }

    private boolean judgement;

    public boolean getJudgement() {
        return judgement;
    }

    public void setJudgement(boolean value) {
        judgement = value;
    }

    private boolean loHSelf;

    public boolean getLoHSelf() {
        return loHSelf;
    }

    public void setLoHSelf(boolean value) {
        loHSelf = value;
    }

    private float sSUptime;

    public float getSSUptime() {
        return sSUptime;
    }

    public void setSSUptime(float value) {
        sSUptime = value;
    }

    private float folOnTank;

    public float getFoLOnTank() {
        return folOnTank;
    }

    public void setFoLOnTank(float value) {
        folOnTank = value;
    }

    private boolean spiritIrrelevant;

    public boolean getSpiritIrrelevant() {
        return spiritIrrelevant;
    }

    public void setSpiritIrrelevant(boolean value) {
        spiritIrrelevant = value;
    }

    private boolean hitIrrelevant;

    public boolean getHitIrrelevant() {
        return hitIrrelevant;
    }

    public void setHitIrrelevant(boolean value) {
        hitIrrelevant = value;
    }

    private Character character;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    // C# CalculationOptionsHealadin.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsHealadin xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<Length>").append(NetFormat.floatR(length)).append("</Length>");
        sb.append("<Activity>").append(NetFormat.floatR(activity)).append("</Activity>");
        sb.append("<Replenishment>").append(NetFormat.floatR(replenishment)).append("</Replenishment>");
        sb.append("<DivinePlea>").append(NetFormat.floatR(divinePlea)).append("</DivinePlea>");
        sb.append("<BoLUp>").append(NetFormat.floatR(boLUp)).append("</BoLUp>");
        sb.append("<HolyShock>").append(NetFormat.floatR(holyShock)).append("</HolyShock>");
        sb.append("<BurstScale>").append(NetFormat.floatR(burstScale)).append("</BurstScale>");
        sb.append("<GHL_Targets>").append(NetFormat.floatR(gHL_Targets)).append("</GHL_Targets>");
        sb.append("<InfusionOfLight>").append(infusionOfLight ? "true" : "false").append("</InfusionOfLight>");
        sb.append("<IoLHolyLight>").append(NetFormat.floatR(ioLHolyLight)).append("</IoLHolyLight>");
        sb.append("<JotP>").append(jotP ? "true" : "false").append("</JotP>");
        sb.append("<Judgement>").append(judgement ? "true" : "false").append("</Judgement>");
        sb.append("<LoHSelf>").append(loHSelf ? "true" : "false").append("</LoHSelf>");
        sb.append("<SSUptime>").append(NetFormat.floatR(sSUptime)).append("</SSUptime>");
        sb.append("<FoLOnTank>").append(NetFormat.floatR(folOnTank)).append("</FoLOnTank>");
        sb.append("<SpiritIrrelevant>").append(spiritIrrelevant ? "true" : "false").append("</SpiritIrrelevant>");
        sb.append("<HitIrrelevant>").append(hitIrrelevant ? "true" : "false").append("</HitIrrelevant>");
        sb.append("</CalculationOptionsHealadin>");
        return sb.toString();
    }
}
