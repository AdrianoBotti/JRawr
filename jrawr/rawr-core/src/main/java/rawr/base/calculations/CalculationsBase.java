// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Calculations.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the members needed by the Mage closure. UI/chart/options-panel members are added with
// the models in phase 3/6. PropertyInfo-based stat handling uses Stats.StatProperty instead.
// Stage B additive: getSubPointNameColors()/getOverallRatingLabel() expose the original
// SubPointNameColors label/color tables to the web UI without any UI type dependency.
package rawr.base.calculations;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.character.Profession;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.stats.Stats;
import rawr.base.util.Extensions;
import rawr.base.util.NetFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Base class of every simulation model: it defines the contract the framework calls and
 * provides the model-independent helpers a model uses while assembling and rating a character.
 *
 * <p>A concrete subclass (for example {@code rawr.models.mage.CalculationsMage}) simulates one
 * character specialization (spec). The framework obtains the shared instance from
 * {@link Calculations#getModel(String)}, then calls
 * {@link #getCharacterCalculations(Character, Item, boolean, boolean, boolean)} whenever the
 * character changes; that entry point totals the character's stats with
 * {@link #getCharacterStats(Character, Item)} and returns a {@link CharacterCalculationsBase}
 * holding the rating points (the numbers the optimizer maximizes) and the human-readable
 * display values. The optimizer calls the same entry points thousands of times per run, so an
 * implementation must be deterministic for equal inputs. Note that because the model instance
 * is shared, any state kept in its fields crosses calls and even characters; the original
 * occasionally relies on this, so per-calculation scratch state must be managed
 * deliberately.</p>
 *
 * <h2>What a model must implement</h2>
 *
 * <ul>
 *   <li>{@link #getCharacterCalculations(Character, Item, boolean, boolean, boolean)} and
 *       {@link #getCharacterStats(Character, Item)} &mdash; the simulation itself.</li>
 *   <li>{@link #createNewComparisonCalculation()} &mdash; factory for the model's comparison
 *       entry type.</li>
 *   <li>{@link #getDefaultGemmingTemplates()} &mdash; recommended socket-color/gem presets.</li>
 *   <li>{@link #deserializeDataObject(String)} &mdash; loading the model's saved options.</li>
 * </ul>
 *
 * <p>The remaining virtual methods have defaults that let a skeletal model run:
 * {@link #getRelevantItemTypes()}, {@link #hasRelevantStats(Stats)}, {@link #getTargetClass()},
 * {@link #includeOffHandInCalculations(Character)} and {@link #canUseAmmo()}. A real model
 * overrides most of them. Two hooks that were abstract in C# ({@code hasRelevantStats},
 * {@code getTargetClass}) are permissive defaults in the port, as noted on the methods.</p>
 *
 * <h2>What this base provides</h2>
 *
 * <ul>
 *   <li><b>Stat accumulation</b> &mdash; {@link #accumulateItemStats} walks the gear slots
 *       including gems and enchants; {@link #accumulateBuffsStats} adds buff stats. Both use
 *       the {@code Stats} incremental ("unsafe") API, which the caller must not nest on the
 *       same object.</li>
 *   <li><b>Relevance filtering</b> &mdash; which gear, buffs and enchants the model cares
 *       about: {@link #isItemRelevant}, {@link #isBuffRelevant}, {@link #isEnchantRelevant},
 *       {@link #itemFitsInSlot}, {@link #enchantFitsInSlot}. These gates keep the optimizer's
 *       candidate pool small. The filtering methods swallow exceptions and treat a failure as
 *       "not relevant", a faithful port of the original's defensive try/catch blocks.</li>
 *   <li><b>Comparison building</b> &mdash; {@link #getItemCalculations} and the
 *       {@code getCharacterComparisonCalculations} overloads turn model runs into
 *       {@link ComparisonCalculationBase} entries for item charts and optimizer upgrade
 *       lists; {@link #getRelativeStatValue} estimates one stat's worth by finite
 *       differences.</li>
 *   <li><b>Caching</b> &mdash; the empty-slot baseline used by {@link #getItemCalculations} is
 *       cached by character reference and slot; see {@link #getCachedCharacter} and
 *       {@link #clearCache()}.</li>
 * </ul>
 *
 * @see Calculations
 * @see CharacterCalculationsBase
 * @see ComparisonCalculationBase
 */
public abstract class CalculationsBase {
    /**
     * Registration name of this model, assigned by {@link Calculations#getModel(String)} when
     * the instance is created. It is {@code null} until the model is first loaded.
     */
    public String name;

    public String getName() {
        return name;
    }

    /**
     * Stage B additive API (not present on the C# base): display metadata for the ratings panel.
     * Sub-point labels mapped to their hex color ({@code #RRGGBB}), in the same order as
     * {@link CharacterCalculationsBase#getSubPoints()}. The default is empty; the UI falls back to
     * generic "Sub-point N" labels and a neutral color. Ported models override it with the label
     * and color tables of the original {@code SubPointNameColors} property (which is UI-only and
     * was omitted from the Stage A port).
     */
    public LinkedHashMap<String, String> getSubPointNameColors() {
        return new LinkedHashMap<>();
    }

    /**
     * Stage B additive API: label of the overall rating (the model score the optimizer maximizes).
     * Defaults to {@code "Overall"}; models may override it.
     */
    public String getOverallRatingLabel() {
        return "Overall";
    }

    /**
     * Stage B additive API (not present on the C# base): the original
     * {@code CharacterDisplayCalculationLabels} string array of a model, in display order. Each
     * entry has the form {@code "Group:Label"}, optionally followed by {@code "*Tooltip"} (the C#
     * UI tooltip suffix, kept verbatim). The web UI uses it to order and group the entries of
     * {@code CharacterDisplayCalculationValues} (a display key matches the label after {@code ':'},
     * before any {@code '*'}). Defaults to empty; callers then fall back to the map's own order and
     * a generic group.
     */
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of();
    }

    /**
     * The item types (armor class, weapon kind, ...) this model can use. The item filter
     * caches this list per model name and uses it as the first gate when discarding gear that
     * can never contribute. A model should include {@link ItemType#None} for items without a
     * proficiency requirement (rings, necklaces, cloaks, and similar). The default is an
     * empty list, which makes {@link #isItemRelevant} reject every item, so a usable model
     * must override this.
     */
    public List<ItemType> getRelevantItemTypes() {
        return List.of();
    }

    /**
     * Extra labels the optimizer may optimize for beyond Overall and the sub-points (for example
     * a model-specific average delay or proc value). The returned labels are passed verbatim to
     * {@link CharacterCalculationsBase#getOptimizableCalculationValue(String)}. Defaults to none;
     * models that expose optimizable calculations override this.
     */
    public String[] getOptimizableCalculationLabels() {
        return new String[0];
    }

    /**
     * Factory for the model's comparison entry (a subclass of
     * {@link ComparisonCalculationBase}). Generic comparison and optimizer code calls this
     * instead of constructing a class directly; the returned object is empty and the caller
     * fills in name, points and item.
     */
    public abstract ComparisonCalculationBase createNewComparisonCalculation();

    /**
     * The gemming templates the model recommends: named presets that assign one gem id per
     * socket color (red, yellow, blue, meta). They seed newly imported characters and expand
     * the optimizer's item pool with gemmed variants. See {@link GemmingTemplate}.
     */
    public abstract List<GemmingTemplate> getDefaultGemmingTemplates();

    /**
     * Recreates this model's calculation options (fight length, movement, latency, ...) from
     * the XML fragment stored in a saved character file. Called while loading characters, so
     * it must accept the exact element and attribute names the original wrote.
     */
    public abstract ICalculationOptionBase deserializeDataObject(String xml);

    /**
     * Primary model entry point: simulates the character and returns the finished rating
     * result.
     *
     * <p>Implementations usually call {@link #getCharacterStats(Character, Item)} to total
     * the character's stats, then translate those stats into an overall rating and the
     * model's sub-point ratings, plus the display values when they are requested.</p>
     *
     * @param character character to simulate
     * @param additionalItem item treated as worn in addition to the slotted gear, or
     *     {@code null}; used for gems and other slotless comparisons
     * @param referenceCalculation true when this run defines the baseline that subsequent
     *     comparison runs are measured against, so a model may keep carry-over state for it
     * @param significantChange true when the change from the previous reference run can alter
     *     the character's behavior (talents, glyphs), so cached intermediary results must not
     *     be reused
     * @param needsDisplayCalculations false when only rating numbers are needed and the model
     *     may skip building the display map
     * @return the model's result object; never {@code null}
     */
    public abstract CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations);

    public CharacterCalculationsBase getCharacterCalculations(Character character) {
        return getCharacterCalculations(character, null, false, false, false);
    }

    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem) {
        return getCharacterCalculations(character, additionalItem, false, false, false);
    }

    // C# Calculations.cs:561 `public virtual Stats GetCharacterStats(Character character)`.
    public Stats getCharacterStats(Character character) {
        return getCharacterStats(character, null);
    }

    // C# Calculations.cs:574 `public abstract Stats GetCharacterStats(Character character, Item additionalItem)`.
    /**
     * Totals every stat contribution for the character into one {@link Stats} snapshot: base
     * attributes, gear with gems/enchants/socket bonuses, buffs and any model-specific
     * effects. This is the input to the simulation and the second-most expensive step of a
     * calculation. The shared part is implemented by {@link #accumulateItemStats} and
     * {@link #accumulateBuffsStats}.
     *
     * @param character character whose stats should be totaled
     * @param additionalItem extra item treated as worn, or {@code null}
     * @return the combined stats
     */
    public abstract Stats getCharacterStats(Character character, Item additionalItem);

    /**
     * Accumulates the stats of all equipped gear into {@code stats}, plus {@code additionalItem}
     * when it is non-null. Only the {@link Character#OptimizableSlotCount} gear slots are
     * walked; the off-hand slot is skipped when
     * {@link #includeOffHandInCalculations(Character)} reports that it contributes nothing
     * (main hand is a two-handed weapon). The increments use the {@code Stats} unsafe accumulate
     * API.
     */
    public void accumulateItemStats(Stats stats, Character character, Item additionalItem) {
        for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
            if (slot != CharacterSlot.OffHand.value() || includeOffHandInCalculations(character)) {
                ItemInstance item = character.getItem(slot);
                if (item != null) {
                    item.accumulateTotalStats(character, stats);
                }
            }
        }
        if (additionalItem != null)
            stats.accumulateUnsafe(additionalItem.getStats());
    }

    /**
     * Accumulates the stats of buffs looked up by name from the global buff cache. Names that
     * do not resolve, and null or empty names, are silently skipped. The {@code Buff}
     * overload is preferable when the caller already holds the objects.
     */
    public void accumulateBuffsStatsByNames(Stats stats, List<String> buffs) {
        for (String buffName : buffs)
            if (buffName != null && !buffName.isEmpty()) {
                Buff buff = Buff.getBuffByName(buffName);
                if (buff != null) {
                    stats.accumulate(buff.stats);
                }
            }
    }

    /**
     * Accumulates the stats of the given buffs into {@code stats}; null entries are skipped.
     * The {@code true} argument to the unsafe accumulate call builds each buff's sparse index
     * cache when it is missing, which is what keeps the optimizer's repeated character
     * rebuilds fast.
     */
    public void accumulateBuffsStats(Stats stats, List<Buff> buffs) {
        for (Buff buff : buffs)
            if (buff != null) {
                stats.accumulateUnsafe(buff.stats, true);
            }
    }

    public Stats getBuffsStatsByNames(List<String> buffs) {
        Stats stats = new Stats();
        accumulateBuffsStatsByNames(stats, buffs);
        return stats;
    }

    public Stats getBuffsStats(List<Buff> buffs) {
        Stats stats = new Stats();
        accumulateBuffsStats(stats, buffs);
        return stats;
    }

    /**
     * Builds the plain-text "stats report" used by the application's copy-stats command: a
     * header with the character's name, realm and race, followed by every display entry
     * rendered as {@code label: value} with any tooltip suffix (the part after {@code '*'})
     * removed. It calls the model calculation with {@code needsDisplayCalculations} set to
     * true.
     */
    public String getCharacterStatsString(Character character) {
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("Character:\t\t%s@%s-%s\r\nRace:\t\t%s",
                character.getName(), character.getRegion(), character.getRealm(), character.getRace()));

        for (Map.Entry<String, String> kvp : getCharacterCalculations(character, null, false, false, true).getCharacterDisplayCalculationValues().entrySet()) {
            stats.append(String.format("\r\n%s:\t\t%s", kvp.getKey(), kvp.getValue().split("\\*")[0]));
        }

        return stats.toString();
    }

    // C# Calculations.cs:421-424 cache fields (shared with the ItemInstance overload in the original;
    // only the Item overload is ported).
    protected CharacterCalculationsBase cachedCharacterStatsWithSlotEmpty = null;
    protected CharacterSlot cachedSlot = CharacterSlot.Shirt;
    protected Character cachedCharacter = null;

    /**
     * The character reference stored by the last {@link #getItemCalculations} call, or
     * {@code null} after {@link #clearCache()}. Comparison charts use it to reuse the cached
     * empty-slot baseline.
     */
    public Character getCachedCharacter() {
        return cachedCharacter;
    }

    // C# Calculations.cs:729-733.
    /**
     * Drops the cached empty-slot baseline. Call this between top-level calculations: the
     * cache is keyed by character reference and slot only, so reusing the same character
     * object after mutating it would otherwise score new items against a stale baseline.
     */
    public void clearCache() {
        cachedCharacterStatsWithSlotEmpty = null;
        cachedCharacter = null;
        cachedSlot = CharacterSlot.Shirt;
    }

    // C# Calculations.cs:662-700 UpdateProfessions (virtual; no Java model overrides it).
    /**
     * Synchronizes the automatic profession buffs on the character: miners always have
     * "Toughness" active, skinners "Master of Anatomy", and a character with an injector buff
     * gains its "(Engineer Bonus)" version only while Engineering is among the professions.
     * The base implementation is model-independent; the Java port has no overrides.
     */
    public void updateProfessions(Character character) {
        // Miners should always have this buff activated, others shouldn't have this buff
        if (character.hasProfession(Profession.Mining) & !character.activeBuffsContains("Toughness")) {
            character.activeBuffsAdd("Toughness");
        } else if (character.activeBuffsContains("Toughness") && !character.hasProfession(Profession.Mining)) {
            character.getActiveBuffs().remove(Buff.getBuffByName("Toughness"));
        }
        // Skinners should always have this buff activated, others shouldn't have this buff
        if (character.hasProfession(Profession.Skinning) & !character.activeBuffsContains("Master of Anatomy")) {
            character.activeBuffsAdd("Master of Anatomy");
        } else if (character.activeBuffsContains("Master of Anatomy") && !character.hasProfession(Profession.Skinning)) {
            character.getActiveBuffs().remove(Buff.getBuffByName("Master of Anatomy"));
        }
        // Engineers should always have this buff activated IF the Primary is
        if (character.hasProfession(Profession.Engineering)) {
            List<String> list = java.util.Arrays.asList("Runic Mana Injector", "Runic Healing Injector");
            for (String name : list) {
                if (character.getActiveBuffs().contains(Buff.getBuffByName(name))
                        && !character.getActiveBuffs().contains(Buff.getBuffByName(name + " (Engineer Bonus)"))) {
                    character.getActiveBuffs().add(Buff.getBuffByName(name + " (Engineer Bonus)"));
                }
            }
        }
    }

    // C# Calculations.cs:736-780.
    /**
     * Rates one exact gemmed instance in the given slot: the baseline run empties the slot,
     * the comparison run equips {@code item}, and the result is the point difference. This is
     * the overload comparison charts use, because it distinguishes gemming and enchant
     * variants of the same item.
     *
     * <p>The baseline run is cached on the model keyed by character object and slot, exactly
     * like {@link #getItemCalculations(Item, Character, CharacterSlot)}. The pseudo-slots
     * {@link CharacterSlot#Gems} and {@link CharacterSlot#Metas} only use the cache key and
     * never write to the equipped slots. The passed character is not modified; the method
     * works on clones.</p>
     */
    public ComparisonCalculationBase getItemCalculations(ItemInstance item, Character character, CharacterSlot slot) {
        boolean useCache = character == cachedCharacter && slot == cachedSlot;
        Character characterWithSlotEmpty = null;
        if (!useCache)
            characterWithSlotEmpty = character.clone();
        Character characterWithNewItem = character.clone();

        if (slot != CharacterSlot.Metas && slot != CharacterSlot.Gems) {
            if (!useCache) characterWithSlotEmpty.set(slot, null);
            characterWithNewItem.set(slot, item);
        }

        CharacterCalculationsBase characterStatsWithSlotEmpty;
        if (useCache && cachedCharacterStatsWithSlotEmpty != null) {
            characterStatsWithSlotEmpty = cachedCharacterStatsWithSlotEmpty;
        } else {
            characterStatsWithSlotEmpty = getCharacterCalculations(characterWithSlotEmpty, null, false, false, false);
            cachedCharacter = character;
            cachedSlot = slot;
            cachedCharacterStatsWithSlotEmpty = characterStatsWithSlotEmpty;
        }

        CharacterCalculationsBase characterStatsWithNewItem = getCharacterCalculations(characterWithNewItem, null, false, false, false);

        ComparisonCalculationBase itemCalc = createNewComparisonCalculation();
        itemCalc.setItemInstance(item);
        itemCalc.setItem(item.getItem());
        itemCalc.setName(item.getItem() != null ? item.getItem().getName() : item.getGemmedId());
        itemCalc.setEquipped(character.get(slot) == item);
        itemCalc.setOverallPoints(characterStatsWithNewItem.getOverallPoints() - characterStatsWithSlotEmpty.getOverallPoints());
        float[] subPoints = new float[characterStatsWithNewItem.getSubPoints().length];
        for (int i = 0; i < characterStatsWithNewItem.getSubPoints().length; i++) {
            subPoints[i] = characterStatsWithNewItem.getSubPoints()[i] - characterStatsWithSlotEmpty.getSubPoints()[i];
        }
        itemCalc.setSubPoints(subPoints);
        return itemCalc;
    }

    // C# Calculations.cs:782-821. The empty-slot result is cached per character/slot: the
    // RestoSham comparison golden depends on the original call sequence (the model keeps
    // per-calculation carry-over fields).
    /**
     * Rates one item in the given slot by difference: runs the model once with the character
     * as-is and once with {@code additionalItem} added, and returns
     * {@code withItem - withoutItem} for the overall and each sub-point rating. The result
     * carries the item's name and {@code equipped} is false; it is the per-slot entry shown
     * in item comparison charts.
     *
     * <p>The baseline run (without the item) is cached on the model, keyed by character
     * object and slot; pass the same character instance and slot to reuse it, and call
     * {@link #clearCache()} after changing the character. In this overload the slot argument
     * is only a cache key: the item reaches the model through its {@code additionalItem}
     * parameter and is not written into either clone's slot array (see
     * {@link #getItemCalculations(ItemInstance, Character, CharacterSlot)} for the chart
     * overload that equips the exact gemmed instance). The passed character is not modified;
     * the method works on clones.</p>
     */
    public ComparisonCalculationBase getItemCalculations(Item additionalItem, Character character, CharacterSlot slot) {
        boolean useCache = character == cachedCharacter && slot == cachedSlot;
        Character characterWithSlotEmpty = null;
        if (!useCache)
            characterWithSlotEmpty = character.clone();
        Character characterWithNewItem = character.clone();

        CharacterCalculationsBase characterStatsWithSlotEmpty;
        if (useCache && cachedCharacterStatsWithSlotEmpty != null) {
            characterStatsWithSlotEmpty = cachedCharacterStatsWithSlotEmpty;
        } else {
            characterStatsWithSlotEmpty = getCharacterCalculations(characterWithSlotEmpty, null, false, false, false);
            cachedCharacter = character;
            cachedSlot = slot;
            cachedCharacterStatsWithSlotEmpty = characterStatsWithSlotEmpty;
        }

        CharacterCalculationsBase characterStatsWithNewItem = getCharacterCalculations(characterWithNewItem, additionalItem, false, false, false);

        ComparisonCalculationBase itemCalc = createNewComparisonCalculation();
        itemCalc.setItem(additionalItem);
        itemCalc.setName(additionalItem.getName());
        itemCalc.setEquipped(false);
        itemCalc.setOverallPoints(characterStatsWithNewItem.getOverallPoints() - characterStatsWithSlotEmpty.getOverallPoints());
        float[] subPoints = new float[characterStatsWithNewItem.getSubPoints().length];
        for (int i = 0; i < characterStatsWithNewItem.getSubPoints().length; i++) {
            subPoints[i] = characterStatsWithNewItem.getSubPoints()[i] - characterStatsWithSlotEmpty.getSubPoints()[i];
        }
        itemCalc.setSubPoints(subPoints);
        return itemCalc;
    }

    // C# Calculations.cs:1039-1088 GetCharacterComparisonCalculations (three overloads).
    /**
     * Runs the model for {@code character} and returns its points as a difference against
     * {@code baseCalculations}: overall and sub-points are
     * {@code newCalculation - baseCalculations}. The comparison entry gets the given name, a
     * blank item carrying that name (so charts can show a tooltip), and the equipped flag.
     */
    public ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase baseCalculations, Character character, String name, boolean equipped) {
        CharacterCalculationsBase characterCalculations = getCharacterCalculations(character, null, false, true, false);
        ComparisonCalculationBase comparisonCalculations = createNewComparisonCalculation();
        comparisonCalculations.setName(name);
        Item item = new Item();
        item.setName(name);
        comparisonCalculations.setItem(item);
        comparisonCalculations.setEquipped(equipped);
        comparisonCalculations.setOverallPoints(characterCalculations.getOverallPoints() - baseCalculations.getOverallPoints());
        float[] subPoints = new float[characterCalculations.getSubPoints().length];
        for (int i = 0; i < characterCalculations.getSubPoints().length; i++) {
            subPoints[i] = characterCalculations.getSubPoints()[i] - baseCalculations.getSubPoints()[i];
        }
        comparisonCalculations.setSubPoints(subPoints);
        return comparisonCalculations;
    }

    /**
     * Wraps an already computed result as a comparison entry with its absolute points: the
     * overall and sub-points are copied from {@code calculations} unchanged. Used when the
     * entry is a reference (for example the currently equipped set in a chart), not a delta.
     */
    public ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase calculations, String name, boolean equipped) {
        ComparisonCalculationBase comparisonCalculations = createNewComparisonCalculation();
        comparisonCalculations.setName(name);
        Item item = new Item();
        item.setName(name);
        comparisonCalculations.setItem(item);
        comparisonCalculations.setEquipped(equipped);
        comparisonCalculations.setOverallPoints(calculations.getOverallPoints());
        float[] subPoints = new float[calculations.getSubPoints().length];
        for (int i = 0; i < calculations.getSubPoints().length; i++) {
            subPoints[i] = calculations.getSubPoints()[i];
        }
        comparisonCalculations.setSubPoints(subPoints);
        return comparisonCalculations;
    }

    /**
     * Difference of two already computed results: overall and sub-points are
     * {@code newCalculation - baseCalculation}. In addition to the equipped flag the entry
     * carries the {@code partequipped} flag, marking a gear set in which only some of the
     * compared items are actually equipped.
     */
    public ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase baseCalculation, CharacterCalculationsBase newCalculation, String name, boolean equipped, boolean partequipped) {
        ComparisonCalculationBase comparisonCalculations = createNewComparisonCalculation();
        comparisonCalculations.setName(name);
        Item item = new Item();
        item.setName(name);
        comparisonCalculations.setItem(item);
        comparisonCalculations.setEquipped(equipped);
        comparisonCalculations.setPartEquipped(partequipped);
        comparisonCalculations.setOverallPoints(newCalculation.getOverallPoints() - baseCalculation.getOverallPoints());
        float[] subPoints = new float[newCalculation.getSubPoints().length];
        for (int i = 0; i < newCalculation.getSubPoints().length; i++) {
            subPoints[i] = newCalculation.getSubPoints()[i] - baseCalculation.getSubPoints()[i];
        }
        comparisonCalculations.setSubPoints(subPoints);
        return comparisonCalculations;
    }

    /**
     * Computes the relative value of every stat the current model considers relevant, the
     * data behind the original "Relative Stats" chart.
     *
     * <p>The method builds a probe {@code Stats} whose additive values are all {@code 1},
     * asks the model which of those it uses ({@link Calculations#getRelevantStats}), and
     * estimates each one with {@link #getRelativeStatValue(Character, Stats.StatProperty,
     * Item, float)} at scale {@code 1}. Entries that come out {@code null} or with zero
     * overall points are dropped. Models that expose a base stat (for example Enhancement's
     * Attack Power) set the divisor so that stat reads {@code 1.00}; the whole list,
     * sub-points included, is divided by it.</p>
     *
     * <p>The order is the stat-name order of {@code Stats.values()} (the C# {@code Values}
     * SortedList order), not rating order.</p>
     */
    public static ComparisonCalculationBase[] getRelativeStatValues(Character character) {
        List<ComparisonCalculationBase> result = new ArrayList<ComparisonCalculationBase>();
        Stats allStats = new Stats();
        Arrays.fill(allStats.getRawAdditiveData(), 1f);

        float divisor = 1.0f;
        SortedMap<String, Float> relevantStats = Calculations.getRelevantStats(allStats)
                .values(value -> value > 0f);
        for (Map.Entry<String, Float> pair : relevantStats.entrySet()) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(pair.getKey());
            ComparisonCalculationBase ccb = getRelativeStatValue(character, property);
            if (ccb != null && ccb.getOverallPoints() != 0f) {
                result.add(ccb);
                if (ccb.getBaseStatOption(character) && ccb.getName().equals(ccb.getBaseStat())) {
                    divisor = ccb.getOverallPoints();
                }
            }
        }
        if (divisor != 1.0f && divisor != 0.0f) {
            for (ComparisonCalculationBase ccb : result) {
                // Mutate the live array like the C# code: setSubPoints() is not a plain field
                // write in some models (TankDK recomputes overall from the sub-points), and the
                // original deliberately avoids that setter here.
                float[] subPoints = ccb.getSubPoints();
                for (int i = 0; i < subPoints.length; i++) {
                    subPoints[i] /= divisor;
                }
                ccb.setOverallPoints(ccb.getOverallPoints() / divisor);
            }
        }
        return result.toArray(new ComparisonCalculationBase[0]);
    }

    /**
     * Convenience overload of {@link #getRelativeStatValue(Character, Stats.StatProperty,
     * Item, float)} using a throw-away item with empty stats and scale {@code 1}, as the
     * original two-argument method does.
     */
    public static ComparisonCalculationBase getRelativeStatValue(Character character, Stats.StatProperty property) {
        Item item = new Item();
        item.setStats(new Stats());
        return getRelativeStatValue(character, property, item, 1.0f);
    }

    /**
     * Estimates what a stat point is worth on this character, returning a comparison entry
     * whose overall and sub-points are the rating change per point of the stat multiplied by
     * {@code scale} (it answers questions like "how much is one spell power worth?").
     *
     * <p>The estimate is a finite difference around the value {@code item} already has:
     * perturb the stat, re-run the model, divide the rating change by the stat change. Many
     * stats have thresholds (caps) where the rating jumps; when the one-point interval is not
     * continuous, the code searches for the nearest jumps at least {@code minRange} away from
     * the current value (upper bound {@code minRange + 10}) and measures across them instead.
     * A property whose {@code minRange} is negative is not suitable and yields
     * {@code null}.</p>
     *
     * <p>Side effect: while probing, the method writes the stat into {@code item}'s stats and
     * invalidates the item's cache, leaving the last probed value in place. Treat the item's
     * stats as unspecified after the call.</p>
     */
    public static ComparisonCalculationBase getRelativeStatValue(Character character, Stats.StatProperty property, Item item, float scale) {
        final float resolution = 0.005f;
        ComparisonCalculationBase ccb = null;
        float minRange = property.minRange();
        if (minRange >= 0) {
            CharacterCalculationsBase charCalcsBase = Calculations.getCharacterCalculations(character, item, false, false, false);
            float basePoints = charCalcsBase.getOverallPoints();
            float baseOffset = property.accessor().get(item.getStats());
            float upperChangePoint = baseOffset + 1.0f;
            float lowerChangePoint = baseOffset + 0.0f;
            if (!propertyValueIsContinuous(character, baseOffset, basePoints, property, item, resolution)) {
                upperChangePoint = getStatValueUpperChangePoint(character, basePoints, property, item, baseOffset + minRange + resolution, baseOffset + minRange + 10.0f, resolution);
                lowerChangePoint = getStatValueLowerChangePoint(character, basePoints, property, item, baseOffset - minRange - 10.0f, baseOffset - minRange, resolution);
            }
            float changePointDifference = upperChangePoint - lowerChangePoint;
            property.mutator().set(item.getStats(), upperChangePoint);
            item.invalidateCachedData();
            CharacterCalculationsBase charCalcsUpper = Calculations.getInstance().getCharacterCalculations(character, item, false, false, false);
            property.mutator().set(item.getStats(), lowerChangePoint);
            item.invalidateCachedData();
            CharacterCalculationsBase charCalcsLower = Calculations.getInstance().getCharacterCalculations(character, item, false, false, false);
            ccb = Calculations.createNewComparisonCalculation();
            ccb.setName(Extensions.displayName(property));
            ccb.setSubPoints(new float[charCalcsUpper.getSubPoints().length]);
            float[] subPoints = ccb.getSubPoints();
            for (int i = 0; i < charCalcsUpper.getSubPoints().length; i++) {
                subPoints[i] = scale * (charCalcsUpper.getSubPoints()[i] - charCalcsLower.getSubPoints()[i]) / changePointDifference;
                ccb.setOverallPoints(ccb.getOverallPoints() + subPoints[i]);
            }
            ccb.setDescription("If you had " + NetFormat.netFloatToString(scale) + " more" + ccb.getName());
        }
        return ccb;
    }

    private static boolean propertyValueIsContinuous(Character character, float baseOffset, float basePoints, Stats.StatProperty property, Item tagItem, float resolution) {
        boolean continuous;
        property.mutator().set(tagItem.getStats(), baseOffset + resolution);
        tagItem.invalidateCachedData();
        continuous = basePoints != Calculations.getInstance().getCharacterCalculations(character, tagItem, false, false, false).getOverallPoints();
        if (continuous) {
            property.mutator().set(tagItem.getStats(), baseOffset - resolution);
            tagItem.invalidateCachedData();
            continuous = basePoints != Calculations.getInstance().getCharacterCalculations(character, tagItem, false, false, false).getOverallPoints();
        }
        return continuous;
    }

    private static float getStatValueUpperChangePoint(Character character, float basePoints, Stats.StatProperty property, Item tagItem, float lowerBound, float upperBound, float resolution) {
        return getStatValueChangePoint(character, basePoints, property, tagItem, lowerBound, upperBound, resolution, true);
    }

    private static float getStatValueLowerChangePoint(Character character, float basePoints, Stats.StatProperty property, Item tagItem, float lowerBound, float upperBound, float resolution) {
        return getStatValueChangePoint(character, basePoints, property, tagItem, lowerBound, upperBound, resolution, false);
    }

    /**
     * Binary search for the point where the model's overall points stop matching {@code basePoints}.
     * {@code upperOnEqual} selects which half to keep when the midpoint still matches: the upper
     * search recurses upward, the lower search downward.
     */
    private static float getStatValueChangePoint(Character character, float basePoints, Stats.StatProperty property,
            Item tagItem, float lowerBound, float upperBound, float resolution, boolean upperOnEqual) {
        if ((upperBound - lowerBound) <= resolution) {
            return upperBound;
        }
        float midPoint = (upperBound + lowerBound) / 2f;
        property.mutator().set(tagItem.getStats(), midPoint);
        tagItem.invalidateCachedData();
        float newOverall = Calculations.getInstance().getCharacterCalculations(character, tagItem, false, false, false).getOverallPoints();
        if (basePoints == newOverall) {
            return upperOnEqual
                    ? getStatValueChangePoint(character, basePoints, property, tagItem, midPoint, upperBound, resolution, true)
                    : getStatValueChangePoint(character, basePoints, property, tagItem, lowerBound, midPoint, resolution, false);
        }
        return upperOnEqual
                ? getStatValueChangePoint(character, basePoints, property, tagItem, lowerBound, midPoint, resolution, true)
                : getStatValueChangePoint(character, basePoints, property, tagItem, midPoint, upperBound, resolution, false);
    }

    /**
     * Returns whether {@code item} (non-null) may occupy {@code slot}. The base rule is
     * uniqueness: unless {@code ignoreUnique} is true, a unique item is rejected when the
     * opposite half of a paired slot (Finger1/Finger2, Trinket1/Trinket2, MainHand/OffHand)
     * already holds the same item id, or holds an id listed in this item's unique-id set.
     * The item's own slot-type check ({@code item.fitsInSlot}) is always applied last.
     */
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (item != null && item.isUnique() && !ignoreUnique) {
            CharacterSlot otherSlot;
            switch (slot) {
                case Finger1:
                    otherSlot = CharacterSlot.Finger2;
                    break;
                case Finger2:
                    otherSlot = CharacterSlot.Finger1;
                    break;
                case Trinket1:
                    otherSlot = CharacterSlot.Trinket2;
                    break;
                case Trinket2:
                    otherSlot = CharacterSlot.Trinket1;
                    break;
                case MainHand:
                    otherSlot = CharacterSlot.OffHand;
                    break;
                case OffHand:
                    otherSlot = CharacterSlot.MainHand;
                    break;
                default:
                    otherSlot = CharacterSlot.None;
                    break;
            }
            if (otherSlot != CharacterSlot.None) {
                ItemInstance other = character.get(otherSlot);
                if (other != null && (item.getId() == other.getItem().getId() || (item.getUniqueId() != null && item.getUniqueId().contains(other.getId())))) return false;
            }
        }
        return item.fitsInSlot(slot);
    }

    /**
     * Returns whether {@code enchant} may be applied given the slot kind. The base rule is
     * the enchant's own slot check; models override this to forbid class-specific enchants.
     */
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        return enchant.fitsInSlot(slot);
    }

    /**
     * Whether the off-hand item contributes stats. The base rule: it is ignored only when the
     * main hand holds a two-handed weapon, since there is no off-hand then. Overridden by
     * models only for special cases; used by {@link #accumulateItemStats}.
     */
    public boolean includeOffHandInCalculations(Character character) {
        return character.get(CharacterSlot.MainHand) == null || character.get(CharacterSlot.MainHand).getSlot() != ItemSlot.TwoHand;
    }

    // C# Calculations.cs:1371-1384
    /**
     * Combined enchant gate: the class may use it ({@link #isEnchantAllowedForClass}), the
     * character's professions permit it when profession gating is on
     * ({@link #isProfEnchantRelevant}), and it carries stats the model cares about. Any
     * exception during the checks is treated as "not relevant".
     */
    public boolean isEnchantRelevant(Enchant enchant, Character character) {
        try {
            return isEnchantAllowedForClass(enchant, character.getCharacterClass())
                    && isProfEnchantRelevant(enchant, character)
                    && hasRelevantStats(enchant.stats);
        } catch (Exception e) {
            return false;
        }
    }

    // C# Calculations.cs:713 `public abstract Stats GetRelevantStats(Stats stats)`. As with
    // hasRelevantStats the Java port keeps a permissive virtual default for skeletal test models;
    // every real model overrides it with its stat whitelist.
    /**
     * Projects {@code stats} onto the stats this model uses, dropping the rest. This is the
     * whitelist behind {@link #hasRelevantStats(Stats)} and the source of the stat list for
     * {@link #getRelativeStatValues(Character)}. The default returns a copy of the input so
     * skeletal models keep every stat; real models override it.
     */
    public Stats getRelevantStats(Stats stats) {
        return stats.copy();
    }

    /**
     * Names of the glyphs this model actually reads, or {@code null} to include every glyph. The
     * default is {@code null}; models with a curated glyph list override it (the C# chart used the
     * same list to filter the glyph comparison chart).
     */
    public List<String> getRelevantGlyphs() {
        return null;
    }

    // C# Calculations.cs:720 `public abstract bool HasRelevantStats(Stats stats)`. The Java port keeps a
    // virtual default so test doubles that do not filter stats stay compilable (cf. getRelevantItemTypes).
    /**
     * Whether any positive contribution in {@code stats} is a stat this model uses. This is
     * the model's definition of "relevant" and drives item, buff and enchant filtering. The
     * default answers {@code true} (see the note above); real models override it with their
     * stat list.
     */
    public boolean hasRelevantStats(Stats stats) {
        return true;
    }

    // C# Calculations.cs:515 `public abstract CharacterClass TargetClass { get; }`. PARTIAL: Mage is
    // the only ported model, so the virtual default returns Mage until Stage B adds the other models.
    /**
     * The character class this model simulates; used to reject class-restricted gear in
     * {@link #isItemRelevant}. The Java default answers {@link CharacterClass#Mage} until
     * further models are ported; each model should override it.
     */
    public CharacterClass getTargetClass() {
        return CharacterClass.Mage;
    }

    // C# Calculations.cs:1252-1275 IsItemRelevant.
    /**
     * First filter applied to every item from the item cache: the item's required-classes
     * string (spaces removed) must contain the target class name, the item type must be in
     * {@link #getRelevantItemTypes()}, and the item must either have relevant stats or belong
     * to a set whose set bonus is relevant. Any exception during the checks is treated as
     * "not relevant".
     */
    public boolean isItemRelevant(Item item) {
        try {
            boolean b = (item.getRequiredClasses() == null || item.getRequiredClasses().isEmpty()
                    || item.getRequiredClasses().replace(" ", "").contains(getTargetClass().toString()));
            b &= ItemFilter.getRelevantItemTypesList(this).contains(item.getType());
            if (b) {
                if (hasRelevantStats(item.getStats())) {
                    return true;
                }
                if (item.getSetName() != null && !item.getSetName().isEmpty()) {
                    for (Buff buff : Buff.getRelevantSetBonuses()) {
                        if (java.util.Objects.equals(buff.setName, item.getSetName())) return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // C# Calculations.cs:1278-1288 IsBuffRelevant.
    /**
     * Whether a buff should be offered for this character: when the shared profession gating
     * flag is on, the character must have one of the professions listed on the buff, and the
     * buff's total stats must be relevant to the model. Any exception is treated as
     * "not relevant".
     */
    public boolean isBuffRelevant(Buff buff, Character character) {
        try {
            if (character != null && getHideProfEnchants() && !character.hasProfession(buff.getProfessions()))
                return false;
            return hasRelevantStats(buff.getTotalStats());
        } catch (Exception e) {
            return false;
        }
    }

    // C# Calculations.cs:1292-1299
    /**
     * Class restriction for enchants: the death-knight rune enchants (names starting with
     * "Rune of ") are rejected for every other class. Overridable.
     */
    public boolean isEnchantAllowedForClass(Enchant enchant, CharacterClass characterClass) {
        if (enchant.name.startsWith("Rune of ") && (characterClass != CharacterClass.DeathKnight))
            return false;

        return true;
    }

    // C# Calculations.cs:1309-1367. Rawr.Properties.GeneralSettings.Default.HideProfEnchants is a
    // WinForms user setting; its default (True) is kept in the rawr-core flag below.
    /**
     * Profession gate for enchants, active while {@code hideProfEnchants} is true (the
     * default): enchanter ring enchants require Enchanting, engineering tinkers require
     * Engineering, inscription shoulder enchants require Inscription, leatherworking leg
     * linings require Leatherworking, and tailoring embroideries/spellthreads require
     * Tailoring. The checks are substring matches on the enchant name, exactly as in the
     * original. An exception reports the enchant as relevant.
     */
    public boolean isProfEnchantRelevant(Enchant enchant, Character character) {
        try {
            String name = enchant.name;
            if (hideProfEnchants) {
                if (!character.hasProfession(Profession.Enchanting)) {
                    if (enchant.slot == ItemSlot.Finger) {
                        return false;
                    }
                }
                if (!character.hasProfession(Profession.Engineering)) {
                    if (name.contains("Mind Amplification Dish")
                            || name.contains("Flexweave Underlay")
                            || name.contains("Hyperspeed Accelerators")
                            || name.contains("Reticulated Armor Webbing")
                            || name.contains("Nitro Boosts")
                            || name.contains("Springy Arachnoweave")
                            || name.contains("Hand-Mounted")) {
                        return false;
                    }
                }
                if (!character.hasProfession(Profession.Inscription)) {
                    if (name.contains("Master's")
                            && !name.contains("Spellthread")) {
                        return false;
                    }
                }
                if (!character.hasProfession(Profession.Leatherworking)) {
                    if (name.contains("Fur Lining")
                            || name.contains("Nerubian Leg Reinforcements")) {
                        return false;
                    }
                }
                if (!character.hasProfession(Profession.Tailoring)) {
                    if (name.contains("Embroidery")
                            || name.contains("Sanctified Spellthread")
                            || name.contains("Master's Spellthread")) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // C# Calculations.cs:1433-1436
    /**
     * Whether the model consumes ammunition (only hunters do). The default is false; a model
     * that uses ammo overrides it.
     */
    public boolean canUseAmmo() {
        return false;
    }

    /**
     * Process-wide replacement for the original WinForms user setting
     * {@code GeneralSettings.Default.HideProfEnchants}: when true (the default) enchants and
     * buffs tied to a profession are hidden from characters that lack it. Static and mutable;
     * set it once at startup rather than per calculation.
     */
    private static volatile boolean hideProfEnchants = true;

    public static boolean getHideProfEnchants() {
        return hideProfEnchants;
    }

    public static void setHideProfEnchants(boolean value) {
        hideProfEnchants = value;
    }
}
