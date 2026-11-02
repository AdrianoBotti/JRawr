// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Utilities.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: clone (XmlSerializer deep clone, used only by the WinForms editor forms) and the
// IEqualityComparer overload of areArraysEqual (no Java equivalent) are omitted. The uint rotate
// overloads are folded into the int versions, which operate on the same 32-bit pattern.
package rawr.base.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Assorted general-purpose helpers ported from the C# {@code Rawr.Base} utility class: hash-code
 * combination, array equality, lazy permutation enumeration and weighted averages. None of them
 * knows about World of Warcraft.
 *
 * <p>The C# delegate types are modelled as Java functional interfaces: {@link EqualityComparison}
 * for a custom element equality and {@link FloatValueSource} for a lazily evaluated numeric
 * operand.</p>
 *
 * <h2>Hash codes and equality</h2>
 *
 * <p>{@link #combineHashCodes(Iterable)} folds a sequence of 32-bit hash codes together by rotating
 * the accumulator left by 23 bits and XOR-ing in the next code;
 * {@link #getCombinedHashCode(Iterable)} first maps objects to hash codes (null maps to 0).
 * {@link #getArrayHashCode(Object[])} copies and boxes the elements and combines their hash codes,
 * and a null array hashes to 0. {@link #areArraysEqual(Object[], Object[])} is an element-wise
 * equality check with the usual reference/length shortcuts, plus an optional per-element comparison
 * for types whose equality is not {@code Objects.equals}.</p>
 *
 * <h2>Permutations and weighted sums</h2>
 *
 * <p>{@link #getDifferentElementPermutations(List)} returns a lazy iterable over every ordering of
 * the input list (all elements considered distinct), used to try each ordering of a priority queue.
 * The weighted-sum family computes a weighted average and divides by the sum of the absolute weights
 * (so negative weights still contribute to the magnitude). The two-value overload returns 0 when
 * both weights are zero; the {@code FloatValueSource} overload never evaluates a value whose weight
 * is zero.</p>
 *
 * <h2>Porting notes</h2>
 *
 * <p>The C# {@code uint} rotate overloads are folded into the {@code int} versions, which operate on
 * the same 32-bit pattern; the {@code XmlSerializer} deep clone (used only by WinForms editor forms)
 * and the {@code IEqualityComparer} overload of {@code areArraysEqual} (no Java equivalent) are
 * omitted.</p>
 */
public final class Utilities {
    private Utilities() {
    }

    @FunctionalInterface
    public interface EqualityComparison<T> {
        boolean invoke(T x, T y);
    }

    @FunctionalInterface
    public interface FloatValueSource {
        float invoke();
    }

    public static int rotateLeft(int value, int rotationBitCount) {
        return Integer.rotateLeft(value, rotationBitCount);
    }

    public static int rotateRight(int value, int rotationBitCount) {
        return Integer.rotateRight(value, rotationBitCount);
    }

    /**
     * Folds hash codes into one by rotating the running value left 23 bits and XOR-ing each code in
     * order. The same algorithm and order as the C# original, so combined hash codes (and therefore
     * any hash-based behavior that depends on them) are identical.
     *
     * @throws NullPointerException if {@code hashCodes} is null
     */
    public static int combineHashCodes(Iterable<Integer> hashCodes) {
        if (hashCodes == null)
            throw new NullPointerException("hashCodes");

        int combinedHashCode = 0;
        for (int hashCode : hashCodes)
            combinedHashCode = rotateLeft(combinedHashCode, 23) ^ hashCode;

        return combinedHashCode;
    }

    public static int combineHashCodes(int... hashCodes) {
        int combinedHashCode = 0;
        for (int hashCode : hashCodes) {
            combinedHashCode = rotateLeft(combinedHashCode, 23) ^ hashCode;
        }
        return combinedHashCode;
    }

    /**
     * Maps each object to its hash code (null becomes 0) and combines them with
     * {@link #combineHashCodes(Iterable)}.
     *
     * @throws NullPointerException if {@code objects} is null
     */
    public static int getCombinedHashCode(Iterable<?> objects) {
        if (objects == null)
            throw new NullPointerException("objects");

        int combinedHashCode = 0;
        for (Object anObject : objects) {
            combinedHashCode = rotateLeft(combinedHashCode, 23) ^ (anObject == null ? 0 : anObject.hashCode());
        }
        return combinedHashCode;
    }

    public static int getCombinedHashCode(Object... objects) {
        return getCombinedHashCode(Arrays.asList(objects));
    }

    /**
     * Hash code of an array's elements, combined with {@link #getCombinedHashCode(Iterable)}; a null
     * array hashes to 0. The elements are boxed into a copy first because the combine step works on
     * objects.
     */
    public static <T> int getArrayHashCode(T[] array) {
        if (array == null)
            return 0;

        int combinedHashCode = 0;
        for (T element : array) {
            combinedHashCode = rotateLeft(combinedHashCode, 23) ^ (element == null ? 0 : element.hashCode());
        }
        return combinedHashCode;
    }

    /**
     * Element-wise equality using {@code Objects.equals} (so nulls compare equal to nulls).
     */
    public static <TElement> boolean areArraysEqual(TElement[] array1, TElement[] array2) {
        return areArraysEqual(array1, array2, (x, y) -> Objects.equals(x, y));
    }

    /**
     * Element-wise equality with a caller-supplied comparison. Two references to the same array are
     * equal; a null operand or a length mismatch returns false.
     *
     * @throws NullPointerException if {@code comparison} is null
     */
    public static <TElement> boolean areArraysEqual(
            TElement[] array1,
            TElement[] array2,
            EqualityComparison<TElement> comparison) {
        if (comparison == null)
            throw new NullPointerException("comparison");

        if (array1 == array2)
            return true;

        if ((array1 == null) || (array2 == null) || (array1.length != array2.length))
            return false;

        for (int elementIndex = 0; elementIndex < array1.length; elementIndex++)
            if (!comparison.invoke(array1[elementIndex], array2[elementIndex]))
                return false;

        return true;
    }

    /**
     * Lazy enumeration of all permutations of the input list, in lexicographic order of the index
     * sequences, starting with the input order itself. Each {@code next()} materializes a fresh array
     * whose runtime component type is the most specific common superclass of the non-null elements
     * (or {@code Object}); duplicates are treated as distinct elements, as the C# original documents.
     * The input list is read on demand and must not change during iteration.
     *
     * @throws NullPointerException if {@code elements} is null
     */
    public static <TElement> Iterable<TElement[]> getDifferentElementPermutations(List<TElement> elements) {
        if (elements == null)
            throw new NullPointerException("elements");

        return () -> new Iterator<TElement[]>() {
            private final int[] elementIndices = getInitialElementIndices(elements.size());
            private final Class<?> elementType = getCommonElementType(elements);
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public TElement[] next() {
                if (!hasNext)
                    throw new NoSuchElementException();

                TElement[] result = getElementsByIndices(elements, elementIndices, elementType);
                hasNext = tryMakeNextElementIndexPermutation(elementIndices);
                return result;
            }
        };
    }

    /**
     * Weighted average of two values: {@code (x*xWeight + y*yWeight) / (|xWeight| + |yWeight|)}.
     * When both weights are zero it returns 0 instead of dividing by zero.
     */
    public static float getWeightedSum(float x, float xWeight, float y, float yWeight) {
        if ((xWeight == 0) && (yWeight == 0))
            return 0;

        return (x * xWeight + y * yWeight) / (Math.abs(xWeight) + Math.abs(yWeight));
    }

    /**
     * Weighted average of any number of values, passed as {@code (value, weight)} pairs. Unlike the
     * two-value overload there is no zero-denominator guard, so an all-zero weight list yields
     * {@code NaN} exactly as in the original.
     */
    public static float getWeightedSum(float... arguments) {
        float numerator = 0f;
        float denominator = 0f;
        for (int i = 0; i < arguments.length; i += 2) {
            numerator += arguments[i] * arguments[i + 1];
            denominator += Math.abs(arguments[i + 1]);
        }
        return numerator / denominator;
    }

    /**
     * Weighted average of two parallel lists; no zero-denominator guard, and it indexes
     * {@code weights} by the size of {@code values}.
     */
    public static float getWeightedSum(List<Float> values, List<Float> weights) {
        float numerator = 0f;
        float denominator = 0f;
        for (int i = 0; i < values.size(); ++i) {
            numerator += values.get(i) * weights.get(i);
            denominator += Math.abs(weights.get(i));
        }
        return numerator / denominator;
    }

    /**
     * Lazy variant that is handed value suppliers instead of values: a supplier is only invoked when
     * its weight is non-zero, and with exactly one non-zero weight the other value is returned
     * unchanged (no scaling). Both weights zero returns 0.
     */
    public static float getWeightedSum(
            FloatValueSource x,
            float xWeight,
            FloatValueSource y,
            float yWeight) {
        if (xWeight == 0) {
            if (yWeight == 0)
                return 0;

            return y.invoke();
        }

        if (yWeight == 0)
            return x.invoke();

        return getWeightedSum(x.invoke(), xWeight, y.invoke(), yWeight);
    }

    /**
     * Swaps two list positions in place.
     */
    public static <T> void swapElements(List<T> list, int index1, int index2) {
        T tmp = list.get(index1);
        list.set(index1, list.get(index2));
        list.set(index2, tmp);
    }

    private static int[] getInitialElementIndices(int count) {
        int[] elementIndices = new int[count];
        for (int elementIndex = 0; elementIndex < count; elementIndex++)
            elementIndices[elementIndex] = elementIndex;
        return elementIndices;
    }

    private static Class<?> getCommonElementType(List<?> elements) {
        Class<?> commonType = null;
        for (Object element : elements) {
            if (element == null)
                continue;

            Class<?> elementType = element.getClass();
            if (commonType == null) {
                commonType = elementType;
            } else if (!commonType.isAssignableFrom(elementType)) {
                do {
                    commonType = commonType.getSuperclass();
                } while ((commonType != null) && !commonType.isAssignableFrom(elementType));

                if (commonType == null)
                    commonType = Object.class;
            }
        }
        return commonType == null ? Object.class : commonType;
    }

    @SuppressWarnings("unchecked")
    private static <TElement> TElement[] getElementsByIndices(
            List<TElement> elements,
            int[] elementIndices,
            Class<?> elementType) {
        TElement[] result = (TElement[]) Array.newInstance(elementType, elements.size());

        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++)
            result[elementIndex] = elements.get(elementIndices[elementIndex]);

        return result;
    }

    private static boolean tryMakeNextElementIndexPermutation(int[] elementIndices) {
        if (elementIndices.length < 2)
            return false;

        int lastNotReversedIndexIndex = elementIndices.length - 2;
        while (elementIndices[lastNotReversedIndexIndex] > elementIndices[lastNotReversedIndexIndex + 1]) {
            lastNotReversedIndexIndex--;

            if (lastNotReversedIndexIndex < 0)
                return false;
        }

        int lastGreaterIndexIndex = elementIndices.length - 1;
        while (elementIndices[lastGreaterIndexIndex] < elementIndices[lastNotReversedIndexIndex])
            lastGreaterIndexIndex--;

        swapElements(elementIndices, lastNotReversedIndexIndex, lastGreaterIndexIndex);

        for (int i = lastNotReversedIndexIndex + 1, j = elementIndices.length - 1; i < j; i++, j--) {
            swapElements(elementIndices, i, j);
        }

        return true;
    }

    private static void swapElements(int[] elements, int index1, int index2) {
        int element1 = elements[index1];
        elements[index1] = elements[index2];
        elements[index2] = element1;
    }
}
