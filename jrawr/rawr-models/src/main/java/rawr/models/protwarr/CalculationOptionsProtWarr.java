// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CalculationOptionsProtWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsCat/Bear/Healadin).
package rawr.models.protwarr;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

public class CalculationOptionsProtWarr implements ICalculationOptionBase {
    private int targetLevel;
    private int targetArmor;
    private int bossAttackValue;
    private float bossAttackSpeed;
    private boolean useParryHaste;
    private float threatScale;
    private float mitigationScale;
    private int rankingMode;
    private boolean useVigilance;
    private int vigilanceValue;
    private float heroicStrikeFrequency;

    public CalculationOptionsProtWarr() {
        targetLevel = 83;
        targetArmor = (int) StatConversion.NPC_ARMOR[83 - 80];
        bossAttackValue = 80000;
        bossAttackSpeed = 2.0f;
        useParryHaste = false;
        threatScale = 8.0f;
        mitigationScale = 0.125f;
        rankingMode = 1;
        useVigilance = true;
        vigilanceValue = 5000;
        heroicStrikeFrequency = 0.9f;
    }

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

    public int getBossAttackValue() {
        return bossAttackValue;
    }

    public void setBossAttackValue(int value) {
        bossAttackValue = value;
    }

    public float getBossAttackSpeed() {
        return bossAttackSpeed;
    }

    public void setBossAttackSpeed(float value) {
        bossAttackSpeed = value;
    }

    public boolean getUseParryHaste() {
        return useParryHaste;
    }

    public void setUseParryHaste(boolean value) {
        useParryHaste = value;
    }

    public float getThreatScale() {
        return threatScale;
    }

    public void setThreatScale(float value) {
        threatScale = value;
    }

    public float getMitigationScale() {
        return mitigationScale;
    }

    public void setMitigationScale(float value) {
        mitigationScale = value;
    }

    public int getRankingMode() {
        return rankingMode;
    }

    public void setRankingMode(int value) {
        rankingMode = value;
    }

    public boolean getUseVigilance() {
        return useVigilance;
    }

    public void setUseVigilance(boolean value) {
        useVigilance = value;
    }

    public int getVigilanceValue() {
        return vigilanceValue;
    }

    public void setVigilanceValue(int value) {
        vigilanceValue = value;
    }

    public float getHeroicStrikeFrequency() {
        return heroicStrikeFrequency;
    }

    public void setHeroicStrikeFrequency(float value) {
        heroicStrikeFrequency = value;
    }

    // C# CalculationOptionsProtWarr.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsProtWarr xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(targetArmor).append("</TargetArmor>");
        sb.append("<BossAttackValue>").append(bossAttackValue).append("</BossAttackValue>");
        sb.append("<BossAttackSpeed>").append(NetFormat.floatR(bossAttackSpeed)).append("</BossAttackSpeed>");
        sb.append("<UseParryHaste>").append(useParryHaste ? "true" : "false").append("</UseParryHaste>");
        sb.append("<ThreatScale>").append(NetFormat.floatR(threatScale)).append("</ThreatScale>");
        sb.append("<MitigationScale>").append(NetFormat.floatR(mitigationScale)).append("</MitigationScale>");
        sb.append("<RankingMode>").append(rankingMode).append("</RankingMode>");
        sb.append("<UseVigilance>").append(useVigilance ? "true" : "false").append("</UseVigilance>");
        sb.append("<VigilanceValue>").append(vigilanceValue).append("</VigilanceValue>");
        sb.append("<HeroicStrikeFrequency>").append(NetFormat.floatR(heroicStrikeFrequency)).append("</HeroicStrikeFrequency>");
        sb.append("</CalculationOptionsProtWarr>");
        return sb.toString();
    }
}
