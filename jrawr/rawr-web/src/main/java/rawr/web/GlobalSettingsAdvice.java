package rawr.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSettingsAdvice {
    private final GlobalSettingsService settings;

    public GlobalSettingsAdvice(GlobalSettingsService settings) {
        this.settings = settings;
    }

    @ModelAttribute("globalSettings")
    public GlobalSettings globalSettings(HttpServletRequest request) {
        return settings.from(request);
    }
}
