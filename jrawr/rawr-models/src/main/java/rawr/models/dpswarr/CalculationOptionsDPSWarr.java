// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/CalculationOptionsDPSWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port). getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsDPSDK).
package rawr.models.dpswarr;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.bosses.Impedance;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;

import java.util.ArrayList;
import java.util.List;

public class CalculationOptionsDPSWarr implements ICalculationOptionBase {
    public enum Maintenances {
        _RageGen__(0),
        BerserkerRage_(1),
        Bloodrage_(2),
        _Maintenance__(3),
        ShoutChoice_(4),
        BattleShout_(5),
        CommandingShout_(6),
        DemoralizingShout_(7),
        SunderArmor_(8),
        ThunderClap_(9),
        Hamstring_(10),
        _Periodics__(11),
        ShatteringThrow_(12),
        SweepingStrikes_(13),
        DeathWish_(14),
        Recklessness_(15),
        EnragedRegeneration_(16),
        _DamageDealers__(17),
        Fury_(18),
        Whirlwind_(19),
        Bloodthirst_(20),
        Bloodsurge_(21),
        Arms_(22),
        Bladestorm_(23),
        MortalStrike_(24),
        Rend_(25),
        Overpower_(26),
        TasteForBlood_(27),
        SuddenDeath_(28),
        Slam_(29),
        ExecuteSpam_(30),
        _RageDumps__(31),
        Cleave_(32),
        HeroicStrike_(33);

        public final int index;

        Maintenances(int index) {
            this.index = index;
        }
    }

    public CalculationOptionsDPSWarr() {
        allowFlooring = false;
        seUseDur = true;
        useMarkov = false;
        ptrMode = false;
        hideBadItemsDef = true;
        hideBadItemsSpl = true;
        hideBadItemsPvP = true;
        survScale = 1.0f;
        maintenance = new boolean[] {
                true,  // == Rage Gen ==
                    true,  // Berserker Rage
                    true,  // Bloodrage
                false, // == Maintenance ==
                    false, // Shout Choice
                        false, // Battle Shout
                        false, // Commanding Shout
                    false, // Demoralizing Shout
                    false, // Sunder Armor
                    false, // Thunder Clap
                    false, // Hamstring
                true,  // == Periodics ==
                    true,  // Shattering Throw
                    true,  // Sweeping Strikes
                    true,  // DeathWish
                    true,  // Recklessness
                    false, // Enraged Regeneration
                true,  // == Damage Dealers ==
                    true,  // Fury
                        true,  // Whirlwind
                        true,  // Bloodthirst
                        true,  // Bloodsurge
                    true,  // Arms
                        true,  // Bladestorm
                        true,  // Mortal Strike
                        true,  // Rend
                        true,  // Overpower
                        true,  // Taste for Blood
                        true,  // Sudden Death
                        true,  // Slam
                    true,  // <20% Execute Spamming
                true,  // == Rage Dumps ==
                    true,  // Cleave
                    true   // Heroic Strike
        };
        lag = 179f;
        react = 220f;
        filterType = "Content";
        filter = "All";
        bossName = "Custom";
        targetLevel = 83;
        targetArmor = StatConversion.NPC_ARMOR[targetLevel - 80];
        targetHP = 1000000f;
        duration = 300f;
        under20Perc = 0.17f;
        inBack = true; inBackPerc = 100;
        multipleTargets = false; multipleTargetsPerc = 25; multipleTargetsMax = 3;
        movingTargets = false;
        stunningTargets = false;
        fearingTargets = false;
        rootingTargets = false;
        disarmingTargets = false;
        aoETargets = false; aoETargetsFreq = 20; aoETargetsDMG = 5000;
        cachedLatency = lag / 1000f;
        cachedAllowedReact = Math.max(0f, (react - 200f) / 1000f);
    }

    private boolean allowFlooring;

    public boolean getAllowFlooring() {
        return allowFlooring;
    }

    public void setAllowFlooring(boolean value) {
        allowFlooring = value;
    }

    private boolean seUseDur;

    public boolean getSE_UseDur() {
        return seUseDur;
    }

    public void setSE_UseDur(boolean value) {
        seUseDur = value;
    }

    private boolean useMarkov;

    public boolean getUseMarkov() {
        return useMarkov;
    }

    public void setUseMarkov(boolean value) {
        useMarkov = value;
    }

    private boolean ptrMode;

    public boolean getPTRMode() {
        return ptrMode;
    }

    public void setPTRMode(boolean value) {
        ptrMode = value;
    }

    private boolean hideBadItemsDef;

