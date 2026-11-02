// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the unsafe pointer path is replaced by LU's safe array API (same arithmetic, DECISIONS #4).
package rawr.models.mage;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GenericCycle extends Cycle {
    public List<CycleState> stateList;
    public double[] stateWeight;
    Map<Spell, Double> SpellWeight = new LinkedHashMap<Spell, Double>();
    Map<Cycle, Double> CycleWeight = new LinkedHashMap<Cycle, Double>();
    public String spellDistribution;

    public GenericCycle(String name, CastingState castingState, List<CycleState> stateDescription, boolean needsDisplayCalculations) {
        super(needsDisplayCalculations, castingState);
        this.name = name;

        stateList = stateDescription;
        for (int i = 0; i < stateList.size(); i++) {
            stateList.get(i).index = i;
        }

        int size = stateList.size() + 1;

        ArraySet arraySet = ArrayPool.requestArraySet(false);
        try {
            LU M = new LU(size, arraySet);

            stateWeight = new double[size];

            M.beginSafe();

            Arrays.fill(arraySet.LU_U, 0, size * size, 0.0);

            for (CycleState state : stateList) {
                for (CycleStateTransition transition : state.transitions) {
                    arraySet.LU_U[transition.targetState.index * size + state.index] += transition.getTransitionProbability();
                }
                arraySet.LU_U[state.index * size + state.index] -= 1.0;
            }

            for (int i = 0; i < size - 1; i++) {
                arraySet.LU_U[(size - 1) * size + i] = 1;
            }

            stateWeight[size - 1] = 1;

            M.decompose();
            M.fSolve(stateWeight);

            M.endUnsafe();

            SpellWeight = new LinkedHashMap<Spell, Double>();
            CycleWeight = new LinkedHashMap<Cycle, Double>();

            for (CycleState state : stateList) {
                double stateWeight = this.stateWeight[state.index];
                if (stateWeight > 0) {
                    for (CycleStateTransition transition : state.transitions) {
                        float transitionProbability = transition.getTransitionProbability();
                        if (transitionProbability > 0) {
                            if (transition.spell != null) {
                                double weight = SpellWeight.containsKey(transition.spell) ? SpellWeight.get(transition.spell) : 0d;
                                SpellWeight.put(transition.spell, weight + stateWeight * transitionProbability);
                            }
                            if (transition.cycle != null) {
                                double weight = CycleWeight.containsKey(transition.cycle) ? CycleWeight.get(transition.cycle) : 0d;
                                CycleWeight.put(transition.cycle, weight + stateWeight * transitionProbability);
                            }
                            if (transition.pause > 0) {
                                addPause(transition.pause, (float) (stateWeight * transitionProbability));
                            }
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Spell, Double> kvp : SpellWeight.entrySet()) {
                addSpell(needsDisplayCalculations, kvp.getKey(), (float) (double) kvp.getValue());
                if (kvp.getValue() > 0)
                    sb.append(String.format(Locale.ROOT, "%s:\t%.2f%%\r\n", kvp.getKey().label != null ? kvp.getKey().label : kvp.getKey().spellId.toString(), 100.0 * kvp.getValue()));
            }
            for (Map.Entry<Cycle, Double> kvp : CycleWeight.entrySet()) {
                addCycle(needsDisplayCalculations, kvp.getKey(), (float) (double) kvp.getValue());
                if (kvp.getValue() > 0)
                    sb.append(String.format(Locale.ROOT, "%s:\t%.2f%%\r\n", kvp.getKey().cycleId, 100.0 * kvp.getValue()));
            }

            calculate();

            spellDistribution = sb.toString();
        } finally {
            ArrayPool.releaseArraySet(arraySet);
        }
    }
}
