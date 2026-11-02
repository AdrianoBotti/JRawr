// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.optimizer.AvailableItemGenerator;
import rawr.base.optimizer.ItemInstanceOptimizer;

/**
 * Controls how thoroughly the optimizers' direct-upgrade refinement evaluates item, gem and
 * enchant alternatives for a single slot. Besides the main search, an optimizer repeatedly
 * tries to improve the current best gear set by swapping one slot's item; this setting trades
 * search time against the quality of that localized search.
 *
 * <p>It is read by {@code ItemInstanceOptimizer.lookForDirectItemUpgrades}, which dispatches
 * to a different strategy per mode, and it also reaches {@code AvailableItemGenerator} as the
 * flag deciding whether the precomputed upgrade trees the cheaper modes need are generated in
 * the first place. Only {@code AllCombinations} avoids building them.</p>
 */
public enum GreedyOptimizationMethod {
    /**
     * Evaluate all item, gem and enchant combinations of the slot (the base optimizer
     * behavior); the most thorough and most expensive choice.
     */
    AllCombinations,
    /**
     * Walk the precomputed direct-upgrade tree of each item and descend into the best child
     * at each level instead of enumerating combinations; the locked slot still uses all
     * combinations.
     */
    SingleChanges,
    /**
     * Pick the best gem for each socket color, the best meta gem and the best enchant by
     * evaluating each candidate once, then build the item variants from those picks; the
     * cheapest approximation.
     */
    GreedyBest
}
