'use strict';

/*
 * Angular-free, dependency-free palette engine for the Appearance modal.
 *
 * The saved theme is a JSON map of CSS custom properties on :root. It is read before the
 * stylesheet loads (this script is intentionally not deferred) so a custom palette is applied on
 * the very first paint. The Appearance modal edits that map live; presets replace all tokens.
 */
(function () {
    var STORAGE_KEY = 'jrawr.palette.v1';
    var THEMES_KEY = 'jrawr.themes.v1';
    var KEY_PATTERN = /^--[a-z0-9-]{1,72}$/;
    var MAX_VALUE_LENGTH = 240;
    var MAX_THEME_NAME = 40;
    var MAX_THEMES = 50;
    var statusTimer = null;
    var libraryTimer = null;

    var PRESETS = {
        midnight: {
            '--bg': '#0a0f16',
            '--bg-top': '#121c2a',
            '--bg-elevated': '#14202d',
            '--bg-deep': '#080c12',
            '--bg-deepest': '#05080c',
            '--bg-inset': '#0d1520',
            '--panel': '#101a26',
            '--panel-alt': '#162332',
            '--panel-raised': '#1a2a3c',
            '--card': '#172433',
            '--card-head': '#1d2e40',
            '--card-highlight': '#24384e',
            '--surface-tint': '#1a2838',
            '--button': '#21344a',
            '--button-hover': '#2a425d',
            '--text': '#e6edf7',
            '--text-muted': '#b5c4d8',
            '--text-dim': '#8ea1b8',
            '--text-faint': '#6f8199',
            '--on-accent': '#08131f',
            '--on-solid': '#ffffff',
            '--border': '#39526c',
            '--border-soft': '#293c50',
            '--border-strong': '#5f7d9e',
            '--border-hover': '#4d6985',
            '--border-deep': '#223344',
            '--accent': '#7fb0ff',
            '--accent-strong': '#4f86d6',
            '--accent-2': '#69c5b3',
            '--info': '#b18cff',
            '--warning': '#e0b34a',
            '--danger': '#e07b6d',
            '--success': '#86c98a',
            '--highlight': '#ffd100',
            '--highlight-strong': '#d9a400'
        },
        parchment: {
            '--bg': '#efe5d1',
            '--bg-top': '#f8f1e3',
            '--bg-elevated': '#f6eedf',
            '--bg-deep': '#e4d5bb',
            '--bg-deepest': '#d8c6a6',
            '--bg-inset': '#f2e8d7',
            '--panel': '#fbf5e9',
            '--panel-alt': '#f4ead8',
            '--panel-raised': '#fffaf0',
            '--card': '#fff8ec',
            '--card-head': '#f1e3c9',
            '--card-highlight': '#f7edda',
            '--surface-tint': '#f6eddd',
            '--button': '#ead8b8',
            '--button-hover': '#dfc9a3',
            '--text': '#332a1f',
            '--text-muted': '#5c4c38',
            '--text-dim': '#7d6a51',
            '--text-faint': '#9c8a70',
            '--on-accent': '#fff8ec',
            '--on-solid': '#ffffff',
            '--border': '#b9a17a',
            '--border-soft': '#d3c1a2',
            '--border-strong': '#8a6f3a',
            '--border-hover': '#9d8157',
            '--border-deep': '#c9b48e',
            '--accent': '#8a5a12',
            '--accent-strong': '#6d4308',
            '--accent-2': '#1f6f62',
            '--info': '#6f4bb8',
            '--warning': '#9a5b00',
            '--danger': '#a53a2f',
            '--success': '#2f6b39',
            '--highlight': '#b58100',
            '--highlight-strong': '#9c6e00',
            '--q-poor': '#6d6358',
            '--q-common': '#2f2a22',
            '--q-uncommon': '#1f7a1f',
            '--q-rare': '#1a5fb4',
            '--q-epic': '#7a2bb8',
            '--q-legendary': '#b35a00',
            '--q-artifact': '#8a6f3a',
            '--q-heirloom': '#007fa8',
            '--q-temp': '#6d6358',
            '--class-warrior': '#7a5a32',
            '--class-hunter': '#557f22',
            '--class-rogue': '#7a6a00',
            '--class-priest': '#4a4a4a',
            '--class-mage': '#1f7fa8',
            '--class-warlock': '#5f4a9e',
            '--class-paladin': '#c04f80',
            '--class-deathknight': '#a51f38',
            '--class-shaman': '#005aa8',
            '--class-druid': '#c05f00',
            '--class-default': '#4b5a6b',
            '--gem-green': '#1f8a3a',
            '--gem-yellow': '#a88800',
            '--gem-meta': '#6d6d6d',
            '--gem-unknown': '#5b6678',
            '--spec-frost': '#1f7fa8',
            '--spec-arcane': '#6a3fb0',
            '--spec-balance': '#6a3fb0',
            '--spec-shadow': '#6a3fb0',
            '--spec-affliction': '#6a3fb0',
            '--spec-demonology': '#5f3fa8',
            '--spec-subtlety': '#5f4a9e',
            '--spec-discipline': '#3f4a5a',
            '--spec-holy': '#8a6f1e',
            '--spec-survival': '#8a6f1e',
            '--spec-marksmanship': '#2f5fa8',
            '--spec-elemental': '#1f5fb8'
        },
        contrast: {
            '--bg': '#000000',
            '--bg-top': '#000000',
            '--bg-elevated': '#0b0b0b',
            '--bg-deep': '#000000',
            '--bg-deepest': '#000000',
            '--bg-inset': '#0f0f0f',
            '--panel': '#090909',
            '--panel-alt': '#101010',
            '--panel-raised': '#181818',
            '--card': '#111111',
            '--card-head': '#1a1a1a',
            '--card-highlight': '#222222',
            '--surface-tint': '#151515',
            '--button': '#222222',
            '--button-hover': '#333333',
            '--text': '#ffffff',
            '--text-muted': '#e0e0e0',
            '--text-dim': '#bdbdbd',
            '--text-faint': '#9e9e9e',
            '--on-accent': '#000000',
            '--on-solid': '#ffffff',
            '--border': '#808080',
            '--border-soft': '#555555',
            '--border-strong': '#b0b0b0',
            '--border-hover': '#999999',
            '--border-deep': '#444444',
            '--accent': '#ffe45c',
            '--accent-strong': '#ffc400',
            '--accent-2': '#6fffe0',
            '--info': '#c9a7ff',
            '--warning': '#ffbf00',
            '--danger': '#ff7b7b',
            '--success': '#7dff9a',
            '--highlight': '#ffff00',
            '--highlight-strong': '#ffd000',
            '--q-poor': '#bdbdbd',
            '--q-common': '#ffffff',
            '--q-uncommon': '#7dff9a',
            '--q-rare': '#6fb3ff',
            '--q-epic': '#d59bff',
            '--q-legendary': '#ffb066',
            '--q-artifact': '#ffe08a',
            '--q-heirloom': '#7fe8ff',
            '--q-temp': '#bdbdbd'
        }
    };

    function readPalette() {
        try {
            var raw = window.localStorage.getItem(STORAGE_KEY);
            if (!raw) return {};
            var parsed = JSON.parse(raw);
            if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
            var clean = {};
            Object.keys(parsed).forEach(function (key) {
                if (validKey(key) && validValue(parsed[key])) {
                    clean[key] = parsed[key];
                }
            });
            return clean;
        } catch (error) {
            return {};
        }
    }

    function writePalette(palette) {
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(palette));
        } catch (error) {
            /* Private mode or a full quota: the live preview still works for this page. */
        }
    }

    function validKey(key) {
        return typeof key === 'string' && KEY_PATTERN.test(key);
    }

    function validValue(value) {
        return typeof value === 'string'
                && value.length > 0
                && value.length <= MAX_VALUE_LENGTH
                && !/[;{}\r\n]/.test(value);
    }

    function sanitizePalette(source) {
        var clean = {};
        if (!source || typeof source !== 'object' || Array.isArray(source)) return clean;
        Object.keys(source).forEach(function (key) {
            if (validKey(key) && validValue(source[key])) {
                clean[key] = source[key];
            }
        });
        return clean;
    }

    function readThemes() {
        try {
            var raw = window.localStorage.getItem(THEMES_KEY);
            if (!raw) return [];
            var parsed = JSON.parse(raw);
            if (!Array.isArray(parsed)) return [];
            var themes = [];
            parsed.forEach(function (item) {
                if (!item || typeof item.name !== 'string') return;
                var name = item.name.trim().slice(0, MAX_THEME_NAME);
                if (!name) return;
                var palette = sanitizePalette(item.palette);
                if (!Object.keys(palette).length) return;
                themes.push({
                    name: name,
                    palette: palette,
                    updatedAt: typeof item.updatedAt === 'number' ? item.updatedAt : 0
                });
            });
            return themes.slice(0, MAX_THEMES);
        } catch (error) {
            return [];
        }
    }

    function writeThemes(themes) {
        try {
            window.localStorage.setItem(THEMES_KEY, JSON.stringify(themes));
            return true;
        } catch (error) {
            return false;
        }
    }

    function findTheme(themes, name) {
        var needle = String(name || '').trim().toLowerCase();
        for (var i = 0; i < themes.length; i++) {
            if (themes[i].name.toLowerCase() === needle) return i;
        }
        return -1;
    }

    function samePalette(left, right) {
        var leftKeys = Object.keys(left || {});
        var rightKeys = Object.keys(right || {});
        if (leftKeys.length !== rightKeys.length) return false;
        for (var i = 0; i < leftKeys.length; i++) {
            var key = leftKeys[i];
            if (left[key] !== right[key]) return false;
        }
        return true;
    }

    function applyToken(key, value) {
        if (!validKey(key) || !validValue(value)) return;
        document.documentElement.style.setProperty(key, value);
    }

    function clearToken(key) {
        if (!validKey(key)) return;
        document.documentElement.style.removeProperty(key);
    }

    function normalizeHex(value) {
        var text = String(value == null ? '' : value).trim().toLowerCase();
        if (/^#[0-9a-f]{6}$/.test(text)) return text;
        if (/^#[0-9a-f]{3}$/.test(text)) {
            return '#' + text.charAt(1) + text.charAt(1) + text.charAt(2) + text.charAt(2)
                    + text.charAt(3) + text.charAt(3);
        }
        return null;
    }

    function colorToHex(value) {
        var hex = normalizeHex(value);
        if (hex) return hex;
        var match = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i.exec(String(value || '').trim());
        if (!match) return null;
        return '#' + [match[1], match[2], match[3]].map(function (part) {
            var number = Math.max(0, Math.min(255, parseInt(part, 10)));
            var text = number.toString(16);
            return text.length === 1 ? '0' + text : text;
        }).join('');
    }

    function currentToken(key) {
        var inline = document.documentElement.style.getPropertyValue(key).trim();
        if (inline) return inline;
        return window.getComputedStyle(document.documentElement).getPropertyValue(key).trim();
    }

    function editorKeys(editor) {
        var keys = [];
        if (!editor || !editor.querySelectorAll) return keys;
        editor.querySelectorAll('[data-theme-token], [data-theme-font]').forEach(function (input) {
            var key = input.getAttribute('data-theme-token')
                    || input.getAttribute('data-theme-font');
            if (validKey(key) && keys.indexOf(key) === -1) keys.push(key);
        });
        return keys;
    }

    function paletteKeys(editor) {
        var keys = editorKeys(editor);
        Object.keys(readPalette()).forEach(function (key) {
            if (validKey(key) && keys.indexOf(key) === -1) keys.push(key);
        });
        return keys;
    }

    function editorValue(editor, key) {
        var input = editor.querySelector('[data-theme-token="' + key + '"]');
        if (!input) input = editor.querySelector('[data-theme-font="' + key + '"]');
        return input ? input.value.trim() : '';
    }

    /*
     * Snapshot the effective palette, including CSS defaults when no override is stored. This
     * makes a saved theme self-contained: applying it later does not depend on the current theme.
     */
    function capturePalette(editor) {
        var palette = {};
        editorKeys(editor).forEach(function (key) {
            var value = currentToken(key) || editorValue(editor, key);
            if (!value || !validValue(value)) return;
            palette[key] = colorToHex(value) || value;
        });
        return palette;
    }

    function syncEditor(editor) {
        if (!editor) return;
        editor.querySelectorAll('[data-theme-token]').forEach(function (input) {
            var key = input.getAttribute('data-theme-token');
            var hex = colorToHex(currentToken(key));
            if (hex) input.value = hex;
            var text = editor.querySelector('[data-theme-hex="' + key + '"]');
            if (text && hex) text.value = hex;
        });
        editor.querySelectorAll('[data-theme-font]').forEach(function (input) {
            var key = input.getAttribute('data-theme-font');
            var value = currentToken(key);
            if (value) input.value = value;
        });
        syncPresetState(editor);
    }

    function syncPresetState(editor) {
        if (!editor) return;
        var stored = readPalette();
        var keys = editorKeys(editor);
        var active = null;
        if (!keys.some(function (key) { return Object.prototype.hasOwnProperty.call(stored, key); })) {
            active = 'default';
        } else {
            Object.keys(PRESETS).forEach(function (name) {
                if (active) return;
                var preset = PRESETS[name];
                var presetKeys = Object.keys(preset);
                if (presetKeys.length !== Object.keys(stored).length) return;
                var matches = presetKeys.every(function (key) {
                    return stored[key] === preset[key];
                });
                if (matches) active = name;
            });
        }
        editor.querySelectorAll('[data-theme-preset]').forEach(function (button) {
            var name = button.getAttribute('data-theme-preset');
            var selected = name === active;
            button.classList.toggle('is-active', selected);
            button.setAttribute('aria-pressed', selected ? 'true' : 'false');
        });
        renderThemeLibrary(editor);
    }

    function announce(editor, message) {
        var status = editor ? editor.querySelector('[data-theme-status]') : null;
        if (!status) return;
        status.textContent = message;
        if (statusTimer) window.clearTimeout(statusTimer);
        statusTimer = window.setTimeout(function () {
            status.textContent = '';
        }, 1800);
    }

    function persistToken(key, value) {
        var palette = readPalette();
        palette[key] = value;
        writePalette(palette);
    }

    function removeTokens(keys) {
        var palette = readPalette();
        keys.forEach(function (key) {
            delete palette[key];
        });
        writePalette(palette);
    }

    function applyPaletteMap(editor, palette, message) {
        if (!editor) return;
        var keys = paletteKeys(editor);
        keys.forEach(function (key) {
            clearToken(key);
        });
        removeTokens(keys);
        var clean = {};
        if (palette) {
            Object.keys(palette).forEach(function (key) {
                if (!validKey(key) || !validValue(palette[key])) return;
                applyToken(key, palette[key]);
                clean[key] = palette[key];
            });
        }
        writePalette(clean);
        syncEditor(editor);
        if (message) announce(editor, message);
    }

    function applyPreset(editor, name) {
        var preset = name === 'default' ? null : PRESETS[name];
        applyPaletteMap(editor, preset,
                'Applied ' + (name === 'default' ? 'the default palette' : name) + '.');
    }

    function resetAll(editor) {
        applyPaletteMap(editor, null, 'Reset to the default palette.');
    }

    function setLibraryStatus(editor, message, kind) {
        var status = editor ? editor.querySelector('[data-theme-library-status]') : null;
        if (!status) return;
        status.textContent = message;
        status.classList.toggle('is-error', kind === 'error');
        status.classList.toggle('is-warning', kind === 'warning');
        if (libraryTimer) window.clearTimeout(libraryTimer);
        libraryTimer = window.setTimeout(function () {
            status.textContent = '';
            status.classList.remove('is-error', 'is-warning');
        }, 4200);
    }

    function renderThemeLibrary(editor) {
        if (!editor) return;
        var list = editor.querySelector('[data-theme-library]');
        if (!list) return;
        var themes = readThemes();
        var active = readPalette();
        var rows = themes.map(function (theme) {
            return {theme: theme, active: samePalette(theme.palette, active)};
        });
        rows.sort(function (a, b) {
            return a.theme.name.toLowerCase().localeCompare(b.theme.name.toLowerCase());
        });
        var signature = JSON.stringify(rows.map(function (row) {
            return [row.theme.name, row.theme.updatedAt, row.active];
        }));
        if (list.getAttribute('data-theme-signature') === signature) return;
        list.setAttribute('data-theme-signature', signature);
        list.textContent = '';
        if (!rows.length) {
            var empty = document.createElement('p');
            empty.className = 'hint theme-library-empty';
            empty.textContent = 'No saved themes yet. Save the current palette to create one.';
            list.appendChild(empty);
            return;
        }
        rows.forEach(function (row) {
            var theme = row.theme;
            var item = document.createElement('div');
            item.className = 'theme-library-item';
            if (row.active) item.classList.add('is-active');

            var swatch = document.createElement('span');
            swatch.className = 'theme-library-swatch';
            swatch.setAttribute('aria-hidden', 'true');
            [['--bg'], ['--card'], ['--accent']].forEach(function (parts) {
                var part = document.createElement('i');
                var hex = colorToHex(theme.palette[parts[0]]);
                if (hex) part.style.background = hex;
                swatch.appendChild(part);
            });

            var name = document.createElement('span');
            name.className = 'theme-library-name';
            name.textContent = theme.name;

            var actions = document.createElement('span');
            actions.className = 'theme-library-actions';
            if (item.classList.contains('is-active')) {
                var badge = document.createElement('span');
                badge.className = 'theme-library-active';
                badge.textContent = 'Active';
                actions.appendChild(badge);
            }
            var apply = document.createElement('button');
            apply.type = 'button';
            apply.className = 'btn btn-small';
            apply.textContent = 'Apply';
            apply.setAttribute('data-theme-apply-theme', theme.name);
            var remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'btn btn-small theme-library-delete';
            remove.textContent = 'Delete';
            remove.setAttribute('data-theme-delete-theme', theme.name);
            actions.appendChild(apply);
            actions.appendChild(remove);

            item.appendChild(swatch);
            item.appendChild(name);
            item.appendChild(actions);
            list.appendChild(item);
        });
    }

    function saveCustomTheme(editor) {
        var input = editor ? editor.querySelector('[data-theme-name]') : null;
        var name = input ? input.value.trim().slice(0, MAX_THEME_NAME) : '';
        if (!name) {
            setLibraryStatus(editor, 'Enter a theme name first.', 'error');
            if (input) input.focus();
            return;
        }
        var palette = capturePalette(editor);
        if (!Object.keys(palette).length) {
            setLibraryStatus(editor, 'This palette has no values to save.', 'error');
            return;
        }
        var themes = readThemes();
        var index = findTheme(themes, name);
        if (index < 0 && themes.length >= MAX_THEMES) {
            setLibraryStatus(editor, 'Theme limit reached (' + MAX_THEMES + '). Delete one first.',
                    'error');
            return;
        }
        var record = {name: name, palette: palette, updatedAt: Date.now()};
        if (index >= 0) {
            themes[index] = record;
        } else {
            themes.push(record);
        }
        if (!writeThemes(themes)) {
            setLibraryStatus(editor, 'Could not save the theme in this browser.', 'error');
            return;
        }
        writePalette(palette);
        syncEditor(editor);
        setLibraryStatus(editor, index >= 0 ? 'Updated "' + name + '".' : 'Saved "' + name + '".');
        if (input) input.value = name;
    }

    function applyCustomTheme(editor, name) {
        var themes = readThemes();
        var index = findTheme(themes, name);
        if (index < 0) {
            setLibraryStatus(editor, 'That theme no longer exists.', 'error');
            return;
        }
        var theme = themes[index];
        var input = editor.querySelector('[data-theme-name]');
        if (input) input.value = theme.name;
        applyPaletteMap(editor, theme.palette, 'Applied "' + theme.name + '".');
        setLibraryStatus(editor, 'Applied "' + theme.name + '".');
    }

    function deleteCustomTheme(editor, name) {
        var themes = readThemes();
        var index = findTheme(themes, name);
        if (index < 0) {
            setLibraryStatus(editor, 'That theme no longer exists.', 'error');
            return;
        }
        var removed = themes.splice(index, 1)[0];
        if (!writeThemes(themes)) {
            setLibraryStatus(editor, 'Could not delete the theme in this browser.', 'error');
            return;
        }
        var input = editor.querySelector('[data-theme-name]');
        if (input && input.value.trim().toLowerCase() === removed.name.toLowerCase()) {
            input.value = '';
        }
        syncEditor(editor);
        setLibraryStatus(editor, 'Deleted "' + removed.name + '".', 'warning');
    }

    function editorFor(element) {
        return element && element.closest ? element.closest('[data-theme-editor]') : null;
    }

    function syncAllEditors() {
        document.querySelectorAll('[data-theme-editor]').forEach(syncEditor);
    }

    /* Apply the saved palette before the stylesheet paints. */
    (function boot() {
        var palette = readPalette();
        Object.keys(palette).forEach(function (key) {
            applyToken(key, palette[key]);
        });
    })();

    document.addEventListener('input', function (event) {
        var target = event.target;
        if (!target || !target.matches) return;
        var editor = editorFor(target);
        if (!editor) return;

        if (target.matches('[data-theme-token]')) {
            var colorKey = target.getAttribute('data-theme-token');
            var hex = normalizeHex(target.value);
            if (!hex) return;
            applyToken(colorKey, hex);
            persistToken(colorKey, hex);
            var text = editor.querySelector('[data-theme-hex="' + colorKey + '"]');
            if (text) text.value = hex;
            syncPresetState(editor);
            return;
        }

        if (target.matches('[data-theme-hex]')) {
            var textKey = target.getAttribute('data-theme-hex');
            var typed = normalizeHex(target.value);
            if (!typed) return;
            applyToken(textKey, typed);
            persistToken(textKey, typed);
            var swatch = editor.querySelector('[data-theme-token="' + textKey + '"]');
            if (swatch) swatch.value = typed;
            syncPresetState(editor);
            return;
        }

        if (target.matches('[data-theme-font]')) {
            var fontKey = target.getAttribute('data-theme-font');
            var fontValue = target.value.trim();
            if (!fontValue) return;
            applyToken(fontKey, fontValue);
            persistToken(fontKey, fontValue);
            syncPresetState(editor);
        }
    });

    document.addEventListener('focusout', function (event) {
        var target = event.target;
        if (!target || !target.matches || !target.matches('[data-theme-hex]')) return;
        var editor = editorFor(target);
        if (!editor) return;
        var key = target.getAttribute('data-theme-hex');
        var hex = normalizeHex(target.value) || colorToHex(currentToken(key));
        if (hex) {
            target.value = hex;
            var swatch = editor.querySelector('[data-theme-token="' + key + '"]');
            if (swatch) swatch.value = hex;
        }
    });

    document.addEventListener('click', function (event) {
        var target = event.target;
        if (!target || !target.closest) return;
        var editor = editorFor(target);
        if (!editor) return;

        var preset = target.closest('[data-theme-preset]');
        if (preset) {
            applyPreset(editor, preset.getAttribute('data-theme-preset'));
            return;
        }
        var applyTheme = target.closest('[data-theme-apply-theme]');
        if (applyTheme) {
            applyCustomTheme(editor, applyTheme.getAttribute('data-theme-apply-theme'));
            return;
        }
        var deleteTheme = target.closest('[data-theme-delete-theme]');
        if (deleteTheme) {
            var name = deleteTheme.getAttribute('data-theme-delete-theme');
            if (deleteTheme.getAttribute('data-confirm') !== 'true') {
                deleteTheme.setAttribute('data-confirm', 'true');
                deleteTheme.classList.add('is-confirming');
                deleteTheme.textContent = 'Confirm';
                setLibraryStatus(editor, 'Click Confirm to delete "' + name + '".', 'warning');
                return;
            }
            deleteCustomTheme(editor, name);
            return;
        }
        if (target.closest('[data-theme-save]')) {
            saveCustomTheme(editor);
            return;
        }
        if (target.closest('[data-theme-reset]')) {
            resetAll(editor);
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Enter') return;
        var target = event.target;
        if (!target || !target.matches || !target.matches('[data-theme-name]')) return;
        var editor = editorFor(target);
        if (!editor) return;
        event.preventDefault();
        saveCustomTheme(editor);
    });

    document.addEventListener('htmx:afterSwap', function (event) {
        var target = event.detail && event.detail.target;
        if (!target) return;
        if (target.matches && target.matches('[data-theme-editor]')) {
            syncEditor(target);
        } else if (target.querySelectorAll) {
            target.querySelectorAll('[data-theme-editor]').forEach(syncEditor);
        }
    });

    document.addEventListener('DOMContentLoaded', syncAllEditors);

    window.JRawrTheme = {
        applyPreset: applyPreset,
        reset: resetAll,
        refresh: syncAllEditors,
        palette: readPalette,
        themes: readThemes,
        current: currentToken
    };
})();
