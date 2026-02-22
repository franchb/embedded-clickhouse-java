package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryCacheTest {

    @Test
    void cacheDir_override() {
        String dir = BinaryCache.cacheDir("/custom/path");
        assertThat(dir).isEqualTo("/custom/path");
    }

    @Test
    void cacheDir_default() {
        // Clear XDG to test default behavior
        String dir = BinaryCache.cacheDir(null);
        String home = System.getProperty("user.home");
        String xdg = System.getenv("XDG_CACHE_HOME");

        if (xdg != null && !xdg.isEmpty()) {
            assertThat(dir).isEqualTo(xdg + File.separator + BinaryCache.CACHE_SUBDIR);
        } else {
            assertThat(dir).isEqualTo(
                    home + File.separator + ".cache" + File.separator + BinaryCache.CACHE_SUBDIR);
        }
    }

    @Test
    void cachedBinaryPath_containsVersionAndPlatform() {
        String path = BinaryCache.cachedBinaryPath("/cache", ClickHouseVersion.V25_8);

        assertThat(path).startsWith("/cache" + File.separator + "clickhouse-25.8.16.34-lts-");
        assertThat(path).contains(PlatformResolver.detectOS());
        assertThat(path).contains(PlatformResolver.detectArch());
    }
}
