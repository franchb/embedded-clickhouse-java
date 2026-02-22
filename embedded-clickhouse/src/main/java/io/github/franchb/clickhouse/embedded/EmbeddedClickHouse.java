package io.github.franchb.clickhouse.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages a ClickHouse server process for testing.
 * <p>
 * Downloads, caches, and manages a real ClickHouse server process.
 * <p>
 * Usage:
 * <pre>
 * EmbeddedClickHouse server = EmbeddedClickHouse.builder()
 *     .version(ClickHouseVersion.V25_8)
 *     .build();
 * server.start();
 * try {
 *     String jdbcUrl = "jdbc:clickhouse://" + server.httpAddr();
 *     // ... run tests
 * } finally {
 *     server.stop();
 * }
 * </pre>
 */
public final class EmbeddedClickHouse {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedClickHouse.class);

    private final Config config;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile boolean started;
    private Process process;
    private String tmpDir;
    private int tcpPort;
    private int httpPort;

    private EmbeddedClickHouse(Config config) {
        this.config = config;
    }

    /**
     * Creates a new EmbeddedClickHouse with default configuration.
     */
    public static EmbeddedClickHouse create() {
        return new EmbeddedClickHouse(Config.defaultConfig());
    }

    /**
     * Creates a new EmbeddedClickHouse with the given configuration.
     */
    public static EmbeddedClickHouse create(Config config) {
        return new EmbeddedClickHouse(config);
    }

    /**
     * Downloads the ClickHouse binary (if needed), generates config, and starts the server.
     *
     * @throws ServerAlreadyStartedException if the server is already running
     * @throws EmbeddedClickHouseException   on any startup failure
     */
    public void start() {
        lock.writeLock().lock();
        try {
            if (started) {
                throw new ServerAlreadyStartedException();
            }

            List<Runnable> cleanups = new ArrayList<Runnable>();
            boolean success = false;

            try {
                // Resolve binary.
                String binPath = BinaryDownloader.ensureBinary(config);

                // Allocate ports.
                int tcp = config.tcpPort;
                if (tcp == 0) {
                    tcp = PortAllocator.allocatePort();
                }

                int http = config.httpPort;
                if (http == 0) {
                    http = PortAllocator.allocatePort();
                }

                // Create temp directory or use configured data path.
                final String workDir;
                if (config.dataPath != null && !config.dataPath.isEmpty()) {
                    workDir = config.dataPath;
                    Files.createDirectories(Paths.get(workDir));
                } else {
                    workDir = Files.createTempDirectory("embedded-clickhouse-").toString();
                    cleanups.add(new Runnable() {
                        @Override
                        public void run() {
                            deleteRecursively(Paths.get(workDir));
                        }
                    });
                }

                // Write server config.
                String configPath = ServerConfigWriter.writeServerConfig(
                        workDir, tcp, http, config.settings);

                // Start process.
                OutputStream logger = config.logger != null ? config.logger : System.out;
                Process proc = ProcessManager.startProcess(binPath, configPath, logger);

                final Process procRef = proc;
                cleanups.add(new Runnable() {
                    @Override
                    public void run() {
                        ProcessManager.stopProcess(procRef, config.stopTimeout);
                    }
                });

                // Wait for server to be ready.
                HealthChecker.waitForReady(http, config.startTimeout, proc);

                this.process = proc;
                this.tmpDir = workDir;
                this.tcpPort = tcp;
                this.httpPort = http;
                this.started = true;
                success = true;
            } catch (IOException e) {
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: startup failed: " + e.getMessage(), e);
            } finally {
                if (!success) {
                    // Run cleanups in reverse order.
                    for (int i = cleanups.size() - 1; i >= 0; i--) {
                        try {
                            cleanups.get(i).run();
                        } catch (Exception e) {
                            LOG.warn("Cleanup failed", e);
                        }
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gracefully shuts down the ClickHouse server and cleans up resources.
     *
     * @throws ServerNotStartedException   if the server has not been started
     * @throws EmbeddedClickHouseException on any shutdown failure
     */
    public void stop() {
        lock.writeLock().lock();
        try {
            if (!started) {
                throw new ServerNotStartedException();
            }

            List<Exception> errors = new ArrayList<Exception>();

            try {
                ProcessManager.stopProcess(process, config.stopTimeout);
            } catch (Exception e) {
                errors.add(e);
            }

            // Only remove temp dir if no explicit data path was set.
            if ((config.dataPath == null || config.dataPath.isEmpty())
                    && tmpDir != null) {
                try {
                    deleteRecursively(Paths.get(tmpDir));
                } catch (Exception e) {
                    errors.add(new EmbeddedClickHouseException(
                            "embedded-clickhouse: remove temp dir: " + e.getMessage(), e));
                }
            }

            started = false;
            process = null;
            tcpPort = 0;
            httpPort = 0;

            if (!errors.isEmpty()) {
                EmbeddedClickHouseException first =
                        (errors.get(0) instanceof EmbeddedClickHouseException)
                                ? (EmbeddedClickHouseException) errors.get(0)
                                : new EmbeddedClickHouseException(errors.get(0).getMessage(),
                                errors.get(0));
                for (int i = 1; i < errors.size(); i++) {
                    first.addSuppressed(errors.get(i));
                }
                throw first;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the TCP address for the ClickHouse native protocol (e.g. "127.0.0.1:19000").
     */
    public String tcpAddr() {
        lock.readLock().lock();
        try {
            return "127.0.0.1:" + tcpPort;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the HTTP address for the ClickHouse HTTP interface (e.g. "127.0.0.1:18123").
     */
    public String httpAddr() {
        lock.readLock().lock();
        try {
            return "127.0.0.1:" + httpPort;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a ClickHouse DSN for use with JDBC drivers
     * (e.g. "clickhouse://127.0.0.1:19000/default").
     */
    public String dsn() {
        lock.readLock().lock();
        try {
            return "clickhouse://127.0.0.1:" + tcpPort + "/default";
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the base HTTP URL (e.g. "http://127.0.0.1:18123").
     */
    public String httpURL() {
        lock.readLock().lock();
        try {
            return "http://127.0.0.1:" + httpPort;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the JDBC URL for the ClickHouse HTTP interface
     * (e.g. "jdbc:clickhouse://127.0.0.1:18123/default").
     */
    public String jdbcUrl() {
        lock.readLock().lock();
        try {
            return "jdbc:clickhouse://127.0.0.1:" + httpPort + "/default";
        } finally {
            lock.readLock().unlock();
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Failed to delete directory: {}", path, e);
        }
    }
}
