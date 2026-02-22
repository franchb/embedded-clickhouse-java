package io.github.franchb.clickhouse.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Manages the ClickHouse server process lifecycle (start, stop).
 * Equivalent to process.go in the Go source.
 */
final class ProcessManager {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessManager.class);

    private ProcessManager() {
    }

    /**
     * Launches the ClickHouse server process.
     *
     * @param binaryPath path to the ClickHouse binary
     * @param configPath path to the generated config.xml
     * @param logger     output stream for stdout/stderr, may be null
     * @return the started Process
     */
    static Process startProcess(String binaryPath, String configPath,
                                OutputStream logger) {
        ProcessBuilder pb = new ProcessBuilder(
                binaryPath, "server", "--config-file=" + configPath);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // Redirect process output to logger in a background thread.
            OutputStream out = logger != null ? logger : System.out;
            Thread reader = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }
                } catch (IOException e) {
                    // Process closed, expected
                }
            }, "clickhouse-output-reader");
            reader.setDaemon(true);
            reader.start();

            return process;
        } catch (IOException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: start process: " + e.getMessage(), e);
        }
    }

    /**
     * Sends SIGTERM and waits for graceful shutdown, then destroys forcibly if needed.
     */
    static void stopProcess(Process process, Duration timeout) {
        if (process == null || !process.isAlive()) {
            return;
        }

        // Send SIGTERM (Process.destroy() on Unix sends SIGTERM)
        process.destroy();

        try {
            boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                // Force kill after timeout.
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                throw new StopTimeoutException();
            }

            int exitCode = process.exitValue();
            // Exit code 143 = SIGTERM, which is expected.
            if (exitCode != 0 && exitCode != 143) {
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: server exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: interrupted while stopping server", e);
        }
    }
}
