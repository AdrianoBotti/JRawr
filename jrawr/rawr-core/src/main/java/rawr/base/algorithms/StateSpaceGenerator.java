// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/StateSpace.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.algorithms;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the reachable {@link State} graph for a model by walking the transitions out of a start
 * state.
 *
 * <p>A model implements the two hooks: {@link #getInitialState()} supplies the state to start from,
 * and {@link #getStateTransitions(State)} returns the outgoing transitions of any given state. The
 * base class explores the graph and returns every reachable state exactly once, with each returned
 * state's {@link State#transitions} already filled in. The result feeds {@link MarkovProcess}.</p>
 *
 * <h2>How the walk works</h2>
 *
 * <p>The walk is an explicit-stack depth-first search. The pending list is used as a LIFO (states
 * are removed from its end), so the returned list is ordered by when states finished expanding, not
 * by discovery. A state is marked "seen" as soon as it is pushed. The bookkeeping map is an
 * {@code IdentityHashMap}: two {@code State} objects that compare equal but are distinct objects are
 * treated as different graph vertices, matching the reference-equality comparer of the C# original.
 * A transition pointing back at its own state is kept in the transition list but never enqueued
 * again, so cyclic graphs terminate.</p>
 *
 * <p>The returned order is deterministic for a deterministic hook implementation and is part of the
 * observable behavior: {@link MarkovProcess} numbers states by their position in this list and models
 * index their own arrays the same way. Hooks must therefore return transitions in a stable order and
 * must hand out one {@code State} object per distinct vertex.</p>
 */
public abstract class StateSpaceGenerator<TAbility> {
    /**
     * Walks the graph and returns the reachable states in expansion order, with every state's
     * {@code transitions} field filled in. The hooks are called exactly once per reachable state.
     */
    public List<State<TAbility>> generateStateSpace() {
        List<State<TAbility>> remainingStates = new ArrayList<State<TAbility>>();
        List<State<TAbility>> processedStates = new ArrayList<State<TAbility>>();
        // The C# dictionary uses an explicit reference-equality comparer; IdentityHashMap matches it.
        Map<State<TAbility>, Boolean> seenStates = new IdentityHashMap<State<TAbility>, Boolean>();
        State<TAbility> initState = getInitialState();
        remainingStates.add(initState);
        seenStates.put(initState, Boolean.TRUE);

        while (!remainingStates.isEmpty()) {
            State<TAbility> state = remainingStates.remove(remainingStates.size() - 1);

            List<StateTransition<TAbility>> transitions = getStateTransitions(state);
            state.transitions = transitions;
            for (StateTransition<TAbility> transition : transitions) {
                if (transition.targetState != state && !seenStates.containsKey(transition.targetState)) {
                    remainingStates.add(transition.targetState);
                    seenStates.put(transition.targetState, Boolean.TRUE);
                }
            }

            processedStates.add(state);
        }

        return processedStates;
    }

    /**
     * Creates the state the walk starts from. Called once per {@link #generateStateSpace()} call.
     */
    protected abstract State<TAbility> getInitialState();

    /**
     * Returns the outgoing transitions of {@code state} in a deterministic order. The list is stored
     * on the state and must not be {@code null}; target states must be distinct objects per vertex,
     * because the walk deduplicates on object identity.
     */
    protected abstract List<StateTransition<TAbility>> getStateTransitions(State<TAbility> state);
}
