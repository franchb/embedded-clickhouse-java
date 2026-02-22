package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;

class ProcessManagerTest {

    @Test
    void stopProcess_nullProcess() {
        assertThatCode(() -> ProcessManager.stopProcess(null, Duration.ofSeconds(5)))
                .doesNotThrowAnyException();
    }
}
