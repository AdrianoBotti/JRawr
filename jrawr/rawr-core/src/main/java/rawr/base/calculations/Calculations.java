// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Calculations.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: explicit model registry (AGENTS phase 2: reflection replaced by registration) and the static
// helpers needed by the base item/character code. UI/settings/chart members are omitted.
package rawr.base.calculations;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static facade and explicit model registry of the engine: it maps model names to their
 * {@link CalculationsBase} implementations and forwards the model-dependent helpers to the
 * currently loaded model.
 *
 * <p>A <b>model</b> here is the simulation of one player specialization (spec) in the game
 * &mdash; for example the Arcane Mage model. A model turns a resolved {@link Character}
 * (gear, buffs, talents) into a {@link CharacterCalculationsBase} result holding the
 * "ratings": the model score (overall points) and its breakdown into sub-points, which the
 * user interface displays and the optimizer maximizes. The models themselves live outside
 * this module in {@code rawr-models}.</p>
 *
 * <h2>Why there is a registry</h2>
 *
 * <p>The original C# application discovered its models at startup by scanning
 * {@code Data/*.dll}, loading every assembly with reflection and reading a
 * {@code RawrModelInfo} attribute from each {@link CalculationsBase} subclass. The Java port
 * replaces that mechanism with explicit registration
 * ({@link #registerModel(String, Class)}), so the lookup tables are plain maps rather than
 * the result of an assembly scan.</p>
 *
 * <ul>
 *   <li>{@code MODELS} maps a model name to its class. It is a {@code LinkedHashMap} so
 *       registration order is stable and can be used to list models deterministically.</li>
 *   <li>{@code MODEL_CACHE} maps a class to its one instantiated model. Models are stateful
 *       (caches, per-calculation carry-over fields) and this single instance is shared
 *       process-wide, so any mutable state kept on a model is global. Register and load all
 *       models before starting concurrent work; the maps are not synchronized.</li>
 *   <li>{@code instance} is the model selected by {@link #loadModel}, if any. Helpers such
 *       as {@link #getCharacterCalculations(Character)} dereference it directly and fail with
 *       a {@code NullPointerException} when no model is loaded, while the filtering helpers
 *       ({@link #hasRelevantStats}, {@link #itemFitsInSlot}, ...) return a neutral value.</li>
 * </ul>
 *
 * @see CalculationsBase
 * @see CharacterCalculationsBase
 */
public final class Calculations {
    private static final Map<String, Class<? extends CalculationsBase>> MODELS =
            new LinkedHashMap<String, Class<? extends CalculationsBase>>();
    private static final Map<Class<?>, CalculationsBase> MODEL_CACHE =
            new HashMap<Class<?>, CalculationsBase>();
    private static CalculationsBase instance;

    private Calculations() {
    }

    /**
     * The model selected by the last {@link #loadModel} call, or {@code null} if no model has
     * been loaded yet.
     */
    public static CalculationsBase getInstance() {
        return instance;
    }

    /**
     * Registers a {@link CalculationsBase} subclass under a lookup name, normally the
     * class/spec name used in saved character files ("Mage", ...). The class must have a
     * no-argument constructor; {@link #getModel(String)} instantiates it lazily on first use.
     *
     * <p>Registering the same name twice silently replaces the previous class, and the name
     * keeps its original position in the registration order.</p>
     */
    public static void registerModel(String name, Class<? extends CalculationsBase> type) {
        MODELS.put(name, type);
    }

    public static boolean isModelRegistered(String name) {
        return MODELS.containsKey(name);
    }

    /**
     * Returns the shared instance registered under {@code model}, constructing and caching it
     * on first use. On construction its {@code name} field is set to the registry key, so the
     * same class registered under two names would share one instance under the first name
     * requested.
     *
     * @throws IllegalArgumentException if no model is registered under that name
     * @throws IllegalStateException if the registered class cannot be instantiated
     */
    public static CalculationsBase getModel(String model) {
        Class<? extends CalculationsBase> type = MODELS.get(model);
        if (type == null) {
            throw new IllegalArgumentException("Model not registered: " + model);
        }
        CalculationsBase cached = MODEL_CACHE.get(type);
        if (cached == null) {
            try {
                cached = type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot instantiate model " + type.getName(), e);
            }
            cached.name = model;
            MODEL_CACHE.put(type, cached);
        }
        return cached;
    }

    /**
     * Looks up a registered model by its exact class (identity comparison, not
     * {@code equals}) and delegates to {@link #getModel(String)}. The lookup is a linear scan
     * because the registry is expected to hold only the handful of class/spec models.
     *
     * @throws IllegalArgumentException if the class was never registered
     */
    public static CalculationsBase getModel(Class<? extends CalculationsBase> type) {
        for (Map.Entry<String, Class<? extends CalculationsBase>> entry : MODELS.entrySet()) {
            if (entry.getValue() == type) {
                return getModel(entry.getKey());
            }
        }
        throw new IllegalArgumentException("Model type not registered: " + type.getName());
    }

    /**
     * Makes the registered model of the given class the process-wide current model. Every
     * other static helper on this class delegates to that instance until the next call. The
     * class must already have been registered with {@link #registerModel(String, Class)}.
     *
     * @throws IllegalArgumentException if the class was never registered
     */
    public static void loadModel(Class<? extends CalculationsBase> type) {
        CalculationsBase next = getModel(type);
        CalculationsBase previous = instance;
        instance = next;
        if (previous != null && !java.util.Objects.equals(previous.getName(), next.getName())) {
            // The relevant-item list is model-specific; drop it so a model switch cannot serve
            // the previous class's items to item pickers, charts or the optimizer.
            ItemCache.invalidateRelevantCaches();
        }
    }

    /**
     * Replaces the cached instance of a registered model with a fresh one and makes it current.
     * Models keep per-calculation carry-over state on the shared instance; long-running processes
     * (the web optimizer, repeated calculations) use this to isolate a top-level run from the
     * previous one. Static model state is not reset by this method.
     *
     * @throws IllegalArgumentException if no model is registered under that name
     * @throws IllegalStateException if the registered class cannot be instantiated
     */
    public static CalculationsBase loadFreshModel(String model) {
        Class<? extends CalculationsBase> type = MODELS.get(model);
        if (type == null) {
            throw new IllegalArgumentException("Model not registered: " + model);
        }
        CalculationsBase fresh;
        try {
            fresh = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate model " + type.getName(), e);
        }
        fresh.name = model;
        CalculationsBase previous = instance;
        MODEL_CACHE.put(type, fresh);
        instance = fresh;
        if (previous != null && !java.util.Objects.equals(previous.getName(), fresh.getName())) {
            ItemCache.invalidateRelevantCaches();
        }
        return fresh;
    }

    /**
     * Runs the current model's simulation for the character with all boolean hints left at
     * their defaults; see {@link #getCharacterCalculations(Character, Item, boolean, boolean, boolean)}.
     */
    public static CharacterCalculationsBase getCharacterCalculations(Character character) {
        return instance.getCharacterCalculations(character);
    }

    /**
     * Facade for the model's main calculation entry point. The booleans are hints that let a
     * model trade accuracy of cached intermediate state for speed; they are not results.
     *
     * @param character the character to simulate
     * @param additionalItem an item treated as worn in addition to the slotted gear, or
     *     {@code null}; used for gems and other slotless comparisons
     * @param referenceCalculation true when this run defines the baseline against which later
     *     comparison runs are measured, so a model may keep carry-over state for it
     * @param significantChange true when the change from the previous reference run can alter
     *     the character's behavior (talents, glyphs), so cached intermediary results must not
     *     be reused
     * @param needsDisplayCalculations false when only the rating numbers are needed and the
     *     model may skip building the human-readable display map
     * @return the model's result object
     */
    public static CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        return instance.getCharacterCalculations(character, additionalItem, referenceCalculation, significantChange, needsDisplayCalculations);
    }

    /**
     * Creates a new, empty comparison entry of the model-specific type returned by
     * {@link CalculationsBase#createNewComparisonCalculation()}. Callers fill in the name,
     * points and item themselves.
     */
    public static ComparisonCalculationBase createNewComparisonCalculation() {
        return instance.createNewComparisonCalculation();
    }

    /**
     * Facade for {@link CalculationsBase#getItemCalculations(ItemInstance, Character,
     * CharacterSlot)}: rates an exact gemmed instance against an empty slot.
     */
    public static ComparisonCalculationBase getItemCalculations(ItemInstance item, Character character, CharacterSlot slot) {
        return instance.getItemCalculations(item, character, slot);
    }

    /**
     * Facade for {@link CalculationsBase#getItemCalculations(Item, Character, CharacterSlot)}:
     * rates an item template added as an extra worn item.
     */
    public static ComparisonCalculationBase getItemCalculations(Item item, Character character, CharacterSlot slot) {
        return instance.getItemCalculations(item, character, slot);
    }

    public static ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase baseCalculations, Character character, String name, boolean equipped) {
        return instance.getCharacterComparisonCalculations(baseCalculations, character, name, equipped);
    }

    public static ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase calculations, String name, boolean equipped) {
        return instance.getCharacterComparisonCalculations(calculations, name, equipped);
    }

    public static ComparisonCalculationBase getCharacterComparisonCalculations(CharacterCalculationsBase baseCalculation, CharacterCalculationsBase newCalculation, String name, boolean equipped, boolean partequipped) {
        return instance.getCharacterComparisonCalculations(baseCalculation, newCalculation, name, equipped, partequipped);
    }

    /**
     * Deserializes a model-specific calculation-options XML fragment. Returns {@code null}
     * when no model is loaded; otherwise delegates to
     * {@link CalculationsBase#deserializeDataObject(String)}.
     */
    public static ICalculationOptionBase deserializeDataObject(String xml) {
        return instance != null ? instance.deserializeDataObject(xml) : null;
    }

    /**
     * Facade for {@link CalculationsBase#getRelevantStats(Stats)}. Returns an empty snapshot
     * when no model is loaded, so callers such as the relative-stat chart degrade to no rows
     * instead of throwing.
     */
    public static Stats getRelevantStats(Stats stats) {
        return instance != null ? instance.getRelevantStats(stats) : new Stats();
    }

    // C# Calculations.cs:327-343 static HasRelevantStats/IsBuffRelevant helpers.
    /**
     * Null-safe facade for {@link CalculationsBase#hasRelevantStats(Stats)}: returns
     * {@code false} when no model is loaded.
     */
    public static boolean hasRelevantStats(Stats stats) {
        return instance != null && instance.hasRelevantStats(stats);
    }

    /**
     * Null-safe facade for {@link CalculationsBase#isBuffRelevant(Buff, Character)}: returns
     * {@code false} when no model is loaded.
     */
    public static boolean isBuffRelevant(Buff buff, Character character) {
        return instance != null && instance.isBuffRelevant(buff, character);
    }

    public static String getCharacterStatsString(Character character) {
        return instance.getCharacterStatsString(character);
    }

    /**
     * Null-safe facade for {@link CalculationsBase#itemFitsInSlot(Item, Character, CharacterSlot, boolean)}:
     * returns {@code false} when no model is loaded and also when the base check rejects the item.
     */
    public static boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        return instance != null ? instance.itemFitsInSlot(item, character, slot, ignoreUnique) : false;
    }

    // C# Calculations.cs:318-320 static UpdateProfessions delegates to the current model instance
    // (the base CalculationsBase implementation; no Java model overrides it).
    public static void updateProfessions(Character character) {
        if (instance != null) {
            instance.updateProfessions(character);
        }
    }

    /**
     * Removes from {@code activeBuffs} every buff that conflicts with {@code buff}, plus the
     * "improvements" (upgraded versions, e.g. a stronger flask) of each removed buff.
     * Conflicts are declared between buff groups, so activating a battle elixir removes an
     * active conflicting one. The buff itself is never removed, and {@code null} does
     * nothing. The list is mutated in place with a backwards index walk that keeps the loop
     * valid across multiple removals.
     */
    public static void removeConflictingBuffs(List<Buff> activeBuffs, Buff buff) {
        if (buff != null) {
            for (int i = 0; i < activeBuffs.size(); i++) {
                if (activeBuffs.get(i) != buff) {
                    Buff b2 = activeBuffs.get(i);
                    for (String buffName : b2.getConflictingBuffs()) {
                        if (buff.getConflictingBuffs().contains(buffName)) {
                            activeBuffs.remove(i);
                            i--;
                            for (Buff improvement : b2.improvements) {
                                int j = activeBuffs.indexOf(improvement);
                                if (j >= 0) {
                                    activeBuffs.remove(j);
                                    if (j <= i) i--;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Null-safe facade for {@link CalculationsBase#enchantFitsInSlot(Enchant, Character, ItemSlot)}:
     * returns {@code false} when no model is loaded.
     */
    public static boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        if (instance != null)
            return instance.enchantFitsInSlot(enchant, character, slot);
        return false;
    }

    /**
     * Null-safe facade for {@link CalculationsBase#includeOffHandInCalculations(Character)}:
     * returns {@code false} when no model is loaded, meaning an off-hand item is ignored.
     */
    public static boolean includeOffHandInCalculations(Character character) {
        if (instance != null)
            return instance.includeOffHandInCalculations(character);
        return false;
    }
}
