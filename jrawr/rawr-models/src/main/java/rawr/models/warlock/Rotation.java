// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/CalculationOptionsWarlock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port).
package rawr.models.warlock;

import rawr.base.talents.WarlockTalents;

import java.util.ArrayList;
import java.util.List;

public class Rotation {

    public String name = "Dummy";
    public String filler = "Shadow Bolt";
    public String execute = "";
    public List<String> spellPriority = new ArrayList<String>();

    public Rotation() {
        // here for XML deserialization
    }

    public Rotation(
            String name,
            String filler,
            String execute,
            String... priorities) {

        this.name = name;
        this.filler = filler;
        this.execute = execute;
        spellPriority = new ArrayList<String>();
        for (String priority : priorities) {
            spellPriority.add(priority);
        }
    }

    public String getError() {
        boolean foundCurse = false;
        for (String spell : spellPriority) {
            if (!Spell.ALL_SPELLS.contains(spell)) {
                return spell + " can no longer be prioritized.";
            }
            if (spell.startsWith("Curse")) {
                if (foundCurse) {
                    return "You may only include one curse.";
                }
                foundCurse = true;
            }
        }

        int immo = spellPriority.indexOf("Immolate");
        int conf = spellPriority.indexOf("Conflagrate");
        if (conf >= 0 && conf < immo) {
            return "Conflagrate may only appear after Immolate.";
        }

        if (immo >= 0 && spellPriority.contains("Unstable Affliction")) {
            return "Unstable Affliction and Immolate don't mix.";
        }

        return null;
    }

    /**
     * Gets a modified version of the user's spell priorities, for internal
     * purposes.
     */
    public List<String> getPrioritiesForCalcs(WarlockTalents talents, boolean execute) {
        List<String> forCalcs = new ArrayList<String>(spellPriority);
        if (talents.getBackdraft() > 0
                && !spellPriority.contains("Incinerate (Under Backdraft)")) {

            forCalcs.add(forCalcs.size(), "Incinerate (Under Backdraft)");
        }
        if (!execute
                && filler.equals("Shadow Bolt")
                && !forCalcs.contains("Shadow Bolt (Instant)")
                && ShadowBolt_Instant.isCastable(talents, forCalcs)) {

            forCalcs.add(forCalcs.size(), "Shadow Bolt (Instant)");
        }
        return forCalcs;
    }

    public boolean contains(String spellname) {
        return spellPriority.contains(spellname)
                || filler.equals(spellname);
    }
}
