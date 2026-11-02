package rawr.web;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-browser checks for the Item editor's small JS layer: dynamic effect/stat rows, row
 * removal, focus, source selection, list-row selection, Clear, first-error focus and the
 * Add-result custom-copy shortcut. htmx is polyfilled; the actual requests are covered by the
 * controller tests.
 */
class ItemEditorBrowserTest {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html><body>
            <div class="modal item-editor-modal">
              <div class="tabs">
                <button type="button" data-tab="edit">Edit items</button>
              </div>
              <div id="item-editor-edit-body"></div>
            </div>

            <form class="ie-custom-form">
              <label data-field-key="name"><input name="name"/></label>
              <div id="custom-effect-rows"></div>
              <button type="button" data-add-effect>Add effect</button>
              <div id="custom-stat-rows"></div>
              <button type="button" data-add-row="stat">Add stat</button>
            </form>

            <div class="ie-source-form">
              <select data-source-preset>
                <option value="wotlkdb">wotlkdb</option>
                <option value="custom">Custom</option>
              </select>
              <input type="text" data-source-template disabled/>
            </div>

            <div class="ie-list">
              <button type="button" class="ie-list-row">One</button>
              <button type="button" class="ie-list-row">Two</button>
              <button type="button" class="ie-list-row">Three</button>
            </div>

            <form id="item-editor-add-form">
              <textarea name="items"></textarea>
              <button type="button" data-clear-add>Clear</button>
            </form>
            <div id="item-editor-add-results"></div>
            <span id="item-editor-summary"></span>

            <button type="button" data-open-custom data-copy-source="42">Create custom copy</button>

            <div id="item-picker">
              <input type="hidden" name="slot" value="Head"/>
            </div>
            <div id="item-results"></div>

            <template id="custom-effect-template">
              <div class="ie-effect" data-effect-index="__idx__">
                <select name="effect__idx__Trigger"></select>
                <div class="ie-stat-rows"></div>
                <button type="button" data-add-effect-stat>Add effect stat</button>
                <button type="button" data-remove-effect>Remove effect</button>
              </div>
            </template>
            <template id="custom-stat-options-template">
              <option value="">(none)</option>
              <option value="Intellect">Intellect</option>
            </template>
            <template id="custom-effect-stat-template">
              <div class="ie-stat-row">
                <select name="effect__idx__StatName" aria-label="Effect stat"
                        data-stat-select></select>
                <input type="number" name="effect__idx__StatValue" aria-label="Effect value"/>
                <button type="button" data-remove-row>Remove</button>
              </div>
            </template>
            <template id="custom-stat-template">
              <div class="ie-stat-row">
                <select name="statName" aria-label="Stat" data-stat-select></select>
                <input type="number" name="statValue" aria-label="Value"/>
                <button type="button" data-remove-row>Remove</button>
              </div>
            </template>

