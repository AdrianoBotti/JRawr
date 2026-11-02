// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/StateSpace.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.algorithms;

import java.util.List;

/**
 * One vertex of the state graph used by {@link StateSpaceGenerator} and {@link MarkovProcess}.
 *
 * <p>A state stands for a situation a rotation can be in (cooldowns, procs, resources, phase); the
 * concrete subclass holds the data that distinguishes it, while {@link #transitions} lists the edges
 * that can be taken from it. Each transition's {@link StateTransition#targetState} points at another
 * {@code State} of the same graph. States are identified by object identity, not by
 * {@code equals}: the graph walk and any deduplication rely on that.</p>
 *
 * <p>{@link #index} is the state's position in the list returned by
 * {@link StateSpaceGenerator#generateStateSpace()}; {@link MarkovProcess} assigns it and uses it to
 * index its matrix. Subclasses may use the index for their own arrays afterwards, but must not
 * assign it before that point. {@link #name} is a human-readable label used by {@link #toString()}
 * for diagnostics only. The fields are public and mutable because the generator fills
 * {@code transitions} after the state object has been created.</p>
 */
public class State<TAbility> {
    public List<StateTransition<TAbility>> transitions;
    public int index;
    public String name;

    public List<StateTransition<TAbility>> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<StateTransition<TAbility>> value) {
        transitions = value;
    }

    @Override
    public String toString() {
        return name;
    }
}
