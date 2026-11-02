package rawr.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Add-items input into item IDs. It accepts bare IDs, classic item links
 * ({@code |Hitem:...|h[Name]|h}, including a colour prefix) and Wowhead-style URLs
 * ({@code ?item=123}, {@code /item/123}, {@code item=123}, {@code item:123}). IDs are
 * de-duplicated in input order and capped at {@link #MAX_IDS}; tokens that are neither item
 * markup nor a number are returned as invalid so the UI can report them.
 */
public final class ItemIdParser {

    /** Maximum number of IDs one batch may fetch. */
    public static final int MAX_IDS = 50;

    private static final Pattern LINK = Pattern.compile(
            "(?i)(?:\\|c[0-9a-f]{8})?\\|Hitem:(\\d+)(?::[^|]*)?\\|h(?:\\[[^\\]]*\\])?(?:\\|h)?");

    private static final Pattern URL = Pattern.compile(
            "(?i)(?:https?://[^\\s]*?)?(?:[?&]item=|/item/|item=|item:)(\\d+)[^\\s]*");

    /** One parsed batch: ordered unique IDs plus the tokens that were not understood. */
    public record Parsed(List<Integer> ids, List<String> invalidTokens, boolean truncated) {
    }

    private ItemIdParser() {
    }

    public static Parsed parse(String input) {
        String text = input == null ? "" : input;
        Set<Integer> ids = new LinkedHashSet<Integer>();
        String remaining = collect(text, LINK, ids);
        remaining = collect(remaining, URL, ids);

        List<String> invalid = new ArrayList<String>();
        boolean truncated = false;
        for (String token : remaining.split("[,\\s;]+")) {
            String candidate = trimPunctuation(token);
            if (candidate.isEmpty() || candidate.startsWith("|")) {
                continue;
            }
            Integer value = positiveId(candidate);
            if (value != null) {
                if (ids.add(value) && ids.size() > MAX_IDS) {
                    truncated = true;
                }
            } else {
                invalid.add(candidate);
            }
        }

        List<Integer> ordered = new ArrayList<Integer>(ids);
        if (ordered.size() > MAX_IDS) {
            ordered = new ArrayList<Integer>(ordered.subList(0, MAX_IDS));
            truncated = true;
        }
        return new Parsed(List.copyOf(ordered), List.copyOf(invalid), truncated);
    }

    private static String collect(String text, Pattern pattern, Set<Integer> ids) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder remaining = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            ids.add(Integer.valueOf(matcher.group(1)));
            remaining.append(text, last, matcher.start()).append(' ');
            last = matcher.end();
        }
        remaining.append(text.substring(last));
        return remaining.toString();
    }

    private static Integer positiveId(String token) {
        if (token.isEmpty() || token.length() > 10) {
            return null;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return null;
            }
        }
        try {
            int value = Integer.parseInt(token);
            return value > 0 ? Integer.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimPunctuation(String token) {
        String value = token.trim();
        while (!value.isEmpty() && "#()[]{}.,:'\"<>".indexOf(value.charAt(0)) >= 0) {
            value = value.substring(1);
        }
        while (!value.isEmpty() && "#()[]{}.,:'\"<>".indexOf(value.charAt(value.length() - 1)) >= 0) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
