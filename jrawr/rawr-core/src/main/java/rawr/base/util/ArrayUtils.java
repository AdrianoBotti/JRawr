// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ArrayUtils.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.util;

/**
 * Element-wise comparison helpers for {@code float[]} snapshots, with the partial-order semantics the
 * stat engine needs.
 *
 * <p>Comparing two stat arrays is not a total order: one can be higher in strength and lower in
 * agility at the same time. {@link CompareResult} captures that: the arrays are {@code Equal} (every
 * element equal), {@code LessThan}/{@code GreaterThan} (every differing element points the same way)
 * or {@code Unequal} (both directions occur). {@link #and(CompareResult, CompareResult)} combines two
 * such results component-wise, treating {@code Equal} as the identity.</p>
 *
 * <p>{@link #allCompare(float[], float[], int)} performs a comparison while restricting which
 * directions count as success: the allowed directions form a bit set built from
 * {@link CompareOption}. Its boolean result answers "does the relation hold under these
 * permissions", while the classification is written into a caller-provided single-element array
 * because Java has no {@code out} parameters. The two simpler overloads compare with all directions
 * allowed, and {@link #allEqual(float[], float[])} is the exact-equality case.</p>
 *
 * <p>Element comparison follows .NET {@code Single.CompareTo}: NaN compares less than every non-NaN
 * value, while two NaNs compare equal (Java's {@code Float.compare} would order NaN last instead,
 * hence the private helper). This is part of the observable behavior; do not replace it with a
 * library comparator.</p>
 */
public final class ArrayUtils {
    private ArrayUtils() {
    }

    /**
     * The classification of an element-wise array comparison: all elements equal, all differing
     * elements pointing one way, or conflicting.
     */
    public enum CompareResult {LessThan, Equal, Unequal, GreaterThan}

    /**
     * Combines two comparison results into one: {@code Equal} is the identity, equal results stay
     * unchanged, and {@code LessThan} combined with {@code GreaterThan} (in either order) yields
     * {@code Unequal}.
     */
    public static CompareResult and(CompareResult a, CompareResult b) {
        if (a == b) {
            return a;
        }
        if (a == CompareResult.Equal) {
            return b;
        }
        if (b == CompareResult.Equal) {
            return a;
        }
        return CompareResult.Unequal;
    }

    public static final class CompareOption {
        public static final int LESS_THAN = 1;
        public static final int EQUAL = 2;
        public static final int GREATER_THAN = 4;

        private CompareOption() {
        }
    }

    /**
     * Exact element-wise equality with reference and length shortcuts; uses primitive {@code !=}, so
     * NaN is never equal to anything and negative zero equals positive zero.
     */
    public static boolean allEqual(float[] lhs, float[] rhs) {
        if (lhs == rhs) return true;
        if (lhs == null || rhs == null) return false;
        if (lhs.length != rhs.length) return false;

        for (int i = 0; i < lhs.length; i++) {
            if (lhs[i] != rhs[i]) return false;
        }
        return true;
    }

    /**
     * Same as {@link #allCompare(float[], float[], int, CompareResult[])}, discarding the computed
     * classification.
     */
    public static boolean allCompare(float[] lhs, float[] rhs, int comparison) {
        return allCompare(lhs, rhs, comparison, new CompareResult[1]);
    }

    /**
     * Compares with every direction allowed and returns the classification; {@code Unequal} means
     * some elements are greater and some smaller.
     */
    public static CompareResult allCompare(float[] lhs, float[] rhs) {
        CompareResult[] compareResult = new CompareResult[1];
        allCompare(lhs, rhs, CompareOption.EQUAL | CompareOption.LESS_THAN | CompareOption.GREATER_THAN, compareResult);
        return compareResult[0];
    }

    /**
     * Compares two arrays element-wise under the allowed directions in {@code comparison} (a mask of
     * {@link CompareOption} values) and writes the overall relation into {@code compareResult[0]}
     * ({@code Equal} by default). Returns false as soon as an element pair points in a disallowed
     * direction; a null or length-mismatched pair returns false with the result left as
     * {@code Equal}.
     */
    public static boolean allCompare(float[] lhs, float[] rhs, int comparison, CompareResult[] compareResult) {
        compareResult[0] = CompareResult.Equal;
        if (lhs == null || rhs == null) return false;
        if (lhs.length != rhs.length) return false;

        boolean allowLessThan = (comparison & CompareOption.LESS_THAN) == CompareOption.LESS_THAN;
        boolean allowEqual = (comparison & CompareOption.EQUAL) == CompareOption.EQUAL;
        boolean allowGreaterThan = (comparison & CompareOption.GREATER_THAN) == CompareOption.GREATER_THAN;

        if (allowEqual && lhs == rhs) return true;

        boolean haveGreaterThan = false;
        boolean haveLessThan = false;
        for (int i = 0; i < lhs.length; i++) {
            int val = compareTo(lhs[i], rhs[i]);
            if (val < 0) {
                haveLessThan = true;
                if (!allowLessThan) return false;
                if (haveGreaterThan) {
                    compareResult[0] = CompareResult.Unequal;
                    return allowGreaterThan || allowLessThan || allowEqual;
                }
            } else if (val > 0) {
                haveGreaterThan = true;
                if (!allowGreaterThan) return false;
                if (haveLessThan) {
                    compareResult[0] = CompareResult.Unequal;
                    return allowGreaterThan || allowLessThan || allowEqual;
                }
            }
        }
        if (haveGreaterThan && haveLessThan) compareResult[0] = CompareResult.Unequal;
        else if (haveGreaterThan) compareResult[0] = CompareResult.GreaterThan;
        else if (haveLessThan) compareResult[0] = CompareResult.LessThan;
        else compareResult[0] = CompareResult.Equal;

        return (allowGreaterThan && haveGreaterThan) ||
                (allowLessThan && haveLessThan) || allowEqual;
    }

    // .NET Single.CompareTo: NaN compares less than every value (Java Float.compare puts NaN last).
    private static int compareTo(float x, float y) {
        if (x < y) return -1;
        if (x > y) return 1;
        if (x == y) return 0;
        if (Float.isNaN(x)) return Float.isNaN(y) ? 0 : -1;
        return 1;
    }
}
