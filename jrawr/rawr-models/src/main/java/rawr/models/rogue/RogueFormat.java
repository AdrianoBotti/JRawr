// New helper (not a line-by-line port): .NET/Mono custom numeric formatting used by the Rogue
// display strings. The golden display map fixes: `{n}` uses the default Single ToString (G7),
// `{n:F0}`/`{n:F1}` are fixed-point, `{n:P1}` is one-decimal percent and `{n:P}` is the .NET
// standard "P" default of two decimals; Mono invariant writes a space before the percent sign.
package rawr.models.rogue;

import rawr.base.util.NetFormat;

public final class RogueFormat {
    private RogueFormat() {
    }

    /** .NET default Single ToString (G7-ish) used by `{n}` format placements. */
    public static String netFloatToString(float value) {
        return rawr.models.mage.NetFormat.netFloatToString(value);
    }

    /** .NET `F0`/`F1`... standard format on a Single. */
    public static String fixed(float value, int decimals) {
        return NetFormat.fixed(value, decimals);
    }

    /** .NET `P0`/`P1`... standard format on a Single (Mono invariant writes a space before `%`). */
    public static String percent(float value, int decimals) {
        String s = NetFormat.percent(value, decimals);
        if (s.endsWith("%")) {
            return s.substring(0, s.length() - 1) + " %";
        }
        return s;
    }
}
