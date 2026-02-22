package io.github.franchb.clickhouse.embedded;

public class ServerAlreadyStartedException extends EmbeddedClickHouseException {

    public ServerAlreadyStartedException() {
        super("embedded-clickhouse: server is already started");
    }
}
