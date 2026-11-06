package us.axspace.geomotd.paper;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;
import us.axspace.geomotd.paper.command.PaperCommand;
import us.axspace.geomotd.paper.icon.PaperIconManager;
import us.axspace.geomotd.paper.listener.PaperPingListener;

/**
 * Main plugin class for Paper and Folia.
 * Developed for axspace.us/geomotd
 */
public class GeoMOTDPaper extends JavaPlugin {

    private GeoMOTDCore core;
    private PaperIconManager iconManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        this.core = new GeoMOTDCore(getDataFolder(), getDescription().getVersion(), msg -> getLogger().info(msg));
        this.core.init();

        this.iconManager = new PaperIconManager(getDataFolder(), core, msg -> getLogger().info(msg));
        this.iconManager.reload();
        this.core.registerReloadHook(iconManager::reload);

        MessagesManager messages = core.getMessages();
        getLogger().info(messages.getLog("logs.plugin-enabling", "Paper / Folia"));

        // Register ping listener
        getServer().getPluginManager().registerEvents(new PaperPingListener(core, iconManager), this);

        // Register command
        PluginCommand cmd = getCommand("geomotd");
        if (cmd != null) {
            PaperCommand paperCmd = new PaperCommand(core);
            cmd.setExecutor(paperCmd);
            cmd.setTabCompleter(paperCmd);
        }

        // bStats Metrics (can be disabled in config.yml)
        if (core.getConfig().isMetricsEnabled()) {
            int pluginId = 35172;
            try {
                org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);
                metrics.addCustomChart(new org.bstats.charts.SimplePie("default_language", () -> core.getConfig().getDefaultLanguage()));
                metrics.addCustomChart(new org.bstats.charts.SimplePie("auto_update_countries", () -> core.getConfig().isCountriesAutoUpdate() ? "Enabled" : "Disabled"));
            } catch (Exception ignored) {}
        }

        // Hook into PlaceholderAPI if present
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new us.axspace.geomotd.paper.placeholder.GeoMOTDExpansion(core, getDescription().getVersion()).register();
            getLogger().info("Hooked into PlaceholderAPI successfully!");
        }

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
