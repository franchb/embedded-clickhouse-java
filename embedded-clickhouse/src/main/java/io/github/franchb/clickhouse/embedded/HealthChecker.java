package io.github.franchb.clickhouse.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * Polls the ClickHouse HTTP /ping endpoint until the server is ready.
 * Equivalent to health.go in the Go source.
 */
final class HealthChecker {

    private static final Logger LOG = LoggerFactory.getLogger(HealthChecker.class);

    private static final long POLL_INTERVAL_MS = 100;
    private static final int REQUEST_TIMEOUT_MS = 2000;

    private HealthChecker() {
    }

    /**
     * Polls /ping until HTTP 200 or the timeout expires.
     * Also checks if the process has exited prematurely.
     *
     * @param httpPort the ClickHouse HTTP port
     * @param timeout  maximum time to wait for readiness
     * @param process  the server process to monitor for early exit (may be null)
     * @throws EmbeddedClickHouseException if the server does not become ready within the timeout
     */
    static void waitForReady(int httpPort, Duration timeout, Process process) {
        String url = "http://127.0.0.1:" + httpPort + "/ping";
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        // Immediate poll to avoid unnecessary latency when the server is already up.
        if (ping(url)) {
            return;
        }

        while (System.currentTimeMillis() < deadline) {
            // Check if the process has exited prematurely.
            if (process != null && !process.isAlive()) {
                int exitCode = process.exitValue();
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: server process exited prematurely with code "
                                + exitCode);
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: interrupted while waiting for server", e);
            }

            if (ping(url)) {
                return;
            }
        }

        throw new EmbeddedClickHouseException(
                "embedded-clickhouse: server did not become ready within "
                        + timeout.toMillis() + "ms");
    }

    static boolean ping(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(REQUEST_TIMEOUT_MS);
            conn.setReadTimeout(REQUEST_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();

            // Drain the response body
            InputStream is = conn.getInputStream();
            byte[] buf = new byte[256];
            while (is.read(buf) != -1) {
                // discard
            }
            is.close();

            return responseCode == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
