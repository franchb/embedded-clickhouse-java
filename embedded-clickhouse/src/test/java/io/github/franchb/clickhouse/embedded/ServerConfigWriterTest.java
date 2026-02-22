package io.github.franchb.clickhouse.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerConfigWriterTest {

    @Test
    void writeServerConfig(@TempDir Path tempDir) throws IOException {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("max_threads", "4");

        String configPath = ServerConfigWriter.writeServerConfig(
                tempDir.toString(), 19000, 18123, settings);

        assertThat(Paths.get(configPath).getParent()).isEqualTo(tempDir);

        String xml = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);

        assertThat(xml).contains("<tcp_port>19000</tcp_port>");
        assertThat(xml).contains("<http_port>18123</http_port>");
        assertThat(xml).contains("<max_threads>4</max_threads>");
        assertThat(xml).contains("<password></password>");
        assertThat(xml).contains("<max_server_memory_usage>1073741824</max_server_memory_usage>");
    }

    @Test
    void writeServerConfig_createsSubdirs(@TempDir Path tempDir) throws IOException {
        ServerConfigWriter.writeServerConfig(tempDir.toString(), 19000, 18123, null);

        assertThat(tempDir.resolve("data")).isDirectory();
        assertThat(tempDir.resolve("tmp")).isDirectory();
        assertThat(tempDir.resolve("user_files")).isDirectory();
        assertThat(tempDir.resolve("format_schemas")).isDirectory();
    }

    @Test
    void writeServerConfig_noSettings(@TempDir Path tempDir) throws IOException {
        String configPath = ServerConfigWriter.writeServerConfig(
                tempDir.toString(), 9000, 8123, null);

        String xml = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);

        assertThat(xml).contains("<tcp_port>9000</tcp_port>");
    }

    @Test
    void writeServerConfig_invalidSettingKey(@TempDir Path tempDir) {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("invalid key!", "value");

        assertThatThrownBy(() -> ServerConfigWriter.writeServerConfig(
                tempDir.toString(), 9000, 8123, settings))
                .isInstanceOf(InvalidSettingKeyException.class);
    }

    @Test
    void xmlEscape() {
        assertThat(ServerConfigWriter.xmlEscape("hello")).isEqualTo("hello");
        assertThat(ServerConfigWriter.xmlEscape("<>&\"'"))
                .isEqualTo("&lt;&gt;&amp;&quot;&apos;");
        assertThat(ServerConfigWriter.xmlEscape("/tmp/data & more"))
                .isEqualTo("/tmp/data &amp; more");
    }
}
