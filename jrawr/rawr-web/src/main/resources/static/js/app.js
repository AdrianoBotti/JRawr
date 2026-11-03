'use strict';

/*
 * JRawr web UI helpers: modal tab switching/closing and the settings icon-URL example hint.
 * Item tooltips are anchored with CSS (.item-icon-wrap:hover / :focus-within .item-tooltip) and
 * their content is lazy-loaded by htmx on first hover/focus, so no positioning script is needed.
 */

/*
 * Modal focus management: remember the opener, move focus into a newly opened modal, trap Tab in
 * the topmost dialog (including the custom confirm dialog), and restore focus when it closes.
 */
(function () {
    var returnFocus = new WeakMap();
    var wasEmpty = new WeakMap();

    function visible(element) {
        return !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length);
    }

    function focusables(root) {
        var selector = 'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), '
                + 'select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
        return Array.prototype.slice.call(root.querySelectorAll(selector)).filter(function (element) {
            return visible(element) && element.getAttribute('aria-hidden') !== 'true';
        });
    }

    function activeDialog() {
        var confirm = document.getElementById('confirm-modal');
        if (confirm && confirm.childElementCount > 0) {
            return confirm;
        }
        var roots = document.querySelectorAll('.modal-root');
        for (var i = roots.length - 1; i >= 0; i--) {
            if (roots[i].childElementCount > 0) {
                return roots[i];
            }
        }
        return null;
    }

    function focusFirst(root) {
        var items = focusables(root);
        if (items.length) {
            items[0].focus({ preventScroll: true });
        } else {
            root.setAttribute('tabindex', '-1');
            root.focus({ preventScroll: true });
        }
    }

    function remember(root) {
        var active = document.activeElement;
        if (active && !root.contains(active)) {
            returnFocus.set(root, active);
        }
    }

    function restore(root) {
        var target = returnFocus.get(root);
        returnFocus.delete(root);
        if (target && document.body.contains(target)) {
            target.focus({ preventScroll: true });
        }
    }

    window.jrawrRestoreModalFocus = restore;

    document.addEventListener('toggle', function (event) {
        if (event.target.matches && event.target.matches('.filters-defaults')) {
            storageSet(DEFAULTS_KEY, event.target.open ? 'true' : 'false');
        }
    }, true);

    document.addEventListener('htmx:beforeSwap', function (event) {
        var target = event.detail && event.detail.target;
        if (target && target.id
                && (target.id === 'modal' || target.id === 'availability-modal'
                || target.id === 'specs-modal')) {
            wasEmpty.set(target, target.childElementCount === 0);
        }
    });

    document.addEventListener('htmx:afterSwap', function (event) {
        var target = event.detail && event.detail.target;
        if (!target || (target.id !== 'modal' && target.id !== 'availability-modal'
                && target.id !== 'specs-modal')) {
            return;
        }
        if (wasEmpty.get(target)) {
            remember(target);
            focusFirst(target);
        }
        wasEmpty.delete(target);
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.modal-root').forEach(function (root) {
            if (root.childElementCount > 0) {
                focusFirst(root);
            }
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Tab' || event.altKey || event.ctrlKey || event.metaKey) {
            return;
        }
        var dialog = activeDialog();
        if (!dialog) {
            return;
        }
        var items = focusables(dialog);
        if (!items.length) {
            event.preventDefault();
            return;
        }
        var first = items[0];
        var last = items[items.length - 1];
        var active = document.activeElement;
        if (!dialog.contains(active)) {
            event.preventDefault();
            (event.shiftKey ? last : first).focus({ preventScroll: true });
        } else if (event.shiftKey && active === first) {
            event.preventDefault();
            last.focus({ preventScroll: true });
        } else if (!event.shiftKey && active === last) {
            event.preventDefault();
            first.focus({ preventScroll: true });
        }
    });
})();

/* Modal and optimizer tab switching, and closing modals (modals are swapped into #modal). */
document.addEventListener('click', function (event) {
    var tab = event.target.closest ? event.target.closest('[data-tab]') : null;
    if (tab) {
        var scope = tab.closest('.modal') || tab.closest('.optimize-form');
        if (scope) {
            scope.querySelectorAll('[data-tab]').forEach(function (button) {
                var selected = button === tab;
                button.classList.toggle('is-active', selected);
                button.setAttribute('aria-selected', selected ? 'true' : 'false');
            });
            scope.querySelectorAll('[data-tab-panel]').forEach(function (panel) {
                panel.hidden = panel.getAttribute('data-tab-panel') !== tab.getAttribute('data-tab');
            });
        }
        return;
    }
    var closer = event.target.closest ? event.target.closest('[data-modal-close]') : null;
    if (closer) {
        var root = closer.closest('.modal-root');
        if (root) {
            if (window.jrawrRestoreModalFocus) window.jrawrRestoreModalFocus(root);
            root.innerHTML = '';
        }
    }
});

/*
 * Build/Optimize screens: the top-bar tabs switch modes without a page load. The optimizer is
 * fetched the first time Optimize is entered and then kept mounted, so its form, live progress
 * and results survive switching back to Build.
 */
(function () {
    var current = 'build';
    var optimizeLoaded = false;

    function modeTabs() {
        return Array.prototype.slice.call(document.querySelectorAll('.mode-tab'))
                .filter(function (tab) {
                    return tab.getAttribute('data-mode') !== null;
                });
    }

    function syncModeTabs(mode) {
        modeTabs().forEach(function (tab) {
            var on = tab.getAttribute('data-mode') === mode;
            tab.classList.toggle('is-active', on);
            tab.setAttribute('aria-selected', on ? 'true' : 'false');
            tab.tabIndex = on ? 0 : -1;
        });
    }

    function showMode(mode) {
        var build = document.getElementById('build-mode');
        var optimize = document.getElementById('optimize-mode');
        if (!build || !optimize) return;
        var isOptimize = mode === 'optimize';
        build.hidden = isOptimize;
        optimize.hidden = !isOptimize;
        syncModeTabs(mode);
        if (isOptimize && !optimizeLoaded) {
            optimizeLoaded = true;
            htmx.ajax('GET', '/character/optimize/live',
                    {target: '#optimize-mode', swap: 'innerHTML'});
        }
        if (isOptimize) {
            var form = document.querySelector('.optimize-form');
            if (form) form.dispatchEvent(new Event('change', {bubbles: true}));
        }
        if (!isOptimize && current === 'optimize') {
            // The charts were hidden while optimizing; re-measure them on the way back.
            window.setTimeout(function () {
                window.dispatchEvent(new Event('resize'));
            }, 0);
        }
        current = mode;
        document.body.setAttribute('data-mode', mode);
    }

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest ? event.target.closest('[data-mode]') : null;
        if (!trigger) return;
        var mode = trigger.getAttribute('data-mode');
        if (mode !== 'build' && mode !== 'optimize') return;
        showMode(mode);
    });

    document.addEventListener('keydown', function (event) {
        var tab = event.target;
        if (!tab || !tab.classList || !tab.classList.contains('mode-tab')
                || event.altKey || event.ctrlKey || event.metaKey) return;
        var keys = { ArrowLeft: -1, ArrowRight: 1, Home: 'first', End: 'last' };
        if (!(event.key in keys)) return;
        var tabs = modeTabs();
        var index = tabs.indexOf(tab);
        if (index < 0) return;
        var next = keys[event.key] === 'first' ? 0
                : keys[event.key] === 'last' ? tabs.length - 1
                : (index + keys[event.key] + tabs.length) % tabs.length;
        event.preventDefault();
        showMode(tabs[next].getAttribute('data-mode'));
        tabs[next].focus();
    });
})();

/*
 * Arrow/Home/End navigation between the tabs of one tablist.
 */
document.addEventListener('keydown', function (event) {
    var tab = event.target.closest ? event.target.closest('[data-tab]') : null;
    if (!tab || event.altKey || event.ctrlKey || event.metaKey) return;
    var keys = { ArrowLeft: -1, ArrowRight: 1, Home: 'first', End: 'last' };
    if (!(event.key in keys)) return;
    var list = tab.closest('[role="tablist"]');
    if (!list) return;
    var tabs = Array.prototype.slice.call(list.querySelectorAll('[data-tab]'));
    var index = tabs.indexOf(tab);
    if (index < 0) return;
    var next = keys[event.key] === 'first' ? 0
            : keys[event.key] === 'last' ? tabs.length - 1
            : (index + keys[event.key] + tabs.length) % tabs.length;
    event.preventDefault();
    tabs[next].focus();
    tabs[next].click();
});

/*
 * Open-character file picker: choosing a file is the action, so submit the import form as soon
 * as the browser has a file. The server redirects back to the editor (or the import error page).
 */
(function () {
    document.addEventListener('change', function (event) {
        var input = event.target.closest ? event.target.closest('#open-file') : null;
        if (!input) return;
        var name = document.getElementById('open-file-name');
        if (name) {
            name.textContent = input.files && input.files.length
                    ? input.files[0].name : '';
        }
        if (!input.files || !input.files.length || !input.form) return;
        if (input.form.requestSubmit) {
            input.form.requestSubmit();
        } else {
            input.form.submit();
        }
    });
})();

/*
 * Item/enchant/gem picker dismissal: clicking outside closes it, as does Escape. Clicks on the
 * slot controls are left alone so htmx can replace the open picker with the new one.
 */
(function () {
    function closePicker() {
        var picker = document.getElementById('item-picker');
        if (picker) {
            picker.innerHTML = '';
        }
    }

    document.addEventListener('click', function (event) {
        var picker = document.getElementById('item-picker');
        if (!picker || picker.innerHTML.trim() === '') {
            return;
        }
        if (event.target.closest && event.target.closest('[data-close-picker]')) {
            closePicker();
            return;
        }
        if (picker.contains(event.target)) {
            return;
        }
        if (event.target.closest && event.target.closest('[hx-target="#item-picker"]')) {
            return;
        }
        closePicker();
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closePicker();
        }
    });
})();

/*
 * Floating item/enchant/gem tooltips: the picker and the comparison modal scroll, so their
 * tooltips are position: fixed and placed next to the hovered/focused icon, flipping/clamping
 * inside the viewport instead of being clipped by the scroller. The compare tooltip is
 * repositioned once its lazy htmx content arrives because its height is only known then.
 */
