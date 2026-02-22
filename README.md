# embedded-clickhouse-java

[![CI](https://github.com/franchb/embedded-clickhouse-java/actions/workflows/ci.yml/badge.svg)](https://github.com/franchb/embedded-clickhouse-java/actions/workflows/ci.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/franchb/embedded-clickhouse-java/badge)](https://scorecard.dev/viewer/?uri=github.com/franchb/embedded-clickhouse-java)

Run a real ClickHouse database locally on Linux or macOS as part of a Java application or test.

This provides a much higher level of confidence than mocking or using Docker. It requires no external dependencies beyond the JDK — no Docker, no testcontainers, no pre-installed ClickHouse.

Java port of [embedded-clickhouse](https://github.com/franchb/embedded-clickhouse) (Go). Inspired by [fergusstrange/embedded-postgres](https://github.com/fergusstrange/embedded-postgres). ClickHouse binaries are fetched directly from [official GitHub releases](https://github.com/ClickHouse/ClickHouse/releases).

## Why not testcontainers?

testcontainers needs a Docker daemon. In CI that means picking your poison:

**Docker-in-Docker (DinD)** requires `privileged: true` — a container breakout waiting to happen. **Docker-outside-of-Docker (DooD)** mounts the Docker socket, which is a root-equivalent backdoor. Both approaches break in CI environments with strict security policies.

ClickHouse ships as a single self-contained binary. This library downloads it, verifies its SHA-512 checksum, and runs it as a child process — no daemon, no socket, no sidecar, no privilege escalation. The binary starts in under a second, listens on a random port, and exits when your test exits.

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `embedded-clickhouse` | Core | Server lifecycle, download, config (Java 8+) |
| `embedded-clickhouse-junit5` | JUnit 5 | `@EmbeddedClickHouseTest` annotation + parameter injection |
| `embedded-clickhouse-junit4` | JUnit 4 | `ClickHouseRule` for `@ClassRule` usage |
| `embedded-clickhouse-spring-boot` | Spring Boot | Auto-configuration via `embedded.clickhouse.*` properties |
| `embedded-clickhouse-bom` | BOM | Version-aligned dependency management |

## Quick start

### JUnit 5 (recommended)

```java
@EmbeddedClickHouseTest
class MyTest {
    @Test
    void query(EmbeddedClickHouse server) throws Exception {
        try (Connection conn = DriverManager.getConnection(server.jdbcUrl())) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT 1");
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}
```

### JUnit 4

```java
public class MyTest {
    @ClassRule
    public static final ClickHouseRule clickHouse = new ClickHouseRule();

    @Test
    public void query() throws Exception {
        try (Connection conn = DriverManager.getConnection(clickHouse.getJdbcUrl())) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT 1");
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}
```

### Direct API

```java
EmbeddedClickHouse server = EmbeddedClickHouse.create();
server.start();
try {
    // server.jdbcUrl() => "jdbc:clickhouse://127.0.0.1:<port>/default"
    // server.httpURL() => "http://127.0.0.1:<port>"
    // server.dsn()     => "clickhouse://127.0.0.1:<port>/default"
} finally {
    server.stop();
}
```

### Custom configuration

```java
EmbeddedClickHouse server = EmbeddedClickHouse.create(
    Config.defaultConfig()
        .version(ClickHouseVersion.V25_3)
        .tcpPort(19000)
        .httpPort(18123)
        .dataPath("/tmp/ch-data")
        .startTimeout(Duration.ofSeconds(60))
        .logger(OutputStream.nullOutputStream())
        .settings(Collections.singletonMap("max_server_memory_usage", "2147483648"))
);
server.start();
try {
    // ...
} finally {
    server.stop();
}
```

### Spring Boot

```yaml
# application-test.yml
embedded:
  clickhouse:
    enabled: true
    version: "25.8.16.34-lts"
```

The auto-configuration creates and starts an `EmbeddedClickHouse` bean, available for injection.

## Defaults

| Configuration | Default |
|---|---|
| Version | `V25_8` (25.8.16.34-lts) |
| TCP Port | Auto-allocated |
| HTTP Port | Auto-allocated |
| Cache Path | `$XDG_CACHE_HOME/embedded-clickhouse/` or `~/.cache/embedded-clickhouse/` |
| Data Path | Temporary directory (removed on stop) |
| Start Timeout | 30 seconds |
| Stop Timeout | 10 seconds |

## Configuration reference

All configuration methods return a new `Config` instance (immutable copy-on-write):

```java
Config base = Config.defaultConfig();
Config custom = base.version(ClickHouseVersion.V25_3); // base is unchanged
```

| Method | Description |
|---|---|
| `version(ClickHouseVersion)` | ClickHouse version to download and run |
| `tcpPort(int)` | Native protocol port (0 = auto-allocate) |
| `httpPort(int)` | HTTP interface port (0 = auto-allocate) |
| `cachePath(String)` | Override binary cache directory |
| `dataPath(String)` | Persistent data directory (survives stop) |
| `binaryPath(String)` | Use a pre-existing binary, skip download |
| `binaryRepositoryURL(String)` | Custom mirror URL (default: GitHub releases) |
| `startTimeout(Duration)` | Max wait for server readiness |
| `stopTimeout(Duration)` | Max wait for graceful shutdown |
| `logger(OutputStream)` | Destination for server stdout/stderr |
| `settings(Map<String, String>)` | Arbitrary ClickHouse server settings |

## Available versions

| Constant | Version | Channel |
|---|---|---|
| `V26_1` | 26.1.3.52-stable | Stable |
| `V25_8` | 25.8.16.34-lts | LTS (default) |
| `V25_3` | 25.3.14.14-lts | LTS |

Any version string can be used — these constants are provided for convenience. Pass the full version from a [ClickHouse release tag](https://github.com/ClickHouse/ClickHouse/releases):

```java
new ClickHouseVersion("24.8.6.70-lts")
```

## Server accessors

After `start()` returns successfully:

| Method | Example return value |
|---|---|
| `tcpAddr()` | `"127.0.0.1:19000"` |
| `httpAddr()` | `"127.0.0.1:18123"` |
| `dsn()` | `"clickhouse://127.0.0.1:19000/default"` |
| `httpURL()` | `"http://127.0.0.1:18123"` |
| `jdbcUrl()` | `"jdbc:clickhouse://127.0.0.1:18123/default"` |

## Platform support

| OS | Arch | Asset type |
|---|---|---|
| Linux | amd64 | `.tgz` archive |
| Linux | arm64 | `.tgz` archive |
| macOS | amd64 | Raw binary |
| macOS | arm64 | Raw binary |

## CI caching

The downloaded ClickHouse binary (~200 MB for Linux, ~130 MB for macOS) is cached at the cache path. In CI, cache this directory to avoid re-downloading on every run:

```yaml
# GitHub Actions
- uses: actions/cache@v5
  with:
    path: ~/.cache/embedded-clickhouse
    key: clickhouse-${{ runner.os }}-${{ runner.arch }}-25.8.16.34-lts
```

## How it works

1. **Download** — fetches the ClickHouse binary from GitHub releases (or a configured mirror) on first use
2. **Verify** — checks SHA-512 hash for downloaded assets
3. **Cache** — stores the extracted binary at `~/.cache/embedded-clickhouse/` for reuse
4. **Configure** — generates a minimal XML config with allocated ports and a temp data directory
5. **Start** — launches `clickhouse server` as a child process
6. **Health check** — polls `GET /ping` every 100 ms until the server responds
7. **Stop** — sends SIGTERM, waits for graceful shutdown, then SIGKILL if needed; cleans up the temp directory

## Requirements

- Java 8+ (compiled to Java 8 bytecode)
- Linux or macOS (amd64 or arm64)

## License

Apache 2.0
