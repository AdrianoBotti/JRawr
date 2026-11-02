// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CalculationOptionsProtPaladin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsProtWarr/Healadin).
package rawr.models.protpally;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.StatConversion;

public class CalculationOptionsProtPaladin implements ICalculationOptionBase {
    private int targetLevel = 83;
    private int targetArmor = (int) StatConversion.NPC_ARMOR[83 - 80];
    private int bossAttackValue = 80000;
    private float bossAttackSpeed = 2.0f;
    private int bossAttackValueMagic = 20000;
    private float bossAttackSpeedMagic = 1.0f;
    private boolean useParryHaste = false;
    private float threatScale = 10.0f;
    private float mitigationScale = 17000f;
    private int rankingMode = 1;
    private boolean useHolyShield = true;
    private String sealChoice = "Seal of Vengeance";
    private String targetType = "Unspecified";
    private String magicDamageType = "None";
    private String trinketOnUseHandling = "Ignore";
    private PaladinTalents talents = null;
    private boolean ptrMode = false;
    private int survivalSoftCap = 300000;

    public int getTargetLevel() { return targetLevel; }
    public void setTargetLevel(int value) { targetLevel = value; }

    public int getTargetArmor() { return targetArmor; }
    public void setTargetArmor(int value) { targetArmor = value; }

    public int getBossAttackValue() { return bossAttackValue; }
    public void setBossAttackValue(int value) { bossAttackValue = value; }

    public float getBossAttackSpeed() { return bossAttackSpeed; }
    public void setBossAttackSpeed(float value) { bossAttackSpeed = value; }

    public int getBossAttackValueMagic() { return bossAttackValueMagic; }
    public void setBossAttackValueMagic(int value) { bossAttackValueMagic = value; }

    public float getBossAttackSpeedMagic() { return bossAttackSpeedMagic; }
    public void setBossAttackSpeedMagic(float value) { bossAttackSpeedMagic = value; }

    public boolean getUseParryHaste() { return useParryHaste; }
    public void setUseParryHaste(boolean value) { useParryHaste = value; }

    public float getThreatScale() { return threatScale; }
    public void setThreatScale(float value) { threatScale = value; }

    public float getMitigationScale() { return mitigationScale; }
    public void setMitigationScale(float value) { mitigationScale = value; }

    public int getRankingMode() { return rankingMode; }
    public void setRankingMode(int value) { rankingMode = value; }

    public boolean getUseHolyShield() { return useHolyShield; }
    public void setUseHolyShield(boolean value) { useHolyShield = value; }

    public String getSealChoice() { return sealChoice; }
    public void setSealChoice(String value) { sealChoice = value; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }

    public String getMagicDamageType() { return magicDamageType; }
    public void setMagicDamageType(String value) { magicDamageType = value; }

    public String getTrinketOnUseHandling() { return trinketOnUseHandling; }
    public void setTrinketOnUseHandling(String value) { trinketOnUseHandling = value; }

    public PaladinTalents getTalents() { return talents; }
    public void setTalents(PaladinTalents value) { talents = value; }

    public boolean getPtrMode() { return ptrMode; }
    public void setPtrMode(boolean value) { ptrMode = value; }

    /** Alias matching the serialized element name {@code <PTRMode>} (deserialization shim). */
    public void setPTRMode(boolean value) { ptrMode = value; }

    public int getSurvivalSoftCap() { return survivalSoftCap; }
    public void setSurvivalSoftCap(int value) { survivalSoftCap = value; }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsProtPaladin xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(targetArmor).append("</TargetArmor>");
        sb.append("<BossAttackValue>").append(bossAttackValue).append("</BossAttackValue>");
        sb.append("<BossAttackSpeed>").append(NetFormat.floatR(bossAttackSpeed)).append("</BossAttackSpeed>");
        sb.append("<BossAttackValueMagic>").append(bossAttackValueMagic).append("</BossAttackValueMagic>");
        sb.append("<BossAttackSpeedMagic>").append(NetFormat.floatR(bossAttackSpeedMagic)).append("</BossAttackSpeedMagic>");
        sb.append("<UseParryHaste>").append(useParryHaste ? "true" : "false").append("</UseParryHaste>");
        sb.append("<ThreatScale>").append(NetFormat.floatR(threatScale)).append("</ThreatScale>");
        sb.append("<MitigationScale>").append(NetFormat.floatR(mitigationScale)).append("</MitigationScale>");
        sb.append("<RankingMode>").append(rankingMode).append("</RankingMode>");
        sb.append("<UseHolyShield>").append(useHolyShield ? "true" : "false").append("</UseHolyShield>");
        sb.append("<SealChoice>").append(sealChoice).append("</SealChoice>");
        sb.append("<TargetType>").append(targetType).append("</TargetType>");
        sb.append("<MagicDamageType>").append(magicDamageType).append("</MagicDamageType>");
        sb.append("<TrinketOnUseHandling>").append(trinketOnUseHandling).append("</TrinketOnUseHandling>");
        // XmlSerializer omits the null talents element (fixtures never carry one).
        sb.append("<PTRMode>").append(ptrMode ? "true" : "false").append("</PTRMode>");
        sb.append("<SurvivalSoftCap>").append(survivalSoftCap).append("</SurvivalSoftCap>");
        sb.append("</CalculationOptionsProtPaladin>");
        return sb.toString();
    }
}
