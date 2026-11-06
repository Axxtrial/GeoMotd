package us.axspace.geomotd.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.velocity.command.VelocityCommand;
import us.axspace.geomotd.velocity.listener.VelocityPingListener;

import java.nio.file.Path;

/**
 * Velocity Proxy Plugin Main Class.
 * Developed for axspace.us/geomotd
 */
@Plugin(
        id = "geomotd",
        name = "GeoMOTD",
        version = "1.0.0",
        description = "Ultra-fast GeoIP Multi-Language MOTD for Velocity",
        authors = {"axxtrial"}
)
public class GeoMOTDVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private GeoMOTDCore core;

    @Inject
    public GeoMOTDVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        long startTime = System.currentTimeMillis();

        this.core = new GeoMOTDCore(dataDirectory.toFile(), "1.0.0", logger::info);
        this.core.init();

        MessagesManager messages = core.getMessages();
        logger.info(messages.getLog("logs.plugin-enabling", "Velocity Proxy"));

        // Register ping listener
        server.getEventManager().register(this, new VelocityPingListener(core));

        // Register command
        CommandManager commandManager = server.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("geomotd")
                .plugin(this)
                .build();
        commandManager.register(meta, new VelocityCommand(core));

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info(messages.getLog("logs.plugin-enabled", elapsed));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            MessagesManager messages = core.getMessages();
            if (messages != null) {
                logger.info(messages.getLog("logs.plugin-disabled"));
            }
            core.shutdown();
        }
    }

    public GeoMOTDCore getCore() {
        return core;
    }
}
