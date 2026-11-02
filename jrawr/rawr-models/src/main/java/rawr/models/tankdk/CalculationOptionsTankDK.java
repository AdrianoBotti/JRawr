// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/CalculationOptionsTankDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port) and the rotation
// convenience properties (CurRotationDuration, IcyTouch, ...) are omitted with it; the rotated
// members are reached through mRotation. getXml writes the XmlSerializer element order by hand
// (same approach as CalculationOptionsProtPaladin). The serialized m_Rotation and hCurrentBoss
// subtrees are omitted: the C# lazy Rotation rebuild discards a deserialized m_Rotation on first
// access (its tTalents is null) and hCurrentBoss is only read when bExperimental is set.
package rawr.models.tankdk;

import rawr.base.bosses.BossHandler;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.bosses.LordMarrowgar;

public class CalculationOptionsTankDK implements ICalculationOptionBase {
    private int targetLevel = 83;

    public int getTargetLevel() {
        if (targetLevel < 80 || targetLevel > 83) {
            targetLevel = 83;
        }
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private float threatWeight = 1;

    public float getThreatWeight() {
        if (threatWeight < 0) {
            threatWeight = 1f;
        }
        return threatWeight;
    }

    public void setThreatWeight(float value) {
        threatWeight = value;
    }

    private float survivalWeight = 1;

    public float getSurvivalWeight() {
        if (survivalWeight < 0) {
            survivalWeight = 1f;
        }
        return survivalWeight;
    }

    public void setSurvivalWeight(float value) {
        survivalWeight = value;
    }

    private float mitigationWeight = 4;

    public float getMitigationWeight() {
        if (mitigationWeight < 0) {
            mitigationWeight = 1f;
        }
        return mitigationWeight;
    }

    public void setMitigationWeight(float value) {
        mitigationWeight = value;
    }

    // Physical Damage
    private int incomingDamage = 60000;

    public int getIncomingDamage() {
        if (incomingDamage <= 0) {
            incomingDamage = 10000;
        }
        return incomingDamage;
    }

    public void setIncomingDamage(int value) {
        incomingDamage = value;
    }

    private float bossAttackSpeed = 2f;

    public float getBossAttackSpeed() {
        if (bossAttackSpeed < 0f) {
            bossAttackSpeed = 2f;
        }
        return bossAttackSpeed;
    }

    public void setBossAttackSpeed(float value) {
        bossAttackSpeed = value;
    }

    // Magic Damage
    private int incomingMagicDamage = 15000;

    public int getIncomingMagicDamage() {
        if (incomingMagicDamage <= 0) {
            incomingMagicDamage = 0;
        }
        return incomingMagicDamage;
    }

    public void setIncomingMagicDamage(int value) {
        incomingMagicDamage = value;
    }

    private float incomingFromMagicFrequency = 20;

    public float getIncomingFromMagicFrequency() {
        if (incomingFromMagicFrequency < 0) {
            incomingFromMagicFrequency = .0f;
        }
        return incomingFromMagicFrequency;
    }

    public void setIncomingFromMagicFrequency(float value) {
        incomingFromMagicFrequency = value;
    }

    // Bleed Damage
    private int incomingBleedDamage = 4000;

    public int getIncomingBleedDamage() {
        if (incomingBleedDamage <= 0) {
            incomingBleedDamage = 0;
        }
        return incomingBleedDamage;
    }

    public void setIncomingBleedDamage(int value) {
        incomingBleedDamage = value;
    }

    private float incomingBleedTickFrequency = 2;

    public float getBleedTickFrequency() {
        if (incomingBleedTickFrequency < 0) {
            incomingBleedTickFrequency = .0f;
        }
        return incomingBleedTickFrequency;
    }

    public void setBleedTickFrequency(float value) {
        incomingBleedTickFrequency = value;
    }

    private float bossArmor = StatConversion.NPC_ARMOR[83 - 80];

    public float getBossArmor() {
        if (bossArmor < 0) {
            bossArmor = StatConversion.NPC_ARMOR[83 - 80];
        }
        return bossArmor;
    }

    public void setBossArmor(float value) {
        bossArmor = value;
    }

    public boolean Bloodlust = false;

    public boolean getBloodlust() {
        return Bloodlust;
    }

    public void setBloodlust(boolean value) {
        Bloodlust = value;
    }

    private float fightLength = 10;

    public float getFightLength() {
        if (fightLength < 1f) {
            fightLength = 10f;
        }
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    private int uNumberTargets = 1;

    public int getUNumberTargets() {
        if (uNumberTargets < 1) {
            uNumberTargets = 1;
        }
        return uNumberTargets;
    }

    public void setUNumberTargets(int value) {
        uNumberTargets = value;
    }

    private CalculationType cType = CalculationType.SMT;

    public CalculationType getCType() {
        if (cType != CalculationType.Burst && cType != CalculationType.SMT) {
            cType = CalculationType.SMT;
        }
        return cType;
    }

    public void setCType(CalculationType value) {
        cType = value;
    }

    private boolean bExperimental = false;

    public boolean getBExperimental() {
        return bExperimental;
    }

    public void setBExperimental(boolean value) {
        bExperimental = value;
    }

    private boolean additiveMitigation = false;

    public boolean getAdditiveMitigation() {
        return additiveMitigation;
    }

    public void setAdditiveMitigation(boolean value) {
        additiveMitigation = value;
    }

    private boolean bParryHaste = true;

    public boolean getBParryHaste() {
        return bParryHaste;
    }

    public void setBParryHaste(boolean value) {
        bParryHaste = value;
    }

    private boolean bUseOnUseAbilities = true;

    public boolean getBUseOnUseAbilities() {
        return bUseOnUseAbilities;
    }

    public void setBUseOnUseAbilities(boolean value) {
        bUseOnUseAbilities = value;
    }

    private Rotation mRotation;

    // C# CalculationOptionsTankDK.cs:252-264 lazy m_Rotation property.
    public Rotation getMRotation() {
        if (null == mRotation || null == mRotation.tTalents || talents != mRotation.tTalents) {
            mRotation = new Rotation(talents);
        }
        return mRotation;
    }

    public void setMRotation(Rotation value) {
        mRotation = value;
    }

    private BossHandler hCurrentBoss = new LordMarrowgar().get(BossHandler.Versions.V_10N.value());

    public BossHandler getHCurrentBoss() {
        return hCurrentBoss;
    }

    public void setHCurrentBoss(BossHandler value) {
        hCurrentBoss = value;
    }

    public DeathKnightTalents talents;

    public DeathKnightTalents getTalents() {
        return talents;
    }

    public void setTalents(DeathKnightTalents value) {
        talents = value;
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsTankDK xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<Bloodlust>").append(Bloodlust ? "true" : "false").append("</Bloodlust>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<ThreatWeight>").append(NetFormat.floatR(threatWeight)).append("</ThreatWeight>");
        sb.append("<SurvivalWeight>").append(NetFormat.floatR(survivalWeight)).append("</SurvivalWeight>");
        sb.append("<MitigationWeight>").append(NetFormat.floatR(mitigationWeight)).append("</MitigationWeight>");
        sb.append("<IncomingDamage>").append(incomingDamage).append("</IncomingDamage>");
        sb.append("<BossAttackSpeed>").append(NetFormat.floatR(bossAttackSpeed)).append("</BossAttackSpeed>");
        sb.append("<IncomingMagicDamage>").append(incomingMagicDamage).append("</IncomingMagicDamage>");
        sb.append("<IncomingFromMagicFrequency>").append(NetFormat.floatR(incomingFromMagicFrequency)).append("</IncomingFromMagicFrequency>");
        sb.append("<IncomingBleedDamage>").append(incomingBleedDamage).append("</IncomingBleedDamage>");
        sb.append("<BleedTickFrequency>").append(NetFormat.floatR(incomingBleedTickFrequency)).append("</BleedTickFrequency>");
        sb.append("<BossArmor>").append(NetFormat.floatR(bossArmor)).append("</BossArmor>");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<uNumberTargets>").append(uNumberTargets).append("</uNumberTargets>");
        sb.append("<cType>").append(cType.name()).append("</cType>");
        sb.append("<bExperimental>").append(bExperimental ? "true" : "false").append("</bExperimental>");
        sb.append("<AdditiveMitigation>").append(additiveMitigation ? "true" : "false").append("</AdditiveMitigation>");
        sb.append("<bParryHaste>").append(bParryHaste ? "true" : "false").append("</bParryHaste>");
        sb.append("<bUseOnUseAbilities>").append(bUseOnUseAbilities ? "true" : "false").append("</bUseOnUseAbilities>");
        sb.append("</CalculationOptionsTankDK>");
        return sb.toString();
    }
}
