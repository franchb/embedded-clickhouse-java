package io.github.franchb.clickhouse.embedded.junit4;

import io.github.franchb.clickhouse.embedded.Config;
import io.github.franchb.clickhouse.embedded.EmbeddedClickHouse;
import org.junit.rules.ExternalResource;

/**
 * JUnit 4 {@link org.junit.Rule @Rule} that starts an embedded ClickHouse server
 * before each test class and stops it after.
 * <p>
 * Usage:
 * <pre>
 * public class MyTest {
 *     &#64;ClassRule
 *     public static final ClickHouseRule clickHouse = new ClickHouseRule();
 *
 *     &#64;Test
 *     public void test() {
 *         String jdbcUrl = clickHouse.getJdbcUrl();
 *         // ...
 *     }
 * }
 * </pre>
 */
public class ClickHouseRule extends ExternalResource {

    private final Config config;
    private EmbeddedClickHouse server;

    /**
     * Creates a rule with default configuration.
     */
    public ClickHouseRule() {
        this(Config.defaultConfig());
    }

    /**
     * Creates a rule with the given configuration.
     */
    public ClickHouseRule(Config config) {
        this.config = config;
    }

    @Override
    protected void before() throws Throwable {
        server = EmbeddedClickHouse.create(config);
        server.start();
    }

    @Override
    protected void after() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Log but don't fail the test on cleanup
                System.err.println("embedded-clickhouse: stop failed: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the TCP address (e.g. "127.0.0.1:19000").
     */
    public String getTcpAddr() {
        return server.tcpAddr();
    }

    /**
     * Returns the HTTP address (e.g. "127.0.0.1:18123").
     */
    public String getHttpAddr() {
        return server.httpAddr();
    }

    /**
     * Returns a ClickHouse DSN (e.g. "clickhouse://127.0.0.1:19000/default").
     */
    public String getDsn() {
        return server.dsn();
    }

    /**
     * Returns the HTTP URL (e.g. "http://127.0.0.1:18123").
     */
    public String getHttpUrl() {
        return server.httpURL();
    }

    /**
     * Returns the JDBC URL (e.g. "jdbc:clickhouse://127.0.0.1:18123/default").
     */
    public String getJdbcUrl() {
        return server.jdbcUrl();
    }

    /**
     * Returns the underlying EmbeddedClickHouse instance.
     */
    public EmbeddedClickHouse getServer() {
        return server;
    }
}
