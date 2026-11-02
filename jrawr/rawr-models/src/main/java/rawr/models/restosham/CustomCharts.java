// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/Charting.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: StatsGraphColors (System.Drawing.Color) and the RAWR3 branches are omitted; the
// non-Silverlight (desktop) chart names/calculators are ported.
package rawr.models.restosham;

import rawr.base.stats.Stats;

/**
 * Class used to define the available custom charts
 */
final class CustomCharts {
    private CustomCharts() {
    }

    static final CustomChart[] Charts = {
            new CustomChart("Mana Available per Second", ChartCalculators::calculateMAPSChart),
            new CustomChart("Healing Sequences", ChartCalculators::calculateSequencesChart)
    };

    public static final String[] CustomRenderedChartNames = {
            "Burst Stats Graph",
            "Sustained Stats Graph",
            "Overall Rating Stats Graph"
    };

    public static final Stats[] StatsGraphStatsList = new Stats[] {
            statsWithSpellPower(1f),
            statsWithMp5(1f),
            statsWithCritRating(1f),
            statsWithHasteRating(1f),
            statsWithIntellect(1f)
    };

    public static String[] getCustomChartNames() {
        if (Charts == null || Charts.length == 0)
            return null;

        String[] chartNames = new String[Charts.length];
        for (int i = 0; i < Charts.length; i++) {
            chartNames[i] = Charts[i].toString();
        }
        return chartNames;
    }

    public static ChartCalculator getChartCalculator(String chartName) {
        for (CustomChart c : Charts) {
            if (c.chartName.equals(chartName))
                return c.chartCalculatorMethod;
        }
        return null;
    }

    private static Stats statsWithSpellPower(float value) {
        Stats stats = new Stats();
        stats.setSpellPower(value);
        return stats;
    }

    private static Stats statsWithMp5(float value) {
        Stats stats = new Stats();
        stats.setMp5(value);
        return stats;
    }

    private static Stats statsWithCritRating(float value) {
        Stats stats = new Stats();
        stats.setCritRating(value);
        return stats;
    }

    private static Stats statsWithHasteRating(float value) {
        Stats stats = new Stats();
        stats.setHasteRating(value);
        return stats;
    }

    private static Stats statsWithIntellect(float value) {
        Stats stats = new Stats();
        stats.setIntellect(value);
        return stats;
    }
}
