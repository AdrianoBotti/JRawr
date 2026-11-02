// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/SimulatedStat.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.warlock;

import rawr.base.util.Utilities;

import java.util.ArrayList;
import java.util.List;

public class SimulatedStat {

    public List<Float> values = new ArrayList<Float>();
    public List<Float> weights = new ArrayList<Float>();
    private float result;
    private boolean solved;

    public void addSample(float value, float weight) {
        values.add(value);
        weights.add(weight);
    }

    public float getValue() {
        if (!solved) {
            result = Utilities.getWeightedSum(values, weights);
            solved = true;
        }
        return result;
    }

    public float getTotalWeight() {
        float sum = 0f;
        for (float w : weights) {
            sum += w;
        }
        return sum;
    }
}
