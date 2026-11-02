// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.optimizer.ItemInstanceOptimizer;

// Subclasses implement a covariant clone() (C# ICloneable).
/**
 * Abstract base for a character's talent and glyph allocation: the shared contract of the concrete
 * class tables in this package ({@link MageTalents}, {@link WarriorTalents}, ...).
 *
 * <p>At level 80 a player spends 71 <b>talent points</b> across three class-specific <b>talent
 * trees</b> (Arcane/Fire/Frost for a mage). A <b>talent</b> is one entry in a tree; points put into
 * it raise its <b>rank</b> up to a per-talent maximum. <b>Glyphs</b> are separate one-slot
 * inscriptions that are either present or not, hence the boolean storage. Each subclass owns a
 * {@code int[] data} (one rank per talent slot) and a {@code boolean[] glyphData} (one flag per
 * glyph slot); the slot meanings are declared by the {@link TalentDataAttribute} and
 * {@link GlyphDataAttribute} annotations on the subclass getters, and reflective consumers such as
 * {@link ItemInstanceOptimizer} read them. This class itself performs no lookup by name or id and no
 * point validation; those live in the consumers and in the models.</p>
 *
 * <h2>Serialization</h2>
 *
 * <p>{@link #toString()} and {@link #loadString(String)} round-trip one compact string: one decimal
 * digit per talent slot ("5" = rank 5, "0" = untrained), then a {@code '.'} and one {@code '1'} or
 * {@code '0'} character per glyph slot. That string is what the saved character XML stores and what
 * {@link SavedTalentSpec} keeps. {@code loadString} reads the talent part only when it is at least
 * {@code getData().length} characters long (the first {@code data.length} digits are used) and the
 * glyph part only when its length matches the glyph array exactly; beyond that it does not
 * validate.</p>
 *
 * <h2>Caller contract</h2>
 *
 * <p>{@link #getData()} and {@link #getGlyphData()} return the <b>live</b> arrays, not copies:
 * callers see and may mutate the same storage (the optimizer writes through it, and the mage
 * {@code Solver} sums rank ranges per tree). {@link #clone()} yields an independent
 * copy. {@code getGlyphData} may return {@code null} on a subclass without glyphs, although every
 * class in this package overrides it.</p>
 *
 * <p><b>Porting note:</b> the C# original compared exact types in {@code GetClass}; this port uses
 * {@code instanceof} in {@link #getCharacterClass()}. With the current hierarchy the result is
 * identical, and any type not listed there (including {@link WarriorTalentsCata}) falls through to
 * {@link CharacterClass#Warrior}.</p>
 */
public abstract class TalentsBase {
    /**
     * The live rank array, one slot per talent. The slot meaning of each position is declared by
     * the concrete class's {@link TalentDataAttribute} metadata. This is not a copy: callers
     * (optimizer and model code) read or write the same storage, so clone first when a
     * shared allocation must not change.
     */
    public abstract int[] getData();

    /**
     * Independent deep copy of this allocation (the C# {@code ICloneable.Clone} contract). Every
     * concrete class narrows the return type so callers avoid a cast; the copied data and glyph
     * arrays share nothing with the original.
     */
    public abstract TalentsBase clone();

    /**
     * The live glyph flags, one slot per {@link GlyphDataAttribute} entry, or {@code null} when a
     * subclass has no glyph table. Every class in this package overrides it. Same aliasing rule as
     * {@link #getData()}.
     */
    public boolean[] getGlyphData() {
        return null;
    }

    /**
     * Parses the compact allocation string written by {@link #toString()}, which is how saved
     * character files and {@link SavedTalentSpec} carry talents. Null and empty input are ignored.
     * The talent part is applied only when it is at least {@code getData().length} characters long
     * (the first {@code data.length} digits are used), the glyph part only when its length matches
     * exactly. There is no further validation, so a non-digit character throws
     * {@link NumberFormatException}.
     */
    protected void loadString(String code) {
        if (code == null || code.isEmpty()) return;
        int[] data = getData();
        String[] tmp = code.split("\\.");
        String talents = tmp[0];
        if (talents.length() >= data.length) {
            for (int i = 0; i < data.length; i++) {
                data[i] = Integer.parseInt(talents.substring(i, i + 1));
            }
        }
        if (tmp.length > 1) {
            String glyphs = tmp[1];
            boolean[] glyphData = getGlyphData();
            if (glyphData != null && glyphs.length() == glyphData.length) {
                for (int i = 0; i < glyphData.length; i++) {
                    glyphData[i] = Integer.parseInt(glyphs.substring(i, i + 1)) == 1;
                }
            }
        }
    }

    /**
     * Serializes the allocation as one decimal digit per talent slot, then a {@code '.'} and one
     * {@code '1'} or {@code '0'} per glyph slot (the glyph section is omitted when
     * {@link #getGlyphData()} is {@code null}). This is the string stored under
     * {@code <MageTalents>} and the like in saved character XML; see {@link #loadString(String)}
     * for the parse rules.
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int digit : getData())
            ret.append(Integer.toString(digit));
        if (getGlyphData() != null) {
            ret.append('.');
            for (boolean glyph : getGlyphData()) {
                ret.append(glyph ? '1' : '0');
            }
        }
        return ret.toString();
    }

    /**
     * Maps the concrete allocation to its {@link CharacterClass}, checking types in the same order
     * as the C# {@code GetClass} (Warlock, Mage, Priest, Druid, Rogue, Hunter, Shaman, DeathKnight,
     * Paladin) and defaulting to {@link CharacterClass#Warrior} for anything else, including
     * {@link WarriorTalentsCata}.
     */
    public CharacterClass getCharacterClass() {
        return switch (this) {
            case WarlockTalents ignored -> CharacterClass.Warlock;
            case MageTalents ignored -> CharacterClass.Mage;
            case PriestTalents ignored -> CharacterClass.Priest;
            case DruidTalents ignored -> CharacterClass.Druid;
            case RogueTalents ignored -> CharacterClass.Rogue;
            case HunterTalents ignored -> CharacterClass.Hunter;
            case ShamanTalents ignored -> CharacterClass.Shaman;
            case DeathKnightTalents ignored -> CharacterClass.DeathKnight;
            case PaladinTalents ignored -> CharacterClass.Paladin;
            default -> CharacterClass.Warrior;
        };
    }
}
