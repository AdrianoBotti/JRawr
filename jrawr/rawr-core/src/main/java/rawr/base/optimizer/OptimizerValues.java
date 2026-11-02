// Java-only helper extracted from the three identical optimizer value parsers.
package rawr.base.optimizer;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

/**
 * Parses the optimizer's calculation-name syntax: {@code "[Overall]"}, {@code "[SubPoint N]"},
 * {@code "[Talent N]"}, {@code "[Glyph N]"}, or any other name delegated to the model.
 *
 * <p>The same parser was duplicated in {@link ItemOptimizer}, {@link ItemInstanceOptimizer} and the
 * batch optimizer. It is a pure function of the character and the current calculation result; the
 * order of the prefix checks and the {@code trimEndBracket} behavior are observable and must stay
 * as they were.</p>
 */
public final class OptimizerValues {
    private OptimizerValues() {
    }

    public static float getCalculationValue(Character character, CharacterCalculationsBase calcs, String calculation) {
        if (calculation == null || "[Overall]".equals(calculation)) {
            return calcs.getOverallPoints();
        } else if ("[Cost]".equals(calculation)) {
            return getNonAvailableItemCost(character);
        } else if (calculation.startsWith("[SubPoint ")) {
            return calcs.getSubPoints()[Integer.parseInt(trimEndBracket(calculation.substring(10)))];
        } else if (calculation.startsWith("[Talent ")) {
            return character.getCurrentTalents().getData()[Integer.parseInt(trimEndBracket(calculation.substring(8)))];
        } else if (calculation.startsWith("[Glyph ")) {
            return character.getCurrentTalents().getGlyphData()[Integer.parseInt(trimEndBracket(calculation.substring(7)))] ? 1 : 0;
        } else {
            return calcs.getOptimizableCalculationValue(calculation);
        }
    }

    /**
     * C# Optimizer.GetNonAvailableItemCost: the purchase cost of the equipped items that the
     * candidate generator marked as positive-cost (badge/vendor) items.
     */
    public static float getNonAvailableItemCost(Character character) {
        float cost = 0f;
        for (int slot = 0; slot < 19; slot++) {
            ItemInstance instance = character.get(CharacterSlot.fromValue(slot));
            if (instance == null) {
                continue;
            }
            Item item = instance.getItem();
            if (item != null && item.getAvailabilityInformation() != null
                    && item.getAvailabilityInformation().positiveCostItem) {
                cost += item.getCost();
            }
        }
        return cost;
    }

    /** C# {@code string.TrimEnd(']')}. */
    public static String trimEndBracket(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ']') {
            end--;
        }
        return value.substring(0, end);
    }
}
