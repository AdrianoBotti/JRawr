// .NET-compatible number formatting used by the ported boss types (XmlSerializer "R" output and the
// custom numeric format strings in the display ToString/GenInfoString methods).
package rawr.base.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Number-formatting shims that reproduce .NET/Mono formatting, for the ported code that turns numbers
 * into strings.
 *
 * <p>This class exists because Java's formatting is not .NET's, and the differences are observable:
 * the runtimes disagree about how many significant digits a {@code float} has when printed, about
 * half-up versus half-even rounding, about trailing zeros, and about the shape of scientific
 * notation ({@code 1.5E+05} in both, but Java's {@code %e} uses different digit counts and .NET
 * always writes a two-digit exponent). Rawr's display strings, XML output and golden tests must match
 * the original, so those call sites route through these helpers instead of
 * {@code String.format}/{@code NumberFormat}. The behavior is locked by tests (and the golden
 * suite); the rounding modes and the Mono-induced pre-rounding to seven significant digits are
 * deliberate, not accidents.</p>
 *
 * <h2>Families of formats</h2>
 *
 * <ul>
 *   <li><b>Round-trip</b> ({@link #floatR(float)}, {@link #doubleR(double)}) &mdash; the .NET
 *       {@code XmlSerializer}/"R" format: it first tries a 7/15 significant-digit general rendering
 *       and, when parsing that back does not return the original value, falls back to a 9/17-digit
 *       rendering. Used by the boss XML writer.</li>
 *   <li><b>General</b> ({@link #netFloatToString(float)}, {@link #netDoubleToString(double)}) &mdash;
 *       .NET "G7"/"G15" invariant output used by ported {@code ToString} methods: plain decimal when
 *       the exponent is small, otherwise scientific notation with a signed, zero-padded two-digit
 *       exponent.</li>
 *   <li><b>Custom and standard patterns</b> ({@link #fixed(float, int)},
 *       {@link #optionalDecimal(float, int)}, {@link #grouped(float)}, {@link #grouped(float, int)},
 *       {@link #percent(float, int)}) &mdash; the .NET patterns {@code 0.00}, {@code 0.0#},
 *       {@code #,##0}, {@code N0}..{@code N2} and {@code 0%}.</li>
 * </ul>
 *
 * <p>All non-finite inputs return .NET spellings, which differ by family: the XML round-trip format
 * uses {@code "INF"}/{@code "-INF"} (what XmlSerializer emits) while the display formats use
 * {@code "Infinity"}/{@code "-Infinity"}. Negative zero is preserved by the round-trip formats and
 * printed as {@code "0"} by the display formats, as in .NET. {@code float} inputs are pre-rounded to
 * seven significant digits where Mono does so; do not remove that step. The class is a stateless
 * utility; every method is thread-safe.</p>
 */
public final class NetFormat {
    private NetFormat() {
    }

    /** .NET XmlSerializer / ToString("R") formatting of a Single. */
    public static String floatR(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "INF" : "-INF";
        if (value == 0f) return 1f / value < 0 ? "-0" : "0";
        String s = general(new BigDecimal((double) value), 7);
        if (Float.parseFloat(s) == value) return s;
        return general(new BigDecimal((double) value), 9);
    }

    /** .NET XmlSerializer / ToString("R") formatting of a Double. */
    public static String doubleR(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "INF" : "-INF";
        if (value == 0.0) return 1.0 / value < 0 ? "-0" : "0";
        String s = general(new BigDecimal(value), 15);
        if (Double.parseDouble(s) == value) return s;
        return general(new BigDecimal(value), 17);
    }

    /** .NET/Mono general formatting of a Single (G7, invariant), used by the Core ToString ports. */
    public static String netFloatToString(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == 0f) return "0";
        BigDecimal rounded = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        return general(rounded, 7);
    }

    /** .NET/Mono general formatting of a Double (G15, invariant), used by the Core ToString ports. */
    public static String netDoubleToString(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == 0.0) return "0";
        BigDecimal rounded = new BigDecimal(value).round(new MathContext(15, RoundingMode.HALF_UP));
        return general(rounded, 15);
    }

    /** .NET `0`/`0.0`/`0.00` custom format on a Single (Mono pre-rounds to 7 significant digits). */
    public static String fixed(float value, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        return bd.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /** .NET `0.0#` custom format on a Single (at least minDecimals, at most minDecimals+1). */
    public static String optionalDecimal(float value, int minDecimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        String s = bd.setScale(minDecimals + 1, RoundingMode.HALF_UP).toPlainString();
        if (s.endsWith("0") && s.charAt(s.length() - 2) != '.') {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** .NET `#,##0` custom format on a Single (integer with thousands separators). */
    public static String grouped(float value) {
        return grouped(value, 0);
    }

    /** .NET `N0`/`N1`/`N2` standard format on a Single (grouped, fixed decimals; Mono pre-rounds to 7 digits). */
    public static String grouped(float value, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        String s = bd.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);
        int dot = s.indexOf('.');
        if (dot < 0) dot = s.length();
        StringBuilder sb = new StringBuilder();
        int firstGroup = dot % 3;
        if (firstGroup == 0) firstGroup = 3;
        sb.append(s, 0, firstGroup);
        for (int i = firstGroup; i < dot; i += 3) {
            sb.append(',').append(s, i, i + 3);
        }
        if (dot < s.length()) {
            sb.append(s, dot, s.length());
        }
        return negative ? "-" + sb : sb.toString();
    }

    /** .NET `0%`/`0.0%` custom format on a Single (value scaled by 100). */
    public static String percent(float value, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        return bd.multiply(new BigDecimal(100)).setScale(decimals, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /**
     * Renders a rounded decimal in .NET general form: plain digits when the decimal exponent lies in
     * the range from -4 (inclusive) to the precision (exclusive), otherwise one leading digit, an
     * optional fraction and a signed exponent padded to at least two digits (for example
     * {@code 1.5E-05}).
     */
    private static String general(BigDecimal exact, int precision) {
        BigDecimal rounded = exact.round(new MathContext(precision, RoundingMode.HALF_UP)).stripTrailingZeros();
        int exponent = rounded.precision() - rounded.scale() - 1;
        if (exponent >= -4 && exponent < precision) {
            return rounded.toPlainString();
        }
        String unscaled = rounded.unscaledValue().abs().toString();
        StringBuilder sb = new StringBuilder();
        if (rounded.signum() < 0) sb.append('-');
        sb.append(unscaled.charAt(0));
        if (unscaled.length() > 1) {
            sb.append('.').append(unscaled.substring(1));
        }
        sb.append('E');
        sb.append(exponent < 0 ? '-' : '+');
        int absExponent = Math.abs(exponent);
        if (absExponent < 10) sb.append('0');
        sb.append(absExponent);
        return sb.toString();
    }
}
