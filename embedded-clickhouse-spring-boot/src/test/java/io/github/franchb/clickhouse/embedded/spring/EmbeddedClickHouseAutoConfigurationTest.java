package io.github.franchb.clickhouse.embedded.spring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedClickHouseAutoConfigurationTest {

    @Test
    void propertiesDefaults() {
        EmbeddedClickHouseProperties props = new EmbeddedClickHouseProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getVersion()).isNull();
        assertThat(props.getTcpPort()).isZero();
        assertThat(props.getHttpPort()).isZero();
        assertThat(props.getStartTimeoutSeconds()).isEqualTo(30);
        assertThat(props.getStopTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    void propertiesSetters() {
        EmbeddedClickHouseProperties props = new EmbeddedClickHouseProperties();
        props.setEnabled(false);
        props.setVersion("25.3.14.14-lts");
        props.setTcpPort(19000);
        props.setHttpPort(18123);
        props.setCachePath("/tmp/cache");
        props.setDataPath("/tmp/data");
        props.setBinaryPath("/usr/bin/clickhouse");
        props.setStartTimeoutSeconds(60);
        props.setStopTimeoutSeconds(20);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getVersion()).isEqualTo("25.3.14.14-lts");
        assertThat(props.getTcpPort()).isEqualTo(19000);
        assertThat(props.getHttpPort()).isEqualTo(18123);
        assertThat(props.getCachePath()).isEqualTo("/tmp/cache");
        assertThat(props.getDataPath()).isEqualTo("/tmp/data");
        assertThat(props.getBinaryPath()).isEqualTo("/usr/bin/clickhouse");
        assertThat(props.getStartTimeoutSeconds()).isEqualTo(60);
        assertThat(props.getStopTimeoutSeconds()).isEqualTo(20);
    }
}
