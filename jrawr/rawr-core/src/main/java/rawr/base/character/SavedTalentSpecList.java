// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SavedTalentSpec.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import java.util.ArrayList;

/**
 * Insertion-ordered list of {@link SavedTalentSpec} entries: the collection type the C#
 * {@code XmlSerializer} expected for the saved-specs registry.
 *
 * <p>It is a thin {@link java.util.ArrayList} subclass so the public shape matches the original
 * API; ordering is the order in which specs were added (or loaded), which the UI presented as-is.
 * The process-wide instance is installed by {@link SavedTalentSpec#load(SavedTalentSpecList)}
 * from the rawr-data loader.</p>
 */
public class SavedTalentSpecList extends ArrayList<SavedTalentSpec> {
    public SavedTalentSpecList() {
        super();
    }

    public SavedTalentSpecList(int capacity) {
        super(capacity);
    }
}
