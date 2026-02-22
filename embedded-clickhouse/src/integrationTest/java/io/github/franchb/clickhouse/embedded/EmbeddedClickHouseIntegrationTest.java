package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddedClickHouseIntegrationTest {

    @Test
    void startAndStop() {
        EmbeddedClickHouse server = EmbeddedClickHouse.create(
                Config.defaultConfig()
                        .startTimeout(Duration.ofSeconds(60))
                        .logger(new ByteArrayOutputStream()));

        server.start();
        try {
            assertThat(server.tcpAddr()).matches("127\\.0\\.0\\.1:\\d+");
            assertThat(server.httpAddr()).matches("127\\.0\\.0\\.1:\\d+");
            assertThat(server.dsn()).startsWith("clickhouse://127.0.0.1:");
            assertThat(server.httpURL()).startsWith("http://127.0.0.1:");
            assertThat(server.jdbcUrl()).startsWith("jdbc:clickhouse://127.0.0.1:");

            // Verify the server responds to ping.
            String pingResult = httpGet(server.httpURL() + "/ping");
            assertThat(pingResult.trim()).isEqualTo("Ok.");
        } finally {
            server.stop();
        }
    }

    @Test
    void startAndStop_withQuery() {
        EmbeddedClickHouse server = EmbeddedClickHouse.create(
                Config.defaultConfig()
                        .startTimeout(Duration.ofSeconds(60))
                        .logger(new ByteArrayOutputStream()));

        server.start();
        try {
            // Run a simple query via HTTP interface.
            String result = httpGet(server.httpURL()
                    + "/?query=" + urlEncode("SELECT 1"));
            assertThat(result.trim()).isEqualTo("1");

            // Run SELECT version().
            String version = httpGet(server.httpURL()
                    + "/?query=" + urlEncode("SELECT version()"));
            assertThat(version.trim()).isNotEmpty();
        } finally {
            server.stop();
        }
    }

    @Test
    void doubleStartThrows() {
        EmbeddedClickHouse server = EmbeddedClickHouse.create(
                Config.defaultConfig()
                        .startTimeout(Duration.ofSeconds(60))
                        .logger(new ByteArrayOutputStream()));

        server.start();
        try {
            assertThatThrownBy(server::start)
                    .isInstanceOf(ServerAlreadyStartedException.class);
        } finally {
            server.stop();
        }
    }

    @Test
    void stopBeforeStartThrows() {
        EmbeddedClickHouse server = EmbeddedClickHouse.create();

        assertThatThrownBy(server::stop)
                .isInstanceOf(ServerNotStartedException.class);
    }

    @Test
    void customSettings() {
        EmbeddedClickHouse server = EmbeddedClickHouse.create(
                Config.defaultConfig()
                        .startTimeout(Duration.ofSeconds(60))
                        .settings(java.util.Collections.singletonMap(
                                "max_concurrent_queries", "50"))
                        .logger(new ByteArrayOutputStream()));

        server.start();
        try {
            String result = httpGet(server.httpURL()
                    + "/?query=" + urlEncode("SELECT 1"));
            assertThat(result.trim()).isEqualTo("1");
        } finally {
            server.stop();
        }
    }

    private static String httpGet(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (InputStream is = conn.getInputStream()) {
                byte[] buf = new byte[4096];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    bos.write(buf, 0, bytesRead);
                }
                return bos.toString("UTF-8");
            }
        } catch (IOException e) {
            throw new RuntimeException("HTTP GET failed: " + urlString, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
