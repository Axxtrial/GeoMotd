package us.axspace.geomotd.core.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Utility for HTTP downloads with follow-redirects and timeouts.
 * Author: axxtrial
 */
public class HttpUtil {

    private static final String USER_AGENT = "GeoMOTD/1.0 (axspace.us; Minecraft Plugin)";

    public static boolean downloadFile(String fileUrl, File destination, Consumer<String> logger) {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(fileUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                String newUrl = connection.getHeaderField("Location");
                return downloadFile(newUrl, destination, logger);
            }

            if (status != HttpURLConnection.HTTP_OK) {
                if (logger != null) logger.accept("HTTP Download failed. Status code: " + status);
                return false;
            }

            File parent = destination.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            File tempFile = new File(destination.getAbsolutePath() + ".tmp");
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            if (destination.exists()) {
                destination.delete();
            }
            return tempFile.renameTo(destination);
        } catch (Exception e) {
            if (logger != null) logger.accept("Error downloading file from " + fileUrl + ": " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String fetchString(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP response code: " + code);
        }

        try (InputStream in = conn.getInputStream();
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        } finally {
            conn.disconnect();
        }
    }
}
