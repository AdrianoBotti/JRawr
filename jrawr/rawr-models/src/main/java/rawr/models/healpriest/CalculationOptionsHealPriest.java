// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/CalculationOptionsHealPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsHealadin).
package rawr.models.healpriest;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

public class CalculationOptionsHealPriest implements ICalculationOptionBase {
    public CalculationOptionsHealPriest() {
        role = eRole.AUTO_Tank;
        rotation = 0;
        fsrRatio = 93f;
        fightLengthSeconds = 480f;
        serendipity = 75f;
        replenishment = 75f;
        shadowfiend = 100f;
        survivability = 2f;
        rapture = 25f;
        testOfFaith = 25f;
        modelProcs = true;
        manaPot = 4;
    }

    private eRole role;

    public eRole getRole() {
        return role;
    }

    public void setRole(eRole value) {
        role = value;
    }

    private int rotation;

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int value) {
        rotation = value;
    }

    private float fsrRatio;

    public float getFSRRatio() {
        return fsrRatio;
    }

    public void setFSRRatio(float value) {
        fsrRatio = value;
    }

    private float fightLengthSeconds;

    public float getFightLengthSeconds() {
        return fightLengthSeconds;
    }

    public void setFightLengthSeconds(float value) {
        fightLengthSeconds = value;
    }

    private float serendipity;

    public float getSerendipity() {
        return serendipity;
    }

    public void setSerendipity(float value) {
        serendipity = value;
    }

    private float replenishment;

    public float getReplenishment() {
        return replenishment;
    }

    public void setReplenishment(float value) {
        replenishment = value;
    }

    private float shadowfiend;

    public float getShadowfiend() {
        return shadowfiend;
    }

    public void setShadowfiend(float value) {
        shadowfiend = value;
    }

    private float survivability;

    public float getSurvivability() {
        return survivability;
    }

    public void setSurvivability(float value) {
        survivability = value;
    }

    private float rapture;

    public float getRapture() {
        return rapture;
    }

    public void setRapture(float value) {
        rapture = value;
    }

    private float testOfFaith;

    public float getTestOfFaith() {
        return testOfFaith;
    }

    public void setTestOfFaith(float value) {
        testOfFaith = value;
    }

    private boolean modelProcs;

    public boolean getModelProcs() {
        return modelProcs;
    }

    public void setModelProcs(boolean value) {
        modelProcs = value;
    }

    private int flashHealCast;

    public int getFlashHealCast() {
        return flashHealCast;
    }

    public void setFlashHealCast(int value) {
        flashHealCast = value;
    }

    private int bindingHealCast;

    public int getBindingHealCast() {
        return bindingHealCast;
    }

    public void setBindingHealCast(int value) {
        bindingHealCast = value;
    }

    private int greaterHealCast;

    public int getGreaterHealCast() {
        return greaterHealCast;
    }

    public void setGreaterHealCast(int value) {
        greaterHealCast = value;
    }

    private int penanceCast;

    public int getPenanceCast() {
        return penanceCast;
    }

    public void setPenanceCast(int value) {
        penanceCast = value;
    }

    private int renewCast;

    public int getRenewCast() {
        return renewCast;
    }

    public void setRenewCast(int value) {
        renewCast = value;
    }

    private int renewTicks;

    public int getRenewTicks() {
        return renewTicks;
    }

    public void setRenewTicks(int value) {
        renewTicks = value;
    }

    private int proMCast;

    public int getProMCast() {
        return proMCast;
    }

    public void setProMCast(int value) {
        proMCast = value;
    }

    private int proMTicks;

    public int getProMTicks() {
        return proMTicks;
    }

    public void setProMTicks(int value) {
        proMTicks = value;
    }

    private int poHCast;

    public int getPoHCast() {
        return poHCast;
    }

    public void setPoHCast(int value) {
        poHCast = value;
    }

    private int pwsCast;

    public int getPWSCast() {
        return pwsCast;
    }

    public void setPWSCast(int value) {
        pwsCast = value;
    }

    private int coHCast;

    public int getCoHCast() {
        return coHCast;
    }

    public void setCoHCast(int value) {
        coHCast = value;
    }

    private int holyNovaCast;

    public int getHolyNovaCast() {
        return holyNovaCast;
    }

    public void setHolyNovaCast(int value) {
        holyNovaCast = value;
    }

    private int divineHymnCast;

    public int getDivineHymnCast() {
        return divineHymnCast;
    }

    public void setDivineHymnCast(int value) {
        divineHymnCast = value;
    }

    private int dispelCast;

    public int getDispelCast() {
        return dispelCast;
    }

    public void setDispelCast(int value) {
        dispelCast = value;
    }

    private int mdCast;

    public int getMDCast() {
        return mdCast;
    }

    public void setMDCast(int value) {
        mdCast = value;
    }

    // Obsolete, to be Removed (C# CalculationOptionsHealPriest.cs:83-88).
    private static final int[] manaAmt = new int[] { 0, 1800, 2200, 2400, 4300 };
    private int manaPot;

    public int getManaPot() {
        return manaPot;
    }

    public void setManaPot(int value) {
        manaPot = value;
    }

    public int getManaAmt() {
        return manaAmt[manaPot];
    }

    // C# CalculationOptionsHealPriest.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsHealPriest xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<Role>").append(role.name()).append("</Role>");
        sb.append("<Rotation>").append(rotation).append("</Rotation>");
        sb.append("<FSRRatio>").append(NetFormat.floatR(fsrRatio)).append("</FSRRatio>");
        sb.append("<FightLengthSeconds>").append(NetFormat.floatR(fightLengthSeconds)).append("</FightLengthSeconds>");
        sb.append("<Serendipity>").append(NetFormat.floatR(serendipity)).append("</Serendipity>");
        sb.append("<Replenishment>").append(NetFormat.floatR(replenishment)).append("</Replenishment>");
        sb.append("<Shadowfiend>").append(NetFormat.floatR(shadowfiend)).append("</Shadowfiend>");
        sb.append("<Survivability>").append(NetFormat.floatR(survivability)).append("</Survivability>");
        sb.append("<Rapture>").append(NetFormat.floatR(rapture)).append("</Rapture>");
        sb.append("<TestOfFaith>").append(NetFormat.floatR(testOfFaith)).append("</TestOfFaith>");
        sb.append("<ModelProcs>").append(modelProcs ? "true" : "false").append("</ModelProcs>");
        sb.append("<FlashHealCast>").append(flashHealCast).append("</FlashHealCast>");
        sb.append("<BindingHealCast>").append(bindingHealCast).append("</BindingHealCast>");
        sb.append("<GreaterHealCast>").append(greaterHealCast).append("</GreaterHealCast>");
        sb.append("<PenanceCast>").append(penanceCast).append("</PenanceCast>");
        sb.append("<RenewCast>").append(renewCast).append("</RenewCast>");
        sb.append("<RenewTicks>").append(renewTicks).append("</RenewTicks>");
        sb.append("<ProMCast>").append(proMCast).append("</ProMCast>");
        sb.append("<ProMTicks>").append(proMTicks).append("</ProMTicks>");
        sb.append("<PoHCast>").append(poHCast).append("</PoHCast>");
        sb.append("<PWSCast>").append(pwsCast).append("</PWSCast>");
        sb.append("<CoHCast>").append(coHCast).append("</CoHCast>");
        sb.append("<HolyNovaCast>").append(holyNovaCast).append("</HolyNovaCast>");
        sb.append("<DivineHymnCast>").append(divineHymnCast).append("</DivineHymnCast>");
        sb.append("<DispelCast>").append(dispelCast).append("</DispelCast>");
        sb.append("<MDCast>").append(mdCast).append("</MDCast>");
        sb.append("<ManaPot>").append(manaPot).append("</ManaPot>");
        sb.append("</CalculationOptionsHealPriest>");
        return sb.toString();
    }
}
