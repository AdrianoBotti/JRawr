package rawr.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final GlobalSettingsInterceptor globalSettingsInterceptor;

    public WebConfig(GlobalSettingsInterceptor globalSettingsInterceptor) {
        this.globalSettingsInterceptor = globalSettingsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalSettingsInterceptor);
    }
}
