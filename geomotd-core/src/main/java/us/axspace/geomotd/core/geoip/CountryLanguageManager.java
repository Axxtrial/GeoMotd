package us.axspace.geomotd.core.geoip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.core.util.HttpUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages Country ISO-2 code to Language mappings.
 * Reads from dynamic countries.json, falls back to embedded resource, and supports remote updates.
 * Developed for axspace.us/geomotd
 */
public class CountryLanguageManager {

    private final File dataFolder;
    private final File countriesFile;
    private final MessagesManager messages;
    private final Consumer<String> logger;
    private final Map<String, String> countryToLanguage = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CountryLanguageManager(File dataFolder, MessagesManager messages, Consumer<String> logger) {
        this.dataFolder = dataFolder;
        this.countriesFile = new File(dataFolder, "countries.json");
        this.messages = messages;
        this.logger = logger != null ? logger : (msg) -> {};
    }

    public synchronized void load() {
        if (!countriesFile.exists()) {
            exportDefaultCountries();
        }

        if (countriesFile.exists()) {
            try (Reader reader = Files.newBufferedReader(countriesFile.toPath(), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loaded = gson.fromJson(reader, type);
                if (loaded != null && !loaded.isEmpty()) {
                    countryToLanguage.clear();
                    loaded.forEach((k, v) -> countryToLanguage.put(k.toUpperCase(Locale.ROOT), v.toLowerCase(Locale.ROOT)));
                    logger.accept(messages.getLog("logs.countries-loaded-file", countryToLanguage.size()));
                    return;
                }
            } catch (Exception e) {
                logger.accept("Error reading countries.json: " + e.getMessage());
            }
        }

        // Fallback to internal bundled resource
        loadInternalBundled();
    }

    private void exportDefaultCountries() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            try (InputStream in = getClass().getResourceAsStream("/countries.json")) {
                if (in != null) {
                    Files.copy(in, countriesFile.toPath());
                    logger.accept(messages.getLog("logs.countries-exported", countriesFile.getName()));
                } else {
                    saveToDisk(getDefaultHardcodedMap());
                }
            }
        } catch (Exception e) {
            logger.accept("Could not export countries.json: " + e.getMessage());
        }
    }

    private void loadInternalBundled() {
        try (InputStream in = getClass().getResourceAsStream("/countries.json")) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> loaded = gson.fromJson(reader, type);
                    if (loaded != null) {
                        countryToLanguage.clear();
                        loaded.forEach((k, v) -> countryToLanguage.put(k.toUpperCase(Locale.ROOT), v.toLowerCase(Locale.ROOT)));
                        logger.accept(messages.getLog("logs.countries-loaded-internal", countryToLanguage.size()));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.accept("Error loading internal countries.json resource: " + e.getMessage());
        }

        // Ultimate hardcoded baseline
        countryToLanguage.clear();
        countryToLanguage.putAll(getDefaultHardcodedMap());
    }

    public synchronized boolean updateFromRemote(String url) {
        try {
            logger.accept(messages.getLog("commands.update-countries.checking"));
            String jsonContent = HttpUtil.fetchString(url);
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> downloaded = gson.fromJson(jsonContent, type);

            if (downloaded == null || downloaded.isEmpty()) {
                logger.accept(messages.getLog("logs.countries-update-invalid"));
                return false;
            }

            countryToLanguage.clear();
            downloaded.forEach((k, v) -> countryToLanguage.put(k.toUpperCase(Locale.ROOT), v.toLowerCase(Locale.ROOT)));

            // Save to disk
            saveToDisk(countryToLanguage);
            logger.accept(messages.getLog("logs.countries-updated-remote", countryToLanguage.size()));
            return true;
        } catch (Exception e) {
            logger.accept(messages.getLog("logs.countries-update-failed", e.getMessage()));
            return false;
        }
    }

    private void saveToDisk(Map<String, String> map) {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            try (Writer writer = Files.newBufferedWriter(countriesFile.toPath(), StandardCharsets.UTF_8)) {
                gson.toJson(map, writer);
            }
        } catch (Exception e) {
            logger.accept("Could not save countries.json to disk: " + e.getMessage());
        }
    }

    public String getLanguage(String countryCode, String defaultLanguage) {
        if (countryCode == null || countryCode.isEmpty()) {
            return defaultLanguage;
        }
        String lang = countryToLanguage.get(countryCode.toUpperCase(Locale.ROOT));
        return (lang != null && !lang.isEmpty()) ? lang : defaultLanguage;
    }

    public int getCountryCount() {
        return countryToLanguage.size();
    }

    public Map<String, String> getAllMappings() {
        return Collections.unmodifiableMap(countryToLanguage);
    }

    private Map<String, String> getDefaultHardcodedMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        // Hispanic America & Spain
        for (String c : new String[]{"ES", "MX", "AR", "CO", "CL", "PE", "VE", "UY", "EC", "GT", "CR", "PA", "BO", "PY", "SV", "HN", "NI", "DO", "PR", "CU", "GQ"}) {
            map.put(c, "es");
        }
        // Portuguese
        for (String c : new String[]{"BR", "PT", "AO", "MZ", "CV", "GW", "ST", "TL"}) {
            map.put(c, "pt");
        }
        // English
        for (String c : new String[]{"US", "GB", "CA", "AU", "NZ", "IE", "ZA", "PH", "IN", "SG"}) {
            map.put(c, "en");
        }
        return map;
    }
}
