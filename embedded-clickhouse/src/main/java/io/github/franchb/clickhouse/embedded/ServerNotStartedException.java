package io.github.franchb.clickhouse.embedded;

public class ServerNotStartedException extends EmbeddedClickHouseException {

    public ServerNotStartedException() {
        super("embedded-clickhouse: server has not been started");
    }
}
