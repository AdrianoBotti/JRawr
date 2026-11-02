// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/JsonParser.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the legacy {@code Rawr.JsonParser}. Reads the brace-less
 * {@code "key":value,"key":value} object body used by Wowhead's {@code <json>} and
 * {@code <jsonEquip>} payloads (see {@link #parse(String)}); nested objects and arrays are parsed
 * as values. Numbers without a decimal point stay {@link Integer}, numbers with one become
 * {@link Double} (the original type split); insertion order is preserved
 * ({@link LinkedHashMap}) because the merged key set drives the stats mapping.
 */
public final class JsonParser {

    private JsonParser() {
    }

    private enum Token {
        NONE,
        CURLY_OPEN,
        CURLY_CLOSE,
        SQUARE_OPEN,
        SQUARE_CLOSE,
        COMMA,
        STRING,
        NUMBER,
        COLON,
        FALSE,
        TRUE,
        NULL,
    }

    private static final class Cursor {
        int index;
    }

    private static Token peek(String json, Cursor cursor) {
        consumeWhitespace(json, cursor);
        if (cursor.index >= json.length()) {
            return Token.NONE;
        }
        switch (json.charAt(cursor.index)) {
            case '{':
                return Token.CURLY_OPEN;
            case '}':
                return Token.CURLY_CLOSE;
            case '[':
                return Token.SQUARE_OPEN;
            case ']':
                return Token.SQUARE_CLOSE;
            case ',':
                return Token.COMMA;
            case '"':
                return Token.STRING;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
                return Token.NUMBER;
            case ':':
                return Token.COLON;
            case 't':
            case 'T':
                return Token.TRUE;
            case 'f':
            case 'F':
                return Token.FALSE;
            case 'n':
            case 'N':
                return Token.NULL;
            default:
                return Token.NONE;
        }
    }

    private static Object parseValue(String json, Cursor cursor) {
        switch (peek(json, cursor)) {
            case CURLY_OPEN:
                return parseObject(json, cursor);
            case STRING:
                return parseString(json, cursor);
            case SQUARE_OPEN:
                return parseArray(json, cursor);
            case NUMBER:
                return parseNumber(json, cursor);
            case TRUE:
                return parseTrue(json, cursor);
            case FALSE:
                return parseFalse(json, cursor);
            case NULL:
                return parseNull(json, cursor);
            default:
                throw new IllegalArgumentException(
                        "Unexpected token in JSON at index " + cursor.index);
        }
    }

    private static Object parseNull(String json, Cursor cursor) {
        consumeChar(json, cursor, 'n', 'N');
        consumeChar(json, cursor, 'u', 'U');
        consumeChar(json, cursor, 'l', 'L');
        consumeChar(json, cursor, 'l', 'L');
        return false;
    }

    private static boolean parseFalse(String json, Cursor cursor) {
        consumeChar(json, cursor, 'f', 'F');
        consumeChar(json, cursor, 'a', 'A');
        consumeChar(json, cursor, 'l', 'L');
        consumeChar(json, cursor, 's', 'S');
        consumeChar(json, cursor, 'e', 'E');
        return false;
    }

    private static boolean parseTrue(String json, Cursor cursor) {
        consumeChar(json, cursor, 't', 'T');
        consumeChar(json, cursor, 'r', 'R');
        consumeChar(json, cursor, 'u', 'U');
        consumeChar(json, cursor, 'e', 'E');
        return true;
    }

    private static Object parseNumber(String json, Cursor cursor) {
        int startIndex = cursor.index;
        for (; cursor.index < json.length(); cursor.index++) {
            if ("0123456789+-.eE".indexOf(json.charAt(cursor.index)) == -1) {
                break;
            }
        }
        String number = json.substring(startIndex, cursor.index);
        if (number.contains(".")) {
            return Double.parseDouble(number);
        }
        return Integer.parseInt(number);
    }

    private static Object[] parseArray(String json, Cursor cursor) {
        List<Object> array = new ArrayList<Object>();
        consumeChar(json, cursor, '[');
        if (peek(json, cursor) == Token.SQUARE_CLOSE) {
            consumeChar(json, cursor, ']');
            return array.toArray();
        }
        while (true) {
            array.add(parseValue(json, cursor));
            switch (peek(json, cursor)) {
                case SQUARE_CLOSE:
                    consumeChar(json, cursor, ']');
                    return array.toArray();
                case COMMA:
                    consumeChar(json, cursor, ',');
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unexpected token in JSON at index " + cursor.index);
            }
        }
    }

    private static String parseString(String json, Cursor cursor) {
        consumeChar(json, cursor, '"');
        int start = cursor.index;
        StringBuilder builder = new StringBuilder();
        while (cursor.index < json.length()) {
            switch (json.charAt(cursor.index)) {
                case '"':
                    cursor.index++;
                    return builder.toString();
                case '\\':
                    cursor.index++;
                    switch (json.charAt(cursor.index)) {
                        case '"':
                            cursor.index++;
                            builder.append('"');
                            break;
                        case '\\':
                            cursor.index++;
                            builder.append('\\');
                            break;
                        case '/':
                            cursor.index++;
                            builder.append('/');
                            break;
                        case 'b':
                            cursor.index++;
                            builder.append('\b');
                            break;
                        case 'f':
                            cursor.index++;
                            builder.append('\f');
                            break;
                        case 'n':
                            cursor.index++;
                            builder.append('\n');
                            break;
                        case 'r':
                            cursor.index++;
                            builder.append('\r');
                            break;
                        case 't':
                            cursor.index++;
                            builder.append('\t');
                            break;
                        case 'u':
                            // Faithful port: the original reads the four characters starting at
                            // the 'u', so every unicode escape throws (SUSPECTED_BUGS #34).
                            builder.appendCodePoint(
                                    Integer.parseInt(json.substring(cursor.index, cursor.index + 4), 16));
                            cursor.index += 4;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unexpected escape sequence in JSON at index " + start);
                    }
                    break;
                default:
                    builder.append(json.charAt(cursor.index));
                    cursor.index++;
                    break;
            }
        }
        throw new IllegalArgumentException("Unterminated string in JSON at index " + start);
    }

    private static void consumeChar(String json, Cursor cursor, char... validChars) {
        if (cursor.index >= json.length()) {
            throw new IllegalArgumentException("Unexpected token in JSON at index " + cursor.index);
        }
        char current = json.charAt(cursor.index);
        for (char valid : validChars) {
            if (current == valid) {
                cursor.index++;
                return;
            }
        }
        throw new IllegalArgumentException("Unexpected token in JSON at index " + cursor.index);
    }

    private static void consumeWhitespace(String json, Cursor cursor) {
        while (cursor.index < json.length() && Character.isWhitespace(json.charAt(cursor.index))) {
            cursor.index++;
        }
    }

    private static Map<String, Object> parseObject(String json, Cursor cursor) {
        consumeChar(json, cursor, '{');
        return parseObjectBody(json, cursor, false);
    }

    private static Map<String, Object> parseObjectBody(String json, Cursor cursor, boolean noneTerminated) {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        switch (peek(json, cursor)) {
            case CURLY_CLOSE:
                consumeChar(json, cursor, '}');
                return object;
            case NONE:
                if (!noneTerminated) {
                    throw new IllegalArgumentException(
                            "Unexpected token in JSON at index " + cursor.index);
                }
                return object;
            case STRING:
                break;
            default:
                throw new IllegalArgumentException(
                        "Unexpected token in JSON at index " + cursor.index);
        }
        while (true) {
            String key = parseString(json, cursor);
            consumeWhitespace(json, cursor);
            consumeChar(json, cursor, ':');
            consumeWhitespace(json, cursor);
            object.put(key, parseValue(json, cursor));
            switch (peek(json, cursor)) {
                case CURLY_CLOSE:
                    consumeChar(json, cursor, '}');
                    return object;
                case NONE:
                    if (!noneTerminated) {
                        throw new IllegalArgumentException(
                                "Unexpected token in JSON at index " + cursor.index);
                    }
                    return object;
                case COMMA:
                    consumeChar(json, cursor, ',');
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unexpected token in JSON at index " + cursor.index);
            }
        }
    }

    /**
     * Parses an object body. When {@code json} starts with a quote the leading brace is optional
     * (the Wowhead payload style); a closing brace is only required when one was opened.
     */
    public static Map<String, Object> parse(String json) {
        return parseObjectBody(json, new Cursor(), true);
    }

    /** Copies {@code obj1} and overwrites it field by field with {@code obj2}. */
    public static Map<String, Object> merge(Map<String, Object> obj1, Map<String, Object> obj2) {
        Map<String, Object> merged = new LinkedHashMap<String, Object>(obj1);
        merged.putAll(obj2);
        return merged;
    }
}
