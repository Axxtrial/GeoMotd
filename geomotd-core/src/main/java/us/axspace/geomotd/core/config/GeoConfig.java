package us.axspace.geomotd.core.config;

import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Platform-independent YAML configuration for GeoMOTD.
 * Developed for axspace.us/geomotd
 */
public class GeoConfig {

    private final File dataFolder;
    private final File configFile;
    private final MessagesManager messages;
    private final Consumer<String> logger;

    private String defaultLanguage = "en";
    private String localLanguage = "en";

    private String databaseFile = "GeoLite2-Country.mmdb";
    private String databaseDownloadUrl = "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-Country.mmdb";
    private boolean databaseAutoDownload = true;

    private String countriesFile = "countries.json";
    private boolean countriesAutoUpdate = false;
    private String countriesUpdateUrl = "https://raw.githubusercontent.com/axspace/GeoMOTD-data/main/countries.json";

    private int cacheMaxEntries = 50000;
    private int cacheExpireMinutes = 1440;

    private final Map<String, List<MOTDEntry>> motdsByLanguage = new ConcurrentHashMap<>();
    private final MOTDEntry fallbackEntry = new MOTDEntry("<gradient:#00FFAA:#00AAFF><bold>Minecraft Server</bold></gradient>", "<yellow>Welcome to our server!</yellow>");

    public GeoConfig(File dataFolder, MessagesManager messages, Consumer<String> logger) {
        this.dataFolder = dataFolder;
        this.configFile = new File(dataFolder, "config.yml");
        this.messages = messages;
        this.logger = logger != null ? logger : (msg) -> {};
    }

    public synchronized void load() {
        if (!configFile.exists()) {
            exportDefaultConfig();
        }

        Yaml yaml = new Yaml();
        Map<String, Object> data = null;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            data = yaml.load(reader);
        } catch (Exception e) {
            logger.accept("Error loading config.yml: " + e.getMessage());
        }

        if (data == null) {
            data = new HashMap<>();
        }

        this.defaultLanguage = getString(data, "default-language", "en").toLowerCase(Locale.ROOT);
        this.localLanguage = getString(data, "local-language", "en").toLowerCase(Locale.ROOT);

        Map<?, ?> dbSection = getSection(data, "database");
        this.databaseFile = getString(dbSection, "file", "GeoLite2-Country.mmdb");
        this.databaseDownloadUrl = getString(dbSection, "download-url", "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-Country.mmdb");
        this.databaseAutoDownload = getBoolean(dbSection, "auto-download", true);

        Map<?, ?> countriesSection = getSection(data, "countries");
        this.countriesFile = getString(countriesSection, "file", "countries.json");
        this.countriesAutoUpdate = getBoolean(countriesSection, "auto-update", false);
        this.countriesUpdateUrl = getString(countriesSection, "update-url", "https://raw.githubusercontent.com/axspace/GeoMOTD-data/main/countries.json");

        Map<?, ?> cacheSection = getSection(data, "cache");
        this.cacheMaxEntries = getInt(cacheSection, "max-entries", 50000);
        this.cacheExpireMinutes = getInt(cacheSection, "expire-minutes", 1440);

        // Load MOTD entries
        motdsByLanguage.clear();
        Map<?, ?> motdsSection = getSection(data, "motds");
        if (motdsSection != null) {
            for (Map.Entry<?, ?> entry : motdsSection.entrySet()) {
                String langKey = entry.getKey().toString().toLowerCase(Locale.ROOT);
                Object val = entry.getValue();

                List<MOTDEntry> list = new ArrayList<>();
                if (val instanceof List<?> rawList) {
                    for (Object item : rawList) {
                        if (item instanceof Map<?, ?> map) {
                            Object o1 = map.get("line1");
                            Object o2 = map.get("line2");
                            String l1 = o1 != null ? o1.toString() : "";
                            String l2 = o2 != null ? o2.toString() : "";
                            list.add(new MOTDEntry(l1, l2));
                        }
                    }
                }
                if (!list.isEmpty()) {
                    motdsByLanguage.put(langKey, list);
                }
            }
        }

        logger.accept(messages.getLog("logs.config-loaded", motdsByLanguage.keySet()));
    }

    private void exportDefaultConfig() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    logger.accept(messages.getLog("logs.config-default-exported"));
                }
            }
        } catch (Exception e) {
            logger.accept("Error exporting default config.yml: " + e.getMessage());
        }
    }

    public MOTDEntry getMOTD(String language) {
        String lang = (language != null) ? language.toLowerCase(Locale.ROOT) : defaultLanguage;

        // 1. Direct language match
        List<MOTDEntry> entries = motdsByLanguage.get(lang);

        // 2. Fallback to defaultLanguage if not found
        if ((entries == null || entries.isEmpty()) && !lang.equals(defaultLanguage)) {
            entries = motdsByLanguage.get(defaultLanguage);
        }

        // 3. Fallback to any first available language if default is missing
        if (entries == null || entries.isEmpty()) {
            if (!motdsByLanguage.isEmpty()) {
                entries = motdsByLanguage.values().iterator().next();
            }
        }

        // 4. Return random entry or hardcoded emergency fallback
        if (entries != null && !entries.isEmpty()) {
            if (entries.size() == 1) {
                return entries.get(0);
            }
            int idx = ThreadLocalRandom.current().nextInt(entries.size());
            return entries.get(idx);
        }

        return fallbackEntry;
    }

    private String getString(Map<?, ?> map, String key, String def) {
        if (map == null) return def;
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean def) {
        if (map == null) return def;
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return def;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        if (map == null) return def;
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private Map<?, ?> getSection(Map<?, ?> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return (val instanceof Map<?, ?> m) ? m : null;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getLocalLanguage() {
        return localLanguage;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public String getDatabaseDownloadUrl() {
        return databaseDownloadUrl;
    }

    public boolean isDatabaseAutoDownload() {
        return databaseAutoDownload;
    }

    public String getCountriesFile() {
        return countriesFile;
    }

    public boolean isCountriesAutoUpdate() {
        return countriesAutoUpdate;
    }

    public String getCountriesUpdateUrl() {
        return countriesUpdateUrl;
    }

    public int getCacheMaxEntries() {
        return cacheMaxEntries;
    }

    public int getCacheExpireMinutes() {
        return cacheExpireMinutes;
    }

    public Set<String> getConfiguredLanguages() {
        return Collections.unmodifiableSet(motdsByLanguage.keySet());
    }
}
