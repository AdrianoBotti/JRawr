// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/MarkovProcess.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop constructor is `unsafe` and pins the ArraySet arrays with `fixed`; the Java port
// uses the same arrays directly (DECISIONS #4/#59). AbilityWeight uses LinkedHashMap so the
// enumeration order matches the C# Dictionary insertion order (no removals).
package rawr.base.algorithms;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the long-run average behavior of a Markov process built from a state graph, as produced by
 * a {@link StateSpaceGenerator}.
 *
 * <p>A Markov process here is a finite set of <b>states</b> (for example a fury warrior's cooldown
 * and proc conditions) and, for each state, a list of <b>transitions</b>: each transition has a
 * probability, a duration in seconds and a payload naming the ability it represents. Once the graph
 * is known, the model does not have to simulate casts one by one; this class computes how often each
 * state and each ability occurs on average, and how long an average transition lasts.</p>
 *
 * <h2>Stationary distribution</h2>
 *
 * <p>The constructor builds one linear system over the graph and solves it with {@link LU}. Writing
 * {@code p} for a transition probability, the system says that the long-run weight flowing into a
 * state equals the weight leaving it, for every state; one extra equation forces all state weights
 * to sum to 1. Concretely, for each transition from state {@code s} to state {@code t}, {@code p} is
 * added at matrix row {@code t}, column {@code s}; the diagonal receives {@code -1}; the last row is
 * all ones and the last right-hand-side entry is 1. The unknowns of that extra row are auxiliary and
 * not read back. State indexes are the positions in the input list, and the constructor writes that
 * position into {@link State#index}.</p>
 *
 * <p>The solved vector is {@link #stateWeight}: the stationary probability of each state, i.e. the
 * fraction of transitions that start in it. Its first {@code stateSpace.size()} entries correspond to
 * the states; the last entry belongs to the auxiliary normalization unknown.</p>
 *
 * <h2>Ability weights and average duration</h2>
 *
 * <p>For every state with positive weight, each outgoing transition contributes
 * {@code stateWeight[s] * p} to its ability's weight in {@link #abilityWeight} (null abilities are
 * skipped; the map is a {@code LinkedHashMap} so the C# insertion order is preserved). The same
 * contribution times the transition's duration is accumulated into
 * {@link #averageTransitionDuration}, making it the duration of one average transition. Ability
 * weights are therefore probabilities per transition, and a model can compute, for example, damage
 * per second as {@code sum(ability damage * weight) / averageTransitionDuration}.</p>
 *
 * <h2>Resources and preconditions</h2>
 *
 * <p>The solve borrows one {@link LU.ArraySet} from {@link ArrayPool} and releases it when the
 * constructor finishes; the pool therefore bounds how many Markov processes can be built
 * concurrently. Input states must form a closed graph (every transition target present in the list),
 * otherwise matrix indexes run out of bounds; the generator normally guarantees this by construction.
 * The constructor mutates the passed states' {@code index} fields and caches nothing; the resulting
 * object is a plain data holder.</p>
 *
 * @see StateSpaceGenerator
 * @see State
 * @see StateTransition
 */
public class MarkovProcess<TAbility> {
    public List<State<TAbility>> stateSpace;
    public double[] stateWeight;
    public Map<TAbility, Double> abilityWeight;
    public double averageTransitionDuration;

    public MarkovProcess(List<State<TAbility>> stateSpace) {
        this.stateSpace = stateSpace;
        for (int i = 0; i < stateSpace.size(); i++) {
            stateSpace.get(i).index = i;
        }

        int size = stateSpace.size() + 1;

        LU.ArraySet arraySet = ArrayPool.requestArraySet();
        LU m = new LU(size, arraySet);

        stateWeight = new double[size];

        m.beginUnsafe(arraySet.luU, arraySet.luSparseL, arraySet.luP, arraySet.luQ, arraySet.luLj,
                arraySet.luSparseLI, arraySet.luSparseLstart, arraySet.luColumn, arraySet.luColumn2);

        Arrays.fill(arraySet.luU, 0, size * size, 0.0);

        // U[i * rows + j]

        // The C# pins arraySet.LU_U directly here (the LU fields alias the same array).
        for (State<TAbility> state : stateSpace) {
            for (StateTransition<TAbility> transition : state.transitions) {
                arraySet.luU[transition.targetState.index * size + state.index] += transition.getTransitionProbability();
            }
            arraySet.luU[state.index * size + state.index] -= 1.0;
        }

        for (int i = 0; i < size - 1; i++) {
            arraySet.luU[(size - 1) * size + i] = 1;
        }

        stateWeight[size - 1] = 1;

        m.decompose();
        m.fSolve(stateWeight);

        m.endUnsafe();

        abilityWeight = new LinkedHashMap<TAbility, Double>();

        for (State<TAbility> state : stateSpace) {
            double stateWeightValue = stateWeight[state.index];
            if (stateWeightValue > 0) {
                for (StateTransition<TAbility> transition : state.transitions) {
                    double transitionProbability = transition.getTransitionProbability();
                    if (transitionProbability > 0) {
                        double p = stateWeightValue * transitionProbability;
                        if (transition.ability != null) {
                            double weight = abilityWeight.containsKey(transition.ability) ? abilityWeight.get(transition.ability) : 0.0;
                            abilityWeight.put(transition.ability, weight + p);
                        }
                        averageTransitionDuration += p * transition.getTransitionDuration();
                    }
                }
            }
        }

        ArrayPool.releaseArraySet(arraySet);
    }
}
