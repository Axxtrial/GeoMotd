package us.axspace.geomotd.paper.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBukkitPing(ServerListPingEvent event) {
        // Fallback for standard Bukkit if PaperServerListPingEvent isn't fired
        try {
            InetAddress address = event.getAddress();
            MOTDEntry motd = core.getMOTDEntryForAddress(address);
            event.setMotd(motd.getRawLine1() + "\n" + motd.getRawLine2());
        } catch (Exception ignored) {}
    }
}
