package io.github.franchb.clickhouse.embedded;

import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for an embedded ClickHouse server.
 * <p>
 * Each setter returns a new Config instance (immutable copy-on-write),
 * mirroring the Go API:
 * <pre>
 * Config config = Config.defaultConfig()
 *     .version(ClickHouseVersion.V25_3)
 *     .tcpPort(19000)
 *     .startTimeout(Duration.ofSeconds(60));
 * </pre>
 */
public final class Config {

    final ClickHouseVersion version;
    final int tcpPort;
    final int httpPort;
    final String cachePath;
    final String dataPath;
    final String binaryPath;
    final String binaryRepositoryURL;
    final Duration startTimeout;
    final Duration stopTimeout;
    final OutputStream logger;
    final Map<String, String> settings;

    private Config(ClickHouseVersion version, int tcpPort, int httpPort,
                   String cachePath, String dataPath, String binaryPath,
                   String binaryRepositoryURL, Duration startTimeout,
                   Duration stopTimeout, OutputStream logger,
                   Map<String, String> settings) {
        this.version = version;
        this.tcpPort = tcpPort;
        this.httpPort = httpPort;
        this.cachePath = cachePath;
        this.dataPath = dataPath;
        this.binaryPath = binaryPath;
        this.binaryRepositoryURL = binaryRepositoryURL;
        this.startTimeout = startTimeout;
        this.stopTimeout = stopTimeout;
        this.logger = logger;
        this.settings = settings;
    }

    /**
     * Returns a Config with sensible defaults.
     */
    public static Config defaultConfig() {
        return new Config(
                ClickHouseVersion.DEFAULT,
                0, 0,
                null, null, null, null,
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                System.out,
                Collections.<String, String>emptyMap()
        );
    }

    public Config version(ClickHouseVersion v) {
        return new Config(v, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config tcpPort(int port) {
        return new Config(version, port, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config httpPort(int port) {
        return new Config(version, tcpPort, port, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config cachePath(String path) {
        return new Config(version, tcpPort, httpPort, path, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config dataPath(String path) {
        return new Config(version, tcpPort, httpPort, cachePath, path, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config binaryPath(String path) {
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, path,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, settings);
    }

    public Config binaryRepositoryURL(String url) {
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                url, startTimeout, stopTimeout, logger, settings);
    }

    public Config startTimeout(Duration d) {
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, d, stopTimeout, logger, settings);
    }

    public Config stopTimeout(Duration d) {
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, d, logger, settings);
    }

    public Config logger(OutputStream out) {
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, out, settings);
    }

    /**
     * Sets arbitrary ClickHouse server settings.
     * The provided map is copied; subsequent caller mutations do not affect the Config.
     */
    public Config settings(Map<String, String> s) {
        Map<String, String> copy = Collections.unmodifiableMap(new HashMap<String, String>(s));
        return new Config(version, tcpPort, httpPort, cachePath, dataPath, binaryPath,
                binaryRepositoryURL, startTimeout, stopTimeout, logger, copy);
    }
}
