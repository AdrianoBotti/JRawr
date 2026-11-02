// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ArrayPool.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public final class ArrayOffset<T> {
    private final T[] array;
    private final int offset;

    public ArrayOffset(T[] array, int offset) {
        this.array = array;
        this.offset = offset;
    }

    public T get(int index) {
        return array[offset + index];
    }

    public void set(int index, T value) {
        array[offset + index] = value;
    }
}
