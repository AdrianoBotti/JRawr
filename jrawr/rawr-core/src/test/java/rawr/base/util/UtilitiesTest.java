package rawr.base.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the general-purpose helpers after the Java-native cleanup (single-pass hash combining and
 * precompiled extension patterns). Expected hash values are the C# algorithm's rotate-left-23 /
 * XOR fold, hardcoded so a refactor cannot silently change them.
 */
class UtilitiesTest {

    @Test
    void hashCombinationMatchesTheOriginalAlgorithm() {
        assertThat(Utilities.combineHashCodes()).isZero();
        assertThat(Utilities.combineHashCodes(1, 2, 3)).isEqualTo(16793603);
        assertThat(Utilities.combineHashCodes(-1)).isEqualTo(-1);
        assertThat(Utilities.combineHashCodes(0x12345678)).isEqualTo(0x12345678);

        assertThat(Utilities.getCombinedHashCode((Object) null, "a")).isEqualTo(97);
        assertThat(Utilities.getArrayHashCode(new String[] {null, "a"})).isEqualTo(97);
        assertThat(Utilities.getArrayHashCode(null)).isZero();
    }

    @Test
    void arrayEqualityKeepsReferenceNullAndLengthShortcuts() {
        String[] a = {"x", null, "y"};
        String[] b = {"x", null, "y"};
        assertThat(Utilities.areArraysEqual(a, b)).isTrue();
        assertThat(Utilities.areArraysEqual(a, a)).isTrue();
        assertThat(Utilities.areArraysEqual(a, null)).isFalse();
        assertThat(Utilities.areArraysEqual(null, null)).isTrue();
        assertThat(Utilities.areArraysEqual(new String[] {"x"}, new String[] {"X"})).isFalse();
        assertThat(Utilities.areArraysEqual(new String[] {"x"}, new String[] {"X"},
                (x, y) -> x != null && y != null && x.equalsIgnoreCase(y))).isTrue();
    }

    @Test
    void permutationsAreLazyAndLexicographic() {
        List<int[]> permutations = new ArrayList<int[]>();
        for (Integer[] permutation : Utilities.getDifferentElementPermutations(Arrays.asList(1, 2, 3))) {
            permutations.add(Arrays.stream(permutation).mapToInt(Integer::intValue).toArray());
        }
        assertThat(permutations).hasSize(6);
        assertThat(permutations.get(0)).containsExactly(1, 2, 3);
        assertThat(permutations.get(5)).containsExactly(3, 2, 1);
        assertThat(permutations).allSatisfy(p -> assertThat(p).hasSize(3));
    }

    @Test
    void weightedSumsKeepTheOriginalZeroWeightRules() {
        assertThat(Utilities.getWeightedSum(10f, 1f, 20f, 3f)).isEqualTo(17.5f);
        assertThat(Utilities.getWeightedSum(10f, 0f, 20f, 0f)).isZero();
        assertThat(Utilities.getWeightedSum(10f, 0f, 20f, 1f)).isEqualTo(20f);
        assertThat(Utilities.getWeightedSum(Arrays.asList(10f, 20f), Arrays.asList(1f, 3f))).isEqualTo(17.5f);

        boolean[] invoked = new boolean[1];
        float value = Utilities.getWeightedSum(
                () -> 10f, 0f,
                () -> {
                    invoked[0] = true;
                    return 20f;
                }, 0f);
        assertThat(value).isZero();
        assertThat(invoked[0]).isFalse();
    }
}
