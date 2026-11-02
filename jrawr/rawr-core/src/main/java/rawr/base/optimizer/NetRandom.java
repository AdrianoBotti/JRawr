// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Split out of OptimizerBase.java when rawr-base was reorganized into sub-packages (the batch
// tools need this type outside the optimizer package).
package rawr.base.optimizer;

/**
 * Faithful port of .NET Framework System.Random (subtractive generator). Same implementation as
 * LP.NetRandom in rawr-models; duplicated here so rawr-core does not depend on rawr-models.
 *
 * <p>This is the random number generator behind the optimizer's genetic algorithm. It reproduces
 * .NET's {@code System.Random} bit for bit so that a fixed seed yields the same sequence as the C#
 * oracle; tests pin the seed via {@code OptimizerBase.setRndSeed(int)}. The seedless constructor
 * uses the clock and is therefore non-deterministic by design, like the original.</p>
 */
public final class NetRandom {
    private static final int MBIG = Integer.MAX_VALUE;
    private static final int MSEED = 161803398;
    private final int[] seedArray = new int[56];
    private int inext;
    private int inextp;

    public NetRandom() {
        this((int) (System.nanoTime() & 0xFFFFFFFFL));
    }

    public NetRandom(int seed) {
        int ii;
        int mj, mk;
        int subtraction = (seed == Integer.MIN_VALUE) ? Integer.MAX_VALUE : Math.abs(seed);
        mj = MSEED - subtraction;
        seedArray[55] = mj;
        mk = 1;
        for (int i = 1; i < 55; i++) {
            ii = (21 * i) % 55;
            seedArray[ii] = mk;
            mk = mj - mk;
            if (mk < 0) mk += MBIG;
            mj = seedArray[ii];
        }
        for (int k = 1; k < 5; k++) {
            for (int i = 1; i < 56; i++) {
                seedArray[i] -= seedArray[1 + (i + 30) % 55];
                if (seedArray[i] < 0) seedArray[i] += MBIG;
            }
        }
        inext = 0;
        inextp = 21;
    }

    private int internalSample() {
        int retVal;
        int locINext = inext;
        int locINextp = inextp;
        if (++locINext >= 56) locINext = 1;
        if (++locINextp >= 56) locINextp = 1;
        retVal = seedArray[locINext] - seedArray[locINextp];
        if (retVal == MBIG) retVal--;
        if (retVal < 0) retVal += MBIG;
        seedArray[locINext] = retVal;
        inext = locINext;
        inextp = locINextp;
        return retVal;
    }

    // C# Random.Next()
    public int next() {
        return internalSample();
    }

    public double nextDouble() {
        return internalSample() * (1.0 / MBIG);
    }

    public int next(int maxValue) {
        if (maxValue < 0) throw new RuntimeException("Argument out of range");
        return (int) (internalSample() * (1.0 / MBIG) * maxValue);
    }

    // C# Random.Next(minValue, maxValue)
    public int next(int minValue, int maxValue) {
        if (minValue > maxValue) throw new RuntimeException("Argument out of range");
        long range = (long) maxValue - minValue;
        if (range <= Integer.MAX_VALUE) {
            return (int) (internalSample() * (1.0 / MBIG) * range) + minValue;
        }
        return (int) (getSampleForLargeRange() * range) + minValue;
    }

    // C# Random.GetSampleForLargeRange()
    private double getSampleForLargeRange() {
        int result = internalSample();
        boolean negative = (internalSample() % 2 == 0);
        if (negative) result = -result;
        double d = result;
        d += (Integer.MAX_VALUE - 1);
        d /= 2 * (long) Integer.MAX_VALUE - 1;
        return d;
    }
}
