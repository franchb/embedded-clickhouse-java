package io.github.franchb.clickhouse.embedded;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BinaryDownloaderTest {

    @Test
    void downloadFile(@TempDir Path tempDir) throws IOException {
        String content = "hello clickhouse";

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = content.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            String dest = tempDir.resolve("downloaded").toString();
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/file";

            BinaryDownloader.downloadFile(url, dest);

            String got = new String(Files.readAllBytes(tempDir.resolve("downloaded")),
                    StandardCharsets.UTF_8);
            assertThat(got).isEqualTo(content);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadFile_httpError(@TempDir Path tempDir) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });
        server.start();

        try {
            String dest = tempDir.resolve("downloaded").toString();
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/file";

            assertThatThrownBy(() -> BinaryDownloader.downloadFile(url, dest))
                    .isInstanceOf(DownloadException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parseSHA512_standardFormat() {
        String hash = BinaryDownloader.parseSHA512(
                "abc123def456  myfile.tgz\n", "myfile.tgz");

        assertThat(hash).isEqualTo("abc123def456");
    }

    @Test
    void parseSHA512_filenameNotFound() {
        assertThatThrownBy(() -> BinaryDownloader.parseSHA512(
                "abc123  otherfile.tgz\n", "myfile.tgz"))
                .isInstanceOf(DownloadException.class)
                .hasMessageContaining("SHA512 hash not found");
    }

    @Test
    void parseSHA512_singleHashLine() {
        // A hash-only line with no filename should not match
        assertThatThrownBy(() -> BinaryDownloader.parseSHA512(
                "a66ab5824e9d826188a467170e7b24b031a21f936c4c5aa73e49d4c3a01dc136\n",
                "clickhouse-common-static-25.3.3.42-amd64.tgz"))
                .isInstanceOf(DownloadException.class);
    }

    @Test
    void fileSHA512(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("test");
        Files.write(file, content);

        String hash = BinaryDownloader.fileSHA512(file.toString());

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] expected = digest.digest(content);
        StringBuilder hex = new StringBuilder();
        for (byte b : expected) {
            hex.append(String.format("%02x", b & 0xff));
        }

        assertThat(hash).isEqualTo(hex.toString());
    }

    @Test
    void verifySHA512_success(@TempDir Path tempDir) throws Exception {
        byte[] content = "test content for sha512 verification".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("testfile.tgz");
        Files.write(file, content);

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = digest.digest(content);
        StringBuilder expectedHash = new StringBuilder();
        for (byte b : hashBytes) {
            expectedHash.append(String.format("%02x", b & 0xff));
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = expectedHash.toString() + "  testfile.tgz\n";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            String sha512url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hash";
            BinaryDownloader.verifySHA512(file.toString(), sha512url, "testfile.tgz");
            // Should not throw
        } finally {
            server.stop(0);
        }
    }

    @Test
    void verifySHA512_mismatch(@TempDir Path tempDir) throws Exception {
        byte[] content = "real content".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("testfile.tgz");
        Files.write(file, content);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = "0000000000000000000000000000000000000000000000000000000000000000"
                    + "0000000000000000000000000000000000000000000000000000000000000000"
                    + "  testfile.tgz\n";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            String sha512url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hash";

            assertThatThrownBy(() -> BinaryDownloader.verifySHA512(
                    file.toString(), sha512url, "testfile.tgz"))
                    .isInstanceOf(DownloadException.class)
                    .hasMessageContaining("SHA512 mismatch");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ensureBinary_explicitPath(@TempDir Path tempDir) throws IOException {
        Path binPath = tempDir.resolve("clickhouse");
        Files.write(binPath, "fake".getBytes(StandardCharsets.UTF_8));

        Config cfg = Config.defaultConfig().binaryPath(binPath.toString());
        String got = BinaryDownloader.ensureBinary(cfg);

        assertThat(got).isEqualTo(binPath.toString());
    }

    @Test
    void ensureBinary_explicitPathNotFound() {
        Config cfg = Config.defaultConfig().binaryPath("/nonexistent/clickhouse");

        assertThatThrownBy(() -> BinaryDownloader.ensureBinary(cfg))
                .isInstanceOf(EmbeddedClickHouseException.class)
                .hasMessageContaining("specified binary not found");
    }

    @Test
    void ensureBinary_cachedBinary(@TempDir Path tempDir) throws IOException {
        Config cfg = Config.defaultConfig().cachePath(tempDir.toString());

        String binPath = BinaryCache.cachedBinaryPath(tempDir.toString(), cfg.version);
        Path binFile = java.nio.file.Paths.get(binPath);
        Files.createDirectories(binFile.getParent());
        Files.write(binFile, "cached".getBytes(StandardCharsets.UTF_8));

        String got = BinaryDownloader.ensureBinary(cfg);
        assertThat(got).isEqualTo(binPath);
    }
}
