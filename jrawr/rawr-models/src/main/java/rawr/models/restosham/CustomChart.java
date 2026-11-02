// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/Charting.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.restosham;

/**
 * Class used to describe custom charts
 */
final class CustomChart {
    public String chartName;
    public ChartCalculator chartCalculatorMethod;

    public CustomChart() {
    }

    public CustomChart(String chartName, ChartCalculator chartCalculatorMethod) {
        this.chartName = chartName;
        this.chartCalculatorMethod = chartCalculatorMethod;
    }

    @Override
    public String toString() {
        return this.chartName;
    }
}
