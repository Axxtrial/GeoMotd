package us.axspace.geomotd.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Message and localization manager for GeoMOTD.
 * Reads messages.yml, provides MiniMessage components and localized logs.
 * Developed for axspace.us/geomotd
 */
public class MessagesManager {

    private final File dataFolder;
    private final File messagesFile;
    private final Consumer<String> fallbackLogger;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, String> messages = new ConcurrentHashMap<>();

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("prefix", "<gradient:#FFD700:#FFA500><bold>GeoMOTD</bold></gradient> <dark_gray>»</dark_gray> ");
        DEFAULTS.put("general.no-permission", "<red>You do not have permission to execute this command.</red>");
        DEFAULTS.put("general.unknown-command", "<red>Unknown sub-command. Type <yellow>/geomotd</yellow> for available commands.</red>");

        DEFAULTS.put("commands.help.header", "<gradient:#FFD700:#FFA500><bold>GeoMOTD Commands</bold></gradient> <dark_gray>(axspace.us/geomotd)</dark_gray>");
        DEFAULTS.put("commands.help.reload", "<yellow>/geomotd reload</yellow> <gray>- Reloads configuration, messages, and country database</gray>");
        DEFAULTS.put("commands.help.status", "<yellow>/geomotd status</yellow> <gray>- Displays system status, cache metrics, and mappings</gray>");

        DEFAULTS.put("commands.reload.success", "<green>Configuration, messages, countries, and cache reloaded successfully!</green>");

        DEFAULTS.put("commands.status.header", "<gradient:#FFD700:#FFA500><bold>=== GeoMOTD System Status ===</bold></gradient>");
        DEFAULTS.put("commands.status.platform", "<gray>Platform: <aqua><platform></aqua></gray>");
        DEFAULTS.put("commands.status.database-loaded", "<gray>GeoLite2 Database: <status></gray>");
        DEFAULTS.put("commands.status.database-loaded-true", "<green>Loaded & Active</green>");
        DEFAULTS.put("commands.status.database-loaded-false", "<red>Not loaded (check .mmdb file)</red>");
        DEFAULTS.put("commands.status.countries-count", "<gray>Mapped Countries: <green><count></green></gray>");
        DEFAULTS.put("commands.status.configured-languages", "<gray>Configured Languages: <gold><languages></gold></gray>");
        DEFAULTS.put("commands.status.default-language", "<gray>Default Fallback Language: <white><language></white></gray>");
        DEFAULTS.put("commands.status.cache-size", "<gray>Cached IP Addresses: <aqua><size></aqua></gray>");
        DEFAULTS.put("commands.status.cache-hit-rate", "<gray>RAM Cache Hit Rate: <green><rate></green></gray>");

        DEFAULTS.put("logs.plugin-enabling", "Enabling GeoMOTD ({0})...");
        DEFAULTS.put("logs.plugin-enabled", "GeoMOTD enabled successfully in {0} ms!");
        DEFAULTS.put("logs.plugin-disabled", "GeoMOTD disabled.");
        DEFAULTS.put("logs.core-initialized", "GeoMOTD Core engine initialized successfully.");
        DEFAULTS.put("logs.countries-loaded-file", "Loaded {0} countries from countries.json.");
        DEFAULTS.put("logs.countries-loaded-internal", "Loaded {0} countries from internal fallback.");
        DEFAULTS.put("logs.countries-exported", "Exported default countries.json successfully to {0}.");
        DEFAULTS.put("logs.db-found-root", "Found local GeoLite2 database in server root. Copying...");
        DEFAULTS.put("logs.db-downloading", "GeoLite2 database not found. Downloading automatically...");
        DEFAULTS.put("logs.db-download-success", "GeoLite2 database downloaded successfully ({0} MB).");
        DEFAULTS.put("logs.db-download-failed", "ERROR: Could not download GeoLite2 database. Please place it manually in {0}.");
        DEFAULTS.put("logs.db-missing", "WARNING: GeoLite2 database missing. GeoIP detection will fall back to default language.");
        DEFAULTS.put("logs.db-loaded", "GeoLite2 database loaded into memory successfully (CHMCache active).");
        DEFAULTS.put("logs.db-load-error", "Error opening GeoLite2 database: {0}");
        DEFAULTS.put("logs.config-loaded", "Configuration loaded. Configured languages: {0}");
        DEFAULTS.put("logs.config-default-exported", "Default config.yml created.");
        DEFAULTS.put("logs.messages-loaded", "Messages loaded from messages.yml.");
        DEFAULTS.put("logs.messages-default-exported", "Default messages.yml created.");
    }

    public MessagesManager(File dataFolder, Consumer<String> fallbackLogger) {
        this.dataFolder = dataFolder;
        this.messagesFile = new File(dataFolder, "messages.yml");
        this.fallbackLogger = fallbackLogger != null ? fallbackLogger : System.out::println;
    }

    public synchronized void load() {
        if (!messagesFile.exists()) {
            exportDefaultMessages();
        }

        messages.clear();
        messages.putAll(DEFAULTS);

        if (messagesFile.exists()) {
            try (Reader reader = Files.newBufferedReader(messagesFile.toPath(), StandardCharsets.UTF_8)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(reader);
                if (data != null) {
                    flattenYaml("", data, messages);
                    fallbackLogger.accept(getLog("logs.messages-loaded"));
                    return;
                }
            } catch (Exception e) {
                fallbackLogger.accept("Error loading messages.yml: " + e.getMessage());
            }
        }
    }

    private void flattenYaml(String prefix, Map<?, ?> map, Map<String, String> out) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> subMap) {
                flattenYaml(key, subMap, out);
            } else if (value != null) {
                out.put(key, value.toString());
            }
        }
    }

    private void exportDefaultMessages() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                    fallbackLogger.accept(getLog("logs.messages-default-exported"));
                }
            }
        } catch (Exception e) {
            fallbackLogger.accept("Error exporting default messages.yml: " + e.getMessage());
        }
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, DEFAULTS.getOrDefault(key, key));
    }

    public String getLog(String key, Object... args) {
        String template = getRaw(key);
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(template, args);
            } catch (Exception ignored) {
                for (int i = 0; i < args.length; i++) {
                    template = template.replace("{" + i + "}", String.valueOf(args[i]));
                }
            }
        }
        return template;
    }

    public Component getComponent(String key) {
        return mm.deserialize(getRaw(key));
    }

    public Component getComponent(String key, Map<String, String> placeholders) {
        String str = getRaw(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                str = str.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        return mm.deserialize(str);
    }

    public Component getPrefixed(String key) {
        String prefix = getRaw("prefix");
        return mm.deserialize(prefix + getRaw(key));
    }

    public Component getPrefixed(String key, Map<String, String> placeholders) {
        String prefix = getRaw("prefix");
        String str = getRaw(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                str = str.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        return mm.deserialize(prefix + str);
    }
}
