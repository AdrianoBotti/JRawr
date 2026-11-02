package rawr.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for the icon site-base normalization, the known-host layout map and the
 * optional advanced template cookie. Icons are always hotlinked; there is no local cache path.
 */
class ItemIconServiceTest {

    private ItemIconService service;

    @BeforeEach
    void setUp() {
        service = new ItemIconService("https://wow.zamimg.com");
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void normalizeBaseUrlKeepsOnlySchemeHostAndPort() {
        assertThat(ItemIconService.normalizeBaseUrl("  HTTPS://WotLKDB.com/some/path/?q=1#frag "))
                .isEqualTo("https://wotlkdb.com");
        assertThat(ItemIconService.normalizeBaseUrl("http://localhost:8080/icons/"))
                .isEqualTo("http://localhost:8080");
        assertThat(ItemIconService.normalizeBaseUrl("https://wotlkdb.com"))
                .isEqualTo("https://wotlkdb.com");
        assertThat(ItemIconService.normalizeBaseUrl("https://wotlkdb.com/"))
                .isEqualTo("https://wotlkdb.com");
    }

    @Test
    void normalizeBaseUrlRejectsNonSiteBases() {
        assertThat(ItemIconService.normalizeBaseUrl(null)).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("   ")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("wotlkdb.com")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("ftp://wotlkdb.com")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("javascript:alert(1)")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("https://user@wotlkdb.com")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("https://wotlkdb.com:99999")).isNull();
        assertThat(ItemIconService.normalizeBaseUrl("https://ho st")).isNull();
    }

    @Test
    void isValidTemplateRequiresHttpSchemeAndIconPlaceholder() {
        assertThat(ItemIconService.isValidTemplate("https://x.example/i/{icon}.jpg")).isTrue();
        assertThat(ItemIconService.isValidTemplate("http://x.example/{icon}")).isTrue();
        assertThat(ItemIconService.isValidTemplate("ftp://x.example/{icon}.jpg")).isFalse();
        assertThat(ItemIconService.isValidTemplate("https://x.example/i.jpg")).isFalse();
        assertThat(ItemIconService.isValidTemplate("https://x.example/{icon} two")).isFalse();
        assertThat(ItemIconService.isValidTemplate(null)).isFalse();
    }

    @Test
    void propertyFallbackUsesTheZamimgLayoutAndIconNamesAreLowercased() {
        assertThat(service.url(ItemIconService.EXAMPLE_ICON))
                .isEqualTo("https://wow.zamimg.com/images/wow/icons/large/"
                        + ItemIconService.EXAMPLE_ICON + ".jpg");
        assertThat(service.url("INV_HELMET_150BLACK"))
                .isEqualTo("https://wow.zamimg.com/images/wow/icons/large/"
                        + ItemIconService.EXAMPLE_ICON + ".jpg");
        assertThat(service.url(null)).isNull();
        assertThat(service.url("  ")).isNull();
    }

    @Test
    void wotlkdbCookieUsesTheAoWowLayout() {
        requestWith(new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com"));
        assertThat(service.baseUrl()).isEqualTo("https://wotlkdb.com");
        assertThat(service.url(ItemIconService.EXAMPLE_ICON))
                .isEqualTo("https://wotlkdb.com/static/images/wow/icons/large/"
                        + ItemIconService.EXAMPLE_ICON + ".jpg");
    }

    @Test
    void templateCookieOverridesTheHostLayout() {
        requestWith(
                new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com"),
                new Cookie(ItemIconService.TEMPLATE_COOKIE, "https://cdn.example/i/{icon}.png"));
        assertThat(service.template()).isEqualTo("https://cdn.example/i/{icon}.png");
        assertThat(service.url(ItemIconService.EXAMPLE_ICON))
                .isEqualTo("https://cdn.example/i/" + ItemIconService.EXAMPLE_ICON + ".png");
    }

    @Test
    void invalidTemplateCookieIsIgnored() {
        requestWith(
                new Cookie(ItemIconService.BASE_URL_COOKIE, "https://wotlkdb.com"),
                new Cookie(ItemIconService.TEMPLATE_COOKIE, "https://cdn.example/i.jpg"));
        assertThat(service.template()).isEmpty();
        assertThat(service.url(ItemIconService.EXAMPLE_ICON))
                .isEqualTo("https://wotlkdb.com/static/images/wow/icons/large/"
                        + ItemIconService.EXAMPLE_ICON + ".jpg");
    }

    @Test
    void legacyFullUrlCookieIsNormalizedToItsOrigin() {
        requestWith(new Cookie(ItemIconService.BASE_URL_COOKIE,
                "https://x.example/static/images/wow/icons/large/"));
        assertThat(service.baseUrl()).isEqualTo("https://x.example");
        assertThat(service.url(ItemIconService.EXAMPLE_ICON))
                .isEqualTo("https://x.example/static/images/wow/icons/large/"
                        + ItemIconService.EXAMPLE_ICON + ".jpg");
    }

    @Test
    void layoutTemplateMapsKnownHostsAndSubdomains() {
        assertThat(ItemIconService.layoutTemplate("https://icons.evowow.com"))
                .isEqualTo("https://icons.evowow.com/static/images/wow/icons/large/{icon}.jpg");
        assertThat(ItemIconService.layoutTemplate("https://wotlkdb.com"))
                .isEqualTo("https://wotlkdb.com/static/images/wow/icons/large/{icon}.jpg");
        assertThat(ItemIconService.layoutTemplate("https://wowhead.com"))
                .isEqualTo("https://wow.zamimg.com/images/wow/icons/large/{icon}.jpg");
        assertThat(ItemIconService.layoutTemplate("https://zamimg.com"))
                .isEqualTo("https://zamimg.com/images/wow/icons/large/{icon}.jpg");
        assertThat(ItemIconService.layoutTemplate("https://render.worldofwarcraft.com"))
                .isEqualTo("https://render.worldofwarcraft.com/icons/56/{icon}.jpg");
        assertThat(ItemIconService.layoutTemplate("https://anything.example"))
                .isEqualTo("https://anything.example/static/images/wow/icons/large/{icon}.jpg");
    }

    private static void requestWith(Cookie... cookies) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookies);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