    public boolean getHideBadItems_Def() {
        return hideBadItemsDef;
    }

    public void setHideBadItems_Def(boolean value) {
        hideBadItemsDef = value;
    }

    private boolean hideBadItemsSpl;

    public boolean getHideBadItems_Spl() {
        return hideBadItemsSpl;
    }

    public void setHideBadItems_Spl(boolean value) {
        hideBadItemsSpl = value;
    }

    private boolean hideBadItemsPvP;

    public boolean getHideBadItems_PvP() {
        return hideBadItemsPvP;
    }

    public void setHideBadItems_PvP(boolean value) {
        hideBadItemsPvP = value;
    }

    private float survScale;

    public float getSurvScale() {
        return survScale;
    }

    public void setSurvScale(float value) {
        survScale = value;
    }

    private boolean[] statsList = new boolean[] { true, true, true, true, true, true, true, true, true, true };

    public boolean[] getStatsList() {
        return statsList;
    }

    public void setStatsList(boolean[] value) {
        statsList = value;
    }

    private int statsIncrement = 100;

    public int getStatsIncrement() {
        return statsIncrement;
    }

    public void setStatsIncrement(int value) {
        statsIncrement = value;
    }

    private String calculationToGraph = "DPS Rating";

    public String getCalculationToGraph() {
        return calculationToGraph;
    }

    public void setCalculationToGraph(String value) {
        calculationToGraph = value;
    }

    private boolean[] maintenance;

    public boolean[] getMaintenance() {
        if (maintenance == null) {
            maintenance = new boolean[] {
                    true, true, true, false, false, false, false, false, false, false, false,
                    true, true, true, true, true, false,
                    true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                    true, true, true };
        }
        return maintenance;
    }

    public void setMaintenance(boolean[] value) {
        maintenance = value;
    }

    public boolean getM_BerserkerRage() {
        return getMaintenance()[Maintenances.BerserkerRage_.index];
    }

    public void setM_BerserkerRage(boolean value) {
        getMaintenance()[Maintenances.BerserkerRage_.index] = value;
    }

    public boolean getM_Bloodrage() {
        return getMaintenance()[Maintenances.Bloodrage_.index];
    }

    public void setM_Bloodrage(boolean value) {
        getMaintenance()[Maintenances.Bloodrage_.index] = value;
    }

    public boolean getM_BattleShout() {
        return getMaintenance()[Maintenances.BattleShout_.index];
    }

    public void setM_BattleShout(boolean value) {
        getMaintenance()[Maintenances.BattleShout_.index] = value;
    }

    public boolean getM_CommandingShout() {
        return getMaintenance()[Maintenances.CommandingShout_.index];
    }

    public void setM_CommandingShout(boolean value) {
        getMaintenance()[Maintenances.CommandingShout_.index] = value;
    }

    public boolean getM_DemoralizingShout() {
        return getMaintenance()[Maintenances.DemoralizingShout_.index];
    }

    public void setM_DemoralizingShout(boolean value) {
        getMaintenance()[Maintenances.DemoralizingShout_.index] = value;
    }

    public boolean getM_SunderArmor() {
        return getMaintenance()[Maintenances.SunderArmor_.index];
    }

    public void setM_SunderArmor(boolean value) {
        getMaintenance()[Maintenances.SunderArmor_.index] = value;
    }

    public boolean getM_ThunderClap() {
        return getMaintenance()[Maintenances.ThunderClap_.index];
    }

    public void setM_ThunderClap(boolean value) {
        getMaintenance()[Maintenances.ThunderClap_.index] = value;
    }

    public boolean getM_Hamstring() {
        return getMaintenance()[Maintenances.Hamstring_.index];
    }

    public void setM_Hamstring(boolean value) {
        getMaintenance()[Maintenances.Hamstring_.index] = value;
    }

    public boolean getM_ShatteringThrow() {
        return getMaintenance()[Maintenances.ShatteringThrow_.index];
    }

    public void setM_ShatteringThrow(boolean value) {
        getMaintenance()[Maintenances.ShatteringThrow_.index] = value;
    }

    public boolean getM_SweepingStrikes() {
        return getMaintenance()[Maintenances.SweepingStrikes_.index];
    }

    public void setM_SweepingStrikes(boolean value) {
        getMaintenance()[Maintenances.SweepingStrikes_.index] = value;
    }

    public boolean getM_DeathWish() {
        return getMaintenance()[Maintenances.DeathWish_.index];
    }

    public void setM_DeathWish(boolean value) {
        getMaintenance()[Maintenances.DeathWish_.index] = value;
    }

