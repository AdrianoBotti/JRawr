// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SavedBuffSet.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Load/Save(TextReader/TextWriter) are replaced by load(SavedBuffSetList) because XML
// parsing lives in rawr-data loaders (see Buff.load).
package rawr.base.character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named, saved selection of active buffs (a "buff set") that a user can reapply as a whole.
 *
 * <p>The selection is stored twice: as the {@link #buffSet} list and as its string form
 * {@link #setAsString}, both with "Set Bonuses" and "Profession Buffs" entries filtered out.
 * Those two groups are derived automatically from equipped gear and professions
 * ({@link Character#recalculateSetBonuses()}, {@link Character#validateActiveBuffs()}) and must
 * not be part of a saved selection, or re-applying the set would fight the automatic logic. The
 * registry of all saved sets is process-wide static state installed by
 * {@link #load(SavedBuffSetList)}; unlike {@link SavedTalentSpec#getAllSpecs()},
 * {@link #getAllSets()} lazily substitutes an empty list when the loader has not run.</p>
 */
public class SavedBuffSet {
    public String name;
    public String setAsString;
    public List<Buff> buffSet;

    public SavedBuffSet() {
        this("", null);
    }

    private static SavedBuffSetList allSets;

    public static SavedBuffSetList getAllSets() {
        if (allSets == null)
            allSets = new SavedBuffSetList();
        return allSets;
    }

    /** Installs the process-wide registry of saved buff sets, replacing any previous one (called by the rawr-data loader). */
    public static void load(SavedBuffSetList sets) {
        allSets = sets;
    }

    public SavedBuffSet(String name, List<Buff> buffSet) {
        this.name = name;
        if (buffSet != null) {
            List<Buff> toSave = new ArrayList<Buff>();
            for (Buff b : buffSet) {
                if (b == null || b.group == null)
                    continue;
                if (b.group.equals("Set Bonuses") || b.group.equals("Profession Buffs")) {
                } else {
                    toSave.add(b);
                }
            }
            setAsString = toSave.toString();
            this.buffSet = toSave;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * True when the argument holds exactly the same buff names as this set, ignoring order and
     * ignoring "Set Bonuses"/"Profession Buffs" entries on both sides. Returns false when the
     * argument is null or this set was never built.
     */
    public boolean equals(List<Buff> otherBuffSet) {
        if (otherBuffSet == null || setAsString == null) {
            return false;
        }
        List<Buff> clonedotherBuffSet = new ArrayList<Buff>();
        for (Buff b : otherBuffSet) {
            if (b == null || b.group == null)
                continue;
            if (b.group.equals("Set Bonuses") || b.group.equals("Profession Buffs")) {
            } else {
                clonedotherBuffSet.add(b);
            }
        }
        if (buffSet.size() != clonedotherBuffSet.size()) {
            return false;
        }
        List<String> thisSetAsStrings = new ArrayList<String>();
        for (Buff b : buffSet) {
            thisSetAsStrings.add(b.name);
        }
        Collections.sort(thisSetAsStrings);
        List<String> otherSetAsStrings = new ArrayList<String>();
        for (Buff b : clonedotherBuffSet) {
            otherSetAsStrings.add(b.name);
        }
        Collections.sort(otherSetAsStrings);
        boolean noMatch = false;
        for (String t : thisSetAsStrings) {
            if (!otherSetAsStrings.contains(t)) {
                noMatch = true;
                break;
            }
        }
        if (noMatch)
            return false;
        for (String o : otherSetAsStrings) {
            if (!thisSetAsStrings.contains(o)) {
                noMatch = true;
                break;
            }
        }
        if (noMatch)
            return false;

        return true;
    }
}
