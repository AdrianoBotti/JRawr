// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

import rawr.base.optimizer.ItemInstanceOptimizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// PARTIAL: the tab type, description and icon fields are omitted (UI only).
/**
 * Metadata attached to every talent-rank getter of a {@code *Talents} class (for example
 * {@code MageTalents.getArcaneConcentration()}), replacing the C# {@code [TalentData(...)]}
 * attribute. Each element describes one slot of the concrete class's {@code int[]} data array.
 *
 * <ul>
 *   <li>{@link #index()} &mdash; the slot into the class's data array; the getter returns
 *       {@code data[index]}.</li>
 *   <li>{@link #name()} &mdash; English talent name; the optimizer and display code use it as the
 *       human-readable key. The legacy Character Profiler importer also used it as the saved-
 *       variables key; for localized data it matched {@link #tree()} against the tree's
 *       {@code Order} field (1-based) and {@link #row()}:{@link #column()} against the talent's
 *       {@code Location}.</li>
 *   <li>{@link #maxPoints()} &mdash; maximum rank the talent accepts (5 in the WotLK tables, 3 in
 *       {@link WarriorTalentsCata}); the optimizer treats a slot holding this many points as
 *       full.</li>
 *   <li>{@link #tree()} &mdash; zero-based index into the class's {@code TreeNames} array.</li>
 *   <li>{@link #column()} / {@link #row()} &mdash; position in that tree's grid, row 1 at the top.
 *       {@link ItemInstanceOptimizer} uses {@code row} to enforce the game's gating rule that a
 *       talent on row N requires 5*(N-1) points already spent in the same tree.</li>
 *   <li>{@link #prerequisite()} &mdash; index of another talent that must be filled before this one
 *       can be trained, or -1 when there is none. The optimizer builds its talent mutation graph
 *       from this field (a prerequisite's filled slot blocks removing points from its children).</li>
 * </ul>
 *
 * <p>Consumers find these attributes reflectively by iterating a concrete class's getters and
 * calling {@code getAnnotation(TalentDataAttribute.class)}, so the annotation must stay
 * {@code RUNTIME}-retained and on the getter, not the setter.
 * The C# attribute also carried a tab type, a per-rank description array and an icon name; those
 * were UI-only and are omitted here.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TalentDataAttribute {
    int index();

    String name();

    int maxPoints();

    int tree();

    int column();

    int row();

    int prerequisite();
}
