// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

/**
 * Selects the top-level search algorithm the optimizer uses to explore gear combinations.
 *
 * <p>Both algorithms are heuristics: for a realistic item pool an exhaustive search is
 * infeasible, so they explore a subset of the possibilities and are not guaranteed to find
 * the true optimum. The choice affects only how the search proceeds; scoring a gear set
 * itself is unaffected. The search is driven by the optimizer base class, which returns the
 * best candidate it found.</p>
 */
public enum OptimizationMethod {
    /**
     * Population-based search: a set of candidate gear sets evolves over generations through
     * crossover, mutation and elite retention, plus optional direct-swap improvements. This
     * is the default and the method the determinism tests use.
     */
    GeneticAlgorithm,
    /**
     * Simulated annealing: one candidate performs a random walk and worse moves are accepted
     * with a probability that decreases over time, which lets the search escape local optima.
     * Ported from the contributor implementation in the original.
     */
    SimulatedAnnealing
}
