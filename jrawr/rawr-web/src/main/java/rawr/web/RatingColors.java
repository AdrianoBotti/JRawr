package rawr.web;

import java.util.List;
import java.util.Locale;

/**
 * Curated sub-rating colours for the warm header panel. Model-provided colours came from the
 * Windows chart palette and include dark/ARGB values that were hard to read (or transparent) on the
 * web background, so the web UI uses this jewel-tone palette instead. Every colour has a WCAG
 * contrast ratio of at least 4.5:1 against the rating pill (see {@code RatingPanelTest}).
 */
final class RatingColors {
    static final String DPS = "#e7b85a";
    static final String BURST = "#f08a4b";
    static final String SURVIVAL = "#8bc98c";
    static final String HEALING = "#e8a3b5";
    static final String MANA = "#b5a3e8";
    static final String MITIGATION = "#74c9c2";
    static final String THREAT = "#d4834f";

    static final List<String> PALETTE = List.of(DPS, BURST, SURVIVAL, HEALING, MANA, MITIGATION, THREAT);

    private RatingColors() {
    }

    /** Semantic colour for a sub-rating label, falling back to the palette by index. */
    static String forLabel(String label, int index) {
        String key = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (key.contains("surviv")) return SURVIVAL;
        if (key.contains("mitigat")) return MITIGATION;
        if (key.contains("threat")) return THREAT;
        if (key.contains("burst")) return BURST;
        if (key.contains("pet")) return BURST;
        if (key.contains("mana") || key.contains("mp5") || key.contains("mps") || key.contains("hp5")) {
            return MANA;
        }
        if (key.contains("heal") || key.contains("hps")) return HEALING;
        if (key.contains("dps") || key.contains("damage")) return DPS;
        if (key.contains("raid")) return THREAT;
        if (key.contains("reaction")) return HEALING;
        if (key.contains("sustained") || key.contains("single")) return DPS;
        return PALETTE.get(index % PALETTE.size());
    }
}
