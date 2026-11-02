// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/Charting.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

import rawr.base.character.Character;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class used to define the various calculator methods for the custom charts
 */
final class ChartCalculators {
    private ChartCalculators() {
    }

    public static List<ComparisonCalculationBase> calculateMAPSChart(Character character, CalculationsRestoSham baseCalculations) {
        List<ComparisonCalculationBase> list = new ArrayList<ComparisonCalculationBase>();
        CharacterCalculationsRestoSham calc = (CharacterCalculationsRestoSham) baseCalculations.getCharacterCalculations(character);
        if (calc == null)
            calc = new CharacterCalculationsRestoSham();

        StatRelativeWeight[] stats = new StatRelativeWeight[] {
                new StatRelativeWeight("Intellect", statsIntellect(10f)),
                new StatRelativeWeight("Haste Rating", statsHasteRating(10f)),
                new StatRelativeWeight("Spellpower", statsSpellPower(10f)),
                new StatRelativeWeight("MP5", statsMp5(10f)),
                new StatRelativeWeight("Crit Rating", statsCritRating(10f))};

        // Get the percentage total healing is changed by a change in a single stat:
        for (StatRelativeWeight weight : stats) {
            CharacterCalculationsRestoSham statCalc = (CharacterCalculationsRestoSham) baseCalculations.getCharacterCalculations(character, null, weight.stat);
            weight.change = (statCalc.getMAPS() - calc.getMAPS());
        }

        // Create the chart data points:
        for (StatRelativeWeight weight : stats) {
            ComparisonCalculationRestoSham comp = new ComparisonCalculationRestoSham(weight.name);
            comp.setOverallPoints(weight.change);
            comp.setSubPoints(new float[] { 0f, weight.change, 0f });
            comp.setDescription(String.format(Locale.ROOT, "If you added 10 more %s.", weight.name));
            list.add(comp);
        }

        return list;
    }

    public static List<ComparisonCalculationBase> calculateSequencesChart(Character character, CalculationsRestoSham baseCalculations) {
        List<ComparisonCalculationBase> list = new ArrayList<ComparisonCalculationBase>();
        CalculationOptionsRestoSham originalOptions = character.getCalculationOptions() instanceof CalculationOptionsRestoSham
                ? (CalculationOptionsRestoSham) character.getCalculationOptions()
                : null;
        if (originalOptions == null)
            originalOptions = new CalculationOptionsRestoSham();

        CalculationOptionsRestoSham opts = originalOptions;
        String[] styles = new String[] { "CH Spam", "HW Spam", "LHW Spam", "RT+HW", "RT+CH", "RT+LHW" };
        String[] descs = new String[] {
                "All chain heal, all the time.  \nMana available for use per minute added to sustained.",
                "All healing wave, all the time  \nMana available for use per minute added to sustained.",
                "All lesser healing wave, all the time  \nMana available for use per minute added to sustained.",
                "Riptide + Healing Wave.  \nMana available for use per minute added to sustained.",
                "Riptide + Chain Heal.  \nMana available for use per minute added to sustained.",
                "Riptide + Lesser Healing Wave.\nMana available for use per minute added to sustained."
        };
        for (int i = 0; i < styles.length; i++) {
            opts.setSustStyle(styles[i]);
            opts.setBurstStyle(styles[i]);
            character.setCalculationOptions(opts);
            CharacterCalculationsRestoSham statCalc = (CharacterCalculationsRestoSham) baseCalculations.getCharacterCalculations(character);

            // normalize the mana a bit to make a better chart
            float mana = statCalc.getManaUsed() / (opts.getFightLength());

            ComparisonCalculationRestoSham hsComp = new ComparisonCalculationRestoSham(styles[i]);
            hsComp.setOverallPoints(statCalc.getBurstHPS() + statCalc.getSustainedHPS() + mana);
            hsComp.setSubPoints(new float[] { statCalc.getBurstHPS(), statCalc.getSustainedHPS() + mana, 0f });
            hsComp.setDescription(descs[i]);
            list.add(hsComp);
        }

        return list;
    }

    private static Stats statsIntellect(float value) {
        Stats stats = new Stats();
        stats.setIntellect(value);
        return stats;
    }

    private static Stats statsHasteRating(float value) {
        Stats stats = new Stats();
        stats.setHasteRating(value);
        return stats;
    }

    private static Stats statsSpellPower(float value) {
        Stats stats = new Stats();
        stats.setSpellPower(value);
        return stats;
    }

    private static Stats statsMp5(float value) {
        Stats stats = new Stats();
        stats.setMp5(value);
        return stats;
    }

    private static Stats statsCritRating(float value) {
        Stats stats = new Stats();
        stats.setCritRating(value);
        return stats;
    }
}
