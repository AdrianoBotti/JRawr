package rawr.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GlobalSettingsInterceptor implements HandlerInterceptor {
    private final GlobalSettingsService settings;

    public GlobalSettingsInterceptor(GlobalSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) {
        settings.applyFrom(request);
        return true;
    }
}
