package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformResolverTest {

    @ParameterizedTest
    @CsvSource({
            "amd64, clickhouse-common-static-25.8.16.34-amd64.tgz",
            "arm64, clickhouse-common-static-25.8.16.34-arm64.tgz"
    })
    void resolveAsset_linux(String arch, String expectedFile) {
        PlatformResolver.PlatformAsset asset =
                PlatformResolver.resolveAsset(ClickHouseVersion.V25_8, "linux", arch);

        assertThat(asset.filename).isEqualTo(expectedFile);
        assertThat(asset.assetType).isEqualTo(PlatformResolver.AssetType.ARCHIVE);
    }

    @ParameterizedTest
    @CsvSource({
            "amd64, clickhouse-macos",
            "arm64, clickhouse-macos-aarch64"
    })
    void resolveAsset_darwin(String arch, String expectedFile) {
        PlatformResolver.PlatformAsset asset =
                PlatformResolver.resolveAsset(ClickHouseVersion.V25_8, "darwin", arch);

        assertThat(asset.filename).isEqualTo(expectedFile);
        assertThat(asset.assetType).isEqualTo(PlatformResolver.AssetType.RAW_BINARY);
    }

    @ParameterizedTest
    @CsvSource({
            "windows, amd64",
            "linux,   386",
            "darwin,  386",
            "freebsd, amd64"
    })
    void resolveAsset_unsupported(String os, String arch) {
        assertThatThrownBy(() -> PlatformResolver.resolveAsset(
                ClickHouseVersion.V25_8, os, arch))
                .isInstanceOf(UnsupportedPlatformException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "25.8.16.34-lts,     25.8.16.34",
            "25.3.14.14-lts,     25.3.14.14",
            "26.1.3.52-stable,   26.1.3.52",
            "24.1.1.1,           24.1.1.1"
    })
    void numericVersion(String version, String expected) {
        ClickHouseVersion v = new ClickHouseVersion(version);
        assertThat(v.numericVersion()).isEqualTo(expected);
    }

    @Test
    void downloadURL_defaultBase() {
        PlatformResolver.PlatformAsset asset = new PlatformResolver.PlatformAsset(
                "clickhouse-common-static-25.8.16.34-amd64.tgz",
                PlatformResolver.AssetType.ARCHIVE);

        String url = PlatformResolver.downloadURL(null, ClickHouseVersion.V25_8, asset);

        assertThat(url).isEqualTo(
                "https://github.com/ClickHouse/ClickHouse/releases/download/"
                        + "v25.8.16.34-lts/clickhouse-common-static-25.8.16.34-amd64.tgz");
    }

    @Test
    void downloadURL_customBase() {
        PlatformResolver.PlatformAsset asset = new PlatformResolver.PlatformAsset(
                "clickhouse-macos-aarch64", PlatformResolver.AssetType.RAW_BINARY);

        String url = PlatformResolver.downloadURL(
                "https://mirror.example.com/releases",
                ClickHouseVersion.V25_3, asset);

        assertThat(url).isEqualTo(
                "https://mirror.example.com/releases/v25.3.14.14-lts/clickhouse-macos-aarch64");
    }

    @Test
    void sha512URL() {
        PlatformResolver.PlatformAsset asset = new PlatformResolver.PlatformAsset(
                "clickhouse-common-static-25.8.16.34-amd64.tgz",
                PlatformResolver.AssetType.ARCHIVE);

        String url = PlatformResolver.sha512URL(null, ClickHouseVersion.V25_8, asset);

        assertThat(url).endsWith(".sha512");
        assertThat(url).isEqualTo(
                "https://github.com/ClickHouse/ClickHouse/releases/download/"
                        + "v25.8.16.34-lts/clickhouse-common-static-25.8.16.34-amd64.tgz.sha512");
    }

    @Test
    void detectOS_returnsKnownValue() {
        String os = PlatformResolver.detectOS();
        assertThat(os).isIn("linux", "darwin", "windows");
    }

    @Test
    void detectArch_returnsKnownValue() {
        String arch = PlatformResolver.detectArch();
        assertThat(arch).isIn("amd64", "arm64");
    }
}
