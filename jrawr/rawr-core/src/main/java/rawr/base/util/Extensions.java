// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.util;

import rawr.base.stats.Stats;

import java.util.regex.Pattern;

/**
 * Stat-name display helpers: conversion between the programmatic name of a stat and the label the
 * UI shows.
 *
 * <p>Every stat in {@link Stats} carries a {@link Stats.StatProperty} with a programmatic name such
 * as {@code HitRating} and an optional display name. The methods here map a property to a printable
 * label and back. The C# original implemented these as extension methods on {@code PropertyInfo};
 * Java statics keep the same calls with an explicit argument.</p>
 *
 * <ul>
 *   <li>{@link #spaceCamel(String)} inserts a space before every capital letter
 *       ({@code HitRating} becomes {@code Hit Rating}) with the same single regex replacement as the
 *       C# original, then trims.</li>
 *   <li>{@link #unSpaceCamel(String)} removes every space; it does not restore capitalization, so it
 *       is a formatting counterpart, not a parser.</li>
 *   <li>{@link #displayName(Stats.StatProperty)} prefers the stored display name, otherwise derives
 *       one with {@code spaceCamel}, and prefixes a space unless the result starts with a percent
 *       sign (percentage stats are not indented like the others).</li>
 *   <li>{@link #unDisplayName(String)} scans {@link Stats#PROPERTY_INFO} and returns the first
 *       property whose trimmed display name equals the trimmed argument (ordinal string comparison;
 *       C# used {@code ==}, which is culture-independent for these ASCII names), or {@code null}
 *       when nothing matches.</li>
 * </ul>
 *
 * <p>These helpers affect labels and lookup only, never numerical results.</p>
 */
public final class Extensions {
    private static final Pattern CAPITALS = Pattern.compile("([A-Z])");
    private static final Pattern SPACES = Pattern.compile("( )");

    private Extensions() {
    }

    public static Stats.StatProperty unDisplayName(String displayName) {
        for (Stats.StatProperty info : Stats.PROPERTY_INFO) {
            if (displayName(info).trim().equals(displayName.trim()))
                return info;
        }
        return null;
    }

    public static String displayName(Stats.StatProperty info) {
        String prettyName;

        if ( info.displayName() != null) {
            prettyName = info.displayName();
        } else {
            prettyName = spaceCamel(info.name());
        }
        if (!prettyName.startsWith("%"))
            prettyName = " " + prettyName;
        return prettyName;
    }

    public static String spaceCamel(String name) {
        return CAPITALS.matcher(name).replaceAll(" $1").trim();
    }

    public static String unSpaceCamel(String name) {
        return SPACES.matcher(name).replaceAll("").trim();
    }
}
