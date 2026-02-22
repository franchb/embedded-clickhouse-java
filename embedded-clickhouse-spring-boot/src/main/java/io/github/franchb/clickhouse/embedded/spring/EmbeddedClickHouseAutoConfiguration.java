package io.github.franchb.clickhouse.embedded.spring;

import io.github.franchb.clickhouse.embedded.ClickHouseVersion;
import io.github.franchb.clickhouse.embedded.Config;
import io.github.franchb.clickhouse.embedded.EmbeddedClickHouse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.time.Duration;

/**
 * Spring Boot auto-configuration that creates and starts an {@link EmbeddedClickHouse}
 * server as a Spring bean.
 * <p>
 * Enabled by default. Disable with {@code embedded.clickhouse.enabled=false}.
 */
@Configuration
@ConditionalOnClass(EmbeddedClickHouse.class)
@ConditionalOnProperty(prefix = "embedded.clickhouse", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EmbeddedClickHouseProperties.class)
public class EmbeddedClickHouseAutoConfiguration {

    private EmbeddedClickHouse server;

    @Bean
    public EmbeddedClickHouse embeddedClickHouse(EmbeddedClickHouseProperties properties) {
        Config config = Config.defaultConfig();

        if (properties.getVersion() != null && !properties.getVersion().isEmpty()) {
            config = config.version(new ClickHouseVersion(properties.getVersion()));
        }
        if (properties.getTcpPort() > 0) {
            config = config.tcpPort(properties.getTcpPort());
        }
        if (properties.getHttpPort() > 0) {
            config = config.httpPort(properties.getHttpPort());
        }
        if (properties.getCachePath() != null) {
            config = config.cachePath(properties.getCachePath());
        }
        if (properties.getDataPath() != null) {
            config = config.dataPath(properties.getDataPath());
        }
        if (properties.getBinaryPath() != null) {
            config = config.binaryPath(properties.getBinaryPath());
        }
        config = config.startTimeout(Duration.ofSeconds(properties.getStartTimeoutSeconds()));
        config = config.stopTimeout(Duration.ofSeconds(properties.getStopTimeoutSeconds()));

        server = EmbeddedClickHouse.create(config);
        server.start();
        return server;
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }
}
