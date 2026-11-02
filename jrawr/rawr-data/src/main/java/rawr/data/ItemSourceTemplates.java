package rawr.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * Item lookup URL templates (DECISIONS #31). The Item editor offers three known database
 * presets and a user-provided absolute template; the template contains the literal
 * {@value #ID_PLACEHOLDER} placeholder. The icon-site base is deliberately not part of item
 * lookup anymore: images are configured in Settings, item sources in the Item editor.
 */
public final class ItemSourceTemplates {

    /** Placeholder replaced with the numeric item ID. */
    public static final String ID_PLACEHOLDER = "{id}";

    /** One selectable item-source preset. */
    public record Preset(String key, String label, String template) {
    }

    /** The known sources, in display order. */
    public static final List<Preset> PRESETS = List.of(
            new Preset("wowhead", "Wowhead (WotLK)",
                    "https://www.wowhead.com/wotlk/?item=" + ID_PLACEHOLDER + "&xml"),
            new Preset("wotlkdb", "wotlkdb (AoWoW)",
                    "https://wotlkdb.com/?item=" + ID_PLACEHOLDER + "&xml"),
            new Preset("evowow", "EvoWow",
                    "https://wotlk.evowow.com/?item=" + ID_PLACEHOLDER + "&xml"));

    /** The source used until the user selects another one. */
    public static final String DEFAULT_TEMPLATE = PRESETS.get(1).template();

    private ItemSourceTemplates() {
    }

    /** The preset with the given key, or null when the key is not a known source. */
    public static Preset preset(String key) {
        if (key == null) {
            return null;
        }
        for (Preset preset : PRESETS) {
            if (preset.key().equalsIgnoreCase(key.trim())) {
                return preset;
            }
        }
        return null;
    }

    /** The preset matching an exact template, or null for a custom template. */
    public static Preset presetFor(String template) {
        if (template == null) {
            return null;
        }
        String trimmed = template.trim();
        for (Preset preset : PRESETS) {
            if (preset.template().equals(trimmed)) {
                return preset;
            }
        }
        return null;
    }

    /**
     * True for an absolute http(s) URL template containing the literal {@value #ID_PLACEHOLDER}
     * placeholder and no whitespace. The URL is parsed with the placeholder replaced so malformed
     * hosts are rejected too.
     */
    public static boolean isValidTemplate(String template) {
        if (template == null) {
            return false;
        }
        String trimmed = template.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))
                || !trimmed.contains(ID_PLACEHOLDER)) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                return false;
            }
        }
        try {
            URI uri = new URI(trimmed.replace(ID_PLACEHOLDER, "0"));
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** Host of a source template, or null when it is not an absolute http(s) URL. */
    public static String hostOf(String template) {
        if (template == null) {
            return null;
        }
        try {
            String host = new URI(template.trim().replace(ID_PLACEHOLDER, "0")).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
