package rawr.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Item source backed by a URL template (DECISIONS #31), e.g.
 * {@code https://wotlkdb.com/?item={id}&xml}. Plain {@link HttpClient} with a browser
 * {@code User-Agent}, a 15 s connect and request timeout and no retries; redirects are followed.
 */
public final class UrlTemplateItemSource implements ItemSourceProvider {

    /** Modern browser UA; the legacy fork sends the same kind of string. */
    public static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36";

    static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String urlTemplate;

    public UrlTemplateItemSource(String urlTemplate) {
        if (urlTemplate == null || !urlTemplate.contains(ItemSourceTemplates.ID_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                    "Item source template must contain " + ItemSourceTemplates.ID_PLACEHOLDER);
        }
        this.urlTemplate = urlTemplate.trim();
    }

    /** The concrete URL for an item ID, with {@value ItemSourceTemplates#ID_PLACEHOLDER} replaced. */
    public String urlFor(int itemId) {
        return urlTemplate.replace(ItemSourceTemplates.ID_PLACEHOLDER, Integer.toString(itemId));
    }

    @Override
    public String fetchXml(int itemId) throws IOException {
        String url = urlFor(itemId);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
    }
}
