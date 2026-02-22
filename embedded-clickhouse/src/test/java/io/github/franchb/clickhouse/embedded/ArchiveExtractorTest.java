package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchiveExtractorTest {

    @Test
    void isClickHouseBinaryPath() {
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("usr/bin/clickhouse")).isTrue();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("bin/clickhouse")).isTrue();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("clickhouse")).isTrue();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("some/prefix/usr/bin/clickhouse")).isTrue();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("some/prefix/bin/clickhouse")).isTrue();

        assertThat(ArchiveExtractor.isClickHouseBinaryPath("usr/bin/clickhouse-client")).isFalse();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("etc/clickhouse")).isFalse();
        assertThat(ArchiveExtractor.isClickHouseBinaryPath("share/clickhouse")).isFalse();
    }

    @Test
    void extractClickHouseBinary_missingArchive(@TempDir Path tempDir) {
        assertThatThrownBy(() -> ArchiveExtractor.extractClickHouseBinary(
                "/nonexistent/archive.tgz", tempDir.resolve("clickhouse").toString()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractClickHouseBinary_notATgz(@TempDir Path tempDir) throws IOException {
        Path badFile = tempDir.resolve("bad.tgz");
        Files.write(badFile, "not a gzip file".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> ArchiveExtractor.extractClickHouseBinary(
                badFile.toString(), tempDir.resolve("clickhouse").toString()))
                .isInstanceOf(Exception.class);
    }
}
