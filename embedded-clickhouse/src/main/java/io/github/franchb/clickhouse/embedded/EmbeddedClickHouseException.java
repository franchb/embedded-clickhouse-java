package io.github.franchb.clickhouse.embedded;

/**
 * Base exception for all embedded-clickhouse errors.
 */
public class EmbeddedClickHouseException extends RuntimeException {

    public EmbeddedClickHouseException(String message) {
        super(message);
    }

    public EmbeddedClickHouseException(String message, Throwable cause) {
        super(message, cause);
    }
}
