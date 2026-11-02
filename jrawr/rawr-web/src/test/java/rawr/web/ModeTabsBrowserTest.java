package rawr.web;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-browser checks for the Build/Optimize mode tabs: arrow/Home/End navigation must move
 * the selected tab, expose it to assistive technology and switch the visible screen.
 */
class ModeTabsBrowserTest {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html><body>
            <nav role="tablist" aria-label="Mode">
              <button id="mode-tab-build" class="mode-tab" type="button" role="tab" aria-selected="true"
                      aria-controls="build-mode" tabindex="0" data-mode="build">Build</button>
              <button id="mode-tab-optimize" class="mode-tab" type="button" role="tab" aria-selected="false"
                      aria-controls="optimize-mode" tabindex="-1" data-mode="optimize">Optimize</button>
            </nav>
            <div id="build-mode" role="tabpanel"></div>
            <div id="optimize-mode" role="tabpanel" hidden></div>
            <script>
            window.htmx = {ajax: function () { window.ajaxCount = (window.ajaxCount || 0) + 1; }};
            </script>
            </body></html>
            """;

    @Test
    void arrowKeysMoveTheModeSelectionAndPanel() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);

            page.executeJavaScript("""
                    var build = document.getElementById('mode-tab-build');
                    build.focus();
                    build.dispatchEvent(new KeyboardEvent('keydown',
                            {key: 'ArrowRight', bubbles: true}));
                    """);

            assertThat(result(page, "document.getElementById('optimize-mode').hidden"))
                    .isEqualTo(false);
            assertThat(result(page, "document.getElementById('build-mode').hidden"))
                    .isEqualTo(true);
            assertThat(result(page, "document.getElementById('mode-tab-optimize')"
                    + ".getAttribute('aria-selected')")).isEqualTo("true");
            assertThat(result(page, "document.getElementById('mode-tab-build')"
                    + ".getAttribute('aria-selected')")).isEqualTo("false");
            assertThat(result(page, "document.getElementById('mode-tab-optimize').tabIndex"))
                    .isEqualTo(0);
            assertThat(result(page, "document.getElementById('mode-tab-build').tabIndex"))
                    .isEqualTo(-1);
            assertThat(result(page, "document.body.getAttribute('data-mode')"))
                    .isEqualTo("optimize");
        }
    }

    @Test
    void homeAndEndJumpToTheFirstAndLastMode() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);

            page.executeJavaScript("""
                    var optimize = document.getElementById('mode-tab-optimize');
                    optimize.focus();
                    optimize.dispatchEvent(new KeyboardEvent('keydown',
                            {key: 'Home', bubbles: true}));
                    """);
            assertThat(result(page, "document.getElementById('build-mode').hidden"))
                    .isEqualTo(false);

            page.executeJavaScript("""
                    var build = document.getElementById('mode-tab-build');
                    build.focus();
                    build.dispatchEvent(new KeyboardEvent('keydown',
                            {key: 'End', bubbles: true}));
                    """);
            assertThat(result(page, "document.getElementById('optimize-mode').hidden"))
                    .isEqualTo(false);
        }
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
        try (InputStream in = ModeTabsBrowserTest.class.getResourceAsStream("/static/js/app.js")) {
            assertThat(in).as("app.js on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load app.js", e);
        }
    }
}
