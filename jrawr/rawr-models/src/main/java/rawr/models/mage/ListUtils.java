// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ListUtils.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ListUtils {
    private ListUtils() {
    }

    public static <T> List<T> intersect(List<T> left, List<T> right) {
        List<T> intersect = new ArrayList<T>();
        for (T item : left)
            if (right.contains(item))
                intersect.add(item);
        return intersect;
    }

    public static <T> List<T> removeDuplicates(List<T> inputList) {
        LinkedHashSet<T> uniqueStore = new LinkedHashSet<T>();
        List<T> finalList = new ArrayList<T>();
        for (T currValue : inputList) {
            if (uniqueStore.add(currValue)) {
                finalList.add(currValue);
            }
        }
        return finalList;
    }
}
