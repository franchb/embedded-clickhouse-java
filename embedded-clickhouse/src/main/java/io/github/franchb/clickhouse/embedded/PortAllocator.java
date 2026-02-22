package io.github.franchb.clickhouse.embedded;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Allocates free TCP ports by binding to port 0 and immediately closing.
 * Equivalent to allocatePort() in process.go.
 */
final class PortAllocator {

    private PortAllocator() {
    }

    /**
     * Finds a free TCP port by binding to 127.0.0.1:0 and immediately closing.
     *
     * @return an available port number
     * @throws EmbeddedClickHouseException if port allocation fails
     */
    static int allocatePort() {
        try (ServerSocket socket = new ServerSocket(0, 1,
                InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: allocate port: " + e.getMessage(), e);
        }
    }
}
