package rawr.data;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Provider tests against a loopback {@link HttpServer} and a temp directory. No external network
 * access (AGENTS rule 9); the URL is built from the template and the loopback port.
 */
class ItemSourceProviderTest {

    @Test
    void urlTemplateProviderFetchesThePayload() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            byte[] body = ("<wowhead><item id=\"42\"><name><![CDATA[Q " + query + "]]></name></item></wowhead>")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String template = "http://127.0.0.1:" + server.getAddress().getPort() + "/?item={id}&xml";
            UrlTemplateItemSource source = new UrlTemplateItemSource(template);

            assertThat(source.urlFor(42)).endsWith("/?item=42&xml");
            assertThat(source.fetchXml(42)).contains("item=42").contains("id=\"42\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void urlTemplateProviderReportsHttpErrorsWithoutRetrying() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int[] calls = { 0 };
        server.createContext("/", exchange -> {
            calls[0]++;
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            UrlTemplateItemSource source = new UrlTemplateItemSource(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/?item={id}");
            assertThatThrownBy(() -> source.fetchXml(7))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("404");
            assertThat(calls[0]).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void templatesWithoutTheIdPlaceholderAreRejected() {
        assertThatThrownBy(() -> new UrlTemplateItemSource("https://x.example/item"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fileItemSourceReadsById(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("51281.xml"), "<aowow><item id=\"51281\"/></aowow>");
        FileItemSource source = new FileItemSource(dir);

        assertThat(source.fetchXml(51281)).contains("id=\"51281\"");
        assertThatThrownBy(() -> source.fetchXml(1))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("1");
    }
}
