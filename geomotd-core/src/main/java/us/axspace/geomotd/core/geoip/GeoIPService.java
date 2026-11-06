package us.axspace.geomotd.core.geoip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.core.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Ultra-fast GeoIP resolution service with Caffeine Cache and MaxMind DB Reader.
 * Developed for axspace.us/geomotd
 */
public class GeoIPService {

    private final File dataFolder;
    private final String dbFileName;
    private final String downloadUrl;
    private final boolean autoDownload;
    private final MessagesManager messages;
    private final Consumer<String> logger;

    private Reader maxmindReader;
    private Cache<String, GeoLookupResult> ipCache;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public GeoIPService(File dataFolder, String dbFileName, String downloadUrl, boolean autoDownload,
                        int maxEntries, int expireMinutes, MessagesManager messages, Consumer<String> logger) {
        this.dataFolder = dataFolder;
        this.dbFileName = dbFileName != null ? dbFileName : "GeoLite2-Country.mmdb";
        this.downloadUrl = downloadUrl;
        this.autoDownload = autoDownload;
        this.messages = messages;
        this.logger = logger != null ? logger : (msg) -> {};

        initCache(maxEntries, expireMinutes);
    }

    public void initCache(int maxEntries, int expireMinutes) {
        this.ipCache = Caffeine.newBuilder()
                .maximumSize(maxEntries > 0 ? maxEntries : 50000)
                .expireAfterWrite(Duration.ofMinutes(expireMinutes > 0 ? expireMinutes : 1440))
                .recordStats()
                .build();
    }

    public synchronized boolean init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, dbFileName);

        if (!dbFile.exists()) {
            File fallbackGeo = new File("GeoLite2-Country.mmdb");
            if (fallbackGeo.exists()) {
                logger.accept(messages.getLog("logs.db-found-root"));
                try {
                    java.nio.file.Files.copy(fallbackGeo.toPath(), dbFile.toPath());
                } catch (Exception ignored) {}
            }
        }

        if (!dbFile.exists() && autoDownload && downloadUrl != null && !downloadUrl.isEmpty()) {
            logger.accept(messages.getLog("logs.db-downloading"));
            boolean success = HttpUtil.downloadFile(downloadUrl, dbFile, logger);
            if (!success) {
                logger.accept(messages.getLog("logs.db-download-failed", dbFile.getAbsolutePath()));
                return false;
            }
            logger.accept(messages.getLog("logs.db-download-success", (dbFile.length() / 1024 / 1024)));
        }

        if (!dbFile.exists()) {
            logger.accept(messages.getLog("logs.db-missing"));
            return false;
        }

        try {
            if (maxmindReader != null) {
                try { maxmindReader.close(); } catch (Exception ignored) {}
            }
            this.maxmindReader = new Reader(dbFile, new CHMCache(4096));
            this.loaded.set(true);
            logger.accept(messages.getLog("logs.db-loaded"));
            return true;
        } catch (IOException e) {
            logger.accept(messages.getLog("logs.db-load-error", e.getMessage()));
            this.loaded.set(false);
            return false;
        }
    }

    public GeoLookupResult lookup(InetAddress address, CountryLanguageManager countryManager,
                                   String defaultLanguage, String localLanguage) {
        if (address == null) {
            return new GeoLookupResult("unknown", "UNKNOWN", defaultLanguage, false);
        }

        String ipStr = address.getHostAddress();

        // 1. Check RAM Cache
        GeoLookupResult cached = ipCache.getIfPresent(ipStr);
        if (cached != null) {
            return new GeoLookupResult(cached.ip(), cached.countryCode(), cached.language(), true);
        }

        // 2. Check if local/private network
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || "127.0.0.1".equals(ipStr)) {
            GeoLookupResult localRes = new GeoLookupResult(ipStr, "LOCAL", localLanguage, false);
            ipCache.put(ipStr, localRes);
            return localRes;
        }

        // 3. Resolve from MaxMind
        String countryCode = "UNKNOWN";
        if (loaded.get() && maxmindReader != null) {
            try {
                Map<?, ?> record = maxmindReader.get(address, Map.class);
                if (record != null) {
                    Object countryObj = record.get("country");
                    if (countryObj instanceof Map<?, ?> countryMap) {
                        Object isoObj = countryMap.get("iso_code");
                        if (isoObj != null) {
                            countryCode = isoObj.toString();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 4. Resolve Language from Country code
        String language = countryManager.getLanguage(countryCode, defaultLanguage);
        GeoLookupResult result = new GeoLookupResult(ipStr, countryCode, language, false);

        // 5. Store in Caffeine
        ipCache.put(ipStr, result);
        return result;
    }

    public GeoLookupResult lookup(String ipStr, CountryLanguageManager countryManager,
                                   String defaultLanguage, String localLanguage) {
        try {
            InetAddress addr = InetAddress.getByName(ipStr);
            return lookup(addr, countryManager, defaultLanguage, localLanguage);
        } catch (Exception e) {
            return new GeoLookupResult(ipStr, "UNKNOWN", defaultLanguage, false);
        }
    }

    public void clearCache() {
        if (ipCache != null) {
            ipCache.invalidateAll();
        }
    }

    public long getCacheSize() {
        return ipCache != null ? ipCache.estimatedSize() : 0;
    }

    public double getCacheHitRate() {
        return ipCache != null ? ipCache.stats().hitRate() : 0.0;
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void close() {
        if (maxmindReader != null) {
            try {
                maxmindReader.close();
            } catch (Exception ignored) {}
            loaded.set(false);
        }
    }
}
