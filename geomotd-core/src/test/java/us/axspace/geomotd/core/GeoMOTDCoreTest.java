package us.axspace.geomotd.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import us.axspace.geomotd.core.config.MOTDEntry;
import us.axspace.geomotd.core.geoip.GeoLookupResult;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class GeoMOTDCoreTest {

    private static GeoMOTDCore core;
    private static File tempFolder;

    @BeforeAll
    public static void setup() throws Exception {
        tempFolder = Files.createTempDirectory("geomotd-test").toFile();

        // Copy mmdb if available
        File localMmdb = new File("../GeoLite2-Country.mmdb");
        if (localMmdb.exists()) {
            Files.copy(localMmdb.toPath(), new File(tempFolder, "GeoLite2-Country.mmdb").toPath());
        }

        core = new GeoMOTDCore(tempFolder, System.out::println);
        core.init();
    }

    @AfterAll
    public static void teardown() {
        if (core != null) {
            core.shutdown();
        }
    }

    @Test
    public void testCountriesCount() {
        int count = core.getCountryManager().getCountryCount();
        System.out.println("Total mapped countries: " + count);
        assertTrue(count >= 200, "Should load at least 200 countries.");
    }

    @Test
    public void testMessagesManager() {
        assertNotNull(core.getMessages());
        String prefix = core.getMessages().getRaw("prefix");
        assertNotNull(prefix);
        assertFalse(prefix.isEmpty());
        System.out.println("MessagesManager prefix: " + prefix);
    }

    @Test
    public void testResolutionAndCache() {
        // Mexico IP
        GeoLookupResult r1 = core.lookup("189.200.1.1");
        System.out.println("189.200.1.1 -> " + r1.countryCode() + " / " + r1.language() + " (cached=" + r1.cached() + ")");
        assertEquals("MX", r1.countryCode());
        assertEquals("es", r1.language());
        assertFalse(r1.cached());

        // Second lookup must be cached
        GeoLookupResult r1Cached = core.lookup("189.200.1.1");
        assertTrue(r1Cached.cached(), "Second lookup must be cached in Caffeine.");

        // USA IP
        GeoLookupResult r2 = core.lookup("8.8.8.8");
        System.out.println("8.8.8.8 -> " + r2.countryCode() + " / " + r2.language());
        assertEquals("US", r2.countryCode());
        assertEquals("en", r2.language());

        // Brazil IP
        GeoLookupResult r3 = core.lookup("200.100.1.1");
        System.out.println("200.100.1.1 -> " + r3.countryCode() + " / " + r3.language());
        assertEquals("BR", r3.countryCode());
        assertEquals("pt", r3.language());

        // Localhost (default local-language in config is "en")
        GeoLookupResult rLocal = core.lookup("127.0.0.1");
        System.out.println("127.0.0.1 -> " + rLocal.countryCode() + " / " + rLocal.language());
        assertEquals("LOCAL", rLocal.countryCode());
        assertEquals("en", rLocal.language());
    }

    @Test
    public void testMOTDFallback() {
        // Unknown or unmapped country should gracefully return default language (en)
        String lang = core.getCountryManager().getLanguage("ZZ", "en");
        assertEquals("en", lang);

        // Language not configured in motds should fallback cleanly
        MOTDEntry motd = core.getConfig().getMOTD("nonexistent_lang");
        assertNotNull(motd);
        assertNotNull(motd.getFullComponent());
        System.out.println("Generic MOTD Fallback Raw Line 1: " + motd.getRawLine1());
        assertTrue(motd.getRawLine1().contains("Minecraft Server") || motd.getRawLine1().contains("Survival"));
    }
}
