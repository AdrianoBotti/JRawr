// New documentation (not ported from C#): package-level guide.
/**
 * The model framework: the contract every simulation model implements, the explicit model registry
 * that replaces C# reflection, and the result/optimization-goal types the framework passes around.
 *
 * <h2>The model contract</h2>
 *
 * <p>A <b>model</b> is the simulation for one specialization (Arcane Mage, Fury Warrior, ...). It
 * extends {@link rawr.base.calculations.CalculationsBase} and returns a
 * {@link rawr.base.calculations.CharacterCalculationsBase} from its calculation entry point: the
 * ratings the user sees (DPS, HPS, threat, survival, ...), the character's final
 * {@link rawr.base.stats.Stats}, display strings and comparison charts. {@code CalculationsBase}
 * supplies the shared helpers (item/buff stat accumulation, relevance and slot-fit checks, option
 * handling, profession updates); the per-model overrides decide the simulation itself.</p>
 *
 * <h2>Registry</h2>
 *
 * <p>{@link rawr.base.calculations.Calculations} is the static facade. Models register themselves
 * by name and class ({@link rawr.base.calculations.Calculations#registerModel(String, Class)})
 * instead of being discovered by reflection; the registry is queried by the UI, the character
 * loader and the optimizer to find the model for a character.</p>
 *
 * <h2>Results and goals</h2>
 *
 * <ul>
 *   <li>{@link rawr.base.calculations.CharacterCalculationsBase} &mdash; one finished calculation:
 *       overall rating, named sub-points, stats, display map, charts.</li>
 *   <li>{@link rawr.base.calculations.ComparisonCalculationBase} &mdash; one entry in an item
 *       comparison (the item, its rating, the differences against the current item).
 *       {@link rawr.base.calculations.ComparisonCalculationUpgrades} is the optimizer's variant.</li>
 *   <li>{@link rawr.base.calculations.OptimizationRequirement},
 *       {@link rawr.base.calculations.OptimizationMethod},
 *       {@link rawr.base.calculations.GreedyOptimizationMethod},
 *       {@link rawr.base.calculations.OptimizerRangeValidatorBase} &mdash; what to optimize for,
 *       how hard to search, and the constraints/penalties the optimizer enforces.</li>
 *   <li>{@link rawr.base.calculations.ICharacterCalculationOptions},
 *       {@link rawr.base.calculations.ICalculationOptionBase} &mdash; the option objects each
 *       model defines (fight length, rotation, ...) and their XML round-trip contract.</li>
 * </ul>
 */
package rawr.base.calculations;
