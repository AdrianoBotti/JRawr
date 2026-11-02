// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ArrayPool.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Java specialization of C# ArrayOffset<double>: Java generics cannot take primitive type
// arguments, so the double instantiation used by ArraySet.pD / LP.pD is its own class.
package rawr.models.mage;

public final class ArrayOffsetDouble {
    private final double[] array;
    private final int offset;

    public ArrayOffsetDouble(double[] array, int offset) {
        this.array = array;
        this.offset = offset;
    }

    public double get(int index) {
        return array[offset + index];
    }

    public void set(int index, double value) {
        array[offset + index] = value;
    }
}
