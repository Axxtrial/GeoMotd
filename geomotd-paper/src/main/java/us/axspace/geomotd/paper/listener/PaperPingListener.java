package us.axspace.geomotd.paper.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;

import java.net.InetAddress;

/**
 * Ping listener for Paper, Purpur, and Folia servers.
 * Author: axxtrial
 */
public class PaperPingListener implements Listener {

    private final GeoMOTDCore core;

    public PaperPingListener(GeoMOTDCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperPing(PaperServerListPingEvent event) {
        InetAddress address = event.getAddress();
        MOTDEntry motd = core.getMOTDEntryForAddress(address);
        event.motd(motd.getFullComponent());
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
        } catch (Exception ignored) {}
    }
}
