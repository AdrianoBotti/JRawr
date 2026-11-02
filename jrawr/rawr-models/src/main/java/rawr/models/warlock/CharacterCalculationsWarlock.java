// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/CharacterCalculationsWarlock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: Pet creation (Type.GetType + Activator) and GetSpell (Type.GetType) use
// explicit factories; display values use the .NET-compatible NetFormat helpers.
package rawr.models.warlock;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.stats.WeightedStat;
import rawr.base.talents.WarlockTalents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CharacterCalculationsWarlock extends CharacterCalculationsBase {

    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getPersonalDps() {
        return subPoints[0];
    }

    public float getPetDps() {
        return subPoints[1];
    }

    public Character character;
    public Pet pet;
    public Stats preProcStats;
    public Stats petBuffs;
    public Stats stats;
    public CalculationOptionsWarlock options;
    public WarlockTalents talents;
    public SpellModifiers spellModifiers;
    public List<WeightedStat> haste;

    public float baseMana;
    public float hitChance;
    public float avgTimeUsed;
    public float extraCritAtMax;
    public float avgHaste;

    public List<Spell> priorities;
    public Map<String, Spell> spells;
    public Map<String, Spell> castSpells;

    public CharacterCalculationsWarlock() {
    }

    /**
     * @param stats This should already have buffStats factored in.
     */
    public CharacterCalculationsWarlock(Character character, Stats stats, Stats petBuffs) {
        this.character = character;
        options = (CalculationOptionsWarlock) character.getCalculationOptions();
        if (options == null) {
            options = CalculationOptionsWarlock.makeDefaultOptions();
        }
        talents = character.getWarlockTalents();
        this.stats = stats;
        preProcStats = stats.copy();
        this.petBuffs = petBuffs;
        baseMana = BaseStats.getBaseStats(character).getMana();
        spells = new LinkedHashMap<String, Spell>();
        castSpells = new LinkedHashMap<String, Spell>();
        hitChance = Math.min(1f, options.getBaseHitRate() / 100f + calcSpellHit());

        if (!options.getPet().equals("None")
                && (talents.getSummonFelguard() > 0
                    || !options.getPet().equals("Felguard"))) {
            pet = createPet(options.getPet());
        }

        float personalDps = calcPersonalDps();
        float petDps = calcPetDps();
        float raidBuff = calcRaidBuff();
        subPoints = new float[] { personalDps, petDps, raidBuff };
        overallPoints = personalDps + petDps + raidBuff;
    }

    private Pet createPet(String name) {
        switch (name) {
            case "Felguard": return new Felguard(this);
            case "Felhunter": return new Felhunter(this);
            case "Imp": return new Imp(this);
            case "Succubus": return new Succubus(this);
            case "Voidwalker": return new Voidwalker(this);
            default: throw new IllegalArgumentException("Unknown pet: " + name);
        }
    }

    public float calcStamina() {
        return StatUtils.calcStamina(stats);
    }

    public float calcIntellect() {
        return StatUtils.calcIntellect(stats);
    }

    public float calcSpirit() {
        return StatUtils.calcSpirit(stats);
    }

    public float calcHealth() {
        return StatUtils.calcHealth(stats);
    }

    public float calcMana() {
        return StatUtils.calcMana(stats);
    }

    public float calcUsableMana(float fightLen) {
        return StatUtils.calcUsableMana(stats, fightLen);
    }

    public float calcSpellCrit() {
        return StatUtils.calcSpellCrit(stats);
    }

    public float calcSpellHit() {
        return StatUtils.calcSpellHit(stats);
    }

    public float calcSpellPower() {
        return StatUtils.calcSpellPower(stats);
    }

    public float calcSpellHaste() {
        return StatUtils.calcSpellHaste(stats);
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();

        dictValues.put("Personal DPS", rawr.base.util.NetFormat.fixed(getPersonalDps(), 0));
        dictValues.put("Pet DPS", rawr.base.util.NetFormat.fixed(getPetDps(), 0));
        dictValues.put("Total DPS", rawr.base.util.NetFormat.fixed(overallPoints, 0));

        dictValues.put("Health",
                rawr.base.util.NetFormat.fixed(calcHealth(), 1) + "*"
                        + rawr.base.util.NetFormat.fixed(calcStamina(), 1) + " stamina");
        dictValues.put("Mana",
                rawr.base.util.NetFormat.fixed(calcMana(), 1) + "*"
                        + rawr.base.util.NetFormat.fixed(calcIntellect(), 1) + " intellect");
        dictValues.put("Spirit", rawr.base.util.NetFormat.fixed(calcSpirit(), 1));

        dictValues.put("Bonus Damage",
                rawr.base.util.NetFormat.fixed(calcSpellPower(), 1) + "*"
                        + rawr.base.util.NetFormat.fixed(StatUtils.calcSpellPower(preProcStats), 1)
                        + "\tBefore Procs");

        // region Hit Rating
        float onePercentOfHitRating = (1f / StatConversion.getSpellHitFromRating(1));
        float hitFromRating = StatConversion.getSpellHitFromRating(stats.getHitRating());
        float hitFromTalents = talents.getSuppression() * 0.01f;
        float hitFromBuffs = (calcSpellHit() - hitFromRating - hitFromTalents);
        float targetHit = options.getBaseHitRate() / 100f;
        float totalHit = targetHit + calcSpellHit();
        float missChance = totalHit > 1 ? 0 : (1 - totalHit);
        dictValues.put("Hit Rating",
                rawr.models.mage.NetFormat.netFloatToString(stats.getHitRating())
                        + "*" + rawr.base.util.NetFormat.percent(totalHit, 2)
                        + " Hit Chance (max 100%) | " + rawr.base.util.NetFormat.percent(missChance, 2)
                        + " Miss Chance \r\n\r\n"
                        + rawr.base.util.NetFormat.percent(targetHit, 2)
                        + "\t Base Hit Chance on a Level " + options.getTargetLevel() + " target\r\n"
                        + rawr.base.util.NetFormat.percent(hitFromRating, 2)
                        + "\t from " + rawr.base.util.NetFormat.fixed(stats.getHitRating(), 0)
                        + " Hit Rating [gear, food and/or flasks]\r\n"
                        + rawr.base.util.NetFormat.percent(hitFromTalents, 2)
                        + "\t from Talent: Suppression\r\n"
                        + rawr.base.util.NetFormat.percent(hitFromBuffs, 2)
                        + "\t from Buffs: Racial and/or Spell Hit Chance Taken\r\n\r\n"
                        + "You are "
                        + rawr.models.mage.NetFormat.netDoubleToString(
                                Math.ceil(Math.abs((totalHit - 1) * onePercentOfHitRating)))
                        + " hit rating " + ((totalHit > 1) ? "above" : "below")
                        + " the 446 hard cap [no hit from gear, talents or buffs]\r\n\r\n"
                        + "Hit Rating soft caps:\r\n"
                        + "420 - Heroic Presence\r\n"
                        + "368 - Suppression\r\n"
                        + "342 - Suppression and Heroic Presence\r\n"
                        + "289 - Suppression, Improved Faerie Fire / Misery\r\n"
                        + "263 - Suppression, Improved Faerie Fire / Misery and  Heroic Presence");
        // endregion

        dictValues.put("Crit Chance",
                rawr.base.util.NetFormat.percent(calcSpellCrit(), 2) + "*"
                        + rawr.base.util.NetFormat.percent(StatUtils.calcSpellCrit(preProcStats), 2)
                        + "\tBefore Procs");

        dictValues.put("Average Haste",
                rawr.base.util.NetFormat.fixed((avgHaste - 1f) * 100f, 2) + "%*"
                        + rawr.base.util.NetFormat.fixed(
                                StatConversion.getSpellHasteFromRating(stats.getHasteRating()) * 100f, 2)
                        + "%\tfrom " + rawr.base.util.NetFormat.fixed(stats.getHasteRating(), 1)
                        + " Haste rating\r\n"
                        + rawr.base.util.NetFormat.fixed(stats.getSpellHaste() * 100f, 2)
                        + "%\tfrom Buffs\r\n"
                        + rawr.base.util.NetFormat.fixed(
                                (avgHaste - StatUtils.calcSpellHaste(preProcStats)) * 100f, 1)
                        + "ish%\tfrom Procs\r\n"
                        + "\r\n"
                        + rawr.base.util.NetFormat.fixed(Math.max(1.0f, 1.5f / avgHaste), 2)
                        + "s\tGlobal Cooldown\r\n");

        // Pet Stats
        if (pet == null) {
            dictValues.put("Pet Stamina", "-");
            dictValues.put("Pet Intellect", "-");
            dictValues.put("Pet Health", "-");
        } else {
            dictValues.put("Pet Stamina", rawr.base.util.NetFormat.fixed(pet.calcStamina(), 1));
            dictValues.put("Pet Intellect", rawr.base.util.NetFormat.fixed(pet.calcIntellect(), 1));
            dictValues.put("Pet Health", rawr.base.util.NetFormat.fixed(pet.calcHealth(), 1));
        }

        // Spell Stats
        for (String spellName : Spell.ALL_SPELLS) {
            if (castSpells.containsKey(spellName)) {
                dictValues.put(spellName, castSpells.get(spellName).getToolTip());
            } else {
                dictValues.put(spellName, "-");
            }
        }

        return dictValues;
    }

    private float calcPersonalDps() {
        // SP & Crit: lock before pet (both affected by procs)
        // Procs after crit

        if (options.getActiveRotation().getError() != null) {
            return 0f;
        }

        calcHasteAndManaProcs();
        avgTimeUsed = Spell.getTimeUsed(
                CalculationsWarlock.AVG_UNHASTED_CAST_TIME,
                0f,
                haste,
                options.getLatency());

        float timeRemaining = options.getDuration();
        float totalMana = calcUsableMana(timeRemaining);
        float maxMana = StatUtils.calcMana(preProcStats);
        float manaFromEffects = totalMana - maxMana;
        float manaUsed = 0f;

        // region Calculate NumCasts for each spell

        // execute stage collision delays
        Spell execute = null;
        float executePercent = getExecutePercentage();
        String executeName = options.getActiveRotation().execute;
        if (executePercent > 0) {
            execute = getSpell(executeName);
            setupSpells(true);
            recordCollisionDelays(new CastingState(this, execute, executePercent));
        }

        // normal collision delays
        Spell filler = getSpell(options.getActiveRotation().filler);
        setupSpells(false);
        recordCollisionDelays(new CastingState(this, filler, 1f - executePercent));

        // calc numcasts
        for (Spell spell : priorities) {
            float numCasts = spell.getNumCasts();
            timeRemaining -= numCasts * spell.getAvgTimeUsed();
            manaUsed += numCasts * spell.manaCost;
        }
        LifeTap lifeTap = (LifeTap) getSpell("Life Tap");
        if (executePercent > 0) {
            float executeTime = executePercent * timeRemaining;
            float taps = lifeTap.addCastsForRegen(
                    timeRemaining * executePercent,
                    maxMana + (manaFromEffects - manaUsed) * executePercent,
                    execute);
            executeTime -= taps * lifeTap.getAvgTimeUsed();
            manaUsed += taps * lifeTap.manaCost;
            execute.spam(executeTime);
            timeRemaining -= executeTime;
            manaUsed += execute.manaCost * execute.getNumCasts();
            castSpells.put(options.getActiveRotation().execute, execute);
        }
        timeRemaining -= lifeTap.getAvgTimeUsed()
                * lifeTap.addCastsForRegen(timeRemaining, totalMana - manaUsed, filler);
        filler.spam(timeRemaining);
        castSpells.put(options.getActiveRotation().filler, filler);

        for (Spell spell : castSpells.values()) {
            spell.adjustAfterCastingIsSet();
        }

        // endregion

        // region Calculate spell modifiers, Part 1

        // add procs to RawStats
        if (castSpells.containsKey("Curse Of The Elements")) {
            // If the raid is already providing this debuff, the curse will
            // not actually end up casting, so this will not double-count
            // the debuff.
            stats.setBonusFireDamageMultiplier(.13f);
            stats.setBonusShadowDamageMultiplier(.13f);
            stats.setBonusHolyDamageMultiplier(.13f);
            stats.setBonusFrostDamageMultiplier(.13f);
            stats.setBonusNatureDamageMultiplier(.13f);
            petBuffs.setBonusFireDamageMultiplier(.13f);
            petBuffs.setBonusShadowDamageMultiplier(.13f);
            petBuffs.setBonusHolyDamageMultiplier(.13f);
            petBuffs.setBonusFrostDamageMultiplier(.13f);
            petBuffs.setBonusNatureDamageMultiplier(.13f);
        }
        float critBuff = calcAddedCritBuff();
        stats.setSpellCritOnTarget(stats.getSpellCritOnTarget() + critBuff);
        petBuffs.setSpellCritOnTarget(petBuffs.getSpellCritOnTarget() + critBuff);
        stats.setSpellPower(stats.getSpellPower() + lifeTap.getAvgBonusSpellPower());

        // create the SpellModifiers object
        spellModifiers = new SpellModifiers();
        spellModifiers.addMultiplicativeMultiplier(stats.getBonusDamageMultiplier());
        spellModifiers.addMultiplicativeMultiplier(talents.getMalediction() * .01f);
        spellModifiers.addMultiplicativeMultiplier(talents.getDemonicPact() * .02f);
        spellModifiers.addCritOverallMultiplier(stats.getBonusCritMultiplier());
        if (talents.getMetamorphosis() > 0) {
            spellModifiers.addMultiplicativeMultiplier(getMetamorphosisBonus());
        }
        if (pet instanceof Felguard) {
            spellModifiers.addMultiplicativeMultiplier(talents.getMasterDemonologist() * .01f);
        }
        add4pT10(spellModifiers);

        Stats critProcs = calcCritProcs();
        stats.setCritRating(stats.getCritRating() + critProcs.getCritRating());
        stats.setSpellCrit(stats.getSpellCrit() + critProcs.getSpellCrit());
        stats.setSpellCritOnTarget(stats.getSpellCritOnTarget() + critProcs.getSpellCritOnTarget());
        spellModifiers.addCritChance(calcSpellCrit());

        if (pet != null) {
            pet.calcStats1();
            stats.setSpellPower(stats.getSpellPower()
                    + talents.getDemonicKnowledge() * .04f
                            * (pet.calcStamina() + pet.calcIntellect()));
        }

        // endregion

        float damageDone = calcRemainingProcs();

        // region Calculate Spell Modifiers, Part 2

        if (pet != null) {
            pet.calcStats2();
            stats.setSpellPower(stats.getSpellPower() + pet.applyPactProcBenefit());
        }

        // finilize each spell's modifiers.
        // Start with Conflagrate, since pyroclasm depends on its results.
        if (castSpells.containsKey("Conflagrate")) {
            castSpells.get("Conflagrate").finalizeSpellModifiers();
        }
        for (Spell spell : castSpells.values()) {
            if (!(spell instanceof Conflagrate)) {
                spell.finalizeSpellModifiers();
            }
        }

        // endregion

        // region Calculate damage done for each spell
        Spell conflagrate = null;
        float spellPower = calcSpellPower();
        for (Map.Entry<String, Spell> pair : castSpells.entrySet()) {
            Spell spell = pair.getValue();
            if (pair.getKey().equals("Conflagrate")) {
                conflagrate = spell;
                continue; // save until we're sure immolate is done
            }
            spell.setDamageStats(spellPower);
            damageDone += spell.getNumCasts() * spell.avgDamagePerCast;
        }
        if (conflagrate != null) {
            conflagrate.setDamageStats(spellPower);
            damageDone += conflagrate.getNumCasts() * conflagrate.avgDamagePerCast;
        }
        // endregion

        return damageDone / options.getDuration();
    }

    private float calcPetDps() {
        if (pet == null) {
            return 0f;
        } else {
            return pet.calcMeleeDps() + pet.calcSpecialDps();
        }
    }

    private float calcRaidBuff() {
        float raidBuff = 0f;

        float perSP = options.getPerSP();
        if (perSP > 0 && pet != null) {
            raidBuff += perSP * pet.getPactProcBenefit();
            if (options.getConvertTotem()) {
                float curTotem = StatUtils.getActiveBuff(
                        character.getActiveBuffs(),
                        "Spell Power",
                        s -> s.getSpellPower());
                if (curTotem == 144f || curTotem == 165f) {
                    raidBuff += options.getPerFlametongue();
                }
            }
        }

        if (castSpells.containsKey("Curse Of The Elements")) {
            raidBuff += options.getPerMagicBuff();
        }

        raidBuff += options.getPerCritBuff() * (calcAddedCritBuff() / .05f);

        raidBuff += options.getPerInt()
                * CalculationsWarlock.calcPetIntBuff(
                        options.getPet(), talents, character.getActiveBuffs());
        raidBuff += options.getPerSpi()
                * CalculationsWarlock.calcPetSpiBuff(
                        options.getPet(), talents, character.getActiveBuffs());
        raidBuff += options.getPerHealth()
                * CalculationsWarlock.calcPetHealthBuff(
                        options.getPet(), talents, character.getActiveBuffs());

        return raidBuff;
    }

    public float getExecutePercentage() {
        String executeName = options.getActiveRotation().execute;
        if (executeName == null || executeName.equals("") || executeName.equals("Drain Soul")) {
            return 0f;
        }

        Spell execute = getSpell(executeName);
        if (!execute.isCastable()) {
            return 0f;
        }

        if (execute instanceof SoulFire) {
            return options.getThirtyFive();
        } else {
            return options.getTwentyFive();
        }
    }

    private float calcAddedCritBuff() {
        // If the 5% crit debuff is not already being maintained by
        // somebody else (i.e. it's not selected in the buffs tab), we
        // may supply it via Improved Shadow Bolt.
        if (talents.getImprovedShadowBolt() == 0
                || StatUtils.getActiveBuff(
                        character.getActiveBuffs(),
                        "Spell Critical Strike Taken",
                        s -> s.getSpellCritOnTarget())
                    > 0) {
            return 0f;
        }

        float casts = 0f;
        if (castSpells.containsKey("Shadow Bolt")) {
            casts += castSpells.get("Shadow Bolt").getNumCasts();
        }
        if (castSpells.containsKey("Shadow Bolt (Instant)")) {
            casts += castSpells.get("Shadow Bolt (Instant)").getNumCasts();
        }
        if (casts == 0) {
            return 0f;
        }

        float uprate = Spell.calcUprate(
                talents.getImprovedShadowBolt() * .2f, // proc rate
                30f, // duration
                options.getDuration() / casts); // trigger period
        float benefit = .05f - stats.getSpellCritOnTarget();
        return benefit * uprate;
    }

    private void calcHasteAndManaProcs() {
        float nonProcHaste = StatUtils.calcSpellHaste(preProcStats);
        if (options.getNoProcs()) {
            WeightedStat staticHaste = new WeightedStat(1f, nonProcHaste);
            haste = new ArrayList<WeightedStat>();
            haste.add(staticHaste);
            avgHaste = nonProcHaste;
            return;
        }

        // the trigger rates are all guestimates at this point, since the
        // real values depend on haste (which obviously has not been
        // finalized yet)
        Map<Integer, Float> periods = new LinkedHashMap<Integer, Float>();
        Map<Integer, Float> chances = new LinkedHashMap<Integer, Float>();
        float corruptionPeriod = 0f;
        if (options.getActiveRotation().contains("Corruption")) {
            corruptionPeriod = 3.1f;
            if (talents.isGlyphQuickDecay()) {
                corruptionPeriod /= nonProcHaste;
            }
        }
        populateTriggers(
                periods,
                chances,
                CalculationsWarlock.AVG_UNHASTED_CAST_TIME / nonProcHaste
                        + options.getLatency(),
                1f / 1.5f,
                corruptionPeriod,
                1f);

        // calculate the haste procs
        haste = new ArrayList<WeightedStat>();
        WeightedStat[] percentages = getUptimes(
                stats,
                periods,
                chances,
                s -> s.getSpellHaste(),
                (a, b, c, d, e, f, g, h) -> SpecialEffect
                        .getAverageCombinedUptimeCombinationsMultiplicative(
                                a, b, c, d, e, f, g, h));
        WeightedStat[] ratings = getUptimes(
                stats,
                periods,
                chances,
                s -> s.getHasteRating(),
                (a, b, c, d, e, f, g, h) -> SpecialEffect
                        .getAverageCombinedUptimeCombinations(
                                a, b, c, d, e, f, g, h));
        for (int p = percentages.length, f = 0; --p >= 0; ) {
            if (percentages[p].chance() == 0) {
                continue;
            }

            for (int r = ratings.length; --r >= 0; ++f) {
                if (ratings[r].chance() == 0) {
                    continue;
                }
                WeightedStat s = new WeightedStat(percentages[p].chance() * ratings[r].chance(),
                        (1f + percentages[p].value())
                        * (1f + StatConversion.getSpellHasteFromRating(
                                ratings[r].value() + stats.getHasteRating()))
                        * (1f + stats.getSpellHaste()));
                haste.add(s);
                avgHaste += s.chance() * s.value();
            }
        }

        // calculate mana procs
        Stats procStats = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (!periods.containsKey(effect.getTrigger().ordinal())) {
                continue;
            }

            Stats proc = effect.getAverageStats(
                    periods.get(effect.getTrigger().ordinal()),
                    chances.get(effect.getTrigger().ordinal()),
                    CalculationsWarlock.AVG_UNHASTED_CAST_TIME,
                    options.getDuration());
            if (proc.getManaRestore() > 0) {
                proc.setManaRestore(proc.getManaRestore() * options.getDuration());
            }
            procStats.accumulate(proc);
        }
        stats.setMana(stats.getMana() + procStats.getMana());
        stats.setManaRestore(stats.getManaRestore() + procStats.getManaRestore());
        stats.setManaRestoreFromBaseManaPPM(
                stats.getManaRestoreFromBaseManaPPM() + procStats.getManaRestoreFromBaseManaPPM());
        stats.setManaRestoreFromMaxManaPerSecond(
                stats.getManaRestoreFromMaxManaPerSecond() + procStats.getManaRestoreFromMaxManaPerSecond());
        stats.setMp5(stats.getMp5() + procStats.getMp5());
    }

    public WeightedStat[] getUptimes(
            Stats stats,
            Map<Integer, Float> periods,
            Map<Integer, Float> chances,
            StatExtractor statExtractor,
            UptimeCombiner uptimeCombiner) {

        List<SpecialEffect> hasteEffects = new ArrayList<SpecialEffect>();
        List<Float> hasteIntervals = new ArrayList<Float>();
        List<Float> hasteChances = new ArrayList<Float>();
        List<Float> hasteOffsets = new ArrayList<Float>();
        List<Float> hasteScales = new ArrayList<Float>();
        List<Float> hasteValues = new ArrayList<Float>();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (!periods.containsKey(effect.getTrigger().ordinal())) {
                continue;
            }

            float value = statExtractor.extract(effect.getStats());
            if (value > 0) {
                hasteEffects.add(effect);
                hasteIntervals.add(periods.get(effect.getTrigger().ordinal()));
                hasteChances.add(chances.get(effect.getTrigger().ordinal()));
                hasteOffsets.add(0f);
                hasteScales.add(1f);
                hasteValues.add(value);
            }
        }
        return uptimeCombiner.combine(
                hasteEffects.toArray(new SpecialEffect[0]),
                toFloatArray(hasteIntervals),
                toFloatArray(hasteChances),
                toFloatArray(hasteOffsets),
                toFloatArray(hasteScales),
                CalculationsWarlock.AVG_UNHASTED_CAST_TIME,
                options.getDuration(),
                toFloatArray(hasteValues));
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private Stats calcCritProcs() {
        if (options.getNoProcs()) {
            return new Stats();
        }

        Map<Integer, Float> periods = new LinkedHashMap<Integer, Float>();
        Map<Integer, Float> chances = new LinkedHashMap<Integer, Float>();
        populateTriggers(periods, chances);

        Stats procStats = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (!periods.containsKey(effect.getTrigger().ordinal())) {
                continue;
            }

            Stats proc = calcNormalProc(effect, periods, chances);
            procStats.accumulate(proc);
            if (effect.getTrigger() == Trigger.Use && !isDoublePot(effect)) {
                extraCritAtMax += StatUtils.calcSpellCrit(effect.getStats())
                        - StatUtils.calcSpellCrit(proc);
            }
        }
        return procStats;
    }

    private float calcRemainingProcs() {
        if (options.getNoProcs()) {
            return 0f;
        }

        Map<Integer, Float> periods = new LinkedHashMap<Integer, Float>();
        Map<Integer, Float> chances = new LinkedHashMap<Integer, Float>();
        populateTriggers(periods, chances);

        float procdDamage = 0f;
        Stats procStats = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (!periods.containsKey(effect.getTrigger().ordinal())) {
                continue;
            }

            Stats effectStats = effect.getStats();
            if (effectStats.getValkyrDamage() > 0) {
                SpellModifiers mods = new SpellModifiers();
                mods.addCritChance(.05f + stats.getSpellCritOnTarget());
                mods.addMultiplicativeMultiplier(stats.getBonusHolyDamageMultiplier());
                procdDamage += calcDamageProc(
                        effect,
                        effect.getStats().getValkyrDamage(),
                        periods,
                        chances,
                        mods);
            } else if (effectStats.getShadowDamage() > 0
                    || effectStats.getFireDamage() > 0
                    || effectStats.getNatureDamage() > 0
                    || effectStats.getHolyDamage() > 0
                    || effectStats.getFrostDamage() > 0) {
                SpellModifiers mods = new SpellModifiers();
                mods.accumulate(spellModifiers);
                if (options.getImbue().equals("Grand Firestone")) {
                    mods.addAdditiveDirectMultiplier(.01f);
                }
                if (effectStats.getShadowDamage() > 0) {
                    addShadowModifiers(mods);
                } else if (effectStats.getFireDamage() > 0) {
                    addFireModifiers(mods);
                }
                procdDamage += calcDamageProc(
                        effect,
                        effectStats.getShadowDamage()
                                + effectStats.getFireDamage()
                                + effectStats.getNatureDamage()
                                + effectStats.getHolyDamage()
                                + effectStats.getFrostDamage(),
                        periods,
                        chances,
                        mods);
            } else {
                procStats.accumulate(calcNormalProc(effect, periods, chances));
            }
        }

        procStats.setHasteRating(0f);
        procStats.setSpellHaste(0f);
        procStats.setMana(0f);
        procStats.setManaRestore(0f);
        procStats.setManaRestoreFromBaseManaPPM(0f);
        procStats.setManaRestoreFromMaxManaPerSecond(0f);
        procStats.setMp5(0f);
        procStats.setCritRating(0f);
        procStats.setSpellCrit(0f);
        procStats.setSpellCritOnTarget(0f);
        procStats.setPhysicalCrit(0f);
        stats.accumulate(procStats);

        return procdDamage;
    }

    private Stats calcNormalProc(
            SpecialEffect effect,
            Map<Integer, Float> periods,
            Map<Integer, Float> chances) {

        Stats proc = effect.getAverageStats(
                periods.get(effect.getTrigger().ordinal()),
                chances.get(effect.getTrigger().ordinal()),
                CalculationsWarlock.AVG_UNHASTED_CAST_TIME,
                options.getDuration());

        // Handle "recursive effects" - i.e. those that *enable* a
        // proc during a short window.
        if (effect.getStats().getRawSpecialEffectDataSize() == 1
                && periods.containsKey(
                        effect.getStats().getRawSpecialEffectData()[0].getTrigger().ordinal())) {

            SpecialEffect inner = effect.getStats().getRawSpecialEffectData()[0];
            Stats innerStats = inner.getAverageStats(
                    periods.get(inner.getTrigger().ordinal()),
                    chances.get(inner.getTrigger().ordinal()),
                    1f,
                    effect.getDuration());
            float upTime = effect.getAverageUptime(
                    periods.get(effect.getTrigger().ordinal()),
                    chances.get(effect.getTrigger().ordinal()),
                    1f,
                    options.getDuration());
            proc.accumulate(innerStats, upTime);
        }

        return proc;
    }

    private float calcDamageProc(
            SpecialEffect effect,
            float damagePerProc,
            Map<Integer, Float> periods,
            Map<Integer, Float> chances,
            SpellModifiers modifiers) {

        damagePerProc *= modifiers.getFinalDirectMultiplier()
                * (1 + (modifiers.getFinalCritMultiplier() - 1)
                        * modifiers.getCritChance())
                * (1 - StatConversion.getAverageResistance(
                        80, options.getTargetLevel(), 0f, 0f));
        float numProcs = options.getDuration()
                * effect.getAverageProcsPerSecond(
                        periods.get(effect.getTrigger().ordinal()),
                        chances.get(effect.getTrigger().ordinal()),
                        CalculationsWarlock.AVG_UNHASTED_CAST_TIME,
                        options.getDuration());
        return numProcs * damagePerProc;
    }

    private boolean isDoublePot(SpecialEffect effect) {
        return effect.getCooldown() == 1200f && effect.getDuration() == 14f;
    }

    private void populateTriggers(
            Map<Integer, Float> periods,
            Map<Integer, Float> chances) {

        float totalCasts = 0f;
        float totalTicks = 0f;
        float corruptionTicks = 0f;
        SimulatedStat castsPerCrittable = new SimulatedStat();
        for (Spell spell : castSpells.values()) {
            if (spell.baseDamage == 0 && spell.baseTickDamage == 0) {
                continue;
            }

            float numCasts = spell.getNumCasts();
            float numTicks = hitChance * numCasts * spell.numTicks;
            totalCasts += numCasts;
            totalTicks += numTicks;

            float numCrittables = 0f;
            if (spell.baseDamage > 0) {
                numCrittables += hitChance * numCasts;
            }
            if (spell.baseTickDamage > 0 && spell.canTickCrit) {
                numCrittables += numTicks;
            }
            castsPerCrittable.addSample(
                    numCrittables == 0 ? 0f : numCasts / numCrittables,
                    numCasts);

            if (spell instanceof Corruption) {
                corruptionTicks += numTicks;
            }
        }
        populateTriggers(
                periods,
                chances,
                options.getDuration() / totalCasts,
                totalTicks / options.getDuration(),
                corruptionTicks == 0 ? -1 : options.getDuration() / corruptionTicks,
                castsPerCrittable.getValue());
    }

    private void populateTriggers(
            Map<Integer, Float> periods,
            Map<Integer, Float> chances,
            float castPeriod,
            float dotFrequency,
            float corruptionPeriod,
            float castsPerCrittable) {

        periods.put(Trigger.Use.ordinal(), 0f);
        periods.put(Trigger.SpellHit.ordinal(), castPeriod);
        periods.put(Trigger.SpellCast.ordinal(), castPeriod);
        periods.put(Trigger.SpellMiss.ordinal(), castPeriod);
        periods.put(Trigger.DamageSpellHit.ordinal(), castPeriod);
        periods.put(Trigger.DamageSpellCast.ordinal(), castPeriod);
        periods.put(Trigger.SpellCrit.ordinal(), castPeriod * castsPerCrittable);
        periods.put(Trigger.DamageSpellCrit.ordinal(), castPeriod * castsPerCrittable);
        periods.put(Trigger.DoTTick.ordinal(), 1f / dotFrequency);
        periods.put(Trigger.DamageDone.ordinal(), 1f / (dotFrequency + 1f / castPeriod));
        periods.put(Trigger.DamageOrHealingDone.ordinal(), 1f / (dotFrequency + 1f / castPeriod));
        periods.put(Trigger.CorruptionTick.ordinal(), corruptionPeriod);

        chances.put(Trigger.Use.ordinal(), 1f);
        chances.put(Trigger.SpellHit.ordinal(), hitChance);
        chances.put(Trigger.DamageSpellHit.ordinal(), hitChance);
        chances.put(Trigger.DamageDone.ordinal(), hitChance);
        chances.put(Trigger.DamageOrHealingDone.ordinal(), hitChance);
        chances.put(Trigger.SpellCrit.ordinal(), calcSpellCrit());
        chances.put(Trigger.DamageSpellCrit.ordinal(), calcSpellCrit());
        chances.put(Trigger.SpellCast.ordinal(), 1f);
        chances.put(Trigger.DamageSpellCast.ordinal(), 1f);
        chances.put(Trigger.SpellMiss.ordinal(), 1 - hitChance);
        chances.put(Trigger.DoTTick.ordinal(), 1f);
        chances.put(Trigger.CorruptionTick.ordinal(), corruptionPeriod == 0f ? 0f : 1f);
    }

    private void setupSpells(boolean execute) {
        priorities = new ArrayList<Spell>();
        for (String spellName
                : options.getActiveRotation().getPrioritiesForCalcs(talents, execute)) {

            Spell spell = getSpell(spellName);
            if (spell.isCastable()
                    && (!execute || spell.isCastDuringExecute())) {

                priorities.add(spell);
                if (!castSpells.containsKey(spellName)) {
                    castSpells.put(spellName, spell);
                }
            }
        }
    }

    // This technique assumes that if you pick a random time during filler
    // spell(s) or downtime, the "cooldowns" remaining on the rest of your
    // spells are all equally likely to be at any value. This is unrealistic
    // (e.g. it's impossible for them all to be at their full value), but
    // for some classes is a reasonable approximation.
    private void recordCollisionDelays(CastingState state) {
        float pRemaining = 1f;
        for (Spell spell : priorities) {
            float p = spell.getQueueProbability(state);
            if (p == 0f) {
                continue;
            }

            List<CastingState> nextStates = spell.simulateCast(state, p * pRemaining);
            for (CastingState nextState : nextStates) {
                if (nextState.probability > .0001f) {
                    // Only calculate if the probabilty of the state is
                    // large enough to make any difference at all.
                    recordCollisionDelays(nextState);
                }
            }
            if (p == 1f) {
                return;
            }

            pRemaining *= 1f - p;
        }
    }

    public float getMetamorphosisBonus() {
        if (talents.getMetamorphosis() == 0) {
            return 0f;
        }

        float cooldown = 180f * (1f - talents.getNemesis() * .1f);
        float duration = 30f;
        if (talents.isGlyphMetamorphosis()) {
            duration += 6f;
        }
        return .2f * duration / cooldown;
    }

    public void addShadowModifiers(SpellModifiers modifiers) {
        modifiers.addMultiplicativeMultiplier(stats.getBonusShadowDamageMultiplier());
        modifiers.addAdditiveMultiplier(talents.getShadowMastery() * .03f);
        if (options.getActiveRotation().contains("Shadow Bolt")
                || (options.getActiveRotation().contains("Haunt")
                    && talents.getHaunt() > 0)) {

            modifiers.addMultiplicativeTickMultiplier(
                    talents.getShadowEmbrace() * .01f * 3f);
        }
        if (castSpells.containsKey("Haunt")) {
            modifiers.addMultiplicativeTickMultiplier(
                    ((Haunt) castSpells.get("Haunt")).getAvgTickBonus());
        }
        if (pet instanceof Succubus) {
            float bonus = talents.getMasterDemonologist() * .01f;
            modifiers.addMultiplicativeMultiplier(bonus);
            modifiers.addCritChance(bonus);
        }
    }

    public void addFireModifiers(SpellModifiers modifiers) {
        modifiers.addMultiplicativeMultiplier(stats.getBonusFireDamageMultiplier());
        modifiers.addAdditiveMultiplier(talents.getEmberstorm() * .03f);
        if (pet instanceof Imp) {
            float bonus = talents.getMasterDemonologist() * .01f;
            modifiers.addMultiplicativeMultiplier(bonus);
            modifiers.addCritChance(bonus);
        }
    }

    public void add4pT10(SpellModifiers modifiers) {
        if (stats.getWarlock4T10() == 0) {
            return;
        }

        Spell trigger = null;
        if (castSpells.containsKey("Immolate")) {
            trigger = castSpells.get("Immolate");
        } else if (castSpells.containsKey("Unstable Affliction")) {
            trigger = castSpells.get("Unstable Affliction");
        }
        if (trigger != null) {
            float numTicks = hitChance * trigger.getNumCasts() * trigger.numTicks;
            float uprate = Spell.calcUprate(
                    .15f, 10f, options.getDuration() / numTicks);
            modifiers.addMultiplicativeMultiplier(.1f * uprate);
        }
    }

    public Spell getSpell(String spellName) {
        if (spells.containsKey(spellName)) {
            return spells.get(spellName);
        }

        Spell spell = createSpell(spellName);
        spells.put(spellName, spell);
        return spell;
    }

    private Spell createSpell(String spellName) {
        switch (spellName) {
            case "Chaos Bolt": return new ChaosBolt(this);
            case "Conflagrate": return new Conflagrate(this);
            case "Corruption": return new Corruption(this);
            case "Curse Of Agony": return new CurseOfAgony(this);
            case "Curse Of Doom": return new CurseOfDoom(this);
            case "Curse Of The Elements": return new CurseOfTheElements(this);
            case "Haunt": return new Haunt(this);
            case "Immolate": return new Immolate(this);
            case "Immolation Aura": return new ImmolationAura(this);
            case "Incinerate": return new Incinerate(this);
            case "Incinerate (Under Backdraft)": return new Incinerate_UnderBackdraft(this);
            case "Incinerate (Under Molten Core)": return new Incinerate_UnderMoltenCore(this);
            case "Life Tap": return new LifeTap(this);
            case "Shadow Bolt": return new ShadowBolt(this);
            case "Shadow Bolt (Instant)": return new ShadowBolt_Instant(this);
            case "Soul Fire": return new SoulFire(this);
            case "Unstable Affliction": return new UnstableAffliction(this);
            default: throw new IllegalArgumentException("Unknown spell: " + spellName);
        }
    }

    public boolean isPriorityOrdered(Spell s1, Spell s2) {
        int i1 = priorities.indexOf(s1);
        int i2 = priorities.indexOf(s2);
        return (i1 < i2 && i1 != -1) || (i1 != -1 && i2 == -1);
    }
}
