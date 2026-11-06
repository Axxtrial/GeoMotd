package us.axspace.geomotd.core.version;

import us.axspace.geomotd.core.config.MessagesManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Non-blocking, asynchronous Version Manager & Update Checker for GeoMOTD.
 * Checks the official GitHub Releases API for updates.
 * Developed for axspace.us/geomotd
 */
public class VersionManager {

    private static final String DEFAULT_REPO = "Axxtrial/GeoMotd";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final String currentVersion;
    private final String githubRepo;
    private final MessagesManager messages;
    private final Consumer<String> logger;

    private volatile String latestVersion;
    private volatile String downloadUrl;
    private volatile boolean updateAvailable = false;
    private volatile boolean checkCompleted = false;

    public VersionManager(String currentVersion, String githubRepo, MessagesManager messages, Consumer<String> logger) {
        this.currentVersion = cleanVersion(currentVersion != null ? currentVersion : "1.0.0");
        this.githubRepo = githubRepo != null ? githubRepo : DEFAULT_REPO;
        this.latestVersion = this.currentVersion;
        this.downloadUrl = "https://github.com/" + this.githubRepo + "/releases";
        this.messages = messages;
        this.logger = logger != null ? logger : (msg) -> {};
    }

    /**
     * Checks for updates asynchronously without blocking the server thread.
     */
    public CompletableFuture<VersionCheckResult> checkForUpdatesAsync(Consumer<VersionCheckResult> callback) {
        return CompletableFuture.supplyAsync(() -> {
            VersionCheckResult result = performCheck();
            this.checkCompleted = true;
            if (callback != null) {
                callback.accept(result);
            }
            return result;
        });
    }

    private VersionCheckResult performCheck() {
        String apiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        try {
            URL url = URI.create(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GeoMOTD-VersionManager/" + currentVersion);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code == 404) {
                // No releases published yet on GitHub
                this.updateAvailable = false;
                return new VersionCheckResult(VersionStatus.UP_TO_DATE, currentVersion, currentVersion, downloadUrl);
            }

            if (code != 200) {
                return new VersionCheckResult(VersionStatus.ERROR, currentVersion, null, downloadUrl);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String json = sb.toString();
            Matcher tagMatcher = TAG_PATTERN.matcher(json);
            Matcher urlMatcher = URL_PATTERN.matcher(json);

            if (tagMatcher.find()) {
                String remoteTag = tagMatcher.group(1);
                String remoteVersion = cleanVersion(remoteTag);
                this.latestVersion = remoteVersion;

                if (urlMatcher.find()) {
                    this.downloadUrl = urlMatcher.group(1);
                }

                if (compareVersions(remoteVersion, currentVersion) > 0) {
                    this.updateAvailable = true;
                    logger.accept(String.format("[GeoMOTD] A new update is available: v%s (Current: v%s)", remoteVersion, currentVersion));
                    logger.accept(String.format("[GeoMOTD] Download the latest release at: %s", downloadUrl));
                    return new VersionCheckResult(VersionStatus.UPDATE_AVAILABLE, currentVersion, remoteVersion, downloadUrl);
                } else {
                    this.updateAvailable = false;
                    return new VersionCheckResult(VersionStatus.UP_TO_DATE, currentVersion, remoteVersion, downloadUrl);
                }
            }

            return new VersionCheckResult(VersionStatus.UP_TO_DATE, currentVersion, currentVersion, downloadUrl);

        } catch (Exception e) {
            // Non-fatal: GitHub rate limit or network unreachable
            return new VersionCheckResult(VersionStatus.ERROR, currentVersion, null, downloadUrl);
        }
    }

    /**
     * Compares two semantic version strings.
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = cleanVersion(v1).split("[.-]");
        String[] parts2 = cleanVersion(v2).split("[.-]");
        int len = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < len; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                try {
                    num1 = Integer.parseInt(parts1[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (i < parts2.length) {
                try {
                    num2 = Integer.parseInt(parts2[i]);
                } catch (NumberFormatException ignored) {}
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static String cleanVersion(String ver) {
        if (ver == null) return "1.0.0";
        ver = ver.trim();
        if (ver.startsWith("v") || ver.startsWith("V")) {
            ver = ver.substring(1);
        }
        return ver;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean isCheckCompleted() {
        return checkCompleted;
    }

    public String getStatusDisplay() {
        if (updateAvailable) {
            return "<yellow>v" + currentVersion + "</yellow> <gray>→</gray> <green><bold>v" + latestVersion + "</bold></green> <gold>(Update Available!)</gold>";
        }
        return "<green>v" + currentVersion + " (Latest)</green>";
    }

    public enum VersionStatus {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        ERROR
    }

    public record VersionCheckResult(VersionStatus status, String currentVersion, String latestVersion, String downloadUrl) {}
}
