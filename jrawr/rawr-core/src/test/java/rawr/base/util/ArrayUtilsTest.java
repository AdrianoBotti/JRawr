package rawr.base.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the deliberate .NET comparison semantics that keep {@link ArrayUtils} from delegating to
 * {@code java.util.Arrays.equals} / {@code Float.compare} (TD-002/TD-003).
 */
class ArrayUtilsTest {

    @Test
    void allEqualUsesPrimitiveInequalityLikeTheOriginal() {
        assertThat(ArrayUtils.allEqual(new float[] {1f, -0f}, new float[] {1f, 0f})).isTrue();
        assertThat(ArrayUtils.allEqual(new float[] {Float.NaN}, new float[] {Float.NaN})).isFalse();
        assertThat(ArrayUtils.allEqual(new float[] {Float.NaN}, new float[] {1f})).isFalse();
        assertThat(ArrayUtils.allEqual(null, null)).isTrue();
        assertThat(ArrayUtils.allEqual(new float[] {1f}, null)).isFalse();
        assertThat(ArrayUtils.allEqual(new float[] {1f}, new float[] {1f, 2f})).isFalse();
    }

    @Test
    void allCompareTreatsNaNAsLessThanEveryValueAndEqualToNaN() {
        assertThat(ArrayUtils.allCompare(new float[] {Float.NaN}, new float[] {1f}))
                .isEqualTo(ArrayUtils.CompareResult.LessThan);
        assertThat(ArrayUtils.allCompare(new float[] {1f}, new float[] {Float.NaN}))
                .isEqualTo(ArrayUtils.CompareResult.GreaterThan);
        assertThat(ArrayUtils.allCompare(new float[] {Float.NaN}, new float[] {Float.NaN}))
                .isEqualTo(ArrayUtils.CompareResult.Equal);
        assertThat(ArrayUtils.allCompare(new float[] {1f, -1f}, new float[] {-1f, 1f}))
                .isEqualTo(ArrayUtils.CompareResult.Unequal);
    }

    @Test
    void andCombinesPartialOrderResults() {
        assertThat(ArrayUtils.and(ArrayUtils.CompareResult.Equal, ArrayUtils.CompareResult.LessThan))
                .isEqualTo(ArrayUtils.CompareResult.LessThan);
        assertThat(ArrayUtils.and(ArrayUtils.CompareResult.LessThan, ArrayUtils.CompareResult.GreaterThan))
                .isEqualTo(ArrayUtils.CompareResult.Unequal);
        assertThat(ArrayUtils.and(ArrayUtils.CompareResult.Equal, ArrayUtils.CompareResult.Equal))
                .isEqualTo(ArrayUtils.CompareResult.Equal);
    }
}
