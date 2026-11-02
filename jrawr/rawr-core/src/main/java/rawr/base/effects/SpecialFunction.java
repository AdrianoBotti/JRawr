// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialFunction.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: only the functions needed by the Mage closure are ported. The Cephes implementations are
// replaced by Apache Commons Math 3.6.1 (TD-006); the cache/interpolation layer is unchanged.
package rawr.base.effects;

import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.special.Gamma;

/**
 * Numerical special functions used by the proc-uptime mathematics, plus a precomputed interpolation
 * grid for the incomplete beta function.
 *
 * <p>{@link #gamma(double)}, {@link #logGamma(double)}, {@link #erf(double)}, {@link #erfc(double)},
 * {@link #normal(double)} and {@link #ibeta(double, double, double)} delegate to Apache Commons Math
 * 3.6.1 (TD-006, DECISIONS #75). The public shapes and the domain exceptions are kept compatible
 * with the original Cephes port; the interpolation cache below still precomputes the same grid
 * because it is part of the engine's observable calculation mode.</p>
 */
public final class SpecialFunction {
    private static final double SQRT_HALF = Math.sqrt(0.5);

    private SpecialFunction() {
    }

    public static double gamma(double x) {
        try {
            return Gamma.gamma(x);
        } catch (RuntimeException e) {
            throw new ArithmeticException("gamma: singular");
        }
    }

    public static double logGamma(double x) {
        if (x > 0.0) {
            return Gamma.logGamma(x);
        }
        double value = Gamma.gamma(x);
        if (Double.isNaN(value) || Double.isInfinite(value) || value == 0.0) {
            throw new ArithmeticException("lgamma: Overflow");
        }
        return Math.log(Math.abs(value));
    }

    /** Area under the Gaussian probability density from -infinity to {@code a}. */
    public static double normal(double a) {
        return 0.5 * Erf.erfc(-a * SQRT_HALF);
    }

    public static double erf(double x) {
        return Erf.erf(x);
    }

    public static double erfc(double a) {
        return Erf.erfc(a);
    }

    public static float ibeta(int a, float b, float x) {
        return (float) ibeta((double) a, (double) b, (double) x);
    }

    /** Regularized incomplete beta {@code I_x(a,b)}; domain errors keep the original exception type. */
    public static double ibeta(double aa, double bb, double xx) {
        if (aa <= 0.0 || bb <= 0.0) {
            throw new ArithmeticException("ibeta: Domain error!");
        }
        if (xx <= 0.0) {
            if (xx == 0.0) return 0.0;
            throw new ArithmeticException("ibeta: Domain error!");
        }
        if (xx >= 1.0) {
            if (xx == 1.0) return 1.0;
            throw new ArithmeticException("ibeta: Domain error!");
        }
        try {
            return Beta.regularizedBeta(xx, aa, bb);
        } catch (RuntimeException e) {
            throw new ArithmeticException("ibeta: Domain error!");
        }
    }

    private static final int IBETA_CACHE_SIZE = 100;
    private static final int IBETA_B_RESOLUTION = 100;
    private static final int IBETA_X_RESOLUTION = 100;

    private static final class InterpolationData {
        float[] y;
        float bmin;
        float bmax;
    }

    private static final InterpolationData[][] IBETA_CACHE = calculateIbetaCache();

    private static InterpolationData[][] calculateIbetaCache() {
        InterpolationData[][] cache = new InterpolationData[IBETA_CACHE_SIZE][];

        for (int a = 0; a < IBETA_CACHE_SIZE; a++) {
            cache[a] = new InterpolationData[IBETA_X_RESOLUTION + 1];
            for (int i = 1; i < IBETA_X_RESOLUTION; i++) {
                cache[a][i] = new InterpolationData();
                cache[a][i].y = new float[IBETA_B_RESOLUTION + 1];

                double x = i / (double) IBETA_X_RESOLUTION;
                double bmin = bisectIbeta(a + 1, x, 0.0001, 0.00001);
                double bmax = bisectIbeta(a + 1, x, 0.9999, 0.00001);

                cache[a][i].bmin = (float) bmin;
                cache[a][i].bmax = (float) bmax;
                for (int j = 0; j <= IBETA_B_RESOLUTION; j++) {
                    cache[a][i].y[j] = (float) ibeta(a + 1, bmin + (bmax - bmin) * j / (double) IBETA_B_RESOLUTION, x);
                }
            }
        }
        return cache;
    }

    private static double bisectIbeta(double a, double x, double y, double epsilon) {
        double low = 0;
        double lowy = 0;
        double high = 100;
        double highy = ibeta(a, high, x);

        while (highy < y) {
            high += 100;
            highy = ibeta(a, high, x);
        }

        while (highy - lowy > epsilon) {
            double mid = (low + high) / 2.0;
            double midy = ibeta(a, mid, x);

            if (midy > y) {
                high = mid;
                highy = midy;
            } else {
                low = mid;
                lowy = midy;
            }
        }

        return (low + high) / 2.0;
    }

