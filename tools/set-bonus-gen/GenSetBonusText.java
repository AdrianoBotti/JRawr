// Dev-time generator for jrawr/rawr-web/src/main/resources/set-bonus-text.json.
// Not part of the Gradle build and never run by tests: it fetches item tooltips from an AoWoW
// database (network) to capture the real WoW set-bonus prose that Rawr's BuffCache.xml lacks.
//
// Usage (from the JRawr repo root):
//   java -cp jrawr/rawr-core/build/classes/java/main:jrawr/rawr-data/build/classes/java/main \
//        tools/set-bonus-gen/GenSetBonusText.java [baseUrl] [outFile]
// Defaults: baseUrl https://wotlkdb.com, out jrawr/rawr-web/src/main/resources/set-bonus-text.json.
// Data dir: -Djrawr.data-dir=<dir>, default legacy/RAWR_WotLK_3.3.5a/bin/Release/Data.
// See README.md next to this file for provenance and regeneration notes.

import rawr.base.Buff;
import rawr.base.Item;
import rawr.base.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.ItemCacheLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenSetBonusText {
    private static final String DEFAULT_BASE = "https://wotlkdb.com";
    private static final String FALLBACK_BASE = "https://wotlk.evowow.com";
    private static final String DEFAULT_DATA_DIR = "legacy/RAWR_WotLK_3.3.5a/bin/Release/Data";
    private static final String DEFAULT_OUT = "jrawr/rawr-web/src/main/resources/set-bonus-text.json";
    private static final Path CACHE_DIR = Paths.get("/tmp/opencode/set-bonus-cache");
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0 Safari/537.36";
    private static final long REQUEST_DELAY_MS = 400L;

    private static final Pattern TOOLTIP =
            Pattern.compile("<htmlTooltip><!\\[CDATA\\[(.*?)\\]\\]></htmlTooltip>", Pattern.DOTALL);
    private static final Pattern SET_LINE = Pattern.compile("\\((\\d+)\\)\\s*Set:\\s*");
    private static final Pattern ENTITY =
            Pattern.compile("&(#x[0-9A-Fa-f]+|#[0-9]+|[A-Za-z][A-Za-z0-9]*);");

    private static final HttpClient CLIENT =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static long lastRequestAt = 0L;

    private GenSetBonusText() {
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = stripTrailingSlash(args.length > 0 ? args[0] : DEFAULT_BASE);
        Path outFile = Paths.get(args.length > 1 ? args[1] : DEFAULT_OUT);
        Path dataDir = Paths.get(System.getProperty("jrawr.data-dir", DEFAULT_DATA_DIR));
        if (!Files.isDirectory(dataDir)) {
            System.err.println("Data directory not found: " + dataDir.toAbsolutePath());
            System.exit(2);
        }

        System.out.println("data dir:  " + dataDir.toAbsolutePath());
        System.out.println("primary:   " + baseUrl);
        System.out.println("fallback:  " + FALLBACK_BASE);
        System.out.println();

        BuffLoader.load(dataDir.resolve("BuffCache.xml"));
        ItemCacheLoader.load(dataDir.resolve("ItemCache.xml"));

        Map<String, TreeSet<Integer>> thresholds = collectSetThresholds();
        Map<String, Item> firstItems = firstItemPerSet();

        Map<String, Map<Integer, String>> resolved = new TreeMap<String, Map<Integer, String>>();
        List<String> failed = new ArrayList<String>();
        int fromPrimary = 0;
        int fromFallback = 0;
        Boolean primaryHasSetData = null;
        int done = 0;
        for (Map.Entry<String, TreeSet<Integer>> entry : thresholds.entrySet()) {
            String setName = entry.getKey();
            Item item = firstItems.get(setName);
            done++;
            if (item == null) {
                System.err.println("[" + done + "/" + thresholds.size() + "] " + setName
                        + ": no matching item in ItemCache");
                failed.add(setName);
                continue;
            }

            Map<Integer, String> bonuses = new LinkedHashMap<Integer, String>();
            if (primaryHasSetData == null || primaryHasSetData) {
                String body = fetch(baseUrl, item.getId());
                if (body != null) {
                    bonuses = parseSetBonuses(body);
                    if (primaryHasSetData == null && !baseUrl.equals(FALLBACK_BASE)) {
                        primaryHasSetData = !bonuses.isEmpty();
                        if (!primaryHasSetData) {
                            System.out.println("note: " + baseUrl + " returned no set-bonus text for item "
                                    + item.getId() + "; falling back to " + FALLBACK_BASE
                                    + " for the remaining sets");
                        }
                    }
                }
            }
            boolean usedFallback = false;
            if (bonuses.isEmpty() && !baseUrl.equals(FALLBACK_BASE)) {
                String body = fetch(FALLBACK_BASE, item.getId());
                if (body != null) {
                    bonuses = parseSetBonuses(body);
                    usedFallback = !bonuses.isEmpty();
                }
            }

            Map<Integer, String> mapped = new LinkedHashMap<Integer, String>();
            for (Integer threshold : entry.getValue()) {
                String text = bonuses.get(threshold);
                if (text != null) {
                    mapped.put(threshold, text);
                }
            }
            if (mapped.isEmpty()) {
                System.err.println("[" + done + "/" + thresholds.size() + "] " + setName
                        + " (item " + item.getId() + "): no set-bonus lines parsed");
                failed.add(setName);
            } else {
                resolved.put(setName, mapped);
                if (usedFallback) {
                    fromFallback++;
                } else {
                    fromPrimary++;
                }
                System.out.println("[" + done + "/" + thresholds.size() + "] " + setName
                        + " (item " + item.getId() + "): " + mapped.keySet()
                        + (usedFallback ? " [fallback]" : ""));
            }
        }

        writeJson(outFile, resolved);

        System.out.println();
        System.out.println("sets total: " + thresholds.size());
        System.out.println("resolved:   " + resolved.size()
                + " (primary " + fromPrimary + ", fallback " + fromFallback + ")");
        System.out.println("failed:     " + failed.size());
        for (String setName : failed) {
            System.out.println("  - " + setName);
        }
        System.out.println();
        System.out.println("wrote " + outFile.toAbsolutePath());
        int samples = 0;
        for (Map.Entry<String, Map<Integer, String>> entry : resolved.entrySet()) {
            for (Map.Entry<Integer, String> bonus : entry.getValue().entrySet()) {
                System.out.println("sample: " + entry.getKey() + " (" + bonus.getKey() + ") Set: "
                        + bonus.getValue());
                if (++samples >= 3) {
                    return;
                }
            }
        }
    }

    /** Distinct set names with their buff thresholds, in name order, thresholds ascending. */
    private static Map<String, TreeSet<Integer>> collectSetThresholds() {
        Map<String, TreeSet<Integer>> thresholds = new TreeMap<String, TreeSet<Integer>>();
        for (Buff buff : Buff.getAllBuffs()) {
            if (buff.setName != null && !buff.setName.isEmpty() && buff.setThreshold > 0) {
                TreeSet<Integer> set = thresholds.get(buff.setName);
                if (set == null) {
                    set = new TreeSet<Integer>();
                    thresholds.put(buff.setName, set);
                }
                set.add(buff.setThreshold);
            }
        }
        return thresholds;
    }

    /** First item (lowest id, ItemCache order) per set name present in the item cache. */
    private static Map<String, Item> firstItemPerSet() {
        Map<String, Item> first = new HashMap<String, Item>();
        for (Item item : ItemCache.allItems()) {
            String setName = item.getSetName();
            if (setName != null && !setName.isEmpty() && !first.containsKey(setName)) {
                first.put(setName, item);
            }
        }
        return first;
    }

    /** Returns the raw tooltip XML, from the on-disk cache when present; null when both attempts fail. */
    private static String fetch(String baseUrl, int itemId) {
        String host = host(baseUrl);
        Path cacheFile = CACHE_DIR.resolve(host + "_item_" + itemId + ".xml");
        if (Files.isRegularFile(cacheFile)) {
            try {
                String cached = Files.readString(cacheFile, StandardCharsets.UTF_8);
                if (!cached.isEmpty()) {
                    return cached;
                }
            } catch (Exception e) {
                System.err.println("cache read failed for " + cacheFile + ": " + e);
            }
        }
        String url = baseUrl + "/?item=" + itemId + "&xml";
        for (int attempt = 1; attempt <= 2; attempt++) {
            rateLimit();
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
                HttpResponse<String> response =
                        CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200 && response.body().contains("<htmlTooltip>")) {
                    try {
                        Files.createDirectories(CACHE_DIR);
                        Files.writeString(cacheFile, response.body(), StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (Exception e) {
                        System.err.println("cache write failed for " + cacheFile + ": " + e);
                    }
                    return response.body();
                }
                System.err.println("fetch " + url + " -> HTTP " + response.statusCode()
                        + (attempt == 1 ? ", retrying" : ", giving up"));
            } catch (Exception e) {
                System.err.println("fetch " + url + " -> " + e
                        + (attempt == 1 ? ", retrying" : ", giving up"));
            }
            try {
                Thread.sleep(REQUEST_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static synchronized void rateLimit() {
        long wait = lastRequestAt + REQUEST_DELAY_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    /** Maps threshold to prose from the tooltip text, up to the next "(N) Set:" line. */
    private static Map<Integer, String> parseSetBonuses(String xml) {
        Map<Integer, String> bonuses = new LinkedHashMap<Integer, String>();
        Matcher tooltip = TOOLTIP.matcher(xml);
        if (!tooltip.find()) {
            return bonuses;
        }
        String text = toPlainText(tooltip.group(1));
        Matcher matcher = SET_LINE.matcher(text);
        List<Integer> thresholds = new ArrayList<Integer>();
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> ends = new ArrayList<Integer>();
        while (matcher.find()) {
            thresholds.add(Integer.valueOf(matcher.group(1)));
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        for (int i = 0; i < thresholds.size(); i++) {
            int to = i + 1 < thresholds.size() ? starts.get(i + 1) : text.length();
            String prose = text.substring(ends.get(i), to).replaceAll("\\s+", " ").trim();
            if (!prose.isEmpty() && !bonuses.containsKey(thresholds.get(i))) {
                bonuses.put(thresholds.get(i), prose);
            }
        }
        return bonuses;
    }

    /** Strips tags (br/tr become newlines) and decodes HTML entities. */
    private static String toPlainText(String html) {
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</tr>", "\n")
                .replaceAll("<[^>]*>", "");
        Matcher matcher = ENTITY.matcher(text);
        StringBuilder out = new StringBuilder(text.length());
        int last = 0;
        while (matcher.find()) {
            out.append(text, last, matcher.start());
            String entity = matcher.group(1);
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                out.appendCodePoint(Integer.parseInt(entity.substring(2), 16));
            } else if (entity.startsWith("#")) {
                out.appendCodePoint(Integer.parseInt(entity.substring(1)));
            } else {
                out.append(switch (entity) {
                    case "amp" -> "&";
                    case "lt" -> "<";
                    case "gt" -> ">";
                    case "quot" -> "\"";
                    case "apos" -> "'";
                    case "nbsp" -> " ";
                    default -> matcher.group(0);
                });
            }
            last = matcher.end();
        }
        out.append(text, last, text.length());
        return out.toString();
    }

    private static void writeJson(Path outFile, Map<String, Map<Integer, String>> data) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        int setIndex = 0;
        for (Map.Entry<String, Map<Integer, String>> entry : data.entrySet()) {
            json.append("  \"").append(escape(entry.getKey())).append("\": {\n");
            int thresholdIndex = 0;
            for (Map.Entry<Integer, String> bonus : entry.getValue().entrySet()) {
                json.append("    \"").append(bonus.getKey()).append("\": \"")
                        .append(escape(bonus.getValue())).append("\"")
                        .append(++thresholdIndex < entry.getValue().size() ? "," : "").append("\n");
            }
            json.append("  }").append(++setIndex < data.size() ? "," : "").append("\n");
        }
        json.append("}\n");
        Path parent = outFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outFile, json.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String stripTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "unknown" : host;
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
    }
}
