// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchOptimizer.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted (reported): BossOptions are not ported in the Java core, so the Character copy constructor
// call drops that argument (see Character(String, String, CharacterRegion, CharacterRace,
// ItemInstance[], List<Buff>, String)).
package rawr.base.batch;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterRegion;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One individual of the batch genetic algorithm: a gene array that assigns items to the equipment
 * slots of every character in the batch at once.
 *
 * <p>The layout is: the first {@code itemCount} gene slots hold one representative
 * {@link ItemInstance} per distinct item in the shared pool, one extra slot holds the item
 * currently being tested as an upgrade (or nothing), and then {@code batchSize * 19} genes each
 * hold a plain {@link Item} for one slot of one character. Because the batch objective is the
 * weighted sum of the characters' ratings, the search has to decide which character gets a scarce
 * item.</p>
 *
 * <p>The constructor decodes the genes into {@link #character} (one rebuilt {@link Character} per
 * batch member, taking buffs, options, class and talents from the original batch list), fills
 * {@link #availableItem} with the representative instance of every pool item, and fills
 * {@link #usedCount} with how many times each pool item is used across the whole batch. An item
 * gene equal to the upgrade item under test is mapped to the extra slot instead of a pool slot.
 * The optimizer allocates a new {@code BatchIndividual} for every candidate rather than recycling
 * one, so a constructed object is only read from afterwards.</p>
 */
public class BatchIndividual {
    public Object[] items;
    public ItemInstance[] availableItem;
    public int[] usedCount;
    public List<Character> character;

    private static final int characterSlots = 19;

    public BatchIndividual(Object[] items, int itemCount, Map<Integer, Integer> indexFromId, Item upgradeItem, List<Character> batchList) {
        this.items = items;
        availableItem = new ItemInstance[itemCount + 1];
        usedCount = new int[itemCount + 1];
        for (int i = 0; i <= itemCount; i++) {
            ItemInstance itemInstance = (ItemInstance) items[i];
            availableItem[i] = itemInstance;
        }
        int charCount = (items.length - itemCount - 1) / characterSlots;
        character = new ArrayList<Character>();
        for (int c = 0; c < charCount; c++) {
            ItemInstance[] slotItems = new ItemInstance[characterSlots];
            for (int i = 0; i < characterSlots; i++) {
                Item item = (Item) items[itemCount + 1 + c * characterSlots + i];
                if (item != null) {
                    if (item == upgradeItem) {
                        slotItems[i] = availableItem[itemCount];
                        usedCount[itemCount]++;
                    } else {
                        int index = indexFromId.get(item.getId());
                        slotItems[i] = availableItem[index];
                        usedCount[index]++;
                    }
                }
            }
            Character sourceCharacter = batchList.get(c);
            Character ch = new Character(sourceCharacter.getName(), sourceCharacter.getRealm(), sourceCharacter.getRegion(), sourceCharacter.getRace(), slotItems, sourceCharacter.getActiveBuffs(), sourceCharacter.getCurrentModel());
            ch.setCalculationOptions(sourceCharacter.getCalculationOptions());
            ch.setCharacterClass(sourceCharacter.getCharacterClass());
            ch.assignAllTalentsFromCharacter(sourceCharacter, false);
            ch.setEnforceGemRequirements(sourceCharacter.getEnforceGemRequirements());
            character.add(ch);
        }
    }
}
