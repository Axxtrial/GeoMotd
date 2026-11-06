package us.axspace.geomotd.paper.icon;

import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Preloads and caches Minecraft 64x64 PNG server icons in memory for Paper and Folia.
 * Completely eliminates disk I/O on ping requests.
 * Developed for axspace.us/geomotd
 */
public class PaperIconManager {

    private final File dataFolder;
    private final GeoMOTDCore core;
    private final Consumer<String> logger;
    private final Map<String, CachedServerIcon> iconCache = new ConcurrentHashMap<>();

    public PaperIconManager(File dataFolder, GeoMOTDCore core, Consumer<String> logger) {
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
                    CachedServerIcon icon = Bukkit.loadServerIcon(file);
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

    public CachedServerIcon getIcon(MOTDEntry entry, String language) {
        if (!core.getConfig().isIconsEnabled()) {
            return null;
        }
        String path = core.getConfig().resolveIconPath(entry, language);
        if (path == null) {
            return null;
        }
        CachedServerIcon icon = iconCache.get(path);
        if (icon == null) {
            File file = resolveFile(path);
            if (file != null && file.exists() && file.isFile()) {
                try {
                    icon = Bukkit.loadServerIcon(file);
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
