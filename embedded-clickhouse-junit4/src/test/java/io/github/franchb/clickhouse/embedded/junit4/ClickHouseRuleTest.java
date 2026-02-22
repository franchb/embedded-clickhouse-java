package io.github.franchb.clickhouse.embedded.junit4;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClickHouseRuleTest {

    @Test
    void canBeCreatedWithDefaultConfig() {
        ClickHouseRule rule = new ClickHouseRule();
        assertThat(rule).isNotNull();
    }

    @Test
    void canBeCreatedWithCustomConfig() {
        io.github.franchb.clickhouse.embedded.Config config =
                io.github.franchb.clickhouse.embedded.Config.defaultConfig()
                        .tcpPort(19000);
        ClickHouseRule rule = new ClickHouseRule(config);
        assertThat(rule).isNotNull();
    }
}
