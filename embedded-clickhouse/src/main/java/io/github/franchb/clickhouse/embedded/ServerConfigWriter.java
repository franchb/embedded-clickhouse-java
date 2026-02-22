package io.github.franchb.clickhouse.embedded;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Generates a ClickHouse XML config file with ports, paths, and custom settings.
 * Equivalent to server_config.go in the Go source.
 */
final class ServerConfigWriter {

    private static final Pattern VALID_SETTING_KEY = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    private ServerConfigWriter() {
    }

    /**
     * Generates a ClickHouse XML config file in the given directory.
     *
     * @return the path to the generated config.xml
     */
    static String writeServerConfig(String dir, int tcpPort, int httpPort,
                                    Map<String, String> settings) throws IOException {
        if (settings != null) {
            for (String key : settings.keySet()) {
                if (!VALID_SETTING_KEY.matcher(key).matches()) {
                    throw new InvalidSettingKeyException(key);
                }
            }
        }

        String dataDir = Paths.get(dir, "data").toString();
        String tmpDir = Paths.get(dir, "tmp").toString();
        String userFilesDir = Paths.get(dir, "user_files").toString();
        String formatSchemaDir = Paths.get(dir, "format_schemas").toString();

        for (String d : new String[]{dataDir, tmpDir, userFilesDir, formatSchemaDir}) {
            Files.createDirectories(Paths.get(d));
        }

        Path configPath = Paths.get(dir, "config.xml");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<clickhouse>\n");
        xml.append("    <logger>\n");
        xml.append("        <level>warning</level>\n");
        xml.append("        <console>1</console>\n");
        xml.append("    </logger>\n");
        xml.append("\n");
        xml.append("    <tcp_port>").append(tcpPort).append("</tcp_port>\n");
        xml.append("    <http_port>").append(httpPort).append("</http_port>\n");
        xml.append("\n");
        xml.append("    <path>").append(xmlEscape(dataDir)).append("/</path>\n");
        xml.append("    <tmp_path>").append(xmlEscape(tmpDir)).append("/</tmp_path>\n");
        xml.append("    <user_files_path>").append(xmlEscape(userFilesDir)).append("/</user_files_path>\n");
        xml.append("    <format_schema_path>").append(xmlEscape(formatSchemaDir)).append("/</format_schema_path>\n");
        xml.append("\n");
        xml.append("    <max_server_memory_usage>1073741824</max_server_memory_usage>\n");
        xml.append("\n");
        xml.append("    <users>\n");
        xml.append("        <default>\n");
        xml.append("            <password></password>\n");
        xml.append("            <networks>\n");
        xml.append("                <ip>::1</ip>\n");
        xml.append("                <ip>127.0.0.1</ip>\n");
        xml.append("            </networks>\n");
        xml.append("            <profile>default</profile>\n");
        xml.append("            <quota>default</quota>\n");
        xml.append("            <access_management>1</access_management>\n");
        xml.append("        </default>\n");
        xml.append("    </users>\n");
        xml.append("\n");
        xml.append("    <profiles>\n");
        xml.append("        <default/>\n");
        xml.append("    </profiles>\n");
        xml.append("\n");
        xml.append("    <quotas>\n");
        xml.append("        <default/>\n");
        xml.append("    </quotas>\n");

        if (settings != null) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                xml.append("    <").append(entry.getKey()).append(">");
                xml.append(xmlEscape(entry.getValue()));
                xml.append("</").append(entry.getKey()).append(">\n");
            }
        }

        xml.append("</clickhouse>\n");

        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            writer.write(xml.toString());
        }

        return configPath.toString();
    }

    /**
     * Escapes a string for safe embedding in an XML text node.
     */
    static String xmlEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
