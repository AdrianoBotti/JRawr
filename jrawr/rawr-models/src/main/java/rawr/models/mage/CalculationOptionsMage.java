// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CalculationOptionsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted; SequenceReconstruction/DisplayCalculations fields are
// added with the sequence/solver slices; getXml is a reflective best-effort writer (save path).
package rawr.models.mage;

import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;

public class CalculationOptionsMage implements ICalculationOptionBase, ICharacterCalculationOptions {
    public static final float SetBonus4T8ProcRate = 0.25f;

    /**
     * C# fields marked {@code [XmlIgnore]}: runtime solver state, not part of the saved options.
     * They must stay out of both the writer and the reader; an empty saved array would otherwise
     * look like an initialized incremental set and make the model solve with no cycles.
     */
    private static final java.util.Set<String> XML_IGNORED_FIELDS = java.util.Set.of(
            "incrementalSetStateIndexes", "incrementalSetSortedStates", "incrementalSetSegments",
            "incrementalSetSpells", "incrementalSetArmor", "incrementalSetVariableType",
            "incrementalSetManaSegment", "adviseAdvancedSolver", "cooldownRestrictionList",
            "character", "calculations", "sequenceReconstruction", "cooldownStackingCache",
            "levelScalingFactor");

    /** True when an XML element belongs to a C# {@code [XmlIgnore]} runtime field. */
    static boolean isXmlIgnoredField(String elementName) {
        if (elementName == null || elementName.isEmpty()) {
            return false;
        }
        String fieldName = java.lang.Character.toLowerCase(elementName.charAt(0)) + elementName.substring(1);
        return XML_IGNORED_FIELDS.contains(fieldName);
    }

    private int[] incrementalSetStateIndexes;
    private int[] incrementalSetSortedStates;
    private int[] incrementalSetSegments;
    private CycleId[] incrementalSetSpells;
    private String incrementalSetArmor;
    private VariableType[] incrementalSetVariableType;
    private int[] incrementalSetManaSegment;
    private boolean adviseAdvancedSolver;
    private java.util.List<CooldownRestriction> cooldownRestrictionList;
    private boolean useFireWard;
    private boolean useFrostWard;
    private float frostbiteUtilization;
    private boolean beta;
    private int targetLevel;
    private int aoeTargetLevel;
    private float latencyCast;
    private float latencyGCD;
    private float latencyChannel;
    private float movementFrequency;
    private float movementDuration;
    private int aoeTargets;
    private float arcaneResist;
    private float fireResist;
    private float frostResist;
    private float natureResist;
    private float shadowResist;
    private float holyResist;
    private float tpsLimit;
    private boolean heroismAvailable;
    private boolean powerInfusionAvailable;
    private boolean potionOfWildMagic;
    private boolean potionOfSpeed;
    private boolean flameCap;
    private float moltenFuryPercentage;
    private boolean maintainScorch;
    private boolean maintainSnare;
    private float interruptFrequency;
    private float aoeDuration;
    private boolean smartOptimization;
    private float dpsTime;
    private boolean drumsOfBattle;
    private boolean automaticArmor;
    private boolean forceIncrementalOptimizations;
    private boolean incrementalOptimizations;
    private float snaredTime;
    private float warlockSpellPower;
    private boolean reconstructSequence;
    private boolean comparisonSegmentCooldowns;
    private boolean displaySegmentCooldowns;
    private boolean comparisonIntegralMana;
    private boolean displayIntegralMana;
    private boolean comparisonSegmentMana;
    private boolean displaySegmentMana;
    private int comparisonAdvancedConstraintsLevel;
    private int displayAdvancedConstraintsLevel;
    private int fixedSegmentDuration;
    private boolean variableSegmentDuration;
    private String additionalSegmentSplits;
    private double lowerBoundHint;
    private String cooldownRestrictions;
    private boolean enableHastedEvocation;
    private boolean enable2T10Evocation;
    private boolean advancedHasteProcs;
    private boolean encounterEnabled;
    private Encounter encounter;
    private MIPMethod mIPMethod;
    private float innervate;
    private float manaTide;
    private float fragmentation;
    private float survivabilityRating;
    private boolean aldor;
    private boolean pVP;
    private int heroismControl;
    private boolean averageCooldowns;
    private boolean evocationEnabled;
    private boolean manaPotionEnabled;
    private boolean manaGemEnabled;
    private boolean mirrorImageEnabled;
    private boolean disableCooldowns;
    private int maxHeapLimit;
    private float drinkingTime;
    private float targetDamage;
    private boolean farmingMode;
    private float focusMagicTargetCritRate;
    private boolean unlimitedMana;
    private float absorptionPerSecond;
    private java.util.List<SpellWeight> customSpellMix = new java.util.ArrayList<SpellWeight>(); // oracle DLL yields a non-null empty list when absent; DECISIONS #24
    private boolean customSpellMixEnabled;
    private int globalRestarts;
    private boolean customSpellMixOnly;
    private float meleeDps;
    private float meleeCrit;
    private float meleeDot;
    private float physicalDps;
    private float physicalCrit;
    private float physicalDot;
    private float fireDps;
    private float fireCrit;
    private float fireDot;
    private float frostDps;
    private float frostCrit;
    private float frostDot;
    private float arcaneDps;
    private float arcaneCrit;
    private float arcaneDot;
    private float shadowDps;
    private float shadowCrit;
    private float shadowDot;
    private float natureDps;
    private float natureCrit;
    private float natureDot;
    private float holyDps;
    private float holyCrit;
    private float holyDot;
    private float burstWindow;
    private float burstImpacts;
    private float chanceToLiveScore;
    private float chanceToLiveAttenuation;
    private float effectSpiritMultiplier;
    private float effectCritBonus;
    private float effectCritDamageBonus;
    private float effectDamageMultiplier;
    private float effectHasteMultiplier;
    private float effectCostMultiplier;
    private float effectRegenMultiplier;
    private boolean effectDisableManaSources;
    private float effectShadowSilenceFrequency;
    private float effectShadowSilenceDuration;
    private float effectShadowManaDrainFrequency;
    private float effectShadowManaDrain;
    private float effectArcaneOtherBinary;
    private float effectFireOtherBinary;
    private float effectFrostOtherBinary;
    private float effectShadowOtherBinary;
    private float effectNatureOtherBinary;
    private float effectHolyOtherBinary;
    private float effectArcaneOther;
    private float effectFireOther;
    private float effectFrostOther;
    private float effectShadowOther;
    private float effectNatureOther;
    private float effectHolyOther;

