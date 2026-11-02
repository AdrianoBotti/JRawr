package rawr.web;

import java.util.List;

/** View model for the htmx item tooltip fragment. */
public record ItemTooltip(
        String name,
        String qualityKey,
        int itemLevel,
        String slotText,
        String setName,
        List<StatLine> stats,
        List<EffectLine> effects,
        String enchantName,
        List<String> enchantStats,
        List<GemLine> gems,
        List<SocketLine> sockets,
        String socketBonusText,
        Boolean socketBonusActive,
        List<SetBonusLine> setBonuses,
        String setSummary,
        boolean setSimulated,
        int itemId,
        String sourceKind,
        String source,
        String iconUrl,
        String initials) {

    public boolean hasSource() {
        return source != null && !source.isBlank();
    }

    public boolean hasSetBonuses() {
        return setBonuses != null && !setBonuses.isEmpty();
    }

    public boolean hasSetSummary() {
        return setSummary != null && !setSummary.isBlank();
    }

    /**
     * One displayed stat. {@code text()} mirrors the legacy ItemToolTip drawing: value followed by
     * the display name (no extra space when the name already starts with {@code %}).
     */
    public record StatLine(String value, String label) {
        public String text() {
            return label.startsWith("%") ? value + label : value + " " + label;
        }
    }

    /** One proc/on-use effect: a use/equip label and the engine's effect description. */
    public record EffectLine(String label, String text) {
        public String line() {
            return label + ": " + text;
        }
    }

    public record SocketLine(String colorKey, String label) {
    }

    /** One socketed gem: its name, its stats, and its socket colour key (red/blue/...). */
    public record GemLine(String name, String statsText, String colorKey) {
    }

    /**
     * One line of the item's set bonus block. {@code active} is null when no character context is
     * known, otherwise true when the set threshold is met by the equipped (or simulated) piece
     * count. {@code description} comes from the bonus buff's stats, rendered with the same
     * StatLine helper as the item stats.
     */
    public record SetBonusLine(int threshold, String description, Boolean active) {
        public String text() {
            return "(" + threshold + ") Set: " + (description == null ? "" : description);
        }

        public String stateClass() {
            return active == null ? "" : (active ? "is-active" : "is-inactive");
        }
    }
}
