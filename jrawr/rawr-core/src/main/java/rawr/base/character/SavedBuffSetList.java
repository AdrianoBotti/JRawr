// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SavedBuffSet.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import java.util.ArrayList;

/**
 * Insertion-ordered list of {@link SavedBuffSet} entries: the collection type the C#
 * {@code XmlSerializer} expected for the saved buff-set registry.
 *
 * <p>A thin {@link java.util.ArrayList} subclass kept for API parity with the original. The
 * process-wide instance is installed by {@link SavedBuffSet#load(SavedBuffSetList)} from the
 * rawr-data loader; {@link SavedBuffSet#getAllSets()} lazily substitutes an empty list when the
 * loader has not run.</p>
 */
public class SavedBuffSetList extends ArrayList<SavedBuffSet> {
    public SavedBuffSetList() {
        super();
    }

    public SavedBuffSetList(int capacity) {
        super(capacity);
    }
}
