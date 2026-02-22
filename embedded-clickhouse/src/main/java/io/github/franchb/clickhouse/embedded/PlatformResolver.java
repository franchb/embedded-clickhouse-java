package io.github.franchb.clickhouse.embedded;

import java.util.Locale;

/**
 * Resolves the current OS and architecture to ClickHouse release asset names.
 * Equivalent to platform.go in the Go source.
 */
final class PlatformResolver {

    static final String DEFAULT_BASE_URL =
            "https://github.com/ClickHouse/ClickHouse/releases/download";

    enum AssetType {
        ARCHIVE,     // .tgz archive (Linux)
        RAW_BINARY   // raw executable (macOS)
    }

    static final class PlatformAsset {
        final String filename;
        final AssetType assetType;

        PlatformAsset(String filename, AssetType assetType) {
            this.filename = filename;
            this.assetType = assetType;
        }
    }

    private PlatformResolver() {
    }

    static PlatformAsset resolveCurrentPlatformAsset(ClickHouseVersion version) {
        return resolveAsset(version, detectOS(), detectArch());
    }

    static PlatformAsset resolveAsset(ClickHouseVersion version, String os, String arch) {
        switch (os) {
            case "linux":
                String linuxArch = resolveLinuxArch(arch);
                String numVer = version.numericVersion();
                return new PlatformAsset(
                        "clickhouse-common-static-" + numVer + "-" + linuxArch + ".tgz",
                        AssetType.ARCHIVE
                );
            case "darwin":
                return new PlatformAsset(
                        resolveDarwinAssetName(arch),
                        AssetType.RAW_BINARY
                );
            default:
                throw new UnsupportedPlatformException(os, arch);
        }
    }

    static String downloadURL(String baseURL, ClickHouseVersion version, PlatformAsset asset) {
        if (baseURL == null || baseURL.isEmpty()) {
            baseURL = DEFAULT_BASE_URL;
        }
        return baseURL + "/v" + version.toString() + "/" + asset.filename;
    }

    static String sha512URL(String baseURL, ClickHouseVersion version, PlatformAsset asset) {
        return downloadURL(baseURL, version, asset) + ".sha512";
    }

    private static String resolveLinuxArch(String arch) {
        switch (arch) {
            case "amd64":
                return "amd64";
            case "arm64":
                return "arm64";
            default:
                throw new UnsupportedPlatformException("linux", arch);
        }
    }

    private static String resolveDarwinAssetName(String arch) {
        switch (arch) {
            case "amd64":
                return "clickhouse-macos";
            case "arm64":
                return "clickhouse-macos-aarch64";
            default:
                throw new UnsupportedPlatformException("darwin", arch);
        }
    }

    static String detectOS() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return "darwin";
        } else if (osName.contains("win")) {
            return "windows";
        }
        return osName;
    }

    static String detectArch() {
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        switch (osArch) {
            case "amd64":
            case "x86_64":
                return "amd64";
            case "aarch64":
            case "arm64":
                return "arm64";
            default:
                return osArch;
        }
    }
}
