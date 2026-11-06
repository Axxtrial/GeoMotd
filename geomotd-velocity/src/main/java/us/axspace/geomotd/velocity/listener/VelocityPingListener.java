package us.axspace.geomotd.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;
import us.axspace.geomotd.velocity.icon.VelocityIconManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Ping listener for Velocity proxies.
 * Supports dynamic MiniMessage MOTD and per-language server icons.
 * Developed for axspace.us/geomotd
 */
public class VelocityPingListener {

    private final GeoMOTDCore core;
    private final VelocityIconManager iconManager;

    public VelocityPingListener(GeoMOTDCore core, VelocityIconManager iconManager) {
        this.core = core;
        this.iconManager = iconManager;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onProxyPing(ProxyPingEvent event) {
        InetSocketAddress remoteAddress = event.getConnection().getRemoteAddress();
        InetAddress address = remoteAddress != null ? remoteAddress.getAddress() : null;

        MOTDEntry motd = core.getMOTDEntryForAddress(address);

        ServerPing currentPing = event.getPing();
        ServerPing.Builder builder = currentPing.asBuilder();
        builder.description(motd.getFullComponent());

        if (iconManager != null) {
            String language = core.getLanguageForAddress(address);
            Favicon icon = iconManager.getIcon(motd, language);
            if (icon != null) {
                builder.favicon(icon);
            }
        }

        event.setPing(builder.build());
    }
}
