// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/StateSpace.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.algorithms;

/**
 * One directed edge of a {@link State} graph: from the owning state, with probability
 * {@link #transitionProbability}, the process moves to {@link #targetState}, spending
 * {@link #transitionDuration} seconds and performing {@link #ability}.
 *
 * <p>These are exactly the inputs {@link MarkovProcess} consumes: the probability drives the linear
 * system and the per-ability weights, the duration drives the average transition duration.
 * {@code ability} is a generic payload supplied by the model (typically an enum or an ability
 * object) and may be {@code null} for a transition that only advances time; {@code MarkovProcess}
 * skips null abilities when accumulating ability weights but still counts their duration.</p>
 *
 * <p>In C#, {@code TransitionProbability} is a virtual auto-property that subclasses override (for
 * example with a lazily computed chance that depends on the current state). Java has no virtual
 * fields, so subclasses override {@link #getTransitionProbability()} instead, and
 * {@link #transitionProbability} is the plain stored value for the common case. Consumers must call
 * the getter, never read the field directly, to observe overridden probabilities.</p>
 */
public class StateTransition<TAbility> {
    public State<TAbility> targetState;
    public TAbility ability;
    public double transitionDuration;

    // C# virtual auto-property TransitionProbability; subclasses override it.
    public double transitionProbability;

    public double getTransitionProbability() {
        return transitionProbability;
    }

    public double getTransitionDuration() {
        return transitionDuration;
    }
}
