package rawr.web;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-browser checks for the availability manager's small JS interactions: the checkbox
 * updates the row form's hidden value and submits it, and the import mode toggles the destructive
 * Replace confirmation. htmx is intentionally absent; the test polyfills requestSubmit so the
 * handler's submit path is observable.
 */
class AvailabilityBrowserTest {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html><body>
            <form id="row-form" action="/submit">
              <input id="available-hidden" type="hidden" name="available" value="false">
              <label><input id="availability-item-1" type="checkbox"
                            class="availability-checkbox"></label>
            </form>
            <form id="import-form" action="/import" hx-post="/import"
                  hx-confirm="Replace the entire custom item pool with these entries?">
              <textarea name="entries"></textarea>
              <select id="import-mode" name="mode" data-availability-import-mode>
                <option value="replace">Replace</option>
                <option value="append">Append</option>
              </select>
            </form>
            <script>
            window.submitCount = 0;
            document.addEventListener('submit', function (event) {
              event.preventDefault();
              window.submitCount++;
            });
            HTMLFormElement.prototype.requestSubmit = function () {
              var event = document.createEvent('Event');
              event.initEvent('submit', true, true);
              this.dispatchEvent(event);
            };
            </script>
            </body></html>
            """;

    @Test
    void checkboxChangeUpdatesHiddenValueAndSubmitsTheRowForm() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript("""
                    var box = document.getElementById('availability-item-1');
                    box.checked = true;
                    var event = document.createEvent('Event');
                    event.initEvent('change', true, false);
                    box.dispatchEvent(event);
                    """);

            assertThat(result(page, "document.getElementById('available-hidden').value"))
                    .isEqualTo("true");
            assertThat(result(page, "window.submitCount")).isEqualTo(1);
        }
    }

    @Test
    void importModeTogglesTheReplaceConfirmation() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);

            page.executeJavaScript("""
                    var mode = document.getElementById('import-mode');
                    mode.value = 'append';
                    var event = document.createEvent('Event');
                    event.initEvent('change', true, false);
                    mode.dispatchEvent(event);
                    """);
            assertThat(result(page, "document.getElementById('import-form').hasAttribute('hx-confirm')"))
                    .isEqualTo(false);

            page.executeJavaScript("""
                    mode.value = 'replace';
                    var event2 = document.createEvent('Event');
                    event2.initEvent('change', true, false);
                    mode.dispatchEvent(event2);
                    """);
            assertThat(result(page, "document.getElementById('import-form').hasAttribute('hx-confirm')"))
                    .isEqualTo(true);
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
        try (InputStream in = AvailabilityBrowserTest.class.getResourceAsStream("/static/js/app.js")) {
            assertThat(in).as("app.js on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load app.js", e);
        }
    }
}
