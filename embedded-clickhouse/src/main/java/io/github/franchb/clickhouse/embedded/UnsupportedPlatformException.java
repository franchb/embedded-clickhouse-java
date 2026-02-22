package io.github.franchb.clickhouse.embedded;

public class UnsupportedPlatformException extends EmbeddedClickHouseException {

    public UnsupportedPlatformException(String os, String arch) {
        super("embedded-clickhouse: unsupported platform: " + os + "/" + arch);
    }
}
