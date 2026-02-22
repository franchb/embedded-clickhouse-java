package io.github.franchb.clickhouse.embedded;

public class StopTimeoutException extends EmbeddedClickHouseException {

    public StopTimeoutException() {
        super("embedded-clickhouse: server did not stop within timeout, killed");
    }
}
