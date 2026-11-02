// .NET-compatible number formatting used by the Tree display strings (fixed by the golden
// display map). The C# code uses `Math.Round(x, n)` (banker's rounding) followed by the default
// Single/Double ToString; the rounded values are emitted through the Mage NetFormat helpers.
package rawr.models.tree;

import rawr.base.util.NetFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TreeFormat {
    private TreeFormat() {
    }

    /** C# `Math.Round(double, digits)` (banker's rounding). */
    public static double round(double value, int digits) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** C# `Math.Round(x, digits).ToString()` (a rounded double formatted as G15). */
    public static String roundToString(double value, int digits) {
        return rawr.models.mage.NetFormat.netDoubleToString(round(value, digits));
    }

    /** C# default Single ToString (G7-like). */
    public static String netFloatToString(float value) {
        return rawr.models.mage.NetFormat.netFloatToString(value);
    }

    /** C# default Double ToString (G15-like). */
    public static String netDoubleToString(double value) {
        return rawr.models.mage.NetFormat.netDoubleToString(value);
    }

    /** C# `Math.Ceiling(x).ToString()` (a double formatted as G15). */
    public static String ceilingToString(double value) {
        return netDoubleToString(Math.ceil(value));
    }

    /** .NET `0` custom format on a Single (used by the optimizer labels elsewhere). */
    public static String fixed0(float value) {
        return NetFormat.fixed(value, 0);
    }
}