(function () {
    function positionFloatingTooltip(wrap) {
        if (!wrap.closest || (!wrap.closest('#item-picker') && !wrap.closest('.charts-card')
                && !wrap.closest('.availability-modal') && !wrap.closest('.slot'))) {
            return;
        }
        var tip = wrap.querySelector('.item-tooltip');
        if (!tip) {
            return;
        }
        var rect = wrap.getBoundingClientRect();
        // Estimate in rem so the flip/clamp logic matches the scaled tooltip (max-width: 20rem).
        var rem = parseFloat(window.getComputedStyle(document.documentElement).fontSize) || 16;
        var gap = 0.75 * rem;
        var edge = 0.5 * rem;
        var width = Math.min(20 * rem, window.innerWidth - rem);

        // .charts-card uses layout containment to keep its clipped candidate rows out of the
        // document height. That makes the card the containing block for its fixed tooltips, so
        // position them in the card's coordinate space and clamp them to its visible box.
        var card = tip.offsetParent;
        var containedByCard = card && card.classList && card.classList.contains('charts-card');
        var boundsLeft = 0;
        var boundsTop = 0;
        var boundsRight = window.innerWidth;
        var boundsBottom = window.innerHeight;
        if (containedByCard) {
            var cardRect = card.getBoundingClientRect();
            var cardStyle = window.getComputedStyle(card);
            boundsLeft = cardRect.left + (parseFloat(cardStyle.borderLeftWidth) || 0);
            boundsTop = cardRect.top + (parseFloat(cardStyle.borderTopWidth) || 0);
            boundsRight = cardRect.right - (parseFloat(cardStyle.borderRightWidth) || 0);
            boundsBottom = cardRect.bottom - (parseFloat(cardStyle.borderBottomWidth) || 0);
        }

        var left = rect.right + gap;
        if (left + width > boundsRight - edge) {
            left = rect.left - width - gap;
        }
        if (left < boundsLeft + edge) {
            left = boundsLeft + edge;
        }
        var height = tip.getBoundingClientRect().height;
        var top = rect.top;
        if (height > 0 && top + height > boundsBottom - edge) {
            var above = rect.top - height - edge;
            top = above >= boundsTop + edge ? above : Math.max(boundsTop + edge, boundsBottom - height - edge);
        } else if (height === 0 && top + 5 * rem > boundsBottom) {
            top = Math.max(boundsTop + edge, boundsBottom - 16.25 * rem);
        }
        if (containedByCard) {
            left -= boundsLeft;
            top -= boundsTop;
        }
        tip.style.position = 'fixed';
        tip.style.left = left + 'px';
        tip.style.top = top + 'px';
        tip.style.right = 'auto';
        tip.style.bottom = 'auto';
    }

    function positionFromEvent(event) {
        var target = event.target;
        if (!target || !target.closest) {
            return;
        }
        var wrap = target.closest('.item-icon-wrap');
        if (wrap) {
            positionFloatingTooltip(wrap);
        }
    }

    document.addEventListener('mouseover', positionFromEvent);
    document.addEventListener('focusin', positionFromEvent);
    document.addEventListener('scroll', function () {
        positionFloatingTooltipFromHover();
    }, true);
    window.addEventListener('resize', positionFloatingTooltipFromHover);
    document.addEventListener('htmx:afterSwap', function (event) {
        var target = event.target;
        if (target && target.classList && target.classList.contains('item-tooltip')) {
            var wrap = target.closest('.item-icon-wrap');
            if (wrap) {
                positionFloatingTooltip(wrap);
            }
        }
    });

    function positionFloatingTooltipFromHover() {
        var wrap = document.querySelector(
                '#item-picker .item-icon-wrap:hover, .charts-card .item-icon-wrap:hover, '
                + '.availability-modal .item-icon-wrap:hover');
        if (wrap) {
            positionFloatingTooltip(wrap);
        }
    }
})();

/* Stats widget: open/close the advanced detail dialog. */
(function () {
    document.addEventListener('click', function (event) {
        var opener = event.target.closest ? event.target.closest('[data-open-advanced-stats]') : null;
        if (opener) {
            var dialog = document.getElementById('stats-advanced-dialog');
            if (dialog) {
                if (typeof dialog.showModal === 'function') {
                    dialog.showModal();
                } else {
                    dialog.setAttribute('open', 'open');
                }
            }
            return;
        }
        var closer = event.target.closest ? event.target.closest('[data-close-advanced-stats]') : null;
        if (closer) {
            var openDialog = closer.closest('dialog');
            if (openDialog) {
                openDialog.close();
            }
        }
    });

    document.addEventListener('click', function (event) {
        if (event.target && event.target.tagName === 'DIALOG') {
            event.target.close();
        }
    });
})();

/*
 * Unsaved-changes indicator: the File menu dot. The server renders it on page load;
 * htmx swaps do not reload the page, so this hook keeps them in sync with the session state:
 * - HX-Redirect: the browser navigates right after -> hide; the next page render is authoritative.
 * - direct save (/character/save): hide only when the saved/no-changes toast came back (the server
 *   cleared the dirty flag); an error toast or the Save As prompt leaves the flag visible.
 * - the remaining mutating editor endpoints: the server marked the session dirty -> show.
 */
(function () {
    var MUTATING_PATHS = {
        '/character/slot': true,
        '/character/slot/instance': true,
        '/character/slot/enchant': true,
        '/character/slot/gem': true,
        '/character/slot/remove': true,
        '/character/buff': true,
        '/character/talents': true,
        '/character/talents/adjust': true,
        '/character/talents/reset': true,
        '/character/buffs/reset': true,
        '/character/glyphs/toggle': true,
        '/character/meta': true,
        '/character/optimize/apply': true,
        '/character/compare/availability': true,
        '/character/calculation-options': true,
        '/character/options': true,
        '/character/specs/apply': true
    };

    // Build edits that change the ratings: the persistent chart reloads after a short debounce.
    // Availability edits are excluded (they only toggle the diamonds) and library-only spec edits
    // do not touch the character.
    var CHART_PATHS = {
        '/character/slot': true,
        '/character/slot/instance': true,
        '/character/slot/enchant': true,
        '/character/slot/gem': true,
        '/character/slot/remove': true,
        '/character/buff': true,
        '/character/talents': true,
        '/character/talents/adjust': true,
        '/character/talents/reset': true,
        '/character/buffs/reset': true,
        '/character/glyphs/toggle': true,
        '/character/glyphs/reset': true,
        '/character/meta': true,
        '/character/optimize/apply': true,
        '/character/calculation-options': true,
        '/character/options': true,
        '/character/specs/apply': true,
        // Filters are global, so they do not dirty the character, but they change the item
        // candidate set used by the charts. Refresh the persistent pane after each filter edit.
        '/character/item-filters/enabled': true,
        '/character/item-filters/node': true,
        '/character/item-filters/add': true,
        '/character/item-filters/delete': true,
        '/character/item-filters/other': true,
        '/character/item-filters/types': true,
        '/character/gemming-templates/top-gemmings': true
    };

    var chartRefreshTimer = null;

    function scheduleChartRefresh() {
        if (chartRefreshTimer) {
            clearTimeout(chartRefreshTimer);
        }
        chartRefreshTimer = setTimeout(function () {
            chartRefreshTimer = null;
            document.body.dispatchEvent(new CustomEvent('jrawrRefreshCharts'));
        }, 600);
    }

    function setDirty(dirty) {
        var targets = document.querySelectorAll('.menu-dot');
        for (var i = 0; i < targets.length; i++) {
            targets[i].classList.toggle('is-hidden', !dirty);
        }
    }

    document.body.addEventListener('jrawrDirty', function () {
        setDirty(true);
    });

    document.addEventListener('htmx:afterRequest', function (event) {
        var detail = event.detail;
        if (!detail || !detail.successful) {
            return;
        }
        var xhr = detail.xhr;
        if (xhr && xhr.getResponseHeader && xhr.getResponseHeader('HX-Redirect')) {
            setDirty(false);
            return;
        }
        var path = detail.pathInfo && detail.pathInfo.requestPath ? detail.pathInfo.requestPath : '';
        path = path.split('?')[0];
        if (path === '/character/save') {
            var body = xhr && xhr.responseText ? xhr.responseText : '';
            if (body.indexOf('data-toast="saved"') !== -1
                    || body.indexOf('data-toast="no-changes"') !== -1) {
                setDirty(false);
            }
            return;
        }
        if (MUTATING_PATHS[path]) {
            setDirty(true);
        }
        if (CHART_PATHS[path]) {
            scheduleChartRefresh();
        }
    });
})();

/*
 * Chart pane shortcut: the per-slot chart icon updates the persistent pane. When the pane is
 * stacked under the build (narrow layout), scroll it into view after the swap; on wide layouts the
 * pane is already visible.
 */
