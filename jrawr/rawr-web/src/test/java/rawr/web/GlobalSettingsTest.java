package rawr.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rawr.base.calculations.CalculationsBase;
import rawr.base.effects.SpecialEffect;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalSettingsTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataBootstrap dataBootstrap;

    @Autowired
    private GlobalSettingsService settings;

    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("jrawr.data-dir", () -> REPO_ROOT.resolve("jrawr/data").toString());
    }

    @BeforeEach
    void loadData() {
        dataBootstrap.load();
        settings.apply(GlobalSettings.defaults());
    }

    @AfterEach
    void reset() {
        settings.apply(GlobalSettings.defaults());
    }

    @Test
    void settingsModalRendersGlobalCalculationDefaults() throws Exception {
        String html = html(mockMvc.perform(get("/settings"))
                .andExpect(status().isOk()).andReturn());
        assertThat(html).contains(
                "name=\"hideProfEnchants\"",
                "name=\"procEffectMode\"",
                "name=\"combinationEffectMode\"",
                "value=\"true\" selected=\"selected\">Hide when the profession is missing",
                "value=\"3\" selected=\"selected\">Interpolated",
                "value=\"1\" selected=\"selected\">Interpolated (original default)");
    }

    @Test
    void postStoresAndAppliesGlobalSettings() throws Exception {
        MvcResult posted = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://wotlkdb.com")
                        .param("hideProfEnchants", "false")
                        .param("procEffectMode", "1")
                        .param("combinationEffectMode", "2")
                        .param("useMultithreading", "false")
                        .param("maxThreads", "2")
                        .param("topGemmings", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        assertThat(cookie(posted, GlobalSettingsService.HIDE_PROF_COOKIE)).isEqualTo("false");
        assertThat(cookie(posted, GlobalSettingsService.PROC_MODE_COOKIE)).isEqualTo("1");
        assertThat(cookie(posted, GlobalSettingsService.COMBINATION_MODE_COOKIE)).isEqualTo("2");
        assertThat(cookie(posted, GlobalSettingsService.MULTITHREADING_COOKIE)).isEqualTo("false");
        assertThat(cookie(posted, GlobalSettingsService.MAX_THREADS_COOKIE)).isEqualTo("2");
        assertThat(cookie(posted, GlobalSettingsService.TOP_GEMMINGS_COOKIE)).isEqualTo("5");

        assertThat(CalculationsBase.getHideProfEnchants()).isFalse();
        assertThat(SpecialEffect.getCalculationMode()).isEqualTo(SpecialEffect.CalculationMode.Simple);
        assertThat(SpecialEffect.getCombinationMode()).isEqualTo(SpecialEffect.CombinationMode.Exact);
        assertThat(settings.current().useMultithreading()).isFalse();
        assertThat(settings.current().maxThreads()).isEqualTo(2);
        assertThat(settings.current().topGemmings()).isEqualTo(5);
    }

    @Test
    void optimizerPerformanceSettingsSaveFromTheOptimizeEndpoint() throws Exception {
        MvcResult posted = mockMvc.perform(post("/settings/optimizer")
                        .param("useMultithreading", "false")
                        .param("maxThreads", "2"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(posted.getResponse().getContentAsString())
                .contains("Saved.");
        assertThat(cookie(posted, GlobalSettingsService.MULTITHREADING_COOKIE)).isEqualTo("false");
        assertThat(cookie(posted, GlobalSettingsService.MAX_THREADS_COOKIE)).isEqualTo("2");
        assertThat(settings.current().useMultithreading()).isFalse();
        assertThat(settings.current().maxThreads()).isEqualTo(2);

        MvcResult invalid = mockMvc.perform(post("/settings/optimizer")
                        .param("maxThreads", "0"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(invalid.getResponse().getContentAsString()).contains("thread count");
    }

    @Test
    void interceptorAppliesCookiesToTheEngine() throws Exception {
        mockMvc.perform(get("/settings")
                        .cookie(new Cookie(GlobalSettingsService.HIDE_PROF_COOKIE, "false"),
                                new Cookie(GlobalSettingsService.PROC_MODE_COOKIE, "2"),
                                new Cookie(GlobalSettingsService.COMBINATION_MODE_COOKIE, "0"),
                                new Cookie(GlobalSettingsService.MULTITHREADING_COOKIE, "false"),
                                new Cookie(GlobalSettingsService.MAX_THREADS_COOKIE, "3")))
                .andExpect(status().isOk());

        assertThat(CalculationsBase.getHideProfEnchants()).isFalse();
        assertThat(SpecialEffect.getCalculationMode()).isEqualTo(SpecialEffect.CalculationMode.Advanced);
        assertThat(SpecialEffect.getCombinationMode()).isEqualTo(SpecialEffect.CombinationMode.Linear);
        assertThat(settings.current().useMultithreading()).isFalse();
        assertThat(settings.current().maxThreads()).isEqualTo(3);
    }

    @Test
    void invalidCalculationSettingsAreRejectedWithoutCookies() throws Exception {
        MvcResult posted = mockMvc.perform(post("/settings")
                        .param("iconBaseUrl", "https://wotlkdb.com")
                        .param("procEffectMode", "9")
                        .param("maxThreads", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?open=settings&settingsError=calculation"))
                .andReturn();
        assertThat(posted.getResponse().getCookie(GlobalSettingsService.PROC_MODE_COOKIE)).isNull();
        assertThat(posted.getResponse().getCookie(GlobalSettingsService.MAX_THREADS_COOKIE)).isNull();
        assertThat(CalculationsBase.getHideProfEnchants()).isTrue();
        assertThat(SpecialEffect.getCalculationMode())
                .isEqualTo(SpecialEffect.CalculationMode.Interpolation);
    }

    private static String cookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).as("cookie %s", name).isNotNull();
        return cookie.getValue();
    }

    private static String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
