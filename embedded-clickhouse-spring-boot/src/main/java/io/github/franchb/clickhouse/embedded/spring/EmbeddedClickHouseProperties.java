package io.github.franchb.clickhouse.embedded.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for embedded ClickHouse.
 */
@ConfigurationProperties(prefix = "embedded.clickhouse")
public class EmbeddedClickHouseProperties {

    private boolean enabled = true;
    private String version;
    private int tcpPort;
    private int httpPort;
    private String cachePath;
    private String dataPath;
    private String binaryPath;
    private long startTimeoutSeconds = 30;
    private long stopTimeoutSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public long getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }

    public void setStartTimeoutSeconds(long startTimeoutSeconds) {
        this.startTimeoutSeconds = startTimeoutSeconds;
    }

    public long getStopTimeoutSeconds() {
        return stopTimeoutSeconds;
    }

    public void setStopTimeoutSeconds(long stopTimeoutSeconds) {
        this.stopTimeoutSeconds = stopTimeoutSeconds;
    }
}
