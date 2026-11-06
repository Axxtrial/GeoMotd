package us.axspace.geomotd.paper.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.geoip.GeoLookupResult;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * PlaceholderAPI expansion for GeoMOTD.
 * Provides player geolocation and cache metrics as placeholders.
 * Developed for axspace.us/geomotd
 */
public class GeoMOTDExpansion extends PlaceholderExpansion {

    private final GeoMOTDCore core;
    private final String version;

    public GeoMOTDExpansion(GeoMOTDCore core, String version) {
        this.core = core;
        this.version = version != null ? version : "1.0.0";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "geomotd";
    }

    @Override
    public @NotNull String getAuthor() {
        return "axxtrial";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);

        // System & Cache Placeholders
        switch (lower) {
            case "cache_size" -> {
                return String.valueOf(core.getGeoIPService().getCacheSize());
            }
            case "cache_hit_rate" -> {
                return String.format(Locale.ROOT, "%.2f%%", core.getGeoIPService().getCacheHitRate() * 100);
            }
            case "countries_count" -> {
                return String.valueOf(core.getCountryManager().getCountryCount());
            }
            case "default_language" -> {
                return core.getConfig().getDefaultLanguage();
            }
            case "database_status" -> {
                return core.getGeoIPService().isLoaded() ? "Active" : "Offline";
            }
        }

        // Player-specific Placeholders (requires online player with valid IP socket)
        if (offlinePlayer != null && offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                InetSocketAddress sock = player.getAddress();
                if (sock != null) {
                    InetAddress address = sock.getAddress();
                    GeoLookupResult lookup = core.lookup(address);

                    switch (lower) {
                        case "country_code" -> {
                            return lookup.countryCode();
                        }
                        case "country" -> {
                            String code = lookup.countryCode();
                            if (code != null && code.length() == 2) {
                                String display = new Locale("", code).getDisplayCountry(Locale.ENGLISH);
                                if (!display.isEmpty()) {
                                    return display;
                                }
                            }
                            return code;
                        }
                        case "language" -> {
                            return lookup.language();
                        }
                        case "ip" -> {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        }

        return null;
    }
}