    public boolean getM_Recklessness() {
        return getMaintenance()[Maintenances.Recklessness_.index];
    }

    public void setM_Recklessness(boolean value) {
        getMaintenance()[Maintenances.Recklessness_.index] = value;
    }

    public boolean getM_EnragedRegeneration() {
        return getMaintenance()[Maintenances.EnragedRegeneration_.index];
    }

    public void setM_EnragedRegeneration(boolean value) {
        getMaintenance()[Maintenances.EnragedRegeneration_.index] = value;
    }

    public boolean getM_Whirlwind() {
        return getMaintenance()[Maintenances.Whirlwind_.index];
    }

    public void setM_Whirlwind(boolean value) {
        getMaintenance()[Maintenances.Whirlwind_.index] = value;
    }

    public boolean getM_Bloodthirst() {
        return getMaintenance()[Maintenances.Bloodthirst_.index];
    }

    public void setM_Bloodthirst(boolean value) {
        getMaintenance()[Maintenances.Bloodthirst_.index] = value;
    }

    public boolean getM_Bloodsurge() {
        return getMaintenance()[Maintenances.Bloodsurge_.index];
    }

    public void setM_Bloodsurge(boolean value) {
        getMaintenance()[Maintenances.Bloodsurge_.index] = value;
    }

    public boolean getM_Bladestorm() {
        return getMaintenance()[Maintenances.Bladestorm_.index];
    }

    public void setM_Bladestorm(boolean value) {
        getMaintenance()[Maintenances.Bladestorm_.index] = value;
    }

    public boolean getM_MortalStrike() {
        return getMaintenance()[Maintenances.MortalStrike_.index];
    }

    public void setM_MortalStrike(boolean value) {
        getMaintenance()[Maintenances.MortalStrike_.index] = value;
    }

    public boolean getM_Rend() {
        return getMaintenance()[Maintenances.Rend_.index];
    }

    public void setM_Rend(boolean value) {
        getMaintenance()[Maintenances.Rend_.index] = value;
    }

    public boolean getM_Overpower() {
        return getMaintenance()[Maintenances.Overpower_.index];
    }

    public void setM_Overpower(boolean value) {
        getMaintenance()[Maintenances.Overpower_.index] = value;
    }

    public boolean getM_TasteForBlood() {
        return getMaintenance()[Maintenances.TasteForBlood_.index];
    }

    public void setM_TasteForBlood(boolean value) {
        getMaintenance()[Maintenances.TasteForBlood_.index] = value;
    }

    public boolean getM_SuddenDeath() {
        return getMaintenance()[Maintenances.SuddenDeath_.index];
    }

    public void setM_SuddenDeath(boolean value) {
        getMaintenance()[Maintenances.SuddenDeath_.index] = value;
    }

    public boolean getM_Slam() {
        return getMaintenance()[Maintenances.Slam_.index];
    }

    public void setM_Slam(boolean value) {
        getMaintenance()[Maintenances.Slam_.index] = value;
    }

    public boolean getM_ExecuteSpam() {
        return getMaintenance()[Maintenances.ExecuteSpam_.index];
    }

    public void setM_ExecuteSpam(boolean value) {
        getMaintenance()[Maintenances.ExecuteSpam_.index] = value;
    }

    public boolean getM_Cleave() {
        return getMaintenance()[Maintenances.Cleave_.index];
    }

    public void setM_Cleave(boolean value) {
        getMaintenance()[Maintenances.Cleave_.index] = value;
    }

    public boolean getM_HeroicStrike() {
        return getMaintenance()[Maintenances.HeroicStrike_.index];
    }

    public void setM_HeroicStrike(boolean value) {
        getMaintenance()[Maintenances.HeroicStrike_.index] = value;
    }

    private float lag;

    public float getLag() {
        return lag;
    }

    public void setLag(float value) {
        lag = value;
        cachedLatency = value / 1000f;
    }

    private float react;

    public float getReact() {
        return react;
    }

    public void setReact(float value) {
        react = value;
        cachedAllowedReact = Math.max(0f, (value - 200f) / 1000f);
    }

    private float cachedLatency = -1000000f;

    public float getLatency() {
        return cachedLatency;
    }

    private float cachedAllowedReact = -1000000f;

    public float getAllowedReact() {
        return cachedAllowedReact;
    }

    public float getFullLatency() {
        return getAllowedReact() + getLatency();
    }

