// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/ProcEffect.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

// Combination generator public class
// Helper public class to generate all possible combinations of proc effects to mathematically account for interactions.
public class CombinationGenerator {
    int[] a;
    int n;
    int r;
    long numLeft;
    long total;

    public CombinationGenerator(int n, int r) {
        if (r > n) throw new IllegalArgumentException("r must be less than or equal to n");
        if (n < 1) throw new IllegalArgumentException("n must be greater than or equal to 1");

        this.n = n;
        this.r = r;
        a = new int[r];
        long nFact = getFactorial(n);
        long rFact = getFactorial(r);
        long nMinusRFact = getFactorial(n - r);
        total = nFact / (rFact * nMinusRFact);
        reset();
    }

    public boolean hasNext() {
        return numLeft > 0;
    }

    public int[] getNext() {
        if (numLeft == total) {
            --numLeft;
            return a;
        }

        int i = r - 1;
        while (a[i] == n - r + i) {
            --i;
        }
        a[i]++;
        for (int j = i + 1; j < r; ++j) {
            a[j] = a[i] + j - i;
        }
        --numLeft;
        return a;
    }

    private void reset() {
        for (int i = 0; i < a.length; ++i) {
            a[i] = i;
        }
        numLeft = total;
    }

    private long getFactorial(int p) {
        long fact = 1;
        for (int i = p; i > 1; --i) {
            fact *= i;
        }
        return fact;
    }
}
