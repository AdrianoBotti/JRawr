// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: .NET float/double default "G" ToString is reproduced by netFloatToString/netDoubleToString
// so display strings match the C# golden output (Java's Float.toString would print "60.0" where .NET prints "60").
package rawr.models.mage;

import static rawr.models.mage.NetFormat.*;

import rawr.base.talents.MageTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DisplayCalculations {
    //public CalculationsMage Calculations { get; set; }
    private Stats baseStats;

    public Stats getBaseStats() {
        return baseStats;
    }

    public void setBaseStats(Stats value) {
        baseStats = value;
    }

    private CastingState baseState;

    public CastingState getBaseState() {
        return baseState;
    }

    public void setBaseState(CastingState value) {
        baseState = value;
    }

    private CalculationOptionsMage calculationOptions;

    public CalculationOptionsMage getCalculationOptions() {
        return calculationOptions;
    }

    public void setCalculationOptions(CalculationOptionsMage value) {
        calculationOptions = value;
    }

    private MageTalents mageTalents;

    public MageTalents getMageTalents() {
        return mageTalents;
    }

    public void setMageTalents(MageTalents value) {
        mageTalents = value;
    }

    private rawr.base.character.Character character;

    public rawr.base.character.Character getCharacter() {
        return character;
    }

    public void setCharacter(rawr.base.character.Character value) {
        character = value;
    }

    private List<EffectCooldown> cooldownList;

    public List<EffectCooldown> getCooldownList() {
        return cooldownList;
    }

    public void setCooldownList(List<EffectCooldown> value) {
        cooldownList = value;
    }

    private Map<Integer, EffectCooldown> effectCooldown;

    public Map<Integer, EffectCooldown> getEffectCooldown() {
        return effectCooldown;
    }

    public void setEffectCooldown(Map<Integer, EffectCooldown> value) {
        effectCooldown = value;
    }

    private EffectCooldown[] itemBasedEffectCooldowns;

    public EffectCooldown[] getItemBasedEffectCooldowns() {
        return itemBasedEffectCooldowns;
    }

    public void setItemBasedEffectCooldowns(EffectCooldown[] value) {
        itemBasedEffectCooldowns = value;
    }

    private SpecialEffect[] spellPowerEffects;

    public SpecialEffect[] getSpellPowerEffects() {
        return spellPowerEffects;
    }

    public void setSpellPowerEffects(SpecialEffect[] value) {
        spellPowerEffects = value;
    }

    private SpecialEffect[] hasteRatingEffects;

    public SpecialEffect[] getHasteRatingEffects() {
        return hasteRatingEffects;
    }

    public void setHasteRatingEffects(SpecialEffect[] value) {
        hasteRatingEffects = value;
    }

    private List<Segment> segmentList;

    public List<Segment> getSegmentList() {
        return segmentList;
    }

    public void setSegmentList(List<Segment> value) {
        segmentList = value;
    }

    private float baseGlobalCooldown;

    public float getBaseGlobalCooldown() {
        return baseGlobalCooldown;
    }

    public void setBaseGlobalCooldown(float value) {
        baseGlobalCooldown = value;
    }

    private float startingMana;

    public float getStartingMana() {
        return startingMana;
    }

    public void setStartingMana(float value) {
        startingMana = value;
    }

    private String mageArmor;

    public String getMageArmor() {
        return mageArmor;
    }

    public void setMageArmor(String value) {
        mageArmor = value;
    }

    private boolean manaGemEffect;

    public boolean getManaGemEffect() {
        return manaGemEffect;
    }

    public void setManaGemEffect(boolean value) {
        manaGemEffect = value;
    }

    private float manaAdeptBonus;

    public float getManaAdeptBonus() {
        return manaAdeptBonus;
    }

    public void setManaAdeptBonus(float value) {
        manaAdeptBonus = value;
    }

    private float rawArcaneHitRate;

    public float getRawArcaneHitRate() {
        return rawArcaneHitRate;
    }

    public void setRawArcaneHitRate(float value) {
        rawArcaneHitRate = value;
    }

    private float rawFireHitRate;

    public float getRawFireHitRate() {
        return rawFireHitRate;
    }

    public void setRawFireHitRate(float value) {
        rawFireHitRate = value;
    }

    private float rawFrostHitRate;

    public float getRawFrostHitRate() {
        return rawFrostHitRate;
    }

    public void setRawFrostHitRate(float value) {
        rawFrostHitRate = value;
    }

    public float evocationDuration;
    public float evocationRegen;
    public float evocationDurationIV;
    public float evocationRegenIV;
    public float evocationDurationHero;
    public float evocationRegenHero;
    public float evocationDurationIVHero;
    public float evocationRegenIVHero;

    public int maxManaGem;
    public float maxEvocation;
    public float manaGemTps;
    public float manaPotionTps;
    public float manaGemValue;
    public float maxManaGemValue;
    public float manaPotionValue;
    public float maxManaPotionValue;

    public float combustionCooldown;
    public float powerInfusionDuration;
    public float powerInfusionCooldown;
    public float mirrorImageDuration;
    public float mirrorImageCooldown;
    public float icyVeinsCooldown;
    public float coldsnapCooldown;
    public float arcanePowerCooldown;
    public float arcanePowerDuration;
    public float waterElementalCooldown;
    public float waterElementalDuration;
    public float evocationCooldown;
    public float manaGemEffectDuration;

    public double[] solution;
    public List<SolutionVariable> solutionVariable;
    public float tps;
    public double upperBound = Double.POSITIVE_INFINITY;
    public double lowerBound = 0;

    private Cycle conjureManaGem;

    public Cycle getConjureManaGem() {
        return conjureManaGem;
    }

    public void setConjureManaGem(Cycle value) {
        conjureManaGem = value;
    }

    private int maxConjureManaGem;

    public int getMaxConjureManaGem() {
        return maxConjureManaGem;
    }

    public void setMaxConjureManaGem(int value) {
        maxConjureManaGem = value;
    }

    private Cycle ward;

    public Cycle getWard() {
        return ward;
    }

    public void setWard(Cycle value) {
        ward = value;
    }

    private int maxWards;

    public int getMaxWards() {
        return maxWards;
    }

    public void setMaxWards(int value) {
        maxWards = value;
    }

    private Spell wand;

    public Spell getWand() {
        return wand;
    }

    public void setWand(Spell value) {
        wand = value;
    }

    private float chanceToDie;

    public float getChanceToDie() {
        return chanceToDie;
    }

    public void setChanceToDie(float value) {
        chanceToDie = value;
    }

    private float meanIncomingDps;

    public float getMeanIncomingDps() {
        return meanIncomingDps;
    }

    public void setMeanIncomingDps(float value) {
        meanIncomingDps = value;
    }

    private float damageTakenReduction;

    public float getDamageTakenReduction() {
        return damageTakenReduction;
    }

    public void setDamageTakenReduction(float value) {
        damageTakenReduction = value;
    }

    public String ReconstructSequence() {
        calculationOptions.setSequenceReconstruction(null);
        calculationOptions.setAdviseAdvancedSolver(false);
        if (!calculationOptions.getReconstructSequence()) return "*Disabled";
        if (calculationOptions.getFightDuration() > 900) return "*Unavailable";

        StringBuilder timing = new StringBuilder();
        double bestUnexplained = Double.POSITIVE_INFINITY;
        String bestTiming = "*";

        SequenceItem.calculations = this;
        double unexplained;
        Sequence sequence = generateRawSequence(false);
        if (!sequence.sortGroups(displaySolver)) {
            //sequence = GenerateRawSequence(true);
            //sequence.SortGroups(displaySolver);
        }
        for (SequenceItem item : sequence.sequence) {
            item.setMinTime(segmentList.get(item.getSegment()).getTimeStart());
            item.setMaxTime(segmentList.get(item.getSegment()).getTimeEnd() - item.duration);
        }

        // mana gem/pot/evo positioning
        if (calculationOptions.getCooldownRestrictionList() == null && !calculationOptions.getEncounterEnabled()) {
            sequence.repositionManaConsumption();
        }

        sequence.removeIndex(VariableType.TimeExtension);
        sequence.compact(true);
        if (displaySolver == null || CalculationsMage.isSolverEnabled(displaySolver)) {
            calculationOptions.setSequenceReconstruction(sequence);
        }

        // evaluate sequence
        unexplained = sequence.evaluate(timing, Sequence.EvaluationMode.Unexplained);
        if (unexplained < bestUnexplained) {
            bestUnexplained = unexplained;
            bestTiming = timing.toString();
        }

        if (unexplained > 0 && !(calculationOptions.getDisplaySegmentCooldowns() && calculationOptions.getDisplayIntegralMana() && calculationOptions.getDisplaySegmentMana() && calculationOptions.getDisplayAdvancedConstraintsLevel() >= 5)) {
            calculationOptions.setAdviseAdvancedSolver(true);
            bestTiming = "*Sequence Reconstruction was not fully successful, it is recommended that you enable more options in\r\nadvanced solver (segment cooldowns, segment mana, integral mana consumables, advanced constraints options)!\r\n\r\n" + bestTiming.replaceFirst("^\\*+", "");
        }

        return bestTiming;
    }

    private Sequence generateRawSequence(boolean ignoreSegments) {
        Sequence sequence = new Sequence();

        double totalTime = 0.0;
        double totalGem = 0.0;
        int columnIdleRegen = 0;
        int columnManaOverflow = -1;
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solutionVariable.get(i).type == VariableType.IdleRegen) {
                columnIdleRegen = i;
                break;
            }
        }
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solutionVariable.get(i).type == VariableType.ManaGem) {
                sequence.ColumnManaGem = i;
                break;
            }
        }
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solutionVariable.get(i).type == VariableType.ManaPotion) {
                sequence.ColumnManaPotion = i;
                break;
            }
        }
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solutionVariable.get(i).type == VariableType.ManaOverflow) {
                columnManaOverflow = i;
                break;
            }
        }
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solution[i] > 0.01 && solutionVariable.get(i).type != VariableType.ManaOverflow) {
                SequenceItem item = new SequenceItem(i, solution[i]);
                if (ignoreSegments) item.setSegment(0);
                sequence.add(item);
                if (!item.isManaPotionOrGem()) totalTime += item.duration;
                if (item.getVariableType() == VariableType.ManaGem) totalGem += solution[i];
            }
        }
        if (calculationOptions.getTargetDamage() == 0.0 && totalTime < calculationOptions.getFightDuration() - 0.00001) {
            sequence.add(new SequenceItem(columnIdleRegen, calculationOptions.getFightDuration() - totalTime));
        }

        // evaluate sequence

        /*unexplained = sequence.Evaluate(timing, Sequence.EvaluationMode.Unexplained);
        if (unexplained < bestUnexplained)
        {
            bestUnexplained = unexplained;
            bestTiming = timing.ToString();
        }*/

        sequence.groupMoltenFury();
        SequenceGroup heroismGroup = sequence.groupHeroism();
        if (calculationOptions.getHeroismControl() == 3) {
            heroismGroup.setMinTime(Math.min(calculationOptions.getFightDuration() - calculationOptions.getMoltenFuryPercentage() * calculationOptions.getFightDuration(), calculationOptions.getFightDuration() - 40.0));
        }
        sequence.groupCombustion();
        sequence.groupArcanePower();
        sequence.groupPotionOfWildMagic();
        sequence.groupPotionOfSpeed();
        for (EffectCooldown cooldown : itemBasedEffectCooldowns) {
            sequence.groupSpecialEffect(cooldown);
        }
        List<SequenceGroup> list = sequence.groupManaGemEffect();
        if (list != null && manaGemEffect && calculationOptions.getDisplaySegmentCooldowns() && columnManaOverflow != -1) {
            float manaBurn = 0;
            for (int i = 0; i < solutionVariable.size(); i++) {
                if (solution[i] > 0.01 && solutionVariable.get(i).segment == 0 && solutionVariable.get(i).type == VariableType.Spell) {
                    CastingState state = solutionVariable.get(i).state;
                    if (state != null && !state.effectsActive(StandardEffect.ManaGemEffect)) {
                        float burn = solutionVariable.get(i).cycle.getManaPerSecond();
                        if (burn > manaBurn) manaBurn = burn;
                    }
                }
            }

            double overflow = solution[columnManaOverflow];
            double tmin = 0;
            if (manaBurn > 0) tmin = (manaGemValue * (1 + baseStats.getBonusManaGem()) - overflow) / manaBurn;

            for (SequenceGroup g : list) {
                if (g.getSegment() == 0) g.setMinTime(tmin);
            }
        }
        sequence.groupIcyVeins(); // should come after trinkets because of coldsnap
        sequence.groupWaterElemental();
        sequence.groupMirrorImage();
        sequence.groupBerserking();
        list = sequence.groupFlameCap();
        // very very special case for now
        if (list != null && list.size() == 2 && calculationOptions.getFightDuration() < 400 && totalGem >= 1) {
            for (SequenceGroup group : list) {
                for (CooldownConstraint constraint : group.constraint) {
                    if (constraint.effectCooldown.standardEffect == StandardEffect.FlameCap) {
                        constraint.cooldown = 300.0;
                    }
                }
            }
        }
        sequence.groupEvocation();
        return sequence;
    }

    private String getHitRatingDescription(float hitRate) {
        float diff = (hitRate - 1) * 800 / calculationOptions.getLevelScalingFactor();
        if (diff >= 1) {
            return " (" + (int) Math.floor(diff) + " hit rating over cap)";
        } else if (diff <= -1) {
            return " (" + (int) Math.ceil(-diff) + " hit rating to cap)";
        }
        return "";
    }

    public float getMinimumRange() {
        float minRange = Float.POSITIVE_INFINITY;
        for (SpellContribution contrib : damageSources.values()) {
            if (contrib.range > 0 && contrib.range < minRange) {
                minRange = contrib.range;
            }
        }
        return minRange;
    }

    public float getThreatReduction() {
        return 1 - tps / baseCalculations.getDpsRating();
    }

    private Solver displaySolver;

    public Solver getDisplaySolver() {
        return displaySolver;
    }

    public void setDisplaySolver(Solver value) {
        displaySolver = value;
    }

    private Map<String, String> displayCalculationValues;

    public Map<String, String> getDisplayCalculationValues() {
        return displayCalculationValues;
    }

    private Map<String, SpellContribution> damageSources;

    public Map<String, SpellContribution> getDamageSources() {
        return damageSources;
    }

    private Map<String, Float> manaSources;

    public Map<String, Float> getManaSources() {
        return manaSources;
    }

    private Map<String, Float> manaUsage;

    public Map<String, Float> getManaUsage() {
        return manaUsage;
    }

    private CharacterCalculationsMage baseCalculations;

    public CharacterCalculationsMage getBaseCalculations() {
        return baseCalculations;
    }

    public void setBaseCalculations(CharacterCalculationsMage value) {
        baseCalculations = value;
    }

    public Map<String, String> getCharacterDisplayCalculationValues(boolean computeReconstruction) {
        Map<String, String> dictValues = displayCalculationValues = new LinkedHashMap<String, String>();
        dictValues.put("Stamina", netFloatToString(baseStats.getStamina()));
        dictValues.put("Intellect", netFloatToString(baseStats.getIntellect()));
        dictValues.put("Spirit", netFloatToString(baseStats.getSpirit()));
        dictValues.put("Armor", netFloatToString(baseStats.getArmor()));
        dictValues.put("Health", netFloatToString(baseStats.getHealth()));
        dictValues.put("Mana", netFloatToString(baseStats.getMana()));
        dictValues.put("Crit Rate", String.format(Locale.ROOT, "%.2f%%*%s Crit Rating", 100 * Math.max(0, baseState.getCritRate()), netFloatToString(baseStats.getCritRating())));
        float levelScalingFactor = calculationOptions.getLevelScalingFactor();
        // hit rating = hitrate * 800 / levelScalingFactor
        dictValues.put("Hit Rate", String.format(Locale.ROOT, "%.2f%%*%s Hit Rating\r\nArcane\t%.2f%%%s\r\nFire\t%.2f%%%s\r\nFrost\t%.2f%%%s", 100 * baseState.getSpellHit(), netFloatToString(baseStats.getHitRating()), 100 * baseState.getArcaneHitRate(), getHitRatingDescription(rawArcaneHitRate), 100 * baseState.getFireHitRate(), getHitRatingDescription(rawFireHitRate), 100 * baseState.getFrostHitRate(), getHitRatingDescription(rawFrostHitRate)));
        dictValues.put("Spell Penetration", netFloatToString(baseStats.getSpellPenetration()));
        dictValues.put("Haste", String.format(Locale.ROOT, "%.2f%%*%s Haste Rating", 100 * (baseState.castingSpeed - 1f), netFloatToString(baseState.getSpellHasteRating())));
        dictValues.put("Arcane Damage", netFloatToString(baseState.getArcaneSpellPower()));
        dictValues.put("Fire Damage", netFloatToString(baseState.getFireSpellPower()));
        dictValues.put("Frost Damage", netFloatToString(baseState.getFrostSpellPower()));
        dictValues.put("MP5", netFloatToString(baseStats.getMp5()));
        dictValues.put("Mana Regen", netDoubleToString(Math.floor(baseState.getManaRegen() * 5)) + String.format(Locale.ROOT, "*Mana Regen in 5SR: %s\r\nMana Regen Drinking: %s", netDoubleToString(Math.floor(baseState.getManaRegen5SR() * 5)), netDoubleToString(Math.floor(baseState.getManaRegenDrinking() * 5))));
        dictValues.put("Health Regen", netDoubleToString(Math.floor(baseState.getHealthRegenCombat() * 5)) + String.format(Locale.ROOT, "*Health Regen Eating: %s", netDoubleToString(Math.floor(baseState.getHealthRegenEating() * 5))));
        dictValues.put("Arcane Resist", netFloatToString(baseStats.getArcaneResistance()));
        dictValues.put("Fire Resist", netFloatToString(baseStats.getFireResistance()));
        dictValues.put("Nature Resist", netFloatToString(baseStats.getNatureResistance()));
        dictValues.put("Frost Resist", netFloatToString(baseStats.getFrostResistance()));
        dictValues.put("Shadow Resist", netFloatToString(baseStats.getShadowResistance()));
        dictValues.put("Physical Mitigation", String.format(Locale.ROOT, "%.2f%%", 100 * baseState.getMeleeMitigation()));
        dictValues.put("Resilience", String.format(Locale.ROOT, "%.2f*%.2f Damage Taken Reduction", baseStats.getResilience(), damageTakenReduction));
        dictValues.put("Defense", netFloatToString(baseState.getDefense()));
        dictValues.put("Crit Reduction", String.format(Locale.ROOT, "%1$.2f%%*Spell Crit Reduction: %1$.2f%%\r\nPhysical Crit Reduction: %2$.2f%%\r\nCrit Damage Reduction: %3$.2f%%", baseState.getSpellCritReduction() * 100, baseState.getPhysicalCritReduction() * 100, baseState.getCritDamageReduction() * 100));
        dictValues.put("Dodge", String.format(Locale.ROOT, "%.2f%%", 100 * baseState.getDodge()));
        dictValues.put("Chance to Die", String.format(Locale.ROOT, "%.2f%%", 100 * chanceToDie));
        dictValues.put("Mean Incoming Dps", String.format(Locale.ROOT, "%.2f", meanIncomingDps));
        List<CycleId> cycleList = new ArrayList<CycleId>(Arrays.asList(CycleId.FBPyro, CycleId.FFBPyro, CycleId.FBScPyro, CycleId.FFBScPyro, CycleId.FBLBPyro, CycleId.FrBFB, CycleId.FrBIL, CycleId.FrBILFB, CycleId.FBScLBPyro, CycleId.ScLBPyro, CycleId.FFBLBPyro, CycleId.FFBScLBPyro, CycleId.FrBFBIL, CycleId.ABSpam04MBAM, CycleId.ABSpam024MBAM, CycleId.ABSpam0234MBAM, CycleId.AB4AM0234MBAM, CycleId.AB3AM023MBAM, CycleId.AB2AM, CycleId.FrBDFFBIL, CycleId.ABSpam034MBAM, CycleId.FrBDFFFB));
        List<SpellId> spellList = new ArrayList<SpellId>(Arrays.asList(SpellId.ArcaneMissiles, SpellId.ArcaneBarrage, SpellId.Scorch, SpellId.Fireball, SpellId.Pyroblast, SpellId.FrostboltFOF, SpellId.FireBlast, SpellId.ArcaneExplosion, SpellId.FlamestrikeSingle, SpellId.Blizzard, SpellId.BlastWave, SpellId.DragonsBreath, SpellId.ConeOfCold, SpellId.FrostfireBoltFOF, SpellId.LivingBomb, SpellId.IceLance));
        for (CycleId cycle : cycleList) {
            Cycle s = baseState.getCycle(cycle);
            if (s != null) {
                dictValues.put(s.name, String.format(Locale.ROOT, "%1$s Dps*%2$s Mps\r\n%3$s Tps\r\n%9$s Dps per Spell Power\r\n%4$s Cast Procs / Sec\r\n%5$s Hit Procs / Sec\r\n%8$s Crit Procs / Sec\r\n%6$s Damage Procs / Sec\r\n%7$s Dot Procs / Sec", f2(s.getDamagePerSecond()), f2(s.getManaPerSecond()), f2(s.getThreatPerSecond()), f2(s.castProcs / s.castTime), f2(s.hitProcs / s.castTime), f2(s.damageProcs / s.castTime), f2(s.dotProcs / s.castTime), f2(s.critProcs / s.castTime), f2(s.dpsPerSpellPower)));
            }
        }
        Spell bs;
        String spellFormatString = "%1$s Dps*%2$s Mps\r\n%3$s Tps\r\n%4$s sec\r\n%14$s Mana\r\n%9$s - %10$s Hit\r\n%11$s - %12$s Crit%13$s\r\n%5$sx Amplify\r\n%6$s%% Crit Rate\r\n%7$s%% Hit Rate\r\n%8$s Crit Multiplier";
        for (SpellId spell : spellList) {
            bs = baseState.getSpell(spell);
            if (bs != null) {
                dictValues.put(bs.getName(), String.format(Locale.ROOT, spellFormatString, f2(bs.getCycle().getDamagePerSecond()), f2(bs.getCycle().getManaPerSecond()), f2(bs.getThreatPerSecond()), f2(bs.castTime - bs.getLatency()), f2(bs.spellModifier), f2(bs.critRate * 100), f2(bs.getHitRate() * 100), f2(bs.critBonus), f2(bs.getMinHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMinCritDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxCritDamage() * (1 + manaAdeptBonus)), ((bs.getDotDamage() > 0) ? ("\n" + f2(bs.getDotDamage() * (1 + manaAdeptBonus)) + " Dot") : ""), f2(bs.getCost())));
            }
        }
        if (wand != null) {
            dictValues.put(wand.getName(), String.format(Locale.ROOT, spellFormatString, f2(wand.getCycle().getDamagePerSecond()), f2(wand.getCycle().getManaPerSecond()), f2(wand.getThreatPerSecond()), f2(wand.castTime - wand.getLatency()), f2(wand.spellModifier), f2(wand.critRate * 100), f2(wand.getHitRate() * 100), f2(wand.critBonus), f2(wand.getMinHitDamage()), f2(wand.getMaxHitDamage()), f2(wand.getMinCritDamage()), f2(wand.getMaxCritDamage()), ((wand.getDotDamage() > 0) ? ("\n" + f2(wand.getDotDamage()) + " Dot") : ""), f2(wand.getCost())));
        }
        bs = baseState.getSpell(SpellId.ArcaneBlast0);
        dictValues.put("Arcane Blast(0)", String.format(Locale.ROOT, spellFormatString, f2(bs.getCycle().getDamagePerSecond()), f2(bs.getCycle().getManaPerSecond()), f2(bs.getThreatPerSecond()), f2(bs.castTime - bs.getLatency()), f2(bs.spellModifier), f2(bs.critRate * 100), f2(bs.getHitRate() * 100), f2(bs.critBonus), f2(bs.getMinHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMinCritDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxCritDamage() * (1 + manaAdeptBonus)), ((bs.getDotDamage() > 0) ? ("\n" + f2(bs.getDotDamage() * (1 + manaAdeptBonus)) + " Dot") : ""), f2(bs.getABCost())));
        bs = baseState.getSpell(SpellId.ArcaneBlast4);
        dictValues.put("Arcane Blast(4)", String.format(Locale.ROOT, spellFormatString, f2(bs.getCycle().getDamagePerSecond()), f2(bs.getCycle().getManaPerSecond()), f2(bs.getThreatPerSecond()), f2(bs.castTime - bs.getLatency()), f2(bs.spellModifier), f2(bs.critRate * 100), f2(bs.getHitRate() * 100), f2(bs.critBonus), f2(bs.getMinHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMinCritDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxCritDamage() * (1 + manaAdeptBonus)), ((bs.getDotDamage() > 0) ? ("\n" + f2(bs.getDotDamage() * (1 + manaAdeptBonus)) + " Dot") : ""), f2(bs.getABCost())));
        bs = baseState.getFrozenState().getSpell(SpellId.DeepFreeze);
        dictValues.put("Deep Freeze", String.format(Locale.ROOT, spellFormatString, f2(bs.getCycle().getDamagePerSecond()), f2(bs.getCycle().getManaPerSecond()), f2(bs.getThreatPerSecond()), f2(bs.castTime - bs.getLatency()), f2(bs.spellModifier), f2(bs.critRate * 100), f2(bs.getHitRate() * 100), f2(bs.critBonus), f2(bs.getMinHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMinCritDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxCritDamage() * (1 + manaAdeptBonus)), ((bs.getDotDamage() > 0) ? ("\n" + f2(bs.getDotDamage() * (1 + manaAdeptBonus)) + " Dot") : ""), f2(bs.getCost())));
        bs = baseState.getSpell(SpellId.ArcaneMissilesMB);
        dictValues.put("MBAM", String.format(Locale.ROOT, spellFormatString, f2(bs.getCycle().getDamagePerSecond()), f2(bs.getCycle().getManaPerSecond()), f2(bs.getThreatPerSecond()), f2(bs.castTime - bs.getLatency()), f2(bs.spellModifier), f2(bs.critRate * 100), f2(bs.getHitRate() * 100), f2(bs.critBonus), f2(bs.getMinHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxHitDamage() * (1 + manaAdeptBonus)), f2(bs.getMinCritDamage() * (1 + manaAdeptBonus)), f2(bs.getMaxCritDamage() * (1 + manaAdeptBonus)), ((bs.getDotDamage() > 0) ? ("\n" + f2(bs.getDotDamage() * (1 + manaAdeptBonus)) + " Dot") : ""), f2(bs.getCost())));
        Spell abss = baseState.getSpell(SpellId.FireWard);
        dictValues.put("Fire Ward", String.format(Locale.ROOT, "%.2f Absorb*%.2f Mps\r\nAverage Cast Time: %.2f\r\n%.2f Mana", abss.absorb, abss.getCycle().getManaPerSecond(), abss.castTime - abss.getLatency(), abss.getABCost()));
        abss = baseState.getSpell(SpellId.FrostWard);
        dictValues.put("Frost Ward", String.format(Locale.ROOT, "%.2f Absorb*%.2f Mps\r\nAverage Cast Time: %.2f\r\n%.2f Mana", abss.absorb, abss.getCycle().getManaPerSecond(), abss.castTime - abss.getLatency(), abss.getABCost()));
        float totalDamage = (calculationOptions.getTargetDamage() > 0.0f) ? calculationOptions.getTargetDamage() : baseCalculations.getDpsRating() * calculationOptions.getFightDuration();
        dictValues.put("Total Damage", String.format(Locale.ROOT, "%s*Upper Bound: %s\r\nLower Bound: %s", f2(totalDamage), d2(upperBound), d2(lowerBound)));
        dictValues.put("Score", f2(baseCalculations.getOverallPoints()));
        dictValues.put("Dps", f2(baseCalculations.getDpsRating()));
        dictValues.put("Tps", f2(tps));
        dictValues.put("Status", String.format(Locale.ROOT, "Score: %s, Dps: %s, Survivability: %s", f2(baseCalculations.getOverallPoints()), f2(baseCalculations.getDpsRating()), f2(baseCalculations.getSurvivabilityRating())));
        dictValues.put("Sequence", computeReconstruction ? ReconstructSequence() : "...");
        StringBuilder sb = new StringBuilder("*");
        if (mageArmor != null) sb.append(mageArmor).append("\n");
        Map<String, Double> combinedSolution = new LinkedHashMap<String, Double>();
        Map<String, Integer> combinedSolutionData = new LinkedHashMap<String, Integer>();
        double idleRegen = 0;
        double evocation = 0;
        double evocationIV = 0;
        double evocationHero = 0;
        double evocationIVHero = 0;
        double manaPotion = 0;
        double manaGem = 0;
        double drums = 0;
        double we = 0;
        double mi = 0;
        double cmg = 0;
        double ward = 0;
        boolean segmentedOutput = CharacterCalculationsMage.debugCooldownSegmentation;
        damageSources = new LinkedHashMap<String, SpellContribution>();
        manaSources = new LinkedHashMap<String, Float>();
        manaUsage = new LinkedHashMap<String, Float>();
        manaSources.put("Initial Mana", startingMana);
        manaSources.put("Replenishment", 0.0f);
        manaSources.put("Mana Gem", 0.0f);
        manaSources.put("Mana Potion", 0.0f);
        manaSources.put("MP5", 0.0f);
        manaSources.put("Intellect/Spirit", 0.0f);
        manaSources.put("Evocation", 0.0f);
        manaSources.put("Judgement of Wisdom", 0.0f);
        manaSources.put("Innervate", 0.0f);
        manaSources.put("Mana Tide", 0.0f);
        manaSources.put("Drinking", 0.0f);
        manaSources.put("Water Elemental", 0.0f);
        manaSources.put("Other", 0.0f);
        manaUsage.put("Overflow", 0.0f);
        manaUsage.put("Summon Water Elemental", 0.0f);
        manaUsage.put("Summon Mirror Image", 0.0f);
        float spiritFactor = 0.003345f;
        CastingState evoBaseState = baseState;
        if (calculationOptions.getEnable2T10Evocation() && baseStats.getMage2T10() > 0) {
            evoBaseState = baseState.getTier10TwoPieceState();
        }
        for (int i = 0; i < solutionVariable.size(); i++) {
            if (solution[i] > 0.01) {
                switch (solutionVariable.get(i).type) {
                    case IdleRegen:
                        idleRegen += solution[i];
                        // manaRegen = -(calculationResult.BaseState.ManaRegen * (1 - calculationOptions.Fragmentation) + calculationResult.BaseState.ManaRegen5SR * calculationOptions.Fragmentation);
                        // ManaRegen = SpiritRegen + characterStats.Mp5 / 5f + SpiritRegen * 4 * 20 * calculationOptions.Innervate / calculationOptions.FightDuration + calculationOptions.ManaTide * 0.24f * characterStats.Mana / calculationOptions.FightDuration + characterStats.ManaRestoreFromMaxManaPerSecond * characterStats.Mana;
                        // ManaRegen5SR = SpiritRegen * characterStats.SpellCombatManaRegeneration + characterStats.Mp5 / 5f + SpiritRegen * (5 - characterStats.SpellCombatManaRegeneration) * 20 * calculationOptions.Innervate / calculationOptions.FightDuration + calculationOptions.ManaTide * 0.24f * characterStats.Mana / calculationOptions.FightDuration + characterStats.ManaRestoreFromMaxManaPerSecond * characterStats.Mana;
                        if (!calculationOptions.getEffectDisableManaSources()) {
                            addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (baseState.getSpiritRegen() * (1 - calculationOptions.getFragmentation()) + baseState.getSpiritRegen() * baseStats.getSpellCombatManaRegeneration() * calculationOptions.getFragmentation()));
                            addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                            addFloat(manaSources, "Innervate", (float) solution[i] * ((15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()) * (1 - calculationOptions.getFragmentation()) + (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()) * calculationOptions.getFragmentation()));
                            addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                            addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        }
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2f sec", "Idle Regen", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    case Evocation:
                        evocation += solution[i];
                        //double evoManaRegen5SR = ((0.001f + BaseStats.Spirit * spiritFactor * (float)Math.Sqrt(BaseStats.Intellect)) * BaseStats.SpellCombatManaRegeneration + BaseStats.Mp5 / 5f + calculationResult.BaseState.SpiritRegen * (5 - characterStats.SpellCombatManaRegeneration) * 20 * calculationOptions.Innervate / calculationOptions.FightDuration + calculationOptions.ManaTide * 0.24f * characterStats.Mana / calculationOptions.FightDuration);
                        //double evocationRegen = evoManaRegen5SR + 0.15f * BaseStats.Mana / 2f * calculationResult.BaseState.CastingSpeed;
                        //calculationResult.EvocationRegenIV = evoManaRegen5SR + 0.15f * evocationMana / 2f * calculationResult.BaseState.CastingSpeed * 1.2;
                        //calculationResult.EvocationRegenHero = evoManaRegen5SR + 0.15f * evocationMana / 2f * calculationResult.BaseState.CastingSpeed * 1.3;
                        //calculationResult.EvocationRegenIVHero = evoManaRegen5SR + 0.15f * evocationMana / 2f * calculationResult.BaseState.CastingSpeed * 1.2 * 1.3;
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (0.001f + baseStats.getSpirit() * spiritFactor * (float) Math.sqrt(baseStats.getIntellect())) * baseStats.getSpellCombatManaRegeneration());
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaSources, "Evocation", (float) solution[i] * 0.15f * baseStats.getMana() / 2f * evoBaseState.castingSpeed);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Evocation", solution[i] / evocationDuration, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    case EvocationIV:
                        evocationIV += solution[i];
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (0.001f + baseStats.getSpirit() * spiritFactor * (float) Math.sqrt(baseStats.getIntellect())) * baseStats.getSpellCombatManaRegeneration());
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaSources, "Evocation", (float) solution[i] * 0.15f * baseStats.getMana() / 2f * evoBaseState.castingSpeed * 1.2f);
                        if (segmentedOutput) {
                            if (solutionVariable.get(i).state != null && solutionVariable.get(i).state.effectsActive(StandardEffect.IcyVeins)) {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2f", "Icy Veins+Evocation", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            } else {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Evocation (Icy Veins)", solution[i] / evocationDurationIV, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            }
                        }
                        break;
                    case EvocationHero:
                        evocationHero += solution[i];
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (0.001f + baseStats.getSpirit() * spiritFactor * (float) Math.sqrt(baseStats.getIntellect())) * baseStats.getSpellCombatManaRegeneration());
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaSources, "Evocation", (float) solution[i] * 0.15f * baseStats.getMana() / 2f * evoBaseState.castingSpeed * 1.3f);
                        if (segmentedOutput) {
                            if (solutionVariable.get(i).state != null && solutionVariable.get(i).state.effectsActive(StandardEffect.Heroism)) {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2f", "Heroism+Evocation", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            } else {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Evocation (Heroism)", solution[i] / evocationDurationHero, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            }
                        }
                        break;
                    case EvocationIVHero:
                        evocationIVHero += solution[i];
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (0.001f + baseStats.getSpirit() * spiritFactor * (float) Math.sqrt(baseStats.getIntellect())) * baseStats.getSpellCombatManaRegeneration());
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaSources, "Evocation", (float) solution[i] * 0.15f * baseStats.getMana() / 2f * evoBaseState.castingSpeed * 1.2f * 1.3f);
                        if (segmentedOutput) {
                            if (solutionVariable.get(i).state != null && solutionVariable.get(i).state.effectsActive(StandardEffect.IcyVeins | StandardEffect.Heroism)) {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2f", "Icy Veins+Heroism+Evocation", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            } else {
                                sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Evocation (Icy Veins+Heroism)", solution[i] / evocationDurationIVHero, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                            }
                        }
                        break;
                    case ManaPotion:
                        manaPotion += solution[i];
                        // (1 + characterStats.BonusManaPotion) * calculationResult.ManaPotionValue
                        addFloat(manaSources, "Mana Potion", (float) (solution[i] * (1 + baseStats.getBonusManaPotion()) * manaPotionValue));
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Mana Potion", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    case ManaGem:
                        manaGem += solution[i];
                        addFloat(manaSources, "Mana Gem", (float) (solution[i] * (1 + baseStats.getBonusManaGem()) * manaGemValue));
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Mana Gem", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    case Drinking:
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (baseState.getSpiritRegen()));
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        if (calculationOptions.getPlayerLevel() < 75) {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 240f);
                        } else if (calculationOptions.getPlayerLevel() < 80) {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 306f);
                        } else {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 432f);
                        }
                        sb.append(String.format(Locale.ROOT, "%s: %.2f sec", "Drinking", solution[i])).append("\n");
                        break;
                    case TimeExtension:
                        break;
                    case ManaOverflow:
                        addFloat(manaUsage, "Overflow", (float) solution[i]);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Mana Overflow", solution[i], segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    case AfterFightRegen:
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (baseState.getSpiritRegen()));
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        if (calculationOptions.getPlayerLevel() < 75) {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 240f);
                        } else if (calculationOptions.getPlayerLevel() < 80) {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 306f);
                        } else {
                            addFloat(manaSources, "Drinking", (float) solution[i] * 432f);
                        }
                        sb.append(String.format(Locale.ROOT, "%s: %.2f sec", "Drinking Regen", solution[i])).append("\n");
                        break;
                    case SummonWaterElemental:
                    {
                        we += solution[i];
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (baseState.getSpiritRegen() * baseStats.getSpellCombatManaRegeneration()));
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaUsage, "Summon Water Elemental", (float) solution[i] * (int) (0.16 * SpellTemplate.BaseMana.get(calculationOptions.getPlayerLevel())) / baseGlobalCooldown);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Summon Water Elemental", solution[i] / baseGlobalCooldown, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        Spell waterbolt = solutionVariable.get(i).state.getSpell(SpellId.Waterbolt);
                        SpellContribution contrib = damageSources.get(waterbolt.getName());
                        if (contrib == null) {
                            contrib = new SpellContribution();
                            contrib.name = waterbolt.getName();
                            damageSources.put(waterbolt.getName(), contrib);
                        }
                        contrib.hits += (float) solution[i] / waterbolt.castTime;
                        contrib.damage += waterbolt.getDamagePerSecond() * (float) solution[i];
                    }
                    break;
                    case SummonMirrorImage:
                    {
                        mi += solution[i];
                        addFloat(manaSources, "Intellect/Spirit", (float) solution[i] * (baseState.getSpiritRegen() * baseStats.getSpellCombatManaRegeneration()));
                        addFloat(manaSources, "MP5", (float) solution[i] * baseStats.getMp5() / 5f);
                        addFloat(manaSources, "Innervate", (float) solution[i] * (15732 * calculationOptions.getInnervate() / calculationOptions.getFightDuration()));
                        addFloat(manaSources, "Mana Tide", (float) solution[i] * calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / calculationOptions.getFightDuration());
                        addFloat(manaSources, "Replenishment", (float) solution[i] * baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana());
                        addFloat(manaUsage, "Summon Mirror Image", (float) solution[i] * (int) (0.10 * SpellTemplate.BaseMana.get(calculationOptions.getPlayerLevel())) / baseGlobalCooldown);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Summon Mirror Image", solution[i] / baseGlobalCooldown, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        Spell mirrorImage = solutionVariable.get(i).state.getSpell(SpellId.MirrorImage);
                        SpellContribution contrib = damageSources.get("Mirror Image");
                        if (contrib == null) {
                            contrib = new SpellContribution();
                            contrib.name = "Mirror Image";
                            damageSources.put("Mirror Image", contrib);
                        }
                        contrib.hits += 3 * (mageTalents.isGlyphOfMirrorImage() ? 4 : 3) * (float) solution[i] / mirrorImage.castTime;
                        contrib.damage += mirrorImage.getDamagePerSecond() * (float) solution[i];
                    }
                    break;
                    case ConjureManaGem:
                        cmg += solution[i];
                        Cycle smg = solutionVariable.get(i).cycle;
                        smg.addManaUsageContribution(manaUsage, (float) solution[i]);
                        smg.addManaSourcesContribution(manaSources, (float) solution[i]);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$.2fx", "Conjure Mana Gem", solution[i] / conjureManaGem.castTime, segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                    /*case VariableType.Ward:
                        ward += Solution[i];
                        Cycle sward = SolutionVariable[i].Cycle;
                        sward.AddManaUsageContribution(ManaUsage, (float)Solution[i]);
                        sward.AddManaSourcesContribution(ManaSources, (float)Solution[i]);
                        if (segmentedOutput) sb.AppendLine(String.Format("{2} {0}: {1:F}x", Ward.Name, Solution[i] / Ward.CastTime, SegmentList[SolutionVariable[i].Segment]));
                        break;*/
                    case Wand:
                    case Spell:
                        Cycle s = solutionVariable.get(i).cycle;
                        s.addDamageContribution(damageSources, (float) solution[i]);
                        s.addManaUsageContribution(manaUsage, (float) solution[i]);
                        s.addManaSourcesContribution(manaSources, (float) solution[i]);
                        String label = ((solutionVariable.get(i).state.getBuffLabel().length() > 0) ? (solutionVariable.get(i).state.getBuffLabel() + "+") : "") + s.name;
                        Double value = combinedSolution.get(label);
                        combinedSolution.put(label, (value != null ? value : 0.0) + solution[i]);
                        combinedSolutionData.put(label, i);
                        if (segmentedOutput) sb.append(String.format(Locale.ROOT, "%3$s.%4$d %1$s: %2$s sec", label, d2(solution[i]), segmentList.get(solutionVariable.get(i).segment), solutionVariable.get(i).manaSegment)).append("\n");
                        break;
                }
            }
        }
        if (!segmentedOutput) {
            if (idleRegen > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %s sec", "Idle Regen", d2(idleRegen))).append("\n");
            }
            if (evocation > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx (%s mps)", "Evocation", d2(evocation / evocationDuration), f2(evocationRegen))).append("\n");
            }
            if (evocationIV > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx (%s mps)", "Evocation (Icy Veins)", d2(evocationIV / evocationDurationIV), f2(evocationRegenIV))).append("\n");
            }
            if (evocationHero > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx (%s mps)", "Evocation (Heroism)", d2(evocationHero / evocationDurationHero), f2(evocationRegenHero))).append("\n");
            }
            if (evocationIVHero > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx (%s mps)", "Evocation (Icy Veins+Heroism)", d2(evocationIVHero / evocationDurationIVHero), f2(evocationRegenIVHero))).append("\n");
            }
            if (manaPotion > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Mana Potion", d2(manaPotion))).append("\n");
            }
            if (manaGem > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Mana Gem", d2(manaGem))).append("\n");
            }
            if (drums > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Drums of Battle", d2(drums / baseGlobalCooldown))).append("\n");
            }
            if (we > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Summon Water Elemental", d2(we / baseGlobalCooldown))).append("\n");
            }
            if (mi > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Summon Mirror Image", d2(mi / baseGlobalCooldown))).append("\n");
            }
            if (cmg > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", "Conjure Mana Gem", d2(cmg / conjureManaGem.castTime))).append("\n");
            }
            if (ward > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %sx", this.ward.name, d2(ward / this.ward.castTime))).append("\n");
            }
            for (Map.Entry<String, Double> kvp : combinedSolution.entrySet()) {
                Cycle s = solutionVariable.get(combinedSolutionData.get(kvp.getKey())).cycle;
                if (s != null) {
                    sb.append(String.format(Locale.ROOT, "%s: %s sec (%s dps, %s mps, %s tps)", kvp.getKey(), d2(kvp.getValue()), f2(s.getDamagePerSecond()), f2(s.getManaPerSecond()), f2(s.getThreatPerSecond()))).append("\n");
                } else {
                    sb.append(String.format(Locale.ROOT, "%s: %s sec", kvp.getKey(), d2(kvp.getValue()))).append("\n");
                }
            }
        }
        //if (WaterElemental) sb.AppendLine(String.Format("Water Elemental: {0:F}x", WaterElementalDuration / 45f));
        dictValues.put("Spell Cycles", sb.toString());
        sb = new StringBuilder("*");
        List<SpellContribution> contribList = new ArrayList<SpellContribution>(damageSources.values());
        java.util.Collections.sort(contribList);
        for (SpellContribution contrib : contribList) {
            if (contrib.hitDamage > 0) {
                sb.append(String.format(Locale.ROOT, "%s: %s%%, %s Damage\r\n",
                        contrib.name,
                        d2(100.0 * contrib.damage / totalDamage),
                        f2(contrib.damage)));
                sb.append(String.format(Locale.ROOT, "\tHits (#: %s, Avg: %s, Total: %s)\r\n",
                        f2(contrib.hits),
                        f2(contrib.hitDamage / contrib.hits),
                        f2(contrib.hitDamage)));
            } else {
                sb.append(String.format(Locale.ROOT, "%s: %s%%, %s Damage, %s Hits\r\n",
                        contrib.name,
                        d2(100.0 * contrib.damage / totalDamage),
                        f2(contrib.damage),
                        f2(contrib.hits)));
            }
            if (contrib.critDamage > 0) {
                sb.append(String.format(Locale.ROOT, "\tCrits (#: %s, Avg: %s, Total: %s)\r\n",
                        f2(contrib.crits),
                        f2(contrib.critDamage / contrib.crits),
                        f2(contrib.critDamage)));
            }
            if (contrib.tickDamage > 0) {
                sb.append(String.format(Locale.ROOT, "\tTicks (#: %s, Avg: %s, Total: %s)\r\n",
                        f2(contrib.ticks),
                        f2(contrib.tickDamage / contrib.ticks),
                        f2(contrib.tickDamage)));
            }
        }
        dictValues.put("By Spell", sb.toString());
        dictValues.put("Minimum Range", f2(getMinimumRange()));
        dictValues.put("Threat Reduction", f2(getThreatReduction() * 100) + "%");
        calculationOptions.setCalculations(this);
        return dictValues;
    }

    private static void addFloat(Map<String, Float> map, String key, float delta) {
        map.put(key, map.get(key) + delta);
    }

}
