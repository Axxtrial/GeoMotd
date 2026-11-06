package us.axspace.geomotd.core;

import net.kyori.adventure.text.Component;
import us.axspace.geomotd.core.config.GeoConfig;
import us.axspace.geomotd.core.config.MOTDEntry;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.core.geoip.CountryLanguageManager;
import us.axspace.geomotd.core.geoip.GeoIPService;
import us.axspace.geomotd.core.geoip.GeoLookupResult;

import us.axspace.geomotd.core.version.VersionManager;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Universal Core Engine for GeoMOTD.
 * Supports Paper, Folia, Velocity, and BungeeCord.
 * Developed for axspace.us/geomotd
 */
public class GeoMOTDCore {

    private final File dataFolder;
    private final String currentVersion;
    private final Consumer<String> logger;
    private final List<Runnable> reloadHooks = new CopyOnWriteArrayList<>();

    private MessagesManager messages;
    private GeoConfig config;
    private CountryLanguageManager countryManager;
    private GeoIPService geoIPService;
    private VersionManager versionManager;

    public GeoMOTDCore(File dataFolder, Consumer<String> logger) {
        this(dataFolder, "1.0.0", logger);
    }

    public GeoMOTDCore(File dataFolder, String currentVersion, Consumer<String> logger) {
        this.dataFolder = dataFolder;
        this.currentVersion = currentVersion != null ? currentVersion : "1.0.0";
        this.logger = logger != null ? logger : (msg) -> {};
    }

    public synchronized void init() {
        this.messages = new MessagesManager(dataFolder, logger);
        this.messages.load();

        this.config = new GeoConfig(dataFolder, messages, logger);
        this.config.load();

        this.countryManager = new CountryLanguageManager(dataFolder, messages, logger);
        this.countryManager.load();

        if (config.isCountriesAutoUpdate() && config.getCountriesUpdateUrl() != null) {
            CompletableFuture.runAsync(() -> {
                countryManager.updateFromRemote(config.getCountriesUpdateUrl());
            });
        }

        this.geoIPService = new GeoIPService(
                dataFolder,
                config.getDatabaseFile(),
                config.getDatabaseDownloadUrl(),
                config.isDatabaseAutoDownload(),
                config.getCacheMaxEntries(),
                config.getCacheExpireMinutes(),
                messages,
                logger
        );
        this.geoIPService.init();

        this.versionManager = new VersionManager(currentVersion, "Axxtrial/GeoMotd", messages, logger);
        if (config.isCheckUpdates()) {
            this.versionManager.checkForUpdatesAsync(null);
        }

        logger.accept(messages.getLog("logs.core-initialized"));
    }

    public MOTDEntry getMOTDEntryForAddress(InetAddress address) {
        GeoLookupResult result = geoIPService.lookup(
                address,
                countryManager,
                config.getDefaultLanguage(),
                config.getLocalLanguage()
        );
        return config.getMOTD(result.language());
    }

    public String getLanguageForAddress(InetAddress address) {
        GeoLookupResult result = lookup(address);
        return result != null ? result.language() : config.getDefaultLanguage();
    }

    public Component getComponentForAddress(InetAddress address) {
        return getMOTDEntryForAddress(address).getFullComponent();
    }

    public GeoLookupResult lookup(InetAddress address) {
        return geoIPService.lookup(
                address,
                countryManager,
                config.getDefaultLanguage(),
                config.getLocalLanguage()
        );
    }

    public GeoLookupResult lookup(String ip) {
        return geoIPService.lookup(
                ip,
                countryManager,
                config.getDefaultLanguage(),
                config.getLocalLanguage()
        );
    }

    public void registerReloadHook(Runnable hook) {
        if (hook != null) {
            reloadHooks.add(hook);
        }
    }

    public synchronized void reload() {
        if (messages != null) {
            messages.load();
        }
        if (config != null) {
            config.load();
        }
        if (countryManager != null) {
            countryManager.load();
        }
        if (geoIPService != null) {
            geoIPService.clearCache();
            geoIPService.initCache(config.getCacheMaxEntries(), config.getCacheExpireMinutes());
        }
        for (Runnable hook : reloadHooks) {
            try {
                hook.run();
            } catch (Exception e) {
                logger.accept("Error executing reload hook: " + e.getMessage());
            }
        }
    }

    public CompletableFuture<Boolean> updateCountries() {
        return CompletableFuture.supplyAsync(() -> {
            if (countryManager != null && config.getCountriesUpdateUrl() != null) {
                return countryManager.updateFromRemote(config.getCountriesUpdateUrl());
            }
            return false;
        });
    }

    public void shutdown() {
        if (geoIPService != null) {
            geoIPService.close();
        }
    }

    public MessagesManager getMessages() {
        return messages;
    }

    public GeoConfig getConfig() {
        return config;
    }

    public CountryLanguageManager getCountryManager() {
        return countryManager;
    }

    public GeoIPService getGeoIPService() {
        return geoIPService;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }
}
