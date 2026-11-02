package rawr.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings modal (icon site base URL + advanced icon template). The chosen site base is stored in
 * a long-lived cookie read by {@link ItemIconService}; the optional icon URL template goes into a
 * second cookie (DECISIONS #31/#35). Item lookup sources live in the Item editor, not here. Invalid
 * submissions keep the previous cookies. Character import lives in the File menu, not here.
 */
@Controller
public class SettingsController {
    private static final int COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 30 days

    private final ItemIconService itemIcons;
    private final GlobalSettingsService globalSettings;

    public SettingsController(ItemIconService itemIcons, GlobalSettingsService globalSettings) {
        this.itemIcons = itemIcons;
        this.globalSettings = globalSettings;
    }

    /** Exposes the icon helper to the settings fragment as {@code ${icons}}. */
    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /**
     * The icon-source dropdown options. The effective base is added when it is not one of the known
     * sites (for example a legacy cookie or a deployment-specific icon base).
     */
    @ModelAttribute("iconSources")
    public List<ItemIconService.SourceOption> iconSources() {
        List<ItemIconService.SourceOption> options =
                new ArrayList<ItemIconService.SourceOption>(ItemIconService.SOURCE_OPTIONS);
        String current = itemIcons.baseUrl();
        for (ItemIconService.SourceOption option : options) {
            if (option.base().equals(current)) {
                return options;
            }
        }
        options.add(0, new ItemIconService.SourceOption(current,
                "Current site (" + current.replaceFirst("^https?://", "") + ")"));
        return options;
    }

    /** htmx: the settings modal. */
    @GetMapping("/settings")
    public String settings() {
        return "fragments/settings :: settingsModal";
    }

    /**
     * Stores the site base in {@link ItemIconService#BASE_URL_COOKIE} (normalized to its origin)
     * and the optional advanced icon template in {@link ItemIconService#TEMPLATE_COOKIE}. An empty
     * template clears that cookie. Invalid values are ignored (the previous cookies stay) and the
     * browser goes back to the editor with the settings modal reopened.
     */
    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam(name = "iconBaseUrl", required = false) String iconBaseUrl,
            @RequestParam(name = "iconTemplate", required = false) String iconTemplate,
            @RequestParam(name = "hideProfEnchants", required = false) String hideProfEnchants,
            @RequestParam(name = "procEffectMode", required = false) String procEffectMode,
            @RequestParam(name = "combinationEffectMode", required = false) String combinationEffectMode,
            @RequestParam(name = "useMultithreading", required = false) String useMultithreading,
            @RequestParam(name = "maxThreads", required = false) String maxThreads,
            @RequestParam(name = "topGemmings", required = false) String topGemmings,
            HttpServletRequest request,
            HttpServletResponse response) {
        String base = ItemIconService.normalizeBaseUrl(iconBaseUrl);
        String template = iconTemplate == null ? "" : iconTemplate.trim();
        boolean hasTemplate = !template.isEmpty();
        if (base == null || (hasTemplate && !ItemIconService.isValidTemplate(template))) {
            return "redirect:/?open=settings&settingsError=icons";
        }
        GlobalSettings current = globalSettings.from(request);
        GlobalSettings updated;
        try {
            updated = new GlobalSettings(
                    parseBoolean(hideProfEnchants, current.hideProfEnchants()),
                    parseInt(procEffectMode, current.procEffectMode(),
                            GlobalSettings.PROC_SIMPLE, GlobalSettings.PROC_INTERPOLATION),
                    parseInt(combinationEffectMode, current.combinationEffectMode(),
                            GlobalSettings.COMBINATION_LINEAR, GlobalSettings.COMBINATION_EXACT),
                    parseBoolean(useMultithreading, current.useMultithreading()),
                    parseInt(maxThreads, current.maxThreads(),
                            GlobalSettings.MIN_THREADS, GlobalSettings.MAX_THREADS),
                    parseInt(topGemmings, current.topGemmings(), 1,
                            GlobalSettings.MAX_TOP_GEMMINGS));
        } catch (IllegalArgumentException e) {
            return "redirect:/?open=settings&settingsError=calculation";
        }
        storeCookie(response, GlobalSettingsService.HIDE_PROF_COOKIE,
                Boolean.toString(updated.hideProfEnchants()));
        storeCookie(response, GlobalSettingsService.PROC_MODE_COOKIE,
                Integer.toString(updated.procEffectMode()));
        storeCookie(response, GlobalSettingsService.COMBINATION_MODE_COOKIE,
                Integer.toString(updated.combinationEffectMode()));
        storeCookie(response, GlobalSettingsService.MULTITHREADING_COOKIE,
                Boolean.toString(updated.useMultithreading()));
        storeCookie(response, GlobalSettingsService.MAX_THREADS_COOKIE,
                Integer.toString(updated.maxThreads()));
        storeCookie(response, GlobalSettingsService.TOP_GEMMINGS_COOKIE,
                Integer.toString(updated.topGemmings()));
        globalSettings.apply(updated);
        storeCookie(response, ItemIconService.BASE_URL_COOKIE, base);
        if (hasTemplate) {
            storeCookie(response, ItemIconService.TEMPLATE_COOKIE, template);
        } else {
            expireCookie(response, ItemIconService.TEMPLATE_COOKIE);
        }
        return "redirect:/";
    }

    /**
     * htmx: save the optimizer-only performance settings. The fields live in the Optimize Advanced
     * tab; the values are global cookies applied by the interceptor.
     */
    @PostMapping("/settings/optimizer")
    public String saveOptimizerSettings(
            @RequestParam(name = "useMultithreading", required = false) String useMultithreading,
            @RequestParam(name = "maxThreads", required = false) String maxThreads,
            HttpServletResponse response,
            org.springframework.ui.Model model) {
        GlobalSettings current = globalSettings.current();
        boolean use;
        int threads;
        try {
            use = parseBoolean(useMultithreading, current.useMultithreading());
            threads = parseInt(maxThreads, current.maxThreads(),
                    GlobalSettings.MIN_THREADS, GlobalSettings.MAX_THREADS);
        } catch (IllegalArgumentException e) {
            model.addAttribute("optimizerSettingsError",
                    "Enter a thread count between " + GlobalSettings.MIN_THREADS + " and "
                            + GlobalSettings.MAX_THREADS + ".");
            return "fragments/optimize :: performanceStatus";
        }
        GlobalSettings updated = new GlobalSettings(current.hideProfEnchants(),
                current.procEffectMode(), current.combinationEffectMode(), use, threads,
                current.topGemmings());
        globalSettings.apply(updated);
        storeCookie(response, GlobalSettingsService.MULTITHREADING_COOKIE, Boolean.toString(use));
        storeCookie(response, GlobalSettingsService.MAX_THREADS_COOKIE, Integer.toString(threads));
        model.addAttribute("optimizerSettingsSaved", true);
        return "fragments/optimize :: performanceStatus";
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Not a boolean: " + value);
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("Out of range: " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + value);
        }
    }

    private static void storeCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static void expireCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
