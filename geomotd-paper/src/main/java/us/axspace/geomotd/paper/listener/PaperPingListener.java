package us.axspace.geomotd.paper.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;
import us.axspace.geomotd.paper.icon.PaperIconManager;

import java.net.InetAddress;

/**
 * Ping listener for Paper, Purpur, and Folia servers.
 * Supports dynamic MiniMessage MOTD and per-language server icons.
 * Developed for axspace.us/geomotd
 */
public class PaperPingListener implements Listener {

    private final GeoMOTDCore core;
    private final PaperIconManager iconManager;

    public PaperPingListener(GeoMOTDCore core, PaperIconManager iconManager) {
        this.core = core;
        this.iconManager = iconManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperPing(PaperServerListPingEvent event) {
        InetAddress address = event.getAddress();
        MOTDEntry motd = core.getMOTDEntryForAddress(address);
        event.motd(motd.getFullComponent());

        if (iconManager != null) {
            String language = core.getLanguageForAddress(address);
            CachedServerIcon icon = iconManager.getIcon(motd, language);
            if (icon != null) {
                event.setServerIcon(icon);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBukkitPing(ServerListPingEvent event) {
        // Crucial: PaperServerListPingEvent extends ServerListPingEvent!
        // We must ignore Paper events here so we do NOT overwrite the Adventure Component with raw strings!
        if (event instanceof PaperServerListPingEvent) {
            return;
        }

        // Fallback for standard Spigot without Paper's Adventure ping event
        try {
            InetAddress address = event.getAddress();
            MOTDEntry motd = core.getMOTDEntryForAddress(address);
            String legacy = LegacyComponentSerializer.legacySection().serialize(motd.getFullComponent());
            event.setMotd(legacy);

            if (iconManager != null) {
                String language = core.getLanguageForAddress(address);
                CachedServerIcon icon = iconManager.getIcon(motd, language);
                if (icon != null) {
                    event.setServerIcon(icon);
                }
            }
        } catch (Exception ignored) {}
    }
}
