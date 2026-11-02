// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/FSRCalc.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;

public class FSRCalc {
    public List<Float> manaSpentTimestamp;
    public List<Float> channelDuration;
    public float duration;

    public FSRCalc() {
        manaSpentTimestamp = new ArrayList<Float>();
        channelDuration = new ArrayList<Float>();
    }

    public FSRCalc(int capacity) {
        manaSpentTimestamp = new ArrayList<Float>(capacity);
        channelDuration = new ArrayList<Float>(capacity);
    }

    public void addSpell(float duration, float lag, boolean channel) {
        if (channel) {
            manaSpentTimestamp.add(duration + lag);
            channelDuration.add(duration);
        } else {
            manaSpentTimestamp.add(duration + duration + lag);
            channelDuration.add(0f);
        }
        duration += duration + lag;
    }

    public void addPause(float duration) {
        duration += duration;
    }

    public float calculateOO5SR(float clearcastingChance) {
        int N = manaSpentTimestamp.size();
        float c;
        float ret = 0;
        for (int a = 0; a < N; a++) {
            c = 1 - clearcastingChance;
            float t;
            float endstamp = manaSpentTimestamp.get(a);
            int k = 0;
            int bb = a - 1;
            if (bb < 0) {
                bb += N;
                k += 1;
            }
            float bbstamp = manaSpentTimestamp.get(bb);
            float t0 = endstamp - bbstamp + k * duration - channelDuration.get(bb);
            ret += channelDuration.get(bb);
            do {
                t = endstamp - bbstamp + k * duration - 5;
                if (t < 0)
                    ret += t0 * c;
                else if (t < t0)
                    ret += (t0 - t) * c;
                c *= clearcastingChance;
                bb -= 1;
                if (bb < 0) {
                    bb += N;
                    k += 1;
                }
                bbstamp = manaSpentTimestamp.get(bb);
            } while (t < t0);
        }
        ret = (duration - ret) / duration;

        return ret;
    }

    public static float calculateSimpleOO5SR(float clearcastingChance, float duration, float lag, boolean channel) {
        float timestamp;
        float channelDuration = 0;
        if (channel) {
            timestamp = lag;
            channelDuration = duration;
        } else {
            timestamp = duration + lag;
        }
        duration += lag;
        float c;
        float ret = 0;

        c = 1 - clearcastingChance;
        float t;
        int k = 1;
        float t0 = k * duration - channelDuration;
        ret += channelDuration;
        do {
            t = k * duration - 5;
            if (t < 0)
                ret += t0 * c;
            else if (t < t0)
                ret += (t0 - t) * c;
            c *= clearcastingChance;
            k += 1;
        } while (t < t0);

        ret = (duration - ret) / duration;

        return ret;
    }
}
