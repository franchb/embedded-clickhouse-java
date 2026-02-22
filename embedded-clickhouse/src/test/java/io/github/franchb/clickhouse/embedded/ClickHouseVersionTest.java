package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClickHouseVersionTest {

    @Test
    void toStringReturnsVersion() {
        assertThat(ClickHouseVersion.V25_8.toString()).isEqualTo("25.8.16.34-lts");
        assertThat(ClickHouseVersion.V25_3.toString()).isEqualTo("25.3.14.14-lts");
        assertThat(ClickHouseVersion.V26_1.toString()).isEqualTo("26.1.3.52-stable");
    }

    @Test
    void equality() {
        ClickHouseVersion v1 = new ClickHouseVersion("25.8.16.34-lts");
        assertThat(v1).isEqualTo(ClickHouseVersion.V25_8);
        assertThat(v1.hashCode()).isEqualTo(ClickHouseVersion.V25_8.hashCode());
    }

    @Test
    void nullVersionThrows() {
        assertThatThrownBy(() -> new ClickHouseVersion(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyVersionThrows() {
        assertThatThrownBy(() -> new ClickHouseVersion(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultIsV25_8() {
        assertThat(ClickHouseVersion.DEFAULT).isEqualTo(ClickHouseVersion.V25_8);
    }
}
