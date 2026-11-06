package us.axspace.geomotd.core;

import net.kyori.adventure.text.Component;
import us.axspace.geomotd.core.config.GeoConfig;
import us.axspace.geomotd.core.config.MOTDEntry;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.core.geoip.CountryLanguageManager;
import us.axspace.geomotd.core.geoip.GeoIPService;
import us.axspace.geomotd.core.geoip.GeoLookupResult;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Universal Core Engine for GeoMOTD.
 * Supports Paper, Folia, Velocity, and BungeeCord.
 * Developed for axspace.us/geomotd
 */
public class GeoMOTDCore {

    private final File dataFolder;
    private final Consumer<String> logger;

    private MessagesManager messages;
    private GeoConfig config;
    private CountryLanguageManager countryManager;
    private GeoIPService geoIPService;

    public GeoMOTDCore(File dataFolder, Consumer<String> logger) {
        this.dataFolder = dataFolder;
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
}
