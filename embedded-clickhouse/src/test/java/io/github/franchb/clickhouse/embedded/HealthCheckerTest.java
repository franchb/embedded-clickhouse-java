package io.github.franchb.clickhouse.embedded;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthCheckerTest {

    @Test
    void waitForReady_immediateSuccess() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", exchange -> {
            byte[] response = "Ok.\n".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        int port = server.getAddress().getPort();

        try {
            assertThatCode(() ->
                    HealthChecker.waitForReady(port, Duration.ofSeconds(5), null))
                    .doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void waitForReady_timeout() {
        int port = PortAllocator.allocatePort();

        assertThatThrownBy(() ->
                HealthChecker.waitForReady(port, Duration.ofMillis(300), null))
                .isInstanceOf(EmbeddedClickHouseException.class)
                .hasMessageContaining("did not become ready");
    }

    @Test
    void waitForReady_delayedStart() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.stop(0); // Close immediately so nothing is listening yet.

        // Start serving after a delay.
        HttpServer delayedServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        delayedServer.createContext("/ping", exchange -> {
            byte[] response = "Ok.\n".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> delayedServer.start(), 200, TimeUnit.MILLISECONDS);

        try {
            assertThatCode(() ->
                    HealthChecker.waitForReady(port, Duration.ofSeconds(5), null))
                    .doesNotThrowAnyException();
        } finally {
            delayedServer.stop(0);
            scheduler.shutdown();
        }
    }

    @Test
    void ping_returnsTrue() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", exchange -> {
            byte[] response = "Ok.\n".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        int port = server.getAddress().getPort();

        try {
            boolean result = HealthChecker.ping("http://127.0.0.1:" + port + "/ping");
            assertThatCode(() -> {}).doesNotThrowAnyException();
            org.assertj.core.api.Assertions.assertThat(result).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ping_returnsFalseForClosedPort() {
        int port = PortAllocator.allocatePort();
        boolean result = HealthChecker.ping("http://127.0.0.1:" + port + "/ping");
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }
}
