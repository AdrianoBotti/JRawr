// Java-only helper extracted from the identical private makeStats methods in Buff and Enchant.
package rawr.base.stats;

/**
 * Builds a {@link Stats} snapshot from alternating stat names and numeric values, the compact form
 * used by the hardcoded buff and enchant catalogues ({@code makeStats("SpellPower", 144f, ...)}).
 *
 * <p>The name is looked up in {@link Stats#PROPERTY_BY_NAME}; a missing name fails with the same
 * {@link NullPointerException} the original code produced. Values are converted through
 * {@link Number#floatValue()}, so the integer literals in the catalogues become the same floats as
 * before. The class has no state and returns a fresh snapshot on every call.</p>
 */
public final class StatFactory {
    private StatFactory() {
    }

    public static Stats makeStats(Object... namesAndValues) {
        Stats stats = new Stats();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Stats.PROPERTY_BY_NAME.get((String) namesAndValues[i])
                    .mutator().set(stats, ((Number) namesAndValues[i + 1]).floatValue());
        }
        return stats;
    }
}
