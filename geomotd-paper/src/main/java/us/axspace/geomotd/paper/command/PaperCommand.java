package us.axspace.geomotd.paper.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;

import java.util.*;

/**
 * Administrative command executor for Paper and Folia.
 * Developed for axspace.us/geomotd
 */
public class PaperCommand implements CommandExecutor, TabCompleter {

    private final GeoMOTDCore core;

    public PaperCommand(GeoMOTDCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessagesManager msg = core.getMessages();

        if (!sender.hasPermission("geomotd.admin")) {
            sender.sendMessage(msg.getPrefixed("general.no-permission"));
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender, msg);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                core.reload();
                sender.sendMessage(msg.getPrefixed("commands.reload.success"));
            }
            case "status" -> {
                sendStatus(sender, msg);
            }
            default -> {
                sender.sendMessage(msg.getPrefixed("general.unknown-command"));
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender, MessagesManager msg) {
        sender.sendMessage(msg.getComponent("commands.help.header"));
        sender.sendMessage(msg.getComponent("commands.help.reload"));
        sender.sendMessage(msg.getComponent("commands.help.status"));
    }

    private void sendStatus(CommandSender sender, MessagesManager msg) {
        sender.sendMessage(msg.getComponent("commands.status.header"));

        String versionStr = core.getVersionManager() != null ? core.getVersionManager().getStatusDisplay() : "1.0.0";
        sender.sendMessage(msg.getComponent("commands.status.version", Collections.singletonMap("version", versionStr)));

        Map<String, String> platformMap = Collections.singletonMap("platform", "Paper / Folia");
        sender.sendMessage(msg.getComponent("commands.status.platform", platformMap));

        boolean isLoaded = core.getGeoIPService().isLoaded();
        String statusStr = isLoaded ? msg.getRaw("commands.status.database-loaded-true") : msg.getRaw("commands.status.database-loaded-false");
        sender.sendMessage(msg.getComponent("commands.status.database-loaded", Collections.singletonMap("status", statusStr)));

        sender.sendMessage(msg.getComponent("commands.status.countries-count", Collections.singletonMap("count", String.valueOf(core.getCountryManager().getCountryCount()))));
        sender.sendMessage(msg.getComponent("commands.status.configured-languages", Collections.singletonMap("languages", core.getConfig().getConfiguredLanguages().toString())));
        sender.sendMessage(msg.getComponent("commands.status.default-language", Collections.singletonMap("language", core.getConfig().getDefaultLanguage())));
        sender.sendMessage(msg.getComponent("commands.status.cache-size", Collections.singletonMap("size", String.valueOf(core.getGeoIPService().getCacheSize()))));
        sender.sendMessage(msg.getComponent("commands.status.cache-hit-rate", Collections.singletonMap("rate", String.format(Locale.ROOT, "%.2f%%", core.getGeoIPService().getCacheHitRate() * 100))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("geomotd.admin")) {
            return Collections.emptyList();
        }
        if (args.length <= 1) {
            String current = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> options = Arrays.asList("reload", "status", "help");
            List<String> result = new ArrayList<>();
            for (String opt : options) {
                if (opt.toLowerCase(Locale.ROOT).startsWith(current)) {
                    result.add(opt);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
