/* Item editor: live per-item fetch queue, picker refresh and "Open in picker". */
(function () {
    'use strict';

    var activeRun = 0;

    function summaryText(root) {
        var counts = {
            added: 0,
            refreshed: 0,
            cached: 0,
            not_found: 0,
            error: 0,
            invalid: 0,
            queued: 0,
            undone: 0
        };
        root.querySelectorAll('.ie-result[data-status]').forEach(function (row) {
            var status = row.getAttribute('data-status');
            counts[status] = (counts[status] || 0) + 1;
        });
        var parts = [];
        if (counts.added) parts.push(counts.added + ' added');
        if (counts.refreshed) parts.push(counts.refreshed + ' updated');
        if (counts.cached) parts.push(counts.cached + ' cached');
        if (counts.not_found) parts.push(counts.not_found + ' not found');
        if (counts.error) parts.push(counts.error + (counts.error === 1 ? ' error' : ' errors'));
        if (counts.invalid) parts.push(counts.invalid + ' invalid');
        if (counts.queued) parts.push(counts.queued + ' fetching');
        if (counts.undone) parts.push(counts.undone + ' removed');
        return parts.length ? parts.join(' \u00b7 ') : 'No items';
    }

    function updateSummary() {
        var summary = document.getElementById('item-editor-summary');
        var results = document.getElementById('item-editor-add-results');
        if (summary) {
            summary.textContent = results ? summaryText(results) : 'No items';
        }
    }

    function setProgress(text) {
        var progress = document.querySelector('[data-fetch-progress]');
        var label = document.querySelector('[data-fetch-progress-text]');
        if (!progress || !label) return;
        if (text === null) {
            progress.hidden = true;
            return;
        }
        progress.hidden = false;
        label.textContent = text;
    }

    function startQueue(queue) {
        if (!queue || queue.getAttribute('data-started') === 'true') return;
        queue.setAttribute('data-started', 'true');
        var run = ++activeRun;
        var rows = Array.prototype.slice.call(queue.querySelectorAll('[data-fetch="true"]'));
        var total = rows.length;
        var host = queue.getAttribute('data-source-host') || 'the item source';
        var index = 0;

        function step() {
            if (run !== activeRun || !queue.isConnected) return;
            if (index >= rows.length) {
                setProgress(null);
                updateSummary();
                return;
            }
            var row = rows[index++];
            if (!row.isConnected) {
                step();
                return;
            }
            setProgress('Fetching ' + index + ' of ' + total + ' from ' + host + '\u2026');
            var values = {id: row.getAttribute('data-item-id')};
            if (row.getAttribute('data-refresh') === 'true') {
                values.refresh = 'true';
            }
            htmx.ajax('POST', '/tools/item-editor/fetch', {
                target: row,
                swap: 'outerHTML',
                values: values
            }).then(function () {
                updateSummary();
                step();
            }).catch(function () {
                if (row.isConnected) {
                    row.classList.add('is-missing');
                    var badge = row.querySelector('.ie-badge');
                    if (badge) badge.textContent = 'Error';
                    var text = row.querySelector('.ie-result-text');
                    if (text) {
                        var note = row.querySelector('.ie-result-note');
                        if (!note) {
                            note = document.createElement('p');
                            note.className = 'ie-result-note';
                            text.appendChild(note);
                        }
                        note.textContent = 'The request failed. Retry the fetch.';
                    }
                }
                updateSummary();
                step();
            });
        }

        var cancel = document.querySelector('[data-cancel-fetch]');
        if (cancel) {
            cancel.onclick = function () {
                activeRun++;
                setProgress(null);
                queue.querySelectorAll('[data-status="queued"]').forEach(function (row) {
                    row.remove();
                });
                updateSummary();
            };
        }
        if (!total) {
            setProgress(null);
            updateSummary();
            return;
        }
        setProgress('Fetching 1 of ' + total + ' from ' + host + '\u2026');
        updateSummary();
        step();
    }

    document.addEventListener('htmx:afterSwap', function () {
        var queue = document.querySelector(
                '#item-editor-queue[data-queue="true"]:not([data-started="true"])');
        if (queue) startQueue(queue);
    });

    function refreshPicker(id) {
        var picker = document.getElementById('item-picker');
        var results = document.getElementById('item-results');
        if (!picker || !results) return;
        var slotInput = picker.querySelector('input[name="slot"]');
        if (!slotInput) return;
        var url = '/items?slot=' + encodeURIComponent(slotInput.value);
        if (id) url += '&added=' + encodeURIComponent(id);
        htmx.ajax('GET', url, {
            target: '#item-results',
            swap: 'outerHTML',
            source: picker
        });
    }

    document.body.addEventListener('jrawr:item-cache-changed', function (event) {
        var detail = event.detail || {};
        refreshPicker(detail.id || null);
    });

    function triggerCustomPreview(element) {
        var form = element && element.closest ? element.closest('form.ie-custom-form') : null;
        if (form) {
            form.dispatchEvent(new Event('change', {bubbles: true}));
        }
    }

    function populateStatSelects(container) {
        var template = document.getElementById('custom-stat-options-template');
        if (!template) return;
        var scope = container && container.querySelectorAll ? container : document;
        scope.querySelectorAll('select[data-stat-select]').forEach(function (select) {
            if (select.getAttribute('data-populated') === 'true') return;
            var selected = select.getAttribute('data-selected') || '';
            select.innerHTML = template.innerHTML;
            select.value = selected;
            select.setAttribute('data-populated', 'true');
        });
    }

    function focusNewControl(container) {
        var last = container && container.lastElementChild;
        if (!last) return;
        var control = last.matches && last.matches('select, input') ? last
                : (last.querySelector ? last.querySelector('select, input') : null);
        if (control) {
            control.focus();
        }
    }

    document.addEventListener('click', function (event) {
        var addEffect = event.target.closest ? event.target.closest('[data-add-effect]') : null;
        if (addEffect) {
            var effectTemplate = document.getElementById('custom-effect-template');
            var effectRows = document.getElementById('custom-effect-rows');
            if (effectTemplate && effectRows) {
                var next = 0;
                effectRows.querySelectorAll('[data-effect-index]').forEach(function (el) {
                    var value = parseInt(el.getAttribute('data-effect-index'), 10);
                    if (!isNaN(value) && value >= next) next = value + 1;
                });
                var wrapper = document.createElement('div');
                wrapper.innerHTML = effectTemplate.innerHTML.replaceAll('__idx__', String(next));
                effectRows.appendChild(wrapper.firstElementChild);
                focusNewControl(effectRows);
                triggerCustomPreview(addEffect);
            }
            return;
        }
        var removeEffect = event.target.closest ? event.target.closest('[data-remove-effect]') : null;
        if (removeEffect) {
            var effectCard = removeEffect.closest('.ie-effect');
            if (effectCard) {
                effectCard.remove();
                triggerCustomPreview(removeEffect);
            }
            return;
        }
        var addEffectStat = event.target.closest ? event.target.closest('[data-add-effect-stat]') : null;
        if (addEffectStat) {
            var card = addEffectStat.closest('.ie-effect');
            var statTemplate = document.getElementById('custom-effect-stat-template');
            if (card && statTemplate) {
                var effectIndex = card.getAttribute('data-effect-index');
                var rows = card.querySelector('.ie-stat-rows');
                if (rows) {
                    var statWrapper = document.createElement('div');
                    statWrapper.innerHTML = statTemplate.innerHTML.replaceAll('__idx__', effectIndex);
                    rows.appendChild(statWrapper.firstElementChild);
                    populateStatSelects(rows);
                    focusNewControl(rows);
                    triggerCustomPreview(addEffectStat);
                }
            }
            return;
        }
        var add = event.target.closest ? event.target.closest('[data-add-row]') : null;
        if (add) {
            var kind = add.getAttribute('data-add-row');
            var template = document.getElementById(
                    kind === 'bonus' ? 'custom-bonus-template' : 'custom-stat-template');
            var container = document.getElementById(
                    kind === 'bonus' ? 'custom-bonus-rows' : 'custom-stat-rows');
            if (template && container) {
                container.appendChild(template.content.cloneNode(true));
                populateStatSelects(container);
                focusNewControl(container);
                triggerCustomPreview(add);
            }
            return;
        }
        var remove = event.target.closest ? event.target.closest('[data-remove-row]') : null;
        if (remove) {
            var line = remove.closest('.ie-stat-line');
            var row = line || remove.closest('.ie-stat-row');
            if (row) {
                row.remove();
                triggerCustomPreview(remove);
            }
        }
    });

    function focusCustomError() {
        var form = document.querySelector('.ie-custom-form');
        if (!form) return;
        var key = form.getAttribute('data-first-error');
        if (key) {
            var holder = form.querySelector('[data-field-key="' + key.replace(/"/g, '\\"') + '"]');
            if (holder) {
                var control = holder.matches('input, select, textarea') ? holder
                        : holder.querySelector('input, select, textarea');
                if (control) {
                    control.focus();
                    if (control.scrollIntoView) {
                        try {
                            control.scrollIntoView({block: 'center'});
                        } catch (error) {
                            control.scrollIntoView();
                        }
                    }
                }
            }
            return;
        }
        if (!form.contains(document.activeElement)) {
            var name = form.querySelector('[data-field-key="name"] input');
            if (name) name.focus();
        }
    }

    document.addEventListener('htmx:afterSettle', focusCustomError);
    document.addEventListener('htmx:afterSwap', function () {
        populateStatSelects(document);
    });

    document.addEventListener('click', function (event) {
        var clear = event.target.closest ? event.target.closest('[data-clear-add]') : null;
        if (clear) {
            activeRun++;
            setProgress(null);
            var addForm = document.getElementById('item-editor-add-form');
            if (addForm) addForm.reset();
            var results = document.getElementById('item-editor-add-results');
            if (results) {
                results.innerHTML = '<p class="empty-state">Paste item IDs and press Fetch items.</p>';
            }
            var summary = document.getElementById('item-editor-summary');
            if (summary) summary.textContent = 'No items yet';
            return;
        }
        var copy = event.target.closest ? event.target.closest('[data-open-custom]') : null;
        if (copy) {
            var source = copy.getAttribute('data-copy-source');
            if (source) {
                htmx.ajax('GET', '/tools/item-editor/custom?source=' + encodeURIComponent(source), {
                    target: '#item-editor-edit-body',
                    swap: 'outerHTML'
                }).then(function () {
                    var tab = document.querySelector('.item-editor-modal .tabs [data-tab="edit"]');
                    if (tab) tab.click();
                });
            }
            return;
        }
        var row = event.target.closest ? event.target.closest('.ie-list-row') : null;
        if (!row) return;
        document.querySelectorAll('.ie-list-row.is-selected').forEach(function (other) {
            other.classList.remove('is-selected');
        });
        row.classList.add('is-selected');
    });

    document.addEventListener('change', function (event) {
        var select = event.target.closest ? event.target.closest('[data-source-preset]') : null;
        if (!select) return;
        var form = select.closest('.ie-source-form');
        var input = form ? form.querySelector('[data-source-template]') : null;
        if (!input) return;
        var custom = select.value === 'custom';
        input.disabled = !custom;
        if (custom) {
            input.focus();
        } else {
            input.value = '';
        }
    });

    document.addEventListener('click', function (event) {
        var button = event.target.closest ? event.target.closest('[data-open-picker]') : null;
        if (!button) return;
        var slot = button.getAttribute('data-picker-slot');
        if (!slot) return;
        var id = button.getAttribute('data-item-id');
        var modal = document.getElementById('modal');
        if (modal) modal.innerHTML = '';
        var picker = document.getElementById('item-picker');
        if (!picker) return;
        var url = '/items?slot=' + encodeURIComponent(slot);
        if (id) url += '&added=' + encodeURIComponent(id);
        htmx.ajax('GET', url, {target: '#item-picker', swap: 'outerHTML'});
    });
})();
