package us.axspace.geomotd.core.geoip;

/**
 * Result of a GeoIP + Language resolution.
 * Author: axxtrial
 */
public record GeoLookupResult(
        String ip,
        String countryCode,
        String language,
        boolean cached
) {
    public boolean isLocal() {
        return "LOCAL".equalsIgnoreCase(countryCode);
    }

    public boolean isUnknown() {
        return "UNKNOWN".equalsIgnoreCase(countryCode);
    }
}
