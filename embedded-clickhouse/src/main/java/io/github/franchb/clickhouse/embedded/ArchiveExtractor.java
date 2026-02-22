package io.github.franchb.clickhouse.embedded;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extracts the ClickHouse binary from tar.gz archives.
 * Equivalent to extract.go in the Go source.
 */
final class ArchiveExtractor {

    private ArchiveExtractor() {
    }

    /**
     * Returns true if the tar entry path looks like the main ClickHouse server binary.
     */
    static boolean isClickHouseBinaryPath(String name) {
        String clean = name.replace('\\', '/');
        // Normalize path
        clean = Paths.get(clean).normalize().toString().replace('\\', '/');

        return clean.endsWith("/usr/bin/clickhouse")
                || clean.endsWith("/bin/clickhouse")
                || clean.equals("usr/bin/clickhouse")
                || clean.equals("bin/clickhouse")
                || clean.equals("clickhouse");
    }

    /**
     * Extracts the clickhouse binary from a .tgz archive.
     */
    static void extractClickHouseBinary(String archivePath, String destPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (!entry.isFile()) {
                    continue;
                }
                if (!isClickHouseBinaryPath(entry.getName())) {
                    continue;
                }
                writeExecutable(tis, destPath);
                return;
            }
        }

        throw new BinaryNotFoundException(
                "embedded-clickhouse: binary not found in archive: " + archivePath);
    }

    /**
     * Writes reader content to destPath atomically via a temp file.
     */
    static void writeExecutable(InputStream in, String destPath) throws IOException {
        Path dest = Paths.get(destPath).normalize();
        if (dest.toString().contains("..")) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: invalid destination path: " + destPath);
        }

        Files.createDirectories(dest.getParent());

        String tmpPath = destPath + ".tmp";
        File tmpFile = new File(tmpPath);

        try (OutputStream out = new FileOutputStream(tmpFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            tmpFile.delete();
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: write binary: " + e.getMessage(), e);
        }

        if (!tmpFile.setExecutable(true, false)) {
            tmpFile.delete();
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: chmod binary failed");
        }

        File destFile = dest.toFile();
        if (!tmpFile.renameTo(destFile)) {
            tmpFile.delete();
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: rename temp file failed");
        }
    }
}