    public static float ibetaInterpolated(int a, float b, float x) {
        if (a > IBETA_CACHE_SIZE) {
            return (float) ibeta(a, b, x);
        }
        a -= 1;

        float xx = x * IBETA_X_RESOLUTION;
        int i = (int) xx;
        if (i >= IBETA_X_RESOLUTION) return 1.0f;

        float y0;
        float y1;
        if (i >= 2) {
            y0 = interpolateAux(IBETA_CACHE[a][i - 1], b);
            y1 = interpolateAux(IBETA_CACHE[a][i], b);
        } else if (i == 1) {
            y0 = 0.0f;
            y1 = interpolateAux(IBETA_CACHE[a][i], b);
        } else {
            y0 = 0.0f;
            y1 = 0.0f;
        }
        float y2;
        float y3;
        if (i < IBETA_X_RESOLUTION - 2) {
            y2 = interpolateAux(IBETA_CACHE[a][i + 1], b);
            y3 = interpolateAux(IBETA_CACHE[a][i + 2], b);
        } else if (i == IBETA_X_RESOLUTION - 2) {
            y2 = interpolateAux(IBETA_CACHE[a][i + 1], b);
            y3 = 1.0f;
        } else {
            y2 = 1.0f;
            y3 = 1.0f;
        }
        xx -= i;
        float y = 0.5f * (y0 * (xx * ((2 - xx) * xx - 1)) + y1 * (xx * xx * (3 * xx - 5) + 2) + y2 * (xx * ((4 - 3 * xx) * xx + 1)) + y3 * ((xx - 1) * xx * xx));
        return Math.min(1.0f, y);
    }

    private static float interpolateAux(InterpolationData data, float b) {
        if (b < data.bmin) {
            return data.y[0] * b / data.bmin;
        } else if (b > data.bmax) {
            float d = b - data.bmax;
            return data.y[IBETA_B_RESOLUTION] + (1.0f - data.y[IBETA_B_RESOLUTION]) * d / (1 + d);
        } else {
            float d = (b - data.bmin) * IBETA_B_RESOLUTION / (data.bmax - data.bmin);
            int j = (int) d;
            float y0;
            if (j == 0) {
                y0 = 0f;
            } else {
                y0 = data.y[j - 1];
            }
            float y1 = data.y[j];
            float y2;
            float y3;
            if (j <= IBETA_B_RESOLUTION - 2) {
                y2 = data.y[j + 1];
                y3 = data.y[j + 2];
            } else if (j == IBETA_B_RESOLUTION - 1) {
                y2 = data.y[j + 1];
                y3 = 1f;
            } else {
                y2 = 1f;
                y3 = 1f;
            }
            d -= j;
            return 0.5f * (y0 * (d * ((2 - d) * d - 1)) + y1 * (d * d * (3 * d - 5) + 2) + y2 * (d * ((4 - 3 * d) * d + 1)) + y3 * ((d - 1) * d * d));
        }
    }

    public static float ibetaInterpolatedLinear(int a, float b, float x) {
        if (a > IBETA_CACHE_SIZE) {
            return (float) ibeta(a, b, x);
        }
        a -= 1;

        float xx = x * IBETA_X_RESOLUTION;
        int i = (int) xx;
        if (i >= IBETA_X_RESOLUTION) return 1.0f;

        float y1;
        if (i >= 1) {
            y1 = interpolateAuxLinear(IBETA_CACHE[a][i], b);
        } else {
            y1 = 0.0f;
        }
        float y2;
        if (i <= IBETA_X_RESOLUTION - 2) {
            y2 = interpolateAuxLinear(IBETA_CACHE[a][i + 1], b);
        } else {
            y2 = 1.0f;
        }
        xx -= i;
        return y1 + xx * (y2 - y1);
    }

    private static float interpolateAuxLinear(InterpolationData data, float b) {
        if (b < data.bmin) {
            return data.y[0] * b / data.bmin;
        } else if (b > data.bmax) {
            float d = b - data.bmax;
            return data.y[IBETA_B_RESOLUTION] + (1.0f - data.y[IBETA_B_RESOLUTION]) * d / (1 + d);
        } else {
            float d = (b - data.bmin) * IBETA_B_RESOLUTION / (data.bmax - data.bmin);
            int j = (int) d;
            float y1 = data.y[j];
            float y2;
            if (j <= IBETA_B_RESOLUTION - 1) {
                y2 = data.y[j + 1];
            } else {
                y2 = 1f;
            }
            d -= j;
            return y1 + d * (y2 - y1);
        }
    }
}
