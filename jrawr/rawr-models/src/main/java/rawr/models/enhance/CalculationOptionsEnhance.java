// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/CalculationOptionsEnhance.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsRetribution).
package rawr.models.enhance;

import rawr.base.bosses.BossHandler;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.ItemDamageType;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.character.TargetGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationOptionsEnhance implements ICalculationOptionBase {

    // Boss parameters
    private BossHandler boss = new BossHandler();
    private String bossName = "Custom";
    private int targetLevel = 83;
    private float targetArmor = StatConversion.NPC_ARMOR[83 - 80];
    private boolean inBack = true;
    private int inBackPerc = 100;

    // General parameters
    private int averageLag = 250;
    private int reactionTime = 250;
    private String mainhandImbue = "Windfury";
    private String offhandImbue = "Flametongue";
    private String calculationToGraph = "DPS Rating";
    private float fightLength = 7.0f;
    private float fightLengthMultiplier = 5.0f;
    private float targetFireResistance = 0;
    private float targetNatureResistance = 0;
    private float minManaSR = 1250;
    private boolean multipleTargets = false;
    private int additionalTargets = 2;
    private float additionalTargetPercent = 0.25f;
    private boolean baseStatOption = true;
    private boolean useMana = false;
    private boolean showExportMessageBox = true;
    private boolean[] statsList = new boolean[] { true, true, true, true, true, true, true, true, true, true };
    private LinkedHashMap<EnhanceAbility, Priority> priorityList;
    // this can be read from multiple threads, make sure all modifications are only done from
    // when no calculations are going on so we can avoid locking
    // make sure it is updated whenever changes to _priorityList are made
    private List<Map.Entry<EnhanceAbility, Priority>> sortedList;

    public CalculationOptionsEnhance() {
        priorityList = setPriorityDefaults();
        sortPriorities();
    }

    public String getBossName() {
        return bossName;
    }

    public void setBossName(String value) {
        bossName = value;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    public float getTargetArmor() {
        return targetArmor;
    }

    public void setTargetArmor(float value) {
        targetArmor = value;
    }

    public boolean getInBack() {
        return inBack;
    }

    public void setInBack(boolean value) {
        inBack = value;
    }

    public int getInBackPerc() {
        return inBackPerc;
    }

    public void setInBackPerc(int value) {
        inBackPerc = value;
    }

    public int getAverageLag() {
        return averageLag;
    }

    public void setAverageLag(int value) {
        averageLag = value;
    }

    public int getReactionTime() {
        return reactionTime;
    }

    public void setReactionTime(int value) {
        reactionTime = value;
    }

    public float getMinManaSR() {
        return minManaSR;
    }

    public void setMinManaSR(float value) {
        minManaSR = value;
    }

    public String getMainhandImbue() {
        return mainhandImbue;
    }

    public void setMainhandImbue(String value) {
        mainhandImbue = value;
    }

    public String getOffhandImbue() {
        return offhandImbue;
    }

    public void setOffhandImbue(String value) {
        offhandImbue = value;
    }

    public String getCalculationToGraph() {
        return calculationToGraph;
    }

    public void setCalculationToGraph(String value) {
        calculationToGraph = value;
    }

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    public float getFightLengthMultiplier() {
        return fightLengthMultiplier;
    }

    public void setFightLengthMultiplier(float value) {
        fightLengthMultiplier = value;
    }

    public float getTargetFireResistance() {
        return targetFireResistance;
    }

    public void setTargetFireResistance(float value) {
        targetFireResistance = value;
    }

    public float getTargetNatureResistance() {
        return targetNatureResistance;
    }

    public void setTargetNatureResistance(float value) {
        targetNatureResistance = value;
    }

    public boolean getMagma() {
        return priorityInUse(EnhanceAbility.MagmaTotem);
    }

    public boolean getSearing() {
        return priorityInUse(EnhanceAbility.SearingTotem);
    }

    public boolean getFireElemental() {
        return priorityInUse(EnhanceAbility.FireElemental);
    }

    public boolean getBaseStatOption() {
        return baseStatOption;
    }

    public void setBaseStatOption(boolean value) {
        baseStatOption = value;
    }

    //public bool UseMana { get { return _useMana; } set { _useMana = value; OnPropertyChanged("UseMana"); } }
    public boolean getUseMana() {
        return useMana;
    }

    public void setUseMana(boolean value) {
        useMana = false;
    }

    public boolean getShowExportMessageBox() {
        return showExportMessageBox;
    }

    public void setShowExportMessageBox(boolean value) {
        showExportMessageBox = value;
    }

    public boolean getMultipleTargets() {
        return multipleTargets;
    }

    public void setMultipleTargets(boolean value) {
        multipleTargets = value;
    }

    public int getAdditionalTargets() {
        return additionalTargets;
    }

    public void setAdditionalTargets(int value) {
        additionalTargets = value;
    }

    public float getAdditionalTargetPercent() {
        return additionalTargetPercent;
    }

    public void setAdditionalTargetPercent(float value) {
        additionalTargetPercent = value;
    }

    public boolean[] getStatsList() {
        return statsList;
    }

    public void setStatsList(boolean[] value) {
        statsList = value;
    }

    public LinkedHashMap<EnhanceAbility, Priority> getPriorityList() {
        return priorityList;
    }

    public void setPriorityList(LinkedHashMap<EnhanceAbility, Priority> value) {
        priorityList = value;
        sortPriorities();
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsEnhance xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<BossName>").append(bossName).append("</BossName>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(NetFormat.floatR(targetArmor)).append("</TargetArmor>");
        sb.append("<InBack>").append(inBack ? "true" : "false").append("</InBack>");
        sb.append("<InBackPerc>").append(inBackPerc).append("</InBackPerc>");
        sb.append("<AverageLag>").append(averageLag).append("</AverageLag>");
        sb.append("<ReactionTime>").append(reactionTime).append("</ReactionTime>");
        sb.append("<MinManaSR>").append(NetFormat.floatR(minManaSR)).append("</MinManaSR>");
        sb.append("<MainhandImbue>").append(mainhandImbue).append("</MainhandImbue>");
        sb.append("<OffhandImbue>").append(offhandImbue).append("</OffhandImbue>");
        sb.append("<CalculationToGraph>").append(calculationToGraph).append("</CalculationToGraph>");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<FightLengthMultiplier>").append(NetFormat.floatR(fightLengthMultiplier)).append("</FightLengthMultiplier>");
        sb.append("<TargetFireResistance>").append(NetFormat.floatR(targetFireResistance)).append("</TargetFireResistance>");
        sb.append("<TargetNatureResistance>").append(NetFormat.floatR(targetNatureResistance)).append("</TargetNatureResistance>");
        sb.append("<BaseStatOption>").append(baseStatOption ? "true" : "false").append("</BaseStatOption>");
        sb.append("<UseMana>").append(useMana ? "true" : "false").append("</UseMana>");
        sb.append("<ShowExportMessageBox>").append(showExportMessageBox ? "true" : "false").append("</ShowExportMessageBox>");
        sb.append("<MultipleTargets>").append(multipleTargets ? "true" : "false").append("</MultipleTargets>");
        sb.append("<AdditionalTargets>").append(additionalTargets).append("</AdditionalTargets>");
        sb.append("<AdditionalTargetPercent>").append(NetFormat.floatR(additionalTargetPercent)).append("</AdditionalTargetPercent>");
        sb.append("<StatsList>");
        for (boolean b : statsList)
            sb.append("<boolean>").append(b ? "true" : "false").append("</boolean>");
        sb.append("</StatsList>");
        sb.append("<PriorityList>");
        for (Map.Entry<EnhanceAbility, Priority> kvp : priorityList.entrySet()) {
            sb.append("<item><key><EnhanceAbility>").append(kvp.getKey().name()).append("</EnhanceAbility></key>");
            sb.append("<value><Priority>");
            sb.append("<PriorityName>").append(kvp.getValue().getPriorityName()).append("</PriorityName>");
            sb.append("<AbilityType>").append(kvp.getValue().getAbilityType().name()).append("</AbilityType>");
            sb.append("<Description>").append(kvp.getValue().getDescription()).append("</Description>");
            sb.append("<PriorityValue>").append(kvp.getValue().getPriorityValue()).append("</PriorityValue>");
            sb.append("<Checked>").append(kvp.getValue().getChecked() ? "true" : "false").append("</Checked>");
            sb.append("</Priority></value></item>");
        }
        sb.append("</PriorityList>");
        sb.append("</CalculationOptionsEnhance>");
        return sb.toString();
    }

    public void setBoss(BossHandler boss) {
        this.boss = boss;
        bossName = boss.getName();
        targetLevel = boss.getLevel();
        targetArmor = (int) boss.getArmor();
        fightLength = boss.getBerserkTimer() / 60f;
        inBack = ((inBackPerc = (int) (boss.getInBackPercMelee() * 100f)) != 0);
        targetFireResistance = (float) boss.getResistance(ItemDamageType.Fire);
        targetNatureResistance = (float) boss.getResistance(ItemDamageType.Nature);
        //_additionalTargets = (int)boss.MaxNumTargets - 1;
        {
            float value = 0;
            for (TargetGroup tg : boss.targets) {
                if (tg.chance <= 0 || tg.frequency <= 0 || tg.duration <= 0) continue;
                value += tg.frequency / boss.getBerserkTimer() * tg.duration / 1000f;
            }
            float num = value / boss.targets.size();
            additionalTargets = (int) num - 1;
        }
        //_additionalTargetPercent = (float)boss.MultiTargsPerc;
        {
            float time = 0;
            for (TargetGroup tg : boss.targets) {
                if (tg.chance <= 0 || tg.frequency <= 0 || tg.duration <= 0) continue;
                time += tg.frequency / boss.getBerserkTimer() * tg.duration / 1000f;
            }
            float perc = time / boss.getBerserkTimer();
            additionalTargetPercent = Math.max(0f, Math.min(1f, perc));
        }
        multipleTargets = additionalTargets > 0 && additionalTargetPercent > 0;
    }

    public boolean priorityInUse(EnhanceAbility abilityType) {
        Priority p = priorityList.get(abilityType);
        if (p != null) {
            return p.getChecked();
        }
        return false;
    }

    public Priority getAbilityPriority(EnhanceAbility abilityType) {
        return priorityList.get(abilityType);
    }

    public int getAbilityPriorityValue(EnhanceAbility abilityType) {
        int priority = 0;
        for (Map.Entry<EnhanceAbility, Priority> kvp : sortedList) {
            Priority p = kvp.getValue();
            if (p.getChecked() && p.getPriorityValue() > 0)
                priority++;
            if (p.getAbilityType() == abilityType)
                return p.getChecked() ? priority : 0;
        }
        return -1;
    }

    public void setAbilityPriority(EnhanceAbility abilityType, Priority priority) {
        Priority value = getAbilityPriority(abilityType);
        if (value != null && value.getAbilityType() != EnhanceAbility.None)
            priorityList.remove(abilityType);
        if (priority.getPriorityValue() > 0)
            priorityList.put(abilityType, priority);
        // update the sorted list
        sortPriorities();
    }

    public int activePriorities() {
        int activePriorities = 0;
        for (Priority p : priorityList.values()) {
            if (p.getChecked() && p.getPriorityValue() > 0) activePriorities++;
        }
        return activePriorities;
    }

    // make sure not to call this from calculations, this should be setup before calculations start
    public void sortPriorities() {
        sortedList = new ArrayList<Map.Entry<EnhanceAbility, Priority>>(priorityList.entrySet());
        sortedList.sort((firstPair, nextPair) -> Integer.compare(firstPair.getValue().getPriorityValue(), nextPair.getValue().getPriorityValue()));
    }

    public static LinkedHashMap<EnhanceAbility, Priority> setPriorityDefaults() {
        LinkedHashMap<EnhanceAbility, Priority> priorityList = new LinkedHashMap<EnhanceAbility, Priority>();
        if (priorityList.isEmpty()) {
            int priority = 0;
            priorityList.put(EnhanceAbility.ShamanisticRage, new Priority("Shamanistic Rage", EnhanceAbility.ShamanisticRage, "Use Shamanistic Rage", true, ++priority, "SR"));
            priorityList.put(EnhanceAbility.FeralSpirits, new Priority("Feral Spirits", EnhanceAbility.FeralSpirits, "Use Feral Sprirts", true, ++priority, "SW"));
            priorityList.put(EnhanceAbility.LightningBolt, new Priority("Lightning Bolt on 5 stacks of MW", EnhanceAbility.LightningBolt, "Use Lightning Bolt when you have 5 stacks of Maelstrom Weapon", true, ++priority, "MW5_LB"));
            priorityList.put(EnhanceAbility.FireElemental, new Priority("Fire Elemental", EnhanceAbility.FireElemental, "Drop Fire Elemental Totem", false, ++priority, "FE"));
            priorityList.put(EnhanceAbility.MagmaTotem, new Priority("Magma Totem", EnhanceAbility.MagmaTotem, "Refresh Magma Totem", true, ++priority, "MT"));
            priorityList.put(EnhanceAbility.FlameShock, new Priority("Flame Shock", EnhanceAbility.FlameShock, "Use Flame Shock if no Flame Shock debuff on target", true, ++priority, "FS"));
            priorityList.put(EnhanceAbility.StormStrike, new Priority("Stormstrike", EnhanceAbility.StormStrike, "Use Stormstrike", true, ++priority, "SS"));
            priorityList.put(EnhanceAbility.EarthShock, new Priority("Earth Shock", EnhanceAbility.EarthShock, "Use Earth Shock", true, ++priority, "ES"));
            priorityList.put(EnhanceAbility.LavaLash, new Priority("Lava Lash", EnhanceAbility.LavaLash, "Use Lava Lash", true, ++priority, "LL"));
            priorityList.put(EnhanceAbility.FireNova, new Priority("Fire Nova", EnhanceAbility.FireNova, "Use Fire Nova", true, ++priority, "FN"));
            priorityList.put(EnhanceAbility.SearingTotem, new Priority("Searing Totem", EnhanceAbility.SearingTotem, "Refresh Searing Totem", false, ++priority, "ST"));
            priorityList.put(EnhanceAbility.LightningShield, new Priority("Lightning Shield", EnhanceAbility.LightningShield, "Refresh Lightning Shield", true, ++priority, "LS"));
            priorityList.put(EnhanceAbility.RefreshTotems, new Priority("Refresh Totems", EnhanceAbility.RefreshTotems, "Refresh Totems", true, ++priority, ""));
        }
        return priorityList;
    }
}
