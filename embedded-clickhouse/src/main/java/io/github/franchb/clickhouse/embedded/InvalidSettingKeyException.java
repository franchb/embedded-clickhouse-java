package io.github.franchb.clickhouse.embedded;

public class InvalidSettingKeyException extends EmbeddedClickHouseException {

    public InvalidSettingKeyException(String key) {
        super("embedded-clickhouse: invalid setting key: \"" + key
                + "\" (must match [a-zA-Z][a-zA-Z0-9_]*)");
    }
}