    private String filterType;

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String value) {
        filterType = value;
    }

    private String filter;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String value) {
        filter = value;
    }

    private String bossName;

    public String getBossName() {
        return bossName;
    }

    public void setBossName(String value) {
        bossName = value;
    }

    private int targetLevel;

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    private float targetArmor;

    public float getTargetArmor() {
        return targetArmor;
    }

    public void setTargetArmor(float value) {
        targetArmor = value;
    }

    private float targetHP;

    public float getTargetHP() {
        return targetHP;
    }

    public void setTargetHP(float value) {
        targetHP = value;
    }

    private float duration;

    public float getDuration() {
        return duration;
    }

    public void setDuration(float value) {
        duration = value;
    }

    private float under20Perc;

    public float getUnder20Perc() {
        return under20Perc;
    }

    public void setUnder20Perc(float value) {
        under20Perc = value;
    }

    private boolean inBack;

    public boolean getInBack() {
        return inBack;
    }

    public void setInBack(boolean value) {
        inBack = value;
    }

    private int inBackPerc;

    public int getInBackPerc() {
        return inBackPerc;
    }

    public void setInBackPerc(int value) {
        inBackPerc = value;
    }

    private boolean multipleTargets;

    public boolean getMultipleTargets() {
        return multipleTargets;
    }

    public void setMultipleTargets(boolean value) {
        multipleTargets = value;
    }

    private int multipleTargetsPerc;

    public int getMultipleTargetsPerc() {
        return multipleTargetsPerc;
    }

    public void setMultipleTargetsPerc(int value) {
        multipleTargetsPerc = value;
    }

    private float multipleTargetsMax;

    public float getMultipleTargetsMax() {
        return multipleTargetsMax;
    }

    public void setMultipleTargetsMax(float value) {
        multipleTargetsMax = value;
    }

    private boolean movingTargets;

    public boolean getMovingTargets() {
        return movingTargets;
    }

    public void setMovingTargets(boolean value) {
        movingTargets = value;
    }

    private boolean stunningTargets;

    public boolean getStunningTargets() {
        return stunningTargets;
    }

    public void setStunningTargets(boolean value) {
        stunningTargets = value;
    }

    private boolean fearingTargets;

    public boolean getFearingTargets() {
        return fearingTargets;
    }

    public void setFearingTargets(boolean value) {
        fearingTargets = value;
    }

    private boolean rootingTargets;

    public boolean getRootingTargets() {
        return rootingTargets;
    }

    public void setRootingTargets(boolean value) {
        rootingTargets = value;
    }

    private boolean disarmingTargets;

    public boolean getDisarmingTargets() {
        return disarmingTargets;
    }

    public void setDisarmingTargets(boolean value) {
        disarmingTargets = value;
    }

    private boolean aoETargets;

    public boolean getAoETargets() {
        return aoETargets;
    }

    public void setAoETargets(boolean value) {
        aoETargets = value;
    }

    private int aoETargetsFreq;

    public int getAoETargetsFreq() {
        return aoETargetsFreq;
    }

    public void setAoETargetsFreq(int value) {
        aoETargetsFreq = value;
    }

    private float aoETargetsDMG;

    public float getAoETargetsDMG() {
        return aoETargetsDMG;
    }

    public void setAoETargetsDMG(float value) {
        aoETargetsDMG = value;
    }

    private List<Impedance> stuns;

    public List<Impedance> getStuns() {
        if (stuns == null) stuns = new ArrayList<Impedance>();
        return stuns;
    }

    public void setStuns(List<Impedance> value) {
        stuns = value;
    }

    private List<Impedance> moves;

    public List<Impedance> getMoves() {
        if (moves == null) moves = new ArrayList<Impedance>();
        return moves;
    }

    public void setMoves(List<Impedance> value) {
        moves = value;
    }

    private List<Impedance> fears;

    public List<Impedance> getFears() {
        if (fears == null) fears = new ArrayList<Impedance>();
        return fears;
    }

    public void setFears(List<Impedance> value) {
        fears = value;
    }

    private List<Impedance> roots;

    public List<Impedance> getRoots() {
        if (roots == null) roots = new ArrayList<Impedance>();
        return roots;
    }

    public void setRoots(List<Impedance> value) {
        roots = value;
    }

    private List<Impedance> disarms;

    public List<Impedance> getDisarms() {
        if (disarms == null) disarms = new ArrayList<Impedance>();
        return disarms;
    }

    public void setDisarms(List<Impedance> value) {
        disarms = value;
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsDPSWarr xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<AllowFlooring>").append(allowFlooring ? "true" : "false").append("</AllowFlooring>");
        sb.append("<SE_UseDur>").append(seUseDur ? "true" : "false").append("</SE_UseDur>");
        sb.append("<UseMarkov>").append(useMarkov ? "true" : "false").append("</UseMarkov>");
        sb.append("<PTRMode>").append(ptrMode ? "true" : "false").append("</PTRMode>");
        sb.append("<HideBadItems_Def>").append(hideBadItemsDef ? "true" : "false").append("</HideBadItems_Def>");
        sb.append("<HideBadItems_Spl>").append(hideBadItemsSpl ? "true" : "false").append("</HideBadItems_Spl>");
        sb.append("<HideBadItems_PvP>").append(hideBadItemsPvP ? "true" : "false").append("</HideBadItems_PvP>");
        sb.append("<SurvScale>").append(NetFormat.floatR(survScale)).append("</SurvScale>");
        sb.append("<StatsList>");
        for (boolean b : getStatsList()) sb.append("<boolean>").append(b ? "true" : "false").append("</boolean>");
        sb.append("</StatsList>");
        sb.append("<StatsIncrement>").append(statsIncrement).append("</StatsIncrement>");
        sb.append("<CalculationToGraph>").append(calculationToGraph).append("</CalculationToGraph>");
        sb.append("<Maintenance>");
        for (boolean b : getMaintenance()) sb.append("<boolean>").append(b ? "true" : "false").append("</boolean>");
        sb.append("</Maintenance>");
        sb.append("<Lag>").append(NetFormat.floatR(lag)).append("</Lag>");
        sb.append("<React>").append(NetFormat.floatR(react)).append("</React>");
        sb.append("<FilterType>").append(filterType).append("</FilterType>");
        sb.append("<Filter>").append(filter).append("</Filter>");
        sb.append("<BossName>").append(bossName).append("</BossName>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TargetArmor>").append(NetFormat.floatR(targetArmor)).append("</TargetArmor>");
        sb.append("<TargetHP>").append(NetFormat.floatR(targetHP)).append("</TargetHP>");
        sb.append("<Duration>").append(NetFormat.floatR(duration)).append("</Duration>");
        sb.append("<Under20Perc>").append(NetFormat.floatR(under20Perc)).append("</Under20Perc>");
        sb.append("<InBack>").append(inBack ? "true" : "false").append("</InBack>");
        sb.append("<InBackPerc>").append(inBackPerc).append("</InBackPerc>");
        sb.append("<MultipleTargets>").append(multipleTargets ? "true" : "false").append("</MultipleTargets>");
        sb.append("<MultipleTargetsPerc>").append(multipleTargetsPerc).append("</MultipleTargetsPerc>");
        sb.append("<MultipleTargetsMax>").append(NetFormat.floatR(multipleTargetsMax)).append("</MultipleTargetsMax>");
        sb.append("<MovingTargets>").append(movingTargets ? "true" : "false").append("</MovingTargets>");
        sb.append("<StunningTargets>").append(stunningTargets ? "true" : "false").append("</StunningTargets>");
        sb.append("<FearingTargets>").append(fearingTargets ? "true" : "false").append("</FearingTargets>");
        sb.append("<RootingTargets>").append(rootingTargets ? "true" : "false").append("</RootingTargets>");
        sb.append("<DisarmingTargets>").append(disarmingTargets ? "true" : "false").append("</DisarmingTargets>");
        sb.append("<AoETargets>").append(aoETargets ? "true" : "false").append("</AoETargets>");
        sb.append("<AoETargetsFreq>").append(aoETargetsFreq).append("</AoETargetsFreq>");
        sb.append("<AoETargetsDMG>").append(NetFormat.floatR(aoETargetsDMG)).append("</AoETargetsDMG>");
        appendImpedances(sb, "Stuns", getStuns());
        appendImpedances(sb, "Moves", getMoves());
        appendImpedances(sb, "Fears", getFears());
        appendImpedances(sb, "Roots", getRoots());
        appendImpedances(sb, "Disarms", getDisarms());
        sb.append("</CalculationOptionsDPSWarr>");
        return sb.toString();
    }

    private static void appendImpedances(StringBuilder sb, String name, List<Impedance> list) {
        if (list.isEmpty()) return;
        sb.append("<").append(name).append(">");
        for (Impedance i : list) {
            sb.append("<Impedance>");
            sb.append("<Frequency>").append(NetFormat.floatR(i.frequency)).append("</Frequency>");
            sb.append("<Duration>").append(NetFormat.floatR(i.duration)).append("</Duration>");
            sb.append("<Chance>").append(NetFormat.floatR(i.chance)).append("</Chance>");
            sb.append("<Breakable>").append(i.breakable ? "true" : "false").append("</Breakable>");
            sb.append("</Impedance>");
        }
        sb.append("</").append(name).append(">");
    }
}
