package rawr.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import rawr.base.calculations.CalculationsBase;
import rawr.base.effects.SpecialEffect;

@Service
public class GlobalSettingsService {
    public static final String HIDE_PROF_COOKIE = "jrawr.hideProfEnchants";
    public static final String PROC_MODE_COOKIE = "jrawr.procEffectMode";
    public static final String COMBINATION_MODE_COOKIE = "jrawr.combinationEffectMode";
    public static final String MULTITHREADING_COOKIE = "jrawr.useMultithreading";
    public static final String MAX_THREADS_COOKIE = "jrawr.maxThreads";
    public static final String TOP_GEMMINGS_COOKIE = "jrawr.topGemmings";

    private volatile GlobalSettings current = GlobalSettings.defaults();

    public GlobalSettings from(HttpServletRequest request) {
        GlobalSettings defaults = GlobalSettings.defaults();
        Cookie[] cookies = request == null ? null : request.getCookies();
        if (cookies == null) {
            return defaults;
        }
        boolean hide = defaults.hideProfEnchants();
        int proc = defaults.procEffectMode();
        int combination = defaults.combinationEffectMode();
        boolean multithreading = defaults.useMultithreading();
        int maxThreads = defaults.maxThreads();
        int topGemmings = defaults.topGemmings();
        for (Cookie cookie : cookies) {
            if (cookie == null || cookie.getName() == null) {
                continue;
            }
            String value = cookie.getValue();
            switch (cookie.getName()) {
                case HIDE_PROF_COOKIE -> {
                    if (value != null && (value.equalsIgnoreCase("true")
                            || value.equalsIgnoreCase("false"))) {
                        hide = Boolean.parseBoolean(value);
                    }
                }
                case PROC_MODE_COOKIE -> {
                    int parsed = parseInt(value);
                    if (parsed >= GlobalSettings.PROC_SIMPLE
                            && parsed <= GlobalSettings.PROC_INTERPOLATION) {
                        proc = parsed;
                    }
                }
                case COMBINATION_MODE_COOKIE -> {
                    int parsed = parseInt(value);
                    if (parsed >= GlobalSettings.COMBINATION_LINEAR
                            && parsed <= GlobalSettings.COMBINATION_EXACT) {
                        combination = parsed;
                    }
                }
                case MULTITHREADING_COOKIE -> {
                    if (value != null && (value.equalsIgnoreCase("true")
                            || value.equalsIgnoreCase("false"))) {
                        multithreading = Boolean.parseBoolean(value);
                    }
                }
                case MAX_THREADS_COOKIE -> {
                    int parsed = parseInt(value);
                    if (parsed >= GlobalSettings.MIN_THREADS
                            && parsed <= GlobalSettings.MAX_THREADS) {
                        maxThreads = parsed;
                    }
                }
                case TOP_GEMMINGS_COOKIE -> {
                    int parsed = parseInt(value);
                    if (parsed >= 1 && parsed <= GlobalSettings.MAX_TOP_GEMMINGS) {
                        topGemmings = parsed;
                    }
                }
                default -> {
                }
            }
        }
        return new GlobalSettings(hide, proc, combination, multithreading, maxThreads, topGemmings);
    }

    public GlobalSettings current() {
        return current;
    }

    /** Stores one of the global settings cookies (path, 30 days, HttpOnly, SameSite=Lax). */
    public static void storeCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public synchronized void apply(GlobalSettings settings) {
        current = settings;
        CalculationsBase.setHideProfEnchants(settings.hideProfEnchants());
        SpecialEffect.setCalculationMode(switch (settings.procEffectMode()) {
            case GlobalSettings.PROC_SIMPLE -> SpecialEffect.CalculationMode.Simple;
            case GlobalSettings.PROC_ADVANCED -> SpecialEffect.CalculationMode.Advanced;
            default -> SpecialEffect.CalculationMode.Interpolation;
        });
        SpecialEffect.setCombinationMode(switch (settings.combinationEffectMode()) {
            case GlobalSettings.COMBINATION_LINEAR -> SpecialEffect.CombinationMode.Linear;
            case GlobalSettings.COMBINATION_EXACT -> SpecialEffect.CombinationMode.Exact;
            default -> SpecialEffect.CombinationMode.Interpolated;
        });
    }

    public void applyFrom(HttpServletRequest request) {
        apply(from(request));
    }

    private static int parseInt(String value) {
        try {
            return value == null ? Integer.MIN_VALUE : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }
}
