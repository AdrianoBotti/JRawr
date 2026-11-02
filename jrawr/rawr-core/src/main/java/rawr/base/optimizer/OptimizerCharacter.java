// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Optimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.optimizer;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.talents.TalentsBase;

/**
 * The working copy of a character used as one genetic-algorithm individual by
 * {@link ItemInstanceOptimizer}.
 *
 * <p>{@link #items} is the gene array: the first 19 entries are {@link ItemInstance}s (or null for
 * an empty slot), optionally followed by a food {@link Buff}, a battle elixir or flask, a guardian
 * elixir and a {@link TalentsBase} when the corresponding optimizer options are enabled.
 * {@link #character} is the live {@link Character} those genes are applied to; the optimizer
 * recycles one instance per individual across generations, so a copy must be made explicitly
 * whenever an individual is carried over.</p>
 *
 * <p>The constructor classifies the consumes already active on the character into the extra slots:
 * a buff that conflicts with both a battle and a guardian elixir is treated as a flask and stored
 * in the battle slot, and talents always go into the last extra slot. When either the food or the
 * elixir option is enabled, both kinds of active consume are classified. {@link #extraItem} is
 * scratch space the GreedyBest direct-upgrade scan uses to evaluate one gem or enchant in
 * isolation; it is not part of a real individual.</p>
 */
public class OptimizerCharacter {
    public Object[] items;
    public Character character;
    public Item extraItem;

    private static final int characterSlots = 19;

    public OptimizerCharacter() {
    }

    public OptimizerCharacter(Character character, boolean includeFood, boolean includeElixir, boolean includeTalents) {
        this.character = character;
        if (includeFood || includeElixir || includeTalents) {
            items = new Object[characterSlots + 4];
            System.arraycopy(character.getRawItems(), 0, items, 0, characterSlots);
            if (includeFood || includeElixir) {
                for (Buff buff : character.getActiveBuffs()) {
                    if ("Elixirs and Flasks".equals(buff.group)) {
                        boolean isbattle = buff.getConflictingBuffs().contains("Battle Elixir");
                        boolean isguardian = buff.getConflictingBuffs().contains("Guardian Elixir");
                        if (isbattle && isguardian) {
                            items[characterSlots + 1] = buff;
                        } else if (isbattle) {
                            items[characterSlots + 1] = buff;
                        } else if (isguardian) {
                            items[characterSlots + 2] = buff;
                        }
                    } else if ("Food".equals(buff.group)) {
                        items[characterSlots] = buff;
                    }
                }
            }
            items[characterSlots + 3] = character.getCurrentTalents();
        } else {
            items = character.getRawItems();
        }
    }
}
