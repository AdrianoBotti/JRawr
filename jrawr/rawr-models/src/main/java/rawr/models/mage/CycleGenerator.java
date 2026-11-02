// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the BackgroundWorker progress overload of Analyze is UI-only and omitted; the goto RESTART
// validation loop is expressed with a labeled-style inner loop.
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CycleGenerator {
    public List<CycleState> stateList;
    public int[] controlOptions;
    public int[] controlValue;
    public int[] controlIndex;
    public Map<String, Integer>[] spellMap;

    public String getStateDescription() {
        return "";
    }

    public void generateStateDescription() {
        List<CycleState> remainingStates = new ArrayList<CycleState>();
        List<CycleState> processedStates = new ArrayList<CycleState>();
        remainingStates.add(getInitialState());

        while (remainingStates.size() > 0) {
            CycleState state = remainingStates.get(remainingStates.size() - 1);
            remainingStates.remove(remainingStates.size() - 1);

            List<CycleControlledStateTransition> transitions = getStateTransitions(state);
            List<CycleStateTransition> stateTransitions = new ArrayList<CycleStateTransition>();
            for (CycleControlledStateTransition transition : transitions) {
                stateTransitions.add(transition);
            }
            state.transitions = stateTransitions;
            for (CycleControlledStateTransition transition : transitions) {
                if (transition.targetState != state && !processedStates.contains(transition.targetState) && !remainingStates.contains(transition.targetState)) {
                    remainingStates.add(transition.targetState);
                }
            }

            processedStates.add(state);
        }

        stateList = processedStates;
        for (int i = 0; i < stateList.size(); i++) {
            stateList.get(i).index = i;
        }

        controlIndex = new int[stateList.size()];
        List<CycleState> controlledStates = new ArrayList<CycleState>();
        for (CycleState state : stateList) {
            int controlIndex = -1;
            for (CycleState controlledState : controlledStates) {
                if (!canStatesBeDistinguished(state, controlledState)) {
                    controlIndex = this.controlIndex[controlledState.index];
                    break;
                }
            }
            if (controlIndex == -1) {
                controlIndex = controlledStates.size();
                controlledStates.add(state);
            }
            this.controlIndex[state.index] = controlIndex;
        }

        controlOptions = new int[controlledStates.size()];
        controlValue = new int[controlledStates.size()];

        @SuppressWarnings("unchecked")
        Map<String, Integer>[] spellMap = new Map[controlledStates.size()];
        this.spellMap = spellMap;

        for (CycleState state : stateList) {
            int controlIndex = this.controlIndex[state.index];
            if (spellMap[controlIndex] == null) {
                spellMap[controlIndex] = new LinkedHashMap<String, Integer>();
            }
            for (CycleStateTransition transitionBase : state.transitions) {
                CycleControlledStateTransition transition = (CycleControlledStateTransition) transitionBase;
                String n;
                if (transition.spell != null) {
                    n = transition.spell.getName();
                } else {
                    n = "Pause";
                }
                Integer controlValue = spellMap[controlIndex].get(n);
                if (controlValue == null) {
                    controlValue = spellMap[controlIndex].keySet().size();
                    spellMap[controlIndex].put(n, controlValue);
                }
                transition.setControls(controlIndex, this.controlValue, controlValue);
            }
        }

        for (int i = 0; i < controlOptions.length; i++) {
            controlOptions[i] = spellMap[i].keySet().size();
        }
    }

    public GenericCycle generateCycle(String name, CastingState castingState) {
        return new GenericCycle(name, castingState, stateList, false);
    }

    public List<Cycle> analyze(CastingState castingState, Cycle wand) {
        Map<String, Cycle> cycleDict = new LinkedHashMap<String, Cycle>();
        int j;
        for (int i = 0; i < controlValue.length; i++) {
            controlValue[i] = 0;
        }
        int total = 0;
        do {
            total++;
            j = controlValue.length - 1;
            controlValue[j]++;
            while (controlValue[j] >= controlOptions[j]) {
                controlValue[j] = 0;
                j--;
                if (j < 0) {
                    break;
                }
                controlValue[j]++;
            }
        } while (j >= 0);
        for (int i = 0; i < controlValue.length; i++) {
            controlValue[i] = 0;
        }
        do {
            String name = "";
            for (int i = 0; i < controlValue.length; i++) {
                name += controlValue[i];
            }
            GenericCycle generic = new GenericCycle(name, castingState, stateList, false);
            if (!cycleDict.containsKey(generic.spellDistribution)) {
                cycleDict.put(generic.spellDistribution, generic);
            }
            j = controlValue.length - 1;
            controlValue[j]++;
            while (controlValue[j] >= controlOptions[j]) {
                controlValue[j] = 0;
                j--;
                if (j < 0) {
                    break;
                }
                controlValue[j]++;
            }
        } while (j >= 0);

        if (wand != null) {
            cycleDict.put("Wand", wand);
        }

        List<Cycle> cyclePalette = new ArrayList<Cycle>();

        double maxdps = 0;
        Cycle maxdpsCycle = null;
        for (Cycle cycle : cycleDict.values()) {
            if (cycle.getDamagePerSecond() > maxdps) {
                maxdpsCycle = cycle;
                maxdps = cycle.getDamagePerSecond();
            }
        }

        cyclePalette.add(maxdpsCycle);

        Cycle mindpmCycle;
        do {
            Cycle highdpsCycle = cyclePalette.get(cyclePalette.size() - 1);
            while (true) {
                mindpmCycle = null;
                double mindpm = Double.POSITIVE_INFINITY;
                for (Cycle cycle : cycleDict.values()) {
                    double dpm = (cycle.getDamagePerSecond() - highdpsCycle.getDamagePerSecond()) / (cycle.getManaPerSecond() - highdpsCycle.getManaPerSecond());
                    if (dpm > 0 && dpm < mindpm && cycle.getManaPerSecond() < highdpsCycle.getManaPerSecond()) {
                        mindpm = dpm;
                        mindpmCycle = cycle;
                    }
                }
                boolean restart = false;
                if (mindpmCycle != null) {
                    for (Cycle cycle : cycleDict.values()) {
                        double dpm = (cycle.getDamagePerSecond() - mindpmCycle.getDamagePerSecond()) / (cycle.getManaPerSecond() - mindpmCycle.getManaPerSecond());
                        if (cycle != highdpsCycle && cycle.getDamagePerSecond() > mindpmCycle.getDamagePerSecond() && dpm > mindpm + 0.000001) {
                            highdpsCycle = cycle;
                            restart = true;
                            break;
                        }
                    }
                }
                if (!restart) break;
            }
            if (mindpmCycle != null) {
                cyclePalette.add(mindpmCycle);
            }
        } while (mindpmCycle != null);
        return cyclePalette;
    }

    protected abstract CycleState getInitialState();

    protected abstract List<CycleControlledStateTransition> getStateTransitions(CycleState state);

    protected abstract boolean canStatesBeDistinguished(CycleState state1, CycleState state2);
}
