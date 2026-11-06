package us.axspace.geomotd.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Ping listener for Velocity proxies.
 * Author: axxtrial
 */
public class VelocityPingListener {

    private final GeoMOTDCore core;

    public VelocityPingListener(GeoMOTDCore core) {
        this.core = core;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onProxyPing(ProxyPingEvent event) {
        InetSocketAddress remoteAddress = event.getConnection().getRemoteAddress();
        InetAddress address = remoteAddress != null ? remoteAddress.getAddress() : null;

        MOTDEntry motd = core.getMOTDEntryForAddress(address);

        ServerPing currentPing = event.getPing();
        ServerPing.Builder builder = currentPing.asBuilder();
        builder.description(motd.getFullComponent());

        event.setPing(builder.build());
    }
}
