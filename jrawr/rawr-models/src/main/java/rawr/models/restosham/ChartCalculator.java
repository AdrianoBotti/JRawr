// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/Charting.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

import rawr.base.character.Character;
import rawr.base.calculations.ComparisonCalculationBase;

import java.util.Collection;

/**
 * Delegate function that lets us specify the calculator method for a chart
 */
@FunctionalInterface
public interface ChartCalculator {
    /**
     * @param character       Character object to chart
     * @param baseCalculations The calculation object for the character
     * @return Collection of the calculation comparison values
     */
    Collection<ComparisonCalculationBase> apply(Character character, CalculationsRestoSham baseCalculations);
}
