// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// PARTIAL: the description field is omitted (UI only).
/**
 * Metadata attached to every glyph getter of a {@code *Talents} class (for example
 * {@code MageTalents.isGlyphOfFireball()}), replacing the C# {@code [GlyphData(...)]} attribute. A
 * <b>glyph</b> is a permanent one-slot inscription modelled as a boolean; the game limits a
 * character to three <b>major</b> glyphs (the stronger, ability-altering ones) and three
 * <b>minor</b> ones (cosmetic or utility), and {@link #major()} records which kind an entry is.
 *
 * <ul>
 *   <li>{@link #index()} &mdash; the slot into the class's {@code boolean[]} glyph array; the
 *       getter returns {@code glyphData[index]}.</li>
 *   <li>{@link #name()} &mdash; English glyph name (display only in this port).</li>
 *   <li>{@link #major()} &mdash; {@code true} for a major glyph, {@code false} for a minor one.</li>
 * </ul>
 *
 * <p>Unlike {@link TalentDataAttribute}, no code in the Java engine reads this annotation at
 * runtime today: models call the {@code isGlyph...()} getters directly and the flags travel in the
 * serialized talent string. It is kept {@code RUNTIME}-retained for parity with the original and so
 * a future UI can enumerate the available glyphs as the C# panels did. The C# attribute also
 * carried a description string, omitted here as UI-only.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GlyphDataAttribute {
    int index();

    String name();

    boolean major();
}
