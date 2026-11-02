// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FrostCycleGenerator extends CycleGenerator {
    private static class State extends CycleState {
        public boolean brainFreezeRegistered;
        public float brainFreezeDuration;
        public int fingersOfFrostRegistered;
        public int fingersOfFrostActual;
        public boolean latentFingersOfFrostWindow;
        public boolean deepFreezeCooldown;
    }

    public Spell FrB, FrBS, FB, FBS, IL, ILS, DFS;

    private float BF;
    private float FOF;
    private float T8;

    private boolean deepFreeze;
    private boolean useLatencyCombos;

    public FrostCycleGenerator(CastingState castingState, boolean useLatencyCombos, boolean useDeepFreeze) {
        this.useLatencyCombos = useLatencyCombos;

        FrB = castingState.getSpell(SpellId.Frostbolt);
        FrBS = castingState.getFrozenState().getSpell(SpellId.Frostbolt);
        FB = castingState.getSpell(SpellId.FireballBF);
        FBS = castingState.getFrozenState().getSpell(SpellId.FireballBF);
        IL = castingState.getSpell(SpellId.IceLance);
        ILS = castingState.getFrozenState().getSpell(SpellId.IceLance);
        DFS = castingState.getFrozenState().getSpell(SpellId.DeepFreeze);

        BF = 0.05f * castingState.mageTalents.getBrainFreeze();
        FOF = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());
        T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        deepFreeze = useDeepFreeze;

        generateStateDescription();
    }

    @Override
    protected CycleState getInitialState() {
        return getState(false, 0.0f, 0, 0, false, false);
    }

    @Override
    protected List<CycleControlledStateTransition> getStateTransitions(CycleState state) {
        State s = (State) state;
        List<CycleControlledStateTransition> list = new ArrayList<CycleControlledStateTransition>();
        Spell FrB = null;
        Spell IL = null;
        Spell FB = null;
        Spell DF = null;
        if (s.fingersOfFrostActual > 0) {
            FrB = this.FrBS;
        } else {
            FrB = this.FrB;
        }
        if (s.fingersOfFrostActual > 0 || (useLatencyCombos && s.latentFingersOfFrostWindow)) {
            IL = this.ILS;
            FB = this.FBS;
        } else {
            IL = this.IL;
            FB = this.FB;
        }
        if (!s.deepFreezeCooldown && (s.fingersOfFrostRegistered > 0 || (useLatencyCombos && s.latentFingersOfFrostWindow))) {
            DF = this.DFS;
        }
        if (FOF > 0 && BF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeDuration > FrB.castTime,
                    15.0f,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    2,
                    s.fingersOfFrostActual > 0,
                    s.deepFreezeCooldown && s.fingersOfFrostActual > 0
            );
            transition.setTransitionProbability(FOF * BF);
            list.add(transition);
        }
        if (FOF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeDuration > FrB.castTime,
                    Math.max(0.0f, s.brainFreezeDuration - FrB.castTime),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    2,
                    s.fingersOfFrostActual > 0,
                    s.deepFreezeCooldown && s.fingersOfFrostActual > 0
            );
            transition.setTransitionProbability(FOF * (1 - BF));
            list.add(transition);
        }
        if (BF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeDuration > FrB.castTime,
                    15.0f,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 0,
                    s.deepFreezeCooldown && s.fingersOfFrostActual > 0
            );
            transition.setTransitionProbability((1 - FOF) * BF);
            list.add(transition);
        }
        {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeDuration > FrB.castTime,
                    Math.max(0.0f, s.brainFreezeDuration - FrB.castTime),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 0,
                    s.deepFreezeCooldown && s.fingersOfFrostActual > 0
            );
            transition.setTransitionProbability((1 - FOF) * (1 - BF));
            list.add(transition);
        }
        {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = IL;
            transition.targetState = getState(
                    s.brainFreezeDuration > IL.castTime,
                    Math.max(0.0f, s.brainFreezeDuration - IL.castTime),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 1,
                    s.deepFreezeCooldown && s.fingersOfFrostActual > 1
            );
            transition.setTransitionProbability(1);
            list.add(transition);
        }
        if (s.brainFreezeRegistered) {
            if (T8 > 0) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = FB;
                transition.targetState = getState(
                        s.brainFreezeDuration > FB.castTime,
                        Math.max(0.0f, s.brainFreezeDuration - FB.castTime),
                        Math.max(0, s.fingersOfFrostActual - 1),
                        Math.max(0, s.fingersOfFrostActual - 1),
                        s.fingersOfFrostActual > 1,
                        s.deepFreezeCooldown && s.fingersOfFrostActual > 1
                );
                transition.setTransitionProbability(T8);
                list.add(transition);
            }
            {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = FB;
                transition.targetState = getState(
                        false,
                        0.0f,
                        Math.max(0, s.fingersOfFrostActual - 1),
                        Math.max(0, s.fingersOfFrostActual - 1),
                        s.fingersOfFrostActual > 1,
                        s.deepFreezeCooldown && s.fingersOfFrostActual > 1
                );
                transition.setTransitionProbability(1 - T8);
                list.add(transition);
            }
        }
        if (DF != null && deepFreeze) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = DF;
            transition.targetState = getState(
                    s.brainFreezeDuration > DF.castTime,
                    Math.max(0.0f, s.brainFreezeDuration - DF.castTime),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 1,
                    s.fingersOfFrostActual > 1
            );
            transition.setTransitionProbability(1);
            list.add(transition);
        }

        return list;
    }

    private Map<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(boolean brainFreezeRegistered, float brainFreezeDuration, int fingersOfFrostRegistered, int fingersOfFrostActual, boolean latentFingersOfFrostWindow, boolean deepFreezeCooldown) {
        if (!useLatencyCombos) {
            latentFingersOfFrostWindow = false;
        }
        String name = String.format(Locale.ROOT, "BF%s%s,FOF%s%s(%s),DF%s", brainFreezeDuration, brainFreezeRegistered ? "+" : "-", fingersOfFrostRegistered, latentFingersOfFrostWindow ? "+" : "-", fingersOfFrostActual, deepFreezeCooldown ? "-" : "+");
        State state;
        if (!stateDictionary.containsKey(name)) {
            state = new State();
            state.name = name;
            state.brainFreezeDuration = brainFreezeDuration;
            state.brainFreezeRegistered = brainFreezeRegistered;
            state.fingersOfFrostActual = fingersOfFrostActual;
            state.fingersOfFrostRegistered = fingersOfFrostRegistered;
            state.latentFingersOfFrostWindow = latentFingersOfFrostWindow;
            state.deepFreezeCooldown = deepFreezeCooldown;
            stateDictionary.put(name, state);
        } else {
            state = stateDictionary.get(name);
        }
        return state;
    }

    @Override
    protected boolean canStatesBeDistinguished(CycleState state1, CycleState state2) {
        State a = (State) state1;
        State b = (State) state2;
        return (
                a.fingersOfFrostRegistered != b.fingersOfFrostRegistered ||
                a.latentFingersOfFrostWindow != b.latentFingersOfFrostWindow ||
                a.deepFreezeCooldown != b.deepFreezeCooldown ||
                a.brainFreezeRegistered != b.brainFreezeRegistered);
    }

    @Override
    public String getStateDescription() {
        return "Cycle Code Legend: 0 = FrB, 1 = IL, 2 = FB, 3 = DF\n" +
                "State Descriptions: BFx+-,FOFy+-(z),DF+-\n" +
                "x = remaining time on Brain Freeze\n" +
                "+ = Brain Freeze proc visible\n" +
                "- = Brain Freeze proc not visible\n" +
                "y = visible count on Fingers of Frost\n" +
                "+ = ghost Fingers of Frost charge for instant available\n" +
                "- = ghost Fingers of Frost charge for instant not available\n" +
                "z = actual count on Fingers of Frost\n" +
                "+ = Deep Freeze not on cooldown (within single FoF only)\n" +
                "- = Deep Freeze on cooldown (within single FoF only)";
    }
}
