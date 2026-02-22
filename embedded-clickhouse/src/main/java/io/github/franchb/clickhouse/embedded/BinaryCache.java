package io.github.franchb.clickhouse.embedded;

import java.io.File;
import java.nio.file.Paths;

/**
 * Manages the OS-appropriate cache directory for downloaded ClickHouse binaries.
 * Equivalent to cache.go in the Go source.
 */
final class BinaryCache {

    static final String CACHE_SUBDIR = "embedded-clickhouse";

    private BinaryCache() {
    }

    /**
     * Returns the directory used to store cached ClickHouse binaries.
     * Priority: explicit override > $XDG_CACHE_HOME/embedded-clickhouse > ~/.cache/embedded-clickhouse.
     */
    static String cacheDir(String override) {
        if (override != null && !override.isEmpty()) {
            return override;
        }

        String xdg = System.getenv("XDG_CACHE_HOME");
        if (xdg != null && !xdg.isEmpty()) {
            return Paths.get(xdg, CACHE_SUBDIR).toString();
        }

        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: cannot determine home directory");
        }

        return Paths.get(home, ".cache", CACHE_SUBDIR).toString();
    }

    /**
     * Returns the full path to a cached ClickHouse binary for the given version and platform.
     */
    static String cachedBinaryPath(String cacheDir, ClickHouseVersion version) {
        String safeVersion = version.toString().replace(File.separatorChar, '_');
        String os = PlatformResolver.detectOS();
        String arch = PlatformResolver.detectArch();
        return Paths.get(cacheDir,
                "clickhouse-" + safeVersion + "-" + os + "-" + arch).toString();
    }
}
