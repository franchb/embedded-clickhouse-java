package io.github.franchb.clickhouse.embedded;

public class DownloadException extends EmbeddedClickHouseException {

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
