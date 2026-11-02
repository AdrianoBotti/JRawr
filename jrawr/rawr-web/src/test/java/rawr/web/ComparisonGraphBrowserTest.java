package rawr.web;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-browser checks for the two chart renderers: the Relative Stats histogram toggle and
 * the legacy multiline Stat Graph data renderer.
 */
class ComparisonGraphBrowserTest {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html><body>
            <select id="compare-view" data-compare-view>
              <option value="rows">Rows</option>
              <option value="histogram">Histogram</option>
            </select>
            <input id="compare-filter" type="search" data-compare-filter>
            <span data-stat-visible-count>2</span>
            <div id="compare-list" data-compare-rows data-compare-list>
              <div class="compare-row compare-stat-row" data-name="Spell Power" data-overall="2.5">
                <span class="compare-subpoint" data-sub-index="0" data-sub-value="2.5"
                      style="color:#62a8ff"><span class="compare-subpoint-label">Dps</span></span>
                <span class="compare-subpoint" data-sub-index="1" data-sub-value="0"
                      style="color:#63d68f"><span class="compare-subpoint-label">Survivability</span></span>
              </div>
              <div class="compare-row compare-stat-row" data-name="Crit Rating" data-overall="1.5">
                <span class="compare-subpoint" data-sub-index="0" data-sub-value="1.5"
                      style="color:#62a8ff"><span class="compare-subpoint-label">Dps</span></span>
                <span class="compare-subpoint" data-sub-index="1" data-sub-value="0"
                      style="color:#63d68f"><span class="compare-subpoint-label">Survivability</span></span>
              </div>
              <div class="compare-row compare-stat-row" data-name="Health" data-overall="-0.5">
                <span class="compare-subpoint" data-sub-index="0" data-sub-value="0"
                      style="color:#62a8ff"><span class="compare-subpoint-label">Dps</span></span>
                <span class="compare-subpoint" data-sub-index="1" data-sub-value="-0.5"
                      style="color:#63d68f"><span class="compare-subpoint-label">Survivability</span></span>
              </div>
            </div>
            <div id="compare-histogram" data-compare-histogram hidden></div>
            <div data-stat-graph-data hidden>
              <span data-stat-series data-key="HitRating" data-label="Hit Rating" data-color="#ff6b6b"
                    data-visible="true" data-points="-10,-0.4 0,0 10,0.4"></span>
              <span data-stat-series data-key="CritRating" data-label="Crit Rating" data-color="#62a8ff"
                    data-visible="true" data-points="-10,-0.2 0,0 10,0.2"></span>
            </div>
            <div id="stat-graph" data-stat-graph data-x-min="-10" data-x-max="10"
                 data-y-min="-1" data-y-max="3" data-x-label="Stat change"
                 data-y-label="Overall change"></div>
            </body></html>
            """;

    @Test
    void histogramViewDrawsStackedBarsFromTheRows() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            change(page, "compare-view", "histogram");

            assertThat(result(page, "document.getElementById('compare-histogram').hidden")).isEqualTo(false);
            assertThat(result(page, "document.getElementById('compare-list').hidden")).isEqualTo(true);
            assertThat(result(page, "document.querySelector('[data-compare-histogram] svg') !== null"))
                    .isEqualTo(true);
            assertThat(((Number) result(page,
                    "document.querySelectorAll('[data-compare-histogram] rect').length")).intValue())
                    .isGreaterThan(0);
            assertThat(result(page, "document.querySelector('[data-compare-histogram]').textContent"))
                    .asString().contains("Dps", "Survivability", "Spell Power");
        }
    }

    @Test
    void filterRedrawsTheHistogramWithOnlyTheMatchingStat() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            change(page, "compare-view", "histogram");
            type(page, "compare-filter", "spell");

            assertThat(((Number) result(page,
                    "document.querySelectorAll('[data-compare-histogram] rect').length")).intValue())
                    .isGreaterThan(0);
            assertThat(result(page, "document.querySelector('[data-compare-histogram]').textContent"))
                    .asString().contains("Spell Power").doesNotContain("Crit Rating", "Health");
        }
    }

    @Test
    void rowsViewHidesTheHistogramAgain() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            change(page, "compare-view", "histogram");
            change(page, "compare-view", "rows");

            assertThat(result(page, "document.getElementById('compare-list').hidden")).isEqualTo(false);
            assertThat(result(page, "document.getElementById('compare-histogram').hidden")).isEqualTo(true);
        }
    }

    @Test
    void statGraphDrawsOneLinePerStat() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript("""
                    var event = document.createEvent('Event');
                    event.initEvent('htmx:afterSwap', true, false);
                    document.dispatchEvent(event);
                    """);

            assertThat(result(page, "document.querySelector('[data-stat-graph] svg') !== null"))
                    .isEqualTo(true);
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] polyline').length"))
                    .isEqualTo(2);
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] circle').length"))
                    .isEqualTo(6);
            assertThat(result(page, "document.querySelector('[data-stat-graph]').textContent"))
                    .asString().contains("Hit Rating", "Crit Rating", "Stat change", "Overall change");
        }
    }


    @Test
    void legendIsTheLinePickerAndStaysVisibleWhenAllLinesAreHidden() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript("""
                    var event = document.createEvent('Event');
                    event.initEvent('htmx:afterSwap', true, false);
                    document.dispatchEvent(event);
                    """);
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] polyline').length"))
                    .isEqualTo(2);

            click(page, "[data-stat-legend-key=\"HitRating\"]");
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] polyline').length"))
                    .isEqualTo(1);
            assertThat(result(page, "document.querySelector('[data-stat-visible-count]').textContent"))
                    .isEqualTo("1");
            assertThat(result(page,
                    "document.querySelector('[data-stat-legend-key=\"HitRating\"]').classList.contains('is-hidden')"))
                    .isEqualTo(true);

            click(page, "[data-stat-legend-key=\"CritRating\"]");
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] polyline').length"))
                    .isEqualTo(0);
            assertThat(result(page, "document.querySelector('[data-stat-graph] .compare-graph-legend') !== null"))
                    .isEqualTo(true);
            assertThat(result(page, "document.querySelector('[data-stat-graph]').textContent"))
                    .asString().contains("No lines selected", "Hit Rating", "Crit Rating");
            assertThat(result(page, "document.querySelector('[data-stat-visible-count]').textContent"))
                    .isEqualTo("0");

            click(page, "[data-stat-legend-key=\"HitRating\"]");
            assertThat(result(page, "document.querySelectorAll('[data-stat-graph] polyline').length"))
                    .isEqualTo(1);
        }
    }

    private static void click(HtmlPage page, String selector) {
        page.executeJavaScript("""
                var item = document.querySelector('%s');
                var event = document.createEvent('Event');
                event.initEvent('click', true, true);
                item.dispatchEvent(event);
                """.formatted(selector.replace("'", "\\'")));
    }

    private static void change(HtmlPage page, String id, String value) {
        page.executeJavaScript("""
                var element = document.getElementById('%s');
                element.value = '%s';
                var event = document.createEvent('Event');
                event.initEvent('change', true, false);
                element.dispatchEvent(event);
                """.formatted(id, value));
    }

    private static void type(HtmlPage page, String id, String value) {
        page.executeJavaScript("""
                var element = document.getElementById('%s');
                element.value = '%s';
                var event = document.createEvent('Event');
                event.initEvent('input', true, false);
                element.dispatchEvent(event);
                """.formatted(id, value));
    }

    private static HtmlPage load(WebClient client) throws Exception {
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(true);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        MockWebConnection connection = new MockWebConnection();
        connection.setDefaultResponse(PAGE, 200, "UTF-8", "text/html");
        client.setWebConnection(connection);
        HtmlPage page = client.getPage("http://localhost/test");
        page.executeJavaScript(APP_JS);
        return page;
    }

    private static Object result(HtmlPage page, String script) {
        return page.executeJavaScript(script).getJavaScriptResult();
    }

    private static final String APP_JS = loadAppJs();

    private static String loadAppJs() {
        try (InputStream in = ComparisonGraphBrowserTest.class.getResourceAsStream("/static/js/app.js")) {
            assertThat(in).as("app.js on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load app.js", e);
        }
    }
}
