package us.axspace.geomotd.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;

import java.util.*;

/**
 * Command handler for Velocity proxies.
 * Developed for axspace.us/geomotd
 */
public class VelocityCommand implements SimpleCommand {

    private final GeoMOTDCore core;

    public VelocityCommand(GeoMOTDCore core) {
        this.core = core;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        MessagesManager msg = core.getMessages();

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(source, msg);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                core.reload();
                source.sendMessage(msg.getPrefixed("commands.reload.success"));
            }
            case "status" -> {
                sendStatus(source, msg);
            }
            default -> {
                source.sendMessage(msg.getPrefixed("general.unknown-command"));
            }
        }
    }

    private void sendHelp(CommandSource source, MessagesManager msg) {
        source.sendMessage(msg.getComponent("commands.help.header"));
        source.sendMessage(msg.getComponent("commands.help.reload"));
        source.sendMessage(msg.getComponent("commands.help.status"));
    }

    private void sendStatus(CommandSource source, MessagesManager msg) {
        source.sendMessage(msg.getComponent("commands.status.header"));

        String versionStr = core.getVersionManager() != null ? core.getVersionManager().getStatusDisplay() : "1.0.0";
        source.sendMessage(msg.getComponent("commands.status.version", Collections.singletonMap("version", versionStr)));

        source.sendMessage(msg.getComponent("commands.status.platform", Collections.singletonMap("platform", "Velocity Proxy")));

        boolean isLoaded = core.getGeoIPService().isLoaded();
        String statusStr = isLoaded ? msg.getRaw("commands.status.database-loaded-true") : msg.getRaw("commands.status.database-loaded-false");
        source.sendMessage(msg.getComponent("commands.status.database-loaded", Collections.singletonMap("status", statusStr)));

        source.sendMessage(msg.getComponent("commands.status.countries-count", Collections.singletonMap("count", String.valueOf(core.getCountryManager().getCountryCount()))));
        source.sendMessage(msg.getComponent("commands.status.configured-languages", Collections.singletonMap("languages", core.getConfig().getConfiguredLanguages().toString())));
        source.sendMessage(msg.getComponent("commands.status.default-language", Collections.singletonMap("language", core.getConfig().getDefaultLanguage())));
        source.sendMessage(msg.getComponent("commands.status.cache-size", Collections.singletonMap("size", String.valueOf(core.getGeoIPService().getCacheSize()))));
        source.sendMessage(msg.getComponent("commands.status.cache-hit-rate", Collections.singletonMap("rate", String.format(Locale.ROOT, "%.2f%%", core.getGeoIPService().getCacheHitRate() * 100))));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasPermission(invocation)) {
            return Collections.emptyList();
        }
        String[] args = invocation.arguments();
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

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("geomotd.admin");
    }
}
