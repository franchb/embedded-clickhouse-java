package io.github.franchb.clickhouse.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Downloads ClickHouse binaries from GitHub releases and verifies SHA512 checksums.
 * Equivalent to download.go in the Go source.
 */
final class BinaryDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(BinaryDownloader.class);

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 600_000; // 10 minutes

    private static final ReentrantLock DOWNLOAD_LOCK = new ReentrantLock();

    private BinaryDownloader() {
    }

    /**
     * Returns the path to a ClickHouse binary, downloading it if necessary.
     */
    static String ensureBinary(Config cfg) {
        if (cfg.binaryPath != null && !cfg.binaryPath.isEmpty()) {
            File f = new File(cfg.binaryPath);
            if (!f.exists()) {
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: specified binary not found: " + cfg.binaryPath);
            }
            return cfg.binaryPath;
        }

        String dir = BinaryCache.cacheDir(cfg.cachePath);
        String binPath = BinaryCache.cachedBinaryPath(dir, cfg.version);

        if (new File(binPath).exists()) {
            return binPath;
        }

        DOWNLOAD_LOCK.lock();
        try {
            // Double-check after acquiring lock.
            if (new File(binPath).exists()) {
                return binPath;
            }

            PlatformResolver.PlatformAsset asset =
                    PlatformResolver.resolveCurrentPlatformAsset(cfg.version);

            String url = PlatformResolver.downloadURL(
                    cfg.binaryRepositoryURL, cfg.version, asset);

            LOG.info("Downloading ClickHouse v{}...", cfg.version);

            switch (asset.assetType) {
                case ARCHIVE:
                    downloadAndExtract(cfg, url, asset, binPath);
                    break;
                case RAW_BINARY:
                    downloadRawBinary(cfg, asset, url, binPath);
                    break;
                default:
                    throw new EmbeddedClickHouseException(
                            "embedded-clickhouse: unknown asset type: " + asset.assetType);
            }

            LOG.info("Done.");
            return binPath;
        } finally {
            DOWNLOAD_LOCK.unlock();
        }
    }

    private static void downloadAndExtract(Config cfg, String url,
                                           PlatformResolver.PlatformAsset asset,
                                           String binPath) {
        String dir = BinaryCache.cacheDir(cfg.cachePath);

        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: create cache dir: " + e.getMessage(), e);
        }

        String archivePath = Paths.get(dir, asset.filename + ".tmp").toString();
        try {
            downloadFile(url, archivePath);

            String sha512url = PlatformResolver.sha512URL(
                    cfg.binaryRepositoryURL, cfg.version, asset);
            verifySHA512(archivePath, sha512url, asset.filename);

            ArchiveExtractor.extractClickHouseBinary(archivePath, binPath);
        } catch (IOException e) {
            throw new DownloadException(
                    "embedded-clickhouse: download and extract failed: " + e.getMessage(), e);
        } finally {
            new File(archivePath).delete();
        }
    }

    private static void downloadRawBinary(Config cfg,
                                          PlatformResolver.PlatformAsset asset,
                                          String url, String binPath) {
        try {
            Files.createDirectories(Paths.get(binPath).getParent());
        } catch (IOException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: create cache dir: " + e.getMessage(), e);
        }

        String tmp = binPath + ".tmp";
        try {
            downloadFile(url, tmp);

            String sha512url = PlatformResolver.sha512URL(
                    cfg.binaryRepositoryURL, cfg.version, asset);
            verifySHA512(tmp, sha512url, asset.filename);

            File tmpFile = new File(tmp);
            if (!tmpFile.setExecutable(true, false)) {
                tmpFile.delete();
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: chmod binary failed");
            }

            if (!tmpFile.renameTo(new File(binPath))) {
                tmpFile.delete();
                throw new EmbeddedClickHouseException(
                        "embedded-clickhouse: rename binary failed");
            }
        } catch (EmbeddedClickHouseException e) {
            new File(tmp).delete();
            throw e;
        }
    }

    static void downloadFile(String urlString, String destPath) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new DownloadException(
                        "embedded-clickhouse: download failed: " + urlString
                                + ": HTTP " + responseCode);
            }

            try (InputStream in = conn.getInputStream();
                 OutputStream out = new FileOutputStream(destPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (DownloadException e) {
            new File(destPath).delete();
            throw e;
        } catch (IOException e) {
            new File(destPath).delete();
            throw new DownloadException(
                    "embedded-clickhouse: download " + urlString + ": " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static void verifySHA512(String filePath, String sha512URL, String expectedFilename) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(sha512URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                LOG.warn("SHA512 not available for {} (HTTP {}), skipping verification",
                        expectedFilename, responseCode);
                return;
            }

            byte[] buf = new byte[4096];
            StringBuilder body = new StringBuilder();
            try (InputStream in = conn.getInputStream()) {
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    body.append(new String(buf, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            String expectedHash = parseSHA512(body.toString(), expectedFilename);
            String actualHash = fileSHA512(filePath);

            if (!actualHash.equals(expectedHash)) {
                new File(filePath).delete();
                throw new DownloadException(
                        "embedded-clickhouse: SHA512 mismatch: " + expectedFilename
                                + ": expected " + expectedHash + ", got " + actualHash);
            }
        } catch (DownloadException e) {
            throw e;
        } catch (IOException e) {
            throw new DownloadException(
                    "embedded-clickhouse: download SHA512: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Parses a sha512sum-format string and returns the hex hash for the given filename.
     * Format: "&lt;hash&gt;  &lt;filename&gt;\n".
     */
    static String parseSHA512(String content, String filename) {
        for (String line : content.trim().split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2 && parts[1].equals(filename)) {
                return parts[0].toLowerCase();
            }
        }
        throw new DownloadException(
                "embedded-clickhouse: SHA512 hash not found: " + filename);
    }

    static String fileSHA512(String path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (FileInputStream fis = new FileInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: SHA-512 not available", e);
        } catch (IOException e) {
            throw new EmbeddedClickHouseException(
                    "embedded-clickhouse: compute SHA512: " + e.getMessage(), e);
        }
    }
}
