// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFrost.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FrostCycleGenerator2 extends CycleGenerator {
    private static class State extends CycleState {
        public boolean brainFreezeRegistered;
        public boolean brainFreezeProcced;
        public int fingersOfFrostRegistered;
        public int fingersOfFrostActual;
        public boolean latentFingersOfFrostWindow;
        public float deepFreezeCooldown;
        public float tier10TwoPieceDuration;
    }

    public Spell[][] FrB, FB, IL, DF;

    private float BF;
    private float FOF;
    private float T8;
    private boolean T10;

    private boolean deepFreeze;
    private float deepFreezeCooldown;
    private boolean useLatencyCombos;
    private boolean tier10TwoPieceCollapsed;
    private boolean fofInstantsOnLastChargeOnly;
    private boolean ffbBrainFreeze;

    public FrostCycleGenerator2(CastingState castingState, boolean useLatencyCombos, boolean useDeepFreeze, float deepFreezeCooldown, boolean tier10TwoPieceCollapsed, boolean fofInstantsOnLastChargeOnly, boolean ffbBrainFreeze) {
        this.useLatencyCombos = useLatencyCombos;
        this.tier10TwoPieceCollapsed = tier10TwoPieceCollapsed;
        this.fofInstantsOnLastChargeOnly = fofInstantsOnLastChargeOnly;
        this.deepFreezeCooldown = deepFreezeCooldown;
        this.deepFreeze = useDeepFreeze;
        this.ffbBrainFreeze = ffbBrainFreeze;

        FrB = new Spell[2][2];
        FB = new Spell[2][2];
        IL = new Spell[2][2];
        DF = new Spell[2][2];

        for (int fof = 0; fof < 2; fof++) {
            for (int t10 = 0; t10 < 2; t10++) {
                CastingState cstate = castingState;
                String label = "";
                if (t10 == 1) {
                    cstate = cstate.getTier10TwoPieceState();
                    label = "2T10";
                }
                if (fof == 1) {
                    cstate = cstate.getFrozenState();
                    label = label.length() > 0 ? label + "+" + "FOF" : "FOF";
                }
                if (label.length() > 0) {
                    label += ":";
                }
                FrB[fof][t10] = cstate.getSpell(SpellId.Frostbolt);
                FrB[fof][t10].label = label + "Frostbolt";
                if (ffbBrainFreeze) {
                    FB[fof][t10] = cstate.getSpell(SpellId.FrostfireBoltBF);
                    FB[fof][t10].label = label + "FrostfireBolt";
                } else {
                    FB[fof][t10] = cstate.getSpell(SpellId.FireballBF);
                    FB[fof][t10].label = label + "Fireball";
                }
                IL[fof][t10] = cstate.getSpell(SpellId.IceLance);
                IL[fof][t10].label = label + "IceLance";
                DF[fof][t10] = cstate.getSpell(SpellId.DeepFreeze);
                DF[fof][t10].label = label + "DeepFreeze";
            }
        }

        BF = 0.05f * castingState.mageTalents.getBrainFreeze();
        FOF = (castingState.mageTalents.getFingersOfFrost() == 2 ? 0.15f : 0.07f * castingState.mageTalents.getFingersOfFrost());
        T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        T10 = castingState.baseStats.getMage2T10() > 0;

        generateStateDescription();
    }

    @Override
    protected CycleState getInitialState() {
        return getState(false, false, 0, 0, false, 0.0f, 0.0f);
    }

    @Override
    protected List<CycleControlledStateTransition> getStateTransitions(CycleState state) {
        State s = (State) state;
        List<CycleControlledStateTransition> list = new ArrayList<CycleControlledStateTransition>();
        Spell FrB = null;
        Spell IL = null;
        Spell FB = null;
        Spell DF = null;
        int t10 = s.tier10TwoPieceDuration > 0 ? 1 : 0;
        if (s.fingersOfFrostActual > 0) {
            FrB = this.FrB[1][t10];
        } else {
            FrB = this.FrB[0][t10];
        }
        if (s.fingersOfFrostActual > 0 || (useLatencyCombos && s.latentFingersOfFrostWindow)) {
            IL = this.IL[1][t10];
            FB = this.FB[1][t10];
        } else {
            IL = this.IL[0][t10];
            FB = this.FB[0][t10];
        }
        if (s.deepFreezeCooldown == 0.0f && (s.fingersOfFrostRegistered > 0 || (useLatencyCombos && s.latentFingersOfFrostWindow))) {
            DF = this.DF[1][t10];
        }
        if (FOF > 0 && BF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeProcced,
                    true,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    2,
                    s.fingersOfFrostActual > 0,
                    Math.max(0, s.deepFreezeCooldown - FrB.castTime),
                    Math.max(0, s.tier10TwoPieceDuration - FrB.castTime)
            );
            transition.setTransitionProbability(FOF * BF);
            list.add(transition);
        }
        if (FOF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeProcced,
                    s.brainFreezeProcced,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    2,
                    s.fingersOfFrostActual > 0,
                    Math.max(0, s.deepFreezeCooldown - FrB.castTime),
                    Math.max(0, s.tier10TwoPieceDuration - FrB.castTime)
            );
            transition.setTransitionProbability(FOF * (1 - BF));
            list.add(transition);
        }
        if (BF > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeProcced,
                    true,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 0,
                    Math.max(0, s.deepFreezeCooldown - FrB.castTime),
                    Math.max(0, s.tier10TwoPieceDuration - FrB.castTime)
            );
            transition.setTransitionProbability((1 - FOF) * BF);
            list.add(transition);
        }
        {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = FrB;
            transition.targetState = getState(
                    s.brainFreezeProcced,
                    s.brainFreezeProcced,
                    Math.max(0, s.fingersOfFrostActual - 1),
                    Math.max(0, s.fingersOfFrostActual - 1),
                    s.fingersOfFrostActual > 0,
                    Math.max(0, s.deepFreezeCooldown - FrB.castTime),
                    Math.max(0, s.tier10TwoPieceDuration - FrB.castTime)
            );
            transition.setTransitionProbability((1 - FOF) * (1 - BF));
            list.add(transition);
        }
        if (!fofInstantsOnLastChargeOnly || !(s.fingersOfFrostRegistered > 0) || (useLatencyCombos && s.latentFingersOfFrostWindow && s.fingersOfFrostRegistered == 0) || (!useLatencyCombos && s.fingersOfFrostRegistered == 1)) {
            if (s.fingersOfFrostRegistered > 0 || (useLatencyCombos && s.latentFingersOfFrostWindow)) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = IL;
                transition.targetState = getState(
                        s.brainFreezeProcced,
                        s.brainFreezeProcced,
                        Math.max(0, s.fingersOfFrostActual - 1),
                        Math.max(0, s.fingersOfFrostActual - 1),
                        s.fingersOfFrostActual > 1,
                        Math.max(0, s.deepFreezeCooldown - IL.castTime),
                        Math.max(0, s.tier10TwoPieceDuration - IL.castTime)
                );
                transition.setTransitionProbability(1);
                list.add(transition);
            }
            if (s.brainFreezeRegistered) {
                if (ffbBrainFreeze) {
                    // T8 not supported for FFB mode
                    if (FOF > 0) {
                        CycleControlledStateTransition transition = new CycleControlledStateTransition();
                        transition.spell = FB;
                        transition.targetState = getState(
                                false,
                                false,
                                Math.max(0, s.fingersOfFrostActual - 1),
                                2,
                                s.fingersOfFrostActual > 1,
                                Math.max(0, s.deepFreezeCooldown - FB.castTime),
                                Math.max(0, T10 ? 5.0f - FB.castTime : s.tier10TwoPieceDuration - FB.castTime)
                        );
                        transition.setTransitionProbability(FOF);
                        list.add(transition);
                    }
                    {
                        CycleControlledStateTransition transition = new CycleControlledStateTransition();
                        transition.spell = FB;
                        transition.targetState = getState(
                                false,
                                false,
                                Math.max(0, s.fingersOfFrostActual - 1),
                                Math.max(0, s.fingersOfFrostActual - 1),
                                s.fingersOfFrostActual > 1,
                                Math.max(0, s.deepFreezeCooldown - FB.castTime),
                                Math.max(0, T10 ? 5.0f - FB.castTime : s.tier10TwoPieceDuration - FB.castTime)
                        );
                        transition.setTransitionProbability(1 - FOF);
                        list.add(transition);
                    }
                } else {
                    if (T8 > 0) {
                        CycleControlledStateTransition transition = new CycleControlledStateTransition();
                        transition.spell = FB;
                        transition.targetState = getState(
                                true,
                                true,
                                Math.max(0, s.fingersOfFrostActual - 1),
                                Math.max(0, s.fingersOfFrostActual - 1),
                                s.fingersOfFrostActual > 1,
                                Math.max(0, s.deepFreezeCooldown - FB.castTime),
                                Math.max(0, T10 ? 5.0f - FB.castTime : s.tier10TwoPieceDuration - FB.castTime)
                        );
                        transition.setTransitionProbability(T8);
                        list.add(transition);
                    }
                    {
                        CycleControlledStateTransition transition = new CycleControlledStateTransition();
                        transition.spell = FB;
                        transition.targetState = getState(
                                false,
                                false,
                                Math.max(0, s.fingersOfFrostActual - 1),
                                Math.max(0, s.fingersOfFrostActual - 1),
                                s.fingersOfFrostActual > 1,
                                Math.max(0, s.deepFreezeCooldown - FB.castTime),
                                Math.max(0, T10 ? 5.0f - FB.castTime : s.tier10TwoPieceDuration - FB.castTime)
                        );
                        transition.setTransitionProbability(1 - T8);
                        list.add(transition);
                    }
                }
            }
            if (DF != null && deepFreeze) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = DF;
                transition.targetState = getState(
                        s.brainFreezeProcced,
                        s.brainFreezeProcced,
                        Math.max(0, s.fingersOfFrostActual - 1),
                        Math.max(0, s.fingersOfFrostActual - 1),
                        s.fingersOfFrostActual > 1,
                        Math.max(0, deepFreezeCooldown - DF.castTime),
                        Math.max(0, s.tier10TwoPieceDuration - DF.castTime)
                );
                transition.setTransitionProbability(1);
                list.add(transition);
            }
        }

        return list;
    }

    private Map<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(boolean brainFreezeRegistered, boolean brainFreezeProcced, int fingersOfFrostRegistered, int fingersOfFrostActual, boolean latentFingersOfFrostWindow, float deepFreezeCooldown, float tier10TwoPieceDuration) {
        if (!useLatencyCombos) {
            latentFingersOfFrostWindow = false;
        }
        String name = String.format(Locale.ROOT, "BF%s%s,FOF%s%s(%s),DF%s,2T10=%s", brainFreezeProcced ? "+" : "-", brainFreezeRegistered ? "+" : "-", fingersOfFrostRegistered, latentFingersOfFrostWindow ? "+" : "-", fingersOfFrostActual, deepFreezeCooldown, tier10TwoPieceDuration);
        State state;
        if (!stateDictionary.containsKey(name)) {
            state = new State();
            state.name = name;
            state.brainFreezeProcced = brainFreezeProcced;
            state.brainFreezeRegistered = brainFreezeRegistered;
            state.fingersOfFrostActual = fingersOfFrostActual;
            state.fingersOfFrostRegistered = fingersOfFrostRegistered;
            state.latentFingersOfFrostWindow = latentFingersOfFrostWindow;
            state.deepFreezeCooldown = deepFreezeCooldown;
            state.tier10TwoPieceDuration = tier10TwoPieceDuration;
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
                (a.deepFreezeCooldown == 0 && (a.fingersOfFrostRegistered > 0 || a.latentFingersOfFrostWindow)) != (b.deepFreezeCooldown == 0 && (b.fingersOfFrostRegistered > 0 || b.latentFingersOfFrostWindow)) ||
                (!tier10TwoPieceCollapsed && (a.tier10TwoPieceDuration != b.tier10TwoPieceDuration)) ||
                a.brainFreezeRegistered != b.brainFreezeRegistered);
    }

    @Override
    public String getStateDescription() {
        return "Cycle Code Legend: 0 = FrB, 1 = IL, 2 = FB, 3 = DF\n" +
                "State Descriptions: BF+-+-,FOFy+-(z),DFw,2T10=z\n" +
                "+ = Brain Freeze procced\n" +
                "- = Brain Freeze not procced\n" +
                "+ = Brain Freeze proc visible\n" +
                "- = Brain Freeze proc not visible\n" +
                "y = visible count on Fingers of Frost\n" +
                "+ = ghost Fingers of Frost charge for instant available\n" +
                "- = ghost Fingers of Frost charge for instant not available\n" +
                "z = actual count on Fingers of Frost\n" +
                "w = Deep Freeze cooldown\n" +
                "z = remaining duration on 2T10";
    }
}
