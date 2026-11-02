// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/CalculationOptionsDPSDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port). getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsTankDK); the lazy
// `rotation` getter and the member-copying setter keep the C# semantics.
package rawr.models.dpsdk;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

public class CalculationOptionsDPSDK implements ICalculationOptionBase {
    public enum Presence {
        None(0),
        Blood(1),
        Unholy(2),
        Frost(3);

        private final int value;

        Presence(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private float ghoulUptime = 1f;

    public float getGhoulUptime() {
        return ghoulUptime;
    }

    public void setGhoulUptime(float value) {
        ghoulUptime = value;
    }

    private Presence presence = Presence.None;

    public Presence getCurrentPresence() {
        return presence;
    }

    public void setCurrentPresence(Presence value) {
        presence = value;
    }

    private float kmProcUsage = 1f;

    public float getKMProcUsage() {
        return kmProcUsage;
    }

    public void setKMProcUsage(float value) {
        kmProcUsage = value;
    }

    private float bloodwormsUptime = 0.25f;

    public float getBloodwormsUptime() {
        return bloodwormsUptime;
    }

    public void setBloodwormsUptime(float value) {
        bloodwormsUptime = value;
    }

    private boolean ghoul = true;

    public boolean getGhoul() {
        return ghoul;
    }

    public void setGhoul(boolean value) {
        ghoul = value;
    }

    private boolean enforceMetagemRequirements = false;

    public boolean getEnforceMetagemRequirements() {
        return enforceMetagemRequirements;
    }

    public void setEnforceMetagemRequirements(boolean value) {
        enforceMetagemRequirements = value;
    }

    private float fightLength = 6;

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    private int targetLevel = 83;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private int bossArmor = (int) StatConversion.NPC_ARMOR[83 - 80];

    public int getBossArmor() {
        return bossArmor;
    }

    public void setBossArmor(int value) {
        bossArmor = value;
    }

    private boolean getRefreshForReferenceCalcs = true;

    public boolean getGetRefreshForReferenceCalcs() {
        return getRefreshForReferenceCalcs;
    }

    public void setGetRefreshForReferenceCalcs(boolean value) {
        getRefreshForReferenceCalcs = value;
    }

    private boolean getRefreshForDisplayCalcs = true;

    public boolean getGetRefreshForDisplayCalcs() {
        return getRefreshForDisplayCalcs;
    }

    public void setGetRefreshForDisplayCalcs(boolean value) {
        getRefreshForDisplayCalcs = value;
    }

    private boolean getRefreshForSignificantChange = false;

    public boolean getGetRefreshForSignificantChange() {
        return getRefreshForSignificantChange;
    }

    public void setGetRefreshForSignificantChange(boolean value) {
        getRefreshForSignificantChange = value;
    }

    private boolean m_bExperimental = false;

    public boolean getM_bExperimental() {
        return m_bExperimental;
    }

    public void setM_bExperimental(boolean value) {
        m_bExperimental = value;
    }

    private Rotation rotation = null;

    public Rotation getRotation() {
        if (rotation == null) rotation = new Rotation();
        return rotation;
    }

    public void setRotation(Rotation value) {
        if (rotation == null) rotation = new Rotation();
        rotation.setManagedRP(value.getManagedRP());
        rotation.setAvgDiseaseMult(value.getAvgDiseaseMult());
        rotation.setBloodPlague(value.getBloodPlague());
        rotation.setBloodStrike(value.getBloodStrike());
        rotation.setCurRotationDuration(value.getCurRotationDuration());
        rotation.curRotationType = value.curRotationType;
        rotation.setDancingRuneWeapon(value.getDancingRuneWeapon());
        rotation.setDeathCoil(value.getDeathCoil());
        rotation.setDeathStrike(value.getDeathStrike());
        rotation.setDiseaseUptime(value.getDiseaseUptime());
        rotation.setFrostFever(value.getFrostFever());
        rotation.setFrostStrike(value.getFrostStrike());
        rotation.setGargoyleDuration(value.getGargoyleDuration());
        rotation.setGcdTime(value.getGcdTime());
        rotation.setGhoulFrenzy(value.getGhoulFrenzy());
        rotation.setHeartStrike(value.getHeartStrike());
        rotation.setHorn(value.getHorn());
        rotation.setHowlingBlast(value.getHowlingBlast());
        rotation.setIcyTouch(value.getIcyTouch());
        rotation.setNumDisease(value.getNumDisease());
        rotation.setObliterate(value.getObliterate());
        rotation.setPestilence(value.getPestilence());
        rotation.setPlagueStrike(value.getPlagueStrike());
        rotation.setPresence(value.getPresence());
        rotation.setPTRCalcs(value.getPTRCalcs());
        rotation.setRP(value.getRP());
        rotation.setScourgeStrike(value.getScourgeStrike());
    }

    private boolean talentsSaved = false;

    public boolean getTalentsSaved() {
        return talentsSaved;
    }

    public void setTalentsSaved(boolean value) {
        talentsSaved = value;
    }

    private double weightScale = .01;

    public double getWeightScale() {
        return weightScale;
    }

    public void setWeightScale(double value) {
        weightScale = value;
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsDPSDK xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<GhoulUptime>").append(NetFormat.floatR(ghoulUptime)).append("</GhoulUptime>");
        sb.append("<CurrentPresence>").append(presence.name()).append("</CurrentPresence>");
        sb.append("<KMProcUsage>").append(NetFormat.floatR(kmProcUsage)).append("</KMProcUsage>");
        sb.append("<BloodwormsUptime>").append(NetFormat.floatR(bloodwormsUptime)).append("</BloodwormsUptime>");
        sb.append("<Ghoul>").append(ghoul ? "true" : "false").append("</Ghoul>");
        sb.append("<EnforceMetagemRequirements>").append(enforceMetagemRequirements ? "true" : "false").append("</EnforceMetagemRequirements>");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<BossArmor>").append(bossArmor).append("</BossArmor>");
        sb.append("<GetRefreshForReferenceCalcs>").append(getRefreshForReferenceCalcs ? "true" : "false").append("</GetRefreshForReferenceCalcs>");
        sb.append("<GetRefreshForDisplayCalcs>").append(getRefreshForDisplayCalcs ? "true" : "false").append("</GetRefreshForDisplayCalcs>");
        sb.append("<GetRefreshForSignificantChange>").append(getRefreshForSignificantChange ? "true" : "false").append("</GetRefreshForSignificantChange>");
        sb.append("<m_bExperimental>").append(m_bExperimental ? "true" : "false").append("</m_bExperimental>");
        sb.append("<rotation>");
        Rotation r = getRotation();
        sb.append("<curRotationType>").append(r.curRotationType.name()).append("</curRotationType>");
        sb.append("<presence>").append(r.getPresence().name()).append("</presence>");
        sb.append("<PresenceByIndex>").append(r.getPresenceByIndex()).append("</PresenceByIndex>");
        sb.append("<CurRotationDuration>").append(NetFormat.doubleR(r.getCurRotationDuration())).append("</CurRotationDuration>");
        sb.append("<AvgDiseaseMult>").append(NetFormat.doubleR(r.getAvgDiseaseMult())).append("</AvgDiseaseMult>");
        sb.append("<NumDisease>").append(NetFormat.doubleR(r.getNumDisease())).append("</NumDisease>");
        sb.append("<DiseaseUptime>").append(NetFormat.doubleR(r.getDiseaseUptime())).append("</DiseaseUptime>");
        sb.append("<GargoyleDuration>").append(NetFormat.doubleR(r.getGargoyleDuration())).append("</GargoyleDuration>");
        sb.append("<DeathCoil>").append(NetFormat.doubleR(r.getDeathCoil())).append("</DeathCoil>");
        sb.append("<IcyTouch>").append(NetFormat.doubleR(r.getIcyTouch())).append("</IcyTouch>");
        sb.append("<PlagueStrike>").append(NetFormat.doubleR(r.getPlagueStrike())).append("</PlagueStrike>");
        sb.append("<FrostFever>").append(NetFormat.doubleR(r.getFrostFever())).append("</FrostFever>");
        sb.append("<BloodPlague>").append(NetFormat.doubleR(r.getBloodPlague())).append("</BloodPlague>");
        sb.append("<ScourgeStrike>").append(NetFormat.doubleR(r.getScourgeStrike())).append("</ScourgeStrike>");
        sb.append("<FrostStrike>").append(NetFormat.doubleR(r.getFrostStrike())).append("</FrostStrike>");
        sb.append("<HowlingBlast>").append(NetFormat.doubleR(r.getHowlingBlast())).append("</HowlingBlast>");
        sb.append("<Obliterate>").append(NetFormat.doubleR(r.getObliterate())).append("</Obliterate>");
        sb.append("<DeathStrike>").append(NetFormat.doubleR(r.getDeathStrike())).append("</DeathStrike>");
        sb.append("<BloodStrike>").append(NetFormat.doubleR(r.getBloodStrike())).append("</BloodStrike>");
        sb.append("<BloodBoil>").append(NetFormat.doubleR(r.getBloodBoil())).append("</BloodBoil>");
        sb.append("<KMFS>").append(NetFormat.doubleR(r.getKMFS())).append("</KMFS>");
        sb.append("<KMRime>").append(NetFormat.doubleR(r.getKMRime())).append("</KMRime>");
        sb.append("<FFTick>").append(NetFormat.doubleR(r.getFFTick())).append("</FFTick>");
        sb.append("<BPTick>").append(NetFormat.doubleR(r.getBPTick())).append("</BPTick>");
        sb.append("<HeartStrike>").append(NetFormat.doubleR(r.getHeartStrike())).append("</HeartStrike>");
        sb.append("<DancingRuneWeapon>").append(NetFormat.doubleR(r.getDancingRuneWeapon())).append("</DancingRuneWeapon>");
        sb.append("<Horn>").append(NetFormat.doubleR(r.getHorn())).append("</Horn>");
        sb.append("<GhoulFrenzy>").append(NetFormat.doubleR(r.getGhoulFrenzy())).append("</GhoulFrenzy>");
        sb.append("<Pestilence>").append(NetFormat.doubleR(r.getPestilence())).append("</Pestilence>");
        sb.append("<ManagedRP>").append(r.getManagedRP() ? "true" : "false").append("</ManagedRP>");
        sb.append("<PTRCalcs>").append(r.getPTRCalcs() ? "true" : "false").append("</PTRCalcs>");
        sb.append("<GCDTime>").append(NetFormat.doubleR(r.getGcdTime())).append("</GCDTime>");
        sb.append("<RP>").append(NetFormat.doubleR(r.getRP())).append("</RP>");
        sb.append("<Weight>").append(NetFormat.doubleR(r.getWeight())).append("</Weight>");
        sb.append("</rotation>");
        sb.append("<TalentsSaved>").append(talentsSaved ? "true" : "false").append("</TalentsSaved>");
        sb.append("<WeightScale>").append(NetFormat.doubleR(weightScale)).append("</WeightScale>");
        sb.append("</CalculationOptionsDPSDK>");
        return sb.toString();
    }
}
