package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PortAllocatorTest {

    @Test
    void allocatePort_returnsValidPort() {
        int port = PortAllocator.allocatePort();

        assertThat(port).isGreaterThan(0);
        assertThat(port).isLessThanOrEqualTo(65535);
    }

    @Test
    void allocatePort_returnsUniquePortsMostly() {
        Set<Integer> ports = new HashSet<Integer>();

        for (int i = 0; i < 10; i++) {
            int port = PortAllocator.allocatePort();
            ports.add(port);
        }

        // Port reuse is technically possible but very unlikely in 10 allocations.
        assertThat(ports.size()).isGreaterThan(5);
    }
}
