package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @Test
    void defaultConfig() {
        Config cfg = Config.defaultConfig();

        assertThat(cfg.version).isEqualTo(ClickHouseVersion.DEFAULT);
        assertThat(cfg.tcpPort).isZero();
        assertThat(cfg.httpPort).isZero();
        assertThat(cfg.startTimeout).isEqualTo(Duration.ofSeconds(30));
        assertThat(cfg.stopTimeout).isEqualTo(Duration.ofSeconds(10));
        assertThat(cfg.logger).isNotNull();
    }

    @Test
    void builderChaining() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("max_threads", "2");

        Config cfg = Config.defaultConfig()
                .version(ClickHouseVersion.V25_3)
                .tcpPort(19000)
                .httpPort(18123)
                .cachePath("/tmp/cache")
                .dataPath("/tmp/data")
                .binaryPath("/usr/bin/clickhouse")
                .binaryRepositoryURL("https://mirror.example.com")
                .startTimeout(Duration.ofSeconds(60))
                .stopTimeout(Duration.ofSeconds(20))
                .logger(buf)
                .settings(settings);

        assertThat(cfg.version).isEqualTo(ClickHouseVersion.V25_3);
        assertThat(cfg.tcpPort).isEqualTo(19000);
        assertThat(cfg.httpPort).isEqualTo(18123);
        assertThat(cfg.cachePath).isEqualTo("/tmp/cache");
        assertThat(cfg.dataPath).isEqualTo("/tmp/data");
        assertThat(cfg.binaryPath).isEqualTo("/usr/bin/clickhouse");
        assertThat(cfg.binaryRepositoryURL).isEqualTo("https://mirror.example.com");
        assertThat(cfg.startTimeout).isEqualTo(Duration.ofSeconds(60));
        assertThat(cfg.stopTimeout).isEqualTo(Duration.ofSeconds(20));
        assertThat(cfg.logger).isSameAs(buf);
        assertThat(cfg.settings).containsEntry("max_threads", "2");
    }

    @Test
    void builderImmutability() {
        Config base = Config.defaultConfig();
        Config modified = base.version(ClickHouseVersion.V25_3).tcpPort(9000);

        assertThat(base.version).isEqualTo(ClickHouseVersion.DEFAULT);
        assertThat(base.tcpPort).isZero();
        assertThat(modified.version).isEqualTo(ClickHouseVersion.V25_3);
        assertThat(modified.tcpPort).isEqualTo(9000);
    }

    @Test
    void settingsAreCopied() {
        Map<String, String> original = new HashMap<String, String>();
        original.put("key", "value");

        Config cfg = Config.defaultConfig().settings(original);
        original.put("key2", "value2");

        assertThat(cfg.settings).doesNotContainKey("key2");
    }
}
