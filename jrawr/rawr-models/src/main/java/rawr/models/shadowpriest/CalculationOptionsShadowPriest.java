// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ShadowPriest/CalculationOptionsShadowPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsHealPriest).
package rawr.models.shadowpriest;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

import java.util.List;

public class CalculationOptionsShadowPriest implements ICalculationOptionBase {
    public CalculationOptionsShadowPriest() {
        targetLevel = 3;
        fightLength = 6f;
        moveFrequency = 60f;
        moveDuration = 5f;
        fsrRatio = 100f;
        delay = 350f;
        shadowfiend = 100f;
        replenishment = 100f;
        joW = 100f;
        survivability = 2f;
        ptr = false;
        spellPriority = null;
        manaPot = 4;
    }

    private int targetLevel;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private float fightLength;

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    private float moveFrequency;

    public float getMoveFrequency() {
        return moveFrequency;
    }

    public void setMoveFrequency(float value) {
        moveFrequency = value;
    }

    private float moveDuration;

    public float getMoveDuration() {
        return moveDuration;
    }

    public void setMoveDuration(float value) {
        moveDuration = value;
    }

    private float fsrRatio;

    public float getFSRRatio() {
        return fsrRatio;
    }

    public void setFSRRatio(float value) {
        fsrRatio = value;
    }

    private float delay;

    public float getDelay() {
        return delay;
    }

    public void setDelay(float value) {
        delay = value;
    }

    private float shadowfiend;

    public float getShadowfiend() {
        return shadowfiend;
    }

    public void setShadowfiend(float value) {
        shadowfiend = value;
    }

    private float replenishment;

    public float getReplenishment() {
        return replenishment;
    }

    public void setReplenishment(float value) {
        replenishment = value;
    }

    private float joW;

    public float getJoW() {
        return joW;
    }

    public void setJoW(float value) {
        joW = value;
    }

    private float survivability;

    public float getSurvivability() {
        return survivability;
    }

    public void setSurvivability(float value) {
        survivability = value;
    }

    private boolean ptr;

    public boolean getPTR() {
        return ptr;
    }

    public void setPTR(boolean value) {
        ptr = value;
    }

    private List<String> spellPriority;

    public List<String> getSpellPriority() {
        return spellPriority;
    }

    public void setSpellPriority(List<String> value) {
        spellPriority = value;
    }

    // Obsolete, to be Removed (C# CalculationOptionsShadowPriest.cs:55).
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

    // C# CalculationOptionsShadowPriest.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsShadowPriest xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<MoveFrequency>").append(NetFormat.floatR(moveFrequency)).append("</MoveFrequency>");
        sb.append("<MoveDuration>").append(NetFormat.floatR(moveDuration)).append("</MoveDuration>");
        sb.append("<FSRRatio>").append(NetFormat.floatR(fsrRatio)).append("</FSRRatio>");
        sb.append("<Delay>").append(NetFormat.floatR(delay)).append("</Delay>");
        sb.append("<Shadowfiend>").append(NetFormat.floatR(shadowfiend)).append("</Shadowfiend>");
        sb.append("<Replenishment>").append(NetFormat.floatR(replenishment)).append("</Replenishment>");
        sb.append("<JoW>").append(NetFormat.floatR(joW)).append("</JoW>");
        sb.append("<Survivability>").append(NetFormat.floatR(survivability)).append("</Survivability>");
        sb.append("<PTR>").append(ptr ? "true" : "false").append("</PTR>");
        if (spellPriority != null) {
            sb.append("<SpellPriority>");
            for (String spell : spellPriority) {
                sb.append("<string>").append(spell).append("</string>");
            }
            sb.append("</SpellPriority>");
        }
        sb.append("<ManaPot>").append(manaPot).append("</ManaPot>");
        sb.append("</CalculationOptionsShadowPriest>");
        return sb.toString();
    }
}
