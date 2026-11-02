// .NET/Mono custom numeric format helpers used by the Hunter display strings (fixed by the golden
// display map). Mono's "P2" invariant format is "12.34 %" (space before the percent sign) and the
// zero-placeholder formats pad the integer part to at least N digits.
package rawr.models.hunter;

import rawr.base.util.NetFormat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class HunterFormat {
    private HunterFormat() {
    }

    /** C# `Math.Round(x, digits)` (banker's rounding) used by the Hunter calculations. */
    public static double roundHalfEven(double value, int digits) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** .NET `##,##0` custom format on a Single. */
    public static String grouped(float value) {
        return NetFormat.grouped(value);
    }

    /** .NET default Single ToString (G7-ish) used by `{n}` format placements. */
    public static String netFloatToString(float value) {
        return rawr.models.mage.NetFormat.netFloatToString(value);
    }

    /** .NET `00.00%` custom format on a Single (zero padded integer part, value scaled by 100). */
    public static String percentCustom(float value, int intDigits, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        bd = bd.multiply(new BigDecimal(100)).setScale(decimals, RoundingMode.HALF_UP);
        boolean negative = bd.signum() < 0;
        String s = bd.abs().toPlainString();
        int dot = s.indexOf('.');
        if (dot < 0) dot = s.length();
        StringBuilder sb = new StringBuilder();
        for (int i = dot; i < intDigits; i++) sb.append('0');
        sb.append(s);
        return (negative ? "-" : "") + sb + "%";
    }

    /** .NET `P2` standard format on a Single (Mono invariant writes a space before `%`). */
    public static String p2(float value) {
        String s = NetFormat.percent(value, 2);
        if (s.endsWith("%")) {
            return s.substring(0, s.length() - 1) + " %";
        }
        return s;
    }

    /** .NET `0000` custom format on a Single (zero padded integer, half-up). */
    public static String zeroPad0(float value, int digits) {
        return zeroPadFixed(value, digits, 0);
    }

    /** .NET `0000.00` custom format on a Single (zero padded integer part + fixed decimals). */
    public static String zeroPadFixed(float value, int intDigits, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        bd = bd.setScale(decimals, RoundingMode.HALF_UP);
        boolean negative = bd.signum() < 0;
        String s = bd.abs().toPlainString();
        int dot = s.indexOf('.');
        if (dot < 0) dot = s.length();
        StringBuilder sb = new StringBuilder();
        for (int i = dot; i < intDigits; i++) sb.append('0');
        sb.append(s);
        return negative ? "-" + sb : sb.toString();
    }

    /** .NET `00,000` custom format on a Single (zero padded grouped integer part). */
    public static String zeroPadGrouped(float value, int intDigits) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        bd = bd.setScale(0, RoundingMode.HALF_UP);
        boolean negative = bd.signum() < 0;
        String s = bd.abs().toPlainString();
        StringBuilder digits = new StringBuilder();
        for (int i = s.length(); i < intDigits; i++) digits.append('0');
        digits.append(s);
        String d = digits.toString();
        StringBuilder sb = new StringBuilder();
        int firstGroup = d.length() % 3;
        if (firstGroup == 0) firstGroup = 3;
        sb.append(d, 0, firstGroup);
        for (int i = firstGroup; i < d.length(); i += 3) {
            sb.append(',').append(d, i, i + 3);
        }
        return negative ? "-" + sb : sb.toString();
    }
}
