package rawr.web;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-browser checks for the palette engine: a stored palette is applied on boot, typing a
 * hex value changes the live CSS custom property, presets replace the palette and Reset clears it.
 */
class AppearanceBrowserTest {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html><body>
            <div data-theme-editor>
              <input type="color" data-theme-token="--accent" value="#e6bb63">
              <input type="text" data-theme-hex="--accent" value="#e6bb63">
              <input type="text" data-theme-font="--font-body" value="sans-serif">
              <input type="text" data-theme-name>
              <button type="button" data-theme-save>Save</button>
              <p data-theme-library-status></p>
              <div data-theme-library></div>
              <button type="button" data-theme-preset="default">Default</button>
              <button type="button" data-theme-preset="midnight">Midnight</button>
              <button type="button" data-theme-reset>Reset</button>
              <span data-theme-status></span>
            </div>
            </body></html>
            """;

    @Test
    void storedPaletteIsAppliedWhenTheEngineBoots() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript("localStorage.setItem('jrawr.palette.v1',"
                    + " JSON.stringify({'--accent': '#123456', '--font-body': 'Georgia, serif'}));");
            page.executeJavaScript(THEME_JS);

            assertThat(result(page, "window.JRawrTheme.palette()['--accent']"))
                    .isEqualTo("#123456");
            assertThat(result(page, "window.JRawrTheme.palette()['--font-body']"))
                    .isEqualTo("Georgia, serif");
        }
    }

    @Test
    void editingHexAppliesTheColourAndPersistsIt() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("""
                    var input = document.querySelector('[data-theme-hex="--accent"]');
                    input.value = '#112233';
                    var event = document.createEvent('Event');
                    event.initEvent('input', true, true);
                    input.dispatchEvent(event);
                    """);

            assertThat(result(page, "window.JRawrTheme.palette()['--accent']"))
                    .isEqualTo("#112233");
            assertThat(result(page, "JSON.parse(localStorage.getItem('jrawr.palette.v1'))['--accent']"))
                    .isEqualTo("#112233");
            assertThat(result(page, "document.querySelector('[data-theme-token=\"--accent\"]').value"))
                    .isEqualTo("#112233");
        }
    }

    @Test
    void presetsReplaceThePaletteAndResetClearsIt() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("document.querySelector('[data-theme-preset=\"midnight\"]').click()");

            assertThat(result(page, "window.JRawrTheme.palette()['--bg']"))
                    .isEqualTo("#0a0f16");
            assertThat(result(page, "JSON.parse(localStorage.getItem('jrawr.palette.v1'))['--accent']"))
                    .isEqualTo("#7fb0ff");
            assertThat(result(page,
                    "document.querySelector('[data-theme-preset=\"midnight\"]').getAttribute('aria-pressed')"))
                    .isEqualTo("true");

            page.executeJavaScript("document.querySelector('[data-theme-reset]').click()");
            assertThat(result(page, "String(Object.keys(window.JRawrTheme.palette()).length)"))
                    .isEqualTo("0");
            assertThat(result(page, "String(Object.keys(JSON.parse(localStorage.getItem('jrawr.palette.v1'))).length)"))
                    .isEqualTo("0");
            assertThat(result(page,
                    "document.querySelector('[data-theme-preset=\"default\"]').getAttribute('aria-pressed')"))
                    .isEqualTo("true");
        }
    }


    @Test
    void savingCreatesANamedThemeAndRendersTheLibrary() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("""
                    document.querySelector('[data-theme-name]').value = 'Frostfire';
                    document.querySelector('[data-theme-save]').click();
                    """);

            assertThat(result(page, "String(window.JRawrTheme.themes().length)"))
                    .isEqualTo("1");
            assertThat(result(page, "window.JRawrTheme.themes()[0].name"))
                    .isEqualTo("Frostfire");
            assertThat(result(page, "window.JRawrTheme.themes()[0].palette['--accent']"))
                    .isEqualTo("#e6bb63");
            assertThat(result(page, "window.JRawrTheme.palette()['--accent']"))
                    .isEqualTo("#e6bb63");
            assertThat(result(page, "document.querySelector('.theme-library-name').textContent"))
                    .isEqualTo("Frostfire");
            assertThat(result(page, "document.querySelector('.theme-library-item.is-active') !== null"))
                    .isEqualTo(true);
            assertThat((String) result(page,
                    "document.querySelector('[data-theme-library-status]').textContent"))
                    .contains("Saved");
        }
    }

    @Test
    void applyingACustomThemeRestoresItsCapturedPalette() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("""
                    document.querySelector('[data-theme-name]').value = 'Frostfire';
                    document.querySelector('[data-theme-save]').click();
                    var input = document.querySelector('[data-theme-hex="--accent"]');
                    input.value = '#112233';
                    var event = document.createEvent('Event');
                    event.initEvent('input', true, true);
                    input.dispatchEvent(event);
                    document.querySelector('[data-theme-apply-theme]').click();
                    """);

            assertThat(result(page, "window.JRawrTheme.palette()['--accent']"))
                    .isEqualTo("#e6bb63");
            assertThat(result(page, "document.querySelector('[data-theme-name]').value"))
                    .isEqualTo("Frostfire");
        }
    }

    @Test
    void savingTheSameNameOverwritesTheTheme() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("""
                    document.querySelector('[data-theme-name]').value = 'Frostfire';
                    document.querySelector('[data-theme-save]').click();
                    var input = document.querySelector('[data-theme-hex="--accent"]');
                    input.value = '#112233';
                    var event = document.createEvent('Event');
                    event.initEvent('input', true, true);
                    input.dispatchEvent(event);
                    document.querySelector('[data-theme-save]').click();
                    """);

            assertThat(result(page, "String(window.JRawrTheme.themes().length)"))
                    .isEqualTo("1");
            assertThat(result(page, "window.JRawrTheme.themes()[0].palette['--accent']"))
                    .isEqualTo("#112233");
        }
    }

    @Test
    void deleteRequiresAConfirmationClick() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(THEME_JS);
            page.executeJavaScript("""
                    document.querySelector('[data-theme-name]').value = 'Frostfire';
                    document.querySelector('[data-theme-save]').click();
                    document.querySelector('[data-theme-delete-theme]').click();
                    """);

            assertThat(result(page, "String(window.JRawrTheme.themes().length)"))
                    .isEqualTo("1");
            assertThat(result(page, "document.querySelector('[data-theme-delete-theme]').textContent"))
                    .isEqualTo("Confirm");

            page.executeJavaScript("document.querySelector('[data-theme-delete-theme]').click()");
            assertThat(result(page, "String(window.JRawrTheme.themes().length)"))
                    .isEqualTo("0");
            assertThat(result(page, "document.querySelector('.theme-library-empty') !== null"))
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
        return client.getPage("http://localhost/test");
    }

    private static Object result(HtmlPage page, String script) {
        return page.executeJavaScript(script).getJavaScriptResult();
    }

    private static final String THEME_JS = loadThemeJs();

    private static String loadThemeJs() {
        try (InputStream in = AppearanceBrowserTest.class.getResourceAsStream("/static/js/theme.js")) {
            assertThat(in).as("theme.js on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load theme.js", e);
        }
    }
}
