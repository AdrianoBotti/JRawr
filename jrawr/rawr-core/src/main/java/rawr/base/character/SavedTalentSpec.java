// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SavedTalentSpec.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: Load/Save(TextReader/TextWriter) are replaced by load(SavedTalentSpecList) because XML
// parsing lives in rawr-data loaders (see Buff.load).
package rawr.base.character;

import rawr.base.talents.DeathKnightTalents;
import rawr.base.talents.DruidTalents;
import rawr.base.talents.HunterTalents;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.TalentsBase;
import rawr.base.talents.WarlockTalents;
import rawr.base.talents.WarriorTalents;

import java.util.Locale;

/**
 * One named talent build ("spec") of one character class, as stored in the user's saved-specs
 * list.
 *
 * <p>A talent spec is the allocation of a limited number of talent points across a class's three
 * talent trees; it is what makes one Mage an "Arcane Mage" and another a "Fire Mage". The
 * canonical build representation is the compact string produced by {@link TalentsBase#toString()}
 * and stored in {@code spec}; the three ints are just the points spent per tree, used for
 * display and for the saved-specs list. {@link #talentSpec()} rebuilds the typed {@code *Talents}
 * object by handing that string to the right class constructor. The registry of all saved specs
 * is process-wide static state installed by {@link #load(SavedTalentSpecList)} and queried
 * through {@link #getAllSpecs()} / {@link #specsFor(CharacterClass)}; until the rawr-data loader
 * runs, {@code getAllSpecs()} may return {@code null}.</p>
 */
public class SavedTalentSpec {
    public String name;
    public CharacterClass characterClass;
    public String spec;

    public int tree1;
    public int tree2;
    public int tree3;

    public SavedTalentSpec() {
        this("", null, 0, 0, 0);
    }

    private static SavedTalentSpecList allSpecs;

    public static SavedTalentSpecList getAllSpecs() {
        return allSpecs;
    }

    /** Installs the process-wide registry of saved specs, replacing any previous one (called by the rawr-data loader). */
    public static void load(SavedTalentSpecList specs) {
        allSpecs = specs;
    }

    /** Returns a new list with the saved specs of one class, in registry order. */
    public static SavedTalentSpecList specsFor(CharacterClass charClass) {
        SavedTalentSpecList ret = new SavedTalentSpecList();
        for (SavedTalentSpec sts : getAllSpecs()) {
            if (sts.characterClass == charClass)
                ret.add(sts);
        }
        return ret;
    }

    public SavedTalentSpec(String name, TalentsBase talentSpec, int tree1, int tree2, int tree3) {
        this.name = name;
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.tree3 = tree3;
        if (talentSpec != null) {
            spec = talentSpec.toString();
            characterClass = talentSpec.getCharacterClass();
        }
    }

    /**
     * Reconstructs the typed talents object by re-parsing {@code spec} with the constructor of the
     * matching {@code *Talents} class. Returns {@code null} when no spec string is set; a class
     * without its own branch falls through to {@code MageTalents} exactly like the C# code.
     */
    public TalentsBase talentSpec() {
        if (spec == null) return null;
        TalentsBase ts;
        if (characterClass == CharacterClass.DeathKnight) ts = new DeathKnightTalents(spec);
        else if (characterClass == CharacterClass.Warrior) ts = new WarriorTalents(spec); // CataMode branch is commented out in the original
        else if (characterClass == CharacterClass.Paladin) ts = new PaladinTalents(spec);
        else if (characterClass == CharacterClass.Shaman) ts = new ShamanTalents(spec);
        else if (characterClass == CharacterClass.Hunter) ts = new HunterTalents(spec);
        else if (characterClass == CharacterClass.Rogue) ts = new RogueTalents(spec);
        else if (characterClass == CharacterClass.Druid) ts = new DruidTalents(spec);
        else if (characterClass == CharacterClass.Warlock) ts = new WarlockTalents(spec);
        else if (characterClass == CharacterClass.Priest) ts = new PriestTalents(spec);
        else ts = new MageTalents(spec);
        return ts;
    }

    /**
     * Display form: name, the {@code tree1/tree2/tree3} point split, and a warning when the saved
     * points do not total 71 (the level-80 talent-point budget): "N Points Left" when under,
     * "N Points Over" when above.
     */
    @Override
    public String toString() {
        String warning = "";
        int pointsleft = 71 - (tree1 + tree2 + tree3);

        if (pointsleft > 0)
            warning = String.format(Locale.ROOT, " (%d Points Left)", pointsleft);
        else if (pointsleft < 0)
            warning = String.format(Locale.ROOT, " (%d Points Over)", Math.abs(pointsleft));

        return String.format(Locale.ROOT, "%s (%d/%d/%d)%s",
                name == null ? "" : name, tree1, tree2, tree3, warning);
    }

    /**
     * Compares the canonical spec string and the class of another talents object; the spec name is
     * not part of the comparison. Returns false when either side has no spec string.
     */
    public boolean equals(TalentsBase talents) {
        if (talents == null || spec == null)
            return false;
        return talents.toString().equals(spec) && characterClass == talents.getCharacterClass();
    }
}
