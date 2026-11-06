package us.axspace.geomotd.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import us.axspace.geomotd.bungee.command.BungeeCommand;
import us.axspace.geomotd.bungee.icon.BungeeIconManager;
import us.axspace.geomotd.bungee.listener.BungeePingListener;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;

/**
 * BungeeCord / Waterfall Plugin Main Class.
 * Developed for axspace.us/geomotd
 */
public class GeoMOTDBungee extends Plugin {

    private GeoMOTDCore core;
    private BungeeIconManager iconManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        this.core = new GeoMOTDCore(getDataFolder(), getDescription().getVersion(), msg -> getLogger().info(msg));
        this.core.init();

        this.iconManager = new BungeeIconManager(getDataFolder(), core, msg -> getLogger().info(msg));
        this.iconManager.reload();
        this.core.registerReloadHook(iconManager::reload);

        MessagesManager messages = core.getMessages();
        getLogger().info(messages.getLog("logs.plugin-enabling", "BungeeCord / Waterfall"));

        // Register ping listener
        getProxy().getPluginManager().registerListener(this, new BungeePingListener(core, iconManager));

        // Register command
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(core));

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info(messages.getLog("logs.plugin-enabled", elapsed));
    }

    @Override
    public void onDisable() {
        if (core != null) {
            MessagesManager messages = core.getMessages();
            if (messages != null) {
                getLogger().info(messages.getLog("logs.plugin-disabled"));
            }
            core.shutdown();
        }
    }

    public GeoMOTDCore getCore() {
        return core;
    }
}