    public int[] getIncrementalSetStateIndexes() {
        return incrementalSetStateIndexes;
    }

    public void setIncrementalSetStateIndexes(int[] value) {
        incrementalSetStateIndexes = value;
    }

    public int[] getIncrementalSetSortedStates() {
        return incrementalSetSortedStates;
    }

    public void setIncrementalSetSortedStates(int[] value) {
        incrementalSetSortedStates = value;
    }

    public int[] getIncrementalSetSegments() {
        return incrementalSetSegments;
    }

    public void setIncrementalSetSegments(int[] value) {
        incrementalSetSegments = value;
    }

    public CycleId[] getIncrementalSetSpells() {
        return incrementalSetSpells;
    }

    public void setIncrementalSetSpells(CycleId[] value) {
        incrementalSetSpells = value;
    }

    public String getIncrementalSetArmor() {
        return incrementalSetArmor;
    }

    public void setIncrementalSetArmor(String value) {
        incrementalSetArmor = value;
    }

    public VariableType[] getIncrementalSetVariableType() {
        return incrementalSetVariableType;
    }

    public void setIncrementalSetVariableType(VariableType[] value) {
        incrementalSetVariableType = value;
    }

    public int[] getIncrementalSetManaSegment() {
        return incrementalSetManaSegment;
    }

    public void setIncrementalSetManaSegment(int[] value) {
        incrementalSetManaSegment = value;
    }

    public boolean getAdviseAdvancedSolver() {
        return adviseAdvancedSolver;
    }

    public void setAdviseAdvancedSolver(boolean value) {
        adviseAdvancedSolver = value;
    }

    public java.util.List<CooldownRestriction> getCooldownRestrictionList() {
        return cooldownRestrictionList;
    }

    public void setCooldownRestrictionList(java.util.List<CooldownRestriction> value) {
        cooldownRestrictionList = value;
    }

    public boolean getUseFireWard() {
        return useFireWard;
    }

    public void setUseFireWard(boolean value) {
        useFireWard = value;
    }

    public boolean getUseFrostWard() {
        return useFrostWard;
    }

    public void setUseFrostWard(boolean value) {
        useFrostWard = value;
    }

    public float getFrostbiteUtilization() {
        return frostbiteUtilization;
    }

    public void setFrostbiteUtilization(float value) {
        frostbiteUtilization = value;
    }

    public boolean getBeta() {
        return beta;
    }

