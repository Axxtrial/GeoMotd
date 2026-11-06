package us.axspace.geomotd.velocity.icon;

import com.velocitypowered.api.util.Favicon;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Preloads and caches Minecraft 64x64 PNG server icons in memory for Velocity proxies.
 * Developed for axspace.us/geomotd
 */
public class VelocityIconManager {

    private final File dataFolder;
    private final GeoMOTDCore core;
    private final Consumer<String> logger;
    private final Map<String, Favicon> iconCache = new ConcurrentHashMap<>();

    public VelocityIconManager(File dataFolder, GeoMOTDCore core, Consumer<String> logger) {
        this.dataFolder = dataFolder;
        this.core = core;
        this.logger = logger != null ? logger : (s) -> {};
    }

    public synchronized void reload() {
        iconCache.clear();
        if (!core.getConfig().isIconsEnabled()) {
            return;
        }

        Set<String> paths = core.getConfig().getAllReferencedIconPaths();
        int loaded = 0;
        for (String relPath : paths) {
            File file = resolveFile(relPath);
            if (file != null && file.exists() && file.isFile()) {
                try {
                    Favicon icon = Favicon.create(file.toPath());
                    iconCache.put(relPath, icon);
                    loaded++;
                } catch (Exception e) {
                    logger.accept("Warning: Could not load server icon '" + relPath + "': " + e.getMessage() + ". Minecraft server icons must be 64x64 PNG images.");
                }
            }
        }
        if (loaded > 0) {
            logger.accept("Loaded " + loaded + " custom server icon(s) into memory cache.");
        }
    }

    public Favicon getIcon(MOTDEntry entry, String language) {
        if (!core.getConfig().isIconsEnabled()) {
            return null;
        }
        String path = core.getConfig().resolveIconPath(entry, language);
        if (path == null) {
            return null;
        }
        Favicon icon = iconCache.get(path);
        if (icon == null) {
            File file = resolveFile(path);
            if (file != null && file.exists() && file.isFile()) {
                try {
                    icon = Favicon.create(file.toPath());
                    iconCache.put(path, icon);
                } catch (Exception ignored) {}
            }
        }
        return icon;
    }

    private File resolveFile(String relPath) {
        File file = new File(dataFolder, relPath);
        if (file.exists()) {
            return file;
        }
        File direct = new File(relPath);
        if (direct.exists()) {
            return direct;
        }
        return file;
    }
}
