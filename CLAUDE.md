# embedded-clickhouse-java

Java port of [github.com/franchb/embedded-clickhouse](https://github.com/franchb/embedded-clickhouse).
Downloads, caches, and manages a real ClickHouse server process for Java tests.

## Build commands

```bash
./gradlew build --dry-run                          # verify project structure
./gradlew :embedded-clickhouse:compileJava         # compile core module
./gradlew :embedded-clickhouse:test                # unit tests
./gradlew :embedded-clickhouse:integrationTest     # real ClickHouse tests
./gradlew test                                     # all modules
```

## Module layout

| Module | Purpose |
|---|---|
| `embedded-clickhouse` | Core library |
| `embedded-clickhouse-junit4` | JUnit 4 `@Rule` |
| `embedded-clickhouse-junit5` | JUnit Jupiter (JUnit 6) `@ExtendWith` |
| `embedded-clickhouse-spring-boot` | Spring Boot 4 auto-configuration |
| `embedded-clickhouse-bom` | BOM for version alignment |

## Java baseline

All modules compile with a Java 25 toolchain (`--release 25`), configured in
`buildSrc/src/main/kotlin/java-library-conventions.gradle.kts`. Gradle provisions
the JDK via the foojay toolchain resolver if it is not installed locally.
Modern Java APIs and language features are allowed.

## Go → Java translation map

| Go concept | Java equivalent |
|---|---|
| `sync.RWMutex` | `ReentrantReadWriteLock` |
| `sync.Mutex` + double-checked | `ReentrantLock` + volatile flag |
| `io.Writer` for logger | `OutputStream` or SLF4J `Logger` |
| `exec.Cmd` | `ProcessBuilder` → `Process` |
| `os.MkdirAll` | `Files.createDirectories()` |
| `os.MkdirTemp` | `Files.createTempDirectory()` |
| `time.Duration` | `java.time.Duration` |
| error variables | `RuntimeException` subclasses |
| `context.Context` | `Duration` timeout parameter |
| `defer` | try-with-resources or try-finally |
| `filepath.Join` | `Paths.get(...).toString()` or `new File(parent, child)` |
| `net.Listen("tcp", ":0")` | `new ServerSocket(0)` → `getLocalPort()` |

## Go source file → Java class map

| Go file | Java class | Purpose |
|---|---|---|
| `config.go` | `Config.java` + `ClickHouseVersion.java` | Fluent config builder |
| `platform.go` | `PlatformResolver.java` | OS/arch → asset filename |
| `cache.go` | `BinaryCache.java` | XDG cache directory |
| `download.go` | `BinaryDownloader.java` | HTTP download + SHA512 |
| `extract.go` | `ArchiveExtractor.java` | tar.gz extraction |
| `server_config.go` | `ServerConfigWriter.java` | XML config generation |
| `process.go` | `ProcessManager.java` | Start/stop ClickHouse process |
| `health.go` | `HealthChecker.java` | HTTP /ping polling |
| `clickhouse.go` | `EmbeddedClickHouse.java` | Main public API |

## Package

`io.github.franchb.clickhouse.embedded`

## ClickHouse binaries cache

Cached at `~/.cache/embedded-clickhouse/` (XDG_CACHE_HOME).
Pre-cached versions: 25.3 and 25.8 linux-amd64.

## Key design decisions

- SLF4J for logging (not java.util.logging)
- Apache Commons Compress for tar.gz extraction
- No Guava dependency — use JDK utilities only
- Thread-safe: all mutable state behind locks
- Atomic file writes: write to `.tmp`, rename on success
