// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Heap.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class Heap<T extends Comparable<T>> {
    private int cap;
    private T[] list;
    private int mCount;
    private boolean typeLogic;

    @SuppressWarnings("unchecked")
    public Heap(HeapType type) {
        this.cap = 16;
        this.list = (T[]) new Comparable[this.cap + 1];
        if (type == HeapType.MaximumHeap) {
            this.typeLogic = true;
        }
    }

    @SuppressWarnings("unchecked")
    public Heap(int capacity, HeapType type) {
        this.cap = capacity;
        this.list = (T[]) new Comparable[this.cap + 1];
        if (type == HeapType.MaximumHeap) {
            this.typeLogic = true;
        }
    }

    public void clear() {
        this.mCount = 0;
    }

    public T pop() {
        if (this.mCount == 0) {
            return null;
        }
        T entry = this.list[1];
        int mCount = this.mCount;
        int index = mCount;
        mCount--;
        int num3 = 1;
        while ((num3 * 2) <= mCount) {
            int num = num3 * 2;
            if (((num + 1) <= mCount) && ((this.list[num + 1].compareTo(this.list[num]) < 0) ^ this.typeLogic)) {
                num++;
            }
            if (!((this.list[index].compareTo(this.list[num]) > 0) ^ this.typeLogic)) {
                break;
            }
            this.list[num3] = this.list[num];
            num3 = num;
        }
        this.list[num3] = this.list[index];
        this.mCount--;
        return entry;
    }

    @SuppressWarnings("unchecked")
    public void push(T entry) {
        if (this.mCount == cap) {
            cap *= 2;
            T[] newList = (T[]) new Comparable[cap + 1];
            System.arraycopy(list, 0, newList, 0, list.length);
            list = newList;
        }
        this.mCount++;
        int mCount = this.mCount;
        while ((mCount > 1) && ((this.list[mCount / 2].compareTo(entry) > 0) ^ this.typeLogic)) {
            this.list[mCount] = this.list[mCount / 2];
            mCount = mCount / 2;
        }
        this.list[mCount] = entry;
    }

    public int getCount() {
        return this.mCount;
    }

    public T getHead() {
        if (this.mCount == 0) {
            return null;
        }
        return this.list[1];
    }

    public T get(int index) {
        return this.list[index];
    }
}
