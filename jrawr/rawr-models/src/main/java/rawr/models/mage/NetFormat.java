// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// .NET Framework / Mono-compatible number formatting used by the Mage display values (DECISIONS #23).
package rawr.models.mage;

public final class NetFormat {
    private NetFormat() {
    }

    /** .NET-compatible `F2` formatting of a Single (Mono rounds the G7 decimal first, half away from zero). */
    public static String f2(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        java.math.BigDecimal g7 = new java.math.BigDecimal(value).round(new java.math.MathContext(7, java.math.RoundingMode.HALF_UP));
        return g7.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** .NET-compatible `F2` formatting of a Double (Mono rounds the G15 decimal first, half away from zero). */
    public static String d2(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        java.math.BigDecimal g15 = new java.math.BigDecimal(value).round(new java.math.MathContext(15, java.math.RoundingMode.HALF_UP));
        return g15.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** .NET-compatible general formatting of a Single (G7, invariant). */
    public static String netFloatToString(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == 0f) return "0";
        java.math.BigDecimal rounded = new java.math.BigDecimal(value).round(new java.math.MathContext(7, java.math.RoundingMode.HALF_UP));
        return formatNumber(rounded, 7);
    }

    /** .NET-compatible general formatting of a Double (G15, invariant). */
    public static String netDoubleToString(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == 0.0) return "0";
        java.math.BigDecimal rounded = new java.math.BigDecimal(value).round(new java.math.MathContext(15, java.math.RoundingMode.HALF_UP));
        return formatNumber(rounded, 15);
    }

    private static String formatNumber(java.math.BigDecimal rounded, int precision) {
        java.math.BigDecimal stripped = rounded.stripTrailingZeros();
        int exponent = stripped.precision() - stripped.scale() - 1;
        if (exponent >= -4 && exponent < precision) {
            return stripped.toPlainString();
        }
        String unscaled = stripped.unscaledValue().abs().toString();
        StringBuilder sb = new StringBuilder();
        if (stripped.signum() < 0) sb.append('-');
        sb.append(unscaled.charAt(0));
        if (unscaled.length() > 1) {
            sb.append('.');
            sb.append(unscaled.substring(1));
        }
        sb.append('E');
        sb.append(exponent < 0 ? '-' : '+');
        int absExponent = Math.abs(exponent);
        if (absExponent < 10) sb.append('0');
        sb.append(absExponent);
        return sb.toString();
    }
}
