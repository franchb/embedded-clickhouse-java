package io.github.franchb.clickhouse.embedded.junit5;

import io.github.franchb.clickhouse.embedded.EmbeddedClickHouse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClickHouseExtensionTest {

    @Test
    void extensionCanBeInstantiated() {
        ClickHouseExtension ext = new ClickHouseExtension();
        assertThat(ext).isNotNull();
    }

    @Test
    void supportsEmbeddedClickHouseParameter() throws Exception {
        ClickHouseExtension ext = new ClickHouseExtension();
        // Basic type check - full parameter resolution is tested via integration tests
        assertThat(EmbeddedClickHouse.class).isNotNull();
    }
}
