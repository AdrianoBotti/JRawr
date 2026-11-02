// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Profs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * Conversions between {@link Profession} and the representations used by the UI and file
 * formats: the 1-based profession index (1 = Alchemy ... 11 = Tailoring) and the English
 * profession name.
 *
 * <p>Unknown inputs map to {@link Profession#None} (or index 0) instead of failing, matching the
 * original dictionary lookups, so an unrecognized name in old data is not an error. The index
 * order deliberately differs from the {@link Profession} declaration order, so the conversions
 * are explicit tables rather than {@code ordinal()} arithmetic. All methods are pure.</p>
 */
public final class Profs {
    private Profs() {
    }

    /** Returns the profession for a 1-based index; anything outside 1..11 yields {@link Profession#None}. */
    public static Profession indexToProfession(int i) {
        Profession p = Profession.None;
        if (i == 1) {
            p = Profession.Alchemy;
        } else if (i == 2) {
            p = Profession.Blacksmithing;
        } else if (i == 3) {
            p = Profession.Enchanting;
        } else if (i == 4) {
            p = Profession.Engineering;
        } else if (i == 5) {
            p = Profession.Herbalism;
        } else if (i == 6) {
            p = Profession.Inscription;
        } else if (i == 7) {
            p = Profession.Jewelcrafting;
        } else if (i == 8) {
            p = Profession.Leatherworking;
        } else if (i == 9) {
            p = Profession.Mining;
        } else if (i == 10) {
            p = Profession.Skinning;
        } else if (i == 11) {
            p = Profession.Tailoring;
        }
        return p;
    }

    /** Returns the 1-based index stored in saved data; {@link Profession#None} and unmatched values yield 0. */
    public static int professionToIndex(Profession p) {
        int s = 0;
        if (p == Profession.Alchemy) {
            s = 1;
        } else if (p == Profession.Blacksmithing) {
            s = 2;
        } else if (p == Profession.Enchanting) {
            s = 3;
        } else if (p == Profession.Engineering) {
            s = 4;
        } else if (p == Profession.Herbalism) {
            s = 5;
        } else if (p == Profession.Inscription) {
            s = 6;
        } else if (p == Profession.Jewelcrafting) {
            s = 7;
        } else if (p == Profession.Leatherworking) {
            s = 8;
        } else if (p == Profession.Mining) {
            s = 9;
        } else if (p == Profession.Skinning) {
            s = 10;
        } else if (p == Profession.Tailoring) {
            s = 11;
        }
        return s;
    }

    /** Parses an exact English profession name as written by {@link #professionToString(Profession)}; unknown names yield {@link Profession#None}. */
    public static Profession stringToProfession(String s) {
        Profession p = Profession.None;
        if ("Alchemy".equals(s)) {
            p = Profession.Alchemy;
        } else if ("Blacksmithing".equals(s)) {
            p = Profession.Blacksmithing;
        } else if ("Enchanting".equals(s)) {
            p = Profession.Enchanting;
        } else if ("Engineering".equals(s)) {
            p = Profession.Engineering;
        } else if ("Herbalism".equals(s)) {
            p = Profession.Herbalism;
        } else if ("Inscription".equals(s)) {
            p = Profession.Inscription;
        } else if ("Jewelcrafting".equals(s)) {
            p = Profession.Jewelcrafting;
        } else if ("Leatherworking".equals(s)) {
            p = Profession.Leatherworking;
        } else if ("Mining".equals(s)) {
            p = Profession.Mining;
        } else if ("Skinning".equals(s)) {
            p = Profession.Skinning;
        } else if ("Tailoring".equals(s)) {
            p = Profession.Tailoring;
        }
        return p;
    }

    /** Canonical English name written to files and shown in the UI; {@link Profession#None} (and unmatched values) yield {@code "None"}. */
    public static String professionToString(Profession p) {
        String s = "None";
        if (p == Profession.Alchemy) {
            s = "Alchemy";
        } else if (p == Profession.Blacksmithing) {
            s = "Blacksmithing";
        } else if (p == Profession.Enchanting) {
            s = "Enchanting";
        } else if (p == Profession.Engineering) {
            s = "Engineering";
        } else if (p == Profession.Herbalism) {
            s = "Herbalism";
        } else if (p == Profession.Inscription) {
            s = "Inscription";
        } else if (p == Profession.Jewelcrafting) {
            s = "Jewelcrafting";
        } else if (p == Profession.Leatherworking) {
            s = "Leatherworking";
        } else if (p == Profession.Mining) {
            s = "Mining";
        } else if (p == Profession.Skinning) {
            s = "Skinning";
        } else if (p == Profession.Tailoring) {
            s = "Tailoring";
        }
        return s;
    }
}
