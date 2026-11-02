package rawr.web;

import java.util.List;

/**
 * Colour and typography tokens exposed by the Appearance modal. The values mirror the defaults in
 * {@code tokens.css}; the browser reads the current values from the live stylesheet so user overrides
 * and presets are always shown correctly. Keys are CSS custom properties on {@code :root}.
 */
public final class AppearancePalette {
    public record Token(String key, String label, String value, String kind) {
    }

    public record Group(String id, String title, String description, boolean advanced, List<Token> tokens) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("surfaces", "Surfaces", "Application, panel, card and control backgrounds.", false,
                    List.of(
                            color("--bg", "Application background", "#0f1218"),
                            color("--bg-top", "Background gradient top", "#161b23"),
                            color("--bg-elevated", "Elevated background", "#191f28"),
                            color("--bg-deep", "Deep inset", "#0b0e13"),
                            color("--bg-deepest", "Deepest inset", "#07090d"),
                            color("--bg-inset", "Inset surface", "#11151b"),
                            color("--panel", "Input panel", "#151a21"),
                            color("--panel-alt", "Alternate panel", "#1a2029"),
                            color("--panel-raised", "Raised input", "#202732"),
                            color("--card", "Card", "#191f27"),
                            color("--card-head", "Card header", "#212933"),
                            color("--card-highlight", "Card highlight", "#28313c"),
                            color("--surface-tint", "Tinted surface", "#1e242d"),
                            color("--button", "Button", "#232a34"),
                            color("--button-hover", "Button hover", "#2d3743"))),
            new Group("text", "Text", "Body, heading, muted and inverse text colours.", false,
                    List.of(
                            color("--text", "Primary text", "#e7ecf3"),
                            color("--text-muted", "Muted text", "#b2bcc9"),
                            color("--text-dim", "Dim text", "#8892a1"),
                            color("--text-faint", "Faint text", "#66707d"),
                            color("--on-accent", "Text on accent", "#221a07"),
                            color("--on-solid", "Text on solid colour", "#ffffff"))),
            new Group("borders", "Borders", "Dividers, input outlines and emphasis borders.", false,
                    List.of(
                            color("--border", "Border", "#2c3541"),
                            color("--border-soft", "Soft border", "#222a33"),
                            color("--border-strong", "Strong border", "#9a833f"),
                            color("--border-hover", "Hover border", "#3a4552"),
                            color("--border-deep", "Deep border", "#1a2129"))),
            new Group("accents", "Accents and status", "Links, highlights, warnings and validation colours.",
                    false,
                    List.of(
                            color("--accent", "Primary accent", "#e3b341"),
                            color("--accent-strong", "Strong accent", "#c29026"),
                            color("--accent-2", "Secondary accent", "#54b3a6"),
                            color("--info", "Info", "#7ba3ff"),
                            color("--warning", "Warning", "#e0b34a"),
                            color("--danger", "Danger", "#e0645b"),
                            color("--success", "Success", "#57bb7a"),
                            color("--highlight", "Highlight", "#ffd100"),
                            color("--highlight-strong", "Strong highlight", "#d9a400"))),
            new Group("fonts", "Typography", "Font stacks for body text, display headings and code.", false,
                    List.of(
                            font("--font-body", "Body font",
                                    "\"Inter\", \"Segoe UI\", system-ui, -apple-system, sans-serif"),
                            font("--font-display", "Display font",
                                    "\"Palatino Linotype\", \"Book Antiqua\", Georgia, serif"),
                            font("--font-mono", "Monospace font",
                                    "ui-monospace, \"Cascadia Code\", Consolas, monospace"))),
            new Group("quality", "Item quality",
                    "Quality colours for item names, tooltips and icon fallbacks.", true,
                    List.of(
                            color("--q-poor", "Poor", "#9d9d9d"),
                            color("--q-common", "Common", "#ffffff"),
                            color("--q-uncommon", "Uncommon", "#1eff00"),
                            color("--q-rare", "Rare", "#0070dd"),
                            color("--q-epic", "Epic", "#a335ee"),
                            color("--q-legendary", "Legendary", "#ff8000"),
                            color("--q-artifact", "Artifact", "#e6cc80"),
                            color("--q-heirloom", "Heirloom", "#00ccff"),
                            color("--q-temp", "Temporary", "#7f7f7f"))),
            new Group("classes", "Classes",
                    "Class identity colours used by the character avatar and class labels.", true,
                    List.of(
                            color("--class-default", "Default class", "#8a9bb3"),
                            color("--class-warrior", "Warrior", "#c79c6e"),
                            color("--class-paladin", "Paladin", "#f58cba"),
                            color("--class-hunter", "Hunter", "#abd473"),
                            color("--class-rogue", "Rogue", "#fff569"),
                            color("--class-priest", "Priest", "#f2f2f2"),
                            color("--class-deathknight", "Death Knight", "#c41f3b"),
                            color("--class-shaman", "Shaman", "#0070de"),
                            color("--class-mage", "Mage", "#69ccf0"),
                            color("--class-warlock", "Warlock", "#9482c9"),
                            color("--class-druid", "Druid", "#ff7d0a"))),
            new Group("gems", "Gems and sockets",
                    "Socket pips, gem dots and the prismatic rainbow.", true,
                    List.of(
                            color("--gem-red", "Red", "#e03a3a"),
                            color("--gem-blue", "Blue", "#3a78e0"),
                            color("--gem-yellow", "Yellow", "#e0c53a"),
                            color("--gem-green", "Green", "#39d353"),
                            color("--gem-orange", "Orange", "#e08a3a"),
                            color("--gem-purple", "Purple", "#a335ee"),
                            color("--gem-meta", "Meta", "#d8d8d8"),
                            color("--gem-prismatic", "Prismatic", "#f0f0fa"),
                            color("--gem-unknown", "Unknown gem", "#6b7688"))),
            new Group("specs", "Specializations",
                    "Talent-tree accent colours for every specialization.", true,
                    List.of(
                            color("--spec-arcane", "Arcane", "#b18cff"),
                            color("--spec-fire", "Fire", "#ff7a3d"),
                            color("--spec-frost", "Frost", "#6fd3ff"),
                            color("--spec-blood", "Blood", "#d64545"),
                            color("--spec-unholy", "Unholy", "#7ecb5a"),
                            color("--spec-balance", "Balance", "#b18cff"),
                            color("--spec-feral-combat", "Feral / Combat", "#e0a43a"),
                            color("--spec-restoration", "Restoration", "#55c98a"),
                            color("--spec-beast-mastery", "Beast Mastery", "#9acd5a"),
                            color("--spec-marksmanship", "Marksmanship", "#7fb0ff"),
                            color("--spec-survival", "Survival", "#d6b25a"),
                            color("--spec-holy", "Holy", "#f0d58a"),
                            color("--spec-protection", "Protection", "#7fa7c9"),
                            color("--spec-retribution", "Retribution", "#e06a7a"),
                            color("--spec-discipline", "Discipline", "#e8ecf5"),
                            color("--spec-shadow", "Shadow", "#a98bdf"),
                            color("--spec-assassination", "Assassination", "#c76a6a"),
                            color("--spec-combat", "Combat", "#d6c25a"),
                            color("--spec-subtlety", "Subtlety", "#9a8ad6"),
                            color("--spec-elemental", "Elemental", "#62a8ff"),
                            color("--spec-enhancement", "Enhancement", "#d6a75a"),
                            color("--spec-affliction", "Affliction", "#a98bdf"),
                            color("--spec-demonology", "Demonology", "#9b7ad6"),
                            color("--spec-destruction", "Destruction", "#ff7a3d"),
                            color("--spec-arms", "Arms", "#d66a5a"),
                            color("--spec-fury", "Fury", "#e08a4a"))));

    private AppearancePalette() {
    }

    public static List<Group> groups() {
        return GROUPS;
    }

    private static Token color(String key, String label, String value) {
        return new Token(key, label, value, "color");
    }

    private static Token font(String key, String label, String value) {
        return new Token(key, label, value, "font");
    }
}
