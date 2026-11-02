package rawr.golden;

import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.GemmingTemplate;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Test double registered as the "Mage" model so the base-code paths that consult the current model
 * (gem counts, enchant slot fitting) behave like the real model's defaults. The calculation entry
 * points are unused by the Base-level gates.
 */
public class TestMageModel extends CalculationsBase {
    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return null;
    }

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        return new ArrayList<GemmingTemplate>();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        return null;
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        return null;
    }

    @Override
    public rawr.base.stats.Stats getCharacterStats(Character character, Item additionalItem) {
        return null;
    }
}
