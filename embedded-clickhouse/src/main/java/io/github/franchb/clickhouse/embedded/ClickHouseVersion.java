package io.github.franchb.clickhouse.embedded;

/**
 * Represents a ClickHouse server version string.
 */
public final class ClickHouseVersion {

    /** ClickHouse 26.1 (stable channel). */
    public static final ClickHouseVersion V26_1 = new ClickHouseVersion("26.1.3.52-stable");

    /** ClickHouse 25.8 (LTS channel). */
    public static final ClickHouseVersion V25_8 = new ClickHouseVersion("25.8.16.34-lts");

    /** ClickHouse 25.3 (LTS channel). */
    public static final ClickHouseVersion V25_3 = new ClickHouseVersion("25.3.14.14-lts");

    /** Default version used when none is specified. */
    public static final ClickHouseVersion DEFAULT = V25_8;

    private final String version;

    public ClickHouseVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("version must not be null or empty");
        }
        this.version = version;
    }

    /**
     * Returns the full version string (e.g. "25.8.16.34-lts").
     */
    @Override
    public String toString() {
        return version;
    }

    /**
     * Returns the numeric part of the version, stripping -stable/-lts/-testing suffix.
     * e.g. "25.8.16.34-lts" → "25.8.16.34".
     */
    String numericVersion() {
        int i = version.lastIndexOf('-');
        if (i != -1) {
            String suffix = version.substring(i + 1);
            if ("lts".equals(suffix) || "stable".equals(suffix) || "testing".equals(suffix)) {
                return version.substring(0, i);
            }
        }
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClickHouseVersion)) return false;
        ClickHouseVersion that = (ClickHouseVersion) o;
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }
}