    public void setBeta(boolean value) {
        beta = value;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    public int getAoeTargetLevel() {
        return aoeTargetLevel;
    }

    public void setAoeTargetLevel(int value) {
        aoeTargetLevel = value;
    }

    public float getLatencyCast() {
        return latencyCast;
    }

    public void setLatencyCast(float value) {
        latencyCast = value;
    }

    public float getLatencyGCD() {
        return latencyGCD;
    }

    public void setLatencyGCD(float value) {
        latencyGCD = value;
    }

    public float getLatencyChannel() {
        return latencyChannel;
    }

    public void setLatencyChannel(float value) {
        latencyChannel = value;
    }

    public float getMovementFrequency() {
        return movementFrequency;
    }

    public void setMovementFrequency(float value) {
        movementFrequency = value;
    }

    public float getMovementDuration() {
        return movementDuration;
    }

    public void setMovementDuration(float value) {
        movementDuration = value;
    }

    public int getAoeTargets() {
        return aoeTargets;
    }

    public void setAoeTargets(int value) {
        aoeTargets = value;
    }

    public float getArcaneResist() {
        return arcaneResist;
    }

    public void setArcaneResist(float value) {
        arcaneResist = value;
    }

    public float getFireResist() {
        return fireResist;
    }

    public void setFireResist(float value) {
        fireResist = value;
    }

    public float getFrostResist() {
        return frostResist;
    }

    public void setFrostResist(float value) {
        frostResist = value;
    }

    public float getNatureResist() {
        return natureResist;
    }

    public void setNatureResist(float value) {
        natureResist = value;
    }

    public float getShadowResist() {
        return shadowResist;
    }

    public void setShadowResist(float value) {
        shadowResist = value;
    }

    public float getHolyResist() {
        return holyResist;
    }

    public void setHolyResist(float value) {
        holyResist = value;
    }

    public float getTpsLimit() {
        return tpsLimit;
    }

    public void setTpsLimit(float value) {
        tpsLimit = value;
    }

    public boolean getHeroismAvailable() {
        return heroismAvailable;
    }

    public void setHeroismAvailable(boolean value) {
        heroismAvailable = value;
    }

    public boolean getPowerInfusionAvailable() {
        return powerInfusionAvailable;
    }

    public void setPowerInfusionAvailable(boolean value) {
        powerInfusionAvailable = value;
    }

    public boolean getPotionOfWildMagic() {
        return potionOfWildMagic;
    }

    public void setPotionOfWildMagic(boolean value) {
        potionOfWildMagic = value;
    }

    public boolean getPotionOfSpeed() {
        return potionOfSpeed;
    }

    public void setPotionOfSpeed(boolean value) {
        potionOfSpeed = value;
    }

    public boolean getFlameCap() {
        return flameCap;
    }

    public void setFlameCap(boolean value) {
        flameCap = value;
    }

    public float getMoltenFuryPercentage() {
        return moltenFuryPercentage;
    }

    public void setMoltenFuryPercentage(float value) {
        moltenFuryPercentage = value;
    }

    public boolean getMaintainScorch() {
        return maintainScorch;
    }

    public void setMaintainScorch(boolean value) {
        maintainScorch = value;
    }

    public boolean getMaintainSnare() {
        return maintainSnare;
    }

    public void setMaintainSnare(boolean value) {
        maintainSnare = value;
    }

    public float getInterruptFrequency() {
        return interruptFrequency;
    }

    public void setInterruptFrequency(float value) {
        interruptFrequency = value;
    }

    public float getAoeDuration() {
        return aoeDuration;
    }

    public void setAoeDuration(float value) {
        aoeDuration = value;
    }

    public boolean getSmartOptimization() {
        return smartOptimization;
    }

    public void setSmartOptimization(boolean value) {
        smartOptimization = value;
    }

    public float getDpsTime() {
        return dpsTime;
    }

    public void setDpsTime(float value) {
        dpsTime = value;
    }

    public boolean getDrumsOfBattle() {
        return drumsOfBattle;
    }

    public void setDrumsOfBattle(boolean value) {
        drumsOfBattle = value;
    }

    public boolean getAutomaticArmor() {
        return automaticArmor;
    }

    public void setAutomaticArmor(boolean value) {
        automaticArmor = value;
    }

    public boolean getForceIncrementalOptimizations() {
        return forceIncrementalOptimizations;
    }

    public void setForceIncrementalOptimizations(boolean value) {
        forceIncrementalOptimizations = value;
    }

    public boolean getIncrementalOptimizations() {
        return incrementalOptimizations;
    }

    public void setIncrementalOptimizations(boolean value) {
        incrementalOptimizations = value;
    }

    public float getSnaredTime() {
        return snaredTime;
    }

    public void setSnaredTime(float value) {
        snaredTime = value;
    }

    public float getWarlockSpellPower() {
        return warlockSpellPower;
    }

    public void setWarlockSpellPower(float value) {
        warlockSpellPower = value;
    }

    public boolean getReconstructSequence() {
        return reconstructSequence;
    }

    public void setReconstructSequence(boolean value) {
        reconstructSequence = value;
    }

    public boolean getComparisonSegmentCooldowns() {
        return comparisonSegmentCooldowns;
    }

    public void setComparisonSegmentCooldowns(boolean value) {
        comparisonSegmentCooldowns = value;
    }

    public boolean getDisplaySegmentCooldowns() {
        return displaySegmentCooldowns;
    }

    public void setDisplaySegmentCooldowns(boolean value) {
        displaySegmentCooldowns = value;
    }

    public boolean getComparisonIntegralMana() {
        return comparisonIntegralMana;
    }

    public void setComparisonIntegralMana(boolean value) {
        comparisonIntegralMana = value;
    }

    public boolean getDisplayIntegralMana() {
        return displayIntegralMana;
    }

    public void setDisplayIntegralMana(boolean value) {
        displayIntegralMana = value;
    }

    public boolean getComparisonSegmentMana() {
        return comparisonSegmentMana;
    }

    public void setComparisonSegmentMana(boolean value) {
        comparisonSegmentMana = value;
    }

    public boolean getDisplaySegmentMana() {
        return displaySegmentMana;
    }

    public void setDisplaySegmentMana(boolean value) {
        displaySegmentMana = value;
    }

    public int getComparisonAdvancedConstraintsLevel() {
        return comparisonAdvancedConstraintsLevel;
    }

    public void setComparisonAdvancedConstraintsLevel(int value) {
        comparisonAdvancedConstraintsLevel = value;
    }

    public int getDisplayAdvancedConstraintsLevel() {
        return displayAdvancedConstraintsLevel;
    }

    public void setDisplayAdvancedConstraintsLevel(int value) {
        displayAdvancedConstraintsLevel = value;
    }

    public int getFixedSegmentDuration() {
        return fixedSegmentDuration;
    }

    public void setFixedSegmentDuration(int value) {
        fixedSegmentDuration = value;
    }

    public boolean getVariableSegmentDuration() {
        return variableSegmentDuration;
    }

    public void setVariableSegmentDuration(boolean value) {
        variableSegmentDuration = value;
    }

    public String getAdditionalSegmentSplits() {
        return additionalSegmentSplits;
    }

    public void setAdditionalSegmentSplits(String value) {
        additionalSegmentSplits = value;
    }

    public double getLowerBoundHint() {
        return lowerBoundHint;
    }

    public void setLowerBoundHint(double value) {
        lowerBoundHint = value;
    }

    public String getCooldownRestrictions() {
        return cooldownRestrictions;
    }

    public void setCooldownRestrictions(String value) {
        cooldownRestrictions = value;
    }

    public boolean getEnableHastedEvocation() {
        return enableHastedEvocation;
    }

    public void setEnableHastedEvocation(boolean value) {
        enableHastedEvocation = value;
    }

    public boolean getEnable2T10Evocation() {
        return enable2T10Evocation;
    }

    public void setEnable2T10Evocation(boolean value) {
        enable2T10Evocation = value;
    }

    public boolean getAdvancedHasteProcs() {
        return advancedHasteProcs;
    }

    public void setAdvancedHasteProcs(boolean value) {
        advancedHasteProcs = value;
    }

    public boolean getEncounterEnabled() {
        return encounterEnabled;
    }

    public void setEncounterEnabled(boolean value) {
        encounterEnabled = value;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter value) {
        encounter = value;
    }

    public MIPMethod getMIPMethod() {
        return mIPMethod;
    }

    public void setMIPMethod(MIPMethod value) {
        mIPMethod = value;
    }

    public float getInnervate() {
        return innervate;
    }

    public void setInnervate(float value) {
        innervate = value;
    }

    public float getManaTide() {
        return manaTide;
    }

    public void setManaTide(float value) {
        manaTide = value;
    }

    public float getFragmentation() {
        return fragmentation;
    }

    public void setFragmentation(float value) {
        fragmentation = value;
    }

    public float getSurvivabilityRating() {
        return survivabilityRating;
    }

    public void setSurvivabilityRating(float value) {
        survivabilityRating = value;
    }

    public boolean getAldor() {
        return aldor;
    }

    public void setAldor(boolean value) {
        aldor = value;
    }

    public boolean getPVP() {
        return pVP;
    }

    public void setPVP(boolean value) {
        pVP = value;
    }

    public int getHeroismControl() {
        return heroismControl;
    }

    public void setHeroismControl(int value) {
        heroismControl = value;
    }

    public boolean getAverageCooldowns() {
        return averageCooldowns;
    }

    public void setAverageCooldowns(boolean value) {
        averageCooldowns = value;
    }

    public boolean getEvocationEnabled() {
        return evocationEnabled;
    }

    public void setEvocationEnabled(boolean value) {
        evocationEnabled = value;
    }

    public boolean getManaPotionEnabled() {
        return manaPotionEnabled;
    }

    public void setManaPotionEnabled(boolean value) {
        manaPotionEnabled = value;
    }

    public boolean getManaGemEnabled() {
        return manaGemEnabled;
    }

    public void setManaGemEnabled(boolean value) {
        manaGemEnabled = value;
    }

    public boolean getMirrorImageEnabled() {
        return mirrorImageEnabled;
    }

    public void setMirrorImageEnabled(boolean value) {
        mirrorImageEnabled = value;
    }

    public boolean getDisableCooldowns() {
        return disableCooldowns;
    }

    public void setDisableCooldowns(boolean value) {
        disableCooldowns = value;
    }

    public int getMaxHeapLimit() {
        return maxHeapLimit;
    }

    public void setMaxHeapLimit(int value) {
        maxHeapLimit = value;
    }

    public float getDrinkingTime() {
        return drinkingTime;
    }

    public void setDrinkingTime(float value) {
        drinkingTime = value;
    }

    public float getTargetDamage() {
        return targetDamage;
    }

    public void setTargetDamage(float value) {
        targetDamage = value;
    }

    public boolean getFarmingMode() {
        return farmingMode;
    }

    public void setFarmingMode(boolean value) {
        farmingMode = value;
    }

    public float getFocusMagicTargetCritRate() {
        return focusMagicTargetCritRate;
    }

    public void setFocusMagicTargetCritRate(float value) {
        focusMagicTargetCritRate = value;
    }

    public boolean getUnlimitedMana() {
        return unlimitedMana;
    }

    public void setUnlimitedMana(boolean value) {
        unlimitedMana = value;
    }

    public float getAbsorptionPerSecond() {
        return absorptionPerSecond;
    }

    public void setAbsorptionPerSecond(float value) {
        absorptionPerSecond = value;
    }

    public java.util.List<SpellWeight> getCustomSpellMix() {
        return customSpellMix;
    }

    public void setCustomSpellMix(java.util.List<SpellWeight> value) {
        customSpellMix = value;
    }

    public boolean getCustomSpellMixEnabled() {
        return customSpellMixEnabled;
    }

    public void setCustomSpellMixEnabled(boolean value) {
        customSpellMixEnabled = value;
    }

    public int getGlobalRestarts() {
        return globalRestarts;
    }

    public void setGlobalRestarts(int value) {
        globalRestarts = value;
    }

    public boolean getCustomSpellMixOnly() {
        return customSpellMixOnly;
    }

    public void setCustomSpellMixOnly(boolean value) {
        customSpellMixOnly = value;
    }

    public float getMeleeDps() {
        return meleeDps;
    }

    public void setMeleeDps(float value) {
        meleeDps = value;
    }

    public float getMeleeCrit() {
        return meleeCrit;
    }

    public void setMeleeCrit(float value) {
        meleeCrit = value;
    }

    public float getMeleeDot() {
        return meleeDot;
    }

    public void setMeleeDot(float value) {
        meleeDot = value;
    }

    public float getPhysicalDps() {
        return physicalDps;
    }

    public void setPhysicalDps(float value) {
        physicalDps = value;
    }

    public float getPhysicalCrit() {
        return physicalCrit;
    }

    public void setPhysicalCrit(float value) {
        physicalCrit = value;
    }

    public float getPhysicalDot() {
        return physicalDot;
    }

    public void setPhysicalDot(float value) {
        physicalDot = value;
    }

    public float getFireDps() {
        return fireDps;
    }

    public void setFireDps(float value) {
        fireDps = value;
    }

    public float getFireCrit() {
        return fireCrit;
    }

    public void setFireCrit(float value) {
        fireCrit = value;
    }

    public float getFireDot() {
        return fireDot;
    }

    public void setFireDot(float value) {
        fireDot = value;
    }

    public float getFrostDps() {
        return frostDps;
    }

    public void setFrostDps(float value) {
        frostDps = value;
    }

    public float getFrostCrit() {
        return frostCrit;
    }

    public void setFrostCrit(float value) {
        frostCrit = value;
    }

    public float getFrostDot() {
        return frostDot;
    }

    public void setFrostDot(float value) {
        frostDot = value;
    }

    public float getArcaneDps() {
        return arcaneDps;
    }

    public void setArcaneDps(float value) {
        arcaneDps = value;
    }

    public float getArcaneCrit() {
        return arcaneCrit;
    }

    public void setArcaneCrit(float value) {
        arcaneCrit = value;
    }

    public float getArcaneDot() {
        return arcaneDot;
    }

    public void setArcaneDot(float value) {
        arcaneDot = value;
    }

    public float getShadowDps() {
        return shadowDps;
    }

    public void setShadowDps(float value) {
        shadowDps = value;
    }

    public float getShadowCrit() {
        return shadowCrit;
    }

    public void setShadowCrit(float value) {
        shadowCrit = value;
    }

    public float getShadowDot() {
        return shadowDot;
    }

    public void setShadowDot(float value) {
        shadowDot = value;
    }

    public float getNatureDps() {
        return natureDps;
    }

    public void setNatureDps(float value) {
        natureDps = value;
    }

    public float getNatureCrit() {
        return natureCrit;
    }

    public void setNatureCrit(float value) {
        natureCrit = value;
    }

    public float getNatureDot() {
        return natureDot;
    }

    public void setNatureDot(float value) {
        natureDot = value;
    }

    public float getHolyDps() {
        return holyDps;
    }

    public void setHolyDps(float value) {
        holyDps = value;
    }

    public float getHolyCrit() {
        return holyCrit;
    }

    public void setHolyCrit(float value) {
        holyCrit = value;
    }

    public float getHolyDot() {
        return holyDot;
    }

    public void setHolyDot(float value) {
        holyDot = value;
    }

    public float getBurstWindow() {
        return burstWindow;
    }

    public void setBurstWindow(float value) {
        burstWindow = value;
    }

    public float getBurstImpacts() {
        return burstImpacts;
    }

    public void setBurstImpacts(float value) {
        burstImpacts = value;
    }

    public float getChanceToLiveScore() {
        return chanceToLiveScore;
    }

    public void setChanceToLiveScore(float value) {
        chanceToLiveScore = value;
    }

    public float getChanceToLiveAttenuation() {
        return chanceToLiveAttenuation;
    }

    public void setChanceToLiveAttenuation(float value) {
        chanceToLiveAttenuation = value;
    }

    public float getEffectSpiritMultiplier() {
        return effectSpiritMultiplier;
    }

    public void setEffectSpiritMultiplier(float value) {
        effectSpiritMultiplier = value;
    }

    public float getEffectCritBonus() {
        return effectCritBonus;
    }

    public void setEffectCritBonus(float value) {
        effectCritBonus = value;
    }

    public float getEffectCritDamageBonus() {
        return effectCritDamageBonus;
    }

    public void setEffectCritDamageBonus(float value) {
        effectCritDamageBonus = value;
    }

    public float getEffectDamageMultiplier() {
        return effectDamageMultiplier;
    }

    public void setEffectDamageMultiplier(float value) {
        effectDamageMultiplier = value;
    }

    public float getEffectHasteMultiplier() {
        return effectHasteMultiplier;
    }

    public void setEffectHasteMultiplier(float value) {
        effectHasteMultiplier = value;
    }

    public float getEffectCostMultiplier() {
        return effectCostMultiplier;
    }

    public void setEffectCostMultiplier(float value) {
        effectCostMultiplier = value;
    }

    public float getEffectRegenMultiplier() {
        return effectRegenMultiplier;
    }

    public void setEffectRegenMultiplier(float value) {
        effectRegenMultiplier = value;
    }

    public boolean getEffectDisableManaSources() {
        return effectDisableManaSources;
    }

    public void setEffectDisableManaSources(boolean value) {
        effectDisableManaSources = value;
    }

    public float getEffectShadowSilenceFrequency() {
        return effectShadowSilenceFrequency;
    }

    public void setEffectShadowSilenceFrequency(float value) {
        effectShadowSilenceFrequency = value;
    }

    public float getEffectShadowSilenceDuration() {
        return effectShadowSilenceDuration;
    }

    public void setEffectShadowSilenceDuration(float value) {
        effectShadowSilenceDuration = value;
    }

    public float getEffectShadowManaDrainFrequency() {
        return effectShadowManaDrainFrequency;
    }

    public void setEffectShadowManaDrainFrequency(float value) {
        effectShadowManaDrainFrequency = value;
    }

    public float getEffectShadowManaDrain() {
        return effectShadowManaDrain;
    }

    public void setEffectShadowManaDrain(float value) {
        effectShadowManaDrain = value;
    }

    public float getEffectArcaneOtherBinary() {
        return effectArcaneOtherBinary;
    }

    public void setEffectArcaneOtherBinary(float value) {
        effectArcaneOtherBinary = value;
    }

    public float getEffectFireOtherBinary() {
        return effectFireOtherBinary;
    }

    public void setEffectFireOtherBinary(float value) {
        effectFireOtherBinary = value;
    }

    public float getEffectFrostOtherBinary() {
        return effectFrostOtherBinary;
    }

    public void setEffectFrostOtherBinary(float value) {
        effectFrostOtherBinary = value;
    }

    public float getEffectShadowOtherBinary() {
        return effectShadowOtherBinary;
    }

    public void setEffectShadowOtherBinary(float value) {
        effectShadowOtherBinary = value;
    }

    public float getEffectNatureOtherBinary() {
        return effectNatureOtherBinary;
    }

    public void setEffectNatureOtherBinary(float value) {
        effectNatureOtherBinary = value;
    }

    public float getEffectHolyOtherBinary() {
        return effectHolyOtherBinary;
    }

    public void setEffectHolyOtherBinary(float value) {
        effectHolyOtherBinary = value;
    }

    public float getEffectArcaneOther() {
        return effectArcaneOther;
    }

    public void setEffectArcaneOther(float value) {
        effectArcaneOther = value;
    }

    public float getEffectFireOther() {
        return effectFireOther;
    }

    public void setEffectFireOther(float value) {
        effectFireOther = value;
    }

    public float getEffectFrostOther() {
        return effectFrostOther;
    }

    public void setEffectFrostOther(float value) {
        effectFrostOther = value;
    }

    public float getEffectShadowOther() {
        return effectShadowOther;
    }

    public void setEffectShadowOther(float value) {
        effectShadowOther = value;
    }

    public float getEffectNatureOther() {
        return effectNatureOther;
    }

    public void setEffectNatureOther(float value) {
        effectNatureOther = value;
    }

    public float getEffectHolyOther() {
        return effectHolyOther;
    }

    public void setEffectHolyOther(float value) {
        effectHolyOther = value;
    }

    private static final java.util.Map<Float, java.util.List<CooldownStackingCacheEntry>> cooldownStackingCacheMapNoMaxUseAssumption = new java.util.HashMap<Float, java.util.List<CooldownStackingCacheEntry>>();
    private static final java.util.Map<Float, java.util.List<CooldownStackingCacheEntry>> cooldownStackingCacheMap = new java.util.HashMap<Float, java.util.List<CooldownStackingCacheEntry>>();

    private int playerLevel;
    private float levelScalingFactor;
    private float fightDuration;
    private boolean maxUseAssumption;
    private java.util.List<CooldownStackingCacheEntry> cooldownStackingCache;
    private Character character;

    public CalculationOptionsMage() {
        setTargetLevel(83);
        setAoeTargetLevel(80);
        setLatencyCast(0.01f);
        setLatencyGCD(0.05f);
        setLatencyChannel(0.2f);
        setAoeTargets(9);
        setFightDuration(300);
        setHeroismAvailable(true);
        setMoltenFuryPercentage(0.3f);
        setPotionOfWildMagic(true);
        setPotionOfSpeed(true);
        setFlameCap(false);
        setDpsTime(1);
        setInterruptFrequency(0);
        setAoeDuration(0);
        setSmartOptimization(true);
        setDrumsOfBattle(false);
        setAutomaticArmor(false);
        setTpsLimit(0);
        setIncrementalOptimizations(true);
        setReconstructSequence(false);
        setInnervate(0);
        setManaTide(0);
        setFragmentation(0);
        setSurvivabilityRating(0.0001f);
        setAldor(true);
        setEvocationEnabled(true);
        setManaPotionEnabled(true);
        setManaGemEnabled(true);
        setMaxHeapLimit(300);
        setDrinkingTime(300);
        setBurstWindow(5f);
        setBurstImpacts(5f);
        setPlayerLevel(80);
        setFocusMagicTargetCritRate(0.2f);
        setSnaredTime(1f);
        setFixedSegmentDuration(30);
        setWarlockSpellPower(2800);
        setEffectSpiritMultiplier(1.0f);
        setEffectDamageMultiplier(1.0f);
        setEffectHasteMultiplier(1.0f);
        setEffectRegenMultiplier(1.0f);
        setEffectCostMultiplier(1.0f);
        setChanceToLiveAttenuation(0.1f);
        setMaxUseAssumption(true);
        setFrostbiteUtilization(1.0f);
        setMirrorImageEnabled(true);
        setEnable2T10Evocation(true);
        setEncounter(new Encounter());
    }

    public CalculationOptionsMage(Character character) {
        this();
        setCharacter(character);
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public void setPlayerLevel(int value) {
        playerLevel = value;
        if (playerLevel <= 80) {
            levelScalingFactor = (float) ((52f / 82f) * Math.pow(63f / 131f, (playerLevel - 70) / 10f));
        } else {
            levelScalingFactor = (float) ((1638f / 5371f) * Math.pow(7f / 41f, (playerLevel - 80) / 5f));
        }
    }

    public float getLevelScalingFactor() {
        return levelScalingFactor;
    }

    public float getFightDuration() {
        return fightDuration;
    }

    public void setFightDuration(float value) {
        fightDuration = value;
        updateCooldownStackingCache();
    }

    public boolean getMaxUseAssumption() {
        return maxUseAssumption;
    }

    public void setMaxUseAssumption(boolean value) {
        maxUseAssumption = value;
        updateCooldownStackingCache();
    }

    public java.util.List<CooldownStackingCacheEntry> getCooldownStackingCache() {
        return cooldownStackingCache;
    }

    private void updateCooldownStackingCache() {
        java.util.Map<Float, java.util.List<CooldownStackingCacheEntry>> map = maxUseAssumption ? cooldownStackingCacheMap : cooldownStackingCacheMapNoMaxUseAssumption;
        synchronized (map) {
            java.util.List<CooldownStackingCacheEntry> cache = map.get(fightDuration);
            if (cache == null) {
                cache = new java.util.ArrayList<CooldownStackingCacheEntry>();
                map.put(fightDuration, cache);
            }
            cooldownStackingCache = cache;
        }
    }

    public String getShattrathFaction() {
        return aldor ? "Aldor" : "Scryers";
    }

    public void setShattrathFaction(String value) {
        aldor = value.equals("Aldor");
    }

    @Override
    public Character getCharacter() {
        return character;
    }

    @Override
    public void setCharacter(Character value) {
        if (value != null) {
            value.addCalculationsInvalidatedListener(this::characterItemsChanged);
        }
        character = value;
    }

    private void characterItemsChanged() {
        incrementalSetStateIndexes = null;
        incrementalSetSegments = null;
        incrementalSetSortedStates = null;
        incrementalSetSpells = null;
        incrementalSetManaSegment = null;
        incrementalSetVariableType = null;
        cooldownRestrictionList = null;
    }

    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1);
    }

    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsMage xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            String fieldName = field.getName();
            if (XML_IGNORED_FIELDS.contains(fieldName)) continue;
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(this);
            } catch (IllegalAccessException e) {
                continue;
            }
            appendXmlValue(sb, capitalize(fieldName), value);
        }
        sb.append("</CalculationOptionsMage>");
        return sb.toString();
    }

    private static void appendXmlValue(StringBuilder sb, String elementName, Object value) {
        if (value == null) return;
        sb.append('<').append(elementName).append('>');
        if (value instanceof java.util.List) {
            for (Object item : (java.util.List<?>) value) {
                appendXmlValue(sb, item.getClass().getSimpleName(), item);
            }
        } else if (value instanceof Boolean || value instanceof Number || value instanceof String || value instanceof Enum) {
            if (value instanceof Boolean) sb.append(((Boolean) value) ? "true" : "false");
            else if (value instanceof Float || value instanceof Double) sb.append(value.toString());
            else sb.append(value);
        } else if (!value.getClass().getName().startsWith("java.") && !value.getClass().getName().startsWith("javax.")) {
            for (java.lang.reflect.Field field : value.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    appendXmlValue(sb, capitalize(field.getName()), field.get(value));
                } catch (IllegalAccessException e) {
                    // skip
                }
            }
        }
        sb.append("</").append(elementName).append('>');
    }


    public DisplayCalculations calculations; // calculations that are result of the last display in rawr
    public Sequence sequenceReconstruction;

    public DisplayCalculations getCalculations() {
        return calculations;
    }

    public void setCalculations(DisplayCalculations value) {
        calculations = value;
    }

    public Sequence getSequenceReconstruction() {
        return this.sequenceReconstruction;
    }

    public void setSequenceReconstruction(Sequence value) {
        this.sequenceReconstruction = value;
    }

    public boolean cooldownRestrictionsValid(Segment segment, CastingState state) {
        if (cooldownRestrictionList == null) return true;
        for (CooldownRestriction restriction : cooldownRestrictionList) {
            if (segment.timeStart >= restriction.timeStart && segment.getTimeEnd() <= restriction.timeEnd) {
                if (!restriction.isMatch.test(state.getEffects())) return false;
            }
        }
        return true;
    }

    public float getDamageMultiplier(Segment segment) {
        float mult = 1.0f;
        Encounter encounter = getEncounter();
        if (encounter != null && getEncounterEnabled()) {
            // very naive for now, assume that exactly one target can apply
            for (DamageMultiplier multiplier : encounter.globalMultipliers) {
                float startTime, endTime;
                if (multiplier.relativeTime) {
                    startTime = multiplier.startTime * getFightDuration();
                    endTime = multiplier.endTime * getFightDuration();
                } else {
                    startTime = multiplier.startTime;
                    endTime = multiplier.endTime;
                }
                if (segment.timeStart >= startTime && segment.getTimeEnd() <= endTime) {
                    mult *= multiplier.multiplier;
                }
            }
            boolean foundTarget = false;
            for (TargetGroup group : encounter.targetGroups) {
                float startTime, endTime;
                if (group.relativeTime) {
                    startTime = group.entranceTime * getFightDuration();
                    endTime = group.exitTime * getFightDuration();
                } else {
                    startTime = group.entranceTime;
                    endTime = group.exitTime;
                }
                if (segment.timeStart >= startTime && segment.getTimeEnd() <= endTime) {
                    foundTarget = true;
                    for (DamageMultiplier multiplier : group.multipliers) {
                        float t1, t2;
                        if (multiplier.relativeTime) {
                            t1 = startTime + multiplier.startTime * (endTime - startTime);
                            t2 = startTime + multiplier.endTime * (endTime - startTime);
                        } else {
                            t1 = startTime + multiplier.startTime;
                            t2 = startTime + multiplier.endTime;
                        }
                        if (segment.timeStart >= t1 && segment.getTimeEnd() <= t2) {
                            mult *= multiplier.multiplier;
                        }
                    }
                    break;
                }
            }
            if (!foundTarget) {
                mult = 0.0f;
            }
        }
        return mult;
    }
}