(function () {
    var pending = false;

    document.addEventListener('click', function (event) {
        if (event.target.closest && event.target.closest('[data-chart-scroll]')) {
            pending = true;
        }
    });

    document.addEventListener('htmx:afterSwap', function (event) {
        if (!pending) {
            return;
        }
        var target = event.detail && event.detail.target;
        if (!target || target.id !== 'charts-panel') {
            return;
        }
        pending = false;
        if (window.matchMedia && window.matchMedia('(max-width: 1500px)').matches) {
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    });
})();

/*
 * Gemming templates: the Add button opens the creation form in the fixed footer, so it stays
 * visible while the template list scrolls. Creating a template replaces the panel; scroll the
 * list to the resulting custom row.
 */
(function () {
    function createForm() {
        return document.querySelector('[data-gemming-create-form]');
    }

    function createButton() {
        return document.querySelector('[data-gemming-create-toggle]');
    }

    document.addEventListener('click', function (event) {
        var toggle = event.target.closest ? event.target.closest('[data-gemming-create-toggle]') : null;
        if (toggle) {
            var form = createForm();
            if (form) {
                form.hidden = false;
                toggle.hidden = true;
                var first = form.querySelector('select');
                if (first) first.focus();
            }
            return;
        }
        var cancel = event.target.closest ? event.target.closest('[data-gemming-create-cancel]') : null;
        if (cancel) {
            var openForm = createForm();
            var button = createButton();
            if (openForm) openForm.hidden = true;
            if (button) button.hidden = false;
        }
    });

    var pendingScroll = null;
    var pendingBottom = false;

    document.addEventListener('htmx:beforeRequest', function (event) {
        var elt = event.detail && event.detail.elt;
        if (elt && elt.closest && elt.closest('[data-gemming-preserve-scroll]')) {
            var rows = document.querySelector('.gemming-rows');
            if (rows) pendingScroll = rows.scrollTop;
        }
    });

    document.addEventListener('htmx:afterRequest', function (event) {
        var detail = event.detail;
        if (!detail || !detail.successful || !detail.elt) return;
        if (detail.elt.closest && detail.elt.closest('[data-gemming-add]')) {
            pendingBottom = true;
        }
    });

    document.addEventListener('htmx:afterSettle', function () {
        var rows = document.querySelector('.gemming-rows');
        if (rows) {
            if (pendingBottom) {
                rows.scrollTop = rows.scrollHeight;
            } else if (pendingScroll !== null) {
                rows.scrollTop = pendingScroll;
            }
        }
        pendingScroll = null;
        pendingBottom = false;
        var form = createForm();
        var button = createButton();
        if (form) form.hidden = true;
        if (button) button.hidden = false;
    });
})();

/*
 * Gemming-template filters: one select per socket color. A value restricts the list to templates
 * using that gem; All (0) means no restriction for that color. The filters are client-side so the
 * list re-renders keep the selection, and the selection survives panel swaps in sessionStorage.
 */
(function () {
    var KEY = 'jrawr.gemmingFilters.v1';

    function selects() {
        return Array.prototype.slice.call(document.querySelectorAll('[data-gemming-filter]'));
    }

    function read() {
        try {
            return JSON.parse(window.sessionStorage.getItem(KEY) || '{}') || {};
        } catch (error) {
            return {};
        }
    }

    function write(state) {
        try {
            window.sessionStorage.setItem(KEY, JSON.stringify(state));
        } catch (error) {
            /* storage can be unavailable in private browsing */
        }
    }

    function apply() {
        var filters = selects();
        if (!filters.length) return;
        var rows = document.querySelectorAll('.gemming-rows .gemming-row');
        var any = false;
        Array.prototype.forEach.call(rows, function (row) {
            var visible = true;
            filters.forEach(function (select) {
                var value = select.value;
                if (value && value !== '0'
                        && row.getAttribute('data-gem-' + select.getAttribute('data-gemming-filter'))
                                !== value) {
                    visible = false;
                }
            });
            row.classList.toggle('is-hidden', !visible);
            if (visible) any = true;
        });
        var empty = document.querySelector('[data-gemming-filter-empty]');
        if (empty) empty.hidden = !(rows.length && !any);
    }

    function restore() {
        var filters = selects();
        if (!filters.length) return;
        var state = read();
        filters.forEach(function (select) {
            var value = state[select.getAttribute('data-gemming-filter')];
            if (value !== undefined && value !== null) select.value = String(value);
        });
        apply();
    }

    document.addEventListener('change', function (event) {
        if (!event.target.matches || !event.target.matches('[data-gemming-filter]')) return;
        var state = {};
        selects().forEach(function (select) {
            state[select.getAttribute('data-gemming-filter')] = select.value;
        });
        write(state);
        apply();
    });

    document.addEventListener('htmx:afterSettle', function () {
        if (selects().length) restore();
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restore);
    } else {
        restore();
    }
})();

/*
 * Confirmations: htmx's native window.confirm is replaced with the same card/modal chrome used by
 * the rest of the app. The htmx:confirm event is cancelled, the styled dialog is shown, and the
 * pending request is issued only when the user confirms.
 */
(function () {
    function root() {
        var element = document.getElementById('confirm-modal');
        if (!element) {
            element = document.createElement('div');
            element.id = 'confirm-modal';
            document.body.appendChild(element);
        }
        return element;
    }

    var previousFocus = null;

    function close() {
        var element = document.getElementById('confirm-modal');
        if (element) {
            element.innerHTML = '';
        }
        if (previousFocus && document.body.contains(previousFocus)) {
            previousFocus.focus({ preventScroll: true });
        }
        previousFocus = null;
    }

    function show(question, onConfirm) {
        previousFocus = document.activeElement;
        var element = root();
        element.innerHTML = '';
        var backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop confirm-backdrop';
        backdrop.innerHTML = ''
            + '<div class="modal card" role="alertdialog" aria-modal="true" '
            + 'aria-labelledby="confirm-title" aria-describedby="confirm-message">'
            + '<header class="modal-head"><h2 id="confirm-title">Confirm</h2></header>'
            + '<div class="modal-body"><p id="confirm-message"></p></div>'
            + '<div class="modal-actions">'
            + '<button class="btn" type="button" data-confirm-cancel>Cancel</button>'
            + '<button class="btn btn-primary" type="button" data-confirm-ok>Confirm</button>'
            + '</div></div>';
        backdrop.querySelector('#confirm-message').textContent = question;
        element.appendChild(backdrop);

        backdrop.addEventListener('click', function (event) {
            if (event.target.closest('[data-confirm-ok]')) {
                event.preventDefault();
                close();
                onConfirm();
            } else if (event.target.closest('[data-confirm-cancel]') || event.target === backdrop) {
                event.preventDefault();
                close();
            }
        });

        var ok = backdrop.querySelector('[data-confirm-ok]');
        if (ok) {
            ok.focus();
        }
    }

    document.addEventListener('htmx:confirm', function (event) {
        if (!event.detail || !event.detail.question) {
            return;
        }
        event.preventDefault();
        show(event.detail.question, function () {
            event.detail.issueRequest(true);
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var element = document.getElementById('confirm-modal');
        if (element && element.childElementCount > 0) {
            event.preventDefault();
            close();
        }
    });
})();

/*
 * Settings modal: live example URL under the Site base URL field. The host layout rules are
 * rendered by rawr.web.ItemIconService into data-icon-layouts so the map has one source of truth
 * (this mirrors ItemIconService.layoutTemplate/resolve logic for the hint only).
 */

function iconLayoutTemplate(rules, host) {
    for (var i = 0; i < rules.rules.length; i++) {
        var rule = rules.rules[i];
        if (host === rule.host || host.endsWith('.' + rule.host)) {
            return rule.template;
        }
    }
    return rules.fallback;
}

function resolveIconExample(rules, base, template, icon) {
    if (template && /^https?:\/\//i.test(template) && template.indexOf('{icon}') !== -1) {
        return template.replace('{icon}', icon);
    }
    var match = /^(https?):\/\/([^/?#]+)/i.exec(base);
    if (!match) {
        return '';
    }
    var authority = match[2];
    var host = authority;
    if (host.charAt(0) === '[') {
        host = host.slice(0, host.indexOf(']') + 1);
    } else if (host.indexOf(':') !== -1) {
        host = host.slice(0, host.indexOf(':'));
    }
    // Mirror ItemIconService.normalizeBaseUrl for the hint: lowercase scheme/host, keep the port.
    var origin = match[1].toLowerCase() + '://' + host.toLowerCase() + authority.slice(host.length);
    return iconLayoutTemplate(rules, host.toLowerCase())
        .replace('{base}', origin)
        .replace('{icon}', icon);
}

function updateIconHint(event) {
    var form = event.target.closest ? event.target.closest('#settings-form') : null;
    var hint = form ? form.querySelector('#icon-base-hint') : null;
    if (!form || !hint) {
        return;
    }
    var rules;
    try {
        rules = JSON.parse(form.getAttribute('data-icon-layouts'));
    } catch (error) {
        rules = null;
    }
    var baseInput = form.querySelector('#icon-base-url');
    var templateInput = form.querySelector('#icon-template');
    if (!rules || !baseInput || !templateInput) {
        return;
    }
    var icon = form.getAttribute('data-icon-example') || 'inv_helmet_150black';
    var example = resolveIconExample(rules, baseInput.value.trim(), templateInput.value.trim(), icon);
    hint.textContent = example ? 'Example icon URL: ' + example : 'Choose an icon source.';
}

document.addEventListener('input', updateIconHint);
document.addEventListener('change', updateIconHint);

/*
 * Charts pane maximize: hide the build column and let the charts fill the workspace. The state is a
 * browser preference stored in localStorage; the toggle lives in the persistent charts card header.
 */
(function () {
    var KEY = 'jrawr.chartsMaximized';

    function workspace() {
        return document.querySelector('.workspace');
    }

    function stored() {
        try {
            return window.localStorage.getItem(KEY) === 'true';
        } catch (error) {
            return false;
        }
    }

    function apply(on) {
        var ws = workspace();
        if (!ws) return;
        ws.classList.toggle('charts-maximized', on);
        var button = document.querySelector('[data-chart-maximize]');
        if (button) {
            button.textContent = on ? 'Restore' : 'Maximize';
            button.setAttribute('aria-label', on ? 'Restore the build column' : 'Maximize charts');
            button.setAttribute('title', on ? 'Restore the build column' : 'Expand the charts pane');
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        apply(stored());
    });
    // The charts card is lazy-loaded, so re-apply the stored state after swaps to label the button.
    document.addEventListener('htmx:afterSwap', function () {
        apply(stored());
    });
    document.addEventListener('click', function (event) {
        var button = event.target.closest ? event.target.closest('[data-chart-maximize]') : null;
        if (!button) return;
        var ws = workspace();
        var on = !(ws && ws.classList.contains('charts-maximized'));
        try {
            window.localStorage.setItem(KEY, on ? 'true' : 'false');
        } catch (error) {
            /* private mode: the toggle still works for this page */
        }
        apply(on);
        // The chart SVG is sized to its container; redraw after the workspace reflows.
        window.setTimeout(function () {
            window.dispatchEvent(new Event('resize'));
        }, 0);
    });
})();

/* Close any open menu (Character dropdown, Advanced build menu) when clicking outside it or
   after choosing a menu item. */
document.addEventListener('click', function (event) {
    document.querySelectorAll('details.menu[open]').forEach(function (menu) {
        if (!menu.contains(event.target)) {
            menu.open = false;
            return;
        }
        if (event.target.closest('.menu-panel .menu-item')) {
            menu.open = false;
        }
    });
});

/*
 * Buff group expansion: remember manual open/close state by group name so an htmx refresh (after
 * toggling a buff or swapping the paper doll) does not re-open a group the user closed. A click on
 * the summary runs before the native toggle, so the inverse of the current state is the new state.
 */
(function () {
    var openByGroup = new Map();

    function buffGroups(button) {
        var panel = button.closest('#buff-panel');
        return panel ? panel.querySelectorAll('.buff-group[data-buff-group]') : [];
    }

    function updateLabels() {
        document.querySelectorAll('[data-buff-toggle-all]').forEach(function (button) {
            var anyOpen = false;
            buffGroups(button).forEach(function (group) {
                if (group.open) {
                    anyOpen = true;
                }
            });
            button.textContent = anyOpen ? 'Collapse all' : 'Expand all';
        });
    }

    function restore() {
        document.querySelectorAll('.buff-group[data-buff-group]').forEach(function (group) {
            var stored = openByGroup.get(group.getAttribute('data-buff-group'));
            if (stored !== undefined) {
                group.open = stored;
            }
        });
        updateLabels();
    }

    document.addEventListener('click', function (event) {
        var toggleAll = event.target.closest ? event.target.closest('[data-buff-toggle-all]') : null;
        if (toggleAll) {
            var groups = buffGroups(toggleAll);
            var anyOpen = false;
            groups.forEach(function (group) {
                if (group.open) {
                    anyOpen = true;
                }
            });
            var next = !anyOpen;
            groups.forEach(function (group) {
                group.open = next;
                var name = group.getAttribute('data-buff-group');
                if (name) {
                    openByGroup.set(name, next);
                }
            });
            updateLabels();
            return;
        }

        var summary = event.target.closest ? event.target.closest('summary') : null;
        if (!summary) {
            return;
        }
        var group = summary.parentElement;
        if (!group || !group.classList.contains('buff-group')) {
            return;
        }
        var name = group.getAttribute('data-buff-group');
        if (name) {
            openByGroup.set(name, !group.open);
        }
        // The native toggle happens after this click handler; update the label afterwards.
        setTimeout(updateLabels, 0);
    });

    document.addEventListener('htmx:afterSwap', restore);

    // Locked buffs are visually disabled but must stay hoverable for their tooltip; block the click
    // during the capture phase so htmx never issues the toggle request.
    document.addEventListener('click', function (event) {
        var chip = event.target.closest ? event.target.closest('[data-buff]') : null;
        if (chip && chip.getAttribute('data-buff-locked') === 'true') {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);

    updateLabels();
})();

/* Close the item picker when clicking outside it (or opening another slot). */
document.addEventListener('click', function (event) {
    var picker = document.getElementById('item-picker');
    if (!picker || picker.childElementCount === 0) return;
    if (picker.contains(event.target)) return;
    if (event.target.closest('.slot-main')) return;
    picker.innerHTML = '';
});

/*
 * Optimizer requirement rows: clone the inert <template> and let each row be removed.
 */
(function () {
    document.addEventListener('click', function (event) {
        var add = event.target.closest ? event.target.closest('[data-add-requirement]') : null;
        if (add) {
            var template = document.getElementById('requirement-row-template');
            var container = document.getElementById('optimizer-requirements');
            if (template && container) {
                container.appendChild(template.content.cloneNode(true));
            }
            return;
        }
        var remove = event.target.closest ? event.target.closest('[data-remove-requirement]') : null;
        if (remove) {
            var row = remove.closest('.requirement-row');
            if (row) {
                row.remove();
            }
        }
    });
})();

/*
 * Availability manager checkboxes: a native checkbox updates the form's hidden available value and
 * submits the row form through htmx (browsers omit unchecked checkboxes from form data).
 */
(function () {
    document.addEventListener('change', function (event) {
        var box = event.target.closest ? event.target.closest('.availability-checkbox') : null;
        if (!box) return;
        var form = box.closest('form');
        if (!form) return;
        var hidden = form.querySelector('input[name="available"]');
        if (hidden) hidden.value = box.checked ? 'true' : 'false';
        if (form.requestSubmit) form.requestSubmit();
        else form.submit();
    });
})();

/*
 * Availability import confirmation: replacing the pool is destructive, appending is not. The form
 * gets an hx-confirm attribute only while the Replace mode is selected.
 */
(function () {
    document.addEventListener('change', function (event) {
        var mode = event.target.closest
                ? event.target.closest('[data-availability-import-mode]') : null;
        if (!mode) return;
        var form = mode.closest('form');
        if (!form) return;
        if (mode.value === 'replace') {
            form.setAttribute('hx-confirm',
                    'Replace the entire custom item pool with these entries?');
        } else {
            form.removeAttribute('hx-confirm');
        }
    });
})();

/*
 * Optimizer tab memory: starting or cancelling a run replaces the whole optimizer panel, and the
 * server-rendered form always starts on Gear & pool. Remember the active tab and restore it after
 * the swap so a talent-only run does not bounce back to the gear tab.
 */
(function () {
    var activeTab = 'gear';

    function activate(form, name) {
        var tab = form.querySelector('.optimize-tabs [data-tab="' + name + '"]');
        if (!tab) return;
        form.querySelectorAll('[data-tab]').forEach(function (button) {
            var selected = button === tab;
            button.classList.toggle('is-active', selected);
            button.setAttribute('aria-selected', selected ? 'true' : 'false');
        });
        form.querySelectorAll('[data-tab-panel]').forEach(function (panel) {
            panel.hidden = panel.getAttribute('data-tab-panel') !== name;
        });
    }

    document.addEventListener('click', function (event) {
        var tab = event.target.closest ? event.target.closest('.optimize-tabs [data-tab]') : null;
        if (tab) activeTab = tab.getAttribute('data-tab');
    });

    document.addEventListener('htmx:afterSettle', function () {
        var form = document.querySelector('.optimize-form');
        if (form) activate(form, activeTab);
    });
})();

/*
 * Talent mode and the talent-only run: the modes are exclusive, and the talent-only subflow button
 * is enabled only when a talent mode is selected (and no run is active). The server validates the
 * same rule as a safety net.
 */
(function () {
    function syncTalentOnly(form) {
        var button = form.querySelector('#talents-only-button');
        if (!button) return;
        var mode = form.querySelector('input[name="talentMode"]:checked');
        var busy = button.getAttribute('data-busy') === 'true';
        button.disabled = busy || !mode || mode.value === 'none';
    }

    function syncAll() {
        document.querySelectorAll('.optimize-form').forEach(syncTalentOnly);
    }

    document.addEventListener('change', function (event) {
        var form = event.target.closest ? event.target.closest('.optimize-form') : null;
        if (!form) return;
        if (event.target.matches && event.target.matches('input[name="talentMode"]')) {
            syncTalentOnly(form);
        }
    });
    document.addEventListener('click', function (event) {
        var submit = event.target.closest ? event.target.closest('button[type="submit"]') : null;
        if (!submit) return;
        var form = submit.closest('.optimize-form');
        if (!form) return;
        var hidden = form.querySelector('input[name="talentsOnly"][type="hidden"]');
        if (!hidden) return;
        hidden.value = submit.id === 'talents-only-button' ? 'true' : 'false';
    });
    document.addEventListener('htmx:afterSwap', syncAll);
    syncAll();
})();

/*
 * Optimizer setup summary: the tabbed form has one Start action, so a highlighted footer strip
 * shows the live "Current setup" as labelled chips (target, search, slots, pool, optional
 * talent/consumable searches and constraints). Re-sync after htmx swaps the panel and when
 * constraint rows are added or removed.
 */
(function () {
    function selectedText(form, name) {
        var field = form.querySelector('[name="' + name + '"]');
        if (!field || field.tagName !== 'SELECT') return '';
        var option = field.options[field.selectedIndex];
        return option ? option.textContent.trim() : '';
    }

    function isChecked(form, name) {
        return form.querySelector('input[name="' + name + '"]:checked') !== null;
    }

    function summaryParts(form) {
        var parts = [];
        function add(label, value) {
            if (value) parts.push([label, value]);
        }
        add('Target', selectedText(form, 'calculation') || 'Overall rating');
        var search = [];
        var method = selectedText(form, 'method');
        if (method) search.push(method);
        var range = form.querySelector('input[type="range"][data-thoroughness-values]');
        var effortLabels = form.querySelectorAll('.thoroughness-labels span');
        if (range && effortLabels.length) {
            var index = Math.max(0, Math.min(effortLabels.length - 1, Number(range.value) - 1));
            search.push(effortLabels[index].textContent.trim());
        }
        add('Search', search.join(' \u00b7 '));
        var slots = form.querySelectorAll('input[name="slot"]');
        var selected = form.querySelectorAll('input[name="slot"]:checked');
        if (slots.length) add('Slots', selected.length + '/' + slots.length);
        add('Pool', selectedText(form, 'poolMode'));
        var poolCap = selectedText(form, 'itemsPerSlot');
        if (poolCap && poolCap !== 'All items') add('Pool cap', poolCap);
        var talentMode = form.querySelector('input[name="talentMode"]:checked');
        if (talentMode && talentMode.value === 'mutate') add('Talents & glyphs', 'mutate');
        else if (talentMode && talentMode.value === 'spec') add('Talents & glyphs', 'best spec');
        var consumables = [];
        if (isChecked(form, 'optimizeFood')) consumables.push('food');
        if (isChecked(form, 'optimizeElixirs')) consumables.push('elixirs');
        var mixologyInfo = form.querySelector('[data-mixology-info]');
        var alchemist = mixologyInfo && mixologyInfo.getAttribute('data-alchemist') === 'true';
        if (isChecked(form, 'optimizeElixirs') && alchemist) {
            consumables.push('mixology');
        }
        add('Consumables', consumables.join(', '));
        var constraints = form.querySelectorAll('.requirement-row').length;
        if (constraints) add('Constraints', constraints + (constraints === 1 ? ' rule' : ' rules'));
        return parts;
    }

    function syncSummary(form) {
        var target = form.querySelector('[data-optimize-setup-summary]');
        if (!target) return;
        target.textContent = '';
        summaryParts(form).forEach(function (part) {
            var chip = document.createElement('span');
            chip.className = 'optimize-setup-part';
            var label = document.createElement('span');
            label.className = 'optimize-setup-key';
            label.textContent = part[0];
            var value = document.createElement('span');
            value.className = 'optimize-setup-value';
            value.textContent = part[1];
            chip.appendChild(label);
            chip.appendChild(value);
            target.appendChild(chip);
        });
    }

    function syncAllSummaries() {
        document.querySelectorAll('.optimize-form').forEach(syncSummary);
    }

    function summaryEvent(event) {
        var form = event.target.closest ? event.target.closest('.optimize-form') : null;
        if (form) syncSummary(form);
    }

    document.addEventListener('input', summaryEvent);
    document.addEventListener('change', summaryEvent);
    document.addEventListener('click', function (event) {
        if (event.target.closest
                && event.target.closest('[data-add-requirement], [data-remove-requirement]')) {
            window.setTimeout(syncAllSummaries, 0);
        }
    });
    document.addEventListener('htmx:afterSwap', syncAllSummaries);
    syncAllSummaries();
})();

/*
 * Mixology info: it follows the character's professions, including changes made after the optimizer
 * panel has been loaded. The server derives the flag again when a run starts.
 */
(function () {
    function isAlchemist() {
        var primary = document.querySelector('.identity-strip select[name="primaryProfession"]');
        var secondary = document.querySelector('.identity-strip select[name="secondaryProfession"]');
        return (primary && primary.value === 'Alchemy')
                || (secondary && secondary.value === 'Alchemy');
    }

    function sync() {
        var boxes = document.querySelectorAll('[data-mixology-info]');
        if (!boxes.length) return;
        var alchemist = isAlchemist();
        boxes.forEach(function (box) {
            box.setAttribute('data-alchemist', alchemist ? 'true' : 'false');
            var text = box.querySelector('[data-mixology-text]');
            if (text) {
                text.textContent = alchemist
                        ? 'Included automatically for Alchemy. Applies when an elixir or flask is chosen.'
                        : 'Not included: this character does not have Alchemy.';
            }
        });
        var form = document.querySelector('.optimize-form');
        if (form) {
            form.dispatchEvent(new Event('change', {bubbles: true}));
        }
    }

    document.addEventListener('change', function (event) {
        if (event.target.matches
                && event.target.matches('.identity-strip select[name="primaryProfession"],'
                        + ' .identity-strip select[name="secondaryProfession"]')) {
            sync();
        }
    });
    document.addEventListener('htmx:afterSettle', sync);
    sync();
})();

/*
 * Optimizer jobs: live progress over SSE with a status-polling fallback.
 * The panel is htmx-swapped into #modal; #optimize-live carries the job state so the stream is
 * (re)attached after every swap. Terminal events close the stream and refresh the panel; a
 * finished run is shown for review and is applied only when the user clicks "Load optimized".
 */
(function () {
    var stream = null;

    function liveEl() {
        return document.getElementById('optimize-live');
    }

    function closeStream() {
        if (stream) {
            stream.close();
            stream = null;
        }
    }

    function applyStatus(data) {
        var bar = document.getElementById('optimize-bar');
        var text = document.getElementById('optimize-progress-text');
        if (bar) bar.style.width = data.progress + '%';
        if (text) text.textContent = data.progress + '%';
    }

    function refreshPanel() {
        if (!liveEl()) return;
        htmx.ajax('GET', '/character/optimize/live',
                {target: '#optimize-live', swap: 'outerHTML'});
    }

    function attachStream() {
        var live = liveEl();
        if (!live) {
            closeStream();
            return;
        }
        var state = live.getAttribute('data-job-state');
        if (state !== 'queued' && state !== 'running') {
            closeStream();
            return;
        }
        if (stream) return;
        stream = new EventSource('/character/optimize/stream');
        stream.addEventListener('status', function (event) {
            try {
                applyStatus(JSON.parse(event.data));
            } catch (error) {
                /* polling fallback keeps the panel current */
            }
        });
        stream.addEventListener('done', function (event) {
            var data = null;
            try {
                data = JSON.parse(event.data);
            } catch (error) {
                data = null;
            }
            closeStream();
            refreshPanel();
        });
        stream.onerror = function () {
            closeStream();
        };
    }

    document.addEventListener('htmx:afterSwap', function () {
        attachStream();
    });

    document.addEventListener('htmx:responseError', function (event) {
        var error = document.getElementById('upgrade-request-error');
        if (!error) return;
        error.hidden = false;
        error.textContent = 'Upgrade request failed (HTTP '
                + (event.detail && event.detail.xhr ? event.detail.xhr.status : '?') + ').';
    });
    document.addEventListener('htmx:sendError', function () {
        var error = document.getElementById('upgrade-request-error');
        if (!error) return;
        error.hidden = false;
        error.textContent = 'Could not reach the server.';
    });

    document.addEventListener('click', function (event) {
        if (event.target.closest && event.target.closest('#modal [data-modal-close]')) {
            closeStream();
        }
    });
})();

/*
 * WoW-style talent planner: left-click learns, right-click (or Shift+click) unlearns. Glyph
 * slots and glyph list entries toggle through the same server-validated endpoints. A floating
 * tooltip shows the server-rendered .talent-tip content without being clipped by the modal.
 */
(function () {
    var tooltip = null;

    function tip() {
        if (!tooltip) {
            tooltip = document.createElement('div');
            tooltip.id = 'talent-tooltip';
            tooltip.className = 'talent-tooltip';
            document.body.appendChild(tooltip);
        }
        return tooltip;
    }

    function place(event) {
        if (!tooltip) return;
        var pad = 16;
        var rect = tooltip.getBoundingClientRect();
        var x = event.clientX + pad;
        var y = event.clientY + pad;
        if (x + rect.width > window.innerWidth - 8) {
            x = event.clientX - rect.width - pad;
        }
        if (y + rect.height > window.innerHeight - 8) {
            y = event.clientY - rect.height - pad;
        }
        tooltip.style.left = Math.max(8, x) + 'px';
        tooltip.style.top = Math.max(8, y) + 'px';
    }

    function show(event, host) {
        var source = host.querySelector('.talent-tip');
        if (!source) return;
        var element = tip();
        element.innerHTML = source.innerHTML;
        element.classList.add('is-visible');
        place(event);
    }

    function hide() {
        if (tooltip) {
            tooltip.classList.remove('is-visible');
        }
    }

    function post(path, values, target) {
        htmx.ajax('POST', path, {
            values: values,
            target: target,
            swap: 'outerHTML'
        });
    }

    document.addEventListener('mouseover', function (event) {
        var host = event.target.closest ? event.target.closest('[data-tip-host]') : null;
        if (host) show(event, host);
    });
    document.addEventListener('mousemove', function (event) {
        if (tooltip && tooltip.classList.contains('is-visible')) place(event);
    });
    document.addEventListener('mouseout', function (event) {
        var host = event.target.closest ? event.target.closest('[data-tip-host]') : null;
        if (!host) return;
        if (event.relatedTarget && host.contains(event.relatedTarget)) return;
        hide();
    });
    document.addEventListener('focusin', function (event) {
        var host = event.target.closest ? event.target.closest('[data-tip-host]') : null;
        if (!host) return;
        var rect = host.getBoundingClientRect();
        show({clientX: rect.left, clientY: rect.bottom}, host);
    });
    document.addEventListener('focusout', hide);
    document.addEventListener('scroll', hide, true);
    document.addEventListener('htmx:beforeSwap', hide);

    document.addEventListener('click', function (event) {
        var talent = event.target.closest ? event.target.closest('[data-talent-index]') : null;
        if (talent) {
            event.preventDefault();
            var index = talent.getAttribute('data-talent-index');
            var remove = event.shiftKey || event.altKey;
            if (remove) {
                if (talent.getAttribute('data-can-remove') === 'true') {
                    post('/character/talents/adjust', {index: index, delta: -1}, '#talent-panel');
                }
            } else if (talent.getAttribute('data-can-add') === 'true') {
                post('/character/talents/adjust', {index: index, delta: 1}, '#talent-panel');
            }
            return;
        }
        var glyph = event.target.closest ? event.target.closest('[data-glyph-index]') : null;
        if (glyph) {
            event.preventDefault();
            if (glyph.getAttribute('data-glyph-locked') === 'true') {
                return;
            }
            var glyphIndex = glyph.getAttribute('data-glyph-index');
            if (!glyphIndex) return;
            post('/character/glyphs/toggle', {index: glyphIndex}, '#glyph-panel');
        }
    });

    document.addEventListener('contextmenu', function (event) {
        var talent = event.target.closest ? event.target.closest('[data-talent-index]') : null;
        if (!talent) return;
        event.preventDefault();
        if (talent.getAttribute('data-can-remove') === 'true') {
            post('/character/talents/adjust', {
                index: talent.getAttribute('data-talent-index'),
                delta: -1
            }, '#talent-panel');
        }
    });
})();

/*
 * Paper-doll tabs: Gear / Talents / Glyphs. The active tab is kept client-side and restored after
 * htmx swaps (gear, model or options changes replace the whole paper-doll card).
 */
(function () {
    var active = 'gear';

    function activate(root, name) {
        root.querySelectorAll('[data-paperdoll-tab]').forEach(function (button) {
            var selected = button.getAttribute('data-paperdoll-tab') === name;
            button.classList.toggle('is-active', selected);
            button.setAttribute('aria-selected', selected ? 'true' : 'false');
        });
        root.querySelectorAll('[data-paperdoll-panel]').forEach(function (panel) {
            panel.hidden = panel.getAttribute('data-paperdoll-panel') !== name;
        });
    }

    document.addEventListener('click', function (event) {
        var tab = event.target.closest ? event.target.closest('[data-paperdoll-tab]') : null;
        if (!tab) return;
        event.preventDefault();
        active = tab.getAttribute('data-paperdoll-tab');
        var picker = document.getElementById('item-picker');
        if (picker) {
            picker.innerHTML = '';
        }
        var root = tab.closest('#paperdoll');
        if (root) activate(root, active);
    });

    document.addEventListener('htmx:afterSettle', function () {
        var root = document.getElementById('paperdoll');
        if (root) activate(root, active);
    });
})();

/*
 * Comparison chart: client-side sort and name filter over the server-rated rows. The chart is
 * rated once per slot/limit, so re-sorting never reruns the model. The availability toggle
 * event updates the other gemming rows of the same item in place.
 */
(function () {
    var statLineState = {};

    function rows() {
        return Array.prototype.slice.call(
            document.querySelectorAll('[data-compare-rows] .compare-row'));
    }

    function value(row, key) {
        if (key === 'name') {
            return (row.getAttribute('data-name') || '').toLowerCase();
        }
        if (key.indexOf('sub-') === 0) {
            var index = key.substring(4);
            var chip = row.querySelector('[data-sub-index="' + index + '"]');
            return chip ? parseFloat(chip.getAttribute('data-sub-value')) || 0 : 0;
        }
        return parseFloat(row.getAttribute('data-overall')) || 0;
    }

    function filterRows() {
        var input = document.querySelector('[data-compare-filter]');
        var needle = input ? input.value.trim().toLowerCase() : '';
        rows().forEach(function (row) {
            var name = (row.getAttribute('data-name') || '').toLowerCase();
            row.hidden = needle !== '' && name.indexOf(needle) < 0;
        });
        renderHistogram();
    }

    function graphXml(value) {
        return String(value).replace(/[&<>"']/g, function (character) {
            return {
                '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
            }[character];
        });
    }

    function graphNumber(value) {
        if (Math.abs(value - Math.round(value)) < 1e-6) return value.toFixed(0);
        var magnitude = Math.abs(value);
        if (magnitude >= 100) return value.toFixed(0);
        if (magnitude >= 10) return value.toFixed(1);
        return value.toFixed(2);
    }

    /*
     * Resolve a theme token for SVG presentation attributes, which cannot reliably read CSS
     * custom properties directly. The appearance editor overrides these on :root.
     */
    function themeValue(name, fallback) {
        try {
            var value = window.getComputedStyle(document.documentElement).getPropertyValue(name);
            return value && value.trim() ? value.trim() : fallback;
        } catch (error) {
            return fallback;
        }
    }

    function chartDimensions(graph, fallbackWidth, fallbackHeight) {
        var width = Math.round(graph.clientWidth || fallbackWidth);
        width = Math.max(420, width);
        var viewport = window.innerHeight || 800;
        var height = Math.round(viewport * 0.5);
        height = Math.max(280, Math.min(560, height));
        if (!height) height = fallbackHeight;
        return {width: width, height: height};
    }

    function renderHistogram() {
        var graph = document.querySelector('[data-compare-histogram]');
        if (!graph) return;
        var visible = rows().filter(function (row) {
            return !row.hidden;
        });
        if (!visible.length) {
            graph.innerHTML = '<p class="empty-state">No stats match the filter.</p>';
            return;
        }

        var labels = [];
        var overall = [];
        visible.forEach(function (row) {
            labels.push(row.getAttribute('data-name') || '');
            overall.push(parseFloat(row.getAttribute('data-overall')) || 0);
        });

        var series = [];
        var first = visible[0];
        Array.prototype.forEach.call(first.querySelectorAll('[data-sub-index]'), function (chip) {
            var label = chip.querySelector('.compare-subpoint-label');
            series.push({
                index: chip.getAttribute('data-sub-index'),
                label: label ? label.textContent : 'Sub-point',
                color: chip.style.color || '#e7b85a',
                values: []
            });
        });
        visible.forEach(function (row) {
            series.forEach(function (item) {
                var chip = row.querySelector('[data-sub-index="' + item.index + '"]');
                item.values.push(chip ? parseFloat(chip.getAttribute('data-sub-value')) || 0 : 0);
            });
        });

        var values = overall.slice();
        series.forEach(function (item) {
            values = values.concat(item.values);
        });
        var minimum = Math.min.apply(null, values.concat([0]));
        var maximum = Math.max.apply(null, values.concat([0]));
        if (maximum === minimum) maximum = minimum + 1;

        var size = chartDimensions(graph, 720, 330);
        var width = size.width;
        var height = size.height;
        var left = 52;
        var right = 20;
        var top = 22;
        var bottom = 92;
        var plotWidth = width - left - right;
        var plotHeight = height - top - bottom;
        function y(value) {
            return top + plotHeight - ((value - minimum) / (maximum - minimum)) * plotHeight;
        }
        var band = plotWidth / Math.max(1, labels.length);
        var barWidth = Math.min(46, band * 0.62);
        var axisText = themeValue('--text-muted', '#b8a58a');
        var axisDim = themeValue('--text-dim', '#a69174');
        var gridColor = themeValue('--border-soft', 'rgba(255,255,255,0.08)');
        var gridStrong = themeValue('--border-strong', 'rgba(255,255,255,0.28)');

        var svg = ['<svg class="compare-histogram-svg" viewBox="0 0 ' + width + ' ' + height
                + '" role="img" aria-label="Relative stat values histogram">'];
        for (var tick = 0; tick <= 4; tick++) {
            var tickValue = maximum - (maximum - minimum) * tick / 4;
            var tickY = y(tickValue);
            svg.push('<line x1="' + left + '" y1="' + tickY.toFixed(1) + '" x2="'
                    + (left + plotWidth) + '" y2="' + tickY.toFixed(1)
                    + '" stroke="' + graphXml(gridColor) + '" stroke-opacity="0.65" stroke-width="1"/>');
            svg.push('<text x="' + (left - 8) + '" y="' + (tickY + 3).toFixed(1)
                    + '" text-anchor="end" fill="' + graphXml(axisText) + '" font-size="10">'
                    + graphXml(graphNumber(tickValue)) + '</text>');
        }
        if (minimum < 0 && maximum > 0) {
            var zeroY = y(0);
            svg.push('<line x1="' + left + '" y1="' + zeroY.toFixed(1) + '" x2="'
                    + (left + plotWidth) + '" y2="' + zeroY.toFixed(1)
                    + '" stroke="' + graphXml(gridStrong) + '" stroke-opacity="0.9" stroke-width="1"/>');
        }

        labels.forEach(function (label, index) {
            var center = left + band * (index + 0.5);
            var labelX = center;
            var labelY = top + plotHeight + 14;
            svg.push('<text x="' + labelX.toFixed(1) + '" y="' + labelY.toFixed(1)
                    + '" text-anchor="end" fill="' + graphXml(axisDim) + '" font-size="9" transform="rotate(-38 '
                    + labelX.toFixed(1) + ' ' + labelY.toFixed(1) + ')">'
                    + graphXml(label) + '</text>');

            var baseline = y(0);
            var barX = center - barWidth / 2;
            var group = ['<g><title>' + graphXml(label + ': ' + graphNumber(overall[index]))
                    + '</title>'];
            var stackY = baseline;
            series.forEach(function (item) {
                var value = item.values[index];
                if (value <= 0) return;
                var segmentY = stackY - (y(0) - y(value));
                group.push('<rect x="' + barX.toFixed(1) + '" y="' + segmentY.toFixed(1)
                        + '" width="' + barWidth.toFixed(1) + '" height="'
                        + Math.max(0, stackY - segmentY).toFixed(1) + '" fill="'
                        + graphXml(item.color) + '" opacity="0.9"/>');
                stackY = segmentY;
            });
            stackY = baseline;
            series.forEach(function (item) {
                var value = item.values[index];
                if (value >= 0) return;
                var segmentY = stackY + (y(value) - y(0));
                group.push('<rect x="' + barX.toFixed(1) + '" y="' + stackY.toFixed(1)
                        + '" width="' + barWidth.toFixed(1) + '" height="'
                        + Math.max(0, segmentY - stackY).toFixed(1) + '" fill="'
                        + graphXml(item.color) + '" opacity="0.9"/>');
                stackY = segmentY;
            });
            group.push('</g>');
            svg.push(group.join(''));
        });
        svg.push('</svg>');

        var legend = ['<div class="compare-graph-legend">'];
        series.forEach(function (item) {
            legend.push('<span><i class="compare-graph-swatch" style="background:'
                    + graphXml(item.color) + '"></i>' + graphXml(item.label) + '</span>');
        });
        legend.push('</div>');
        graph.innerHTML = legend.join('') + svg.join('');
    }

    function renderStatGraph() {
        var graph = document.querySelector('[data-stat-graph]');
        if (!graph) return;
        var series = [];
        Array.prototype.forEach.call(document.querySelectorAll('[data-stat-graph-data] [data-stat-series]'), function (line) {
            var points = (line.getAttribute('data-points') || '').trim().split(/\s+/)
                    .filter(function (point) {
                        return point.indexOf(',') > 0;
                    })
                    .map(function (point) {
                        var parts = point.split(',');
                        return {x: parseFloat(parts[0]) || 0, y: parseFloat(parts[1]) || 0};
                    });
            series.push({
                key: line.getAttribute('data-key') || line.getAttribute('data-label') || '',
                label: line.getAttribute('data-label') || '',
                color: line.getAttribute('data-color') || '#e7b85a',
                visible: line.getAttribute('data-visible') !== 'false',
                points: points
            });
        });
        if (!series.length) {
            graph.innerHTML = '<p class="empty-state">Select at least one stat line.</p>';
            return;
        }

        series.forEach(function (item) {
            if (!(item.key in statLineState)) {
                statLineState[item.key] = item.visible;
            }
        });
        var visibleSeries = series.filter(function (item) {
            return statLineState[item.key] !== false;
        });
        updateStatLineCounts(visibleSeries.length, series.length);
        var legendHtml = statGraphLegend(series);
        if (!visibleSeries.length) {
            graph.innerHTML = legendHtml
                    + '<p class="empty-state">No lines selected. Click a legend item to show it.</p>';
            graph._statGraph = null;
            return;
        }

        // Render the legend first so the plot can fill the remaining charts height.
        graph.innerHTML = legendHtml;
        var legendNode = graph.querySelector('.compare-graph-legend');
        var legendHeight = legendNode ? legendNode.offsetHeight : 0;

        var xMin = parseFloat(graph.getAttribute('data-x-min')) || 0;
        var xMax = parseFloat(graph.getAttribute('data-x-max')) || 1;
        var yValues = [];
        visibleSeries.forEach(function (item) {
            item.points.forEach(function (point) {
                yValues.push(point.y);
            });
        });
        var yMin = Math.min.apply(null, yValues);
        var yMax = Math.max.apply(null, yValues);
        if (yMin === yMax) {
            yMin = Math.min(0, yMin);
            yMax = Math.max(1, yMax);
        }
        var padding = (yMax - yMin) * 0.05;
        yMin -= padding;
        yMax += padding;
        if (xMin === xMax) xMax = xMin + 1;

        var axisText = themeValue('--text-muted', '#b8a58a');
        var axisDim = themeValue('--text-dim', '#a69174');
        var gridColor = themeValue('--border-soft', 'rgba(255,255,255,0.08)');
        var gridStrong = themeValue('--border-strong', 'rgba(255,255,255,0.28)');

        var width = Math.max(420, Math.round(graph.clientWidth || 720));
        var height = 0;
        var main = graph.closest ? graph.closest('.charts-main') : null;
        if (main && main.clientHeight) {
            var mainStyle = window.getComputedStyle(main);
            var reserved = parseFloat(mainStyle.paddingTop) + parseFloat(mainStyle.paddingBottom);
            var kids = main.children;
            for (var i = 0; i < kids.length; i++) {
                var kid = kids[i];
                if (kid === graph || (kid.contains && kid.contains(graph))) continue;
                if (kid.offsetParent === null) continue;
                var kidStyle = window.getComputedStyle(kid);
                reserved += kid.offsetHeight + parseFloat(kidStyle.marginTop)
                        + parseFloat(kidStyle.marginBottom);
            }
            var graphStyle = window.getComputedStyle(graph);
            var legendStyle = legendNode ? window.getComputedStyle(legendNode) : null;
            var legendSpace = legendHeight + (legendStyle ? parseFloat(legendStyle.marginBottom) : 0);
            height = Math.round(main.clientHeight - reserved
                    - parseFloat(graphStyle.marginTop) - parseFloat(graphStyle.marginBottom)
                    - legendSpace);
        }
        if (height < 240) {
            var viewport = window.innerHeight || 800;
            height = Math.max(280, Math.min(900, Math.round(viewport * 0.55)));
        }
        var left = 58;
        var right = 22;
        var top = 20;
        var bottom = 58;
        var plotWidth = width - left - right;
        var plotHeight = height - top - bottom;
        function x(value) {
            return left + (value - xMin) / (xMax - xMin) * plotWidth;
        }
        function y(value) {
            return top + plotHeight - (value - yMin) / (yMax - yMin) * plotHeight;
        }

        var svg = ['<svg class="stat-graph-svg" viewBox="0 0 ' + width + ' ' + height
                + '" role="img" aria-label="Stat comparison line graph">'];
        for (var tick = 0; tick <= 4; tick++) {
            var tickValue = yMax - (yMax - yMin) * tick / 4;
            var tickY = y(tickValue);
            svg.push('<line x1="' + left + '" y1="' + tickY.toFixed(1) + '" x2="'
                    + (left + plotWidth) + '" y2="' + tickY.toFixed(1)
                    + '" stroke="' + graphXml(gridColor) + '" stroke-opacity="0.65" stroke-width="1"/>');
            svg.push('<text x="' + (left - 8) + '" y="' + (tickY + 3).toFixed(1)
                    + '" text-anchor="end" fill="' + graphXml(axisText) + '" font-size="10">'
                    + graphXml(graphNumber(tickValue)) + '</text>');
        }
        if (yMin < 0 && yMax > 0) {
            var zeroY = y(0);
            svg.push('<line x1="' + left + '" y1="' + zeroY.toFixed(1) + '" x2="'
                    + (left + plotWidth) + '" y2="' + zeroY.toFixed(1)
                    + '" stroke="' + graphXml(gridStrong) + '" stroke-opacity="0.9" stroke-width="1"/>');
        }
        for (var xTick = 0; xTick <= 4; xTick++) {
            var xValue = xMin + (xMax - xMin) * xTick / 4;
            var xPosition = x(xValue);
            svg.push('<line x1="' + xPosition.toFixed(1) + '" y1="' + top + '" x2="'
                    + xPosition.toFixed(1) + '" y2="' + (top + plotHeight)
                    + '" stroke="' + graphXml(gridColor) + '" stroke-opacity="0.35" stroke-width="1"/>');
            svg.push('<text x="' + xPosition.toFixed(1) + '" y="' + (top + plotHeight + 15)
                    + '" text-anchor="middle" fill="' + graphXml(axisText) + '" font-size="10">'
                    + graphXml(graphNumber(xValue)) + '</text>');
        }
        var xTitle = graph.getAttribute('data-x-label') || '';
        var yTitle = graph.getAttribute('data-y-label') || '';
        svg.push('<text x="' + (left + plotWidth / 2).toFixed(1) + '" y="'
                + (height - 12) + '" text-anchor="middle" fill="' + graphXml(axisDim) + '" font-size="10">'
                + graphXml(xTitle) + '</text>');
        svg.push('<text x="14" y="' + (top + plotHeight / 2).toFixed(1)
                + '" text-anchor="middle" fill="' + graphXml(axisDim) + '" font-size="10" transform="rotate(-90 14 '
                + (top + plotHeight / 2).toFixed(1) + ')">' + graphXml(yTitle) + '</text>');

        visibleSeries.forEach(function (item) {
            var points = item.points.map(function (point) {
                return x(point.x).toFixed(1) + ',' + y(point.y).toFixed(1);
            }).join(' ');
            svg.push('<polyline points="' + points + '" fill="none" stroke="'
                    + graphXml(item.color) + '" stroke-width="2.2" stroke-linejoin="round" '
                    + 'stroke-linecap="round" opacity="0.92"/>');
            item.points.forEach(function (point) {
                svg.push('<circle cx="' + x(point.x).toFixed(1) + '" cy="' + y(point.y).toFixed(1)
                        + '" r="2.2" fill="' + graphXml(item.color) + '"><title>'
                        + graphXml(item.label + ' @ ' + graphNumber(point.x) + ': '
                        + graphNumber(point.y)) + '</title></circle>');
            });
        });
        svg.push('<line data-stat-hover-line x1="' + left + '" y1="' + top + '" x2="' + left
                + '" y2="' + (top + plotHeight) + '" stroke="' + graphXml(axisText) + '" stroke-opacity="0.65" '
                + 'stroke-width="1" visibility="hidden"/>');
        svg.push('</svg>');

        graph.innerHTML = legendHtml + svg.join('')
                + '<div class="stat-graph-tooltip" data-stat-tooltip hidden></div>';
        graph._statGraph = {
            series: visibleSeries,
            xMin: xMin, xMax: xMax, yMin: yMin, yMax: yMax,
            width: width, height: height, left: left, top: top,
            plotWidth: plotWidth, plotHeight: plotHeight
        };
        bindStatGraphHover(graph);
    }

    function bindStatGraphHover(graph) {
        if (graph._statHoverBound) return;
        graph._statHoverBound = true;
        graph.addEventListener('mousemove', function (event) {
            var state = graph._statGraph;
            var svg = graph.querySelector('svg');
            if (!state || !svg) return;
            var rect = svg.getBoundingClientRect();
            if (!rect.width || !rect.height) return;
            var vx = (event.clientX - rect.left) / rect.width * state.width;
            var vy = (event.clientY - rect.top) / rect.height * state.height;
            if (vx < state.left || vx > state.left + state.plotWidth
                    || vy < state.top || vy > state.top + state.plotHeight) {
                hideStatGraphHover(graph);
                return;
            }
            var base = state.series[0].points;
            if (!base || !base.length) {
                hideStatGraphHover(graph);
                return;
            }
            var nearest = 0;
            var best = Number.POSITIVE_INFINITY;
            base.forEach(function (point, index) {
                var gx = state.left + (point.x - state.xMin) / (state.xMax - state.xMin)
                        * state.plotWidth;
                var distance = Math.abs(gx - vx);
                if (distance < best) {
                    best = distance;
                    nearest = index;
                }
            });
            var xValue = base[nearest].x;
            var xPosition = state.left + (xValue - state.xMin) / (state.xMax - state.xMin)
                    * state.plotWidth;
            var hoverLine = graph.querySelector('[data-stat-hover-line]');
            if (hoverLine) {
                hoverLine.setAttribute('x1', xPosition.toFixed(1));
                hoverLine.setAttribute('x2', xPosition.toFixed(1));
                hoverLine.setAttribute('visibility', 'visible');
            }

            var rows = [];
            state.series.forEach(function (item) {
                var point = item.points[nearest];
                if (!point) return;
                rows.push({label: item.label, color: item.color, value: point.y});
            });
            rows.sort(function (a, b) {
                return b.value - a.value;
            });
            var tooltip = graph.querySelector('[data-stat-tooltip]');
            if (!tooltip) return;
            var html = ['<div class="stat-graph-tooltip-head">x = '
                    + graphXml(graphNumber(xValue)) + '</div>'];
            rows.forEach(function (row) {
                html.push('<div class="stat-graph-tooltip-row"><i class="compare-graph-swatch" '
                        + 'style="background:' + graphXml(row.color) + '"></i><span>'
                        + graphXml(row.label) + '</span><strong>'
                        + graphXml(graphNumber(row.value)) + '</strong></div>');
            });
            tooltip.innerHTML = html.join('');
            tooltip.hidden = false;
            var graphRect = graph.getBoundingClientRect();
            var left = event.clientX - graphRect.left + 14;
            var top = event.clientY - graphRect.top + 12;
            left = Math.min(left, graph.clientWidth - tooltip.offsetWidth - 8);
            top = Math.min(top, graph.clientHeight - tooltip.offsetHeight - 8);
            tooltip.style.left = Math.max(8, left) + 'px';
            tooltip.style.top = Math.max(8, top) + 'px';
        });
        graph.addEventListener('mouseleave', function () {
            hideStatGraphHover(graph);
        });
    }

    function hideStatGraphHover(graph) {
        var hoverLine = graph.querySelector('[data-stat-hover-line]');
        if (hoverLine) hoverLine.setAttribute('visibility', 'hidden');
        var tooltip = graph.querySelector('[data-stat-tooltip]');
        if (tooltip) tooltip.hidden = true;
    }

    function statGraphLegend(series) {
        var legend = ['<div class="compare-graph-legend">'];
        series.forEach(function (item) {
            var hidden = statLineState[item.key] === false;
            legend.push('<button type="button" class="compare-graph-legend-item'
                    + (hidden ? ' is-hidden' : '') + '" data-stat-legend-key="'
                    + graphXml(item.key) + '" aria-pressed="' + (hidden ? 'false' : 'true')
                    + '" title="' + (hidden ? 'Show ' : 'Hide ') + graphXml(item.label)
                    + '"><i class="compare-graph-swatch" style="background:'
                    + graphXml(item.color) + '"></i>' + graphXml(item.label) + '</button>');
        });
        legend.push('</div>');
        return legend.join('');
    }

    function updateStatLineCounts(visible, total) {
        var visibleCount = document.querySelector('[data-stat-visible-count]');
        if (visibleCount) visibleCount.textContent = String(visible);
    }

    function applyView() {
        var select = document.querySelector('[data-compare-view]');
        var list = document.querySelector('[data-compare-list]');
        var graph = document.querySelector('[data-compare-histogram]');
        if (!list || !graph) return;
        var showGraph = select && (select.value === 'histogram' || select.value === 'graph');
        list.hidden = showGraph;
        graph.hidden = !showGraph;
        if (showGraph) renderHistogram();
    }

    function sortRows() {
        var container = document.querySelector('[data-compare-rows]');
        if (!container) {
            return;
        }
        var select = document.querySelector('[data-compare-sort]');
        var key = select ? select.value : 'overall';
        var list = rows();
        list.sort(function (a, b) {
            if (key === 'name') {
                return value(a, key).localeCompare(value(b, key));
            }
            var difference = value(b, key) - value(a, key);
            if (difference !== 0) {
                return difference;
            }
            return (a.getAttribute('data-name') || '')
                    .localeCompare(b.getAttribute('data-name') || '');
        });
        list.forEach(function (row) {
            container.appendChild(row);
        });
        filterRows();
    }

    document.addEventListener('change', function (event) {
        if (event.target && event.target.matches && event.target.matches('[data-compare-sort]')) {
            sortRows();
        }
        if (event.target && event.target.matches && event.target.matches('[data-compare-view]')) {
            applyView();
        }
    });

    document.addEventListener('click', function (event) {
        var item = event.target && event.target.closest
                ? event.target.closest('[data-stat-legend-key]') : null;
        if (!item) return;
        var key = item.getAttribute('data-stat-legend-key');
        statLineState[key] = statLineState[key] === false;
        renderStatGraph();
    });

    document.addEventListener('input', function (event) {
        if (event.target && event.target.matches && event.target.matches('[data-compare-filter]')) {
            filterRows();
        }
    });

    document.addEventListener('htmx:afterSwap', function (event) {
        if (!event.target || !event.target.querySelector) {
            return;
        }
        if (event.target.querySelector('[data-compare-rows]')
                || event.target.querySelector('[data-stat-graph]')
                || event.target.querySelector('[data-compare-histogram]')) {
            applyView();
            sortRows();
            renderStatGraph();
        }
    });

    var chartResizeTimer = null;
    window.addEventListener('resize', function () {
        if (chartResizeTimer) clearTimeout(chartResizeTimer);
        chartResizeTimer = setTimeout(function () {
            renderHistogram();
            renderStatGraph();
        }, 150);
    });

    document.body.addEventListener('compareAvailabilityChanged', function (event) {
        var detail = event.detail || {};
        if (detail.gemmedId === undefined) {
            return;
        }
        var available = !!detail.available;
        var title = available
                ? 'Remove this exact gemming/enchant from the custom item pool'
                : 'Add this exact gemming/enchant to the custom item pool';
        document.querySelectorAll(
                '[data-compare-availability][data-gemmed-id="' + detail.gemmedId + '"]')
                .forEach(function (button) {
                    button.classList.toggle('is-available', available);
                    button.setAttribute('aria-pressed', available ? 'true' : 'false');
                    button.title = title;
                    var diamond = button.querySelector('.compare-diamond');
                    if (diamond) {
                        diamond.textContent = available ? '\u25c6' : '\u25c7';
                    }
                    var label = button.querySelector('.sr-only');
                    if (label) {
                        label.textContent = available ? 'Available' : 'Not available';
                    }
                });
    });
})();

/*
 * Generic modal dismissal: Escape or a click on the dark backdrop clears the server-rendered
 * modal root. The custom confirm dialog keeps its own handler; the item picker and the advanced
 * stats <dialog> also keep their dedicated behavior.
 */
(function () {
    function closeModal() {
        var roots = document.querySelectorAll('.modal-root');
        for (var i = roots.length - 1; i >= 0; i--) {
            var root = roots[i];
            if (root.childElementCount > 0) {
                if (window.jrawrRestoreModalFocus) window.jrawrRestoreModalFocus(root);
                root.innerHTML = '';
                return true;
            }
        }
        return false;
    }

    document.addEventListener('keydown', function (event) {
        if (event.defaultPrevented) {
            return;
        }
        if (event.key === 'Escape' && closeModal()) {
            event.preventDefault();
        }
    });

    document.addEventListener('click', function (event) {
        var target = event.target;
        if (target && target.classList && target.classList.contains('modal-backdrop')) {
            var root = target.closest('.modal-root');
            if (root) {
                if (window.jrawrRestoreModalFocus) window.jrawrRestoreModalFocus(root);
                root.innerHTML = '';
            }
        }
    });
})();

/*
 * Optimizer thoroughness: the visible slider has five labelled stops; the hidden numeric field is
 * what the form submits (the engine still uses the 1-400 scale). Re-sync after htmx swaps the panel.
 */
(function () {
    function syncThoroughness(range) {
        var values = (range.getAttribute('data-thoroughness-values') || '')
                .split(',').map(Number);
        var index = Math.max(0, Math.min(values.length - 1, Number(range.value) - 1));
        var field = range.closest('.field');
        var hidden = field ? field.querySelector('input[type="hidden"][name="thoroughness"]') : null;
        var labels = field ? field.querySelectorAll('.thoroughness-labels span') : [];
        var current = field ? field.querySelector('.thoroughness-current') : null;
        if (hidden && values[index]) {
            hidden.value = values[index];
        }
        if (labels[index]) {
            range.setAttribute('aria-valuetext', labels[index].textContent.trim());
            if (current) {
                current.textContent = labels[index].textContent.trim();
            }
        }
    }

    function syncAllThoroughness() {
        document.querySelectorAll('input[type="range"][data-thoroughness-values]')
                .forEach(syncThoroughness);
    }

    document.addEventListener('input', function (event) {
        if (event.target && event.target.matches
                && event.target.matches('input[type="range"][data-thoroughness-values]')) {
            syncThoroughness(event.target);
        }
    });
    document.addEventListener('htmx:afterSwap', syncAllThoroughness);
    syncAllThoroughness();
})();

/*
 * Item filter tree: collapsible branches, live name/pattern search and an on/off status filter.
 * Enable checkboxes post through htmx and re-render the panel, so the expanded branches, the
 * search text and the status filter are kept in sessionStorage and restored after each swap.
 */
(function () {
    var OPEN_KEY = 'jrawr.itemFilters.open.v1';
    var FILTER_KEY = 'jrawr.itemFilters.filter.v1';
    var DEFAULTS_KEY = 'jrawr.itemFilters.defaultsOpen.v1';
    var lastSelected = null;
    var pendingScroll = null;
    var pendingFocus = null;

    function storageGet(key) {
        try {
            return window.sessionStorage.getItem(key);
        } catch (error) {
            return null;
        }
    }

    function storageSet(key, value) {
        try {
            window.sessionStorage.setItem(key, value);
        } catch (error) {
            /* storage can be unavailable in private browsing */
        }
    }

    function readOpen() {
        var raw = storageGet(OPEN_KEY);
        if (raw === null) return null;
        try {
            var value = JSON.parse(raw);
            return Array.isArray(value) ? value : null;
        } catch (error) {
            return null;
        }
    }

    function readFilter() {
        var raw = storageGet(FILTER_KEY);
        try {
            var value = raw ? JSON.parse(raw) : null;
            return {
                query: value && typeof value.query === 'string' ? value.query : '',
                status: value && (value.status === 'on' || value.status === 'off')
                        ? value.status : 'all'
            };
        } catch (error) {
            return { query: '', status: 'all' };
        }
    }

    function treeRoot() {
        return document.querySelector('[data-filter-tree]');
    }

    function branchOf(element) {
        return element && element.closest ? element.closest('.filter-branch') : null;
    }

    function chevronOf(branch) {
        var row = branch ? branch.querySelector('.filter-row') : null;
        return row ? row.querySelector('[data-filter-toggle]') : null;
    }

    function setCollapsed(branch, collapsed) {
        if (!branch) return;
        branch.classList.toggle('is-collapsed', collapsed);
        var chevron = chevronOf(branch);
        if (chevron) chevron.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
    }

    function openPaths(tree) {
        return Array.prototype.map.call(
                tree.querySelectorAll('.filter-branch:not(.is-collapsed)'),
                function (branch) { return branch.getAttribute('data-path'); });
    }

    function applyOpen(tree, paths) {
        var open = {};
        paths.forEach(function (path) { open[path] = true; });
        tree.querySelectorAll('.filter-branch').forEach(function (branch) {
            setCollapsed(branch, !open[branch.getAttribute('data-path')]);
        });
    }

    function expandSelected(tree) {
        var row = tree.querySelector('.filter-row.is-selected');
        var branch = branchOf(row);
        var parent = branch && branch.parentElement;
        var ancestor = parent && parent.closest ? parent.closest('.filter-branch') : null;
        while (ancestor) {
            setCollapsed(ancestor, false);
            var next = ancestor.parentElement;
            ancestor = next && next.closest ? next.closest('.filter-branch') : null;
        }
        return row;
    }

    function nodeMatches(branch, query, status) {
        var search = (branch.getAttribute('data-search') || '').toLowerCase();
        var enabled = branch.getAttribute('data-enabled') === 'true';
        var queryMatch = !query || search.indexOf(query) !== -1;
        var statusMatch = status === 'all' || (status === 'on' ? enabled : !enabled);
        return queryMatch && statusMatch;
    }

    function filterBranch(branch, query, status, active) {
        var self = nodeMatches(branch, query, status);
        var children = branch.querySelector('.filter-children');
        var childMatch = false;
        if (children) {
            Array.prototype.forEach.call(children.children, function (child) {
                if (child.classList && child.classList.contains('filter-branch')
                        && filterBranch(child, query, status, active)) {
                    childMatch = true;
                }
            });
        }
        var visible = self || childMatch;
        branch.classList.toggle('is-hidden', !visible);
        if (active && childMatch && (query !== '' || !self)) setCollapsed(branch, false);
        return visible;
    }

    function applyFilter(tree) {
        if (!tree) return;
        var filter = readFilter();
        var query = filter.query.trim().toLowerCase();
        var status = filter.status;
        var active = query !== '' || status !== 'all';
        tree.querySelectorAll('.filter-tree > .filter-branch').forEach(function (branch) {
            filterBranch(branch, query, status, active);
        });
        tree.querySelectorAll('.filter-section').forEach(function (section) {
            section.classList.toggle('is-hidden',
                    !section.querySelector('.filter-branch:not(.is-hidden)'));
        });
        var empty = tree.parentElement
                ? tree.parentElement.querySelector('[data-filter-empty]') : null;
        if (empty) {
            empty.hidden = !tree.querySelector('.filter-branch')
                    || !!tree.querySelector('.filter-branch:not(.is-hidden)');
        }
        var input = document.querySelector('[data-filter-search]');
        if (input && input.value !== filter.query) input.value = filter.query;
        document.querySelectorAll('[data-filter-status]').forEach(function (button) {
            var selected = button.getAttribute('data-filter-status') === status;
            button.classList.toggle('is-active', selected);
            button.setAttribute('aria-pressed', selected ? 'true' : 'false');
        });
        var editor = document.querySelector('[data-filter-editor]');
        var editorHidden = document.querySelector('[data-filter-editor-hidden]');
        var hidden = editor !== null && !selectionVisible(tree);
        if (editor) editor.hidden = hidden;
        if (editorHidden) editorHidden.hidden = !hidden;
    }

    function selectionVisible(tree) {
        var row = tree.querySelector('.filter-row.is-selected');
        var branch = branchOf(row);
        while (branch) {
            if (branch.classList.contains('is-hidden')) return false;
            var parent = branch.parentElement;
            branch = parent && parent.closest ? parent.closest('.filter-branch') : null;
        }
        return true;
    }

    function sync(tree) {
        if (!tree) return;
        var stored = readOpen();
        if (stored !== null) applyOpen(tree, stored);
        expandSelected(tree);
        applyFilter(tree);
        tree.querySelectorAll('[data-filter-enable][data-check-state]').forEach(function (box) {
            box.indeterminate = box.getAttribute('data-check-state') === 'some';
        });
        var selected = tree.querySelector('.filter-row.is-selected');
        var path = selected && selected.closest('.filter-branch')
                ? selected.closest('.filter-branch').getAttribute('data-path') : null;
        if (pendingScroll !== null) {
            tree.scrollTop = pendingScroll;
            pendingScroll = null;
        }
        if (selected && path !== lastSelected) selected.scrollIntoView({ block: 'nearest' });
        if (pendingFocus !== null) {
            var box = tree.querySelector('.filter-branch[data-path="' + pendingFocus
                    + '"] [data-filter-enable]');
            if (box) box.focus({ preventScroll: true });
            pendingFocus = null;
        }
        var defaults = document.querySelector('.filters-defaults');
        var defaultsOpen = storageGet(DEFAULTS_KEY);
        if (defaults && defaultsOpen !== null) defaults.open = defaultsOpen === 'true';
        lastSelected = path;
    }

    document.addEventListener('click', function (event) {
        var toggle = event.target.closest ? event.target.closest('[data-filter-toggle]') : null;
        if (toggle) {
            var branch = branchOf(toggle);
            if (!branch) return;
            var tree = branch.closest('[data-filter-tree]');
            setCollapsed(branch, !branch.classList.contains('is-collapsed'));
            if (tree) storageSet(OPEN_KEY, JSON.stringify(openPaths(tree)));
            event.preventDefault();
            return;
        }
        var status = event.target.closest ? event.target.closest('[data-filter-status]') : null;
        if (status) {
            var state = readFilter();
            state.status = status.getAttribute('data-filter-status');
            storageSet(FILTER_KEY, JSON.stringify(state));
            applyFilter(treeRoot());
        }
    });

    document.addEventListener('input', function (event) {
        if (!event.target.matches || !event.target.matches('[data-filter-search]')) return;
        var state = readFilter();
        state.query = event.target.value;
        storageSet(FILTER_KEY, JSON.stringify(state));
        applyFilter(treeRoot());
    });

    function directChildBranches(branch) {
        for (var i = 0; i < branch.children.length; i++) {
            var children = branch.children[i];
            if (children.classList && children.classList.contains('filter-children')) {
                return Array.prototype.filter.call(children.children, function (child) {
                    return child.classList && child.classList.contains('filter-branch');
                });
            }
        }
        return [];
    }

    function setOwnRecursive(branch, value) {
        branch.setAttribute('data-own-enabled', value ? 'true' : 'false');
        directChildBranches(branch).forEach(function (child) {
            setOwnRecursive(child, value);
        });
    }

    function refreshBranch(branch, parentEffective) {
        var own = branch.getAttribute('data-own-enabled') === 'true';
        var effective = parentEffective && own;
        var children = directChildBranches(branch);
        var total;
        var enabled;
        if (children.length) {
            total = 0;
            enabled = 0;
            children.forEach(function (child) {
                var stats = refreshBranch(child, effective);
                total += stats.total;
                enabled += stats.enabled;
            });
            if (!effective) enabled = 0;
        } else {
            total = 1;
            enabled = effective ? 1 : 0;
        }
        var row = branch.querySelector('.filter-row');
        var box = row ? row.querySelector('[data-filter-enable]') : null;
        if (row) row.classList.toggle('is-off', !effective);
        if (children.length) {
            var state = enabled === total ? 'all' : (enabled === 0 ? 'none' : 'some');
            if (box) {
                box.checked = state === 'all';
                box.indeterminate = state === 'some';
                box.setAttribute('data-check-state', state);
            }
            branch.setAttribute('data-check-state', state);
            var count = row ? row.querySelector('.filter-node-count') : null;
            if (count) count.textContent = enabled + '/' + total;
        } else {
            var leafState = effective ? 'all' : 'none';
            if (box) {
                box.checked = effective;
                box.indeterminate = false;
                box.setAttribute('data-check-state', leafState);
            }
            branch.setAttribute('data-check-state', leafState);
        }
        branch.setAttribute('data-enabled', effective ? 'true' : 'false');
        return { total: total, enabled: enabled };
    }

    function enableAncestors(branch) {
        var parent = branch.parentElement;
        var ancestor = parent && parent.closest ? parent.closest('.filter-branch') : null;
        while (ancestor) {
            ancestor.setAttribute('data-own-enabled', 'true');
            var next = ancestor.parentElement;
            ancestor = next && next.closest ? next.closest('.filter-branch') : null;
        }
    }

    function refreshTreeAfterToggle(box) {
        var tree = treeRoot();
        var branch = branchOf(box);
        if (!tree || !branch) return;
        var form = box.closest('form');
        var deep = !!(form && form.querySelector('input[name="deep"]'));
        var target = box.checked;
        if (target) {
            enableAncestors(branch);
            if (deep) setOwnRecursive(branch, true);
            else branch.setAttribute('data-own-enabled', 'true');
        } else if (deep) {
            setOwnRecursive(branch, false);
        } else {
            branch.setAttribute('data-own-enabled', 'false');
        }
        var on = 0;
        tree.querySelectorAll('.filter-tree > .filter-branch').forEach(function (top) {
            on += refreshBranch(top, true).enabled;
        });
        var summary = document.querySelector('[data-filter-summary]');
        if (summary) {
            var total = summary.getAttribute('data-total') || '';
            summary.textContent = total + ' filters · ' + on + ' on';
        }
        var selected = tree.querySelector('.filter-row.is-selected');
        var selectedBranch = branchOf(selected);
        var editor = document.querySelector('[data-filter-editor]');
        if (selectedBranch && editor) {
            var enabledBox = editor.querySelector('input[name="enabled"]');
            if (enabledBox) {
                enabledBox.checked = selectedBranch.getAttribute('data-own-enabled') === 'true';
            }
        }
        applyFilter(tree);
    }

    document.addEventListener('change', function (event) {
        var box = event.target.closest ? event.target.closest('[data-filter-enable]') : null;
        if (!box) return;
        refreshTreeAfterToggle(box);
        var form = box.closest('form');
        if (!form) return;
        if (form.requestSubmit) form.requestSubmit();
        else form.submit();
    });

    document.addEventListener('toggle', function (event) {
        if (event.target.matches && event.target.matches('.filters-defaults')) {
            storageSet(DEFAULTS_KEY, event.target.open ? 'true' : 'false');
        }
    }, true);

    document.addEventListener('htmx:beforeSwap', function (event) {
        var tree = treeRoot();
        if (!tree) return;
        if (event.target && event.target.closest && !event.target.closest('#filters-panel')) return;
        pendingScroll = tree.scrollTop;
        var active = document.activeElement;
        if (active && active.matches && active.matches('[data-filter-enable]')) {
            var branch = branchOf(active);
            pendingFocus = branch ? branch.getAttribute('data-path') : null;
        }
    });

    document.addEventListener('htmx:afterSettle', function () {
        // The filter modal is fetched into #modal (target #modal), while selecting a node swaps
        // #filters-panel. Sync whenever the tree exists; the old #filters-panel-only guard skipped
        // the initial modal load, so partial groups rendered without their indeterminate dash
        // until a node was selected.
        var tree = treeRoot();
        if (tree) sync(tree);
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { sync(treeRoot()); });
    } else {
        sync(treeRoot());
    }
})();
