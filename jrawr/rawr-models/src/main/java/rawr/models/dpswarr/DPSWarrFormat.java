// .NET/Mono custom numeric format helpers used by the DPSWarr display/tooltip strings (fixed by the
// golden display map). Mono runs the oracle on Linux, so Environment.NewLine is "\n".
package rawr.models.dpswarr;

import rawr.base.util.NetFormat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class DPSWarrFormat {
    private DPSWarrFormat() {
    }

    public static final String NL = "\n";

    /** .NET `0`/`0.0`/`0.00` custom format on a Single. */
    public static String fixed(float value, int decimals) {
        return NetFormat.fixed(value, decimals);
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
        return (negative ? "-" : "") + group(s, intDigits);
    }

    /** .NET default Single ToString used by `{n}` format placements. */
    public static String netFloatToString(float value) {
        return rawr.models.mage.NetFormat.netFloatToString(value);
    }

    /**
     * Minimal .NET custom-numeric-format formatter for the DPSWarr display strings. Supports the
     * patterns used by CharacterCalculationsDPSWarr: `0`, `0.0`, `0.#`, `00`, `000`, `0000`, `00000`,
     * `000.00`, `0.00%`, `00.00%`, `0.#%`, `00.#%`, `0.0%`, `00.0%`, `##,##0`, `#,###,##0`, `00,000`.
     * `#` decimals are optional (trailing zeros trimmed); `%` scales by 100.
     */
    public static String custom(float value, String format) {
        boolean percent = format.endsWith("%");
        String core = percent ? format.substring(0, format.length() - 1) : format;
        int dot = core.indexOf('.');
        String intPattern = dot >= 0 ? core.substring(0, dot) : core;
        String decPattern = dot >= 0 ? core.substring(dot + 1) : "";
        int minInt = 0;
        for (int i = 0; i < intPattern.length(); i++) if (intPattern.charAt(i) == '0') minInt++;
        int minDec = 0, maxDec = 0;
        for (int i = 0; i < decPattern.length(); i++) {
            char c = decPattern.charAt(i);
            if (c == '0') {
                minDec++;
                maxDec++;
            } else if (c == '#') {
                maxDec++;
            }
        }
        boolean optionalDec = decPattern.indexOf('#') >= 0;
        boolean grouped = intPattern.indexOf(',') >= 0;

        if (!Float.isFinite(value)) {
            String s = Float.isNaN(value) ? "NaN" : (value > 0 ? "Infinity" : "-Infinity");
            return s + (percent ? "%" : "");
        }
        BigDecimal bd = new BigDecimal((double) value).round(new MathContext(7, RoundingMode.HALF_UP));
        if (percent) bd = bd.multiply(new BigDecimal(100));
        bd = bd.setScale(maxDec, RoundingMode.HALF_UP);
        boolean negative = bd.signum() < 0;
        String s = bd.abs().toPlainString();
        String intPart;
        String decPart;
        int sDot = s.indexOf('.');
        if (sDot < 0) {
            intPart = s;
            decPart = "";
        } else {
            intPart = s.substring(0, sDot);
            decPart = s.substring(sDot + 1);
        }
        if (optionalDec) {
            while (decPart.length() > minDec && decPart.endsWith("0")) {
                decPart = decPart.substring(0, decPart.length() - 1);
            }
            if (decPart.length() < minDec) {
                StringBuilder pad = new StringBuilder(decPart);
                while (pad.length() < minDec) pad.append('0');
                decPart = pad.toString();
            }
        } else {
            StringBuilder pad = new StringBuilder(decPart);
            while (pad.length() < minDec) pad.append('0');
            decPart = pad.toString();
        }
        intPart = padIntDigits(intPart, minInt);
        if (grouped) intPart = group(intPart, 0);
        StringBuilder sb = new StringBuilder();
        if (negative) sb.append('-');
        sb.append(intPart);
        if (decPart.length() > 0) sb.append('.').append(decPart);
        if (percent) sb.append('%');
        return sb.toString();
    }

    private static String padIntDigits(String digits, int minDigits) {
        if (digits.length() >= minDigits) return digits;
        StringBuilder sb = new StringBuilder();
        for (int i = digits.length(); i < minDigits; i++) sb.append('0');
        sb.append(digits);
        return sb.toString();
    }

    private static String group(String digits, int minDigits) {
        String d = padIntDigits(digits, minDigits);
        StringBuilder sb = new StringBuilder();
        int firstGroup = d.length() % 3;
        if (firstGroup == 0) firstGroup = 3;
        sb.append(d, 0, firstGroup);
        for (int i = firstGroup; i < d.length(); i += 3) {
            sb.append(',').append(d, i, i + 3);
        }
        return sb.toString();
    }

    /** .NET `string.Format`-style substitution over a skeleton with `{i}` / `{i:custom}` holes. */
    public static String format(String skeleton, Object... args) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < skeleton.length()) {
            char c = skeleton.charAt(i);
            if (c == '{') {
                int close = skeleton.indexOf('}', i);
                if (close > i) {
                    String token = skeleton.substring(i + 1, close);
                    int colon = token.indexOf(':');
                    String indexPart = colon >= 0 ? token.substring(0, colon) : token;
                    try {
                        int index = Integer.parseInt(indexPart.trim());
                        Object arg = args[index];
                        if (colon >= 0) {
                            String spec = token.substring(colon + 1);
                            if (arg instanceof Number) {
                                out.append(custom(((Number) arg).floatValue(), spec));
                            } else {
                                out.append(arg);
                            }
                        } else {
                            out.append(defaultString(arg));
                        }
                        i = close + 1;
                        continue;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                        // fall through: treat as literal
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String defaultString(Object arg) {
        if (arg == null) return "";
        if (arg instanceof Float) return netFloatToString((Float) arg);
        if (arg instanceof Double) return rawr.models.mage.NetFormat.netFloatToString(((Double) arg).floatValue());
        return String.valueOf(arg);
    }
}
