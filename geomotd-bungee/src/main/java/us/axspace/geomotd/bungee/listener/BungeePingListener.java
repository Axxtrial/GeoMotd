package us.axspace.geomotd.bungee.listener;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MOTDEntry;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Ping listener for BungeeCord and Waterfall proxies.
 * Author: axxtrial
 */
public class BungeePingListener implements Listener {

    private final GeoMOTDCore core;

    public BungeePingListener(GeoMOTDCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        if (ping == null) return;

        SocketAddress socketAddress = event.getConnection().getSocketAddress();
        InetAddress address = (socketAddress instanceof InetSocketAddress isa) ? isa.getAddress() : null;

        MOTDEntry motd = core.getMOTDEntryForAddress(address);

        try {
            String json = GsonComponentSerializer.gson().serialize(motd.getFullComponent());
            BaseComponent[] components = ComponentSerializer.parse(json);
            ping.setDescriptionComponent(new TextComponent(components));
        } catch (Exception e) {
            // Fallback plain
            ping.setDescription(motd.getRawLine1() + "\n" + motd.getRawLine2());
        }

        event.setResponse(ping);
    }
}