            <script>
            window.formChanges = 0;
            window.tabClicks = 0;
            document.querySelector('.ie-custom-form').addEventListener('change', function () {
                window.formChanges++;
            });
            document.querySelector('.item-editor-modal .tabs [data-tab="edit"]').addEventListener('click', function () {
                window.tabClicks++;
            });
            window.htmx = {
                ajax: function (method, url, context) {
                    window.ajaxMethod = method;
                    window.ajaxUrl = url;
                    window.ajaxTarget = context && context.target;
                    return {
                        then: function (callback) {
                            callback();
                        }
                    };
                }
            };
            </script>
            </body></html>
            """;

    @Test
    void effectAndStatRowsCanBeAddedRemovedAndFocusTheNewControl() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            click(page, "[data-add-effect]");
            assertThat(String.valueOf(result(page, "document.querySelectorAll('.ie-effect').length")))
                    .isEqualTo("1");
            assertThat(result(page, "document.activeElement.name")).isEqualTo("effect0Trigger");
            assertThat(result(page, "window.formChanges > 0")).isEqualTo(true);

            click(page, "[data-add-effect-stat]");
            assertThat(String.valueOf(result(page,
                    "document.querySelectorAll('[name=\"effect0StatName\"]').length")))
                    .isEqualTo("1");
            assertThat(result(page, "document.activeElement.name")).isEqualTo("effect0StatName");

            click(page, ".ie-effect [data-remove-row]");
            assertThat(String.valueOf(result(page,
                    "document.querySelectorAll('[name=\"effect0StatName\"]').length")))
                    .isEqualTo("0");
            click(page, "[data-remove-effect]");
            assertThat(String.valueOf(result(page, "document.querySelectorAll('.ie-effect').length")))
                    .isEqualTo("0");

            click(page, "[data-add-row=\"stat\"]");
            assertThat(String.valueOf(result(page,
                    "document.querySelectorAll('#custom-stat-rows [name=\"statName\"]').length")))
                    .isEqualTo("1");
            assertThat(String.valueOf(result(page,
                    "document.querySelector('#custom-stat-rows [name=\"statName\"]').options.length")))
                    .isEqualTo("2");
            assertThat(result(page, "document.activeElement.name")).isEqualTo("statName");
            click(page, "#custom-stat-rows [data-remove-row]");
            assertThat(String.valueOf(result(page,
                    "document.querySelectorAll('#custom-stat-rows .ie-stat-row').length")))
                    .isEqualTo("0");
        }
    }

    @Test
    void customSourceSelectionEnablesTheTemplateInput() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            page.executeJavaScript("""
                    var select = document.querySelector('[data-source-preset]');
                    select.value = 'custom';
                    var event = document.createEvent('Event');
                    event.initEvent('change', true, true);
                    select.dispatchEvent(event);
                    """);
            assertThat(result(page, "document.querySelector('[data-source-template]').disabled"))
                    .isEqualTo(false);

            page.executeJavaScript("""
                    var select = document.querySelector('[data-source-preset]');
                    select.value = 'wotlkdb';
                    var event = document.createEvent('Event');
                    event.initEvent('change', true, true);
                    select.dispatchEvent(event);
                    """);
            assertThat(result(page, "document.querySelector('[data-source-template]').disabled"))
                    .isEqualTo(true);
            assertThat(result(page, "document.querySelector('[data-source-template]').value"))
                    .isEqualTo("");
        }
    }

    @Test
    void listRowsTrackTheSelectedItem() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            click(page, ".ie-list-row:nth-child(2)");
            assertThat(String.valueOf(result(page,
                    "document.querySelectorAll('.ie-list-row.is-selected').length")))
                    .isEqualTo("1");
            assertThat(result(page, "document.querySelector('.ie-list-row:nth-child(2)').classList.contains('is-selected')"))
                    .isEqualTo(true);
        }
    }

    @Test
    void clearResetsTheAddFormResultsAndSummary() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            page.executeJavaScript("""
                    document.querySelector('#item-editor-add-form textarea').value = '50731';
                    document.querySelector('#item-editor-add-results').innerHTML = 'result';
                    document.querySelector('#item-editor-summary').textContent = '1 added';
                    """);
            click(page, "[data-clear-add]");
            assertThat(result(page, "document.querySelector('#item-editor-add-form textarea').value"))
                    .isEqualTo("");
            assertThat(String.valueOf(result(page,
                    "document.querySelector('#item-editor-add-results').textContent")))
                    .contains("Paste item IDs and press Fetch items.");
            assertThat(result(page, "document.querySelector('#item-editor-summary').textContent"))
                    .isEqualTo("No items yet");
        }
    }

    @Test
    void theFirstInvalidFieldIsFocusedAfterAnErrorRender() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            page.executeJavaScript("""
                    document.querySelector('.ie-custom-form').setAttribute('data-first-error', 'name');
                    var event = document.createEvent('Event');
                    event.initEvent('htmx:afterSettle', true, true);
                    document.dispatchEvent(event);
                    """);
            assertThat(result(page, "document.activeElement.name")).isEqualTo("name");
        }
    }

    @Test
    void aCacheChangeEventRefreshesTheOpenPicker() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            page.executeJavaScript("""
                    var event = document.createEvent('CustomEvent');
                    event.initCustomEvent('jrawr:item-cache-changed', true, true, {id: 123});
                    document.body.dispatchEvent(event);
                    """);
            assertThat(String.valueOf(result(page, "window.ajaxMethod"))).isEqualTo("GET");
            assertThat(String.valueOf(result(page, "window.ajaxUrl")))
                    .isEqualTo("/items?slot=Head&added=123");
            assertThat(String.valueOf(result(page, "window.ajaxTarget"))).isEqualTo("#item-results");
        }
    }

    @Test
    void theAddResultCustomCopyShortcutLoadsTheFormAndSwitchesTabs() throws Exception {
        try (WebClient client = new WebClient()) {
            HtmlPage page = load(client);
            page.executeJavaScript(ITEM_EDITOR_JS);

            click(page, "[data-open-custom]");
            assertThat(result(page, "window.ajaxMethod")).isEqualTo("GET");
            assertThat(result(page, "window.ajaxUrl")).isEqualTo("/tools/item-editor/custom?source=42");
            assertThat(result(page, "window.ajaxTarget")).isEqualTo("#item-editor-edit-body");
            assertThat(result(page, "window.tabClicks")).isEqualTo(1);
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

    private static void click(HtmlPage page, String selector) {
        page.executeJavaScript("document.querySelector('" + selector + "').click()");
    }

    private static Object result(HtmlPage page, String script) {
        return page.executeJavaScript(script).getJavaScriptResult();
    }

    private static final String ITEM_EDITOR_JS = loadItemEditorJs();

    private static String loadItemEditorJs() {
        try (InputStream in = ItemEditorBrowserTest.class.getResourceAsStream(
                "/static/js/item-editor.js")) {
            assertThat(in).as("item-editor.js on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load item-editor.js", e);
        }
    }
}
