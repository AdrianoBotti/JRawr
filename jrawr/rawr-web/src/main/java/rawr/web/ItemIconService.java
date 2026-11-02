package rawr.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * View helper exposed to Thymeleaf as {@code ${icons}}. Rawr ships no icon images, so icon URLs
 * are built from a user-chosen site base (e.g. {@code https://wotlkdb.com}) stored in
 * {@value #BASE_URL_COOKIE}; a per-host layout map turns that base into a full icon URL. The
 * optional advanced cookie {@value #TEMPLATE_COOKIE} overrides the map with a full URL template
 * containing {@value #ICON_PLACEHOLDER}. The {@code jrawr.icon-base-url} property is the fallback
 * when no cookie is set (also a site base). Legacy cookies that still hold a full icon base with a
 * path are normalized to their origin on read, which keeps them working for AoWoW-style hosts.
 */
@Component("icons")
public class ItemIconService {

    /** Cookie holding the user-chosen site base URL (30 days, set by {@code POST /settings}). */
    public static final String BASE_URL_COOKIE = "jrawr_icon_base_url";

    /** Cookie holding the optional advanced icon URL template, with a literal {@value #ICON_PLACEHOLDER}. */
    public static final String TEMPLATE_COOKIE = "jrawr_icon_template";

    /** Placeholder replaced with the item's icon name (no extension) in templates. */
    public static final String ICON_PLACEHOLDER = "{icon}";

    /** Placeholder replaced with the normalized site base in layout templates. */
    public static final String BASE_PLACEHOLDER = "{base}";

    /** Real icon name used by the settings modal's live example hint. */
    public static final String EXAMPLE_ICON = "inv_helmet_150black";

    /**
     * Ordered host layout rules; the first matching host wins and subdomains match too. Keep this
     * map in sync with the data rendered to the settings hint (see {@link #layoutRulesJson()}).
     */
    static final List<LayoutRule> LAYOUT_RULES = List.of(
            new LayoutRule("wotlkdb.com",
                    BASE_PLACEHOLDER + "/static/images/wow/icons/large/" + ICON_PLACEHOLDER + ".jpg"),
            new LayoutRule("evowow.com",
                    BASE_PLACEHOLDER + "/static/images/wow/icons/large/" + ICON_PLACEHOLDER + ".jpg"),
            new LayoutRule("wowhead.com",
                    "https://wow.zamimg.com/images/wow/icons/large/" + ICON_PLACEHOLDER + ".jpg"),
            new LayoutRule("zamimg.com",
                    BASE_PLACEHOLDER + "/images/wow/icons/large/" + ICON_PLACEHOLDER + ".jpg"),
            new LayoutRule("render.worldofwarcraft.com",
                    BASE_PLACEHOLDER + "/icons/56/" + ICON_PLACEHOLDER + ".jpg"));

    /** AoWoW layout used for hosts without a dedicated rule. */
    static final String DEFAULT_LAYOUT =
            BASE_PLACEHOLDER + "/static/images/wow/icons/large/" + ICON_PLACEHOLDER + ".jpg";

    private static final String FALLBACK_BASE = "https://wow.zamimg.com";

    /** One selectable icon source in Settings. */
    public record SourceOption(String base, String label) {
    }

    /** Known icon sources offered by the Settings dropdown. */
    public static final List<SourceOption> SOURCE_OPTIONS = List.of(
            new SourceOption("https://wow.zamimg.com", "zamimg (default)"),
            new SourceOption("https://wotlkdb.com", "wotlkdb"),
            new SourceOption("https://wowhead.com", "Wowhead"),
            new SourceOption("https://wotlk.evowow.com", "EvoWow"),
            new SourceOption("https://render.worldofwarcraft.com", "WoW render CDN"));

    private final String defaultBaseUrl;

    public ItemIconService(@Value("${jrawr.icon-base-url:https://wow.zamimg.com}") String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        this.defaultBaseUrl = normalized != null ? normalized : FALLBACK_BASE;
    }

    /** The effective site base: normalized request cookie first, normalized property otherwise. */
    public String baseUrl() {
        String fromCookie = normalizeBaseUrl(cookieValue(BASE_URL_COOKIE));
        return fromCookie != null ? fromCookie : defaultBaseUrl;
    }

    /** The advanced template cookie value, or an empty string when unset or invalid. */
    public String template() {
        String value = cookieValue(TEMPLATE_COOKIE);
        return isValidTemplate(value) ? value.trim() : "";
    }

    /** The icon URL the current site base resolves {@link #EXAMPLE_ICON} to (modal hint). */
    public String exampleUrl() {
        return webUrl(EXAMPLE_ICON);
    }

    /** The example icon name, exposed to the modal's live hint script. */
    public String exampleIcon() {
        return EXAMPLE_ICON;
    }

    /** Icon URL for the item: always hotlinked from the configured site, never cached locally. */
    public String url(String iconPath) {
        return webUrl(iconPath);
    }

    /**
     * The CDN icon URL the current site base/template resolves the icon to. Null when the item has
     * no icon path (the template shows a fallback).
     */
    public String webUrl(String iconPath) {
        if (iconPath == null || iconPath.isBlank()) {
            return null;
        }
        String icon = iconPath.toLowerCase(Locale.ROOT);
        String template = cookieTemplate();
        if (template == null) {
            template = layoutTemplate(baseUrl());
        }
        return template.replace(ICON_PLACEHOLDER, icon);
    }

    /** The layout template a normalized site base resolves to; AoWoW layout as the default. */
    public static String layoutTemplate(String baseUrl) {
        String host = hostOf(baseUrl);
        if (host != null) {
            for (LayoutRule rule : LAYOUT_RULES) {
                if (host.equals(rule.host()) || host.endsWith("." + rule.host())) {
                    return rule.template().replace(BASE_PLACEHOLDER, baseUrl);
                }
            }
        }
        return DEFAULT_LAYOUT.replace(BASE_PLACEHOLDER, baseUrl);
    }

    /**
     * Normalizes a site base to {@code scheme://host[:port]}: lowercases scheme and host, drops any
     * path, query, fragment, userinfo and trailing slash. Returns null when the value is not an
     * absolute http(s) URL with a host. Legacy full icon base URLs normalize to their origin; the
     * host layout then rebuilds the icon path for AoWoW-style hosts.
     */
    public static String normalizeBaseUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                return null;
            }
            if (uri.getUserInfo() != null) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort();
            if (port != -1 && (port < 1 || port > 65535)) {
                return null;
            }
            StringBuilder normalized = new StringBuilder(scheme)
                    .append("://")
                    .append(host.toLowerCase(Locale.ROOT));
            if (port != -1) {
                normalized.append(':').append(port);
            }
            return normalized.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * True for an absolute http(s) URL template containing the literal {@value #ICON_PLACEHOLDER}
     * placeholder and no whitespace.
     */
    public static boolean isValidTemplate(String template) {
        if (template == null) {
            return false;
        }
        String trimmed = template.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))
                || !trimmed.contains(ICON_PLACEHOLDER)) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** JSON with the host layout rules, rendered into the settings form for the live hint script. */
    public String layoutRulesJson() {
        StringBuilder json = new StringBuilder("{\"rules\":[");
        for (LayoutRule rule : LAYOUT_RULES) {
            if (json.charAt(json.length() - 1) != '[') {
                json.append(',');
            }
            json.append("{\"host\":\"").append(rule.host())
                    .append("\",\"template\":\"").append(rule.template()).append("\"}");
        }
        return json.append("],\"fallback\":\"").append(DEFAULT_LAYOUT).append("\"}").toString();
    }

    /** Up to two uppercase initials, used by the CSS fallback square. */
    public String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        StringBuilder initials = new StringBuilder(2);
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                initials.append(Character.toUpperCase(word.charAt(0)));
                if (initials.length() == 2) {
                    break;
                }
            }
        }
        return initials.toString();
    }

    public String tooltipUrl(int itemId, String slot) {
        return tooltipUrl(itemId, slot, false);
    }

    /**
     * Tooltip URL for an item, optionally bound to a slot (equipped instance stats) and flagged as
     * a picker candidate (set bonuses simulated for that slot).
     */
    public String tooltipUrl(int itemId, String slot, boolean picker) {
        StringBuilder url = new StringBuilder("/item-tooltip?id=").append(itemId);
        if (slot != null && !slot.isBlank()) {
            url.append("&slot=").append(slot);
        }
        if (picker) {
            url.append("&picker=true");
        }
        return url.toString();
    }

    /** Tooltip URL for an exact gemmed/enchanted comparison instance. */
    public String instanceTooltipUrl(int itemId, String slot, String gemmedId) {
        StringBuilder url = new StringBuilder(tooltipUrl(itemId, slot, false));
        if (gemmedId != null && !gemmedId.isBlank()) {
            url.append("&instance=").append(gemmedId);
        }
        return url.toString();
    }

    private String cookieTemplate() {
        String value = cookieValue(TEMPLATE_COOKIE);
        return isValidTemplate(value) ? value.trim() : null;
    }

    private String cookieValue(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String hostOf(String siteBase) {
        String normalized = normalizeBaseUrl(siteBase);
        if (normalized == null) {
            return null;
        }
        try {
            String host = new URI(normalized).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** One host entry of the icon layout map. */
    record LayoutRule(String host, String template) {
    }
}
